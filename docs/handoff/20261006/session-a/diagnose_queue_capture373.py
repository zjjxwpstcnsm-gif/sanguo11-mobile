#!/usr/bin/env python3
"""Original raw342 PCM timeline, failed fullgate and actual player/recorder metrics."""
from pathlib import Path
import json,subprocess,sys
from read_session_state import read_session_state
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/capture-timeline373';CASE=ROOT/'out/session-a/capture-queue366/large-menu-pcm'
def main():
 assert not OUT.exists();OUT.mkdir(parents=True);state=read_session_state(CASE/'session.json');assert state['stage']=='restored-verified';capture=next(x for x in state['audioCapture']['captures'] if x['run'].endswith('_menu'));wav=Path(capture['hostPath'])/'android-mix.wav';assert sha(wav)==capture['result']['wavSha256'];ref=ROOT/'out/session-a/music-submission-appop-44/reference/2238.wav';manifest=read_session_state(ref.parent/'manifest.json');entry=next(x for x in manifest['entries'] if x['resourceId']==2238);assert sha(ref)==entry['wavSha256'];python=Path('/Users/paopao/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3')
 target=OUT/'fixed-windows.json'
 with (OUT/'diagnostic.log').open('w') as f:r=subprocess.run([str(python),str(ROOT/'tools/audio/diagnose_music_timeline.py'),str(wav),str(ref),'--output',str(target)],stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode==0,(OUT/'diagnostic.log').read_text();rows=read_session_state(target);assert len(rows['rows'])==17;assert sha(wav)==capture['result']['wavSha256'];assert sha(ref)==entry['wavSha256']
 report={'wholeTrackAccepted':False,'wholeGateThresholdUnchanged':.995,'actualWholeGateFailureLog':(CASE/'whole-music-check.log').read_text(),'wholeFailureLogSha256':sha(CASE/'whole-music-check.log'),'actualMenuMetadata':read_session_state(CASE/'evidence/menu-music.json'),'rawCaptureResult':capture['result'],'fixedWindowsDiagnostic':rows,'unchangedRawWavSha256':sha(wav),'originalReferenceSha256':sha(ref),'rawEditedOrTimeAlignedForAcceptance':False,'uniqueResponsibleComponentProven':False,'scope':'Actual366 original2238 full waveform correlationFAIL; fixed17 original windows identify discontinuity hints only. Player underruns and recorder/mixer timestamps remain independent evidence, not unique causal attribution. No PCM edits/splices/timewarp or weaker0.995, no unmodified reinstall retry or ARM/source BGM acceptance.','wholeGoalComplete':False};(DOC/'CAPTURE_TIMELINE373.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'wholeTrackAccepted':False,'originalFirstLoopUnderruns':report['actualMenuMetadata']['originalFirstLoopUnderruns'],'offsets':[x['offsetSamples'] for x in rows['rows']]}),flush=True)
if __name__=='__main__':main()
