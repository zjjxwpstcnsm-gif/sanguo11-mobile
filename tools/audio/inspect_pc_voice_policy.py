#!/usr/bin/env python3
"""Execute original actor voice selectors without commands, rule RNG or Wine.

Actor validity and the already-calculated ability getter are explicit read-only
boundary shims. All selector, type and profile table instructions run unchanged.
Raw arguments remain raw; this probe does not invent mobile event bindings.
"""
import argparse
import gzip
import hashlib
import itertools
import json
from pathlib import Path
import struct

from capstone import Cs, CS_ARCH_X86, CS_MODE_32
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP

EXE_SHA = '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
RANGES = [(0x4d0010, 0x4d00d0), (0x4cffd0, 0x4d000c),
          (0x4d1290, 0x4d13a1), (0x4d13b0, 0x4d148e), (0x4d1490, 0x4d156b),
          (0x4882d0, 0x4882e6), (0x4d19d0, 0x4d1a32),
          (0x4d1a40, 0x4d1a95), (0x4d1aa0, 0x4d1b14)]


def inspect(installation, output):
    if output.exists() or installation.resolve() in output.resolve().parents:
        raise ValueError('Fresh output outside read-only installation required')
    raw = (installation / 'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest() != EXE_SHA:
        raise ValueError('Changed executable requires new source audit')
    pe = struct.unpack_from('<I', raw, 60)[0]
    base = struct.unpack_from('<I', raw, pe + 52)[0]
    sections = []
    for index in range(struct.unpack_from('<H', raw, pe + 6)[0]):
        at = pe + 24 + struct.unpack_from('<H', raw, pe + 20)[0] + 40 * index
        virtual, va, size, offset = struct.unpack_from('<4I', raw, at + 8)
        sections.append((base + va, size, offset, raw[at:at+8].rstrip(b'\0')))

    def read(address, size):
        for start, length, offset, _ in sections:
            if start <= address and address + size <= start + length:
                return raw[offset+address-start:offset+address-start+size]
        raise ValueError('Unmapped PE range: ' + hex(address))

    u = Uc(UC_ARCH_X86, UC_MODE_32)
    # Load PE sections by their virtual addresses, never assume file offset=RVA.
    spans = []
    for start, size, offset, _ in sections:
        low, high = start & ~4095, (start + size + 4095) & ~4095
        if spans and low <= spans[-1][1]:
            spans[-1] = (spans[-1][0], max(high, spans[-1][1]))
        else:
            spans.append((low, high))
    for low, high in spans:
        u.mem_map(low, high - low)
    for start, size, offset, _ in sections:
        u.mem_write(start, raw[offset:offset+size])
    actor, manager, stack, stop = 0x10000000, 0x10002000, 0x20000000, 0x30000000
    u.mem_map(actor, 0x4000)
    u.mem_map(stack, 0x2000)
    u.mem_map(stop, 4096)
    validity, abilities, observed = 1, (50, 50, 50, 50), []

    def readonly_boundary(machine, address, size, user):
        sp = machine.reg_read(UC_X86_REG_ESP)
        ret, argument = struct.unpack('<2I', machine.mem_read(sp, 8))
        if address == 0x47a600:
            if argument != actor:
                raise ValueError('Unexpected actor identity pointer')
            result, consumed = validity, 0  # cdecl, caller removes actor argument
        else:
            if machine.reg_read(UC_X86_REG_ECX) != actor or argument >= 4:
                raise ValueError('Unexpected current ability getter')
            observed.append(argument)
            result, consumed = abilities[argument], 4
        machine.reg_write(UC_X86_REG_EAX, result)
        machine.reg_write(UC_X86_REG_ESP, sp + 4 + consumed)
        machine.reg_write(UC_X86_REG_EIP, ret)

    for address in (0x47a600, 0x489030):
        u.hook_add(UC_HOOK_CODE, readonly_boundary, begin=address, end=address)

    def call(address, args):
        observed.clear()
        sp = stack + 4096
        u.mem_write(sp, struct.pack('<'+'I'*(len(args)+1), stop, *[x & 0xffffffff for x in args]))
        u.reg_write(UC_X86_REG_ESP, sp)
        u.reg_write(UC_X86_REG_ECX, manager)
        u.emu_start(address, stop, count=1000)
        if u.reg_read(UC_X86_REG_EIP) != stop or u.reg_read(UC_X86_REG_ESP) != sp+4+4*len(args):
            raise ValueError('Native selector did not return with exact stdcall stack')
        return struct.unpack('<i', struct.pack('<I', u.reg_read(UC_X86_REG_EAX)))[0]

    types = []
    for kind in range(8):
        u.mem_write(manager+0x200, bytes(12))
        call(0x4d0010, [manager+0x200, kind])
        types.append(list(struct.unpack('<3i', u.mem_read(manager+0x200, 12))))
    bases = list(struct.unpack('<71i', read(0x7f1420, 71*4)))
    tables = {hex(a): list(struct.unpack('<16i', read(a, 64))) for a in (0x7f1360, 0x7f13a0, 0x7f13e0)}
    rows, checks = [], 8
    # Equalities and unsigned-byte extrema matter: strict martial superiority only.
    patterns = [(50,50,50,50),(49,50,49,49),(50,50,49,49),(49,50,50,49),
                (49,50,49,50),(51,50,49,49),(49,50,51,49),(49,50,49,51),
                (0,255,0,0),(255,0,255,255)]
    for profile, kind in itertools.product(range(71), range(8)):
        first, second, override = types[kind]
        u.mem_write(actor+0x100, struct.pack('<i', kind))
        for abilities in patterns:
            variant = tables['0x7f1360'][2*(first+4*second)+int(abilities[1]>max(abilities[0],abilities[2],abilities[3]))]
            expected = bases[profile]+variant
            actual = call(0x4d1290, [profile, actor])
            if actual != expected:
                raise ValueError(('Ability selector differs', profile, kind, abilities, actual, expected))
            rows.append(dict(selector='4d1290', profile=profile, voiceTypeRaw=kind,
                             abilityRaw=list(abilities), getterIndices=list(observed), variant=variant, voiceId=actual))
            checks += 1
        for address, table in ((0x4d13b0, '0x7f13a0'), (0x4d1490, '0x7f13e0')):
            for feedback in (0,1):
                variant = override if 0 <= override < 14 else tables[table][2*(first+4*second)+feedback]
                actual = call(address, [profile, actor, feedback])
                if actual != bases[profile]+variant or observed:
                    raise ValueError('Feedback selector differs or reads an ability')
                rows.append(dict(selector=hex(address)[2:], profile=profile, voiceTypeRaw=kind,
                                 feedbackRaw=feedback, variant=variant, voiceId=actual))
                checks += 1
    boundaries = []
    for address in (0x4d1290, 0x4d13b0, 0x4d1490):
        for profile, kind, validity in itertools.product((-1,0,70,71), (-1,0,7,8), (0,1)):
            u.mem_write(actor+0x100, struct.pack('<i', kind))
            feedbacks = (-1,0,1,2) if address != 0x4d1290 else (None,)
            for feedback in feedbacks:
                args = [profile, actor] + ([] if feedback is None else [feedback])
                actual = call(address, args)
                valid = 0 <= profile < 71 and 0 <= kind < 8 and validity and (feedback is None or feedback in (0,1))
                if (actual >= 0) != bool(valid):
                    raise ValueError(('Native invalid boundary', hex(address), args, kind, validity, actual))
                boundaries.append(dict(selector=hex(address)[2:], profile=profile, voiceTypeRaw=kind,
                                       actorValidRaw=validity, feedbackRaw=feedback, voiceId=actual))
                checks += 1
    # Scan valid decoded instruction boundaries, retaining candidates without role guesses.
    xrefs = []
    md = Cs(CS_ARCH_X86, CS_MODE_32)
    md.skipdata = True
    destinations = {0x4d19d0, 0x4d1a40, 0x4d1aa0}
    for start, size, offset, name in sections:
        if name != b'.text':
            continue
        for ins in md.disasm(raw[offset:offset+size], start):
            if ins.mnemonic == 'call' and ins.op_str.startswith('0x') and int(ins.op_str,16) in destinations:
                xrefs.append(dict(callAddress=hex(ins.address), target=ins.op_str,
                                  contextHex=read(ins.address-16, 37).hex(), status='CALL_SITE_ROLE_PENDING'))
    report = dict(schema=1, sourceExecutableSha256=EXE_SHA, nativeChecks=checks,
                  policy='ORIGINAL_INSTRUCTIONS_READ_ONLY_NO_WINE_NO_COMMAND_NO_RULE_RNG',
                  actorFieldOffsetHex='0x100', voiceTypes=types, profileBaseVoiceIds=bases,
                  abilityVariantTable=tables['0x7f1360'], feedbackVariantTables=[tables['0x7f13a0'],tables['0x7f13e0']],
                  rows=rows, boundaries=boundaries, wrapperCallCandidates=xrefs,
                  codeSha256={hex(a):hashlib.sha256(read(a,b-a)).hexdigest() for a,b in RANGES},
                  shims=[dict(address='47a600', purpose='Explicit actor validity, not original world validity'),
                         dict(address='489030', purpose='Explicit already-calculated unsigned byte current ability values, not ability calculation')],
                  limits=['71 profiles are raw voice profiles; their action/event names are not inferred.',
                          'Actor source voiceType offset is hexadecimal0x100, not decimal100.',
                          'Feedback0/1 semantics require each actual caller, not generic success/failure inference.',
                          'No voice playback, mobile GameEvent binding, speaker identity or normal scene acceptance.'])
    output.parent.mkdir(parents=True, exist_ok=True)
    data = (json.dumps(report, ensure_ascii=False, separators=(',',':'))+'\n').encode()
    output.write_bytes(gzip.compress(data, mtime=0))
    print(json.dumps(dict(result='PASS', nativeChecks=checks, rows=len(rows), callers=len(xrefs), voiceTypes=types)))


if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', type=Path, required=True)
    a=p.parse_args()
    inspect(a.installation, a.output)
