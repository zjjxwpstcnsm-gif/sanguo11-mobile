package game.sanguo.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public final class AbilityResearchTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static World fixture(){World w=new World(20,14,"甲军","乙军");w.cities.add(new World.City(10,"甲城",new Hex(2,2),0));w.cities.add(new World.City(20,"乙城",new Hex(17,11),1));for(World.City c:w.cities){c.gold=50000;c.food=200000;for(int i=0;i<8;i++)w.officers.add(new World.Officer(c.owner*10+i,"武将"+(c.owner*10+i),c.owner,c.id,50,50,50,50,50));}w.strategy.initializeOffices();return w;}
    static void unlock(World w,int side,String id){AbilityResearch.Node n=AbilityResearch.node(id);for(String pre:n.prerequisites)unlock(w,side,pre);w.abilities.states[side].learned.add(id);}
    private static void tick(World w)throws Exception{w.turn++;w.contests.tick();w.campaign.tick();w.abilities.tick();w.strategy.tick();w.active=w.player;Arrays.fill(w.actionPoints,60);for(World.Officer o:w.officers)o.acted=false;SaveCodec.validate(w);}
    private static void reject(World w,java.util.function.Supplier<World.Result> cmd)throws Exception{byte[] before=bytes(w);check(!cmd.get().ok,"invalid command accepted");check(Arrays.equals(before,bytes(w)),"rejection changes complete save or RNG");}
    private static void invalid(World w)throws Exception{try{bytes(w);throw new AssertionError("invalid PK state accepted");}catch(IOException expected){checks++;}}
    private static void graph()throws Exception{
        World w=fixture();Set<String> visited=new HashSet<>();for(AbilityResearch.Node n:AbilityResearch.catalog())visit(n,visited,new HashSet<>());
        check(AbilityResearch.catalog().stream().filter(n->n.slot.isEmpty()).count()==48,"48 ordinary research nodes");
        check(AbilityResearch.catalog().size()==98,"50 hidden variants retained with explicit IDs");
        check(AbilityResearch.node("tiebi").prerequisites.equals(Arrays.asList("buqu","zhucheng")),"official manual AND prerequisites");
        for(int side=0;side<2;side++){check(w.abilities.states[side].hidden.size()==5,"five selected hidden slots");check(w.abilities.visible(side).stream().allMatch(n->n.slot.isEmpty()),"hidden unrevealed before prerequisites");}
        byte[] original=bytes(w);for(int i=0;i<20;i++){w.abilities.visible(0);w.abilities.researchError(10,"bawang");w.abilities.trainingError(10,0,"lead.low",false);}check(Arrays.equals(original,bytes(w)),"previews preserve RNG and hidden selection");
        World copy=SaveCodec.decode(original);check(Arrays.equals(original,bytes(copy)),"hidden selections persist without reroll");
        for(String id:w.abilities.states[0].hidden){AbilityResearch.Node n=AbilityResearch.node(id);for(String p:n.prerequisites)unlock(w,0,p);check(w.abilities.visible(0).contains(n),"hidden revealed through correct branch");}
    }
    private static void visit(AbilityResearch.Node n,Set<String> seen,Set<String> visiting){if(seen.contains(n.id))return;check(visiting.add(n.id),"research graph cycle");for(String p:n.prerequisites){check(AbilityResearch.node(p)!=null,"missing node");visit(AbilityResearch.node(p),seen,visiting);}visiting.remove(n.id);seen.add(n.id);}
    private static void research()throws Exception{
        World w=fixture();reject(w,()->w.abilities.startResearch(10,"bawang"));reject(w,()->w.abilities.startResearch(20,"lead.low"));reject(w,()->w.abilities.startResearch(10,"invalid"));
        w.actionPoints[0]=19;reject(w,()->w.abilities.startResearch(10,"lead.low"));w.actionPoints[0]=60;
        ok(w.abilities.startResearch(10,"lead.low"));check(w.city(10).gold==49700&&w.actionPoints[0]==40&&w.idle(w.city(10)).size()==8,"research exact costs and no officer lock");
        reject(w,()->w.abilities.startResearch(10,"war.low"));World copy=SaveCodec.decode(bytes(w));
        for(int i=0;i<8;i++){tick(w);tick(copy);check(!w.abilities.learned(0,"lead.low"),"no early unlock");check(Arrays.equals(bytes(w),bytes(copy)),"research continuation matches");}
        tick(w);tick(copy);check(w.abilities.learned(0,"lead.low")&&w.abilities.remaining(0,"lead.low")==5,"9th tick unlocks five uses");check(Arrays.equals(bytes(w),bytes(copy)),"completion persisted once");
        reject(w,()->w.abilities.startResearch(10,"lead.low"));ok(w.abilities.startResearch(10,"buqu"));ok(w.abilities.cancelResearch(0));int gold=w.city(10).gold;reject(w,()->w.abilities.cancelResearch(0));check(w.city(10).gold==gold,"no repeated refund");
    }
    private static void training()throws Exception{
        World w=fixture();reject(w,()->w.campaign.study(10,0,Campaign.Study.LEADERSHIP));unlock(w,0,"lead.low");unlock(w,0,"war.low");unlock(w,0,"spear.b");unlock(w,0,"buqu");w.officer(1).aptitude[0]=0;
        ok(w.abilities.train(10,0,"lead.low",false));reject(w,()->w.abilities.train(10,3,"war.low",false));ok(w.abilities.train(10,1,"spear.b",false));ok(w.abilities.train(10,2,"buqu",false));
        check(w.actionPoints[0]==0&&w.city(10).gold==50000&&w.abilities.training().size()==3,"three categories each AP20 and no gold");check(w.abilities.remaining(0,"lead.low")==5,"use not consumed before completion");
        World copy=SaveCodec.decode(bytes(w));for(int i=0;i<3;i++){tick(w);tick(copy);check(Arrays.equals(bytes(w),bytes(copy)),"training per-turn replay");}
        check(w.officer(0).leadership==55&&w.officer(1).aptitude[0]==1&&w.officer(2).skillId.equals(Skill.BUQU.id),"all three categories apply real fields");check(w.abilities.remaining(0,"lead.low")==4&&w.abilities.gained(0,0)==5,"one use and gain recorded");tick(w);check(w.officer(0).leadership==55,"no duplicate award");
        unlock(w,0,"jingang");reject(w,()->w.abilities.train(10,2,"jingang",false));ok(w.abilities.train(10,2,"jingang",true));ok(w.abilities.cancelTraining(2));check(w.officer(2).skillId.equals(Skill.BUQU.id)&&w.abilities.remaining(0,"jingang")==3,"cancel preserves skill and use");
        reject(w,()->w.abilities.cancelTraining(2));ok(w.abilities.train(10,3,"jingang",false));for(int i=0;i<3;i++)tick(w);check(w.officer(3).skillId.equals(Skill.JINGANG.id),"new skill visible to combat hooks");
        w.officer(4).leadership=69;ok(w.abilities.train(10,4,"lead.low",false));for(int i=0;i<3;i++)tick(w);check(w.officer(4).leadership==70&&w.abilities.gained(4,0)==1,"cap records actual gain rather than five");reject(w,()->w.abilities.train(10,4,"lead.low",false));
        w.officer(5).aptitude[0]=1;reject(w,()->w.abilities.train(10,5,"spear.b",false));unlock(w,0,"tiebi");ok(w.abilities.train(10,5,"tiebi",false));for(int i=0;i<3;i++)tick(w);check(w.officer(5).skillId.equals(Skill.TIEBI.id),"implemented ironwall can now be taught");
    }
    private static void limits()throws Exception{
        World w=fixture();unlock(w,0,"lead.mid");w.officer(0).leadership=51;
        for(int i=0;i<4;i++){ok(w.abilities.train(10,0,"lead.low",false));for(int t=0;t<3;t++)tick(w);}
        check(w.officer(0).leadership==70&&w.abilities.gained(0,0)==19,"headroom actual gain");ok(w.abilities.train(10,0,"lead.mid",false));for(int t=0;t<3;t++)tick(w);check(w.officer(0).leadership==75&&w.abilities.gained(0,0)==24,"last allowed training crosses twenty to twenty-four");reject(w,()->w.abilities.train(10,0,"lead.mid",false));
        ok(w.abilities.train(10,1,"lead.low",false));for(int t=0;t<3;t++)tick(w);check(w.abilities.remaining(0,"lead.low")==0,"uses shared across officers");reject(w,()->w.abilities.train(10,2,"lead.low",false));
        World bad=SaveCodec.decode(bytes(w));bad.abilities.states[0].used.put("lead.low",6);invalid(bad);bad=SaveCodec.decode(bytes(w));bad.abilities.states[0].hidden.clear();invalid(bad);bad=SaveCodec.decode(bytes(w));bad.abilities.states[0].learned.add("not.real");invalid(bad);
    }
    private static void lossAndLegacy()throws Exception{
        World w=fixture();w.cities.add(new World.City(11,"后方",new Hex(2,10),0));unlock(w,0,"war.low");ok(w.abilities.train(10,0,"war.low",false));ok(w.abilities.startResearch(10,"lead.low"));w.city(10).owner=1;for(World.Officer o:w.officers)if(o.owner==0)o.cityId=11;w.abilities.cleanup();check(w.abilities.training().isEmpty()&&w.abilities.research(0)==null&&w.abilities.remaining(0,"war.low")==5,"lost city cancels training and research without granting results");SaveCodec.validate(w);
        try(InputStream in=AbilityResearchTest.class.getResourceAsStream("/legacy-v9.sg11.b64")){World old=SaveCodec.decode(Base64.getMimeDecoder().decode(in.readAllBytes()));check(old.campaign.projects().size()==1&&old.abilities.training().isEmpty(),"real v9 running legacy project preserved");check(ByteBuffer.wrap(bytes(old),4,4).getInt()==25,"migrates v9 to v18");for(int i=0;i<3;i++)tick(old);check(old.officer(3001).aptitude[0]==2&&old.campaign.projects().isEmpty(),"legacy task finishes original reward once");}
        World broken=fixture();unlock(broken,0,"war.low");ok(broken.abilities.train(10,0,"war.low",false));broken.officer(0).otherTask="";invalid(broken);
    }
    private static void allOrdinaryNodes()throws Exception{
        for(AbilityResearch.Node n:AbilityResearch.catalog())if(n.slot.isEmpty()){
            World w=fixture();for(String p:n.prerequisites)unlock(w,0,p);ok(w.abilities.startResearch(10,n.id));for(int i=0;i<n.turns;i++)tick(w);check(w.abilities.learned(0,n.id),"node actual research completion: "+n.id);
            if(n.category==AbilityResearch.Category.SKILL&&!AbilityResearch.skillAvailable(n.skill))continue;
            if(n.category==AbilityResearch.Category.APTITUDE)w.officer(0).aptitude[n.index]=n.cap-1;
            if(n.category==AbilityResearch.Category.STAT)AbilityResearch.setStat(w.officer(0),n.index,n.cap-1);
            ok(w.abilities.train(10,0,n.id,false));for(int i=0;i<3;i++)tick(w);check(w.abilities.remaining(0,n.id)==n.uses-1,"node real use: "+n.id);check(Arrays.equals(bytes(w),bytes(SaveCodec.decode(bytes(w)))),"node saved reward: "+n.id);
        }
    }
    private static void economy()throws Exception{
        World w=fixture();w.city(10).order=100;int g=w.domestic.monthlyGold(10),f=w.domestic.monthlyFood(10);check(w.domestic.goldIncome(10,1)==0&&w.domestic.goldIncome(10,3)==g,"monthly gold");check(w.domestic.foodIncome(10,3)==0&&w.domestic.foodIncome(10,9)==f,"seasonal food");
        w.officer(0).skillId=Skill.ZHENGSHUI.id;w.officer(1).skillId=Skill.FUHAO.id;check(w.domestic.goldIncome(10,1)+w.domestic.goldIncome(10,2)+w.domestic.goldIncome(10,3)==g*7/4,"tax wealth combination totals 1.75, not 2.25");
        w.officer(2).skillId=Skill.ZHENGSHOU.id;w.officer(3).skillId=Skill.MIDAO.id;check(w.domestic.foodIncome(10,3)+w.domestic.foodIncome(10,6)+w.domestic.foodIncome(10,9)==f*7/4,"levy grain combination applies boost at season only");
        w.officer(4).skillId=Skill.FUHAO.id;check(w.domestic.goldIncome(10,3)==g*3/4,"same skill does not stack");
        w.officer(1).cityId=20;w.officer(1).owner=1;w.officer(4).cityId=20;w.officer(4).owner=1;check(w.domestic.goldIncome(10,3)==g/2,"leaving removes city bonus");
    }
    private static World.Unit unit(World w,int officer,World.Weapon weapon,Hex hex){World.Officer o=w.officer(officer);World.Unit u=new World.Unit(w.nextUnitId++,o.owner,officer,weapon,hex,5000,20000);o.cityId=-1;o.unitId=u.id;w.units.add(u);return u;}
    private static void combatChanges()throws Exception{
        World w=fixture();World.Unit rider=unit(w,0,World.Weapon.CAVALRY,new Hex(6,5)),target=unit(w,10,World.Weapon.SPEAR,new Hex(8,5));reject(w,()->w.attack(rider.id,target.id));w.officer(0).skillId=Skill.BAIMA.id;int troops=rider.troops;ok(w.attack(rider.id,target.id));check(target.troops<5000&&rider.troops==troops,"white horse opens actual ranged normal without melee counter");check(w.combat.critical(rider,target,false),"white horse ranged critical");
        World high=fixture();high.campaign.learned.put(0,EnumSet.of(Campaign.Tech.LOGISTICS));high.city(10).morale=119;ok(high.train(10,0));check(high.city(10).morale==120,"veteran city trains up to 120");high.officer(1).acted=false;ok(high.deploy(10,1,World.Weapon.SPEAR,3000));World.Unit veteran=high.unit(1);veteran.hex=new Hex(6,5);World.Unit foe=unit(high,10,World.Weapon.SPEAR,new Hex(7,5));check(veteran.energy==120,"deploy retains 120 energy");ok(high.attack(veteran.id,foe.id));check(veteran.energy==120,"damage adapter retains real PK energy");check(Arrays.equals(bytes(high),bytes(SaveCodec.decode(bytes(high)))),"120 energy battle survives save");
        World ranges=fixture();World.Unit caster=unit(ranges,0,World.Weapon.SPEAR,new Hex(6,5));check(ranges.war.plotRange(caster.id,War.Plot.FIRE)==1,"base fire range");ranges.campaign.learned.put(0,EnumSet.of(Campaign.Tech.FIRE_MASTERY));check(ranges.war.plotRange(caster.id,War.Plot.FIRE)==3,"divine fire expands to three");ranges.officer(0).skillId=Skill.GUIMOU.id;check(ranges.war.plotRange(caster.id,War.Plot.FIRE)==4,"ghost strategy stacks range");
        int supports=0,wounds=0,blocks=0;
        for(int seed=0;seed<80;seed++){
            World base=fixture();World.Unit a=unit(base,0,World.Weapon.CROSSBOW,new Hex(6,5)),b=unit(base,10,World.Weapon.HALBERD,new Hex(8,5)),helper=unit(base,1,World.Weapon.SPEAR,new Hex(8,4));base.strategy.setSeed(seed);World support=SaveCodec.decode(bytes(base));support.officer(1).skillId=Skill.FUZUO.id;support.unit(helper.id).acted=true;
            ok(base.attack(a.id,b.id));ok(support.attack(a.id,b.id));if(support.unit(b.id).troops<base.unit(b.id).troops)supports++;check(support.unit(helper.id).acted,"support does not reset used action");
            World shield=fixture();World.Unit sa=unit(shield,0,World.Weapon.CROSSBOW,new Hex(6,5)),sb=unit(shield,10,World.Weapon.HALBERD,new Hex(8,5));shield.campaign.learned.put(1,EnumSet.of(Campaign.Tech.HALBERD_DRILL,Campaign.Tech.SHIELD));int hit=shield.combat.physicalDamage(sa,sb,1,false,new Random(seed));if(hit==0)blocks++;check(shield.combat.physicalDamage(sa,sb,1,true,new Random(seed))>0,"arrow shield never blocks tactics");
            World injured=fixture();World.Unit ma=unit(injured,0,World.Weapon.SPEAR,new Hex(6,5)),mb=unit(injured,10,World.Weapon.SPEAR,new Hex(7,5));injured.officer(0).skillId=Skill.MENGZHE.id;injured.strategy.setSeed(seed);ok(injured.war.tactic(ma.id,mb.id,War.Tactic.THRUST));if(injured.contests.injury(10)>0){wounds++;check(!mb.hex.equals(new Hex(7,5)),"wound requires actual displacement");check(Arrays.equals(bytes(injured),bytes(SaveCodec.decode(bytes(injured)))),"field wound saved");for(int t=0;t<3;t++)tick(injured);check(injured.contests.injury(10)==0,"field wound heals after three turns");}
        }
        check(supports>0&&supports<80&&wounds>0&&wounds<80&&blocks>0&&blocks<80,"probabilistic hooks both trigger and miss");
        World movement=fixture();World.Unit foot=unit(movement,0,World.Weapon.SPEAR,new Hex(6,5));int before=movement.war.movement(foot);movement.officer(0).skillId=Skill.QIANGXING.id;check(movement.war.movement(foot)==before+1,"forced march enters actual movement budget");
    }
    public static void main(String[] args)throws Exception{graph();research();training();limits();lossAndLegacy();allOrdinaryNodes();economy();combatChanges();System.out.println("PASS: "+checks+" PK assertions: full ordinary graph, hidden stability, real research/training/limits/cancellation/capture, v9 migration and income schedule.");}
}
