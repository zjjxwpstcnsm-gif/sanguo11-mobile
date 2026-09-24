package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;

/** Independent material identities and hard constraints, in both supported grid layouts. */
public final class NativeR04Test {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void weights(float[] a){float sum=0;for(float v:a){check(Float.isFinite(v)&&v>=0&&v<=1,"bounded weight");sum+=v;}check(Math.abs(sum-1)<1e-5,"normalized");}
    public static void main(String[] args)throws Exception{
        for(float[] a:new float[][]{{0,0,0,0},{-1,Float.NaN,Float.POSITIVE_INFINITY,0},{2,3,1,4}}){TerrainMaterialField.normalize(a);weights(a);}
        for(boolean staggered:new boolean[]{false,true}){
            World w=new World(64,64);w.columnStaggered=staggered;
            for(int q=0;q<64;q++)for(int r=0;r<64;r++)w.terrain[q][r]=q<16?World.Terrain.PLAIN:q<32?World.Terrain.SAND:q<48?World.Terrain.MOUNTAIN:World.Terrain.ROAD;
            for(int q=0;q<64;q++)w.terrain[q][8]=World.Terrain.WATER;
            for(int r=16;r<48;r++)w.terrain[40][r]=World.Terrain.MOUNTAIN_PATH;
            String before=Arrays.deepToString(w.terrain);MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);TerrainMaterialField f=new TerrainMaterialField(g);
            for(int q=1;q<63;q++)for(int r=1;r<63;r++){
                float x=g.grid.x(q,r),z=g.grid.z(q,r);float[] a=f.sample(x,z,Float.NaN);weights(a);
                check(Arrays.equals(a,f.sample(x,z,0)),"invalid slope is safe flat slope");
                float[] b=f.sample(x+.00001f,z+.00001f);for(int k=0;k<4;k++)check(Math.abs(a[k]-b[k])<.001,"continuous field");
                if(r==8)check(g.surface.at(new Hex(q,r))==0,"water footprint remains exact");
            }
            float[] grass=f.sample(g.grid.x(8,32),g.grid.z(8,32));
            float[] sand=f.sample(g.grid.x(24,32),g.grid.z(24,32),1);
            float[] rock=f.sample(g.grid.x(35,32),g.grid.z(35,32));
            float[] path=f.sample(g.grid.x(40,32),g.grid.z(40,32));
            float roadX=g.grid.x(56,32),roadZ=g.grid.z(56,32);
            float[] roadCentre=f.sample(roadX,roadZ),roadBetween=f.sample(roadX+.3f,roadZ+.2f);
            for(int k=0;k<4;k++)check(Math.abs(roadCentre[k]-roadBetween[k])<.0001,"dense ordinary road has no repeated centre stamp");
            check(Math.abs(roadCentre[1]-.18f)<.0001,"ordinary road retains prior continuous biome identity");
            check(grass[0]>.85,"grass region identity");check(sand[2]>.98,"sand not converted into generic mud by slope");
            check(rock[3]>.85,"rock identity");check(path[1]>.70,"hard path centre remains soil");
            check(before.equals(Arrays.deepToString(w.terrain)),"synthetic map unchanged");
        }
        World official=ScenarioCatalog.all().get(0);byte[] save=SaveCodec.encode(official);
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(official);TerrainMaterialField f=new TerrainMaterialField(g);
        for(World.City city:official.cities){float[] a=f.sample(g.grid.x(city.hex),g.grid.z(city.hex));weights(a);}
        check(Arrays.equals(save,SaveCodec.encode(official)),"official complete rules/map/RNG/save unchanged");
        System.out.println("PASS R04 "+checks+" material identity/normalization/constraints checks");
    }
}
