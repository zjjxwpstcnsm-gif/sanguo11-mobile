package game.sanguo.core;

import game.sanguo.core.Campaign;
import game.sanguo.core.Domestic;
import game.sanguo.core.Government;
import game.sanguo.core.Strategy;
import game.sanguo.core.War;
import game.sanguo.core.World;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

final class FactionCollapse {
    static void resolve(World w, final int owner) {
        Map<Integer, Hex> locations = new HashMap<>();
        boolean remains = false;
        Iterator<World.Officer> it = w.officers.iterator();
        while (true) {
            Hex at = null;
            if (!it.hasNext()) {
                break;
            }
            World.Officer o = it.next();
            if (o.owner == owner) {
                World.Unit unit = w.unit(o.unitId);
                World.City city = w.city(o.cityId);
                if (unit != null) {
                    at = unit.hex;
                } else if (city != null) {
                    at = city.hex;
                }
                Government.Prisoner prisoner = w.government.prisoner(o.id);
                if (prisoner != null) {
                    at = w.government.location(prisoner);
                }
                if (at != null) {
                    locations.put(Integer.valueOf(o.id), at);
                }
                remains = true;
            }
        }
        for (World.City c : w.cities) {
            if (c.owner == owner) {
                remains = true;
                c.owner = -1;
                c.governorId = -1;
                c.troops = 0;
                c.morale = 0;
                c.gold = 0;
                c.food = 0;
                Arrays.fill(c.equipment, 0);
                Arrays.fill(c.ships, 0);
                c.defense = c.baseDefense;
                w.domestic.captured(c.id);
                w.government.policies.remove(Integer.valueOf(c.id));
            }
        }
        Iterator it2 = new ArrayList(w.government.prisoners()).iterator();
        while (it2.hasNext()) {
            Government.Prisoner p = (Government.Prisoner) it2.next();
            if (p.captor == owner) {
                w.government.free(p);
            }
        }
        Iterator it3 = new ArrayList(w.units).iterator();
        while (it3.hasNext()) {
            World.Unit u = (World.Unit) it3.next();
            if (u.owner == owner) {
                remains = true;
                w.units.remove(u);
            }
        }
        Iterator it4 = new ArrayList(w.domestic.missions).iterator();
        while (it4.hasNext()) {
            Domestic.Mission m = (Domestic.Mission) it4.next();
            if (m.owner == owner) {
                remains = true;
                for (int id : m.crew()) {
                    locations.put(Integer.valueOf(id), m.hex);
                }
                w.domestic.missions.remove(m);
            }
        }
        w.recruitment.cancelOwner(owner);
        w.envoys.cancelOwner(owner);
        for (World.Officer o2 : w.officers) {
            if (o2.owner == owner) {
                final Hex from = locations.getOrDefault(Integer.valueOf(o2.id), new Hex(0, 0));
                World.City near=w.cities.stream().filter(c->c.kind==World.SiteKind.CITY)
                    .min(Comparator.comparingInt((World.City c)->c.hex.distance(from)).thenComparingInt(c->c.id)).orElse(null);
                w.strategy.releaseGovernor(o2.id);
                w.government.allegianceChanged(o2.id);
                o2.owner = -1;
                o2.unitId = -1;
                o2.role = Strategy.Role.UNAFFILIATED;
                o2.loyalty = 0;
                o2.otherTask = "";
                o2.otherTaskTurns = 0;
                o2.acted = true;
                o2.cityId = (w.government.captive(o2.id) || !w.life.present(o2.id) || near == null) ? -1 : near.id;
            }
        }
        w.war.structures.removeIf(s2->s2.owner==owner);
        w.campaign.treaties.removeIf(t->t.a==owner||t.b==owner);
        w.actionPoints[owner] = 0;
        if (w.life.pendingOwner == owner) {
            w.life.pendingOwner = -1;
            w.life.pendingRuler = -1;
        }
        if (remains) {
            w.campaign.cleanupProjects();
            w.army.cleanup();
            w.abilities.cleanup();
            w.fieldworks.cleanup();
            w.districts.cleanup();
            w.aiOrders.cleanup();
            w.diplomacy.cleanup();
            w.government.relocatePrisoners();
            w.note(w.faction(owner) + "失去最后一座城池，势力灭亡；港口、关卡归为无主，残余部队解散");
        }
    }

    private FactionCollapse() {}
}
