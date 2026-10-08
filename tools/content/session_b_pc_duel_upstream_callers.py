#!/usr/bin/env python3
"""Read-only upstream challenge callers; raw candidates require alignment audit."""
import argparse
import json
import struct
from pathlib import Path
import capstone
from audit_pc_restoration_sources import EXE_SHA, sha, output_guard


def inspect(installation, output):
    output_guard(installation, output)
    if output.exists():
        raise ValueError('Preserve earlier receipt')
    raw = (installation / 'san11pk.exe').read_bytes()
    assert sha(raw) == EXE_SHA
    h = struct.unpack_from('<I', raw, 60)[0]
    count = struct.unpack_from('<H', raw, h+6)[0]
    optional = struct.unpack_from('<H', raw, h+20)[0]
    base = struct.unpack_from('<I', raw, h+52)[0]
    sections = [struct.unpack_from('<4I', raw, h+24+optional+40*i+8) for i in range(count)]
    def read(address, length):
        virtual, rva, stored, offset = next(s for s in sections if s[1] <= address-base < s[1]+max(s[0], s[2]))
        return raw[offset+address-base-rva:offset+address-base-rva+length]
    dis = capstone.Cs(capstone.CS_ARCH_X86, capstone.CS_MODE_32)
    def bounded(address, length):
        data = read(address, length)
        return dict(address=hex(address), boundedBytes=length, sha256=sha(data), bytes=data.hex(), instructions=[dict(address=hex(i.address), mnemonic=i.mnemonic, operands=i.op_str) for i in dis.disasm(data, address)])
    targets = [0x4bca30,0x59f570,0x4ad1f0,0x4959a0,0x4952c0,0x495310,0x495330,0x495350,0x495370,0x495390,0x417120,0x41f350,0x41ef20,0x41f000,0x41eec0,0x415f20,0x415e20,0x489b40,0x599cf0,0x59a810,0x5884c0, 0x58b640, 0x57dd80, 0x4964f0, 0x48a8b0, 0x489bd0]
    calls = []
    for virtual, rva, stored, offset in sections:
        if rva != 0x1000:
            continue
        text = raw[offset:offset+stored]
        for i in range(len(text)-5):
            if text[i] != 0xe8:
                continue
            address = base+rva+i
            target = address+5+struct.unpack_from('<i', text, i+1)[0]
            if target not in targets:
                continue
            if target == 0x4964f0 and not 0x570000 <= address < 0x5c0000:
                continue
            calls.append(dict(target=hex(target), callAddress=hex(address), context=bounded(address-64, 144)))
    functions = [bounded(a,n) for a,n in [(0x5884c0,0x48), (0x57dd80,0x80), (0x4964f0,0x50),(0x57a150,0xa7),(0x5a8d50,0x2f),(0x57dbc0,0x70),(0x57dbd0,0x110),(0x57e010,0xb0),(0x57e5d0,0x200),(0x632a10,6),(0x634e60,0xeb),(0x635030,0xf),(0x634550,0x1f),(0x495170,0x21),(0x5a3100,0xcb),(0x5a3c20,0x46),(0x5a3cd0,0x2c0),(0x5a4c50,0x9a),(0x5a4cf0,0x36a),(0x5a3980,0xd5),(0x5a4990,0x2b6),(0x4882b0,0x16),(0x5a3870,0xe5),(0x495fe0,0x1e),(0x496cc0,0x71),(0x5933a0,0x259),(0x58b640,0x41b),(0x58ad60,0xb0),(0x58ae10,0x110),(0x4b0e20,0x300),(0x4b1950,0x180),(0x4a9120,0x220),(0x4b27f0,0x30),(0x4b2380,0x460),(0x4b03d0,0xd0),(0x4b2820,0x464),(0x4b0140,0x70),(0x4b01b0,0x70),(0x4b0220,0x1b0),(0x4add50,0x110)]]
    functions.extend(bounded(a,n) for a,n in [(0x4b0f50,0x340),(0x4ce2f0,0x120),(0x4acbe0,0x500),(0x4bbb00,0x180),(0x58a5e0,0x220),(0x58a420,0x1c0),(0x4a7410,0x1b0),(0x4a7990,0x1e0),(0x48a8b0,0x90),(0x489bd0,0x90),(0x489b40,0x60),(0x489bb0,0x40),(0x4a6340,0xc0),(0x49e4d0,0x180),(0x47a950,0x170),(0x489bc0,0x10),(0x49e450,0x80),(0x47b480,0xc0),(0x489b40,0x60),(0x4a57b0,0xc0),(0x599d00,0x380),(0x4bf6f0,0x3f0),(0x483b00,0x70),(0x5b9e10,0x488),(0x5b9b40,0x120),(0x5ba320,0xbc),(0x598d60,0x1c0),(0x482f20,0x90),(0x482f80,0x90),(0x489120,0x40),(0x599b00,0x250),(0x599cf0,0xc20),(0x59c420,0x300),(0x598630,0x68),(0x589f70,0x290),(0x50d7a0,0x190),(0x50d030,0x1a0),(0x589a50,0x40),(0x4843a0,0x190),(0x589ba0,0x100),(0x5899f0,0x40),(0x589a30,0x20),(0x58b400,0x240),(0x58aca0,0x150),(0x58a7f0,0xb0),(0x483b20,0x60),(0x5a0700,0x100),(0x4d19d0,0x130),(0x59fe00,0xb0),(0x59fea0,0x100),(0x468e60,0x20),(0x573470,0x20),(0x57a150,0x120),(0x57f020,0x80),(0x4d3a50,0x180),(0x4a6d50,0x90),(0x4afd60,0x170),(0x4a92c0,0x180),(0x4afed0,0x270),(0x4a8440,0x430),(0x4a88a0,0xd0),(0x5c4f80,0x240),(0x48bdf0,0x50),(0x488c70,0x30),(0x4adb30,0x90),(0x4aed40,0x190),(0x58af20,0x4e0),(0x57a150,0xb0),(0x59a7d0,0x110),(0x59a800,0x120),(0x472590,0x50),(0x47bc30,0x30),(0x4b0f50,0x320),(0x4a31e0,0x100)])
    functions.extend(bounded(a,n) for a,n in [(0x48bd80,0x70),(0x4af7d0,0x590)])
    functions.extend(bounded(a,n) for a,n in [(0x4cf500,0x200),(0x4af5f0,0x100),(0x412390,0x20),(0x4123b0,0x20)])
    functions.append(bounded(0x658fb0,0x80))
    functions.extend(bounded(a,n) for a,n in [(0x4adb30,0xd0),(0x4aed40,0x250),(0x4ad9a0,0x110),(0x4adaa0,0x90)])
    functions.extend(bounded(a,n) for a,n in [(0x489030,0x90),(0x481910,0x30),(0x436740,0x30),(0x49f2a0,0x100)])
    functions.extend(bounded(a,n) for a,n in [(0x4aa680,0x600),(0x489d40,0x80),(0x484de0,0x80),(0x4f6690,0x500),(0x57f9a0,0x280),(0x85eb60,0x80),(0x6ba9e0,0x250),(0x6b1ed0,0x150),(0x9c40000,0x80),(0x9c41450,0x80)])
    aiConstants={hex(a):read(a,n).hex() for a,n in [(0x7e849c,12),(0x77b334,4),(0x74e718,4),(0x659034,24)]}
    functions.extend(bounded(a,n) for a,n in [(0x472150,0x30),(0x472180,0x50),(0x4721d0,0x70),(0x472110,0x40),(0x481910,0x60),(0x4cf360,0x100),(0x486680,0x40),(0x4cf2f0,0x70)])
    functions.extend(bounded(a,n) for a,n in [(0x486c80,0xa0),(0x487bc0,0x100),(0x487e10,0x80),(0x489fe0,0x60),(0x49cdf0,0x20)])
    functions.append(bounded(0x486c80,0xb0))
    functions.extend(bounded(a,n) for a,n in [(0x415e20,0x330),(0x415d00,0x120),(0x56fc20,0xb0)])
    functions.append(bounded(0x415f20,0x30))
    functions.extend(bounded(a,n) for a,n in [(0x4813e0,0x40),(0x4812a0,0x70),(0x4be2a0,0x510),(0x4cef90,0x1d0),(0x59c280,0x1a0),(0x589850,0x120),(0x59a4b0,0x370),(0x4b9550,0x180),(0x481240,0x80),(0x4aa200,0x100),(0x4a0940,0x80)])
    functions.extend(bounded(a,n) for a,n in [(0x4a93b0,0x520),(0x4a9280,0x90),(0x4a92c0,0x190),(0x4a7280,0x190)])
    functions.append(bounded(0x849c88,0x60))
    functions.extend(bounded(a,n) for a,n in [(0x579bc0,0x180),(0x576c10,0x90),(0x59f570,0x90)])
    functions.extend(bounded(a,n) for a,n in [(0x4958d0,0x200),(0x572860,0x30),(0x4cc850,0x80)])
    functions.extend(bounded(a,n) for a,n in [(0x5afbe0,0x110),(0x423230,0x80),(0x4952d0,0x50),(0x495390,0x100)])
    functions.extend(bounded(a,n) for a,n in [(0x4952d0,0x420),(0x496100,0x470)])
    functions.extend(bounded(a,n) for a,n in [(0x4951c0,0x110),(0x495240,0x70),(0x496010,0x40),(0x589400,0x198),(0x5860a0,0x140)])
    functions.extend(bounded(a,n) for a,n in [(0x589400,0x390),(0x495ca0,0x140),(0x4955a0,0x90),(0x5870e0,0x90),(0x58a9e0,0x20)])
    functions.extend(bounded(a,n) for a,n in [(0x634590,0x120),(0x634610,0x130),(0x5a0700,0x100)])
    functions.extend(bounded(a,n) for a,n in [(0x417120,0x550),(0x41eec0,0x400),(0x41f2f0,0x70)])
    functions.extend(bounded(a,n) for a,n in [(0x41eef0,0x160),(0x41f350,0x190),(0x416040,0x170),(0x417040,0x190)])
    functions.extend(bounded(a,n) for a,n in [(0x419020,0x180),(0x56fc00,0xe0)])
    gridRefs=[]
    for virtual,rva,stored,offset in sections:
        if rva!=0x1000:continue
        data=raw[offset:offset+stored];start=0
        while True:
            at=data.find(struct.pack('<I',0x44e5878),start)
            if at<0:break
            address=base+rva+at;gridRefs.append(dict(operandAddress=hex(address),context=bounded(address-40,96)));start=at+1
    singletonRefs=[]
    for value in [0x3260860,0x1285018,0x1285012,0x1285019,0x1da19c]:
        for virtual,rva,stored,offset in sections:
            if rva!=0x1000:continue
            data=raw[offset:offset+stored];start=0
            while True:
                at=data.find(struct.pack('<I',value),start)
                if at<0:break
                address=base+rva+at;singletonRefs.append(dict(value=hex(value),operandAddress=hex(address),context=bounded(address-32,100)));start=at+1
    functionPointerRefs=[]
    for value in [0x57a150,0x579c40,0x849c88,0x849c30,0x849cb0]:
        for virtual,rva,stored,offset in sections:
            data=raw[offset:offset+stored];start=0
            while True:
                at=data.find(struct.pack('<I',value),start)
                if at<0:break
                address=base+rva+at;functionPointerRefs.append(dict(value=hex(value),address=hex(address),sectionRva=hex(rva),bytes=read(address-32,100).hex()));start=at+1
    task37=dict(completionSelector=read(0x5ba2e8+35,1)[0],completionTarget=hex(struct.unpack('<I',read(0x5ba298+4*read(0x5ba2e8+35,1)[0],4))[0]),destinationSelector=read(0x5ba3f0+28,1)[0],destinationTarget=hex(struct.unpack('<I',read(0x5ba3dc+4*read(0x5ba3f0+28,1)[0],4))[0]))
    output.write_text(json.dumps(dict(functionPointerRefs=functionPointerRefs,speechSingletonRefs=singletonRefs,aiDispositionConstants=aiConstants,speechGridRefs=gridRefs,task37Dispatch=task37,exeSha=EXE_SHA, functions=functions, possibleDirectCalls=calls, staticEnergyCostTable=dict(address='0x79cc08',bytes=read(0x79cc08,9).hex()), rendererSubVtable=dict(address='0x857dc0',bytes=read(0x857dc0,32).hex()), limits=['Raw E8 references are candidates, not certified aligned callers', 'Static callers alone do not prove original ordinary command fee/action or Android flow', 'Energy table495170/79cc08 and5933a0 belong to separate strategy mask: do not assign index3 cost15 to challenge', 'Challenge handler57a150 consumes cached record+4 bit8 from5a4990; strategy5a3cd0 output is record+8' ], completeGoal=False), indent=2) + '\n')
    print('PASS original upstream candidates', sha(output.read_bytes()), len(calls))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    inspect(a.installation, a.output)
