package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
/** Retained boxed emission is the independent byte-level oracle for the allocation change. */
public final class GridPrimitiveEquivalenceTest {
    private static int checks,meshes;private static long referenceBytes,primitiveBytes;
    private static final com.sun.management.ThreadMXBean memory=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static long allocated(){return memory.isThreadAllocatedMemorySupported()?memory.getThreadAllocatedBytes(Thread.currentThread().getId()):0;}
    private static void pair(MapSceneSnapshot.Ground g,int q,int r,int lod){
        SceneMesh bounds=new SceneMesh(new float[0],new int[0],g.grid.x(q+8,r+8),g.grid.z(q+8,r+8),13);
        long start=allocated();SceneMesh expected=referenceGrid(g,q,r,bounds,lod);referenceBytes+=allocated()-start;
        start=allocated();SceneMesh actual=SceneMesh.grid(g,q,r,bounds,lod);primitiveBytes+=allocated()-start;
        equal(expected,actual);meshes++;
    }
    private static void equal(SceneMesh expected,SceneMesh actual){
        check(expected.vertices.length==actual.vertices.length,"same vertex count");
        for(int i=0;i<expected.vertices.length;i++){checks++;if(Float.floatToRawIntBits(expected.vertices[i])!=Float.floatToRawIntBits(actual.vertices[i]))throw new AssertionError("exact position/color bits at "+i);}
        check(Arrays.equals(expected.indices,actual.indices),"same ordered winding/index bytes");
        check(expected.landIndexCount==actual.landIndexCount&&expected.x==actual.x&&expected.z==actual.z&&expected.radius==actual.radius&&expected.minY==actual.minY&&expected.maxY==actual.maxY,"same renderer metadata and bounds");
    }
    private static void inspect(World w)throws Exception{
        byte[] before=w.cities.isEmpty()?null:SaveCodec.encode(w);int terrain=Arrays.deepHashCode(w.terrain);
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
        int[][] chunks=g.pcMap==null?new int[][]{{0,0},{16,16},{32,32},{-16,-16}}:new int[][]{{128,80},{144,96},{160,80},{-16,-16},{g.width/16*16,g.height/16*16}};
        for(int[] p:chunks)for(int lod=0;lod<3;lod++)pair(g,p[0],p[1],lod);
        if(g.pcMap!=null){
            List<SceneMesh> batch=SceneMesh.ground(g,Collections.emptyList(),new SceneMesh.TerrainWindow(g.grid.x(144,96),g.grid.z(144,96),2,2,5));
            check(batch.size()>1,"multiple published chunks reuse task scratch");
            for(SceneMesh m:batch)if(m.grid!=null)equal(referenceGrid(g,m.chunkQ,m.chunkR,m,m.terrainLod),m.grid);
        }
        check(terrain==Arrays.deepHashCode(w.terrain),"authority terrain exact");if(before!=null)check(Arrays.equals(before,SaveCodec.encode(w)),"complete save/RNG exact");
    }
    public static void main(String[] args)throws Exception{
        if(memory.isThreadAllocatedMemorySupported())memory.setThreadAllocatedMemoryEnabled(true);
        for(boolean staggered:new boolean[]{false,true}){
            World w=new World(36,36);w.columnStaggered=staggered;
            for(int q=0;q<36;q++)for(int r=0;r<36;r++)w.terrain[q][r]=q<12||r==16?World.Terrain.WATER:World.Terrain.PLAIN;
            w.terrain[17][16]=World.Terrain.PLAIN;w.terrain[24][24]=World.Terrain.MOUNTAIN;w.terrain[25][24]=World.Terrain.NON_NAVIGABLE_WATER;w.terrain[26][24]=World.Terrain.MOUNTAIN_PATH;
            inspect(w);
        }
        inspect(ScenarioCatalog.load("heroes-250",0));
        System.out.println("PASS primitive-grid "+checks+" checks; pairs="+meshes+" boxed_allocated_bytes="+referenceBytes+" primitive_allocated_bytes="+primitiveBytes+"; JVM allocations, not ART/GPU performance");
    }
    private static SceneMesh referenceGrid(MapSceneSnapshot.Ground g,int q,int r,SceneMesh bounds,int lod){
        SceneMesh.Builder b=new SceneMesh.Builder();
        float width=lod==0?.012f:lod==1?.023f:.038f;
        for(int rr=Math.max(0,r);rr<Math.min(r+16,g.height);rr++)for(int qq=Math.max(0,q);qq<Math.min(q+16,g.width);qq++){
            Hex h=new Hex(qq,rr);if(!g.gridCell(h,false))continue;
            float x=g.grid.x(h),z=g.grid.z(h),cy=g.surface.at(h);
            for(int e=0;e<8;e++){
                float[] a=SceneMesh.EDGE[e],d=SceneMesh.EDGE[(e+1)%8];
                float ax=x+a[0],az=z+a[1],dx=x+d[0],dz=z+d[1];
                float ay=g.surface.sample(ax,az),dy=g.surface.sample(dx,dz);
                // Each half-stroke stays inside its eligible cell. Shared edges
                // join without drawing over a mountain/restricted cell interior.
                int segments=g.pcMap==null?1:2;
                for(int part=0;part<segments;part++){
                    float t0=part/(float)segments,t1=(part+1)/(float)segments;
                    float ex=ax+(dx-ax)*t0,ez=az+(dz-az)*t0,fx=ax+(dx-ax)*t1,fz=az+(dz-az)*t1;
                    float ey=g.surface.sample(ex,ez),fy=g.surface.sample(fx,fz);
                    gridRibbon(b,g,x,z,cy,ex,ez,ey,fx,fz,fy,width,.018f,0xff243b38);
                    gridRibbon(b,g,x,z,cy,ex,ez,ey,fx,fz,fy,width*.42f,.022f,0xffd9dfbe);
                }
            }
        }
        return b.mesh(bounds.x,bounds.z,bounds.radius);
    }
    private static void gridRibbon(SceneMesh.Builder b,MapSceneSnapshot.Ground g,float x,float z,float y,
            float ax,float az,float ay,float dx,float dz,float dy,float inset,float lift,int color){
        // Exact barycentric inset within the same visible fan planes, including
        // rounded shoreline transport. Width is an offline LOD choice, not a rule.
        float[] a=g.shoreline.project(ax,az),d=g.shoreline.project(dx,dz);
        float t=inset*2;
        b.face(new float[]{a[0],ay+lift,a[1],d[0],dy+lift,d[1],
            d[0]+(x-d[0])*t,dy+(y-dy)*t+lift,d[1]+(z-d[1])*t,
            a[0]+(x-a[0])*t,ay+(y-ay)*t+lift,a[1]+(z-a[1])*t},color);
    }
}
