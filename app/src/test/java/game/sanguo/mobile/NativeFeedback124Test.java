package game.sanguo.mobile;

import game.sanguo.core.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Real input/candidate algorithms, exact geometry and full save parity. Host
 * allocation/CPU measurements are explicitly not Android frame-rate evidence. */
public final class NativeFeedback124Test {
    static int checks;static long sink;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void equal(SceneMesh a,SceneMesh b){
        check(Arrays.equals(a.vertices,b.vertices),"exact unchanged vertex positions and pigments");
        check(Arrays.equals(a.indices,b.indices),"exact unchanged triangles/order");
        check(Arrays.equals(a.uv,b.uv),"exact unchanged UV boundaries");
    }
    static float[] normal(SceneMesh m,int i){
        float x=m.tangents[i*4],y=m.tangents[i*4+1],z=m.tangents[i*4+2],w=m.tangents[i*4+3];
        return new float[]{2*(x*z+y*w),2*(y*z-x*w),1-2*(x*x+y*y)};
    }
    static void rotation()throws Exception{
        String rig="{\"version\":1,\"rigs\":{\"unit-SPEAR-lod0\":{\"parts\":[{\"name\":\"body\",\"parent\":-1,\"pivot\":[1,2,3],\"first\":0,\"count\":3}]}}}";
        JSONObject clips=new JSONObject().put("version",1).put("fps",12);JSONArray frames=new JSONArray();
        float p=(float)Math.PI;
        float[][] angles={{0,0,p/2},{p,0,0},{0,p,0},{0,0,p},{p/2,0,0},{0,p/2,0},{.2f,.3f,.4f},{0,0,0}};
        for(int i=0;i<12;i++)frames.put(new JSONObject().put("body",new JSONArray(angles[i%angles.length])));
        clips.put("clips",new JSONObject().put("idle",frames));
        FieldAssets assets=new FieldAssets(name->new java.io.ByteArrayInputStream((name.startsWith("rigs")?rig:clips.toString()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        SceneMesh rest=new SceneMesh(new float[]{1,2,3,1,1,1,1,2,2,3,1,1,1,1,1,3,3,1,1,1,1},new int[]{0,1,2},0,0,1);
        rest.uv=new float[]{0,0,1,0,0,1};rest.setNormals(new float[]{.6f,.8f,0,.6f,.8f,0,.6f,.8f,0});rest.authoredTangentFrame=true;
        float[] originalFrame=rest.tangents.clone();
        var field=FieldAssets.class.getDeclaredField("rest");field.setAccessible(true);((Map<String,SceneMesh>)field.get(assets)).put("unit-SPEAR-lod0",rest);
        SceneMesh pose=assets.pose("unit-SPEAR-lod0","idle",0,1);float[] n=normal(pose,0);
        check(Math.abs(n[0]+.8f)<.0001&&Math.abs(n[1]-.6f)<.0001&&Math.abs(n[2])<.0001,"authored normal rotated 90 degrees without pivot translation");
        check(Math.abs(pose.vertices[7]-1)<.0001&&Math.abs(pose.vertices[8]-3)<.0001,"positions rotate around real pivot");
        check(Arrays.equals(rest.tangents,originalFrame)&&rest.vertices[7]==2,"immutable authored streams retained");
        // Independent Rx -> Ry -> Rz vector rotations cover every trace branch,
        // including 180 degree joints where a trace-only conversion is singular.
        for(int i=0;i<angles.length;i++){
            float[] a=angles[i];double x=.6,y=.8*Math.cos(a[0]),z=.8*Math.sin(a[0]);
            double xx=x*Math.cos(a[1])+z*Math.sin(a[1]),zz=-x*Math.sin(a[1])+z*Math.cos(a[1]);
            double X=xx*Math.cos(a[2])-y*Math.sin(a[2]),Y=xx*Math.sin(a[2])+y*Math.cos(a[2]);
            float[] got=normal(assets.pose("unit-SPEAR-lod0","idle",i,1),0);
            check(Math.abs(got[0]-X)+Math.abs(got[1]-Y)+Math.abs(got[2]-zz)<.0002,"normal direction preserved through arbitrary rigid rotation "+i);
            if(i==angles.length-1)check(Arrays.equals(originalFrame,assets.pose("unit-SPEAR-lod0","idle",i,1).tangents),"identity joints copy authored frames exactly");
        }
    }
    interface Operation {void run()throws Exception;}
    static void measure(String name,Operation op)throws Exception{
        com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        check(bean.isThreadAllocatedMemorySupported(),"JDK allocation instrumentation available");bean.setThreadAllocatedMemoryEnabled(true);
        long id=Thread.currentThread().getId();for(int i=0;i<3;i++)op.run();
        for(int round=0;round<5;round++){
            long bytes=bean.getThreadAllocatedBytes(id),cpu=bean.getCurrentThreadCpuTime(),wall=System.nanoTime();
            op.run();long elapsed=System.nanoTime()-wall,used=bean.getCurrentThreadCpuTime()-cpu,allocated=bean.getThreadAllocatedBytes(id)-bytes;
            Files.writeString(Path.of("out/feedback124/host-samples.csv"),name+","+round+","+allocated+","+used+","+elapsed+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }
    }
    public static void main(String[] args)throws Exception{
        FieldAssets assets=new FieldAssets(n->Files.newInputStream(Path.of("app/src/main/assets/3d/field",n)));
        FieldAssetsBaseline124 old=new FieldAssetsBaseline124(n->Files.newInputStream(Path.of("app/src/main/assets/3d/field",n)));
        rotation();
        JSONObject report=new JSONObject(Files.readString(Path.of("docs/native-pc-visual/feedback-v124-blender-assets.json")));
        check(report.getJSONArray("assets").length()==49,"49 actual versioned GLBs");
        for(Object object:report.getJSONArray("assets")){
            JSONObject a=(JSONObject)object;SceneMesh mesh;
            try(var input=Files.newInputStream(Path.of(a.getString("path")))){mesh=SiteGlb.read(input);}
            check(mesh.vertices.length/7==a.getInt("vertices")&&mesh.indices.length/3==a.getInt("triangles"),"actual strict loader matches asset manifest");
            check(mesh.authoredTangentFrame,"Blender normals available in encoded frames");
            if(!a.getString("path").contains("/unit-"))continue;
            String model=Path.of(a.getString("path")).getFileName().toString().replace(".glb","");
            float[] before=assets.mesh(model).vertices.clone();
            for(String clip:new String[]{"idle","turn","walk","prepare","attack","hit","defeat","enter"})for(int frame=0;frame<12;frame++){
                SceneMesh pose=assets.pose(model,clip,frame,1),previous=old.pose(model,clip,frame,1);equal(previous,pose);
                for(int i=0;i<pose.vertices.length/7;i++){
                    float[] n=normal(pose,i);check(Float.isFinite(n[0])&&Math.abs(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]-1)<.0005,"unit quaternion normal finite and normalized");
                }
                check(pose.indices==assets.mesh(model).indices&&pose.uv==assets.mesh(model).uv,"single member still shares immutable topology");
            }
            check(Arrays.equals(before,assets.mesh(model).vertices),"all clips leave rest model unchanged");
        }
        for(boolean staggered:new boolean[]{false,true}){
            World w=new World(16,16,"甲","乙");w.columnStaggered=staggered;
            w.cities.add(new World.City(1,"legal host fixture",new Hex(2,2),0));
            for(int q=0;q<16;q++)for(int r=0;r<16;r++)w.terrain[q][r]=q<8?World.Terrain.FOREST:World.Terrain.MOUNTAIN;
            for(int q=3;q<13;q++)w.terrain[q][8]=q<7?World.Terrain.PLANK_ROAD:World.Terrain.MOUNTAIN_PATH;
            for(Hex h:SiteFootprint.cells(w.cities.get(0)))w.terrain[h.q][h.r]=World.Terrain.PLAIN;
            byte[] before=SaveCodec.encode(w);MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
            SceneMesh.TerrainWindow window=new SceneMesh.TerrainWindow(g.grid.x(8,8),g.grid.z(8,8),7,7,8);
            List<SceneMesh> input=VegetationBaseline124.buildWindow(g,Set.of(),List.of(),assets,window),candidate=Vegetation.buildWindow(g,Set.of(),List.of(),assets,window);
            check(input.size()==candidate.size(),"identical production chunk coverage");
            for(int i=0;i<input.size();i++){equal(input.get(i),candidate.get(i));equal(input.get(i).distant,candidate.get(i).distant);check(Arrays.equals(input.get(i).tangents,candidate.get(i).tangents),"unchanged merged lighting");}
            List<SceneMesh> reuse=Vegetation.buildWindow(g,Set.of(),candidate,assets,window);
            for(int i=0;i<reuse.size();i++)check(reuse.get(i)==candidate.get(i),"unchanged chunks reused");
            check(Arrays.equals(before,SaveCodec.encode(w)),"all map, rules, save and RNG bytes unchanged");
            if(!staggered){
                Files.writeString(Path.of("out/feedback124/host-samples.csv"),"operation,round,allocated_bytes,thread_cpu_ns,wall_ns\n");
                measure("input-vegetation",()->{for(SceneMesh m:VegetationBaseline124.buildWindow(g,Set.of(),List.of(),assets,window))sink+=m.vertices.length;});
                measure("candidate-vegetation",()->{for(SceneMesh m:Vegetation.buildWindow(g,Set.of(),List.of(),assets,window))sink+=m.vertices.length;});
            }
        }
        measure("input-pose",()->{for(int i=0;i<100;i++)sink+=old.pose("unit-CAVALRY-lod0","walk",i,1).vertices.length;});
        measure("candidate-pose",()->{for(int i=0;i<100;i++)sink+=assets.pose("unit-CAVALRY-lod0","walk",i,1).vertices.length;});
        System.out.println("PASS FEEDBACK124 checks="+checks+"; exact input/candidate geometry; 13 armies x 2 LOD x 8 clips x 12 frames; host samples only; sink="+sink);
    }
}
