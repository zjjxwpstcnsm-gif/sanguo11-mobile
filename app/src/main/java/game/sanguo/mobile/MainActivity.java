package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.util.AtomicFile;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import game.sanguo.core.*;

public final class MainActivity extends Activity {
    private World world;
    private MapView map;
    private LinearLayout panel;
    private TextView title,log;
    private Hex selected;
    private int moving=-1;
    private boolean aiRunning;
    private Button nextTurn;
    private final int ink=Color.rgb(17,32,37),paper=Color.rgb(228,220,197),gold=Color.rgb(216,183,116);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);world=DemoScenario.create();
        String restoreError=null;boolean restored=false;
        AtomicFile autosave=file("auto");
        if(autosave.getBaseFile().exists()||new File(autosave.getBaseFile()+".bak").exists()) {
            try{world=readSave(autosave);restored=true;}catch(IOException e){restoreError=e.getMessage();}
        }
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(ink);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(safe.left,safe.top,safe.right,safe.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(16),dp(4),dp(8),dp(4));
        title=text("",16,gold);header.addView(title,new LinearLayout.LayoutParams(0,dp(44),1));
        header.addView(button("菜单",v->menu()),new LinearLayout.LayoutParams(dp(80),dp(44)));
        root.addView(header);
        LinearLayout body=new LinearLayout(this);map=new MapView(this,this::onTile);body.addView(map,new LinearLayout.LayoutParams(0,-1,1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(12),dp(8),dp(12),dp(10));
        scroll.addView(panel);body.addView(scroll,new LinearLayout.LayoutParams(dp(226),-1));root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER_VERTICAL);bottom.setPadding(dp(12),dp(4),dp(8),dp(4));
        log=text("",12,paper);log.setMaxLines(2);bottom.addView(log,new LinearLayout.LayoutParams(0,dp(46),1));
        nextTurn=button("下一旬  →",v->advanceTurn());
        bottom.addView(nextTurn,new LinearLayout.LayoutParams(dp(118),dp(48)));
        root.addView(bottom);setContentView(root);root.requestApplyInsets();
        selected=world.home().hex;refresh();
        if(!restored&&restoreError==null)root.post(this::scenarioPicker);
        if(restoreError!=null)new AlertDialog.Builder(this).setTitle("自动存档未能读取").setMessage(restoreError+"。当前打开测试新局；手动存档仍可在菜单中读取。").setPositiveButton("知道了",null).show();
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private Button button(String value,View.OnClickListener action){Button b=new Button(this);b.setText(value);b.setTextSize(13);b.setAllCaps(false);b.setTextColor(paper);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(38,58,63)));b.setOnClickListener(action);return b;}
    private void line(String value,int size,int color){TextView t=text(value,size,color);t.setPadding(0,dp(4),0,dp(4));panel.addView(t);}
    private void action(String label,View.OnClickListener click){Button b=button(label,v->{if(!aiRunning)click.onClick(v);});b.setEnabled(!aiRunning);panel.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));}
    private void onTile(Hex h) {
        if(h==null||aiRunning)return;
        World.Unit target=world.unitAt(h);World.City city=world.cityAt(h);World.Unit source=world.unit(moving);
        if(source!=null&&source.owner==world.player) {
            if(target!=null&&target.owner!=world.player){confirm("攻击"+world.officer(target.officerId).name+"部队？",()->apply(world.attack(source.id,target.id)));return;}
            if(city!=null){
                if(city.owner==world.player)confirm("进入"+city.name+"并归还兵装与粮草？",()->{World.Result result=world.enter(source.id,city.id);if(result.ok)moving=-1;apply(result);});
                else confirm("攻击"+city.name+"？",()->apply(world.siege(source.id,city.id)));
                return;
            }
            if(target==null){World.Result result=world.move(source.id,h);if(result.ok)selected=h;apply(result);return;}
        }
        selected=h;moving=target!=null&&target.owner==world.player?target.id:-1;refresh();
    }
    private void confirm(String message,Runnable action){new AlertDialog.Builder(this).setMessage(message).setPositiveButton("执行",(d,w)->action.run()).setNegativeButton("取消",null).show();}
    private void apply(World.Result result){
        if(!result.ok)Toast.makeText(this,result.message,Toast.LENGTH_SHORT).show();
        refresh();if(result.ok)save("auto",false);
        if(result.ok&&world.gameOver())new AlertDialog.Builder(this).setTitle(world.winner==world.player?"战场胜利":"战场战败").setMessage("本局为原创测试沙盘。可选择其他势力或剧本重新开局。").setPositiveButton("查看战场",null).setNeutralButton("重开",(d,w)->restart()).show();
    }
    private void refresh(){
        title.setText(world.scenarioName+"  ·  "+world.faction(world.player)+"     "+world.date()+"     行动力 "+world.actionPoints[world.player]);
        nextTurn.setEnabled(!aiRunning&&!world.gameOver());
        panel.removeAllViews();
        World.Unit unit=selected==null?null:world.unitAt(selected);World.City city=selected==null?null:world.cityAt(selected);
        if(world.unit(moving)==null)moving=-1;
        if(unit!=null)showUnit(unit);else if(city!=null)showCity(city);else {
            line("山河之间",23,gold);line("点选城池或部队",15,paper);line("拖动地图 · 双指缩放\n旗帜颜色代表不同势力。菜单中可按城池定位。",13,paper);
        }
        line("规则测试 · 非原版完整游戏",11,Color.rgb(149,164,163));
        log.setText(world.log.isEmpty()?"":world.log.get(world.log.size()-1));map.setWorld(world,selected,moving);
    }
    private void showCity(World.City c){
        line(c.name,27,gold);line(world.faction(c.owner),13,paper);
        line("金 "+c.gold+"    粮 "+c.food,14,paper);line("兵 "+c.troops+"    城防 "+c.defense,14,paper);
        line("治安 "+c.order+"    气力 "+c.morale,13,paper);
        if(c.owner==world.player&&!world.gameOver()) {
            action("出征",v->chooseOfficer(c,o->chooseWeapon(weapon->new AlertDialog.Builder(this).setTitle("出征兵力").setItems(new String[]{"3000人","5000人","8000人"},(d,which)->{
                World.Result result=world.deploy(c.id,o.id,weapon,new int[]{3000,5000,8000}[which]);
                if(result.ok){World.Unit u=world.unit(o.unitId);selected=u.hex;moving=u.id;}
                apply(result);
            }).show())));
            action("征兵  +2000 · 金300",v->chooseOfficer(c,o->apply(world.recruit(c.id,o.id))));
            action("训练  气力+15 · 金100",v->chooseOfficer(c,o->apply(world.train(c.id,o.id))));
            action("巡察  治安+10 · 金100",v->chooseOfficer(c,o->apply(world.patrol(c.id,o.id))));
            action("生产兵装  +2000 · 金400",v->chooseOfficer(c,o->chooseWeapon(weapon->apply(world.produce(c.id,o.id,weapon)))));
            line("可用武将 "+world.idle(c).size()+"  /  每次命令10行动力",12,paper);
        }
        StringBuilder stocks=new StringBuilder("兵装库存\n");for(World.Weapon w:World.Weapon.values())stocks.append(w.label).append(" ").append(c.equipment[w.ordinal()]).append("  ");
        line(stocks.toString(),12,paper);
    }
    private void showUnit(World.Unit u){
        World.Officer o=world.officer(u.officerId);line(o.name,26,gold);line(world.faction(u.owner)+" · "+u.weapon.label,14,paper);
        line("兵力 "+u.troops+"\n携粮 "+u.food+"\n气力 "+u.energy,16,paper);
        line("统率 "+o.leadership+"  武力 "+o.war,13,paper);
        line("移动 "+u.weapon.movement+"  射程 "+u.weapon.range,13,paper);
        if(u.owner==world.player){line(u.acted?"本旬已行动":"点击高亮空地移动\n点敌军攻击 / 点城池攻城或入城",14,paper);action("取消部队选择",v->{moving=-1;selected=null;refresh();});}
    }
    private interface OfficerChoice {void choose(World.Officer officer);}
    private interface WeaponChoice {void choose(World.Weapon weapon);}
    private void chooseOfficer(World.City c,OfficerChoice callback){
        List<World.Officer> options=world.idle(c);if(options.isEmpty()){Toast.makeText(this,"没有本旬可行动的在城武将",Toast.LENGTH_SHORT).show();return;}
        String[] names=new String[options.size()];for(int i=0;i<names.length;i++)names[i]=options.get(i).name;
        new AlertDialog.Builder(this).setTitle("执行武将").setItems(names,(d,index)->callback.choose(options.get(index))).show();
    }
    private void chooseWeapon(WeaponChoice callback){
        String[] labels=new String[World.Weapon.values().length];for(int i=0;i<labels.length;i++)labels[i]=World.Weapon.values()[i].label;
        new AlertDialog.Builder(this).setTitle("选择兵种").setItems(labels,(d,index)->callback.choose(World.Weapon.values()[index])).show();
    }
    private void menu(){
        if(aiRunning)return;
        new AlertDialog.Builder(this).setTitle("三国 · 研制版").setItems(new String[]{"保存局面（3个槽位）","读取存档","城池一览 / 定位","武将一览","战报","回到全图","新游戏 / 选择势力","版本与范围"},(d,index)->{
            switch(index){
                case 0:saveSlots(false);break;
                case 1:saveSlots(true);break;
                case 2:cityList();break;
                case 3:officerList();break;
                case 4:new AlertDialog.Builder(this).setTitle("战报").setMessage(joinLog()).setPositiveButton("返回",null).show();break;
                case 5:map.fit();break;
                case 6:scenarioPicker();break;
                case 7:new AlertDialog.Builder(this).setTitle("0.2.0 · M1开发中").setMessage("独立Android策略游戏。\n\n基础演练：3城6将，2势力。\n区域争雄：9城18将，3势力，可任选一方。\n\n两张地图均为原创沙盘，布局、归属、数值和规则尚未与原版对齐。全国地图、全武将、战法、外交和3D表现仍在开发。\n\n当前数据："+world.scenarioId+" r"+world.dataRevision).setPositiveButton("知道了",null).show();break;
            }
        }).show();
    }
    private void scenarioPicker(){
        try {
            List<World> scenarios=ScenarioCatalog.all();String[] labels=new String[scenarios.size()];
            for(int i=0;i<labels.length;i++){World w=scenarios.get(i);labels[i]=w.scenarioName+" · "+w.cities.size()+"城 / "+w.officers.size()+"将 / "+w.factions.length+"势力";}
            new AlertDialog.Builder(this).setTitle("选择剧本 · 原创测试沙盘").setItems(labels,(dialog,index)->{
                World template=scenarios.get(index);
                new AlertDialog.Builder(this).setTitle(template.scenarioName+" · 选择势力").setItems(template.factions,(d,side)->{
                    confirm("以"+template.faction(side)+"开始新局？当前自动存档将更新，手动存档保留。",()->startScenario(template.scenarioId,side));
                }).setNegativeButton("返回",(d,n)->scenarioPicker()).show();
            }).setNegativeButton("取消",null).show();
        }catch(IOException e){showError("剧本读取失败",e);}
    }
    private void startScenario(String id,int player){
        try{World fresh=ScenarioCatalog.load(id,player);world=fresh;moving=-1;selected=world.home().hex;refresh();map.focus(selected);save("auto",false);}
        catch(IOException e){showError("无法开始剧本",e);}
    }
    private void cityList(){
        List<World.City> options=new ArrayList<>(world.cities);
        options.sort((a,b)->{int friendly=Boolean.compare(b.owner==world.player,a.owner==world.player);return friendly!=0?friendly:Integer.compare(a.id,b.id);});
        String[] labels=new String[options.size()];
        for(int i=0;i<labels.length;i++){World.City c=options.get(i);labels[i]=c.name+" · "+world.faction(c.owner)+" · 兵"+c.troops;}
        new AlertDialog.Builder(this).setTitle("城池一览 · 点击定位").setItems(labels,(d,index)->{moving=-1;selected=options.get(index).hex;refresh();map.focus(selected);}).setNegativeButton("返回",null).show();
    }
    private String slotName(int index){return index==0?"manual":"manual"+(index+1);}
    private void saveSlots(boolean loading){
        String[] labels=new String[3];
        for(int i=0;i<labels.length;i++) {
            AtomicFile entry=file(slotName(i));File f=entry.getBaseFile();
            boolean present=f.exists()||new File(f+".bak").exists();
            labels[i]="槽位 "+(i+1)+" · 空";
            if(present)try{World saved=readSave(entry);labels[i]="槽位 "+(i+1)+" · "+saved.scenarioName+" · "+saved.faction(saved.player)+" · "+saved.date();}
            catch(IOException e){labels[i]="槽位 "+(i+1)+" · 文件损坏 / 不兼容";}
        }
        new AlertDialog.Builder(this).setTitle(loading?"读取存档":"保存局面").setItems(labels,(d,index)->{
            String slot=slotName(index);
            confirm(loading?"读取槽位 "+(index+1)+"，替换当前局面？":"保存到槽位 "+(index+1)+"？已有内容将被覆盖。",()->{if(loading)loadSlot(slot);else save(slot,true);});
        }).setNegativeButton("取消",null).show();
    }
    private void showError(String title,IOException e){new AlertDialog.Builder(this).setTitle(title).setMessage(e.getMessage()+"。当前局面未改变。").setPositiveButton("返回",null).show();}
    /** Mutate a detached snapshot off the UI thread; the live autosave stays consistent. */
    private void advanceTurn(){
        if(aiRunning||world.gameOver())return;
        final World next;
        try{next=SaveCodec.decode(SaveCodec.encode(world));}catch(IOException e){showError("无法结束本旬",e);return;}
        aiRunning=true;nextTurn.setText("电脑行动中…");refresh();
        new Thread(()->{
            try {
                World.Result result=next.nextTurn();SaveCodec.validate(next);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    aiRunning=false;nextTurn.setText("下一旬  →");world=next;moving=-1;apply(result);
                });
            }catch(Exception e){runOnUiThread(()->{if(isFinishing()||isDestroyed())return;aiRunning=false;nextTurn.setText("下一旬  →");refresh();showError("回合结算失败",new IOException(e));});}
        },"strategy-turn").start();
    }
    private String joinLog(){StringBuilder b=new StringBuilder();for(String s:world.log)b.append(s).append('\n');return b.toString();}
    private void officerList(){
        StringBuilder b=new StringBuilder("名称  统/武/智/政/魅\n（均为工程测试数值）\n\n");
        for(World.Officer o:world.officers){b.append(o.name).append("  ").append(o.leadership).append('/').append(o.war).append('/').append(o.intelligence).append('/').append(o.politics).append('/').append(o.charm).append('\n');
            b.append(world.faction(o.owner)).append(" · ").append(o.unitId>=0?"出征中":o.cityId>=0?world.city(o.cityId).name:"已退出战场").append(o.acted?" · 本旬已行动":"").append("\n\n");}
        new AlertDialog.Builder(this).setTitle("武将一览").setMessage(b.toString()).setPositiveButton("返回",null).show();
    }
    private AtomicFile file(String slot){return new AtomicFile(new File(getFilesDir(),slot+".sg11"));}
    private void save(String slot,boolean announce){
        AtomicFile file=file(slot);FileOutputStream out=null;
        try{byte[] bytes=SaveCodec.encode(world);out=file.startWrite();out.write(bytes);file.finishWrite(out);if(announce)Toast.makeText(this,"局面已保存",Toast.LENGTH_SHORT).show();}
        catch(IOException e){if(out!=null)file.failWrite(out);Toast.makeText(this,"保存失败："+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private World readSave(AtomicFile file)throws IOException {
        try(FileInputStream in=file.openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int count;
            while((count=in.read(buffer))!=-1){if(out.size()+count>4*1024*1024+20)throw new IOException("存档超过大小限制");out.write(buffer,0,count);}
            return SaveCodec.decode(out.toByteArray());
        }
    }
    private void loadSlot(String slot){
        try{World restored=readSave(file(slot));world=restored;moving=-1;selected=world.home().hex;refresh();map.focus(selected);save("auto",false);}
        catch(IOException e){new AlertDialog.Builder(this).setTitle("读取失败").setMessage(e.getMessage()+"。当前局面未改变。").setPositiveButton("返回",null).show();}
    }
    private void restart(){scenarioPicker();}
    @Override protected void onPause(){super.onPause();if(world!=null)save("auto",false);}
}
