package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Diplomacy outcomes share the campaign state. Probabilities and dispatch planning are engineering rules.
 * Official manual structure: 30 AP, exchange, whole-force surrender, allied expeditions lasting 18 turns. */
public final class Diplomacy {
    public static final int ACTION_POINTS=30, AID_TURNS=18;
    public static final class Aid {
        public final int requester,ally,source,target,targetOwner,expires;
        public int unit=-1;
        public boolean returning;
        public String status="等待盟军下一旬出征";
        Aid(int requester,int ally,int source,int target,int targetOwner,int expires){
            this.requester=requester;this.ally=ally;this.source=source;this.target=target;this.targetOwner=targetOwner;this.expires=expires;
        }
    }
    private final World w;
    final List<Aid> aids=new ArrayList<>();
    final SortedSet<String> attempts=new TreeSet<>();
    int attemptTurn=-1;
    Diplomacy(World w){this.w=w;}
    public List<Aid> aids(){return Collections.unmodifiableList(aids);}
    public Aid aidForUnit(int id){for(Aid a:aids)if(a.unit==id&&id>=0)return a;return null;}
    private boolean attempted(String kind,int owner,int target){return attemptTurn==w.turn&&attempts.contains(kind+":"+owner+":"+target);}
    private void attempt(String kind,int owner,int target){if(attemptTurn!=w.turn){attempts.clear();attemptTurn=w.turn;}attempts.add(kind+":"+owner+":"+target);}
    private String foreignError(int city,int actor,int side,int gold){
        String error=w.cityError(w.city(city),w.officer(actor),gold);if(error!=null)return error;
        if(w.actionPoints[w.active]<ACTION_POINTS)return "行动力不足30";
        if(side<0||side>=w.factions.length||side==w.active||!w.alive(side))return "请选择其他存活势力";
        return null;
    }
    private void spend(int city,int actor,int gold){w.spend(w.city(city),w.officer(actor),gold);w.actionPoints[w.active]-=ACTION_POINTS-10;}
    private int clamp(int chance){return Math.max(5,Math.min(95,chance));}
    private World.Officer ruler(int side){for(World.Officer o:w.officers)if(o.owner==side&&o.role==Strategy.Role.RULER&&w.life.present(o.id))return o;return null;}
    public long strength(int side){long result=0;for(World.City c:w.cities)if(c.owner==side)result+=c.troops+2L*c.defense;for(World.Unit u:w.units)if(u.owner==side)result+=u.troops;return result;}
    public int cityCount(int side){int n=0;for(World.City c:w.cities)if(c.owner==side&&c.kind==World.SiteKind.CITY)n++;return n;}

    public String surrenderError(int city,int actor,int side){
        String error=foreignError(city,actor,side,0);if(error!=null)return error;
        if(side==w.player)return "玩家势力不能被电脑自动劝降";
        if(attempted("surrender",w.active,side))return "本旬已向该势力劝降";
        if(ruler(side)==null||ruler(w.active)==null)return "双方须有在位君主";
        if(cityCount(side)==0||cityCount(w.active)<2*cityCount(side)||strength(w.active)<3*strength(side))return "劝降需要至少两倍城市、三倍军力优势";
        if(w.strategy.factionRelation(w.active,side)<20)return "劝降需要双方关系至少20";
        World.Officer target=ruler(side),own=ruler(w.active);
        if(w.relations.dislikes(target.id,actor)||w.relations.dislikes(target.id,own.id)||w.relations.dislikes(own.id,target.id))return "君主或使者存在厌恶关系，拒绝归顺";
        boolean near=false;for(World.City a:w.cities)if(a.owner==w.active)for(World.City b:w.cities)if(b.owner==side&&a.hex.distance(b.hex)<=12)near=true;
        return near?null:"双方据点相距过远，无法劝降";
    }
    public int surrenderChance(int actor,int side){
        World.Officer o=w.officer(actor);if(o==null||side<0||side>=w.factions.length||o.owner<0||side==o.owner)return 0;
        long ratio=Math.min(6,strength(o.owner)/Math.max(1,strength(side)));
        return clamp(5+o.charm/2+w.strategy.factionRelation(o.owner,side)/5+(int)ratio*5);
    }
    public World.Result surrender(int city,int actor,int side){
        String error=surrenderError(city,actor,side);if(error!=null)return w.fail(error);
        int chance=surrenderChance(actor,side),owner=w.active;spend(city,actor,0);attempt("surrender",owner,side);
        if(w.strategy.nextInt(100)>=chance){w.strategy.setFactionRelation(owner,side,Math.max(-100,w.strategy.factionRelation(owner,side)-10));return w.success(w.faction(side)+"拒绝劝降，行动力已消耗，关系下降10");}
        absorb(side,owner);return w.success(w.faction(side)+"接受劝降，全部据点、武将、部队和库存归入"+w.faction(owner));
    }

    public String exchangeError(int city,int actor,int wanted,int offered,int gold){
        Government.Prisoner target=w.government.prisoner(wanted);World.Officer own=w.officer(wanted);
        if(target==null||own==null||own.owner!=w.active||target.captor==w.active)return "请选择被其他势力俘虏的己方武将";
        if(gold<0||gold>10000||gold%100!=0)return "交换金须为0至10000的整百金";
        String error=foreignError(city,actor,target.captor,gold);if(error!=null)return error;
        if(target.captor==w.player)return "玩家持有的俘虏不能被自动交换";
        if(attempted("exchange",w.active,wanted))return "本旬已交涉此武将";
        if(offered>=0){Government.Prisoner gift=w.government.prisoner(offered);World.Officer o=w.officer(offered);
            if(gift==null||gift.captor!=w.active||gift.cityId!=city||gift.unitId>=0||o.owner!=target.captor)return "交换对象须为本城关押的对方武将";
        }else if(gold==0)return "请提供交换武将或金";
        World.City jail=w.city(target.cityId);World.Unit escort=w.unit(target.unitId);
        if(target.unitId>=0?(escort==null||escort.owner!=target.captor||escort.gold>10000-gold):(jail==null||jail.owner!=target.captor||jail.gold>w.campaign.goldCap(jail)-gold))return "对方金容量不足或关押位置无效";
        if(w.government.refuge(target.captor,w.city(city).hex)==null)return "对方没有可接收获释武将的据点";
        return null;
    }
    public int exchangeChance(int actor,int wanted,int offered,int gold){
        World.Officer o=w.officer(actor);Government.Prisoner p=w.government.prisoner(wanted);if(o==null||p==null)return 0;
        int value=gold+(offered<0?0:w.government.ransomCost(offered));
        return clamp(35+o.politics/3+w.strategy.factionRelation(o.owner,p.captor)/5+(value-w.government.ransomCost(wanted))/50);
    }
    public World.Result exchange(int city,int actor,int wanted,int offered,int gold){
        String error=exchangeError(city,actor,wanted,offered,gold);if(error!=null)return w.fail(error);
        Government.Prisoner target=w.government.prisoner(wanted);int side=target.captor,chance=exchangeChance(actor,wanted,offered,gold);
        spend(city,actor,0);attempt("exchange",w.active,wanted);
        if(w.strategy.nextInt(100)>=chance)return w.success("交换提议被拒绝，行动力已消耗；金与俘虏保留");
        w.city(city).gold-=gold;if(target.unitId>=0)w.unit(target.unitId).gold+=gold;else w.city(target.cityId).gold+=gold;
        w.government.free(target);if(offered>=0)w.government.free(w.government.prisoner(offered));
        w.strategy.setFactionRelation(w.active,side,Math.min(100,w.strategy.factionRelation(w.active,side)+5));
        return w.success("交换成功，"+w.officer(wanted).name+"获释；交付金"+gold+(offered>=0?"，释放"+w.officer(offered).name:""));
    }

    public String aidError(int city,int actor,int source,int target,int gold){
        World.City ally=w.city(source),enemy=w.city(target);
        if(ally==null)return "请选择盟军出兵据点";
        if(gold<0||gold>10000||gold%100!=0)return "援军礼金须为0至10000的整百金";
        String error=foreignError(city,actor,ally.owner,gold);if(error!=null)return error;
        if(ally.owner==w.player)return "玩家援军需要玩家自行出征";
        Campaign.Treaty t=w.campaign.treaty(w.active,ally.owner);if(t==null||t.kind!=Campaign.TreatyKind.ALLIANCE)return "援军请求需要有效同盟";
        if(enemy==null||enemy.owner<0||!w.campaign.hostile(w.active,enemy.owner)||!w.campaign.hostile(ally.owner,enemy.owner))return "攻略目标须为双方共同敌对据点";
        for(Aid a:aids)if(a.requester==w.active||a.ally==ally.owner)return "已有援军请求或援军尚未归城";
        if(attempted("aid",w.active,ally.owner))return "本旬已向该盟军请求援军";
        if(ally.gold>w.campaign.goldCap(ally)-gold)return "盟军据点金容量不足";
        return plannedAid(source,target)==null?"盟军暂无可出征兵粮、兵装、武将或可通行路线":null;
    }
    /** Predict the ally's next action on a copy. UI previews never reset the live ally or draw RNG. */
    public CampaignAi.Deployment plannedAid(int source,int target){
        try{
            World copy=SaveCodec.decode(SaveCodec.encode(w));World.City c=copy.city(source);if(c==null||c.owner<0)return null;
            copy.active=c.owner;copy.actionPoints[c.owner]=60;
            for(World.Officer o:copy.officers)if(o.owner==c.owner)o.acted=false;
            return new CampaignAi(copy).deployment(source,6000,t->t.id==target);
        }catch(IOException e){return null;}
    }
    public int aidChance(int actor,int ally,int gold){World.Officer o=w.officer(actor);return o==null?0:clamp(30+o.politics/3+w.strategy.factionRelation(o.owner,ally)/4+gold/200);}
    public World.Result requestAid(int city,int actor,int source,int target,int gold){
        String error=aidError(city,actor,source,target,gold);if(error!=null)return w.fail(error);
        World.City ally=w.city(source);int chance=aidChance(actor,ally.owner,gold);spend(city,actor,0);attempt("aid",w.active,ally.owner);
        if(w.strategy.nextInt(100)>=chance)return w.success("盟军拒绝援军请求，行动力已消耗，礼金保留");
        w.city(city).gold-=gold;ally.gold+=gold;aids.add(new Aid(w.active,ally.owner,source,target,w.city(target).owner,w.turn+AID_TURNS));
        return w.success(w.faction(ally.owner)+"接受援军请求，下一次行动从"+ally.name+"出征，攻略"+w.city(target).name+"；期限18旬");
    }
    public String describe(Aid a){World.Unit u=w.unit(a.unit);return w.faction(a.ally)+" · "+w.city(a.source).name+" → "+w.city(a.target).name+"\n"+a.status+
        (u==null?"":" · "+w.officer(u.officerId).name+"率"+u.troops+"兵")+"\n"+(a.returning?"返程中":"剩余"+Math.max(0,a.expires-w.turn)+"旬");}
    public World.Result cancelAid(int source){
        if(w.commandsBlocked()||w.gameOver())return w.fail("当前不能变更援军任务");
        for(Aid a:new ArrayList<>(aids))if(a.requester==w.active&&a.source==source&&!a.returning){end(a,"请求方结束援军任务");return w.success("援军任务结束，已出征部队返回盟军据点；礼金不退还");}
        return w.fail("没有可结束的本势力援军请求");
    }
    private void end(Aid a,String reason){a.returning=true;a.status=reason+"，返程";if(a.unit<0)aids.remove(a);w.note(w.faction(a.ally)+"："+reason);}
    void cleanup(){
        for(Aid a:new ArrayList<>(aids)){
            World.Unit u=w.unit(a.unit);if(a.unit>=0&&(u==null||u.owner!=a.ally)){aids.remove(a);w.note(w.faction(a.ally)+"援军归城、解编或覆灭，任务结束");continue;}
            if(a.returning)continue;
            Campaign.Treaty t=w.campaign.treaty(a.requester,a.ally);World.City source=w.city(a.source),target=w.city(a.target);
            if(!w.alive(a.requester)||!w.alive(a.ally))end(a,"参与势力已覆灭");
            else if(t==null||t.kind!=Campaign.TreatyKind.ALLIANCE)end(a,"同盟已结束");
            else if(w.turn>=a.expires)end(a,"18旬援军期限届满");
            else if(target.owner!=a.targetOwner)end(a,"攻略目标归属已改变");
            else if(!w.campaign.hostile(a.ally,target.owner)||!w.campaign.hostile(a.requester,target.owner))end(a,"与目标已停止交战");
            else if(a.unit<0&&source.owner!=a.ally)end(a,"出兵据点失守");
        }
    }
    void dispatch(){
        cleanup();CampaignAi ai=new CampaignAi(w);
        for(Aid a:new ArrayList<>(aids))if(a.ally==w.active&&!a.returning&&a.unit<0){
            int id=w.nextUnitId;
            if(ai.deploy(a.source,6000,c->c.id==a.target)){a.unit=id;a.status="已出征，向攻略目标推进";w.note(w.faction(a.ally)+"援军已从"+w.city(a.source).name+"出征");}
            else a.status="出兵条件变化，等待兵粮、武将或路线恢复";
        }
    }
    void tick(){cleanup();if(attemptTurn!=w.turn){attempts.clear();attemptTurn=w.turn;}}

    /** Whole-force peaceful transition. IDs, locations and cargo are preserved; no second turn is granted. */
    void absorb(int former,int owner){
        for(World.City c:w.cities)if(c.owner==former){c.owner=owner;c.governorId=-1;w.government.policies.remove(c.id);}
        for(World.Officer o:w.officers)if(o.owner==former){w.government.allegianceChanged(o.id);o.owner=owner;o.role=Strategy.Role.OFFICER;o.loyalty=80;o.acted=true;}
        for(int i=0;i<w.units.size();i++){World.Unit old=w.units.get(i);if(old.owner!=former)continue;
            World.Unit u=new World.Unit(old.id,owner,old.officerId,old.weapon,old.hex,old.troops,old.food);
            u.deputies=old.deputies.clone();u.ship=old.ship;u.gold=old.gold;u.energy=old.energy;u.acted=true;
            u.movementBudget=old.movementBudget;u.movementSpent=old.movementSpent;u.status=old.status;u.statusTurns=old.statusTurns;
            u.burning=old.burning;u.burningOwner=old.burningOwner==former?owner:old.burningOwner;u.burningPower=old.burningPower;
            w.units.set(i,u);
        }
        for(int i=0;i<w.war.structures.size();i++){War.Structure old=w.war.structures.get(i);if(old.owner!=former)continue;
            War.Structure s=new War.Structure(old.id,owner,old.kind,old.hex,old.hp);s.builder=old.builder;s.direction=old.direction;s.complete=old.complete;w.war.structures.set(i,s);
        }
        for(World.Unit u:w.units)if(u.burningOwner==former)u.burningOwner=owner;
        for(int i=0;i<w.war.fires.size();i++){War.Fire old=w.war.fires.get(i);if(old.owner!=former)continue;
            War.Fire fire=new War.Fire(old.hex,owner,old.remaining);fire.power=old.power;fire.trap=old.trap;w.war.fires.set(i,fire);
        }
        for(int i=0;i<w.domestic.missions.size();i++){Domestic.Mission m=w.domestic.missions.get(i);if(m.owner!=former)continue;
            m.owner=owner;m.acted=true;m.march=null;m.escortId=-1;if(m.burningOwner==former)m.burningOwner=owner;
        }
        for(int i=0;i<w.army.productions.size();i++){Army.Production p=w.army.productions.get(i);if(p.owner==former)w.army.productions.set(i,new Army.Production(p.cityId,p.officerId,owner,p.weapon,p.ship));}
        // Preserve completed military technology (and migrated legacy prerequisites), not hidden PK rolls/uses.
        w.campaign.learned.computeIfAbsent(owner,k->EnumSet.noneOf(Campaign.Tech.class)).addAll(w.campaign.learned.getOrDefault(former,EnumSet.noneOf(Campaign.Tech.class)));
        w.campaign.legacyTechs.computeIfAbsent(owner,k->EnumSet.noneOf(Campaign.Tech.class)).addAll(w.campaign.legacyTechs.getOrDefault(former,EnumSet.noneOf(Campaign.Tech.class)));
        w.campaign.cleanupProjects();w.army.cleanup();w.abilities.cleanup();
        for(Treasures.Item item:new ArrayList<>(w.treasures.items()))if(item.place==Treasures.Place.TREASURY&&item.holder==former)w.treasures.place(item.definition,Treasures.Place.TREASURY,owner);
        for(Government.Prisoner p:new ArrayList<>(w.government.prisoners())){if(p.captor==former)p.captor=owner;if(w.officer(p.officerId).owner==p.captor)w.government.free(p);}
        w.campaign.treaties.removeIf(t->t.a==former||t.b==former);w.actionPoints[former]=0;
        w.fieldworks.cleanup();w.districts.cleanup();cleanup();w.checkVictory();
    }

    void write(DataOutputStream d)throws IOException{
        d.writeInt(0x44495031);d.writeInt(attemptTurn);d.writeInt(attempts.size());for(String attempt:attempts)d.writeUTF(attempt);
        d.writeInt(aids.size());for(Aid a:aids){d.writeInt(a.requester);d.writeInt(a.ally);d.writeInt(a.source);d.writeInt(a.target);d.writeInt(a.targetOwner);d.writeInt(a.expires);d.writeInt(a.unit);d.writeBoolean(a.returning);d.writeUTF(a.status);}
    }
    void read(DataInputStream d)throws IOException{
        require(d.readInt()==0x44495031,"外交扩展标记无效");attemptTurn=d.readInt();int n=count(d,1024);
        for(int i=0;i<n;i++)require(attempts.add(d.readUTF()),"外交尝试记录重复");
        n=count(d,w.factions.length);for(int i=0;i<n;i++){Aid a=new Aid(d.readInt(),d.readInt(),d.readInt(),d.readInt(),d.readInt(),d.readInt());a.unit=d.readInt();a.returning=d.readBoolean();a.status=d.readUTF();aids.add(a);}
    }
    void validate()throws IOException{
        require(attemptTurn>=-1&&attemptTurn<=w.turn&&attempts.size()<=1024,"外交尝试时间或数量无效");
        for(String key:attempts){String[] parts=key.split(":");require(parts.length==3&&Arrays.asList("aid","exchange","surrender").contains(parts[0]),"外交尝试类型无效");
            try{int owner=Integer.parseInt(parts[1]),target=Integer.parseInt(parts[2]);require(side(owner)&&(parts[0].equals("exchange")?w.officer(target)!=null:side(target)&&target!=owner),"外交尝试引用无效");}catch(NumberFormatException e){throw new IOException("外交尝试引用无效",e);}}
        Set<Integer> requesters=new HashSet<>(),allies=new HashSet<>(),units=new HashSet<>();require(aids.size()<=w.factions.length,"援军数量过多");
        for(Aid a:aids){require(side(a.requester)&&side(a.ally)&&side(a.targetOwner)&&a.ally!=a.requester&&a.targetOwner!=a.ally&&a.targetOwner!=a.requester,"援军势力引用无效");
            require(requesters.add(a.requester)&&allies.add(a.ally)&&w.city(a.source)!=null&&w.city(a.target)!=null&&a.source!=a.target,"援军请求重复或据点引用无效");
            require(a.expires>=AID_TURNS&&a.expires<=w.turn+AID_TURNS&&a.unit>=-1&&a.status!=null&&a.status.length()<=200,"援军期限或状态无效");
            // A just-defeated or absorbed unit may be absent until command cleanup; never allow aliasing another force.
            require(a.unit<0?!a.returning:(a.unit<w.nextUnitId&&units.add(a.unit)&&(w.unit(a.unit)==null||w.unit(a.unit).owner==a.ally)),"援军部队引用无效");
        }
    }
    private boolean side(int n){return n>=0&&n<w.factions.length;}
    private static int count(DataInputStream d,int max)throws IOException{int n=d.readInt();require(n>=0&&n<=max,"外交记录数量无效");return n;}
    private static void require(boolean ok,String text)throws IOException{if(!ok)throw new IOException(text);}
}
