package game.sanguo.core;
import java.io.*;
/** Explicit saved settlement capability. Format3 is selected only by the
 * new-source factory overload; historical format1 is never adopted at load. */
final class PcDuelCampaignPolicy {
    static final String NAMESPACE="pc-duel-campaign-settlement-v1";
    private static final int MAGIC=0x50445531;
    static final class State {int version,last;PcDuelOptions options;}
    static boolean enabled(World w){byte[]b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&java.nio.ByteBuffer.wrap(b).getInt()==MAGIC;}
    static State read(World w)throws IOException {
        byte[]raw=w.extensions.get(NAMESPACE);if(!enabled(w)||raw.length>2048)throw new IOException("原单挑战役终局策略尚未启用");var source=PcScenarioIdentity.saved(w);var d=new DataInputStream(new ByteArrayInputStream(raw));State s=new State();
        if(d.readInt()!=MAGIC)throw new IOException("原单挑策略格式无效");s.version=d.readInt();if((s.version<1||s.version>3)||source==null||!d.readUTF().equals(source.scenarioId)||!d.readUTF().equals(source.sha)||!d.readUTF().equals(source.sourceVariant))throw new IOException("原单挑战役终局保存来源未知");s.last=d.readInt();
        if(s.version>=2){try{s.options=new PcDuelOptions(d.readInt(),d.readInt(),d.readInt());}catch(IllegalArgumentException e){throw new IOException(e);}if(d.readInt()!=1)throw new IOException("原单挑命令聚焦呈现策略未知");}
        if(s.last< -1||s.last>=w.contests.nextId||d.available()!=0||!PcDuelHealthPolicy.enabled(w)||!PcNativeItemPolicy.enabled(w)||!PcNativeDebatePolicy.enabled(w))throw new IOException("原单挑战役终局保存策略无效");return s;
    }
    static int lastFinished(World w)throws IOException{return read(w).last;}
    static PcDuelOptions options(World w)throws IOException{return enabled(w)?read(w).options:null;}
    private static void write(World w,State s)throws IOException {var source=PcScenarioIdentity.saved(w);var b=new ByteArrayOutputStream();var d=new DataOutputStream(b);d.writeInt(MAGIC);d.writeInt(s.version);d.writeUTF(source.scenarioId);d.writeUTF(source.sha);d.writeUTF(source.sourceVariant);d.writeInt(s.last);if(s.version>=2){d.writeInt(s.options.life);d.writeInt(s.options.death);d.writeInt(s.options.difficulty);d.writeInt(1);}w.extensions.put(NAMESPACE,b.toByteArray());}
    static void initializeOpening(World w,PcDuelOptions options)throws IOException {
        if(options==null||!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null)throw new IOException("原单挑战役策略只可显式创建新局");State s=new State();s.version=3;s.last=-1;s.options=options;write(w,s);read(w);PcLoyaltyProperty23Policy.initializeOpening(w);PcDuelAiActorPolicy.initializeOpening(w);PcDuelHumanActorPolicy.initializeOpening(w);PcDuelRecruitItemPolicy.initializeOpening(w);PcDuelPhysicalRecoveryPolicy.initializeOpening(w);
    }
    static void recordFinished(World w,int id)throws IOException {State s=read(w);if(id<=s.last||id<0||id>=w.contests.nextId)throw new IOException("原单挑终局已结算或编号无效");s.last=id;write(w,s);}
    static void validate(World w)throws IOException {if(enabled(w)){var s=read(w);if(s.version==3&&PcGovernorPolicy.data(w).format<2)throw new IOException("新单挑策略与当前军团保存版本不同");}PcDuelAiActorPolicy.validate(w);PcDuelHumanActorPolicy.validate(w);PcDuelRecruitItemPolicy.validate(w);PcDuelPhysicalRecoveryPolicy.validate(w);}
    private PcDuelCampaignPolicy(){}
}
