package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
public final class TerrainSurfaceTest {
    static int checks;
    static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(w);
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
        List<SceneMesh> meshes=SceneMesh.ground(g);Map<String,Float> edges=new HashMap<>();int far=0,near=0;float maximum=0;
        for(SceneMesh m:meshes){
            check(m.distant!=null,"real far LOD");near+=m.indices.length/3;far+=m.distant.indices.length/3;
            for(int v=0;v<m.distant.vertices.length;v+=7){float x=m.vertices[v],y=m.vertices[v+1],z=m.vertices[v+2];maximum=Math.max(maximum,y);
                String key=x+":"+z;Float old=edges.put(key,y);if(old!=null)check(old==y,"shared edges exact across cells and chunks");
                check(Math.abs(g.surface.meshHeight(x,z)-y)<.0001,"ray height agrees with rendered fan");
                check(m.distant.vertices[v]==x&&m.distant.vertices[v+1]==y,"LOD boundary identical");
            }
        }
        check(near==far*3,"far LOD reduces triangles by 2/3");check(maximum>.5,"non-flat national mountains");
        SceneCamera c=new SceneCamera();c.width=1080;c.height=1920;c.span=15;
        int water=0,picks=0;
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){
            Hex h=new Hex(q,r);if(!g.valid(h))continue;
            if(g.surface.water(h)){water++;check(g.surface.at(h)==0,"stable water level");}
            if((q*17+r)%97==0){float x=g.grid.x(h),z=g.grid.z(h);c.x=x;c.z=z;
                check(h.equals(g.surface.pick(c,c.screenX(x),c.screenY(z,g.surface.at(h)))),"surface center pick "+h);picks++;}
        }
        check(water>0&&picks>300,"national coverage");
        for(Hex h:g.bases)if(g.valid(h))check(g.surface.at(h)==0,"flat site footprint");
        List<SceneMesh> same=SceneMesh.ground(new MapSceneSnapshot.Ground(w),meshes);for(int i=0;i<meshes.size();i++)check(same.get(i)==meshes.get(i),"unchanged chunks reused");
        Hex changed=null;for(int r=40;r<g.height&&changed==null;r++)for(int q=40;q<g.width;q++){Hex h=new Hex(q,r);if(g.valid(h)&&!g.bases.contains(h)&&!g.surface.water(h)){changed=h;break;}}
        w.terrain[changed.q][changed.r]=w.terrain[changed.q][changed.r]==World.Terrain.MOUNTAIN?World.Terrain.SAND:World.Terrain.MOUNTAIN;
        check(!g.matches(w),"patch invalidates ground");List<SceneMesh> patched=SceneMesh.ground(new MapSceneSnapshot.Ground(w),meshes);int reused=0;for(SceneMesh m:patched)if(meshes.contains(m))reused++;
        check(reused>meshes.size()/2&&reused<meshes.size(),"only affected chunks and halo rebuilt");
        World original=SaveCodec.decode(before);check(Arrays.equals(before,SaveCodec.encode(original)),"baseline preserved");
        check(SceneMesh.terrain(World.Terrain.ROAD.ordinal())==SceneMesh.terrain(World.Terrain.PLAIN.ordinal()),"roads retain plain visual");
        System.out.println("PASS S02: "+checks+" assertions; triangles near="+near+", far="+far+"; water="+water+"; ray picks="+picks+"; patched chunks reused="+reused+"/"+meshes.size()+"; max height="+maximum);
    }
}
