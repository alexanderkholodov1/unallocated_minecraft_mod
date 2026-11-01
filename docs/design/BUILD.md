*Translated from the original Spanish on 2026-11-01.*

# UNALLOCATED — Build order (for the Beacon captain)

*By milestones, not dates (the owner's preference): milestones can be advanced all at once or one by one depending on resources.
Each milestone has a clear acceptance criterion, to make a production disaster impossible. Minecraft Java 26.3,
Fabric, single player first. The engine of fear is the scripted map; the director is a bonus.*

---

## 1. Existing code components (reuse or discard, no obligation)

From the codebase already available:
- **"Contract" screen** before the menu → use for consent + warnings + subject number + spec in
  fine print (LORE.md §4.1, NARRATIVE.md §6).
- **Session core with acts and saved state** → use for phases, current Trial, per-player profile, flags
  (process suspended, point of no return).
- **Map built through code (lobby, hill, path, base, mine)** → Overworld skeleton (MAP.md §3).
- **Spyglass bait with a scare** → the hill [3] (GAMEPLAY.md §9).
- **Interface effects (camera, chat, HUD, notifications, death screen)** → interface as a weapon, rewritten chat,
  death screen written by it, watchdog window.
- **Provisional ending** → foundation for the watchdog window and the three endings.
- **Audio system** → lag/attenuation, occasional screamers, ambience.

If the design needs something that does not exist, ask for it.

---

## 2. Milestones (with acceptance criteria)

> Dependencies in brackets. The recommended order is H0→H6; H7 and H8 are optional/later.

### H0 — Foundations
The mod starts in 26.3; contract → menu → map loading; session core with phases; options manager that **always saves and
restores** (even after a crash); basic local telemetry; Overworld skeleton built through code.
**Acceptance:** green build; a session enters, loads the lobby and exits without crashing; any changed setting is restored.

### H1 — Playable prey (baseline + stimulus) [H0]
Lobby/hub/hill/path/base/mine; spyglass bait; lag as presence; interface as a weapon (subtle); rewritten chat;
lost time; seamless mine loop; rotten floor → transition to the Heap.
**Acceptance:** a player goes from the contract to the fall into the Heap in ~7 min, without crashing, understanding where to go without
being told.

### H2 — The Heap and the first death [H1]
Heap dimension (corrupted copy through a datapack) with stages of decay; inevitable first death; flip to
Puppet view; the network of Threads and the Artifacts visible as a Puppet.
**Acceptance:** fall into the Heap, die, see yourself as a Puppet with the network, and respawn ("De acuerdo" [I agree]) reliably.

### H3 — Puppet mechanics [H2]
Cut threads (shears) → leaks + faster return (corrupted sample rejection); threads pulling; rising Trials
and Heap escalation between deaths; blinding sensors on the way to the Kernel.
**Acceptance:** the player can speed up their return by cutting threads; two or three deliberate deaths open the way to the
Kernel; the Heap feels more hostile with each death.

### H4 — Story delivery [H1, H3]
Predecessor's trail (forum/MOTD, signs, book, postmortem); behavioral echoes; logs/errors through several channels;
language incorporating the player's nickname and chat. Map each lore fact to its piece (NARRATIVE.md §6).
**Acceptance:** a new player, without explanations, can reconstruct the story from beginning to end upon finishing
(check with 2–3 testers and a short questionnaire).

### H5 — Kernel, Shell and endings [H2, H3]
Signposted point of no return; diegetic Shell (Puppet input → interpreter) with per-world secrets; suspend the
loop; guided watchdog window; the three endings (Complete / Poison / Consumed) with their texts (LORE.md §4).
**Acceptance:** all three endings are reachable and readable; the bad ending (Consumed) only happens after crossing the
warned point of no return; Complete and Poison close with their texts.

### H6 — Director and polish [H1–H5]
Telemetry director tuning timing/intensity (with fallback to fixed triggers); pacing balance; content
warnings; packaging; itch.io page; screenshots and short video.
**Acceptance:** 0 crashes across several consecutive sessions; successful clean installation from the page; pacing that does not wear players out.

### H7 — (Optional) Voice [H4]
Capture "De acuerdo" [I agree] and play it back degraded, only if resources allow. No voice-chat dependency. **Not on the critical path.**

### H8 — (Later) Multiplayer
Some players' Puppets helping/hindering other living players (GAMEPLAY.md §12). After the single-player delivery.

---

## 3. What to cut first if time gets tight (technical, NEVER the story)

The owner was explicit: the story and endings are not cut ("hacemos bien o no hacemos" [we do it properly or we do not do it]). If time is short,
cut **technical scope**, in this order:
1. **H8 multiplayer** (already later).
2. **H7 voice** (optional).
3. **H6 director** → fall back to **fixed triggers** (the scripted map is already the engine; the director is a bonus).
4. **Heap fidelity** → fewer stages of decay / smaller copy radius, without losing the first death or the Kernel.
5. **Interface as a weapon** → keep the strongest hits (death screen, F3, rewritten chat) and drop the
   smaller ones.

What is **not** cut: contract, playable prey (H1), first death and Puppet (H2–H3), story delivery (H4),
Kernel and the three endings (H5).

---

## 4. Delivery constraints (itch.io)

Delivery target: **2026-10-28 23:59 (UTC−5)** (confirm current status with the owner). The page must include:
mod file and dependencies; exact loader and version (Fabric, Minecraft 26.3, Java 25); installation and
startup instructions; map/config if needed; short description; team and external resource credits; known
issues and **content warnings** (flashing lights, loud sounds). Recommended: screenshots and short video.

Dependency criterion: **every mandatory dependency makes installation harder**; include only what is truly
necessary and easy to install. Verify in 26.3: Fabric Loader/API, and optimization mods if truly
needed (ModernFix, Lithium, C2ME, Noisium, Starlight). Simple Voice Chat only if H7 is implemented.

---

## 5. Minimum tests

- **E0 Smoke:** loads, does not crash, settings are restored.
- **E1 Complete single player:** end-to-end session without crashing; a new tester, without explanations, reaches an ending and
  understands the story (short questionnaire: scariest moment, where they got confused, whether they understood what to do and what
  the Reclaimer was, whether the audio bothered them, whether they would play again).
- **E5 Judges:** clean installation from the itch page by a stranger, 10–15 min timed.
