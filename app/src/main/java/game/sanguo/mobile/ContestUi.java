package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.Consumer;

/** Native, scrollable controls. Every input carries the displayed session revision. */
final class ContestUi {
    private final Activity a;private final World w;private final LegacyCommandSink apply;
    private final int paper=Color.rgb(235,227,205),gold=Color.rgb(216,183,116);
    ContestUi(Activity a,World w,LegacyCommandSink apply){this.a=a;this.w=w;this.apply=apply;}
    private int dp(int value){return Math.round(value*a.getResources().getDisplayMetrics().density);}
    private void confirm(String title,String text,Runnable action){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("执行",(d,n)->action.run()).setNegativeButton("取消",null).show();}
    private void info(String title,String text){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("返回",null).show();}
    void challenge(World.Unit actor){
        List<World.Unit> targets=new ArrayList<>();for(World.Unit u:w.units)if(w.contests.duelError(actor.id,u.id)==null)targets.add(u);
        if(targets.isEmpty()){info("单挑","需要相邻的陆上交战部队、正常状态和10气力；器械与水军不能发起。");return;}
        String[] labels=new String[targets.size()];for(int i=0;i<labels.length;i++){World.Unit u=targets.get(i);labels[i]=w.officer(u.officerId).name+" · 应战率"+w.contests.acceptance(actor.id,u.id)+"%";}
        new AlertDialog.Builder(a).setTitle("选择单挑目标").setItems(labels,(d,n)->{
            World.Unit target=targets.get(n);confirm("发起单挑","消耗10气力和本旬行动；对方可能拒绝。\n当前上阵武将体力归零即败，五十合平手。\n主将败北可能被俘并导致部队解散，副将败北则仅退出编队。",()->apply.execute(w,()->w.contests.challenge(actor.id,target.id)));
        }).setNegativeButton("取消",null).show();
    }
    View view(){
        Contests.Session s=w.contests.current();ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);
        LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(10),dp(6),dp(10),dp(12));scroll.addView(panel);
        if(s==null)return scroll;
        if(s.isDuel())duel(panel,s);else debate(panel,s);
        label(panel,"每次操作后自动保存。可到菜单手动保存、导出，或读取另一局。",11,paper);
        return scroll;
    }
    private void label(LinearLayout panel,String text,int size,int color){TextView v=new TextView(a);v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(3),0,dp(5));panel.addView(v);}
    private void button(LinearLayout panel,String text,boolean enabled,Runnable action){
        Button b=CompactButtons.create(a);b.setText(text);b.setTextColor(paper);b.setTextSize(13);b.setAllCaps(false);b.setMinHeight(dp(48));b.setEnabled(enabled);b.setOnClickListener(v->action.run());panel.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));
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
            button(panel,move.label+(move.cost>0?" · 斗志"+move.cost:""),error==null,()->apply.execute(w,()->w.contests.duelMove(id,revision,Duel.Stance.values()[stance.getSelectedItemPosition()],move,-1)));
        }
        for(int i=0;i<d.fighters(0).size();i++){
            final int index=i;Duel.Fighter f=d.fighters(0).get(i);if(f==d.active(0))continue;
            button(panel,"换将 · "+w.officer(f.officerId()).name+(f.joined()?" · 体力"+f.hp():" · 等待支援"),d.moveError(w,Duel.Move.SWAP,index)==null,()->apply.execute(w,()->w.contests.duelMove(id,revision,Duel.Stance.values()[stance.getSelectedItemPosition()],Duel.Move.SWAP,index)));
        }
        button(panel,"认输并结束单挑",true,()->confirm("认输","当前上阵武将直接判负，按败北处置；这不是退却。",()->apply.execute(w,()->w.contests.concede(id,revision))));
        button(panel,"单挑规则",true,()->info("单挑规则","重视攻击：伤害提高，防御与蓄气较弱。\n重视防御：降低伤害，可格挡和完全防御。\n重视斗志：更快蓄气，可额外获得100斗志。\n重视一击：偶尔重击。\n急所使对手负伤，无双清除强化；暗器与伪退需携物且每场一次，伪退需15合。\n支援者到场后可换将，体力与斗志分别保留。"));
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
            if(s.diplomatic())button(panel,"结算外交结果",true,()->apply.execute(w,()->w.contests.finishDebate(id,revision,true)));
            else if(d.winner()==0){button(panel,"留情 · 技巧+50",true,()->apply.execute(w,()->w.contests.finishDebate(id,revision,true)));button(panel,"乘胜追问 · 尝试提升智力",true,()->apply.execute(w,()->w.contests.finishDebate(id,revision,false)));}
            else button(panel,"结算结果",true,()->apply.execute(w,()->w.contests.finishDebate(id,revision,true)));
            return;
        }
        for(int i=0;i<d.speaker(0).hand().size();i++){
            final int index=i;Debate.Card card=d.speaker(0).hand().get(i);button(panel,"出牌 · "+card.label(),d.cardError(i)==null,()->apply.execute(w,()->w.contests.debateCard(id,revision,index)));
        }
        button(panel,"再考 · 更换全部手牌",d.speaker(0).canRethink(),()->apply.execute(w,()->w.contests.rethink(id,revision)));
        button(panel,"认输并结束舌战",true,()->confirm("认输",s.diplomatic()?"协定未成立，出使费用不会返还。":"登用失败，已经消耗的金与行动不会返还。",()->apply.execute(w,()->w.contests.concede(id,revision))));
        button(panel,"舌战规则",true,()->info("舌战规则","本话题优先；相同话题大＞中＞小。\n无视＞大喝＞诡辩＞话题＞镇静＞逆上。\n镇静与逆上承伤后生效；留在手中可自动反制憤激。\n冷静：增强出牌、封印对方话术，每合可再考。\n刚胆：除无视、大喝外压过对方牌。\n小心：打出手中全部话题牌。\n莽撞：爆发心理伤害。\n再考通常需心理台阶下降才恢复。"));
    }
}
