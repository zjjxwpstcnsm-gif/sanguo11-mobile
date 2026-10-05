#!/usr/bin/env python3
"""Bounded named infantry order -> original dispatch/renderer -> source sound chain.

Execute only argument forwarding, final record stores, renderer selector and
readonly voice callback. Damage, commands, animation state entry and both RNG
engines are never entered. Random choice is explicit already-produced input.
"""
import argparse
import hashlib
import itertools
import json
from pathlib import Path
import struct
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'content'))
from inspect_pc_scenario_tail import NativeTailDecoder
from unicorn import UC_HOOK_CODE
from capstone import Cs, CS_ARCH_X86, CS_MODE_32
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_EBX, UC_X86_REG_ECX, UC_X86_REG_EBP, UC_X86_REG_ESI, UC_X86_REG_ESP, UC_X86_REG_EIP

EXE_SHA = '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
INITIALIZERS = {0: (0x562f70, 0x562fdf, 6), 1: (0x563040, 0x5630b2, 7),
                2: (0x563110, 0x56317f, 8),
                3: (0x5631e0, 0x563264, 9), 4: (0x5632b0, 0x563333, 10),
                5: (0x563380, 0x563403, 11), 6: (0x563450, 0x5634e0, 12),
                7: (0x563550, 0x5635e3, 13), 8: (0x563650, 0x5636e3, 14), 17: (0x563e60, 0x563ecf, 27)}


def inspect(installation, output, include_cavalry=False):
    if output.exists() or installation.resolve() in output.resolve().parents:
        raise ValueError('Fresh output outside readonly PC source required')
    raw = (installation / 'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest() != EXE_SHA:
        raise ValueError('Source executable differs')
    shared = (installation / 'Media/scenario/Scenario.s11').read_bytes()
    d = NativeTailDecoder(raw)
    tables = d.decode_tail(shared, True)
    u = d.u
    initializers = dict(INITIALIZERS)
    if include_cavalry:
        initializers.update({9: (0x5638f0, 0x56396e, 17),
                             10: (0x563820, 0x56389e, 16),
                             11: (0x5638f0, 0x56396e, 17)})
    u.mem_map(0x6fb0000, 0x100000)
    u.mem_map(0x9500000, 0x2000000)
    u.mem_map(0x9119000, 0x6000)
    u.mem_map(d.stream + 0x1000, 0x3000)
    if not any(lo <= 0x6ed37f0 and 0x6ed37f0 + 2496 <= hi + 1 for lo, hi, _ in u.mem_regions()):
        u.mem_map(0x6ed3000, 0x2000)
    order, record, renderer, payload, available = [d.stream + n for n in (0x100, 0x200, 0x1000, 0x1800, 0x1900)]
    unit = d.root + 0x169730
    actor = d.root + 0xc0bc + 5 * 0x190
    source_point, target_point = (10, 20), (30, 40)
    u.mem_write(unit + 0x3c, struct.pack('<hh', *source_point))
    u.mem_write(unit + 0xc, struct.pack('<i', 5))
    u.mem_write(unit + 0xa8, struct.pack('<i', 4))  # Explicit non6 weapon; no source fleet-terrain flags.
    # Explicit valid/current actor fixture; never called a live PC state.
    u.mem_write(actor + 0xa0, struct.pack('<i', 0))
    u.mem_write(actor + 0x17c, struct.pack('<i', 0))
    u.mem_write(actor + 0x100, struct.pack('<i', 0))
    u.mem_write(actor + 0x170, bytes([50, 50, 50, 50]))
    u.mem_write(payload + 8, struct.pack('<H', 0))
    u.mem_write(0x95499b0, struct.pack('<I', renderer))
    u.mem_write(0x9119810 + 0x34, struct.pack('<I', available))
    u.mem_write(0x6fb0e70 + 20 * (source_point[0] * 200 + source_point[1]), struct.pack('<H', 0))
    u.mem_write(0x6fb0e70 + 20 * (target_point[0] * 200 + target_point[1]), struct.pack('<H', 1))
    vtable = struct.unpack('<I', u.mem_read(unit, 4))[0]
    getter = struct.unpack('<I', u.mem_read(vtable + 0x3c, 4))[0]
    if getter != 0x496030 or bytes(u.mem_read(getter, 4)) != bytes.fromhex('8d413cc3'):
        raise ValueError('Original coordinate getter differs')
    phase = ''
    captured, profiles, voices, effects, sounds, routes = [], [], [], [], [], []
    choice = 0

    def forbidden(machine, address, size, user):
        raise ValueError('Forbidden rule/RNG entry ' + hex(address))
    for address in [0x472150, 0x4721d0, 0x444150, 0x4442a0, 0x5ae610, 0x593c40, 0x594300, 0x5b1870]:
        u.hook_add(UC_HOOK_CODE, forbidden, begin=address, end=address)

    def boundary(machine, address, size, user):
        sp = machine.reg_read(UC_X86_REG_ESP)
        if address == 0x595630:
            if phase != 'cavalry-wrapper':
                raise ValueError('Unreviewed full cavalry handler attempted')
            captured.append(list(struct.unpack('<3I', machine.mem_read(sp + 4, 12))))
            machine.emu_stop()
            return
        if address in [0x5a66e0, 0x5afc70]:
            if (phase, address) not in [('order', 0x5a66e0), ('packet', 0x5afc70)]:
                raise ValueError('Unreviewed full constructor attempted')
            count = 4 if address == 0x5a66e0 else 7
            captured.append(list(struct.unpack('<' + 'I' * count, machine.mem_read(sp + 4, 4 * count))))
            machine.emu_stop()
            return
        if phase == 'initializer' and address in [row[1] for row in initializers.values()]:
            machine.emu_stop()
            return
        ret = struct.unpack('<I', machine.mem_read(sp, 4))[0]
        consumed, result = 0, 1
        if address == 0x56f0c0:
            consumed = 4
        elif address in [row[0] for row in initializers.values()]:
            if machine.reg_read(UC_X86_REG_ECX) != renderer:
                raise ValueError('Original grid selected a different renderer')
            if phase == 'initializer':
                return  # Execute the original prefix field stores unchanged.
            if phase != 'route':
                raise ValueError('Unreviewed renderer initializer phase')
            if struct.unpack('<I', machine.mem_read(sp + 4, 4))[0] != record:
                raise ValueError('Original initializer record differs')
            routes.append(address)
            consumed = 12 if address in (0x563820, 0x5638f0) else 4
        elif address == 0x402310:
            if phase != 'voice' or struct.unpack('<I', machine.mem_read(sp + 4, 4))[0] != 2:
                raise ValueError('Unreviewed random choice boundary')
            result = choice
        elif address == 0x4d1000:
            voices.append(struct.unpack('<2I', machine.mem_read(sp + 4, 8))[0])
            consumed = 8
        elif address == 0x6e96a0:
            if machine.reg_read(UC_X86_REG_ECX) != available:
                raise ValueError('Audio availability receiver differs')
        elif address == 0x414670:
            effects.append(struct.unpack('<I', machine.mem_read(sp + 4, 4))[0])
            consumed = 8
        elif address == 0x564bb0:
            sounds.append(struct.unpack('<I', machine.mem_read(sp + 4, 4))[0])
            consumed = 8
        machine.reg_write(UC_X86_REG_EAX, result)
        machine.reg_write(UC_X86_REG_ESP, sp + 4 + consumed)
        machine.reg_write(UC_X86_REG_EIP, ret)

    for address in sorted(set([0x5a66e0, 0x5afc70, 0x56fd60, 0x56f0c0, 0x402310, 0x4d1000,
                    0x6e96a0, 0x414670, 0x564bb0] + [x for row in initializers.values() for x in row[:2]])):
        u.hook_add(UC_HOOK_CODE, boundary, begin=address, end=address)
    wrappers = []
    if include_cavalry:
        u.hook_add(UC_HOOK_CODE, boundary, begin=0x595630, end=0x595630)
        for address, expected_mode in [(0x595b70, 1), (0x595b90, 2)]:
            phase = 'cavalry-wrapper'; captured.clear()
            u.reg_write(UC_X86_REG_ECX, 0x95499b0); u.reg_write(UC_X86_REG_ESP, d.stack)
            u.mem_write(d.stack, struct.pack('<3I', d.stop, record, 0x11223344))
            u.emu_start(address, d.stop, count=100)
            if captured != [[record, expected_mode, 0x11223344]]:
                raise ValueError('Original cavalry wrapper record/mode differs')
            wrappers.append(dict(wrapper=hex(address), stopBeforeHandler='0x595630',
                                 recordPointer=record, modeRaw=expected_mode,
                                 alreadyProducedArgumentRaw=0x11223344))
        for address, callee in [(0x595869, 0x570aa0), (0x595cac, 0x570a00)]:
            instruction = next(Cs(CS_ARCH_X86, CS_MODE_32).disasm(bytes(u.mem_read(address, 5)), address))
            if instruction.mnemonic != 'call' or int(instruction.op_str, 16) != callee:
                raise ValueError('Original primary cavalry presentation route differs')

    def profile_observer(machine, address, size, user):
        sp = machine.reg_read(UC_X86_REG_ESP)
        values = struct.unpack('<3I', machine.mem_read(sp + 4, 12))
        if phase != 'voice' or values[1:] != (actor, 0xbf800000):
            raise ValueError('Actual readonly unit leader voice differs')
        profiles.append(values[0])  # Observation only; original selector continues.
    u.hook_add(UC_HOOK_CODE, profile_observer, begin=0x4d19d0, end=0x4d19d0)

    # Read the original paid/appplied tactic dispatch switch without entering any handler.
    code = Cs(CS_ARCH_X86, CS_MODE_32)
    dispatch_calls = {}
    for native in range(19):
        branch = struct.unpack('<I', u.mem_read(0x587118 + native * 4, 4))[0]
        if branch == 0x5863ff:
            continue  # Original default branch is unbound here.
        call = next(i for i in code.disasm(bytes(u.mem_read(branch, 40)), branch) if i.mnemonic == 'call')
        dispatch_calls[call.address] = int(call.op_str, 16)
    dispatch_observed = []
    def dispatch_boundary(machine, address, size, user):
        if phase != 'dispatch':
            return
        if address in dispatch_calls:
            sp = machine.reg_read(UC_X86_REG_ESP)
            if struct.unpack('<I', machine.mem_read(sp, 4))[0] != d.stack + 0x4d8:
                raise ValueError('Original dispatcher record pointer differs')
            dispatch_observed.append(dict(callAddress=hex(address), handler=hex(dispatch_calls[address])))
        machine.emu_stop()  # Handler, miss presentation and default are not executed.
    for address in list(dispatch_calls) + [0x58651a, 0x5863ff]:
        u.hook_add(UC_HOOK_CODE, dispatch_boundary, begin=address, end=address)
    dispatch_world=bytes(u.mem_read(0x7200000,0x300000));dispatch_rng=bytes(u.mem_read(0x8a5d44,4))+bytes(u.mem_read(0x8a5b68,4))+bytes(u.mem_read(0x6ed37f0,2496))
    dispatch_rows = []
    for native in range(-1, 21):
        for already_success in [0, 1]:
            phase = 'dispatch'; dispatch_observed.clear()
            u.mem_write(d.stack + 0x4d8, bytes(0x80))
            u.mem_write(d.stack + 0x528, struct.pack('<i', native))
            u.mem_write(d.stack + 0x52c, struct.pack('<I', already_success))
            u.mem_write(d.stack + 0x10, struct.pack('<I', 0x95499b0))
            u.reg_write(UC_X86_REG_ESP, d.stack);u.emu_start(0x586230,0x586555,count=1000)
            stopped=u.reg_read(UC_X86_REG_EIP)
            if already_success==0 and (stopped!=0x58651a or dispatch_observed):
                raise ValueError('Original no-success gate did not select separate miss path')
            if already_success==1 and 0<=native<19 and native!=13 and len(dispatch_observed)!=1:
                raise ValueError('Original successful tactic dispatcher incomplete')
            if dispatch_world!=bytes(u.mem_read(0x7200000,0x300000)) or dispatch_rng!=bytes(u.mem_read(0x8a5d44,4))+bytes(u.mem_read(0x8a5b68,4))+bytes(u.mem_read(0x6ed37f0,2496)):raise ValueError('Original read-only dispatch altered world or either RNG')
            dispatch_rows.append(dict(nativeTacticId=native,alreadyAppliedSuccessRaw=already_success,stopAddress=hex(stopped),calls=dispatch_observed.copy()))

    rows = []
    for order_type, action, choice, record0 in itertools.product([3, 4, 5], range(-1, 20), [0, 1], [0, 1, -1, 2]):
        u.mem_write(order, struct.pack('<4I', order_type, 37, struct.unpack('<I', struct.pack('<hh', *target_point))[0], action & 0xffffffff))
        u.mem_write(record, bytes(0x700))
        u.mem_write(record, struct.pack('<i', record0))
        u.mem_write(record+0x54,struct.pack('<I',1))  # Explicit already-applied hit gate, never computed here.
        u.mem_write(record + 0x70, struct.pack('<I', 0x11223344))
        u.mem_write(renderer, bytes(0x280))
        u.mem_write(renderer, struct.pack('<I', payload))
        world = bytes(u.mem_read(0x7200000, 0x300000))
        rng = bytes(u.mem_read(0x8a5d44, 4)) + bytes(u.mem_read(0x8a5b68, 4)) + bytes(u.mem_read(0x6ed37f0, 2496))
        grid = bytes(u.mem_read(0x6fb0000, 0x100000))
        original_order = bytes(u.mem_read(order, 16))
        phase = 'order'; captured.clear()
        u.reg_write(UC_X86_REG_EBX, order); u.reg_write(UC_X86_REG_ESI, unit); u.reg_write(UC_X86_REG_ESP, d.stack)
        u.emu_start(0x5a696a, 0x5a698d, count=100)
        # The call at5a698d must execute, then the original helper is captured.
        u.emu_start(0x5a698d, 0x5a6992, count=100)
        if len(captured) != 1:
            raise ValueError('Original order argument forwarding not captured')
        expected_action = action & 0xffffffff if order_type == 4 else 0xffffffff
        if captured[0][1:] != [37, expected_action, struct.unpack('<I', struct.pack('<hh', *target_point))[0]]:
            raise ValueError('Original order raw fields differ')
        phase = 'packet'; captured.clear(); sp = d.stack
        u.mem_write(sp + 0x18, struct.pack('<I', record)); u.mem_write(sp + 0x20, struct.pack('<I', expected_action))
        u.mem_write(sp + 0x24, struct.pack('<hh', *target_point))
        u.reg_write(UC_X86_REG_ESI, unit); u.reg_write(UC_X86_REG_ESP, sp)
        u.emu_start(0x5a67ce, 0x5a67e9, count=100)
        if captured != [[record, unit, expected_action, struct.unpack('<I', struct.pack('<hh', *target_point))[0], 0, 0, 0]]:
            raise ValueError('Original record constructor packet differs')
        phase = 'record'; sp = d.stack
        u.mem_write(sp + 0x34, struct.pack('<8I', d.stop, *captured[0]))
        u.reg_write(UC_X86_REG_EBP, record); u.reg_write(UC_X86_REG_ESP, sp)
        u.emu_start(0x5b060c, d.stop, count=100)
        if u.reg_read(UC_X86_REG_EIP) != d.stop or u.reg_read(UC_X86_REG_ESP) != sp + 0x38:
            raise ValueError('Original record tail return differs')
        fields = [struct.unpack('<I', u.mem_read(record + offset, 4))[0] for offset in [0x50, 0x58, 0x5c]]
        if fields != [expected_action, struct.unpack('<I', struct.pack('<hh', *source_point))[0], struct.unpack('<I', struct.pack('<hh', *target_point))[0]]:
            raise ValueError('Original record field/getter writes differ')
        phase = 'dispatch'; dispatch_observed.clear()
        u.mem_write(d.stack+0x4d8,bytes(u.mem_read(record,0x80)))
        u.mem_write(d.stack+0x10,struct.pack('<I',0x95499b0));u.reg_write(UC_X86_REG_ESP,d.stack)
        u.emu_start(0x586230,0x586555,count=1000)
        if 0<=fields[0]<19 and fields[0]!=13 and len(dispatch_observed)!=1:
            raise ValueError('Produced original record did not dispatch once')
        joined_dispatch=dispatch_observed.copy()
        phase = 'route'; routes.clear()
        u.reg_write(UC_X86_REG_ECX, 0x95499b0); u.reg_write(UC_X86_REG_ESP, d.stack)
        u.mem_write(d.stack, struct.pack('<4I', d.stop, record, 0, 0))
        cavalry_routes = {9:0x570aa0,10:0x570a00,11:0x570aa0} if include_cavalry else {}
        route = {3:0x5711a0,4:0x5706b0,5:0x570770,6:0x570830,7:0x570830,8:0x570830,**cavalry_routes}.get(action,0x570490) if order_type==4 else 0x570490
        route_stop = {0x570490:0x57050c,0x5711a0:0x5711f3,0x5706b0:0x570701,0x570770:0x5707c1,0x570830:0x570906,0x570aa0:0x570af7,0x570a00:0x570a57}[route]
        u.emu_start(route, route_stop, count=10000)
        initializer = initializers.get(action) if order_type == 4 else None
        if routes != ([] if initializer is None else [initializer[0]]):
            raise ValueError('Original generated record routes differ: '+str((order_type,action,routes,initializer)))
        if initializer:
            phase = 'initializer'
            u.reg_write(UC_X86_REG_ECX, renderer); u.reg_write(UC_X86_REG_ESP, d.stack)
            u.mem_write(d.stack, struct.pack('<4I', d.stop, record, 0, 0))
            u.emu_start(initializer[0], initializer[1], count=100)
            if struct.unpack('<I', u.mem_read(renderer + 4, 4))[0] != initializer[2]:
                raise ValueError('Original renderer field stores differ')
            if struct.unpack('<i', u.mem_read(renderer + 0x240, 4))[0] != record0 or struct.unpack('<I', u.mem_read(renderer + 0x25c, 4))[0] != 0x11223344:
                raise ValueError('Distinct original record0 and record70 fields differ')
        phase = 'voice'; profiles.clear(); voices.clear(); effects.clear(); sounds.clear()
        u.reg_write(UC_X86_REG_ECX, renderer); u.reg_write(UC_X86_REG_ESP, d.stack)
        u.mem_write(d.stack, struct.pack('<I', d.stop)); u.emu_start(0x564cb0, d.stop, count=10000)
        if len(profiles) != int(initializer is not None) or len(voices) != len(profiles):
            raise ValueError('Full readonly original voice selector did not finish')
        if initializer and (effects != [46 if record0 == 0 else 78] or sounds != [49 if record0 == 0 else 78]):
            raise ValueError('Original record0 effect/sound dispatch differs')
        if world != bytes(u.mem_read(0x7200000, 0x300000)) or grid != bytes(u.mem_read(0x6fb0000, 0x100000)) or original_order != bytes(u.mem_read(order, 16)):
            raise ValueError('Source world/grid/order mutated')
        if rng != bytes(u.mem_read(0x8a5d44, 4)) + bytes(u.mem_read(0x8a5b68, 4)) + bytes(u.mem_read(0x6ed37f0, 2496)):
            raise ValueError('Source RNG changed')
        rows.append(dict(orderTypeRaw=order_type, order4Raw=37, order12Raw=action,
                         nativeTacticName=next((bytes.fromhex(r['actor_hex'])[4:13].split(b'\0')[0].decode('big5') for r in tables['records'] if r['kind']=='table_84858' and r['native_index']==action),None),
                         presentationRoute=hex(route),alreadyAppliedSuccessRaw=1,joinedDispatch=joined_dispatch,
                         record50Raw=expected_action if expected_action < 0x80000000 else expected_action - 0x100000000,
                         record58Raw=list(source_point), record5cRaw=list(target_point),
                         record0Raw=record0, record70Raw=0x11223344,
                         rendererActionRaw=initializer[2] if initializer else 0,
                         alreadyProducedChoiceRaw=choice, speakerNativeId=5, profiles=profiles.copy(),
                         voiceIds=voices.copy(), effectIds=effects.copy(), soundIds=sounds.copy()))
    ranges = [(0x5a696a, 0x5a6992), (0x5a67ce, 0x5a67e9), (0x5b060c, 0x5b0632),
              (0x496030, 0x496034),(0x586230,0x5863ff),(0x58651a,0x586554),(0x587118,0x587164),(0x4d58c0,0x4d58c7),(0x496410,0x496481), (0x570490, 0x57050c), (0x564cb0, 0x564e30)] + [(a, b) for a, b, _ in initializers.values()]
    if include_cavalry:
        ranges += [(0x595b70,0x595b82),(0x595b90,0x595ba2),(0x595630,0x595b5f),
                   (0x595bb0,0x595d1a),(0x570a00,0x570a57),(0x570aa0,0x570af7)]
    report = dict(sourceExecutableSha256=EXE_SHA, sourceSharedSha256=hashlib.sha256(shared).hexdigest(),
                  checks=len(rows)+len(dispatch_rows)+len(wrappers), rows=rows, appliedSuccessDispatch=dispatch_rows,
                  cavalryWrapperPrefixes=wrappers, cavalryIncluded=include_cavalry,
                  codeSha256={hex(a): hashlib.sha256(bytes(u.mem_read(a, b - a))).hexdigest() for a, b in ranges},
                  established=['Original586230 tests record+54 before dispatch; zero selects58651a, a separate miss effect59/sound58 path. Noncritical is not proof of hit. Only established applied primary physical Strike permits Android49/78.',
                               'The bounded original final record path writes actual unit virtual3c coordinates to+58 and supplied destination to+5c.',
                               'Renderer+240 is original record+0, not record+70; +25c receives record+70. Record0 zero/nonzero selects effect46/78 and sound49/78.',
                               'Generated record source coordinates select the original unit renderer, then actual native leader/profile/voice selectors execute.'],
                  boundaries=['Earlier target validity, damage/rules and complete constructor are not executed; only original forwarding and tail field stores.',
                              'Explicit constructed order, source unit/actor/grid/current-byte inputs; not actual PC or Android gameplay.',
                              'Explicit infantry non6 weapon and grid terrain0 fixture; original getters execute unchanged. Cavalry/siege/naval alternatives are not bound by this report.',
                              '402310(2) supplied already-produced choice0/1; all underlying RNG entries forbidden.'],
                  limits=['Native3..8 exact Shared names and route prefixes established; project enum joins must be explicit and ordinary installed triggers separately verified.',
                          'No normal Android voice binding, installed voice, Windows native PCM, ARM speaker or full restored battle claim.'])
    if include_cavalry:
        report['boundaries'].append('Cavalry9/11 wrappers execute to595630 argument boundary; its570aa0 primary route and native10 handler570a00 link are static exact-call checks. Earlier movement preflight and target/secondary renderer are not executed.')
        report['boundaries'][2] = 'Explicit non6 weapon and terrain0; infantry0..8 and primary cavalry9..11 prefixes verified. Siege/naval remain unbound.'
        report['limits'][0] = 'Source Shared names0..11 and primary route prefixes established; Android enum joins and actual committed-trigger playback require separate verification.'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(dict(status='PASS_BOUNDED_SOURCE_CHAIN', checks=report['checks'], voicedRows=sum(bool(r['voiceIds']) for r in rows))))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--include-cavalry', action='store_true',
                        help='Also execute primary cavalry route prefixes; pre-route movement context stays static')
    args = parser.parse_args()
    inspect(args.installation, args.output, args.include_cavalry)
