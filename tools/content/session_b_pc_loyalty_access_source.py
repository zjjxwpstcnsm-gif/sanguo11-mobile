#!/usr/bin/env python3
"""Original property23/rawAC relation and aligned operand candidates; no rule substitution."""
import argparse,json,struct
from pathlib import Path
import capstone
from unicorn import UC_HOOK_CODE,UC_HOOK_MEM_READ
from unicorn.x86_const import UC_X86_REG_EIP
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve previous evidence')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 pe=struct.unpack_from('<I',exe,60)[0];count=struct.unpack_from('<H',exe,pe+6)[0];opt=struct.unpack_from('<H',exe,pe+20)[0];base=struct.unpack_from('<I',exe,pe+52)[0]
 sections=[struct.unpack_from('<4I',exe,pe+24+opt+40*i+8)for i in range(count)]
 dis=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);dis.detail=True;dis.skipdata=True;access=[]
 for virtual,rva,stored,offset in sections:
  if rva!=0x1000:continue
  data=exe[offset:offset+stored]
  for i in dis.disasm(data,base+rva):
   if i.id!=0 and any(o.type==capstone.x86.X86_OP_MEM and o.mem.disp==0xac for o in i.operands):
    start=max(0,i.address-base-rva-64);block=data[start:start+144]
    access.append(dict(address=i.address,mnemonic=i.mnemonic,operands=i.op_str,bytes=i.bytes.hex(),contextAddress=base+rva+start,contextHex=block.hex(),contextSha=sha(block)))
 print('Original aligned rawAC operand candidates',len(access),flush=True)
 w=NativeDebateFlow(installation,exe).world;load_original_data(w.u)
 shared=(installation/'Media/scenario/Scenario.s11').read_bytes();source=(installation/'Media/scenario/Scen000.s11').read_bytes();w.load(shared,True);loaded=w.load(source);w.call(0x73c500);w.call(0x73ca80)
 original=bytes(w.u.mem_read(0x7200000,0x300000));seed=bytes(w.u.mem_read(0x8a5d44,4));rows=[];visited=set();addresses={x['address']for x in access}
 def observe(u,ip,size,unused):
  if ip in addresses:visited.add(ip)
 active_raw=[None];read_sites=set()
 def read(u,access,address,size,value,unused):
  if active_raw[0] is not None and address<=active_raw[0]<address+size:read_sites.add(u.reg_read(UC_X86_REG_EIP))
 token=w.u.hook_add(UC_HOOK_CODE,observe);read_token=w.u.hook_add(UC_HOOK_MEM_READ,read)
 try:
  for native in [503,346,189,555]:
   person=w.call(0x490b00,native,receiver=w.root);record=bytes(w.u.mem_read(person,0x190));values=[];active_raw[0]=person+0xac
   for raw in range(256):
    w.u.mem_write(person+0xac,bytes([raw]));before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));value=w.call(0x4c8720,person,23,count=10000000)
    if before!=bytes(w.u.mem_read(0x7200000,0x300000))or rng!=bytes(w.u.mem_read(0x8a5d44,4)):raise ValueError('Original getter changed full World/RNG')
    values.append(value)
   w.u.mem_write(person,record);rows.append(dict(nativeId=native,serializedSourceRecordSha=next(x['sha256']for x in loaded['records']if x['kind']=='officer'and x['native_index']==native),loadedRuntimeObjectShaBeforeFixture=sha(record),displayByRaw=values,allOriginalGetterCallsFullWorldRngPure=True));print('Original getter completed native',native,flush=True)
 finally:w.u.hook_del(token);w.u.hook_del(read_token);w.u.mem_write(0x7200000,original);w.u.mem_write(0x8a5d44,seed)
 assert original==bytes(w.u.mem_read(0x7200000,0x300000))and seed==bytes(w.u.mem_read(0x8a5d44,4))
 sites=[]
 for ip in sorted(read_sites):
  virtual,rva,stored,offset=next(x for x in sections if x[1]<=ip-base<x[1]+max(x[0],x[2]));data=exe[offset+ip-base-rva:offset+ip-base-rva+96];sites.append(dict(address=ip,originalBytes=data.hex(),originalSha=sha(data),instructions=[dict(address=i.address,mnemonic=i.mnemonic,operands=i.op_str)for i in dis.disasm(data,ip)]))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,sharedSha=sha(shared),sourceSha=sha(source),property=23,rawOffset='AC',rows=rows,visitedOriginalOperandAddresses=sorted(visited),originalGetterRawReadSites=sites,operandCandidates=access,wholeWorldAndRngRestored=True,limits=['Source0 identities and declared raw0..255 fixture, original getter untouched','Operand candidates are not aligned containing-function or seasonal mutation proof','Current Android engineering loyalty mutations are not automatically certified by this getter relation','No original season/AP/new-game/control or ordinary defeat closure claim'],completeGoal=False),indent=2)+'\n');print('PASS original getter/source receipt',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
