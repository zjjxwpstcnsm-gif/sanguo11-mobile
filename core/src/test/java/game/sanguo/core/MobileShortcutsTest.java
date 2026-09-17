package game.sanguo.core;

import java.util.*;

/** Mobile-rule economics and the new playable content, exercised through real commands. */
public final class MobileShortcutsTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        for(Domestic.Kind kind:Arrays.asList(Domestic.Kind.MARKET,Domestic.Kind.FARM,Domestic.Kind.BARRACKS,Domestic.Kind.SMITH,Domestic.Kind.STABLE)){
            World w=TestScenarios.load("regional-sandbox",0);World.City c=w.home();World.Officer o=w.idle(c).get(0);
            int gold=c.gold,ap=w.actionPoints[0];Hex h=w.domestic.buildSites(c.id).get(0);
            check(w.domestic.build(c.id,o.id,kind,h).ok,"build highest level "+kind);
            Domestic.Facility f=w.domestic.at(h);check(f.level==3&&f.upgradeTo==0&&f.remaining>0,"level three construction uses no absorption");
            check(c.gold==gold-kind.cost&&w.actionPoints[0]==ap-10,"one construction payment");
            byte[] before=SaveCodec.encode(w);check(!w.domestic.build(c.id,o.id,kind,h).ok,"duplicate construction rejected");
            check(Arrays.equals(before,SaveCodec.encode(w)),"rejected construction is atomic");
            w=SaveCodec.decode(before);f=w.domestic.at(h);while(f.remaining>0)w.domestic.tick();
            check(f.level==3&&f.builderId==-1,"highest level survives construction save and completion");
            check(w.domestic.mergeCandidates(f.id).isEmpty(),"highest level has no merge candidates");
        }
        String[] ids={"heroes-mobile-sandbox","central-mobile-sandbox","jingxiang-mobile-sandbox"};
        int[] cities={42,18,12},people={670,180,120};
        for(int i=0;i<ids.length;i++)for(int side=0;side<3;side++){
            World w=TestScenarios.load(ids[i],side);
            check(w.cities.stream().filter(c->c.kind==World.SiteKind.CITY).count()==cities[i]&&w.officers.size()==people[i],"playable city and officer totals");
            check(w.officers.stream().map(o->o.id).distinct().count()==people[i],"stable unique officer IDs");
            check(w.officers.stream().anyMatch(o->o.owner==-1),"unaffiliated officers are actually recruitable");
            check(!w.life.naturalDeaths,"all-era sandbox does not kill historical characters on start");
            check(w.relations.sworn(1000,1001),"sourced relations loaded into gameplay");
            check(w.officer(new int[]{1000,2000,3000}[side]).role==Strategy.Role.RULER,"selected faction has its named ruler");
            World.City home=w.home();check(!w.idle(home).isEmpty()&&!w.domestic.buildSites(home.id).isEmpty(),"starting city has idle officers and development land");
            World.Officer leader=w.idle(home).get(0);int gold=home.gold,food=home.food,troops=home.troops;
            check(w.army.deploy(home.id,leader.id,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,2345,6789,4321).ok,"nonpreset troop food gold quantities deploy");
            World.Unit u=w.unit(leader.unitId);check(u.troops==2345&&u.food==6789&&u.gold==4321,"exact carried quantities");
            check(home.gold==gold-4321&&home.food==food-6789&&home.troops==troops-2345,"exact stock conservation");
            byte[] bytes=SaveCodec.encode(w);check(Arrays.equals(bytes,SaveCodec.encode(SaveCodec.decode(bytes))),"new content saves independently of catalog");
            if(side==0){World replay=SaveCodec.decode(bytes);w.nextTurn();replay.nextTurn();check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"expanded AI campaign resumes deterministically");}
        }
        System.out.println("PASS: "+checks+" mobile shortcut/content assertions: Lv3 construction, costs, rejection, saving, 42/18/12 cities, 670/180/120 officers, 3 factions, exact deployment and AI replay.");
    }
}
