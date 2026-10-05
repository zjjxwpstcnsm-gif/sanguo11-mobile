/* GDI capture inside an isolated Wine prefix. No host screen or game memory access.
 * Build without CRT using build_capture.py. Outputs belong to the game COPY.
 * A successful BMP write does not establish that D3D pixels are captured.
 */
typedef void *H;
typedef unsigned long U32;
typedef unsigned short U16;
typedef unsigned long long UP;
typedef int BOOL;
typedef struct {int x,y,w,h;} RECT;
typedef struct {U32 size;int w,h;U16 planes,bits;U32 compression,bytes;int xp,yp;U32 used,important;} BI;
#define API __declspec(dllimport)
API H CreateFileA(const char*,U32,U32,H,U32,U32,H);
API BOOL WriteFile(H,const void*,U32,U32*,H);
API BOOL CloseHandle(H);
API void ExitProcess(U32);
API U32 GetLastError(void);
API int WideCharToMultiByte(U32,U32,const U16*,int,char*,int,const char*,BOOL*);
API BOOL EnumWindows(BOOL (*)(H,UP),UP);
API BOOL IsWindowVisible(H);
API int GetWindowTextW(H,U16*,int);
API BOOL GetClientRect(H,RECT*);
API H GetDC(H);
API int ReleaseDC(H,H);
API BOOL PrintWindow(H,H,U32);
API H CreateCompatibleDC(H);
API H CreateDIBSection(H,const BI*,U32,void**,H,U32);
API H SelectObject(H,H);
API BOOL BitBlt(H,int,int,int,int,H,int,int,U32);
API BOOL DeleteDC(H);
API BOOL DeleteObject(H);
static H logFile, largest;
static int largestArea;
static U32 written;
static void put(const char *s){U32 n=0;while(s[n])++n;WriteFile(logFile,s,n,&written,0);}
static void number(int n){char b[16];int i=15;b[i]=0;if(n<0){put("-");n=-n;}do{b[--i]=(char)('0'+n%10);n/=10;}while(n);put(b+i);}
static BOOL visit(H window,UP unused){
    (void)unused;
    if(!IsWindowVisible(window))return 1;
    U16 wide[512];char text[2048];RECT r={0};
    int n=GetWindowTextW(window,wide,512);GetClientRect(window,&r);
    int bytes=WideCharToMultiByte(65001,0,wide,n,text,2047,0,0);text[bytes]=0;
    number(r.w);put("x");number(r.h);put(" ");put(text);put("\r\n");
    if(r.w>640&&r.h>400&&r.w*r.h>largestArea){largest=window;largestArea=r.w*r.h;}
    return 1;
}
void entry(void){
    logFile=CreateFileA("g:\\pc-reference-windows.txt",0x40000000,0,0,2,0,0);
    EnumWindows(visit,0);
    if(!largest){put("NO LARGE VISIBLE WINE WINDOW\r\n");CloseHandle(logFile);ExitProcess(2);}
    RECT r={0};GetClientRect(largest,&r);
    if(r.w>4096||r.h>4096){put("SIZE BUDGET\r\n");CloseHandle(logFile);ExitProcess(3);}
    H dc=GetDC(largest),memory=CreateCompatibleDC(dc);void *pixels=0;
    BI info={40,r.w,r.h,1,32,0,(U32)(r.w*r.h*4),0,0,0,0};
    H bitmap=CreateDIBSection(dc,&info,0,&pixels,0,0),old=SelectObject(memory,bitmap);
    if(!bitmap){put("DIB FAILED ");number((int)GetLastError());put("\r\n");CloseHandle(logFile);ExitProcess(4);}
    if(!BitBlt(memory,0,0,r.w,r.h,dc,0,0,0x00cc0020)){
        put("GDI COPY FAILED ");number((int)GetLastError());put("; trying WM_PRINT\r\n");
        if(!PrintWindow(largest,memory,3)){put("WM_PRINT FAILED\r\n");CloseHandle(logFile);ExitProcess(4);}
    }
    H file=CreateFileA("g:\\pc-reference-window.bmp",0x40000000,0,0,2,0,0);
    unsigned char head[14]={0x42,0x4d};U32 total=54+info.bytes;
    head[2]=(unsigned char)total;head[3]=(unsigned char)(total>>8);head[4]=(unsigned char)(total>>16);head[5]=(unsigned char)(total>>24);head[10]=54;
    WriteFile(file,head,14,&written,0);WriteFile(file,&info,40,&written,0);WriteFile(file,pixels,info.bytes,&written,0);
    CloseHandle(file);SelectObject(memory,old);DeleteObject(bitmap);DeleteDC(memory);ReleaseDC(largest,dc);
    put("BMP WRITTEN; VISUAL INSPECTION REQUIRED\r\n");CloseHandle(logFile);ExitProcess(0);
}
