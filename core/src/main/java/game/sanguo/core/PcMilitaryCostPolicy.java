package game.sanguo.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/** Original base fees saved only by explicit fresh PC-source creation.
 * Existing source/engineering saves keep their prior fees; no decode backfill. */
final class PcMilitaryCostPolicy {
    static final String NAMESPACE="pc-military-base-fees-v1";
    private static final int MAGIC=0x504d4631;
    private static final String SHARED_SHA="dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f";
    private static final Map<War.StructureKind,Integer> NATIVE=new EnumMap<>(War.StructureKind.class);
    static {
        NATIVE.put(War.StructureKind.CAMP,3);NATIVE.put(War.StructureKind.FORT,4);NATIVE.put(War.StructureKind.FORTRESS,5);
        NATIVE.put(War.StructureKind.ARROW_TOWER,6);NATIVE.put(War.StructureKind.CROSSBOW_TOWER,7);
        NATIVE.put(War.StructureKind.EARTH_WALL,8);NATIVE.put(War.StructureKind.STONE_WALL,9);
        NATIVE.put(War.StructureKind.CATAPULT_TOWER,10);NATIVE.put(War.StructureKind.DRUM,11);
        NATIVE.put(War.StructureKind.MUSIC,12);NATIVE.put(War.StructureKind.STONE_MAZE,13);
        NATIVE.put(War.StructureKind.FIRE_SEED,16);NATIVE.put(War.StructureKind.FLAME_SEED,17);
        NATIVE.put(War.StructureKind.FIRE_BALL,18);NATIVE.put(War.StructureKind.FLAME_BALL,19);
        NATIVE.put(War.StructureKind.FIRE_SHIP,20);NATIVE.put(War.StructureKind.INFERNO_BALL,21);NATIVE.put(War.StructureKind.INFERNO_SEED,22);
    }
    static boolean enabled(World w){byte[] raw=w.extensions.get(NAMESPACE);return w.pcSourceFrame&&raw!=null&&raw.length>=4&&ByteBuffer.wrap(raw).getInt()==MAGIC;}
    static void initializeOpening(World w)throws IOException{
        PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);
        if(!w.pcSourceFrame||source==null||!SHARED_SHA.equals(source.sharedSha)||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null)throw new IOException("Original military fees require explicit fresh source game");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(MAGIC);out.writeInt(1);
        for(String text:new String[]{source.scenarioId,source.sha,source.sourceVariant,source.sharedSha,PcScenarioIdentity.EXE_SHA})PcOfficerInfo.text(out,text);
        out.writeInt(NATIVE.size());for(Map.Entry<War.StructureKind,Integer> entry:NATIVE.entrySet()){
            PcOfficerInfo.text(out,entry.getKey().name());out.writeInt(entry.getValue());out.writeInt(PcFacilityCosts.at(entry.getValue()));
        }
        w.extensions.put(NAMESPACE,bytes.toByteArray());validate(w);
    }
    private static Map<War.StructureKind,Integer> read(World w)throws IOException{
        byte[] raw=w.extensions.get(NAMESPACE);if(!enabled(w)||raw.length>4096)throw new IOException("Original military fee policy missing/invalid");
        PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));
        if(in.readInt()!=MAGIC||in.readInt()!=1||source==null)throw new IOException("Original military fee policy version/source unknown");
        for(String text:new String[]{source.scenarioId,source.sha,source.sourceVariant,source.sharedSha,PcScenarioIdentity.EXE_SHA})if(!PcOfficerInfo.text(in,300).equals(text))throw new IOException("Saved military fee identity differs");
        if(!source.sharedSha.equals(SHARED_SHA)||in.readInt()!=NATIVE.size())throw new IOException("Saved military base definition coverage differs");
        Map<War.StructureKind,Integer> result=new EnumMap<>(War.StructureKind.class);
        for(Map.Entry<War.StructureKind,Integer> entry:NATIVE.entrySet()){
            if(!PcOfficerInfo.text(in,64).equals(entry.getKey().name())||in.readInt()!=entry.getValue())throw new IOException("Saved military fee definition identity differs");
            int price=PcOfficerInfo.bounded(in.readInt(),0,10000);
            if(price!=PcFacilityCosts.at(entry.getValue()))throw new IOException("Saved military fee differs from pinned Shared definition");result.put(entry.getKey(),price);
        }
        if(in.available()!=0)throw new IOException("Saved military fee trailing bytes");return result;
    }
    static int cost(World w,War.StructureKind kind){
        if(kind==null)return 0;if(!enabled(w))return kind.gold;
        try{return read(w).getOrDefault(kind,kind.gold);}catch(IOException e){throw new IllegalStateException(e);}
    }
    static void validate(World w)throws IOException{if(enabled(w))read(w);}
    private PcMilitaryCostPolicy(){}
}
