package game.sanguo.mobile;

import game.sanguo.core.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** Shared immutable rest meshes and rigid-joint clips. Per-instance phase stays in the renderer. */
final class FieldAssets {
    interface Source { InputStream open(String name)throws IOException; }
    private final Source source;
    private record Joint(String name,int parent,int first,int count,float x,float y,float z) {}
    private record Rig(Joint[] parts,int vertices) {}
    private final Map<String,Rig> rigs=new HashMap<>();
    private final Map<String,List<Map<String,float[]>>> clips=new HashMap<>();
    private final Map<String,SceneMesh> rest=new LinkedHashMap<>(32,.75f,true);
    private long restBytes;
    private static long bytes(SceneMesh m){return 4L*(m.vertices.length+m.indices.length+(m.uv==null?0:m.uv.length)+(m.tangents==null?0:m.tangents.length));}
    FieldAssets(Source source)throws Exception {
        this.source=source;JSONObject r=json(source,"rigs-v124.json"),c=json(source,"clips.json");
        if(r.getInt("version")!=1||c.getInt("version")!=1||c.getInt("fps")!=12)throw new IOException("unsupported rigid animation version");
        // These bundled descriptions never change. Validate once and retain
        // primitive values, rather than reading JSON for every joint/pose.
        JSONObject models=r.getJSONObject("rigs"),motions=c.getJSONObject("clips");
        for(Iterator<String> names=models.keys();names.hasNext();){String model=names.next();
            JSONArray parts=models.getJSONObject(model).getJSONArray("parts");
            if(parts.length()<1||parts.length()>128)throw new IOException("rig/clip budget");
            Joint[] joints=new Joint[parts.length()];int covered=0;
            for(int i=0;i<joints.length;i++){
                JSONObject part=parts.getJSONObject(i);int parent=part.getInt("parent"),first=part.getInt("first"),count=part.getInt("count");
                if(first!=covered)throw new IOException("overlapping or missing rigid range");
                if(parent>=i||parent< -1||first<0||count<0||(long)first+count>30000)throw new IOException("rig range/parent");
                float[] p=vector(part.getJSONArray("pivot"),64);covered+=count;
                joints[i]=new Joint(part.getString("name"),parent,first,count,p[0],p[1],p[2]);
            }
            rigs.put(model,new Rig(joints,covered));
        }
        for(Iterator<String> names=motions.keys();names.hasNext();){String clip=names.next();
            JSONArray frames=motions.getJSONArray(clip);if(frames.length()!=12)throw new IOException("rig/clip budget");
            List<Map<String,float[]>> decoded=new ArrayList<>(12);
            for(int i=0;i<12;i++){
                JSONObject keys=frames.getJSONObject(i);Map<String,float[]> angles=new HashMap<>();
                for(Iterator<String> namesOfJoints=keys.keys();namesOfJoints.hasNext();){String name=namesOfJoints.next();angles.put(name,vector(keys.getJSONArray(name),Math.PI*4));}
                decoded.add(angles);
            }
            clips.put(clip,decoded);
        }
    }
    private static float[] vector(JSONArray values,double limit)throws Exception{
        if(values.length()!=3)throw new IOException("rig vector width");float[] result=new float[3];
        for(int k=0;k<3;k++){double value=values.getDouble(k);if(!Double.isFinite(value)||Math.abs(value)>limit)throw new IOException("rig numeric bounds");result[k]=(float)value;}
        return result;
    }
    private static JSONObject json(Source source,String name)throws Exception{
        try(InputStream in=source.open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>1_000_000)throw new IOException("animation budget");out.write(buffer,0,n);}
            return new JSONObject(out.toString("UTF-8"));
        }
    }
    synchronized SceneMesh mesh(String name)throws Exception{
        if(!name.matches("[A-Za-z0-9_-]{1,100}"))throw new IOException("asset ID rejected");
        SceneMesh value=rest.get(name);if(value==null){try(InputStream in=source.open((name.startsWith("cascade-")||name.startsWith("rock-ledge")||name.startsWith("rock-talus")?"v125/":name.startsWith("unit-")||name.startsWith("tree")||name.startsWith("shrub")||name.startsWith("rock-strata-v121")?"v124/":"")+name+".glb")){value=SiteGlb.read(in);}rest.put(name,value);restBytes+=bytes(value);
            Iterator<SceneMesh> entries=rest.values().iterator();while(restBytes>24L*1024*1024&&rest.size()>1){restBytes-=bytes(entries.next());entries.remove();}}return value;
    }
    static boolean farm(MapSceneSnapshot.Item item){return item.facility!=null&&item.facility.type.equals("domestic/FARM");}
    static long farmSurfaceKey(MapSceneSnapshot.Ground ground,Hex at){
        long key=1469598103934665603L;
        float x=ground.grid.x(at),z=ground.grid.z(at);
        for(int r=-4;r<=4;r++)for(int q=-4;q<=4;q++)key=(key^Float.floatToIntBits(ground.surface.meshHeight(x+q*.125f,z+r*.125f)))*1099511628211L;
        return key;
    }
    /** Farm earth/rows use a conforming instance mesh; the shared rest GLB stays immutable. */
    static SceneMesh conformFarm(SceneMesh source,MapSceneSnapshot.Ground ground,Hex at){
        float[] vertices=source.vertices.clone();float x=ground.grid.x(at),z=ground.grid.z(at),base=ground.surface.meshHeight(x,z);
        for(int i=0;i<vertices.length;i+=7)vertices[i+1]+=ground.surface.meshHeight(x+vertices[i],z+vertices[i+2])-base;
        SceneMesh mesh=new SceneMesh(vertices,source.indices,source.x,source.z,source.radius);mesh.uv=source.uv;mesh.generateTangents();return mesh;
    }
    static String facility(MapSceneSnapshot.FacilityState state,int lod){
        return state.type.replace('/','-')+"-"+Math.max(1,state.level)+"-lod"+lod;
    }
    static String unit(UnitVisual unit,boolean naval,int lod){
        return "unit-"+(naval?(unit.mission?Army.Ship.BOAT:unit.ship).name():unit.mission?"transport":unit.weapon.name())+"-lod"+Math.min(1,lod);
    }
    static int count(UnitVisual unit,boolean naval,int lod){
        return unit.representatives(naval,unit.troops,lod);
    }
    synchronized SceneMesh pose(String model,String clip,int frame,int count)throws Exception{
        if(count<1||count>8)throw new IOException("formation budget");
        SceneMesh source=mesh(model);Rig rig=rigs.get(model);List<Map<String,float[]>> frames=clips.get(clip);
        if(rig==null||frames==null)throw new IOException("unknown rig/clip");
        Joint[] parts=rig.parts;Map<String,float[]> keys=frames.get(Math.floorMod(frame,frames.size()));
        if(rig.vertices!=source.vertices.length/7)throw new IOException("incomplete rigid coverage");
        float[][] matrices=new float[parts.length][];
        float[] posed=source.vertices.clone();
        float[] tangents=source.authoredTangentFrame?new float[source.tangents.length]:null;
        for(int i=0;i<parts.length;i++){
            Joint part=parts[i];int parent=part.parent,first=part.first,length=part.count;float[] angles=keys.get(part.name);
            float rx=angles==null?0:angles[0],ry=angles==null?0:angles[1],rz=angles==null?0:angles[2];
            float[] local=rotation(rx,ry,rz,part.x,part.y,part.z);
            matrices[i]=parent<0?local:multiply(matrices[parent],local);
            float[] m=matrices[i];
            if(tangents!=null)rotateFrames(source.tangents,tangents,first,length,m);
            for(int v=first;v<first+length;v++){
                float x=source.vertices[v*7],y=source.vertices[v*7+1],z=source.vertices[v*7+2];
                posed[v*7]=m[0]*x+m[4]*y+m[8]*z+m[12];
                posed[v*7+1]=m[1]*x+m[5]*y+m[9]*z+m[13];
                posed[v*7+2]=m[2]*x+m[6]*y+m[10]*z+m[14];
            }
        }
        if(!clip.equals("defeat")&&!clip.equals("hit")){
            float contact=Float.POSITIVE_INFINITY;boolean mounted=model.contains("CAVALRY");
            for(Joint part:parts){
                if(mounted?part.name.startsWith("horseShin"):part.name.startsWith("shin"))
                    for(int v=part.first,end=v+part.count;v<end;v++)contact=Math.min(contact,posed[v*7+1]);
            }
            if(Float.isFinite(contact))for(int v=1;v<posed.length;v+=7)posed[v]-=contact;
        }
        // Production renders one shared member through GPU instances. Reuse
        // immutable topology/UV and the already-private posed vertices.
        if(count==1){SceneMesh mesh=new SceneMesh(posed,source.indices,0,0,1);mesh.uv=source.uv;if(tangents==null)mesh.generateTangents();else mesh.tangents=tangents;return mesh;}
        // Legacy CPU merged API is retained for compatibility tests; R10 runtime requests count=1.
        // One merged draw per formation. No entity for each soldier; no troop-count expansion.
        float[] vertices=new float[posed.length*count];int[] indices=new int[source.indices.length*count];
        float[] uv=new float[source.uv.length*count];float scale=count>1?.78f:1;
        for(int member=0;member<count;member++){
            int columns=count==8?4:2;float dx=count==1?0:(member%columns-(columns-1)*.5f)*(count==8?.21f:.32f);
            float dz=count==1?0:(member/columns-(count/columns-1)*.5f)*.31f;
            int offset=member*posed.length;System.arraycopy(posed,0,vertices,offset,posed.length);
            for(int v=0;v<posed.length;v+=7){vertices[offset+v]=posed[v]*scale+dx;vertices[offset+v+1]=posed[v+1]*scale;vertices[offset+v+2]=posed[v+2]*scale+dz;}
            for(int i=0;i<source.indices.length;i++)indices[member*source.indices.length+i]=source.indices[i]+member*(posed.length/7);
            System.arraycopy(source.uv,0,uv,member*source.uv.length,source.uv.length);
        }
        SceneMesh mesh=new SceneMesh(vertices,indices,0,0,1);mesh.uv=uv;
        if(tangents==null)mesh.generateTangents();else{
            mesh.tangents=new float[tangents.length*count];
            for(int member=0;member<count;member++)System.arraycopy(tangents,0,mesh.tangents,member*tangents.length,tangents.length);
        }
        return mesh;
    }
    private static float[] rotation(float x,float y,float z,float px,float py,float pz){
        float cx=(float)Math.cos(x),sx=(float)Math.sin(x),cy=(float)Math.cos(y),sy=(float)Math.sin(y),cz=(float)Math.cos(z),sz=(float)Math.sin(z);
        float[] m={cy*cz,cy*sz,-sy,0,sx*sy*cz-cx*sz,sx*sy*sz+cx*cz,sx*cy,0,cx*sy*cz+sx*sz,cx*sy*sz-sx*cz,cx*cy,0,0,0,0,1};
        m[12]=px-m[0]*px-m[4]*py-m[8]*pz;m[13]=py-m[1]*px-m[5]*py-m[9]*pz;m[14]=pz-m[2]*px-m[6]*py-m[10]*pz;return m;
    }
    private static float[] multiply(float[] a,float[] b){float[] c=new float[16];for(int col=0;col<4;col++)for(int row=0;row<4;row++)for(int k=0;k<4;k++)c[col*4+row]+=a[k*4+row]*b[col*4+k];return c;}
    /** Compose a rigid joint rotation with each authored normal frame. No
     * per-triangle reconstruction, normal scratch or per-vertex square roots. */
    private static void rotateFrames(float[] source,float[] out,int first,int count,float[] m){
        // Walking animates limbs, but most indexed body/horse vertices keep the
        // exact identity rotation. Preserve their authored frames with one copy.
        if(m[0]==1&&m[5]==1&&m[10]==1&&m[1]==0&&m[2]==0&&m[4]==0&&m[6]==0&&m[8]==0&&m[9]==0){
            System.arraycopy(source,first*4,out,first*4,count*4);return;
        }
        float x,y,z,w,trace=m[0]+m[5]+m[10];
        if(trace>0){float s=(float)Math.sqrt(trace+1)*2;w=s*.25f;x=(m[6]-m[9])/s;y=(m[8]-m[2])/s;z=(m[1]-m[4])/s;}
        else if(m[0]>m[5]&&m[0]>m[10]){float s=(float)Math.sqrt(1+m[0]-m[5]-m[10])*2;x=s*.25f;y=(m[4]+m[1])/s;z=(m[8]+m[2])/s;w=(m[6]-m[9])/s;}
        else if(m[5]>m[10]){float s=(float)Math.sqrt(1+m[5]-m[0]-m[10])*2;y=s*.25f;x=(m[4]+m[1])/s;z=(m[9]+m[6])/s;w=(m[8]-m[2])/s;}
        else{float s=(float)Math.sqrt(1+m[10]-m[0]-m[5])*2;z=s*.25f;x=(m[8]+m[2])/s;y=(m[9]+m[6])/s;w=(m[1]-m[4])/s;}
        float inv=1/(float)Math.sqrt(x*x+y*y+z*z+w*w);x*=inv;y*=inv;z*=inv;w*=inv;
        for(int v=first*4,end=(first+count)*4;v<end;v+=4){
            float a=source[v],b=source[v+1],c=source[v+2],d=source[v+3];
            float X=w*a+x*d+y*c-z*b,Y=w*b-x*c+y*d+z*a,Z=w*c+x*b-y*a+z*d,W=w*d-x*a-y*b-z*c;
            // Filament reserves quaternion.w's sign for handedness. q and -q
            // represent the same rotation; bundled frames use positive handedness.
            float sign=W<0?-1:1;out[v]=X*sign;out[v+1]=Y*sign;out[v+2]=Z*sign;out[v+3]=Math.max(.00001f,Math.abs(W));
        }
    }
}
