/* Private-copy observer. Original Present passes through untouched; only
 * independent GPU copies are read. No source matrices, shaders, textures,
 * camera, game authority/RNG or resources are replaced. PC/Wine parity still
 * requires separate validation. Build is pinned to the supplied fixed-base EXE. */
typedef void *H;typedef unsigned long U32;typedef unsigned short U16;typedef long HR;
#define CALL __stdcall
#define API __declspec(dllimport)
void* memset(void*dest,int value,unsigned int n){volatile unsigned char*p=(volatile unsigned char*)dest;for(unsigned int i=0;i<n;i++)p[i]=(unsigned char)value;return dest;}
API H CALL CreateFileA(const char*,U32,U32,H,U32,U32,H);API int CALL WriteFile(H,const void*,U32,U32*,H);
API int CALL CloseHandle(H);API U32 CALL GetFileAttributesA(const char*);API int CALL DeleteFileA(const char*);
API H CALL CreateThread(H,U32,U32(CALL*)(void*),void*,U32,U32*);
API H CALL GetModuleHandleA(const char*);API void CALL Sleep(U32);
typedef struct{void*base,*allocation;U32 allocationProtection,size,state,protection,type;} Memory;
API U32 CALL VirtualQuery(const void*,Memory*,U32);
static H logFile;static U32 written,frames;static void*methods[119];
static H drawFile;static U32 drawCount;
typedef struct {U32 format,type,usage,pool,multisample,quality,width,height;} Desc;
typedef struct {int pitch;void *bits;} Locked;
static H textureFile;static void*textureSeen[64];static U32 textureCount,textureLimit=256;
/* Optional one-frame managed texture observation. READONLY LockRect preserves
 * source pixels. Failures remain records, and repeated pointers are identified
 * within this frame only. Never substitute a source texture or sampler. */
static void texturePixels(void*texture,U32 stage,Desc*desc){
 if(!textureFile||textureCount>=64||desc->pool!=1||desc->width==0||desc->height==0||desc->width>textureLimit||desc->height>textureLimit||(desc->format!=21&&desc->format!=22))return;
 for(U32 i=0;i<textureCount;i++)if(textureSeen[i]==texture)return;
 textureSeen[textureCount++]=texture;void**v=*(void***)texture;Locked lock={0};
 HR hr=((HR(CALL*)(void*,U32,Locked*,void*,U32))v[19])(texture,0,&lock,0,0x10);
 U32 bytes=hr>=0&&lock.bits&&lock.pitch>=(int)(desc->width*4)?desc->width*desc->height*4:0;
 U32 row[12]={0x31584554,drawCount-1,stage,(U32)texture,desc->format,desc->pool,desc->width,desc->height,(U32)hr,(U32)lock.pitch,bytes,0};
 WriteFile(textureFile,row,sizeof(row),&written,0);
 if(bytes)for(U32 y=0;y<desc->height;y++)WriteFile(textureFile,(char*)lock.bits+y*lock.pitch,desc->width*4,&written,0);
 if(hr>=0)((HR(CALL*)(void*,U32))v[20])(texture,0);
}
typedef HR(CALL *Present)(void*,void*,void*,H,void*);static Present realPresent;
typedef HR(CALL *DrawIndexed)(void*,U32,int,U32,U32,U32,U32);static DrawIndexed realDrawIndexed;
typedef struct{U32 format,type,usage,pool,size,fvf;}BufferDesc;
/* Version2 appends read-only D3D9 draw state and active shader bytecode.
 * Keys are retained verbatim; decoding must distinguish FOGENABLE28 from
 * ALPHABLENDENABLE27. HRESULTs are evidence, never treated as defaults. */
static void drawState(void*d){
 U32 state[256]={0x32545344,0,0,0},at=4;
 const U32 renderKeys[]={7,8,9,14,15,16,19,20,22,23,24,25,26,27,28,34,35,36,48,52,53,54,55,56,60,137,168,171,174,175,176,178,194};
 for(U32 i=0;i<sizeof(renderKeys)/sizeof(renderKeys[0]);i++){
  state[at++]=renderKeys[i];U32 value=0;HR hr=((HR(CALL*)(void*,U32,U32*))methods[58])(d,renderKeys[i],&value);state[at++]=hr;state[at++]=value;
 }state[1]=sizeof(renderKeys)/sizeof(renderKeys[0]);
 const U32 stageKeys[]={1,2,3,4,5,6,11,24,26,32};
 for(U32 stage=0;stage<2;stage++)for(U32 i=0;i<sizeof(stageKeys)/sizeof(stageKeys[0]);i++){
  state[at++]=stage;state[at++]=stageKeys[i];U32 value=0;HR hr=((HR(CALL*)(void*,U32,U32,U32*))methods[66])(d,stage,stageKeys[i],&value);state[at++]=hr;state[at++]=value;
 }state[2]=2*sizeof(stageKeys)/sizeof(stageKeys[0]);
 const U32 samplerKeys[]={1,2,5,6,7};
 for(U32 stage=0;stage<2;stage++)for(U32 i=0;i<sizeof(samplerKeys)/sizeof(samplerKeys[0]);i++){
  state[at++]=stage;state[at++]=samplerKeys[i];U32 value=0;HR hr=((HR(CALL*)(void*,U32,U32,U32*))methods[68])(d,stage,samplerKeys[i],&value);state[at++]=hr;state[at++]=value;
 }state[3]=2*sizeof(samplerKeys)/sizeof(samplerKeys[0]);
 state[at++]=0x32585444;state[at++]=2;
 for(U32 stage=0;stage<2;stage++){
  void*texture=0;Desc desc={0};
  state[at++]=stage;HR hr=((HR(CALL*)(void*,U32,void**))methods[64])(d,stage,&texture);
  state[at++]=hr;U32 type=0;HR descHr=(HR)0xffffffff;
  if(hr>=0&&texture){void**v=*(void***)texture;type=((U32(CALL*)(void*))v[10])(texture);
   if(type==3)descHr=((HR(CALL*)(void*,U32,void*))v[17])(texture,0,&desc);
   if(type==3&&descHr>=0)texturePixels(texture,stage,&desc);
   ((U32(CALL*)(void*))v[2])(texture);
  }
  state[at++]=type;state[at++]=descHr;
  const U32*values=(const U32*)&desc;for(U32 j=0;j<8;j++)state[at++]=values[j];
 }
 WriteFile(drawFile,state,sizeof(state),&written,0);
 U32 shaderInfo[4]={0x32565344,0xffffffff,0xffffffff,0},shaderBytes[512]={0};void*shader=0;
 HR hr=((HR(CALL*)(void*,void**))methods[93])(d,&shader);shaderInfo[1]=hr;
 if(hr>=0&&shader){void**v=*(void***)shader;U32 bytes=0;
  hr=((HR(CALL*)(void*,void*,U32*))v[4])(shader,0,&bytes);shaderInfo[2]=hr;shaderInfo[3]=bytes;
  if(hr>=0&&bytes<=sizeof(shaderBytes))shaderInfo[2]=((HR(CALL*)(void*,void*,U32*))v[4])(shader,shaderBytes,&bytes);
  ((U32(CALL*)(void*))v[2])(shader);
 }
 WriteFile(drawFile,shaderInfo,sizeof(shaderInfo),&written,0);WriteFile(drawFile,shaderBytes,sizeof(shaderBytes),&written,0);
}
/* Ground texture/color attributes live in stream1, separately from stream0
 * position/normal. The prefix is read only and follows the same base/minimum. */
static void drawStream1(void*d,int base,U32 minimum){
 U32 row[7]={0x33564244,1,0,0,0xffffffff,0xffffffff,0};BufferDesc desc={0};unsigned char sample[512]={0};void*vb=0;
 HR hr=((HR(CALL*)(void*,U32,void**,U32*,U32*))methods[101])(d,1,&vb,row+2,row+3);row[4]=hr;
 if(hr>=0&&vb){void**v=*(void***)vb;hr=((HR(CALL*)(void*,BufferDesc*))v[13])(vb,&desc);
  U32 first=minimum+(U32)base,offset=row[2],stride=row[3];
  if(base<0&&(U32)(-base)>minimum)hr=(HR)0x80070057;
  if(stride&&first>(0xffffffff-offset)/stride)hr=(HR)0x80070057;else offset+=first*stride;
  if(hr>=0&&offset<desc.size){U32 bytes=desc.size-offset;if(bytes>512)bytes=512;void*data=0;
   hr=((HR(CALL*)(void*,U32,U32,void**,U32))v[11])(vb,offset,bytes,&data,0x10);row[5]=hr;
   if(hr>=0&&data){row[6]=bytes;for(U32 i=0;i<bytes;i++)sample[i]=((unsigned char*)data)[i];((HR(CALL*)(void*))v[12])(vb);}
  }((U32(CALL*)(void*))v[2])(vb);
 }
 WriteFile(drawFile,row,sizeof(row),&written,0);WriteFile(drawFile,&desc,sizeof(desc),&written,0);WriteFile(drawFile,sample,sizeof(sample),&written,0);
}
/* Read only original fixed-function transforms and shader constants for a
 * requested frame. Optional READONLY VB lock failures are retained. No bound
 * stream, constants, sampler, texture or original draw arguments are changed. */
static HR CALL drawIndexed(void*d,U32 type,int base,U32 minimum,U32 vertices,U32 start,U32 primitives){
 if(drawFile&&drawCount<2048){
  U32 row[16]={0x57415244,drawCount++,type,(U32)base,minimum,vertices,start,primitives};static float constants[256*4],transforms[48];
  memset(constants,0,sizeof(constants));memset(transforms,0,sizeof(transforms));
  row[8]=((HR(CALL*)(void*,U32,float*,U32))methods[95])(d,0,constants,256);
  for(int t=0;t<3;t++)row[9+t]=((HR(CALL*)(void*,U32,float*))methods[45])(d,t==0?256:t+1,transforms+t*16);
  void*vb=0;U32 offset=0,stride=0;BufferDesc desc={0};void*data=0;unsigned char sample[512]={0};
  HR stream=((HR(CALL*)(void*,U32,void**,U32*,U32*))methods[101])(d,0,&vb,&offset,&stride);row[12]=stride;row[13]=offset;row[14]=stream;row[15]=0xffffffff;
  if(stream>=0&&vb){void**v=*(void***)vb;HR hr=((HR(CALL*)(void*,BufferDesc*))v[13])(vb,&desc);
   U32 first=minimum+(U32)base,readOffset=offset;
   if(base<0&&(U32)(-base)>minimum)hr=(HR)0x80070057;
   if(stride&&first>(0xffffffff-offset)/stride)hr=(HR)0x80070057;else readOffset+=first*stride;
   if(hr>=0&&readOffset<desc.size){U32 bytes=desc.size-readOffset;if(bytes>512)bytes=512;
    hr=((HR(CALL*)(void*,U32,U32,void**,U32))v[11])(vb,readOffset,bytes,&data,0x10);row[15]=hr;
    if(hr>=0&&data){for(U32 i=0;i<bytes;i++)sample[i]=((unsigned char*)data)[i];((HR(CALL*)(void*))v[12])(vb);}
   }((U32(CALL*)(void*))v[2])(vb);
  }
  WriteFile(drawFile,row,sizeof(row),&written,0);WriteFile(drawFile,constants,sizeof(constants),&written,0);WriteFile(drawFile,transforms,sizeof(transforms),&written,0);WriteFile(drawFile,&desc,sizeof(desc),&written,0);WriteFile(drawFile,sample,sizeof(sample),&written,0);
  drawState(d);
  drawStream1(d,base,minimum);
 }
 return realDrawIndexed(d,type,base,minimum,vertices,start,primitives);
}
static void log(const char*s){U32 n=0;while(s[n])n++;if(!logFile)logFile=CreateFileA("g:\\pc-readback-observer.log",0x40000000,1,0,2,0,0);WriteFile(logFile,s,n,&written,0);}
static void value(U32 n){char s[11]="0x00000000";for(int i=0;i<8;i++){s[9-i]="0123456789abcdef"[n&15];n>>=4;}log(s);log("\r\n");}
static int readable(const void*p,U32 bytes){Memory m;return VirtualQuery(p,&m,sizeof(m))==sizeof(m)&&m.state==0x1000&&!(m.protection&0x101)&&(U32)p>=(U32)m.base&&bytes<=m.size-((U32)p-(U32)m.base);}
static void release(void*p){if(p)((U32(CALL*)(void*))(*(void***)p)[2])(p);}
static void capture(void*device){
 void**v=*(void***)device;void*surface=0,*resolve=0,*copy=0;Desc desc={0};Locked lock;
 HR hr=((HR(CALL*)(void*,U32,U32,U32,void**))v[18])(device,0,0,0,&surface);
 if(hr<0){log("GetBackBuffer ");value(hr);return;}void**s=*(void***)surface;
 hr=((HR(CALL*)(void*,Desc*))s[12])(surface,&desc);
 if(hr<0||desc.width==0||desc.height==0||desc.width>4096||desc.height>4096||(desc.format!=21&&desc.format!=22)){log("Unsupported backbuffer ");value(desc.format);goto done;}
 if(desc.multisample){
  hr=((HR(CALL*)(void*,U32,U32,U32,U32,U32,int,void**,H*))v[28])(device,desc.width,desc.height,desc.format,0,0,0,&resolve,0);
  if(hr<0){log("Create resolve ");value(hr);goto done;}
  hr=((HR(CALL*)(void*,void*,void*,void*,void*,U32))v[34])(device,surface,0,resolve,0,0);
  if(hr<0){log("Resolve copy ");value(hr);goto done;}
 }
 hr=((HR(CALL*)(void*,U32,U32,U32,U32,void**,H*))v[36])(device,desc.width,desc.height,desc.format,2,&copy,0);
 if(hr<0){log("CreateOffscreen ");value(hr);goto done;}
 hr=((HR(CALL*)(void*,void*,void*))v[32])(device,resolve?resolve:surface,copy);
 if(hr<0){log("GetRenderTargetData ");value(hr);goto done;}
 void**c=*(void***)copy;hr=((HR(CALL*)(void*,Locked*,void*,U32))c[13])(copy,&lock,0,0x10);
 if(hr<0){log("LockRect ");value(hr);goto done;}
 if(lock.pitch<(int)(desc.width*4)||!lock.bits){log("Invalid readback pitch\r\n");((HR(CALL*)(void*))c[14])(copy);goto done;}
 H file=CreateFileA("g:\\pc-source-frame.bmp",0x40000000,1,0,2,0,0);
 U32 pixels=desc.width*desc.height*4,total=pixels+54;unsigned char head[14]={0x42,0x4d};
 head[2]=(unsigned char)total;head[3]=(unsigned char)(total>>8);head[4]=(unsigned char)(total>>16);head[5]=(unsigned char)(total>>24);head[10]=54;
 struct{U32 size;int w,h;U16 planes,bits;U32 compression,bytes;int xp,yp;U32 used,important;}info={40,(int)desc.width,-(int)desc.height,1,32,0,pixels,0,0,0,0};
 WriteFile(file,head,14,&written,0);WriteFile(file,&info,40,&written,0);
 for(U32 y=0;y<desc.height;y++)WriteFile(file,(char*)lock.bits+y*lock.pitch,desc.width*4,&written,0);
 CloseHandle(file);((HR(CALL*)(void*))c[14])(copy);log("Original frame readback ");value(frames);value(desc.width);value(desc.height);
done:release(copy);release(resolve);release(surface);
}
static HR CALL present(void*device,void*src,void*dst,H override,void*dirty){
 if(drawFile){U32 footer[4]={0x444e4544,drawCount,frames+1,0};WriteFile(drawFile,footer,sizeof(footer),&written,0);CloseHandle(drawFile);drawFile=0;
  if(textureFile){U32 end[4]={0x54444e45,textureCount,frames+1,0};WriteFile(textureFile,end,sizeof(end),&written,0);CloseHandle(textureFile);textureFile=0;DeleteFileA("g:\\pc-texture.request");DeleteFileA("g:\\pc-texture-512.request");}
  DeleteFileA("g:\\pc-draw.request");log("Source draw observation complete ");value(drawCount);}
 else if(GetFileAttributesA("g:\\pc-draw.request")!=0xffffffff){drawCount=0;drawFile=CreateFileA("g:\\pc-draw-frame.bin",0x40000000,1,0,2,0,0);U32 header[4]={0x31445350,3,frames+1,8540};WriteFile(drawFile,header,sizeof(header),&written,0);
  if(GetFileAttributesA("g:\\pc-texture.request")!=0xffffffff){textureCount=0;
   /* Explicit optional diagnostic bound for the original512-square shared
    * presentation sheet. Default requests retain the historical256 bound.
    * At most64 managed ARGB textures; no resize, write or replacement. */
   textureLimit=GetFileAttributesA("g:\\pc-texture-512.request")!=0xffffffff?512:256;
   textureFile=CreateFileA("g:\\pc-texture-frame.bin",0x40000000,1,0,2,0,0);U32 head[4]={0x31544350,1,frames+1,textureLimit};WriteFile(textureFile,head,sizeof(head),&written,0);}
 }
 if(++frames==30||GetFileAttributesA("g:\\pc-capture.request")!=0xffffffff){capture(device);DeleteFileA("g:\\pc-capture.request");}
 return realPresent(device,src,dst,override,dirty);
}
static U32 CALL observe(void*unused){
 (void)unused;if((U32)GetModuleHandleA(0)!=0x400000){log("Unexpected EXE base\r\n");return 2;}
 const void*slot=(void*)0x6ed6f74;
 for(int attempt=0;attempt<150;attempt++){
  if(readable(slot,4)){void*device=*(void**)slot;if(device&&readable(device,4)){
   void**v=*(void***)device;if(readable(v,sizeof(methods))&&readable(v[17],1)){
    for(int i=0;i<119;i++)methods[i]=v[i];realPresent=(Present)v[17];methods[17]=(void*)present;realDrawIndexed=(DrawIndexed)v[82];methods[82]=(void*)drawIndexed;
    *(void***)device=methods;log("Original device Present observer installed ");value((U32)device);return 0;
   }
  }}Sleep(100);
 }log("Original device not found within15s\r\n");return 3;
}
int CALL DllMain(H module,U32 reason,H reserved){(void)module;(void)reserved;if(reason==1){H thread=CreateThread(0,0,observe,0,0,0);if(thread)CloseHandle(thread);}return 1;}
