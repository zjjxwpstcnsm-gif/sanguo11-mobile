package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
import game.sanguo.api.*;
import game.sanguo.runtime.*;

/** Complete World files emitted and continued by separate actual JVMs. */
public final class PcDebateCampaignColdTest {
    static void progress(GameSession game){
        LegacyView view=game.legacyView();Contests.Session s=view.draft.contests.current();PcDebateCampaign.Facts f=s.nativeDebate();if(!f.waitingCard)throw new AssertionError("Input boundary not reached");
        int slot=0;for(int i=1;i<f.hand.size();i++)if(s.nativeDebate.cardError(i)==null){slot=i;break;}
        if(!game.execute(ContestCommand.card(game.state(),s.id(),s.revision(),slot)).ok())throw new AssertionError("Actual command rejected");
    }
    public static void main(String[]args)throws Exception{
        Path folder=Path.of(args[1]);
        if(args[0].equals("emit")){
            if(Files.exists(folder))throw new IllegalArgumentException("Preserve existing files");Files.createDirectories(folder);String id=PcScenarioCatalog.all().get(0).identity.scenarioId;
            World p=PcScenarioCatalog.preview(id),w=PcScenarioCatalog.load(id,p.officer(10116).owner,23);PcNativeDebatePolicy.initializeOpening(w);
            try(GameSession game=new GameSession(w)){
                LegacyView view=game.legacyView();if(!game.legacy(view.draft,()->view.draft.contests.persuade(view.draft.officer(10116).cityId,10116,10222)).ok)throw new AssertionError("Actual trigger rejected");
                for(int i=0;i<4;i++)progress(game);Files.write(folder.resolve("mid.sg11"),game.captureSave());
                for(int i=0;i<5;i++)progress(game);Files.write(folder.resolve("control.sg11"),game.captureSave());
            }
            System.out.println("EMIT actual source World39 mid-debate file after four commands and full live-control file after five more; exit this JVM");
        }else if(args[0].equals("continue")){
            byte[] mid=Files.readAllBytes(folder.resolve("mid.sg11"));try(GameSession cold=new GameSession(SaveCodec.decode(mid))){
                if(!Arrays.equals(mid,cold.captureSave()))throw new AssertionError("Cold World/RNG decode changed bytes");
                for(int i=0;i<5;i++)progress(cold);if(!Arrays.equals(Files.readAllBytes(folder.resolve("control.sg11")),cold.captureSave()))throw new AssertionError("Cold actual source commands differ from live control");
            }
            System.out.println("PASS PcDebateCampaignColdTest independent JVM actual source World39/typed contest/full dual-RNG/events/identity byte-exact continuation; installed Android and original settlement pending");
        }else throw new IllegalArgumentException("Unknown mode");
    }
}
