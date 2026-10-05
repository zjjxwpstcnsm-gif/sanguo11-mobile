/* One original Present interval; GetSamplerState only, no device setters.
 * Chains existing observers and restores their table on the next Present. */
typedef void* H;typedef unsigned long U32;typedef long HR;
#define CALL __stdcall
#define API __declspec(dllimport)
API H CALL CreateThread(H,U32,U32(CALL*)(void*),void*,U32,U32*);
API int CALL CloseHandle(H);API void CALL Sleep(U32);
API H CALL CreateFileA(const char*,U32,U32,H,U32,U32,H);
API int CALL WriteFile(H,const void*,U32,U32*,H);
typedef struct{void*base,*allocation;U32 allocationProtection,size,state,protection,type;} Memory;
API U32 CALL VirtualQuery(const void*,Memory*,U32);
static void**previous;static void*methods[119];static int armed;static U32 count;static H file;
typedef HR(CALL*Present)(void*,void*,void*,H,void*);static Present original;
typedef HR(CALL*Draw)(void*,U32,int,U32,U32,U32,U32);static Draw originalDraw;
static HR CALL draw(void*d,U32 type,int base,U32 min,U32 vertices,U32 start,U32 primitives){
 if(armed && count<256){
  U32 row[9]={count+1,type,(U32)base,min,vertices,start,primitives,0xffffffff,0xffffffff},written;
  typedef HR(CALL*GetSampler)(void*,U32,U32,U32*);
  ((GetSampler)previous[68])(d,0,11,&row[7]);((GetSampler)previous[68])(d,1,11,&row[8]);
  WriteFile(file,row,sizeof(row),&written,0);count++;
 }
 return originalDraw(d,type,base,min,vertices,start,primitives);
}
static HR CALL present(void*d,void*src,void*dst,H window,void*dirty){
 if(!armed){U32 header[2]={0x31504d53,1},written;file=CreateFileA("g:\\pc-sampler-observation.bin",0x40000000,1,0,2,0,0);WriteFile(file,header,8,&written,0);armed=1;}
 else {*(void***)d=previous;CloseHandle(file);}
 return original(d,src,dst,window,dirty);
}
static int readable(const void*p,U32 size){Memory m;return VirtualQuery(p,&m,sizeof(m))==sizeof(m)&&m.state==0x1000&&!(m.protection&0x101)&&(U32)p>=(U32)m.base&&size<=m.size-((U32)p-(U32)m.base);}
static U32 CALL observe(void*unused){
 (void)unused;const void*slot=(void*)0x6ed6f74;
 for(int attempt=0;attempt<150;attempt++){
  if(readable(slot,4)){void*d=*(void**)slot;if(d&&readable(d,4)){
   void**table=*(void***)d;
   if(readable(table,sizeof(methods))&&readable(table[17],1)&&readable(table[82],1)){
    previous=table;original=(Present)table[17];originalDraw=(Draw)table[82];
    for(int i=0;i<119;i++)methods[i]=table[i];methods[17]=(void*)present;methods[82]=(void*)draw;
    *(void***)d=methods;return 0;
   }
  }}Sleep(100);
 }return 1;
}
int CALL DllMain(H module,U32 reason,H reserved){(void)module;(void)reserved;if(reason==1){H thread=CreateThread(0,0,observe,0,0,0);if(thread)CloseHandle(thread);}return 1;
}
