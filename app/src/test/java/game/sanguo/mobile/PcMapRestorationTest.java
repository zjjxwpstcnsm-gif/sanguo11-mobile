package game.sanguo.mobile;

import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.util.*;

/** Source-grid alignment, authored geometry, picking and legacy/save isolation. */
public final class PcMapRestorationTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.load("heroes-250",0);
        byte[] before=SaveCodec.encode(w);PcMap pc=PcMap.get();
        if(args.length==1){
            // Independent supplied-EXE result:256 float32 planes, followed by
            // every source face byte in runtime x-fast order. This catches
            // byte truncation, bias, transposition and zero-water handling.
            try(var in=new java.io.DataInputStream(new java.io.FileInputStream(args[0]))){
                check(in.readLong()==0x5043575452303031L,"water source reference version");
                float[] planes=new float[256];
                for(int i=0;i<planes.length;i++)planes[i]=Float.intBitsToFloat(Integer.reverseBytes(in.readInt()))*PcMap.WORLD_SCALE;
                int correctedHigh=0,formerlyMissing=0;
                for(int iz=0;iz<1024;iz++)for(int ix=0;ix<1024;ix++){
                    int value=in.readUnsignedByte();
                    float actual=pc.waterHeight((ix+.5f-PcMap.ORIGIN)/4,(iz+.5f-PcMap.ORIGIN)/4);
                    check(Float.floatToRawIntBits(actual)==Float.floatToRawIntBits(planes[value]),"source machine water plane exact at "+ix+","+iz);
                    if(value>15)correctedHigh++;
                    if(value>0&&(value&15)==0)formerlyMissing++;
                }
                check(correctedHigh==14010&&formerlyMissing==3797,"all source faces damaged by former four-bit field are recovered");
                check(in.read()==-1,"complete independent source water reference");
            }
        }
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
        check(g.pcMap!=null&&w.mapRevision==65,"new national world uses PC surface");
        int plots=0;
        for(World.City c:w.cities){
            SourceGridCoord s=MapCoordinates.nationalSource(w,c.hex);
            check(pc.terrain(s.x,s.y)==(c.kind==World.SiteKind.CITY?16:c.kind==World.SiteKind.PORT?17:18),"PC site position "+c.name);
            for(Hex h:w.development.parcels(c.id)){
                SourceGridCoord p=MapCoordinates.nationalSource(w,h);
                check(pc.development(p.x,p.y),"development parcel from source flag");plots++;
            }
        }
        check(plots==591,"all original development parcels");
        MapWater waterways=new MapWater(w);
        for(World.City c:w.cities)if(c.kind==World.SiteKind.PORT){
            check(waterways.portError(c)==null,"authored port connects to production embarkation: "+c.name);
        }
        for(int y=0;y<200;y++)for(int x=0;x<200;x++){
            Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(x,y));
            check(g.valid(h),"full source rectangle");
            float wx=g.grid.x(h),wz=g.grid.z(h);
            float expected=pc.height(x,y+(x&1)*.5f);
            check(Math.abs(g.surface.at(h)-expected)<1e-5f,"column parity and raster origin "+x+","+y+" world="+wx+","+wz+" actual="+g.surface.at(h)+" expected="+expected);
            check(expected>=0&&expected<=PcMap.MAX_HEIGHT,"height bounds");
            check(g.shoreline.dx(wx+.5f,wz)==0&&g.shoreline.dz(wx,wz+.5f)==0,"authored shore is not warped");
        }
        // Build the real streaming path at Luoyang and verify every uploaded vertex.
        World.City city=w.city(20015);float x=g.grid.x(city.hex),z=g.grid.z(city.hex);
        List<SceneMesh> chunks=SceneMesh.ground(g,Collections.emptyList(),new SceneMesh.TerrainWindow(x,z,3,3,10));
        check(!chunks.isEmpty(),"streamed ground");
        for(SceneMesh m:chunks){
            for(int i=0;i<m.landIndexCount;i++){
                int v=m.indices[i]*7;
                check(Math.abs(m.vertices[v+1]-g.surface.pcLandHeight(m.vertices[v],m.vertices[v+2]))<1e-5f,"land mesh preserves source heights under water");
            }
            // Native422980 emits full coarse quads with alpha32 at dry corners.
            // The historical fine-clipping assertion and implementation are kept
            // in fixtures/native-v146; they cannot describe native full quads.
            check(m.landIndexCount==m.indices.length,"original ground never binds legacy fine-face water material");
            if(m.sourceWater!=null)PcWaterRestorationTest.checkMesh(m.sourceWater,g,null);
            check(m.indices.length%3==0&&m.surfaceData.length==m.vertices.length/7*2,"complete compact native UV streams");
            for(int v=0;v<m.vertices.length/7;v++)check(m.surfaceData[v*2]==m.vertices[v*7]&&m.surfaceData[v*2+1]==-m.vertices[v*7+2],"native UV0 keeps exact world source coordinate");
        }
        SceneCamera camera=new SceneCamera();camera.width=1000;camera.height=800;camera.x=x;camera.z=z;camera.span=8;
        float y=g.surface.at(city.hex),sx=camera.screenX(x,z),sy=camera.screenY(x,z,y);
        float hit=g.surface.rayHeight(camera,sx,sy);
        check(Float.isFinite(hit)&&hit>=y-1e-4,"triangle ray reaches visible PC terrain");
        check(g.surface.pick(camera,sx,sy)!=null,"native source grid picking");
        // The former2.6 ray interval clipped valid source mountains once the
        // proven uniform scale was restored. Exercise an actual high summit.
        Hex summit=null;float summitY=0;
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++)if(g.valid(q,r)){
            Hex h=new Hex(q,r);float value=g.surface.at(h);if(value>summitY){summit=h;summitY=value;}
        }
        check(summit!=null&&summitY>TerrainSurface.MAX_HEIGHT,"source peak exceeds old flattened display ceiling");
        camera.x=g.grid.x(summit);camera.z=g.grid.z(summit);camera.clampTo(g);
        check(camera.heightLimit==PcMap.MAX_HEIGHT,"source culling window uses complete authored height interval");
        float summitHit=g.surface.rayHeight(camera,camera.screenX(camera.x,camera.z),camera.screenY(camera.x,camera.z,summitY));
        check(Float.isFinite(summitHit)&&summitHit>=summitY-1e-4f,"high source summit remains triangle-pickable");
        check(Arrays.equals(before,SaveCodec.encode(w)),"renderer never mutates rules, RNG or saves");
        World restored=SaveCodec.decode(before);
        check(Arrays.equals(before,SaveCodec.encode(restored)),"revision65 save roundtrip");
        restored.mapRevision=63;
        MapSceneSnapshot.Ground legacy=new MapSceneSnapshot.Ground(restored);
        check(legacy.pcMap==null,"legacy save keeps its original visual surface");
        NationalMap.validateIdentity(restored);check(true,"revision63 identity still accepted");
        try(var in=new java.util.zip.GZIPInputStream(PcMapRestorationTest.class.getResourceAsStream("/pc-map-v063.sg11.gz"))){
            byte[] old=in.readAllBytes();World oldWorld=SaveCodec.decode(old);
            check(oldWorld.mapRevision==63&&Arrays.equals(old,SaveCodec.encode(oldWorld)),"actual pre-change save retains exact geography/state");
            check(new MapSceneSnapshot.Ground(oldWorld).pcMap==null,"actual revision63 save retains procedural renderer");
            int blocked=0;for(int q=0;q<oldWorld.width;q++)for(int r=0;r<oldWorld.height;r++)if(NationalMap.restricted(oldWorld,new Hex(q,r)))blocked++;
            check(blocked==14,"actual revision63 movement restrictions retained");
        }
        for(World crop:ScenarioCatalog.all())check(new MapSceneSnapshot.Ground(crop).pcMap!=null,"national crops share PC origin");
        System.out.println("PASS PC map restoration "+checks+" source/mesh/picking/save checks; chunks="+chunks.size());
    }
}
