#!/usr/bin/env python3
"""Read original effect13 child-loop work without changing execution budgets."""
from pathlib import Path
import json,hashlib,subprocess,struct,os
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/fire-instance-work241'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    assert not OUT.exists();OUT.mkdir(parents=True)
    source=ROOT/'tools/content/native';names=['pc_effect_scene_probe.c','pc_effect_vm_probe.c'];before={n:sha(source/n) for n in names}
    for n in names:(OUT/n).write_bytes((source/n).read_bytes())
    p=OUT/'pc_effect_vm_probe.c';s=p.read_text();needle='static void block_budget(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {';assert s.count(needle)==1
    trace='''static uint32_t observed_root;
static void observe_original_work(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {
    (void)u;(void)size;Machine *m=opaque;
    if(address==0x457a93){observed_root=reg32(m,UC_X86_REG_ESI);return;}
    if(observed_root!=HEAP+0x200)return;
    uint32_t driver=reg32(m,UC_X86_REG_ESI),field=address==0x46d4a7?8:address==0x46d4c7?0x14:0x10;
    fprintf(stderr,"ORIGINAL_CHILD_LOOP call=%llx driver=%x count=%u index=%u owner=%x bytesBefore=%llu\\n",
        (unsigned long long)address,driver,read32(m,driver+field),reg32(m,UC_X86_REG_EDI),reg32(m,UC_X86_REG_ECX),(unsigned long long)m->executed_code_bytes);
}
'''
    s=s.replace(needle,trace+needle);old='if(m->executed_code_bytes>5000000) {m->fault=1;uc_emu_stop(u);}';assert s.count(old)==1;s=s.replace(old,'if(m->executed_code_bytes>5000000) {fprintf(stderr,"EXISTING_BLOCK_BYTE_GUARD pc=%llx bytes=%llu\\n",(unsigned long long)address,(unsigned long long)m->executed_code_bytes);m->fault=1;uc_emu_stop(u);}')
    needle='    const unsigned char reset[]={0xdb,0xe3,0xc3};';assert s.count(needle)==1
    s=s.replace(needle,'    const uint32_t observed_calls[]={0x457a93,0x46d4a7,0x46d4c7,0x46d4e7};\n    for(unsigned j=0;j<4;j++){uc_hook h;require_uc(uc_hook_add(m->u,&h,UC_HOOK_CODE,(void*)observe_original_work,m,observed_calls[j],observed_calls[j]));}\n'+needle);p.write_text(s)
    lib=ROOT/'out/session-a/portrait-native-deps/unicorn/lib/libunicorn.a';include=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/unicorn-2.1.4/include')
    command=['clang','-std=c11','-O3','-Wall','-Wextra','-Werror','-I',str(include),str(OUT/names[0]),str(lib),'-lm','-lpthread','-o',str(OUT/'host-probe')]
    with (OUT/'compile.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
    assert r.returncode==0,(OUT/'compile.log').read_text()
    env=os.environ.copy();env['PC_VM_PROBE_BLOCK_BUDGET']='1';rows=[]
    for count in [32,64,128]:
        raw=(ROOT/f'out/session-a/fire-capacity-matrix236/cells-{count:03d}/commands.bin').read_bytes();camera=raw[12:216];center=struct.unpack_from('<3f',camera)
        payload=raw[:436+count*12]+struct.pack('<IIf',3,2,0.)+camera;case=OUT/f'cells-{count:03d}';case.mkdir()
        r=subprocess.run([str(OUT/'host-probe'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin'),*[str(v) for v in center],'--stream'],input=payload,capture_output=True,env=env,timeout=30)
        (case/'stdout.bin').write_bytes(r.stdout);(case/'stderr.txt').write_bytes(r.stderr);log=r.stderr.decode();observations=[dict(x.split('=') for x in line.split()[1:]) for line in log.splitlines() if line.startswith('ORIGINAL_CHILD_LOOP ')]
        rows.append({'cells':count,'exit':r.returncode,'observations':observations,'stderr':log,'stderrSha256':sha(case/'stderr.txt'),'stdoutSha256':sha(case/'stdout.bin')});print(json.dumps({'cells':count,'exit':r.returncode,'observedChildren':len(observations),'first':observations[:2],'last':observations[-2:]}),flush=True)
    for n in names:assert sha(source/n)==before[n]
    report={'originalSourceBeforeAfter':before,'originalEffect13TemplateRoot':'HEAP+0x200 from verified9-template packed scene order8/9/13/16/17/18/19/20/23','cases':rows,'compileCommand':command,'scope':'Host-only logging at actual original46d400 three child-loop calls, scoped by actual457a93 parent to the verified effect13 template. Unchanged5Mbyte/5s budget; no source VM writes/reset/scaled budget or canonical/JNI/World/rule/RNG changes. Counts describe original loop shape, not assumed Android field semantics or normal128 acceptance.','wholeGoalComplete':False}
    (DOC/'FIRE_INSTANCE_WORK241.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
