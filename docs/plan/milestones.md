# UNALLOCATED — milestones (hitos)

Written 2026-10-28 by the v0.2.0 technical lead for the owner, Alexander Kholodov, and the Beacon captain. English;
player-facing text stays Spanish. This file is the plan of record for hito `v0.2.0`. Older plan documents
(`PLAN.md`, `WORK-PACKAGES.md`, `HISTORIA-Y-DISENO.md`) stay until the captain retires them; **no story is taken from
them**. The owner's intent is in `docs/intent/` and `~/Workspaces/mc-story/intent/PALABRAS-DEL-DUENO.md` (the chat wins
where they differ) and his answers in `docs/plan/DECISIONS.md`; this plan cites them by path and never restates them.

Contents: 1 hito v0.1.0 · 2 hito v0.2.0 · 3 technical choices · 4 skeleton packages · 5 shared files · 6 the content
interface (story format v1) · 7 worked example · 8 content packages and tomorrow's critical path · 9 design intake list ·
10 itch.io page · 11 cut list · 12 assumptions and questions for the design.

---

## 1. Hito v0.1.0 — done

Head `b47348a` (`0.1.0.23`) on branch `v0.1.0`; real CI run **40366435324** succeeded on that commit (all four Fabric API
profiles: `none`, `0.161.0+26.3`, `0.165.0+26.3` start and pass every server/client check; `0.153.1+26.3` is rejected by
Loader as designed). Package reconciliation and every rehearsal run are recorded in
`docs/integration/MILESTONE-v0.1.0.md`.

What it proves in real use (CI, production JAR, real server and real client under Xvfb):

| Capability | API document | Evidence in CI |
| --- | --- | --- |
| Contract before the menu, typed/clicked "De acuerdo", receipt, replay | `docs/plan/CONTRACT-TEXT.md` | `unallocated-contract*.png`, second process skips the contract without rewriting the receipt |
| Saved session core: arming, acts, budgets, no-cheats rescue `/unallocated siguiente` | `docs/core/INTEGRATION.md` | core armed join / rescue / reload markers |
| Map by code: lobby, hill, path, base, mine; verify and rebuild | `docs/map/INTEGRATION.md` | nine `map-*.png` checkpoints, dedicated build/verify/reload |
| Spyglass bait and screamer | `docs/plan/SPYGLASS-BAIT.md` | four `unallocated-bait-*.png`, step and screamer markers |
| Reversible interface effects (F3, death, splash, loading, advancement, sign, perception) | `docs/plan/INTERFACE.md` | six `unallocated-interface-*.png`, options byte-for-byte |
| Four audio cues | `docs/audio/AUDIO.md` | audio engine markers |
| Epilogue that saves and closes the world | `docs/ending/INTEGRATION.md` | four `unallocated-ending-*.png`, reload as finished |

What it does not have (README): a start from our own button, and a composed 10–15 minute story. The content commands
are debug tools; nothing composes them.

---

## 2. Hito v0.2.0 — the complete jam build

**Goal.** The build the judges download: one JAR (plus Fabric Loader) whose "UNALLOCATED" title button opens a pre-built
story map and plays a 10–15 minute first-person horror experience with a monster, a pocket dimension and a climax with
one of several endings (owner's answers in `docs/plan/DECISIONS.md`, D-01…D-05, D-07; `docs/intent/UPDATES-2026-10-26.md`),
delivered on itch.io with everything the jam rules list (`docs/intent/Itch.io Competition Instructions.txt`).

**Split.**
- **Skeleton (tonight and tomorrow daytime, packages 0.2.1–0.2.5):** everything any design needs, built and proven
  before the design exists: a data-driven story runtime, the launcher and world template, a generic CI playthrough that
  plays whatever story is shipped, world and creature primitives, and the release kit.
- **Content (tomorrow evening, packages C1–C6):** the story, written as data files in the format of §6 by cheap agents
  from the design document, with Java only where the design needs a primitive the skeleton lacks.

**Hito acceptance (the whole of v0.2.0), all in real use:**
1. Real CI green on the final `v0.2.0` commit in all four profiles (the existing checks are kept, none weakened).
2. The CI `play` phase: on a clean client with only Loader and our JAR, the contract is accepted, the title shows the
   UNALLOCATED button, one click opens the template world, the **shipped story** is played by autoplay to **every
   ending listed in `autoplay.runs`**, each run ends on the farewell screen and returns to the title; every `capture`
   screenshot and the gameplay video exist in the release artifact.
3. `python3 scripts/story_check.py` passes on the shipped story (no errors), and the Java loader logs
   `UNALLOCATED story loaded` with no `story invalid` line.
4. The release artifact `unallocated-release-<sha>` contains the JAR, its SHA-256, the world ZIP fallback, the
   screenshots and the video; `scripts/release_bundle.py` turns it into `dist/` with a filled itch page.
5. Human: the owner (or a tester) installs from the itch page on a clean launcher profile and plays once to an ending
   (`docs/plan/DECISIONS.md` D-13: "yo hago las pruebas"). CI cannot judge fear, sound quality or readability.

**Always submittable.** Every package below leaves a build that installs and plays at least as well as v0.1.0. Until
content lands, the shipped story is the neutral default of §6.9 (identical behaviour to v0.1.0: free roam, then the
existing SERVER_CLOSED epilogue). The captain can upload any green `v0.2.0` artifact to itch at any time (D-12).

---

## 3. Technical choices (decided here, with the reason)

1. **Story as data, interpreted by one runtime.** JSON under `src/main/resources/data/unallocated/story/`, one file per
   chapter so several agents can author in parallel without merge conflicts. Reason: tomorrow's authors are cheap
   agents with no Java build; JSON is checkable locally by a Python validator in seconds, and the same files are proven
   in CI by autoplay. Minecraft's own data-file idiom; Gson and Python's `json` both read it.
2. **Chapters ride on the core's existing act slots.** Up to five chapters map in order onto `LOBBY, THEFT, LOOP, POCKET,
   CLIMAX`; `EPILOGUE` closes with the chosen ending. Reason: reuses the saved session, budgets, checkpoints and the
   no-cheats rescue that v0.1.0 proved, with no save-schema change. The slot names are wire/save names only; chapter
   ids are free. A design with more than five rescue checkpoints is the only thing this cannot express (§12, A1).
3. **A vanilla `command`/`function` action as the universal escape hatch.** Everything Minecraft can already do (fill,
   clone across dimensions, place a hand-built structure, summon, effects, particles, playsound, gamerules, items,
   biomes) is available to content without Java, in syntax the owner and every model already know. Every command is
   parsed by the server's dispatcher at load, so a typo fails CI, not the judge. Typed actions exist only where vanilla
   cannot: story flow, two-language text with local placeholders, our interface/audio/bait/ending APIs, blackout,
   creature behaviour and corruption.
4. **Player-facing story text is rendered on the client.** Text actions travel in the core's existing `EffectPayload`
   (ids `story.*`, §6.6) with every language variant; the client picks its own language and fills local placeholders
   (`{agreement}` from the contract receipt, which by design never leaves the client, `{local_time}`, `{world}`). Reason:
   language is an open design question (designer's question 11); this supports Spanish only, English only or both
   without code changes, and the fourth-wall data the brainstorm asks for ("hora real, nombre de usuario, nombre de la
   carpeta del mundo") never leaves the machine.
5. **Template world authored in CI and carried inside the JAR (D-03 A).** CI authors a fresh flat-air world with the
   map, the story's own `build` commands and the session armed, then packs it into the player JAR; the button copies it
   to `saves/` and opens it. The same ZIP is the itch fallback (D-03's "ZIP de respaldo"). If the owner polishes the map
   by hand (D-04), a committed `template/override/world.zip` replaces the authored one, verified the same way.
6. **Autoplay proves content in real use.** A test-only mode of the runtime (system property, never reachable by a
   player) satisfies each beat's trigger the way a player would (teleport into the zone, turn to look, interact, type
   the example input, die and respawn) with story time compressed, so every content commit is played end to end, per
   ending, in CI. Beats can mark `capture` screenshots, which become the itch screenshots.
7. **Two validators, one fixture set.** `scripts/story_check.py` (Python stdlib, runs on Beacon and in CI's unittest
   step) and the Java loader must agree on `src/test/resources/story-fixtures/{valid,invalid}/`; JUnit and Python tests
   both run the fixtures. Reason: cheap agents get the same answer locally that CI gives.
8. **Pocket dimension as data plus two Java primitives.** `unallocated:pocket` is a datapack dimension (flat air, own
   biome for fog/sky); copying uses cross-dimension clone in chunk batches; a seeded `corrupt` pass does the "rot". No
   custom blocks or textures in the skeleton (D-08 A: vanilla effects; risk on unknown GPUs).
9. **The creature has a data-defined body and Java behaviours.** Body = a mannequin (player profile or bundled skin
   texture) or any vanilla `summon` (including display-entity assemblies); behaviours = stalk-while-unseen (reusing
   the bait's visibility logic), chase, hold, face, despawn; triggers near/seen/caught. Reason: the creature's body is
   open (designer's question 10); this covers a human-shaped "artefact", a vanilla mob and a composite body.
10. **No microphone, no network service, no telemetry model** (D-01). Player behaviour reaches the story only through
    triggers (`still`, `sprint`, `sneak`, `look`, `death`, …) and counters. Shaders by third parties (DECISIONS D-08
    answer) are not in the skeleton; vanilla post chains are offered if 26.3 exposes them (§6.6).

---

## 4. Skeleton packages (dispatch order)

Commit messages: `0.2.0.N: <what>` for package N; briefs in `~/Workspaces/mc-inputs/MC-0.2.N-<slug>.md`. A package is
done only when its CI rehearsal passes all four jobs. Sizes are builder wall-clock estimates.

| N | Package | Lane | Size | Depends on | Produces for others |
| --- | --- | --- | --- | --- | --- |
| 1 | `story-check` | Antigravity (Claude Sonnet 4.6) or OpenCode | 45 min | — | `scripts/story_check.py`, fixtures, worked example files, `scripts/map_anchors.json` (names) |
| 2 | `story-runtime` | Codex gpt-6.1-sol, **high** | 60 min + rehearsals | 1 | runtime, action/trigger registries, `story.*` effects, autoplay, `story` client phase, anchors sync test |
| 3 | `launcher-playthrough` | Codex, **medium** | 60 min + rehearsals | 2 | title button, template in JAR, world ZIP, `play` phase over the shipped story, video, release artifact, version 0.2.0 |
| 4 | `world-creature` | Codex, **medium** | 45 min + rehearsals | 2 (after 3 lands) | pocket dimension, `copy`, `corrupt`, `creature` actions and trigger |
| 5 | `release-kit` | OpenCode or Antigravity | 40 min | artifact names of §4.3 (can run any time) | itch page template, upload checklist, `scripts/release_bundle.py` |

Quota (Codex, ~16 % weekly left for both nights; a 2.4 h high run used 15 %): packages 2–4 are about 2.5 h of builder
time, about half of it at medium. The captain should read the quota after package 2 and apply §11 if package 4 would
leave less than ~8 % for tomorrow's content night.

### 4.1 Package 1 — `story-check` (Python validator and fixtures)
- **Delivers:** `scripts/story_check.py` (CLI `python3 scripts/story_check.py [STORY_DIR] [--anchors FILE]
  [--features FILE] [--functions DIR]`, default STORY_DIR `src/main/resources/data/unallocated/story`; exit 0 when no
  errors; one line per finding `<file>#<json-pointer>: error|warning: [<rule>] <message>`), `scripts/test_story_check.py`
  (stdlib `unittest`), fixtures `src/test/resources/story-fixtures/valid/example/` (exactly §7) and
  `src/test/resources/story-fixtures/invalid/<rule>/` (one directory per rule of §6.8, each with `EXPECT.txt` holding
  `<file>#<json-pointer> <rule>`), and `scripts/map_anchors.json` (map names transcribed from `MapLayout`).
- **Acceptance:** all Python tests in `scripts/` pass locally and in the CI unittest step; the example is valid; every
  invalid fixture fails with exactly its expected pointer.

### 4.2 Package 2 — `story-runtime` (the engine; the critical package)
- **Delivers:** `org.unallocated.story` (model, loader, validator, engine, registries, autoplay, debug commands),
  client handlers for the `story.*` effects (§6.6) including custom text for interface effects, data-driven endings in
  `EpilogueAct`/ending client, the neutral default story (§6.9), a JUnit test keeping `scripts/map_anchors.json` equal to
  `MapLayout`, `docs/story/RUNTIME.md` (Java extension API for packages 3–4), a client `story` phase that
  autoplays the §7 example (both endings) on a fresh map world.
- **Interface to packages 3–4 (exact shape):**
  - `StoryActions.register(String do, ActionParser parser)` and `StoryTriggers.register(String type, TriggerParser
    parser)` during mod initialization; a parser validates its JSON at load and returns an executable; unknown `do` or
    `type` is a load error. Packages 3–4 register their own actions here, nothing else in the engine changes.
  - `StoryRuntime.get(server)` (server thread): `fire(String beatId)`, `flag/counter` access through `SessionState`,
    `currentChapter()`, `resolvePosition(JsonElement)` (anchors §6.3), `resolveBox(JsonElement)` (zones §6.4).
  - Server events on `runtime.events()`: `StoryBeatFired(String beatId, String chapter)`,
    `StoryCapture(String name)`, `StoryEnding(String id)`.
  - Autoplay SPI: each trigger implements `simulate(ServerPlayer)`; packages 3–4 implement it for theirs.
  - System properties (test helper JVM only): `unallocated.story.dir` (load a story from a directory instead of the
    JAR), `unallocated.story.autoplay=true`, `unallocated.story.autoplay.run=<index>`.
- **Acceptance:** JUnit (loader agrees with every Python fixture, engine scheduling, persistence of fired beats and
  pending actions across save/reload, rescue/escape, endings); dedicated server smoke: shipped story and example load,
  every `command` parses; client `story` phase: both example runs reach `UNALLOCATED story ending=EXAMPLE_A` and
  `…=EXAMPLE_B`, captures `story-example-sign.png` and `story-example-end.png` saved, farewell shows the example ending
  text; all v0.1.0 checks unchanged.

### 4.3 Package 3 — `launcher-playthrough` (the judge's path, proven)
- **Delivers:** the UNALLOCATED title button (Fabric screen events, no new title mixin unless unavoidable): continue the
  newest unfinished copy or start a new copy; a "connecting" screen while the template is copied off the render thread;
  vanilla world opening. Template authoring in CI (dedicated server, flat air + `unallocated:lobby` biome, `build all`,
  story `build`, arm, clean stop, deterministic ZIP), its injection into the player JAR as
  `unallocated/template/world.zip`, `verify_jar.py` checks for it, the `template/override/world.zip` path (D-04).
  A `play` client phase: button → template world → shipped story autoplayed once per `autoplay.runs` entry → farewell →
  title; ffmpeg capture of the Xvfb display during `play` (profile `none`) to `gameplay.mp4`. Version `0.2.0`; README
  "Cómo comenzar" in Spanish for the button and the ZIP fallback.
- **Artifact names (interface to package 5, exact):** upload `unallocated-release-<sha>` (profile `none`, success only)
  containing `unallocated-<version>.jar`, `unallocated-<version>.jar.sha256`, `UNALLOCATED-world.zip` (one top folder
  `UNALLOCATED/` with `level.dat`), `screenshots/*.png` (every `story-*.png` and `map-*.png` and
  `unallocated-title-button.png`), `video/gameplay.mp4` (absent only if ffmpeg failed; a log line says so), and
  `release.json` = `{"version","minecraft","loader","fabric_api_bundled","jar","sha256","world_zip","commit","run"}`.
- **Acceptance:** `play` phase markers and screenshots for the shipped (default) story; the JAR contains exactly one
  template; the template's session is armed, unstarted, Adventure, cheats off; second click after an ending starts a
  new copy; existing phases unchanged; client step and job timeouts raised only as needed (job ≤ 45 min).

### 4.4 Package 4 — `world-creature`
- **Delivers:** `data/unallocated/dimension/pocket.json`, `dimension_type/pocket.json`, biome `unallocated:pocket`
  (placeholder fog/sky: neutral grey, to be set by content), present in template worlds; actions `copy`, `corrupt`,
  `creature`; trigger `creature`; autoplay simulation for them; example additions in a second fixture
  `story-fixtures/valid/world/` exercised in the `story` phase (teleport into the pocket, copy, corrupt, creature
  stalk/chase/caught), screenshots `story-world-pocket.png`, `story-world-creature.png`.
- **Acceptance:** JUnit for corruption determinism and creature visibility/steering; client `story` phase runs the
  world fixture to its ending with both captures; creatures are never written to the save (as the bait) and are
  restored by the runtime on resume.

### 4.5 Package 5 — `release-kit`
- **Delivers:** `docs/release/ITCH-PAGE.md` (Spanish page body, every section the jam rules list, `{{TOKENS}}` for
  generated values and `{{DESIGN:…}}` tokens for the design's text), `docs/release/CHECKLIST.md` (English: upload,
  cover image, screenshots, video, jam submission, final clean-install test), `scripts/release_bundle.py`
  (stdlib; input = a directory holding the downloaded `unallocated-release-<sha>` artifact; output `dist/` with the
  files renamed for upload and `dist/itch-page.md` with generated tokens filled and design tokens listed as missing)
  and `scripts/test_release_bundle.py` using a synthetic artifact.
- **Acceptance:** Python tests pass locally and in CI; the filled page has no generated token left; unknown or missing
  artifact files are errors.

---

## 5. Shared files: one owner at a time

Packages 2→3→4 run **sequentially** on `v0.2.0`; a package edits a shared file only in the way listed. The v0.1.0 merge
that hid three mixin registrations through duplicate JSON keys is guarded by `scripts/mixin_packages.py` and the JSON
uniqueness test; no package may weaken those checks.

| Shared file | Who may edit (in order) and how |
| --- | --- |
| `src/main/resources/fabric.mod.json` | 2, 3, 4: append one entry for **their own** new mixin config (or entrypoint) to the existing arrays; never rewrite, reorder or remove entries. Content packages: never. |
| Mixin configs `*.mixins.json` | each package owns only the configs it creates; existing configs are read-only except a one-line addition by the package that owns the feature. |
| `assets/unallocated/lang/*.json` (all 8 files) | 3 adds `menu.unallocated.*` (button, connecting, continue/new). Content: only package C5 (UI wording), never story text (story text lives in story files). Every key added to `es_es.json` is added to `en_us.json` (Spanish default, existing convention). |
| `assets/unallocated/features.json` | 2 (enables the five act slots); content package C1 sets `effects.*` as the design needs. |
| `UnallocatedMod.java`, `UnallocatedClient.java` | 2, 3, 4: append one initializer call each. |
| `src/clientSmoke/**` (single orchestrator `TitleScreenSmokeTest`, `fabric.mod.json` of the helper) | 2 adds the `story` phase scenario; 3 adds `play`; 4 adds the world fixture run inside `story`. One gametest entrypoint stays. |
| `scripts/smoke_test.py` (phases, evidence, screenshots) | 2 (`story` phase), 3 (`play` phase, release artifact, video), 4 (world evidence). Expected markers for `play` are **derived from the shipped story files**, so content never edits this file. |
| `.github/workflows/build.yml` | 2 may raise the client step timeout; 3 owns everything else for v0.2.0 (timeouts, ffmpeg, release artifact). |
| `scripts/verify_jar.py` | 3 (template entry, size budget). |
| `build.gradle`, `gradle.properties` | 2 (helper JVM properties for `story`), 3 (phase property, template, `version=0.2.0`). |
| `README.md` | 3 ("Cómo comenzar", version); captain at the hito close. |
| `src/main/resources/data/unallocated/story/**` | 2 creates the neutral default; content packages C1–C3 replace it, one chapter file per agent. |
| `scripts/story_check.py`, `scripts/map_anchors.json`, fixtures | 1 creates; 2 and 4 may correct it where it disagrees with §6 (and must update §6 in this file in the same commit if the format itself changes). |
| `docs/plan/milestones.md` §6 | the format is changed only by 2 or 4, in the same commit as the code, never by content packages. |

---

## 6. The content interface — story format v1

Everything in this section is exact. Unknown keys anywhere are errors (they catch typos). All times are seconds
(decimals allowed). Comments are not allowed in JSON; this document explains every key.

### 6.1 Files

```
src/main/resources/data/unallocated/story/story.json            global settings, chapter order, anchors, zones, endings
src/main/resources/data/unallocated/story/chapters/<id>.json    one chapter per file (filename = chapter id)
src/main/resources/data/unallocated/function/**.mcfunction      optional vanilla functions, called by "function"
src/main/resources/data/unallocated/structure/**.nbt            optional hand-built structures (structure block export)
```

Identifiers: chapter `[a-z][a-z0-9_]{0,23}`; beat `<chapter>.<name>` with name `[a-z0-9_]{1,32}`; flag and counter
`[a-z][a-z0-9_.]{0,47}`; ending `[A-Z][A-Z0-9_]{0,31}`; capture `[a-z0-9][a-z0-9-]{0,39}`; story anchor/zone keys
`[a-z][a-z0-9_.]{0,47}` and different from every map name in `scripts/map_anchors.json`.

### 6.2 Text

A text is an object with one string per language listed in `story.json` `languages` (all required), plus optional
style: `{"es": "…", "en": "…", "color": "#rrggbb", "bold": false, "italic": false, "obfuscated": false}`.
`\n` is allowed in chat and farewell lines only. Placeholders (anything else in braces is an error; no literal braces):

| Placeholder | Filled by | Value |
| --- | --- | --- |
| `{player}` | client | the player's name |
| `{agreement}` | client | the text typed in the contract (original capitalization); `agreement_fallback` if the player clicked |
| `{local_time}` | client | the player's clock, `HH:mm` |
| `{world}` | client | the save folder name |
| `{elapsed_min}` | server | whole minutes of session time |
| `{deaths}` | server | deaths in this session |
| `{counter:<id>}` | server | the counter's value |

Language selection: `language_select: "fixed"` shows `default_language` always; `"client"` shows the client's
language when it is one of `languages` (any `es_*` locale counts as `es`), otherwise `default_language`.

### 6.3 Positions

A position is one of:
- a name string: a map checkpoint or landmark from `scripts/map_anchors.json` (e.g. `"lobby.spawn"`, `"mine.end"`),
  or a key of `story.json` `anchors`;
- `{"anchor": "<name>", "offset": [dx, dy, dz]}`;
- `{"pos": [x, y, z], "dimension": "overworld", "yaw": 0, "pitch": 0}` (`dimension` `overworld` or `pocket`,
  default `overworld`; yaw/pitch optional; teleports go to the block centre `x+0.5, y, z+0.5`).

### 6.4 Zones (areas)

A zone is a name (map trigger from `map_anchors.json`: `lobby.exit`, `hill.bait_path`, `base.theft_zone`,
`mine.entrance`; or a key of `story.json` `zones`), or `{"box": [x1, y1, z1, x2, y2, z2], "dimension": "overworld"}`
(inclusive block corners in any order), or `{"section": "lobby|hill|path|base|mine"}` (a whole map section).

### 6.5 Triggers (`"when"` of a beat)

| `type` | Keys | Fires when | Autoplay does |
| --- | --- | --- | --- |
| `chapter_start` | — | the chapter is entered (after `on_enter`) | nothing |
| `after` | `seconds`, optional `beat` | `seconds` after chapter entry, or after `beat` fired | waits (scaled) |
| `flag` | `flag` | the flag is set | waits for it |
| `zone` | `zone`, optional `seconds` | the player is inside for `seconds` (default 0) continuously | teleports to the zone's centre floor |
| `look` | `at` (position), `degrees` (default 8), `seconds` (default 0.5), `max_distance` (default 96), `spyglass` (default false) | the player looks at the point with line of sight (through a spyglass if `spyglass`) | moves within range if needed and turns the player to it |
| `interact` | `at` (position) | the player right-clicks that block | moves next to it and performs the interaction |
| `use_item` | `item` | the player uses that item | gives it if missing and uses it |
| `has_item` | `item`, `count` (default 1) | the inventory holds it | gives it |
| `break` | `block`, optional `zone` | the player breaks that block type | breaks one |
| `chat` | `pattern` (regex), `hide` (default false) | the player sends a chat line (not a command) matching it; `hide` keeps it out of chat | sends `autoplay.input` |
| `still` / `sprint` / `sneak` | `seconds` | the player stays still / sprints / sneaks that long | simulates it |
| `death` / `respawn` | — | the player dies / respawns | kills / respawns the player |
| `bait` | `event`: `step` / `screamer` / `gone` | the spyglass bait reports it | forces the step / scare |
| `creature` (package 4) | `creature`, `event`: `near` / `seen` / `caught`, `distance`, `seconds` | the creature is near / seen / reaches the player | moves the creature |
| `any` | `of`: list of triggers | any of them fires | simulates the first |

### 6.6 Actions (`"actions"` of a beat, `on_enter`, `on_resume`, `escape`)

Each action is `{"at": <seconds after the beat fires, default 0>, "do": "<name>", …}`. `at` always means time;
a position key is named `where` unless the table names it (`to`, `lure`, `sign`). Actions run in `at` order
(ties keep file order). `next_chapter` and `end` cancel every pending action of the old chapter, so put them last.

| `do` | Keys | Effect |
| --- | --- | --- |
| `flag` | `set` or `clear` | sets/clears a flag (saved) |
| `counter` | `id`, `add` or `set` | changes a counter (saved) |
| `next_chapter` | — | advances to the next chapter (or to the ending if none: `fallback_ending`) |
| `end` | `ending` | ends the story with that ending (farewell, save, close, title) |
| `cancel` | `beat` | cancels that beat's pending actions |
| `title` | `title`, optional `subtitle`, `fade_in` (0.5), `stay` (3), `fade_out` (1) | client title |
| `actionbar` | `text` | client action bar |
| `chat` | `text`, optional `from` (a text: fake sender name) | a chat line, as `<from> text` or as a system line |
| `blackout` | `fade_in` (0.1), `hold` (0.5), `fade_out` (0.4), `color` (`#000000`) | full-screen cut |
| `interface` | `effect` (`F3`, `DEATH`, `SPLASH`, `LOADING`, `ADVANCEMENT`, `SENSITIVITY`, `FOV`, `GAMMA`, `SIGN`), `seconds`, `generation` (0–5), `strength` (−1…1), `leak` (`es`/`en`/`mixed`), `seed`, `sign` (position, SIGN only), `advancement` (id, optional), `text` (optional text replacing the generated leak) | `docs/plan/INTERFACE.md`; a refusal is logged and skipped, never retried |
| `audio` | `cue` (`ambient`, `threads`, `muffle`, `screamer`, `stop`), optional `where` (position; default the player) | `docs/audio/AUDIO.md` |
| `post_effect` | — | Rejected at load in 26.3: no verified public API for applying a timed story post chain. |
| `teleport` | `to` (position), `blackout` (default false) | moves the player, across dimensions too |
| `bait` | `op`: `place` (keys `lure` position, `distances` list, `scare` bool, `appear_delay`, `fallback_after`, `max_lifetime`, `seed`), `step`, `scare`, `clear` | `docs/plan/SPYGLASS-BAIT.md` |
| `command` | `run` (no leading `/`) | runs a vanilla command as the player, at the player, permission level 2, output hidden. Forbidden roots: `stop op deop kick ban ban-ip pardon pardon-ip whitelist save-all save-off save-on reload datapack publish debug perf jfr transfer tick` |
| `function` | `id` (`unallocated:<path>`) | runs that `.mcfunction` the same way |
| `copy` (package 4) | `from` (zone), `to` (position: the destination's minimum corner) | copies blocks, across dimensions |
| `corrupt` (package 4) | `area` (zone), `rules`: list of `{"from": [blocks], "to": [blocks], "ratio": 0–1}`, `seed`, `over_seconds` (0 = at once) | seeded block replacement, optionally spreading over time |
| `creature` (package 4) | `op`: `spawn` (`id`, `body`, `where`, `facing`: `player`/yaw), `stalk` (`id`, `step`, `min_distance`), `chase` (`id`, `speed`, `catch_distance`), `hold`, `face_player`, `despawn` | body: `{"mannequin": {"profile": "player" or "<name>", "texture": "unallocated:<png path>", "scale": 1.0}}` or `{"summon": "<entity id>[{snbt}]"}` |

Server → client transport (package 2): the core's `EffectPayload(effectId, args, durationTicks)` with ids
`story.title`, `story.actionbar`, `story.chat`, `story.blackout`, `story.interface`, `story.post_effect`,
`story.ending_text`; `args` carries every language variant, style and server placeholder values. No new payload type.

### 6.7 `story.json` and chapter files

`story.json` keys: `format` (1), `languages` (non-empty subset of `es`, `en`), `default_language`,
`language_select` (`fixed`/`client`), `agreement_fallback` (text), `chapters` (1–5 chapter ids, in order), `anchors`
(name → `{"pos", "dimension", "yaw", "pitch"}`), `zones` (name → `{"box", "dimension"}`), `endings` (id →
`{"title": text, "lines": [text…], "splash": text, "quote_agreement": bool}` or `{"builtin": true}` for the existing
`SERVER_CLOSED` farewell), `fallback_ending` (used when the last chapter advances or expires without `end`), `build`
(`{"forceload": [[x1, z1, x2, z2, "overworld"|"pocket"]…], "run": [commands…]}` executed once when the template is
authored, after the map is built), `autoplay` (`{"time_scale": 0.25, "runs": [{"ending": "<ID>", "skip": [beat ids]}…]}`;
every ending must appear in exactly one run). Required: `format`, `languages`, `default_language`, `language_select`,
`chapters`, `endings`, `fallback_ending`, `autoplay`. Optional: `agreement_fallback` (required only when some text uses
`{agreement}`), `anchors`, `zones`, `build`. `build.run` commands run as the server console (permission level 2, no
player, positioned at the world spawn), so they use absolute coordinates.

Chapter file keys: `id`, `budget_seconds` (30–1200; on expiry `escape` runs and the chapter advances),
`checkpoint` (position: respawn point, and where `/unallocated siguiente` lands), `beats` (may be empty) — required;
optional `respawn` (position, overrides `checkpoint` after death), `on_enter`, `on_resume` (after save/reload or
reconnect), `escape` (lists of actions, default empty).

Beat keys: `id`, `when` (trigger), optional `if` (`{"flags": [], "not_flags": [], "beats": [], "not_beats": []}`, all
must hold when the trigger fires), optional `repeat` (`{"max": n, "cooldown_seconds": s}`; absent = fires once),
`actions`, optional `capture` (`{"name": "<capture>", "at": seconds}`: CI saves `story-<name>.png`), optional
`autoplay` (`{"input": "<chat text>", "wait_seconds": 1}`).

Persistence: fired beats, flags, counters, the chapter and the pending actions of a fired beat are saved; after a reload
the remaining actions run from where they stopped and `on_resume` runs once.

### 6.8 Validation rules (both validators; each has an invalid fixture)

`unknown-key`, `bad-id`, `duplicate-id`, `missing-chapter-file`, `chapter-id-mismatch`, `too-many-chapters` (>5),
`missing-language`, `unknown-placeholder`, `unknown-anchor`, `unknown-zone`, `unknown-beat-ref`, `unknown-ending`,
`ending-unreachable` (no `end` action and not the fallback), `flag-never-set` (used in `if`/`flag` but set nowhere),
`bad-command` (empty, leading `/`, forbidden root), `unknown-function` (no `.mcfunction` file), `bad-time` (negative,
`at` > 600, budget outside 30–1200), `unknown-trigger`, `unknown-action`, `chat-without-input` (or input not matching),
`duplicate-capture`, `autoplay-ending-coverage`, `bad-json` (unparseable file), `bad-value` (wrong type,
out-of-range number, bad colour or id syntax), `feature-off` (an `interface` SENSITIVITY/FOV/GAMMA action while
`effects.options` is false, or bait `scare` while `effects.screamer` is false). Warnings (not errors): total budget
outside 600–900 s (10–15 min), more than two `screamer` cues (AUDIO.md limit), title text over 40 characters.
Java alone additionally checks: every `command` parses on the real dispatcher; every structure/function/texture exists;
dimensions exist.

### 6.9 Neutral default story (shipped until content replaces it)

`story.json`: languages `["es"]`, one chapter `free_roam` (checkpoint `lobby.spawn`, budget 420 s, no beats),
endings `{"SERVER_CLOSED": {"builtin": true}}`, fallback `SERVER_CLOSED`, autoplay one run to `SERVER_CLOSED`.
Behaviour equals v0.1.0. No new player-facing words.

---

## 7. Worked example (placeholders only; also the fixture `story-fixtures/valid/example/`)

`story.json`:

```json
{
  "format": 1,
  "languages": ["es", "en"],
  "default_language": "es",
  "language_select": "client",
  "agreement_fallback": {"es": "[ES] acuerdo-por-clic", "en": "[EN] click-agreement"},
  "chapters": ["first", "second"],
  "anchors": {
    "example.mark": {"pos": [90, 64, 20], "dimension": "overworld", "yaw": -90, "pitch": 0}
  },
  "zones": {
    "example.end_zone": {"box": [98, 64, 18, 100, 67, 22], "dimension": "overworld"}
  },
  "endings": {
    "EXAMPLE_A": {
      "title": {"es": "[ES] Final A título", "en": "[EN] Ending A title"},
      "lines": [{"es": "[ES] Final A línea, {agreement}", "en": "[EN] Ending A line, {agreement}"}],
      "splash": {"es": "[ES] Final A splash", "en": "[EN] Ending A splash"},
      "quote_agreement": true
    },
    "EXAMPLE_B": {
      "title": {"es": "[ES] Final B título", "en": "[EN] Ending B title"},
      "lines": [{"es": "[ES] Final B línea, {elapsed_min} min", "en": "[EN] Ending B line, {elapsed_min} min"}],
      "splash": {"es": "[ES] Final B splash", "en": "[EN] Ending B splash"},
      "quote_agreement": false
    }
  },
  "fallback_ending": "EXAMPLE_A",
  "build": {"forceload": [], "run": ["setblock 90 64 22 minecraft:oak_sign"]},
  "autoplay": {
    "time_scale": 0.25,
    "runs": [
      {"ending": "EXAMPLE_A", "skip": ["second.choose_b"]},
      {"ending": "EXAMPLE_B", "skip": ["second.choose_a"]}
    ]
  }
}
```

`chapters/first.json`:

```json
{
  "id": "first",
  "budget_seconds": 240,
  "checkpoint": "lobby.spawn",
  "on_enter": [
    {"at": 0, "do": "audio", "cue": "ambient"},
    {"at": 2, "do": "title", "title": {"es": "[ES] Título 1", "en": "[EN] Title 1"},
     "subtitle": {"es": "[ES] Subtítulo 1, {player}", "en": "[EN] Subtitle 1, {player}"}}
  ],
  "on_resume": [{"at": 0, "do": "audio", "cue": "ambient"}],
  "escape": [{"at": 0, "do": "teleport", "to": "mine.entrance", "blackout": true}],
  "beats": [
    {
      "id": "first.leave_lobby",
      "when": {"type": "zone", "zone": "lobby.exit"},
      "actions": [
        {"at": 0, "do": "actionbar", "text": {"es": "[ES] Texto 1", "en": "[EN] Text 1"}},
        {"at": 1, "do": "flag", "set": "example.left_lobby"}
      ]
    },
    {
      "id": "first.look_hill",
      "when": {"type": "look", "at": "hill.bait", "degrees": 10, "seconds": 0.5},
      "if": {"flags": ["example.left_lobby"]},
      "actions": [{"at": 0, "do": "audio", "cue": "threads", "where": "hill.bait"}]
    },
    {
      "id": "first.sign",
      "when": {"type": "interact", "at": {"pos": [90, 64, 22]}},
      "actions": [
        {"at": 0, "do": "interface", "effect": "SIGN", "seconds": 5, "generation": 2, "strength": 0.5,
         "leak": "es", "seed": 17, "sign": {"pos": [90, 64, 22]},
         "text": {"es": "[ES] Letrero", "en": "[EN] Sign"}},
        {"at": 1, "do": "chat", "from": {"es": "[ES] Remitente", "en": "[EN] Sender"},
         "text": {"es": "[ES] Mensaje {local_time}", "en": "[EN] Message {local_time}"}},
        {"at": 3, "do": "next_chapter"}
      ],
      "capture": {"name": "example-sign", "at": 0.5}
    }
  ]
}
```

`chapters/second.json`:

```json
{
  "id": "second",
  "budget_seconds": 180,
  "checkpoint": "example.mark",
  "on_enter": [
    {"at": 0, "do": "blackout", "fade_in": 0.1, "hold": 0.5, "fade_out": 0.4},
    {"at": 0.2, "do": "teleport", "to": "example.mark"},
    {"at": 1, "do": "command", "run": "effect give @s minecraft:darkness 4 0 true"}
  ],
  "on_resume": [],
  "escape": [{"at": 0, "do": "end", "ending": "EXAMPLE_A"}],
  "beats": [
    {
      "id": "second.choose_a",
      "when": {"type": "chat", "pattern": "(?i)^\\[a\\]$", "hide": true},
      "actions": [{"at": 0, "do": "flag", "set": "example.chose_a"}],
      "autoplay": {"input": "[a]"}
    },
    {
      "id": "second.choose_b",
      "when": {"type": "zone", "zone": "example.end_zone", "seconds": 1},
      "if": {"not_flags": ["example.chose_a"]},
      "actions": [{"at": 0, "do": "flag", "set": "example.chose_b"}]
    },
    {
      "id": "second.end_a",
      "when": {"type": "flag", "flag": "example.chose_a"},
      "actions": [
        {"at": 0, "do": "audio", "cue": "muffle"},
        {"at": 1, "do": "end", "ending": "EXAMPLE_A"}
      ],
      "capture": {"name": "example-end", "at": 0.5}
    },
    {
      "id": "second.end_b",
      "when": {"type": "flag", "flag": "example.chose_b"},
      "actions": [{"at": 1, "do": "end", "ending": "EXAMPLE_B"}]
    }
  ]
}
```

---

## 8. Content packages (tomorrow evening) and the critical path

The design document (§9) arrives from about 20:00Z on 2026-10-28; the jam closes **04:59Z on 2026-10-29**.

| C | Package | Lane | Input | Output |
| --- | --- | --- | --- | --- |
| C1 | `story-global` | Antigravity (Sonnet) | design §§ global, endings, chapters list | `story.json`, `features.json` effects |
| C2 | `chapter-<id>` ×N (parallel, one agent per chapter) | Antigravity / OpenCode | the chapter's beat table | `chapters/<id>.json` |
| C3 | `places` | Antigravity (Sonnet) or owner by hand | the places list | `data/unallocated/function/story/build*.mcfunction`, optional `.nbt`, new anchors/zones (into C1's file via the captain) |
| C4 | `primitive-<name>` (only if the design needs a missing trigger/action) | Codex medium | the gap, exactly | Java registered through §4.2's registries, fixture, §6 row |
| C5 | `ui-text` | OpenCode | contract changes, button/connecting wording | lang files, `CONTRACT-TEXT.md` (+ `scripts/contract_text.py --check`) |
| C6 | `itch-text` | OpenCode | description, credits, warnings | the `{{DESIGN:…}}` values for `release_bundle.py` |

Each content brief is generated tomorrow from the template `~/Workspaces/mc-inputs/MC-0.2.C-content-TEMPLATE.md`.
Local acceptance: `python3 scripts/story_check.py` clean. Real acceptance: rehearsal green, the `play` phase reaches every
ending, the captures look right (captain opens them).

**Critical path:** design intake complete (target 22:00Z) → C1 (20 min) → C2 ×N and C3 in parallel (45 min) → captain
merges and runs `story_check.py` → rehearsal (30–40 min) → fixes (one more rehearsal) → **content freeze 02:30Z** →
push `v0.2.0`, real CI green → `release_bundle.py` → owner uploads to itch, plays once from the page on a clean profile
→ submit by **04:00Z** (one hour of margin). C4 is the only Codex item and must be requested by 22:30Z or dropped.

---

## 9. Design intake list (what the design document must state)

Write it as tables; every line becomes data without guessing. "Text" always means the exact words in every chosen
language. Positions use existing map names (§1 table of `docs/map/INTEGRATION.md`, landmarks there) or a new place from
section D.

**A. Global**
1. Languages shown to the player (`es`, `en`, both) and whether the client language chooses (§6.2).
2. Text when the player clicked instead of typing "De acuerdo" (`agreement_fallback`), if `{agreement}` is used.
3. Effects allowed in the session: perception drift (FOV/sensitivity/darkness: `effects.options`), screamers
   (`effects.screamer`), and how many screamers at most (AUDIO.md recommends two).
4. Target length; per chapter minutes (the sum is the session; 10–15 min).
5. Contract text changes (designer's question 2), or "unchanged" — exact new Spanish text if changed.
6. The button and connecting-screen wording, or "unchanged" ("UNALLOCATED" / "Conectando al servidor…").

**B. Chapters (1–5, in order)** — for each: id; purpose in one line; where it happens; minutes; checkpoint (respawn
and rescue landing); what happens on entry (`on_enter`), after reload (`on_resume`), and if the player is stuck or
uses `/unallocated siguiente` (`escape`: usually a teleport onward or an ending).

**C. Beats** — for each: id; chapter; **trigger** (one row of §6.5 with its values: which zone, what to look at and how
long, which item/block, after how many seconds since what, which typed words and one example input); **condition**
(flags required/forbidden); **effect** as a timed list (second → action of §6.6 with its values: text kind and exact
text, cue, interface effect and parameters, blackout timings, teleport target, bait settings, creature op, world
change, flag set); repeatable or once (cooldown); screenshot for itch (capture name) or not.

**D. Places** — for each new place: where (an existing section, or free space: overworld x 200–1000 at y 60–120, or the
pocket dimension at any coordinates); size; what it is made of (blocks), layout sketch with named points (anchors with
facing) and named areas (zones); signs (exact lines); chests (items); light level. Or: "the owner builds it by hand"
(then a structure block export `.nbt` and its origin).

**E. Pocket dimension** — player-facing name (designer's question 3) if shown; which map sections it copies and when
(authoring or a beat); the rot: which blocks become which, how much, at once or spreading; fog/sky colour; whether
the player falls in (beat) and how they leave.

**F. Creature** — body (designer's question 10): the player's own skin, another skin (64×64 PNG with licence), a
vanilla entity, or a composite of display entities (describe parts); behaviours per beat (spawn where, stalk, chase
speed, hold); what "caught" does (death, teleport, ending).

**G. Death and after** (designer's questions 5 and 6) — whether death is mandatory and which beat causes it (`kill`
command), the death-screen line (interface `DEATH` text), immediate respawn or the vanilla button, where the player
respawns, and every change after it (game mode, effects, visible "threads" as blocks where, abilities as items).

**H. Endings** (question 7) — for each: id; the condition (which beat ends it, from which flags); farewell title and
lines; title splash; whether to quote the agreement. Which ending is the fallback when time runs out.

**I. Presentation** — sounds beyond the four cues (vanilla sound ids with pitch/volume, or new OGG files with licence and
credit); interface effects per beat; any third-party shader (question for the owner, §12).

**J. Itch page** — short description (Spanish, 2–4 sentences); team members and roles; credits for any external asset;
content warnings beyond those of the contract; known issues the owner accepts.

---

## 10. The itch.io page: requirement → producer

| Jam rule item | Produced by | How |
| --- | --- | --- |
| Mod file and dependency list | package 3 artifact, package 5 bundle | `unallocated-0.2.0.jar` + `.sha256`; dependencies: Fabric Loader only (Fabric API inside, D-02) |
| Loader and exact version | package 5 (from `gradle.properties` via `release.json`) | Minecraft Java 26.3 exactly, Fabric Loader ≥ 0.20.1, Java 25 from the launcher |
| Installation and how to start | package 5 template (Spanish), package 3 behaviour | install Loader, copy JAR, title button "UNALLOCATED" |
| Map and setup | package 3 | template inside the JAR; `UNALLOCATED-world.zip` fallback with instructions |
| Short description | design (J) → C6 | `{{DESIGN:DESCRIPTION}}` |
| Team and external credits | design (J) → C6; Fabric API credit from README | `{{DESIGN:TEAM}}`, `{{DESIGN:CREDITS}}` |
| Known bugs and content warnings | contract Appendix C + design (J) → C6; captain after the final test | `{{DESIGN:WARNINGS}}`, `{{KNOWN_ISSUES}}` |
| Screenshots | package 3 `play` phase (`capture` beats, map tour) | `dist/screenshots/` (owner picks 3–5 and a 630×500 cover) |
| Short gameplay video | package 3 ffmpeg of the `play` phase (time-compressed, no sound) | `dist/gameplay.mp4`; a real-time take with sound recorded by the owner is better (CHECKLIST) |

---

## 11. Cut list (first cut first) and what the owner loses

1. **Package 4's creature part.** Loses: a monster that stalks/chases with its own triggers. Fallback: the monster is
   the bait mannequin (place/step/scare) plus `summon`ed entities moved by `command` (`tp`) in timed beats.
2. **Package 4's pocket dimension, `copy`, `corrupt`.** Loses: a real separate dimension and seeded rot. Fallback: the
   "pocket" is a far overworld region built by `build` commands (`clone`, `fill … replace`), with its own biome JSON
   (data only, cheap lane) applied by `fillbiome` for fog and sky.
3. **Package 3's video and multi-ending runs.** Loses: automatic video and real-play proof of endings after the first.
   Fallback: the owner records the video; other endings are proven only by the validator.
4. **Package 3's continue/new choice.** Loses: resuming a quit session from the button. Fallback: always a new copy;
   resuming via Singleplayer.
5. Never cut: package 2 (everything depends on it), the template/button (D-03; judges' installation), package 1.

---

## 12. Assumptions and questions the design may contradict

- **A1.** At most five chapters (rescue checkpoints). Beats per chapter are unlimited. More needs a core change.
- **A2.** Single player, one participant (UPDATES-2026-10-26 item 5). Puppets for other players are out of scope.
- **A3.** No microphone, voice or network service (D-01). If the design wants microphone input, it is a new primitive
  and a risk on unknown machines; question for the owner.
- **A4.** The story plays in the existing map plus new places built by commands/structures; the five existing sections
  stay where `MapLayout` puts them (D-04: coordinates must not move). Removing or rebuilding a section is a Java change.
- **A5.** The pocket's technical id is `unallocated:pocket`; its shown name is the design's.
- **A6.** "Threads" visible after death (owner's words in PALABRAS-DEL-DUENO.md) are expressible as blocks placed by a
  beat (cobweb/string/tripwire via `command`/`function`) and a post effect if available; no custom renderer.
- **A7.** The creature has no pathfinding AI; it moves by steering/teleport steps. A design needing navigation through
  doors or mazes is a C4 primitive.
- **A8.** Judges have default render distance or more; the bait already shrinks to fit short render distances.
- **A9.** Third-party shaders (DECISIONS D-08 answer) are not bundled: extra dependencies hurt installation. Question:
  does the owner want an optional shader recommendation on the itch page instead?
- **A10.** Story text is not in lang files; translators edit story files. UI text (button, contract) stays in lang files.
- **A11.** The template is authored fresh in every CI run from code; hand polishing (D-04) needs `template/override/`.
- **A12.** `{agreement}` uses the receipt on the client only (existing privacy rule in `docs/ending/INTEGRATION.md`).
