package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Read-only reproducible data-driven camera stops; shared by host audit and installed tour.
 * Test code only. No city names, national dimensions, gameplay mutations or RNG use. */
final class R15TourPlan {
    static final class Stop {
        final String id,reason; final Hex hex;
        Stop(String id,String reason,Hex hex){this.id=id;this.reason=reason;this.hex=hex;}
    }
    static List<Stop> regions(World w){
        GridWorldTransform grid=new GridWorldTransform(w.sourceMapWidth>0?(w.height-1)/2:0,w.columnStaggered);
        List<Hex> cells=new ArrayList<>();float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;
        for(int r=0;r<w.height;r++)for(int q=0;q<w.width;q++){
            Hex h=new Hex(q,r);if(!w.inside(h))continue;cells.add(h);
            minX=Math.min(minX,grid.x(h));maxX=Math.max(maxX,grid.x(h));minZ=Math.min(minZ,grid.z(h));maxZ=Math.max(maxZ,grid.z(h));
        }
        List<Stop> result=new ArrayList<>();Set<Hex> used=new HashSet<>();
        for(int z=0;z<3;z++)for(int x=0;x<4;x++){
            float cx=minX+(maxX-minX)*(x+.5f)/4,cz=minZ+(maxZ-minZ)*(z+.5f)/3;
            Hex best=null;float distance=Float.MAX_VALUE;
            for(Hex h:cells)if(!used.contains(h)){float dx=grid.x(h)-cx,dz=grid.z(h)-cz,d=dx*dx+dz*dz;if(d<distance){best=h;distance=d;}}
            if(best!=null){used.add(best);result.add(new Stop("region-"+z+"-"+x,"4x3 projected bounds strata; nearest distinct valid center",best));}
        }
        // Highest actual density, including difficult areas instead of curated scenic cities.
        for(World.Terrain terrain:new World.Terrain[]{World.Terrain.FOREST,World.Terrain.MOUNTAIN,World.Terrain.SAND,World.Terrain.WATER}){
            Hex best=null;int score=-1;
            for(Hex h:cells)if(w.terrain[h.q][h.r]==terrain){int count=0;
                for(int r=Math.max(0,h.r-4);r<Math.min(w.height,h.r+5);r++)for(int q=Math.max(0,h.q-4);q<Math.min(w.width,h.q+5);q++)if(w.terrain[q][r]==terrain)count++;
                if(count>score){best=h;score=count;}}
            if(best!=null)result.add(new Stop("dense-"+terrain,"maximum same-terrain count in 9x9 storage neighborhood="+score,best));
        }
        for(World.SiteKind kind:new World.SiteKind[]{World.SiteKind.PORT,World.SiteKind.GATE,World.SiteKind.CITY}){
            World.City best=null;int score=-1;
            for(World.City c:w.cities)if(c.kind==kind){int count=0;for(World.City other:w.cities)if(c.hex.distance(other.hex)<=12)count++;if(count>score){best=c;score=count;}}
            if(best!=null)result.add(new Stop("dense-"+kind,"most sites within 12 logical steps="+score+"; id="+best.id,best.hex));
        }
        return Collections.unmodifiableList(result);
    }
}
