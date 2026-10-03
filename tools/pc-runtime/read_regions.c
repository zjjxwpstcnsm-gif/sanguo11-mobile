/* Read-only original city/province climate records. Fixed source addresses
 * follow41c3a0 ->4839f0 ->490a10 ->47b200 ->490b50 ->47a310.
 * No source authorities, province values, resources or rule RNG are written. */
typedef void*H;typedef unsigned long U32;typedef unsigned short U16;
#define CALL __stdcall
#define API __declspec(dllimport)
API H CALL CreateFileA(const char*,U32,U32,H,U32,U32,H);API int CALL WriteFile(H,const void*,U32,U32*,H);API int CALL CloseHandle(H);API void CALL ExitProcess(U32);
API int CALL EnumWindows(int(CALL*)(H,U32),U32);API int CALL IsWindowVisible(H);API int CALL GetWindowTextW(H,U16*,int);API U32 CALL GetWindowThreadProcessId(H,U32*);
API H CALL OpenProcess(U32,int,U32);API int CALL ReadProcessMemory(H,const void*,void*,U32,U32*);
static U32 pid;static unsigned char cities[42*32],provinces[12*72];
static int CALL visit(H w,U32 ignored){(void)ignored;if(!IsWindowVisible(w))return 1;U16 s[256];int n=GetWindowTextW(w,s,256);for(int i=0;i+1<n;i++)if(s[i]=='1'&&s[i+1]=='1'){GetWindowThreadProcessId(w,&pid);return 0;}return 1;}
static int read(H p,U32 address,void*d,U32 n){U32 actual=0;return ReadProcessMemory(p,(const void*)address,d,n,&actual)&&actual==n;}
void entry(void){
 EnumWindows(visit,0);if(!pid)ExitProcess(2);H process=OpenProcess(0x410,0,pid);if(!process)ExitProcess(3);
 for(U32 i=0;i<42;i++)if(!read(process,0x7201958+0x1d8+i*0x248,cities+i*32,32))ExitProcess(4);
 if(!read(process,0x7201958+0x7984c,provinces,sizeof(provinces)))ExitProcess(5);
 for(U32 i=0;i<42;i++)if(*(U32*)(cities+i*32+0x18)>=12)ExitProcess(6);
 for(U32 i=0;i<12;i++)if(*(U32*)(provinces+i*72+0x2c)>5)ExitProcess(7);
 U32 header[8]={0x4c434350,0x31304d49,42,12,pid,0x7201958,0x248,0x48},written;
 H file=CreateFileA("g:\\pc-climate-snapshot.bin",0x40000000,1,0,2,0,0);
 WriteFile(file,header,sizeof(header),&written,0);WriteFile(file,cities,sizeof(cities),&written,0);WriteFile(file,provinces,sizeof(provinces),&written,0);
 CloseHandle(file);CloseHandle(process);ExitProcess(0);
}
