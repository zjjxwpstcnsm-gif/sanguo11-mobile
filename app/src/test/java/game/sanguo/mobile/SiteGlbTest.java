package game.sanguo.mobile;
import java.io.*;
import java.nio.file.*;
import java.nio.*;
import java.util.*;
/** Host loader test uses org.json's reference implementation; Android supplies org.json at runtime. */
public final class SiteGlbTest {
    static int checks;
    static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    static void rejects(byte[] b)throws Exception{try{SiteGlb.read(new ByteArrayInputStream(b));throw new AssertionError("invalid file accepted");}catch(IOException|org.json.JSONException expected){checks++;}}
    public static void main(String[] args)throws Exception{
        for(String family:new String[]{"city0","city1","city2","port","gate"}){
            int previous=Integer.MAX_VALUE;
            for(int lod=0;lod<3;lod++){
                byte[] bytes=Files.readAllBytes(Path.of("app/src/main/assets/3d/sites/"+family+"-lod"+lod+".glb"));SceneMesh m=SiteGlb.read(new ByteArrayInputStream(bytes));
                check(m.uv.length==m.vertices.length/7*2,"UV count");check(m.indices.length<=previous,"LOD never increases triangles");previous=m.indices.length;
                for(float uv:m.uv)check(uv>=0&&uv<=1,"atlas range");
                for(int i:m.indices)check(i>=0&&i<m.vertices.length/7,"index bounds");
                float min=Float.MAX_VALUE,max=-min;for(int i=1;i<m.vertices.length;i+=7){min=Math.min(min,m.vertices[i]);max=Math.max(max,m.vertices[i]);}check(max-min>.3,"true volumetric model");
                rejects(Arrays.copyOf(bytes,bytes.length-4));byte[] bad=bytes.clone();bad[0]=0;rejects(bad);
                ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);int offset=28+b.getInt(12);b.putFloat(offset,Float.NaN);rejects(bytes);
            }
        }
        System.out.println("PASS S03 GLB: "+checks+" checks; all 15 real runtime GLBs loaded; invalid/truncated/nonfinite files rejected");
    }
}
