#!/usr/bin/env python3
"""Generate base construction costs and menu identity from verified installed data."""
import argparse, hashlib, json, struct
from pathlib import Path
from inspect_pc_effect_bindings import EXE_SHA

PROJECT_IDS = {'MARKET':31,'FARM':32,'BARRACKS':33,'SMITH':34,'MINT':38,
               'GRANARY':39,'STABLE':35,'BLACK_MARKET':48,'WORKSHOP':36,
               'SHIPYARD':37,'BRONZE_TERRACE':30}
PROJECT_NAMES = {'MARKET':'市場 Lv1','FARM':'農場 Lv1','BARRACKS':'兵舍 Lv1',
                 'SMITH':'鍛冶廠 Lv1','MINT':'造幣廠','GRANARY':'穀倉','STABLE':'廄舍 Lv1',
                 'BLACK_MARKET':'黑市','WORKSHOP':'工房','SHIPYARD':'造船廠','BRONZE_TERRACE':'銅雀台'}

def export(source,exe,java,audit):
    raw=source.read_bytes();report=json.loads(raw);binary=exe.read_bytes()
    if report['source_executable_sha256']!=EXE_SHA or hashlib.sha256(binary).hexdigest()!=EXE_SHA:
        raise ValueError('Unverified executable')
    shared=next(s for s in report['sources'] if s['path']=='Media/scenario/Scenario.s11')
    shared_raw=(exe.parent/shared['path']).read_bytes()
    if hashlib.sha256(shared_raw).hexdigest()!=shared['sha256']:raise ValueError('Shared source changed')
    rows=[r for r in shared['records'] if r['kind']=='table_79c54']
    if [r['native_index'] for r in rows]!=list(range(64)):raise ValueError('Incomplete facility table')
    # Menu600e15/600e25 and loop6011bf, independently executed by the native tests.
    menu=[struct.unpack_from('<I',binary,0x8ba670-0x400000+20*i)[0] for i in range(20)]
    if menu!=list(range(31,40))+[30]+list(range(40,50)):raise ValueError('Construction menu changed')
    ap_instruction=binary[0x5bc4b7-0x400000:0x5bc4b9-0x400000]
    if ap_instruction!=bytes.fromhex('6a14'):raise ValueError('Construction AP instruction changed')
    costs=[];records=[]
    for row in rows:
        record=bytes.fromhex(row['raw_hex'])
        if hashlib.sha256(record).hexdigest()!=row['sha256'] or shared_raw[row['offset']:row['offset']+len(record)]!=record:
            raise ValueError('Record differs from installed source')
        read=next(x for x in row['reads'] if x['destination']=='actor+0xc4')
        if read['bytes']!=2:raise ValueError('Changed cost field format')
        actor=bytes.fromhex(row['actor_hex']);name=actor[4:0xb4].split(b'\0')[0].decode('big5',errors='strict')
        cost=struct.unpack_from('<H',actor,0xc4)[0]
        offset=read['offset']-row['offset']
        if struct.unpack_from('<H',bytes.fromhex(row['raw_hex']),offset)[0]!=cost:raise ValueError('Source/actor mismatch')
        costs.append(cost);records.append(dict(native_index=row['native_index'],native_name=name,cost=cost,source_offset=read['offset'],record_sha256=row['sha256']))
    for project,native in PROJECT_IDS.items():
        if records[native]['native_name']!=PROJECT_NAMES[project]:raise ValueError('Unverified project identity: '+project)
    java.write_text('''package game.sanguo.core;

/** Generated from the installed Scenario.s11 by export_pc_facility_costs.py.
 * Native construction5bc462..5bc49b debits actor+c4; normal menu600e15 has
 * base facilities only. These are base costs, not battlefield discount rules. */
final class PcFacilityCosts {
    private PcFacilityCosts() {}
    static final int BUILD_ACTION_POINTS=20;
    private static final int[] VALUES={%s};
    static int at(int nativeId) { return VALUES[nativeId]; }
}
''' % ','.join(map(str,costs)))
    audit.write_text(json.dumps(dict(schema=2,action_points=dict(value=20,instruction_address='0x5bc4b7',instruction_hex=ap_instruction.hex(),debit_chain=['5b9340','4a1820','47e3e0'],destination='district+0x2c'),executable_sha256=EXE_SHA,source_path=shared['path'],source_sha256=shared['sha256'],report_sha256=hashlib.sha256(raw).hexdigest(),menu=dict(address='0x8ba670',stride=20,native_ids=menu,sha256=hashlib.sha256(binary[0x8ba670-0x400000:0x8ba670-0x400000+400]).hexdigest()),project_ids=PROJECT_IDS,records=records,evidence=['Original construction5bc4b5 passes20 to district debit5b9340; original4a1820 updates district+2c', 'Original getter5bb2b0 reads facility+c4','Original construction5bc462..5bc49b debits the city resource through native virtual methods','Original menu600e15..600e37 selects base facilities; Lv2/Lv3 are not menu entries'],limits=['No complete construction simulation: progress, durability, district AP ownership, three workers, merging and yield remain separate audit items','Optional external runtime patches are not proven active']),ensure_ascii=False,indent=2)+'\n')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path)
    for name in ('exe','java','audit'):p.add_argument('--'+name,required=True,type=Path)
    a=p.parse_args();export(a.source,a.exe,a.java,a.audit)
