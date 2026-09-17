package game.sanguo.core.battle;

import java.util.Objects;
import java.util.Random;

/** Register new tactics without adding branches to BattleEngine. Implementations must be stateless. */
public interface Tactic {
    final class Definition {
        public final String id, name;
        public final WeaponType weapon;
        public final int energyCost, minRange, maxRange;
        public final double damageMultiplier;
        public Definition(String id, String name, WeaponType weapon, int cost, int minRange,
                          int maxRange, double damageMultiplier) {
            this.id = Objects.requireNonNull(id); this.name = Objects.requireNonNull(name);
            BattleRules.weapon(weapon); this.weapon = weapon;
            if (id.isEmpty() || cost < 1 || cost > BattleRules.MAX_ENERGY || minRange < 1
                    || maxRange < minRange || maxRange > 6 || !Double.isFinite(damageMultiplier)
                    || damageMultiplier <= 0 || damageMultiplier > 3)
                throw new IllegalArgumentException("Invalid tactic definition");
            this.energyCost = cost; this.minRange = minRange; this.maxRange = maxRange;
            this.damageMultiplier = damageMultiplier;
        }
    }
    final class Context {
        public final BattleUnit actor, target;
        public final Battlefield battlefield;
        Context(BattleUnit actor, BattleUnit target, Battlefield battlefield) {
            this.actor = actor; this.target = target; this.battlefield = battlefield;
        }
    }
    /** Limited effect capability; the engine rechecks occupancy, death and terrain for all displacement. */
    interface Effects {
        void pushTarget(boolean follow);
        void pullTarget();
        void applyStatus(StatusEffect effect);
    }
    Definition definition();
    /** null means eligible. Must not mutate state or consume random numbers. */
    default String conditionError(Context context) { return null; }
    default void specialEffects(Context context, Effects effects, Random random) { }
}
