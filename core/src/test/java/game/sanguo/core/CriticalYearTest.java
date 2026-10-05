package game.sanguo.core;

import java.util.Arrays;

/** Normal damaging commands across calendar boundaries; visual facts never enter saves. */
public final class CriticalYearTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        int[][] calendars={{207,1,0,207},{207,12,2,207},{207,12,3,208},{207,1,36,208},{207,6,93,210}};
        for(int[] date:calendars){
            var fixture=PcCriticalsFixture.preparePortrait("关羽",207,132,"calendar-boundary");
            World w=fixture.world;w.startYear=date[0];w.startMonth=date[1];w.turn=date[2];
            byte[] before=SaveCodec.encode(w);World reference=SaveCodec.decode(before);
            World.Result expected=fixture.command(reference);check(expected.ok,"independent normal tactic succeeds");
            TurnJournal journal=new TurnJournal(w);World.Result result=fixture.command(w);journal.close();
            check(result.ok&&result.critical!=null,"normal damaging tactic emits critical");
            check(result.critical.year==date[3],"applied critical uses current calendar year");
            check(journal.events().stream().anyMatch(e->e.critical==result.critical),"journal carries same immutable applied fact");
            check(Arrays.equals(SaveCodec.encode(reference),SaveCodec.encode(w)),"journal consumes no extra RNG and changes no saved state");
            byte[] after=SaveCodec.encode(w);World restored=SaveCodec.decode(after);
            check(Arrays.equals(after,SaveCodec.encode(restored)),"calendar boundary saves roundtrip exactly");
            w.startYear=1;w.startMonth=1;w.turn=0;
            check(result.critical.year==date[3],"later calendar changes cannot rewrite emitted fact");
            TurnJournal rejected=new TurnJournal(restored);World.Result failed=fixture.command(restored);rejected.close();
            check(!failed.ok&&failed.critical==null,"already acted command rejected without a critical");
            check(rejected.events().isEmpty(),"rejected command has no journal visual events");
            check(Arrays.equals(after,SaveCodec.encode(restored)),"rejection preserves complete saved state including RNG");
        }
        System.out.println("PASS "+checks+" critical calendar boundaries, immutable facts, normal commands, exact save/RNG");
    }
}
