#!/usr/bin/env python3
"""Native loop harness runs the untouched48d3b0 classifier for all targets."""
import argparse,json,struct,time
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EAX,UC_X86_REG_EIP
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_HOOK_CODE
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output,index,rows,clone=False,cache_permissions=False):
 cache_permissions=cache_permissions or clone and index>=4
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve original receipt')
 d,w,source,geo,unused=prepare(installation,index);before=bytes(w.u.mem_read(w.root,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));codeAt=d.fixture+0x3000;resultAt=d.fixture+0x4000
 cloneRegions=[]
 if clone:
  original=w.u;fresh=Uc(UC_ARCH_X86,UC_MODE_32)
  for start,end,permissions in original.mem_regions():
   b=bytes(original.mem_read(start,end-start+1));fresh.mem_map(start,end-start+1,permissions);fresh.mem_write(start,b);cloneRegions.append(dict(start=hex(start),end=hex(end),permissions=permissions,sha=sha(b)))
  fresh.context_restore(original.context_save());assert struct.unpack('<I',fresh.mem_read(0x74e268,4))[0]==w.stop+0x200 and struct.unpack('<I',fresh.mem_read(0x74e26c,4))[0]==w.stop+0x200
  regions=tuple(fresh.mem_regions())
  def cached_probe(u,address,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);ret,pointer,length=struct.unpack('<III',u.mem_read(sp,12));allowed=length==0 or pointer!=0 and any(start<=pointer and pointer+length-1<=end and permissions&3==3 for start,end,permissions in regions);u.reg_write(UC_X86_REG_EAX,0 if allowed else 1);u.reg_write(UC_X86_REG_ESP,sp+12);u.reg_write(UC_X86_REG_EIP,ret)
  fresh.hook_add(UC_HOOK_CODE,cached_probe if cache_permissions else w.probe_pointer,begin=w.stop+0x200,end=w.stop+0x200);w.u=fresh
  assert before==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 # cdecl outer harness(leftNative). EDI/ESI/EBP are preserved by original
 # thiscall; only this test loop/output lives outside authoritative World.
 code=bytearray(b'\x55\x56\x57\x8b\x7c\x24\x10\x31\xf6\xbd'+struct.pack('<I',resultAt))
 loop=len(code);code+=b'\x56\x69\xcf'+struct.pack('<I',0x190)+b'\x81\xc1'+struct.pack('<I',w.root+0xc0bc)
 at=codeAt+len(code);code+=b'\xe8'+struct.pack('<i',0x48d3b0-at-5)
 code+=b'\x88\x44\x35\x00\x46\x81\xfe'+struct.pack('<I',670)
 at=codeAt+len(code);code+=b'\x0f\x85'+struct.pack('<i',codeAt+loop-at-6)+b'\x5f\x5e\x5d\xc3'
 w.u.mem_write(codeAt,bytes(code));results=[];started=time.monotonic()
 for left in range(rows):
  w.call(codeAt,left,count=50000000);row=bytes(w.u.mem_read(resultAt,670));assert all(v in (0,1,2,3,255)for v in row);results.append(row.hex())
  if left%10==0:print('original classifier source',index,'row',left,'elapsed',round(time.monotonic()-started,3),flush=True)
 if cache_permissions:assert clone and tuple(w.u.mem_regions())==regions
 assert before==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(cachedImmutableOsPermissions=cache_permissions,cloneObserversOnly=clone,cloneRegions=cloneRegions,exeSha=EXE_SHA,source=source,geography=geo,rows=rows,columns=670,classifier='48d3b0 raw0..3 or -1; source graph, not current editor mutation',harnessHex=bytes(code).hex(),harnessSha=sha(code),rowHex=results,elapsedSeconds=time.monotonic()-started,wholeWorldAndRngPure=True,completeGoal=False),indent=2)+'\n');print('PASS original blood matrix rows',rows,'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--source-index',type=int,default=0);p.add_argument('--rows',type=int,default=670);p.add_argument('--clone-observers-only',action='store_true');p.add_argument('--cache-permissions',action='store_true');a=p.parse_args();inspect(a.installation,a.output,a.source_index,a.rows,a.clone_observers_only,a.cache_permissions)
