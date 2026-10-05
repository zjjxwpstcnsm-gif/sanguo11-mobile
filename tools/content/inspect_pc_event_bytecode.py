#!/usr/bin/env python3
"""Original event bytecode container/getters; no instruction execution.

Reads the exact lazy-body spans established by the original event manager.
Game/UI handlers, event conditions and effects are never called here.
"""
import argparse
import gzip
import json
import struct
from pathlib import Path
from inspect_pc_layered_scenario import NativeLayeredWorld
from pc_startup_platform import StartupPlatform
from audit_pc_restoration_sources import EXE_SHA, output_guard, json_bytes, sha


class BytecodePlatform(StartupPlatform):
    def __init__(self, world, installation):
        self.original_mb_ready = False
        super().__init__(world, installation)
        self.original_mb_ready = True

    def callback(self, u, address, size, user):
        if self.callbacks[address][0] == 'ascii_mbscmp' and self.original_mb_ready:
            # This executes the pinned CRT709c0e byte/lead-table comparison,
            # after actual CRT thread and CP950 initialization. It is not the
            # Windows NLS collation required by the game's Chinese name sort.
            return
        return super().callback(u, address, size, user)


def decode_program(world, pointer, raw):
    """Call original header binding and native accessor dispatch."""
    if len(raw) < 40 or struct.unpack_from('<I', raw)[0] != 0x57bf3:
        raise ValueError('Unexamined original event bytecode header')
    if struct.unpack_from('<II', raw, 4) != (72, 0):
        raise ValueError('Unexamined original event bytecode header version/flags')
    world.u.mem_write(pointer, raw)
    world.call(0x658e40, pointer, receiver=0x9c41450)
    code_pointer, count = struct.unpack('<2I', world.u.mem_read(0x9c41454, 8))
    string_data, string_offsets, string_count = struct.unpack('<3I', world.u.mem_read(0x9c41460, 12))
    if not pointer <= code_pointer <= pointer + len(raw) or not 0 <= count <= 10000 or code_pointer + count * 12 > pointer + len(raw):
        raise ValueError('Original instruction range escaped source body')
    if not 0 <= string_count <= 10000 or not pointer <= string_offsets <= pointer + len(raw) or string_offsets + string_count * 4 > pointer + len(raw):
        raise ValueError('Original string offset array escaped source body')
    if not pointer <= string_data <= pointer + len(raw):
        raise ValueError('Original string data escaped source body')
    rows = []
    for index in range(count):
        instruction = world.call(0x659ae0, index, receiver=0x9c41450)
        active = bool(world.call(0x659b30, receiver=instruction))
        fields = [world.call(function, receiver=instruction) for function in (0x659b50, 0x659b60, 0x659b70)]
        original = bytes(world.u.mem_read(code_pointer + index * 12, 12))
        if struct.unpack('<3I', original) != tuple(fields):
            raise ValueError('Original instruction getter differs from actual source span')
        rows.append(dict(index=index, sourceOffset=code_pointer - pointer + index * 12,
                         opcode=fields[0], operand1=fields[1], operand2=fields[2],
                         nativeOpcodeValid=active, rawHex=original.hex(), executed=False))
    strings = []
    for index in range(string_count):
        encoded_pointer = world.call(0x659ba0, index, receiver=0x9c4145c)
        if not string_data <= encoded_pointer < pointer + len(raw):
            raise ValueError('Original string accessor escaped body')
        encoded = bytes(world.u.mem_read(encoded_pointer, pointer + len(raw) - encoded_pointer))
        if b'\0' not in encoded:
            raise ValueError('Original event string has no source terminator')
        encoded = encoded.split(b'\0')[0]
        decoded_pointer = world.call(0x659bd0, index, receiver=0x9c4145c)
        decoded = b'' if not decoded_pointer else bytes(world.u.mem_read(decoded_pointer, len(encoded) + 1)).split(b'\0')[0]
        try:
            text = decoded.decode('big5')
            unknown = None
        except UnicodeDecodeError:
            text = None
            unknown = 'original game gaiji/encoding unresolved; raw bytes retained'
        strings.append(dict(index=index, sourceOffset=encoded_pointer - pointer,
                            encodedHex=encoded.hex(), decodedHex=decoded.hex(), text=text,
                            unknown=unknown, originalGetter='659bd0'))
    return dict(headerHex=raw[:40].hex(), originalHeaderBinding='658e40',
                codeSourceOffset=code_pointer - pointer, instructionCount=count,
                stringOffsetsSourceOffset=string_offsets - pointer, stringDataSourceOffset=string_data - pointer,
                instructions=rows, strings=strings, conditionOrEffectExecuted=False)


def inspect(installation, input_report, output):
    installation = installation.resolve()
    output_guard(installation, output)
    packed = input_report.read_bytes()
    report = json.loads(gzip.decompress(packed))
    if report['sourceExecutableSha256'] != EXE_SHA or report['completeStartup']:
        raise ValueError('Unexamined event-manager provenance')
    world = NativeLayeredWorld((installation / 'san11pk.exe').read_bytes())
    platform = BytecodePlatform(world, installation)
    world.call(0x73fd70)
    pointer = platform.allocate(0x10000)
    before = bytes(world.u.mem_read(0x7200000, 0x300000))
    rng = bytes(world.u.mem_read(0x8a5d44, 4))
    programs = []
    for record in report['installedEventAssembly']['originalLazyBodyReferences']:
        source = (installation / record['sourcePath']).read_bytes()
        if sha(source) != record['sourceSha256']:
            raise ValueError('Original event file changed')
        raw = source[record['sourceFileOffset']:record['sourceFileOffset'] + record['storedBodyBytes']]
        if sha(raw) != record['bodySha256'] or raw.hex() != record['bodyHex']:
            raise ValueError('Original lazy body span differs')
        if len(raw) > 0x10000:
            raise ValueError('Unexamined event bytecode size')
        body = decode_program(world, pointer, raw)
        metadata = bytes.fromhex(record['metadataHex'])
        condition = decode_program(world, pointer, metadata[0x40:])
        programs.append(dict(stableId=record['stableId'], sourcePath=record['sourcePath'], sourceSha256=record['sourceSha256'],
            sourceFileOffset=record['sourceFileOffset'], section=record['section'], originalManagerIndex=record['originalManagerIndex'],
            body=body, condition=condition, completeOpening=False, activePriorityProven=False))
        if len(programs) % 50 == 0:
            print(json.dumps(dict(programs=len(programs))), flush=True)
    if before != bytes(world.u.mem_read(0x7200000, 0x300000)) or rng != bytes(world.u.mem_read(0x8a5d44, 4)):
        raise ValueError('Bytecode accessor changed native world or RNG')
    result = dict(schema=1, sourceExecutableSha256=EXE_SHA, nativeEventManagerReportSha256=sha(packed),
                  completeStartup=False, conditionOrEffectExecuted=False, nativeWorldAndRngUnchanged=True,
                  programs=programs, platformCalls=platform.calls,
                  platformBoundary='Original CRT709c0e executes after thread/CP950 initialization; Windows Chinese NLS collation remains unsupported')
    payload = json_bytes(result)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(gzip.compress(payload, mtime=0))
    output.with_suffix('.summary.json').write_bytes(json_bytes(dict(schema=1, packedSha256=sha(output.read_bytes()),
        decodedSha256=sha(payload), programs=len(programs), completeOpening=False,
        bodyInstructions=sum(p['body']['instructionCount'] for p in programs),
        conditionInstructions=sum(p['condition']['instructionCount'] for p in programs),
        strings=sum(len(p[k]['strings']) for p in programs for k in ('body', 'condition')),
        undecodedStrings=sum(s['unknown'] is not None for p in programs for k in ('body', 'condition') for s in p[k]['strings']))))
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--input', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    inspect(args.installation, args.input, args.output)
