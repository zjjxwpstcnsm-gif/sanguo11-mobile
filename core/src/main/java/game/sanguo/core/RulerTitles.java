package game.sanguo.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** SAN11 title/command data. Evidence and remaining event differences: docs/RULER_TITLES.md.
 * Cities mean CITY sites, never ports, gates or individual cells of a city footprint.
 * Grades are internal progression indices, NOT the legacy v0.66 save ordinals. */
public final class RulerTitles {
    private RulerTitles() {}

    public enum Title {
        NONE("无爵位",0,10000),
        INSPECTOR("州刺史",2,11000),
        GOVERNOR("州牧",4,11000),
        YULIN("羽林中郎将",6,12000),
        WUGUAN("五官中郎将",8,12000),
        GENERAL("大将军",12,13000),
        MARSHAL("大司马",14,13000),
        DUKE("公",18,14000),
        KING("王",20,14000),
        EMPEROR("皇帝",24,15000);

        public final String label;
        public final int cities,troops;
        Title(String label,int cities,int troops){this.label=label;this.cities=cities;this.troops=troops;}
        public int grade(){return ordinal();}
        public Title next(){return this==EMPEROR?null:TABLE.get(ordinal()+1);}
    }

    private static final List<Title> TABLE=Collections.unmodifiableList(Arrays.asList(Title.values()));
    public static List<Title> all(){return TABLE;}
    public static Title at(int grade){
        if(grade<0||grade>=TABLE.size())throw new IllegalArgumentException("君主爵位等级无效: "+grade);
        return TABLE.get(grade);
    }
    /** Pure eligibility table; acquisition timing belongs to Governance. */
    public static Title forCities(int count){
        Title earned=Title.NONE;
        for(Title title:TABLE)if(count>=title.cities)earned=title;
        return earned;
    }
    /** Preserve already acquired titles and national names in six-tier v0.66 saves. */
    public static Title fromLegacyGrade(int grade){
        switch(grade){
            case 0:return Title.NONE;
            case 1:return Title.INSPECTOR;
            case 2:return Title.GOVERNOR;
            case 3:return Title.GENERAL;
            case 4:return Title.KING;
            case 5:return Title.EMPEROR;
            default:throw new IllegalArgumentException("旧版君主爵位等级无效: "+grade);
        }
    }
}
