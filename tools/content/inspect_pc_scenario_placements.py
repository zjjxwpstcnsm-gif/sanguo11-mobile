#!/usr/bin/env python3
"""Audit officer authority through native district/force getters, preserving raw status/location."""
import argparse
import gzip
import json
import struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_EIP
from inspect_pc_scenario_units import NativeScenarioUnitDecoder
from inspect_pc_scenario_officers import ROOT, sha


def native_labels(decoder):
    u=decoder.u
    before=bytes(u.mem_read(0x7200000,0x300000))
    # Direct source UI consumer4c9c11 loads officer+a0 before48ea10.
    if bytes(u.mem_read(0x4c9c11,18)).hex()!='8b96a0000000528d44241450e8ee4dfcff83':
        raise ValueError('Status UI consumer changed')
    result={}
    for name,start,stop,count in [('status',0x48ea20,0x48ea30,9),('ability',0x48ea80,0x48ea90,5)]:
        entries=[]
        for index in range(count):
            u.reg_write(UC_X86_REG_EAX,index)
            u.emu_start(start,stop,count=20)
            if u.reg_read(UC_X86_REG_EIP)!=stop:
                raise ValueError('Native label lookup boundary changed')
            pointer=u.reg_read(UC_X86_REG_EAX)
            raw=bytes(u.mem_read(pointer,32)).split(b'\0',1)[0]
            entries.append(dict(native_index=index,name=raw.decode('big5'),pointer=hex(pointer),raw_hex=raw.hex()))
        result[name]=dict(lookup=[hex(start),hex(stop)],entries=entries)
    if before!=bytes(u.mem_read(0x7200000,0x300000)):
        raise ValueError('Native label lookup changed game state')
    return result


def placements(decoder):
    u=decoder.u
    before=bytes(u.mem_read(0x7200000,0x300000))
    rows=[]
    for record in decoder.records[:850]:
        actor=record['actor_address']
        if record['kind']!='officer' or record['bytes']!=152:
            raise ValueError('Original source officer table missing')
        raw=bytes(u.mem_read(actor,0x190))
        rows.append(dict(native_index=record['native_index'],offset=record['offset'],bytes=record['bytes'],sha256=record['sha256'],
                         district_native_index=decoder.scalar(0x4883b0,actor),
                         force_native_index=decoder.scalar(0x47b2b0,actor),
                         location_field_9c=struct.unpack_from('<i',raw,0x9c)[0],
                         status_field_a0=struct.unpack_from('<i',raw,0xa0)[0]))
    if before!=bytes(u.mem_read(0x7200000,0x300000)):
        raise ValueError('Original ownership getters mutated world/RNG')
    return rows


def audit(installation,output):
    installation,output=installation.resolve(),output.resolve()
    if output==installation or installation in output.parents:
        raise ValueError('Output must be outside read-only PC installation')
    identity_path=ROOT/'docs/pc-data/scenario-officers-native.json.gz'
    identity_bytes=identity_path.read_bytes()
    identity=json.loads(gzip.decompress(identity_bytes))
    mapping={(r['source'],r['native_index']):r for r in identity['mappings']}
    records={(r['source'],r['native_index']):r for r in identity['records']}
    exe=(installation/'san11pk.exe').read_bytes()
    decoder=NativeScenarioUnitDecoder(exe)
    labels=native_labels(decoder)
    status_names={r['native_index']:r['name'] for r in labels['status']['entries']}
    sources=[]
    for source in identity['sources']:
        path=installation/source['path']
        raw=path.read_bytes()
        if sha(raw)!=source['sha256']:
            raise ValueError('Source differs from pinned identity audit: '+source['path'])
        decoder.decode_units(raw)
        rows=placements(decoder)
        for row in rows:
            key=(source['path'],row['native_index'])
            if row['sha256']!=records[key]['sha256']:
                raise ValueError('Original serializers disagree on source record')
            row['project_id']=mapping[key]['project_id']
            row['identity']=mapping[key]['identity']
            row['native_status_label']=status_names.get(row['status_field_a0'])
        sources.append(dict(path=source['path'],sha256=source['sha256'],records=rows))
        print(json.dumps(dict(path=source['path'],records=len(rows),assigned_force=sum(r['force_native_index']>=0 for r in rows))),flush=True)
    report=dict(schema=2,executable_sha256=sha(exe),identity_audit_sha256=sha(identity_bytes),labels=labels,sources=sources,
                evidence=['Original1100-object officer loop493812..49383f reads850 records; no identity replacement hook',
                          '4883b0 reads actor94; original47b2b0 resolves district490ad0 then native force getter',
                          'Each source record SHA matches the previous name/birth/sex mapping; unresolved project identities remain null',
                          '4c9c11 loads officer+a0 before48ea10; original48ea20 label lookup preserves exact abbreviations',
                          'All ownership getters preserve complete3MiB world state'],
                limits=['Raw9c location has no inferred site label; native status abbreviations are retained without expanded appearance semantics',
                        'Registry ownership is not proof an officer is active, selectable or already appeared',
                        'MOD activation and full scenario-start processing remain unresolved; no existing project data overwritten'])
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    return report


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation',type=Path)
    p.add_argument('--output',type=Path,required=True)
    a=p.parse_args()
    audit(a.installation,a.output)
