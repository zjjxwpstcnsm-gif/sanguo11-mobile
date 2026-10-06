package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
public final class MediaIdentityProbe {
 public static void main(String[] args)throws Exception{
  int index=0;for(PcScenarioCatalog.Source source:PcScenarioCatalog.all()){
   World w=args.length==0?PcScenarioCatalog.preview(source.identity.scenarioId):SaveCodec.decode(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(args[0],"source-"+index+".sg11")));index++;
   PcScenarioIdentity.Source actual=PcScenarioIdentity.saved(w);if(actual==null||!actual.scenarioId.equals(source.identity.scenarioId)||!actual.path.equals(source.identity.path)||!actual.sourceVariant.equals(source.identity.sourceVariant)||!actual.sha.equals(source.identity.sha))throw new AssertionError("Actual source identity differs from clicked source");byte[] before=SaveCodec.encode(w);Map<Integer,String> names=new HashMap<>();for(World.Officer o:w.officers)names.put(o.id,o.name);
   Map<Integer,PortraitMediaIdentity> identities=PortraitSavedSources.read(w.extensions.get(PcOfficerInfo.NAMESPACE),names);
   for(World.Officer o:w.officers){PortraitMediaIdentity id=identities.get(o.id);System.out.println(source.identity.scenarioId+"\t"+o.id+"\t"+o.name+"\t"+w.life.year()+"\t"+(id==null?"unknown":id.nativeId+"\t"+id.sourceVariant+"\t"+id.sourcePath+"\t"+id.sourceSha+"\t"+id.recordSha));}
   if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("saved media projection changes completeSave/RNG");
  }
 }
}
