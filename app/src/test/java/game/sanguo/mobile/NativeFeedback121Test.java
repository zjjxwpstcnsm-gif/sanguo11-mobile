package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;

/** Geometry/authority evidence, not art or phone performance acceptance. */
public final class NativeFeedback121Test {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static SceneMesh load(String path)throws Exception{try(InputStream in=Files.newInputStream(Path.of("app/src/main/assets/3d/"+path))){return SiteGlb.read(in);}}
    public static void main(String[] args)throws Exception{
        boolean baseline=args.length>0;
        for(boolean staggered:new boolean[]{false,true}){
            World w=new World(24,24);w.columnStaggered=staggered;
            for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++)w.terrain[q][r]=World.Terrain.MOUNTAIN;
            for(int r=0;r<w.height;r++)w.terrain[12][r]=World.Terrain.MOUNTAIN_PATH;
            MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);Hex pass=new Hex(12,12);
            float relief=0;for(Hex h:pass.neighbors())if(g.terrain[h.r*g.width+h.q]==World.Terrain.MOUNTAIN.ordinal())relief=Math.max(relief,g.surface.at(h));
            System.out.println("narrow-pass staggered="+staggered+" adjacentMountain="+relief+" path="+g.surface.at(pass));
            check(g.surface.at(pass)<=.080001,"road stays low");
            check(baseline?Math.abs(relief-.16916323f)<.000001f:relief>.65,"narrow-pass mountain relief (old defect reproduction)");
            if(baseline)continue;
            w.terrain[4][4]=World.Terrain.WATER;w.terrain[5][4]=World.Terrain.NON_NAVIGABLE_WATER;
            g=new MapSceneSnapshot.Ground(w);
            SceneMesh.TerrainWindow window=new SceneMesh.TerrainWindow(g.grid.x(pass),g.grid.z(pass),8,8,8);
            List<SceneMesh> chunks=SceneMesh.ground(g,List.of(),window);
            long bytes=0;int triangles=0;
            for(SceneMesh m:chunks){check(m.grid!=null,"worker builds static grid batch");SceneMesh grid=m.grid;triangles+=grid.indices.length/3;bytes+=grid.vertices.length*4L+grid.indices.length*4L;
                for(int i=0;i<grid.indices.length;i+=3){float x=0,z=0,y=0;for(int j=0;j<3;j++){int at=grid.indices[i+j]*7;x+=grid.vertices[at]/3;z+=grid.vertices[at+2]/3;y+=grid.vertices[at+1]/3;}
                    Hex h=g.shoreline.inverse(x,z).cell;check(g.gridCell(h,false),"grid triangle stays in allowed cell, no mountain/non-navigable water interior");
                    check(Math.abs(y-g.surface.meshHeight(x,z))<.025,"grid uses actual displayed triangle planes");
                }
            }
            List<SceneMesh> reused=SceneMesh.ground(g,chunks,window);
            for(int i=0;i<chunks.size();i++)check(chunks.get(i)==reused.get(i)&&chunks.get(i).grid==reused.get(i).grid,"unchanged worker batch reused");
            System.out.println("grid chunks="+chunks.size()+" triangles="+triangles+" CPU_bytes="+bytes+" (not GPU allocation)");
        }
        World national=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(national);MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(national);
        int gates=0,joined=0;double mountainHeight=0;int mountains=0;
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++)if(g.terrain[r*g.width+q]==World.Terrain.MOUNTAIN.ordinal()){mountainHeight+=g.surface.at(new Hex(q,r));mountains++;}
        System.out.println("national mountain count="+mountains+" meanHeight="+mountainHeight/mountains);
        if(!baseline){
            for(String name:new String[]{"field/fire-v121.glb","field/smoke-v121.glb","field/rock-strata-v121-lod0.glb","field/rock-strata-v121-lod1.glb","sites/gate-v121-lod0.glb","sites/gate-v121-lod1.glb","sites/gate-v121-lod2.glb"}){
                SceneMesh m=load(name);check(m.indices.length>0&&m.indices.length/3<2000,"Blender asset triangle budget "+name);for(float f:m.vertices)check(Float.isFinite(f),"finite Blender geometry");
                System.out.println("asset="+name+" triangles="+m.indices.length/3);
            }
            SceneMesh model=load("sites/gate-v121-lod0.glb");
            for(World.City city:national.cities)if(city.kind==World.SiteKind.GATE){
                SiteVisual site=new SiteVisual(national,city,g.grid);SceneMesh gate=SiteVisual.joinGate(model,g,city.hex,site.yaw);gates++;
                int extra=gate.indices.length-model.indices.length;if(extra>0)joined++;
                for(int i=model.vertices.length;i<gate.vertices.length;i+=7){float x=gate.vertices[i],z=gate.vertices[i+2],cs=(float)Math.cos(site.yaw),sn=(float)Math.sin(site.yaw);
                    Hex h=g.grid.cell(g.grid.x(city.hex)+cs*x+sn*z,g.grid.z(city.hex)-sn*x+cs*z);
                    check(h.equals(city.hex)||g.valid(h)&&g.terrain[h.r*g.width+h.q]==World.Terrain.MOUNTAIN.ordinal(),"gate wing never closes a legal adjacent cell "+city.name);
                }
                System.out.println("gate="+city.name+" hex="+city.hex+" addedWingTriangles="+extra/3);
            }
            check(joined>0,"real national gates have mountain connections");
            check(CombatVisual.mesh(2).indices.length/3<load("field/fire-v121.glb").indices.length/3,"new authored flame replaces fallback diamond at runtime");
        }
        check(Arrays.equals(before,SaveCodec.encode(national)),"entire official save/RNG preserved");
        System.out.println("PASS FEEDBACK121 checks="+checks+" baseline="+baseline+" gates="+gates+" connected="+joined);
    }
}
