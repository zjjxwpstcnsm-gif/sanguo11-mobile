package game.sanguo.core.battle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable strategic handoff. Commander candidates are NOT automatically captured. */
public final class BattleResult {
    public enum Reason { ELIMINATION, CRITICAL_STRONGHOLD_CAPTURED, TURN_LIMIT }
    /** null means a draw (TURN_LIMIT), never an invented winner. */
    public final Integer winner;
    public final Reason reason;
    public final int completedRounds;
    /** Unit id -> surviving troop count. Includes zero for defeated units for conservation checks. */
    public final Map<Integer, Integer> survivingTroops, casualties;
    public final List<Integer> defeatedUnits, capturedCommanderCandidates;
    /** Strategic city/stronghold id -> final capturing force id. */
    public final Map<Integer, Integer> capturedCities;
    public final boolean cityCaptured;
    BattleResult(Integer winner, Reason reason, int rounds, Collection<BattleUnit> units,
                 Map<Integer, Integer> initialTroops, Map<Integer, Integer> capturedCities) {
        this.winner = winner; this.reason = reason; this.completedRounds = rounds;
        Map<Integer, Integer> remaining = new LinkedHashMap<>(), losses = new LinkedHashMap<>();
        List<Integer> defeated = new ArrayList<>(), candidates = new ArrayList<>();
        for (BattleUnit unit : units) {
            remaining.put(unit.id, unit.troopCount);
            losses.put(unit.id, initialTroops.get(unit.id) - unit.troopCount);
            if (!unit.alive()) {
                defeated.add(unit.id);
                if (winner != null && unit.forceId != winner) candidates.add(unit.commander.id);
            }
        }
        survivingTroops = Collections.unmodifiableMap(remaining);
        casualties = Collections.unmodifiableMap(losses);
        defeatedUnits = Collections.unmodifiableList(defeated);
        capturedCommanderCandidates = Collections.unmodifiableList(candidates);
        this.capturedCities = Collections.unmodifiableMap(new LinkedHashMap<>(capturedCities));
        cityCaptured = !capturedCities.isEmpty();
    }
}
