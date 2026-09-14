package game.sanguo.core;

import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

/** Versioned, bounded save fields; CRC detects accidental damage, not hostile tampering. */
public final class SaveCodec {
    private static final int MAGIC=0x53473131, VERSION=7, MAX_BYTES=4*1024*1024;
    private SaveCodec() {}
    public static byte[] encode(World w) throws IOException {
        validate(w);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        DataOutputStream d=new DataOutputStream(bytes);
        d.writeInt(w.width);d.writeInt(w.height);d.writeInt(w.turn);d.writeInt(w.active);d.writeInt(w.nextUnitId);d.writeInt(w.winner);
        d.writeInt(w.factions.length);for(String faction:w.factions)d.writeUTF(faction);
        d.writeInt(w.player);d.writeInt(w.startYear);d.writeInt(w.startMonth);d.writeInt(w.dataRevision);
        d.writeUTF(w.scenarioId);d.writeUTF(w.scenarioName);d.writeUTF(w.dataSource);d.writeUTF(w.dataHash);
        for(int ap:w.actionPoints)d.writeInt(ap);
        for(World.Terrain[] row:w.terrain)for(World.Terrain t:row)d.writeByte(t.ordinal());
        d.writeInt(w.cities.size());
        for(World.City c:w.cities) {
            d.writeInt(c.id);d.writeUTF(c.name);hex(d,c.hex);d.writeInt(c.owner);
            d.writeInt(c.gold);d.writeInt(c.food);d.writeInt(c.troops);d.writeInt(c.order);d.writeInt(c.morale);d.writeInt(c.defense);
            for(int j=0;j<4;j++)d.writeInt(c.equipment[j]);
        }
        d.writeInt(w.officers.size());
        for(World.Officer o:w.officers) {
            d.writeInt(o.id);d.writeUTF(o.name);d.writeInt(o.owner);d.writeInt(o.cityId);d.writeInt(o.unitId);
            d.writeInt(o.leadership);d.writeInt(o.war);d.writeInt(o.intelligence);d.writeInt(o.politics);d.writeInt(o.charm);d.writeBoolean(o.acted);
        }
        d.writeInt(w.units.size());
        for(World.Unit u:w.units) {
            d.writeInt(u.id);d.writeInt(u.owner);d.writeInt(u.officerId);d.writeByte(u.weapon.ordinal());hex(d,u.hex);
            d.writeInt(u.troops);d.writeInt(u.food);d.writeInt(u.energy);d.writeBoolean(u.acted);
        }
        w.domestic.write(d);
        w.strategy.write(d);
        CampaignSave.write(w,d);
        ArmySave.write(w,d);
        RulesSave.write(w,d);
        d.writeInt(w.log.size());for(String line:w.log)d.writeUTF(line);
        d.flush();byte[] payload=bytes.toByteArray();
        if(payload.length>MAX_BYTES)throw new IOException("存档过大");
        CRC32 crc=new CRC32();crc.update(payload);
        bytes=new ByteArrayOutputStream();d=new DataOutputStream(bytes);
        d.writeInt(MAGIC);d.writeInt(VERSION);d.writeInt(payload.length);d.writeLong(crc.getValue());d.write(payload);d.flush();
        return bytes.toByteArray();
    }
    public static World decode(byte[] data) throws IOException {
        if(data==null||data.length<20||data.length>MAX_BYTES+20)throw new IOException("存档长度无效");
        DataInputStream d=new DataInputStream(new ByteArrayInputStream(data));
        if(d.readInt()!=MAGIC)throw new IOException("不是本项目存档");
        int version=d.readInt();if(version<1||version>VERSION)throw new IOException("存档版本不支持");
        int length=d.readInt();long expected=d.readLong();
        if(length!=data.length-20)throw new IOException("存档不完整");
        byte[] payload=new byte[length];d.readFully(payload);CRC32 crc=new CRC32();crc.update(payload);
        if(crc.getValue()!=expected)throw new IOException("存档校验失败");
        d=new DataInputStream(new ByteArrayInputStream(payload));
        int width=bounded(d.readInt(),1,128),height=bounded(d.readInt(),1,128);
        int turn=d.readInt(),active=d.readInt(),nextUnitId=d.readInt(),winner=d.readInt();
        String[] factions={"刘备军","曹操军"};
        if(version>=2){factions=new String[bounded(d.readInt(),2,32)];for(int i=0;i<factions.length;i++)factions[i]=d.readUTF();}
        World w=new World(width,height,factions);w.turn=turn;w.active=active;w.nextUnitId=nextUnitId;w.winner=winner;
        if(version>=2) {
            w.player=d.readInt();w.startYear=d.readInt();w.startMonth=d.readInt();w.dataRevision=d.readInt();
            w.scenarioId=d.readUTF();w.scenarioName=d.readUTF();w.dataSource=d.readUTF();w.dataHash=d.readUTF();
        }
        for(int i=0;i<factions.length;i++)w.actionPoints[i]=bounded(d.readInt(),0,60);
        for(int q=0;q<width;q++)for(int r=0;r<height;r++)w.terrain[q][r]=World.Terrain.values()[bounded(d.readUnsignedByte(),0,3)];
        int count=bounded(d.readInt(),1,1000);
        for(int i=0;i<count;i++) {
            int id=d.readInt();String name=d.readUTF();Hex h=hex(d);int owner=d.readInt();
            World.City c=new World.City(id,name,h,owner);
            c.gold=d.readInt();c.food=d.readInt();c.troops=d.readInt();c.order=d.readInt();c.morale=d.readInt();c.defense=d.readInt();
            for(int j=0;j<4;j++)c.equipment[j]=d.readInt();w.cities.add(c);
        }
        count=bounded(d.readInt(),0,10000);
        for(int i=0;i<count;i++) {
            int id=d.readInt();String name=d.readUTF();int owner=d.readInt(),city=d.readInt(),unit=d.readInt();
            World.Officer o=new World.Officer(id,name,owner,city,d.readInt(),d.readInt(),d.readInt(),d.readInt(),d.readInt());
            o.unitId=unit;o.acted=d.readBoolean();w.officers.add(o);
        }
        count=bounded(d.readInt(),0,10000);
        for(int i=0;i<count;i++) {
            int id=d.readInt(),owner=d.readInt(),officer=d.readInt();
            World.Weapon weapon=World.Weapon.values()[bounded(d.readUnsignedByte(),0,version>=6?World.Weapon.values().length-1:3)];
            World.Unit u=new World.Unit(id,owner,officer,weapon,hex(d),d.readInt(),d.readInt());
            u.energy=d.readInt();u.acted=d.readBoolean();w.units.add(u);
        }
        if(version>=3)w.domestic.read(d);
        if(version>=4)w.strategy.read(d);else w.strategy.initializeOffices();
        if(version>=5)CampaignSave.read(w,d);
        if(version>=6)ArmySave.read(w,d);
        if(version>=7)RulesSave.read(w,d);
        count=bounded(d.readInt(),0,40);for(int i=0;i<count;i++)w.log.add(d.readUTF());
        if(d.available()!=0)throw new IOException("存档存在未知尾部数据");
        validate(w);return w;
    }
    private static void hex(DataOutputStream d,Hex h)throws IOException { d.writeInt(h.q);d.writeInt(h.r); }
    private static Hex hex(DataInputStream d)throws IOException { return new Hex(d.readInt(),d.readInt()); }
    private static int bounded(int n,int min,int max)throws IOException { if(n<min||n>max)throw new IOException("存档字段越界");return n; }
    private static void require(boolean ok,String message)throws IOException { if(!ok)throw new IOException(message); }
    public static void validate(World w)throws IOException {
        bounded(w.width,1,128);bounded(w.height,1,128);bounded(w.factions.length,2,32);
        bounded(w.active,0,w.factions.length-1);bounded(w.player,0,w.factions.length-1);bounded(w.turn,0,100000);bounded(w.winner,-1,w.factions.length-1);
        require(w.actionPoints.length==w.factions.length,"势力行动力缺失");
        Set<String> factionNames=new HashSet<>();for(String name:w.factions){label(name,100);require(factionNames.add(name),"势力名称重复");}
        bounded(w.startYear,1,9999);bounded(w.startMonth,1,12);bounded(w.dataRevision,1,1000000);
        label(w.scenarioId,80);label(w.scenarioName,100);label(w.dataSource,500);
        require(w.dataHash!=null&&(w.dataHash.isEmpty()||w.dataHash.matches("[0-9a-f]{64}")),"数据指纹无效");
        bounded(w.nextUnitId,1,10000000);for(int ap:w.actionPoints)bounded(ap,0,60);
        require(!w.cities.isEmpty()&&w.cities.size()<=1000&&w.officers.size()<=10000&&w.units.size()<=10000&&w.log.size()<=40,"记录数量无效");
        for(World.Terrain[] row:w.terrain)for(World.Terrain t:row)require(t!=null,"地形缺失");
        Set<Integer> ids=new HashSet<>();Set<Hex> occupied=new HashSet<>();
        for(World.City c:w.cities) {
            require(ids.add(c.id)&&c.id>=0,"城池ID重复或无效");require(c.name!=null&&!c.name.isEmpty()&&c.name.length()<=100,"城池名无效");
            require(w.inside(c.hex)&&occupied.add(c.hex)&&w.cost(c.hex,World.Weapon.SPEAR)>0,"城池位置冲突或不可通行");bounded(c.owner,-1,w.factions.length-1);
            bounded(c.gold,0,1000000);bounded(c.food,0,1000000);bounded(c.troops,0,100000);bounded(c.order,0,100);bounded(c.morale,0,100);bounded(c.defense,1,100000);
            for(int amount:c.equipment)bounded(amount,0,100000);
        }
        ids.clear();
        for(World.Officer o:w.officers) {
            require(ids.add(o.id)&&o.id>=0,"武将ID重复或无效");require(o.name!=null&&!o.name.isEmpty()&&o.name.length()<=100,"武将名无效");bounded(o.owner,-1,w.factions.length-1);
            bounded(o.leadership,0,100);bounded(o.war,0,100);bounded(o.intelligence,0,100);bounded(o.politics,0,100);bounded(o.charm,0,100);
            require(o.cityId>=-1&&o.unitId>=-1,"武将驻地无效");
            if(o.cityId>=0)require(o.unitId==-1&&w.city(o.cityId)!=null&&(o.owner==-1||w.city(o.cityId).owner==o.owner),"武将城池归属错误");
            if(o.unitId>=0)require(o.cityId==-1&&w.unit(o.unitId)!=null&&w.army.contains(w.unit(o.unitId),o.id),"武将部队引用错误");
        }
        ids.clear();Set<Integer> assigned=new HashSet<>();
        for(World.Unit u:w.units) {
            require(ids.add(u.id)&&u.id>0&&u.id<w.nextUnitId,"部队ID重复或无效");bounded(u.owner,0,w.factions.length-1);
            require(u.weapon!=null&&w.inside(u.hex)&&(w.cost(u.hex,u.weapon)>0||w.army.water(u.hex))&&occupied.add(u.hex),"部队位置冲突或不可通行");
            require(assigned.add(u.officerId),"武将重复带队");World.Officer o=w.officer(u.officerId);
            require(o!=null&&o.owner==u.owner&&o.unitId==u.id&&o.cityId==-1,"部队武将引用错误");
            bounded(u.troops,1,10000);bounded(u.food,0,1000000);bounded(u.energy,0,100);
        }
        w.domestic.validate();
        w.strategy.validate();
        CampaignSave.validate(w);
        ArmySave.validate(w);
        RulesSave.validate(w);
        for(String line:w.log)label(line,2000);
        if(w.winner>=0) {
            require(w.alive(w.winner),"胜者势力不存在");
            for(int side=0;side<w.factions.length;side++)if(side!=w.winner)require(!w.alive(side),"仍有敌对势力，不能判定获胜");
        }
    }
    private static void label(String value,int max)throws IOException {require(value!=null&&!value.trim().isEmpty()&&value.length()<=max,"文本字段无效");}
}
