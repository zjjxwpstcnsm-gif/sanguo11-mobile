#!/usr/bin/env python3
"""Read-only current full external file SHA against an existing completed backup; never restore/install."""
import pathlib,subprocess,hashlib,tarfile,json,time,re
ROOT=pathlib.Path(__file__).resolve().parents[4];OUT=ROOT/'out/session-a/backup-reuse-audit';OUT.mkdir(parents=True,exist_ok=True);ADB=pathlib.Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platform-tools/adb');prefix=[str(ADB),'-s','emulator-5554'];old=pathlib.Path('/Users/paopao/.codex/worktrees/scenario-officer-restoration/sanguo11-mobile/out/session1/installed-batch26-all16/source-15-cold/external-before.tar')
assert subprocess.run(prefix+['shell','pidof','game.sanguo.mobile.dev'],capture_output=True,text=True).returncode!=0,'application active, stop read-only audit'
started=time.monotonic();r=subprocess.run(prefix+['shell',"cd /sdcard/Android/data/game.sanguo.mobile.dev && find . -type f -exec sha256sum '{}' '+'"],capture_output=True,text=True,check=True);(OUT/'current-external-sha.txt').write_text(r.stdout)
current={}
for line in r.stdout.splitlines():
 m=re.fullmatch(r'([0-9a-f]{64})  (.+)',line);assert m,line
 name=m[2].removeprefix('./');assert name not in current;current[name]=m[1]
print('Current external files',len(current),flush=True)
prior={};total=0
with tarfile.open(old,'r:') as archive:
 for member in archive:
  if not member.isfile():continue
  name=member.name.removeprefix('./');h=hashlib.sha256();f=archive.extractfile(member)
  for block in iter(lambda:f.read(1048576),b''):h.update(block)
  assert name not in prior;prior[name]=h.hexdigest();total+=member.size
assert subprocess.run(prefix+['shell','pidof','game.sanguo.mobile.dev'],capture_output=True,text=True).returncode!=0,'application became active during audit'
missing=sorted(set(current)-set(prior));extra=sorted(set(prior)-set(current));changed=sorted(n for n in set(current)&set(prior) if current[n]!=prior[n]);match=not missing and not extra and not changed
report={'scope':'read-only emulator external regular files vs old complete backup; no installation/restore; must refresh internal/prefs and reacquire exclusive lock before use','serial':'emulator-5554','archive':str(old),'archiveFileCount':len(prior),'archiveLogicalFileBytes':total,'deviceFileCount':len(current),'missingFromArchiveCount':len(missing),'extraArchiveCount':len(extra),'differentShaCount':len(changed),'exactCurrentBackupMatch':match,'currentSetDigest':hashlib.sha256(json.dumps(current,sort_keys=True).encode()).hexdigest(),'priorSetDigest':hashlib.sha256(json.dumps(prior,sort_keys=True).encode()).hexdigest(),'seconds':time.monotonic()-started,'mayReuseAsCurrentFullBackup':match,'normalAndroidAccepted':False,'completeGoal':False}
(ROOT/'docs/handoff/20261006/session-a/BACKUP_REUSE_AUDIT.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False))
