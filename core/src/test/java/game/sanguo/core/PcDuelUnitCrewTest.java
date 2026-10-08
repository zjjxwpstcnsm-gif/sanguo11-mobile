package game.sanguo.core;
import java.util.*;
/** Actual source6 current abilities compared with original496f40 cache bytes. */
public final class PcDuelUnitCrewTest {
    public static void main(String[]args)throws Exception {
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);
        int[]natives={365,116,466,558,14,517},ids=new int[6];
        for(int i=0;i<6;i++){final int n=natives[i];ids[i]=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==n).findFirst().orElseThrow().officerId;}
        // Getter purity uses canonical fullWorld without claiming this is
        // ordinary deployment. Valid legal cells are selected explicitly.
        Hex first=null;outer:for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){Hex h=new Hex(q,r),k=new Hex(q+1,r);if(w.inside(k)&&w.cost(h,World.Weapon.SWORD)>0&&w.cost(k,World.Weapon.SWORD)>0&&w.cityAt(h)==null&&w.cityAt(k)==null){first=h;break outer;}}
        if(first==null)throw new AssertionError("No placement");
        for(int side=0;side<2;side++){var u=new World.Unit(side+1,w.officer(ids[side*3]).owner,ids[side*3],World.Weapon.SWORD,new Hex(first.q+side,first.r),5000,17000);u.deputies=new int[]{ids[side*3+1],ids[side*3+2]};w.units.add(u);for(int i=0;i<3;i++){var o=w.officer(ids[side*3+i]);w.strategy.releaseGovernor(o.id);o.unitId=u.id;o.cityId=-1;}}
        w.nextUnitId=3;w.governance.reconcile(false);byte[]before=SaveCodec.encode(w);
        int[][]expected={{93,90,79,74,91},{75,86,64,59,89}};
        for(int side=0;side<2;side++)if(!Arrays.equals(expected[side],PcDuelUnitStats.currentCrew(w,w.unit(side+1))))throw new AssertionError("Original current crew differs side="+side+" actual="+Arrays.toString(PcDuelUnitStats.currentCrew(w,w.unit(side+1))));
        int[][]apt={{3,3,2,2,1,3},{1,2,2,3,1,0}};
        for(int side=0;side<2;side++)if(!Arrays.equals(apt[side],PcDuelUnitStats.currentAptitudes(w,w.unit(side+1))))throw new AssertionError("Original6 current aptitudes differ");
        int[][]combat={{32,33},{30,27}};for(int side=0;side<2;side++){var result=PcDuelUnitStats.currentCombat(w,w.unit(side+1),false);if(result.attack!=combat[side][0]||result.defense!=combat[side][1])throw new AssertionError("Original current C9/CA differs");}
        if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("Current crew getter mutated fullWorld/RNG");
        var old=w.unit(1);var spear=new World.Unit(old.id,old.owner,old.officerId,World.Weapon.SPEAR,old.hex,old.troops,old.food);spear.deputies=old.deputies.clone();w.units.set(w.units.indexOf(old),spear);
        w.campaign.learned.computeIfAbsent(spear.owner,id->EnumSet.noneOf(Campaign.Tech.class)).addAll(EnumSet.of(Campaign.Tech.SPEAR_DRILL,Campaign.Tech.SUPPLY_RAID,Campaign.Tech.FOREST_AMBUSH,Campaign.Tech.ELITE_SPEAR));
        byte[]eliteSaved=SaveCodec.encode(w);World eliteCold=SaveCodec.decode(eliteSaved);var elite=PcDuelUnitStats.currentCombat(eliteCold,eliteCold.unit(1));
        // Separate original complete496570 elite-v1: Source0 own crew,
        // native equipment1/category0/status0/troops5000 =>94/97.
        if(elite.attack!=94||elite.defense!=97||!Arrays.equals(eliteSaved,SaveCodec.encode(eliteCold)))throw new AssertionError("Current source native3/full combat/coldSave differs");
        // Any deputy pair dislike bypasses *all* deputy attributes.
        var a=w.relations.people.computeIfAbsent(ids[1],id->new Relations.Person());a.dislikes.add(ids[2]);
        var leader=w.officer(ids[0]);int[]solo={leader.leadership,leader.war,leader.intelligence,leader.politics,leader.charm};
        if(!Arrays.equals(solo,PcDuelUnitStats.currentCrew(w,w.unit(1))))throw new AssertionError("Original whole crew dislike guard differs");
        if(!Arrays.equals(apt[0],PcDuelUnitStats.currentAptitudes(w,w.unit(1))))throw new AssertionError("Dislike incorrectly removed original aptitude contribution");
        System.out.println("PASS current original unit crew two independent source caches/fullWorld pure and any-pair dislike; normal unit stats/APK pending");
    }
}
