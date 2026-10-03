#!/usr/bin/env python3
"""Compare frozen R25 and candidate on real v33 fixed-command/turn saves."""
import argparse
import json
import os
from pathlib import Path
import subprocess
from verify_legacy_merchant_continuation import sha

SOURCE = '''package game.sanguo.core;
import java.nio.file.*;
public class LegacyCityRewardsContinuation {
 public static void main(String[] args) throws Exception {
  Path out=Path.of(args[1]);Files.createDirectories(out);
  for(int operation=0;operation<4;operation++) {
   World w=SaveCodec.decode(Files.readAllBytes(Path.of(args[0])));
   if(w.officerAbilities.enabled())throw new AssertionError("expected genuine unmanaged save");
   World.City c=w.home();w.active=w.player;w.actionPoints[w.player]=60;
   c.order=70;c.morale=40;c.gold=50000;c.troops=12000;
   Domestic.Kind kind=operation==2?Domestic.Kind.BARRACKS:operation==3?Domestic.Kind.SMITH:null;
   if(kind!=null&&w.domestic.capacity(c.id,kind)==0) {
    Hex h=w.domestic.buildSites(c.id).get(0);
    w.domestic.facilities.add(new Domestic.Facility(w.domestic.nextFacilityId++,c.id,kind,h,-1,0));
   }
   World.Officer actor=w.idle(c).get(0);
   World.Result result=operation==0?w.strategy.patrol(c.id,actor.id):operation==1?w.strategy.trainArmy(c.id,actor.id):operation==2?w.strategy.recruitSoldiers(c.id,actor.id):w.produce(c.id,actor.id,World.Weapon.SPEAR);
   if(!result.ok)throw new AssertionError(result.message);
   Files.write(out.resolve("command-"+operation+".sg11"),SaveCodec.encode(w));
   for(int turn=0;turn<3;turn++) {
    if(!w.nextTurn().ok)throw new AssertionError("actual full turn failed");
    Files.write(out.resolve("turn-"+operation+"-"+turn+".sg11"),SaveCodec.encode(w));
   }
  }
  System.out.println("PASS genuine v33 fixed normal commands and twelve full turns");
 }
}
'''

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--frozen-jar',required=True,type=Path)
    p.add_argument('--candidate-jar',required=True,type=Path)
    p.add_argument('--output',required=True,type=Path)
    a=p.parse_args();root=Path(__file__).resolve().parents[2]
    jar_provenance=json.loads((root/'tools/content/fixtures/pre-merchant-r25-v34.json').read_text())
    provenance=json.loads((root/'tools/content/fixtures/pc-officer-legacy-am.json').read_text())
    fixture=root/provenance['fixture']
    if sha(a.frozen_jar)!=jar_provenance['writer_jar_sha256'] or sha(fixture)!=provenance['sha256']:
        raise ValueError('Frozen production jar or genuine legacy fixture differs')
    a.output.mkdir(parents=True,exist_ok=False);source=a.output/'LegacyCityRewardsContinuation.java';source.write_text(SOURCE)
    classes=a.output/'classes';classes.mkdir();java=Path(os.environ['JAVA_HOME'])/'bin'
    subprocess.run([str(java/'javac'),'-cp',str(a.frozen_jar.resolve()),'-d',str(classes),str(source)],check=True)
    for name,jar in [('frozen',a.frozen_jar),('candidate',a.candidate_jar)]:
        with (a.output/(name+'.log')).open('w') as log:
            subprocess.run([str(java/'java'),'-cp',str(classes.resolve())+os.pathsep+str(jar.resolve()),'game.sanguo.core.LegacyCityRewardsContinuation',str(fixture),str(a.output/name)],stdout=log,stderr=subprocess.STDOUT,check=True)
    records=[]
    for saved in sorted((a.output/'frozen').glob('*.sg11')):
        current=a.output/'candidate'/saved.name
        records.append(dict(path=saved.name,bytes=saved.stat().st_size,frozen_sha256=sha(saved),candidate_sha256=sha(current),byte_equal=saved.read_bytes()==current.read_bytes()))
    passed=len(records)==16 and all(r['byte_equal'] for r in records)
    (a.output/'results.json').write_text(json.dumps(dict(passed=passed,source_commit=jar_provenance['source_commit'],fixture_sha256=sha(fixture),frozen_jar_sha256=sha(a.frozen_jar),candidate_jar_sha256=sha(a.candidate_jar),saves=records,scope='Genuine unmanaged v33 fixture; authored legal command prerequisites; actual four commands/twelve full turns and encoded bytes, no normalization'),indent=2)+'\n')
    if not passed:raise AssertionError('Unmanaged v33 continuation differs')
    print('PASS 16 complete genuine v33 command/turn saves byte equal')

if __name__=='__main__':main()
