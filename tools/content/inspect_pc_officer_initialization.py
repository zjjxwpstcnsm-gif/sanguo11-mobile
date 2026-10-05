#!/usr/bin/env python3
"""Verify scenario-read initialization in the original 1100-officer loop.

Seed every actor with nonzero bytes before its real constructor and serializer.
No replacement initializer, identity hook, Wine or writes to the PC installation.
This is the loading boundary, before later scenario events or MOD processing.
"""
import argparse
import gzip
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_ESP
from inspect_pc_scenario_units import NativeScenarioUnitDecoder
from inspect_pc_scenario_officers import sha


class SeededDecoder(NativeScenarioUnitDecoder):
    def __init__(self, exe):
        super().__init__(exe)
        self.fill, self.initialized = 0xa5, []
        self.u.hook_add(UC_HOOK_CODE, self.initializer, begin=0x488470, end=0x488470)

    def scalar(self, function, receiver):
        if function == 0x489f10:
            self.u.mem_write(receiver, bytes([self.fill]) * 0x190)
        return super().scalar(function, receiver)

    def initializer(self, u, address, size, user):
        actor = u.reg_read(UC_X86_REG_ECX)
        offset = actor - self.root - 0xc0bc
        if offset % 0x190 or not 0 <= offset // 0x190 < 1100:
            raise ValueError('Unexpected original initializer receiver')
        argument = struct.unpack('<I', u.mem_read(u.reg_read(UC_X86_REG_ESP) + 4, 4))[0]
        if argument != 0:
            raise ValueError('Unexpected initializer argument')
        self.initialized.append(offset // 0x190)


def audit(installation, prior, output):
    installation = installation.resolve()
    if output.resolve() == installation or installation in output.resolve().parents:
        raise ValueError('PC installation is read-only')
    old = json.loads(gzip.decompress(prior.read_bytes()))
    exe = (installation / 'san11pk.exe').read_bytes()
    if sha(exe) != old['source_executable_sha256']:
        raise ValueError('Prior audit belongs to a different executable')
    decoder = SeededDecoder(exe)
    previous = {(r['source'], r['native_index']): r for r in old['records']}
    sources, control_runs = [], []
    for source_no, source in enumerate(old['sources']):
        raw = (installation / source['path']).read_bytes()
        if sha(raw) != source['sha256']:
            raise ValueError('Scenario changed: ' + source['path'])
        for fill in ((0xa5, 0, 0xff) if source_no == 0 else (0xa5,)):
            decoder.fill, decoder.initialized = fill, []
            rng_before = bytes(decoder.u.mem_read(0x8a5d44, 4))
            decoded = decoder.decode_units(raw)
            if decoder.initialized != list(range(1100)):
                raise ValueError('Every original actor must initialize exactly once in order')
            if bytes(decoder.u.mem_read(0x8a5d44, 4)) != rng_before:
                raise ValueError('Scenario registry load consumed RNG')
            vectors = []
            for index in range(1100):
                actor = bytes(decoder.u.mem_read(decoder.root + 0xc0bc + index * 0x190, 0x190))
                experience = struct.unpack_from('<5H', actor, 0x12a)
                injury = struct.unpack_from('<i', actor, 0x15c)[0]
                if experience != (0, 0, 0, 0, 0) or injury != 0:
                    raise ValueError('Unexpected load-boundary experience/injury')
                if index < 850:
                    prior_actor = bytes.fromhex(previous[(source['path'], index)]['actor_hex'])
                    # Compare source-backed fields only. Other initialized defaults
                    # intentionally differ from the old zero-filled partial decoder.
                    for start, end in ((4, 14), (0x3c, 0x50), (0x60, 0x64),
                                       (0xa4, 0xa8), (0xc8, 0xcd), (0xd0, 0xe4)):
                        if actor[start:end] != prior_actor[start:end]:
                            raise ValueError('Source-backed field differs after real initialization')
                vectors.append(list(experience) + [injury])
            row = dict(path=source['path'], source_sha256=source['sha256'], fill=fill,
                       initialized_count=len(decoder.initialized), serialized_count=850,
                       vectors_sha256=sha(json.dumps(vectors, separators=(',', ':')).encode()),
                       registry_sha256=decoded['officers']['registry_sha256'], rng_unchanged=True)
            (sources if fill == 0xa5 else control_runs).append(row)
            print(json.dumps(row), flush=True)
    if len(sources) != 16:
        raise ValueError('Expected all16 audited sources')
    functions = [('serializer_read_entry', 0x48b760, 0x48b775),
                 ('officer_initializer', 0x488470, 0x4886d9),
                 ('officer_load_loop', 0x493812, 0x49383f)]
    report = dict(schema=1, source_executable_sha256=sha(exe), prior_sha256=sha(prior.read_bytes()),
                  functions=[dict(name=n, address=hex(a), end_exclusive=hex(b),
                                  sha256=sha(exe[a-0x400000:b-0x400000])) for n,a,b in functions],
                  sources=sources, sentinel_control_runs=control_runs,
                  initialized_source_slots=17600, serialized_source_slots=13600,
                  experience_default=[0]*5, injury_default=0,
                  evidence='493812 loop ->48b760 reading flag+8=1 ->vtable+20 ->488470 before source fields',
                  limits=['Defaults belong to original loading boundary, not explicit source record fields',
                          'Later scenario events, MOD hooks and full new-game application flow not executed',
                          'Actor padding may retain sentinel bytes; no claim of whole-object zero initialization',
                          'Runtime content and user saves are unchanged'])
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    return report


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--installation', type=Path, required=True)
    p.add_argument('--prior', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    audit(a.installation, a.prior, a.output)
