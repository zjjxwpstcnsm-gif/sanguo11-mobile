package game.sanguo.core.battle.adapter;

import game.sanguo.core.battle.BattleResult;
import game.sanguo.core.battle.BattleUnit;
import game.sanguo.core.battle.HexPos;
import game.sanguo.core.battle.WeaponType;
import java.util.List;
import java.util.Map;

/** Value-only strategic boundary. Neither conversion mutates a strategic world or a save. */
public final class BattleAdapters {
    private BattleAdapters() { }
    public static final class StrategicArmy {
        public final int id, forceId, troopCount, energy, attack, defense;
        public final BattleUnit.Commander commander;
        public final WeaponType weapon;
        public StrategicArmy(int id, int forceId, BattleUnit.Commander commander, WeaponType weapon,
                             int troops, int energy, int attack, int defense) {
            this.id = id; this.forceId = forceId; this.commander = commander; this.weapon = weapon;
            this.troopCount = troops; this.energy = energy; this.attack = attack; this.defense = defense;
        }
    }
    public static final class StrategicResult {
        public final Integer winner;
        public final Map<Integer, Integer> survivingTroops, casualties, capturedCities;
        public final List<Integer> defeatedUnits, capturedCommanderCandidates;
        public final boolean cityCaptured;
        private StrategicResult(BattleResult result) {
            winner = result.winner; survivingTroops = result.survivingTroops; casualties = result.casualties;
            capturedCities = result.capturedCities; defeatedUnits = result.defeatedUnits;
            capturedCommanderCandidates = result.capturedCommanderCandidates; cityCaptured = result.cityCaptured;
        }
    }
    public static BattleUnit toBattleUnit(StrategicArmy army, HexPos localPosition) {
        return new BattleUnit(army.id, army.forceId, army.commander, army.weapon, localPosition,
                army.troopCount, army.energy, army.attack, army.defense);
    }
    public static StrategicResult toStrategicResult(BattleResult result) { return new StrategicResult(result); }
}
