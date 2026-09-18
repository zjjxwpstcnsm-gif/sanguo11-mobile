package game.sanguo.core;

import java.util.*;
import static game.sanguo.core.Skill.*;

public final class ArchitectureRulesTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static void skill(World w,int officer,Skill s){w.officer(officer).skillId=s.id;}
    public static void main(String[] args)throws Exception{numericalBaseline();holders();energyAndHit();pureQueries();fireReplay();music();splashAndTrap();roster();auraScope();aiSharedResults();System.out.println("PASS: "+checks+" v033 architecture/rule assertions.");}
    private static void numericalBaseline(){
        // v0.41 intentionally replaces the v0.33 frozen formula. Keep broad numeric
        // contracts here; gameplay role/tempo benchmarks live in BalanceTest.
        Random r=new Random(330);World w=ArchitectureFixture.create();
        for(int i=0;i<2000;i++){
            World.Weapon aw=World.Weapon.values()[i%4],bw=World.Weapon.values()[(i/4)%4];
            World.Unit a=new World.Unit(1,0,0,aw,new Hex(6,6),1+r.nextInt(18000),50000),b=new World.Unit(2,1,3,bw,new Hex(7,6),10000,50000);
            for(World.Officer o:w.officers){o.leadership=1+r.nextInt(100);o.war=1+r.nextInt(100);o.intelligence=1+r.nextInt(100);}
            World.Terrain t=i%3==0?World.Terrain.FOREST:i%3==1?World.Terrain.MOUNTAIN:World.Terrain.PLAIN;w.terrain[6][6]=t;w.terrain[7][6]=t;
            double scale=.5+(i%5)*.3;long seed=r.nextLong();
            int full=w.combat.rawDamage(a,b,scale,new Random(seed));
            check(full>=1&&full<=2500,"bounded physical damage "+i);
            check(full==w.combat.rawDamage(a,b,scale,new Random(seed)),"deterministic physical roll "+i);
            a.troops=Math.max(1,a.troops/2);
            check(w.combat.rawDamage(a,b,scale,new Random(seed))<=full,"smaller force cannot increase damage "+i);
        }
    }
    private static void holders(){
        World w=ArchitectureFixture.create();World.Unit a=w.unit(1),b=w.unit(2);w.officer(0).war=110;w.officer(1).war=60;skill(w,1,SHENJIANG);
        check(!w.combat.critical(a,b,false),"low deputy cannot borrow commander's strength");w.officer(1).war=100;
        check(w.combat.critical(a,b,false)&&w.combat.critical(a,b,true),"deputy god-general normal/tactic");
        int single=w.combat.physicalDamage(a,b,1,false,new Random(7));skill(w,2,QUZHU);w.officer(2).war=110;
        check(w.combat.physicalDamage(a,b,1,false,new Random(7))==single,"two critical skills never multiply twice");
        w.terrain[a.hex.q][a.hex.r]=World.Terrain.WATER;check(!w.combat.critical(a,b,true),"god-general no naval tactics");check(w.combat.critical(a,b,false),"naval normal drive-away condition retained");
        w.officer(1).war=w.officer(2).war=90;check(!w.combat.critical(a,b,false),"equal ability is not greater");
        check(!w.combat.critical(a,null,false),"unit comparison cannot apply to structure");
    }
    private static void energyAndHit(){
        World w=ArchitectureFixture.create();World.Unit a=w.unit(1),b=w.unit(2);skill(w,1,WEIFENG);skill(w,2,SAOTAO);b.energy=11;
        w.combatEffects.hit(a,b,100,true,true);check(b.energy==0,"weifeng priority clamps lower bound");
        b.energy=50;w.combatEffects.hit(a,b,0,true,true);check(b.energy==50,"zero damage no on-hit");
        w.combatEffects.hit(a,b,100,true,true);w.combatEffects.hit(a,b,400,false,false);check(b.energy==30,"physical plus fire drains once");
        EnergyRules.Change c=w.energy.change(b,200,EnergyRules.Reason.MUSIC);check(c.actual==70&&c.after==100,"actual recovery rather than requested amount");
        check(w.energy.change(b,20,EnergyRules.Reason.MUSIC).actual==0,"capped recovery");
        b.gold=123;b.food=456;w.combatEffects.hit(a,b,20000,false,true);int gold=a.gold,food=a.food;
        w.combatEffects.hit(a,b,20000,false,true);w.defeatUnit(b,a);check(a.gold==gold&&a.food==food&&w.unit(2)==null,"duplicate hit/defeat cannot loot twice");
    }
    private static void pureQueries()throws Exception{
        World w=ArchitectureFixture.create();World.Unit a=w.unit(1),b=w.unit(2);skill(w,1,WEIFENG);byte[] before=bytes(w);long revision=w.commandRevision();
        for(int i=0;i<30;i++){w.war.attackPreview(1,2);w.war.tacticPreview(1,2,War.Tactic.FIRE_ARROW);w.combat.firePreview(a,b,CombatRules.DIRECT_FIRE_BASE,false);w.energy.recovery(a);new CampaignAi(w).bestAction(1,true);}
        check(w.commandRevision()==revision,"queries preserve transient command revision");
        check(Arrays.equals(before,bytes(w)),"all previews and AI preserve state, log and RNG");
        CombatRules.DamageRange range=w.combat.preview(a,b,1,false);
        for(int i=0;i<250;i++){int hit=w.combat.physicalDamage(a,b,1,false,new Random(i));check(hit>=range.min&&hit<=range.max,"preview bounds enclose execution");}
        w.orders.reset(a);check(Arrays.equals(bytes(w),bytes(SaveCodec.decode(bytes(w)))),"save excludes temporary calculators");
    }
    private static void fireReplay()throws Exception{
        World w=ArchitectureFixture.create();World.Unit a=w.unit(1),b=w.unit(2);skill(w,1,HUOSHEN);skill(w,4,TENGJIA);
        check(w.combat.fireDamage(b,400,0,2,false)==1600,"fire god and wicker combine on fire only");skill(w,5,HUOSHEN);check(w.combat.fireDamage(b,400,0,2,true)==0,"fire immunity wins over wicker");
        skill(w,5,TAPO);check(w.combat.fireDamage(b,700,0,2,true)==1400,"trap reduction after multipliers");
        w.war.ignite(b.hex,a);w.removeUnit(a);World restored=SaveCodec.decode(bytes(w));w.war.tick();restored.war.tick();check(Arrays.equals(bytes(w),bytes(restored)),"source death and reload preserve fire power and continuation");
    }
    private static void music()throws Exception{
        World w=ArchitectureFixture.create();World.Unit a=w.unit(1);a.energy=40;skill(w,1,SHIXIANG);skill(w,2,ZOUYUE);
        War.Structure first=new War.Structure(1,0,War.StructureKind.MUSIC,new Hex(6,5),800),second=new War.Structure(2,0,War.StructureKind.MUSIC,new Hex(5,6),800);w.war.structures.add(first);w.war.structures.add(second);w.war.nextStructureId=3;
        check(w.energy.recovery(a)==20,"overlapping music plus poetry, music wins over music skill");first.complete=second.complete=false;check(w.energy.recovery(a)==5,"unfinished buildings not active");first.complete=true;second.hp=400;
        w.energy.settleTurn();check(a.energy==60,"one global recovery");byte[] before=bytes(w);World restored=SaveCodec.decode(before);check(restored.unit(1).energy==60&&Arrays.equals(before,bytes(restored)),"load never applies recovery");
        a.hex=new Hex(12,12);check(w.energy.recovery(a)==5,"movement immediately leaves aura");a.hex=new Hex(6,6);w.war.structures.remove(first);check(w.energy.recovery(a)==5,"destruction immediately removes aura");
        second.complete=true;w.war.structures.clear();w.war.structures.add(new War.Structure(3,1,War.StructureKind.MUSIC,new Hex(5,6),800));check(w.energy.recovery(a)==5,"enemy music excluded");
    }
    private static void splashAndTrap()throws Exception{
        World w=ArchitectureFixture.create();World.Unit a=w.unit(1),b=w.unit(2);skill(w,1,WEIFENG);w.campaign.finishTech(0,Campaign.Tech.THUNDERBOLT);
        w.fieldworks.stoneSplash(a,new Hex(8,6));check(b.energy==60,"thunderbolt collateral receives one weifeng drain");
        w=ArchitectureFixture.create();a=w.unit(1);b=w.unit(2);skill(w,1,HUOSHEN);
        War.Structure trap=new War.Structure(1,0,War.StructureKind.INFERNO_SEED,new Hex(8,6),200);w.war.structures.add(trap);w.war.nextStructureId=2;
        byte[] before=bytes(w);String preview=w.fieldworks.ignitionPreview(a,trap.hex);check(preview.contains("3000")&&Arrays.equals(before,bytes(w)),"inferno preview uses shared base without mutation");
        w.fieldworks.ignite(trap.hex,a);check(b.troops==7000,"inferno execution matches primary preview");
        check(w.war.fires.stream().allMatch(f->f.power==2&&f.owner==0&&f.trap),"chain retains source snapshot");
    }
    private static void roster(){
        World w=ArchitectureFixture.create();World.Officer original=w.officer(1);w.officers.remove(original);check(w.officer(1)==null,"removed officer index invalidates");
        w.officers.add(0,original);check(w.officer(1)==original,"inserted officer index invalidates");
        Collections.reverse(w.officers);check(w.officer(1)==original,"reordering preserves stable identity");
    }
    private static void auraScope(){
        World w=ArchitectureFixture.create();World.Unit a=w.unit(1),b=w.unit(2);War.Structure camp=new War.Structure(1,1,War.StructureKind.FORTRESS,new Hex(9,6),2000);w.war.structures.add(camp);
        int live=w.combat.expectedDamage(a,b,1,false);check(w.fieldworks.queryAuras(()->w.combat.expectedDamage(a,b,1,false))==live,"bounded AI aura query matches live calculation");
        w.war.structures.set(0,new War.Structure(1,0,War.StructureKind.FORTRESS,camp.hex,2000));check(w.fieldworks.queryAuras(()->w.combat.expectedDamage(a,b,1,false))>live,"next query sees immediate ownership change");
        try{w.fieldworks.queryAuras(()->{throw new IllegalStateException("test");});}catch(IllegalStateException expected){}
        w.war.structures.set(0,camp);check(w.combat.expectedDamage(a,b,1,false)==live,"exception clears query cache");
    }
    private static void aiSharedResults()throws Exception{
        World w=ArchitectureFixture.create();World.Unit a=w.unit(1),b=w.unit(2);skill(w,1,HUOSHEN);
        int physical=w.combat.expectedDamage(a,b,1.3,true);check(w.combat.expectedWithFire(a,b,1.3,true)==physical+800,"AI includes the real elemental component");
        skill(w,4,HUOSHEN);check(w.combat.expectedWithFire(a,b,1.3,true)==physical,"AI fire immunity retains physical component");
        b.hex=new Hex(15,10);w.campaign.finishTech(0,Campaign.Tech.SIEGE_LADDERS);War.Structure s=new War.Structure(1,1,War.StructureKind.STONE_WALL,new Hex(7,6),1000);w.war.structures.add(s);w.war.nextStructureId=2;
        CampaignAi.Action choice=new CampaignAi(w).bestAction(a.id,true);check(choice!=null&&choice.kind==CampaignAi.Kind.STRUCTURE&&choice.score==300+Math.min(s.hp,w.combat.structureDamage(a,false)),"AI structure value includes actual construction technology");
    }
}
