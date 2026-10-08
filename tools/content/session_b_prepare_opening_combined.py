#!/usr/bin/env python3
"""Full inherited immutable B inputs plus committed A closure, never live A WIP."""
import argparse, hashlib, json, shutil, subprocess, tarfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
A=Path('/Users/paopao/.codex/worktrees/2191/sanguo11-mobile')
CANDIDATE=ROOT/'out/session-b/native-candidate60'
STAGE=ROOT/'out/session-b/native-opening-combined61'
OUT=ROOT/'out/session-b/native-opening-combined61-source-inputs.json'
MANIFEST='docs/handoff/20261006/session-a/CANDIDATE_A_CLOSURE227.json'
OWN_INPUTS=[
 'app/src/androidTest/java/game/sanguo/mobile/SessionBNativeDuelInstrumentation.java',
 'docs/handoff/20261006/session-b/native-opening-acceptance61.init.gradle',
 'tools/content/session_b_prepare_opening_combined.py',
 'tools/content/session_b_verify_fieldworks_ui.py',
 'tools/content/session_b_freeze_apk.py',
]
def sha(path):
 d=hashlib.sha256()
 with path.open('rb') as f:
  for b in iter(lambda:f.read(1048576),b''):d.update(b)
 return d.hexdigest()
def rawsha(b):return hashlib.sha256(b).hexdigest()
def safe(name):
 p=Path(name)
 if p.is_absolute() or '..' in p.parts or str(p)!=name:raise ValueError('Unsafe inherited path '+name)
 return p
def extract_checked(archive,destination,expected):
 seen=set()
 with tarfile.open(archive,'r:gz') as t:
  for m in t:
   p=safe(m.name)
   if not m.isfile() or m.name not in expected or m.name in seen:raise ValueError('Unexpected source archive member '+m.name)
   row=expected[m.name];target=destination/p;target.parent.mkdir(parents=True,exist_ok=True)
   with t.extractfile(m) as src,target.open('wb') as dst:shutil.copyfileobj(src,dst)
   if target.stat().st_size!=row['bytes'] or sha(target)!=row['sha256']:raise ValueError('Source byte mismatch '+m.name)
   target.chmod(m.mode);seen.add(m.name)
 if seen!=set(expected):raise ValueError('Incomplete inherited source archive')
def main(commit):
 if STAGE.exists() or OUT.exists():raise ValueError('Independent evidence stage must be new')
 c=json.loads((CANDIDATE/'frozen-report.json').read_text());before={r['path']:r for r in c['files']}
 if sha(CANDIDATE/'frozen-report.json')!='a8a61bd12deb88fcdc69033d801e2c2e6a12c03f8f1869a1e838ef3e7fe1b324':raise ValueError('Candidate60 report changed')
 raw=subprocess.check_output(['git','show',commit+':'+MANIFEST],cwd=A);a=json.loads(raw)
 if (A/MANIFEST).read_bytes()!=raw:raise ValueError('A dependency manifest is not the committed frozen version')
 if a['supersedes']!=226 or a['includesBProduction'] or a['includesRootGradleBridgeUnityOriginal4Jni']:raise ValueError('Invalid A closure scope')
 if a['bCandidateReportSha256']!=sha(CANDIDATE/'frozen-report.json') or a['commonBase']!='0e7b9bc2df90249a50851baeda58c7d183ea6059':raise ValueError('A/B common source differs')
 overlay=Path(a['overlayPath']);source=Path(c['sourceArchive']['path'])
 if sha(overlay)!=a['overlaySha256'] or sha(source)!=c['sourceArchive']['sha256']:raise ValueError('Frozen archive SHA differs')
 app={r['path']:r['owner'] for r in json.loads((ROOT/'docs/handoff/20261006/parallel-repair/APP_OWNERSHIP.json').read_text())['files']}
 registered=set(json.loads(subprocess.check_output(['git','show',commit+':docs/handoff/20261006/session-a/OWNERSHIP.json'],cwd=A))['paths'])
 after={};increments=[]
 for r in a['paths']:
  path=r['path'];safe(path)
  if r['owner']!='A' or path.startswith(('core/','game-api/','game-runtime/')) or '/bridge/' in path:raise ValueError('Non-A closure input '+path)
  if path.endswith('.java') and app.get(path)!='A' and path not in registered:raise ValueError('Unregistered A Java '+path)
  base=before.get(path)
  if (base['sha256'] if base else None)!=r['candidateSourceBeforeSha256']:raise ValueError('A candidate original before differs '+path)
  after[path]=dict(path=path,bytes=r['bytes'],sha256=r['afterSha256']);increments.append(dict(r))
 for path,digest in a['sharedFrozenInputsExact'].items():
  if before[path]['sha256']!=digest or sha(ROOT/path)!=digest:raise ValueError('Shared frozen input differs '+path)
 production=('app/src/main/','core/src/main/','game-api/src/main/','game-runtime/src/main/','out/pc-native-runtime/','out/session-b/readonly-theme-dependencies/')
 for path,r in before.items():
  if path.startswith(production) and sha(ROOT/path)!=r['sha256']:raise ValueError('B candidate production changed '+path)
 own={p:dict(path=p,bytes=(ROOT/p).stat().st_size,sha256=sha(ROOT/p)) for p in OWN_INPUTS}
 needed=sum(r['bytes'] for r in before.values())+sum(r['bytes'] for r in after.values())+sum(r['bytes'] for r in own.values())
 free=shutil.disk_usage(ROOT).free
 if free<needed+4*1024**3:raise ValueError('Not enough free disk for complete inheritance and independent build')
 # Record every exact copied/overlaid path before writes, with its owner and SHA.
 record=dict(status='preparing',candidateReportSha256=sha(CANDIDATE/'frozen-report.json'),candidateInputs=len(before),candidateInputBytes=sum(r['bytes'] for r in before.values()),
  aClosureCommit=commit,aClosureManifestSha256=rawsha(raw),aClosureArchiveSha256=a['overlaySha256'],aClosurePaths=increments,
  ownLatestInputs=list(own.values()),diskFreeBefore=free,stage=str(STAGE),installed=False,complete=False)
 OUT.write_text(json.dumps(record,indent=2)+'\n');STAGE.mkdir()
 extract_checked(source,STAGE,before)
 extract_checked(overlay,STAGE,after)
 for path,row in own.items():
  target=STAGE/path;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/path,target)
  if sha(target)!=row['sha256']:raise ValueError('Own copied input SHA differs '+path)
 expected=dict(before);expected.update(after);expected.update(own)
 actual={str(p.relative_to(STAGE)) for p in STAGE.rglob('*') if p.is_file()}
 if actual!=set(expected):raise ValueError('Complete inherited source path set differs')
 for path,r in expected.items():
  if sha(STAGE/path)!=r['sha256']:raise ValueError('Final inherited source SHA differs '+path)
 # No old partial-theme init is used: closure227 materializes canonical A bytes.
 original_jni=[r for p,r in before.items() if p.startswith('out/pc-native-runtime/jniLibs/')]
 if len(original_jni)!=4 or any(sha(STAGE/r['path'])!=r['sha256'] for r in original_jni):raise ValueError('Original four JNI changed')
 guardprefix=production+('out/session-a/pc-fire-runtime/additional-jniLibs/',)
 guard={p:r['sha256'] for p,r in expected.items() if p.startswith(guardprefix) or p in ['app/build.gradle','build.gradle','settings.gradle','gradle.properties','version.properties']}
 record.update(status='complete_inputs_verified_build_pending',files=list(expected.values()),sourceGuard=guard,fullPathSetExact=True,allShaExact=True,originalFourJniExact=True,
  newAAdditionalJni=[r for p,r in after.items() if p.startswith('out/session-a/pc-fire-runtime/additional-jniLibs/')])
 OUT.write_text(json.dumps(record,indent=2)+'\n')
 print('PASS full inherited combination',len(expected),'files; A closure',len(after),'paths; original4 JNI exact; no install',flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--a-closure-commit',required=True);main(p.parse_args().a_closure_commit)
