package game.sanguo.core;

/** Deterministic presentation-only fixture; not a new production scenario. */
public final class SceneFacilityFixture {
    public static World create() throws java.io.IOException{
        World w=ScenarioCatalog.all().get(0);
        w.domestic.facilities.clear();w.war.structures.clear();w.war.fires.clear();
        int id=10000;World.City home=w.cities.get(0);
        for(Domestic.Kind kind:Domestic.Kind.values())
            for(int level=1;level<=(Domestic.mergeable(kind)?3:1);level++){
                Domestic.Facility f=new Domestic.Facility(id++,home.id,kind,new Hex(20+(id%20),40+id%10),-1,0);
                f.level=level;w.domestic.facilities.add(f);
            }
        for(War.StructureKind kind:War.StructureKind.values())
            w.war.structures.add(new War.Structure(id++,home.owner,kind,new Hex(50+id%20,60+id%10),kind.hp));
        return w;
    }
    public static void ignite(World w,Hex h){w.war.fires.add(new War.Fire(h,w.player,2));}
    public static void extinguish(World w){w.war.fires.clear();}
    public static void remove(World w,War.Structure s){w.war.structures.remove(s);}
}
