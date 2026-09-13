package game.sanguo.core.battle;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** JVM smoke benchmark, not an Android device benchmark or a hard real-time guarantee. */
public final class BattlePerformanceTest {
    public static void main(String[] args) {
        Map<HexPos, Terrain> tiles = new LinkedHashMap<>(); Random random = new Random(50);
        for (int q = 0; q < 50; q++) for (int r = 0; r < 50; r++) {
            Terrain terrain = random.nextInt(4) == 0 ? Terrain.FOREST : Terrain.PLAIN;
            if (r == 25) terrain = Terrain.ROAD;
            if (q == 25) terrain = r == 25 ? Terrain.SHALLOW : Terrain.RIVER;
            tiles.put(new HexPos(q, r), terrain);
        }
        Battlefield field = new Battlefield(tiles);
        for (int i = 0; i < 20; i++) query(field, i);
        long[] elapsed = new long[100]; long started = System.nanoTime(); int maxExpanded = 0;
        for (int i = 0; i < elapsed.length; i++) {
            long start = System.nanoTime(); Pathfinder.Path path = query(field, i); elapsed[i] = System.nanoTime() - start;
            if (path.expandedNodes > field.size()) throw new AssertionError("Path expanded beyond local battlefield");
            maxExpanded = Math.max(maxExpanded, path.expandedNodes);
        }
        double totalMs = (System.nanoTime() - started) / 1e6;
        long reachableStart = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            Map<HexPos, Integer> reached = Pathfinder.reachableTiles(field, WeaponType.CAVALRY,
                    new HexPos(10, 10), Collections.emptySet(), BattleRules.weapon(WeaponType.CAVALRY).movement);
            if (reached.size() > 61) throw new AssertionError("Bounded movement searched too many reachable cells");
        }
        double reachableMs = (System.nanoTime() - reachableStart) / 1e6;
        Arrays.sort(elapsed);
        // Deliberately generous on shared CI; graph expansion bound above is the stable regression guard.
        if (totalMs > 15000 || reachableMs > 5000) throw new AssertionError("Pathfinding smoke timeout");
        System.out.printf(Locale.ROOT,
                "PASS: 50x50 benchmark, 100 A* queries: p50=%.3fms p95=%.3fms max=%.3fms total=%.3fms maxExpanded=%d/2500; 100 reachable queries=%.3fms%n",
                elapsed[49] / 1e6, elapsed[94] / 1e6, elapsed[99] / 1e6, totalMs, maxExpanded, reachableMs);
    }
    private static Pathfinder.Path query(Battlefield field, int index) {
        return Pathfinder.findPath(field, WeaponType.SPEAR, new HexPos(0, index % 10),
                new HexPos(49, 49 - index % 10), Collections.emptySet(), 1000)
                .orElseThrow(() -> new AssertionError("Benchmark bridge route is missing"));
    }
}
