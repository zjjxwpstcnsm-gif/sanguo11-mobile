#!/usr/bin/env python3
"""Log exact existing visual guard cause in a host-only clone, never JNI."""
from pathlib import Path
import json,hashlib,subprocess,os,time
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/fire-capacity-fault231'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    assert not OUT.exists();OUT.mkdir(parents=True)
    source=ROOT/'tools/content/native';names=['pc_effect_scene_probe.c','pc_effect_vm_probe.c'];before={n:sha(source/n) for n in names}
    for n in names:(OUT/n).write_bytes((source/n).read_bytes())
    p=OUT/'pc_effect_vm_probe.c';s=p.read_text()
    old='if(m->executed_code_bytes>5000000) {m->fault=1;uc_emu_stop(u);}'
    assert s.count(old)==1
    s=s.replace(old,'if(m->executed_code_bytes>5000000) {fprintf(stderr,"EXISTING_BLOCK_BYTE_GUARD pc=%llx bytes=%llu\\n",(unsigned long long)address,(unsigned long long)m->executed_code_bytes);m->fault=1;uc_emu_stop(u);}')
    old='fprintf(stderr,"Native boundary/budget failure at%x\\n",address);'
    assert s.count(old)==1
    s=s.replace(old,'fprintf(stderr,"Native boundary/budget failure at%x; FAULT_DETAIL pc=%x fault=%d codeBytes=%llu next=%x allocatedBytes=%u allocations=%u timedOut=%d\\n",address,reg32(m,UC_X86_REG_EIP),m->fault,(unsigned long long)m->executed_code_bytes,m->next,m->bytes,m->allocations,d.timed_out);')
    p.write_text(s)
    lib=ROOT/'out/session-a/portrait-native-deps/unicorn/lib/libunicorn.a';include=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/unicorn-2.1.4/include')
    command=['clang','-std=c11','-O3','-Wall','-Wextra','-Werror','-I',str(include),str(OUT/names[0]),str(lib),'-lm','-lpthread','-o',str(OUT/'host-probe')]
    with (OUT/'compile.log').open('w') as f:c=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
    assert c.returncode==0,(OUT/'compile.log').read_text()
    raw=(ROOT/'out/session-a/fire-controller-capacity230/commands.bin').read_bytes();import struct
    center=struct.unpack_from('<3f',raw,12);env=os.environ.copy();env['PC_VM_PROBE_BLOCK_BUDGET']='1'
    env.pop('PC_VM_PROBE_TRACE',None);env.pop('PC_VM_PROBE_HEAP_PATH',None);env.pop('PC_VM_PROBE_VISUAL_RNG_PATH',None)
    began=time.monotonic();r=subprocess.run([str(OUT/'host-probe'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin'),*[str(v) for v in center],'--stream'],input=raw,capture_output=True,env=env,timeout=30)
    (OUT/'stdout.bin').write_bytes(r.stdout);(OUT/'stderr.txt').write_bytes(r.stderr)
    for n in names:assert sha(source/n)==before[n]
    log=r.stderr.decode();report={'exit':r.returncode,'wallSeconds':time.monotonic()-began,'sourceBeforeAfterExact':before,'diagnosticSourceSha256':{n:sha(OUT/n) for n in names},'compileCommand':command,'staticUnicornSha256':sha(lib),'stderr':log,'stderrSha256':sha(OUT/'stderr.txt'),'stdoutSha256':sha(OUT/'stdout.bin'),'productionBlockByteGuardObserved':'EXISTING_BLOCK_BYTE_GUARD' in log,'scope':'Host-only native clone adds two logging statements at existing guard boundaries. Same commands as230, same original kernel/scene and existing5M block-byte/5s deadlines. Canonical source and allJNI untouched; no Android128/Java OOM/GPU/ARM proof or increased budget.','wholeGoalComplete':False}
    (DOC/'FIRE_CAPACITY_FAULT231.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k not in ('compileCommand','sourceBeforeAfterExact','diagnosticSourceSha256')},ensure_ascii=False))
if __name__=='__main__':main()
