package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import game.sanguo.core.*;
import game.sanguo.api.ContestCommand;
import game.sanguo.api.ContestSnapshot;
import java.util.*;
import java.util.function.Consumer;

/** Native, scrollable controls. Every input carries the displayed session revision. */
final class ContestUi {
    private final MainActivity a;private final World w;private final LegacyCommandSink apply;
    private final int paper=Color.rgb(235,227,205),gold=Color.rgb(216,183,116);
    ContestUi(MainActivity a,World w,LegacyCommandSink apply){this.a=a;this.w=w;this.apply=apply;}
    private int dp(int value){return Math.round(value*a.getResources().getDisplayMetrics().density);}
    private void confirm(String title,String text,Runnable action){UiTheme.dialog(new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("执行",(d,n)->action.run()).setNegativeButton("取消",null).show());}
    private void info(String title,String text){UiTheme.dialog(new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("返回",null).show());}
    void challenge(World.Unit actor){
        List<World.Unit> targets=new ArrayList<>();for(World.Unit u:w.units)if(w.contests.duelError(actor.id,u.id)==null)targets.add(u);
        if(targets.isEmpty()){info("单挑",w.contests.nativeDuelConfigured()?"需要相邻的正常陆上交战部队；请选择可作战的编队。":"需要相邻的陆上交战部队、正常状态和10气力；器械与水军不能发起。");return;}
        String[] labels=new String[targets.size()];for(int i=0;i<labels.length;i++){World.Unit u=targets.get(i);labels[i]=w.officer(u.officerId).name+" · 应战率"+w.contests.acceptance(actor.id,u.id)+"%";}
        UiTheme.dialog(new AlertDialog.Builder(a).setTitle("选择单挑目标").setItems(labels,(d,n)->{
            World.Unit target=targets.get(n);if(w.contests.nativeDuelConfigured()){nativeChallenge(actor,target);return;}confirm("发起单挑","消耗10气力和本旬行动；对方可能拒绝。\n当前上阵武将体力归零即败，五十合平手。\n主将败北可能被俘并导致部队解散，副将败北则仅退出编队。",()->apply.execute(w,()->w.contests.challenge(actor.id,target.id)));
        }).setNegativeButton("取消",null).show());
    }
    private void nativeChallenge(World.Unit actor,World.Unit target){
        List<Contests.DuelCandidate> candidates;
        try{candidates=w.contests.nativeDuelCandidates(actor.id,target.id);}catch(IllegalStateException error){info("单挑",error.getMessage());return;}
        ContestSnapshot current=a.contestSnapshot();String[]labels=new String[candidates.size()];for(int i=0;i<labels.length;i++){var candidate=candidates.get(i);labels[i]=candidate.name+" · 原应战估计"+candidate.chance+"%";}
        UiTheme.dialog(new AlertDialog.Builder(a).setTitle("选择上阵武将").setItems(labels,(dialog,index)->{
            int nominee=candidates.get(index).officerId;confirm("发起单挑","消耗发起部队本旬行动，无预付气力；应战方保留已有行动状态。\n拒绝时按原规则结算气力与兵损。\n本新局采用命令聚焦呈现；原相机边界仍待核实。",()->a.executeContest(w,ignored->ContestCommand.startNativeDuel(current.state,actor.id,target.id,nominee)));
        }).setNegativeButton("取消",null).show());
    }
    View view(){
        Contests.Session s=w.contests.current();ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);
        LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(10),dp(6),dp(10),dp(12));scroll.addView(panel);
        if(s==null)return scroll;
        ContestSnapshot facts=a.contestSnapshot();
        if(facts.kind==ContestSnapshot.Kind.SEARCH_CHOICE&&facts.contestId==s.id()&&facts.revision==s.revision())searchChoice(panel,facts);
        else if(facts.nativeDuel!=null&&facts.contestId==s.id()&&facts.revision==s.revision())nativeDuelFacts(panel,facts);
        else if(facts.nativeRules&&facts.contestId==s.id()&&facts.revision==s.revision())nativeDebate(panel,facts);
        else if(s.isDuel())duel(panel,s);else if(s.debate()!=null)debate(panel,s);
        label(panel,"每次操作后自动保存。可到菜单手动保存、导出，或读取另一局。",11,paper);
        return scroll;
    }
    private void label(LinearLayout panel,String text,int size,int color){TextView v=new TextView(a);v.setText(text);v.setTextSize(size);v.setTextColor(color);UiTheme.readable(v);v.setPadding(0,dp(3),0,dp(5));panel.addView(v);}
    private void searchChoice(LinearLayout panel,ContestSnapshot facts){
        label(panel,facts.purpose,20,gold);for(var p:facts.speakers)label(panel,p.name+" · 智力 "+p.intelligence+" · 武力 "+p.war,15,paper);label(panel,facts.status,13,paper);
        label(panel,"搜索行动力20，金费0；可保存后继续选择。人物关系特例及未发现后的寻金、宝物分支仍有工程替代。",12,paper);
        button(panel,facts.phase==1?"尝试招揽":"进入舌战",true,()->a.executeContest(w,state->ContestCommand.searchChoice(state,facts.contestId,facts.revision,true)));
        button(panel,facts.phase==1?"不招揽，结束搜索":"放弃舌战，结束招揽",true,()->a.executeContest(w,state->ContestCommand.searchChoice(state,facts.contestId,facts.revision,false)));
    }
    private void button(LinearLayout panel,String text,boolean enabled,Runnable action){
        Button b=CompactButtons.create(a);b.setText(text);b.setTextColor(paper);UiTheme.readable(b);b.setTextSize(13);b.setAllCaps(false);b.setMinHeight(dp(48));b.setEnabled(enabled);b.setOnClickListener(v->action.run());panel.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));
    }
    private void duel(LinearLayout panel,Contests.Session s){
        Duel d=s.duel();final int id=s.id(),revision=s.revision();
        label(panel,"单挑 · 第"+d.round()+" / 50合",20,gold);
        for(int side=0;side<2;side++){
            Duel.Fighter f=d.active(side);label(panel,(side==0?"我方 ":"对方 ")+w.officer(f.officerId()).name+" · 武力"+w.contests.war(w.officer(f.officerId())),15,paper);
            label(panel,"体力 "+f.hp()+" / 100    斗志 "+f.spirit()+" / 300\n"+f.stance().label+"  "+f.effects(),13,paper);
        }
        label(panel,d.report(),12,gold);
        Spinner stance=new Spinner(a);ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,new String[]{"重视攻击","重视防御","重视斗志","重视一击"});stance.setAdapter(adapter);stance.setSelection(d.active(0).stance().ordinal());stance.setContentDescription("单挑行动方针");panel.addView(stance,new LinearLayout.LayoutParams(-1,dp(48)));
        for(Duel.Move move:Duel.Move.values())if(move!=Duel.Move.SWAP){
            String error=d.moveError(w,move,-1);
            button(panel,move.label+(move.cost>0?" · 斗志"+move.cost:""),error==null,()->a.executeContest(w,state->ContestCommand.duel(state,id,revision,Duel.Stance.values()[stance.getSelectedItemPosition()].name(),move.name(),-1)));
        }
        for(int i=0;i<d.fighters(0).size();i++){
            final int index=i;Duel.Fighter f=d.fighters(0).get(i);if(f==d.active(0))continue;
            button(panel,"换将 · "+w.officer(f.officerId()).name+(f.joined()?" · 体力"+f.hp():" · 等待支援"),d.moveError(w,Duel.Move.SWAP,index)==null,()->a.executeContest(w,state->ContestCommand.duel(state,id,revision,Duel.Stance.values()[stance.getSelectedItemPosition()].name(),Duel.Move.SWAP.name(),index)));
        }
        button(panel,"认输并结束单挑",true,()->confirm("认输","当前上阵武将直接判负，按败北处置；这不是退却。",()->a.executeContest(w,state->ContestCommand.concede(state,id,revision))));
        button(panel,"单挑规则",true,()->info("单挑规则","重视攻击：伤害提高，防御与蓄气较弱。\n重视防御：降低伤害，可格挡和完全防御。\n重视斗志：更快蓄气，可额外获得100斗志。\n重视一击：偶尔重击。\n急所使对手负伤，无双清除强化；暗器与伪退需携物且每场一次，伪退需15合。\n支援者到场后可换将，体力与斗志分别保留。"));
    }
    private void nativeDuelFacts(LinearLayout panel,ContestSnapshot facts){
        label(panel,"单挑 · 第"+facts.round+" / "+facts.nativeDuel.roundLimit+"合",20,gold);
        for(var team:facts.nativeDuel.teams)for(var f:team.fighters){
            label(panel,(team.side==0?"我方 ":"对方 ")+f.name+(f.active?" · 上阵":" · 支援"),15,paper);
            label(panel,"体力 "+f.health+" / 100    斗志 "+f.spirit+" / 300",13,paper);
            if(f.terminalOutcome==2)label(panel,"阵亡 · 待战役结算",13,gold);
        }
        label(panel,facts.status,13,paper);
        var options=facts.nativeDuel.openingOptions;if(options!=null&&options.state.equals(facts.state)&&options.hasSavedValues())label(panel,options.groups.stream().map(g->g.title+"："+g.savedLabel()).collect(java.util.stream.Collectors.joining(" · ")),12,paper);
        var specialMenu=facts.phase==3?facts.nativeDuel.choices.stream().filter(c->c.special>=0&&c.enabled()).findFirst().orElse(null):null;
        if(facts.phase==3&&!facts.nativeDuel.terminal){
            button(panel,"选择特殊动作",specialMenu!=null,()->{if(specialMenu!=null)a.executeContest(w,ignored->ContestCommand.nativeDuelInput(facts.state,facts.contestId,facts.revision,specialMenu.stance,specialMenu.special,specialMenu.replacement));});
            if(specialMenu==null)label(panel,"当前没有可用的特殊动作。",12,paper);
        }
        for(var choice:facts.nativeDuel.choices){
            if(facts.phase==3&&choice.special>=0){if(!choice.error.isEmpty())label(panel,choice.label+"："+choice.error,11,paper);continue;}
            button(panel,choice.label+(choice.cost>0?" · 斗志"+choice.cost:""),choice.enabled(),()->a.executeContest(w,ignored->ContestCommand.nativeDuelInput(facts.state,facts.contestId,facts.revision,choice.stance,choice.special,choice.replacement)));
            if(!choice.error.isEmpty())label(panel,choice.error,11,paper);
        }
        if(facts.nativeDuel.terminal){
            var recovery=facts.nativeDuel.physicalRecovery;
            if(recovery!=null&&recovery.enabled)label(panel,"原逐旬体力恢复已采用",12,paper);
            if(recovery!=null&&recovery.adoptionAvailable)button(panel,"采用原逐旬体力恢复",true,()->confirm("采用原逐旬体力恢复","当前体力、伤病、人物、对局和随机数保持。后续完整旬按原体力+30及伤病上限恢复；已有保存不会自动采用。",()->a.executeContest(w,ignored->ContestCommand.adoptNativePhysicalRecovery(facts.state,facts.contestId,facts.revision))));

            if(facts.nativeDuel.humanActorOfficerId>=0)label(panel,(facts.nativeDuel.humanActorPolicyEnabled?"原登用判定君主 · ":"旧登用判定主将 · ")+facts.nativeDuel.humanActorName,12,paper);
            if(facts.nativeDuel.humanActorPolicyAdoptionAvailable)button(panel,"采用原人工登用君主绑定",true,()->confirm("采用原人工登用判定","后续单挑人工登用按当前势力君主判定；已核实Source0城市内登用允许零距离归队，任务保留到下一次人员结算。人物数值、部队与驻点、原对局和随机数保留；已有登用尝试或处置选择不重做。",()->a.executeContest(w,ignored->ContestCommand.adoptNativeHumanActor(facts.state,facts.contestId,facts.revision))));

            if(facts.nativeDuel.recruitItemRecipientOfficerId>=0)label(panel,(facts.nativeDuel.recruitItemPolicyEnabled?"原登用宝物接收者 · ":"旧登用宝物接收者 · ")+facts.nativeDuel.recruitItemRecipientName,12,paper);
            if(facts.nativeDuel.recruitItemPolicyAdoptionAvailable)button(panel,"采用原登用宝物归属",true,()->confirm("采用原登用宝物归属","后续合法登用时，玉玺与铜雀交给当前势力君主，其余宝物由被登用者保留。现有物品、人物、驻点和随机数不变；已作出的尝试或处置不重做。",()->a.executeContest(w,ignored->ContestCommand.adoptNativeRecruitItemRecipient(facts.state,facts.contestId,facts.revision))));
            if(facts.nativeDuel.aiActorOfficerId>=0)label(panel,(facts.nativeDuel.aiActorPolicyEnabled?"原AI判定君主 · ":"旧AI判定主将 · ")+facts.nativeDuel.aiActorName,12,paper);
            if(facts.nativeDuel.aiActorPolicyAdoptionAvailable)button(panel,"采用原AI势力君主绑定",true,()->confirm("采用原AI君主判定","这份存档后续的单挑自动处置改由当前势力君主判定。人物数值、原对局和随机数保留；部队驻点仍按部队绑定。已作出的处置不重选。",()->a.executeContest(w,ignored->ContestCommand.adoptNativeAiActor(facts.state,facts.contestId,facts.revision))));

            if(facts.nativeDuel.loyaltyInputAdoptionAvailable){
                label(panel,"当前保存仍保留原忠诚策略，可明确采用已核实的唯一输入解析。",12,paper);
                button(panel,"采用已核实的忠诚输入解析",true,()->confirm("采用忠诚输入解析","显示忠诚低于100时，按原显示函数求唯一原始输入；显示100仍保留未知。人物数值、原对局和随机数不改。这不恢复未记录的PC季度变化。",()->a.executeContest(w,ignored->ContestCommand.adoptNativeLoyaltyInput(facts.state,facts.contestId,facts.revision))));
            }else if(facts.nativeDuel.loyaltyInputEnabled)label(panel,"已采用忠诚唯一输入解析；显示100的未知原始值仍不推定。",12,paper);
            var inheritance=facts.nativeDuel.inheritance;
            if(inheritance!=null){
                label(panel,inheritance.selectedHeir<0?"选择本势力继承人":"继承人已选择",16,gold);
                label(panel,"确认前保留完整终局，可保存后继续选择。",12,paper);
                for(var heir:inheritance.candidates){
                    if(inheritance.selectedHeir<0)button(panel,"继承 · "+heir.name,heir.enabled(),()->confirm("立"+heir.name+"为君主","继承人官职、太守、军团及忠诚按当前规则结算；确认后执行一次。",()->a.executeContest(w,ignored->ContestCommand.nativeDuelHeir(facts.state,facts.contestId,facts.revision,heir.officerId))));
                    else if(heir.officerId==inheritance.selectedHeir)label(panel,"待登位 · "+heir.name,15,paper);
                    if(inheritance.selectedHeir<0&&!heir.error.isEmpty())label(panel,heir.name+"："+heir.error,11,paper);
                }
            }
            String[] names={"登用","拘留","释放","处斩"};
            for(var row:facts.nativeDuel.disposition){
                label(panel,row.name+" · "+(row.choice==4?"尚未选择":names[row.choice]),15,gold);
                if(!row.error.isEmpty())label(panel,row.error,11,paper);
                for(int action=0;action<4;action++){final int selected=action;button(panel,names[action],row.enabled(action),()->a.executeContest(w,ignored->ContestCommand.nativeDuelDisposition(facts.state,facts.contestId,facts.revision,row.officerId,selected)));if(row.choice==4&&!row.actionErrors.get(action).isEmpty())label(panel,names[action]+"："+row.actionErrors.get(action),11,paper);}
            }
            if(!facts.nativeDuel.settlementError.isEmpty())label(panel,facts.nativeDuel.settlementError,12,paper);
            button(panel,facts.nativeDuel.disposition.isEmpty()?"查看战役结果":"确认处置并结算",facts.settlementAvailable,()->a.executeContest(w,ignored->ContestCommand.finishNativeDuel(facts.state,facts.contestId,facts.revision)));
        }
    }
    private void nativeDebate(LinearLayout panel,ContestSnapshot facts){
        label(panel,"舌战 · 第"+facts.round+"合",20,gold);label(panel,facts.purpose,14,gold);label(panel,w.contests.searchOrigin()?"搜索发现→招揽→可选舌战；金费0，终局一次结算搜索行动力20。原认输、关系特例及完整开局仍待核实。":"原卡牌/数值/登用终局；触发准入、起始费用、认输和外交仍待原链核实",12,paper);
        for(int side=0;side<facts.speakers.size();side++){
            ContestSnapshot.Speaker p=facts.speakers.get(side);
            label(panel,(side==0?"我方 ":"对方 ")+p.name+" · "+p.personality,15,paper);
            label(panel,"智力 "+p.intelligence+"    武力 "+p.war+"\n心理 "+p.health+" / "+p.maxHealth+"    怒气 "+p.anger+" / 100"+(p.fury>0?"\n憤激剩余 "+p.fury+"合":""),13,paper);
        }
        label(panel,"当前话题："+facts.topic+" · "+(facts.leader==0?"我方先手":"对方先手"),14,gold);label(panel,facts.status,13,paper);
        if(facts.waitingCard)for(ContestSnapshot.Card card:facts.cards){
            button(panel,(card.nativeCard==0?"再考 · ":"出牌 · ")+card.label,card.enabled(),()->a.executeContest(w,ignored->ContestCommand.card(facts.state,facts.contestId,facts.revision,card.slot)));
            if(!card.error.isEmpty())label(panel,card.error,11,paper);
        }
        if(facts.waitingMercy){
            button(panel,"选择智力经验奖励",true,()->a.executeContest(w,ignored->ContestCommand.finishDebate(facts.state,facts.contestId,facts.revision,true)));
            button(panel,"选择势力技巧奖励",true,()->a.executeContest(w,ignored->ContestCommand.finishDebate(facts.state,facts.contestId,facts.revision,false)));
        }
        if(facts.phase==9){label(panel,facts.winner==0?"舌战获胜 · 等待结算":"舌战落败 · 等待结算",17,gold);
            button(panel,"结算原登用终局",true,()->a.executeContest(w,ignored->ContestCommand.finishDebate(facts.state,facts.contestId,facts.revision,false)));
        }
        if(!w.contests.nativeCampaignSettlementEnabled())button(panel,"旧原舌战 · 明确采用已核实终局策略",true,()->confirm("采用原终局策略","仅为这份存档启用原数值和登用回调，并为新增原伤病启用恢复策略；已有伤病保持原策略。不重抽手牌、改人物能力或随机数。原准入及起始费用仍是已有工程规则。原认输与外交回调尚未闭合。",()->a.executeContest(w,ignored->ContestCommand.adoptNativeSettlement(facts.state,facts.contestId,facts.revision))));
    }
    private void debate(LinearLayout panel,Contests.Session s){
        Debate d=s.debate();final int id=s.id(),revision=s.revision();
        label(panel,"舌战 · 第"+d.round()+"合",20,gold);
        label(panel,s.purpose(),14,gold);
        for(int side=0;side<2;side++){
            Debate.Speaker p=d.speaker(side);label(panel,(side==0?"我方 ":"对方 ")+w.officer(p.officerId()).name+" · "+p.temper().label,15,paper);
            label(panel,"心理 "+p.hp()+" / 100    怒气 "+p.anger()+" / 100"+(p.fury()>0?"\n憤激剩余 "+p.fury()+"合":""),13,paper);
        }
        label(panel,"当前话题："+d.topic().label+" · "+d.opponentOpening(),14,gold);label(panel,d.report(),12,paper);
        if(d.winner()!=-2){
            label(panel,d.winner()==0?"舌战获胜 · 等待结算":d.winner()==1?"舌战落败":"舌战平手",18,gold);
            if(s.diplomatic())button(panel,"结算外交结果",true,()->a.executeContest(w,state->ContestCommand.finishDebate(state,id,revision,true)));
            else if(d.winner()==0){button(panel,"留情 · 技巧+50",true,()->a.executeContest(w,state->ContestCommand.finishDebate(state,id,revision,true)));button(panel,"乘胜追问 · 尝试提升智力",true,()->a.executeContest(w,state->ContestCommand.finishDebate(state,id,revision,false)));}
            else button(panel,"结算结果",true,()->a.executeContest(w,state->ContestCommand.finishDebate(state,id,revision,true)));
            return;
        }
        for(int i=0;i<d.speaker(0).hand().size();i++){
            final int index=i;Debate.Card card=d.speaker(0).hand().get(i);button(panel,"出牌 · "+card.label(),d.cardError(i)==null,()->a.executeContest(w,state->ContestCommand.card(state,id,revision,index)));
        }
        button(panel,"再考 · 更换全部手牌",d.speaker(0).canRethink(),()->a.executeContest(w,state->ContestCommand.rethink(state,id,revision)));
        button(panel,"认输并结束舌战",true,()->confirm("认输",s.diplomatic()?"协定未成立，出使费用不会返还。":"登用失败，已经消耗的金与行动不会返还。",()->a.executeContest(w,state->ContestCommand.concede(state,id,revision))));
        button(panel,"舌战规则",true,()->info("舌战规则","本话题优先；相同话题大＞中＞小。\n无视＞大喝＞诡辩＞话题＞镇静＞逆上。\n镇静与逆上承伤后生效；留在手中可自动反制憤激。\n冷静：增强出牌、封印对方话术，每合可再考。\n刚胆：除无视、大喝外压过对方牌。\n小心：打出手中全部话题牌。\n莽撞：爆发心理伤害。\n再考通常需心理台阶下降才恢复。"));
    }
}
