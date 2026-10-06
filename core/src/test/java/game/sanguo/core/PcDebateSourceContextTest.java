package game.sanguo.core;
import java.util.*;
/** Current source-backed model inputs; compare structured full-original context receipt, not pointer addresses. */
public final class PcDebateSourceContextTest {
 public static void main(String[] args)throws Exception{
  World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,42);Map<Integer,PcContestProfiles.Fact> f=PcContestProfiles.saved(w);World.Officer a=null,b=null;
  for(var p:PcScenarioPeople.saved(w)){if(p.nativeId==116)a=w.officer(p.officerId);if(p.nativeId==222)b=w.officer(p.officerId);}
  if(a==null||b==null)throw new AssertionError("strict original source identity missing");var af=f.get(a.id);var bf=f.get(b.id);
  PcDebateState s=new PcDebateState(a.intelligence,b.intelligence,af.nativePersonality,bf.nativePersonality,af.nativeTalkMask,bf.nativeTalkMask,23,a.war,b.war);
  System.out.println("source actor116="+a.id+" iq="+a.intelligence+" war="+a.war+" temper="+af.nativePersonality+" mask="+af.nativeTalkMask+" owner="+a.owner+" city="+a.cityId);
  System.out.println("source target222="+b.id+" iq="+b.intelligence+" war="+b.war+" temper="+bf.nativePersonality+" mask="+bf.nativeTalkMask+" owner="+b.owner+" city="+b.cityId);
  System.out.println("seed="+s.random.state+" leftHand="+Arrays.toString(s.left.hand)+" rightHand="+Arrays.toString(s.right.hand)+" leftSpecial="+Arrays.toString(s.left.specialPool)+" rightSpecial="+Arrays.toString(s.right.specialPool));
  if(a.intelligence!=55||b.intelligence!=36||s.random.state!=1912572727||!Arrays.equals(s.left.hand,new int[]{0,4,10,14,-1,-1,-1})||!Arrays.equals(s.right.hand,new int[]{0,4,5,8,-1,-1,-1}))throw new AssertionError("actual full original source initialization mismatch");
  System.out.println("PASS source0 original116/222 initial current capabilities/native seed/full hands after complete original manager/model initialization; normal trigger and campaign settlement separate");
 }
}
