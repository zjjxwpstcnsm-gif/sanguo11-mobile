package game.sanguo.core.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static game.sanguo.core.battle.BattleTestSupport.*;

final class GeometryPathTest {
    static void runAll() {
        run("hex six neighbors and inverse directions", () -> {
            HexPos center = h(4, -2);
            eq(6, new HashSet<>(center.neighbors()).size());
            for (HexPos.Direction direction : HexPos.Direction.values()) {
                HexPos next = center.neighbor(direction);
                eq(1, center.distance(next)); eq(center, next.neighbor(direction.opposite()));
                eq(direction, center.directionTo(next).get());
            }
            eq(center.neighbor(0), center.neighbor(HexPos.Direction.E));
        });
        run("distance is symmetric and obeys triangle inequality", () -> {
            for (HexPos a : h(0, 0).range(4)) for (HexPos b : h(1, -1).range(3)) {
                eq(a.distance(b), b.distance(a)); eq(0, a.distance(a));
                check(a.distance(b) <= a.distance(h(0, 0)) + h(0, 0).distance(b), "Triangle inequality");
            }
            eq(7, h(-4, 2).distance(h(3, -5)));
        });
        run("hex range exact cardinality without duplicates", () -> {
            for (int radius = 0; radius <= 8; radius++) {
                List<HexPos> range = h(-2, 5).range(radius);
                eq(1 + 3 * radius * (radius + 1), range.size()); eq(range.size(), new HashSet<>(range).size());
                for (HexPos point : range) check(point.distance(h(-2, 5)) <= radius, "Range outside radius");
            }
        });
        run("straight directions and nonaligned rays", () -> {
            eq(HexPos.Direction.NE, h(0, 0).directionTo(h(4, -4)).get());
            check(!h(0, 0).directionTo(h(1, 1)).isPresent(), "Nonaligned ray has no single direction");
            check(!h(1, 1).directionTo(h(1, 1)).isPresent(), "Center has no direction");
        });
        run("invalid geometry and immutable range", () -> {
            rejects(IllegalArgumentException.class, () -> h(Integer.MIN_VALUE, 0));
            rejects(IllegalArgumentException.class, () -> h(0, 0).range(-1));
            rejects(IllegalArgumentException.class, () -> h(0, 0).range(257));
            rejects(IllegalArgumentException.class, () -> h(0, 0).neighbor(6));
            rejects(UnsupportedOperationException.class, () -> h(0, 0).neighbors().clear());
        });
        run("nine terrain profiles and movement costs", () -> {
            eq(9, Terrain.values().length);
            eq(2, BattleRules.movementCost(Terrain.PLAIN, WeaponType.SPEAR));
            eq(2, BattleRules.movementCost(Terrain.GRASS, WeaponType.SPEAR));
            eq(1, BattleRules.movementCost(Terrain.ROAD, WeaponType.CAVALRY));
            eq(3, BattleRules.movementCost(Terrain.FOREST, WeaponType.SPEAR));
            eq(5, BattleRules.movementCost(Terrain.FOREST, WeaponType.CAVALRY));
            eq(4, BattleRules.movementCost(Terrain.MOUNTAIN, WeaponType.SPEAR));
            eq(-1, BattleRules.movementCost(Terrain.MOUNTAIN, WeaponType.CAVALRY));
            eq(-1, BattleRules.movementCost(Terrain.RIVER, WeaponType.SWORD));
            eq(3, BattleRules.movementCost(Terrain.SHALLOW, WeaponType.SWORD));
            eq(4, BattleRules.movementCost(Terrain.SHALLOW, WeaponType.CAVALRY));
            check(BattleRules.terrain(Terrain.CITY).defense > BattleRules.terrain(Terrain.PLAIN).defense, "City cover");
            check(BattleRules.terrain(Terrain.PASS).defense > BattleRules.terrain(Terrain.CITY).defense, "Pass cover");
        });
        run("weapons have distinct movement range and modifiers", () -> {
            check(BattleRules.weapon(WeaponType.CAVALRY).movement > BattleRules.weapon(WeaponType.HALBERD).movement, "Cavalry mobility");
            eq(2, BattleRules.weapon(WeaponType.CROSSBOW).minRange); eq(3, BattleRules.weapon(WeaponType.CROSSBOW).maxRange);
            check(BattleRules.weapon(WeaponType.SWORD).attack < BattleRules.weapon(WeaponType.SPEAR).attack, "Sword fallback penalty");
            check(BattleRules.weapon(WeaponType.HALBERD).defense > BattleRules.weapon(WeaponType.CAVALRY).defense, "Halberd defense");
            for (WeaponType reserved : Arrays.asList(WeaponType.RAM, WeaponType.SIEGE_TOWER, WeaponType.WOODEN_BEAST, WeaponType.CATAPULT))
                rejects(IllegalArgumentException.class, () -> u(0, 0, reserved, 0, 0));
        });
        run("cyclic weapon advantage and neutral pair", () -> {
            check(BattleRules.matchup(WeaponType.SPEAR, WeaponType.CAVALRY) > 1, "Spear counters cavalry");
            check(BattleRules.matchup(WeaponType.CAVALRY, WeaponType.HALBERD) > 1, "Cavalry counters halberd");
            check(BattleRules.matchup(WeaponType.HALBERD, WeaponType.SPEAR) > 1, "Halberd counters spear");
            check(BattleRules.matchup(WeaponType.CAVALRY, WeaponType.SPEAR) < 1, "Reverse disadvantage");
            eq(1.0, BattleRules.matchup(WeaponType.SWORD, WeaponType.CROSSBOW));
        });
        run("immutable local battlefield and input validation", () -> {
            Map<HexPos, Terrain> source = new LinkedHashMap<>(); source.put(h(0, 0), Terrain.PLAIN);
            Battlefield field = new Battlefield(source); source.clear(); eq(1, field.size());
            rejects(UnsupportedOperationException.class, () -> field.tiles().clear());
            rejects(IllegalArgumentException.class, () -> Battlefield.rectangle(0, 1, Terrain.PLAIN));
            rejects(IllegalArgumentException.class, () -> Battlefield.rectangle(1000, 1000, Terrain.PLAIN));
            rejects(IllegalArgumentException.class, () -> field.withTerrain(h(2, 2), Terrain.PLAIN));
            eq(-1, field.movementCost(h(-1, -1), WeaponType.SWORD));
            rejects(IllegalArgumentException.class, () -> new Battlefield(Collections.singletonMap(h(HexPos.LIMIT - 1, 0), Terrain.PLAIN)));
        });
        run("reachable area has budget costs and includes origin", () -> {
            Battlefield field = Battlefield.rectangle(9, 9, Terrain.PLAIN);
            Map<HexPos, Integer> reachable = Pathfinder.reachableTiles(field, WeaponType.SWORD, h(4, 4), Collections.emptySet(), 4);
            eq(19, reachable.size()); eq(0, reachable.get(h(4, 4))); eq(4, reachable.get(h(6, 4)));
            for (Map.Entry<HexPos, Integer> entry : reachable.entrySet()) eq(2 * entry.getKey().distance(h(4, 4)), entry.getValue());
            eq(1, Pathfinder.reachableTiles(field, WeaponType.SWORD, h(4, 4), Collections.emptySet(), 0).size());
            rejects(UnsupportedOperationException.class, reachable::clear);
        });
        run("cheapest path detours around expensive terrain", () -> {
            Battlefield field = Battlefield.rectangle(5, 3, Terrain.ROAD).withTerrain(h(1, 1), Terrain.FOREST)
                    .withTerrain(h(2, 1), Terrain.FOREST).withTerrain(h(3, 1), Terrain.FOREST);
            Pathfinder.Path path = Pathfinder.findPath(field, WeaponType.CAVALRY, h(0, 1), h(4, 1), Collections.emptySet(), 99).get();
            eq(5, path.totalCost); check(!path.tiles.contains(h(2, 1)), "Path should use cheap road detour");
            eq(h(0, 1), path.tiles.get(0)); eq(h(4, 1), path.tiles.get(path.tiles.size() - 1));
            rejects(UnsupportedOperationException.class, () -> path.tiles.clear());
        });
        run("river barrier is only crossed at shallow bridge", () -> {
            Battlefield field = Battlefield.rectangle(7, 5, Terrain.PLAIN);
            for (int r = 0; r < 5; r++) field = field.withTerrain(h(3, r), Terrain.RIVER);
            check(!Pathfinder.findPath(field, WeaponType.SPEAR, h(0, 2), h(6, 2), Collections.emptySet(), 100).isPresent(), "River barrier");
            field = field.withTerrain(h(3, 4), Terrain.SHALLOW);
            Pathfinder.Path path = Pathfinder.findPath(field, WeaponType.SPEAR, h(0, 2), h(6, 2), Collections.emptySet(), 100).get();
            check(path.tiles.contains(h(3, 4)), "Must use bridge");
        });
        run("unit occupancy blocks transit and destination", () -> {
            Battlefield field = Battlefield.rectangle(5, 1, Terrain.PLAIN);
            Set<HexPos> blocked = new HashSet<>(Arrays.asList(h(0, 0), h(2, 0)));
            check(!Pathfinder.findPath(field, WeaponType.SPEAR, h(0, 0), h(4, 0), blocked, 100).isPresent(), "Occupied choke point");
            check(!Pathfinder.findPath(field, WeaponType.SPEAR, h(0, 0), h(2, 0), blocked, 100).isPresent(), "Occupied goal");
            eq(2, Pathfinder.reachableTiles(field, WeaponType.SPEAR, h(0, 0), blocked, 100).size());
            eq(0, Pathfinder.findPath(field, WeaponType.SPEAR, h(0, 0), h(0, 0), blocked, 0).get().totalCost);
        });
        run("pathfinder budget and impassable endpoints", () -> {
            Battlefield field = Battlefield.rectangle(5, 2, Terrain.PLAIN).withTerrain(h(3, 1), Terrain.RIVER);
            check(!Pathfinder.findPath(field, WeaponType.SWORD, h(0, 0), h(4, 0), Collections.emptySet(), 7).isPresent(), "Budget respected");
            eq(8, Pathfinder.findPath(field, WeaponType.SWORD, h(0, 0), h(4, 0), Collections.emptySet(), 8).get().totalCost);
            check(!Pathfinder.findPath(field, WeaponType.SWORD, h(3, 1), h(0, 0), Collections.emptySet(), 100).isPresent(), "Impassable start");
            check(!Pathfinder.findPath(field, WeaponType.SWORD, h(0, 0), h(-1, 0), Collections.emptySet(), 100).isPresent(), "Outside goal");
            rejects(IllegalArgumentException.class, () -> Pathfinder.reachableTiles(field, WeaponType.SWORD, h(0, 0), Collections.emptySet(), -1));
        });
        run("weapon mobility changes actual reachable tiles", () -> {
            Battlefield field = Battlefield.rectangle(20, 20, Terrain.PLAIN);
            Map<HexPos, Integer> horse = Pathfinder.reachableTiles(field, WeaponType.CAVALRY, h(10, 10), Collections.emptySet(), BattleRules.weapon(WeaponType.CAVALRY).movement);
            Map<HexPos, Integer> halberd = Pathfinder.reachableTiles(field, WeaponType.HALBERD, h(10, 10), Collections.emptySet(), BattleRules.weapon(WeaponType.HALBERD).movement);
            check(horse.size() > halberd.size(), "Different reachable area");
            check(horse.containsKey(h(14, 10)) && !halberd.containsKey(h(14, 10)), "Horse reaches four flat cells");
        });
        run("multi-goal path finds cheapest open attack slot", () -> {
            Battlefield field = Battlefield.rectangle(8, 4, Terrain.PLAIN);
            Set<HexPos> blocked = Collections.singleton(h(3, 0));
            Pathfinder.Path path = Pathfinder.findPathToAny(field, WeaponType.SWORD, h(0, 0), Arrays.asList(h(3, 0), h(4, 0), h(1, 1)), blocked, 100).get();
            eq(h(1, 1), path.tiles.get(path.tiles.size() - 1)); eq(4, path.totalCost);
        });
        run("A star matches Dijkstra on seeded weighted maps", () -> {
            for (int seed = 0; seed < 30; seed++) {
                Random random = new Random(seed); Map<HexPos, Terrain> tiles = new LinkedHashMap<>();
                Terrain[] choices = {Terrain.PLAIN, Terrain.ROAD, Terrain.FOREST, Terrain.MOUNTAIN, Terrain.RIVER};
                for (int q = 0; q < 8; q++) for (int r = 0; r < 8; r++) tiles.put(h(q, r), choices[random.nextInt(choices.length)]);
                tiles.put(h(0, 0), Terrain.PLAIN); tiles.put(h(7, 7), Terrain.PLAIN);
                Battlefield field = new Battlefield(tiles);
                Set<HexPos> occupied = Collections.singleton(h(4, 4));
                Map<HexPos, Integer> costs = Pathfinder.reachableTiles(field, WeaponType.SPEAR, h(0, 0), occupied, 200);
                for (HexPos goal : field.tiles().keySet()) {
                    Optional<Pathfinder.Path> path = Pathfinder.findPath(field, WeaponType.SPEAR, h(0, 0), goal, occupied, 200);
                    eq(costs.containsKey(goal), path.isPresent());
                    if (path.isPresent()) {
                        eq(costs.get(goal), path.get().totalCost); int total = 0;
                        List<HexPos> points = path.get().tiles;
                        for (int i = 1; i < points.size(); i++) {
                            eq(1, points.get(i - 1).distance(points.get(i))); check(!occupied.contains(points.get(i)), "Occupied route cell");
                            total += field.movementCost(points.get(i), WeaponType.SPEAR);
                        }
                        eq(total, path.get().totalCost); check(path.get().expandedNodes <= field.size(), "Expanded outside finite local field");
                    }
                }
            }
        });
    }
}
