#!/usr/bin/env python3
"""Session B exclusive actual APK/widget test with complete internal/external user guards."""
import argparse,hashlib,io,json,os,subprocess,tarfile,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];PACKAGE='game.sanguo.mobile.dev'
def sha(raw):return hashlib.sha256(raw).hexdigest()
def members(path):
 rows={}
 with tarfile.open(path,'r|')as archive:
  for m in archive:
   if not m.isfile():continue
   if m.name.startswith('/')or '..'in Path(m.name).parts:raise ValueError('Unsafe archive')
   digest=hashlib.sha256();f=archive.extractfile(m)
   for b in iter(lambda:f.read(1024*1024),b''):digest.update(b)
   rows[m.name]=dict(bytes=m.size,sha256=digest.hexdigest())
 return rows
def run(a):
 runner={'capacity':'game.sanguo.mobile.SessionBCapacityInstrumentation','governor':'game.sanguo.mobile.SessionBGovernorInstrumentation','fieldworks':'game.sanguo.mobile.SessionBFieldworksInstrumentation'}[a.runner]
 marker={'capacity':'PASS SESSION B CAPACITY','governor':'PASS SESSION B GOVERNOR','fieldworks':'PASS SESSION B FIELDWORKS'}[a.runner]
 if a.serial!='emulator-5582':raise ValueError('B target is5582; no other device authorized by this tool')
 guard=json.loads(a.source_guard.read_text())
 from session_b_freeze_apk import paths
 def guarded(name):return name.startswith(('app/src/main/','core/src/main/','game-api/src/main/','game-runtime/src/main/','out/pc-native-runtime/','out/session-b/readonly-theme-dependencies/','out/session-b/cache-core-stage/'))or name in ['app/build.gradle','build.gradle','settings.gradle','gradle.properties','version.properties']
 actual_paths={name for name in paths()if guarded(name)}
 if any(name.startswith('out/session-b/cache-core-stage/')for name in guard):
  stage=ROOT/'out/session-b/cache-core-stage'
  actual_paths.update(str(p.relative_to(ROOT))for p in stage.rglob('*')if p.is_file())
 if actual_paths!=set(guard):raise ValueError('Frozen APK production input path set differs: '+repr(sorted(actual_paths^set(guard))))
 changed=[name for name,digest in guard.items()if sha((ROOT/name).read_bytes())!=digest]
 if changed:raise ValueError('Frozen APK source no longer matches: '+repr(changed))
 output=a.output.resolve()
 if ROOT/'out/session-b' not in output.parents:raise ValueError('Use session-b ignored output')
 output.mkdir(parents=True,exist_ok=False)
 lock=Path('/tmp/sanguo11-'+a.serial+'.lock');lock.mkdir()
 owner=dict(branch='codex/rules-content-contest-repair',cwd=str(ROOT),pid=os.getpid(),started=time.time());(lock/'owner.json').write_text(json.dumps(owner))
 adb=[str(ROOT/'out/toolchain/android-sdk/platform-tools/adb'),'-s',a.serial]
 def command(*parts,timeout=180):return subprocess.run(adb+list(parts),check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=timeout).stdout
 def archive(remote,path):
  with path.open('wb')as f:subprocess.run(adb+['exec-out','tar','-C',remote,'-cf','-','.'],check=True,stdout=f,stderr=subprocess.PIPE,timeout=300)
  return members(path)
 internal='/data/data/'+PACKAGE;external='/sdcard/Android/data/'+PACKAGE
 report=dict(passed=False,serial=a.serial,owner=owner,apks={str(p):sha(p.read_bytes())for p in [a.apk,a.test_apk]},device=command('shell','getprop').decode(),scope=({'capacity':'actual source0/native58/13000/new/deploy/turns/save/cold','governor':'actual source0/two factions/menu/administration/transfer/capture/save/cold','fieldworks':'actual source14/new/deploy/map military widgets'}[a.runner])+'; separate from core probe and ARM',runner=runner)
 def save():(output/'results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
 backed=False
 try:
  if command('shell','id','-u').strip()!=b'0':raise ValueError('Root required for full user backup')
  state=command('shell','dumpsys','activity','activities').decode();(output/'device-before.txt').write_text(state)
  processes=command('shell','ps','-A').decode();(output/'processes-before.txt').write_text(processes)
  if any(PACKAGE in line for line in processes.splitlines()):raise ValueError('5582 package active; defer rather than interrupt another run')
  report['before']={}
  for label,remote in [('internal',internal),('external',external)]:report['before'][label]=archive(remote,output/(label+'-before.tar'))
  backed=True;save()
  for label in ['internal','external']:command('push',str(output/(label+'-before.tar')),'/data/local/tmp/session-b-'+label+'-backup.tar')
  for p in [a.apk,a.test_apk]:
   with (output/'install.txt').open('ab')as f:f.write(command('install','-r',str(p.resolve())))
  for package,p in [(PACKAGE,a.apk),(PACKAGE+'.test',a.test_apk)]:
   actual=command('shell','pm','path',package).decode().strip().removeprefix('package:');digest=sha(command('exec-out','cat',actual,timeout=300));report.setdefault('installed',{})[package]=digest
   if digest!=report['apks'][str(p)]:raise ValueError('Installed APK SHA differs')
  save()
  with (output/'instrumentation.txt').open('wb')as f:
   result=subprocess.run(adb+['shell','am','instrument','-w',PACKAGE+'.test/'+runner],stdout=f,stderr=subprocess.STDOUT,timeout=7200)
  text=(output/'instrumentation.txt').read_text();report['instrumentationPassed']=result.returncode==0 and marker in text and 'FAIL'not in text
  if report['instrumentationPassed']:
   command('shell','am','force-stop',PACKAGE)
   gone=subprocess.run(adb+['shell','pidof',PACKAGE],stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=30)
   if gone.stdout.strip():raise ValueError('Own process still active before cold continuation')
   with (output/'cold-instrumentation.txt').open('wb')as f:
    cold=subprocess.run(adb+['shell','am','instrument','-w','-e','mode','cold',PACKAGE+'.test/'+runner],stdout=f,stderr=subprocess.STDOUT,timeout=900)
   cold_text=(output/'cold-instrumentation.txt').read_text();report['coldPassed']=cold.returncode==0 and marker+' COLD'in cold_text and 'FAIL'not in cold_text
   report['instrumentationPassed']=report['instrumentationPassed']and report['coldPassed'];save()
  command('pull',external+'/files/session-b',str(output/'evidence'))
 finally:
  if backed:
   command('shell','am','force-stop',PACKAGE)
   report['restoration']={}
   for label,remote in [('internal',internal),('external',external)]:
    current=archive(remote,output/(label+'-post-test.tar'));added=set(current)-set(report['before'][label]);removed=[]
    # Only added files authored during this isolated test are removable. Preserve unrelated additions.
    for name in sorted(added):
     plain=name.removeprefix('./')
     if label=='external'and plain.startswith('files/session-b/')or label=='internal'and (plain in {'files/auto.sg11','files/manual3.sg11','files/customOfficers/library.json'}or plain.startswith('shared_prefs/')):
      command('shell','rm',remote+'/'+plain);removed.append(name)
    command('shell','tar','-C',remote,'-xf','/data/local/tmp/session-b-'+label+'-backup.tar')
    actual=archive(remote,output/(label+'-restored.tar'));bad=[name for name,row in report['before'][label].items()if actual.get(name)!=row]
    report['restoration'][label]=dict(originalFiles=len(report['before'][label]),shaReadbackMatches=not bad,mismatches=bad,removedTestFiles=removed,preservedAdditionalFiles=sorted(set(actual)-set(report['before'][label])))
   report['passed']=report.get('instrumentationPassed',False)and all(row['shaReadbackMatches']for row in report['restoration'].values());save()
  if (lock/'owner.json').exists()and json.loads((lock/'owner.json').read_text())==owner:(lock/'owner.json').unlink();lock.rmdir()
 if not report.get('passed'):raise SystemExit(1)
 print('PASS Session B actual APK/widgets and complete original user SHA restoration',flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--serial',required=True);p.add_argument('--runner',choices=['fieldworks','capacity','governor'],default='fieldworks')
 for name in ['apk','test-apk','source-guard','output']:p.add_argument('--'+name,type=Path,required=True)
 run(p.parse_args())
