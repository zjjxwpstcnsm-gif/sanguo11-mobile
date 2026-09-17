package game.sanguo.core.battle;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable public unit snapshot. Only the engine replaces live state; callers cannot mutate it. */
public final class BattleUnit {
    public static final class Commander {
        public final int id, leadership, war, intelligence;
        public final String name;
        public Commander(int id, String name, int leadership, int war, int intelligence) {
            if (id < 0 || leadership < 0 || leadership > 100 || war < 0 || war > 100
                    || intelligence < 0 || intelligence > 100)
                throw new IllegalArgumentException("Invalid commander identity or attributes (0..100)");
            this.id = id; this.name = Objects.requireNonNull(name);
            this.leadership = leadership; this.war = war; this.intelligence = intelligence;
        }
    }
    public final int id, forceId, troopCount, energy, movement, movementRemaining, attack, defense;
    public final Commander commander;
    public final HexPos position;
    public final WeaponType weaponType;
    public final boolean actedThisTurn, movedThisTurn;
    public final Map<StatusEffect.Kind, StatusEffect> status;

    public BattleUnit(int id, int forceId, Commander commander, WeaponType weapon,
                      HexPos position, int troops, int energy, int attack, int defense) {
        this(id, forceId, commander, weapon, position, troops, energy, attack, defense,
                BattleRules.weapon(weapon).movement, false, false, Collections.emptyMap());
    }
    private BattleUnit(int id, int forceId, Commander commander, WeaponType weapon,
                       HexPos position, int troops, int energy, int attack, int defense,
                       int remaining, boolean acted, boolean moved,
                       Map<StatusEffect.Kind, StatusEffect> status) {
        if (id < 0 || forceId < 0 || troops < 0 || troops > BattleRules.MAX_TROOPS
                || energy < 0 || energy > BattleRules.MAX_ENERGY || attack < 1 || attack > 300
                || defense < 1 || defense > 300)
            throw new IllegalArgumentException("Invalid unit identity, troops, energy, attack or defense");
        this.id = id; this.forceId = forceId; this.commander = Objects.requireNonNull(commander);
        this.weaponType = Objects.requireNonNull(weapon); this.position = Objects.requireNonNull(position);
        this.movement = BattleRules.weapon(weapon).movement;
        if (remaining < 0 || remaining > movement) throw new IllegalArgumentException("Invalid movement budget");
        this.troopCount = troops; this.energy = energy; this.attack = attack; this.defense = defense;
        this.movementRemaining = remaining; this.actedThisTurn = acted; this.movedThisTurn = moved;
        EnumMap<StatusEffect.Kind, StatusEffect> copy = new EnumMap<>(StatusEffect.Kind.class);
        copy.putAll(status); this.status = Collections.unmodifiableMap(copy);
    }
    public boolean alive() { return troopCount > 0; }
    public boolean actionBlocked() {
        for (StatusEffect effect : status.values()) if (effect.kind.blocksAction) return true;
        return false;
    }
    public boolean movementBlocked() {
        for (StatusEffect effect : status.values()) if (effect.kind.blocksMovement) return true;
        return false;
    }
    public double attackStatusMultiplier() {
        double factor = 1;
        for (StatusEffect effect : status.values()) factor *= effect.kind.attackMultiplier;
        return factor;
    }
    public double defenseStatusMultiplier() {
        double factor = 1;
        for (StatusEffect effect : status.values()) factor *= effect.kind.defenseMultiplier;
        return factor;
    }
    /** Scenario/adapter convenience: returns a new value; does not change an engine's state. */
    public BattleUnit withEnergy(int energy) {
        return copy(troopCount, energy, position, movementRemaining, actedThisTurn, movedThisTurn, status);
    }
    /** Refreshes to the longer duration instead of stacking identical status multipliers. */
    public BattleUnit withStatus(StatusEffect effect) {
        EnumMap<StatusEffect.Kind, StatusEffect> copy = new EnumMap<>(StatusEffect.Kind.class);
        copy.putAll(status);
        StatusEffect previous = copy.get(effect.kind);
        if (previous == null || previous.remainingOwnerTurns < effect.remainingOwnerTurns) copy.put(effect.kind, effect);
        return copy(troopCount, energy, position, movementRemaining, actedThisTurn, movedThisTurn, copy);
    }
    BattleUnit copy(int troops, int energy, HexPos position, int remaining, boolean acted,
                    boolean moved, Map<StatusEffect.Kind, StatusEffect> status) {
        return new BattleUnit(id, forceId, commander, weaponType, position, troops, energy,
                attack, defense, remaining, acted, moved, status);
    }
    BattleUnit startOwnerTurn() {
        return copy(troopCount, Math.min(BattleRules.MAX_ENERGY, energy + BattleRules.TURN_ENERGY_RECOVERY),
                position, movementBlocked() ? 0 : movement, actionBlocked(), false, status);
    }
    BattleUnit endOwnerTurn() {
        EnumMap<StatusEffect.Kind, StatusEffect> next = new EnumMap<>(StatusEffect.Kind.class);
        for (StatusEffect effect : status.values()) if (effect.remainingOwnerTurns > 1)
            next.put(effect.kind, new StatusEffect(effect.kind, effect.remainingOwnerTurns - 1));
        return copy(troopCount, energy, position, 0, true, movedThisTurn, next);
    }
}
