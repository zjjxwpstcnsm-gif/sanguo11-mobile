#!/usr/bin/env python3
"""Retain native49/78 PCM and reproducibly convert22.05kHz to44.1kHz playback.

Only declared original bank bytes; no synthetic samples, gain normalization,
padding, trimming, unknown role inference or PC installation writes.
"""
import argparse,hashlib,io,json,wave
from pathlib import Path
import av
import numpy as np

EXE='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
ARCHIVE='e61c97fee43ee23b1248c46a2eb3e50adb0db620b8c0bff6e8fd67c30ca9c31a'
def sha(data):return hashlib.sha256(data).hexdigest()

def stage(banks,chain,semantics,output):
    if output.exists():raise ValueError('Fresh staging output required')
    manifest=json.loads((banks/'manifest.json').read_bytes());calls=json.loads(chain.read_bytes());names=json.loads(semantics.read_bytes())
    if manifest['sourceSha256']!=ARCHIVE or calls['sourceExecutableSha256']!=EXE or calls['checks']!=504 or names['sourceExecutableSha256']!=EXE or names['checks']!=160:raise ValueError('Pinned original complete source proofs required')
    voiced=[r for r in calls['rows'] if r['profiles']]
    if not voiced or any(r['soundIds']!=[49 if r['record0Raw']==0 else 78] for r in voiced):raise ValueError('Original caller sound IDs differ')
    output.mkdir(parents=True);entries=[]
    for sound in [49,78]:
        row=next(r for r in manifest['entries'] if r['soundIds']==[sound]);original=(banks/row['asset']).read_bytes()
        if sha(original)!=row['wavSha256'] or (row['sampleRate'],row['channels'],row['loopFlagRaw'])!=(22050,1,0):raise ValueError('Original bank sample differs')
        with wave.open(io.BytesIO(original)) as source:
            if (source.getsampwidth(),source.getnframes())!=(2,row['samples']):raise ValueError('Original source extent differs')
            pcm=source.readframes(source.getnframes())
        if sha(pcm)!=row['pcmS16leSha256']:raise ValueError('Original raw PCM changed')
        frame=av.AudioFrame.from_ndarray(np.frombuffer(pcm,dtype='<i2').reshape(1,-1),format='s16',layout='mono');frame.sample_rate=22050
        resampler=av.AudioResampler(format='s16',layout='mono',rate=44100)
        frames=resampler.resample(frame)+resampler.resample(None)
        converted=b''.join(f.to_ndarray().astype('<i2',copy=False).tobytes() for f in frames)
        if len(converted)!=len(pcm)*2:raise ValueError('Resampler timeline differs; no pad/trim allowed')
        target=io.BytesIO()
        with wave.open(target,'wb') as wav:
            wav.setnchannels(1);wav.setsampwidth(2);wav.setframerate(44100);wav.writeframes(converted)
        data=target.getvalue();native_name=f'tactic-{sound}-original-22050.wav';name=f'tactic-{sound}.wav'
        (output/native_name).write_bytes(original);(output/name).write_bytes(data)
        entries.append(dict(nativeSoundId=sound,original=row,originalAsset='audio/pc/'+native_name,
                            asset='audio/pc/'+name,sampleRate=44100,channels=1,samples=len(converted)//2,
                            wavBytes=len(data),wavSha256=sha(data),pcmSha256=sha(converted),
                            conversion='libswresample s16 mono22050->44100; complete flush, exact doubled timeline, no gain/trim/pad'))
    report=dict(schema=1,sourceExecutableSha256=EXE,sourceArchiveSha256=ARCHIVE,
                sourceChainSha256=sha(chain.read_bytes()),sourceSemanticsSha256=sha(semantics.read_bytes()),
                converter=dict(pyav=av.__version__,libraries=av.library_versions),entries=entries,
                nativeBinding='Original order4 action0/1/2 -> renderer6/7/8, applied record0 critical -> callback564cb0 sound49/78',
                androidBinding='Immutable committed named spear tactic/critical facts, source map only; current Android presentation phase, original wall-clock pending',
                limits=['Only three named spear tactics; not other attacks/plots/equipment/naval or full sound matrix.','Converted playback44.1kHz samples retain their original22.05kHz WAV/PCM/descriptor and exact native duration.','Source critical probability/damage formulas unchanged and not re-evaluated by media.','Installed playback pending; Windows native PCM, original callback timing and ARM speaker not proven.'])
    (output/'tactic-sound-manifest.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(dict(status='SOURCE_SAMPLES_STAGED',entries=[dict(id=e['nativeSoundId'],sha=e['wavSha256'],frames=e['samples']) for e in entries])))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('banks',type=Path);p.add_argument('--chain',type=Path,required=True);p.add_argument('--semantics',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();stage(a.banks,a.chain,a.semantics,a.output)
