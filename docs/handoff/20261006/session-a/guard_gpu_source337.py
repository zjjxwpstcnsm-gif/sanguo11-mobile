#!/usr/bin/env python3
"""Exact approved new A class already inside326; reject all other tracked WIP."""
from pathlib import Path
import json,subprocess
from read_session_state import read_session_state
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
PATH='app/src/main/java/game/sanguo/mobile/SceneIndexLeasePool.java'
def guard_gpu_source(revision,expected_main):
 inherited=read_session_state(DOC/'INHERITANCE.json');source=Path(inherited['source'])
 actual=subprocess.check_output(['git','-C',str(source),'rev-parse','main','HEAD'],text=True).splitlines();assert actual==[expected_main,inherited['base']],'Complete source/main changed; new complete inheritance required'
 build=read_session_state(DOC/'GPU_INDEX_BUILD326.json');diagnostic=read_session_state(DOC/'REPEAT_TEST_BUILD330.json');delta=read_session_state(DOC/'GPU_INDEX_DELTA324.json')
 assert revision==diagnostic['sourceRevision'];assert build['sourceRevision']==revision
 row=next(x for x in delta['paths'] if x['path']==PATH);assert row['beforeSha256'] is None and PATH in read_session_state(DOC/'OWNERSHIP.json')['paths']
 manifest=Path(build['candidateInputManifest']);assert sha(manifest)==build['candidateInputManifestSha256'];entry=next(x for x in read_session_state(manifest) if x['path']==PATH)
 assert entry['sha256']==row['afterSha256']==sha(ROOT/PATH)==sha(manifest.parent/'source'/PATH)
 diagManifest=Path(diagnostic['candidateInputManifest']);assert sha(diagManifest)==diagnostic['candidateInputManifestSha256'];assert next(x for x in read_session_state(diagManifest) if x['path']==PATH)['sha256']==entry['sha256']
 changes=set(subprocess.check_output(['git','diff','--name-only',revision,'--','app/src','app/build.gradle','core','game-api','game-runtime'],cwd=ROOT,text=True).splitlines());assert changes<={PATH},'Unapproved app/core/API/runtime tracked change: '+repr(changes-{PATH})
 for path,h in build['protectedCurrentSixUnchanged'].items():assert sha(ROOT/path)==h
 return {'approvedChanges':sorted(changes),'approvedExactClassSha256':entry['sha256'],'candidate326ManifestSha256':sha(manifest),'diagnostic330ManifestSha256':sha(diagManifest),'protectedSixUnchanged':True,'completeSourceMainUnchanged':actual,'allOtherTrackedProductionChangesRejected':True}
def main():
 out=DOC/'GPU_SOURCE_GUARD337.json';assert not out.exists();r=read_session_state(DOC/'REPEAT_TEST_BUILD330.json');report=guard_gpu_source(r['sourceRevision'],read_session_state(DOC/'INHERITANCE.json')['main']);report.update({'scope':'Compatibility only: exact single new A class was fully inside built326 and330 input manifests before its Git registration. No content changes accepted, no arbitrary whitelist/guard disable, no B WIP import or actual Android acceptance.','wholeGoalComplete':False});out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report),flush=True)
if __name__=='__main__':main()
