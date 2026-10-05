#!/usr/bin/env python3
"""Measure actual Android PCM against the retained reference. Never rewrite samples or infer PC codec parity."""
import argparse
import hashlib
import json
from pathlib import Path
import wave
import numpy as np

def compare(actual,reference,resource):
    raw=actual.read_bytes()
    with wave.open(str(reference)) as source:
        if source.getsampwidth()!=2:raise ValueError('Reference must be PCM16')
        rate=source.getframerate();channels=source.getnchannels();reference_bytes=source.readframes(source.getnframes())
    if len(raw)!=len(reference_bytes):raise ValueError('Frame extents differ; alignment/trim not guessed')
    a=np.frombuffer(raw,dtype='<i2').astype(np.int32);b=np.frombuffer(reference_bytes,dtype='<i2').astype(np.int32);delta=a-b
    values,counts=np.unique(delta,return_counts=True);af=a.astype(np.float64);bf=b.astype(np.float64)
    return dict(status='MEASURED',resourceId=resource,
        scope='This one actual Android decoder output vs deterministic PyAV reference; no PC native decoder/speaker assertion',
        sampleRate=rate,channels=channels,frames=len(a)//channels,actualSha256=hashlib.sha256(raw).hexdigest(),referenceSha256=hashlib.sha256(reference_bytes).hexdigest(),
        maxAbsolutePcmDifference=int(np.abs(delta).max()),differenceHistogram={str(value):int(count) for value,count in zip(values,counts)},
        changedSamples=int(np.count_nonzero(delta)),meanDifference=float(delta.mean()),rmsDifference=float(np.sqrt(np.mean(delta.astype(np.float64)**2))),
        correlation=float(af@bf/(np.linalg.norm(af)*np.linalg.norm(bf))))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('actual',type=Path);p.add_argument('reference',type=Path);p.add_argument('--resource',type=int,required=True);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();report=compare(a.actual,a.reference,a.resource);a.output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
