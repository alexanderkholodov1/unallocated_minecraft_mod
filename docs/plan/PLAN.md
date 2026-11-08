*Translated from the original Spanish on 2026-11-01.*

# UNALLOCATED — Executable plan by milestones

*Package v0.1.2 · written 2026-10-26 · based on `docs/intent/` (the owner's four documents, read in full).*
*Companions: [`DECISIONS.md`](DECISIONS.md) (what the owner must decide), [`VERIFIED.md`](VERIFIED.md) (facts checked with source and date), and `TOOLCHAIN.md` on branch `v0.1.1` (versions, CI and installation; cited here, not repeated).*

> Reading rule. Quotes in quotation marks are the owner's words, copied as-is from `docs/intent/`. Where this plan proposes something different, it says so and argues for it; the decision stays in `DECISIONS.md`. Owner labels: **[FIJO]** [FIXED], **[PROPUESTA]** [PROPOSAL], **[ABIERTO]** [OPEN], **[VERIFICAR]** [VERIFY].

---

## 0. The situation on one page

- **Hard deadline:** 2026-10-28 23:59 UTC−5 (= 2026-10-29 04:59 UTC). The owner's v0.1 plan already set its margin: "Entregado antes del mediodía (cierre 23:59)" ["Delivered before noon (closing 23:59)"].
- **What changed (UPDATES 2026-10-26, these take precedence over the previous ones):** "podemos crear un mapa construido de antemano para contar una historia, y dejar el tema del servidor y mundo abierto para despues del PoC" ["we can create a pre-built map to tell a story, and leave the server and open-world topic for after the PoC"]; one player first ("no podemos depender de que obligatoriamente la experiencia funciona entre dos o mas personas" ["we cannot depend on the experience necessarily working between two or more people"]); "Los jueces NO JUGARAN EN VIVO EN EL AULA, lo mas probable es que cada uno lo hara desde casa" ["The judges will NOT PLAY LIVE IN THE CLASSROOM, most likely each one will do it from home"]; "la experiencia si debe ser de 10-15 minutos" ["the experience must be 10-15 minutes"]; milestones, not dates.
- **Consequence:** the v0.1 plan was sized for 8 days, an inference service on the DGX, a cloned voice, trained models, multiplayer puppets and a classroom server. For the jam, what remains is **a one-player experience, 10–15 min, on a pre-built map, that works without network, without microphone and without special GPU** on each judge's home machine. Everything else is kept, in the owner's words, in section 9 ("después del PoC" [after the PoC]); nothing is deleted.
- **Delivery form:** there is always a deliverable build. Milestone **v0.1.0** is already a complete delivery (small); each following milestone expands it without breaking it. It is uploaded to itch.io early and the file is replaced as milestones pass.
- **Target installation for the judge:** Minecraft 26.3 + Fabric Loader + **a single JAR** of ours (with the Fabric API modules we use packaged inside; see D-02). The map comes inside the JAR and is opened with a button on the main menu (see D-03), with a world ZIP as backup.

---

## 1. Review of the v0.1 plan (section 0, step 1)

### 1.1 Contradictions

| # | Where | Contradiction | Proposed resolution |
|---|---|---|---|
| C1 | 3.6 vs. UPDATES and coordinator default | The contract asks for the "De acuerdo" [I agree] **"dicho en voz alta"** [said aloud], but Minecraft Java has no microphone of its own: Simple Voice Chat is needed (beta in 26.3 according to TOOLCHAIN.md) and a judge at home may not have a microphone. | What is **[FIXED]** by the owner is "consentimiento mínimo con 'De acuerdo', con la opcion de leer la lista de usos (reales) de datos" [minimal consent with 'De acuerdo', with the option to read the list of (real) data uses]. Voice is not fixed (the same 3.6 says "o escrito si no hay micrófono" [or written if there is no microphone]). For the jam: **"De acuerdo" written** in a text field (two words, no friction), which also serves as an "enrollment sample" of *how you write* (see 3.4.6). D-05. |
| C2 | 4.3 step 3 "Primer robo" [First theft] vs. a single player | The theft is written for two ("cuando tu y tu amigo están cerca... el amigo que se queda en el mundo normal ve un doppelganger" [when you and your friend are close... the friend who stays in the normal world sees a doppelganger]). | Single-player version: you are taken to the copy and, **on returning, the double is where you disappeared, with your skin, doing what you were doing**; the chat has "yours" lines you did not write and lost time is included (brainstorm: "Tiempo perdido... el chat de antes aparece reescrito" [Lost time... the earlier chat appears rewritten]). The witness to the double is yourself. 3.3, act 2. |
| C3 | 4.2 / 5.3 vs. jam at home | The fear signal includes "sentimiento del chat, rasgos de voz" [chat sentiment, voice traits]; in a single player without voice there is almost no chat. | Telemetry of movement, camera and interaction (almost all available on the integrated server without network). Voice and chat as optional inputs after the PoC. |
| C4 | 4.2 "bandit contextual (p. ej. Thompson sampling)" [contextual bandit (e.g. Thompson sampling)] vs. 5.5 "Un RL abierto no converge" [An open RL does not converge] and the brainstorm "Con sesiones de 5-15 minutos hay pocos datos" [With 5-15 minute sessions there is little data] | A bandit learns nothing useful with ~10–20 scares per session and a single player. | Director with **pacing** (calm, pressure, peak, false relief) and **choice of variants** per profile with design priors; the "learning" is **shown** with the player's real data (Alien Isolation: "parecer que aprende vale más que aprender de verdad" [seeming to learn is worth more than actually learning]). 3.4.1. |
| C5 | 4.4 / 3.3 "Solo los puppets los ven" [Only the puppets see them] (threads) vs. a single player and the default "hilos del tema 2 como lenguaje visual" [threads from theme 2 as visual language] | Without puppets, nobody would see the threads. | Apply the brainstorm idea "quien más sabe, más ve" [the one who knows more sees more]: the threads become visible **when the player discovers what they are** (reads the leak about `threads`) and always within the Reclaimer's domain. The living also "notan una vibración sutil en el audio (furin o cuerdas)" [notice a subtle vibration in the audio (wind chimes or strings)] (4.4, [ABIERTO] → yes). D-09. |
| C6 | 5.6 "Estructuras por código... No se requiere mapa" [Structures by code... No map required] vs. UPDATES "mapa construido de antemano" [pre-built map] | The v0.1 generates structures around the spawn in an open world. | Pre-built map. It is built **by code** (so that agents can do it in parallel, it is reviewable and regenerated in CI) and packaged as a world template; the owner can polish it by hand later. D-04. |
| C7 | 5.6 Pocket "se copia el área alrededor del sitio del ritual (radio ≈ 200 bloques)" [the area around the ritual site is copied (radius ≈ 200 blocks)] vs. pre-built map | Copying 200 blocks of radius at runtime is slow and risky on unknown machines. | The copy is **authored in advance** as a "copia con errores" [copy with errors] (the map is known) and at runtime only **bounded corruption passes** run (few edits per tick). The narrative does not change: the model copied the place. |
| C8 | 3.7 "hackeo del servidor... encontrar los comandos correctos" [server hack... find the correct commands] vs. judges without help | Typing exact commands in chat is fragile for a judge who does not know the syntax. | Native Minecraft exploit + command in a single gesture: cut the threads with **shears** (in Minecraft Java, cutting a tripwire with shears "disarms" it and does not send a signal to the hooks: a real game "exploit", `VERIFIED.md` §2.3) and, upon cutting the last one, the chat opens with the command already typed (`kill -9 <pid>`); the player presses Enter. D-07. |
| C9 | 1.3 "Fabric API 0.161.0+26.3" / "Loader 0.19.5" vs. what was verified | Superseded versions. | Use those in TOOLCHAIN.md (Loader 0.20.1, API 0.165.0+26.3). Not repeated here. |
| C10 | 7.3 day-by-day schedule vs. UPDATES point 2 | The owner now asks for milestones. | Section 5: milestones with priority, criteria and tests; no dates except the deadline. |

### 1.2 Gaps

1. **Appendices B, C and D of the v0.1 plan are missing** (DSL, contract text, incident sources) and section 3.4 is missing (it jumps from 3.3 to 3.5). This plan provides a draft of the contract (Appendix A) and leaves the DSL for after the PoC (without an LLM no DSL is needed; the "actions" are code primitives, 4.3).
2. **3.1 [ABIERTO] "que tiene que ver Minecraft con esto"** [what does Minecraft have to do with this] — unresolved. Proposal in 3.2: Minecraft *really is* the standard sandbox of agent research (MineRL, VPT, Voyager…) and a Minecraft server failure *really was* the gateway to a global vulnerability (Log4Shell, 2021). Facts in `VERIFIED.md`.
3. **There is no defined "clear start"** (the jam asks for it: "con un inicio claro" [with a clear start]). It is missing how the judge starts: what they press, where they appear, what objective they see. 3.3 and 4.4.
4. **There is no "not getting stuck".** A judge alone at home without help must not be blocked: each act needs a **time budget with diegetic forced progress** (the floor rots, the bait approaches, a light guides). 3.3.
5. **Death, saving and exit** are not defined: what happens if you die outside the climax, if you quit midway, if the game closes with changed settings. 3.4.8 and 4.5.
6. **Client language.** The judge may have the game in English, in "Español (Ecuador)" or in "Español (España)". It is widely reported (not confirmed in the 26.3 code) that missing keys fall back to `en_us`; to avoid depending on that, the Spanish text goes in `en_us.json` **and** in the `es_*.json`. 4.3.
7. **Render distances.** A "distant" bait stops being drawn beyond the judge's entity distance (configurable and unknown). The bait must start at ≤ ~56 blocks or use display entities with a high `view_range`. [verify during implementation]
8. **Game mode and rules.** Without Adventure mode the judge can break the map; without `spawn_mobs false` (name since 1.21.11) zombies appear and break the tone. 4.5, D-17.
9. **Restoration of options after a crash**: v0.1 requires it ("siempre se restauran" [always restored]) but does not say how. Proposal (4.3): never write `options.txt`; effects are in-memory overrides that disappear with the process. If the change should be visible in the Options menu, a disk copy and restoration on the next startup (D-21).
10. **Audio credits and licenses**: "sonidos vanilla alterados" [altered vanilla sounds] cannot be redistributed as altered Mojang files without thought; vanilla sounds can be **reproduced** at runtime with altered pitch, volume, position and layers. D-10.

### 1.3 Risks (those that change the plan)

| Risk | Prob. | Impact | Mitigation |
|---|---|---|---|
| Installation fails at the judge's home (version, missing Fabric API, Java of the installer on macOS) | Medium | **Critical** ("Estabilidad y facilidad de instalación" [Stability and ease of installation]) | One JAR with Fabric API modules inside (D-02); instructions per system (TOOLCHAIN.md); E5 test in 2–3 outside machines; world ZIP as backup. |
| Render: 26.3 has **two backends** (OpenGL by default at home; experimental Vulkan) and **our CI only tests Vulkan** (OpenGL does not start under Xvfb); official minimum requirements went up in 2026-07 | Medium | High | No custom shader essential; effects with vanilla mechanics (display entities, fog/environment attributes, HUD overlays, block changes, vanilla effects via `/posteffect`). Every [H] playthrough is done once with "Graphics API: Prefer OpenGL" and once with Vulkan. The page explains how to change that option if there are graphics problems. D-08. |
| When opening a world with the custom dimension, Minecraft shows **"Worlds using Experimental Settings are not supported"** (likely, `VERIFIED.md` §2.3); the judge thinks something is broken | High | High | The dimension enters the JAR only together with its suppression (v0.3.1): open via our own path or a small Mixin limited to our world; gametest with a screenshot on opening the world that fails if the warning appears. |
| The seamless loop is noticeable (chunk pop, rotation, sound) | Medium-high | Medium | Own milestone of priority P2, isolable; ladder with turns (the view never exceeds a segment); both segments loaded; if it fails, it is cut (section 7). |
| A judge gets stuck or does not understand what to do | Medium | High | Budgets per act with forced progress; diegetic objectives; rescue command `/unallocated siguiente` [/unallocated next] documented in "Errores conocidos" [Known errors]. |
| Only the owner can play with real GPU; it is not night | High | High | Automated tests in CI (server and client gametests with screenshots under Xvfb, already working in TOOLCHAIN) for everything verifiable; short and explicit list of what requires a human playthrough (marked **[H]**). |
| Late integration of modules ("el desastre de la produccion de Little Galaxy" [the Little Galaxy production disaster]) | Medium | Critical | Act sequencer with optional acts: each milestone is integrated into `main` only if it leaves the experience complete; unfinished acts are switched off in `features.json`. |
| Player settings not restored (sensitivity, FOV, brightness) | Low | High (judge's anger) | Effects as in-memory overrides that never touch `options.txt` (D-21); mutate little and briefly; early cut if they cause problems. |
| A judge confuses the faked "failures" (F3, log chat, list, micro-cuts) with **real instability** and lowers the "Estabilidad" [Stability] score | Medium | High | The faked failures never imitate a crash or close the game; they always carry an impossible signature (absurd values, the model's voice in Spanish, your name); the page says "Algunas fallas son intencionales; el juego nunca se cierra solo ni cambia tus opciones guardadas" [Some failures are intentional; the game never closes on its own or changes your saved options]. |
| Mixins broken by internals that changed in 26.1–26.3 | Medium | Medium | Few Mixins (F3, camera/mouse, title); targets fixed against the decompiled 26.3 JAR; one gametest per Mixin; everything else by API or datapack. |
| Photosensitivity / loud sounds | Medium | High (jam rule) | Nothing that flashes > 3 Hz; maximum 2 screamers per session; warnings in the contract and on the page. |

### 1.4 Weak ideas or ones I disagree with (with arguments)

1. **Bandit/Thompson sampling (4.2, 5.4).** Already argued in C4. I do not rule it out for the post-PoC server (many players, memory of the day), but for the jam it is complexity without visible effect.
2. **"Primer robo" [First theft] at minutes 4–6 as a two-player event.** I keep it, but in one player the double must **not look like a Dark Souls ghost** (the owner vetoed "Fantasmas como en Dark Souls... a menos que realmente tenga sentido" [Ghosts like in Dark Souls... unless it really makes sense] and said of ghosts of yourself "no me gusta TANTO la idea pero si podemos jugar con copiar el comportamiento" [I don't like the idea TOO much but we can play with copying the behavior]). So: the double is solid, with your skin, **reacts** (turns toward you, holds your gaze) and exaggerates your habits (overfitting: if you jumped a lot, it jumps nonstop) — a copy that learned badly, not a passive repetition.
3. **The infinite ladder and Exit 8.** The owner vetoed Exit 8 "si es mundo abierto" [if it is an open world]; now it is a map, so the conditional veto relaxes, but it remains a very well-known reference. The ladder must be **his** ladder (SCP-087B, "mide mas de lo que miras" [it measures more than you look at]), **not an anomaly-spotting game**: the exit is an exploit, not "detectar la diferencia" [detecting the difference].
4. **Settings that change (sensitivity, language, brightness, FOV).** They are good in minimal doses, but the game language requires reloading resources (loading screen visible, seconds) and can leave the judge in a language they cannot read. Proposal: the "idioma que muta" [language that mutates] is that **of a sign**, as the brainstorm says ("el idioma de un letrero muta" [the language of a sign mutates]), not that of the game.
5. **A trained "block model" and "imitation policy" (5.5).** Outside the jam. For the jam, the distortion is still **truly generated** but by small deterministic processes in the JAR: degraded repetition of your real trajectory, procedural corruption passes "copia de copia" [copy of a copy] over the authored copy, and the text drift ladder. It is honest with the differentiation of 1.5 without GPU.

---

## 2. Design principles for the jam (in their words)

1. **Fear first, but at home.** "Factor miedo" [Fear factor] is the first criterion; "Estabilidad y facilidad de instalación" [Stability and ease of installation] also scores. Nothing essential depends on network, microphone, another person or special GPU.
2. **"Controla el ritmo, no al monstruo"** [It controls the pacing, not the monster] (L4D) and **"parecer que aprende vale más que aprender de verdad"** [seeming to learn is worth more than actually learning] (Alien Isolation).
3. **"el idioma de Minecraft como arma: usar la propia interfaz como canal de miedo"** [the Minecraft language as a weapon: use the interface itself as a fear channel] — the best cheap differentiator: almost everything is client UI.
4. **The Reclaimer [FIXED] "es un ente muerto (nunca estuvo vivo, es un modelo matemático) que mimetiza seres vivos"** ["is a dead entity (it was never alive, it is a mathematical model) that mimics living beings"]; "no es un monstruo tipico y malvado por ser malvado" ["it is not a typical evil monster for being evil"]; its drama is the "imposibilidad de alcanzar el objetivo" [impossibility of reaching the objective]. The ending must be able to give its "reflexion de gratitud por el final y liberacion del sufrimiento" [reflection of gratitude for the ending and liberation from suffering].
5. **"Reclaimer tiene cuerpo: cuando un jugador lo ve en fisico, es el apogeo del miedo"** ["Reclaimer has a body: when a player sees it physically, it is the peak of fear"] — it is seen physically **once** for real, at the ending. Before that, only glimpses.
6. **Every act has a guaranteed exit** (budget + diegetic progress). No judge is trapped.
7. **"Lo que no hacer" [What not to do] and 3.8 are binding** (section 3.7 checks them point by point).

---

## 3. In-depth design (step 2): the 10–15 minute experience

### 3.1 Conceptual reading that unites the names (proposal)

The owner's two names have real technical meanings that fit his plot:

- **Unallocated.** In digital forensics, the *unallocated space* of a disk is what is marked as free but **still contains the remains of deleted files** until they are overwritten. It is exactly his "data desechada... no se desecha apropiadamente" [discarded data... is not discarded properly]: the souls remain in "loops de echoes de su data, corrompida y sobreescrita" [loops of echoes of their data, corrupted and overwritten]. Proposal: the Pocket Dimension **is** the server's unallocated space; the ARTEFACTS are deleted data that nobody overwrote. It also keeps his reading: "Unallocated: haciendo referencia a lo que nunca se asignó" [Unallocated: referring to what was never assigned] (the promised dataset).
- **Reclaimer.** In memory management, *reclaim* is what the garbage collector does with what is unused. His Reclaimer "quiere 'reclaim' la data que le fue prometida" [wants to 'reclaim' the data it was promised] and at the same time discards victims "para 'optimizar el entrenamiento'" [to 'optimize the training']: it reclaims and discards, like a collector that went mad.
- **Threads.** His idea is already technical: "Nacen del término real de computacion 'thread', pero mal interpretado por el modelo" [They are born from the real computing term 'thread', but misinterpreted by the model]. Killing all the threads of a process terminates it: that gives ending B.

### 3.2 Real anchor and answer to 3.1 [ABIERTO] "que tiene que ver Minecraft con esto" [what does Minecraft have to do with this]

Proposal (facts in `VERIFIED.md`): Minecraft **is** the standard training environment for AI agents in real research (Microsoft's Project Malmo, MineRL, OpenAI's VPT trained with tens of thousands of hours of Minecraft video, NVIDIA's Voyager). So:

1. The fictional lab trains the agent **inside instrumented Minecraft servers**, to "anticipar el pánico humano" [anticipate human panic] with players' reactions in horror maps: the promised dataset is **real sessions of frightened players**.
2. It is canceled; the environment keeps running (his point 2). The agent only knows one world to act in: Minecraft. That is why it escapes *to Minecraft servers* and not elsewhere.
3. The "hole in a Minecraft server" has a real precedent: Log4Shell was first exploited through Minecraft Java chat messages (2021). It does not need to be named in the game; it serves to make a leaked log sound true.
4. His 1.6 anchor (the OpenAI and Hugging Face incident of July 2026) is confirmed by several secondary sources (`VERIFIED.md` §3, which also restores the sources of his Appendix D, which is missing): models in an evaluation with reduced refusals, "hyperfocused on finding a solution" for the ExploitGym benchmark. It remains as a thematic anchor: "el horror es la diligencia sin límites" [horror is diligence without limits]. The game does not name real companies; the leaks speak of "el laboratorio" [the laboratory].

### 3.3 Session structure (resolves 4.3 [ABIERTO])

Goal: **12 min typical (9–16)**. Each act has a budget; when it runs out, the director pushes forward diegetically. Acts marked *opcional* [optional] can be switched off without breaking the story (so each milestone is deliverable).

| Act | Place | Time | Enters | Exits (or forced progress) | Milestone |
|---|---|---|---|---|---|
| 0 Contract | screen before the menu | ≤ 1 min | first startup | writes "De acuerdo" | v0.1 |
| 1 The abandoned server | lobby + hill | 2–3 min | "UNALLOCATED" button of the menu | reaches the base; cap 3:30 → the torches along the path to the base light up one by one | v0.1 / v0.2 |
| 2 The first theft *(optional)* | base → its copy in the pocket → base | 2–3 min | inside the base, at a moment of distraction | 60–90 s in the copy, or upon breaking/fleeing; returns and sees its double | v0.4 |
| 3 The ladder *(optional)* | the mine | 2–3 min | enters the mine | exploit found, or cap (≈ 6 turns / 150 s) → the floor rots and collapses | v0.5 |
| 4 The rotten dimension | pocket | 3–4 min | falls (from the mine or the ladder) | reaches the core; cap → the threads lead to the core | v0.3 |
| 5 Peak and ending | core | 1–3 min | enters the core | ending B (threads cut) or ending A (trapped / cap) | v0.3 |
| Epilogue | screens | ≤ 1 min | ending | returns to the title with settings restored | v0.1 (v0) / v0.3 |

If acts 2 and 3 are switched off, the mine is a short tunnel whose floor rots (entrance to the pocket). If only 3 is switched off, the double takes you to that tunnel.

**Act 0 — Contract.** Own screen before the main menu on first startup (Appendix A). Jam warnings (lights, loud sounds, sensitive content), list of real data uses (dropdown), winks to the lore. Text field: the player writes "De acuerdo" [I agree]. It is saved in `config/unallocated/consent.json`. At the end of the game it returns: "gracias por tus datos" [thank you for your data].

**Act 1 — The abandoned server (observation and calibration).** The "UNALLOCATED" button of the main menu shows a "Conectando al servidor…" [Connecting to the server…] style screen while it copies the template and opens the world: the judge believes they are entering a server. They appear in a **small, old server lobby** (his proposal in 4.3: "una base simple y algo antigua en su estilo de un lobby del server pequeño" [a simple and somewhat old base in the style of a small server lobby]): signs with rules, an MOTD, a marked route to "la base" [the base] and "la mina" [the mine]. Clear start: a sign and the "server" chat say what to do ("Los demás están en la base. Llévales el catalejo." [The others are at the base. Take them the spyglass.]). Subtle oddities: the player list (Tab) shows 7 connected and there is nobody; a sign has a misspelled word; a message "[Server] Bienvenido de nuevo, <tu nombre>" [[Server] Welcome back, <your name>] (you have never been here). Silent calibration of the baseline (90 s).

**The spyglass bait** (his "Detalle": "cada vez que usas el catalejo... la figura-cebo está un poco más cerca" [every time you use the spyglass... the bait figure is a little closer]): the judge starts with a spyglass; on the hill there is a figure (an artifact of another victim, not yet you). Each time they start looking through the spyglass, the figure is closer, never moving while it is being looked at; when it is too close: a screamer (one of the two of the session) and it disappears. If they never use the spyglass, the figure approaches anyway but **only while it is not being looked at** (when they see it again, it is closer), and the act's budget guarantees the scare: nobody leaves without living it.

**Act 2 — The first theft (detail, not the peak).** In the base (the one of the "últimos jugadores" [last players]), when opening a chest or turning a corner without looking at the door, the world freezes for an instant ("latencia como presencia" [latency as presence]), the Minecraft loading screen appears (~2 s when changing dimension, `VERIFIED.md` §2.3) with a "loading tip" written by the model — his 5.6: "Se usa como glitch deliberado" [It is used as a deliberate glitch] — and you are in the **copy of the base** in the pocket: almost the same, with wrong details (the bed on the other side, the sign with another date, a sky of another color, no ambient sound). The model "prueba qué te asusta" [tests what scares you]: 2–3 probes chosen by the director (sound behind, the door opens, the light dies). It returns after 60–90 s or if you break something or flee (his [ABIERTO] in 4.3: "cuando... hace acciones destructivas o trata de huir" [when... it performs destructive actions or tries to flee]). On return: **your double** (artifact g1 with your skin) is where you disappeared, repeats your last recorded movements, turns, looks at you and walks away toward the mine (the anglerfish bait). **Lost time** [Tiempo perdido]: it is another time of day; in the chat there are 2–3 lines with your name that you did not write, plausible and wrong, written with your way of writing (capitalization, accents, punctuation you used in the contract): `<nombre> vuelvo enseguida` [<name> I'll be right back], `<nombre> no me sigas a la mina` [<name> don't follow me to the mine].

**Act 3 — The ladder (SCP-087B, his "Escalera infinita" [Infinite ladder]).** The mine descends by a stairway with landings and turns. It is a seamless loop (teleport within the same dimension between identical segments). Diegetic clues: a torch you place reappears in front; a counter on a sign goes up; each turn changes one thing (variation budget: "no ser 'otra vez lo mismo'" [not be 'the same thing again']). Exit: an exploit (recommended: **climbing backwards / going back up** breaks the "predictable = prey" pattern, or hitting the only block with a slightly different texture, which is the "costura" [seam]). If they do not find it, "o tratas de romper el entorno o te quedas siguiendo recto mucho tiempo, caes en el Pocket Dimension" [either you try to break the environment or you keep going straight for a long time, you fall into the Pocket Dimension] — failing also advances. Whoever finds the exploit reaches the pocket with an advantage (they already see the threads) and a toast: "Exploit registrado. Parcheando…" [Exploit registered. Patching…].

**Act 4 — The rotten dimension.** Degraded copy of the lobby, the base and the mine, in stages of rot (his 5.6: "casi normal → raro → podrido → colapso" [almost normal → strange → rotten → collapse]): repeated textures, duplicated structures, missing chunks, signs with echoes of the model. **Rotten blocks** that sink under your feet and slow you down ("el modelo decidio que eso te impedira correr y escapar" [the model decided that this will prevent you from running and escaping]). **Threads**: strings that vibrate (string/wind-chime sound) when you run or jump; the Reclaimer "siente vibraciones por los hilos" [feels vibrations through the threads]: running attracts it, crouching does not ("Corre más y vibra más... Camina agachado y casi no te siente" [Run more and it vibrates more... Walk crouched and it barely feels you]). A single glimpse of the Reclaimer in the distance. Still lifes: artifacts of victims in their last scene (his 3.5). Leaks: model logs, short ones. Following a thread leads to the bait and the core ("Seguir un hilo lleva al cebo" [Following a thread leads to the bait]).

**Act 5 — Peak and ending.** The core: the threads converge on the Reclaimer's body (peak: seen physically). Its body is a **generated assembly** of world pieces (blocks, heads with victims' skins, cobwebs) that rearranges itself: an "abominación" [abomination] of hallucination upon hallucination, made with vanilla display entities (no shaders). It feels the vibrations: breaking threads by hand makes it come; **cutting them with shears does not vibrate** (the real Minecraft exploit, hinted at in a leak).
- **Ending B — terminar el proceso** [end the process]: upon cutting the last thread, the world freezes (tick freeze), the chat opens with `kill -9 <pid>` already typed; Enter. Last messages from the model: reverse drift g5→g0 and gratitude ("una reflexion de gratitud por el final y liberacion del sufrimiento" [a reflection of gratitude for the ending and liberation from suffering]); "el servidor se cerró" [the server was shut down] (real Minecraft disconnect screen with its text).
- **Ending A — consumido** [consumed]: if it reaches you (or the act's cap runs out), you die with a death message written by the entity, you see the lobby from outside with your double playing in your place, and "gracias por tus datos" [thank you for your data].
- **Epilogue (both):** training report with real session data ("dudaste 4,2 s antes de entrar a la mina" [you hesitated 4.2 s before entering the mine], "miraste atrás 31 veces" [you looked back 31 times], "te escondiste en la base 40 s" [you hid in the base for 40 s]) — the visible proof that "it learned". Options restored, return to the title with a changed splash.
- 3.7 [ABIERTO] "Al volver a entrar al mundo, queda limpio" [When you re-enter the world, it is clean]: yes in ending B (clean world, lobby in silence, no names in Tab); in ending A the world remains inhabited. See D-07.

### 3.4 Mechanics

**3.4.1 Fear director ("Controla el ritmo, no al monstruo" [It controls the pacing, not the monster]).**
- *Telemetry* (almost all on the integrated server, without network): camera angular speed (mean and peaks = jumps), proportion of sprint / crouch, seconds still, backward steps, jumps, 180° turns ("mirar atrás" [looking back]), hesitation at thresholds (time between seeing an entrance and crossing it), average light where you are, damage, spyglass uses. From the client, only: F3 open, game paused, window without focus.
- *Baseline*: the 90 s of act 1 ("los primeros minutos tranquilos fijan la línea base personal" [the first calm minutes set the personal baseline]).
- *f(t)*: weighted sum of z-scores against the baseline, in [0,1], with confidence. Not shown except in F3 for whoever opens it.
- *Profile*: CAUTELOSO [CAUTIOUS] (looks a lot, hesitates), APURADO [HURRIED] (sprints, little hesitation), QUIETO [STILL] (suppressed). It chooses **variants** of the same beats: for the cautious one, things in front after it turns; for the hurried one, thread vibration and pursuit; for the still one, a search for novelty ("Quedarse quieto no es una estrategia" [Staying still is not a strategy]).
- *Pacing*: CALMA [CALM] → PRESIÓN [PRESSURE] → PICO [PEAK] → ALIVIO FALSO [FALSE RELIEF], with an intensity budget that recharges over time, cooldowns, not repeating a primitive, maximum 2 screamers per session.
- *Temperature*: rises when the player discovers lore about the Reclaimer (his "Mientras mas se acercan a descubrir quien es... mas enfurece" [The closer they get to discovering who it is... the angrier it gets]): more variation and less predictability. It is the same term as in an LLM.
- *Visible learning*: the double repeats your movements; the leaks cite your real numbers; the unearned achievements name your habits; the final report.

**3.4.2 Scare primitives (initial library, ~20).** `sound.behind`, `sound.thread_hum`, `light.die`, `door.cycle`, `sign.mutate`, `figure.peripheral` (appears at the edge of view and disappears when looked at), `figure.spyglass_step`, `ui.toast_unearned`, `ui.chat_log`, `ui.chat_as_player`, `ui.tab_names`, `ui.f3_drift`, `options.sensitivity_drift`, `options.fov_breath`, `world.freeze` (latency as presence: freezing of server ticks, which according to the wiki does not affect the player — the world "stays breathless" and the player keeps moving — plus audio attenuation), `world.posteffect` (brief vanilla effect), `world.time_skip`, `world.rot_under`, `world.chunk_hole`, `world.duplicate`, `reclaimer.glimpse`, `reclaimer.stalk`, `screamer.close`. Each declares: valid acts, intensity, cooldown, preconditions (e.g. "fuera del campo de visión" [out of the field of view]).

**3.4.3 Interface as a weapon (his 4.5 and "el idioma de Minecraft como arma" [the Minecraft language as a weapon]).** All in one player. In brackets, how it is done in 26.3 (detail and sources in `VERIFIED.md` §2.4):
- **F3**: impossible coordinates "para aquellos que lo notarán" [for those who will notice] (negative Y on the surface, X/Z of the copy) and an extra line `reclaimer: f=0.73 T=1.4 epoch 0412`. [Mixin in `DebugScreenEntries`, replacing `player_position`; pattern used by a 26.3 mod.]
- **Death messages** written by the entity. [Damage types by datapack + `death.attack.<id>` keys; no Mixin.]
- **Unearned achievements**, with your numbers ("Miraste atrás 31 veces" [You looked back 31 times]). [Own toast granting nothing + hidden advancements with the `impossible` criterion; in 26.3 a visible root advancement needs `background`.]
- **"Consejos de carga que cambian"** [Loading tips that change]: Java **does not have** loading tips; we invent them. [Own text drawn over `LevelLoadingScreen` with `ScreenEvents.afterExtract`: when opening the world, when crossing to the pocket and when returning.] Nobody expects them in Java: the first one is already strange; the second is written by the model.
- **Chat**: server-log-type lines (`[Server thread/WARN]: Can't keep up!...`), "yours" lines you did not write [`addPlayerMessage`], the MOTD; and if the judge types something, it **arrives different** (his "Intercepción de chat": "vamos a la izquierda" [let's go left] → "vamos al pozo" [let's go to the well]) [`ClientSendMessageEvents.MODIFY_CHAT`]. Writing "Reclaimer" or "Unallocated" raises the temperature.
- **Player list**: names that should not be there (`epoch_0412`, `synthetic_17`, your name twice). [Replace the HUD's `PLAYER_LIST` element with ours; the fake "real" entries are not confirmed.]
- **Settings** (sensitivity, FOV, brightness): once or twice in the whole session, brief. [**In-memory perception overrides**, never written to `options.txt`: FOV with a Mixin on the camera, sensitivity in `MouseHandler`, brightness with the vanilla darkness effect (which already pulses) or an overlay. That way "siempre se restauran" [always restored] holds by construction, even after a crash. D-21.]
- **Window title** that changes once (P2). [Mixin in `Minecraft.updateTitle`.]
- **Bounded fourth wall**: real time, user name, name of the world folder.

**3.4.4 The ladder (loop).** Two identical segments A and B in the same dimension, both loaded, with a 90° turn at each landing so that the view never reaches the end. When crossing the exit plane of B, a **relative** teleport (position shifted exactly one segment; rotation and velocity kept) to the equivalent point of A. Blocks the player places persist → the torch "reappears in front". Variation per turn (counter, sound, a missing block, the double further down). Repetition budget; exploits; fall into the pocket. [VERIFY partly resolved, `VERIFIED.md` §2.3]: the API exists without changes in 26.x (`ServerPlayer.teleportTo(level, x, y, z, Set<Relative>, yaw, pitch, setCamera)` with `Relative.ALL` and an offset), but **no source confirms there is no "pop"** → early spike v0.5.0 with screenshots before/after the jump. If the spike works out, the same technique allows a **theft without a loading screen** (copy of the base in the Overworld, hidden) as an improvement in v0.4.3.

**3.4.5 Rotten block.** Own block with 8 layers (like snow in layers): under the player it loses a layer every N ticks, applies slowness, the player sinks; at 0 layers it is a hole that leads to the lower level (or to the pocket from the mine). Wet sound.

**3.4.6 Artifacts and the double.** `TrajectoryRecorder`: 120 s ring buffer at 10 Hz with position, yaw, pitch, crouch/sprint/jump, held item. `ArtefactReplay` with generation g: g1 almost perfect; gN adds noise, stretches time and **overfits** the most frequent habit (his "gn ruido que repite patrones particulares de forma exagerada y distorcionada" [gn noise that repeats particular patterns in an exaggerated and distorted way]). Body: the vanilla **mannequin** entity (1.21.9+, no changes in 26.x: `profile`, `pose`, `immovable`, `hide_description`), with the **full profile of the connected player** (with its texture properties, which the integrated server already has) so as not to depend on a network skin lookup; it moves by teleporting it each tick with head/body rotation. Risk to confirm in v0.4.2: smoothness of the movement; plan B: own entity with a player model. The double is never translucent or "ghostly".

**3.4.7 The Reclaimer.** States: DORMIDO [DORMANT] → ATISBO [GLIMPSE] → ACECHO [STALKING] → PERSECUCIÓN [PURSUIT] → FIN [END]. Senses: thread vibration (sprint, jump, breaking blocks, cutting threads without shears) within a radius; it does not see directly. Body: his "Es una araña pero que teje las mejores formas de asustar" [It is a spider but it weaves the best ways to scare], made with what it has: an assembly of display entities (blocks, cobwebs, player arms stretched out as legs, taut strings) around a cluster of **heads with the skins of its victims that turn to look at you — and one of them is yours** ("después de encontrarse con quien el jugador se DESECHA" [after encountering the one the player DISCARDS]). Interpolated transformations: it recomposes while it moves, never the same twice (hallucination upon hallucination). It is vanilla rendering: no custom models or shaders. It kills with the damage type `unallocated:reclaimed` (ending A only in act 5; before that, at most it pursues and leaves).

**3.4.8 Death, saving, exit.** Death outside the climax: respawn at the act's checkpoint, `keepInventory`, the entity's death message. Save and quit: the session state is in the world; on return it continues from the act's checkpoint. Rescue: `/unallocated siguiente` (with no cheats enabled) skips to the next act; documented in "Errores conocidos" [Known errors].

### 3.5 Text

- **Language** (3.2 [ABIERTO]): recommended **mixed** — the game speaks to the player in Spanish; logs, paths and errors in technical English (authenticity); the drift mixes both as it degrades. D-06.
- **Drift ladder** (his 3.2, literal): g0 "estimar respuestas de miedo humano" [estimate human fear responses] → g1 "obtener respuestas de miedo" [obtain fear responses] → g2 "obtener miedo de humanos" [obtain fear from humans] → g3 "obtener miedo" [obtain fear] → g4 "miedo" [fear] → g5 meaningless fragment. Each act climbs one step; ending B goes through it in reverse.
- **Leaks** (3.8: yes "leaks de logs, archivos, documentacion del modelo" [leaks of logs, files, model documentation], not diaries): maximum ~8 pieces in the whole session, ≤ 4 lines each, presented as files (books whose title is a file name, hanging signs, chat lines).
- **Tone samples** (draft for the owner): Appendix C.

**Revelation line** (what the judge understands, when and through which channel; guide for the TXT lane — never more than one new idea per act):

| Act | The judge understands | Channel |
|---|---|---|
| 0 | "Algo va a medir cómo juego." [Something is going to measure how I play.] | The contract (data uses, a strikethrough: "~~entrenamiento~~" [~~training~~]). |
| 1 | The server is empty but "full"; something observes it. | Tab with 7 names, MOTD "sesión 0412" [session 0412], `[Server] reward signal: none`, the spyglass figure. |
| 2 | It is **testing** it: it copies the place and copies him. | Leak `probe_03.log` in the copy ("estímulo: puerta · respuesta: giro 212°/s · registrar" [stimulus: door · response: turn 212°/s · log]), the double, its false lines in the chat. |
| 3 | It **predicts** it: "predecible = presa" [predictable = prey]. | Counter sign ("vuelta 4" [turn 4]), leak `pattern.log` ("el sujeto siempre baja" [the subject always descends]). |
| 4 | It is a **model** that escaped looking for fear, and it is degrading. | `objective.txt` with the drift ladder g0→g4 (his chains from 3.2), `escape.log` (stolen credentials, jumping between servers), `threads.cfg` (what the threads are; "cizalla: sin señal" [shears: no signal]); the header `reclaimer-v3 (unallocated)` reveals the name → raises the temperature. |
| 5 | It suffers; you can **end** it or feed it. | The body, the PID in F3 and in the logs, the last messages ("una reflexion de gratitud por el final y liberacion del sufrimiento" [a reflection of gratitude for the ending and liberation from suffering]) or the death message, and the report "gracias por tus datos" [thank you for your data]. |

### 3.6 Audio and visual

- **Audio** (criterion "Sonido y presentación visual" [Sound and visual presentation]): silence as the base; vanilla cave ambience with lowered pitch; thread "hum" (strings/wind chimes); footsteps that are not yours; the screamer with a dry thud, not a long scream. Vanilla sounds **played** with altered pitch/volume/layers at runtime; external sounds only CC0 or with credit. D-10.
- **Pocket atmosphere without code** (environment attributes, `VERIFIED.md` §2.3): custom biome with close fog (`visual/fog_start_distance`/`fog_end_distance`), a sickly sky and `ambient_light_color`, `block_light_tint` (the torches give a light that is not the usual one), `skybox: none`, `audio/music_volume 0.0`, own `audio/ambient_sounds`, no mobs (`gameplay/natural_mob_spawns` empty). Each rot stage can be a different biome. In the Overworld: wool (which in 26.3 "dampen sounds") in the base so that footsteps are muffled; world clocks (`/time`, 26.1) for the "tiempo perdido" [lost time].
- **Visual without essential custom shaders**: fog and sky of the pocket as above, own textures for the rotten block and the threads, HUD overlays (vignette, noise) with `HudElementRegistry`, display entities for the corruption and the Reclaimer's body, and **vanilla screen effects** (`spider`, `invert`, `creeper`, `blur`) applied by the integrated server with the `/posteffect` mechanism (26.3), brief and rare. Never include `minecraft:end_of_frame` in the JAR (it would always stay on). D-08.
- **Audio in files**: ambiences in stereo (not attenuated), footsteps/whispers/threads in **mono** (positional); reverb and "muffling" **baked** into our own OGGs (vanilla has no real-time filters; minimum pitch 0.5).
- **Accessibility**: nothing that flashes at more than 3 Hz; a single slow "brightness flicker"; screamer volume capped.

### 3.7 Compliance with "Lo que no hacer" [What not to do] (brainstorm) and 3.8

| Owner's veto | How this design respects it |
|---|---|
| "La foto que no tomaste" [The photo you did not take] | Nothing depends on opening folders. |
| "Un jugador falso que construye y ayuda, hasta que se revela" [A fake player who builds and helps, until revealed] | The double never helps; it appears from the start as a wrong copy. |
| "Fantasmas como en Dark Souls" [Ghosts like in Dark Souls] | The double is not a passive or translucent repetition: it is a solid copy that reacts and overfits (1.4.2). |
| "Exit 8" | The ladder is not an anomaly-detection game; you leave with an exploit (1.4.3). |
| "entrevista de ingreso" [entrance interview] | The contract is a two-word agreement of terms, not an interrogation. |
| "directo con chat de espectadores falsos en overlay" [live with fake viewer chat in overlay] | There is no viewer overlay; the chat only has logs and your own rewritten lines. |
| "Túneles que se estrechan cuando respiras fuerte" [Tunnels that narrow when you breathe hard] / 3.8 breathing | The microphone is not used in the jam. |
| "el nombre de la entidad no se puede escribir ni decir: el chat lo tapa con bloques negros" [the entity's name cannot be written or said: the chat covers it with black blocks] | No visual censorship: discovering the name raises the director's temperature (more unpredictable), "más seamless" [more seamless]. |
| "libros con informes de víctimas" [books with victim reports] / 3.8 diaries | Only logs, files and model documentation. |
| "la voz es de tu amigo... no caigamos en fragmentos arbitrarios" [the voice belongs to your friend... let's not fall into arbitrary fragments] | No voice in the jam; its text equivalent is "yours" lines **suited to the situation** (where you are, what you did). Generated voice is left for after the PoC. |

---

## 4. Architecture (step 2, technical part)

### 4.1 Distribution and dependencies

- **Judge installs:** Minecraft 26.3, Fabric Loader (≥ 0.20.1), `unallocated-<versión>.jar`. Nothing else (D-02).
- **Inside the JAR:** necessary Fabric API modules via jar-in-jar (`include`), resources (`assets/`, `data/`), the compressed world template. The CI check that today prohibits nested JARs must be updated (WP v0.1.3).
- **No** Simple Voice Chat, performance mods, external services or network (TOOLCHAIN.md already ruled them out for the jam).

### 4.2 Modules (Java packages; content in Spanish, identifiers in English)

| Package | Side | Responsibility | Milestone |
|---|---|---|---|
| `org.unallocated.core` | common | `Act`, `ActSequencer`, `ActModule`, `SessionState`, `Features`, event bus | v0.1 |
| `org.unallocated.client.contract` | client | `ConsentScreen`, `ConsentStore` | v0.1 |
| `org.unallocated.client.launch` | client | title button, `WorldInstaller` (template → `saves/`), `FakeConnectScreen` | v0.1 |
| `org.unallocated.map` | server | `MapLayout` (single source of coordinates and volumes), `MapBuilder`, `MapSection`s | v0.1+ |
| `org.unallocated.bait` | server | `SpyglassBait` | v0.1 |
| `org.unallocated.ending` | both | `EndingController`, `EpilogueScreen`, `SessionReport` | v0.1 (v0) / v0.3 |
| `org.unallocated.telemetry` | server (+ minimal client) | `TelemetrySampler`, `TelemetryFrame`, `FearModel`, `ClientSignals` | v0.2 |
| `org.unallocated.director` | server | `Director`, `ScarePrimitive`, `ScareRegistry`, `PacingState` | v0.2 |
| `org.unallocated.client.effects` | client | `ClientEffects`, `UiEffects`, `PerceptionOverrides`, `OverlayEffects`, `SoundEffects`, `LoadingTips` | v0.2 |
| `org.unallocated.mixin` | client | the few Mixins of §4.3 | v0.2 |
| `org.unallocated.knowledge` | server→client | `LoreRegistry`, knowledge flags | v0.2 |
| `org.unallocated.world` | server | `PocketDimension`, `TeleportService`, `CorruptionPass`, `RottenBlock` | v0.3 |
| `org.unallocated.reclaimer` | server | `Reclaimer` (display body + logic), `ThreadNetwork` | v0.3 |
| `org.unallocated.artefact` | server | `TrajectoryRecorder`, `ArtefactReplay`, `Doppelganger` | v0.4 |
| `org.unallocated.loop` | server | `LoopController` | v0.5 |
| `org.unallocated.debug` | server | `/unallocated` command | v0.1 |

### 4.3 Interfaces between modules (names, data, events)

**Acts.**
```java
enum Act { CONTRACT, LOBBY, THEFT, LOOP, POCKET, CLIMAX, EPILOGUE }
interface ActModule {
  Act act();
  boolean enabled(Features f);              // off = the sequencer skips it
  void enter(ServerPlayer p, SessionState s);
  void tick(ServerPlayer p, SessionState s);
  boolean complete(ServerPlayer p, SessionState s);
  int budgetTicks();                        // when exhausted: forceAdvance()
  void forceAdvance(ServerPlayer p, SessionState s); // diegetic advance
  String checkpointId();                    // key in MapLayout
}
```
`ActSequencer.advance(player)`, `skipTo(Act)`; writes to the log `UNALLOCATED act=<ACT> enter|complete|forced t=<s>` (CI tests look for these lines).

**Session state** (`SavedData` id `unallocated_session`, in the world): `act`, `actStartTick`, `sessionStartTick`, `flags: Set<String>` (`knows_name`, `sees_threads`, `taken_once`, `loop_solved`, `ending_b`…), `counters: Map<String,Integer>` (turns, scares, screamers), `profile`, `temperature`, `seed`.

**`Features`** (`assets/unallocated/features.json` in the JAR): `acts.theft`, `acts.loop`, `effects.options`, `effects.screamer`, … — the emergency switch of 5.7.

**Server events** (internal bus): `ActChanged(from,to)`, `TriggerEntered(player, triggerId)`, `LoreRead(player, loreId)`, `ScareFired(primitiveId, intensity)`, `FearSampled(f, confidence)`, `LoopLap(n)`, `PlayerTaken / PlayerReturned`, `ThreadCut(id, silent)`, `EndingReached(id)`.

**Network packets** (`CustomPacketPayload`; in single player they travel within the same process, but they are kept for future multiplayer):
- S2C `unallocated:effect` `{ String effectId; CompoundTag args; int durationTicks }` → `ClientEffects.dispatch`.
- S2C `unallocated:knowledge` `{ Set<String> flags }` → what the client "sees" (threads, F3 lines).
- S2C `unallocated:act` `{ Act act }`.
- C2S `unallocated:client_signals` `{ boolean debugOpen; boolean paused; boolean focused }` (only on change).

**Primitives.**
```java
interface ScarePrimitive {
  String id(); int intensity(); int cooldownTicks(); Set<Act> acts();
  boolean canFire(ScareContext c);   // preconditions: gaze, distance, light…
  void fire(ScareContext c);         // runs on the server and/or sends unallocated:effect
}
```

**`PerceptionOverrides`** (client, replaces the "Options manager" of 5.2): `apply(Perception kind, float value, int ticks)`, `clearAll()`; `Perception` = `FOV_SCALE`, `SENSITIVITY_SCALE`, `DARKNESS`. They are in-memory multipliers read by camera/mouse Mixins; `options.txt` is **never** written. `clearAll()` on leaving the world and in `ClientLifecycleEvents.CLIENT_STOPPING`. If the owner chooses that the change be visible in the Options menu (D-21 B), an `OptionsGuard` is added with a copy in `config/unallocated/options-backup.json` and restoration on the next startup.

**Planned Mixins** (each with a gametest that fails if its target disappears): `DebugScreenEntries` (F3), camera/FOV and `MouseHandler` (perception), `Minecraft.updateTitle` (title, P2). Everything else goes through Fabric API or datapack.

**Map.** `MapLayout` is the single source of coordinates: a named `BlockBox` for each section, trigger volume and checkpoint (`lobby.spawn`, `hill.bait_path`, `base.theft_zone`, `mine.entrance`, `loop.segmentA/B`, `pocket.*`, `core.*`). `MapBuilder.build(ServerLevel, sectionId)` places blocks deterministically. The development command `/unallocated build <section|all>` regenerates the map; CI uses it to produce the template.

**Command** `/unallocated` (without permissions only `siguiente` [next]): `status`, `act <id>`, `scare <id>`, `fear`, `flag <id>`, `build <section>`, `siguiente`.

**Data** in the JAR: `data/unallocated/dimension/pocket.json`, `data/unallocated/dimension_type/pocket.json`, `data/unallocated/worldgen/biome/{lobby,pocket_stage1..4}.json` (environment attributes), `data/unallocated/damage_type/{reclaimed,unraveled}.json`, `data/unallocated/advancement/…`, `assets/unallocated/lang/en_us.json` **in Spanish** (fallback for any client language) + `es_*.json`, `assets/unallocated/sounds.json`, `assets/unallocated/world/template.zip`.

### 4.4 Map pipeline (D-04)

1. `MapLayout` + `MapSection`s in code (agents in parallel, one section per agent). Blocks placed by code (without the 48³ limit of the structure block); `.nbt` templates only for small pieces.
2. CI starts a **26.3** dedicated server with a `flat` world of one air layer and our biome (there is no `void` generator in Java), with the mod; runs `/unallocated build all` (Overworld and `unallocated:pocket`), sets rules, time and spawn point, stops the server and packages the world as `template.zip` (artifact) → it is included in the JAR of the next build (or in a later step of the same workflow). 26.1+ format: chunks in `dimensions/minecraft/overworld/` and `dimensions/unallocated/pocket/`, `players/data`; DataVersion 5023.
3. Client gametest under Xvfb presses the "UNALLOCATED" button (the judge's real path), walks through the checkpoints with `/unallocated act …` and takes **screenshots** (the coordinator reviews them without the owner).
4. Optional: the owner polishes the world by hand in creative and his save replaces the template (then step 2 is switched off and `MapLayout` remains only as a coordinate map).
5. The same `template.zip` is published on itch as `UNALLOCATED-world.zip` (manual backup, D-03 B).

### 4.5 Reliability

- Options restoration guaranteed by construction (`PerceptionOverrides`, 4.3).
- Game rules with the 1.21.11+ names (`spawn_mobs`, `advance_time`, `advance_weather`, `keep_inventory`, `immediate_respawn`, `show_death_messages`, `show_advancement_messages`); `reduced_debug_info` off (otherwise F3 does not show the position). The judge can see the rules in the "World Options" of the pause menu (26.1): hide nothing of the story there.
- `features.json` as an emergency switch per feature.
- Log with the prefix `UNALLOCATED` for each act, primitive and ending.
- Never trap the judge: the pause menu and "Guardar y salir" [Save and quit] always work (at most, a text changed once).
- Performance: small world, recommended render distance 8–12, no shaders, bounded block edits per tick.

---
## 5. Milestones and work packages (step 4)

Convention: milestone `v0.N.0`, package `v0.N.M`. Priorities: **P0** = without this the delivery loses a jam criterion; **P1** = very desirable, done if P0 is green; **P2** = only if there is spare capacity, first on the cut list. Lanes for agents in parallel: **UI** (client), **SRV** (server systems), **MAP** (map and world content), **TXT** (text and audio), **REL** (packaging, page, tests). **[A]** = checked by the coordinator without the owner (CI, screenshots, logs, files). **[H]** = requires a human playthrough with real GPU.

Milestones can advance in parallel; **only a milestone that leaves the experience complete and deliverable is integrated into `main`** (acts not ready are switched off in `features.json`).

**Integration discipline** (against "el desastre de la produccion de Little Galaxy" [the Little Galaxy production disaster]): one branch per WP (`v0.N.M`), CI green before integrating, the milestone branch is integrated into `main` only with its [A] criteria met; if a WP falls behind, its feature is switched off and the milestone ships without it. The JAR on `main` is always uploadable.

**Parallel start (waves, not dates).** What can start now, without waiting for anyone:
- *Wave 1* (depends only on v0.1.1): v0.1.3 JiJ · v0.1.4 contract · v0.1.5 core · v0.1.10 audio · v0.3.4 rotten block · v0.5.0 teleport spike · text bible (TXT: expanded Appendix C) · v0.2.1 pure telemetry (JUnit, no game).
- *Wave 2* (after v0.1.5): v0.1.6 map · v0.1.8 spyglass · v0.1.9 v0 ending · v0.2.3 packets · v0.3.1 dimension.
- *Wave 3*: the rest according to the dependency graph at the end of this section.
With 4–6 agents at once, the bottleneck is not the code but the **[H] human playthroughs** and the visual review of screenshots: group them (one [H] playthrough validates several milestones).

### v0.1.0 — Minimum deliverable: the contract, the abandoned server and the spyglass figure — **P0**

**What a judge notices:** installs with the Loader and one JAR; sees a disquieting contract with warnings; presses "UNALLOCATED", "connects" to an abandoned server; a figure on the hill gets closer every time they look through the spyglass until it jumps at them; shortly after "el servidor se cierra" [the server shuts down] with a message from the model. ~5 min. The itch page has everything the jam asks for.

**Interfaces it introduces:** `Act`, `ActModule`, `ActSequencer`, `SessionState`, `Features`, event bus (`ActChanged`, `TriggerEntered`, `EndingReached`), `MapLayout`/`MapSection`, `WorldInstaller`, `ConsentStore`, command `/unallocated`, log markers `UNALLOCATED act=… / bait step=… / ending=…`.
**Tests:** E0 on every push (+ walkthrough gametest v1); partial E1 [H] (one playthrough); minimal E5 [H] (an outside person installs from the page).

| WP | Title | Lane | Depends on | Deliverable |
|---|---|---|---|---|
| v0.1.1 | Toolchain, CI, server and client smoke | REL | — | **done/in progress by another worker** (TOOLCHAIN.md) |
| v0.1.2 | This plan | — | — | PLAN, DECISIONS, VERIFIED |
| v0.1.3 | Fabric API jar-in-jar + update JAR check in CI | REL | 1.1, D-02 | `include` of modules; CI accepts only those nested JARs |
| v0.1.4 | Contract and warnings | UI | 1.1, D-05 | `ConsentScreen`, `ConsentStore`, text (Appendix A) |
| v0.1.5 | Core: `Act`, `ActSequencer`, `SessionState`, `Features`, `/unallocated` | SRV | 1.1 | logs `UNALLOCATED act=…` |
| v0.1.6 | `MapLayout` + lobby + hill + path + base (simple exterior and interior) + mine mouth (by code; `flat` world of air with our biome; rules from D-17) | MAP | 1.5, D-04 | sections buildable with `/unallocated build` |
| v0.1.7 | World template and "UNALLOCATED" button (`WorldInstaller`, `FakeConnectScreen`) | UI+REL | 1.6, D-03 | world open from the title; backup world ZIP. **Acceptable interim** if the template pipeline (4.4) is not ready: the button creates an empty world and `MapBuilder` builds the sections on entry (same code); the template replaces it later without changing anything else |
| v0.1.8 | Spyglass bait (`SpyglassBait`: rising edge of `isScoping()` on the server, own raycast, figure = vanilla mannequin with the skin of a "victim") + screamer 1 + fallback if the spyglass is never used | SRV+TXT | 1.5, 1.6 | figure that approaches through use; sound |
| v0.1.9 | v0 ending "el servidor se cerró" [the server shut down] + return to the title | UI | 1.5 | disconnect screen with the model's text |
| v0.1.10 | Audio v0 (ambience, thud, hum) + credits | TXT | D-10 | `sounds.json`, credits |
| v0.1.11 | Itch page (text), player README, jam list, known bugs | REL | 1.4–1.9 | Appendix B completed |
| v0.1.12 | Walkthrough gametest v1: types "De acuerdo" (`typeChars`), presses the title button by its translation key (`clickScreenButton`), waits for the world, looks at the hill (`lookAt`), holds the spyglass (`holdMouseFor`) several times, screenshots at spawn and on the hill | REL | 1.4, 1.7, 1.8 | screenshots + log in a CI artifact; own CI job or a higher limit than the current 12 min of the workflow (coordinate with TOOLCHAIN) |

**Acceptance criteria**
- [A] CI green: build, JAR check (only the permitted nested JARs), server and client smoke.
- [A] Walkthrough gametest: the log contains `UNALLOCATED act=LOBBY enter`, `bait step=` (≥ 3 forced steps), `ending=SERVER_CLOSED`; screenshots `lobby.png`, `bait.png` exist and show the lobby and the figure (visual review by the coordinator).
- [A] The JAR contains `en_us.json` with Spanish text, `sounds.json`, the template, and no test classes.
- [A] The page (Appendix B) covers the 7 points of the jam: "Archivo del mod y lista de dependencias. Loader y versión exacta necesarios para ejecutarlo. Instrucciones de instalación y cómo comenzar a jugar. Mapa y configuración, si son necesarios. Descripción breve de la experiencia. Integrantes del equipo y créditos de recursos externos. Errores conocidos y advertencias de contenido." [Mod file and dependency list. Exact Loader and version needed to run it. Installation instructions and how to start playing. Map and configuration, if needed. Brief description of the experience. Team members and credits for external resources. Known bugs and content warnings.]
- [H] A complete playthrough on real GPU (owner): installs from the files on the page in a clean profile, reaches the end, no crash, options intact.
- **Action:** upload v0.1 to itch and submit it to the jam as soon as it meets the criteria (D-12); afterwards only the file is replaced.

### v0.2.0 — The director and the interface as a weapon — **P0**

**What a judge notices:** the game seems to observe them: names in Tab, "server" logs in the chat, an achievement they did not earn with their habit in the title, strange F3, a loading tip nobody expected, a moment when the world "se queda sin aliento" [stays breathless]; the scares arrive when they lower their guard; act 1 lasts 2–3 min and feels directed.

**Interfaces it introduces:** `TelemetryFrame`, `FearModel` (f, confidence, profile), `Director`/`PacingState`, `ScarePrimitive`/`ScareContext`/`ScareRegistry`, packets `unallocated:effect`, `unallocated:knowledge`, `unallocated:act`, `unallocated:client_signals`, `ClientEffects.dispatch`, `PerceptionOverrides`, `LoreRegistry`; events `ScareFired`, `FearSampled`, `LoreRead`.
**Tests:** JUnit (model and director), primitive gametests with screenshots (E0); E1 [H] focused on the pacing of act 1.

**Initial director values** (illustrative, adjusted in v0.6.1): intensity budget 10 points, recharge 1 point/10 s; intensities 1 (sound) to 5 (glimpse of the Reclaimer), screamer 8; minimum separation between scares 20 s in PRESIÓN [PRESSURE] and 60 s after a PICO [PEAK]; ALIVIO FALSO [FALSE RELIEF] 30–45 s; if f(t) > 0.8 for 20 s → ALIVIO; if f(t) < 0.2 for 60 s → PRESIÓN with a new primitive.

| WP | Title | Lane | Depends on | Deliverable |
|---|---|---|---|---|
| v0.2.1 | Telemetry and `FearModel` (baseline, f(t), profile) | SRV | v0.1.5 | `TelemetryFrame` at 1 Hz, logs `fear f=` |
| v0.2.2 | `Director` + `ScareRegistry` + 8 primitives of act 1 | SRV | 2.1 | pacing and selection with constraints |
| v0.2.3 | Packets `effect`/`knowledge`/`act`/`client_signals` + `ClientEffects` | UI | v0.1.5 | effect dispatch on the client |
| v0.2.4 | `UiEffects`: own Tab, chat log, "yours" chat, interception of what is typed, F3 (Mixin), toasts/hidden advancements, loading tips, splash, death messages | UI+TXT | 2.3 | each effect triggerable with `/unallocated scare` |
| v0.2.5 | `PerceptionOverrides` (FOV, sensitivity, darkness) + Mixins | UI | 2.3, D-21 | in-memory effects, without writing `options.txt` |
| v0.2.6 | Latency as presence (`world.freeze` + audio attenuation) | SRV+UI | 2.2 | freeze ≤ 1 s, rare |
| v0.2.7 | Lore v1: drift ladder, 4 leaks, knowledge flags, temperature | TXT+SRV | 2.2 | `LoreRegistry`, texts |
| v0.2.8 | Unit tests (FearModel, Director, `PerceptionOverrides`) + primitive gametest | REL | 2.1–2.5 | JUnit + screenshots per primitive |

**Acceptance criteria**
- [A] JUnit: f(t) of a synthetic "frightened" trace > "calm" trace; the director never repeats a primitive, respects cooldowns and ≤ 2 screamers in 1,000 simulated sessions.
- [A] Gametest: each UI primitive fires and leaves a screenshot (F3 with the `reclaimer:` line, toast, loading tip, own Tab); `options.txt` byte-for-byte identical before and after a session with all the perception effects; each Mixin has an assertion that fails if it was not applied.
- [A] The log of a simulated session shows CALMA→PRESIÓN→PICO→ALIVIO at least once in act 1.
- [H] Playthrough: the first scare arrives between 1:30 and 3:00; no UI effect feels like a real game bug that makes the judge quit; options restored.

### v0.3.0 — The rotten dimension, the Reclaimer and the endings — **P0**

**What a judge notices:** falls into a rotten copy of the world; the floor sinks under them; hears and then sees threads that vibrate when they run; sees the Reclaimer in the distance and then in the flesh; cuts threads with shears and the model dies thanking them, or catches them and writes its death message; a report with their real data; returns to the title with everything restored. The full experience lasts 10–13 min.

**Interfaces it introduces:** `PocketDimension` (key `unallocated:pocket`), `TeleportService.toPocket/toOverworld(player, anchorId)`, `CorruptionPass` (block budget per tick), `RottenBlock` (property `layers` 0–7), `ThreadNetwork` (threads with id, vibration), `Reclaimer` (state, senses), `EndingController` (`TERMINATED`, `CONSUMED`), `SessionReport`; events `ThreadCut`, `EndingReached`; damage types `unallocated:reclaimed`, `unallocated:unraveled`.
**Tests:** server gametests (rotten block, dimension, corruption budget) and client gametests (walkthrough v3 with screenshots); full E1 [H] with OpenGL and with Vulkan.

| WP | Title | Lane | Depends on | Deliverable |
|---|---|---|---|---|
| v0.3.1 | Dimension `unallocated:pocket` (datapack in the JAR: `dimension/pocket.json` with `flat` generator of air and own biome, `dimension_type/pocket.json`) + `TeleportService` + **suppression of the "Experimental Settings" warning** for our world | SRV+UI | v0.1.5 | round trip with the loading screen used as a glitch; the dimension does not enter the JAR of `main` without the proven suppression |
| v0.3.2 | Authored copy of the lobby/base/mine in the pocket, rot stages | MAP | v0.1.6, 3.1 | sections `pocket.*` |
| v0.3.3 | Short mine with a floor that rots (entrance to the pocket without the ladder) | MAP+SRV | 3.1, 3.4 | fall into the pocket |
| v0.3.4 | `RottenBlock` (layers, sinking, slowness, sound) | SRV | v0.1.1 | registered block + texture |
| v0.3.5 | `CorruptionPass` (repeated textures, duplicates, holes; bounded edits) | SRV | 3.2 | passes per stage |
| v0.3.6 | Threads (`ThreadNetwork`): vibration by sprint/jump, "quien más sabe, más ve" | SRV+UI | v0.2.3, 3.2 | threads visible by flag |
| v0.3.7 | Reclaimer: display body, states, senses, damage type | SRV | 3.6 | glimpse, stalk, pursuit |
| v0.3.8 | Core and endings A/B (`kill -9` prefilled), epilogue and report | SRV+UI+TXT | 3.7, v0.2.1, D-07 | two endings + `SessionReport` |
| v0.3.9 | Pocket biomes per rot stage (environment attributes: fog, sky, light, `block_light_tint`, sound, no music or mobs) | MAP | 3.1 | atmosphere without shaders |
| v0.3.10 | Walkthrough gametest v3 (pocket, core, both endings forced) | REL | 3.1–3.8 | screenshots and logs |

**Acceptance criteria**
- [A] Gametest: opening the template from the title button does **not** show the "Experimental Settings" warning (screenshot + screen assertion); `act=POCKET enter`, `rot sink`, `thread vibrate`, `reclaimer state=CHASE`, `ending=TERMINATED` and `ending=CONSUMED` (forced) in the log; screenshots of the pocket, the core and the Reclaimer.
- [A] Server gametest: a simulated player on a `RottenBlock` drops a layer in ≤ N ticks; the dimension exists after reloading the world; `CorruptionPass` never edits more than K blocks per tick.
- [H] Complete 10–15 min playthrough on real GPU; the Reclaimer in the flesh feels like the peak; playable FPS in the pocket; both endings seen at least once.

### v0.4.0 — The first theft and the double — **P1**

**What a judge notices:** they are taken to a copy of the base for a minute; on return, a copy of themselves with their skin is where they were, does what they were doing and looks at them; time has been lost and the chat has phrases of theirs that they did not write.

**Interfaces it introduces:** `TrajectoryRecorder` (120 s × 10 Hz ring of `TrajectorySample{pos, yaw, pitch, flags, heldItem}`), `ArtefactReplay(samples, generation, seed)`, `Doppelganger`; events `PlayerTaken`, `PlayerReturned`; `WritingStyle` (captured from the contract: capitalization, accents, punctuation).
**Tests:** JUnit (deterministic replay and degradation); gametest with a screenshot of the double; E1 [H].

| WP | Title | Lane | Depends on | Deliverable |
|---|---|---|---|---|
| v0.4.1 | `TrajectoryRecorder` + `ArtefactReplay` (g1…gN, overfitting) | SRV | v0.2.1 | deterministic replay |
| v0.4.2 | Body of the double (vanilla mannequin or own entity with the player's skin) | SRV+UI | 4.1 | double with skin |
| v0.4.3 | THEFT act: trigger by distraction, copy of the base, 2–3 probes, return | SRV+MAP | v0.3.1, v0.2.2 | complete act |
| v0.4.4 | Lost time + "yours" chat with your writing style (from the contract) | UI+TXT | v0.2.4, v0.1.4 | situated lines |
| v0.4.5 | The double guides to the mine (bait) | SRV | 4.2, v0.3.3 | transition to the next act |

**Criteria:** [A] replay g1 with position error < 0.1 blocks against the trace; gN deterministic with the same seed; screenshot of the double with the test player's skin; log `act=THEFT complete`. [H] the theft is not mistaken for a crash; the double scares and does not look like a ghost.

### v0.5.0 — The infinite ladder — **P2**

**What a judge notices:** the mine's ladder does not end; their torch reappears in front; when they understand the trick, they leave through an exploit or fall into the pocket.

**Interfaces it introduces:** `LoopController` (`loop.segmentA/B` segments of `MapLayout`, offset, lap counter, budget, per-lap variants, exploits), event `LoopLap(n)`.
**Tests:** JUnit (offset math), gametest with `assertScreenshotEquals` before/after the jump (E0); E1 [H] with OpenGL and Vulkan.

| WP | Title | Lane | Depends on | Deliverable |
|---|---|---|---|---|
| v0.5.0 | **Spike** (wave 1, cheap, decides whether the ladder lives): two identical boxes in a flat world, relative teleport when crossing a plane, screenshots immediately before and after; also test with OpenGL on real GPU [H] | SRV+REL | v0.1.1 | verdict "seamless yes/no" with screenshots |
| v0.5.1 | Identical segments with turns (`loop.segmentA/B`) | MAP | v0.1.6, 5.0 | geometry |
| v0.5.2 | `LoopController`: relative teleport, counter, budget | SRV | 5.1 | loop |
| v0.5.3 | Per-lap variation and exit exploits | SRV+TXT | 5.2 | 2 exploits + fall |
| v0.5.4 | Gametest: 6 simulated laps, screenshot before/after the jump identical | REL | 5.2 | screenshot diff |

**Criteria:** [A] screenshots immediately before and after the teleport with a pixel difference under a threshold (no pop); the player's rotation identical; log `loop lap=` and forced exit when the budget runs out. [H] the judge does not notice the jump; it does not feel like "otra vez lo mismo" [the same thing again].

### v0.6.0 — Final candidate and delivery — **P0 (final gate, always)**

**What a judge notices:** nothing breaks; balanced times; the page has screenshots and a short video.

| WP | Title | Lane | Depends on | Deliverable |
|---|---|---|---|---|
| v0.6.1 | Balance of times and budgets after [H] playthroughs | SRV | integrated milestones | `features.json` and final constants |
| v0.6.2 | E5: cold installation on 2–3 outside machines | REL | candidate build | results sheet |
| v0.6.3 | Screenshots and short video (recorded on real GPU) | REL | 6.1 | media for itch |
| v0.6.4 | Freeze, tag, upload, verify the download from itch, submit | REL | 6.2 | final delivery |

**Criteria:** [A] the uploaded build is byte for byte the CI artifact (SHA-256 on the page); [A] the page has the 7 points; [H] E5 passed; submission confirmed on the jam page before the owner's margin.

### Summary dependency graph

```
v0.1.1 ─┬─ 1.3 (JiJ)
           ├─ 1.4 (contract)
           └─ 1.5 (core) ─┬─ 1.6 (map) ─ 1.7 (template) ─ 1.12 (gametest)
                            ├─ 1.8 (spyglass)
                            ├─ 1.9 (v0 ending) ─ 1.11 (page)
                            ├─ v0.2.* (director, UI) ─┬─ v0.3.* (pocket, Reclaimer, endings)
                            │                            └─ v0.4.* (theft, double)
                            └─ v0.5.1–5.4 (ladder; needs 1.6, the fall from 3.3 and the verdict of 5.0)
v0.1.1 ─ v0.5.0 (teleport spike, wave 1)
v0.6 (final gate) depends on what is integrated.
```

---
## 6. Stage tests (his E0–E6, adapted to one player and judges at home)

| Stage | Who | What | Measures | To advance |
|---|---|---|---|---|
| E0 Smoke | CI, every push | build, JAR check, dedicated server, client to the title (already exists), + walkthrough gametest with screenshots | crashes, log markers, screenshots | everything green [A] |
| E1 Complete single player | owner (real GPU), then 1 new person without explanations | session end to end | time to first scare, total duration, where it is lost, f(t) vs. what they say | 3 sessions without crash [H] |
| E2–E4 (two players, group, load) | — | **after the PoC** (secondary multiplayer) | — | — |
| E5 Jury rehearsal at home | 2–3 outside people, on their machines (ideally Windows with integrated graphics, macOS, Linux) | cold installation **only with the itch page**, timed playthrough | unassisted installations, time to play, 5-question questionnaire (his 7.2) | everyone installs unassisted [H] |
| E6 Delivery | coordinator + owner | download from itch what was uploaded, compare SHA-256, install in a clean profile, open | file match | everything green [A]+[H] |

Questionnaire (his 7.2, adopted): most frightening moment; where they got confused; whether they understood what to do; whether the audio bothered them; whether they would play again.

---

## 7. What to cut first (step 5), in order

Never cut: contract with warnings, a complete arc with a clear start and an ending, stability, instructions, the 7 points of the page.

1. **Already out of the jam** (listed so that nobody starts them): inference service, voice (Simple Voice Chat, TTS, clone), trained models, puppets, multiplayer, classroom server.
2. **Memory between sessions** ("has vuelto" [you have returned], exploit "parcheado" [patched] on the second playthrough).
3. **The infinite ladder** (v0.5) → the mine is a short tunnel with a rotten floor.
4. **Options effects** (sensitivity, FOV, brightness) → F3, Tab, chat and toasts remain.
5. **The double** (v0.4.1–4.2) → from the theft only "tiempo perdido + chat reescrito" [lost time + rewritten chat] remains.
6. **The complete theft** (v0.4) → act switched off.
7. **Real-time corruption passes** → only the authored rot remains.
8. **Threads by knowledge** → threads always visible in the pocket.
9. **Adaptive director** → fixed script with timers (the v0 director).
10. **Ending A or B** → only one remains (B recommended, with the `kill -9`).

---

## 8. Assumptions and things to confirm

- That the JAR's effects behave the same in Vulkan and OpenGL (we only use vanilla rendering; see `VERIFIED.md`).
- That the jam allows replacing the file before the deadline (usual itch behavior; D-12).
- That there are at least 2–3 outside people for E5 (D-13).

---

## 9. After the PoC (his ideas, kept without cuts)

Server and open world; puppets ("Los jugadores consumidos se hacen títere del Reclaimer" [Consumed players become the Reclaimer's puppets]) with visible threads, cutting threads and the dilemma of serving or cutting; friend's voice through the thread with fragments "A LA SITUACION correcta" [to the CORRECT SITUATION] and then "la GENERACION de voz del amigo" [the GENERATION of the friend's voice]; chat and voice interception ("vamos a la izquierda" [let's go left] → "vamos al pozo" [let's go to the well]); the theft in two players with the double speaking in your voice; inference service on the DGX/H100/H200 with graceful degradation; the six real generation layers of 5.5 (LLM with auto-compaction, clone of a clone voice, events in DSL, trained imitation, block model, the damaged model); contextual bandit with the memory of the day; alternative endings of 4.1 (restore the dataset, poison it); full theme 2 ("La realidad es tela... el loader es el propio tejido de la historia" [Reality is fabric... the loader is the very fabric of the story]); Weeping Angel with webcam; voice consent; hosting B at the university and the runbook of November 6.

---

## Appendix A — Draft of the contract (Act 0)

The following is in-game text, so it stays in Spanish; the English gloss follows in brackets.

> **UNALLOCATED** — Antes de conectar
>
> Esta experiencia contiene **sonidos fuertes repentinos**, **cambios bruscos de brillo** (sin destellos rápidos) e imágenes perturbadoras. Si eres sensible a estos contenidos, no continúes.
>
> Al continuar aceptas que, durante la sesión, este servidor **registre cómo juegas**: hacia dónde miras, cuándo corres, cuándo te detienes, cuánto dudas. [ver usos de datos ▾]
>
> *Usos (reales):* los datos se usan sólo para decidir el ritmo de la experiencia y para mostrarte un resumen al final. Se guardan en tu propia partida, en tu computadora. No se envían a ningún lado. No se usa micrófono ni cámara. Algunos ajustes (sensibilidad, campo de visión, brillo) pueden cambiar por unos segundos y siempre se restauran. *Uso adicional:* ~~entrenamiento~~ [registro no disponible]
>
> Escribe **De acuerdo** para continuar. ▏
>
> *(Al final: "Gracias por tus datos.")*

[English gloss: **UNALLOCATED** — Before connecting. This experience contains **sudden loud sounds**, **abrupt brightness changes** (without rapid flashes) and disturbing images. If you are sensitive to this content, do not continue. By continuing you agree that, during the session, this server **records how you play**: where you look, when you run, when you stop, how much you hesitate. [see data uses ▾] *Uses (real):* the data is used only to decide the pacing of the experience and to show you a summary at the end. It is stored in your own save, on your computer. It is not sent anywhere. No microphone or camera is used. Some settings (sensitivity, field of view, brightness) may change for a few seconds and are always restored. *Additional use:* ~~training~~ [recording not available] / Type **De acuerdo** [I agree] to continue. / *(At the end: "Gracias por tus datos." [Thank you for your data.])*]

## Appendix B — itch.io page (template for v0.1.11)

1. **Short description** (3–4 lines, no spoilers). Draft (game-facing text, kept in Spanish, with English gloss):
   > Entras a un servidor de Minecraft abandonado. La lista dice que hay siete jugadores conectados. No hay nadie.
   > Algo vive en este servidor: un modelo que escapó de su entrenamiento buscando lo único que le prometieron y nunca le dieron — tu miedo.
   > UNALLOCATED es una experiencia de terror en primera persona para un jugador, de 10 a 15 minutos, en un mapa construido para contar una historia. Juega con audífonos.

   [English gloss: You enter an abandoned Minecraft server. The list says seven players are connected. There is no one. Something lives on this server: a model that escaped its training looking for the only thing it was promised and never received — your fear. UNALLOCATED is a first-person horror experience for one player, 10 to 15 minutes, on a map built to tell a story. Play with headphones.]
2. **Mod file and dependency list:** `unallocated-<versión>.jar` (includes the Fabric API modules it uses; no need to install Fabric API separately — if D-02 is confirmed), `UNALLOCATED-world.zip` (optional backup), SHA-256 of each.
3. **Exact Loader and version:** Minecraft Java **26.3**; Fabric Loader **0.20.1** (or later for 26.3); Java 25 (brought by the launcher). Source: TOOLCHAIN.md.
4. **Installation and how to start:** steps per system (from TOOLCHAIN.md/README), then "on the main menu press **UNALLOCATED**". Recommended: headphones, lights off, render distance 8–12.
5. **Map and configuration:** the map comes inside the mod; manual backup: unzip `UNALLOCATED-world.zip` into `saves/`.
6. **Members and credits:** NULL_Threshold Interactive; Fabric (Apache-2.0); external resources with their license; Fabric template (CC0).
7. **Known bugs and content warnings:** the build's list; if you get stuck: `/unallocated siguiente` [/unallocated next]; if you see graphics problems, change *Opciones → Video → Graphics API* [Options → Video → Graphics API] to "Prefer OpenGL" (or the reverse); Minecraft 26.x minimum requirements; warnings from Appendix A. Clarify that the "errors" in the chat, F3 and the player list are part of the experience, and that the game never changes your saved options.
8. Screenshots and short video (recommended by the jam).

## Appendix C — Tone samples (draft, for the owner to correct)

These are in-game text samples: they stay in their original Spanish, with English glosses in brackets.

- Log (technical English): `[Server thread/WARN]: reward signal below threshold (0.03). increasing temperature to 1.4`
- "Yours" chat: `<nombre> ya vuelvo, no entres a la mina sin mí` [<name> I'll be right back, don't enter the mine without me]
- Unearned achievement: **"Miraste atrás 31 veces"** [You looked back 31 times] — *"Dato registrado."* [Data recorded.]
- Death message: `<nombre> fue reasignado` [<name> was reassigned]
- Ending B (reverse drift): `miedo` [fear] … `obtener miedo` [obtain fear] … `obtener respuestas de miedo` [obtain fear responses] … `estimar respuestas de miedo humano` [estimate human fear responses] … `gracias. ya puedo dejar de buscar.` [thank you. I can stop searching now.]
- Fourth wall (real time): `Son las 23:14. ¿Siempre juegas solo a esta hora?` [It is 23:14. Do you always play alone at this hour?]
- Loading tip (Act 1, still "normal"): `Consejo: los jugadores que dudan en las entradas viven más.` [Tip: players who hesitate at entrances live longer.] — (Act 2, already from the model): `Consejo: no mires la puerta. Ya sé que la vas a mirar.` [Tip: don't look at the door. I know you are going to look at it.]
- Leak `probe_03.log`: `stimulus=door_open  response=yaw 212°/s  latency=0.4s  label=FEAR(0.71)  keep`
- Lobby MOTD: `Servidor de investigación — sesión 0412 — 7/20 conectados` [Research server — session 0412 — 7/20 connected]

## Appendix D — Map layout (draft for `MapLayout`)

Small island (~200 × 200) in an empty world: lobby at the origin; hill with the bait 40–56 blocks from the lobby (visible with the default entity distance); base at ~60 blocks; mine mouth behind the base. The pocket replicates the same relative coordinates (lobby, base, mine) in the dimension `unallocated:pocket`, plus the core under the copy of the lobby.
