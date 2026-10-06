#!/usr/bin/env python3
"""Reproduce original country5/native58 failure without enabling candidate27/28."""
import argparse,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('installation',type=Path);parser.add_argument('--output',type=Path,required=True);parser.add_argument('--force-getters',action='store_true');parser.add_argument('--controlled-cases',action='store_true');args=parser.parse_args()
install=args.installation.resolve();output_guard(install,args.output)
if args.output.exists():raise ValueError('Preserve previous report')
exe=(install/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
source=json.loads((ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_text())['scenarios'][0]
raw=(install/source['sourcePath']).read_bytes();assert sha(raw)==source['sourceSha256']
w=NativeDebateFlow(install,exe).world;load_original_data(w.u);w.load((install/'Media/scenario/Scenario.s11').read_bytes(),True);w.load(raw);w.call(0x73c500);w.call(0x73c2b0)
for f,v in zip([0x4826e0,0x482700,0x482720],source['date']):w.call(f,v,receiver=w.root)
fixed=struct.unpack('<i',w.u.mem_read(w.root+0x18,4))[0];w.call(0x4827b0,1 if fixed else 0,receiver=w.root)
if fixed:w.call(0x493f70,1,receiver=w.root);w.call(0x4827f0,3,receiver=w.root)
w.call(0x493400,receiver=w.root,count=50000000)
p403=w.call(0x490b00,403,receiver=w.root);father403=struct.unpack('<i',w.u.mem_read(p403+0x54,4))[0]
before=sha(bytes(w.u.mem_read(0x7200000,0x300000)));seed=bytes(w.u.mem_read(0x8a5d44,4)).hex();rows=[]
for n in [58,403]:
 p=w.call(0x490b00,n,receiver=w.root);b=bytes(w.u.mem_read(p,0x190));table=struct.unpack_from('<I',b)[0];getter=struct.unpack('<I',w.u.mem_read(table+0x40,4))[0];owner=w.call(getter,receiver=p);force=w.call(0x490aa0,owner,receiver=w.root);fb=bytes(w.u.mem_read(force,0x80));country=struct.unpack_from('<i',fb,0x40)[0];father=struct.unpack_from('<i',b,0x54)[0]
 ruler=bool(w.call(0x488c00,receiver=p));title=w.call(0x436740,receiver=force);t0=w.call(0x490bf0,0,receiver=w.root);rank20=w.call(0x490c10,20,receiver=w.root)
 row=dict(nativeId=n,nativeOwner=owner,country=country,status=struct.unpack_from('<i',b,0xa0)[0],rawOffice=struct.unpack_from('<i',b,0xa4)[0],rawFather=father,rawFather403=father403,sameFather=father==father403,ruler=ruler,forceTitle=title,title0Capacity=struct.unpack('<H',w.u.mem_read(t0+0x2e,2))[0],rank20Capacity=struct.unpack('<H',w.u.mem_read(rank20+0x2c,2))[0],effectiveOriginal=w.call(0x48a4f0,receiver=p)&65535)
 if args.force_getters:row['forceGetterFields']={str(key):w.call(0x4c4260,force,key)&0xffffffff for key in list(range(3,21))+[35]}
 rows.append(row)
if args.controlled_cases:
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));controlled=[]
 p58=w.call(0x490b00,58,receiver=w.root);p403=w.call(0x490b00,403,receiver=w.root);ref403=struct.unpack('<i',w.u.mem_read(p403+0x54,4))[0]
 for country,status,rank,same in [(0,3,80,False),(5,3,80,False),(5,3,20,False),(5,3,80,True),(0,0,80,False),(5,0,80,False)]:
  w.u.mem_write(0x7200000,baseline)
  force=w.call(0x490aa0,28,receiver=w.root)
  w.u.mem_write(force+0x40,struct.pack('<i',country));w.u.mem_write(p58+0xa0,struct.pack('<i',status));w.u.mem_write(p58+0xa4,struct.pack('<i',rank))
  if same:w.u.mem_write(p58+0x54,struct.pack('<i',ref403))
  b=bytes(w.u.mem_read(0x7200000,0x300000));seed0=bytes(w.u.mem_read(0x8a5d44,4));result=w.call(0x48a4f0,receiver=p58)&65535
  assert b==bytes(w.u.mem_read(0x7200000,0x300000))and seed0==bytes(w.u.mem_read(0x8a5d44,4))
  controlled.append(dict(country=country,status=status,rank=rank,sameReference403=same,effectiveOriginal=result,worldPure=True,rngPure=True,scope='controlled VM fields; not normal GUI or altered source file'))
 w.u.mem_write(0x7200000,baseline)
after=sha(bytes(w.u.mem_read(0x7200000,0x300000)));seedAfter=bytes(w.u.mem_read(0x8a5d44,4)).hex();assert before==after and seed==seedAfter
report=dict(source=source,exeSha=EXE_SHA,rows=rows,worldPure=True,rngPure=True,scope='read-only actual original source0 post-load493400 capacity derivation; not production enablement')
if args.controlled_cases:report['controlled']=controlled
args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(rows,ensure_ascii=False))
