package game.sanguo.core;
import java.util.*;

/** Source-map setup only. Every accepted visible transition uses a real command. */
public final class PcFacilitiesFixture {
    public static final class Case {
        private final World world;private final Hex target;private final int actor,city,officer;private final String mode;
        Case(World w,Hex h,int a,int c,int o,String m){world=w;target=h;actor=a;city=c;officer=o;mode=m;}
        public World world(){return world;}public Hex target(){return target;}public String mode(){return mode;}
        public World.Result command(World w){return switch(mode){
            case "domestic-build"->w.domestic.build(city,officer,Domestic.Kind.FARM,target);
            case "military-build"->w.fieldworks.build(actor,War.StructureKind.CAMP,target,0);
            case "domestic-damage","domestic-destroy"->w.war.attackFacility(actor,target);
            default->w.war.attackStructure(actor,target);
        };}
    }
    public static Case prepare(String mode)throws Exception {
        World w=ScenarioCatalog.load("heroes-250",0);
        if(mode.equals("domestic-build")){
            for(World.City c:w.cities)if(c.owner==0&&c.kind==World.SiteKind.CITY&&!w.idle(c).isEmpty()){
                List<Hex> parcels=w.domestic.buildSites(c.id);if(!parcels.isEmpty())return new Case(w,parcels.get(0),-1,c.id,w.idle(c).get(0).id,mode);
            }
            throw new AssertionError("No normal source domestic build site");
        }
        World.Officer leader=null;for(World.Officer o:w.officers)if(o.owner==0){leader=o;break;}
        if(leader==null)throw new AssertionError("No source commander");w.strategy.releaseGovernor(leader.id);
        World.Unit attacker=new World.Unit(w.nextUnitId++,0,leader.id,World.Weapon.SWORD,new Hex(0,0),5000,10000);attacker.gold=10000;
        w.units.add(attacker);leader.unitId=attacker.id;leader.cityId=-1;
        if(mode.startsWith("domestic-")){
            for(World.City c:w.cities)if(c.owner>0&&c.kind==World.SiteKind.CITY)for(Hex parcel:w.domestic.buildSites(c.id))for(Hex h:parcel.neighbors())if(freeLand(w,h)){
                Domestic.Facility f=new Domestic.Facility(w.domestic.nextFacilityId++,c.id,Domestic.Kind.FARM,parcel,-1,0);f.hp=mode.endsWith("destroy")?1:510;w.domestic.facilities.add(f);
                attacker.hex=h;return new Case(w,parcel,attacker.id,c.id,-1,mode);
            }
        }else{
            for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
                Hex h=new Hex(q,r);if(!freeLand(w,h))continue;attacker.hex=h;
                for(Hex target:h.neighbors())if(w.fieldworks.buildError(attacker.id,War.StructureKind.CAMP,target,0)==null){
                    if(!mode.equals("military-build"))w.war.structures.add(new War.Structure(w.war.nextStructureId++,1,War.StructureKind.CAMP,target,mode.endsWith("destroy")?1:510));
                    return new Case(w,target,attacker.id,-1,-1,mode);
                }
            }
        }
        throw new AssertionError("No legal source facility fixture "+mode);
    }
    private static boolean freeLand(World w,Hex h){return w.inside(h)&&w.cost(h,World.Weapon.SWORD)>0&&!w.army.water(h)&&w.cityAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&w.unitAt(h)==null&&w.events.at(h)==null;}
}
