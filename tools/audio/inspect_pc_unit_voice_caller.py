#!/usr/bin/env python3
"""Full original unit voice speaker/validity/current-byte selection; no RNG.

Only audio availability and final audio dispatch are supplied. Actor/unit
getters and validity/type/ability selectors execute on native registries.
Explicit fixtures do not establish the action-to-profile producer.
"""
import argparse,hashlib,itertools,json,struct,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'content'))
from inspect_pc_scenario_tail import NativeTailDecoder
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP

EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly source required')
    exe=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(exe).hexdigest()!=EXE_SHA:raise ValueError('Executable changed')
    d=NativeTailDecoder(exe);shared=(installation/'Media/scenario/Scenario.s11').read_bytes();d.decode_tail(shared,True);u=d.u;u.mem_map(0x9119000,0x6000)
    context,payload,available=d.stream+0x100,d.stream+0x180,d.stream+0x200
    u.mem_write(context,struct.pack('<I',payload));u.mem_write(0x9119810+0x34,struct.pack('<I',available));dispatch=[];availability=True
    def backend(m,a,size,user):
        sp=m.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',m.mem_read(sp,4))[0]
        if a==0x4d1000:
            values=struct.unpack('<2I',m.mem_read(sp+4,8));dispatch.append(dict(voiceId=values[0] if values[0]<0x80000000 else values[0]-0x100000000,gainSentinelRaw=values[1]));consumed=8;result=1
        else:
            if m.reg_read(UC_X86_REG_ECX)!=available:raise ValueError('Unexpected audio availability receiver')
            consumed=0;result=int(availability)
        m.reg_write(UC_X86_REG_EAX,result);m.reg_write(UC_X86_REG_ESP,sp+4+consumed);m.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,backend,begin=0x4d1000,end=0x4d1000);u.hook_add(UC_HOOK_CODE,backend,begin=0x6e96a0,end=0x6e96a0)
    def forbid(m,a,size,user):raise ValueError('Forbidden original RNG entry '+hex(a))
    for a in [0x472150,0x4721d0]:u.hook_add(UC_HOOK_CODE,forbid,begin=a,end=a)
    bases=list(struct.unpack('<71i',u.mem_read(0x7f1420,284)));table=list(struct.unpack('<16i',u.mem_read(0x7f1360,64)))
    types=[[0,0],[1,0],[2,0],[3,0],[1,1],[2,1],[3,0],[1,0]]
    patterns=[[50,50,50,50],[49,50,49,49],[50,50,49,49],[49,50,50,49],[49,50,49,50],[0,255,0,0],[255,0,255,255]]
    rows=[]
    def run(unit_id,leader,profile,kind,abilities,status,raw17c):
        unit=d.root+0x169730+unit_id*0xf4;u.mem_write(payload+8,struct.pack('<H',unit_id));u.mem_write(unit+0xc,struct.pack('<i',leader))
        if 0<=leader<1100:
            actor=d.root+0xc0bc+leader*0x190;u.mem_write(actor+0x100,struct.pack('<i',kind));u.mem_write(actor+0x170,bytes(abilities));u.mem_write(actor+0xa0,struct.pack('<i',status));u.mem_write(actor+0x17c,struct.pack('<i',raw17c))
        before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));receiver=bytes(u.mem_read(context,0x200));dispatch.clear()
        u.reg_write(UC_X86_REG_ECX,context);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<II',d.stop,profile&0xffffffff));u.emu_start(0x564c00,d.stop,count=100000)
        if u.reg_read(UC_X86_REG_EIP)!=d.stop or u.reg_read(UC_X86_REG_ESP)!=d.stack+8:raise ValueError('Original unit caller return/stack differs')
        if before!=bytes(u.mem_read(0x7200000,0x300000)) or rng!=bytes(u.mem_read(0x8a5d44,4)) or receiver!=bytes(u.mem_read(context,0x200)):raise ValueError('Readonly caller mutated original world/RNG/context')
        valid=0<=leader<1100 and (raw17c!=0 or 0<=status<=8) and 0<=profile<71
        expected=[]
        if valid and availability:
            first,second=types[kind];superior=int(abilities[1]>max(abilities[0],abilities[2],abilities[3]));expected=[dict(voiceId=bases[profile]+table[2*(first+4*second)+superior],gainSentinelRaw=0xbf800000)]
        if dispatch!=expected:raise ValueError(('Original unit voice dispatch differs',leader,profile,kind,status,raw17c,dispatch,expected))
        row=dict(sourceUnitNativeId=unit_id,leaderNativeId=leader,profileRaw=profile,voiceTypeRaw=kind,currentAbilityBytes=abilities,statusRaw=status,actor17cRaw=raw17c,audioAvailable=availability,dispatch=list(dispatch),returnRaw=u.reg_read(UC_X86_REG_EAX));rows.append(row)
    for profile,kind,abilities in itertools.product(range(71),range(8),patterns):run(profile%3,[0,5,1099][profile%3],profile,kind,abilities,0,0)
    for status,raw17c,availability in itertools.product([-2,-1,0,8,9,255],[0,1,-1],[False,True]):run(999,699,0,0,patterns[0],status,raw17c)
    availability=True
    for leader,profile in itertools.product([-1,1100],[0,-1,71]):run(1,leader,profile,0,patterns[0],0,0)
    for profile in [-1,71]:run(0,0,profile,0,patterns[0],0,0)
    report=dict(sourceExecutableSha256=EXE_SHA,sourceSharedScenarioSha256=hashlib.sha256(shared).hexdigest(),checks=len(rows),rows=rows,
        sourcePath='564c00 ->56dba0 payload+8 ushort native unit index ->495a40 unit+c leader ->490b00 original actor registry ->4d19d0 ->4d1290 ->4d1000',
        evidence=['Original47a600/actor virtual4=4883f0 validates source status0..8 or actor17c nonzero; no actor-validity shim.','Original489030 reads cached current unsigned byte atactor+170+index; no ability-getter shim.','Complete3MiB original world, original RNG and fixture UI receiver/payload bytes unchanged per call.'],
        limits=['Explicit actor/unit/current-cache fixtures, not a normal Android action-to-profile mapping.','Audio availability and final backend captured; no installed voice playback claim.','No original RNG function executed; RNG entries fail immediately; no Wine, source writes or gameplay commands.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print('PASS full original unit voice caller checks=',len(rows))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
