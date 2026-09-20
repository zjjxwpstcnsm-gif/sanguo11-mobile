package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Explicit biographical data. Unknown years stay zero; no dates are inferred from names. */
public final class Lifecycle {
    public enum State { ACTIVE("已登场"), UNAPPEARED("未登场"), DEAD("已故");
        public final String label;State(String label){this.label=label;}
    }
    public static final class Life {
        public final int officer,birth,appearance,expectedDeath,home;
        State state;int diedTurn=-1;
        Life(int officer,int birth,int appearance,int death,int home,State state){this.officer=officer;this.birth=birth;this.appearance=appearance;expectedDeath=death;this.home=home;this.state=state;}
        public State state(){return state;}public int diedTurn(){return diedTurn;}
    }
    final SortedMap<Integer,Life> people=new TreeMap<>();
    final List<String> history=new ArrayList<>();
    long randomState=0x4c4946453137L;
    boolean naturalDeaths;
    int pendingRuler=-1,pendingOwner=-1;
    private final World w;
    Lifecycle(World w){this.w=w;}
    public int year(){return w.startYear+(w.startMonth-1+w.turn/3)/12;}
    public int month(){return (w.startMonth-1+w.turn/3)%12+1;}
    public Life life(int officer){return people.get(officer);}
    public State state(int officer){Life p=life(officer);return p==null?State.ACTIVE:p.state;}
    public boolean present(int officer){return w.officer(officer)!=null&&state(officer)==State.ACTIVE;}
    public int age(int officer){Life p=life(officer);return p==null||p.birth==0?-1:Math.max(0,year()-p.birth+1);}
    public boolean enabled(){return naturalDeaths;}
    public boolean pending(){return pendingRuler>=0;}
    public int departedRuler(){return pendingRuler;}
    public List<Life> people(){return Collections.unmodifiableList(new ArrayList<>(people.values()));}
    public List<String> history(){return Collections.unmodifiableList(history);}
    private void record(String text){String entry=w.date()+" · "+text;history.add(entry);while(history.size()>200)history.remove(0);w.note(text);}
    public String describe(int officer){Life p=life(officer);if(p==null)return "生卒与登场资料未配置";
        return p.state.label+" · "+(age(officer)<0?"年龄未知":age(officer)+"岁")+"\n出生年："+(p.birth==0?"未知":p.birth)+" · 登场年："+(p.appearance==0?"未配置":p.appearance)+"\n预计没年："+(p.expectedDeath==0?"未知":p.expectedDeath)+(p.state==State.DEAD?"\n已在第"+p.diedTurn+"旬去世":"");}
    /** Setup/editor entry, with all validation before mutation. Cannot resurrect or retire active assignments. */
    public void configure(int officer,int birth,int appearance,int death,int home,State state){
        World.Officer o=w.officer(officer);Life old=life(officer);
        if(o==null||state==null||state==State.DEAD||old!=null&&old.state==State.DEAD)throw new IllegalArgumentException("不能编辑已故武将或通过生卒表处死武将");
        if(birth<0||birth>9999||appearance<0||appearance>9999||death<0||death>9999||w.city(home)==null)throw new IllegalArgumentException("年份须为0（未知）或1—9999，登场据点须存在");
        if(birth>0&&(appearance>0&&appearance<birth||death>0&&death<birth)||appearance>0&&death>0&&death<appearance)throw new IllegalArgumentException("出生、登场、预计没年的顺序无效");
        if(state==State.UNAPPEARED&&(appearance<=year()||o.owner>=0||o.unitId>=0||w.domestic.busy(officer)||w.strategy.busy(officer)||!w.treasures.held(officer).isEmpty()))throw new IllegalArgumentException("未登场人物须无所属、编队、任务或宝物，且登场年晚于当前年");
        if(state==State.ACTIVE&&old!=null&&old.state==State.UNAPPEARED)throw new IllegalArgumentException("未登场人物由年初登场结算，不能提前激活");
        if(state==State.ACTIVE&&appearance>year())throw new IllegalArgumentException("已登场人物的登场年不能在未来");
        people.put(officer,new Life(officer,birth,appearance,death,home,state));
        if(state==State.UNAPPEARED){o.cityId=-1;o.role=Strategy.Role.UNAFFILIATED;o.loyalty=0;o.acted=true;}
    }
    public World.Result toggle(){w.reports.prepare();if(w.commandsBlocked()||w.active!=w.player||w.gameOver())return w.fail("当前不能更改寿命设置");naturalDeaths=!naturalDeaths;return w.success("自然死亡"+(naturalDeaths?"已开启，仅使用已配置的预计没年":"已关闭；已故武将不会复活"));}
    private int roll(){long z=(randomState+=0x9E3779B97F4A7C15L);z=(z^(z>>>30))*0xBF58476D1CE4E5B9L;z=(z^(z>>>27))*0x94D049BB133111EBL;return (int)(((z^(z>>>31))>>>1)%100);}
    /** Engineering hazard after expected death, not a claimed original executable formula. */
    public int deathChance(int officer){Life p=life(officer);if(!naturalDeaths||p==null||p.state!=State.ACTIVE||p.expectedDeath==0||year()<p.expectedDeath)return 0;return Math.min(100,10+10*(year()-p.expectedDeath));}
    void tick(){
        if(w.turn%3!=0)return;
        if(month()==1)for(Life p:people.values())if(p.state==State.UNAPPEARED&&p.appearance<=year()){
            p.state=State.ACTIVE;World.Officer o=w.officer(p.officer);o.cityId=p.home;o.acted=true;record(o.name+"于"+w.city(p.home).name+"登场，可搜索/登用");
        }
        Set<Integer> fallen=new TreeSet<>();Map<Integer,Integer> rulers=new TreeMap<>();
        for(Life p:people.values())if(deathChance(p.officer)>0&&roll()<deathChance(p.officer))fallen.add(p.officer);
        // Resolve the entire cohort before selecting successors, so an heir dying this month cannot inherit.
        for(int id:fallen){World.Officer o=w.officer(id);if(o.role==Strategy.Role.RULER)rulers.put(o.owner,id);die(id,"寿终");}
        for(Map.Entry<Integer,Integer> e:rulers.entrySet())succession(e.getKey(),e.getValue());
        w.campaign.cleanupProjects();w.army.cleanup();w.abilities.cleanup();w.districts.cleanup();w.government.relocatePrisoners();
    }
    void die(int id,String reason){
        World.Officer o=w.officer(id);if(o==null||!present(id))return;
        int former=o.owner;World.City place=o.cityId>=0?w.city(o.cityId):o.unitId>=0?w.government.refuge(former,w.unit(o.unitId).hex):null;
        Life p=life(id);if(p==null){p=new Life(id,0,0,0,place==null?w.cities.get(0).id:place.id,State.ACTIVE);people.put(id,p);}
        for(Domestic.Facility f:new ArrayList<>(w.domestic.facilities))if(f.builderId==id){
            if(f.upgradeTo>0){f.upgradeTo=0;f.remaining=0;f.builderId=-1;}else w.domestic.facilities.remove(f);
            record(o.name+"经办的"+f.kind.label+"施工中止，不退费");
        }
        w.domestic.officerDied(id);
        World.Unit u=w.unit(o.unitId);
        if(u!=null){
            if(u.officerId!=id)u.deputies=Arrays.stream(u.deputies).filter(x->x!=id).toArray();
            else if(u.deputies.length>0){
                int heir=Arrays.stream(u.deputies).boxed().min(Comparator.comparingInt((Integer x)->-w.officer(x).leadership).thenComparingInt(x->x)).get();
                World.Unit next=new World.Unit(u.id,u.owner,heir,u.weapon,u.hex,u.troops,u.food);
                next.deputies=Arrays.stream(u.deputies).filter(x->x!=heir).toArray();next.gold=u.gold;next.ship=u.ship;next.energy=u.energy;next.acted=u.acted;next.march=u.march;
                next.movementBudget=u.movementBudget;next.movementSpent=u.movementSpent;next.status=u.status;next.statusTurns=u.statusTurns;next.burning=u.burning;next.burningOwner=u.burningOwner;next.burningPower=u.burningPower;
                w.units.set(w.units.indexOf(u),next);record(w.officer(heir).name+"接掌"+o.name+"部队，兵粮与行军指令保留");
            }else{w.government.escortLost(u,null);w.units.remove(u);record(o.name+"部队失去主将而解散，所携兵粮散失");}
        }
        for(Treasures.Item i:new ArrayList<>(w.treasures.held(id)))w.treasures.place(i.definition,former>=0?Treasures.Place.TREASURY:Treasures.Place.HIDDEN,former>=0?former:p.home);
        w.strategy.releaseGovernor(id);w.government.allegianceChanged(id);w.government.prisoners.remove(id);w.contests.injuries.remove(id);
        o.owner=-1;o.cityId=-1;o.unitId=-1;o.role=Strategy.Role.UNAFFILIATED;o.loyalty=0;o.otherTask="";o.otherTaskTurns=0;o.acted=true;
        p.state=State.DEAD;p.diedTurn=w.turn;record(o.name+"去世（"+reason+"）");
        w.campaign.cleanupProjects();w.army.cleanup();w.abilities.cleanup();w.fieldworks.cleanup();
    }
    private List<World.Officer> candidates(int owner,int departed){
        List<World.Officer> list=new ArrayList<>();for(World.Officer o:w.officers)if(o.owner==owner&&present(o.id)&&!w.government.captive(o.id))list.add(o);
        list.sort(Comparator.comparingInt((World.Officer o)->-priority(o,departed)).thenComparingInt(o->o.id));return list;
    }
    private int priority(World.Officer o,int dead){return (w.relations.parent(o.id,false)==dead||w.relations.parent(o.id,true)==dead?1000000:w.relations.blood(o.id,dead)?500000:w.relations.bonded(o.id,dead)?200000:0)+Math.min(100000,w.government.merit(o.id))/10+o.leadership+o.politics+o.charm;}
    public List<World.Officer> successors(){return pending()?Collections.unmodifiableList(candidates(pendingOwner,pendingRuler)):Collections.emptyList();}
    private void succession(int owner,int departed){
        List<World.Officer> list=candidates(owner,departed);
        if(list.isEmpty()){
            for(World.City c:w.cities)if(c.owner==owner){c.owner=-1;c.governorId=-1;w.domestic.captured(c.id);w.government.policies.remove(c.id);}
            for(Treasures.Item i:new ArrayList<>(w.treasures.owned(owner)))if(i.place==Treasures.Place.TREASURY)w.treasures.place(i.definition,Treasures.Place.HIDDEN,life(departed).home);
            record(w.faction(owner)+"无人继承，势力解散");return;
        }
        if(owner==w.player&&w.alive(owner)){pendingRuler=departed;pendingOwner=owner;record("请为"+w.faction(owner)+"选择继承人");}
        else crown(owner,list.get(0));
    }
    private void crown(int owner,World.Officer o){
        w.strategy.releaseGovernor(o.id);w.government.allegianceChanged(o.id);
        o.role=Strategy.Role.RULER;o.loyalty=100;
        // Faction identity stays stable for treaties, districts, saves and targeted march orders.
        record(o.name+"继承"+w.faction(owner)+"君主之位");w.districts.cleanup();
    }
    public World.Result inherit(int departed,int heir){w.reports.prepare();
        if(!pending()||departed!=pendingRuler||pendingOwner!=w.active||w.contests.busy())return w.fail("继承事件已变化，请刷新");
        World.Officer o=w.officer(heir);if(!successors().contains(o))return w.fail("请选择现存、未被俘的本势力武将");
        int owner=pendingOwner;pendingOwner=-1;pendingRuler=-1;crown(owner,o);w.checkVictory();
        return w.success("继承完成，可继续当前旬");
    }
    public World.Result executePrisoner(int city,int actor,int target){w.reports.prepare();
        World.City c=w.city(city);String error=w.cityError(c,w.officer(actor),0);if(error!=null)return w.fail(error);
        Government.Prisoner prisoner=w.government.prisoner(target);if(prisoner==null||prisoner.captor!=w.active||prisoner.cityId!=city)return w.fail("请选择本城关押的俘虏");
        World.Officer t=w.officer(target);int owner=t.owner;boolean ruler=t.role==Strategy.Role.RULER;
        w.spend(c,w.officer(actor),0);die(target,"处决");if(ruler)succession(owner,target);
        if(owner>=0&&owner!=w.active)w.strategy.setFactionRelation(w.active,owner,-100);
        w.government.relocatePrisoners();w.checkVictory();return w.success(t.name+"已被处决");
    }
    void write(DataOutputStream d)throws IOException{
        d.writeInt(0x4c494631);d.writeBoolean(naturalDeaths);d.writeLong(randomState);d.writeInt(pendingRuler);d.writeInt(pendingOwner);d.writeInt(people.size());
        for(Life p:people.values()){d.writeInt(p.officer);d.writeInt(p.birth);d.writeInt(p.appearance);d.writeInt(p.expectedDeath);d.writeInt(p.home);d.writeByte(p.state.ordinal());d.writeInt(p.diedTurn);}
        d.writeInt(history.size());for(String entry:history)d.writeUTF(entry);
    }
    void read(DataInputStream d)throws IOException{
        if(d.readInt()!=0x4c494631)throw new IOException("生卒存档段错误");naturalDeaths=d.readBoolean();randomState=d.readLong();pendingRuler=d.readInt();pendingOwner=d.readInt();int n=bound(d.readInt(),0,w.officers.size());
        for(int i=0;i<n;i++){int id=d.readInt();Life p=new Life(id,d.readInt(),d.readInt(),d.readInt(),d.readInt(),State.values()[bound(d.readUnsignedByte(),0,2)]);p.diedTurn=d.readInt();if(people.put(id,p)!=null)throw new IOException("重复生卒人物");}
        n=bound(d.readInt(),0,200);for(int i=0;i<n;i++)history.add(d.readUTF());
    }
    void validate()throws IOException{
        bound(people.size(),0,w.officers.size());
        for(Life p:people.values()){
            World.Officer o=w.officer(p.officer);require(o!=null&&p.state!=null&&w.city(p.home)!=null,"生卒人物/据点缺失");
            bound(p.birth,0,9999);bound(p.appearance,0,9999);bound(p.expectedDeath,0,9999);bound(p.diedTurn,-1,w.turn);
            require(p.birth==0||(p.appearance==0||p.appearance>=p.birth)&&(p.expectedDeath==0||p.expectedDeath>=p.birth),"生卒日期顺序错误");require(p.appearance==0||p.expectedDeath==0||p.expectedDeath>=p.appearance,"登场晚于预计没年");
            require((p.state==State.DEAD)==(p.diedTurn>=0),"死亡日期与状态矛盾");
            if(p.state!=State.ACTIVE)require(o.owner==-1&&o.cityId==-1&&o.unitId==-1&&o.role==Strategy.Role.UNAFFILIATED&&o.otherTaskTurns==0&&!w.domestic.busy(o.id)&&!w.government.captive(o.id)&&w.treasures.held(o.id).isEmpty(),"未登场/已故人物仍在参与战局");
            if(p.state==State.UNAPPEARED)require(p.appearance>year(),"逾期未登场人物");
        }
        bound(pendingOwner,-1,w.factions.length-1);require(pendingRuler>=-1&&(pendingOwner<0)==(pendingRuler<0),"继承引用不完整");
        if(pending())require(pendingOwner==w.player&&state(pendingRuler)==State.DEAD&&!successors().isEmpty()&&!w.contests.busy()&&w.officers.stream().noneMatch(o->o.owner==pendingOwner&&o.role==Strategy.Role.RULER),"继承事件无效");
        bound(history.size(),0,200);for(String s:history)require(s!=null&&!s.isEmpty()&&s.length()<=500,"生卒事件记录无效");
    }
    private static int bound(int n,int a,int b)throws IOException{require(n>=a&&n<=b,"生卒字段越界");return n;}
    private static void require(boolean ok,String text)throws IOException{if(!ok)throw new IOException(text);}
}
