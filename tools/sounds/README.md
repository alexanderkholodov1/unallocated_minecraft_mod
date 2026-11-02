# UNALLOCATED sound synthesis

All sounds are synthesized from scratch in `synth.py` (numpy/scipy, fixed per-file seeds). Run `python tools/sounds/synth.py`; needs ffmpeg with libvorbis. Output: `src/main/resources/assets/unallocated/sounds/<path>.ogg`.

- ambient/drone (stereo, 40 s, loops): 50 Hz mains hum with 7 harmonics and random frequency wobble, three detuned saw+sine pads beating slowly, lowpassed slow-modulated noise "air", faint stepped sine data chirps with echoes; tail crossfaded into head.
- ambient/distant_1: saw groan with falling/wobbling pitch, swept band-pass and noise resonance, drenched in a decaying-noise impulse-response reverb, darkened.
- ambient/distant_2: irregular low knocks (damped sine + noise thump) through a long noise-IR reverb, lowpassed.
- thread/vibrate_1..3: modal synthesis, stiff-string inharmonic partials plus extra non-integer partials, slight downward pitch bend, scraped band-passed noise; different pitch/inharmonicity each.
- thread/chime: detuned FM bells (non-integer mod ratio 3.51) with decaying index, soft attack, high-passed.
- thread/snap: high-passed noise transient + low click, then a fast down-sweeping twang with inharmonic overtone.
- fx/breath: band-pass noise with rising center frequency and squared swell envelope that cuts abruptly, followed by a low sine thump.
- fx/glitch_1..3: gated square beeps / gated noise+tone / falling sine+noise, bit-crushed, sample-and-hold decimated, and chopped into stuttering buffer repeats.
- screamer/impact: instant noise burst + dropping sub sine + detuned two-carrier FM screech with falling pitch, heavy tanh saturation.
- screamer/shriek: detuned saw stack with rising pitch glide and vibrato, a/i formant morph, heavy saturation, then a pitch crack and noise burst.
- reclaimer/click_1..4: bursts of short damped-sine/noise ticks at irregular intervals (resonant or dry, different bands); click_4 adds a hard-drive seek whirr.
- reclaimer/step_1..3: pitch-dropping sub thud + lowpassed noise, two bony resonant clicks, scattered tiny high debris ticks.
- reclaimer/breath (loops): noise through moving band-pass formants, inhale/exhale envelopes, wet amplitude-modulated crackle, low motor whine; crossfaded loop.
- reclaimer/roar: layered detuned saw stack with exponential pitch drop, a/o formant morph and noise, saturated, ending in a bit-crushed repeating-buffer stutter.
- reclaimer/whisper_1..3: noise through five random-weighted vowel formant sets changing ~14 times/s, breathy high band, syllable-rate amplitude modulation.
- ui/heartbeat: two pitch-dropping low sine thumps (lub-dub), lowpassed at 160 Hz.
- ui/terminal: short square blip + noise tick + low sine, lowpassed.
