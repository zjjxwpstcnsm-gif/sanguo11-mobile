package game.sanguo.core;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/** Durable revision high-water marks survive library deletion and interrupted publication.
 * Allocation is persisted before writing a revision file; activation is persisted afterwards.
 * A failed publication can leave a gap, never a reused immutable (map ID, revision) identity. */
public final class MapRevisionLedger {
    public static final int MAX_REVISION=1000000,MAX_BYTES=1024*1024;
    private static final String HEADER="sanguo-map-revisions-v1";
    private record State(int highest,boolean deleted){}
    private final SortedMap<String,State> maps=new TreeMap<>();
    public int highest(String id){State s=maps.get(id);return s==null?0:s.highest;}
    public boolean deleted(String id){State s=maps.get(id);return s!=null&&s.deleted;}
    public static boolean validId(String id){return id!=null&&id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");}
    private static void id(String id)throws IOException {if(!validId(id))throw new IOException("地图ID无效");}
    private static void revision(int value)throws IOException {if(value<1||value>MAX_REVISION)throw new IOException("地图修订必须在1—1000000之间");}

    /** Persists as hidden for a brand-new map until complete() commits publication. */
    public int reserve(String id,int requested,int observedMaximum)throws IOException {
        id(id);revision(requested);if(observedMaximum<0||observedMaximum>MAX_REVISION)throw new IOException("已存修订号无效");
        int prior=Math.max(highest(id),observedMaximum);
        if(prior==MAX_REVISION)throw new IOException("修订编号已用尽；请另存为新的地图ID");
        int next=Math.max(prior+1,requested);
        boolean hidden=maps.containsKey(id)?deleted(id):observedMaximum==0;
        maps.put(id,new State(next,hidden));return next;
    }
    public void complete(String id,int revision)throws IOException {
        id(id);revision(revision);if(revision!=highest(id))throw new IOException("地图修订未预留");
        maps.put(id,new State(highest(id),false));
    }
    public void hide(String id,int observedMaximum)throws IOException {
        id(id);if(observedMaximum<0||observedMaximum>MAX_REVISION)throw new IOException("已存修订号无效");int high=Math.max(highest(id),observedMaximum);revision(high);maps.put(id,new State(high,true));
    }
    public byte[] encode()throws IOException {
        StringBuilder s=new StringBuilder(HEADER).append('\n');
        for(var e:maps.entrySet())s.append(e.getKey()).append('=').append(e.getValue().highest).append(',').append(e.getValue().deleted?'D':'A').append('\n');
        byte[] bytes=s.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)throw new IOException("修订索引过大");return bytes;
    }
    public static MapRevisionLedger decode(byte[] bytes)throws IOException {
        if(bytes.length>MAX_BYTES)throw new IOException("修订索引过大");String text;
        try{text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}catch(CharacterCodingException e){throw new IOException("修订索引UTF-8损坏，原文件保留",e);}
        String[] lines=text.split("\\n",-1);if(lines.length<2||!HEADER.equals(lines[0])||!lines[lines.length-1].isEmpty())throw new IOException("修订索引格式错误或写入不完整");
        MapRevisionLedger out=new MapRevisionLedger();
        for(int i=1;i<lines.length-1;i++){
            String[] parts=lines[i].split("[=,]",-1);if(parts.length!=3)throw new IOException("修订索引行错误");id(parts[0]);int r;
            try{r=Integer.parseInt(parts[1]);}catch(NumberFormatException e){throw new IOException("修订索引编号错误",e);}revision(r);
            if(!parts[2].equals("A")&&!parts[2].equals("D"))throw new IOException("修订索引状态错误");
            if(out.maps.put(parts[0],new State(r,parts[2].equals("D")))!=null)throw new IOException("修订索引ID重复");
        }
        return out;
    }
}
