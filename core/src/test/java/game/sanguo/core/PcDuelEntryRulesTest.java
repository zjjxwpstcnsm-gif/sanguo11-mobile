package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Independent original589f70/50de30 bytes, with normalized identity joins. */
public final class PcDuelEntryRulesTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception {
        var r=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));World initial=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);var facts=PcDuelSourceFacts.saved(initial);var joined=new HashMap<Integer,Integer>();for(var f:facts.values())joined.put(f.nativeId,f.officerId);int[]natives={365,116,466,558,14,517};
        for(int side=0;side<2;side++){int leader=joined.get(natives[3*side]);var officer=initial.officer(leader);var home=initial.city(officer.cityId);Hex cell=null;outer:for(int q=0;q<initial.width;q++)for(int y=0;y<initial.height;y++){Hex h=new Hex(q,y);if(initial.cost(h,World.Weapon.SWORD)>0&&!initial.army.water(h)&&initial.cityAt(h)==null&&initial.units.stream().noneMatch(u->u.hex.equals(h))){cell=h;break outer;}}var unit=new World.Unit(side+1,officer.owner,leader,World.Weapon.SWORD,cell,5000,10000);unit.acted=true;unit.deputies=new int[]{joined.get(natives[side*3+1]),joined.get(natives[side*3+2])};initial.units.add(unit);for(var o:initial.army.crew(unit)){initial.strategy.releaseGovernor(o.id);o.unitId=unit.id;o.cityId=-1;o.acted=true;}PcGovernorPolicy.deployed(initial,unit,home);}
        initial.nextUnitId=3;initial.governance.reconcile(false);
        var settings=new PcDuelKernel.OriginalSettings(false,-1,false,-1);
        for(Object row:MapJson.array(r.get("rows"))){var expected=MapJson.object(row);boolean low=MapJson.bool(expected.get("lowHealthFixture"));if(low)PcDuelHealthPolicy.writeDuelResult(initial,Map.of(joined.get(14),49));else PcDuelHealthPolicy.writeDuelResult(initial,Map.of(joined.get(14),100));
            byte[]before=rawWorld(initial);int own=joined.get(((Number)expected.get("own")).intValue()),other=joined.get(((Number)expected.get("other")).intValue());var contexts=MapJson.array(expected.get("contexts"));var controls=MapJson.array(expected.get("controllers"));int scene=((Number)expected.get("scene")).intValue();
            var input=PcDuelEntryRules.prepare(initial,initial.unit(1),initial.unit(2),own,other,((Number)contexts.get(0)).intValue(),((Number)contexts.get(1)).intValue(),((Number)controls.get(0)).intValue()==0,((Number)controls.get(1)).intValue()==0,scene,settings);
            byte[]original=HexFormat.of().parseHex((String)expected.get("managerHex"));var originalCrew=MapJson.array(expected.get("nativeCrew"));
            for(int i=0;i<6;i++){int nativeId=((Number)originalCrew.get(i)).intValue();int id=nativeId<0?-1:joined.get(nativeId);check(input.officers[i]==id&&input.natives[i]==nativeId,"original filtered nominated-first crew");PcDuelKernel.writeManager(original,4*i,id<0?0:id);}
            for(int side=0;side<2;side++)PcDuelKernel.writeManager(original,0x18+4*side,side+1);
            check(Arrays.equals(input.manager,original),"all208 original entry-manager bytes own="+own+" other="+other+" low="+low);
            check(Arrays.equals(before,rawWorld(initial)),"entry getter fullWorld/extensions/bothRNG pure");
            var campaign=PcDuelCampaign.initialize(input.officers,input.natives,input.manager,input.actors,input.held,23,settings);check(Arrays.equals(PcDuelModelSave.write(campaign.state),PcDuelModelSave.write(PcDuelModelSave.read(PcDuelModelSave.write(campaign.state)))),"prepared model complete saved state roundtrip");
            if(expected.containsKey("modelHex")){
                var model=new PcDuelKernel(HexFormat.of().parseHex((String)expected.get("modelHex")));model.set(0,1);
                for(int side=0;side<2;side++){model.set(0x238+24*side+4,1);for(int slot=0;slot<3;slot++){int id=input.officers[side*3+slot];if(id>=0)model.set(model.fighter(side,slot),id);}}
                check(Arrays.equals(model.state,campaign.state.model.state),"all1436 original initialized model bytes");check(campaign.state.random.state==(int)Long.parseLong((String)expected.get("rngAfter")),"original initialized RNG");
            }
        }
        System.out.println("PASS original field entry manager "+checks+" checks; current command selection/ordinary opening/APK pending");
    }
    static byte[]rawWorld(World w)throws Exception {return SaveCodec.encode(w);}
}
