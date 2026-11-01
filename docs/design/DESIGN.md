*Translated from the original Spanish on 2026-11-01.*

# UNALLOCATED — Design (index)

*NULL_Threshold Interactive. Narrative and game design approved by the owner (Alexander Kholodov).
Horror mod for Minecraft Java 26.3 (Fabric), ~15 min single-player experience.*

This design is split across several documents so each worker can take their part. Read them in this order:

1. **[LORE.md](LORE.md)** — the complete, canonical story (the chain of events from the real world to the server),
   nomenclature, entities, three ways to arrive, and the three endings with their texts. It is the **narrative source
   of truth**; if anything contradicts this, this wins.
2. **[GAMEPLAY.md](GAMEPLAY.md)** — mechanics and how they are implemented in the mod: prey, death and Trials, the point
   of no return, the Puppet state, shears, the diegetic Shell, the watchdog window and endings.
3. **[MAP.md](MAP.md)** — the map, places, the two "dimensions" (Overworld and the Heap) and the Kernel, with the
   player's minute-by-minute route.
4. **[NARRATIVE.md](NARRATIVE.md)** — how the COMPLETE story reaches the player, from beginning to end, through
   believable media (no chat exposition). Maps each lore fact to a piece of the world.
5. **[DIRECTOR.md](DIRECTOR.md)** — the "apparent AI" layer: local telemetry and pregenerated content. It is a
   bonus; the engine of fear is the scripted map. Nothing goes onto the internet.
6. **[BUILD.md](BUILD.md)** — build order by milestones, dependencies, which code components to reuse, and what to
   cut first (technical, never the story) if time gets tight.

**The owner's fixed principles:**
- The story must make sense in the real world: at no point should the player be able to say "I do not believe it".
- Single player is priority #1. Multiplayer later, if resources allow.
- The scripted map is the engine of fear; the telemetry director is an extra.
- No online service (LLM/TTS/GPU): everything local and pregenerated.
- Spanish for what the player must understand; Reclaimer leaks in technical English that gradually
  mixes with Spanish as it "learns" from you.
