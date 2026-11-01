#!/usr/bin/env python3
"""UNALLOCATED sound synthesizer.

Every sound is generated from scratch with numpy/scipy (no samples, no vanilla
assets). Deterministic: each sound uses its own fixed RNG seed.

Usage: python tools/sounds/synth.py
Writes 44.1 kHz WAVs to a temp dir, converts them to OGG Vorbis with ffmpeg and
places them in src/main/resources/assets/unallocated/sounds/<path>.ogg
"""
import os
import subprocess
import tempfile
import zlib
from pathlib import Path

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, fftconvolve, sosfilt

SR = 44100
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/unallocated/sounds"

# ----------------------------------------------------------------- helpers


def rng_for(name):
    return np.random.default_rng(zlib.crc32(name.encode()) & 0xFFFFFFFF)


def tvec(dur):
    return np.arange(int(round(dur * SR))) / SR


def noise(r, n):
    return r.standard_normal(n)


def _clip_f(f):
    return float(np.clip(f, 20.0, SR / 2 - 500))


def lp(x, f, order=2):
    return sosfilt(butter(order, _clip_f(f), "low", fs=SR, output="sos"), x)


def hp(x, f, order=2):
    return sosfilt(butter(order, _clip_f(f), "high", fs=SR, output="sos"), x)


def bp(x, f, q=4.0, order=2):
    lo = _clip_f(f * (1 - 0.5 / q))
    hi = _clip_f(f * (1 + 0.5 / q))
    if hi <= lo * 1.01:
        hi = lo * 1.05
    return sosfilt(butter(order, [lo, hi], "band", fs=SR, output="sos"), x)


def sweep_bp(x, fcurve, q=4.0, chunk=512):
    """Band-pass with a time-varying centre frequency (chunked, state carried)."""
    out = np.zeros_like(x)
    zi = None
    for s in range(0, len(x), chunk):
        f = _clip_f(fcurve[min(s + chunk // 2, len(x) - 1)])
        sos = butter(2, [_clip_f(f * (1 - 0.5 / q)), _clip_f(f * (1 + 0.5 / q))],
                     "band", fs=SR, output="sos")
        if zi is None:
            zi = np.zeros((sos.shape[0], 2))
        seg, zi = sosfilt(sos, x[s:s + chunk], zi=zi)
        out[s:s + chunk] = seg
    return out


def fade(x, fin_ms=5, fout_ms=5):
    x = x.copy()
    a = int(SR * fin_ms / 1000)
    b = int(SR * fout_ms / 1000)
    if a > 0:
        x[:a] *= np.linspace(0, 1, a)
    if b > 0:
        x[-b:] *= np.linspace(1, 0, b)
    return x


def expdec(t, tau):
    return np.exp(-t / tau)


def sat(x, drive=3.0):
    return np.tanh(x * drive) / np.tanh(drive)


def bitcrush(x, bits):
    q = 2 ** (bits - 1)
    return np.round(x * q) / q


def decimate_hold(x, n):
    """Sample-rate reduction by sample-and-hold."""
    n = max(1, int(n))
    return np.repeat(x[::n], n)[:len(x)]


def phase_from_freq(f):
    return 2 * np.pi * np.cumsum(f) / SR


def smooth_noise(r, n, rate_hz):
    """Slowly varying random curve in [-1, 1] (interpolated random points)."""
    pts = max(2, int(n / SR * rate_hz) + 2)
    v = r.uniform(-1, 1, pts)
    return np.interp(np.linspace(0, pts - 1, n), np.arange(pts), v)


def reverb_ir(r, dur, tau, dark=3000):
    n = int(dur * SR)
    t = np.arange(n) / SR
    ir = noise(r, n) * np.exp(-t / tau)
    ir = lp(ir, dark)
    ir[:int(0.01 * SR)] *= np.linspace(0, 1, int(0.01 * SR))
    return ir / np.sqrt(np.sum(ir ** 2))


def reverb(x, r, dur=3.0, tau=0.8, wet=0.7, dark=3000, tail=0.0):
    ir = reverb_ir(r, dur, tau, dark)
    y = fftconvolve(x, ir)
    xd = np.pad(x, (0, len(y) - len(x)))
    return (1 - wet) * xd + wet * y * (np.std(x) / (np.std(y) + 1e-9)) * 1.5


def loop_crossfade(x, xf):
    """Make x loop seamlessly: crossfade its extra tail (len xf) into the head."""
    n = x.shape[0] - xf
    out = x[:n].copy()
    w = np.linspace(0, np.pi / 2, xf)
    fi, fo = np.sin(w), np.cos(w)
    if x.ndim == 2:
        fi, fo = fi[:, None], fo[:, None]
    out[:xf] = out[:xf] * fi + x[n:n + xf] * fo
    return out


def formant_noise(src, weights, vowels, bw_q=8.0):
    """Mix of vowel-filtered copies of src; weights: (n_vowels, n) in [0,1]."""
    out = np.zeros_like(src)
    for i, fs_ in enumerate(vowels):
        y = sum(bp(src, f, q=bw_q) * g for f, g in zip(fs_, (1.0, 0.7, 0.4)))
        out += y * weights[i]
    return out


VOWELS = {  # rough F1, F2, F3
    "a": (800, 1150, 2800), "e": (400, 1900, 2600), "i": (300, 2300, 3000),
    "o": (450, 800, 2830), "u": (325, 700, 2530),
}


def peak_norm(x, db=-1.0):
    return x / (np.max(np.abs(x)) + 1e-12) * 10 ** (db / 20)


def rms_norm(x, db=-12.0, peak_db=-1.0):
    ref = x[np.abs(x) > 1e-4] if np.any(np.abs(x) > 1e-4) else x
    g = 10 ** (db / 20) / (np.sqrt(np.mean(ref ** 2)) + 1e-12)
    y = x * g
    pk = np.max(np.abs(y))
    lim = 10 ** (peak_db / 20)
    if pk > lim:
        y *= lim / pk
    return y


def add_at(buf, snd, pos):
    i = int(pos * SR)
    if i >= len(buf):
        return
    m = min(len(snd), len(buf) - i)
    buf[i:i + m] += snd[:m]


def click(r, f, dur_ms=4, tau_ms=1.2, noise_amt=0.3):
    n = int(SR * dur_ms / 1000)
    t = np.arange(n) / SR
    e = np.exp(-t / (tau_ms / 1000))
    return (np.sin(2 * np.pi * f * t) + noise_amt * noise(r, n)) * e


def modal(t, f0, ratios, decays, amps, bend=None):
    """Sum of decaying sinusoids. bend(t) -> multiplicative pitch factor."""
    b = bend(t) if bend is not None else np.ones_like(t)
    y = np.zeros_like(t)
    for rt, dc, am in zip(ratios, decays, amps):
        y += am * np.sin(phase_from_freq(f0 * rt * b)) * np.exp(-t / dc)
    return y


# ------------------------------------------------------------------ sounds
SOUNDS = {}  # path -> (function, kind)  kind: 'peak' or 'rms'


def sound(path, kind="rms"):
    def deco(fn):
        SOUNDS[path] = (fn, kind)
        return fn
    return deco


@sound("ambient/drone", "stereo")
def drone(r):
    dur, xf = 40.0, 4.0
    n = int((dur + xf) * SR)
    t = np.arange(n) / SR
    chans = []
    for side in range(2):
        # mains hum, 50 Hz + harmonics, frequency wobbles slightly
        wob = 1 + 0.004 * smooth_noise(r, n, 0.4)
        hum = np.zeros(n)
        for k, a in zip(range(1, 8), (1.0, 0.5, 0.45, 0.2, 0.25, 0.1, 0.08)):
            hum += a * np.sin(phase_from_freq(50 * k * wob) + r.uniform(0, 6.28))
        hum *= 0.5 * (1 + 0.15 * smooth_noise(r, n, 0.3))
        # two detuned pads that beat slowly (side offsets widen the stereo image)
        pad = np.zeros(n)
        for f in (55.0 + 0.11 * side, 55.37 + 0.07 * side, 110.2 - 0.1 * side):
            ph = phase_from_freq(f * np.ones(n))
            saw = ((ph / (2 * np.pi)) % 1) * 2 - 1
            pad += 0.5 * lp(saw, 220) + 0.7 * np.sin(ph)
        pad *= 0.35 * (1 + 0.3 * np.sin(2 * np.pi * 0.07 * t + side))
        # slow filtered-noise air
        air = lp(noise(r, n), 600)
        air = lp(air, 250) * (0.5 + 0.5 * (smooth_noise(r, n, 0.12) * 0.5 + 0.5))
        air *= 2.2
        hiss = bp(noise(r, n), 5200, 2) * 0.02 * (0.5 + 0.5 * np.sin(2 * np.pi * 0.05 * t))
        chans.append(hum * 0.55 + pad * 0.7 + air * 0.9 + hiss)
    st = np.stack(chans, axis=1)
    # No data chirps in the bed: the owner found looping beeps tiring and meaningless. Beeps now play only as
    # scripted glitch cues (fx/glitch_*), when something happens.
    st = loop_crossfade(st, int(xf * SR))
    return st


@sound("ambient/distant_1")
def distant_1(r):
    dur = 9.0
    n = int(dur * SR)
    t = np.arange(n) / SR
    gd = 3.2
    gm = t < gd
    env = np.where(gm, np.sin(np.pi * np.clip(t / gd, 0, 1)) ** 1.5, 0)
    # metallic groan: falling-then-rising frequency with stressed partials
    f = 110 - 40 * (t / gd) + 12 * np.sin(2 * np.pi * 5.5 * t) * (t / gd)
    f = np.where(gm, f, 70)
    ph = phase_from_freq(f)
    saw = ((ph / (2 * np.pi)) % 1) * 2 - 1
    y = saw
    y = sweep_bp(y, 500 + 900 * np.sin(np.pi * np.clip(t / gd, 0, 1)), q=3)
    y += 0.5 * sweep_bp(noise(r, n), 220 + 300 * (t / gd), q=6)
    y += 0.4 * np.sin(ph * 2.76) * 0.3
    y *= env
    y = reverb(y, r, dur=5.0, tau=1.4, wet=0.85, dark=1800)
    y = y[:n]
    y = lp(y, 2500)
    return fade(y, 40, 300)


@sound("ambient/distant_2")
def distant_2(r):
    dur = 8.0
    n = int(dur * SR)
    y = np.zeros(n)
    pos = 0.4
    gaps = [0.5, 0.22, 0.2, 1.1, 0.35, 0.6, 0.18, 1.4, 0.3]
    for g in gaps:
        m = int(0.25 * SR)
        t = np.arange(m) / SR
        f = r.uniform(85, 150)
        k = np.sin(2 * np.pi * f * (1 - 0.3 * t) * t) * expdec(t, 0.035)
        k += 0.5 * lp(noise(r, m), 900) * expdec(t, 0.012)
        k += 0.25 * np.sin(2 * np.pi * f * 2.9 * t) * expdec(t, 0.02)
        add_at(y, k * r.uniform(0.6, 1.0), pos)
        pos += g
    y = reverb(y, r, dur=5.0, tau=1.6, wet=0.88, dark=1500)[:n]
    y = lp(y, 2200)
    return fade(y, 20, 400)


def _vibrate(r, f0, extras, B, dur=2.6, bowed=0.5):
    t = tvec(dur)
    ks = np.arange(1, 9)
    ratios = list(ks * np.sqrt(1 + B * ks ** 2)) + list(extras)
    decays = [2.2 / (1 + 0.55 * i) for i in range(len(ks))] + [0.9, 0.6][:len(extras)]
    amps = [1.0 / (1 + 0.45 * i) for i in range(len(ks))] + [0.25, 0.18][:len(extras)]

    def bend(tt):
        return 1 - 0.035 * (1 - np.exp(-tt / 0.5)) + 0.01 * np.exp(-tt / 0.03)

    y = modal(t, f0, ratios, decays, amps, bend)
    y *= 1 - np.exp(-t / 0.002)  # pluck attack
    # scraped / bowed noise
    sc = bp(noise(r, len(t)), f0 * 3.1, q=2.5) * (np.exp(-t / 0.25) * (1 - np.exp(-t / 0.04)))
    sc += 0.3 * hp(noise(r, len(t)), 3000) * expdec(t, 0.04)
    y = y / np.max(np.abs(y)) + bowed * sc / (np.max(np.abs(sc)) + 1e-9) * 0.6
    return fade(lp(y, 5000), 3, 120)


@sound("thread/vibrate_1")
def vibrate_1(r):
    return _vibrate(r, 146.8, [2.76, 5.4], 0.004)


@sound("thread/vibrate_2")
def vibrate_2(r):
    return _vibrate(r, 207.6, [3.13, 6.1], 0.009, dur=2.4, bowed=0.7)


@sound("thread/vibrate_3")
def vibrate_3(r):
    return _vibrate(r, 110.0, [2.41, 4.7], 0.002, dur=3.0, bowed=0.4)


@sound("thread/chime")
def chime(r):
    t = tvec(2.0)
    y = np.zeros_like(t)
    for fc, a in ((1318.5, 1.0), (1325.9, 0.8), (1976.0, 0.4), (2791.0, 0.25)):
        idx = 3.0 * np.exp(-t / 0.5)
        y += a * np.sin(2 * np.pi * fc * t + idx * np.sin(2 * np.pi * fc * 3.51 * t))
    y *= (1 - np.exp(-t / 0.03)) * np.exp(-t / 0.55)
    y = hp(y, 500)
    return fade(y, 10, 100)


@sound("thread/snap")
def snap(r):
    t = tvec(1.0)
    n = len(t)
    tr = hp(noise(r, n), 2500) * expdec(t, 0.004)
    tr += 0.8 * np.sin(2 * np.pi * 180 * t) * expdec(t, 0.006)
    f = 1100 * np.exp(-t / 0.05) + 380
    tw = np.sin(phase_from_freq(f)) * expdec(t, 0.09) * 0.6
    tw += 0.3 * np.sin(phase_from_freq(f * 2.76)) * expdec(t, 0.04)
    y = tr + tw
    return fade(y, 0.2 and 5, 80)


@sound("fx/breath")
def breath(r):
    dur = 1.2
    n = int(dur * SR)
    t = np.arange(n) / SR
    te = 1.0
    x = noise(r, n)
    fc = 250 + 2600 * np.clip(t / te, 0, 1) ** 1.5
    y = sweep_bp(x, fc, q=1.2) + 0.3 * lp(x, 400)
    # reversed-decay swell: grows then cuts off
    env = np.where(t < te, (t / te) ** 2.2, 0)
    y *= env
    y = fade(y, 20, 5)
    th = np.sin(2 * np.pi * 42 * np.clip(t - te, 0, None) * (1 - 0.5 * (t - te)))
    th *= (t >= te) * expdec(np.clip(t - te, 0, None), 0.06) * 2.5 * np.max(np.abs(y)) * 2
    th = fade(th, 3, 5)
    return y + th


def _glitch_base(r, n, kind):
    t = np.arange(n) / SR
    if kind == "beep":
        gate = (np.floor(t * 55) % 2 == 0) | (np.floor(t * 23) % 3 == 0)
        return np.sign(np.sin(2 * np.pi * 1750 * t)) * 0.5 * gate + 0.15 * noise(r, n) * gate
    if kind == "noise":
        return noise(r, n) * 0.4 * (np.floor(t * 40) % 2 == 0)
    f = 300 + 1500 * (np.floor(t * 30) % 5) / 5
    return np.sin(phase_from_freq(f)) * 0.6 + 0.3 * np.sign(np.sin(phase_from_freq(f * 0.5)))


def _stutter(x, r, seg_ms, reps_range=(3, 8)):
    out = []
    pos = 0
    seg = int(seg_ms / 1000 * SR)
    while pos < len(x):
        s = max(40, int(seg * r.uniform(0.4, 1.6)))
        chunk = x[pos:pos + s]
        for _ in range(int(r.integers(*reps_range))):
            out.append(chunk)
        pos += s + int(r.integers(0, seg))
    return np.concatenate(out)


@sound("fx/glitch_1")
def glitch_1(r):
    y = _glitch_base(r, int(0.5 * SR), "beep")
    y = bitcrush(decimate_hold(y, 6), 4)
    y = _stutter(y, r, 18, (2, 5))[:int(0.5 * SR)]
    return fade(y, 5, 20)


@sound("fx/glitch_2")
def glitch_2(r):
    y = _glitch_base(r, int(0.7 * SR), "noise")
    y += _glitch_base(r, int(0.7 * SR), "tone") * 0.5
    y = decimate_hold(bitcrush(y, 3), 11)
    y = _stutter(y, r, 30, (2, 4))[:int(0.7 * SR)]
    y *= (np.arange(len(y)) / SR % 0.09 < 0.06)
    return fade(y, 5, 30)


@sound("fx/glitch_3")
def glitch_3(r):
    n = int(0.4 * SR)
    t = np.arange(n) / SR
    y = np.sin(phase_from_freq(2400 * np.exp(-t * 6)) ) + 0.5 * noise(r, n)
    y = bitcrush(decimate_hold(y, 3), 5)
    y = np.concatenate([y[:int(.03 * SR)]] * 4 + [y[int(.05 * SR):int(.09 * SR)]] * 3 + [y[:int(.12 * SR)]])
    y = hp(y, 600)
    return fade(y, 5, 25)


@sound("screamer/impact", "peak")
def impact(r):
    n = int(1.3 * SR)
    t = np.arange(n) / SR
    bn = noise(r, n) * expdec(t, 0.05)
    sub = np.sin(phase_from_freq(32 + 90 * np.exp(-t / 0.08))) * expdec(t, 0.35) * 1.6
    fcar = 1500 * np.exp(-t / 0.35) + 250
    fm = np.sin(phase_from_freq(fcar * 1.414))
    scr = np.sin(phase_from_freq(fcar) + 9 * fm * np.exp(-t / 0.5))
    scr += np.sin(phase_from_freq(fcar * 1.013) + 6 * fm)
    scr *= expdec(t, 0.3) * 0.7
    y = sat(bn * 1.2 + sub + scr, 3.5)
    y[:] = y
    y = y * np.where(t > 1.1, np.clip(1 - (t - 1.1) / 0.2, 0, 1), 1)
    return y


@sound("screamer/shriek", "peak")
def shriek(r):
    dur = 1.6
    n = int(dur * SR)
    t = np.arange(n) / SR
    glide = 500 + 1500 * np.clip(t / 1.1, 0, 1) ** 1.5
    crack = t > 1.15
    glide = np.where(crack, 1900 * np.exp(-(t - 1.15) * 3) + 200 * r.uniform(0.5, 1.5, n)[0], glide)
    vib = 1 + 0.04 * np.sin(2 * np.pi * 7 * t) + 0.02 * np.sin(2 * np.pi * 13 * t)
    f = glide * vib
    src = np.zeros(n)
    for d in (1.0, 1.007, 0.993, 2.01):
        ph = phase_from_freq(f * d / 2)
        src += ((ph / (2 * np.pi)) % 1) * 2 - 1
    src += 0.5 * noise(r, n)
    # formants morph a -> i -> a
    w_a = 0.5 + 0.5 * np.cos(2 * np.pi * 2.2 * t)
    y = formant_noise(src, [w_a, 1 - w_a], [VOWELS["a"], VOWELS["i"]], bw_q=6)
    y = sat(y / (np.max(np.abs(y)) + 1e-9), 6)
    y += 0.3 * hp(noise(r, n), 3500) * (crack * expdec(np.clip(t - 1.15, 0, None), 0.1))
    env = np.minimum(1, t / 0.004) * np.where(t < 1.15, 1, expdec(np.clip(t - 1.15, 0, None), 0.12))
    return y * env


def _clicks(r, dur, n_events, resonant, base=(1800, 6000)):
    buf = np.zeros(int(dur * SR))
    pos = 0.01
    for _ in range(n_events):
        burst = int(r.integers(1, 4))
        for _b in range(burst):
            c = click(r, r.uniform(*base), dur_ms=r.uniform(3, 8),
                      tau_ms=r.uniform(0.6, 2.5) if resonant else 0.5)
            add_at(buf, c * r.uniform(0.4, 1.0), pos)
            pos += r.uniform(0.004, 0.012)
        pos += r.uniform(0.02, 0.09) * (1 + r.integers(0, 3))
        if pos > dur - 0.03:
            break
    return buf


@sound("reclaimer/click_1")
def click_1(r):
    return fade(_clicks(r, 0.45, 12, True), 2, 30)


@sound("reclaimer/click_2")
def click_2(r):
    y = _clicks(r, 0.9, 18, False, (900, 3500))
    return fade(bp(y, 2500, 0.7), 2, 40)


@sound("reclaimer/click_3")
def click_3(r):
    y = _clicks(r, 0.3, 8, True, (3000, 7000))
    return fade(y, 2, 20)


@sound("reclaimer/click_4")
def click_4(r):
    y = _clicks(r, 1.0, 26, True, (700, 5000))
    # hard-drive seek whirr under the clicks
    t = tvec(1.0)
    y += 0.25 * np.sin(phase_from_freq(180 + 700 * np.abs(np.sin(2 * np.pi * 3 * t)))) * np.hanning(len(t))
    return fade(y, 2, 60)


def _step(r, f0, click_f):
    t = tvec(0.55)
    n = len(t)
    thud = np.sin(phase_from_freq(f0 * (1 + 0.6 * np.exp(-t / 0.05)))) * expdec(t, 0.09)
    thud += 0.5 * lp(noise(r, n), 250) * expdec(t, 0.04)
    bony = np.zeros(n)
    for dly, fr, a in ((0.002, click_f, 0.7), (0.014, click_f * 1.7, 0.5)):
        add_at(bony, click(r, fr, 8, 1.5, 0.5) * a, dly)
    deb = np.zeros(n)
    for _ in range(int(r.integers(5, 10))):
        add_at(deb, click(r, r.uniform(3000, 7500), 3, 0.5, 0.6) * r.uniform(0.05, 0.2), r.uniform(0.06, 0.45))
    y = thud * 1.3 + bony * 0.5 + deb
    return fade(y, 2, 60)


@sound("reclaimer/step_1")
def step_1(r):
    return _step(r, 55, 1700)


@sound("reclaimer/step_2")
def step_2(r):
    return _step(r, 48, 2300)


@sound("reclaimer/step_3")
def step_3(r):
    return _step(r, 62, 1400)


@sound("reclaimer/breath")
def r_breath(r):
    dur, xf = 4.0, 0.4
    n = int((dur + xf) * SR)
    t = np.arange(n) / SR
    cyc = (t % dur) / dur  # one breath per loop
    inh = np.where(cyc < 0.45, np.abs(np.sin(np.pi * cyc / 0.45)) ** 1.5, 0)
    exh = np.where((cyc >= 0.5) & (cyc < 0.95), np.abs(np.sin(np.pi * (cyc - 0.5) / 0.45)) ** 1.5, 0)
    x = noise(r, n)
    fc = 500 + 700 * inh - 300 * exh + 250 * np.sin(2 * np.pi * 1.1 * t)
    y = sweep_bp(x, fc, q=2.5) + 0.6 * sweep_bp(x, fc * 2.3, q=3)
    y *= (0.15 + 0.85 * np.maximum(inh, exh * 0.8))
    wet = lp(noise(r, n), 1200) * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * t + 2 * np.sin(2 * np.pi * 0.5 * t))) ** 3
    y += 0.5 * wet * np.maximum(inh, exh) * 0.8
    whine = np.sin(phase_from_freq(310 + 120 * (inh + exh))) * 0.05 * (0.3 + inh + exh)
    y = y / np.max(np.abs(y)) + whine
    return loop_crossfade(y, int(xf * SR))


@sound("reclaimer/roar", "peak")
def roar(r):
    dur = 2.5
    n = int(dur * SR)
    t = np.arange(n) / SR
    body = 2.0
    glide = 700 * np.exp(-t / 0.9) + 90
    vib = 1 + 0.03 * np.sin(2 * np.pi * 8 * t)
    layers = np.zeros(n)
    for d, a in ((1.0, 1.0), (0.5, 0.8), (1.5, 0.4), (1.004, 0.7)):
        ph = phase_from_freq(glide * d * vib)
        layers += a * (((ph / (2 * np.pi)) % 1) * 2 - 1)
    layers += 0.7 * noise(r, n) * (0.4 + 0.6 * np.sin(2 * np.pi * 30 * t) ** 2)
    w = 0.5 + 0.5 * np.sin(2 * np.pi * 1.3 * t)
    y = formant_noise(layers, [w, 1 - w], [VOWELS["a"], VOWELS["o"]], bw_q=5)
    y += 0.3 * lp(layers, 400)
    y = sat(y / (np.max(np.abs(y)) + 1e-9), 5)
    env = np.minimum(1, t / 0.02) * np.where(t < body, 1, 0)
    env *= np.where(t < 0.15, 1, 1)  # sustained
    env *= np.clip((body - t) / 0.2, 0, 1) * (1 if True else 0)
    y *= env
    # digital stutter tail
    s0 = int(1.6 * SR)
    seg = y[s0:s0 + int(0.045 * SR)]
    st = np.concatenate([seg] * 4 + [seg[:len(seg) // 2]] * 3 + [seg[:len(seg) // 4]] * 4)
    st = bitcrush(decimate_hold(st, 4), 5)
    st *= np.linspace(1, 0.5, len(st))
    y[s0:] = 0
    m = min(len(st), n - s0)
    y[s0:s0 + m] = st[:m]
    return fade(y, 3, 5)


def _whisper(r, seed_syl, dur=2.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    src = noise(r, n)
    keys = list(VOWELS)
    # fast-changing vowel weights
    ws = []
    for _ in keys:
        c = smooth_noise(r, n, 14) * 0.5 + 0.5
        ws.append(c ** 3)
    tot = sum(ws) + 1e-9
    ws = [w / tot for w in ws]
    y = formant_noise(src, ws, [VOWELS[k] for k in keys], bw_q=7)
    # syllable rhythm
    srate = seed_syl
    ph = np.cumsum(srate * (1 + 0.5 * smooth_noise(r, n, 3))) / SR
    am = np.abs(np.sin(np.pi * ph)) ** 1.5
    am *= 0.5 + 0.5 * (smooth_noise(r, n, 5) * 0.5 + 0.5)
    y = (y + 0.3 * hp(src, 4500)) * am
    y *= np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 0.6
    return fade(y, 40, 80)


@sound("reclaimer/whisper_1")
def whisper_1(r):
    return _whisper(r, 4.5)


@sound("reclaimer/whisper_2")
def whisper_2(r):
    return _whisper(r, 6.0)


@sound("reclaimer/whisper_3")
def whisper_3(r):
    return _whisper(r, 3.2, 2.2)


@sound("ui/heartbeat")
def heartbeat(r):
    t = tvec(1.2)
    n = len(t)
    y = np.zeros(n)
    for pos, f, a, tau in ((0.0, 58, 1.0, 0.07), (0.27, 46, 0.7, 0.08)):
        m = int(0.4 * SR)
        tt = np.arange(m) / SR
        b = np.sin(phase_from_freq(f * (1 + 0.4 * np.exp(-tt / 0.03)))) * expdec(tt, tau)
        b *= np.minimum(1, tt / 0.008)
        add_at(y, b * a, pos)
    y = lp(y, 160, 3)
    return fade(y, 3, 100)


@sound("ui/terminal")
def terminal(r):
    t = tvec(0.15)
    n = len(t)
    y = np.sign(np.sin(2 * np.pi * 1900 * t)) * expdec(t, 0.012) * 0.4
    y += lp(noise(r, n), 5000) * expdec(t, 0.003) * 0.8
    y += np.sin(2 * np.pi * 950 * t) * expdec(t, 0.02) * 0.5
    y = lp(y, 6000)
    return fade(y, 1, 20)


# -------------------------------------------------------------------- main


def to_int16(x):
    return (np.clip(x, -1, 1) * 32767).astype(np.int16)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        for path, (fn, kind) in SOUNDS.items():
            r = rng_for(path)
            x = fn(r)
            if kind == "peak":
                x = peak_norm(x, -1.0)
            elif kind == "stereo":
                x = rms_norm(x, -16.0)
            else:
                x = rms_norm(x, -12.0)
            if kind not in ("peak",):
                # guarantee fade at both ends (loop files already crossfaded)
                if path not in ("ambient/drone", "reclaimer/breath"):
                    x = fade(x, 5, 5) if x.ndim == 1 else x
                else:
                    x = fade(x, 0, 0) if x.ndim == 1 else x
            wav = Path(tmp) / (path.replace("/", "_") + ".wav")
            dst = OUT / (path + ".ogg")
            dst.parent.mkdir(parents=True, exist_ok=True)
            # Encode, then decode and verify the Vorbis overshoot stays under
            # -1 dBFS; if not, scale down and re-encode.
            for _ in range(4):
                wavfile.write(wav, SR, to_int16(x))
                subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav),
                                "-c:a", "libvorbis", "-q:a", "5", str(dst)], check=True)
                raw = subprocess.run(["ffmpeg", "-v", "error", "-i", str(dst), "-f", "f32le", "-"],
                                     capture_output=True, check=True).stdout
                pk = np.max(np.abs(np.frombuffer(raw, np.float32)))
                if pk <= 10 ** (-0.95 / 20):
                    break
                x = x * 10 ** (-1.0 / 20) / pk
            print(f"{path}: {len(x) / SR:.2f}s")


if __name__ == "__main__":
    main()
