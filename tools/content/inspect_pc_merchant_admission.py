#!/usr/bin/env python3
"""Pin native city-only merchant admission and stock-based quantity calculations."""
import argparse,hashlib,json
from pathlib import Path
from inspect_pc_effect_bindings import EXE_SHA
def inspect(exe):
 raw=exe.read_bytes()
 if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Unverified executable')
 rows=[]
 for name,start,end in [('building_type_equals',0x486660,0x486671),('command_fields_valid',0x5ca450,0x5ca4d4),('maximum_quantity',0x5ca770,0x5ca91c),('city_entry_gate',0x5ca985,0x5ca9c9),('quantity_dialog_setup',0x6095ee,0x6096dc)]:
  rows.append(dict(name=name,address=hex(start),end_exclusive=hex(end),sha256=hashlib.sha256(raw[start-0x400000:end-0x400000]).hexdigest()))
 return dict(schema=1,source_path='san11pk.exe',executable_sha256=EXE_SHA,functions=rows,
  implemented=dict(rule='Merchant building must be a city',native='building type0 and valid native city index0..41',project='World.SiteKind.CITY, not literal project ID0..41; custom city IDs remain valid',failure_code='TRADE_SITE',field='city',legacy='Preserve old gate/port stocks and traded records; reject new trades without mutation'),
  quantity=dict(buy='min(gold*rate*400/((450-currentPolitics)*10),1000000-food), integer division',sell='min((100000-gold)*(450-currentPolitics)*rate/3200,max(0,food-1)), integer division',fields='rate city+7c, currentPolitics officer+173',maximum_result='signed64 minimum with INT_MAX; valid stock cases tested, no inferred lower clamp',validator='nonzero signed food delta and food+delta<=1000000; does not itself check every resource condition',numeric_input='6095ee..6096dc supplies absolute final food bounds to numeric dialog and stores returned integer; no thousand rounding in this path'),
  tests=dict(site_cases=261,capacity_cases=320,capacity_null_cases=2,quantity_validator_cases=60,method='Original native functions, full3MiB world and RNG purity'),
  limits=['Quantity calculations/input/validator evidence is not yet implemented; project still1000..20000 step1000','UI numeric dialog internals not fully emulated, and full merchant command includes separate actor/AP/use gates','Prices/state initialization/environment conditions still require integration','No claim about enabled MOD overlays; pinned offline EXE only'])
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--exe',required=True,type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
 if a.output.resolve()==a.exe.parent.resolve() or a.exe.parent.resolve() in a.output.resolve().parents:raise ValueError('Read-only PC installation')
 a.output.write_text(json.dumps(inspect(a.exe),indent=2)+'\n')
