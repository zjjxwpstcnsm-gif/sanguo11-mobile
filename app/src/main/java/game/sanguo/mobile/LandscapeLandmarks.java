package game.sanguo.mobile;

import java.util.*;

/** Shared presentation anchors for landscape placement and the normal camera menu. */
final class LandscapeLandmarks {
    record Entry(String label,int x,int y,int cascade) {}
    static final List<Entry> ALL=List.of(
        new Entry("泰山区域瀑布",147,59,0),new Entry("西南山涧瀑布",31,183,1),
        new Entry("庐山区域瀑布",119,146,2),new Entry("黄河壶口瀑布",66,58,3),
        new Entry("北方长城",92,15,-1),new Entry("太湖烟波",175,107,-2));
}
