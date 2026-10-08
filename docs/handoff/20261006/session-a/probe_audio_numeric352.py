#!/usr/bin/env python3
"""Diagnose numerical coarse-score candidates in unchanged actual PCM."""
from pathlib import Path
import json,wave,numpy as np
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;CASE=ROOT/'out/session-a/gpu-menu349/large-menu-pcm'
def read(p):
 with wave.open(str(p)) as w:return w.getframerate(),np.frombuffer(w.readframes(w.getnframes()),'<i2').astype(float).reshape(-1,w.getnchannels()).mean(1)/32768
r=json.loads((CASE/'session.json').read_text());c=next(x for x in r['audioCapture']['captures'] if x['run'].endswith('_menu'));capture=Path(c['hostPath'])/'android-mix.wav';ref=ROOT/'out/session-a/music-submission-appop-44/reference/2238.wav';rate,x=read(capture);rr,y=read(ref);assert rate==rr
k=np.sinc(np.arange(-32,33)*.1)*np.hamming(65);k/=k.sum();x=np.convolve(x,k,'same');y=np.convolve(y,k,'same');xc=x[::8];results=[]
for sec in [None,5,55,75]:
 t=y if sec is None else y[sec*rate:(sec+2)*rate];tc=t[::8];n=1<<(len(xc)+len(tc)-2).bit_length();cross=np.fft.irfft(np.fft.rfft(xc,n)*np.fft.rfft(tc[::-1],n),n)[len(tc)-1:len(xc)];prefix=np.r_[0,np.cumsum(xc*xc)];energies=prefix[len(tc):]-prefix[:-len(tc)];scores=np.abs(cross)/np.sqrt(np.maximum(1e-20,energies*float(tc@tc)));indices=np.argsort(scores)[-8:][::-1];candidates=[]
 for idx in indices:
  win=xc[idx:idx+len(tc)];energy=float(win@win);corr=abs(float(win@tc))/np.sqrt(max(1e-20,energy*float(tc@tc)));candidates.append({'coarseSample':int(idx)*8,'coarsePrefixScore':float(scores[idx]),'prefixEnergy':float(energies[idx]),'actualWindowEnergy':energy,'actualCoarseCorrelation':corr})
 results.append({'referenceSeconds':sec,'candidates':candidates,'captureMaximumCoarsePrefixScore':float(scores.max())})
report={'scope':'Numerical diagnostic of unchanged actual PCM coarse search only; no repaired recording/whole acceptance/threshold relaxation or unique playback attribution','results':results,'wholeGoalComplete':False};out=DOC/'AUDIO_NUMERIC352.json';assert not out.exists();out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
