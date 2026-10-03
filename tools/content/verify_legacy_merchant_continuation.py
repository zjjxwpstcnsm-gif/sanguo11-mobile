#!/usr/bin/env python3
"""Compare frozen R25 production and candidate on ordinary v34 commands/turns.

Pass the preserved R25 jar and candidate core jar; no save normalization, rule
replacement or user-file access. An output directory must be new.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess

SOURCE = '''package game.sanguo.core;
import java.nio.file.*;
public class LegacyMarketContinuation {
 public static void main(String[] args) throws Exception {
  World w=SaveCodec.decode(Files.readAllBytes(Path.of(args[0])));
  Path out=Path.of(args[1]);Files.createDirectories(out);
  for(int n=0;n<4;n++) {
   World.City c=w.home();World.Officer o=w.idle(c).get(0);
   World.Result r=w.campaign.trade(c.id,o.id,n%2==0,1000);
   if(!r.ok)throw new AssertionError(r.message);
   Files.write(out.resolve("trade-"+n+".sg11"),SaveCodec.encode(w));
   if(!w.nextTurn().ok)throw new AssertionError("normal nextTurn failed");
   Files.write(out.resolve("turn-"+n+".sg11"),SaveCodec.encode(w));
  }
  System.out.println("PASS frozen v34 ordinary trade and four full turns");
 }
}
'''

def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--frozen-jar', required=True, type=Path)
    p.add_argument('--candidate-jar', required=True, type=Path)
    p.add_argument('--output', required=True, type=Path)
    a = p.parse_args()
    root = Path(__file__).resolve().parents[2]
    provenance = json.loads((root/'tools/content/fixtures/pre-merchant-r25-v34.json').read_text())
    fixture = root/provenance['fixtures'][0]['path']
    if sha(a.frozen_jar) != provenance['writer_jar_sha256']:
        raise ValueError('Frozen R25 jar differs from the recorded production input')
    if sha(fixture) != provenance['fixtures'][0]['sha256']:
        raise ValueError('Frozen v34 fixture differs')
    a.output.mkdir(parents=True, exist_ok=False)
    source = a.output/'LegacyMarketContinuation.java'
    source.write_text(SOURCE)
    java_bin = Path(os.environ['JAVA_HOME'])/'bin'
    classes = a.output/'classes'
    classes.mkdir()
    subprocess.run([str(java_bin/'javac'), '-cp', str(a.frozen_jar.resolve()), '-d', str(classes), str(source)], check=True)
    for name, jar in [('frozen', a.frozen_jar), ('candidate', a.candidate_jar)]:
        with (a.output/(name+'.log')).open('w') as log:
            subprocess.run([str(java_bin/'java'), '-cp', str(classes.resolve())+os.pathsep+str(jar.resolve()),
                            'game.sanguo.core.LegacyMarketContinuation', str(fixture), str(a.output/name)],
                           stdout=log, stderr=subprocess.STDOUT, check=True)
    records = []
    for f in sorted((a.output/'frozen').glob('*.sg11')):
        current = a.output/'candidate'/f.name
        equal = f.read_bytes() == current.read_bytes()
        records.append(dict(path=f.name, bytes=f.stat().st_size, frozen_sha256=sha(f), candidate_sha256=sha(current), byte_equal=equal))
    passed = len(records) == 8 and all(r['byte_equal'] for r in records)
    report = dict(passed=passed, source_commit=provenance['source_commit'], fixture_sha256=sha(fixture),
                  frozen_jar_sha256=sha(a.frozen_jar), candidate_jar_sha256=sha(a.candidate_jar), saves=records,
                  scope='JVM actual normal Campaign.trade and World.nextTurn; complete encoded bytes, no normalization')
    (a.output/'results.json').write_text(json.dumps(report, indent=2)+'\n')
    if not passed:
        raise AssertionError('v34 normal continuation differs')
    print('PASS 8 complete v34 normal-command/turn saves byte equal')

if __name__ == '__main__':
    main()
