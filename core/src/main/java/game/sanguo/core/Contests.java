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
        Duel duel;Debate debate;
        Campaign.TreatyKind treaty;int foreign=-1,duration;
        Session(int id,int owner,int turn,int left,int right,int city){this.id=id;this.owner=owner;this.turn=turn;leftRef=left;rightRef=right;this.city=city;}
        public int id(){return id;} public int revision(){return revision;}
        public Duel duel(){return duel;} public Debate debate(){return debate;}
        public boolean isDuel(){return duel!=null;}
        public boolean diplomatic(){return treaty!=null;}
        public String purpose(){return treaty==null?"登用":"外交 · "+treaty.label+" "+duration+"旬";}
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
    public Session current(){return session;}
    public String lastResult(){return lastResult;}
    public Profile profile(int officer){Profile p=profiles.getOrDefault(officer,DEFAULT);int gear=p.gearMask|w.treasures.gearMask(officer);return gear==p.gearMask?p:new Profile(p.temper,p.talkMask,gear);}
    public boolean hasProfile(int officer){return profiles.containsKey(officer);}
    /** Scenario setup only: do not infer canonical traits from name, gender, intelligence or affiliation. */
    public void configure(int officer,Profile profile){
        if(busy()||w.officer(officer)==null||profile==null)throw new IllegalArgumentException("配置武将无效或对局进行中");
        profiles.put(officer,profile);
    }
    public int injury(int officer){Injury injury=injuries.get(officer);return injury==null||injury.until<=w.turn?0:injury.severity;}
    public int war(World.Officer o){return o==null?0:Math.max(0,o.war-injury(o.id)*10);}
    public int injuryTurns(int officer){Injury injury=injuries.get(officer);return injury==null?0:Math.max(0,injury.until-w.turn);}
    void tick(){injuries.entrySet().removeIf(e->e.getValue().until<=w.turn);}
    public String duelError(int actor,int target){
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
        return Math.max(15,Math.min(90,60+(war(w.officer(b.officerId))-war(w.officer(a.officerId)))/2));
    }
    public World.Result challenge(int actor,int target){
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
    public World.Result persuade(int city,int actor,int target){
        String error=debateError(city,actor,target);if(error!=null)return w.fail(error);
        w.spend(w.city(city),w.officer(actor),100);w.officer(target).acted=true;
        session=new Session(nextId++,w.active,w.turn,actor,target,city);session.debate=new Debate(w,actor,target);
        return w.success(w.officer(actor).name+"以舌战说服"+w.officer(target).name+"；金100、行动力10已消耗");
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
    public World.Result duelMove(int id,int revision,Duel.Stance stance,Duel.Move move,int replacement){
        String error=currentError(id,revision,true);if(error!=null)return w.fail(error);
        if(stance==null)return w.fail("请选择行动方针");
        error=session.duel.error(w,0,move,replacement);if(error!=null)return w.fail(error);
        session.duel.step(w,stance,move,replacement);session.revision++;
        if(session.duel.winner!=-2)return finishDuel();
        return w.success(session.duel.report);
    }
    public World.Result debateCard(int id,int revision,int index){
        String error=currentError(id,revision,false);if(error!=null)return w.fail(error);
        error=session.debate.error(index);if(error!=null)return w.fail(error);
        session.debate.play(w,index);session.revision++;
        return w.success(session.debate.report);
    }
    public World.Result rethink(int id,int revision){
        String error=currentError(id,revision,false);if(error!=null)return w.fail(error);
        Debate d=session.debate;if(d.winner!=-2||!d.left.canRethink())return w.fail("目前不能再考，心理台阶下降后恢复一次机会");
        // Even a calm fury gets only one rethink per exchange. Reset is keyed to rounds, not clicks.
        if(d.left.fury>0&&d.left.temper==Debate.Temper.CALM&&!d.left.rethink)return w.fail("本合已经再考");
        d.rethink(w);session.revision++;return w.success(d.report);
    }
    public World.Result finishDebate(int id,int revision,boolean mercy){
        String error=currentError(id,revision,false);if(error!=null)return w.fail(error);
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
            w.government.earn(actor.id,200);w.campaign.earn(session.owner,mercy?50:20);
            boolean grew=!mercy&&actor.intelligence<100&&w.strategy.nextInt(100)<20;if(grew)actor.intelligence++;
            text=actor.name+"舌战获胜，"+target.name+"加入"+w.faction(session.owner)+(mercy?"；留情，技巧+50":grew?"；智力+1，技巧+20":"；继续追问，技巧+20，智力未增长");
        }else text=d.winner==1?actor.name+"舌战落败，登用未成功":"舌战平手，登用未成功";
        session=null;lastResult=text;return w.success(text);
    }
    /** Conceding is a paid outcome, not cancelling the already-started command. */
    public World.Result concede(int id,int revision){
        if(session==null)return w.fail("没有正在进行的对局");
        String error=currentError(id,revision,session.isDuel());if(error!=null)return w.fail(error);
        if(session.isDuel()){
            // A voluntary surrender loses the current combatant; retreat remains a separate 100-spirit move.
            session.duel.winner=1;session.duel.active(0).hp=0;session.revision++;return finishDuel();
        }
        if(session.debate.winner!=-2)return w.fail("舌战已结束，请结算结果");
        session.debate.winner=1;session.debate.left.hp=0;session.revision++;
        return finishDebate(session.id,session.revision,false);
    }
    private World.Result finishDuel(){
        Duel d=session.duel;
        for(int side=0;side<2;side++)for(Duel.Fighter f:d.team(side))if(f.wounds>0)
            injuries.put(f.officer,new Injury(Math.min(3,Math.max(injury(f.officer),f.wounds)),w.turn+3));
        String text=d.report;
        if(d.winner>=0){
            int side=d.winner;World.Unit victor=w.unit(side==0?session.leftRef:session.rightRef),loser=w.unit(side==0?session.rightRef:session.leftRef);
            World.Officer beaten=w.officer(d.active(1-side).officer);
            w.government.earn(d.active(side).officer,200);w.campaign.earn(victor.owner,20);w.energy.change(victor,10,EnergyRules.Reason.DUEL);
            w.energy.change(loser,-20,EnergyRules.Reason.DUEL);
            if(d.escaped<0){
                boolean immune=w.skills.has(beaten,Skill.QIANGYUN)||w.skills.has(loser,Skill.XUELU)||profile(beaten.id).has(Gear.HORSE);
                if(loser.officerId==beaten.id){
                    for(World.Officer o:w.army.crew(loser))if(o.id!=beaten.id)w.retreat(o,loser.hex);
                    w.government.escortLost(loser,victor);w.units.remove(loser);
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
