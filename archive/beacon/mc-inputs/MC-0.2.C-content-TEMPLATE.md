# Template for tomorrow's content briefs (C1–C6) — fill the «…» fields from the design document

The captain copies this file to `~/Workspaces/mc-inputs/MC-0.2.C<n>-<slug>.md`, replaces every «…», and dispatches.
One brief per agent; for chapters, one brief per chapter file so agents never edit the same file.

---

# Brief MC-0.2.C«n» — «slug»

**Lane:** «Antigravity (Claude Sonnet 4.6) | OpenCode | Codex medium (C4 only)». Budget «20–45» minutes.

## Who you are, where, for whom
You write content data for UNALLOCATED, a first-person horror mod for Minecraft Java 26.3 (Fabric) by Alexander
Kholodov (`alexanderkholodov1`), for the USFQ jam closing **2026-10-29 04:59Z**. The Beacon captain dispatched you and
verifies. Worktree `~/Workspaces/mc-hito`, branch `v0.2.0`. **Never run Gradle, Java or Minecraft; never write under
`/tmp`.** Python 3 is fine.

## Goal
Turn exactly the part of the design named below into data in story format v1. You add no story of your own: every
word the player reads and every event comes from the design text quoted below. If the design is silent or ambiguous
on something you need, do not invent it: write `QUESTION: …` in your final message and leave that beat out.

## Inputs
- The format, normative: `docs/plan/milestones.md` §6 (and the §7 example for shape). Runtime notes:
  `docs/story/RUNTIME.md`. Map names: `scripts/map_anchors.json`; coordinates: `docs/map/INTEGRATION.md`
  "Coordinate contract".
- The design, verbatim, for your part: «paste the design document's tables for this package here, unedited».
- Already decided global settings (C1's `story.json`): «languages, chapter ids in order, anchors/zones names, flags
  other chapters set that this one reads».

## Files you may create or edit (nothing else)
«C1: `src/main/resources/data/unallocated/story/story.json`, `src/main/resources/assets/unallocated/features.json`
(only `effects.*`) | C2: `src/main/resources/data/unallocated/story/chapters/«id».json` | C3:
`src/main/resources/data/unallocated/function/story/**.mcfunction`, `src/main/resources/data/unallocated/structure/**`
| C5: the eight lang files (only the named keys) and `docs/plan/CONTRACT-TEXT.md` | C6: `.tmp/release/design-tokens.json`
(outside git, for `release_bundle.py --design`)».

**Never yours (the owner's alone, 2026-10-28):** logos, title art, the mod's name or icon, the itch page's address or
any URL, and any player-facing text the design document does not give word for word. Use his files and his words; if
one is missing, stop and say so in the final message instead of drawing or writing one.

## Exact delivery
- «C2: one chapter file with beats «list of beat ids from the design»; every beat with the trigger, guard, timed
  actions and capture the design gives.»
- Local acceptance: `python3 scripts/story_check.py` prints `0 errors` (warnings listed in your final message);
  `python3 -B -m unittest discover -s scripts -p 'test_*.py'` passes; for C5 also `python3 scripts/contract_text.py --check`.
- `git status --untracked-files=all` shows only your files.
- Commit: author `alexanderkholodov1 <kholodov.alexander1@gmail.com>`, message `0.2.0.C«n»: «what»`, no AI attribution.
  If `.git` is read-only: relay `.tmp/commit-requests/C«n».json` = `{"paths": [...], "message": "..."}`, read
  `.tmp/commit-results/C«n».json`. Otherwise leave the files uncommitted and list them; the captain commits.
- Real acceptance (the captain): rehearsal green in all four jobs; the `play` phase reaches every ending; the captain
  opens each `story-*.png` capture.
- Final message (English): files, `story_check.py` output, every QUESTION.
