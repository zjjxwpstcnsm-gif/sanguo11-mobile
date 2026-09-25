package game.sanguo.mobile;

import game.sanguo.core.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Exact v105 geometry controls: reducing work must not remove or move any content. */
public final class NativePreviewWorkTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        World w=ScenarioCatalog.load("huangjin-184",0,20260925L);
        byte[] authority=SaveCodec.encode(w);
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
        SceneMesh.TerrainWindow window=new SceneMesh.TerrainWindow((g.minX+g.maxX)/2,(g.minZ+g.maxZ)/2,200,200,86.256714f);
        long started=System.nanoTime();
        SceneMesh.BuildStats stats=new SceneMesh.BuildStats();
        List<SceneMesh> terrain=SceneMesh.ground(g,List.of(),window,stats);
        check(stats.sharedSamples>0&&stats.heightNanos>0,"production measurements reach shared normal work");
        double terrainMs=(System.nanoTime()-started)/1e6;
        MessageDigest md=MessageDigest.getInstance("SHA-256");ByteBuffer number=ByteBuffer.allocate(4);
        for(SceneMesh m:terrain){
            for(float[] a:new float[][]{m.vertices,m.surfaceData})for(float f:a){number.clear();md.update(number.putFloat(f).array());}
            for(int i:m.indices){number.clear();md.update(number.putInt(i).array());}
        }
        // Captured from unmodified ef54951 production classes, before optimization.
        check(HexFormat.of().formatHex(md.digest()).equals("e716c21a20f7043eed4ef2acaf6d77bc8c56a522a7fa61db65b89f6ab0a70b48"),"every national terrain vertex/index/material byte matches v105");
        FieldAssets assets=new FieldAssets(n->new FileInputStream("app/src/main/assets/3d/field/"+n));
        Set<Hex> excluded=Vegetation.exclusions(new MapSceneSnapshot(g,w,null,-1));
        started=System.nanoTime();List<SceneMesh> scenery=Vegetation.buildWindow(g,excluded,List.of(),assets,window);
        double sceneryMs=(System.nanoTime()-started)/1e6;
        List<String> triangles=new ArrayList<>();ByteBuffer triangle=ByteBuffer.allocate(3*13*4);
        for(SceneMesh source:scenery){SceneMesh m=source.distant;
            for(int j=0;j<m.indices.length;j+=3){triangle.clear();
                for(int k=0;k<3;k++){int v=m.indices[j+k];
                    for(int c=0;c<7;c++)triangle.putFloat(m.vertices[v*7+c]);
                    for(int c=0;c<2;c++)triangle.putFloat(m.uv[v*2+c]);
                    for(int c=0;c<4;c++)triangle.putFloat(m.tangents[v*4+c]);
                }
                triangles.add(HexFormat.of().formatHex(md.digest(triangle.array())));
            }
        }
        Collections.sort(triangles);md.reset();for(String hash:triangles)md.update(hash.getBytes(StandardCharsets.UTF_8));
        check(triangles.size()==236375,"all original distant scenery triangles retained");
        check(HexFormat.of().formatHex(md.digest()).equals("6fd66849a26cc23b509b753745920f673f3f9dc2aa20313e4759f31c225a8275"),"every scenery triangle/UV/normal matches v105, independent of batching");
        check(Vegetation.buildWindow(g,excluded,scenery,assets,window).equals(scenery),"stable overview reuses all batches");
        check(Arrays.equals(authority,SaveCodec.encode(w)),"complete authority/RNG/save untouched");
        System.out.println("terrainChunks="+terrain.size()+" sceneryChunks="+scenery.size()+" sceneryTriangles="+triangles.size()+" terrain_ms="+terrainMs+" scenery_ms="+sceneryMs+" HOST_ONLY");
        check(scenery.size()<=256,"national far scenery must not require hundreds of redundant small uploads");
        // Compare cached heights with the exact pre-existing evaluator at quarter positions,
        // negative coordinates, collisions, non-lattice positions and concurrent readers.
        var compute=TerrainSurface.class.getDeclaredMethod("compute",float.class,float.class);compute.setAccessible(true);
        float[] expected=new float[8192];Random random=new Random(311);
        float[] xs=new float[expected.length],zs=new float[expected.length];
        for(int i=0;i<expected.length;i++){xs[i]=(random.nextInt(900)-40)*.25f;zs[i]=(random.nextInt(900)-40)*.25f;expected[i]=(Float)compute.invoke(g.surface,xs[i],zs[i]);}
        Throwable[] failure={null};Thread worker=new Thread(()->{try{for(int pass=0;pass<3;pass++)for(int i=expected.length-1;i>=0;i--)check(Float.floatToIntBits(g.surface.sample(xs[i],zs[i]))==Float.floatToIntBits(expected[i]),"concurrent exact height");}catch(Throwable e){failure[0]=e;}});
        worker.start();for(int pass=0;pass<3;pass++)for(int i=0;i<expected.length;i++)check(Float.floatToIntBits(g.surface.sample(xs[i],zs[i]))==Float.floatToIntBits(expected[i]),"owner exact height");worker.join();if(failure[0]!=null)throw new AssertionError(failure[0]);
        SceneMesh large=scenery.get(0),a=new SceneMesh(new float[0],new int[0],0,0,1),b=new SceneMesh(new float[0],new int[0],0,0,1);
        a.chunkQ=large.chunkQ;a.chunkR=large.chunkR;b.chunkQ=large.chunkQ+8;b.chunkR=large.chunkR;
        Set<SceneMesh> wanted=Set.of(a,b);
        check(Vegetation.replacementPending(large,wanted,Set.of(a)),"split preserves old coverage until all replacements uploaded");
        check(!Vegetation.replacementPending(large,wanted,wanted),"split releases old only when complete");
        check(Vegetation.replacementPending(a,Set.of(large),Set.of()),"merge retains old until large upload");
        check(!Vegetation.replacementPending(a,Set.of(large),Set.of(large)),"merge releases replaced GPU mesh");
        System.out.println("PASS preview work: full geometry, bounded uploads, cache identity, concurrent samples, full authority equality");
    }
}
