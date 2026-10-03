#!/usr/bin/env python3
"""Decode native scenario metadata and prove the actor-table start via original loops."""
import argparse
import json
import struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_EBX, UC_X86_REG_EBP, UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP
from inspect_pc_scenario_domains import NativeDomainDecoder, scenario_header
from inspect_pc_scenario_officers import BASE, sha
from inspect_pc_effect_bindings import EXE_SHA


class NativeMetadataDecoder(NativeDomainDecoder):
    def __init__(self, exe):
        super().__init__(exe)
        self.meta = self.stream + 0x1000
        self.u.mem_map(self.meta, 0x5000)

    def call(self, function, receiver, argument=None):
        u = self.u
        u.reg_write(UC_X86_REG_ECX, receiver)
        u.reg_write(UC_X86_REG_ESP, self.stack)
        u.mem_write(self.stack, struct.pack('<I', self.stop) + (struct.pack('<I', argument) if argument is not None else b''))
        u.emu_start(function, self.stop, count=1000000)
        if u.reg_read(UC_X86_REG_EIP) != self.stop:
            raise ValueError('Native metadata call did not return')

    def begin(self, kind, address, stride):
        self.finish_record()
        self.records.append(dict(kind=kind, actor_address=address, actor_stride=stride, offset=self.cursor, reads=[]))

    def decode_metadata(self, raw):
        versions = scenario_header(raw)
        u = self.u
        u.mem_write(0x7200000, bytes(0x300000))
        u.mem_write(self.meta, bytes(0x5000))
        u.mem_write(self.stream, bytes(4096))
        u.mem_write(self.stream + 8, struct.pack('<I', 1))
        u.mem_write(self.stream + 0x54, struct.pack('<III', 22, *versions))
        self.raw, self.cursor, self.records = raw, 90, []
        self.call(0x480830, self.meta)
        self.begin('scenario_metadata', self.meta, 0x3f6a)
        self.call(0x480d50, self.meta, self.stream)
        if u.reg_read(UC_X86_REG_EAX) != 1 or self.cursor != 16183:
            raise ValueError('Native scenario metadata boundary changed')
        actor = bytes(u.mem_read(self.meta, 0x3f6a))
        name_raw = actor[0x14:0x25].split(b'\0')[0]
        description_raw = actor[0x25:0x190].split(b'\0')[0]
        decoded = dict(native_id=struct.unpack_from('<i', actor, 4)[0],
                       date=list(struct.unpack_from('<3i', actor, 8)),
                       name=name_raw.decode('big5'), description=description_raw.decode('big5'),
                       name_bytes=name_raw.hex(), description_bytes=description_raw.hex())
        self.call(0x4830b0, self.root)
        self.begin('global_header', self.root, 0x1d8)
        self.call(0x483120, self.root, self.stream)
        if self.cursor != 16194:
            raise ValueError('Native global header boundary changed')
        for index in range(16384):
            self.call(0x4880a0, self.root + 0x89730 + 0x38 * index)
        self.begin('grid', self.root + 0x89730, 0x38 * 16384)
        u.reg_write(UC_X86_REG_EBX, self.root)
        u.reg_write(UC_X86_REG_EBP, self.stream)
        u.reg_write(UC_X86_REG_ESP, self.stack)
        u.emu_start(0x4937ee, 0x493812, count=20000000)
        if u.reg_read(UC_X86_REG_EIP) != 0x493812 or self.cursor != BASE:
            raise ValueError('Native actor table start changed')
        self.finish_record()
        for row in self.records:
            memory = bytes(u.mem_read(row['actor_address'], row['actor_stride']))
            row['actor_sha256'] = sha(memory)
            if row['kind'] != 'grid':
                row['actor_hex'] = memory.hex()
        return dict(start=90, end=self.cursor, versions=list(versions), decoded=decoded, records=self.records)


def audit(installation, output):
    installation, output = installation.resolve(), output.resolve()
    if output == installation or installation in output.parents:
        raise ValueError('Output must not be inside the read-only installation')
    decoder = NativeMetadataDecoder((installation / 'san11pk.exe').read_bytes())
    sources = []
    for path in sorted((installation / 'Media/scenario').glob('*'), key=lambda p: p.name.lower()):
        if path.name.lower().startswith('scen0') and path.suffix.lower() == '.s11':
            raw = path.read_bytes()
            sources.append(dict(path=path.relative_to(installation).as_posix(), sha256=sha(raw), **decoder.decode_metadata(raw)))
    if len(sources) != 16:
        raise ValueError('Expected 16 installed scenario files')
    report = dict(schema=1, source_executable_sha256=EXE_SHA, sources=sources,
                  evidence=dict(loader='43b9b5 ctor480830 ->480d50 then4937b0',
                                metadata='480ca0..480d49: actor4 id,8 year,c month,10 day,14 name17,25 description363',
                                metadata_tail='480d50 city metadata42 records retained without guessing field meanings',
                                original_boundaries=[90, 16183, 16194, BASE],
                                grid='4937ee..493812 executes16384 original receiver serializers, consumes1566 bytes',
                                scalar_versions='file24/28 -> stream58/5c; header90 derived from43b330'),
                  limits=['Installed metadata only; launcher/MOD activation unresolved',
                          'Grid1566-byte payload and city metadata fields retained; meanings not inferred',
                          'No gameplay resource overwritten, no Wine, no PC installation writes'])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(dict(scenarios=len(sources), sha256=sha(output.read_bytes()),
                          entries=[dict(path=s['path'], **s['decoded']) for s in sources]), ensure_ascii=False))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    audit(args.installation, args.output)
