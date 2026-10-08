#!/usr/bin/env python3
"""Original current ruler/force3c/49cdf0/481910 context, read-only observations."""
import argparse,json,struct,itertools
from pathlib import Path
import capstone
from unicorn.x86_const import UC_X86_REG_EIP,UC_X86_REG_EAX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def signed(n):return n if n<0x80000000 else n-0x100000000
def inspect(installation,output,index=0,forced_matrix=False):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve earlier receipt')
 d,w,source,geo,_=prepare(installation,index);before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
 functions=[]
 for a,n in [(0x49cdf0,0x60),(0x481910,0x60),(0x436740,4),(0x4adb30,0xc5),(0x490a10,0x120),(0x491270,0x20)]:
  b=bytes(w.u.mem_read(a,n));functions.append(dict(address=hex(a),sha256=sha(b),hex=b.hex(),instructions=[dict(address=hex(i.address),mnemonic=i.mnemonic,operands=i.op_str)for i in md.disasm(b,a)]))
 result=dict(exeSha=EXE_SHA,source=source,geography=geo,functions=functions,root14=signed(struct.unpack('<I',w.u.mem_read(w.root+0x14,4))[0]),forces=[],completeGoal=False)
 try:
  place=w.call(0x49cdf0,receiver=0x780b3cc,count=10000000);result['actual49cdf0PlaceNative']=signed(place)
  native=result['root14']
  if 0<=native<1100:
   p=w.call(0x490b00,native,receiver=w.root);b=bytes(w.u.mem_read(p,0x190));result['root14Person']=dict(nativeId=native,pointer=p,hex=b.hex(),actualValid=bool(w.call(0x47a600,p)),actualPresent=bool(w.call(0x47a630,p)),nativeIdGetter=w.call(0x4883c0,receiver=p),currentPlace=signed(struct.unpack_from('<I',b,0x9c)[0]),administrativeHome=signed(struct.unpack_from('<I',b,0x98)[0]),status=signed(struct.unpack_from('<I',b,0xa0)[0]))
  for native in range(47):
   p=w.call(0x490aa0,native,receiver=w.root);b=bytes(w.u.mem_read(p,0x12c));valid=bool(w.call(0x47a630,p));fields={str(k):signed(w.call(0x4c4260,p,k))for k in range(1,25)}
   predicate=w.call(0x481910,receiver=p,count=10000000);raw3c=signed(struct.unpack_from('<I',b,0x3c)[0]);getter=w.call(0x436740,receiver=p);assert signed(getter)==raw3c
   result['forces'].append(dict(nativeId=native,pointer=p,valid=valid,hex=b.hex(),raw3c=raw3c,original436740=signed(getter),original481910=bool(predicate),properties=fields))
  if forced_matrix:
   result['declaredTitleMatrix']=[]
   for targetForce,captorForce in [(3,2),(3,29),(29,3)]:
    t=next(f for f in result['forces']if f['nativeId']==targetForce);a=next(f for f in result['forces']if f['nativeId']==captorForce);target=w.call(0x490b00,t['properties']['3'],receiver=w.root);captor=w.call(0x490b00,a['properties']['3'],receiver=w.root);assert w.call(0x488c00,receiver=target)and w.call(0x47a630,captor)
    disliked=bool(w.call(0x4889e0,t['properties']['3'],receiver=captor))
    for targetTitle,captorTitle in itertools.product([0,4,9],repeat=2):
     w.u.mem_write(0x7200000,before);w.u.mem_write(t['pointer']+0x3c,struct.pack('<i',targetTitle));w.u.mem_write(a['pointer']+0x3c,struct.pack('<i',captorTitle));declared=bytes(w.u.mem_read(0x7200000,0x300000));w.u.reg_write(UC_X86_REG_EAX,captor);actual=w.call(0x4adb30,receiver=target,count=10000000);assert declared==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
     expected=disliked or targetTitle==0 and a['original481910']or t['original481910']and captorTitle==0;assert bool(actual)==expected
     result['declaredTitleMatrix'].append(dict(targetForce=targetForce,captorForce=captorForce,targetNative=t['properties']['3'],captorNative=a['properties']['3'],targetTitle=targetTitle,captorTitle=captorTitle,target481910=t['original481910'],captor481910=a['original481910'],captorDislikesTarget=disliked,original4adb30=bool(actual)))
   w.u.mem_write(0x7200000,before)
  assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 except Exception as e:
  result['failure']=dict(error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid);output.with_suffix('.failure.json').write_text(json.dumps(result,indent=2)+'\n');raise
 result.update(fullSourceWorldAndRngQueryPure=True,worldSha=sha(before),nativeRng=struct.unpack('<I',rng)[0],limits=['Original complete direct postload493400, source date and initialized geography','Raw force3c and original predicates retain address names until field/identity semantics proven','No original normal GUI/menu/event/emperor movement or Android gameplay completion claim'])
 output.write_text(json.dumps(result,indent=2)+'\n');print('PASS original ruler context',source['sourcePath'],'SHA',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--forced-matrix',action='store_true');p.add_argument('--source-index',type=int,choices=range(16),default=0);a=p.parse_args();inspect(a.installation,a.output,a.source_index,a.forced_matrix)
