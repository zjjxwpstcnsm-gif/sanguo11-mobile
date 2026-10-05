#!/usr/bin/env python3
"""Decode the owned PC visual descriptor snapshot without assigning officers.

Source480320 returns a signed slot only when descriptor flag2 is set, else39.
The source584ed3 caller adds131. This does not equate face IDs to officer IDs.
"""
import argparse
import hashlib
import json
import struct
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

def inspect(installation,capture,output):
    exe=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(exe).hexdigest()!=EXE_SHA:
        raise ValueError('Reinspect portrait lookup for changed source EXE')
    data=capture.read_bytes()
    if len(data)!=32+2400*4:
        raise ValueError('Incomplete visual descriptor snapshot')
    magic,version,count,stride,pid,table,lookup,actor=struct.unpack_from('<8I',data)
    if (magic,version,count,stride,table,lookup,actor)!=(0x31525450,1,2400,4,0x6fae8b8,0x480320,0x48a5b0) or not pid:
        raise ValueError('Unexpected portrait snapshot contract')
    rows=[]
    for face in range(count):
        raw=data[32+face*4:36+face*4]
        slot=struct.unpack_from('<b',raw,2)[0] if raw[3]&2 else 39
        selector=131+slot
        if not 0<=selector<347:
            raise ValueError('Source portrait selector outside dynamic table')
        template,top=struct.unpack_from('<II',exe,0x3774e0+selector*8)
        if not 0<=template<244:
            raise ValueError('Portrait template outside effect table')
        resource=struct.unpack_from('<I',exe,0x37692c+template*12+4)[0]
        rows.append(dict(face_id=face,descriptor_hex=raw.hex(),flags=raw[3],slot=slot,
            dynamic_selector=selector,texture_resource=369+selector,effect_index=template,
            effect_resource=resource,destination_top=top,officer_binding=None))
    report=dict(schema=1,goal_complete=False,status='LIVE_PC_VISUAL_DESCRIPTORS_READ_OFFICER_BINDINGS_PENDING',
        source_exe_sha256=EXE_SHA,capture=str(capture),capture_sha256=hashlib.sha256(data).hexdigest(),
        process_id=pid,source_table=hex(table),entries=rows,explicit_slot_faces=sum(bool(r['flags']&2)for r in rows),
        limits=['Two equal read-only process reads required by producer; no game state or texture writes',
                'Face IDs are visual descriptor indices, not content/officers.tsv sourceId',
                'Actual actor face selection, normal critical cause and source fullscreen playback still require live evidence',
                'Source template is resolved through actual permuted244-entry table, never125+index'])
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(entries=count,explicit_slot_faces=report['explicit_slot_faces'],
        selectors=sorted({r['dynamic_selector']for r in rows}),capture_sha256=report['capture_sha256'])))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation',type=Path);p.add_argument('capture',type=Path)
    p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();inspect(a.installation,a.capture,a.output)
