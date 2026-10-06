/* Shared source manager/arena feasibility, not an Android game runtime.
 * The same native boundaries/diagnostic camera are reused without engine or
 * gameplay RNG access. No guessed procedural particles or per-placement VM.
 */
#define main pc_effect_single_probe_main
#define PC_VM_SHARED_SCENE 1
#include "pc_effect_vm_probe.c"
#undef main

static uint32_t file_u32(const unsigned char *p) {uint32_t v;memcpy(&v,p,4);return v;}
static float finite_arg(const char *text) {
    char *end;float value=strtof(text,&end);
    if(*end||!isfinite(value)){fprintf(stderr,"Finite scene input required\n");exit(2);}return value;
}
static uint32_t original_queue_order(Machine *m,uint32_t manager,uint32_t *order) {
    const uint32_t buckets=read32(m,manager+0x224),count=read32(m,manager+0x22c);
    if(count!=4096)exit(2);
    uint32_t emitted=0;unsigned char *seen=calloc(m->packet_count?m->packet_count:1,1);if(!seen)exit(2);
    for(uint32_t bin=count;bin-->0;)for(uint32_t node=read32(m,buckets+bin*4);node;node=read32(m,node+0xc)) {
        uint32_t j;for(j=0;j<m->packet_count;j++)if(m->packets[j].pointer==node)break;
        if(j==m->packet_count||seen[j]||emitted>=m->packet_count)exit(2);
        seen[j]=1;order[emitted++]=j;
    }
    free(seen);if(emitted!=read32(m,manager+0x228))exit(2);return emitted;
}
static void stream_write(const void *data,size_t bytes) {
    if(fwrite(data,1,bytes,stdout)!=bytes)exit(2);
}
static void source_camera_input(Machine *m,uint32_t camera,const float values[51]) {
    for(unsigned j=0;j<51;j++)if(!isfinite(values[j]))exit(2);
    require_uc(uc_mem_write(m->u,camera,(float[]){values[0],values[1],values[2],1},16));
    require_uc(uc_mem_write(m->u,camera+0x40,values+3,64));
    require_uc(uc_mem_write(m->u,camera+0x80,values+35,64));
    require_uc(uc_mem_write(m->u,camera+0xc0,values+19,64));
    require_uc(uc_mem_write(m->u,camera+0x140,values+19,64));
    require_uc(uc_mem_write(m->u,camera+0x180,values+35,64));
}
static void forbid_rule_rng(uc_engine *u,uint64_t address,uint32_t size,void *opaque) {
    (void)u;(void)size;(void)opaque;fprintf(stderr,"Forbidden rule RNG visual call %llx\n",(unsigned long long)address);exit(2);
}
/* Schema2 consumes only immutable admitted burning cells; no gameplay access. */
static uint32_t fire_handles[40000],fire_generations[40000];
static void source_fire_sync(Machine *m,uint32_t manager) {
    unsigned char count_bytes[4];if(fread(count_bytes,1,4,stdin)!=4)exit(2);
    uint32_t count=file_u32(count_bytes);if(count>128)exit(2);
    unsigned char seen[40000]={0};
    for(uint32_t i=0;i<count;i++) {
        unsigned char row[12];if(fread(row,1,12,stdin)!=12)exit(2);
        uint32_t x=file_u32(row),y=file_u32(row+4);float height;memcpy(&height,row+8,4);
        if(x>=200||y>=200||!isfinite(height)||height<0||height>128)exit(2);
        uint32_t key=y*200+x;if(seen[key])exit(2);seen[key]=1;
        if(!fire_handles[key]) {
            float point[4]={(4*x+114)*5,height,(4*y+114+2*(x&1))*5,1};
            require_uc(uc_mem_write(m->u,STOP+0x500,point,16));
            require_uc(uc_mem_write(m->u,manager+0x334,(uint32_t[2]){0,0},8));
            uint32_t handle=call(m,0x414670,manager,2,(uint32_t[]){13,STOP+0x500});
            uint32_t generation;require_uc(uc_reg_read(m->u,UC_X86_REG_EDX,&generation));
            //413d20 copies the callback opaque pair to its return packet and clears
            //manager+334/+338 before414670 returns. EDX retains the copied generation.
            if(!handle||read32(m,manager+0x334)||read32(m,manager+0x338)){
                fprintf(stderr,"Original fire factory boundary: cell=%u,%u handle=%x\n",x,y,handle);exit(2);
            }
            fire_handles[key]=handle;fire_generations[key]=generation;
        }
    }
    for(uint32_t key=0;key<40000;key++)if(fire_handles[key]&&!seen[key]) {
        call(m,0x413470,manager,1,&fire_handles[key]);fire_handles[key]=0;fire_generations[key]=0;
    }
}
int main(int argc,char **argv) {
    if(argc<7){fprintf(stderr,"scene-probe VERIFIED_KERNEL VERIFIED_SCENE SOURCE_CAM_X Y Z DT...\n");return 2;}
    int stream=argc==7&&!strcmp(argv[6],"--stream");
    if(stream) {
        // This persistent CHILD process owns only the visual evaluator. Any
        // source boundary failure exits the child, never an Android UI/core VM.
        setenv("PC_VM_PROBE_SOURCE_QUEUE","1",1);setenv("PC_VM_PROBE_MATERIALS","1",1);
        setenv("PC_VM_PROBE_QUADS","1",1);setenv("PC_VM_PROBE_BATCH_QUADS","1",1);
    }
    size_t kernel_size,pack_size;
    unsigned char *kernel=read_file(argv[1],&kernel_size),*packed=read_file(argv[2],&pack_size);
    int fire_scene=pack_size>=64&&!memcmp(packed,"PCFXSC02",8);
    uint32_t templates=fire_scene?9:8;
    if(pack_size<64||pack_size>0x100000||(!fire_scene&&memcmp(packed,"PCFXSC01",8))||file_u32(packed+8)!=templates||file_u32(packed+12)!=126||
       file_u32(packed+20)!=2280||file_u32(packed+16)+2280u+64u!=pack_size||memcmp(packed+56,(unsigned char[8]){0},8)) {
        fprintf(stderr,"Exact source scene container boundary\n");return 2;
    }
    float center[]={finite_arg(argv[3]),finite_arg(argv[4]),finite_arg(argv[5])};
    float initial_camera[51]={0};
    if(stream) {
        unsigned char initial[216];
        if(fread(initial,1,sizeof(initial),stdin)!=sizeof(initial)||file_u32(initial)||file_u32(initial+4)||file_u32(initial+8))return 2;
        memcpy(initial_camera,initial+12,sizeof(initial_camera));
        for(unsigned j=0;j<51;j++)if(!isfinite(initial_camera[j]))return 2;
        if(memcmp(initial_camera,center,12))return 2;
    }
    Machine m;double begin=now();initialize(&m,kernel,kernel_size);free(kernel);
    uc_hook no_rule_a,no_rule_b;
    require_uc(uc_hook_add(m.u,&no_rule_a,UC_HOOK_CODE,(void*)forbid_rule_rng,NULL,0x472150,0x472150));
    require_uc(uc_hook_add(m.u,&no_rule_b,UC_HOOK_CODE,(void*)forbid_rule_rng,NULL,0x4721d0,0x4721d0));
    call(&m,0x73bc80,0,0,NULL);call(&m,0x73bd20,0,0,NULL);
    int source_queue=getenv("PC_VM_PROBE_SOURCE_QUEUE")!=NULL;
    int materials=getenv("PC_VM_PROBE_MATERIALS")!=NULL;
    int batch_quads=getenv("PC_VM_PROBE_BATCH_QUADS")!=NULL;
    if(materials&&(!source_queue||!getenv("PC_VM_PROBE_QUADS")))return 2;
    if(batch_quads&&!materials)return 2;
    const uint32_t manager=HEAP+0x8000,camera=DRAW_BASE+0x1000;
    uint32_t slots=HEAP+0x9000;
    if(source_queue) {
        call(&m,fire_scene?0x413510:0x45a820,manager,0,NULL);
        if(fire_scene&&(read32(&m,manager+0x33c)!=9||read32(&m,manager+0x344)!=0xffffffffu))exit(2);
        // Original4140df/4140e4/4140f5 inputs:244 roots,4096 depth buckets.
        if(call(&m,0x45a620,manager,3,(uint32_t[]){0,4096,244})!=1)return 2;
        slots=read32(&m,manager+0x2e4);
        // Eight sparse roots still use the original244-slot traversal order.
        require_uc(uc_mem_write(m.u,manager+0x2ec,(uint32_t[]){244},4));
    } else if(call(&m,0x45b710,0,3,(uint32_t[]){0,0,0x400000})!=1)return 2;
    camera_observer(&m,center);
    if(stream)source_camera_input(&m,camera,initial_camera);
    if(getenv("PC_VM_PROBE_QUADS"))quad_observer(&m);
    // Explicit diagnostic manager inputs. Native45a530 advances its clock and
    // updates the sparse244-slot table in original table order. Native45a590
    // copies view*projection(+c0) to+140 and traverses the source draw nodes.
    // Source depth/material queue and final quad writes run when enabled above.
    require_uc(uc_mem_write(m.u,manager+0x2dc,(uint32_t[]){0x795190,camera},8));
    if(!source_queue) {
        require_uc(uc_mem_write(m.u,manager+0x2e4,&slots,4));
        require_uc(uc_mem_write(m.u,manager+0x2e8,(uint32_t[]){244,244},8));
        require_uc(uc_mem_write(m.u,manager+0x220,(uint32_t[]){DRAW_BASE+0x100},4));
    } else {
        uc_hook h;require_uc(uc_hook_add(m.u,&h,UC_HOOK_CODE,(void*)record_draw,&m,0x458650,0x458650));
    }
    m.packet_queue=manager+0x220;
    const unsigned char *p=packed+64,*seff=packed+64+file_u32(packed+16);
    uint32_t data=HEAP+0x10000;
    const uint32_t expected_old[]={8,9,16,17,18,19,20,23},expected_fire[]={8,9,13,16,17,18,19,20,23};
    const uint32_t *expected=fire_scene?expected_fire:expected_old;
    for(uint32_t i=0;i<templates;i++) {
        if(p+44>seff)return 2;
        uint32_t effect=file_u32(p),resource=file_u32(p+4),length=file_u32(p+8),root=HEAP+i*0x100;
        if(effect!=expected[i]||resource!=read32(&m,0x77692c+effect*12+4)||length<20||length>0xe0000||
           length>(size_t)(seff-p-44)||memcmp(p+44,"KSEF0131",8))return 2;
        data=(data+15)&~15u;if((uint64_t)data+length>HEAP+0x100000)return 2;
        require_uc(uc_mem_write(m.u,data,p+44,length));
        call(&m,0x457c90,root,0,NULL);
        call(&m,0x457bd0,root,2,(uint32_t[]){1,manager+0x2dc});
        if(call(&m,0x457dd0,root,3,(uint32_t[]){data+20,0,0})!=data+length)return 2;
        call(&m,0x457900,root,0,NULL);
        call(&m,0x45a280,manager,2,(uint32_t[]){effect,root});
        data+=length;p+=44+length;
    }
    if(p!=seff||memcmp(seff,"SEFF0001",8)||file_u32(seff+8)!=126)return 2;
    for(uint32_t i=0;i<126;i++) {
        const unsigned char *row=seff+12+i*18;uint16_t effect;memcpy(&effect,row,2);
        if(effect>=244)return 2;
        uint32_t root=call(&m,0x45a270,manager,1,(uint32_t[]){effect});
        if(!root)return 2;
        require_uc(uc_mem_write(m.u,STOP+0x500,row+2,16));
        call(&m,0x413a80,0,2,(uint32_t[]){STOP+0x600,STOP+0x500});
        call(&m,0x457880,root,2,(uint32_t[]){1,STOP+0x600});
    }
    if(fire_scene)require_uc(uc_mem_write(m.u,manager+0x314,(uint32_t[]){1},4));
    free(packed);
    if(stream) {
        stream_write(fire_scene?"PCFXRDY2":"PCFXRDY1",8);stream_write((uint32_t[]){templates,126},8);if(fflush(stdout))return 2;
    } else printf("{\"initialization_ms\":%.6f,\"source_queue\":%s,\"templates\":%u,\"placements\":126,\"frames\":[",(now()-begin)*1000,source_queue?"true":"false",templates);
    uint32_t previous_serial=0;
    for(int i=6;stream||i<argc;i++) {
        float dt;uint32_t serial=0,type=1;
        if(stream) {
            unsigned char command[216];size_t got=fread(command,1,sizeof(command),stdin);
            if(!got&&feof(stdin))break;
            if(got!=sizeof(command)){fprintf(stderr,"Incomplete216-byte visual command\n");return 2;}
            type=file_u32(command);serial=file_u32(command+4);memcpy(&dt,command+8,4);
            if(serial<=previous_serial||type<1||type>(fire_scene?4u:3u)||!isfinite(dt)||dt<0||dt>30||(type==1&&dt==0)||((type==2||type==3)&&dt!=0))return 2;
            previous_serial=serial;if(type==3)break;
            float values[51];memcpy(values,command+12,sizeof(values));
            source_camera_input(&m,camera,values);
            if(type==4)source_fire_sync(&m,manager);
            call(&m,0x441a40,0,2,(uint32_t[]){camera,DRAW_BASE+0x9010});
        } else {dt=finite_arg(argv[i]);if(dt<=0||dt>30)return 2;}
        uint32_t bits;memcpy(&bits,&dt,4);double tick=now();
        if(type==1||(type==4&&dt>0))call(&m,0x45a530,manager,2,(uint32_t[]){camera,bits});
        double update_ms=(now()-tick)*1000;m.packet_count=0;tick=now();
        call(&m,0x45a590,manager,3,(uint32_t[]){0,0,camera});
        double draw_ms=(now()-tick)*1000;
        if(source_queue)for(uint32_t j=0;j<m.packet_count;j++)
            require_uc(uc_mem_read(m.u,m.packets[j].pointer,m.packets[j].raw,m.packets[j].length));
        uint32_t *order=NULL,order_count=0;
        if(source_queue) {
            order=calloc(m.packet_count?m.packet_count:1,sizeof(*order));if(!order)return 2;
            order_count=original_queue_order(&m,manager,order);
        }
        tick=now();
        if(batch_quads)final_quad_batch(&m,order,order_count);
        else if(materials)for(uint32_t j=0;j<order_count;j++)final_quad(&m,m.packets+order[j]);
        double geometry_ms=(now()-tick)*1000;
        if(stream) {
            stream_write("PCFXFR01",8);stream_write((uint32_t[]){serial,order_count},8);
            float elapsed;require_uc(uc_mem_read(m.u,manager+0x14,&elapsed,4));
            stream_write((float[]){elapsed,(float)update_ms,(float)draw_ms,(float)geometry_ms},16);
            for(uint32_t j=0;j<order_count;j++) {
                DrawPacket *p=m.packets+order[j];
                stream_write((uint32_t[]){p->primitive,p->texture_index,p->blend_operation,p->blend_source,p->blend_destination,p->depth_bits},24);
                stream_write(p->vertices,96);stream_write(p->matrix,64);
            }
            free(order);if(fflush(stdout))return 2;continue;
        }
        printf("%s{\"input_dt\":%.10g,\"native_update_ms\":%.6f,\"native_draw_ms\":%.6f,\"native_geometry_ms\":%.6f,\"source_emission_attempts\":%u,\"source_particle_allocations\":%u,\"memory_allocation_count\":%u,\"memory_allocation_bytes\":%u,\"packets\":[",
            i==6?"":",",dt,update_ms,draw_ms,geometry_ms,m.attempts,m.successes,m.allocations,m.bytes);
        for(uint32_t j=0;j<m.packet_count;j++) {
            DrawPacket *packet=m.packets+j;
            printf("%s{\"primitive\":%u,\"pointer\":\"0x%x\",\"depth_bits\":\"0x%x\",\"flags\":%u,\"prefix_bytes\":%u,\"raw_hex\":\"",
                j?",":"",packet->primitive,packet->pointer,packet->depth_bits,packet->flags,packet->length);
            for(uint32_t k=0;k<packet->length;k++)printf("%02x",packet->raw[k]);printf("\"");
            if(materials)printf(",\"queue_accepted\":%s",packet->draws==1?"true":"false");
            if(getenv("PC_VM_PROBE_QUADS")&&packet->primitive<=3&&(!materials||packet->draws==1)) {
                if(!materials)final_quad(&m,packet);printf(",\"quad_vb_hex\":\"");
                for(unsigned k=0;k<96;k++)printf("%02x",packet->vertices[k]);printf("\",\"quad_matrix_hex\":\"");
                for(unsigned k=0;k<64;k++)printf("%02x",packet->matrix[k]);printf("\"");
                if(materials)printf(",\"texture_index\":%u,\"blend_operation\":%u,\"blend_source\":%u,\"blend_destination\":%u",
                    packet->texture_index,packet->blend_operation,packet->blend_source,packet->blend_destination);
                if(batch_quads)printf(",\"vertex_offset\":%u,\"lock_flags\":%u,\"draw_first\":%u",
                    packet->vertex_offset,packet->lock_flags,packet->draw_first);
            }
            printf("}");
        }
        printf("]");
        if(source_queue) {
            printf(",\"queue_order\":[");
            for(uint32_t j=0;j<order_count;j++)printf("%s%u",j?",":"",order[j]);
            printf("]");free(order);
        }
        printf("}");
    }
    if(!stream)printf("]}\n");
    const char *heap_path=getenv("PC_VM_PROBE_HEAP_PATH"),*rng_path=getenv("PC_VM_PROBE_VISUAL_RNG_PATH");
    if(heap_path) {
        unsigned char *heap=malloc(0x1000000);if(!heap)return 2;
        require_uc(uc_mem_read(m.u,HEAP,heap,0x1000000));FILE *f=fopen(heap_path,"wb");
        if(!f||fwrite(heap,1,0x1000000,f)!=0x1000000)return 2;fclose(f);free(heap);
    }
    if(rng_path) {
        unsigned char state[2504];require_uc(uc_mem_read(m.u,0x8a5b68,state,sizeof(state)));FILE *f=fopen(rng_path,"wb");
        if(!f||fwrite(state,1,sizeof(state),f)!=sizeof(state))return 2;fclose(f);
    }
    free(m.packets);require_uc(uc_close(m.u));return 0;
}
