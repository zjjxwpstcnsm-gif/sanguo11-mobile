package game.sanguo.core.battle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/** A* for one/many destinations; bounded Dijkstra for reachable tiles. No strategic map scans. */
public final class Pathfinder {
    private Pathfinder() { }
    public static final class Path {
        public final List<HexPos> tiles;
        public final int totalCost, expandedNodes;
        private Path(List<HexPos> tiles, int totalCost, int expandedNodes) {
            this.tiles = Collections.unmodifiableList(new ArrayList<>(tiles));
            this.totalCost = totalCost; this.expandedNodes = expandedNodes;
        }
    }
    private static final class Node {
        final HexPos hex;
        final int cost;
        final long score;
        Node(HexPos hex, int cost, int heuristic) {
            this.hex = hex; this.cost = cost; this.score = (long) cost + heuristic;
        }
    }
    private static PriorityQueue<Node> queue() {
        return new PriorityQueue<>(Comparator.comparingLong((Node node) -> node.score)
                .thenComparingInt(node -> node.cost).thenComparing(node -> node.hex));
    }
    private static void validate(Battlefield field, WeaponType weapon, HexPos start,
                                 Set<HexPos> occupied, int budget) {
        Objects.requireNonNull(field); Objects.requireNonNull(start); Objects.requireNonNull(occupied);
        BattleRules.weapon(weapon);
        if (budget < 0) throw new IllegalArgumentException("Negative movement budget");
    }
    public static Optional<Path> findPath(Battlefield field, WeaponType weapon, HexPos start,
                                         HexPos goal, Set<HexPos> occupied, int maxCost) {
        Objects.requireNonNull(goal);
        return findPathToAny(field, weapon, start, Collections.singleton(goal), occupied, maxCost);
    }
    /** Multi-goal A*: lets AI seek a legal firing/melee position rather than an occupied enemy hex. */
    public static Optional<Path> findPathToAny(Battlefield field, WeaponType weapon, HexPos start,
                                              Collection<HexPos> destinations, Set<HexPos> occupied,
                                              int maxCost) {
        validate(field, weapon, start, occupied, maxCost);
        if (!field.passable(start, weapon)) return Optional.empty();
        Set<HexPos> goals = new HashSet<>();
        for (HexPos goal : destinations)
            if (field.passable(goal, weapon) && (goal.equals(start) || !occupied.contains(goal))) goals.add(goal);
        if (goals.isEmpty()) return Optional.empty();
        Map<HexPos, Integer> costs = new HashMap<>();
        Map<HexPos, HexPos> previous = new HashMap<>();
        PriorityQueue<Node> open = queue();
        costs.put(start, 0); open.add(new Node(start, 0, heuristic(start, goals)));
        int expanded = 0;
        while (!open.isEmpty()) {
            Node node = open.remove();
            if (node.cost != costs.get(node.hex)) continue;
            expanded++;
            if (goals.contains(node.hex)) {
                List<HexPos> path = new ArrayList<>();
                for (HexPos at = node.hex; at != null; at = previous.get(at)) path.add(at);
                Collections.reverse(path);
                return Optional.of(new Path(path, node.cost, expanded));
            }
            for (HexPos next : node.hex.neighbors()) {
                int step = field.movementCost(next, weapon);
                if (step <= 0 || occupied.contains(next)) continue;
                long total = (long) node.cost + step;
                if (total > maxCost || total >= costs.getOrDefault(next, Integer.MAX_VALUE)) continue;
                costs.put(next, (int) total); previous.put(next, node.hex);
                open.add(new Node(next, (int) total, heuristic(next, goals)));
            }
        }
        return Optional.empty();
    }
    private static int heuristic(HexPos from, Set<HexPos> goals) {
        int distance = Integer.MAX_VALUE;
        for (HexPos goal : goals) distance = Math.min(distance, from.distance(goal));
        return distance; // minimum traversable cost is 1, so this is admissible and consistent
    }
    /** Includes the start at cost 0. All other occupied cells, allied or enemy, are blocked. */
    public static Map<HexPos, Integer> reachableTiles(Battlefield field, WeaponType weapon, HexPos start,
                                                     Set<HexPos> occupied, int maxCost) {
        validate(field, weapon, start, occupied, maxCost);
        Map<HexPos, Integer> costs = new LinkedHashMap<>();
        if (!field.passable(start, weapon)) return Collections.unmodifiableMap(costs);
        PriorityQueue<Node> open = queue();
        costs.put(start, 0); open.add(new Node(start, 0, 0));
        while (!open.isEmpty()) {
            Node node = open.remove();
            if (node.cost != costs.get(node.hex)) continue;
            for (HexPos next : node.hex.neighbors()) {
                int step = field.movementCost(next, weapon);
                if (step <= 0 || occupied.contains(next)) continue;
                long total = (long) node.cost + step;
                if (total > maxCost || total >= costs.getOrDefault(next, Integer.MAX_VALUE)) continue;
                costs.put(next, (int) total); open.add(new Node(next, (int) total, 0));
            }
        }
        return Collections.unmodifiableMap(costs);
    }
}
