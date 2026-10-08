#!/usr/bin/env python3
"""Full original AI successor selector; force EDI and source ruler are explicit."""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EDI,UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output,pairs=False):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve original receipt')
 d,w,source,geo,_=prepare(installation);before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[];vector=d.fixture+0x6200;preference=d.fixture+0x6400;w.call(0x4b5430,receiver=preference)
 try:
  for force in range(42):
   f=w.call(0x490aa0,force,receiver=w.root)
   if not w.call(0x47a630,f):continue
   leader=w.call(0x4c4260,f,3);p=w.call(0x490b00,leader,receiver=w.root)
   if not w.call(0x47a630,p)or not w.call(0x488c00,receiver=p):continue
   human=w.call(0x47a690,receiver=p)
   if human:raise ValueError('Human source selector needs actual GUI input')
   w.u.reg_write(UC_X86_REG_EDI,f);selected=w.call(0x4b7e70,p,count=20000000);native=w.call(0x4883c0,receiver=selected)if w.call(0x47a630,selected)else -1
   assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
   row=dict(forceNative=force,rulerNative=leader,selectorForceRegister='EDI',forcePointer=f,rulerPointer=p,selectedPointer=selected,selectedNative=native,originalForceHuman=human)
   if pairs:
    b=bytes(w.u.mem_read(p,400));root,father,mother=struct.unpack_from('<3i',b,0x54);row['references']=[root,father,mother];row['referenceValid']=[bool(w.call(0x48bb40,father&0xffffffff)),bool(w.call(0x412390,root&0xffffffff)),bool(w.call(0x412390,mother&0xffffffff))];w.call(0x47c250,receiver=vector);w.call(0x4cf480,vector,f,15,receiver=0x7999808,count=10000000);cursor=struct.unpack('<I',w.u.mem_read(vector+4,4))[0];candidates=[]
    while cursor:
     person=struct.unpack('<I',w.u.mem_read(cursor+8,4))[0];n=w.call(0x491310,person,receiver=w.root)
     if n!=leader:
      raw=bytes(w.u.mem_read(person,400));pf,pm=struct.unpack_from('<2i',raw,0x58);flags=[bool(w.call(0x48bd80,n,receiver=p)),pf==father,bool(w.call(0x48bc40,n,receiver=p)),bool(w.call(0x48bb70,root&0xffffffff,receiver=person)),bool(w.call(0x48bc70,n,receiver=p)),pm==mother,bool(w.call(0x4887d0,n,receiver=p)),bool(w.call(0x488790,n,receiver=p)),bool(w.call(0x488780,receiver=person))];candidates.append(dict(nativeId=n,pointer=person,flags=flags,age=w.call(0x488a20,receiver=person),merit=struct.unpack_from('<H',raw,0xae)[0],preferenceRaw=w.call(0x4b5410,n,receiver=preference),preference=w.call(0x4b5410,n,receiver=preference)&255))
     cursor=struct.unpack('<I',w.u.mem_read(cursor,4))[0]
    row['candidates']=candidates;row['comparisons']=[]
    for a in candidates:
     for b in candidates:
      actual=w.call(0x4b5460,a['pointer'],b['pointer'],p,preference,count=10000000);row['comparisons'].append(dict(first=a['nativeId'],second=b['nativeId'],originalBetter=bool(actual)))
    w.call(0x47c100,receiver=vector);assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
   rows.append(row);print('PASS original successor',force,leader,native,'pairs',len(row.get('comparisons',[])),flush=True)
 except Exception as e:
  output.with_suffix('.failure.json').write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,rows=rows,error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid),indent=2)+'\n');raise
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,rows=rows,wholeWorldAndRngPure=True,limits=['Full original4b7e70 query from source force/ruler; no death-state edit','Original per-person comparator/relations/preferences retained','Original normal GUI human successor input, callback settlement and APK remain required']),indent=2)+'\n');print('PASS original successor corpus',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--pairs',action='store_true');a=p.parse_args();inspect(a.installation,a.output,a.pairs)
