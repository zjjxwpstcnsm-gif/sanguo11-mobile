package game.sanguo.mobile;
import android.app.*;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Isolated ART allocation probe; geometry references actual installed production classes. */
public final class GridAllocationInstrumentation extends Instrumentation {
    private File output;private int checks;private String run="gridAllocation";private final StringBuilder rows=new StringBuilder("variant,order,q,r,lod,wall_ms,thread_cpu_ms,process_allocated_bytes,process_gc_ms,process_blocking_gc_ms,vertices,indices\n");
    @Override public void onCreate(Bundle args){super.onCreate(args);if(args!=null)run=args.getString("run",run).replaceAll("[^a-zA-Z0-9_-]","_");start();}
    private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private long counter(String key){String value=Debug.getRuntimeStat(key);return value==null?-1:Long.parseLong(value);}
    private SceneMesh measure(MapSceneSnapshot.Ground ground,int q,int r,int lod,boolean reference,String order){
        SceneMesh bounds=new SceneMesh(new float[0],new int[0],ground.grid.x(q+8,r+8),ground.grid.z(q+8,r+8),13);
        long wall=System.nanoTime(),cpu=Debug.threadCpuTimeNanos(),bytes=counter("art.gc.bytes-allocated"),gc=counter("art.gc.gc-time"),blocking=counter("art.gc.blocking-gc-time");
        SceneMesh mesh=reference?referenceGrid(ground,q,r,bounds,lod):SceneMesh.grid(ground,q,r,bounds,lod);
        long endWall=System.nanoTime(),endCpu=Debug.threadCpuTimeNanos(),endBytes=counter("art.gc.bytes-allocated"),endGc=counter("art.gc.gc-time"),endBlocking=counter("art.gc.blocking-gc-time");
        rows.append(reference?"retained_boxed":"installed_production").append(',').append(order).append(',').append(q).append(',').append(r).append(',').append(lod).append(',').append((endWall-wall)/1e6).append(',').append((endCpu-cpu)/1e6).append(',').append(bytes<0||endBytes<0?-1:endBytes-bytes).append(',').append(gc<0||endGc<0?-1:endGc-gc).append(',').append(blocking<0||endBlocking<0?-1:endBlocking-blocking).append(',').append(mesh.vertices.length/7).append(',').append(mesh.indices.length).append('\n');
        return mesh;
    }
    private void equal(SceneMesh a,SceneMesh b){
        check(a.vertices.length==b.vertices.length,"vertex count");
        for(int i=0;i<a.vertices.length;i++){checks++;if(Float.floatToRawIntBits(a.vertices[i])!=Float.floatToRawIntBits(b.vertices[i]))throw new AssertionError("vertex bits "+i);}
        check(Arrays.equals(a.indices,b.indices),"index order");check(a.landIndexCount==b.landIndexCount&&a.minY==b.minY&&a.maxY==b.maxY&&a.x==b.x&&a.z==b.z&&a.radius==b.radius,"metadata/bounds");
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        output=new File(getTargetContext().getExternalFilesDir("grid-profile"),run);output.mkdirs();
        World world=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(world);MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(world);
        // Warm both emission paths and the shared immutable-field memos before paired measurements.
        SceneMesh bounds=new SceneMesh(new float[0],new int[0],0,0,13);referenceGrid(ground,144,96,bounds,0);SceneMesh.grid(ground,144,96,bounds,0);
        for(String order:new String[]{"AB","BA"})for(int[] chunk:new int[][]{{128,80},{144,96},{160,80}})for(int lod=0;lod<3;lod++){
            SceneMesh a,b;if(order.equals("AB")){a=measure(ground,chunk[0],chunk[1],lod,true,order);b=measure(ground,chunk[0],chunk[1],lod,false,order);}else{b=measure(ground,chunk[0],chunk[1],lod,false,order);a=measure(ground,chunk[0],chunk[1],lod,true,order);}equal(a,b);
        }
        check(Arrays.equals(before,SaveCodec.encode(world)),"whole fixture authority/RNG unchanged");
        Files.write(new File(output,"paired-art-grid.csv").toPath(),rows.toString().getBytes("UTF-8"));
        String text="GRID ALLOCATION PASS "+checks+" checks; actual installed SceneMesh against retained boxed emitter; process ART counters, no GPU or first-frame claim\n";Files.write(new File(output,"result.txt").toPath(),text.getBytes("UTF-8"));result.putString("stream",text);finish(Activity.RESULT_OK,result);
    }catch(Throwable error){try{Files.write(new File(output,"paired-art-grid.csv").toPath(),rows.toString().getBytes("UTF-8"));}catch(Exception ignored){}StringWriter trace=new StringWriter();error.printStackTrace(new PrintWriter(trace));result.putString("stream","GRID ALLOCATION FAIL "+trace);finish(Activity.RESULT_CANCELED,result);}}
    private static SceneMesh referenceGrid(MapSceneSnapshot.Ground g,int q,int r,SceneMesh bounds,int lod){
        SceneMesh.Builder b=new SceneMesh.Builder();
        float width=lod==0?.012f:lod==1?.023f:.038f;
        for(int rr=Math.max(0,r);rr<Math.min(r+16,g.height);rr++)for(int qq=Math.max(0,q);qq<Math.min(q+16,g.width);qq++){
            Hex h=new Hex(qq,rr);if(!g.gridCell(h,false))continue;
            float x=g.grid.x(h),z=g.grid.z(h),cy=g.surface.at(h);
            for(int e=0;e<8;e++){
                float[] a=SceneMesh.EDGE[e],d=SceneMesh.EDGE[(e+1)%8];
                float ax=x+a[0],az=z+a[1],dx=x+d[0],dz=z+d[1];
                float ay=g.surface.sample(ax,az),dy=g.surface.sample(dx,dz);
                // Each half-stroke stays inside its eligible cell. Shared edges
                // join without drawing over a mountain/restricted cell interior.
                int segments=g.pcMap==null?1:2;
                for(int part=0;part<segments;part++){
                    float t0=part/(float)segments,t1=(part+1)/(float)segments;
                    float ex=ax+(dx-ax)*t0,ez=az+(dz-az)*t0,fx=ax+(dx-ax)*t1,fz=az+(dz-az)*t1;
                    float ey=g.surface.sample(ex,ez),fy=g.surface.sample(fx,fz);
                    gridRibbon(b,g,x,z,cy,ex,ez,ey,fx,fz,fy,width,.018f,0xff243b38);
                    gridRibbon(b,g,x,z,cy,ex,ez,ey,fx,fz,fy,width*.42f,.022f,0xffd9dfbe);
                }
            }
        }
        return b.mesh(bounds.x,bounds.z,bounds.radius);
    }
    private static void gridRibbon(SceneMesh.Builder b,MapSceneSnapshot.Ground g,float x,float z,float y,
            float ax,float az,float ay,float dx,float dz,float dy,float inset,float lift,int color){
        // Exact barycentric inset within the same visible fan planes, including
        // rounded shoreline transport. Width is an offline LOD choice, not a rule.
        float[] a=g.shoreline.project(ax,az),d=g.shoreline.project(dx,dz);
        float t=inset*2;
        b.face(new float[]{a[0],ay+lift,a[1],d[0],dy+lift,d[1],
            d[0]+(x-d[0])*t,dy+(y-dy)*t+lift,d[1]+(z-d[1])*t,
            a[0]+(x-a[0])*t,ay+(y-ay)*t+lift,a[1]+(z-a[1])*t},color);
    }
}
