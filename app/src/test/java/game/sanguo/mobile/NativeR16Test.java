package game.sanguo.mobile;

import game.sanguo.core.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Exact CPU index round trips over production meshes; no phone performance inference. */
public final class NativeR16Test {
    static long checks,oldBytes,newBytes,meshes;static int wide;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void mesh(SceneMesh m){
        int vertices=m.vertices.length/7;Buffer packed=MeshIndexBuffer.encode(vertices,m.indices);
        for(int i=0;i<m.indices.length;i++)check((packed instanceof ShortBuffer?Short.toUnsignedInt(((ShortBuffer)packed).get(i)):((IntBuffer)packed).get(i))==m.indices[i],"lossless index "+i);
        oldBytes+=(long)m.indices.length*4;newBytes+=MeshIndexBuffer.bytes(vertices,m.indices.length);meshes++;if(packed instanceof IntBuffer)wide++;
    }
    static void thermal(){
        SceneQuality.Thermal t=new SceneQuality.Thermal();
        check(t.update(3,0)&&t.constrained,"severe immediate");
        for(int i=1;i<=60;i++){t.update(i%2==0?1:2,i*1_000_000_000L);check(t.constrained,"oscillation cannot recover");}
        t.update(-1,61_000_000_000L);t.tick(100_000_000_000L);check(t.constrained,"unknown cannot prove cool");
        t.update(1,101_000_000_000L);t.tick(130_999_999_999L);check(t.constrained,"whole cooldown required");
        check(t.tick(131_000_000_000L)&&!t.constrained&&t.transitions==2,"recovery without another callback");
        t.update(4,132_000_000_000L);check(t.constrained&&t.transitions==3,"reheat immediate");
        for(SceneQuality q:SceneQuality.values()){check(t.scale(q)<=q.scale&&t.fps(q)<=q.fps,"thermal never upgrades");}
    }
    static void stamp(){
        SceneCamera c=new SceneCamera();SceneVisibilityStamp s=new SceneVisibilityStamp();check(!s.matches(c),"initial invalid");s.set(c);check(s.matches(c),"stationary");
        for(int axis=0;axis<8;axis++){s.set(c);switch(axis){case 0:c.x+=.001f;break;case 1:c.z+=.001f;break;case 2:c.span+=.001f;break;case 3:c.yaw+=.001f;break;case 4:c.tilt+=.001f;break;case 5:c.width++;break;case 6:c.height++;break;default:c.facing=-c.facing;}check(!s.matches(c),"camera dimension "+axis);}
        s.set(c);s.invalidate();check(!s.matches(c),"new mesh/session invalidates");
    }
    public static void main(String[] args)throws Exception{
        thermal();stamp();SceneQualityTest.main(new String[0]);
        for(int vertices:new int[]{1,32768,32769,65535,65536,65537,100000}){
            int[] indices={0,vertices/2,vertices-1};Buffer p=MeshIndexBuffer.encode(vertices,indices);
            check((p instanceof ShortBuffer)==(vertices<=65536),"ushort boundary");
            for(int i=0;i<3;i++)check((p instanceof ShortBuffer?Short.toUnsignedInt(((ShortBuffer)p).get(i)):((IntBuffer)p).get(i))==indices[i],"unsigned range");
        }
        for(int index:new int[]{-1,65536}){boolean rejected=false;try{MeshIndexBuffer.encode(65536,new int[]{index});}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid index rejected");}
        try(var paths=Files.walk(Path.of("app/src/main/assets/3d"))){for(Path p:(Iterable<Path>)paths.filter(f->f.toString().endsWith(".glb"))::iterator)try(var in=Files.newInputStream(p)){mesh(SiteGlb.read(in));}}
        World w=ScenarioCatalog.load("coalition-190",5);byte[] before=SaveCodec.encode(w);MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
        for(float span:new float[]{8,25,120}){
            SceneMesh.TerrainWindow window=new SceneMesh.TerrainWindow((g.minX+g.maxX)/2,(g.minZ+g.maxZ)/2,span,span,span);
            for(SceneMesh m:SceneMesh.ground(g,Collections.emptyList(),window))mesh(m);
        }
        mesh(SceneMesh.backdrop(g));check(Arrays.equals(before,SaveCodec.encode(w)),"authority and RNG unchanged");
        SceneFrameMetrics metrics=new SceneFrameMetrics();for(int i=0;i<1000;i++)metrics.record(i,2,3,4,5,i%2==0);
        check(metrics.csv().split("\n").length==301,"bounded raw ring");check(metrics.summary().contains("submitted=500 rejected=500"),"admission separate");
        Files.createDirectories(Path.of("out/r16"));
        String report="{\"meshes\":"+meshes+",\"uint32_before_bytes\":"+oldBytes+",\"encoded_after_bytes\":"+newBytes+",\"uint32_retained_meshes\":"+wide+",\"checks\":"+checks+",\"measurement\":\"exact index upload payload; NOT GPU memory or FPS\"}";
        Files.writeString(Path.of("out/r16/index-comparison.json"),report+"\n");System.out.println("PASS R16 "+report);
    }
}
