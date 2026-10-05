#!/usr/bin/env python3
"""Import hidden original record facts with actual serializer/identity checks.

No inferred IDs; read-only installation. Snapshot facts are fresh-game data,
not a restore-time override or a replacement for current campaign loyalty.
"""
import argparse,gzip,json,struct
from pathlib import Path
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,json_bytes,output_guard
from inspect_pc_scenario_officers import NativeOfficerDecoder
INPUT_SHA='e4341546432e8d257851ed830bc3388d1f8ce8697db1547e108285d2b3b68d2c'
def build(installation,input_report,output):
    installation=installation.resolve();output_guard(installation,output);exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Original EXE changed')
    packed=input_report.read_bytes()
    if sha(packed)!=INPUT_SHA:raise ValueError('Original identity report differs')
    r=json.loads(gzip.decompress(packed));manifest_raw=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes()
    if r['sourceExecutableSha256']!=EXE_SHA or r['sourceManifestSha256']!=sha(manifest_raw):raise ValueError('Original provenance differs')
    manifest={s['sourcePath']:s for s in json.loads(manifest_raw)['scenarios']};d=NativeOfficerDecoder(exe);data=bytearray();coverage=[]
    def integer(n):data.extend(struct.pack('>i',n))
    def text(s):b=s.encode('utf8');integer(len(b));data.extend(b)
    integer(0x50434631);text(EXE_SHA);text(INPUT_SHA);integer(16)
    for s in r['sources']:
        m=manifest[s['sourcePath']];raw=(installation/s['sourcePath']).read_bytes()
        if sha(raw)!=s['sourceSha256']or any(m[k]!=s[k]for k in ['sourceSha256','sourceVariant','scenarioId'])or len(s['people'])!=850:raise ValueError('Source/record coverage differs')
        for k in ['scenarioId','sourcePath','sourceVariant','sourceSha256']:text(s[k])
        integer(850)
        for native,p in enumerate(s['people']):
            record=raw[p['sourceOffset']:p['sourceOffset']+152]
            if native!=p['nativeId']or sha(record)!=p['recordSha256']:raise ValueError('Record identity differs')
            decoded,actor=d.decode(record,native)
            if ''.join(decoded['name_bytes'])!=p['nameRawHex']or decoded['birth']!=p['birth']or decoded['sex']!=p['sex']or actor[0xac]!=record[104]:raise ValueError('Original serializer/name/birth/sex/raw loyalty differs')
            integer(native);text(p['recordSha256']);text(p['nameRawHex']);integer(p['birth']);integer(p['sex']);data.append(actor[0xac])
        coverage.append(dict(sourceId=s['scenarioId'],sourcePath=s['sourcePath'],sourceVariant=s['sourceVariant'],sourceSha256=s['sourceSha256'],records=850,historicalRecords=670,originalSerializerChecked=True))
        print(json.dumps(dict(source=s['sourcePath'],records=850)),flush=True)
    if len(coverage)!=16:raise ValueError('All16 sources required')
    output.mkdir(parents=True,exist_ok=False);(output/'facts.bin.gz').write_bytes(gzip.compress(data,mtime=0));(output/'index.txt').write_text(sha(data)+'\n')
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,identityReportSha256=INPUT_SHA,sourceManifestSha256=sha(manifest_raw),decodedSha256=sha(data),packedSha256=sha((output/'facts.bin.gz').read_bytes()),coverage=coverage,
        fields=dict(initialRawLoyalty=dict(actorOffset=0xac,sourceRecordOffset=104,type='unsigned-byte',originalGetterDisplayIsSeparate=True)),
        limits=['Original record snapshot only; no current campaign loyalty or restore-time backfill','Extra/template source slots are retained metadata; not newly activated officers'],completeGoal=False)
    (output/'build-report.json').write_bytes(json_bytes(report));print(json.dumps(dict(records=13600,decodedSha256=sha(data))),flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--input-report',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();build(a.installation,a.input_report,a.output)
