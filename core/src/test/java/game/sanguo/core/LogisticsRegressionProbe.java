package game.sanguo.core;

/** Same public v0.25 commands on both revisions; intentionally failing baseline probes. */
public final class LogisticsRegressionProbe {
    public static void main(String[] args) {
        World w=StrategicManagementTest.fixture();
        if(args[0].equals("fee")||args[0].equals("food")){
            int gold=w.city(11).gold;
            if(!w.domestic.transport(11,12,4,500,5000,1000,new int[4]).ok)throw new AssertionError("dispatch");
            if(args[0].equals("fee")){
                if(w.city(11).gold!=gold-500)throw new AssertionError("manual p38: transport must not charge 100 gold");
            }else{Domestic.Mission m=w.domestic.missions.get(0);w.turn++;w.domestic.tick();if(m.food>=5000)throw new AssertionError("manual p38: traveling convoy does not consume carried food");}
        }else{
            World.Unit u=StrategicManagementTest.unit(w,4,World.Weapon.CROSSBOW,new Hex(33,15));u.energy=0;
            Domestic.Facility f=new Domestic.Facility(w.domestic.nextFacilityId++,20,Domestic.Kind.FARM,new Hex(35,15),-1,0);w.domestic.facilities.add(f);
            int hp=f.hp;new CampaignAi(w).runUnit(u,true,c->true,c->c.owner==0);
            if(f.hp>=hp)throw new AssertionError("fixture must damage a real facility");
            if(w.aiOrders.get(u).stalled!=0)throw new AssertionError("productive facility attack is counted as stalled");
        }
        System.out.println("PASS v026 probe: "+args[0]);
    }
}
