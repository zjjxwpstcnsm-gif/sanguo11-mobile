package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;

/** Independent distance oracle, real interior refinement and no-authority-write checks. */
public final class NativeR05Test {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static float oracle(MapSceneSnapshot.Ground g,float x,float z){
        boolean wet=g.surface.water(g.grid.cell(x,z));double best=1.5;
        for(int r=-4;r<g.height+4;r++)for(int q=-4;q<g.width+4;q++){
            if(g.surface.water(q,r)==wet)continue;
            double dx=Math.max(0,Math.abs(x-g.grid.x(q,r))-.5),dz=Math.max(0,Math.abs(z-g.grid.z(q,r))-.5);
            best=Math.min(best,Math.hypot(dx,dz));
        }return (float)(wet?best:-best);
    }
    public static void main(String[] args)throws Exception{
        for(boolean staggered:new boolean[]{false,true}){
            World w=new World(36,36);w.columnStaggered=staggered;
            for(int r=0;r<36;r++)for(int q=0;q<36;q++)
                w.terrain[q][r]=(q==16||r==17||q<7&&r<7||q==30&&r==30)?World.Terrain.WATER:World.Terrain.PLAIN;
            w.terrain[16][17]=World.Terrain.PLAIN; // island in confluence
            MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);WaterVisualField f=new WaterVisualField(g);
            for(int q=0;q<36;q++)for(int r=0;r<36;r++){
                float x=g.grid.x(q,r),z=g.grid.z(q,r);
                for(float dx:new float[]{-.49f,0,.49f}){
                    float got=f.distance(x+dx,z+.23f);
                    check(Math.abs(got-oracle(g,x+dx,z+.23f))<1e-5,"distance agrees with exhaustive opposite-mask oracle");
                }
                float d=f.distance(x,z);check(g.surface.water(q,r)?d>=.49f:d<=-.49f,"centre protected including narrow river/island");
            }
            List<SceneMesh> meshes=SceneMesh.ground(g);int refined=0;
            for(SceneMesh fine:meshes){SceneMesh coarse=fine.distant;int start=coarse.vertices.length/7;
                for(int t=0;t<coarse.indices.length;t+=3){int i=start+t/3;float x=fine.vertices[i*7],z=fine.vertices[i*7+2];
                    check(fine.surfaceData[i*8+6]==f.distance(x,z),"new interior vertex samples actual signed distance");
                    float old=0;for(int j=0;j<3;j++)old+=coarse.surfaceData[coarse.indices[t+j]*8+6]/3;
                    if(Math.abs(old-fine.surfaceData[i*8+6])>.001)refined++;
                    if(t>=coarse.landIndexCount){check(fine.vertices[i*7+1]==0,"water remains shared pick/anchor height");check(fine.surfaceData[i*8+7]==f.flowAngle(x,z),"flow resampled at same world point");}
                    int a=fine.indices[t*3]*7,b=fine.indices[t*3+1]*7,c=fine.indices[t*3+2]*7;
                    float cross=(fine.vertices[b+2]-fine.vertices[a+2])*(fine.vertices[c]-fine.vertices[a])-(fine.vertices[b]-fine.vertices[a])*(fine.vertices[c+2]-fine.vertices[a+2]);
                    check(cross>0,"positive winding and nonzero triangles");
                }
            }check(refined>0,"refinement adds actual shore information, not redundant interpolation");
        }
        World official=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(official);
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(official);WaterVisualField f=new WaterVisualField(g);
        for(World.City c:official.cities)if(c.kind==World.SiteKind.PORT){
            int n=0;for(Hex h:c.hex.neighbors())if(official.army.water(h)){n++;check(f.distance(g.grid.x(h),g.grid.z(h))>=.49f,"navigable port contact stays wet");check(g.surface.at(h)==g.surface.at(c.hex),"port and water anchor agree");}
            check(n>0,"port retains authorized neighbor");
        }
        check(Arrays.equals(before,SaveCodec.encode(official)),"official save incl RNG unchanged");
        System.out.println("PASS R05 "+checks+" oracle/refinement/winding/port checks");
    }
}
