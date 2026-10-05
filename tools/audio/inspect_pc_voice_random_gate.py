#!/usr/bin/env python3
"""Static-only proof of original speech11/12 RNG dependency. Never emulate RNG."""
import argparse,hashlib,json,struct
from pathlib import Path
from capstone import Cs,CS_ARCH_X86,CS_MODE_32

EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly source required')
    raw=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Executable changed')
    pe=struct.unpack_from('<I',raw,60)[0];sections=[]
    for i in range(struct.unpack_from('<H',raw,pe+6)[0]):
        at=pe+24+struct.unpack_from('<H',raw,pe+20)[0]+40*i;_,va,size,off=struct.unpack_from('<4I',raw,at+8);sections.append((0x400000+va,size,off))
    def read(a,n):
        for start,size,off in sections:
            if start<=a and a+n<=start+size:return raw[off+a-start:off+a-start+n]
        raise ValueError('Unmapped source address')
    cs=Cs(CS_ARCH_X86,CS_MODE_32)
    def rows(a,b):return [dict(address=hex(i.address),bytesHex=i.bytes.hex(),mnemonic=i.mnemonic,operands=i.op_str) for i in cs.disasm(read(a,b-a),a)]
    spans=[(0x4721d0,0x472208),(0x5038b4,0x5038e0),(0x503919,0x503921)]
    code={hex(a):dict(end=hex(b),sha256=hashlib.sha256(read(a,b-a)).hexdigest(),instructions=rows(a,b)) for a,b in spans}
    rng=code['0x4721d0']['instructions'];by={r['address']:(r['mnemonic'],r['operands']) for r in rng}
    required={'0x4721db':('mov','eax, dword ptr [0x8a5d44]'),'0x4721e0':('imul','eax, eax, 0x6c078965'),'0x4721e6':('add','eax, 0x3039'),'0x4721eb':('mov','dword ptr [0x8a5d44], eax'),'0x4721f0':('shr','eax, 0x10'),'0x4721f5':('mov','esi, 0x64'),'0x4721fa':('idiv','esi'),'0x472202':('cmp','edx, ecx'),'0x472204':('setl','al')}
    if any(by.get(a)!=instruction for a,instruction in required.items()):raise ValueError('Source RNG formula/control flow differs')
    call=code['0x5038b4']['instructions']
    if (call[0]['mnemonic'],call[0]['operands'])!=('push','0x32') or not any(r['address']=='0x5038d8' and r['mnemonic']=='call' and r['operands']=='0x4721d0' for r in call):raise ValueError('Original speech RNG call differs')
    choice=code['0x503919']['instructions']
    if [(r['mnemonic'],r['operands']) for r in choice]!=[('neg','eax'),('sbb','eax, eax'),('add','eax, 0xc'),('push','eax')]:raise ValueError('Original speech choice differs')
    report=dict(sourceExecutableSha256=EXE_SHA,inspection='STATIC_ONLY_NO_UNICORN_OR_RNG_EXECUTION',code=code,
        originalDependency=dict(call='5038d8 ->4721d0',percentThresholdRaw=50,rngStateAddress='8a5d44',stateUpdate='uint32(previous *0x6c078965 +0x3039)',decision='((updated >>16)%100) <50',resultRange=[0,1],selectorByResult={'0':12,'1':11}),
        established=['Speech11/12 choice consumes original scalar RNG when entered; not a proved tactic ordinal or combat success result.','Native lower voice selector can be readonly while a caller chooses its profile via upstream RNG.'],
        limits=['Original percent RNG never executed or reproduced as a runtime media chooser.','Requires an already committed presentation choice supplied by authority; media must not draw RNG, re-roll, default11/12 or hash a fact to manufacture it.','No normal Android voice binding, original call scene semantics or playback acceptance claimed.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print('PASS static original voice11/12 RNG dependency; native RNG not executed')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
