#!/usr/bin/env python3
"""Export pinned original merchant initialization table and price/RNG evidence."""
import argparse,hashlib,json,struct
from pathlib import Path
from inspect_pc_effect_bindings import EXE_SHA
def inspect(exe):
 raw=exe.read_bytes()
 if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Unverified executable')
 data=raw[0x3e84b8:0x3e84b8+144];rows=[]
 for i in range(12):
  record=data[i*12:(i+1)*12];base,step,bound=struct.unpack('<iii',record)
  rows.append(dict(month=i+1,source_address=hex(0x7e84b8+i*12),base=base,step=step,random_bound=bound,record_sha256=hashlib.sha256(record).hexdigest()))
 functions=[]
 for name,start,end in [('price_update',0x4b3c60,0x4b3dff),('uniform_rng',0x472150,0x472184),('percent_rng',0x4721d0,0x472208),('city_condition_mask',0x47b420,0x47b42d)]:
  functions.append(dict(name=name,address=hex(start),end_exclusive=hex(end),sha256=hashlib.sha256(raw[start-0x400000:end-0x400000]).hexdigest()))
 conditions=[]
 for bit in range(3):
  property_id=0x9a+bit;table_address=0x8af628+property_id*4;pointer=struct.unpack_from('<I',raw,table_address-0x400000)[0];label=raw[pointer-0x400000:pointer-0x400000+32].split(b'\0')[0]
  conditions.append(dict(bit=bit,property_id=hex(property_id),name=label.decode('big5'),label_table_entry=hex(table_address),label_address=hex(pointer),label_sha256=hashlib.sha256(label).hexdigest(),name_branch='4c09ef..4c0a1f',set_branch='4a3abd..4a3add -> 47b550',get_branch='4c0cd3..4c0cf0 -> 47b3c0'))
 return dict(schema=2,source_path='san11pk.exe',executable_sha256=EXE_SHA,table_sha256=hashlib.sha256(data).hexdigest(),months=rows,functions=functions,conditions=conditions,
  state=dict(rate='city+7c unsigned byte',conditions='city+9c bit0 plague, bit1 locust, bit2 harvest; masks3 then4, disaster takes precedence',rng_address='8a5d44',rng_update='(state*0x6c078965+0x3039) modulo2^32'),
  calls=dict(initial='4a11f0 city initialization -> 4a1278 -> 4b3c60(initial=1)',monthly='590c30 day-of-month1 guard; per-city58d6d0 -> 58d6ed -> 4b3c60(initial=0)'),
  verification=dict(initial_valid_cases=192,invalid_month_cases=32,monthly_cases=2816,rng_edge_cases=85,named_properties=3,property_set_get_cases=48,method='Unhooked original functions and RNG, exact final RNG and3MiB world mutation comparison; original property label/set/get branches'),
  limits=['Not yet applied to project: requires saved price state, current politics modifiers and verified event timing','Monthly outer call chain statically inspected, not entire global settlement emulated','Raw scenario7c=50 is overwritten at initialization; cannot assume fixed starting price','Project RNG remains its existing saved64bit algorithm; no claim of identical native random stream'])
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--exe',required=True,type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
 if a.output.resolve()==a.exe.parent.resolve() or a.exe.parent.resolve() in a.output.resolve().parents:raise ValueError('Read-only PC installation')
 a.output.write_text(json.dumps(inspect(a.exe),indent=2)+'\n')
