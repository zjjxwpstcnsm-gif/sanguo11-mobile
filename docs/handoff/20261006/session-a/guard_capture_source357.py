#!/usr/bin/env python3
"""Exact full356 test delta against330; game326 and canonical production unchanged."""
from pathlib import Path
import json, subprocess
from read_session_state import read_session_state as read
from run_remaining_normal_media import sha
from guard_gpu_source337 import guard_gpu_source
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
def guard_capture_source(revision,expected_main):
 b=read(DOC/'CAPTURE_TEST_BUILD356.json');p=read(DOC/'REPEAT_TEST_BUILD330.json');delta=read(DOC/'CAPTURE_BUFFER355.json')['paths']
 assert b['buildSuccessful'] and b['gameReusedExact326'] and revision==b['sourceRevision']
 previous=guard_gpu_source(p['sourceRevision'],expected_main)
 changes=subprocess.check_output(['git','diff','--name-only',revision,'--','app/src','app/build.gradle','core','game-api','game-runtime'],cwd=ROOT,text=True).splitlines();assert not changes,changes
 manifests=[]
 for build in [p,b]:
  path=Path(build['candidateInputManifest']);assert sha(path)==build['candidateInputManifestSha256'];rows=read(path);data={x['path']:x for x in rows};assert len(data)==len(rows)==11419;manifests.append((path,data))
 old,new=manifests;assert set(old[1])==set(new[1]);actual={k for k in old[1] if old[1][k]!=new[1][k]};assert actual=={x['path'] for x in delta}
 for x in delta:assert old[1][x['path']]['sha256']==x['beforeSha256'] and new[1][x['path']]['sha256']==x['afterSha256']
 for path,row in new[1].items():assert sha(new[0].parent/'source'/path)==row['sha256'],path
 for apk in b['apks']:assert sha(Path(apk['path']))==apk['sha256']
 assert b['apks'][0]==p['apks'][0]
 return {'sourceRevision':revision,'parentGuard':previous,'full356ManifestSha256':sha(new[0]),'completeInputFilesVerified':len(new[1]),'exactOnlyCaptureDelta':delta,'game326ByteIdentical':True,'canonicalProductionChangedSince356':False,'wholeGoalComplete':False}
if __name__=='__main__':
 out=DOC/'CAPTURE_SOURCE_GUARD357.json';assert not out.exists();r=guard_capture_source(read(DOC/'CAPTURE_TEST_BUILD356.json')['sourceRevision'],read(DOC/'INHERITANCE.json')['main']);out.write_text(json.dumps(r,indent=2)+'\n');print(json.dumps(r))
