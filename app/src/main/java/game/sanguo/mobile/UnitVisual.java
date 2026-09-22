package game.sanguo.mobile;

import game.sanguo.core.*;

/** Detached equipment and HUD state. Model keys describe actual core types, not invented units. */
final class UnitVisual {
    final int id, owner, troops, wounded, energy, food, gold, burning;
    final World.Weapon weapon;
    final Army.Ship ship;
    final War.Status status;
    final boolean transport, mission, naval, acted;
    final String commander, equipment;

    UnitVisual(World world, World.Unit unit) {
        id=unit.id;owner=unit.owner;troops=unit.troops;wounded=unit.wounded;
        energy=unit.energy;food=unit.food;gold=unit.gold;burning=unit.burning;
        weapon=unit.weapon;ship=unit.ship;status=unit.status;acted=unit.acted;
        mission=unit instanceof Domestic.Mission;
        transport=mission&&((Domestic.Mission)unit).transport;
        naval=world.army.water(unit.hex);
        World.Officer officer=world.officer(unit.officerId);
        commander=officer==null?"部队 #"+id:officer.name;
        equipment=world.army.equipmentLabel(unit);
    }
    String modelKey() {
        return naval?"ship/"+(mission?Army.Ship.BOAT:ship).name():mission?"transport":"weapon/"+weapon.name();
    }
    String label() {
        return commander+" · "+equipment+" · "+troops+"兵 · 气"+energy
            +(status==War.Status.NORMAL?"":" · "+status.label)+(burning>0?" · 起火":"");
    }
}
