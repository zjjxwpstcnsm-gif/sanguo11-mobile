#!/usr/bin/env python3
"""Read installed scenario items/forces/districts/cities/gates/ports using the EXE.

Offline bounded execution only: no Wine, PC writes, host game or injected rules.
Unknown field semantics remain raw bytes. This is an audit, not a game importer.
"""
import argparse
import csv
import json
import struct
from pathlib import Path

from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import (UC_X86_REG_EAX, UC_X86_REG_EBX, UC_X86_REG_EBP,
                              UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP)
from inspect_pc_scenario_officers import BASE, STRIDE, NATIVE_COUNT, ROOT, sha
from inspect_pc_effect_bindings import EXE_SHA

# Native constructor492db0 and registry getters490a10..490b30.
# kind, root offset, memory stride, count, constructor, serializer
GROUPS = (
    ('item', 0x7777c, 0x54, 100, 0x484c80, 0x484fb0),
    ('force', 0x7af8, 0x12c, 47, 0x481830, 0x481d30),
    ('district', 0xb20c, 0x50, 47, 0x47eb10, 0x47e4c0),
    ('city', 0x1d8, 0x248, 42, 0x47c880, 0x47c900),
    ('gate', 0x61a8, 0x90, 10, 0x4838e0, 0x48ddd0),
    ('port', 0x6748, 0x90, 35, 0x48dd70, 0x48ddd0),
)
TAIL_START = BASE + STRIDE * NATIVE_COUNT


def scenario_header(raw, shared=False):
    if len(raw) != (47928 if shared else 170010) or raw[:18] != struct.pack('<II', 0xfffe0000, 24 if shared else 22) + b'KOEI%SAN11':
        raise ValueError('Unexamined scenario format')
    versions = struct.unpack_from('<II', raw, 24)
    if versions != (1, 2):
        raise ValueError('Unexamined scenario version: ' + repr(versions))
    return versions


class NativeDomainDecoder:
    def __init__(self, exe):
        if sha(exe) != EXE_SHA:
            raise ValueError('Executable changed; serializers must be reinspected')
        self.u = u = Uc(UC_ARCH_X86, UC_MODE_32)
        u.mem_map(0x400000, 0x500000)
        u.mem_write(0x400000, exe[:0x500000])
        u.mem_map(0x7200000, 0x300000)
        u.mem_map(0, 4096)  # FS:0 used by the original constructor exception frame.
        self.stream, self.stack, self.stop, self.root = 0x10000000, 0x20010000, 0x30000000, 0x7201958
        u.mem_map(self.stream, 4096)
        u.mem_map(self.stack - 0x10000, 0x20000)
        u.mem_map(self.stop, 4096)
        # PE imports: EnterCriticalSection/LeaveCriticalSection. This isolated
        # single-thread VM has no competing threads. No game/allocator hooks.
        u.mem_write(self.stop + 0x100, bytes.fromhex('31c0c20400'))
        for slot in (0x74e360, 0x74e35c):
            u.mem_write(slot, struct.pack('<I', self.stop + 0x100))
        for slot in (0x74e268, 0x74e26c):
            # Verified PE imports IsBadReadPtr / IsBadWritePtr. Resolve against
            # VM memory permissions, never blindly declare a pointer valid.
            u.mem_write(slot, struct.pack('<I', self.stop + 0x200))
        u.hook_add(UC_HOOK_CODE, self.probe_pointer, begin=self.stop + 0x200, end=self.stop + 0x200)
        self.addresses = {}
        for kind, offset, stride, count, ctor, serializer in GROUPS:
            for index in range(count):
                self.addresses[self.root + offset + index * stride] = (kind, index, stride, serializer)
        u.hook_add(UC_HOOK_CODE, self.read, begin=0x46ff20, end=0x46ff20)
        for serializer in set(g[-1] for g in GROUPS):
            u.hook_add(UC_HOOK_CODE, self.enter, begin=serializer, end=serializer)
        self.raw, self.cursor, self.records = b'', TAIL_START, []

    def probe_pointer(self, u, address, size, user):
        sp = u.reg_read(UC_X86_REG_ESP)
        ret, pointer, length = struct.unpack('<III', u.mem_read(sp, 12))
        readable_writable = length == 0 or pointer != 0 and any(start <= pointer and pointer + length - 1 <= end and permissions & 3 == 3 for start, end, permissions in u.mem_regions())
        u.reg_write(UC_X86_REG_EAX, 0 if readable_writable else 1)
        u.reg_write(UC_X86_REG_ESP, sp + 12)
        u.reg_write(UC_X86_REG_EIP, ret)

    def scalar(self, function, receiver):
        u = self.u
        u.reg_write(UC_X86_REG_ECX, receiver)
        u.reg_write(UC_X86_REG_ESP, self.stack)
        u.mem_write(self.stack, struct.pack('<I', self.stop))
        u.emu_start(function, self.stop, count=100000)
        if u.reg_read(UC_X86_REG_EIP) != self.stop:
            raise ValueError('Native domain accessor did not return')
        value = u.reg_read(UC_X86_REG_EAX)
        return value if value < 0x80000000 else value - 0x100000000

    def enter(self, u, address, size, user):
        pointer = u.reg_read(UC_X86_REG_ECX)
        if pointer not in self.addresses:
            raise ValueError('Unexamined native serializer receiver')
        kind, index, stride, serializer = self.addresses[pointer]
        if address != serializer:
            raise ValueError('Unexpected native serializer')
        self.finish_record()
        self.records.append(dict(kind=kind, native_index=index, offset=self.cursor,
                                 actor_address=pointer, actor_stride=stride, serializer=hex(address), reads=[]))

    def finish_record(self):
        if self.records:
            row = self.records[-1]
            record = self.raw[row['offset']:self.cursor]
            row.update(bytes=len(record), sha256=sha(record), raw_hex=record.hex())

    def read(self, u, address, size, user):
        sp = u.reg_read(UC_X86_REG_ESP)
        ret, destination, length = struct.unpack('<III', u.mem_read(sp, 12))
        if u.reg_read(UC_X86_REG_ECX) != self.stream or not self.records or length <= 0 or self.cursor + length > len(self.raw):
            raise ValueError('Unexamined native IO request')
        row = self.records[-1]
        if not (row['actor_address'] <= destination and destination + length <= row['actor_address'] + row['actor_stride'] or
                self.stack - 0x10000 <= destination and destination + length <= self.stack + 0x10000):
            raise ValueError('Unexpected native IO destination')
        u.mem_write(destination, self.raw[self.cursor:self.cursor + length])
        row['reads'].append(dict(offset=self.cursor, bytes=length,
                                 destination=('actor+' + hex(destination - row['actor_address']))
                                 if destination >= row['actor_address'] and destination < row['actor_address'] + row['actor_stride'] else 'stack_scalar',
                                 return_address=hex(ret)))
        self.cursor += length
        u.reg_write(UC_X86_REG_EAX, 1)
        u.reg_write(UC_X86_REG_ESP, sp + 12)
        u.reg_write(UC_X86_REG_EIP, ret)

    def decode(self, raw, shared=False):
        versions = scenario_header(raw, shared)
        u = self.u
        # Reconstruct every object with the actual constructors; never copy one
        # prototype over embedded self-relative container pointers.
        u.mem_write(0x7200000, bytes(0x300000))
        for kind, offset, stride, count, ctor, serializer in GROUPS:
            for index in range(count):
                pointer = self.root + offset + index * stride
                u.reg_write(UC_X86_REG_ECX, pointer)
                u.reg_write(UC_X86_REG_ESP, self.stack)
                u.mem_write(self.stack, struct.pack('<I', self.stop))
                u.emu_start(ctor, self.stop, count=100000)
                if u.reg_read(UC_X86_REG_EIP) != self.stop:
                    raise ValueError('Native constructor did not return')
                vtable = struct.unpack('<I', u.mem_read(pointer, 4))[0]
                if struct.unpack('<I', u.mem_read(vtable + 0x30, 4))[0] != serializer:
                    raise ValueError('Native vtable serializer changed')
        if shared:
            # Type24 skips grid and officer data in the original dispatch.
            # Execute those loops too; construct every receiver rather than
            # assuming how many bytes an unexamined table would consume.
            for offset, stride, count, ctor in ((0xc0bc, 0x190, 1100, 0x489f10), (0x89730, 0x38, 16384, 0x4880a0)):
                for index in range(count):
                    u.reg_write(UC_X86_REG_ECX, self.root + offset + stride * index)
                    u.reg_write(UC_X86_REG_ESP, self.stack)
                    u.mem_write(self.stack, struct.pack('<I', self.stop))
                    u.emu_start(ctor, self.stop, count=100000)
                    if u.reg_read(UC_X86_REG_EIP) != self.stop:
                        raise ValueError('Native shared-table constructor did not return')
        self.raw, self.cursor, self.records = raw, 90 if shared else TAIL_START, []
        u.mem_write(self.stream, bytes(4096))
        u.mem_write(self.stream + 8, struct.pack('<I', 1))
        u.mem_write(self.stream + 0x54, struct.pack('<III', 24 if shared else 22, *versions))
        if shared:
            # Native base header has no type24 payload after the90-byte file
            # header. Execute it and reject any unexpected read.
            for function, args in ((0x4830b0, b''), (0x483120, struct.pack('<I', self.stream))):
                u.reg_write(UC_X86_REG_ECX, self.root)
                u.reg_write(UC_X86_REG_ESP, self.stack)
                u.mem_write(self.stack, struct.pack('<I', self.stop) + args)
                u.emu_start(function, self.stop, count=100000)
                if u.reg_read(UC_X86_REG_EIP) != self.stop or self.cursor != 90:
                    raise ValueError('Shared base header unexpectedly consumed payload')
        u.reg_write(UC_X86_REG_EBX, self.root)
        u.reg_write(UC_X86_REG_EBP, self.stream)
        u.reg_write(UC_X86_REG_ESP, self.stack)
        # Execute the original consecutive loops. Stop before unrelated tables.
        u.emu_start(0x4937ec if shared else 0x49383f, 0x49391c, count=20000000)
        if u.reg_read(UC_X86_REG_EIP) != 0x49391c:
            raise ValueError('Native domain loop did not finish')
        self.finish_record()
        expected = [(kind, index) for kind, _, _, count, _, _ in GROUPS for index in range(count)]
        if [(r['kind'], r['native_index']) for r in self.records] != expected:
            raise ValueError('Native table count/order changed')
        authority_before = bytes(u.mem_read(0x7200000, 0x300000))
        for row in self.records:
            row['actor_hex'] = bytes(u.mem_read(row['actor_address'], row['actor_stride'])).hex()
            if not shared and row['kind'] in ('city', 'gate', 'port'):
                row['decoded'] = dict(district_native_index=self.scalar(0x47c320 if row['kind'] == 'city' else 0x483810, row['actor_address']),
                                      force_native_index=self.scalar(0x47b2b0, row['actor_address']))
            elif not shared and row['kind'] == 'district':
                row['decoded'] = dict(force_native_index=self.scalar(0x65d6c0, row['actor_address']))
            elif not shared and row['kind'] == 'force':
                row['decoded'] = dict(name_officer_native_index=struct.unpack_from('<i', bytes.fromhex(row['actor_hex']), 4)[0])
        if authority_before != bytes(u.mem_read(0x7200000, 0x300000)):
            raise ValueError('Native ownership lookup mutated domain state')
        return dict(start=90 if shared else TAIL_START, end=self.cursor, versions=list(versions), records=self.records)


def audit(installation, output):
    installation, output = installation.resolve(), output.resolve()
    if output == installation or installation in output.parents:
        raise ValueError('Output must not be inside the read-only installation')
    decoder = NativeDomainDecoder((installation / 'san11pk.exe').read_bytes())
    shared_path = installation / 'Media/scenario/Scenario.s11'
    shared_raw = shared_path.read_bytes()
    shared_source = dict(path=shared_path.relative_to(installation).as_posix(), sha256=sha(shared_raw),
                         bytes=len(shared_raw), **decoder.decode(shared_raw, shared=True))
    catalog_path = ROOT / 'core/src/main/resources/content/sites.tsv'
    catalog = list(csv.DictReader(catalog_path.open(), delimiter='\t'))
    site_mappings = {}
    for row in shared_source['records']:
        kind = row['kind']
        if kind not in ('city', 'gate', 'port'):
            if row['bytes']:
                raise ValueError('Unexamined shared non-site payload')
            continue
        actor = bytes.fromhex(row['actor_hex'])
        name = actor[4:9 if kind == 'city' else 11].split(b'\0')[0].decode('big5')
        suffix = {'city': '', 'gate': '關', 'port': '港'}[kind]
        matches = [c for c in catalog if c['kind'] == kind and name in (c['name'], c['name'] + suffix)]
        if len(matches) != 1:
            raise ValueError('Shared name/kind does not uniquely map to project: ' + name)
        mapping = dict(source_name=name, project_name=matches[0]['name'], project_id=int(matches[0]['id']),
                       method='exact name and kind, with explicit optional 關/港 suffix')
        row['identity'] = mapping
        site_mappings[kind, row['native_index']] = mapping
    if len(site_mappings) != 87 or len({m['project_id'] for m in site_mappings.values()}) != 87:
        raise ValueError('Incomplete/duplicate shared site identity')
    sources = []
    for path in sorted((installation / 'Media/scenario').glob('*'), key=lambda p: p.name.lower()):
        if not path.name.lower().startswith('scen0') or path.suffix.lower() != '.s11':
            continue
        raw = path.read_bytes()
        sources.append(dict(path=path.relative_to(installation).as_posix(), sha256=sha(raw), bytes=len(raw),
                            **decoder.decode(raw)))
        for row in sources[-1]['records']:
            if (row['kind'], row['native_index']) in site_mappings:
                row['identity'] = site_mappings[row['kind'], row['native_index']]
    if len(sources) != 16:
        raise ValueError('Expected 16 installed scenarios')
    report = dict(schema=1, source_executable_sha256=EXE_SHA, sources=sources,
                  shared_source=shared_source, site_catalog_sha256=sha(catalog_path.read_bytes()),
                  evidence=dict(array_constructor='492db0', loop=['49383f', '49391c'],
                                header='43b330: file24/28 -> stream58/5c',
                                shared_header='90 bytes: marker4+type4+magic16+six int32+two21-byte fields; base483120 skips type24 payload',
                                shared_names='47c928 reads5 bytes to city+4;48ddf6 reads7 bytes to gate/port+4',
                                ownership='native47b2b0: site virtual44 -> district490ad0 -> virtual40;47c320/483810 read district;65d6c0 reads force',
                                force_name_officer='481640: force+4 -> actor registry490b00 -> name489690; no guessed ruler trait/state',
                                start='17760 + 850*152; actor count confirmed by48b787',
                                io='46ff20 supplies source bytes only',
                                imports={'74e360': 'EnterCriticalSection', '74e35c': 'LeaveCriticalSection',
                                         '74e268': 'IsBadReadPtr: check VM mapping/permissions',
                                         '74e26c': 'IsBadWritePtr: check VM mapping/permissions'}),
                  limits=['Installed candidates; MOD/launcher activation unresolved',
                          'Resource and remaining actor offsets are not assigned guessed field meanings',
                          '87 names and native district/force ownership resolved; resource and geography semantics still unknown',
                          'Units, facilities, scenario metadata and later tables remain to decode',
                          'No gameplay resource changed; no Wine; source installation read-only'])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(dict(scenarios=len(sources), records=sum(len(s['records']) for s in sources),
                          end_offsets=sorted(set(s['end'] for s in sources)), output_sha256=sha(output.read_bytes()))))
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    audit(args.installation, args.output)
