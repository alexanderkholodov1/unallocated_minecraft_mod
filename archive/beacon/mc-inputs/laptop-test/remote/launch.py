#!/usr/bin/env python3
"""Minimal headless stand-in for the official Minecraft Launcher (stdlib only).

Reads the profile the Fabric installer wrote (versions/<id>/<id>.json), fetches the vanilla
parent version, libraries and assets from Mojang/Fabric exactly as the launcher would, builds
the command line from the version JSON's own `arguments`, and starts the game with an offline
user. It does nothing mod-specific.

    launch.py prepare --game-dir DIR --version-id ID      download everything, do not start
    launch.py run     --game-dir DIR --version-id ID --java JAVA [--width W --height H]
"""
import argparse
import concurrent.futures
import hashlib
import json
import os
import platform
import subprocess
import sys
import time
import urllib.request
import uuid
from pathlib import Path

MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
ASSET_URL = "https://resources.download.minecraft.net/"
FEATURES = {"has_custom_resolution": True}


def log(msg):
    print(f"[launch {time.strftime('%H:%M:%SZ', time.gmtime())}] {msg}", flush=True)


def fetch(url, dest, sha1=None):
    dest = Path(dest)
    if dest.is_file() and (sha1 is None or hashlib.sha1(dest.read_bytes()).hexdigest() == sha1):
        return False
    dest.parent.mkdir(parents=True, exist_ok=True)
    last = None
    for attempt in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "unallocated-cleantest"}), timeout=60) as r:
                data = r.read()
            if sha1 and hashlib.sha1(data).hexdigest() != sha1:
                raise IOError(f"sha1 mismatch for {url}")
            tmp = dest.with_name(dest.name + ".part")
            tmp.write_bytes(data)
            tmp.replace(dest)
            return True
        except Exception as e:  # retry transient failures
            last = e
            time.sleep(1 + attempt)
    raise SystemExit(f"download failed: {url}: {last}")


def get_json(url):
    with urllib.request.urlopen(url, timeout=60) as r:
        return json.load(r)


def rules_allow(rules):
    if not rules:
        return True
    allowed = False
    for rule in rules:
        match = True
        os_rule = rule.get("os")
        if os_rule:
            if "name" in os_rule and os_rule["name"] != "linux":
                match = False
            if "arch" in os_rule and os_rule["arch"] != "x86_64" and platform.machine() == "x86_64":
                match = False
            if "version" in os_rule:
                match = False
        for feature, wanted in (rule.get("features") or {}).items():
            if FEATURES.get(feature, False) != wanted:
                match = False
        if match:
            allowed = rule["action"] == "allow"
    return allowed


def library_path(lib):
    art = lib.get("downloads", {}).get("artifact")
    if art:
        return art["path"], art["url"], art.get("sha1")
    group, name, version, *classifier = lib["name"].split(":")
    rel = f"{group.replace('.', '/')}/{name}/{version}/{name}-{version}" + (f"-{classifier[0]}" if classifier else "") + ".jar"
    return rel, lib.get("url", "https://libraries.minecraft.net/") + rel, lib.get("sha1")


def load_version(game_dir, version_id):
    path = game_dir / "versions" / version_id / f"{version_id}.json"
    if not path.is_file():
        log(f"{version_id}.json not present; fetching from Mojang manifest")
        entry = next(v for v in get_json(MANIFEST)["versions"] if v["id"] == version_id)
        fetch(entry["url"], path)
    data = json.loads(path.read_text())
    parent = data.get("inheritsFrom")
    if not parent:
        return data
    base = load_version(game_dir, parent)
    merged = dict(base)
    merged.update({k: v for k, v in data.items() if k not in ("libraries", "arguments", "inheritsFrom")})
    merged["libraries"] = data.get("libraries", []) + base.get("libraries", [])
    merged["arguments"] = {
        "jvm": base.get("arguments", {}).get("jvm", []) + data.get("arguments", {}).get("jvm", []),
        "game": base.get("arguments", {}).get("game", []) + data.get("arguments", {}).get("game", []),
    }
    merged["_jar_id"] = base.get("_jar_id", parent)
    return merged


def prepare(game_dir, version_id):
    version = load_version(game_dir, version_id)
    jar_id = version.get("_jar_id", version_id)
    client = version["downloads"]["client"]
    fetch(client["url"], game_dir / "versions" / jar_id / f"{jar_id}.jar", client["sha1"])
    jobs = []
    classpath = []
    for lib in version["libraries"]:
        if not rules_allow(lib.get("rules")):
            continue
        rel, url, sha1 = library_path(lib)
        jobs.append((url, game_dir / "libraries" / rel, sha1))
        if str(game_dir / "libraries" / rel) not in classpath:
            classpath.append(str(game_dir / "libraries" / rel))
    index_info = version["assetIndex"]
    index_path = game_dir / "assets" / "indexes" / f"{index_info['id']}.json"
    fetch(index_info["url"], index_path, index_info["sha1"])
    objects = json.loads(index_path.read_text())["objects"]
    for entry in objects.values():
        h = entry["hash"]
        jobs.append((f"{ASSET_URL}{h[:2]}/{h}", game_dir / "assets" / "objects" / h[:2] / h, h))
    log(f"{len(jobs)} libraries+assets to verify/download")
    start = time.time()
    done = 0
    with concurrent.futures.ThreadPoolExecutor(16) as pool:
        for fetched in pool.map(lambda j: fetch(*j), jobs):
            done += fetched
    log(f"downloaded {done} files in {time.time() - start:.0f}s")
    classpath.append(str(game_dir / "versions" / jar_id / f"{jar_id}.jar"))
    return version, classpath


def expand(arguments):
    out = []
    for arg in arguments:
        if isinstance(arg, str):
            out.append(arg)
        elif rules_allow(arg.get("rules")):
            value = arg["value"]
            out.extend([value] if isinstance(value, str) else value)
    return out


def main():
    p = argparse.ArgumentParser()
    p.add_argument("mode", choices=("prepare", "run"))
    p.add_argument("--game-dir", required=True)
    p.add_argument("--version-id", required=True)
    p.add_argument("--java")
    p.add_argument("--width", default="854")
    p.add_argument("--height", default="480")
    p.add_argument("--user", default="JudgeTest")
    p.add_argument("--xmx", default="2G")
    a = p.parse_args()
    game_dir = Path(a.game_dir).resolve()
    version, classpath = prepare(game_dir, a.version_id)
    if a.mode == "prepare":
        return 0
    natives = game_dir / "versions" / a.version_id / "natives"
    natives.mkdir(parents=True, exist_ok=True)
    offline = bytearray(hashlib.md5(f"OfflinePlayer:{a.user}".encode()).digest())
    offline[6] = offline[6] & 0x0F | 0x30
    offline[8] = offline[8] & 0x3F | 0x80
    subst = {
        "auth_player_name": a.user, "version_name": a.version_id, "game_directory": str(game_dir),
        "assets_root": str(game_dir / "assets"), "assets_index_name": version["assetIndex"]["id"],
        "auth_uuid": str(uuid.UUID(bytes=bytes(offline))), "auth_access_token": "0", "clientid": "0",
        "auth_xuid": "0", "user_type": "msa", "version_type": version.get("type", "release"),
        "natives_directory": str(natives), "launcher_name": "unallocated-cleantest", "launcher_version": "1",
        "classpath": os.pathsep.join(classpath), "classpath_separator": os.pathsep,
        "library_directory": str(game_dir / "libraries"), "resolution_width": a.width, "resolution_height": a.height,
    }

    def sub(arg):
        for key, value in subst.items():
            arg = arg.replace("${" + key + "}", value)
        return arg
    jvm = [sub(x) for x in expand(version["arguments"]["jvm"])]
    game = [sub(x) for x in expand(version["arguments"]["game"])]
    cmd = [a.java, f"-Xmx{a.xmx}", *jvm, version["mainClass"], *game]
    log("java: " + a.java)
    log("version: " + a.version_id + " mainClass=" + version["mainClass"])
    (game_dir / "launch-command.txt").write_text("\n".join(cmd) + "\n")
    os.chdir(game_dir)
    os.execv(a.java, cmd)


if __name__ == "__main__":
    sys.exit(main())
