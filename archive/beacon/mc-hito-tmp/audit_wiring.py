from pathlib import Path
import subprocess,json,hashlib
p=Path('src/main/resources/fabric.mod.json');data=json.loads(p.read_text());configs=[]
for branch in ['origin/v0.1.3','origin/v0.1.4','origin/v0.1.5','origin/v0.1.10','origin/v0.1.11']:
 meta=json.loads(subprocess.check_output(['git','show',branch+':src/main/resources/fabric.mod.json']))
 for config in meta.get('mixins',[]):
  if config not in configs:configs.append(config)
data['mixins']=configs
data['description']='Mod de terror: contrato, sesión persistente, mapa, cebo de catalejo, efectos de interfaz, audio y epílogo.'
p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
# All package-added paths must exist. Compare independent content, not just merge ancestry.
for n in [1,3,4,5,6,7,8,10,11,14,15]:
 branch=f'origin/v0.1.{n}';base=subprocess.check_output(['git','merge-base','8969f93',branch],text=True).strip()
 added=subprocess.check_output(['git','diff','--name-only','--diff-filter=A',base,branch],text=True).splitlines()
 missing=[name for name in added if not Path(name).is_file()]
 assert not missing,(branch,missing)
 print(branch,'all',len(added),'added paths present')
assert Path('docs/plan/DECISIONS.md').read_bytes()==subprocess.check_output(['git','show','origin/v0.1.14:docs/plan/DECISIONS.md'])
print('Owner decisions SHA-256',hashlib.sha256(Path('docs/plan/DECISIONS.md').read_bytes()).hexdigest())
