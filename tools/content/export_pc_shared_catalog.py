#!/usr/bin/env python3
"""Extract bounded source names from executed type24 records, without guessing rule fields."""
import argparse
import json
import hashlib
from pathlib import Path
from inspect_pc_effect_bindings import EXE_SHA

KINDS={'table_7984c':'province','table_79bac':'region','table_79c54':'facility','table_7d054':'equipment',
       'table_7d7d4':'ruler_title','table_7d9dc':'officer_rank','table_80ef8':'skill','table_83928':'technology',
       'table_84858':'tactic','table_850d8':'terrain','table_854d8':'name_character','table_86dd8':'ability_research'}


def export(source,output):
    raw=source.read_bytes();report=json.loads(raw)
    if report['source_executable_sha256']!=EXE_SHA:raise ValueError('Unexpected executable')
    shared=next(s for s in report['sources'] if s['path']=='Media/scenario/Scenario.s11')
    rows=[]
    for record in shared['records']:
        if record['kind'] not in KINDS:
            if record['bytes']:raise ValueError('Unknown type24 nonempty table')
            continue
        reads=record['reads'];first=reads[0]
        if first['destination']!='actor+0x4' or first['bytes']!=1:raise ValueError('Unknown name field layout')
        width=0
        for read in reads:
            if read['destination']!='actor+'+hex(4+width) or read['return_address']!=first['return_address'] or read['bytes']!=1:break
            width+=1
        name_raw=bytes.fromhex(record['actor_hex'])[4:4+width].split(b'\0')[0]
        try:name=name_raw.decode('big5');unknown=None
        except UnicodeDecodeError:name=None;unknown='unsupported_source_glyph'
        rows.append(dict(kind=KINDS[record['kind']],table=record['kind'],native_index=record['native_index'],
            name=name,name_raw_hex=name_raw.hex(),name_unknown=unknown,name_width=width,name_read_function_return=first['return_address'],
            source_offset=record['offset'],source_record_bytes=record['bytes'],source_record_sha256=record['sha256'],serializer=record['serializer']))
    if len(rows)!=883:raise ValueError('Incomplete shared catalog')
    result=dict(schema=1,source=dict(path=shared['path'],sha256=shared['sha256'],bytes=shared['bytes']),
        source_executable_sha256=EXE_SHA,executed_report_sha256=hashlib.sha256(raw).hexdigest(),records=rows,
        limits=['Catalog names and IDs only; remaining field meanings not inferred from plausible numbers',
                'Placeholder records remain; table membership does not imply playable or enabled',
                'Names are source Big5; no internet/community replacement or runtime import',
                'Type22 candidates skip these tables; MOD activation remains unverified'])
    output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(records=len(rows),unknown_names=sum(r['name'] is None for r in rows)),ensure_ascii=False))
    return result


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();export(a.source,a.output)
