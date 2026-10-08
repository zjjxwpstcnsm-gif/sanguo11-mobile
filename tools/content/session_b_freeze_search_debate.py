#!/usr/bin/env python3
"""Bounded test-only SEARCH acceptance over the exact unchanged r27 production."""
from pathlib import Path
import hashlib,json,tarfile,shutil,difflib,subprocess,argparse
p=argparse.ArgumentParser();p.add_argument("--attempt",type=int,required=True);args=p.parse_args();assert args.attempt in [1,2]
R=Path(__file__).resolve().parents[2];S=R/'out/session-b/native-opening-combined61';F=R/'out/session-b/native-opening-combined61-frozen-r27';B=R/f'out/session-b/search-debate-current-built-v{args.attempt}';O=R/'out/session-b/native-opening-combined61-frozen-r28-search-v2'
def sha(p):
 h=hashlib.sha256()
 with p.open('rb')as f:
  for b in iter(lambda:f.read(1048576),b''):h.update(b)
 return h.hexdigest()
def raw(b):return hashlib.sha256(b).hexdigest()
aPath=R/f'out/session-b/search-debate-current-apk-acceptance-v{args.attempt}/results.json';a=json.loads(aPath.read_text());assert a['passed']and a['coldPassed']and all(v['shaReadbackMatches']and not v['preservedAdditionalFiles']for v in a['restoration'].values())
d=json.loads((B/'native-opening-combined61-source-inputs.json').read_text());old=json.loads((F/'build-inputs.json').read_text());g=json.loads((B/'source-guard.json').read_text());assert g==json.loads((F/'source-guard.json').read_text())
rows={r['path']:r for r in d['files']};beforeRows={r['path']:r for r in old['files']}
for p,r in rows.items():assert sha(S/p)==r['sha256'],p
paths=sorted(p for p in set(rows)|set(beforeRows)if rows.get(p)!=beforeRows.get(p));assert set(paths)=={'app/src/androidTest/java/game/sanguo/mobile/SessionBDebateInstrumentation.java','docs/handoff/20261006/session-b/native-opening-search-debate61.init.gradle'}
for p,h in g.items():assert sha(S/p)==h and rows[p]['sha256']==h,p
assert sha(B/'app-debug.apk')==sha(F/'app-debug.apk')==a['installed']['game.sanguo.mobile.dev'];assert sha(B/'app-debug-androidTest.apk')==a['installed']['game.sanguo.mobile.dev.test']
assert not O.exists();O.mkdir()
for m in ['core','game-api','game-runtime']:assert sha(S/m/'build/libs'/f'{m}.jar')==sha(F/f'{m}.jar');shutil.copy2(F/f'{m}.jar',O/f'{m}.jar')
for p in [B/'app-debug.apk',B/'app-debug-androidTest.apk',aPath,R/f'out/session-b/search-debate-current-apk-guard-v{args.attempt}.json']:shutil.copy2(p,O/p.name)
shutil.copy2(B/'native-opening-combined61-source-inputs.json',O/'build-inputs.json');shutil.copy2(B/'source-guard.json',O/'source-guard.json')
archive=O/'combined-source.tar.gz'
with tarfile.open(archive,'w:gz',dereference=True)as t:
 for p in sorted(rows):t.add(S/p,arcname=p,recursive=False)
seen=set()
with tarfile.open(archive,'r:gz')as t:
 for m in t:
  assert m.isfile()and m.name in rows and m.name not in seen;row=rows[m.name];assert raw(t.extractfile(m).read())==row['sha256']and m.size==row['bytes']and m.mode==((S/m.name).stat().st_mode&0o7777),m.name;seen.add(m.name)
assert seen==set(rows)
fReport=json.loads((F/'frozen-report.json').read_text());assert sha(F/'combined-source.tar.gz')==fReport['sourceArchive']['sha256']
audit=R/'out/session-b/search-debate-current-increment-audit';assert not audit.exists();audit.mkdir();patchText=[];changes=[]
with tarfile.open(F/'combined-source.tar.gz')as t:
 for p in paths:
  before=t.extractfile(p).read()if p in beforeRows else b'';after=(S/p).read_bytes();assert (raw(before)if p in beforeRows else None)==(beforeRows[p]['sha256']if p in beforeRows else None);assert raw(after)==rows[p]['sha256']
  if p in beforeRows:q=audit/p;q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(before)
  patchText+=difflib.unified_diff(before.decode().splitlines(True),after.decode().splitlines(True),fromfile='a/'+p if p in beforeRows else '/dev/null',tofile='b/'+p);changes.append(dict(path=p,beforeSha256=raw(before)if p in beforeRows else None,afterSha256=raw(after),owner='B',production=False))
patch=R/'docs/handoff/20261006/session-b/CURRENT_SEARCH_TEST_INCREMENT.patch';assert not patch.exists();patch.write_text(''.join(patchText));subprocess.run(['git','apply','--check',str(patch)],cwd=audit,check=True);subprocess.run(['git','apply',str(patch)],cwd=audit,check=True)
for p in paths:assert sha(audit/p)==rows[p]['sha256']
manifest=dict(commonMain='ef413be3653820dd6449ba7f02aa60bed5b26ef5',latestFullMain='0e7b9bc2df90249a50851baeda58c7d183ea6059',testedProductionCommit='cdd7f949f076dd7106aaefeff7fcbf86314f05cf',changes=changes,patchSha256=sha(patch),applyCheckAndAfterShaReadback=True,productionChanges=[])
(O/'increment-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
report=dict(wholeGoalComplete=False,pairRevision=28,productionExactlyFrozenR27=True,normalSearchNativeDebateAndColdAndUserRestorationPassed=True,commonMain=manifest['commonMain'],latestFullMain=manifest['latestFullMain'],aClosureCommit=d['aClosureCommit'],sourceInputs=len(seen),productionInputs=len(g),sourceArchive=dict(path=str(archive),sha256=sha(archive),bytes=archive.stat().st_size),apk=dict(path=str(O/'app-debug.apk'),sha256=sha(O/'app-debug.apk')),testApk=dict(path=str(O/'app-debug-androidTest.apk'),sha256=sha(O/'app-debug-androidTest.apk')),fixedResources=168,originalFourAndAdditionalTwoJniExactlyFrozenR27=True,increment=manifest,limits=['Source0 twoforces current APK only; all16 normal campaigns and ARM unverified','Original mid-debate voluntary concession/diplomacy/direct engineering fee still unknown','Full officer effectiveness and original activation/event coverage incomplete','Existing required core fixture failures and whole goal remain open'])
(O/'frozen-report.json').write_text(json.dumps(report,indent=2)+'\n');print('PASS exact test-only SEARCH/fullsource/mode/patch/unchanged production',report['sourceArchive']['sha256'],manifest['patchSha256'])
