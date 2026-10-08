#!/usr/bin/env python3
"""Unchanged Android decoder PCM versus original reference; diagnostic never playback pass."""
from pathlib import Path
import json,subprocess,wave,sys
import numpy as np
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/android-music371'
sys.path.insert(0,str(D));from run_remaining_normal_media import sha

def main():
 assert not OUT.exists();OUT.mkdir(parents=True);e=json.loads((D/'ANDROID_MUSIC_DECODE372.json').read_text());assert e['stage']=='restored-verified';pcm=Path(e['pcmPath']);assert sha(pcm)==e['pcmSha256'];ref=ROOT/'out/session-a/music-submission-appop-44/reference/2238.wav';manifest=json.loads((ref.parent/'manifest.json').read_text());row=next(x for x in manifest['entries'] if x['resourceId']==2238);assert sha(ref)==row['wavSha256']
 android=OUT/'android-original2238.wav'
 with wave.open(str(android),'wb') as w:w.setnchannels(2);w.setsampwidth(2);w.setframerate(44100);w.writeframes(pcm.read_bytes())
 a=np.frombuffer(pcm.read_bytes(),'<i2').astype(np.float64).reshape(-1,2)
 with wave.open(str(ref),'rb') as w:assert w.getframerate()==44100 and w.getnchannels()==2;a_ref=np.frombuffer(w.readframes(w.getnframes()),'<i2').astype(np.float64).reshape(-1,2)
 assert a.shape==a_ref.shape==(3904512,2)
 windows=[]
 for start in range(0,len(a),44100):
  x=a[start:start+44100].ravel();y=a_ref[start:start+44100].ravel();dot=float(x@y);denom=np.sqrt(float(x@x)*float(y@y));windows.append({'startFrame':start,'frames':len(x)//2,'correlationSameFrame':dot/denom if denom>0 else None,'maximumAbsoluteSampleDifference':float(np.max(np.abs(x-y)))})
 raw_a=a.ravel();raw_b=a_ref.ravel();raw_score=float(raw_a@raw_b)/np.sqrt(float(raw_a@raw_a)*float(raw_b@raw_b))
 target=OUT/'fixed-original-windows.json';r=subprocess.run([sys.executable,str(ROOT/'tools/audio/diagnose_music_timeline.py'),str(android),str(ref),'--output',str(target)],capture_output=True,text=True);(OUT/'window-log.txt').write_text(r.stdout+r.stderr);assert r.returncode==0,r.stderr
 whole=subprocess.run([sys.executable,str(ROOT/'tools/audio/check_pc_music_mix.py'),str(android),str(ref),'--resource','2238','--manifest',str(ref.parent/'manifest.json'),'--output',str(OUT/'whole-decoder-check.json')],capture_output=True,text=True);(OUT/'whole-decoder-check.log').write_text(whole.stdout+whole.stderr)
 assert sha(pcm)==e['pcmSha256'] and sha(ref)==row['wavSha256'];report={'androidDecoderPcmSha256':sha(pcm),'originalReferenceSha256':sha(ref),'sameFrameWholeStereoCorrelation':float(raw_score),'all89DeclaredOneSecondWindows':windows,'fixed17OriginalWindows':json.loads(target.read_text()),'strictOriginalWholeCheckerExitCode':whole.returncode,'strictOriginalWholeCheckerLog':whole.stdout+whole.stderr,'wholeDecoderResult':json.loads((OUT/'whole-decoder-check.json').read_text()) if (OUT/'whole-decoder-check.json').exists() else None,'derivedWavIsExactPcmHeaderOnly':True,'derivedWavSha256':sha(android),'originalInputsUnchanged':True,'normalMenuPlaybackAccepted':False,'scope':'Actual Android decoded complete2238 byte stream versus unchanged original FFMpeg reference. Diagnostics of codec/source conversion only; no audio output/normal scene/mixer/ARM acceptance. Original waveform0.995 unchanged; never splice/timewarp/drop to accept output.','wholeGoalComplete':False};(D/'ANDROID_MUSIC_COMPARE371.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'sameFrameWholeStereoCorrelation':raw_score,'strictCheckerExitCode':whole.returncode,'offsets':[x['offsetSamples'] for x in report['fixed17OriginalWindows']['rows']]}))
if __name__=='__main__':main()
