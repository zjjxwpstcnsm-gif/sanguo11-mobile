#!/usr/bin/env python3
"""Reflink only unchanged own-checkout inputs; atomic replacement, complete SHA/mode/time guards."""
import pathlib,json,subprocess,hashlib,shutil,stat,os
ROOT=pathlib.Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';base=json.loads((DOC/'SOURCE_GUARD.json').read_text());SOURCE=pathlib.Path(base['source']);TEMP=ROOT/'out/session-a/reflink-staging';TEMP.mkdir(parents=True,exist_ok=True)
def sha(path):
 h=hashlib.sha256()
 with path.open('rb') as f:
  for block in iter(lambda:f.read(1048576),b''):h.update(block)
 return h.hexdigest()
free=shutil.disk_usage(ROOT).free;rows=[];skipped=[];sourceHead=subprocess.check_output(['git','rev-parse','HEAD'],cwd=SOURCE,text=True).strip();assert sourceHead==base['head']
for index,row in enumerate(sorted(base['files'],key=lambda x:-x['bytes'])):
 if row['bytes']<65536:continue
 path=ROOT/row['path'];source=SOURCE/row['path']
 if not path.is_file() or path.is_symlink() or sha(path)!=row['sha256']:skipped.append(row['path']);continue
 assert source.is_file() and not source.is_symlink() and sha(source)==row['sha256'],str(source)
 old=path.stat();stage=TEMP/f'input-{index}.clone';assert not stage.exists()
 try:
  subprocess.run(['/bin/cp','-c','-p',str(source),str(stage)],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
  assert sha(stage)==row['sha256'];assert stage.stat().st_ino!=source.stat().st_ino
  os.chmod(stage,stat.S_IMODE(old.st_mode));os.utime(stage,ns=(old.st_atime_ns,old.st_mtime_ns));os.replace(stage,path)
  assert sha(path)==row['sha256'];assert path.stat().st_ino!=source.stat().st_ino
  rows.append({'path':row['path'],'bytes':row['bytes'],'sha256BeforeAndAfter':row['sha256'],'independentInode':True})
 except Exception:
  if stage.exists():stage.unlink()
  raise
report={'source':str(SOURCE),'sourceHead':sourceHead,'ownCheckout':str(ROOT),'strategy':'APFS clone into private staging, verify SHA, preserve own mode/times, atomic replace; no hardlinks','userResourcePathsDeleted':[],'originalDirectoriesWritten':False,'sameByteFilesConverted':len(rows),'sameByteTotal':sum(r['bytes'] for r in rows),'freeBefore':free,'freeAfter':shutil.disk_usage(ROOT).free,'freeIncrease':shutil.disk_usage(ROOT).free-free,'changedOwnedFilesSkipped':skipped,'files':rows}
(DOC/'REFLINK_STORAGE_AUDIT.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k not in ['files','changedOwnedFilesSkipped']},ensure_ascii=False))
