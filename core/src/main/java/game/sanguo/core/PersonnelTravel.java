package game.sanguo.core;

import game.sanguo.core.Strategy;
import game.sanguo.core.World;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.ToIntFunction;

public final class PersonnelTravel {
    private Map<Integer, SortedSet<Integer>> links;
    private int siteCount = -1;
    private final World w;

    PersonnelTravel(World w) {
        this.w = w;
    }

    private List<World.City> hubs() {
        List<World.City> list = new ArrayList<>();
        for (World.City c : this.w.cities) {
            if (c.kind == World.SiteKind.CITY) {
                list.add(c);
            }
        }
        return list.isEmpty() ? this.w.cities : list;
    }

    public World.City region(Hex h) {
        return hubs().stream().min(Comparator.comparingInt((World.City c)->c.hex.distance(h)).thenComparingInt(c->c.id)).orElse(null);
    }

    private void connect(int a, int b) {
        if (a != b) {
            this.links.get(Integer.valueOf(a)).add(Integer.valueOf(b));
            this.links.get(Integer.valueOf(b)).add(Integer.valueOf(a));
        }
    }

    private void build() {
        int distance;
        if (this.links != null && this.siteCount == this.w.cities.size()) {
            return;
        }
        this.siteCount = this.w.cities.size();
        this.links = new TreeMap();
        List<World.City> cities = hubs();
        Iterator<World.City> it = cities.iterator();
        while (it.hasNext()) {
            this.links.put(Integer.valueOf(it.next().id), new TreeSet());
        }
        Territory territory = new Territory(this.w);
        Map<Integer, Integer> parent = new HashMap<>();
        for (World.City c : this.w.cities) {
            parent.put(Integer.valueOf(c.id), Integer.valueOf(region(c.hex).id));
        }
        for (World.City c2 : this.w.cities) {
            Iterator<Integer> it2 = territory.neighbors(c2.id).iterator();
            while (it2.hasNext()) {
                int neighbor = it2.next().intValue();
                connect(parent.get(Integer.valueOf(c2.id)).intValue(), parent.get(Integer.valueOf(neighbor)).intValue());
            }
        }
        while (true) {
            Set<Integer> seen = new HashSet<>();
            if (cities.isEmpty()) {
                return;
            }
            visit(cities.get(0).id, seen);
            if (seen.size() != cities.size()) {
                World.City from = null;
                World.City to = null;
                int best = Integer.MAX_VALUE;
                for (World.City a : cities) {
                    if (seen.contains(Integer.valueOf(a.id))) {
                        for (World.City b : cities) {
                            if (!seen.contains(Integer.valueOf(b.id)) && (distance = a.hex.distance(b.hex)) < best) {
                                best = distance;
                                from = a;
                                to = b;
                            }
                        }
                    }
                }
                connect(from.id, to.id);
            } else {
                return;
            }
        }
    }

    private void visit(int id, Set<Integer> seen) {
        if (seen.add(Integer.valueOf(id))) {
            Iterator<Integer> it = this.links.get(Integer.valueOf(id)).iterator();
            while (it.hasNext()) {
                int next = it.next().intValue();
                visit(next, seen);
            }
        }
    }

    public List<Integer> route(Hex from, int destination) {
        int id;
        World.City target = this.w.city(destination);
        World.City start = region(from);
        if (target == null || start == null) {
            return Collections.emptyList();
        }
        build();
        int end = region(target.hex).id;
        Map<Integer, Integer> previous = new HashMap<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(Integer.valueOf(start.id));
        previous.put(Integer.valueOf(start.id), -1);
        while (!queue.isEmpty() && (id = queue.remove().intValue()) != end) {
            Iterator<Integer> it = this.links.get(Integer.valueOf(id)).iterator();
            while (it.hasNext()) {
                int next = it.next().intValue();
                if (!previous.containsKey(Integer.valueOf(next))) {
                    previous.put(Integer.valueOf(next), Integer.valueOf(id));
                    queue.add(Integer.valueOf(next));
                }
            }
        }
        if (!previous.containsKey(Integer.valueOf(end))) {
            return Collections.emptyList();
        }
        LinkedList<Integer> result = new LinkedList<>();
        for (int id2 = end; id2 >= 0; id2 = previous.get(Integer.valueOf(id2)).intValue()) {
            result.addFirst(Integer.valueOf(id2));
        }
        return result;
    }

    public int turns(int source, int target) {
        World.City a = this.w.city(source);
        if (a == null) {
            return 0;
        }
        return turns(a.hex, target);
    }

    public int turns(Hex from, int target) {
        World.City c = this.w.city(target);
        if (c == null) {
            return -1;
        }
        if (from.equals(c.hex)) {
            return 0;
        }
        List<Integer> route = route(from, target);
        if (route.isEmpty()) {
            return -1;
        }
        return Math.max(1, route.size() - 1);
    }

    public Hex next(Hex from, int target) {
        List<Integer> route = route(from, target);
        if (route.isEmpty()) {
            return from;
        }
        return (route.size() <= 2 ? this.w.city(target) : this.w.city(route.get(1).intValue())).hex;
    }

    public int destination(int side) {
        for (World.Officer o : this.w.officers) {
            if (o.owner == side && o.role == Strategy.Role.RULER && o.cityId >= 0 && this.w.city(o.cityId).owner == side) {
                return o.cityId;
            }
        }
        for (World.City c : this.w.cities) {
            if (c.owner == side && c.kind == World.SiteKind.CITY) {
                return c.id;
            }
        }
        for (World.City c2 : this.w.cities) {
            if (c2.owner == side) {
                return c2.id;
            }
        }
        return -1;
    }

    public String description(int source, int target) {
        List<Integer> path = route(this.w.city(source).hex, target);
        StringJoiner names = new StringJoiner(" → ");
        Iterator<Integer> it = path.iterator();
        while (it.hasNext()) {
            int id = it.next().intValue();
            names.add(this.w.city(id).name);
        }
        return String.valueOf(names) + " · 单程" + turns(source, target) + "旬（相邻城市1旬）";
    }
}
