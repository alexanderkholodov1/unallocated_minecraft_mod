#!/usr/bin/env python3
"""Tiny X11 driver (ctypes: libX11 + libXtst, no extra packages).

    xdrive.py shot FILE.png          screenshot of the whole root window
    xdrive.py stats                  print "mean=<0-255> hash=<md5>" of the root window
    xdrive.py move X Y
    xdrive.py click X Y [button]     move, short pause, press, release
    xdrive.py type TEXT              type TEXT with real key events
    xdrive.py key NAME               e.g. Return, Escape, F2
    xdrive.py pixel X Y              print "R G B" at that point
    xdrive.py size                   print "<w>x<h>"
    xdrive.py close                  ask every top-level window to close (WM_DELETE_WINDOW)
"""
import ctypes
import ctypes.util
import hashlib
import struct
import sys
import time
import zlib

x11 = ctypes.CDLL(ctypes.util.find_library("X11") or "libX11.so.6")
xtst = ctypes.CDLL(ctypes.util.find_library("Xtst") or "libXtst.so.6")

x11.XOpenDisplay.restype = ctypes.c_void_p
x11.XOpenDisplay.argtypes = [ctypes.c_char_p]
x11.XDefaultRootWindow.restype = ctypes.c_ulong
x11.XDefaultRootWindow.argtypes = [ctypes.c_void_p]
x11.XDisplayWidth.argtypes = [ctypes.c_void_p, ctypes.c_int]
x11.XDisplayHeight.argtypes = [ctypes.c_void_p, ctypes.c_int]
x11.XStringToKeysym.restype = ctypes.c_ulong
x11.XStringToKeysym.argtypes = [ctypes.c_char_p]
x11.XKeysymToKeycode.restype = ctypes.c_ubyte
x11.XKeysymToKeycode.argtypes = [ctypes.c_void_p, ctypes.c_ulong]
x11.XFlush.argtypes = [ctypes.c_void_p]
x11.XSync.argtypes = [ctypes.c_void_p, ctypes.c_int]
xtst.XTestFakeKeyEvent.argtypes = [ctypes.c_void_p, ctypes.c_uint, ctypes.c_int, ctypes.c_ulong]
xtst.XTestFakeButtonEvent.argtypes = [ctypes.c_void_p, ctypes.c_uint, ctypes.c_int, ctypes.c_ulong]
xtst.XTestFakeMotionEvent.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_int, ctypes.c_int, ctypes.c_ulong]


class XImage(ctypes.Structure):
    _fields_ = [("width", ctypes.c_int), ("height", ctypes.c_int), ("xoffset", ctypes.c_int),
                ("format", ctypes.c_int), ("data", ctypes.c_void_p), ("byte_order", ctypes.c_int),
                ("bitmap_unit", ctypes.c_int), ("bitmap_bit_order", ctypes.c_int),
                ("bitmap_pad", ctypes.c_int), ("depth", ctypes.c_int),
                ("bytes_per_line", ctypes.c_int), ("bits_per_pixel", ctypes.c_int)]


x11.XGetImage.restype = ctypes.POINTER(XImage)
x11.XGetImage.argtypes = [ctypes.c_void_p, ctypes.c_ulong, ctypes.c_int, ctypes.c_int,
                          ctypes.c_uint, ctypes.c_uint, ctypes.c_ulong, ctypes.c_int]

dpy = x11.XOpenDisplay(None)
if not dpy:
    sys.exit("xdrive: cannot open display")
root = x11.XDefaultRootWindow(dpy)
W, H = x11.XDisplayWidth(dpy, 0), x11.XDisplayHeight(dpy, 0)

SHIFT = "Shift_L"
SPECIAL = {" ": "space", "-": "minus", "/": "slash", ".": "period", ",": "comma", ":": "colon"}


def rgb_frame():
    img = x11.XGetImage(dpy, root, 0, 0, W, H, 0xFFFFFFFF, 2)  # ZPixmap
    if not img:
        sys.exit("xdrive: XGetImage failed")
    im = img.contents
    raw = ctypes.string_at(im.data, im.bytes_per_line * H)
    if im.bits_per_pixel != 32:
        sys.exit(f"xdrive: unsupported depth {im.bits_per_pixel}")
    rgb = bytearray(W * H * 3)
    if im.bytes_per_line == W * 4:
        rgb[0::3], rgb[1::3], rgb[2::3] = raw[2::4], raw[1::4], raw[0::4]
    else:
        for y in range(H):
            row = raw[y * im.bytes_per_line:y * im.bytes_per_line + W * 4]
            base = y * W * 3
            rgb[base:base + W * 3:3], rgb[base + 1:base + W * 3:3], rgb[base + 2:base + W * 3:3] = row[2::4], row[1::4], row[0::4]
    return bytes(rgb)


def write_png(path, rgb):
    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)
    stride = W * 3
    rows = b"".join(b"\x00" + rgb[y * stride:(y + 1) * stride] for y in range(H))
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 2, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(rows, 6)) + chunk(b"IEND", b""))


def keycode(name):
    sym = x11.XStringToKeysym(name.encode())
    code = x11.XKeysymToKeycode(dpy, sym) if sym else 0
    if not code:
        sys.exit(f"xdrive: no keycode for {name!r}")
    return code


def tap(name, shift=False):
    if shift:
        xtst.XTestFakeKeyEvent(dpy, keycode(SHIFT), 1, 0)
    code = keycode(name)
    xtst.XTestFakeKeyEvent(dpy, code, 1, 0)
    x11.XSync(dpy, 0)
    time.sleep(0.05)
    xtst.XTestFakeKeyEvent(dpy, code, 0, 0)
    if shift:
        xtst.XTestFakeKeyEvent(dpy, keycode(SHIFT), 0, 0)
    x11.XSync(dpy, 0)
    time.sleep(0.08)


def move(x, y):
    xtst.XTestFakeMotionEvent(dpy, 0, x, y, 0)
    x11.XSync(dpy, 0)


class ClientMessage(ctypes.Structure):
    _fields_ = [("type", ctypes.c_int), ("serial", ctypes.c_ulong), ("send_event", ctypes.c_int),
                ("display", ctypes.c_void_p), ("window", ctypes.c_ulong), ("message_type", ctypes.c_ulong),
                ("format", ctypes.c_int), ("data", ctypes.c_long * 5)]


def close_windows():
    x11.XInternAtom.restype = ctypes.c_ulong
    x11.XInternAtom.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_int]
    protocols, delete = x11.XInternAtom(dpy, b"WM_PROTOCOLS", 0), x11.XInternAtom(dpy, b"WM_DELETE_WINDOW", 0)
    root_ret, parent_ret, n = ctypes.c_ulong(), ctypes.c_ulong(), ctypes.c_uint()
    children = ctypes.POINTER(ctypes.c_ulong)()
    x11.XQueryTree.argtypes = [ctypes.c_void_p, ctypes.c_ulong, ctypes.c_void_p, ctypes.c_void_p,
                               ctypes.c_void_p, ctypes.c_void_p]
    x11.XQueryTree(dpy, root, ctypes.byref(root_ret), ctypes.byref(parent_ret), ctypes.byref(children), ctypes.byref(n))
    x11.XSendEvent.argtypes = [ctypes.c_void_p, ctypes.c_ulong, ctypes.c_int, ctypes.c_long, ctypes.c_void_p]
    for i in range(n.value):
        event = ClientMessage(type=33, send_event=1, display=dpy, window=children[i],  # ClientMessage
                              message_type=protocols, format=32)
        event.data[0], event.data[1] = delete, 0
        x11.XSendEvent(dpy, children[i], 0, 0, ctypes.byref(event))
    x11.XSync(dpy, 0)
    print(f"close sent to {n.value} window(s)")


def main(argv):
    cmd = argv[1] if len(argv) > 1 else ""
    if cmd == "shot":
        write_png(argv[2], rgb_frame())
    elif cmd == "stats":
        frame = rgb_frame()
        print(f"mean={sum(frame[::97]) / len(frame[::97]):.1f} hash={hashlib.md5(frame).hexdigest()}")
    elif cmd == "size":
        print(f"{W}x{H}")
    elif cmd == "pixel":
        frame, x, y = rgb_frame(), int(argv[2]), int(argv[3])
        o = (y * W + x) * 3
        print(frame[o], frame[o + 1], frame[o + 2])
    elif cmd == "close":
        close_windows()
    elif cmd == "move":
        move(int(argv[2]), int(argv[3]))
    elif cmd == "click":
        x, y, button = int(argv[2]), int(argv[3]), int(argv[4]) if len(argv) > 4 else 1
        move(x, y)
        time.sleep(0.3)
        move(x + 1, y)
        time.sleep(0.2)
        move(x, y)
        time.sleep(0.2)
        xtst.XTestFakeButtonEvent(dpy, button, 1, 0)
        x11.XSync(dpy, 0)
        time.sleep(0.12)
        xtst.XTestFakeButtonEvent(dpy, button, 0, 0)
        x11.XSync(dpy, 0)
    elif cmd == "type":
        for ch in argv[2]:
            if ch.isalpha() and ch.isascii():
                tap(ch.lower(), shift=ch.isupper())
            elif ch.isdigit():
                tap(ch)
            elif ch in SPECIAL:
                tap(SPECIAL[ch])
            else:
                sys.exit(f"xdrive: cannot type {ch!r}")
    elif cmd == "key":
        tap(argv[2])
    else:
        sys.exit(__doc__)
    x11.XFlush(dpy)


if __name__ == "__main__":
    main(sys.argv)
