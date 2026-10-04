package game.sanguo.core;
import java.nio.file.*;

/** Compile this identical probe against checkpoint21 and current production.
 * Only the newly added source snapshot is removed for whole-state comparison. */
public final class PcOfficerCampaignBaseline {
    static void save(Path folder,int index,int step,World w)throws Exception{
        byte[] snapshot=w.extensions.get("pc-officer-campaign-record-v1");
        w.extensions.put("pc-officer-campaign-record-v1",null);
        byte[] bytes=SaveCodec.encode(w);w.extensions.put("pc-officer-campaign-record-v1",snapshot);
        Files.write(folder.resolve(index+"-"+step+".sg11"),bytes);
    }
    public static void main(String[]args)throws Exception{
        Path folder=Path.of(args[0]);Files.createDirectories(folder);int index=0;
        for(PcScenarioCatalog.Source source:PcScenarioCatalog.all()){
            World w=PcScenarioCatalog.preview(source.identity.scenarioId);save(folder,index,0,w);
            World.City city=w.home();World.Officer actor=w.idle(city).get(0);
            if(!w.patrol(city.id,actor.id).ok)throw new AssertionError("Actual source patrol rejected");save(folder,index,1,w);
            for(int step=2;step<5;step++){if(!w.nextTurn().ok)throw new AssertionError("Actual source next turn rejected");save(folder,index,step,w);}
            System.out.println("PASS baseline source "+index+" "+source.identity.path);index++;
        }
        if(index!=16)throw new AssertionError("All16 sources required");
    }
}
