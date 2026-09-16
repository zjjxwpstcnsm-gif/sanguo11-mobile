package game.sanguo.core;

import java.util.*;

/** Invariants on actual campaign commands, not tests of an independent combat demo. */
public final class DisplacementTest {
    private static int checks;
    private static void check(boolean b,String reason){checks++;if(!b)throw new AssertionError(reason);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    public static void main(String[] args)throws Exception{
        boundaries();terrainCapabilities();trapAndFire();killsAndIdentity();transport();ai();
        System.out.println("PASS: "+checks+" v029 displacement assertions: authoritative previews, blocked steps, hazards, protected parties, kill/cargo identity, deterministic AI and replay.");
    }
    private static void boundaries()throws Exception{
        for(String scene:DisplacementFixture.CASES){
            World w=DisplacementFixture.create(scene);World.Unit a=w.unit(1),b=w.unit(2);War.Tactic tactic=DisplacementFixture.tactic(scene);boolean sea=scene.equals("naval-shore");
            byte[] before=bytes(w);Displacement.Preview p=sea?w.army.tacticPreview(1,b.hex,Army.Tactic.RAM):w.war.tacticPreview(1,2,tactic);
            for(int i=0;i<4;i++){if(sea)w.army.tacticPreview(1,b.hex,Army.Tactic.RAM);else w.war.tacticPreview(1,2,tactic);}
            check(Arrays.equals(before,bytes(w)),scene+" preview and repeated candidate inspection preserve full world/RNG");
            if(scene.equals("hook-retreat")||scene.equals("hook-cliff")){
                check(!p.valid()&&p.error.contains("己方退路"),scene+" identifies own retreat");check(!w.war.tactic(1,2,tactic).ok,scene+" invalid command rejected");check(Arrays.equals(before,bytes(w)),"invalid command has no cost");continue;
            }
            check(p.valid(),scene+" remains castable: "+p.error);World replay=SaveCodec.decode(before);
            World.Result r=sea?w.army.tactic(1,b.hex,Army.Tactic.RAM):w.war.tactic(1,2,tactic);ok(r);
            if(sea)ok(replay.army.tactic(1,new Hex(6,6),Army.Tactic.RAM));else ok(replay.war.tactic(1,2,tactic));
            check(Arrays.equals(bytes(w),bytes(replay)),scene+" deterministic replay");
            check(a.acted&&a.energy==100-(sea?Army.Tactic.RAM.energy:tactic.energy),scene+" one action and one energy debit");
            if(Arrays.asList("mountain","cliff","shore","edge","friend","enemy","ally","ceasefire","city","gate","port","facility","tower","naval-shore").contains(scene)){
                check(b.hex.equals(scene.equals("edge")?new Hex(15,6):new Hex(6,6)),scene+" cannot enter obstruction");check(r.message.contains("实际位移"),scene+" blocked movement explained");
            }
            if(scene.equals("second-block"))check(b.hex.equals(new Hex(7,6)),"two-step stops after one legal tile");
            if(scene.equals("friend")||scene.equals("enemy"))check(w.unit(3).troops==3900,"legacy collision hits blocker once");
            if(scene.equals("ally")||scene.equals("ceasefire"))check(w.unit(3).troops==4000,"collateral cannot bypass treaty");
            byte[] after=bytes(w);check(!(sea?w.army.tactic(1,b.hex,Army.Tactic.RAM):w.war.tactic(1,2,tactic)).ok,"repeated action rejected");check(Arrays.equals(after,bytes(w)),"repeat cannot duplicate loot/action");
            SaveCodec.validate(w);
        }
        for(War.Tactic t:new War.Tactic[]{War.Tactic.THRUST,War.Tactic.DOUBLE_THRUST,War.Tactic.CHARGE,War.Tactic.ADVANCE}){
            World w=DisplacementFixture.create("mountain",t.weapon);check(w.war.tacticError(1,2,t)==null,t+" blockage is an effect, not cast rejection");ok(w.war.tactic(1,2,t));check(w.unit(1).hex.equals(new Hex(5,6))&&w.unit(2).hex.equals(new Hex(6,6)),t+" no ghost motion");
        }
        World blocked=DisplacementFixture.create("mountain",World.Weapon.CAVALRY);byte[] before=bytes(blocked);
        check(blocked.war.tacticError(1,2,War.Tactic.BREAKTHROUGH).contains("身后落点"),"breakthrough names required landing");check(!blocked.war.tactic(1,2,War.Tactic.BREAKTHROUGH).ok&&Arrays.equals(before,bytes(blocked)),"blocked breakthrough is atomic");
        World protectedWorld=DisplacementFixture.create("ally");before=bytes(protectedWorld);check(!protectedWorld.war.tactic(1,3,War.Tactic.THRUST).ok&&Arrays.equals(before,bytes(protectedWorld)),"cannot directly target protected unit");
    }
    private static void learn(World w,int owner,Campaign.Tech t){if(t.prerequisite!=null)learn(w,owner,t.prerequisite);w.campaign.finishTech(owner,t);}
    private static void terrainCapabilities()throws Exception{
        World w=DisplacementFixture.create("cliff");learn(w,0,Campaign.Tech.DIFFICULT_MARCH);ok(w.war.tactic(1,2,War.Tactic.THRUST));check(w.unit(2).hex.equals(new Hex(6,6)),"attacker research does not grant victim movement ability");
        w=DisplacementFixture.create("cliff");learn(w,1,Campaign.Tech.DIFFICULT_MARCH);ok(w.war.tactic(1,2,War.Tactic.THRUST));check(w.unit(2).hex.equals(new Hex(7,6)),"victim's research permits its own destination");
        w=DisplacementFixture.create("hook-cliff");learn(w,0,Campaign.Tech.DIFFICULT_MARCH);check(w.war.tacticError(1,2,War.Tactic.HOOK)==null,"researched own retreat is legal");ok(w.war.tactic(1,2,War.Tactic.HOOK));check(w.unit(1).hex.equals(new Hex(4,6)),"legal hook moves actor");
        World hook=DisplacementFixture.create("plain",World.Weapon.HALBERD);hook.terrain[5][6]=World.Terrain.MOUNTAIN_PATH;learn(hook,0,Campaign.Tech.DIFFICULT_MARCH);
        ok(hook.war.tactic(1,2,War.Tactic.HOOK));check(hook.unit(2).hex.equals(new Hex(6,6))&&hook.unit(1).hex.equals(new Hex(4,6)),"hook cannot pull victim onto attacker's privileged terrain");
    }
    private static void trapAndFire()throws Exception{
        World plain=DisplacementFixture.create("plain");ok(plain.war.tactic(1,2,War.Tactic.THRUST));int base=plain.unit(2).troops;
        World seed=DisplacementFixture.create("own-seed");World.Result r=seed.war.tactic(1,2,War.Tactic.THRUST);ok(r);
        check(seed.war.at(new Hex(7,6))==null&&seed.unit(2).hex.equals(new Hex(7,6)),"own seed triggers rather than blocking push");check(seed.unit(2).troops==base-700,"one existing trap fire amount, no extra collision");check(r.message.contains("火伤700")&&r.message.contains("火种引爆"),"actual trap result included");
        World fire=DisplacementFixture.create("fire");ok(fire.war.tactic(1,2,War.Tactic.THRUST));check(fire.unit(2).troops==base,"ordinary flame tile does not invent an extra immediate tick");
        World chain=DisplacementFixture.create("own-seed");War.Structure second=new War.Structure(2,0,War.StructureKind.FIRE_SEED,new Hex(8,6),200);chain.war.structures.add(second);chain.war.nextStructureId=3;
        byte[] before=bytes(chain);check(chain.war.tacticPreview(1,2,War.Tactic.THRUST).riskHexes.contains(second.hex),"preview includes actual supported chain geometry");check(Arrays.equals(before,bytes(chain)),"trap footprint pure");ok(chain.war.tactic(1,2,War.Tactic.THRUST));check(chain.war.structures.isEmpty(),"two traps each removed once");SaveCodec.validate(chain);
        World friendly=DisplacementFixture.create("own-seed");DisplacementFixture.unit(friendly,3,0,World.Weapon.SPEAR,new Hex(7,5),5000);check(friendly.war.tacticPreview(1,2,War.Tactic.THRUST).friendlyRisk,"friendly trap risk includes explicit own assets");
    }
    private static void killsAndIdentity()throws Exception{
        World w=DisplacementFixture.create("kill");ok(w.war.tactic(1,2,War.Tactic.CHARGE));check(w.unit(2)==null&&w.unit(1).hex.equals(new Hex(6,6)),"main kill follows into the one vacated original tile");check(w.unit(1).gold==123&&w.unit(1).food==30456,"loot once through official settlement");
        World deadSource=DisplacementFixture.create("own-seed",World.Weapon.CAVALRY);deadSource.unit(1).troops=1;deadSource.war.structures.get(0).kind=War.StructureKind.INFERNO_SEED;
        ok(deadSource.war.tactic(1,2,War.Tactic.ADVANCE));check(deadSource.unit(1)==null,"source dies in trap");check(deadSource.unit(2).hex.equals(new Hex(7,6)),"no second displacement or resurrection after source dies");SaveCodec.validate(deadSource);
        World secondKill=DisplacementFixture.create("plain",World.Weapon.CAVALRY);secondKill.unit(2).troops=1300;secondKill.war.structures.add(new War.Structure(1,1,War.StructureKind.FIRE_SEED,new Hex(8,6),200));secondKill.war.nextStructureId=2;
        ok(secondKill.war.tactic(1,2,War.Tactic.ADVANCE));check(secondKill.unit(2)==null&&secondKill.unit(1).hex.equals(new Hex(7,6)),"second-step trap kill keeps actual follow position instead of teleporting backward");SaveCodec.validate(secondKill);
    }
    private static void transport()throws Exception{
        World w=TacticalLogisticsTest.fixture();Domestic.Mission m=TacticalLogisticsTest.send(w,false,false);TacticalLogisticsTest.tick(w);m.hex=new Hex(8,10);m.stopped=true;m.status=War.Status.CONFUSED;m.statusTurns=1;
        World.Unit a=StrategicManagementTest.unit(w,20,World.Weapon.SPEAR,new Hex(7,10));w.active=1;Arrays.fill(w.officer(20).aptitude,3);int gold=m.gold,food=m.food,spent=m.movementSpent;
        ok(w.war.tactic(a.id,m.id,War.Tactic.THRUST));check(w.unit(m.id)==m&&w.domestic.mission(m.id)==m&&m.hex.equals(new Hex(9,10)),"transport task and map share displaced identity");check(m.gold==gold&&m.food==food&&m.movementSpent==spent,"forced move does not duplicate cargo or reset movement");SaveCodec.validate(w);
        World kill=TacticalLogisticsTest.fixture();Domestic.Mission cargo=TacticalLogisticsTest.send(kill,false,false);TacticalLogisticsTest.tick(kill);cargo.hex=new Hex(8,10);cargo.troops=1;cargo.status=War.Status.CONFUSED;cargo.statusTurns=1;
        World.Unit attacker=StrategicManagementTest.unit(kill,20,World.Weapon.SPEAR,new Hex(7,10));Arrays.fill(kill.officer(20).aptitude,3);kill.active=1;int totalGold=attacker.gold+cargo.gold,totalFood=attacker.food+cargo.food;
        World.Result result=kill.war.tactic(attacker.id,cargo.id,War.Tactic.DOUBLE_THRUST);ok(result);check(kill.unit(cargo.id)==null&&kill.domestic.mission(cargo.id)==null,"dead transport disappears from both entry points");check(attacker.gold==totalGold&&attacker.food==totalFood,"transport loot paid exactly once");check(result.message.contains("运输兵装散失"),"transport loss uses shared combat report");SaveCodec.validate(kill);
    }
    private static void ai()throws Exception{
        World w=DisplacementFixture.create("friend");byte[] before=bytes(w);CampaignAi ai=new CampaignAi(w);CampaignAi.Action first=ai.bestAction(1,true),again=ai.bestAction(1,true);
        check(first!=null&&again!=null&&first.kind==again.kind&&first.target==again.target&&first.tactic==again.tactic,"stable candidate ordering");check(Arrays.equals(before,bytes(w)),"AI scoring cannot consume RNG or change state");check(first.tactic!=War.Tactic.THRUST&&first.tactic!=War.Tactic.DOUBLE_THRUST,"AI avoids known friendly collision");ok(ai.execute(first));
        World fallback=DisplacementFixture.create("mountain");fallback.unit(1).energy=0;CampaignAi.Action attack=new CampaignAi(fallback).bestAction(1,true);check(attack!=null&&attack.kind==CampaignAi.Kind.ATTACK,"no legal tactic falls back to real normal attack");ok(new CampaignAi(fallback).execute(attack));
    }
}
