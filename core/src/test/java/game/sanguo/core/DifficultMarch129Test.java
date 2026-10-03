package game.sanguo.core;

import java.nio.file.*;
import java.util.*;

/** v129's explicitly requested plank entry gate, through the real movement consumers. */
public final class DifficultMarch129Test {
    private static int checks;
    private static final Hex FROM=new Hex(8,8),PLANK=new Hex(9,8),BEYOND=new Hex(10,8);
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static void unchanged(byte[] before,World w,String why)throws Exception{check(Arrays.equals(before,bytes(w)),why);}
    private static World corridor(){
        World w=NativeGrid129Fixture.world();
        for(int r=0;r<w.height;r++)w.terrain[9][r]=World.Terrain.MOUNTAIN;
        w.terrain[9][8]=World.Terrain.PLANK_ROAD;w.terrain[8][9]=World.Terrain.PLAIN;
        return w;
    }
    private static void direct()throws Exception{
        for(World.Terrain terrain:World.Terrain.values())check(Fieldworks.requiresDifficultMarch(terrain)==
            (terrain==World.Terrain.MOUNTAIN_PATH||terrain==World.Terrain.SHALLOWS||terrain==World.Terrain.PLANK_ROAD),"only the existing difficult-terrain family plus requested plank is gated");
        for(World.Terrain type:new World.Terrain[]{World.Terrain.SHALLOWS,World.Terrain.MOUNTAIN_PATH,World.Terrain.PLANK_ROAD}){
            World w=NativeGrid129Fixture.world();w.terrain[PLANK.q][PLANK.r]=type;World.Unit u=w.unit(1);w.officer(u.officerId).skillId=Skill.TAPO.id;
            byte[] before=bytes(w);
            for(World.Weapon weapon:World.Weapon.values())check(w.fieldworks.landCost(PLANK,weapon,0)<0,"all weapons require force research: "+type+"/"+weapon);
            check(w.army.moveCost(u,FROM,PLANK)<0,"edge blocked before research, including 踏破");
            check(!w.reachable(u).containsKey(PLANK),"player reachability excludes gated cell");
            check(!w.orders.previewMove(1,PLANK).valid(),"immediate move preview rejects gated cell");
            check(!w.marches.previewMove(1,PLANK).valid(),"persistent move preview rejects gated cell");
            unchanged(before,w,"all previews preserve exact state and RNG");
            check(!w.move(1,PLANK).ok,"real move rejected before research");unchanged(before,w,"rejection preserves exact state and RNG");
            NativeGrid129Fixture.learn(w,1);check(w.army.moveCost(u,FROM,PLANK)<0,"enemy research cannot grant actor access");
            NativeGrid129Fixture.learn(w,0);
            check(w.fieldworks.landCost(PLANK,u.weapon,u.owner)==(type==World.Terrain.SHALLOWS?2:3),"existing researched movement cost retained");
            check(w.reachable(u).containsKey(PLANK)&&w.orders.previewMove(1,PLANK).valid()&&w.marches.previewMove(1,PLANK).valid(),"research updates every movement preview");
            int troops=u.troops;ok(w.move(1,PLANK));check(u.hex.equals(PLANK)&&u.troops==troops,"actual researched move enters with no changed damage rule");
            World loaded=SaveCodec.decode(bytes(w));check(loaded.campaign.has(0,Campaign.Tech.DIFFICULT_MARCH)&&loaded.unit(1).hex.equals(PLANK),"researched save roundtrip retains access and position");
        }
        // Preserve the existing fallback hazard routine for legacy/migrated callers.
        World old=NativeGrid129Fixture.world();old.terrain[9][8]=World.Terrain.PLANK_ROAD;int troops=old.unit(1).troops;
        old.fieldworks.traveled(old.unit(1),List.of(FROM,PLANK));check(old.unit(1).troops==troops-troops/100,"unresearched legacy hazard formula unchanged");
        old.officer(1).skillId=Skill.TAPO.id;troops=old.unit(1).troops;old.fieldworks.traveled(old.unit(1),List.of(FROM,PLANK));check(old.unit(1).troops==troops,"踏破 hazard immunity unchanged");
    }
    private static Object aiRoute(World w,Hex goal)throws Exception{
        var method=CampaignAi.class.getDeclaredMethod("route",World.Unit.class,Hex.class,int.class);method.setAccessible(true);
        return method.invoke(new CampaignAi(w),w.unit(1),goal,1);
    }
    private static void routersAndTransport()throws Exception{
        World w=corridor();byte[] before=bytes(w);
        check(!w.marches.previewMove(1,BEYOND).valid(),"player path cannot cross plank to otherwise-open target");
        check(w.domestic.route(FROM,BEYOND,0)==null,"strategic route cannot cross plank");
        check(aiRoute(w,BEYOND)==null,"actual AI route search cannot cross plank");
        unchanged(before,w,"route searches preserve full state/RNG");
        World.City target=new World.City(2,"运输目标",new Hex(13,8),0);w.cities.add(target);for(Hex cell:SiteFootprint.cells(target))w.terrain[cell.q][cell.r]=World.Terrain.PLAIN;
        Domestic.Mission probe=new Domestic.Mission(0,0,0,0,2,FROM,true,100,2000,1000,new int[World.Weapon.values().length]);
        check(w.army.moveCost(probe,FROM,PLANK)<0,"transport movement edge shares gate");
        check(!w.marches.convoyRoute(probe).valid(),"transport route shares gate");
        for(boolean sea:new boolean[]{false,true})check(w.domestic.transportError(0,2,0,new int[0],100,2000,1000,new int[World.Weapon.values().length],sea)!=null,"paid land/water transport cannot bypass gate");
        NativeGrid129Fixture.learn(w,0);
        check(w.marches.previewMove(1,BEYOND).valid(),"player path opens after research: "+w.marches.previewMove(1,BEYOND).error);
        check(w.domestic.route(FROM,BEYOND,0)!=null&&aiRoute(w,BEYOND)!=null,"strategic and AI paths open after research");
        check(w.marches.convoyRoute(probe).valid()&&w.army.moveCost(probe,FROM,PLANK)==3,"transport path and cost open after research");
        check(w.domestic.transportError(0,2,0,new int[0],100,2000,1000,new int[World.Weapon.values().length],false)==null,"actual transport validation opens after research: "+w.domestic.transportError(0,2,0,new int[0],100,2000,1000,new int[World.Weapon.values().length],false));
        ok(w.domestic.transport(0,2,0,100,2000,1000,new int[World.Weapon.values().length]));
        check(!w.domestic.missions.isEmpty(),"real paid transport dispatch succeeds after research");
    }
    private static void landingAndDeployment()throws Exception{
        World w=NativeGrid129Fixture.world();World.Unit u=w.unit(1);w.terrain[8][8]=World.Terrain.WATER;w.terrain[9][8]=World.Terrain.PLANK_ROAD;
        World.City port=new World.City(2,"渡口",new Hex(8,9),0);port.kind=World.SiteKind.PORT;w.cities.add(port);
        for(Army.Ship ship:Army.Ship.values()){u.ship=ship;check(w.army.moveCost(u,FROM,PLANK)<0,"owned-port landing cannot bypass gate: "+ship);}
        NativeGrid129Fixture.learn(w,0);
        for(Army.Ship ship:Army.Ship.values()){u.ship=ship;check(w.army.moveCost(u,FROM,PLANK)==3,"researched owned-port landing retains cost: "+ship);}
        w=NativeGrid129Fixture.world();World.City city=w.city(0);
        for(Hex edge:SiteFootprint.edge(city))if(w.inside(edge))w.terrain[edge.q][edge.r]=World.Terrain.PLANK_ROAD;
        byte[] before=bytes(w);check(w.army.deploymentExit(city,World.Weapon.SPEAR)==null,"deployment preview has no unresearched plank exit");
        check(!w.deploy(0,0,World.Weapon.SPEAR,1000).ok,"actual deployment rejects plank-only exits");unchanged(before,w,"failed deployment spends no resources/RNG");
        NativeGrid129Fixture.learn(w,0);check(w.army.deploymentExit(city,World.Weapon.SPEAR)!=null,"research enables exit preview");ok(w.deploy(0,0,World.Weapon.SPEAR,1000));
    }
    private static void displacement()throws Exception{
        for(boolean researched:new boolean[]{false,true}){
            World w=DisplacementFixture.create("plain");w.terrain[7][6]=World.Terrain.PLANK_ROAD;NativeGrid129Fixture.learn(w,0);
            w.officer(w.unit(2).officerId).skillId=Skill.TAPO.id;if(researched)NativeGrid129Fixture.learn(w,1);
            byte[] before=bytes(w);Displacement.Preview p=w.war.tacticPreview(1,2,War.Tactic.THRUST);
            check(p.valid()&&p.targetPath.size()==(researched?2:1),"push preview uses victim force research, not attacker/踏破");unchanged(before,w,"displacement preview preserves state/RNG");
            ok(w.war.tactic(1,2,War.Tactic.THRUST));check(w.unit(2).hex.equals(new Hex(researched?7:6,6)),"actual push follows researched destination gate");
        }
    }
    private static void researchCompletion()throws Exception{
        byte[] saved=bytes(NativeGrid129Fixture.researchReadyWorld());World w=SaveCodec.decode(saved),reference=SaveCodec.decode(saved);
        check(!w.campaign.has(0,Campaign.Tech.DIFFICULT_MARCH)&&w.campaign.projects().size()==1,"staged fixture still lacks technique before real completion");
        check(w.fieldworks.landCost(new Hex(10,7),World.Weapon.SPEAR,0)<0,"unfinished research cannot grant plank access");
        ok(w.nextTurn());ok(reference.nextTurn());
        check(w.campaign.has(0,Campaign.Tech.DIFFICULT_MARCH)&&w.fieldworks.landCost(new Hex(10,7),World.Weapon.SPEAR,0)==3,"actual next-turn research completion opens plank access");
        unchanged(bytes(reference),w,"real research turn remains deterministic across save/load");
    }
    private static void oldSave()throws Exception{
        byte[] legacy=Files.readAllBytes(Path.of("core/src/test/resources/grid129/pre-gate-plank.sg11"));World w=SaveCodec.decode(legacy);
        unchanged(legacy,w,"real pre-gate v128 save decodes/re-encodes byte-for-byte without migration");
        check(w.unit(1).hex.equals(FROM)&&w.terrain[8][8]==World.Terrain.PLANK_ROAD&&!w.campaign.has(0,Campaign.Tech.DIFFICULT_MARCH),"legacy unit, map and absent technology remain untouched");
        check(w.army.moveCost(w.unit(1),FROM,new Hex(7,8))>0,"legacy unit already on plank can exit to ordinary ground");
        check(!w.marches.current(w.unit(1)).valid()&&!w.marches.previewMove(1,PLANK).valid(),"old queued route and new preview cannot enter another plank");
        unchanged(legacy,w,"legacy queries do not rewrite save/RNG");
        w.marches.advanceAll();check(w.unit(1).hex.equals(FROM)&&!w.unit(1).march.paused.isEmpty(),"old queued order pauses rather than crossing newly gated plank");
        World exit=SaveCodec.decode(legacy);ok(exit.move(1,new Hex(7,8)));check(exit.unit(1).hex.equals(new Hex(7,8)),"old occupant can actually leave without forced reposition/migration");
        NativeGrid129Fixture.learn(w,0);check(w.marches.current(w.unit(1)).valid(),"same saved order opens after authoritative research");
    }
    public static void main(String[] args)throws Exception{
        direct();routersAndTransport();landingAndDeployment();displacement();researchCompletion();oldSave();
        System.out.println("PASS DIFFICULT_MARCH129 "+checks+" checks: direct/preview/path/AI/transport/landing/deployment/displacement/skills/legacy-save");
    }
}
