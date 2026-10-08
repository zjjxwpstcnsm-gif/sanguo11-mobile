#!/usr/bin/env python3
"""Read-only Android10 framework path explains requested/actual capture buffer discrepancy."""
from pathlib import Path
import urllib.request,base64,hashlib,json
D=Path(__file__).resolve().parent
BASE='https://android.googlesource.com/platform/frameworks/base/+/android-10.0.0_r1/'
def source(path):return base64.b64decode(urllib.request.urlopen(BASE+path+'?format=TEXT',timeout=25).read()).decode()
def main():
 out=D/'CAPTURE_PLATFORM360.json';assert not out.exists();a=source('media/java/android/media/AudioRecord.java');b=source('media/java/android/media/audiopolicy/AudioPolicy.java')
 start=a.index('private @NonNull AudioRecord buildAudioPlaybackCaptureRecord()');end=a.index('/**',start);flow=a[start:end];assert 'mBufferSizeInBytes' not in flow and 'createAudioRecordSink(audioMix)' in flow
 start=b.index('public AudioRecord createAudioRecordSink(');end=b.index('/**',start);sink=b[start:end];assert 'getMinBufferSize' in sink
 state=json.loads((D.parents[3]/'out/session-a/capture-menu358/large-menu-pcm/session.json').read_text());trials=[{'run':x['run'],'rate':x['rate'],'requestedFrames':x['rate']*2,'actualFrames':x['result']['actualRecorderBufferFrames']} for x in state['audioCapture']['captures'] if x.get('result')]
 report={'android10Tag':'android-10.0.0_r1','source':[{'url':BASE+'media/java/android/media/AudioRecord.java','decodedSha256':hashlib.sha256(a.encode()).hexdigest(),'exactMethod':flow},{'url':BASE+'media/java/android/media/audiopolicy/AudioPolicy.java','decodedSha256':hashlib.sha256(b.encode()).hexdigest(),'exactMethod':sink}],'actualFresh358Trials':trials,'playbackCaptureBuilderIgnoresRequestedBufferInThisFrameworkPath':True,'actualDeviceBuildExactTagNotProven':True,'frameworkRootExplanationConsistentWithMeasuredBuffer':True,'wholeMusicAcceptance':False,'scope':'Android platform only; not PC game truth or unique timeline jump cause. Recorder getBufferSize actual2820/3072 kept; no pretend2sec ring. Disk512KiB still real candidate, output whole0.995 pending. No source/device mutations.'}
 out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'trials':trials,'builderBufferForwarded':False}))
if __name__=='__main__':main()
