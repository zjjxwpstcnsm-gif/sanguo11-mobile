package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;

/** S13 checks real uploaded payloads for every national chunk, not just sample cells. */
public final class NationwideAcceptanceTest {
    static long checks,vertices,triangles;static int chunks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void audit(World world)throws Exception{
        byte[] save=SaveCodec.encode(world);MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(world);
        List<SceneMesh> meshes=SceneMesh.ground(g);Map<String,float[]> shared=new HashMap<>();
        for(SceneMesh fine:meshes){chunks++;
            for(SceneMesh m:new SceneMesh[]{fine,fine.distant}){
                int count=m.vertices.length/7;vertices+=count;triangles+=m.indices.length/3;
                check(m.surfaceData.length==count*8,"attribute counts");
                for(int i:m.indices)check(i>=0&&i<count,"index in bounds");
                for(float v:m.vertices)check(Float.isFinite(v),"finite vertex");
                for(float v:m.surfaceData)check(Float.isFinite(v),"finite material payload");
                for(int i=0;i<count;i++){
                    float x=m.vertices[i*7],y=m.vertices[i*7+1],z=m.vertices[i*7+2];
                    check(Math.abs(x-m.x)<=m.radius&&Math.abs(z-m.z)<=m.radius,"chunk XZ bounds");
                    float weight=0;for(int k=0;k<4;k++){float v=m.vertices[i*7+3+k];check(v>=0&&v<=1,"normalized layer");weight+=v;}
                    check(Math.abs(weight-1)<.00002,"weight sum");
                    if(m==fine.distant){
                        String key=x+":"+z;float[] payload=new float[12];payload[0]=y;
                        System.arraycopy(m.surfaceData,i*8,payload,1,7);System.arraycopy(m.vertices,i*7+3,payload,8,4);
                        float[] prior=shared.putIfAbsent(key,payload);if(prior!=null)for(int k=0;k<payload.length;k++)check(Math.abs(prior[k]-payload[k])<.00002,"shared edge position/material/normal "+key+" channel "+k);
                    }
                }
                if(m==fine)for(int i=0;i<fine.distant.vertices.length;i++)check(m.vertices[i]==fine.distant.vertices[i],"mixed LOD boundary geometry");
            }
        }
        check(Arrays.equals(save,SaveCodec.encode(world)),"complete save neutral");
        List<SceneMesh> cached=SceneMesh.ground(g,meshes);for(int i=0;i<meshes.size();i++)check(cached.get(i)==meshes.get(i),"unchanged cache identity");
        System.out.println("audited "+world.mapId+" "+world.width+"x"+world.height+" chunks="+meshes.size());
    }
    static void seedRegression(){
        World w=new World(24,20);for(World.Terrain[] row:w.terrain)Arrays.fill(row,World.Terrain.PLAIN);
        List<SceneMesh> old=SceneMesh.ground(new MapSceneSnapshot.Ground(w));w.mapRevision++;
        MapSceneSnapshot.Ground next=new MapSceneSnapshot.Ground(w);List<SceneMesh> cached=SceneMesh.ground(next,old),fresh=SceneMesh.ground(next);
        for(int i=0;i<fresh.size();i++){check(cached.get(i)!=old.get(i),"changed visual seed invalidates old mesh");check(Arrays.equals(cached.get(i).surfaceData,fresh.get(i).surfaceData)&&Arrays.equals(cached.get(i).vertices,fresh.get(i).vertices),"seed cache equals fresh output");}
    }
    public static void main(String[] args)throws Exception{
        seedRegression();audit(ScenarioCatalog.all().get(0));
        for(int layout=0;layout<3;layout++)for(int origin:new int[]{0,5}){
            World w=new World(layout==0?35:layout==1?48:44,layout==2?35:27);w.columnStaggered=layout==2;w.sourceMapWidth=layout==0?0:35;w.sourceMapHeight=layout==0?0:27;w.sourceOriginX=origin;w.sourceOriginY=origin;
            for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++)w.terrain[q][r]=!w.sourceInside(new Hex(q,r))||(q+r)%11==0?World.Terrain.VOID:q==16?World.Terrain.WATER:q<10?World.Terrain.FOREST:q<22?World.Terrain.MOUNTAIN:World.Terrain.SAND;
            World.City site=new World.City(0,"Test",new Hex(20,13),0);w.cities.add(site);for(Hex h:SiteFootprint.cells(site))w.terrain[h.q][h.r]=World.Terrain.PLAIN;
            audit(w);
        }
        System.out.println("PASS S13 nationwide: "+checks+" checks, "+chunks+" chunks, "+vertices+" vertices, "+triangles+" triangles");
    }
}
