# Brief MC-0.2.2 — `story-runtime`: the data-driven story engine

**Recommended lane:** Codex CLI, gpt-6.1-sol, effort **high**. Why: this is the package everything else depends on
(launcher playthrough, world/creature kit and every content file tomorrow); it touches the saved session, the server
tick, client rendering and the CI orchestrator, and mistakes here cost the content night. Budget: about 60 minutes of
work plus rehearsals. Keep strictly to the scope below; if you see you will exceed ~75 minutes, stop at the cut line in
"If time runs short" and report.

## Who you are, where, for whom
You are a senior Minecraft 26.3 / Fabric engineer on UNALLOCATED, a first-person horror mod for the USFQ jam "Terror en
Primera Persona" by Alexander Kholodov (`alexanderkholodov1`), dispatched by the Beacon captain, who verifies and
pushes. Worktree `~/Workspaces/mc-hito`, branch `v0.2.0` (hito v0.1.0 at b47348a plus the plan commit and package
0.2.1). Machine Beacon: 3.3 GB RAM. **Never run Gradle, Java builds or the game here; never write under `/tmp`.** Builds
and game tests run only in CI. Python checks run locally (`python3 -B -m unittest discover -s scripts -p 'test_*.py'`).
Reference binaries for reading signatures: `~/Workspaces/mc-ref/` (official 26.3 client JAR, Fabric API 0.165.0 and
gametest JARs); inspect them without running Java (e.g. Python `zipfile` + class-file parsing, as earlier packages did).
`~/Workspaces/mc-ref/javacheck.sh` (type-check only) may be used only if the captain's dispatch message authorizes it.
English in code and docs; Spanish only in player-facing text.

## Goal and why
The story of the game is not designed yet; it will be written tomorrow evening as JSON by cheap agents, with only hours
left before the jam closes (2026-10-29 04:59Z). You build the runtime that plays that JSON, so that tomorrow is
content, not engineering: load and validate the story format v1, run chapters on the core's act slots, fire beats from
triggers, execute timed actions over the existing APIs and vanilla commands, render two-language text on the client,
end with data-defined endings, persist everything across save/reload, and — essential — an **autoplay** test mode that
plays any story end to end in CI, so content is proven in real use with no human.

## Inputs (read in this order; do not restate the owner's documents)
1. `docs/plan/milestones.md`: §3 (choices 1–7, 10), §4.2 (your package and the **exact interface** packages 3 and 4
   will consume), §5 (which shared files you may touch and how), **§6 and §7 (normative format and example)**.
2. API documents you build on: `docs/core/INTEGRATION.md` (modules, budgets, rescue, events, payloads — you must
   respect "Reentrant advance/skip calls inside ActChanged listeners are ignored" and "Later packages must not register
   duplicate codecs/receivers"), `docs/ending/INTEGRATION.md` (the reusable `EndingController.close` boundary and the
   tagged disconnect reason), `docs/plan/INTERFACE.md` (client API, refusals, cooldowns), `docs/audio/AUDIO.md`,
   `docs/plan/SPYGLASS-BAIT.md`, `docs/map/INTEGRATION.md` (coordinates, `MapLayout`, boxes).
3. Code: `src/main/java/org/unallocated/core/*`, `ending/*`, `bait/SpyglassBait*.java`, `audio/AudioSounds.java`,
   `map/MapLayout.java`, `debug/UnallocatedCommand.java`, `client/core/ClientSession.java`,
   `client/interfacefx/InterfaceEffects.java`, `client/ending/EndingClient.java`, `clientSmoke/.../TitleScreenSmokeTest.java`
   and `MapTourSmokeTest.java` (how the helper creates a flat-air map world), `scripts/smoke_test.py`, `build.gradle`.
4. Package 0.2.1's `scripts/story_check.py`, `scripts/map_anchors.json` and `src/test/resources/story-fixtures/`.
5. `docs/integration/MILESTONE-v0.1.0.md` for the failure history (races, option leaks, duplicate JSON keys) you must
   not reintroduce.

## What to build (exact outcomes; internal design is yours)
1. **Loader + validator** (`org.unallocated.story`): reads `data/unallocated/story/` from the mod JAR, or from the
   directory in system property `unallocated.story.dir` when set (CI helper only). Implements every rule of §6.8 with
   the **same rule ids**, plus the Java-only checks (every `command`/`function` parses on the real server dispatcher
   with a permission-level-2 source at `SERVER_STARTED`; referenced structures/functions/dimensions exist). Invalid →
   log `UNALLOCATED story invalid <file>#<pointer>: [<rule>] <message>` for each finding and run **no** story (the
   world stays as v0.1.0's fallback acts) — add that marker to the smoke watchdog's fatal patterns so CI fails.
   Valid → `UNALLOCATED story loaded chapters=<n> beats=<m> endings=<k> format=1`.
2. **Engine:** registers one `ActModule` per slot `LOBBY, THEFT, LOOP, POCKET, CLIMAX`; chapter *i* occupies slot *i*;
   slots without a chapter return `enabled=false`; set those five act switches to `true` in `features.json` (do not
   touch `effects.*`). Budget = `budget_seconds`; `forceAdvance` runs `escape` then the core advances; `checkpointId`
   reports the chapter checkpoint; respawn moves the player to `respawn` or `checkpoint`. Triggers and actions exactly
   as §6.5/§6.6 except package 4's (`copy`, `corrupt`, `creature`, trigger `creature`), which your registries must
   reject as unknown until package 4 registers them. `end` from any chapter reaches EPILOGUE (add the smallest core API
   needed for a jump that records a truthful marker; keep every existing core guard and test). Markers:
   `UNALLOCATED story chapter=<id> slot=<ACT> enter|resume|escape`, `UNALLOCATED story beat=<id> fired t=<s>`,
   `UNALLOCATED story action beat=<id> do=<do> result=ok|skipped|refused`, `UNALLOCATED story ending=<ID>`.
   Persist fired beats, repeat counts, flags, counters, pending actions (beat id + next action index + remaining delay)
   in `SessionState` flags/counters under a `story.` prefix — no SavedData schema change. Server thread only.
3. **Registries for packages 3–4** (the interface of milestones §4.2, names exact): `StoryActions.register(String do,
   ActionParser)`, `StoryTriggers.register(String type, TriggerParser)`, `StoryRuntime.get(server)` with `fire`,
   `currentChapter`, `resolvePosition`, `resolveBox`; events `StoryBeatFired`, `StoryCapture`, `StoryEnding` on
   `runtime.events()`; an autoplay `simulate(ServerPlayer)` on every trigger. Document them in `docs/story/RUNTIME.md`
   (English, short, with one example of registering an action).
4. **Client side:** handlers for `EffectPayload` ids `story.title`, `story.actionbar`, `story.chat`, `story.blackout`,
   `story.interface`, `story.post_effect` (only if 26.3 exposes post chains to a client API you can verify in the
   reference JAR; otherwise reject `post_effect` at load and change its §6.6 row to say so), `story.ending_text`;
   register through `ClientSession.onEffect` — no new payload type. Language selection and the placeholders of §6.2;
   `{agreement}` reads the contract receipt through the existing client-only bridge (`ContractReceiptLookup`
   pattern) and never sends it. Extend `InterfaceEffects.Request` with an optional custom text that replaces the
   generated leak line (default behaviour unchanged; existing interface smoke must still pass byte for byte).
   Blackout is a HUD overlay with fade timings; it must clear on world leave and never persist.
5. **Endings:** `EpilogueAct` closes with the ending chosen by the story (flag `story.ending.<ID>`, else
   `fallback_ending`); `{"builtin": true}` keeps today's `SERVER_CLOSED` text exactly; data endings send
   `story.ending_text` before `EndingController.close`, and the client's farewell screen and splash show the title, lines
   and splash in the client language, plus the existing typed-agreement line when `quote_agreement`. Do not duplicate
   the shutdown path; keep the v0.1.0.22 tagged-reason publication fix.
6. **Autoplay** (only when `-Dunallocated.story.autoplay=true`; impossible to enable from the game): every
   unfired, enabled beat not in the run's `skip` list and whose trigger is not time/flag based is simulated in file
   order after `autoplay.wait_seconds` (default 1) as §6.5's "Autoplay does" column; all story time (`after`, `at`,
   `seconds`, budgets) is multiplied by `autoplay.time_scale`; the run index comes from
   `unallocated.story.autoplay.run` (default 0) or a static setter the helper calls before each world. Markers
   `UNALLOCATED story autoplay simulate beat=<id> trigger=<type>` and `UNALLOCATED story autoplay run=<i>
   ending=<ID> complete`. `capture` posts `StoryCapture(name)` and logs `UNALLOCATED story capture=<name>`; the helper
   saves `story-<name>.png` at that moment.
7. **Debug commands** under the existing root (gamemaster only, console works): `/unallocated story status`,
   `/unallocated story fire <beat>`, `/unallocated story check` (re-runs validation, prints findings).
8. **`build`:** `StoryBuild.run(server)` forceloads `build.forceload`, runs `build.run`, releases forceloads, logs
   `UNALLOCATED story build ok commands=<n>` or fails loudly. Expose `/unallocated story build` (gamemaster) for
   authoring; package 3 calls the Java method when it authors the template.
9. **Neutral default story** at `src/main/resources/data/unallocated/story/` exactly as milestones §6.9.
10. **`scripts/map_anchors.json` sync:** a JUnit test that the file equals `MapLayout`'s names. Correct
    `scripts/story_check.py` only where it disagrees with §6; if the **format** must change, change §6 of
    `docs/plan/milestones.md` in the same commit and say why.

## Tests and CI you add
- JUnit: Java loader on every fixture (valid example zero errors; each invalid fixture yields its `EXPECT.txt` rule at
  its pointer — the same contract as the Python test); engine scheduling with a fake clock (ordering, `at` ties, cancel
  on `next_chapter`/`end`, repeat/cooldown, guards); persistence round trip of pending actions; language selection and
  placeholders; map anchors sync.
- Dedicated server smoke: the shipped story and the example (via `unallocated.story.dir`) load and every command
  parses (`UNALLOCATED story loaded`), `story check` prints no finding.
- New client phase **`story`** (a third process in `scripts/smoke_test.py`'s `client_phases`, after `restart`; the
  contract is already accepted): the helper creates a fresh flat-air map world as the map tour does, runs the example's
  `build`, arms it, joins with autoplay run 0, waits for `UNALLOCATED story autoplay run=0 ending=EXAMPLE_A complete`,
  the farewell screen showing the example ending title, the title screen; then a second fresh world with run 1 to
  `EXAMPLE_B`. Required evidence: both completion markers, `story-example-sign.png`, `story-example-end.png`,
  `unallocated-story-farewell-a.png`, `unallocated-story-farewell-b.png`; options and receipt unchanged byte for byte.
  Pass `-Dunallocated.story.dir=<repo>/src/test/resources/story-fixtures/valid/example` and the autoplay property only
  to that phase's helper JVM (`build.gradle`). Raise the workflow's client step `timeout-minutes` and the job's only as
  much as the new phase needs (keep job ≤ 40 min); edit nothing else in the workflow.
- Every existing check stays; never weaken, skip or loosen one. Update `scripts/test_smoke_test.py` fixtures for the new
  phase.

## Files you may touch
New: `src/main/java/org/unallocated/story/**`, `src/client/java/org/unallocated/client/story/**`,
`src/test/java/org/unallocated/story/**`, `src/main/resources/data/unallocated/story/**`, `docs/story/RUNTIME.md`,
your own mixin config if a hook has no Fabric event (register it with **one appended entry** in `fabric.mod.json`).
Edit (minimal, as §5 of the plan says): `UnallocatedMod.java`, `UnallocatedClient.java` (one line each),
`core/*` (only the jump API), `ending/EpilogueAct.java`, `ending/EndingController.java`, `client/ending/**`,
`client/interfacefx/InterfaceEffects.java` and `InterfaceTexts.java`, `bait/SpyglassBait*.java` (only to publish
step/screamer/gone events), `debug/UnallocatedCommand.java`, `assets/unallocated/features.json` (act switches),
`src/clientSmoke/**`, `scripts/smoke_test.py`, `scripts/test_smoke_test.py`, `scripts/story_check.py`,
`scripts/test_story_check.py`, `scripts/map_anchors.json`, `build.gradle`, `.github/workflows/build.yml` (timeouts only),
`docs/plan/milestones.md` (§6 only, only if the format changes). Nothing else; in particular not README, lang files,
contract files, `verify_jar.py`, `fabric-api-modules.json` — if you truly need another Fabric API module, stop and
report instead.

## Decisions you may take alone
Class layout and names beyond the published interface; how each trigger is detected (Fabric events from the bundled
modules or a mixin of yours); trigger evaluation cadence (cheap: zone/look checks at most every 2 ticks are fine);
how blackout is drawn; message wording of findings; whether `post_effect` is supportable. You may **not** decide story
or game design, change §6 for convenience, change player-facing Spanish texts that exist, or enable `effects.*`.

## If time runs short (cut line)
Deliver 1–3, 5, 6, 9, 10 and the `story` phase for run 0 only. Drop in this order: `post_effect`, `still/sprint/sneak`
triggers, the second autoplay run, debug `fire`. Report exactly what was dropped and update §6 accordingly.

## Delivery and mechanics
- Before each commit: `git status --untracked-files=all`; run the Python test suite and
  `python3 scripts/story_check.py` (shipped story, exit 0) and `python3 scripts/contract_text.py --check`.
- `.git` is read-only in your sandbox. Commit through the relay: write `.tmp/commit-requests/0.2.2-<k>.json` =
  `{"paths": [<every changed or new file>], "message": "0.2.0.2: <what>"}` and read
  `.tmp/commit-results/0.2.2-<k>.json`. Messages start `0.2.0.2: `; no AI attribution, no co-author lines. Wait for each
  result before the next request (an overlapping request once captured merge text in v0.1.0).
- Rehearse your committed HEAD: write `.tmp/rehearse-requests/0.2.2-<k>.json` = `{"note": "<what changed>"}` and wait
  for `.tmp/rehearse-results/0.2.2-<k>.json` (pass/fail, run id, failing jobs' last lines; check every minute or so; a
  run takes 20–40 minutes). Fix causes, never symptoms. You cannot push.
- **Done = a rehearsal of your final commit passes all four jobs** with the evidence above.
- Final message (English): what was built, file list, every rehearsal run id and result, anything dropped, any change
  to §6, and anything a content author must know that §6 does not say.
