package game.sanguo.mobile;

import android.app.*;
import game.sanguo.core.*;
import java.util.*;

/** The legend uses the same renderers as the map and pickers, so it cannot drift into unrelated artwork. */
final class VisualGuide {
    static void show(Activity a,World w){
        new AlertDialog.Builder(a).setTitle("兵种与建筑图例").setItems(new String[]{"陆军与水军","城池、关隘与港口","内政设施","军事设施与陷阱","地图状态说明","地形与通行"},(d,i)->{
            if(i==5){terrain(a);return;}
            List<Object> items=new ArrayList<>();
            if(i==0){items.addAll(Arrays.asList(World.Weapon.values()));items.addAll(Arrays.asList(Army.Ship.values()));}
            if(i==1)items.addAll(Arrays.asList(World.SiteKind.values()));
            if(i==2)items.addAll(Arrays.asList(Domestic.Kind.values()));
            if(i==3)items.addAll(Arrays.asList(War.StructureKind.values()));
            if(i==4){new AlertDialog.Builder(a).setTitle("地图状态说明").setMessage("颜色表示势力，金色格框表示选中。\n兵力条：当前兵力占主将统兵上限；城防条：当前耐久占该据点真实上限。\n✓：已行动；粮!：携粮低于约3旬基础消耗；援：盟军援兵；火：燃烧；乱/伪：混乱或伪报。\n脚手架：尚未完成施工。缩小地图时用兵装轮廓辨认部队，放大显示完整模型。\n头像为原创插画；未单独绘制的人物使用稳定默认头像。").setPositiveButton("返回",null).show();return;}
            new AlertDialog.Builder(a).setTitle("图标 · 点击查看").setAdapter(GameIcon.adapter(a,w,items,VisualGuide::label),(dialog,n)->{
                Object item=items.get(n);String detail=item instanceof Domestic.Kind?Domestic.buildEffect((Domestic.Kind)item):item instanceof War.StructureKind?((War.StructureKind)item).effect:
                    item instanceof World.Weapon?"基础移动 "+MarchScale.base(w,((World.Weapon)item).movement)+" · 基础射程 "+((World.Weapon)item).range+"\n实际数值受地形、编队、特技和技巧影响。":item instanceof Army.Ship?"水上使用舰船，上岸恢复陆战兵装。":"据点模型按城市、关隘、港口分别绘制。";
                new AlertDialog.Builder(a).setTitle(label(item)).setMessage(detail).setPositiveButton("返回",null).show();
            }).setNegativeButton("返回",null).show();
        }).setNegativeButton("返回",null).show();
    }
    private static void terrain(Activity a){
        android.widget.LinearLayout rows=new android.widget.LinearLayout(a);rows.setOrientation(android.widget.LinearLayout.VERTICAL);
        TerrainTiles tiles=new TerrainTiles();int size=Math.round(64*a.getResources().getDisplayMetrics().density);
        for(World.Terrain t:World.Terrain.values()){
            String label=TerrainPresentation.of(t).legend();
            World sample=new World(3,3);sample.terrain[1][1]=t;NaturalStructures.seedOpening(sample);
            MapModels models=new MapModels();
            android.widget.LinearLayout row=new android.widget.LinearLayout(a);row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            android.view.View preview=new android.view.View(a){protected void onDraw(android.graphics.Canvas canvas){canvas.save();canvas.translate(getWidth()/2f,getHeight()/2f);canvas.scale(size/56f,size/56f);tiles.draw(canvas,sample,1,1,0,0);if(t==World.Terrain.DAM)models.structure(canvas,War.StructureKind.DAM,FactionColors.color(sample,-1));canvas.restore();}};
            preview.setContentDescription(label);row.addView(preview,new android.widget.LinearLayout.LayoutParams(size,size));
            android.widget.TextView text=new android.widget.TextView(a);text.setText(label);text.setTextSize(14);row.addView(text,new android.widget.LinearLayout.LayoutParams(0,-2,1));rows.addView(row);
        }
        android.widget.ScrollView scroll=new android.widget.ScrollView(a);scroll.addView(rows);
        new AlertDialog.Builder(a).setTitle("地形与通行").setView(scroll).setPositiveButton("返回",null).show();
    }
    private static String label(Object item){
        if(item instanceof World.Weapon)return ((World.Weapon)item).label;if(item instanceof Army.Ship)return ((Army.Ship)item).label;
        if(item instanceof Domestic.Kind)return ((Domestic.Kind)item).label;if(item instanceof War.StructureKind)return ((War.StructureKind)item).label;
        return item==World.SiteKind.CITY?"城市":item==World.SiteKind.GATE?"关隘":"港口";
    }
}
