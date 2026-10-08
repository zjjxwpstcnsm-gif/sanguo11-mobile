#!/usr/bin/env python3
"""Freeze a successfully built complete sequential source, never inherited test scores."""
import argparse,hashlib,json,shutil,tarfile,zipfile,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def sha(p):
 h=hashlib.sha256()
 with p.open('rb')as f:
  for b in iter(lambda:f.read(1048576),b''):h.update(b)
 return h.hexdigest()
def rawsha(b):return hashlib.sha256(b).hexdigest()
def main(revision):
 inputs=ROOT/'out/session-b/native-opening-combined61-source-inputs.json';d=json.loads(inputs.read_text());stage=Path(d['stage']);suffix=''if revision==1 else '-r'+str(revision);out=ROOT/('out/session-b/native-opening-combined61-frozen'+suffix);log=ROOT/('out/session-b/duel-query-check/native-opening-combined61-build'+suffix+'.log')
 if out.exists():raise ValueError('New freeze destination required')
 if 'BUILD SUCCESSFUL'not in log.read_text():raise ValueError('Independent APK build not successful')
 for r in d['files']:
  if sha(stage/r['path'])!=r['sha256']:raise ValueError('Input changed during build '+r['path'])
 source_inventory={str(p.relative_to(stage)) for prefix in ['app/src/main','core/src/main','game-api/src/main','game-runtime/src/main','out/pc-native-runtime','out/session-a/pc-fire-runtime/additional-jniLibs','out/session-b/readonly-theme-dependencies'] for p in (stage/prefix).rglob('*')if p.is_file()}
 source_inventory.update(p for p in ['app/build.gradle','build.gradle','settings.gradle','gradle.properties','version.properties']if (stage/p).is_file())
 if source_inventory!=set(d['sourceGuard']):raise ValueError('Complete production path inventory differs')
 init='docs/handoff/20261006/session-b/native-opening-acceptance61-r24.init.gradle'if revision==24 else 'docs/handoff/20261006/session-b/native-opening-acceptance61-r23.init.gradle'if revision==23 else 'docs/handoff/20261006/session-b/native-opening-acceptance61-r22.init.gradle'if revision==22 else 'docs/handoff/20261006/session-b/native-opening-acceptance61-r21.init.gradle'if revision==21 else 'docs/handoff/20261006/session-b/native-opening-acceptance61-r20.init.gradle'if revision==20 else 'docs/handoff/20261006/session-b/native-opening-military61.init.gradle'if revision==9 else 'docs/handoff/20261006/session-b/native-opening-acceptance61-r19.init.gradle'if revision==19 else 'docs/handoff/20261006/session-b/native-opening-acceptance61-r18.init.gradle'if revision==18 else 'docs/handoff/20261006/session-b/native-opening-acceptance61-r17.init.gradle'if revision==17 else 'docs/handoff/20261006/session-b/native-opening-acceptance61-r16.init.gradle'if revision==16 else 'docs/handoff/20261006/session-b/native-opening-acceptance61.init.gradle'
 if sha(ROOT/init)!=next(r['sha256'] for r in d['files'] if r['path']==init):raise ValueError('External compilation init changed')
 out.mkdir();shutil.copy2(inputs,out/'build-inputs.json');shutil.copy2(ROOT/'out/session-b/native-opening-combined61-source-guard.json',out/'source-guard.json');artifacts=[]
 for src in ['core/build/libs/core.jar','game-api/build/libs/game-api.jar','game-runtime/build/libs/game-runtime.jar','app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
  p=stage/src;t=out/p.name;shutil.copy2(p,t)
  if sha(p)!=sha(t):raise ValueError('Artifact copy differs')
  artifacts.append(dict(path=str(t),bytes=t.stat().st_size,sha256=sha(t)))
 jar_parity={}
 for name in ['core.jar','game-api.jar','game-runtime.jar']:
  with zipfile.ZipFile(out/name)as fresh,zipfile.ZipFile(ROOT/'out/session-b/native-candidate60/frozen'/name)as previous:
   added=set(fresh.namelist())-set(previous.namelist());removed=set(previous.namelist())-set(fresh.namelist())
   changed=[p for p in set(fresh.namelist())&set(previous.namelist()) if fresh.read(p)!=previous.read(p)]
   planned=revision>=3 and name=='core.jar'
   allowed_added={'game/sanguo/core/PcResourceBytes.class'} if planned else set()
   allowed_changed={'game/sanguo/core/PcDuelMenuOptions.class','game/sanguo/core/PcSourceOpeningOptions.class'} if planned else set()
   if revision>=4 and name=='core.jar':allowed_changed.add('game/sanguo/core/PcDuelCampaign$Facts.class')
   if revision>=12 and name=='core.jar':allowed_changed.add('game/sanguo/core/Contests.class')
   if revision>=4 and name=='game-runtime.jar':allowed_changed={'game/sanguo/runtime/query/PcOpeningOptionsQuery.class','game/sanguo/runtime/query/ContestQuery.class'}
   if revision>=15 and name=='core.jar':
    allowed_added.add('game/sanguo/core/PcLoyaltyProperty23Policy.class')
    allowed_changed.update({'game/sanguo/core/PcDuelRawLoyalty.class','game/sanguo/core/PcDuelCampaignPolicy.class'})
   if revision>=15 and name=='game-api.jar':
    allowed_changed={'game/sanguo/api/ContestCommand.class','game/sanguo/api/ContestCommand$Operation.class','game/sanguo/api/ContestSnapshot$NativeDuel.class'}
   if revision>=15 and name=='game-runtime.jar':
    allowed_changed.update({'game/sanguo/runtime/GameSession.class','game/sanguo/runtime/GameSession$1.class'})
   if revision>=16 and name=='core.jar':
    allowed_added.update({'game/sanguo/core/PcDuelAiActorPolicy.class','game/sanguo/core/Contests$NativeAiActor.class'})
    allowed_changed.add('game/sanguo/core/PcDuelAiDisposition.class')
   if revision>=17 and name=='core.jar':
    allowed_added.add('game/sanguo/core/PcDuelHumanActorPolicy.class')
    allowed_changed.add('game/sanguo/core/PcDuelRelease.class')
   if revision>=19 and name=='core.jar':
    allowed_added.add('game/sanguo/core/PcDuelRecruitItemPolicy.class')
    allowed_changed.update({'game/sanguo/core/PcDuelRecruitment.class','game/sanguo/core/SaveExtensions.class'})
   if revision>=24 and name=='core.jar':
    allowed_added.add('game/sanguo/core/PcDuelPhysicalRecoveryPolicy.class')
    allowed_changed.add('game/sanguo/core/PcDuelHealthPolicy.class')
   if revision>=24 and name=='game-api.jar':allowed_added={'game/sanguo/api/ContestSnapshot$PhysicalRecovery.class'}
   metadata=[]
   if revision>=15:
    metadata=({'core.jar':['game/sanguo/core/Contests$NativeHeir.class'],'game-api.jar':['game/sanguo/api/ContestSnapshot.class','game/sanguo/api/ContestSnapshot$DuelDisposition.class','game/sanguo/api/ContestSnapshot$DuelHeir.class','game/sanguo/api/ContestSnapshot$DuelInheritance.class','game/sanguo/api/ContestSnapshot$DuelInput.class']}).get(name,[])
    for entry in metadata:
     cls=entry[:-6].replace('/','.')
     fresh_code=subprocess.check_output(['javap','-c','-p','-classpath',str(out/name),cls])
     old_code=subprocess.check_output(['javap','-c','-p','-classpath',str(ROOT/'out/session-b/native-candidate60/frozen'/name),cls])
     if fresh_code!=old_code:raise ValueError('Unplanned executable/field/signature change '+entry)
    allowed_changed.update(metadata)
   if added!=allowed_added or removed or set(changed)!=allowed_changed:raise ValueError('Unplanned B production jar changes '+name+' '+repr((added,removed,changed)))
   jar_parity[name]=dict(entries=len(fresh.namelist()),added=sorted(added),changed=sorted(changed),removed=sorted(removed),allOtherUncompressedBytesExactlyCandidate60=True,plannedApi29StreamRepair=planned,metadataShiftInstructionFieldSignatureParity=metadata)
 apk=out/'app-debug.apk';pins=json.loads((stage/'tools/content/map-release-manifest.json').read_text())['files'];jni=[]
 with zipfile.ZipFile(apk)as z:
  for r in pins:
   if rawsha(z.read(r['apk_path']))!=r['sha256']:raise ValueError('Fixed resource APK differs '+r['apk_path'])
  for r in d['files']:
   p=r['path']
   if p.startswith(('out/pc-native-runtime/jniLibs/','out/session-a/pc-fire-runtime/additional-jniLibs/')):
    entry='lib/'+str(Path(p).parent.name)+'/'+Path(p).name
    if rawsha(z.read(entry))!=r['sha256']:raise ValueError('JNI APK differs '+p)
    jni.append(dict(path=p,apkPath=entry,sha256=r['sha256']))
 if len(pins)!=168 or len(jni)!=6:raise ValueError('Fixed resources/original4 plus A2 JNI coverage differs')
 sdk=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/build-tools/35.0.0')
 signature=subprocess.check_output([str(sdk/'apksigner'),'verify','--verbose',str(apk)],stderr=subprocess.STDOUT).decode()
 manifest=subprocess.check_output([str(sdk/'aapt'),'dump','xmltree',str(out/'app-debug-androidTest.apk'),'AndroidManifest.xml']).decode()
 runner='game.sanguo.mobile.SessionBFieldworksInstrumentation'if revision==9 else 'game.sanguo.mobile.SessionBNativeDuelInstrumentation'
 if runner not in manifest:raise ValueError('Fresh runner not registered')
 archive=out/'combined-source.tar.gz'
 with tarfile.open(archive,'w:gz',dereference=True)as t:
  for r in sorted(d['files'],key=lambda r:r['path']):t.add(stage/r['path'],arcname=r['path'],recursive=False)
 expected={r['path']:r for r in d['files']};seen=set()
 with tarfile.open(archive,'r:gz')as t:
  for m in t:
   r=expected.get(m.name)
   if not r or not m.isfile() or m.name in seen:raise ValueError('Unexpected archived source input')
   if rawsha(t.extractfile(m).read())!=r['sha256']or m.size!=r['bytes']:raise ValueError('Archive source readback differs '+m.name)
   seen.add(m.name)
 if seen!=set(expected):raise ValueError('Source archive incomplete')
 for r in d['files']:
  if sha(stage/r['path'])!=r['sha256']:raise ValueError('Input changed during source freeze')
 report=dict(candidateOnly=True,wholeGoalComplete=False,installed=False,bSourceHead='36f059b452f8fc5712747425a37fecbfee3ddd01',commonMain='ef413be3653820dd6449ba7f02aa60bed5b26ef5',
  aClosureCommit=d['aClosureCommit'],sourceInputManifest=str(out/'build-inputs.json'),sourceInputManifestSha256=sha(out/'build-inputs.json'),sourceInputs=len(d['files']),sourceStage=str(stage),
  fullInputArchiveReadbackExact=True,productionPathInventoryExact=True,sourceGuardPath=str(out/'source-guard.json'),sourceGuardSha256=sha(out/'source-guard.json'),
  sourceArchive=dict(path=str(archive),sha256=sha(archive),bytes=archive.stat().st_size),artifacts=artifacts,jarParity=jar_parity,
  fixedResources=len(pins),originalFourAndAdditionalTwoJni=jni,signature=signature,testRunnerRegistered=True,testRunner=runner,
  buildLogSha256=sha(log),pairRevision=revision)
 (out/'frozen-report.json').write_text(json.dumps(report,indent=2)+'\n')
 print(json.dumps(dict(passed=True,sourceInputs=len(seen),artifacts=artifacts,sourceArchive=report['sourceArchive']),indent=2),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--revision',type=int,choices=[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],default=1);main(p.parse_args().revision)
