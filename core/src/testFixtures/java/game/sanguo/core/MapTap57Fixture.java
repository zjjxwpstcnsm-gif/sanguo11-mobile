package game.sanguo.core;

import java.util.*;

/** Explicitly synthetic dangerous-object fixtures on a real loaded scenario. Not shipped data. */
public final class MapTap57Fixture {
    private MapTap57Fixture() {}
    public static Hex[] addDangerousObjects(World w) {
        List<Hex> spots=new ArrayList<>();
        for(int y=74;y<88&&spots.size()<4;y++)for(int x=69;x<88&&spots.size()<4;x++) {
            Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(x,y));
            if(!w.inside(h)||w.cityAt(h)!=null||w.unitAt(h)!=null||w.domestic.at(h)!=null||w.war.at(h)!=null||w.development.cityAt(h)!=null)continue;
            World.Terrain t=w.terrain[h.q][h.r];if(t!=World.Terrain.PLAIN&&t!=World.Terrain.ROAD)continue;
            spots.add(h);
        }
        if(spots.size()!=4)throw new AssertionError("fixture needs four unused land cells");
        Hex dam=spots.get(0);w.terrain[dam.q][dam.r]=World.Terrain.DAM;
        NaturalStructures.seedOpening(w);
        Hex bare=spots.get(1);w.terrain[bare.q][bare.r]=World.Terrain.DAM;
        for(int i=2;i<4;i++) {
            Hex h=spots.get(i);War.StructureKind kind=i==2?War.StructureKind.EARTH_WALL:War.StructureKind.STONE_WALL;
            w.war.structures.add(new War.Structure(w.war.nextStructureId++,w.player,kind,h,kind.hp));
        }
        w.terrainRevision++;
        return spots.toArray(new Hex[0]);
    }
    public static void destroyDam(World w,Hex h){w.fieldworks.destroy(w.war.at(h));}
}
