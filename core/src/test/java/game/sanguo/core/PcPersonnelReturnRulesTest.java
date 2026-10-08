package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Independent original getter and raw authority, including invalid ranges. */
public final class PcPersonnelReturnRulesTest {
    public static void main(String[]args)throws Exception {
        var r=MapJson.object(MapJson.parse(Files.readString(Path.of(args[0])).replace("4294967295", "-1").getBytes(java.nio.charset.StandardCharsets.UTF_8)));int checks=0;
        for(Object row:MapJson.array(r.get("rows"))){var a=MapJson.array(row);int x=((Number)a.get(0)).intValue(),y=((Number)a.get(1)).intValue(),expected=((Number)a.get(2)).intValue();if(PcPersonnelReturnRules.duration(x,y)!=expected)throw new AssertionError("Original personnel table differs "+x+","+y);checks++;}
        var parents=MapJson.array(r.get("parents"));for(int i=0;i<87;i++){if(PcPersonnelReturnRules.parent(i)!=((Number)parents.get(i)).intValue())throw new AssertionError("Original parent differs");checks++;}
        if(args.length>1){var route=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[1]))));for(Object row:MapJson.array(route.get("rows"))){var a=MapJson.array(row);int x=((Number)a.get(0)).intValue(),y=((Number)a.get(1)).intValue(),expected=((Number)a.get(2)).intValue();if(PcPersonnelReturnRules.nextCitySource0(x,y)!=expected)throw new AssertionError("Original next-city differs "+x+","+y);checks++;}}
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);byte[]before=SaveCodec.encode(w);
        var cell=MapCoordinates.fromNationalSource(w,new game.sanguo.core.map.SourceGridCoord(80,80));if(PcPersonnelReturnRules.cityAt(w,cell)!=15||PcPersonnelReturnRules.duration(15,21)!=3)throw new AssertionError("Original actual duel return origin/duration differs");
        if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("Source region read changed fullWorld/RNG");
        System.out.println("PASS original personnel return rules "+checks+" + source duel origin15/home21/3; task37/campaign/fullTurn/APK pending");
    }
}
