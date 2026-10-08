#!/usr/bin/env python3
"""Measure actual TCG block instruction counts, preserving candidate byte cap."""
from pathlib import Path
import json,hashlib,subprocess,struct,os,time
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/instruction-lifecycle255'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    assert not OUT.exists();OUT.mkdir(parents=True)
    src=ROOT/'tools/content/native';names=['pc_effect_scene_probe.c','pc_effect_vm_probe.c','session_a_cell_fire_budget_worker.c'];guards={n:sha(src/n) for n in names}
    for n in names:(OUT/n).write_bytes((src/n).read_bytes())
    p=OUT/names[-1];s=p.read_text();needle='static void admitted_block_budget(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {';assert s.count(needle)==1
    body='''typedef struct {uint64_t pc;uint32_t bytes;uint16_t instructions;uint32_t epoch;} CountCache;
static CountCache count_cache[65536];
static uint64_t call_instructions,instruction_peak,metadata_queries,metadata_mismatches;
static uint64_t previous_call_bytes;static uint32_t call_epoch;
static void report_instruction_accounting(void){
    fprintf(stderr,"INSTRUCTION_ACCOUNTING peak=%llu queries=%llu mismatches=%llu\\n",
        (unsigned long long)instruction_peak,(unsigned long long)metadata_queries,(unsigned long long)metadata_mismatches);
}
'''
    s=s.replace(needle,body+needle)
    needle='    Machine *m=opaque;m->executed_code_bytes+=size;';assert s.count(needle)==1
    replacement='''    Machine *m=opaque;
    if(m->executed_code_bytes==0||m->executed_code_bytes<previous_call_bytes){call_instructions=0;call_epoch++;}
    uint32_t slot=(uint32_t)(((address>>2)^size)&65535);CountCache *entry=&count_cache[slot];
    uint16_t instructions=0;
    if(entry->pc==address&&entry->bytes==size&&((address>=0x400000&&address<0x900000)||entry->epoch==call_epoch))instructions=entry->instructions;
    else{
        uc_tb tb={0};uc_err result=uc_ctl_request_cache(u,address,&tb);metadata_queries++;
        if(result!=UC_ERR_OK||tb.pc!=address||tb.size!=size||!tb.icount||tb.icount>size){
            metadata_mismatches++;fprintf(stderr,"TB_METADATA_MISMATCH pc=%llx hookBytes=%u result=%u tbpc=%llx tbsize=%u count=%u\\n",(unsigned long long)address,size,result,(unsigned long long)tb.pc,tb.size,tb.icount);m->fault=1;uc_emu_stop(u);return;
        }
        instructions=tb.icount;
        *entry=(CountCache){address,size,instructions,call_epoch};
    }
    call_instructions+=instructions;if(call_instructions>instruction_peak)instruction_peak=call_instructions;
    m->executed_code_bytes+=size;previous_call_bytes=m->executed_code_bytes;'''
    s=s.replace(needle,replacement)
    needle='if(atexit(report_admitted_budget)){return UC_ERR_RESOURCE;}';assert s.count(needle)==1;s=s.replace(needle,'if(atexit(report_admitted_budget)||atexit(report_instruction_accounting)){return UC_ERR_RESOURCE;}');p.write_text(s)
    lib=ROOT/'out/session-a/portrait-native-deps/unicorn/lib/libunicorn.a';include=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/unicorn-2.1.4/include')
    command=['clang','-std=c11','-O3','-Wall','-Wextra','-Werror','-I',str(include),str(p),str(lib),'-lm','-lpthread','-o',str(OUT/'host-probe')]
    with (OUT/'compile.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
    assert r.returncode==0,(OUT/'compile.log').read_text()
    rows=[];env=os.environ.copy();env['PC_VM_PROBE_BLOCK_BUDGET']='1'
    for count in [128]:
        source=ROOT/'out/session-a/fire-budget-lifecycle249/long128/commands.bin';raw=source.read_bytes();camera=raw[12:216];center=struct.unpack_from('<3f',camera)
        payload=raw;case=OUT/f'cells-{count:03d}';case.mkdir();start=time.monotonic()
        r=subprocess.run([str(OUT/'host-probe'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin'),*[str(v) for v in center],'--stream'],input=payload,capture_output=True,env=env,timeout=240)
        (case/'stdout.bin').write_bytes(r.stdout);(case/'stderr.txt').write_bytes(r.stderr);log=r.stderr.decode();row={'cells':count,'exit':r.returncode,'wallSeconds':time.monotonic()-start,'stderr':log,'stderrSha256':sha(case/'stderr.txt'),'stdoutSha256':sha(case/'stdout.bin')};rows.append(row);print(json.dumps(row),flush=True)
    for n in names:assert sha(src/n)==guards[n]
    report={'cases':rows,'sourceBeforeAfter':guards,'compileCommand':command,'instructionCacheSlots':65536,'scope':'Host-only measurement using actual uc_tb.icount/size returned by immutable Unicorn2.1.4 uc_ctl_request_cache. Bounded65536-entry metadata cache. Static code may reuse across calls; mutable adapters/geometry epoch expires every native call. Same249 long128 payload and20M cap retained. Byte quota20M and5s deadline unchanged; mismatch rejects, never guessed counts. Canonical code/JNI/World/RNG unchanged; no Android capacity or performance acceptance.','wholeGoalComplete':False};(DOC/'INSTRUCTION_LIFECYCLE255.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
