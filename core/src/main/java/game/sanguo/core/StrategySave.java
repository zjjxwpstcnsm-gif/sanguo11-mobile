package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Save v4 extension. The complete v3 field order is retained before this section. */
final class StrategySave {
    private static final int MARKER=0x53545234;
    private StrategySave() {}
    static void write(Strategy s,DataOutputStream d)throws IOException {
        World w=s.w;d.writeInt(MARKER);d.writeLong(s.randomState);
        d.writeInt(w.cities.size());
        for(World.City c:w.cities){d.writeInt(c.id);d.writeInt(c.recruitReserve);d.writeInt(c.governorId);}
        d.writeInt(w.officers.size());
        for(World.Officer o:w.officers){
            d.writeInt(o.id);d.writeInt(o.loyalty);d.writeByte(o.role.ordinal());
            d.writeInt(o.otherTaskTurns);d.writeUTF(o.otherTask);d.writeInt(o.lastRewardTurn);
        }
        d.writeInt(s.talents.size());
        for(Strategy.Talent t:s.talents){
            d.writeInt(t.id);d.writeUTF(t.name);d.writeInt(t.cityId);d.writeInt(t.leadership);d.writeInt(t.war);
            d.writeInt(t.intelligence);d.writeInt(t.politics);d.writeInt(t.charm);d.writeInt(t.availableTurn);
        }
        d.writeInt(s.relations.size());
        for(Map.Entry<Long,Integer> e:s.relations.entrySet()){d.writeLong(e.getKey());d.writeInt(e.getValue());}
    }
    static void read(Strategy s,DataInputStream d)throws IOException {
        World w=s.w;require(d.readInt()==MARKER,"战略扩展标记无效");s.randomState=d.readLong();
        int n=bound(d.readInt(),0,1000);require(n==w.cities.size(),"城池战略字段数量错误");Set<Integer> ids=new HashSet<>();
        for(int i=0;i<n;i++){
            int id=d.readInt();World.City c=w.city(id);require(c!=null&&ids.add(id),"城池战略引用重复或缺失");
            c.recruitReserve=d.readInt();c.governorId=d.readInt();
        }
        n=bound(d.readInt(),0,10000);require(n==w.officers.size(),"武将战略字段数量错误");ids.clear();
        for(int i=0;i<n;i++){
            int id=d.readInt();World.Officer o=w.officer(id);require(o!=null&&ids.add(id),"武将战略引用重复或缺失");
            o.loyalty=d.readInt();o.role=Strategy.Role.values()[bound(d.readUnsignedByte(),0,Strategy.Role.values().length-1)];
            o.otherTaskTurns=d.readInt();o.otherTask=d.readUTF();o.lastRewardTurn=d.readInt();
        }
        n=bound(d.readInt(),0,10000);
        for(int i=0;i<n;i++)s.talents.add(new Strategy.Talent(d.readInt(),d.readUTF(),d.readInt(),d.readInt(),d.readInt(),d.readInt(),d.readInt(),d.readInt(),d.readInt()));
        n=bound(d.readInt(),0,496);
        for(int i=0;i<n;i++){long key=d.readLong();require(!s.relations.containsKey(key),"势力关系重复");s.relations.put(key,d.readInt());}
    }
    static void validateTalent(World w,Strategy.Talent t)throws IOException {
        require(t!=null&&t.id>=0,"未发现人才编号无效");text(t.name,100,false);
        require(w.city(t.cityId)!=null,"未发现人才城池不存在");bound(t.availableTurn,0,100000);
        for(int n:new int[]{t.leadership,t.war,t.intelligence,t.politics,t.charm})bound(n,0,100);
    }
    static void validate(Strategy s)throws IOException {
        World w=s.w;Set<Integer> governors=new HashSet<>(),rulers=new HashSet<>(),ids=new HashSet<>();
        for(World.City c:w.cities){
            bound(c.recruitReserve,0,1_000_000);require(c.governorId>=-1,"太守编号无效");
            if(c.governorId>=0){
                World.Officer o=w.officer(c.governorId);
                require(o!=null&&c.owner>=0&&o.owner==c.owner&&o.cityId==c.id&&o.unitId==-1&&governors.add(o.id),"太守位置或所属势力无效");
                require(o.role==Strategy.Role.GOVERNOR||o.role==Strategy.Role.RULER,"太守身份不匹配");
            }
        }
        for(World.Officer o:w.officers){
            ids.add(o.id);bound(o.loyalty,0,100);bound(o.lastRewardTurn,-1,w.turn);require(o.role!=null,"武将身份缺失");
            bound(o.otherTaskTurns,0,12);text(o.otherTask,80,true);
            require((o.otherTaskTurns>0)==!o.otherTask.isEmpty(),"任务名称与剩余旬数不匹配");
            if(o.otherTaskTurns>0)require(o.owner>=0&&o.cityId>=0&&o.unitId==-1&&!w.domestic.busy(o.id),"武将战略任务冲突");
            if(o.role==Strategy.Role.GOVERNOR)require(governors.contains(o.id),"太守缺少任命城池");
            if(o.role==Strategy.Role.RULER)require(o.owner>=0&&rulers.add(o.owner)&&o.loyalty==100,"君主重复、势力无效或忠诚错误");
            if(o.owner<0)require(o.role==Strategy.Role.UNAFFILIATED&&o.cityId>=0&&o.unitId==-1&&o.loyalty==0&&o.otherTaskTurns==0&&!w.domestic.busy(o.id),"在野武将身份或任务错误");
            else require(o.role!=Strategy.Role.UNAFFILIATED,"所属勢力与身份不符");
        }
        bound(s.talents.size()+w.officers.size(),0,10000);
        for(Strategy.Talent t:s.talents){validateTalent(w,t);require(ids.add(t.id),"未发现人才与武将编号重复");}
        bound(s.relations.size(),0,496);
        for(Map.Entry<Long,Integer> e:s.relations.entrySet()){
            long key=e.getKey();int a=(int)(key>>>32),b=(int)key;
            require(a>=0&&a<b&&b<w.factions.length,"势力关系引用无效");bound(e.getValue(),-100,100);
            require(e.getValue()!=0,"默认关系不应重复保存");
        }
    }
    private static int bound(int n,int min,int max)throws IOException { require(n>=min&&n<=max,"战略存档字段越界");return n; }
    private static void require(boolean ok,String reason)throws IOException { if(!ok)throw new IOException(reason); }
    private static void text(String text,int max,boolean empty)throws IOException {
        require(text!=null&&text.length()<=max&&(empty||!text.trim().isEmpty()),"战略存档文本无效");
    }
}
