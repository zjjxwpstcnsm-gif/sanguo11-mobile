#!/usr/bin/env python3
"""Execute original58b640, retaining selection, model, rejection and callback.

Declared source units/controllers are numeric fixtures, not ordinary menu proof.
Only previously examined void value-display boundary is omitted.
"""
import argparse
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_EIP, UC_X86_REG_ESP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA, sha, output_guard


def inspect(installation, output, seed, camera_fixture=False):
    output_guard(installation, output)
    if output.exists() or output.with_suffix('.failure.json').exists():
        raise ValueError('Preserve earlier receipt')
    context_raw = Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes()
    assert sha(context_raw) == '49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38'
    context = json.loads(context_raw)
    d, w, source, geography, unused = prepare(installation)
    assert source == context['source']
    baseline = bytes(w.u.mem_read(w.root, 0x300000))
    original_rng = bytes(w.u.mem_read(0x8a5d44, 4))
    units = []
    for unit, case in zip(context['units'], context['cases']):
        pointer = unit['pointer']
        w.u.mem_write(pointer, bytes.fromhex(case['afterHex']))
        w.u.mem_write(pointer + 0x3c, struct.pack('<hh', 80 + unit['index'], 80))
        w.u.mem_write(pointer + 0x24, struct.pack('<i', 0))
        w.call(0x4962b0, 0, 0, 5000, receiver=pointer)
        w.call(0x496250, 997, receiver=pointer)
        w.call(0x496280, 17000, receiver=pointer)
        w.u.mem_write(pointer + 0x18, struct.pack('<H', 5000))
        w.u.mem_write(pointer + 0x1a, bytes([100]))
        w.u.reg_write(UC_X86_REG_EAX, pointer)
        location = w.call(0x4a7530)
        for person in case['declaredCrew']:
            w.call(0x4a0cb0, person['pointer'], location, receiver=0x799895c, count=10000000)
            for injury in range(4):
                w.call(0x50c690, person['pointer'], injury, 1, count=10000000)
        w.call(0x496f40, receiver=pointer, count=10000000)
        assert w.call(0x47a630, pointer) == 1
        units.append(pointer)
    # Actual command consumes unit IDs, a packed point, and tactic ID.
    command = d.fixture + 0x7000
    target = bytes(w.u.mem_read(units[1] + 0x3c, 4))
    w.u.mem_write(command, struct.pack('<2I', 0, 1) + target + struct.pack('<I', 0xffffffff))
    before = bytes(w.u.mem_read(w.root, 0x300000))
    entries, omitted, terminal_model = [], [], []
    presentation = json.loads(Path('out/session-b/duel-campaign-presentation-source-v2.json').read_text())
    display = next(f for f in presentation['functions'] if f['address'] == '0x588bb0')
    assert sha(bytes(w.u.mem_read(0x588bb0, 0xb0))) == display['sha256']

    def skip_display(u, address, size, user):
        sp = u.reg_read(UC_X86_REG_ESP)
        ret, *args = struct.unpack('<4I', bytes(u.mem_read(sp, 16)))
        omitted.append(dict(address=hex(address), arguments=args, boundedSha=display['sha256'], scope='examined void value-display only'))
        u.reg_write(UC_X86_REG_ESP, sp + 16)
        u.reg_write(UC_X86_REG_EIP, ret)

    w.u.hook_add(UC_HOOK_CODE, skip_display, begin=0x588bb0, end=0x588bb0)
    if camera_fixture:
        camera = json.loads(Path('out/session-b/duel-command-camera-source.json').read_text())
        assert sha(bytes(w.u.mem_read(0x58a380, camera['boundedBytes']))) == camera['sha256']
        # Explicit controller storage supports the original callback flag; no
        # player/control/admission value is changed. The camera object remains
        # absent, so omit only its examined void wrapper and record every call.
        controller = d.fixture + 0x7200
        w.u.mem_write(controller, bytes(32))
        w.u.mem_write(0x91ba018, struct.pack('<I', controller))
        def skip_camera(u, address, size, user):
            sp = u.reg_read(UC_X86_REG_ESP)
            ret, *args = struct.unpack('<3I', bytes(u.mem_read(sp, 12)))
            omitted.append(dict(address=hex(address), arguments=args, boundedSha=camera['sha256'], scope='examined void camera animation only; declared campaign flag storage'))
            u.reg_write(UC_X86_REG_ESP, sp + 4)
            u.reg_write(UC_X86_REG_EIP, ret)
        w.u.hook_add(UC_HOOK_CODE, skip_camera, begin=0x58a380, end=0x58a380)
        refresh_source = json.loads(Path('out/session-b/duel-command-audio-source.json').read_text())
        refresh = next(f for f in refresh_source['functions'] if f['address'] == '0x5a1330')
        assert sha(bytes(w.u.mem_read(0x5a1330, refresh['boundedBytes']))) == refresh['sha256']
        # Despite the historical source file label, this function updates the
        # current renderer camera distance and scene, not a proven voice rule.
        def skip_refresh(u, address, size, user):
            sp = u.reg_read(UC_X86_REG_ESP)
            ret, arg = struct.unpack('<2I', bytes(u.mem_read(sp, 8)))
            omitted.append(dict(address=hex(address), arguments=[arg], boundedSha=refresh['sha256'], scope='examined void renderer-camera/scene refresh only; no speaker/audio evidence'))
            u.reg_write(UC_X86_REG_ESP, sp + 4)
            u.reg_write(UC_X86_REG_EIP, ret)
        w.u.hook_add(UC_HOOK_CODE, skip_refresh, begin=0x5a1330, end=0x5a1330)
    for address in [0x58b400, 0x589f70, 0x50e3f0, 0x50de30, 0x50c030, 0x505eb0, 0x4d3a50, 0x4d3340, 0x4d3260, 0x4964f0, 0x4ae4a0]:
        def observe(u, ip, size, user):
            sp = u.reg_read(UC_X86_REG_ESP)
            entries.append(dict(address=hex(ip), receiver=hex(u.reg_read(UC_X86_REG_ECX)), stack=list(struct.unpack('<9I', bytes(u.mem_read(sp, 36))))))
            if ip == 0x505eb0:
                terminal_model.append(dict(modelHex=bytes(u.mem_read(u.reg_read(UC_X86_REG_ECX), 0x59c)).hex(), rngBefore=struct.unpack('<I', u.mem_read(0x8a5d44, 4))[0]))
        w.u.hook_add(UC_HOOK_CODE, observe, begin=address, end=address)
    for address in [0x58b6ed, 0x58b73c]:
        def selection_return(u, ip, size, user):
            entries.append(dict(address=hex(ip), selectionReturnPointer=u.reg_read(UC_X86_REG_EAX)))
        w.u.hook_add(UC_HOOK_CODE, selection_return, begin=address, end=address)
    w.u.mem_write(0x8a5d44, struct.pack('<I', seed))
    facts = dict(exeSha=EXE_SHA, source=source, geography=geography, contextSha=sha(context_raw), seed=seed, commandHex=bytes(w.u.mem_read(command, 16)).hex(), unitsBefore=[bytes(w.u.mem_read(p, 244)).hex() for p in units], originalManualControllers=[w.call(0x47a6d0, receiver=p) for p in units], originalDisplayOptions={hex(p):struct.unpack('<i', w.u.mem_read(p, 4))[0] for p in [0x73fb524, 0x73f550c]}, campaignControllerPointer=struct.unpack('<I', w.u.mem_read(0x91ba018, 4))[0], completeGoal=False)
    output.with_suffix('.partial.json').write_text(json.dumps(facts, indent=2) + '\n')
    try:
        result = w.call(0x58b640, command, count=100000000)
    except Exception as error:
        sp = w.u.reg_read(UC_X86_REG_ESP)
        facts.update(error=repr(error), ip=hex(w.u.reg_read(UC_X86_REG_EIP)), receiver=hex(w.u.reg_read(UC_X86_REG_ECX)), stackHex=bytes(w.u.mem_read(sp, 96)).hex(), invalid=w.invalid, entries=entries, omitted=omitted)
        output.with_suffix('.failure.json').write_text(json.dumps(facts, indent=2) + '\n')
        raise
    after = bytes(w.u.mem_read(w.root, 0x300000))
    facts.update(result=result, entries=entries, omitted=omitted, terminalModels=terminal_model, managerHex=bytes(w.u.mem_read(0x8b3740, 0xd0)).hex(), rngAfter=struct.unpack('<I', w.u.mem_read(0x8a5d44, 4))[0], changedBytes=[dict(offset=i, before=a, after=b) for i, (a,b) in enumerate(zip(before, after)) if a != b], unitsAfter=[bytes(w.u.mem_read(p, 244)).hex() for p in units], limits=['Declared units/geometry/control state; not ordinary deployment or player selection', 'Original complete command rules and RNG unchanged; display omission recorded', 'Upstream action/AP/fee/menu/Android acceptance remain required'])
    w.u.mem_write(w.root, baseline)
    w.u.mem_write(0x8a5d44, original_rng)
    assert baseline == bytes(w.u.mem_read(w.root, 0x300000)) and original_rng == bytes(w.u.mem_read(0x8a5d44, 4))
    facts['wholeWorldAndRngRestored'] = True
    output.write_text(json.dumps(facts, indent=2) + '\n')
    print('PASS original full command', result, 'changed', len(facts['changedBytes']), 'SHA', sha(output.read_bytes()), flush=True)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--seed', type=int, default=23)
    p.add_argument('--camera-fixture', action='store_true')
    a = p.parse_args()
    inspect(a.installation, a.output, a.seed, a.camera_fixture)
