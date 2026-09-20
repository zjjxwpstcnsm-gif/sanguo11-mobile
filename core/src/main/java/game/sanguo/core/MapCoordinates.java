package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Explicit odd-row offset to axial conversion. It never invents terrain or claims source fidelity. */
public final class MapCoordinates {
    private MapCoordinates(){}
    /** Odd-row source to axial; algebra intentionally also supports off-map neighbors. */
    public static Hex toAxial(SourceGridCoord s,int rows){
        Objects.requireNonNull(s,"source");checkRows(rows);
        return new Hex(s.x-(s.y-(s.y&1))/2+(rows-1)/2,s.y);
    }
    public static SourceGridCoord toSource(Hex h,int rows){
        Objects.requireNonNull(h,"axial");checkRows(rows);
        return new SourceGridCoord(h.q+(h.r-(h.r&1))/2-(rows-1)/2,h.r);
    }
    private static void checkRows(int rows){
        if(rows<1||rows>200)throw new IllegalArgumentException("源地图行数必须在1..200之间");
    }
    /** Compatibility for existing callers; new source-grid code uses SourceGridCoord. */
    public static Hex axial(int x,int y,int rows){return toAxial(new SourceGridCoord(x,y),rows);}
    public static Hex source(Hex h,int rows){SourceGridCoord s=toSource(h,rows);return new Hex(s.x,s.y);}
    static int normalize(Properties p)throws IOException {
        String layout=(String)p.remove("coordinates");if(layout==null||layout.equals("axial"))return 0;
        if(!layout.equals("odd-r"))throw new IOException("不支持的格子坐标系");
        int width=Integer.parseInt(p.getProperty("width")),height=Integer.parseInt(p.getProperty("height"));
        if(width<1||width>200||height<1||height>200)throw new IOException("错行原始地图最大200×200格");
        int convertedWidth=width+(height-1)/2;
        for(int y=0;y<height;y++){
            String key="terrain."+y,row=p.getProperty(key);if(row==null||row.length()!=width)throw new IOException("错行地形行宽错误");
            char[] converted=new char[convertedWidth];Arrays.fill(converted,'M');for(int x=0;x<width;x++)converted[axial(x,y,height).q]=row.charAt(x);
            p.remove(key);p.setProperty(key,new String(converted));
        }
        for(String key:new ArrayList<>(p.stringPropertyNames())){
            int xIndex=key.matches("development-plot\\.\\d+")?1:key.matches("city\\.\\d+")?2:key.matches("initial-unit\\.\\d+")?6:key.matches("initial-camp\\.\\d+")?2:-1;
            if(xIndex<0)continue;
            String[] f=p.getProperty(key).split("\\|",-1);if(f.length<=xIndex+1)throw new IOException("坐标对象列数错误");
            int x=Integer.parseInt(f[xIndex].trim()),y=Integer.parseInt(f[xIndex+1].trim());if(x<0||x>=width||y<0||y>=height)throw new IOException("原始坐标越界："+key);
            Hex h=toAxial(new SourceGridCoord(x,y).requireWithin(width,height),height);f[xIndex]=Integer.toString(h.q);f[xIndex+1]=Integer.toString(h.r);p.remove(key);p.setProperty(key,String.join("|",f));
        }
        p.remove("width");p.setProperty("width",Integer.toString(convertedWidth));return width;
    }
}
