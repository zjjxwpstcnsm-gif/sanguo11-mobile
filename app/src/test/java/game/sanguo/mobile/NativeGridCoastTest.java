package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Display-only repair: explicit topology, centre, seam, winding and grid oracles. */
public final class NativeGridCoastTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void inspect(World w)throws Exception{
        // Tiny geometry fixtures have no factions/officers and deliberately cannot
        // be serialized. The official scenario below checks complete save/RNG bytes.
        byte[] before=w.cities.isEmpty()?null:SaveCodec.encode(w);int terrainBefore=Arrays.deepHashCode(w.terrain);
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
        int moved=0,land=0,water=0;double area=0;float maxShift=0;
        Map<String,float[]> shared=new HashMap<>();
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){
            Hex h=new Hex(q,r);if(!g.valid(h))continue;
            float x=g.grid.x(h),z=g.grid.z(h);float[] p=g.shoreline.project(x,z);
            check(p[0]==x&&p[1]==z,"every gameplay centre stays fixed");
            check(h.equals(g.shoreline.inverse(x,z).cell),"land/water/island/port centre picks original cell");
            boolean expected=w.terrain[q][r]!=World.Terrain.MOUNTAIN&&w.terrain[q][r]!=World.Terrain.NON_NAVIGABLE_WATER&&!NationalMap.restricted(w,h)&&(w.campaign.has(w.player,Campaign.Tech.DIFFICULT_MARCH)||!Fieldworks.requiresDifficultMarch(w.terrain[q][r]));
            check(g.gridCell(h,false)==expected,"ordinary grid excludes permanent and current-force technology restrictions");
            check(g.gridCell(h,true),"editor can inspect/paint all valid terrain");
            if(g.surface.water(h))water++;else land++;
        }
        List<SceneMesh> meshes=SceneMesh.ground(g);
        for(SceneMesh fine:meshes){SceneMesh mesh=fine.distant;
            check(fine.indices.length==mesh.indices.length*3,"existing LOD budget retained");
            for(int i=0;i<mesh.vertices.length/7;i++){
                float x=mesh.vertices[i*7],z=mesh.vertices[i*7+2],cx=mesh.surfaceData[i*8],cz=-mesh.surfaceData[i*8+1];
                float shift=Math.max(Math.abs(x-cx),Math.abs(z-cz));maxShift=Math.max(maxShift,shift);if(shift>0)moved++;
                check(shift<=.1875f,"bounded shoreline displacement cannot cross a cell centre");
                String key=cx+":"+cz;float[] old=shared.put(key,new float[]{x,mesh.vertices[i*7+1],z});
                if(old!=null)check(Arrays.equals(old,new float[]{x,mesh.vertices[i*7+1],z}),"shared chunk/cell boundary transported identically");
                check(mesh.vertices[i*7]==fine.vertices[i*7]&&mesh.vertices[i*7+2]==fine.vertices[i*7+2],"all LOD edges identical");
                check(Math.abs(g.surface.meshHeight(x,z)-mesh.vertices[i*7+1])<.0002,"actual transported mesh height used by anchors");
            }
            for(int t=0;t<mesh.indices.length;t+=3){
                int a=mesh.indices[t]*7,b=mesh.indices[t+1]*7,c=mesh.indices[t+2]*7;
                float signed=(mesh.vertices[b+2]-mesh.vertices[a+2])*(mesh.vertices[c]-mesh.vertices[a])
                    -(mesh.vertices[b]-mesh.vertices[a])*(mesh.vertices[c+2]-mesh.vertices[a+2]);
                check(signed>0,"no inverted or collapsed land/water fan");area+=signed*.5;
                float x=(mesh.vertices[a]+mesh.vertices[b]+mesh.vertices[c])/3,z=(mesh.vertices[a+2]+mesh.vertices[b+2]+mesh.vertices[c+2])/3;
                check(g.surface.water(g.shoreline.inverse(x,z).cell)==(t>=mesh.landIndexCount),"rendered triangle picks its own water/land cell");
            }
        }
        check(moved>0,"production mesh contains actual rounded coasts");
        check(Math.abs(area-land-water)<.002,"pinned exterior and shared edges retain total displayed coverage");
        check(terrainBefore==Arrays.deepHashCode(w.terrain),"geometry never mutates authoritative terrain");
        if(before!=null)check(Arrays.equals(before,SaveCodec.encode(w)),"entire save and rule RNG unchanged");
        System.out.println("coast map="+w.mapId+" cells="+(land+water)+" movedVertices="+moved+" maxAxisShift="+maxShift+" area="+area);
    }
    public static void main(String[] args)throws Exception{
        for(boolean staggered:new boolean[]{false,true}){
            World w=new World(36,36);w.columnStaggered=staggered;
            for(int r=0;r<36;r++)for(int q=0;q<36;q++)w.terrain[q][r]=(q<12||q==17||r==16)?World.Terrain.WATER:World.Terrain.PLAIN;
            w.terrain[17][16]=World.Terrain.PLAIN;w.terrain[24][24]=World.Terrain.SEA;
            w.terrain[25][24]=World.Terrain.NON_NAVIGABLE_WATER;w.terrain[26][24]=World.Terrain.MOUNTAIN;
            w.terrain[27][24]=World.Terrain.MOUNTAIN_PATH;w.terrain[28][24]=World.Terrain.PLANK_ROAD;
            inspect(w);
        }
        inspect(ScenarioCatalog.all().get(0));
        // Isolated right-angle corner: both relaxation passes reduce the 90-degree kink.
        World corner=new World(12,12);for(int q=0;q<12;q++)for(int r=0;r<12;r++)corner.terrain[q][r]=World.Terrain.PLAIN;
        corner.terrain[5][5]=World.Terrain.WATER;MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(corner);
        float cx=g.grid.x(5,5),cz=g.grid.z(5,5),x=cx-.5f,z=cz-.5f;
        float[] a=g.shoreline.project(x,z+.5f),b=g.shoreline.project(x,z),c=g.shoreline.project(x+.5f,z);
        double ax=b[0]-a[0],az=b[1]-a[1],bx=c[0]-b[0],bz=c[1]-b[1];
        double turn=Math.toDegrees(Math.acos((ax*bx+az*bz)/Math.hypot(ax,az)/Math.hypot(bx,bz)));
        check(turn<65,"sharp rectangle corner is materially rounded");
        System.out.println("corner turn: before=90 after="+turn+" degrees");
        System.out.println("PASS GRID_COAST "+checks+" checks; host geometry is not device/art acceptance");
    }
}
