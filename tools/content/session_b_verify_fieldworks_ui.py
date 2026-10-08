#!/usr/bin/env python3
"""Session B exclusive actual APK/widget test with complete internal/external user guards."""
import argparse,hashlib,io,json,os,re,subprocess,tarfile,time,zipfile,signal
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
 runner={'military-continuation':'game.sanguo.mobile.SessionBMilitaryContinuationInstrumentation','replenishment':'game.sanguo.mobile.SessionBReplenishmentInstrumentation','native-duel':'game.sanguo.mobile.SessionBNativeDuelInstrumentation','legacy39':'game.sanguo.mobile.SessionBLegacy39Instrumentation','items':'game.sanguo.mobile.SessionBDebateInstrumentation','direct':'game.sanguo.mobile.SessionBDebateInstrumentation','search':'game.sanguo.mobile.SessionBDebateInstrumentation','debate':'game.sanguo.mobile.SessionBDebateInstrumentation','capacity':'game.sanguo.mobile.SessionBCapacityInstrumentation','governor':'game.sanguo.mobile.SessionBGovernorInstrumentation','fieldworks':'game.sanguo.mobile.SessionBFieldworksInstrumentation'}[a.runner]
 marker={'military-continuation':'PASS SESSION B MILITARY CONTINUATION','replenishment':'PASS SESSION B REPLENISHMENT','native-duel':'PASS SESSION B NATIVE DUEL','legacy39':'PASS SESSION B LEGACY39','items':'PASS SESSION B ITEMS','direct':'PASS SESSION B DIRECT','search':'PASS SESSION B SEARCH','debate':'PASS SESSION B DEBATE','capacity':'PASS SESSION B CAPACITY','governor':'PASS SESSION B GOVERNOR','fieldworks':'PASS SESSION B FIELDWORKS'}[a.runner]
 if a.serial!='emulator-5582':raise ValueError('B target is5582; no other device authorized by this tool')
 if a.runner=='military-continuation' and a.initial_save is None:raise ValueError('Military continuation requires exact real r25 checkpoint')
 if a.initial_save is not None:
  captured={ROOT/'out/session-b/native-duel-apk61-r5-acceptance-v1/evidence/native-duel-normal/failed-campaign.sg11':('c9d219988a317d963da89ad2acd44b2354eb316728abf01ec41f70d837a549b4','r5 actual ordinary menu/deployment/9 wholeturns; natural unit1 loss, original dual RNG retained'),ROOT/'out/session-b/native-duel-apk61-r10-acceptance-v1/evidence/native-duel-normal/failed-campaign.sg11':('06329224846382ae8cea34988458622ce13f8756c1912ac6f10f9e00e4fb2661','r10 actual ordinary load/deployment/map/AI throughturn18; actual intercepted survivor16 and original dual RNG retained')}
  captured[ROOT/'out/session-b/native-duel-apk61-r12-acceptance-v1/evidence/native-duel-normal/battle.sg11']=('668b77e99cbed02f6093530f17f0b38bdc2b0e4745a6b2f69ba8a58c0455a2b2','r12 actual ordinary accepted human native battle; original model/World/dual RNG retained')
  captured[ROOT/'out/session-b/native-duel-apk61-r13-spirit-acceptance-v1/evidence/native-duel-cold/failed-campaign.sg11']=('96bfcc08d64f4c53699a1506ef31c06df571fa418a1c8d32a7046618f2d3778f','r13 actual ordinary human loss winner1; original full World/model/dual RNG and unknown PDL1 preserved; explicit UI adoption required')
  captured[ROOT/'out/session-b/native-duel-apk61-r12-acceptance-v1/evidence/native-duel-cold/terminal.sg11']=('5e2730ea0dcbba4702f5ce6f6bf9b105c2a576a8205c89d8fb7dedb8a73ee0c5','r12 actual ordinary player victory winner0, complete original terminal/World/dual RNG retained; no callback selection yet')
  captured[ROOT/'out/session-b/native-duel-apk61-r18-human-acceptance-v1/evidence/native-duel-cold/recruitment-attempt.sg11']=('20f253f77b4fe8e21b205976a568ebcbfe53cd441bca730bbcf0a329fa6efa24','r18 actual ordinary failed recruitment pending, mask14 and full World/model/dual RNG/legacy item strategy retained')
  captured[ROOT/'out/session-b/native-duel-apk61-r20-ji-acceptance-v1/evidence/native-duel-normal/failed-campaign.sg11']=('8a15fd9e5371b7303e9cabede78e964b1d8e7b0a071d4d8bb83ee4074771b4c6','actual r20 normal Source0/player4/three-person/drum/map/full AI through19, test observer race only; original complete World/dual RNG')
  captured[ROOT/'out/session-b/native-duel-apk61-r21-ji-acceptance-v1/evidence/native-duel-normal/battle.sg11']=('b81af67a7a940d53061f960d2121b194b0e0240d7bc9affb4843c709b65741ca','actual ordinary r21 three-person accepted Source0/player4 battle, whole World/numerical model/dual RNG exact')
  captured[ROOT/'out/session-b/native-duel-apk61-r21-ji-acceptance-v1/evidence/native-duel-cold/terminal.sg11']=('a1e304702ef426190d50378e51b52d1647f6a41eb36845967c2720369c35327f','actual r21 ordinary three-person loss; original World/model/dual RNG/no recovery marker')
  initial_mode='continue-battle' if a.initial_save.resolve()==ROOT/'out/session-b/native-duel-apk61-r12-acceptance-v1/evidence/native-duel-normal/battle.sg11' else 'continue-campaign'
  if a.initial_save.resolve()==ROOT/'out/session-b/native-duel-apk61-r13-spirit-acceptance-v1/evidence/native-duel-cold/failed-campaign.sg11':initial_mode='continue-terminal'
  if a.initial_save.resolve()==ROOT/'out/session-b/native-duel-apk61-r12-acceptance-v1/evidence/native-duel-cold/terminal.sg11':initial_mode='continue-human-terminal'
  if a.initial_save.resolve()==ROOT/'out/session-b/native-duel-apk61-r18-human-acceptance-v1/evidence/native-duel-cold/recruitment-attempt.sg11':initial_mode='continue-human-pending'
  if a.initial_save.resolve()==ROOT/'out/session-b/native-duel-apk61-r20-ji-acceptance-v1/evidence/native-duel-normal/failed-campaign.sg11':
   if a.native_route!='ji-recruit':raise ValueError('r20 source4 requires actual Ji route')
   initial_mode='continue-ji-campaign'
  if a.initial_save.resolve()==ROOT/'out/session-b/native-duel-apk61-r21-ji-acceptance-v1/evidence/native-duel-normal/battle.sg11':initial_mode='continue-battle'
  if a.initial_save.resolve()==ROOT/'out/session-b/native-duel-apk61-r21-ji-acceptance-v1/evidence/native-duel-cold/terminal.sg11':
   if a.physical_recovery!='adopt':raise ValueError('r21 recovery run requires explicit adopt')
   initial_mode='continue-physical-terminal'
  military_input=ROOT/'out/session-b/replenishment-apk61-r25-acceptance-v2/evidence/replenishment-normal/after.sg11'
  if a.runner=='military-continuation':
   if a.initial_save.resolve()!=military_input:raise ValueError('Only exact actual r25 supply checkpoint for military continuation')
   captured[military_input]=('791bc706d94ed98e7737e24e4ca83c14f4ad414dcd7cbc68f513e0f8887d906d','actual r25 Source0 Sun normal newgame/deploy/map/supply/save/cold, unchanged full World/dual RNG')
   initial_mode='continue-military'
  initial_fact=captured.get(a.initial_save.resolve())
  if a.runner not in ['native-duel','military-continuation'] or initial_fact is None or sha(a.initial_save.read_bytes())!=initial_fact[0]:raise ValueError('Only exact captured ordinary Android turn9/turn18/battle checkpoints accepted; no constructed/alternate world')
 # Reject an unregistered runner BEFORE device locks, backup or installation.
 aapt=ROOT/'out/toolchain/android-sdk/build-tools/35.0.0/aapt'
 manifest=subprocess.run([str(aapt),'dump','xmltree',str(a.test_apk.resolve()),'AndroidManifest.xml'],check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=60).stdout.decode()
 registrations=[];block=[];level=None
 for line in manifest.splitlines()+['E: end']:
  indent=len(line)-len(line.lstrip())
  if level is not None and 'E: 'in line and indent<=level:
   registrations.append('\n'.join(block));block=[];level=None
  if 'E: instrumentation ('in line:level=indent
  if level is not None:block.append(line)
 if not any('="'+runner+'"'in row and 'android:name('in row and 'android:targetPackage('in row and '="'+PACKAGE+'"'in row for row in registrations):raise ValueError('Test APK does not register the requested runner/target')
 guard=json.loads(a.source_guard.read_text())
 source_root=ROOT if a.source_root is None else a.source_root.resolve()
 if source_root!=ROOT and source_root!=ROOT/'out/session-b/native-opening-combined61':raise ValueError('Only own complete inherited combination source is supported')
 from session_b_freeze_apk import paths
 def guarded(name):return name.startswith(('app/src/main/','core/src/main/','game-api/src/main/','game-runtime/src/main/','out/pc-native-runtime/','out/session-a/pc-fire-runtime/additional-jniLibs/','out/session-b/readonly-theme-dependencies/','out/session-b/readonly-opening-dependencies61/','out/session-b/cache-core-stage/'))or name in ['app/build.gradle','build.gradle','settings.gradle','gradle.properties','version.properties']
 if source_root==ROOT:actual_paths={name for name in paths()if guarded(name)}
 else:
  prefixes=['app/src/main','core/src/main','game-api/src/main','game-runtime/src/main','out/pc-native-runtime','out/session-a/pc-fire-runtime/additional-jniLibs','out/session-b/readonly-theme-dependencies','out/session-b/readonly-opening-dependencies61','out/session-b/cache-core-stage']
  actual_paths={str(p.relative_to(source_root))for prefix in prefixes for p in (source_root/prefix).rglob('*')if p.is_file()}
  actual_paths.update(name for name in ['app/build.gradle','build.gradle','settings.gradle','gradle.properties','version.properties']if (source_root/name).is_file())
 for prefix in ['out/session-b/cache-core-stage/','out/session-b/completed-repair-stage/','out/session-b/completed-repair-stage-v2/','out/session-b/completed-repair-stage-v3/','out/session-b/completed-repair-stage-v4/','out/session-b/completed-legacy39-stage/']:
  if any(name.startswith(prefix)for name in guard):
   stage=source_root/prefix
   actual_paths.update(str(p.relative_to(source_root))for p in stage.rglob('*')if p.is_file())
 if actual_paths!=set(guard):raise ValueError('Frozen APK production input path set differs: '+repr(sorted(actual_paths^set(guard))))
 changed=[name for name,digest in guard.items()if sha((source_root/name).read_bytes())!=digest]
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
 report=dict(passed=False,serial=a.serial,owner=owner,apks={str(p):sha(p.read_bytes())for p in [a.apk,a.test_apk]},device=command('shell','getprop').decode(),sourceRoot=str(source_root),sourceGuardSha=sha(a.source_guard.read_bytes()),physicalRecovery=a.physical_recovery,nativeRoute=a.native_route,nativeStrategy=a.native_strategy,nativeAiActorPolicy=a.native_ai_actor_policy,nativeHumanActorPolicy=a.native_human_actor_policy,nativeHumanDisposition=a.native_human_disposition,nativeRecruitItemPolicy=a.native_recruit_item_policy,scope=({'military-continuation':'exact real r25 checkpoint/ordinary map camp/cancel/double/stop/fullturn/repair/unfinished save/cold automatic completion/ordinary city demolition/final save; enemy destruction/fire/allsource/ARM separate','replenishment':'actual Source0/player2 ordinary newgame/three-person sword deployment/legal map station/government supply unavailable reason/legal amounts/cancel/double/physical resource debit/fullWorld dualRNG/save/cold; numerical PC supply parity and ARM pending','native-duel':'actual source-bound menu options/cancel/normal source0 deployment/map/AI armies/human duel/manual save/in-battle cold/terminal/three wholeturns/final cold; full16 scenario/defaults/retreat/ARM separate','legacy39':'genuine original39 saved native debate/menu/manual load/explicit adoption/cancel/human/terminal/three wholeturns/save/fullWorld-allRNG/cold; concession/diplomacy/ARM pending','items':'actual source0 ordinary native book/newmenu/cancel/confiscate-award/current inventory/save/fullWorld/RNG/cold/fullturn; hidden/latent/fullDuel/ARM pending','direct':'actual source0 normal independent direct recruit/cancel/success-failure/original0gold20AP/rewards/datehash/fullsave/allRNG/cold/fullturn; full special gates/crosscity/ARM pending','search':'actual source0 twoforces ordinary SEARCH/cancel/discovery/consent/optionalDebate/decline/naturalwin-loss/whole model/allRNG/save/cold/terminal/nextturn; complete special gates/treasure/concession/ARM pending','debate':'actual source0 ordinary recruitment/human win-loss/whole model save/cold/terminal/nextturn; original admission-cost-concession-diplomacy pending','capacity':'actual source0/native58/13000/new/deploy/turns/save/cold','governor':'actual source0/two factions/menu/administration/transfer/capture/save/cold','fieldworks':'actual source14/new/deploy/map military widgets'}[a.runner])+'; separate from core probe and ARM',runner=runner)
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
  if a.initial_save is not None:
   raw=a.initial_save.read_bytes();report['actualCampaignInput']=dict(path=str(a.initial_save.resolve()),sha256=sha(raw),origin=initial_fact[1])
   command('push',str(a.initial_save.resolve()),'/data/local/tmp/session-b-actual-campaign9.sg11')
   command('shell','cp','/data/local/tmp/session-b-actual-campaign9.sg11',internal+'/files/manual3.sg11')
   uid=command('shell','stat','-c','%u:%g',internal+'/files').decode().strip()
   if not re.fullmatch(r'[0-9]+:[0-9]+',uid):raise ValueError('Unexpected private files owner')
   command('shell','chown',uid,internal+'/files/manual3.sg11')
   command('shell','chmod','600',internal+'/files/manual3.sg11')
   directory=external+('/files/session-b/military-continuation-normal'if a.runner=='military-continuation'else '/files/session-b/native-duel-normal');command('shell','mkdir','-p',directory)
   command('push',str(a.initial_save.resolve()),directory+'/continue-input.sg11')
   if sha(command('exec-out','cat',internal+'/files/manual3.sg11'))!=sha(raw):raise ValueError('Actual saved campaign staged bytes differ')
  save()
  with (output/'instrumentation.txt').open('wb')as f:
   result=subprocess.run(adb+['shell','am','instrument','-w']+(['-e','mode',initial_mode]if a.initial_save is not None else[])+(['-e','flow',a.runner]if a.runner in ['search','direct','items','legacy39']else[])+(['-e','physical_recovery',a.physical_recovery,'-e','native_route',a.native_route,'-e','native_strategy',a.native_strategy,'-e','ai_actor_policy',a.native_ai_actor_policy,'-e','human_actor_policy',a.native_human_actor_policy,'-e','human_disposition',a.native_human_disposition,'-e','recruit_item_policy',a.native_recruit_item_policy]if a.runner=='native-duel'else[])+[PACKAGE+'.test/'+runner],stdout=f,stderr=subprocess.STDOUT,timeout=7200)
  text=(output/'instrumentation.txt').read_text();report['instrumentationPassed']=result.returncode==0 and marker in text and 'FAIL'not in text
  if report['instrumentationPassed']:
   command('shell','am','force-stop',PACKAGE)
   gone=subprocess.run(adb+['shell','pidof',PACKAGE],stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=30)
   if gone.stdout.strip():raise ValueError('Own process still active before cold continuation')
   with (output/'cold-instrumentation.txt').open('wb')as f:
    cold=subprocess.run(adb+['shell','am','instrument','-w','-e','mode','cold']+(['-e','flow',a.runner]if a.runner in ['search','direct','items','legacy39']else[])+(['-e','physical_recovery',a.physical_recovery,'-e','native_route',a.native_route,'-e','native_strategy',a.native_strategy,'-e','ai_actor_policy',a.native_ai_actor_policy,'-e','human_actor_policy',a.native_human_actor_policy,'-e','human_disposition',a.native_human_disposition,'-e','recruit_item_policy',a.native_recruit_item_policy]if a.runner=='native-duel'else[])+[PACKAGE+'.test/'+runner],stdout=f,stderr=subprocess.STDOUT,timeout=3600 if a.native_route=='ji-recruit' else 900)
   cold_text=(output/'cold-instrumentation.txt').read_text();report['coldPassed']=cold.returncode==0 and marker+' COLD'in cold_text and 'FAIL'not in cold_text
   report['instrumentationPassed']=report['instrumentationPassed']and report['coldPassed'];save()
   if report['instrumentationPassed'] and a.runner=='native-duel':
    command('shell','am','force-stop',PACKAGE)
    if subprocess.run(adb+['shell','pidof',PACKAGE],stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=30).stdout.strip():raise ValueError('Own process active before final cold continuation')
    with (output/'terminal-cold-instrumentation.txt').open('wb')as f:
     terminal=subprocess.run(adb+['shell','am','instrument','-w','-e','mode','terminal-cold',PACKAGE+'.test/'+runner],stdout=f,stderr=subprocess.STDOUT,timeout=900)
    terminal_text=(output/'terminal-cold-instrumentation.txt').read_text();report['terminalColdPassed']=terminal.returncode==0 and marker+' TERMINAL COLD'in terminal_text and 'FAIL'not in terminal_text
    report['instrumentationPassed']=report['instrumentationPassed']and report['terminalColdPassed'];save()
  command('pull',external+'/files/session-b',str(output/'evidence'))
 finally:
  if backed:
   signal.signal(signal.SIGINT,signal.SIG_IGN)
   command('shell','am','force-stop',PACKAGE)
   report['restoration']={}
   for label,remote in [('internal',internal),('external',external)]:
    current=archive(remote,output/(label+'-post-test.tar'));added=set(current)-set(report['before'][label]);removed=[]
    # Only added files authored during this isolated test are removable. Preserve unrelated additions.
    for name in sorted(added):
     plain=name.removeprefix('./')
     own_stream=label=='internal'and re.fullmatch(r'cache/pc-music-stream-[0-9]+-[0-9]+/original\.pcm(?:\.part)?',plain)
     own_source_cache=False
     source_match=re.fullmatch(r'cache/pc-source-effects/([0-9a-f]{64})-(source(?:-fire)?-scene\.bin)',plain)if label=='internal'else None
     if source_match:
      # PcEffectProcess.verifiedAsset copies these exact frozen APK bytes.
      # Only absent-before, archived, live-identical derived copies qualify.
      with zipfile.ZipFile(a.apk)as packaged:asset=packaged.read('assets/3d/pc-effects/'+source_match.group(2))
      digest=sha(asset)
      if digest!=source_match.group(1)or current[name]['sha256']!=digest or current[name]['bytes']!=len(asset):raise ValueError('Generated source cache differs from frozen APK asset')
      if command('shell','sha256sum',remote+'/'+plain).decode().split()[0]!=digest:raise ValueError('Generated source cache changed after archive')
      own_source_cache=True;report.setdefault('removedAuthoredSourceCaches',[]).append(dict(path=plain,**current[name],source='PcEffectProcess.verifiedAsset; absent-before/postarchive/live/APK asset exact'))
     if own_stream:
      # PcMusicStreamPlayer97 creates a fresh elapsedRealtimeNanos/musicId
      # directory for this exclusively locked activity; force-stop can interrupt
      # its finally cleanup. Preserve post-test archive, then remove only this
      # added file after live SHA equals that archive. No preexisting cache/file
      # or unknown additional resource is eligible.
      digest=command('shell','sha256sum',remote+'/'+plain).decode().split()[0]
      if digest!=current[name]['sha256']:raise ValueError('Own generated stream changed after archive')
      report.setdefault('removedAuthoredStreamCaches',[]).append(dict(path=plain,**current[name],source='PcMusicStreamPlayer97/115; only added exclusive-run file, archived/liveSHA exact'))
     if own_stream or own_source_cache or label=='external'and plain.startswith('files/session-b/')or label=='internal'and (plain in {'files/auto.sg11','files/manual3.sg11','files/customOfficers/library.json'}or plain.startswith('shared_prefs/')):
      command('shell','rm',remote+'/'+plain);removed.append(name)
    command('shell','tar','-C',remote,'-xf','/data/local/tmp/session-b-'+label+'-backup.tar')
    actual=archive(remote,output/(label+'-restored.tar'));bad=[name for name,row in report['before'][label].items()if actual.get(name)!=row]
    report['restoration'][label]=dict(originalFiles=len(report['before'][label]),shaReadbackMatches=not bad,mismatches=bad,removedTestFiles=removed,preservedAdditionalFiles=sorted(set(actual)-set(report['before'][label])))
   report['passed']=report.get('instrumentationPassed',False)and all(row['shaReadbackMatches']and not row['preservedAdditionalFiles']for row in report['restoration'].values());save()
  if (lock/'owner.json').exists()and json.loads((lock/'owner.json').read_text())==owner:(lock/'owner.json').unlink();lock.rmdir()
 if not report.get('passed'):raise SystemExit(1)
 print('PASS Session B actual APK/widgets and complete original user SHA restoration',flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--serial',required=True);p.add_argument('--runner',choices=['military-continuation','replenishment','native-duel','legacy39','fieldworks','capacity','governor','debate','search','direct','items'],default='fieldworks')
 p.add_argument('--source-root',type=Path,default=None)
 p.add_argument('--initial-save',type=Path,default=None)
 p.add_argument('--physical-recovery',choices=['preserve','adopt'],default='preserve')
 p.add_argument('--native-route',choices=['legacy','ji-recruit'],default='legacy')
 p.add_argument('--native-recruit-item-policy',choices=['preserve','force-ruler'],default='preserve')
 p.add_argument('--native-human-actor-policy',choices=['preserve','force-ruler'],default='preserve')
 p.add_argument('--native-human-disposition',choices=['detain','recruit'],default='detain')
 p.add_argument('--native-ai-actor-policy',choices=['preserve','force-ruler'],default='preserve')
 p.add_argument('--native-strategy',choices=['attack','spirit','retreat','swap-critical'],default='attack')
 for name in ['apk','test-apk','source-guard','output']:p.add_argument('--'+name,type=Path,required=True)
 run(p.parse_args())
