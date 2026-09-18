package game.sanguo.mobile;

final class PortraitCatalog {
    static final String[] NAMES = {"曹操", "刘备", "孙权", "诸葛亮", "关羽", "张飞", "赵云", "吕布", "貂蝉", "孙尚香", "黄月英", "司马懿", "周瑜", "张辽", "马超", "黄忠", "夏侯惇", "袁绍", "典韦", "许褚", "张郃", "徐晃", "吕蒙", "陆逊", "鲁肃", "太史慈", "甘宁", "庞统", "姜维", "魏延", "大乔", "小乔"};
    private static final String[] TRADITIONAL = {"曹操", "劉備", "孫權", "諸葛亮", "關羽", "張飛", "趙雲", "呂布", "貂蟬", "孫尚香", "黃月英", "司馬懿", "周瑜", "張遼", "馬超", "黃忠", "夏侯惇", "袁紹", "典韋", "許褚", "張郃", "徐晃", "呂蒙", "陸遜", "魯肅", "太史慈", "甘寧", "龐統", "姜維", "魏延", "大喬", "小喬"};

    PortraitCatalog() {
    }

    static int index(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name) || TRADITIONAL[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    static int variant(int id, String name) {
        return ((id * 31) + name.hashCode()) & Integer.MAX_VALUE;
    }
}
