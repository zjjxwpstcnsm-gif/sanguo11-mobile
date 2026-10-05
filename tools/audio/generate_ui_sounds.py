#!/usr/bin/env python3
"""Deterministic mobile sound adaptation. Original compositions, no rule RNG.
No PC event/sample identity is claimed; KOVS catalog remains reference only.
Run from repository root. Outputs PCM16 mono WAV and hashes/level evidence.
"""
from pathlib import Path
import math, struct, wave, json, hashlib
ROOT=Path(__file__).resolve().parents[2];RATE=44100
DURATIONS={'ui':.09,'march':.36,'attack':.42,'tactic':.60,'critical':.72,'plot':.64,'construction':.45,'complete':.67,'turn':.82,'technique_gain':.40,'technique_loss':.40}
def noise(n):
    # Stateless integer visual/audio hash: independent of authority and RNG state.
    v=(n*1664525+1013904223)&0xffffffff;v^=v>>13;v=(v*2246822519)&0xffffffff
    return (v&65535)/32767.5-1
rows=[]
for name,duration in DURATIONS.items():
 pcm=[]
 for n in range(round(duration*RATE)):
  t=n/RATE;env=min(1,t/.004)*max(0,1-t/duration)**1.5
  if name=='ui':value=(math.sin(2*math.pi*880*t)+.4*math.sin(2*math.pi*1320*t))*math.exp(-t*36)
  elif name=='march':
   phase=t%.17;value=(noise(n)*.45+math.sin(2*math.pi*120*phase)*.6)*math.exp(-phase*32)
  elif name in ('attack','construction'):
   value=(noise(n)*.6+math.sin(2*math.pi*(105 if name=='attack' else 190)*t)*.6)*math.exp(-t*9)
  elif name=='tactic':value=noise(n)*.45*math.sin(math.pi*t/duration)+math.sin(2*math.pi*(260*t+720*t*t))*.45
  elif name=='critical':value=(math.sin(2*math.pi*135*t)+.4*math.sin(2*math.pi*207*t)+noise(n)*.22)*math.exp(-t*4)
  elif name=='plot':value=(math.sin(2*math.pi*540*t)+.35*math.sin(2*math.pi*810*t))*.6
  elif name.startswith('technique_'):
   notes=(660,880,1100) if name=='technique_gain' else (880,660,440)
   step=min(2,int(t/.13));value=(math.sin(2*math.pi*notes[step]*t)+.25*math.sin(4*math.pi*notes[step]*t))*.7
  else:
   freqs=(392,494,587) if name=='complete' else (196,294,392)
   value=sum(math.sin(2*math.pi*f*t) for f in freqs)*.32
  pcm.append(int(max(-1,min(1,value*env*.62))*32767))
 data=struct.pack('<'+'h'*len(pcm),*pcm);path=ROOT/'app/src/main/assets/audio'/f'{name}.wav'
 with wave.open(str(path),'wb') as out:out.setnchannels(1);out.setsampwidth(2);out.setframerate(RATE);out.writeframes(data)
 rms=math.sqrt(sum(v*v for v in pcm)/len(pcm))/32768
 assert rms>.03 and max(map(abs,pcm))<32767
 rows.append(dict(cue=name,path=str(path.relative_to(ROOT)),seconds=len(pcm)/RATE,rms=rms,peak=max(map(abs,pcm))/32768,sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
(ROOT/'docs/uiux/pure3d-20261003/audio-assets.json').write_text(json.dumps(dict(source='Original deterministic mobile compositions; not a claimed PC sound restoration',rate=RATE,format='PCM16 mono',assets=rows),indent=2)+'\n')
print('Generated',len(rows),'non-silent bounded PCM cues')
