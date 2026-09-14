package game.sanguo.core;

import java.io.*;
import java.util.*;

/** v6 adds equipment, six aptitudes, three-officer formations and production after the unchanged v5 sections. */
final class ArmySave {
    private static final int MARKER=0x41524D36;
    static void write(World w,DataOutputStream d)throws IOException {
        d.writeInt(MARKER);d.writeInt(w.cities.size());
        for(World.City c:w.cities){d.writeInt(c.id);for(int i=4;i<c.equipment.length;i++)d.writeInt(c.equipment[i]);for(int n:c.ships)d.writeInt(n);}
        d.writeInt(w.officers.size());for(World.Officer o:w.officers){d.writeInt(o.id);d.writeByte(o.aptitude[4]);d.writeByte(o.aptitude[5]);}
        d.writeInt(w.units.size());for(World.Unit u:w.units){d.writeInt(u.id);d.writeByte(u.ship.ordinal());d.writeByte(u.deputies.length);for(int id:u.deputies)d.writeInt(id);d.writeByte(u.burning);}
        d.writeInt(w.domestic.missions.size());for(Domestic.Mission m:w.domestic.missions){d.writeInt(m.id);for(int i=4;i<m.equipment.length;i++)d.writeInt(m.equipment[i]);}
        d.writeInt(w.army.productions.size());for(Army.Production p:w.army.productions){d.writeInt(p.cityId);d.writeInt(p.officerId);d.writeInt(p.owner);d.writeInt(p.weapon==null?-1:p.weapon.ordinal());d.writeInt(p.ship==null?-1:p.ship.ordinal());}
    }
    static void read(World w,DataInputStream d)throws IOException {
        require(d.readInt()==MARKER,"军备扩展标记错误");Set<Integer> ids=new HashSet<>();int n=count(d,1000,w.cities.size());
        for(int j=0;j<n;j++){int id=d.readInt();World.City c=w.city(id);require(c!=null&&ids.add(id),"军备城池引用重复或缺失");for(int i=4;i<c.equipment.length;i++)c.equipment[i]=d.readInt();for(int i=0;i<c.ships.length;i++)c.ships[i]=d.readInt();}
        ids.clear();n=count(d,10000,w.officers.size());for(int j=0;j<n;j++){int id=d.readInt();World.Officer o=w.officer(id);require(o!=null&&ids.add(id),"兵器水军适性引用重复或缺失");o.aptitude[4]=d.readUnsignedByte();o.aptitude[5]=d.readUnsignedByte();}
        ids.clear();n=count(d,10000,w.units.size());for(int j=0;j<n;j++){int id=d.readInt();World.Unit u=w.unit(id);require(u!=null&&ids.add(id),"编队引用重复或缺失");u.ship=Army.Ship.values()[bound(d.readUnsignedByte(),0,2)];u.deputies=new int[bound(d.readUnsignedByte(),0,2)];for(int i=0;i<u.deputies.length;i++)u.deputies[i]=d.readInt();u.burning=d.readUnsignedByte();}
        ids.clear();n=count(d,10000,w.domestic.missions.size());for(int j=0;j<n;j++){int id=d.readInt();Domestic.Mission m=w.domestic.mission(id);require(m!=null&&ids.add(id),"运输军备引用重复或缺失");for(int i=4;i<m.equipment.length;i++)m.equipment[i]=d.readInt();}
        n=bound(d.readInt(),0,10000);for(int i=0;i<n;i++){
            int city=d.readInt(),officer=d.readInt(),owner=d.readInt(),weapon=bound(d.readInt(),-1,World.Weapon.values().length-1),ship=bound(d.readInt(),-1,2);
            require((weapon<0)!=(ship<0),"制造种类错误");w.army.productions.add(new Army.Production(city,officer,owner,weapon<0?null:World.Weapon.values()[weapon],ship<0?null:Army.Ship.values()[ship]));
        }
    }
    static void validate(World w)throws IOException {
        for(World.City c:w.cities){require(c.equipment.length==World.Weapon.values().length&&c.ships.length==2,"军备数量错误");require(c.equipment[4]==0,"剑兵无需库存");for(int i=5;i<c.equipment.length;i++)bound(c.equipment[i],0,100);for(int n:c.ships)bound(n,0,100);}
        Set<Integer> assigned=new HashSet<>();for(World.Unit u:w.units){
            require(u.ship!=null&&u.deputies!=null&&u.deputies.length<=2&&assigned.add(u.officerId),"编队主将或舰船错误");bound(u.burning,0,2);
            for(int id:u.deputies){World.Officer o=w.officer(id);require(o!=null&&assigned.add(id)&&o.owner==u.owner&&o.unitId==u.id&&o.cityId==-1&&!w.domestic.busy(id)&&o.otherTaskTurns==0,"副将重复、位置或任务冲突");}
        }
        Set<Integer> workers=new HashSet<>();bound(w.army.productions.size(),0,10000);
        for(Army.Production p:w.army.productions){
            World.City c=w.city(p.cityId);World.Officer o=w.officer(p.officerId);require((p.weapon==null)!=(p.ship==null),"制造类型冲突");
            require(p.weapon==null?p.ship!=Army.Ship.BOAT:Army.siegeWeapon(p.weapon),"制造物品错误");
            require(c!=null&&o!=null&&workers.add(o.id)&&c.owner==p.owner&&o.owner==p.owner&&o.cityId==c.id&&o.unitId==-1&&o.otherTaskTurns>0&&o.otherTaskTurns<=3&&o.otherTask.equals(p.label())&&!w.domestic.busy(o.id),"制造人员位置或任务错误");
            require(w.campaign.projects.stream().noneMatch(x->x.officerId==o.id),"制造与研究任务冲突");
            require(w.domestic.facilities.stream().anyMatch(f->f.cityId==c.id&&f.kind==(p.weapon!=null?Domestic.Kind.WORKSHOP:Domestic.Kind.SHIPYARD)&&f.remaining==0),"制造工场不存在");
        }
        for(World.Officer o:w.officers)if(o.otherTask.startsWith("制造"))require(workers.contains(o.id),"制造任务缺失");
    }
    private static int count(DataInputStream d,int max,int expected)throws IOException {int n=bound(d.readInt(),0,max);require(n==expected,"军备扩展记录数量错误");return n;}
    private static int bound(int value,int min,int max)throws IOException {require(value>=min&&value<=max,"军备存档字段越界");return value;}
    private static void require(boolean valid,String message)throws IOException {if(!valid)throw new IOException(message);}
}
