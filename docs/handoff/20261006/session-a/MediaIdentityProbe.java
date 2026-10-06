package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
public final class MediaIdentityProbe {
 public static void main(String[] args)throws Exception{
  for(PcScenarioCatalog.Source source:PcScenarioCatalog.all()){
   World w=PcScenarioCatalog.preview(source.identity.scenarioId);byte[] before=SaveCodec.encode(w);Map<Integer,String> names=new HashMap<>();for(World.Officer o:w.officers)names.put(o.id,o.name);
   Map<Integer,PortraitMediaIdentity> identities=PortraitSavedSources.read(w.extensions.get(PcOfficerInfo.NAMESPACE),names);
   for(World.Officer o:w.officers){PortraitMediaIdentity id=identities.get(o.id);System.out.println(source.identity.scenarioId+"\t"+o.id+"\t"+o.name+"\t"+w.life.year()+"\t"+(id==null?"unknown":id.nativeId+"\t"+id.sourceVariant+"\t"+id.sourcePath+"\t"+id.sourceSha+"\t"+id.recordSha));}
   if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("saved media projection changes completeSave/RNG");
  }
 }
}
