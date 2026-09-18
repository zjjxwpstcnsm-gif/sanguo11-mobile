package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.Arrays;

public final class TerrainConnectionsTest {
    static int checks;
    static void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
    public static void main(String[] args){
        World w=new World(9,9);Hex center=new Hex(4,4);
        for(int mask=0;mask<64;mask++){
            for(World.Terrain[] row:w.terrain)Arrays.fill(row,World.Terrain.MOUNTAIN);
            w.terrain[4][4]=World.Terrain.PLANK_ROAD;
            for(int d=0;d<6;d++)if((mask&(1<<d))!=0){Hex h=center.neighbors().get(d);w.terrain[h.q][h.r]=d%2==0?World.Terrain.PLANK_ROAD:World.Terrain.MOUNTAIN_PATH;}
            check(TerrainConnections.mask(w,4,4)==mask,"all 64 topologies preserve actual adjacency: "+mask);
            for(int d=0;d<6;d++)if((mask&(1<<d))!=0){
                Hex h=center.neighbors().get(d);check((TerrainConnections.mask(w,h.q,h.r)&(1<<((d+3)%6)))!=0,"neighbor has reciprocal branch");
                float dx=(float)((h.q-4+(h.r-4)*.5)*25*Math.sqrt(3)),dy=(h.r-4)*37.5f;
                check(Math.abs(TerrainConnections.edgeX(d)*2-dx)<.001&&Math.abs(TerrainConnections.edgeY(d)*2-dy)<.001,"branches meet exactly at shared edge");
            }
        }
        for(World.Terrain[] row:w.terrain)Arrays.fill(row,World.Terrain.MOUNTAIN);
        w.terrain[0][0]=World.Terrain.PLANK_ROAD;check(TerrainConnections.mask(w,0,0)==0,"map edge cannot invent a road");
        w.terrain[1][0]=World.Terrain.PLAIN;check(TerrainConnections.mask(w,0,0)==1,"road endpoint reaches accessible land");
        w.terrain[4][4]=World.Terrain.WATER;w.terrain[5][4]=World.Terrain.SHALLOWS;
        check(TerrainConnections.mask(w,4,4)==1,"water and shallows share an open shore edge");
        System.out.println("PASS: "+checks+" terrain connection contracts.");
    }
}
