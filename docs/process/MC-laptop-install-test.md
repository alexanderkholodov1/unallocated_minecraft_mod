# Brief: clean-install test of UNALLOCATED on the owner's laptop (worker for the mod's lead), 2026-10-29

## Who, for whom, why
You are a careful release engineer working for the mod's lead (an Opus session on Beacon) on UNALLOCATED, a
Minecraft Java 26.3 Fabric horror mod for a game jam that closes **04:59Z today**. The judges install it at home on
unknown machines; the owner's acceptance line is "installation and stability on unknown machines". Before release, the
build must be installed **from the release files, on a clean profile, following the itch page's own instructions**, and
started up to the first story chapter. You build and prove that test on the owner's laptop so the lead can rerun it on
the final files in minutes. Harness ready and proven on the current build by **03:15Z** (`date -u`); report earlier if
blocked.

## Where
- Beacon (this machine): 3.3 GB RAM, shared; **never run Gradle, Java or Minecraft here; never write under /tmp**.
  Your workspace: `~/Workspaces/mc-inputs/laptop-test/` (write only there).
- The laptop: `ssh ak-laptop`, then `wsl -d Ubuntu -e bash -l -s` with a script on stdin (WSL Ubuntu, 7.4 GB free RAM,
  12 CPUs, portable JDK 21 in `~/opt/jdk21`). Keep every run in the foreground of your ssh call; use a clean directory
  `~/unallocated-cleantest/` there and nothing else of the owner's.
- Repository worktree (read only for you): `~/Workspaces/mc-hito`. Do not edit or commit anything there.

## Inputs
- Current release artifact (stand-in until the final one): `gh run download 40700637219 -R
  alexanderkholodov1/unallocated_minecraft_mod -n unallocated-release-9498b53845907e6093dc33753bfded12bdacd126 -D
  ~/Workspaces/mc-inputs/laptop-test/artifact`, then `python3 ~/Workspaces/mc-hito/scripts/release_bundle.py
  <artifact dir> --out ~/Workspaces/mc-inputs/laptop-test/dist` (exit 2 = only design tokens unfilled; that is fine).
- The itch page the judges follow: `dist/itch-page.md` (from `docs/release/ITCH-PAGE.md`), and `docs/release/CHECKLIST.md`.
- README "Instalación en casa" / "Cómo comenzar" in the worktree (Spanish).

## What to build
`~/Workspaces/mc-inputs/laptop-test/laptop-install-test.sh <dist-dir>` run from Beacon, which:
1. Copies the dist files to the laptop into a fresh `~/unallocated-cleantest/run-<UTC>/` (no previous mods, configs,
   saves or receipts).
2. Follows the page's steps as literally as a headless Linux machine allows: Java 25 from Temurin if the page's path
   needs it (cache the download in `~/unallocated-cleantest/cache/`), the **official Fabric installer** (version the
   page names) in client mode for Minecraft 26.3 and the Loader version the page names into the clean game directory,
   then the mod JAR from dist copied into that directory's `mods/` and nothing else. The official Minecraft Launcher
   cannot run headless in WSL: substitute the smallest faithful launcher (e.g. `portablemc` with an offline user, or
   the Fabric profile's own launch arguments), and say exactly what you substituted and why in the report.
3. Starts the client under Xvfb (or WSLg if present), and drives it like a judge: the contract screen appears, type
   `De acuerdo` and confirm; the title screen shows the **UNALLOCATED** button; click it; the world opens. Use
   screenshots (you can read images) to find positions; xdotool or similar for input. Capture PNGs at: contract, title
   with the button, connecting screen, in-world after load.
4. Passes only if `logs/latest.log` shows `UNALLOCATED initialized (common).`, `UNALLOCATED initialized (client).`,
   `UNALLOCATED story loaded`, and a chapter entry `UNALLOCATED story chapter=<first> slot=LOBBY enter`, no crash
   report, no `UNALLOCATED story invalid`, and the game keeps running 60 s in-world. Then quits the game cleanly.
5. Copies back to Beacon: the PNGs, `latest.log`, any crash report, and a short `RESULT.md` (pass/fail, versions used,
   timings, every substitution).

## Rules
- No Gradle builds and no changes to the mod; you test the release files only. If the build fails your test, report
  the evidence; do not fix the mod.
- No AI attribution anywhere; no commits; no pushes. Don't touch the owner's other files on the laptop.
- Report: write `~/Workspaces/mc-inputs/laptop-test/REPORT.md` (English): how to run the harness, the result on the
  current build, the evidence paths, and any instruction in the itch page that a judge would find wrong or unclear
  (quote the line).
