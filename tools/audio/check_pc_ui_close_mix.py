#!/usr/bin/env python3
"""Recognize declared original close1 from normal Back in actual PCM.

Normal cancellation precedes the explicit all-cue test; no other source1
producer runs in this acceptance. The later explicit probe overlaps HUD33's
tail under the inherited950ms cadence and is not accepted as an isolated source.
"""
import argparse,hashlib,json,wave
from pathlib import Path
import numpy as np

ROOT=Path(__file__).resolve().parents[2]

def verify(mixed,flow,normal,output):
    if output.exists():raise ValueError('Fresh evidence required')
    f=json.loads(flow.read_bytes());event=json.loads(normal.read_bytes())
    if not f['passed'] or not f['restoration']['all_original_files_byte_equal']:raise ValueError('Actual successful install and restoration required')
    if not event['fullSaveRngByteEqual'] or event['nativeSoundId']!=1 or event['cancelledBefore']!=0 or event['cancelledAfter']!=1 or event['ordinaryDismissAfter']!=1:raise ValueError('Normal cancellation/ordinary dismissal evidence missing')
    source=ROOT/'app/src/main/assets/audio/pc/ui-close-1.wav'
    if hashlib.sha256(source.read_bytes()).hexdigest()!='a0c2ee88acf2518afff960358e4c5054de54f1d58bcd2809afe70813ca6db3a6':raise ValueError('Original source changed')
    def read(p):
        with wave.open(str(p)) as w:
            if w.getsampwidth()!=2 or w.getframerate()!=44100:raise ValueError('Unexamined original PCM format')
            return np.frombuffer(w.readframes(w.getnframes()),dtype='<i2').astype(float).reshape(-1,w.getnchannels()).mean(1)/32768
    x=read(mixed);y=read(source);t=np.arange(-32,33);kernel=np.sinc(t*.1)*np.hamming(65);kernel/=kernel.sum();x=np.convolve(x,kernel,'same');y=np.convolve(y,kernel,'same')
    small_x=x[::4];small_y=y[::4];n=1<<(len(small_x)+len(small_y)-2).bit_length()
    cross=np.fft.irfft(np.fft.rfft(small_x,n)*np.fft.rfft(small_y[::-1],n),n)[len(small_y)-1:len(small_x)]
    energy=np.r_[0,np.cumsum(small_x*small_x)];score=cross/np.sqrt(np.maximum(1e-20,(energy[len(small_y):]-energy[:-len(small_y)])*(small_y@small_y)))
    found=[]
    for _ in range(8):
        rough=int(np.argmax(score));quality=float(score[rough])
        if quality<.995:break
        candidate=rough*4;best=None
        for start in range(max(0,candidate-32),min(len(x)-len(y),candidate+32)+1):
            segment=x[start:start+len(y)];gain=float(segment@y/(y@y));correlation=float(segment@y/np.sqrt(max(1e-20,(segment@segment)*(y@y))))
            if best is None or correlation>best['correlation']:best=dict(startSample=start,endSample=start+len(y),correlation=correlation,gain=gain,audibleRms=float(np.sqrt(np.mean((y*gain)**2))))
        if best and best['correlation']>.999 and best['gain']>0 and best['audibleRms']>8/32768:found.append(best)
        radius=len(small_y);score[max(0,rough-radius):min(len(score),rough+radius+1)]=-1
    found.sort(key=lambda v:v['startSample'])
    if not found:raise AssertionError({'expected':'isolated first original source1 from actual normal Back','originalCloseOccurrences':found})
    # The normal cancellation occurs before the explicit cue loop's first HUD33.
    # This source anchor rejects a recording in which only the later cue probe
    # was audible. Do not infer waveform time from Android wall time: the WAV
    # backend can omit inactive intervals.
    hud_path=ROOT/'app/src/main/assets/audio/pc/technique-33.wav'
    if hashlib.sha256(hud_path.read_bytes()).hexdigest()!='1cf7d3edbf967ac7f7123fdb2f91a54714827520abce2ced13c4a70610909f78':raise ValueError('Original HUD33 anchor changed')
    hud=np.convolve(read(hud_path),kernel,'same')[:35280];hx=x[::8];hy=hud[::8];hn=1<<(len(hx)+len(hy)-2).bit_length()
    hc=np.fft.irfft(np.fft.rfft(hx,hn)*np.fft.rfft(hy[::-1],hn),hn)[len(hy)-1:len(hx)];he=np.r_[0,np.cumsum(hx*hx)];hs=hc/np.sqrt(np.maximum(1e-20,(he[len(hy):]-he[:-len(hy)])*(hy@hy)));anchors=[]
    for _ in range(8):
        rough=int(np.argmax(hs))
        # Coarse8frame grid can miss the phase of the original narrow-period
        # HUD33. It only nominates candidates; full-rate acceptance stays>.999.
        if hs[rough]<.80:break
        candidate=rough*8;best=(-1,0)
        for start in range(max(0,candidate-1024),min(len(x)-len(hud),candidate+1024)+1):
            segment=x[start:start+len(hud)];quality=float(segment@hud/np.sqrt(max(1e-20,(segment@segment)*(hud@hud))))
            if quality>best[0]:best=(quality,start)
        if best[0]>.999:anchors.append({'startSample':best[1],'correlation':best[0]})
        hs[max(0,rough-len(hy)):min(len(hs),rough+len(hy)+1)]=-1
    if not anchors or found[0]['endSample']>=min(a['startSample'] for a in anchors):raise AssertionError('No measured normal close before explicit HUD33 cue-loop anchor')
    result=dict(status='PASS',scope='First actual normal settings Back cancellation; later overlapping cue probe/source OS routing/all UI matrix/ARM unverified',
        serial=f['serial'],apkSha256=f['installed_sha256'],sourceWavSha256=hashlib.sha256(source.read_bytes()).hexdigest(),mixedWavSha256=hashlib.sha256(mixed.read_bytes()).hexdigest(),normalUiEvent=event,
        occurrences=found,normalCancellationOccurrence=0,explicitCueLoopHud33Anchors=sorted(anchors,key=lambda a:a['startSample']),laterProbeLimit='Inherited950ms gap after1.025s HUD33 overlaps source1; no isolated second-source acceptance claim',method='Declared source1 full3042frame fit before independently measured HUD33 cue-loop anchor; same fixed65-tap FIR; >.999 positive audible normal source')
    output.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({'status':'PASS','occurrences':found}))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('mixed',type=Path);p.add_argument('--flow',type=Path,required=True);p.add_argument('--normal',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();verify(a.mixed,a.flow,a.normal,a.output)
