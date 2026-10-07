#!/usr/bin/env python3
"""Record only this exclusive5554 normal-flow session; no app data changes.
Video encoder runs in a separate process. Memory samples are recorded independently.
"""
import argparse,hashlib,json,pathlib,re,shlex,subprocess,time,uuid,signal,atexit
ADB='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platform-tools/adb'
ROOT=pathlib.Path(__file__).resolve().parents[4]
p=argparse.ArgumentParser();p.add_argument('--session',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,required=True);p.add_argument('--max-parts',type=int,default=24);p.add_argument('--remove-verified-device-parts',action='store_true');a=p.parse_args()
if not 1<=a.max_parts<=240:raise ValueError('Raw recording part limit must be1..240')
session=a.session.resolve();own=json.loads(pathlib.Path('/tmp/sanguo11-emulator-5554-session-a.lock/owner.json').read_text())
assert own['root']==str(ROOT) and own['output']==str(session.parent)
name=session.parent.name.replace('-','_');assert re.fullmatch('[a-zA-Z0-9_]+',name)
def adb(*args,**kw):return subprocess.check_output([ADB,'-s','emulator-5554',*args],timeout=40,**kw)
def shell(cmd):return adb('shell',cmd,text=True).strip()
def receipt():
 for attempt in range(30):
  try:
   value=json.loads(session.read_text())
   assert value['root']==str(ROOT) and value['serial']=='emulator-5554'
   return value
  except json.JSONDecodeError:
   if attempt==29:raise
   time.sleep(.1)
def state():return receipt()['stage']
initial=receipt();assert initial['stage']=='installed-verified'
stop_requested=False
def request_stop(signum,frame):
 global stop_requested
 stop_requested=True
signal.signal(signal.SIGTERM,request_stop)
a.output.mkdir(parents=True,exist_ok=False);device='/data/local/tmp/session_a_video_'+name+'_'+uuid.uuid4().hex;shell('mkdir '+shlex.quote(device));report={'scope':'actual exclusive exact-cohort normal UI recording; encoder/host processing can perturb performance, startup omission/segment gaps retained; no app data/config changes, no PC/ARM pixel/timing/audio acceptance','session':str(session),'apks':initial['apks'],'deviceCaptureDirectory':device,'parts':[]}
active_encoder={}
def stop_own_encoder_at_exit():
 if not active_encoder:return
 pid,target=active_encoder['pid'],active_encoder['target']
 command=shell('cat /proc/'+pid+'/cmdline 2>/dev/null || true').replace('\x00',' ')
 if 'screenrecord' in command and target in command:shell('kill -2 '+pid)
atexit.register(stop_own_encoder_at_exit)
with (a.output/'recording.jsonl').open('x') as log:
 for index in range(a.max_parts):
  if stop_requested:break
  if receipt()['apks']!=report['apks']:raise ValueError('Actual recording APK cohort changed')
  while state() in ['backup','backup-verified','installing']:
   time.sleep(2)
  if state()=='restored-verified':break
  target=device+'/part'+str(index+1).zfill(2)+'.mp4';local=a.output/pathlib.Path(target).name
  began=time.time();pid=shell('screenrecord --bit-rate 4000000 --time-limit 180 '+shlex.quote(target)+' > '+shlex.quote(target+'.log')+' 2>&1 & echo $!');assert pid.isdigit()
  active_encoder.update(pid=pid,target=target)
  while time.time()-began<195:
   command=shell('cat /proc/'+pid+'/cmdline 2>/dev/null || true').replace('\x00',' ')
   if 'screenrecord' not in command or target not in command:break
   if stop_requested or state()=='restored-verified':
    shell('kill -2 '+pid);time.sleep(2);break
   time.sleep(2)
  command=shell('cat /proc/'+pid+'/cmdline 2>/dev/null || true').replace('\x00',' ')
  if 'screenrecord' in command and target in command:shell('kill -2 '+pid);time.sleep(2)
  record={'part':index+1,'beginUnix':began,'endUnix':time.time(),'recordPid':pid,'devicePath':target,'stageAtEnd':state(),'hostStopRequested':stop_requested}
  try:
   record['deviceSha256']=shell('sha256sum '+shlex.quote(target)).split()[0];adb('pull',target,str(local),stderr=subprocess.STDOUT)
   record['sha256']=hashlib.sha256(local.read_bytes()).hexdigest();record['bytes']=local.stat().st_size;assert record['sha256']==record['deviceSha256'];record['path']=str(local.resolve())
   if a.remove_verified_device_parts:
    assert target.startswith(device+'/') and re.fullmatch('part[0-9]+.mp4',pathlib.Path(target).name)
    record['deviceSha256AfterPull']=shell('sha256sum '+shlex.quote(target)).split()[0]
    assert record['deviceSha256AfterPull']==record['sha256']
    # Only our fresh UUID recording file, never save/library/preferences/resources.
    shell('rm -- '+shlex.quote(target));record['verifiedDevicePartRemovedAfterPull']=True
  except Exception as error:record['error']=str(error)
  log.write(json.dumps(record)+'\n');log.flush();report['parts'].append(record)
  (a.output/'video.json').write_text(json.dumps(report,indent=2)+'\n')
  if stop_requested or state()=='restored-verified':break
report['endedStage']=state();report['hostStopRequested']=stop_requested;report['captureLimitReachedBeforeRestoration']=len(report['parts'])==a.max_parts and state()!='restored-verified'
(a.output/'video.json').write_text(json.dumps(report,indent=2)+'\n')
