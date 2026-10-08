#!/usr/bin/env python3
"""Original47b480 personnel duration table and4839f0 parent; no route guesses."""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ESP,UC_X86_REG_EIP
from inspect_pc_scenario_domains import NativeDomainDecoder
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier receipt')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA;d=NativeDomainDecoder(exe)
 table=bytes(d.u.mem_read(0x79b830,42*42));parents=bytes(d.u.mem_read(0x79c2b0,87));before=bytes(d.u.mem_read(0x7200000,0x300000));rng=bytes(d.u.mem_read(0x8a5d44,4));rows=[]
 def call(function,*args):
  d.u.reg_write(UC_X86_REG_ESP,d.stack);d.u.mem_write(d.stack,struct.pack('<'+'I'*(len(args)+1),d.stop,*[a&0xffffffff for a in args]));d.u.emu_start(function,d.stop,count=10000);assert d.u.reg_read(UC_X86_REG_EIP)==d.stop;return d.u.reg_read(UC_X86_REG_EAX)
 for a in range(-1,43):
  for b in range(-1,43):
   actual=call(0x47b480,a,b);expected=table[a*42+b]if 0<=a<42 and 0<=b<42 else 0xffffffff;assert actual==expected;rows.append([a,b,actual])
 for n in range(87):assert call(0x4839f0,n)==parents[n]
 assert before==bytes(d.u.mem_read(0x7200000,0x300000))and rng==bytes(d.u.mem_read(0x8a5d44,4));blob=table+parents
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,tableAddress='0x79b830',tableSha=sha(table),parentAddress='0x79c2b0',parentSha=sha(parents),resourceSha=sha(blob),rows=rows,parents=list(parents),blobHex=blob.hex(),fullWorldAndRngPure=True,limits=['Original static table and full47b480 getter boundaries; no engineering graph','Original4839f0 parents87 not native invalid port IDs87..127','Task37 completion/normal fullTurn and APK remain separate'],completeGoal=False),indent=2)+'\n');print('PASS original personnel table',len(rows)+87,sha(output.read_bytes()),sha(blob),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
