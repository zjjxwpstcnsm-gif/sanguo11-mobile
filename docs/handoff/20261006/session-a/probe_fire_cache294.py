#!/usr/bin/env python3
"""Host-only bounded four-way instruction metadata cache, exact source output."""
from pathlib import Path
import json,hashlib,subprocess,os,time,re,struct
from probe_admitted_fire_budget246 import semantic
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/fire-cache294'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True)
 src=ROOT/'tools/content/native';names=['pc_effect_scene_probe.c','pc_effect_vm_probe.c','session_a_cell_fire_instruction_worker.c'];guards={n:sha(src/n) for n in names}
 for n in names:(OUT/n).write_bytes((src/n).read_bytes())
 p=OUT/names[-1];s=p.read_text();needle='static CountCache count_cache[65536];';assert s.count(needle)==1
 s=s.replace(needle,needle+'\nstatic uint8_t count_replacement[16384];\nstatic uint64_t cache_hits,cache_collisions;')
 needle='    uint32_t slot=(uint32_t)(((address>>2)^size)&65535);CountCache *entry=&count_cache[slot];'
 replacement='''    uint32_t hash=(uint32_t)address*UINT32_C(0x9e3779b1)^(size*UINT32_C(0x85ebca6b));
    uint32_t set=(hash^(hash>>16))&16383;
    CountCache *entry=NULL;
    for(uint32_t way=0;way<4;way++){
        CountCache *candidate=&count_cache[set*4+way];
        if(candidate->pc==address&&candidate->bytes==size){entry=candidate;break;}
    }
    if(!entry){
        uint8_t way=count_replacement[set];entry=&count_cache[set*4+way];
        count_replacement[set]=(way+1)&3;
        if(entry->pc)cache_collisions++;
    }'''
 assert s.count(needle)==1;s=s.replace(needle,replacement)
 needle='))instructions=entry->instructions;';assert s.count(needle)==1;s=s.replace(needle,')){instructions=entry->instructions;cache_hits++;}')
 needle='static void report_instruction_accounting(void){';assert s.count(needle)==1;s=s.replace(needle,needle+'\n    fprintf(stderr,"COUNT_CACHE hits=%llu collisions=%llu slots=65536 replacementBytes=16384\\n",(unsigned long long)cache_hits,(unsigned long long)cache_collisions);')
 p.write_text(s);lib=ROOT/'out/session-a/portrait-native-deps/unicorn/lib/libunicorn.a';include=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/unicorn-2.1.4/include');binary=OUT/'host-probe'
 command=['clang','-std=c11','-O3','-Wall','-Wextra','-Werror','-I',str(include),str(p),str(lib),'-lm','-lpthread','-o',str(binary)]
 with (OUT/'compile.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode==0,(OUT/'compile.log').read_text()
 original=ROOT/'out/session-a/instruction-budget257/host-probe';assert original.is_file();env=os.environ.copy();env['PC_VM_PROBE_BLOCK_BUDGET']='1';full=(ROOT/'out/session-a/fire-budget-lifecycle249/long128/commands.bin').read_bytes();camera=full[12:216];center=struct.unpack_from('<3f',camera);cells=[(x,y,0.) for x in range(150,158) for y in range(80,96)]
 def command_bytes(kind,serial,dt,active=None):
  raw=struct.pack('<IIf',kind,serial,dt)+camera
  if active is not None:raw+=struct.pack('<I',len(active))+b''.join(struct.pack('<IIf',*x) for x in active)
  return raw
 cases=[]
 for count in [2,128]:
  active=cells[:count];steps=[(.1,active)]*600+[(0.,active)]+[(.1,[])]*40+[(.1,active)]*120+[(.1,[])]*40;payload=command_bytes(0,0,0.)+b''.join(command_bytes(4,i+1,dt,c) for i,(dt,c) in enumerate(steps))+command_bytes(3,len(steps)+1,0.)
  case=OUT/('cells-'+str(count));case.mkdir();(case/'commands.bin').write_bytes(payload);results=[];record_bytes=[]
  for label,worker in [('baseline257',original),('four-way294',binary)]:
   began=time.monotonic();r=subprocess.run([str(worker),str(ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin'),*[str(x) for x in center],'--stream'],input=payload,capture_output=True,env=env,timeout=240)
   out=case/(label+'.bin');err=case/(label+'.stderr');out.write_bytes(r.stdout);err.write_bytes(r.stderr);log=r.stderr.decode();records,frames=semantic(r.stdout);record_bytes.append(records);metadata=re.search(r'INSTRUCTION_ACCOUNTING peak=(\d+) queries=(\d+) mismatches=(\d+)',log)
   row={'label':label,'exitCode':r.returncode,'wallSeconds':time.monotonic()-began,'framesCompleted':len(frames),'instructionPeak':int(metadata[1]) if metadata else None,'metadataQueries':int(metadata[2]) if metadata else None,'metadataMismatches':int(metadata[3]) if metadata else None,'maximumCallMs':{k:max((x[k] for x in frames),default=0) for k in ['updateMs','drawMs','geometryMs']},'stdoutSha256':sha(out),'stderrSha256':sha(err),'pauseExact':len(frames)>600 and frames[599]['elapsed']==frames[600]['elapsed'] and frames[599]['packetSha256']==frames[600]['packetSha256']};results.append(row);print(json.dumps({'cells':count,**row}),flush=True)
   assert r.returncode==0 and len(frames)==801 and row['metadataMismatches']==0 and row['pauseExact']
  exact=record_bytes[0]==record_bytes[1];assert exact
  cases.append({'cells':count,'recordsAndSourceClockByteExact':exact,'results':results,'metadataQueryRatio':results[1]['metadataQueries']/results[0]['metadataQueries'],'wallRatio':results[1]['wallSeconds']/results[0]['wallSeconds']})
 for n,h in guards.items():assert sha(src/n)==h
 report={'cases':cases,'sourceBeforeUnchanged':guards,'candidateSource':str(p),'candidateSourceSha256':sha(p),'compileCommand':command,'instructionCacheSlots':65536,'extraReplacementBytes':16384,'native5sAndJava6sUnchanged':True,'guest16MiBTcg32MiBPacket32768AndAdmission128Unchanged':True,'androidWatchdogRootCauseProven':False,'actualInstalled':False,'scope':'Host-only cache associativity/mixed-key experiment; same icount matching and dynamic per-call epoch. Original source instructions/resource/order/clock/visualRNG output compared bytewise2/128 each801 lifecycle frames. No rule World/save/RNG or current JNI/APK rewrite. Actual293 Android timeout was below instruction cap; metadata overhead causal relation remains unknown until actual independent normal APK acceptance.','wholeGoalComplete':False}
 (DOC/'FIRE_CACHE_CANDIDATE294.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'candidateHostVerified':True,'cases':cases}),flush=True)
if __name__=='__main__':main()
