package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Focused v44 regression: real command path, saved terrain, packaged maps. */
public final class SandTerrainTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.load("heroes-mobile-sandbox",0);
        World.City home=w.home();List<World.Officer> idle=w.idle(home);check(!idle.isEmpty(),"available officer");
        World.Officer officer=idle.get(0);Arrays.fill(officer.aptitude,3);
        World.Officer foe=w.officers.stream().filter(o->o.owner>=0&&o.owner!=w.player&&o.unitId<0).findFirst().orElseThrow();
        // Detached open field avoids city/structure restrictions obscuring the terrain rule.
        Hex origin=null;
        for(int q=5;q<w.width-5&&origin==null;q++)for(int r=5;r<w.height-5&&origin==null;r++){
            Hex h=new Hex(q,r),target=new Hex(q+1,r);
            if(w.terrain[q][r]==World.Terrain.PLAIN&&w.terrain[q+1][r]==World.Terrain.PLAIN&&w.cityAt(h)==null&&w.cityAt(target)==null&&w.domestic.at(h)==null&&w.domestic.at(target)==null&&w.development.cityAt(h)==null&&w.development.cityAt(target)==null)origin=h;
        }
        check(origin!=null,"open field");
        World.Unit a=new World.Unit(900001,w.player,officer.id,World.Weapon.SPEAR,origin,5000,30000);
        World.Unit b=new World.Unit(900002,foe.owner,foe.id,World.Weapon.HALBERD,new Hex(origin.q+1,origin.r),5000,30000);
        a.energy=100;officer.cityId=-1;officer.unitId=a.id;foe.cityId=-1;foe.unitId=b.id;w.units.add(a);w.units.add(b);w.nextUnitId=900003;
        // Snapshot all state touched by a failed tactic; it must be mutation-free.
        w.terrain[origin.q][origin.r]=World.Terrain.SAND;
        for(War.Tactic tactic:new War.Tactic[]{War.Tactic.THRUST,War.Tactic.SPIRAL,War.Tactic.DOUBLE_THRUST}){
            check(w.war.tacticError(a.id,b.id,tactic).contains("沙地"),"spear sand error "+tactic);
            check(w.war.tacticPreview(a.id,b.id,tactic).text.contains("沙地"),"preview uses same error");
            byte[] before=SaveCodec.encode(w);
            check(!w.war.tactic(a.id,b.id,tactic).ok,"reject sand tactic");
            check(a.energy==100&&!a.acted&&a.hex.equals(origin)&&b.troops==5000,"no combat mutation");
            // Failure may append a UI/log message; energy, positions and troops are unchanged.
            check(SaveCodec.decode(before).terrain[origin.q][origin.r]==World.Terrain.SAND,"sand save round trip");
        }
        check(w.cost(origin,World.Weapon.SPEAR)==1,"sand is normal walkable land");
        check(w.war.attackError(a.id,b.id)==null,"spear basic attack still legal on sand");
        w.terrain[origin.q][origin.r]=World.Terrain.PLAIN;w.terrain[b.hex.q][b.hex.r]=World.Terrain.SAND;
        check(w.war.tacticError(a.id,b.id,War.Tactic.SPIRAL)==null,"sand target does not forbid spear on plain");
        World.Unit halberd=new World.Unit(a.id,a.owner,a.officerId,World.Weapon.HALBERD,origin,5000,30000);halberd.energy=100;
        w.units.set(w.units.indexOf(a),halberd);w.terrain[origin.q][origin.r]=World.Terrain.SAND;
        check(w.war.tacticError(halberd.id,b.id,War.Tactic.SWEEP)==null,"halberd tactics not restricted on sand");
        for(ScenarioCatalog.Summary entry:ScenarioCatalog.summaries()){
            World game=ScenarioCatalog.load(entry.id,0);int sand=0;
            for(World.Terrain[] row:game.terrain)for(World.Terrain t:row)if(t==World.Terrain.SAND)sand++;
            check(sand>0,"sand present: "+entry.id);
            World restored=SaveCodec.decode(SaveCodec.encode(game));int restoredSand=0;
            for(World.Terrain[] row:restored.terrain)for(World.Terrain t:row)if(t==World.Terrain.SAND)restoredSand++;
            check(sand==restoredSand,"all sand retained after reload: "+entry.id);
            World.City xuchang=game.city(20013),xinye=game.city(20028);
            if(xuchang!=null&&xinye!=null)check(connected(game,xuchang.hex,xinye.hex),"Xuchang-Xinye overland connection: "+entry.id);
            for(World.City city:game.cities)check(game.terrain[city.hex.q][city.hex.r]!=World.Terrain.SAND,"site footprint retained");
        }
        System.out.println("PASS v44 sand terrain: "+checks+" checks");
    }
    private static boolean connected(World w,Hex a,Hex b){
        Set<Hex> seen=new HashSet<>();ArrayDeque<Hex> queue=new ArrayDeque<>();queue.add(a);seen.add(a);
        while(!queue.isEmpty())for(Hex h:queue.remove().neighbors()){
            if(!w.inside(h)||seen.contains(h))continue;World.Terrain t=w.terrain[h.q][h.r];
            if(t!=World.Terrain.PLAIN&&t!=World.Terrain.FOREST&&t!=World.Terrain.SAND)continue;
            if(h.equals(b))return true;seen.add(h);queue.add(h);
        }return false;
    }
}
