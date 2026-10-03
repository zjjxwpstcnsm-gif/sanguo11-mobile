#!/usr/bin/env python3
"""Export original city capacities; port/gate and legacy save limits stay separate."""
import argparse,hashlib,json
from pathlib import Path
from inspect_pc_effect_bindings import EXE_SHA

def export(exe,java,audit):
 raw=exe.read_bytes()
 if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Unverified executable')
 for output in (java,audit):
  if output.resolve()==exe.parent.resolve() or exe.parent.resolve() in output.resolve().parents:raise ValueError('Output inside read-only PC installation')
 rows=[]
 for name,entry,address,value in [('GOLD',0x486d30,0x486ddc,100000),('FOOD',0x486ea0,0x486f4c,1000000)]:
  expected=b'\xb8'+value.to_bytes(4,'little')+b'\x5e\xc3';actual=raw[address-0x400000:address-0x400000+7]
  if actual!=expected:raise ValueError('Original city capacity changed')
  rows.append(dict(resource=name,capacity=value,getter=hex(entry),city_branch=hex(address),instruction_hex=actual.hex()))
 java.write_text('''package game.sanguo.core;
/** Generated from the pinned original EXE by export_pc_city_capacities.py.
 * These are gameplay admission caps, not destructive legacy-save migrations. */
final class PcCityCapacities {
    private PcCityCapacities() {}
    static final int GOLD=100000, FOOD=1000000;
}
''')
 audit.write_text(json.dumps(dict(schema=1,source_path='san11pk.exe',executable_sha256=EXE_SHA,records=rows,evidence=['test_pc_merchant_quote executes original resource commit and capacity getters with 3MiB mutation comparisons'],limits=['City building branch only; gates, ports and expansion are separate unresolved rules','Legacy project saves retain up to1000000 gold without truncation; new credits obey the gameplay cap','Original command admission, merchant rate updates and enabled MODs are not inferred']),indent=2)+'\n')

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for name in ('exe','java','audit'):p.add_argument('--'+name,required=True,type=Path)
 a=p.parse_args();export(a.exe,a.java,a.audit)
