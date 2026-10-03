package game.sanguo.core;

/** Explicit original-national-map setup; firing is driven only by the normal next-turn command. */
public final class PcFacilityRigsFixture {
    public static final class Case {
        private final World world;private final int tower,target;private final Hex focus;
        Case(World world,int tower,int target,Hex focus){this.world=world;this.tower=tower;this.target=target;this.focus=focus;}
        public World world(){return world;}public int tower(){return tower;}public int target(){return target;}public Hex focus(){return focus;}
        public World.Result command(World w){return w.nextTurn();}
    }
    public static Case prepare(boolean damaged)throws Exception{
        World w=ScenarioCatalog.load("heroes-250",0);
        World.Officer leader=w.officers.stream().filter(o->o.owner==1&&o.unitId<0).findFirst().orElseThrow();w.strategy.releaseGovernor(leader.id);
        Hex anchor=MapCoordinates.fromNationalSource(w,new game.sanguo.core.map.SourceGridCoord(120,100));
        java.util.List<Hex> places=new java.util.ArrayList<>();for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){Hex h=new Hex(q,r);if(free(w,h))places.add(h);}
        places.sort(java.util.Comparator.comparingInt((Hex h)->h.distance(anchor)).thenComparingInt(h->h.r).thenComparingInt(h->h.q));
        for(Hex site:places)for(Hex cell:places)if(site.distance(cell)==2){
            War.Structure tower=new War.Structure(w.war.nextStructureId++,0,War.StructureKind.CATAPULT_TOWER,site,damaged?400:War.StructureKind.CATAPULT_TOWER.hp);w.war.structures.add(tower);
            World.Unit target=new World.Unit(w.nextUnitId++,1,leader.id,World.Weapon.SWORD,cell,5000,30000);target.status=War.Status.CONFUSED;target.statusTurns=2;w.units.add(target);leader.unitId=target.id;leader.cityId=-1;
            if(w.fieldworks.inRange(tower,cell)&&w.fieldworks.landTarget(0,cell))return new Case(w,tower.id,target.id,site);
            throw new AssertionError("Original map platform fixture range");
        }
        throw new AssertionError("No original map platform firing site");
    }
    private static boolean free(World w,Hex h){return w.inside(h)&&!w.army.water(h)&&w.terrain[h.q][h.r]==World.Terrain.PLAIN&&!NationalMap.restricted(w,h)&&w.cityAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&w.unitAt(h)==null&&w.events.at(h)==null;}
}
