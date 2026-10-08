package game.sanguo.core;
import java.util.*;
class session_b_human_inheritance_cases {
 public static void main(String[]a)throws Exception{World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,11,0,new PcDuelOptions(0,0,0));byte[]before=SaveCodec.encode(w);Map<Integer,Integer>ids=new HashMap<>();for(var f:PcDuelSourceFacts.saved(w).values())ids.put(f.nativeId,f.officerId);for(int[]p:new int[][]{{618,180},{343,180},{411,180},{365,493},{98,493},{432,493}}){var x=w.officer(ids.get(p[0]));var y=w.officer(ids.get(p[1]));System.out.println(Arrays.toString(p)+" wars "+x.war+"/"+y.war+" nonHarm "+PcDuelKinship.nonHarm(w,x.id,y.id)+" class "+PcDuelKinship.classifier(w,p[0],p[1])+" dislike "+w.relations.dislikes(x.id,y.id));}if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("Read changed World");}
}
