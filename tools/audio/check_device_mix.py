#!/usr/bin/env python3
"""Recognize packaged cues in actual emulator mixed PCM; not a SoundPool call counter.
Input must be a finalized WAV or an explicitly labelled snapshot with repaired length.
Requires numpy, e.g. PYTHONPATH=out/toolchain/pc-system-python /usr/bin/python3.
"""
from pathlib import Path
import argparse,wave,json,hashlib
import numpy as np
ROOT=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser();p.add_argument('wave',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--cue',action='append',default=[]);a=p.parse_args()
def read(path):
 with wave.open(str(path)) as f:
  assert f.getsampwidth()==2
  rate=f.getframerate();x=np.frombuffer(f.readframes(f.getnframes()),dtype='<i2').astype(np.float64).reshape(-1,f.getnchannels()).mean(1)/32768
 return rate,x
rate,signal=read(a.wave);assert np.sqrt(np.mean(signal**2))>.001,'Silent device mixer capture'
# Suppress the real device's 44.1/48k resampler cutoff difference before decimation.
t=np.arange(-32,33);kernel=np.sinc(t*.1)*np.hamming(len(t));kernel/=kernel.sum()
def filtered(x):return np.convolve(x,kernel,mode='same')[::8]
x=filtered(signal);rows=[]
overridePath=ROOT/'app/src/main/assets/audio/pc-media-manifest.json'
overrides=json.loads(overridePath.read_text())['cues'] if overridePath.exists() else {}
for path in sorted((ROOT/'app/src/main/assets/audio').glob('*.wav')):
 cue=path.stem
 if a.cue and cue not in a.cue:continue
 if cue in overrides:path=ROOT/'app/src/main/assets'/overrides[cue]
 sourceRate,source=read(path);assert sourceRate==rate;template=filtered(source)
 n=len(x)+len(template)-1;fftSize=1<<(n-1).bit_length()
 cross=np.fft.irfft(np.fft.rfft(x,fftSize)*np.fft.rfft(template[::-1],fftSize),fftSize)[len(template)-1:len(x)]
 squares=np.r_[0,np.cumsum(x*x)];energy=squares[len(template):]-squares[:-len(template)]
 scores=np.abs(cross)/np.sqrt(np.maximum(1e-20,energy*np.sum(template*template)));at=int(np.argmax(scores));score=float(scores[at])
 assert score>.80,(path.name,'cue not recognized in actual mixed output',score)
 rows.append(dict(cue=cue,correlation=score,seconds=at*8/rate,asset_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),source='native PC HUD33' if cue in overrides else 'inherited mobile composition'))
assert rows and (not a.cue or {r['cue'] for r in rows}==set(a.cue)),'Requested cue missing from packaged catalog'
report=dict(status='PASS',scope='Actual Android playback mixed by emulator WAV backend; no microphone; not ARM hardware speaker quality',sample_rate=rate,seconds=len(signal)/rate,rms=float(np.sqrt(np.mean(signal**2))),device_wave_sha256=hashlib.sha256(a.wave.read_bytes()).hexdigest(),unique_sample_hashes=len({r['asset_sha256'] for r in rows}),cues=rows)
a.output.write_text(json.dumps(report,indent=2)+'\n');print('PASS',len(rows),'actual mixed cues; minimum correlation',min(r['correlation'] for r in rows))
