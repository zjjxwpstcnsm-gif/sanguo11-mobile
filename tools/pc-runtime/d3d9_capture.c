/* Readback-only D3D9 observer for the private PC copy. Passes every call through.
 * Does not access game authority, shaders, materials, transforms or resources.
 * The reference is Wine output, not evidence of native Windows parity.
 */
typedef void *H;typedef unsigned long U32;typedef unsigned short U16;typedef long HR;
#define API __declspec(dllimport)
#define CALL __stdcall
API H CALL LoadLibraryA(const char*);API H CALL GetProcAddress(H,const char*);
API H CALL CreateFileA(const char*,U32,U32,H,U32,U32,H);API int CALL WriteFile(H,const void*,U32,U32*,H);
API int CALL CloseHandle(H);API U32 CALL GetFileAttributesA(const char*);API int CALL DeleteFileA(const char*);
static H logFile;static U32 written,frames;
static void log(const char *s){U32 n=0;while(s[n])n++;if(!logFile)logFile=CreateFileA("g:\\pc-d3d9-capture.log",0x40000000,1,0,2,0,0);WriteFile(logFile,s,n,&written,0);}
static void value(U32 n){char s[11]="0x00000000";for(int i=0;i<8;i++){s[9-i]="0123456789abcdef"[n&15];n>>=4;}log(s);log("\r\n");}
static void *table9[17],*tableDevice[119];
typedef HR(CALL *CreateDeviceFn)(void*,U32,U32,H,U32,void*,void**);
typedef HR(CALL *PresentFn)(void*,void*,void*,H,void*);
static CreateDeviceFn realCreate;static PresentFn realPresent;
typedef struct {U32 format,type,usage,pool,multisample,quality,width,height;} Desc;
typedef struct {int pitch;void *bits;} Locked;
static void capture(void *device){
    void **v=*(void***)device,*surface=0,*copy=0;Desc desc;Locked lock;
    HR hr=((HR(CALL*)(void*,U32,U32,U32,void**))v[18])(device,0,0,0,&surface);
    if(hr<0){log("GetBackBuffer ");value(hr);return;}
    void **s=*(void***)surface;
    hr=((HR(CALL*)(void*,Desc*))s[12])(surface,&desc);
    if(hr<0||desc.width>4096||desc.height>4096||(desc.format!=21&&desc.format!=22)){log("Unsupported backbuffer ");value(desc.format);goto done;}
    hr=((HR(CALL*)(void*,U32,U32,U32,U32,void**,H*))v[36])(device,desc.width,desc.height,desc.format,2,&copy,0);
    if(hr<0){log("CreateOffscreen ");value(hr);goto done;}
    hr=((HR(CALL*)(void*,void*,void*))v[32])(device,surface,copy);
    if(hr<0){log("GetRenderTargetData ");value(hr);goto done;}
    void **c=*(void***)copy;
    hr=((HR(CALL*)(void*,Locked*,void*,U32))c[13])(copy,&lock,0,0x10);
    if(hr<0){log("LockRect ");value(hr);goto done;}
    H file=CreateFileA("g:\\pc-d3d9-frame.bmp",0x40000000,1,0,2,0,0);
    U32 pixels=desc.width*desc.height*4,total=pixels+54;unsigned char head[14]={0x42,0x4d};
    head[2]=(unsigned char)total;head[3]=(unsigned char)(total>>8);head[4]=(unsigned char)(total>>16);head[5]=(unsigned char)(total>>24);head[10]=54;
    struct {U32 size;int w,h;U16 planes,bits;U32 compression,bytes;int xp,yp;U32 used,important;} info={40,(int)desc.width,-(int)desc.height,1,32,0,pixels,0,0,0,0};
    WriteFile(file,head,14,&written,0);WriteFile(file,&info,40,&written,0);
    for(U32 y=0;y<desc.height;y++)WriteFile(file,(char*)lock.bits+y*lock.pitch,desc.width*4,&written,0);
    CloseHandle(file);((HR(CALL*)(void*))c[14])(copy);log("Frame readback ");value(desc.width);value(desc.height);
done:
    if(copy)((U32(CALL*)(void*))(*(void***)copy)[2])(copy);
    if(surface)((U32(CALL*)(void*))s[2])(surface);
}
static HR CALL present(void *device,void *src,void *dst,H override,void *dirty){
    if(++frames==240||GetFileAttributesA("g:\\pc-capture.request")!=0xffffffff){capture(device);DeleteFileA("g:\\pc-capture.request");}
    return realPresent(device,src,dst,override,dirty);
}
static HR CALL create(void *self,U32 adapter,U32 type,H focus,U32 flags,void *params,void **device){
    HR result=realCreate(self,adapter,type,focus,flags,params,device);log("CreateDevice ");value(result);
    if(result>=0){void **v=*(void***)*device;for(int i=0;i<119;i++)tableDevice[i]=v[i];realPresent=(PresentFn)v[17];tableDevice[17]=(void*)present;*(void***)*device=tableDevice;}
    return result;
}
__declspec(dllexport) void *CALL Direct3DCreate9(U32 sdk){
    H dll=LoadLibraryA("g:\\pc-real-d3d9.dll");log("Load real D3D9 ");value((U32)dll);
    void *(CALL *fn)(U32)=(void *(CALL*)(U32))GetProcAddress(dll,"Direct3DCreate9");if(!fn)return 0;
    void *object=fn(sdk);if(object){void **v=*(void***)object;for(int i=0;i<17;i++)table9[i]=v[i];realCreate=(CreateDeviceFn)v[16];table9[16]=(void*)create;*(void***)object=table9;}
    return object;
}
