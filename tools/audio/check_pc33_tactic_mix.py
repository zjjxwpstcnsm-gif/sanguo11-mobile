#!/usr/bin/env python3
"""Verify declared normal tactic/critical/PC33 superposition in actual PCM.

Candidate correlation is not acceptance: refine exact original-rate positions,
require positive audible independent coefficients, >.999 total reconstruction
and >.999 PC33 correlation after subtracting two declared mobile nuisance cues.
No waveform is edited, no sample library is searched for unrelated identities.
"""
import argparse
import hashlib
import json
from pathlib import Path
import wave
import numpy as np

ROOT=Path(__file__).resolve().parents[2]

def inspect(source,flow_path,facts_path,output):
    if output.exists():raise ValueError('Fresh evidence required')
    flow=json.loads(flow_path.read_text());facts=json.loads(facts_path.read_text())
    if not flow['passed'] or not flow['restoration']['all_original_files_byte_equal'] or not facts['productionHostOnly'] or not facts['facts']:
        raise ValueError('Actual normal host/facts/restoration required')
    entries=[('critical','audio/critical.wav','INHERITED_MOBILE_COMPOSITION'),('tactic','audio/tactic.wav','INHERITED_MOBILE_COMPOSITION'),('pc33','audio/pc/technique-33.wav','ORIGINAL_PC_HUD33')]
    expected=json.loads((ROOT/'app/src/main/assets/audio/pc-media-manifest.json').read_text())['wavSha256']
    if hashlib.sha256((ROOT/'app/src/main/assets'/entries[2][1]).read_bytes()).hexdigest()!=expected:raise ValueError('Original33 asset drift')
    def read(path):
        with wave.open(str(path)) as f:
            if f.getsampwidth()!=2:raise ValueError('Expected actual16-bit PCM')
            return f.getframerate(),np.frombuffer(f.readframes(f.getnframes()),dtype='<i2').astype(float).reshape(-1,f.getnchannels()).mean(1)/32768
    rate,raw=read(source);t=np.arange(-32,33);kernel=np.sinc(t*.1)*np.hamming(len(t));kernel/=kernel.sum()
    signal=np.convolve(raw,kernel,mode='same');coarse=signal[::8];templates=[];starts=[];candidate_scores=[]
    for name,path,label in entries:
        sr,pixels=read(ROOT/'app/src/main/assets'/path)
        if sr!=rate:raise ValueError('Unexpected source/output rate')
        template=np.convolve(pixels,kernel,mode='same');templates.append(template);y=template[::8]
        fft=1<<(len(coarse)+len(y)-2).bit_length();cross=np.fft.irfft(np.fft.rfft(coarse,fft)*np.fft.rfft(y[::-1],fft),fft)[len(y)-1:len(coarse)]
        squares=np.r_[0,np.cumsum(coarse*coarse)];scores=cross/np.sqrt(np.maximum(1e-20,(squares[len(y):]-squares[:-len(y)])*np.dot(y,y)));at=int(np.argmax(scores));starts.append(at*8);candidate_scores.append(float(scores[at]))
    lo=min(starts)-32;hi=max(starts[i]+len(templates[i]) for i in range(3))+32
    if lo<0 or hi>len(signal):raise ValueError('Incomplete declared cue interval')
    target=signal[lo:hi]
    def fit(positions):
        cols=[]
        for at,y in zip(positions,templates):
            col=np.zeros(len(target));at-=lo;col[at:at+len(y)]=y;cols.append(col)
        matrix=np.column_stack(cols);gains=np.linalg.lstsq(matrix,target,rcond=None)[0];residual=target-matrix@gains
        return float(np.dot(residual,residual)),gains,matrix
    for _ in range(2):
        for index in range(3):
            best=None
            for delta in range(-8,9):
                candidate=starts.copy();candidate[index]+=delta;loss,_,_=fit(candidate)
                if best is None or loss<best[0]:best=(loss,candidate)
            starts=best[1]
    loss,gains,matrix=fit(starts);model=matrix@gains;correlation=lambda a,b:float(np.dot(a,b)/np.sqrt(np.dot(a,a)*np.dot(b,b)))
    joint=correlation(model,target);clean=target-matrix[:,:2]@gains[:2];pc33=correlation(clean,matrix[:,2]*gains[2]);condition=float(np.linalg.cond(matrix.T@matrix))
    rms=np.array([np.sqrt(np.mean(y*y)) for y in templates])*gains
    if np.any(gains<=0) or np.any(rms<8/32768) or condition>100 or joint<.999 or pc33<.999:
        raise AssertionError(dict(gains=gains.tolist(),audibleRms=rms.tolist(),condition=condition,joint=joint,pc33=pc33))
    nuisance=matrix[:,:2]@np.linalg.lstsq(matrix[:,:2],target,rcond=None)[0];without=float(np.mean((target-nuisance)**2));with_pc=float(loss/len(target))
    if without<with_pc*100:raise AssertionError('Original33 contribution not independently identifiable')
    result=dict(status='PASS',scope='Actual declared normal tactic mix: original PC33 plus two inherited mobile nuisance cues. Not full PC battle audio restoration, microphone or ARM speaker evidence.',
        serial=flow['serial'],apkSha256=flow['installed_sha256'],waveSha256=hashlib.sha256(source.read_bytes()).hexdigest(),sampleRate=rate,intervalSamples=[lo,hi],jointCorrelation=joint,pc33NuisanceRemovedCorrelation=pc33,gramCondition=condition,
        residualMseWithPc33=with_pc,residualMseWithoutPc33=without,facts=facts['facts'],cues=[dict(name=name,label=label,path=path,sha256=hashlib.sha256((ROOT/'app/src/main/assets'/path).read_bytes()).hexdigest(),startSample=starts[i],seconds=starts[i]/rate,gain=float(gains[i]),audibleRms=float(rms[i]),candidateCorrelation=candidate_scores[i]) for i,(name,path,label) in enumerate(entries)],
        filters='Same fixed65-tap cutoff FIR as existing mixer check; original rate phase refinement; no acceptance threshold relaxation')
    output.write_text(json.dumps(result,indent=2)+'\n');print('PASS normal authoritative fact PC33 mixed PCM; joint=',joint,'pc33=',pc33)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('wave',type=Path);p.add_argument('--flow',type=Path,required=True);p.add_argument('--facts',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.wave,a.flow,a.facts,a.output)
