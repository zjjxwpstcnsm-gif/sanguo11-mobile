package game.sanguo.core;

import java.io.*;
import java.util.*;

public final class GovernmentTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static void ok(World.Result r){check(r.ok,r.message);}
    static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    static World copy(World w)throws Exception{return SaveCodec.decode(bytes(w));}
    static void rejected(World w,java.util.function.Supplier<World.Result> command)throws Exception{
        byte[] before=bytes(w);check(!command.get().ok,"command rejected");check(Arrays.equals(before,bytes(w)),"rejection preserves complete state and RNG");
    }
    static World world(){
        World w=new World(18,12,"甲","乙","丙");
        w.cities.add(new World.City(0,"甲城",new Hex(1,1),0));w.cities.add(new World.City(1,"后方",new Hex(2,9),0));
        w.cities.add(new World.City(2,"乙城",new Hex(14,1),1));w.cities.add(new World.City(3,"乙后方",new Hex(14,9),1));
        w.cities.add(new World.City(4,"丙城",new Hex(17,6),2));
        for(int i=0;i<12;i++)w.officers.add(new World.Officer(i,"将"+i,i<6?0:i<11?1:2,i<6?0:i<11?2:4,80,80,80,80,80));
        w.officer(0).role=Strategy.Role.RULER;w.officer(0).loyalty=100;
        w.officer(6).role=Strategy.Role.RULER;w.officer(6).loyalty=100;
        w.officer(11).role=Strategy.Role.RULER;w.officer(11).loyalty=100;
        for(World.City c:w.cities){c.gold=50000;c.food=200000;c.troops=50000;Arrays.fill(c.equipment,0);for(int j=0;j<4;j++)c.equipment[j]=50000;}
        return w;
    }
    static World.Unit unit(World w,int officer,int owner,Hex hex,int troops,int... deputies){
        World.Unit u=new World.Unit(w.nextUnitId++,owner,officer,World.Weapon.SPEAR,hex,troops,20000);u.deputies=deputies;w.units.add(u);
        for(World.Officer o:w.army.crew(u)){o.cityId=-1;o.unitId=u.id;}return u;
    }
    static void refresh(World w){w.actionPoints[w.active]=60;for(World.Officer o:w.officers)if(o.owner==w.active)o.acted=false;for(World.Unit u:w.units)if(u.owner==w.active)u.acted=false;}
    public static void main(String[] args)throws Exception{
        ranks();captivity();friendlyFire();releaseAndRescue();administration();migration();longCampaigns();
        System.out.println("PASS: "+checks+" governance assertions: rank caps/payroll, captive crew and immunity, recruit/release/ransom/rescue, delegation, real v7 migration and long campaigns.");
    }
    static void ranks()throws Exception{
        World w=world();check(Government.ranks().size()==40,"40 named military offices");
        rejected(w,()->w.government.appointRank(0,0,1,"大都督"));
        w.government.earn(1,36000);ok(w.government.appointRank(0,0,1,"大都督"));
        check(w.government.commandLimit(1)==15000&&w.officer(1).acted,"office expands cap and consumes target action");
        refresh(w);w.government.earn(2,36000);rejected(w,()->w.government.appointRank(0,0,2,"大都督"));
        ok(w.army.deploy(0,1,new int[]{2},World.Weapon.SPEAR,Army.Ship.BOAT,15000,30000));
        check(copy(w).unit(1).troops==15000,"15000 troops save without truncation");
        World.Unit enemy=unit(w,7,1,new Hex(3,1),8000);w.unit(1).hex=new Hex(2,1);ok(w.attack(1,enemy.id));
        check(enemy.troops<8000,"rank-sized unit uses actual tactical damage adapter");
        refresh(w);rejected(w,()->w.government.removeRank(0,0,1));
        w.unit(1).hex=new Hex(2,1);ok(w.enter(1,0));refresh(w);ok(w.government.removeRank(0,0,1));
        refresh(w);ok(w.government.appointRank(0,0,1,"偏将军"));refresh(w);
        rejected(w,()->w.army.deploy(0,1,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,9000,18000));
        w.turn=3;int gold=w.city(0).gold;w.government.tick();check(w.city(0).gold==gold-20,"rank payroll debits treasury once per settlement");
        World clone=copy(w);check(clone.government.office(1).id.equals("偏将军"),"stable rank ID restores");
        clone.government.ranks.put(2,"偏将军");invalid(clone,"duplicate same-side office rejected");
    }
    static void captivity()throws Exception{
        World w=world();World.Unit a=unit(w,1,0,new Hex(4,4),5000),b=unit(w,7,1,new Hex(5,4),1,8,9);
        w.officer(1).skillId=Skill.BOFU.id;w.officer(9).skillId=Skill.QIANGYUN.id;
        ok(w.attack(a.id,b.id));check(w.unit(b.id)==null&&w.government.prisoners().size()==2,"defeat captures leader and deputy, strong luck escapes");
        check(!w.government.captive(9)&&w.officer(9).cityId>=0,"immune officer retreats");
        check(w.officer(7).owner==1&&w.officer(7).cityId==-1&&w.strategy.officerState(7).activity==Strategy.Activity.CAPTIVE,"captivity retains old allegiance and has distinct state");
        World c=copy(w);check(Arrays.equals(bytes(w),bytes(c)),"all prisoner metadata round trips");
        w.active=1;rejected(w,()->w.government.summon(2,7));w.active=0;
        a.hex=new Hex(2,1);refresh(w);ok(w.enter(a.id,0));
        Government.Prisoner p=w.government.prisoner(7);w.officer(7).loyalty=0;refresh(w);
        if(p.cityId!=0){w.officer(0).cityId=p.cityId;}
        World expected=copy(w);int chance=w.government.recruitChance(0,7);boolean succeeds=expected.strategy.nextInt(100)<chance;
        ok(w.government.recruitPrisoner(p.cityId,0,7));check((w.officer(7).owner==0)==succeeds,"recruit result follows saved RNG");
        if(!succeeds){refresh(w);rejected(w,()->w.government.recruitPrisoner(p.cityId,0,7));}
        check(Arrays.equals(bytes(w),bytes(copy(w))),"post-recruit state valid");
        World shield=world();a=unit(shield,1,0,new Hex(4,4),5000);b=unit(shield,7,1,new Hex(5,4),1,8);
        shield.officer(1).skillId=Skill.BOFU.id;shield.officer(8).skillId=Skill.XUELU.id;
        ok(shield.attack(a.id,b.id));check(shield.government.prisoners().isEmpty(),"blood road protects whole crew from capture skill");
        World royal=world();royal.government.capture(royal.officer(6),royal.city(0));
        check(royal.government.recruitChance(0,6)==0,"living ruler cannot be recruited");rejected(royal,()->royal.government.recruitPrisoner(0,0,6));
        World orphan=world();orphan.government.capture(orphan.officer(7),orphan.city(0));orphan.government.prisoner(7).cityId=2;invalid(orphan,"hostile prison ownership rejected");
    }
    static void releaseAndRescue()throws Exception{
        World w=world();w.government.capture(w.officer(7),w.city(0));int relation=w.strategy.factionRelation(0,1);
        ok(w.government.release(0,0,7));check(w.officer(7).cityId==2&&!w.government.captive(7)&&w.strategy.factionRelation(0,1)==relation+10,"release and relationship result");
        w.government.capture(w.officer(1),w.city(2));refresh(w);int before=w.city(0).gold+w.city(2).gold;
        ok(w.government.ransom(0,0,1));check(w.city(0).gold+w.city(2).gold==before&&w.officer(1).cityId==1,"ransom conserves gold and returns own officer");
        w.government.capture(w.officer(7),w.city(0));w.city(0).owner=1;
        // Emulate the capture command's defender evacuation before validating the full state.
        for(World.Officer o:w.officers)if(o.owner==0&&o.cityId==0)w.retreat(o,w.city(0).hex);
        w.government.relocatePrisoners();check(!w.government.captive(7)&&w.officer(7).cityId==0,"conquering original jail rescues officer");copy(w);
        World displaced=world();displaced.government.capture(displaced.officer(7),displaced.city(0));displaced.city(0).owner=2;
        displaced.government.relocatePrisoners();check(displaced.government.prisoner(7).cityId==1,"lost prison relocates to remaining captor city");
        displaced.city(1).owner=2;displaced.government.relocatePrisoners();check(!displaced.government.captive(7)&&displaced.officer(7).cityId==2,"no remaining prison releases captive safely");
    }
    static void friendlyFire()throws Exception{
        World w=world();World.Unit a=new World.Unit(w.nextUnitId++,0,1,World.Weapon.CROSSBOW,new Hex(4,4),5000,20000);
        w.units.add(a);w.officer(1).cityId=-1;w.officer(1).unitId=a.id;w.officer(1).aptitude[2]=3;w.officer(1).skillId=Skill.BOFU.id;
        World.Unit enemy=unit(w,7,1,new Hex(5,4),5000),friend=unit(w,2,0,new Hex(5,5),1);
        enemy.status=War.Status.CONFUSED;enemy.statusTurns=1;
        ok(w.war.tactic(a.id,enemy.id,War.Tactic.VOLLEY));
        check(w.unit(friend.id)==null&&!w.government.captive(2)&&w.officer(2).owner==0,"lethal friendly splash retreats ally without self-capture");copy(w);
    }
    static void administration()throws Exception{
        World w=world();ok(w.government.appointAdvisor(0,0,1));String snapshot=Arrays.toString(bytes(w));String advice=w.government.advice(0);
        check(advice.startsWith("将1建议")&&snapshot.equals(Arrays.toString(bytes(w))),"advisor preview is read-only");
        refresh(w);ok(w.government.summon(1,2));check(w.domestic.busy(2)&&w.officer(2).cityId==-1,"summon is a real travel mission");
        rejected(w,()->w.government.summon(1,2));
        refresh(w);ok(w.government.delegate(0,0,Government.Policy.ECONOMY));int gold=w.city(0).gold,ap=w.actionPoints[0];w.government.runDelegated();
        check(w.domestic.facilities.size()==1&&w.city(0).gold<gold&&w.actionPoints[0]==ap-10,"delegation spends real resources and AP");
        World c=copy(w);check(c.government.policy(0)==Government.Policy.ECONOMY&&c.government.advisor(0).id==1,"administration persists");
        w.government.allegianceChanged(1);check(w.government.advisor(0)==null,"office cleared on allegiance change");
    }
    static void migration()throws Exception{
        byte[] old;try(InputStream in=GovernmentTest.class.getResourceAsStream("/legacy-v7.sg11.b64")){old=Base64.getMimeDecoder().decode(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
        check(old[7]==7,"fixture generated by unmodified v0.8 writer");World w=SaveCodec.decode(old);byte[] upgraded=bytes(w);
        check(upgraded[7]==17&&w.government.prisoners().isEmpty()&&w.government.ranks.isEmpty(),"v7 upgrades with empty new state");
        check(Arrays.equals(upgraded,bytes(SaveCodec.read(new ByteArrayInputStream(upgraded)))),"document import shares exact save decoder");
        try{SaveCodec.read(new ByteArrayInputStream(new byte[4*1024*1024+21]));throw new AssertionError("oversize accepted");}catch(IOException expected){checks++;}
        byte[] corrupt=upgraded.clone();corrupt[corrupt.length-1]^=1;try{SaveCodec.read(new ByteArrayInputStream(corrupt));throw new AssertionError("bad CRC accepted");}catch(IOException expected){checks++;}
    }
    static void invalid(World w,String label)throws Exception{try{bytes(w);throw new AssertionError(label);}catch(IOException expected){checks++;}}
    static void longCampaigns()throws Exception{
        for(int player=0;player<3;player++){
            World w=ScenarioCatalog.load("regional-sandbox",player),c=copy(w);
            for(int turn=0;turn<80&&!w.gameOver();turn++){
                ok(w.nextTurn());ok(c.nextTurn());check(Arrays.equals(bytes(w),bytes(c)),"long campaign deterministic after each save");c=copy(c);
            }
        }
    }
}
