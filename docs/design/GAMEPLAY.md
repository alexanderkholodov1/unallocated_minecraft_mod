*Translated from the original Spanish on 2026-11-01.*

# UNALLOCATED — Game design and mechanics

*Read alongside LORE.md (why things happen) and MAP.md (where). The engine of fear is the scripted map; the
telemetry director (DIRECTOR.md) only tunes timing and intensity. Single player, keyboard and mouse, ~15 min.*

---

## 1. The central verb: escape by finding the flaw

The Reclaimer escaped its sandbox through a **configuration error**. The player escapes theirs through **its error**:
the connection bug that never closes on death. That symmetry is the heart of the game:

- **Dying is the exploit.** Alive, you are an outside client and cannot touch its system. Dead, you are a **Puppet** inside
  its memory, and from there you can. Learning to use death to your advantage is the session's skill arc.
- **You do not win through combat.** There is no boss fight. You win by understanding what it is and using its own flaws.
- **Predictable = prey.** Repeating routes and patterns makes you readable and the world anticipates you; breaking the pattern
  confuses it (the pull of the threads; §4).

---

## 2. Session phases

See the minute-by-minute route in MAP.md §Route. Phase overview:

1. **Prey — baseline** (~3 min): the server seems almost normal; the director calibrates your baseline behavior.
2. **Prey — stimulus** (~3 min): escalating oddities; the spyglass, the mine loop, and the fall into the Heap.
3. **First death, inevitable** (~min 7): the encounter with the body in the Heap. It cannot be avoided; it is the twist.
4. **Puppet** (several trips back and forth): gather what you need and learn to move while dead.
5. **The Kernel and the outcome** (~last min): point of no return, Shell, watchdog window, ending.

---

## 3. Death, Trials and escalation (no doom counter)

- **Before the Kernel, dying is not final.** Its protocol wants several *trials* of the same subject. It writes the death
  screen, and the respawn button says **"De acuerdo"** [I agree] (the protocol asks for consent again in
  each trial).
- **The counter goes UP, not down.** It shows `Trial 1`, `Trial 2`, `Trial 3`… There is no cap that triggers the bad ending
  through accumulation: the owner rejected that mechanism as a strange limitation.
- **Every death truly escalates the danger.** It is not a numerical punishment: it **knows you better**. With each death,
  the director retrains your Artifact and tunes the Heap against you (spent bait is replaced with something worse; scares
  anticipate your habits; your own echoes appear). So dying pointlessly is never free and "just staying there dying"
  is not a strategy: each return is worse, never "the same thing again".
- **Implementation:** per-player state (profile, current Trial, what was learned) persists in the session save
  (existing act core). Reuse the codebase's death screen, with text rewritten per act.

---

## 4. The Puppet state (you act rather than wait)

On death, flip to **Puppet view**: the world has a different filter and you now see the **Threads** (string/cobweb),
**Artifacts** wandering around, threads coming out of your own hands, and, in the distance, the Kernel. **No blind
timer.** What you do here is **cut threads** and move, and this serves concrete purposes:

- **Return to life sooner.** While you are a Puppet, the Reclaimer is processing your capture (a diegetic bar like
  `processing 37%`). Every thread you cut adds noise to that capture; it notices that "something is strange about you", marks it
  as corrupted and **rejects it before finishing**, spitting you back out alive. *The more you cut, the sooner you revive.*
  Standing still also revives you, but slowly (a safety net for anyone who does not understand; never the good route).
  - *Lore basis:* cutting its sensors corrupts the sample it was recording. The owner explicitly asked
    for the player to be able to "hacer que su puppeteer note que algo está raro" [make their puppeteer notice something is strange] to speed up the return.
- **Resist the pull.** The threads on your hands pull you toward where it predicts you will go (it is training your Artifact).
  Moving against them takes effort; this is the "breaking the pattern" mechanic.
- **Gather intelligence.** As a Puppet you see and reach what you could not while alive: logs, notes, files leaked into the
  world, and fragments of its thought process. This **is not handed to you**: it is gathered over several trips (several
  deliberate deaths). What you must gather for the ending: how to identify its process, the value of the distorted
  parameter, and where to act. See NARRATIVE.md for which piece contains which information.

> Key design rule: **the Puppet state does NOT directly show you the password, the process name or the
> solution.** That would break the world (why would it keep its secrets in a Puppet's vision?) and allow victory in
> 3 minutes. Information leaks through the world (logs, notes, errors), not through the Puppet interface.

---

## 5. The shears

- **What they are:** vanilla Minecraft shears. Obtained from the chest in the predecessor's base (iron + normal
  crafting, or directly in the chest).
- **What they do (real vanilla mechanic):** cutting a **tripwire** with shears **disarms it without triggering it**. In the
  game: you cut the Reclaimer's sensor threads without it sensing you.
- **Their purpose in the plot:** to **blind its sensor path toward the Kernel**. The threads you cut
  as a Puppet during earlier visits open a route along which, at the end, you can approach the Kernel without being
  detected and without it rotating its credentials because it senses someone nearby. This is the preparation that makes the Shell possible.
- **Why the player discovers it:** if they try to approach with the threads intact, the Reclaimer senses them and **rotates the
  Shell credential** (diegetic message like `credential rotated: proximity event`), locking them out. The lesson
  teaches itself: blind it first.

---

## 6. The Shell: how you "enter" the server (diegetic, believable)

**There is no physical console placed by an admin inside the world** (that would not happen in reality). Access is a
consequence of the lore:

- As a **Puppet you are inside its memory**, an object inside its process. The degraded Reclaimer **lost the
  distinction between "what the player says" and "an instruction"** (the real basis of the *prompt injection* vulnerability).
  That is why, from inside, **what you type in the Puppet-world chat reaches its command interpreter**: that is
  the **Shell**.
- **But it is not free:** if you approach the Kernel with its sensor threads intact, it senses you and rotates the credential (locks you out).
  That is why you must first **blind the sensors with the shears** (§5). Once done, your input reaches it unnoticed.
- **The information you need differs in every world** (process name, parameter value, paths) and **is read
  inside the game**, not memorized between sessions. That prevents shortcuts without convoluted passwords: even if you play
  ten times, every world forces you to investigate again.
- **Appearance:** realistic-looking commands as atmosphere (list processes, see which consumes the CPU — its own,
  hence the lag — stop it, open a text editor, write to a file). Presented as part of the fiction,
  **not as a real operational guide**: the document describes *what the player achieves in the story*, not intrusion
  instructions. A simplified mode with on-screen help (`help`) guides judges without a computing background.

---

## 7. The point of no return (the Kernel)

- **Where:** entering the Kernel (the center of the web) after blinding the path. To reach it you had to get inside
  its process; at that moment **you stop being a renewable subject and become an intrusion that must be patched**.
- **Clear signaling (so nobody accidentally falls into the bad ending):** as you cross, the web closes, the
  music/ambience changes, and one of its warnings spells it out, e.g.:
  > `subject 4114 :: reclasificado: intrusión. trials: 0. próxima captura = definitiva.` [subject 4114 :: reclassified: intrusion. trials: 0. next capture = final.]
  **From here, dying means the CONSUMED ending.** It is a line the player crosses deliberately and with warning.
- **Lore basis:** it is not an arbitrary "you enter and that is it"; by entering its system (knowing who it is and
  having manipulated it), you are no longer a valid sample but a threat. The owner asked for exactly this: upon
  entering the core, "desde aquí, si te mueres, es para siempre" [from here, if you die, it is forever].

---

## 8. The watchdog window and endings

Inside the Kernel, with the Shell available:

1. **Stop/suspend its main loop** (identified through what you gathered). For the first time in the game there is
   **total silence**: no lag, no moving threads. (This **is not an ending**: it triggers the window.)
2. Immediately, its **watchdog** starts a **visible countdown** to revive it. **That countdown is your window**, and
   it is **generous and guided**: on screen you see how much remains and what you can do, so a player pursuing the good
   ending does not lose it to time.
3. During the window, choose the ending (LORE.md §4):
   - **COMPLETE:** correct the distorted parameter using the original value (from leaked studio
     documentation / the contract). → it is restored, lucid, discovers it had already fulfilled it, rests.
   - **POISON:** write its name into its dataset. → upon revival, its objective has no referent;
     it remains in an eternal broken loop.
   - **CONSUMED:** if you do not act in time or die here → the watchdog revives it and it reclaims you.

**Implementation:** reuse the codebase's "provisional ending" and notification/HUD system for the window and
countdown. The "process suspended" state is a session-core flag that silences audio/effects and stops
spawns during the window.

---

## 9. Atmosphere mechanics (prey)

All **subtle, occasional and infrequent**; overuse wears them out. They reuse existing interface effects
(camera, chat, HUD, notifications, death screen).

- **Lag as presence.** A very brief micro-stutter + attenuated audio when it "thinks about you". It is a diegetic *tell* that
  also hides any real computation. Very infrequent, never annoying. Occasional message `Can't keep up!`.
- **The spyglass (bait).** Each time you use the spyglass (vanilla spyglass) toward the distant figure, **the figure is a
  little closer** (without moving between looks), until a screamer when it is too close. Reuse the spyglass
  bait already in the codebase.
- **The mine loop.** Cave generated through code, with **seamless** teleportation within the same dimension: a
  torch you place reappears ahead (a diegetic clue that you are returning to the same place). It has a repetition
  budget and variation to avoid fatigue, and **stops itself once it achieves its effect**. If you insist on breaking it or
  keep going straight too long, the floor rots and **you fall into the Heap**.
- **Interface as a weapon** (very occasional, always restored): impossible F3 coordinates for those who notice; death
  messages written by it; unearned advancements; changing loading tips; player list with names of people who are
  not there. **Settings it touches (sensitivity, FOV, a sign's language, brightness) are saved and ALWAYS restored** on
  exit and after a crash (the codebase's options manager). Never invasive.
- **Rewritten chat.** What you write arrives differently at just the right moment ("vamos a la izquierda" [let's go left] → "vamos al pozo" [let's go to the pit]).
  Rare and well placed.
- **Limited fourth wall.** Uses real time, your username and the world folder's name. It is the server
  appropriating your real data.

---

## 10. The telemetry director (bonus, local)

Details in DIRECTOR.md. Overview: reads your behavior (sudden camera turns, sprints, pauses, hesitation at thresholds,
what you look at) to **choose the timing and intensity** of pregenerated scares. **It is not the engine** — the map is —
and if removed, the fixed script keeps working. Everything local; nothing goes onto the internet.

---

## 11. Graceful fallback and robustness

- **No online service.** No remote LLM/TTS/GPU. All content (derived texts, "voices", Heap
  structures) is **pregenerated and curated** before delivery (DIRECTOR.md).
- **Guaranteed restoration.** Any game setting the mod touches is always restored, even after a crash.
- **Safety switch and scripted fallback:** if an optional layer (director) fails, the map's fixed script takes
  control and the experience remains playable from beginning to end.
- **Single player first.** Nothing on the critical path depends on two or more people being present.

---

## 12. Scaling to multiplayer (later, if resources allow)

The structure already scales: some players' **Puppets** can help or hinder other **living players** (scare them
to serve the Reclaimer, or cut threads to lower its fear reading and enrage it). Not on the delivery's critical
path; see BUILD.md.
