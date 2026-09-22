package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Selection-only immutable model: no per-frame national-map/unit scans or rule mutation. */
final class SiegeOverlay {
    final World.City site;
    final List<Hex> cells;
    final Set<Hex> enemies;
    private SiegeOverlay(World.City site,List<Hex> cells,Set<Hex> enemies){this.site=site;this.cells=cells;this.enemies=enemies;}
    static SiegeOverlay selected(World world,Hex selected){
        World.City site=world.cityAt(selected);
        if(site==null)return new SiegeOverlay(null,Collections.emptyList(),Collections.emptySet());
        Set<Hex> enemies=new HashSet<>();
        for(World.Unit u:world.fieldUnits())if(SiegeRules.hostile(world,site,u))enemies.add(u.hex);
        return new SiegeOverlay(site,SiegeRules.cells(world,site),Collections.unmodifiableSet(enemies));
    }
}
