package game.sanguo.core.battle;

import java.util.Objects;

/** Optional city/pass objective. A critical stronghold's capture wins the battle immediately. */
public final class Stronghold {
    public final int id, forceId, durability;
    public final HexPos position;
    public final boolean critical;
    public Stronghold(int id, int forceId, HexPos position, int durability, boolean critical) {
        if (id < 0 || forceId < 0 || durability < 1 || durability > BattleRules.MAX_TROOPS)
            throw new IllegalArgumentException("Invalid stronghold");
        this.id = id; this.forceId = forceId; this.position = Objects.requireNonNull(position);
        this.durability = durability; this.critical = critical;
    }
}
