package game.sanguo.mobile;

/** Stable atlas positions. Unknown names use an original deterministic portrait, never a wrong famous face. */
final class PortraitCatalog {
    static final String[] NAMES={"曹操","刘备","孙权","诸葛亮","关羽","张飞","赵云","吕布","貂蝉","孙尚香","黄月英","司马懿","周瑜","张辽","马超","黄忠"};
    private static final String[] TRADITIONAL={"曹操","劉備","孫權","諸葛亮","關羽","張飛","趙雲","呂布","貂蟬","孫尚香","黃月英","司馬懿","周瑜","張遼","馬超","黃忠"};
    static int index(String name){for(int i=0;i<NAMES.length;i++)if(NAMES[i].equals(name)||TRADITIONAL[i].equals(name))return i;return -1;}
    static int variant(int id,String name){return (31*id+name.hashCode())&0x7fffffff;}
}
