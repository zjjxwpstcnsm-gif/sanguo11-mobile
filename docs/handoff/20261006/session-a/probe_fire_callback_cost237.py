#!/usr/bin/env python3
"""Observe original callback boundaries at unchanged evaluator budget."""
from pathlib import Path
import json,hashlib,subprocess,struct,os,re
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/fire-callback-cost237'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    assert not OUT.exists();OUT.mkdir(parents=True)
    source=ROOT/'tools/content/native';names=['pc_effect_scene_probe.c','pc_effect_vm_probe.c'];guards={n:sha(source/n) for n in names}
    for n in names:(OUT/n).write_bytes((source/n).read_bytes())
    p=OUT/'pc_effect_vm_probe.c';s=p.read_text()
    needle='static void block_budget(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {'
    assert s.count(needle)==1
    callback='''static void debug_node_call(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {
        (void)u;(void)address;(void)size;Machine *m=opaque;
        uint32_t root=reg32(m,UC_X86_REG_ESI),node=reg32(m,UC_X86_REG_ECX);
        fprintf(stderr,"NODE_CALL root=%x node=%x target=%x declaredNodes=%u bytesBefore=%llu\\n",
            root,node,read32(m,read32(m,node)+0x14),read32(m,root+0x6c),(unsigned long long)m->executed_code_bytes);
    }
'''
    s=s.replace(needle,callback+needle)
    old='if(m->executed_code_bytes>5000000) {m->fault=1;uc_emu_stop(u);}'
    assert s.count(old)==1;s=s.replace(old,'if(m->executed_code_bytes>5000000) {fprintf(stderr,"EXISTING_BLOCK_BYTE_GUARD pc=%llx bytes=%llu\\n",(unsigned long long)address,(unsigned long long)m->executed_code_bytes);m->fault=1;uc_emu_stop(u);}')
    needle='    const unsigned char reset[]={0xdb,0xe3,0xc3};'
    assert s.count(needle)==1;s=s.replace(needle,'    uc_hook debug;require_uc(uc_hook_add(m->u,&debug,UC_HOOK_CODE,(void*)debug_node_call,m,0x457a93,0x457a93));\n'+needle);p.write_text(s)
    lib=ROOT/'out/session-a/portrait-native-deps/unicorn/lib/libunicorn.a';include=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/unicorn-2.1.4/include')
    cmd=['clang','-std=c11','-O3','-Wall','-Wextra','-Werror','-I',str(include),str(OUT/names[0]),str(lib),'-lm','-lpthread','-o',str(OUT/'host-probe')]
    with (OUT/'compile.log').open('w') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
    assert r.returncode==0,(OUT/'compile.log').read_text()
    rows=[];env=os.environ.copy();env['PC_VM_PROBE_BLOCK_BUDGET']='1'
    for count in [32,64,128]:
        # Exactly the first admitted update from immutable matrix236.
        raw=(ROOT/f'out/session-a/fire-capacity-matrix236/cells-{count:03d}/commands.bin').read_bytes();camera=raw[12:216];center=struct.unpack_from('<3f',camera)
        payload=raw[:216+220+count*12]+struct.pack('<IIf',3,2,0.)+camera
        case=OUT/f'cells-{count:03d}';case.mkdir();(case/'commands.bin').write_bytes(payload)
        r=subprocess.run([str(OUT/'host-probe'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin'),*[str(x) for x in center],'--stream'],input=payload,capture_output=True,env=env,timeout=30)
        (case/'stdout.bin').write_bytes(r.stdout);(case/'stderr.txt').write_bytes(r.stderr);text=r.stderr.decode()
        observations=[]
        for line in text.splitlines():
            if line.startswith('NODE_CALL '):
                values=dict(item.split('=') for item in line.split()[1:]);observations.append(values)
        rows.append({'cells':count,'exit':r.returncode,'callbacks':observations,'lastCallbackBeforeFailure':observations[-1] if observations and r.returncode else None,'stderr':text,'stderrSha256':sha(case/'stderr.txt'),'stdoutSha256':sha(case/'stdout.bin')})
        print(json.dumps({k:v for k,v in rows[-1].items() if k not in ('callbacks','stderr')}),flush=True)
    for n in names:assert sha(source/n)==guards[n]
    report={'sourceBeforeAfterExact':guards,'diagnosticSourceSha256':{n:sha(OUT/n) for n in names},'staticUnicornSha256':sha(lib),'compileCommand':cmd,'originalCallbackAddress':'0x457a93','cases':rows,'scope':'Host-only original manager node-update observation at32/64/128, first update, unchanged5Mbyte/5s limits. Original callback register/PC memory reads and C logging only; no reset/increased budgets/patch to original VM. Canonical code/JNI/currentAPK/World/save/ruleRNG unchanged. No Android/ARM/GPU/performance acceptance.','wholeGoalComplete':False}
    (DOC/'FIRE_CALLBACK_COST237.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
