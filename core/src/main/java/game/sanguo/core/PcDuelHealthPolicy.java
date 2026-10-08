package game.sanguo.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/** Mutable physical health. Initial source facts stay immutable. Missing old
 * policies are never inferred from injury, psychological health or a catalog. */
final class PcDuelHealthPolicy {
    static final String NAMESPACE="pc-duel-physical-health-v1";
    private static final int MAGIC=0x50444831;
    private static final class State {
        final SortedMap<Integer,Integer> health=new TreeMap<>();
    }
    static boolean enabled(World w){byte[]b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&ByteBuffer.wrap(b).getInt()==MAGIC;}
    static void initializeOpening(World w)throws IOException {
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null)throw new IOException("原体力策略只能在明确新局建立");
        Map<Integer,PcDuelSourceFacts.Fact> facts=PcDuelSourceFacts.saved(w);
        if(facts.size()!=670)throw new IOException("原体力初始人物事实不完整");
        State s=new State();for(var f:facts.values())s.health.put(f.officerId,f.initialPhysicalHealth);write(w,s);
    }
    private static State read(World w)throws IOException {
        byte[]b=w.extensions.get(NAMESPACE);if(!enabled(w)||b.length>32768)throw new IOException("原体力保存策略缺失或过大");
        DataInputStream d=new DataInputStream(new ByteArrayInputStream(b));var source=PcScenarioIdentity.saved(w);
        if(d.readInt()!=MAGIC||d.readInt()!=1||source==null||!d.readUTF().equals(source.scenarioId)||!d.readUTF().equals(source.sha)||!d.readUTF().equals(source.sourceVariant))throw new IOException("原体力来源或保存策略不一致");
        Map<Integer,PcDuelSourceFacts.Fact> facts=PcDuelSourceFacts.saved(w);int count=d.readInt();if(count!=facts.size()||count!=670)throw new IOException("原体力人物覆盖不同");
        State s=new State();for(int i=0;i<count;i++){int id=d.readInt(),nativeId=d.readInt(),hp=d.readUnsignedByte();var f=facts.get(id);if(f==null||f.nativeId!=nativeId||w.officer(id)==null||hp>100||s.health.put(id,hp)!=null)throw new IOException("原体力人物或数值无效");}
        if(d.available()!=0)throw new IOException("原体力保存尾部未知");return s;
    }
    private static void write(World w,State s)throws IOException {
        var source=PcScenarioIdentity.saved(w);var facts=PcDuelSourceFacts.saved(w);if(source==null||s.health.size()!=facts.size())throw new IOException("原体力来源缺失");
        var b=new ByteArrayOutputStream();var d=new DataOutputStream(b);d.writeInt(MAGIC);d.writeInt(1);d.writeUTF(source.scenarioId);d.writeUTF(source.sha);d.writeUTF(source.sourceVariant);d.writeInt(s.health.size());
        for(var e:s.health.entrySet()){var f=facts.get(e.getKey());if(f==null||e.getValue()<0||e.getValue()>100)throw new IOException("原体力写入无效");d.writeInt(e.getKey());d.writeInt(f.nativeId);d.writeByte(e.getValue());}w.extensions.put(NAMESPACE,b.toByteArray());
    }
    static int health(World w,int id)throws IOException {Integer hp=read(w).health.get(id);if(hp==null)throw new IOException("原体力人物未核实");return hp;}
    /** Original4d3110 writes terminal participant health at least1. No recovery
     * or healing rule is inferred here; the verified campaign caller owns it. */
    static void writeDuelResult(World w,Map<Integer,Integer> terminalHealth)throws IOException {
        State s=read(w);for(var e:terminalHealth.entrySet()){if(!s.health.containsKey(e.getKey())||e.getValue()<0||e.getValue()>100)throw new IOException("原单挑终局体力无效");}
        for(var e:terminalHealth.entrySet())s.health.put(e.getKey(),Math.max(1,e.getValue()));write(w,s);
    }
    static void recoverGlobalTurn(World w)throws IOException {
        State s=read(w);for(var e:s.health.entrySet())if(PcDuelPhysicalRecoveryPolicy.validPerson(w,e.getKey()))e.setValue(PcDuelPhysicalRecoveryPolicy.recovered(e.getValue(),w.contests.injury(e.getKey())));write(w,s);
    }
    static void validate(World w)throws IOException {if(enabled(w))read(w);}
    private PcDuelHealthPolicy(){}
}
