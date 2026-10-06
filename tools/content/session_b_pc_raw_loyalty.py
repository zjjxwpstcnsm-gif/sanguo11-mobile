#!/usr/bin/env python3
"""Original initialized rawloyalty versus property getter; never infer capped raw.
Loaded source identities/validity are recorded, not interpreted as activation.
"""
import argparse,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from session_b_pc_geography_context import load_geography
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard

def inspect(installation,output,index):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier evidence')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 manifest=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();source=json.loads(manifest)['scenarios'][index];raw=(installation/source['sourcePath']).read_bytes();assert sha(raw)==source['sourceSha256']
 shared=(installation/'Media/scenario/Scenario.s11').read_bytes();w=NativeDebateFlow(installation,exe).world;load_original_data(w.u);w.load(shared,True);loaded=w.load(raw);w.call(0x73c500);w.call(0x73ca80)
 for f,v in zip([0x4826e0,0x482700,0x482720],source['date']):w.call(f,v,receiver=w.root)
 w.call(0x4827b0,0,receiver=w.root);geography=load_geography(w,installation);w.call(0x493400,receiver=w.root,count=50000000)
 before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));records={r['native_index']:r for r in loaded['records']if r['kind']=='officer'};rows=[]
 for native in range(1100):
  p=w.call(0x490b00,native,receiver=w.root);assert w.call(0x4883c0,receiver=p)==native;data=bytes(w.u.mem_read(p,0x190));record=records.get(native)
  rows.append(dict(nativeId=native,rawLoyalty=data[0xac],getterLoyalty=w.call(0x4c8720,p,23),rawStatus=struct.unpack_from('<i',data,0xa0)[0],rawHome=struct.unpack_from('<i',data,0x98)[0],action=bool(struct.unpack_from('<I',data,0x124)[0]&1),validObject=bool(w.call(0x47a630,p)),validPerson=bool(w.call(0x47a600,p)),serializedRecordSha=None if record is None else record['sha256']))
 assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 report=dict(source=source,exeSha=EXE_SHA,sharedSha=sha(shared),sourceManifestSha=sha(manifest),geography=geography,rows=rows,wholeWorldAndRngUnchanged=True,limits=['Original initialized/postload1100-domain rawbyteAC/getter/status/validity; not effective activation or original menu/events','Source immutable rawloyalty only; current mutable rawloyalty cannot be inferred from display cap or catalog after owner/relationship/loyalty mutation','Native IDs need independently checked stable runtime/source joins; no ID arithmetic','Serialized record absent remains absent; constructed template/NPC/ancient slots do not become playable'],completeGoal=False)
 output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print('PASS source',index,'rawdomain',len(rows),'validPerson',sum(x['validPerson']for x in rows),'raw/getter differences',sum(x['rawLoyalty']!=x['getterLoyalty']for x in rows),'World/RNG pure SHA',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--source',type=int,choices=range(16),required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output,a.source)
