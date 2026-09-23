package game.sanguo.mobile;
import game.sanguo.core.map.SourceGridCoord;

import game.sanguo.core.World;

/** One reviewed ground/connection plan, shared by close view and overview.
 * No terrain code is ever an index into the military atlas. */
final class TerrainArt {
    enum Connection { NONE, ROAD, MOUNTAIN_PATH, PLANK }
    private TerrainArt() {}
    static int sandVariant(World world,int q,int r) {
        game.sanguo.core.map.SourceGridCoord source=game.sanguo.core.MapCoordinates.nationalSource(world,new game.sanguo.core.Hex(q,r));
        return Math.floorMod(source.x*31+source.y*17,4);
    }
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
