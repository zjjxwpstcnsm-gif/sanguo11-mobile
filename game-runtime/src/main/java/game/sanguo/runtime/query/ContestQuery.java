package game.sanguo.runtime.query;
import java.util.*;
import game.sanguo.api.*;
import game.sanguo.core.*;

/** Pure detached facts; legality comes from the actual core input boundary. */
public final class ContestQuery {
    private static final String[] TOPICS={"故事","道理","時節"},PERSONALITIES={"膽小","冷靜","剛膽","莽撞"};
    private static String nativeLabel(int card){if(card==0)return "熟慮";if(card>=1&&card<=9)return TOPICS[(card-1)/3]+"·"+new String[]{"小","中","大"}[(card-1)%3];return new String[]{"大喝","詭辯","無視","鎮靜","憤怒"}[card-10];}
    public static ContestSnapshot capture(World w,StateToken state){
        Contests.Session current=w.contests.current();if(current==null)return ContestSnapshot.none(state);
        PcDebateCampaign.Facts f=current.nativeDebate();
        if(f!=null){
            List<ContestSnapshot.Speaker> speakers=new ArrayList<>();int[] ids={f.actorId,f.targetId},natives={f.actorNativeId,f.targetNativeId};
            for(int side=0;side<2;side++)speakers.add(new ContestSnapshot.Speaker(ids[side],natives[side],w.officer(ids[side]).name,PERSONALITIES[f.personalities.get(side)],f.sourceVariant,f.health.get(side),1000,f.anger.get(side),f.fury.get(side),f.intelligence.get(side),f.war.get(side)));
            List<ContestSnapshot.Card> cards=new ArrayList<>();for(int slot=0;slot<f.hand.size();slot++){int card=f.hand.get(slot);if(card>=0)cards.add(new ContestSnapshot.Card(slot,card,nativeLabel(card),f.cardErrors.get(slot)));}
            List<ContestSnapshot.Event> events=f.events.stream().map(e->new ContestSnapshot.Event(e.frame,e.nativeFunction,e.side,e.card,e.counter,e.reflected,e.choices)).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));
            String status=f.waitingCard?"请选择手牌":f.waitingMercy?"选择智力经验或势力技巧奖励":f.phase==9?"原战役结算仍待核实，当前对局已完整保留":"原对局状态已保留";
            return new ContestSnapshot(state,ContestSnapshot.Kind.DEBATE,current.id(),current.revision(),f.phase,f.sub,f.round,f.leader,f.winner,true,f.waitingCard,f.waitingMercy,false,current.purpose(),TOPICS[f.topic],f.sourceId,f.sourceVariant,f.sourceSha,status,speakers,cards,events);
        }
        List<ContestSnapshot.Speaker> speakers=new ArrayList<>();List<ContestSnapshot.Card> cards=new ArrayList<>();int round,leader,winner;String topic,status;boolean waiting=false,settlement=false;
        if(current.isDuel()){
            Duel d=current.duel();round=d.round();leader=-1;winner=d.winner();topic="";status=d.report();
            for(int side=0;side<2;side++){Duel.Fighter p=d.active(side);World.Officer o=w.officer(p.officerId());speakers.add(new ContestSnapshot.Speaker(o.id,-1,o.name,"","",p.hp(),100,p.spirit(),0,o.intelligence,w.contests.war(o)));}
        }else{
            Debate d=current.debate();round=d.round();leader=d.leader();winner=d.winner();topic=d.topic().label;status=d.report();waiting=winner==-2;settlement=!waiting;
            for(int side=0;side<2;side++){Debate.Speaker p=d.speaker(side);World.Officer o=w.officer(p.officerId());speakers.add(new ContestSnapshot.Speaker(o.id,-1,o.name,p.temper().label,"",p.hp(),100,p.anger(),p.fury(),o.intelligence,o.war));}
            for(int slot=0;slot<d.speaker(0).hand().size();slot++)cards.add(new ContestSnapshot.Card(slot,-1,d.speaker(0).hand().get(slot).label(),d.cardError(slot)));
        }
        return new ContestSnapshot(state,current.isDuel()?ContestSnapshot.Kind.DUEL:ContestSnapshot.Kind.DEBATE,current.id(),current.revision(),-1,-1,round,leader,winner,false,waiting,false,settlement,current.purpose(),topic,"","","",status,speakers,cards,List.of());
    }
    private ContestQuery(){}
}
