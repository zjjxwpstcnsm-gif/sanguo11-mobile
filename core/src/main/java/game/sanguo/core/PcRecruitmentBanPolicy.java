package game.sanguo.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Native58/59 temporary refusal, distinct from permanent dislike and raw
 * loyalty. Fresh initializer only; old missing/opaque strategies stay inert. */
final class PcRecruitmentBanPolicy {
    static final String NAMESPACE="pc-recruitment-ban-v1";
    private static final int MAGIC=0x50424e31;
    private static final String RESOURCE_SHA="fcd40647848c6492ab45b5b000eee8f7cbe6679eb5e068ddeb7903a43f8c0d32";
    static final class Row {
        final int id,nativeId;int ruler,months;boolean known;
        Row(int id,int nativeId,int ruler,int months,boolean known){this.id=id;this.nativeId=nativeId;this.ruler=ruler;this.months=months;this.known=known;}
    }
    static boolean enabled(World w){byte[]b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&java.nio.ByteBuffer.wrap(b).getInt()==MAGIC;}
    static void initializeOpening(World w)throws IOException {
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null)throw new IOException("原登用拒绝仅能在明确新局建立");var source=PcScenarioIdentity.saved(w);var facts=PcDuelSourceFacts.saved(w);if(source==null||facts.size()!=670)throw new IOException("原登用拒绝人物来源缺失");byte[]raw;
        try(var in=PcRecruitmentBanPolicy.class.getResourceAsStream("/pc-duel/recruitment-bans.tsv");var b=new ByteArrayOutputStream()){if(in==null)throw new IOException("原拒绝登用初值缺失");byte[]buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(b.size()+n>8*1024*1024)throw new IOException("原拒绝登用资源过大");b.write(buffer,0,n);}raw=b.toByteArray();}
        try{if(!RESOURCE_SHA.equals(PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(raw))))throw new IOException("原拒绝登用资源SHA不同");}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
        Map<Integer,PcDuelSourceFacts.Fact>byNative=new HashMap<>();for(var f:facts.values())byNative.put(f.nativeId,f);SortedMap<Integer,Row>rows=new TreeMap<>();int count=0;
        for(String line:new String(raw,StandardCharsets.UTF_8).split("\n")){if(line.startsWith("#"))continue;String[]p=line.split("\t",-1);if(p.length!=8)throw new IOException("原拒绝登用资源列数不同");if(!p[1].equals(source.scenarioId))continue;count++;if(!p[2].equals(source.sha)||!p[3].equals(source.sourceVariant))throw new IOException("原拒绝登用来源不同");int nativeId=Integer.parseInt(p[4]);var f=byNative.get(nativeId);if(f==null)continue;if(!f.recordSha.equals(p[5])||rows.put(f.officerId,new Row(f.officerId,nativeId,Integer.parseInt(p[6]),Integer.parseInt(p[7]),true))!=null)throw new IOException("原拒绝登用人物记录连接不同");}
        if(count!=850||rows.size()!=670)throw new IOException("原拒绝登用来源覆盖不足");write(w,rows);read(w);
    }
    private static SortedMap<Integer,Row>read(World w)throws IOException {
        byte[]raw=w.extensions.get(NAMESPACE);if(!enabled(w)||raw.length>32768)throw new IOException("当前保存没有原拒绝登用策略");var in=new DataInputStream(new ByteArrayInputStream(raw));var source=PcScenarioIdentity.saved(w);var facts=PcDuelSourceFacts.saved(w);if(in.readInt()!=MAGIC||in.readInt()!=1||source==null||!in.readUTF().equals(PcScenarioIdentity.EXE_SHA)||!in.readUTF().equals(source.scenarioId)||!in.readUTF().equals(source.sha)||!in.readUTF().equals(source.sourceVariant)||in.readInt()!=670)throw new IOException("原拒绝登用保存来源不同");SortedMap<Integer,Row>rows=new TreeMap<>();
        for(int n=0;n<670;n++){int id=in.readInt(),nativeId=in.readInt(),ruler=in.readInt(),months=in.readUnsignedByte(),flag=in.readUnsignedByte();var f=facts.get(id);if(f==null||f.nativeId!=nativeId||ruler< -1||ruler>=1100||flag>1||rows.put(id,new Row(id,nativeId,ruler,months,flag!=0))!=null)throw new IOException("原拒绝登用保存人物或字段无效");}if(in.available()!=0)throw new IOException("原拒绝登用保存尾部未知");return rows;
    }
    private static void write(World w,SortedMap<Integer,Row>rows)throws IOException {
        var source=PcScenarioIdentity.saved(w);if(source==null||rows.size()!=670)throw new IOException("原拒绝登用保存覆盖不足");var b=new ByteArrayOutputStream();var d=new DataOutputStream(b);d.writeInt(MAGIC);d.writeInt(1);d.writeUTF(PcScenarioIdentity.EXE_SHA);d.writeUTF(source.scenarioId);d.writeUTF(source.sha);d.writeUTF(source.sourceVariant);d.writeInt(rows.size());for(var r:rows.values()){if(r.ruler< -1||r.ruler>=1100||r.months<0||r.months>255)throw new IOException("原拒绝登用写入范围无效");d.writeInt(r.id);d.writeInt(r.nativeId);d.writeInt(r.ruler);d.writeByte(r.months);d.writeBoolean(r.known);}w.extensions.put(NAMESPACE,b.toByteArray());
    }
    static Row current(World w,int id)throws IOException {var r=read(w).get(id);if(r==null||!r.known)throw new IOException("当前拒绝登用变更尚未核实");return r;}
    static void invalidate(World w,int id){if(!enabled(w))return;try{var rows=read(w);var r=rows.get(id);if(r!=null&&r.known){r.known=false;write(w,rows);}}catch(IOException e){throw new IllegalStateException(e);}}
    /** Only independently proven original setters may establish current facts. */
    static void originalWrite(World w,int id,int ruler,int months)throws IOException {if(!enabled(w))return;var rows=read(w);var r=rows.get(id);if(r==null)throw new IOException("原拒绝登用人物缺失");r.ruler=ruler;r.months=months;r.known=true;write(w,rows);}
    /** Refusal part only of58bb30; status5 captive counter is separate. */
    static void monthly(Row r,boolean personAllowed,boolean rulerAllowed){
        if(!r.known||!personAllowed||r.months<=0)return;
        if(!rulerAllowed||--r.months==0){r.ruler=-1;r.months=0;}
    }
    private static boolean allowed(World w,int id){var state=w.life.state(id);return state==Lifecycle.State.ACTIVE||state==Lifecycle.State.UNDISCOVERED;}
    static void tick(World w)throws IOException {
        if(!enabled(w)||w.turn%3!=0)return;var rows=read(w);var facts=PcDuelSourceFacts.saved(w);Map<Integer,Integer>byNative=new HashMap<>();for(var f:facts.values())byNative.put(f.nativeId,f.officerId);
        for(var r:rows.values()){if(!r.known||r.months==0||!allowed(w,r.id))continue;Integer target=byNative.get(r.ruler);if(target==null)throw new IOException("原拒绝登用到期目标人物尚未覆盖");monthly(r,true,allowed(w,target));}write(w,rows);
    }
    static void validate(World w)throws IOException {if(enabled(w))read(w);}
    private PcRecruitmentBanPolicy(){}
}
