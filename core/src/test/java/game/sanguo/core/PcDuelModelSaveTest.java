package game.sanguo.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Original full traces normalized only at pointer/reference fields. Rules and
 * RNG bytes are retained; this is model storage, not normal World acceptance. */
public final class PcDuelModelSaveTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static int[] ints(String value){String[]raw=value.split(":");int[]out=new int[raw.length];for(int i=0;i<raw.length;i++)out[i]=Integer.parseInt(raw[i]);return out;}
    static PcDuelModelSave.State normalized(byte[]raw,byte[]manager,int seed,int draws,int[]ids,int[]natives){
        PcDuelKernel m=new PcDuelKernel(raw);m.set(0,1);byte[]savedManager=manager.clone();
        for(int side=0;side<2;side++){
            m.set(0x238+24*side+4,1);PcDuelKernel.writeManager(savedManager,0x18+4*side,side);
            for(int slot=0;slot<3;slot++){int i=side*3+slot;m.set(m.fighter(side,slot),ids[i]);PcDuelKernel.writeManager(savedManager,12*side+4*slot,ids[i]);}
        }
        PcDuelKernel.Random random=new PcDuelKernel.Random(seed);random.draws=draws;
        return new PcDuelModelSave.State(m,savedManager,random,ids,natives);
    }
    public static void main(String[]args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("Pinned complete continuous corpus required");
        World w=PcScenarioCatalog.load("pc-scen000-843abd9f9702618fc95223b0993454252645e926c2d2a10e9384c411551e643c",2,23);
        Map<Integer,PcContestProfiles.Fact>source=PcContestProfiles.saved(w);byte[]worldBefore=SaveCodec.encode(w),manager=null;int[]ids=null,natives=null;int frames=0,draws=0,seed=23;PcDuelModelSave.State latest=null;
        for(String line:Files.readAllLines(Path.of(args[0]))){if(line.startsWith("#"))continue;String[]p=line.split("\t");
            if(p[0].equals("START")){
                manager=PcDuelKernel.hex(p[2]);ids=new int[6];natives=new int[6];String[]actors=p[7].split(",");
                for(int i=0;i<6;i++){natives[i]=ints(actors[i])[0];int nativeId=natives[i];ids[i]=source.values().stream().filter(f->f.nativeId==nativeId).findFirst().orElseThrow().officerId;}
                draws=0;seed=23;int target=(int)Long.parseLong(p[6]);while(seed!=target){seed=seed*0x6c078965+0x3039;if(++draws>1000)throw new AssertionError("initializer RNG transition absent");}latest=normalized(PcDuelKernel.hex(p[5]),manager,seed,draws,ids,natives);
            }else if(p[0].equals("FRAME")){
                frames++;int target=(int)Long.parseLong(p[4]),steps=0;while(seed!=target){seed=seed*0x6c078965+0x3039;draws++;if(++steps>1000)throw new AssertionError("original RNG transition absent");}latest=normalized(PcDuelKernel.hex(p[3]),manager,seed,draws,ids,natives);
            }else if(p[0].equals("END")){
                latest=normalized(latest.model.state,PcDuelKernel.hex(p[2]),latest.random.state,draws,ids,natives);
            }
            byte[]beforeModel=latest.model.state.clone(),beforeManager=latest.manager.clone();int beforeSeed=latest.random.state,beforeDraws=latest.random.draws;
            byte[]saved=PcDuelModelSave.write(latest);PcDuelModelSave.State copy=PcDuelModelSave.read(saved);
            check(Arrays.equals(saved,PcDuelModelSave.write(copy)),"all model/manager/RNG bytes roundtrip");
            check(Arrays.equals(beforeModel,latest.model.state)&&Arrays.equals(beforeManager,latest.manager)&&beforeSeed==latest.random.state&&beforeDraws==latest.random.draws,"saving draws no RNG or model mutations");
            check(Arrays.equals(ids,copy.officers)&&Arrays.equals(natives,copy.natives),"stable source joins retained");
        }
        check(frames>1000,"full original trace required");check(Arrays.equals(worldBefore,SaveCodec.encode(w)),"complete source World and saved RNG untouched");
        byte[]good=PcDuelModelSave.write(latest);for(byte[]bad:new byte[][]{Arrays.copyOf(good,good.length-1),new byte[1]}){boolean rejected=false;try{PcDuelModelSave.read(bad);}catch(IOException e){rejected=true;}check(rejected,"invalid length/hash rejected");}
        byte[]bad=good.clone();bad[100]^=1;boolean rejected=false;try{PcDuelModelSave.read(bad);}catch(IOException e){rejected=true;}check(rejected,"corrupt model rejected");
        PcDuelKernel.writeManager(latest.manager,0x64,3);rejected=false;try{PcDuelModelSave.write(latest);}catch(IOException e){rejected=true;}check(rejected,"unknown native disposition cannot enter a playable model save");PcDuelKernel.writeManager(latest.manager,0x64,0);
        latest.model.set(0x230,0xc600000);rejected=false;try{PcDuelModelSave.write(latest);}catch(IOException e){rejected=true;}check(rejected,"native renderer pointer cannot enter save");
        System.out.println("PASS complete native duel model storage frames="+frames+" checks="+checks+"; normal campaign Save/API/APK pending");
    }
}
