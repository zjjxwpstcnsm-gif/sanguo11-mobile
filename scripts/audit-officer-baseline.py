#!/usr/bin/env python3
"""Compare legacy suites against main and candidate using identical current test fixtures.
This is diagnostic, not a replacement for test-core.sh or Gradle check.
Exit nonzero for any changed failing outcome, timeout, compilation error, or new failure.
"""
import concurrent.futures
import json
from pathlib import Path
import re
import shlex
import subprocess
import sys

candidate = Path.cwd()
baseline = Path(sys.argv[1]).resolve()
out = candidate / 'out/custom-officers/legacy-audit'
out.mkdir(parents=True, exist_ok=True)
commands = []
for line in (candidate / 'scripts/test-core.sh').read_text().splitlines():
    if line.startswith('java -cp') and '"$mode"' not in line:
        commands.append(shlex.split(line)[3:])
for mode in ('fee', 'food', 'progress'):
    commands.append(['game.sanguo.core.LogisticsRegressionProbe', mode])
tests = list((candidate / 'core/src/test/java').rglob('*.java')) + list((candidate / 'core/src/testFixtures/java').rglob('*.java'))
tests = [p for p in tests if p.name not in ('CustomOfficerTest.java', 'OfficerPackArchiveTest.java')]

def execute(root, label):
    classes = out / (label + '-classes')
    classes.mkdir(exist_ok=True)
    sources = list((root / 'core/src/main/java').rglob('*.java')) + tests
    argfile = out / (label + '-sources.txt')
    argfile.write_text('\n'.join(str(p) for p in sources))
    build = subprocess.run(['javac', '-encoding', 'UTF-8', '--release', '17', '-d', str(classes), '@' + str(argfile)], capture_output=True, text=True, timeout=120)
    (out / (label + '-compile.txt')).write_text(build.stdout + build.stderr)
    if build.returncode:
        raise RuntimeError(label + ' compilation failed; inspect compile log')
    cp = ':'.join(map(str, (classes, root / 'core/src/main/resources', candidate / 'core/src/test/resources')))
    def test(args):
        name = '-'.join(args).replace('game.sanguo.core.', '')
        try:
            result = subprocess.run(['java', '-Dfile.encoding=UTF-8', '-cp', cp] + args, capture_output=True, text=True, timeout=90, cwd=candidate)
            log = result.stdout + result.stderr
            reason = next((line for line in log.splitlines() if line.startswith('Exception in thread')), '')
            code = result.returncode
        except subprocess.TimeoutExpired:
            log, reason, code = 'TIMEOUT', 'TIMEOUT', 124
        (out / (label + '-' + name + '.txt')).write_text(log)
        return name, {'code': code, 'firstError': reason}
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        return dict(pool.map(test, commands))

before = execute(baseline, 'main')
after = execute(candidate, 'candidate')
changed = {name: {'main': before[name], 'candidate': result} for name, result in after.items()
           if result['code'] == 124 or result['code'] != 0 and result != before[name]}
report = {'baseline': subprocess.check_output(['git', '-C', str(baseline), 'rev-parse', 'HEAD'], text=True).strip(),
          'candidate': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip(),
          'sameCurrentFixtures': True, 'main': before, 'candidateResults': after, 'changedFailures': changed}
(out / 'comparison.json').write_text(json.dumps(report, ensure_ascii=False, indent=2))
print('LEGACY COMPARISON:', len(commands), 'suites;', sum(r['code'] != 0 for r in before.values()),
      'main failures;', sum(r['code'] != 0 for r in after.values()), 'candidate failures;', len(changed), 'changed failures')
print('Existing failures remain visible in comparison.json and do NOT constitute a green full regression.')
if changed:
    print(json.dumps(changed, ensure_ascii=False, indent=2))
    sys.exit(1)
