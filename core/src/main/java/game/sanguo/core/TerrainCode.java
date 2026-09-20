package game.sanguo.core;

/** Stable source-file alphabet. In particular D is a mountain path, H is a dam.
 * This is independent of the ordinal ABI used by SaveCodec31. */
public final class TerrainCode {
    private TerrainCode() {}
    public static World.Terrain decode(char code) {
        return switch (code) {
            case 'P' -> World.Terrain.PLAIN; case 'F' -> World.Terrain.FOREST;
            case 'M' -> World.Terrain.MOUNTAIN; case 'W' -> World.Terrain.WATER;
            case 'D' -> World.Terrain.MOUNTAIN_PATH; case 'S' -> World.Terrain.SHALLOWS;
            case 'B' -> World.Terrain.PLANK_ROAD; case 'X' -> World.Terrain.POISON;
            case 'O' -> World.Terrain.SEA; case 'V' -> World.Terrain.VOID;
            case 'Z' -> World.Terrain.SWAMP; case 'H' -> World.Terrain.DAM;
            case 'A' -> World.Terrain.SAND; case 'R' -> World.Terrain.ROAD;
            default -> throw new IllegalArgumentException("未知地形：" + code);
        };
    }
    public static char encode(World.Terrain terrain) {
        return switch (terrain) {
            case PLAIN -> 'P'; case FOREST -> 'F'; case MOUNTAIN -> 'M'; case WATER -> 'W';
            case MOUNTAIN_PATH -> 'D'; case SHALLOWS -> 'S'; case PLANK_ROAD -> 'B';
            case POISON -> 'X'; case SEA -> 'O'; case VOID -> 'V'; case SWAMP -> 'Z';
            case DAM -> 'H'; case SAND -> 'A'; case ROAD -> 'R';
        };
    }
}
