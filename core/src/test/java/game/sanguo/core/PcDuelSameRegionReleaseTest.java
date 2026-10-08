package game.sanguo.core;
import java.nio.*;import java.nio.file.*;import java.util.*;
/** Declared source formations compare full original release/task37 endpoints.
 * Ordinary deployment/battle/menu and APK remain separate acceptance. */
public final class PcDuelSameRegionReleaseTest {
    static int checks;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    static int raw(Map<String,Object>row,String key,int offset){return ByteBuffer.wrap(HexFormat.of().parseHex((String)row.get(key))).order(ByteOrder.LITTLE_ENDIAN).getInt(offset);}
    static World fixture(int targetNative,boolean current)throws Exception {
        String source=PcScenarioCatalog.all().get(0).identity.scenarioId;
        World w=current?PcScenarioCatalog.load(source,2,23,new PcDuelOptions(0,0,0)):PcScenarioCatalog.load(source,2,23);
        var facts=PcDuelSourceFacts.saved(w);int[]natives=targetNative==558?new int[]{365,116,466,558,14,517}:new int[]{365,116,466,517,-1,-1};int[]ids=new int[6];
        for(int i=0;i<6;i++){final int n=natives[i];ids[i]=n<0?-1:facts.values().stream().filter(f->f.nativeId==n).findFirst().orElseThrow().officerId;}
        for(int side=0;side<2;side++){var leader=w.officer(ids[side*3]);var home=w.city(leader.cityId);Hex cell=null;
            outer:for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){Hex h=new Hex(q,r);if(w.cost(h,World.Weapon.SWORD)>0&&!w.army.water(h)&&w.cityAt(h)==null&&w.units.stream().noneMatch(v->v.hex.equals(h))&&PcPersonnelReturnRules.cityAt(w,h)==21){cell=h;break outer;}}
            check(cell!=null,"valid land in original region21");var u=new World.Unit(side+1,leader.owner,leader.id,World.Weapon.SWORD,cell,5000,10000);u.deputies=java.util.stream.IntStream.of(ids[side*3+1],ids[side*3+2]).filter(id->id>=0).toArray();u.gold=997;u.energy=100;u.acted=true;w.units.add(u);
            for(int slot=0;slot<3;slot++){int id=ids[side*3+slot];if(id<0)continue;var o=w.officer(id);w.strategy.releaseGovernor(id);o.unitId=u.id;o.cityId=-1;o.acted=true;}w.districts.deployed(home.id,u);PcGovernorPolicy.deployed(w,u,home);
        }w.nextUnitId=3;w.governance.reconcile(false);return w;
    }
    public static void main(String[]args)throws Exception {
        var receipt=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));
        for(Object value:MapJson.array(receipt.get("cases"))){var row=MapJson.object(value);int nativeId=((Number)row.get("nativeId")).intValue();World w=fixture(nativeId,true);var target=w.officer(w.unit(2).officerId);int id=target.id;byte[]before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();int nativeRng=PcNativeDebatePolicy.seed(w);
            PcDuelRelease.validateRelease(w,w.unit(1),w.unit(2),id);check(Arrays.equals(before,SaveCodec.encode(w)),"zero travel release preview pure");PcDuelRelease.apply(w,w.unit(1),w.unit(2),id);
            check(target.cityId==PcPersonnelReturnRules.projectSite(w,raw(row,"personAfter",0x9c)),"original release current location");check(PcDuelRelease.busy(w,id)==(raw(row,"personAfter",0x13c)==37),"original task37 persists at zero duration");check(PcDuelRelease.remaining(w,id)==(raw(row,"personAfter",0x158)&255),"original zero duration");check(target.acted&&!w.strategy.officerState(id).canAct,"released officer remains occupied until personnel phase");check(target.unitId<0&&target.owner==3&&!w.government.captive(id),"allegiance and field cleanup");
            int governor=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==109).findFirst().orElseThrow().officerId;check(w.city(PcPersonnelReturnRules.projectSite(w,21)).governorId==governor&&w.officer(governor).role==Strategy.Role.GOVERNOR,"original full callback appoints resident109 while zero return remains busy");check(PcGovernorPolicy.data(w).assignments.get(id).home==21,"home retained");check(nativeId!=517||target.role==Strategy.Role.RULER,"ruler role retained");byte[]saved=SaveCodec.encode(w);check(Arrays.equals(saved,SaveCodec.encode(SaveCodec.decode(saved))),"zero task full World cold exact");
            boolean refused=false;try{PcDuelRelease.apply(w,w.unit(1),w.unit(2),id);}catch(java.io.IOException e){refused=true;}check(refused&&Arrays.equals(saved,SaveCodec.encode(w)),"duplicate no partial write");
            int turn=0;for(Object phaseValue:MapJson.array(row.get("phases"))){var phase=MapJson.object(phaseValue);w=SaveCodec.decode(SaveCodec.encode(w));PcDuelRelease.tick(w);target=w.officer(id);check(target.cityId==PcPersonnelReturnRules.projectSite(w,raw(phase,"personHex",0x9c)),"original personnel location "+turn);check(PcDuelRelease.busy(w,id)==(raw(phase,"personHex",0x13c)==37),"original phase task "+turn);check(PcDuelRelease.remaining(w,id)==(raw(phase,"personHex",0x158)&255),"original phase duration "+turn);check(w.strategy.getRandomState()==rng&&PcNativeDebatePolicy.seed(w)==nativeRng,"both RNG unchanged");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(SaveCodec.decode(SaveCodec.encode(w)))),"full phase cold exact");turn++;}
            World old=fixture(nativeId,false);byte[]legacy=SaveCodec.encode(old);boolean blocked=false;try{PcDuelRelease.validateRelease(old,old.unit(1),old.unit(2),old.unit(2).officerId);}catch(java.io.IOException e){blocked=true;}check(blocked&&Arrays.equals(legacy,SaveCodec.encode(old)),"previous no native option policy stays byte pure");
        }System.out.println("PASS original same-region task37 release "+checks+" checks; declared source units, APK pending");
    }
}
