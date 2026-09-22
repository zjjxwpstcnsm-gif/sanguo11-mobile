package game.sanguo.core;

import java.io.IOException;
import java.util.*;

/** Executable behavioral regression suite; runs identically locally and in CI. */
public final class CoreTest {
    private static int checks;
    private static void check(boolean condition,String name){checks++;if(!condition)throw new AssertionError(name);}
    private static World deployed() {
        World w=DemoScenario.create();check(w.deploy(0,0,World.Weapon.SPEAR,3000).ok,"can deploy");return w;
    }
    private static void expectBad(byte[] bytes,String name){try{SaveCodec.decode(bytes);throw new AssertionError(name);}catch(IOException expected){checks++;}}
    public static void main(String[] args)throws Exception {
        hex();actions();movement();combat();logistics();saving();longRun();
        System.out.println("PASS: "+checks+" assertions covering hex paths, resources, combat, turns, saves, and deterministic simulation.");
    }
    private static void hex() {
        Hex h=new Hex(4,6);Set<Hex> set=new HashSet<>(h.neighbors());check(set.size()==6,"six neighbors");
        for(Hex n:set){check(n.distance(h)==1,"adjacency distance");check(n.neighbors().contains(h),"symmetric adjacency");}
        check(new Hex(0,0).distance(new Hex(2,-2))==2,"axial distance");
    }
    private static void actions()throws Exception {
        World w=DemoScenario.create();FacilityProductionTest.facility(w,0,Domestic.Kind.BARRACKS);byte[] before=SaveCodec.encode(w);
        check(!w.recruit(1,3).ok,"cannot command enemy city");check(Arrays.equals(before,SaveCodec.encode(w)),"invalid command is atomic");
        check(w.recruit(0,0).ok,"recruit");check(w.city(0).troops==14500&&w.city(0).gold==4700&&w.city(0).order==85,"recruit costs and troops");
        check(!w.train(0,0).ok,"officer cannot act twice");check(w.actionPoints[0]==50,"spent AP exactly once");
        check(w.deploy(0,1,World.Weapon.CROSSBOW,3000).ok,"deploy second officer");
        check(w.city(0).troops==11500&&w.city(0).food==34000&&w.city(0).equipment[2]==9000,"deployment conserves stores");
        check(w.officer(1).cityId==-1&&w.officer(1).unitId==1,"officer has single location");
        w.city(0).food=0;before=SaveCodec.encode(w);check(!w.deploy(0,2,World.Weapon.SPEAR,3000).ok,"cannot deploy without supply");
        check(Arrays.equals(before,SaveCodec.encode(w)),"failed deployment does not spend resources");
        w=DemoScenario.create();w.actionPoints[0]=0;check(!w.train(0,0).ok,"AP exhaustion");
        w=DemoScenario.create();for(Hex n:SiteFootprint.edge(w.city(0)))w.terrain[n.q][n.r]=World.Terrain.MOUNTAIN;
        before=SaveCodec.encode(w);check(!w.deploy(0,0,World.Weapon.SPEAR,3000).ok,"blocked exit");check(Arrays.equals(before,SaveCodec.encode(w)),"blocked deployment atomic");
    }
    private static void movement()throws Exception {
        World w=deployed();World.Unit u=w.unit(1);Hex initial=u.hex;
        Hex bad=new Hex(-1,0);check(!w.move(1,bad).ok&&!u.acted,"out of bounds move rejected");
        Map<Hex,Integer> reachable=w.reachable(u);
        for(Map.Entry<Hex,Integer> e:reachable.entrySet()) {
            check(e.getValue()<=u.weapon.movement,"path within budget");check(w.cost(e.getKey(),u.weapon)>0||w.army.water(e.getKey()),"land or navigable water, never mountain");
            check(w.cityAt(e.getKey())==null||w.cityAt(e.getKey()).owner==u.owner,"own city allows paid transit; enemy city is blocked");
        }
        // This checks field commands; landing inside a seven-cell city legitimately auto-enters.
        Hex destination=null;for(Hex h:reachable.keySet())if(!h.equals(initial)&&w.cityAt(h)==null){destination=h;break;}
        check(destination!=null&&w.move(1,destination).ok,"legal path move");check(!u.acted&&w.orders.remaining(u)<u.weapon.movement,"moving preserves command with reduced movement");
        check(w.war.waitUnit(u.id).ok&&!w.move(1,initial).ok,"finished action cannot move again");
        w=deployed();u=w.unit(1);for(Hex n:u.hex.neighbors())if(w.inside(n))w.terrain[n.q][n.r]=World.Terrain.MOUNTAIN;
        check(w.reachable(u).size()==1,"cannot jump surrounding mountains");
        w=deployed();u=w.unit(1);Hex forest=u.hex.neighbors().get(0);w.terrain[forest.q][forest.r]=World.Terrain.FOREST;
        check(w.reachable(u).get(forest)==2,"forest consumes extra movement");
    }
    private static void combat()throws Exception {
        World w=deployed();check(w.deploy(0,1,World.Weapon.HALBERD,3000).ok,"friendly deployment");
        byte[] before=SaveCodec.encode(w);check(!w.attack(1,2).ok,"friendly fire rejected");check(Arrays.equals(before,SaveCodec.encode(w)),"friendly fire atomic");
        w.active=1;check(w.deploy(1,3,World.Weapon.SPEAR,3000).ok,"enemy deployment");w.active=0;
        World.Unit enemy=w.unit(3);enemy.hex=new Hex(5,9);World.Unit a=w.unit(1);a.hex=new Hex(4,9);
        check(w.attack(1,3).ok&&enemy.troops<3000,"attack causes casualties");check(!w.attack(1,3).ok,"double attack rejected");
        a.acted=false;enemy.troops=1;check(w.attack(1,3).ok&&w.unit(3)==null,"defeated unit removed");check(w.officer(3).unitId==-1,"defeated officer detached");
        a.acted=false;a.hex=new Hex(5,4);w.city(2).defense=1;
        check(w.siege(1,2).ok&&w.city(2).owner==0,"capture neutral city");check(w.city(2).defense>0,"captured defense restored");
        SaveCodec.validate(w);
        w=deployed();int troops=w.city(0).troops+w.unit(1).troops, food=w.city(0).food+w.unit(1).food;
        check(!w.enter(1,0).ok,"cannot enter before reaching the city footprint");
        w.unit(1).hex=w.city(0).hex;
        check(w.enter(1,0).ok&&w.units.isEmpty(),"enter reached friendly city footprint");check(w.city(0).troops==troops&&w.city(0).food==food,"return conserves resources");
        check(w.officer(0).cityId==0&&w.officer(0).acted,"returned officer cannot redeploy same turn");
    }
    private static void logistics()throws Exception {
        World w=deployed();w.unit(1).food=0;int troops=w.unit(1).troops;
        check(w.nextTurn().ok,"next turn");check(w.turn==1&&w.active==0,"turn advances and returns control");
        check(w.unit(1).food==0&&w.unit(1).troops<troops,"starvation never gives negative food");
        check(!w.unit(1).acted&&w.actionPoints[0]==60,"actions restored");
        check(w.units.stream().anyMatch(u->u.owner==1),"AI uses deployment commands");
        w=DemoScenario.create();int gold=w.city(0).gold;w.nextTurn();w.nextTurn();check(w.city(0).gold==gold,"no premature monthly income");
        w.nextTurn();check(w.city(0).gold==gold+800,"monthly income once");
        w=DemoScenario.create();w.city(1).owner=0;w.officers.removeIf(o->o.owner==1);w.checkVictory();check(w.winner==0,"enemy force eliminated");
        byte[] before=SaveCodec.encode(w);check(!w.nextTurn().ok&&Arrays.equals(before,SaveCodec.encode(w)),"finished game cannot advance");
    }
    private static void saving()throws Exception {
        World w=deployed();w.nextTurn();byte[] saved=SaveCodec.encode(w);World loaded=SaveCodec.decode(saved);
        check(Arrays.equals(saved,SaveCodec.encode(loaded)),"all save fields round trip");
        w.nextTurn();loaded.nextTurn();check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(loaded)),"loaded game continues deterministically");
        byte[] broken=saved.clone();broken[broken.length-1]^=1;expectBad(broken,"corrupt payload accepted");
        expectBad(Arrays.copyOf(saved,saved.length-3),"truncated save accepted");
        broken=saved.clone();broken[7]=99;expectBad(broken,"future version accepted");
        w=deployed();w.officer(0).unitId=999;try{SaveCodec.encode(w);throw new AssertionError("dangling reference accepted");}catch(IOException expected){checks++;}
    }
    private static void longRun()throws Exception {
        World a=DemoScenario.create(),b=DemoScenario.create();
        for(int i=0;i<100;i++) {
            a.nextTurn();b.nextTurn();SaveCodec.validate(a);
            check(Arrays.equals(SaveCodec.encode(a),SaveCodec.encode(b)),"deterministic multi-turn simulation "+i);
            if(a.winner>=0)break;
        }
    }
}
