package game.sanguo.core;

/** Explicit host-only setup. Never installed as app data or used by the touch runner. */
public final class FirebasePlotAuditFixture {
    private FirebasePlotAuditFixture() {}
    public static World world(War.Plot plot, War.Status status) {
        World w = CombatSceneFixture.world("counter");
        w.scenarioName = "Firebase audit HOST FIXTURE: " + plot + "/" + status;
        World.Unit ally = new World.Unit(w.nextUnitId++, 0, 0, World.Weapon.SPEAR,
                new Hex(8, 9), 6000, 12000);
        ally.status = status;
        ally.statusTurns = status == War.Status.NORMAL ? 0 : 2;
        w.units.add(ally);
        w.officer(0).cityId = -1;
        w.officer(0).unitId = ally.id;
        w.city(0).governorId = -1; // Fixture ruler is in the field, not a city governor.
        if (plot == War.Plot.EXTINGUISH) {
            w.war.fires.add(new War.Fire(ally.hex, 1, 2));
        }
        return w;
    }
    public static Hex target() { return new Hex(8, 9); }
}
