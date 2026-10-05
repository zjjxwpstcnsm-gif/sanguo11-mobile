package game.sanguo.core;
import java.util.*;
/** Prepared source-map battlefield; damage is exclusively the normal siege command. */
public final class PcSitesFixture {
    public static final class Case {
        private final World world;private final int target,attacker;
        Case(World w,int t,int a){world=w;target=t;attacker=a;}
        public World world(){return world;}public int target(){return target;}public int attacker(){return attacker;}
    }
    public static Case prepare(int kind)throws Exception {
        World w=ScenarioCatalog.load("heroes-250",0);World.City target=null;
        for(World.City c:w.cities)if((c.kind==World.SiteKind.CITY?0:c.kind==World.SiteKind.GATE?2:1)==kind&&c.owner>0){target=c;break;}
        if(target==null)throw new AssertionError("No enemy source site");target.defense=kind==0?1010:510;target.troops=12000;
        World.Officer leader=null;for(World.Officer o:w.officers)if(o.owner==0){leader=o;break;}
        if(leader==null)throw new AssertionError("No faction commander");
        Hex approach=null;Set<Hex> candidates=new LinkedHashSet<>();for(Hex h:SiteFootprint.cells(target))candidates.addAll(h.neighbors());
        for(Hex h:candidates)if(w.inside(h)&&w.cityAt(h)==null&&!w.army.water(h)&&w.terrain[h.q][h.r]!=World.Terrain.MOUNTAIN){approach=h;break;}
        if(approach==null)throw new AssertionError("No legal approach");
        World.Unit u=new World.Unit(w.nextUnitId++,0,leader.id,World.Weapon.SWORD,approach,5000,10000);
        w.units.add(u);leader.unitId=u.id;leader.cityId=-1;
        return new Case(w,target.id,u.id);
    }
}
