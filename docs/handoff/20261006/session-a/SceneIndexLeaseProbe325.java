package game.sanguo.mobile;
import game.sanguo.core.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Owner lifetime plus actual immutable source-index encoding; no GPU execution claim. */
public final class SceneIndexLeaseProbe325 {
 static int checks;
 static void check(boolean good){if(!good)throw new AssertionError("check "+checks);checks++;}
 static void reject(Runnable r){try{r.run();throw new AssertionError("expected failure");}catch(IllegalStateException|IllegalArgumentException expected){checks++;}}
 static byte[] bytes(Buffer b){ByteBuffer out=ByteBuffer.allocate(b.remaining()*(b instanceof ShortBuffer?2:4)).order(ByteOrder.nativeOrder());if(b instanceof ShortBuffer s)while(s.hasRemaining())out.putShort(s.get());else{IntBuffer i=(IntBuffer)b;while(i.hasRemaining())out.putInt(i.get());}return out.array();}
 public static void main(String[] args)throws Exception{
  var pool=new SceneIndexLeasePool<Object>(16);int[] indices={0,1,2};AtomicInteger created=new AtomicInteger(),destroyed=new AtomicInteger();
  var one=pool.acquire(3,indices,()->{created.incrementAndGet();return new Object();},r->destroyed.incrementAndGet());
  var two=pool.acquire(4,indices,()->{throw new AssertionError("duplicate factory");},r->{throw new AssertionError("wrong destroy");});
  check(one.resource()==two.resource());check(created.get()==1&&pool.liveLeases()==2&&pool.liveResources()==1);reject(pool::close);
  reject(()->pool.acquire(2,indices,Object::new,r->{}));check(pool.liveLeases()==2);one.release();one.release();check(destroyed.get()==0&&pool.liveLeases()==1);reject(one::resource);two.release();check(destroyed.get()==1&&pool.liveResources()==0&&pool.registeredResources()==0);
  var again=pool.acquire(3,indices,()->{created.incrementAndGet();return new Object();},r->destroyed.incrementAndGet());check(created.get()==2);
  var uint=pool.acquire(65537,indices,Object::new,r->destroyed.incrementAndGet());check(uint.resource()!=again.resource());
  var other=new SceneIndexLeasePool<Object>(16);var differentEngine=other.acquire(3,indices,Object::new,r->{});check(differentEngine.resource()!=again.resource());differentEngine.release();other.close();
  again.release();uint.release();check(pool.liveResources()==0&&pool.destroyed()==3);
  var leases=new ArrayList<SceneIndexLeasePool.Lease<Object>>();for(int n=0;n<40;n++)leases.add(pool.acquire(3,new int[]{0,1,2},Object::new,r->{}));check(pool.registeredResources()==16&&pool.liveResources()==40&&pool.liveLeases()==40);
  for(var lease:leases)lease.release();check(pool.liveResources()==0&&pool.liveLeases()==0&&pool.registeredResources()==0);
  try{pool.acquire(3,new int[]{0},()->{throw new IllegalArgumentException("factory failed");},r->{throw new AssertionError("never created");});throw new AssertionError("factory fail missing");}catch(IllegalArgumentException expected){checks++;}
  check(pool.liveResources()==0&&pool.liveLeases()==0);reject(()->pool.acquire(3,new int[]{-1},Object::new,r->{}));reject(()->pool.acquire(3,new int[]{3},Object::new,r->{}));reject(()->pool.acquire(3,new int[]{0},()->null,r->{}));
  AtomicInteger wrong=new AtomicInteger();Thread t=new Thread(()->{try{pool.acquire(3,indices,Object::new,r->{});}catch(IllegalStateException expected){wrong.incrementAndGet();}});t.start();t.join();check(wrong.get()==1);pool.close();reject(()->pool.acquire(3,indices,Object::new,r->{}));
  World w=PcScenarioCatalog.preview(PcScenarioCatalog.all().get(14).identity.scenarioId);byte[] saved=SaveCodec.encode(w);var g=new MapSceneSnapshot.Ground(w);var meshes=SceneMesh.ground(g,List.of(),new SceneMesh.TerrainWindow((g.minX+g.maxX)/2,(g.minZ+g.maxZ)/2,130,130,87.153015f));
  var actual=new SceneIndexLeasePool<byte[]>(16);var actualLeases=new ArrayList<SceneIndexLeasePool.Lease<byte[]>>();long oldBytes=0;long[] newBytes={0};int[] factories={0},destroys={0};
  for(var mesh:meshes){
   int vertices=mesh.vertices.length/7;byte[] baseline=bytes(MeshIndexBuffer.encode(vertices,mesh.indices));oldBytes+=baseline.length;
   var lease=actual.acquire(vertices,mesh.indices,()->{byte[] encoded=bytes(MeshIndexBuffer.encode(vertices,mesh.indices));newBytes[0]+=encoded.length;factories[0]++;return encoded;},r->destroys[0]++);
   check(Arrays.equals(baseline,lease.resource()));actualLeases.add(lease);
  }
  check(actual.reused()>0&&newBytes[0]<oldBytes&&actual.liveLeases()==meshes.size());Collections.reverse(actualLeases);for(var lease:actualLeases)lease.release();check(actual.liveResources()==0&&actual.liveLeases()==0&&destroys[0]==factories[0]);actual.close();check(Arrays.equals(saved,SaveCodec.encode(w)));
  System.out.println("checks="+checks+" chunks="+meshes.size()+" originalEncodedBytes="+oldBytes+" sharedEncodedBytes="+newBytes[0]+" resourceCreates="+factories[0]+" resourceDestroys="+destroys[0]+" sharedReuses="+actual.reused()+" fullSaveRngPure=true");
 }
}
