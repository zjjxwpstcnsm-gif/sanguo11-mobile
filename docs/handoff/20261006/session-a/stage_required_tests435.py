#!/usr/bin/env python3
"""Own actual touch tests choose explicit original settings, never injected policy."""
from pathlib import Path
import json
from stage_serial401 import ROOT,D,sha
S=ROOT/'out/session-a/required-opening433/source'
def main():
 changes=[]
 p=S/'app/src/androidTest/java/game/sanguo/mobile/SessionAFireFlowInstrumentation.java';before=sha(p);s=p.read_text();needle='text("曹操");description("确认开局势力");';assert s.count(needle)==1;s=s.replace(needle,'text("曹操");configurePcOpening(source.identity.scenarioId,0,0,0);description("确认开局势力");');p.write_text(s);changes.append({'path':str(p.relative_to(S)),'beforeSha256':before,'afterSha256':sha(p)})
 p=S/'app/src/androidTest/java/game/sanguo/mobile/SessionAMapRepairInstrumentation.java';before=sha(p);s=p.read_text();needle='''    private void retired(MapHost host)throws Exception{''';assert s.count(needle)==1;s=s.replace(needle,'''    private void explicitOpening(String sourceId)throws Exception{
        byte[] before=capture();StateToken prior=token();
        tap(await(v->"pc.opening.options".equals(v.getTag())));
        var facts=game.sanguo.runtime.GameSession.previewNewSourceOptions(sourceId);
        for(var group:facts.groups){
            int value=group.fixedMenuValue==null?0:group.fixedMenuValue;
            View choice=await(v->("pc.opening."+group.id+"."+value).equals(v.getTag()));
            if(group.fixedMenuValue==null)tap(choice);else check(choice.isSelected()&&!choice.isEnabled(),"fixed original source option locked");
        }
        tap(await(v->"pc.opening.confirm".equals(v.getTag())));unchanged(before,prior,"explicit settings are still local draft");
    }
    private void retired(MapHost host)throws Exception{''')
 needle='''            tap(await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("确认开局势力")));text("开始新局");''';assert s.count(needle)==1;s=s.replace(needle,'''            explicitOpening(source.identity.scenarioId);tap(await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("确认开局势力")));text("开始新局");''');p.write_text(s);changes.append({'path':str(p.relative_to(S)),'beforeSha256':before,'afterSha256':sha(p)})
 p=S/'app/src/androidTest/java/game/sanguo/mobile/SessionANativeDuelOpeningInstrumentation.java';before=sha(p);s=p.read_text();needle='''    description("选择原新局设置");View apply=''';assert s.count(needle)==1;s=s.replace(needle,'''    View newGame=await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("确认开局势力"));check(!newGame.isEnabled(),"normal PC new-game disabled until explicit settings");
    description("选择原新局设置");View apply=''');p.write_text(s);changes.append({'path':str(p.relative_to(S)),'beforeSha256':before,'afterSha256':sha(p)})
 rows=[{'path':str(f.relative_to(S)),'sha256':sha(f),'bytes':f.stat().st_size,'mode':f.stat().st_mode&0o777} for f in sorted(S.rglob('*')) if f.is_file()];m=S.parent/'required-inputs.json';m.write_text(json.dumps(rows,indent=2)+'\n');(D/'REQUIRED_TESTS435.json').write_text(json.dumps({'sourcePath':str(S),'candidateInputManifest':str(m),'candidateInputManifestSha256':sha(m),'changedOnlyATests':changes,'scope':'Actual A map and fire fixture now select original values through visible tags, same source-fixed constraints as frozen B helper. Fixture choice0 is explicit test input, never a claimed PC default. Normal opening asserts no legacy fallback by disabled start. Save/RNG/Token pure checks retained, no World injection or acceptance deadline changed.','wholeGoalComplete':False},indent=2)+'\n');print(json.dumps({'testPaths':len(changes)}))
if __name__=='__main__':main()
