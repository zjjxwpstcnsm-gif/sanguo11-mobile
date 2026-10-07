#!/usr/bin/env python3
"""Log actual factory handles and original live-instance update boundaries."""
from pathlib import Path
import json,hashlib,subprocess,struct,os
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/fire-live-instances244'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    assert not OUT.exists();OUT.mkdir(parents=True)
    source=ROOT/'tools/content/native';names=['pc_effect_scene_probe.c','pc_effect_vm_probe.c'];before={n:sha(source/n) for n in names}
    for n in names:(OUT/n).write_bytes((source/n).read_bytes())
    p=OUT/'pc_effect_scene_probe.c';s=p.read_text();needle='fire_handles[key]=handle;fire_generations[key]=generation;created++;total_created++;';assert s.count(needle)==1
    s=s.replace(needle,'fprintf(stderr,"FACTORY_INSTANCE x=%u y=%u handle=%x generation=%x\\n",x,y,handle,generation);'+needle);p.write_text(s)
    p=OUT/'pc_effect_vm_probe.c';s=p.read_text();needle='static void block_budget(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {';assert s.count(needle)==1
    trace='''static uint32_t observed_root;
static void observe_live_instance(uc_engine *u,uint64_t address,uint32_t size,void *opaque){
    (void)u;(void)size;Machine *m=opaque;
    if(address==0x457a93){observed_root=reg32(m,UC_X86_REG_ESI);return;}
    if(observed_root!=HEAP+0x200)return;
    uint32_t sp=reg32(m,UC_X86_REG_ESP),instance=read32(m,sp+4);
    fprintf(stderr,"LIVE_INSTANCE emitter=%x instance=%x return=%x bytesBefore=%llu\\n",reg32(m,UC_X86_REG_ECX),instance,read32(m,sp),(unsigned long long)m->executed_code_bytes);
}
'''
    s=s.replace(needle,trace+needle);needle='    const unsigned char reset[]={0xdb,0xe3,0xc3};';assert s.count(needle)==1
    s=s.replace(needle,'    const uint32_t observed[]={0x457a93,0x45ca40};for(unsigned j=0;j<2;j++){uc_hook h;require_uc(uc_hook_add(m->u,&h,UC_HOOK_CODE,(void*)observe_live_instance,m,observed[j],observed[j]));}\n'+needle);p.write_text(s)
    lib=ROOT/'out/session-a/portrait-native-deps/unicorn/lib/libunicorn.a';include=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/unicorn-2.1.4/include')
    command=['clang','-std=c11','-O3','-Wall','-Wextra','-Werror','-I',str(include),str(OUT/names[0]),str(lib),'-lm','-lpthread','-o',str(OUT/'host-probe')]
    with (OUT/'compile.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
    assert r.returncode==0,(OUT/'compile.log').read_text()
    env=os.environ.copy();env['PC_VM_PROBE_BLOCK_BUDGET']='1';rows=[]
    for count in [32,64,128]:
        raw=(ROOT/f'out/session-a/fire-capacity-matrix236/cells-{count:03d}/commands.bin').read_bytes();camera=raw[12:216];center=struct.unpack_from('<3f',camera);payload=raw[:436+count*12]+struct.pack('<IIf',3,2,0.)+camera;case=OUT/f'cells-{count:03d}';case.mkdir()
        r=subprocess.run([str(OUT/'host-probe'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin'),*[str(v) for v in center],'--stream'],input=payload,capture_output=True,env=env,timeout=30)
        (case/'stdout.bin').write_bytes(r.stdout);(case/'stderr.txt').write_bytes(r.stderr);log=r.stderr.decode();facts=[dict(x.split('=') for x in line.split()[1:]) for line in log.splitlines() if line.startswith('FACTORY_INSTANCE ')];calls=[dict(x.split('=') for x in line.split()[1:]) for line in log.splitlines() if line.startswith('LIVE_INSTANCE ')]
        row={'cells':count,'exit':r.returncode,'factory':facts,'calls':calls,'factoryHandleEqualObservedInstanceCount':len({x['handle'] for x in facts}&{x['instance'] for x in calls}),'stderrSha256':sha(case/'stderr.txt'),'stdoutSha256':sha(case/'stdout.bin')};rows.append(row);print(json.dumps({'cells':count,'exit':r.returncode,'factoryCount':len(facts),'observedCalls':len(calls),'directHandleMatches':row['factoryHandleEqualObservedInstanceCount'],'firstFactory':facts[:1],'firstCalls':calls[:2],'lastCalls':calls[-2:]}),flush=True)
    for n in names:assert sha(source/n)==before[n]
    report={'sourceBeforeAfterExact':before,'cases':rows,'compileCommand':command,'scope':'Host-only logging of original factory13 return handle/generation and original45ca40 instance callback within exact effect13 template. Same first32/64/128 diagnostic inputs and5Mbyte/5s budget. Raw handles/addresses not asserted core officer/cell identity without observed join; no reset/increase/PC memory/production/JNI/RNG changes or Android acceptance.','wholeGoalComplete':False};(DOC/'FIRE_LIVE_INSTANCES244.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
