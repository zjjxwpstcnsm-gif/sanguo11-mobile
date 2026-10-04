#!/usr/bin/env python3
"""Named city/district/force properties in one original layered registry.

This reports read-boundary evidence, not completed opening state. Original
numeric getters and labels execute or are read after their original initializer;
no online table, replacement owner, invented value or runtime import is used.
"""
import argparse
import collections
import gc
import gzip
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_MEM_READ, UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_EIP
from inspect_pc_layered_scenario import NativeLayeredWorld
from audit_pc_restoration_sources import EXE_SHA, output_guard, json_bytes, sha

# Initializers, descriptors and getter dispatch independently inspected in the
# pinned executable. Dynamic diplomatic labels also come from original globals.
DOMAINS = (
    ('city', 0x1d8, 0x248, 42, 0x73bfa0, 0x8a6d70, 130, 0x4c0c30),
    ('district', 0xb20c, 0x50, 47, 0x73c2b0, 0x8a7c50, 115, 0x4c2960),
    ('force', 0x7af8, 0x12c, 47, 0x73c500, 0x8a9190, 130, 0x4c4260),
)


class NativeScenarioFields(NativeLayeredWorld):
    def __init__(self, exe):
        super().__init__(exe)
        self.collect = False
        self.read_addresses = set()
        self.writes = []
        self.observation_hooks = []
        self.descriptors = {}
        for kind, offset, stride, count, initializer, base, fields, getter in DOMAINS:
            self.call(initializer)
            self.descriptors[kind] = [self.descriptor(base + i * 16, i)
                                      for i in range(fields)]
        # Force properties130..270 have original dynamic labels, not synthetic
        # interpretations of bytes. The native getter uses a target force index.
        for start, pointer in ((130, 0x8ae9e0), (177, 0x8ae9e8), (224, 0x8ae9e4)):
            label_pointer = struct.unpack('<I', self.u.mem_read(pointer, 4))[0]
            label = self.text(label_pointer)
            self.descriptors['force'].extend(dict(id=start + i, name=label,
                originalLabelPointer=hex(label_pointer), targetNativeForceId=i,
                dynamicDescriptor='4c3fb0') for i in range(47))

    def text(self, pointer):
        if not pointer:
            return None
        if not 0x400000 <= pointer < 0x900000:
            raise ValueError('Unexamined descriptor label pointer ' + hex(pointer))
        return bytes(self.u.mem_read(pointer, 256)).split(b'\0')[0].decode('big5')

    def descriptor(self, address, index):
        pointer, kind, subtype, flags = struct.unpack('<4I', self.u.mem_read(address, 16))
        return dict(id=index, name=self.text(pointer), kind=kind, subtype=subtype,
                    flags=flags, originalLabelPointer=hex(pointer))

    def observe_read(self, u, access, address, size, value, user):
        if self.collect:
            self.read_addresses.update(range(address, address + size))

    def observe_write(self, u, access, address, size, value, user):
        if self.collect:
            self.writes.append(dict(pc=hex(u.reg_read(UC_X86_REG_EIP)),
                                    address=hex(address), bytes=size, value=value))

    def load(self, *args, **kwargs):
        # Read tracing concerns getters only. Removing inactive observers from
        # the large original grid loop changes no emulated instruction/state.
        for hook in self.observation_hooks:
            self.u.hook_del(hook)
        self.observation_hooks = []
        return super().load(*args, **kwargs)

    def observe_properties(self):
        if not self.observation_hooks:
            self.observation_hooks = [
                self.u.hook_add(UC_HOOK_MEM_READ, self.observe_read, begin=0x7200000, end=0x74fffff),
                self.u.hook_add(UC_HOOK_MEM_WRITE, self.observe_write, begin=0x7200000, end=0x74fffff)]

    @staticmethod
    def source_addresses(layer, source_path):
        result = {}
        for record in layer['records']:
            for request in record['reads']:
                if not request['destination'].startswith('actor+'):
                    continue
                destination = record['actor_address'] + int(request['destination'][6:], 16)
                for i in range(request['bytes']):
                    result[destination + i] = dict(source=source_path,
                        offset=request['offset'] + i, kind=record['kind'],
                        nativeId=record['native_index'], recordSha256=record['sha256'])
        return result

    def properties(self, actor, stride, getter, descriptors, addresses):
        self.observe_properties()
        result = {}
        active = bool(self.call(0x47a630, actor))
        for descriptor in descriptors:
            field = descriptor['id']
            if not active:
                result[str(field)] = dict(unknown='native active-actor gate rejects actor; zero is not a field value')
                continue
            self.read_addresses = set()
            self.writes = []
            rng = bytes(self.u.mem_read(0x8a5d44, 4))
            aggregate = getter == 0x4c2960 and field in (14, 16, 17)
            aggregate_before = None
            if aggregate:
                # These computed finance getters scan the complete registry.
                # Execute all original instructions, verifying the full world
                # instead of collecting redundant per-byte dependencies. Their
                # returned aggregate is NOT a serialized source value.
                aggregate_before = bytes(self.u.mem_read(0x7200000, 0x300000))
                for hook in self.observation_hooks:
                    self.u.hook_del(hook)
                self.observation_hooks = []
            self.collect = True
            try:
                value = self.call(getter, actor, field, count=2000000 if aggregate else 300000)
                value = value if value < 0x80000000 else value - 0x100000000
                evidence = [dict(address=hex(a), **addresses[a])
                            for a in sorted(self.read_addresses) if a in addresses]
                result[str(field)] = dict(value=value, getter=hex(getter),
                    actorReadOffsets=sorted(a - actor for a in self.read_addresses
                                             if actor <= a < actor + stride),
                    readsSerializedAddresses=evidence,
                    status='original_getter_at_layered_read_boundary', completeOpening=False,
                    sourceValueProven=False)
                if aggregate:
                    result[str(field)].update(originalInstructionBudget=2000000,
                        dependencyTrace='not_collected_for_computed_aggregate; full_world_and_rng_verified')
            except Exception as error:
                result[str(field)] = dict(unknown=str(error), stop=hex(self.u.reg_read(UC_X86_REG_EIP)))
            finally:
                self.collect = False
                if aggregate:
                    self.observe_properties()
            if aggregate and aggregate_before != bytes(self.u.mem_read(0x7200000, 0x300000)):
                raise ValueError('Computed aggregate getter mutated full world')
            if self.writes:
                raise ValueError('Property getter mutated layered world: ' + str(self.writes))
            if rng != bytes(self.u.mem_read(0x8a5d44, 4)):
                raise ValueError('Property getter consumed RNG')
        return active, result


def inspect(installation, output, only_source=None):
    installation = installation.resolve()
    output_guard(installation, output)
    exe = (installation / 'san11pk.exe').read_bytes()
    shared_path = 'Media/scenario/Scenario.s11'
    shared_raw = (installation / shared_path).read_bytes()
    paths = sorted((p for p in (installation / 'Media/scenario').iterdir()
                    if p.name.lower().startswith('scen0') and p.suffix.lower() == '.s11'),
                   key=lambda p: p.name.lower())
    if len(paths) != 16:
        raise ValueError('Source discovery changed; expected16 independently audited candidates')
    sources = []
    counts = collections.Counter()
    for path in paths:
        relative = path.relative_to(installation).as_posix()
        if only_source and relative != only_source:
            continue
        # A fresh complete constructor for each alternative. Within that world,
        # Shared and the selected scenario use the SAME native registries.
        world = NativeScenarioFields(exe)
        shared = world.load(shared_raw, True)
        scenario = world.load(path.read_bytes())
        addresses = world.source_addresses(shared, shared_path)
        addresses.update(world.source_addresses(scenario, relative))
        before = bytes(world.u.mem_read(0x7200000, 0x300000))
        domains = {}
        for kind, offset, stride, count, initializer, base, fields, getter in DOMAINS:
            actors = []
            selected = {'city': list(range(3, 6)) + list(range(7, 19)) + list(range(22, 25)) + [30, 38, 39, 40, 43] + list(range(53, 62)),
                        'district': list(range(3, 19)),
                        'force': list(range(3, 21)) + [35] + list(range(130, 271))}[kind]
            invoked = [d for d in world.descriptors[kind] if d['id'] in selected]
            for native in range(count):
                actor = world.root + offset + stride * native
                active, values = world.properties(actor, stride, getter,
                                                  invoked, addresses)
                counts[kind + '_actors'] += 1
                counts[kind + '_active'] += active
                counts['getter_gate_rejected' if not active else 'getter_execution_unknown'] += sum('unknown' in v for v in values.values())
                counts['getter_values'] += sum('value' in v for v in values.values())
                actors.append(dict(nativeId=native, activeAtReadBoundary=active,
                    properties=values, coverage=dict(complete=False,
                        notYetInvokedPropertyIds=[d['id'] for d in world.descriptors[kind] if d['id'] not in selected],
                        unknown=['complete_postload', 'menu_settings', 'opening_events',
                                 'active_resource_priority', 'reference_identity_joins',
                                 'source_value_vs_validity_read_dependency'])))
            domains[kind] = actors
            print(json.dumps(dict(source=relative, domain=kind, actors=len(actors))), flush=True)
        # Gate/port resources must use their shared site getter dispatch, not
        # city-only fields. Keep current proven ownership and raw provenance.
        sites = []
        for kind, offset, stride, count in (('gate', 0x61a8, 0x90, 10),
                                            ('port', 0x6748, 0x90, 35)):
            for native in range(count):
                actor = world.root + offset + stride * native
                name_raw = bytes(world.u.mem_read(actor + 4, 5)).split(b'\0')[0]
                try:
                    name = name_raw.decode('big5')
                except UnicodeDecodeError:
                    name = None
                owner = world.scalar(0x47b2b0, actor)
                sites.append(dict(kind=kind, nativeId=native, name=name, nameRawHex=name_raw.hex(),
                    ownerNativeForceId=owner, getter='47b2b0', completeOpening=False,
                    unknown=['full_site_property_dispatch', 'postload', 'opening_events']))
        if before != bytes(world.u.mem_read(0x7200000, 0x300000)):
            raise ValueError('Named getter block changed native world')
        sources.append(dict(path=relative, sha256=sha(path.read_bytes()),
            sharedSha256=sha(shared_raw), completeOpening=False, domains=domains, gatePorts=sites,
            nativeWorldReadOnly=True, rngUnchanged=True,
            layeredReadEvidence=dict(sharedBytes=shared['readEnd'], scenarioBytes=scenario['readEnd'],
                sharedWorldSha256=shared['worldSha256'], scenarioWorldSha256=scenario['worldSha256'])))
        descriptors = world.descriptors
        print(json.dumps(dict(source=relative, doneSources=len(sources))), flush=True)
        del world
        gc.collect()
    if not sources:
        raise ValueError('No selected source')
    report = dict(schema=1, sourceExecutableSha256=EXE_SHA, descriptors=descriptors,
        counts=dict(counts), sources=sources, limits=[
            'Every value is an original getter at the layered read boundary, not startup truth',
            'Serialized-address reads alone do not prove value provenance: validity/reference gates remain explicit',
            'Computed capacity, income, troops, action flags and zero XP cannot be silently imported',
            'Native reference IDs are not Android entity IDs; source differences stay separate',
            'PC install read only; no Wine; no gameplay or old save changed'])
    output.parent.mkdir(parents=True, exist_ok=True)
    payload = json_bytes(report)
    output.write_bytes(gzip.compress(payload, mtime=0))
    output.with_suffix('.summary.json').write_bytes(json_bytes(dict(schema=1,
        counts=report['counts'], packedSha256=sha(output.read_bytes()), decodedSha256=sha(payload),
        sources=[dict(path=s['path'], sha256=s['sha256']) for s in sources], limits=report['limits'])))
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--source')
    args = parser.parse_args()
    inspect(args.installation, args.output, args.source)
