package game.sanguo.core;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

public final class WorldSystemsTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static World copy(World w)throws Exception{return SaveCodec.decode(bytes(w));}
    private static void reject(World w,Supplier<World.Result> command)throws Exception{byte[] b=bytes(w);check(!command.get().ok,"rejected");check(Arrays.equals(b,bytes(w)),"rejection is atomic");}
    private static World fixture(){
        World w=new World(32,22,"甲军","乙军","丙军");
        w.cities.add(new World.City(10,"甲都",new Hex(2,2),0));w.cities.add(new World.City(11,"后方",new Hex(2,17),0));w.cities.add(new World.City(12,"南境",new Hex(13,17),0));
        w.cities.add(new World.City(20,"乙都",new Hex(28,2),1));w.cities.add(new World.City(30,"丙都",new Hex(28,17),2));
        for(World.City c:w.cities){c.gold=50000;c.food=200000;c.troops=30000;c.order=90;c.morale=90;}
        for(int id=0;id<12;id++)w.officers.add(new World.Officer(id,"甲将"+id,0,id<4?10:id<8?11:12,80,80,80,80,80));
        for(int id=20;id<25;id++)w.officers.add(new World.Officer(id,"乙将"+id,1,20,80,80,60,60,60));
        w.officers.add(new World.Officer(30,"丙将",2,30,70,70,70,70,70));w.strategy.initializeOffices();return w;
    }
    private static World.Unit unit(World w,int officer,World.Weapon weapon,int q,int r){
        World.Officer o=w.officer(officer);w.strategy.releaseGovernor(officer);World.Unit u=new World.Unit(w.nextUnitId++,o.owner,officer,weapon,new Hex(q,r),6000,60000);u.energy=100;o.cityId=-1;o.unitId=u.id;w.units.add(u);return u;
    }
    private static void seed(World w,int chance,boolean success){for(long seed=0;seed<10000;seed++){w.strategy.setSeed(seed);boolean roll=w.strategy.nextInt(100)<chance;if(roll==success){w.strategy.setSeed(seed);return;}}throw new AssertionError("seed");}
    private static void reset(World w){Arrays.fill(w.actionPoints,60);for(World.Officer o:w.officers)o.acted=false;for(World.Unit u:w.units)w.orders.reset(u);}
    public static void main(String[] args)throws Exception{
        movement();marchIntegration();joint();plots();diplomacy();events();facilityLoss();districts();migration();branchMigration();malformed();replay();
        System.out.println("PASS: "+checks+" world-system assertions: ZOC/poison, joint/ironwall/infighting/magic, diplomatic debates, disasters/raiders, district budgets/orders/capture, v12/v13 branch migration, integrated marches and saved replay.");
    }
    private static void movement()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,World.Weapon.SPEAR,5,6),b=unit(w,21,World.Weapon.SPEAR,7,6);
        UnitOrders.MovePlan p=w.orders.previewMove(a.id,new Hex(6,6));check(p.valid()&&p.cost==4&&p.remaining==0,"entering hostile ZOC consumes remaining movement");
        byte[] before=bytes(w);w.orders.previewMove(a.id,new Hex(6,6));check(Arrays.equals(before,bytes(w)),"ZOC preview is pure");
        w.officer(1).skillId=Skill.DUNZOU.id;p=w.orders.previewMove(a.id,new Hex(6,6));check(p.cost==1&&p.remaining==3,"dunzou ignores land ZOC");
        ok(w.orders.execute(p));check(!a.acted,"moving still permits action");ok(w.attack(a.id,b.id));
        World water=fixture();World.Unit ship=unit(water,1,World.Weapon.SPEAR,5,6);unit(water,21,World.Weapon.SPEAR,7,6);for(int q=5;q<=7;q++)water.terrain[q][6]=World.Terrain.WATER;
        check(water.orders.previewMove(ship.id,new Hex(6,6)).cost==water.war.movement(ship),"water ZOC");water.officer(1).skillId=Skill.TUIJIN.id;check(water.orders.previewMove(ship.id,new Hex(6,6)).cost==1,"tuijin ignores water ZOC");water.officer(1).skillId=Skill.DUNZOU.id;check(water.orders.previewMove(ship.id,new Hex(6,6)).remaining==0,"land skill does not ignore water ZOC");
        World engine=fixture();World.Unit ram=unit(engine,1,World.Weapon.RAM,5,6);unit(engine,21,World.Weapon.SPEAR,7,6);engine.officer(1).skillId=Skill.FEIJIANG.id;check(engine.orders.previewMove(ram.id,new Hex(6,6)).remaining==0,"siege does not inherit land escape");
        World poison=fixture();World.Unit traveler=unit(poison,1,World.Weapon.SPEAR,5,6);poison.terrain[6][6]=World.Terrain.POISON;ok(poison.move(traveler.id,new Hex(6,6)));check(traveler.troops==5700,"poison on traversal");poison.turn++;poison.events.tick();check(traveler.troops==5415,"poison on residence");
        World protectedWorld=fixture();World.Unit immune=unit(protectedWorld,1,World.Weapon.SPEAR,5,6);protectedWorld.officer(1).skillId=Skill.JIEDU.id;protectedWorld.terrain[6][6]=World.Terrain.POISON;ok(protectedWorld.move(immune.id,new Hex(6,6)));protectedWorld.turn++;protectedWorld.events.tick();check(immune.troops==6000,"detox protects movement and residence");
        World peace=fixture();World.Unit ally=unit(peace,1,World.Weapon.SPEAR,5,6);unit(peace,21,World.Weapon.SPEAR,7,6);peace.campaign.treaties.add(new Campaign.Treaty(0,1,Campaign.TreatyKind.ALLIANCE,10));check(peace.orders.previewMove(ally.id,new Hex(6,6)).cost==1,"treaty disables hostile ZOC");
    }
    private static void marchIntegration()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,World.Weapon.SPEAR,5,6);unit(w,21,World.Weapon.SPEAR,7,5);
        MarchOrders.Plan p=w.marches.preview(a.id,new Hex(11,6));check(p.valid()&&p.stepsNow==1&&p.estimatedTurns>=2,"march forecast stops on hostile ZOC");
        ok(w.marches.execute(p));check(a.hex.equals(new Hex(6,6))&&w.orders.remaining(a)==0&&a.march!=null,"march cannot cross ZOC in one turn");
        World saved=copy(w);w.orders.reset(a);saved.orders.reset(saved.unit(a.id));w.marches.advanceAll();saved.marches.advanceAll();check(Arrays.equals(bytes(w),bytes(saved)),"ZOC march resumes deterministically");
        World escape=fixture();World.Unit e=unit(escape,1,World.Weapon.SPEAR,5,6);unit(escape,21,World.Weapon.SPEAR,7,5);escape.officer(1).skillId=Skill.DUNZOU.id;
        p=escape.marches.preview(e.id,new Hex(11,6));check(p.stepsNow==4,"escape skill is respected by march forecast");ok(escape.marches.execute(p));check(e.hex.equals(new Hex(9,6)),"escape march uses actual four-point allowance");
        World group=fixture();ok(group.districts.configure(-1,"联调军",new int[]{11},Districts.Policy.CITY_ATTACK,20,-1,true,true));group.turn=1;reset(group);group.districts.reset(0);group.districts.run();
        World.Unit delegated=group.units.stream().filter(u->group.districts.unit(u.id)!=null).findFirst().get();check(!group.marches.preview(delegated.id,new Hex(15,15)).valid(),"delegated unit rejects manual march");reject(group,()->group.marches.stop(delegated.id));
    }
    private static void joint()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,World.Weapon.SPEAR,6,6),helper=unit(w,2,World.Weapon.HALBERD,7,5),enemy=unit(w,21,World.Weapon.SPEAR,7,6);
        World iron=copy(w);iron.officer(21).skillId=Skill.TIEBI.id;ok(iron.advancedBattle.joint(a.id,enemy.id));check(iron.unit(a.id).acted&&!iron.unit(helper.id).acted,"ironwall only spends initiating unit action");
        ok(w.advancedBattle.joint(a.id,enemy.id));check(a.acted&&helper.acted&&enemy.troops<6000,"joint consumes all participants and deals damage");reject(w,()->w.advancedBattle.joint(a.id,enemy.id));
        boolean confused=false,normal=false;for(int seed=0;seed<40;seed++){World p=fixture();World.Unit first=unit(p,1,World.Weapon.SPEAR,6,6),second=unit(p,2,World.Weapon.HALBERD,7,5),target=unit(p,21,World.Weapon.SPEAR,7,6);p.officer(2).skillId=Skill.JIJIAO.id;p.strategy.setSeed(seed);ok(p.advancedBattle.joint(first.id,target.id));confused|=target.status==War.Status.CONFUSED;normal|=target.status==War.Status.NORMAL;bytes(p);}
        check(confused&&normal,"flanking skill can trigger without being guaranteed");
    }
    private static void plots()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,World.Weapon.SPEAR,5,6),enemy=unit(w,21,World.Weapon.SPEAR,7,6),other=unit(w,22,World.Weapon.SPEAR,8,6);
        w.officer(1).skillId=Skill.GUIJI.id;w.officer(1).intelligence=90;check(w.war.plotChance(a.id,enemy.hex,War.Plot.INFIGHT)==100,"guiji guarantees against lower intelligence");
        ok(w.war.plot(a.id,enemy.hex,War.Plot.INFIGHT));check(enemy.troops<6000&&other.troops<6000&&a.energy==80,"infighting damages both enemy units with quoted cost");
        World no=fixture();World.Unit caster=unit(no,1,World.Weapon.SPEAR,5,6),alone=unit(no,21,World.Weapon.SPEAR,7,6);reject(no,()->no.war.plot(caster.id,alone.hex,War.Plot.INFIGHT));reject(no,()->no.war.plot(caster.id,alone.hex,War.Plot.LIGHTNING));
        World magic=fixture();World.Unit mage=unit(magic,1,World.Weapon.SPEAR,5,6),victim=unit(magic,21,World.Weapon.SPEAR,7,6),friend=unit(magic,2,World.Weapon.SPEAR,6,7),neutral=unit(magic,30,World.Weapon.SPEAR,8,6);
        magic.officer(1).skillId=Skill.GUIMEN.id;magic.officer(3).skillId=Skill.BAICHU.id;magic.officer(3).cityId=-1;magic.officer(3).unitId=mage.id;mage.deputies=new int[]{3};magic.campaign.treaties.add(new Campaign.Treaty(0,2,Campaign.TreatyKind.ALLIANCE,10));
        check(magic.war.plotCost(mage.id,War.Plot.LIGHTNING)==1,"baichu makes lightning cost one");seed(magic,magic.war.plotChance(mage.id,victim.hex,War.Plot.LIGHTNING),true);
        ok(magic.war.plot(mage.id,victim.hex,War.Plot.LIGHTNING));check(victim.troops<6000&&friend.troops<6000&&neutral.troops==6000,"lightning hits own nearby troops but respects treaty");check(!magic.war.fires().isEmpty()&&mage.energy==99,"lightning creates persistent fire");check(Arrays.equals(bytes(magic),bytes(copy(magic))),"AOE and fire save roundtrip");
        World sorcery=fixture();World.Unit s=unit(sorcery,1,World.Weapon.SPEAR,5,6),t=unit(sorcery,21,World.Weapon.SPEAR,7,6),neighbor=unit(sorcery,22,World.Weapon.SPEAR,8,6);sorcery.officer(1).skillId=Skill.YAOSHU.id;sorcery.officer(22).skillId=Skill.DONGCHA.id;seed(sorcery,sorcery.war.plotChance(s.id,t.hex,War.Plot.SORCERY),true);ok(sorcery.war.plot(s.id,t.hex,War.Plot.SORCERY));check(t.status!=War.Status.NORMAL&&neighbor.status==War.Status.NORMAL,"sorcery status and neighboring immunity");
        sorcery.orders.reset(s);s.energy=100;reject(sorcery,()->sorcery.war.plot(s.id,t.hex,War.Plot.LIGHTNING));
    }
    private static World foreign()throws Exception{
        World w=fixture();World.Officer actor=w.officer(1);actor.skillId=Skill.LUNKE.id;actor.politics=0;actor.intelligence=100;w.contests.configure(1,new Contests.Profile(Debate.Temper.CALM,31,0));
        seed(w,w.campaign.treatyChance(1,1,Campaign.TreatyKind.CEASEFIRE),false);ok(w.campaign.negotiate(10,1,1,Campaign.TreatyKind.CEASEFIRE,6));TravelChecks.arrive(w);return w;
    }
    private static void diplomacy()throws Exception{
        World w=foreign();check(w.contests.busy()&&w.contests.current().diplomatic()&&w.city(10).gold==49000&&w.actionPoints[0]==50,"failed negotiation invokes paid diplomatic debate");
        World pair=copy(w);check(Arrays.equals(bytes(w),bytes(pair)),"diplomatic purpose survives save");reject(w,w::nextTurn);
        ok(w.contests.concede(w.contests.current().id(),w.contests.current().revision()));check(w.campaign.treaty(0,1)==null&&w.city(10).gold==49000,"loss does not create treaty or refund cost");
        int wins=0;
        for(int game=0;game<8;game++){
            World playing=foreign();playing.strategy.setSeed(game+800);World saved=copy(playing);
            while(playing.contests.current().debate().winner()==-2){
                Contests.Session s=playing.contests.current();int index=0;while(s.debate().cardError(index)!=null)index++;
                ok(playing.contests.debateCard(s.id(),s.revision(),index));Contests.Session other=saved.contests.current();ok(saved.contests.debateCard(other.id(),other.revision(),index));check(Arrays.equals(bytes(playing),bytes(saved)),"diplomatic per-card replay");saved=copy(saved);
            }
            Contests.Session session=playing.contests.current();boolean won=session.debate().winner()==0;int representative=session.rightRef;
            ok(playing.contests.finishDebate(session.id(),session.revision(),true));check((playing.campaign.treaty(0,1)!=null)==won,"only victory concludes treaty");check(playing.officer(representative).owner==1,"foreign representative is never recruited");
            if(won){wins++;check(playing.campaign.treaty(0,1).expires==6&&!playing.campaign.hostile(0,1),"actual hostility and duration change");}
            reject(playing,()->playing.contests.finishDebate(session.id(),session.revision(),true));
        }
        check(wins>0,"full diplomatic card play wins at least one treaty");
    }
    private static void events()throws Exception{
        for(WorldEvents.Tribe tribe:WorldEvents.Tribe.values()){
            World w=fixture();w.city(11).order=40;w.events.configureRegion(11,tribe);int chance=w.events.raidChance(11,tribe);check(chance>0,"low order creates risk");
            if(tribe.protection!=null){w.officer(4).skillId=tribe.protection.id;check(w.events.raidChance(11,tribe)==0&&w.events.raidChance(11,WorldEvents.Tribe.BANDIT)>0,"kinship prevents own tribe only");w.officer(4).skillId="none";}
            check(w.events.spawn(11,tribe),"actual persistent camp");WorldEvents.Camp camp=w.events.camps().get(0);check(w.cost(camp.hex,World.Weapon.SPEAR)<0,"camp blocks movement/building");
            World.Unit army=null;for(Hex h:camp.hex.neighbors())if(w.inside(h)&&w.cost(h,World.Weapon.SPEAR)>0&&w.cityAt(h)==null){army=unit(w,1,World.Weapon.SPEAR,h.q,h.r);break;}
            check(army!=null,"attacking fixture");World.Unit force=army;while(w.events.camp(camp.id)!=null){w.orders.reset(force);ok(w.events.attack(force.id,camp.id));}check(force.gold==500,"destroying camp gives cargo reward once");reject(w,()->w.events.attack(force.id,camp.id));
        }
        World w=fixture();w.city(11).order=65;w.officer(4).skillId=Skill.WEIYA.id;check(w.events.raidChance(11,WorldEvents.Tribe.BANDIT)==0,"intimidation lowers safe-order threshold");
        w.officer(4).skillId=Skill.FENGSHUI.id;check(!w.events.beginDisaster(11,WorldEvents.Disaster.PLAGUE),"fengshui prevents onset");w.officer(4).skillId="none";check(w.events.beginDisaster(11,WorldEvents.Disaster.PLAGUE),"plague starts");w.officer(4).skillId=Skill.FENGSHUI.id;int troops=w.city(11).troops;w.turn=1;w.events.tick();check(w.city(11).troops<troops,"later fengshui does not cure active disaster");byte[] before=bytes(w);w.events.tick();check(Arrays.equals(before,bytes(w)),"events cannot tick twice same turn");
        w.turn=3;w.events.tick();check(w.events.hazards().isEmpty(),"disaster expires naturally");w.officer(8).skillId=Skill.QIYUAN.id;check(w.events.harvestChance(12)>w.events.harvestChance(11),"prayer raises harvest chance");
        World locust=fixture();check(locust.events.beginDisaster(11,WorldEvents.Disaster.LOCUST),"locust starts");int food=locust.city(11).food;locust.turn=1;locust.events.tick();check(locust.city(11).food==food*9/10,"locust affects real stock");
        World raids=fixture();raids.city(11).order=30;check(raids.events.spawn(11,WorldEvents.Tribe.BANDIT),"raid fixture");raids.turn=3;food=raids.city(11).food;raids.events.tick();check(raids.city(11).food==food-1000&&raids.city(11).order==25,"camp monthly raid reaches city resources");check(Arrays.equals(bytes(raids),bytes(copy(raids))),"camp RNG and strength roundtrip");
    }
    private static World producing(Domestic.Kind kind)throws Exception{
        World w=fixture();if(kind==Domestic.Kind.SHIPYARD)w.terrain[3][18]=World.Terrain.WATER;
        w.domestic.facilities.add(new Domestic.Facility(w.domestic.nextFacilityId++,11,kind,new Hex(3,17),-1,0));
        ok(w.army.produce(11,5,kind==Domestic.Kind.WORKSHOP?World.Weapon.RAM:null,kind==Domestic.Kind.SHIPYARD?Army.Ship.TOWER_SHIP:null));return w;
    }
    private static void facilityLoss()throws Exception{
        for(Domestic.Kind kind:new Domestic.Kind[]{Domestic.Kind.WORKSHOP,Domestic.Kind.SHIPYARD}){
            World raid=producing(kind);int gold=raid.city(11).gold;raid.city(11).order=30;check(raid.events.spawn(11,WorldEvents.Tribe.BANDIT),"factory raid camp exists");
            for(long value=0;;value++){raid.events.randomState=value;if(raid.events.nextInt(100)<25){raid.events.randomState=value;break;}}
            raid.turn=3;raid.events.tick();check(raid.domestic.facilities.isEmpty(),"raid actually destroys production building");
            check(raid.army.productions().isEmpty()&&raid.officer(5).otherTaskTurns==0&&raid.officer(5).otherTask.isEmpty(),"raid clears orphan production and worker clock immediately");
            check(raid.city(11).gold==gold&&Arrays.equals(bytes(raid),bytes(copy(raid))),"raid cancels without refund and remains saveable");
            World lightning=producing(kind);gold=lightning.city(11).gold;World.Unit mage=unit(lightning,1,World.Weapon.SPEAR,5,17),enemy=unit(lightning,21,World.Weapon.SPEAR,4,17);lightning.officer(1).skillId=Skill.GUIMEN.id;
            seed(lightning,lightning.war.plotChance(mage.id,enemy.hex,War.Plot.LIGHTNING),true);ok(lightning.war.plot(mage.id,enemy.hex,War.Plot.LIGHTNING));
            check(lightning.domestic.facilities.isEmpty()&&lightning.army.productions().isEmpty()&&lightning.officer(5).otherTaskTurns==0,"lightning clears factory production and occupied worker");
            check(lightning.city(11).gold==gold&&Arrays.equals(bytes(lightning),bytes(copy(lightning))),"lightning factory loss can be saved without free refund");
        }
    }
    private static void districts()throws Exception{
        World w=fixture();reject(w,()->w.districts.configure(-1,"非法",new int[]{10},Districts.Policy.ECONOMY,-1,-1,false,true));
        reject(w,()->w.districts.configure(-1,"非法",new int[]{11,11},Districts.Policy.ECONOMY,-1,-1,false,true));
        ok(w.districts.configure(-1,"后军",new int[]{11},Districts.Policy.ECONOMY,-1,10,false,true));Districts.District d=w.districts.all().get(0);check(d.points()==0&&w.actionPoints[0]==40&&d.leader()>=0,"creation consumes main budget and cannot mint current-turn AP");
        reject(w,()->w.recruit(11,4));w.districts.run();check(w.domestic.missions.isEmpty()&&w.domestic.facilities.isEmpty(),"zero-budget new district does no work");
        w.turn++;reset(w);w.districts.reset(0);int main=w.actionPoints[0];w.districts.run();check(w.actionPoints[0]==main&&d.points()<60,"delegated AP is isolated from first district");check(w.domestic.missions.stream().anyMatch(m->m.sourceCity==11&&m.targetCity==10)&&!w.domestic.facilities.isEmpty(),"real transport and construction issued");byte[] before=bytes(w);w.districts.run();check(Arrays.equals(before,bytes(w)),"district cannot run twice in same turn");
        ok(w.districts.dissolve(d.id));check(w.districts.all().isEmpty()&&w.districts.directCity(11),"dissolve restores control without deleting work");
        World attack=fixture();ok(attack.districts.configure(-1,"进军",new int[]{11,12},Districts.Policy.CITY_ATTACK,20,-1,true,true));attack.turn=1;reset(attack);attack.districts.reset(0);attack.districts.run();check(attack.units.stream().anyMatch(u->attack.districts.unit(u.id)!=null),"attack district deploys real armies");check(attack.city(11).troops>=10000&&attack.city(12).troops>=10000,"minimum garrisons retained");
        World.Unit attached=attack.units.stream().filter(u->attack.districts.unit(u.id)!=null).findFirst().get();attack.orders.reset(attached);reject(attack,()->attack.war.waitUnit(attached.id));
        Districts.District advancing=attack.districts.all().get(0);attack.city(20).owner=0;for(World.Officer o:attack.officers)if(o.owner==1&&o.cityId==20)o.cityId=-1;attack.districts.captured(attack.city(20),attached);check(advancing.cities().contains(20)&&advancing.policy()==Districts.Policy.DEFENSE,"capture joins correct district and ends conquest objective");bytes(attack);
        World capital=fixture();ok(capital.districts.configure(-1,"余部",new int[]{11,12},Districts.Policy.DEFENSE,-1,-1,false,true));capital.city(10).owner=1;for(World.Officer o:capital.officers)if(o.owner==0&&o.cityId==10)o.cityId=-1;capital.districts.cleanup();check(capital.districts.city(11)==null,"lost capital appoints replacement direct city");bytes(capital);
    }
    private static void migration()throws Exception{
        try(InputStream in=WorldSystemsTest.class.getResourceAsStream("/legacy-v12.sg11.b64")){
            check(in!=null,"real prior-version fixture exists");byte[] old=Base64.getMimeDecoder().decode(in.readAllBytes());check(old[7]==12,"old writer header");World w=SaveCodec.decode(old);check(w.treasures.items().size()==43&&!w.events.enabled()&&w.districts.all().isEmpty(),"old relationship/treasure save retains state without invented world events");check(bytes(w)[7]==24&&Arrays.equals(bytes(w),bytes(copy(w))),"v12 migration roundtrips");
        }
    }
    private static void branchMigration()throws Exception{
        for(String branch:new String[]{"world","march"})try(InputStream in=WorldSystemsTest.class.getResourceAsStream("/legacy-v13-"+branch+".sg11.b64")){
            check(in!=null,"real v13 branch fixture exists");byte[] raw=Base64.getMimeDecoder().decode(in.readAllBytes());check(raw[7]==13,"fixture is a real unmodified v13 writer");World w=SaveCodec.decode(raw);
            if(branch.equals("world")){check(w.events.enabled()&&w.events.camps().size()==1&&w.districts.all().size()==1,"world v13 retains district and hazards");check(w.units.stream().noneMatch(u->u.march!=null),"world v13 adds no march");}
            else {check(w.unit(1).march!=null&&w.unit(1).hex.equals(new Hex(7,6)),"march v13 retains order and spent movement");check(!w.events.enabled()&&w.districts.all().isEmpty(),"march v13 adds no world events");}
            check(bytes(w)[7]==24&&Arrays.equals(bytes(w),bytes(copy(w))),"both v13 layouts upgrade to stable v17");World b=copy(w);for(int i=0;i<3;i++){ok(w.nextTurn());ok(b.nextTurn());check(Arrays.equals(bytes(w),bytes(b)),"old branch resumes with all new systems");}
        }
    }
    private static void malformed()throws Exception{
        World w=fixture();w.events.hazards.put(11,new WorldEvents.Hazard(11,WorldEvents.Disaster.LOCUST,0));invalid(w);
        w=fixture();w.events.camps.add(new WorldEvents.Camp(1,11,WorldEvents.Tribe.BANDIT,w.city(11).hex,3000));w.events.nextCamp=2;invalid(w);
        w=fixture();ok(w.districts.configure(-1,"后军",new int[]{11},Districts.Policy.ECONOMY,-1,-1,false,true));w.districts.all().get(0).points=61;invalid(w);
        w=foreign();w.contests.current().foreign=0;invalid(w);
        byte[] good=bytes(fixture());for(int cut=20;cut<good.length;cut+=113){try{SaveCodec.decode(Arrays.copyOf(good,cut));throw new AssertionError("truncated accepted");}catch(IOException expected){checks++;}}
    }
    private static void invalid(World w)throws Exception{try{bytes(w);throw new AssertionError("invalid world accepted");}catch(IOException expected){checks++;}}
    private static void replay()throws Exception{
        World a=TestScenarios.load("world-drill",0,41);check(a.events.enabled()&&a.events.camps().size()==1&&a.events.hazards().size()==1,"new scenario loads real events and poison");ok(a.districts.configure(-1,"北军",new int[]{11},Districts.Policy.ECONOMY,-1,10,false,true));World b=copy(a);
        for(int i=0;i<36&&!a.gameOver();i++){ok(a.nextTurn());ok(b.nextTurn());check(Arrays.equals(bytes(a),bytes(b)),"world/district per-turn replay "+i);b=copy(b);}
    }
}
