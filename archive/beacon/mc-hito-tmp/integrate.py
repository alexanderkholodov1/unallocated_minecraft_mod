import os, subprocess, pathlib, json, sys
ROOT=pathlib.Path.cwd()
os.environ['TMPDIR']=str(ROOT/'.tmp')
def git(*args): return subprocess.check_output(['git',*args])
branch=sys.argv[1]
base=git('merge-base','HEAD',branch).decode().strip()
work=ROOT/'.tmp/integration'/branch.replace('/','_'); work.mkdir(parents=True,exist_ok=True)
conflicts=[]; changed=[]
for raw in git('diff','--name-only',base,branch).decode().splitlines():
 p=ROOT/raw
 def content(ref):
  result=subprocess.run(['git','show',ref+':'+raw],stdout=subprocess.PIPE,stderr=subprocess.DEVNULL)
  return result.stdout if result.returncode==0 else b''
 old=content(base); incoming=content(branch); ours=p.read_bytes() if p.exists() else b''
 if incoming==old or ours==incoming: continue
 if ours==old: merged=incoming
 else:
  for name,data in [('ours',ours),('base',old),('theirs',incoming)]: (work/name).write_bytes(data)
  result=subprocess.run(['git','merge-file','-p','--diff3','-L','CURRENT','-L',base[:7],'-L',branch,str(work/'ours'),str(work/'base'),str(work/'theirs')],stdout=subprocess.PIPE)
  merged=result.stdout
  if result.returncode:
   conflicts.append(raw)
   backup=work/(raw.replace('/','__')+'.theirs'); backup.write_bytes(incoming)
 p.parent.mkdir(parents=True,exist_ok=True); p.write_bytes(merged)
 mode=git('ls-tree',branch,'--',raw).decode().split(' ')[0]
 if mode=='100755': p.chmod(0o755)
 changed.append(raw)
(work/'files.json').write_text(json.dumps(changed,indent=2))
print(json.dumps({'branch':branch,'base':base,'changed':changed,'conflicts':conflicts},indent=2))
