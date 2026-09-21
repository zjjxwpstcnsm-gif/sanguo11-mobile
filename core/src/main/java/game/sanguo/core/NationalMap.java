package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Single immutable source-grid resource shared by all eras. Scenario ownership,
 * officers, diplomacy and resources are never copied from another era. */
public final class NationalMap {
    public static final String RESOURCE="national-map-v056", ID="san11-national", LAYOUT="native-200";
    public static final int REVISION=61, COLUMNS=200, ROWS=200;
    private static Properties cached;
    private NationalMap(){}
    private static synchronized Properties data()throws IOException {
        if(cached!=null)return cached;
        Properties p=new Properties();
        try(InputStream in=NationalMap.class.getResourceAsStream("/maps/"+RESOURCE+".properties")){
            if(in==null)throw new IOException("全国地图资源缺失");p.load(new InputStreamReader(in,StandardCharsets.UTF_8));
        }
        if(!ID.equals(p.getProperty("mapId"))||!LAYOUT.equals(p.getProperty("layout"))||!Integer.toString(REVISION).equals(p.getProperty("revision"))||!"200".equals(p.getProperty("columns"))||!"200".equals(p.getProperty("rows")))throw new IOException("全国地图身份错误");
        for(int y=0;y<ROWS;y++){String row=p.getProperty("terrain."+y);if(row==null||row.length()!=COLUMNS)throw new IOException("全国地图行宽错误");for(int x=0;x<COLUMNS;x++)try{TerrainCode.decode(row.charAt(x));}catch(IllegalArgumentException e){throw new IOException("全国地图非法地形 "+x+","+y,e);}}
        cached=p;return p;
    }
    static final class Selection {
        final int x,y,width,height;
        Selection(int x,int y,int width,int height){this.x=x;this.y=y;this.width=width;this.height=height;}
        void apply(World w){w.mapId=ID;w.mapLayout=LAYOUT;w.mapRevision=REVISION;w.sourceOriginX=x;w.sourceOriginY=y;}
    }
    static Selection attach(Properties scenario)throws IOException {
        String ref=(String)scenario.remove("map");if(ref==null)return null;
        if(!RESOURCE.equals(ref))throw new IOException("不支持的全国地图版本，请重新开局");
        String crop=(String)scenario.remove("map-crop");String[] f=(crop==null?"0,0,200,200":crop).split(",");
        if(f.length!=4)throw new IOException("全国地图裁区字段错误");
        int x=Integer.parseInt(f[0]),y=Integer.parseInt(f[1]),width=Integer.parseInt(f[2]),height=Integer.parseInt(f[3]);
        if(x<0||y<0||width<1||height<1||x+width>COLUMNS||y+height>ROWS||(x&1)!=0)throw new IOException("全国地图裁区越界或奇偶错位");
        for(String key:scenario.stringPropertyNames())if(key.startsWith("terrain.")||key.startsWith("development-plot")||key.equals("coordinates")||key.equals("width")||key.equals("height"))throw new IOException("引用全国地图的剧本禁止内嵌旧地形："+key);
        Properties map=data();scenario.setProperty("coordinates","odd-q");scenario.setProperty("width",""+width);scenario.setProperty("height",""+height);
        for(int r=0;r<height;r++)scenario.setProperty("terrain."+r,map.getProperty("terrain."+(y+r)).substring(x,x+width));
        int plot=0;
        for(String key:new TreeSet<>(scenario.stringPropertyNames()))if(key.matches("city\\.\\d+")){
            String[] city=scenario.getProperty(key).split("\\|",-1);String[] pos=required(map,"site."+city[0]).split(",");
            int sx=Integer.parseInt(pos[0])-x,sy=Integer.parseInt(pos[1])-y;
            if(sx<0||sy<0||sx>=width||sy>=height)throw new IOException("剧本据点不在全国裁区："+city[1]);
            city[2]=""+sx;city[3]=""+sy;scenario.remove(key);scenario.setProperty(key,String.join("|",city));
            String plots=map.getProperty("plots."+city[0],"");
            if(!plots.isEmpty())for(String item:plots.split(";")){
                String[] xy=item.split(",");int px=Integer.parseInt(xy[0])-x,py=Integer.parseInt(xy[1])-y;
                if(px<0||py<0||px>=width||py>=height)throw new IOException("开发区不在全国裁区："+city[1]);
                scenario.setProperty("development-plot."+plot++,city[0]+"|"+px+"|"+py);
            }
        }
        scenario.setProperty("development-plots",""+plot);
        return new Selection(x,y,width,height);
    }
    private static String required(Properties p,String key)throws IOException{String v=p.getProperty(key);if(v==null)throw new IOException("全国据点记录缺失："+key);return v;}
    /** Legacy saves keep their own terrain. Never transplant a revised map under armies. */
    public static String compatibilityNotice(World w){
        return ID.equals(w.mapId)&&(w.mapRevision==56||w.mapRevision==57||w.mapRevision==58||w.mapRevision==59||w.mapRevision==60)?
            "旧地图修订"+w.mapRevision+"：保留存档原地形、城市与部队，不自动迁移。本次两角地形与外景收边仅对新开局生效，旧档不套用新版外景边界；旧档地形和行军预算保持不变。请保留手动存档后重新开局体验修订"+REVISION+"。":"";
    }
    public static void validateIdentity(World w)throws IOException {
        if(!ID.equals(w.mapId))return;
        if(!LAYOUT.equals(w.mapLayout)||(w.mapRevision!=REVISION&&w.mapRevision!=60&&w.mapRevision!=59&&w.mapRevision!=58&&w.mapRevision!=57&&w.mapRevision!=56)||!w.columnStaggered||w.sourceMapWidth<1||w.sourceMapHeight<1||w.sourceOriginX<0||w.sourceOriginY<0||w.sourceOriginX+w.sourceMapWidth>COLUMNS||w.sourceOriginY+w.sourceMapHeight>ROWS)throw new IOException("旧版本地图存档无法继续使用，请保留原档并重新开局");
    }
}
