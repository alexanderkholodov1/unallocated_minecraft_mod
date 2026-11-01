from pathlib import Path
import json
root=Path('src/main/resources/data/unallocated')
from zipfile import ZipFile
with ZipFile('/home/alexamaze/Workspaces/mc-ref/client-26.3.jar') as z:
 dimension=json.loads(z.read('data/minecraft/dimension_type/overworld.json'))
dimension.update(has_fixed_time=True, timelines=[], attributes={'minecraft:audio/music_volume':0.0})
dimension.pop('default_clock',None)
biome=json.loads((root/'worldgen/biome/lobby.json').read_text())
biome.update(has_precipitation=False,downfall=0.0)
biome['attributes'].update({'minecraft:visual/sky_color':'#808080','minecraft:visual/fog_color':'#808080'})
for name,data in [('dimension_type/pocket.json',dimension),('dimension/pocket.json',{'type':'unallocated:pocket','generator':{'type':'minecraft:flat','settings':{'biome':'unallocated:pocket','layers':[{'block':'minecraft:air','height':1}],'lakes':False,'features':False,'structure_overrides':[]}}}),('worldgen/biome/pocket.json',biome)]:
 p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
p=Path('src/main/java/org/unallocated/UnallocatedMod.java');s=p.read_text().replace('        org.unallocated.story.StoryRuntime.initialize();','        org.unallocated.world.WorldActions.initialize();\n        org.unallocated.story.StoryRuntime.initialize();');p.write_text(s)
