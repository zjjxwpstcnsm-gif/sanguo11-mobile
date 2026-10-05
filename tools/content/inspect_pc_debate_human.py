#!/usr/bin/env python3
"""Original523dc0 human getter and51f350 bounded selected-input branch.

Widget visibility/GUI scheduling is deliberately outside this bounded oracle.
No rule/selector is replaced. The original prologue and selected-input remainder
execute; its UI-index value is obtained from the actual original getter.
"""
import argparse,gzip,itertools,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EIP
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    packed=(ROOT/'docs/handoff/20261004/session1/debate-flow-native.json.gz').read_bytes()
    if sha(packed)!='193ca13a6bb92a3c1d389934166249611536150a50733ba4fd183fe786a5fca7':raise ValueError('Original initial oracle changed')
    initial=json.loads(gzip.decompress(packed))['cases'];d=NativeDebateFlow(installation,(installation/'san11pk.exe').read_bytes());w=d.world;model=d.fixture
    d.run([90,82],[0,0],[31,31],23);view=0xc300000;w.u.mem_map(view,0x2000);rows=[]
    boundary=w.u.hook_add(UC_HOOK_CODE,lambda u,a,n,user:u.emu_stop(),begin=0x51f393,end=0x51f393)
    for case,side,slot in itertools.product(range(16),range(2),range(-1,9)):
        entry=initial[case];w.u.mem_write(model,bytes.fromhex(entry['initialStateHex']))
        for actor,temper in zip(d.people,entry['nativePersonality']):w.u.mem_write(actor+0xfc,struct.pack('<i',temper))
        for offset,value in [(4,4),(8,4),(12,0),(0x16c,side),(0x170,-1),(0x174,-1),(0x10+side*0xa0+0x98,1)]:w.u.mem_write(model+offset,struct.pack('<i',value))
        w.u.mem_write(view+0x9ac+side*0x19c,struct.pack('<i',slot));w.u.mem_write(0x8a5d44,struct.pack('<I',23))
        before=bytes(w.u.mem_read(model,0x1b0))
        # Original prologue establishes native stack/locals/register ABI.
        w.call(0x51f350,1,receiver=model,stop=0x51f393);context=w.u.context_save();stack=bytes(w.u.mem_read(w.stack-0x200,0x400))
        selected=w.call(0x523dc0,side,receiver=view)
        w.u.mem_write(w.stack-0x200,stack);w.u.context_restore(context);w.u.reg_write(UC_X86_REG_EAX,selected)
        # Start exactly at the original human/AI index merge, after the UI getter.
        w.u.emu_start(0x51f3d7,w.stop,count=10000000)
        if w.u.reg_read(UC_X86_REG_EIP)!=w.stop:raise ValueError('Original selected-index branch did not return')
        after=bytes(w.u.mem_read(model,0x1b0));accepted=struct.unpack_from('<i',after,12)[0]==1
        if struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]!=23:raise ValueError('Original human input unexpectedly consumed RNG')
        if not accepted and before!=after:raise ValueError('Original rejected UI index mutated model')
        rows.append(dict(case=case,side=side,slot=slot,accepted=accepted,beforeStateHex=before.hex(),afterStateHex=after.hex(),nativeRng=23))
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,initialOracleSha256=sha(packed),cases=rows,
                sequence=['51f350 original prologue ->51f393 boundary','523dc0 actual input index getter','51f3d7..51f41f original selected-input merge ->51e510'],
                limits=['Synthetic UI index storage, not original widget constructor/visibility/GUI scheduling',
                        'Original branch slot validation only; ordinary UI card legality is separately proven51e3c0',
                        'No complete headed human PC flow, campaign settlement or APK certification'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(cases=len(rows),sha256=sha(output.read_bytes()),completeHumanGui=False)))
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',required=True,type=Path)
    a=p.parse_args();inspect(a.installation,a.output)
