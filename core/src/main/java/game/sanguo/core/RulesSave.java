package game.sanguo.core;

import java.io.*;
import java.util.*;

/** v7 runtime extension. Unknown stable skill IDs survive content pack removal and updates. */
final class RulesSave {
    private static final int MARKER=0x52554C37;
    static void write(World w,DataOutputStream d)throws IOException {
        d.writeInt(MARKER);d.writeInt(w.units.size());
        for(World.Unit u:w.units){d.writeInt(u.id);d.writeInt(u.movementBudget);d.writeInt(u.movementSpent);d.writeByte(u.burningPower);}
        d.writeInt(w.war.fires.size());for(War.Fire f:w.war.fires){d.writeInt(f.hex.q);d.writeInt(f.hex.r);d.writeByte(f.power);d.writeBoolean(f.trap);}
        d.writeInt(w.officers.size());
        for(World.Officer o:w.officers){d.writeInt(o.id);d.writeUTF(o.skillId);d.writeByte(o.sex.ordinal());}
    }
    static void read(World w,DataInputStream d)throws IOException {
        require(d.readInt()==MARKER,"规则扩展标记错误");Set<Integer> ids=new HashSet<>();
        require(d.readInt()==w.units.size(),"行动记录数量错误");
        for(int i=0;i<w.units.size();i++){
            int id=d.readInt();World.Unit u=w.unit(id);require(u!=null&&ids.add(id),"行动引用重复或缺失");
            u.movementBudget=d.readInt();u.movementSpent=d.readInt();u.burningPower=d.readUnsignedByte();
        }
        Set<Hex> fires=new HashSet<>();require(d.readInt()==w.war.fires.size(),"火源记录数量错误");
        for(int i=0;i<w.war.fires.size();i++){Hex h=new Hex(d.readInt(),d.readInt());War.Fire f=w.war.fireAt(h);require(f!=null&&fires.add(h),"火源引用重复或缺失");f.power=d.readUnsignedByte();f.trap=d.readBoolean();}
        ids.clear();require(d.readInt()==w.officers.size(),"特技记录数量错误");
        for(int i=0;i<w.officers.size();i++){
            int id=d.readInt();World.Officer o=w.officer(id);require(o!=null&&ids.add(id),"特技引用重复或缺失");
            o.skillId=d.readUTF();int sex=d.readUnsignedByte();require(sex<World.Sex.values().length,"性别无效");o.sex=World.Sex.values()[sex];
        }
    }
    static void validate(World w)throws IOException {
        for(World.Unit u:w.units)require(u.movementBudget>=-1&&u.movementBudget<=1000&&u.movementSpent>=0&&
            (u.movementBudget==-1?u.movementSpent==0:u.movementSpent<=u.movementBudget),"部队移动预算无效");
        for(World.Unit u:w.units)require(u.burningPower>=1&&u.burningPower<=2&&(u.burning>0||u.burningPower==1),"着火来源无效");
        for(War.Fire f:w.war.fires)require(f.power>=1&&f.power<=2,"火场来源无效");
        for(World.Officer o:w.officers)require(o.skillId!=null&&o.skillId.matches("[a-z0-9][a-z0-9._-]{0,79}")&&o.sex!=null,"特技标识或性别无效");
    }
    private static void require(boolean value,String message)throws IOException{if(!value)throw new IOException(message);}
}
