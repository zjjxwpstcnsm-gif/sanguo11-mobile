#!/usr/bin/env python3
"""Archive complete89534120 plus accepted39 test/tools and frozen inputs.

Only compiled inputs and completed-source overlays are admitted. User backups
and all other Native WIP remain in the original worktree, outside this archive.
"""
import gzip,hashlib,io,json,subprocess,tarfile
from pathlib import Path
from session_b_freeze_apk import ROOT
BASE='89534120e46e488136db0e661270757d9c4a4c50'
STAGE='out/session-b/completed-legacy39-stage'
OUTPUT=ROOT/'out/session-b/completed-legacy39-source.tar.gz'
MANIFEST=ROOT/'out/session-b/completed-legacy39-source-manifest.json'
ADDITIONS=['app/src/androidTest/java/game/sanguo/mobile/SessionBLegacy39Instrumentation.java','app/src/androidTest/assets/session-b/legacy39-original.sg11','docs/handoff/20261006/session-b/legacy39-completed.init.gradle','tools/content/session_b_stage_completed_legacy39.py','tools/content/session_b_freeze_completed_legacy39.py','tools/content/session_b_verify_legacy39_ui.py','tools/content/session_b_archive_completed_legacy39.py','docs/handoff/20261006/session-b/COMPLETED_LEGACY39.md']
def sha(raw):return hashlib.sha256(raw).hexdigest()
def file_sha(path):
 h=hashlib.sha256()
 with path.open('rb')as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 return h.hexdigest()
def main():
 if OUTPUT.exists()or MANIFEST.exists():raise ValueError('Preserve earlier archive')
 proof=json.loads((ROOT/'out/session-b/legacy39-completed-ui59/results.json').read_text())
 if not proof.get('passed'):raise ValueError('Independent completed39 driver/UI/cold/restoration must pass')
 stage=json.loads((ROOT/STAGE/'manifest.json').read_text());assert stage['base']==BASE and stage['productionOverlays']==[]
 for row in stage['compiled']:
  assert file_sha(ROOT/row['path'])==row['compiledSha256']
  if row['overlay']:assert file_sha(ROOT/row['logical'])==row['compiledSha256']
 jni=json.loads((ROOT/'docs/handoff/20261006/session-b/INHERITANCE.json').read_text())['jni']
 for row in jni:assert file_sha(ROOT/row['path'])==row['sha256']
 extras=set(ADDITIONS+[r['path']for r in jni])
 for prefix in [STAGE,'out/session-b/readonly-theme-dependencies','app/src/main/assets']:
  extras.update(str(p.relative_to(ROOT))for p in (ROOT/prefix).rglob('*')if p.is_file())
 rows=[];parent_files=set(subprocess.check_output(['git','ls-tree','-r','--name-only','-z',BASE],cwd=ROOT).decode().split('\0'))-{''}
 parent=subprocess.Popen(['git','archive','--format=tar',BASE],cwd=ROOT,stdout=subprocess.PIPE)
 try:
  with OUTPUT.open('xb')as stream,gzip.GzipFile(fileobj=stream,mode='wb',filename='',mtime=0,compresslevel=1)as compressed,tarfile.open(fileobj=compressed,mode='w|',format=tarfile.PAX_FORMAT)as dest,tarfile.open(fileobj=parent.stdout,mode='r|')as source:
   seen=set()
   for m in source:
    if m.isdir():continue
    if not m.isfile()or m.name.startswith('/')or '..'in Path(m.name).parts:raise ValueError('Unsafe parent entry')
    raw=source.extractfile(m).read();
    if m.name.startswith('app/src/main/assets/')and (ROOT/m.name).read_bytes()!=raw:raise ValueError('Compiled tracked asset differs from completed parent')
    dest.addfile(m,io.BytesIO(raw));rows.append(dict(path=m.name,bytes=len(raw),sha256=sha(raw),mode=m.mode,origin='complete-parent'));seen.add(m.name)
   assert seen==parent_files
   for name in sorted(extras-seen):
    p=ROOT/name;raw=p.read_bytes();m=tarfile.TarInfo(name);m.size=len(raw);m.mode=p.stat().st_mode&0o777;dest.addfile(m,io.BytesIO(raw));rows.append(dict(path=name,bytes=len(raw),sha256=sha(raw),mode=m.mode,origin='completed-test-tool-or-frozen-input'))
  if parent.wait()!=0:raise ValueError('Parent archive failed')
 finally:
  parent.stdout.close()
  if parent.poll()is None:parent.terminate();parent.wait()
 expected={r['path']:r for r in rows};seen=set()
 with tarfile.open(OUTPUT,'r|gz')as archive:
  for m in archive:
   row=expected[m.name];assert m.isfile()and m.mode==row['mode']and sha(archive.extractfile(m).read())==row['sha256']and m.name not in seen;seen.add(m.name)
 assert seen==set(expected)
 MANIFEST.write_text(json.dumps(dict(base=BASE,rows=rows,fileCount=len(rows),bytes=OUTPUT.stat().st_size,archiveSha256=file_sha(OUTPUT),everyEntryAndModeReadbackVerified=True,productionOverlays=[],acceptedApkSha=proof['installed']['game.sanguo.mobile.dev'],jni=jni,limits=['One genuine39 explicit policy adoption and continuation; full Native goal remains incomplete','All other Native WIP/user backups/SDK excluded','Frozen A dependencies retain47326188fbc43837c8d52caf0fa76051390faaf4 origin']),indent=2)+'\n')
 print('PASS completed legacy39 source archive',len(rows),file_sha(OUTPUT),OUTPUT.stat().st_size)
if __name__=='__main__':main()
