#!/usr/bin/env python3
"""Synthetic numerical verifier regression; never Android playback acceptance."""
from pathlib import Path
import importlib.util,json,wave,hashlib
import numpy as np
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/timeline-numeric353';assert not OUT.exists();OUT.mkdir(parents=True)
spec=importlib.util.spec_from_file_location('timeline',ROOT/'tools/audio/diagnose_music_timeline.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
rate=2000;rng=np.random.default_rng(1947);reference=rng.normal(0,.12,rate*88);pcm=np.clip(reference*32768,-32768,32767).astype('<i2');lead=1234
signals={'continuous-silent-tail':np.r_[np.zeros(lead,dtype='<i2'),pcm,np.zeros(rate*90,dtype='<i2')],'known-gap':np.r_[np.zeros(lead,dtype='<i2'),pcm[:rate*44],np.zeros(173,dtype='<i2'),pcm[rate*44:],np.zeros(rate*90,dtype='<i2')]}
def wav(p,a):
 with wave.open(str(p),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(rate);w.writeframes(a.tobytes())
ref=OUT/'reference.wav';wav(ref,pcm);results={}
for name,a in signals.items():
 cap=OUT/(name+'.wav');wav(cap,a);before=hashlib.sha256(cap.read_bytes()).hexdigest();out=OUT/(name+'.json');module.inspect(cap,ref,out);r=json.loads(out.read_text());assert len(r['rows'])==17
 for row in r['rows']:
  expected=lead+(173 if name=='known-gap' and row['referenceSeconds']>=45 else 0)
  assert row['captureSample']-row['referenceSeconds']*rate==expected and row['correlation']>.999 and row['reliableOffset'],row
 assert hashlib.sha256(cap.read_bytes()).hexdigest()==before;results[name]=r
report={'numericalRegressionPassed':True,'syntheticSignalRows':34,'sourceVerifierSha256':hashlib.sha256((ROOT/'tools/audio/diagnose_music_timeline.py').read_bytes()).hexdigest(),'allPredeclaredWindowsChecked':True,'continuousAndKnownDiscontinuityLocated':True,'inputWavsUnchanged':True,'scope':'Synthetic verifier numerical regression only; no Android fullTrack or waveform threshold acceptance.','wholeGoalComplete':False};(DOC/'TIMELINE_NUMERIC353.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
