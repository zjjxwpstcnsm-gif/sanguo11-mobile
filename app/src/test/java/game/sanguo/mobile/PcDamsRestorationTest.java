package game.sanguo.mobile;

import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.nio.file.*;
import java.util.*;

/** Exact source transforms, opening/crop isolation and real destruction/save flood semantics. */
public final class PcDamsRestorationTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static MapSceneSnapshot scene(World w){return new MapSceneSnapshot(new MapSceneSnapshot.Ground(w),w,null,-1);}
    static final int[][] SOURCE={{678,155,71,367,200},{680,114,58,285,173},{682,145,90,347,238},{1182,182,16,421,89}};
    public static void main(String[] args)throws Exception{
        PcDams placements=new PcDams(Files.newInputStream(Path.of("app/src/main/assets/3d/pc-facilities/dams.pcz")));
        PcFacilities library=new PcFacilities(Files.newInputStream(Path.of("app/src/main/assets/3d/pc-facilities/facilities.pcz")));
        check(placements.placements().size()==4,"four exact source OBJS objects");
        float[] yaw={4.71238899230957f,0,3.1415927410125732f,1.5707963705062866f};
        for(int n=0;n<4;n++){PcDams.Placement p=placements.placements().get(n);int[] s=SOURCE[n];
            check(p.slot==s[0]&&p.sourceX==s[1]&&p.sourceY==s[2],"original slot and independent source-cell inversion");
            check(p.x==s[3]*.5f-28.5f&&p.z==s[4]*.5f-28.5f&&p.y==0&&p.yaw==yaw[n],"authored half-grid, original height and float yaw");
        }
        for(World w:ScenarioCatalog.all()){
            byte[] before=SaveCodec.encode(w);MapSceneSnapshot s=scene(w);int expected=0;
            for(int[] xy:SOURCE)if(xy[1]>=w.sourceOriginX&&xy[1]<w.sourceOriginX+w.sourceColumns()&&xy[2]>=w.sourceOriginY&&xy[2]<w.sourceOriginY+w.sourceRows()){
                expected++;Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(xy[1],xy[2]));War.Structure dam=w.war.at(h);
                check(dam!=null&&dam.kind==War.StructureKind.DAM&&dam.owner==-1&&dam.hp==1200&&dam.complete,"opening materializes one neutral source dam in its exact crop");
                check(w.terrain[h.q][h.r]==World.Terrain.SWAMP&&PcMap.get().terrain(xy[1],xy[2])==3,"source wetland raster never repainted for dam");
            }
            check(w.war.structures().stream().filter(d->d.kind==War.StructureKind.DAM).count()==expected,"no duplicate or out-of-crop dam");
            check(s.items.stream().filter(i->placements.placement(s.ground,i)!=null).count()==expected,"live source entities, not independent scenery clones");
            check(NaturalStructures.seedOpening(w)==0&&Arrays.equals(before,SaveCodec.encode(w)),"opening materialization is idempotent without RNG/id changes");
            verify(library,placements,s);check(Arrays.equals(before,SaveCodec.encode(w)),"source binding and all seasonal/LOD mesh queries preserve authority");
        }
        for(int n=0;n<4;n++)for(String mode:new String[]{"damage","destroy"}){
            PcDamsFixture.Case prepared=PcDamsFixture.prepare(n,mode);World w=prepared.world();byte[] before=SaveCodec.encode(w);MapSceneSnapshot old=scene(w);verify(library,placements,old);
            check(Arrays.equals(before,SaveCodec.encode(w)),"source rendering before real dam attack preserves entire save/RNG");
            World.Result result=prepared.command(w);check(result.ok,"normal source dam attack: "+result.message);byte[] after=SaveCodec.encode(w);MapSceneSnapshot s=scene(w);verify(library,placements,s);
            check(Arrays.equals(after,SaveCodec.encode(w)),"source rendering after attack preserves authoritative battle result/RNG");
            check(old.items.stream().filter(i->placements.placement(old.ground,i)!=null).count()==4,"old detached dam snapshot is immutable");
            if(mode.equals("destroy")){
                check(w.war.at(prepared.target())==null&&w.terrain[prepared.target().q][prepared.target().r]==World.Terrain.SHALLOWS,"normal attack removes authority and changes only dam ground through existing rules");
                check(w.unit(prepared.ally()).troops==4400&&w.unit(prepared.enemy()).troops==4400,"existing flood rules damage friendly and enemy lowland troops by exactly600");
                check(s.items.stream().filter(i->placements.placement(s.ground,i)!=null).count()==3,"destroyed source body disappears immediately");
            }else{
                War.Structure dam=w.war.at(prepared.target());check(dam!=null&&dam.hp>0&&dam.hp<500,"normal attack selects original damaged model state");
                check(w.unit(prepared.ally()).troops==5000&&w.unit(prepared.enemy()).troops==5000,"damage without destruction does not flood");
            }
            World restored=SaveCodec.decode(after);check(Arrays.equals(after,SaveCodec.encode(restored)),"dam command full save roundtrip without seeding");
            if(mode.equals("destroy"))check(restored.war.at(prepared.target())==null,"destroyed dam never respawns during save load");
            System.out.println("NORMAL_DAM slot="+SOURCE[n][0]+" "+mode+" "+result.message+" ally="+w.unit(prepared.ally()).troops+" enemy="+w.unit(prepared.enemy()).troops);
        }
        World old64=PcDamsFixture.legacy64();
        verifyOld64(SaveCodec.encode(old64),placements);
        for(String file:args){byte[] actual=Files.readAllBytes(Path.of(file));verifyOld64(actual,placements);System.out.println("ACTUAL_OLD64 "+file+" bytes="+actual.length);}
        System.out.println("PASS PC dams "+checks+" source/opening/crop/normal-command/flood/save/LOD checks; eight real attacks");
    }
    static void verifyOld64(byte[] bytes,PcDams placements)throws Exception{
        World w=SaveCodec.decode(bytes);check(w.mapRevision==64,"actual legacy PC revision accepted");int count=w.war.structures().size();byte[] canonical=SaveCodec.encode(w);verifyPlatformEnvelope(bytes,canonical);MapSceneSnapshot s=scene(w);
        check(s.ground.pcMap!=null,"revision64 retains genuine PC terrain");check(Arrays.equals(canonical,SaveCodec.encode(w)),"legacy render retains every byte/entity/RNG within the same runtime");
        check(NaturalStructures.seedOpening(w)==0&&w.war.structures().size()==count&&Arrays.equals(canonical,SaveCodec.encode(w)),"revision64 opening cannot introduce source65 entities");
        check(s.items.stream().filter(i->placements.placement(s.ground,i)!=null).count()==w.war.structures().stream().filter(d->d.kind==War.StructureKind.DAM&&Arrays.stream(SOURCE).anyMatch(xy->{var c=MapCoordinates.nationalSource(w,d.hex);return c.x==xy[1]&&c.y==xy[2];})).count(),"only actual saved source dams can render");
    }
    static void verifyPlatformEnvelope(byte[] original,byte[] canonical)throws Exception{
        if(Arrays.equals(original,canonical)){check(true,"legacy save byte-exact load within same gzip runtime");return;}
        // Unchanged BattleReports.write uses each runtime's GZIPOutputStream OS byte:
        // Android API29 writes0, host JDK17 writes255. No rule/entity field is exempted.
        check(original.length==canonical.length,"legacy payload length exact across runtimes");int changed=-1;
        for(int i=20;i<original.length;i++)if(original[i]!=canonical[i]){check(changed<0,"exactly one cross-runtime payload byte differs");changed=i;}
        check(changed>=29,"one gzip metadata byte, not save header/body truncation");int start=changed-9;
        check(original[start]==31&&original[start+1]==(byte)139&&original[start+2]==8&&original[start+3]==0,"difference is unflagged GZIP OS header field");
        check((original[changed]==0&&canonical[changed]==(byte)255)||(original[changed]==(byte)255&&canonical[changed]==0),"only Android/JDK gzip platform identities allowed");
        int length=java.nio.ByteBuffer.wrap(original,start-4,4).getInt();check(length>=18&&start+length<=original.length,"length-prefixed battle report gzip remains bounded");
        try(var a=new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(original,start,length));var b=new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(canonical,start,length))){check(Arrays.equals(a.readAllBytes(),b.readAllBytes()),"all decompressed report records byte-exact across runtimes");}
        byte[] restored=canonical.clone();restored[changed]=original[changed];java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(restored,20,restored.length-20);java.nio.ByteBuffer.wrap(restored).putLong(12,crc.getValue());
        check(Arrays.equals(original,restored),"every serialized byte matches original after only gzip OS and dependent CRC restoration");
        System.out.println("CROSS_RUNTIME_GZIP_ONLY offset="+changed+" Android_OS=0 JDK_OS=255; decompressed reports and all authority bytes identical");
    }
    static void verify(PcFacilities library,PcDams placements,MapSceneSnapshot s){
        for(MapSceneSnapshot.Item item:s.items){var p=placements.placement(s.ground,item);if(p==null)continue;
            check(PcFacilities.kind(item.facility)==20,"original source object family20");
            for(int month:new int[]{1,4,7,10})for(int lod:new int[]{0,1}){
                SceneMesh body=library.mesh(item,month,lod);check(body.pcFacility&&body.authoredTangentFrame,"dam uses source facility material/normal frame");
                check(body.vertices.length>0&&body.indices.length>0,"all source dam states have original bodies");for(float v:body.vertices)check(Float.isFinite(v),"finite original attribute");for(float v:body.uv)check(v>=0&&v<=1,"original atlas UV bounds");for(int v:body.indices)check(v>=0&&v<body.vertices.length/7,"original triangle bounds");
            }
        }
    }
}
