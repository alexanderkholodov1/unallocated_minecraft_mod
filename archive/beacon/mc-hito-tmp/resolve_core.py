from resolve import resolve
from pathlib import Path
for file in ['gradle.properties','build.gradle','src/clientSmoke/java/org/unallocated/smoke/TitleScreenSmokeTest.java']:resolve(file)
resolve('README.md',['o','o','o','o',lambda m:m[1]+'\nAfter the title check, a real-player integrated-server test verifies unarmed worlds stay clean, armed JOIN starts at LOBBY, no-cheats rescue guards, client act synchronization and single-player save/reopen.\n',lambda m:m[3].replace('and all four network codecs','all four network codecs and the contract receipt')])
resolve('docs/core/INTEGRATION.md',['t'])
resolve('docs/plan/TOOLCHAIN.md',['o','o','o','b'])
resolve('scripts/smoke_test.py',[lambda m:m[1].replace("('unallocated act LOBBY', [entered])", "('unallocated arm', [r'UNALLOCATED session armed']),\n            ('unallocated act LOBBY', [entered])"),'o'])
p=Path('src/clientSmoke/java/org/unallocated/smoke/TitleScreenSmokeTest.java');s=p.read_text();s=s.replace('"UNALLOCATED smoke: title screen reached from player JAR.");','"UNALLOCATED smoke: title screen reached from player JAR.");\n        if (phase.equals("first")) CoreSessionSmokeTest.run(context);');p.write_text(s)
p=Path('scripts/smoke_test.py');s=p.read_text().replace("modules, title,\n            ],", "modules, title,\n                r'UNALLOCATED smoke: core armed join, no-cheats rescue guards, client act sync and single-player reload verified\\.',\n            ],",1);p.write_text(s)
