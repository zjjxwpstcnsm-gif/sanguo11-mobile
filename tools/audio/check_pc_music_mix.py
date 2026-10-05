#!/usr/bin/env python3
"""Recognize a complete original music waveform in actual device PCM; no play-return acceptance."""
import argparse
import hashlib
import json
from pathlib import Path
import wave
import numpy as np

def read(path):
    with wave.open(str(path)) as source:
        if source.getsampwidth()!=2:raise ValueError('Expected measured PCM16')
        return source.getframerate(),np.frombuffer(source.readframes(source.getnframes()),dtype='<i2').astype(np.float64).reshape(-1,source.getnchannels()).mean(1)/32768
def check(capture,reference,resource,manifest):
    entry=next(row for row in json.loads(manifest.read_text())['entries'] if row['resourceId']==resource)
    if hashlib.sha256(reference.read_bytes()).hexdigest()!=entry['wavSha256']:raise ValueError('Original music reference changed')
    rate,signal=read(capture);original_rate,template=read(reference)
    if rate!=original_rate or len(signal)<len(template):raise ValueError('Unexamined sampling/insufficient interval')
    taps=np.arange(-32,33);kernel=np.sinc(taps*.1)*np.hamming(len(taps));kernel/=kernel.sum()
    wide_signal=np.convolve(signal,kernel,mode='same');wide_template=np.convolve(template,kernel,mode='same')
    signal=wide_signal[::8];template=wide_template[::8]
    size=1<<(len(signal)+len(template)-2).bit_length()
    cross=np.fft.irfft(np.fft.rfft(signal,size)*np.fft.rfft(template[::-1],size),size)[len(template)-1:len(signal)]
    squares=np.r_[0,np.cumsum(signal*signal)];energy=squares[len(template):]-squares[:-len(template)]
    scores=np.abs(cross)/np.sqrt(np.maximum(1e-20,energy*(template@template)));coarse=int(np.argmax(scores))*8
    original_energy=float(wide_template@wide_template);prefix=np.r_[0,np.cumsum(wide_signal*wide_signal)];at=coarse;score=0
    for position in range(max(0,coarse-64),min(len(wide_signal)-len(wide_template),coarse+64)+1):
        window=wide_signal[position:position+len(wide_template)];current=abs(float(window@wide_template))/np.sqrt(max(1e-20,(prefix[position+len(wide_template)]-prefix[position])*original_energy))
        if current>score:at=position;score=current
    if score<.995:raise AssertionError(('Original complete music waveform not found in actual PCM',score))
    return dict(status='PASS',resourceId=resource,scope='Original complete music waveform in actual emulator mix; no original scene binding/ARM speaker claim',
        sampleRate=rate,correlation=float(score),seconds=at/rate,captureSha256=hashlib.sha256(capture.read_bytes()).hexdigest(),originalReferenceSha256=entry['wavSha256'],originalOggSha256=entry['oggSha256'])
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('capture',type=Path);p.add_argument('reference',type=Path);p.add_argument('--resource',type=int,required=True);p.add_argument('--manifest',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();report=check(a.capture,a.reference,a.resource,a.manifest);a.output.write_text(json.dumps(report,indent=2)+'\n');print('PASS original music actual PCM correlation=',report['correlation'])
