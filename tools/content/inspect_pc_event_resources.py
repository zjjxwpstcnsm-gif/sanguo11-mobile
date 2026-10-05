#!/usr/bin/env python3
"""Original installed-directory event discovery/header/decode boundary.

The original event manager also scans a configured/Documents Expansion folder.
Stop before that unresolved process context; never pretend the installed scan
proves effective event priority, complete definitions or executed opening events.
"""
import argparse
import gzip
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP
from inspect_pc_layered_scenario import NativeLayeredWorld
from pc_readonly_platform import ReadOnlyPlatform
from audit_pc_restoration_sources import EXE_SHA, output_guard, json_bytes, sha


def inspect(installation, output):
    installation = installation.resolve()
    output_guard(installation, output)
    world = NativeLayeredWorld((installation / 'san11pk.exe').read_bytes())
    platform = ReadOnlyPlatform(world, installation)
    world.load((installation / 'Media/scenario/Scenario.s11').read_bytes(), True)
    world.load((installation / 'Media/scenario/Scen000.s11').read_bytes())
    actor = world.root + 0x1bd9ac
    boundary = 0x679d9f
    world.u.hook_add(UC_HOOK_CODE, lambda u, address, size, user: u.emu_stop(),
                    begin=boundary, end=boundary)
    world.call(0x679cb0, actor + 0xc, 0, receiver=0x9c41a00,
               count=50000000, stop=boundary)
    stack = world.u.reg_read(UC_X86_REG_ESP)
    begin, end, capacity = struct.unpack('<3I', world.u.mem_read(stack + 0x24, 12))
    if not begin or (end - begin) % 0x154 or not 0 < (end - begin) // 0x154 <= 100:
        raise ValueError('Unexamined native event-discovery container')
    candidates = []
    for index in range((end - begin) // 0x154):
        native = bytes(world.u.mem_read(begin + index * 0x154, 0x154))
        words = struct.unpack('<85I', native)
        event_id = words[2]
        path = installation / ('Media/script/event/%08x.eve' % event_id)
        raw = path.read_bytes()
        if raw[:0x100] != native[:0x100] or words[0] != 0x57bf3 or words[3] != 20:
            raise ValueError('Original event header/source identity mismatch')
        counts = list(words[8:28])
        if sum(counts) != words[4]:
            raise ValueError('Original section counts disagree with declared total')
        if not 0 < words[5] < 2 * 1024 * 1024 or not words[84]:
            raise ValueError('Unexamined decoded buffer declaration')
        decoded = bytes(world.u.mem_read(words[84], words[5]))
        candidates.append(dict(path=path.relative_to(installation).as_posix(),
            sha256=sha(raw), bytes=len(raw), nativeResourceId=event_id,
            originalHeaderHex=native[:0x100].hex(), sectionCount=words[3],
            sectionRecordCounts=counts, declaredRecordCount=words[4],
            nativePrefixCounts=list(words[64:84]),
            decodedBufferDeclaredBytes=words[5], decodedBufferSha256=sha(decoded),
            decodedBufferHex=decoded.hex(), completeDefinitionLoad=False,
            activePriorityProven=False, openingEventsExecuted=False))
    opened = {c['sourcePath']: c['sha256'] for c in platform.calls if c['function'] == 'fopen' and c['result']}
    if opened != {c['path']: c['sha256'] for c in candidates}:
        raise ValueError('Native discovery candidates and actual opened files differ')
    comparisons = [c for c in platform.calls if c['function'] == 'ascii_mbscmp']
    if len(comparisons) != len(candidates) or any(c['result'] != 0 for c in comparisons):
        raise ValueError('Original decoded event identifier validation did not pass')
    report = dict(schema=1, sourceExecutableSha256=EXE_SHA,
        completeStartup=False, originalManager='679cb0', stoppedBefore=hex(boundary),
        installedDirectoryCandidates=candidates, platformCalls=platform.calls,
        platformFixture=dict(executableDirectory='G:\\', directoryIteration='casefold-sorted fixture; host order not proven',
            io='bounded read-only Win32 and statically linked CRT FILE boundaries',
            allocator='original custom heap; bounded CRT malloc/realloc arena; free on fixture disposal',
            stringComparison='ASCII-only CRT mbscmp; rejects multibyte comparisons',
            deferredAtexit=True, applicationConstructorExecuted=False, wineStarted=False),
        unknown=['configuration/Documents path from43f270', 'Expansion override directory',
                 'effective duplicate resource priority', 'full20-section manager assembly',
                 'opening conditions and effects', 'scenario/person identity joins'])
    payload = json_bytes(report)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(gzip.compress(payload, mtime=0))
    output.with_suffix('.summary.json').write_bytes(json_bytes(dict(
        schema=1, packedSha256=sha(output.read_bytes()), decodedSha256=sha(payload),
        candidates=[{k: v for k, v in c.items() if k not in ('decodedBufferHex', 'originalHeaderHex')}
                    for c in candidates], unknown=report['unknown'])))
    print(json.dumps(dict(candidates=len(candidates), packedSha256=sha(output.read_bytes()))), flush=True)
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    inspect(args.installation, args.output)
