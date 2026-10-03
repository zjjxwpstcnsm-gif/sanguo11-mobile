#!/usr/bin/env python3
"""Compile a frozen pre-rule core, write real double-trade save, read/replay with candidate.

Run Gradle :core:testClasses first. Requires explicit JAVA_HOME and a fresh output.
No emulator, user saves, PC installation, or Git state are changed.
"""
import argparse,hashlib,json,os,subprocess
from pathlib import Path

def run(snapshot,output):
 root=Path(__file__).resolve().parents[2];snapshot=snapshot.resolve();output=output.resolve()
 if root/'out' not in output.parents:raise ValueError('Output must be in project out/')
 manifest=json.loads((snapshot/'manifest.json').read_text());source=root/manifest['snapshot_directory'];old=source/'core'
 sources=[]
 for row in manifest['files']:
  name=row['path']
  if not name.startswith(('core/src/main/java/','core/src/main/resources/')):continue
  path=source/name;raw=path.read_bytes()
  if len(raw)!=row['bytes'] or hashlib.sha256(raw).hexdigest()!=row['sha256']:raise ValueError('Frozen input mismatch: '+name)
  if name.endswith('.java'):sources.append(str(path))
 if not sources:raise ValueError('No frozen core sources')
 java=Path(os.environ['JAVA_HOME'])/'bin';probe=root/'core/src/test/java/game/sanguo/core/PcMerchantUseCompatibilityProbe.java';port_probe=probe.with_name('PcMerchantSiteCompatibilityProbe.java');merit_probe=probe.with_name('PcMerchantMeritCompatibilityProbe.java')
 output.mkdir(parents=True,exist_ok=False);classes=output/'baseline-classes';classes.mkdir();save=output/'legacy-two-orders.sg11'
 with (output/'verification.log').open('w') as log:
  def command(args):subprocess.run(args,check=True,stdout=log,stderr=log,cwd=root)
  command([str(java/'javac'),'-encoding','UTF-8','-d',str(classes)]+sources+[str(probe),str(port_probe),str(merit_probe)])
  command([str(java/'java'),'-Xmx768m','-cp',str(classes)+':'+str(old/'src/main/resources'),'game.sanguo.core.PcMerchantUseCompatibilityProbe','write-legacy',str(save)])
  command([str(java/'java'),'-Xmx768m','-cp','core/build/classes/java/test:core/build/classes/java/main:core/build/resources/main:core/build/resources/test','game.sanguo.core.PcMerchantUseCompatibilityProbe','read-candidate',str(save)])
  port_save=output/'legacy-port-orders.sg11'
  command([str(java/'java'),'-Xmx768m','-cp',str(classes)+':'+str(old/'src/main/resources'),'game.sanguo.core.PcMerchantSiteCompatibilityProbe','write-legacy',str(port_save)])
  command([str(java/'java'),'-Xmx768m','-cp','core/build/classes/java/test:core/build/classes/java/main:core/build/resources/main:core/build/resources/test','game.sanguo.core.PcMerchantSiteCompatibilityProbe','read-candidate',str(port_save)])
  merit_save=output/'legacy-excess-merit.sg11'
  command([str(java/'java'),'-Xmx768m','-cp',str(classes)+':'+str(old/'src/main/resources'),'game.sanguo.core.PcMerchantMeritCompatibilityProbe','write-legacy',str(merit_save)])
  command([str(java/'java'),'-Xmx768m','-cp','core/build/classes/java/test:core/build/classes/java/main:core/build/resources/main:core/build/resources/test','game.sanguo.core.PcMerchantMeritCompatibilityProbe','read-candidate',str(merit_save)])
 raw=save.read_bytes();report=dict(passed=True,frozen_manifest_sha256=hashlib.sha256((snapshot/'manifest.json').read_bytes()).hexdigest(),probe_sha256=hashlib.sha256(probe.read_bytes()).hexdigest(),old_save_bytes=len(raw),old_save_sha256=hashlib.sha256(raw).hexdigest(),scope='Frozen actual two trades; lossless candidate load; repeat rejection; three normal turns and transactions with full save/RNG replay')
 port=port_save.read_bytes();report['port_compatibility']=dict(passed=True,old_save_bytes=len(port),old_save_sha256=hashlib.sha256(port).hexdigest(),probe_sha256=hashlib.sha256(port_probe.read_bytes()).hexdigest(),scope='Real frozen port buys/sells; lossless read and both directions rejected; three normal turns and exact save/RNG replay')
 merit=merit_save.read_bytes();report['merit_compatibility']=dict(passed=True,old_save_bytes=len(merit),old_save_sha256=hashlib.sha256(merit).hexdigest(),probe_sha256=hashlib.sha256(merit_probe.read_bytes()).hexdigest(),scope='Frozen real100-merit trade creates60090 merit; candidate reads byte-exact and preserves excess through three turns and normal transactions')
 (output/'results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--snapshot',required=True,type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args();run(a.snapshot,a.output)
