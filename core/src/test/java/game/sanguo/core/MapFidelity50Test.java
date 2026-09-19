package game.sanguo.core;

import java.util.*;
import java.util.function.Predicate;

/** Runtime checks on shipped geography, using actual movement and occupation rules. */
public final class MapFidelity50Test {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Hex at(World w,int x,int y){return MapCoordinates.axial(x,y,w.height);}
    private static boolean open(World w,Hex h){return w.inside(h)&&w.cityAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&w.unitAt(h)==null;}
    private static Set<Hex> reach(World w,World.Unit u,Hex start,Predicate<Hex> allowed){
        Set<Hex> seen=new HashSet<>();ArrayDeque<Hex> todo=new ArrayDeque<>();seen.add(start);todo.add(start);
        while(!todo.isEmpty()){Hex h=todo.remove();for(Hex v:h.neighbors())if(!seen.contains(v)&&allowed.test(v)&&open(w,v)&&w.army.moveCost(u,h,v)>0){seen.add(v);todo.add(v);}}
        return seen;
    }
    public static void main(String[] args)throws Exception{
        for(ScenarioCatalog.Summary summary:ScenarioCatalog.summaries()){
            World w=ScenarioCatalog.load(summary.id,0);byte[] saved=SaveCodec.encode(w);
            World restored=SaveCodec.decode(saved);check(Arrays.equals(saved,SaveCodec.encode(restored)),summary.id+" embedded map save is stable");
            // Existing worlds embed their geography. The new catalog must not silently relocate them.
            World.City first=restored.cities.get(0);World.Terrain marker=restored.terrain[first.hex.q][first.hex.r];
            restored.terrain[first.hex.q][first.hex.r]=World.Terrain.FOREST;
            World changed=SaveCodec.decode(SaveCodec.encode(restored));check(changed.terrain[first.hex.q][first.hex.r]==World.Terrain.FOREST,"catalog does not overwrite saved terrain");
            restored.terrain[first.hex.q][first.hex.r]=marker;
            w.campaign.treaties.clear();w.units.clear();w.domestic.missions.clear();w.war.structures.clear();w.domestic.facilities.clear();
            for(World.City c:w.cities)c.owner=0;
            World.Officer officer=w.officers.get(0);World.Unit u=new World.Unit(99999,0,officer.id,World.Weapon.SPEAR,new Hex(1,1),5000,30000);
            int next=1;for(World.City c:w.cities)for(Hex h:w.development.parcels(c.id))w.domestic.facilities.add(new Domestic.Facility(next++,c.id,Domestic.Kind.FARM,h,-1,0));
            for(World.City port:w.cities){
                if(port.kind!=World.SiteKind.PORT)continue;
                Hex land=null,water=null;int bestLand=0;
                for(Hex a:port.hex.neighbors())for(Hex b:port.hex.neighbors())if(open(w,a)&&open(w,b)&&!w.army.water(a)&&w.army.water(b)&&a.distance(b)==1&&w.army.moveCost(u,a,b)>0&&w.army.moveCost(u,b,a)>0){int size=reach(w,u,a,h->!w.army.water(h)&&h.distance(port.hex)<=7).size();if(size>bestLand){bestLand=size;land=a;water=b;}}
                check(land!=null,summary.id+" full domestic build blocks dock "+port.name);
                check(w.army.transitionPort(0,land,water)==port,port.name+" own dock required");
                port.owner=1;check(w.army.moveCost(u,land,water)<0&&w.army.moveCost(u,water,land)<0,port.name+" hostile dock cannot embark or land");port.owner=0;
                final Hex l=land;Set<Hex> hinterland=reach(w,u,l,h->!w.army.water(h)&&h.distance(port.hex)<=7);
                check(hinterland.size()>5,summary.id+" isolated dock land pocket "+port.id+" "+port.name+" "+hinterland.size());
            }
            if(w.height==100){
                for(int[] gate:GATES){
                    World.City c=w.city(gate[0]);Hex source=MapCoordinates.source(c.hex,w.height);
                    check(source.q==gate[1]&&source.r==gate[2],"registered gate pin "+c.name);
                    int x=gate[1],y=gate[2],extent=gate[4];boolean vertical=gate[3]==1;
                    Hex a=at(w,vertical?x-2:x,vertical?y:y-2),b=at(w,vertical?x+2:x,vertical?y:y+2);
                    Predicate<Hex> local=h->{Hex p=MapCoordinates.source(h,w.height);return !w.army.water(h)&&Math.abs(p.q-x)<=(vertical?2:extent)&&Math.abs(p.r-y)<=(vertical?extent:2);};
                    check(reach(w,u,a,local).contains(b),summary.id+" friendly gate crossing "+c.name);
                    c.owner=1;check(!reach(w,u,a,local).contains(b),summary.id+" hostile gate cannot be bypassed in its pass "+c.id+" "+c.name);c.owner=0;
                }
                for(int[] pair:LAND_PAIRS){World.City a=w.city(20000+pair[0]),b=w.city(20000+pair[1]);Set<Hex> targets=new HashSet<>(b.hex.neighbors());boolean found=false;
                    for(Hex start:a.hex.neighbors())if(open(w,start)&&!w.army.water(start)&&w.fieldworks.landCost(start,u.weapon,0)>0){
                        Set<Hex> route=reach(w,u,start,h->!w.army.water(h));found=route.stream().anyMatch(targets::contains);if(found)break;
                    }
                    check(found,summary.id+" sourced land corridor "+a.name+" -> "+b.name);
                }
            }
            System.out.println("PASS runtime map "+summary.id+": all built plots, docks, terrain save, gate corridors");
        }
        System.out.println("PASS v50 runtime: "+checks+" geography/movement/save checks");
    }
    private static final int[][] GATES={{20042,46,28,1,4},{20043,45,38,1,4},{20044,30,38,1,4},{20045,34,38,1,3},{20046,34,43,1,3},{20047,13,41,0,4},{20048,6,48,0,4},{20049,11,51,0,3},{20050,5,59,1,3},{20051,4,62,0,4}};
    private static final int[][] LAND_PAIRS={{21,19},{19,20},{20,36},{36,37},{37,39},{39,38},{36,18},{17,15},{15,12},{5,6},{16,13},{13,28},{22,23},{23,24}};
}
