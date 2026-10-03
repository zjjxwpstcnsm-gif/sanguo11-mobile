#!/usr/bin/env python3
"""Preserve all frozen R30 flow assertions; replace global idle with bounded main-queue acknowledgement.
No app/test-source edits. Target package must remain the frozen APK, not this acceptance APK.
"""
from pathlib import Path
import argparse,hashlib,json,subprocess
from build_native_merchant_ui_probe import build,ROOT
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
revision='677f7b88aef37abf412661959c119f3298dd94f1';paths=['app/src/androidTest/java/game/sanguo/mobile/UiUxInstrumentation.java','app/src/androidTest/java/game/sanguo/mobile/SessionProbe.java','app/src/androidTest/java/game/sanguo/mobile/MapTap57Probe.java','app/src/androidTest/java/game/sanguo/mobile/MapTap57Harness.java','app/src/androidTest/java/game/sanguo/mobile/MapView.java','app/src/androidTest/java/game/sanguo/mobile/MapOverview.java','core/src/testFixtures/java/game/sanguo/core/DisplacementFixture.java','core/src/testFixtures/java/game/sanguo/core/MapTap57Fixture.java'];originals=[subprocess.check_output(['git','show',revision+':'+path],cwd=ROOT) for path in paths]
src=originals[0].decode('utf-8');old='private void settle(){SystemClock.sleep(350);waitForIdleSync();}';assert src.count(old)==1
new='''private void settle(){
        SystemClock.sleep(350);
        CountDownLatch acknowledged=new CountDownLatch(1);long began=SystemClock.uptimeMillis();
        if(!new Handler(Looper.getMainLooper()).post(acknowledged::countDown))throw new AssertionError("Main queue refused barrier");
        try{if(!acknowledged.await(10,TimeUnit.SECONDS))throw new AssertionError("Main queue acknowledgement exceeded 10s");}
        catch(InterruptedException failure){Thread.currentThread().interrupt();throw new AssertionError("UI barrier interrupted",failure);}
        long elapsed=SystemClock.uptimeMillis()-began;
        if(elapsed>1000)log.append("NOTE main queue barrier ").append(elapsed).append("ms; UI predicates and full state assertions still required\\n");
    }'''
changed=src.replace(old,new).replace('public class UiUxInstrumentation extends','public class BoundedRuleFlowInstrumentation extends')
# Verify exact reversibility: no assertion, predicate, deadline, pointer input or rule flow changed.
assert changed.replace(new,old).replace('public class BoundedRuleFlowInstrumentation extends','public class UiUxInstrumentation extends')==src
source=out/'BoundedRuleFlowInstrumentation.java';source.write_text(changed,encoding='utf-8');helper=out/'SessionProbe.java';helper.write_bytes(originals[1]);
helpers=[helper]
for path,raw in zip(paths[2:],originals[2:]):
 copy=out/Path(path).name;copy.write_bytes(raw);helpers.append(copy)
build(out/'probe','BoundedRuleFlowInstrumentation',[source]+helpers)
(out/'source-audit.json').write_text(json.dumps(dict(revision=revision,originals=[dict(path=path,sha256=hashlib.sha256(raw).hexdigest()) for path,raw in zip(paths,originals)],changed_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),all_original_assertions_and_flow_byte_preserved=True,scope='Only separate acceptance runner; original historical timeout retained; bounded main queue acknowledgement does not assert renderer globally idle'),indent=2)+'\n')
