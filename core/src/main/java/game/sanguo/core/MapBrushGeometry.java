package game.sanguo.core;

import java.util.*;

/** Sampling-independent editor strokes on the production axial six-neighbour grid.
 * Returns candidates only: the edit session applies its protection/transaction rules. */
public final class MapBrushGeometry {
    private MapBrushGeometry() {}

    /** Ordered, gap-free line. Reversing a drag yields exactly the reversed cell list. */
    public static List<Hex> line(Hex from, Hex to) {
        Objects.requireNonNull(from,"from"); Objects.requireNonNull(to,"to");
        boolean reverse=from.q>to.q || from.q==to.q && from.r>to.r;
        Hex a=reverse?to:from, b=reverse?from:to;
        int steps=a.distance(b);
        if(steps>10000)throw new IllegalArgumentException("Stroke endpoints too far apart");
        List<Hex> result=new ArrayList<>(steps+1);
        if(steps==0){result.add(a);return result;}
        for(int i=0;i<=steps;i++) {
            double t=(double)i/steps;
            // Fixed tie-break keeps lines through vertices deterministic on all six axes.
            double q=a.q+(b.q-a.q)*t+1e-6;
            double r=a.r+(b.r-a.r)*t+1e-6;
            double s=-q-r;
            int iq=(int)Math.round(q),ir=(int)Math.round(r),is=(int)Math.round(s);
            double dq=Math.abs(iq-q),dr=Math.abs(ir-r),ds=Math.abs(is-s);
            if(dq>dr&&dq>ds)iq=-ir-is;else if(dr>ds)ir=-iq-is;
            Hex h=new Hex(iq,ir);
            if(result.isEmpty()||!h.equals(result.get(result.size()-1)))result.add(h);
        }
        if(reverse)Collections.reverse(result);
        return result;
    }

    /** Radius 0/1/2 means 1/7/19 real hexes, not a square of screen pixels. */
    public static Set<Hex> disk(Hex center,int radius) {
        if(radius<0||radius>8)throw new IllegalArgumentException("Brush radius must be 0..8");
        LinkedHashSet<Hex> out=new LinkedHashSet<>();
        for(int dq=-radius;dq<=radius;dq++)
            for(int dr=Math.max(-radius,-dq-radius);dr<=Math.min(radius,-dq+radius);dr++)
                out.add(new Hex(center.q+dq,center.r+dr));
        return out;
    }

    /** Interpolate sparse touch samples first; expand the complete path second. */
    public static Set<Hex> stroke(World world,Hex from,Hex to,int radius) {
        Objects.requireNonNull(world,"world");
        if(!world.sourceInside(from)||!world.sourceInside(to))return Collections.emptySet();
        LinkedHashSet<Hex> out=new LinkedHashSet<>();
        for(Hex h:line(from,to))for(Hex p:disk(h,radius))if(world.sourceInside(p))out.add(p);
        return out;
    }
}
