#!/usr/bin/env python3
"""Generate independent raw-PC geometry references for the Android CPU skin evaluator.

Read WKMD and FCVD directly from the installation, not units.pcz. Includes every
source table-bound model/clip pair at endpoints and fractional source keyframes.
Numeric comparison does not substitute for a running PC visual reference.
"""
import argparse
from pathlib import Path
import struct
import numpy as np
from pc_resources import Archive,sha
from pc_unit_formats import nested_link,fcvd,wkmd,pose
from import_pc_units import EXE_SHA


def generate(installation,output):
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('PC source EXE changed')
    models=list(struct.unpack_from('<14H',exe,0x369b94));states=list(struct.iter_unpack('<8B',exe[0x369bb0:0x369bb0+112]))
    pairs=set()
    for kind,row in enumerate(states):
        for state,clip in enumerate(row):
            model=320 if kind==3 and state==2 else 325 if kind==4 and state==3 else models[kind]
            pairs.add((model-320,clip))
    cases=[(m,c,t) for m,c in sorted(pairs) for t in (0,.125,.33333334,.5,.875,1)]
    target=bytearray(b'PCUREF02'+struct.pack('<I',len(cases)))
    a=Archive(installation/'Media/san11pkres.bin')
    try:
        motions=[fcvd(d) for d in nested_link(a.read(2236))];rigs=[wkmd(a.read(2207+i*2)) for i in range(14)]
        for model,clip,f in cases:
            # Frame passed to Android is exactly the stored float32 value.
            frame=float(np.float32(f*(motions[clip]['frames']-1)))
            rig=rigs[model];positions,normals=pose(rig,motions[clip],frame)
            positions=(positions*np.array([.05,.05,.05],dtype='<f4')).astype('<f4')
            target+=struct.pack('<IIfIII',model,clip,frame,len(positions),len(rig['indices']),sum(d['triangles']*3 for d in rig['draws'] if not d['alpha_pass']))
            target+=positions.tobytes()+rig['attributes'][:,8:12].astype('<f4').tobytes()+rig['attributes'][:,6:8].astype('<f4').tobytes()+rig['indices'].tobytes()
        output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(target)
        print('PC unit raw-source references: cases=%d bytes=%d sha256=%s'%(len(cases),len(target),sha(target)))
    finally:a.close()


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('output',type=Path);a=p.parse_args();generate(a.installation,a.output)
