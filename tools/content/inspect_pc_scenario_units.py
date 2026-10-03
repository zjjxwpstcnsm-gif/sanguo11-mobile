#!/usr/bin/env python3
"""Resolve native scenario unit validity using original officer records and fixups.

No Wine, gameplay launch, activation inference, or installation writes. This is
the load-time registry before later scenario-start events, not an army importer.
"""
import argparse
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_EBX, UC_X86_REG_EBP, UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP
from inspect_pc_scenario_tail import NativeTailDecoder
from inspect_pc_scenario_officers import BASE, STRIDE, NATIVE_COUNT, sha


class NativeScenarioUnitDecoder(NativeTailDecoder):
    def __init__(self, exe):
        self.reading_officers = self.running_fixup = False
        super().__init__(exe)
        self.u.hook_add(UC_HOOK_CODE, self.officer_boundary, begin=0x49383f, end=0x49383f)
        self.u.hook_add(UC_HOOK_CODE, self.enter, begin=0x48b760, end=0x48b760)

    def boundary(self, u, address, size, user):
        if not self.running_fixup:
            super().boundary(u, address, size, user)

    def officer_boundary(self, u, address, size, user):
        if self.reading_officers:
            u.emu_stop()

    def decode_units(self, raw):
        tail = self.decode_tail(raw)
        unit_rows = [r for r in tail['records'] if r['kind'] == 'table_169730']
        u = self.u
        for index in range(1100):
            actor = self.root+0xc0bc+index*0x190
            self.scalar(0x489f10, actor)
            self.addresses[actor] = ('officer', index, 0x190, 0x48b760)
        self.cursor, self.records = BASE, []
        u.reg_write(UC_X86_REG_EBX, self.root)
        u.reg_write(UC_X86_REG_EBP, self.stream)
        u.reg_write(UC_X86_REG_ESP, self.stack)
        self.reading_officers = True
        try:
            u.emu_start(0x493812, 0x49383f, count=20000000)
        finally:
            self.reading_officers = False
        self.finish_record()
        if u.reg_read(UC_X86_REG_EIP) != 0x49383f or self.cursor != BASE+STRIDE*NATIVE_COUNT:
            raise ValueError('Original officer loop boundary changed')
        if len(self.records) != 1100 or any(r['bytes'] != (152 if r['native_index'] < 850 else 0) for r in self.records):
            raise ValueError('Original officer type/count dispatch changed')
        officers_sha = sha(bytes(u.mem_read(self.root+0xc0bc,1100*0x190)))
        before = bytes(u.mem_read(0x7200000,0x300000))
        u.reg_write(UC_X86_REG_EBX, self.root)
        u.reg_write(UC_X86_REG_EBP, self.stream)
        u.reg_write(UC_X86_REG_ESP, self.stack)
        self.running_fixup = True
        try:
            u.emu_start(0x493b06,0x493b48,count=10000000)
        finally:
            self.running_fixup = False
        if u.reg_read(UC_X86_REG_EIP) != 0x493b48:
            raise ValueError('Original unit fixup loop did not complete')
        after = bytes(u.mem_read(0x7200000,0x300000))
        changed = [hex(0x7200000+i) for i,(a,b) in enumerate(zip(before,after)) if a!=b]
        units = []
        for row in unit_rows:
            actor = row['actor_address']
            u.reg_write(UC_X86_REG_ESP,self.stack)
            u.mem_write(self.stack,struct.pack('<II',self.stop,actor))
            u.emu_start(0x47a630,self.stop,count=100000)
            if u.reg_read(UC_X86_REG_EIP)!=self.stop:
                raise ValueError('Original unit validity did not return')
            valid = u.reg_read(UC_X86_REG_EAX)
            if valid not in (0,1):
                raise ValueError('Unexpected native validity result')
            units.append(dict(native_index=row['native_index'],source_offset=row['offset'],source_bytes=row['bytes'],source_sha256=row['sha256'],valid=bool(valid),before_actor_hex=row['actor_hex'],after_actor_hex=bytes(u.mem_read(actor,0xf4)).hex()))
        if after != bytes(u.mem_read(0x7200000,0x300000)):
            raise ValueError('Validity inspection mutated registry')
        return dict(officers=dict(loop=['0x493812','0x49383f'],count=1100,serialized_count=850,source_start=BASE,source_end=self.cursor,registry_sha256=officers_sha),fixup=dict(loop=['0x493b06','0x493b48'],changed_byte_addresses=changed),unit_count=len(units),valid_unit_count=sum(x['valid'] for x in units),units=units)


def audit(installation, output):
    installation,output=installation.resolve(),output.resolve()
    if output==installation or installation in output.parents:
        raise ValueError('Output must be outside read-only PC installation')
    exe=(installation/'san11pk.exe').read_bytes()
    decoder=NativeScenarioUnitDecoder(exe)
    sources=[]
    for path in sorted((installation/'Media/scenario').iterdir(),key=lambda p:p.name.lower()):
        if path.name.lower().startswith('scen0') and path.suffix.lower()=='.s11':
            raw=path.read_bytes()
            row=dict(path=path.relative_to(installation).as_posix(),sha256=sha(raw),**decoder.decode_units(raw))
            sources.append(row)
            print(json.dumps(dict(path=row['path'],units=row['unit_count'],valid=row['valid_unit_count'],fixup_bytes=len(row['fixup']['changed_byte_addresses']))),flush=True)
    if len(sources)!=16:
        raise ValueError('Expected 16 source scenario candidates')
    report=dict(schema=1,executable_sha256=sha(exe),sources=sources,limits=['Load-time unit registry only; later scenario-start events and MOD overlays are not executed','No assumption that source file presence establishes active scenario or official identity','No runtime data imported; unknown fields remain original bytes'])
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    return report


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation',type=Path)
    p.add_argument('--output',type=Path,required=True)
    a=p.parse_args()
    audit(a.installation,a.output)
