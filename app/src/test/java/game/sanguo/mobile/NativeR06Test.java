package game.sanguo.mobile;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.json.*;
public final class NativeR06Test {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static byte[] change(byte[] input,Consumer<JSONObject> edit)throws Exception{
  ByteBuffer b=ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN);int length=b.getInt(12);JSONObject doc=new JSONObject(new String(input,20,length,java.nio.charset.StandardCharsets.UTF_8));edit.accept(doc);
  byte[] json=doc.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),bin=Arrays.copyOfRange(input,28+length,input.length);int pad=(json.length+3)&~3;
  ByteBuffer out=ByteBuffer.allocate(28+pad+bin.length).order(ByteOrder.LITTLE_ENDIAN);out.putInt(0x46546c67).putInt(2).putInt(out.capacity()).putInt(pad).putInt(0x4e4f534a).put(json);while(out.position()<20+pad)out.put((byte)32);out.putInt(bin.length).putInt(0x004e4942).put(bin);return out.array();
 }
 static void rejects(byte[] b,String why)throws Exception{try{SiteGlb.read(new ByteArrayInputStream(b));throw new AssertionError("accepted "+why);}catch(IOException|JSONException expected){checks++;}}
 static void until(SceneAssetQueue q,java.util.function.BooleanSupplier done)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!done.getAsBoolean()&&System.nanoTime()<end){q.drain();Thread.yield();}check(done.getAsBoolean(),"queue completion");}
 public static void main(String[] args)throws Exception{
  byte[] good=Files.readAllBytes(Path.of("app/src/main/assets/3d/sites/gate-lod0.glb"));
  for(Path dir:List.of(Path.of("app/src/main/assets/3d/sites"),Path.of("app/src/main/assets/3d/field")))try(var files=Files.list(dir)){for(Path p:files.filter(p->p.toString().endsWith(".glb")).toList()){try(InputStream in=Files.newInputStream(p)){check(SiteGlb.read(in).indices.length>0,"all bundled models");}}}
  rejects(Arrays.copyOf(good,good.length-1),"truncated buffer");byte[] bad=good.clone();bad[0]=0;rejects(bad,"header");
  rejects(change(good,d->d.put("extensionsRequired",new JSONArray().put("unknown"))),"required extension");
  rejects(change(good,d->d.getJSONArray("buffers").getJSONObject(0).put("uri","../../secret")),"path traversal");
  rejects(change(good,d->d.getJSONArray("nodes").getJSONObject(0).put("scale",new JSONArray().put(2).put(2).put(2))),"node transform");
  rejects(change(good,d->d.put("skins",new JSONArray())),"skin");
  rejects(change(good,d->d.put("animations",new JSONArray())),"animation");
  rejects(change(good,d->d.getJSONArray("accessors").getJSONObject(0).put("normalized",true)),"normalized float");
  rejects(change(good,d->d.getJSONArray("bufferViews").getJSONObject(0).put("byteStride",16)),"stride");
  rejects(change(good,d->d.getJSONArray("bufferViews").getJSONObject(0).put("byteOffset",Integer.MAX_VALUE)),"offset overflow");
  rejects(change(good,d->d.getJSONArray("accessors").getJSONObject(0).put("count",Integer.MAX_VALUE)),"count overflow");
  rejects(change(good,d->d.getJSONArray("materials").getJSONObject(0).put("alphaMode","BLEND")),"unsupported alpha");
  rejects(change(good,d->d.getJSONArray("meshes").getJSONObject(0).getJSONArray("primitives").getJSONObject(0).getJSONObject("attributes").put("JOINTS_0",0)),"silent dropped attribute");
  ByteBuffer buf=ByteBuffer.wrap(good).order(ByteOrder.LITTLE_ENDIAN);int base=28+buf.getInt(12);JSONObject doc=new JSONObject(new String(good,20,buf.getInt(12),java.nio.charset.StandardCharsets.UTF_8));
  bad=good.clone();ByteBuffer.wrap(bad).order(ByteOrder.LITTLE_ENDIAN).putInt(base+doc.getJSONArray("bufferViews").getJSONObject(3).getInt("byteOffset"),Integer.MAX_VALUE);rejects(bad,"index overflow");
  bad=good.clone();ByteBuffer.wrap(bad).order(ByteOrder.LITTLE_ENDIAN).putFloat(base,Float.POSITIVE_INFINITY);rejects(bad,"nonfinite position");
  bad=good.clone();ByteBuffer.wrap(bad).order(ByteOrder.BIG_ENDIAN).putInt(base+doc.getJSONArray("bufferViews").getJSONObject(4).getInt("byteOffset")+16,99999);rejects(bad,"texture dimension budget");
  FieldAssets assets=new FieldAssets(name->Files.newInputStream(Path.of("app/src/main/assets/3d/field",name)));
  try{assets.mesh("../sites/gate-lod0");throw new AssertionError("unsafe asset ID");}catch(IOException expected){checks++;}
  SceneMesh rest=assets.mesh("unit-CATAPULT-lod0");check(rest==assets.mesh("unit-CATAPULT-lod0"),"shared rest data");
  check(!Arrays.equals(assets.pose("unit-CATAPULT-lod0","attack",0,1).vertices,assets.pose("unit-CATAPULT-lod0","attack",6,1).vertices),"working lever moves");
  try(SceneAssetQueue q=new SceneAssetQueue()){
   Thread owner=Thread.currentThread();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
   check(q.request("blocked",()->{check(Thread.currentThread()!=owner,"decode worker thread");entered.countDown();while(true)try{release.await();break;}catch(InterruptedException ignored){}return rest;})==null,"nonblocking request");
   check(entered.await(5,TimeUnit.SECONDS),"worker entered");q.invalidate();release.countDown();
   q.request("good",()->rest);until(q,()->q.pending()==0);check(q.request("good",()->{throw new AssertionError("decoded twice");})==rest,"shared result");
   check(q.bytes()>0,"tracked CPU budget");q.invalidate();check(q.pending()==0&&q.bytes()==0,"world releases CPU references");
   q.request("bad",()->{throw new IOException("bad asset");});until(q,()->q.pending()==0);check(q.error("bad").contains("bad asset"),"visible error diagnostic");
  }
  System.out.println("PASS R06 "+checks+" checks: actual bundled subset, hostile input, rigid parts, shared/cancelled CPU decode");
 }
}
