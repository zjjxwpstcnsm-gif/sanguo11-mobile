package game.sanguo.runtime.query;

import game.sanguo.api.*;
import game.sanguo.api.bridge.BridgeEntity;
import game.sanguo.core.*;
import java.util.*;

/** Projects a detached, consistently captured rule world into immutable game facts. */
public final class SnapshotQuery {
    private SnapshotQuery(){}
    public static GameSnapshot capture(World world,StateToken state){
        List<BridgeEntity> entities=new ArrayList<>();
        for(World.City c:world.cities){List<World.Officer> idle=world.idle(c);
            entities.add(new BridgeEntity("site:"+c.id,c.kind.name(),c.name,c.hex.q,c.hex.r,c.owner,c.troops,c.morale,
                idle.isEmpty()?-1:idle.get(0).id,c.gold,c.food,c.order));
        }
        for(World.Unit u:world.fieldUnits())entities.add(new BridgeEntity("unit:"+u.id,"UNIT",
            world.officer(u.officerId)==null?"部队":world.officer(u.officerId).name,u.hex.q,u.hex.r,u.owner,u.troops,u.energy,u.officerId,u.gold,u.food,0));
        StringBuilder terrain=new StringBuilder(world.width*world.height);
        for(int r=0;r<world.height;r++)for(int q=0;q<world.width;q++)terrain.append(TerrainWireCode.encode(world.terrain[q][r]));
        return new GameSnapshot(state,world.width,world.height,world.mapRevision,world.terrainRevision,world.turn,world.player,
            terrain.toString(),new GridLayout(world.columnStaggered,world.sourceMapWidth>0?(world.height-1)/2:0,
                world.sourceOriginX,world.sourceOriginY),entities);
    }
}
