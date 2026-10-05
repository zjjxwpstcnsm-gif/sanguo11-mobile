package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
import java.security.MessageDigest;

public final class NativeR03Test {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void near(float a,float b,String why){check(Math.abs(a-b)<.0003f,why+" "+a+" != "+b);}
    static String key(SceneMesh m){return m.chunkQ+":"+m.chunkR;}
    static void geometry(MapSceneSnapshot.Ground g,List<SceneMesh> meshes){
        Map<String,float[]> shared=new HashMap<>();SceneCamera c=new SceneCamera();c.width=900;c.height=900;c.tilt=90;
        for(SceneMesh m:meshes){
            check(m.distant==null,"only requested CPU precision retained");
            for(int index:m.indices)check(index>=0&&index<m.vertices.length/7,"valid index");
            for(float f:m.vertices)check(Float.isFinite(f),"finite vertex");
            for(int i=0;i<m.vertices.length/7;i++){
                int v=i*7;float x=m.vertices[v],y=m.vertices[v+1],z=m.vertices[v+2];
                near(g.surface.meshHeight(x,z),y,"all levels preserve exact surface anchor");
                // Canonical boundary vertices lie on the global half-unit lattice.
                if(Math.abs(x*2-Math.round(x*2))>.00001||Math.abs(z*2-Math.round(z*2))>.00001)continue;
                float[] data=new float[11];data[0]=y;System.arraycopy(m.surfaceData,i*8,data,1,6);System.arraycopy(m.vertices,v+3,data,7,4);
                float[] old=shared.putIfAbsent(x+":"+z,data);if(old!=null)for(int j=0;j<data.length;j++)near(data[j],old[j],"shared height/normal/UV/weight");
            }
            int v=(m.vertices.length/7/2)*7;float x=m.vertices[v],z=m.vertices[v+2];c.x=x;c.z=z;
            near(g.surface.rayHeight(c,450,450),ScenePicking.hit(c,m,0,0,0,0,1,450,450),"actual rendered triangle hit");
            for(SceneMesh n:meshes)if(Math.abs(m.chunkQ-n.chunkQ)+Math.abs(m.chunkR-n.chunkR)==16)check(Math.abs(m.terrainLod-n.terrainLod)<=1,"neighbor LOD delta <=1");
        }
    }
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.all().get(0);byte[] authority=SaveCodec.encode(w);MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
        float x=g.grid.x(w.home().hex),z=g.grid.z(w.home().hex);
        SceneCamera focus=new SceneCamera();focus.x=x;focus.z=z;focus.clampTo(g);
        near(focus.x,x,"focus X independent of old CPU window");near(focus.z,z,"focus Z independent of old CPU window");
        for(World.City site:w.cities){focus.x=g.grid.x(site.hex);focus.z=g.grid.z(site.hex);focus.clampTo(g);near(focus.x,g.grid.x(site.hex),"all site focus X");near(focus.z,g.grid.z(site.hex),"all site focus Z");}
        focus.x=-10000;focus.z=10000;focus.clampTo(g);near(focus.x,g.minX,"whole-map west bound");near(focus.z,g.maxZ,"whole-map south bound");
        for(float span:new float[]{8,24,70}){
            SceneMesh.TerrainWindow window=new SceneMesh.TerrainWindow(x,z,span,span,span);
            List<SceneMesh> meshes=SceneMesh.ground(g,Collections.emptyList(),window);geometry(g,meshes);
            if(span==8)check(meshes.size()<40,"near cache is view bounded, not nationwide");
            List<SceneMesh> same=SceneMesh.ground(g,meshes,window);for(int i=0;i<meshes.size();i++)check(meshes.get(i)==same.get(i),"unchanged chunk identity");
            System.out.println("WINDOW span="+span+" chunks="+meshes.size()+" triangles="+meshes.stream().mapToInt(m->m.indices.length/3).sum());
        }
        check(Arrays.equals(authority,SaveCodec.encode(w)),"national terrain/reachability/sites/occupancy/RNG save identical");
        System.out.println("AUTHORITY SHA256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(authority)));
        for(boolean staggered:new boolean[]{false,true}){
            World custom=new World(96,96);custom.columnStaggered=staggered;
            for(int q=0;q<96;q++)for(int r=0;r<96;r++)custom.terrain[q][r]=q==32?World.Terrain.ROAD:r==48?World.Terrain.WATER:World.Terrain.MOUNTAIN;
            MapSceneSnapshot.Ground before=new MapSceneSnapshot.Ground(custom);
            SceneMesh.TerrainWindow window=new SceneMesh.TerrainWindow(48,48,160,160,70);
            List<SceneMesh> old=SceneMesh.ground(before,List.of(),window);
            custom.terrain[47][47]=World.Terrain.WATER;custom.mapRevision++;
            MapSceneSnapshot.Ground after=new MapSceneSnapshot.Ground(custom);
            List<SceneMesh> cached=SceneMesh.ground(after,old,window),fresh=SceneMesh.ground(after,List.of(),window);
            Map<String,SceneMesh> prior=new HashMap<>();for(SceneMesh m:old)prior.put(key(m),m);int reused=0,changed=0;
            for(int i=0;i<cached.size();i++){
                SceneMesh a=cached.get(i),b=fresh.get(i);check(Arrays.equals(a.vertices,b.vertices)&&Arrays.equals(a.surfaceData,b.surfaceData)&&Arrays.equals(a.indices,b.indices),"patch equals full rebuild");
                boolean affected=47>=a.chunkQ-8&&47<a.chunkQ+24&&47>=a.chunkR-8&&47<a.chunkR+24;
                if(!affected){check(a==prior.get(key(a)),"outside declared halo identity unchanged despite mapRevision");reused++;}else changed++;
            }
            check(reused>0&&changed>0,"edit invalidates only halo");geometry(after,cached);
            for(int r=1;r<95;r++){Hex h=new Hex(32,r);check(after.surface.at(h)<=.08f,"road center retained");}
            for(int q=1;q<95;q++)if(q!=32)check(after.surface.at(new Hex(q,48))==0,"water center retained");
            System.out.println("PATCH staggered="+staggered+" reused="+reused+" changed="+changed);
        }
        System.out.println("PASS R03 "+checks+" assertions");
    }
}
