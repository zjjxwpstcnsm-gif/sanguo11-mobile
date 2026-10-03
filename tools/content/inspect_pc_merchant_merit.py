#!/usr/bin/env python3
"""Export pinned merchant merit call and original officer property identity."""
import argparse,hashlib,json,struct
from pathlib import Path
from inspect_pc_effect_bindings import EXE_SHA
def inspect(exe):
 raw=exe.read_bytes()
 if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Unverified EXE')
 pointer=struct.unpack_from('<I',raw,0x8ae57c-0x400000)[0]
 label=raw[pointer-0x400000:pointer-0x400000+32].split(b'\0')[0]
 assert label.decode('big5')=='功績'
 functions=[]
 for name,start,end in [('trade_merit_call',0x5cacc8,0x5cacd5),('merit_add',0x4a6d50,0x4a6dad),('merit_set',0x48a7a0,0x48a7c8),('officer_name_init',0x73cb40,0x73cb62),('officer_property_set',0x4a3c9c,0x4a3cab)]:
  functions.append(dict(name=name,address=hex(start),end_exclusive=hex(end),sha256=hashlib.sha256(raw[start-0x400000:end-0x400000]).hexdigest()))
 return dict(schema=1,source_path='san11pk.exe',executable_sha256=EXE_SHA,award=50,cap=60000,
  property=dict(id=24,name=label.decode('big5'),label_pointer=hex(pointer),label_table='8ae57c',descriptor='8ab758 +24*16 =8ab8d8',field='officer+ae uint16',dispatch='4a41f4 ->4a412c ->4a3c9c ->48a7a0'),functions=functions,
  verification='test_pc_merchant_merit.py: original officer name initializer/lookup and12 complete3MiB/RNG award boundaries',
  integration='Campaign.trade and TradePlan effects share PcMerchantRules.meritGain; other modeled city awards unchanged',
  compatibility='Project legacy60001..1000000 remains unchanged and earns no further merchant merit; native raw65535 instead clamps down to60000, an explicit non-destructive compatibility exception',
  limits=['Political experience+5 call also observed but modifiers/level changes not yet integrated','Price/quantity pure arithmetic exists but is not yet gameplay-connected','No claim of full officer ability or rank-rule parity'])
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--exe',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
 if a.output.resolve()==a.exe.parent.resolve() or a.exe.parent.resolve() in a.output.resolve().parents:raise ValueError('Read-only PC directory')
 a.output.write_text(json.dumps(inspect(a.exe),ensure_ascii=False,indent=2)+'\n')
