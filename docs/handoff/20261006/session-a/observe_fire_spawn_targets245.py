#!/usr/bin/env python3
"""Observe original effect13 nested generation targets; no budget change."""
from pathlib import Path
import json,hashlib,subprocess,struct,os
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/fire-spawn-targets245'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    assert not OUT.exists();OUT.mkdir(parents=True)
    source=ROOT/'tools/content/native';names=['pc_effect_scene_probe.c','pc_effect_vm_probe.c'];before={n:sha(source/n) for n in names}
    for n in names:(OUT/n).write_bytes((source/n).read_bytes())
    p=OUT/'pc_effect_vm_probe.c';s=p.read_text();needle='static void block_budget(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {';assert s.count(needle)==1
    observer='''static uint32_t observed_root;
static void observe_emitter(uc_engine *u,uint64_t address,uint32_t size,void *opaque){
    (void)u;(void)size;Machine *m=opaque;
    if(address==0x457a93){observed_root=reg32(m,UC_X86_REG_ESI);return;}
    if(observed_root!=HEAP+0x200)return;
    uint32_t emitter=reg32(m,UC_X86_REG_ECX),table=reg32(m,UC_X86_REG_EAX),driver=reg32(m,UC_X86_REG_ESI);
    fprintf(stderr,"ORIGINAL_EMITTER owner=%x table=%x target=%x rawKindWord=%x rawPool=%x bytesBefore=%llu originalChildCount=%u index=%u\\n",emitter,table,read32(m,table+0x18),read32(m,emitter+4)&0xffff,read32(m,emitter+0x1c),(unsigned long long)m->executed_code_bytes,read32(m,driver+0x130),reg32(m,UC_X86_REG_EDI));
}
'''
    s=s.replace(needle,observer+needle);needle='    const unsigned char reset[]={0xdb,0xe3,0xc3};';assert s.count(needle)==1
    s=s.replace(needle,'    const uint32_t observed[]={0x457a93,0x460a2f};for(unsigned j=0;j<2;j++){uc_hook h;require_uc(uc_hook_add(m->u,&h,UC_HOOK_CODE,(void*)observe_emitter,m,observed[j],observed[j]));}\n'+needle);p.write_text(s)
    lib=ROOT/'out/session-a/portrait-native-deps/unicorn/lib/libunicorn.a';include=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/unicorn-2.1.4/include')
    cmd=['clang','-std=c11','-O3','-Wall','-Wextra','-Werror','-I',str(include),str(OUT/names[0]),str(lib),'-lm','-lpthread','-o',str(OUT/'host-probe')]
    with (OUT/'compile.log').open('w') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
    assert r.returncode==0,(OUT/'compile.log').read_text()
    env=os.environ.copy();env['PC_VM_PROBE_BLOCK_BUDGET']='1';rows=[]
    for count in [32,64,128]:
        raw=(ROOT/f'out/session-a/fire-capacity-matrix236/cells-{count:03d}/commands.bin').read_bytes();camera=raw[12:216];center=struct.unpack_from('<3f',camera);payload=raw[:436+count*12]+struct.pack('<IIf',3,2,0.)+camera;case=OUT/f'cells-{count:03d}';case.mkdir()
        r=subprocess.run([str(OUT/'host-probe'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin'),*[str(v) for v in center],'--stream'],input=payload,capture_output=True,env=env,timeout=30)
        (case/'stdout.bin').write_bytes(r.stdout);(case/'stderr.txt').write_bytes(r.stderr);log=r.stderr.decode();calls=[dict(x.split('=') for x in line.split()[1:]) for line in log.splitlines() if line.startswith('ORIGINAL_EMITTER ')]
        row={'cells':count,'exit':r.returncode,'calls':calls,'stderr':log,'stderrSha256':sha(case/'stderr.txt'),'stdoutSha256':sha(case/'stdout.bin')};rows.append(row);print(json.dumps({k:row[k] for k in ['cells','exit','calls']}),flush=True)
    for n in names:assert sha(source/n)==before[n]
    report={'sourceBeforeAfter':before,'cases':rows,'compileCommand':cmd,'scope':'Host-only logging before original emitter child-generation virtual call460a2f, scoped by original parent effect13 node; raw fields carry no assumed Android/core semantics. Same first32/64/128 admitted visual input, same5Mbyte/5s limits. No production/JNI/PC memory/RNG/budget changes or Android acceptance.','wholeGoalComplete':False};(DOC/'FIRE_SPAWN_TARGETS245.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
