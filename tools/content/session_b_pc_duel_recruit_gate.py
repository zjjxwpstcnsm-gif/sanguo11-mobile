#!/usr/bin/env python3
"""Untouched4af7d0 modes1/2 original serving target558 with declared relationships."""
import argparse,json,struct,itertools
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 d,w,source,geo,_=prepare(installation);target=w.call(0x490b00,558,receiver=w.root);actor=w.call(0x490b00,365,receiver=w.root);other=w.call(0x490b00,517,receiver=w.root);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[];probe=d.fixture+0x7400
 for mode,status,relation in itertools.product([1,2],[3,0],['none','refuses-ruler','spouse-actor','spouse-old','sworn-actor','sworn-old','liked-new','liked-old','disliked-new','disliked-actor']):
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(target+0xa0,struct.pack('<i',status));w.u.mem_write(target+0x164,struct.pack('<i',365 if relation=='refuses-ruler' else -1));w.u.mem_write(target+0x60,struct.pack('<ii',365 if relation=='spouse-actor' else 517 if relation=='spouse-old' else -1,365 if relation=='sworn-actor' else 517 if relation=='sworn-old' else -1));w.u.mem_write(actor+0x64,struct.pack('<i',365));w.u.mem_write(other+0x64,struct.pack('<i',517));w.u.mem_write(target+0x6c,struct.pack('<5i',*([365 if relation=='liked-new' else 517 if relation=='liked-old' else -1]+[-1]*4)));w.u.mem_write(target+0x80,struct.pack('<5i',*([365 if relation.startswith('disliked') else -1]+[-1]*4)));w.u.mem_write(probe,struct.pack('<i',-99));before=bytes(w.u.mem_read(0x7200000,0x300000));forced=w.call(0x4af7d0,target,actor,mode,probe,receiver=0x799895c,count=50000000);result=struct.unpack('<i',w.u.mem_read(probe,4))[0];assert before==bytes(w.u.mem_read(0x7200000,0x300000)) and rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(mode=mode,status=status,relation=relation,handled=bool(forced),decision=result));print(mode,status,relation,forced,result,flush=True)
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,rows=rows,fullWorldAndRngPure=True,limits=['Explicit relation fixtures retaining native source identities and original force getters','Serving/ruler status fixtures do not prove succession callback or ordinary menu','No probability/RNG/gate replacement'],completeGoal=False),indent=2)+'\n');print('PASS original field forced gate',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
