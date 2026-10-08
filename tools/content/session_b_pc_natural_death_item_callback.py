#!/usr/bin/env python3
"""Full original natural death callback from an actual archived outcome2.
Held-item placements after battle are explicitly isolated boundary inputs.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_EIP,UC_X86_REG_ESP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve receipt')
 raw=Path('out/session-b/natural-duel-death-source0-v1.json').read_bytes();assert sha(raw)=='4436f4ad74f967f775ad63aaf9c45a504058eee17e71c28c781a8d5e47145655';r=json.loads(raw);endpoint=r['selected'];assert endpoint['deadNative']==116 and endpoint['outcomes']==[0,2,0,0,0,0]
 print('Loading original source',flush=True);d,w,source,geo,_=prepare(installation);assert source==r['source'];baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));cases=[];stage='constructors'
 try:
  for f,p in [(0x415400,0x32602b0),(0x477810,0x6ee7888),(0x4f52d0,0x91ba558)]:w.call(f,receiver=p,count=50000000)
  assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
  context=json.loads(Path('out/session-b/duel-unit-context-source0-v4.json').read_text());units=[row['pointer']for row in context['units']]
  for ptr,data,case in zip(units,endpoint['unitsBefore'],context['cases']):
   w.u.mem_write(ptr,bytes.fromhex(data));w.u.reg_write(UC_X86_REG_EAX,ptr);loc=w.call(0x4a7530)
   for person in case['declaredCrew']:w.call(0x4a0cb0,person['pointer'],loc,receiver=0x799895c,count=50000000)
  assert all(bytes(w.u.mem_read(w.root+0xc0bc+p['nativeId']*0x190,0x190)).hex()==p['hex']for p in endpoint['peopleBefore'])
  configured=bytes(w.u.mem_read(0x7200000,0x300000));target=w.call(0x490b00,116,receiver=w.root)
  def items():return [dict(nativeId=n,hex=bytes(w.u.mem_read(w.root+0x7777c+n*0x54,0x54)).hex())for n in range(43)]
  display=next(p for p in json.loads(Path('out/session-b/duel-campaign-presentation-source-v2.json').read_text())['functions']if p['address']=='0x588bb0');assert sha(bytes(w.u.mem_read(0x588bb0,0xb0)))==display['sha256'];entries=[];omitted=[]
  def observe(u,ip,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);entries.append(dict(address=hex(ip),receiver=hex(u.reg_read(UC_X86_REG_ECX)),stackWords=list(struct.unpack('<6I',u.mem_read(sp,24)))))
  for address in [0x4aa680,0x489d40,0x484de0,0x4acbe0,0x4a9120]:w.u.hook_add(UC_HOOK_CODE,observe,begin=address,end=address)
  def valueDisplay(u,ip,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);ret,*args=struct.unpack('<4I',u.mem_read(sp,16));omitted.append(dict(address=hex(ip),arguments=args,sha=display['sha256']));u.reg_write(UC_X86_REG_ESP,sp+16);u.reg_write(UC_X86_REG_EIP,ret)
  w.u.hook_add(UC_HOOK_CODE,valueDisplay,begin=0x588bb0,end=0x588bb0)
  from session_b_pc_readonly_render_abi import bind_readonly_render_abi
  environmentCalls=bind_readonly_render_abi(w)
  for held in [[],[30],[0,30]]:
   stage='full4d3340 held'+str(held);w.u.mem_write(0x7200000,configured);w.u.mem_write(d.fixture,bytes.fromhex(endpoint['modelHex']));w.u.mem_write(0x8b3740,bytes.fromhex(endpoint['managerHex']));w.u.mem_write(0x8a5d44,struct.pack('<I',endpoint['terminalRng']));entries.clear();omitted.clear()
   for item in held:w.call(0x484de0,116,0xffffffff,receiver=w.root+0x7777c+item*0x54,count=50000000)
   before=items();w.call(0x4d3340,count=50000000);after=items();cases.append(dict(declaredPostbattleHeld=held,before=before,after=after,entries=list(entries),omittedValueDisplay=list(omitted),deadHex=bytes(w.u.mem_read(target,0x190)).hex(),rngAfter=int.from_bytes(w.u.mem_read(0x8a5d44,4),'little')));print('PASS original natural item callback',held,flush=True)
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
  output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,actualNaturalReceiptSha=sha(raw),cases=cases,wholeWorldAndRngRestored=True,readonlyEnvironmentCalls=environmentCalls,limits=['Actual archived natural657-frame outcome2/model/manager retained exactly, no death result changed','Item holder116 placement only after battle; this isolates full held-item callback, not normal equipped combat','Full4d3340/4acbe0/4aa680/getter/setter retained; only prior examined void588bb0 value display omitted','Direct3D registry unavailable/scalar CPU are explicit OS fixtures, actual Windows state unknown; no game getter/outcome replacement','Original GUI/normal deployment/equipment/other sources remain separate'],completeGoal=False),indent=2)+'\n');print('PASS original natural item receipt',sha(output.read_bytes()),flush=True)
 except Exception as e:output.with_suffix('.failure.json').write_text(json.dumps(dict(stage=stage,cases=cases,error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid,esp=hex(w.u.reg_read(UC_X86_REG_ESP)),codeHex=bytes(w.u.mem_read(w.u.reg_read(UC_X86_REG_EIP),24)).hex(),currentEntries=entries[-20:],platformCalls=d.platform.calls[-20:]),indent=2)+'\n');raise
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
