#!/usr/bin/env python3
"""Measure exact296 real host mesh producer and audit actual311 GC semantics."""
from pathlib import Path
import subprocess,json,hashlib,re,time
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/prepare-allocation313'
JAVA=Path('/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True)
 base=ROOT/'out/session-a/fire-cache-apk296/source'
 assert base.exists()
 cp=[base/n/'build/classes/java/main' for n in ['core','game-api','game-runtime']]
 cp.append(base/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes')
 names=['TileGeometry','GridWorldTransform','SceneCamera','SceneMesh','TerrainMaterialField','TerrainSurface','WaterVisualField','MapSceneSnapshot','FactionColors','SceneFactsPresentation']
 src=[base/f'app/src/main/java/game/sanguo/mobile/{n}.java' for n in names]
 src +=[DOC/'MeshParityProbe.java',DOC/'PrepareAllocationProbe313.java']
 classes=OUT/'classes';classes.mkdir()
 command=[str(JAVA/'javac'),'--release','17','-encoding','UTF-8','-cp',':'.join(map(str,cp)),'-d',str(classes),*map(str,src)]
 subprocess.run(command,check=True)
 cp.insert(0,classes);cp.append(base/'core/src/main/resources');cp.append(base/'core/src/test/resources')
 cmd=[str(JAVA/'java'),'-Xmx384m','-XX:StartFlightRecording=filename='+str(OUT/'producer.jfr')+',settings=profile,dumponexit=true','-cp',':'.join(map(str,cp)),'game.sanguo.mobile.PrepareAllocationProbe313']
 with (OUT/'host.log').open('w') as f:subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,check=True)
 row=next(x for x in (OUT/'host.log').read_text().splitlines() if x.startswith('source14\t')).split('\t')
 raw=ROOT/'out/session-a/prepare-diagnostic-installed311/logcat.txt';lines=raw.read_text(errors='replace').splitlines()
 target=[x for x in lines if re.search(r'\s514\s+527\s+I nguo.mobile.de: .* GC freed',x)]
 # Actual process-specific pause values vs entire concurrent collection duration.
 records=[]
 for line in target:
  match=re.search(r'paused (.+?) total ([\d.]+)(ms|s)',line)
  if not match:continue
  pause=sum(float(n)*(1 if u=='ms' else .001 if u=='us' else 1000) for n,u in re.findall(r'([\d.]+)(ms|us|s)',match[1]))
  records.append({'raw':line,'pauseMillis':pause,'concurrentTotalMillis':float(match[2])*(1000 if match[3]=='s' else 1)})
 ground=[x for x in lines if 'Ground CPU ready chunks=330' in x or 'Field CPU ready forestChunks=55' in x]
 report={'hostProducer':{'chunks':int(row[1]),'actualThreadAllocatedBytes':int(row[2]),'retainedMeshArrayBytes':int(row[3]),'wallNanos':int(row[4]),'threadCpuNanos':int(row[5]),'indexedShaderSha256':row[6],'fullSaveRngPure':True},'sources':[{'path':str(p),'sha256':sha(p)} for p in src],'actual311LogSha256':sha(raw),'actual311GroundRows':ground,'actual311ProcessGcRows':records,'gcConcurrentTotalIsNotPause':True,'androidRootCauseProven':False,'scope':'Exact296 real host CPU source14 mesh producer under384MiB, actual thread allocation and JFR; not normal Android or allocation-stack reproduction. Actual311 GC rows preserve pause separately from concurrent total; no inference that entire42.86s process GC blocked worker. Rendering GPU/native and host scheduling remain separate unknowns.','wholeGoalComplete':False}
 with (OUT/'allocation-samples.txt').open('w') as f:subprocess.run([str(JAVA/'jfr'),'print','--events','jdk.ObjectAllocationSample','--stack-depth','12',str(OUT/'producer.jfr')],stdout=f,check=True)
 samples=[part.strip() for part in (OUT/'allocation-samples.txt').read_text().split('jdk.ObjectAllocationSample {') if 'SceneMesh.ground' in part and 'MeshParityProbe' not in part]
 report['hostJfr']={'path':str(OUT/'producer.jfr'),'sha256':sha(OUT/'producer.jfr'),'producerAllocationSampleCount':len(samples),'selectedSampleRows':samples[:14],'samplingNotExactAllocationCounts':True,'digestAndWorldPreparationSamplesExcluded':True}
 (DOC/'PREPARE_ALLOCATION313.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report['hostProducer']),flush=True)
if __name__=='__main__':main()
