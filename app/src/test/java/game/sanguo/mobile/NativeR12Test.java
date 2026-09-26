package game.sanguo.mobile;
import game.sanguo.core.*;
import game.sanguo.mobile.presentation.MapLayerData;
import java.util.*;

public final class NativeR12Test {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.load("coalition-190",0,20260924L);byte[] before=SaveCodec.encode(w);
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);MapProjectionQuery query=new MapProjectionQuery();Territory rules=new Territory(w);
        NavigatorTransform mini=new NavigatorTransform(g.minX,g.minZ,g.maxX,g.maxZ);
        for(int mode:new int[]{1,2,0,2,1}){
            MapLayerData layer=query.layers(w,g,mode);int[] borders=layer.boundaries();
            if(mode==0){check(borders==null&&layer.colors()==null,"off independent");continue;}
            for(int r=0;r<w.height;r++)for(int q=0;q<w.width;q++){
                check(borders[r*w.width+q]==rules.boundary(q,r,mode==2),"exact authority border mode="+mode);
                if(g.valid(new Hex(q,r))){float x=g.grid.x(q,r),z=g.grid.z(q,r);check(Math.abs(mini.x(mini.u(x))-x)<.0001f&&Math.abs(mini.z(mini.v(z))-z)<.0001f,"overview world roundtrip");}
            }
            borders[0]=-999;check(layer.boundaries()[0]!=-999,"frozen boundaries");
        }
        // Full actual official site/structure coverage uses authority; no renderer range derivation.
        for(World.City c:w.cities){MapSceneSnapshot s=new MapSceneSnapshot(g,w,c.hex,-1);check(s.siege.equals(new HashSet<>(SiegeOverlay.selected(w,c.hex).cells)),"siege authority cells");}
        World fixture=FieldSceneFixture.create(8,false,true);MapSceneSnapshot.Ground fg=new MapSceneSnapshot.Ground(fixture);
        for(World.Unit u:fixture.fieldUnits()){MapSceneSnapshot s=new MapSceneSnapshot(fg,fixture,u.hex,u.id);check(s.reachable.equals(fixture.orders.marchReachable(u).keySet()),"move authority cells");
            Set<Hex> expected=new HashSet<>();if(fixture.orders.error(u)==null){
                for(World.Unit target:fixture.fieldUnits())if(target.id!=u.id&&fixture.war.attackError(u.id,target.id)==null)expected.add(target.hex);
                for(World.City city:fixture.cities)if(fixture.siegeError(u.id,city.id)==null)for(Hex h:SiteFootprint.cells(city))if(h.equals(fixture.siegeHit(u,city,h)))expected.add(h);
                for(Domestic.Facility f:fixture.domestic.facilities)if(fixture.war.facilityAttackError(u.id,f.hex)==null)expected.add(f.hex);
                for(War.Structure st:fixture.war.structures())if(fixture.war.structureAttackError(u.id,st.hex)==null)expected.add(st.hex);
            }check(s.attackTargets.equals(expected),"unchanged 2D attack-target queries");}
        for(War.Structure structure:fixture.war.structures()){MapSceneSnapshot s=new MapSceneSnapshot(fg,fixture,structure.hex,-1);check(s.coverage.equals(new HashSet<>(fixture.fieldworks.coverage(structure))),"facility authority cells");}
        check(Arrays.equals(before,SaveCodec.encode(w)),"whole save and RNG unchanged");
        System.out.println("NativeR12Test PASS "+checks+" checks; official boundaries/navigation, fixture ranges, full save unchanged");
    }
}
