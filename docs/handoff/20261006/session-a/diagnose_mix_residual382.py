#!/usr/bin/env python3
"""Original strict matcher numerical diagnostics; cannot turn a failed capture into PASS."""
from pathlib import Path
import json,time,sys
import numpy as np
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent
sys.path.insert(0,str(D));from run_remaining_normal_media import sha
sys.path.insert(0,str(ROOT/'tools/audio'));from check_pc_music_mix import read
from read_session_state import read_session_state
from run_music_reserve378 import live_case

def main():
 out=D/'MIX_RESIDUAL382.json';assert not out.exists();case=ROOT/'out/session-a/reserve-map380'
 while True:
  if (case/'session.json').exists():
   r=read_session_state(case/'session.json')
   if r['stage']=='restored-verified' and not live_case(case):break
  time.sleep(5)
 source=ROOT/'out/session-a/music-reserve378/large-menu-pcm';s=read_session_state(source/'session.json');assert s['stage']=='restored-verified';capture=next(x for x in s['audioCapture']['captures'] if x['run'].endswith('_menu'));wav=Path(capture['hostPath'])/'android-mix.wav';assert sha(wav)==capture['result']['wavSha256'];ref=ROOT/'out/session-a/music-submission-appop-44/reference/2238.wav';ref_hash=sha(ref);rate,x=read(wav);ref_rate,y=read(ref);assert rate==ref_rate==44100
 taps=np.arange(-32,33);kernel=np.sinc(taps*.1)*np.hamming(len(taps));kernel/=kernel.sum();x,y=[np.convolve(a,kernel,'same') for a in [x,y]];cx,cy=x[::8],y[::8];size=1<<(len(cx)+len(cy)-2).bit_length();cross=np.fft.irfft(np.fft.rfft(cx,size)*np.fft.rfft(cy[::-1],size),size)[len(cy)-1:len(cx)];pre=np.r_[0,np.cumsum(cx*cx)];energy=pre[len(cy):]-pre[:-len(cy)];scores=np.abs(cross)/np.sqrt(np.maximum(1e-20,energy*(cy@cy)));seed=int(np.argmax(scores))*8;best=(0,0,0)
 for pos in range(max(0,seed-64),min(len(x)-len(y),seed+64)+1):
  window=x[pos:pos+len(y)];dot=float(window@y);ex=float(window@window);ey=float(y@y);score=abs(dot)/np.sqrt(max(1e-20,ex*ey))
  if score>best[0]:best=(float(score),pos,dot/ey)
 score,pos,gain=best;window=x[pos:pos+len(y)];rows=[]
 for second in range((len(y)+rate-1)//rate):
  a=window[second*rate:(second+1)*rate];b=y[second*rate:(second+1)*rate];ey=float(b@b);ex=float(a@a);dot=float(a@b);local_gain=dot/ey if ey>0 else 0;error=a-gain*b;local_error=a-local_gain*b
  rows.append({'referenceSeconds':second,'frames':len(b),'sourceEnergy':ey,'actualEnergy':ex,'sameConstantOffsetCorrelation':dot/np.sqrt(ex*ey) if ex*ey>0 else None,'wholeFittedGain':gain,'diagnosticLocalGain':local_gain,'globalGainResidualEnergy':float(error@error),'localGainResidualEnergy':float(local_error@local_error)})
 regions=[]
 for begin,end in [(0,15),(15,40),(40,55),(55,89)]:
  chosen=rows[begin:end];regions.append({'referenceSeconds':[begin,end],'sourceEnergy':sum(a['sourceEnergy'] for a in chosen),'actualEnergy':sum(a['actualEnergy'] for a in chosen),'globalGainResidualEnergy':sum(a['globalGainResidualEnergy'] for a in chosen),'localGainResidualEnergy':sum(a['localGainResidualEnergy'] for a in chosen)})
 assert sha(wav)==capture['result']['wavSha256'] and sha(ref)==ref_hash;report={'strictOriginalFullGateStillFailed':True,'diagnosticSameMatcherCorrelation':score,'oneConstantOffsetSamples':pos,'wholeFittedGain':gain,'all89FixedOneSecondIntervals':rows,'predeclaredRegions':regions,'actualMenuMetadata':read_session_state(source/'evidence/menu-music.json'),'rawCaptureSha256':sha(wav),'originalReferenceWavSha256':sha(ref),'scope':'Original full checker best constant offset, per-second residual/energy diagnostics only. Normal UI sound/gain overlap is possible; cannot uniquely attribute residual. No per-window time correction, waveform rewrite, sample dropping or relaxed0.995. Defer heavy numerical work until actual380 full restore/owner exit, original game playback failure retained.','wholeGoalComplete':False};out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'correlation':score,'regions':regions}))
if __name__=='__main__':main()
