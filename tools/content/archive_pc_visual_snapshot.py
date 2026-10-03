#!/usr/bin/env python3
"""Archive the inherited dirty source tree and pinned ignored build inputs.

Output must be a new directory under this project's ignored out/. No checkout,
Git mutation, PC install write, or toolchain cache is included.
"""
import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NATIVE_INPUTS = {
    f'out/pc-native-runtime/jniLibs/{abi}/{library}'
    for abi in ('arm64-v8a', 'x86_64')
    for library in ('libpc_effect_worker.so', 'libunicorn.so')
}


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT)


def archive(output, manifest, status):
    output = output.resolve()
    manifest = manifest.resolve()
    for path in [output, manifest]:
        if ROOT / 'out' not in path.parents:
            raise ValueError('Snapshot output must be inside project out/')
    if output.exists() or manifest.exists():
        raise ValueError('Use a fresh output/manifest to preserve historical evidence')
    sources = {p.decode() for p in git('ls-files', '-z', '--cached', '--others', '--exclude-standard').split(b'\0') if p}
    pinned = json.loads((ROOT / 'tools/content/map-release-manifest.json').read_text())['files']
    sources.update(row['source_path'] for row in pinned)
    # These ignored libraries are declared app build inputs, not disposable caches.
    # Preserve exact relative paths so this snapshot can build without another checkout.
    for name in sorted(NATIVE_INPUTS):
        if not (ROOT / name).is_file():
            raise ValueError('Required native build input missing: ' + name)
    sources.update(NATIVE_INPUTS)
    output.mkdir(parents=True)
    rows, deleted = [], []
    for name in sorted(sources):
        source = ROOT / name
        if source.is_absolute() and ROOT not in source.resolve().parents:
            raise ValueError('Source escapes project: ' + name)
        if source == ROOT or name.startswith('.git/') or name.startswith('out/') and name not in NATIVE_INPUTS:
            raise ValueError('Unexpected cache/metadata source: ' + name)
        if not source.is_file():
            deleted.append(name)
            continue
        target = output / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)
        raw = target.read_bytes()
        rows.append(dict(path=name, bytes=len(raw), sha256=hashlib.sha256(raw).hexdigest()))
    by_path = {r['path']: r for r in rows}
    for pin in pinned:
        if by_path.get(pin['source_path'], {}).get('sha256') != pin['sha256']:
            raise ValueError('Pinned source mismatch: ' + pin['source_path'])
    report = dict(schema=2, goal_complete=False, status=status,
                  branch=git('branch', '--show-current').decode().strip(),
                  head=git('rev-parse', 'HEAD').decode().strip(),
                  includes_inherited_dirty_worktree=True,
                  includes_pinned_ignored_build_inputs=True,
                  native_build_inputs=sorted(NATIVE_INPUTS),
                  excludes_toolchain_caches=True, deleted_tracked_paths=deleted,
                  snapshot_directory=str(output.relative_to(ROOT)),
                  file_count=len(rows), total_bytes=sum(r['bytes'] for r in rows),
                  files=rows)
    manifest.parent.mkdir(parents=True, exist_ok=True)
    manifest.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({k: v for k, v in report.items() if k != 'files'}, ensure_ascii=False))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--manifest', type=Path, required=True)
    parser.add_argument('--status', required=True)
    args = parser.parse_args()
    archive(args.output, args.manifest, args.status)
