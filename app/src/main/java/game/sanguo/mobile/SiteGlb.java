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
        int jl=b.getInt();if(b.getInt()!=0x4e4f534a||jl<0||jl%4!=0||jl>b.remaining()-8)throw new IOException("GLB JSON");
        byte[] j=new byte[jl];b.get(j);JSONObject doc=new JSONObject(new String(j,java.nio.charset.StandardCharsets.UTF_8));
        contract(doc);
        if(doc.optJSONArray("extensionsRequired")!=null&&doc.getJSONArray("extensionsRequired").length()!=0||doc.has("animations"))throw new IOException("unsupported GLB extension/animation");
        if(doc.getJSONArray("buffers").length()!=1||doc.getJSONArray("nodes").length()!=1)throw new IOException("single baked node required");
        JSONObject node=doc.getJSONArray("nodes").getJSONObject(0);
        if(node.getInt("mesh")!=0||node.has("matrix")||node.has("rotation")||node.has("scale")||node.has("translation")||node.has("skin"))throw new IOException("bake node transforms offline");
        int bl=b.getInt();if(b.getInt()!=0x004e4942||bl<0||bl%4!=0||bl!=b.remaining())throw new IOException("GLB BIN");int start=b.position();
        int declared=doc.getJSONArray("buffers").getJSONObject(0).getInt("byteLength");
        if(declared<0||declared>bl||bl-declared>3)throw new IOException("buffer declared length");
        image(doc,b,start,declared);
        if(doc.getJSONArray("meshes").length()!=1)throw new IOException("one baked mesh required");
        JSONArray ps=doc.getJSONArray("meshes").getJSONObject(0).getJSONArray("primitives");
        if(ps.length()!=1)throw new IOException("one material primitive required");JSONObject p=ps.getJSONObject(0),a=p.getJSONObject("attributes");
        if(p.optInt("material",-1)!=0||p.has("targets")||a.length()<3||a.length()>4||a.length()==4&&!a.has("NORMAL"))throw new IOException("unsupported primitive attributes/material");
        if(p.optInt("mode",4)!=4)throw new IOException("triangles required");
        float[] positions=floats(doc,b,start,bl,a.getInt("POSITION"),3,"VEC3");
        float[] colors=floats(doc,b,start,bl,a.getInt("COLOR_0"),4,"VEC4");
        float[] uv=floats(doc,b,start,bl,a.getInt("TEXCOORD_0"),2,"VEC2");
        int count=positions.length/3;if(colors.length!=count*4||uv.length!=count*2||count==0||count>30000)throw new IOException("attribute counts");
        List<Float> v=new ArrayList<>(count*7);for(int i=0;i<count;i++){for(int k=0;k<3;k++)v.add(positions[i*3+k]);for(int k=0;k<4;k++)v.add(colors[i*4+k]);}
        JSONObject index=doc.getJSONArray("accessors").getJSONObject(p.getInt("indices"));
        if(index.getInt("componentType")!=5125||!index.getString("type").equals("SCALAR"))throw new IOException("uint32 indices required");
        int ni=index.getInt("count");if(ni<3||ni%3!=0)throw new IOException("triangle index count");
        ByteBuffer ib=access(doc,b,start,bl,index,ni,4);List<Integer> indices=new ArrayList<>();
        for(int i=0;i<ni;i++){int value=ib.getInt();if(value<0||value>=count)throw new IOException("index range");indices.add(value);}
        SceneMesh mesh=new SceneMesh(v,indices,0,0,2);mesh.uv=uv;
        if(a.has("NORMAL")){float[] normals=floats(doc,b,start,bl,a.getInt("NORMAL"),3,"VEC3");if(normals.length!=count*3)throw new IOException("normal count");for(int i=0;i<normals.length;i+=3){float norm=normals[i]*normals[i]+normals[i+1]*normals[i+1]+normals[i+2]*normals[i+2];if(norm<.98f||norm>1.02f)throw new IOException("normal length");}mesh.setNormals(normals);}
        else mesh.generateTangents(); // Legal missing NORMAL; authored split edges stay hard.
        return mesh;
    }
    /** Versioned baked subset: reject silently dropped semantics before allocation/upload. */
    private static void contract(JSONObject doc)throws Exception{
        rejectExternal(doc);
        JSONObject asset=doc.getJSONObject("asset");
        if(!"2.0".equals(asset.getString("version"))||asset.has("minVersion")||doc.has("skins")||doc.has("cameras"))throw new IOException("unsupported asset version/skin/camera");
        if(doc.getJSONArray("scenes").length()!=1||doc.optInt("scene",0)!=0||doc.getJSONArray("scenes").getJSONObject(0).getJSONArray("nodes").length()!=1||doc.getJSONArray("scenes").getJSONObject(0).getJSONArray("nodes").getInt(0)!=0)throw new IOException("single scene root required");
        if(doc.getJSONArray("nodes").getJSONObject(0).has("children"))throw new IOException("bake hierarchy offline");
        if(doc.getJSONArray("materials").length()!=1||doc.getJSONArray("textures").length()!=1||doc.getJSONArray("images").length()!=1||doc.has("samplers"))throw new IOException("shared atlas subset");
        JSONObject m=doc.getJSONArray("materials").getJSONObject(0),pbr=m.getJSONObject("pbrMetallicRoughness"),t=pbr.getJSONObject("baseColorTexture");
        if(!m.optString("alphaMode","OPAQUE").equals("OPAQUE")||m.has("alphaCutoff")||m.has("normalTexture")||m.has("occlusionTexture")||m.has("emissiveTexture")||m.has("emissiveFactor")||!m.optBoolean("doubleSided",false)||pbr.has("baseColorFactor")||pbr.has("metallicRoughnessTexture")||pbr.optDouble("metallicFactor",1)!=0||pbr.optDouble("roughnessFactor",1)!=1||t.getInt("index")!=0||t.optInt("texCoord",0)!=0||doc.getJSONArray("textures").getJSONObject(0).getInt("source")!=0)throw new IOException("unsupported material; bake to atlas offline");
    }
    private static void rejectExternal(Object value)throws Exception{
        if(value instanceof JSONObject){JSONObject o=(JSONObject)value;Iterator<String> keys=o.keys();while(keys.hasNext()){String k=keys.next();if(k.equals("uri")||k.equals("extensions"))throw new IOException("external URI/extension rejected");rejectExternal(o.get(k));}}
        else if(value instanceof JSONArray){JSONArray a=(JSONArray)value;for(int i=0;i<a.length();i++)rejectExternal(a.get(i));}
    }
    private static void image(JSONObject doc,ByteBuffer b,int start,int length)throws Exception{
        JSONObject image=doc.getJSONArray("images").getJSONObject(0);
        if(!image.getString("mimeType").equals("image/png"))throw new IOException("PNG atlas required");
        JSONObject v=doc.getJSONArray("bufferViews").getJSONObject(image.getInt("bufferView"));
        int off=v.optInt("byteOffset",0),size=v.getInt("byteLength");
        if(v.getInt("buffer")!=0||off<0||size<33||off+(long)size>length)throw new IOException("image bounds");
        ByteBuffer png=b.duplicate().order(ByteOrder.BIG_ENDIAN);png.position(start+off);png.limit(start+off+size);
        if(png.getLong()!=0x89504e470d0a1a0aL||png.getInt()!=13||png.getInt()!=0x49484452)throw new IOException("PNG header");
        int w=png.getInt(),h=png.getInt();if(w<1||h<1||w>2048||h>2048||(long)w*h>1048576)throw new IOException("texture budget");
        png.position(start+off+8);boolean ended=false;
        while(png.remaining()>=12){int n=png.getInt();if(n<0||n>png.remaining()-8)throw new IOException("PNG chunk bounds");int type=png.getInt();java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(b.array(),png.position()-4,n+4);png.position(png.position()+n);if((int)crc.getValue()!=png.getInt())throw new IOException("PNG CRC");if(type==0x49454e44){ended=n==0&&png.remaining()==0;break;}}
        if(!ended)throw new IOException("PNG truncated");
    }
    private static float[] floats(JSONObject doc,ByteBuffer b,int start,int length,int id,int width,String type)throws Exception{
        JSONObject a=doc.getJSONArray("accessors").getJSONObject(id);if(a.getInt("componentType")!=5126||!a.getString("type").equals(type))throw new IOException("float attribute required");
        int count=a.getInt("count");ByteBuffer v=access(doc,b,start,length,a,count,width*4);float[] out=new float[count*width];
        for(int i=0;i<out.length;i++){out[i]=v.getFloat();if(!Float.isFinite(out[i])||Math.abs(out[i])>64||(!type.equals("VEC3")&&(out[i]<0||out[i]>1)))throw new IOException("nonfinite vertex");}return out;
    }
    private static ByteBuffer access(JSONObject doc,ByteBuffer b,int start,int length,JSONObject a,int count,int stride)throws Exception{
        if(count<0||count>100000||a.has("sparse")||a.optBoolean("normalized",false))throw new IOException("accessor budget");
        JSONObject v=doc.getJSONArray("bufferViews").getJSONObject(a.getInt("bufferView"));
        int offset=v.optInt("byteOffset",0),inner=a.optInt("byteOffset",0),size=v.getInt("byteLength");long need=(long)count*stride;
        if(v.getInt("buffer")!=0||v.optInt("byteStride",stride)!=stride||offset<0||inner<0||offset%4!=0||inner%4!=0||size<0||offset+(long)size>length||inner+need>size)throw new IOException("buffer view bounds");
        ByteBuffer copy=b.duplicate().order(ByteOrder.LITTLE_ENDIAN);copy.position(start+offset+inner);copy.limit((int)(start+offset+inner+need));return copy;
    }
}
