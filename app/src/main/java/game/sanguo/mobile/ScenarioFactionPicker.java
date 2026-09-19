package game.sanguo.mobile;

import android.app.Dialog;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.IntConsumer;

/** A real-territory opening preview. Selection never changes the template's owner, AP or RNG. */
final class ScenarioFactionPicker {
    private final MainActivity a;private final World w;private final IntConsumer choose;
    private final RealmOverview overview;private final Dialog dialog;
    private final MapView map;private final TextView summary;private final ImageView portrait;
    private final Button start,details;private final List<Button> chips=new ArrayList<>();
    private int selected;
    ScenarioFactionPicker(MainActivity a,World w,IntConsumer choose){
        this.a=a;this.w=w;this.choose=choose;overview=new RealmOverview(w);dialog=new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(a.ink);root.setPadding(a.dp(10),a.dp(8),a.dp(10),a.dp(8));
        LinearLayout heading=new LinearLayout(a);heading.setGravity(Gravity.CENTER_VERTICAL);root.addView(heading,new LinearLayout.LayoutParams(-1,a.dp(48)));
        heading.addView(a.button("返回",v->dialog.dismiss()),new LinearLayout.LayoutParams(a.dp(62),-1));
        TextView title=a.text(w.scenarioName+" · "+w.date(),16,a.gold);title.setMaxLines(2);heading.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        Button fit=a.button("全图",v->{mapFit();});heading.addView(fit,new LinearLayout.LayoutParams(a.dp(62),-1));
        TextView hint=a.text("点选城池或着色领地选择势力 · 拖动 / 双指缩放",12,a.muted);hint.setPadding(a.dp(8),a.dp(6),a.dp(8),a.dp(6));root.addView(hint);
        boolean landscape=a.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout middle=new LinearLayout(a);middle.setOrientation(landscape?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);root.addView(middle,new LinearLayout.LayoutParams(-1,0,1));
        map=new MapView(a,this::tap);map.setContentDescription("开局势力地图 · 点城池或着色领地选择势力");map.previewMode();map.setWorld(w,null,-1);map.setTerritoryMode(1);
        middle.addView(map,landscape?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(8));card.setBackground(UiTheme.surface(a,0xff253d3b,0xff172c34,16));
        LinearLayout lead=new LinearLayout(a);lead.setGravity(Gravity.CENTER_VERTICAL);card.addView(lead);
        portrait=new ImageView(a);lead.addView(portrait,new LinearLayout.LayoutParams(a.dp(54),a.dp(64)));
        summary=a.text("",14,a.paper);summary.setPadding(a.dp(12),0,0,0);lead.addView(summary,new LinearLayout.LayoutParams(0,-2,1));
        details=a.button("查看势力详情 / 技巧树",v->new RealmUi(a,w,new ClientState()).factionDetail(selected,()->{dialog.dismiss();choose.accept(selected);}));details.setContentDescription("查看开局势力详情");card.addView(details,new LinearLayout.LayoutParams(-1,a.dp(42)));
        middle.addView(card,landscape?new LinearLayout.LayoutParams(a.dp(260),-1):new LinearLayout.LayoutParams(-1,-2));
        HorizontalScrollView scroll=new HorizontalScrollView(a);scroll.setHorizontalScrollBarEnabled(false);LinearLayout factions=new LinearLayout(a);scroll.addView(factions);root.addView(scroll,new LinearLayout.LayoutParams(-1,a.dp(48)));
        for(int i=0;i<w.factions.length;i++){final int side=i;Button b=a.button(w.faction(i),v->select(side));b.setContentDescription("选择势力 · "+w.faction(i));b.setEnabled(w.alive(i));chips.add(b);factions.addView(b,new LinearLayout.LayoutParams(a.dp(88),a.dp(46)));}
        start=a.button("",v->{dialog.dismiss();choose.accept(selected);});start.setSelected(true);root.addView(start,new LinearLayout.LayoutParams(-1,a.dp(50)));
        dialog.setContentView(root);dialog.setOnDismissListener(d->{map.criticalFrame(null,0);});
        selected=w.player;for(int i=0;!w.alive(selected)&&i<w.factions.length;i++)selected=i;select(selected);
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
        summary.setText(f.name+"  ·  "+(leader==null?"君主未定":leader.name)+"\n"+f.cities+"城  "+f.ports+"港  "+f.gates+"关  ·  "+f.officers+"将\n兵力 "+f.troops+"  ·  金 "+f.gold+" / 粮 "+f.food);
        summary.setContentDescription("已选势力 · "+f.name+" · 城池"+f.cities+" · 武将"+f.officers);
        start.setText("以「"+f.name+"」开始新局  →");start.setContentDescription("确认开局势力 · "+f.name);start.setEnabled(f.alive);
        for(int i=0;i<chips.size();i++)chips.get(i).setSelected(i==side);map.setPreviewFaction(side);
    }
    void show(){dialog.show();if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout(-1,-1);}mapFit();}
}
