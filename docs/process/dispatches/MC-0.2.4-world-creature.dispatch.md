*Translated from the original Spanish on 2026-11-01.*

**Captain's dispatch note (2026-10-28 10:15Z):** packages 0.2.1, 0.2.2, 0.2.3 and 0.2.5 are on `v0.2.0`, head
`b77bb10` (0.2.3: the title button opens the shipped world, the `play` CI phase plays it to an ending, the judge
release artifact with a gameplay video, the shared Gradle dependency cache and the 12-minute no-output watchdog).
Read `docs/story/RUNTIME.md` and `scripts/author_template.py` before you start, and build on what they actually
deliver. Effort: **medium**, as this brief recommends: package 0.2.3 at high spent a whole Codex 5-hour window in 98
minutes, and the owner's content night (from 20:00Z) needs Codex after you. Plan to finish by about 18:30Z; if Part B
does not fit, deliver Part A proven and report Part B as not done.

**CI is unavailable right now, so this package changes its mechanics (captain, 10:30Z):** GitHub refuses to start
jobs on this private repository ("The job was not started because recent account payments have failed or your
spending limit needs to be increased"; run 40669686055): the account's included Actions minutes for October are used
up, and the owner decides how to restore them. Until the captain tells you CI is back: **do not write rehearsal
requests** (they fail instantly and teach nothing). Build Part A, then Part B; before each commit run
`javacheck.sh` (under the memory condition below), every Python test and `story_check.py`; commit through the relay
piece by piece; write in the checkpoint exactly what still needs CI to be proven (each JUnit test, each client-phase
marker and screenshot). When you finish, say so and stop: the captain rehearses your commits once CI returns and
resumes this same session with the results, and then you fix what they show. If the dispatch is resumed with "CI is
back", the brief's rehearsal mechanics apply again.

Facts from tonight's CI (do not rediscover them):
- A new `rehearsal/*` branch cannot restore another branch's cache (GitHub scopes caches by branch), so every
  rehearsal starts cold and downloads every dependency. Some downloads stall on TLS; the watchdog now fails such a
  step after 12 minutes of silence and uploads the thread stacks under `.tmp/smoke/gradle-diagnostics/`. A rehearsal
  that fails only that way (stacks in a TLS download, no source, Java or game failure) is requested once more,
  unchanged, and you say so.
- Vanilla `MusicManager` fades the MUSIC bus independently of the mod's effects, so audio assertions compare readings
  taken in the same client task (see `AudioSmokeTest` at `b77bb10`). Apply the same care to any new timing check.
- The relays for `~/Workspaces/mc-hito` are running (commit and rehearsal, as in the brief). `~/Workspaces/mc-ref/javacheck.sh ~/Workspaces/mc-hito`
  is authorized before each commit when `MemAvailable` is at least 900 MB and `pgrep -f "chrom|firestore-emulator"`
  prints nothing; otherwise rely on the rehearsal.
- Checkpoint after every step in `.tmp/024-checkpoint.md`, in plain English sentences with spaces (the last
  package's checkpoint ran its words together and was hard to read).

One small CI diagnostic is also yours, first (allowed in `scripts/smoke_test.py` and its test): rehearsal 40667754056
on `b77bb10` failed only in the `0.165.0+26.3` build step, where the daemon sat for minutes in an HTTPS
`DownloadAction` read (stacks in that run's diagnostics artifact) despite `socketTimeout=180000` and six retries, so
the stall is sticky on one host and we do not know which. When the watchdog fires, also write the Gradle JVMs' open
TCP connections (`ss -tnpi`, filtered to their pids) into the diagnostics, and when any stack is inside a dependency
download, print one line `UNALLOCATED Gradle watchdog: dependency download stalled (infrastructure)` before failing.
Do not change the timeouts or retries without evidence of which host stalls.

One documentation fix is also yours (allowed in addition to the brief's file list): `docs/plan/TOOLCHAIN.md` around
line 71 still says Gradle's action owns the dependency cache and that pull-request runs read it without writing; since
0.2.3 the workflow disables that cache and restores one shared `actions/cache` key for every profile. Make that
paragraph true, with the branch-scoping fact above and the official link
https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching#restrictions-for-accessing-a-cache.

# Brief MC-0.2.4 — `world-creature`: pocket dimension, copy, corruption and the creature

**Recommended lane:** Codex CLI, gpt-6.1-sol, effort **medium**. Why: two bounded primitives on top of the runtime's
published registries; the format is already fixed in the plan; the risk is in Minecraft details (datapack dimension
loading in an existing world, cross-dimension block copy, entity visibility), which medium handles with the reference
JARs. Budget about 45 minutes plus rehearsals. **This is the first package to cut** if quota is short (plan §11): the
captain may dispatch only Part A (world) or skip it.

## Who you are, where, for whom
Senior Minecraft 26.3 / Fabric engineer on UNALLOCATED (first-person horror mod for the USFQ jam, by Alexander
Kholodov, `alexanderkholodov1`), dispatched by the Beacon captain. Worktree `~/Workspaces/mc-hito`, branch `v0.2.0`,
with packages 0.2.1–0.2.3 integrated. Beacon: 3.3 GB RAM. **Never run Gradle, Java builds or the game here; never write
under `/tmp`.** CI only. Reference JARs in `~/Workspaces/mc-ref/` (read without running Java; `javacheck.sh` only if
the dispatch authorizes it). English in code/docs.

## Goal and why
The owner decided the experience has "a monster, a pocket dimension and a climax with one of several endings" (his
answers in `docs/plan/DECISIONS.md`; the pocket dimension and the creature with a body are his words in
`docs/intent/Initial brainstorm.txt`, "Tematica" [Theme] 1, and `docs/intent/UNALLOCATED — Plan de construcción (AI generated,
Author redacted).md` §3.3 and §5.6 — read them, do not take story from anywhere else). What the pocket looks like, what
the creature's body is and what it does are **open design questions** (designer's questions 3 and 10 in
`~/Workspaces/mc-story/NOTAS.md`). You build the primitives that serve any reasonable answer, so tomorrow's content only
fills in data.

## Inputs
1. `docs/plan/milestones.md` §3 choices 8–9, **§4.4**, §5, §6.4–§6.6 (rows `copy`, `corrupt`, `creature`, trigger
   `creature`), §11 (cut list), §12 A5–A7.
2. `docs/story/RUNTIME.md` (registries `StoryActions`/`StoryTriggers`, `resolvePosition`/`resolveBox`, autoplay
   `simulate`), `docs/plan/SPYGLASS-BAIT.md` and `bait/SpyglassBaitLogic.java` (visibility test and "moves only while
   unseen"; `shouldBeSaved` false), `docs/map/INTEGRATION.md` (biome, flat-air world, section boxes), `scripts/author_template.py`
   from 0.2.3.

## What to build
**Part A — world**
1. Datapack dimension `unallocated:pocket` in mod data: `dimension_type/pocket.json` and `dimension/pocket.json` (flat
   air like the overworld template, fixed time, no weather, no mob spawning, no structures) and biome
   `unallocated:pocket` with placeholder neutral-grey fog/sky and no music (content sets the real values tomorrow by
   editing that JSON). Prove it exists in a fresh world **and** in the authored template (the template is authored
   with the mod present). If 26.3 does not create a datapack dimension in an existing world, report it with evidence
   and implement the fallback of plan §11 item 2 instead (do not invent a third way).
2. Actions `copy` (zone → destination minimum corner, any dimension to any dimension; chunk-batched within the
   vanilla modification limit; forceloads what it needs and releases it; deterministic; block entities copied) and
   `corrupt` (zone; rules applied with a seeded RNG; `over_seconds` > 0 spreads the replacement evenly over that time
   in small per-tick batches and resumes after reload). Also expose them as gamemaster commands
   `/unallocated story copy <zone> <x> <y> <z> <dimension>` and `/unallocated story corrupt <zone> <seed>` with a rule
   preset, so `story.json` `build.run` can pre-copy sections into the pocket when the template is authored.
3. `teleport` to `dimension: "pocket"` must work (the runtime already resolves dimension names; verify the round trip).

**Part B — creature**
4. Action `creature` with ops of §6.6: `spawn` (body `mannequin` with `profile: "player"` = the participant's profile
   as the bait does, a named profile, optional `texture` = a skin PNG shipped under `assets/unallocated/textures/` if
   26.3's mannequin supports a texture override — verify in the reference JAR; otherwise reject `texture` at load and
   say so in §6.6 — and `scale`; or `summon` = any vanilla entity with SNBT, tagged by the runtime), `stalk` (moves only
   while unseen, toward the player, `step` blocks per move, never closer than `min_distance`; reuse the bait's
   visibility logic), `chase` (moves toward the player at `speed` blocks/second with ground snapping and no wall
   clipping through solid blocks; teleport steps are fine; no pathfinding AI), `hold`, `face_player`, `despawn`.
   Creatures are never written to the save; the runtime state (id, body, position, op) is persisted under `story.`
   keys and the creature is restored on resume.
5. Trigger `creature` (`near` within `distance`; `seen` = in the player's view with line of sight for `seconds`;
   `caught` = within `catch_distance` while chasing) with autoplay `simulate` (move the creature to satisfy it).

**Proof**
6. Fixture `src/test/resources/story-fixtures/valid/world/` (placeholders only, like the §7 example): one chapter
   that copies the `base` section into the pocket, corrupts it with a seeded rule, teleports the player there, spawns
   a mannequin creature that stalks then chases until `caught`, and ends; captures `world-pocket` and `world-creature`.
   It must pass `scripts/story_check.py`. Run it in the client `story` phase after the example runs (a third fresh
   world), with evidence `story-world-pocket.png`, `story-world-creature.png` and its completion marker.
7. JUnit: corruption determinism (same seed → same blocks; ratio respected), copy batching boundaries, creature
   steering (never through solid blocks, never closer than `min_distance` while stalking, moves only while unseen).

## Files you may touch
New: `src/main/java/org/unallocated/world/**`, `src/main/java/org/unallocated/creature/**`, matching tests,
`src/main/resources/data/unallocated/dimension/**`, `dimension_type/**`, `worldgen/biome/pocket.json`,
`src/test/resources/story-fixtures/valid/world/**`. Edit: `UnallocatedMod.java` (one line), `src/clientSmoke/**`,
`scripts/smoke_test.py`, `scripts/test_smoke_test.py`, `scripts/story_check.py` (only if it disagrees with §6),
`docs/story/RUNTIME.md` (append your actions), `docs/plan/milestones.md` §6.6 rows of your actions (only if they
change), `fabric.mod.json` (one appended entry only if you add a mixin config). Nothing else.

## Decisions you may take alone
Implementation of copy batching, corruption scheduling, steering and visibility thresholds; whether a mixin is needed;
debug commands. Not yours: the pocket's look, the creature's body or behaviour in the story, any player-facing text.

## If time runs short
Deliver Part A with its proof first and rehearse; Part B second. If only Part A fits, report Part B as not done.

## Delivery and mechanics
- Before each commit: `git status --untracked-files=all`; Python tests; `python3 scripts/story_check.py` on the
  shipped story and on `story-fixtures/valid/world`.
- Commit through the relay: `.tmp/commit-requests/0.2.4-<k>.json` = `{"paths": [...], "message": "0.2.0.4: <what>"}`;
  read `.tmp/commit-results/0.2.4-<k>.json`; one request at a time. No AI attribution, no co-author lines.
- Rehearse: `.tmp/rehearse-requests/0.2.4-<k>.json` = `{"note": "..."}`; wait for `.tmp/rehearse-results/0.2.4-<k>.json`
  (20–40 min). You cannot push. Fix causes, never weaken checks. **Done = final commit's rehearsal passes all four jobs.**
- Final message (English): what works, evidence, run ids, any §6 change (e.g. `texture` unsupported), what is not done.
