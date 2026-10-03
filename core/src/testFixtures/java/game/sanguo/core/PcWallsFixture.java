package game.sanguo.core;

/** Legal source-map setup. Every transition after installation is a real command. */
public final class PcWallsFixture {
    public static final class Case {
        private final World world;private final Hex target;private final int actor,id;private final String mode;
        Case(World w,Hex h,int a,int id,String m){world=w;target=h;actor=a;this.id=id;mode=m;}
        public World world(){return world;}public Hex target(){return target;}public String mode(){return mode;}
        public World.Result command(World w){
            War.StructureKind kind=mode.startsWith("earth")?War.StructureKind.EARTH_WALL:War.StructureKind.STONE_WALL;
            return mode.contains("build")?w.fieldworks.build(actor,kind,target,0):mode.contains("complete")?w.fieldworks.repair(actor,id):w.war.attackStructure(actor,target);
        }
    }
    public static Case prepare(String mode)throws Exception {
        World w=ScenarioCatalog.load("heroes-250",0);War.StructureKind kind=mode.startsWith("earth")?War.StructureKind.EARTH_WALL:War.StructureKind.STONE_WALL;
        if(kind==War.StructureKind.STONE_WALL){w.campaign.finishTech(0,Campaign.Tech.AXLE);w.campaign.finishTech(0,Campaign.Tech.STONE_BUILDING);}
        World.Officer leader=null;for(World.Officer o:w.officers)if(o.owner==0){leader=o;break;}
        if(leader==null)throw new AssertionError("No commander");w.strategy.releaseGovernor(leader.id);
        if(mode.contains("destroy"))leader.war=100;
        World.Unit actor=new World.Unit(w.nextUnitId++,0,leader.id,World.Weapon.SWORD,new Hex(0,0),5000,10000);actor.gold=10000;w.units.add(actor);leader.unitId=actor.id;leader.cityId=-1;
        boolean building=mode.contains("build"),completing=mode.contains("complete");
        for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
            Hex target=new Hex(q,r);if(!free(w,target))continue;
            if(mode.endsWith("odd")&&(MapCoordinates.nationalSource(w,target).x&1)==0||mode.endsWith("even")&&(MapCoordinates.nationalSource(w,target).x&1)!=0)continue;
            Hex[] ring=target.neighbors().toArray(new Hex[0]);if(!free(w,ring[0])||!free(w,ring[3]))continue;
            for(int n:new int[]{1,2,4,5})if(free(w,ring[n])){
                actor.hex=ring[n];if(w.fieldworks.buildError(actor.id,kind,target,0)!=null)continue;
                int owner=building||completing?0:1;
                w.war.structures.add(new War.Structure(w.war.nextStructureId++,owner,kind,ring[0],kind.hp));
                w.war.structures.add(new War.Structure(w.war.nextStructureId++,owner,kind,ring[3],kind.hp));
                int id=-1;
                if(!building){
                    int hp=completing?kind.hp-w.fieldworks.constructionRate(actor):mode.contains("destroy")?kind.hp/2:kind==War.StructureKind.EARTH_WALL?400:510;
                    War.Structure s=new War.Structure(w.war.nextStructureId++,owner,kind,target,hp);s.complete=!completing;w.war.structures.add(s);id=s.id;
                }
                return new Case(w,target,actor.id,id,mode);
            }
        }
        throw new AssertionError("No legal source connected-wall fixture "+mode);
    }
    private static boolean free(World w,Hex h){return w.inside(h)&&w.cost(h,World.Weapon.SWORD)>0&&!w.army.water(h)&&w.cityAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&w.unitAt(h)==null&&w.events.at(h)==null;}
}
