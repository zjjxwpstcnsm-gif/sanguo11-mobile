#!/usr/bin/env python3
"""Compare decoded UV evaluation against supplied EXE x86 machine code.

Requires the optional Unicorn runtime outside the source tree. Executes only
examined UV initializer/updater/rectangle lookup functions in an isolated
memory emulator, without running the PC game or writing its installation.
This verifies source arithmetic, not original event timing or rendered pixels.
"""
import argparse
import json
import struct
from pathlib import Path
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EAX, UC_X86_REG_EIP
from pc_effect_uv import sample_uv
from pc_resources import Archive, sha
from inspect_pc_effect_bindings import EXE_SHA, ROOT


def check(installation, source_report, output):
    exe = (installation/'san11pk.exe').read_bytes()
    if sha(exe) != EXE_SHA:
        raise ValueError('Reinspect UV executable')
    report = json.loads(source_report.read_text())
    if report['executable_sha256'] != EXE_SHA:
        raise ValueError('UV report executable mismatch')
    machine = Uc(UC_ARCH_X86, UC_MODE_32)
    machine.mem_map(0x400000, 0x400000)
    machine.mem_write(0x400000, exe[:0x400000])
    record = 0x10000000; context = 0x10020000
    machine.mem_map(record, 0x30000)
    stack = 0x20000000; stop = 0x30000000
    machine.mem_map(stack, 0x10000); machine.mem_map(stop, 0x1000)
    def call(address, this, *arguments):
        machine.reg_write(UC_X86_REG_ESP, stack+0x8000)
        machine.reg_write(UC_X86_REG_ECX, this)
        machine.mem_write(stack+0x8000, struct.pack('<'+'I'*(len(arguments)+1),stop,*arguments))
        machine.emu_start(address, stop, count=10000)
        if machine.reg_read(UC_X86_REG_EIP) != stop:
            raise ValueError('Source UV function did not return within instruction bound')
        return machine.reg_read(UC_X86_REG_EAX)
    archive = Archive(installation/'Media/san11pkres.bin')
    samples = 0; types = set(); resources = {}; comparisons = []
    try:
        for row in report['records']:
            uv = row['uv']
            if uv is None:
                continue
            r = row['resource_id']
            if r not in resources:
                resources[r] = archive.read(r)
                if sha(resources[r]) != row['source_sha256']:
                    raise ValueError('UV source resource mismatch')
            raw = resources[r][uv['offset']:uv['offset']+uv['bytes']]
            if sha(raw) != uv['sha256']:
                raise ValueError('UV program hash mismatch')
            kind = uv['kind']; types.add(kind)
            addresses = struct.unpack_from('<5I',exe,0x3961a8+kind*20)
            for initial in (0, 3, 0xffffffff):
                times = (0, .025, .1, .333, .999, 1, 2, 10)
                updates = (0, 1, 31, 32, 33, 1023, 1024, 2049)
                for elapsed, steps in zip(times, updates):
                    elapsed = struct.unpack('<f', struct.pack('<f',elapsed))[0]
                    lifetime = 2.5
                    machine.mem_write(record,raw)
                    machine.mem_write(context,struct.pack('<4I',0,0,0,0))
                    call(0x46d670, record)
                    call(addresses[2], record, context, initial)
                    machine.mem_write(context+8,struct.pack('<2f',lifetime,elapsed))
                    for _ in range(steps if kind == 3 else 1):
                        call(addresses[0], record, context)
                    current = struct.unpack('<I',machine.mem_read(context+4,4))[0]
                    pointer = call(addresses[3],record,current)
                    expected_index, expected_rect = sample_uv(uv,initial_index=initial,elapsed=elapsed,lifetime=lifetime,updates=steps)
                    expected_bytes = struct.pack('<4f',*expected_rect)
                    actual_bytes = bytes(machine.mem_read(pointer,16))
                    actual_index = (pointer-record-16)//16
                    if actual_index != expected_index or actual_bytes != expected_bytes:
                        raise AssertionError((r,row['renderer_offset'],kind,initial,elapsed,steps,actual_index,expected_index))
                    samples += 1
            comparisons.append(dict(resource_id=r,renderer_offset=row['renderer_offset'],kind=kind,source_program_sha256=uv['sha256'],samples=24))
        if types != {0,1,2,3}:
            raise ValueError('UV source dispatch coverage changed')
        result = dict(schema=1,goal_complete=False,status='PASS',programs=len(comparisons),samples=samples,
                      executable_sha256=EXE_SHA,source_report=source_report.relative_to(ROOT).as_posix(),
                      source_report_sha256=sha(source_report.read_bytes()),
                      oracle='Unicorn x86_32 executing supplied EXE UV initialize/update/rectangle methods and 707a74 conversion helper',
                      comparison='Exact source rectangle index and float32 bytes, all four dispatch kinds',
                      records=comparisons,
                      limits=['PC game not running','No event/wall-clock/billboard/blend/texture/material acceptance','No source effect runtime integration'])
        output.parent.mkdir(parents=True,exist_ok=True)
        output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
        print(json.dumps({key:result[key]for key in ('status','programs','samples')}))
    finally:
        archive.close()


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation',type=Path)
    parser.add_argument('--source-report',type=Path,default=ROOT/'docs/pc-visual/effect-uv-working.json')
    parser.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/v140-effect-uv-machine-check.json')
    args=parser.parse_args();check(args.installation,args.source_report.resolve(),args.output.resolve())
