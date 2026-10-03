package game.sanguo.core;

import java.util.*;
import game.sanguo.core.map.SourceGridCoord;

/** Explicit setup on unchanged original national terrain; every transition is a normal command. */
public final class PcUnitsFixture {
    public static final String[] KINDS={"SWORD","SPEAR","HALBERD","CROSSBOW","CAVALRY","RAM","SIEGE_TOWER","CATAPULT","WOODEN_BEAST","transport","BOAT","TOWER_SHIP","WARSHIP","crossbow-melee","mounted-archery"};
    public static final class Case {
        private final World world;private final int actor,enemy,city;private final Hex focus,destination;private final String kind,mode;
        Case(World world,int actor,int enemy,int city,Hex focus,Hex destination,String kind,String mode){this.world=world;this.actor=actor;this.enemy=enemy;this.city=city;this.focus=focus;this.destination=destination;this.kind=kind;this.mode=mode;}
        public World world(){return world;}public int actor(){return actor;}public Hex focus(){return focus;}
        public World.Result command(World w){
            if(mode.equals("move"))return w.move(actor,destination);
            if(city>=0)return w.army.tacticCity(actor,city,tactic(kind));
            if(kind.equals("CATAPULT"))return w.army.tactic(actor,w.unit(enemy).hex,Army.Tactic.STONE);
            return w.attack(actor,enemy);
        }
    }
    private static Army.Tactic tactic(String kind){return kind.equals("RAM")?Army.Tactic.RAM:kind.equals("WOODEN_BEAST")?Army.Tactic.FLAME:Army.Tactic.FIRE_ARROW;}
    public static Case prepare(String kind,String mode)throws Exception{
        if(!Arrays.asList(KINDS).contains(kind)||!Arrays.asList("move","attack").contains(mode)||kind.equals("transport")&&mode.equals("attack"))throw new IllegalArgumentException("source unit fixture scope");
        World w=ScenarioCatalog.load("heroes-250",0);boolean ship=Arrays.asList("BOAT","TOWER_SHIP","WARSHIP").contains(kind);
        World.Officer officer=available(w,0);officer.war=100;Arrays.fill(officer.aptitude,3);
        World.Weapon weapon=ship||kind.equals("transport")?World.Weapon.SWORD:kind.equals("crossbow-melee")?World.Weapon.CROSSBOW:kind.equals("mounted-archery")?World.Weapon.CAVALRY:World.Weapon.valueOf(kind);
        if(kind.equals("mounted-archery")){
            var learned=w.campaign.learned.computeIfAbsent(0,k->EnumSet.noneOf(Campaign.Tech.class));
            for(Campaign.Tech tech=Campaign.Tech.MOUNTED_ARCHERY;tech!=null;tech=tech.prerequisite)learned.add(tech);
        }
        World.Unit actor;
        if(kind.equals("transport")){
            List<World.City> cities=w.cities.stream().filter(c->c.owner==0).collect(java.util.stream.Collectors.toList());if(cities.size()<2)throw new AssertionError("transport endpoints");
            Domestic.Mission m=new Domestic.Mission(w.domestic.nextMissionId++,0,officer.id,cities.get(0).id,cities.get(1).id,new Hex(0,0),true,1000,30000,5000,new int[9]);
            w.domestic.missions.add(m);actor=m;
        }else{actor=new World.Unit(w.nextUnitId++,0,officer.id,weapon,new Hex(0,0),5000,30000);w.units.add(actor);officer.unitId=actor.id;}
        officer.cityId=-1;actor.energy=100;if(ship)actor.ship=Army.Ship.valueOf(kind);
        List<Hex> cells=new ArrayList<>();
        for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){Hex h=new Hex(q,r);if(free(w,h)&&w.army.water(h)==ship&&(ship||w.terrain[q][r]==World.Terrain.PLAIN))cells.add(h);}
        SourceGridCoord anchor=new SourceGridCoord(120,100);Hex anchorCell=MapCoordinates.fromNationalSource(w,anchor);
        cells.sort(Comparator.comparingInt((Hex h)->h.distance(anchorCell)).thenComparingInt(h->h.r).thenComparingInt(h->h.q));
        if(mode.equals("move")){
            for(Hex h:cells){actor.hex=h;for(Hex n:h.neighbors())if(free(w,n)&&w.army.water(n)==ship&&w.orders.previewMove(actor.id,n).valid())return new Case(w,actor.id,-1,-1,h,n,kind,mode);}
        }else if(Arrays.asList("RAM","SIEGE_TOWER","WOODEN_BEAST").contains(kind)){
            for(Hex h:cells){actor.hex=h;for(World.City c:w.cities)if(c.owner>0&&w.army.tacticCityError(actor.id,c.id,tactic(kind))==null)return new Case(w,actor.id,-1,c.id,h,null,kind,mode);}
        }else{
            World.Officer opponent=available(w,1);World.Unit enemy=new World.Unit(w.nextUnitId++,1,opponent.id,World.Weapon.SWORD,new Hex(0,0),5000,30000);w.units.add(enemy);opponent.unitId=enemy.id;opponent.cityId=-1;
            int distance=kind.equals("CROSSBOW")||kind.equals("CATAPULT")||kind.equals("mounted-archery")?2:1;
            for(Hex h:cells){actor.hex=h;for(Hex n:ring(h,distance))if(free(w,n)&&w.army.water(n)==ship){
                enemy.hex=n;String error=kind.equals("CATAPULT")?w.army.tacticError(actor.id,n,Army.Tactic.STONE):w.war.attackError(actor.id,enemy.id);
                if(error==null)return new Case(w,actor.id,enemy.id,-1,h,null,kind,mode);
                enemy.hex=new Hex(0,0);
            }}
        }
        throw new AssertionError("No normal source terrain command for "+kind+"/"+mode);
    }
    private static List<Hex> ring(Hex h,int distance){List<Hex> cells=new ArrayList<>();for(int q=h.q-distance;q<=h.q+distance;q++)for(int r=h.r-distance;r<=h.r+distance;r++){Hex n=new Hex(q,r);if(h.distance(n)==distance)cells.add(n);}return cells;}
    private static boolean free(World w,Hex h){return w.inside(h)&&!NationalMap.restricted(w,h)&&w.cityAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&w.unitAt(h)==null&&w.events.at(h)==null;}
    private static World.Officer available(World w,int owner){for(World.Officer o:w.officers)if(o.owner==owner&&o.unitId<0){w.strategy.releaseGovernor(o.id);o.acted=false;return o;}throw new AssertionError("source commander "+owner);}
}
