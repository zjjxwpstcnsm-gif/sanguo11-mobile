package game.sanguo.mobile;
import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.util.*;

/** Independent triangle projection oracle, full national centres and view-pixel invariance. */
public final class NativeR02Test {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void near(float a,float b,String why){check(Math.abs(a-b)<.001f,why+" "+a+" != "+b);}
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.all().get(0);byte[] authority=SaveCodec.encode(w);
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);SceneCamera c=new SceneCamera();c.width=1080;c.height=1920;
        int valid=0;long golden=1469598103934665603L;
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){
            Hex h=new Hex(q,r);if(!g.valid(h))continue;valid++;
            SourceGridCoord source=MapCoordinates.nationalSource(w,h);
            check(h.equals(MapCoordinates.fromNationalSource(w,source)),"source transpose roundtrip");
            float x=g.grid.x(h),z=g.grid.z(h);check(h.equals(g.grid.cell(x,z)),"world centre");
            golden=(golden^q)*1099511628211L;golden=(golden^r)*1099511628211L;
            golden=(golden^Float.floatToIntBits(x))*1099511628211L;golden=(golden^Float.floatToIntBits(z))*1099511628211L;
            for(float yaw:new float[]{0,90,180,270}){
                c.yaw=yaw;float y=g.surface.at(h),sx=c.screenX(x,z),sy=c.screenY(x,z,y);
                near(c.worldX(sx,sy,y),x,"all-centre projected x");near(c.worldZ(sx,sy,y),z,"all-centre projected z");
            }
        }
        check(valid==38949&&Long.toUnsignedString(golden).equals("15475946728566902866"),"pinned national centre cohort");
        System.out.println("NATIONAL_GOLDEN valid="+valid+" hash="+Long.toUnsignedString(golden));
        // GridLayout gold samples independent of production formulas, including negative cells/offset.
        GridWorldTransform grid=new GridWorldTransform(99,true);
        check(grid.x(100,101)==101&&grid.z(100,101)==51.5f,"transposed odd source sample");
        check(grid.cell(-3,-4).equals(new Hex(97,-3)),"negative half tie goes positive");
        check(grid.cell(0,-99).equals(new Hex(0,0)),"boundary origin not added twice");
        List<SceneMesh> chunks=SceneMesh.ground(g);int samples=0,occluded=0;
        for(float span:new float[]{3,15,80})for(float yaw:new float[]{0,90,180,270})for(float tilt:new float[]{40,55,70}){
            c.span=span;c.yaw=yaw;c.tilt=tilt;c.facing=1;
            for(SceneMesh fine:chunks){
                if((fine.chunkQ+fine.chunkR)%64!=0)continue;SceneMesh mesh=fine.distant;
                int v=(mesh.vertices.length/7/2)*7;float x=mesh.vertices[v],y=mesh.vertices[v+1],z=mesh.vertices[v+2];
                c.x=x;c.z=z;float sx=c.screenX(x,z),sy=c.screenY(x,z,y);
                float hit=g.surface.rayHeight(c,sx,sy),expected=Float.NEGATIVE_INFINITY;
                for(SceneMesh candidate:chunks)if(Math.abs(candidate.x-x)<candidate.radius+5&&Math.abs(candidate.z-z)<candidate.radius+5)
                    expected=Math.max(expected,ScenePicking.hit(c,candidate.distant,0,0,0,0,1,sx,sy));
                near(hit,expected,"nearest terrain triangle oracle");if(hit>y+.001f)occluded++;
                Hex picked=g.surface.pick(c,sx,sy);check(picked!=null,"valid terrain hit");
                for(float scale:new float[]{.7f,.85f,1}){
                    // PixelCopy/render buffer dimensions never enter production camera input.
                    int bw=Math.round(c.width*scale),bh=Math.round(c.height*scale);
                    check(bw>0&&bh>0&&picked.equals(g.surface.pick(c,sx,sy)),"view pixel invariant "+scale);
                }
                float ox=c.worldX(sx,sy,hit),oz=c.worldZ(sx,sy,hit);c.zoom(1.15f,sx,sy,hit);
                check(Math.abs(c.screenX(ox,oz)-sx)<.01f&&Math.abs(c.screenY(ox,oz,hit)-sy)<.01f,"height zoom anchor within .01 view pixel (float world positions)");samples++;
            }
        }
        c.x=1000;c.z=1000;check(g.surface.pick(c,540,960)==null,"outside/VOID not actionable");
        c.tilt=0;c.yaw=Float.NaN;c.sanitize();check(c.tilt==40&&c.yaw==0,"bounded invalid camera");
        for(World.City site:w.cities)for(Hex h:SiteFootprint.cells(site)){
            c.x=g.grid.x(h);c.z=g.grid.z(h);c.tilt=55;c.yaw=37;c.span=3;
            check(h.equals(g.surface.pick(c,540,960)),"city/port/gate exact authoritative footprint");
        }
        check(Arrays.equals(authority,SaveCodec.encode(w)),"camera/pick never alters authority/RNG");
        System.out.println("PASS R02 "+checks+" checks; matrix samples="+samples+" front occlusions="+occluded);
    }
}
