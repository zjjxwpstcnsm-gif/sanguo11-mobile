#!/usr/bin/env python3
"""Export only the owned World/session integration delta, preserving all inputs."""
import argparse,difflib,hashlib,json
from pathlib import Path
PATHS=['core/src/main/java/game/sanguo/core/'+p+'.java'for p in ['Contests','ContestSave','SaveCodec','PcNativeDebatePolicy','PcDebateCampaign']]+['game-runtime/src/test/java/game/sanguo/core/'+p+'.java'for p in ['PcDebateCampaignTest','PcDebateCampaignColdTest']]
UI_PATHS=[
 "game-api/src/main/java/game/sanguo/api/GameApi.java",
 "game-api/src/main/java/game/sanguo/api/ContestSnapshot.java",
 "game-runtime/src/main/java/game/sanguo/runtime/GameSession.java",
 "game-runtime/src/main/java/game/sanguo/runtime/query/ContestQuery.java",
 "game-runtime/src/test/java/game/sanguo/core/PcDebateQueryTest.java",
 "app/src/main/java/game/sanguo/mobile/MainActivity.java",
 "app/src/main/java/game/sanguo/mobile/ContestUi.java"]
def export(checkpoint,prototype,output,batch=17):
    paths=PATHS+(UI_PATHS if batch==18 else [])
    manifest=json.loads((checkpoint/'manifest.json').read_bytes());base=checkpoint/'source';old={x['path']:x for x in manifest['files']};sha=lambda b:hashlib.sha256(b).hexdigest();rows=[];patch=[]
    for path,row in old.items():
        if sha((base/path).read_bytes())!=row['sha256']:raise ValueError('Checkpoint changed '+path)
        if path not in paths and sha((prototype/path).read_bytes())!=row['sha256']:raise ValueError('Unowned prototype change '+path)
    for path in paths:
        before=(base/path).read_bytes()if path in old else b'';after=(prototype/path).read_bytes()
        if before==after:continue
        rows.append(dict(path=path,beforeSha256=sha(before)if path in old else None,sha256=sha(after),new=path not in old))
        patch.extend(difflib.unified_diff(before.decode().splitlines(True),after.decode().splitlines(True),fromfile='a/'+path if path in old else '/dev/null',tofile='b/'+path))
    output.mkdir(parents=True,exist_ok=True);p=output/('batch%d-integration.patch'%batch);p.write_text(''.join(patch))
    (output/('batch%d-prototype-manifest.json'%batch)).write_text(json.dumps(dict(baseCommit=manifest['commit'],checkpointManifestSha256=sha((checkpoint/'manifest.json').read_bytes()),inheritedFiles=manifest['fileCount'],patchSha256=sha(p.read_bytes()),changes=rows,prototypeOnly=True,appliedToBranchProduction=False,completeGoal=False),indent=2)+'\n')
    print(json.dumps(dict(paths=len(rows),patchSha256=sha(p.read_bytes()),bytes=p.stat().st_size)))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('checkpoint',type=Path);p.add_argument('prototype',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--batch',type=int,choices=[17,18],default=17);a=p.parse_args();export(a.checkpoint,a.prototype,a.output,a.batch)
