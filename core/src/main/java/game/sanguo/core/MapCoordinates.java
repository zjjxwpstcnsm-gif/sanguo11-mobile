package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Source grids are NOT axial storage. Native odd-q is transposed into the existing
 * six-direction axial frame, retaining every reference cell and avoiding negative indexes. */
public final class MapCoordinates {
    private MapCoordinates(){}
    /** Legacy odd-r source API; native map conversion must use the World overload. */
    public static Hex toAxial(SourceGridCoord s,int rows){
        Objects.requireNonNull(s,"source");checkRows(rows);return axial(s.x,s.y,rows);
    }
    public static SourceGridCoord toSource(Hex h,int rows){
        Objects.requireNonNull(h,"axial");checkRows(rows);Hex s=source(h,rows);return new SourceGridCoord(s.q,s.r);
    }
    private static void checkRows(int rows){if(rows<1||rows>200)throw new IllegalArgumentException("源地图行数必须在1..200之间");}

    public static Hex axial(int x,int y,int rows){return new Hex(x-Math.floorDiv(y,2)+(rows-1)/2,y);}
    public static Hex source(Hex h,int rows){return new Hex(h.q+Math.floorDiv(h.r,2)-(rows-1)/2,h.r);}
    public static Hex axial(SourceGridCoord source,int rows){return axial(source.x,source.y,rows);}
    public static SourceGridCoord sourceCoord(Hex axial,int rows){Hex h=source(axial,rows);return new SourceGridCoord(h.q,h.r);}
    public static boolean contains(Hex h,int columns,int rows){return sourceCoord(h,rows).isInside(columns,rows);}
    public static Hex axialColumn(int x,int y,int columns){return axial(y,x,columns);}
    public static SourceGridCoord sourceColumn(Hex h,int columns){Hex s=source(h,columns);return new SourceGridCoord(s.r,s.q);}
    public static Hex axial(World w,SourceGridCoord s){return w.columnStaggered?axialColumn(s.x,s.y,w.sourceMapWidth):w.sourceMapWidth>0?axial(s.x,s.y,w.height):new Hex(s.x,s.y);}
    public static SourceGridCoord source(World w,Hex h){return w.columnStaggered?sourceColumn(h,w.sourceMapWidth):w.sourceMapWidth>0?sourceCoord(h,w.height):new SourceGridCoord(h.q,h.r);}
    public static SourceGridCoord nationalSource(World w,Hex h){SourceGridCoord s=source(w,h);return new SourceGridCoord(s.x+w.sourceOriginX,s.y+w.sourceOriginY);}
    public static Hex fromNationalSource(World w,SourceGridCoord s){return axial(w,new SourceGridCoord(s.x-w.sourceOriginX,s.y-w.sourceOriginY));}
    /** A single user-facing coordinate convention for a point in national/cropped maps. */
    public static String display(World w,Hex h){
        if(h==null)return "未知坐标";
        return (NationalMap.ID.equals(w.mapId)?"全国源坐标 ":"地图坐标 ")+nationalSource(w,h);
    }
    static int normalize(Properties p)throws IOException {
        String layout=(String)p.remove("coordinates");if(layout==null||layout.equals("axial"))return 0;
        boolean columns=layout.equals("odd-q");if(!columns&&!layout.equals("odd-r"))throw new IOException("不支持的格子坐标系");
        int width=Integer.parseInt(p.getProperty("width")),height=Integer.parseInt(p.getProperty("height"));
        if(width<1||width>200||height<1||height>200)throw new IOException("原始地图最大200×200格");
        int axialRows=columns?width:height,axialWidth=(columns?height:width)+(axialRows-1)/2;
        char[][] terrain=new char[axialRows][axialWidth];for(char[] row:terrain)Arrays.fill(row,'V');
        for(int y=0;y<height;y++){
            String row=(String)p.remove("terrain."+y);if(row==null||row.length()!=width)throw new IOException("地形行宽错误");
            for(int x=0;x<width;x++){Hex h=columns?axialColumn(x,y,width):axial(x,y,height);terrain[h.r][h.q]=row.charAt(x);}
        }
        for(int r=0;r<axialRows;r++)p.setProperty("terrain."+r,new String(terrain[r]));
        for(String key:new ArrayList<>(p.stringPropertyNames())){
            int index=key.matches("development-plot\\.\\d+")?1:key.matches("city\\.\\d+")?2:key.matches("initial-unit\\.\\d+")?6:key.matches("initial-camp\\.\\d+")?2:-1;
            if(index<0)continue;
            String[] f=p.getProperty(key).split("\\|",-1);if(f.length<=index+1)throw new IOException("坐标对象列数错误");
            int x=Integer.parseInt(f[index].trim()),y=Integer.parseInt(f[index+1].trim());if(x<0||x>=width||y<0||y>=height)throw new IOException("原始坐标越界："+key);
            Hex h=columns?axialColumn(x,y,width):axial(x,y,height);f[index]=Integer.toString(h.q);f[index+1]=Integer.toString(h.r);p.remove(key);p.setProperty(key,String.join("|",f));
        }
        p.remove("width");p.remove("height");p.setProperty("width",Integer.toString(axialWidth));p.setProperty("height",Integer.toString(axialRows));return width;
    }
}
