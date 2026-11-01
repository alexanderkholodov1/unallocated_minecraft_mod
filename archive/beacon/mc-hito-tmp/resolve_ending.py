from resolve import resolve
from pathlib import Path
for name in ['gradle.properties','README.md','build.gradle','.github/workflows/build.yml','docs/plan/TOOLCHAIN.md','docs/core/INTEGRATION.md','src/clientSmoke/java/org/unallocated/smoke/TitleScreenSmokeTest.java','scripts/smoke_test.py']:resolve(name)
p=Path('src/clientSmoke/java/org/unallocated/smoke/TitleScreenSmokeTest.java');p.write_text(p.read_text().replace('MapTourSmokeTest.run(context);','MapTourSmokeTest.run(context);\n            EndingSmokeTest.run(context);'))
p=Path('src/clientSmoke/java/org/unallocated/smoke/CoreSessionSmokeTest.java');s=p.read_text().replace('runtime.sequencer().advance(world.getConnection().getServerPlayer());', '''// Exercise the core's terminal/rescue guard without invoking the player disconnect;
                // EndingSmokeTest separately verifies the real-player EPILOGUE and teardown.
                runtime.sequencer().advance(null);''',1).replace('runtime.sequencer().advance(world.getConnection().getServerPlayer());','runtime.sequencer().advance(null);',1);p.write_text(s)
p=Path('src/clientSmoke/java/org/unallocated/smoke/EndingSmokeTest.java');s=p.read_text().replace('// No receipt package is bundled in this worktree; the tested farewell must still work.', '// The integrated contract receipt must be reused without being rewritten.')
s=s.replace('            // Normal sequencer path,', '            CoreRuntime.arm(s);\n            // Normal sequencer path,')
s=s.replace('        commandWorld.getServer().runOnServer(s -> {', '        commandWorld.getServer().runOnServer(s -> {\n            CoreRuntime.arm(s);')
s=s.replace('!text.contains("Gracias por tus datos.")', '!text.contains("Gracias por tus datos.")\n                    || !text.contains("Dijiste «dE AcUeRdO»")');p.write_text(s)
p=Path('scripts/smoke_test.py');s=p.read_text().replace("r'UNALLOCATED smoke: map tour completed\\.',", "r'UNALLOCATED smoke: map tour completed\\.',\n                r'UNALLOCATED ending=SERVER_CLOSED',\n                r'UNALLOCATED smoke: ending save reloaded as finished; options and receipt unchanged; splash reset\\.',\n                r'UNALLOCATED smoke: ending command showed vanilla disconnect and returned to title with changed splash\\.',")
s=s.replace("'unallocated-title-first',", "'unallocated-title-first',\n                            'unallocated-ending-disconnect', 'unallocated-ending-title',\n                            'unallocated-ending-command-disconnect', 'unallocated-ending-command-title',")
p.write_text(s)
