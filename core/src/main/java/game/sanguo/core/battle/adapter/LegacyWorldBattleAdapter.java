package game.sanguo.core.battle.adapter;

import game.sanguo.core.World;
import game.sanguo.core.battle.BattleUnit;
import game.sanguo.core.battle.HexPos;
import game.sanguo.core.battle.WeaponType;

/** Only this optional adapter knows legacy World classes. The tactical engine does not. */
public final class LegacyWorldBattleAdapter {
    private LegacyWorldBattleAdapter() { }
    public static BattleUnit toBattleUnit(World.Unit unit, World.Officer officer, HexPos localPosition) {
        if (unit == null || officer == null || unit.officerId != officer.id || unit.owner != officer.owner)
            throw new IllegalArgumentException("Strategic army/commander mismatch");
        BattleUnit.Commander commander = new BattleUnit.Commander(officer.id, officer.name,
                officer.leadership, officer.war, officer.intelligence);
        return BattleAdapters.toBattleUnit(new BattleAdapters.StrategicArmy(unit.id, unit.owner, commander,
                weapon(unit.weapon), unit.troops, unit.energy, 100, 100), localPosition);
    }
    private static WeaponType weapon(World.Weapon weapon) {
        if (weapon == null) throw new IllegalArgumentException("Missing strategic weapon");
        switch (weapon) {
            case SWORD: return WeaponType.SWORD;
            case RAM: return WeaponType.RAM;
            case SIEGE_TOWER: return WeaponType.SIEGE_TOWER;
            case WOODEN_BEAST: return WeaponType.WOODEN_BEAST;
            case CATAPULT: return WeaponType.CATAPULT;
            case SPEAR: return WeaponType.SPEAR;
            case HALBERD: return WeaponType.HALBERD;
            case CROSSBOW: return WeaponType.CROSSBOW;
            case CAVALRY: return WeaponType.CAVALRY;
            default: throw new IllegalArgumentException("Unmapped strategic weapon: " + weapon);
        }
    }
}
