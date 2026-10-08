#!/usr/bin/env python3
"""Source postloader person/gear getters, without assuming playable activation."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output,index=0):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier receipt')
 d,w,source,geo,unused=prepare(installation,index);before=bytes(w.u.mem_read(w.root,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));items=[];people=[]
 for native in range(100):
  p=w.root+0x7777c+native*0x54;b=bytes(w.u.mem_read(p,0x54));items.append(dict(nativeId=native,valid=bool(w.call(0x47a630,p)),kind=struct.unpack_from('<i',b,0x38)[0],owner=struct.unpack_from('<i',b,0x40)[0],runtimeSha=sha(b),runtimeHex=b.hex()))
 for native in range(670):
  p=w.call(0x490b00,native,receiver=w.root);b=bytes(w.u.mem_read(p,0x190));held=[[i['nativeId'],i['kind']]for i in items if i['valid']and i['owner']==native];get=lambda at:struct.unpack_from('<i',b,at)[0]
  people.append(dict(nativeId=native,valid=bool(w.call(0x47a600,p)),internalFather=get(0x54),publicFather=get(0x58),publicMother=get(0x5c),spouse=get(0x60),swornGroup=get(0x64),birthplace=get(0xe4),rawLoyalty=b[0xac],heldNativeItems=held,originalTreasureBonus=w.call(0x4faa60,p,count=10000000)))
 assert before==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,items=items,people=people,worldAndRngPure=True,limits=['Original postserializer/post493400 state, full opening events unknown','Valid records never imply activation; native items not project names or IDs','No battle/campaign/normalUI/APK acceptance'],completeGoal=False),indent=2)+'\n');print('PASS original runtime bindings',len(people),len(items),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--source-index',type=int,default=0);a=p.parse_args();inspect(a.installation,a.output,a.source_index)
