#!/usr/bin/env python3
"""No production delta: exact r27 source plus current ordinary search/debate runner and init."""
from pathlib import Path
import json,hashlib,shutil,argparse
p=argparse.ArgumentParser();p.add_argument('--attempt',type=int,default=1);a=p.parse_args();assert a.attempt in [1,2]
R=Path(__file__).resolve().parents[2];S=R/'out/session-b/native-opening-combined61';F=R/'out/session-b/native-opening-combined61-frozen-r27'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
previous=F/'build-inputs.json' if a.attempt==1 else R/'out/session-b/search-debate-current-built-v1/native-opening-combined61-source-inputs.json';d=json.loads(previous.read_text());rows={r['path']:r for r in d['files']}
for p,r in rows.items():assert sha(S/p)==r['sha256'],p
paths=['app/src/androidTest/java/game/sanguo/mobile/SessionBDebateInstrumentation.java','docs/handoff/20261006/session-b/native-opening-search-debate61.init.gradle'];changes=[dict(path=p,beforeSha256=rows[p]['sha256']if p in rows else None,afterSha256=sha(R/p),bytes=(R/p).stat().st_size)for p in paths];out=R/f'out/session-b/search-debate-current-overlay-v{a.attempt}.json';assert not out.exists();out.write_text(json.dumps(dict(changes=changes,productionChanges=[]),indent=2)+'\n')
for c in changes:
 p=c['path'];(S/p).parent.mkdir(parents=True,exist_ok=True);shutil.copy2(R/p,S/p);rows[p]=dict(path=p,sha256=c['afterSha256'],bytes=c['bytes'])
d['files']=list(rows.values());d['currentSearchDebateOverlay']=str(out);(R/'out/session-b/native-opening-combined61-source-inputs.json').write_text(json.dumps(d,indent=2)+'\n');print('PASS exact full r27 preimages and test-only search/explicit-option overlay',len(rows))
