#!/usr/bin/env python3
"""Fixed17 original windows; exclude measured inaudible candidates, no PCM/time repair or acceptance."""
import sys,json,pathlib,wave,hashlib
import numpy as np
capture,reference,output=map(pathlib.Path,sys.argv[1:]);assert not output.exists()
def read(p):
 with wave.open(str(p)) as w:
  assert w.getsampwidth()==2 and w.getframerate()==44100
  return np.frombuffer(w.readframes(w.getnframes()),'<i2').reshape(-1,w.getnchannels()).astype(float).mean(1)/32768
x,y=read(capture),read(reference);t=np.arange(-32,33);k=np.sinc(t*.1)*np.hamming(65);k/=k.sum();x=np.convolve(x,k,'same');y=np.convolve(y,k,'same');small=x[::8];prefix=np.r_[0,np.cumsum(small*small)];rows=[]
for sec in range(5,86,5):
 template=y[sec*44100:(sec+2)*44100];short=template[::8];n=1<<(len(small)+len(short)-2).bit_length();cross=np.fft.irfft(np.fft.rfft(small,n)*np.fft.rfft(short[::-1],n),n)[len(short)-1:len(small)];energy=prefix[len(short):]-prefix[:-len(short)];scores=np.abs(cross)/np.sqrt(np.maximum(1e-20,energy*float(short@short)));bad=int(np.argmax(scores));valid=energy>=len(short)*(8/32768)**2;scores[~valid]=-np.inf;seed=int(np.argmax(scores))*8;best=None
 for at in range(max(0,seed-64),min(len(x)-len(template),seed+64)+1):
  window=x[at:at+len(template)];dot=float(window@template);corr=dot/np.sqrt(max(1e-20,float(window@window)*float(template@template)));candidate=(abs(corr),at,corr,dot/max(1e-20,float(template@template)))
  if best is None or candidate[0]>best[0]:best=candidate
 row=dict(referenceSeconds=sec,correlation=best[2],gain=best[3],captureSample=best[1],offsetSamples=best[1]-sec*44100,excludedRawArgmaxSample=bad*8,excludedRawArgmaxEnergy=float(energy[bad]),rawArgmaxDirectEnergy=float(small[bad:bad+len(short)]@small[bad:bad+len(short)]));rows.append(row);print(json.dumps(row),flush=True)
report=dict(scope='Fixed17 diagnostic; measured audible-candidate gate 8 PCM16 RMS counts, no lowered .995 whole-track threshold, no PCM correction or acceptance',captureSha256=hashlib.sha256(capture.read_bytes()).hexdigest(),referenceSha256=hashlib.sha256(reference.read_bytes()).hexdigest(),rows=rows,wholeTrackPassed=False);output.write_text(json.dumps(report,indent=2)+'\n')
