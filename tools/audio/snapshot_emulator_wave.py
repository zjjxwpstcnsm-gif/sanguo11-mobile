#!/usr/bin/env python3
"""Copy a live emulator WAV prefix, repairing only its unfinalized lengths.

The emulator remains running. PCM bytes are preserved exactly; the provenance
labels this as a snapshot rather than a finalized recording.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
from datetime import datetime, timezone

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('source', type=Path)
p.add_argument('--output', type=Path, required=True)
p.add_argument('--flow-result', type=Path, required=True)
a = p.parse_args()
flow = json.loads(a.flow_result.read_text())
assert flow['passed'] and flow['restoration']['all_original_files_byte_equal']
assert not a.output.exists(), 'Preserve earlier evidence'
data = a.source.read_bytes()
assert len(data) >= 44
assert data[:4] == b'RIFF' and data[8:12] == b'WAVE'
assert data[12:16] == b'fmt ' and struct.unpack_from('<I', data, 16)[0] == 16
assert data[36:40] == b'data', 'Unknown chunk layout; inspect before repairing'
assert struct.unpack_from('<H', data, 20)[0] == 1, 'Expected PCM'
alignment = struct.unpack_from('<H', data, 32)[0]
assert (len(data) - 44) % alignment == 0, 'Snapshot ends inside a PCM frame'
assert struct.unpack_from('<I', data, 4)[0] == 0
assert struct.unpack_from('<I', data, 40)[0] == 0
snapshot = bytearray(data)
struct.pack_into('<I', snapshot, 4, len(data) - 8)
struct.pack_into('<I', snapshot, 40, len(data) - 44)
assert snapshot[44:] == data[44:]
a.output.write_bytes(snapshot)
sha = lambda b: hashlib.sha256(b).hexdigest()
report = dict(scope='Live emulator WAV prefix snapshot; only RIFF/data lengths repaired; PCM exact',
              captured_utc=datetime.now(timezone.utc).isoformat(), source=str(a.source.resolve()),
              output=str(a.output.resolve()), bytes=len(data), raw_prefix_sha256=sha(data),
              snapshot_sha256=sha(snapshot), pcm_sha256=sha(data[44:]),
              repaired_offsets=[4, 40], serial=flow['serial'], apk_sha256=flow['installed_sha256'],
              flow_result=str(a.flow_result.resolve()), flow_result_sha256=sha(a.flow_result.read_bytes()))
a.output.with_suffix('.provenance.json').write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
