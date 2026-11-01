from resolve import resolve
from pathlib import Path
for name in ['gradle.properties','build.gradle','.github/workflows/build.yml','docs/plan/TOOLCHAIN.md','docs/core/INTEGRATION.md','src/clientSmoke/java/org/unallocated/smoke/TitleScreenSmokeTest.java']:resolve(name)
resolve('README.md',['o','o','o',lambda m:m[1].split('En el menú principal, entra')[0]+m[3],'o','o',lambda m:m[1]+'\n'+m[3].split('CI also runs')[0]])
resolve('scripts/smoke_test.py',[lambda m:m[1].replace("('unallocated arm', [r'UNALLOCATED session armed']),", "('unallocated build all', [r'UNALLOCATED map built section=all', r'UNALLOCATED map verified section=all']),\n            ('unallocated mapcheck rebuild', [r'UNALLOCATED map regeneration verified sections=5']),"),'o','o','o'])
p=Path('scripts/smoke_test.py');s=p.read_text().replace('    # Dedicated startup overrides world mode/difficulty', '    if expects_rejection(api_version):\n        return\n    # Dedicated startup overrides world mode/difficulty')
s=s.replace("'unallocated-title-first',", "'unallocated-title-first',\n                            'map-lobby-spawn', 'map-lobby-board', 'map-hill-bait_path', 'map-hill-view', 'map-path-midpoint',\n                            'map-base-entrance', 'map-base-theft_zone', 'map-mine-entrance', 'map-mine-threshold',")
s=s.replace("r'UNALLOCATED spyglass smoke: three looks, closer each time, screamer, figure gone\\.',", "r'UNALLOCATED spyglass smoke: three looks, closer each time, screamer, figure gone\\.',\n                r'UNALLOCATED smoke: map tour completed\\.',")
# Accept the package's tuple-of-names API as well as one string, with exact PNG suffixes.
a=s.index('    # Fabric can prepend',s.index('def verify_screenshot'));b=s.index('\n\ndef client_phases',a)
s=s[:a]+'''    # Fabric can prepend a numeric counter; one capture cannot substitute for another.
    names = (name,) if isinstance(name, str) else name
    for expected in names:
        screenshots = list(directory.rglob(f'*{expected}.png'))
        if not any(path.read_bytes().startswith(b'\\x89PNG\\r\\n\\x1a\\n') for path in screenshots):
            raise RuntimeError(f'Client exited without a valid screenshot: {expected}')
'''+s[b:];p.write_text(s)
p=Path('src/clientSmoke/java/org/unallocated/smoke/TitleScreenSmokeTest.java');p.write_text(p.read_text().replace('new SpyglassBaitSmokeTest().runTest(context);','new SpyglassBaitSmokeTest().runTest(context);\n            MapTourSmokeTest.run(context);'))
p=Path('src/clientSmoke/java/org/unallocated/smoke/MapTourSmokeTest.java');s=p.read_text().replace('                world.getConnection().waitForChunksRender();','''                try {
                    world.getConnection().waitForChunksRender();
                } catch (AssertionError failure) {
                    context.runOnClient(client -> {
                        int radius = client.options.getEffectiveRenderDistance();
                        int cx = client.player.blockPosition().getX() >> 4;
                        int cz = client.player.blockPosition().getZ() >> 4;
                        var missing = new java.util.ArrayList<String>();
                        for (int dz = -radius; dz <= radius; dz++) for (int dx = -radius; dx <= radius; dx++) {
                            if (client.level.getChunk(cx + dx, cz + dz,
                                    net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) == null) {
                                missing.add(dx + "," + dz);
                            }
                        }
                        LoggerFactory.getLogger("unallocated-smoke").error(
                                "UNALLOCATED map render wait checkpoint={} radius={} rendered={} missing={}",
                                entry.getKey(), radius, client.levelRenderer.hasRenderedAllSections(), missing);
                    });
                    throw failure;
                }''');p.write_text(s)
