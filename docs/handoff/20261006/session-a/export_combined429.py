#!/usr/bin/env python3
"""Complete exact ce83 game/268 test plus own reviewed tools, after fire rollback."""
from pathlib import Path
import json,subprocess,ctypes,os,shutil,tarfile,time,hashlib
from stage_serial401 import ROOT,D,sha
from read_session_state import read_session_state as read
from run_music_reserve378 import live_case
OUT=ROOT/'out/session-a/combined-source429'
def main():
 assert not OUT.exists();print('Wait real combined428 all original SHA and owner exit',flush=True)
 while not (D/'COMBINED_FIRE428_LAUNCH.json').exists() or read(D/'COMBINED_FIRE428_LAUNCH.json')['stage'] not in ['restored-verified','stopped-preserve-real-evidence']:time.sleep(5)
 case=ROOT/'out/session-a/combined-fire428';r=read(case/'session.json');assert r['stage']=='restored-verified' and not live_case(case) and all(x['exactRegularFileSha'] for x in r['restoration'].values())
 adb='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platform-tools/adb'
 while subprocess.run([adb,'-s','emulator-5554','shell','pidof','game.sanguo.mobile.dev'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0:time.sleep(5)
 b=read(D/'SERIAL_TEST_BUILD416.json');index=Path(b['candidateInputManifest']);assert sha(index)==b['candidateInputManifestSha256'];source=Path(b['sourcePath']);original=read(index);files={x['path']:(source/x['path'],x['sha256']) for x in original}
 for p in D.rglob('*'):
  if p.is_file() and '__pycache__' not in p.parts:files[str(p.relative_to(ROOT))]=(p,sha(p))
 for p in ['README.md','progress.md','docs/architecture/MODULE_RULES.md','PARITY_UI_CONTRACT.md','docs/PC_PARITY_STATUS.md']:
  if p in files:assert files[p][1]==sha(source/p)
 assert shutil.disk_usage(ROOT).free>3*1024**3
 OUT.mkdir();dest=OUT/'source';dest.mkdir();libc=ctypes.CDLL('/usr/lib/libSystem.B.dylib',use_errno=True);clone=libc.clonefile;clone.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_int];clone.restype=ctypes.c_int;rows=[]
 for name,(src,digest) in sorted(files.items()):
  assert sha(src)==digest,name;dst=dest/name;dst.parent.mkdir(parents=True,exist_ok=True)
  if clone(os.fsencode(src),os.fsencode(dst),0):shutil.copy2(src,dst)
  assert sha(dst)==digest and src.stat().st_ino!=dst.stat().st_ino and (src.stat().st_mode&0o777)==(dst.stat().st_mode&0o777),name
  if name.endswith('.json') and name.startswith('docs/handoff/20261006/session-a/'):json.loads(dst.read_text())
  rows.append({'path':name,'sha256':digest,'bytes':dst.stat().st_size,'mode':dst.stat().st_mode&0o777})
 archive=OUT/'sanguo11-mobile-combined-source.tar'
 with tarfile.open(archive,'w:') as t:
  for row in rows:t.add(dest/row['path'],arcname=row['path'])
 with tarfile.open(archive,'r:') as t:
  members={m.name:m for m in t};assert len(members)==len(rows)
  for row in rows:
   m=members[row['path']];assert m.isfile() and m.mode==row['mode'] and hashlib.sha256(t.extractfile(m).read()).hexdigest()==row['sha256']
 manifest=OUT/'source-files.json';manifest.write_text(json.dumps(rows,indent=2)+'\n')
 jni=[x for x in rows if 'jniLibs/' in x['path'] and x['path'].endswith('.so')];assert len(jni)==6
 for row in read(D/'MUSIC_RESERVE_BUILD369.json')['protectedCurrentSixUnchanged'].items():assert sha(ROOT/row[0])==row[1]
 report={'commonBase':b['commonBase'],'snapshotCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'completedBProduction':'cdd7f949','aOpeningAdapter':'OPENING_ADAPTER404.patch','sourceApks':b['apks'],'archivePath':str(archive),'archiveBytes':archive.stat().st_size,'archiveSha256':sha(archive),'sourceFiles':len(rows),'fileManifest':str(manifest),'fileManifestSha256':sha(manifest),'everyTarMemberShaAndModeVerified':True,'independentInodes':True,'jni':jni,'fixedBuildInputs':168,'containsBuildSdkCachesOrDeviceBackups':False,'actualCombinedFireOutcome':r.get('passed'),'scope':'Complete immutable candidate exact ce83/268 game/test source, completed B27 production/resources and A originals/new2JNI plus current reviewed A tools/docs. Whole archive and every member/mode readback. Runtime SOURCE_REVISION metadata b8 is not the full source commit; outside-Git builds may change APK digest. Frozen original4 unchanged; final shared serialization/Unity compatibility still not expanded. Current first-source0 performance419/425 remainsFAIL; actual options420 limitedPASS;428 outcome separate. No B WIP or B28 later tests automatically imported. Full source delivery is not whole-goal, ARM, full media, source pixel or all-rule acceptance.','wholeGoalComplete':False};(D/'COMBINED_SOURCE430.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'archive':str(archive),'sha256':report['archiveSha256'],'files':len(rows)}),flush=True)
if __name__=='__main__':main()
