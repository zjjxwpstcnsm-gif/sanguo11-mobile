#!/usr/bin/env python3
"""Compare actual source ground vertex buffers with K3ST quantized normals.

Identify terrain by its bound source vertex shader, not stride alone: sky,
water and UI also use 24-byte vertices with different declarations.
"""
import argparse, json, struct
from pathlib import Path
import numpy as np
from pc_resources import Archive, sha
from inspect_pc_live_draw import inspect_draw_state

GROUND_SHADERS = {
    'e549cd2d0623d4332efbdf1883345871b7bd16d616ad3ff8c6ed7f1f4181eddc': 'base-color',
    'd51cee117cfbcaadaa2a1acf4c2031521750ef5985e0b789e07a6c67b6c081d9': 'painting',
    '7cec5ac0ddeb335ec0be19570aa788f966b17ea89e35db30e36826782cb38319': 'outline',
}

def check(source, capture, output):
    data=capture.read_bytes()
    magic,version,start,size=struct.unpack_from('<4I',data)
    assert magic==0x31445350 and version==3 and size==8540
    footer,count,end,reserved=struct.unpack_from('<4I',data,len(data)-16)
    assert footer==0x444e4544 and reserved==0 and len(data)==32+count*size
    archive=Archive(source/'Media/san11pkres.bin')
    try: terrain=archive.read(4793)
    finally: archive.close()
    assert terrain[:8]==b'K3ST0006' and len(terrain)==8+1025**2*8+1024**2*8
    rows=[];skipped=[]
    for i in range(count):
        offset=16+i*size;v=struct.unpack_from('<16I',data,offset)
        assert v[:2]==(0x57415244,i)
        shader=inspect_draw_state(data,offset)['vertex_shader']['sha256']
        if shader not in GROUND_SHADERS:
            skipped.append(dict(draw=i,shader_sha256=shader,stride=v[12],reason='bound shader is not an identified source terrain shader'))
            continue
        assert v[2]==4 and v[12]==24 and v[14]==0 and v[15]==0
        prefix=data[offset+4376:offset+4888]
        vertices=[]
        for j in range(min(v[5],len(prefix)//24)):
            x,y,z,nx,ny,nz=struct.unpack_from('<6f',prefix,j*24)
            q,r=round(x/5),round(z/5)
            assert x==q*5 and z==r*5 and 0<=q<=1024 and 0<=r<=1024
            raw=terrain[8+(q*1025+r)*8:16+(q*1025+r)*8]
            assert y==raw[0]*.5, (i,j,'source height')
            # Original SSE float32 multiply, multiply, subtract. Do not normalize.
            decoded=np.float32(np.float32(np.asarray(list(raw[4:7]),dtype=np.float32)*np.float32(1/255))*np.float32(2)-np.float32(1))
            assert decoded.tobytes()==struct.pack('<3f',nx,ny,nz), (i,j,'source quantized normal')
            vertices.append(dict(q=q,r=r,raw_normal=list(raw[4:7]),source_normal=[nx,ny,nz]))
        rows.append(dict(draw=i,pass_type=GROUND_SHADERS[shader],shader_sha256=shader,vertices=vertices))
    assert rows and all(any(row['pass_type']==kind for row in rows) for kind in GROUND_SHADERS.values())
    report=dict(status='PASS_SOURCE_GROUND_NORMAL_PREFIXES',goal_complete=False,
                capture_sha256=sha(data),resource=4793,resource_sha256=sha(terrain),
                draws=len(rows),checked_vertices=sum(len(row['vertices']) for row in rows),
                records=rows,skipped_draws=skipped,
                limits=['Captured stream prefixes only; Android terrain normal/material pipeline still needs original binding',
                        'Earlier stride-only probe failed because sky/water/UI share the stride; failure retained separately'])
    output.write_text(json.dumps(report,indent=2)+'\n')
    print(report['status'],report['draws'],report['checked_vertices'])

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('capture',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();check(a.installation,a.capture,a.output)
