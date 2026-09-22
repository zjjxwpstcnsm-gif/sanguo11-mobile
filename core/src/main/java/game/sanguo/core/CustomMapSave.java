package game.sanguo.core;

import java.io.*;

/** v33: immutable revision identity + administrative parents. The regular save already embeds
 * terrain, actual site positions/state, parcels and event regions. No external file is opened. */
final class CustomMapSave {
    static void write(World w,DataOutputStream d)throws IOException {d.writeInt(0x4d503637);d.writeUTF(w.customMapId);d.writeUTF(w.customMapName);d.writeInt(w.customMapRevision);d.writeUTF(w.customMapBase);d.writeUTF(w.customMapFingerprint);d.writeInt(w.siteParents.size());for(var e:w.siteParents.entrySet()){d.writeInt(e.getKey());d.writeInt(e.getValue());}}
    static void read(World w,DataInputStream d)throws IOException {if(d.readInt()!=0x4d503637)throw new IOException("自定义地图存档标记无效");w.customMapId=d.readUTF();w.customMapName=d.readUTF();w.customMapRevision=d.readInt();w.customMapBase=d.readUTF();w.customMapFingerprint=d.readUTF();int n=d.readInt();if(n<0||n>w.cities.size())throw new IOException("港关关联数量无效");for(int i=0;i<n;i++){int id=d.readInt(),parent=d.readInt();if(w.siteParents.put(id,parent)!=null)throw new IOException("港关关联重复");}validate(w);}
    static void validate(World w)throws IOException {
        if(w.customMapId.isEmpty()){if(w.customMapRevision!=0||!w.customMapName.isEmpty()||!w.customMapBase.isEmpty()||!w.customMapFingerprint.isEmpty())throw new IOException("原版地图不应带有自定义修订标记");}
        else if(!w.customMapId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")||w.customMapName.trim().isEmpty()||w.customMapName.length()>60||w.customMapRevision<1||w.customMapRevision>1000000||!w.customMapBase.matches("[0-9a-f]{64}")||!w.customMapFingerprint.matches("[0-9a-f]{64}"))throw new IOException("自定义地图身份字段错误");
        for(var e:w.siteParents.entrySet()){World.City site=w.city(e.getKey()),parent=w.city(e.getValue());if(site==null||site.kind==World.SiteKind.CITY||parent==null||parent.kind!=World.SiteKind.CITY)throw new IOException("存档港关地理关联无效");}
    }
}
