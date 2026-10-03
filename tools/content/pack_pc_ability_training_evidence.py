#!/usr/bin/env python3
"""Reproduce the compact source evidence artifact from two original-execution reports."""
import argparse
import gzip
import hashlib
import io
import json
from pathlib import Path

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--parameters', type=Path, required=True)
p.add_argument('--completion', type=Path, required=True)
p.add_argument('--output', type=Path, required=True)
a = p.parse_args()
source = Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
output = a.output.resolve()
if output == source or source in output.parents:
    raise ValueError('PC installation is read-only')
if output.exists():
    raise ValueError('Use a new output path; preserve existing evidence')
params_raw, completion_raw = a.parameters.read_bytes(), a.completion.read_bytes()
params, complete = json.loads(params_raw), json.loads(completion_raw)
assert params['exe_sha256'] == complete['exe_sha256'] == '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
assert params['shared_sha256'] == complete['shared_sha256'] == 'dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'
assert [r['native_index'] for r in params['rows']] == list(range(98))
assert [r['source_record_sha256'] for r in params['rows']] == complete['source_record_sha256']
assert params['getter_calls'] == 490 and params['full_world_rng_unchanged']
assert complete['full_3mib_mutation_guard'] and complete['rng_unchanged']
assert [len(complete[k]) for k in ['successful_cases', 'rejected_cases', 'aptitude_admission',
                                'aptitude_completion', 'skill_admission', 'skill_completion']] == [480, 4, 48, 24, 213, 276]
for key, start, stop in [('successful_cases', 0, 15), ('aptitude_completion', 15, 27), ('skill_completion', 27, 98)]:
    assert {r['native_index'] for r in complete[key]} == set(range(start, stop))
report = dict(schema=1, official_effective_source_verified=False, android_rules_imported=False,
              user_base_cultivation_policy_preserved=True,
              parameter_report_sha256=hashlib.sha256(params_raw).hexdigest(),
              completion_report_sha256=hashlib.sha256(completion_raw).hexdigest(),
              parameters=params, complete_nonhuman_functions=complete)
raw = (json.dumps(report, ensure_ascii=False, separators=(',', ':'))+'\n').encode()
buffer = io.BytesIO()
with gzip.GzipFile(filename='', mode='wb', fileobj=buffer, mtime=0) as f:
    f.write(raw)
packed = buffer.getvalue()
assert gzip.decompress(packed) == raw
output.parent.mkdir(parents=True, exist_ok=True)
output.write_bytes(packed)
print(json.dumps(dict(bytes=len(packed), sha256=hashlib.sha256(packed).hexdigest(), native_rows=98)))
