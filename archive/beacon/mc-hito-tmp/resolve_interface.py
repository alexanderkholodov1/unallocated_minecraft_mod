from resolve import resolve
from pathlib import Path
import subprocess,json
for name in ['gradle.properties','.github/workflows/build.yml','docs/plan/TOOLCHAIN.md','src/clientSmoke/java/org/unallocated/smoke/TitleScreenSmokeTest.java']:resolve(name)
resolve('README.md',['o','o','o','t','o'])
resolve('build.gradle',['o','o',lambda m:m[1]+"    jvmArgs.addAll('-Dunallocated.interface.debug=true', '-Dunallocated.interface.debug.quiet=true')\n"])
resolve('scripts/smoke_test.py',[lambda m:m[3].split('def client():')[0]+m[1], 'o'])
resolve('scripts/test_smoke_test.py',[lambda m:m[1].rstrip()+', verify_interface_screenshots\n','b'])
resolve('src/main/resources/fabric.mod.json',['o',lambda m:m[1].replace('"environment": "client"}', '"environment": "client"},\n    {"config": "unallocated-interface.mixins.json", "environment": "client"}')])
for language in ['en_us','es_es']:
 name=f'src/main/resources/assets/unallocated/lang/{language}.json'
 ours=json.loads(subprocess.check_output(['git','show','HEAD:'+name])); theirs=json.loads(subprocess.check_output(['git','show','origin/v0.1.4:'+name]))
 for k,v in theirs.items():
  assert k not in ours or ours[k]==v,k
  ours[k]=v
 Path(name).write_text(json.dumps(ours,ensure_ascii=False,indent=2)+'\n')
p=Path('src/clientSmoke/java/org/unallocated/smoke/TitleScreenSmokeTest.java');p.write_text(p.read_text().replace('if (phase.equals("first")) CoreSessionSmokeTest.run(context);','if (phase.equals("first")) {\n            CoreSessionSmokeTest.run(context);\n            InterfaceEffectsSmokeTest.run(context);\n        }'))
p=Path('scripts/smoke_test.py');s=p.read_text().replace("r'UNALLOCATED smoke: core armed join, no-cheats rescue guards, client act sync and single-player reload verified\\.',", "r'UNALLOCATED smoke: core armed join, no-cheats rescue guards, client act sync and single-player reload verified\\.',\n                r'UNALLOCATED interface smoke: effects rendered; options untouched byte-for-byte after world exit\\.',")
s=s.replace("'unallocated-title-first'],", "'unallocated-title-first',\n                            *[f'unallocated-interface-{effect}' for effect in\n                              ('splash', 'loading', 'f3', 'advancement', 'sign', 'death')]],")
p.write_text(s)
p=Path('src/clientSmoke/resources/fabric.mod.json');s=json.loads(p.read_text());s['version']='0.1.0';s['depends']['minecraft']='26.3';p.write_text(json.dumps(s,indent=2)+'\n')
