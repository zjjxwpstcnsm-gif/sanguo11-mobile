package game.sanguo.mobile;

import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Serialize all actual streamed GPU inputs for before/after allocator comparison. */
public final class PcGroundSnapshotProbe {
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("output directory");Path out=Path.of(args[0]);Files.createDirectories(out);
        World w=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(w);
        float[][] views={{155,71.5f,3},{155,71.5f,50},{50,160,5},{99.5f,99.75f,50}};
        for(int n=0;n<views.length+1;n++){
            if(n==views.length)w.mapRevision=63;
            float[] v=views[n==views.length?0:n];MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);var window=new SceneMesh.TerrainWindow(v[0],v[1],v[2]*1.3f,v[2]*1.6f,v[2]);
            List<SceneMesh> meshes=SceneMesh.ground(g,List.of(),window);MessageDigest sha=MessageDigest.getInstance("SHA-256");long count=0;
            try(DataOutputStream d=new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(),sha))){
                for(SceneMesh m:meshes){write(d,m);count+=m.vertices.length+m.indices.length+(m.surfaceData==null?0:m.surfaceData.length);}
                SceneMesh background=SceneMesh.backdrop(g);if(background!=null)write(d,background);
            }
            String digest=HexFormat.of().formatHex(sha.digest());String line="GROUND_INPUT view="+n+" pc="+(g.pcMap!=null)+" chunks="+meshes.size()+" attributes="+count+" sha256="+digest;
            Files.writeString(out.resolve("view-"+n+".txt"),line+"\n");System.out.println(line);
        }
        w.mapRevision=65;if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("ground query mutated authority/RNG");
    }
    private static void write(DataOutputStream d,SceneMesh m)throws IOException{
        d.writeInt(m.chunkQ);d.writeInt(m.chunkR);d.writeInt(m.landIndexCount);d.writeInt(m.terrainLod);d.writeInt(Float.floatToRawIntBits(m.x));d.writeInt(Float.floatToRawIntBits(m.z));d.writeInt(Float.floatToRawIntBits(m.radius));
        floats(d,m.vertices);ints(d,m.indices);floats(d,m.uv);floats(d,m.tangents);floats(d,m.surfaceData);
        d.writeBoolean(m.grid!=null);if(m.grid!=null)write(d,m.grid);d.writeBoolean(m.distant!=null);if(m.distant!=null)write(d,m.distant);
    }
    private static void floats(DataOutputStream d,float[] a)throws IOException{d.writeInt(a==null?-1:a.length);if(a!=null)for(float v:a)d.writeInt(Float.floatToRawIntBits(v));}
    private static void ints(DataOutputStream d,int[] a)throws IOException{d.writeInt(a==null?-1:a.length);if(a!=null)for(int v:a)d.writeInt(v);}
}
