#!/usr/bin/env python3
"""Fixed-window signed/absolute correlation diagnostics; never PCM acceptance."""
import pathlib,json,wave,hashlib,sys
import numpy as np
capture,reference,output=map(pathlib.Path,sys.argv[1:]);assert not output.exists()
def read(path):
 with wave.open(str(path)) as w:
  assert w.getsampwidth()==2 and w.getframerate()==44100
  a=np.frombuffer(w.readframes(w.getnframes()),'<i2').reshape(-1,w.getnchannels()).astype(float)/32768
  return a,a.mean(1)
raw,x=read(capture);original,y=read(reference);t=np.arange(-32,33);kernel=np.sinc(t*.1)*np.hamming(len(t));kernel/=kernel.sum();x=np.convolve(x,kernel,'same');y=np.convolve(y,kernel,'same');coarse=x[::8];prefix=np.r_[0,np.cumsum(coarse*coarse)];rows=[]
for sec in range(5,86,5):
 template=y[sec*44100:(sec+2)*44100];short=template[::8];size=1<<(len(coarse)+len(short)-2).bit_length();cross=np.fft.irfft(np.fft.rfft(coarse,size)*np.fft.rfft(short[::-1],size),size)[len(short)-1:len(coarse)];energy=prefix[len(short):]-prefix[:-len(short)];scores=cross/np.sqrt(np.maximum(1e-20,energy*float(short@short)));seed=int(np.argmax(np.abs(scores)))*8;best=None
 for at in range(max(0,seed-64),min(len(x)-len(template),seed+64)+1):
  window=x[at:at+len(template)];dot=float(window@template);signed=dot/np.sqrt(max(1e-20,float(window@window)*float(template@template)));candidate=(abs(signed),at,signed,dot/max(1e-20,float(template@template)))
  if best is None or candidate[0]>best[0]:best=candidate
 rows.append({'referenceSeconds':sec,'absoluteCorrelation':best[0],'signedCorrelation':best[2],'gain':best[3],'captureSample':best[1],'offsetSamples':best[1]-sec*44100})
report={'scope':'fixed17windows signed/absolute diagnostic only; no waveform acceptance, polarity correction, timeline repair or threshold change','captureSha256':hashlib.sha256(capture.read_bytes()).hexdigest(),'referenceSha256':hashlib.sha256(reference.read_bytes()).hexdigest(),'actualChannelRms':np.sqrt(np.mean(raw*raw,axis=0)).tolist(),'referenceChannelRms':np.sqrt(np.mean(original*original,axis=0)).tolist(),'rows':rows};output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(rows))
