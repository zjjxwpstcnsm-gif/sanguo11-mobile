package game.sanguo.mobile;

import android.app.Dialog;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** A real-territory opening preview. Selection never changes the template's owner, AP or RNG. */
final class ScenarioFactionPicker {
    private final MainActivity a;private final World w;private final IntConsumer choose;
    private final RealmOverview overview;private final Dialog dialog;
    private final MapHost map;private final TextView summary;private final ImageView portrait;
    private final Button start,details;private final List<Button> chips=new ArrayList<>();
    private int selected;
    private boolean modeChosen,accepted,confirming;
    private IntFunction<String> confirmation;
    private Runnable back;
    private AlertDialog confirmationDialog;
    private String officerTextSource;
    private String officerTextLabel="沿用工程资料";
    private WindowSurfaceRecovery windowSurfaceRecovery;
    ScenarioFactionPicker(MainActivity a,World w,IntConsumer choose){
        this.a=a;this.w=w;this.choose=choose;overview=new RealmOverview(w);dialog=new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(a.ink);root.setPadding(a.dp(10),a.dp(8),a.dp(10),a.dp(8));
        LinearLayout heading=new LinearLayout(a);heading.setGravity(Gravity.CENTER_VERTICAL);root.addView(heading,new LinearLayout.LayoutParams(-1,a.dp(48)));
        heading.addView(a.button("返回",v->goBack()),new LinearLayout.LayoutParams(a.dp(62),-1));
        TextView title=a.text(w.scenarioName+" · "+w.date(),16,a.gold);title.setMaxLines(2);heading.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        Button fit=a.button("全图",v->{mapFit();});heading.addView(fit,new LinearLayout.LayoutParams(a.dp(62),-1));
        TextView hint=a.text("点选城池或着色领地选择势力 · 拖动 / 双指缩放"+(pcOpening()?" · 新局默认 3D":""),12,a.muted);hint.setPadding(a.dp(8),a.dp(6),a.dp(8),a.dp(6));root.addView(hint);
        boolean landscape=a.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout middle=new LinearLayout(a);middle.setOrientation(landscape?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);root.addView(middle,new LinearLayout.LayoutParams(-1,0,1));
        map=new MapHost(a,this::tap);map.setContentDescription("开局势力地图 · 点城池或着色领地选择势力");map.previewMode();map.setWorld(w,null,-1);map.setTerritoryMode(1);
        middle.addView(map,landscape?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(8));card.setBackground(UiTheme.surface(a,0xff253d3b,0xff172c34,16));
        LinearLayout lead=new LinearLayout(a);lead.setGravity(Gravity.CENTER_VERTICAL);card.addView(lead);
        portrait=new ImageView(a);lead.addView(portrait,new LinearLayout.LayoutParams(a.dp(54),a.dp(64)));
        summary=a.text("",14,a.paper);summary.setPadding(a.dp(12),0,0,0);lead.addView(summary,new LinearLayout.LayoutParams(0,-2,1));
        details=a.button("查看势力详情 / 技巧树",v->new RealmUi(a,w,new ClientState()).factionDetail(selected,()->accept(selected)));details.setContentDescription("查看开局势力详情");card.addView(details,new LinearLayout.LayoutParams(-1,a.dp(48)));
        ScrollView cardScroll=new ScrollView(a);cardScroll.setFillViewport(false);cardScroll.addView(card);middle.addView(cardScroll,landscape?new LinearLayout.LayoutParams(a.dp(260),-1):new LinearLayout.LayoutParams(-1,-2));
        root.addOnLayoutChangeListener((view,l,t,r,b,ol,ot,or,ob)->{
            boolean wide=r-l>b-t;int orientation=wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL;
            if(middle.getOrientation()==orientation)return;
            middle.setOrientation(orientation);
            map.setLayoutParams(wide?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
            cardScroll.setLayoutParams(wide?new LinearLayout.LayoutParams(a.dp(260),-1):new LinearLayout.LayoutParams(-1,-2));
        });
        HorizontalScrollView scroll=new HorizontalScrollView(a);scroll.setHorizontalScrollBarEnabled(false);LinearLayout factions=new LinearLayout(a);scroll.addView(factions);root.addView(scroll,new LinearLayout.LayoutParams(-1,a.dp(48)));
        for(int i=0;i<w.factions.length;i++){final int side=i;Button b=a.button(w.governance.label(i),v->select(side));b.setContentDescription("选择势力 · "+w.faction(i));b.setEnabled(w.alive(i));chips.add(b);factions.addView(b,new LinearLayout.LayoutParams(a.dp(88),a.dp(48)));}
        root.addView(a.button("自定义武将 · 启用 / 投放 / 校验预览",v->new CustomOfficerPlacementUi(a,w).show()),new LinearLayout.LayoutParams(-1,a.dp(48)));
        Button textSource=a.button("人物文字资料："+officerTextLabel,null);
        textSource.setContentDescription("选择本局人物文字资料来源");
        textSource.setOnClickListener(v->a.chooseOfficerTextSource(source->{
            officerTextSource=source==null?null:source.id;officerTextLabel=source==null?"沿用工程资料":source.label();
            textSource.setText("人物文字资料："+officerTextLabel);
        }));root.addView(textSource,new LinearLayout.LayoutParams(-1,a.dp(48)));
        start=a.button("",v->accept(selected));start.setSelected(true);root.addView(start,new LinearLayout.LayoutParams(-1,a.dp(50)));
        dialog.setContentView(root);dialog.setOnCancelListener(d->{if(back!=null)back.run();});dialog.setOnDismissListener(d->{if(confirmationDialog!=null)confirmationDialog.dismiss();if(windowSurfaceRecovery!=null){windowSurfaceRecovery.close();windowSurfaceRecovery=null;}map.criticalFrame(null,0);map.release();});
        selected=w.player;for(int i=0;!w.alive(selected)&&i<w.factions.length;i++)selected=i;select(selected);
    }
    private boolean pcOpening(){return NationalMap.ID.equals(w.mapId)&&NationalMap.pcRevision(w.mapRevision)&&w.customMapId.isEmpty();}
    ScenarioFactionPicker confirmWith(IntFunction<String> message){confirmation=message;return this;}
    ScenarioFactionPicker onBack(Runnable action){back=action;return this;}
    String officerTextSource(){return officerTextSource;}
    String officerTextLabel(){return officerTextLabel;}
    void dismiss(){dialog.dismiss();}
    private void goBack(){dialog.dismiss();if(back!=null)back.run();}
    private void accept(int side){
        if(accepted||confirming)return;
        if(confirmation==null){commit(side);return;}
        confirming=true;start.setEnabled(false);
        confirmationDialog=new AlertDialog.Builder(a).setTitle("开始新局").setMessage(confirmation.apply(side))
            .setPositiveButton("开始新局",(d,n)->commit(side)).setNegativeButton("返回选择",null).create();
        confirmationDialog.setOnDismissListener(d->{confirming=false;if(!accepted)start.setEnabled(w.alive(selected));confirmationDialog=null;});
        confirmationDialog.show();a.trackDialog(confirmationDialog);
    }
    private void commit(int side){
        if(accepted)return;accepted=true;start.setEnabled(false);
        boolean spatial=true;dialog.dismiss();a.setNextScenario3D(spatial);choose.accept(side);
    }
    private void mapFit(){map.post(map::fit);}
    private void tap(Hex h){
        World.City c=w.cityAt(h);if(c==null&&map.territory()!=null)c=w.city(map.territory().siteAt(h));
        if(c!=null&&c.owner>=0&&w.alive(c.owner))select(c.owner);
        else Toast.makeText(a,"此处没有可选势力，请点选有颜色的领地或下方势力名",Toast.LENGTH_SHORT).show();
    }
    private void select(int side){
        selected=side;RealmOverview.Faction f=overview.factions.get(side);World.Officer leader=null;
        for(World.Officer o:w.officers)if(o.owner==side&&o.role==Strategy.Role.RULER){leader=o;break;}
        portrait.setImageDrawable(leader==null?null:new OfficerPortrait(a,w,leader));
        summary.setText(w.governance.label(side)+" · "+w.governance.title(side)+"\n军师 "+w.governance.advisor(side)+"\n"+f.cities+"城  "+f.ports+"港  "+f.gates+"关  ·  "+f.officers+"将\n兵力 "+f.troops+"  ·  金 "+f.gold+" / 粮 "+f.food);
        summary.setContentDescription("已选势力 · "+f.name+" · 城池"+f.cities+" · 武将"+f.officers);
        start.setText("以「"+w.governance.label(side)+"」开始新局  →");start.setContentDescription("确认开局势力 · "+f.name);start.setEnabled(f.alive);
        for(int i=0;i<chips.size();i++)chips.get(i).setSelected(i==side);map.setPreviewFaction(side);
    }
    void show(){dialog.show();if(dialog.getWindow()!=null)windowSurfaceRecovery=new WindowSurfaceRecovery(dialog.getWindow());if(a.current3D())map.switchMode(true);if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout(-1,-1);}mapFit();}
}
