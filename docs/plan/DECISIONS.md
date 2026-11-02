*Translated from the original Spanish on 2026-11-01.*

# UNALLOCATED — Open decisions for the owner

*Package v0.1.2 · 2026-10-26. Each decision: options, recommendation, what changes. Decisions that **block work** come first. If the owner does not respond, the agents proceed with the recommendation (marked **→**) and the decision can be reverted where indicated.*

Suggested response format for the owner: `D-03: B` or `D-07: A, pero …` [D-07: A, but …].

## Summary

| # | Decision | Recommendation | Blocks | Operator's response
|---|---|---|---|
| D-01 | Core without external AI or voice? | Yes | all of v0.1 | Sí [Yes].
| D-02 | Fabric API for the judge | Inside our JAR (jar-in-jar) | v0.1.3, page | Sí [Yes]
| D-03 | How the judge starts | "UNALLOCATED" button on the title + backup ZIP | v0.1.7 | Sí [Yes]
| D-04 | How the map is built | By code, polishable by hand | v0.1.6 | Sí [Yes]
| D-05 | "De acuerdo" [I agree] | Written | v0.1.4 |  Sí [Yes]
| D-06 | Language of the leaked text | Mixed (Spanish to the player, logs in English) | v0.2.7 | Sí [Yes]
| D-07 | Endings | Two: scissors + `kill -9` (B) / consumed (A) | v0.3.8 | No estoy seguro, estas distorcionando porque yo propuse finales distintos. [I'm not sure, you're distorting it because I proposed different endings.]
| D-08 | Custom shaders | Not essential; vanilla effects | v0.2–3 | No, podemos usar shaders creados por alguien. [No, we can use shaders made by someone else.]
| D-09 | Threads in a single player | "Quien más sabe, más ve" [the one who knows more sees more] + they are always heard | v0.3.6 | Interesante, pero me debes explicar bien [Interesting, but you have to explain it well to me]
| D-10 | Resources and sound | CC0/credited; vanilla altered at runtime | v0.1.10 | Prefiero B [I prefer B]
| D-21 | Settings that change | In memory, without touching `options.txt` | v0.2.5 | Prefiero B [I prefer B]
| D-11 | Director | Pacing + visible learning, no bandit | v0.2.2 | Como fuimos por construir un mapa no se que tan aplicable sea [Since we went for building a map, I don't know how applicable it is]
| D-12…D-20 | Upload early, testers, names, ritual, anchor, rules, license, memory, theft/ladder order | see below | v0.6 | Vamos a entregar la primera version COMPLETA; yo hago las pruebas; apruebo; B; no lo explicaste bien, pero supongo que A; B; si; me gusta!; esta bien. [We are going to deliver the first COMPLETE version; I do the tests; I approve; B; you didn't explain it well, but I suppose A; B; yes; I like it!; it's fine.]

---

## Blocking work now (v0.1)

### D-01 — The core of the experience does not depend on external AI or voice *(coordinator default, not confirmed)*
- **A →** Yes: the jam works without network, without microphone and without an inference service; AI and voice are left for after the PoC (PLAN §9).
- B: optional inference service (LAN/VPN) that improves texts if available.
- C: mandatory local model (Ollama-like).
- **Why A:** judges play at home on unknown machines ("Los jueces NO JUGARAN EN VIVO EN EL AULA" [Judges will NOT PLAY LIVE IN THE CLASSROOM]); "Estabilidad y facilidad de instalación" [Stability and ease of installation] is a criterion; Neuro Lurker already occupies the "local LLM that stalks you" niche. B adds nothing the judge would see if the service does not respond.
- **What changes:** removes from the jam the service, Simple Voice Chat, the DSL and all network risks; the contract asks for a written "De acuerdo" (D-05).

### D-02 — How the judge receives Fabric API
- **A →** Package inside our JAR (jar-in-jar, `include`) only the Fabric API modules we use (Apache-2.0, credited). The judge installs Loader + one JAR.
- B: ask the judge to download Fabric API 0.165.0+26.3 separately.
- C: do not use Fabric API (only Mixins and Loader).
- **Why A:** every extra dependency is a point of failure at home (their criterion in 1.3: "cada dependencia obligatoria puede decrementar la facilidad de instalación" [each mandatory dependency can decrease ease of installation]). C multiplies the work and the risk (events, network, HUD, hand-made commands).
- **What changes:** WP v0.1.3 modifies the CI check that today prohibits nested JARs; the page says "no hace falta Fabric API" [Fabric API is not needed]. Reversible: going to B is only documentation. Verification in `VERIFIED.md`.

### D-03 — How the judge starts the experience
- **A →** **"UNALLOCATED"** button in the main menu: copies the world template (inside the JAR) to `saves/`, shows a "Conectando al servidor…" [Connecting to the server…] style screen and opens the world. Additionally, a world ZIP on itch as a manual backup.
- B: only the world ZIP; the judge unzips it into `saves/` and opens it from "Un jugador" [Singleplayer].
- C: the mod generates the map in a new world created by the judge.
- **Why A:** fewer manual steps (the most common error at home is the wrong folder); the "abandoned server" starts on the connection screen ("el idioma de Minecraft como arma" [the Minecraft language as a weapon]). B remains as backup.
- **What changes:** WP v0.1.7 (UI+REL). If A causes problems on a platform, the page refers to B without changing the mod.

### D-04 — How the pre-built map is constructed
- **A →** Hybrid: the map is built **by code** (`MapLayout` + `MapSection`s; agents in parallel; CI generates the template and the snapshot); the owner can polish it by hand later and his save replaces the template.
- B: the owner builds it by hand in creative mode.
- C: map generated by code on the judge's machine at startup (no template).
- **Why A:** the owner "no estara disponible durante la noche" [will not be available during the night]; the code is reviewable, reproducible and can be photographed in CI without a GPU; B concentrates everything in his time. C adds risk on unknown machines and does not test exactly what the judge receives.
- **What changes:** the visual style will be more "machine-built" unless hand-polished — which fits a world fabricated by a model. If the owner polishes by hand, the `MapLayout` coordinates (triggers, checkpoints) must not be moved.

### D-05 — The "De acuerdo" of the contract [I agree]
What is **[FIXED]**: "consentimiento mínimo con 'De acuerdo', con la opcion de leer la lista de usos (reales) de datos, con explicaciones de verdad, y ciertos 'guiños' al lore del mundo. Incluye las advertencias" [minimal consent with 'De acuerdo', with the option to read the list of (real) data uses, with truthful explanations, and certain 'winks' to the world's lore. It includes the warnings]. The open part is the form.
- **A →** Written: the player types "De acuerdo" (accepting variants in capitalization and accents). Their way of writing (capitalization, accents, punctuation) is reused in the "their" chat lines of act 2.
- B: a "De acuerdo" button.
- C: said aloud (plan 3.6), with microphone.
- **Why A:** two words are not friction ("El agente no debe añadir fricción" [The agent must not add friction]), and it is the only way for "El 'De acuerdo' del inicio se vuelve parte de la historia" [The 'De acuerdo' at the start becomes part of the story] without a microphone. C requires Simple Voice Chat (beta in 26.3) and a microphone (D-01).
- **What changes:** WP v0.1.4 and v0.4.4. B is a one-line change if A turns out to be annoying.

---

## Blocking v0.2–v0.3

### D-06 — Language of the leaked text (plan 3.2 [OPEN])
- **A →** Mixed: the game speaks to the player in Spanish; logs, paths and errors in technical English; the drift mixes both as it degrades.
- B: everything in Spanish. C: everything in English.
- **Why A:** the judges are from USFQ (what matters must be understood in Spanish) and the real logs are in English (authenticity that the plan itself asks for: "inglés por autenticidad técnica" [English for technical authenticity]).
- **What changes:** TXT style guide; everything in `en_us.json` equally (language backup, PLAN §1.2.6).

### D-07 — Endings (plan 3.7 [OPEN]) and the form of the "hack"
- **A →** Two endings: **B "terminar el proceso" [end the process]** by cutting the threads with scissors (they do not vibrate: a real Minecraft exploit with the tripwire) and, upon cutting the last one, the open chat with `kill -9 <pid>` already typed, Enter; **A "consumido" [consumed]** if the Reclaimer reaches you or time runs out. After B, the world is clean when you re-enter; after A, it remains inhabited.
- B: a single ending (B).
- C: the original plan: find and write the correct commands without help.
- **Why A:** it keeps "Dos destinos: ser consumido o hackeo del servidor para borrar el modelo" [Two destinations: be consumed or hack the server to erase the model] and its "encontrar el exploit" [find the exploit] (4.1), with the final gesture of a "command" but without demanding syntax from a judge without help; the scissors exploit is pure Minecraft ("Creatividad: uso de Minecraft para crear terror" [Creativity: using Minecraft to create horror]).
- **What changes:** WP v0.3.8. The alternative endings of 4.1 (restore or poison the dataset) move to after the PoC. If time runs short: B only.

### D-08 — Visual effects: custom shaders?
- **A →** No custom shader is essential. Atmosphere with vanilla mechanics: dimension format and environment attributes (fog, sky, sound), custom textures, HUD overlays, display entities, and the 26.3 vanilla **screen effects** (`spider`, `invert`, `creeper`, `blur`, applied by the integrated server as `/posteffect`), brief and rare.
- B: in addition, a custom "digital corruption" post-effect as an optional ornament (P2), switchable in `features.json`, that the experience does not need.
- C: custom post-effect as the central piece.
- **Why A:** 26.3 has two backends (OpenGL by default at home, experimental Vulkan) and our CI only tests Vulkan; Mojang warns that the `/posteffect` and shader system "may be redesigned". A custom shader must work on both and on unknown GPUs (laptops, macOS): the risk falls on "Estabilidad" [Stability]. See `VERIFIED.md` §2.1.
- **What changes:** the corruption style is "of the world" (blocks, entities, text, UI) rather than "of the screen", which is also more faithful to "La corrupción del mundo se parece a un colapso de modo: texturas repetidas, estructuras duplicadas" [The corruption of the world resembles a mode collapse: repeated textures, duplicated structures]. B can be added at the end without touching anything else.

### D-09 — Threads in a single player *(coordinator default: theme 1 with the theme 2 threads as visual language)*
- **A →** Theme 1 as the plot (the rotten Pocket Dimension and digital corruption). The threads: "quien más sabe, más ve" [the one who knows more, sees more] — they appear when the player discovers what they are (leak about `threads`) or whether they solved the ladder; they are always **heard** (subtle vibration, his 4.4 [OPEN]: "furin o cuerdas" [wind chimes or strings]); in the pocket they guide to the core and transmit the vibration that attracts the Reclaimer.
- B: threads always visible in the pocket. C: no threads (only puppets, after the PoC).
- **Why A:** his 3.3 said "Solo los puppets los ven" [Only the puppets see them], but without puppets nobody would see them; the brainstorm marks "quien más sabe, más ve" as "SERIA INCREIBLE" [it would be INCREDIBLE]. B is the cut-down version of A.
- **What changes:** WP v0.3.6; knowledge flags in v0.2.7.

### D-10 — External resources and sound *(coordinator default: CC0 or credited + altered vanilla sounds)*
- **A →** External ones only CC0 or with a license that allows redistribution and credit; vanilla sounds are **reproduced** at runtime with altered pitch, volume, position and layers (modified Mojang files are not redistributed).
- B: include modified vanilla files inside the JAR.
- **Why A:** the jam requires "librerías y recursos externos con permiso de uso, siempre que los declares y acredites" [libraries and external resources with permission of use, provided you declare and credit them]; redistributing altered Mojang files is a grey area that does not need to be stepped on. See `VERIFIED.md`.
- **What changes:** the audio designer works with vanilla `SoundEvent`s and a few own/CC0 OGGs.

### D-21 — "Ajustes que cambian" [Settings that change]: in memory or in the real Options?
His 4.5: "ajustes que cambian (sensibilidad, idioma de un letrero, brillo parpadeante, FOV; sin ser demasiado invasivo, muy puntual y muy poco frecuente). Los ajustes se guardan y **siempre se restauran**." [settings that change (sensitivity, the language of a sign, flickering brightness, FOV; without being too invasive, very punctual and very infrequent). The settings are saved and **always restored**.]
- **A →** Perception overrides **in memory** (FOV, sensitivity, darkness) that are never written to `options.txt`; the "brillo parpadeante" [flickering brightness] is the vanilla darkness effect (it pulses on its own). They are restored by construction, even after a crash.
- B: change the real options (they appear changed if the judge opens Options) with a disk backup and restoration on exit and at the next startup.
- **Why A:** meets "siempre se restauran" [always restored] without depending on the restoration running; the judge feels the change just the same. B gives an extra scare (opening Options and seeing the changed value) in exchange for risk: other parts of the game call `options.save()`, and a crash mid-way leaves the value changed until the next startup with the mod.
- **What changes:** WP v0.2.5 (camera and mouse Mixins in A; `OptionsGuard` in B).

### D-11 — The fear director "that seems to learn" *(coordinator default)*
- **A →** Pacing director with a per-player profile, design priors and **visible** learning (double with your movements, leaks with your numbers, final report). No bandit in the jam.
- B: contextual bandit (Thompson sampling) as in 4.2/5.4.
- **Why A:** "parecer que aprende vale más que aprender de verdad" [seeming to learn is worth more than actually learning]; "Controla el ritmo, no al monstruo" [It controls the pacing, not the monster]; with 10–20 scares per session a bandit does not converge (the plan itself: "Un RL abierto no converge en estas condiciones" [An open RL does not converge under these conditions]).
- **What changes:** v0.2.1–2.2. The bandit stays in §9 for the post-PoC server.

---

## Not blocking, but worth deciding before v0.6

### D-12 — Upload early to itch
- **A →** Publish and submit v0.1 as soon as it meets its criteria; replace the file with each milestone; freeze with the margin of its v0.1 plan ("Entregado antes del mediodía (cierre 23:59)" [Delivered before noon (closing 23:59)]).
- B: upload only the final version.
- **Why A:** avoids "el desastre de la produccion de Little Galaxy" [the Little Galaxy production disaster]: there is always something delivered. [To be confirmed: that the jam allows editing the submission before the deadline — common on itch.]

### D-13 — Who does the human tests [H]
- E1: the owner (real GPU). E5: 2–3 outsiders in their homes. **We need names** (no testers have been recruited; Phase 0 of v0.1 asked for them).

### D-14 — Name of the Pocket Dimension (plan 3.3 [OPEN])
- **A →** Keep his word from the brainstorm: "su POCKET DIMENSION (que se llame asi)" [his POCKET DIMENSION (let it be called that)]; technical identifier `unallocated:pocket`. In model texts, sometimes "espacio no asignado" [unallocated space] (forensic reading of *unallocated space*, PLAN §3.1).
- B: another proper name.

### D-15 — Ritual (plan 5.6 [OPEN]: "hablada o escrita" [spoken or written]; "entrada, salida o ambas" [entrance, exit or both])
- **A →** In the jam there is no entrance ritual (you fall into the pocket from the mine or the ladder); the "ritual" is the B ending: candles and threads of the core, cutting with scissors, `kill -9`. 
- B: a candle and thread ritual as the entrance to the pocket (more agency, more build time).

### D-16 — Answer to 3.1 [OPEN] "que tiene que ver Minecraft con esto" [what does Minecraft have to do with this]
- **A →** Minecraft as a real sandbox for training agents (Malmo, MineRL, VPT, Voyager) and the promised dataset = sessions of frightened players in horror maps; real precedent of the "Minecraft server hole": Log4Shell (PLAN §3.2, `VERIFIED.md`).
- B: leave it implicit.

### D-17 — Game mode and map rules
- **A →** Adventure Mode (the map does not break; scissors with permission to cut thread, torches with permission to place), `spawn_mobs false`, `advance_time` and `advance_weather` set to false (the mod controls time and weather), `keep_inventory true`, normal difficulty. (Rule names from 1.21.11+, `VERIFIED.md` §2.2.)
- B: free Survival.

### D-18 — Mod license
- Today `fabric.mod.json` says `All-Rights-Reserved`. The jam: "Los participantes conservan la autoría" [Participants retain authorship]. **→** keep it; third-party resources carry their license on the page.

### D-19 — Memory between sessions
- **A →** Minimal and optional (P2): if the judge plays again, "has vuelto" [you have returned] and the ladder exploit "parcheado" [patched] (his 4.1: "el siguiente grupo lo encuentra parcheado o mutado" [the next group finds it patched or mutated]). First cut (PLAN §7).

### D-20 — Priority of the first theft versus the ladder
- **A →** Theft (P1) before the ladder (P2): theft is cheaper and less risky; the ladder is "lo mas dificil" [the hardest thing] (seamless) and the plan marked it **High** risk.
- B: ladder first (it is the most original idea from the brainstorm).
