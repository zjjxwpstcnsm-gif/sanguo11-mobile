package game.sanguo.core;

import java.util.*;

/** Independent normal source-map command and strict save preconditions for installed verification. */
public final class PcUnitCommandsProbe {
    public static void main(String[] args)throws Exception{
        int cases=0;
        for(String kind:PcUnitsFixture.KINDS)for(String mode:new String[]{"move","attack"}){
            if(kind.equals("transport")&&mode.equals("attack"))continue;
            var c=PcUnitsFixture.prepare(kind,mode);byte[] before=SaveCodec.encode(c.world());World w=SaveCodec.decode(before);
            if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("fixture roundtrip "+kind+"/"+mode);
            TurnJournal journal=new TurnJournal(w);World.Result result=c.command(w);journal.close();
            if(!result.ok||journal.events().isEmpty())throw new AssertionError("normal command "+kind+"/"+mode+": "+result.message);
            byte[] after=SaveCodec.encode(w);if(!Arrays.equals(after,SaveCodec.encode(SaveCodec.decode(after))))throw new AssertionError("post-command roundtrip");
            System.out.println("SOURCE_COMMAND "+kind+"/"+mode+" focus="+MapCoordinates.nationalSource(w,c.focus())+" events="+journal.events().size()+" "+result.message);cases++;
        }
        System.out.println("PASS PC unit source-map normal commands cases="+cases+"; installed rendering separately required");
    }
}
