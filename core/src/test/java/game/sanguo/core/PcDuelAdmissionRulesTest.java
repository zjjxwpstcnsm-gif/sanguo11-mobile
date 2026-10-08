package game.sanguo.core;
import java.nio.file.*;import java.util.*;
/** Actual original scores and cohort results; candidate facts do not feed answers. */
public final class PcDuelAdmissionRulesTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static PcDuelAdmissionRules.Candidate health(PcDuelAdmissionRules.Candidate c,int hp){return new PcDuelAdmissionRules.Candidate(c.id,c.nativeId,hp,c.war,c.personality,c.treasureBonus,c.ruler);}
    public static void main(String[]args)throws Exception{
        Map<Integer,PcDuelAdmissionRules.Candidate>people=new HashMap<>();int scores=0,cohorts=0;
        for(String line:Files.readAllLines(Path.of(args[0]))){if(line.startsWith("#"))continue;String[]p=line.split("\t");int id=p[0].equals("C")?-1:Integer.parseInt(p[1]);
            if(p[0].equals("P"))people.put(id,new PcDuelAdmissionRules.Candidate(id,id,Integer.parseInt(p[2]),Integer.parseInt(p[3]),Integer.parseInt(p[4]),Integer.parseInt(p[5]),p[6].equals("1")));
            if(p[0].equals("S")){int hp=Integer.parseInt(p[2]),expected=Integer.parseInt(p[4]);check(PcDuelAdmissionRules.score(health(people.get(id),hp),p[3].equals("1"))==expected,"original candidate score native="+id+" health="+hp+" low="+p[3]+" expected="+expected);scores++;}
            if(p[0].equals("C")){List<PcDuelAdmissionRules.Candidate>crew=new ArrayList<>();for(String n:p[1].split(","))crew.add(health(people.get(Integer.parseInt(n)),Integer.parseInt(p[2])));check(PcDuelAdmissionRules.best(crew,p[5].equals("1"),p[3].equals("1"))==Integer.parseInt(p[4]),"original cohort "+line);cohorts++;}
        }
        check(scores==14*people.size()&&cohorts==60,"original boundary domain");
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);byte[]before=SaveCodec.encode(w);var facts=PcDuelSourceFacts.saved(w);
        for(var fact:facts.values()){var o=w.officer(fact.officerId);if(!w.life.present(o.id))continue;var actual=PcDuelAdmissionRules.current(w,o.id);var original=people.get(fact.nativeId);check(original!=null&&actual.war==original.war&&actual.health==original.health&&actual.personality==original.personality&&actual.treasureBonus==original.treasureBonus&&actual.ruler==original.ruler,"current source candidate binding native="+fact.nativeId);}
        check(Arrays.equals(before,SaveCodec.encode(w)),"source candidate reads preserve entire World and both RNG");
        System.out.println("PASS original cohort rules "+checks+" checks; ordinary admission/fees/APK pending");
    }
}
