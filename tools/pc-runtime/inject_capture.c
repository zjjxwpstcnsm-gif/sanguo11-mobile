/* Win32 helper, private Wine prefix only. Load the readback observer in the
 * owned San11 window process; no game resources, rules or saves are patched. */
typedef void *H;typedef unsigned long U32;typedef unsigned short U16;
#define CALL __stdcall
#define API __declspec(dllimport)
API H CALL CreateFileA(const char*,U32,U32,H,U32,U32,H);
API int CALL WriteFile(H,const void*,U32,U32*,H);
API int CALL CloseHandle(H);API void CALL ExitProcess(U32);
API int CALL EnumWindows(int(CALL*)(H,U32),U32);
API int CALL IsWindowVisible(H);API int CALL GetWindowTextW(H,U16*,int);
API U32 CALL GetWindowThreadProcessId(H,U32*);
API H CALL OpenProcess(U32,int,U32);
API void *CALL VirtualAllocEx(H,void*,U32,U32,U32);
API int CALL WriteProcessMemory(H,void*,const void*,U32,U32*);
API H CALL CreateRemoteThread(H,H,U32,U32(CALL*)(void*),void*,U32,U32*);
API H CALL GetModuleHandleA(const char*);API H CALL GetProcAddress(H,const char*);
API U32 CALL WaitForSingleObject(H,U32);API int CALL GetExitCodeThread(H,U32*);
static H file;static U32 written,pid;
static void put(const char*s){U32 n=0;while(s[n])n++;WriteFile(file,s,n,&written,0);}
static void hex(U32 n){char s[11]="0x00000000";for(int i=0;i<8;i++){s[9-i]="0123456789abcdef"[n&15];n>>=4;}put(s);put("\r\n");}
static int CALL visit(H w,U32 unused){(void)unused;if(!IsWindowVisible(w))return 1;U16 title[256];int n=GetWindowTextW(w,title,256);for(int i=0;i+1<n;i++)if(title[i]=='1'&&title[i+1]=='1'){GetWindowThreadProcessId(w,&pid);return 0;}return 1;}
void entry(void){
#if defined(PC_SAMPLER_OBSERVER)
 file=CreateFileA("g:\\pc-inject-sampler.log",0x40000000,1,0,2,0,0);
#elif defined(PC_FPU_OBSERVER)
 file=CreateFileA("g:\\pc-inject-fpu.log",0x40000000,1,0,2,0,0);
#elif defined(PC_MOUSE_INPUT)
 file=CreateFileA("g:\\pc-inject-mouse.log",0x40000000,1,0,2,0,0);
#else
 file=CreateFileA("g:\\pc-inject-capture.log",0x40000000,1,0,2,0,0);
#endif
 EnumWindows(visit,0);put("Owned San11 pid ");hex(pid);if(!pid)ExitProcess(2);
 H process=OpenProcess(0x43a,0,pid);if(!process){put("OpenProcess failed\r\n");ExitProcess(3);}
#if defined(PC_SAMPLER_OBSERVER)
 const char dll[]="g:\\pc-sampler-observer.dll";
#elif defined(PC_FPU_OBSERVER)
 const char dll[]="g:\\pc-fpu-observer.dll";
#elif defined(PC_MOUSE_INPUT)
 const char dll[]="g:\\pc-mouse-input.dll";
#else
 const char dll[]="g:\\pc-readback-observer.dll";
#endif
 void*remote=VirtualAllocEx(process,0,sizeof(dll),0x3000,4);U32 bytes=0;
 if(!remote||!WriteProcessMemory(process,remote,dll,sizeof(dll),&bytes)||bytes!=sizeof(dll)){put("Observer path allocation failed\r\n");ExitProcess(4);}
 /* Both helper and target are Win32 in this prefix. kernel32 is shared at
  * one module base; no Win64 address is passed into a WOW64 thread. */
 H load=GetProcAddress(GetModuleHandleA("kernel32.dll"),"LoadLibraryA");
 H thread=CreateRemoteThread(process,0,0,(U32(CALL*)(void*))load,remote,0,0);
 if(!thread){put("CreateRemoteThread failed\r\n");ExitProcess(5);}
 U32 wait=WaitForSingleObject(thread,15000),code=0;put("Observer loader wait ");hex(wait);
 if(wait||!GetExitCodeThread(thread,&code)){put("Observer loader incomplete\r\n");ExitProcess(6);}
 put("Observer module ");hex(code);CloseHandle(thread);CloseHandle(process);CloseHandle(file);ExitProcess(code?0:7);
}
