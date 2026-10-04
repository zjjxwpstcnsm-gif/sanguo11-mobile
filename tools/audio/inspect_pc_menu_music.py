#!/usr/bin/env python3
"""Original established load/new-game/tutorial/options/exit menu music entry.

Executes559220 music prologue through original wrappers; stops before GUI work.
Audio availability and final backend are explicit platform observation seams.
No rule, RNG, Wine, PC writes or guessed map selection.
"""
import argparse,hashlib,json,struct,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'content'))
from inspect_pc_biography_messages import NativeMessageInterpreter,text_spans
from inspect_pc_message_resources import decode
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
def sha(raw):return hashlib.sha256(raw).hexdigest()
def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly PC source required')
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Original executable changed')
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000]);u.mem_map(0x9119000,0x2000)
    receiver,stack,stop=0x10000000,0x20000000,0x30000000
    u.mem_map(receiver,0x1000);u.mem_map(stack,0x2000);u.mem_map(stop,0x1000)
    u.mem_write(0x8a5d44,struct.pack('<I',0x12345678));u.mem_map(0x6ed3000,0x2000);u.mem_write(0x6ed37f0,bytes([0x5a])*2496)
    current={};calls=[]
    def backend(m,a,size,user):
        sp=m.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',m.mem_read(sp,4))[0]
        if a==0x6e96a0:result=current['available'];consumed=0
        else:
            calls.append(list(struct.unpack('<5I',m.mem_read(sp+4,20))));result=1;consumed=20
        m.reg_write(UC_X86_REG_EAX,result);m.reg_write(UC_X86_REG_ESP,sp+4+consumed);m.reg_write(UC_X86_REG_EIP,ret)
    for a in [0x6e96a0,0x4cfc20]:u.hook_add(UC_HOOK_CODE,backend,begin=a,end=a)
    rows=[]
    for audio_pointer in [0,receiver+0x800]:
        for available in [0,1]:
            for enabled in [0,1]:
                for volume in [0x00000000,0x3eb33333,0x3f400000,0x3f800000]:
                    current['available']=available;calls.clear();u.mem_write(0x9119810,bytes(0x100));u.mem_write(0x9119810+0x34,struct.pack('<I',audio_pointer));u.mem_write(0x9119810+0x58,struct.pack('<I',enabled));u.mem_write(0x9119810+0x64,struct.pack('<I',volume))
                    manager=bytes(u.mem_read(0x9119810,0x100));state=bytes(u.mem_read(receiver,0x1000));rng=bytes(u.mem_read(0x8a5d44,4));mt=bytes(u.mem_read(0x6ed37f0,2496));counter=bytes(u.mem_read(0x8a5b68,4))
                    sp=stack+4096;u.mem_write(sp,struct.pack('<I',stop));u.reg_write(UC_X86_REG_ECX,receiver);u.reg_write(UC_X86_REG_ESP,sp);u.emu_start(0x559220,0x55923f,count=10000)
                    expected=[[1,1,volume,500,0]] if audio_pointer and available and enabled else []
                    if u.reg_read(UC_X86_REG_EIP)!=0x55923f or calls!=expected:raise ValueError('Original menu dispatch differs')
                    if manager!=bytes(u.mem_read(0x9119810,0x100)) or state!=bytes(u.mem_read(receiver,0x1000)) or rng!=bytes(u.mem_read(0x8a5d44,4)) or mt!=bytes(u.mem_read(0x6ed37f0,2496)) or counter!=bytes(u.mem_read(0x8a5b68,4)):raise ValueError('Menu music selection mutated guarded source state')
                    rows.append(dict(audioPointerPresent=bool(audio_pointer),availabilityRaw=available,musicEnabledRaw=enabled,defaultVolumeRaw=volume,dispatch=list(calls)))
    widgets=list(struct.unpack('<7I',exe[0x8b93c0-0x400000:0x8b93c0-0x400000+28]));pointers=list(struct.unpack('<7I',exe[0x8b93e8-0x400000:0x8b93e8-0x400000+28]));labels=[]
    for widget,pointer in zip(widgets,pointers):
        if 0x400000<=pointer<0x900000:
            raw=exe[pointer-0x400000:pointer-0x400000+128].split(b'\0')[0];labels.append(dict(widgetId=widget,pointer=hex(pointer),rawHex=raw.hex(),text=raw.decode('big5')))
    if [r['text'] for r in labels]!=['載入進度','重新開始新遊戲','遊戲教學','項目','結束遊戲']:raise ValueError('Original established menu role changed')
    source=(installation/'Media/msg/S11MSG02.s11').read_bytes();decoded,proof=decode(exe,source);native=NativeMessageInterpreter(exe,decoded);raw=native.render(0x2da4);spans=text_spans(raw)
    if ''.join(s['text'] or '' for s in spans)!='請選擇目錄選單':raise ValueError('Original menu caption differs')
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,nativeChecks=len(rows),rows=rows,music=dict(musicId=1,resourceId=2238,repeat=1,fadeMillis=500,volume='Original -1 sentinel resolves manager+64 user gain',caller='559220 ->4d1900 ->4d04f0 ->4cfc20',stop='55923f before GUI construction'),menuEvidence=dict(windowInitializer='558b20 ->55b0a0(0)',buttonTable='8b93c0/8b93e8',labels=labels,captionId=0x2da4,captionRawHex=raw.hex(),captionSpans=spans,messageResourceSha256=sha(source),messageDecode=proof),codeSha256={hex(a):sha(exe[a-0x400000:a-0x400000+n]) for a,n in [(0x559220,0x1f),(0x558b20,0x128),(0x4d1900,0x43),(0x4d04f0,0x74)]},limits=['Original established main menu role; not normal map/season music or12/16 scene naming.','Music prologue/wrappers execute; later GUI/scene commands are not executed.','Explicit backend availability and final capture; no actual Windows playback claim.','Android must bind after actual equivalent menu establishment, preserve mute/gain, stop when leaving for an unbound scene, and prove fresh installed PCM/lifecycle.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'nativeChecks':len(rows),'musicId':1,'menuLabels':[r['text'] for r in labels],'outputSha256':sha(output.read_bytes())}))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
