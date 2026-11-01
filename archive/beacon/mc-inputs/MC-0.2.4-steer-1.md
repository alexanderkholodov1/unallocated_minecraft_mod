**Captain, 2026-10-28 13:25Z: CI is back.** The owner restored the Actions minutes; the brief's rehearsal mechanics
apply again (`.tmp/rehearse-requests/0.2.4-<k>.json`, then `.tmp/rehearse-results/0.2.4-<k>.json`). Two changes to
the mechanics, effective now:
- `javacheck.sh` now checks memory and running browsers itself and exits 75 when the machine is busy; just run it
  before each commit (the `pgrep` you ran yourself matched its own shell, which is why it refused this morning).
- Minutes now cost money. Rehearse only when a commit is ready to be proven, not to explore; read the logs and the
  diagnostics artifacts of a failed run fully before the next request.

**The captain's rehearsal of your head `88a8e8d` (run 40694193721):** `build (0.153.1+26.3)` passed (rejection).
`build (none)` failed in the `first` phase at `LauncherSmokeTest.opened` (`LauncherSmokeTest.java:78`, called from
line 48): after `menu.unallocated.play` was clicked, the client logged only `Loaded 1866 advancements` and then nothing
until `waitFor` timed out about 9 seconds later (`AssertionError: Timed out waiting for predicate`); no screenshot was
taken at the failure. On `b77bb10` (run 40667754056) the same profile passed this test, so the change since then (the
`unallocated:pocket` datapack dimension, and anything else 0.2.4 added to world loading) is the first suspect: a
screen shown while opening the shipped copy (datapack or experimental-settings warning, a load error), or a slower
load with the extra level. Find the cause with evidence. The full job log is
`~/Workspaces/mc-hito/.tmp/024-run1-none.log`, and the diagnostics artifact is unpacked in
`~/Workspaces/mc-hito/.tmp/024-diag-run1-none/`. The two API profiles then failed differently: `build (0.161.0+26.3)` and
`build (0.165.0+26.3)` passed the `first` phase (launcher included) but failed in the `story` phase with
`AssertionError: Timeout loading world` at `WorldSmokeTest.run` (`WorldSmokeTest.java:106`, from
`StorySmokeTest.java:81`), when your world fixture's fresh world is created. Three profiles failing on world loading
after the pocket dimension arrived is one finding until the evidence says otherwise; their logs and artifacts are in
run 40694193721 (`gh run view 40694193721 --log-failed`, `gh run download 40694193721 -n <artifact>`). Also: the optional `ffmpeg` install printed "UNALLOCATED video unavailable: ffmpeg installation failed" in
that job; check whether the video still came out, and fix it only if it did not.

Fix the cause (do not lengthen the wait to hide a screen; if the load is genuinely slower, give the wait a measured
bound and say so), add a screenshot on that timeout so a future failure shows the screen, then continue: **done is a
rehearsal of your final commit with all four jobs green**, with the world fixture's evidence (`story-world-pocket.png`,
`story-world-creature.png`, the markers in your checkpoint) and the play phase's authored-template pocket marker.
Commits start `0.2.0.4: `. Final message as in the brief, with run ids.
