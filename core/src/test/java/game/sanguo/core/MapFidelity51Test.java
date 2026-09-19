package game.sanguo.core;

import java.util.*;

/** New regional corridors checked with the shipped engine and every domestic plot occupied. */
public final class MapFidelity51Test {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static boolean open(World w,Hex h){return w.inside(h)&&w.cityAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&w.unitAt(h)==null;}
    private static Set<Hex> reach(World w,World.Unit u,World.City from){
        Set<Hex> seen=new HashSet<>();ArrayDeque<Hex> todo=new ArrayDeque<>();
        for(Hex h:from.hex.neighbors())if(open(w,h)&&!w.army.water(h)&&w.fieldworks.landCost(h,u.weapon,0)>0){seen.add(h);todo.add(h);}
        while(!todo.isEmpty()){
            Hex a=todo.remove();
            for(Hex b:a.neighbors())if(!seen.contains(b)&&open(w,b)&&!w.army.water(b)&&w.army.moveCost(u,a,b)>0){seen.add(b);todo.add(b);}
        }
        return seen;
    }
    public static void main(String[] args)throws Exception {
        for(ScenarioCatalog.Summary s:ScenarioCatalog.summaries()){
            World w=ScenarioCatalog.load(s.id,0);
            if(w.height!=100)continue;
            byte[] saved=SaveCodec.encode(w);World loaded=SaveCodec.decode(saved);
            check(Arrays.equals(saved,SaveCodec.encode(loaded)),"new terrain survives embedded save");
            w.units.clear();w.domestic.missions.clear();w.war.structures.clear();w.domestic.facilities.clear();w.campaign.treaties.clear();w.campaign.learned.clear();
            for(World.City c:w.cities)c.owner=0;
            int next=1;for(World.City c:w.cities)for(Hex h:w.development.parcels(c.id))w.domestic.facilities.add(new Domestic.Facility(next++,c.id,Domestic.Kind.FARM,h,-1,0));
            World.Unit u=new World.Unit(99999,0,w.officers.get(0).id,World.Weapon.SPEAR,new Hex(1,1),5000,30000);
            Map<Integer,Set<Hex>> reachable=new HashMap<>();
            for(int[] pair:PAIRS){
                World.City a=w.city(pair[0]),b=w.city(pair[1]);
                Set<Hex> seen=reachable.computeIfAbsent(a.id,k->reach(w,u,a));
                check(b.hex.neighbors().stream().anyMatch(seen::contains),s.id+" full-built dry route "+a.name+" -> "+b.name);
            }
            for(int[] xy:RESTRICTED){
                Hex h=MapCoordinates.axial(xy[0],xy[1],w.height);
                check(w.terrain[h.q][h.r]==World.Terrain.MOUNTAIN_PATH,s.id+" source red pass at "+Arrays.toString(xy));
                check(w.fieldworks.landCost(h,World.Weapon.SPEAR,0)<0,"red pass unavailable without difficult march");
                w.campaign.learned.put(0,EnumSet.of(Campaign.Tech.DIFFICULT_MARCH));
                check(w.fieldworks.landCost(h,World.Weapon.SPEAR,0)>0,"red pass available after difficult march");
                w.campaign.learned.clear();
            }
            int poison=0;for(int y=76;y<100;y++)for(int x=0;x<30;x++){
                Hex h=MapCoordinates.axial(x,y,100);if(w.terrain[h.q][h.r]==World.Terrain.POISON)poison++;
            }
            check(poison>=30,"southern source poison terrain must retain gameplay semantics");

            System.out.println("PASS v51 "+s.id+": 11 full-built dry connections, 13 technology-gated source cells, southern poison and save");
        }
        System.out.println("PASS v51 runtime: "+checks+" checks");
    }
    private static final int[][] PAIRS={{20002,20001},{20001,20000},{20002,20003},{20003,20004},{20004,20006},{20007,20056},{20038,20040},{20040,20041},{20032,20034},{20031,20033},{20034,20033}};
    private static final int[][] RESTRICTED={{73,2},{73,3},{65,5},{82,5},{83,5},{65,6},{66,6},{67,6},{69,7},{56,9},{57,9},{56,18},{56,19}};
}
