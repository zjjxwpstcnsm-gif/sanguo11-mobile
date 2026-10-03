/* Private Wine UI automation adapter. Only the EXE's three imported OS mouse
 * polling functions are adapted, not game code, rules, scenario, RNG, camera
 * or render resources. Input goes through the original game's UI loop.
 * Disable record restores normal OS polling. Button down expires after1s.
 */
typedef void*H;typedef unsigned long U32;typedef unsigned short U16;
#define CALL __stdcall
#define API __declspec(dllimport)
API H CALL CreateFileA(const char*,U32,U32,H,U32,U32,H);API int CALL ReadFile(H,void*,U32,U32*,H);API int CALL WriteFile(H,const void*,U32,U32*,H);
API int CALL CloseHandle(H);API H CALL CreateThread(H,U32,U32(CALL*)(void*),void*,U32,U32*);API void CALL Sleep(U32);
API H CALL GetModuleHandleA(const char*);API H CALL GetProcAddress(H,const char*);API int CALL VirtualProtect(void*,U32,U32,U32*);API U32 CALL GetTickCount(void);
API int CALL EnumWindows(int(CALL*)(H,U32),U32);API int CALL IsWindowVisible(H);API int CALL GetWindowTextW(H,U16*,int);API U32 CALL GetWindowThreadProcessId(H,U32*);API U32 CALL GetCurrentProcessId(void);
typedef struct{int x,y;}Point;typedef struct{int l,t,r,b;}Rect;
API int CALL GetClientRect(H,Rect*);API int CALL ClientToScreen(H,Point*);
typedef int(CALL*Cursor)(Point*);typedef short(CALL*Key)(int);
static Cursor originalCursor;static Key originalAsync,originalKey;static H window;
static volatile U32 enabled,button,downAt,cursorReads,keyReads;static volatile int screenX,screenY;
static int CALL cursor(Point*p){if(enabled&&p){p->x=screenX;p->y=screenY;cursorReads++;return 1;}return originalCursor(p);}
static short state(void){if(button&&GetTickCount()-downAt<1000)return (short)0x8000;return 0;}
static short CALL async(int k){if(enabled&&k==1){keyReads++;return state();}return originalAsync(k);}
static short CALL key(int k){if(enabled&&k==1){keyReads++;return state();}return originalKey(k);}
static int CALL visit(H w,U32 unused){(void)unused;U32 pid=0;GetWindowThreadProcessId(w,&pid);if(pid!=GetCurrentProcessId()||!IsWindowVisible(w))return 1;U16 title[256];int n=GetWindowTextW(w,title,256);for(int i=0;i+1<n;i++)if(title[i]=='1'&&title[i+1]=='1'){window=w;return 0;}return 1;}
static int replace(U32 address,H expected,H target){H*slot=(H*)address;if(*slot!=expected)return 0;U32 before,ignored;if(!VirtualProtect(slot,4,4,&before))return 0;*slot=target;return VirtualProtect(slot,4,before,&ignored);}
static U32 CALL loop(void*unused){
 (void)unused;if((U32)GetModuleHandleA(0)!=0x400000)return 2;
 H user=GetModuleHandleA("user32.dll");originalCursor=(Cursor)GetProcAddress(user,"GetCursorPos");originalAsync=(Key)GetProcAddress(user,"GetAsyncKeyState");originalKey=(Key)GetProcAddress(user,"GetKeyState");
 EnumWindows(visit,0);if(!window||!originalCursor||!originalAsync||!originalKey)return 3;
 if(!replace(0x74e5a8,(H)originalCursor,(H)cursor)||!replace(0x74e56c,(H)originalAsync,(H)async)||!replace(0x74e51c,(H)originalKey,(H)key))return 4;
 U32 prior=0,written;H status=CreateFileA("g:\\pc-mouse-installed.bin",0x40000000,1,0,2,0,0);U32 installed[4]={0x534d4350,GetCurrentProcessId(),0x74e5a8,0x74e56c};WriteFile(status,installed,sizeof(installed),&written,0);CloseHandle(status);
 for(;;){
  U32 record[7],bytes=0;H file=CreateFileA("g:\\pc-mouse-input.bin",0x80000000,3,0,3,0,0);
  int ok=ReadFile(file,record,sizeof(record),&bytes,0);CloseHandle(file);
  if(ok&&bytes==sizeof(record)&&record[0]==0x534d4350&&record[1]!=prior&&record[4]<=1&&record[5]<=1&&record[6]==0){
   Rect r;Point p={(int)record[2],(int)record[3]};
   if(GetClientRect(window,&r)&&p.x>=0&&p.y>=0&&p.x<r.r&&p.y<r.b&&ClientToScreen(window,&p)){
    screenX=p.x;screenY=p.y;downAt=GetTickCount();button=record[4];enabled=record[5];prior=record[1];
    U32 ack[7]={0x534d4350,prior,(U32)screenX,(U32)screenY,enabled,cursorReads,keyReads};
    file=CreateFileA("g:\\pc-mouse-ack.bin",0x40000000,3,0,2,0,0);WriteFile(file,ack,sizeof(ack),&written,0);CloseHandle(file);
   }
  }Sleep(10);
 }return 0;
}
int CALL DllMain(H module,U32 reason,H reserved){(void)module;(void)reserved;if(reason==1){H thread=CreateThread(0,0,loop,0,0,0);if(thread)CloseHandle(thread);}return 1;}
