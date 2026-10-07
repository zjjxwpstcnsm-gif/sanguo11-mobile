#!/usr/bin/env python3
"""Append exact committed gaiji voice types; no caller or speaker inference."""
import argparse
import copy
import gzip
import hashlib
import json
import pathlib


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def key(row):
    return row['officerId'], row['nativeId'], row['sourceVariant']


def main():
    p = argparse.ArgumentParser()
    for name in ['portrait', 'voice', 'native', 'authority', 'source-fields', 'installation', 'output']:
        p.add_argument('--'+name, type=pathlib.Path, required=True)
    args = p.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    portrait_raw = args.portrait.read_bytes()
    portrait = json.loads(portrait_raw)
    original_raw = args.voice.read_bytes()
    voice = json.loads(original_raw)
    old_rows = copy.deepcopy(voice['identities'])
    old_array_raw = json.dumps(old_rows, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
    if old_array_raw not in original_raw:
        raise ValueError('Existing row bytes use an unexamined serialization')
    native_raw = gzip.decompress(args.native.read_bytes())
    native = json.loads(native_raw)
    authority_raw = args.authority.read_bytes()
    authority = json.loads(authority_raw)
    approved = portrait['supplementalIdentityAuthority']
    if sha(native_raw) != approved['nativePortraitReportSha256'] or sha(authority_raw) != approved['metadataSha256']:
        raise ValueError('Committed supplemental provenance differs')
    if native['requestCount'] != 64 or authority['producer'] != 'session1 identity authority':
        raise ValueError('Original64 committed source reports required')
    if len(old_rows) != 10656 or len(portrait['identities']) != 10720:
        raise ValueError('Exact before/after source extents required')
    if voice['sourceExecutableSha256'] != native['sourceExecutableSha256']:
        raise ValueError('Original executable provenance differs')
    facts = {}
    for line in args.source_fields.read_text().splitlines():
        fields = line.split('\t')
        if len(fields) != 7:
            raise ValueError('Invalid complete source metadata observation')
        k = int(fields[0]), int(fields[1]), fields[2]
        if k in facts:
            raise ValueError('Duplicate current source identity')
        facts[k] = fields[3], fields[4], fields[5], int(fields[6])
    current = {key(r): r for r in portrait['identities']}
    old = {key(r): r for r in old_rows}
    supplemental = {key(r): r for r in native['entries']}
    identity_authority = {key(r): r for r in authority['rows']}
    if len(current) != 10720 or set(facts) != set(current) or set(current)-set(old) != set(supplemental) or set(supplemental) != set(identity_authority):
        raise ValueError('Complete exact source identity joins missing')
    files = {}
    for k, row in current.items():
        path, source_sha, record_sha, field48 = facts[k]
        if (path, source_sha, record_sha, field48) != (row['sourcePath'], row['sourceSha256'], row['recordSha256'], row['voiceTypeRaw']):
            raise ValueError('Saved raw field48 differs from original actor+0x100 voice type')
        if path not in files:
            raw = (args.installation/path).read_bytes()
            if len(raw) != 170010 or raw[:8] != bytes.fromhex('0000feff16000000') or sha(raw) != source_sha:
                raise ValueError('Actual readonly original scenario SHA/header differs')
            files[path] = raw
        record = files[path][17760+152*row['nativeId']:17760+152*(row['nativeId']+1)]
        if sha(record) != record_sha or field48 not in range(8):
            raise ValueError('Original152-byte record or voice type differs')
        if k in old and any(old[k][name] != row[name] for name in old[k]):
            raise ValueError('Existing approved voice identity changed')
    fields = ['officerId', 'nativeId', 'sourceVariant', 'sourcePath', 'sourceSha256', 'recordSha256', 'identityStatus', 'voiceTypeRaw']
    for row in native['entries']:
        k = key(row)
        for name in ['sourcePath', 'sourceSha256', 'recordSha256', 'voiceTypeRaw']:
            if row[name] != current[k][name]:
                raise ValueError('Native supplemental source differs')
        for name in ['sourcePath', 'sourceSha256', 'recordSha256']:
            if row[name] != identity_authority[k][name]:
                raise ValueError('Committed identity authority differs')
        voice['identities'].append({name: row[name] for name in fields})
    if voice['identities'][:10656] != old_rows or len({key(r) for r in voice['identities']}) != 10720:
        raise ValueError('Existing rows/order changed or new source extent invalid')
    if not json.dumps(voice['identities'], ensure_ascii=False, separators=(',', ':')).encode('utf-8').startswith(old_array_raw[:-1]):
        raise ValueError('Existing identity row bytes changed')
    voice['approvedMediaManifestSha256'] = sha(portrait_raw)
    voice['supplementalIdentityAuthority'] = approved
    result_raw = (json.dumps(voice, ensure_ascii=False, separators=(',', ':'))+'\n').encode('utf-8')
    (args.output/'voice-identities.json').write_bytes(result_raw)
    (args.output/'voice-identities-before.json').write_bytes(original_raw)
    report = {'beforeSha256': sha(original_raw), 'afterSha256': sha(result_raw),
              'existingRowsByteEquivalent': True, 'existingIdentityRows': 10656,
              'identityRows': 10720, 'addedRows': 64, 'actualOriginalSourceFiles': len(files),
              'original152ByteRecordsShaVerified': 10720, 'savedField48EqualsOriginalVoiceType': 10720,
              'portraitManifestSha256': sha(portrait_raw), 'sourceFieldsSha256': sha(args.source_fields.read_bytes()),
              'supplementalIdentityAuthority': approved, 'originalOggModified': False,
              'scope': 'Exact source/voice type metadata only; no Android caller, actual speaker, action profile, voice playback, MOD activation or ARM acceptance',
              'wholeGoalComplete': False}
    (args.output/'report.json').write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps(report))


if __name__ == '__main__':
    main()
