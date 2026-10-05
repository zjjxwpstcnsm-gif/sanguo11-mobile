package game.sanguo.runtime;
import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.query.SnapshotQuery;
import java.util.*;

/** Validate the projection against the previous complete-codec copy boundary. */
public final class SnapshotReadOnlyTest {
    private static int checks;
    private static void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
    private static void same(GameSnapshot a,GameSnapshot b){
        check(a.state.equals(b.state)&&a.width==b.width&&a.height==b.height&&a.mapRevision==b.mapRevision&&a.terrainRevision==b.terrainRevision&&a.turn==b.turn&&a.player==b.player,"all scalar/token facts equal codec projection");
        check(a.terrain.equals(b.terrain)&&a.entities.equals(b.entities),"every terrain cell and entity equals codec projection");
        check(a.layout.columnStaggered==b.layout.columnStaggered&&a.layout.offset==b.layout.offset&&a.layout.sourceOriginX==b.layout.sourceOriginX&&a.layout.sourceOriginY==b.layout.sourceOriginY,"layout identity retained");
    }
    private static GameSnapshot verify(GameSession game)throws Exception{
        byte[] before=game.captureSave();StateToken token=game.state();
        GameSnapshot actual=game.snapshot(),control=SnapshotQuery.capture(game.legacyView().draft,token);same(actual,control);
        check(Arrays.equals(before,game.captureSave())&&token.equals(game.state()),"projection changes no saved state/RNG/token");
        boolean immutable=false;try{actual.entities.clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"entity collection immutable");
        return actual;
    }
    public static void main(String[] args)throws Exception{
        for(ScenarioCatalog.Summary row:ScenarioCatalog.summaries()){
          World opening=ScenarioCatalog.load(row.id,0,12345);opening.terrainRevision=42;
          try(GameSession game=new GameSession(opening)){
            GameSnapshot retained=verify(game);String terrain=retained.terrain;List<?> entities=new ArrayList<>(retained.entities);
            TurnTicket ticket=game.beginTurn();verify(game);World computed=SaveCodec.decode(ticket.initial());
            check(computed.nextTurn().ok,"actual full turn "+row.id);check(game.commitTurn(ticket,computed),"serial commit "+row.id);verify(game);
            check(retained.terrain.equals(terrain)&&retained.entities.equals(entities)&&retained.turn+1==game.snapshot().turn,"retained snapshot unaffected by later authority");
            byte[] saved=game.captureSave();Throwable[] error={null};Thread wrong=new Thread(()->{try{game.snapshot();}catch(Throwable expected){error[0]=expected;}});wrong.start();wrong.join();
            check(error[0] instanceof IllegalStateException&&Arrays.equals(saved,game.captureSave()),"wrong thread rejected without writes");
            game.close();boolean closed=false;try{game.snapshot();}catch(IllegalStateException expected){closed=true;}check(closed,"closed session rejected");
          }
        }
        System.out.println("PASS SnapshotReadOnlyTest checks="+checks+" actual9openings/fullturns/busy/retained/wrongthread/closed, every cell/entity/token and full-save/RNG match old codec projection");
    }
}
