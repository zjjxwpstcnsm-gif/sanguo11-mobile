package game.sanguo.mobile;

import android.app.*;
import android.view.*;
import android.widget.*;
import android.util.AtomicFile;
import game.sanguo.core.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Edits a separately loaded map, never the campaign owned by MainActivity. */
final class MapEditorUi {
    private final MainActivity a;
    private World world;
    private MapView map;
    private TextView status;
    private Dialog dialog;
    private World.Terrain brush=World.Terrain.PLAIN;
    private boolean drawing;
    private final SortedMap<String,World.Terrain> changes=new TreeMap<>();
    private String id=UUID.randomUUID().toString();
    MapEditorUi(MainActivity a){this.a=a;}
    void show(){
        new Thread(()->{
            try{world=ScenarioCatalog.load("heroes-250",0);restore();
                a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())build();});
            }catch(Exception e){a.runOnUiThread(()->error("草稿读取失败",e));}
        },"map-editor-load").start();
    }
    private void build(){
        dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff14252d);
        status=new TextView(a);UiTheme.text(status);status.setText("地图编辑器 · 独立草稿 · 浏览模式");status.setPadding(a.dp(8),a.dp(8),a.dp(8),a.dp(8));root.addView(status);
        map=new MapView(a,this::tap);map.setWorld(world,null,-1);root.addView(map,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout tools=new LinearLayout(a);root.addView(tools);
        Button mode=button("浏览",()->{});mode.setOnClickListener(v->{drawing=!drawing;mode.setText(drawing?"绘制":"浏览");status.setText(drawing?"点击单格绘制；拖动仍为平移":"浏览模式 · 不会修改地形");});tools.addView(mode,new LinearLayout.LayoutParams(0,a.dp(48),1));
        tools.addView(button("地形",this::terrain),new LinearLayout.LayoutParams(0,a.dp(48),1));
        tools.addView(button("全图",()->map.fit()),new LinearLayout.LayoutParams(0,a.dp(48),1));
        tools.addView(button("保存草稿",this::save),new LinearLayout.LayoutParams(0,a.dp(48),1));
        dialog.setContentView(root);dialog.setOnCancelListener(d->save());dialog.show();
        dialog.getWindow().setLayout(-1,-1);map.post(map::fit);
    }
    private Button button(String text,Runnable run){Button b=CompactButtons.create(a);b.setText(text);b.setTextSize(11);b.setOnClickListener(v->run.run());return b;}
    private void terrain(){World.Terrain[] all=World.Terrain.values();String[] names=new String[all.length];for(int i=0;i<all.length;i++)names[i]=TerrainPresentation.of(all[i]).name();
        new AlertDialog.Builder(a).setTitle("正式游戏地形").setItems(names,(d,n)->{brush=all[n];status.setText("画笔 · "+names[n]);}).show();}
    private void tap(Hex h){
        if(!world.inside(h))return;
        if(drawing){
            if(brush==World.Terrain.VOID||world.cityAt(h)!=null||world.domestic.at(h)!=null||world.war.at(h)!=null){status.setText("关键区域受保护，不能刷除据点、设施或有效地图边界");return;}
            world.terrain[h.q][h.r]=brush;changes.put(h.q+","+h.r,brush);world.terrainRevision++;
        }
        status.setText(MapCoordinates.display(world,h)+" · "+TerrainPresentation.of(world.terrain[h.q][h.r]).name()+" · "+(drawing?"绘制":"浏览"));map.setWorld(world,h,-1);
    }
    private AtomicFile draft(){return new AtomicFile(new File(a.getFilesDir(),"map-editor-draft-v1.json"));}
    private void save(){
        try{
            JSONObject document=new JSONObject().put("format",1).put("base",NationalMap.SHA256).put("id",id);JSONArray cells=new JSONArray();
            for(Map.Entry<String,World.Terrain> e:changes.entrySet())cells.put(new JSONObject().put("hex",e.getKey()).put("terrain",e.getValue().name()));document.put("cells",cells);
            byte[] bytes=document.toString().getBytes(StandardCharsets.UTF_8);AtomicFile file=draft();FileOutputStream out=null;
            try{out=file.startWrite();out.write(bytes);file.finishWrite(out);}catch(IOException e){if(out!=null)file.failWrite(out);throw e;}
            if(status!=null)status.setText("草稿已保存 · "+changes.size()+"格 · 重启后可恢复；当前战局未改变");
        }catch(Exception e){error("保存失败",e);}
    }
    private void restore()throws Exception{
        AtomicFile file=draft();if(!file.getBaseFile().exists()&&!new File(file.getBaseFile()+".bak").exists())return;
        byte[] bytes=file.readFully();if(bytes.length>4*1024*1024)throw new IOException("草稿过大");
        JSONObject doc=new JSONObject(new String(bytes,StandardCharsets.UTF_8));if(doc.getInt("format")!=1||!NationalMap.SHA256.equals(doc.getString("base")))throw new IOException("草稿地图基线不匹配，原文件已保留");
        SortedMap<String,World.Terrain> pending=new TreeMap<>();JSONArray cells=doc.getJSONArray("cells");
        for(int i=0;i<cells.length();i++){JSONObject c=cells.getJSONObject(i);String key=c.getString("hex");String[] qr=key.split(",");if(qr.length!=2)throw new IOException("草稿坐标错误");Hex h=new Hex(Integer.parseInt(qr[0]),Integer.parseInt(qr[1]));World.Terrain t=World.Terrain.valueOf(c.getString("terrain"));
            if(!world.inside(h)||t==World.Terrain.VOID||world.cityAt(h)!=null||world.domestic.at(h)!=null||world.war.at(h)!=null||pending.put(key,t)!=null)throw new IOException("草稿修改冲突："+key);
        }
        id=doc.getString("id");changes.putAll(pending);
        for(Map.Entry<String,World.Terrain> e:changes.entrySet()){String[] qr=e.getKey().split(",");world.terrain[Integer.parseInt(qr[0])][Integer.parseInt(qr[1])]=e.getValue();}world.terrainRevision++;
    }
    private void error(String title,Exception e){if(!a.isFinishing()&&!a.isDestroyed())new AlertDialog.Builder(a).setTitle(title).setMessage(e.getMessage()).setPositiveButton("返回",null).show();}
}
