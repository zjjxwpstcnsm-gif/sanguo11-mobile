#!/usr/bin/env python3
"""Pack pinned original contest traits, retaining per-source record identities."""
import argparse,gzip,json,struct
from pathlib import Path
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha
INPUT_SHA='e4341546432e8d257851ed830bc3388d1f8ce8697db1547e108285d2b3b68d2c'

def build(installation,input_report,output):
    installation=installation.resolve();output_guard(installation,output)
    if sha((installation/'san11pk.exe').read_bytes())!=EXE_SHA:raise ValueError('Executable changed')
    packed=input_report.read_bytes()
    if sha(packed)!=INPUT_SHA:raise ValueError('Original traits report changed')
    report=json.loads(gzip.decompress(packed));manifest_raw=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();manifest=json.loads(manifest_raw)
    if report['sourceExecutableSha256']!=EXE_SHA or report['sourceManifestSha256']!=sha(manifest_raw):raise ValueError('Cross-source report')
    sources={s['sourcePath']:s for s in manifest['scenarios']};data=bytearray()
    def integer(value):data.extend(struct.pack('>i',value))
    def text(value):raw=value.encode('utf8');integer(len(raw));data.extend(raw)
    integer(0x50435031);text(EXE_SHA);text(INPUT_SHA);integer(len(report['sources']));coverage=[]
    for source in report['sources']:
        raw=(installation/source['sourcePath']).read_bytes();m=sources[source['sourcePath']]
        if sha(raw)!=source['sourceSha256']or source['sourceVariant']!=m['sourceVariant']or source['scenarioId']!=m['scenarioId']:raise ValueError('Mixed source')
        if len(source['people'])!=850:raise ValueError('Incomplete original record coverage')
        for value in ['scenarioId','sourcePath','sourceVariant','sourceSha256']:text(source[value])
        integer(850)
        for native,p in enumerate(source['people']):
            record=raw[p['sourceOffset']:p['sourceOffset']+152]
            if native!=p['nativeId']or sha(record)!=p['recordSha256']or not p['queryPure']:raise ValueError('Record provenance changed')
            if not 0<=p['nativePersonality']<4 or not 0<=p['nativeTalkMask']<32 or p['nativeTalkMask']!=sum(v<<i for i,v in enumerate(p['nativeTalkFlags'])):raise ValueError('Unexamined native trait')
            integer(native);text(p['recordSha256']);text(p['nameRawHex']);integer(p['birth']);integer(p['sex']);integer(p['nativePersonality']);integer(p['nativeTalkMask'])
        coverage.append(dict(sourceId=source['scenarioId'],sourcePath=source['sourcePath'],sourceSha256=source['sourceSha256'],records=850,historical=670,extraSlots=180,identityJoin='runtime checked original person record, not arithmetic'))
    if len(coverage)!=16:raise ValueError('All16 independent sources required')
    output.mkdir(parents=True,exist_ok=False);(output/'profiles.bin.gz').write_bytes(gzip.compress(data,mtime=0));(output/'index.txt').write_text(sha(data)+'\n')
    audit=dict(sourceExecutableSha256=EXE_SHA,inputReportSha256=INPUT_SHA,decodedSha256=sha(data),packedSha256=sha((output/'profiles.bin.gz').read_bytes()),sourceCoverage=coverage,completeContestRestoration=False)
    (output/'build-report.json').write_bytes(json_bytes(audit));print(json.dumps(dict(sha256=sha(data),sources=len(coverage),records=13600)));return audit
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--input-report',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();build(a.installation,a.input_report,a.output)
