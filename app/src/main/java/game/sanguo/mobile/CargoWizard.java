package game.sanguo.mobile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import game.sanguo.api.*;
import java.util.*;

/** Editable cargo intent only. Core inspection and the final session command own all rules. */
final class CargoWizard {
    private final MainActivity a;private final World w;private final Bundle draft;
    private AlertDialog dialog;private LinearLayout[] pages;private Button[] tabs,roles;
    private QuantityControl[] amounts;private TextView summary,error;private CheckBox returning;
    private ScrollView scroll;private int equipmentEnd;private boolean reviewing;private Button errorDetails;
    private String[] stockKeys,amountNames;
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable inspectPending=this::inspect;
    private TransportCommand inspectedCommand;private TransportPreview inspected;
    CargoWizard(MainActivity a,World w,Bundle draft){this.a=a;this.w=w;this.draft=new Bundle(draft);}
    static Bundle start(World.City source,World.City target,boolean sea){Bundle d=new Bundle();d.putString("kind","cargo");d.putInt("city",source.id);d.putInt("target",target.id);d.putBoolean("sea",sea);d.putInt("leader",-1);d.putIntArray("deputies",new int[0]);d.putBoolean("open",true);return d;}
    private World.City source(){return w.city(draft.getInt("city"));}
    private World.City target(){return w.city(draft.getInt("target"));}
    private void remember(){draft.putBoolean("open",true);a.rememberForm(draft);}
    void show(){
        World.City c=source(),d=target();if(c==null||d==null||c.owner!=w.player||d.owner!=w.player){a.closeForm();return;}
        LinearLayout host=new LinearLayout(a);host.setOrientation(LinearLayout.VERTICAL);host.setPadding(a.dp(12),0,a.dp(12),a.dp(4));
        summary=a.text(c.name+" → "+d.name,14,UiTheme.BRASS);summary.setTag("cargo.summary");host.addView(summary);
        LinearLayout tabRow=new LinearLayout(a);String[] titles={"运输编队","钱粮兵力","兵装","舰船"};tabs=new Button[titles.length];pages=new LinearLayout[titles.length];
        LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);scroll=new ScrollView(a);scroll.setTag("cargo.scroll");scroll.addView(content);
        for(int i=0;i<pages.length;i++){final int page=i;tabs[i]=a.button(titles[i],v->selectPage(page));tabRow.addView(tabs[i],new LinearLayout.LayoutParams(0,a.dp(48),1));pages[i]=new LinearLayout(a);pages[i].setOrientation(LinearLayout.VERTICAL);content.addView(pages[i]);}
        host.addView(tabRow);host.addView(scroll,new LinearLayout.LayoutParams(-1,a.dp(320)));
        roles=new Button[3];
        for(int i=0;i<3;i++){final int role=i;LinearLayout row=new LinearLayout(a);roles[i]=a.button("",v->pickOfficer(role));roles[i].setTag("cargo.role."+i);row.addView(roles[i],new LinearLayout.LayoutParams(0,a.dp(56),1));Button remove=a.button("移除",v->setOfficer(role,-1));remove.setContentDescription("移除运输"+(i==0?"主将":"副将"+i));row.addView(remove,new LinearLayout.LayoutParams(a.dp(56),a.dp(56)));pages[0].addView(row);}
        returning=new CheckBox(a);returning.setText("卸货后武将返回出发城");returning.setTextColor(UiTheme.TEXT);UiTheme.readable(returning);returning.setMinHeight(a.dp(48));returning.setChecked(draft.getBoolean("returning"));pages[0].addView(returning);
        pages[0].addView(a.text("先选择主将，可选两名副将。\n确认前核对实际路线、耗粮与目的地容量。途中可能被截击。",14,UiTheme.MUTED));
        equipmentEnd=3+World.Weapon.values().length;amounts=new QuantityControl[equipmentEnd+2];int[] stock=new int[amounts.length];stock[0]=c.gold;stock[1]=c.food;stock[2]=c.troops;
        String[] names=amountNames=new String[amounts.length];stockKeys=new String[amounts.length];stockKeys[0]="GOLD";stockKeys[1]="FOOD";stockKeys[2]="TROOPS";names[0]="运输金";names[1]="运输粮";names[2]="运输兵";
        for(World.Weapon weapon:World.Weapon.values()){int i=3+weapon.ordinal();names[i]=weapon.label+"装备";stockKeys[i]=weapon.name();stock[i]=c.equipment[weapon.ordinal()];}
        stockKeys[equipmentEnd]="TOWER_SHIP";stockKeys[equipmentEnd+1]="WARSHIP";names[equipmentEnd]="楼船货物";names[equipmentEnd+1]="斗舰货物";stock[equipmentEnd]=c.ships[0];stock[equipmentEnd+1]=c.ships[1];
        pages[1].addView(a.text("可选范围来自当前运输条件，已计入库存与运载限制；修改数量不会自动派遣。",13,UiTheme.MUTED));
        pages[2].addView(a.text("兵装作为货物运输；未选数量为0。",13,UiTheme.MUTED));
        pages[3].addView(a.text("舰船作为货物运输；不改变运输方式。未选数量为0。",13,UiTheme.MUTED));
        String[] raw=draft.getStringArray("amounts");
        for(int i=0;i<amounts.length;i++){int initial=i==1?5000:i==2?1000:0;amounts[i]=new QuantityControl(a,names[i],0,Math.max(0,stock[i]),Math.min(initial,Math.max(0,stock[i])));amounts[i].rangeDescription(names[i]+" · 出发城库存 "+stock[i]);amounts[i].inputDescription(names[i]);amounts[i].setTag("cargo.amount."+i);if(raw!=null&&i<raw.length)amounts[i].restoreValue(raw[i]);pages[i<3?1:i<equipmentEnd?2:3].addView(amounts[i]);}
        error=a.text("",13,0xfff2aa9e);error.setTag("cargo.error");error.setMinHeight(a.dp(40));error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);LinearLayout feedback=new LinearLayout(a);feedback.setGravity(Gravity.CENTER_VERTICAL);feedback.addView(error,new LinearLayout.LayoutParams(0,-2,1));host.addView(feedback);
        errorDetails=a.button("查看运输条件",v->{});errorDetails.setEnabled(false);feedback.addView(errorDetails,new LinearLayout.LayoutParams(a.dp(132),a.dp(48)));
        dialog=new AlertDialog.Builder(a).setTitle("资源运输").setView(host).setNegativeButton("取消",(x,n)->a.closeForm()).setPositiveButton("预览运输",null).create();dialog.setOnCancelListener(x->a.closeForm());
        // In a short keyboard viewport, all form chrome scrolls rather than squeezing out the focused field.
        android.graphics.Rect frame=new android.graphics.Rect();final int[] lastSize={-1,-1};
        ViewTreeObserver.OnPreDrawListener fit=()->{host.getWindowVisibleDisplayFrame(frame);
            if(lastSize[0]==frame.height()&&lastSize[1]==feedback.getHeight())return true;
            lastSize[0]=frame.height();lastSize[1]=feedback.getHeight();boolean compact=frame.height()<a.dp(320);
            if(compact&&tabRow.getParent()==host){host.removeView(summary);host.removeView(tabRow);host.removeView(feedback);content.addView(summary,0);content.addView(tabRow,1);content.addView(feedback);}
            else if(!compact&&tabRow.getParent()==content){content.removeView(summary);content.removeView(tabRow);content.removeView(feedback);host.addView(summary,0);host.addView(tabRow,1);host.addView(feedback);}
            int height=Math.max(a.dp(64),Math.min(a.dp(380),frame.height()-a.dp(compact?120:220)-(compact?0:feedback.getHeight())));if(scroll.getLayoutParams().height!=height){scroll.getLayoutParams().height=height;scroll.requestLayout();}return true;};
        host.getViewTreeObserver().addOnPreDrawListener(fit);dialog.setOnDismissListener(x->{handler.removeCallbacks(inspectPending);host.getViewTreeObserver().removeOnPreDrawListener(fit);});
        dialog.show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag("cargo.preview");dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->preview());a.trackDialog(dialog);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        for(QuantityControl amount:amounts)amount.onChange(this::update);returning.setOnCheckedChangeListener((v,checked)->update());selectPage(draft.getInt("page",0));update();
    }
    private int[] crew(){int[] result={draft.getInt("leader",-1),-1,-1};int[] deputies=draft.getIntArray("deputies");if(deputies!=null)for(int i=0;i<Math.min(2,deputies.length);i++)result[i+1]=deputies[i];return result;}
    private void setOfficer(int role,int officer){int[] ids=crew();ids[role]=officer;draft.putInt("leader",ids[0]);draft.putIntArray("deputies",Arrays.stream(ids).skip(1).filter(id->id>=0).toArray());update();}
    private void pickOfficer(int role){
        int[] ids=crew();List<World.Officer> options=new ArrayList<>(w.idle(source()));options.removeIf(o->{for(int i=0;i<ids.length;i++)if(i!=role&&ids[i]==o.id)return true;return false;});
        DataTable.choose(a,w,role==0?"选择运输主将":"选择运输副将"+role,options,o->"",o->setOfficer(role,o.id),null,()->{if(dialog.isShowing()){a.trackDialog(dialog);update();}});
    }
    private void selectPage(int page){page=Math.max(0,Math.min(pages.length-1,page));draft.putInt("page",page);for(int i=0;i<pages.length;i++){pages[i].setVisibility(i==page?View.VISIBLE:View.GONE);tabs[i].setSelected(i==page);}scroll.scrollTo(0,0);remember();}
    private void update(){
        handler.removeCallbacks(inspectPending);inspected=null;inspectedCommand=null;errorDetails.setEnabled(false);
        String[] raw=new String[amounts.length];boolean numeric=true;for(int i=0;i<amounts.length;i++){raw[i]=amounts[i].draftValue();numeric&=amounts[i].value()>=0;}draft.putStringArray("amounts",raw);draft.putBoolean("returning",returning.isChecked());
        int[] ids=crew();for(int i=0;i<ids.length;i++){World.Officer o=w.officer(ids[i]);String role=i==0?"主将":"副将"+i;roles[i].setText(role+" · "+(o==null?"选择武将":o.name));roles[i].setContentDescription("运输"+role+" · "+(o==null?"未选择":o.name));}
        World.Officer leader=w.officer(ids[0]);summary.setText(source().name+" → "+target().name+" · "+(leader==null?"未选主将":leader.name)+" · 副将"+(int)Arrays.stream(ids).skip(1).filter(id->id>=0).count()+"人");
        error.setTextColor(UiTheme.MUTED);UiTheme.readable(error);error.setText(numeric?"正在核对运输条件…":"请在钱粮或兵装页填写完整数量。");error.setVisibility(View.VISIBLE);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);remember();
        if(numeric&&!reviewing)handler.postDelayed(inspectPending,100);
    }
    private TransportCommand command(){
        int[] values=new int[amounts.length];for(int i=0;i<values.length;i++)values[i]=amounts[i].value();
        return new TransportCommand(a.deploymentState(),source().id,target().id,draft.getInt("leader",-1),draft.getIntArray("deputies"),values[0],values[1],values[2],Arrays.copyOfRange(values,3,equipmentEnd),draft.getBoolean("sea"),returning.isChecked(),Arrays.copyOfRange(values,equipmentEnd,values.length));
    }
    private void inspect(){
        if(!dialog.isShowing()||reviewing)return;
        if(!a.currentWorld(w)){inspected=null;inspectedCommand=null;errorDetails.setEnabled(false);error.setText("局面已变化，请重新打开运输；本次没有派遣。");error.setVisibility(View.VISIBLE);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);return;}
        for(QuantityControl amount:amounts)if(amount.value()<0)return;
        inspectedCommand=command();inspected=a.previewTransport(inspectedCommand);
        if(inspected.resources!=null){Map<String,TransportPreview.Stock> stocks=new HashMap<>();for(TransportPreview.Stock stock:inspected.resources.stocks)stocks.put(stock.kind,stock);
            for(int i=0;i<amounts.length;i++){TransportPreview.Stock stock=stocks.get(stockKeys[i]);if(stock==null){amounts[i].bounds(0,-1);continue;}
                amounts[i].bounds(0,stock.dispatchLimit);amounts[i].rangeDescription(amountNames[i]+" · 可选 0–"+stock.dispatchLimit+"\n出发城库存 "+stock.sourceAvailable+" · 目的地空位 "+stock.destinationFree);
            }
        }
        boolean valid=true;for(QuantityControl amount:amounts)valid&=amount.valid();
        String hint=draft.getInt("leader",-1)<0?"请选择运输主将。":!inspected.allowed()?MainActivity.commandError(inspected.error,inspected.detail):!valid?"请修正标红的运输数量。":"";
        error.setTextColor(hint.isEmpty()?UiTheme.JADE:0xfff2aa9e);UiTheme.readable(error);error.setText(hint.isEmpty()?"条件已核对 · 行动力 "+inspected.resources.actionPointsCost:hint);error.setVisibility(View.VISIBLE);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(inspected.allowed()&&valid);
        if(inspected.resources!=null){TransportPreview current=inspected;errorDetails.setEnabled(true);errorDetails.setOnClickListener(v->{
            if(!a.currentWorld(w))return;
            AlertDialog details=new AlertDialog.Builder(a).setTitle("运输条件").setMessage(conditions(current,true)).setNegativeButton("返回修改",null).show();a.trackDialog(details);details.setOnDismissListener(x->{if(dialog.isShowing())a.trackDialog(dialog);});
        });}
    }
    private String conditions(TransportPreview preview,boolean allStock){
        StringBuilder text=new StringBuilder();
        if(preview.resources!=null){text.append("行动力 ").append(preview.resources.actionPointsCost).append(" / 当前 ").append(preview.resources.actionPointsAvailable);
            if(allStock)for(TransportPreview.Stock stock:preview.resources.stocks){int index=Arrays.asList(stockKeys).indexOf(stock.kind);text.append("\n").append(index<0?stock.kind:amountNames[index]).append("：可选0–").append(stock.dispatchLimit).append("，目的地空位 ").append(stock.destinationFree);}
        }
        if(!preview.allowed())text.append("\n\n").append(MainActivity.commandError(preview.error,preview.detail));
        if(preview.forecast!=null){TransportPreview.Forecast f=preview.forecast;
            text.append("\n出发位置 ").append(f.departureQ).append(",").append(f.departureR).append(" · 本旬余移动 ").append(f.movementRemaining);
            text.append(f.turns<0?"\n抵达时间暂不可预测":"\n预计 "+f.turns+"旬抵达");
            if(f.foodPerTurn>=0)text.append(" · 每旬耗粮 ").append(f.foodPerTurn);
            if(f.projectedFoodUse>=0)text.append("\n预计旅途耗粮 ").append(f.projectedFoodUse).append(" · 抵达余粮 ").append(f.projectedFoodArrival);
            if(f.shortage)text.append("\n注意：当前携粮不足预计旅途消耗。");
            if(!f.capacityFits)text.append("\n注意：目的地当前容量不足，抵达后可能等待卸货。");
            text.append(f.returnOfficers?"\n卸货后武将返回出发城。":"\n卸货后武将留在目的地。").append("\n以上预测以路线及耗粮条件不变为前提，途中可能受阻或被截击。");
        }
        return text.toString();
    }
    private void preview(){
        if(reviewing)return;handler.removeCallbacks(inspectPending);inspect();if(inspected==null||!inspected.allowed())return;for(QuantityControl amount:amounts)if(!amount.valid())return;
        final TransportCommand command=inspectedCommand;final TransportPreview forecast=inspected;int[] equipment=command.equipment(),ships=command.ships();
        StringBuilder payload=new StringBuilder(source().name+" → "+target().name+"\n编队：");for(int id:crew())if(id>=0)payload.append(w.officer(id).name).append(' ');payload.append("\n金 ").append(command.gold).append(" / 粮 ").append(command.food).append(" / 兵 ").append(command.troops);for(World.Weapon weapon:World.Weapon.values())if(equipment[weapon.ordinal()]>0)payload.append('\n').append(weapon.label).append("装备 ").append(equipment[weapon.ordinal()]);for(int i=0;i<2;i++)if(ships[i]>0)payload.append('\n').append(i==0?"楼船货物 ":"斗舰货物 ").append(ships[i]);
        reviewing=true;dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        AlertDialog confirmation=a.commandDialog("确认运输",payload+"\n\n"+conditions(forecast,false),"确认发送","返回修改",w,()->{
            CommandResult result=a.executeTransport(command);if(result.ok()){a.closeForm();dialog.dismiss();}
        });confirmation.setOnDismissListener(x->{reviewing=false;if(dialog.isShowing()){a.trackDialog(dialog);update();}});
    }
}
