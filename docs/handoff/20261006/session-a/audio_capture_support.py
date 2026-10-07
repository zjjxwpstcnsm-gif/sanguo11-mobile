#!/usr/bin/env python3
"""Optional true-projection capture, only after guarded test-package installation.
All captured PCM is raw. Initializing input is never a waveform acceptance.
"""
import json,pathlib,re,shlex,subprocess,tarfile,time,threading,xml.etree.ElementTree as ET
import device_session as ds
TEST=ds.PACKAGE+'.test';SERVICE='game.sanguo.mobile.SessionAAudioCaptureService';ACTIVITY='game.sanguo.mobile.SessionAAudioCaptureActivity'
def shell(command):return ds.run('shell',command).decode().strip()
_MIXER_MONITORS={}
_INPUT_ROUTE_MONITORS={}
def observe_input_route(out,r,record,phase):
 """Sequential shell observations, never proof of an atomic native route."""
 if not re.fullmatch('[A-Za-z0-9_-]{1,64}',phase):raise ValueError('Route observation phase')
 out=pathlib.Path(out).resolve()
 if not out.is_relative_to(ds.ROOT/'out/session-a'):raise ValueError('Owned route evidence required')
 if r.get('root')!=str(ds.ROOT) or r.get('serial')!='emulator-5554':raise ValueError('Route observation cohort/owner')
 rows=record.setdefault('inputRouteObservations',[])
 path=out/('input-route-'+record['run']+'-'+str(len(rows))+'-'+phase+'.json')
 sample={'phase':phase,'observedHostUnixStart':time.time(),'apks':r['apks'],
         'scope':'Read-only sequential AudioPolicy/AudioFlinger/projection/permission observations. Not atomic getInputForAttr-call state, capture/playback parity, old-22 reproduction or unique cause.'}
 commands={'deviceUptime':'cat /proc/uptime','audioPolicy':'dumpsys media.audio_policy',
           'audioFlinger':'dumpsys media.audio_flinger','mediaProjection':'dumpsys media_projection',
           'recordAppOp':'appops get '+TEST+' RECORD_AUDIO'}
 for name,command in commands.items():
  began=time.time()
  try:value={'text':ds.run('shell',command,timeout=10).decode(errors='replace')}
  except Exception as error:value={'unavailable':str(error)}
  sample[name]={'hostUnixStart':began,'hostUnixEnd':time.time(),**value}
 sample['observedHostUnixEnd']=time.time()
 with path.open('x') as f:json.dump(sample,f,indent=2)
 rows.append({'phase':phase,'path':str(path),'sha256':ds.digest(path),
              'unavailable':[name for name in commands if 'unavailable' in sample[name]]})
 return sample
def begin_input_route_observation(out,r,record,phase):
 """Never spend the raw PCM window waiting for five diagnostic shell calls."""
 if record['run'] in _INPUT_ROUTE_MONITORS:raise ValueError('Own route observer already active')
 record.setdefault('inputRouteObservations',[])
 record['inputRouteObserverRequestedHostUnix']=time.time()
 record['inputRouteObserverErrors']=[]
 record['inputRouteObserverJoined']=False
 def observe():
  try:observe_input_route(out,r,record,phase)
  except BaseException as error:record['inputRouteObserverErrors'].append(repr(error))
 thread=threading.Thread(target=observe,name='session-a-input-route-observation',daemon=True)
 _INPUT_ROUTE_MONITORS[record['run']]=thread;thread.start()
def end_input_route_observation(record):
 thread=_INPUT_ROUTE_MONITORS.get(record['run'])
 if thread is not None:
  # Five sequential calls have ten-second individual timeouts. Freeze the
  # record only after the sole own observer is done; never accept live hashes.
  thread.join(timeout=55)
  if thread.is_alive():raise RuntimeError('Own input-route observer still writing; retain evidence')
  _INPUT_ROUTE_MONITORS.pop(record['run'],None)
  record['inputRouteObserverJoined']=True
  if record['inputRouteObserverErrors']:raise RuntimeError('Own input-route observation failed: '+repr(record['inputRouteObserverErrors']))
def begin_mixer_measurement(out,record):
 path=out/('mixer-'+record['run']+'.jsonl');stop=threading.Event()
 record['mixerMeasurementPath']=str(path);record['mixerMeasurementScope']='read-only dumpsys AudioFlinger every5s, counters cumulative; only time-local changes eligible for attribution; no PCM rewrite/threshold change'
 def measure():
  began=time.monotonic()
  with path.open('x') as f:
   while not stop.is_set() and time.monotonic()-began<record['seconds']+30:
    sample={'observedHostUnix':time.time()}
    try:
     sample['deviceMonotonicUptime']=ds.run('shell','cat /proc/uptime',timeout=10).decode().strip()
     sample['audioFlinger']=ds.run('shell','dumpsys media.audio_flinger',timeout=15).decode()
    except Exception as e:sample['unavailable']=str(e)
    f.write(json.dumps(sample)+'\n');f.flush();stop.wait(5)
 thread=threading.Thread(target=measure,name='session-a-mixer-measurement',daemon=True)
 _MIXER_MONITORS[record['run']]=(stop,thread);thread.start()
def end_mixer_measurement(record):
 monitor=_MIXER_MONITORS.get(record['run'])
 if monitor is not None:
  stop,thread=monitor;stop.set();thread.join(timeout=20)
  if thread.is_alive():raise RuntimeError('Own mixer observer still writing; retain evidence and inspect restoration')
  _MIXER_MONITORS.pop(record['run'],None)
  path=pathlib.Path(record['mixerMeasurementPath']);record['mixerMeasurementSha256']=ds.digest(path)
  record['mixerObserverJoined']=True

def save(out,r): (out/'session.json').write_text(json.dumps(r,indent=2))
def prepare(out,r):
 if TEST in shell('ps -A'):raise ValueError('Test package already active')
 c={'trees':{},'permissionBefore':shell('dumpsys package '+TEST),'appOpBefore':shell('appops get '+TEST+' RECORD_AUDIO'),'stage':'before-install','captures':[]}
 c['grantedBefore']=bool(re.search(r'android.permission.RECORD_AUDIO: granted=true',c['permissionBefore']))
 m=re.search(r'RECORD_AUDIO:\s*(\w+)',c['appOpBefore']);c['modeBefore']=m.group(1) if m else 'default'
 for name,tree in {'test-internal':'/data/data/'+TEST,'test-external':'/sdcard/Android/data/'+TEST}.items():
  exists=shell('test -d '+shlex.quote(tree)+' && echo yes || true')=='yes';p=out/(name+'-before.tar')
  if exists:
   before=ds.device_manifest(tree)
   with p.open('wb') as f:ds.run('exec-out','tar','-C',tree,'-cf','-','.',output=f)
   files=ds.archive_manifest(p);assert ds.device_manifest(tree)==before=={k:v['sha256'] for k,v in files.items()}
  else:
   with tarfile.open(p,'w'):pass
   files={}
  c['trees'][name]={'path':tree,'existed':exists,'archive':str(p),'archiveSha256':ds.digest(p),'files':files}
 remote=shell('pm path '+TEST).removeprefix('package:');p=out/'previous-test-installed.apk'
 with p.open('wb') as f:ds.run('exec-out','cat',remote,output=f)
 c['previousApkPath']=str(p);c['previousApkSha256']=ds.digest(p);assert shell('sha256sum '+shlex.quote(remote)).split()[0]==c['previousApkSha256']
 r['audioCapture']=c;save(out,r)
def start(out,r,rate,run,seconds):
 if not re.fullmatch('[A-Za-z0-9_]{1,64}',run) or rate not in [44100,48000]:raise ValueError('Capture identity/rate')
 shell('pm grant '+TEST+' android.permission.RECORD_AUDIO')
 c=r['audioCapture'];c['appOpBeforeThisCapture']=shell('appops get '+TEST+' RECORD_AUDIO');shell('appops set '+TEST+' RECORD_AUDIO allow');c['appOpDuring']=shell('appops get '+TEST+' RECORD_AUDIO');assert re.search(r'RECORD_AUDIO:\s*allow',c['appOpDuring']);c['permissionDuring']=shell('dumpsys package '+TEST);assert re.search(r'android.permission.RECORD_AUDIO: granted=true',c['permissionDuring'])
 path='/sdcard/Android/data/'+TEST+'/files/session-a-game-mix/'+run
 record={'run':run,'rate':rate,'seconds':seconds,'devicePath':path,'ready':False,'consentFromObservedSystemUi':False};c['captures'].append(record);save(out,r)
 observe_input_route(out,r,record,'before-projection-request');save(out,r)
 shell('am start -n '+TEST+'/'+ACTIVITY+' --es run '+run+' --ei seconds '+str(seconds)+' --ei sampleRate '+str(rate))
 end=time.monotonic()+30
 while time.monotonic()<end:
  raw=shell('uiautomator dump /data/local/tmp/session_a_projection.xml >/dev/null 2>&1; cat /data/local/tmp/session_a_projection.xml')
  (out/(run+'-consent.xml')).write_text(raw)
  tree=ET.fromstring(raw)
  if 'MediaProjectionPermissionActivity' not in shell('dumpsys activity top'):
   time.sleep(1);continue
  candidates=[n for n in tree.iter('node') if n.get('package')=='com.android.systemui' and n.get('enabled')=='true' and n.get('clickable')=='true' and (n.get('resource-id','').endswith('/start_button') or n.get('text') in ['Start now','立即开始','开始录制'])]
  if candidates:
   node=candidates[0];bounds=list(map(int,re.findall(r'\d+',node.get('bounds',''))));assert len(bounds)==4
   record['consentNode']=node.attrib;ds.run('shell','input','tap',str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2));record['consentFromObservedSystemUi']=True;save(out,r);break
  time.sleep(1)
 if not record['consentFromObservedSystemUi']:raise ValueError('Actual system MediaProjection consent button not observed')
 begin_input_route_observation(out,r,record,'after-consent-background');save(out,r)
 end=time.monotonic()+20
 while time.monotonic()<end:
  ready=shell('cat '+shlex.quote(path+'/ready.json')+' 2>/dev/null || true');result=shell('cat '+shlex.quote(path+'/result.json')+' 2>/dev/null || true')
  if result:record['result']=json.loads(result);record['initialResultObservedHostUnix']=time.time();save(out,r);return record
  if ready:record['ready']=True;record['readyData']=json.loads(ready);record['readyReceiptObservedHostUnix']=time.time();begin_mixer_measurement(out,record);save(out,r);return record
  time.sleep(.3)
 raise ValueError('No initialized capture or failure receipt')
def collect(out,r,record,timeout=20):
 path=record['devicePath'];end=time.monotonic()+timeout
 while time.monotonic()<end:
  raw=shell('cat '+shlex.quote(path+'/result.json')+' 2>/dev/null || true')
  if raw:
   end_mixer_measurement(record);record['result']=json.loads(raw);end_input_route_observation(record);observe_input_route(out,r,record,'final-result-observed');dest=out/('audio-'+record['run']);ds.run('pull',path,str(dest));record['hostPath']=str(dest);save(out,r)
   until=time.monotonic()+10
   while SERVICE in shell('dumpsys activity services '+TEST) and time.monotonic()<until:time.sleep(.5)
   if SERVICE in shell('dumpsys activity services '+TEST):raise ValueError('Actual previous capture service not yet stopped')
   return
  time.sleep(.5)
 raise ValueError('Capture end receipt missing; retained raw evidence')
def stop_and_restore(out,r):
 c=r.get('audioCapture')
 if c is None:return
 if SERVICE in shell('dumpsys activity services '+TEST):shell('am stopservice -n '+TEST+'/'+SERVICE)
 else:c['serviceAlreadyStoppedAtRollback']=True;save(out,r)
 for record in c['captures']:
  end_mixer_measurement(record)
  end_input_route_observation(record)
  if 'hostPath' not in record:
   try:collect(out,r,record,20)
   except Exception as error:record['collectionError']=str(error);save(out,r)
 shell('am force-stop '+TEST)
 p=pathlib.Path(c['previousApkPath']);assert ds.digest(p)==c['previousApkSha256'];ds.run('install','-r',str(p),timeout=300)
 remote=shell('pm path '+TEST).removeprefix('package:');assert shell('sha256sum '+shlex.quote(remote)).split()[0]==c['previousApkSha256']
 if c['grantedBefore']:shell('pm grant '+TEST+' android.permission.RECORD_AUDIO')
 else:
  # Replacing the original undeclared-permission test APK normally revokes it.
  if re.search(r'android.permission.RECORD_AUDIO: granted=true',shell('dumpsys package '+TEST)):shell('pm revoke '+TEST+' android.permission.RECORD_AUDIO')
 shell('appops set '+TEST+' RECORD_AUDIO '+c['modeBefore'])
 c['permissionAfter']=shell('dumpsys package '+TEST);c['appOpAfter']=shell('appops get '+TEST+' RECORD_AUDIO')
 assert bool(re.search(r'android.permission.RECORD_AUDIO: granted=true',c['permissionAfter']))==c['grantedBefore']
 mode=re.search(r'RECORD_AUDIO:\s*(\w+)',c['appOpAfter']);assert (mode.group(1) if mode else 'default')==c['modeBefore']
 c['restoration']={}
 for name,record in c['trees'].items():
  tree=record['path'];p=pathlib.Path(record['archive']);assert ds.digest(p)==record['archiveSha256'];expected={k:v['sha256'] for k,v in record['files'].items()}
  exists=shell('test -d '+shlex.quote(tree)+' && echo yes || true')=='yes';current=ds.device_manifest(tree) if exists else {}
  for file in current.keys()-expected.keys():ds.run('shell','rm','--',tree+'/'+file)
  if expected and current!=expected:
   with p.open('rb') as f:ds.run('shell','-T','tar','-C',tree,'-xf','-',input=f)
  actual=ds.device_manifest(tree) if exists else {};assert actual==expected
  c['restoration'][name]={'exactRegularFileSha':True,'files':len(actual),'directoryOriginallyExisted':record['existed']}
 c['stage']='restored-verified';c['note']='record permission and app-op mode restored; access timestamps are observed history, not rewound';save(out,r)
