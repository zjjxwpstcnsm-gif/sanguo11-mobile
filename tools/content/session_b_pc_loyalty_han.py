#!/usr/bin/env python3
"""Original property49/raw108 and complete loyalty, declared numeric inputs."""
import argparse,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve original receipt')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA;raw=(installation/'Media/scenario/Scen000.s11').read_bytes();shared=(installation/'Media/scenario/Scenario.s11').read_bytes();w=NativeDebateFlow(installation,exe).world;w.load(shared,True);w.load(raw);w.call(0x73c500)
 a=w.call(0x490b00,222,receiver=w.root);r=w.call(0x490b00,365,receiver=w.root);force=w.call(0x490aa0,2,receiver=w.root);assert w.call(0x4c4260,force,3)==365;baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[]
 for targetHan in range(3):
  for rulerHan in range(3):
   w.u.mem_write(0x7200000,baseline);w.u.mem_write(a+0xa0,struct.pack('<i',3));w.u.mem_write(a+0xac,bytes([80]));w.u.mem_write(a+0x54,struct.pack('<i',-1));w.u.mem_write(a+0x60,struct.pack('<2i',-1,-1));w.u.mem_write(a+0x6c,struct.pack('<10i',*([-1]*10)));w.u.mem_write(a+0x69,bytes([50]));w.u.mem_write(r+0x69,bytes([0]));w.u.mem_write(a+0xf0,struct.pack('<2i',0,0));w.u.mem_write(a+0x108,struct.pack('<i',targetHan));w.u.mem_write(r+0x108,struct.pack('<i',rulerHan));w.u.mem_write(a+0xe4,struct.pack('<i',1));w.u.mem_write(r+0xe4,struct.pack('<i',2));w.u.mem_write(r+0x174,bytes([0]));w.u.mem_write(0x8a5d44,struct.pack('<I',23));before=bytes(w.u.mem_read(0x7200000,0x300000));tg=w.call(0x4c8720,a,49);rg=w.call(0x4c8720,r,49);w.call(0x4a75a0,a,force,0,receiver=0x799895c,count=10000000);value=bytes(w.u.mem_read(a+0xac,1))[0];expected=bytearray(before);expected[a-0x7200000+0xac]=value;assert bytes(expected)==bytes(w.u.mem_read(0x7200000,0x300000))and struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]==23;rows.append(dict(targetRaw108=targetHan,rulerRaw108=rulerHan,targetProperty49=tg,rulerProperty49=rg,originalRawLoyalty=value,originalRng=23))
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(exeSha=EXE_SHA,sourceSha=sha(raw),sharedSha=sha(shared),rows=rows,wholeWorldAndRngRestored=True,limits=['Declared numeric inputs on source identities222/365, not historical field changes','Original full getter and4a75a0 unchanged; no normal recruitment command or APK claim']),indent=2)+'\n');print('PASS original Han equality',rows,sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
