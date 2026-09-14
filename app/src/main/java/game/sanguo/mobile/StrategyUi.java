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
    private void choose(String title,List<World.Officer> officers,Consumer<World.Officer> next){
        if(officers.isEmpty()){info(title,"没有符合条件的武将。");return;}
        String[] labels=new String[officers.size()];
        for(int i=0;i<labels.length;i++){
            World.Officer o=officers.get(i);
            labels[i]=o.name+" · "+o.role.label+" · 政"+o.politics+" / 智"+o.intelligence+" / 魅"+o.charm+" · 忠诚"+o.loyalty;
        }
        new AlertDialog.Builder(activity).setTitle(title).setItems(labels,(d,i)->next.accept(officers.get(i))).setNegativeButton("取消",null).show();
    }
    void city(World.City c){
        String title=c.name+" · 人事 / 城市治理";
        String[] labels={"城市与武将状态","搜索人才","登用武将","褒奖武将","任命太守","巡察","征兵","训练"};
        new AlertDialog.Builder(activity).setTitle(title).setItems(labels,(d,n)->command(c,n)).setNegativeButton("返回",null).show();
    }
    void command(World.City c,int n){
        World.Officer governor=w.officer(c.governorId);
        String title=c.name+" · 人事 / 城市治理";
        if(n==0){
            StringBuilder text=new StringBuilder("太守："+(governor==null?"未任命":governor.name)+"\n兵源 "+c.recruitReserve+" / 守军 "+c.troops+"\n治安 "+c.order+" / 气力 "+w.strategy.getArmyReadiness(c.id)+"\n月金 "+w.domestic.monthlyGold(c.id)+" / 月粮 "+w.domestic.monthlyFood(c.id)+"\n");
            for(World.Officer o:w.officers)if(o.cityId==c.id){
                Strategy.OfficerState s=w.strategy.officerState(o.id);World.City location=w.city(o.cityId);
                text.append('\n').append(o.name).append(" · ").append(o.role.label).append(" · 忠诚").append(o.loyalty)
                    .append("\n").append(location==null?"在途 / 出征 / 无驻地":location.name).append(" · ").append(activityLabel(s.activity));
                if(s.remainingTurns>0)text.append(" · 剩").append(s.remainingTurns).append("旬");
            }
            info(title,text.toString());return;
        }
        choose("选择执行武将",w.idle(c),o->{
            switch(n){
                case 1:confirm("搜索人才","不消耗金；有适时隐士时发现概率 "+w.strategy.searchChance(o.id)+"%。\n也可能获得少量金或毫无发现。",()->apply.accept(w.strategy.search(c.id,o.id)));break;
                case 2:choose("选择登用目标",w.strategy.recruitmentTargets(c.id),target->confirm("登用"+target.name,
                    "消耗金100；成功率 "+w.strategy.recruitmentChance(c.id,o.id,target.id)+"%。失败也消耗资源；新加入武将本旬休整。",
                    ()->apply.accept(w.strategy.recruitOfficer(c.id,o.id,target.id))));break;
                case 3:{
                    List<World.Officer> targets=new ArrayList<>();
                    for(World.Officer target:w.officers)if(target.owner==c.owner&&target.cityId==c.id&&target.role!=Strategy.Role.RULER&&target.loyalty<100&&target.lastRewardTurn!=w.turn&&!w.domestic.busy(target.id)&&!w.strategy.busy(target.id))targets.add(target);
                    choose("选择褒奖目标",targets,target->confirm("褒奖"+target.name,"消耗金200；忠诚 +"+Math.min(100-target.loyalty,StrategyRules.rewardGain(target.politics,target.charm))+"。每名武将每旬最多接受一次褒奖。",()->apply.accept(w.strategy.rewardOfficer(c.id,o.id,target.id))));break;
                }
                case 4:choose("选择太守",w.idle(c),target->confirm("任命"+target.name,"金粮收入加成 "+(target.politics/4)+"%；仍受治安影响。执行者和新太守均消耗本旬行动；出征或调动自动卸任。",()->apply.accept(w.strategy.appointGovernor(c.id,o.id,target.id))));break;
                case 5:confirm("巡察","消耗金100；治安 +"+Math.min(100-c.order,StrategyRules.patrolGain(o.politics,o.charm))+"。",()->apply.accept(w.strategy.patrol(c.id,o.id)));break;
                case 6:confirm("征兵","消耗金300、治安5；预计征兵 "+w.strategy.recruitAmount(c.id,o.id)+"，扣减等量兵源。当前兵源 "+c.recruitReserve+"。",()->apply.accept(w.strategy.recruitSoldiers(c.id,o.id)));break;
                case 7:confirm("训练","消耗金100；气力 +"+Math.min(100-c.morale,StrategyRules.trainingGain(o.leadership,o.war))+"，出征时作为初始部队气力。",()->apply.accept(w.strategy.trainArmy(c.id,o.id)));break;
                default:break;
            }
        });
    }

    private static String activityLabel(Strategy.Activity state){
        switch(state){
            case CAPTIVE:return "被俘虏";case IDLE:return "可行动";case ACTED:return "本旬已行动";case CONSTRUCTION:return "建设中";
            case TRANSFER:return "调动中";case TRANSPORT:return "运输中";case OTHER_TASK:return "战略任务中";
            case DEPLOYED:return "带队出征";case UNAFFILIATED:return "在野";default:return "无有效驻地";
        }
    }
}
