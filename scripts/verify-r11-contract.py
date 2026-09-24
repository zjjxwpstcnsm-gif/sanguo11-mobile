#!/usr/bin/env python3
"""Verify observational-only core edits and retained binary/map/backend cohorts against R10."""
from pathlib import Path
import hashlib,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
BASE='5df81a04fd18231d0821471699a4d46eeb827c42'
def git(*args):return subprocess.check_output(['git','-C',str(ROOT),*args])
core=git('diff','--name-only',BASE,'--','core/src/main').decode().splitlines()
assert set(core)=={'core/src/main/java/game/sanguo/core/'+x+'.java' for x in ['Army','War','TurnJournal']},core
for name in ['Army','War']:
    path='core/src/main/java/game/sanguo/core/'+name+'.java'
    old=git('show',BASE+':'+path).decode();new=(ROOT/path).read_text()
    for metadata in ['if(w.turnJournal!=null)w.turnJournal.tactic(tactic);','if(w.turnJournal!=null)w.turnJournal.plot(plot);']:
        new=new.replace(metadata,'')
    assert old==new,'Unexpected non-observational change: '+path
for path in ['game-api','game-runtime','app/src/main/assets','unity','core/src/main/resources']:
    assert not git('diff','--name-only',BASE,'--',path).strip(),'Protected cohort changed: '+path
out=ROOT/'out/r11';out.mkdir(parents=True,exist_ok=True)
assets={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((ROOT/'app/src/main/assets/3d').rglob('*')) if p.is_file()}
(out/'RETAINED_ASSET_SHA256.json').write_text(json.dumps(assets,indent=2)+'\n')
print('R11 contract PASS: only typed observational rule markers; maps/API/runtime/Unity/binary asset cohort unchanged; '+str(len(assets))+' retained asset hashes')
