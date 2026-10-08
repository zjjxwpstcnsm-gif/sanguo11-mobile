package game.sanguo.core;
import java.io.*;import java.util.*;

/** Raw byteAC is independent of capped property23. Only original proven
 * writes establish a new raw value; engineering mutations invalidate trust. */
final class PcDuelRawLoyalty {
    static final String NAMESPACE="pc-duel-raw-loyalty-v1";
    private static final int MAGIC=0x50444c31;
    static final class Row {final int id,nativeId;int owner,display,raw;boolean trusted;Row(int id,int nativeId,int owner,int display,int raw,boolean trusted){this.id=id;this.nativeId=nativeId;this.owner=owner;this.display=display;this.raw=raw;this.trusted=trusted;}}
    static boolean enabled(World w){byte[]b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&java.nio.ByteBuffer.wrap(b).getInt()==MAGIC;}
    static void initializeOpening(World w)throws IOException {
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null)throw new IOException("原始忠诚只能在明确新局建立");
        var source=PcDuelSourceFacts.saved(w);var runtime=PcDuelRuntimeFacts.saved(w);if(source.size()!=670||runtime==null)throw new IOException("原始忠诚人物来源缺失");
        SortedMap<Integer,Row>rows=new TreeMap<>();for(var f:source.values()){var o=w.officer(f.officerId);var p=runtime.people.get(f.nativeId);if(o==null||p==null)throw new IOException("原始忠诚连接缺失");rows.put(o.id,new Row(o.id,f.nativeId,o.owner,o.loyalty,p.rawLoyalty,true));}write(w,rows);
    }
    private static SortedMap<Integer,Row>read(World w)throws IOException {
        byte[]b=w.extensions.get(NAMESPACE);if(!enabled(w)||b.length>32768)throw new IOException("当前保存没有原始忠诚策略");var d=new DataInputStream(new ByteArrayInputStream(b));var source=PcScenarioIdentity.saved(w);var facts=PcDuelSourceFacts.saved(w);
        if(d.readInt()!=MAGIC||d.readInt()!=1||source==null||!d.readUTF().equals(PcScenarioIdentity.EXE_SHA)||!d.readUTF().equals(source.scenarioId)||!d.readUTF().equals(source.sha)||!d.readUTF().equals(source.sourceVariant)||d.readInt()!=670)throw new IOException("原始忠诚保存来源不同");
        SortedMap<Integer,Row>rows=new TreeMap<>();for(int i=0;i<670;i++){int id=d.readInt(),nativeId=d.readInt(),owner=d.readInt(),display=d.readInt(),raw=d.readUnsignedByte();int flag=d.readUnsignedByte();if(flag>1)throw new IOException("Raw loyalty trust flag invalid");boolean trusted=flag==1;var f=facts.get(id);var o=w.officer(id);if(f==null||f.nativeId!=nativeId||o==null||owner< -1||owner>=w.factions.length||display<0||display>100||rows.put(id,new Row(id,nativeId,owner,display,raw,trusted))!=null||trusted&&(owner!=o.owner||display!=o.loyalty))throw new IOException("原始忠诚人物或已知状态不同");}if(d.available()!=0)throw new IOException("原始忠诚保存尾部未知");return rows;
    }
    private static void write(World w,SortedMap<Integer,Row>rows)throws IOException {
        var source=PcScenarioIdentity.saved(w);if(source==null||rows.size()!=670)throw new IOException("原始忠诚覆盖缺失");var b=new ByteArrayOutputStream();var d=new DataOutputStream(b);d.writeInt(MAGIC);d.writeInt(1);d.writeUTF(PcScenarioIdentity.EXE_SHA);d.writeUTF(source.scenarioId);d.writeUTF(source.sha);d.writeUTF(source.sourceVariant);d.writeInt(rows.size());for(Row r:rows.values()){d.writeInt(r.id);d.writeInt(r.nativeId);d.writeInt(r.owner);d.writeInt(r.display);d.writeByte(r.raw);d.writeBoolean(r.trusted);}w.extensions.put(NAMESPACE,b.toByteArray());
    }
    static int current(World w,int id)throws IOException {
        Row r=read(w).get(id);if(r==null)throw new IOException("当前原始忠诚人物连接缺失");if(r.trusted)return r.raw;
        if(!PcLoyaltyProperty23Policy.enabled(w))throw new IOException("当前原始忠诚变更尚未核实");
        PcLoyaltyProperty23Policy.validate(w);var o=w.officer(id);
        if(o==null||r.owner!=o.owner)throw new IOException("当前归属变更的忠诚输入未核实");
        // Pure inverse of the declared current display property. Never persist
        // an inferred byte or restore the old trusted flag; saturated100 rejects.
        return PcLoyaltyProperty23Policy.uniqueInput(o.loyalty);
    }
    /** Call before even a capped/no-op engineering write; raw120 cannot be
     * assumed unchanged just because the visible value remains100. */
    static void invalidate(World w,int id){if(!enabled(w))return;try{var rows=read(w);Row r=rows.get(id);if(r!=null&&r.trusted){r.trusted=false;write(w,rows);}}catch(IOException e){throw new IllegalStateException(e);}}
    /** Called only after an independently verified original raw setter result. */
    static void originalWrite(World w,int id,int raw)throws IOException {if(!enabled(w))return;if(raw<0||raw>255)throw new IOException("已核实原忠诚写入范围无效");var rows=read(w);var r=rows.get(id);var o=w.officer(id);if(r==null||o==null||o.loyalty!=Math.min(100,raw))throw new IOException("原忠诚写入与当前显示不同");r.raw=raw;r.display=o.loyalty;r.owner=o.owner;r.trusted=true;write(w,rows);}
    static void validate(World w)throws IOException {if(enabled(w))read(w);PcLoyaltyProperty23Policy.validate(w);}
    private PcDuelRawLoyalty(){}
}
