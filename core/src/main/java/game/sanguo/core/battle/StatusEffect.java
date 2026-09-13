package game.sanguo.core.battle;

import java.util.Objects;

/** Durations are counted in the affected unit's own turns, never in global half-turns. */
public final class StatusEffect {
    public enum Kind {
        CONFUSED(true, true, 0.90, 0.80),
        IMMOBILIZED(false, true, 1.00, 1.00),
        MORALE_BREAK(false, false, 0.75, 0.90);
        public final boolean blocksAction, blocksMovement;
        public final double attackMultiplier, defenseMultiplier;
        Kind(boolean blocksAction, boolean blocksMovement, double attack, double defense) {
            this.blocksAction = blocksAction; this.blocksMovement = blocksMovement;
            this.attackMultiplier = attack; this.defenseMultiplier = defense;
        }
    }
    public final Kind kind;
    public final int remainingOwnerTurns;
    public StatusEffect(Kind kind, int remainingOwnerTurns) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (remainingOwnerTurns < 1 || remainingOwnerTurns > 99)
            throw new IllegalArgumentException("Status duration must be 1..99 owner turns");
        this.remainingOwnerTurns = remainingOwnerTurns;
    }
}
