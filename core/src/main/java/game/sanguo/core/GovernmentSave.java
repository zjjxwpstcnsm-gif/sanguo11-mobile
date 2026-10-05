package game.sanguo.core;

import java.io.*;
import java.util.*;

/** v8 append-only extension; v1-v7 field order remains intact. */
final class GovernmentSave {
    private static final int MARKER=0x474F5638;
    static void write(World w,DataOutputStream d)throws IOException {
        Government g=w.government;d.writeInt(MARKER);
        d.writeInt(g.merits.size());for(Map.Entry<Integer,Integer> e:g.merits.entrySet()){d.writeInt(e.getKey());d.writeInt(e.getValue());}
        d.writeInt(g.ranks.size());for(Map.Entry<Integer,String> e:g.ranks.entrySet()){d.writeInt(e.getKey());d.writeUTF(e.getValue());}
        d.writeInt(g.advisors.size());for(Map.Entry<Integer,Integer> e:g.advisors.entrySet()){d.writeInt(e.getKey());d.writeInt(e.getValue());}
        d.writeInt(g.policies.size());for(Map.Entry<Integer,Government.Policy> e:g.policies.entrySet()){d.writeInt(e.getKey());d.writeUTF(e.getValue().name());}
        d.writeInt(g.prisoners.size());for(Government.Prisoner p:g.prisoners.values()){
            d.writeInt(p.officerId);d.writeInt(p.captor);d.writeInt(p.cityId);d.writeInt(p.capturedTurn);d.writeInt(p.lastAttempt);d.writeInt(p.unitId);
        }
        d.writeInt(w.domestic.missions.size());for(Domestic.Mission m:w.domestic.missions){d.writeInt(m.id);d.writeBoolean(m.sea);}
    }
    static void read(World w,DataInputStream d,int version)throws IOException {
        require(d.readInt()==MARKER,"军政扩展标记无效");Government g=w.government;
        int n=count(d,10000);for(int i=0;i<n;i++){int k=d.readInt();require(g.merits.put(k,d.readInt())==null,"功绩记录重复");}
        n=count(d,10000);for(int i=0;i<n;i++){int k=d.readInt();require(g.ranks.put(k,d.readUTF())==null,"官职记录重复");}
        n=count(d,w.factions.length);for(int i=0;i<n;i++){int k=d.readInt();require(g.advisors.put(k,d.readInt())==null,"军师记录重复");}
        n=count(d,w.cities.size());for(int i=0;i<n;i++){int k=d.readInt();Government.Policy policy;
            try{policy=Government.Policy.valueOf(d.readUTF());}catch(IllegalArgumentException e){throw new IOException("委任方针无效",e);}
            require(g.policies.put(k,policy)==null,"委任记录重复");}
        n=count(d,w.officers.size());for(int i=0;i<n;i++){
            Government.Prisoner p=new Government.Prisoner(d.readInt(),d.readInt(),d.readInt(),d.readInt());p.lastAttempt=d.readInt();if(version>=16)p.unitId=d.readInt();require(g.prisoners.put(p.officerId,p)==null,"俘虏记录重复");
        }
        n=count(d,w.domestic.missions.size());require(n==w.domestic.missions.size(),"运输方式记录缺失");Set<Integer> ids=new HashSet<>();
        for(int i=0;i<n;i++){int id=d.readInt();Domestic.Mission m=w.domestic.mission(id);require(m!=null&&ids.add(id),"运输方式引用无效");m.sea=d.readBoolean();}
    }
    static void validate(World w)throws IOException {
        Government g=w.government;
        Map<Integer,PcScenarioPeople.Person> sourcePeople=new HashMap<>();for(PcScenarioPeople.Person p:PcScenarioPeople.saved(w))if(p.officerId>=0)sourcePeople.put(p.officerId,p);
        Map<Integer,Map<Integer,Integer>> sourceForces=PcScenarioPeople.savedForces(w);
        require(g.merits.size()<=w.officers.size()&&g.ranks.size()<=w.officers.size()&&g.prisoners.size()<=w.officers.size(),"军政记录过多");
        for(Map.Entry<Integer,Integer> e:g.merits.entrySet())require(w.officer(e.getKey())!=null&&e.getValue()>0&&e.getValue()<=1000000,"功绩引用或数值无效");
        Map<String,Integer> offices=new HashMap<>();
        for(Map.Entry<Integer,String> e:g.ranks.entrySet()){
            World.Officer o=w.officer(e.getKey());Government.Rank r=Government.rank(e.getValue());
            require(o!=null&&o.owner>=0&&o.role!=Strategy.Role.RULER&&!g.captive(o.id)&&r!=null,"官职引用无效");
            Integer previous=offices.putIfAbsent(o.owner+":"+r.id,o.id);
            boolean initial=sourceRank(w,sourcePeople.get(o.id),o,r);
            require(g.merit(o.id)>=r.merit||initial,"官职功绩不足");
            require(previous==null||initial&&sourceRank(w,sourcePeople.get(previous),w.officer(previous),r),"同势力重复官职，且不是原保存的初始配置");
        }
        for(Map.Entry<Integer,Integer> e:g.advisors.entrySet()){
            World.Officer o=w.officer(e.getValue());require(e.getKey()>=0&&e.getKey()<w.factions.length&&o!=null&&o.owner==e.getKey()&&!g.captive(o.id)&&((o.intelligence>=70&&o.role!=Strategy.Role.RULER)||sourceAdvisor(w,sourcePeople.get(o.id),sourceForces.get(e.getKey()),o,e.getKey())),"军师引用无效");
        }
        for(Map.Entry<Integer,Government.Policy> e:g.policies.entrySet())require(w.city(e.getKey())!=null&&w.city(e.getKey()).owner>=0&&e.getValue()!=null&&e.getValue()!=Government.Policy.MANUAL,"委任城池或方针无效");
        for(Government.Prisoner p:g.prisoners.values()){
            World.Officer o=w.officer(p.officerId);World.City c=w.city(p.cityId);World.Unit u=w.unit(p.unitId);
            require(o!=null&&o.owner>=-1&&p.captor>=0&&p.captor<w.factions.length&&p.captor!=o.owner,"俘虏归属错误");
            require(p.unitId>=0?(p.cityId==-1&&u!=null&&u.owner==p.captor):(p.unitId==-1&&c!=null&&c.owner==p.captor),"俘虏关押地或押送部队错误");
            require(o.unitId==-1&&o.cityId==-1&&o.otherTaskTurns==0&&!w.domestic.busy(o.id)&&o.role!=Strategy.Role.GOVERNOR,"俘虏仍承担部队或城务");
            require(p.capturedTurn>=0&&p.capturedTurn<=w.turn&&p.lastAttempt>=-1&&p.lastAttempt<=w.turn,"俘虏日期无效");
        }
        for(Domestic.Mission m:w.domestic.missions)require(!m.sea||m.transport||m.returning,"人员调动不能使用运输方式扩展");
    }
    private static boolean sourceRank(World w,PcScenarioPeople.Person original,World.Officer officer,Government.Rank rank)throws IOException{
        if(!w.pcSourceFrame||original==null||officer==null||rank==null)return false;
        int nativeRank=original.field(21);
        return nativeRank>=0&&nativeRank<80&&PcOfficerRanks.all().get(nativeRank).projectId.equals(rank.id)
            &&officer.owner==original.field(75)&&w.government.merit(officer.id)>=original.field(24);
    }
    private static boolean sourceAdvisor(World w,PcScenarioPeople.Person original,Map<Integer,Integer> force,World.Officer officer,int side)throws IOException{
        return w.pcSourceFrame&&original!=null&&force!=null&&Objects.equals(force.get(4),original.nativeId)
            &&original.field(75)==side&&w.life.present(officer.id);
    }
    private static int count(DataInputStream d,int max)throws IOException {int n=d.readInt();require(n>=0&&n<=max,"军政记录数量无效");return n;}
    private static void require(boolean ok,String reason)throws IOException{if(!ok)throw new IOException(reason);}
}
