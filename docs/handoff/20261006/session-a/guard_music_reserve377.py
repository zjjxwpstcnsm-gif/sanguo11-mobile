#!/usr/bin/env python3
"""Exact1 A product path delta, full369/363 inputs, no new B/Bridge/JNI changes."""
from pathlib import Path
import json,subprocess
from read_session_state import read_session_state as read
from run_remaining_normal_media import sha
from guard_capture_queue365 import guard_capture_queue
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent
P='app/src/main/java/game/sanguo/mobile/PcMusicStreamPlayer.java'
def guard_music_reserve(revision,expected_main):
 b=read(D/'MUSIC_RESERVE_BUILD369.json');p=read(D/'CAPTURE_QUEUE_BUILD363.json');delta=read(D/'MUSIC_BUFFER368.json')['paths'];assert b['buildSuccessful'] and revision==b['sourceRevision'];parent=guard_capture_queue(p['sourceRevision'],expected_main)
 changes=subprocess.check_output(['git','diff','--name-only',revision,'--','app/src','app/build.gradle','core','game-api','game-runtime'],cwd=ROOT,text=True).splitlines();assert not changes,changes
 datasets=[]
 for build in [p,b]:
  path=Path(build['candidateInputManifest']);assert sha(path)==build['candidateInputManifestSha256'];rows=read(path);data={x['path']:x for x in rows};assert len(data)==len(rows)==11420;datasets.append((path,data))
 old,new=datasets;assert set(old[1])==set(new[1]);changes={k for k in old[1] if old[1][k]!=new[1][k]};assert changes=={P}=={x['path'] for x in delta}
 for x in delta:assert old[1][x['path']]['sha256']==x['beforeSha256'] and new[1][x['path']]['sha256']==x['afterSha256']
 for path,row in new[1].items():assert sha(new[0].parent/'source'/path)==row['sha256'],path
 for x in b['apks']:assert sha(Path(x['path']))==x['sha256']
 for path,h in b['protectedCurrentSixUnchanged'].items():assert sha(ROOT/path)==h
 return {'parentGuard':parent,'sourceRevision':revision,'completeInputFilesVerified':11420,'full369ManifestSha256':sha(new[0]),'onlyExactAProductDelta':delta,'original5637Assets168PinsSixJniInherited':True,'canonicalProductionChanged':False,'wholeGoalComplete':False}
if __name__=='__main__':
 out=D/'MUSIC_RESERVE_GUARD377.json';assert not out.exists();r=guard_music_reserve(read(D/'MUSIC_RESERVE_BUILD369.json')['sourceRevision'],read(D/'INHERITANCE.json')['main']);out.write_text(json.dumps(r,indent=2)+'\n');print(json.dumps({'completeInputFilesVerified':r['completeInputFilesVerified']}))
