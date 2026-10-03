#!/usr/bin/env python3
"""Retain only the exact EXE pages required by the existing effect oracle.

No source instruction changes, new assets or game rule access. This is not an
Android effect integration. Outputs remain investigation artifacts by default.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct

EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'


def pack(installation,output):
    exe=(installation/'san11pk.exe').read_bytes()
    if len(exe)!=159920128 or hashlib.sha256(exe).hexdigest()!=EXE_SHA:
        raise ValueError('Reinspect source EXE before exporting executable pages')
    prefix=exe[:0x500000];tail=exe[0x9800000:0x9900000]
    if len(tail)!=536576:raise ValueError('Native oracle actual EXE tail')
    header=b'PCVMEX01'+struct.pack('<4I',len(exe),len(prefix),0x9800000,len(tail))+bytes.fromhex(EXE_SHA)+bytes(8)
    data=header+prefix+tail
    if len(data)!=5779520:raise ValueError('Native fragment exact boundary')
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(data)
    def digest(b):return hashlib.sha256(b).hexdigest()
    report=dict(schema=1,goal_complete=False,status='SOURCE_CODE_PAGES_ONLY_NOT_ANDROID_ACCEPTANCE',
        source_file=str(installation/'san11pk.exe'),source_sha256=EXE_SHA,
        output=str(output),bytes=len(data),sha256=digest(data),
        segments=[dict(source_offset=0,bytes=len(prefix),mapped_address='400000',sha256=digest(prefix)),
            dict(source_offset=0x9800000,bytes=len(tail),mapped_address='9c00000',sha256=digest(tail))],
        source_instruction_changes=0,runtime_effects_added=0,
        limits=['Exact same source pages as Python oracle; remaining mapped tail is zero as before',
            'Fragment metadata is a source record; caller must pin/check whole output SHA before execution',
            'No rules/save/game RNG bridge, Android rendering or PC comparison'])
    output.with_suffix(output.suffix+'.json').write_text(json.dumps(report,indent=2)+'\n')
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
    p.add_argument('--output',type=Path,default=Path('out/pc-visual/native-effect-feasibility/source-kernel.bin'))
    a=p.parse_args();print(json.dumps(pack(a.installation,a.output)))
