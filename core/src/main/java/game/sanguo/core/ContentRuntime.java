package game.sanguo.core;

import java.io.IOException;
import java.util.*;

/** Explicit source-ID bridge. Used only when constructing a new sourced opening, never by save decoding. */
public final class ContentRuntime {
    private static final Map<String,Skill> SKILLS=new HashMap<>();
    private static final Map<String,String> LABELS=new HashMap<>();
    private ContentRuntime(){}
    private static void bind(String source,String label,Skill skill){SKILLS.put(source,skill);LABELS.put(source,label);}
    static {
        bind("skill-000","飛將",Skill.FEIJIANG);
        bind("skill-001","遁走",Skill.DUNZOU);
        bind("skill-002","強行",Skill.QIANGXING);
        bind("skill-003","長驅",Skill.CHANGQU);
        bind("skill-004","推進",Skill.TUIJIN);
        bind("skill-005","操舵",Skill.CAODUO);
        bind("skill-006","踏破",Skill.TAPO);
        bind("skill-007","搬運",Skill.YUNBAN);
        bind("skill-008","解毒",Skill.JIEDU);
        bind("skill-009","掃蕩",Skill.SAOTAO);
        bind("skill-010","威風",Skill.WEIFENG);
        bind("skill-011","昂揚",Skill.ANGYANG);
        bind("skill-012","連擊",Skill.LIANZHAN);
        bind("skill-013","突襲",Skill.JIXI);
        bind("skill-014","強襲",Skill.QIANGXI);
        bind("skill-015","亂戰",Skill.LUANZHAN);
        bind("skill-016","待伏",Skill.DAIFU);
        bind("skill-017","攻城",Skill.GONGCHENG);
        bind("skill-018","掎角",Skill.JIJIAO);
        bind("skill-019","捕縛",Skill.BOFU);
        bind("skill-020","精妙",Skill.JINGMIAO);
        bind("skill-021","掠奪",Skill.QIANGDUO);
        bind("skill-022","攻心",Skill.XINGONG);
        bind("skill-023","驅逐",Skill.QUZHU);
        bind("skill-024","射程",Skill.SHECHENG);
        bind("skill-025","白馬",Skill.BAIMA);
        bind("skill-026","輔佐",Skill.FUZUO);
        bind("skill-027","不屈",Skill.BUQU);
        bind("skill-028","金剛",Skill.JINGANG);
        bind("skill-029","鐵壁",Skill.TIEBI);
        bind("skill-030","怒髮",Skill.NUFA);
        bind("skill-031","藤甲",Skill.TENGJIA);
        bind("skill-032","強運",Skill.QIANGYUN);
        bind("skill-033","血路",Skill.XUELU);
        bind("skill-034","槍將",Skill.QIANGJIANG);
        bind("skill-035","戟將",Skill.JIJIANG);
        bind("skill-036","弓將",Skill.GONGJIANG);
        bind("skill-037","騎將",Skill.QIJIANG);
        bind("skill-038","水將",Skill.SHUIJIANG);
        bind("skill-039","勇將",Skill.YONGJIANG);
        bind("skill-040","神將",Skill.SHENJIANG);
        bind("skill-041","鬥神",Skill.DOUSHEN);
        bind("skill-042","槍神",Skill.QIANGSHEN);
        bind("skill-043","戟神",Skill.JISHEN);
        bind("skill-044","弓神",Skill.GONGSHEN);
        bind("skill-045","騎神",Skill.QISHEN);
        bind("skill-046","工神",Skill.GONGSHEN_SIEGE);
        bind("skill-047","水神",Skill.SHUISHEN);
        bind("skill-048","霸王",Skill.BAWANG);
        bind("skill-049","疾馳",Skill.JICHI);
        bind("skill-050","射手",Skill.SHESHOU);
        bind("skill-051","猛者",Skill.MENGZHE);
        bind("skill-052","護衛",Skill.HUWEI);
        bind("skill-053","火攻",Skill.HUOGONG);
        bind("skill-054","言毒",Skill.YANDU);
        bind("skill-055","機智",Skill.JILUE);
        bind("skill-056","詭計",Skill.GUIJI);
        bind("skill-057","虛實",Skill.XUSHI);
        bind("skill-058","妙計",Skill.MIAOJI);
        bind("skill-059","秘計",Skill.MIJI);
        bind("skill-060","看破",Skill.KANPO);
        bind("skill-061","洞察",Skill.DONGCHA);
        bind("skill-062","火神",Skill.HUOSHEN);
        bind("skill-063","神算",Skill.SHENSUAN);
        bind("skill-064","百出",Skill.BAICHU);
        bind("skill-065","鬼謀",Skill.GUIMOU);
        bind("skill-066","連環",Skill.LIANHUAN);
        bind("skill-067","深謀",Skill.SHENMOU);
        bind("skill-068","反計",Skill.FANJI);
        bind("skill-069","傾國",Skill.QINGGUO);
        bind("skill-070","妖術",Skill.YAOSHU);
        bind("skill-071","鬼門",Skill.GUIMEN);
        bind("skill-072","規律",Skill.GUILV);
        bind("skill-073","沉著",Skill.CHENZHUO);
        bind("skill-074","明鏡",Skill.MINGJING);
        bind("skill-075","奏樂",Skill.ZOUYUE);
        bind("skill-076","詩想",Skill.SHIXIANG);
        bind("skill-077","築城",Skill.ZHUCHENG);
        bind("skill-078","屯田",Skill.TUNTIAN);
        bind("skill-079","名聲",Skill.MINGSHENG);
        bind("skill-080","能吏",Skill.NENGLI);
        bind("skill-081","繁殖",Skill.FANZHI);
        bind("skill-082","發明",Skill.FAMING);
        bind("skill-083","造船",Skill.ZAOCHUAN);
        bind("skill-084","指導",Skill.ZHIDAO);
        bind("skill-085","眼力",Skill.YANLI);
        bind("skill-086","論客",Skill.LUNKE);
        bind("skill-087","富豪",Skill.FUHAO);
        bind("skill-088","米道",Skill.MIDAO);
        bind("skill-089","徵稅",Skill.ZHENGSHUI);
        bind("skill-090","徵收",Skill.ZHENGSHOU);
        bind("skill-091","親烏",Skill.QINWU);
        bind("skill-092","親羌",Skill.QINQIANG);
        bind("skill-093","親越",Skill.QINYUE);
        bind("skill-094","親蠻",Skill.QINMAN);
        bind("skill-095","威壓",Skill.WEIYA);
        bind("skill-096","仁政",Skill.RENZHENG);
        bind("skill-097","風水",Skill.FENGSHUI);
        bind("skill-098","祈願",Skill.QIYUAN);
        bind("skill-099","內助",Skill.NEIZHU);
    }
    public static Skill skill(String sourceId){return SKILLS.get(sourceId);}
    public static void validate(ContentCatalog catalog)throws IOException {
        Set<Skill> skills=new HashSet<>();
        for(ContentCatalog.Entry entry:catalog.rows("skills")){
            Skill skill=skill(entry.id);
            if(skill==null||!entry.name.equals(LABELS.get(entry.id))||!skills.add(skill))throw new IOException("特技来源ID或名称桥接不匹配："+entry.id);
        }
        if(skills.size()!=100)throw new IOException("特技桥接数量错误");
    }
    static void initializeOpening(World w,ContentCatalog catalog)throws IOException {
        validate(catalog);
        for(World.Officer o:w.officers){
            ContentCatalog.Officer definition=catalog.officer(o.id);
            if(definition==null)throw new IOException("开局人物定义缺失");
            if(definition.skillId.equals("none"))o.skillId="none";
            else {Skill skill=skill(definition.skillId);if(skill==null)throw new IOException("开局特技未映射");o.skillId=skill.id;}
            o.sex=definition.gender.equals("男")?World.Sex.MALE:definition.gender.equals("女")?World.Sex.FEMALE:World.Sex.UNKNOWN;
        }
    }
}
