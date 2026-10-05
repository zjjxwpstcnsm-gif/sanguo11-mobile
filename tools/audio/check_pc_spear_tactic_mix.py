#!/usr/bin/env python3
"""Strict full declared original49/78 + original33 reconstruction in actual PCM.

Source33 is periodic: search its declared phase around the FFT candidate,
then require both independent source contributions and full-waveform fit.
No waveform editing, sample-library search or threshold relaxation.
"""
import argparse,hashlib,json,wave
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[2]

def read(path):
    with wave.open(str(path)) as source:
        if source.getsampwidth()!=2 or source.getframerate()!=44100:raise ValueError('Actual/reference PCM16 44100Hz required')
        return np.frombuffer(source.readframes(source.getnframes()),'<i2').reshape(-1,source.getnchannels()).mean(1).astype(float)/32768

def inspect(capture,flow_path,facts_path,output):
    if output.exists():raise ValueError('Fresh actual evidence required')
    flow=json.load(open(flow_path));facts=json.load(open(facts_path));sound=facts['originalTacticSoundId']
    if sound not in [49,78] or not flow['passed'] or not flow['restoration']['all_original_files_byte_equal'] or not facts['productionHostOnly'] or facts['originalTacticEvents']!=1 or facts['syntheticCriticalEvents']!=0 or facts['sourceTacticEnum'] not in ['THRUST','SPIRAL','DOUBLE_THRUST','HOOK','SWEEP','WHIRLWIND','FIRE_ARROW','PIERCE','VOLLEY'] or not facts.get('primaryHitRecorded',False) or facts['alreadyAppliedCritical']!=(sound==78) or not facts['facts']:raise ValueError('Actual committed original spear path required')
    paths=[ROOT/f'app/src/main/assets/audio/pc/tactic-{sound}.wav',ROOT/'app/src/main/assets/audio/pc/technique-33.wav']
    manifest=json.load(open(ROOT/'app/src/main/assets/audio/pc/tactic-sound-manifest.json'));entry=next(r for r in manifest['entries'] if r['nativeSoundId']==sound)
    if hashlib.sha256(paths[0].read_bytes()).hexdigest()!=entry['wavSha256']:raise ValueError('Original converted source changed')
    original33=json.load(open(ROOT/'app/src/main/assets/audio/pc-media-manifest.json'))
    if hashlib.sha256(paths[1].read_bytes()).hexdigest()!=original33['wavSha256']:raise ValueError('Original HUD33 changed')
    taps=np.arange(-32,33);kernel=np.sinc(taps*.1)*np.hamming(len(taps));kernel/=kernel.sum();signal=np.convolve(read(capture),kernel,'same');templates=[np.convolve(read(p),kernel,'same') for p in paths]
    def locate(template):
        x=signal[::8];y=template[::8];n=1<<(len(x)+len(y)-2).bit_length();cross=np.fft.irfft(np.fft.rfft(x,n)*np.fft.rfft(y[::-1],n),n)[len(y)-1:len(x)];square=np.r_[0,np.cumsum(x*x)];scores=cross/np.sqrt(np.maximum(1e-20,(square[len(y):]-square[:-len(y)])*float(y@y)));seed=int(np.argmax(scores))*8;best=None
        for at in range(max(0,seed-8),min(len(signal)-len(template),seed+8)+1):
            x=signal[at:at+len(template)];corr=float(x@template)/np.sqrt(max(1e-20,float(x@x)*float(template@template)))
            if best is None or corr>best[0]:best=(corr,at)
        return best[1]
    starts=[locate(t) for t in templates];seeds=starts.copy();radius=1024
    lo=min(starts)-radius-16;hi=max(starts[i]+len(templates[i]) for i in range(2))+radius+16
    if lo<0 or hi>len(signal):raise ValueError('Complete declared source interval required')
    target=signal[lo:hi];energy=float(target@target)
    def fit(positions):
        columns=[]
        for start,template in zip(positions,templates):
            column=np.zeros(len(target));at=start-lo;column[at:at+len(template)]=template;columns.append(column)
        a,b=columns;aa=float(a@a);bb=float(b@b);ab=float(a@b);ay=float(a@target);by=float(b@target);det=aa*bb-ab*ab
        if det<=0:return float('inf'),None,None
        gain=np.array([(ay*bb-by*ab)/det,(by*aa-ay*ab)/det]);return energy-float(gain@np.array([ay,by])),gain,np.column_stack(columns)
    best=None
    for shift in range(-radius,radius+1):
        candidate=[starts[0],starts[1]+shift];loss,gain,matrix=fit(candidate)
        if best is None or loss<best[0]:best=(loss,candidate)
    starts=best[1]
    for _ in range(2):
        for index in range(2):
            best=None
            for shift in range(-8,9):
                candidate=starts.copy();candidate[index]+=shift;loss,_,_=fit(candidate)
                if best is None or loss<best[0]:best=(loss,candidate)
            starts=best[1]
    loss,gains,matrix=fit(starts);model=matrix@gains
    def corr(a,b):return float(a@b)/np.sqrt(max(1e-20,float(a@a)*float(b@b)))
    joint=corr(model,target);independent=[corr(target-matrix[:,1-i]*gains[1-i],matrix[:,i]*gains[i]) for i in range(2)];normalized=matrix/np.linalg.norm(matrix,axis=0);condition=float(np.linalg.cond(normalized.T@normalized));rms=[float(np.sqrt(np.mean(t*t))*g) for t,g in zip(templates,gains)]
    if joint<.999 or min(independent)<.999 or np.any(gains<=0) or min(rms)<8/32768 or condition>20:raise AssertionError(dict(joint=joint,independent=independent,gains=gains.tolist(),rms=rms,condition=condition,seeds=seeds,starts=starts,intervalSamples=[lo,hi]))
    report=dict(status='PASS',scope='Actual two declared original converted spear49/78 and nativeHUD33 waveforms; current Android phase, not PC wall-clock/nativeWindows/ARM speaker or full audio restoration',apkSha256=flow['installed_sha256'],captureSha256=hashlib.sha256(capture.read_bytes()).hexdigest(),nativeSoundId=sound,jointCorrelation=joint,independentCorrelations=independent,gains=gains.tolist(),audibleRms=rms,normalizedCondition=condition,seeds=seeds,starts=starts,intervalSamples=[lo,hi],originalSpearPcmSha256=entry['original']['pcmS16leSha256'],playbackWavSha256=entry['wavSha256'],facts=facts['facts'])
    output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['status','nativeSoundId','jointCorrelation','independentCorrelations']}))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('capture',type=Path);p.add_argument('--flow',type=Path,required=True);p.add_argument('--facts',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.capture,a.flow,a.facts,a.output)
