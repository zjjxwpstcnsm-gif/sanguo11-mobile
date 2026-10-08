#!/usr/bin/env python3
"""Original5d5220(target,actor) search-discovery recruitment resolver, observed UI boundaries.
No game getter, probability, roll, result or RNG replacement. Direct recruitment is a separate5c5940/5c5220 chain; player selection and upstream menu/payment remain unknown.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EIP,UC_X86_REG_ESP,UC_X86_REG_EAX,UC_X86_REG_ECX
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from session_b_pc_geography_context import load_geography
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard

def inspect(installation,output,residents=False,inputs=False,targets=False):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier evidence')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 manifest=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();src=json.loads(manifest)['scenarios'][0]
 shared=(installation/'Media/scenario/Scenario.s11').read_bytes();raw=(installation/src['sourcePath']).read_bytes();assert sha(raw)==src['sourceSha256']
 w=NativeDebateFlow(installation,exe).world;load_original_data(w.u);w.load(shared,True);w.load(raw);w.call(0x73c500);w.call(0x73ca80);w.call(0x49b490,receiver=0x767cab8)
 for f,v in zip([0x4826e0,0x482700,0x482720],src['date']):w.call(f,v,receiver=w.root)
 w.call(0x4827b0,0,receiver=w.root);geography=load_geography(w,installation);w.call(0x493400,receiver=w.root,count=50000000)
 actor=w.call(0x490b00,116,receiver=w.root);target=w.call(0x490b00,222,receiver=w.root);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4))
 probability=w.call(0x5c51c0,target,actor,0,0);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 resident_rows=[]
 if residents:
  for native in range(850):
   person=w.call(0x490b00,native,receiver=w.root);table=struct.unpack('<I',w.u.mem_read(person,4))[0];owner=w.call(struct.unpack('<I',w.u.mem_read(table+0x40,4))[0],receiver=person)
   if owner!=2 or struct.unpack('<i',w.u.mem_read(person+0x98,4))[0]!=8 or not w.call(0x47a600,person):continue
   chance=w.call(0x5c51c0,target,person,0,0);ruler=w.call(0x490b00,w.call(0x489d40,receiver=person),receiver=w.root);gap=w.call(0x489f80,w.call(0x4883c0,receiver=ruler),receiver=person);charm=w.call(0x4890b0,receiver=person)
   resident_rows.append(dict(nativeId=native,probability=chance,currentCharm=charm,actorRulerAffinityGap=gap,debateThreshold=chance+charm-gap,person=person))
  assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 target_lists=[]
 if targets:
  vector=0xc400000;w.u.mem_map(vector,0x1000);city=w.call(0x490a10,8,receiver=w.root)
  for row in resident_rows:
   w.call(0x47c250,receiver=vector);returned=w.call(0x5d1ea0,row['person'],city,vector,count=50000000);nodes=[];cursor=struct.unpack('<I',w.u.mem_read(vector+4,4))[0]
   while cursor:
    if cursor in nodes:raise ValueError('Original enumeration cycle')
    nodes.append(cursor);cursor=struct.unpack('<I',w.u.mem_read(cursor,4))[0]
   people=[]
   for node in nodes:
    pointer=struct.unpack('<I',w.u.mem_read(node+8,4))[0];people.append(w.call(0x4883c0,receiver=pointer))
   target_lists.append(dict(actorNative=row['nativeId'],returned=returned,nativeTargets=people,containerHex=bytes(w.u.mem_read(vector,32)).hex()))
   w.call(0x47c100,receiver=vector)
 cases=[];tracked={0x5ba410,0x5c512d,0x5c5169,0x5c519d,0x5c4f80,0x5d5220,0x5c51c0,0x5ba4c0,0x5d52b6,0x5d52f5,0x5b81d0,0x5d3c90,0x51db80,0x520680,0x4f5450,0x4f54a0};boundaries={0x520680,0x4f5450,0x4f54a0}
 def write(partial=False):
  report=dict(source=src,exeSha=EXE_SHA,sharedSha=sha(shared),manifestSha=sha(manifest),geographyContext=geography,targetNative=222,actorNative=116,queryProbability=probability,residentActors=resident_rows,targetLists=target_lists,cases=cases,completeGoal=False,limits=['Original resolver itself, not upstream legal menu/admission/payment','Source original controller flags retained; no player/controller invented','Explicit starting seeds are fixtures; do not certify original startup RNG','Observation stops before original UI/presentation calls; no rule or RNG function substituted'])
  dest=output.with_suffix('.partial.json')if partial else output;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(json.dumps(report,indent=2)+'\n')
 for chosen_actor,seed in ([(r["person"],23)for r in resident_rows]if residents else [(actor,s)for s in [0,1,2,3,4,5,23,42,99,100]]):
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,struct.pack('<I',seed));row=dict(actorNative=w.call(0x4883c0,receiver=chosen_actor),seed=seed,trace=[],originalResolverComplete=False);cases.append(row)
  def trace(u,ip,size,data):
   if ip not in tracked:return
   sp=u.reg_read(UC_X86_REG_ESP);event=dict(ip=hex(ip),eax=u.reg_read(UC_X86_REG_EAX),ecx=hex(u.reg_read(UC_X86_REG_ECX)),stack=bytes(u.mem_read(sp,36)).hex());row['trace'].append(event)
   if inputs and ip in [0x5c512d,0x5c5169,0x5c519d]:event['localsHex']=bytes(u.mem_read(sp,80)).hex()
   if ip==0x520680:event['inputHex']=bytes(u.mem_read(struct.unpack('<I',u.mem_read(sp+4,4))[0],32)).hex()
   if ip in boundaries:row['uiBoundary']=hex(ip);u.emu_stop()
  hook=w.u.hook_add(UC_HOOK_CODE,trace)
  try:w.call(0x5d5220,target,chosen_actor,count=50000000);row['originalResolverComplete']=True
  except Exception as error:
   if 'uiBoundary'not in row:row.update(error=repr(error),nativeIp=hex(w.u.reg_read(UC_X86_REG_EIP)),invalidMemory=w.invalid);write(True);raise
  finally:w.u.hook_del(hook)
  row['finalRng']=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];row['worldChangedBytes']=sum(a!=b for a,b in zip(baseline,bytes(w.u.mem_read(0x7200000,0x300000))));write(True);print(json.dumps({k:v for k,v in row.items()if k!='trace'}),flush=True)
 write()
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--residents',action='store_true');p.add_argument('--inputs',action='store_true');p.add_argument('--targets',action='store_true');a=p.parse_args();inspect(a.installation,a.output,a.residents,a.inputs,a.targets)
