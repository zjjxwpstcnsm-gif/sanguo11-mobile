package game.sanguo.mobile;

import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;

public final class PcConstructibleWallsTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static MapSceneSnapshot scene(World w){return new MapSceneSnapshot(new MapSceneSnapshot.Ground(w),w,null,-1);}
    public static void main(String[] args)throws Exception {
        PcFacilities library=new PcFacilities(Files.newInputStream(Path.of("app/src/main/assets/3d/pc-facilities/facilities.pcz")));
        for(String kind:new String[]{"earth","stone"})for(String mode:new String[]{"build","complete","damage","destroy"})for(String parity:new String[]{"odd","even"}){
            PcWallsFixture.Case prepared=PcWallsFixture.prepare(kind+"-"+mode+"-"+parity);World w=prepared.world();byte[] before=SaveCodec.encode(w);MapSceneSnapshot initial=scene(w);int initialEdges=edges(initial);
            check(initialEdges==(mode.equals("damage")||mode.equals("destroy")?2:0),"source graph precondition "+prepared.mode());
            verify(library,initial);
            check(Arrays.equals(before,SaveCodec.encode(w)),"detached topology and meshes preserve entire authority/RNG/save");
            World.Result result=prepared.command(w);check(result.ok,"real wall command "+prepared.mode()+": "+result.message);byte[] afterCommand=SaveCodec.encode(w);MapSceneSnapshot after=scene(w);verify(library,after);
            check(Arrays.equals(afterCommand,SaveCodec.encode(w)),"post-command wall connection rebuild preserves authority/RNG/save");
            check(edges(after)==(mode.equals("complete")?2:0),"normal command updates both endpoints and removes all obsolete edges "+prepared.mode());
            check(edges(initial)==initialEdges,"older immutable topology stays intact after normal command");
            System.out.println("NORMAL_WALL "+prepared.mode()+" edges="+initialEdges+"→"+edges(after)+" "+result.message);
        }
        System.out.println("PASS PC constructible walls "+checks+" checks; 16 legal command cases");
    }
    static int edges(MapSceneSnapshot s){int n=0;for(int m:s.wallConnections.values())n+=Integer.bitCount(m&56);return n;}
    static void verify(PcFacilities library,MapSceneSnapshot s){
        for(MapSceneSnapshot.Item i:s.items){PcConstructibleWalls.Placement p=PcConstructibleWalls.placement(s.ground,i);if(p==null)continue;int mask=s.wallConnections.getOrDefault(i.hex,0);
            var cell=s.ground.source(i.hex);check(p.ix==cell.x*4+114&&p.iz==cell.y*4+(cell.x&1)*2+114,"source creation quarter-vertex coordinate");
            check(p.x==cell.x-s.ground.sourceOriginX&&p.z==cell.y+(cell.x&1)*.5f-s.ground.sourceOriginY,"source half-grid creation transform, not axial rounding");
            int kind=PcFacilities.kind(i.facility),state=PcFacilities.state(i.facility);
            for(int month:new int[]{1,4,7,10})for(int lod:new int[]{0,1}){
                SceneMesh m=PcConstructibleWalls.mesh(library,s.ground,i,month,lod,p,mask);
                SceneMesh pillar=mask==0?library.mesh(kind,state,PcFacilities.quarter(month),lod,0):library.meshIndex(kind,0,kind==17?136:208,PcFacilities.quarter(month),lod,0);
                int nv=pillar.vertices.length/7,ni=pillar.indices.length;
                if(mask!=0){SceneMesh segment=library.meshIndex(kind,0,kind==17?130:196,PcFacilities.quarter(month),lod,0);nv+=Integer.bitCount(mask&56)*segment.vertices.length/7;ni+=Integer.bitCount(mask&56)*segment.indices.length;}
                check(m.pcWall&&m.pcFacility&&!m.pcCliffWall&&m.authoredTangentFrame,"independent dynamic source-wall tagging");
                check(m.vertices.length==nv*7&&m.indices.length==ni,"all original pillar/owned segment vertices and triangles retained once");
                for(float v:m.vertices)check(Float.isFinite(v),"finite source transformed attribute");for(float v:m.uv)check(v>=0&&v<=1,"source atlas bounds");for(float v:m.tangents)check(Float.isFinite(v),"finite inverse-transpose normal frame");for(int v:m.indices)check(v>=0&&v<nv,"full source triangle bounds");
            }
        }
    }
}
