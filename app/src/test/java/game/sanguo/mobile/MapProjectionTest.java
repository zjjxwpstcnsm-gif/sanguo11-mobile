package game.sanguo.mobile;
import game.sanguo.core.*;
import game.sanguo.mobile.presentation.MapLayerData;
import java.util.*;
public final class MapProjectionTest {
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.load("coalition-190",0,20260923L);byte[] before=SaveCodec.encode(w);
        MapProjectionQuery mapper=new MapProjectionQuery();Object ground=new Object();
        MapLayerData data=mapper.layers(w,ground,1);int[] actual=data.colors();Territory old=new Territory(w);
        int assertions=0;
        for(int r=0;r<w.height;r++)for(int q=0;q<w.width;q++){
            int owner=old.ownerAt(q,r);int expected=owner<0?0:FactionColors.color(w,owner);
            if(actual[r*w.width+q]!=expected)throw new AssertionError("Old territory projection mismatch");assertions++;
        }
        Set<Long> blocked=mapper.blocked(w,true);
        for(int r=0;r<w.height;r++)for(int q=0;q<w.width;q++){
            Hex h=new Hex(q,r);if(blocked.contains(MapLayerData.cellKey(q,r))!=(w.inside(h)&&w.cost(h,World.Weapon.SPEAR)<1))throw new AssertionError("Passability mismatch");assertions++;
        }
        actual[0]=123;if(data.colors()[0]==123)throw new AssertionError("Leaked projection array");
        if(mapper.layers(w,ground,0).colors()!=null)throw new AssertionError("Territory toggle");
        if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("Projection mutated rule state");
        System.out.println("MapProjectionTest PASS: "+assertions+" exact old-query comparisons; detached values and unchanged complete save");
    }
}
