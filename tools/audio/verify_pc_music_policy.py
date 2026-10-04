#!/usr/bin/env python3
"""Compare app music policy to complete original-selector execution dispatches."""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import subprocess


def verify(report, output, java_home):
    root = Path(__file__).resolve().parents[2]
    if output.exists():
        raise ValueError('Fresh evidence directory required')
    raw = report.read_bytes()
    source = json.loads(gzip.decompress(raw) if raw[:2] == b'\x1f\x8b' else raw)
    if source['sourceExecutableSha256'] != '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb':
        raise ValueError('Unexpected source executable')
    rows = source['rows']
    if len(rows) != 4096 or source['nativeChecks'] != 4096:
        raise ValueError('Incomplete original execution vectors')
    vectors = []
    for row in rows:
        if row['dispatch'] != [[row['musicId'], 1, 500, 0xbf800000]]:
            raise ValueError('Missing actual original dispatch')
        vectors.append('\t'.join(map(str, [*row['predicates'], row['actualOwnedCities'], row['seasonRaw'], row['dispatch'][0][0]])))
    output.mkdir(parents=True)
    path = output/'native-vectors.tsv'
    path.write_text('\n'.join(vectors)+'\n')
    classes = output/'classes'
    classes.mkdir()
    files = [root/'app/src/main/java/game/sanguo/mobile/PcMusicPolicy.java',
        root/'app/src/main/java/game/sanguo/mobile/PcMapMusicDirective.java',
        root/'game-api/src/main/java/game/sanguo/api/StateToken.java',
        root/'app/src/test/java/game/sanguo/mobile/PcMusicPolicyTest.java']
    compile_result = subprocess.run([str(java_home/'bin/javac'), '-d', str(classes), *map(str, files)], capture_output=True, text=True)
    (output/'compile.log').write_text(compile_result.stdout+compile_result.stderr)
    compile_result.check_returncode()
    result = subprocess.run([str(java_home/'bin/java'), '-cp', str(classes), 'game.sanguo.mobile.PcMusicPolicyTest', str(path)], capture_output=True, text=True)
    (output/'test.log').write_text(result.stdout+result.stderr)
    result.check_returncode()
    if 'PASS original music policy comparisons=4125' not in result.stdout:
        raise AssertionError('Missing full native/unknown boundary acceptance')
    evidence = dict(result='PASS', nativeVectors=4096, unknownAndPriorityChecks=16, sourceCallerGateChecks=13,
        nativeReportSha256=hashlib.sha256(raw).hexdigest(),
        paths={str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in files},
        limits=['Host production Java selector comparison, not normal scene binding or installed playback.'])
    (output/'acceptance.json').write_text(json.dumps(evidence, indent=2)+'\n')
    print(result.stdout.strip())


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('report', type=Path)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--java-home', type=Path, required=True)
    a = p.parse_args()
    verify(a.report, a.output, a.java_home)
