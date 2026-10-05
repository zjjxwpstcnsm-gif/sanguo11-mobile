#!/usr/bin/env python3
"""Compare native422da0 clock writes with bounded admitted mobile frame inputs."""
import argparse
import json
from pathlib import Path
import struct
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP, UC_X86_REG_FPCW
from inspect_pc_effect_bindings import EXE_SHA
from pc_resources import sha

ROOT=Path(__file__).resolve().parents[2]


def check(installation,output):
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Source EXE changed')
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    terrain,renderer,stack,stop=0x10000000,0x20000000,0x30000000,0x40000000
    u.mem_map(terrain,0x1400000);u.mem_map(renderer,0x10000);u.mem_map(stack,0x10000);u.mem_map(stop,4096)
    # Native CRT707075 control state was independently executed by the effect
    # oracle. This water-only oracle uses that explicitly recorded CPU input.
    u.reg_write(UC_X86_REG_FPCW,0x23f)
    u.mem_write(0x8a5a78,struct.pack('<I',10000))
    u.mem_write(renderer+0x5544,struct.pack('<I',1))
    u.mem_write(renderer+8,struct.pack('<I',0)) # packed source coarse coordinate0,0
    record=terrain+0x1285012
    samples=[]
    for period in (10000,10001,12345,16384,19999):
        for phase in (0,1,period//2,period-1):
            for input_dt in (.0009,.001,.0166666667,.0333333333,.0999,.1):
                dt=struct.unpack('<f',struct.pack('<f',input_dt))[0]
                u.mem_write(record,struct.pack('<HH',phase,period))
                u.mem_write(renderer+0x5568,struct.pack('<f',0))
                u.mem_write(stack+0x8000,struct.pack('<2If',stop,terrain+8,dt))
                u.reg_write(UC_X86_REG_ESP,stack+0x8000);u.reg_write(UC_X86_REG_ECX,renderer)
                u.emu_start(0x422da0,stop,count=5000)
                actual=struct.unpack('<H',u.mem_read(record,2))[0]
                delta=int(dt*1000)
                expected=((phase+delta)&65535)%period
                if actual!=expected or u.reg_read(UC_X86_REG_EIP)!=stop:raise AssertionError((period,phase,dt,actual,expected))
                # Integer high/low clock arithmetic used in the shader. The
                # native small-step recurrence agrees with total accumulated ms.
                samples.append(dict(period=period,phase=phase,input_dt=dt,delta_ms=delta,result=actual))
    clock_cases=0
    for total in (0,100,2**24-1,2**24+1,2**32-1,2**32+1234567,2**48+87,2**63-1):
        for period in range(10000,20000,37):
            low,high=total&0xffffffff,total>>32
            term=(high%period)*(65536%period)%period
            term=term*(65536%period)%period
            got=(123+term+low%period)%period
            if got!=(123+total)%period:raise AssertionError('64-bit shader water clock')
            clock_cases+=1
    report=dict(schema=1,status='PASS_SOURCE_CLOCK_ONLY',goal_complete=False,executable_sha256=EXE_SHA,
        native_updates=len(samples),shader_integer_cases=clock_cases,native_function='422da0',cpu_control_input='023f',
        limits=['Mobile admitted delta is capped100ms; original PC caller timing remains unverified',
                'Android currently advances visible owner chunks, rather than exact original per-cell draw lists',
                'No GPU frame capture, PC video timing or Android visual acceptance in this check'],samples=samples)
    output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:report[k]for k in ('status','native_updates','shader_integer_cases')}))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
    p.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/v147-water-clock-source.json')
    a=p.parse_args();check(a.installation,a.output)
