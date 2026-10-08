#!/usr/bin/env python3
"""Final main complete source, preserved modes/inodes, plus ignored native inputs."""
from pathlib import Path
import json,subprocess,ctypes,os,shutil,tarfile,hashlib
from stage_serial401 import ROOT,D,sha
OUT=ROOT/'out/session-a/main-source450'
def main():
 assert not OUT.exists();m=Path(json.loads((D/'MAIN_SYNC_PLAN440.json').read_text())['mainWorktree']);assert not subprocess.check_output(['git','status','--porcelain'],cwd=m)
 head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=m,text=True).strip();assert head==subprocess.check_output(['git','rev-parse','main'],cwd=ROOT,text=True).strip()
 acceptance=json.loads((D/'MAIN_ACCEPTANCE449.json').read_text());assert acceptance['normalSource0Source14AndFinalColdAccepted'];assert all(r['exactRegularFileSha'] for r in acceptance['completeOriginalRestoration'].values())
 b=json.loads((D/'MAIN_BUILD446.json').read_text());files={p:m/p for p in subprocess.check_output(['git','ls-files','-z'],cwd=m).decode().split('\0') if p}
 for row in b['jniGuards']:
  p=m/row['path'];assert sha(p)==row['afterSha256'];files[row['path']]=p
 # Self-contained exporter recipe and path registration, only own docs change.
 for p in [D/'export_main450.py',D/'OWNERSHIP.json']:files[str(p.relative_to(ROOT))]=p
 assert shutil.disk_usage(ROOT).free>2*1024**3;OUT.mkdir();source=OUT/'source';source.mkdir();libc=ctypes.CDLL('/usr/lib/libSystem.B.dylib',use_errno=True);clone=libc.clonefile;clone.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_int];clone.restype=ctypes.c_int;rows=[]
 for name,src in sorted(files.items()):
  before=sha(src);dst=source/name;dst.parent.mkdir(parents=True,exist_ok=True)
  if clone(os.fsencode(src),os.fsencode(dst),0):shutil.copy2(src,dst)
  assert sha(dst)==before and dst.stat().st_ino!=src.stat().st_ino and dst.stat().st_mode==src.stat().st_mode,name
  rows.append({'path':name,'bytes':dst.stat().st_size,'sha256':before,'mode':dst.stat().st_mode&0o777})
 pins=json.loads((source/'tools/content/map-release-manifest.json').read_text())['files'];assert len(pins)==168
 for row in pins:assert sha(source/row['source_path'])==row['sha256']
 archive=OUT/'sanguo11-mobile-main-source.tar'
 with tarfile.open(archive,'w:') as t:
  for row in rows:t.add(source/row['path'],arcname=row['path'])
 with tarfile.open(archive,'r:') as t:
  members={x.name:x for x in t};assert len(members)==len(rows)
  for row in rows:
   x=members[row['path']];assert x.isfile() and x.mode==row['mode'] and hashlib.sha256(t.extractfile(x).read()).hexdigest()==row['sha256']
 index=OUT/'source-files.json';index.write_text(json.dumps(rows,indent=2)+'\n')
 assert head==subprocess.check_output(['git','rev-parse','HEAD'],cwd=m,text=True).strip() and not subprocess.check_output(['git','status','--porcelain'],cwd=m)
 report={'sourceMainCommit':head,'apkCodeCommit':b['sourceRevision'],'mainWorktree':str(m),'archivePath':str(archive),'archiveBytes':archive.stat().st_size,'archiveSha256':sha(archive),'files':len(rows),'fileManifest':str(index),'fileManifestSha256':sha(index),'everyTarMemberShaAndModeVerified':True,'independentSourceInodes':True,'fixedInputsVerified':168,'jniInputs':b['jniGuards'],'deliveredApks':b['apks'],'installedAcceptance':'MAIN_ACCEPTANCE449.json','containsSDKBuildCachesOrUserBackups':False,'sourceOnlyOwnDocExceptions':['docs/handoff/20261006/session-a/OWNERSHIP.json','docs/handoff/20261006/session-a/export_main450.py'],'scope':'Full final main source/resources/tests/tools/docs plus ignored original4/testedA2 JNI, original modes and every tar member verified. No .git history or SDK/cache/userdata. Generated SOURCE_REVISION differs outside Git, so digest changes on rebuild must be separately recorded/installed. Main code05236 and docs7f8; this exporter and registration are the only own-doc additions. Known performance/ARM/media/rule gaps retained; not whole-goal acceptance.','wholeGoalComplete':False};(D/'MAIN_SOURCE451.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'archive':str(archive),'sha256':report['archiveSha256'],'files':len(rows)}),flush=True)
if __name__=='__main__':main()
