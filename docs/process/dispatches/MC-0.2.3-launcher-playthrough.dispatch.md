*Translated from the original Spanish on 2026-11-01.*

**Captain's dispatch note (2026-10-28 07:05Z):** packages 0.2.1 (`dfffb3d`), 0.2.5 (`2a2ffb9`) and 0.2.2 (the story runtime, through `2867c3e`) are on `v0.2.0`; read `docs/story/RUNTIME.md`, which 0.2.2 wrote, before you start, and build on what it actually delivered (its markers, commands and the `story` CI phase). Codex quota is no longer scarce (weekly window reset tonight), so do this package completely and well, at the effort you are given. The commit relay and the rehearsal relay are running for `~/Workspaces/mc-hito`. `~/Workspaces/mc-ref/javacheck.sh ~/Workspaces/mc-hito` is authorized before each commit when `MemAvailable` is at least 900 MB and `pgrep -f "chrom|firestore-emulator"` prints nothing; otherwise rely on the rehearsal. A rehearsal takes 10-35 minutes; two tonight failed only on infrastructure (a dependency download "Read timed out", and a Gradle daemon that started and printed nothing for 35 minutes until the job timeout): if a rehearsal fails only that way, request it once more and say so.

**Also yours in this package, first (captain, 08:20Z), because it decides whether tomorrow's content night can trust CI:**
package 0.2.2 closed at `22164e9` (rehearsal 40654800731 passed all four jobs; its story evidence is real). But the
clean profile (`build (none)`) hung silently twice tonight: "Starting a Gradle Daemon", then no output until the
35-minute job timeout; Codex also found that this profile restores a different Gradle dependency archive than the
others. (a) Make every profile restore the same dependency cache where the inputs are the same, or say with evidence
why they cannot. (b) Give the Gradle step a no-output watchdog: after 12 minutes without a new output line, dump the
Gradle daemon's thread stacks (`jcmd <pid> Thread.print` for each Gradle JVM) into the diagnostics artifact and fail the
step, so a hang costs 12 minutes and leaves evidence. (c) If the stacks of a hang show its cause, fix it.


# Brief MC-0.2.3 — `launcher-playthrough`: the judge's path, built and proven

**Recommended lane:** Codex CLI, gpt-6.1-sol, effort **medium** (switch to high only if two rehearsals fail on the
same cause). Why: the work is well-bounded engineering on known APIs (title screen, world copy/open, CI scripting), its
interfaces are fixed by package 0.2.2, and quota must last for tomorrow. Budget about 60 minutes plus rehearsals.

## Who you are, where, for whom
You are a senior Minecraft 26.3 / Fabric engineer on UNALLOCATED (first-person horror mod for the USFQ jam "Terror en
Primera Persona", by Alexander Kholodov, `alexanderkholodov1`), dispatched by the Beacon captain, who verifies and
pushes. Worktree `~/Workspaces/mc-hito`, branch `v0.2.0`, with packages 0.2.1 and 0.2.2 integrated. Machine Beacon:
3.3 GB RAM. **Never run Gradle, Java builds or the game here; never write under `/tmp`.** Builds and game tests run only
in CI. Python checks run locally. Reference JARs for signatures: `~/Workspaces/mc-ref/` (read without running Java;
`javacheck.sh` only if the captain's dispatch authorizes it). English in code/docs; Spanish in player-facing text.

## Goal and why
The judges play at home on unknown machines; installation and stability are a judging criterion
(`docs/intent/Itch.io Competition Instructions.txt`, "Estabilidad y facilidad de instalación" [Stability and ease of installation]). The owner decided how
they start: **D-03 A** in `docs/plan/DECISIONS.md` (read D-03 and D-04 whole, with his "Respuesta" column): a
"UNALLOCATED" button on the title screen that copies the world template carried in our JAR to `saves/`, shows a
"connecting to the server" screen and opens the world, plus a world ZIP as fallback. You build that, and you prove the
whole judge path in CI on every commit: click the button on a clean client, play the **shipped story** by autoplay to
every ending it declares, and keep screenshots and a video for the itch page. After you, every content commit tomorrow
is played end to end automatically.

## Inputs
1. `docs/plan/milestones.md` §2 (hito acceptance), §3 choices 5–6, **§4.3 (your package, including the exact release
   artifact names package 5 consumes)**, §5 (shared files), §6.7 (`build`, `autoplay`), §10.
2. `docs/story/RUNTIME.md` (from 0.2.2): `StoryBuild.run`, autoplay properties/setter, capture events, markers.
3. `docs/core/INTEGRATION.md` "Arming, consent and first join" (direct template builders must call
   `CoreRuntime.arm(server)` on the server thread, then save and stop; "The template package must deliberately choose
   the judge's cheats setting"). `docs/map/INTEGRATION.md` "Authoring commands and world" (flat-air properties, the
   smoke save "is a test fixture, not a release template").
4. Code: `scripts/smoke_test.py` (server/client phases, watchdog, `verify_mod_directory`), `scripts/verify_jar.py`,
   `build.gradle`, `.github/workflows/build.yml`, `src/clientSmoke/**` (single orchestrator, `story` phase from 0.2.2),
   `client/contract/**` (the title is reached after the contract), `README.md` ("Instalación en casa" [Installation at home], "Cómo comenzar" [How to begin]).

## What to build (exact outcomes)
1. **Template authoring in CI** (`scripts/author_template.py`, Python, CI only like `smoke_test.py`): start a dedicated
   server with the exact player JAR on a fresh world with the flat-air properties of `docs/map/INTEGRATION.md`, run
   `unallocated build all`, then the story build (`unallocated story build`), arm through the core, `save-all flush`,
   stop cleanly; check `UNALLOCATED story build ok`, the armed-session marker and a clean stop. Produce a deterministic
   ZIP (sorted entries, fixed timestamps) of the world with top folder `UNALLOCATED/`, excluding `session.lock`,
   `playerdata/`, `stats/`, `advancements/`, logs. Choose and enforce in the template: Adventure, the map's rules,
   **cheats off** (`/unallocated siguiente` works without cheats), level name "UNALLOCATED"; if a committed
   `template/override/world.zip` exists (owner's hand polish, D-04), use it instead after the same checks.
2. **Template in the JAR:** inject the ZIP as `unallocated/template/world.zip` into the player JAR before
   `verify_jar.py` and the checksum (or rebuild with it; your choice — one JAR, verified after its last change);
   `verify_jar.py` requires exactly one template entry, a valid ZIP with `UNALLOCATED/level.dat` and the session file;
   raise the 2 MiB budget only as far as the measured size needs (state the measured size in the commit message body).
   The smoke must install that final JAR (byte for byte, as today).
3. **The button** (client; Fabric screen events, bundled `fabric-screen-api-v1`; no new title mixin unless unavoidable):
   label from lang key `menu.unallocated.play` ("UNALLOCATED"); placed prominently above Singleplayer without hiding
   vanilla buttons at any GUI scale. On press: if a copy with an unfinished session exists in `saves/`, offer continue
   or new (vanilla `ConfirmScreen`, keys `menu.unallocated.continue.*`); a new copy uses the next free folder name
   (`UNALLOCATED`, `UNALLOCATED (2)`, …). Show a connecting screen (key `menu.unallocated.connecting`, Spanish
   "Conectando al servidor…", D-03's words) while copying **off the render thread**; then open the world through
   vanilla's world-open flow. Errors (disk full, missing template) show a vanilla error screen with the fallback hint
   (key `menu.unallocated.error`) and never crash. Add the keys to all eight lang files (Spanish in `en_us.json` too,
   existing convention; locale copies follow the existing pattern).
4. **Client phase `play`** (after `story`; contract already accepted): title → screenshot
   `unallocated-title-button` → click the real button → screenshot `unallocated-connecting` → world opens → assert the
   session started in the first chapter → autoplay run 0 of the **shipped** story to its ending → farewell → title; then
   the button again, "new", run 1, and so on for every entry of `autoplay.runs`. Save every `story-<capture>.png`.
   `scripts/smoke_test.py` derives this phase's required markers and screenshots **from the shipped story files**
   (`src/main/resources/data/unallocated/story/`: run count, ending ids, capture names), so content never edits the
   orchestrator. Add a Python unit test for that derivation with a synthetic story.
5. **Video:** during `play` in profile `none` only, record the Xvfb display with ffmpeg (`x11grab`, modest frame rate,
   H.264 `yuv420p`, ≤ 25 MB) to `.tmp/smoke/video/gameplay.mp4`; if ffmpeg is missing or fails, log
   `UNALLOCATED video unavailable: <reason>` and continue (the video never fails the build; everything else does).
6. **Release artifact** (profile `none`, success only): `unallocated-release-<sha>` exactly as §4.3 lists, including
   `UNALLOCATED-world.zip` (the same template ZIP) and `release.json`. Keep the existing `unallocated-<sha>` artifact.
7. **Version 0.2.0** in `gradle.properties`; README: "Requisitos" [Requirements] file name, "Instalación en casa" [Installation at home] step 4, and "Cómo
   comenzar" [How to begin] rewritten in Spanish for the button, continue/new, `/unallocated siguiente` as rescue, and the ZIP
   fallback (unzip `UNALLOCATED-world.zip` into `saves/`, open from Un jugador). Keep every other README section.
8. Timeouts: raise the client step and job timeouts only as needed (job ≤ 45 min). Order workflow steps so the
   template exists before `verify_jar.py`.

## Files you may touch
New: `scripts/author_template.py`, its test `scripts/test_author_template.py`, `src/client/java/org/unallocated/client/launcher/**`,
`template/override/.gitkeep` with a README line in `template/override/README.md` (English: what to put there). Edit:
`UnallocatedClient.java` (one line), lang files (only `menu.unallocated.*` keys), `scripts/smoke_test.py`,
`scripts/test_smoke_test.py`, `scripts/verify_jar.py`, `scripts/test_verify_jar.py`, `build.gradle`,
`gradle.properties`, `.github/workflows/build.yml`, `src/clientSmoke/**`, `README.md`, `fabric.mod.json` (one appended
entry only if you add a mixin config). Nothing else; do not change the story runtime (report gaps instead), the core,
the contract, or `docs/plan/milestones.md` except §4.3 artifact names if they must change (then also tell package 5).

## Decisions you may take alone
Button placement and size; continue/new behaviour details; ZIP layout details within the constraints; how the JAR gets
the template; ffmpeg parameters; how the helper finds the world-open API. Not yours: story, the template's content
(it comes from the map code and the story's `build`), player-facing wording beyond the four keys (the content package
may reword them tomorrow).

## Acceptance
- Rehearsal green in all four profiles; profile `none` uploads `unallocated-release-<sha>` with every §4.3 file.
- `play` phase evidence for the shipped (neutral) story: button and connecting screenshots, run 0 completion marker
  `UNALLOCATED story autoplay run=0 ending=SERVER_CLOSED complete`, farewell, title; the session in the opened copy
  was armed and unstarted before join, Adventure, cheats off (assert in the helper).
- `verify_jar.py` rejects a JAR without the template and with two (unit tests).
- All existing phases and checks unchanged and passing.

## If time runs short
Keep 1–4 and 6–7. Drop in order: video (5), continue/new (always a new copy), multi-run loop (run 0 only). Report.

## Delivery and mechanics
- Before each commit: `git status --untracked-files=all`; Python tests; `python3 scripts/story_check.py`;
  `python3 scripts/contract_text.py --check`.
- Commit through the relay (`.git` is read-only): `.tmp/commit-requests/0.2.3-<k>.json` = `{"paths": [...],
  "message": "0.2.0.3: <what>"}`, read `.tmp/commit-results/0.2.3-<k>.json`; wait for each result before the next.
  No AI attribution, no co-author lines.
- Rehearse: `.tmp/rehearse-requests/0.2.3-<k>.json` = `{"note": "..."}`, wait for `.tmp/rehearse-results/0.2.3-<k>.json`
  (20–40 min). You cannot push. Fix causes; never weaken a check. **Done = final commit's rehearsal passes all four jobs.**
- Final message (English): files, run ids and results, measured template and JAR sizes, video size, anything dropped.
