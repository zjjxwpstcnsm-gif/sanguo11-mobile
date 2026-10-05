#!/usr/bin/env python3
"""Original installed event assembly and isolated native world postload.

This is deliberately NOT complete startup. Shared NLS sorting, menu choices,
external resource activation and event execution are unresolved. Outputs stay
audit-only until their dependencies are proven; no APK/save content is changed.
"""
import argparse
import gc
import gzip
import json
import struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_FPCW
from inspect_pc_layered_scenario import NativeLayeredWorld
from pc_startup_platform import StartupPlatform
from audit_pc_restoration_sources import EXE_SHA, output_guard, json_bytes, sha

MANAGER = 0x9c41a00
PROPERTY_IDS = list(range(30, 35)) + [40, 52, 58, 59, 60, 61, 62, 88, 89] + list(range(106, 122)) + list(range(128, 136))
LIMITS = ['Shared original Chinese NLS sorting not executed',
          'Windows user Documents/configuration/Expansion contents unknown',
          'Menu settings and active resource priority not proven',
          'Event metadata and lazy bodies loaded; conditions/effects not executed',
          'Native postload called directly, not complete normal game start',
          'No source values imported into Android worlds or old saves']


def uint(world, address):
    return struct.unpack('<I', world.u.mem_read(address, 4))[0]


def value(world, actor, field):
    result = world.call(0x4c8720, actor, field)
    return result if result < 0x80000000 else result - 0x100000000


def assemble_events(world, platform, read_bodies=True):
    """Execute original manager, then its original descriptor/path/FILE readers.

    Metadata offsets and twelve-byte lazy references are actual native outputs,
    not a Python reimplementation of the event parser or condition evaluator.
    """
    if world.call(0x679cb0, world.root + 0x1bd9ac + 0xc, 0,
                  receiver=MANAGER, count=100000000) != 1:
        raise ValueError('Original installed event manager assembly failed')
    pointer, count, data, data_bytes = struct.unpack('<4I', world.u.mem_read(MANAGER, 16))
    if count != 326 or data_bytes != 216908:
        raise ValueError('Unexamined installed event assembly size')
    starts = list(struct.unpack('<20I', world.u.mem_read(MANAGER + 0x10, 80)))
    ends = list(struct.unpack('<20I', world.u.mem_read(MANAGER + 0x60, 80)))
    byte_ends = list(struct.unpack('<20I', world.u.mem_read(MANAGER + 0xb0, 80)))
    if starts[0] or starts[1:] != ends[:-1] or ends[-1] != count or byte_ends[-1] != data_bytes:
        raise ValueError('Original section ranges disagree')
    offsets = list(struct.unpack('<%dI' % count, world.u.mem_read(pointer, count * 4)))
    descriptions = uint(world, MANAGER + 0x100)
    scratch = uint(world, MANAGER + 0x104)
    capacity = uint(world, MANAGER + 0x108)
    path_object = world.meta + 0x4100
    size_pointer = world.meta + 0x4300
    world.call(0x55cb30, receiver=path_object)
    resources = {}
    for call in platform.calls:
        if call['function'] == 'fopen' and call['result']:
            resources[call['sourcePath']] = dict(path=call['sourcePath'], sha256=call['sha256'], bytes=call['bytes'])
    rows = []
    for section in range(20):
        previous_bytes = byte_ends[section - 1] if section else 0
        if starts[section] != ends[section] and offsets[starts[section]] != previous_bytes:
            raise ValueError('Original section metadata origin differs')
        for index in range(starts[section], ends[section]):
            stop = offsets[index + 1] if index + 1 < ends[section] else byte_ends[section]
            if not previous_bytes <= offsets[index] < stop <= byte_ends[section]:
                raise ValueError('Original event metadata range invalid')
            metadata = bytes(world.u.mem_read(data + offsets[index], stop - offsets[index]))
            descriptor = world.call(0x678480, index, receiver=MANAGER)
            if descriptor != descriptions + index * 12:
                raise ValueError('Original lazy descriptor accessor disagrees')
            resource, file_offset, stored_bytes = struct.unpack('<3I', world.u.mem_read(descriptor, 12))
            world.call(0x678870, path_object, resource)
            path_pointer, path_capacity, path_length = struct.unpack('<3I', world.u.mem_read(path_object, 12))
            if path_length >= path_capacity or path_capacity != 0x104:
                raise ValueError('Original event path escaped bounded object')
            virtual_path = bytes(world.u.mem_read(path_pointer, path_length)).decode('ascii')
            source = platform.path(virtual_path)
            if source is None or not source.is_file():
                raise ValueError('Original installed lazy reference has no source')
            relative = source.relative_to(platform.root).as_posix()
            raw = source.read_bytes()
            if relative not in resources or sha(raw) != resources[relative]['sha256']:
                raise ValueError('Lazy event body source differs from actual manager input')
            if not 0 <= file_offset <= len(raw) or not 0 < stored_bytes <= capacity or file_offset + stored_bytes > len(raw):
                raise ValueError('Original lazy body reference escaped source')
            row = dict(stableId='pc-installed-event-v1:%s:%d' % (resources[relative]['sha256'], file_offset),
                       originalManagerIndex=index, section=section, indexWithinSection=index - starts[section],
                       sourcePath=relative, sourceSha256=resources[relative]['sha256'], nativeResourceId=resource,
                       sourceFileOffset=file_offset, storedBodyBytes=stored_bytes,
                       metadataOffset=offsets[index], metadataBytes=len(metadata), metadataSha256=sha(metadata),
                       metadataHex=metadata.hex(), conditionsExecuted=False, effectsExecuted=False,
                       actualActivationProven=False)
            if read_bodies:
                world.u.mem_write(size_pointer, struct.pack('<I', stored_bytes))
                got = world.call(0x46dc70, path_pointer, scratch, capacity, file_offset, size_pointer)
                body = bytes(world.u.mem_read(scratch, stored_bytes))
                if got != stored_bytes or body != raw[file_offset:file_offset + stored_bytes]:
                    raise ValueError('Original body reader differs from actual source span')
                row.update(bodyReadFunction='46dc70', bodySha256=sha(body), bodyHex=body.hex())
            rows.append(row)
    if platform.files or platform.directories:
        raise ValueError('Original event reader left fixture IO handles open')
    return dict(originalFunction='679cb0', result=1, completeStartup=False,
                sourceResources=sorted(resources.values(), key=lambda r: r['path']),
                sectionRecordCounts=[end - start for start, end in zip(starts, ends)],
                recordCount=count, decodedMetadataBytes=data_bytes, originalLazyBodyReferences=rows,
                originalBodyReaderInvoked=read_bodies, openingEventsExecuted=False)


def descriptors(world):
    world.call(0x73ca80)
    result = []
    for field in PROPERTY_IDS:
        pointer, kind, subtype, flags = struct.unpack('<4I', world.u.mem_read(0x8ab758 + field * 16, 16))
        label = bytes(world.u.mem_read(pointer, 128)).split(b'\0')[0].decode('big5')
        result.append(dict(id=field, name=label, kind=kind, subtype=subtype, flags=flags, originalLabelPointer=hex(pointer)))
    return result


def ability_state(world, actor, native_calculation=False):
    """Retain actual cached bytes and compare the original ability function.

    Layout/function provenance is independently established in inherited R19;
    source values and menu-dependent computed values stay distinct here.
    """
    state = dict(base=list(bytes(world.u.mem_read(actor + 0xc8, 5))),
        growthCodes=list(struct.unpack('<5i', world.u.mem_read(actor + 0xd0, 20))),
        experience=list(struct.unpack('<5H', world.u.mem_read(actor + 0x12a, 10))),
        condition=uint(world, actor + 0x15c),
        cachedWithCondition=list(bytes(world.u.mem_read(actor + 0x170, 5))),
        cachedWithoutCondition=list(bytes(world.u.mem_read(actor + 0x175, 5))),
        completeOpening=False, currentValuesSourceProven=False)
    if native_calculation:
        active = bool(world.call(0x47a630, actor))
        with_condition = [world.call(0x48a110, stat, state['condition'], receiver=actor) & 255 for stat in range(5)]
        without = [world.call(0x48a110, stat, 0xffffffff, receiver=actor) & 255 for stat in range(5)]
        if active and (with_condition != state['cachedWithCondition'] or without != state['cachedWithoutCondition']):
            raise ValueError('Original postload cache differs from original current-ability calculation: actor=%x cached=%r/%r calculated=%r/%r' % (actor, state['cachedWithCondition'], state['cachedWithoutCondition'], with_condition, without))
        state.update(originalCurrentAbilityFunction='48a110', nativeComputedWithCondition=with_condition,
                     nativeComputedWithoutCondition=without, nativePostloadActive=active, nativeCacheComparison=active)
        if not active:
            state['unknown'] = 'Original488430 activity gate skips cache refresh for status6/8; cached zeros are not current ability truth'
    return state


def inspect(installation, output, only_source=None):
    installation = installation.resolve()
    output_guard(installation, output)
    exe = (installation / 'san11pk.exe').read_bytes()
    identity_path = Path(__file__).resolve().parents[2] / 'docs/pc-data/scenario-officers-native.json.gz'
    packed_identity = identity_path.read_bytes()
    identity = json.loads(gzip.decompress(packed_identity))
    if identity['source_executable_sha256'] != EXE_SHA:
        raise ValueError('Identity audit executable changed')
    mappings = {(r['source'], r['native_index']): r for r in identity['mappings']}
    manifest_path = identity_path.parents[1] / 'handoff/20261004/session1/source-manifest.json'
    manifest_bytes = manifest_path.read_bytes()
    variants = {s['sourcePath']: s for s in json.loads(manifest_bytes)['scenarios']}
    shared = (installation / 'Media/scenario/Scenario.s11').read_bytes()
    sources = []
    event_report = None
    for source in identity['sources']:
        if only_source and source['path'] != only_source:
            continue
        raw = (installation / source['path']).read_bytes()
        if sha(raw) != source['sha256']:
            raise ValueError('Scenario differs from identity audit')
        variant = variants[source['path']]
        if variant['sourceSha256'] != sha(raw):
            raise ValueError('Stable scenario manifest differs from source')
        world = NativeLayeredWorld(exe)
        platform = StartupPlatform(world, installation)
        world.load(shared, True)
        world.load(raw)
        labels = descriptors(world)
        before_fields = [[value(world, world.root + 0xc0bc + i * 0x190, f) for f in PROPERTY_IDS] for i in range(850)]
        before_abilities = [ability_state(world, world.root + 0xc0bc + i * 0x190) for i in range(850)]
        before = bytes(world.u.mem_read(0x7200000, 0x300000))
        rng_before = bytes(world.u.mem_read(0x8a5d44, 4))
        events = assemble_events(world, platform, read_bodies=event_report is None)
        if event_report is None:
            event_report = events
        elif [{k: v for k, v in r.items() if k not in ('bodyReadFunction', 'bodySha256', 'bodyHex')}
              for r in event_report['originalLazyBodyReferences']] != events['originalLazyBodyReferences']:
            raise ValueError('Scenario event metadata/lazy provenance changed')
        postload = world.call(0x493400, receiver=world.root, count=50000000)
        after = bytes(world.u.mem_read(0x7200000, 0x300000))
        query_before = after
        query_rng = bytes(world.u.mem_read(0x8a5d44, 4))
        officers = []
        for native in range(850):
            actor = world.root + 0xc0bc + native * 0x190
            mapped = mappings[(source['path'], native)]
            valid = bool(world.call(0x47a600, actor))
            fields = {str(f): dict(before=old, after=value(world, actor, f), sourceValueProven=False,
                                  completeOpening=False, scope='isolated native postload fixture')
                      for f, old in zip(PROPERTY_IDS, before_fields[native])}
            officers.append(dict(nativeId=native, officerId=mapped['project_id'], identity=mapped['identity'],
                sourceVariant=variant['sourceVariant'],
                postloadValid=valid, properties=fields,
                abilityBefore=before_abilities[native], abilityAfter=ability_state(world, actor, valid),
                changedPropertyIds=[int(k) for k, v in fields.items() if v['before'] != v['after']],
                coverageComplete=False, unknown=LIMITS[:5]))
        if query_before != bytes(world.u.mem_read(0x7200000, 0x300000)) or query_rng != bytes(world.u.mem_read(0x8a5d44, 4)):
            raise ValueError('Native postload getters mutated world/RNG')
        sources.append(dict(path=source['path'], sha256=sha(raw), sharedSha256=sha(shared),
            sourceVariant=variant['sourceVariant'], scenarioId=variant['scenarioId'],
            originalPostloadFunction='493400', originalPostloadReturn=postload,
            beforeWorldSha256=sha(before), afterWorldSha256=sha(after),
            originalX87ControlWord=hex(world.u.reg_read(UC_X86_REG_FPCW)),
            rngBeforeHex=rng_before.hex(), rngAfterHex=query_rng.hex(),
            getterWorldAndRngUnchanged=True, completeStartup=False, officers=officers,
            platformCalls=platform.calls))
        print(json.dumps(dict(source=source['path'], postloadReturn=postload, officers=len(officers),
                              changedOfficers=sum(bool(o['changedPropertyIds']) for o in officers),
                              nativeAbilityCachesChecked=sum(o['abilityAfter'].get('nativeCacheComparison', False) for o in officers))), flush=True)
        del world, platform
        gc.collect()
    if not sources:
        raise ValueError('No audited scenario source matched')
    report = dict(schema=1, sourceExecutableSha256=EXE_SHA, identityAuditSha256=sha(packed_identity),
        stableSourceManifestSha256=sha(manifest_bytes),
        completeStartup=False, androidImportPerformed=False, originalPropertyDescriptors=labels,
        installedEventAssembly=event_report, sources=sources, limits=LIMITS,
        fixture=dict(documents='G:\\__fixture_documents__', externalCoverage='unknown; user has no other local copy',
                     actualDocumentsSuffix='Koei\\San11 Tc\\Expansion', originalCRTThreadInitializer='70e5c0',
                     originalCRTFloatInitializer='707075(1)', originalCRTCodePageInitializer='709048(950)',
                     chineseNlsCollation='unsupported; no ordinal replacement', wineStarted=False))
    payload = json_bytes(report)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(gzip.compress(payload, mtime=0))
    output.with_suffix('.summary.json').write_bytes(json_bytes(dict(schema=1, packedSha256=sha(output.read_bytes()),
        decodedSha256=sha(payload), sourceCount=len(sources), sourceExecutableSha256=EXE_SHA,
        eventRecordCount=event_report['recordCount'], eventSectionCounts=event_report['sectionRecordCounts'],
        lazyBodiesRead=sum('bodyHex' in r for r in event_report['originalLazyBodyReferences']),
        queriedOfficerSlots=sum(len(s['officers']) for s in sources), completeStartup=False, limits=LIMITS)))
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--source')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    inspect(args.installation, args.output, args.source)
