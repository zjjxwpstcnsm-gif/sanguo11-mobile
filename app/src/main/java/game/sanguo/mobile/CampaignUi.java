package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.text.*;
import game.sanguo.core.*;
import game.sanguo.api.*;
import java.util.*;
import java.util.function.Consumer;

/** Native campaign forms; previews never execute commands or advance RNG. */
final class CampaignUi {
    private final MainActivity a;private final World w;private final LegacyCommandSink apply;
    CampaignUi(MainActivity a,World w,LegacyCommandSink apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String title,String text){UiTheme.dialog(new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("返回",null).show());}
    private void confirm(String title,String text,Runnable action){a.commandDialog(title,text,"执行",w,action);}
    private <T> void choose(String title,List<T> values,java.util.function.Function<T,String> label,Consumer<T> next){ChoiceDialog.show(a,w,title,values,label,next);}
    private void officer(World.City c,Consumer<World.Officer> next){choose("选择执行武将",w.idle(c),o->o.name+" · 政"+o.politics+" / 智"+o.intelligence+" / 魅"+o.charm,next);}
    void diplomacy(World.City c){
        List<Integer> sides=new ArrayList<>();for(int side=0;side<w.factions.length;side++)if(side!=c.owner&&w.alive(side))sides.add(side);
        choose("外交 · 选择势力",sides,side->w.faction(side)+" · "+w.campaign.relationLabel(c.owner,side),side->foreignCommands(c,side));
    }
    private void foreignCommands(World.City c,int side){
        DiplomacyUi foreign=new DiplomacyUi(a,w,apply);String[] commands={"亲善","停战","同盟","解除协定","请求援军","劝降","交换俘虏","援军任务"};
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(w.faction(side)).setItems(commands,(d,index)->{
            if(index==7){foreign.missions(side);return;}if(index==4){foreign.aid(c,side);return;}if(index==6){foreign.exchange(c,side);return;}
            if(index==1||index==2){treaty(c,side,index==1?Campaign.TreatyKind.CEASEFIRE:Campaign.TreatyKind.ALLIANCE);return;}
            if(index==0||index==3){reviewDiplomacy(c,side,index==0?"GOODWILL":"BREAK_TREATY",0,index==0?"亲善":"解除协定",()->foreignCommands(c,side));return;}
            foreignOfficer(c,o->foreign.surrender(c,o,side),()->foreignCommands(c,side));
        }).setNegativeButton("返回势力",(d,n)->diplomacy(c)).create();dialog.setOnCancelListener(d->diplomacy(c));dialog.show();a.trackDialog(dialog);
    }
    private void treaty(World.City c,int side,Campaign.TreatyKind kind){
        DiplomacyPreview terms=a.previewDiplomacy(new DiplomacyCommand(a.deploymentState(),c.id,-1,side,kind.name(),0));
        if(terms.treatyDurations.isEmpty()){UiTheme.dialog(new AlertDialog.Builder(a).setTitle("暂时无法设置期限").setMessage(MainActivity.commandError(terms.error,terms.detail)).setNegativeButton("返回外交",(d,n)->foreignCommands(c,side)).show());return;}
        ChoiceDialog.show(a,w,"选择期限",terms.treatyDurations,n->n+"旬",turns->reviewDiplomacy(c,side,kind.name(),turns,kind.label,()->treaty(c,side,kind)),()->foreignCommands(c,side));
    }
    private void reviewDiplomacy(World.City c,int side,String operation,int turns,String title,Runnable back){
        final AlertDialog[] picker={null};
        picker[0]=DataTable.chooseRetained(a,w,title+" · "+w.faction(side)+" · 选择执行武将",w.idle(c),o->{
            DiplomacyCommand command=new DiplomacyCommand(a.deploymentState(),c.id,o.id,side,operation,turns);
            DiplomacyPreview preview=a.previewDiplomacy(command);
            StringBuilder detail=new StringBuilder(c.name+" · 执行武将 "+o.name+"\n目标："+w.faction(side));
            if(preview.resources!=null){DiplomacyPreview.Resources r=preview.resources;detail.append("\n消耗金 ").append(r.goldCost).append(" / 行动力 ").append(r.actionPointsCost).append("\n当前金 ").append(r.goldAvailable).append(" / 行动力 ").append(r.actionPointsAvailable);}
            if(!preview.allowed())detail.append("\n\n不能执行：").append(MainActivity.commandError(preview.error,preview.detail));
            if(preview.forecast!=null){DiplomacyPreview.Forecast f=preview.forecast;
                detail.append("\n当前关系 ").append(f.currentRelation);
                if(f.delayed){detail.append("\n派往 ").append(f.destinationName).append(" · 单程 ").append(duration(f.oneWayTurns)).append(" / 往返 ").append(duration(f.roundTripTurns));
                    detail.append("\n抵达后按当时局面交涉，旅途变化可能影响结果。");
                }else detail.append("\n确认后立即执行。");
                if(f.initialAcceptancePercent>=0)detail.append("\n首次接受检定 ").append(f.initialAcceptancePercent).append("%");
                if(f.debateOnRejection)detail.append("；拒绝后可能进入舌战，不代表最终成功率。");
                if(f.treatyTurns>0)detail.append("\n协定期限 ").append(f.treatyTurns).append("旬，抵达交涉成功后开始生效。");
                if(f.relationDeltaOnSuccess!=0)detail.append("\n成功时双方关系变化 ").append(signed(f.relationDeltaOnSuccess)).append("（受关系上下限限制）");
                if(f.otherRelationsDelta!=0)detail.append("\n其他势力关系变化 ").append(signed(f.otherRelationsDelta)).append("（受关系上下限限制）");
            }
            AlertDialog review=a.commandDialog(title,detail.toString(),"执行","返回修改",w,()->{picker[0].dismiss();a.executeDiplomacy(command);});
            review.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(preview.allowed());review.setOnDismissListener(d->{if(picker[0].isShowing())a.trackDialog(picker[0]);});
        });
        picker[0].getButton(AlertDialog.BUTTON_NEGATIVE).setText("上一步");picker[0].getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->{picker[0].dismiss();back.run();});picker[0].setOnCancelListener(d->back.run());
    }
    private static String duration(int turns){return turns<0?"暂不可预测":turns+"旬";}
    private static String signed(int value){return value>0?"+"+value:Integer.toString(value);}
    private void foreignOfficer(World.City c,Consumer<World.Officer> next,Runnable back){ChoiceDialog.show(a,w,"选择执行武将",w.idle(c),o->o.name+" · 政"+o.politics+" / 智"+o.intelligence+" / 魅"+o.charm,next,back);}
    void rumor(World.City c){
        List<World.City> targets=new ArrayList<>();for(World.City target:w.cities)if(target.owner>=0&&w.campaign.hostile(c.owner,target.owner))targets.add(target);
        choose("流言目标",targets,t->t.name+" · "+w.faction(t.owner)+" · 治安"+t.order,target->officer(c,o->confirm("散布流言",
            "目标 "+target.name+"\n"+w.personnel.description(c.id,target.id)+"\n抵达后结算；成功率 "+w.campaign.rumorChance(o.id,target.id)+"%\n金300、行动力10；失败也消耗资源。\n成功降低治安10、非君主在城武将忠诚5。",()->apply.execute(w,()->w.campaign.rumor(c.id,o.id,target.id)))));
    }
    void trade(World.City c){Bundle draft=new Bundle();draft.putString("kind","trade");draft.putInt("city",c.id);draft.putInt("officer",-1);draft.putString("operation","BUY");new TradeForm(draft).show();}
    void restoreTrade(Bundle draft){new TradeForm(draft).show();}
    /** Only editable intent is retained; every quote and legality decision comes from the session. */
    private final class TradeForm {
        final Bundle draft;final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
        final Runnable pending=this::inspect;AlertDialog dialog;EditText input;TextView quote,error;Button actor,buy,sell,minimum,maximum;TradePreview preview;TradeCommand command;boolean reviewing;
        TradeForm(Bundle source){draft=new Bundle(source);}
        World.City city(){return w.city(draft.getInt("city"));}
        void show(){
            if(city()==null||city().owner!=w.player){a.closeForm();return;}
            LinearLayout host=new LinearLayout(a);host.setOrientation(LinearLayout.VERTICAL);host.setPadding(a.dp(12),0,a.dp(12),0);
            LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(a);scroll.addView(body);host.addView(scroll,new LinearLayout.LayoutParams(-1,a.dp(340)));
            LinearLayout modes=new LinearLayout(a);buy=a.button("买粮",v->mode("BUY"));sell=a.button("卖粮",v->mode("SELL"));modes.addView(buy,new LinearLayout.LayoutParams(0,a.dp(48),1));modes.addView(sell,new LinearLayout.LayoutParams(0,a.dp(48),1));body.addView(modes);
            actor=a.button("",v->DataTable.choose(a,w,"交易 · 选择执行武将",w.idle(city()),o->"",o->{draft.putInt("officer",o.id);update();},null,()->{if(dialog.isShowing())a.trackDialog(dialog);}));actor.setTag("trade.actor");body.addView(actor,new LinearLayout.LayoutParams(-1,a.dp(48)));
            body.addView(a.text("交易粮食数量",14,UiTheme.TEXT));LinearLayout row=new LinearLayout(a);
            input=new EditText(a);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setSingleLine(true);input.setTextColor(UiTheme.TEXT);UiTheme.readable(input);input.setContentDescription("交易粮食数量");input.setTag("trade.amount");input.setSelectAllOnFocus(true);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
            input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);input.setOnEditorActionListener((v,action,event)->{if(action!=android.view.inputmethod.EditorInfo.IME_ACTION_DONE)return false;hideKeyboard();return true;});
            row.addView(input,new LinearLayout.LayoutParams(0,a.dp(48),1));minimum=a.button("最少",v->{if(preview!=null&&preview.quote!=null)input.setText(Integer.toString(preview.quote.minimum));});maximum=a.button("可用上限",v->{if(preview!=null&&preview.quote!=null)input.setText(Integer.toString(preview.quote.availableMaximum));});row.addView(minimum,new LinearLayout.LayoutParams(a.dp(56),a.dp(48)));row.addView(maximum,new LinearLayout.LayoutParams(a.dp(88),a.dp(48)));body.addView(row);
            quote=a.text("核对交易条件…",14,UiTheme.TEXT);quote.setTag("trade.quote");quote.setPadding(0,a.dp(8),0,a.dp(8));body.addView(quote);
            error=a.text("",13,0xfff2aa9e);error.setTag("trade.error");error.setMaxLines(2);error.setEllipsize(TextUtils.TruncateAt.END);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);host.addView(error);
            dialog=new AlertDialog.Builder(a).setTitle("商人 · "+city().name).setView(host).setNegativeButton("取消",(d,n)->a.closeForm()).setPositiveButton("核对交易",null).create();dialog.setOnCancelListener(d->a.closeForm());
            ViewTreeObserver.OnGlobalLayoutListener fit=()->{android.graphics.Rect frame=new android.graphics.Rect();host.getWindowVisibleDisplayFrame(frame);int h=Math.max(a.dp(64),Math.min(a.dp(420),frame.height()-a.dp(180)-error.getHeight()));if(scroll.getLayoutParams().height!=h){scroll.getLayoutParams().height=h;scroll.requestLayout();}if(input.hasFocus())input.requestRectangleOnScreen(new android.graphics.Rect(0,0,input.getWidth(),input.getHeight()),true);};
            host.getViewTreeObserver().addOnGlobalLayoutListener(fit);dialog.setOnDismissListener(d->{handler.removeCallbacks(pending);host.getViewTreeObserver().removeOnGlobalLayoutListener(fit);});dialog.show();a.trackDialog(dialog);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag("trade.review");dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->review());
            input.setText(draft.getString("amount",""));input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(Editable s){update();}});update();
        }
        void hideKeyboard(){((android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);input.clearFocus();}
        void mode(String operation){draft.putString("operation",operation);update();}
        void update(){
            handler.removeCallbacks(pending);preview=null;command=null;dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);minimum.setEnabled(false);maximum.setEnabled(false);
            draft.putString("amount",input.getText().toString());draft.putBoolean("open",true);a.rememberForm(draft);World.Officer o=w.officer(draft.getInt("officer",-1));actor.setText(o==null?"选择执行武将":"执行武将 · "+o.name);
            buy.setSelected("BUY".equals(draft.getString("operation")));sell.setSelected("SELL".equals(draft.getString("operation")));error.setText("核对交易条件…");handler.postDelayed(pending,100);
        }
        void inspect(){
            if(!dialog.isShowing())return;if(!a.currentWorld(w)){preview=null;command=null;error.setText("局面已变化，请关闭后重新选择。");dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);return;}int amount;try{amount=Integer.parseInt(input.getText().toString());}catch(NumberFormatException e){amount=-1;}
            command=new TradeCommand(a.deploymentState(),draft.getString("operation"),city().id,draft.getInt("officer",-1),amount);preview=a.previewTrade(command);StringBuilder s=new StringBuilder();
            if(preview.quote!=null){TradePreview.Quote q=preview.quote;s.append("数量范围 ").append(q.minimum).append("–").append(q.maximum).append("，步长 ").append(q.step).append("粮")
                .append("\n每1000粮 / 金 ").append(q.pricePerThousand).append(" · 行动力 ").append(q.actionPointsCost)
                .append("\n本旬可交易量 ").append(q.quotaRemaining).append("，资源可用上限 ").append(q.availableMaximum)
                .append("\n本旬已成交 ").append(q.tradedBefore).append("粮")
                .append("\n当前金 ").append(q.goldBefore).append(" / 容量 ").append(q.goldCapacity).append("\n当前粮 ").append(q.foodBefore).append(" / 容量 ").append(q.foodCapacity);
                if(q.quantityValid)s.append("\n本次数量 ").append(q.requestedFood).append("粮 · 报价 ").append(q.quotedGold).append("金");
                minimum.setEnabled(true);maximum.setEnabled(q.availableMaximum>=q.minimum);
            }
            if(preview.effects!=null){TradePreview.Effects e=preview.effects;s.append("\n成交后金 ").append(e.goldAfter).append(" / 粮 ").append(e.foodAfter).append("\n成交后行动力 ").append(e.actionPointsAfter).append(e.actedAfter?" · 武将本旬已行动":"");}
            if(!preview.allowed())s.append("\n\n不能执行：").append(MainActivity.commandError(preview.error,preview.detail));quote.setText(s);error.setTextColor(preview.allowed()?UiTheme.JADE:0xfff2aa9e);UiTheme.readable(error);error.setText(preview.allowed()?"条件已核对，确认后成交":MainActivity.commandError(preview.error,preview.detail));dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(preview.allowed());
        }
        void review(){
            if(reviewing)return;handler.removeCallbacks(pending);inspect();if(preview==null||!preview.allowed())return;hideKeyboard();reviewing=true;TradeCommand reviewed=command;
            AlertDialog review=a.commandDialog("BUY".equals(reviewed.operation)?"买入粮草":"卖出粮草",city().name+" · "+actor.getText()+"\n"+quote.getText(),"确认交易","返回修改",w,()->{a.closeForm();dialog.dismiss();a.executeTrade(reviewed);});
            review.setOnDismissListener(d->{reviewing=false;if(dialog.isShowing())a.trackDialog(dialog);});
        }
    }
    void research(World.City c){
        choose("技巧研究 · 九系36项",Arrays.asList(0,1,2,3,4,5,6,7,8),i->Campaign.Tech.BRANCHES[i],branch->researchBranch(c,branch));
    }
    private void researchBranch(World.City c,int branch){
        choose(Campaign.Tech.BRANCHES[branch]+" · 当前"+w.campaign.points(c.owner)+"点",Campaign.Tech.branch(branch),tech->tech.label+(w.campaign.has(c.owner,tech)?" · 已掌握":" · "+tech.points+"点 / 金"+tech.gold),tech->officer(c,o->{
            String error=w.campaign.researchError(c.id,o.id,tech);
            if(error!=null){info(tech.label,error+"\n效果："+tech.effect);return;}
            confirm("研究"+tech.label,tech.effect+"\n消耗"+tech.points+"技巧点、金"+w.campaign.researchGold(o.id,tech)+"、行动力10。\n研究占用"+o.name+tech.turns+"旬，每势力同时研究一项。\n城池失守时中止，费用不退还。",()->apply.execute(w,()->w.campaign.research(c.id,o.id,tech)));
        }));
    }
    void study(World.City c){new AbilityUi(a,w,apply).train(c);}
    void projects(World.City c){
        StringBuilder text=new StringBuilder("技巧点 "+w.campaign.points(c.owner)+"\n每城每旬产出10点（势力上限100点/旬），战斗和部分军政命令也可获得技巧点。\n");
        for(Campaign.Project p:w.campaign.projects())if(p.owner==c.owner)text.append('\n').append(w.city(p.cityId).name).append(" · ").append(w.officer(p.officerId).name).append(" · ").append(p.label()).append(" · 剩").append(w.officer(p.officerId).otherTaskTurns).append("旬");
        for(Campaign.Tech t:Campaign.Tech.values())if(w.campaign.has(c.owner,t))text.append("\n已掌握 ").append(t.label).append("：").append(t.effect);
        UiTheme.dialog(new AlertDialog.Builder(a).setTitle("研究与培养进度").setMessage(text.toString()).setPositiveButton("返回",null)
            .setNeutralButton("中止任务",(d,n)->{
                List<Campaign.Project> own=new ArrayList<>();for(Campaign.Project p:w.campaign.projects())if(p.owner==w.active)own.add(p);
                choose("选择中止的任务",own,p->w.officer(p.officerId).name+" · "+p.label(),p->confirm("中止"+p.label(),"已付金和技巧点不退还，武将本旬仍算已行动。",()->apply.execute(w,()->w.campaign.cancelProject(p.officerId))));
            }).show());
    }
    void repair(World.City c){officer(c,o->confirm("修复城防","金300、行动力10；修复"+w.cityDefense.repairAmount(c,o)+"城防。"+(w.cityDefense.besieged(c)?"\n受围攻，补修效率为平时的¼。":""),()->apply.execute(w,()->w.campaign.repair(c.id,o.id))));}
    void dismiss(World.City c){List<World.Officer> targets=new ArrayList<>();for(World.Officer t:w.officers)if(t.owner==c.owner&&t.cityId==c.id&&!w.domestic.busy(t.id)&&!w.strategy.busy(t.id))targets.add(t);
        choose("流放武将",targets,t->t.name,t->{List<World.Officer> actors=new ArrayList<>(w.idle(c));actors.removeIf(o->o.id==t.id);choose("选择执行武将",actors,o->o.name,o->confirm("流放"+t.name,"行动力10；解除任命并成为本城在野武将。"+(t.role==Strategy.Role.RULER?"\n流放君主将触发继任；相性疏远者忠诚下降。":""),()->apply.execute(w,()->w.campaign.dismiss(c.id,o.id,t.id))));});}
    void buildMilitary(World.City c){info("部队设置", "请在编队出征时携带金，移动到工地相邻格，然后选择部队→设置军事设施。\n施工期间部队自动补修，完成后设施开始生效。");}
    void structures(World.City c){List<War.Structure> options=new ArrayList<>();for(War.Structure s:w.war.structures())if(s.owner==c.owner&&SiteFootprint.distance(c,s.hex)<=3)options.add(s);
        choose("军事设施管理",options,s->s.kind.label+" · "+s.hex+" · 耐久"+s.hp,s->officer(c,o->confirm("拆除"+s.kind.label,"行动力10，不退还建造费用。",()->apply.execute(w,()->w.war.removeStructure(c.id,o.id,s.id)))));}
}
