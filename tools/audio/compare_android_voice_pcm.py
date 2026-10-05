#!/usr/bin/env python3
"""Measure retained Android voice PCM against a separately retained reference.

Only declared start/end overlap diagnostics are computed. Samples are never
rewritten, padded or trimmed, and diagnostic correlation is not PC playback
acceptance. The actual bytes must match this run's captured codec report.
"""
import argparse
import hashlib
import json
from pathlib import Path
import wave
import numpy as np


def compare(actual,reference,report,resource,output):
    if output.exists():raise ValueError('Fresh diagnostic output required')
    capture=json.loads(report.read_text())
    if not capture['result'].startswith('VOICE_SOURCE PASS'):raise ValueError('Actual successful codec capture required')
    rows=[r for r in capture['decoded'] if r['resourceId']==resource]
    if len(rows)!=1:raise ValueError('Exactly one actual resource row required')
    row=rows[0];raw=actual.read_bytes()
    if hashlib.sha256(raw).hexdigest()!=row['actualPcmSha256'] or len(raw)!=row['actualBytes']:
        raise ValueError('Actual PCM does not match this run')
    with wave.open(str(reference)) as f:
        if f.getsampwidth()!=2 or f.getframerate()!=row['sampleRate'] or f.getnchannels()!=row['channels']:
            raise ValueError('Reference format differs')
        reference_raw=f.readframes(f.getnframes())
    if hashlib.sha256(reference_raw).hexdigest()!=row['referencePcmSha256']:
        raise ValueError('Reference PCM hash differs')
    channels=row['channels'];a=np.frombuffer(raw,dtype='<i2').astype(np.int32).reshape(-1,channels)
    b=np.frombuffer(reference_raw,dtype='<i2').astype(np.int32).reshape(-1,channels)
    difference=len(a)-len(b);overlap=min(len(a),len(b))
    positions=[(0,0)]
    if difference>0:positions.append((difference,0))
    elif difference<0:positions.append((0,-difference))
    diagnostics=[]
    for ao,bo in positions:
        x=a[ao:ao+overlap].reshape(-1);y=b[bo:bo+overlap].reshape(-1);delta=x-y
        values,counts=np.unique(delta,return_counts=True);norm=np.linalg.norm(x)*np.linalg.norm(y)
        diagnostics.append(dict(androidOffsetFrames=ao,referenceOffsetFrames=bo,overlapFrames=overlap,
                                correlation=float(x.astype(float)@y/norm) if norm else None,
                                maxAbsolutePcmDifference=int(np.abs(delta).max()),
                                differenceHistogram={str(v):int(n) for v,n in zip(values,counts)} if len(values)<15 else dict(distinctDifferences=len(values))))
    tail=a[overlap:] if difference>0 else b[overlap:]
    result=dict(resourceId=resource,status='MEASURED_ONLY',scope='Unmodified actual Android bytes vs original-bitstream PyAV reference; no native PCM or speaker assertion',
                sourceEndGranuleFrames=row['sourceEndGranuleFrames'],actualFrames=len(a),referenceFrames=len(b),
                actualSha256=hashlib.sha256(raw).hexdigest(),referenceSha256=hashlib.sha256(reference_raw).hexdigest(),
                unmatchedTailFrames=abs(difference),unmatchedTailNonzeroSamples=int(np.count_nonzero(tail)),
                unmatchedTailPeakS16=int(np.abs(tail).max()) if tail.size else 0,diagnostics=diagnostics)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('actual',type=Path);p.add_argument('reference',type=Path)
    p.add_argument('--report',type=Path,required=True);p.add_argument('--resource',type=int,required=True);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();compare(a.actual,a.reference,a.report,a.resource,a.output)
