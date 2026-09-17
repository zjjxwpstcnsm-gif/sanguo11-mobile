package game.sanguo.core;

import java.util.*;
import java.io.*;

/** Real command/serialization regressions for terrain, fixed parcels and elemental fire. */
public final class MapSkillsTest {
    private static int checks;
    private static void check(boolean yes,String label){checks++;if(!yes)throw new AssertionError(label);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static World copy(World w)throws Exception{return SaveCodec.decode(bytes(w));}
    public static World fireFixture(World.Weapon weapon){
        World w=new World(20,16);w.cities.add(new World.City(10,"甲城",new Hex(2,3),0));w.cities.add(new World.City(20,"乙城",new Hex(17,10),1));
        for(int i=0;i<6;i++){World.Officer o=new World.Officer(i,"将"+i,i<3?0:1,i<3?10:20,90,90,90,90,90);Arrays.fill(o.aptitude,3);w.officers.add(o);}
        World.Unit a=new World.Unit(1,0,0,weapon,new Hex(6,6),10000,50000),b=new World.Unit(2,1,3,World.Weapon.SPEAR,new Hex(7,6),10000,50000);
        a.deputies=new int[]{1,2};w.officer(0).unitId=1;w.officer(1).unitId=1;w.officer(2).unitId=1;w.officer(3).unitId=2;
        for(World.Officer o:w.officers)if(o.unitId>0)o.cityId=-1;
        w.units.add(a);w.units.add(b);w.nextUnitId=3;w.strategy.setSeed(88);return w;
    }
    private static void geography()throws Exception {
        World w=ScenarioCatalog.load("heroes-mobile-sandbox",1);
        int[] expected={12,15,12,15,12,12,20,12,18,10,15,15,15,18,12,22,15,22,12,10,12,15,18,15,15,15,12,12,10,18,15,12,10,12,12,12,15,10,12,18,12,15};
        Set<Hex> all=new HashSet<>();int distant=0;
        for(World.City c:w.cities){if(c.kind!=World.SiteKind.CITY)continue;check(w.development.capacity(c.id)==expected[c.id-20000],"researched city capacity "+c.name);check(w.domestic.buildSites(c.id).size()==expected[c.id-20000],"all parcels can initially build "+c.name);
            for(Hex h:w.development.parcels(c.id)){check(all.add(h),"parcel ownership unique");check(w.development.cityAt(h)==c,"tap parcel resolves correct city");if(h.distance(c.hex)>2)distant++;}
            check(c.hex.neighbors().stream().anyMatch(h->w.cost(h,World.Weapon.SPEAR)>0&&!all.contains(h)),"city has exit "+c.name);
        }
        check(distant>300,"development districts are separated from city footprint");
        check(w.sourceMapWidth>190&&w.height>190,"national map retains original coordinate scale");
        int sea=0,water=0,voids=0;for(World.Terrain[] row:w.terrain)for(World.Terrain t:row){if(t==World.Terrain.SEA)sea++;if(t==World.Terrain.WATER)water++;if(t==World.Terrain.VOID)voids++;}
        check(sea>3000&&water>1500&&voids>500,"coast, rivers and excluded theatre exist as terrain");
        // The two main rivers are continuous between separately chosen known landmarks.
        int oy=16-MapCoordinates.source(w.city(20000).hex,w.height).r;
        for(int[] pair:new int[][]{{59,51,134,61},{44,127,163,93}}){
            Hex from=MapCoordinates.axial(pair[0],pair[1]-oy,w.height),to=MapCoordinates.axial(pair[2],pair[3]-oy,w.height);
            check(w.army.water(from)&&w.army.water(to),"river landmarks water "+from+"="+w.terrain[from.q][from.r]+" "+to+"="+w.terrain[to.q][to.r]);Set<Hex> seen=new HashSet<>();ArrayDeque<Hex> q=new ArrayDeque<>();q.add(from);seen.add(from);
            while(!q.isEmpty())for(Hex h:q.remove().neighbors())if(w.army.water(h)&&seen.add(h))q.add(h);
            check(seen.contains(to),"river has no artificial land cuts");
        }
        check(Arrays.equals(bytes(w),bytes(copy(w))),"terrain and development parcels persist exactly");
        World.City c=w.city(20017);World.Officer worker=w.idle(c).get(0);List<Hex> sites=new ArrayList<>(w.domestic.buildSites(c.id));
        for(Hex h:sites){worker.acted=false;w.actionPoints[w.active]=60;c.gold=10000;check(w.domestic.build(c.id,worker.id,Domestic.Kind.MARKET,h).ok,"build through real command");for(int i=0;i<3;i++)w.domestic.tick();}
        check(w.domestic.count(c.id)==22&&w.domestic.buildSites(c.id).isEmpty(),"22 separate facilities, then capacity enforced");check(Arrays.equals(bytes(w),bytes(copy(w))),"more than six facilities save and load");
        worker.acted=false;w.actionPoints[w.active]=60;byte[] prior=bytes(w);check(!w.domestic.build(c.id,worker.id,Domestic.Kind.FARM,sites.get(0)).ok,"duplicate parcel fails");check(Arrays.equals(prior,bytes(w)),"invalid build consumes nothing");
        Domestic.Facility f=w.domestic.at(sites.get(0));check(w.domestic.demolish(f.id,worker.id).ok&&w.domestic.buildSites(c.id).contains(f.hex),"demolition reopens same parcel");
        try(InputStream in=MapSkillsTest.class.getResourceAsStream("/v21-geography.b64")){byte[] old=Base64.getDecoder().decode(in.readAllBytes());check(old[7]==21,"genuine pre-change writer fixture");World legacy=SaveCodec.decode(old);check(legacy.dataRevision==1&&legacy.sourceMapWidth<100&&legacy.development.parcels(20000).isEmpty(),"old board preserved without moving armies or facilities");check(Arrays.equals(bytes(legacy),bytes(copy(legacy))),"legacy upgrade roundtrip");}
    }
    private static void fires()throws Exception {
        World normal=fireFixture(World.Weapon.SPEAR);normal.officer(0).skillId=Skill.HUOGONG.id;normal.officer(0).intelligence=100;World god=copy(normal);god.officer(0).skillId=Skill.HUOSHEN.id;
        check(normal.war.plot(1,normal.unit(2).hex,War.Plot.FIRE).ok&&god.war.plot(1,god.unit(2).hex,War.Plot.FIRE).ok,"fire plot command accepted");
        check(normal.unit(2).troops==9600&&god.unit(2).troops==9200,"fire plot immediate x2");
        god.officer(0).skillId="none";god=copy(god);god.war.tick();check(god.unit(2).troops==8700,"caster power snapshot survives save and skill change");
        World empty=fireFixture(World.Weapon.SPEAR);empty.officer(0).skillId=Skill.HUOSHEN.id;empty.officer(0).intelligence=100;
        check(empty.war.plotChance(1,new Hex(6,7),War.Plot.FIRE)<100,"fire guarantee needs an enemy intelligence comparison");
        World arrow=fireFixture(World.Weapon.CROSSBOW);arrow.unit(2).status=War.Status.CONFUSED;arrow.unit(2).statusTurns=1;World godArrow=copy(arrow);godArrow.officer(1).skillId=Skill.HUOSHEN.id;
        check(arrow.war.tactic(1,2,War.Tactic.FIRE_ARROW).ok&&godArrow.war.tactic(1,2,War.Tactic.FIRE_ARROW).ok,"arrow commands accepted");
        check(arrow.unit(2).troops-godArrow.unit(2).troops==400,"only elemental component of arrow doubles, deputy skill active");
        World immune=fireFixture(World.Weapon.CROSSBOW);immune.unit(2).status=War.Status.CONFUSED;immune.unit(2).statusTurns=1;immune.officer(3).skillId=Skill.HUOSHEN.id;check(immune.war.tactic(1,2,War.Tactic.FIRE_ARROW).ok,"fire immune still targetable by physical arrows");check(immune.unit(2).troops<10000&&immune.unit(2).troops-arrow.unit(2).troops==400,"fire immune takes exactly physical portion");
        int after=immune.unit(2).troops;immune.war.tick();check(immune.unit(2).troops==after,"no continuing damage to fire god");
        World guard=fireFixture(World.Weapon.SPEAR);guard.officer(3).skillId=Skill.HUWEI.id;guard.fieldworks.ignite(guard.unit(2).hex,guard.unit(1));check(guard.unit(2).troops==9600,"bodyguard does not erase initial fire");guard.war.tick();check(guard.unit(2).troops==9600,"bodyguard protects against continuing field fire");
        World critical=fireFixture(World.Weapon.SPEAR);critical.officer(0).skillId=Skill.HUOGONG.id;critical.officer(0).intelligence=100;critical.officer(1).skillId=Skill.SHENMOU.id;critical.war.plot(1,critical.unit(2).hex,War.Plot.FIRE);check(critical.unit(2).troops==9600&&critical.war.fireAt(critical.unit(2).hex).remaining==3,"fire critical extends duration, not initial damage");
        for(War.StructureKind kind:War.StructureKind.values()){
            World trap=fireFixture(World.Weapon.SPEAR);if(!trap.fieldworks.trap(kind))continue;
            Hex h=new Hex(6,5);War.Structure s=new War.Structure(1,0,kind,h,kind.hp);trap.war.structures.add(s);trap.war.nextStructureId=2;
            if(kind==War.StructureKind.FIRE_SHIP)for(World.Terrain[] row:trap.terrain)Arrays.fill(row,World.Terrain.WATER);
            // Fixture cities remain land-valid even when testing a naval trap.
            for(World.City c:trap.cities)trap.terrain[c.hex.q][c.hex.r]=World.Terrain.PLAIN;
            trap.unit(2).hex=new Hex(7,5);
            World powered=copy(trap);powered.officer(1).skillId=Skill.HUOSHEN.id;
            trap.fieldworks.ignite(h,trap.unit(1));powered.fieldworks.ignite(h,powered.unit(1));
            int ordinary=10000-trap.unit(2).troops,doubled=10000-powered.unit(2).troops;check(ordinary>0&&doubled==ordinary*2,"trap uses igniter skill exactly once "+kind);
            World armor=fireFixture(World.Weapon.SPEAR);armor.officer(3).skillId=Skill.TENGJIA.id;check(armor.combat.fireDamage(armor.unit(2),700,0,2,true)==2800,"vine armor stacks vulnerability with source power");armor.unit(2).deputies=new int[]{4};armor.officer(4).skillId=Skill.TAPO.id;check(armor.combat.fireDamage(armor.unit(2),700,0,2,true)==1400&&armor.combat.fireDamage(armor.unit(2),400,0,2,false)==1600,"tapo halves traps only");
        }
        for(World.Weapon weapon:new World.Weapon[]{World.Weapon.SPEAR,World.Weapon.SIEGE_TOWER,World.Weapon.WOODEN_BEAST}){
            World engine=fireFixture(weapon);engine.unit(2).status=War.Status.CONFUSED;engine.unit(2).statusTurns=1;
            if(weapon==World.Weapon.SPEAR){engine.terrain[6][6]=World.Terrain.WATER;engine.terrain[7][6]=World.Terrain.WATER;engine.unit(1).ship=Army.Ship.TOWER_SHIP;}
            World powered=copy(engine);powered.officer(1).skillId=Skill.HUOSHEN.id;Army.Tactic t=weapon==World.Weapon.WOODEN_BEAST?Army.Tactic.FLAME:Army.Tactic.FIRE_ARROW;
            check(engine.army.tactic(1,engine.unit(2).hex,t).ok&&powered.army.tactic(1,powered.unit(2).hex,t).ok,"engine/naval fire issued "+weapon);
            check(engine.unit(2).troops-powered.unit(2).troops==400,"engine/naval element doubles independently "+weapon);
            int hp=powered.unit(2).troops;powered=copy(powered);powered.army.tick();check(powered.unit(2).troops==hp-400,"saved onboard burn retains firegod power "+weapon);
        }
        World stun=fireFixture(World.Weapon.SPEAR);stun.officer(3).skillId=Skill.HUOSHEN.id;stun.unit(2).hex=new Hex(7,5);
        stun.war.structures.add(new War.Structure(1,0,War.StructureKind.INFERNO_SEED,new Hex(6,5),War.StructureKind.INFERNO_SEED.hp));stun.war.nextStructureId=2;
        stun.fieldworks.ignite(new Hex(6,5),stun.unit(1));check(stun.unit(2).troops==10000&&stun.unit(2).status==War.Status.CONFUSED,"fire immunity does not erase inferno explosion confusion");
        World lethal=fireFixture(World.Weapon.SPEAR);lethal.officer(0).skillId=Skill.HUOSHEN.id;lethal.officer(0).intelligence=100;lethal.unit(2).troops=500;lethal.unit(2).gold=123;lethal.war.plot(1,lethal.unit(2).hex,War.Plot.FIRE);check(lethal.unit(2)==null&&lethal.unit(1).gold==123,"instant fire kill uses normal defeat and loot once");check(Arrays.equals(bytes(lethal),bytes(copy(lethal))),"fire defeat save valid");
    }
    public static void main(String[] args)throws Exception{geography();fires();System.out.println("PASS: "+checks+" v030 map/parcel/fire assertions.");}
}
