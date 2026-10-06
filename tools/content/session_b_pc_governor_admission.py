#!/usr/bin/env python3
"""Original full governor election under explicit VM-only controlled fields.

Fresh original worlds per case retain registration side effects. No native rule
return is replaced; installed files are read-only; normal GUI/controller separate.
"""
import argparse
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP, UC_X86_REG_EIP, UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_EDX
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from audit_pc_restoration_sources import ROOT, EXE_SHA, sha, output_guard

def inspect(installation, output, assignments=False, site_armies=False, site_transitions=False, site_captures=False, initialize_provider=False, arrivals=False, recruitment=False):
    output_guard(installation, output)
    if output.exists():
        raise ValueError('Preserve earlier original evidence')
    executable = (installation / 'san11pk.exe').read_bytes()
    assert sha(executable) == EXE_SHA
    source = json.loads((ROOT / 'docs/handoff/20261004/session1/source-manifest.json').read_text())['scenarios'][0]
    raw = (installation / source['sourcePath']).read_bytes()
    assert sha(raw) == source['sourceSha256']
    shared = (installation / 'Media/scenario/Scenario.s11').read_bytes()
    cases = [
        ('baseline', {}, None),
        ('ruler_and_district_last_priority', {403: {0x94: 6, 0x98: 13, 0x9c: 13}}, None),
        ('ruler_only_priority', {403: {0x94: 6, 0x98: 13, 0x9c: 13}, 440: {0xa0: 3}}, None),
        ('district_then_ruler_last_priority', {403: {0x94: 6, 0x98: 13, 0x9c: 13, 0xa0: 1}, 440: {0xa0: 0}}, None),
        ('ordinary_capacity_order', {403: {0x94: 6, 0x98: 13, 0x9c: 13, 0xa0: 3}, 440: {0xa0: 3}}, None),
        ('different_current_location', {440: {0x9c: 14}}, None),
        ('different_same_force_army', {440: {0x94: 5}}, None),
        ('unappeared_priority', {440: {0xa0: 6}}, None),
        ('old_governor_replaced', {58: {0xa0: 2}}, 58),
        ('ordinary_promoted_old_demoted', {440: {0x9c: 14}, 58: {0xa0: 2}}, 58),
        ('all_candidates_absent_clears', {58: {0x9c: 14}, 440: {0x9c: 14}, 509: {0x9c: 14}}, None),
    ]
    if assignments:
        cases=[('home_setter_only',{},None),('army_setter_only',{},None),('location_setter_only',{},None),('full_station_arrival',{},None)]
    if site_armies:
        cases=[("site_army_only",{},None),("site_army_and_resident",{},None)]
    if site_transitions:
        cases=[("same_force_army_transition",{},None),("foreign_force_army_transition",{},None),("clear_army_transition",{},None)]
    if site_captures:
        cases=[("foreign_army_capture_mode0",{},None),("foreign_army_capture_mode2",{},None)]
    if arrivals:
        cases=[("district_full_arrival",{},None),("ordinary_full_arrival",{},None)]
    if recruitment:
        cases=[("successful_recruitment_callback",{},None)]
    results = []
    for label, changes, previous in cases:
        w = NativeDebateFlow(installation, executable).world
        load_original_data(w.u)
        w.load(shared, True); w.load(raw); w.call(0x73c500); w.call(0x73c2b0)
        for function, value in zip([0x4826e0, 0x482700, 0x482720], source['date']):
            w.call(function, value, receiver=w.root)
        w.call(0x4827b0, 0, receiver=w.root)
        w.call(0x493400, receiver=w.root, count=50000000)
        provider_evidence=None
        if initialize_provider:
            if not site_captures:raise ValueError("Provider construction is an explicit ownership-helper fixture only")
            provider=0x32602b0;before_provider=bytes(w.u.mem_read(provider,0x240))
            code=bytes(w.u.mem_read(0x415400,0xa4));assert code==executable[0x15400:0x154a4]
            w.call(0x415400,receiver=provider,count=50000000)
            assert struct.unpack("<I",w.u.mem_read(provider,4))[0]==0x779b90
            provider_evidence=dict(function="0x415400",codeSha256=sha(code),beforeHex=before_provider.hex(),afterHex=bytes(w.u.mem_read(provider,0x240)).hex(),
                scope="explicit original provider constructor; original current PC viewport/camera is unknown; no substituted camera accessor or gameplay return")
        site = w.call(0x490d00, 13, receiver=w.root)
        def person(native):
            pointer = w.call(0x490b00, native, receiver=w.root)
            assert w.call(0x4883c0, receiver=pointer) == native
            return pointer
        for native, fields in changes.items():
            pointer = person(native)
            for offset, value in fields.items():
                w.u.mem_write(pointer + offset, struct.pack('<i', value))
        if previous is not None:
            w.call(0x4b3a20, site, person(previous), receiver=0x799895c)
        def fields(native):
            pointer = person(native)
            value = bytes(w.u.mem_read(pointer, 0x190))
            table = struct.unpack_from('<I', value)[0]
            def virtual(slot):
                getter = struct.unpack('<I', w.u.mem_read(table + slot, 4))[0]
                result = w.call(getter, receiver=pointer) & 0xffffffff
                return result if result < 0x80000000 else result - 0x100000000
            return dict(nativeId=native, allowed=bool(w.call(0x47a630, pointer)),
                        owner=virtual(0x40), army=virtual(0x44),
                        home=struct.unpack_from('<i', value, 0x98)[0],
                        current=struct.unpack_from('<i', value, 0x9c)[0],
                        status=struct.unpack_from('<i', value, 0xa0)[0],
                        mask15=bool(w.call(0x489fe0, 15, receiver=pointer)),
                        resident=bool(w.call(0x489730, receiver=pointer)),
                        capacity=w.call(0x48a4f0, receiver=pointer) & 65535,
                        leadership=value[0x170], war=value[0x171],
                        merit=struct.unpack_from('<H', value, 0xae)[0])
        transition_trace=[]
        if site_transitions or site_captures:
            target_army={"same_force_army_transition":5,"foreign_force_army_transition":7,"clear_army_transition":-1}.get(label,7)
            def site_values():
                table=struct.unpack("<I",w.u.mem_read(site,4))[0]
                value={}
                for key,slot in [("owner",0x40),("army",0x44)]:
                    getter=struct.unpack("<I",w.u.mem_read(table+slot,4))[0];n=w.call(getter,receiver=site)&0xffffffff;value[key]=n if n<0x80000000 else n-0x100000000
                for key in [3,4,13,14,17,18,19]:
                    n=w.call(0x4c69a0,site,key)&0xffffffff;value[str(key)]=n if n<0x80000000 else n-0x100000000
                return value
            transition_before=site_values();before_memory=bytes(w.u.mem_read(0x7200000,0x300000));transition_seed=bytes(w.u.mem_read(0x8a5d44,4))
            callbacks=[]
            def callback_hook(u,address,size,user):
                words=struct.unpack("<4I",u.mem_read(u.reg_read(UC_X86_REG_ESP),16));callbacks.append(dict(event=words[1],subject=hex(words[2]),value=words[3]))
            callback_token=w.u.hook_add(UC_HOOK_CODE,callback_hook,begin=0x4bbaa0,end=0x4bbaa0)
            recent=[]
            def execution_trace(u,address,size,user):
                recent.append(hex(address))
                if len(recent)>48:recent.pop(0)
            trace_token=w.u.hook_add(UC_HOOK_CODE,execution_trace)if site_captures else None
            try:
                if site_captures:
                    army=w.call(0x490ad0,target_army,receiver=w.root);assert w.call(0x4912c0,army,receiver=w.root)==target_army
                    mode=0 if label.endswith("mode0")else 2
                    w.call(0x4b40c0,site,army,mode,receiver=0x799895c,count=200000000)
                else:w.call(0x4ad550,site,target_army&0xffffffff,receiver=0x799895c,count=50000000)
            except Exception as error:
                eip=w.u.reg_read(UC_X86_REG_EIP)
                failure=dict(case=label,source=source,exeSha=EXE_SHA,function="0x4b40c0"if site_captures else "0x4ad550",error=repr(error),eip=hex(eip),recent=recent,
                    registers={name:hex(w.u.reg_read(reg))for name,reg in [("esp",UC_X86_REG_ESP),("eax",UC_X86_REG_EAX),("ecx",UC_X86_REG_ECX),("edx",UC_X86_REG_EDX)]})
                try:failure["instructionBytes"]=bytes(w.u.mem_read(eip,32)).hex()
                except Exception:pass
                output.parent.mkdir(parents=True,exist_ok=True);output.with_suffix('.failure.json').write_text(json.dumps(failure,ensure_ascii=False,indent=2)+'\n')
                print(json.dumps(failure,ensure_ascii=False),flush=True);raise
            finally:
                w.u.hook_del(callback_token)
                if trace_token is not None:w.u.hook_del(trace_token)
            after_memory=bytes(w.u.mem_read(0x7200000,0x300000));transition_trace.append(dict(targetArmy=target_army,before=transition_before,after=site_values(),callbacks=callbacks,
                worldBeforeSha256=sha(before_memory),worldAfterSha256=sha(after_memory),changedBytes=sum(a!=b for a,b in zip(before_memory,after_memory)),rngBefore=transition_seed.hex(),rngAfter=bytes(w.u.mem_read(0x8a5d44,4)).hex()))
        site_army_trace=[]
        if site_armies:
            city=w.call(0x490a10,13,receiver=w.root)
            before_army=w.call(0x47c320,receiver=city)
            # This concrete original setter validates -1..46 and clamps structure HP.
            # It is not the full capture/controller path or a substituted return.
            w.call(0x47cbd0,5,receiver=city,count=10000000)
            assert w.call(0x47c320,receiver=city)==5
            if label=="site_army_and_resident":
                w.call(0x4a32f0,person(440),5,receiver=0x799895c,count=10000000)
            site_army_trace.append(dict(before=before_army,after=5,setter="0x47cbd0",getter="0x47c320",person=fields(440)))
        recruitment_trace=[]
        if recruitment:
            actor=person(116);target=person(222);coordinate=struct.unpack("<I",w.u.mem_read(target+0x98,4))[0]
            before_recruit=[fields(116),fields(222)]
            w.call(0x5c4840,actor,target,coordinate,1,1,count=50000000)
            recruitment_trace.append(dict(function="0x5c4840",alreadyPaid=True,coordinate=coordinate,before=before_recruit,after=[fields(116),fields(222)]))
        arrival_trace=[]
        if arrivals:
            native=440 if label.startswith("district")else 58;actor=person(native);destination=w.call(0x490d00,3,receiver=w.root)
            def armies():
                return [dict(nativeId=index,allowed=bool(w.call(0x47a630,w.call(0x490ad0,index,receiver=w.root))),properties={str(key):(lambda n:n if n<0x80000000 else n-0x100000000)(w.call(0x4c2960,w.call(0x490ad0,index,receiver=w.root),key)&0xffffffff)for key in range(3,10)})for index in range(47)]
            armies_before=armies()
            arrival_before=fields(native);arrival_seed=bytes(w.u.mem_read(0x8a5d44,4))
            w.call(0x4bf6f0,actor,destination,0,0,receiver=0x799895c,count=50000000)
            arrival_trace.append(dict(nativeId=native,function="0x4bf6f0",before=arrival_before,after=fields(native),armiesBefore=armies_before,armiesAfter=armies(),rngBefore=arrival_seed.hex(),rngAfter=bytes(w.u.mem_read(0x8a5d44,4)).hex()))
        assignment_trace=[]
        if assignments:
            actor=person(440)
            assignment_trace.append(dict(step='before',actor=fields(440)))
            for enabled,function,value,name in [
                (label in ['home_setter_only','full_station_arrival'],0x4a31e0,3,'home'),
                (label in ['army_setter_only','full_station_arrival'],0x4a32f0,5,'army'),
                (label in ['location_setter_only','full_station_arrival'],0x4a0cb0,3,'location')]:
                if enabled:
                    w.call(function,actor,value,receiver=0x799895c,count=10000000)
                    assignment_trace.append(dict(step=name,function=hex(function),actor=fields(440)))
        before = [fields(native) for native in [58, 403, 440, 509]]
        calls = []
        # Avoid reentrant emulator calls in hooks: pointer-to-ID table is complete.
        pointers = {person(native): native for native in range(1100)}
        def hook(u, address, size, user):
            _, building, actor = struct.unpack('<3I', u.mem_read(u.reg_read(UC_X86_REG_ESP), 12))
            if actor and actor not in pointers: raise ValueError('Unverified appointment pointer')
            calls.append(dict(buildingPointer=hex(building), nativeId=pointers.get(actor)))
        token = w.u.hook_add(UC_HOOK_CODE, hook, begin=0x4b3a20, end=0x4b3a20)
        seed = bytes(w.u.mem_read(0x8a5d44, 4))
        try:
            w.call(0x4bca30, site, 1, receiver=0x799895c, count=10000000)
        finally:
            w.u.hook_del(token)
        governor = w.call(0x4c69a0, site, 14) & 0xffffffff
        governor = governor if governor < 0x80000000 else governor - 0x100000000
        assert seed == bytes(w.u.mem_read(0x8a5d44, 4))
        result = dict(case=label, controlledVmFields=changes, previous=previous,
                      before=before, after=[fields(native) for native in [58, 403, 440, 509]],
                      governor=governor, appointments=calls, rngPure=True)
        if assignments or arrivals:
            target=w.call(0x490d00,3,receiver=w.root)
            w.call(0x4bca30,target,1,receiver=0x799895c,count=10000000)
            target_governor=w.call(0x4c69a0,target,14)&0xffffffff
            result['targetGovernor']=target_governor if target_governor<0x80000000 else target_governor-0x100000000
            result['assignmentTrace']=assignment_trace
            if arrivals:result['arrivalTrace']=arrival_trace
            assert seed == bytes(w.u.mem_read(0x8a5d44, 4))
        if site_armies:result['siteArmyTrace']=site_army_trace
        if site_transitions or site_captures:result['transitionTrace']=transition_trace
        if site_captures:result['ownershipHelperMode']=mode
        if provider_evidence is not None:result['providerFixture']=provider_evidence
        if recruitment:result['recruitmentTrace']=recruitment_trace
        results.append(result)
        partial=output.with_suffix('.partial.json');partial.parent.mkdir(parents=True,exist_ok=True)
        partial.write_text(json.dumps(dict(source=source,exeSha=EXE_SHA,completedCases=results,complete=False),ensure_ascii=False,indent=2)+'\n')
        print(json.dumps(dict(case=label, governor=governor)), flush=True)
    report = dict(source=source, exeSha=EXE_SHA, sharedSha=sha(shared), results=results,
                  scope='original full4bca30 election and registration; VM-only controlled inputs; not normal GUI/opening AP or production restoration')
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    options=parser.add_mutually_exclusive_group()
    options.add_argument('--assignments', action='store_true')
    options.add_argument('--site-armies', action='store_true')
    options.add_argument('--site-transitions', action='store_true')
    options.add_argument('--site-captures', action='store_true')
    options.add_argument('--arrivals',action='store_true')
    options.add_argument('--recruitment',action='store_true')
    parser.add_argument('--initialize-provider',action='store_true')
    args = parser.parse_args()
    inspect(args.installation.resolve(), args.output.resolve(),args.assignments,args.site_armies,args.site_transitions,args.site_captures,args.initialize_provider,args.arrivals,args.recruitment)
