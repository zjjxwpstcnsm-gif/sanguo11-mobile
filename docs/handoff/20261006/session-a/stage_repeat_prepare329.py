#!/usr/bin/env python3
"""Test-only two real selections plus bounded allocator observer; exact326 game."""
from pathlib import Path
import json
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/repeat-prepare329'
def main():
 assert not OUT.exists();OUT.mkdir(parents=True);base=ROOT/'out/session-a/gpu-index-build326/source';path='app/src/androidTest/java/game/sanguo/mobile/SessionAScenarioPrepareInstrumentation.java';target=OUT/path;target.parent.mkdir(parents=True);target.write_bytes((DOC/'RepeatPrepare329.java.txt').read_bytes());rows=[{'path':path,'beforeSha256':sha(base/path),'afterSha256':sha(target),'stagedPath':str(target)}]
 sampler=next(x for x in json.loads((DOC/'FIRE_MEMORY_SAMPLER300.json').read_text())['paths'] if x['path'].endswith('SessionAMemorySampler.java'));origin=Path(sampler['stagedPath']);assert sha(origin)==sampler['afterSha256'];assert not (base/sampler['path']).exists();copy=OUT/sampler['path'];copy.write_bytes(origin.read_bytes());rows.append({'path':sampler['path'],'beforeSha256':None,'afterSha256':sha(copy),'stagedPath':str(copy)})
 report={'paths':rows,'game326Unchanged':True,'normalWorldOrRuleInjection':False,'functionalHostAndRendererLimitsMillis':120000,'observationOnlyPerPassMillis':600000,'samplerPeriodMillis':250,'samplerRequestsGcOrHeapDump':False,'compiledInstalled':False,'scope':'Only two actual normal Source14 choose/preview/cancel/reselect plus ui-read/scene-cpu stacks and realRuntime/native allocator. Added observer scheduling/allocation overhead explicit. Late120s remainsFAIL; no timeout relaxation or unrelated normal/zoom/fire/cold acceptance.','wholeGoalComplete':False};(DOC/'REPEAT_PREPARE329.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
