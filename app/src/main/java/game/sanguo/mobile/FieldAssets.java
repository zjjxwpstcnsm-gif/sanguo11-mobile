package game.sanguo.mobile;

import game.sanguo.core.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** Shared immutable rest meshes and rigid-joint clips. Per-instance phase stays in the renderer. */
final class FieldAssets {
    interface Source { InputStream open(String name)throws IOException; }
    private final Source source;
    private final JSONObject rigs,clips;
    private final Map<String,SceneMesh> rest=new HashMap<>();
    FieldAssets(Source source)throws Exception {
        this.source=source;rigs=json(source,"rigs.json").getJSONObject("rigs");clips=json(source,"clips.json").getJSONObject("clips");
    }
    private static JSONObject json(Source source,String name)throws Exception{
        try(InputStream in=source.open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>1_000_000)throw new IOException("animation budget");out.write(buffer,0,n);}
            return new JSONObject(out.toString("UTF-8"));
        }
    }
    SceneMesh mesh(String name)throws Exception{
        SceneMesh value=rest.get(name);if(value==null){try(InputStream in=source.open(name+".glb")){value=SiteGlb.read(in);}rest.put(name,value);}return value;
    }
    static String facility(MapSceneSnapshot.FacilityState state,int lod){
        return state.type.replace('/','-')+"-"+Math.max(1,state.level)+"-lod"+lod;
    }
    static String unit(UnitVisual unit,boolean naval,int lod){
        return "unit-"+(naval?(unit.mission?Army.Ship.BOAT:unit.ship).name():unit.mission?"transport":unit.weapon.name())+"-lod"+Math.min(1,lod);
    }
    static int count(UnitVisual unit,boolean naval,int lod){
        if(naval||unit.mission||Army.siegeWeapon(unit.weapon))return 1;
        return lod==2?1:unit.weapon==World.Weapon.CAVALRY?(lod==0?4:2):(lod==0?8:4);
    }
    SceneMesh pose(String model,String clip,int frame,int count)throws Exception{
        SceneMesh source=mesh(model);JSONObject rig=rigs.getJSONObject(model);JSONArray parts=rig.getJSONArray("parts");
        JSONArray frames=clips.getJSONArray(clip);JSONObject keys=frames.getJSONObject(Math.floorMod(frame,frames.length()));
        float[][] matrices=new float[parts.length()][];
        float[] posed=source.vertices.clone();
        for(int i=0;i<parts.length();i++){
            JSONObject part=parts.getJSONObject(i);int parent=part.getInt("parent"),first=part.getInt("first"),length=part.getInt("count");
            if(parent>=i||parent< -1||first<0||length<0||(long)(first+length)*7>posed.length)throw new IOException("rig range/parent");
            JSONArray pivot=part.getJSONArray("pivot"),angles=keys.optJSONArray(part.getString("name"));
            float rx=angles==null?0:(float)angles.getDouble(0),ry=angles==null?0:(float)angles.getDouble(1),rz=angles==null?0:(float)angles.getDouble(2);
            float[] local=rotation(rx,ry,rz,(float)pivot.getDouble(0),(float)pivot.getDouble(1),(float)pivot.getDouble(2));
            matrices[i]=parent<0?local:multiply(matrices[parent],local);
            float[] m=matrices[i];
            for(int v=first;v<first+length;v++){
                float x=source.vertices[v*7],y=source.vertices[v*7+1],z=source.vertices[v*7+2];
                posed[v*7]=m[0]*x+m[4]*y+m[8]*z+m[12];
                posed[v*7+1]=m[1]*x+m[5]*y+m[9]*z+m[13];
                posed[v*7+2]=m[2]*x+m[6]*y+m[10]*z+m[14];
            }
        }
        // One merged draw per formation. No entity for each soldier; no troop-count expansion.
        List<Float> vertices=new ArrayList<>(posed.length*count);List<Integer> indices=new ArrayList<>(source.indices.length*count);
        float[] uv=new float[source.uv.length*count];float scale=count>1?.78f:1;
        for(int member=0;member<count;member++){
            int columns=count==8?4:2;float dx=count==1?0:(member%columns-(columns-1)*.5f)*(count==8?.21f:.32f);
            float dz=count==1?0:(member/columns-(count/columns-1)*.5f)*.31f;
            for(int v=0;v<posed.length;v+=7){vertices.add(posed[v]*scale+dx);vertices.add(posed[v+1]*scale);vertices.add(posed[v+2]*scale+dz);for(int k=3;k<7;k++)vertices.add(posed[v+k]);}
            for(int index:source.indices)indices.add(index+member*(posed.length/7));
            System.arraycopy(source.uv,0,uv,member*source.uv.length,source.uv.length);
        }
        SceneMesh mesh=new SceneMesh(vertices,indices,0,0,1);mesh.uv=uv;return mesh;
    }
    private static float[] rotation(float x,float y,float z,float px,float py,float pz){
        float cx=(float)Math.cos(x),sx=(float)Math.sin(x),cy=(float)Math.cos(y),sy=(float)Math.sin(y),cz=(float)Math.cos(z),sz=(float)Math.sin(z);
        float[] m={cy*cz,cy*sz,-sy,0,sx*sy*cz-cx*sz,sx*sy*sz+cx*cz,sx*cy,0,cx*sy*cz+sx*sz,cx*sy*sz-sx*cz,cx*cy,0,0,0,0,1};
        m[12]=px-m[0]*px-m[4]*py-m[8]*pz;m[13]=py-m[1]*px-m[5]*py-m[9]*pz;m[14]=pz-m[2]*px-m[6]*py-m[10]*pz;return m;
    }
    private static float[] multiply(float[] a,float[] b){float[] c=new float[16];for(int col=0;col<4;col++)for(int row=0;row<4;row++)for(int k=0;k<4;k++)c[col*4+row]+=a[k*4+row]*b[col*4+k];return c;}
}
