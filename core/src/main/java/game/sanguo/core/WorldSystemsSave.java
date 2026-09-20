package game.sanguo.core;

import java.io.*;
import java.util.*;

/** v13 append-only section. Older saves retain their existing RNG and default to no new random events. */
final class WorldSystemsSave {
    static final int MARKER=0x57535944;
    static void write(World w,DataOutputStream d)throws IOException{
        d.writeInt(MARKER);WorldEvents e=w.events;d.writeBoolean(e.enabled);d.writeLong(e.randomState);d.writeInt(e.nextCamp);d.writeInt(e.lastTick);
        d.writeInt(e.regions.size());for(Map.Entry<Integer,WorldEvents.Tribe> item:e.regions.entrySet()){d.writeInt(item.getKey());d.writeUTF(item.getValue().name());}
        d.writeInt(e.hazards.size());for(WorldEvents.Hazard h:e.hazards.values()){d.writeInt(h.city);d.writeUTF(h.kind.name());d.writeInt(h.until);}
        d.writeInt(e.camps.size());for(WorldEvents.Camp c:e.camps){d.writeInt(c.id);d.writeInt(c.city);d.writeUTF(c.tribe.name());d.writeInt(c.hex.q);d.writeInt(c.hex.r);d.writeInt(c.troops);}
        Districts g=w.districts;d.writeInt(g.nextId);d.writeInt(g.groups.size());
        for(Districts.District district:g.groups.values()){
            d.writeInt(district.id);d.writeInt(district.owner);d.writeUTF(district.name);d.writeUTF(district.policy.name());d.writeInt(district.leader);d.writeInt(district.target);d.writeInt(district.supply);d.writeInt(district.points);d.writeInt(district.actedTurn);d.writeBoolean(district.attack);d.writeBoolean(district.produce);
            d.writeInt(district.cities.size());for(int city:district.cities)d.writeInt(city);
        }
        d.writeInt(g.units.size());for(Map.Entry<Integer,Integer> item:g.units.entrySet()){d.writeInt(item.getKey());d.writeInt(item.getValue());}
        Contests.Session s=w.contests.session;d.writeBoolean(s!=null&&s.diplomatic());if(s!=null&&s.diplomatic()){d.writeUTF(s.treaty.name());d.writeInt(s.foreign);d.writeInt(s.duration);}
    }
    static void read(World w,DataInputStream d)throws IOException{
        require(d.readInt()==MARKER,"军团与事件扩展标记无效");WorldEvents e=w.events;e.enabled=d.readBoolean();e.randomState=d.readLong();e.nextCamp=d.readInt();e.lastTick=d.readInt();
        int n=count(d,w.cities.size());for(int i=0;i<n;i++){int id=d.readInt();require(e.regions.put(id,value(WorldEvents.Tribe.class,d.readUTF()))==null,"区域重复");}
        n=count(d,w.cities.size());for(int i=0;i<n;i++){int id=d.readInt();WorldEvents.Hazard h=new WorldEvents.Hazard(id,value(WorldEvents.Disaster.class,d.readUTF()),d.readInt());require(e.hazards.put(id,h)==null,"灾害重复");}
        n=count(d,w.cities.size());for(int i=0;i<n;i++)e.camps.add(new WorldEvents.Camp(d.readInt(),d.readInt(),value(WorldEvents.Tribe.class,d.readUTF()),new Hex(d.readInt(),d.readInt()),d.readInt()));
        Districts g=w.districts;g.nextId=d.readInt();n=count(d,7);
        for(int i=0;i<n;i++){
            Districts.District district=new Districts.District(d.readInt(),d.readInt(),d.readUTF());district.policy=value(Districts.Policy.class,d.readUTF());district.leader=d.readInt();district.target=d.readInt();district.supply=d.readInt();district.points=d.readInt();district.actedTurn=d.readInt();district.attack=d.readBoolean();district.produce=d.readBoolean();
            int members=count(d,w.cities.size());for(int j=0;j<members;j++)require(district.cities.add(d.readInt()),"军团据点重复");require(g.groups.put(district.id,district)==null,"军团编号重复");
        }
        n=count(d,w.units.size());for(int i=0;i<n;i++){int id=d.readInt();require(g.units.put(id,d.readInt())==null,"军团部队重复");}
        if(d.readBoolean()){Contests.Session s=w.contests.session;require(s!=null&&!s.isDuel(),"外交舌战缺少对局");s.treaty=value(Campaign.TreatyKind.class,d.readUTF());s.foreign=d.readInt();s.duration=d.readInt();}
    }
    static void validate(World w)throws IOException{
        WorldEvents e=w.events;range(e.nextCamp,1,10000000);range(e.lastTick,-1,w.turn);
        require(e.regions.size()<=w.cities.size()&&e.hazards.size()<=w.cities.size()&&e.camps.size()<=w.cities.size(),"事件记录过多");
        for(Map.Entry<Integer,WorldEvents.Tribe> item:e.regions.entrySet())require(w.city(item.getKey())!=null&&item.getValue()!=null&&item.getValue()!=WorldEvents.Tribe.BANDIT,"区域引用无效");
        for(WorldEvents.Hazard h:e.hazards.values()){require(w.city(h.city)!=null&&h.kind!=null,"灾害引用无效");range(h.until,w.turn+1,w.turn+3);}
        Set<Integer> ids=new HashSet<>(),cities=new HashSet<>();Set<Hex> cells=new HashSet<>();
        for(WorldEvents.Camp c:e.camps){
            range(c.id,1,e.nextCamp-1);range(c.troops,1,6000);World.City city=w.city(c.city);
            require(ids.add(c.id)&&cities.add(c.city)&&c.tribe!=null&&city!=null&&w.inside(c.hex)&&cells.add(c.hex)&&c.hex.distance(city.hex)>=3&&c.hex.distance(city.hex)<=4,"贼寨引用或位置无效");
            require(w.cost(c.hex,World.Weapon.SPEAR)>=0&&!w.army.water(c.hex)&&w.cityAt(c.hex)==null&&w.unitAt(c.hex)==null&&w.domestic.at(c.hex)==null&&w.war.at(c.hex)==null,"贼寨地块冲突");
        }
        Districts g=w.districts;range(g.nextId,1,10000000);require(g.groups.size()<=7,"军团数超过上限");cities.clear();
        for(Districts.District district:g.groups.values()){
            range(district.id,1,g.nextId-1);require(district.owner==w.player&&district.policy!=null&&district.name!=null&&!district.name.trim().isEmpty()&&district.name.length()<=30&&!district.cities.isEmpty(),"军团基本资料无效");range(district.points,0,60);range(district.actedTurn,-1,w.turn);
            for(int id:district.cities){World.City c=w.city(id);require(c!=null&&c.owner==district.owner&&cities.add(id),"军团据点归属冲突");}
            World.Officer leader=w.officer(district.leader);require(district.leader==-1||leader!=null&&leader.owner==district.owner&&!w.government.captive(leader.id)&&district.cities.contains(leader.cityId)&&leader.role!=Strategy.Role.RULER,"都督引用无效");
            if(district.policy==Districts.Policy.CITY_ATTACK)require(w.city(district.target)!=null,"军团目标据点无效");
            else if(district.policy==Districts.Policy.FORCE_ATTACK)range(district.target,0,w.factions.length-1);else require(district.target==-1,"不应存在攻略目标");
            require(district.supply==-1||w.city(district.supply)!=null&&w.city(district.supply).owner==district.owner,"军团运输目标无效");
        }
        for(World.Officer o:w.officers)if(o.owner==w.player&&o.role==Strategy.Role.RULER)require(!cities.contains(o.cityId),"君主驻地被委任");
        require(g.groups.isEmpty()||w.cities.stream().anyMatch(c->c.owner==w.player&&!cities.contains(c.id)),"第一军团无据点");
        for(Map.Entry<Integer,Integer> item:g.units.entrySet()){World.Unit u=w.unit(item.getKey());Districts.District district=g.get(item.getValue());require(u!=null&&district!=null&&u.owner==district.owner,"军团部队引用无效");}
        Contests.Session s=w.contests.session;if(s!=null){require(!s.isDuel()||!s.diplomatic(),"单挑不能具有外交目标");if(!s.diplomatic())require(s.foreign==-1&&s.duration==0,"非外交舌战具有外交字段");}
    }
    private static <E extends Enum<E>> E value(Class<E> kind,String name)throws IOException{try{return Enum.valueOf(kind,name);}catch(IllegalArgumentException ex){throw new IOException("未知军团或事件类型",ex);}}
    private static int count(DataInputStream d,int max)throws IOException{int n=d.readInt();range(n,0,max);return n;}
    private static void range(int n,int min,int max)throws IOException{require(n>=min&&n<=max,"军团或事件字段越界");}
    private static void require(boolean ok,String text)throws IOException{if(!ok)throw new IOException(text);}
}
