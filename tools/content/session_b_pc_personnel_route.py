#!/usr/bin/env python3
"""Original initialized city neighbors and untouched598d60 next-city oracle."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier receipt')
 d,w,source,geo,_=prepare(installation);before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));table=bytes(w.u.mem_read(0x79b830,1764));neighbors=[];rows=[]
 for native in range(42):
  city=w.call(0x490a10,native,receiver=w.root);assert w.call(0x47a630,city)==1
  values=list(struct.unpack('<6i',w.u.mem_read(city+0x1c,24)))
  for slot in range(6):assert w.call(0x47bc30,slot,receiver=city)==values[slot]&0xffffffff
  neighbors.append(values)
 for origin in range(42):
  for dest in range(42):
   expected=dest if origin==dest else min((n for n in neighbors[origin]if 0<=n<42),key=lambda n:table[n*42+dest],default=-1)
   actual=w.call(0x598d60,origin,dest,count=1000000);assert actual==expected&0xffffffff,(origin,dest,actual,expected);rows.append([origin,dest,expected])
 assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,neighbors=neighbors,neighborBytesSha=sha(struct.pack('<252i',*[n for row in neighbors for n in row])),rows=rows,fullWorldAndRngPure=True,limits=['Original source0 initialized city six-neighbor fields and original getter/next-city function; equal distance preserves slot order','No ordinary task37 turn scheduling/arrival callback or APK claim'],completeGoal=False),indent=2)+'\n');print('PASS original next-city',len(rows)+252,sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
