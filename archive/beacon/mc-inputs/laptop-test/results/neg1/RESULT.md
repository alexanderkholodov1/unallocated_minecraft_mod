# RESULT: FAIL

Run `neg1` (stage `play`), laptop WSL Ubuntu, openjdk version "25.0.5" 2026-10-20 LTS.

## Versions used
| Item | Value |
| --- | --- |
| Mod JAR | `unallocated-0.2.0.jar` sha256 `45dfa7afe5bb34701b4f1bf8391b2bef7d0f78e7eda0bd6dc89abae8a715a29c` (verified against the page) |
| Minecraft (page: "exactamente") | 26.3 |
| Fabric Loader (page: "o posterior") | requested 0.20.1; log: Loading Minecraft 26.3 with Fabric Loader 0.20.1 |
| Fabric installer (page) | 1.1.2 |
| Java | page says Java 25; used Temurin from `api.adoptium.net` (see java-version.txt) |
| Profile id written by installer | fabric-loader-0.20.1-26.3 |

## Checks
```
FAIL  log: initialized (common)
FAIL  log: initialized (client)
FAIL  log: story loaded
FAIL  log: chapter entry slot=LOBBY
PASS  no 'story invalid'
PASS  no crash report
PASS  game ran 60 s in-world
PASS  clean quit
```
First chapter entry in the log: `(none)`

Page claims / log problems:
```
page claim NOT met: unallocated/contract.properties missing
world check failed: [Errno 2] No such file or directory: '/home/alexamaze/unallocated-cleantest/neg1/mc/saves/UNALLOCATED/level.dat'
installer profile name(s): ['fabric-loader-26.3']
ERROR line (x count):       1 [21:17:37] [Download-2/ERROR]: Failed to fetch user properties
ERROR line (x count):       1 [21:17:37] [Render thread/ERROR]: Failed to create backend OpenGL
ERROR line (x count):       1 [21:17:38] [Download-1/ERROR]: Failed to fetch Realms feature flags
ERROR line (x count):       1 [21:17:38] [Render thread/ERROR]: Error while loading the narrator
ERROR line (x count):       1 [21:21:39] [Download-3/ERROR]: Couldn't connect to realms
ERROR line (x count):       1 [21:21:39] [Render thread/ERROR]: Error while loading the narrator
```
Failures recorded by the harness:
```
client never initialized (rc=1)
contract screen not detected (background pixel is 49 35 31
no LOBBY chapter entry (rc=1)
```

## Timings (seconds since harness start)
| Step | t |
| --- | --- |
| sha_ok | 0 s |
| java_ready | 0 s |
| fabric_installed | 3 s |
| vanilla_ready | 13 s |
| mod_copied | 13 s |
| contract_accepted | 365 s |
| button_clicked | 372 s |
| held_60s | 625 s |
| quit_clean | 627 s |

## Substitutions (what a judge on Windows/macOS would do instead)
1. **Launcher.** The official Minecraft Launcher cannot run headless in WSL. `remote/launch.py` (stdlib Python, ~150 lines) reads the
   profile JSON the **official Fabric installer** wrote, downloads the vanilla `26.3` client, libraries and assets from Mojang
   (`piston-meta`, `libraries.minecraft.net`, `resources.download.minecraft.net`) and Fabric's maven, builds the command line from
   the version JSON's own `arguments` (JVM args, classpath, main class `KnotClient`) and starts the game with an offline user `JudgeTest`.
   It is the smallest faithful substitute: same profile, same arguments, no mod-specific behaviour.
2. **Page step 1** (create a vanilla installation and start it once to the main menu) is not performed; the launcher substitute downloads the same
   vanilla files, but vanilla is never started on its own. A `launcher_profiles.json` was created by the harness because the installer refused without it.
3. **Installer download:** from `maven.fabricmc.net` (the artifact behind fabricmc.net/use/installer), version taken from the page; run with
   `java -jar fabric-installer-X.jar client -dir <clean dir> -mcversion .. -loader ..` (the CLI form of the Client tab; Create Profile left on).
4. **Java:** Temurin 25 tarball (the page's Linux row) instead of the launcher's bundled runtime. WSL's system Java is 21 and unused.
5. **Display/GL/audio:** Xvfb 854x480 (the launcher's default window size), WSLg not used so nothing appears on the owner's screen. Renderer: Using graphics backend Vulkan, using drivers: 1.4.318 llvmpipe Mesa 25.2.8-0ubuntu0.24.04.4 (LLVM 20.1.2). OpenGL fails under Xvfb
   ("Couldn't find matching GLX visual"), so Minecraft 26.3 itself fell back to its Vulkan backend on Mesa llvmpipe; OpenAL
   runs on its `null` device (`ALSOFT_DRIVERS=null`) so the audio code runs without making noise. Real GPU and real audio drivers are **not** exercised.
6. **Input:** XTest key/mouse events (`remote/xdrive.py`, ctypes) at fixed coordinates derived from GUI scale; screenshots read via XGetImage.
7. **Shared download cache:** `libraries/` and `assets/` of the game directory are symlinks to `~/unallocated-cleantest/cache/mc-shared/`
   (re-download avoided); `mods/`, `config/`, `saves/`, `logs/`, `unallocated/`, `options.txt` are fresh per run.
8. **First-run welcome screen.** A profile that never ran vanilla shows Minecraft's Narrator/accessibility welcome before the mod's contract; the harness clicks its Continue
   button (page step 1 has the judge do this in vanilla). Screenshots `00-first-run-welcome-*.png`.
9. **Quit:** a close request (`WM_DELETE_WINDOW`) to the Minecraft window; clean = process exits on its own: **clean**.

## Evidence
PNGs: 00-first-run-welcome-1.png, 00-first-run-welcome-2.png, 00-first-run-welcome-3.png, 01-contract.png, 01b-contract-typed.png, 02-title-button.png, 03-connecting.png, 03b-loading.png, 04-in-world.png, 05-in-world-60s.png  (seq-*.png = loading-screen burst after the click)
Logs: latest.log, game.stdout, harness.log, fabric-installer.log, launch-command via the laptop run dir. Crash reports: .
