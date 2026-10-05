#!/usr/bin/env python3
"""Strict audible identity of declared original menu music in unedited actual PCM.

Separate from full-track continuity. Four predeclared2s windows10/20/30/40
must each exceed.999 with positive audible gain. Does not repair/drop samples.
"""
import argparse,hashlib,json,wave
from pathlib import Path
import numpy as np
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def read(path):
    with wave.open(str(path)) as w:
        if w.getsampwidth()!=2 or w.getframerate()!=44100:raise ValueError('Expected original-rate measured PCM16')
        return np.frombuffer(w.readframes(w.getnframes()),'<i2').reshape(-1,w.getnchannels()).mean(1).astype(float)/32768
def inspect(capture,reference,manifest,flow,scene,output):
    if output.exists():raise ValueError('Fresh evidence required')
    entry=next(r for r in json.load(open(manifest))['entries'] if r['resourceId']==2238);actual=json.load(open(flow));context=json.load(open(scene))
    if sha(reference)!=entry['wavSha256'] or not actual['passed'] or not actual['restoration']['all_original_files_byte_equal'] or not context['normalMenuOnly'] or not context['noTestPlaybackDirective'] or context['resourceId']!=2238 or not context['allSaveRngEqual']:raise ValueError('Original source/normal menu/current installed restore evidence required')
    taps=np.arange(-32,33);kernel=np.sinc(taps*.1)*np.hamming(len(taps));kernel/=kernel.sum();x,y=[np.convolve(read(p),kernel,'same') for p in [capture,reference]];coarse=x[::8];prefix=np.r_[0,np.cumsum(coarse*coarse)];rows=[]
    for second in [10,20,30,40]:
        template=y[second*44100:(second+2)*44100];short=template[::8];n=1<<(len(coarse)+len(short)-2).bit_length();cross=np.fft.irfft(np.fft.rfft(coarse,n)*np.fft.rfft(short[::-1],n),n)[len(short)-1:len(coarse)];energy=prefix[len(short):]-prefix[:-len(short)];scores=cross/np.sqrt(np.maximum(1e-20,energy*float(short@short)));seed=int(np.argmax(scores))*8;best=None
        for at in range(max(0,seed-64),min(len(x)-len(template),seed+64)+1):
            window=x[at:at+len(template)];dot=float(window@template);gain=dot/float(template@template);corr=dot/np.sqrt(float(window@window)*float(template@template));candidate=(float(corr),at,float(gain),float(np.sqrt(np.mean((template*gain)**2))))
            if best is None or candidate[0]>best[0]:best=candidate
        corr,at,gain,rms=best
        if corr<.999 or gain<=0 or rms<8/32768:raise AssertionError(dict(referenceSeconds=second,correlation=corr,gain=gain,rms=rms))
        rows.append(dict(referenceSeconds=second,frames=len(template),captureSample=at,offsetSamples=at-second*44100,correlation=corr,gain=gain,audibleRms=rms))
    report=dict(status='PASS',scope='Actual normal menu original2238 audible sample identity at four predeclared windows; not whole-track continuity, Windows exact PCM or ARM speaker proof',captureSha256=sha(capture),referenceSha256=sha(reference),originalOggSha256=entry['oggSha256'],rows=rows,flowSha256=sha(flow),sceneSha256=sha(scene),continuousFullTrackPassed=False)
    output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'status':'PASS','minimumCorrelation':min(r['correlation'] for r in rows),'offsetSamples':[r['offsetSamples'] for r in rows]}))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('capture',type=Path);p.add_argument('--reference',type=Path,required=True);p.add_argument('--manifest',type=Path,required=True);p.add_argument('--flow',type=Path,required=True);p.add_argument('--scene',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.capture,a.reference,a.manifest,a.flow,a.scene,a.output)
