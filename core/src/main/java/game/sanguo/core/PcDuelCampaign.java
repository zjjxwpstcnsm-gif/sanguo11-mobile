package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Complete numeric model driver. Campaign admission and settlement own when
 * this state is created/committed; no presentation callback can mutate it. */
public final class PcDuelCampaign {
    PcDuelModelSave.State state;
    final PcDuelKernel.OriginalSettings settings;
    int frames,inputs;
    boolean commandBoundary,opponentActed;int openingVoice=-1;
    PcDuelDisposition disposition;
    boolean heirProtocol,naturalHeir;int heirRuler=-1,chosenHeir=-1;
    private PcDuelCampaign(PcDuelModelSave.State state,PcDuelKernel.OriginalSettings settings){this.state=state;this.settings=settings;}
    static PcDuelCampaign initialize(int[]officers,int[]natives,byte[]manager,PcDuelKernel.Actor[][]actors,int[][][]held,int seed,PcDuelKernel.OriginalSettings settings)throws IOException {
        PcDuelKernel m=new PcDuelKernel(new byte[0x59c]);m.set(0,1);var random=new PcDuelKernel.Random(seed);byte[]copy=manager.clone();
        if(!m.initializeModel(copy,1,actors,held,random))throw new IOException("原单挑初始化人物无效");
        var state=new PcDuelModelSave.State(m,copy,random,officers,natives);PcDuelModelSave.validate(state);return new PcDuelCampaign(state,settings);
    }
    /** New ordinary command boundary records the opponent's existing action
     * state rather than spending it. Earlier saved formats keep their behavior. */
    void commandBoundary(boolean previousOpponentActed,int voice)throws IOException {
        if(commandBoundary||frames!=0||inputs!=0||!settings.separateDeath||voice!= -1&&voice!=35&&voice!=36)throw new IOException("原单挑命令边界无效或已保存");
        commandBoundary=true;opponentActed=previousOpponentActed;openingVoice=voice;
    }
    /** Detached numeric facts. Reading them never invokes a frame, relation
     * getter, display callback or random generator. */
    public static final class FighterFacts {
        public final int side,slot,officerId,nativeId,health,spirit,injury,stance,status,gear,terminalOutcome;
        public final boolean active;
        public final List<Integer> moveCharges;
        private FighterFacts(PcDuelCampaign c,int side,int slot){
            PcDuelKernel m=c.state.model;int at=m.fighter(side,slot),index=side*3+slot;
            this.side=side;this.slot=slot;officerId=c.state.officers[index];nativeId=c.state.natives[index];
            terminalOutcome=c.terminal()?PcDuelKernel.readManager(c.state.manager,0x64+4*index):-1;
            health=m.get(at+4);spirit=m.get(at+8);injury=m.get(at+12);stance=m.get(at+16);status=m.get(at+20);gear=m.get(at+28);
            active=m.activeIndex(side)==slot;List<Integer> charges=new ArrayList<>();for(int move=0;move<8;move++)charges.add(m.get(at+32+4*move));moveCharges=List.copyOf(charges);
        }
    }
    public static final class TeamFacts {
        public final int side,activeSlot,count,context,attackBuff,defenseBuff,spiritBuff;
        public final boolean human;
        public final List<FighterFacts> fighters;
        private TeamFacts(PcDuelCampaign c,int side){
            PcDuelKernel m=c.state.model;int at=0x24+side*0xec;this.side=side;activeSlot=m.get(at+0xc4);count=m.get(at+0xc0);context=m.get(at+0xc8);human=m.get(at+0xd8)==0;
            attackBuff=m.get(at+0xdc);defenseBuff=m.get(at+0xe0);spiritBuff=m.get(at+0xe4);
            List<FighterFacts> rows=new ArrayList<>();for(int slot=0;slot<3;slot++)if(c.state.officers[side*3+slot]>=0)rows.add(new FighterFacts(c,side,slot));fighters=List.copyOf(rows);
        }
    }
    public static final class Facts {
        public final int phase,sub,round,roundLimit,winner,frames,inputs,scene,openingSpeechCode;
        public final boolean commandBoundary,previousOpponentActed;
        public final boolean waitingInput,terminal;
        public final List<TeamFacts> teams;
        public final List<InputFacts> choices;
        public final List<DispositionFacts> disposition;
        public final boolean dispositionReady;
        public final int heirRuler,chosenHeir;
        private Facts(PcDuelCampaign c){heirRuler=c.heirRuler;chosenHeir=c.chosenHeir;commandBoundary=c.commandBoundary;previousOpponentActed=c.opponentActed;openingSpeechCode=c.openingVoice;scene=PcDuelKernel.readManager(c.state.manager,0x44);phase=c.phase();sub=c.sub();round=c.state.model.get(0x10);roundLimit=c.state.model.get(0x18);winner=c.state.model.get(0x588);frames=c.frames;inputs=c.inputs;waitingInput=c.waitingInput();terminal=c.terminal();teams=List.of(new TeamFacts(c,0),new TeamFacts(c,1));choices=c.choices();disposition=c.disposition==null?List.of():c.disposition.rows.stream().map(DispositionFacts::new).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));dispositionReady=c.disposition!=null&&c.disposition.confirmationError()==null&&(!c.heirProtocol||c.chosenHeir>=0);}
    }
    public static final class DispositionFacts {
        public final int officerId,nativeId,mask,choice;public final String error;
        DispositionFacts(PcDuelDisposition.Row row){officerId=row.officerId;nativeId=row.nativeId;mask=row.mask;choice=row.choice;error=row.choice==PcDuelDisposition.RECRUIT&&!row.recruitmentAdmitted?"已保存的登用选择尚未完成原成功判定，待办已保留":"";}
    }
    /** Same stage-specific validator as submission; no RNG or frame advances. */
    public static final class InputFacts {
        public final int stance,special,replacement,cost;
        public final String label,error;
        private InputFacts(PcDuelCampaign c,String label,int stance,int special,int replacement){
            this.label=label;this.stance=stance;this.special=special;this.replacement=replacement;
            cost=special>=0?PcDuelKernel.COST[special]:0;
            String rejected=c.inputError(stance,special,replacement,false);error=rejected==null?"":rejected;
        }
    }
    private List<InputFacts> choices(){
        if(!waitingInput()||terminal())return List.of();
        var out=new ArrayList<InputFacts>();int p=phase(),s=sub();
        if(p==5&&s==3){String[]labels={"重视攻击","重视防御","重视斗志","重视一击"};for(int stance=0;stance<4;stance++)out.add(new InputFacts(this,labels[stance],stance,-1,-1));}
        else if(p==5&&s==4){out.add(new InputFacts(this,"继续上阵",-1,-1,-1));for(int slot=0;slot<3;slot++)if(state.officers[slot]>=0&&slot!=state.model.activeIndex(0))out.add(new InputFacts(this,"换将",-1,-1,slot));}
        else if(p==3||p==7){out.add(new InputFacts(this,"继续交锋",-1,-1,-1));String[]labels={"必杀技","集气","坚守","退却","急所","无双","暗器","伪退却"};for(int move=0;move<8;move++)out.add(new InputFacts(this,labels[move],-1,move,-1));}
        return List.copyOf(out);
    }
    public Facts facts(){return new Facts(this);}
    private int phase(){int next=state.model.get(8);return next>=0&&next<=12?next:state.model.get(4);}
    private int sub(){int next=state.model.get(8);return next>=0&&next<=12?0:state.model.get(12);}
    /** Original round arbitration, pose, swap and special selection boundaries.
     * Saves retain numerical frames, support status, queues and RNG exactly. */
    boolean waitingInput(){int p=phase(),s=sub();boolean boundary=p==3&&s==0||p==5&&(s==3||s==4)||p==7&&s==3&&state.model.get(0x598)==0;return boundary&&(!settings.separateDeath||state.model.get(0x24+0xd8)==0);}
    /** Stage-specific platform input. Original source option flags remain the
     * caller's explicit binding; no boolean option is guessed from difficulty. */
    String inputError(int stance,int special,int replacement,boolean retreatRestriction){
        if(terminal())return "原单挑已到终局";
        if(!waitingInput()||state.model.get(0x24+0xd8)!=0)return "当前不在玩家输入阶段";
        if(state.model.get(0x110+0xd8)!=1)return "当前对方人控配置尚未核实";
        int p=phase(),s=sub(),active=state.model.activeIndex(0);
        if(p==3||p==7){
            if(stance!=-1||replacement!=-1||special< -1||special>7)return "当前仅接受必杀选择或继续";
            if(special>=0&&!state.model.legal(0,active,special,retreatRestriction)){
                int at=state.model.fighter(0,active);
                if(!state.model.available(0,active,special))return "当前武将没有该必杀或使用次数已耗尽";
                if(state.model.get(at+8)<PcDuelKernel.COST[special])return "斗志不足：需要"+PcDuelKernel.COST[special]+"，当前"+state.model.get(at+8);
                if(special==7&&state.model.get(0x10)<15)return "伪退却需达到第15合，当前第"+state.model.get(0x10)+"合";
                return "当前场合不能退却";
            }
        }else if(p==5&&s==3){if(stance<0||stance>3||special!=-1||replacement!=-1)return "请选择当前行动方针";}
        else if(p==5&&s==4){
            if(stance!=-1||special!=-1||replacement< -1||replacement>2)return "当前仅接受换将或继续";
            if(replacement>=0){int at=state.model.fighter(0,replacement),status=state.model.get(at+20);if(replacement==active||state.officers[replacement]<0||status!=0&&status!=1)return "请选择已经支援到场的其他武将";}
        }
        return null;
    }
    boolean terminal(){return state.model.get(4)==12&&state.model.get(12)==6;}
    interface SupportBinding {PcDuelKernel.SupportFacts[][] current(PcDuelKernel model)throws IOException;}
    int frame(PcDuelKernel.Actor[][]actors,SupportBinding support,boolean[][]relations,boolean request,int stance,int move)throws IOException {return frame(actors,support,relations,request,stance,move,-1);}
    int frame(PcDuelKernel.Actor[][]actors,SupportBinding support,boolean[][]relations,boolean request,int stance,int move,int replacement)throws IOException {
        // Resume recomputes participant getters from saved model-local injury;
        // caller facts may have been rebound from campaign injury after load.
        for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++){var a=actors[side][slot];if(a.valid&&a.warByInjury!=null)a.war=a.warByInjury[state.model.get(state.model.fighter(side,slot)+12)];}
        int result;if(phase()==1)result=state.model.frameOpening(actors,relations,state.random,settings);
        else if(phase()==12)result=state.model.frameTerminal(actors,relations,state.random,settings.deathValid,settings.rawDeathOption,state.manager);
        else {
            // Original support getters are consumed only by phase3/sub3.
            // Other numerical frames must not reparse670 source persons and
            // saved raw-loyalty policy merely to discard those facts.
            var supportFacts=phase()==3&&sub()==3?support.current(state.model):null;
            try{result=state.model.frameHuman(actors,supportFacts,state.random,settings,new boolean[]{request,false},new int[]{stance,-1},move,replacement);}
            catch(UncheckedIOException e){throw e.getCause();}
        }
        frames++;return result;
    }
    void submit(PcDuelKernel.Actor[][]actors,SupportBinding support,boolean[][]relations,int stance,int special,int replacement,boolean retreatRestriction)throws IOException {
        String error=inputError(stance,special,replacement,retreatRestriction);if(error!=null)throw new IOException(error);
        // Rejection leaves every byte and RNG intact. Accepted frame generation
        // runs on a complete saved copy and installs only at a stable boundary.
        var bytes=new ByteArrayOutputStream();write(new DataOutputStream(bytes));PcDuelCampaign candidate=read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        // Original509fa0/sub2 consumes confirmed UI5116f0 and clears pending1
        // before applying the human pose. Android command acceptance supplies
        // that confirmation without storing a PC view pointer. It also resumes
        // an earlier command-boundary save whose confirmed flag was retained.
        // Read/preview/rejection and original NULL-view fixture drivers are pure.
        if(candidate.commandBoundary&&candidate.state.model.get(0x14)==1)
            candidate.state.model.acknowledgeHumanPose();
        boolean request=phase()==3&&special>=0;
        for(int count=0;count<3000;count++){
            if(count>0&&candidate.waitingInput())break;
            if(candidate.frame(actors,support,relations,request,stance,special,replacement)==1)break;
            if(count==2999)throw new IOException("原单挑未到达下一输入边界");
        }
        if(!candidate.waitingInput()&&!candidate.terminal())throw new IOException("原单挑输入没有稳定结果");
        candidate.inputs=Math.addExact(inputs,1);state=candidate.state;frames=candidate.frames;inputs=candidate.inputs;
    }
    void advance(PcDuelKernel.Actor[][]actors,SupportBinding support,boolean[][]relations,boolean request,int stance,int move)throws IOException {
        if(terminal())throw new IOException("原单挑已到终局");
        for(int count=0;count<3000;count++){
            if(count>0&&waitingInput())return;
            if(frame(actors,support,relations,request,stance,move)==1)return;
        }
        throw new IOException("原单挑未到达下一输入边界");
    }
    void write(DataOutputStream out)throws IOException {
        if(commandBoundary&&(!settings.separateDeath||openingVoice!= -1&&openingVoice!=35&&openingVoice!=36)||!commandBoundary&&(opponentActed||openingVoice!=-1))throw new IOException("原单挑命令保存策略不一致");
        byte[]model=PcDuelModelSave.write(state);boolean admission=disposition!=null&&disposition.rows.stream().anyMatch(r->r.recruitmentAdmitted);int fate=disposition==null?0:admission?3:2;
        if(heirProtocol&&!commandBoundary||naturalHeir&&!heirProtocol||!heirProtocol&&(heirRuler!=-1||chosenHeir!=-1))throw new IOException("原继承选择保存策略无效");
        out.writeInt(heirProtocol?(naturalHeir?7:6):commandBoundary?5:settings.separateDeath?4:disposition==null?1:admission?3:2);out.writeBoolean(settings.lifeValid);out.writeInt(settings.rawLifeOption);out.writeBoolean(settings.difficultyValid);out.writeInt(settings.rawDifficulty);
        if(settings.separateDeath){out.writeBoolean(settings.deathValid);out.writeInt(settings.rawDeathOption);}
        if(commandBoundary){out.writeBoolean(opponentActed);out.writeInt(openingVoice);}
        out.writeInt(frames);out.writeInt(inputs);out.writeInt(model.length);out.write(model);if(settings.separateDeath)out.writeInt(fate);if(disposition!=null)disposition.write(out,admission);
        if(heirProtocol){out.writeInt(heirRuler);out.writeInt(chosenHeir);}
    }
    static PcDuelCampaign read(DataInputStream in)throws IOException {
        int format=in.readInt();if(format<1||format>7)throw new IOException("原单挑战役保存策略未知");boolean lifeValid=in.readBoolean();int life=in.readInt();boolean difficultyValid=in.readBoolean();int difficulty=in.readInt();
        var settings=format>=4?new PcDuelKernel.OriginalSettings(lifeValid,life,difficultyValid,difficulty,in.readBoolean(),in.readInt()):new PcDuelKernel.OriginalSettings(lifeValid,life,difficultyValid,difficulty);
        if(settings.rawLifeOption< -1||settings.rawLifeOption>3||settings.rawDifficulty< -1||settings.rawDifficulty>2||format>=4&&(settings.rawDeathOption< -1||settings.rawDeathOption>2))throw new IOException("原单挑设置范围未知");
        boolean opponentActed=false;int voice=-1;if(format>=5){int flag=in.readUnsignedByte();if(flag>1)throw new IOException("原应战部队行动标志无效");opponentActed=flag==1;voice=in.readInt();if(voice!= -1&&voice!=35&&voice!=36)throw new IOException("原开战发言编号无效");}
        int frames=in.readInt(),inputs=in.readInt(),n=in.readInt();if(n<128||n>4096||frames<0||frames>1000000||inputs<0||inputs>10000)throw new IOException("原单挑战役保存范围无效");byte[]b=new byte[n];in.readFully(b);var c=new PcDuelCampaign(PcDuelModelSave.read(b),settings);c.frames=frames;c.inputs=inputs;c.commandBoundary=format>=5;c.opponentActed=opponentActed;c.openingVoice=voice;int fate=format>=4?in.readInt():format==1?0:format;
        if(fate!=0&&fate!=2&&fate!=3)throw new IOException("原单挑处置保存格式无效");if(fate>=2)c.disposition=PcDuelDisposition.read(in,fate==3);if(fate==3&&c.disposition.rows.stream().noneMatch(r->r.recruitmentAdmitted))throw new IOException("原登用准入格式缺少成功记录");if(format>=6){c.heirProtocol=true;c.naturalHeir=format==7;c.heirRuler=in.readInt();c.chosenHeir=in.readInt();if(c.heirRuler<0||c.chosenHeir< -1)throw new IOException("原继承待办保存范围无效");}return c;
    }
    /** One original natural casualty, independent of UI/controller defaults. */
    int naturalDeathOfficer()throws IOException {
        int index=-1,count=0;for(int i=0;i<6;i++){int outcome=PcDuelKernel.readManager(state.manager,0x64+4*i);if(outcome!=0)count++;if(outcome==2)index=i;}
        if(index<0)return -1;int winner=PcDuelKernel.readManager(state.manager,0x54),loser=PcDuelKernel.readManager(state.manager,0x58),active=PcDuelKernel.readManager(state.manager,0x60);
        if(count!=1||winner<0||winner>1||loser!=1-winner||index/3!=loser||index%3!=active||state.officers[index]<0)throw new IOException("原单挑此俘虏/死亡回调尚未闭合");return state.officers[index];
    }
    void validate(World w,Contests.Session session)throws IOException {
        PcDuelModelSave.validate(state);
        var configured=PcDuelCampaignPolicy.options(w);if(configured!=null&&(!commandBoundary||!settings.separateDeath||!settings.lifeValid||!settings.deathValid||!settings.difficultyValid||settings.rawLifeOption!=configured.life||settings.rawDeathOption!=configured.death||settings.rawDifficulty!=configured.difficulty))throw new IOException("原单挑对局与已保存新局设置不同");
        if(heirProtocol){
            var ruler=w.officer(heirRuler);boolean pending=naturalHeir?disposition==null&&naturalDeathOfficer()==heirRuler:disposition!=null&&disposition.rows.stream().anyMatch(r->r.officerId==heirRuler&&r.choice==PcDuelDisposition.EXECUTE);if(!commandBoundary||PcDuelCampaignPolicy.read(w).version!=3||!terminal()||ruler==null||ruler.owner!=w.player||ruler.role!=Strategy.Role.RULER||!w.life.present(heirRuler)||!pending)throw new IOException("原玩家继承待办与当前终局不同");
            var candidates=PcRulerSuccession.preview(w,heirRuler);if(candidates.candidates.size()<2||chosenHeir>=0&&!candidates.candidates.contains(chosenHeir))throw new IOException("原继承待选人物已不在当前候选中");
        }else if(naturalHeir||heirRuler!=-1||chosenHeir!=-1)throw new IOException("原继承未启用但含待办");
        if(disposition!=null){
            disposition.validate();if(!terminal())throw new IOException("未结束单挑不能有终局处置");
            int loser=PcDuelKernel.readManager(state.manager,0x58),active=PcDuelKernel.readManager(state.manager,0x60);
            Set<Integer>captured=new HashSet<>();for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++)if(PcDuelKernel.readManager(state.manager,0x64+12*side+4*slot)==1){
                if(side!=loser||slot!=active||state.officers[side*3+slot]<0)throw new IOException("终局处置必须对应原败方上阵人物");
                captured.add(state.officers[side*3+slot]);
            }
            if(captured.size()!=disposition.rows.size())throw new IOException("终局处置与原捕获候选不同");
            for(var row:disposition.rows){if(!captured.contains(row.officerId))throw new IOException("终局处置人物不在原候选中");int index=-1;for(int i=0;i<6;i++)if(state.officers[i]==row.officerId)index=i;if(index<0||state.natives[index]!=row.nativeId)throw new IOException("终局处置人物稳定连接不同");}
        }
        if(!PcDuelCampaignPolicy.enabled(w)||PcDuelCampaignPolicy.lastFinished(w)>=session.id||!PcDuelHealthPolicy.enabled(w)||!PcNativeDebatePolicy.enabled(w)||state.random.state!=PcNativeDebatePolicy.seed(w)||inputs!=session.revision||session.city!=-1||frames<0||frames>1000000||inputs<0||inputs>10000)throw new IOException("原单挑战役策略或序号不一致");
        World.Unit left=w.unit(session.leftRef),right=w.unit(session.rightRef);
        if(left==null||right==null||left.owner!=session.owner||!w.campaign.hostile(left.owner,right.owner)||left.hex.distance(right.hex)!=1||!left.acted||(commandBoundary?right.acted!=opponentActed:!right.acted))throw new IOException("原单挑当前部队引用无效");
        if(left.status!=War.Status.NORMAL||right.status!=War.Status.NORMAL||w.army.water(left.hex)||w.army.water(right.hex)||Army.siegeWeapon(left.weapon)||Army.siegeWeapon(right.weapon))throw new IOException("原单挑战役场地无效");
        var facts=PcDuelSourceFacts.saved(w);
        for(int side=0;side<2;side++){
            World.Unit unit=side==0?left:right;var crew=PcDuelEntryRules.crew(w,unit,state.officers[side*3]);if(crew.size()!=state.model.get(0x24+side*0xec+0xc0)||PcDuelKernel.readManager(state.manager,0x18+4*side)!=unit.id)throw new IOException("原单挑编队不同");
            for(int slot=0;slot<crew.size();slot++){int id=state.officers[side*3+slot];var f=facts.get(id);if(crew.get(slot)!=id||f==null||f.nativeId!=state.natives[side*3+slot]||!w.life.present(id))throw new IOException("原单挑人物稳定连接不同");}
        }
        if(terminal()){
            int winner=state.model.get(0x588),loser=state.model.get(0x58c);if(state.model.get(0x1c)!=0){winner=winner>=0?1-winner:-1;loser=loser>=0?1-loser:-1;}
            if(PcDuelKernel.readManager(state.manager,0x54)!=winner||PcDuelKernel.readManager(state.manager,0x58)!=loser)throw new IOException("原单挑终局胜败与模型不同");
            for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++)if(state.officers[side*3+slot]>=0){int at=state.model.fighter(side,slot);if(PcDuelKernel.readManager(state.manager,0x7c+12*side+4*slot)!=state.model.get(at+4)||PcDuelKernel.readManager(state.manager,0xac+12*side+4*slot)!=state.model.get(at+12))throw new IOException("原单挑终局体力伤病与模型不同");}
        }
        if(!waitingInput()&&!terminal())throw new IOException("原单挑保存未到稳定输入边界");
    }
}
