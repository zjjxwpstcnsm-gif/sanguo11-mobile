#!/usr/bin/env python3
"""Generate an old-format fixture with frozen AM APK classes on ART, without installing it."""
import argparse,hashlib,json,os,subprocess,zipfile
from pathlib import Path
from verify_pc_age_install import ROOT,PACKAGE,members
SHA='d0187e6330cebc4d16eadc3264338f01cc0b907588192b7c9bdfd294fa692dd3'
JAR_SHA='a4a9d20e51c066ef94c47937ba02fb9e401ca88afe5b1db10cf5bd42e7e9fc09'
def sha(b):return hashlib.sha256(b).hexdigest()
def run(a):
 out=a.output.resolve();assert ROOT/'out' in out.parents;out.mkdir(exist_ok=False)
 assert sha(a.baseline_apk.read_bytes())==SHA and sha(a.baseline_jar.read_bytes())==JAR_SHA
 sdk=ROOT/'out/toolchain/android-sdk';java=Path(os.environ['JAVA_HOME'])/'bin';adb=[str(sdk/'platform-tools/adb'),'-s',a.serial]
 def cmd(*s,timeout=180):return subprocess.check_output(adb+list(s),timeout=timeout)
 cmd('shell','am','force-stop',PACKAGE);before=cmd('exec-out','tar','-C','/data/data/'+PACKAGE,'-cf','-','files','shared_prefs');(out/'user-before.tar').write_bytes(before)
 installed=cmd('shell','pm','path',PACKAGE).decode().strip().removeprefix('package:');installed_sha=sha(cmd('exec-out','cat',installed))
 report=dict(passed=False,baseline_apk_sha256=SHA,baseline_jar_sha256=JAR_SHA,installed_sha256=installed_sha,scope='Frozen old APK production classes in ART; no installation or user file mutation')
 try:
  source=ROOT/'tools/content/fixtures/WriteOfficerLegacyAm.java.txt';writer=out/'WriteLegacy.java';writer.write_bytes(source.read_bytes());fixture_source=ROOT/'out/parity/checkpoint-20261003-am/source/core/src/test/java/game/sanguo/core/PcOfficerRankTest.java'
  assert fixture_source.read_bytes()==(ROOT/'core/src/test/java/game/sanguo/core/PcOfficerRankTest.java').read_bytes()
  classes=out/'classes';classes.mkdir()
  subprocess.run([str(java/'javac'),'-cp',str(a.baseline_jar.resolve()),'-d',str(classes),str(writer),str(fixture_source)],check=True)
  inputs=sorted(classes.rglob('*.class'));assert len(inputs)==2
  probe=out/'probe.zip';subprocess.run([str(java/'java'),'-cp',str(sdk/'build-tools/35.0.0/lib/d8.jar'),'com.android.tools.r8.D8','--min-api','26','--lib',str(sdk/'platforms/android-35/android.jar'),'--classpath',str(a.baseline_jar.resolve()),'--output',str(probe)]+[str(p) for p in inputs],check=True)
  remote='/data/local/tmp/pc-legacy-am-'+SHA[:16];cmd('push',str(a.baseline_apk.resolve()),remote+'.apk');cmd('push',str(probe),remote+'.zip');cmd('shell','chmod','644',remote+'.apk',remote+'.zip')
  assert sha(cmd('exec-out','cat',remote+'.apk'))==SHA and sha(cmd('exec-out','cat',remote+'.zip'))==sha(probe.read_bytes())
  result=cmd('shell','env','CLASSPATH='+remote+'.apk:'+remote+'.zip','app_process','/system/bin','game.sanguo.core.WriteLegacy',remote+'.sg11');(out/'writer.log').write_bytes(result)
  raw=cmd('exec-out','cat',remote+'.sg11');assert int.from_bytes(raw[4:8],'big')==33
  (out/'pc-officer-legacy-am-art.sg11').write_bytes(raw);report.update(generated=True,fixture_sha256=sha(raw),fixture_bytes=len(raw),probe_sha256=sha(probe.read_bytes()),writer_source_sha256=sha(source.read_bytes()),fixture_source_sha256=sha(fixture_source.read_bytes()))
 finally:
  after=cmd('exec-out','tar','-C','/data/data/'+PACKAGE,'-cf','-','files','shared_prefs');(out/'user-after.tar').write_bytes(after);report['user_files_unchanged']=members(before)==members(after);report['installed_apk_unchanged']=sha(cmd('exec-out','cat',installed))==installed_sha;report['passed']=bool(report.get('generated') and report['user_files_unchanged'] and report['installed_apk_unchanged']);(out/'results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
 assert report['passed'];print(json.dumps(report))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--serial',required=True);p.add_argument('--baseline-apk',type=Path,required=True);p.add_argument('--baseline-jar',type=Path,required=True);p.add_argument('--output',type=Path,required=True);run(p.parse_args())
