#!/usr/bin/env python3
"""A-owned source-bound optional original new-game settings, frozen B26 API."""
from pathlib import Path
import json,hashlib,difflib
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent
S=ROOT/'out/session-a/serial-stage401/source'
def sha(raw):return hashlib.sha256(raw).hexdigest()
def main():
 assert not (D/'OPENING_ADAPTER404.json').exists()
 changes=[];patch=[]
 p='app/src/main/java/game/sanguo/mobile/MainActivity.java';file=S/p;old=file.read_text();text=old
 text=text.replace('private ScenarioFactionPicker factionPicker;','private ScenarioFactionPicker factionPicker;\n    private long scenarioChoiceGeneration;')
 begin=text.index('    private void chooseScenarioTemplate(String id,MapPatch pinned){');end=text.index('\n    private String openingInfo(',begin)
 text=text[:begin]+'''    private void chooseScenarioTemplate(String id,MapPatch pinned){
        final World expected=world;
        final long revision=expected==null?0:expected.commandRevision();
        final long choiceGeneration=++scenarioChoiceGeneration;
        uiReads.run("正在读取剧本…",()->{
            World template=id.startsWith("pc-scen")?PcScenarioCatalog.preview(id):pinned==null?ScenarioCatalog.load(id,0):CustomMaps.preview(pinned,id);
            if(pinned!=null)for(CustomMaps.Issue issue:CustomMaps.diagnose(template,pinned,id))if(issue.blocking())throw new IOException(issue.message());
            PcNewGameOptionsSnapshot options=null;
            if(id.startsWith("pc-scen")){
                options=GameSession.previewNewSourceOptions(id);
                var source=PcScenarioIdentity.saved(template);
                if(source==null||!source.scenarioId.equals(options.scenarioId)||!source.path.equals(options.sourcePath)||!source.sha.equals(options.sourceSha)||!source.sharedSha.equals(options.sharedSha)||!source.sourceVariant.equals(options.sourceVariant))throw new IOException("新局设置与所选剧本来源不同");
            }
            return new Object[]{template,options};
        },prepared->{
            if(choiceGeneration!=scenarioChoiceGeneration||!currentWorld(expected)||(expected!=null&&expected.commandRevision()!=revision))return;
            World template=(World)prepared[0];
            final ScenarioFactionPicker[] ownPicker=new ScenarioFactionPicker[1];
            ownPicker[0]=new ScenarioFactionPicker(this,template,side->{
                if(choiceGeneration!=scenarioChoiceGeneration||!currentWorld(expected)||(expected!=null&&expected.commandRevision()!=revision)){message("未开始新局","当前局面已变化，请重新选择剧本。");return;}
                ScenarioFactionPicker chosen=ownPicker[0];
                startScenario(template.scenarioId,side,pinned,chosen.officerTextSource(),chosen.openingOptions());
            }).openingFacts((PcNewGameOptionsSnapshot)prepared[1])
                .confirmWith(side->openingInfo(template,side)+"\\n人物文字资料："+ownPicker[0].officerTextLabel()+ownPicker[0].openingSummary()+"\\n\\n以"+template.faction(side)+"开始新局？当前自动存档将更新，手动存档保留；损坏的自动存档会另存备份。")
                .onBack(()->{scenarioChoiceGeneration++;scenarioPicker();});
            factionPicker=ownPicker[0];factionPicker.show();
        },error->showError("剧本读取失败："+error.getMessage()));
    }
''' +text[end:]
 needle='''    private void startScenario(String id,int player,MapPatch pinned,String officerTextSource){
        World expected=world;
        uiReads.run("正在建立新局…",()->{
            World resolved=pinned==null?ScenarioCatalog.load(id,player,System.nanoTime()):CustomMaps.load(pinned,id,player,System.nanoTime());'''
 replacement='''    private void startScenario(String id,int player,MapPatch pinned,String officerTextSource){
        startScenario(id,player,pinned,officerTextSource,null);
    }
    private void startScenario(String id,int player,MapPatch pinned,String officerTextSource,PcDuelOptions options){
        World expected=world;long revision=expected==null?0:expected.commandRevision();
        if(options!=null&&(!id.startsWith("pc-scen")||pinned!=null)){showError("新局设置不适用于所选剧本");return;}
        final long seed=System.nanoTime();
        uiReads.run("正在建立新局…",()->{
            World resolved=options!=null?PcScenarioCatalog.load(id,player,seed,options):pinned==null?ScenarioCatalog.load(id,player,seed):CustomMaps.load(pinned,id,player,seed);'''
 assert text.count(needle)==1;text=text.replace(needle,replacement)
 needle='''        },next->{
            if(!currentWorld(expected))return;
            if(!activateWorld(next))return;selectAndFocus(world.home().hex);'''
 assert text.count(needle)==1;text=text.replace(needle,'''        },next->{
            if(!currentWorld(expected)||(expected!=null&&expected.commandRevision()!=revision))return;
            if(!activateWorld(next))return;selectAndFocus(world.home().hex);''')
 changes.append({'path':p,'beforeSha256':sha(old.encode()),'afterSha256':sha(text.encode())});patch.extend(difflib.unified_diff(old.splitlines(True),text.splitlines(True),fromfile='a/'+p,tofile='b/'+p));file.write_text(text)
 p='app/src/main/java/game/sanguo/mobile/ScenarioFactionPicker.java';file=S/p;old=file.read_text();text=old
 text=text.replace('import game.sanguo.core.*;','import game.sanguo.core.*;\nimport game.sanguo.api.PcNewGameOptionsSnapshot;\nimport game.sanguo.api.PcOpeningOptionsSnapshot;')
 text=text.replace('private final Button start,details;', 'private final Button start,details,openingSettings;')
 text=text.replace('    private AlertDialog confirmationDialog;', '    private AlertDialog confirmationDialog;\n    private AlertDialog openingDialog;')
 text=text.replace('    private String officerTextSource;','''    private PcNewGameOptionsSnapshot openingFacts;
    private PcDuelOptions openingOptions;
    private Map<String,Integer> openingChoices=Map.of();
    private String officerTextSource;''')
 needle='''        start=a.button("",v->accept(selected));'''
 assert text.count(needle)==1;text=text.replace(needle,'''        openingSettings=a.button("原新局设置：沿用现有开局",v->showOpeningSettings());
        openingSettings.setContentDescription("选择原新局设置");openingSettings.setVisibility(View.GONE);
        root.addView(openingSettings,new LinearLayout.LayoutParams(-1,a.dp(48)));
        start=a.button("",v->accept(selected));''')
 needle='''    ScenarioFactionPicker confirmWith(IntFunction<String> message){'''
 assert text.count(needle)==1;text=text.replace(needle,'''    ScenarioFactionPicker openingFacts(PcNewGameOptionsSnapshot facts){
        if(facts!=null&&!w.scenarioId.equals(facts.scenarioId))throw new IllegalArgumentException("新局来源已变化");
        openingFacts=facts;openingSettings.setVisibility(facts==null?View.GONE:View.VISIBLE);return this;
    }
    PcDuelOptions openingOptions(){return openingOptions;}
    private String selectedLabel(PcOpeningOptionsSnapshot.Group group,Map<String,Integer> values){
        Integer value=values.get(group.id);if(value==null)return "未选择";
        return group.choices.stream().filter(c->c.value==value).map(c->c.label).findFirst().orElseThrow();
    }
    String openingSummary(){
        if(openingFacts==null||openingOptions==null)return "";
        StringBuilder summary=new StringBuilder("\\n新局设置：");
        for(var group:openingFacts.groups)summary.append(group.title).append(" ").append(selectedLabel(group,openingChoices)).append(group.fixedMenuValue!=null?"（原剧本固定）":"").append(" · ");
        if(openingFacts.sourceFlag18!=0)summary.append("寿命按原剧本固定规则生效");
        return summary.toString();
    }
    private void showOpeningSettings(){
        if(openingFacts==null||accepted||confirming||previewReleased)return;
        final PcNewGameOptionsSnapshot facts=openingFacts;
        Map<String,Integer> draft=new HashMap<>(openingChoices);
        LinearLayout rows=new LinearLayout(a);rows.setOrientation(LinearLayout.VERTICAL);rows.setPadding(a.dp(16),a.dp(8),a.dp(16),a.dp(8));
        rows.addView(a.text("请明确选择本次新局设置",14,a.paper));
        final Map<String,TextView> labels=new HashMap<>();
        final AlertDialog[] holder=new AlertDialog[1];
        Runnable refresh=()->{
            for(var group:facts.groups)labels.get(group.id).setText(group.title+"："+selectedLabel(group,draft)+(group.fixedMenuValue!=null?"（原剧本固定）":""));
            if(holder[0]!=null)holder[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(facts.groups.stream().allMatch(g->draft.containsKey(g.id)));
        };
        for(var group:facts.groups){
            if(group.fixedMenuValue!=null)draft.put(group.id,group.fixedMenuValue);
            TextView label=a.text("",14,a.paper);labels.put(group.id,label);rows.addView(label);
            LinearLayout choices=new LinearLayout(a);rows.addView(choices);
            for(var choice:group.choices){Button option=a.button(choice.label,v->{draft.put(group.id,choice.value);refresh.run();});
                option.setContentDescription("新局设置 "+group.id+" "+choice.value+" "+choice.label);
                option.setEnabled(group.fixedMenuValue==null||group.fixedMenuValue==choice.value);
                choices.addView(option,new LinearLayout.LayoutParams(0,a.dp(48),1));}
        }
        ScrollView scroll=new ScrollView(a);scroll.addView(rows);
        holder[0]=new AlertDialog.Builder(a).setTitle("原新局设置").setView(scroll).setPositiveButton("采用设置",null)
            .setNeutralButton("沿用现有开局",(d,n)->{openingOptions=null;openingChoices=Map.of();openingSettings.setText("原新局设置：沿用现有开局");})
            .setNegativeButton("返回选择",null).create();
        holder[0].show();a.trackDialog(holder[0]);
        openingDialog=holder[0];
        holder[0].setOnDismissListener(d->{if(openingDialog==holder[0])openingDialog=null;});
        holder[0].getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(previewReleased||accepted||openingFacts!=facts)return;
            PcDuelOptions value=PcDuelOptions.fromMenu(draft.get("life"),draft.get("death"),draft.get("difficulty"));
            openingChoices=Map.copyOf(draft);openingOptions=value;openingSettings.setText("原新局设置：已选择");holder[0].dismiss();
        });refresh.run();
    }
    ScenarioFactionPicker confirmWith(IntFunction<String> message){''')
 text=text.replace('if(confirmationDialog!=null)confirmationDialog.dismiss();', 'if(confirmationDialog!=null)confirmationDialog.dismiss();\n        if(openingDialog!=null)openingDialog.dismiss();')
 changes.append({'path':p,'beforeSha256':sha(old.encode()),'afterSha256':sha(text.encode())});patch.extend(difflib.unified_diff(old.splitlines(True),text.splitlines(True),fromfile='a/'+p,tofile='b/'+p));file.write_text(text)
 (D/'OPENING_ADAPTER404.patch').write_text(''.join(patch));(D/'OPENING_ADAPTER404.json').write_text(json.dumps({'sourcePath':str(S),'paths':changes,'patchSha256':sha(''.join(patch).encode()),'onlyAOwnedProductionChanged':True,'noDefaultAssumed':True,'BCompleteInput':'SERIAL_STAGE402.json','built':False,'actualNormalFlowAccepted':False,'scope':'Optional three explicitly selected original menu groups from frozen B source-bound no-session catalog. Source constraint life2 is a fact; effective life3 stays B policy. Unchosen path keeps legacy three-arg factory; all old reads unmodified. Cancels discard dialog draft; picker callbacks capture local source/options and stale World revision/generation. No rule RNG or B/shared edits. Requires new combined build and actual cancel/new/save/cold/options/native command acceptance.','wholeGoalComplete':False},indent=2)+'\n');print(json.dumps({'changedAPaths':len(changes),'built':False}))
if __name__=='__main__':main()
