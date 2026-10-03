package game.sanguo.mobile;

import game.sanguo.core.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Source rig versus independent raw PC geometry plus normal firing and visual clock boundaries. */
public final class PcFacilityRigsRestorationTest {
    static int checks;static float maxError;
    static void check(boolean v,String name){checks++;if(!v)throw new AssertionError(name);}
    public static void main(String[] args)throws Exception{
        PcFacilityRigs rigs=new PcFacilityRigs(Files.newInputStream(Path.of("app/src/main/assets/3d/pc-facilities/rigs.pcz")));
        ByteBuffer b=ByteBuffer.wrap(Files.readAllBytes(Path.of(args[0]))).order(ByteOrder.LITTLE_ENDIAN);check(b.getLong()==0x3146455252464350L,"raw platform reference");int cases=b.getInt();check(cases==12,"both original full firing curves");
        for(int i=0;i<cases;i++){
            int state=b.getInt();float frame=b.getFloat();int nv=b.getInt(),ni=b.getInt(),opaque=b.getInt();SceneMesh mesh=rigs.rawPose(state,frame);
            check(mesh.vertices.length==nv*7&&mesh.indices.length==ni&&mesh.pcUnitOpaqueIndices==opaque,"complete original geometry and alpha groups");
            for(int v=0;v<nv;v++)for(int k=0;k<3;k++){float error=Math.abs(mesh.vertices[v*7+k]-b.getFloat());maxError=Math.max(maxError,error);check(error<2e-5f,"source hierarchy/polynomial pose");}
            for(int v=0;v<nv;v++)for(int k=0;k<4;k++)check(Float.floatToRawIntBits(mesh.vertices[v*7+3+k])==Float.floatToRawIntBits(b.getFloat()),"source RGBA exact");
            for(float uv:mesh.uv)check(Float.floatToRawIntBits(uv)==Float.floatToRawIntBits(b.getFloat()),"source UV exact before seasonal atlas binding");
            for(int index:mesh.indices)check(index==b.getInt(),"source strip topology exact");for(float t:mesh.tangents)check(Float.isFinite(t),"source normal finite");
        }
        check(!b.hasRemaining(),"raw reference consumed");
        check(PcFacilityRigs.frame(0)==0&&PcFacilityRigs.frame(999/2317f)==0&&PcFacilityRigs.frame(1250/2317f)==15&&PcFacilityRigs.frame(1)==79,"source delay,60Hz and original end guard");
        for(boolean damaged:new boolean[]{false,true}){
            var fixture=PcFacilityRigsFixture.prepare(damaged);byte[] initial=SaveCodec.encode(fixture.world());check(Arrays.equals(initial,SaveCodec.encode(SaveCodec.decode(initial))),"fixture strict save roundtrip");World reference=SaveCodec.decode(initial);check(fixture.command(reference).ok,"normal independent next-turn firing");
            World recorded=SaveCodec.decode(initial);TurnJournal journal=new TurnJournal(recorded);check(fixture.command(recorded).ok,"normal recorded next-turn firing");journal.close();check(Arrays.equals(SaveCodec.encode(reference),SaveCodec.encode(recorded)),"journal/visuals do not alter full authority or RNG");
            TurnJournal.Event firing=journal.events().stream().filter(e->PcFacilityRigs.firing(e)&&e.sourceKey.equals("s"+fixture.tower())).findFirst().orElseThrow();
            MapSceneSnapshot before=new MapSceneSnapshot(new MapSceneSnapshot.Ground(fixture.world()),fixture.world(),null,-1);var item=before.items.stream().filter(i->i.key.equals("structure:"+fixture.tower())).findFirst().orElseThrow();
            check(PcFacilityRigs.supports(before.ground,item)&&PcFacilities.state(item.facility)==(damaged?1:0),"live original normal/damaged body state");check(PcFacilityRigs.frame(item,firing,1)==79,"real facility event drives original curve");
            CombatSequence sequence=new CombatSequence(List.of(firing),new CombatReplayLedger(),PcFacilityRigs::duration);for(int step=0;step<20;step++)sequence.advance(50,e->true);check(!sequence.done()&&PcFacilityRigs.frame(sequence.fraction())==0,"original one-second prelaunch retained");sequence.pause(true);float paused=sequence.fraction();sequence.advance(500,e->true);check(paused==sequence.fraction(),"paused source clock");sequence.pause(false);sequence.speed(4);for(int step=0;step<7;step++)sequence.advance(50,e->true);check(sequence.done(),"accelerated source clock finishes normally");
            check(Arrays.equals(initial,SaveCodec.encode(fixture.world())),"all source rendering leaves initial save untouched");System.out.println("NORMAL_PLATFORM damaged="+damaged+" events="+journal.events().size()+" firing="+firing.message);
        }
        System.out.println("PASS PC PLATFORM raw cases="+cases+" checks="+checks+" max display error="+maxError+"; installed and PC rendered reference separately required");
    }
}
