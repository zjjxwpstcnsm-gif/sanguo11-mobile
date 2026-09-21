package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import game.sanguo.core.*;
import game.sanguo.core.strategy.StrategyRules;
import java.util.*;
import java.util.function.Consumer;

/** Thin native entry point; every mutation is delegated to Strategy. */
final class StrategyUi {
    private final Activity activity;
    private final World w;
    private final Consumer<World.Result> apply;
    StrategyUi(Activity activity,World w,Consumer<World.Result> apply){this.activity=activity;this.w=w;this.apply=apply;}
    private void info(String title,String text){new AlertDialog.Builder(activity).setTitle(title).setMessage(text).setPositiveButton("返回",null).show();}
    private void confirm(String title,String text,Runnable action){
        new AlertDialog.Builder(activity).setTitle(title).setMessage(text+"\n消耗行动力10，执行武将本旬不可再次行动。")
            .setPositiveButton("执行",(d,n)->action.run()).setNegativeButton("取消",null).show();
    }
    private void choose(String title,List<World.Officer> officers,Consumer<World.Officer> next){DataTable.choose(activity,w,title,officers,o->"",next,null);}
    void city(World.City c){
        String title=c.name+" · 人事 / 城市治理";
        String[] labels={"城市与武将状态","搜索人才","登用武将","褒奖武将","任命太守","巡察","征兵","训练","舌战登用"};
        new AlertDialog.Builder(activity).setTitle(title).setItems(labels,(d,n)->command(c,n)).setNegativeButton("返回",null).show();
    }
    void command(World.City c,int n){
        World.Officer governor=w.officer(c.governorId);
        String title=c.name+" · 人事 / 城市治理";
        if(n==0){
            StringBuilder text=new StringBuilder("太守："+(governor==null?"未任命":governor.name)+"\n"+Conscription.description(w,c)+"\n守军 "+c.troops+"\n治安 "+c.order+" / 气力 "+w.strategy.getArmyReadiness(c.id)+"\n月金 "+w.domestic.monthlyGold(c.id)+" / 季粮 "+w.domestic.monthlyFood(c.id)+"\n");
            for(World.Officer o:w.officers)if(o.cityId==c.id){
                Strategy.OfficerState s=w.strategy.officerState(o.id);World.City location=w.city(o.cityId);
                text.append('\n').append(o.name).append(" · ").append(o.role.label).append(" · 忠诚").append(o.loyalty)
                    .append("\n").append(location==null?"在途 / 出征 / 无驻地":location.name).append(" · ").append(activityLabel(s.activity));
                if(s.remainingTurns>0)text.append(" · 剩").append(s.remainingTurns).append("旬");
            }
            info(title,text.toString());return;
        }
        if(n==6&&c.kind!=World.SiteKind.CITY){info("不能征兵","港口和关卡请从城市运输兵员。");return;}
        if(n==6&&w.domestic.operationError(c.id,Domestic.Kind.BARRACKS)!=null){info("不能征兵",w.domestic.operationError(c.id,Domestic.Kind.BARRACKS));return;}
        if(n==2){choose("选择登用目标",w.strategy.recruitmentTargets(c.id),target->{
            List<World.Officer> actors=w.loyalty.recruitmentActors(c.id,target.id);
            chooseRecruiter(c,target,actors,
                o->confirm("登用"+target.name,"消耗金100；当前成功率 "+w.strategy.recruitmentChance(c.id,o.id,target.id)+"%。\n"+
                    w.loyalty.recommendation(c.id,target.id,o.id)+"\n"+w.recruitment.travelDescription(c.id,target.id)+
                    "\n抵达时会按目标当时忠诚与归属重新判定，并非必然成功。",()->apply.accept(w.strategy.recruitOfficer(c.id,o.id,target.id))),null);
        });return;}
        if(n==3){List<World.Officer> targets=new ArrayList<>();for(World.Officer t:w.officers)if(t.owner==c.owner&&t.cityId==c.id&&t.unitId<0&&t.role!=Strategy.Role.RULER&&t.loyalty<100&&t.lastRewardTurn!=w.turn&&!w.domestic.busy(t.id)&&!w.strategy.busy(t.id))targets.add(t);
            choose("选择褒奖目标",targets,t->choose("选择执行武将",w.idle(c),o->confirm("褒奖"+t.name,"消耗金200；忠诚 +"+Math.min(100-t.loyalty,StrategyRules.rewardGain(t.politics,t.charm))+"。",()->apply.accept(w.strategy.rewardOfficer(c.id,o.id,t.id)))));return;}
        if(n==4){choose("选择太守",w.idle(c),t->choose("选择执行武将",w.idle(c),o->confirm("任命"+t.name,"金粮收入加成 "+(t.politics/4)+"%；执行者和新太守均消耗本旬行动。",()->apply.accept(w.strategy.appointGovernor(c.id,o.id,t.id)))));return;}
        if(n==8){List<World.Officer> targets=new ArrayList<>();for(World.Officer t:w.officers)if(t.cityId==c.id&&w.strategy.canRecruitTarget(c.id,t.id)&&!t.acted)targets.add(t);
            choose("选择舌战登用目标",targets,t->{List<World.Officer> actors=new ArrayList<>();for(World.Officer o:w.idle(c))if(w.contests.debateError(c.id,o.id,t.id)==null)actors.add(o);choose("选择执行武将",actors,o->confirm("舌战说服"+t.name,"消耗金100；获胜后加入本势力，失败不退费。",()->apply.accept(w.contests.persuade(c.id,o.id,t.id))));});return;}
        choose("选择执行武将",w.idle(c),o->{
            switch(n){
                case 1:confirm("搜索人才","不消耗金；有适时隐士时发现概率 "+w.strategy.searchChance(o.id)+"%。\n也可能获得少量金或毫无发现。",()->apply.accept(w.strategy.search(c.id,o.id)));break;
                case 5:confirm("巡察","消耗金100；治安 +"+Math.min(100-c.order,StrategyRules.patrolGain(o.politics,o.charm))+"。",()->apply.accept(w.strategy.patrol(c.id,o.id)));break;
                case 6:confirm("征兵",w.domestic.usage(c.id,Domestic.Kind.BARRACKS)+"\n消耗金300、治安"+Math.min(c.order,w.campaign.orderLoss(c.owner,w.skills.has(o,Skill.MINGSHENG)?7:5))+"；预计征兵 "+w.strategy.recruitAmount(c.id,o.id)+"，扣减等量兵源。\n气力 "+c.morale+"→"+Conscription.moraleAfter(c,w.strategy.recruitAmount(c.id,o.id))+"：新兵未训练，按新老兵人数加权。\n"+Conscription.description(w,c),()->apply.accept(w.strategy.recruitSoldiers(c.id,o.id)));break;
                case 7:confirm("训练","消耗金100；气力 +"+Math.min(w.campaign.energyCap(c.owner)-c.morale,StrategyRules.trainingGain(o.leadership,o.war))+"，出征时作为初始部队气力。",()->apply.accept(w.strategy.trainArmy(c.id,o.id)));break;
                default:break;
            }
        });
    }

    private void chooseRecruiter(World.City city,World.Officer target,List<World.Officer> options,Consumer<World.Officer> next,Runnable back){
        final AlertDialog[] holder={null};int best=options.isEmpty()?-1:options.get(0).id;
        List<DataTable.Column<World.Officer>> columns=new ArrayList<>();
        columns.add(new DataTable.Column<>("姓名",86,o->(o.id==best?"★ ":"")+o.name,Comparator.comparing(o->o.name),false));
        columns.add(new DataTable.Column<>("魅",36,o->""+o.charm,Comparator.comparingInt(o->o.charm),true));
        columns.add(new DataTable.Column<>("政",36,o->""+o.politics,Comparator.comparingInt(o->o.politics),true));
        columns.add(new DataTable.Column<>("相性差",64,o->{int gap=Loyalty.distance(o,target);return gap<0?"—":""+gap;},Comparator.comparingInt(o->{int gap=Loyalty.distance(o,target);return gap<0?76:gap;}),true));
        columns.add(new DataTable.Column<>("成功率",68,o->w.strategy.recruitmentChance(city.id,o.id,target.id)+"%",Comparator.comparingInt(o->w.strategy.recruitmentChance(city.id,o.id,target.id)),true));
        DataTable<World.Officer> table=new DataTable<>(activity,options,columns,new int[]{0,1,2,3,4},o->o.name+" "+w.loyalty.recommendation(city.id,target.id,o.id),o->o.id,
            o->{if(activity instanceof MainActivity&&!((MainActivity)activity).currentWorld(w))return;holder[0].dismiss();next.accept(o);},o->{if(activity instanceof MainActivity)((MainActivity)activity).officerDetail(o);});
        table.selection(o->o.id==best);
        android.widget.LinearLayout host=new android.widget.LinearLayout(activity);host.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.TextView advice=new android.widget.TextView(activity);UiTheme.text(advice);advice.setTextSize(14);advice.setPadding(16,12,16,12);advice.setTag("recruit.advice");
        advice.setText(best<0?"没有本城可用执行者":w.loyalty.recommendation(city.id,target.id,best)+"\n优先人选："+w.officer(best).name+"；相性差越小越接近。概率会在抵达时重算。");host.addView(advice);
        int height=Math.round(Math.max(160,Math.min(340,activity.getResources().getConfiguration().screenHeightDp-220))*activity.getResources().getDisplayMetrics().density);
        host.addView(table,new android.widget.LinearLayout.LayoutParams(-1,height));
        holder[0]=new AlertDialog.Builder(activity).setTitle("登用"+target.name+" · 选择执行者").setView(host).setNegativeButton("取消",null).create();holder[0].show();
        if(activity instanceof MainActivity)((MainActivity)activity).trackDialog(holder[0]);
        holder[0].getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private static String activityLabel(Strategy.Activity state){
        switch(state){
            case CAPTIVE:return "被俘虏";case IDLE:return "可行动";case ACTED:return "本旬已行动";case CONSTRUCTION:return "建设中";
            case TRANSFER:return "调动中";case TRANSPORT:return "运输中";case OTHER_TASK:return "战略任务中";
            case DEPLOYED:return "带队出征";case UNAFFILIATED:return "在野";default:return "无有效驻地";
        }
    }
}
