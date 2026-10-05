#!/usr/bin/env python3
"""Identify the two declared original sources and measured music ducking in actual PCM.

No sample-library search, waveform edit or normal action/scene binding claim.
Uses one fixed music reference and the exact voice ID reported by this receipt
test adapter; independent audible music before voice plus joint overlap fit.
"""
import argparse
import hashlib
import json
from pathlib import Path
import wave
import numpy as np

ROOT=Path(__file__).resolve().parents[2]


def inspect(mixed,flow_path,observations_path,references,output):
    if output.exists():raise ValueError('Fresh evidence required')
    flow=json.loads(flow_path.read_text());observed=json.loads(observations_path.read_text())
    if not flow['passed'] or not flow['restoration']['all_original_files_byte_equal'] or not observed['result'].startswith('SHARED_MEDIA PASS'):
        raise ValueError('Actual successful source playback and restoration required')
    rows=observed['observations'];first=next(r for r in rows if r['label']=='voice-duck')
    if not first['parentId'] or not first['presentationParentId'] or first['voiceReleased']:
        raise ValueError('Actual declared voice parent/phase required')
    resource=2287+first['voiceId']
    catalog=json.loads((ROOT/'app/src/main/assets/audio/pc/voice-manifest.json').read_text())
    source=next(r for r in catalog['voices'] if r['resourceId']==resource)
    if catalog['sourceArchiveSha256']!='e61c97fee43ee23b1248c46a2eb3e50adb0db620b8c0bff6e8fd67c30ca9c31a':raise ValueError('Unexamined original source')
    def read(path):
        with wave.open(str(path)) as f:
            if f.getsampwidth()!=2 or f.getframerate()!=44100:raise ValueError('Original/actual PCM format')
            return np.frombuffer(f.readframes(f.getnframes()),dtype='<i2').astype(float).reshape(-1,f.getnchannels()).mean(1)/32768
    original=read(mixed);music_path=references/'2261.wav';voice_path=references/(str(resource)+'.wav')
    with wave.open(str(voice_path)) as f:
        if f.getnchannels()!=source['channels'] or f.getnframes()!=source['referenceDecodedFrames'] or hashlib.sha256(f.readframes(f.getnframes())).hexdigest()!=source['referencePcmSha256']:
            raise ValueError('Declared original voice reference differs')
    music_source=next(r for r in json.loads((ROOT/'app/src/main/assets/audio/pc/music-manifest.json').read_text())['tracks'] if r['resourceId']==2261)
    with wave.open(str(music_path)) as f:
        if hashlib.sha256(f.readframes(f.getnframes())).hexdigest()!=music_source['referencePcmSha256']:raise ValueError('Declared original music reference differs')
    music=read(music_path);voice=read(voice_path)
    t=np.arange(-32,33);kernel=np.sinc(t*.1)*np.hamming(len(t));kernel/=kernel.sum()
    raw=np.convolve(original,kernel,'same');music=np.convolve(music,kernel,'same');voice=np.convolve(voice,kernel,'same')
    def correlation(a,b):return float(a@b/(np.linalg.norm(a)*np.linalg.norm(b)))
    def locate(template):
        x=raw[::8];y=template[::8];n=1<<(len(x)+len(y)-2).bit_length()
        cross=np.fft.irfft(np.fft.rfft(x,n)*np.fft.rfft(y[::-1],n),n)[len(y)-1:len(x)]
        square=np.r_[0,np.cumsum(x*x)];scores=cross/np.sqrt(np.maximum(1e-20,(square[len(y):]-square[:-len(y)])*(y@y)))
        candidate=int(np.argmax(scores))*8;best=None
        for start in range(max(0,candidate-8),candidate+9):
            if start+len(template)>len(raw):continue
            score=correlation(raw[start:start+len(template)],template)
            if best is None or score>best[0]:best=(score,start)
        if best is None:raise ValueError('Incomplete declared source interval')
        return best
    music_score,music_start=locate(music[:44100]);voice_score,voice_start=locate(voice[:26460])
    # Fixed200..500ms original voice overlap avoids the initial focus/volume ramp.
    overlap_start=voice_start+8820;length=13230;music_phase=(overlap_start-music_start)%len(music)
    if music_phase+length>len(music) or overlap_start+length>len(raw):raise ValueError('Declared overlap crosses unexamined repeat boundary')
    music_segment=music[music_phase:music_phase+length];voice_segment=voice[8820:8820+length]
    matrix=np.column_stack([music_segment,voice_segment]);target=raw[overlap_start:overlap_start+length]
    gains=np.linalg.lstsq(matrix,target,rcond=None)[0];model=matrix@gains
    normalized=matrix/np.linalg.norm(matrix,axis=0);condition=float(np.linalg.cond(normalized.T@normalized))
    clean=target-music_segment*gains[0];voice_correlation=correlation(clean,voice_segment*gains[1]);joint=correlation(model,target)
    before_template=music[10000:23000];before_target=raw[music_start+10000:music_start+23000]
    before_gain=float(before_target@before_template/(before_template@before_template));duck=float(gains[0]/before_gain)
    before_rms=float(np.sqrt(np.mean((before_template*before_gain)**2)));voice_rms=float(np.sqrt(np.mean((voice_segment*gains[1])**2)))
    if music_score<.999 or joint<.999 or voice_correlation<.999 or condition>20 or np.any(gains<=0) or before_rms<8/32768 or voice_rms<8/32768 or abs(duck-.35)>.02:
        raise AssertionError(dict(music=music_score,joint=joint,voice=voice_correlation,normalizedCondition=condition,gains=gains.tolist(),duck=duck,beforeRms=before_rms,voiceRms=voice_rms))
    result=dict(status='PASS',scope='Actual shared original music/voice PCM and measured ducking with actual receipt + explicit test profile/track. Not normal original scene/action binding or ARM/microphone proof.',
                serial=flow['serial'],apkSha256=flow['installed_sha256'],waveSha256=hashlib.sha256(mixed.read_bytes()).hexdigest(),
                musicResource=2261,voiceResource=resource,voiceFactId=first['voiceFactId'],parentId=first['parentId'],presentationParentId=first['presentationParentId'],
                musicReferenceSha256=hashlib.sha256(music_path.read_bytes()).hexdigest(),voiceReferenceSha256=hashlib.sha256(voice_path.read_bytes()).hexdigest(),
                musicStartSample=music_start,voiceStartSample=voice_start,overlapSamples=[overlap_start,overlap_start+length],
                musicAloneCorrelation=music_score,voiceCandidateCorrelation=voice_score,jointCorrelation=joint,voiceAfterMusicCorrelation=voice_correlation,
                musicBeforeGain=before_gain,musicDuckedGain=float(gains[0]),voiceGain=float(gains[1]),musicDuckRatio=duck,
                normalizedGramCondition=condition,musicBeforeRms=before_rms,voiceRms=voice_rms,
                method='Declared templates only; fixed65-tap cutoff FIR; original44100Hz phase refinement; normalized column condition to avoid amplitude-unit scaling; no threshold relaxation')
    output.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:result[k] for k in ['status','musicAloneCorrelation','jointCorrelation','voiceAfterMusicCorrelation','musicDuckRatio']}))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('mixed',type=Path);p.add_argument('--flow',type=Path,required=True);p.add_argument('--observations',type=Path,required=True);p.add_argument('--references',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();inspect(a.mixed,a.flow,a.observations,a.references,a.output)
