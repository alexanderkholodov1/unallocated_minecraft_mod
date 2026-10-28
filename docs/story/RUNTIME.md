# Story runtime v1

Author JSON according to `docs/plan/milestones.md` §6. The shipped neutral chapter has a
420-second budget and uses the existing SERVER_CLOSED farewell. Invalid data logs each
`UNALLOCATED story invalid file#pointer: [rule] message` and leaves content-free core acts.
The Java loader rejects duplicate JSON keys and parses vanilla commands at SERVER_STARTED
with a permission-level-2 source. `post_effect` is unavailable in this version.

All runtime access and subscriptions belong on the server thread. Register extensions during
mod initialization, before the first server starts. Duplicate registrations fail.

```java
StoryActions.register("my_action", (json, context) -> {
    context.keys(json, "do at value", "value");
    String value = json.get("value").getAsString();
    return (runtime, player) -> {
        runtime.state().addFlag("story.flag." + value);
        return "ok";
    };
});
```

`ActionParser.parse(JsonObject, StoryValidator.Context)` validates and returns an
`Action.execute(StoryRuntime, ServerPlayer)` returning `ok`, `skipped`, or `refused`.
The context exposes `error(suffix, rule, message)`, `keys`, `position`, and `box`.
`StoryTriggers.register(String, TriggerParser)` follows the same pattern and returns a
`Trigger` with `test(ServerPlayer)`, `simulate(ServerPlayer)`, optional `reset()` and
`automatic()` (true for time/flag triggers that autoplay must wait for).
Package 4 registers `copy`, `corrupt`, and the `creature` action and trigger.

`StoryRuntime.get(server)` exposes `fire(String)` (guards/repeat limits still apply),
`currentChapter()` (JSON or null), `state()`, `resolvePosition(JsonElement)` and
`resolveBox(JsonElement)`. Positions retain dimension, block coordinates and camera direction;
`center()` adds the half-block X/Z teleport offset. Boxes include both final block corners.
State flags/counters owned by the engine use `story.`; authored flags use `story.flag.<id>`,
counters use `story.counter.<id>`. Pending queues survive save/reload without changing schema 1.

Subscribe through `runtime.events().subscribe(Event.class, listener)`; the returned subscription
is closeable. Events are `StoryBeatFired(String beatId, String chapter)`,
`StoryCapture(String name)`, and `StoryEnding(String id)`. Capture listeners save
`story-<name>.png` in the CI helper. `StoryBuild.run(server)` authors the story's build section,
releasing only forceloads it acquired, even on failure.

Commands (gamemaster/console): `/unallocated story status`, `check`, `build`, `fire <beat>`.
Build explicitly changes world blocks; check is read-only and reloads the files for validation.

CI alone sets `unallocated.story.dir`, `unallocated.story.autoplay=true` and optionally
`unallocated.story.autoplay.run` (default 0). `StoryRuntime.setAutoplayRun(int)` selects the next
world's run and requires the autoplay JVM property. All story delays/budgets use the saved
active-tick clock and scale only under autoplay. Escape is synchronous, as the core API requires:
its actions execute in at/file order before the core advances. A story ending uses the core's
forward jump to EPILOGUE and the existing tagged save/disconnect boundary.

Text travels through existing `story.*` EffectPayload IDs. Language/style and server placeholder
values are serialized; player name, local clock, save folder and contract agreement are resolved
only on the client. Blackout and ending splash are session memory. Interface custom text uses the
normal toolkit refusal/cooldown policy and never mutates options or the receipt.


## World actions (package 0.2.4, Part A)

`unallocated:pocket` is a flat-air datapack dimension using `unallocated:pocket` biome.
Its neutral grey sky/fog, silent music, empty natural spawn lists and absent precipitation are
placeholder data in `worldgen/biome/pocket.json`. Its dimension type has fixed time and no timelines;
its flat generator disables lakes, features and structures. The mod must be installed when the
world is authored. CI checks a fresh world, reopens that save, and checks the shipped authored
world through the title launcher. Those checks still need CI execution during the billing outage.

- `{"do":"copy","from":{"section":"base"},"to":{"pos":[100,60,100],"dimension":"pocket"}}`
  copies the inclusive source box to the destination minimum block corner. Named zones also work.
  Each synchronous batch stays in a source chunk column and within the current vanilla
  `MAX_BLOCK_MODIFICATIONS` rule. Vanilla cross-dimension `clone ... replace force` preserves block
  entities and scheduled ticks. Overlapping same-dimension copies use ordered single-row batches.
  Only force tickets acquired by the operation are released, including on failure. Copy does not
  provide a whole-operation rollback if a later batch fails.
- `{"do":"corrupt","area":"world.pocket_base","seed":17,"over_seconds":16,
  "rules":[{"from":["minecraft:oak_planks"],"to":["minecraft:cracked_stone_bricks"],"ratio":0.4}]}`
  visits blocks in Y/Z/X order. The first rule matching each original block chooses a replacement
  using a seeded position/rule hash; replacements never cascade through later rules. The ratio is
  a probability per matching block. Destinations use default block states. An empty rule list,
  source list or destination list makes no replacements. The optional seed defaults to zero.
  Zero seconds runs synchronously; positive durations spread visits evenly over active story ticks
  with a maximum of 2048 positions per tick (shorter durations stretch to this minimum). JSON jobs,
  resolved boxes, seeds and starting ticks are packed as UTF-8 words in `story.world.job.*` counters;
  only short job IDs are flags. Cursors and unique job sequence numbers are `story.world.*` counters. Jobs resume without reseeding after reload and
  pause while the story clock is stopped. Concurrent jobs execute in creation order. Never edit
  these internal keys from content. Block resource IDs are checked before any mutation.

Gamemaster/console commands (absolute integer coordinates):
`/unallocated story copy <zone-or-section> <x> <y> <z> <overworld|pocket>` and
`/unallocated story corrupt <zone-or-section> <seed>`. The corruption command applies an instant
25% stone/cobblestone to cracked-stone-bricks preset. `build.run` can use both commands. Content
controls real corruption through the action's rules. The world fixture covers the world and creature primitives, including reload and both captures.


## Creature actions and trigger (package 0.2.4, Part B)

`{"do":"creature","op":"spawn","id":"figure","body":{"mannequin":{"profile":"player",
"scale":1}},"where":"world.pocket","facing":"player"}` creates a transient body. `where` accepts
any position; `facing` is `player` or a yaw (default: anchor yaw). Profile may also be a Minecraft
profile name. `texture` accepts a shipped PNG such as `unallocated:skins/figure.png` or
`unallocated:textures/skins/figure.png`; missing assets are rejected at load. The reference 26.3
JAR's `ResolvableProfile` codec includes `PlayerSkin.Patch` with a `texture` ResourceTexture, so
bundled overrides are supported without changing section 6.6. No new skin is shipped by this package.
Alternatively use `{"summon":"minecraft:zombie{NoAI:1b}"}`; vanilla dispatcher validation checks
summonable entity IDs and SNBT at load on the server. Bodies and passengers receive the runtime tag
`unallocated.story_creature`. Native entity ticking is suspended for these bodies, so even summoned
projectiles obey hold; the runtime supplies all movement. Mobs also have AI disabled. Bodies are
silent, have no gravity and are invulnerable.

Operations after spawn use the same `id`:

- `stalk`: `step` defaults to 1 block per second, `min_distance` to 3 blocks. Moves only while unseen,
  using the bait's conservative plain/scoped view angles and visual line-of-sight rays to head/chest.
- `chase`: `speed` defaults to 3 blocks per second, `catch_distance` to 1 block. Ground snaps within
  one block vertically, and the entire body sweeps the route in at most 0.2-block increments. It
  stops at walls, missing ground and unloaded chunks. There is no pathfinding or door opening.
- `hold` stops movement; `face_player` holds position and follows the participant with its head;
  `despawn` removes the body and saved description. Operations on absent IDs return `skipped`.

Movement parameters accept 0..128, scale accepts greater than zero up to 16. Creature descriptions,
resolved dimension/position, body, operation and parameters are packed in `story.creature.*` counters,
with only short creature IDs stored as flags. Internal JSON never enters the 256-character knowledge wire fields.
The required creature mixins exclude tagged bodies from entity saves and suspend their native ticks.
On resume the runtime restores them from those records using the current participant's profile.
No bodies remain running while the session is inactive or empty.

`{"type":"creature","creature":"figure","event":"near","distance":3}` tests proximity;
`seen` requires view plus line of sight for `seconds` (default 0.5); `caught` requires chase mode
and the configured catch distance. All three optionally accept consecutive `seconds`. Autoplay
moves an existing creature to satisfy the condition; caught simulation requires chase already
active. A blocked seen simulation places it closer in front of the participant. The placeholder
world fixture stalks, holds/faces for `story-world-creature.png`, chases until caught, despawns,
returns to the Overworld and ends. CI still needs to execute this proof during the billing outage.


The package mixin config also marks only `unallocated:pocket` as stable when vanilla checks dimension
settings. Vanilla otherwise marks all custom dimensions experimental; full Fabric API supplies its
own mod-dimension stability check, but the dependency-free judge installation must behave the same.
Other custom dimensions retain vanilla stability checks. Failed launcher/fresh-world/resume waits
capture `unallocated-load-failed-*.png` and log the screen class/title before the test exits.
