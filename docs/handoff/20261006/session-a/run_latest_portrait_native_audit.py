#!/usr/bin/env python3
"""Reexecute supplied portrait serializer/age slices for current exact identities.

No Android actions, World/rule RNG, Wine, UI-role or live MOD inference.
"""
import argparse
import gzip
import hashlib
import importlib.util
import json
import pathlib
import struct
import sys

sys.dont_write_bytecode = True
ROOT = pathlib.Path(__file__).resolve().parents[4]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def key(row):
    return row['officerId'], row['nativeId'], row['sourceVariant']


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--installation', type=pathlib.Path, required=True)
    p.add_argument('--output', type=pathlib.Path, required=True)
    p.add_argument('--receipt', type=pathlib.Path, required=True)
    a = p.parse_args()
    if not a.output.resolve().is_relative_to(ROOT/'out/session-a') or a.output.exists():
        raise ValueError('Fresh isolated output required')
    if a.receipt.parent.resolve() != pathlib.Path(__file__).resolve().parent or a.receipt.exists():
        raise ValueError('Fresh owned receipt required')
    manifest = ROOT/'app/src/main/assets/portraits/pc/media-manifest.json'
    original_requests = ROOT/'docs/handoff/20261004/session1/portrait-requirements.json.gz'
    supplemental_requests = ROOT/'docs/handoff/20261004/session2/portrait-gaiji-requests-30.json'
    pixels = ROOT/'docs/handoff/20261004/session2/portrait-pixels-current.json.gz'
    inspector = ROOT/'tools/media/inspect_pc_portrait_inputs.py'
    exe = a.installation/'san11pk.exe'
    pixel_data = json.loads(gzip.decompress(pixels.read_bytes()))
    face = a.installation/pixel_data['sourceFile']
    guarded = [manifest, original_requests, supplemental_requests, pixels, inspector, exe, face]
    before = {str(path): sha(path) for path in guarded}
    current = json.loads(manifest.read_bytes())
    identities = {key(r): r for r in current['identities']}
    assert len(identities) == len(current['identities']) == 10720
    requests = json.loads(gzip.decompress(original_requests.read_bytes()))['requests']
    requests += json.loads(supplemental_requests.read_bytes())['requests']
    approved = {}
    for row in requests:
        k = key(row)
        if k not in identities:
            continue
        if k in approved and row != approved[k]:
            raise ValueError('Ambiguous authority request')
        person = identities[k]
        for field in ('sourcePath', 'sourceSha256', 'recordSha256', 'birth'):
            assert row[field] == person[field], (k, field)
        assert row['faceNativeId'] == person['faceId'], k
        approved[k] = row
    assert set(approved) == set(identities)
    a.output.mkdir(parents=True)
    request_path = a.output/'latest-approved-requests.json'
    request_path.write_text(json.dumps(dict(schema=1, requests=list(approved.values())), ensure_ascii=False)+'\n')
    pixel_path = a.output/'source-pixels.json'
    pixel_path.write_text(json.dumps(pixel_data, ensure_ascii=False)+'\n')
    spec = importlib.util.spec_from_file_location('readonly_portrait_inspector', inspector)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    result = a.output/'native-result.json'
    module.inspect(a.installation, request_path, pixel_path, result,
                   current['metadataCommit']+' + '+current['supplementalIdentityAuthority']['commit'])
    native = json.loads(result.read_bytes())
    assert native['requestCount'] == 10720 and native['effectiveRuntimeOfficerCoverage'] == 0
    images = {(r['faceId'], r['imageGroup']): r for r in current['images']}
    sources, gaiji, boundaries = {}, [], 0
    for row in native['entries']:
        person = identities[key(row)]
        for field in ('faceId', 'sexRaw', 'birth', 'ageThreshold', 'dynamicSelector', 'voiceTypeRaw', 'ageBoundaries'):
            assert row[field] == person[field], (key(row), field)
        boundaries += len(row['ageBoundaries'])
        sources[row['sourcePath']] = sources.get(row['sourcePath'], 0)+1
        for form in row['forms']:
            if form.get('asset'):
                image = images[(form['faceId'], form['imageGroup'])]
                # Export-relative filenames differ from packaged asset names;
                # bind by original face/group and full pixel/file hashes.
                for field in ('pngSha256', 'rgbaSha256', 'width', 'height'):
                    assert form[field] == image[field], (key(row), field)
                assert image['asset'] == 'portraits/pc/group-%d/face-%04d.png' % (form['imageGroup'], form['faceId'])
        if row['officerId'] in (156234, 844857, 598828, 850922):
            gaiji.append({field: row[field] for field in ('officerId','nativeId','sourcePath','recordSha256','ageBoundaries')})
    assert len(sources) == 16 and set(sources.values()) == {670}
    assert len(gaiji) == 64 and boundaries == 32160
    for image in current['images']:
        assert sha(ROOT/'app/src/main/assets'/image['asset']) == image['pngSha256']
    assert {str(path): sha(path) for path in guarded} == before
    pe = struct.unpack_from('<I', exe.read_bytes(), 0x3c)[0]
    report = dict(scope='Fresh supplied original serializer, face/age lookup and all source image groups for current committed identity requests; no normal Android/PC UI caller, crop/color/fullscreen/timing, MOD reachability, rule commands or RNG acceptance.',
        sourceReadOnly=True, wineUsed=False, guardedInputs=before,
        currentIdentities=10720, sources=sources, gaijiRecords=gaiji,
        nativeUniqueRecords=native['uniqueNativeRecords'], nativeAgeSlicesExecuted=native['nativeAgeBoundaryChecks'],
        comparedIdentityAgeVectors=boundaries, allCurrentNativeFieldsEqual=True,
        originalPngShaVerified=len(current['images']), outputs={str(path):sha(path) for path in (request_path,pixel_path,result)},
        sourcePeHeaderOffset=pe, old652And4CountsReused=False,
        normalAndroidCallerCoverageAccepted=False, fullScreenCropTimingAccepted=False,
        wholeGoalComplete=False)
    a.receipt.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:report[k] for k in ('currentIdentities','nativeUniqueRecords','nativeAgeSlicesExecuted','comparedIdentityAgeVectors','allCurrentNativeFieldsEqual','originalPngShaVerified')}))


if __name__ == '__main__':
    main()
