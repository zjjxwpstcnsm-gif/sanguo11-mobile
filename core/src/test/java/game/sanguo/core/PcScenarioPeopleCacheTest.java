package game.sanguo.core;
import java.io.*;import java.util.*;
/** Cache only immutable saved source records; invalidation preserves exact parser semantics. */
public final class PcScenarioPeopleCacheTest {
    static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        for(var source:PcScenarioCatalog.all()){
            World w=PcScenarioCatalog.preview(source.identity.scenarioId);byte[] before=SaveCodec.encode(w),raw=w.extensions.get(PcScenarioPeople.NAMESPACE);
            var first=PcScenarioPeople.saved(w);check(first.size()==850,"original saved record coverage");
            World copied=SaveCodec.decode(before);check(first==PcScenarioPeople.saved(copied),"identical committed/save-decoded source blob reuses detached immutable records");
            for(int i=0;i<100;i++)check(first==PcScenarioPeople.saved(w),"same revision reuses immutable parse");
            check(Arrays.equals(before,SaveCodec.encode(w)),"repeated cache reads preserve wholeWorld/bothRNG");
            w.extensions.put(PcScenarioPeople.NAMESPACE,null);check(PcScenarioPeople.saved(w).isEmpty(),"removal invalidates cached original records");
            w.extensions.put(PcScenarioPeople.NAMESPACE,raw);var restored=PcScenarioPeople.saved(w);check(restored.size()==first.size(),"reinsert parses current namespace");
            for(int i=0;i<first.size();i++){var a=first.get(i);var b=restored.get(i);check(a.nativeId==b.nativeId&&a.officerId==b.officerId&&a.recordSha.equals(b.recordSha)&&a.fields.equals(b.fields)&&a.originalName.equals(b.originalName)&&a.unknown.equals(b.unknown),"complete source record identity");}
            byte[] bad=raw.clone();bad[0]^=1;w.extensions.put(PcScenarioPeople.NAMESPACE,bad);boolean rejected=false;try{PcScenarioPeople.saved(w);}catch(IOException expected){rejected=true;}check(rejected,"malformed replacement never returns old cached data");
            w.extensions.put(PcScenarioPeople.NAMESPACE,raw);check(PcScenarioPeople.saved(w).size()==850&&Arrays.equals(before,SaveCodec.encode(w)),"parser recovery and transient revisions preserve exact save");
            World authored=new World(24,20);authored.extensions.put(PcScenarioPeople.NAMESPACE,raw);check(PcScenarioPeople.saved(authored).isEmpty(),"authored immutable frame retains prior empty semantics");check(PcScenarioPeople.saved(w).size()==850,"source world cache remains independent");
            boolean immutable=false;try{restored.clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"cached outer records immutable");
            immutable=false;try{restored.get(0).fields.clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"cached field maps immutable");
        }
        System.out.println("PASS immutable PcScenarioPeople cache "+checks+" checks; current source records/save/RNG unchanged; actual APK latency acceptance pending");
    }
}
