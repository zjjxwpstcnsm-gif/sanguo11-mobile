#!/usr/bin/env python3
"""Optional true-projection capture, only after guarded test-package installation.
All captured PCM is raw. Initializing input is never a waveform acceptance.
"""
import json,pathlib,re,shlex,subprocess,tarfile,time,xml.etree.ElementTree as ET
import device_session as ds
TEST=ds.PACKAGE+'.test';SERVICE='game.sanguo.mobile.SessionAAudioCaptureService';ACTIVITY='game.sanguo.mobile.SessionAAudioCaptureActivity'
def shell(command):return ds.run('shell',command).decode().strip()
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
 c=r['audioCapture'];c['permissionDuring']=shell('dumpsys package '+TEST);assert re.search(r'android.permission.RECORD_AUDIO: granted=true',c['permissionDuring'])
 path='/sdcard/Android/data/'+TEST+'/files/session-a-game-mix/'+run
 record={'run':run,'rate':rate,'seconds':seconds,'devicePath':path,'ready':False,'consentFromObservedSystemUi':False};c['captures'].append(record);save(out,r)
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
 end=time.monotonic()+20
 while time.monotonic()<end:
  ready=shell('cat '+shlex.quote(path+'/ready.json')+' 2>/dev/null || true');result=shell('cat '+shlex.quote(path+'/result.json')+' 2>/dev/null || true')
  if result:record['result']=json.loads(result);save(out,r);return record
  if ready:record['ready']=True;record['readyData']=json.loads(ready);save(out,r);return record
  time.sleep(.3)
 raise ValueError('No initialized capture or failure receipt')
def collect(out,r,record,timeout=20):
 path=record['devicePath'];end=time.monotonic()+timeout
 while time.monotonic()<end:
  raw=shell('cat '+shlex.quote(path+'/result.json')+' 2>/dev/null || true')
  if raw:
   record['result']=json.loads(raw);dest=out/('audio-'+record['run']);ds.run('pull',path,str(dest));record['hostPath']=str(dest);save(out,r)
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
