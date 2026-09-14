package game.sanguo.core;

import java.util.*;

/** PC PK research/training lifecycle. Evidence and unresolved differences: docs/PK_V0_11.md. */
public final class AbilityResearch {
    public enum Category { STAT("基础能力"), APTITUDE("兵种适性"), SKILL("特技");
        public final String label; Category(String label){this.label=label;}
    }
    public static final class Node {
        public final String id,label,branch,slot;
        public final Category category;
        public final int index,cap,uses,turns;
        public final Skill skill;
        public final List<String> prerequisites;
        Node(String id,String label,String branch,Category category,int index,int cap,int uses,int months,Skill skill,String slot,String... prerequisites){
            this.id=id;this.label=label;this.branch=branch;this.category=category;this.index=index;this.cap=cap;this.uses=uses;turns=months*3;this.skill=skill;this.slot=slot;
            this.prerequisites=Collections.unmodifiableList(Arrays.asList(prerequisites));
        }
        public String effect(){return category==Category.STAT?"提高"+Campaign.Study.values()[index].label+"5点，上限"+cap:
            category==Category.APTITUDE?Campaign.Study.values()[index+5].label+"从"+War.rankLabel(cap-1)+"提升至"+War.rankLabel(cap):"习得"+skill.label+"，覆盖原有特技";}
    }
    private static final Map<String,Node> CATALOG=new LinkedHashMap<>();
    private static void stat(String id,String label,String branch,int index,int cap,int months,String... pre){add(new Node(id,label,branch,Category.STAT,index,cap,cap==95?3:5,months,null,"",pre));}
    private static void aptitude(String id,String label,int index,int cap,String... pre){add(new Node(id,label,"攻击",Category.APTITUDE,index,cap,5,cap==1?4:5,null,"",pre));}
    private static void skill(String id,String branch,Skill skill,int months,int uses,String... pre){add(new Node(id,skill.label,branch,Category.SKILL,-1,0,uses,months,skill,"",pre));}
    private static void add(Node node){if(CATALOG.put(node.id,node)!=null)throw new ExceptionInInitializerError(node.id);}
    private static void hidden(String slot,String branch,int months,Skill[] skills,int[] uses,String... prerequisites){
        for(int i=0;i<skills.length;i++)add(new Node("hidden."+slot+"."+i,skills[i].label,branch,Category.SKILL,-1,0,uses[i],months,skills[i],slot,prerequisites[i].split(",")));
    }
    static {
        stat("war.low","武力+5低","攻击",1,70,3);
        stat("lead.low","统率+5低","防御",0,70,3);
        stat("int.low","智力+5低","计略",2,70,3);
        stat("pol.low","政治+5低","内政",3,70,3);
        aptitude("bow.b","弩兵B",2,1,"war.low");
        aptitude("spear.b","枪兵B",0,1,"bow.b");
        aptitude("halberd.b","戟兵B",1,1,"war.low");
        aptitude("navy.b","水军B",5,1,"halberd.b");
        stat("war.mid","武力+5中","攻击",1,80,4,"spear.b","navy.b");
        aptitude("bow.a","弩兵A",2,2,"war.mid");
        aptitude("spear.a","枪兵A",0,2,"bow.a");
        skill("lianzhan","攻击",Skill.LIANZHAN,6,3,"spear.a");
        aptitude("siege.b","兵器B",4,1,"war.low");
        aptitude("cavalry.b","骑兵B",3,1,"siege.b");
        skill("shecheng","攻击",Skill.SHECHENG,4,3,"cavalry.b");
        aptitude("siege.a","兵器A",4,2,"shecheng");
        aptitude("cavalry.a","骑兵A",3,2,"siege.a");
        stat("war.high","武力+5高","攻击",1,95,6,"lianzhan","cavalry.a");
        aptitude("halberd.a","戟兵A",1,2,"navy.b");
        aptitude("navy.a","水军A",5,2,"halberd.a");
        skill("fuzuo","防御",Skill.FUZUO,6,3,"navy.a");
        skill("buqu","防御",Skill.BUQU,3,3,"lead.low");
        skill("jingang","防御",Skill.JINGANG,3,3,"buqu");
        stat("lead.mid","统率+5中","防御",0,80,4,"jingang");
        stat("lead.high","统率+5高","防御",0,95,6,"lead.mid");
        skill("bawang","防御",Skill.BAWANG,6,1,"fuzuo","lead.mid");
        skill("zhucheng","内政",Skill.ZHUCHENG,3,3,"pol.low");
        // Official PC manual p.10 names 不屈 + 築城; community diagram disagrees on the first arrow.
        skill("tiebi","防御",Skill.TIEBI,3,3,"buqu","zhucheng");
        skill("yunban","内政",Skill.YUNBAN,3,3,"zhucheng");
        stat("pol.mid","政治+5中","内政",3,80,4,"yunban");
        skill("zaochuan","内政",Skill.ZAOCHUAN,4,3,"pol.mid");
        skill("faming","内政",Skill.FAMING,4,3,"zaochuan");
        stat("pol.high","政治+5高","内政",3,95,6,"faming");
        skill("fanzhi","内政",Skill.FANZHI,6,3,"pol.high");
        skill("nengli","内政",Skill.NENGLI,6,3,"fanzhi");
        skill("mingsheng","防御",Skill.MINGSHENG,6,3,"lead.mid","nengli");
        skill("bofu","防御",Skill.BOFU,6,1,"mingsheng");
        skill("lunke","内政",Skill.LUNKE,6,1,"bofu","nengli");
        skill("daifu","计略",Skill.DAIFU,3,3,"int.low");
        skill("saotao","计略",Skill.SAOTAO,3,3,"daifu");
        stat("int.mid","智力+5中","计略",2,80,4,"saotao");
        stat("charm.low","魅力+5低","计略",4,70,3,"int.low","pol.low");
        skill("weiya","计略",Skill.WEIYA,3,3,"charm.low");
        stat("charm.mid","魅力+5中","计略",4,80,4,"weiya");
        stat("charm.high","魅力+5高","计略",4,95,6,"charm.mid");
        skill("mingjing","计略",Skill.MINGJING,6,3,"int.mid","weiya");
        skill("shenmou","计略",Skill.SHENMOU,6,1,"mingjing");
        stat("int.high","智力+5高","计略",2,95,6,"shenmou");
        hidden("war_a","攻击",6,new Skill[]{Skill.LUANZHAN,Skill.JIJIAO,Skill.YONGJIANG,Skill.DOUSHEN,Skill.JICHI},new int[]{5,5,5,5,5},"spear.a","lianzhan","spear.a","lianzhan","war.high");
        hidden("war_b","攻击",3,new Skill[]{Skill.XINGONG,Skill.QIANGSHEN,Skill.JISHEN,Skill.GONGSHEN,Skill.SHUISHEN},new int[]{5,3,3,3,3},"cavalry.b","spear.b,lianzhan","bow.b,lianzhan","bow.b,lianzhan","bow.b,lianzhan");
        hidden("war_c","攻击",6,new Skill[]{Skill.GONGCHENG,Skill.SHENJIANG,Skill.QISHEN,Skill.GONGSHEN_SIEGE,Skill.MENGZHE},new int[]{5,5,3,3,5},"int.high,siege.a","int.high,siege.a","siege.a","siege.a","siege.a");
        hidden("lead_a","防御",3,new Skill[]{Skill.CHANGQU,Skill.TAPO,Skill.XUELU,Skill.HUWEI,Skill.ZOUYUE},new int[]{5,5,5,5,3},"jingang","jingang","lead.low","buqu","fuzuo,buqu");
        hidden("lead_b","防御",6,new Skill[]{Skill.FEIJIANG,Skill.DUNZOU,Skill.QIANGXING,Skill.TENGJIA,Skill.QIANGYUN},new int[]{3,3,3,3,5},"lead.high,bawang","lead.mid","lead.high","lead.high","lead.mid");
        hidden("int_a","计略",6,new Skill[]{Skill.XUSHI,Skill.DONGCHA,Skill.SHENSUAN,Skill.BAICHU,Skill.GUIMEN},new int[]{3,3,3,3,3},"int.high","int.high","shenmou","int.high","int.high");
        hidden("int_b","计略",3,new Skill[]{Skill.KANPO,Skill.HUOSHEN,Skill.GUIMOU,Skill.LIANHUAN,Skill.FANJI},new int[]{3,3,3,3,3},"charm.low,daifu","int.mid","int.low,mingjing","charm.low,daifu","int.low,daifu");
        hidden("pol_a","内政",3,new Skill[]{Skill.ZHIDAO,Skill.YANLI,Skill.ZHENGSHUI,Skill.FENGSHUI,Skill.QIYUAN},new int[]{5,5,3,5,5},"buqu,zhucheng","buqu,zhucheng","fanzhi","zhucheng","zhucheng");
        hidden("pol_b","内政",6,new Skill[]{Skill.MIDAO,Skill.MIDAO,Skill.ZHENGSHOU,Skill.ZHENGSHOU,Skill.ZHENGSHOU},new int[]{5,5,5,5,5},"faming","pol.high","faming","pol.high","pol.high,faming");
        hidden("pol_c","内政",6,new Skill[]{Skill.TUNTIAN,Skill.TUNTIAN,Skill.TUNTIAN,Skill.RENZHENG,Skill.RENZHENG},new int[]{5,5,5,5,5},"zaochuan,faming","zaochuan","faming","zaochuan","faming");
        for(Node n:CATALOG.values())for(String p:n.prerequisites)if(!CATALOG.containsKey(p))throw new ExceptionInInitializerError(p);
    }
    static final class State {
        final SortedSet<String> hidden=new TreeSet<>(),learned=new TreeSet<>();
        final SortedMap<String,Integer> used=new TreeMap<>();
        Research research;
    }
    public static final class Research {
        public final int cityId; public final String nodeId; public int remaining;
        Research(int city,String node,int remaining){cityId=city;nodeId=node;this.remaining=remaining;}
    }
    public static final class Training {
        public final int owner,cityId,officerId,startValue; public final String nodeId,previousSkill;
        Training(int owner,int city,int officer,String node,int value,String skill){this.owner=owner;cityId=city;officerId=officer;nodeId=node;startValue=value;previousSkill=skill;}
        public String label(){return "PK培养"+node(nodeId).label;}
    }
    final World w; final State[] states; final List<Training> training=new ArrayList<>();
    final SortedMap<Integer,int[]> gains=new TreeMap<>();
    AbilityResearch(World w){this.w=w;states=new State[w.factions.length];initialize(311);}
    /** Dedicated saved setup; previews do not draw from the gameplay random generator. */
    void initialize(long seed){
        String[] slots={"war_a","war_b","war_c","lead_a","lead_b","int_a","int_b","pol_a","pol_b","pol_c"};
        for(int side=0;side<states.length;side++){
            State s=new State();states[side]=s;Random rng=new Random(seed+side*1000003L);
            List<String> shuffled=new ArrayList<>(Arrays.asList(slots));Collections.shuffle(shuffled,rng);
            for(String slot:shuffled.subList(0,5))s.hidden.add("hidden."+slot+"."+rng.nextInt(5));
        }
    }
    public static Node node(String id){return CATALOG.get(id);}
    public static List<Node> catalog(){return Collections.unmodifiableList(new ArrayList<>(CATALOG.values()));}
    private boolean validSide(int side){return side>=0&&side<states.length;}
    boolean selected(int side,Node n){return validSide(side)&&n!=null&&(n.slot.isEmpty()||states[side].hidden.contains(n.id));}
    public boolean learned(int side,String id){return validSide(side)&&states[side].learned.contains(id);}
    public boolean unlocked(int side,Node n){return selected(side,n)&&states[side].learned.containsAll(n.prerequisites);}
    public List<Node> visible(int side){List<Node> result=new ArrayList<>();if(validSide(side))for(Node n:CATALOG.values())if(n.slot.isEmpty()||selected(side,n)&&unlocked(side,n))result.add(n);return Collections.unmodifiableList(result);}
    public Research research(int side){return validSide(side)?states[side].research:null;}
    public List<Training> training(){return Collections.unmodifiableList(training);}
    public int remaining(int side,String id){Node n=node(id);return !learned(side,id)||n==null?0:n.uses-states[side].used.getOrDefault(id,0);}
    public int gained(int officer,int attribute){return attribute<0||attribute>=5?0:gains.getOrDefault(officer,new int[5])[attribute];}
    private String commandError(int city,int gold){
        if(w.contests.busy())return "请先完成当前单挑或舌战";
        if(w.gameOver())return "本局已结束";World.City c=w.city(city);
        if(c==null||c.owner!=w.active)return "请选择己方城池";
        if(w.actionPoints[w.active]<20)return "行动力不足20";
        return c.gold<gold?"金不足":null;
    }
    public String researchError(int city,String id){
        String error=commandError(city,300);if(error!=null)return error;int side=w.city(city).owner;Node n=node(id);
        if(!selected(side,n))return "研究项目不存在";
        if(learned(side,id))return "能力已经研究完成";
        if(research(side)!=null)return "本势力已有进行中的能力研究";
        if(!unlocked(side,n)){StringJoiner required=new StringJoiner("、");for(String p:n.prerequisites)if(!learned(side,p))required.add(node(p).label);return "需先完成："+required;}
        return null;
    }
    public World.Result startResearch(int city,String id){
        String error=researchError(city,id);if(error!=null)return w.fail(error);
        World.City c=w.city(city);Node n=node(id);c.gold-=300;w.actionPoints[c.owner]-=20;
        states[c.owner].research=new Research(city,id,n.turns);
        return w.success(w.faction(c.owner)+"开始研究"+n.label+"，需要"+n.turns+"旬");
    }
    public World.Result cancelResearch(int side){
        if(w.contests.busy()||w.gameOver()||side!=w.active||research(side)==null)return w.fail("没有可中止的本势力能力研究");
        states[side].research=null;return w.success("能力研究已中止，金与行动力不退还");
    }
    public static boolean skillAvailable(Skill skill){
        // These require systems not present yet; do not silently teach a no-op skill.
        return !EnumSet.of(Skill.ZHUCHENG,Skill.TIEBI,Skill.WEIYA,Skill.LUNKE,Skill.JIJIAO,Skill.DUNZOU,Skill.GUIMEN,Skill.FENGSHUI,Skill.QIYUAN,Skill.TUNTIAN).contains(skill);
    }
    public int value(int officer,Node n){return n==null?0:w.campaign.studyValue(officer,Campaign.Study.values()[n.category==Category.APTITUDE?n.index+5:Math.max(0,n.index)]);}
    public String trainingError(int city,int officer,String id,boolean overwrite){
        String error=commandError(city,0);if(error!=null)return error;World.City c=w.city(city);World.Officer o=w.officer(officer);Node n=node(id);
        if(!w.idle(c).contains(o))return "需要同城未行动且空闲的武将";
        if(!selected(c.owner,n)||!learned(c.owner,id))return "请先研究该能力";
        if(remaining(c.owner,id)<=0)return "该能力的培养次数已用尽";
        for(Training t:training)if(t.owner==c.owner&&node(t.nodeId).category==n.category)return "本势力已有"+n.category.label+"培养，须等其结束";
        for(Campaign.Project p:w.campaign.projects())if(p.owner==c.owner&&p.study!=null&&(p.study.index<5?Category.STAT:Category.APTITUDE)==n.category)return "旧版同类培养尚未结束";
        if(n.category==Category.STAT){if(value(officer,n)>=n.cap)return "已达到本档能力上限";if(gained(officer,n.index)>=20)return "该武将此项能力的研究成长已达20";}
        if(n.category==Category.APTITUDE&&value(officer,n)!=n.cap-1)return "该培养仅适用于"+War.rankLabel(n.cap-1)+"级适性";
        if(n.category==Category.SKILL){if(!skillAvailable(n.skill))return "当前版本暂不可培养此特技";if(n.skill.id.equals(o.skillId))return "已经拥有该特技";if(!"none".equals(o.skillId)&&!overwrite)return "请确认覆盖原有特技";}
        return null;
    }
    public World.Result train(int city,int officer,String id,boolean overwrite){
        String error=trainingError(city,officer,id,overwrite);if(error!=null)return w.fail(error);
        Node n=node(id);World.Officer o=w.officer(officer);Training t=new Training(o.owner,city,officer,id,value(officer,n),o.skillId);
        w.actionPoints[o.owner]-=20;o.acted=true;o.otherTask=t.label();o.otherTaskTurns=3;training.add(t);
        return w.success(o.name+"开始"+t.label()+"，3旬后完成");
    }
    public World.Result cancelTraining(int officer){
        Training t=training.stream().filter(x->x.officerId==officer).findFirst().orElse(null);
        if(w.contests.busy()||w.gameOver()||t==null||t.owner!=w.active)return w.fail("没有可中止的本势力培养");
        release(t);return w.success("培养已中止，保留未完成的培养次数，行动力不退还");
    }
    private void release(Training t){training.remove(t);World.Officer o=w.officer(t.officerId);if(o!=null&&o.otherTask.equals(t.label())){o.otherTask="";o.otherTaskTurns=0;o.acted=true;}}
    private boolean valid(Training t){World.City c=w.city(t.cityId);World.Officer o=w.officer(t.officerId);return c!=null&&o!=null&&c.owner==t.owner&&o.owner==t.owner&&o.cityId==c.id&&o.unitId<0&&!w.government.captive(o.id)&&o.otherTask.equals(t.label())&&o.otherTaskTurns>0;}
    void cleanup(){
        for(int side=0;side<states.length;side++){Research r=research(side);if(r!=null&&(w.city(r.cityId)==null||w.city(r.cityId).owner!=side||!w.alive(side))){states[side].research=null;w.note("能力研究因城池失守或势力覆灭而中止");}}
        for(Training t:new ArrayList<>(training))if(!valid(t)){release(t);w.note("PK培养因武将或城池归属变化中止");}
    }
    void tick(){
        cleanup();
        for(int side=0;side<states.length;side++){Research r=research(side);if(r!=null&&--r.remaining==0){states[side].learned.add(r.nodeId);states[side].research=null;w.note(w.faction(side)+"完成能力研究："+node(r.nodeId).label);}}
        for(Training t:new ArrayList<>(training))if(w.officer(t.officerId).otherTaskTurns==1){
            Node n=node(t.nodeId);World.Officer o=w.officer(t.officerId);
            if(n.category==Category.SKILL&&!o.skillId.equals(t.previousSkill)||n.category!=Category.SKILL&&value(o.id,n)!=t.startValue){release(t);w.note("培养目标发生变化，任务中止并保留次数");continue;}
            if(n.category==Category.SKILL)o.skillId=n.skill.id;
            else if(n.category==Category.APTITUDE)o.aptitude[n.index]=n.cap;
            else {int after=Math.min(n.cap,t.startValue+5);setStat(o,n.index,after);gains.computeIfAbsent(o.id,k->new int[5])[n.index]+=after-t.startValue;}
            State state=states[t.owner];state.used.put(n.id,state.used.getOrDefault(n.id,0)+1);release(t);w.note(o.name+"完成"+t.label());
        }
    }
    static void setStat(World.Officer o,int index,int value){switch(index){case 0:o.leadership=value;break;case 1:o.war=value;break;case 2:o.intelligence=value;break;case 3:o.politics=value;break;case 4:o.charm=value;break;default:throw new IllegalArgumentException();}}
    void runAi(){
        if(w.officers.stream().noneMatch(o->o.owner==w.active&&!w.government.captive(o.id)))return;
        for(World.City c:w.cities)if(c.owner==w.active&&c.gold>=2000){
            if(research(c.owner)==null)for(Node n:visible(c.owner))if(researchError(c.id,n.id)==null){startResearch(c.id,n.id);break;}
            for(Node n:visible(c.owner))if(learned(c.owner,n.id))for(World.Officer o:w.idle(c))if(trainingError(c.id,o.id,n.id,false)==null){train(c.id,o.id,n.id,false);break;}
        }
    }
}
