package game.sanguo.mobile;

import game.sanguo.core.World;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;

/** Stable unique faction colors, assigned independently of player selection. */
final class FactionColors {
    private static final Map<World, int[]> CACHE = Collections.synchronizedMap(new WeakHashMap());
    private static final int[] PALETTE = {-14301736, -1209806, -1875528, -4273870, -7428102, -4433600, -10301535, -5407677, -3167025, -6898586, -12288362, -2384214, -7568063, -10378528, -1062789, -8169112, -5322067, -3458967, -5552147, -2376415, -9669442, -7614791, -7047274, -2395057, -12423456, -11290505, -4143129, -7166152, -1599012, -10191256, -1530528, -9855839, -2134895};

    private FactionColors() {
    }

    static int color(World world, int owner) {
        if (owner < 0 || owner >= world.factions.length) {
            return -5855578;
        }
        return CACHE.computeIfAbsent(world,FactionColors::build)[owner];
    }

    private static int canonical(String name) {
        if (name.contains("曹操") || name.contains("曹丕") || name.equals("魏")) {
            return -14132248;
        }
        if (name.contains("刘备") || name.contains("劉備") || name.contains("刘禅") || name.contains("劉禪") || name.equals("蜀")) {
            return -13194156;
        }
        if (name.contains("孙权") || name.contains("孫權") || name.contains("孙策") || name.contains("孫策") || name.contains("孙坚") || name.contains("孫堅") || name.equals("吴") || name.equals("吳")) {
            return -1882045;
        }
        if (name.contains("張角") || name.contains("张角")) {
            return -1916362;
        }
        if (name.contains("袁紹") || name.contains("袁绍")) {
            return -4619301;
        }
        if (name.contains("董卓")) {
            return -8237638;
        }
        if (name.contains("呂布") || name.contains("吕布")) {
            return -3633050;
        }
        return 0;
    }

    private static int difference(int a, int b) {
        int r = ((a >> 16) & 255) - ((b >> 16) & 255);
        int g = ((a >> 8) & 255) - ((b >> 8) & 255);
        int blue = (a & 255) - (b & 255);
        return (r * r * 2) + (g * g * 3) + (blue * blue);
    }

    public static int[] build(final World w) {
        int[] result = new int[w.factions.length];
        Set<Integer> used = new HashSet<>();
        used.add(-5855578);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < result.length; i++) {
            order.add(Integer.valueOf(i));
        }
        Objects.requireNonNull(w);
        order.sort(Comparator.comparing(w::faction));
        Iterator<Integer> it = order.iterator();
        while (it.hasNext()) {
            int id = it.next().intValue();
            int color = canonical(w.faction(id));
            if (color != 0 && used.add(Integer.valueOf(color))) {
                result[id] = color;
            }
        }
        Iterator<Integer> it2 = order.iterator();
        while (it2.hasNext()) {
            int id2 = it2.next().intValue();
            if (result[id2] == 0) {
                int best = 0;
                int score = -1;
                for (int candidate : PALETTE) {
                    if (!used.contains(Integer.valueOf(candidate))) {
                        int nearest = Integer.MAX_VALUE;
                        Iterator<Integer> it3 = used.iterator();
                        while (it3.hasNext()) {
                            int previous = it3.next().intValue();
                            nearest = Math.min(nearest, difference(candidate, previous));
                        }
                        if (nearest > score) {
                            score = nearest;
                            best = candidate;
                        }
                    }
                }
                result[id2] = best;
                used.add(Integer.valueOf(best));
            }
        }
        return result;
    }
}
