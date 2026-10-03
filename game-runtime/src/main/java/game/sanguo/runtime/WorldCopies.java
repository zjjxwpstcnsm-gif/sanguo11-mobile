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
    static World transactionCopy(World source)throws IOException{
        World copy=copy(source);copy.techniquePointsJournal.begin();return copy;
    }
    static World committedCopy(World source)throws IOException{
        long sequence=0;
        for(var fact:source.techniquePointsJournal.facts())
            if(fact.sequence!=++sequence||fact.owner<0||fact.owner>=source.factions.length||fact.before<0||fact.after<0||fact.before==fact.after||fact.cause==null||fact.phase==null)
                throw new IOException("Invalid transient point fact");
        World copy=copy(source);copy.techniquePointsJournal.inherit(source.techniquePointsJournal);return copy;
    }
}
