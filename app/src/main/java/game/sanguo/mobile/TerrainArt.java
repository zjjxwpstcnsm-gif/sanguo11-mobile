package game.sanguo.mobile;

import game.sanguo.core.World;

/** One reviewed ground/connection plan, shared by close view and overview.
 * No terrain code is ever an index into the military atlas. */
final class TerrainArt {
    enum Connection { NONE, ROAD, MOUNTAIN_PATH, PLANK }
    private TerrainArt() {}
    static Connection connection(World.Terrain terrain) {
        return switch (terrain) {
            // ROAD is a rule identity only; its ground is indistinguishable from PLAIN.
            case ROAD -> Connection.NONE;
            case MOUNTAIN_PATH -> Connection.MOUNTAIN_PATH;
            case PLANK_ROAD -> Connection.PLANK;
            default -> Connection.NONE;
        };
    }
    static World.Terrain ground(World.Terrain terrain) {
        return switch (terrain) {
            case NON_NAVIGABLE_WATER -> World.Terrain.WATER;
            case ROAD -> World.Terrain.PLAIN;
            case MOUNTAIN_PATH, PLANK_ROAD -> World.Terrain.MOUNTAIN;
            // The bank is ground; the one War.Structure supplies the physical dam.
            case DAM -> World.Terrain.SHALLOWS;
            default -> terrain;
        };
    }
}
