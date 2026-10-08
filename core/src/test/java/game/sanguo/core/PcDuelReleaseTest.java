package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Full original battle/release and ordered personnel phases provide expected
 * return fields. Constructed units are explicitly not ordinary admission. */
public final class PcDuelReleaseTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static int original(Map<String,Object>receipt,String field,int turn,int offset)throws Exception {
        Object rows=turn<0?receipt.get(field):MapJson.object(MapJson.array(receipt.get("returnPhases")).get(turn)).get("people");
        for(Object row:MapJson.array(rows)){var p=MapJson.object(row);if(((Number)p.get("nativeId")).intValue()==558){byte[]b=HexFormat.of().parseHex((String)p.get("hex"));return java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt(offset);}}throw new AssertionError("original target missing");
    }
    public static void main(String[]args)throws Exception {
        String raw=Files.readString(Path.of(args[0]));var numbers=java.util.regex.Pattern.compile("(?<=[\\s:\\[,])\\d{10,}(?=\\s*[,}\\]])").matcher(raw);String parsed=numbers.replaceAll(m->Integer.toString((int)Long.parseLong(m.group())));var receipt=MapJson.object(MapJson.parse(parsed.getBytes(java.nio.charset.StandardCharsets.UTF_8)));World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);var facts=PcDuelSourceFacts.saved(w);int[]natives={365,116,466,558,14,517},ids=new int[6];
        for(int i=0;i<6;i++){final int nativeId=natives[i];ids[i]=facts.values().stream().filter(f->f.nativeId==nativeId).findFirst().orElseThrow().officerId;}
        for(int side=0;side<2;side++){var leader=w.officer(ids[side*3]);var home=w.city(leader.cityId);Hex cell=null;outer:for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){Hex h=new Hex(q,r);if(w.cost(h,World.Weapon.SWORD)>0&&!w.army.water(h)&&w.cityAt(h)==null&&w.units.stream().noneMatch(v->v.hex.equals(h))&&PcPersonnelReturnRules.cityAt(w,h)==15){cell=h;break outer;}}if(cell==null)throw new AssertionError("no legal region15 land fixture");var u=new World.Unit(side+1,leader.owner,leader.id,World.Weapon.SWORD,cell,5000,10000);u.deputies=new int[]{ids[side*3+1],ids[side*3+2]};u.gold=700;u.energy=100;u.acted=true;w.units.add(u);for(int slot=0;slot<3;slot++){var o=w.officer(ids[side*3+slot]);w.strategy.releaseGovernor(o.id);o.unitId=u.id;o.cityId=-1;o.acted=true;}w.districts.deployed(home.id,u);PcGovernorPolicy.deployed(w,u,home);}
        w.nextUnitId=3;w.governance.reconcile(false);byte[]before=SaveCodec.encode(w);long random=w.strategy.getRandomState();int nativeRandom=PcNativeDebatePolicy.seed(w);var winner=w.unit(1);var loser=w.unit(2);var target=w.officer(ids[3]);
        PcDuelRelease.validateRelease(w,winner,loser,target.id);check(Arrays.equals(before,SaveCodec.encode(w)),"release preview wholeWorld/bothRNG pure");PcDuelRelease.apply(w,winner,loser,target.id);
        check(loser.officerId==ids[5]&&Arrays.equals(loser.deputies,new int[]{ids[4]}),"original replacement517/14");check(target.owner==3&&!w.government.captive(target.id)&&target.unitId<0,"released allegiance and no captive");
        check(PcDuelRelease.busy(w,target.id)&&w.strategy.busy(target.id)&&PcDuelRelease.remaining(w,target.id)==3,"task37 saved busy and duration3");check(PcGovernorPolicy.view(w).officerAdministrativeHomeNative.get(target.id)==21,"original home remains21");
        byte[]saved=SaveCodec.encode(w);check(Arrays.equals(saved,SaveCodec.encode(SaveCodec.decode(saved))),"full release World save roundtrip");
        boolean refused=false;try{PcDuelRelease.apply(w,winner,loser,target.id);}catch(java.io.IOException e){refused=true;}check(refused&&Arrays.equals(saved,SaveCodec.encode(w)),"duplicate release byte pure");
        for(int turn=0;turn<4;turn++){w=SaveCodec.decode(SaveCodec.encode(w));PcDuelRelease.tick(w);target=w.officer(ids[3]);int current=original(receipt,"personAfter",turn,0x9c),duration=original(receipt,"personAfter",turn,0x158)&255,task=original(receipt,"personAfter",turn,0x13c);
            check(target.cityId==PcPersonnelReturnRules.projectSite(w,current),"original turn current "+turn);check(PcDuelRelease.remaining(w,target.id)==duration,"original turn duration "+turn);check(PcDuelRelease.busy(w,target.id)==(task==37),"original task completion "+turn);check(target.acted==(task==37),"original acted completion "+turn);check(PcGovernorPolicy.view(w).officerAdministrativeHomeNative.get(target.id)==21,"home unchanged "+turn);check(w.strategy.getRandomState()==random&&PcNativeDebatePolicy.seed(w)==nativeRandom,"bothRNG unchanged "+turn);byte[]snapshot=SaveCodec.encode(w);check(Arrays.equals(snapshot,SaveCodec.encode(SaveCodec.decode(snapshot))),"fullWorld cold continuation "+turn);}
        System.out.println("PASS original RELEASE and saved task37 "+checks+" checks; constructed admission/terminal aggregate/normal fullTurn/APK pending");
    }
}
