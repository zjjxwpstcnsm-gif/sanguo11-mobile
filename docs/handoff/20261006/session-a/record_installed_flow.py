#!/usr/bin/env python3
"""Fresh installed normal gesture video; unique own tmp file, exact readback SHA."""
import argparse,pathlib,json,time,subprocess,hashlib,uuid
ADB='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platform-tools/adb'
p=argparse.ArgumentParser();p.add_argument('--mp4',action='store_true');p.add_argument('--session',type=pathlib.Path,required=True);p.add_argument('--apk-sha',required=True);p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False)
started=time.monotonic();report={'apkSha256':a.apk_sha,'videoHasAudio':False,'nativeTimingAvailable':False,'goalComplete':False}
def run(*args,**kwargs):return subprocess.run([ADB,'-s','emulator-5554',*args],check=True,timeout=kwargs.pop('timeout',30),**kwargs)
while time.monotonic()-started<1800:
 d=json.loads(a.session.read_text())
 if d['stage']=='restored-verified':raise RuntimeError('Normal gesture recording did not start')
 if d['stage']=='installed-verified':
  installed=(a.session.parent/'game.sanguo.mobile.dev-installed.sha256').read_text().split()[0];assert installed==a.apk_sha
  relative='/sdcard/Android/data/game.sanguo.mobile.dev/files/session-a-map/'+d['runId']+'/memory.csv'
  result=subprocess.run([ADB,'-s','emulator-5554','shell','cat',relative],stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,timeout=30)
  if '\nzoom,' in result.stdout:break
 time.sleep(2)
else:raise TimeoutError('No normal actual gesture')
remote='/data/local/tmp/session-a-live-'+uuid.uuid4().hex+('.mp4' if a.mp4 else '.h264');local=a.output/('actual-gestures.mp4' if a.mp4 else 'actual-gestures.h264');before=time.time()
r=subprocess.run([ADB,'-s','emulator-5554','shell','screenrecord']+([] if a.mp4 else ['--output-format=h264'])+['--size','540x960','--bit-rate','2000000','--time-limit','60',remote],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=100,text=True)
report.update(recordReturnCode=r.returncode,recordOutput=r.stdout,recordStartUtcEpoch=before,recordEndUtcEpoch=time.time(),normalRun=d['runId'],nativeTimingAvailable=False)
try:
 remote_sha=run('shell','sha256sum',remote,stdout=subprocess.PIPE,text=True).stdout.split()[0];run('pull',remote,str(local),stdout=subprocess.PIPE)
 digest=hashlib.sha256(local.read_bytes()).hexdigest();assert digest==remote_sha
 report.update(nativeTimingAvailable=a.mp4,recordingFormat='native MP4 container' if a.mp4 else 'elementary H264',file=str(local.resolve()),sha256=digest,bytes=local.stat().st_size,hostDeviceByteEqual=True)
 run('shell','rm','--',remote);report['onlyOwnRemoteRemoved']=True
finally:(a.output/'record.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False))
