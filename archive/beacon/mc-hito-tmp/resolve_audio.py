from resolve import resolve
from pathlib import Path
import subprocess,json
for name in ['gradle.properties','README.md','build.gradle','.github/workflows/build.yml','docs/plan/TOOLCHAIN.md','src/clientSmoke/java/org/unallocated/smoke/TitleScreenSmokeTest.java','src/main/resources/fabric.mod.json','src/main/resources/META-INF/NOTICE-Fabric-API.txt']:resolve(name)
for name in ['src/main/java/org/unallocated/UnallocatedMod.java','src/client/java/org/unallocated/client/UnallocatedClient.java','scripts/test_smoke_test.py']:resolve(name,['b'])
resolve('scripts/test_verify_jar.py',[lambda m:m[3].split('    def test_repackaged_module')[0]+m[1]])
resolve('scripts/smoke_test.py',[lambda m:m[3].replace('Mixin apply.*failed|', 'Mixin apply.*failed|UNALLOCATED map command failed|'),lambda m:m[3].split('def client():')[0]+m[1],'o'])
for language in ['en_us','es_es']:
 name=f'src/main/resources/assets/unallocated/lang/{language}.json'
 ours=json.loads(subprocess.check_output(['git','show','HEAD:'+name]));theirs=json.loads(subprocess.check_output(['git','show','origin/v0.1.11:'+name]))
 for k,v in theirs.items():
  assert k not in ours or ours[k]==v,k
  ours[k]=v
 Path(name).write_text(json.dumps(ours,ensure_ascii=False,indent=2)+'\n')
p=Path('src/main/resources/fabric.mod.json');data=json.loads(p.read_text());data['mixins'].append({'config':'unallocated-audio.client.mixins.json','environment':'client'});p.write_text(json.dumps(data,indent=2)+'\n')
p=Path('src/clientSmoke/java/org/unallocated/smoke/TitleScreenSmokeTest.java');p.write_text(p.read_text().replace('EndingSmokeTest.run(context);','EndingSmokeTest.run(context);\n            AudioSmokeTest.run(context);'))
p=Path('scripts/smoke_test.py');s=p.read_text().replace('modules, title,','modules, title, *audio_evidence(),',1);p.write_text(s)
p=Path('src/client/java/org/unallocated/client/effects/AudioLayerSound.java');s=p.read_text().replace('    void finish() { stop(); }','''    // AbstractSoundInstance multiplies these fields by the resolved vanilla/resource-pack
    // sample's gain and pitch. Apply authored bounds AFTER that resolution, retaining fades.
    @Override
    public float getVolume() { return Math.clamp(super.getVolume(), 0f, volume); }

    @Override
    public float getPitch() { return Math.clamp(super.getPitch(), 0.5f, 2f); }

    void finish() { stop(); }''');p.write_text(s)
p=Path('src/clientSmoke/java/org/unallocated/smoke/AudioSmokeTest.java');s=p.read_text().replace('if (sound.getVolume() > 0.55f || sound.getPitch() < 0.5f)', 'if (!Float.isFinite(sound.getVolume()) || !Float.isFinite(sound.getPitch())\n                                || sound.getVolume() < 0f || sound.getVolume() > layer.volume()\n                                || sound.getPitch() < 0.5f || sound.getPitch() > 2f)')
s=s.replace('throw new AssertionError("Audio exceeded authored gain/pitch bounds");','throw new AssertionError("Audio exceeded authored gain/pitch bounds: " + layer.sound()\n                                    + " volume=" + sound.getVolume() + " pitch=" + sound.getPitch());');p.write_text(s)
p=Path('docs/audio/AUDIO.md');p.write_text(p.read_text()+'\nIntegrated playback clamps the resolved sample gain to the current authored fade envelope and\nresolved pitch to 0.5–2.0. Vanilla event and resource-pack multipliers are applied before this\nboundary; a layer field alone is not the final sound-engine value. The real client smoke checks\nall seven resolved layers, their individual gain limits, finite values and both pitch bounds.\n')
p=Path('README.md');s=p.read_text();lines=s.splitlines();lines[4]='**Versión actual: 0.1.0 — hito integrado.** Incluye el contrato antes del menú, núcleo de sesión guardado, mapa por código (lobby, colina, camino, base y mina), cebo de catalejo con susto, herramientas de interfaz, cuatro señales de audio y un epílogo que guarda y cierra la sesión. Todas las dependencias necesarias viajan dentro del JAR. El inicio desde un botón propio y la experiencia narrativa completa de 10–15 minutos siguen pendientes.';p.write_text('\n'.join(lines)+'\n')
