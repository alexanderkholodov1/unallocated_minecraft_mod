# Brief MC-0.2.5 — `release-kit`: the itch.io page and the release bundle

**Recommended lane:** OpenCode (strongest free model) or Antigravity (Gemini 3.8 Flash is enough). Why: Spanish and
English documentation plus one small stdlib Python script with tests, fully specified, independent of all Java work.
It can run **at any time, in parallel** with the Codex packages (it touches none of their files). Budget about 40 minutes.

## Who you are, where, for whom
You prepare the release material for UNALLOCATED, a first-person horror mod for Minecraft Java 26.3 (Fabric) by
Alexander Kholodov (`alexanderkholodov1`, studio NULL_Threshold Interactive), for the USFQ jam "Terror en Primera
Persona", closing **2026-10-29 04:59Z** (23:59 Ecuador, 2026-10-28). The Beacon captain dispatched you and verifies.
Worktree `~/Workspaces/mc-hito`, branch `v0.2.0`. **Never run Gradle, Java or Minecraft; never write under `/tmp`.**
Python 3 stdlib is fine.

## Goal and why
The judges download from itch.io and play at home; the page must contain everything the jam rules require
(`docs/intent/Itch.io Competition Instructions.txt`, section "¿Qué debes entregar?" — read it), with installation
instructions a stranger can follow. The game's story is designed only tomorrow evening, so the page is a template whose
fixed parts are complete now and whose story parts are clearly marked tokens. A script fills the generated parts from
the CI release artifact so the owner uploads in minutes.

## Inputs
- `docs/plan/milestones.md` **§10** (each page item and who produces it) and **§4.3** (exact release artifact names and
  `release.json` fields) and §4.5.
- `README.md` sections "Requisitos para jugar", "Instalación en casa", "Advertencias de contenido y estado conocido",
  "Equipo y créditos" — the verified installation text and credits; reuse their facts (versions, paths per OS, the
  Fabric API Apache-2.0 credit) and adapt them to the button start described in §4.3/§10: in the game, the title screen
  has a button "UNALLOCATED"; a world ZIP `UNALLOCATED-world.zip` is the fallback (unzip into `saves/`, open from
  "Un jugador").
- `docs/plan/CONTRACT-TEXT.md` (the contract's warnings; quote its warning facts, do not invent new ones).
- `docs/plan/DECISIONS.md` D-02, D-03 (the owner's decisions about Fabric API inside our JAR and the start).

## Deliver (exact files; nothing else)
1. `docs/release/ITCH-PAGE.md` — the page body **in Spanish**, Markdown that itch accepts, with these sections in this
   order: título y descripción breve (`{{DESIGN:DESCRIPTION}}`), advertencias de contenido (fixed facts from the
   contract + `{{DESIGN:WARNINGS}}`), requisitos (Minecraft Java Edition `{{MINECRAFT}}` exactly, Fabric Loader
   `{{LOADER}}` or later, Java via the official launcher, Fabric API `{{FABRIC_API}}` **included**, no other mod),
   archivos (`{{JAR}}` with SHA-256 `{{SHA256}}`, `{{WORLD_ZIP}}` optional), instalación paso a paso (Windows, macOS,
   Linux), cómo empezar (contract "De acuerdo", button, rescue `/unallocated siguiente`, recommended 10–15 min with
   headphones, first person), mapa y configuración (built in; ZIP fallback steps), errores conocidos
   (`{{KNOWN_ISSUES}}`), equipo (`{{DESIGN:TEAM}}`), créditos (Fabric API credit as in README + `{{DESIGN:CREDITS}}`),
   capturas y video (a note for the owner, not shown to players — mark it as an HTML comment). Generated tokens:
   `{{VERSION}}`, `{{MINECRAFT}}`, `{{LOADER}}`, `{{FABRIC_API}}`, `{{JAR}}`, `{{SHA256}}`, `{{WORLD_ZIP}}`,
   `{{COMMIT}}`, `{{KNOWN_ISSUES}}`. Design tokens: `{{DESIGN:<NAME>}}`. Write no story and no description of the plot.
2. `docs/release/CHECKLIST.md` — English, numbered, for the owner/captain: download the release artifact (`gh run
   download <run> -n unallocated-release-<sha> -D <dir>`), run `release_bundle.py`, fill design tokens, create/edit the
   itch project (kind: download; upload the JAR and the world ZIP; classification; cover image 630×500 chosen from
   `dist/screenshots/`; 3–5 screenshots; the video, or a better real-time take with sound recorded on a real PC), submit
   to the jam page, then a clean-profile test from the itch page itself. Include the deadline in UTC and Ecuador time.
3. `scripts/release_bundle.py` — `python3 scripts/release_bundle.py ARTIFACT_DIR [--out dist] [--design FILE]
   [--known-issues FILE]`. Reads `release.json` and the files of §4.3 from `ARTIFACT_DIR`; verifies the JAR's SHA-256
   equals both `release.json` and the `.sha256` file; copies to `--out` (default `dist/`, refuse to overwrite a
   non-empty directory): the JAR, the `.sha256`, `UNALLOCATED-world.zip`, `screenshots/` (all PNG, valid PNG
   signature), `gameplay.mp4` if present; writes `itch-page.md` from the template with generated tokens filled and
   design tokens filled from `--design` (a JSON object `{"DESCRIPTION": "...", ...}`) when given. Prints every token
   still unfilled and exits 2 if any remain, 1 on any error (missing file, bad hash, bad PNG), 0 when complete.
4. `scripts/test_release_bundle.py` — stdlib `unittest`; builds a synthetic artifact under `.tmp/release-bundle-tests/`
   in the repository (deleted afterwards): complete case (exit 0, no token left with a full design file), missing
   design (exit 2, tokens listed), hash mismatch (exit 1), missing JAR (exit 1), invalid PNG (exit 1), no video (still 0).
5. Add `dist/` to `.gitignore` (one line) — the only edit outside your new files.

## Decisions you may take alone
Spanish wording of the fixed sections (clear, short, formal "tú" as the README uses), Markdown layout, script
internals. Not yours: any story, description of the game's plot, warnings the contract does not state, team names.

## Delivery
- Run `python3 -B -m unittest discover -s scripts -p 'test_*.py'` (all pass, including existing tests).
- `git status --untracked-files=all`: only your files and `.gitignore`.
- One commit, author `alexanderkholodov1 <kholodov.alexander1@gmail.com>`, message
  `0.2.0.5: itch page template, release checklist and release bundle script` — no AI attribution of any kind. If your
  `.git` is read-only, use the relay: `.tmp/commit-requests/0.2.5.json` = `{"paths": [...], "message": "..."}`, read
  `.tmp/commit-results/0.2.5.json`; if neither is possible, leave files uncommitted and list them for the captain.
- Rehearsal (if available): `.tmp/rehearse-requests/0.2.5.json` = `{"note": "release kit"}`, wait for
  `.tmp/rehearse-results/0.2.5.json`. **Done = rehearsal passes all four jobs.** Do not push.
- Final message (English): files, test output, and the list of design tokens the owner must provide.
