#!/usr/bin/env python3
"""Source-labelled tactic namespace and applied critical result stores, no RNG.

The original critical decision body is inspected statically, never executed.
Only readonly tactic lookup and already-produced result/failure stores run.
"""
import argparse,hashlib,json,struct,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'content'))
from inspect_pc_scenario_tail import NativeTailDecoder
from capstone import Cs,CS_ARCH_X86,CS_MODE_32
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_EBP,UC_X86_REG_ESP,UC_X86_REG_EIP
EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly PC source required')
    raw=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Executable changed')
    shared=(installation/'Media/scenario/Scenario.s11').read_bytes();d=NativeTailDecoder(raw);tables=d.decode_tail(shared,True);u=d.u
    def forbid(m,a,size,user):raise ValueError('Rule/RNG entry forbidden '+hex(a))
    for a in [0x5ae610,0x5af850,0x472150,0x4721d0,0x402310,0x444150,0x4442a0]:u.hook_add(UC_HOOK_CODE,forbid,begin=a,end=a)
    skills=[]
    for index,name in [(39,'勇將'),(41,'鬥神'),(42,'槍神'),(48,'霸王')]:
        row=next(r for r in tables['records'] if r['kind']=='table_80ef8' and r['native_index']==index)
        actor=bytes.fromhex(row['actor_hex']);actual=actor[4:9].split(b'\0')[0].decode('big5');description=actor[0x1c:0x60].split(b'\0')[0].decode('big5')
        if actual!=name or '會心一擊' not in description:raise ValueError('Source critical description differs')
        if not any(r['destination']=='actor+0x1c' and r['return_address']=='0x494639' for r in row['reads']):raise ValueError('Description source read missing')
        skills.append(dict(nativeSkillId=index,name=actual,description=description,recordSha256=row['sha256'],sourceOffset=row['offset'],nameRawHex=actor[4:9].hex(),descriptionRawHex=actor[0x1c:0x60].hex()))
    # Original5ae610 tests raw skill48 before returning1 at5ae760, and
    # tests41/42 in spear dispatch. Named serializer descriptions identify
    # these as applied critical decisions; no probabilities are rerolled.
    instructions=[dict(address=hex(i.address),mnemonic=i.mnemonic,operands=i.op_str,hex=i.bytes.hex()) for i in Cs(CS_ARCH_X86,CS_MODE_32).disasm(bytes(u.mem_read(0x5ae610,0x2bd)),0x5ae610)]
    for address,op in [(0x5ae696,'0x30'),(0x5ae6d1,'0x29'),(0x5ae6e2,'0x2a')]:
        if not any(r['address']==hex(address) and r['mnemonic']=='push' and r['operands']==op for r in instructions):raise ValueError('Original critical skill branch differs')
    tactics=[];stores=[];record=d.stream+0x200
    for action in range(32):
        row=next(r for r in tables['records'] if r['kind']=='table_84858' and r['native_index']==action)
        name=bytes.fromhex(row['actor_hex'])[4:13].split(b'\0')[0].decode('big5')
        before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));u.mem_write(d.stack+0x40,struct.pack('<I',action));u.reg_write(UC_X86_REG_ESP,d.stack)
        u.emu_start(0x5afd82,0x5afda2,count=1000)
        pointer=u.reg_read(UC_X86_REG_EAX)
        if pointer!=d.root+0x84858+action*0x44 or bytes(u.mem_read(pointer,0x44)).hex()!=row['actor_hex']:raise ValueError('Original constructor/tactic namespace differs')
        if before!=bytes(u.mem_read(0x7200000,0x300000)) or rng!=bytes(u.mem_read(0x8a5d44,4)):raise ValueError('Readonly namespace mutated world/RNG')
        tactics.append(dict(nativeTacticId=action,name=name,nameRawHex=bytes(u.mem_read(pointer+4,9)).hex(),recordSha256=row['sha256'],sourceOffset=row['offset']))
        for success,alreadyCritical in [(0,0),(0,1),(1,0),(1,1)]:
            u.mem_write(record,bytes(0x80));u.mem_write(record+0x54,struct.pack('<I',success));u.reg_write(UC_X86_REG_EBP,record);u.reg_write(UC_X86_REG_ESP,d.stack)
            if not success:u.emu_start(0x5aff08,0x5aff78,count=100)
            else:
                # Start after the original decision call with its already
                # computed0/1 result; only original stack cleanup/store executes.
                u.reg_write(UC_X86_REG_EAX,alreadyCritical);u.emu_start(0x5aff72,0x5aff78,count=100)
            if u.reg_read(UC_X86_REG_EIP)!=0x5aff78 or struct.unpack('<I',u.mem_read(record,4))[0]!=(alreadyCritical if success else 0):raise ValueError('Applied critical/failure store differs')
            if before!=bytes(u.mem_read(0x7200000,0x300000)) or rng!=bytes(u.mem_read(0x8a5d44,4)):raise ValueError('Store mutated original authority/RNG')
            stores.append(dict(nativeTacticId=action,alreadyProducedSuccessRaw=success,alreadyProducedCriticalRaw=alreadyCritical,record0Raw=alreadyCritical if success else 0))
    for index,name in [(0,'突刺'),(1,'螺旋突刺'),(2,'二段突刺')]:
        if tactics[index]['name']!=name:raise ValueError('Original spear tactic names differ')
    report=dict(sourceExecutableSha256=EXE_SHA,sourceSharedSha256=hashlib.sha256(shared).hexdigest(),checks=len(tactics)+len(stores),skills=skills,tactics=tactics,stores=stores,staticOnlyCriticalInstructions=instructions,
                codeSha256={hex(a):hashlib.sha256(bytes(u.mem_read(a,b-a))).hexdigest() for a,b in [(0x490c90,0x490caf),(0x5afd82,0x5afda2),(0x5aff08,0x5aff78),(0x5ae610,0x5ae8cd),(0x494670,0x4946d6),(0x494600,0x49466c)]},
                established=['Same order/action argument goes through original490c90 into source-labelled table84858, not project enum ordinal.','Original skill41鬥神/42槍神/48霸王 descriptions and decision branches identify record0 as applied critical; source failure forces0.','Combined source chain27 maps record0 zero/nonzero to sound49/78 in the exact named spear tactic action path.'],
                limits=['Critical decision/probabilities/rules/RNG never executed; inputs already-produced explicit fixture values.','No full PC battle, callback wall-clock timing or Android integration claimed by this tool.','Project critical formula remains owned by core; media consumes actual immutable applied facts without changing it.','String-pool pointer distances are not skill IDs; the initial41槍神 guess is withdrawn and actual source41鬥神/42槍神 preserved.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(dict(status='PASS_SOURCE_TACTIC_AND_CRITICAL_SEMANTICS',checks=report['checks'])))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
