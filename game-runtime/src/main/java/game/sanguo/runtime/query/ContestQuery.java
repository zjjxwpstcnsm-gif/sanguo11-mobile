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
        if(current.searchChoice()){
            try{var source=PcScenarioIdentity.saved(w);var people=PcScenarioPeople.saved(w);List<ContestSnapshot.Speaker> speakers=new ArrayList<>();for(int id:new int[]{w.contests.searchActorId(),w.contests.searchTargetId()}){var p=people.stream().filter(x->x.officerId==id).findFirst().orElseThrow();World.Officer o=w.officer(id);speakers.add(new ContestSnapshot.Speaker(id,p.nativeId,o.name,"",source.sourceVariant,0,0,0,0,o.intelligence,o.war));}
                int phase=w.contests.searchChoicePhase();return new ContestSnapshot(state,ContestSnapshot.Kind.SEARCH_CHOICE,current.id(),current.revision(),phase,0,0,-1,-1,true,false,false,false,phase==1?"搜索 · 是否招揽":"搜索招揽 · 是否舌战","",source.scenarioId,source.sourceVariant,source.sha,phase==1?"人物已发现；选择后结算原搜索回调":"招揽未成功；可进入舌战或放弃",speakers,List.of(),List.of());
            }catch(java.io.IOException e){throw new IllegalStateException(e);}
        }
        PcDuelCampaign.Facts duel=current.nativeDuel();
        if(duel!=null){
            try{
                var source=PcScenarioIdentity.saved(w);List<ContestSnapshot.Speaker> speakers=new ArrayList<>();List<ContestSnapshot.DuelTeam> teams=new ArrayList<>();
                for(var team:duel.teams){List<ContestSnapshot.DuelFighter> fighters=new ArrayList<>();for(var f:team.fighters){World.Officer o=w.officer(f.officerId);if(o==null)throw new IllegalStateException("原单挑人物连接失效");
                    fighters.add(new ContestSnapshot.DuelFighter(f.side,f.slot,f.officerId,f.nativeId,o.name,f.health,f.spirit,f.injury,f.stance,f.status,f.gear,f.active,f.moveCharges,f.terminalOutcome));
                    if(f.active)speakers.add(new ContestSnapshot.Speaker(f.officerId,f.nativeId,o.name,"",source.sourceVariant,f.health,100,f.spirit,0,o.intelligence,o.war));
                }teams.add(new ContestSnapshot.DuelTeam(team.side,team.activeSlot,team.count,team.context,team.attackBuff,team.defenseBuff,team.spiritBuff,team.human,fighters));}
                List<ContestSnapshot.DuelInput> choices=new ArrayList<>();
                for(var choice:duel.choices){String label=choice.label;if(choice.replacement>=0){final int slot=choice.replacement;var person=teams.get(0).fighters.stream().filter(f->f.slot==slot).findFirst().orElseThrow();label+=" · "+person.name;}
                    choices.add(new ContestSnapshot.DuelInput(choice.stance,choice.special,choice.replacement,choice.cost,label,choice.error));}
                List<ContestSnapshot.DuelDisposition> disposition=new ArrayList<>();for(var row:duel.disposition){World.Officer o=w.officer(row.officerId);if(o==null)throw new IllegalStateException("原终局处置人物连接失效");List<String>errors=new ArrayList<>();for(int action=0;action<4;action++){String error=w.contests.nativeDuelDispositionError(row.officerId,action);errors.add(error==null?"":error);}disposition.add(new ContestSnapshot.DuelDisposition(row.officerId,row.nativeId,o.name,row.mask,row.choice,row.error,errors));}
                ContestSnapshot.DuelInheritance inheritance=duel.heirRuler<0?null:new ContestSnapshot.DuelInheritance(duel.heirRuler,duel.chosenHeir,w.contests.nativeDuelHeirs().stream().map(h->new ContestSnapshot.DuelHeir(h.officerId,h.nativeId,h.name,h.error)).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList)));
                String settlementError=w.contests.nativeDuelNaturalDeathError();
                var aiActor=w.contests.nativeDuelAiActor();var humanActor=w.contests.nativeDuelHumanActor();var recipient=w.contests.nativeDuelRecruitItemRecipient();
                var facts=new ContestSnapshot.NativeDuel(duel.roundLimit,duel.frames,duel.inputs,duel.waitingInput,duel.terminal,teams,choices,disposition,duel.dispositionReady,duel.scene,duel.commandBoundary,duel.previousOpponentActed,duel.openingSpeechCode,inheritance,PcOpeningOptionsQuery.capture(w,state),settlementError,w.contests.nativeDuelLoyaltyInputEnabled(),duel.terminal&&w.contests.nativeDuelLoyaltyAdoptionError()==null,w.contests.nativeDuelLoyaltyAdoptionError(),w.contests.nativeDuelAiActorPolicyEnabled(),w.contests.nativeDuelAiActorPolicyAdoptionError()==null,w.contests.nativeDuelAiActorPolicyAdoptionError(),aiActor==null?-1:aiActor.officerId,aiActor==null?-1:aiActor.nativeId,aiActor==null?"":aiActor.name,w.contests.nativeDuelHumanActorPolicyEnabled(),w.contests.nativeDuelHumanActorPolicyAdoptionError()==null,w.contests.nativeDuelHumanActorPolicyAdoptionError(),humanActor==null?-1:humanActor.officerId,humanActor==null?-1:humanActor.nativeId,humanActor==null?"":humanActor.name,w.contests.nativeDuelRecruitItemPolicyEnabled(),w.contests.nativeDuelRecruitItemPolicyAdoptionError()==null,w.contests.nativeDuelRecruitItemPolicyAdoptionError(),recipient==null?-1:recipient.officerId,recipient==null?-1:recipient.nativeId,recipient==null?"":recipient.name,new ContestSnapshot.PhysicalRecovery(w.contests.nativeDuelPhysicalRecoveryEnabled(),w.contests.nativeDuelPhysicalRecoveryAdoptionError()==null,w.contests.nativeDuelPhysicalRecoveryAdoptionError()));
                String status=inheritance!=null?(inheritance.selectedHeir<0?"请选择本势力继承人，可保存后继续":"继承人已选择，确认战役结算"):duel.terminal?(duel.winner<0?"单挑平手，确认战役结算":duel.winner==0?"我方胜利，确认终局处置与战役结算":"我方败北，确认战役结算"):duel.waitingInput?(duel.phase==5&&duel.sub==3?"选择本合行动方针":duel.phase==5&&duel.sub==4?"选择继续上阵或换将":"选择招式或继续交锋"):"交锋进行中";
                return new ContestSnapshot(state,ContestSnapshot.Kind.DUEL,current.id(),current.revision(),duel.phase,duel.sub,duel.round,-1,duel.winner,true,false,false,duel.terminal&&(settlementError==null||settlementError.isEmpty())&&(disposition.isEmpty()||duel.dispositionReady),"单挑","",source.scenarioId,source.sourceVariant,source.sha,status,speakers,List.of(),List.of(),facts);
            }catch(java.io.IOException e){throw new IllegalStateException(e);}
        }
        PcDebateCampaign.Facts f=current.nativeDebate();
        if(f!=null){
            List<ContestSnapshot.Speaker> speakers=new ArrayList<>();int[] ids={f.actorId,f.targetId},natives={f.actorNativeId,f.targetNativeId};
            for(int side=0;side<2;side++)speakers.add(new ContestSnapshot.Speaker(ids[side],natives[side],w.officer(ids[side]).name,PERSONALITIES[f.personalities.get(side)],f.sourceVariant,f.health.get(side),1000,f.anger.get(side),f.fury.get(side),f.intelligence.get(side),f.war.get(side)));
            List<ContestSnapshot.Card> cards=new ArrayList<>();for(int slot=0;slot<f.hand.size();slot++){int card=f.hand.get(slot);if(card>=0)cards.add(new ContestSnapshot.Card(slot,card,nativeLabel(card),f.cardErrors.get(slot)));}
            List<ContestSnapshot.Event> events=f.events.stream().map(e->new ContestSnapshot.Event(e.frame,e.nativeFunction,e.side,e.card,e.counter,e.reflected,e.choices)).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));
            String status=f.waitingCard?"请选择手牌":f.waitingMercy?"选择智力经验或势力技巧奖励":f.phase==9?(w.contests.nativeCampaignSettlementEnabled()?"原登用终局可结算；启动准入/费用仍待核实":"旧原型已保留；需明确采用终局策略"):"原对局状态已保留";
            return new ContestSnapshot(state,ContestSnapshot.Kind.DEBATE,current.id(),current.revision(),f.phase,f.sub,f.round,f.leader,f.winner,true,f.waitingCard,f.waitingMercy,f.phase==9&&w.contests.nativeCampaignSettlementEnabled(),current.purpose(),TOPICS[f.topic],f.sourceId,f.sourceVariant,f.sourceSha,status,speakers,cards,events);
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
