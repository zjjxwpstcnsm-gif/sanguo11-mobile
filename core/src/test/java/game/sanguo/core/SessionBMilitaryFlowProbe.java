package game.sanguo.core;

import java.util.*;
/** Diagnosis uses real source new game and ordinary deployment/movement. No injected unit or stock. */
public final class SessionBMilitaryFlowProbe {
    public static void main(String[] args)throws Exception{
        var source=PcScenarioCatalog.all().get(14);World preview=PcScenarioCatalog.preview(source.identity.scenarioId);
        int player=-1;for(int i=0;i<preview.factions.length;i++)if(preview.faction(i).equals("劉備")&&preview.alive(i))player=i;
        if(player<0)throw new AssertionError("Source14 LiuBei unavailable");
        World w=PcScenarioCatalog.load(source.identity.scenarioId,player,42);
        for(World.City c:w.cities)if(c.owner==player&&c.kind==World.SiteKind.CITY){
            for(World.Officer o:w.idle(c)){
                int gold=Math.min(3000,c.gold-1);int troops=Math.min(3000,Math.min(c.troops,c.food/2));
                if(gold<1500||troops<1000)continue;
                World.Result deployed=w.army.deploy(c.id,o.id,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,troops,troops*2,gold);
                if(!deployed.ok){System.out.println("DEPLOY_REJECT city="+c.name+" leader="+o.name+" "+deployed.message);continue;}
                World.Unit u=w.unit(o.unitId);
                System.out.println("SOURCE "+source.identity.scenarioId+" player="+player+" city="+c.name+" unit="+u.id+" gold="+u.gold+" acted="+u.acted+" status="+u.status+" origin="+u.hex);
                for(Hex h:u.hex.neighbors())System.out.println("INITIAL target="+h+" terrain="+w.terrain[h.q][h.r]+" error="+w.fieldworks.buildError(u.id,War.StructureKind.CAMP,h,0));
                byte[] before=SaveCodec.encode(w);Hex position=null,target=null;
                for(Hex origin:w.orders.reachable(u).keySet()){
                    World copy=SaveCodec.decode(before);World.Unit probe=copy.unit(u.id);
                    if(!origin.equals(probe.hex)&&!copy.move(u.id,origin).ok)continue;
                    List<Hex> legal=copy.fieldworks.sites(u.id,War.StructureKind.CAMP);
                    if(!legal.isEmpty()){position=origin;target=legal.get(0);break;}
                }
                if(position==null){System.out.println("NO LEGAL CAMP IN CURRENT MOVEMENT RANGE");return;}
                if(!position.equals(u.hex)){World.Result moved=w.move(u.id,position);if(!moved.ok)throw new AssertionError(moved.message);}
                System.out.println("LEGAL position="+position+" target="+target+" terrain="+w.terrain[target.q][target.r]+" source="+MapCoordinates.nationalSource(w,target));
                byte[] unchanged=SaveCodec.encode(w);if(w.fieldworks.buildError(u.id,War.StructureKind.CAMP,target,0)!=null)throw new AssertionError("Preview changed");
                if(!Arrays.equals(unchanged,SaveCodec.encode(w)))throw new AssertionError("Preview mutates save/RNG");
                World.Result built=w.fieldworks.build(u.id,War.StructureKind.CAMP,target,0);
                System.out.println("BUILD result="+built.ok+" message="+built.message+" gold="+w.unit(u.id).gold+" acted="+w.unit(u.id).acted);
                if(!built.ok)throw new AssertionError("Valid build failed");
                World restored=SaveCodec.decode(SaveCodec.encode(w));if(!Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)))throw new AssertionError("Full save differs");
                System.out.println("PASS normal source/deploy/move/core-build and full save/RNG; UI not yet verified");return;
            }
        }
        throw new AssertionError("No ordinary source deployment");
    }
}
