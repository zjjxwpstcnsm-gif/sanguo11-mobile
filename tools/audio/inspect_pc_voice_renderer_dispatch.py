#!/usr/bin/env python3
"""Original display callback maps raw renderer action + supplied random result.

No RNG executes: 402310 is an explicit already-produced0/1 boundary. Voice,
effect spawning and positional sound sinks are captured, not normal gameplay.
Raw action IDs are not assumed to be War/Army tactic ordinals.
"""
import argparse,hashlib,itertools,json,struct,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'content'))
from inspect_pc_scenario_tail import NativeTailDecoder
from capstone import Cs,CS_ARCH_X86,CS_MODE_32
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly source required')
    raw=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Executable changed')
    d=NativeTailDecoder(raw);d.decode_tail((installation/'Media/scenario/Scenario.s11').read_bytes(),True);u=d.u
    context,payload=d.stream+0x100,d.stream+0x700;u.mem_write(context,struct.pack('<I',payload));u.mem_write(payload+8,struct.pack('<H',0));u.mem_write(d.root+0x169730+0xc,struct.pack('<i',5));choice=0;voices=[];effects=[];sounds=[];random_calls=[]
    def boundary(m,a,size,user):
        sp=m.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',m.mem_read(sp,4))[0];consumed=0;result=1
        if a==0x402310:
            bound=struct.unpack('<I',m.mem_read(sp+4,4))[0]
            if bound!=2:raise ValueError('Unexpected original random bound')
            random_calls.append(bound);result=choice
        elif a==0x4d19d0:
            p,actor,gain=struct.unpack('<3I',m.mem_read(sp+4,12))
            if actor!=d.root+0xc0bc+5*0x190 or gain!=0xbf800000:raise ValueError('Original source speaker/gain differs')
            voices.append(p);consumed=12
        elif a==0x414670:
            effect,position=struct.unpack('<2I',m.mem_read(sp+4,8));effects.append(effect)
            if position!=context+0x20 or m.reg_read(UC_X86_REG_ECX)!=0xa5fef0:raise ValueError('Original effect position/receiver differs')
            consumed=8
        elif a==0x564bb0:
            sound,position=struct.unpack('<2I',m.mem_read(sp+4,8));sounds.append(sound)
            if position!=context+0x20 or m.reg_read(UC_X86_REG_ECX)!=context:raise ValueError('Original sound position/receiver differs')
            consumed=8
        m.reg_write(UC_X86_REG_EAX,result);m.reg_write(UC_X86_REG_ESP,sp+4+consumed);m.reg_write(UC_X86_REG_EIP,ret)
    for a in [0x402310,0x4d19d0,0x414670,0x564bb0]:u.hook_add(UC_HOOK_CODE,boundary,begin=a,end=a)
    def forbidden(m,a,size,user):raise ValueError('RNG entered '+hex(a))
    for a in [0x444150,0x4442a0,0x472150,0x4721d0]:u.hook_add(UC_HOOK_CODE,forbidden,begin=a,end=a)
    rows=[]
    for action,choice,raw240 in itertools.product(range(-1,38),[0,1],[0,1]):
        u.mem_write(context+4,struct.pack('<i',action));u.mem_write(context+0x240,struct.pack('<i',raw240));voices.clear();effects.clear();sounds.clear();random_calls.clear();before=bytes(u.mem_read(0x7200000,0x300000));receiver=bytes(u.mem_read(context,0x280));rng=bytes(u.mem_read(0x8a5b68,4))+bytes(u.mem_read(0x8a5d44,4))
        u.reg_write(UC_X86_REG_ECX,context);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<I',d.stop));u.emu_start(0x564cb0,d.stop,count=100000)
        if u.reg_read(UC_X86_REG_EIP)!=d.stop or u.reg_read(UC_X86_REG_ESP)!=d.stack+4:raise ValueError('Original callback return differs')
        if before!=bytes(u.mem_read(0x7200000,0x300000)) or receiver!=bytes(u.mem_read(context,0x280)) or rng!=bytes(u.mem_read(0x8a5b68,4))+bytes(u.mem_read(0x8a5d44,4)):raise ValueError('Captured readonly dispatch changed state')
        active=6<=action<=28 and action not in [22,23,24,25]
        if len(voices)!=int(active) or len(random_calls)!=int(active) or len(effects)!=int(active) or len(sounds)!=int(active):raise ValueError('Original callback dispatch counts differ')
        if active and (effects!=([78] if raw240 else [46]) or sounds!=([78] if raw240 else [49])):raise ValueError('Original positional sound/effect fields differ')
        rows.append(dict(rendererActionRaw=action,alreadyProducedRandomRaw=choice,renderer240Raw=raw240,profiles=voices.copy(),effectIds=effects.copy(),soundIds=sounds.copy(),randomBoundaryBounds=random_calls.copy()))
    cs_ranges=[(0x564cb0,0x564e30),(0x402310,0x40232b),(0x444150,0x44426e),(0x5619e0,0x561a1a)]
    static_rng=[dict(address=hex(i.address),mnemonic=i.mnemonic,operands=i.op_str,bytesHex=i.bytes.hex()) for a,b in [(0x402310,0x40232b),(0x444150,0x44426e)] for i in Cs(CS_ARCH_X86,CS_MODE_32).disasm(bytes(u.mem_read(a,b-a)),a)]
    report=dict(sourceExecutableSha256=EXE_SHA,checks=len(rows),rows=rows,codeSha256={hex(a):hashlib.sha256(bytes(u.mem_read(a,b-a))).hexdigest() for a,b in cs_ranges},
        staticOnlyRngInstructions=static_rng,evidence=['Original dispatch field is renderer+4; separate FSM state is renderer+c.','The callback uses402310(2) ->4442a0 ->444150, whose state counter8a5b68 and624word buffer6ed37f0 differ from scalar8a5d44.','Originalunit+cleader/native getters execute; existing lower voice selection is separately proved inbatch21.'],
        limits=['Raw renderer action domain remains distinct from project War/Army ordinals; upstream mapping not inferred.','Random result is supplied as already-produced boundary; neither original RNG is executed.','Voice/effect/sound sinks captured; nativeOS/normal Android action binding and installed playback not proved.','renderer240Raw semantics remain unknown; do not rename critical/success.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print('PASS original renderer voice/profile/sound dispatch checks=',len(rows))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
