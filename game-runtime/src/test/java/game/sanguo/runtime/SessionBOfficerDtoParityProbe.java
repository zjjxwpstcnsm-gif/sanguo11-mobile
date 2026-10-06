package game.sanguo.runtime;
import game.sanguo.core.*;
import game.sanguo.runtime.query.OfficerQuery;
import game.sanguo.api.StateToken;
import java.io.*;import java.lang.reflect.*;import java.nio.file.*;import java.security.*;import java.util.*;
public final class SessionBOfficerDtoParityProbe {
 static void write(DataOutputStream d,Object v)throws Exception{
  if(v==null){d.writeByte(0);return;}if(v instanceof String||v instanceof Number||v instanceof Boolean){d.writeByte(1);d.writeUTF(v.getClass().getName());byte[] b=v.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);d.writeInt(b.length);d.write(b);return;}
  if(v instanceof Map){d.writeByte(4);Map<?,?> map=(Map<?,?>)v;List<Object> keys=new ArrayList<>(map.keySet());keys.sort(Comparator.comparing(Object::toString));d.writeInt(keys.size());for(Object key:keys){write(d,key);write(d,map.get(key));}return;}
  if(v instanceof List){d.writeByte(2);d.writeInt(((List<?>)v).size());for(Object x:(List<?>)v)write(d,x);return;}
  d.writeByte(3);d.writeUTF(v.getClass().getName());Field[] fs=v.getClass().getFields();Arrays.sort(fs,Comparator.comparing(Field::getName));for(Field f:fs)if(!Modifier.isStatic(f.getModifiers())){d.writeUTF(f.getName());write(d,f.get(v));}d.writeUTF("");
 }
 static String hash(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 static void capture(String label,World w)throws Exception{
  byte[] before=SaveCodec.encode(w);ByteArrayOutputStream b=new ByteArrayOutputStream();long t=System.nanoTime();write(new DataOutputStream(b),OfficerQuery.capture(w,new StateToken("dto-equivalence",1,0)));long elapsed=System.nanoTime()-t;
  if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("DTO changes full world/bothRNG "+label);System.out.println(label+"\t"+hash(b.toByteArray())+"\t"+hash(before));System.err.println(label+" queryMs="+elapsed/1000000);
 }
 public static void main(String[] a)throws Exception{
  for(var s:PcScenarioCatalog.all())capture(s.identity.scenarioId,PcScenarioCatalog.preview(s.identity.scenarioId));
  for(String p:List.of("core/src/test/resources/save-v32-central-native.sg11","core/src/test/resources/pre-base-construction-v33.sg11","core/src/test/resources/pre-atomic-ship-cargo-v33.sg11","game-runtime/src/test/resources/architecture/prepared-9548bb35-v33.sg11","core/src/test/resources/pre-merchant-r25-v34.sg11","core/src/test/resources/legacy-market-v35/host/coalition-190.sg11","core/src/test/resources/legacy-production-v36/coalition-190-host.sg11","docs/handoff/20261004/session2/six-turn-authority-29/authority-before.sg11","docs/handoff/20261004/session1/batch19-actual-art-mid.sg11"))capture(p,SaveCodec.decode(Files.readAllBytes(Path.of(p))));
 }
}
