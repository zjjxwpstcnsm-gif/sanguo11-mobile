package game.sanguo.core;
import game.sanguo.runtime.GameSession;import java.util.*;
/** Actual new-source force/city/person command, no location/attribute edits. */
public final class PcHanLoyaltySessionTest {
 static int checks;static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
 public static void main(String[]args)throws Exception {
  boolean completed=false;
  outerSources:for(var source:PcScenarioCatalog.all()){
   World scan=PcScenarioCatalog.load(source.identity.scenarioId,-1,23);Map<Integer,World.Officer>rulers=new TreeMap<>();for(var o:scan.officers)if(o.owner>=0&&o.owner<42&&o.role==Strategy.Role.RULER&&scan.life.present(o.id))rulers.put(o.owner,o);
   for(var entry:rulers.entrySet()){
    int force=entry.getKey();var ruler=entry.getValue();int han=PcDebateCampaignRules.source(scan,ruler.id).field(49);if(han==1)continue;
    int actor=-1,target=-1,city=-1,expected=-1;
    outer:for(var c:scan.cities)if(c.owner==force)for(var a:scan.idle(c))for(var t:scan.officers)if(t.owner<0&&t.cityId==c.id&&PcDebateCampaignRules.source(scan,t.id).field(49)==han){
     var raw=PcDirectRecruitmentPolicy.eligible(scan,c,a,t);if(raw==null)continue;int probability=PcDirectRecruitmentPolicy.probability(scan,a,t);if(!PcDirectRecruitmentPolicy.decision(scan,a,t,raw,probability))continue;
     int value=PcDebateCampaignRules.joinLoyalty(scan,t,force);var strategy=PcDebateCampaignPolicy.read(scan);strategy.version=1;PcDebateCampaignPolicy.write(scan,strategy);int old=PcDebateCampaignRules.joinLoyalty(scan,t,force);strategy.version=2;PcDebateCampaignPolicy.write(scan,strategy);
     if(value<=old)continue;actor=a.id;target=t.id;city=c.id;expected=value;break outer;
    }
    System.out.println("Original source="+source.identity.scenarioId+" force="+force+" rulerHan="+han+" changedDirect="+(actor>=0));
    World w=actor>=0?PcScenarioCatalog.load(source.identity.scenarioId,force,23):null;
   if(actor<0)continue;
   try(GameSession game=new GameSession(w)){
    var view=game.legacyView();final int ai=actor,ti=target,ci=city;byte[]before=game.captureSave();var quote=view.draft.strategy.previewRecruitment(ci,ai,ti);check(quote.allowed()&&quote.nativeRules&&quote.nativeSuccess,"actual normal direct quote");check(Arrays.equals(before,game.captureSave()),"preview/cancel World/RNG pure");
    var result=game.legacy(view.draft,()->view.draft.strategy.recruitOfficer(ci,ai,ti));check(result.ok,"normal direct formal "+result.message);World joined=SaveCodec.decode(game.captureSave());check(joined.officer(ti).owner==force&&joined.officer(ti).loyalty==expected,"actual joined authoritative corrected loyalty "+expected);byte[]after=game.captureSave();check(!game.legacy(view.draft,()->view.draft.strategy.recruitOfficer(ci,ai,ti)).ok&&Arrays.equals(after,game.captureSave()),"double/old StateToken pure");game.replace(SaveCodec.decode(after));check(Arrays.equals(after,game.captureSave()),"full cold exact");
    for(int i=0;i<3;i++){var ticket=game.beginTurn();World next=SaveCodec.decode(ticket.initial());check(next.nextTurn().ok,"whole campaign turn");check(game.commitTurn(ticket,next),"whole turn once");byte[]save=game.captureSave();game.replace(SaveCodec.decode(save));check(Arrays.equals(save,game.captureSave()),"whole turn/allRNG cold exact");}
    System.out.println("PASS actual normal direct force="+force+" han="+han+" actor="+ai+" target="+ti+" city="+ci+" loyalty="+expected+" checks="+checks+"; menu/APK pending");completed=true;break outerSources;
   }
  }
  }
  check(completed,"actual normal direct equal0/2 candidate completed without field/position edits");
 }
}
