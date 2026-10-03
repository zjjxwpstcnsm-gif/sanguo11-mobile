#!/usr/bin/env python3
"""Independent raw source platform-pose reference; not a runtime asset readback."""
import argparse,struct
from pathlib import Path
import numpy as np
from pc_resources import Archive,sha
from import_pc_units import EXE_SHA
from pc_unit_formats import wkmd,fcvd,nested_link,pose

def generate(installation,output):
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA or struct.unpack_from('<2I',exe,0x44926c)!=(73,74):raise ValueError('Source platform EXE binding differs')
    a=Archive(installation/'Media/san11pkres.bin')
    try:
        motions=nested_link(a.read(2236));data=bytearray(b'PCFRREF1'+struct.pack('<I',12))
        for state in range(2):
            m=wkmd(a.read(2234+state));c=fcvd(motions[73+state])
            for f in(0,.125,.33333334,.5,.875,1):
                frame=float(np.float32(f*(c['frames']-1)));p,n=pose(m,c,frame)
                p=(p*np.array([.05,.05,.05],dtype='<f4')).astype('<f4')
                data+=struct.pack('<IfIII',state,frame,len(p),len(m['indices']),sum(r['triangles']*3 for r in m['draws']if not r['alpha_pass']))
                data+=p.tobytes()+m['attributes'][:,8:12].astype('<f4').tobytes()+m['attributes'][:,6:8].astype('<f4').tobytes()+m['indices'].tobytes()
        output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(data);print('PC platform raw-source references: cases=12 bytes=%d sha256=%s'%(len(data),sha(data)))
    finally:a.close()
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('output',type=Path);a=p.parse_args();generate(a.installation,a.output)
