#!/usr/bin/env python3
"""Decode all 850 native scenario actors and resolve known catalog identities.

Execute the pinned EXE's original 152-byte serializer in a bounded x86 VM.
This is not Wine or the game: the only host callback supplies record bytes.
Do not infer activation of SIRE, Backup or attachment files from their presence.
"""
import argparse
import csv
import hashlib
import json
import struct
from pathlib import Path

from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import (UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_EDI,
                              UC_X86_REG_ESI, UC_X86_REG_ESP, UC_X86_REG_EIP)
from inspect_pc_effect_bindings import EXE_SHA

ROOT = Path(__file__).resolve().parents[2]
BASE, STRIDE, NATIVE_COUNT = 17760, 152, 850


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


class NativeOfficerDecoder:
    def __init__(self, exe):
        if sha(exe) != EXE_SHA:
            raise ValueError('Executable changed; serializer must be reinspected')
        self.u = u = Uc(UC_ARCH_X86, UC_MODE_32)
        u.mem_map(0x400000, 0x500000)
        u.mem_write(0x400000, exe[:0x500000])
        self.actor, self.stream, self.stack, self.stop = 0x10000000, 0x10001000, 0x20001000, 0x30000000
        u.mem_map(self.actor, 8192)
        u.mem_map(self.stack - 4096, 8192)
        u.mem_map(self.stop, 4096)
        self.current, self.cursor, self.reads = b'', 0, []
        self.native_index = 0
        u.hook_add(UC_HOOK_CODE, self.read, begin=0x46ff20, end=0x46ff20)
        u.hook_add(UC_HOOK_CODE, self.identity, begin=0x4883c0, end=0x4883c0)

    def identity(self, u, address, size, user):
        # Supply only the containing array's index. Execute the EXE's actual
        # type-22 comparison at48b787; it skips every slot from850 onward.
        if u.reg_read(UC_X86_REG_ECX) != self.actor:
            raise ValueError('Unexpected actor identity receiver')
        sp = u.reg_read(UC_X86_REG_ESP)
        ret = struct.unpack('<I', u.mem_read(sp, 4))[0]
        u.reg_write(UC_X86_REG_EAX, self.native_index)
        u.reg_write(UC_X86_REG_ESP, sp + 4)
        u.reg_write(UC_X86_REG_EIP, ret)

    def read(self, u, address, size, user):
        sp = u.reg_read(UC_X86_REG_ESP)
        ret, destination, length = struct.unpack('<III', u.mem_read(sp, 12))
        if u.reg_read(UC_X86_REG_ECX) != self.stream or self.cursor + length > len(self.current):
            raise ValueError('Unexamined native IO request')
        if not (self.actor <= destination < self.actor + 4096 or self.stack - 4096 <= destination < self.stack + 4096):
            raise ValueError('Unexpected native read destination')
        u.mem_write(destination, self.current[self.cursor:self.cursor + length])
        # Native scalar helpers read into a temporary stack slot then sign-extend.
        self.reads.append([self.cursor, length])
        self.cursor += length
        u.reg_write(UC_X86_REG_EAX, 1)
        u.reg_write(UC_X86_REG_ESP, sp + 12)
        u.reg_write(UC_X86_REG_EIP, ret)

    def decode(self, raw, native_index=0, versions=(1, 2)):
        if len(raw) != STRIDE:
            raise ValueError('Truncated actor record')
        self.current, self.cursor, self.reads = raw, 0, []
        self.native_index = native_index
        u = self.u
        u.mem_write(self.actor, bytes(4096))
        u.mem_write(self.actor, struct.pack('<I', 0x79c780))
        u.mem_write(self.stream, bytes(4096))
        u.mem_write(self.stream + 8, struct.pack('<I', 1))
        u.mem_write(self.stream + 0x54, struct.pack('<I', 22))
        # Original header reader43b330 maps file24/28 to stream58/5c.
        # These versions change other serializers, even if actor payloads agree.
        u.mem_write(self.stream + 0x58, struct.pack('<II', *versions))
        u.reg_write(UC_X86_REG_ESI, self.stream)
        u.reg_write(UC_X86_REG_EDI, self.actor)
        u.reg_write(UC_X86_REG_ESP, self.stack)
        u.emu_start(0x48b775, 0x48bb28, count=40000)
        if self.cursor == 0 and u.reg_read(UC_X86_REG_EIP) == 0x48bb28:
            raise ValueError('Actor slot not serialized by the original scenario branch')
        if u.reg_read(UC_X86_REG_EIP) != 0x48bb28 or self.cursor != STRIDE:
            raise ValueError('Native serializer did not consume exactly one record')
        actor = bytes(u.mem_read(self.actor, 0x16c))
        parts = [actor[4:9].split(b'\0')[0], actor[9:14].split(b'\0')[0]]
        try:
            name = ''.join(part.decode('big5') for part in parts)
            name_error = None
        except UnicodeDecodeError:
            # Original gaiji/custom fonts need a separately evidenced table.
            # Do not replace or normalize unknown glyphs into a guessed identity.
            name, name_error = None, 'unmapped Big5 bytes; source font/gaiji mapping required'
        face, sex, appearance, birth, death = struct.unpack_from('<5i', actor, 0x3c)
        if face != struct.unpack_from('<h', raw, 53)[0] or sex != struct.unpack_from('<b', raw, 55)[0]:
            raise ValueError('Native signed face/sex decoding changed')
        return dict(name=name, name_bytes=[part.hex() for part in parts], name_error=name_error,
                    face_id=face, sex=sex, appearance=appearance,
                    birth=birth, death=death, age_change=actor[0x120],
                    stats=list(actor[0xc8:0xcd]), aptitude_codes=list(struct.unpack_from('<6i', actor, 0xb0)),
                    skill_native_id=struct.unpack_from('<i', actor, 0xe8)[0]), actor


def audit(installation, output):
    installation, output = installation.resolve(), output.resolve()
    if output == installation or installation in output.parents:
        raise ValueError('Output must not be inside the read-only installation')
    exe = (installation / 'san11pk.exe').read_bytes()
    decoder = NativeOfficerDecoder(exe)
    catalog_path = ROOT / 'core/src/main/resources/content/officers.tsv'
    catalog = list(csv.DictReader(catalog_path.open(), delimiter='\t'))
    ids = [int(row['sourceId']) for row in catalog]
    if len(catalog) != 670 or set(ids) != set(range(670)):
        raise ValueError('Catalog scope changed; review identity coverage')
    identities = {}
    by_native = {int(row['sourceId']): row for row in catalog}
    for row in catalog:
        identity = (row['name'], int(row['birth']), 1 if row['gender'] == '女' else 0)
        identities.setdefault(identity, []).append(row)
    sources, records, mappings, differences = [], [], [], []
    paths = sorted((installation / 'Media/scenario').glob('*'), key=lambda p: p.name.lower())
    for path in paths:
        if not path.name.lower().startswith('scen0') or path.suffix.lower() != '.s11':
            continue
        raw = path.read_bytes()
        if len(raw) != 170010 or raw[:18] != bytes.fromhex('0000feff16000000') + b'KOEI%SAN11':
            raise ValueError('Unexamined installed scenario format: ' + path.name)
        source = path.relative_to(installation).as_posix()
        sources.append(dict(path=source, bytes=len(raw), sha256=sha(raw),
                            versions=list(struct.unpack_from('<II', raw, 24)),
                            activation='installed Media/scenario candidate; MOD overrides unresolved'))
        for native in range(NATIVE_COUNT):
            row = by_native.get(native)
            offset = BASE + STRIDE * native
            record = raw[offset:offset + STRIDE]
            decoded, actor = decoder.decode(record, native, struct.unpack_from('<II', raw, 24))
            mismatches = {key: dict(project=value, native=decoded[key]) for key, value in
                          [('name', row['name']), ('birth', int(row['birth'])),
                           ('sex', 1 if row['gender'] == '女' else 0)] if decoded[key] != value} if row else {}
            candidates = identities.get((decoded['name'], decoded['birth'], decoded['sex']), [])
            resolved = candidates[0] if len(candidates) == 1 else None
            mapping = dict(source=source, native_index=native, candidate_project_id=int(row['id']) if row else None,
                           project_id=int(resolved['id']) if resolved else None,
                           identity=('name_birth_sex_verified' if not mismatches else 'relocated_identity_verified')
                           if resolved else 'quarantined' if row else 'no_project_catalog_identity', mismatches=mismatches)
            mappings.append(mapping)
            for key in ('appearance', 'death'):
                if resolved and decoded[key] != int(resolved[key]):
                    differences.append(dict(source=source, native_index=native, project_id=int(resolved['id']),
                                            field=key, project=int(resolved[key]), native=decoded[key]))
            if resolved:
                for key, expected in [('stats', list(map(int, resolved['stats'].split(',')))),
                                      ('aptitude_codes', ['CBAS'.index(c) for c in resolved['aptitudes']])]:
                    if decoded[key] != expected:
                        differences.append(dict(source=source, native_index=native, project_id=int(resolved['id']),
                                                field=key, project=expected, native=decoded[key]))
            records.append(dict(source=source, native_index=native, offset=offset, bytes=STRIDE,
                                sha256=sha(record), raw_hex=record.hex(), decoded=decoded,
                                actor_hex=actor.hex()))
    if len(sources) != 16:
        raise ValueError('Expected the 16 previously inventoried installed scenarios')
    for source in sources:
        mapped = [r['project_id'] for r in mappings if r['source'] == source['path'] and r['project_id'] is not None]
        if len(mapped) != len(set(mapped)):
            raise ValueError('Ambiguous duplicate project identity in ' + source['path'])
    report = dict(schema=1, source_executable_sha256=EXE_SHA, catalog_sha256=sha(catalog_path.read_bytes()),
                  scope='850 native actor slots x 16 installed scenarios; not full scenario import',
                  serializer=dict(start='48b775', stop='48bb28', source_type=22, bytes=STRIDE,
                                  sha256=sha(exe[0x8b775:0x8bb28]), io_reads=decoder.reads,
                                  count=NATIVE_COUNT, count_evidence='48b787 compares original registry index with0x352; slots>=850 are skipped',
                                  identity_hook='4883c0 supplies array index only; native type/count dispatch unchanged'),
                  header_evidence='43b330: file24/28 map to stream58/5c; preserved per source',
                  field_evidence=dict(stats='48b93e..48b947 -> actor c8, five byte values; catalog vector cross-check',
                                      aptitude_codes='48b923..48b93c -> actor b0, six signed byte-to-int values; C/B/A/S=0/1/2/3 catalog cross-check',
                                      skill_native_id='48b973..48b981 -> actor e8; raw enum only, no assumed project skill mapping'),
                  sources=sources, mappings=mappings, differences=differences, records=records,
                  limits=['Native record-table base inherited from verified six-person evidence; all850 serialized slots included',
                          'Additional native actors may be event/ancient/reserved; existence does not establish playable or appearing status',
                          'Ability and aptitude field order is corroborated against catalog vectors; PC gameplay consumers not yet traced',
                          'Other field meanings and native skill-to-project mapping remain unknown; raw bytes retained',
                          'Identity match does not prove abilities, relations, scenario membership or activation order',
                          'No gameplay resources changed; no attachment or Backup treated as active; no Wine'])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    summary = dict(records=len(records), identity_verified=sum(m['project_id'] is not None for m in mappings),
                   relocated=sum(m['identity'] == 'relocated_identity_verified' for m in mappings),
                   quarantined=sum(m['identity'] == 'quarantined' for m in mappings),
                   outside_project_catalog=sum(m['identity'] == 'no_project_catalog_identity' for m in mappings),
                   field_differences=len(differences),
                   source_executable_sha256=EXE_SHA, output_sha256=sha(output.read_bytes()))
    print(json.dumps(summary, ensure_ascii=False))
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    audit(args.installation, args.output)
