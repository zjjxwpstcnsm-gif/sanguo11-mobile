#!/usr/bin/env python3
"""Complete original5ba410 deterministic command hash; no RNG or input replacement."""
import argparse,random
from pathlib import Path
from inspect_pc_layered_scenario import NativeLayeredWorld
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve prior native results')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA;world=NativeLayeredWorld(exe);generator=random.Random(564116222);cases=[]
 for bound in [-2147483648,-1,0,1,2,3,5,100,255,2147483647]:
  for inputs in [[0]*7,[1]*7,[-1]*7,[2147483647]*7,[-2147483648]*7,[564,116,222,35,0,0,0],[116,222,67,0,365,0,0]]:
   cases.append((bound,inputs))
 for n in range(500):cases.append((generator.choice([2,3,5,100,255,2147483647]),[generator.randrange(-2147483648,2147483648)for i in range(7)]))
 rng=bytes(world.u.mem_read(0x8a5d44,4));before=bytes(world.u.mem_read(0x7200000,0x300000));rows=['# originalEXE '+EXE_SHA+' function5ba410; fixture input integers; command hash has no global RNG effects']
 for bound,inputs in cases:
  result=world.call(0x5ba410,bound&0xffffffff,*[v&0xffffffff for v in inputs]);rows.append('\t'.join(map(str,[bound,*inputs,result])))
 assert rng==bytes(world.u.mem_read(0x8a5d44,4))and before==bytes(world.u.mem_read(0x7200000,0x300000));output.parent.mkdir(parents=True,exist_ok=True);output.write_text('\n'.join(rows)+'\n');print('PASS complete original command hash',len(cases),'cases World/RNG pure SHA',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
