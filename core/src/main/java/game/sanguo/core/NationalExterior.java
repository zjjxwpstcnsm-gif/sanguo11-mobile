package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Reviewed cartographic exterior ONLY. Never changes World.inside/sourceInside, terrain,
 * movement, territory or save state. Unknown VOID and axial padding have no fallback.
 * The explicit source mask is bound to known revisions 61 and 63; older saves keep their own map. */
public final class NationalExterior {
    public static final int REVISION=61;
    public static final String MAP_SHA256="d5d5659e40a23619dd8515713d21997715733d20cc937a964662591e22f076c4";
    public enum Surface {
        SEA(World.Terrain.NON_NAVIGABLE_WATER), ARID(World.Terrain.SAND), ROCK(World.Terrain.MOUNTAIN);
        public final World.Terrain appearance;
        Surface(World.Terrain appearance){this.appearance=appearance;}
    }
    private NationalExterior(){}
    private static final class Data {
        static final Surface[] CELLS=read();
        static Surface[] read(){
            String path="/maps/national-exterior-v061.properties";
            try(InputStream in=NationalExterior.class.getResourceAsStream(path)){
                if(in==null)throw new IOException("Missing reviewed exterior resource");
                Map<String,String> p=new HashMap<>();
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
                    for(String line;(line=reader.readLine())!=null;){
                        line=line.trim();if(line.isEmpty()||line.startsWith("#"))continue;
                        int at=line.indexOf('=');if(at<1)throw new IOException("Invalid exterior line");
                        String key=line.substring(0,at),value=line.substring(at+1);
                        if(p.put(key,value)!=null)throw new IOException("Duplicate exterior key: "+key);
                    }
                }
                if(!NationalMap.ID.equals(p.remove("mapId"))||!NationalMap.LAYOUT.equals(p.remove("layout"))||!"61".equals(p.remove("revision"))||!MAP_SHA256.equals(p.remove("mapSha256")))throw new IOException("Exterior/map identity mismatch");
                int count=Integer.parseInt(p.remove("count"));
                Surface[] cells=new Surface[NationalMap.COLUMNS*NationalMap.ROWS];int actual=0;
                for(Map.Entry<String,String> e:p.entrySet()){
                    String[] k=e.getKey().split("\\.");if(k.length!=3||!k[0].equals("outside"))throw new IOException("Unknown exterior field");
                    int x=Integer.parseInt(k[1]),y=Integer.parseInt(k[2]);
                    if(x<0||y<0||x>=200||y>=200)throw new IOException("Exterior source coordinate out of range");
                    int index=y*200+x;if(cells[index]!=null)throw new IOException("Duplicate exterior coordinate");
                    cells[index]=Surface.valueOf(e.getValue());actual++;
                }
                if(actual!=count||count!=1051)throw new IOException("Incomplete reviewed exterior resource");
                return cells;
            }catch(IOException|RuntimeException e){throw new ExceptionInInitializerError(e);}
        }
    }
    /** Metadata query in national source coordinates, not an alternative validity function. */
    public static Surface sourceSurface(int x,int y){return x<0||y<0||x>=200||y>=200?null:Data.CELLS[y*200+x];}
    public static Surface surface(World w,Hex h){
        if(w==null||h==null||(w.mapRevision!=REVISION&&w.mapRevision!=63)||!NationalMap.ID.equals(w.mapId)||!NationalMap.LAYOUT.equals(w.mapLayout)||!w.columnStaggered||!w.sourceInside(h)||w.terrain[h.q][h.r]!=World.Terrain.VOID)return null;
        SourceGridCoord s=MapCoordinates.nationalSource(w,h);
        return sourceSurface(s.x,s.y);
    }
    /** Rendering only. Consumers of movement, selection, ownership or development MUST use World. */
    public static World.Terrain appearance(World w,Hex h){
        if(w==null||!w.sourceInside(h))return World.Terrain.VOID;
        World.Terrain real=w.terrain[h.q][h.r];if(real!=World.Terrain.VOID)return real;
        Surface s=surface(w,h);return s==null?World.Terrain.VOID:s.appearance;
    }
}
