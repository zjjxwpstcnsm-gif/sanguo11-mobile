#!/usr/bin/env python3
"""Keep frozen sustained-flow assertions, then prove actual v37 slot load and exit/reopen.
Only a separate acceptance APK is built; installed production classes remain authoritative.
"""
import argparse,hashlib,json,subprocess
from pathlib import Path
from build_native_merchant_ui_probe import ROOT,build

p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve()
if ROOT/'out' not in out.parents:raise ValueError('Output must be inside project out/')
out.mkdir(parents=True,exist_ok=False)
frozen=ROOT/'out/parity/production-next-implementation/fixed-combo/bounded-rule-flow-probe-with-catalog'
audit=json.loads((frozen/'source-audit.json').read_text());original=(frozen/'BoundedRuleFlowInstrumentation.java').read_bytes()
assert hashlib.sha256(original).hexdigest()==audit['changed_sha256'] and audit['all_original_assertions_and_flow_byte_preserved']
src=original.decode('utf-8');needle='else if("longRun".equals(arguments.getString("suite")))longRun();';assert src.count(needle)==1
method='''
    private void longRunWithContinuation()throws Exception{
        check(Integer.parseInt(arguments.getString("rounds","6"))==24,"extended continuation requires all24 real rounds");
        check((capture()[7]&255)==37,"extended continuation starts from actual v37");
        longRun();byte[] finalState=capture();
        check((finalState[7]&255)==37,"sustained continuation uses actual native v37 campaign");
        Files.write(new File(output,"final-v37-after-24.sg11").toPath(),finalState);
        nav("菜单");text("保存局面");tap(awaitText("槽位 3"));text("覆盖存档");
        check(Arrays.equals(finalState,Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"post-longrun normal slot3 saves every authoritative byte");
        game.sanguo.api.StateToken[] old={null},next={null};ui(()->old[0]=activity.deploymentState());
        nav("菜单");text("读取存档");tap(awaitText("槽位 3"));text("读取存档");
        long deadline=SystemClock.uptimeMillis()+90000;
        while(SystemClock.uptimeMillis()<deadline){ui(()->next[0]=activity.deploymentState());if(!next[0].sessionId.equals(old[0].sessionId)&&Arrays.equals(finalState,capture()))break;settle();}
        check(next[0].generation==old[0].generation+1&&next[0].revision==0&&!next[0].sessionId.equals(old[0].sessionId),"normal post-longrun load installs a fresh authority generation");
        check(Arrays.equals(finalState,capture()),"post-longrun normal load resumes full campaign and rule RNG");
        shot("post-longrun-slot3-loaded");exit3D();
        check(Arrays.equals(finalState,capture()),"post-longrun actual exit and reopen retain complete native v37 state");
        Files.write(new File(output,"reopened-v37-after-24.sg11").toPath(),capture());
    }
'''
assert src.endswith('}\n');changed=src.replace(needle,needle.replace('longRun();','longRunWithContinuation();'))[:-2]+method+'}\n'
assert changed.replace(method,'').replace(needle.replace('longRun();','longRunWithContinuation();'),needle)==src
source=out/'BoundedRuleFlowInstrumentation.java';source.write_text(changed,encoding='utf-8');helpers=[]
for row in audit['originals'][1:]:
    raw=(frozen/Path(row['path']).name).read_bytes();assert hashlib.sha256(raw).hexdigest()==row['sha256']
    target=out/Path(row['path']).name;target.write_bytes(raw);helpers.append(target)
catalog=subprocess.check_output(['git','show',audit['revision']+':app/src/androidTest/assets/pure3d-acceptance.json'],cwd=ROOT)
assert hashlib.sha256(catalog).hexdigest()==audit['acceptance_catalog_sha256']
build(out/'probe','BoundedRuleFlowInstrumentation',[source]+helpers,{'assets/pure3d-acceptance.json':catalog})
(out/'extension-audit.json').write_text(json.dumps({'frozen_audit':audit,'frozen_source_sha256':hashlib.sha256(original).hexdigest(),'extended_source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'inverse_byte_equal':True,'all_original_round_assertions_and_deadlines_preserved':True,'additional_flow':'native37 actual slot3 save/load, generation boundary, actual exit3D/reopen, exported final and reopened whole saves','production_apk_changed':False},indent=2)+'\n')
