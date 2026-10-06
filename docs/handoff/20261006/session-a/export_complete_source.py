#!/usr/bin/env python3
"""Export a complete inherited source checkpoint plus six exact JNI; no build/cache/device backup inclusion."""
import argparse,hashlib,json,pathlib,subprocess,tarfile,gzip,io,os
ROOT=pathlib.Path(__file__).resolve().parents[4]
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
def sha(raw):return hashlib.sha256(raw).hexdigest()
p=argparse.ArgumentParser();p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args();out=a.output.resolve();assert not out.exists();assert not git('status','--porcelain').strip(),'Commit owned checkpoint first';out.mkdir(parents=True)
head=git('rev-parse','HEAD').decode().strip();names=set(git('ls-files','-z').decode().split('\0'))-{''};guard=json.loads((ROOT/'docs/handoff/20261006/session-a/SOURCE_GUARD.json').read_text());names|={r['path'] for r in guard['files']};inherit=json.loads((ROOT/'docs/handoff/20261006/session-a/INHERITANCE.json').read_text());jni={r['path']:r['sha256'] for r in inherit['jni']}
for abi in ['arm64-v8a','x86_64']:
 name=f'out/session-a/pc-fire-runtime/additional-jniLibs/{abi}/libpc_effect_fire_worker.so';jni[name]=sha((ROOT/name).read_bytes())
names|=set(jni);manifest=[];archive=out/'sanguo11-mobile-source.tar.gz'
def info(name,data,mode):
 i=tarfile.TarInfo('sanguo11-mobile/'+name);i.size=len(data);i.mode=mode;i.mtime=0;i.uid=i.gid=0;i.uname=i.gname='';return i
with archive.open('xb') as target,gzip.GzipFile(filename='',mode='wb',fileobj=target,mtime=0,compresslevel=1) as zipped,tarfile.open(fileobj=zipped,mode='w') as tar:
 for name in sorted(names):
  file=ROOT/name;assert file.is_file() and not file.is_symlink(),name;raw=file.read_bytes();digest=sha(raw)
  if name in jni:assert digest==jni[name],name
  mode=0o755 if os.access(file,os.X_OK) else 0o644;manifest.append({'path':name,'bytes':len(raw),'sha256':digest,'mode':mode});tar.addfile(info(name,raw,mode),io.BytesIO(raw))
 descriptor={'commonBase':inherit['base'],'revision':head,'branch':git('branch','--show-current').decode().strip(),'fileCount':len(manifest),'bytes':sum(r['bytes'] for r in manifest),'files':manifest,'scope':'tracked plus every inherited SOURCE_GUARD file and original4/new2 JNI, no Git database/Gradle/build/SDK/device backups; checkpoint not final whole-goal or ARM acceptance'};raw=(json.dumps(descriptor,ensure_ascii=False,indent=2)+'\n').encode();tar.addfile(info('SOURCE_SNAPSHOT.json',raw,0o644),io.BytesIO(raw))
assert git('rev-parse','HEAD').decode().strip()==head and not git('status','--porcelain').strip()
verified=0
with tarfile.open(archive,'r:gz') as tar:
 for row in manifest:
  member=tar.extractfile('sanguo11-mobile/'+row['path']);assert member is not None;digest=hashlib.sha256()
  while True:
   chunk=member.read(1048576)
   if not chunk:break
   digest.update(chunk)
  assert digest.hexdigest()==row['sha256'],row['path'];assert sha((ROOT/row['path']).read_bytes())==row['sha256'],row['path'];verified+=1
report={k:v for k,v in descriptor.items() if k!='files'};report.update(archivePath=str(archive),archiveBytes=archive.stat().st_size,archiveSha256=sha(archive.read_bytes()),verifiedEveryFileSha=verified,original4AndNew2Jni=jni,gitDatabaseIncluded=False,deviceBackupsIncluded=False,overallGoalComplete=False);(out/'source-files.json').write_text(json.dumps(descriptor,ensure_ascii=False,indent=2)+'\n');(out/'export.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False,indent=2))
