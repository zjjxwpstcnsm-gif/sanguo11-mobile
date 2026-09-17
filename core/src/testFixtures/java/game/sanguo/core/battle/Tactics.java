package game.sanguo.core.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Registry with one strategy per tactic; there is deliberately no tactic-id switch in the engine. */
public final class Tactics {
    private final Map<String, Tactic> entries;
    public Tactics(Collection<? extends Tactic> tactics) {
        Map<String, Tactic> map = new LinkedHashMap<>();
        for (Tactic tactic : tactics) if (map.put(tactic.definition().id, tactic) != null)
            throw new IllegalArgumentException("Duplicate tactic id");
        entries = Collections.unmodifiableMap(map);
    }
    public static Tactics standard() {
        return new Tactics(Arrays.asList(new SpearThrust(), new HalberdHook(), new CrossbowVolley(), new CavalryCharge()));
    }
    public Tactic get(String id) { return entries.get(id); }
    public List<Tactic> all() { return Collections.unmodifiableList(new ArrayList<>(entries.values())); }

    public static final class SpearThrust implements Tactic {
        private static final Definition DEFINITION = new Definition("spear-thrust", "突刺", WeaponType.SPEAR, 15, 1, 1, 1.20);
        @Override public Definition definition() { return DEFINITION; }
        @Override public void specialEffects(Context context, Effects effects, Random random) {
            effects.pushTarget(false);
            double chance = BattleRules.clamp(BattleRules.CONFUSION_BASE_CHANCE
                    + (context.actor.commander.intelligence - context.target.commander.intelligence)
                    * BattleRules.CONFUSION_INT_SCALE, 0.10, 0.45);
            if (random.nextDouble() < chance) effects.applyStatus(new StatusEffect(StatusEffect.Kind.CONFUSED, 1));
        }
    }
    public static final class HalberdHook implements Tactic {
        private static final Definition DEFINITION = new Definition("halberd-hook", "熊手", WeaponType.HALBERD, 15, 2, 2, 1.10);
        @Override public Definition definition() { return DEFINITION; }
        @Override public String conditionError(Context context) {
            return context.actor.position.directionTo(context.target.position).isPresent() ? null : "Hook requires a straight hex ray";
        }
        @Override public void specialEffects(Context context, Effects effects, Random random) { effects.pullTarget(); }
    }
    public static final class CrossbowVolley implements Tactic {
        private static final Definition DEFINITION = new Definition("crossbow-volley", "齐射", WeaponType.CROSSBOW, 20, 2, 3, 1.45);
        @Override public Definition definition() { return DEFINITION; }
    }
    public static final class CavalryCharge implements Tactic {
        private static final Definition DEFINITION = new Definition("cavalry-charge", "突击", WeaponType.CAVALRY, 20, 1, 1, 1.35);
        @Override public Definition definition() { return DEFINITION; }
        @Override public String conditionError(Context context) {
            Terrain terrain = context.battlefield.terrainAt(context.actor.position);
            return terrain == Terrain.FOREST || terrain == Terrain.SHALLOW ? "Charge requires firm open footing" : null;
        }
        @Override public void specialEffects(Context context, Effects effects, Random random) { effects.pushTarget(true); }
    }
}
