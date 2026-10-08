/* Session A candidate: source-count-bounded visual work, not a rule change.
 * Existing B/native sources are included read-only. This separate executable
 * keeps source objects, RNG, ordering, protocol, allocator and5s deadlines.
 * Host diagnostics only until frozen additional-JNI and Android acceptance.
 */
#include <unicorn/unicorn.h>
static uc_err session_a_fire_hook(uc_engine *,uc_hook *,int,void *,void *,uint64_t,uint64_t);
#define uc_hook_add session_a_fire_hook
#include "pc_effect_scene_probe.c"
#undef uc_hook_add

static uint32_t admitted_highwater;
static uint64_t observed_call_peak;
static int budget_report_registered;
static uint64_t admitted_byte_limit(void) {
    /* A bounded engineering quota, not an original PC numeric rule. The old
     * quota completed the measured32-controller workload; admission never
     * exceeds128. Retain the attained quota through stop/drain of that scene.
     */
    uint32_t batches=(admitted_highwater+31)/32;
    return UINT64_C(5000000)*(batches?batches:1);
}
static void report_admitted_budget(void) {
    fprintf(stderr,"PC_CELL_BUDGET_PEAK bytes=%llu highwater=%u limit=%llu\n",
        (unsigned long long)observed_call_peak,admitted_highwater,
        (unsigned long long)admitted_byte_limit());
}
static void observe_admitted_controllers(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {
    (void)address;(void)size;Machine *m=opaque;uint32_t count=0;
    /* These are the exact handles retained by schema2 source_fire_sync after
     * validating all source cells and executing original factory13. No core
     * query, guessed identity, camera omission, retry or PC memory write.
     */
    for(uint32_t key=0;key<40000;key++)if(fire_handles[key])count++;
    if(count>128){m->fault=1;uc_emu_stop(u);return;}
    if(count>admitted_highwater){
        admitted_highwater=count;
        fprintf(stderr,"PC_CELL_ADMISSION active=%u highwater=%u limit=%llu\n",count,
            admitted_highwater,(unsigned long long)admitted_byte_limit());
    }
}
typedef struct {uint64_t pc;uint32_t bytes;uint16_t instructions;uint32_t epoch;} CountCache;
static CountCache count_cache[65536];
static uint8_t count_replacement[16384];
static uint64_t cache_hits,cache_collisions;
static uint64_t call_instructions,instruction_peak,metadata_queries,metadata_mismatches;
static uint64_t previous_call_bytes;static uint32_t call_epoch;
static void report_instruction_accounting(void){
    fprintf(stderr,"COUNT_CACHE hits=%llu collisions=%llu slots=65536 replacementBytes=16384\n",(unsigned long long)cache_hits,(unsigned long long)cache_collisions);
    fprintf(stderr,"INSTRUCTION_ACCOUNTING peak=%llu queries=%llu mismatches=%llu\n",
        (unsigned long long)instruction_peak,(unsigned long long)metadata_queries,(unsigned long long)metadata_mismatches);
}
static void admitted_block_budget(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {
    Machine *m=opaque;
    if(m->executed_code_bytes==0||m->executed_code_bytes<previous_call_bytes){call_instructions=0;call_epoch++;}
    uint32_t hash=(uint32_t)address*UINT32_C(0x9e3779b1)^(size*UINT32_C(0x85ebca6b));
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
    }
    uint16_t instructions=0;
    if(entry->pc==address&&entry->bytes==size&&((address>=0x400000&&address<0x900000)||entry->epoch==call_epoch)){instructions=entry->instructions;cache_hits++;}
    else{
        uc_tb tb={0};uc_err result=uc_ctl_request_cache(u,address,&tb);metadata_queries++;
        if(result!=UC_ERR_OK||tb.pc!=address||tb.size!=size||!tb.icount||tb.icount>size){
            metadata_mismatches++;fprintf(stderr,"TB_METADATA_MISMATCH pc=%llx hookBytes=%u result=%u tbpc=%llx tbsize=%u count=%u\n",(unsigned long long)address,size,result,(unsigned long long)tb.pc,tb.size,tb.icount);m->fault=1;uc_emu_stop(u);return;
        }
        instructions=tb.icount;
        *entry=(CountCache){address,size,instructions,call_epoch};
    }
    call_instructions+=instructions;if(call_instructions>instruction_peak)instruction_peak=call_instructions;
    m->executed_code_bytes+=size;previous_call_bytes=m->executed_code_bytes;
    if(m->executed_code_bytes>observed_call_peak)observed_call_peak=m->executed_code_bytes;
    if(call_instructions>admitted_byte_limit()){
        fprintf(stderr,"PC_CELL_INSTRUCTION_GUARD pc=%llx instructions=%llu limit=%llu\n",
            (unsigned long long)address,(unsigned long long)call_instructions,
            (unsigned long long)admitted_byte_limit());
        m->fault=1;uc_emu_stop(u);
    }
}
static uc_err session_a_fire_hook(uc_engine *u,uc_hook *handle,int type,void *callback,
        void *opaque,uint64_t begin,uint64_t end) {
    if(type==UC_HOOK_BLOCK&&callback==(void*)block_budget){
        /* Replace only this separate visual child's accounting callback.
         * call() still resets each native call and retains its original5s
         * watchdog. TCG32MiB, guest heap16MiB, packet32768 and Java6s limits
         * are unchanged; original4 executables are never rebuilt here.
         */
        uc_err result=uc_hook_add(u,handle,type,(void*)admitted_block_budget,opaque,begin,end);
        if(result!=UC_ERR_OK)return result;
        uc_hook admission;result=uc_hook_add(u,&admission,UC_HOOK_CODE,
            (void*)observe_admitted_controllers,opaque,0x45a530,0x45a530);
        if(result==UC_ERR_OK&&!budget_report_registered){
            if(atexit(report_admitted_budget)||atexit(report_instruction_accounting)){return UC_ERR_RESOURCE;}
            budget_report_registered=1;
        }
        return result;
    }
    return uc_hook_add(u,handle,type,callback,opaque,begin,end);
}
