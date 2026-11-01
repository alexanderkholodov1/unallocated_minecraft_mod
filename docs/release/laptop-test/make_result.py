#!/usr/bin/env python3
"""Write RESULT.md from the evidence the laptop run copied back. Usage: make_result.py RESULTS_DIR DIST_DIR RUN STAGE"""
import re
import sys
from pathlib import Path

results, dist, run, stage = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3], sys.argv[4]


def read(name, default=""):
    p = results / name
    return p.read_text(encoding="utf-8", errors="replace").strip() if p.is_file() else default


verdict = read("verdict.txt", "NONE (harness did not reach the verdict; see console.log)")
log = read("latest.log")
harness = read("harness.log")
page = (dist / "itch-page.md").read_text(encoding="utf-8") if (dist / "itch-page.md").is_file() else ""


def grab(pattern, text, default="?"):
    m = re.search(pattern, text, re.M)
    return m.group(1) if m else default


timings = {}
for line in read("timings.txt").splitlines():
    key, _, value = line.partition(" ")
    timings[key] = int(value)
order = ["sha_ok", "java_ready", "fabric_installed", "vanilla_ready", "mod_copied", "client_initialized",
         "contract_accepted", "button_clicked", "chapter_entered", "held_60s", "quit_clean"]
timing_rows = "\n".join(f"| {k} | {timings[k]} s |" for k in order if k in timings)
chapter = grab(r"(UNALLOCATED story chapter=\S+ slot=LOBBY enter)", log, "(none)")
pngs = sorted(p.name for p in results.glob("*.png") if not p.name.startswith("seq-"))
failures = read("failures.txt")
backend = grab(r"(Using graphics backend [^\n]*)", log, "unknown")
jar_name = grab(r"^\| `([^`]+\.jar)` \| El mod", page)
jar_hash = grab(r"SHA-256: `([0-9a-f]{64})`", page)
mc_ver = grab(r"exactamente (\S+?)\*\*", page)
loader_req = grab(r"Fabric Loader (\S+) o posterior", page)
loader_log = grab(r"(Loading Minecraft [^\n]*Fabric Loader [\d.]+)", log)
installer_ver = grab(r"instalador oficial de Fabric\]\([^)]*\) \*\*([\d.]+)\*\*", page)
java_page = grab(r"\*\*Java (\d+)\*\*", page)

out = f"""# RESULT: {verdict}

Run `{run}` (stage `{stage}`), laptop WSL Ubuntu, {read("java-version.txt").splitlines()[0] if read("java-version.txt") else "java ?"}.

## Versions used
| Item | Value |
| --- | --- |
| Mod JAR | `{jar_name}` sha256 `{jar_hash}` (verified against the page) |
| Minecraft (page: "exactamente") | {mc_ver} |
| Fabric Loader (page: "o posterior") | requested {loader_req}; log: {loader_log} |
| Fabric installer (page) | {installer_ver} |
| Java | page says Java {java_page}; used Temurin from `api.adoptium.net` (see java-version.txt) |
| Profile id written by installer | {read("version-id.txt", "?")} |

## Checks
```
{read("checks.txt", "(no checks file)")}
```
First chapter entry in the log: `{chapter}`

Page claims / log problems:
```
{read("extra-checks.txt", "(none)")}
```
{("Failures recorded by the harness:" + chr(10) + "```" + chr(10) + failures + chr(10) + "```") if failures else ""}

## Timings (seconds since harness start)
| Step | t |
| --- | --- |
{timing_rows}

## Substitutions (what a judge on Windows/macOS would do instead)
1. **Launcher.** The official Minecraft Launcher cannot run headless in WSL. `remote/launch.py` (stdlib Python, ~150 lines) reads the
   profile JSON the **official Fabric installer** wrote, downloads the vanilla `{mc_ver}` client, libraries and assets from Mojang
   (`piston-meta`, `libraries.minecraft.net`, `resources.download.minecraft.net`) and Fabric's maven, builds the command line from
   the version JSON's own `arguments` (JVM args, classpath, main class `KnotClient`) and starts the game with an offline user `JudgeTest`.
   It is the smallest faithful substitute: same profile, same arguments, no mod-specific behaviour.
2. **Page step 1** (create a vanilla installation and start it once to the main menu) is not performed; the launcher substitute downloads the same
   vanilla files, but vanilla is never started on its own. A `launcher_profiles.json` was {"created by the harness because the installer refused without it" if "launcher_profiles.json created" in read("substitutions.txt") else "not needed: the installer ran on an empty directory"}.
3. **Installer download:** from `maven.fabricmc.net` (the artifact behind fabricmc.net/use/installer), version taken from the page; run with
   `java -jar fabric-installer-X.jar client -dir <clean dir> -mcversion .. -loader ..` (the CLI form of the Client tab; Create Profile left on).
4. **Java:** Temurin 25 tarball (the page's Linux row) instead of the launcher's bundled runtime. WSL's system Java is 21 and unused.
5. **Display/GL/audio:** Xvfb 854x480 (the launcher's default window size), WSLg not used so nothing appears on the owner's screen. Renderer: {backend}. OpenGL fails under Xvfb
   ("Couldn't find matching GLX visual"), so Minecraft 26.3 itself fell back to its Vulkan backend on Mesa llvmpipe; OpenAL
   runs on its `null` device (`ALSOFT_DRIVERS=null`) so the audio code runs without making noise. Real GPU and real audio drivers are **not** exercised.
6. **Input:** XTest key/mouse events (`remote/xdrive.py`, ctypes) at fixed coordinates derived from GUI scale; screenshots read via XGetImage.
7. **Shared download cache:** `libraries/` and `assets/` of the game directory are symlinks to `~/unallocated-cleantest/cache/mc-shared/`
   (re-download avoided); `mods/`, `config/`, `saves/`, `logs/`, `unallocated/`, `options.txt` are fresh per run.
8. **First-run welcome screen.** A profile that never ran vanilla shows Minecraft's Narrator/accessibility welcome before the mod's contract; the harness clicks its Continue
   button (page step 1 has the judge do this in vanilla). Screenshots `00-first-run-welcome-*.png`.
9. **Quit:** a close request (`WM_DELETE_WINDOW`) to the Minecraft window; clean = process exits on its own: **{read("exit-kind.txt", "n/a")}**.

## Evidence
PNGs: {", ".join(pngs) or "(none)"}  (seq-*.png = loading-screen burst after the click)
Logs: latest.log, game.stdout, harness.log, fabric-installer.log, launch-command via the laptop run dir. Crash reports: {read("crash-report-names.txt", "none")}.
"""
(results / "RESULT.md").write_text(out, encoding="utf-8")
print(f"wrote {results / 'RESULT.md'}")
