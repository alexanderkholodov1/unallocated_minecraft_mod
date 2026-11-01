*Translated from the original Spanish on 2026-11-01.*

# MC lead report (2026-10-29)

## 01:55Z — story content authored (commit 2c6fb6b on v0.2.0)
- Read the whole design (~/Workspaces/mc-story: LORE, GAMEPLAY, MAPA, NARRATIVA, DIRECTOR, BUILD, NOTAS).
- Wrote the story as data (story format v1): chapters `presa` → `heap` → `puppet` → `vuelta` → `kernel`; endings
  COMPLETE / POISON / CONSUMED (LORE §4 texts verbatim); the Heap = copy of the island in `unallocated:pocket`, rotted by
  seeded passes that get worse after the first death; the Kernel corridor + chamber built at template authoring;
  the spider body; your own echo (mannequin with your skin) as Puppet; threads (tripwire/cobweb) appear only as Puppet;
  shears that only cut thread; the Shell through chat after crossing the point of no return; watchdog countdown.
- `features.json`: `effects.options` and `effects.screamer` on (the contract already warns of both).
- Local proof: `story_check.py` 0 errors (1 warning: max budgets sum 990 s; actual route is shorter); 96 Python tests pass.
- Laptop clean-install harness: worker `mc-laptop-test` (claude-lab Sonnet high) launched 01:50Z.
- Rehearsal #1 started (log ~/Workspaces/mc-inputs/rehearse-story1.log).

### Player-facing wording NOT given verbatim by the design (for the owner's review)
Shell output and three leak lines carry gameplay facts the design requires (process id, the corrupted value, the
original spec value) but does not word:
- `reclaim[7331] :: cpu 97% :: Can't keep up! Is the server overloaded?`, `watchdog :: guarding pid 7331`
- `success_threshold: 1.37 (spec g0: 0.82)`, `the Kernel :: main loop`, `sample 4114 :: corrupt :: rejected`
- Shell: `ps · params · kill <pid> · set <param> <value> · echo <text> >> dataset`; the `ps` table; `success_threshold = 1.37`,
  `temperature = 2.4`; `[7331] reclaim :: stopped`; `watchdog :: restart in 90 s`; `watchdog :: restart`.
- The predecessor's book "notas" [notes] (author "anon"): pages are the design's own lines (forum line, the three signs) plus one
  closing page cut mid-word: "en todos los errores la misma palabra: UNALLOCATED. y reclaim, reclaim, reclaim. lo que
  firma así tiene un nombre. es" [the same word in every error: UNALLOCATED. and reclaim, reclaim, reclaim. whatever signs like that has a name. it is].
- Numbers chosen by me: pid 7331, threshold 1.37 (design: "por encima de 1" [above 1]), spec value 0.82, temperature 2.4.

## ~02:00Z — rehearsal #1 and fixes
- Rehearsal #1 (run 40803275365, 2c6fb6b): 0/4 — two JUnit tests still pinned the neutral default story and effects
  off (expected; not a content error). Updated both to assert the shipped story (5 chapters, 3 endings) and effects on
  (07291b4). Type-check of all 151 Java sources with mc-ref/javacheck.sh: OK.
- 22d27d3: Kernel playability: cut threads stay cut; `ps` shows `└ thread main: Reclaimer` (the design's "etiqueta de
  su hilo principal visible en el Kernel" [its main thread label visible in the Kernel] — the only in-world source of the name for the Poison ending);
  action bar `> help` after crossing (design §6: "un modo simplificado con ayuda en pantalla (help)" [a simplified mode with on-screen help (help)]).
- e5276ca: **the contract's data-use list was no longer true** for this build (it said no chat, username or behaviour is
  read; the story reads chat for the Shell, shows your name, and uses position/look/sprint/broken blocks as triggers).
  The owner fixed that list as "usos (reales) de datos, con explicaciones de verdad" [(real) data uses, with real explanations], so I rewrote `uses.current`,
  `uses.local`, `uses.absent`, `uses.future` truthfully, and added the design's contract content: "Sujeto #4114" [Subject #4114] in the
  body and the protocol (g0 + the four rules, LORE §1.1 verbatim) with `success_threshold = 0.82` as the fine print
  (`uses.lore`). For the owner's review: docs/plan/CONTRACT-TEXT.md.
- Rehearsal #2 running on 07291b4 (log ~/Workspaces/mc-inputs/rehearse-story2.log).
- Itch design tokens prepared in ~/Workspaces/mc-inputs/release/design-tokens.json: description = LORE §0 verbatim plus
  one line of mine ("Experiencia de un jugador de ~15 minutos, en primera persona, en un mapa construido de antemano.
  Tres finales." [A ~15-minute single-player, first-person experience on a prebuilt map. Three endings.]); warnings = LORE §6 verbatim; team = "NULL_Threshold Interactive — Alexander Kholodov" (owner to
  confirm members); credits = no external assets + Mojang notice.
- ~02:02Z rehearsal #2 (run 40803517230, 07291b4) failed only in the shipped-story JUnit: its helper validates with both effect switches off; now validated against features.json like the runtime (1a712a3). Rehearsal #3 running.
- ~02:06Z rehearsal #3 (run 40803808941, 1a712a3): template authoring refused the story — the Java command check flagged every `execute in … run …` and `tp @s ~-14 ~ ~` because Brigadier records exceptions from abandoned alternative branches. Fixed the check to vanilla's own rule (fully consumed + executable node on the last child context) in StoryValidator (fc3ad31); javacheck OK. Rehearsal #4 running.
- ~02:08Z rehearsal #4 (run 40804128836, fc3ad31): the validator fix worked; the story build then stopped at its first command (sign at the base: chunk not loaded for the console). Added an overworld forceload for the base area (c35cfdb). Rehearsal #5 running.
- ~02:15Z rehearsal #5 (run 40804416017, c35cfdb): build, template, story/world phases and the Loader-rejection profile
  passed; the shipped playthrough reached CONSUMED instead of COMPLETE. Cause: in CI nobody pressed **Respawn** on the
  death screen, so after the first (mandatory) death the player stayed dead and every later beat waited until the
  chapter budgets ran out. Fix (bd640a0): the CI helper presses Respawn like a judge (and screenshots the first death
  screen as `story-death-screen.png`); autoplay no longer forces a server-side respawn before the natural one.
  Rehearsal #6 running.
- Laptop clean-install harness: **PASS three times** on the previous build 9498b53 (contract typed, UNALLOCATED button,
  "Conectando al servidor…" [Connecting to the server…], first chapter entered, 60 s in-world, clean exit; Adventure, cheats off). Substitutions:
  official launcher replaced by a script that launches the installer's own profile (WSL headless), Vulkan/llvmpipe
  instead of a GPU, null audio. Report: ~/Workspaces/mc-inputs/laptop-test/REPORT.md.
- Its findings fixed in 9163077: `release.json` wrote `fabric_api_bundled: true`, which made `release_bundle.py` refuse
  the real artifact → now the version string; the itch page and README named a launcher profile that does not exist →
  now `fabric-loader-26.3` (the name the official installer creates).
- ~02:36Z rehearsal #6 (run 40805747587, bd640a0): deaths and respawns now work through heap → puppet → vuelta → kernel. New blocker: autoplay's simulated thread cut was refused by Adventure mode, so the sensors stayed intact and the Kernel kept rotating the credential (exactly the design's lesson, but CI never cut). Fix f5e5a3f: the simulation removes the thread and reports the same break signal a player with shears produces. Rehearsal #7 running (includes the release fixes 9163077).

## ~02:45Z — first fully green rehearsal: all three endings played by a real client
- Rehearsal #7 (run 40807100054, f5e5a3f): **4/4 jobs PASS.** In the `play` phase the real client clicked UNALLOCATED,
  opened the shipped world and autoplayed the whole story three times: presa → heap (death 1) → puppet (3 threads cut,
  sample rejected) → vuelta (death 2) → kernel (point of no return, `help`, `ps`, `params`, `kill -STOP 7331`) →
  **COMPLETE**, **POISON** and **CONSUMED** (watchdog timeout), each ending on its farewell screen and back to the title.
- I looked at the captures: puppet view with `processing 37%` bar, shears and `subject connection: still open. retaining
  (+fear)`; the Kernel shell (`ps` table with `└ thread main: Reclaimer`, params, `[7331] reclaim :: stopped`, the lucid
  lines); farewells "gracias. / no por los datos. / por el final." [thank you. / not for the data. / for the ending.] and "ven, tienes que ver esto / — <nick>" [come, you have to see this / — <nick>]; the title
  with the UNALLOCATED button. Release artifact: JAR 2,019,849 bytes (budget 2 MiB), world ZIP 1.04 MB, 25 screenshots,
  gameplay video.
- One finding from the captures, fixed in e005c49: the first death screen showed vanilla's line because the toolkit's
  15 s global cooldown was taken by the fall's LOADING effect; the g0 line now goes to chat so the death screen carries
  `subject connection: still open. retaining (+fear)`. In CI's compressed time the DEATH effect can still be refused
  (cooldowns are real-time); in real play the deaths are minutes apart.
- Rehearsal #8 on e005c49 (the final candidate) running; then promote to v0.2.0, real CI, release bundle, laptop install.

## ~02:53Z — final candidate promoted
- Rehearsal #8 (run 40808054371, e005c49): **4/4 PASS** (all three endings again). Promoted e005c49 to `v0.2.0`;
  real CI run 40808889092 in progress. PR to `main` opens automatically when it is green (owner merges).
- dist built from run 40808054371's release artifact (same commit): ~/Workspaces/mc-inputs/release/dist8
  (JAR sha256 e78fca1c…a076, world ZIP, 25+ screenshots, gameplay.mp4, filled itch-page.md, no tokens left).
- Laptop clean install from that dist running now (harness run `final-e005c49`).
- ~02:56Z **laptop clean install of the final dist: PASS** (run `final-e005c49`): JAR sha256 e78fca1c… verified against
  the page; official Fabric installer 1.1.2 → profile `fabric-loader-0.20.1-26.3`; contract typed; UNALLOCATED button;
  connecting screen; `story loaded chapters=5 beats=45 endings=3`; `chapter=presa … enter`; 60 s in-world; no crash;
  clean quit. Evidence: ~/Workspaces/mc-inputs/laptop-test/results/final-e005c49/ (PNGs, latest.log, RESULT.md).

## ~03:03Z — release built from real CI
- Real CI on `v0.2.0` (run 40808889092, e005c49): **4/4 success.** PR #1 to `main` opened:
  https://github.com/alexanderkholodov1/unallocated_minecraft_mod/pull/1 (only the owner merges).
- Final dist from that run's release artifact: **~/Workspaces/mc-inputs/release/dist/**
  - `unallocated-0.2.0.jar` (sha256 1fb9e54f24bdec9277ffca33fcb7fe373cc49a3af01ab2926b4b6f4c3188a318, verified)
  - `UNALLOCATED-world.zip` (fallback), `gameplay.mp4` (CI capture, time-compressed, no sound), `screenshots/`
  - `itch-page.md`: complete Spanish page, every token filled.
- The real-CI JAR is not byte-identical to the rehearsal JAR (build timestamps), so the laptop clean install is being
  repeated on exactly these files (run `final-v0.2.0`).
- ~03:05Z **laptop clean install of the real-CI dist: PASS** (run `final-v0.2.0`, JAR sha256 1fb9e54f… verified against
  the page). Removed the CI test-fixture captures (placeholder text like "[EN] Ending A title") from dist/screenshots;
  they are kept apart in ~/Workspaces/mc-inputs/release/ci-fixture-shots/.

## RELEASE READY (03:05Z) — what the owner must do
1. On itch.io (his account), create/edit the project page with the body of `~/Workspaces/mc-inputs/release/dist/itch-page.md`
   (Spanish, complete). Upload `unallocated-0.2.0.jar` and `UNALLOCATED-world.zip` from the same folder.
2. Pick 3–5 screenshots and a 630×500 cover from `dist/screenshots/` (suggested: unallocated-title-button, story-puppet,
   story-kernel, story-completar, story-death-screen). `dist/gameplay.mp4` is a CI capture without sound and
   time-compressed; a real-time take with sound on his PC is better (docs/release/CHECKLIST.md).
3. Submit the project to the jam page before **04:59Z** (23:59 Ecuador).
4. Review, when he can: the wording I had to write (listed at the top of this report), the contract text change
   (docs/plan/CONTRACT-TEXT.md), the team line "NULL_Threshold Interactive — Alexander Kholodov", and PR #1 (his merge).

## Deviations from the design (technical; the three endings and the whole arc are in)
- Arrival: Explorer only (forum line); the other two random arrivals need a random branch the runtime lacks.
- No fake server-list entry or Tab list of victims; the respawn button keeps vanilla's label (not "De acuerdo" [I agree]).
- One deliberate Puppet trip plus one return; the second death is caused by the spider in the Overworld, not chosen by
  the player. Secrets (pid 7331, 1.37 → 0.82) are fixed, not per world. Director = fixed triggers (DIRECTOR §7).
- POISON ends on the farewell screen with the degraded cascade instead of leaving the game running forever.
- COMPLETE's "reopen message" is the title splash after the ending, not a replacement of the contract.
- Spider body = scaled vanilla spider (no limbs/skins of victims); Artifacts = mannequins with your skin.
