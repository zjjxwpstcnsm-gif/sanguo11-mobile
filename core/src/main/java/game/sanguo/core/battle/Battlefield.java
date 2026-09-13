package game.sanguo.core.battle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable finite local map. Missing hexes are impassable, so sparse maps are supported. */
public final class Battlefield {
    private final Map<HexPos, Terrain> tiles;
    public Battlefield(Map<HexPos, Terrain> tiles) {
        Objects.requireNonNull(tiles, "tiles");
        if (tiles.isEmpty() || tiles.size() > BattleRules.MAX_BATTLEFIELD_TILES)
            throw new IllegalArgumentException("Invalid battlefield size");
        TreeMap<HexPos, Terrain> sorted = new TreeMap<>();
        for (Map.Entry<HexPos, Terrain> entry : tiles.entrySet()) {
            HexPos hex = Objects.requireNonNull(entry.getKey(), "hex");
            // Leave room for every supported range query, even near the coordinate limits.
            if (Math.abs(hex.q) > HexPos.LIMIT - 256 || Math.abs(hex.r) > HexPos.LIMIT - 256)
                throw new IllegalArgumentException("Battlefield reaches coordinate boundary");
            sorted.put(hex, Objects.requireNonNull(entry.getValue(), "terrain"));
        }
        this.tiles = Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }
    public static Battlefield rectangle(int width, int height, Terrain terrain) {
        if (width <= 0 || height <= 0 || (long) width * height > BattleRules.MAX_BATTLEFIELD_TILES)
            throw new IllegalArgumentException("Invalid battlefield dimensions");
        Map<HexPos, Terrain> tiles = new LinkedHashMap<>();
        for (int q = 0; q < width; q++) for (int r = 0; r < height; r++)
            tiles.put(new HexPos(q, r), terrain);
        return new Battlefield(tiles);
    }
    public Battlefield withTerrain(HexPos hex, Terrain terrain) {
        if (!contains(hex)) throw new IllegalArgumentException("Hex outside battlefield");
        Map<HexPos, Terrain> copy = new LinkedHashMap<>(tiles);
        copy.put(hex, terrain);
        return new Battlefield(copy);
    }
    public boolean contains(HexPos hex) { return tiles.containsKey(hex); }
    public Terrain terrainAt(HexPos hex) { return tiles.get(hex); }
    public int size() { return tiles.size(); }
    public Map<HexPos, Terrain> tiles() { return tiles; }
    public int movementCost(HexPos hex, WeaponType weapon) {
        Terrain terrain = terrainAt(hex);
        return terrain == null ? -1 : BattleRules.movementCost(terrain, weapon);
    }
    public boolean passable(HexPos hex, WeaponType weapon) { return movementCost(hex, weapon) > 0; }
}
