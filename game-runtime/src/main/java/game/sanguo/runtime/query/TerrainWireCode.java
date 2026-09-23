package game.sanguo.runtime.query;

import game.sanguo.core.World;

/** Frozen schema-1 codes, byte-compatible with the original ordinal encoding.
 * Adding/reordering an enum must never silently change an existing wire code. */
public final class TerrainWireCode {
    private TerrainWireCode(){}
    public static char encode(World.Terrain terrain){
        switch(terrain){
            case PLAIN:return 'A';case FOREST:return 'B';case MOUNTAIN:return 'C';case WATER:return 'D';
            case MOUNTAIN_PATH:return 'E';case SHALLOWS:return 'F';case PLANK_ROAD:return 'G';case POISON:return 'H';
            case SEA:return 'I';case VOID:return 'J';case SWAMP:return 'K';case DAM:return 'L';case SAND:return 'M';
            case ROAD:return 'N';case NON_NAVIGABLE_WATER:return 'O';default:throw new IllegalArgumentException("Unmapped terrain");
        }
    }
}
