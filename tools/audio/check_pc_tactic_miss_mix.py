#!/usr/bin/env python3
"""No complete declared original49/78 in actual miss PCM, at unchanged.999 gate.

Combined with six real AudioTrack heads0 and once-only committed UI reference.
Not a proof of absolute silence, native58 identity, or complete failed effects.
"""
import argparse,hashlib,json,wave
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[2]
def read(path):
    with wave.open(str(path)) as source:
        if source.getsampwidth()!=2 or source.getframerate()!=44100:raise ValueError('Measured PCM16 44100 required')
        return np.frombuffer(source.readframes(source.getnframes()),'<i2').reshape(-1,source.getnchannels()).mean(1).astype(float)/32768
def inspect(directory,output):
    if output.exists():raise ValueError('Fresh evidence required')
    flow=json.load(open(directory/'results.json'));facts=json.load(open(directory/'ui-evidence/critical-facts.json'));log=(directory/'ui-evidence/result.txt').read_text()
    if not flow['passed'] or not flow['restoration']['all_original_files_byte_equal'] or not facts['explicitMissFixture'] or facts['primaryHitRecorded'] or facts['originalTacticEvents']!=0 or facts['originalTacticSoundId']!=-1 or facts['alreadyAppliedCritical'] or facts['syntheticCriticalEvents']!=0 or log.count('PASS unresolved miss never submits any original49/78 PCM')!=6:raise ValueError('Actual installed reference-equal miss and six source heads0 required')
    taps=np.arange(-32,33);kernel=np.sinc(taps*.1)*np.hamming(len(taps));kernel/=kernel.sum();actual=np.convolve(read(directory/'captured-window.wav'),kernel,'same')[::8];prefix=np.r_[0,np.cumsum(actual*actual)];manifest=json.load(open(ROOT/'app/src/main/assets/audio/pc/tactic-sound-manifest.json'));rows=[]
    for sound in [49,78]:
        path=ROOT/f'app/src/main/assets/audio/pc/tactic-{sound}.wav';entry=next(r for r in manifest['entries'] if r['nativeSoundId']==sound)
        if hashlib.sha256(path.read_bytes()).hexdigest()!=entry['wavSha256']:raise ValueError('Declared source changed')
        template=np.convolve(read(path),kernel,'same')[::8];n=1<<(len(actual)+len(template)-2).bit_length();cross=np.fft.irfft(np.fft.rfft(actual,n)*np.fft.rfft(template[::-1],n),n)[len(template)-1:len(actual)];energy=prefix[len(template):]-prefix[:-len(template)];score=cross/np.sqrt(np.maximum(1e-20,energy*float(template@template)));maximum=float(np.max(score))
        if maximum>=.999:raise AssertionError('Unexpected complete matching original success sound '+str(sound))
        rows.append(dict(nativeSoundId=sound,maximumCompleteWaveCorrelation=maximum,unchangedPositiveIdentityThreshold=.999,sourceWavSha256=entry['wavSha256']))
    report=dict(status='PASS_NO_COMPLETE_ORIGINAL_SUCCESS_WAVE',scope='Installed miss reference/zero source heads plus no full49/78 match at unchanged.999 gate; residual mobile cues remain, native58 unbound',apkSha256=flow['installed_sha256'],captureSha256=hashlib.sha256((directory/'captured-window.wav').read_bytes()).hexdigest(),sourceHeadsZero=6,rows=rows)
    output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('directory',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.directory,a.output)
