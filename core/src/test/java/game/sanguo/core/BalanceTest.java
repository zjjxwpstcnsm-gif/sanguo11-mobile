package game.sanguo.core;

import java.util.*;

/** Gameplay contracts: real siege commands, relative unit roles and interrupted repair. */
public final class BalanceTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static World fixture(World.Weapon weapon){
        World w=new World(22,16);
        w.cities.add(new World.City(10,"我城",new Hex(3,6),0));
        w.cities.add(new World.City(20,"敌港",new Hex(14,6),1));
        w.cities.add(new World.City(30,"敌城",new Hex(19,12),1));
        World.City port=w.city(20);port.kind=World.SiteKind.PORT;port.baseDefense=2000;port.defense=2000;
        w.officers.add(new World.Officer(1,"统军",0,10,80,80,70,80,70));
        w.officers.add(new World.Officer(2,"文官",0,10,70,60,80,80,80));
        w.officers.add(new World.Officer(3,"守将",1,20,80,80,70,80,70));
        for(World.Officer o:w.officers)Arrays.fill(o.aptitude,2);
        World.Unit u=new World.Unit(1,0,1,weapon,new Hex(13,6),10000,30000);
        w.units.add(u);w.nextUnitId=2;w.officer(1).cityId=-1;w.officer(1).unitId=1;
        return w;
    }
    private static int attackPort(World.Weapon weapon)throws Exception{
        World w=fixture(weapon);World.City port=w.city(20);World.Unit u=w.unit(1);int hits=0;
        while(port.owner!=0&&hits<25){
            byte[] before=SaveCodec.encode(w);boolean engine=Army.siegeWeapon(weapon);
            int expected=Math.min(port.defense,w.combat.siege(u,port,engine).wall),wall=port.defense;
            w.combat.siegePreview(u,port,engine);check(Arrays.equals(before,SaveCodec.encode(w)),"preview is pure");
            World.Result r=engine?w.army.tactic(1,port.hex,w.army.tactics(u).get(0)):w.siege(1,20);
            check(r.ok,"siege command "+weapon+": "+r.message);hits++;
            if(port.owner!=0)check(wall-port.defense==expected,"preview equals actual including tactic payment");
            check(u.acted,"one attack consumes one action");u.acted=false;
            SaveCodec.validate(w);
        }
        check(port.owner==0,"port can be captured");
        check(port.defense==w.campaign.defenseCap(port)/4,"capture leaves damaged walls");
        return hits;
    }
    private static void siege()throws Exception{
        int normal=attackPort(World.Weapon.SPEAR),ram=attackPort(World.Weapon.RAM);
        check(normal>=4&&normal<=6,"10k infantry takes port in 4–6 attacks, got "+normal);
        check(ram>=2&&ram<=3&&ram<normal,"engines breach faster, got "+ram);
        World w=fixture(World.Weapon.SPEAR);World.Unit u=w.unit(1);World.City c=w.city(20);
        int port=w.combat.siege(u,c,false).wall;c.kind=World.SiteKind.CITY;int city=w.combat.siege(u,c,false).wall;
        c.kind=World.SiteKind.GATE;int gate=w.combat.siege(u,c,false).wall;
        check(port>city&&city>gate,"port < city < gate wall resistance");
        int full=w.combat.siege(u,c,false).wall;u.troops=1000;check(w.combat.siege(u,c,false).wall<full/2,"small forces do not keep full siege power");
        World engine=fixture(World.Weapon.RAM);full=engine.combat.siege(engine.unit(1),engine.city(20),true).wall;engine.unit(1).troops=1;
        check(engine.combat.siege(engine.unit(1),engine.city(20),true).wall<full/20,"one soldier cannot run a full strength engine");
        System.out.println("BALANCE: 10k spear/2000 port="+normal+" attacks; ram="+ram+"; spear wall damage port/city/gate="+port+"/"+city+"/"+gate);
    }
    private static void repair()throws Exception{
        World w=fixture(World.Weapon.SPEAR);World.City c=w.city(20);c.defense=800;
        check(w.cityDefense.besieged(c)&&w.cityDefense.recovery(c)==0,"nearby hostile stops natural repair");
        int blocked=w.cityDefense.repairAmount(c,w.officer(3));w.unit(1).hex=new Hex(8,6);
        check(!w.cityDefense.besieged(c)&&w.cityDefense.recovery(c)==20,"peace recovers only twenty");
        check(w.cityDefense.repairAmount(c,w.officer(3))==blocked*4,"siege reduces manual repair to one quarter");
        c.food=0;check(w.cityDefense.recovery(c)==0,"no food means no free wall repair");c.food=20000;
        w.unit(1).hex=new Hex(11,6);check(w.cityDefense.besieged(c),"ranged siege at three tiles blocks repair");
        w.campaign.concludeTreaty(0,1,Campaign.TreatyKind.ALLIANCE,12);check(!w.cityDefense.besieged(c),"allies do not besiege");
        w.campaign.treaties.clear();w.active=1;int wall=c.defense,gold=c.gold,ap=w.actionPoints[1];
        check(w.campaign.repair(20,3).ok,"actual repair command");check(c.defense-wall==blocked&&gold-c.gold==300&&ap-w.actionPoints[1]==10,"repair preview and paid effect match");
        World replay=SaveCodec.decode(SaveCodec.encode(w));check(replay.cityDefense.recovery(replay.city(20))==0,"save/load does not resume repair in siege");
        World turn=fixture(World.Weapon.SPEAR);for(World.City base:turn.cities){base.gold=0;base.troops=0;}
        turn.city(20).defense=800;check(turn.nextTurn().ok,"complete besieged global turn");
        check(turn.city(20).defense==800,"global settlement actually suppresses besieged repair");
        turn.unit(1).hex=new Hex(7,6);check(turn.nextTurn().ok,"complete peaceful global turn");
        check(turn.city(20).defense==820,"global settlement resumes twenty-point repair after withdrawal");
    }
    private static void ratings(){
        World w=fixture(World.Weapon.SPEAR);World.Unit a=w.unit(1);
        double high=w.combat.attackRating(a),defense=w.combat.defenseRating(a);w.officer(1).leadership=40;
        check(high>w.combat.attackRating(a)*1.2&&defense>w.combat.defenseRating(a)*1.4,"leadership matters to both attack and defense");
        double low=w.combat.attackRating(a);w.officer(1).war=100;check(w.combat.attackRating(a)>low,"war improves offense");
        World.Unit b=new World.Unit(2,1,3,World.Weapon.HALBERD,new Hex(12,6),10000,30000);w.units.add(b);
        check(w.combat.spiralConfusionChance(a,b)<=40,"ordinary spiral cannot lock every action");
        w.officer(1).skillId=Skill.QIANGSHEN.id;check(w.combat.spiralConfusionChance(a,b)==100,"spear god keeps guaranteed critical control");
        w.officer(1).skillId="none";w.officer(1).leadership=80;w.officer(1).war=80;
        int first=w.combat.preview(a,b,1,false).estimate;w.officer(1).aptitude[0]=0;
        check(w.combat.preview(a,b,1,false).estimate<first,"aptitude changes damage as well as tactic access");
    }
    public static void main(String[] args)throws Exception{siege();repair();ratings();System.out.println("PASS: "+checks+" balance contracts.");}
}
