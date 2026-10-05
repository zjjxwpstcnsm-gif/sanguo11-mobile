#!/usr/bin/env python3
"""Build a labelled source-map battle save for actual 3D UI acceptance.

This authors positions/aptitudes outside commands, retaining the source city and
terrain. It is not an official opening. Orders run via the target APK's UI.
Supply --serial/--apk to generate with exact installed Android production classes;
JVM/ART GZIP headers differ, so never rewrite a host save header for a UI fixture.
"""
import argparse, hashlib, json, os, re, subprocess
from pathlib import Path
from verify_pc_age_install import PACKAGE, members
ROOT=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);p.add_argument('--serial');p.add_argument('--apk',type=Path);a=p.parse_args()
assert bool(a.serial)==bool(a.apk), 'Both serial and pinned APK required'
out=a.output.resolve();assert ROOT/'out' in out.parents;out.mkdir(parents=True,exist_ok=False)
source=ROOT/'tools/content/android-fixtures/NativeCityDisplacementFixture.java';core=ROOT/'core/build/libs/core.jar'
java=Path(os.environ['JAVA_HOME'])/'bin/java';classes=out/'classes';classes.mkdir()
sha=lambda data:hashlib.sha256(data).hexdigest()
with (out/'javac.log').open('w') as log:subprocess.run([str(java.with_name('javac')),'--release','17','-encoding','UTF-8','-cp',str(core),'-d',str(classes),str(source)],check=True,stdout=log,stderr=log)
save=out/'source-map-city-advance.sg11';device={}
if a.serial:
 sdk=ROOT/'out/toolchain/android-sdk';adb=[str(sdk/'platform-tools/adb'),'-s',a.serial]
 def cmd(*parts,timeout=90):return subprocess.check_output(adb+list(parts),stderr=subprocess.PIPE,timeout=timeout)
 installed=cmd('shell','pm','path',PACKAGE).decode().strip().removeprefix('package:');assert installed.startswith('/data/app/') and '\n' not in installed
 expected=sha(a.apk.read_bytes());assert sha(cmd('exec-out','cat',installed,timeout=180))==expected
 assert cmd('shell','id','-u').strip()==b'0'
 cmd('shell','am','force-stop',PACKAGE)
 before=cmd('exec-out','tar','-C','/data/data/'+PACKAGE,'-cf','-','files','shared_prefs');(out/'user-before.tar').write_bytes(before)
 probe=out/'fixture-test-only.zip';inputs=list(classes.glob('*.class'));assert len(inputs)==1 and inputs[0].name=='NativeCityDisplacementFixture.class'
 with (out/'d8.log').open('w') as log:subprocess.run([str(java),'-cp',str(sdk/'build-tools/35.0.0/lib/d8.jar'),'com.android.tools.r8.D8','--min-api','26','--lib',str(sdk/'platforms/android-35/android.jar'),'--classpath',str(core),'--output',str(probe),str(inputs[0])],check=True,stdout=log,stderr=log)
 remote='/data/local/tmp/city-ui-fixture-'+sha(probe.read_bytes())[:20]+'.zip';cmd('push',str(probe),remote);cmd('shell','chmod','644',remote);assert sha(cmd('exec-out','cat',remote))==sha(probe.read_bytes())
 # The authoring class is the sole DEX input. All rules/data come from base.apk.
 remote_save=remote+'.'+str(__import__('time').time_ns())+'.sg11'
 try:
  process=subprocess.run(adb+['shell','env','CLASSPATH='+installed+':'+remote,'app_process','/system/bin','NativeCityDisplacementFixture',remote_save],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=180)
  log=process.stdout;(out/'generation.log').write_bytes(log)
  if process.returncode:
   (out/'failed-runtime.log').write_bytes(cmd('logcat','-d','-t','450','-s','AndroidRuntime'))
   raise RuntimeError('Fixture generation exited '+str(process.returncode)+'; original logs preserved')
  save.write_bytes(cmd('exec-out','cat',remote_save))
 finally:
  after=cmd('exec-out','tar','-C','/data/data/'+PACKAGE,'-cf','-','files','shared_prefs');(out/'user-after.tar').write_bytes(after)
  unchanged=members(before)==members(after);apk_unchanged=sha(cmd('exec-out','cat',installed,timeout=180))==expected
  device=dict(serial=a.serial,apk_sha256=expected,fixture_dex_sha256=sha(probe.read_bytes()),production_classes_source='Exact installed base.apk on ART',user_files_unchanged=unchanged,installed_apk_unchanged=apk_unchanged)
  (out/'device-verification.json').write_text(json.dumps(device,indent=2)+'\n');assert unchanged and apk_unchanged
else:
 log=subprocess.check_output([str(java),'-cp',os.pathsep.join(map(str,[core,classes])),'NativeCityDisplacementFixture',str(save)],stderr=subprocess.STDOUT)
(out/'generation.log').write_bytes(log)
match=re.fullmatch(r'PASS source-map fixture city=(.*?) id=(\d+) footprint=(.*?) actor=(\S+) target=(\S+) stop=(\S+) blocked=(\S+) saveVersion=(\d+) bytes=(\d+)\r?\n',log.decode());assert match
city,city_id,footprint,actor,target,stop,blocked,version,size=match.groups()
assert int(version)==int.from_bytes(save.read_bytes()[4:8],'big') and int(size)==save.stat().st_size
report=dict(scope='Authored battle setup on actual source map/city; not official opening; UI orders use installed APK production rules',scenario='heroes-250',player=0,city=city,city_id=int(city_id),footprint=footprint,actor=actor,target=target,stop=stop,blocked=blocked,source_sha256=sha(source.read_bytes()),core_jar_sha256=sha(core.read_bytes()),save_sha256=sha(save.read_bytes()),save_bytes=save.stat().st_size,save_version=int(version),generation_output=log.decode(),device=device)
(out/'manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False))
