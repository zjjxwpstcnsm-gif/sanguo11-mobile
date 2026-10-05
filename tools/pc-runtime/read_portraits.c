/* Read-only initialized visual portrait descriptors from the owned PC clone.
 * Source480320 indexes2400 four-byte entries at6fae8b8;48a5b0 chooses face.
 * No rule/RNG/save/face/texture writes; two identical reads required. */
typedef void*H;typedef unsigned long U32;typedef unsigned short U16;
#define CALL __stdcall
#define API __declspec(dllimport)
API H CALL CreateFileA(const char*,U32,U32,H,U32,U32,H);API int CALL WriteFile(H,const void*,U32,U32*,H);API int CALL CloseHandle(H);API void CALL ExitProcess(U32);
API int CALL EnumWindows(int(CALL*)(H,U32),U32);API int CALL IsWindowVisible(H);API int CALL GetWindowTextW(H,U16*,int);API U32 CALL GetWindowThreadProcessId(H,U32*);
API H CALL OpenProcess(U32,int,U32);API int CALL ReadProcessMemory(H,const void*,void*,U32,U32*);
static U32 pid;static unsigned char rows[9600],again[9600];
static int CALL visit(H w,U32 ignored){(void)ignored;if(!IsWindowVisible(w))return 1;U16 s[256];int n=GetWindowTextW(w,s,256);for(int i=0;i+1<n;i++)if(s[i]=='1'&&s[i+1]=='1'){GetWindowThreadProcessId(w,&pid);return 0;}return 1;}
static int read(H p,void*dest){U32 actual=0;return ReadProcessMemory(p,(const void*)0x6fae8b8,dest,9600,&actual)&&actual==9600;}
void entry(void){
 EnumWindows(visit,0);if(!pid)ExitProcess(2);H process=OpenProcess(0x410,0,pid);if(!process)ExitProcess(3);
 if(!read(process,rows)||!read(process,again))ExitProcess(4);
 for(U32 i=0;i<9600;i++)if(rows[i]!=again[i])ExitProcess(5);
 U32 header[8]={0x31525450,1,2400,4,pid,0x6fae8b8,0x480320,0x48a5b0},written;
 H file=CreateFileA("g:\\pc-portrait-descriptors.bin",0x40000000,1,0,2,0,0);
 if(file==(H)-1)ExitProcess(6);
 if(!WriteFile(file,header,sizeof(header),&written,0)||written!=sizeof(header))ExitProcess(7);
 if(!WriteFile(file,rows,sizeof(rows),&written,0)||written!=sizeof(rows))ExitProcess(8);
 CloseHandle(file);CloseHandle(process);ExitProcess(0);
}
