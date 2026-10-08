#!/usr/bin/env python3
"""Pack all16 completed original classifier receipts, preserving source identity.

No partial receipt, effective activation or normal campaign claim is accepted.
"""
import argparse
import gzip
import json
import struct
from pathlib import Path
from audit_pc_restoration_sources import ROOT, EXE_SHA, sha


def export(folder, output, index):
    if output.exists() or index.exists():
        raise ValueError('Preserve earlier import')
    manifest = (ROOT / 'docs/handoff/20261004/session1/source-manifest.json').read_bytes()
    sources = json.loads(manifest)['scenarios']
    assert len(sources) == 16
    body = bytearray(b'PDK-PACK-1\n')
    receipts = []
    for source_index, source in enumerate(sources):
        raw = (folder / ('source-%02d.json' % source_index)).read_bytes()
        r = json.loads(raw)
        assert r['exeSha'] == EXE_SHA and r['source'] == source
        assert r['rows'] == 670 and r['columns'] == 670 and r['wholeWorldAndRngPure']
        assert not r['completeGoal'] and len(r['rowHex']) == 670
        rows = [bytes.fromhex(row) for row in r['rowHex']]
        assert all(len(row) == 670 for row in rows)
        matrix = b''.join(rows)
        assert len(matrix) == 448900 and set(matrix) <= {0, 1, 2, 3, 255}
        metadata = dict(sourceIndex=source_index, source=source, receiptSha=sha(raw), matrixSha=sha(matrix))
        encoded = json.dumps(metadata, sort_keys=True, separators=(',', ':')).encode('utf-8')
        body.extend(struct.pack('>II', len(encoded), len(matrix)))
        body.extend(encoded)
        body.extend(matrix)
        receipts.append(metadata)
    packed = gzip.compress(bytes(body), mtime=0)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(packed)
    index.write_text(json.dumps(dict(format='PDK-PACK-1', exeSha=EXE_SHA, sourceManifestSha=sha(manifest), sources=receipts, decodedSha=sha(body), packedSha=sha(packed), matrixRows=16*670, matrixEntries=16*448900, limits=['Source identities and variant differences retained independently', 'Registered670 identity matrix is not effective officer coverage', 'No old save backfill, capability activation or ordinary command/APK proof'], completeGoal=False), indent=2) + '\n')
    print('PASS strict16 original kinship import', sha(packed), 'decoded', sha(body))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('folder', type=Path)
    p.add_argument('output', type=Path)
    p.add_argument('index', type=Path)
    a = p.parse_args()
    export(a.folder, a.output, a.index)
