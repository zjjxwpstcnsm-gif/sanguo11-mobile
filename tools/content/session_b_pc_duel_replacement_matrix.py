#!/usr/bin/env python3
"""Original current replacement comparator, source getters never substituted."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 r=json.loads(Path('out/session-b/duel-unit-context-source0-v4.json').read_text());d,w,src,geo,unused=prepare(installation);assert r['source']==src;baseline=bytes(w.u.mem_read(w.root,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));persons=[]
 for p in sum([c['declaredCrew']for c in r['cases']],[]):
  b=bytes(w.u.mem_read(p['pointer'],400));persons.append(dict(native=p['nativeId'],pointer=p['pointer'],status=struct.unpack_from('<i',b,0xa0)[0],capacity=w.call(0x48a4f0,receiver=p['pointer'])&65535,leadership=w.call(0x489070,receiver=p['pointer'])&255,war=w.call(0x489080,receiver=p['pointer'])&255,merit=struct.unpack_from('<H',b,0xae)[0]))
 rows=[]
 for a in persons:
  for b in persons:
   rows.append(dict(left=a['native'],right=b['native'],less=bool(w.call(0x4cf160,a['pointer'],b['pointer'],count=10000000))))
 assert baseline==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,persons=persons,rows=rows,worldAndRngPure=True,completeGoal=False),indent=2)+'\n');print('PASS original replacement comparator',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
