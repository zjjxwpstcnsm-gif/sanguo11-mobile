#!/usr/bin/env python3
"""Owned staged media delta with exact before/after guards, no B WIP import."""
from pathlib import Path
import json,hashlib,tarfile,subprocess
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/media-delta282'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True);portrait=json.loads((DOC/'ORIGINAL_PORTRAIT_BOUNDS274.json').read_text());audio=json.loads((DOC/'ORIGINAL_MISS_AUDIO278.json').read_text());assert portrait['compiled'] and portrait['patchReadbackExact'] and audio['compiled'] and audio['patchReadbackExact'];paths=[{k:portrait[k] for k in ['path','beforeSha256','afterSha256']},*audio['changes']];sources={portrait['path']:Path(portrait['stagePath'])}
 for row in audio['changes']:sources[row['path']]=ROOT/'out/session-a/original-miss-audio278'/row['path']
 targets=[]
 for row in paths:
  assert sha(ROOT/row['path'])==row['beforeSha256'] and sha(sources[row['path']])==row['afterSha256'];target=OUT/'source'/row['path'];target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(sources[row['path']].read_bytes());targets.append(target)
 asset='app/src/main/assets/audio/pc/tactic-58.wav';assert not (ROOT/asset).exists();source=Path(audio['stageAsset']);assert sha(source)==audio['assetSha256'];target=OUT/'source'/asset;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(source.read_bytes());targets.append(target);paths.append({'path':asset,'beforeSha256':None,'afterSha256':sha(target)})
 classes=OUT/'classes';classes.mkdir();java=Path('/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin/java');android=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platforms/android-35/android.jar');base=ROOT/'out/session-a/map-fire-upload-build269/source/app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes';old=ROOT/'out/session-a/native-opening-stage224/compile';dependencies=ROOT/'out/session-a/native-opening-stage224/dependencies';cp=':'.join(map(str,[android,old/'filament.jar',base]+sorted(dependencies.glob('*.jar'))));command=[str(java),'-Xmx512m','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-cp',cp,'-d',str(classes),*[str(t) for t in targets if t.suffix=='.java']]
 with (OUT/'compile.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode==0,(OUT/'compile.log').read_text();archive=OUT/'a-media-delta.tar.gz'
 with tarfile.open(archive,'w:gz') as t:
  for row in paths:t.add(OUT/'source'/row['path'],arcname=row['path'])
 with tarfile.open(archive) as t:
  assert sorted(m.name for m in t.getmembers())==sorted(row['path'] for row in paths)
  for row in paths:assert hashlib.sha256(t.extractfile(row['path']).read()).hexdigest()==row['afterSha256']
 report={'commonBase':'0e7b9bc2df90249a50851baeda58c7d183ea6059','archivePath':str(archive),'archiveSha256':sha(archive),'paths':paths,'pathCount':len(paths),'combinedCompilePassed':True,'compileCommand':command,'compileLogSha256':sha(OUT/'compile.log'),'everyArchiveFileReadbackExact':True,'canonicalUnchanged':True,'apk269And280Unchanged':True,'normalMissProducerBound':False,'actualInstalled':False,'scope':'Five owned staged paths: original portrait full rectangular drawing, original58 source-rate bounded player/dispatcher support, metadata and raw PCM asset. Exact predecessor hashes and absence guard mandatory; do not blindly overlay B current source. No API/Bridge/Unity/Rule/JNI/synthetic-label change. No normal58 trigger or PC small-family/normal output acceptance created;277 factual producer required. Independent normal APK/fullSaveRNGToken/PCM/lifecycle/ARM still pending.','wholeGoalComplete':False};(DOC/'A_MEDIA_DELTA282.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k not in ['compileCommand','paths']}))
if __name__=='__main__':main()
