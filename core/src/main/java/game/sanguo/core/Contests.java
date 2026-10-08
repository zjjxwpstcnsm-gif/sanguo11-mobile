package game.sanguo.core;

import java.util.*;

/** Owns the single in-progress contest and commits its outcome to the campaign exactly once. */
public final class Contests {
    public enum Gear { HORSE, SWORD, POLEARM, BOW, HIDDEN, BOOK }
    public static final class Profile {
        public final Debate.Temper temper;
        public final int talkMask,gearMask;
        public Profile(Debate.Temper temper,int talkMask,int gearMask){
            if(temper==null||talkMask<0||talkMask>31||gearMask<0||gearMask>63)throw new IllegalArgumentException("论辩/携物配置无效");
            this.temper=temper;this.talkMask=talkMask;this.gearMask=gearMask;
        }
        public boolean has(Gear gear){return (gearMask&(1<<gear.ordinal()))!=0;}
    }
    static final class Injury {
        final int severity,until;
        Injury(int severity,int until){this.severity=severity;this.until=until;}
    }
    public static final class Session {
        final int id,owner,turn,leftRef,rightRef,city;
        int revision;
        Duel duel;Debate debate;PcDebateCampaign nativeDebate;PcDuelCampaign nativeDuel;boolean searchChoice;
        Campaign.TreatyKind treaty;int foreign=-1,duration;
        Session(int id,int owner,int turn,int left,int right,int city){this.id=id;this.owner=owner;this.turn=turn;leftRef=left;rightRef=right;this.city=city;}
        public int id(){return id;} public int revision(){return revision;}
        public Duel duel(){return duel;} public Debate debate(){return debate;}
        public PcDebateCampaign.Facts nativeDebate(){return nativeDebate==null?null:nativeDebate.facts();}
        public PcDuelCampaign.Facts nativeDuel(){return nativeDuel==null?null:nativeDuel.facts();}
        public boolean isDuel(){return duel!=null||nativeDuel!=null;}
        public boolean searchChoice(){return searchChoice;}
        public boolean diplomatic(){return treaty!=null;}
        public String purpose(){return isDuel()?"单挑":treaty==null?"登用":"外交 · "+treaty.label+" "+duration+"旬";}
    }
    private final World w;
    final SortedMap<Integer,Profile> profiles=new TreeMap<>();
    final SortedMap<Integer,Injury> injuries=new TreeMap<>();
    private static final Profile DEFAULT=new Profile(Debate.Temper.CALM,0,0);
    Session session;
    int nextId=1;
    String lastResult="";
    Contests(World w){this.w=w;}
    public boolean busy(){return session!=null;}
    public boolean nativeCampaignSettlementEnabled(){return PcDebateCampaignPolicy.enabled(w);}
    public Session current(){return session;}
    public int searchChoicePhase(){try{return session!=null&&session.searchChoice?PcSearchPolicy.read(w).phase:0;}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    public boolean searchOrigin(){return PcSearchPolicy.owns(w,session);}
    public int searchActorId(){return session==null?-1:session.leftRef;}
    public int searchTargetId(){return session==null?-1:session.rightRef;}
    public World.Result searchChoice(int id,int revision,boolean yes){w.reports.prepare();String error=currentError(id,revision,false);if(error!=null)return w.fail(error);if(!session.searchChoice)return w.fail("当前不是搜索确认阶段");try{return PcSearchPolicy.choose(w,yes);}catch(java.io.IOException e){return w.fail(e.getMessage());}}
    public String lastResult(){return lastResult;}
    public Profile profile(int officer){Profile p=profiles.getOrDefault(officer,DEFAULT);int gear=p.gearMask|w.treasures.gearMask(officer);return gear==p.gearMask?p:new Profile(p.temper,p.talkMask,gear);}
    public boolean hasProfile(int officer){return profiles.containsKey(officer);}
    /** Scenario setup only: do not infer canonical traits from name, gender, intelligence or affiliation. */
    public void configure(int officer,Profile profile){
        if(busy()||w.officer(officer)==null||profile==null)throw new IllegalArgumentException("配置武将无效或对局进行中");
        profiles.put(officer,profile);
    }
    public int injury(int officer){Injury injury=injuries.get(officer);int legacy=injury==null||injury.until<=w.turn?0:injury.severity;return Math.max(legacy,PcNativeHealthPolicy.injury(w,officer));}
    public int war(World.Officer o){return o==null?0:o.abilityProfile!=null?o.war:Math.max(0,o.war-injury(o.id)*10);}
    public int injuryTurns(int officer){Injury injury=injuries.get(officer);int legacy=injury==null?0:Math.max(0,injury.until-w.turn),nativeTurns=PcNativeHealthPolicy.remainingTurns(w,officer);return nativeTurns<0?-1:Math.max(legacy,nativeTurns);}
    void tick(){injuries.entrySet().removeIf(e->e.getValue().until<=w.turn);PcNativeHealthPolicy.tick(w);try{PcDuelPhysicalRecoveryPolicy.tick(w);}catch(java.io.IOException e){throw new IllegalStateException(e);}w.officerAbilities.refresh();}
    public boolean nativeDuelConfigured(){try{return PcDuelCampaignPolicy.options(w)!=null;}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    public static final class DuelCandidate {public final int officerId,chance;public final String name;DuelCandidate(World w,PcDuelResponseRules.CandidateChance p){officerId=p.officerId;chance=p.chance;name=w.officer(officerId).name;}}
    public List<DuelCandidate> nativeDuelCandidates(int actor,int target){try{return PcDuelChallenge.menu(w,actor,target).stream().map(p->new DuelCandidate(w,p)).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),values->{values.forEach(Objects::requireNonNull);return Collections.unmodifiableList(values);}));}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    public World.Result nativeDuelChallenge(int actor,int target,int nominee){w.reports.prepare();try{return PcDuelChallenge.start(w,actor,target,nominee);}catch(java.io.IOException e){return w.fail(e.getMessage());}}
    public String duelError(int actor,int target){
        if(nativeDuelConfigured()){var actorUnit=w.unit(actor);try{return PcDuelChallenge.error(w,actor,target,actorUnit==null?-1:actorUnit.officerId);}catch(java.io.IOException e){return e.getMessage();}}
        World.Unit a=w.unit(actor),b=w.unit(target);String error=w.orders.combatError(a);if(error!=null)return error;
        if(w.active!=w.player)return "仅当前玩家可发起交互单挑";
        if(nextId>=10000000)return "对局编号已达上限";
        if(b==null||!w.campaign.hostile(a.owner,b.owner)||a.hex.distance(b.hex)!=1)return "请选择相邻交战部队";
        if(b.status!=War.Status.NORMAL)return "对方处于异常状态，无法应战";
        if(w.army.water(a.hex)||w.army.water(b.hex)||Army.siegeWeapon(a.weapon)||Army.siegeWeapon(b.weapon))return "需双方均为陆上非器械部队";
        if(a.energy<10)return "单挑需要10气力";
        return null;
    }
    public int acceptance(int actor,int target){
        World.Unit a=w.unit(actor),b=w.unit(target);if(a==null||b==null)return 0;
        if(nativeDuelConfigured()){try{return PcDuelChallenge.menu(w,actor,target).stream().filter(p->p.officerId==a.officerId).mapToInt(p->p.chance).findFirst().orElse(0);}catch(java.io.IOException e){return 0;}}
        return Math.max(15,Math.min(90,60+(war(w.officer(b.officerId))-war(w.officer(a.officerId)))/2));
    }
    private World.Officer cavalryOfficer(World.Unit unit){
        return w.army.crew(unit).stream().filter(o->profile(o.id).temper!=Debate.Temper.TIMID)
            .max(Comparator.comparingInt((World.Officer o)->war(o)).thenComparingInt(o->-o.id)).orElse(null);
    }
    private double cavalryScore(World.Officer officer){
        int health=Math.max(40,100-injury(officer.id)*20),strength=war(officer);
        // Community-documented base. Persistent out-of-duel stamina and exact
        // individual treasure modifiers are unavailable: use existing injury HP
        // and no speculative treasure bonus. See feedback-v125 rule research.
        return (health+200)*strength*strength*.0000025
            -(officer.role==Strategy.Role.RULER||strength>95?0:1);
    }
    public int cavalryChance(World.Unit a,World.Unit b){
        if(busy()||w.gameOver()||a==null||b==null||w.unit(a.id)!=a||w.unit(b.id)!=b
            ||a.weapon!=World.Weapon.CAVALRY||b instanceof Domestic.Mission||Army.siegeWeapon(b.weapon)
            ||a.status!=War.Status.NORMAL||b.status!=War.Status.NORMAL||a.hex.distance(b.hex)!=1
            ||w.army.water(a.hex)||w.army.water(b.hex)||!w.campaign.hostile(a.owner,b.owner)
            ||nextId>=10000000||(long)a.troops>2L*b.troops||a.troops-b.troops>2500)return 0;
        World.Officer officer=cavalryOfficer(a);if(officer==null)return 0;
        Debate.Temper temper=profile(officer.id).temper;int hp=Math.max(40,100-injury(officer.id)*20);
        if(hp<=(temper==Debate.Temper.CALM?70:temper==Debate.Temper.BOLD?60:50))return 0;
        double left=0,right=0;for(World.Officer o:w.army.crew(a))left+=cavalryScore(o)*20;
        for(World.Officer o:w.army.crew(b))right+=cavalryScore(o)*20;
        if(left-right>30)return 0;
        return Math.max(0,Math.min(100,(int)Math.floor(cavalryScore(officer)+(temper==Debate.Temper.BOLD?1:temper==Debate.Temper.RASH?3:0))));
    }
    boolean cavalry(World.Unit a,World.Unit b){
        int chance=cavalryChance(a,b);if(chance==0||w.strategy.nextInt(100)>=chance)return false;
        World.Officer opener=cavalryOfficer(a);
        b.acted=true;session=new Session(nextId++,w.active,w.turn,a.id,b.id,-1);session.duel=new Duel(w,a,b);
        for(int i=0;i<session.duel.left.size();i++)if(session.duel.left.get(i).officer==opener.id){
            session.duel.leftIndex=i;session.duel.left.get(i).joined=true;break;
        }
        w.battleOutcome(opener.name+"发动强制单挑，伤害已结算，不额外消耗气力");
        // AI turns cannot leave an interactive session blocking the turn worker.
        // Resolve with the existing finite duel and settlement, never another combat rule.
        if(a.owner!=w.player){
            Duel duel=session.duel;
            while(duel.winner==-2){duel.step(w,Duel.Stance.ATTACK,Duel.Move.EXCHANGE,-1);session.revision++;}
            finishDuel();
        }
        return true;
    }
    public World.Result challenge(int actor,int target){w.reports.prepare();
        if(nativeDuelConfigured()){var a=w.unit(actor);return nativeDuelChallenge(actor,target,a==null?-1:a.officerId);}
        String error=duelError(actor,target);if(error!=null)return w.fail(error);
        World.Unit a=w.unit(actor),b=w.unit(target);int chance=acceptance(actor,target);
        w.energy.change(a,-10,EnergyRules.Reason.COMMAND);a.acted=true;
        if(w.strategy.nextInt(100)>=chance){lastResult=w.officer(b.officerId).name+"拒绝单挑，挑战方本旬行动与10气力已消耗";return w.success(lastResult);}
        b.acted=true;session=new Session(nextId++,w.active,w.turn,actor,target,-1);session.duel=new Duel(w,a,b);
        return w.success(w.officer(a.officerId).name+"与"+w.officer(b.officerId).name+"开始单挑");
    }
    public String debateError(int city, int actor, int target) {
        World.City c = this.w.city(city);
        String error = this.w.cityError(c, this.w.officer(actor), 100);
        if (error != null) {
            return error;
        }
        if (this.w.active != this.w.player) {
            return "仅当前玩家可发起交互舌战";
        }
        if (this.nextId >= 10000000) {
            return "对局编号已达上限";
        }
        if (this.w.relations.refuses(target, actor, this.w.active) || !this.w.strategy.canRecruitTarget(city, target) || this.w.officer(target).cityId != city || this.w.officer(target).acted) {
            return "目标须为本城未行动的在野武将或符合登用条件的敌将";
        }
        return null;
    }
    public World.Result persuade(int city,int actor,int target){w.reports.prepare();
        String error=debateError(city,actor,target);if(error!=null)return w.fail(error);
        if(PcNativeDebatePolicy.enabled(w)){String nativeError=PcDebateCampaign.inputError(w,actor,target);if(nativeError!=null)return w.fail(nativeError);}
        w.spend(w.city(city),w.officer(actor),100);w.officer(target).acted=true;
        session=new Session(nextId++,w.active,w.turn,actor,target,city);
        if(PcNativeDebatePolicy.enabled(w)){try{session.nativeDebate=new PcDebateCampaign(w,actor,target);}catch(java.io.IOException e){throw new IllegalStateException(e);}}else session.debate=new Debate(w,actor,target);
        return w.success(w.officer(actor).name+"以舌战说服"+w.officer(target).name+"；金100、行动力10已消耗"+(PcDebateCampaignPolicy.enabled(w)?"（工程准入/费用；原完整启动链待核实）":""));
    }
    World.Officer foreignSpeaker(int side){
        return w.officers.stream().filter(o->o.owner==side&&o.cityId>=0&&o.unitId==-1&&!o.acted&&!w.government.captive(o.id)&&!w.domestic.busy(o.id)&&!w.strategy.busy(o.id))
            .min(Comparator.comparingInt((World.Officer o)->-o.intelligence).thenComparingInt(o->o.id)).orElse(null);
    }
    World.Result diplomaticDebate(int city,int actor,int foreign,Campaign.TreatyKind treaty,int duration){
        World.Officer target=foreignSpeaker(foreign);
        if(target==null||nextId>=10000000)return w.success("出使被拒绝，对方没有可进行舌战的代表");
        target.acted=true;session=new Session(nextId++,w.active,w.turn,actor,target.id,city);
        session.treaty=treaty;session.foreign=foreign;session.duration=duration;session.debate=new Debate(w,actor,target.id);
        return w.success("论客发动：与"+target.name+"舌战议定"+duration+"旬"+treaty.label+"；出使费用已支付");
    }
    private String currentError(int id,int revision,boolean duel){
        if(session==null||session.id!=id||session.revision!=revision)return "对局已变化，请使用当前指令";
        if(session.owner!=w.active||session.turn!=w.turn||session.isDuel()!=duel)return "对局状态不匹配";
        return null;
    }
    public World.Result duelMove(int id,int revision,Duel.Stance stance,Duel.Move move,int replacement){w.reports.prepare();
        String error=currentError(id,revision,true);if(error!=null)return w.fail(error);
        if(session.nativeDuel!=null)return w.fail("原单挑操作绑定尚未启用");
        if(stance==null)return w.fail("请选择行动方针");
        error=session.duel.error(w,0,move,replacement);if(error!=null)return w.fail(error);
        session.duel.step(w,stance,move,replacement);session.revision++;
        if(session.duel.winner!=-2)return finishDuel();
        return w.success(session.duel.report);
    }
    public World.Result debateCard(int id,int revision,int index){w.reports.prepare();
        String error=currentError(id,revision,false);if(error!=null)return w.fail(error);
        if(session.searchChoice)return w.fail("请先确认是否招揽或舌战");
        if(session.nativeDebate!=null){error=session.nativeDebate.cardError(index);if(error!=null)return w.fail(error);session.nativeDebate.card(w,index);session.revision++;return w.success("已按原舌战规则出牌");}
        error=session.debate.error(index);if(error!=null)return w.fail(error);
        session.debate.play(w,index);session.revision++;
        return w.success(session.debate.report);
    }
    public World.Result rethink(int id,int revision){w.reports.prepare();
        String error=currentError(id,revision,false);if(error!=null)return w.fail(error);
        if(session.searchChoice)return w.fail("请先确认是否招揽或舌战");
        if(session.nativeDebate!=null)return debateCard(id,revision,0);
        Debate d=session.debate;if(d.winner!=-2||!d.left.canRethink())return w.fail("目前不能再考，心理台阶下降后恢复一次机会");
        // Even a calm fury gets only one rethink per exchange. Reset is keyed to rounds, not clicks.
        if(d.left.fury>0&&d.left.temper==Debate.Temper.CALM&&!d.left.rethink)return w.fail("本合已经再考");
        d.rethink(w);session.revision++;return w.success(d.report);
    }
    public World.Result finishDebate(int id,int revision,boolean mercy){w.reports.prepare();
        String error=currentError(id,revision,false);if(error!=null)return w.fail(error);
        if(session.searchChoice)return w.fail("请先确认是否招揽或舌战");
        if(session.nativeDebate!=null){
            if(session.nativeDebate.waitingMercy()){session.nativeDebate.mercy(w,mercy);session.revision++;if(!PcDebateCampaignPolicy.enabled(w))return w.success("终局选择已记录；原战役结算尚待核实");}
            if(!PcDebateCampaignPolicy.enabled(w))return w.fail("原战役结算尚未启用；旧档需明确采用新结算策略");
            return finishNativeDebate();
        }
        Debate d=session.debate;if(d.winner==-2)return w.fail("请先完成舌战");
        World.Officer actor=w.officer(session.leftRef),target=w.officer(session.rightRef);
        String text;
        if(session.diplomatic()){
            if(d.winner==0){w.campaign.concludeTreaty(session.owner,session.foreign,session.treaty,session.duration);w.government.earn(actor.id,200);text=actor.name+"舌战获胜，"+session.duration+"旬"+session.treaty.label+"成立";}
            else text="外交舌战"+(d.winner==1?"落败":"平手")+"，协定未成立，出使费用不返还";
            session=null;lastResult=text;return w.success(text);
        }
        if(d.winner==0){
            w.strategy.releaseGovernor(target.id);w.government.allegianceChanged(target.id);
            target.owner=session.owner;target.cityId=session.city;target.role=Strategy.Role.OFFICER;
            target.loyalty=70;target.lastRewardTurn=-1;target.acted=true;
            w.government.earn(actor.id,200);w.campaign.earn(session.owner,mercy?50:20,TechniquePointsJournal.Cause.DEBATE,-1,actor.id);
            boolean grew=!mercy&&OfficerAbilities.base(actor,2)<100&&w.strategy.nextInt(100)<20;if(grew)OfficerAbilities.setBase(actor,2,OfficerAbilities.base(actor,2)+1);
            text=actor.name+"舌战获胜，"+target.name+"加入"+w.faction(session.owner)+(mercy?"；留情，技巧+50":grew?"；智力+1，技巧+20":"；继续追问，技巧+20，智力未增长");
        }else text=d.winner==1?actor.name+"舌战落败，登用未成功":"舌战平手，登用未成功";
        session=null;lastResult=text;return w.success(text);
    }
    public World.Result adoptNativeDebateSettlement(int id,int revision){w.reports.prepare();
        String error=currentError(id,revision,false);if(error!=null)return w.fail(error);
        if(session.nativeDebate==null||PcDebateCampaignPolicy.enabled(w))return w.fail("没有可采用新策略的旧原舌战");
        try{PcDebateCampaignPolicy.initialize(w);}catch(java.io.IOException invalid){return w.fail(invalid.getMessage());}
        return w.success("已明确采用原数值与登用终局结算；原先手牌、模型和随机数保持；触发准入及费用仍为已有工程规则");
    }
    private World.Result finishNativeDebate(){
        PcDebateCampaign d=session.nativeDebate;
        if(d.model.phase!=9||d.model.state.terminalWinner<0)return w.fail("请先完成原舌战");
        if(session.diplomatic())return w.fail("原外交回调尚未闭合");
        int id=session.id;World.Officer actor=w.officer(session.leftRef),target=w.officer(session.rightRef);boolean success=d.model.state.terminalWinner==0;
        try{
            if(PcDebateCampaignPolicy.read(w).lastFinished>=id)return w.fail("原战役终局已经结算");
            PcDebateCampaignRules.terminalRewards(w,actor,target,d.model.state.terminalWinner,d.model.outcome);
            PcDebateCampaignRules.recruitmentResult(w,actor,target,session.city,success);
            PcSearchPolicy.finish(w,session);
            PcDebateCampaignPolicy.recordFinished(w,id);
        }catch(java.io.IOException failure){throw new IllegalStateException(failure);}
        String text=success?actor.name+"原舌战获胜，"+target.name+"接受登用；原经验、功绩、技巧与伤病已结算":actor.name+"原舌战落败；登用失败，原双方经验、功绩与伤病已结算";
        session=null;lastResult=text;return w.success(text);
    }
    /** Conceding is a paid outcome, not cancelling the already-started command. */
    public World.Result concede(int id,int revision){w.reports.prepare();
        if(session==null)return w.fail("没有正在进行的对局");
        String error=currentError(id,revision,session.isDuel());if(error!=null)return w.fail(error);
        if(session.searchChoice)return searchChoice(id,revision,false);
        if(session.nativeDuel!=null)return w.fail("原单挑认输回调尚未启用");
        if(session.isDuel()){
            // A voluntary surrender loses the current combatant; retreat remains a separate 100-spirit move.
            session.duel.winner=1;session.duel.active(0).hp=0;session.revision++;return finishDuel();
        }
        if(session.nativeDebate!=null)return w.fail("原认输处置与战役结算尚未核实");
        if(session.debate.winner!=-2)return w.fail("舌战已结束，请结算结果");
        session.debate.winner=1;session.debate.left.hp=0;session.revision++;
        return finishDebate(session.id,session.revision,false);
    }
    /** Typed native input; frozen bridge serialization does not admit new ordinals. */
    public World.Result nativeDuelInput(int id,int revision,int stance,int special,int replacement){w.reports.prepare();
        String error=currentError(id,revision,true);if(error!=null)return w.fail(error);
        if(session.nativeDuel==null)return w.fail("当前不是原单挑输入阶段");
        try{
            PcDuelCampaign duel=session.nativeDuel;duel.validate(w,session);
            if(duel.state.model.get(0x24+0xd8)!=0||duel.state.model.get(0x110+0xd8)!=1)return w.fail("当前人控与AI配置尚未核实");
            error=duel.inputError(stance,special,replacement,false);if(error!=null)return w.fail(error);
            var runtime=PcDuelRuntimeFacts.saved(w);var source=PcDuelSourceFacts.saved(w);
            PcDuelKernel.Actor[][]actors=new PcDuelKernel.Actor[2][3];
            for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++){
                int person=duel.state.officers[side*3+slot];
                actors[side][slot]=person>=0?PcDuelBindings.actor(w,person,duel.settings):new PcDuelKernel.Actor(-1,0,0,0,false,false);
            }
            boolean[][]relations=PcDuelKinship.terminal(w,duel.state.officers);
            PcDuelCampaign.SupportBinding binding=model->{
                PcDuelKernel.SupportFacts[][]facts=new PcDuelKernel.SupportFacts[2][3];
                for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++){
                    int person=duel.state.officers[side*3+slot];
                    if(person<0){facts[side][slot]=new PcDuelKernel.SupportFacts(new int[14]);continue;}
                    int own=duel.state.officers[side*3+model.activeIndex(side)],other=duel.state.officers[(1-side)*3+model.activeIndex(1-side)];
                    facts[side][slot]=PcDuelBindings.supportCurrent(w,person,own,other,runtime);
                }
                return facts;
            };
            duel.submit(actors,binding,relations,stance,special,replacement,false);
            PcNativeDebatePolicy.setSeed(w,duel.state.random.state);session.revision=duel.inputs;
            duel.validate(w,session);return w.success(duel.terminal()?"单挑已结束，请结算战役结果":"单挑已推进至下一操作阶段");
        }catch(java.io.IOException e){return w.fail(e.getMessage());}
    }
    /** Pure natural-casualty admission, shared with actual settlement.
     * Rendering cannot run the AI disposition or a native random draw. */
    public String nativeDuelNaturalDeathError(){
        if(session==null||session.nativeDuel==null||!session.nativeDuel.terminal())return null;
        var duel=session.nativeDuel;
        try{duel.validate(w,session);int dead=duel.naturalDeathOfficer();if(dead<0)return null;int winner=PcDuelKernel.readManager(duel.state.manager,0x54),loser=PcDuelKernel.readManager(duel.state.manager,0x58);var own=w.unit(winner==0?session.leftRef:session.rightRef);var other=w.unit(loser==0?session.leftRef:session.rightRef);int killer=duel.state.officers[winner*3+PcDuelKernel.readManager(duel.state.manager,0x5c)];var ruler=w.officer(dead);
            if(ruler.role==Strategy.Role.RULER&&ruler.owner==w.player){var candidates=PcRulerSuccession.preview(w,dead);if(candidates.candidates.size()>1){if(duel.naturalHeir&&duel.chosenHeir<0)return "请先选择本势力继承人";if(!duel.heirProtocol){String first=null;for(int heir:candidates.candidates){try{PcDuelDeath.validate(w,own,other,dead,killer,heir);return null;}catch(java.io.IOException e){if(first==null)first=e.getMessage();}}return first==null?"原自然阵亡继承没有可用候选":first;}}}
            PcDuelDeath.validate(w,own,other,dead,killer,duel.naturalHeir?duel.chosenHeir:-1);return null;
        }catch(java.io.IOException e){return e.getMessage();}
    }
    public static final class NativeAiActor {
        public final int officerId,nativeId;public final String name;public final boolean originalForceRuler;
        NativeAiActor(World.Officer actor,int nativeId,boolean original){officerId=actor.id;this.nativeId=nativeId;name=actor.name;originalForceRuler=original;}
    }
    public NativeAiActor nativeDuelAiActor()throws java.io.IOException{
        if(session==null||session.nativeDuel==null||!session.nativeDuel.terminal())return null;
        int side=PcDuelKernel.readManager(session.nativeDuel.state.manager,0x54);if(side<0)return null;
        var own=w.unit(side==0?session.leftRef:session.rightRef);if(own==null||own.owner==w.player)return null;
        var actor=PcDuelAiActorPolicy.actor(w,own);var source=PcDuelSourceFacts.saved(w).get(actor.id);if(source==null)throw new java.io.IOException("当前AI人物来源未绑定");return new NativeAiActor(actor,source.nativeId,PcDuelAiActorPolicy.enabled(w));
    }
    public boolean nativeDuelAiActorPolicyEnabled(){return PcDuelAiActorPolicy.enabled(w);}
    public String nativeDuelAiActorPolicyAdoptionError(){
        if(session==null||session.nativeDuel==null||!session.nativeDuel.terminal())return "请先到达原单挑终局";
        var duel=session.nativeDuel;if(duel.disposition!=null)return "AI已作出选择，保留当前处置与随机数";
        int side=PcDuelKernel.readManager(duel.state.manager,0x54);if(side<0)return "平局没有原自动处置";var own=w.unit(side==0?session.leftRef:session.rightRef);
        if(own==null||own.owner==w.player)return "当前终局由玩家选择处置";
        String error=PcDuelAiActorPolicy.adoptionError(w);if(error!=null)return error;
        try{PcDuelAiActorPolicy.ruler(w,own);}catch(java.io.IOException e){return e.getMessage();}return null;
    }
    public World.Result adoptNativeDuelAiActorPolicy(int id,int revision){w.reports.prepare();
        String error=currentError(id,revision,true);if(error!=null)return w.fail(error);error=nativeDuelAiActorPolicyAdoptionError();if(error!=null)return w.fail(error);
        try{PcDuelAiActorPolicy.adopt(w);session.nativeDuel.inputs++;session.revision++;session.nativeDuel.validate(w,session);}catch(java.io.IOException e){return w.fail(e.getMessage());}
        return w.success("已明确采用原AI势力君主判定；人物、部队驻点、原对局与随机数保留");
    }
    public NativeAiActor nativeDuelHumanActor()throws java.io.IOException{
        if(session==null||session.nativeDuel==null||!session.nativeDuel.terminal())return null;
        int side=PcDuelKernel.readManager(session.nativeDuel.state.manager,0x54);if(side<0)return null;var own=w.unit(side==0?session.leftRef:session.rightRef);if(own==null||own.owner!=w.player)return null;
        var actor=PcDuelHumanActorPolicy.actor(w,own);var source=PcDuelSourceFacts.saved(w).get(actor.id);if(source==null)throw new java.io.IOException("人工处分当前人物来源未绑定");return new NativeAiActor(actor,source.nativeId,PcDuelHumanActorPolicy.enabled(w));
    }
    public boolean nativeDuelHumanActorPolicyEnabled(){return PcDuelHumanActorPolicy.enabled(w);}
    public String nativeDuelHumanActorPolicyAdoptionError(){
        if(session==null||session.nativeDuel==null||!session.nativeDuel.terminal())return "请先到达原单挑终局";var duel=session.nativeDuel;
        int side=PcDuelKernel.readManager(duel.state.manager,0x54);if(side<0)return "平局没有人工处分";var own=w.unit(side==0?session.leftRef:session.rightRef);if(own==null||own.owner!=w.player)return "当前终局由AI判定";
        if(duel.disposition!=null)for(var r:duel.disposition.rows){var o=w.officer(r.officerId);boolean city=w.cities.stream().anyMatch(c->c.kind==World.SiteKind.CITY&&c.owner==o.owner);if(r.choice!=PcDuelDisposition.PENDING||r.recruitmentAdmitted||r.mask!=PcDuelDisposition.initialMask(o.role==Strategy.Role.RULER,city))return "已尝试登用或选择处置，保留当前结果与随机数";}
        String error=PcDuelHumanActorPolicy.adoptionError(w);if(error!=null)return error;try{PcDuelHumanActorPolicy.ruler(w,own);}catch(java.io.IOException e){return e.getMessage();}return null;
    }
    public World.Result adoptNativeDuelHumanActorPolicy(int id,int revision){w.reports.prepare();String error=currentError(id,revision,true);if(error!=null)return w.fail(error);error=nativeDuelHumanActorPolicyAdoptionError();if(error!=null)return w.fail(error);
        try{PcDuelHumanActorPolicy.adopt(w);session.nativeDuel.inputs++;session.revision++;session.nativeDuel.validate(w,session);}catch(java.io.IOException e){return w.fail(e.getMessage());}return w.success("已明确采用原人工处分君主判定；人物、原对局和随机数保留，已有尝试不重选");
    }
    public NativeAiActor nativeDuelRecruitItemRecipient()throws java.io.IOException{
        if(session==null||session.nativeDuel==null||!session.nativeDuel.terminal())return null;int side=PcDuelKernel.readManager(session.nativeDuel.state.manager,0x54);if(side<0)return null;var own=w.unit(side==0?session.leftRef:session.rightRef);if(own==null)return null;
        var o=PcDuelRecruitItemPolicy.recipient(w,own);var f=PcDuelSourceFacts.saved(w).get(o.id);if(f==null)throw new java.io.IOException("原登用宝物接收者来源未绑定");return new NativeAiActor(o,f.nativeId,PcDuelRecruitItemPolicy.enabled(w));
    }
    public boolean nativeDuelRecruitItemPolicyEnabled(){return PcDuelRecruitItemPolicy.enabled(w);}
    public String nativeDuelRecruitItemPolicyAdoptionError(){
        if(session==null||session.nativeDuel==null||!session.nativeDuel.terminal())return "请先到达原单挑终局";var duel=session.nativeDuel;
        if(duel.disposition!=null)for(var row:duel.disposition.rows){var o=w.officer(row.officerId);boolean city=w.cities.stream().anyMatch(c->c.kind==World.SiteKind.CITY&&c.owner==o.owner);if(row.choice!=PcDuelDisposition.PENDING||row.recruitmentAdmitted||row.mask!=PcDuelDisposition.initialMask(o.role==Strategy.Role.RULER,city))return "已有登用尝试或处分选择，保留现有结果";}
        String e=PcDuelRecruitItemPolicy.adoptionError(w);if(e!=null)return e;
        try{int side=PcDuelKernel.readManager(duel.state.manager,0x54);if(side<0)return "平局没有登用宝物接收者";PcDuelRecruitItemPolicy.ruler(w,w.unit(side==0?session.leftRef:session.rightRef));}catch(java.io.IOException error){return error.getMessage();}return null;
    }
    public World.Result adoptNativeDuelRecruitItemPolicy(int id,int revision){w.reports.prepare();String e=currentError(id,revision,true);if(e!=null)return w.fail(e);e=nativeDuelRecruitItemPolicyAdoptionError();if(e!=null)return w.fail(e);
        try{PcDuelRecruitItemPolicy.adopt(w);session.nativeDuel.inputs++;session.revision++;session.nativeDuel.validate(w,session);}catch(java.io.IOException error){return w.fail(error.getMessage());}return w.success("已明确采用原登用宝物归属；物品与人物不移动，后续合法登用才按原规则执行");
    }
    public int nativePhysicalHealth(int officer)throws java.io.IOException{return PcDuelHealthPolicy.enabled(w)?PcDuelHealthPolicy.health(w,officer):-1;}
    public boolean nativeDuelPhysicalRecoveryEnabled(){return PcDuelPhysicalRecoveryPolicy.enabled(w);}
    public String nativeDuelPhysicalRecoveryAdoptionError(){if(session==null||session.nativeDuel==null||!session.nativeDuel.terminal())return "请先到达原单挑终局";return PcDuelPhysicalRecoveryPolicy.adoptionError(w);}
    public World.Result adoptNativeDuelPhysicalRecovery(int id,int revision){w.reports.prepare();String e=currentError(id,revision,true);if(e!=null)return w.fail(e);e=nativeDuelPhysicalRecoveryAdoptionError();if(e!=null)return w.fail(e);try{PcDuelPhysicalRecoveryPolicy.adopt(w);session.nativeDuel.inputs++;session.revision++;session.nativeDuel.validate(w,session);}catch(java.io.IOException error){return w.fail(error.getMessage());}return w.success("已明确采用原逐旬体力恢复；当前人物体力与随机数保留，从后续完整旬开始恢复");}
    public boolean nativeDuelLoyaltyInputEnabled(){return PcLoyaltyProperty23Policy.enabled(w);}
    public String nativeDuelLoyaltyAdoptionError(){
        if(session==null||session.nativeDuel==null||!session.nativeDuel.terminal())return "请先到达原单挑终局";
        return PcLoyaltyProperty23Policy.adoptionError(w);
    }
    public World.Result adoptNativeDuelLoyaltyInput(int id,int revision){w.reports.prepare();
        String error=currentError(id,revision,true);if(error!=null)return w.fail(error);error=nativeDuelLoyaltyAdoptionError();if(error!=null)return w.fail(error);
        try{PcLoyaltyProperty23Policy.adopt(w);session.nativeDuel.inputs++;session.revision++;session.nativeDuel.validate(w,session);}
        catch(java.io.IOException e){return w.fail(e.getMessage());}
        return w.success("已明确采用忠诚唯一输入解析；原人物值和对局随机数保留，显示100仍保留未知");
    }
    public World.Result finishNativeDuel(int id,int revision){w.reports.prepare();
        String error=currentError(id,revision,true);if(error!=null)return w.fail(error);
        if(session.nativeDuel==null)return w.fail("当前不是原单挑终局");
        var nativeDuel=session.nativeDuel;
        try{nativeDuel.validate(w,session);}catch(java.io.IOException e){return w.fail(e.getMessage());}
        if(nativeDuel.terminal()&&nativeDuel.state.model.get(0x588)==0&&nativeDuel.state.model.get(0x24+0xd8)==0){
            boolean captured=false;for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++)captured|=PcDuelKernel.readManager(nativeDuel.state.manager,0x64+12*side+4*slot)==1;
            if(captured&&nativeDuel.disposition==null){try{nativeDuel.disposition=PcDuelDisposition.create(w,nativeDuel);nativeDuel.inputs++;session.revision++;return w.success("单挑已结束，请为捕获人物选择处置");}catch(java.io.IOException e){return w.fail(e.getMessage());}}
        }
        if(nativeDuel.disposition!=null){
            String confirmationError=nativeDuel.disposition.confirmationError();if(confirmationError!=null)return w.fail(confirmationError);
        }
        try{
            int naturalDead=nativeDuel.naturalDeathOfficer();
            if(naturalDead>=0&&!nativeDuel.heirProtocol){var ruler=w.officer(naturalDead);if(ruler.role==Strategy.Role.RULER&&ruler.owner==w.player){var candidates=PcRulerSuccession.preview(w,naturalDead);if(candidates.candidates.size()>1){String admission=nativeDuelNaturalDeathError();if(admission!=null)return w.fail(admission);nativeDuel.heirProtocol=true;nativeDuel.naturalHeir=true;nativeDuel.heirRuler=naturalDead;nativeDuel.chosenHeir=-1;nativeDuel.inputs++;session.revision++;nativeDuel.validate(w,session);return w.success("请选择阵亡君主的继承人，再确认单挑战役结算");}}}
            PcDuelSettlement.prepareDisposition(w,session);
            if(!nativeDuel.heirProtocol&&nativeDuel.disposition!=null)for(var row:nativeDuel.disposition.rows){var ruler=w.officer(row.officerId);
                if(row.choice==PcDuelDisposition.EXECUTE&&ruler!=null&&ruler.role==Strategy.Role.RULER&&ruler.owner==w.player){var candidates=PcRulerSuccession.preview(w,ruler.id);
                    if(candidates.candidates.size()>1){nativeDuel.heirProtocol=true;nativeDuel.heirRuler=ruler.id;nativeDuel.chosenHeir=-1;nativeDuel.inputs++;session.revision++;nativeDuel.validate(w,session);return w.success("请选择本势力继承人，再确认单挑战役结算");}
                }
            }
            if(nativeDuel.heirProtocol&&nativeDuel.chosenHeir<0)return w.fail("请先选择本势力继承人");
        }catch(java.io.IOException e){return w.fail(e.getMessage());}
        try{PcDuelSettlement.apply(w,session);}catch(java.io.IOException e){return w.fail(e.getMessage());}
        int winner=session.nativeDuel.state.model.get(0x588);String text=winner<0?"单挑平手，双方原经验和功绩已结算":"单挑结束，原经验、功绩、技巧、气力及兵损已结算";
        session=null;lastResult=text;w.checkVictory();return w.success(text);
    }
    public static final class NativeHeir {
        public final int officerId,nativeId;public final String name,error;
        NativeHeir(World.Officer o,int nativeId,String error){officerId=o.id;this.nativeId=nativeId;name=o.name;this.error=error==null?"":error;}
    }
    public String nativeDuelHeirError(int heir){
        if(session==null||session.nativeDuel==null||!session.nativeDuel.heirProtocol)return "当前没有原玩家继承待办";
        try{var duel=session.nativeDuel;duel.validate(w,session);if(duel.chosenHeir>=0)return "继承人已选择，请确认结算";int winner=PcDuelKernel.readManager(duel.state.manager,0x54),loser=PcDuelKernel.readManager(duel.state.manager,0x58);if(duel.naturalHeir)PcDuelDeath.validate(w,w.unit(winner==0?session.leftRef:session.rightRef),w.unit(loser==0?session.leftRef:session.rightRef),duel.heirRuler,duel.state.officers[winner*3+PcDuelKernel.readManager(duel.state.manager,0x5c)],heir);else PcDuelExecution.validate(w,w.unit(winner==0?session.leftRef:session.rightRef),w.unit(loser==0?session.leftRef:session.rightRef),duel.heirRuler,heir);return null;}catch(java.io.IOException e){return e.getMessage();}
    }
    public List<NativeHeir> nativeDuelHeirs(){
        if(session==null||session.nativeDuel==null||!session.nativeDuel.heirProtocol)return List.of();
        try{var duel=session.nativeDuel;var candidates=PcRulerSuccession.preview(w,duel.heirRuler);var source=PcDuelSourceFacts.saved(w);List<NativeHeir>result=new ArrayList<>();for(int id:candidates.candidates)result.add(new NativeHeir(w.officer(id),source.get(id).nativeId,nativeDuelHeirError(id)));return List.copyOf(result);}catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    public World.Result nativeDuelHeir(int id,int revision,int heir){w.reports.prepare();
        String error=currentError(id,revision,true);if(error!=null)return w.fail(error);error=nativeDuelHeirError(heir);if(error!=null)return w.fail(error);
        var duel=session.nativeDuel;duel.chosenHeir=heir;duel.inputs++;session.revision++;return w.success("继承人已选择，确认后结算战役");
    }
    /** Pure current callback admission shared by page preview and selection.
     * A pending choice must not become permanently unconfirmable after save. */
    private PcDuelRecruitmentAdmission.Plan nativeHumanRecruitmentPlan(PcDuelCampaign duel,World.Unit own,int officer)throws java.io.IOException{
        var target=w.officer(officer);boolean city=w.cities.stream().anyMatch(c->c.kind==World.SiteKind.CITY&&c.owner==target.owner);var actor=PcDuelHumanActorPolicy.actor(w,own);int actorInjury=w.contests.injury(actor.id);
        for(int i=0;i<6;i++)if(duel.state.officers[i]==actor.id)actorInjury=PcDuelKernel.readManager(duel.state.manager,0xac+4*i);
        int charm=w.officerAbilities.currentWithInjury(actor.id,4,actorInjury);return PcDuelRecruitmentAdmission.preview(w,officer,actor.id,city?1:2,charm);
    }
    public String nativeDuelDispositionError(int officer,int action){
        if(session==null||session.nativeDuel==null||!session.nativeDuel.terminal()||session.nativeDuel.disposition==null)return "当前没有原单挑终局处置";
        try{var duel=session.nativeDuel;duel.validate(w,session);String error=duel.disposition.error(officer,action);if(error!=null)return error;var row=duel.disposition.rows.stream().filter(r->r.officerId==officer).findFirst().orElseThrow();if(row.choice!=PcDuelDisposition.PENDING)return "该人物已完成处置选择，请确认终局";
            int winner=PcDuelKernel.readManager(duel.state.manager,0x54),loser=PcDuelKernel.readManager(duel.state.manager,0x58);var own=w.unit(winner==0?session.leftRef:session.rightRef);var other=w.unit(loser==0?session.leftRef:session.rightRef);
            switch(action){case PcDuelDisposition.RECRUIT:PcDuelRecruitment.validate(w,own,other,officer);nativeHumanRecruitmentPlan(duel,own,officer);break;case PcDuelDisposition.DETAIN:PcDuelCapture.validate(w,own,other,officer,duel.state.officers[winner*3+PcDuelKernel.readManager(duel.state.manager,0x5c)]);break;case PcDuelDisposition.RELEASE:PcDuelRelease.validateRelease(w,own,other,officer);break;case PcDuelDisposition.EXECUTE:PcDuelExecution.validate(w,own,other,officer);break;default:return "请选择有效处置";}return null;
        }catch(java.io.IOException e){return e.getMessage();}
    }
    public World.Result nativeDuelDisposition(int id,int revision,int officer,int action){w.reports.prepare();
        String error=currentError(id,revision,true);if(error!=null)return w.fail(error);
        if(session.nativeDuel==null||!session.nativeDuel.terminal()||session.nativeDuel.disposition==null)return w.fail("当前没有原单挑终局处置");
        String previewError=nativeDuelDispositionError(officer,action);if(previewError!=null)return w.fail(previewError);
        try{var duel=session.nativeDuel;duel.validate(w,session);var row=duel.disposition.rows.stream().filter(r->r.officerId==officer).findFirst().orElseThrow();
            if(action==PcDuelDisposition.RECRUIT){int winner=PcDuelKernel.readManager(duel.state.manager,0x54),loser=PcDuelKernel.readManager(duel.state.manager,0x58);var own=w.unit(winner==0?session.leftRef:session.rightRef);var other=w.unit(loser==0?session.leftRef:session.rightRef);PcDuelRecruitment.validate(w,own,other,officer);var target=w.officer(officer);var plan=nativeHumanRecruitmentPlan(duel,own,officer);var random=new PcDuelKernel.Random(duel.state.random.state);boolean success=plan.decision(random);duel.state.random.state=random.state;duel.state.random.draws+=random.draws;PcNativeDebatePolicy.setSeed(w,random.state);
                if(!success){duel.disposition.recruitmentFailed(officer);duel.inputs++;session.revision++;return w.success(target.name+"拒绝登用，请选择其他处置");}
            }
            duel.disposition.select(officer,action);if(action==PcDuelDisposition.RECRUIT)row.recruitmentAdmitted=true;duel.inputs++;session.revision++;return w.success("处置已选择，确认后才执行战役结算");}catch(java.io.IOException e){return w.fail(e.getMessage());}
    }
    private World.Result finishDuel(){
        Duel d=session.duel;
        for(int side=0;side<2;side++)for(Duel.Fighter f:d.team(side))if(f.wounds>0)
            injuries.put(f.officer,new Injury(Math.min(3,Math.max(injury(f.officer),f.wounds)),w.turn+3));
        w.officerAbilities.refresh();
        String text=d.report;
        if(d.winner>=0){
            int side=d.winner;World.Unit victor=w.unit(side==0?session.leftRef:session.rightRef),loser=w.unit(side==0?session.rightRef:session.leftRef);
            World.Officer beaten=w.officer(d.active(1-side).officer);
            w.government.earn(d.active(side).officer,200);w.campaign.earn(victor.owner,20,TechniquePointsJournal.Cause.DUEL,-1,victor.officerId);w.energy.change(victor,10,EnergyRules.Reason.DUEL);
            w.energy.change(loser,-20,EnergyRules.Reason.DUEL);
            if(d.escaped<0){
                boolean immune=w.skills.has(beaten,Skill.QIANGYUN)||w.skills.has(loser,Skill.XUELU)||profile(beaten.id).has(Gear.HORSE);
                if(loser.officerId==beaten.id){
                    for(World.Officer o:w.army.crew(loser))if(o.id!=beaten.id)w.retreat(o,loser.hex);
                    w.government.escortLost(loser,victor);w.units.remove(loser);PcGovernorPolicy.unitRemoved(w,loser.id);
                }else loser.deputies=Arrays.stream(loser.deputies).filter(x->x!=beaten.id).toArray();
                if(!immune){w.government.capture(beaten,victor);text+=" "+beaten.name+"被俘，随胜方部队押送。";}
                else {w.retreat(beaten,loser.hex);text+=" "+beaten.name+"撤回后方。";}
                w.treasures.fallenTreasury(loser.owner,victor.owner);
            }
            text+=" 单挑胜者："+w.officer(d.active(side).officer).name+"。";
        }
        session=null;lastResult=text;w.checkVictory();return w.success(text);
    }
}
