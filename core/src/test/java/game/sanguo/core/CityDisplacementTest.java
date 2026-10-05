package game.sanguo.core;

import java.util.Arrays;

/** Real campaign tactic regression for forced landings at authoritative site cells. */
public final class CityDisplacementTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static Hex add(Hex h,Hex d,int n){return new Hex(h.q+d.q*n,h.r+d.r*n);}
    private static World fixture(Hex landing,Hex outward,int gap,int owner,War.Tactic tactic){
        World w=DisplacementFixture.create("plain",tactic.weapon);
        w.cities.add(new World.City(20,"七格城",new Hex(8,6),owner));
        w.unit(2).hex=add(landing,outward,gap);
        w.unit(1).hex=add(landing,outward,gap+1);
        w.campaign.treaties.add(new Campaign.Treaty(0,2,Campaign.TreatyKind.ALLIANCE,12));
        // Fixture authoring changes entity positions/ownership outside commands.
        // Prime the report recorder on the final arrangement, as loading does.
        w.governance.reconcile(false);w.reports.rebase();
        return w;
    }
    public static void main(String[] args)throws Exception{
        cavalryReproduction();perimeter();legacyOverlap();hookAndCenter();obstructionsAndChain();killAndDamage();singleCellSites();ai();
        System.out.println("PASS: "+checks+" city forced-displacement checks: all perimeter approaches, ownership, partial movement, real tactics, pure preview, RNG/save continuation, legacy overlap and AI.");
    }
    private static void killAndDamage()throws Exception{
        for(War.Tactic tactic:new War.Tactic[]{War.Tactic.CHARGE,War.Tactic.ADVANCE})for(int owner:new int[]{-1,0,1,2}){
            World w=fixture(new Hex(9,6),new Hex(1,0),1,owner,tactic);
            w.unit(1).hex=new Hex(10,6);w.unit(2).hex=new Hex(9,6);w.unit(2).troops=1;w.unit(2).gold=123;w.unit(2).food=456;w.reports.rebase();
            World replay=SaveCodec.decode(SaveCodec.encode(w));
            check(w.war.tactic(1,2,tactic).ok&&replay.war.tactic(1,2,tactic).ok,"legacy rim target can be defeated by normal cavalry command");
            check(w.unit(2)==null&&w.unit(1).hex.equals(new Hex(10,6)),"main kill cannot let actor follow into vacated city cell");
            check(w.unit(1).gold==123&&w.unit(1).food==30456,"blocked kill-follow retains one normal loot settlement");
            check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"kill/blocked-follow full-save/RNG replay");
        }
        World city=fixture(new Hex(9,6),new Hex(1,0),1,1,War.Tactic.ADVANCE);
        city.unit(2).status=War.Status.NORMAL;city.unit(2).statusTurns=0;city.reports.rebase();
        World mountain=SaveCodec.decode(SaveCodec.encode(city));mountain.cities.remove(mountain.city(20));mountain.terrain[9][6]=World.Terrain.MOUNTAIN;mountain.reports.rebase();
        check(city.war.tactic(1,2,War.Tactic.ADVANCE).ok&&mountain.war.tactic(1,2,War.Tactic.ADVANCE).ok,"normal-status defender receives both blocked commands");
        check(city.unit(1).troops==mountain.unit(1).troops&&city.unit(2).troops==mountain.unit(2).troops,"city obstruction does not invent extra damage or counterattack");
        check(city.strategy.getRandomState()==mountain.strategy.getRandomState(),"city obstruction retains blocked cavalry damage/contest RNG consumption");
        check(city.unit(2).hex.equals(new Hex(10,6)),"normal-status defender remains outside own city");
    }
    private static void obstructionsAndChain()throws Exception{
        for(String obstruction:new String[]{"unit","mountain","water","tower","trap"}){
            World w=fixture(new Hex(9,6),new Hex(1,0),2,1,War.Tactic.ADVANCE);
            Hex first=new Hex(10,6);
            switch(obstruction){
                case "unit":DisplacementFixture.unit(w,3,0,World.Weapon.SPEAR,first,4000);break;
                case "mountain":w.terrain[first.q][first.r]=World.Terrain.MOUNTAIN;break;
                case "water":w.terrain[first.q][first.r]=World.Terrain.WATER;break;
                default:War.StructureKind kind=obstruction.equals("trap")?War.StructureKind.FIRE_SEED:War.StructureKind.ARROW_TOWER;
                    w.war.structures.add(new War.Structure(1,0,kind,first,kind.hp));w.war.nextStructureId=2;
            }
            w.reports.rebase();byte[] before=SaveCodec.encode(w);World replay=SaveCodec.decode(before);
            Displacement.Preview p=w.war.tacticPreview(1,2,War.Tactic.ADVANCE);
            check(p.valid()&&Arrays.equals(before,SaveCodec.encode(w)),"intermediate "+obstruction+" preview is pure and castable");
            check(w.war.tactic(1,2,War.Tactic.ADVANCE).ok&&replay.war.tactic(1,2,War.Tactic.ADVANCE).ok,"intermediate obstacle command executes");
            check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"intermediate "+obstruction+" save/RNG replay");
            check(w.unit(2).hex.equals(obstruction.equals("trap")?first:new Hex(11,6)),"obstacle or trap determines actual last external cell");
            check(!SiteFootprint.contains(w.city(20),w.unit(2).hex),"trap continuation cannot cross city rim");
            if(obstruction.equals("trap"))check(w.war.at(first)==null&&w.unit(2).troops<7000,"real trap detonates before rechecking second-step city landing");
        }
    }
    private static void cavalryReproduction()throws Exception{
        World w=fixture(new Hex(9,6),new Hex(1,0),2,1,War.Tactic.ADVANCE);
        Displacement.Preview p=w.war.tacticPreview(1,2,War.Tactic.ADVANCE);
        World.Result result=w.war.tactic(1,2,War.Tactic.ADVANCE);
        System.out.println("ADVANCE reproduction preview="+p.targetPath+" actual="+w.unit(2).hex+" city="+SiteFootprint.cells(w.city(20))+" command="+result.ok);
        check(result.ok,"real cavalry ADVANCE executes");
        check(w.unit(2).hex.equals(new Hex(10,6)),"real target lands outside city after one legal step");
        check(p.valid()&&new Hex(9,6).equals(p.blocked)&&p.targetPath.size()==2,"cavalry ADVANCE stops before defender's own city rim");
    }
    private static void hookAndCenter()throws Exception{
        World hook=fixture(new Hex(9,6),new Hex(1,0),1,0,War.Tactic.HOOK);
        hook.unit(1).hex=new Hex(10,6);hook.unit(2).hex=new Hex(11,6);hook.reports.rebase();
        byte[] before=SaveCodec.encode(hook);Displacement.Preview p=hook.war.tacticPreview(1,2,War.Tactic.HOOK);
        check(!p.valid()&&p.error.contains("己方退路"),"mandatory hook retreat cannot enter actor's own city");
        check(!hook.war.tactic(1,2,War.Tactic.HOOK).ok&&Arrays.equals(before,SaveCodec.encode(hook)),"blocked mandatory retreat rejects without damage/RNG/cost");
        for(int owner:new int[]{-1,0,1,2}){
            World w=fixture(new Hex(9,6),new Hex(1,0),1,owner,War.Tactic.BREAKTHROUGH);
            w.unit(1).hex=new Hex(10,6);w.unit(2).hex=new Hex(9,6);w.reports.rebase();
            p=w.war.tacticPreview(1,2,War.Tactic.BREAKTHROUGH);
            check(p.valid()&&p.actorPath.size()==1&&new Hex(8,6).equals(p.blocked),"breakthrough cannot land at city center from a legacy rim target");
            check(w.war.tactic(1,2,War.Tactic.BREAKTHROUGH).ok&&w.unit(1).hex.equals(new Hex(10,6)),"blocked center preserves main attack without actor teleport");
            check(w.unit(2).hex.equals(new Hex(9,6)),"old overlapping unit stays intact");
        }
    }
    private static void perimeter()throws Exception{
        World.City geometry=new World.City(20,"七格城",new Hex(8,6),1);
        int approaches=0;
        for(Hex rim:SiteFootprint.cells(geometry))for(Hex outward:new Hex(0,0).neighbors()){
            if(SiteFootprint.contains(geometry,add(rim,outward,1)))continue;
            approaches++;
            for(int owner:new int[]{-1,0,1,2})for(War.Tactic tactic:new War.Tactic[]{War.Tactic.THRUST,War.Tactic.DOUBLE_THRUST,War.Tactic.CHARGE,War.Tactic.ADVANCE,War.Tactic.BREAKTHROUGH})for(int gap:new int[]{1,2}){
                // One-step effects cannot reach a site two steps away.
                if(gap==2&&tactic!=War.Tactic.DOUBLE_THRUST&&tactic!=War.Tactic.ADVANCE)continue;
                World w=fixture(rim,outward,gap,owner,tactic);
                Hex actor=w.unit(1).hex,target=w.unit(2).hex;
                byte[] before=SaveCodec.encode(w);
                Displacement.Preview preview=w.war.tacticPreview(1,2,tactic);
                check(preview.valid(),"site blockage must not reject main damage: "+preview.error);
                check(rim.equals(preview.blocked),"preview identifies complete footprint: "+tactic+" owner="+owner+" gap="+gap+" "+rim);
                for(int n=0;n<3;n++)w.war.tacticPreview(1,2,tactic);
                check(Arrays.equals(before,SaveCodec.encode(w)),"preview preserves full state/RNG");
                World replay=SaveCodec.decode(before);
                World.Result result=w.war.tactic(1,2,tactic);
                check(result.ok,result.message);
                check(replay.war.tactic(1,2,tactic).ok,"decoded command succeeds");
                check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"save/RNG replay is byte exact: "+tactic+" owner="+owner+" gap="+gap+" rim="+rim+" live="+result.message);
                Hex expectedTarget=gap==2?add(rim,outward,1):target;
                boolean follow=tactic==War.Tactic.CHARGE||tactic==War.Tactic.ADVANCE;
                Hex expectedActor=follow&&gap==2?target:actor;
                check(w.unit(2)!=null&&w.unit(2).hex.equals(expectedTarget),"target stops at last legal external cell");
                check(w.unit(1)!=null&&w.unit(1).hex.equals(expectedActor),"actor follows only an actual legal displacement");
                check(!SiteFootprint.contains(w.city(20),w.unit(1).hex)&&!SiteFootprint.contains(w.city(20),w.unit(2).hex),"neither unit lands in city");
                check(w.unit(2).troops<8000&&w.unit(1).acted&&w.unit(1).energy==100-tactic.energy,"blocked effect retains damage and one command debit");
                check(w.city(20).defense==geometry.defense,"no invented city collision damage");
                byte[] after=SaveCodec.encode(w);
                check(!w.war.tactic(1,2,tactic).ok&&Arrays.equals(after,SaveCodec.encode(w)),"repeat is rejected atomically");
                w.nextTurn();replay.nextTurn();
                check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"normal turn/save continuation remains deterministic");
            }
        }
        check(approaches==18,"all eighteen external edges of the seven-cell footprint covered");
    }
    private static void legacyOverlap()throws Exception{
        World w=fixture(new Hex(9,6),new Hex(1,0),1,1,War.Tactic.ADVANCE);
        w.unit(2).hex=new Hex(9,6);w.unit(1).hex=new Hex(10,6);
        byte[] before=SaveCodec.encode(w);World loaded=SaveCodec.decode(before);
        check(Arrays.equals(before,SaveCodec.encode(loaded)),"old overlapped save is not silently migrated");
        check(loaded.unit(2).hex.equals(new Hex(9,6)),"old unit identity and original city-rim position preserved");
        check(loaded.army.moveCost(loaded.unit(2),new Hex(9,6),new Hex(10,6))>0,"ordinary own-city outward passage remains legal");
        check(loaded.army.moveCost(loaded.unit(2),new Hex(10,6),new Hex(9,6))>0,"ordinary own-city inward passage remains legal");
        check(loaded.army.canEnterSite(loaded.unit(2),loaded.unit(2).hex,loaded.city(20)),"explicit garrison remains legal");
        Displacement.Preview p=loaded.war.tacticPreview(1,2,War.Tactic.ADVANCE);
        check(p.valid()&&p.targetPath.size()==1&&new Hex(8,6).equals(p.blocked),"forced movement cannot deepen existing overlap");
        check(loaded.war.tactic(1,2,War.Tactic.ADVANCE).ok,"legacy overlapping target still receives real tactic");
        check(loaded.unit(2).hex.equals(new Hex(9,6)),"no deletion or forced city-center migration");
        SaveCodec.validate(loaded);
    }
    private static void singleCellSites()throws Exception{
        for(World.SiteKind kind:new World.SiteKind[]{World.SiteKind.GATE,World.SiteKind.PORT}){
            World w=DisplacementFixture.create("plain",World.Weapon.CAVALRY);
            World.City site=new World.City(20,"友方单格据点",new Hex(7,6),1);site.kind=kind;w.cities.add(site);
            check(w.war.tactic(1,2,War.Tactic.ADVANCE).ok,"single-cell main damage executes");
            check(w.unit(2).hex.equals(new Hex(6,6)),"forced landing cannot dock in friendly "+kind);
        }
    }
    private static void ai()throws Exception{
        World w=fixture(new Hex(9,6),new Hex(1,0),1,1,War.Tactic.ADVANCE);
        byte[] before=SaveCodec.encode(w);CampaignAi ai=new CampaignAi(w);
        CampaignAi.Action first=ai.bestAction(1,true),again=ai.bestAction(1,true);
        check(first!=null&&again!=null&&first.kind==again.kind&&first.tactic==again.tactic&&first.target==again.target,"AI selection is stable");
        check(Arrays.equals(before,SaveCodec.encode(w)),"AI candidate queries preserve state/RNG");
        check(ai.execute(first).ok,"AI executes normal shared entry");
        check(w.unit(2)==null||!SiteFootprint.contains(w.city(20),w.unit(2).hex),"AI cannot push into city");
    }
}
