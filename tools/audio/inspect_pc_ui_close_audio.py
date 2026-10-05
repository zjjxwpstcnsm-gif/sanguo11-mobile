#!/usr/bin/env python3
"""Original dialog close branches and full original effect dispatch; no Wine/rules.

Window callback/fallback and child lookup are explicit UI fixture boundaries.
The raw negative close result is distinguished from an Android role binding.
"""
import argparse, hashlib, json, struct
from pathlib import Path
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP

EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

def inspect(installation, output):
    if output.exists() or installation.resolve() in output.resolve().parents:
        raise ValueError('Fresh output outside readonly source required')
    raw=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest()!=EXE_SHA: raise ValueError('Executable changed')
    pe=struct.unpack_from('<I',raw,60)[0];n=struct.unpack_from('<H',raw,pe+6)[0];opt=struct.unpack_from('<H',raw,pe+20)[0];base=struct.unpack_from('<I',raw,pe+52)[0]
    sections=[]
    for i in range(n):
        at=pe+24+opt+i*40;size,va,length,offset=struct.unpack_from('<IIII',raw,at+8);sections.append((base+va,length,offset))
    def read(address,length):
        for start,size,off in sections:
            if start<=address and address+length<=start+size:return raw[off+address-start:off+address-start+length]
        raise ValueError('Unmapped source address')
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x4a0000);u.mem_write(0x400000,raw[:0x4a0000]);u.mem_map(0x8b1000,0x3000);u.mem_write(0x8b1000,read(0x8b1000,0x3000))
    window,stack,stop,shims,manager=0x10000000,0x20001000,0x30000000,0x30001000,0x9119810
    u.mem_map(window,0x5000);u.mem_map(stack-4096,0x3000);u.mem_map(stop,0x3000);u.mem_map(0x9119000,0x6000)
    u.mem_write(window,struct.pack('<I',window+0x1000));u.mem_write(window+0x1000+0x1ac,struct.pack('<I',shims+0x80))
    u.mem_write(manager+0x34,struct.pack('<I',window+0x2000));u.mem_write(manager+0x70,struct.pack('<I',window+0x3000))
    u.mem_write(0x74e360,struct.pack('<I',shims));u.mem_write(0x74e35c,struct.pack('<I',shims+16))
    sounds=[];closed=[];fallback=[];child_id=0
    def hook(m,address,size,user):
        sp=m.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',m.mem_read(sp,4))[0];consumed=0
        if address==0x6e98b0:
            descriptor=struct.unpack('<I',m.mem_read(sp+4,4))[0];sounds.append(bytes(m.mem_read(descriptor,16)).hex());consumed=8;m.reg_write(UC_X86_REG_EAX,0)
        elif address==0x6e96a0:m.reg_write(UC_X86_REG_EAX,1)
        elif address==shims+0x80:
            closed.append(struct.unpack('<i',m.mem_read(sp+4,4))[0]);consumed=4;m.reg_write(UC_X86_REG_EAX,1)
        elif address==0x4db7e0:consumed=4;m.reg_write(UC_X86_REG_EAX,child_id)
        elif address in [0x4dba70,0x4754c0]:fallback.append(hex(address));consumed=12;m.reg_write(UC_X86_REG_EAX,0)
        else:consumed=4
        m.reg_write(UC_X86_REG_ESP,sp+4+consumed);m.reg_write(UC_X86_REG_EIP,ret)
    for address in [0x6e98b0,0x6e96a0,shims,shims+16,shims+0x80,0x4db7e0,0x4dba70,0x4754c0]:u.hook_add(UC_HOOK_CODE,hook,begin=address,end=address)
    def call(address,args):
        sounds.clear();closed.clear();fallback.clear();u.mem_write(manager+0x200,bytes(0x200));u.reg_write(UC_X86_REG_ECX,window);u.reg_write(UC_X86_REG_ESP,stack);u.mem_write(stack,struct.pack('<'+'I'*(1+len(args)),stop,*args));before=bytes(u.mem_read(window,0x1000));u.emu_start(address,stop,count=100000)
        if u.reg_read(UC_X86_REG_EIP)!=stop or u.reg_read(UC_X86_REG_ESP)!=stack+4+4*len(args):raise ValueError('Native UI return/stack differs')
        if before!=bytes(u.mem_read(window,0x1000)):raise ValueError('Captured close mutated fixture window')
        return dict(dispatchHex=list(sounds),closeResults=list(closed),fallback=list(fallback),returnRaw=u.reg_read(UC_X86_REG_EAX))
    rows=[]
    for address in [0x4dd860,0x4dd8c0]:
        for flags in range(32):
            u.mem_write(window+0x120,struct.pack('<I',flags));r=call(address,[0,0,0])
            expected_close=([-2147483648] if flags&1 else []) if address==0x4dd860 else ([-2147483648] if flags&2 else [-2] if flags&8 else [])
            if r['closeResults']!=expected_close or len(r['dispatchHex'])!=int(bool(expected_close)):raise ValueError('Source close condition differs')
            if r['dispatchHex'] and bytes.fromhex(r['dispatchHex'][0])[:3]!=bytes([0,0,1]):raise ValueError('Original close effect bank/slot differs')
            rows.append(dict(caller=hex(address),flagsRaw=flags,**r))
    for child_id in [0,0x1235,0x1236,0x1237,0x1238]:
        r=call(0x63b270,[window+0x4000]);expected=[10000] if child_id==0x1236 else [-2] if child_id==0x1237 else []
        if r['closeResults']!=expected or len(r['dispatchHex'])!=int(bool(expected)):raise ValueError('Original child dispatch differs')
        if expected and bytes.fromhex(r['dispatchHex'][0])[:3]!=bytes([0,0,int(child_id==0x1237)]):raise ValueError('Distinct native close samples differ')
        rows.append(dict(caller='0x63b270',childIdBoundaryRaw=child_id,**r))
    report=dict(sourceExecutableSha256=EXE_SHA,checks=len(rows),rows=rows,codeSha256={hex(a):hashlib.sha256(read(a,b-a)).hexdigest() for a,b in [(0x4dd860,0x4dd8b5),(0x4dd8c0,0x4dd94a),(0x63b270,0x63b2d6),(0x4d0570,0x4d0697)]},
        boundaries=['Platform availability and critical-section imports supplied; backend captures original packet instead of playing.','Window close callback/fallback and child-ID lookup supplied, not original OS event dispatch or child layout.'],
        limits=['Native negative/positive close results and samples proved; Android cancellation equivalence/entry still pending.','No general button, gameplay failure, BGM or voice role inference.','No rule command, save or RNG call; no Wine or PC write.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print('PASS original UI close audio checks=',len(rows))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
