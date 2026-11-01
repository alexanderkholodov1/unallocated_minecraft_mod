from zipfile import ZipFile
from io import BytesIO
import struct
z=ZipFile('/home/alexamaze/Workspaces/mc-ref/client-26.3.jar')
import sys
target=sys.argv[1].replace('.', '/') + '.class'
if target not in z.namelist():
 z=ZipFile('/home/alexamaze/Workspaces/mc-ref/fabric-api-0.165.0+26.3.jar')
 for entry in z.namelist():
  if entry.endswith('.jar'):
   nested=ZipFile(BytesIO(z.read(entry)))
   if target in nested.namelist(): z=nested;break
f=BytesIO(z.read(target))
def u1():return int.from_bytes(f.read(1),'big')
def u2():return int.from_bytes(f.read(2),'big')
def u4():return int.from_bytes(f.read(4),'big')
f.read(8); count=u2(); cp=[None]*count;i=1
while i<count:
 t=u1()
 if t==1:cp[i]=f.read(u2()).decode(errors='replace')
 elif t in(3,4):cp[i]=(t,f.read(4))
 elif t in(5,6):cp[i]=(t,f.read(8));i+=1
 elif t in(7,8,16,19,20):cp[i]=(t,u2())
 elif t==15:cp[i]=(t,u1(),u2())
 else:cp[i]=(t,u2(),u2())
 i+=1
def show(n):
 x=cp[n]
 if isinstance(x,str):return x
 if x[0]in(7,8):return show(x[1])
 if x[0]in(9,10,11,12,18):return str(x[0])+':'+show(x[1])+'.'+show(x[2])
 return str(x)
f.read(6); f.read(2*u2())
for group in range(2):
 for _ in range(u2()):
  access,name,desc=u2(),cp[u2()],cp[u2()]
  for a in range(u2()):
   attr=cp[u2()];raw=f.read(u4())
   if group==1 and (not __import__('sys').argv[2:] or name in __import__('sys').argv[2:]) and attr=='Code':
    n=int.from_bytes(raw[4:8],'big'); code=raw[8:8+n]
    print(name,desc,code.hex())
    print('strings:', [show(int.from_bytes(code[j+1:j+3],'big') if b==0x13 else code[j+1]) for j,b in enumerate(code[:-2]) if b in (0x12,0x13) and 0<(int.from_bytes(code[j+1:j+3],'big') if b==0x13 else code[j+1])<count]); print('refs:',[show(int.from_bytes(code[j+1:j+3],'big')) for j,b in enumerate(code[:-2]) if b in(0xb2,0xb4,0xb6,0xb7,0xb8,0xb9) and 0<int.from_bytes(code[j+1:j+3],'big')<count])
