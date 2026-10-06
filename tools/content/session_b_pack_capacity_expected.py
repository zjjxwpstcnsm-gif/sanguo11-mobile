#!/usr/bin/env python3
"""Reproduce test evidence from pinned original reports, extracting no policy code."""
import argparse
import gzip
import hashlib
import json
import tarfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ARCHIVE_SHA = 'dd3f2c05f8b7f17aadf2bab73962b3770cf973190563efced773cf63a5c0f27c'
EXE_SHA = '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
SHARED_SHA = 'dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'

def sha(data):
    return hashlib.sha256(data).hexdigest()

def main(output):
    archive = ROOT / 'docs/handoff/20261004/session1/batch28-governor-original-all16.tar.gz'
    raw = archive.read_bytes()
    if sha(raw) != ARCHIVE_SHA:
        raise ValueError('Pinned original report archive changed')
    manifest_raw = (ROOT / 'docs/handoff/20261004/session1/source-manifest.json').read_bytes()
    sources = json.loads(manifest_raw)['scenarios']
    lines = ['sourceId\tnativeId\tcapacity\tstatus\towner\tallowed']
    with tarfile.open(archive, 'r:gz') as tar:
        for index, source in enumerate(sources):
            name = f'source-{index}.json.gz'
            member = tar.getmember(name)
            if not member.isfile():
                raise ValueError('Original report is not a file')
            report = json.loads(gzip.decompress(tar.extractfile(member).read()))
            if (report['schema'] != 2 or report['source'] != source
                    or report['sourceExecutableSha256'] != EXE_SHA
                    or report['sharedSha256'] != SHARED_SHA
                    or report['sourceManifestSha256'] != sha(manifest_raw)
                    or len(report['people']) != 1100):
                raise ValueError('Original source/report identity changed')
            for native_id, person in enumerate(report['people']):
                if person['nativeId'] != native_id:
                    raise ValueError('Original constructed-domain order changed')
                lines.append('\t'.join(str(value) for value in (
                    source['scenarioId'], native_id, person['commandCapacity'],
                    person['status'], person['owner'], person['allowed'])))
    if len(sources) != 16:
        raise ValueError('Original source domain changed')
    data = ('\n'.join(lines) + '\n').encode('ascii')
    compressed = gzip.compress(data, mtime=0)
    output = output.resolve()
    relative = str(output.relative_to(ROOT))
    if (relative != 'core/src/test/resources/pc-command-capacity/original-capacities.tsv.gz'
            and not relative.startswith('out/session-b/')):
        raise ValueError('Exact B output ownership required')
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists() and output.read_bytes() != compressed:
        raise ValueError('Preserve previous evidence')
    output.write_bytes(compressed)
    print(json.dumps(dict(archiveSha256=ARCHIVE_SHA, rawSha256=sha(data),
                         compressedSha256=sha(compressed), sources=16,
                         constructedRows=17600, effectiveCoverage='not implied')))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    main(parser.parse_args().output)
