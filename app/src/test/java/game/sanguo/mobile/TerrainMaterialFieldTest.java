package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
public final class TerrainMaterialFieldTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        World world=ScenarioCatalog.all().get(0);byte[] saved=SaveCodec.encode(world);
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(world);TerrainMaterialField field=new TerrainMaterialField(g);
        for(float x=-3.37f;x<g.width;x+=2.31f)for(float z=-2.81f;z<g.height;z+=3.73f){
            float[] a=field.sample(x,z),b=field.sample(x,z),near=field.sample(x+.00001f,z);float sum=0;
            check(Arrays.equals(a,b),"deterministic");
            for(int k=0;k<4;k++){check(Float.isFinite(a[k])&&a[k]>=0&&a[k]<=1,"bounded weights");sum+=a[k];check(Math.abs(a[k]-near[k])<.001f,"continuous coordinate field");}
            check(Math.abs(sum-1)<1e-5f,"normalized weights");
        }
        check(Arrays.equals(field.sample(Float.NaN,0),new float[]{0,1,0,0}),"invalid coordinate fallback");
        List<SceneMesh> meshes=SceneMesh.ground(g);Map<String,float[]> shared=new HashMap<>();
        for(SceneMesh fine:meshes){SceneMesh coarse=fine.distant;check(coarse.surfaceData!=null&&fine.surfaceData!=null,"both LODs lit");
            for(int i=0;i<coarse.vertices.length/7;i++){
                int v=i*7,s=i*8;float[] value=new float[10];System.arraycopy(coarse.vertices,v+3,value,0,4);System.arraycopy(coarse.surfaceData,s,value,4,6);
                String key=coarse.vertices[v]+":"+coarse.vertices[v+2];float[] old=shared.put(key,value);
                if(old!=null)check(Arrays.equals(old,value),"shared weights UV tangent frame across chunk and cell");
                float length=0;for(int j=2;j<6;j++)length+=coarse.surfaceData[s+j]*coarse.surfaceData[s+j];check(Math.abs(length-1)<1e-5,"unit tangent quaternion");
                for(int j=0;j<8;j++)check(coarse.surfaceData[s+j]==fine.surfaceData[s+j],"LOD boundary stream identical");
            }
            for(int t=0;t<coarse.indices.length;t+=3){int n=coarse.vertices.length/7+t/3;
                for(int k=3;k<7;k++){float expected=0;for(int j=0;j<3;j++)expected+=coarse.vertices[coarse.indices[t+j]*7+k];check(Math.abs(fine.vertices[n*7+k]-expected/3)<1e-6,"interior subdivision preserves field not owning-cell color");}
            }
        }
        List<SceneMesh> cached=SceneMesh.ground(new MapSceneSnapshot.Ground(world),meshes);
        for(int i=0;i<meshes.size();i++)check(cached.get(i)==meshes.get(i),"camera/selection-free cache reuse");
        Hex changed=null;for(int r=32;r<g.height&&changed==null;r++)for(int q=32;q<g.width;q++){Hex h=new Hex(q,r);if(g.valid(h)&&!g.bases.contains(h)&&!g.surface.water(h)){changed=h;break;}}
        check(Arrays.equals(saved,SaveCodec.encode(world)),"rendering preserves actual complete save");
        World.Terrain oldTerrain=world.terrain[changed.q][changed.r];world.terrain[changed.q][changed.r]=oldTerrain==World.Terrain.SAND?World.Terrain.PLAIN:World.Terrain.SAND;MapSceneSnapshot.Ground edited=new MapSceneSnapshot.Ground(world);
        List<SceneMesh> patch=SceneMesh.ground(edited,meshes),fresh=SceneMesh.ground(edited);int retained=0;
        for(int i=0;i<patch.size();i++){check(Arrays.equals(patch.get(i).vertices,fresh.get(i).vertices),"halo edit matches full rebuild weights");check(Arrays.equals(patch.get(i).surfaceData,fresh.get(i).surfaceData),"halo edit matches full rebuild normals");if(patch.get(i)==meshes.get(i))retained++;}
        check(retained>meshes.size()/2&&retained<meshes.size(),"local edit retains distant chunks and rebuilds affected chunks");
        world.terrain[changed.q][changed.r]=oldTerrain;
        check(Arrays.equals(saved,SaveCodec.encode(world)),"undo restores full save");
        check(Arrays.equals(saved,SaveCodec.encode(SaveCodec.decode(saved))),"save bytes stable");
        System.out.println("PASS S10: "+checks+" material/LOD/seam/cache assertions; reused="+retained+"/"+meshes.size());
    }
}
