package game.sanguo.core;
import java.nio.*;import java.nio.file.*;import java.util.*;
/** Compare every mapped native anchor against nine complete original death
 * callbacks. Uses production execution and cold whole-world saves; original
 * standalone cleanup is not a campaign trigger or Android acceptance. */
public final class PcDuelSwornPolicyTest {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static int n(Map<String,Object>m,String k){return ((Number)m.get(k)).intValue();}
    static World opening()throws Exception{return PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,24,new PcDuelOptions(0,0,0));}
    static Map<Integer,Integer> ids(World w)throws Exception{var ids=new TreeMap<Integer,Integer>();for(var f:PcDuelSourceFacts.saved(w).values())ids.put(f.nativeId,f.officerId);return ids;}
    static void deploy(World w,int id,int unitId,Hex hex)throws Exception{var o=w.officer(id);var home=w.city(o.cityId);var u=new World.Unit(unitId,o.owner,id,World.Weapon.SWORD,hex,5000,17000);u.gold=997;u.acted=true;w.units.add(u);w.strategy.releaseGovernor(id);o.unitId=u.id;o.cityId=-1;o.acted=true;w.districts.deployed(home.id,u);PcGovernorPolicy.deployed(w,u,home);w.nextUnitId=Math.max(w.nextUnitId,unitId+1);w.governance.reconcile(false);}
    static Hex land(World w){for(int q=0;q<w.width;q++)for(int y=0;y<w.height;y++){var a=new Hex(q,y);var b=new Hex(q+1,y);if(w.inside(b)&&w.cost(a,World.Weapon.SWORD)>0&&w.cost(b,World.Weapon.SWORD)>0&&!w.army.water(a)&&!w.army.water(b)&&w.cityAt(a)==null&&w.cityAt(b)==null)return a;}throw new AssertionError("land pair");}
    public static void main(String[] args)throws Exception {
        var receipt=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));check(Boolean.TRUE.equals(receipt.get("wholeWorldAndRngRestored")),"original World/RNG restored");
        for(Object value:MapJson.array(receipt.get("rows"))){World w=opening();var ids=ids(w);byte[] opening=SaveCodec.encode(w);check(w.extensions.get(PcDuelSwornPolicy.NAMESPACE)==null,"no implicit opening overlay");var raw=w.extensions.get(PcDuelRuntimeFacts.NAMESPACE).clone();int v=ids.get(365);Hex land=land(w);deploy(w,v,1,land);int next=2;
            for(Object stepValue:MapJson.array(MapJson.object(value).get("steps"))){var step=MapJson.object(stepValue);int target=ids.get(n(step,"target"));deploy(w,target,next,new Hex(land.q+1,land.r));byte[] before=SaveCodec.encode(w);PcDuelExecution.validate(w,w.unit(1),w.unit(next),target);check(Arrays.equals(before,SaveCodec.encode(w)),"production execution preview fullWorld/RNG pure");PcDuelHealthPolicy.writeDuelResult(w,Map.of(target,1));PcDuelExecution.apply(w,w.unit(1),w.unit(next),target);next++;check(w.life.state(target)==Lifecycle.State.DEAD,"actual terminal death");check(Arrays.equals(raw,w.extensions.get(PcDuelRuntimeFacts.NAMESPACE)),"source anchors immutable");
                var facts=PcDuelSourceFacts.saved(w);var runtime=PcDuelRuntimeFacts.saved(w);
                for(Object after:MapJson.array(step.get("after"))){var row=MapJson.object(after);int nativeId=n(row,"nativeId");if(!ids.containsKey(nativeId))continue;check(PcDuelSwornPolicy.current(w,nativeId)==n(row,"anchor"),"all670 original currentanchors "+nativeId);PcDuelRecruitmentAdmission.unchangedGroup(w,ids.get(nativeId),runtime,facts);}
                byte[] save=SaveCodec.encode(w);w=SaveCodec.decode(save);check(Arrays.equals(save,SaveCodec.encode(w)),"death currentanchors wholeWorld/RNG cold exact");check(w.relations.links(target,Relations.Kind.SWORN).isEmpty(),"dead public links cleared");
                for(int nativeId:new int[]{98,432,635})if(w.life.present(ids.get(nativeId)))PcDuelRecruitmentAdmission.preview(w,ids.get(nativeId),v,1);
                check(Arrays.equals(save,SaveCodec.encode(w)),"postdeath ordinary recruitment previews fullWorld/RNG pure");
            }
            World old=SaveCodec.decode(opening);check(old.extensions.get(PcDuelSwornPolicy.NAMESPACE)==null&&Arrays.equals(opening,SaveCodec.encode(old)),"previous save remains byteexact without overlay");
        }
        World w=opening();var ids=ids(w);int dead=ids.get(635);var plan=PcDuelSwornPolicy.death(w,dead,-1);PcDuelSwornPolicy.apply(w,dead,plan);byte[] good=SaveCodec.encode(w);byte[] bad=w.extensions.get(PcDuelSwornPolicy.NAMESPACE).clone();bad[3]^=1;w.extensions.put(PcDuelSwornPolicy.NAMESPACE,bad);boolean rejected=false;try{SaveCodec.encode(w);}catch(Exception e){rejected=true;}check(rejected,"corrupt overlay magic rejected");w=SaveCodec.decode(good);int a=ids.get(98),b=ids.get(432);w.relations.unlink(a,b,Relations.Kind.SWORN);byte[] edited=SaveCodec.encode(w);w=SaveCodec.decode(edited);rejected=false;try{PcDuelSwornPolicy.current(w,98);}catch(Exception e){rejected=true;}check(rejected,"engineering edit invalidates native trust without unsavable World");check(Arrays.equals(edited,SaveCodec.encode(w)),"edited World cold exact");
        System.out.println("PASS original sworn death "+checks+" checks; full natural campaign/Android and surviving ruler loyalty callback pending");
    }
}
