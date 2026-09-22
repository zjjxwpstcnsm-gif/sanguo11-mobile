package game.sanguo.core;

/** Wound-only changes must invalidate and survive detached playback snapshots. */
public final class UnitJournalStateTest {
    public static void main(String[] args) {
        World w=new World(12,12,"甲","乙"),visual=new World(12,12,"甲","乙");
        World.Unit u=new World.Unit(7,0,1,World.Weapon.SPEAR,new Hex(3,3),1000,4000);
        w.units.add(u);visual.units.add(new World.Unit(7,0,1,World.Weapon.SPEAR,u.hex,1000,4000));
        TurnJournal journal=new TurnJournal(w);u.wounded=91;u.woundRemainder=37;journal.checkpoint("伤兵");
        if(journal.events().isEmpty())throw new AssertionError("wound-only mutation omitted");
        for(TurnJournal.Event e:journal.events())e.applyVisual(visual);
        if(visual.unit(7).wounded!=91||visual.unit(7).woundRemainder!=37)throw new AssertionError("wounds lost in replay clone");
        u.wounded=92;journal.checkpoint("恢复");journal.close();
        if(visual.unit(7).wounded!=91)throw new AssertionError("snapshot aliases authority");
        for(TurnJournal.Event e:journal.events())e.applyVisual(visual);
        if(visual.unit(7).wounded!=92||u.wounded!=92)throw new AssertionError("final detached state mismatch");
        System.out.println("PASS journal wounded: delta detection, detached copies, final state");
    }
}
