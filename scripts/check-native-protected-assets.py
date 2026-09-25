#!/usr/bin/env python3
"""Exact protected trees plus individually reviewed runtime/source asset hashes."""
import argparse, hashlib, json, subprocess
from pathlib import Path

p=argparse.ArgumentParser()
p.add_argument('--manifest',default='docs/native-pc-visual/remediation-assets.json')
a=p.parse_args()
m=json.loads(Path(a.manifest).read_text())
def git(*args):return subprocess.check_output(['git',*args],text=True).splitlines()
def digest(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
subprocess.run(['git','diff','--exit-code',m['protected_baseline'],'HEAD','--','core','game-api','game-runtime','data','unity'],check=True)
actual=sorted(str(p) for p in Path('app/src/main/assets').rglob('*') if p.is_file())
assert actual==sorted(m['assets']), 'runtime asset addition/deletion is not registered'
for path,sha in m['assets'].items():assert digest(path)==sha, 'runtime asset hash mismatch: '+path
changed=set(git('diff','--name-only',m['asset_baseline'],'HEAD','--','app/src/main/assets'))
assert changed==set(m['permitted_changes']), 'asset delta does not match the explicit approved list'
for path,entry in m['permitted_changes'].items():
 assert entry['runtime_sha256']==m['assets'][path], 'registered runtime hash mismatch: '+path
 assert digest(entry['source'])==entry['source_sha256'], 'material source hash mismatch: '+entry['source']
 assert entry['version'] and entry['source_license'] and entry['regression'], 'incomplete provenance: '+path
print('PASS protected core/game-api/game-runtime/data/unity; exact',len(actual),'runtime assets;',len(changed),'registered R14 materials')
