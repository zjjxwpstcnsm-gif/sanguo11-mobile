package game.sanguo.core;
import java.util.*;
/** Original six current crew counterpart, explicit unit/cached stat inputs. */
public final class PcDuelResponseCurrentTest {
    public static void main(String[]args)throws Exception {
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);
        int[]natives={365,116,466,558,14,517},ids=new int[6];
        for(int i=0;i<6;i++){final int n=natives[i];ids[i]=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==n).findFirst().orElseThrow().officerId;}
        Hex first=null;outer:for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){Hex h=new Hex(q,r),next=new Hex(q+1,r);if(w.inside(next)&&w.cost(h,World.Weapon.SWORD)>0&&w.cost(next,World.Weapon.SWORD)>0&&w.cityAt(h)==null&&w.cityAt(next)==null&&!w.army.water(h)&&!w.army.water(next)){first=h;break outer;}}
        if(first==null)throw new AssertionError("No legal declared unit placement");
        for(int side=0;side<2;side++){
            var u=new World.Unit(side+1,w.officer(ids[side*3]).owner,ids[side*3],World.Weapon.SWORD,new Hex(first.q+side,first.r),5000,17000);
            u.deputies=new int[]{ids[side*3+1],ids[side*3+2]};w.units.add(u);
            for(int i=0;i<3;i++){var o=w.officer(ids[side*3+i]);w.strategy.releaseGovernor(o.id);o.unitId=u.id;o.cityId=-1;}
        }
        w.nextUnitId=3;w.governance.reconcile(false);byte[]before=SaveCodec.encode(w);
        var random=new PcDuelKernel.Random(23);var settings=new PcDuelKernel.OriginalSettings(false,-1,false,-1);
        for(int side=0;side<2;side++){
            var crew=new ArrayList<PcDuelAdmissionRules.Candidate>();
            for(var o:w.army.crew(w.unit(side+1))){var candidate=PcDuelAdmissionRules.current(w,o.id);crew.add(candidate);System.out.println("CURRENT native="+candidate.nativeId+" WAR="+candidate.war+" health="+candidate.health+" personality="+candidate.personality+" score="+PcDuelAdmissionRules.score(candidate,true));}
            int nominee=side==0?ids[1]:PcDuelAdmissionRules.best(crew,false);
            System.out.println("CURRENT side="+side+" nominee="+PcDuelAdmissionRules.current(w,nominee).nativeId+" crew="+PcDuelAdmissionRules.crewStrength(crew,true,nominee,true,w.relations::dislikes));
        }
        int actual=PcDuelResponseRules.currentResponse(w,ids[1],w.unit(1),w.unit(2),32,33,30,27,false,random,settings);
        // Separate original58a8a0 full-call receipt source-v2 gives40 and23.
        if(actual!=40||random.state!=23)throw new AssertionError("Original current response expected40/RNG23 actual="+actual+" RNG="+Integer.toUnsignedLong(random.state));
        var currentRng=new PcDuelKernel.Random(23);int current=PcDuelResponseRules.currentResponse(w,ids[1],w.unit(1),w.unit(2),false,currentRng,settings);
        if(current!=40||currentRng.state!=23)throw new AssertionError("Current source tech/stat response differs from original40/RNG23");
        if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("Current response mutated World/stored RNG");
        System.out.println("PASS current full response with source six crew/cached stat fixture; wholeWorld pure, ordinary496570/menu/APK pending");
    }
}
