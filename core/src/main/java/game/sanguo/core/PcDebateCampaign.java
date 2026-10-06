package game.sanguo.core;
import java.io.*;
import java.util.*;

/** Native model inside an actual campaign contest. Prototype is explicit opt-in;
 * campaign outcome rewards/settlement are intentionally pending native proof. */
public final class PcDebateCampaign {
    final int leftOfficer,rightOfficer;
    final String sourceId,sourceSha,sourceVariant,leftRecordSha,rightRecordSha;
    final int leftNative,rightNative;
    PcDebateModel model;
    int inputs,frames,rounds;
    final List<Recorded> actionEvents=new ArrayList<>();
    static final class Recorded {final int frame;final PcDebateUiEffects.Event event;Recorded(int frame,PcDebateUiEffects.Event event){this.frame=frame;this.event=event;}}
    PcDebateCampaign(World w,int actor,int target)throws IOException{
        String error=inputError(w,actor,target);if(error!=null)throw new IOException(error);
        PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);Map<Integer,PcContestProfiles.Fact> facts=PcContestProfiles.saved(w);
        PcContestProfiles.Fact a=facts.get(actor),b=facts.get(target);leftOfficer=actor;rightOfficer=target;leftNative=a.nativeId;rightNative=b.nativeId;
        sourceId=source.scenarioId;sourceSha=source.sha;sourceVariant=source.sourceVariant;leftRecordSha=a.recordSha;rightRecordSha=b.recordSha;
        World.Officer left=w.officer(actor),right=w.officer(target);
        int leftTalks=PcDebateCampaignPolicy.enabled(w)&&w.treasures.has(actor,Treasures.Kind.BOOK)?31:a.nativeTalkMask,rightTalks=PcDebateCampaignPolicy.enabled(w)&&w.treasures.has(target,Treasures.Kind.BOOK)?31:b.nativeTalkMask;
        model=new PcDebateModel(new PcDebateState(left.intelligence,right.intelligence,a.nativePersonality,b.nativePersonality,leftTalks,rightTalks,PcNativeDebatePolicy.seed(w),left.war,right.war),0);
        model.state.ui=new PcDebateUiEffects();model.human[0]=true;
        if(PcDebateCampaignPolicy.enabled(w)){model.state.ui.terminalPreferences[0]=w.relations.dislikes(actor,target)?0:w.relations.likes(actor,target)?1:-1;model.state.ui.terminalPreferences[1]=w.relations.dislikes(target,actor)?0:w.relations.likes(target,actor)?1:-1;}
        // Source relationship, effective book ownership and PC caller initiative
        // are still pending; prototype records those boundaries explicitly.
        advance(w);
    }
    private PcDebateCampaign(int actor,int target,int aNative,int bNative,String source,String sha,String variant,String aSha,String bSha){leftOfficer=actor;rightOfficer=target;leftNative=aNative;rightNative=bNative;sourceId=source;sourceSha=sha;sourceVariant=variant;leftRecordSha=aSha;rightRecordSha=bSha;}
    static String inputError(World w,int actor,int target){
        if(!PcNativeDebatePolicy.enabled(w))return "当前存档未启用原舌战策略";
        try{Map<Integer,PcContestProfiles.Fact> f=PcContestProfiles.saved(w);if(!f.containsKey(actor)||!f.containsKey(target))return "对战人物身份尚未核实";}catch(IOException e){return e.getMessage();}
        for(int id:new int[]{actor,target}){World.Officer o=w.officer(id);if(o==null||o.intelligence<0||o.intelligence>100||o.war<0||o.war>100)return "当前能力超出已核实的原舌战范围";}
        return null;
    }
    boolean waitingCard(){int side=model.sub==0?model.state.leader:model.sub==5?1-model.state.leader:-1;return model.phase==4&&side==0&&model.selected[0]<0;}
    boolean waitingMercy(){return model.phase==8&&model.sub==0&&model.state.terminalWinner==0;}
    void advance(World w){
        actionEvents.clear();int limit=2000;while(!waitingCard()&&!waitingMercy()&&model.phase!=9){if(--limit<0)throw new IllegalStateException("Original protocol did not reach next input");int previousPhase=model.phase;model.frame();frames++;if(previousPhase==3)rounds++;for(PcDebateUiEffects.Event e:model.state.ui.events)actionEvents.add(new Recorded(frames,e));}
        PcNativeDebatePolicy.setSeed(w,model.state.random.state);
    }
    String cardError(int slot){if(!waitingCard())return "当前不是选牌阶段";PcDebateState s=model.state;if(slot<0||slot>=s.left.slots||!s.legal(0,s.left.hand[slot]))return "该手牌现在不可使用";return null;}
    void card(World w,int slot){if(!model.selectHuman(0,slot))throw new IllegalArgumentException("Original input rejected");inputs++;advance(w);}
    void mercy(World w,boolean mercy){if(!waitingMercy())throw new IllegalStateException("Original terminal choice unavailable");model.terminalChoicePreference=mercy?0:1;model.frame();frames++;inputs++;advance(w);}
    public static final class Facts {
        public final int actorId,targetId,actorNativeId,targetNativeId,phase,sub,topic,leader,winner,inputs,frames,round;
        public final List<Integer> health,anger,fury,hand,personalities,intelligence,war;
        public final List<String> cardErrors;
        public final boolean waitingCard,waitingMercy;
        public final String sourceId,sourceSha,sourceVariant;
        public final List<EventFact> events;
        Facts(PcDebateCampaign c){PcDebateState s=c.model.state;actorId=c.leftOfficer;targetId=c.rightOfficer;actorNativeId=c.leftNative;targetNativeId=c.rightNative;sourceId=c.sourceId;sourceSha=c.sourceSha;sourceVariant=c.sourceVariant;phase=c.model.phase;sub=c.model.sub;topic=s.topic;leader=s.leader;winner=s.terminalWinner;inputs=c.inputs;frames=c.frames;round=c.rounds;personalities=List.of(s.left.temper,s.right.temper);intelligence=List.of(s.left.intelligence,s.right.intelligence);war=List.of(s.left.war,s.right.war);cardErrors=java.util.stream.IntStream.range(0,7).mapToObj(i->Objects.toString(c.cardError(i),"" )).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));health=List.of(s.left.health,s.right.health);anger=List.of(s.left.anger,s.right.anger);fury=List.of(s.left.fury,s.right.fury);hand=Arrays.stream(s.left.hand).boxed().collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));waitingCard=c.waitingCard();waitingMercy=c.waitingMercy();events=c.actionEvents.stream().map(EventFact::new).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));}
    }
    public static final class EventFact {
        public final int frame,nativeFunction,side,card,counter;public final boolean reflected;public final List<List<Integer>> choices;
        EventFact(Recorded r){PcDebateUiEffects.Event e=r.event;frame=r.frame;nativeFunction=e.nativeFunction;side=e.side;card=e.card;counter=e.counter;reflected=e.reflected;choices=e.choices.stream().map(c->List.of(c.side,c.action)).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));}
    }
    public Facts facts(){return new Facts(this);}
    void write(DataOutputStream d)throws IOException{d.writeInt(leftOfficer);d.writeInt(rightOfficer);d.writeInt(leftNative);d.writeInt(rightNative);d.writeUTF(sourceId);d.writeUTF(sourceSha);d.writeUTF(sourceVariant);d.writeUTF(leftRecordSha);d.writeUTF(rightRecordSha);d.writeInt(inputs);d.writeInt(frames);d.writeInt(rounds);byte[] b=PcDebateModelSave.write(model);d.writeInt(b.length);d.write(b);d.writeInt(actionEvents.size());for(Recorded r:actionEvents){PcDebateUiEffects.Event e=r.event;d.writeInt(r.frame);d.writeInt(e.nativeFunction);d.writeInt(e.side);d.writeInt(e.card);d.writeInt(e.counter);d.writeBoolean(e.reflected);d.writeInt(e.choices.size());for(PcDebateUiRandom.Choice choice:e.choices){d.writeInt(choice.side);d.writeInt(choice.action);}}}
    static PcDebateCampaign read(DataInputStream d)throws IOException{PcDebateCampaign c=new PcDebateCampaign(d.readInt(),d.readInt(),d.readInt(),d.readInt(),d.readUTF(),d.readUTF(),d.readUTF(),d.readUTF(),d.readUTF());c.inputs=d.readInt();c.frames=d.readInt();c.rounds=d.readInt();int n=d.readInt();if(n<64||n>4096)throw new IOException("Native model length invalid");byte[] b=new byte[n];d.readFully(b);c.model=PcDebateModelSave.read(b);n=d.readInt();if(n<0||n>4000)throw new IOException("Native event record count invalid");for(int i=0;i<n;i++){int frame=d.readInt(),function=d.readInt(),side=d.readInt(),card=d.readInt(),counter=d.readInt(),flag=d.readUnsignedByte();if(flag>1)throw new IOException("Native event boolean invalid");boolean reflected=flag==1;int count=d.readInt();if(count<0||count>2)throw new IOException("Native choice count invalid");List<PcDebateUiRandom.Choice> choices=new ArrayList<>();for(int j=0;j<count;j++)choices.add(new PcDebateUiRandom.Choice(d.readInt(),d.readInt()));c.actionEvents.add(new Recorded(frame,new PcDebateUiEffects.Event(function,side,card,reflected,counter,choices)));}return c;}
    void validate(World w,Contests.Session session)throws IOException{
        PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);Map<Integer,PcContestProfiles.Fact> facts=PcContestProfiles.saved(w);PcContestProfiles.Fact a=facts.get(leftOfficer),b=facts.get(rightOfficer);
        if(source==null||!sourceId.equals(source.scenarioId)||!sourceSha.equals(source.sha)||!sourceVariant.equals(source.sourceVariant)||a==null||b==null||a.nativeId!=leftNative||b.nativeId!=rightNative||!a.recordSha.equals(leftRecordSha)||!b.recordSha.equals(rightRecordSha)||session.leftRef!=leftOfficer||session.rightRef!=rightOfficer)throw new IOException("Native contest source identity differs");
        if(model.state.ui==null||!model.human[0]||model.human[1]||inputs<0||inputs>10000||inputs!=session.revision||frames<0||frames>100000||rounds<0||rounds>frames||model.state.random.state!=PcNativeDebatePolicy.seed(w))throw new IOException("Native contest policy/sequence invalid");
        boolean nativeSettlement=PcDebateCampaignPolicy.enabled(w)&&PcDebateCampaignPolicy.read(w).adoptedSession!=session.id;int leftTalks=nativeSettlement&&w.treasures.has(leftOfficer,Treasures.Kind.BOOK)?31:a.nativeTalkMask,rightTalks=nativeSettlement&&w.treasures.has(rightOfficer,Treasures.Kind.BOOK)?31:b.nativeTalkMask;
        if(model.state.left.temper!=a.nativePersonality||model.state.right.temper!=b.nativePersonality||model.state.left.talkMask!=leftTalks||model.state.right.talkMask!=rightTalks)throw new IOException("Native person attributes rebound");
        World.Officer left=w.officer(leftOfficer),right=w.officer(rightOfficer);World.City city=w.city(session.city);
        if(city==null||city.owner!=session.owner||left.owner!=session.owner||left.cityId!=city.id||right.owner==session.owner||left.unitId!=-1||right.unitId!=-1||!left.acted||!right.acted)throw new IOException("Native campaign participants invalid");
        if(!waitingCard()&&!waitingMercy()&&model.phase!=9)throw new IOException("Native saved model not at a stable input boundary");PcDebateModelSave.write(model);
        if(actionEvents.size()>4000)throw new IOException("Native action event count invalid");int prior=0;for(Recorded r:actionEvents){PcDebateUiEffects.Event e=r.event;if(r.frame<prior||r.frame>frames||e.side<0||e.side>1||e.card<-1||e.card>14||(e.counter!=-1&&e.counter!=13&&e.counter!=14)||e.choices.size()>2||!Set.of(0x5184a0,0x519150,0x5193f0,0x5195c0,0x5198d0,0x5199e0,0x519b90,0x519e00,0x519fd0,0x51a4a0,0x51a730,0x51a870,0x51aa70).contains(e.nativeFunction))throw new IOException("Native action event invalid");for(PcDebateUiRandom.Choice choice:e.choices)if(choice.side<0||choice.side>1||choice.action<4||choice.action>10)throw new IOException("Native action choice invalid");prior=r.frame;}
    }
}
