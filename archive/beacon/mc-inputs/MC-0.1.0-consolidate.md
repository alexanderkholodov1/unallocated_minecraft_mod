# UNALLOCATED milestone v0.1.0: consolidate every work package into the one hito branch (Codex, gpt-6.1-sol, high)

## For whom and why
You work for Alexander Kholodov (GitHub `alexanderkholodov1`), who is entering a Minecraft game jam with the mod
UNALLOCATED (repo `alexanderkholodov1/unallocated_minecraft_mod`, private). The submission deadline is 2026-10-28 23:59
UTC-5. His intent documents are in `docs/intent/` (verbatim, "CADA PALABRA esta escrita por mi Y ESTA AHI POR ALGO");
the plan is `docs/plan/PLAN.md`, his answers to the open decisions are in `docs/plan/DECISIONS.md` (branch `v0.1.14`).

Milestone 1 was built as 12 separate work-package branches, each pushed to GitHub. He judged that a grave failure of
repository hygiene: "EL AVANCE QUE PUDO PERFECTAMENTE PODRIA CABER EN UN PR, NO ENTIENDO PARA QUE NECESITO 14 RAMAS",
and asked for a clean-up "NUNCA ELIMINANDO POR ELIMINAR, SINO FUSIONANDO Y UNIENDO COSAS EN HITOS COHERENTES". Your job
is the fusion: one coherent milestone `v0.1.0` that contains the work of every package, builds, and passes CI. The
versions were just corrected from the mistaken `1.0.0-alpha.1.N` to `0.1.N` (1.0.0-alpha is reserved for the complete
game he tests himself); keep `0.x` everywhere.

## Where you are
Machine Beacon (Linux, 3.3 GB RAM shared with other services; `/tmp` is a small RAM disk: never write there, use
`.tmp/` in the worktree). Worktree `~/Workspaces/mc-hito`, local branch `hito-v0.1.0`, which is `origin/v0.1.0` plus a
clean merge of `v0.1.2` (the plan). **Never run Gradle, Java builds or the game locally** (not enough memory): CI on
GitHub runs the real build, unit tests, a real dedicated server and a headless client. Python checks in `scripts/`
(`python3 -m pytest scripts` or the individual `scripts/test_*.py`) are fine locally.

Branches to integrate (all on `origin`; read them with `git show`, `git diff`, `git log`; your sandbox cannot write
`.git`, so do not run `git merge`, `checkout` or `commit`):

| Branch | Package | Last CI |
|---|---|---|
| `v0.1.1` | scaffold: toolchain, CI, install docs | |
| `v0.1.3` | contract screen before the menu ("De acuerdo") | green |
| `v0.1.4` | interface effects (camera, chat, HUD, toasts, death screen...) | green before the rename |
| `v0.1.5` | spyglass bait | **red**: `SpyglassBaitLogicTest.directorCanForceAStepAndTheScare` (SpyglassBaitLogicTest.java:263) |
| `v0.1.6` | Fabric API inside our JAR (jar-in-jar), version matrix, `breaks` | green |
| `v0.1.7` | session core (acts, saved state, test commands) | green |
| `v0.1.8` | map (persistence fix 1.8.12) | red before the rename; check |
| `v0.1.10` | ending v0 | |
| `v0.1.11` | audio | **red**: client smoke "Audio exceeded authored gain/pitch bounds" |
| `v0.1.14` | story and game design proposal + his answers in `DECISIONS.md` | docs only |
| `v0.1.15` | faster CI (headless graphics cached: 3 min 51 s instead of 10 min 47 s) | green |

`v0.1.0` already contains the core merged with the contract (`7ea28bd`). A trial merge showed conflicts in
`README.md`, `gradle.properties`, `build.gradle`, `.github/workflows/build.yml`, `docs/plan/TOOLCHAIN.md`,
`docs/core/INTEGRATION.md`, `scripts/smoke_test.py`, `scripts/test_smoke_test.py`, `TitleScreenSmokeTest.java`,
`fabric.mod.json` (main and clientSmoke), the lang files, `UnallocatedMod.java`, `UnallocatedClient.java` and
`DECISIONS.md`.

## Objective and "done"
The working tree of `hito-v0.1.0` holds the union of every package's behaviour, coherent as one mod:
- every feature, test, CI check and doc of every branch above is present and wired (one `build.gradle`, one workflow
  with the union of checks and the faster graphics cache, one smoke test that exercises title screen, contract,
  interface and audio, merged lang files, one `fabric.mod.json` with every entrypoint and mixin config);
- version `0.1.0` in `gradle.properties` and the README (the README describes what the integrated build does);
- `docs/plan/DECISIONS.md` keeps **his answers verbatim** (from `v0.1.14`); never rewrite his words;
- the three red tests are fixed at their cause (not by weakening or skipping the check); if a fix needs a product or
  design choice, stop and describe it with options;
- CI green on the pushed hito branch.
Done = CI green on `v0.1.0` at your last commit, plus your report.

## Margin
You decide how to resolve each conflict, keeping both sides' intent. A genuine product choice (two behaviours that
cannot coexist) is described with options, not chosen. Do not push, open or merge PRs, deploy, or touch other
worktrees or branches. Do not delete any branch.

## Commits, pushes and checkpoints
`.git` is read-only in your sandbox: commit through the relay running for this worktree (write
`.tmp/commit-requests/<name>.json` = {"paths": [...], "message": "..."}; read `.tmp/commit-results/<name>.json`).
Messages `0.1.0.K: <what and why>`, K from 3, one commit per integrated package (name the branch and its tip hash in
the message) plus fix commits; **no AI attribution of any kind**. Every commit you make is pushed to `origin/v0.1.0`
within a minute by the coordinator's watcher, so CI runs on it: follow it with `gh run list --branch v0.1.0` and
`gh run view <id> --log-failed`. Commit an integrated package only once its files are consistent, so pushed states
build. Write `.tmp/CHECKPOINT.md` after each package (what is integrated, open conflicts, CI results). After you
finish, the coordinator records the merge parents so every package branch is provably contained in `v0.1.0`; a
fail-closed review by a stronger model reads the finished hito.

## Report
Final message in English: per branch, what was integrated and how each conflict was resolved; the fixes for the red
tests and their causes; the CI run ids and results; anything you could not decide.
