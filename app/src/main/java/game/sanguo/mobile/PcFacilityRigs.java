package game.sanguo.mobile;

import game.sanguo.core.TurnJournal;
import java.io.*;
import java.nio.*;
import java.util.zip.GZIPInputStream;

/** Original six-bone catapult-platform bodies. Detached presentation; no rule calls. */
final class PcFacilityRigs {
    // EXE 56f624 subtracts one second, evaluates at 60 FPS and ends at frames-2.
    static final int DELAY_MILLIS=1000,LAST_FRAME=79,DURATION_MILLIS=2317;
    private final PcUnits.Model[] models=new PcUnits.Model[2];
    private final PcUnits.Clip[] clips=new PcUnits.Clip[2];
    PcFacilityRigs(InputStream source)throws IOException{
        byte[] bytes;
        try(InputStream raw=source;InputStream in=new GZIPInputStream(raw);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1){if(out.size()+n>200000)throw new IOException("PC facility rig budget");out.write(block,0,n);}bytes=out.toByteArray();
        }
        try{
            ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if(b.getLong()!=0x3130474952464350L||b.getInt()!=2||b.getInt()!=2||b.getInt()!=73||b.getInt()!=74)throw new IOException("PC facility rig header");
            for(int i=0;i<2;i++){models[i]=PcUnits.readModel(b,2234+i);if(models[i].parents().length!=6)throw new IOException("PC facility rig hierarchy");}
            for(int i=0;i<2;i++){clips[i]=PcUnits.readClip(b);if(clips[i].nodes()!=6||clips[i].frames()!=81)throw new IOException("PC facility rig curves");}
            if(b.hasRemaining())throw new IOException("PC facility rig trailing bytes");
        }catch(BufferUnderflowException|IllegalArgumentException e){throw new IOException("PC facility rig truncated",e);}
    }
    static boolean firing(TurnJournal.Event e){return e!=null&&e.kind==TurnJournal.Kind.FACILITY_ATTACK&&e.sourceType.equals("CATAPULT_TOWER");}
    static int duration(TurnJournal.Event e){return firing(e)?DURATION_MILLIS:e.durationMillis();}
    static boolean supports(MapSceneSnapshot.Ground g,MapSceneSnapshot.Item item){return g.pcMap!=null&&item.facility!=null&&item.facility.complete&&item.facility.type.equals("military/CATAPULT_TOWER");}
    static int frame(float fraction){return Math.min(LAST_FRAME,Math.max(0,(int)((CombatVisual.fraction(fraction)*DURATION_MILLIS-DELAY_MILLIS)*.06f)));}
    static int frame(MapSceneSnapshot.Item item,TurnJournal.Event event,float fraction){return firing(event)&&item.key.equals("structure:"+event.sourceKey.substring(1))?frame(fraction):0;}
    static String key(MapSceneSnapshot.Item item,int month,int frame){return "pc-facility-rig:"+PcFacilities.state(item.facility)+":"+PcFacilities.quarter(month)+":"+frame;}
    SceneMesh mesh(MapSceneSnapshot.Item item,int month,int frame,PcFacilities materials)throws InterruptedException{return mesh(PcFacilities.state(item.facility),month,frame,materials);}
    SceneMesh mesh(int state,int month,float frame,PcFacilities materials)throws InterruptedException{
        if(state<0||state>1)throw new IllegalArgumentException("PC platform rig state");
        SceneMesh mesh=PcUnits.skin(models[state],clips[state],frame);
        materials.rigTexture(mesh,37+PcFacilities.textureOffset(44,state,PcFacilities.quarter(month),0,286+state));
        mesh.pcFacility=true;mesh.pcFacilityRig=true;return mesh;
    }
    SceneMesh rawPose(int state,float frame)throws InterruptedException{return PcUnits.skin(models[state],clips[state],frame);}
}
