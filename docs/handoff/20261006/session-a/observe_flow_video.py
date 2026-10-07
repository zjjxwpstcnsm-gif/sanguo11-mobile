#!/usr/bin/env python3
"""Record only this exclusive5554 normal-flow session; no app data changes.
Video encoder runs in a separate process. Memory samples are recorded independently.
"""
import argparse,hashlib,json,pathlib,re,shlex,subprocess,time
ADB='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platform-tools/adb'
ROOT=pathlib.Path(__file__).resolve().parents[4]
p=argparse.ArgumentParser();p.add_argument('--session',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,required=True);p.add_argument('--max-parts',type=int,default=24);a=p.parse_args()
if not 1<=a.max_parts<=240:raise ValueError('Raw recording part limit must be1..240')
session=a.session.resolve();own=json.loads(pathlib.Path('/tmp/sanguo11-emulator-5554-session-a.lock/owner.json').read_text())
assert own['root']==str(ROOT) and own['output']==str(session.parent)
name=session.parent.name.replace('-','_');assert re.fullmatch('[a-zA-Z0-9_]+',name)
def adb(*args,**kw):return subprocess.check_output([ADB,'-s','emulator-5554',*args],timeout=40,**kw)
def shell(cmd):return adb('shell',cmd,text=True).strip()
def state():return json.loads(session.read_text())['stage']
a.output.mkdir(parents=True,exist_ok=False);device='/data/local/tmp/session_a_video_'+name;shell('mkdir '+shlex.quote(device));report={'scope':'actual exclusive normal UI recording; separate encoder process, no app data/config changes; not ARM/PC pixel oracle','session':str(session),'parts':[]}
with (a.output/'recording.jsonl').open('x') as log:
 for index in range(a.max_parts):
  while state() in ['backup','backup-verified','installing']:
   time.sleep(2)
  if state()=='restored-verified':break
  target=device+'/part'+str(index+1).zfill(2)+'.mp4';local=a.output/pathlib.Path(target).name
  began=time.time();pid=shell('screenrecord --bit-rate 4000000 --time-limit 180 '+shlex.quote(target)+' > '+shlex.quote(target+'.log')+' 2>&1 & echo $!');assert pid.isdigit()
  while time.time()-began<195:
   command=shell('cat /proc/'+pid+'/cmdline 2>/dev/null || true').replace('\x00',' ')
   if 'screenrecord' not in command or target not in command:break
   if state()=='restored-verified':
    shell('kill -2 '+pid);time.sleep(2);break
   time.sleep(2)
  command=shell('cat /proc/'+pid+'/cmdline 2>/dev/null || true').replace('\x00',' ')
  if 'screenrecord' in command and target in command:shell('kill -2 '+pid);time.sleep(2)
  record={'part':index+1,'beginUnix':began,'endUnix':time.time(),'recordPid':pid,'devicePath':target,'stageAtEnd':state()}
  try:
   record['deviceSha256']=shell('sha256sum '+shlex.quote(target)).split()[0];adb('pull',target,str(local),stderr=subprocess.STDOUT)
   record['sha256']=hashlib.sha256(local.read_bytes()).hexdigest();record['bytes']=local.stat().st_size;assert record['sha256']==record['deviceSha256'];record['path']=str(local.resolve())
  except Exception as error:record['error']=str(error)
  log.write(json.dumps(record)+'\n');log.flush();report['parts'].append(record)
  (a.output/'video.json').write_text(json.dumps(report,indent=2)+'\n')
  if state()=='restored-verified':break
