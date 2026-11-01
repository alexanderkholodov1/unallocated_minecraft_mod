# ValleCraft Network v0.4 map implementation

Changes are left uncommitted. No Java, Gradle, Minecraft build, or playtest was run on this machine.

## Files changed

- `scripts/author_template.py`: normal terrain, fixed seed 4114, structures still disabled; other properties preserved. The flat generator settings and lobby biome override are removed.
- `scripts/map_anchors.json`: adds `hub.admin_house`.
- `src/main/java/org/unallocated/map/MapLayout.java`: footprint and orientation changes listed below.
- `src/main/java/org/unallocated/map/MapBuilder.java`: removes island-wide clearing, the artificial flat island slab, and planted scenery; prepares individual sections and clears the spawn-to-bait viewing corridor.
- `src/main/java/org/unallocated/map/MapWriter.java`: grades section footprints to surface y=63, with dirt over stone and clear building space. Normally uses four terrace rings; grading extends farther inside ISLAND where necessary to avoid cliffs against steep generated terrain. Nearest-section ownership prevents regeneration from erasing another section's structures. Terrain slope propagation converges by reducing height displacement toward y=63.
- `src/main/java/org/unallocated/map/LobbySection.java`: public network hub, block letters, gates, boards, leaderboard, parkour, and staff house.
- `src/main/java/org/unallocated/map/PathSection.java`: gravel/dirt-path survival road, three claimed houses, dry wheat farm, and damaged arena. The midpoint checkpoint has a full-height gravel foothold.
- `src/main/java/org/unallocated/map/BaseSection.java`: permanent openings in the fenced garden.
- `src/main/java/org/unallocated/map/MineSection.java`: replaces literal sign text with language keys.
- `src/main/java/org/unallocated/map/MapVerifier.java`: light checks for hub and survival structures/sign keys; removes the requirement for a lobby biome. Existing settings, checkpoint, container, and regeneration checks remain.
- `src/test/java/org/unallocated/map/MapLayoutTest.java`: assigns the `hub.*` landmark namespace to the existing lobby section.
- All eight files under `src/main/resources/assets/unallocated/lang/`: `en_us.json`, `es_es.json`, `es_ar.json`, `es_cl.json`, `es_ec.json`, `es_mx.json`, `es_uy.json`, `es_ve.json`. All seven Spanish files are identical.
- This summary file, `MAP_V04_SUMMARY.md`.

## Hub coordinates

All coordinates below are world block coordinates; surfaces are y=63 and walking space starts at y=64.

| Feature | Coordinates / layout |
| --- | --- |
| Plaza | x=-15..15, z=-15..15; stone bricks with quartz borders/inlays |
| Spawn | (0,64,8), yaw 0, facing south toward VALLECRAFT |
| SURVIVAL gate | Arch plane x=15, z=-3..3, y=63..68; open passage east through the unchanged lobby.exit trigger |
| MINIGAMES gate | Arch plane x=-15, z=-3..3, y=63..68 |
| PLOTS gate | Arch plane z=-15, x=-3..3, y=63..68 |
| CREATIVE gate | Arch plane z=15, x=-3..3, y=63..68 |
| Closed mode gates | Central wooden fence gate with fences beside it; OFFLINE sign; open surrounding grounds and no enclosed back room |
| Name | VALLECRAFT, quartz 3x5 glyphs, x=-26..12, y=70..74, z=24; reads from the north/spawn side. Stone supports at x=-27 and 13 and baseline y=69 |
| Notice board | Backing z=-8; RULES (-5,66,-7), NEWS (0,66,-7), STAFF (5,66,-7) |
| Survival direction | Existing landmark (13,65,-3), facing west |
| Leaderboard | Wall x=-38..-19, z=-17, y=64..67; TOP PLAYERS title (-29,67,-16), seven score signs x=-37,-35,-33,-31,-29,-27,-25, y=65, z=-16 |
| Parkour | Four single-block jumps at (-13,64,-1), (-13,64,2), (-13,64,5), (-13,64,8); solid plaza underneath |
| Staff house | Walls x=-35..-25, z=12..22, y=64..67; slab roof extends x=-36..-24, z=11..23, y=68 |
| Admin landmark | hub.admin_house=(-30,64,12), openable oak door; STAFF ONLY sign (-30,66,11) |
| Staff interior | Desk x=-33..-31, y=64, z=19; chest (-27,64,20), lectern (-27,64,16); second permanent doorway x=-31..-30, z=22 |

The old `lobby.well` coordinate (-9,64,5) is a solid planter, with no water or cavity. Lamps, benches, and flower planters decorate the plaza without enclosing it.

## Survival builds

- Noor's oak cottage: x=19..27, z=22..30; front door (23,64,22), claim sign (20,65,21), rear exit.
- Zuri's stone/quartz house: x=37..46, z=23..32; front door (41,64,23), claim sign (38,65,22), rear exit.
- Efe's half-finished birch house: x=20..29, z=38..47; front door (24,64,38), claim sign (21,65,37), open rear and incomplete sides.
- Farm: x=19..28, z=51..58; dry farmland and mature wheat rows, hay, no irrigation holes or water.
- Ruined arena: x=35..48, z=41..55; permanent wide fence openings north, west, and south. Colored wool obstacles are only one block tall; the blue L shapes repeat the same orientation rather than properly facing the opposing team. Broken scoreboard at (46,66,53).

## MapLayout changes

- GROUND_Y=63 and ISLAND=(-58,57,-84)..(122,91,84) remain unchanged.
- Lobby bounds expand to (-40,60,-24)..(16,90,26).
- Path bounds expand to (17,60,-10)..(50,90,60).
- All existing checkpoint, trigger, and landmark positions remain unchanged. Spawn yaw changes from 180 to 0 to satisfy the requested south-facing arrival; the name is placed south of spawn accordingly.
- New landmark `hub.admin_house` belongs to lobby; no new section id is introduced.
- Authored TREES becomes an empty list; normal world generation provides natural scenery.
- HillSection, base/mine bounds, and all route triggers retain their coordinates. The hill and mine retain their existing authored terrain on top of the newly graded footprints.
- BIOME_ID remains as an unused compatibility constant; normal template generation and verification do not force the custom biome.

## Validation and remaining runtime checks

Passed static checks: all anchor names match MapLayout; every original position declaration is preserved; section ownership remains disjoint; numeric placements fit their section bounds; 66 referenced/expanded sign keys exist in all eight language files; Spanish files are identical; the template script parses; `git diff --check` is clean.

`python3 scripts/story_check.py`: 0 errors, 2 existing story warnings (three screamer cues; session budget 1350 seconds). Story resources were not changed.

Compilation uncertainty: the code follows the existing placement, sign, door, block-family, and state APIs. New block constants are ordinary vanilla blocks. `BlockStateProperties.AGE_7` is newly used for mature wheat and should be checked by the lead against 26.3. No external API lookup or Java execution was performed.

The lead still needs to compile, author a fresh seed-4114 template, run MapVerifier.verifyRegeneration, and inspect the generated terrain boundaries and viewing corridor in Minecraft. Adaptive grading is intentionally allowed beyond four rings on steep terrain to avoid a cliff where a rigid four-block margin would meet a mountain or depression; it remains inside the island horizontally. Natural terrain outside the authored area is retained. Authoring scans natural ground across the island and clears tall terrain/trees above graded columns, so template generation may take longer than before. Runtime physics, natural leaf decay, farmland trampling, and the actual appearance of seed 4114 could not be tested here.
