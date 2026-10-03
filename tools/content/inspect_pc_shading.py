#!/usr/bin/env python3
"""Record supplied EXE static-model shader and constants; no PC writes.

Instruction-range hashes identify the inspected build. They are provenance,
not a substitute for GPU or source-game reference captures.
"""
import argparse
import json
from pathlib import Path
import struct
from inspect_pc_effect_bindings import EXE_SHA
from pc_resources import sha

ROOT=Path(__file__).resolve().parents[2]


def inspect(installation,output):
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Reinspect original model shader for changed EXE')
    def block(va,size):return exe[va-0x400000:va-0x400000+size]
    shaders=[]
    for name,va in (('static-model',0x77ae68),('static-outline',0x77b008)):
        start=va-0x400000;end=exe.index(b'\0',start)
        raw=exe[start:end];text=raw.decode('ascii')
        if not text.startswith('vs.1.1'):raise ValueError('Source shader text boundary')
        shaders.append(dict(name=name,va=hex(va),bytes=len(raw),sha256=sha(raw),text=text,
            runtime='tools/3d/pc-scenery.mat candidate subset' if name=='static-model' else None))
    constants=[]
    for name,va,register,evidence in (
        ('half',0x794884,'c2','44c7d4/44c7dc'),
        ('one',0x794894,'c4','41b624/41b62c')):
        raw=block(va,16)
        constants.append(dict(name=name,va=hex(va),register=register,
            values=list(struct.unpack('<4f',raw)),sha256=sha(raw),upload=evidence))
    if constants[0]['values']!=[.5]*4 or constants[1]['values']!=[1.]*4:
        raise ValueError('Source shader constants changed')
    functions=[]
    for name,start,end in (
        ('model shader creation',0x41b110,0x41b1c0),
        ('static material render states',0x41b420,0x41b637),
        ('static model and outline draw',0x41b7b0,0x41b95a),
        ('global vertex constants',0x44c7ac,0x44c88e),
        ('direction and ambient uploads',0x44caf6,0x44cb9a),
        ('paint and silhouette loader',0x401fb0,0x402160)):
        raw=block(start,end-start)
        functions.append(dict(name=name,start=hex(start),end_exclusive=hex(end),
            bytes=len(raw),sha256=sha(raw)))
    result=dict(schema=1,goal_complete=False,source_executable='san11pk.exe',
        executable_sha256=EXE_SHA,source_policy='read-only',shaders=shaders,
        constants=constants,functions=functions,
        static_pipeline=dict(color='saturate(2*atlas*vertexColor*c4), then saturate(2*previous*paint)',
            paint_uv='per-vertex max(dot(source world normal,c18),0),c2.y',
            light='44cb44: register18+lightIndex receives matrix column at +0/+10/+20',
            alpha='atlasAlpha*vertexAlpha*c4.w*distanceFade; GREATER0 alpha test; SRCALPHA/INVSRCALPHA blend',
            depth_write=True,source_cull_mode=2,
            lookups={'4806':'media/stage/paint.wft','4807':'media/stage/silhouette.wft'}),
        android_limits=['Distance fade and fog not yet ported','Outline draw not yet ported',
            'Double-sided Android candidate; source culling/winding not yet accepted',
            'Encoded source blending versus linear Filament composition still needs PC reference',
            'Original per-model draw ordering versus streamed chunks still needs comparison',
            'Terrain and skinned-unit shader variants not included','No source-game screenshot/video acceptance'])
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(shaders=len(shaders),constants=len(constants),functions=len(functions),goal_complete=False)))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
    p.add_argument('--output',type=Path,default=ROOT/'docs/pc-visual/shading-source-working.json')
    a=p.parse_args();inspect(a.installation,a.output)
