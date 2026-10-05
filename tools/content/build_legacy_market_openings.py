#!/usr/bin/env python3
"""Generate genuine host/ART v35 fixtures from frozen full R29 source and its read-only APK.
This is an isolated compatibility reference, never a replacement project baseline or installed app.
"""
import argparse,hashlib,json,os,subprocess,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
EXPECTED_APK='f1741671001dbcd37404ab098fc8529957806ffb4a3b96c6e6c592082293fb75'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def build(a):
 source=a.reference_source.resolve();manifest=json.loads((source.parent/'manifest.json').read_text())
 if manifest['head']!='a934c2365425bec0b9ae14b9643ac1f9753dec71' or sha(a.reference_apk)!=EXPECTED_APK:raise ValueError('Expected complete frozen R29 reference and its verified APK')
 rows=[r for r in manifest['files'] if r['path'].startswith('core/src/main/')]
 if not rows or any(sha(source/r['path'])!=r['sha256'] for r in rows):raise ValueError('Frozen reference source/resources differ')
 out=a.output.resolve()
 if ROOT/'out' not in out.parents:raise ValueError('Fresh project out required')
 out.mkdir(parents=True,exist_ok=False);classes=out/'classes';classes.mkdir();java=Path(os.environ['JAVA_HOME'])/'bin';sdk=ROOT/'out/toolchain/android-sdk';helper=ROOT/'tools/content/LegacyMarketOpeningWriter.java'
 files=sorted((source/'core/src/main/java').rglob('*.java'));argfile=out/'sources.txt';argfile.write_text('\n'.join(str(p) for p in files+[helper])+'\n')
 def run(name,args):
  with (out/(name+'.log')).open('wb') as log:subprocess.run(list(map(str,args)),check=True,stdout=log,stderr=subprocess.STDOUT)
 run('javac',[java/'javac','--release','17','-encoding','UTF-8','-d',classes,'@'+str(argfile)])
 run('host',[java/'java','-Xmx768m','-cp',os.pathsep.join([str(classes),str(source/'core/src/main/resources')]),'game.sanguo.core.LegacyMarketOpeningWriter',out/'host'])
 probe=out/'probe.zip';run('d8',[java/'java','-cp',sdk/'build-tools/35.0.0/lib/d8.jar','com.android.tools.r8.D8','--min-api','26','--lib',sdk/'platforms/android-35/android.jar','--classpath',classes,'--output',probe,classes/'game/sanguo/core/LegacyMarketOpeningWriter.class'])
 adb=[str(sdk/'platform-tools/adb'),'-s',a.serial]
 def command(*args):return subprocess.check_output(adb+list(map(str,args)))
 package='game.sanguo.mobile.dev';installed=command('shell','pm','path',package).decode().strip().removeprefix('package:');installed_before=hashlib.sha256(command('exec-out','cat',installed)).hexdigest();before=command('exec-out','tar','-C','/data/data/'+package,'-cf','-','files','shared_prefs');(out/'user-before.tar').write_bytes(before)
 remote='/data/local/tmp/source-reference-r29-f174.apk';command('push',a.reference_apk,remote)
 if hashlib.sha256(command('exec-out','cat',remote)).hexdigest()!=EXPECTED_APK:raise ValueError('Read-only reference APK readback differs')
 rp='/data/local/tmp/legacy-market-writer-'+sha(probe)[:20]+'.zip';command('push',probe,rp);directory='/data/local/tmp/legacy-market-openings-r29-'+str(time.time_ns())
 run('art',adb+['shell','env','CLASSPATH='+remote+':'+rp,'app_process','/system/bin','game.sanguo.core.LegacyMarketOpeningWriter',directory]);command('pull',directory,out/'art')
 from verify_pc_age_install import members
 after=command('exec-out','tar','-C','/data/data/'+package,'-cf','-','files','shared_prefs');(out/'user-after.tar').write_bytes(after)
 if members(before)!=members(after) or hashlib.sha256(command('exec-out','cat',installed)).hexdigest()!=installed_before:raise ValueError('Reference-only writer changed user state or installed package')
 report=dict(source_code_commit=manifest['head'],source_apk_sha256=EXPECTED_APK,source_files_checked=len(rows),reference_loaded_from_tmp_not_installed=True,main_installed_not_changed=True,user_files_unchanged=True,fixture_seed=23,official_identity_verified=False,rows=[])
 for vm in ['host','art']:
  for p in sorted((out/vm).glob('*.sg11')):report['rows'].append(dict(vm=vm,id=p.stem,bytes=p.stat().st_size,sha256=sha(p)))
 if len(report['rows'])!=18:raise ValueError('Expected9 actual openings in each VM')
 (out/'provenance.json').write_text(json.dumps(report,indent=2)+'\n');print('PASS genuine frozen v35 host/ART openings; installed app and all user files unchanged')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for name in ['reference-source','reference-apk','output']:p.add_argument('--'+name,type=Path,required=True)
 p.add_argument('--serial',required=True);build(p.parse_args())
