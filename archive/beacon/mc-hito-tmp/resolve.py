from pathlib import Path
import re
rx=re.compile(r'^<<<<<<< CURRENT\n(.*?)^\|{7}.*?\n(.*?)^=======\n(.*?)^>>>>>>>.*?\n',re.M|re.S)
def resolve(file, choices=None):
 p=Path(file); i=iter(choices) if choices else None
 def sub(m):
  c=next(i) if i else 'o'
  return m[1] if c=='o' else m[3] if c=='t' else m[1]+m[3] if c=='b' else c if isinstance(c,str) else c(m)
 p.write_text(rx.sub(sub,p.read_text()))
def show():
 for p in Path('.').rglob('*'):
  if '.tmp' in p.parts or '.git' in p.parts or not p.is_file():continue
  try:s=p.read_text()
  except:continue
  for i,m in enumerate(rx.finditer(s)):print('\n',p,i,'OURS\n',m[1],'THEIRS\n',m[3])
if __name__=='__main__':show()
