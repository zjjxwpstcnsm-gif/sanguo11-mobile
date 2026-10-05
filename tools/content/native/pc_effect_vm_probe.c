/* Host feasibility probe for the verified original effect evaluator.
 * No game state, game RNG, Android renderer or guessed effect controller.
 * Only the same bounded allocation / named platform boundaries as
 * pc_effect_machine.py are adapted. Instruction hooks cover those addresses
 * rather than calling a Python observer for every original instruction.
 * This is not yet an Android runtime or a supported application entry point.
 */
#define _POSIX_C_SOURCE 200809L
#include <unicorn/unicorn.h>
#include <unicorn/x86.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include <pthread.h>
#include <errno.h>

#define HEAP 0x10000000u
#define STACK 0x20000000u
#define STOP 0x30000000u
#define DRAW_BASE 0x31000000u
#define BATCH_BASE 0x32000000u
#define MAX_PACKETS 32768u
typedef struct {
    uint32_t primitive,pointer,depth_bits,flags,length;
    unsigned char raw[0x70];
    unsigned char vertices[96],matrix[64];
    uint32_t matrix_writes,draws,locks;
    uint32_t texture_index,blend_operation,blend_source,blend_destination;
    uint32_t vertex_offset,lock_flags,draw_first;
} DrawPacket;
typedef struct {
    uc_engine *u;
    uint32_t next, allocations, bytes, attempts, successes;
    int fault;
    DrawPacket *packets;
    uint32_t packet_count;
    uint64_t executed_code_bytes;
    DrawPacket *active_quad;
    uint32_t packet_queue;
    uint32_t texture_index,blend_operation,blend_source,blend_destination;
} Machine;
typedef struct {
    uc_engine *u;
    pthread_mutex_t mutex;
    pthread_cond_t condition;
    struct timespec deadline;
    int completed,timed_out;
} CallDeadline;
static void *wait_deadline(void *opaque) {
    CallDeadline *d=opaque;
    if(pthread_mutex_lock(&d->mutex))return NULL;
    while(!d->completed) {
        int result=pthread_cond_timedwait(&d->condition,&d->mutex,&d->deadline);
        if(result==ETIMEDOUT) {
            d->timed_out=1;uc_emu_stop(d->u);break;
        }
        if(result) {d->timed_out=1;uc_emu_stop(d->u);break;}
    }
    pthread_mutex_unlock(&d->mutex);return NULL;
}
static void require_uc(uc_err err) {
    if (err != UC_ERR_OK) { fprintf(stderr,"Unicorn: %s\n",uc_strerror(err));exit(2); }
}
static uint32_t read32(Machine *m,uint32_t p) {
    uint32_t v;require_uc(uc_mem_read(m->u,p,&v,4));return v;
}
static uint32_t reg32(Machine *m,int reg) {
    uint64_t v=0;require_uc(uc_reg_read(m->u,reg,&v));return (uint32_t)v;
}
static void write_reg(Machine *m,int reg,uint32_t v) {uint64_t value=v;require_uc(uc_reg_write(m->u,reg,&value));}
static void block_budget(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {
    (void)address;Machine *m=opaque;
    // Every x86 instruction occupies at least one byte. Accounting executed
    // block bytes is a conservative upper bound on executed instructions,
    // retaining the5M cap without Unicorn's per-instruction count hook.
    // Repeated blocks are charged on every entry, including adapted calls.
    m->executed_code_bytes+=size;
    if(m->executed_code_bytes>5000000) {m->fault=1;uc_emu_stop(u);}
}
static void record_draw(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {
    (void)address;(void)size;Machine *m=opaque;
    uint32_t sp=reg32(m,UC_X86_REG_ESP),ret=read32(m,sp),payload=read32(m,sp+4);
    uint32_t primitive=read32(m,payload);
    if(reg32(m,UC_X86_REG_ECX)!=m->packet_queue || primitive>7 || m->packet_count>=MAX_PACKETS) {
        m->fault=1;uc_emu_stop(u);return;
    }
    DrawPacket *p=m->packets+m->packet_count++;
    memset(p,0,sizeof(*p));
    p->primitive=primitive;p->pointer=payload;p->depth_bits=read32(m,sp+8);p->flags=read32(m,sp+12);
    p->length=(primitive==4||primitive==5)?0x10:(primitive==6||primitive==7)?0x14:0x70;
    require_uc(uc_mem_read(u,payload,p->raw,p->length));
    // Native458650 observation retains the original flags/bucket/link writes.
    // Only the diagnostic STOP callback adapts the draw-queue boundary.
    if(address!=0x458650) {
        write_reg(m,UC_X86_REG_EAX,1);write_reg(m,UC_X86_REG_ESP,sp+16);write_reg(m,UC_X86_REG_EIP,ret);
    }
}
static void quad_device_boundary(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {
    (void)size;Machine *m=opaque;DrawPacket *p=m->active_quad;
    uint32_t sp=reg32(m,UC_X86_REG_ESP),ret=read32(m,sp),pop=0;
    const uint32_t device=DRAW_BASE+0x3000,buffer=DRAW_BASE+0x4000,vertices=DRAW_BASE+0x8000;
    if(address==STOP+0x340||address==STOP+0x360) {
        if(read32(m,sp+4)!=device)goto fault;
        uint32_t key=read32(m,sp+8),value=read32(m,sp+12);
        if(address==STOP+0x340) {
            if(key||value<1||value>33)goto fault;
            m->texture_index=value-1;
        } else if(key==171)m->blend_operation=value;
        else if(key==19)m->blend_source=value;
        else if(key==20)m->blend_destination=value;
        else goto fault;
        pop=12;
    } else
    if(!p)goto fault;
    else if(address==0x44d560) {
        if(read32(m,sp+4)!=device||read32(m,sp+8)!=30||p->matrix_writes++)goto fault;
        require_uc(uc_mem_read(u,read32(m,sp+12),p->matrix,64));
    } else if(address==0x44d160) {
        if(read32(m,sp+4)!=buffer||read32(m,sp+12)!=96||p->locks++)goto fault;
        p->vertex_offset=read32(m,sp+8);p->lock_flags=read32(m,sp+20);
        if(p->vertex_offset%24||p->vertex_offset>0xc000-96||(p->lock_flags!=0x1000&&p->lock_flags!=0x2000))goto fault;
        require_uc(uc_mem_write(u,read32(m,sp+16),&vertices,4));
    } else if(address==STOP+0x300) {pop=4;}
    else if(address==STOP+0x320) {
        p->draw_first=read32(m,sp+12);
        if(read32(m,sp+4)!=device||read32(m,sp+8)!=5||p->draw_first!=p->vertex_offset/24||read32(m,sp+16)!=2||p->draws++)goto fault;
        require_uc(uc_mem_read(u,vertices,p->vertices,96));pop=16;
        p->texture_index=m->texture_index;p->blend_operation=m->blend_operation;
        p->blend_source=m->blend_source;p->blend_destination=m->blend_destination;
    } else goto fault;
    write_reg(m,UC_X86_REG_EAX,0);write_reg(m,UC_X86_REG_ESP,sp+4+pop);write_reg(m,UC_X86_REG_EIP,ret);return;
fault:
    if(getenv("PC_VM_PROBE_TRACE"))fprintf(stderr,"GPU boundary %llx sp%x args %x %x %x %x active %p\n",
        (unsigned long long)address,sp,read32(m,sp+4),read32(m,sp+8),read32(m,sp+12),read32(m,sp+16),(void*)p);
    m->fault=1;uc_emu_stop(u);
}
static void batch_quad_dispatch(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {
    (void)address;(void)size;Machine *m=opaque;
    uint32_t sp=reg32(m,UC_X86_REG_ESP),payload=read32(m,sp+8);
    if(read32(m,sp+4)!=DRAW_BASE+0x9000) {m->fault=1;uc_emu_stop(u);return;}
    DrawPacket *p=NULL;
    for(uint32_t i=0;i<m->packet_count;i++)if(m->packets[i].pointer==payload) {p=m->packets+i;break;}
    if(!p||p->primitive>3||p->draws) {m->fault=1;uc_emu_stop(u);return;}
    m->active_quad=p;
}
static void boundary(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {
    (void)size;Machine *m=opaque;
    if(address==0x45cf99) {
        ++m->attempts;if(reg32(m,UC_X86_REG_EAX))++m->successes;return;
    }
    uint32_t sp=reg32(m,UC_X86_REG_ESP),ret=read32(m,sp);
    uint32_t a=read32(m,sp+4),b=read32(m,sp+8),c=read32(m,sp+12);
    uint32_t length=0,alignment=16;
    if(address==0x4438d0){length=c;alignment=b;}
    else if(address==0x443930)length=b;
    else if(address==0x71deb8)length=a<16?16:a;
    if(length) {
        if(!alignment || (alignment&(alignment-1)) || alignment>0x1000000 || length>0x800000)goto fault;
        uint64_t p=((uint64_t)m->next+alignment-1)&~((uint64_t)alignment-1);
        if(p+length>HEAP+0x1000000)goto fault;
        void *zero=calloc(1,length);if(!zero)goto fault;
        require_uc(uc_mem_write(u,p,zero,length));free(zero);
        m->next=(uint32_t)(p+length);++m->allocations;m->bytes+=length;
        write_reg(m,UC_X86_REG_EAX,(uint32_t)p);
    }
    write_reg(m,UC_X86_REG_ESP,sp+4);write_reg(m,UC_X86_REG_EIP,ret);return;
fault:
    m->fault=1;uc_emu_stop(u);
}
static uint32_t call(Machine *m,uint32_t address,uint32_t owner,int count,const uint32_t *args) {
    uint32_t frame[8]={STOP};if(count<0||count>7)exit(2);
    if(count)memcpy(frame+1,args,(size_t)count*4);
    write_reg(m,UC_X86_REG_ESP,STACK+0x8000);write_reg(m,UC_X86_REG_ECX,owner);
    require_uc(uc_mem_write(m->u,STACK+0x8000,frame,(size_t)(count+1)*4));
    if(getenv("PC_VM_PROBE_TRACE"))fprintf(stderr,"native call %x\n",address);
    m->executed_code_bytes=0;
    // Unicorn2.1.4's timer polls usleep(2us). Avoid that scheduler pressure
    // with a condition wait that wakes only on completion or the same5s limit.
    // The library/source code are unchanged; uc_emu_stop is its own timer API.
    CallDeadline d={.u=m->u};pthread_t watchdog;
    pthread_condattr_t attr;
    if(pthread_mutex_init(&d.mutex,NULL)||pthread_condattr_init(&attr))exit(2);
    clockid_t timer_clock=CLOCK_REALTIME;
#ifdef __ANDROID__
    timer_clock=CLOCK_MONOTONIC;
    if(pthread_condattr_setclock(&attr,timer_clock))exit(2);
#endif
    if(pthread_cond_init(&d.condition,&attr)||clock_gettime(timer_clock,&d.deadline))exit(2);
    pthread_condattr_destroy(&attr);d.deadline.tv_sec+=5;
    if(pthread_create(&watchdog,NULL,wait_deadline,&d))exit(2);
    uc_err result=uc_emu_start(m->u,address,STOP,0,getenv("PC_VM_PROBE_BLOCK_BUDGET")?0:5000000);
    if(pthread_mutex_lock(&d.mutex))exit(2);
    d.completed=1;pthread_cond_signal(&d.condition);pthread_mutex_unlock(&d.mutex);
    if(pthread_join(watchdog,NULL))exit(2);
    pthread_cond_destroy(&d.condition);pthread_mutex_destroy(&d.mutex);
    require_uc(result);
    if(d.timed_out){fprintf(stderr,"Native5s watchdog deadline at%x\n",address);exit(2);}
    if(m->fault||reg32(m,UC_X86_REG_EIP)!=STOP){fprintf(stderr,"Native boundary/budget failure at%x\n",address);exit(2);}
    return reg32(m,UC_X86_REG_EAX);
}
static void map(Machine *m,uint32_t address,uint32_t length) {require_uc(uc_mem_map(m->u,address,length,UC_PROT_ALL));}
static void camera_observer(Machine *m,const float center[3]) {
    // Same explicit origin/identity diagnostic input as the independent
    // Python observer. It is not a measured PC camera or final rasterizer.
    map(m,DRAW_BASE,0x10000);
    uint32_t table=DRAW_BASE+0x100,callback=STOP+0x200,camera=DRAW_BASE+0x1000;
    require_uc(uc_mem_write(m->u,DRAW_BASE,&table,4));
    require_uc(uc_mem_write(m->u,table+4,&callback,4));
    const float identity[]={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
    const float projection[]={.001f,0,0,0,0,.001f,0,0,0,0,.001f,0,0,0,.5f,1};
    const float position[]={center[0],center[1],center[2],1};
    float view[16],inverse[16];memcpy(view,identity,sizeof(view));memcpy(inverse,identity,sizeof(inverse));
    for(unsigned i=0;i<3;i++){view[12+i]=-center[i];inverse[12+i]=center[i];}
    require_uc(uc_mem_write(m->u,camera,position,sizeof(position)));
    require_uc(uc_mem_write(m->u,camera+0x40,view,sizeof(view)));
    for(unsigned offset=0xc0;offset<=0x140;offset+=0x80)
        require_uc(uc_mem_write(m->u,camera+offset,inverse,sizeof(inverse)));
    require_uc(uc_mem_write(m->u,camera+0x180,projection,sizeof(projection)));
    require_uc(uc_mem_write(m->u,DRAW_BASE+0x2000,(uint32_t[]){0x795190,camera},8));
    m->packets=calloc(MAX_PACKETS,sizeof(*m->packets));if(!m->packets)exit(2);
    m->packet_queue=DRAW_BASE;
    uc_hook h;require_uc(uc_hook_add(m->u,&h,UC_HOOK_CODE,(void*)record_draw,m,callback,callback));
}
static void quad_observer(Machine *m) {
    // Original441a40 derives the final billboard basis from the supplied
    // source camera view rotation. No hand-written facing/rotation substitute.
    const uint32_t device=DRAW_BASE+0x3000,buffer=DRAW_BASE+0x4000;
    const uint32_t device_table=DRAW_BASE+0x5000,buffer_table=DRAW_BASE+0x6000;
    require_uc(uc_mem_write(m->u,device,&device_table,4));require_uc(uc_mem_write(m->u,buffer,&buffer_table,4));
    require_uc(uc_mem_write(m->u,device_table+0x144,(uint32_t[]){STOP+0x320},4));
    require_uc(uc_mem_write(m->u,buffer_table+0x30,(uint32_t[]){STOP+0x300},4));
    require_uc(uc_mem_write(m->u,0x6ed6f74,&device,4));require_uc(uc_mem_write(m->u,0x6ed37dc,&buffer,4));
    call(m,0x441a40,0,2,(uint32_t[]){DRAW_BASE+0x1000,DRAW_BASE+0x9010});
    if(getenv("PC_VM_PROBE_MATERIALS")) {
        // Explicit GPU-library handles for the verified33 resource124 images.
        // No pixels are rendered here. Native44fd40 retains source bounds/cache.
        const uint32_t library=DRAW_BASE+0xb000,records=DRAW_BASE+0xb100;
        require_uc(uc_mem_write(m->u,library,&records,4));
        require_uc(uc_mem_write(m->u,library+0xc,(uint16_t[]){33},2));
        require_uc(uc_mem_write(m->u,library+0x10,(uint16_t[]){0xffff},2));
        for(uint32_t i=0;i<33;i++)require_uc(uc_mem_write(m->u,records+i*16,(uint32_t[]){i+1},4));
        require_uc(uc_mem_write(m->u,DRAW_BASE+0x9004,&library,4));
        require_uc(uc_mem_write(m->u,DRAW_BASE+0x900c,(uint16_t[]){5},2));
        require_uc(uc_mem_write(m->u,device_table+0x104,(uint32_t[]){STOP+0x340},4));
        require_uc(uc_mem_write(m->u,device_table+0xe4,(uint32_t[]){STOP+0x360},4));
    }
    const uint32_t hooks[]={0x44d560,0x44d160,STOP+0x300,STOP+0x320,STOP+0x340,STOP+0x360};
    for(unsigned i=0;i<sizeof(hooks)/sizeof(hooks[0]);i++) {
        uc_hook h;require_uc(uc_hook_add(m->u,&h,UC_HOOK_CODE,(void*)quad_device_boundary,m,hooks[i],hooks[i]));
    }
    if(getenv("PC_VM_PROBE_BATCH_QUADS")) {
        if(!getenv("PC_VM_PROBE_MATERIALS"))exit(2);
        map(m,BATCH_BASE,0x40000);
        uc_hook h;require_uc(uc_hook_add(m->u,&h,UC_HOOK_CODE,(void*)batch_quad_dispatch,m,0x442c00,0x442c00));
    }
}
#ifdef PC_VM_SHARED_SCENE
static void final_quad_batch(Machine *m,const uint32_t *order,uint32_t count) {
    if(count>MAX_PACKETS)exit(2);
    uint32_t *payloads=calloc(count?count:1,sizeof(*payloads));if(!payloads)exit(2);
    for(uint32_t i=0;i<count;i++) {
        if(order[i]>=m->packet_count||m->packets[order[i]].primitive>3)exit(2);
        payloads[i]=m->packets[order[i]].pointer;
    }
    // Bounded host-call trampoline only: original442c00/442ad0 and all math,
    // texture/blend/quad code remain untouched. It walks the native queue order
    // with the same frame/payload stack inputs, under ONE5M/5s call budget.
    unsigned char code[]={0x53,0x56,0x8b,0x1d,0,0,0,0,0xbe,0,0,0,0,0x85,0xdb,0x74,0x17,
        0xff,0x36,0x68,0,0,0,0,0xb8,0,0,0,0,0xff,0xd0,0x83,0xc4,8,
        0x83,0xc6,4,0x4b,0x75,0xe9,0x5e,0x5b,0xc3};
    uint32_t count_address=BATCH_BASE+0x20,list=BATCH_BASE+0x1000,frame=DRAW_BASE+0x9000,function=0x442c00;
    // Count is DATA, not a rewritten immediate in a cached translated block.
    memcpy(code+4,&count_address,4);memcpy(code+9,&list,4);memcpy(code+20,&frame,4);memcpy(code+25,&function,4);
    require_uc(uc_mem_write(m->u,count_address,&count,4));
    require_uc(uc_mem_write(m->u,BATCH_BASE+0x100,code,sizeof(code)));
    if(count)require_uc(uc_mem_write(m->u,list,payloads,(size_t)count*4));free(payloads);
    require_uc(uc_mem_write(m->u,0x8a5b64,(uint32_t[]){0},4));
    call(m,BATCH_BASE+0x100,0,0,NULL);m->active_quad=NULL;
    for(uint32_t i=0;i<count;i++) {
        DrawPacket *p=m->packets+order[i];
        if(p->matrix_writes!=1||p->draws!=1||p->locks!=1)exit(2);
    }
}
#endif
static void final_quad(Machine *m,DrawPacket *p) {
    if(p->primitive>3)return;
    // Register/stack inputs are the exact original442ad0 dispatch contract.
    // Texture/blend setup is separate and not represented by this leaf probe.
    const uint32_t payload=DRAW_BASE+0xa000,frame=DRAW_BASE+0x9000;
    require_uc(uc_mem_write(m->u,payload,p->raw,p->length));
    require_uc(uc_mem_write(m->u,0x8a5b64,(uint32_t[]){0},4));
    m->active_quad=p;p->matrix_writes=p->draws=p->locks=0;
    if(getenv("PC_VM_PROBE_MATERIALS")) {
        call(m,0x442c00,0,2,(uint32_t[]){frame,payload});
        p->texture_index=m->texture_index;p->blend_operation=m->blend_operation;
        p->blend_source=m->blend_source;p->blend_destination=m->blend_destination;
    } else if(p->primitive==0) {
        write_reg(m,UC_X86_REG_EAX,payload+0x30);write_reg(m,UC_X86_REG_EDX,frame);
        call(m,0x4423b0,0,1,&payload);
    } else if(p->primitive==1) {
        write_reg(m,UC_X86_REG_EAX,payload);write_reg(m,UC_X86_REG_EDI,payload+0x30);
        call(m,0x442400,0,1,&frame);
    } else if(p->primitive==2) {
        write_reg(m,UC_X86_REG_EAX,payload+0x30);write_reg(m,UC_X86_REG_ESI,payload);
        call(m,0x442230,0,0,NULL);
    } else {
        write_reg(m,UC_X86_REG_EAX,frame);write_reg(m,UC_X86_REG_EDI,payload+0x30);
        call(m,0x442480,payload,0,NULL);
    }
    m->active_quad=NULL;
    if(p->matrix_writes!=1||p->draws!=1||p->locks!=1){fprintf(stderr,"Original final quad boundary\n");exit(2);}
}
static void initialize(Machine *m,const unsigned char *exe,size_t n) {
    memset(m,0,sizeof(*m));m->next=HEAP+0x100000;
    const unsigned char *tail_bytes=NULL;size_t tail_size=0;
    if(n==5779520 && !memcmp(exe,"PCVMEX01",8)) {
        const uint32_t expected[]={159920128,0x500000,0x9800000,536576};
        const unsigned char original_sha[]={0x30,0xd3,0x3b,0x44,0x87,0x6b,0x84,0xa8,
            0xe8,0x75,0x70,0x87,0x3a,0x86,0xde,0x88,0xc6,0x5d,0x24,0x91,0xc7,0xe1,0xcd,0xee,
            0xb5,0x88,0x3d,0xc4,0xb1,0x2f,0xee,0xfb};
        if(memcmp(exe+8,expected,16)||memcmp(exe+24,original_sha,32)||memcmp(exe+56,(unsigned char[8]){0},8)) {
            fprintf(stderr,"Native source fragment boundary\n");exit(2);
        }
        tail_bytes=exe+64+0x500000;tail_size=536576;exe+=64;n=159920128;
    } else if(n==159920128) {tail_bytes=exe+0x9800000;tail_size=n-0x9800000;}
    if(!tail_bytes || n!=159920128 || memcmp(exe+0x49d0a4,(uint32_t[]){0x73bc80,0x73bd20},8) ||
       memcmp(exe+0x4a5b68,(uint32_t[]){625,0,0x9908b0df},12)){fprintf(stderr,"Unverified source layout\n");exit(2);}
    require_uc(uc_open(UC_ARCH_X86,UC_MODE_32,&m->u));
    map(m,0,4096);map(m,0x400000,0x500000);require_uc(uc_mem_write(m->u,0x400000,exe,0x500000));
    map(m,0x6ed0000,0x10000);map(m,0x9c00000,0x100000);
    // The verified EXE ends inside this mapped segment. Python's original
    // slice is shorter than the mapping; never read past the supplied file.
    require_uc(uc_mem_write(m->u,0x9c00000,tail_bytes,tail_size));
    map(m,HEAP,0x1000000);map(m,STACK,0x10000);map(m,STOP,4096);
    const uint32_t iat[]={0x74e008,0x74e21c,0x74e158},values[]={2,0,0},argc[]={3,1,1};
    const char *names[]={"RegOpenKeyA","GetModuleHandleA","IsProcessorFeaturePresent"};
    for(int i=0;i<3;i++) {
        uint32_t offset;memcpy(&offset,exe+iat[i]-0x400000,4);
        const unsigned char *import_name=NULL;size_t length=strlen(names[i])+1;
        uint64_t start=(uint64_t)offset+2,end=start+length;
        if(end<=0x500000)import_name=exe+start;
        else if(start>=0x9800000 && end<=0x9800000+tail_size)import_name=tail_bytes+start-0x9800000;
        if(!import_name||memcmp(import_name,names[i],length)){fprintf(stderr,"Native IAT changed/boundary\n");exit(2);}
        uint32_t adapter=STOP+0x100+(uint32_t)i*16;
        unsigned char code[]={0xb8,0,0,0,0,0xc2,0,0};memcpy(code+1,values+i,4);code[6]=(unsigned char)(argc[i]*4);
        require_uc(uc_mem_write(m->u,adapter,code,sizeof(code)));require_uc(uc_mem_write(m->u,iat[i],&adapter,4));
    }
    const uint32_t hooks[]={0x4438d0,0x443930,0x443950,0x71deb8,0x707dff,0x45cf99};
    for(unsigned i=0;i<sizeof(hooks)/sizeof(hooks[0]);i++) {
        uc_hook h;require_uc(uc_hook_add(m->u,&h,UC_HOOK_CODE,(void*)boundary,m,hooks[i],hooks[i]));
    }
    if(getenv("PC_VM_PROBE_BLOCK_BUDGET")) {
        uc_hook budget;require_uc(uc_hook_add(m->u,&budget,UC_HOOK_BLOCK,(void*)block_budget,m,1,0));
    }
    const unsigned char reset[]={0xdb,0xe3,0xc3};require_uc(uc_mem_write(m->u,STOP+0x180,reset,sizeof(reset)));
    call(m,STOP+0x180,0,0,NULL);call(m,0x707075,0,1,(uint32_t[]){1});
    if(reg32(m,UC_X86_REG_FPCW)!=0x23f){fprintf(stderr,"Native CRT x87 state differs\n");exit(2);}
}
static unsigned char *read_file(const char *path,size_t *n) {
    FILE *f=fopen(path,"rb");if(!f){perror(path);exit(2);}
    if(fseek(f,0,SEEK_END)||(*n=(size_t)ftell(f))>160000000||fseek(f,0,SEEK_SET)){fprintf(stderr,"File size boundary\n");exit(2);}
    unsigned char *b=malloc(*n);if(!b||fread(b,1,*n,f)!=*n){fprintf(stderr,"Read failure\n");exit(2);}fclose(f);return b;
}
static double now(void) {struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return t.tv_sec+t.tv_nsec/1e9;}
int main(int argc,char **argv) {
    if(argc<4){fprintf(stderr,"probe VERIFIED_EXE KSEF DT...\n");return 2;}
    size_t en,rn;unsigned char *exe=read_file(argv[1],&en),*raw=read_file(argv[2],&rn);Machine m;
    if(rn<20||rn>0xe0000||memcmp(raw,"KSEF0131",8)){fprintf(stderr,"KSEF boundary\n");return 2;}
    double start=now();initialize(&m,exe,en);free(exe);
    float center[3]={0,0,0};uint32_t start_matrix=HEAP+0x1014;
    const char *seff=getenv("PC_VM_PROBE_SEFF_PATH");
    if(seff) {
        if(!getenv("PC_VM_PROBE_CAMERA")){fprintf(stderr,"Source SEFF requires pre-load scene camera binding\n");return 2;}
        size_t sn;unsigned char *row=read_file(seff,&sn);
        uint16_t effect;float values[4];
        if(sn!=18){fprintf(stderr,"Original18-byte SEFF row boundary\n");return 2;}
        memcpy(&effect,row,2);memcpy(values,row+2,16);
        if(effect>=244){fprintf(stderr,"Source effect table boundary\n");return 2;}
        for(unsigned i=0;i<4;i++)if(!isfinite(values[i]))return 2;
        memcpy(center,values,12);require_uc(uc_mem_write(m.u,STOP+0x500,row+2,16));free(row);
        start_matrix=STOP+0x600;call(&m,0x413a80,0,2,(uint32_t[]){start_matrix,STOP+0x500});
    }
    require_uc(uc_mem_write(m.u,HEAP+0x1000,raw,rn));free(raw);
    call(&m,0x73bc80,0,0,NULL);call(&m,0x73bd20,0,0,NULL);
    if(call(&m,0x45b710,0,3,(uint32_t[]){0,0,0x400000})!=1){fprintf(stderr,"Native particle arena\n");return 2;}
    call(&m,0x457c90,HEAP,0,NULL);
    if(getenv("PC_VM_PROBE_CAMERA")) {
        camera_observer(&m,center);call(&m,0x457bd0,HEAP,2,(uint32_t[]){1,DRAW_BASE+0x2000});
        if(getenv("PC_VM_PROBE_QUADS"))quad_observer(&m);
    }
    if(call(&m,0x457dd0,HEAP,3,(uint32_t[]){HEAP+0x1014,0,0})!=HEAP+0x1000+rn){fprintf(stderr,"Native loader boundary\n");return 2;}
    call(&m,0x457900,HEAP,0,NULL);call(&m,0x457880,HEAP,2,(uint32_t[]){1,start_matrix});
    printf("{\"initialization_ms\":%.6f,\"frames\":[",(now()-start)*1000);
    for(int i=3;i<argc;i++) {
        char *end;float dt=strtof(argv[i],&end);if(*end||!isfinite(dt)||dt<=0||dt>30)return 2;
        uint32_t bits;memcpy(&bits,&dt,4);double tick=now();call(&m,0x457a20,HEAP,1,&bits);tick=now()-tick;
        unsigned char root[0xb0];require_uc(uc_mem_read(m.u,HEAP,root,sizeof(root)));
        printf("%s{\"input_dt\":%.10g,\"native_update_ms\":%.6f,\"root_hex\":\"",i==3?"":",",dt,tick*1000);
        for(unsigned j=0;j<sizeof(root);j++)printf("%02x",root[j]);
        printf("\",\"source_emission_attempts\":%u,\"source_particle_allocations\":%u,\"memory_allocation_count\":%u,\"memory_allocation_bytes\":%u",m.attempts,m.successes,m.allocations,m.bytes);
        if(m.packets) {
            m.packet_count=0;double draw_start=now();
            call(&m,0x457b00,HEAP,2,(uint32_t[]){DRAW_BASE,DRAW_BASE+0x1000});
            printf(",\"native_draw_ms\":%.6f,\"packets\":[",(now()-draw_start)*1000);
            for(uint32_t j=0;j<m.packet_count;j++) {
                DrawPacket *p=m.packets+j;
                printf("%s{\"primitive\":%u,\"pointer\":\"0x%x\",\"depth_bits\":\"0x%x\",\"flags\":%u,\"prefix_bytes\":%u,\"raw_hex\":\"",
                    j?",":"",p->primitive,p->pointer,p->depth_bits,p->flags,p->length);
                for(uint32_t k=0;k<p->length;k++)printf("%02x",p->raw[k]);
                printf("\"");
                if(getenv("PC_VM_PROBE_QUADS") && p->primitive<=3) {
                    final_quad(&m,p);printf(",\"quad_vb_hex\":\"");
                    for(unsigned k=0;k<96;k++)printf("%02x",p->vertices[k]);
                    printf("\",\"quad_matrix_hex\":\"");
                    for(unsigned k=0;k<64;k++)printf("%02x",p->matrix[k]);
                    printf("\"");
                }
                printf("}");
            }
            printf("]");
        }
        printf("}");
    }
    printf("]}\n");
    const char *dump=getenv("PC_VM_PROBE_HEAP_PATH");
    if(dump) {
        unsigned char *heap=malloc(0x1000000);if(!heap)return 2;
        require_uc(uc_mem_read(m.u,HEAP,heap,0x1000000));FILE *f=fopen(dump,"wb");
        if(!f||fwrite(heap,1,0x1000000,f)!=0x1000000){fprintf(stderr,"Heap evidence write failure\n");return 2;}
        fclose(f);free(heap);
    }
    const char *rng=getenv("PC_VM_PROBE_VISUAL_RNG_PATH");
    if(rng) {
        unsigned char state[2504];require_uc(uc_mem_read(m.u,0x8a5b68,state,sizeof(state)));
        FILE *f=fopen(rng,"wb");if(!f||fwrite(state,1,sizeof(state),f)!=sizeof(state))return 2;fclose(f);
    }
    free(m.packets);require_uc(uc_close(m.u));return 0;
}
