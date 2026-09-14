package game.sanguo.core.battle;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** All v0.4 balance numbers. Original engineering rules, not claimed SAN11 formulas. */
public final class BattleRules {
    private BattleRules() { }
    public static final int MAX_TROOPS = 100000, MAX_ENERGY = 100;
    public static final int NORMAL_ATTACK_ENERGY = 5, TURN_ENERGY_RECOVERY = 6;
    public static final int MAX_ROUNDS = 200, MAX_BATTLEFIELD_TILES = 65536;
    public static final int MIN_DAMAGE = 20, MAX_DAMAGE = 2500;
    public static final double BASE_DAMAGE = 230, TROOP_SCALE = 1000;
    public static final double VARIANCE = 0.10, AI_TACTIC_CHANCE = 0.45;
    public static final double CONFUSION_BASE_CHANCE = 0.25, CONFUSION_INT_SCALE = 0.002;
    public static final double SIEGE_DAMAGE_SCALE = 0.65;

    public static final class WeaponProfile {
        public final int movement, minRange, maxRange;
        public final double attack, defense;
        private WeaponProfile(int movement, int minRange, int maxRange, double attack, double defense) {
            this.movement = movement; this.minRange = minRange; this.maxRange = maxRange;
            this.attack = attack; this.defense = defense;
        }
    }
    public static final class TerrainProfile {
        public final int footCost, cavalryCost;
        public final double attack, defense;
        private TerrainProfile(int footCost, int cavalryCost, double attack, double defense) {
            this.footCost = footCost; this.cavalryCost = cavalryCost;
            this.attack = attack; this.defense = defense;
        }
    }
    private static final Map<WeaponType, WeaponProfile> WEAPONS;
    private static final Map<Terrain, TerrainProfile> TERRAINS;
    private static final Map<WeaponType, WeaponType> ADVANTAGE;
    static {
        EnumMap<WeaponType, WeaponProfile> weapons = new EnumMap<>(WeaponType.class);
        weapons.put(WeaponType.SWORD, new WeaponProfile(6, 1, 1, 0.88, 0.95));
        weapons.put(WeaponType.SPEAR, new WeaponProfile(6, 1, 1, 1.05, 1.00));
        weapons.put(WeaponType.HALBERD, new WeaponProfile(5, 1, 1, 0.98, 1.20));
        weapons.put(WeaponType.CROSSBOW, new WeaponProfile(5, 2, 3, 0.95, 0.85));
        weapons.put(WeaponType.CAVALRY, new WeaponProfile(9, 1, 1, 1.15, 0.95));
        weapons.put(WeaponType.RAM, new WeaponProfile(3, 1, 1, 0.35, 0.70));
        weapons.put(WeaponType.SIEGE_TOWER, new WeaponProfile(3, 1, 2, 1.00, 0.70));
        weapons.put(WeaponType.WOODEN_BEAST, new WeaponProfile(3, 1, 1, 0.90, 0.75));
        weapons.put(WeaponType.CATAPULT, new WeaponProfile(3, 1, 3, 0.90, 0.70));
        WEAPONS = Collections.unmodifiableMap(weapons);
        EnumMap<Terrain, TerrainProfile> terrain = new EnumMap<>(Terrain.class);
        terrain.put(Terrain.PLAIN, new TerrainProfile(2, 2, 1.00, 1.00));
        terrain.put(Terrain.GRASS, new TerrainProfile(2, 2, 1.00, 1.05));
        terrain.put(Terrain.FOREST, new TerrainProfile(3, 5, 0.95, 1.20));
        terrain.put(Terrain.MOUNTAIN, new TerrainProfile(4, -1, 0.90, 1.25));
        terrain.put(Terrain.ROAD, new TerrainProfile(1, 1, 1.00, 0.95));
        terrain.put(Terrain.RIVER, new TerrainProfile(-1, -1, 0.80, 0.80));
        terrain.put(Terrain.SHALLOW, new TerrainProfile(3, 4, 0.85, 0.80));
        terrain.put(Terrain.CITY, new TerrainProfile(2, 2, 1.00, 1.35));
        terrain.put(Terrain.PASS, new TerrainProfile(2, 2, 1.00, 1.40));
        TERRAINS = Collections.unmodifiableMap(terrain);
        EnumMap<WeaponType, WeaponType> advantage = new EnumMap<>(WeaponType.class);
        advantage.put(WeaponType.SPEAR, WeaponType.CAVALRY);
        advantage.put(WeaponType.CAVALRY, WeaponType.HALBERD);
        advantage.put(WeaponType.HALBERD, WeaponType.SPEAR);
        ADVANTAGE = Collections.unmodifiableMap(advantage);
    }
    public static WeaponProfile weapon(WeaponType type) {
        WeaponProfile profile = WEAPONS.get(type);
        if (profile == null) throw new IllegalArgumentException("Weapon is reserved or missing: " + type);
        return profile;
    }
    public static TerrainProfile terrain(Terrain type) {
        TerrainProfile profile = TERRAINS.get(type);
        if (profile == null) throw new IllegalArgumentException("Missing terrain");
        return profile;
    }
    public static int movementCost(Terrain terrain, WeaponType weapon) {
        weapon(weapon);
        TerrainProfile profile = terrain(terrain);
        return weapon == WeaponType.CAVALRY ? profile.cavalryCost : profile.footCost;
    }
    public static double matchup(WeaponType attacker, WeaponType defender) {
        if (ADVANTAGE.get(attacker) == defender) return 1.25;
        if (ADVANTAGE.get(defender) == attacker) return 0.90;
        return 1.00;
    }
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
