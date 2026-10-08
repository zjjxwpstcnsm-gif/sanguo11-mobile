#!/usr/bin/env python3
"""Exact queue delta against full356, game remains326, canonical production unchanged."""
from pathlib import Path
import json,subprocess
from read_session_state import read_session_state as read
from run_remaining_normal_media import sha
from guard_capture_source357 import guard_capture_source
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent
def guard_capture_queue(revision,expected_main):
 b=read(D/'CAPTURE_QUEUE_BUILD363.json');p=read(D/'CAPTURE_TEST_BUILD356.json');delta=read(D/'CAPTURE_QUEUE361.json')['paths'];assert revision==b['sourceRevision'] and b['buildSuccessful'] and b['gameReusedExact326'];parent=guard_capture_source(p['sourceRevision'],expected_main)
 changes=subprocess.check_output(['git','diff','--name-only',revision,'--','app/src','app/build.gradle','core','game-api','game-runtime'],cwd=ROOT,text=True).splitlines();assert not changes,changes
 datasets=[]
 for build in [p,b]:
  path=Path(build['candidateInputManifest']);assert sha(path)==build['candidateInputManifestSha256'];rows=read(path);d={x['path']:x for x in rows};assert len(d)==len(rows);datasets.append((path,d))
 old,new=datasets;expected={x['path'] for x in delta};actual={k for k in set(old[1])|set(new[1]) if old[1].get(k)!=new[1].get(k)};assert actual==expected and len(new[1])==11420
 for x in delta:
  assert old[1].get(x['path'],{}).get('sha256')==x['beforeSha256'];assert new[1][x['path']]['sha256']==x['afterSha256'];assert x['path'] in read(D/'OWNERSHIP.json')['paths']
 for path,row in new[1].items():assert sha(new[0].parent/'source'/path)==row['sha256'],path
 for row in b['apks']:assert sha(Path(row['path']))==row['sha256']
 assert b['apks'][0]==p['apks'][0];return {'parentGuard':parent,'completeInputFilesVerified':len(new[1]),'full363ManifestSha256':sha(new[0]),'onlyExactTestQueueDelta':delta,'game326ByteIdentical':True,'canonicalProductionChanged':False,'wholeGoalComplete':False}
if __name__=='__main__':
 out=D/'CAPTURE_QUEUE_GUARD365.json';assert not out.exists();r=guard_capture_queue(read(D/'CAPTURE_QUEUE_BUILD363.json')['sourceRevision'],read(D/'INHERITANCE.json')['main']);out.write_text(json.dumps(r,indent=2)+'\n');print(json.dumps({'completeInputFilesVerified':r['completeInputFilesVerified']}))
