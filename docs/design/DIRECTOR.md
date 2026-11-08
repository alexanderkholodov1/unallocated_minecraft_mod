*Translated from the original Spanish on 2026-11-01.*

# UNALLOCATED — Telemetry director (bonus layer, 100% local)

*The engine of fear is the scripted map (GAMEPLAY.md). This is an extra layer that makes the experience feel
alive and "learn from you". It is dispensable: if removed or broken, the fixed script still works. **Nothing goes onto the internet.***

---

## 1. What it is and what it is not

- **It is:** a local director that reads your behavior and **chooses the timing and intensity** of pregenerated
  scares, controlling the tension curve (calm → pressure → peak → false relief). It also feeds the illusion
  of "learning from you".
- **It is not:** an online LLM, a remote service, or RL that converges in a 15 min session. "Seeming to
  learn is worth more than actually learning" (Alien: Isolation). Like the Left 4 Dead Director: it controls **pacing**,
  it does not "beat" the player.

---

## 2. Signals (client telemetry, local)

Per player, building a profile for the session:
- Angular camera velocity and spikes (sudden turns = fright).
- Sprints, repeated jumps, crouching, time standing still, backtracking.
- Hesitation at thresholds (how long you take to enter a cave or place).
- What you look at (raycast), distance from baits.
- Hotbar changes, damage received.
- Chat text (for the language it learns; NARRATIVE.md §5).

**Calibration:** the first quiet minutes (baseline prey phase) establish your **personal baseline**. The "fear
signal" f(t) is, in the plot, the Reclaimer's reward signal.

---

## 3. Scare selection

- **Director with manual priorities** (design priors) over a set of ~20 scare primitives. A simple contextual
  bandit (e.g. Thompson sampling) **optionally** on top, with constraints: cooldowns, no pattern
  repetition, intensity caps, per-phase budget.
- **Forced novelty:** if the player suppresses themselves (stands still, does not react), the director explores new stimuli
  (auditory, visual, interface) instead of persisting.
- **Main selection by novelty and coherence; estimated fear breaks ties.** The signal is noisy and there is little data,
  so it is not trusted as truth, only as a bias.

---

## 4. Actually "learning from you" (what is real)

Three things learn from you within the session, and these should be prioritized because they are believable and cheap:
1. **Your Artifact imitates your path.** Your movements (position, yaw/pitch, actions) are recorded and your echo repeats them,
   degrading. It is real, small-scale imitation; accumulated error is an aesthetic advantage (collapse/repetition).
2. **Your language enters its texts.** Your nickname and chat words are incorporated into its logs/messages (NARRATIVE.md
   §5).
3. **The Heap is tuned against you between deaths.** What frightened you (according to telemetry) is intensified or returns
   worse; what you ignored is replaced (GAMEPLAY.md §3).

---

## 5. Pregenerated and curated content

Everything "AI-generated" is **pregenerated offline, curated by hand and packaged** in the mod. There is no live generation:
- Derived texts (drift g0→g5, death messages, logs, loading tips, mutating signs).
- "Voices"/audio, if used (optional; no voice-chat dependency). If there is a microphone, at most capture the "De
  acuerdo" [I agree] from the contract to play it back degraded; otherwise, everything through text. **Not on the critical path.**
- Structures and Heap corruption passes.

Principle: **the model proposes (offline), a filter decides**. Many generations are run before delivery and
only the best are kept. It is still "generated", just filtered.

---

## 6. Privacy and security

- **Everything local.** Telemetry, the profile and any voice/chat capture **never leave the player's machine**. There is no
  inference server or network.
- Contract consent covers its use (microphone if enabled, chat, behavior, temporary settings), with a
  clear explanation. Minimal retention: only what is necessary for the game's purpose, deleted on closing.
- What the player types is **data**, never an instruction for any model (there is no live model to inject into); the game's "Shell"
  is scripted fiction (GAMEPLAY.md §6).

---

## 7. Graceful fallback

If the director layer fails or is disabled: the map fires its scares through **fixed position/time triggers**, and
the experience remains complete from beginning to end. Author debug toggle: force scares, skip acts,
view f(t).
