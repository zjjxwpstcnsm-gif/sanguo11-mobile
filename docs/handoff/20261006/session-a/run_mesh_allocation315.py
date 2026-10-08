#!/usr/bin/env python3
"""Exact296 versus staged314 raw attributes and real host allocated bytes."""
from pathlib import Path
import json,subprocess,hashlib
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/mesh-allocation315'
JAVA=Path('/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True)
 delta=json.loads((DOC/'MESH_ALLOCATION_DELTA314.json').read_text());row=delta['paths'][0]
 base=ROOT/'out/session-a/fire-cache-apk296/source';common=ROOT/'out/session-a/prepare-allocation313/classes'
 cp=[common,*[base/n/'build/classes/java/main' for n in ['core','game-api','game-runtime']],base/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes',base/'core/src/main/resources',base/'core/src/test/resources']
 results={}
 for name,p,h in [('before',base/row['path'],row['beforeSha256']),('after',Path(row['stagedPath']),row['afterSha256'])]:
  assert sha(p)==h;folder=OUT/name;folder.mkdir()
  subprocess.run([str(JAVA/'javac'),'--release','17','-cp',':'.join(map(str,cp)),'-d',str(folder),str(p),str(DOC/'MeshAllocationProbe315.java')],check=True)
  log=folder/'producer.tsv'
  with log.open('w') as f:subprocess.run([str(JAVA/'java'),'-Xmx384m','-cp',':'.join(map(str,[folder,*cp])),'game.sanguo.mobile.MeshAllocationProbe315'],stdout=f,stderr=subprocess.STDOUT,check=True)
  records=[x.split('\t') for x in log.read_text().splitlines()];assert len(records)==10;results[name]={'rows':records,'logSha256':sha(log)}
 for a,b in zip(results['before']['rows'],results['after']['rows']):assert a[:5]==b[:5],(a,b)
 before=sum(int(x[-1]) for x in results['before']['rows']);after=sum(int(x[-1]) for x in results['after']['rows']);assert after<before
 report={'rawParityPassed':True,'hostProducerAllocatedBytesBefore':before,'hostProducerAllocatedBytesAfter':after,'reductionBytes':before-after,'results':results,'deltaReceiptSha256':sha(DOC/'MESH_ALLOCATION_DELTA314.json'),'fullSaveRngPure':True,'publishedArraysImmutableAndCacheReuseExact':True,'androidInstalled':False,'scope':'Exact raw floatbits/index order/streams/fingerprint/chunk bounds/water/grid/LOD for10 actual producer windows, source14 national and repeated near/far, legacy real saved fixture. Actual host ThreadMXBean allocation counters384MiB. Not Android normal paths, device peak/native/GPU, all16 acceptance, ARM or unique OOM root.','wholeGoalComplete':False}
 (DOC/'MESH_ALLOCATION315.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='results'}),flush=True)
if __name__=='__main__':main()
