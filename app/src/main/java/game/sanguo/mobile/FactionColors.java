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

/** Stable faction fills, assigned independently of player selection. Text has its own contrast policy. */
final class FactionColors {
    static final int LABEL_BACKGROUND = 0xff122027;
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
        Objects.requireNonNull(w);
        String[] names = new String[w.factions.length];
        for (int i = 0; i < names.length; i++) names[i] = w.faction(i);
        return build(names);
    }

    static int[] build(String[] names) {
        int[] result = new int[names.length];
        Set<Integer> used = new HashSet<>();
        used.add(-5855578);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < result.length; i++) {
            order.add(Integer.valueOf(i));
        }
        order.sort(Comparator.comparing(i -> names[i]));
        int generated = 0;
        Iterator<Integer> it = order.iterator();
        while (it.hasNext()) {
            int id = it.next().intValue();
            int color = canonical(names[id]);
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
                // The installed sources contain 47 slots, including inactive ones.
                // Keep the original palette choices, then extend deterministically.
                // An odd permutation visits every RGB value before repeating; only
                // the finite RGB space can exhaust uniqueness, never opacity.
                if (best == 0) {
                    do {
                        best = 0xff000000 | ((generated++ * 0x9e3779 + 0x5a83c7) & 0xffffff);
                    } while (used.size() < 0x1000000 && used.contains(best));
                }
                result[id2] = best;
                used.add(Integer.valueOf(best));
            }
        }
        return result;
    }

    /** Preserve hue where possible, lifting only label ink to 4.5:1 on its opaque plaque. */
    static int textColor(int fill) { return readableText(fill, LABEL_BACKGROUND); }

    static int readableText(int color, int background) {
        color |= 0xff000000;
        if (contrast(color, background) >= 4.5) return color;
        int r = (color >>> 16) & 255, g = (color >>> 8) & 255, b = color & 255;
        int low = 0, high = 255;
        while (low < high) {
            int step = (low+high)/2;
            int lifted = lift(r,g,b,step);
            if (contrast(lifted, background) >= 4.5) high = step;
            else low = step+1;
        }
        return lift(r,g,b,high);
    }
    private static int lift(int r,int g,int b,int step) {
        return 0xff000000 | ((r + (255-r)*step/255) << 16)
            | ((g + (255-g)*step/255) << 8) | (b + (255-b)*step/255);
    }

    static double contrast(int a, int b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);
    }
    private static double luminance(int color) {
        return .2126*linear((color>>>16)&255)+.7152*linear((color>>>8)&255)+.0722*linear(color&255);
    }
    private static double linear(int channel) {
        double c = channel/255.0;
        return c <= .04045 ? c/12.92 : Math.pow((c+.055)/1.055,2.4);
    }
}
