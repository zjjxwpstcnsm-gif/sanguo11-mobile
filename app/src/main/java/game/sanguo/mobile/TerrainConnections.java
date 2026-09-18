package game.sanguo.mobile;

import game.sanguo.core.World;

/** Same axial order as Hex.neighbors(): east, northeast, northwest, west, southwest, southeast. */
final class TerrainConnections {
    static final int[] DQ={1,1,0,-1,-1,0},DR={0,-1,-1,0,1,1};
    static boolean road(World.Terrain t){return t==World.Terrain.PLANK_ROAD||t==World.Terrain.MOUNTAIN_PATH;}
    static boolean water(World.Terrain t){return t==World.Terrain.WATER||t==World.Terrain.SEA||t==World.Terrain.SHALLOWS;}
    static boolean inside(World w,int q,int r){return q>=0&&r>=0&&q<w.width&&r<w.height;}
    static int mask(World w,int q,int r){
        World.Terrain terrain=w.terrain[q][r];int mask=0;
        for(int d=0;d<6;d++){
            int nq=q+DQ[d],nr=r+DR[d];if(!inside(w,nq,nr))continue;
            World.Terrain neighbor=w.terrain[nq][nr];
            if(road(terrain)?road(neighbor):water(terrain)&&water(neighbor))mask|=1<<d;
        }
        if(!road(terrain)||Integer.bitCount(mask)>=2)return mask;
        // At an endpoint, continue toward accessible land, favoring a straight exit.
        int entry=mask==0?-1:Integer.numberOfTrailingZeros(mask);
        int exit=landExit(w,q,r,entry,mask);if(exit>=0)mask|=1<<exit;
        if(entry<0&&exit>=0){int second=landExit(w,q,r,exit,mask);if(second>=0)mask|=1<<second;}
        return mask;
    }
    private static int landExit(World w,int q,int r,int from,int used){
        int best=-1,score=-1;
        for(int d=0;d<6;d++){
            int nq=q+DQ[d],nr=r+DR[d];if((used&(1<<d))!=0||!inside(w,nq,nr))continue;
            World.Terrain t=w.terrain[nq][nr];if(t!=World.Terrain.PLAIN&&t!=World.Terrain.FOREST&&t!=World.Terrain.SHALLOWS)continue;
            int separation=from<0?0:Math.min(Math.abs(from-d),6-Math.abs(from-d));
            if(separation>score){best=d;score=separation;}
        }
        return best;
    }
    static float edgeX(int d){return (float)(Math.cos(-d*Math.PI/3)*25*Math.sqrt(3)/2);}
    static float edgeY(int d){return (float)(Math.sin(-d*Math.PI/3)*25*Math.sqrt(3)/2);}
}
