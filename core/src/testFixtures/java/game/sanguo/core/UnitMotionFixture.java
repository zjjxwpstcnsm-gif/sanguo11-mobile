package game.sanguo.core;
import java.util.*;

/** Explicit synthetic presentation paths; not evidence of gameplay route legality. */
public final class UnitMotionFixture {
    public static TurnJournal.Event move(int actor,Hex... path){
        return new TurnJournal.Event(TurnJournal.Kind.MOVE,actor,0,path[0],path[path.length-1],"test","test",
            Arrays.asList(path),Collections.emptyList(),Collections.emptyList(),Collections.emptyList(),null,null,"u"+actor,"SPEAR",Collections.emptyList(),Collections.emptyList());
    }
}
