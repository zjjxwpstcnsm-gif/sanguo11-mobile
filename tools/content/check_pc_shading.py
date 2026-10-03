#!/usr/bin/env python3
"""Execute original static-model pipeline setup with a recording D3D device.

Unicorn is optional investigation tooling. GPU calls are record-only stubs:
this verifies source render-state arguments, not PC rendered pixels.
"""
import argparse
import json
from pathlib import Path
import struct
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP, UC_X86_REG_EAX, UC_X86_REG_EIP
from inspect_pc_effect_bindings import EXE_SHA
from pc_resources import sha

ROOT=Path(__file__).resolve().parents[2]


def check(installation,output):
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Reinspect source shading EXE')
    u=Uc(UC_ARCH_X86,UC_MODE_32)
    u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    heap=0x10000000;stack=0x20000000;stop=0x30000000
    u.mem_map(heap,0x10000);u.mem_map(stack,0x10000);u.mem_map(stop,4096)
    interface=heap;device=heap+0x1000;interface_table=heap+0x2000;device_table=heap+0x3000
    u.mem_write(interface,struct.pack('<I',interface_table));u.mem_write(device,struct.pack('<I',device_table))
    callbacks={};states=[];stages=[];samplers=[];constants=[]
    def register(table,slot,pop,kind):
        address=heap+0x4000+len(callbacks)*16
        u.mem_write(table+slot,struct.pack('<I',address));u.mem_write(address,b'\xc3')
        callbacks[address]=(pop,kind)
    register(interface_table,8,0,'interface-other');register(interface_table,0x10,0,'interface-device')
    register(device_table,0xe4,12,'state');register(device_table,0x10c,16,'stage')
    register(device_table,0x178,16,'constant')
    register(device_table,0x114,16,'sampler') # original 44d1c0 executes through D3D call
    callbacks[0x44ff40]=(0,'binding-clear') # cdecl surrounding device cleanup
    callbacks[0x401490]=(8,'paint-binding') # thiscall(stage,texture)
    def hook(uc,address,size,user):
        if address not in callbacks:return
        pop,kind=callbacks[address];sp=uc.reg_read(UC_X86_REG_ESP)
        ret=struct.unpack('<I',uc.mem_read(sp,4))[0];value=0
        if kind=='interface-device':value=device
        elif kind=='state':
            dev,state,arg=struct.unpack('<3I',uc.mem_read(sp+4,12));assert dev==device
            states.append([state,arg])
        elif kind=='stage':
            dev,stage,state,arg=struct.unpack('<4I',uc.mem_read(sp+4,16));assert dev==device
            stages.append([stage,state,arg])
        elif kind=='sampler':
            dev,stage,state,arg=struct.unpack('<4I',uc.mem_read(sp+4,16));assert dev==device
            samplers.append([stage,state,arg])
        elif kind=='constant':
            dev,index,data,count=struct.unpack('<4I',uc.mem_read(sp+4,16));assert dev==device
            constants.append(dict(register=index,count=count,values=list(struct.unpack('<'+str(count*4)+'f',uc.mem_read(data,count*16)))))
        uc.reg_write(UC_X86_REG_EAX,value);uc.reg_write(UC_X86_REG_ESP,sp+4+pop);uc.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,hook)
    u.mem_write(stack+0x8000,struct.pack('<2I',stop,interface));u.reg_write(UC_X86_REG_ESP,stack+0x8000)
    u.emu_start(0x41b420,stop,count=10000)
    if u.reg_read(UC_X86_REG_EIP)!=stop:raise AssertionError('Source state setup instruction bound')
    state_map=dict(states);stage_map={(stage,state):value for stage,state,value in stages}
    expected={7:1,14:1,15:1,24:0,25:5,27:1,19:5,20:6,22:2,171:1}
    for key,value in expected.items():
        if state_map.get(key)!=value:raise AssertionError(('D3D state',key,state_map.get(key),value))
    for stage in (0,1):
        if stage_map.get((stage,1))!=5:raise AssertionError('Source both COLOROP MODULATE2X')
    if stage_map.get((0,4))!=4 or stage_map.get((1,4))!=2:raise AssertionError('Source alpha stage pipeline')
    if constants!=[dict(register=4,count=1,values=[1.]*4)]:raise AssertionError('Source c4 tint')
    if samplers!=[[0,1,1],[0,2,1],[1,1,3],[1,2,3]]:raise AssertionError('Source texture address modes')
    result=dict(schema=1,goal_complete=False,status='PASS',executable_sha256=EXE_SHA,
        function='41b420',function_sha256=sha(exe[0x1b420:0x1b637]),states=states,
        texture_stages=stages,sampler_helper_calls=samplers,constants=constants,
        comparison='Source machine-code arguments recorded; checked depth/alpha/blend/cull/two-stage color and address modes',
        limits=['D3D device and binding clear are recording stubs; sampler wrapper executes original machine code','No GPU rasterization or source-game runtime acceptance',
            'Source alpha blending framebuffer encoding and model draw ordering still require comparison'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(dict(status='PASS',states=len(states),texture_stages=len(stages),sampler_calls=len(samplers))))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
    p.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/v141-shading-machine-check.json')
    a=p.parse_args();check(a.installation,a.output)
