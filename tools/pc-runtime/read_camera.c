/* Read-only process observation of the supplied original renderer provider.
 * No remote allocation/thread, code execution or game-memory writes. */
typedef void *H;typedef unsigned long U32;typedef unsigned short U16;
#define CALL __stdcall
#define API __declspec(dllimport)
API H CALL CreateFileA(const char*,U32,U32,H,U32,U32,H);API int CALL WriteFile(H,const void*,U32,U32*,H);
API int CALL CloseHandle(H);API void CALL ExitProcess(U32);
API int CALL EnumWindows(int(CALL*)(H,U32),U32);API int CALL IsWindowVisible(H);API int CALL GetWindowTextW(H,U16*,int);
API U32 CALL GetWindowThreadProcessId(H,U32*);API H CALL OpenProcess(U32,int,U32);
API int CALL ReadProcessMemory(H,const void*,void*,U32,U32*);
static H logFile,process;static U32 pid,written;
static void put(const char*s){U32 n=0;while(s[n])n++;WriteFile(logFile,s,n,&written,0);}
static void hex(U32 n){char s[11]="0x00000000";for(int i=0;i<8;i++){s[9-i]="0123456789abcdef"[n&15];n>>=4;}put(s);put("\r\n");}
static int CALL visit(H w,U32 unused){(void)unused;if(!IsWindowVisible(w))return 1;U16 title[256];int n=GetWindowTextW(w,title,256);for(int i=0;i+1<n;i++)if(title[i]=='1'&&title[i+1]=='1'){GetWindowThreadProcessId(w,&pid);return 0;}return 1;}
static void read(U32 address,void*dest,U32 bytes){U32 received=0;if(!ReadProcessMemory(process,(void*)address,dest,bytes,&received)||received!=bytes){put("Read failed at ");hex(address);ExitProcess(4);}}
void entry(void){
 logFile=CreateFileA("g:\\pc-camera-read.log",0x40000000,1,0,2,0,0);EnumWindows(visit,0);
 if(!pid){put("Missing owned game\r\n");ExitProcess(2);}process=OpenProcess(0x410,0,pid);if(!process)ExitProcess(3);
 /* SENV source5a2530 reads this provider and calls accessor0 for camera.
  * Store provider+vtable+accessor bytes first; do not execute unknown code. */
 U32 provider[32],table[8];unsigned char code[128];read(0x32602b0,provider,sizeof(provider));read(provider[0],table,sizeof(table));read(table[0],code,sizeof(code));
 H file=CreateFileA("g:\\pc-renderer-provider.bin",0x40000000,1,0,2,0,0);
 WriteFile(file,provider,sizeof(provider),&written,0);WriteFile(file,table,sizeof(table),&written,0);WriteFile(file,code,sizeof(code),&written,0);CloseHandle(file);
 put("Owned PID/provider vtable/accessor\r\n");hex(pid);hex(provider[0]);hex(table[0]);
 /* Fixed supplied EXE414fd0 = lea eax,[ecx+40]; ret. Validate both
  * pointer and actual loaded instruction bytes before reading its camera. */
 if(provider[0]!=0x779b90||table[0]!=0x414fd0||code[0]!=0x8d||code[1]!=0x41||code[2]!=0x40||code[3]!=0xc3){put("Unverified camera accessor\r\n");ExitProcess(5);}
 unsigned char camera[512];read(0x32602f0,camera,sizeof(camera));
 file=CreateFileA("g:\\pc-camera-snapshot.bin",0x40000000,1,0,2,0,0);
 WriteFile(file,camera,sizeof(camera),&written,0);CloseHandle(file);put("Original read-only camera snapshot512\r\n");
 CloseHandle(process);CloseHandle(logFile);ExitProcess(0);
}
