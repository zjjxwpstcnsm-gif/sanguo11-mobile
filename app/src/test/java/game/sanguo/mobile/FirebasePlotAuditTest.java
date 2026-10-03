package game.sanguo.mobile;

import game.sanguo.core.*;
import java.security.MessageDigest;
import java.util.*;

/** Missing R11 event categories: actual commands, separate from device/touch acceptance. */
public final class FirebasePlotAuditTest {
    private static int checks;
    private static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }
    private static World copy(World w) throws Exception {
        return SaveCodec.decode(SaveCodec.encode(w));
    }
    private static void scenario(War.Plot plot, War.Status initialStatus) throws Exception {
        World initial = copy(FirebasePlotAuditFixture.world(plot, initialStatus));
        World authority = copy(initial), reference = copy(initial);
        Hex target = FirebasePlotAuditFixture.target();
        TurnJournal journal = new TurnJournal(authority);
        World.Result result = authority.war.plot(1, target, plot);
        journal.close();
        check(result.ok, "real command accepted: " + plot + " " + result.message);
        check(reference.war.plot(1, target, plot).ok, "unrecorded control command accepted");
        byte[] expected = SaveCodec.encode(reference);
        check(Arrays.equals(expected, SaveCodec.encode(authority)), "recording preserves full state/RNG");
        if (plot == War.Plot.CALM) check(authority.unitAt(target).status == War.Status.NORMAL,
                "CALM actually clears " + initialStatus);
        else check(authority.war.fireAt(target) == null, "EXTINGUISH actually removes fire");
        List<TurnJournal.Event> events = journal.events();
        long typed = events.stream().filter(e -> e.kind == TurnJournal.Kind.PLOT && e.plot == plot).count();
        check(typed == 1, "exactly one typed actual plot event");
        var ground = new MapSceneSnapshot.Ground(initial);
        CombatVisual visual = new CombatVisual();
        for (TurnJournal.Event event : events) {
            if (event.plot != plot) continue;
            check(event.target.equals(target), "effect target comes from actual journal");
            check(CombatVisual.style(event) == CombatVisual.Style.STATUS, "support plot is a status effect");
            for (int quality = 0; quality < 3; quality++) {
                visual.detail(quality);
                boolean visible = false;
                for (int frame = 0; frame <= 100; frame++) {
                    visual.sample(event, frame / 100f, ground);
                    visible |= visual.count > 0;
                    check(visual.count <= CombatVisual.CAPACITY, "bounded effect pool");
                    for (int i = 0; i < visual.count; i++) {
                        var p = visual.particles[i];
                        check(Float.isFinite(p.x + p.y + p.z + p.scale), "finite actual effect sample");
                    }
                }
                check(visible, "actual support effect sampled at quality " + quality);
            }
        }
        for (String mode : new String[]{"normal", "pause", "2x", "4x", "skip", "offscreen", "rebuild"}) {
            CombatReplayLedger ledger = new CombatReplayLedger();
            CombatSequence sequence = new CombatSequence(events, ledger);
            sequence.speed(mode.equals("2x") ? 2 : mode.equals("4x") ? 4 : 1);
            if (mode.equals("pause")) {
                sequence.advance(50, e -> true);
                float fraction = sequence.fraction();
                sequence.pause(true);
                sequence.advance(200, e -> true);
                check(sequence.fraction() == fraction, "pause retains phase");
                sequence.pause(false);
            }
            if (mode.equals("skip") || mode.equals("rebuild")) sequence.skip();
            for (int i = 0; i < 2000 && !sequence.done(); i++) sequence.advance(50, e -> !mode.equals("offscreen"));
            check(sequence.done(), "bounded completion " + mode);
            CombatSequence recreated = new CombatSequence(events, ledger);
            recreated.advance(50, e -> true);
            check(recreated.done(), "no replay after completed event " + mode);
            check(Arrays.equals(expected, SaveCodec.encode(authority)), "no repeated rule/RNG change " + mode);
        }
        System.out.println("PASS HOST_FIXTURE plot=" + plot + " initialStatus=" + initialStatus
                + " events=" + events.size() + " typed=" + typed + " savedSha256="
                + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(expected)));
    }
    public static void main(String[] args) throws Exception {
        scenario(War.Plot.CALM, War.Status.CONFUSED);
        scenario(War.Plot.CALM, War.Status.MISLED);
        scenario(War.Plot.EXTINGUISH, War.Status.NORMAL);
        System.out.println("PASS Firebase R11 supplemental host audit checks=" + checks
                + "; not device pixels, SAF, autosave IO, or pure-touch acceptance");
    }
}
