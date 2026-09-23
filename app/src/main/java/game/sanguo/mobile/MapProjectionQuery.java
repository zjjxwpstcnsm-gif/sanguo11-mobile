package game.sanguo.mobile;

import game.sanguo.core.*;
import game.sanguo.mobile.presentation.MapLayerData;
import java.util.*;

/** Migration mapper: host logic thread only, fed a detached LegacyView, never the authority.
 * All rule queries previously inside FilamentMapView live here; renderers receive values only. */
final class MapProjectionQuery {
    private Object ground;
    private String identity="";
    private int[] colors;
    Set<Long> blocked(World w,boolean show){
        Set<Long> cells=new HashSet<>();
        if(show&&w!=null)for(int r=0;r<w.height;r++)for(int q=0;q<w.width;q++){
            Hex h=new Hex(q,r);if(w.inside(h)&&w.cost(h,World.Weapon.SPEAR)<1)cells.add(MapLayerData.cellKey(q,r));
        }
        return Collections.unmodifiableSet(cells);
    }
    MapLayerData layers(World w,Object currentGround,int mode){
        Map<String,String> labels=new HashMap<>();Map<String,Integer> owners=new HashMap<>();
        StringBuilder key=new StringBuilder();
        for(World.City c:w.cities){String id="site:"+c.id;owners.put(id,c.owner);
            labels.put(id,c.owner<0?"":w.governance.label(c.owner));
            key.append(c.id).append(':').append(c.owner).append(':').append(c.hex.q).append(':').append(c.hex.r).append(';');
        }
        if(mode==0)return new MapLayerData(labels,owners,null);
        String next=key.toString();
        if(colors==null||ground!=currentGround||!next.equals(identity)){
            Territory territory=new Territory(w);int[] computed=new int[w.width*w.height];
            for(int r=0;r<w.height;r++)for(int q=0;q<w.width;q++){
                int owner=territory.ownerAt(q,r);if(owner>=0)computed[r*w.width+q]=FactionColors.color(w,owner);
            }
            colors=computed;ground=currentGround;identity=next;
        }
        return new MapLayerData(labels,owners,colors);
    }
}
