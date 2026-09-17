package game.sanguo.mobile;

import game.sanguo.core.World;

/** Faction identity, independent of scenario ordering or which side is human. */
final class FactionColors {
    private FactionColors(){}
    static int color(World world,int owner){
        if(owner<0)return 0xffa6a6a6;
        String name=world.faction(owner);
        if(name.contains("曹操")||name.contains("曹丕")||name.equals("魏"))return 0xff285be8;
        if(name.contains("刘备")||name.contains("劉備")||name.contains("刘禅")||name.contains("劉禪")||name.equals("蜀"))return 0xff36ac54;
        if(name.contains("孙权")||name.contains("孫權")||name.contains("孙策")||name.contains("孫策")||name.contains("孙坚")||name.contains("孫堅")||name.equals("吴")||name.equals("吳"))return 0xffe34843;
        if(name.contains("張角")||name.contains("张角"))return 0xffe2c236;
        if(name.contains("袁紹")||name.contains("袁绍"))return 0xffb983db;
        if(name.contains("董卓"))return 0xff824dba;
        if(name.contains("呂布")||name.contains("吕布"))return 0xffc89066;
        // Noncanonical sandboxes retain a stable high-contrast palette.
        int[] palette={0xff36ac54,0xff285be8,0xffe34843,0xffa267d5,0xffe0be39,0xff41b8c4,0xffda76b0,0xffbd844e};
        return world.factions.length<=8?palette[Math.floorMod(owner,palette.length)]:palette[Math.floorMod(name.hashCode(),palette.length)];
    }
}
