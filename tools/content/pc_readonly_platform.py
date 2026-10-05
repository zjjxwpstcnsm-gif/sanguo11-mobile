#!/usr/bin/env python3
"""Bounded read-only Win32 file fixture for unmodified original x86 readers."""
import fnmatch
import struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EAX,UC_X86_REG_EIP
from audit_pc_restoration_sources import sha


class ReadOnlyPlatform:
    def __init__(self,world,installation):
        self.world=world;self.u=world.u;self.root=installation.resolve();self.calls=[];self.files={};self.directories={};self.next_handle=0x4000;self.error=0
        self.heap=0x12000000;self.heap_end=self.heap+0x4000000;self.allocations={};self.u.mem_map(self.heap,0x4000000)
        self.u.mem_map(0x4ed0000,0x2010000);self.u.mem_map(0x9c40000,0x40000)
        self.callbacks={}
        # Import identities independently read from the pinned PE image.
        slots={0x74e27c:('FindFirstFileA',2),0x74e274:('FindNextFileA',2),0x74e278:('FindClose',1),
            0x74e288:('GetFullPathNameA',4),0x74e2a8:('CreateFileA',7),0x74e2a4:('ReadFile',5),
            0x74e298:('GetFileSize',2),0x74e29c:('SetFilePointer',4),0x74e348:('CloseHandle',1),
            0x74e2b8:('InitializeCriticalSection',1),0x74e2bc:('DeleteCriticalSection',1),
            0x74e2f4:('GetLastError',0),0x74e104:('SetLastError',1),0x74e2e0:('GetFileAttributesA',1),
            0x74e2a0:('WriteFile',5),0x74e280:('DeleteFileA',1),0x74e294:('SetEndOfFile',1),0x74e2cc:('lstrlenA',1),
            0x74e260:('InterlockedDecrement',1),0x74e25c:('InterlockedIncrement',1),
            0x74e21c:('GetModuleHandleA',1),0x74e2fc:('GetProcAddress',2)}
        imports=self.imports(world.exe)
        for slot,(name,words) in slots.items():
            if imports.get(slot)!=('KERNEL32.dll',name):raise ValueError('PE import identity changed: '+hex(slot)+' '+name)
            addr=world.stop+0x800+len(self.callbacks)*16;self.callbacks[addr]=(name,words,words)
            self.u.mem_write(slot,struct.pack('<I',addr))
        self.spin_lock=world.stop+0x800+len(self.callbacks)*16
        self.callbacks[self.spin_lock]=('InitializeCriticalSectionAndSpinCount',2,2)
        self.u.hook_add(UC_HOOK_CODE,self.callback,begin=world.stop+0x800,end=world.stop+0xc00)
        # CRT allocation/free boundary only; event parser and custom allocator
        # logic remain original. No field/condition/event result is substituted.
        for address,name,words in [(0x7096ba,'malloc',1),(0x707dff,'free',1),(0x70c84d,'realloc',2)]:
            self.callbacks[address]=(name,words,0);self.u.hook_add(UC_HOOK_CODE,self.callback,begin=address,end=address)
        # Statically linked CRT FILE boundary. Avoid assuming the unstarted
        # process's CRT locks/TLS are initialized. Original 46dc70 and all
        # event discovery, header and section parsing still execute unchanged.
        for address,name,words in [(0x70891d,'fopen',2),(0x708462,'fseek',3),
                                  (0x708880,'ftell',1),(0x708695,'rewind',1),
                                  (0x708649,'fread',4),(0x70850f,'fclose',1),
                                  (0x709c0e,'ascii_mbscmp',2),(0x707d28,'atexit',1)]:
            self.callbacks[address]=(name,words,0);self.u.hook_add(UC_HOOK_CODE,self.callback,begin=address,end=address)
        world.call(0x4439a0,0x6ed2e50,0x4ed2e50,0x200000)
        if world.call(0x70e7da)!=1:raise ValueError('Original CRT lock initializer failed')
        world.call(0x71dfa1);world.call(0x678bc0,receiver=0x9c41a00)
        # Original CString nil manager and assign routine; virtual process
        # executable directory is an explicit platform fixture, not PC launch.
        nil=world.call(0x71df93,receiver=0x9c5df78)+0x10
        self.u.mem_write(0x6ed2e88+0xa4,struct.pack('<I',nil))
        self.u.mem_write(world.meta+0x4800,b'G:\\\0')
        world.call(0x431270,world.meta+0x4800,3,receiver=0x6ed2e88+0xa4)

    @staticmethod
    def imports(exe):
        u32=lambda p:struct.unpack_from('<I',exe,p)[0]
        string=lambda p:exe[p:exe.index(0,p)].decode('ascii')
        descriptor=u32(u32(0x3c)+24+104);result={}
        # Pinned PE32 image has raw RVA=memory RVA in this inspected region.
        # NativeLayeredWorld independently rejects any executable SHA change.
        while u32(descriptor):
            table,_,_,module,iat=struct.unpack_from('<5I',exe,descriptor)
            index=0
            while u32(table+index*4):
                entry=u32(table+index*4)
                if not entry&0x80000000:result[0x400000+iat+index*4]=(string(module),string(entry+2))
                index+=1
            descriptor+=20
        return result

    def string(self,pointer):return bytes(self.u.mem_read(pointer,2048)).split(b'\0')[0].decode('big5')
    def handle(self):self.next_handle+=1;return self.next_handle
    def path(self,text,wildcard=False):
        text=text.replace('\\','/')
        if text[:3].lower()=='g:/':text=text[3:]
        elif ':' in text or text.startswith('/'):raise ValueError('Unmapped host path requested: '+text)
        if '..' in Path(text).parts:raise ValueError('Read-only path escaped virtual installation')
        result=self.root
        for part in Path(text).parts:
            if wildcard and any(c in part for c in '*?'):return result,part
            matches=[p for p in result.iterdir() if p.name.casefold()==part.casefold()] if result.is_dir() else []
            if len(matches)!=1:return None
            result=matches[0]
            if self.root not in result.resolve().parents and result.resolve()!=self.root:raise ValueError('Virtual path escaped through symlink')
        if self.root not in result.resolve().parents and result.resolve()!=self.root:raise ValueError('Virtual path escaped through symlink')
        return result
    def allocate(self,size):
        size=max(1,size);pointer=(self.heap+15)&~15
        if pointer+size>self.heap_end:raise ValueError('Native platform allocation exceeded bounded arena')
        self.heap=pointer+size;self.allocations[pointer]=size;return pointer
    def find_data(self,buffer,path):
        raw=bytearray(320);struct.pack_into('<I',raw,0,0x10 if path.is_dir() else 0x20)
        struct.pack_into('<II',raw,28,0,path.stat().st_size if path.is_file() else 0)
        name=path.name.encode('big5');raw[44:44+len(name)]=name;self.u.mem_write(buffer,bytes(raw))

    def callback(self,u,address,size,user):
        name,words,pop=self.callbacks[address];sp=u.reg_read(UC_X86_REG_ESP)
        stack=struct.unpack('<%dI'%(words+1),u.mem_read(sp,(words+1)*4));ret=stack[0];a=list(stack[1:]);value=1;detail={}
        if name in ('WriteFile','DeleteFileA','SetEndOfFile'):raise ValueError('Original reader requested forbidden installation mutation: '+name)
        if name=='atexit':value=0;detail={'destructor':hex(a[0]),'deferredUntilFixtureDisposal':True}
        elif name=='ascii_mbscmp':
            left=bytes(u.mem_read(a[0],2048)).split(b'\0')[0];right=bytes(u.mem_read(a[1],2048)).split(b'\0')[0]
            if any(b>=128 for b in left+right):raise ValueError('Multibyte CRT comparison requires original locale initialization')
            value=0 if left==right else 1 if left>right else 0xffffffff
            detail={'leftHex':left.hex(),'rightHex':right.hex(),'asciiOnly':True}
        elif name=='fopen':
            text=self.string(a[0]);mode=self.string(a[1]);detail={'path':text,'mode':mode}
            if mode!='rb':raise ValueError('Original CRT open was not read-only binary: '+mode)
            path=self.path(text)
            if path is None or not path.is_file():value=0;self.error=2
            else:
                data=path.read_bytes();value=self.handle();self.files[value]=[data,0]
                detail.update(sourcePath=path.relative_to(self.root).as_posix(),sha256=sha(data),bytes=len(data))
        elif name in ('fseek','rewind'):
            data,index=self.files[a[0]]
            if name=='rewind':target=0
            else:
                offset=a[1] if a[1]<0x80000000 else a[1]-0x100000000
                target=offset if a[2]==0 else index+offset if a[2]==1 else len(data)+offset if a[2]==2 else -1
            if target<0:raise ValueError('Invalid original CRT file seek')
            self.files[a[0]][1]=target;value=0;detail={'position':target}
        elif name=='ftell':value=self.files[a[0]][1]
        elif name=='fread':
            data,index=self.files[a[3]];chunk=data[index:index+a[1]*a[2]];self.files[a[3]][1]+=len(chunk)
            if chunk:u.mem_write(a[0],chunk)
            value=len(chunk)//a[1] if a[1] else 0;detail={'bytes':len(chunk),'position':index}
        elif name=='fclose':self.files.pop(a[0]);value=0
        elif name=='malloc':value=self.allocate(a[0])
        elif name=='free':value=0
        elif name=='realloc':
            value=self.allocate(a[1]);old=self.allocations.get(a[0],0)
            if a[0] and not old:raise ValueError('Unexamined CRT realloc pointer')
            if old:u.mem_write(value,bytes(u.mem_read(a[0],min(old,a[1]))))
        elif name=='FindFirstFileA':
            text=self.string(a[0]);query=self.path(text,True);paths=[] if query is None else sorted([p for p in query[0].iterdir() if fnmatch.fnmatch(p.name.casefold(),query[1].casefold())],key=lambda p:p.name.casefold())
            detail={'path':text,'matches':[p.relative_to(self.root).as_posix() for p in paths]}
            if not paths:value=0xffffffff;self.error=2
            else:value=self.handle();self.directories[value]=[paths,0];self.find_data(a[1],paths[0])
        elif name=='FindNextFileA':
            paths,index=self.directories[a[0]];index+=1;self.directories[a[0]][1]=index
            if index>=len(paths):value=0;self.error=18
            else:self.find_data(a[1],paths[index])
        elif name=='FindClose':self.directories.pop(a[0])
        elif name=='GetFullPathNameA':
            text=self.string(a[0]).replace('/','\\');text=text if text[:3].lower()=='g:\\' else 'G:\\'+text
            data=text.encode('big5')+b'\0';value=len(data)-1
            if len(data)>a[1]:value=len(data)
            else:
                u.mem_write(a[2],data)
                if a[3]:u.mem_write(a[3],struct.pack('<I',a[2]+text.rfind('\\')+1))
        elif name=='CreateFileA':
            if a[1]!=0x80000000 or a[4]!=3:raise ValueError('Original file open was not read-only existing: '+str(a))
            text=self.string(a[0]);path=self.path(text);detail={'path':text}
            if path is None or not path.is_file():value=0xffffffff;self.error=2
            else:
                data=path.read_bytes();value=self.handle();self.files[value]=[data,0]
                detail.update(sourcePath=path.relative_to(self.root).as_posix(),sha256=sha(data),bytes=len(data))
        elif name=='ReadFile':
            if a[4]:raise ValueError('Overlapped file IO not examined')
            data,index=self.files[a[0]];chunk=data[index:index+a[2]];self.files[a[0]][1]+=len(chunk)
            if chunk:u.mem_write(a[1],chunk)
            u.mem_write(a[3],struct.pack('<I',len(chunk)));detail={'bytes':len(chunk),'position':index}
        elif name=='GetFileSize':
            value=len(self.files[a[0]][0])
            if a[1]:u.mem_write(a[1],bytes(4))
        elif name=='SetFilePointer':
            if a[2]:raise ValueError('64-bit seek not examined')
            data,index=self.files[a[0]];offset=a[1] if a[1]<0x80000000 else a[1]-0x100000000
            target=offset if a[3]==0 else index+offset if a[3]==1 else len(data)+offset if a[3]==2 else -1
            if target<0:raise ValueError('Invalid original file seek')
            self.files[a[0]][1]=target;value=target
        elif name=='CloseHandle':self.files.pop(a[0])
        elif name=='GetFileAttributesA':
            path=self.path(self.string(a[0]));value=0xffffffff if path is None else 0x10 if path.is_dir() else 0x20
        elif name=='GetLastError':value=self.error
        elif name=='GetModuleHandleA':
            module=self.string(a[0])
            if module.lower()!='kernel32.dll':raise ValueError('Unexamined dynamic module request '+module)
            value=0x400;detail={'module':module}
        elif name=='GetProcAddress':
            symbol=self.string(a[1]);detail={'symbol':symbol}
            if a[0]!=0x400 or symbol!='InitializeCriticalSectionAndSpinCount':raise ValueError('Unexamined dynamic procedure '+symbol)
            value=self.spin_lock
        elif name=='SetLastError':self.error=a[0];value=0
        elif name=='lstrlenA':value=len(bytes(u.mem_read(a[0],2048)).split(b'\0')[0])
        elif name in ('InterlockedDecrement','InterlockedIncrement'):
            value=(struct.unpack('<I',u.mem_read(a[0],4))[0]+(-1 if name=='InterlockedDecrement' else 1))&0xffffffff
            u.mem_write(a[0],struct.pack('<I',value))
        elif name not in ('InitializeCriticalSection','DeleteCriticalSection','InitializeCriticalSectionAndSpinCount'):raise ValueError('Unhandled platform import '+name)
        if name not in ('malloc','free','realloc'):self.calls.append(dict(function=name,returnPc=hex(ret),result=value,**detail))
        u.reg_write(UC_X86_REG_EAX,value);u.reg_write(UC_X86_REG_ESP,sp+4+pop*4);u.reg_write(UC_X86_REG_EIP,ret)
