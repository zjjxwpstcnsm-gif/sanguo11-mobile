/* Normal keyboard/mouse input in the owned private Wine game. No game
 * memory, source resources, combat rules, RNG or saves are edited. */
typedef void *H;typedef unsigned long U32;typedef unsigned short U16;
#define CALL __stdcall
#define API __declspec(dllimport)
API H CALL CreateFileA(const char*,U32,U32,H,U32,U32,H);
API int CALL ReadFile(H,void*,U32,U32*,H);
API int CALL WriteFile(H,const void*,U32,U32*,H);
API int CALL CloseHandle(H);API void CALL ExitProcess(U32);API void CALL Sleep(U32);
API int CALL EnumWindows(int(CALL*)(H,U32),U32);API int CALL IsWindowVisible(H);
API int CALL GetWindowTextW(H,U16*,int);API U32 CALL GetWindowThreadProcessId(H,U32*);
typedef struct{int left,top,right,bottom;} Rect;
typedef struct{int x,y;} Point;
API int CALL GetClientRect(H,Rect*);API int CALL ClientToScreen(H,Point*);
API int CALL SetForegroundWindow(H);API int CALL SetCursorPos(int,int);
API int CALL GetCursorPos(Point*);API H CALL GetForegroundWindow(void);
API U32 CALL SendMessageTimeoutA(H,U32,U32,U32,U32,U32,U32*);
API void CALL mouse_event(U32,U32,U32,U32,U32);API void CALL keybd_event(unsigned char,unsigned char,U32,U32);
static H window,logFile;static U32 pid,written;
static void put(const char*s){U32 n=0;while(s[n])n++;WriteFile(logFile,s,n,&written,0);}
static void hex(U32 n){char s[11]="0x00000000";for(int i=0;i<8;i++){s[9-i]="0123456789abcdef"[n&15];n>>=4;}put(s);put("\r\n");}
static int CALL visit(H w,U32 ignored){(void)ignored;if(!IsWindowVisible(w))return 1;U16 title[256];int n=GetWindowTextW(w,title,256);for(int i=0;i+1<n;i++)if(title[i]=='1'&&title[i+1]=='1'){window=w;GetWindowThreadProcessId(w,&pid);return 0;}return 1;}
static int number(char**p){while(**p==' ')(*p)++;int n=0,d=0;while(**p>='0'&&**p<='9'){n=n*10+*(*p)++-'0';if(++d>5)return -1;}return d?n:-1;}
void entry(void){
 logFile=CreateFileA("g:\\pc-ui-command.log",0x40000000,1,0,2,0,0);
 char command[128]={0};U32 bytes=0;H file=CreateFileA("g:\\pc-ui-command.txt",0x80000000,1,0,3,0,0);
 if(!ReadFile(file,command,127,&bytes,0)||!bytes){put("Missing command\r\n");ExitProcess(2);}CloseHandle(file);
 EnumWindows(visit,0);if(!window||!pid){put("Owned source window missing\r\n");ExitProcess(3);}
 Rect r;if(!GetClientRect(window,&r)){put("Client rect unavailable\r\n");ExitProcess(4);}
 put("Owned source PID/client width/height\r\n");hex(pid);hex(r.right-r.left);hex(r.bottom-r.top);
 if(command[0]=='m'&&command[1]=='e'&&command[2]=='s'&&command[3]=='s'&&command[4]=='a'&&command[5]=='g'&&command[6]=='e'&&command[7]==' '){
  char*p=command+8;int x=number(&p),y=number(&p);if(x<0||y<0||x>=r.right||y>=r.bottom)ExitProcess(5);
  U32 pos=(U32)x|((U32)y<<16),result=0;
  /* Normal window input messages, bounded delivery. Host mouse warping can
   * report success without reaching its requested point under macOS/Wine. */
  if(!SendMessageTimeoutA(window,0x200,0,pos,2,3000,&result)||!SendMessageTimeoutA(window,0x201,1,pos,2,3000,&result)){put("Window input delivery failed\r\n");ExitProcess(10);}
  Sleep(500);if(!SendMessageTimeoutA(window,0x202,0,pos,2,3000,&result))ExitProcess(10);
  put("Standard window mouse input x/y\r\n");hex(x);hex(y);
 }else if(command[0]=='c'&&command[1]=='l'&&command[2]=='i'&&command[3]=='c'&&command[4]=='k'&&command[5]==' '){
  char*p=command+6;int x=number(&p),y=number(&p);if(x<0||y<0||x>=r.right||y>=r.bottom){put("Out of client bounds\r\n");ExitProcess(5);}
  Point point={x,y};if(!ClientToScreen(window,&point)){put("Client conversion failed\r\n");ExitProcess(6);}
  put("Foreground result/target screen/current screen/foreground PID\r\n");hex(SetForegroundWindow(window));Sleep(150);hex(point.x);hex(point.y);hex(SetCursorPos(point.x,point.y));Sleep(150);
  Point actual={0,0};GetCursorPos(&actual);hex(actual.x);hex(actual.y);U32 foregroundPid=0;GetWindowThreadProcessId(GetForegroundWindow(),&foregroundPid);hex(foregroundPid);
  mouse_event(2,0,0,0,0);Sleep(500);mouse_event(4,0,0,0,0);put("Normal mouse click x/y\r\n");hex(x);hex(y);
 }else if(command[0]=='w'&&command[1]=='h'&&command[2]=='e'&&command[3]=='e'&&command[4]=='l'&&command[5]==' '){
  char*p=command+6;int direction=number(&p),x=number(&p),y=number(&p);
  if((direction!=1&&direction!=2)||x<0||y<0||x>=r.right||y>=r.bottom){put("Invalid wheel command\r\n");ExitProcess(9);}
  Point point={x,y};if(!ClientToScreen(window,&point))ExitProcess(6);
  SetForegroundWindow(window);Sleep(150);SetCursorPos(point.x,point.y);Sleep(150);
  mouse_event(0x800,0,0,direction==1?120:(U32)-120,0);put("Normal wheel direction/x/y\r\n");hex(direction);hex(x);hex(y);
 }else if(command[0]=='k'&&command[1]=='e'&&command[2]=='y'&&command[3]==' '){
  char*p=command+4;int k=number(&p);if(k<1||k>254){put("Invalid virtual key\r\n");ExitProcess(7);}
  SetForegroundWindow(window);Sleep(150);keybd_event((unsigned char)k,0,0,0);Sleep(500);keybd_event((unsigned char)k,0,2,0);put("Normal virtual key\r\n");hex(k);
 }else{put("Unsupported command\r\n");ExitProcess(8);}
 CloseHandle(logFile);ExitProcess(0);
}
