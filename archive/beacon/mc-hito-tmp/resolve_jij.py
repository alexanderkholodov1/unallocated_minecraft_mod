from pathlib import Path
import re
rx=re.compile(r'^<<<<<<< CURRENT\n(.*?)^\|{7}.*?\n(.*?)^=======\n(.*?)^>>>>>>>.*?\n',re.M|re.S)
def resolve(file, choices):
 p=Path(file); i=iter(choices)
 def sub(m):
  c=next(i)
  return m[1] if c=='o' else m[3] if c=='t' else m[1]+m[3] if c=='b' else c if isinstance(c,str) else c(m)
 p.write_text(rx.sub(sub,p.read_text()))
resolve('gradle.properties',['o'])
resolve('build.gradle',['b'])
resolve('.github/workflows/build.yml',[
 lambda m:m[3].replace('timeout-minutes: 16','timeout-minutes: 25'),
 lambda m:m[1]+'        id: package\n',
 lambda m:m[3].replace('timeout-minutes: 4','timeout-minutes: 6'),
 lambda m:m[3].replace('timeout-minutes: 5','timeout-minutes: 10')])
resolve('README.md',['o','t','t','o','o',lambda m:m[1]+'\n'+m[3]])
p=Path('README.md');p.write_text(p.read_text().replace('unallocated-0.1.6.jar','unallocated-0.1.0.jar'))
resolve('docs/plan/TOOLCHAIN.md',['o','t','t','t','b'])
p=Path('docs/plan/TOOLCHAIN.md');p.write_text(p.read_text().replace('unallocated-0.1.6.jar','unallocated-0.1.0.jar').replace('unallocated-0.1.7.jar','unallocated-0.1.0.jar').replace('Toolchain — UNALLOCATED v0.1.7','Toolchain — UNALLOCATED v0.1.0'))
resolve('src/clientSmoke/java/org/unallocated/smoke/TitleScreenSmokeTest.java',[
 lambda m:m[1].replace('six Fabric API modules loaded from player JAR','bundled Fabric API modules verified')])
resolve('scripts/smoke_test.py',[
 '            shutdown_timeout=30, game_directory=None, command_steps=(), startup_required=None,\n            expected_rejection=None):\n    """Require clean game evidence or a specific pre-startup Loader dependency rejection."""\n',
 lambda m:m[3]+m[1],
 '''        command, directory, SMOKE / f'{name}-console.log', startup + [entered, status],
        timeout=120, stop_when_ready=not expects_rejection(api_version), startup_required=startup,
        command_steps=[] if expects_rejection(api_version) else [
            ('unallocated act LOBBY', [entered]), ('unallocated status', [status])],
        expected_rejection=api_version if expects_rejection(api_version) else None,
''',
 lambda m:'''    if expects_rejection(api_version):
        monitor(
            ['gradle', '--no-daemon', '--console=plain', 'runClientSmoke',
             f'-PsmokeApiVersion={api_version}', '-PsmokeExpectRejection=true'],
            ROOT, SMOKE / f'{name}-console.log', [], timeout=240,
            game_directory=directory, expected_rejection=api_version,
        )
        return
'''+m[1].replace("f'-PcontractSmokePhase={phase}']", "f'-PcontractSmokePhase={phase}',\n             f'-PsmokeApiVersion={api_version or \"none\"}']").replace("f'client-{phase}-console.log'", "f'{name}-{phase}-console.log'").replace("*checks['evidence'],", "*checks['evidence'],\n                *([rf'UNALLOCATED smoke: external Fabric API {re.escape(api_version)} loaded\\.']\n                  if api_version else []),")])
p=Path('scripts/smoke_test.py');p.write_text(p.read_text().replace("['java', '-Xms256M', '-Xmx1G', f'-Djava.io.tmpdir={temp}'", "['java', '-Xms256M', '-Xmx1G', '-Djava.awt.headless=true', f'-Djava.io.tmpdir={temp}'").replace('six Fabric API modules loaded from player JAR','bundled Fabric API modules verified'))
