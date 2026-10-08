#!/usr/bin/env python3
"""PE-verified readonly Direct3D registry/scalar CPU fixture. Actual Windows unknown."""
import struct
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EIP,UC_X86_REG_ESP

def bind_readonly_render_abi(w):
 from pc_readonly_platform import ReadOnlyPlatform
 imports=ReadOnlyPlatform.imports(w.exe);abi={};environmentCalls=[];base=w.stop+0x5000;w.u.mem_map(base,0x1000)
 for slot,module,name,words in [(0x74e008,'ADVAPI32.dll','RegOpenKeyA',3),(0x74e014,'ADVAPI32.dll','RegQueryValueExA',6),(0x74e024,'ADVAPI32.dll','RegCloseKey',1),(0x74e158,'KERNEL32.dll','IsProcessorFeaturePresent',1)]:
  assert imports[slot]==(module,name);address=base+16*len(abi);abi[address]=(name,words);w.u.mem_write(slot,struct.pack('<I',address))
 def osReadonly(u,ip,size,user):
  name,words=abi[ip];sp=u.reg_read(UC_X86_REG_ESP);ret,*args=struct.unpack('<'+str(words+1)+'I',u.mem_read(sp,4*(words+1)));detail={}
  if name=='RegOpenKeyA':
   key=bytes(u.mem_read(args[1],160)).split(b'\0')[0];assert args[0]==0x80000002 and key==b'Software\\Microsoft\\Direct3D';value=2;detail={'unavailableRegistryKey':key.decode('ascii'),'actualWindowsRegistryKnown':False}
  elif name=='IsProcessorFeaturePresent':
   assert args[0]in [6,7];value=0;detail={'declaredScalarCPUFeature':args[0],'actualWindowsCPUKnown':False}
  else:raise ValueError('Unavailable registry must not yield a handle/value: '+name)
  environmentCalls.append(dict(function=name,returnPc=hex(ret),arguments=args,result=value,**detail));u.reg_write(UC_X86_REG_EAX,value);u.reg_write(UC_X86_REG_ESP,sp+4*(words+1));u.reg_write(UC_X86_REG_EIP,ret)
 w.u.hook_add(UC_HOOK_CODE,osReadonly,begin=base,end=base+0xff)
 return environmentCalls
