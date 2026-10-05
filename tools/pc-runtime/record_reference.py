#!/usr/bin/env python3
"""Record timestamped original Present readbacks, without generated frames.

This samples the real source GPU at a bounded rate. Observer synchronization
affects performance; sparse samples cannot certify every animation frame.
"""
import argparse,datetime,hashlib,json,re,struct,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]

def record(label,seconds,hz,destination=None):
    if not re.fullmatch(r'[A-Za-z0-9_-]+',label):raise ValueError('Evidence label')
    if not 1<=seconds<=60 or not .5<=hz<=10:raise ValueError('Recording bounds')
    copy=ROOT/'out/pc-visual/pc-runtime-copy';dest=(destination or ROOT/'out/pc-visual/v152/pc-reference')/label
    dest.mkdir(parents=True,exist_ok=False)
    source=copy/'pc-source-frame.bmp';request=copy/'pc-capture.request'
    frames=[];start=time.monotonic();utc=datetime.datetime.now(datetime.timezone.utc).isoformat();failure=None
    try:
        while time.monotonic()-start<seconds:
            before=source.stat().st_mtime_ns;requested=time.monotonic()-start
            request.write_text(label+'\n');deadline=time.monotonic()+15
            while request.exists() or source.stat().st_mtime_ns<=before:
                if time.monotonic()>deadline:raise TimeoutError('No original source Present readback within 15s')
                time.sleep(.01)
            completed=time.monotonic()-start;data=source.read_bytes()
            if len(data)<54 or data[:2]!=b'BM':raise ValueError('Invalid actual frame')
            width,height=struct.unpack_from('<ii',data,18)
            if height>=0 or len(data)!=54+width*(-height)*4:raise ValueError('Unexpected readback layout')
            filename='frame-'+str(len(frames)).zfill(4)+'.bmp';(dest/filename).write_bytes(data)
            log=(copy/'pc-readback-observer.log').read_text();matches=re.findall(r'Original frame readback (0x[0-9a-f]+)',log)
            frames.append(dict(file=filename,sha256=hashlib.sha256(data).hexdigest(),bytes=len(data),width=width,height=-height,
                               request_seconds=requested,time_seconds=completed,source_present=int(matches[-1],16) if matches else None))
            remaining=requested+1/hz-(time.monotonic()-start)
            if remaining>0:time.sleep(remaining)
    except Exception as exc:
        failure=str(exc)
    report=dict(status='SOURCE_GPU_SEQUENCE_RECORDED' if failure is None else 'SOURCE_GPU_SEQUENCE_PARTIAL',goal_complete=False,
                utc=utc,requested_seconds=seconds,requested_hz=hz,frames=frames,failure=failure,
                source='unmodified supplied source rendering, private Wine clone, D3D9 Present observer',
                limits=['GPU readback adds synchronization and file IO; no PC baseline FPS acceptance',
                        'Timestamps bracket request/readback completion on host monotonic clock',
                        'Sparse sampling is retained explicitly; complete animation sequence acceptance requires adequate coverage',
                        'No Android matching or full visual acceptance inferred'])
    (dest/'frames.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    (dest/'observer.log').write_text((copy/'pc-readback-observer.log').read_text())
    print(json.dumps(dict(directory=str(dest.resolve()),frames=len(frames),failure=failure,elapsed=time.monotonic()-start)))
    if failure:raise RuntimeError(failure)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('label');p.add_argument('--seconds',type=float,default=15);p.add_argument('--hz',type=float,default=2);p.add_argument('--destination',type=Path)
    a=p.parse_args();record(a.label,a.seconds,a.hz,a.destination)
