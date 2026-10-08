#!/usr/bin/env python3
"""Read original effect13 instance list at stop/restart, unchanged candidate cap."""
from pathlib import Path
import json,hashlib,subprocess,struct,os,time
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/fire-population251'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    assert not OUT.exists();OUT.mkdir(parents=True)
    src=ROOT/'tools/content/native';names=['pc_effect_scene_probe.c','pc_effect_vm_probe.c','session_a_cell_fire_budget_worker.c'];guards={n:sha(src/n) for n in names}
    for n in names:(OUT/n).write_bytes((src/n).read_bytes())
    p=OUT/'session_a_cell_fire_budget_worker.c';s=p.read_text();needle='    if(count>admitted_highwater){';assert s.count(needle)==1
    trace='''    static uint32_t frame;
    frame++;
    if(frame==1||frame>=598){
        uint32_t slots=read32(m,HEAP+0x8000+0x2e4),root=read32(m,slots+13*4);
        uint32_t nodes=read32(m,root+0x68),node=read32(m,nodes),driver=read32(m,node+0x80);
        uint32_t list=read32(m,driver+0x20),emitter=read32(m,list),pool=read32(m,emitter+0x1c);
        uint32_t pools=0,total=0,flag1=0,flag4=0,joined=0;
        while(pool){
            if(++pools>4096){fprintf(stderr,"POPULATION_BOUND pools\\n");m->fault=1;uc_emu_stop(u);return;}
            uint32_t item=read32(m,pool+0x1c);
            while(item){
                if(++total>40000){fprintf(stderr,"POPULATION_BOUND instances\\n");m->fault=1;uc_emu_stop(u);return;}
                uint32_t flags=(read32(m,item+4)>>16)&255;flag1+=(flags&1)!=0;flag4+=(flags&4)!=0;
                for(uint32_t k=0;k<40000;k++)if(fire_handles[k]==item){joined++;break;}
                item=read32(m,item+0xc);
            }
            pool=read32(m,pool+0x14);
        }
        fprintf(stderr,"POPULATION frame=%u admitted=%u pools=%u total=%u rawFlag1=%u rawFlag4=%u joined=%u guestBytes=%u allocations=%u\\n",frame,count,pools,total,flag1,flag4,joined,m->bytes,m->allocations);
    }
'''
    s=s.replace(needle,trace+needle);p.write_text(s)
    lib=ROOT/'out/session-a/portrait-native-deps/unicorn/lib/libunicorn.a';include=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/unicorn-2.1.4/include')
    cmd=['clang','-std=c11','-O3','-Wall','-Wextra','-Werror','-I',str(include),str(OUT/names[-1]),str(lib),'-lm','-lpthread','-o',str(OUT/'host-probe')]
    with (OUT/'compile.log').open('w') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
    assert r.returncode==0,(OUT/'compile.log').read_text()
    payload=(ROOT/'out/session-a/fire-budget-lifecycle249/long128/commands.bin').read_bytes();center=struct.unpack_from('<3f',payload,12);env=os.environ.copy();env['PC_VM_PROBE_BLOCK_BUDGET']='1';start=time.monotonic()
    r=subprocess.run([str(OUT/'host-probe'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin'),*[str(v) for v in center],'--stream'],input=payload,capture_output=True,env=env,timeout=240)
    (OUT/'stdout.bin').write_bytes(r.stdout);(OUT/'stderr.txt').write_bytes(r.stderr)
    for n in names:assert sha(src/n)==guards[n]
    log=r.stderr.decode();rows=[dict(x.split('=') for x in line.split()[1:]) for line in log.splitlines() if line.startswith('POPULATION ')]
    report={'exit':r.returncode,'wallSeconds':time.monotonic()-start,'population':rows,'sourceBeforeAfter':guards,'compileCommand':cmd,'stderr':log,'stderrSha256':sha(OUT/'stderr.txt'),'stdoutSha256':sha(OUT/'stdout.bin'),'scope':'Host-only original effect13 instance traversal follows observed46099c..460b21 pool/item next pointers. Same249 long128 commands/camera/height0/20M cap; raw flags are not inferred gameplay state. Only PC memory reads/C diagnostics; canonical/PC/JNI/World/RNG untouched. Used to distinguish actual retained original instances from budget margin after stop/restart, not normal Android acceptance.','wholeGoalComplete':False};(DOC/'FIRE_POPULATION251.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k not in ('stderr','compileCommand')},ensure_ascii=False))
if __name__=='__main__':main()
