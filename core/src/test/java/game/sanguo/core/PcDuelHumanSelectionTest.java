package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Independent full58ad60/counter/RNG receipt, actual source current bindings. */
public final class PcDuelHumanSelectionTest {
    public static void main(String[]args)throws Exception {
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);int[]nativeIds={365,116,466,558,14,517},ids=new int[6];
        for(int i=0;i<6;i++){final int n=nativeIds[i];ids[i]=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==n).findFirst().orElseThrow().officerId;}
        Hex first=null;outer:for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){Hex h=new Hex(q,r),k=new Hex(q+1,r);if(w.inside(k)&&w.cost(h,World.Weapon.SWORD)>0&&w.cost(k,World.Weapon.SWORD)>0&&w.cityAt(h)==null&&w.cityAt(k)==null&&!w.army.water(h)&&!w.army.water(k)){first=h;break outer;}}
        if(first==null)throw new AssertionError("No legal declaration");
        for(int side=0;side<2;side++){var u=new World.Unit(side+1,w.officer(ids[side*3]).owner,ids[side*3],World.Weapon.SWORD,new Hex(first.q+side,first.r),5000,17000);u.deputies=new int[]{ids[side*3+1],ids[side*3+2]};w.units.add(u);for(int slot=0;slot<3;slot++){var o=w.officer(ids[side*3+slot]);w.strategy.releaseGovernor(o.id);o.unitId=u.id;o.cityId=-1;}}
        w.nextUnitId=3;w.governance.reconcile(false);byte[]before=SaveCodec.encode(w);int count=0;var settings=new PcDuelKernel.OriginalSettings(false,-1,false,-1);
        for(String line:Files.readAllLines(Path.of(args[0]))){if(line.startsWith("#")||line.isEmpty())continue;long[]v=Arrays.stream(line.split("\t")).mapToLong(Long::parseLong).toArray();if(v.length!=8)throw new AssertionError("Original columns differ");int side=(int)v[0],actor=-1;for(int i=0;i<6;i++)if(nativeIds[i]==v[1])actor=ids[i];if(actor<0)throw new AssertionError("Original identity missing");
            for(int slot=0;slot<3;slot++){int actual=PcDuelResponseRules.currentCounter(w,actor,ids[(1-side)*3+slot],w.unit(side+1),w.unit(2-side));if(actual!=v[5+slot])throw new AssertionError("Original current counter differs native="+v[1]+" slot="+slot+" expected="+v[5+slot]+" actual="+actual);}
            var rng=new PcDuelKernel.Random((int)v[2]);int actual=PcDuelResponseRules.currentHumanSelection(w,actor,w.unit(side+1),w.unit(2-side),false,rng,settings);
            if(actual!=v[3]||Integer.toUnsignedLong(rng.state)!=v[4])throw new AssertionError("Original human selection differs native="+v[1]+" expected="+v[3]+" actual="+actual+" expectedRNG="+v[4]+" actualRNG="+Integer.toUnsignedLong(rng.state));count++;
        }
        if(count!=18||!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("WholeWorld/stored RNG purity or coverage differs");
        System.out.println("PASS current original human selection "+count+" full call/counter/RNG cases; ordinary command/GUI/APK pending");
    }
}
