package game.sanguo.core;

import java.io.*;
import java.util.*;

/** v5 extension; v1-v4 sections stay byte-compatible. No transient combat adapter state is saved. */
final class CampaignSave {
    private static final int MARKER=0x43414D35;
    private CampaignSave(){}
    static void write(World w,DataOutputStream d)throws IOException {
        d.writeInt(MARKER);
        d.writeInt(w.officers.size());for(World.Officer o:w.officers){d.writeInt(o.id);for(int j=0;j<4;j++)d.writeByte(o.aptitude[j]);}
        d.writeInt(w.units.size());for(World.Unit u:w.units){d.writeInt(u.id);d.writeByte(u.status.ordinal());d.writeInt(u.statusTurns);}
        d.writeInt(w.domestic.facilities.size());for(Domestic.Facility f:w.domestic.facilities){d.writeInt(f.id);d.writeInt(f.level);d.writeInt(f.upgradeTo);}
        d.writeInt(w.factions.length);for(int side=0;side<w.factions.length;side++){
            d.writeInt(w.campaign.points(side));int mask=0;for(Campaign.Tech t:Campaign.Tech.values())if(t.ordinal()<15&&w.campaign.learned.getOrDefault(side,EnumSet.noneOf(Campaign.Tech.class)).contains(t))mask|=1<<t.ordinal();d.writeInt(mask);
        }
        d.writeInt(w.campaign.treaties.size());for(Campaign.Treaty t:w.campaign.treaties){d.writeInt(t.a);d.writeInt(t.b);d.writeByte(t.kind.ordinal());d.writeInt(t.expires);}
        d.writeInt(w.campaign.projects.size());for(Campaign.Project p:w.campaign.projects){d.writeInt(p.owner);d.writeInt(p.cityId);d.writeInt(p.officerId);d.writeInt(p.tech==null?-1:p.tech.ordinal());d.writeInt(p.study==null?-1:p.study.ordinal());}
        d.writeInt(w.campaign.traded.size());for(Map.Entry<Integer,Integer> e:w.campaign.traded.entrySet()){d.writeInt(e.getKey());d.writeInt(e.getValue());}
        d.writeInt(w.war.nextStructureId);d.writeInt(w.war.structures.size());for(War.Structure s:w.war.structures){d.writeInt(s.id);d.writeInt(s.owner);d.writeByte(s.kind.ordinal());hex(d,s.hex);d.writeInt(s.hp);}
        d.writeInt(w.war.fires.size());for(War.Fire f:w.war.fires){hex(d,f.hex);d.writeInt(f.owner);d.writeInt(f.remaining);}
    }
    static void read(World w,DataInputStream d)throws IOException {
        require(d.readInt()==MARKER,"军政扩展标记无效");Set<Integer> ids=new HashSet<>();
        int n=bound(d.readInt(),0,10000);require(n==w.officers.size(),"适性记录数量错误");
        for(int i=0;i<n;i++){int id=d.readInt();World.Officer o=w.officer(id);require(o!=null&&ids.add(id),"适性武将引用错误");for(int j=0;j<4;j++)o.aptitude[j]=bound(d.readUnsignedByte(),0,3);}
        ids.clear();n=bound(d.readInt(),0,10000);require(n==w.units.size(),"战斗状态数量错误");
        for(int i=0;i<n;i++){int id=d.readInt();World.Unit u=w.unit(id);require(u!=null&&ids.add(id),"部队状态引用错误");u.status=War.Status.values()[bound(d.readUnsignedByte(),0,War.Status.values().length-1)];u.statusTurns=d.readInt();}
        ids.clear();n=bound(d.readInt(),0,6000);require(n==w.domestic.facilities.size(),"设施等级数量错误");
        for(int i=0;i<n;i++){int id=d.readInt();Domestic.Facility f=w.domestic.facility(id);require(f!=null&&ids.add(id),"设施等级引用错误");f.level=d.readInt();f.upgradeTo=d.readInt();}
        require(d.readInt()==w.factions.length,"技巧势力数量错误");
        for(int side=0;side<w.factions.length;side++){
            int points=bound(d.readInt(),0,100000);if(points>0)w.campaign.points.put(side,points);
            int mask=bound(d.readInt(),0,(1<<15)-1);
            for(Campaign.Tech tech:Campaign.Tech.values())if(tech.ordinal()<15&&(mask&(1<<tech.ordinal()))!=0)w.campaign.learned.computeIfAbsent(side,k->EnumSet.noneOf(Campaign.Tech.class)).add(tech);
        }
        n=bound(d.readInt(),0,496);Set<Long> pairs=new HashSet<>();
        for(int i=0;i<n;i++){int a=d.readInt(),b=d.readInt();require(a<b&&pairs.add(((long)a<<32)|b),"协定双方顺序或重复错误");w.campaign.treaties.add(new Campaign.Treaty(a,b,Campaign.TreatyKind.values()[bound(d.readUnsignedByte(),0,1)],d.readInt()));}
        n=bound(d.readInt(),0,10000);for(int i=0;i<n;i++){
            int owner=d.readInt(),city=d.readInt(),officer=d.readInt(),tech=bound(d.readInt(),-1,Campaign.Tech.values().length-1),study=bound(d.readInt(),-1,Campaign.Study.values().length-1);
            require((tech<0)!=(study<0),"研究类型错误");w.campaign.projects.add(new Campaign.Project(owner,city,officer,tech<0?null:Campaign.Tech.values()[tech],study<0?null:Campaign.Study.values()[study]));
        }
        n=bound(d.readInt(),0,1000);for(int i=0;i<n;i++){int city=d.readInt(),amount=d.readInt();require(!w.campaign.traded.containsKey(city),"商人记录重复");w.campaign.traded.put(city,amount);}
        w.war.nextStructureId=d.readInt();n=bound(d.readInt(),0,1000);
        for(int i=0;i<n;i++)w.war.structures.add(new War.Structure(d.readInt(),d.readInt(),War.StructureKind.values()[bound(d.readUnsignedByte(),0,War.StructureKind.values().length-1)],hex(d),d.readInt()));
        n=bound(d.readInt(),0,w.width*w.height);for(int i=0;i<n;i++)w.war.fires.add(new War.Fire(hex(d),d.readInt(),d.readInt()));
    }
    static void validate(World w)throws IOException {
        for(World.Officer o:w.officers){require(o.aptitude.length==6,"适性数量错误");for(int v:o.aptitude)bound(v,0,3);}
        for(World.Unit u:w.units){require(u.status!=null,"部队状态缺失");bound(u.statusTurns,0,2);require(u.status!=War.Status.NORMAL||u.statusTurns==0,"正常部队仍有异常时长");}
        for(Domestic.Facility f:w.domestic.facilities){bound(f.level,1,3);bound(f.upgradeTo,0,3);require(f.upgradeTo==0||f.remaining>0&&f.upgradeTo==f.level+1&&Domestic.mergeable(f.kind),"设施合并状态错误");}
        for(Map.Entry<Integer,Integer> e:w.campaign.points.entrySet()){bound(e.getKey(),0,w.factions.length-1);bound(e.getValue(),0,100000);}
        for(Map.Entry<Integer,EnumSet<Campaign.Tech>> e:w.campaign.learned.entrySet()){
            bound(e.getKey(),0,w.factions.length-1);for(Campaign.Tech tech:e.getValue())require(w.campaign.grandfathered(e.getKey(),tech)||tech.prerequisite==null||e.getValue().contains(tech.prerequisite),"技巧前置缺失");
        }
        Set<Long> pairs=new HashSet<>();bound(w.campaign.treaties.size(),0,496);
        for(Campaign.Treaty t:w.campaign.treaties){bound(t.a,0,w.factions.length-1);bound(t.b,0,w.factions.length-1);require(t.a<t.b&&t.kind!=null&&pairs.add(((long)t.a<<32)|t.b),"协定双方错误");bound(t.expires,w.turn,100012);}
        Set<Integer> busy=new HashSet<>(),researching=new HashSet<>();bound(w.campaign.projects.size(),0,10000);
        for(Campaign.Project p:w.campaign.projects){
            World.City c=w.city(p.cityId);World.Officer o=w.officer(p.officerId);require((p.tech==null)!=(p.study==null),"研究类型冲突");
            require(c!=null&&o!=null&&c.owner==p.owner&&o.owner==p.owner&&o.cityId==c.id&&busy.add(o.id)&&o.otherTaskTurns>0&&o.otherTask.equals(p.label()),"研究武将或城池引用错误");
            if(p.tech!=null){require(researching.add(p.owner)&&!w.campaign.has(p.owner,p.tech),"重复技巧研究");require(w.campaign.grandfathered(p.owner,p.tech)||p.tech.prerequisite==null||w.campaign.has(p.owner,p.tech.prerequisite),"研究前置缺失");require(o.otherTaskTurns<=p.tech.turns,"研究工期越界");}
            else require(o.otherTaskTurns<=3,"培养工期越界");
        }
        bound(w.campaign.traded.size(),0,1000);for(Map.Entry<Integer,Integer> e:w.campaign.traded.entrySet()){require(w.city(e.getKey())!=null,"商人城池引用错误");bound(e.getValue(),1000,20000);require(e.getValue()%1000==0,"交易数量错误");}
        Set<Integer> ids=new HashSet<>();Set<Hex> occupied=new HashSet<>();bound(w.war.nextStructureId,1,10000000);bound(w.war.structures.size(),0,1000);
        for(War.Structure s:w.war.structures){bound(s.id,1,w.war.nextStructureId-1);bound(s.owner,s.kind==War.StructureKind.DAM?-1:0,w.factions.length-1);require(s.kind!=null&&ids.add(s.id)&&occupied.add(s.hex)&&(s.kind==War.StructureKind.FIRE_SHIP?w.army.water(s.hex):w.cost(s.hex,World.Weapon.SPEAR)>0)&&w.cityAt(s.hex)==null&&w.unitAt(s.hex)==null&&w.domestic.at(s.hex)==null,"军事设施重叠或位置错误");bound(s.hp,1,s.kind.hp);}
        occupied.clear();bound(w.war.fires.size(),0,w.width*w.height);
        for(War.Fire f:w.war.fires){bound(f.owner,0,w.factions.length-1);bound(f.remaining,1,3);require(occupied.add(f.hex)&&w.cost(f.hex,World.Weapon.SPEAR)>0&&w.cityAt(f.hex)==null,"火场位置无效");}
    }
    private static void hex(DataOutputStream d,Hex h)throws IOException{d.writeInt(h.q);d.writeInt(h.r);}
    private static Hex hex(DataInputStream d)throws IOException{return new Hex(d.readInt(),d.readInt());}
    private static int bound(int n,int min,int max)throws IOException{require(n>=min&&n<=max,"军政存档字段越界");return n;}
    private static void require(boolean ok,String message)throws IOException{if(!ok)throw new IOException(message);}
}
