package game.sanguo.core;

import java.util.Arrays;

/** Source-backed thresholds through normal commands, with full save/RNG continuation. */
public final class PcTacticCostTest {
    static int checks;
    static void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
    static World fixture(World.Weapon weapon,boolean water){
        World w=new World(22,16);
        w.cities.add(new World.City(10,"甲城",new Hex(5,5),0));
        w.cities.add(new World.City(20,"敌城",new Hex(18,5),1));
        w.officers.add(new World.Officer(0,"甲将",0,10,80,80,80,80,80));
        w.officers.add(new World.Officer(20,"敌将",1,20,80,80,80,80,80));
        w.city(20).troops=10000;w.strategy.initializeOffices();
        World.Unit a=unit(w,1,0,0,weapon,water?new Hex(9,8):new Hex(17,5));
        if(water){w.terrain[9][8]=World.Terrain.WATER;w.terrain[10][8]=World.Terrain.WATER;a.ship=Army.Ship.WARSHIP;w.officer(0).aptitude[5]=3;unit(w,2,1,20,World.Weapon.SPEAR,new Hex(10,8));}
        return w;
    }
    static World.Unit unit(World w,int id,int owner,int officer,World.Weapon weapon,Hex h){
        World.Unit u=new World.Unit(id,owner,officer,weapon,h,5000,20000);w.units.add(u);w.nextUnitId=id+1;
        World.Officer o=w.officer(officer);w.strategy.releaseGovernor(officer);o.unitId=id;o.cityId=-1;return u;
    }
    static World.Result command(World w,Army.Tactic t,boolean water){return water?w.army.tactic(1,w.unit(2).hex,t):w.army.tacticCity(1,20,t);}
    public static void main(String[] args)throws Exception{
        World.Weapon[] weapons={World.Weapon.SIEGE_TOWER,World.Weapon.RAM,World.Weapon.WOODEN_BEAST,World.Weapon.CATAPULT,World.Weapon.SPEAR,World.Weapon.SPEAR,World.Weapon.SPEAR};
        Army.Tactic[] tactics={Army.Tactic.FIRE_ARROW,Army.Tactic.RAM,Army.Tactic.FLAME,Army.Tactic.STONE,Army.Tactic.FIRE_ARROW,Army.Tactic.RAM,Army.Tactic.STONE};
        for(int i=0;i<weapons.length;i++)for(int mode=0;mode<3;mode++){
            boolean water=i>=4;int cost=i==6?15:10;Army.Tactic t=tactics[i];World w=fixture(weapons[i],water);World.Unit a=w.unit(1);a.energy=mode==0?cost-1:mode==1?cost:100;
            // Seed a real miss for naval mode2; no RNG implementation replacement.
            if(water&&mode==2){long seed=0;do{w.strategy.setSeed(seed++);}while(w.strategy.nextInt(100)<w.army.tacticChance(1,w.unit(2).hex));w.strategy.setSeed(seed-1);}
            byte[] before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();int turn=w.turn,energy=a.energy;
            check(w.army.tacticCost(a,t)==cost,"source cost "+i);
            Displacement.Preview preview=water?w.army.tacticPreview(1,w.unit(2).hex,t):w.army.tacticCityPreview(1,20,t);
            check(preview.text.contains("消耗气力"+cost),"preview uses contextual source cost");
            check(Arrays.equals(before,SaveCodec.encode(w)),"preview preserves every saved field and RNG");
            World copy=SaveCodec.decode(before);World.Result result=command(w,t,water),again=command(copy,t,water);
            check(result.ok==again.ok&&result.message.equals(again.message)&&Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(copy)),"save-before command has identical result and RNG");
            if(mode==0){check(!result.ok&&Arrays.equals(before,SaveCodec.encode(w)),"below threshold rejects atomically");continue;}
            check(result.ok,result.message);check(a.energy==energy-cost&&a.acted&&w.turn==turn,"actual command spends exactly once without advancing turn");
            if(water&&mode==2){check(result.message.contains("未命中"),"genuine miss covered");check(w.strategy.getRandomState()==rng+0x9E3779B97F4A7C15L,"miss consumes exactly one hit roll");}
            byte[] after=SaveCodec.encode(w);check(Arrays.equals(after,SaveCodec.encode(SaveCodec.decode(after))),"post-command save roundtrip");
            check(!command(w,t,water).ok&&Arrays.equals(after,SaveCodec.encode(w)),"repeat rejected without second charge or RNG");
            copy=SaveCodec.decode(after);check(w.nextTurn().ok&&copy.nextTurn().ok,"normal following turn");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(copy)),"following turn and AI deterministic after reload");
        }
        int i=0;for(War.Tactic t:War.Tactic.values())check(t.energy==PcTacticCosts.at(i++),"infantry source cost remains aligned");
        System.out.println("PASS native tactic costs: "+checks+" checks");
    }
}
