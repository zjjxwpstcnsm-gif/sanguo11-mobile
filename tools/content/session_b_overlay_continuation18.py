#!/usr/bin/env python3
"""Exact r16 complete inherited stage to explicit B continuation r18; no A WIP."""
import json,hashlib,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
base=ROOT/'out/session-b/native-opening-combined61-frozen-r17/build-inputs.json'
d=json.loads(base.read_text());stage=Path(d['stage']);rows={r['path']:r for r in d['files']}
for path,r in rows.items():
 if sha(stage/path)!=r['sha256']:raise ValueError('r17 inherited input changed '+path)
paths=[
'app/src/androidTest/java/game/sanguo/mobile/SessionBNativeDuelInstrumentation.java',
'docs/handoff/20261006/session-b/native-opening-acceptance61-r18.init.gradle',
'docs/handoff/20261006/session-b/HUMAN_DISPOSITION_BINDING.md',
'docs/handoff/20261006/session-b/OWNERSHIP.md',
'tools/content/session_b_overlay_continuation18.py',
'tools/content/session_b_freeze_opening_combined.py',
]

registered=(ROOT/'docs/handoff/20261006/session-b/OWNERSHIP.md').read_text()
for path in paths:
 if path not in rows and path not in registered:raise ValueError('New path not registered '+path)
 if path.startswith('app/src/main/')and path!='app/src/main/java/game/sanguo/mobile/ContestUi.java':raise ValueError('A app path forbidden')
manifest=ROOT/'out/session-b/native-r18-overlay-manifest.json'
if manifest.exists():raise ValueError('Preserve completed overlay receipt')
changes=[dict(path=p,beforeSha256=rows[p]['sha256']if p in rows else None,afterSha256=sha(ROOT/p),bytes=(ROOT/p).stat().st_size,owner='B')for p in paths]
manifest.write_text(json.dumps(dict(status='planned',baseManifestSha256=sha(base),sourceMain='0e7b9bc2df90249a50851baeda58c7d183ea6059',commonMain='ef413be3653820dd6449ba7f02aa60bed5b26ef5',changes=changes),indent=2)+'\n')
for r in changes:
 p=r['path'];target=stage/p;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/p,target)
 if sha(target)!=r['afterSha256']:raise ValueError('Overlay mismatch '+p)
 rows[p]=dict(path=p,sha256=r['afterSha256'],bytes=r['bytes'])
for p,r in rows.items():
 if sha(stage/p)!=r['sha256']:raise ValueError('Full post overlay input differs '+p)
prefixes=['app/src/main/','core/src/main/','game-api/src/main/','game-runtime/src/main/','out/pc-native-runtime/','out/session-a/pc-fire-runtime/additional-jniLibs/','out/session-b/readonly-theme-dependencies/']
guard={p:r['sha256']for p,r in rows.items()if any(p.startswith(v)for v in prefixes)or p in ['app/build.gradle','build.gradle','settings.gradle','gradle.properties','version.properties']}
d.update(files=list(rows.values()),sourceGuard=guard,r18Changes=changes,r18BaseManifestSha256=sha(base))
(ROOT/'out/session-b/native-opening-combined61-source-inputs.json').write_text(json.dumps(d,indent=2)+'\n');(ROOT/'out/session-b/native-opening-combined61-source-guard.json').write_text(json.dumps(guard,indent=2)+'\n')
x=json.loads(manifest.read_text());x.update(status='complete_full_inputs_verified_build_pending',sourceInputs=len(rows),productionInputs=len(guard));manifest.write_text(json.dumps(x,indent=2)+'\n');print('PASS exact complete r18 overlay',len(rows),len(guard))
