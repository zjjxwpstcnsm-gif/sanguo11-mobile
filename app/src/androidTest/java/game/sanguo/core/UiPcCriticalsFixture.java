package game.sanguo.core;

import java.util.*;
import game.sanguo.core.map.SourceGridCoord;

/** App-owned installed fixture. Retains managed ability state and original terrain;
 * skill/crew setup is saved and validated before any normal command. The frozen
 * core test fixture is left unchanged. */
public final class UiPcCriticalsFixture {
    public static final class Case {
        public final World world;public final int actor,target;public final boolean tactic;public final War.Plot plot;
        public final int selector;public final String label;
        Case(World world,int actor,int target,War.Plot plot,int selector,String label){this.world=world;this.actor=actor;this.target=target;this.plot=plot;tactic=plot==null;this.selector=selector;this.label=label;}
        public World.Result command(World w){return tactic?w.war.tactic(actor,target,War.Tactic.THRUST):w.war.plot(actor,w.unit(target).hex,plot);}
    }
    public static Case prepare(boolean tactic)throws Exception{return prepare(tactic?null:War.Plot.SORCERY);}
    public static Case prepare(War.Plot plot)throws Exception{
        return prepare(plot,"关羽",250,152,"tactic-guanyu");
    }
    /** Calendar/actor setup is saved before normal tactic resolution. */
    public static Case preparePortrait(String name,int year,int selector,String label)throws Exception{
        return prepare(null,name,year,selector,label);
    }
    public static List<Case> presentations()throws Exception{
        List<Case> cases=new ArrayList<>();for(War.Plot plot:new War.Plot[]{null,War.Plot.SORCERY,War.Plot.LIGHTNING})cases.add(prepare(plot));
        String[] names={"刘备","关羽","张飞","赵云","诸葛亮","曹操"},keys={"liubei","guanyu","zhangfei","zhaoyun","zhugeliang","caocao"};
        int[][] expected={{208,136,156},{208,132,152},{208,143,163},{225,131,151},{223,134,154},{208,133,153}};
        for(int i=0;i<names.length;i++)for(int old=0;old<2;old++)cases.add(preparePortrait(names[i],expected[i][0]-1+old,expected[i][1+old],"tactic-"+keys[i]+(old==0?"-young":"-old")));
        return cases;
    }
    private static Case prepare(War.Plot plot,String portraitName,int year,int selector,String portraitLabel)throws Exception{
        boolean tactic=plot==null;
        World w=ScenarioCatalog.load("heroes-250",0);String name=tactic?portraitName:"诸葛亮";
        w.startYear=year;w.startMonth=1;w.turn=0;
        World.Officer officer=w.officers.stream().filter(o->o.name.equals(name)).findFirst().orElseThrow(()->new AssertionError("Source officer "+name));
        // heroes-250 leaves some original officers unaffiliated. This is
        // explicit fixture setup before saving, never a gameplay resolution.
        if(officer.owner<0){officer.owner=w.officers.stream().filter(o->o.name.equals("关羽")&&o.owner>=0).findFirst().orElseThrow().owner;officer.role=Strategy.Role.OFFICER;officer.loyalty=85;}
        w.player=w.active=officer.owner;w.strategy.releaseGovernor(officer.id);officer.cityId=-1;officer.acted=false;
        officer.skillId=(tactic?Skill.SHENJIANG:Skill.SHENSUAN).id;Arrays.fill(officer.aptitude,3);
        World.Officer opponent=w.officers.stream().filter(o->o.owner>=0&&o.owner!=officer.owner&&w.campaign.hostile(officer.owner,o.owner)&&o.unitId<0).min(Comparator.comparingInt((World.Officer o)->o.war+o.intelligence)).orElseThrow(()->new AssertionError("Hostile source officer"));
        w.strategy.releaseGovernor(opponent.id);opponent.cityId=-1;opponent.acted=false;opponent.skillId="none";
        World.Unit a=new World.Unit(w.nextUnitId++,officer.owner,officer.id,World.Weapon.SPEAR,new Hex(0,0),5000,30000);
        World.Unit b=new World.Unit(w.nextUnitId++,opponent.owner,opponent.id,World.Weapon.SWORD,new Hex(0,0),5000,30000);
        w.units.add(a);w.units.add(b);officer.unitId=a.id;opponent.unitId=b.id;a.energy=100;
        if(plot==War.Plot.SORCERY||plot==War.Plot.LIGHTNING){
            World.Officer helper=w.officers.stream().filter(o->o.id!=officer.id&&o.owner==officer.owner&&o.unitId<0).findFirst().orElseThrow(()->new AssertionError("Friendly source deputy"));
            w.strategy.releaseGovernor(helper.id);helper.cityId=-1;helper.unitId=a.id;helper.skillId=Skill.GUIMEN.id;a.deputies=new int[]{helper.id};
        }
        w.strategy.setSeed(7351); // Explicit saved fixture input, never a presentation roll.
        List<Hex> cells=new ArrayList<>();
        for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){Hex h=new Hex(q,r);if(free(w,h)&&w.terrain[q][r]==World.Terrain.PLAIN)cells.add(h);}
        Hex anchor=MapCoordinates.fromNationalSource(w,new SourceGridCoord(120,100));
        cells.sort(Comparator.comparingInt((Hex h)->h.distance(anchor)).thenComparingInt(h->h.r).thenComparingInt(h->h.q));
        for(Hex h:cells){a.hex=h;for(Hex n:h.neighbors())if(free(w,n)&&w.terrain[n.q][n.r]==World.Terrain.PLAIN){
            b.hex=n;String error=tactic?w.war.tacticError(a.id,b.id,War.Tactic.THRUST):w.war.plotError(a.id,n,plot);
            if(error==null)return new Case(SaveCodec.decode(SaveCodec.encode(w)),a.id,b.id,plot,tactic?selector:plot==War.Plot.SORCERY?126:plot==War.Plot.LIGHTNING?127:-1,tactic?portraitLabel:"plot-"+plot.name().toLowerCase(java.util.Locale.ROOT));
            b.hex=new Hex(0,0);
        }}
        throw new AssertionError("No legal original terrain critical command");
    }
    private static boolean free(World w,Hex h){return w.inside(h)&&!NationalMap.restricted(w,h)&&w.cityAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&w.unitAt(h)==null&&w.events.at(h)==null;}
}
