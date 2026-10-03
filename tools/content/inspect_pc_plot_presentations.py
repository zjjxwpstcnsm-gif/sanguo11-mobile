#!/usr/bin/env python3
"""Verify fullscreen selectors against the supplied EXE's own plot names.

Read-only static evidence; no Wine, process patches or inferred name-to-text
matching. Caller visibility flags, noncritical policy and PC lens stay pending.
"""
import argparse, hashlib, json, struct
from pathlib import Path
from capstone import Cs, CS_ARCH_X86, CS_MODE_32
from inspect_pc_effect_bindings import EXE_SHA

ROOT=Path(__file__).resolve().parents[2]
NAMES=['火計','滅火','偽報','擾亂','鎮靜','伏兵','內訌','妖術','落雷']
BRANCHES=[0x593460,0x59347c,0x593498,0x5934b4,0x5934cd,0x5934e6,0x5934ff,0x593518,0x593531]
TARGETS=[0x591270,0x5915e0,0x5917d0,0x591a20,0x591c70,0x592740,0x592a80,0x592050,0x592ed0]

def inspect(installation,output):
    data=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(data).hexdigest()!=EXE_SHA:raise ValueError('Source EXE changed')
    md=Cs(CS_ARCH_X86,CS_MODE_32)
    def instructions(begin,end):return list(md.disasm(data[begin-0x400000:end-0x400000],begin))
    dispatch=instructions(0x5933a0,0x593548)
    if not any(i.address==0x593459 and i.mnemonic=='jmp' and i.op_str=='dword ptr [ecx*4 + 0x5935fc]' for i in dispatch):raise ValueError('Original plot dispatch changed')
    if list(struct.unpack_from('<9I',data,0x1935fc))!=BRANCHES:raise ValueError('Original plot branch table changed')
    rows=[]
    for plot,(expected,branch,target) in enumerate(zip(NAMES,BRANCHES,TARGETS)):
        ptr=struct.unpack_from('<I',data,0x4aecb4+plot*4)[0]
        name=data[ptr-0x400000:ptr-0x400000+64].split(b'\0',1)[0].decode('big5')
        if name!=expected:raise ValueError('Original plot name order changed')
        code=instructions(branch,branch+0x20)
        calls=[i for i in code if i.mnemonic=='call']
        if not calls or calls[0].op_str!=hex(target):raise ValueError('Original plot handler changed')
        rows.append(dict(source_plot_id=plot,source_name=name,name_pointer=hex(ptr),
            source_name_table='0x8aecb4',dispatch_branch=hex(branch),handler=hex(target),
            dispatch_call=hex(calls[0].address),android_plot=['FIRE','EXTINGUISH','MISLEAD','CONFUSE','CALM','AMBUSH','INFIGHT','SORCERY','LIGHTNING'][plot]))
    for plot,call,selector in [(7,0x592153,126),(8,0x592fb8,127)]:
        code=instructions(TARGETS[plot],call+5)
        last=code[-1]
        if last.address!=call or last.mnemonic!='call' or last.op_str!='0x589780':raise ValueError('Original dynamic fullscreen call changed')
        pushes=[i for i in code[-6:] if i.mnemonic=='push'][-2:]
        if len(pushes)!=2 or [int(i.op_str,0) for i in pushes]!=[1000,selector]:raise ValueError('Original selector arguments changed')
        template,top=struct.unpack_from('<2I',data,0x3774e0+selector*8)
        resource=struct.unpack_from('<I',data,0x37692c+template*12+4)[0]
        rows[plot].update(fullscreen_selector=selector,fullscreen_call=hex(call),source_wait_argument=1000,
            effect_template=template,effect_resource=resource,texture_resource=369+selector,destination_top=top,
            handler_prefix_sha256=hashlib.sha256(data[TARGETS[plot]-0x400000:call+5-0x400000]).hexdigest(),
            cue_context=[dict(va=hex(i.address),bytes=i.bytes.hex(),mnemonic=i.mnemonic,operands=i.op_str) for i in code[-12:]])
    report=dict(schema=1,goal_complete=False,source_executable_sha256=EXE_SHA,
        status='ORIGINAL_NAMED_PLOT_DISPATCH_AND_MAGIC_SELECTORS_VERIFIED',rows=rows,
        correction='Selector126/template121 belongs to original plot7妖術, not plot3擾亂. Previous AndroidCONFUSE binding removed.',
        runtime_binding='Successful critical SORCERY126/121 and LIGHTNING127/122 from immutable applied PlotOutcome; never reroll',
        android_duration_millis=750,android_duration_range_millis=[500,1000],
        limits=['Original name table, dispatch and immediate selector arguments are source evidence, not a PC movie acceptance',
                '1000 is an original wait argument, not proof of complete PC playback duration',
                'Other plots may use other paths; absence in this inspector is not proof no fullscreen effect exists',
                'Visibility flags, successful noncritical magic policy, original lens and MOD playback remain pending',
                'No Wine launched; source installation remains read-only'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(status=report['status'],plots=len(rows),bindings=[(r['android_plot'],r.get('fullscreen_selector')) for r in rows if 'fullscreen_selector' in r])))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('installation',type=Path)
    parser.add_argument('--output',type=Path,default=ROOT/'docs/pc-visual/plot-presentations-source-working.json')
    args=parser.parse_args();inspect(args.installation,args.output)
