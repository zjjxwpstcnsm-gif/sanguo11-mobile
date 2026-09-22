package game.sanguo.mobile;

import java.io.*;
import java.nio.*;
import java.util.*;
import org.json.*;

/** Deliberately restricted offline GLB contract, not a general glTF loader.
 * Rejects unsupported data before touching GPU resources. All offsets are range checked. */
final class SiteGlb {
    static SceneMesh read(InputStream in)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] block=new byte[8192];int n;
        while((n=in.read(block))!=-1){if(out.size()+n>2_000_000)throw new IOException("site asset budget");out.write(block,0,n);}
        byte[] bytes=out.toByteArray();ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if(b.remaining()<28||b.getInt()!=0x46546c67||b.getInt()!=2||b.getInt()!=bytes.length)throw new IOException("GLB header");
        int jl=b.getInt();if(b.getInt()!=0x4e4f534a||jl<0||jl>b.remaining()-8)throw new IOException("GLB JSON");
        byte[] j=new byte[jl];b.get(j);JSONObject doc=new JSONObject(new String(j,java.nio.charset.StandardCharsets.UTF_8));
        if(doc.optJSONArray("extensionsRequired")!=null&&doc.getJSONArray("extensionsRequired").length()!=0||doc.has("animations"))throw new IOException("unsupported GLB extension/animation");
        if(doc.getJSONArray("buffers").length()!=1||doc.getJSONArray("nodes").length()!=1)throw new IOException("single baked node required");
        JSONObject node=doc.getJSONArray("nodes").getJSONObject(0);
        if(node.getInt("mesh")!=0||node.has("matrix")||node.has("rotation")||node.has("scale")||node.has("translation")||node.has("skin"))throw new IOException("bake node transforms offline");
        int bl=b.getInt();if(b.getInt()!=0x004e4942||bl!=b.remaining())throw new IOException("GLB BIN");int start=b.position();
        if(doc.getJSONArray("meshes").length()!=1)throw new IOException("one baked mesh required");
        JSONArray ps=doc.getJSONArray("meshes").getJSONObject(0).getJSONArray("primitives");
        if(ps.length()!=1)throw new IOException("one material primitive required");JSONObject p=ps.getJSONObject(0),a=p.getJSONObject("attributes");
        if(p.optInt("mode",4)!=4)throw new IOException("triangles required");
        float[] positions=floats(doc,b,start,bl,a.getInt("POSITION"),3,"VEC3");
        float[] colors=floats(doc,b,start,bl,a.getInt("COLOR_0"),4,"VEC4");
        float[] uv=floats(doc,b,start,bl,a.getInt("TEXCOORD_0"),2,"VEC2");
        int count=positions.length/3;if(colors.length!=count*4||uv.length!=count*2||count==0)throw new IOException("attribute counts");
        List<Float> v=new ArrayList<>(count*7);for(int i=0;i<count;i++){for(int k=0;k<3;k++)v.add(positions[i*3+k]);for(int k=0;k<4;k++)v.add(colors[i*4+k]);}
        JSONObject index=doc.getJSONArray("accessors").getJSONObject(p.getInt("indices"));
        if(index.getInt("componentType")!=5125||!index.getString("type").equals("SCALAR"))throw new IOException("uint32 indices required");
        int ni=index.getInt("count");if(ni<3||ni%3!=0)throw new IOException("triangle index count");
        ByteBuffer ib=access(doc,b,start,bl,index,ni,4);List<Integer> indices=new ArrayList<>();
        for(int i=0;i<ni;i++){int value=ib.getInt();if(value<0||value>=count)throw new IOException("index range");indices.add(value);}
        SceneMesh mesh=new SceneMesh(v,indices,0,0,2);mesh.uv=uv;
        if(a.has("NORMAL"))mesh.setNormals(floats(doc,b,start,bl,a.getInt("NORMAL"),3,"VEC3"));
        else mesh.generateTangents(); // Legal missing NORMAL; authored split edges stay hard.
        return mesh;
    }
    private static float[] floats(JSONObject doc,ByteBuffer b,int start,int length,int id,int width,String type)throws Exception{
        JSONObject a=doc.getJSONArray("accessors").getJSONObject(id);if(a.getInt("componentType")!=5126||!a.getString("type").equals(type))throw new IOException("float attribute required");
        int count=a.getInt("count");ByteBuffer v=access(doc,b,start,length,a,count,width*4);float[] out=new float[count*width];
        for(int i=0;i<out.length;i++){out[i]=v.getFloat();if(!Float.isFinite(out[i]))throw new IOException("nonfinite vertex");}return out;
    }
    private static ByteBuffer access(JSONObject doc,ByteBuffer b,int start,int length,JSONObject a,int count,int stride)throws Exception{
        if(count<0||count>100000||a.has("sparse"))throw new IOException("accessor budget");
        JSONObject v=doc.getJSONArray("bufferViews").getJSONObject(a.getInt("bufferView"));
        int offset=v.optInt("byteOffset",0),inner=a.optInt("byteOffset",0),size=v.getInt("byteLength");long need=(long)count*stride;
        if(v.getInt("buffer")!=0||v.optInt("byteStride",stride)!=stride||offset<0||inner<0||size<0||offset+(long)size>length||inner+need>size)throw new IOException("buffer view bounds");
        ByteBuffer copy=b.duplicate().order(ByteOrder.LITTLE_ENDIAN);copy.position(start+offset+inner);copy.limit((int)(start+offset+inner+need));return copy;
    }
}
