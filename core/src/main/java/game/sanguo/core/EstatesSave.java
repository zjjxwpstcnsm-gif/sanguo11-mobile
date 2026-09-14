package game.sanguo.core;

import java.io.*;
import java.util.*;

/** v12 stores item definitions and ownership without requiring an updated external catalog. */
final class EstatesSave {
    private static final int MARKER=0x45533132;
    static void write(World w,DataOutputStream d)throws IOException{
        d.writeInt(MARKER);d.writeBoolean(w.editor.edited);d.writeInt(w.editor.revision);ids(d,w.editor.customOfficers);
        d.writeInt(w.relations.people.size());
        for(Map.Entry<Integer,Relations.Person> e:w.relations.people.entrySet()){
            d.writeInt(e.getKey());Relations.Person p=e.getValue();d.writeInt(p.father);d.writeInt(p.mother);d.writeInt(p.spouse);ids(d,p.sworn);ids(d,p.likes);ids(d,p.dislikes);
        }
        d.writeInt(w.treasures.items.size());for(Treasures.Item i:w.treasures.items.values()){
            Treasures.Definition a=i.definition;d.writeUTF(a.id);d.writeUTF(a.name);d.writeUTF(a.kind.name());d.writeInt(a.value);d.writeUTF(i.place.name());d.writeInt(i.holder);
        }
    }
    private static void ids(DataOutputStream d,Collection<Integer> ids)throws IOException{d.writeInt(ids.size());for(int id:ids)d.writeInt(id);}
    private static void ids(DataInputStream d,Set<Integer> ids,int limit)throws IOException{int n=count(d,limit);for(int i=0;i<n;i++)if(!ids.add(d.readInt()))throw new IOException("重复人物关系或新武将ID");}
    static void read(World w,DataInputStream d)throws IOException{
        if(d.readInt()!=MARKER)throw new IOException("人物宝物扩展无效");w.editor.edited=d.readBoolean();w.editor.revision=d.readInt();ids(d,w.editor.customOfficers,10000);
        int n=count(d,10000);for(int i=0;i<n;i++){
            int id=d.readInt();Relations.Person p=new Relations.Person();if(w.relations.people.put(id,p)!=null)throw new IOException("重复关系武将");
            p.father=d.readInt();p.mother=d.readInt();p.spouse=d.readInt();if(p.father< -1||p.mother< -1||p.spouse< -1)throw new IOException("关系ID无效");ids(d,p.sworn,2);ids(d,p.likes,5);ids(d,p.dislikes,5);
        }
        n=count(d,43);for(int i=0;i<n;i++)try{
            Treasures.Definition a=new Treasures.Definition(d.readUTF(),d.readUTF(),Treasures.Kind.valueOf(d.readUTF()),d.readInt());
            Treasures.Item item=new Treasures.Item(a,Treasures.Place.valueOf(d.readUTF()),d.readInt());if(w.treasures.items.put(a.id,item)!=null)throw new IOException("重复宝物ID");
        }catch(IllegalArgumentException e){throw new IOException("宝物类别或位置错误",e);}
    }
    private static int count(DataInputStream d,int max)throws IOException{int n=d.readInt();if(n<0||n>max)throw new IOException("人物宝物记录超限");return n;}
    static void validate(World w)throws IOException{w.relations.validate();w.treasures.validate();w.editor.validate();}
}
