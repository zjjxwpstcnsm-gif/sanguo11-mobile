package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Compare complete original counter selector, including saved RNG purity. */
public final class PcDuelCounterSelectionTest {
    public static void main(String[]args)throws Exception {
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);var joined=new HashMap<Integer,Integer>();for(var f:PcDuelSourceFacts.saved(w).values())joined.put(f.nativeId,f.officerId);int[]natives={365,116,466,558,14,517};
        Hex first=null;outer:for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){Hex h=new Hex(q,r),k=new Hex(q+1,r);if(w.inside(k)&&w.cost(h,World.Weapon.SWORD)>0&&w.cost(k,World.Weapon.SWORD)>0&&w.cityAt(h)==null&&w.cityAt(k)==null&&!w.army.water(h)&&!w.army.water(k)){first=h;break outer;}}
        for(int side=0;side<2;side++){int leader=joined.get(natives[3*side]);var u=new World.Unit(side+1,w.officer(leader).owner,leader,World.Weapon.SWORD,new Hex(first.q+side,first.r),5000,17000);u.deputies=new int[]{joined.get(natives[3*side+1]),joined.get(natives[3*side+2])};w.units.add(u);for(var o:w.army.crew(u)){w.strategy.releaseGovernor(o.id);o.unitId=u.id;o.cityId=-1;}}
        w.nextUnitId=3;w.governance.reconcile(false);byte[]before=SaveCodec.encode(w);int count=0,accepted=0,opponents=0;var settings=new PcDuelKernel.OriginalSettings(false,-1,false,-1);
        for(String line:Files.readAllLines(Path.of(args[0]))){if(line.startsWith("#")||line.isEmpty())continue;boolean opponent=line.startsWith("O\t");long[]v=Arrays.stream((opponent?line.substring(2):line).split("\t")).mapToLong(Long::parseLong).toArray();if(v.length!=(opponent?6:5))throw new AssertionError("Original counter columns differ");int side=(int)v[0];var random=new PcDuelKernel.Random((int)v[2]);int selected;
            if(opponent){var selection=PcDuelResponseRules.currentOpponent(w,joined.get((int)v[1]),w.unit(side+1),w.unit(2-side),false,random,settings);selected=selection.officer;if(selection.counter!=(v[5]!=0))throw new AssertionError("Original response counter flag differs");opponents++;}
            else selected=PcDuelResponseRules.currentCounterSelection(w,joined.get((int)v[1]),w.unit(side+1),w.unit(2-side),random);
            int nativeId=selected<0?-1:PcDuelSourceFacts.saved(w).get(selected).nativeId;
            if(nativeId!=v[3]||Integer.toUnsignedLong(random.state)!=v[4])throw new AssertionError("Original counter selected/RNG differ row="+count+" selected="+nativeId+" expected="+v[3]);if(selected>=0)accepted++;count++;
        }
        if(count!=192+opponents||accepted==0||!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("Counter wholeWorld/RNG purity or accepted/rejected coverage differs "+count+"/"+accepted);
        System.out.println("PASS original counter selection "+count+" cases accepted="+accepted+" fullOpponent="+opponents+"; normal command/GUI/APK pending");
    }
}
