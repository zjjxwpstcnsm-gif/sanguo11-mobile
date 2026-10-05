#!/usr/bin/env python3
"""Run merchant probes against the exact installed APK's core classes on Android ART.

The probe dex contains only test classes and the original-x86 oracle. Production
classes MUST come from the installed APK. No app UI/test source is modified and
all test worlds are in memory. Run after the installation harness has restored
user files, with no other task using this serial.
"""
import argparse,hashlib,json,os,subprocess,time,zipfile
from pathlib import Path
from verify_pc_age_install import ROOT,PACKAGE,members

def sha(data):return hashlib.sha256(data).hexdigest()
def run(args):
 output=args.output.resolve()
 if ROOT/'out' not in output.parents:raise ValueError('Output must be inside project out/')
 output.mkdir(parents=True,exist_ok=False)
 sdk=ROOT/'out/toolchain/android-sdk';java=Path(os.environ['JAVA_HOME'])/'bin/java'
 adb=[str(sdk/'platform-tools/adb'),'-s',args.serial]
 def cmd(*parts,timeout=60):
  return subprocess.run(adb+list(parts),check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=timeout).stdout
 apk_sha=sha(args.apk.read_bytes());installed=cmd('shell','pm','path',PACKAGE).decode().strip().removeprefix('package:')
 if not installed.startswith('/data/app/') or '\n' in installed:raise ValueError('Expected one installed APK')
 if sha(cmd('exec-out','cat',installed,timeout=180))!=apk_sha:raise ValueError('Installed APK differs')
 if cmd('shell','id','-u').strip()!=b'0':raise ValueError('Root required for protected user-file verification; no rule probe executed')
 cmd('shell','am','force-stop',PACKAGE)
 before=cmd('exec-out','tar','-C','/data/data/'+PACKAGE,'-cf','-','files','shared_prefs');(output/'user-before.tar').write_bytes(before)
 original=members(before)
 names=('PcMerchantMeritTest','PcMerchantArithmeticTest','TradePlanTest','CityActionPlanTest')
 if args.ability_arithmetic:names+=('PcOfficerAbilityArithmeticTest',)
 if getattr(args,'officer_ranks',False) or getattr(args,'officer_state',False):names+=('PcOfficerRankTest',)
 if getattr(args,'officer_state',False):names+=('PcOfficerStateTest',)
 classes=ROOT/'core/build/classes/java/test/game/sanguo/core'
 inputs=sorted(p for p in classes.glob('*.class') if p.stem.split('$')[0] in names)
 if len(inputs)!=5+int(args.ability_arithmetic)+int(getattr(args,'officer_ranks',False) or getattr(args,'officer_state',False))+int(getattr(args,'officer_state',False)):raise ValueError('Unexpected probe class set')
 report=dict(passed=False,serial=args.serial,apk_sha256=apk_sha,
  class_sources={str(p.relative_to(ROOT)):sha(p.read_bytes()) for p in inputs},
  scope='Installed APK production classes in ART; ordinary commands and generated worlds, not UI taps',results=[])
 def save():(output/'results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
 save()
 try:
  # This jar never contains World/Campaign/TradePlan/PcMerchantRules production classes.
  probe=output/'probe.zip'
  command=[str(java),'-cp',str(sdk/'build-tools/35.0.0/lib/d8.jar'),'com.android.tools.r8.D8','--min-api','26',
    '--lib',str(sdk/'platforms/android-35/android.jar'),'--classpath',str(ROOT/'core/build/libs/core.jar'),
    '--output',str(probe)]+[str(p) for p in inputs]
  with (output/'d8.log').open('w') as log:subprocess.run(command,stdout=log,stderr=log,check=True)
  with zipfile.ZipFile(probe,'a') as archive:
   archive.write(ROOT/'core/src/test/resources/pc-merchant-native.tsv','pc-merchant-native.tsv')
   if args.ability_arithmetic:
    archive.write(ROOT/'core/src/test/resources/pc-officer-ability-native.tsv','pc-officer-ability-native.tsv')
    archive.write(ROOT/'core/src/test/resources/pc-officer-date-native.tsv','pc-officer-date-native.tsv')
   if getattr(args,'officer_ranks',False) or getattr(args,'officer_state',False):
    archive.write(ROOT/'core/src/test/resources/pc-rank-legacy-ak.tsv','pc-rank-legacy-ak.tsv')
   if getattr(args,'officer_state',False):
    archive.write(ROOT/'core/src/test/resources/pc-officer-legacy-am.sg11','pc-officer-legacy-am.sg11')
    archive.write(ROOT/'core/src/test/resources/pc-officer-legacy-am-art.sg11','pc-officer-legacy-am-art.sg11')
    for file in sorted((ROOT/'core/src/test/resources/legacy-market-v35').rglob('*')):
     if file.is_file():archive.write(file,file.relative_to(ROOT/'core/src/test/resources').as_posix())
  report['probe_sha256']=sha(probe.read_bytes());remote='/data/local/tmp/pc-merchant-'+report['probe_sha256'][:20]+'.zip'
  cmd('push',str(probe),remote);cmd('shell','chmod','644',remote)
  if sha(cmd('exec-out','cat',remote))!=report['probe_sha256']:raise ValueError('Probe dex readback mismatch')
  for name in names[:2]+tuple(n for n in names[4:]):
   started=time.monotonic();result=cmd('shell','env','CLASSPATH='+installed+':'+remote,'app_process','/system/bin','game.sanguo.core.'+name,timeout=180)
   (output/(name+'.log')).write_bytes(result)
   if ('PASS '+name).encode() not in result:raise ValueError('Missing pass marker for '+name)
   report['results'].append(dict(suite=name,seconds=round(time.monotonic()-started,2),output=result.decode()));save()
  if getattr(args,'officer_state',False):
   opening_remote=remote+'.managed190.sg11'
   result=cmd('shell','env','CLASSPATH='+installed+':'+remote,'app_process','/system/bin','game.sanguo.core.PcOfficerStateTest','--write-opening',opening_remote,timeout=180)
   (output/'managed-opening.log').write_bytes(result)
   if b'PASS PcOfficerStateTest opening=' not in result:raise ValueError('Managed opening generation failed')
   opening=cmd('exec-out','cat',opening_remote);(output/'managed190.sg11').write_bytes(opening)
   report['managed_opening']=dict(path=str(output/'managed190.sg11'),sha256=sha(opening),bytes=len(opening),scenario='coalition-190',player=1,seed=23,save_version=int.from_bytes(opening[4:8],'big'),source='Exact installed APK ordinary ScenarioCatalog.load on ART')
  report['rules_passed']=True
 finally:
  try:
   after=cmd('exec-out','tar','-C','/data/data/'+PACKAGE,'-cf','-','files','shared_prefs');(output/'user-after.tar').write_bytes(after)
   current=members(after);report['user_files_unchanged']=original==current
   apk_after=cmd('exec-out','cat',installed,timeout=180)
   report['post_apk_readback_bytes']=len(apk_after)
   report['post_apk_readback_sha256']=sha(apk_after)
   report['installed_apk_unchanged']=report['post_apk_readback_sha256']==apk_sha
  except Exception as error:
   report['verification_error']=dict(type=type(error).__name__,message=str(error))
  report['passed']=bool(report.get('rules_passed') and report.get('user_files_unchanged') and report.get('installed_apk_unchanged'))
  save()
  if not report.get('user_files_unchanged') or not report.get('installed_apk_unchanged'):
   raise ValueError('Post-verification failed; inspect user files, APK readback and device transport')
 print(json.dumps(report,ensure_ascii=False))

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--serial',required=True);p.add_argument('--apk',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--ability-arithmetic',action='store_true',help='Also check original officer arithmetic in the installed APK; does not claim gameplay integration');p.add_argument('--officer-ranks',action='store_true',help='Check imported officer ranks through appointment, deployment, monthly payroll and save continuation');p.add_argument('--officer-state',action='store_true',help='Check managed abilities through normal commands, nine openings, v34 and frozen v33 continuation');run(p.parse_args())
