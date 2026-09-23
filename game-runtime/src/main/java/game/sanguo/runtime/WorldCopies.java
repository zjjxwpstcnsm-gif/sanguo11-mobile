package game.sanguo.runtime;

import game.sanguo.core.SaveCodec;
import game.sanguo.core.World;
import java.io.IOException;

/** The existing bounded, explicit codec is also the transaction-isolation boundary. */
final class WorldCopies {
    private WorldCopies(){}
    static World copy(World source)throws IOException{
        World copy=SaveCodec.decode(SaveCodec.encode(source));
        copy.terrainRevision=source.terrainRevision;
        // Existing native renderer metadata remains out of the rule save.
        if(source.visualMap!=null)copy.visualMap=source.visualMap.copy();
        return copy;
    }
}
