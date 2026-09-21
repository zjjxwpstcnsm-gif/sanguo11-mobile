package game.sanguo.core;

import java.util.Objects;

/** Shared terrain details and legend. Exhaustive switch deliberately has no ordinal indexing
 * or default: adding a terrain must supply a reviewed presentation at compile time. */
public final class TerrainPresentation {
    public record Definition(String name, String description) {
        public Definition {
            if (name == null || name.isBlank() || description == null || description.isBlank())
                throw new IllegalArgumentException("地形名称与说明不可为空");
        }
        public String legend() { return name + " · " + description; }
    }
    private TerrainPresentation() {}
    public static Definition of(World.Terrain terrain) {
        return switch (Objects.requireNonNull(terrain, "terrain")) {
            case PLAIN -> new Definition("平原", "正常通行；开发仍需检查城市用地、占用与出口条件");
            case FOREST -> new Definition("森林", "增加移动消耗，可伏兵；不能建设军事设施");
            case MOUNTAIN -> new Definition("山地", "普通部队不可通行，可查看地形");
            case WATER -> new Definition("河流", "使用舰船与水军战法；水陆转换须经过港口");
            case MOUNTAIN_PATH -> new Definition("山径", "山地通路，受难所行军与兵种限制");
            case SHALLOWS -> new Definition("浅滩", "浅水通路，受难所行军与兵种限制");
            case PLANK_ROAD -> new Definition("栈道", "山地架设通路；未解锁相应技巧可能损兵");
            case POISON -> new Definition("毒泉", "经过可能损兵，解毒特技可免疫");
            case SEA -> new Definition("海域", "舰船通行，不划入陆地势力范围");
            case VOID -> new Definition("未定义／界外", "未定义地形或界外区域，不可选择、通行或建设；不等同于已确认的原版地图边界");
            case SWAMP -> new Definition("沼泽", "步兵移动消耗2，骑兵与器械消耗4；不能建设军事设施");
            case DAM -> new Definition("堤坝", "堤坝地形；可破坏坝体由独立的中立设施表示");
            case SAND -> new Definition("沙地", "正常通行；枪兵不能施放战法，普通攻击不受影响");
            case NON_NAVIGABLE_WATER -> new Definition("不可航水域", "窄水道或封闭水面；保持原格不可通行，陆军与舰船均不能进入，可查看地形");
            case ROAD -> new Definition("道路", "贴地通路，正常通行；不能占用必要通道开发");
        };
    }
    public record Detail(World.Terrain terrain, Definition presentation, SourceGridCoord local,
                         SourceGridCoord national, Hex axial, int infantryCost) {}
    /** Only real source cells may produce a detail; axial padding is never a map tile. */
    public static Detail detail(World world, Hex hex) {
        Objects.requireNonNull(world, "world");
        if (!world.sourceInside(hex)) throw new IllegalArgumentException("不是有效源地图格：" + hex);
        World.Terrain terrain = world.terrain[hex.q][hex.r];
        return new Detail(terrain, of(terrain), MapCoordinates.source(world, hex),
                MapCoordinates.nationalSource(world, hex), hex, world.cost(hex, World.Weapon.SPEAR));
    }
}
