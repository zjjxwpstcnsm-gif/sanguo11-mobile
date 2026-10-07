#!/usr/bin/env python3
"""Compile complete latest committed B parent plus the standalone39 test only.

Every current WIP path remains untouched; no production overlay is admitted.
Original app assets, fixed resources, A frozen dependencies and JNI are guarded
by the full input capture and freeze, rather than copied from an old checkout.
"""
import hashlib,io,json,subprocess,tarfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
BASE='89534120e46e488136db0e661270757d9c4a4c50'
STAGE=ROOT/'out/session-b/completed-legacy39-stage'
RUNNER='app/src/androidTest/java/game/sanguo/mobile/SessionBLegacy39Instrumentation.java'
ASSET='app/src/androidTest/assets/session-b/legacy39-original.sg11'
PREFIXES=['core/src/main/java','core/src/main/resources','game-api/src/main/java','game-runtime/src/main/java','app/src/main/java','core/src/testFixtures/java','app/src/androidTest/java']
def sha(raw):return hashlib.sha256(raw).hexdigest()
def main():
 if subprocess.run(['git','merge-base','--is-ancestor',BASE,'HEAD'],cwd=ROOT).returncode!=0:raise ValueError('Completed parent is not inherited')
 if subprocess.check_output(['git','diff','--name-only',BASE,'HEAD','--','core/src/main','game-api/src/main','game-runtime/src/main','app/src/main'],cwd=ROOT):raise ValueError('Re-audit newer committed production before staging')
 STAGE.mkdir(exist_ok=False);rows=[]
 packed=subprocess.check_output(['git','archive','--format=tar',BASE,*PREFIXES],cwd=ROOT)
 with tarfile.open(fileobj=io.BytesIO(packed))as archive:
  for m in archive:
   if m.isdir():continue
   if not m.isfile()or m.name.startswith('/')or '..'in Path(m.name).parts:raise ValueError('Unsafe parent path')
   if 'PcDuel' in m.name or 'PcNativeItemPolicy' in m.name:raise ValueError('Unfinished Native producer entered completed parent')
   raw=archive.extractfile(m).read();dest=STAGE/m.name;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(raw)
   rows.append(dict(logical=m.name,path=str(dest.relative_to(ROOT)),parentSha256=sha(raw),compiledSha256=sha(raw),overlay=False))
 for name in [RUNNER,ASSET]:
  raw=(ROOT/name).read_bytes()
  if name==ASSET and sha(raw)!='5d002d3cee6f84700cd5e1292c05b672e8a05e0b547ac7d753f2b7987d5e2f7f':raise ValueError('Genuine39 source changed')
  dest=STAGE/name;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(raw)
  rows.append(dict(logical=name,path=str(dest.relative_to(ROOT)),parentSha256=None,compiledSha256=sha(raw),overlay=True))
 if sum(r['overlay']for r in rows)!=2:raise ValueError('Exact test-only overlays required')
 (STAGE/'manifest.json').write_text(json.dumps(dict(base=BASE,compiled=rows,overlays=[RUNNER,ASSET],productionOverlays=[],nativeWipCompiled=False,actualApkAccepted=False),indent=2)+'\n')
 print('PASS staged complete latest committed parent',BASE,len(rows),'paths; standalone39 runner/asset only')
if __name__=='__main__':main()
