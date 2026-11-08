# Brief MC-0.2.1 — `story-check`: the story validator and its fixtures

**Recommended lane:** Antigravity with Claude Sonnet 4.6 (fallback: OpenCode with the strongest free model). Why: the
work is pure Python stdlib and JSON, fully specified below, no Java and no game; it needs care with many small rules,
which Sonnet handles well, and it must not spend Codex quota.

## Who you are, where, for whom
You are a careful Python engineer on the UNALLOCATED project (a first-person horror mod for Minecraft Java 26.3,
Fabric) by Alexander Kholodov (GitHub `alexanderkholodov1`). You work in the git worktree `~/Workspaces/mc-hito`,
branch `v0.2.0`, on the machine Beacon (Linux, 3.3 GB RAM). The Beacon captain dispatched you and verifies your delivery.
**Never run Gradle, Java or Minecraft here, and never write under `/tmp`.** Python 3 is fine. English in code and docs.

## Goal and why
Tomorrow evening the story of the game will be written as JSON data files by several cheap agents in parallel, from a
design document, with only a few hours before the jam closes. A real CI run takes 20–40 minutes, so every author must be
able to check their file locally in seconds. You build that checker, `scripts/story_check.py`, and the fixtures that
define right and wrong. The Java runtime (next package, Codex) will be required to agree with your fixtures exactly, so
precision matters more than speed.

## Inputs (read these; do not summarize the owner's documents)
- `docs/plan/milestones.md` **§6 (the format, normative) and §7 (the worked example)**. §6.8 lists the rules.
- `docs/plan/INTERFACE.md`, table "Name | Maximum duration" (maximum seconds per interface effect: F3 3 s; DEATH,
  SPLASH, LOADING 30 s; ADVANCEMENT, SENSITIVITY, FOV, GAMMA, SIGN 5 s).
- `docs/audio/AUDIO.md` (the four cues; at most two screamers per session).
- `src/main/java/org/unallocated/map/MapLayout.java` lines 40–80 (names of checkpoints, triggers, landmarks; the loop
  at line 75 makes `path.torch.0` … `path.torch.4`; sections are `lobby`, `hill`, `path`, `base`, `mine`).
- `src/main/resources/assets/unallocated/features.json`.
- Existing test style: `scripts/test_smoke_test.py`, `scripts/test_mixin_packages.py` (stdlib `unittest`, run by CI with
  `python3 -B -m unittest discover -s scripts -p 'test_*.py'`).

## Files you may create or edit (nothing else)
- `scripts/story_check.py` (new)
- `scripts/test_story_check.py` (new)
- `scripts/map_anchors.json` (new): names only, `{"checkpoints": [...], "landmarks": [...], "triggers": [...],
  "sections": ["lobby","hill","path","base","mine"]}`, transcribed from `MapLayout.java` (sorted lists). The next package
  adds a JUnit test that keeps it equal to the Java source.
- `src/test/resources/story-fixtures/valid/example/story.json`, `.../chapters/first.json`, `.../chapters/second.json`:
  **exactly** the three JSON blocks of milestones.md §7 (same content; you may reformat whitespace).
- `src/test/resources/story-fixtures/invalid/<rule>/…`: one directory per error rule of §6.8, each a copy of the example
  with **one** change that breaks exactly that rule, plus `EXPECT.txt` (one line: `<file relative to the fixture
  dir>#<json-pointer> <rule>`, e.g. `chapters/first.json#/beats/0/actions/0/do unknown-action`). If the rule needs a
  features file (`feature-off`) put `features.json` in that fixture directory.

## Exact behaviour of `scripts/story_check.py`
- CLI: `python3 scripts/story_check.py [STORY_DIR] [--anchors FILE] [--features FILE] [--functions DIR]`.
  Defaults: STORY_DIR `src/main/resources/data/unallocated/story`, anchors `scripts/map_anchors.json`, features
  `src/main/resources/assets/unallocated/features.json`, functions `src/main/resources/data/unallocated/function`
  (paths relative to the repository root, found from the script's own location). If `STORY_DIR/features.json` exists it
  overrides `--features` (fixtures use this).
- Output: one line per finding, sorted by file then pointer: `<file relative to STORY_DIR>#<json pointer>: error:
  [<rule>] <message>` or `…: warning: [<rule>] <message>`; then a summary line `story_check: N errors, M warnings`.
  Exit code 1 if any error, else 0. Warnings never fail.
- Also importable: `check(story_dir, anchors=None, features=None, functions=None) -> list[Finding]` where `Finding`
  has `file`, `pointer`, `level` (`"error"`/`"warning"`), `rule`, `message`. Tests use this.
- Python 3.10+ stdlib only. No network. Deterministic.

## Schema to enforce (from §6; required keys marked *, all others optional; any other key → `unknown-key`)
Every action also allows `at` (number ≥ 0, ≤ 600) and requires `do`. Positions, zones and texts are as §6.2–§6.4.

Triggers (`when`, key `type`*):
`chapter_start` {} · `after` {seconds*, beat} · `flag` {flag*} · `zone` {zone*, seconds} · `look` {at*, degrees,
seconds, max_distance, spyglass} · `interact` {at*} · `use_item` {item*} · `has_item` {item*, count} · `break`
{block*, zone} · `chat` {pattern*, hide} · `still`/`sprint`/`sneak` {seconds*} · `death` {} · `respawn` {} · `bait`
{event* ∈ step|screamer|gone} · `creature` {creature*, event* ∈ near|seen|caught, distance, seconds} · `any` {of*: list
of ≥1 triggers}.

Actions (`do`):
`flag` {exactly one of set|clear} · `counter` {id*, exactly one of add|set (integers)} · `next_chapter` {} · `end`
{ending*} · `cancel` {beat*} · `title` {title*, subtitle, fade_in, stay, fade_out} · `actionbar` {text*} · `chat`
{text*, from} · `blackout` {fade_in, hold, fade_out, color `#rrggbb`} · `interface` {effect* ∈ F3|DEATH|SPLASH|LOADING|
ADVANCEMENT|SENSITIVITY|FOV|GAMMA|SIGN, seconds*, generation 0–5 int, strength −1…1, leak ∈ es|en|mixed, seed int, sign
(position; required iff effect is SIGN), advancement (`ns:path`), text (text)} · `audio` {cue* ∈ ambient|threads|muffle|
screamer|stop, where} · `post_effect` {id*, seconds*} · `teleport` {to*, blackout bool} · `bait` {op* ∈ place|step|scare|
clear; for place only: lure, distances (list of positive numbers, strictly decreasing), scare bool, appear_delay,
fallback_after, max_lifetime, seed} · `command` {run*} · `function` {id*} · `copy` {from*, to*} · `corrupt` {area*, rules*
(list of {from* [block ids], to* [block ids], ratio* 0–1}), seed, over_seconds} · `creature` {op* ∈ spawn|stalk|chase|
hold|face_player|despawn, id*; spawn: body*, where*, facing (`"player"` or number); stalk: step, min_distance; chase:
speed, catch_distance}. `body` is `{"mannequin": {"profile"*, "texture", "scale"}}` or `{"summon": "<string>"}`.

`story.json`, chapter files and beats: exactly the keys of §6.7, with its required/optional split (a missing
required key is `bad-value`; `agreement_fallback` missing while some text uses `{agreement}` is `missing-language`). Item, block, function, advancement and post-effect ids
match `^[a-z0-9_.-]+:[a-z0-9_./-]+$`. Ids match the patterns of §6.1.

## Rules (rule id = the `[rule]` in the output)
Errors, each with at least one invalid fixture: `unknown-key`, `bad-id`, `duplicate-id` (beat ids across the story,
anchor/zone names, capture names are `duplicate-capture`), `missing-chapter-file`, `chapter-id-mismatch` (file name or
`id` differs from the list), `too-many-chapters` (>5), `missing-language` (a text lacks a language of `languages`; also
`default_language` not in `languages`), `unknown-placeholder` (any `{…}` not in §6.2, or a stray brace),
`unknown-anchor` (position name not in map checkpoints/landmarks nor story anchors), `unknown-zone` (zone name not in map
triggers nor story zones; `section` not a map section), `unknown-beat-ref` (`after.beat`, `if.beats`, `if.not_beats`,
`cancel.beat`, `autoplay.runs[].skip`), `unknown-ending`, `ending-unreachable` (an ending not used by any `end` action
and not `fallback_ending`), `flag-never-set` (a flag in `if.flags`, `if.not_flags` or a `flag` trigger that no `flag`
action sets), `bad-command` (empty, leading `/`, or first word in the forbidden list of §6.6), `unknown-function` (no
file `<functions dir>/<path>.mcfunction` for `unallocated:<path>`; other namespaces are errors too), `bad-time`
(negative time; `at` > 600; `budget_seconds` outside 30–1200; `interface.seconds` above the effect's maximum),
`unknown-trigger`, `unknown-action`, `chat-without-input` (a `chat` trigger whose beat lacks `autoplay.input`, or the
input does not match the pattern with `re.search`; an invalid regex is also this rule), `duplicate-capture`,
`autoplay-ending-coverage` (an ending missing from `autoplay.runs`, listed twice, or a run's ending unknown),
`feature-off` (an `interface` action with SENSITIVITY, FOV or GAMMA while `effects.options` is false; a `bait` place
with `scare: true` while `effects.screamer` is false), `bad-value` (wrong JSON type or out-of-range number, bad colour,
bad id syntax — anything type-like not covered above).
Warnings: `session-length` (sum of `budget_seconds` outside 600–900), `screamer-count` (more than two `audio` actions
with cue `screamer` in the whole story), `long-title` (a `title` text over 40 characters in any language).
Find **all** errors in one run (do not stop at the first). A malformed JSON file is one error `bad-json` at pointer `""`.

## Tests (`scripts/test_story_check.py`)
- The example fixture yields zero errors (warnings allowed; it will warn `session-length`).
- For each `invalid/<rule>/`: at least one error whose `file#pointer` equals `EXPECT.txt` and whose rule matches; the
  test fails listing the actual findings otherwise. One test method per fixture (use `subTest`).
- A test that every rule id named in this brief has an invalid fixture (except warnings, tested inline).
- Warnings: build small stories in a temporary directory **under the repository** (`.tmp/story-check-tests/`, delete it
  after) and assert each warning.
- If `src/main/resources/data/unallocated/story/story.json` exists, it must have zero errors (the shipped story).
- CLI test: run the script with `subprocess` on the example and on one invalid fixture; check exit codes and the output
  line format.

## Decisions you may take alone
Internal structure of the script; wording of messages; which single change each invalid fixture makes; extra invalid
fixtures for the same rule. You may **not** change the format: if §6 is ambiguous or contradictory, choose the reading
that matches the §7 example, note it in your final message, and do not edit `docs/plan/milestones.md`.

## Delivery
- Run `python3 -B -m unittest discover -s scripts -p 'test_*.py'` from the repository root: everything passes (the
  existing 61 tests too). Run `python3 scripts/story_check.py src/test/resources/story-fixtures/valid/example` (exit 0)
  and on two invalid fixtures (exit 1). Paste those outputs in your final message.
- `git status --untracked-files=all`: only your listed files are new or changed.
- One commit, author `alexanderkholodov1 <kholodov.alexander1@gmail.com>`, message
  `0.2.0.1: story validator, map anchor names and story fixtures` (no AI attribution of any kind, no co-author lines).
  If your sandbox has a read-only `.git`, commit through the relay: write `.tmp/commit-requests/0.2.1.json` =
  `{"paths": [<every file>], "message": "<message>"}` and read `.tmp/commit-results/0.2.1.json`. If neither works,
  leave the files uncommitted and list them; the captain commits.
- Rehearsal: if available, write `.tmp/rehearse-requests/0.2.1.json` = `{"note": "story validator and fixtures"}` and
  wait for `.tmp/rehearse-results/0.2.1.json` (pass/fail, run id, failing jobs' last log lines); otherwise the captain
  rehearses. **Done = the rehearsal passes all four jobs.** Do not push.
- Final message (English): files, test output, any ambiguity in §6 you resolved and how.
