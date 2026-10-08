package game.sanguo.core;
import java.io.*;
/** Original ordinary5805d0 global loop59c479, after580e70 injury settlement.
 * Separate explicit capability: historical health and unknown saves never refill on load. */
final class PcDuelPhysicalRecoveryPolicy {
 static final String NAMESPACE="pc-duel-physical-recovery-v1";
 static final String LOOP_SHA="5b2d03100b5e434f54d1bcfe9dc6cc6aa2109c614e5ac857ebc3da59ae6fc768";
 static final String PHASE_SHA="5aec18d3fe3fbb8ea966d77ca0d2237266546c2cf4d0a23bd34b5d228c0536c4";
 private static final int MAGIC=0x50505231;
 static boolean enabled(World w){byte[]b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&java.nio.ByteBuffer.wrap(b).getInt()==MAGIC;}
 private static byte[] receipt(World w,int last)throws IOException{
  var src=PcScenarioIdentity.saved(w);if(src==null)throw new IOException("原体力恢复来源缺失");var b=new ByteArrayOutputStream();try(var d=new DataOutputStream(b)){d.writeInt(MAGIC);d.writeInt(1);d.writeUTF(PcScenarioIdentity.EXE_SHA);d.writeUTF(src.scenarioId);d.writeUTF(src.sha);d.writeUTF(src.sourceVariant);d.writeUTF(LOOP_SHA);d.writeUTF(PHASE_SHA);d.writeInt(last);}return b.toByteArray();
 }
 private static int read(World w)throws IOException{
  byte[]b=w.extensions.get(NAMESPACE);var src=PcScenarioIdentity.saved(w);if(!enabled(w)||b.length>2048||src==null||!PcDuelCampaignPolicy.enabled(w)||PcDuelCampaignPolicy.read(w).version!=3||!PcDuelHealthPolicy.enabled(w))throw new IOException("原体力恢复策略或人物来源不同");
  try(var d=new DataInputStream(new ByteArrayInputStream(b))){if(d.readInt()!=MAGIC||d.readInt()!=1||!d.readUTF().equals(PcScenarioIdentity.EXE_SHA)||!d.readUTF().equals(src.scenarioId)||!d.readUTF().equals(src.sha)||!d.readUTF().equals(src.sourceVariant)||!d.readUTF().equals(LOOP_SHA)||!d.readUTF().equals(PHASE_SHA))throw new IOException("原体力恢复证据或保存来源不同");int last=d.readInt();if(last<0||last>w.turn||d.available()!=0)throw new IOException("原体力恢复旬序或未知尾部不同");return last;}
 }
 static void validate(World w)throws IOException{if(enabled(w))read(w);}
 static String adoptionError(World w){
  if(w.extensions.get(NAMESPACE)!=null)return enabled(w)?"当前保存已采用原逐旬体力恢复":"保存已有未知体力恢复策略，请保留原档";
  try{if(!PcDuelCampaignPolicy.enabled(w)||PcDuelCampaignPolicy.read(w).version!=3)return "旧保存没有明确原单挑战役策略，不自动补填";PcDuelHealthPolicy.validate(w);return w.extensions.putError(NAMESPACE,receipt(w,w.turn).length);}catch(IOException e){return e.getMessage();}
 }
 static void adopt(World w)throws IOException{String e=adoptionError(w);if(e!=null)throw new IOException(e);w.extensions.put(NAMESPACE,receipt(w,w.turn));validate(w);}
 static void initializeOpening(World w)throws IOException{if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy())throw new IOException("原体力恢复只可明确新局建立");adopt(w);}
 static int recovered(int health,int injury){if(health<0||health>100||injury<0||injury>3)throw new IllegalArgumentException("原体力恢复输入未知");return health==100?100:Math.min(health+30,new int[]{100,80,50,30}[injury]);}
 static boolean validPerson(World w,int id){var s=w.life.state(id);return w.officer(id)!=null&&(s==Lifecycle.State.ACTIVE||s==Lifecycle.State.UNDISCOVERED);}
 static void tick(World w)throws IOException{if(!enabled(w))return;int last=read(w);if(last==w.turn)return;PcDuelHealthPolicy.recoverGlobalTurn(w);w.extensions.put(NAMESPACE,receipt(w,w.turn));}
 private PcDuelPhysicalRecoveryPolicy(){}
}
