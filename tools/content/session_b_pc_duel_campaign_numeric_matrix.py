#!/usr/bin/env python3
"""Untouched original draw/no-casualty callback, declared terminal input matrix."""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output,normal=False):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 r=json.loads(Path('out/session-b/duel-campaign-source0-numeric-v1.json').read_text());assert sha(Path('out/session-b/duel-campaign-source0-numeric-v1.json').read_bytes())=='722a6e94c55eacd0a024aaefb7fc30076d78a88e197eaeae209a6eea97df45c7'
 d,w,src,geo,unused=prepare(installation);assert r['source']==src;baseline=bytes(w.u.mem_read(w.root,0x300000));originalRng=bytes(w.u.mem_read(0x8a5d44,4));manager=bytes.fromhex(r['managerHex']);units=r['unitPointers'];crew=r['crew'];rows=[];omitted=[]
 evidence=json.loads(Path('out/session-b/duel-campaign-presentation-source-v2.json').read_text());f=next(v for v in evidence['functions']if v['address']=='0x588bb0');assert sha(bytes(w.u.mem_read(0x588bb0,0xb0)))==f['sha256']
 def display(u,address,size,user):
  sp=u.reg_read(UC_X86_REG_ESP);words=struct.unpack('<4I',u.mem_read(sp,16));omitted.append(list(words[1:]));u.reg_write(UC_X86_REG_ESP,sp+16);u.reg_write(UC_X86_REG_EIP,words[0])
 hook=w.u.hook_add(UC_HOOK_CODE,display,begin=0x588bb0,end=0x588bb0)
 def facts():
  persons=[]
  for p in crew:
   b=bytes(w.u.mem_read(p['pointer'],0x190));persons.append(dict(native=p['nativeId'],health=b[0x128],injury=struct.unpack_from('<i',b,0x15c)[0],merit=struct.unpack_from('<H',b,0xae)[0],xp=list(struct.unpack_from('<5H',b,0x12a))))
  return dict(persons=persons,units=[dict(capacity=w.call(0x4957b0,receiver=p),troops=struct.unpack('<H',w.u.mem_read(p+0x18,2))[0],energy=w.u.mem_read(p+0x1a,1)[0],valid=bool(w.call(0x47a630,p)))for p in units],rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0])
 for winner in [-1,0,1]:
  for troops in [1,100,5000,20000]:
   for energy in [0,95,100]:
    for seed in [0,23,0xffffffff]:
     w.u.mem_write(w.root,baseline);w.u.mem_write(0x8a5d44,struct.pack('<I',seed));m=bytearray(manager)
     for side in range(2):
      w.u.mem_write(units[side],bytes.fromhex(r['unitBefore'][side]));w.u.mem_write(units[side]+0x18,struct.pack('<H',troops));w.u.mem_write(units[side]+0x1a,bytes([energy]));
      if normal:w.u.mem_write(units[side]+8,struct.pack('<i',0))
      struct.pack_into('<i',m,0x54+4*side,winner if side==0 else -1 if winner<0 else 1-winner)
      for slot in range(3):struct.pack_into('<i',m,0x64+12*side+4*slot,0);struct.pack_into('<i',m,0x7c+12*side+4*slot,[0,1,40,80,100,100][side*3+slot]);struct.pack_into('<i',m,0xac+12*side+4*slot,0)
     struct.pack_into('<2i',m,0x5c,0,0);w.u.mem_write(0x8b3740,bytes(m));before=facts();omitted.clear();w.call(0x4d3260 if winner<0 else 0x4d3340,count=10000000);after=facts();rows.append(dict(winner=winner,troops=troops,energy=energy,seed=seed,managerHex=bytes(m).hex(),before=before,after=after,omittedValueDisplay=list(omitted)))
  print('PASS original callback winner',winner,'cases',len(rows),flush=True)
 w.u.hook_del(hook);w.u.mem_write(w.root,baseline);w.u.mem_write(0x8a5d44,originalRng);assert baseline==bytes(w.u.mem_read(w.root,0x300000))and originalRng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(bothCategory0=normal,exeSha=EXE_SHA,source=src,geography=geo,crew=crew,rows=rows,wholeWorldAndRngRestored=True,limits=['Declared final manager/no casualty/troops/energy/seed, actual original callbacks and setters untouched','Only examined void588bb0 value display omitted, no PC UI/audio claim','Capture/death and normal ordinary command are separate mandatory branches'],completeGoal=False),indent=2)+'\n');print('PASS original terminal numeric matrix',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--normal-units',action='store_true');a=p.parse_args();inspect(a.installation,a.output,a.normal_units)
