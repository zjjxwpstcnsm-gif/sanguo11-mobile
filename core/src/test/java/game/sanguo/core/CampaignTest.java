package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Behavioral tests for campaign commands, combat interactions and real old-save migration. */
public final class CampaignTest {
    private static int checks,cases;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void ok(World.Result result){check(result.ok,result.message);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private interface Command {World.Result run();}
    private static void rejected(World w,Command command)throws Exception{byte[] before=bytes(w);check(!command.run().ok,"invalid command rejected");check(Arrays.equals(before,bytes(w)),"invalid command has no resource, RNG, action or state effects");}
    private static World fixture(){
        World w=new World(20,14,"甲军","乙军","丙军");
        w.cities.add(new World.City(10,"甲城",new Hex(2,2),0));w.cities.add(new World.City(20,"乙城",new Hex(16,2),1));w.cities.add(new World.City(30,"丙城",new Hex(8,11),2));
        for(World.City c:w.cities){c.gold=50000;c.food=100000;for(int i=0;i<6;i++)w.officers.add(new World.Officer(c.owner*10+i,"将"+(c.owner*10+i),c.owner,c.id,80,85,90,80,80));}
        w.strategy.initializeOffices();return w;
    }
    private static World.Unit unit(World w,int officer,World.Weapon weapon,int q,int r){
        World.Officer o=w.officer(officer);World.Unit u=new World.Unit(w.nextUnitId++,o.owner,o.id,weapon,new Hex(q,r),5000,10000);
        w.strategy.releaseGovernor(o.id);o.cityId=-1;o.unitId=u.id;w.units.add(u);return u;
    }
    private static void reset(World w){w.active=0;Arrays.fill(w.actionPoints,60);for(World.Officer o:w.officers)o.acted=false;for(World.Unit u:w.units)u.acted=false;}
    private static void tick(World w)throws Exception{w.turn++;w.domestic.tick();w.campaign.tick();w.abilities.tick();w.strategy.tick();w.war.tick();reset(w);SaveCodec.validate(w);}
    private static void seed(World w,int chance,boolean success){for(int n=0;n<10000;n++){w.strategy.setSeed(n);boolean hit=w.strategy.nextInt(100)<chance;if(hit==success){w.strategy.setSeed(n);return;}}throw new AssertionError("seed not found");}
    private static void caseDone(){cases++;}
    public static void main(String[] args)throws Exception{
        trade();diplomacy();rumor();research();study();merge();structures();tactics();plots();combat();techEffects();migration();validation();continuation();
        System.out.println("PASS: "+cases+" campaign cases, "+checks+" assertions covering diplomacy, trade, research, studies, merges, 12 tactics, 6 plots, fire chains, structures and save v1-v5.");
    }
    private static void trade()throws Exception{
        World w=fixture();int gold=w.city(10).gold,food=w.city(10).food;
        int buy=w.campaign.foodPrice(10,true),sell=w.campaign.foodPrice(10,false);check(buy>sell,"ask exceeds bid");
        ok(w.campaign.trade(10,0,true,5000));check(w.city(10).gold==gold-buy*5&&w.city(10).food==food+5000,"buy conserves quoted gold and food");
        rejected(w,()->w.campaign.trade(10,0,true,1000));
        ok(w.campaign.trade(10,1,false,5000));check(w.city(10).food==food&&w.city(10).gold<gold,"round-trip cannot print gold");
        rejected(w,()->w.campaign.trade(10,2,true,Integer.MAX_VALUE));rejected(w,()->w.campaign.trade(10,2,false,-1000));
        ok(w.campaign.trade(10,2,true,10000));rejected(w,()->w.campaign.trade(10,3,true,1000));
        World restored=SaveCodec.decode(bytes(w));check(restored.campaign.traded(10)==20000,"volume guard persists");tick(restored);check(restored.campaign.traded(10)==0,"next turn reopens market");
        w.city(10).food=1000000;reset(w);w.campaign.traded.clear();rejected(w,()->w.campaign.trade(10,3,true,1000));caseDone();
    }
    private static void diplomacy()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,World.Weapon.SPEAR,6,5),b=unit(w,11,World.Weapon.SPEAR,7,5);
        rejected(w,()->w.campaign.negotiate(10,0,1,Campaign.TreatyKind.ALLIANCE,6));
        ok(w.campaign.goodwill(10,0,1));check(w.strategy.factionRelation(0,1)==23,"envoy improves relation");
        seed(w,w.campaign.treatyChance(2,1,Campaign.TreatyKind.ALLIANCE),true);ok(w.campaign.negotiate(10,2,1,Campaign.TreatyKind.ALLIANCE,3));
        check(!w.campaign.hostile(0,1)&&!w.campaign.hostile(1,0)&&w.campaign.hostile(0,2),"symmetric pact doesn't protect third party");
        rejected(w,()->w.attack(a.id,b.id));rejected(w,()->w.war.tactic(a.id,b.id,War.Tactic.THRUST));rejected(w,()->w.war.plot(a.id,b.hex,War.Plot.FIRE));
        a.hex=new Hex(15,2);rejected(w,()->w.siege(a.id,20));a.hex=new Hex(6,5);
        rejected(w,()->w.campaign.negotiate(10,3,1,Campaign.TreatyKind.CEASEFIRE,6));
        w.active=1;rejected(w,()->w.attack(b.id,a.id));w.active=0;
        tick(w);check(!w.campaign.hostile(0,1),"pact survives first tick");tick(w);check(!w.campaign.hostile(0,1),"pact remains until due turn");tick(w);check(w.campaign.hostile(0,1),"pact expires exactly at due turn");
        seed(w,w.campaign.treatyChance(0,1,Campaign.TreatyKind.CEASEFIRE),false);int gold=w.city(10).gold;ok(w.campaign.negotiate(10,0,1,Campaign.TreatyKind.CEASEFIRE,6));
        check(w.campaign.treaty(0,1)==null&&w.city(10).gold==gold-1000&&w.officer(0).acted,"refusal consumes costs without treaty");
        reset(w);seed(w,w.campaign.treatyChance(0,1,Campaign.TreatyKind.CEASEFIRE),true);ok(w.campaign.negotiate(10,0,1,Campaign.TreatyKind.CEASEFIRE,6));int relation=w.strategy.factionRelation(0,1);
        ok(w.campaign.breakTreaty(10,3,1));check(w.campaign.hostile(0,1)&&w.strategy.factionRelation(0,1)==relation-50&&w.strategy.factionRelation(0,2)==-10,"breaking pact changes diplomacy and combat eligibility");caseDone();
    }
    private static void rumor()throws Exception{
        World w=fixture();w.cities.add(new World.City(21,"边城",new Hex(10,3),1));w.officer(12).cityId=21;
        int loyalty=w.officer(12).loyalty;seed(w,w.campaign.rumorChance(0,21),true);ok(w.campaign.rumor(10,0,21));
        check(w.city(21).order==80&&w.officer(12).loyalty==loyalty-5,"rumor affects actual order and loyalty");
        rejected(w,()->w.campaign.rumor(10,1,10));rejected(w,()->w.campaign.rumor(10,1,20));caseDone();
    }
    private static void research()throws Exception{
        World w=fixture();w.campaign.points.put(0,2000);
        rejected(w,()->w.campaign.research(10,0,Campaign.Tech.SUPPLY_RAID));int gold=w.city(10).gold;
        ok(w.campaign.research(10,0,Campaign.Tech.SPEAR_DRILL));check(w.campaign.points(0)==1700&&w.city(10).gold==gold-1000,"research debits exact costs once");
        rejected(w,()->w.deploy(10,0,World.Weapon.SPEAR,3000));rejected(w,()->w.campaign.research(10,1,Campaign.Tech.CROSSBOW_DRILL));
        World restored=SaveCodec.decode(bytes(w));for(int i=0;i<2;i++){tick(w);tick(restored);check(!w.campaign.has(0,Campaign.Tech.SPEAR_DRILL),"no early research effect");}
        tick(w);tick(restored);check(w.campaign.has(0,Campaign.Tech.SPEAR_DRILL)&&!w.strategy.busy(0)&&w.campaign.projects().isEmpty(),"research finishes and unlocks actor");
        check(Arrays.equals(bytes(w),bytes(restored)),"research survives save/reload with exact continuation");rejected(w,()->w.campaign.research(10,0,Campaign.Tech.SPEAR_DRILL));
        ok(w.campaign.research(10,0,Campaign.Tech.SUPPLY_RAID));check(w.campaign.projects().get(0).tech==Campaign.Tech.SUPPLY_RAID,"completed prerequisite enables next technique");caseDone();
        World lost=fixture();lost.campaign.points.put(0,1000);ok(lost.campaign.research(10,0,Campaign.Tech.SPEAR_DRILL));lost.city(10).defense=1;
        World.Unit attacker=unit(lost,11,World.Weapon.SPEAR,3,2);lost.active=1;ok(lost.siege(attacker.id,10));
        check(lost.campaign.projects().isEmpty()&&!lost.campaign.has(0,Campaign.Tech.SPEAR_DRILL)&&lost.officer(0).otherTaskTurns==0,"city capture cancels pending research without awarding it");SaveCodec.validate(lost);caseDone();
    }
    private static void study()throws Exception{
        for(Campaign.Study study:Campaign.Study.values()){
            World w=fixture();for(AbilityResearch.Node n:AbilityResearch.catalog())if(n.slot.isEmpty())AbilityResearchTest.unlock(w,0,n.id);int before=w.campaign.studyValue(0,study);ok(w.campaign.study(10,0,study));rejected(w,()->w.strategy.search(10,0));
            for(int i=0;i<3;i++)tick(w);check(w.campaign.studyValue(0,study)==Math.min(study.ordinal()<5?95:2,before+(study.ordinal()<5?5:1)),"study changes requested attribute or aptitude");
            World copy=SaveCodec.decode(bytes(w));check(copy.campaign.studyValue(0,study)==w.campaign.studyValue(0,study),"trained values persist");caseDone();
        }
        World w=fixture();w.officer(0).war=100;rejected(w,()->w.campaign.study(10,0,Campaign.Study.WAR));w.officer(0).aptitude[0]=3;rejected(w,()->w.campaign.study(10,0,Campaign.Study.SPEAR));caseDone();
    }
    private static Domestic.Facility facility(World w,Domestic.Kind kind,int q,int r){Domestic.Facility f=new Domestic.Facility(w.domestic.nextFacilityId++,10,kind,new Hex(q,r),-1,0);w.domestic.facilities.add(f);return f;}
    private static void merge()throws Exception{
        World w=fixture();Domestic.Facility target=facility(w,Domestic.Kind.MARKET,3,2),material=facility(w,Domestic.Kind.MARKET,4,2);
        ok(w.domestic.merge(target.id,material.id,0));check(w.domestic.facility(material.id)==null&&target.level==1&&target.upgradeTo==2,"merge consumes adjacent level-one material");
        check(w.domestic.monthlyGold(10)==w.strategy.cityIncome(10,1200),"old level continues producing during merge");
        rejected(w,()->w.domestic.merge(target.id,target.id,1));rejected(w,()->w.campaign.study(10,0,Campaign.Study.WAR));
        World restored=SaveCodec.decode(bytes(w));tick(w);tick(restored);tick(w);tick(restored);
        check(target.level==2&&target.upgradeTo==0&&w.domestic.monthlyGold(10)==w.strategy.cityIncome(10,1280),"merge level changes actual income after two ticks");
        check(Arrays.equals(bytes(w),bytes(restored)),"in-flight merge resumes exactly");facility(w,Domestic.Kind.MINT,3,3);
        check(w.domestic.monthlyGold(10)==w.strategy.cityIncome(10,1520),"mint boosts adjacent level-two market");
        material=facility(w,Domestic.Kind.MARKET,4,2);ok(w.domestic.merge(target.id,material.id,0));ok(w.domestic.cancelBuild(target.id));check(target.level==2&&target.remaining==0&&w.domestic.facility(target.id)!=null,"cancel preserves upgraded target but not consumed material");
        tick(w);material=facility(w,Domestic.Kind.MARKET,4,2);ok(w.domestic.merge(target.id,material.id,0));tick(w);tick(w);check(target.level==3,"second merge reaches level three");
        check(w.domestic.mergeCandidates(target.id).isEmpty(),"level three cannot merge further");caseDone();
    }
    private static void structures()throws Exception{
        World w=fixture();Hex site=w.war.buildSites(10).get(0);ok(w.war.build(10,0,War.StructureKind.MUSIC,site));
        War.Structure structure=w.war.at(site);check(structure!=null,"structure exists on map");World.Unit u=unit(w,1,World.Weapon.SPEAR,5,2);u.energy=40;
        u.hex=site.neighbors().stream().filter(h->w.cost(h,u.weapon)>0&&w.cityAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null).findFirst().get();
        check(!w.reachable(u).containsKey(site),"military structure blocks movement");tick(w);check(u.energy==50,"music structure restores energy");
        rejected(w,()->w.war.build(10,2,War.StructureKind.CAMP,site));
        World copy=SaveCodec.decode(bytes(w));check(copy.war.at(site).kind==War.StructureKind.MUSIC,"structure persists");ok(w.war.removeStructure(10,2,structure.id));check(w.war.at(site)==null,"military structure can be dismantled");caseDone();
        World fire=fixture();World.Unit caster=unit(fire,0,World.Weapon.SPEAR,5,5),enemy=unit(fire,10,World.Weapon.SPEAR,8,5),friend=unit(fire,1,World.Weapon.SPEAR,7,4);
        fire.war.structures.add(new War.Structure(1,0,War.StructureKind.FIRE_SEED,new Hex(6,5),200));fire.war.structures.add(new War.Structure(2,0,War.StructureKind.FIRE_SEED,new Hex(7,5),200));fire.war.nextStructureId=3;
        seed(fire,fire.war.plotChance(caster.id,new Hex(6,5),War.Plot.FIRE),true);ok(fire.war.plot(caster.id,new Hex(6,5),War.Plot.FIRE));
        check(fire.war.structures().isEmpty()&&fire.war.fireAt(enemy.hex)!=null,"fire seed chain ignites neighbors without recursion loop");
        tick(fire);check(enemy.troops==4750&&friend.troops==4750,"fire hurts enemy and own troops");tick(fire);check(fire.war.fires().isEmpty()&&enemy.troops==4500,"fire lasts two ticks and expires");caseDone();
    }
    private static void tactics()throws Exception{
        for(War.Tactic tactic:War.Tactic.values()){
            World w=fixture();World.Unit a=unit(w,0,tactic.weapon,6,5),b=unit(w,10,World.Weapon.SPEAR,7,5);Arrays.fill(w.officer(0).aptitude,3);
            int initialEnergy=a.energy;seed(w,w.war.tacticChance(a.id,b.id,tactic),true);ok(w.war.tactic(a.id,b.id,tactic));
            check(b.troops<5000&&a.energy==initialEnergy-tactic.energy&&a.acted,"successful tactic damages and spends one action");
            rejected(w,()->w.war.tactic(a.id,b.id,tactic));SaveCodec.validate(w);
            if(tactic==War.Tactic.THRUST)check(b.hex.equals(new Hex(8,5))&&a.hex.equals(new Hex(6,5)),"thrust displaces only enemy");
            if(tactic==War.Tactic.DOUBLE_THRUST)check(b.hex.equals(new Hex(9,5)),"double thrust pushes two hexes");
            if(tactic==War.Tactic.HOOK)check(a.hex.equals(new Hex(5,5))&&b.hex.equals(new Hex(6,5)),"hook retreats actor and pulls enemy");
            if(tactic==War.Tactic.BREAKTHROUGH)check(a.hex.equals(new Hex(8,5))&&b.hex.equals(new Hex(7,5)),"breakthrough passes behind enemy");
            if(tactic==War.Tactic.SPIRAL)check(b.status==War.Status.CONFUSED,"spiral applies confusion");
            if(tactic==War.Tactic.FIRE_ARROW)check(w.war.fireAt(b.hex)!=null,"fire arrow ignites target hex");
            caseDone();
        }
        World w=fixture();World.Unit a=unit(w,0,World.Weapon.HALBERD,6,5),b=unit(w,10,World.Weapon.SPEAR,7,5),c=unit(w,11,World.Weapon.SPEAR,6,6),ally=unit(w,20,World.Weapon.SPEAR,5,6);
        w.campaign.treaties.add(new Campaign.Treaty(0,2,Campaign.TreatyKind.ALLIANCE,10));w.officer(0).aptitude[1]=3;
        seed(w,w.war.tacticChance(a.id,b.id,War.Tactic.WHIRLWIND),true);ok(w.war.tactic(a.id,b.id,War.Tactic.WHIRLWIND));
        check(b.troops<5000&&c.troops<5000&&ally.troops==5000,"whirlwind hits multiple enemies and respects alliance");caseDone();
        World fail=fixture();World.Unit f=unit(fail,0,World.Weapon.SPEAR,6,5),t=unit(fail,10,World.Weapon.SPEAR,7,5);int energy=f.energy;
        rejected(fail,()->fail.war.tactic(f.id,t.id,War.Tactic.SPIRAL));f.energy=0;rejected(fail,()->fail.war.tactic(f.id,t.id,War.Tactic.THRUST));f.energy=energy;
        seed(fail,fail.war.tacticChance(f.id,t.id,War.Tactic.THRUST),false);ok(fail.war.tactic(f.id,t.id,War.Tactic.THRUST));check(t.troops==5000&&f.acted&&f.energy==energy-15,"miss spends energy but causes no damage or displacement");caseDone();
    }
    private static void plots()throws Exception{
        for(War.Plot plot:War.Plot.values()){
            World w=fixture();World.Unit a=unit(w,0,World.Weapon.SPEAR,6,5),enemy=unit(w,10,World.Weapon.SPEAR,7,5),friend=unit(w,1,World.Weapon.SPEAR,6,6);Hex target=enemy.hex;
            if(plot==War.Plot.EXTINGUISH)w.war.fires.add(new War.Fire(target,1,2));
            if(plot==War.Plot.CALM){friend.status=War.Status.CONFUSED;friend.statusTurns=1;target=friend.hex;}
            if(plot==War.Plot.AMBUSH)w.terrain[6][5]=World.Terrain.FOREST;
            int chance=w.war.plotChance(a.id,target,plot);if(chance<100)seed(w,chance,true);ok(w.war.plot(a.id,target,plot));
            check(a.acted&&a.energy==80-plot.energy,"plot spends action and quoted energy");
            if(plot==War.Plot.EXTINGUISH)check(w.war.fireAt(target)==null,"extinguish removes fire");
            if(plot==War.Plot.CALM)check(friend.status==War.Status.NORMAL&&friend.statusTurns==0,"calm clears state");
            if(plot==War.Plot.CONFUSE||plot==War.Plot.MISLEAD){
                Hex old=enemy.hex;w.war.resetOwner(1);check(enemy.acted&&enemy.statusTurns==0,"one owner action suppressed");
                if(plot==War.Plot.MISLEAD)check(enemy.hex.distance(w.city(20).hex)<old.distance(w.city(20).hex),"false report retreats toward home");
                enemy.acted=false;w.war.resetOwner(1);check(enemy.status==War.Status.NORMAL&&!enemy.acted,"status expires next owner action");
            }
            if(plot==War.Plot.AMBUSH)check(enemy.troops<5000&&enemy.energy==65,"ambush damages troops and energy");
            SaveCodec.validate(w);caseDone();
        }
    }
    private static void combat()throws Exception{
        World w=fixture();World.Unit a=unit(w,0,World.Weapon.SPEAR,6,5),b=unit(w,10,World.Weapon.HALBERD,7,5);
        byte[] before=bytes(w);int preview=w.war.previewDamage(a.id,b.id);check(preview>0&&Arrays.equals(before,bytes(w)),"damage preview doesn't consume RNG");
        ok(w.attack(a.id,b.id));check(a.troops<5000&&b.troops<5000,"adjacent melee counterattacks");caseDone();
        World ranged=fixture();World.Unit x=unit(ranged,0,World.Weapon.CROSSBOW,6,5),y=unit(ranged,10,World.Weapon.HALBERD,8,5);ok(ranged.attack(x.id,y.id));check(x.troops==5000&&y.troops<5000,"ranged strike avoids melee counterattack");
        reset(ranged);y.troops=1;ok(ranged.attack(x.id,y.id));check(ranged.unit(y.id)==null&&ranged.officer(10).unitId==-1,"defeated unit removed with commander reference repaired");SaveCodec.validate(ranged);caseDone();
    }
    private static void migration()throws Exception{
        try(InputStream in=CampaignTest.class.getResourceAsStream("/legacy-v4.sg11.b64")){
            check(in!=null,"authentic v4 fixture exists");World w=SaveCodec.decode(Base64.getMimeDecoder().decode(in.readAllBytes()));
            check(w.city(300).governorId==3001&&w.domestic.facilities.get(0).remaining==2,"real v4 governor/construction survive migration");
            check(w.campaign.projects().isEmpty()&&w.war.fires().isEmpty()&&w.campaign.points(2)==0,"legacy saves don't invent campaign history");
            for(World.Officer o:w.officers)check(Arrays.equals(o.aptitude,new int[]{1,1,1,1,1,1}),"legacy aptitude has documented B default");
            byte[] modern=bytes(w);check(modern[7]==10&&Arrays.equals(modern,bytes(SaveCodec.decode(modern))),"v4 upgrades to exact round-tripping v9");caseDone();
        }
    }
    private static void learn(World w,int owner,Campaign.Tech tech){if(tech.prerequisite!=null)learn(w,owner,tech.prerequisite);w.campaign.learned.computeIfAbsent(owner,k->EnumSet.noneOf(Campaign.Tech.class)).add(tech);}
    private static void techEffects()throws Exception{
        for(Campaign.Tech tech:new Campaign.Tech[]{Campaign.Tech.SPEAR_DRILL,Campaign.Tech.CROSSBOW_DRILL,Campaign.Tech.CAVALRY_DRILL}){
            World w=fixture();World.Weapon weapon=tech==Campaign.Tech.SPEAR_DRILL?World.Weapon.SPEAR:tech==Campaign.Tech.CROSSBOW_DRILL?World.Weapon.CROSSBOW:World.Weapon.CAVALRY;
            World.Unit a=unit(w,0,weapon,6,5),b=unit(w,10,World.Weapon.SPEAR,7,5);War.Tactic tactic=weapon==World.Weapon.SPEAR?War.Tactic.THRUST:weapon==World.Weapon.CROSSBOW?War.Tactic.FIRE_ARROW:War.Tactic.CHARGE;
            seed(w,w.war.tacticChance(a.id,b.id,tactic),true);World enhanced=SaveCodec.decode(bytes(w));learn(enhanced,0,tech);
            ok(w.war.tactic(a.id,b.id,tactic));ok(enhanced.war.tactic(a.id,b.id,tactic));check(enhanced.unit(b.id).troops<b.troops,"weapon drill increases actual tactic damage with identical RNG");caseDone();
        }
        World base=fixture();World.Unit shooter=unit(base,0,World.Weapon.CROSSBOW,6,5),guard=unit(base,10,World.Weapon.HALBERD,8,5);World drill=SaveCodec.decode(bytes(base)),shield=SaveCodec.decode(bytes(base));learn(drill,1,Campaign.Tech.HALBERD_DRILL);learn(shield,1,Campaign.Tech.SHIELD);
        ok(base.attack(shooter.id,guard.id));ok(drill.attack(shooter.id,guard.id));ok(shield.attack(shooter.id,guard.id));check(guard.troops==drill.unit(guard.id).troops&&shield.unit(guard.id).troops>=guard.troops,"halberd drill is offensive; shield can block indirect normals");caseDone();
        World bow=fixture();World.Unit x=unit(bow,0,World.Weapon.CROSSBOW,6,5),y=unit(bow,10,World.Weapon.SPEAR,9,5);rejected(bow,()->bow.attack(x.id,y.id));learn(bow,0,Campaign.Tech.STRONG_BOW);ok(bow.attack(x.id,y.id));check(y.troops<5000,"strong bow opens extended range in real command");caseDone();
        World horse=fixture();World.Unit rider=unit(horse,0,World.Weapon.CAVALRY,6,5);Hex destination=new Hex(13,5);check(!horse.reachable(rider).containsKey(destination),"base cavalry cannot travel seven points");learn(horse,0,Campaign.Tech.HORSE_BREEDING);ok(horse.move(rider.id,destination));check(rider.hex.equals(destination),"horse research extends real movement");caseDone();
        World raid=fixture();World.Unit raider=unit(raid,0,World.Weapon.SPEAR,6,5),victim=unit(raid,10,World.Weapon.SPEAR,7,5);learn(raid,0,Campaign.Tech.SUPPLY_RAID);seed(raid,raid.war.tacticChance(raider.id,victim.id,War.Tactic.THRUST),true);ok(raid.war.tactic(raider.id,victim.id,War.Tactic.THRUST));check(raider.food==11000&&victim.food==9000,"supply raid transfers existing food rather than creating it");caseDone();
        World normal=fixture();normal.city(10).defense=500;World builders=SaveCodec.decode(bytes(normal));learn(builders,0,Campaign.Tech.ENGINEERING);ok(normal.campaign.repair(10,0));ok(builders.campaign.repair(10,0));check(builders.city(10).defense>normal.city(10).defense&&builders.city(10).gold==normal.city(10).gold,"engineering improves repair for same cost");caseDone();
        World city=fixture();World.Unit siege=unit(city,0,World.Weapon.SPEAR,15,2);World fortified=SaveCodec.decode(bytes(city));learn(fortified,1,Campaign.Tech.WALLS);ok(city.siege(siege.id,20));ok(fortified.siege(siege.id,20));check(fortified.city(20).defense>city.city(20).defense,"wall technique reduces actual siege damage");caseDone();
        World fire=fixture();World.Unit target=unit(fire,10,World.Weapon.SPEAR,8,5);fire.war.fires.add(new War.Fire(target.hex,0,2));World stronger=SaveCodec.decode(bytes(fire));learn(stronger,0,Campaign.Tech.FIRE_MASTERY);fire.war.tick();stronger.war.tick();check(stronger.unit(target.id).troops==target.troops,"divine fire changes range, not persistent fire damage");caseDone();
        World supply=fixture();World.Unit army=unit(supply,0,World.Weapon.SPEAR,4,10);World efficient=SaveCodec.decode(bytes(supply));learn(efficient,0,Campaign.Tech.LOGISTICS);ok(supply.nextTurn());ok(efficient.nextTurn());check(efficient.unit(army.id).food==army.food&&efficient.campaign.energyCap(0)==120,"veteran troops increase energy cap, not food efficiency");caseDone();
    }
    private static void invalid(World w)throws Exception{boolean invalid=false;try{SaveCodec.encode(w);}catch(IOException expected){invalid=true;}check(invalid,"invalid state cannot be saved");}
    private static void validation()throws Exception{
        World w=fixture();w.officer(0).aptitude[0]=4;invalid(w);
        w=fixture();w.campaign.learned.put(0,EnumSet.of(Campaign.Tech.SUPPLY_RAID));invalid(w);
        w=fixture();w.campaign.treaties.add(new Campaign.Treaty(0,0,Campaign.TreatyKind.ALLIANCE,5));invalid(w);
        w=fixture();w.campaign.traded.put(10,20001);invalid(w);
        w=fixture();w.war.structures.add(new War.Structure(1,0,War.StructureKind.CAMP,w.city(10).hex,800));w.war.nextStructureId=2;invalid(w);
        w=fixture();w.war.fires.add(new War.Fire(new Hex(5,5),0,0));invalid(w);
        w=fixture();w.campaign.projects.add(new Campaign.Project(0,10,0,Campaign.Tech.SPEAR_DRILL,null));invalid(w);caseDone();
    }
    private static void continuation()throws Exception{
        for(int side=0;side<3;side++){
            World w=ScenarioCatalog.load("regional-sandbox",side);World.City home=w.home();World.Officer officer=w.idle(home).get(0);
            AbilityResearchTest.unlock(w,side,"spear.a");ok(w.campaign.study(home.id,officer.id,Campaign.Study.SPEAR));World copy=SaveCodec.decode(bytes(w));
            for(int turn=0;turn<60&&!w.gameOver();turn++){
                check(w.nextTurn().ok&&copy.nextTurn().ok,"complete campaign turn succeeds");check(Arrays.equals(bytes(w),bytes(copy)),"player "+side+" deterministic turn "+turn);
                copy=SaveCodec.decode(bytes(copy));
            }caseDone();
        }
        World peace=fixture();World.Unit a=unit(peace,0,World.Weapon.SPEAR,6,5),b=unit(peace,10,World.Weapon.SPEAR,7,5);
        peace.campaign.treaties.add(new Campaign.Treaty(0,1,Campaign.TreatyKind.ALLIANCE,6));int troops=a.troops;ok(peace.nextTurn());
        check(a.troops==troops&&b.troops==5000,"full AI turn honors pact for adjacent enemies");caseDone();
    }
}
