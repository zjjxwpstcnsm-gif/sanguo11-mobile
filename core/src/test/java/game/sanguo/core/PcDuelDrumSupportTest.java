package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Independent original grid scanner matrix: kind/owner/complete/rings. */
public final class PcDuelDrumSupportTest {
    public static void main(String[]args)throws Exception {
        var r=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);int nominated=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==116).findFirst().orElseThrow().officerId;var target=MapCoordinates.fromNationalSource(w,new game.sanguo.core.map.SourceGridCoord(80,80));byte[]before=SaveCodec.encode(w);int count=0,positive=0;
        for(Object row:MapJson.array(r.get("drumRows"))){var a=MapJson.array(row);int[]v=a.stream().mapToInt(n->((Number)n).intValue()).toArray();var cell=MapCoordinates.fromNationalSource(w,new game.sanguo.core.map.SourceGridCoord(v[3],v[4]));if(cell.distance(target)!=v[5])throw new AssertionError("Original scan/project source coordinate distance differs");
            var s=new War.Structure(9999,v[1],v[0]==11?War.StructureKind.DRUM:War.StructureKind.MUSIC,cell,800);s.complete=v[2]!=0;w.war.structures.add(s);boolean actual=PcDuelResponseRules.currentDrumSupport(w,nominated,target);if(actual!=(v[6]!=0))throw new AssertionError("Original DRUM predicate/radius differs row="+count);if(actual)positive++;w.war.structures.remove(s);if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("DRUM getter changed fullWorld/bothRNG");count++;
        }
        if(count!=488||positive!=36)throw new AssertionError("Original DRUM boundary coverage differs "+count+"/"+positive);System.out.println("PASS original DRUM current support "+count+" boundary cases; ordinary command/APK pending");
    }
}
