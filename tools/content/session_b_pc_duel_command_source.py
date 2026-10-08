#!/usr/bin/env python3
"""Bounded original command functions and direct call sites; no inferred UI truth."""
import argparse, json, struct
from pathlib import Path
import capstone
from audit_pc_restoration_sources import EXE_SHA, sha, output_guard

def inspect(installation, output):
    output_guard(installation, output)
    if output.exists(): raise ValueError('Preserve earlier receipt')
    raw = (installation/'san11pk.exe').read_bytes()
    assert sha(raw) == EXE_SHA
    header = struct.unpack_from('<I', raw, 60)[0]
    count = struct.unpack_from('<H', raw, header+6)[0]
    optional = struct.unpack_from('<H', raw, header+20)[0]
    base = struct.unpack_from('<I', raw, header+52)[0]
    sections = [struct.unpack_from('<4I', raw, header+24+optional+40*i+8) for i in range(count)]
    def read(address, length):
        virtual, rva, stored, offset = next(s for s in sections if s[1] <= address-base < s[1]+max(s[0], s[2]))
        return raw[offset+address-base-rva:offset+address-base-rva+length]
    dis = capstone.Cs(capstone.CS_ARCH_X86, capstone.CS_MODE_32)
    unitVtable=list(struct.unpack("<24I",read(0x79cc18,96))); functions = []
    for address, length in [(0x58a8a0,0x390),(0x58ac30,0x70),(0x589ca0,0x1c0),(0x589e60,0x140),(0x589ea0,0xd0),(0x58af20,0x4e0),(0x47a6d0,0x90),(0x481350,0x70),(0x5884f0,0x20),(0x589ac0,0xe0),(0x58a200,0x180),(0x50bb90,0x190),(0x4720f0,0x40),(0x588480,0x90),(0x589400,0x220),(0x586420,0x120),(0x47a690,0x40),(0x490ad0,0x80),(0x47a630,0x30),(unitVtable[2],0x90),(0x496570,0x580),(0x4ad7e0,0x140),(0x495ca0,0x140)]:
        data = read(address,length)
        functions.append(dict(address=hex(address), boundedBytes=length, sha256=sha(data), bytes=data.hex(), instructions=[dict(address=hex(i.address), mnemonic=i.mnemonic, operands=i.op_str) for i in dis.disasm(data,address)]))
    # The earlier 0x580-byte excerpt omitted later calculated combat bytes.
    # Preserve earlier receipts. Exact function code ends at RET496b8b;
    # following embedded jump tables are data, and later entries are separate.
    functions=[f for f in functions if f['address']!='0x496570']
    for address,length in [(0x496570,0x61c),(0x496dd0,0x165),(0x496f40,0x50),(0x495ab0,0x160),(0x495b90,0x120),(0x496160,0x100),(0x4811e0,0x70),(0x489090,0x50),(0x4890a0,0x30)]:
        data=read(address,length)
        functions.append(dict(address=hex(address),boundedBytes=length,sha256=sha(data),bytes=data.hex(),instructions=[dict(address=hex(i.address),mnemonic=i.mnemonic,operands=i.op_str)for i in dis.disasm(data,address)]))
    constants={hex(a):dict(bytes=read(a,n).hex(),values=list(struct.unpack(fmt,read(a,n)))) for a,n,fmt in [(0x83ad58,4,"<f"),(0x769b90,4,"<f"),(0x84afe4,4,"<f"),(0x84aff8,16,"<4i")]};aptitudeWrites=[]
    for virtual,rva,stored,offset in sections:
        if rva != 0x1000:continue
        data=raw[offset:offset+stored]
        for pattern in [bytes.fromhex("8981a8000000"),bytes.fromhex("8986a8000000"),bytes.fromhex("c781a8000000"),bytes.fromhex("c786a8000000")]:
            at=0
            while True:
                at=data.find(pattern,at)
                if at<0:break
                address=base+rva+at
                if 0x495000<=address<0x498000:aptitudeWrites.append(dict(address=hex(address),contextAddress=hex(address-32),bytes=read(address-32,96).hex()))
                at+=1
    calls = []
    for virtual,rva,stored,offset in sections:
        if rva != 0x1000: continue
        data=raw[offset:offset+stored]
        for i in range(len(data)-5):
            if data[i]!=0xe8: continue
            address=base+rva+i
            if address+5+struct.unpack_from('<i',data,i+1)[0]==0x58b640:
                calls.append(dict(callAddress=hex(address), contextAddress=hex(address-48), bytes=read(address-48,112).hex()))
    unitStatTables=dict(address='0x496b8c',bytes=read(0x496b8c,0x30).hex(),sha256=sha(read(0x496b8c,0x30)))
    output.write_text(json.dumps(dict(exeSha=EXE_SHA, functions=functions,unitStatTables=unitStatTables,constants=constants,unitVtable=unitVtable,aptitudeWriteCandidates=aptitudeWrites, possibleDirectCalls=calls, limits=['Raw E8 references require aligned caller validation','Bounded static source only; original gameplay/menu and Android flow separate'], completeGoal=False),indent=2)+'\n')
    print('PASS bounded source',sha(output.read_bytes()),'possible callers',len(calls))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
