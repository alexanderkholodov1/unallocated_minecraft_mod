# Map authoring — v0.1.8

`org.unallocated.map.MapLayout` owns world coordinates, named section boxes, trigger volumes,
landmarks and checkpoint camera directions. `MapBuilder.build(ServerLevel, String)` runs on the
server thread and accepts `lobby`, `hill`, `path`, `base`, `mine`, or `all`. There is no startup or
player-join generation hook. The launcher/template package explicitly invokes this API.

The five section implementations use local offsets from their layout origins. `MapWriter` rejects
placements outside the section's box. Section boxes do not overlap. Full authoring clears the
island envelope, creates its irregular ground and sparse trees, then authors the five sections in
layout order. Single-section authoring clears and restores only that box. Connected block shapes
are resolved after placement. Container removal suppresses inventory drops.

## Authoring commands and world

- `/unallocated build <section|all>` — gamemaster/console only; rebuilds blocks and sets D-17 rules.
- `/unallocated mapcheck` — validates pristine authored blocks, sign text, supplies, checkpoints and rules.
- `/unallocated mapcheck rebuild` — destructive CI authoring check on the custom-biome world: compares
  the whole island's block states, sign lines/wax and container item types/counts after damaging and
  rebuilding each section, then rebuilding `all`; checks that no inventory items were dropped.

The checks expect a pristine build; consuming the base's supplies makes `mapcheck` fail until a
rebuild. Authoring resets manual edits inside the selected box. Owner polishing should replace the
eventual saved template and disable code regeneration, preserving layout coordinates (D-04).

For a fresh dedicated authoring world, install the player JAR and use these `server.properties`:

```properties
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:air","height":1}],"biome":"unallocated:lobby","structure_overrides":[]}
generate-structures=false
```

Run `unallocated build all`, then stop cleanly before exporting the save. This package's server
smoke save also exercises core session persistence, so it is a test fixture, not a release template.
The launcher/export package should author a fresh save without the smoke's `act LOBBY` command.

Rules: Adventure (including players already online), normal difficulty, `spawn_mobs false`,
`advance_time false`, `advance_weather false`, `keep_inventory true`, `respawn_radius 0`, clear
weather and time 11000. World spawn is `lobby.spawn` with its camera direction. These are authored
world settings; ordinary worlds are untouched unless an administrator explicitly builds there.
The custom biome has no natural mob spawns or generated features, a muted blue sky and no music.
On dedicated servers the build command also persists Adventure/normal through the dedicated
server's settings API (`server.properties`), since dedicated startup overrides those world-data
values from its properties. Integrated worlds retain the saved world-data settings. The verifier
reports every differing rule, mode, difficulty or complete spawn value (dimension/position/rotation).

The disposable smoke servers additionally set `pause-when-empty-seconds=-1`, so no-player checks
continue ticking. Map commands catch exceptions, log their full causal stack with
`UNALLOCATED map command failed operation=...`, report failure to the source and return zero.
The smoke watchdog treats that marker as an immediate failure. Build logs identify the active
phase and section; placement exceptions include the requested block state and world position.

## Coordinate contract

Ground is Y=63; standing feet are normally Y=64. The roughly 180×168 island is inside
`MapLayout.ISLAND` (including its vertical authoring envelope). Positive X leads from the lobby
to the base and mine; negative Z leads up the hill. Checkpoints use block coordinates; teleport to
`(x + 0.5, y, z + 0.5)` with absolute yaw/pitch. All are in the Overworld for this milestone.

| Checkpoint | Feet X/Y/Z | Purpose / screenshot |
| --- | --- | --- |
| `lobby.spawn` | 0 / 64 / 8 | Spawn plaza; `map-lobby-spawn.png` |
| `lobby.board` | 0 / 64 / -3 | Legible rules, MOTD and task; `map-lobby-board.png` |
| `hill.bait_path` | 34 / 72 / -24 | Plateau beside reserved bait foothold; `map-hill-bait_path.png` |
| `hill.view` | 24 / 64 / -12 | Hill silhouette from its approach; `map-hill-view.png` |
| `path.midpoint` | 34 / 64 / 3 | Track bending toward the base; `map-path-midpoint.png` |
| `base.entrance` | 53 / 64 / 12 | Outside the front door; `map-base-entrance.png` |
| `base.theft_zone` | 61 / 64 / 12 | Workshop/interior; `map-base-theft_zone.png` |
| `mine.entrance` | 80 / 64 / 20 | Timber mouth; `map-mine-entrance.png` |
| `mine.threshold` | 89 / 64 / 20 | Short adit; `map-mine-threshold.png` |

Triggers are separate from camera checkpoints. `lobby.exit` crosses the eastern plaza exit;
`hill.bait_path` covers the plateau; `base.theft_zone` covers the usable interior;
`mine.entrance` covers the mouth. `BlockBox.contains` accepts fractional player coordinates and
includes the entire final block (`max + 1` is the exclusive geometric boundary). Modules should
use those boxes rather than duplicate coordinates. This package defines volumes; act modules own
trigger entry tracking and publishing `ServerEvents.TriggerEntered`.

Named landmarks include the lobby's rules/MOTD/task/route, `hill.bait` (42/72/-24, empty foothold
48 blocks horizontally from spawn), base chest/bed/front and back doors, the path's route/campfire
and `path.torch.0` through `.4`, and mine route/lintel/end. The path torches are placed initially;
the Act 1 module can use these landmarks for its guidance effect. No bait entity is authored.

The mine is a safe, solid-floor, timber-framed tunnel ending at `mine.end` (100/64/20).
Later loop/pocket content owns replacing that seam and/or the floor; this milestone creates no
loop, transfer trigger, pocket dimension, ritual, or final encounter. Pocket copies should retain
relative section coordinates; `loop.*`, `pocket.*` and `core.*` keys belong to their future packages.

## Proof and review

JUnit `MapLayoutTest` checks checkpoint ownership, section separation, landmark/scenery ownership,
fractional box boundaries, unknown IDs and immutable public coordinate maps.

The real dedicated CI smoke authors `all` on a fresh flat-air custom-biome world, checks every
section and checkpoint, runs regeneration checks, then exercises the existing core commands.
It stops and starts another JVM on the same save and validates the map again, including saved
sign text, supplies and spawn/rules. The optional full Fabric API coexistence run also builds it.

The existing client title/package test now continues into a real integrated world created with
the same flat-air custom biome. Its test-only server startup callback runs the build command before
the first player joins (avoiding a fall into the unbuilt void). It checks the biome and authored map,
teleports to **every checkpoint**, waits for the client position and rendered chunks, and saves
the nine views above. The Python watchdog requires the final tour marker and every PNG; a title
image alone cannot pass. Screenshots may have Fabric's numerical filename prefix. Download the
workflow's `smoke-diagnostics-<sha>` artifact and look under `screenshots/`.

Host validation: Python watchdog/packaging tests pass (24); `git diff --check` passes. The coordinator's
lightweight `mc-ref/javacheck.sh` type-checks all 33 Java source files against the pinned reference
JARs with JDK 25: `javac exit 0 (OK)`. Colored blocks use 26.3's `ColorCollection` accessors.
Gradle and the game were not run on the worker host. CI run 40305319183 built and verified every
section, passed all five section regeneration checks, and saved cleanly. Reload failed its settings
check: dedicated startup restores game mode/difficulty from properties that the original world-data
setters did not update. Authoring now persists those properties through the dedicated setters;
smoke checks the written values before restarting. Reload/runtime and screenshot results for this
fix still need the coordinator's next CI run.

Human/coordinator review remains essential: signs readable at normal settings; believable old
small-server construction; hill/spyglass sightline with default entity distance; clear torch route;
doors and hill steps traversable in Adventure; exterior and interior lighting; tone of the minor
sign error and the impossible player count. The map alone cannot validate fear, act pacing, the
launcher, reconnect/respawn callbacks or the complete 10–15 minute experience.

Owner questions: none blocking implementation. Visual approval and any manual world polish remain
with the owner/coordinator after reviewing the CI views.

## Integrated tour chunk-wait correction (v0.1.0.20)

Rehearsal 40358823036 rendered the lobby completely but timed out waiting for four chunks
that vanilla never streams. Minecraft 26.3 `ChunkTrackingView.contains(x,z)` uses a rounded
footprint with an outer buffer; Fabric client gametest 6.0.11's `areChunksLoaded` instead
requires every chunk in a square of the client render radius. At radius 8, exactly the four
square corners fail vanilla's tracking predicate (6² + 6² is not less than 8²). At the Fabric
harness default radius 5, all square corners fit. The map tour itself sets radius 8; none of
the contract, core, interface, bait or audio modules changes that radius or tracking geometry.

The tour now snapshots each real server player's tracking view, asserts its radius is still
8 and its center matches the checkpoint, and requires **all 329 streamed chunks** to reach
the client. This includes the outer buffer beyond the old 289-position square. It then
requires Fabric's unchanged light-queue and render-completion predicate before each of the
nine screenshots. No client/server view distance, timeout, product behavior or package
feature is reduced. Failure diagnostics list missing chunks from that actual footprint.
JUnit checks the pinned game's radius-8 and default-radius-5 footprints directly.

The cause was checked by Python-only disassembly of the official 26.3 client JAR
(SHA-1 `e877b6a07acd633fb3bb475002175cec036e7b87`) and the published Fabric gametest
6.0.11 source JAR. No local Java, Gradle or game process was used.
