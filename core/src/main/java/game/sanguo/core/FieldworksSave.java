package game.sanguo.core;

import java.io.*;
import java.util.*;

/** v11 extension. Technology names avoid 32-bit mask collisions; old sections remain readable. */
final class FieldworksSave {
    private static final int MARKER=0x46573131;
    private FieldworksSave(){}
    static void write(World w,DataOutputStream d)throws IOException{
        d.writeInt(MARKER);d.writeInt(w.factions.length);
        for(int owner=0;owner<w.factions.length;owner++){
            names(d,w.campaign.learned.getOrDefault(owner,EnumSet.noneOf(Campaign.Tech.class)));
            names(d,w.campaign.legacyTechs.getOrDefault(owner,EnumSet.noneOf(Campaign.Tech.class)));
        }
        d.writeInt(w.cities.size());for(World.City c:w.cities){d.writeInt(c.id);d.writeUTF(c.kind.name());d.writeInt(c.baseDefense);}
        d.writeInt(w.units.size());for(World.Unit u:w.units){d.writeInt(u.id);d.writeInt(u.gold);}
        d.writeInt(w.war.structures.size());for(War.Structure s:w.war.structures){d.writeInt(s.id);d.writeBoolean(s.complete);d.writeInt(s.builder);d.writeInt(s.direction);}
    }
    private static void names(DataOutputStream d,Set<Campaign.Tech> set)throws IOException{d.writeInt(set.size());for(Campaign.Tech t:set)d.writeUTF(t.name());}
    private static EnumSet<Campaign.Tech> names(DataInputStream d)throws IOException{
        EnumSet<Campaign.Tech> set=EnumSet.noneOf(Campaign.Tech.class);int count=bound(d.readInt(),0,Campaign.Tech.values().length);
        for(int i=0;i<count;i++)try{require(set.add(Campaign.Tech.valueOf(d.readUTF())),"技巧重复");}catch(IllegalArgumentException e){throw new IOException("未知技巧ID",e);}return set;
    }
    static void read(World w,DataInputStream d)throws IOException{
        require(d.readInt()==MARKER,"野战工程扩展无效");require(d.readInt()==w.factions.length,"技巧势力数量错误");
        for(int side=0;side<w.factions.length;side++){
            EnumSet<Campaign.Tech> previous=w.campaign.learned.getOrDefault(side,EnumSet.noneOf(Campaign.Tech.class));
            EnumSet<Campaign.Tech> set=names(d);for(int i=0;i<15;i++)require(set.contains(Campaign.Tech.values()[i])==previous.contains(Campaign.Tech.values()[i]),"技巧兼容段不一致");
            w.campaign.learned.put(side,set);w.campaign.legacyTechs.put(side,names(d));
        }
        Set<Integer> seen=new HashSet<>();require(d.readInt()==w.cities.size(),"据点类型数量错误");
        for(int i=0;i<w.cities.size();i++){World.City c=w.city(d.readInt());require(c!=null&&seen.add(c.id),"据点类型引用错误");try{c.kind=World.SiteKind.valueOf(d.readUTF());}catch(IllegalArgumentException e){throw new IOException("据点类型错误",e);}c.baseDefense=d.readInt();}
        w.invalidateSiteIndex();
        seen.clear();require(d.readInt()==w.units.size(),"携金数量错误");for(int i=0;i<w.units.size();i++){World.Unit u=w.unit(d.readInt());require(u!=null&&seen.add(u.id),"携金部队引用错误");u.gold=d.readInt();}
        seen.clear();require(d.readInt()==w.war.structures.size(),"施工数量错误");for(int i=0;i<w.war.structures.size();i++){War.Structure s=w.fieldworks.byId(d.readInt());require(s!=null&&seen.add(s.id),"施工设施引用错误");s.complete=d.readBoolean();s.builder=d.readInt();s.direction=d.readInt();}
    }
    static void migrate(World w){
        for(World.City c:w.cities)c.baseDefense=Math.max(3000,c.defense);
        for(int owner=0;owner<w.factions.length;owner++){
            EnumSet<Campaign.Tech> set=w.campaign.learned.getOrDefault(owner,EnumSet.noneOf(Campaign.Tech.class)).clone();
            for(Campaign.Project p:w.campaign.projects)if(p.owner==owner&&p.tech!=null)set.add(p.tech);
            if(!set.isEmpty())w.campaign.legacyTechs.put(owner,set);
        }
    }
    static void validate(World w)throws IOException{
        for(Map.Entry<Integer,EnumSet<Campaign.Tech>> e:w.campaign.legacyTechs.entrySet()){
            bound(e.getKey(),0,w.factions.length-1);for(Campaign.Tech t:e.getValue()){
                require(t.ordinal()<15,"非旧版技巧不能免除前置");boolean present=w.campaign.learned.getOrDefault(e.getKey(),EnumSet.noneOf(Campaign.Tech.class)).contains(t);
                for(Campaign.Project p:w.campaign.projects)if(p.owner==e.getKey()&&p.tech==t)present=true;
                require(present,"旧版技巧许可无对应项目");
            }
        }
        for(World.City c:w.cities){require(c.kind!=null,"据点类型缺失");bound(c.baseDefense,1,100000);}
        for(World.Unit u:w.units)bound(u.gold,0,10000);
        Set<Integer> builders=new HashSet<>();for(War.Structure s:w.war.structures){
            bound(s.direction,0,5);bound(s.builder,-1,9999999);require(s.complete||s.hp<s.kind.hp,"未完成设施却为满耐久");
            if(s.builder>=0){World.Unit u=w.unit(s.builder);require(u!=null&&u.owner==s.owner&&u.hex.distance(s.hex)==1&&s.hp<s.kind.hp&&builders.add(s.builder),"施工部队不存在、重复或远离工地");}
        }
    }
    private static int bound(int n,int lo,int hi)throws IOException{require(n>=lo&&n<=hi,"野战工程字段越界");return n;}
    private static void require(boolean ok,String message)throws IOException{if(!ok)throw new IOException(message);}
}
