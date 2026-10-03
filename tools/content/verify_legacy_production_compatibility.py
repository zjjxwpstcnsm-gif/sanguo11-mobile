#!/usr/bin/env python3
"""Compare normal single-actor legacy production and full-turn saved hashes with frozen R25.
Fixture is created by the genuine frozen engine, not a native-mode save downgraded by editing headers.
"""
import argparse,hashlib,json,os,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def run(a):
 provenance=json.loads((ROOT/'tools/content/fixtures/pre-merchant-r25-v34.json').read_text())
 if sha(a.frozen_jar)!=provenance['writer_jar_sha256']:raise ValueError('Frozen source jar differs')
 output=a.output.resolve()
 if ROOT/'out' not in output.parents:raise ValueError('Fresh output inside project out required')
 output.mkdir(parents=True,exist_ok=False);classes=output/'classes';classes.mkdir();source=ROOT/'tools/content/PcProductionCompatibilityProbe.java';java=Path(os.environ['JAVA_HOME'])/'bin'
 subprocess.run([str(java/'javac'),'--release','17','-encoding','UTF-8','-cp',str(a.frozen_jar.resolve()),'-d',str(classes),str(source)],check=True)
 report=dict(passed=False,writer_jar_sha256=sha(a.frozen_jar),candidate_jar_sha256=sha(a.candidate_jar),source_sha256=sha(source),scope='Frozen-created v33 synthetic worlds, every weapon/ship and11 admission/skill/capacity conditions; normal scalar commands and4 full turns/save roundtrip, not genuine v31/v32/v35 openings or UI')
 for name,jar in [('frozen',a.frozen_jar),('candidate',a.candidate_jar)]:
  with (output/(name+'.log')).open('wb') as log:p=subprocess.run([str(java/'java'),'-Xmx768m','-cp',os.pathsep.join([str(classes),str(jar.resolve())]),'game.sanguo.core.PcProductionCompatibilityProbe'],stdout=log,stderr=subprocess.STDOUT)
  report[name+'_exit_code']=p.returncode;(output/'results.json').write_text(json.dumps(report,indent=2)+'\n')
  if p.returncode:raise ValueError(name+' normal compatibility probe failed, log retained')
 before=(output/'frozen.log').read_bytes();after=(output/'candidate.log').read_bytes();report.update(passed=before==after,complete_saved_state_hash_observations=len(before.splitlines()),output_sha256=hashlib.sha256(before).hexdigest());(output/'results.json').write_text(json.dumps(report,indent=2)+'\n')
 if before!=after:raise ValueError('Normal legacy production changed; both full logs retained')
 print('PASS exact frozen legacy production/turn saves',report['complete_saved_state_hash_observations'],'observations')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for name in ['frozen-jar','candidate-jar','output']:p.add_argument('--'+name,required=True,type=Path)
 run(p.parse_args())
