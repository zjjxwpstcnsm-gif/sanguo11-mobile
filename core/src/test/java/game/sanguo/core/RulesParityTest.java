package game.sanguo.core;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;
import static game.sanguo.core.Skill.*;

/** Command atomicity, skill interactions and real v6 migration through the actual campaign world. */
public final class RulesParityTest {
    private static int checks;
    private static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static void reject(World w,Supplier<World.Result> command)throws Exception {
        byte[] before=bytes(w);check(!command.get().ok,"reject illegal/replayed command");check(Arrays.equals(before,bytes(w)),"rejection preserves complete state and RNG");
    }
    private static World fixture(){
        World w=new World(22,16,"甲军","乙军","丙军");
        for(int side=0;side<3;side++){
            int city=10+side;w.cities.add(new World.City(city,"城"+side,new Hex(2+side*8,2),side));
            w.city(city).gold=50000;w.city(city).food=200000;w.city(city).troops=30000;
            for(int i=0;i<8;i++)w.officers.add(new World.Officer(side*10+i,"将"+(side*10+i),side,city,80,80,80,80,80));
        }
        w.strategy.initializeOffices();return w;
    }
    private static World.Unit unit(World w,int officer,int q,int r,World.Weapon weapon){
        World.Officer o=w.officer(officer);w.strategy.releaseGovernor(officer);
        World.Unit u=new World.Unit(w.nextUnitId++,o.owner,o.id,weapon,new Hex(q,r),5000,20000);
        o.cityId=-1;o.unitId=u.id;w.units.add(u);Arrays.fill(o.aptitude,3);return u;
    }
    private static void deputy(World w,World.Unit u,int id,Skill s){
        World.Officer o=w.officer(id);o.cityId=-1;o.unitId=u.id;o.skillId=s.id;
        int[] ids=Arrays.copyOf(u.deputies,u.deputies.length+1);ids[ids.length-1]=id;u.deputies=ids;
    }
    public static void main(String[] args)throws Exception {
        actions();movementBudget();skills();combat();fireSources();economy();migration();simulation();
        System.out.println("PASS: "+checks+" rules-parity assertions: move previews/replays/budget, skill priorities/holders/chains, actual damage/production, real v6 migration and deterministic campaigns.");
    }
    private static void actions()throws Exception {
        World w=fixture();World.Unit a=unit(w,0,5,6,World.Weapon.SPEAR),b=unit(w,10,7,6,World.Weapon.HALBERD);
        byte[] initial=bytes(w);UnitOrders.MovePlan plan=w.orders.previewMove(a.id,new Hex(6,6));
        check(plan.valid()&&plan.cost==4&&plan.path.size()==2,"path includes origin and target with exact cost");
        for(int i=0;i<10;i++){w.orders.previewMove(a.id,new Hex(6,6));w.war.previewDamage(a.id,b.id);w.war.plotChance(a.id,b.hex,War.Plot.CONFUSE);}
        check(Arrays.equals(initial,bytes(w)),"preview and cancellation change no state");
        ok(w.orders.execute(plan));check(!a.acted&&a.movementSpent==4,"move leaves action available");
        reject(w,()->w.orders.execute(plan));reject(w,()->w.move(a.id,b.hex));
        World loaded=SaveCodec.decode(bytes(w));ok(w.attack(a.id,b.id));ok(loaded.attack(a.id,b.id));
        check(Arrays.equals(bytes(w),bytes(loaded)),"save after move resumes identical attack including counter and RNG");
        reject(w,()->w.attack(a.id,b.id));reject(w,()->w.move(a.id,new Hex(5,6)));
        World stale=fixture();World.Unit u=unit(stale,0,5,6,World.Weapon.SPEAR);UnitOrders.MovePlan old=stale.orders.previewMove(u.id,new Hex(6,6));stale.strategy.setSeed(123);
        reject(stale,()->stale.orders.execute(old));
        World returning=fixture();ok(returning.army.deploy(10,0,new int[]{1,2},World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000));World.Unit moving=returning.unit(1);
        Hex home=returning.city(10).hex;Hex step=null;for(Hex h:moving.hex.neighbors())if(h.distance(home)==1&&returning.reachable(moving).containsKey(h)){step=h;break;}
        check(step!=null,"adjacent alternative exists");ok(returning.move(1,step));ok(returning.enter(1,10));
        check(returning.city(10).troops==30000&&returning.officer(1).cityId==10,"move then enter recovers all crew and resources");reject(returning,()->returning.enter(1,10));
    }
    private static void movementBudget()throws Exception {
        World w=fixture();World.Unit u=unit(w,0,5,6,World.Weapon.SPEAR);u.ship=Army.Ship.WARSHIP;
        for(int q=6;q<10;q++)w.terrain[q][6]=World.Terrain.WATER;
        int budget=w.war.movement(u);ok(w.move(u.id,new Hex(6,6)));check(u.movementBudget==budget&&w.orders.remaining(u)==budget-2,"embark freezes original movement budget");
        World loaded=SaveCodec.decode(bytes(w));ok(loaded.move(u.id,new Hex(8,6)));check(loaded.orders.remaining(loaded.unit(u.id))==0,"split moves spend one finite budget");
        reject(loaded,()->loaded.move(1,new Hex(9,6)));ok(loaded.war.waitUnit(u.id));reject(loaded,()->loaded.war.waitUnit(1));
        w=fixture();u=unit(w,0,5,6,World.Weapon.CAVALRY);w.terrain[6][6]=World.Terrain.FOREST;
        check(w.orders.previewMove(u.id,new Hex(6,6)).cost==3,"forest cost comes from shared core movement");
    }
    private static void skills()throws Exception {
        check(Skill.values().length==100,"complete stock catalog count, independent of implementation coverage");Set<String> ids=new HashSet<>();for(Skill s:Skill.values())check(ids.add(s.id)&&Skill.find(s.id)==s,"stable distinct skill "+s.id);
        World w=fixture();World.Unit a=unit(w,0,5,6,World.Weapon.SPEAR),b=unit(w,10,6,6,World.Weapon.HALBERD),c=unit(w,20,6,7,World.Weapon.SPEAR);
        w.officer(0).skillId=SHENSUAN.id;w.officer(0).intelligence=99;deputy(w,a,1,BAICHU);deputy(w,a,2,LIANHUAN);w.officer(20).skillId=DONGCHA.id;
        check(w.war.plotCost(a.id,War.Plot.CONFUSE)==1&&w.war.plotChance(a.id,b.hex,War.Plot.CONFUSE)==100,"神算百出 combines holder and cost rules");
        World copy=SaveCodec.decode(bytes(w));ok(w.war.plot(a.id,b.hex,War.Plot.CONFUSE));ok(copy.war.plot(a.id,b.hex,War.Plot.CONFUSE));
        check(a.energy==79&&a.acted&&b.statusTurns==2&&c.status==War.Status.NORMAL,"chain rechecks immunity, costs exactly once");check(Arrays.equals(bytes(w),bytes(copy)),"skill sequence identical after save");
        World finished=w;reject(finished,()->finished.war.plot(1,new Hex(6,7),War.Plot.CONFUSE));
        w=fixture();a=unit(w,0,5,6,World.Weapon.SPEAR);b=unit(w,10,6,6,World.Weapon.SPEAR);deputy(w,a,1,XUSHI);w.officer(0).intelligence=100;w.officer(1).intelligence=50;
        check(w.war.plotChance(a.id,b.hex,War.Plot.CONFUSE)<100,"weak skill holder cannot borrow commander's intelligence");
        w.officer(1).intelligence=90;check(w.war.plotChance(a.id,b.hex,War.Plot.CONFUSE)==100,"holder's own intelligence triggers");
        for(Skill defense:new Skill[]{DONGCHA,CHENZHUO,MINGJING}){w.officer(10).skillId=defense.id;check(w.war.plotChance(a.id,b.hex,War.Plot.CONFUSE)==0,"immunity before必中 "+defense);}
        w.officer(10).skillId=GUILV.id;check(w.war.plotChance(a.id,b.hex,War.Plot.MISLEAD)==0&&w.war.plotChance(a.id,b.hex,War.Plot.CONFUSE)==100,"discipline only blocks false report");
        w.officer(10).skillId="none";w.officer(1).skillId=GUIMOU.id;check(w.war.plotRange(a.id,War.Plot.CONFUSE)==3&&w.war.plotRange(a.id,War.Plot.FIRE)==2,"extended range uses plot-specific base");
        w.officer(1).skillId=QINGGUO.id;int unknown=w.war.plotChance(a.id,b.hex,War.Plot.CONFUSE);w.officer(10).sex=World.Sex.MALE;check(w.war.plotChance(a.id,b.hex,War.Plot.CONFUSE)==Math.min(100,unknown*2),"all-male requires known sex");w.officer(10).sex=World.Sex.FEMALE;check(w.war.plotChance(a.id,b.hex,War.Plot.CONFUSE)==unknown,"female blocks 倾国");
        World reflection=fixture();a=unit(reflection,0,5,6,World.Weapon.SPEAR);b=unit(reflection,10,6,6,World.Weapon.SPEAR);
        reflection.officer(0).intelligence=40;reflection.officer(10).skillId=DONGCHA.id;deputy(reflection,b,11,FANJI);deputy(reflection,b,12,SHENSUAN);reflection.officer(12).intelligence=99;
        int energy=b.energy;ok(reflection.war.plot(a.id,b.hex,War.Plot.CONFUSE));check(a.status==War.Status.CONFUSED&&b.energy==energy&&!b.acted,"reflection rolls its own rule without consuming defender action");
    }
    private static void combat()throws Exception {
        World w=fixture();World.Unit a=unit(w,0,5,6,World.Weapon.CROSSBOW),b=unit(w,10,7,6,World.Weapon.SPEAR);w.officer(0).skillId=WEIFENG.id;deputy(w,a,1,SAOTAO);
        ok(w.attack(a.id,b.id));check(b.energy==60,"威风 supersedes 扫讨, no additive -25");
        World crit=fixture();a=unit(crit,0,5,6,World.Weapon.SPEAR);b=unit(crit,10,6,6,World.Weapon.SPEAR);deputy(crit,a,1,QIANGJIANG);crit.officer(0).war=100;crit.officer(1).war=50;
        check(!crit.combat.critical(a,b,true),"combat skill holder cannot borrow war stat");crit.officer(1).war=90;check(crit.combat.critical(a,b,true)&&!crit.combat.critical(a,b,false),"general only modifies successful tactic");
        crit.officer(1).skillId=FEIJIANG.id;crit.terrain[a.hex.q][a.hex.r]=World.Terrain.WATER;check(!crit.combat.critical(a,b,true),"飞将 does not give naval critical");crit.officer(1).skillId=YONGJIANG.id;check(crit.combat.critical(a,b,true),"勇将 applies to naval units");
        World volley=fixture();a=unit(volley,0,5,6,World.Weapon.CROSSBOW);b=unit(volley,10,7,6,World.Weapon.SPEAR);World.Unit friend=unit(volley,1,7,7,World.Weapon.SPEAR);b.status=War.Status.CONFUSED;b.statusTurns=1;
        World safe=SaveCodec.decode(bytes(volley));safe.officer(0).skillId=GONGSHEN.id;
        ok(volley.war.tactic(a.id,b.id,War.Tactic.VOLLEY));ok(safe.war.tactic(a.id,b.id,War.Tactic.VOLLEY));
        check(friend.troops<5000&&safe.unit(friend.id).troops==5000,"弓神 prevents friendly volley splash");
        World shield=fixture();a=unit(shield,0,5,6,World.Weapon.CROSSBOW);b=unit(shield,10,7,6,World.Weapon.SPEAR);World armor=SaveCodec.decode(bytes(shield));armor.officer(10).skillId=TENGJIA.id;
        ok(shield.attack(a.id,b.id));ok(armor.attack(a.id,b.id));int ordinary=5000-b.troops,halved=5000-armor.unit(b.id).troops;check(halved==Math.max(1,ordinary/2),"藤甲 halves physical damage");
        World music=fixture();a=unit(music,0,5,6,World.Weapon.SPEAR);a.energy=20;music.officer(0).skillId=ZOUYUE.id;deputy(music,a,1,SHIXIANG);
        music.war.structures.add(new War.Structure(1,0,War.StructureKind.MUSIC,new Hex(4,6),500));music.war.structures.add(new War.Structure(2,0,War.StructureKind.MUSIC,new Hex(4,7),500));music.war.nextStructureId=3;
        music.war.tick();check(a.energy==40,"one music restoration, poetry doubles, no 奏乐 stacking");
    }
    private static void fireSources()throws Exception {
        World w=fixture();World.Unit a=unit(w,0,5,6,World.Weapon.SPEAR),b=unit(w,10,6,6,World.Weapon.SPEAR);
        w.officer(0).skillId=HUOSHEN.id;w.officer(0).intelligence=100;
        ok(w.war.plot(a.id,b.hex,War.Plot.FIRE));check(w.war.fireAt(b.hex).power==2,"fire records source power");
        int afterIgnition=b.troops;check(afterIgnition==4200,"fire plot applies immediate doubled damage");w.officer(0).skillId="none";World resumed=SaveCodec.decode(bytes(w));w.war.tick();resumed.war.tick();
        check(b.troops==afterIgnition-500&&Arrays.equals(bytes(w),bytes(resumed)),"source effect persists after caster changes and save/load");
        World armor=fixture();World.Unit victim=unit(armor,10,6,6,World.Weapon.SPEAR);armor.officer(10).skillId=TENGJIA.id;
        War.Fire fire=new War.Fire(victim.hex,0,2);fire.trap=true;armor.war.fires.add(fire);deputy(armor,victim,11,TAPO);
        armor.war.tick();check(victim.troops==4750,"藤甲 x2 then 踏破 trap half");deputy(armor,victim,12,HUOSHEN);
        armor.war.tick();check(victim.troops==4750,"PC 火神 immune before vulnerability");
        victim.burning=2;victim.burningOwner=0;victim.burningPower=2;armor=SaveCodec.decode(bytes(armor));armor.army.tick();
        check(armor.unit(victim.id).troops==4750,"火神 also protects persisted arrow burning");
        World lethal=fixture();a=unit(lethal,0,5,6,World.Weapon.CROSSBOW);b=unit(lethal,10,7,6,World.Weapon.SPEAR);lethal.officer(10).skillId=TENGJIA.id;b.troops=10;
        ok(lethal.attack(a.id,b.id));check(lethal.unit(b.id)==null,"armor applies before remaining-troop cap, cannot prevent lethal damage by halving that cap");
        World siege=fixture();World.Unit ram=unit(siege,0,5,6,World.Weapon.RAM);War.Structure tower=new War.Structure(1,1,War.StructureKind.ARROW_TOWER,new Hex(6,6),650);siege.war.structures.add(tower);siege.war.nextStructureId=2;
        ram.energy=0;reject(siege,()->siege.war.attackStructure(ram.id,tower.hex));ram.energy=80;ok(siege.war.attackStructure(ram.id,tower.hex));
        check(ram.energy==80-Army.Tactic.RAM.energy&&ram.acted&&siege.war.at(tower.hex)==null,"structure assault resolves one siege tactic and cost");reject(siege,()->siege.war.attackStructure(ram.id,tower.hex));
        World swift=fixture();a=unit(swift,0,5,6,World.Weapon.CAVALRY);b=unit(swift,10,6,6,World.Weapon.SPEAR);deputy(swift,a,1,JICHI);swift.officer(1).war=1;swift.officer(0).war=100;
        check(swift.skills.swiftConfusion(a,b),"疾驰 compares formation attack, not low-war skill holder");
        swift.officer(0).war=1;swift.officer(0).leadership=1;check(!swift.skills.swiftConfusion(a,b),"疾驰 fails against greater unit attack");
    }
    private static void economy()throws Exception {
        World w=fixture();w.officer(1).skillId=NENGLI.id;int before=w.city(10).equipment[0];int quote=w.skills.produceAmount(10,1,World.Weapon.SPEAR);
        int goldBefore=w.city(10).gold;
        ok(w.produce(10,1,World.Weapon.SPEAR));check(w.city(10).equipment[0]==before+quote&&quote==w.domestic.produceAmount(10,World.Weapon.SPEAR)&&w.city(10).gold==goldBefore-350,"能吏 reduces cost without doubling output");
        check(w.skills.productionGold(1,World.Weapon.CAVALRY)==700,"能吏 does not reduce horse cost");
        w.officer(4).skillId=FANZHI.id;check(w.skills.productionGold(4,World.Weapon.CAVALRY)==350&&w.skills.productionGold(4,World.Weapon.SPEAR)==700,"繁殖 affects only horses");
        World affordable=fixture();affordable.officer(1).skillId=NENGLI.id;affordable.city(10).gold=350;ok(affordable.produce(10,1,World.Weapon.SPEAR));check(affordable.city(10).gold==0,"discount is applied before affordability validation");
        World broke=fixture();broke.officer(1).skillId=FANZHI.id;broke.city(10).gold=349;reject(broke,()->broke.produce(10,1,World.Weapon.CAVALRY));
        check(Army.productionGold(World.Weapon.SPEAR)==700&&Army.productionGold(World.Weapon.HALBERD)==700&&Army.productionGold(World.Weapon.CROSSBOW)==700&&Army.productionGold(World.Weapon.CAVALRY)==700,"four listed base weapon costs");
        check(Army.productionGold(World.Weapon.RAM)==1500&&Army.productionGold(World.Weapon.SIEGE_TOWER)==1600&&Army.productionGold(World.Weapon.WOODEN_BEAST)==1700&&Army.productionGold(World.Weapon.CATAPULT)==1800&&Army.Ship.TOWER_SHIP.gold==1800&&Army.Ship.WARSHIP.gold==2000,"listed siege and ship costs");
        w.officer(2).skillId=MINGSHENG.id;w.city(10).recruitReserve=50;int troops=w.city(10).troops;ok(w.recruit(10,2));check(w.city(10).troops==troops+50&&w.city(10).recruitReserve==0,"名声 cannot create unavailable recruits");
        w.officer(3).skillId=ZHIDAO.id;w.campaign.points.put(0,5000);int gold=w.city(10).gold;
        ok(w.campaign.research(10,3,Campaign.Tech.SPEAR_DRILL));check(w.city(10).gold==gold-Campaign.Tech.SPEAR_DRILL.gold/2,"指导 research gold discount, not points/time");
        ok(w.campaign.cancelProject(3));check(w.officer(3).otherTaskTurns==0&&w.officer(3).acted&&!w.campaign.has(0,Campaign.Tech.SPEAR_DRILL),"cancel releases task without granting technique");reject(w,()->w.campaign.cancelProject(3));
        World manufacture=fixture();manufacture.officer(1).skillId=FAMING.id;
        manufacture.domestic.facilities.add(new Domestic.Facility(1,10,Domestic.Kind.WORKSHOP,new Hex(3,2),-1,0));manufacture.domestic.nextFacilityId=2;
        ok(manufacture.army.produce(10,1,World.Weapon.RAM,null));check(manufacture.officer(1).otherTaskTurns==2,"invention rounds half duration up to a turn");
        int gear=manufacture.city(10).equipment[5];for(int i=0;i<2;i++){manufacture=SaveCodec.decode(bytes(manufacture));manufacture.army.tick();manufacture.strategy.tick();}
        check(manufacture.city(10).equipment[5]==gear+1&&manufacture.army.productions().isEmpty(),"discounted production uses same once-only task clock");
    }
    private static void migration()throws Exception {
        byte[] old=Base64.getMimeDecoder().decode(RulesParityTest.class.getResourceAsStream("/legacy-v6.sg11.b64").readAllBytes());check(ByteBuffer.wrap(old).getInt(4)==6,"fixture is truly v6");
        World w=SaveCodec.decode(old);check(w.officers.size()==9&&w.units.size()==1&&w.cities.size()==3,"migration injects no content");
        check(w.army.productions().size()==1&&w.campaign.projects().size()==2&&w.domestic.missions.size()==1,"all task types retained");
        World.Unit u=w.unit(1);check(u.deputies.length==2&&u.ship==Army.Ship.WARSHIP&&u.burning==2&&u.statusTurns==1&&u.movementBudget==-1,"crew/ship/combat unchanged, only movement initialized");
        check(w.strategy.getRandomState()==0x66778899L,"old RNG preserved");
        w.officer(7).skillId="future.pack-skill";w.scenarioId="removed-content";
        World loaded=SaveCodec.decode(bytes(w));check(loaded.officer(7).skillId.equals("future.pack-skill")&&loaded.scenarioId.equals("removed-content"),"unknown static IDs survive roundtrip without pack");
        for(int i=0;i<4;i++){ok(w.nextTurn());ok(loaded.nextTurn());check(Arrays.equals(bytes(w),bytes(loaded)),"v6 task continuation identical at turn "+i);}
        check(w.army.productions().isEmpty()&&w.campaign.projects().isEmpty(),"legacy tasks settle, do not restart");
        World bad=fixture();unit(bad,0,5,6,World.Weapon.SPEAR).movementSpent=100;
        try{SaveCodec.encode(bad);throw new AssertionError("invalid budget accepted");}catch(java.io.IOException expected){checks++;}
    }
    private static void simulation()throws Exception {
        Skill[] available={SHENSUAN,BAICHU,LIANZHAN,QIANGSHEN,WEIFENG,MINGJING,TENGJIA,RENZHENG};
        for(int player=0;player<3;player++){
            World w=TestScenarios.load("regional-sandbox",player);for(World.Officer o:w.officers)o.skillId=available[o.id%available.length].id;
            World copy=SaveCodec.decode(bytes(w));
            for(int turn=0;turn<60&&!w.gameOver();turn++){
                check(w.nextTurn().ok==copy.nextTurn().ok,"same AI command result");check(Arrays.equals(bytes(w),bytes(copy)),"deterministic multi-faction skill campaign");copy=SaveCodec.decode(bytes(copy));
            }
        }
    }
}
