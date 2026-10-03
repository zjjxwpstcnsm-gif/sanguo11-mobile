#!/usr/bin/env python3
"""Export bounded, source-verified city command AP constants; PC install remains read-only."""
import argparse,hashlib,json,struct
from pathlib import Path
from inspect_pc_effect_bindings import EXE_SHA

DEBITS={'RECRUIT':(1,'徵兵',0x5c3c9c),'PATROL':(3,'巡察',0x5cbf8b),'PRODUCTION':(2,'生產',0x5c67a9),'PRODUCTION_ALTERNATE':(2,'生產',0x5c711b),'TRADE':(4,'商人',0x5cad11)}
def export(exe,java,audit):
 for output in (java,audit):
  if output.resolve()==exe.parent.resolve() or exe.parent.resolve() in output.resolve().parents:raise ValueError("Output cannot be inside read-only PC installation")
 raw=exe.read_bytes()
 if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Unverified executable')
 facilities=json.loads((Path(__file__).resolve().parents[2]/'docs/pc-data/facility-costs-native.json').read_text())
 shared_path='Media/scenario/Scenario.s11';shared=(exe.parent/shared_path).read_bytes()
 if facilities['source_path']!=shared_path or hashlib.sha256(shared).hexdigest()!=facilities['source_sha256']:raise ValueError('Shared facility source changed')
 office=next(r for r in facilities['records'] if r['native_index']==41)
 if office['native_name']!='軍事府':raise ValueError('Unverified facility41 identity')

 def code(at,n):return raw[at-0x400000:at-0x400000+n]
 entries=[]
 for operation,(index,label,at) in DEBITS.items():
  pointer=struct.unpack('<I',code(0x8af2c0+index*4,4))[0]
  if code(pointer,32).split(b'\0',1)[0].decode('big5')!=label:raise ValueError('Command label changed')
  if code(at,4)!=bytes([0x6a,20,0x6a,index]):raise ValueError('Debit argument changed')
  entries.append(dict(operation=operation,native_id=index,native_name=label,action_points=20,instruction_address=hex(at),instruction_hex=code(at,4).hex()))
 expected='f7d81bc083e0f683c014'
 if code(0x5c440f,10).hex()!=expected:raise ValueError('Training arithmetic changed')
 if code(0x5c43fb,2)!=b'\x6a\x29' or code(0x5c441c,2)!=b'\x6a\x06':raise ValueError('Training facility/command changed')
 java.write_text('''package game.sanguo.core;
/** Generated from the pinned EXE by export_pc_city_action_costs.py.
 * Original native tests execute the real district debit, not a Java replica.
 * Facility41 discount is recorded but its facility is not yet represented. */
final class PcCityActionCosts {
    private PcCityActionCosts() {}
    static final int PATROL=20, RECRUIT=20, TRAIN=20, PRODUCTION=20, TRADE=20;
    static final int TRAIN_WITH_MILITARY_OFFICE=10;
}
''')
 audit.write_text(json.dumps(dict(schema=1,executable_sha256=EXE_SHA,shared_source_path=shared_path,shared_source_sha256=facilities['source_sha256'],military_office_record_sha256=office['record_sha256'],commands=entries,training=dict(native_id=6,native_name='訓練',base=20,discounted=10,facility_native_id=41,facility_name='軍事府',condition='Original49dab0 finds city+f4 registry object with +8=41 and nonzero +14',arithmetic_address='0x5c440f',arithmetic_hex=expected),evidence=['48f1a0..48f1b0 original command label lookup','巡察 dialog60ad60/60a7b0 calls5cba10, same calculation in execution5cbd90','徵兵 dialog604f50/604850 calls5c3610, same calculation in execution5c3a50','訓練 dialog605860 calls5c4080; execution5c43ca checks native facility41 before debit','test_pc_city_action_costs.py: original5b9340 ->4a1820 ->47e3e0, 60 AP boundary cases compare all3MiB; both production debit entries and merchant included'],limits=['City formulas, costs other than AP, crew count, facility construction and district scope not fully aligned','Project currently has no military-office facility, so training uses base20','Nonzero field14 tested as original predicate; broader field semantics not asserted','No evidence optional MOD runtime patches are enabled']),ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for key in ('exe','java','audit'):p.add_argument('--'+key,required=True,type=Path)
 a=p.parse_args();export(a.exe,a.java,a.audit)
