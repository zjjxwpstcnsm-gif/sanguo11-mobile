package game.sanguo.core;

/** Opening-only materialization. Save loading restores entities and never calls this:
 * destroyed dams must not respawn. Terrain owns the ground; Structure owns the one
 * destructible dam body, durability, blocking and flood behavior. */
public final class NaturalStructures {
    private NaturalStructures() {}
    public static int seedOpening(World world) {
        int added = 0;
        for (int x=0;x<world.sourceColumns();x++) for (int y=0;y<world.sourceRows();y++) {
            Hex h=MapCoordinates.axial(world,new SourceGridCoord(x,y));
            if (!world.sourceInside(h) || world.terrain[h.q][h.r]!=World.Terrain.DAM) continue;
            War.Structure existing=world.war.at(h);
            if (existing!=null) {
                if (existing.kind!=War.StructureKind.DAM || existing.owner!=-1)
                    throw new IllegalArgumentException("自然堤坝与设施冲突："+h);
                continue;
            }
            if (world.cityAt(h)!=null || world.domestic.at(h)!=null || world.unitAt(h)!=null)
                throw new IllegalArgumentException("自然堤坝地块已占用："+h);
            world.war.structures.add(new War.Structure(world.war.nextStructureId++,-1,
                    War.StructureKind.DAM,h,War.StructureKind.DAM.hp));
            added++;
        }
        return added;
    }
    public static String ownerLabel(World world, int owner) {
        return owner==-1?"中立设施":world.faction(owner);
    }
}
