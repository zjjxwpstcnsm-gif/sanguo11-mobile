package game.sanguo.mobile;

import java.nio.file.Files;
import java.nio.file.Path;
import game.sanguo.api.StateToken;

/** Expected tracks are actual original-executable dispatches, not implementation-derived fixtures. */
public final class PcMusicPolicyTest {
    private static int checks;
    private static void expected(int actual, int expected) {
        checks++;
        if (actual != expected) throw new AssertionError("track=" + actual + " expected=" + expected);
    }
    public static void main(String[] args) throws Exception {
        for (String row : Files.readAllLines(Path.of(args[0]))) {
            String[] c = row.split("\t");
            expected(PcMusicPolicy.select(true, !c[0].equals("0"), !c[1].equals("0"),
                !c[2].equals("0"), Integer.valueOf(c[3]), Integer.valueOf(c[4])), Integer.parseInt(c[5]));
        }
        expected(PcMusicPolicy.select(null,true,true,true,42,0), -1);
        expected(PcMusicPolicy.select(false,true,true,true,42,0), -1);
        expected(PcMusicPolicy.select(true,null,true,true,42,0), -1);
        expected(PcMusicPolicy.select(true,false,null,true,42,0), -1);
        expected(PcMusicPolicy.select(true,false,false,null,42,0), -1);
        expected(PcMusicPolicy.select(true,false,true,false,null,0), -1);
        expected(PcMusicPolicy.select(true,false,false,true,43,0), -1);
        expected(PcMusicPolicy.select(true,false,true,false,-1,0), -1);
        expected(PcMusicPolicy.select(true,false,false,false,null,null), -1);
        expected(PcMusicPolicy.select(true,true,null,null,null,null), 9);
        expected(PcMusicPolicy.select(true,false,true,null,9,null), 8);
        expected(PcMusicPolicy.select(true,false,true,null,10,null), 10);
        expected(PcMusicPolicy.select(true,false,false,true,9,null), 7);
        expected(PcMusicPolicy.select(true,false,false,true,10,null), 11);
        expected(PcMusicPolicy.select(true,false,false,false,null,-1), 7);
        expected(PcMusicPolicy.select(true,false,false,false,null,4), 7);
        StateToken state = new StateToken("source-music", 9007199254740993L, 9007199254740995L);
        for (int slot = -2; slot <= 9; slot++) {
            PcMapMusicDirective directive = new PcMapMusicDirective(state, "cue"+slot, "receipt", "phase",
                "source-established-map", slot, true, true, null, null, null, null);
            expected(directive.selectedMusicId(), slot >= 0 && slot <= 7 ? 9 : -1);
        }
        PcMapMusicDirective unknownControl = new PcMapMusicDirective(state, "unknown-control", "receipt", "phase",
            "source-established-map", null, true, true, false, false, 42, 0);
        expected(unknownControl.selectedMusicId(), -1);
        System.out.println("PASS original music policy comparisons=" + checks);
    }
}
