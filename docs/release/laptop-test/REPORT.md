# UNALLOCATED clean-install test: report

Written 2026-10-29 (UTC) by the laptop-test worker. Harness proven on build `9498b53` (run 40700637219, `unallocated-0.2.0.jar`, sha256 `45dfa7af…a29c`).

## Result on the current build: PASS (three consecutive clean runs)

| Run | Verdict | Total | Click → chapter entry | Notes |
| --- | --- | --- | --- | --- |
| `dev2` | PASS | 203 s | 9 s | first PASS (warm download cache) |
| `dev3` | PASS | 206 s | 8 s | repeat, fresh run directory |
| `dev4` | PASS | 164 s | 8 s | final harness incl. world and profile checks (client init at 34 s, in-world hold done at 160 s) |

Cold download cache (first ever run): about 160 s extra (5,220 library and asset files). Every run uses a fresh `~/unallocated-cleantest/run-*/` directory; `mods/`, `config/`, `saves/`, `logs/` and `unallocated/` are always new.

In every passing run the log contains: `UNALLOCATED initialized (common).`, `UNALLOCATED initialized (client).`, `UNALLOCATED story loaded chapters=1 beats=0 endings=1 format=1`, `UNALLOCATED story chapter=free_roam slot=LOBBY enter`. There is no crash report and no `story invalid`. The game ran 60 s in-world, then closed cleanly on a window-close request (exit status 0, log `Stopping!`). The in-world screenshot at 60 s shows the first-person lobby and the HUD, with no pause menu.

Things I confirmed that the page promises: the contract screen, typing `De acuerdo` into the focused field, the **UNALLOCATED** button on the title screen, the "Conectando al servidor…" screen, `unallocated/contract.properties` is written, `saves/UNALLOCATED` is created with GameType 2 (Adventure) and `allowCommands` 0, and the installer's profile gets Loader 0.20.1.

## Negative control
`results/neg1/`: same install, but the mod JAR removed from `mods/` before the play stage. The harness reported **FAIL** (exit 1): no `initialized`, `story loaded` or chapter lines, no contract screen detected. It took 627 s, because the timeouts are long (240 s client init, 180 s chapter entry). That run also printed `PASS game ran 60 s in-world` while never reaching the world. I then changed the check so it requires a chapter entry as well; that edit is a one-line change that I syntax-checked (`bash -n`) but did not rerun.

## How to run it (from Beacon; nothing heavy runs here)

```
python3 -I ~/Workspaces/mc-hito/scripts/release_bundle.py <artifact-dir> --out <dist>   # see "release_bundle.py blocker" below
~/Workspaces/mc-inputs/laptop-test/laptop-install-test.sh <dist>            # ~3.5 min; exit 0 PASS, 1 FAIL, 3 harness error
```
Options: `--run NAME` (default `run-<UTC>`), `--stage install|play|all`. `<dist>` needs the JAR, its `.sha256`, `UNALLOCATED-world.zip` and `itch-page.md`. The script reads **everything it tests from the page**: Minecraft version, Loader, Fabric installer version, Java major, JAR name and SHA-256. A changed page changes the test. Results land in `~/Workspaces/mc-inputs/laptop-test/results/<run>/`.

Files: `laptop-install-test.sh` (Beacon driver), `remote/run-test.sh` (the laptop orchestration), `remote/launch.py` (launcher substitute), `remote/xdrive.py` (X11 input and screenshots, ctypes only), `make_result.py` (writes `RESULT.md`).

Only `~/unallocated-cleantest/` is touched on the laptop: `cache/` (Temurin 25, Fabric installer, shared libraries and assets), `tools/`, one directory per run. The laptop's other files are untouched.

## Evidence (current build)
- `results/dev4/RESULT.md` (pass/fail, versions, timings, every substitution), `latest.log`, `checks.txt`, `extra-checks.txt`, `game.stdout`, `harness.log`, `fabric-installer.log`, `launcher_profiles.json`.
- PNGs in `results/dev4/`: `00-first-run-welcome-1.png`, `01-contract.png`, `01b-contract-typed.png`, `02-title-button.png`, `03-connecting.png` ("Conectando al servidor…"), `03b-loading.png`, `04-in-world.png`, `05-in-world-60s.png`. `seq-*.png` is the loading-screen burst after the click.
- `results/dev2/` and `results/dev3/` are the earlier passing runs. The `dev1` runs were development runs; they failed because the first-run Minecraft welcome screen was not yet handled. `neg1` is the negative control (see below).

## release_bundle.py blocker (affects the lead's step 3)
`python3 scripts/release_bundle.py <artifact> --out dist` **exits 1** on the real CI artifact (run 40700637219):
`error: release.json field 'fabric_api_bundled' must be a non-empty string`.
CI writes `"fabric_api_bundled": true` (bool; `scripts/smoke_test.py:659`) but `release_bundle.py:83-86` requires a string, which it pastes into the page as `Fabric API {{FABRIC_API}}`. The unit test uses `"0.165.0+26.3"`, which is why tests pass. I did not touch the worktree. Workaround used here: a copy of the artifact (`artifact-staged/`) with that field set to `"0.165.0+26.3"` (the version in the JAR's `META-INF/NOTICE-Fabric-API.txt`); the bundle then exits 2 with only the five design tokens and `{{KNOWN_ISSUES}}` unfilled. The lead must fix this (script or CI) or hand-patch `release.json` before the real bundle step. I tried to message the lead session (`mc-hito-19`); the message was held and expired undelivered, so this is the first time they see it.

## What I substituted (full list is in each `RESULT.md`)
1. **Launcher:** `remote/launch.py` replaces the official Minecraft Launcher. It reads the profile JSON the official Fabric installer wrote, downloads vanilla 26.3, libraries and assets from Mojang and Fabric, builds the command line from the version JSON's own `arguments`, and starts `KnotClient` with an offline user. Reason: the launcher cannot run headless in WSL. `portablemc` was not needed.
2. **Page step 1** (start vanilla once to the main menu) is skipped. Consequences: the installer CLI refuses without `launcher_profiles.json` (the real launcher writes it), so the harness writes a minimal one; and Minecraft's Narrator/accessibility welcome screen appears before the contract, so the harness clicks Continue.
3. **Fabric installer** `1.1.2` is fetched from `maven.fabricmc.net` and run in CLI client mode (`client -dir -mcversion 26.3 -loader 0.20.1`, Create Profile left on), not the GUI.
4. **Java:** Temurin 25 from `api.adoptium.net`, cached; the system Java 21 is unused.
5. **Display, GL, audio:** Xvfb 854×480 (the launcher's default size). **OpenGL fails under Xvfb** (`Couldn't find matching GLX visual`) and Minecraft 26.3 itself falls back to its **Vulkan backend on Mesa llvmpipe** (software). OpenAL runs on its null device. So real GPU drivers, real audio and the OpenGL path are **not** covered. Narrator, Realms and authlib errors in the log are environment noise (offline user, no flite library).
6. **Input:** XTest events at coordinates derived from the GUI scale; screenshots via XGetImage. Positions assume the default 854×480 / GUI scale 2. If the UI layout of the contract or title screen changes, check `01b-contract-typed.png` and `02-title-button.png` first.
7. `libraries/` and `assets/` are symlinks to a shared cache; nothing mod-related is shared.

## Things in the itch page a judge may find wrong or unclear
1. **Profile name (step 6).** Quote: *"selecciona el perfil de **Fabric 26.3 / Loader 0.20.1 o posterior**."* The installer actually creates a profile named **`fabric-loader-26.3`** (`launcher_profiles.json`, `lastVersionId` `fabric-loader-0.20.1-26.3`). I found no profile with the name the page gives. Suggest: *"selecciona el perfil `fabric-loader-26.3`"*.
2. **Session length vs. the build.** Quote: *"Se recomienda una sesión de **10 a 15 minutos**"*. The log of this build has `WARN UNALLOCATED story warning story.json#/chapters: [session-length] Budget outside 600–900 seconds`, and `story loaded chapters=1 beats=0 endings=1`: one chapter (`free_roam`) and no beats. If that is the intended content for this build, fine; if not, the lead should look. I only tested to the first chapter.
3. **Contract wording.** Quote: *"pulsa **De acuerdo** para entrar al menú."* This is accurate (the button label is `De acuerdo`, and the input hint says *"Pulsa el botón, o escríbelo y pulsa Enter."*). I tested typing the phrase and then clicking the button. I did not test other paths (Enter only, empty field), so I have no finding here.
4. **First launch of vanilla (step 1)** will show Minecraft's own Narrator/accessibility welcome. The page does not mention it; judges know it, so this is low risk.
5. **Linux path.** Quote: *"Desde la carpeta de descarga ejecuta `java -jar fabric-installer-1.1.2.jar`."* This works (installer 1.1.2 exists on `maven.fabricmc.net` and ran fine under Temurin 25). With no display it only works through the CLI form; the GUI form was not tested here.
6. **Links.** `fabricmc.net/use/installer/` and `adoptium.net/temurin/releases/?version=25` were not opened in a browser. I only confirmed that the artifacts they point to exist (`api.adoptium.net` serves Temurin 25.0.5; the installer JAR 1.1.2 resolves).

## Limits of this test
- One machine (WSL on the owner's laptop), software Vulkan, null audio. It cannot show GPU or driver trouble, OpenGL-only machines, macOS or Windows path problems, or the real launcher flow.
- It stops after the first chapter's entry plus 60 s of idle in-world time. It does not play to an ending, and it does not test the `UNALLOCATED-world.zip` fallback (checklist step 10 still wants both on a real PC).
- `beats=0` means the beat logic was not exercised here.
