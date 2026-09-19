package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Shared personnel rules. Exact mobile probabilities/decay amounts are documented, not claimed original. */
public final class Loyalty {
    private final World w;
    Loyalty(World w){this.w=w;}
    public World.Officer ruler(int owner){
        for(World.Officer o:w.officers)if(o.owner==owner&&o.role==Strategy.Role.RULER&&w.life.present(o.id))return o;
        return null;
    }
    public World.Unit fieldUnit(int officer){
        World.Officer o=w.officer(officer);if(o==null)return null;
        World.Unit u=w.unit(o.unitId);if(u!=null&&w.army.contains(u,officer))return u;
        for(Domestic.Mission m:w.domestic.missions)if(m.transport&&m.contains(officer))return m;
        return null;
    }
    public static int distance(World.Officer a,World.Officer b){
        if(a==null||b==null||a.affinity<0||b.affinity<0)return -1;
        int delta=Math.abs(a.affinity-b.affinity);return Math.min(delta,150-delta);
    }
    public boolean protectedLoyalty(World.Officer o){
        if(o==null||o.owner<0)return false;
        if(o.role==Strategy.Role.RULER)return true;
        World.Officer ruler=ruler(o.owner);
        return ruler!=null&&w.relations.bonded(o.id,ruler.id);
    }
    private boolean naturalProtected(World.Officer o){
        World.Officer ruler=ruler(o.owner);
        return protectedLoyalty(o)||ruler!=null&&(w.relations.blood(o.id,ruler.id)||w.relations.likes(o.id,ruler.id));
    }
    public int lose(World.Officer o,int amount){
        if(o==null||o.owner<0||protectedLoyalty(o))return 0;
        int lost=Math.min(o.loyalty,Math.max(0,amount));o.loyalty-=lost;return lost;
    }
    /** Season boundary is calendar-based, including scenarios that start outside January. */
    void tick(){
        if(w.turn==0||w.turn%3!=0||(w.life.month()-1)%3!=0)return;
        for(World.Officer o:w.officers){
            if(o.owner<0||!w.life.present(o.id)||w.government.captive(o.id)||naturalProtected(o))continue;
            World.Unit field=fieldUnit(o.id);World.City city=w.city(o.cityId);
            if(field==null&&(city==null||city.owner!=o.owner||w.skills.city(city.id,Skill.RENZHENG)))continue;
            int gap=distance(o,ruler(o.owner));
            int base=field!=null?Math.max(1,1+Math.max(0,gap)/25+(3-o.honor)/2)
                :gap<0?0:Math.max(0,1+gap/25-o.honor/2);
            int lost=lose(o,w.campaign.loyaltyLoss(o.owner,base));
            if(lost>0&&(field!=null||o.loyalty<80))w.note(o.name+(field!=null?"在外领兵":"季节结算")+"，忠诚−"+lost+"（现"+o.loyalty+"）"+(field!=null?"；回城后方可褒奖":""));
        }
    }
    public String describe(World.Officer o){
        int gap=distance(o,ruler(o.owner));
        String detail="相性 "+(o.affinity<0?"未录入":o.affinity)+" · 义理 "+o.honor+"/5"+(gap<0?"":" · 与君主相性差 "+gap);
        if(protectedLoyalty(o))return detail+"\n君主 / 君主配偶或义兄弟：忠诚不下降。";
        if(naturalProtected(o))return detail+"\n与君主的血缘 / 亲爱关系保护自然忠诚衰减。";
        if(fieldUnit(o.id)!=null)return detail+"\n在外领兵：季初结算忠诚衰减，不能远程褒奖。"+(o.loyalty<80?" 低忠诚，请警惕敌方登用。":"");
        return detail+"\n相性差越小越亲近；义理越高越不易变节。";
    }
    int compatibilityBonus(World.Officer actor,World.Officer target){int gap=distance(actor,target);return gap<0?0:(35-gap)/2;}
    boolean refusesRuler(World.Officer target){
        World.Officer current=target==null?null:ruler(target.owner);
        return current!=null&&w.relations.likes(target.id,current.id);
    }
    int recruitmentAdjustment(World.Officer actor,World.Officer target,int owner){
        int gap=distance(target,ruler(owner));
        return compatibilityBonus(actor,target)+(gap<0?0:(35-gap)/5)-(target.owner<0?0:(target.honor-3)*4);
    }
    public List<World.Officer> recruitmentActors(int city,int target){
        List<World.Officer> result=new ArrayList<>(w.idle(w.city(city)));
        result.sort(Comparator.comparingInt((World.Officer o)->w.strategy.recruitmentChance(city,o.id,target)).reversed()
            .thenComparingInt(o->{int d=distance(o,w.officer(target));return d<0?76:d;}).thenComparingInt(o->o.id));
        return result;
    }
    public String recommendation(int city,int target,int actor){
        World.City c=w.city(city);World.Officer adviser=c==null?null:w.government.advisor(c.owner),o=w.officer(actor);
        List<World.Officer> ranked=recruitmentActors(city,target);int gap=distance(o,w.officer(target));
        boolean advising=adviser!=null&&w.life.present(adviser.id)&&!w.government.captive(adviser.id);
        String prefix=advising&&!ranked.isEmpty()&&ranked.get(0).id==actor?adviser.name+"推荐 · ":advising?"":"未任命军师 · ";
        return prefix+(gap<0?"相性未录入":"相性差"+gap)+" · 当前成功率"+w.strategy.recruitmentChance(city,actor,target)+"%";
    }
    public int followChance(World.Officer deputy,World.Officer commander,int newOwner){
        if(deputy==null||deputy.role==Strategy.Role.RULER||protectedLoyalty(deputy))return 0;
        World.Officer next=ruler(newOwner);
        if(w.relations.dislikes(deputy.id,commander.id)||next!=null&&w.relations.dislikes(deputy.id,next.id))return 0;
        if(w.relations.bonded(deputy.id,commander.id))return 100;
        if(w.relations.loyalBond(deputy.id)||refusesRuler(deputy))return 0;
        return Math.max(0,Math.min(85,(100-deputy.loyalty)*2-deputy.honor*6+compatibilityBonus(commander,deputy)));
    }
    private void changeOfficer(World.Officer actor,World.Officer target){
        w.strategy.releaseGovernor(target.id);w.government.allegianceChanged(target.id);
        target.owner=actor.owner;target.role=Strategy.Role.OFFICER;target.loyalty=Math.min(100,60+actor.charm/10+actor.politics/5);
        target.lastRewardTurn=-1;target.acted=true;target.otherTask="";target.otherTaskTurns=0;
    }
    /** Atomic allegiance transition. Field commanders retain their actual unit and cargo; a deputy alone does not. */
    void join(World.Officer actor,World.Officer target,int city){
        World.Unit u=fieldUnit(target.id);
        if(u==null){changeOfficer(actor,target);target.unitId=-1;target.cityId=city;return;}
        if(w.turnJournal!=null)w.turnJournal.mark(TurnJournal.Kind.PLOT,u.id,u.hex,"登用倒戈");
        if(u.officerId!=target.id){
            u.deputies=Arrays.stream(u.deputies).filter(id->id!=target.id).toArray();
            changeOfficer(actor,target);target.unitId=-1;target.cityId=city;
            w.note(target.name+"脱离原部队接受登用，原主将与部队归属不变");
        }else{
            List<World.Officer> following=new ArrayList<>(),staying=new ArrayList<>();
            for(int id:u.deputies){World.Officer deputy=w.officer(id);int chance=followChance(deputy,target,actor.owner);
                if(chance==100||chance>0&&w.strategy.nextInt(100)<chance)following.add(deputy);else staying.add(deputy);}
            String old=w.faction(u.owner);u.owner=actor.owner;u.deputies=following.stream().mapToInt(o->o.id).toArray();
            List<World.Officer> changed=new ArrayList<>(following);changed.add(target);
            for(World.Officer o:changed){changeOfficer(actor,o);o.cityId=-1;o.unitId=u instanceof Domestic.Mission?-1:u.id;}
            for(World.Officer o:staying){w.retreat(o,u.hex);w.note(o.name+"拒绝跟随倒戈，脱离部队返回原势力据点");}
            u.march=null;u.acted=true;u.movementSpent=u.movementBudget<0?0:u.movementBudget;
            u.energy=Math.min(u.energy,w.campaign.energyCap(u.owner));
            w.aiOrders.orders.remove(u.id);w.districts.units.remove(u.id);w.diplomacy.aids.removeIf(a->a.unit==u.id);
            for(Domestic.Mission m:w.domestic.missions)if(m.escortId==u.id)m.escortId=-1;
            if(u instanceof Domestic.Mission){Domestic.Mission m=(Domestic.Mission)u;World.City refuge=w.government.refuge(u.owner,u.hex);
                m.sourceCity=refuge==null?city:refuge.id;m.targetCity=m.sourceCity;m.returnOfficers=false;m.returning=false;m.stopped=true;m.waiting="倒戈后待命，请重新选择运输目的地";m.escortId=-1;}
            for(Government.Prisoner p:new ArrayList<>(w.government.escorted(u.id))){
                if(w.officer(p.officerId).owner==u.owner)w.government.free(p);else{p.captor=u.owner;p.lastAttempt=-1;}}
            w.fieldworks.cleanup();
            w.note(target.name+"率部由"+old+"倒戈至"+w.faction(u.owner)+"，保留兵"+u.troops+"、粮"+u.food+"、金"+u.gold+"；随行副将"+following.size()+"人");
        }
        if(w.turnJournal!=null)w.turnJournal.checkpoint("登用与部队归属变更");
    }
    void write(DataOutputStream out)throws IOException{
        out.writeInt(w.officers.size());for(World.Officer o:w.officers){out.writeInt(o.id);out.writeInt(o.affinity);out.writeInt(o.honor);}
    }
    void read(DataInputStream in)throws IOException{
        int n=in.readInt();if(n!=w.officers.size())throw new IOException("相性资料数量不一致");Set<Integer> seen=new HashSet<>();
        for(int i=0;i<n;i++){int id=in.readInt();World.Officer o=w.officer(id);if(o==null||!seen.add(id))throw new IOException("相性资料引用错误");o.affinity=in.readInt();o.honor=in.readInt();}
    }
    void validate()throws IOException{for(World.Officer o:w.officers)if(o.affinity< -1||o.affinity>149||o.honor<1||o.honor>5)throw new IOException("相性 / 义理越界");}
}
