package game.sanguo.core.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Local battlefield axial coordinates. Independent of the strategic world's map. */
public final class HexPos implements Comparable<HexPos> {
    // A finite local domain also prevents integer overflow in distance/range arithmetic.
    public static final int LIMIT = 1000000;
    public final int q, r;

    public enum Direction {
        E(1, 0), NE(1, -1), NW(0, -1), W(-1, 0), SW(-1, 1), SE(0, 1);
        public final int dq, dr;
        Direction(int dq, int dr) { this.dq = dq; this.dr = dr; }
        public Direction opposite() { return values()[(ordinal() + 3) % 6]; }
    }

    public HexPos(int q, int r) {
        if (Math.abs((long) q) > LIMIT || Math.abs((long) r) > LIMIT)
            throw new IllegalArgumentException("Coordinates exceed local battlefield limits");
        this.q = q; this.r = r;
    }
    public HexPos neighbor(Direction direction) {
        return new HexPos(q + direction.dq, r + direction.dr);
    }
    public HexPos neighbor(int direction) {
        if (direction < 0 || direction >= 6) throw new IllegalArgumentException("Direction must be 0..5");
        return neighbor(Direction.values()[direction]);
    }
    public List<HexPos> neighbors() {
        List<HexPos> result = new ArrayList<>(6);
        for (Direction direction : Direction.values()) result.add(neighbor(direction));
        return Collections.unmodifiableList(result);
    }
    public int distance(HexPos other) {
        int dq = q - other.q, dr = r - other.r;
        return (Math.abs(dq) + Math.abs(dr) + Math.abs(dq + dr)) / 2;
    }
    /** Includes the center. The result has exactly 1 + 3*r*(r+1) cells. */
    public List<HexPos> range(int radius) {
        if (radius < 0 || radius > 256) throw new IllegalArgumentException("Radius must be 0..256");
        List<HexPos> result = new ArrayList<>();
        for (int dq = -radius; dq <= radius; dq++) {
            int low = Math.max(-radius, -dq - radius), high = Math.min(radius, -dq + radius);
            for (int dr = low; dr <= high; dr++) result.add(new HexPos(q + dq, r + dr));
        }
        return Collections.unmodifiableList(result);
    }
    /** A direction exists only along one of the six straight hex rays, never for the center. */
    public Optional<Direction> directionTo(HexPos other) {
        int distance = distance(other);
        if (distance == 0) return Optional.empty();
        for (Direction direction : Direction.values())
            if (q + direction.dq * distance == other.q && r + direction.dr * distance == other.r)
                return Optional.of(direction);
        return Optional.empty();
    }
    @Override public int compareTo(HexPos other) {
        int first = Integer.compare(q, other.q);
        return first == 0 ? Integer.compare(r, other.r) : first;
    }
    @Override public boolean equals(Object other) {
        return other instanceof HexPos && q == ((HexPos) other).q && r == ((HexPos) other).r;
    }
    @Override public int hashCode() { return 31 * q + r; }
    @Override public String toString() { return q + "," + r; }
}
