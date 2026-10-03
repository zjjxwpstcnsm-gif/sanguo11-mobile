#!/usr/bin/env python3
"""Import shared officer ranks through original serializers and named getters.

Legacy project IDs/order are an explicit checked bridge, never native indices.
No scenario activation, title gate or monthly payment rule is inferred here.
"""
import argparse
import csv
import gzip
import hashlib
import io
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_ECX
from inspect_pc_scenario_tail import NativeTailDecoder
from test_pc_city_action_costs import CityActionCostsTest

ROOT = Path(__file__).resolve().parents[2]


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def export(installation, bridge, output, audit):
    installation = installation.resolve()
    for target in (output, audit):
        if target.resolve() == installation or installation in target.resolve().parents:
            raise ValueError('PC installation is read-only')
    exe = (installation/'san11pk.exe').read_bytes()
    path = 'Media/scenario/Scenario.s11'
    shared = (installation/path).read_bytes()
    prior_raw = (ROOT/'docs/pc-data/scenario-tail-native.json.gz').read_bytes()
    prior = json.loads(gzip.decompress(prior_raw))
    if prior['source_executable_sha256'] != sha(exe):
        raise ValueError('Executable provenance changed')
    previous = next(s for s in prior['sources'] if s['path'] == path)
    if previous['sha256'] != sha(shared):
        raise ValueError('Shared source provenance changed')
    source_rows = [r for r in previous['records'] if r['kind'] == 'table_7d9dc']
    bridge_raw = bridge.read_bytes()
    mapping = list(csv.DictReader(io.StringIO(bridge_raw.decode()), delimiter='\t'))
    if [int(r['native_id']) for r in mapping] != list(range(81)):
        raise ValueError('Bridge must cover every native rank once, in native order')
    if sorted(int(r['legacy_order']) for r in mapping[:80]) != list(range(80)):
        raise ValueError('Bridge must preserve every legacy position')
    if len({r['project_id'] for r in mapping[:80]}) != 80 or mapping[80]['project_id'] != '-':
        raise ValueError('Duplicate/invalid project identity bridge')
    decoder = NativeTailDecoder(exe)
    current = [r for r in decoder.decode_tail(shared, True)['records'] if r['kind'] == 'table_7d9dc']
    if len(current) != 81 or any(a['raw_hex'] != b['raw_hex'] or a['actor_hex'] != b['actor_hex'] for a,b in zip(current,source_rows)):
        raise ValueError('Original serializer no longer reproduces prior ranks')
    test = CityActionCostsTest()
    test.d = decoder
    test.call(0x73cfa0)
    names = {}
    for prop in (3,4,5,6,7,8):
        pointer = struct.unpack('<I', decoder.u.mem_read(0x8ac878+prop*16,4))[0]
        names[prop] = bytes(decoder.u.mem_read(pointer,64)).split(b'\0')[0].decode('big5')
    if names != {3:'指揮',4:'上昇能力',5:'上昇值',6:'俸祿',7:'格',8:'功績'}:
        raise ValueError('Original named property identity changed')
    before = bytes(decoder.u.mem_read(0x7200000,0x300000))
    rng = bytes(decoder.u.mem_read(0x8a5d44,4))

    def no_writes(machine,access,address,size,value,user):
        if not decoder.stack-0x10000 <= address or address+size > decoder.stack+0x10000:
            raise ValueError('Rank getter wrote non-stack memory')

    hook = decoder.u.hook_add(UC_HOOK_MEM_WRITE,no_writes)
    rows = []
    try:
        for source, identity in zip(current,mapping):
            index = source['native_index']
            actor = bytes.fromhex(source['actor_hex'])
            name = actor[4:13].split(b'\0')[0].decode('big5')
            if name != identity['native_name']:
                raise ValueError('Native name differs from explicit bridge')
            pointer = decoder.root+0x7d9dc+index*0x3c
            values = [test.call(0x4cb550,pointer,prop) for prop in (3,4,5,6,7,8)]
            if values[:5] != [struct.unpack_from('<H',actor,0x2c)[0],struct.unpack_from('<i',actor,0x30)[0],*actor[0x34:0x37]]:
                raise ValueError('Native getter/source fields differ')
            for stat in range(5):
                decoder.u.reg_write(UC_X86_REG_ECX,pointer)
                if test.call(0x48e1c0,stat) & 255 != (values[2] if values[1]==stat else 0):
                    raise ValueError('Ability bonus getter differs')
            rows.append([index,identity['project_id'],identity['legacy_order'],identity['civilian'],name,*values,source['offset'],source['sha256']])
    finally:
        decoder.u.hook_del(hook)
    if before != bytes(decoder.u.mem_read(0x7200000,0x300000)) or rng != bytes(decoder.u.mem_read(0x8a5d44,4)):
        raise ValueError('Rank access changed registry/RNG')
    skipped = []
    for source in prior['sources']:
        if source['path'] == path:
            continue
        if sha((installation/source['path']).read_bytes()) != source['sha256']:
            raise ValueError('Candidate source changed')
        rank_rows = [r for r in source['records'] if r['kind']=='table_7d9dc']
        if len(rank_rows)!=81 or any(r['bytes'] or r['reads'] for r in rank_rows):
            raise ValueError('Candidate unexpectedly contains a rank override')
        skipped.append(dict(path=source['path'],sha256=source['sha256'],rank_payload_bytes=0))
    buf = io.StringIO()
    buf.write('# pc-officer-ranks-v1\n# source='+path+'\n# source_sha256='+sha(shared)+'\n')
    writer = csv.writer(buf,delimiter='\t',lineterminator='\n')
    writer.writerow(['native_id','project_id','legacy_order','civilian','native_name','command','ability_stat','ability_bonus','salary','grade','merit','source_offset','record_sha256'])
    writer.writerows(rows)
    output.write_bytes(buf.getvalue().encode())
    report = dict(schema=1,source_path=path,source_sha256=sha(shared),executable_sha256=sha(exe),
                  prior_sha256=sha(prior_raw),bridge_sha256=sha(bridge_raw),output_sha256=sha(output.read_bytes()),
                  rows=81,legacy_ids=80,properties=names,source_field_offsets=dict(command=[40,41],ability_stat=[42],ability_bonus=[43],salary=[44],grade=[45]),
                  functions=[dict(address=hex(a),end_exclusive=hex(b),sha256=sha(exe[a-0x400000:b-0x400000])) for a,b in [(0x48e2e0,0x48e35c),(0x73cfa0,0x73d04f),(0x4cb550,0x4cb726),(0x48e1c0,0x48e1d5)]],
                  getter_calls=81*11,non_stack_writes=0,registry_and_rng_unchanged=True,candidate_rank_payloads=skipped,
                  records=[dict(native_id=r[0],native_name=r[4],source_offset=r[-2],record_sha256=r[-1]) for r in rows],
                  limits=['Shared24 branch serializes ranks; all16 audited22 branches consume no rank bytes',
                          'Installed candidate/MOD activation still unresolved; presence is not activation',
                          'Legacy labels/order/civilian grouping retained by explicit bridge',
                          'Required ruler title, appointment admission and actual salary settlement order remain separate rule evidence',
                          'Rank80 unassigned has native salary5; existing project unassigned payroll policy is not changed by this import'])
    audit.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(rows=81,output_sha256=report['output_sha256'],properties=names),ensure_ascii=False))


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('installation','bridge','output','audit'):
        parser.add_argument('--'+name,type=Path,required=True)
    args=parser.parse_args()
    export(args.installation,args.bridge,args.output,args.audit)
