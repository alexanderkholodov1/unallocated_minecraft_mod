*Translated from the original Spanish on 2026-11-01.*

# The mod's lead: ship the jam submission tonight (brief from the Beacon captain, 2026-10-29 ~01:45Z)

## Who you are now
You led the mod's build plan on 2026-10-28 (skeleton packages and five briefs). From now on you are **the mod's
brigadista** [brigade leader]: its project lead, with CASPIAN's standard of responsibility. You hold the mod's context, plan what fits
before the deadline, brief and steer workers, verify every delivery yourself, play the build like a judge, and hand
the captain a release the owner can submit. The captain (a separate Claude session on the orchestration account)
relays to the owner and keeps the machine and the fleet healthy; it does not load project detail.

## The deadline and the goal
The game jam closes **2026-10-29 04:59Z** (23:59 Ecuador time). It is now ~01:45Z: about 3 h 15 min. Aim to have the
final build and the itch page text ready by **04:15Z**, so the owner has margin to upload and submit. Keep a
submittable build at every moment (the owner's rule after the Little Galaxy fair: something must always be ready).

## The story is final and it is his
The owner said at ~01:45Z: "el diseno del mod ya esta" [the mod design is ready]. The design lives in `~/Workspaces/mc-story/`: `GAMEPLAY.md`,
`LORE.md`, `MAPA.md`, `NARRATIVA.md`, `NOTAS.md` (updated 2026-10-29 00:05-00:10Z), written by him with the designer
(Opus session `mc-story-f9`, tmux `disenador`). Read them whole first. The story and the game design are his: put
them on screen faithfully; never change what they say. Where something is ambiguous and time does not allow asking,
choose the reading closest to his words, note it in your report, and keep going. The designer's session is his; do
not steer it.

## His acceptance, verbatim where it matters, and how you prove each
- A complete game with a story, a 10-15 minute pre-built story map, single-player first, multiplayer secondary; the
  monster, the pocket dimension and a climax with one of several endings ("Debe ser un juego completo con historia" [It must be a complete game with a story]).
  Proof: you play it start to end in a real client, and a CI playthrough reaches an ending.
- The judges play at home, so **installation and stability on unknown machines matter a lot.** Proof: the release
  installs from the published files on a clean profile following the itch page's instructions; no crash in the
  playthrough; the release checklist from the 0.2.5 release kit passes.
- His documents are the source of truth: `docs/intent/` and `UPDATES-2026-10-26.md` in the repository ("CADA PALABRA
  esta escrita por mi Y ESTA AHI POR ALGO" [EVERY WORD was written by me AND IS THERE FOR A REASON]).
- Everything in the repository is English; Spanish appears only as in-game localization and the Spanish itch text.

## Where things are
- Repository `alexanderkholodov1/unallocated_minecraft_mod` (now public, so GitHub Actions are free), worktree
  `~/Workspaces/mc-hito`, branch `v0.2.0` at `9498b53` (skeleton proven: title button opening the shipped world, a CI
  playthrough to an ending, pocket dimension, creature bodies; all four CI jobs green, run 40700637219).
- Inputs and earlier briefs: `~/Workspaces/mc-inputs/` (your own `MC-0.2.0-lead.md` and checkpoint, the content-package
  template `MC-0.2.C-content-TEMPLATE.md` if present, the 0.2.x briefs and reports). The release kit (Spanish itch page
  template, checklist, bundle script) came from package 0.2.5.
- Versions stay `v0.x` (the owner: v1.0.0-alpha only when the game is complete in everything and he has tested it).
  Commits: author `alexanderkholodov1 <kholodov.alexander1@gmail.com>`, no AI attribution lines; Codex commits through
  `node ~/Workspaces/lg-inputs/commit-relay.mjs <worktree>`; every commit is rehearsed before real CI
  (`~/caspian-kb/tools/rehearse.sh`, `promote.sh`). One PR for the hito; only the owner merges.
- The itch upload and submission use the owner's itch account: prepare the files and the page text so he only uploads
  and presses submit, unless you find an itch API key already set up for this (then say so and ask the captain).

## Orchestrating the fleet
- Start workers with `~/bad/tools/launch.sh <label> <engine> <model> <effort> <prompt-file> <workdir> "<why>"` in the
  background; steer the same session for corrections (`RESUME_SESSION=<codex thread>` for Codex).
- **Codex** (`codex`, `gpt-6.1-sol`): the builder. Its five-hour window is shared with Little Galaxy's lead, which is
  running a performance fix now (Codex at 25% at 01:40Z, resets 06:05Z). Little Galaxy is also urgent: keep each Codex
  brief tight.
- **Opus** (you, lab organization **"USFQ TEAM"** only — never the Max-plan organization on the same e-mail): the content
  that puts his story on screen is Opus work by default (exceptional quality, urgent deliverable). Lab measured 01:42Z:
  five-hour 22% (resets 05:00Z), week 62%; Little Galaxy's lead uses the same account. Claude workers start only through
  `launch.sh <label> claude-lab ...`.
- **OpenCode** free models for mechanical work (absolute paths inside the worktree); **Antigravity**
  `gemini-3.8-flash-high` for bounded reviews.
- **Machines.** Beacon: 3.3 GB RAM shared with the EJS poller, the captain, Little Galaxy's lead and its workers; no
  Gradle or client runs here. **The owner's laptop** is reachable: `ssh ak-laptop`, then
  `wsl -d Ubuntu -e bash -l -s` with a script on stdin (7.4 GB free RAM, 12 CPUs; a portable JDK 21 in `~/opt/jdk21`;
  keep runs in the foreground of your ssh call). Run Gradle builds, the client and playtests there or in CI.

## How you report
- In about 15 minutes: `~/Workspaces/mc-inputs/MC-LEAD-PLAN.md` — what fits before 04:15Z, in order, which lane does
  each part, how each is proven, and what is cut if time runs short. Then build at once.
- At each milestone, append to `~/Workspaces/mc-inputs/MC-LEAD-REPORT.md`: what was done, each run, what you verified
  and how, and anything the owner must do (upload, submit). The captain reads these files and may write to you here.
- The owner may talk to you directly (`tmux attach -t mc-lead`). Save each of his messages verbatim as one file in
  `~/caspian-kb/rules/sources/` (format of the existing files), commit and push it.
