#!/usr/bin/env python3
"""Two owned presentation successors against immutable A closure227."""
from pathlib import Path
import json,hashlib,tarfile,subprocess
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/presentation-delta261'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True)
 base=json.loads((DOC/'CANDIDATE_A_CLOSURE227.json').read_text());archive=Path(base['overlayPath']);assert sha(archive)==base['overlaySha256'];rows=[];targets=[]
 hints=json.loads((DOC/'NORMAL_MAP_HINT_STAGE235.json').read_text());upload=json.loads((DOC/'MAP_EFFECT_UPLOAD258.json').read_text())
 with tarfile.open(archive,'r:gz') as t:
  for d,stage in [(hints,Path(hints['stagePath'])),(upload,ROOT/'out/session-a/map-effect-upload258'/upload['path'])]:
   name=d['path'];member=t.getmember(name);raw=t.extractfile(member).read();assert hashlib.sha256(raw).hexdigest()==d['beforeSha256'];assert sha(ROOT/name)==d['beforeSha256'] and sha(stage)==d['afterSha256'];target=OUT/'source'/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(stage.read_bytes());targets.append(target);rows.append({'path':name,'owner':'A','closure227BeforeSha256':d['beforeSha256'],'afterSha256':d['afterSha256'],'bytes':target.stat().st_size})
 basecompile=ROOT/'out/session-a/native-opening-stage224/compile';dependencies=ROOT/'out/session-a/native-opening-stage224/dependencies';android=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platforms/android-35/android.jar');java=Path('/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin/java');cp=':'.join(map(str,[android,basecompile/'filament.jar',basecompile/'classes']+sorted(dependencies.glob('*.jar'))));classes=OUT/'classes';classes.mkdir();command=[str(java),'-Xmx512m','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-cp',cp,'-d',str(classes),*map(str,targets)]
 with (OUT/'compile.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode==0,(OUT/'compile.log').read_text()
 output=OUT/'a-presentation-delta.tar.gz'
 with tarfile.open(output,'w:gz') as t:
  for target,row in zip(targets,rows):t.add(target,arcname=row['path'])
 with tarfile.open(output,'r:gz') as t:
  assert sorted(m.name for m in t.getmembers())==sorted(row['path'] for row in rows)
  for row in rows:assert hashlib.sha256(t.extractfile(row['path']).read()).hexdigest()==row['afterSha256'];assert sha(ROOT/row['path'])==row['closure227BeforeSha256']
 report={'commonBase':base['commonBase'],'predecessorClosure227Sha256':base['overlaySha256'],'archivePath':str(output),'archiveSha256':sha(output),'paths':rows,'combinedCompilePassed':True,'compileCommand':command,'compileLogSha256':sha(OUT/'compile.log'),'everyArchiveFileReadbackExact':True,'canonicalUnchanged':True,'actualInstalled':False,'scope':'Only two A presentation successor files after immutable37-path227. Does not include test250 or uninstalled native260. Exact predecessor checks required; conflicting production must be audited rather than overwritten. No B source/WIP/Bridge/Unity/Gradle/JNI changes. Compile/readback only; fresh final combined APK installation and normal Save/RNG/Token/memory/pixels/input/media acceptance required.','wholeGoalComplete':False};(DOC/'A_PRESENTATION_DELTA261.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
