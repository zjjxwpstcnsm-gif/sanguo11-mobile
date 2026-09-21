package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Optional, length-delimited namespace tail. Unknown modules survive a load/save round trip. */
public final class SaveExtensions {
    private static final int MAGIC=0x53475831, MAX_TOTAL=16*1024*1024;
    private final SortedMap<String,byte[]> values=new TreeMap<>();
    public byte[] get(String namespace){byte[] v=values.get(namespace);return v==null?null:v.clone();}
    public void put(String namespace,byte[] value){
        if(namespace==null||!namespace.matches("[A-Za-z][A-Za-z0-9.-]{0,63}"))throw new IllegalArgumentException("存档扩展命名空间无效");
        if(value==null){values.remove(namespace);return;}
        int total=value.length;for(Map.Entry<String,byte[]> e:values.entrySet())if(!e.getKey().equals(namespace))total+=e.getValue().length;
        if(total>MAX_TOTAL||!values.containsKey(namespace)&&values.size()>=32)throw new IllegalArgumentException("存档扩展超过大小限制");values.put(namespace,value.clone());
    }
    void write(DataOutputStream out)throws IOException{
        if(values.isEmpty())return; // Vanilla v31/v32 files retain their exact original representation.
        out.writeInt(MAGIC);out.writeInt(values.size());
        for(Map.Entry<String,byte[]> e:values.entrySet()){out.writeUTF(e.getKey());out.writeInt(e.getValue().length);out.write(e.getValue());}
    }
    void read(DataInputStream in)throws IOException{
        if(in.available()==0)return;
        if(in.readInt()!=MAGIC)throw new IOException("未知存档扩展格式");int n=in.readInt();if(n<1||n>32)throw new IOException("扩展数量无效");
        int total=0;for(int i=0;i<n;i++){String key=in.readUTF();int size=in.readInt();if(size<0||size>MAX_TOTAL||size>in.available()||(total+=size)>MAX_TOTAL||values.containsKey(key))throw new IOException("扩展长度或命名空间无效");
            byte[] bytes=new byte[size];in.readFully(bytes);try{put(key,bytes);}catch(IllegalArgumentException e){throw new IOException(e.getMessage(),e);}}
    }
}
