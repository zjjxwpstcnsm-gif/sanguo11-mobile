#!/usr/bin/env python3
"""Original layered header flag: record actual writes/reads, no startup claims."""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_EIP
from inspect_pc_layered_scenario import NativeLayeredWorld
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA;shared=(installation/'Media/scenario/Scenario.s11').read_bytes();manifest=json.loads((ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_text());rows=[]
 w=NativeLayeredWorld(exe);writes=[]
 def write(u,access,address,size,value,user):writes.append(dict(ip=hex(u.reg_read(UC_X86_REG_EIP)),address=hex(address),bytes=size,value=value))
 w.u.hook_add(UC_HOOK_MEM_WRITE,write,begin=w.root+0x18,end=w.root+0x1b)
 for source in manifest['scenarios']:
  writes=[];w.load(shared,True);sharedWrites=list(writes);writes=[];raw=(installation/source['sourcePath']).read_bytes();assert sha(raw)==source['sourceSha256'];result=w.load(raw);header=next(r for r in result['records']if r['kind']=='world_header');reads=header.get('reads',[]);rows.append(dict(sourceId=source['scenarioId'],path=source['sourcePath'],sha=source['sourceSha256'],flag18=struct.unpack('<i',w.u.mem_read(w.root+0x18,4))[0],sharedWrites=sharedWrites,sourceWrites=list(writes),header=header));print('Original header',source['sourcePath'],'flag',rows[-1]['flag18'],'writes',len(writes),'reads',len(reads),flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,sharedSha=sha(shared),rows=rows,limits=['Complete original Shared/scenario layered read boundary; no geography/postload/normal GUI/startup controller','Write hooks observe original execution, never change it; absent serialized read does not prove source flag value','No production flags/defaults imported and no PC mutations']),indent=2)+'\n');print('PASS original16 header flag provenance',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
