package game.sanguo.core;

import java.util.HashMap;
import java.util.Map;


public enum Skill {
    FEIJIANG("san11.feijiang", "飞将", "非兵器部队在陆上无视敌军控制区；武力高于目标时，陆战战法成功命中必暴击。"),
    DUNZOU("san11.dunzou", "遁走", "非兵器部队在陆上行军时无视敌军控制区；不能穿过实际占用的地块。"),
    QIANGXING("san11.qiangxing", "强行", "枪、戟、弩部队在陆上移动力增加1。"),
    CHANGQU("san11.changqu", "长驱", "骑兵在陆上移动力增加1。"),
    TUIJIN("san11.tuijin", "推进", "部队在水上行军时无视敌军控制区；仍需遵守占格与水陆通行限制。"),
    CAODUO("san11.caoduo", "操舵", "部队在水上移动力增加1。"),
    TAPO("san11.tapo", "踏破", "免除未研究难所行军时的栈道损兵，免疫石兵八阵混乱，受到的火陷阱伤害减半。"),
    YUNBAN("san11.yunban", "运搬", "所在运输队移动力增加2；适用于陆运和水运。"),
    JIEDU("san11.jiedu", "解毒", "所在部队经过、驻留毒泉时不受毒泉损兵。"),
    SAOTAO("san11.saotao", "扫讨", "对部队造成有效物理伤害时，目标气力降低5；与威风取较强效果。"),
    WEIFENG("san11.weifeng", "威风", "对部队造成有效物理伤害时，目标气力降低20；普通攻击和战法均可触发。"),
    ANGYANG("san11.angyang", "昂扬", "击破敌部队时恢复本队10气力，不超过气力上限。"),
    LIANZHAN("san11.lianzhan", "连战", "普通攻击或作为齐攻主攻部队时，有50%概率追加一次攻击。"),
    JIXI("san11.jixi", "急袭", "陆上攻击有50%概率避免敌方反击。"),
    QIANGXI("san11.qiangxi", "强袭", "水上攻击有50%概率避免敌方反击。"),
    LUANZHAN("san11.luanzhan", "乱战", "本队位于森林时，对部队的物理攻击必暴击。"),
    GONGCHENG("san11.gongcheng", "攻城", "攻击据点时，耐久及守军伤害提高15%；对设施的战法攻击可触发暴击。"),
    JIJIAO("san11.jijiao", "掎角", "参与齐攻且目标存活时，有50%概率使其混乱1旬。"),
    BOFU("san11.bofu", "捕缚", "击破敌部队时保证俘获其武将；血路、强运和名马的俘虏保护优先。"),
    JINGMIAO("san11.jingmiao", "精妙", "击破敌军获得的战斗技巧点加倍。"),
    QIANGDUO("san11.qiangduo", "强夺", "击破敌部队时可夺取其携带宝物，具体受宝物与持有人状态限制。"),
    XINGONG("san11.xingong", "心攻", "造成有效物理伤害时，将伤害的10%吸收为本队兵力，不超过带兵上限。"),
    QUZHU("san11.quzhu", "驱逐", "特技持有者武力高于目标部队武力时，普通攻击必暴击。"),
    SHECHENG("san11.shecheng", "射程", "陆上井阑、投石部队射程增加1。"),
    BAIMA("san11.baima", "白马", "骑兵可进行远程射击；骑射普通攻击必暴击。"),
    FUZUO("san11.fuzuo", "辅佐", "作为支援部队主将时，有30%概率支援射程内友军的普通攻击；亲密关系按其支援规则优先。"),
    QIANGJIANG("san11.qiangjiang", "枪将", "持有者武力高于目标部队武力时，枪兵战法成功命中必暴击。"),
    JIJIANG("san11.jijiang", "戟将", "持有者武力高于目标部队武力时，戟兵战法成功命中必暴击。"),
    GONGJIANG("san11.gongjiang", "弓将", "持有者武力高于目标部队武力时，弩兵战法成功命中必暴击。"),
    QIJIANG("san11.qijiang", "骑将", "持有者武力高于目标部队武力时，骑兵战法成功命中必暴击。"),
    SHUIJIANG("san11.shuijiang", "水将", "持有者武力高于目标部队武力时，水军战法成功命中必暴击。"),
    YONGJIANG("san11.yongjiang", "勇将", "持有者武力高于目标部队武力时，对部队的各类战法成功命中必暴击。"),
    SHENJIANG("san11.shenjiang", "神将", "持有者武力高于目标部队武力时，普通攻击及陆战战法成功命中必暴击。"),
    DOUSHEN("san11.doushen", "斗神", "枪兵、戟兵战法成功命中必暴击，无需比较武力。"),
    QIANGSHEN("san11.qiangshen", "枪神", "枪兵战法成功命中必暴击；螺旋突刺的暴击命中会触发混乱。"),
    JISHEN("san11.jishen", "戟神", "戟兵战法成功命中必暴击，无需比较武力。"),
    GONGSHEN("san11.gongshen", "弓神", "弩兵战法成功命中必暴击；乱射不误伤己方部队。"),
    QISHEN("san11.qishen", "骑神", "骑兵战法成功命中必暴击，无需比较武力。"),
    GONGSHEN_SIEGE("san11.gongshen_siege", "工神", "兵器战法成功命中必暴击，无需比较武力。"),
    SHUISHEN("san11.shuishen", "水神", "水军战法成功命中必暴击，无需比较武力。"),
    BAWANG("san11.bawang", "霸王", "各兵种战法成功命中必暴击；不会将战法命中率直接变为100%。"),
    JICHI("san11.jichi", "疾驰", "本队攻击力高于目标时，骑兵战法成功命中可使目标混乱。"),
    SHESHOU("san11.sheshou", "射手", "允许弩兵普通攻击、战法及支援攻击森林中的目标。"),
    MENGZHE("san11.mengzhe", "猛者", "位移战法命中后有50%概率令敌军武将负伤；强运及同队护卫可提供保护。"),
    BUQU("san11.buqu", "不屈", "本队兵力低于3000时，受到普通攻击有50%概率免伤。"),
    JINGANG("san11.jingang", "金刚", "单次普通攻击计算伤害低于500时，有50%概率免伤。"),
    TIEBI("san11.tiebi", "铁壁", "敌方齐攻转为主攻部队的普通攻击，阻止其他部队参加该次齐攻。"),
    NUFA("san11.nufa", "怒发", "受到战法有效物理伤害且部队存活时，恢复5气力。"),
    TENGJIA("san11.tengjia", "藤甲", "受到的物理伤害减半；火焰伤害加倍，火神免疫优先。"),
    QIANGYUN("san11.qiangyun", "强运", "持有者免于战斗俘虏及猛者负伤，并受灾害负伤保护；不免除自然寿终。"),
    XUELU("san11.xuelu", "血路", "部队被击破时，保护同队武将免于战斗俘虏。"),
    HUWEI("san11.huwei", "护卫", "保护同队其他武将免于猛者负伤；所在部队免受持续燃烧损兵。"),
    DAIFU("san11.daifu", "待伏", "伏兵计略成功时必暴击。"),
    HUOGONG("san11.huogong", "火攻", "持有者智力高于目标部队智力时，火计必成功；目标免疫优先。"),
    YANDU("san11.yandu", "言毒", "持有者智力高于目标部队智力时，伪报必成功；规律、明镜等免疫优先。"),
    JILUE("san11.jilue", "机略", "持有者智力高于目标部队智力时，扰乱必成功；沉着、明镜等免疫优先。"),
    GUIJI("san11.guiji", "诡计", "持有者智力高于目标部队智力时，同讨必成功；目标计略免疫优先。"),
    XUSHI("san11.xushi", "虚实", "持有者智力高于目标部队智力时，普通进攻计略必成功；不覆盖目标免疫。"),
    MIAOJI("san11.miaoji", "妙计", "持有者智力高于目标部队智力时，计略成功必暴击。"),
    MIJI("san11.miji", "秘计", "持有者智力低于目标部队智力时，计略成功必暴击。"),
    KANPO("san11.kanpo", "看破", "持有者智力高于来袭部队智力时，免疫其进攻计略。"),
    DONGCHA("san11.dongcha", "洞察", "免疫敌军进攻计略及石兵八阵造成的混乱；不免疫物理伤害。"),
    HUOSHEN("san11.huoshen", "火神", "本队火计、火矢火焰、火陷阱与后续燃烧的火伤加倍，并免疫所受火伤。持有者智力高于目标时火计必成功。火矢的物理伤害独立结算。"),
    SHENSUAN("san11.shensuan", "神算", "持有者智力高于目标时，普通进攻计略必成功；计略成功必暴击。持有者智力高于来袭部队时免疫其进攻计略，目标免疫仍优先。"),
    BAICHU("san11.baichu", "百出", "部队施放计略的气力消耗降为1；不影响战法气力消耗。"),
    GUIMOU("san11.guimou", "鬼谋", "部队计略射程增加1。"),
    LIANHUAN("san11.lianhuan", "连环", "扰乱、伪报或火计成功后，效果可波及目标相邻的敌方部队。"),
    SHENMOU("san11.shenmou", "深谋", "计略成功时必暴击；不直接提高成功率。"),
    FANJI("san11.fanji", "反计", "受到可反射计略时向施术部队反制；反制不会再次反射。"),
    QINGGUO("san11.qingguo", "倾国", "目标编队全部为男性时，普通进攻计略基础成功率加倍，上限100%；目标免疫优先。"),
    YAOSHU("san11.yaoshu", "妖术", "解锁部队计略“妖术”，具体射程、气力与命中概率见施放预览。"),
    GUIMEN("san11.guimen", "鬼门", "解锁“妖术”和“落雷”，具体射程、气力与命中概率见施放预览。"),
    GUILV("san11.guilv", "规律", "免疫伪报计略。"),
    CHENZHUO("san11.chenzhuo", "沉着", "免疫扰乱计略。"),
    MINGJING("san11.mingjing", "明镜", "免疫扰乱与伪报计略。"),
    ZOUYUE("san11.zouyue", "奏乐", "不在军乐台恢复范围内时，每次己方阶段恢复5气力。"),
    SHIXIANG("san11.shixiang", "诗想", "在己方军乐台范围内，将每阶段恢复的气力从10提高到20。"),
    ZHUCHENG("san11.zhucheng", "筑城", "所在部队建设军事设施的每次施工进度加倍。"),
    TUNTIAN("san11.tuntian", "屯田", "持有者驻扎的港口、关卡不消耗驻军粮食；不适用于城市和野战部队。"),
    MINGSHENG("san11.mingsheng", "名声", "持有者执行征兵时兵数提高50%，受兵源和容量限制；治安损失为7。"),
    NENGLI("san11.nengli", "能吏", "持有者生产枪、戟、弩时金费用减半；不额外增加产量。"),
    FANZHI("san11.fanzhi", "繁殖", "持有者生产战马时金费用减半；不额外增加产量。"),
    FAMING("san11.faming", "发明", "持有者制造兵器时，生产时间由3旬缩短至2旬。"),
    ZAOCHUAN("san11.zaochuan", "造船", "持有者制造舰船时，生产时间由3旬缩短至2旬。"),
    ZHIDAO("san11.zhidao", "指导", "持有者执行技巧研究时，研究金费用减半。"),
    YANLI("san11.yanli", "眼力", "持有者执行搜索时，发现成功率为100%；仍需存在可发现的人才。"),
    LUNKE("san11.lunke", "论客", "玩家持有者执行外交交涉时，可进入舌战流程影响结果。"),
    FUHAO("san11.fuhao", "富豪", "持有者所在城市按月结算的金收入提高50%。"),
    MIDAO("san11.midao", "米道", "持有者所在城市按季结算的粮收入提高50%。"),
    ZHENGSHUI("san11.zhengshui", "征税", "将所在城市的金收入按旬分配到账，便于周转；不是每旬额外获得整月收入。"),
    ZHENGSHOU("san11.zhengshou", "征收", "将所在城市的粮收入按月分配到账，便于周转；不是每月额外获得整季收入。"),
    QINWU("san11.qinwu", "亲乌", "持有者所在城市免于乌丸部族侵扰。"),
    QINQIANG("san11.qinqiang", "亲羌", "持有者所在城市免于羌族侵扰。"),
    QINYUE("san11.qinyue", "亲越", "持有者所在城市免于山越侵扰。"),
    QINMAN("san11.qinman", "亲蛮", "持有者所在城市免于南蛮侵扰。"),
    WEIYA("san11.weiya", "威压", "所在城市触发贼乱的治安门槛由80降至60。"),
    RENZHENG("san11.renzheng", "仁政", "持有者所在据点的武将免于因低治安、缺金造成的每月忠诚下降。"),
    FENGSHUI("san11.fengshui", "风水", "持有者所在城市免于新发生的灾害。"),
    QIYUAN("san11.qiyuan", "祈愿", "所在城市丰收发生率由10%提高到30%。"),
    NEIZHU("san11.neizhu", "内助", "通过结婚建立配偶关系时，提高夫妻双方能力；不因反复查看或存读档重复增加。");

    private static final Map<String, Skill> INDEX = new HashMap();
    public final String description;
    public final String id;
    public final String label;

    static {
        for (Skill s : values()) {
            if (INDEX.put(s.id, s) != null) {
                throw new ExceptionInInitializerError(s.id);
            }
        }
    }

    Skill(String id, String label, String description) {
        this.id = id;
        this.label = label;
        this.description = description;
    }

    public static Skill find(String id) {
        return INDEX.get(id);
    }

    public static String description(String id) {
        Skill s = find(id);
        return s != null ? s.description : "none".equals(id) ? "该武将当前没有特技。" : "暂无此特技的说明。";
    }

    public static String label(String id) {
        Skill s = find(id);
        return s != null ? s.label : "none".equals(id) ? "无" : id;
    }
}
