package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import game.sanguo.core.*;
import game.sanguo.api.*;
import java.util.*;
import java.util.function.Consumer;

/** Thin native entry point; every mutation is delegated to Strategy. */
final class StrategyUi {
    private final MainActivity activity;
    private final World w;
    private final LegacyCommandSink apply;
    StrategyUi(Activity activity,World w,LegacyCommandSink apply){this.activity=(MainActivity)activity;this.w=w;this.apply=apply;}
    private void info(String title,String text){activity.trackDialog(new AlertDialog.Builder(activity).setTitle(title).setMessage(text).setPositiveButton("返回",null).show());}
    private void confirm(String title,String text,Runnable action){
        activity.commandDialog(title,text+"\n消耗行动力10，执行武将本旬不可再次行动。","执行",w,action);
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
        if(n==2){choose("选择登用目标",w.strategy.recruitmentTargets(c.id),target->{
            List<World.Officer> actors=w.loyalty.recruitmentActors(c.id,target.id);
            chooseRecruiter(c,target,actors,
                o->confirm("登用"+target.name,"消耗金100；当前成功率 "+w.strategy.recruitmentChance(c.id,o.id,target.id)+"%。\n"+
                    w.loyalty.recommendation(c.id,target.id,o.id)+"\n"+w.recruitment.travelDescription(c.id,target.id)+
                    "\n抵达时会按目标当时忠诚与归属重新判定，并非必然成功。",()->apply.execute(w,()->w.strategy.recruitOfficer(c.id,o.id,target.id))),null);
        });return;}
        if(n==3){batchReward(c);return;}
        if(n==4){final AlertDialog[] targets={null};targets[0]=DataTable.chooseRetained(activity,w,"选择太守",w.idle(c),t->{
            chooseCityActor(c,"任命"+t.name,"APPOINT_GOVERNOR",new int[]{t.id},targets[0]::dismiss,()->{if(targets[0].isShowing())activity.trackDialog(targets[0]);});
        });return;}
        if(n==8){List<World.Officer> targets=new ArrayList<>();for(World.Officer t:w.officers)if(t.cityId==c.id&&w.strategy.canRecruitTarget(c.id,t.id)&&!t.acted)targets.add(t);
            choose("选择舌战登用目标",targets,t->{List<World.Officer> actors=new ArrayList<>();for(World.Officer o:w.idle(c))if(w.contests.debateError(c.id,o.id,t.id)==null)actors.add(o);choose("选择执行武将",actors,o->confirm("舌战说服"+t.name,"消耗金100；获胜后加入本势力，失败不退费。",()->apply.execute(w,()->w.contests.persuade(c.id,o.id,t.id))));});return;}
        basicCommand(c,n);
    }

    private void basicCommand(World.City c,int operation){
        String title=operation==1?"搜索人才":operation==5?"巡察":operation==6?"征兵":"训练";
        chooseCityActor(c,title,operation==1?"SEARCH":operation==5?"PATROL":operation==6?"RECRUIT":"TRAIN",new int[0],null);
    }

    private AlertDialog chooseCityActor(World.City c,String title,String operation,int[] targets,Runnable committed){return chooseCityActor(c,title,operation,targets,committed,null);}
    private AlertDialog chooseCityActor(World.City c,String title,String operation,int[] targets,Runnable committed,Runnable closed){
        final AlertDialog[] picker={null};
        picker[0]=DataTable.chooseRetained(activity,w,title+" · "+c.name+" · 选择执行武将",w.idle(c),o->{
            CityActionCommand command=new CityActionCommand(activity.deploymentState(),operation,c.id,o.id,targets);
            CityActionPreview preview=activity.previewCityAction(command);
            StringBuilder detail=new StringBuilder(c.name+" · 执行武将 "+o.name+"\n");
            if(preview.resources!=null){CityActionPreview.Resources r=preview.resources;
                detail.append("\n消耗金 ").append(r.goldCost).append(" / 行动力 ").append(r.actionPointsCost)
                    .append("\n当前金 ").append(r.goldAvailable).append(" / 行动力 ").append(r.actionPointsAvailable);
            }
            if(!preview.allowed())detail.append("\n\n不能执行：").append(MainActivity.commandError(preview.error,preview.detail));
            if(preview.effects!=null){CityActionPreview.Effects e=preview.effects;
                if(operation.equals("PATROL")||operation.equals("RECRUIT"))detail.append("\n治安 ").append(e.orderBefore).append(" → ").append(e.orderAfter);
                if(operation.equals("TRAIN")||operation.equals("RECRUIT"))detail.append("\n气力 ").append(e.moraleBefore).append(" → ").append(e.moraleAfter);
                if(operation.equals("RECRUIT"))detail.append("\n守军 ").append(e.troopsBefore).append(" → ").append(e.troopsAfter).append("\n兵源 ").append(e.reserveBefore).append(" → ").append(e.reserveAfter);
                if(operation.equals("APPOINT_GOVERNOR"))detail.append("\n新太守：").append(w.officer(e.governorAfter).name);
                for(CityActionPreview.OfficerEffect effect:e.officers){
                    World.Officer officer=w.officer(effect.id);if(officer==null)continue;
                    if(effect.loyaltyBefore!=effect.loyaltyAfter)detail.append("\n").append(officer.name).append(" 忠诚 ").append(effect.loyaltyBefore).append(" → ").append(effect.loyaltyAfter);
                }
                StringJoiner acted=new StringJoiner("、");for(CityActionPreview.OfficerEffect effect:e.officers){World.Officer officer=w.officer(effect.id);if(officer!=null&&!effect.actedBefore&&effect.actedAfter)acted.add(officer.name);}
                if(acted.length()>0)detail.append("\n本旬行动用尽：").append(acted);

            }
            if(preview.search!=null){CityActionPreview.Search search=preview.search;
                detail.append("\n\n有符合条件的人才时，发现检定 ").append(search.officerCheckChance).append("%。")
                    .append("\n进入寻金判定时，成功检定 ").append(search.goldCheckChance).append("%，可能获得金 ").append(search.goldFoundMinimum).append("～").append(search.goldFoundMaximum).append("。")
                    .append("\n以上为条件检定，并非最终结果概率；也可能发现宝物或毫无发现。");
            }
            AlertDialog review=activity.commandDialog(title,detail.toString(),"执行","返回修改",w,()->{
                picker[0].dismiss();if(committed!=null)committed.run();activity.executeCityAction(command);
            });
            review.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(preview.allowed());
            review.setOnDismissListener(d->{if(picker[0].isShowing())activity.trackDialog(picker[0]);});
        },closed);
        if(closed!=null)picker[0].getButton(AlertDialog.BUTTON_NEGATIVE).setText("返回目标");
        return picker[0];
    }

    private void batchReward(World.City c){
        List<World.Officer> targets=new ArrayList<>();for(World.Officer o:w.officers)if(w.strategy.rewardable(c,o))targets.add(o);
        if(targets.isEmpty()){info("批量褒奖","本城没有符合条件的武将；在外、忠诚已满或本旬已褒奖者不可选。");return;}
        Set<Integer> selected=new LinkedHashSet<>();android.widget.LinearLayout host=new android.widget.LinearLayout(activity);host.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.TextView count=new android.widget.TextView(activity);UiTheme.text(count);count.setTextColor(UiTheme.JADE);count.setPadding(16,12,16,12);host.addView(count);
        Runnable update=()->count.setText("已选 "+selected.size()+" / "+targets.size()+" 人 · 选择执行武将后核对费用与效果");
        DataTable<World.Officer> table=DataTable.officers(activity,w,targets,o->"忠诚 "+o.loyalty,o->{if(!selected.add(o.id))selected.remove(o.id);update.run();});
        table.selection(o->selected.contains(o.id));table.list.setOnItemClickListener((parent,v,index,id)->{if(!selected.add((int)id))selected.remove((int)id);table.selection(o->selected.contains(o.id));update.run();});
        host.addView(table,new android.widget.LinearLayout.LayoutParams(-1,Math.round(Math.min(380,Math.max(160,activity.getResources().getConfiguration().screenHeightDp-210))*activity.getResources().getDisplayMetrics().density)));
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("批量褒奖 · "+c.name).setView(host).setPositiveButton("确认选择",null).setNeutralButton("全选",null).setNegativeButton("取消",null).create();
        android.view.ViewTreeObserver.OnGlobalLayoutListener fit=()->{android.graphics.Rect frame=new android.graphics.Rect();host.getWindowVisibleDisplayFrame(frame);int height=Math.max(activity.dp(120),Math.min(activity.dp(380),frame.height()-activity.dp(170)-count.getHeight()));if(table.getLayoutParams().height!=height){table.getLayoutParams().height=height;table.requestLayout();}};
        host.getViewTreeObserver().addOnGlobalLayoutListener(fit);dialog.setOnDismissListener(d->host.getViewTreeObserver().removeOnGlobalLayoutListener(fit));
        dialog.show();dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);update.run();UiTheme.dialog(dialog);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{if(selected.size()==targets.size())selected.clear();else for(World.Officer o:targets)selected.add(o.id);table.selection(o->selected.contains(o.id));update.run();dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText(selected.size()==targets.size()?"清空":"全选");});
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(activity instanceof MainActivity&&!((MainActivity)activity).currentWorld(w))return;if(selected.isEmpty()){count.setText("请至少选择一名武将");return;}
            int[] ids=selected.stream().mapToInt(Integer::intValue).toArray();chooseCityActor(c,"褒奖 "+ids.length+" 人","REWARD",ids,dialog::dismiss,()->{if(dialog.isShowing())activity.trackDialog(dialog);});});
        activity.trackDialog(dialog);

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
