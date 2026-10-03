package game.sanguo.mobile;

import game.sanguo.core.*;

/** Detached equipment and HUD state. Model keys describe actual core types, not invented units. */
final class UnitVisual {
    final int id, owner, troops, wounded, energy, food, gold, burning;
    final World.Weapon weapon;
    final Army.Ship ship;
    final War.Status status;
    final boolean transport, mission, naval, acted;
    final String commander, equipment, allegiance;

    UnitVisual(World world, World.Unit unit) {
        id=unit.id;owner=unit.owner;troops=unit.troops;wounded=unit.wounded;
        energy=unit.energy;food=unit.food;gold=unit.gold;burning=unit.burning;
        weapon=unit.weapon;ship=unit.ship;status=unit.status;acted=unit.acted;
        mission=unit instanceof Domestic.Mission;
        transport=mission&&((Domestic.Mission)unit).transport;
        naval=world.army.water(unit.hex);
        World.Officer officer=world.officer(unit.officerId);
        commander=officer==null?"部队 #"+id:officer.name;
        equipment=world.army.equipmentLabel(unit);allegiance=owner==world.player?"我军":world.faction(owner);
    }
    boolean cavalry() { return !mission&&weapon==World.Weapon.CAVALRY; }
    boolean singleModel(boolean atSea) { return atSea||mission||Army.siegeWeapon(weapon); }
    int representatives(boolean atSea,int strength,int lod) {
        if(singleModel(atSea))return 1;
        int capacity=cavalry()?4:8,perMember=cavalry()?2500:1250;
        int count=strength<=0?1:Math.min(capacity,1+(strength-1)/perMember);
        return Math.min(count,lod>=2?1:lod==1?capacity/2:capacity);
    }
    String identity() { return allegiance+" · #"+id; }
    String modelKey() {
        return naval?"ship/"+(mission?Army.Ship.BOAT:ship).name():mission?"transport":"weapon/"+weapon.name();
    }
    String label() {
        return commander+" · "+equipment+" · "+troops+"兵 · 气"+energy
            +(status==War.Status.NORMAL?"":" · "+status.label)+(burning>0?" · 起火":"");
    }
}
