package game.sanguo.core;

import java.util.*;

/** Playable campaign rules. Costs, probabilities and effects are engineering values, not verified SAN11 formulas. */
public final class Campaign {
    public enum TreatyKind { CEASEFIRE("停战"), ALLIANCE("同盟");
        public final String label; TreatyKind(String label){this.label=label;}
    }
    public enum Tech {
        SPEAR_DRILL("枪兵锻炼",300,1000,3,null,"枪兵战法伤害 +10%"),
        HALBERD_DRILL("戟兵锻炼",300,1000,3,null,"戟兵受到伤害 -10%"),
        CROSSBOW_DRILL("弩兵锻炼",300,1000,3,null,"弩兵战法伤害 +10%"),
        CAVALRY_DRILL("骑兵锻炼",300,1000,3,null,"骑兵战法伤害 +10%"),
        SUPPLY_RAID("兵粮袭击",500,1500,4,SPEAR_DRILL,"枪兵成功战法夺取目标最多1000粮"),
        SHIELD("矢盾",500,1500,4,HALBERD_DRILL,"戟兵受到弩兵伤害额外 -20%"),
        STRONG_BOW("强弩",700,2000,5,CROSSBOW_DRILL,"弩兵普通攻击与战法射程 +1"),
        HORSE_BREEDING("良马产出",500,1500,4,CAVALRY_DRILL,"骑兵每旬移动 +1"),
        ENGINEERING("工兵育成",400,1200,3,null,"城防修复效果 +50%"),
        WALLS("城壁强化",600,1800,4,ENGINEERING,"攻城受到的城防伤害 -20%"),
        FIRE_MASTERY("神火计",600,1800,4,null,"火计与火场伤害 +30%"),
        LOGISTICS("熟练兵",400,1200,3,null,"野战部队每旬耗粮 -20%"),
        WOODEN_BEAST("开发木兽",800,2000,4,ENGINEERING,"工房可制造木兽"),
        CATAPULT("开发投石",1000,2500,5,ENGINEERING,"工房可制造投石"),
        WARSHIP("开发斗舰",800,2000,4,ENGINEERING,"造船厂可制造斗舰");
        public final String label,effect; public final int points,gold,turns; public final Tech prerequisite;
        Tech(String label,int points,int gold,int turns,Tech prerequisite,String effect){this.label=label;this.points=points;this.gold=gold;this.turns=turns;this.prerequisite=prerequisite;this.effect=effect;}
    }
    public enum Study {
        LEADERSHIP("统率",0), WAR("武力",1), INTELLIGENCE("智力",2), POLITICS("政治",3), CHARM("魅力",4),
        SPEAR("枪兵适性",5), HALBERD("戟兵适性",6), CROSSBOW("弩兵适性",7), CAVALRY("骑兵适性",8), SIEGE("兵器适性",9), NAVY("水军适性",10);
        public final String label; final int index; Study(String label,int index){this.label=label;this.index=index;}
    }
    public static final class Treaty {
        public final int a,b; public final TreatyKind kind; public final int expires;
        Treaty(int a,int b,TreatyKind kind,int expires){this.a=Math.min(a,b);this.b=Math.max(a,b);this.kind=kind;this.expires=expires;}
    }
    public static final class Project {
        public final int owner,cityId,officerId; public final Tech tech; public final Study study;
        Project(int owner,int city,int officer,Tech tech,Study study){this.owner=owner;cityId=city;officerId=officer;this.tech=tech;this.study=study;}
        public String label(){return tech!=null?"研究"+tech.label:"培养"+study.label;}
    }
    final World w;
    final List<Treaty> treaties=new ArrayList<>();
    final List<Project> projects=new ArrayList<>();
    final Map<Integer,EnumSet<Tech>> learned=new TreeMap<>();
    final Map<Integer,Integer> points=new TreeMap<>(),traded=new TreeMap<>();
    Campaign(World w){this.w=w;}
    public List<Project> projects(){return Collections.unmodifiableList(projects);}
    public int points(int side){return points.getOrDefault(side,0);}
    void earn(int side,int amount){if(side>=0&&side<w.factions.length&&amount>0)points.put(side,Math.min(100000,points(side)+amount));}
    public boolean has(int side,Tech tech){return learned.getOrDefault(side,EnumSet.noneOf(Tech.class)).contains(tech);}
    public Treaty treaty(int a,int b){for(Treaty t:treaties)if(t.a==Math.min(a,b)&&t.b==Math.max(a,b)&&t.expires>w.turn)return t;return null;}
    public boolean hostile(int a,int b){return a!=b&&(a<0||b<0||treaty(a,b)==null);}
    public String relationLabel(int a,int b){if(a==b)return "本势力";if(a<0||b<0)return "未占领";Treaty t=treaty(a,b);return (t==null?"交战":t.kind.label+" · 剩"+(t.expires-w.turn)+"旬")+" · 关系 "+w.strategy.factionRelation(a,b);}
    private String foreignError(World.City c,World.Officer o,int side,int cost){
        String error=w.cityError(c,o,cost);if(error!=null)return error;
        return side<0||side>=w.factions.length||side==w.active||!w.alive(side)?"请选择另一个存活势力":null;
    }
    private void relation(int a,int b,int change){w.strategy.setFactionRelation(a,b,Math.max(-100,Math.min(100,w.strategy.factionRelation(a,b)+change)));}
    public World.Result goodwill(int city,int officer,int side){
        World.City c=w.city(city);World.Officer o=w.officer(officer);String error=foreignError(c,o,side,500);if(error!=null)return w.fail(error);
        if(w.strategy.factionRelation(c.owner,side)>=100)return w.fail("双方关系已达上限");
        w.spend(c,o,500);relation(c.owner,side,15+o.politics/10);earn(c.owner,20);
        return w.success(o.name+"出使"+w.faction(side)+"，双方关系改善");
    }
    public int treatyChance(int officer,int side,TreatyKind kind){
        World.Officer o=w.officer(officer);if(o==null||side<0||side>=w.factions.length||side==o.owner||kind==null)return 0;
        return Math.max(10,Math.min(95,35+o.politics/3+w.strategy.factionRelation(o.owner,side)/3+(kind==TreatyKind.CEASEFIRE?15:0)));
    }
    public World.Result negotiate(int city,int officer,int side,TreatyKind kind,int turns){
        World.City c=w.city(city);World.Officer o=w.officer(officer);String error=foreignError(c,o,side,1000);if(error!=null)return w.fail(error);
        if(kind==null||(turns!=3&&turns!=6&&turns!=12))return w.fail("协定类型或期限无效");
        if(treaty(c.owner,side)!=null)return w.fail("双方已有有效协定");
        if(kind==TreatyKind.ALLIANCE&&w.strategy.factionRelation(c.owner,side)<20)return w.fail("同盟需要双方关系至少20");
        int chance=treatyChance(officer,side,kind);w.spend(c,o,1000);boolean accepted=w.strategy.nextInt(100)<chance;
        if(accepted){treaties.removeIf(t->t.a==Math.min(c.owner,side)&&t.b==Math.max(c.owner,side));treaties.add(new Treaty(c.owner,side,kind,w.turn+turns));relation(c.owner,side,10);earn(c.owner,50);}
        return w.success(w.faction(side)+(accepted?"接受"+turns+"旬"+kind.label:"拒绝"+kind.label+"提议，出使费用已消耗"));
    }
    public World.Result breakTreaty(int city,int officer,int side){
        World.City c=w.city(city);World.Officer o=w.officer(officer);String error=foreignError(c,o,side,0);if(error!=null)return w.fail(error);
        Treaty t=treaty(c.owner,side);if(t==null)return w.fail("没有可解除的协定");
        w.spend(c,o,0);treaties.remove(t);relation(c.owner,side,-50);
        for(int other=0;other<w.factions.length;other++)if(other!=c.owner&&other!=side&&w.alive(other))relation(c.owner,other,-10);
        return w.success("与"+w.faction(side)+"解除协定，关系和对外信任下降");
    }
    public int rumorChance(int officer,int targetCity){
        World.Officer o=w.officer(officer);World.City c=w.city(targetCity);if(o==null||c==null)return 0;
        int defense=40;for(World.Officer guard:w.officers)if(guard.owner==c.owner&&guard.cityId==c.id)defense=Math.max(defense,guard.intelligence);
        return Math.max(10,Math.min(90,60+(o.intelligence-defense)/2));
    }
    public World.Result rumor(int city,int officer,int targetCity){
        World.City c=w.city(city),target=w.city(targetCity);World.Officer o=w.officer(officer);String error=w.cityError(c,o,300);if(error!=null)return w.fail(error);
        if(target==null||target.owner<0||!hostile(c.owner,target.owner))return w.fail("请选择交战势力的城池");
        if(c.hex.distance(target.hex)>12)return w.fail("目标超出流言范围12格");
        int chance=rumorChance(officer,targetCity);w.spend(c,o,300);boolean success=w.strategy.nextInt(100)<chance;relation(c.owner,target.owner,-5);
        if(success){target.order=Math.max(0,target.order-10);for(World.Officer t:w.officers)if(t.cityId==target.id&&t.owner==target.owner&&t.role!=Strategy.Role.RULER)t.loyalty=Math.max(0,t.loyalty-5);earn(c.owner,30);}
        return w.success(o.name+"在"+target.name+"散布流言"+(success?"，治安与武将忠诚下降":"，被识破"));
    }
    /** Same-turn ask/bid spread and per-city volume prevent profitable round-trip trading. */
    public int foodPrice(int city,boolean buy){
        World.City c=w.city(city);if(c==null)return 0;
        int month=(w.startMonth-1+w.turn/3)%12;
        int ask=100+((month/3+c.id%3)%4)*20;return buy?ask:ask*4/5;
    }
    public int traded(int city){return traded.getOrDefault(city,0);}
    public World.Result trade(int city,int officer,boolean buy,int food){
        World.City c=w.city(city);World.Officer o=w.officer(officer);
        if(food<1000||food>20000||food%1000!=0)return w.fail("交易量须为1000至20000的整千粮");
        int price=food/1000*foodPrice(city,buy);String error=w.cityError(c,o,buy?price:0);if(error!=null)return w.fail(error);
        if(traded(city)+food>20000)return w.fail("本城商人本旬交易量上限20000粮");
        if(buy?c.food>1000000-food:c.food<food||c.gold>1000000-price)return w.fail("粮草不足或库存容量不足");
        w.spend(c,o,buy?price:0);if(buy)c.food+=food;else{c.food-=food;c.gold+=price;}traded.put(city,traded(city)+food);
        return w.success(c.name+(buy?"买入":"卖出")+food+"粮，"+(buy?"支出":"收入")+price+"金");
    }
    public String researchError(int city,int officer,Tech tech){
        if(tech==null)return "技巧无效";
        World.City c=w.city(city);World.Officer o=w.officer(officer);String error=w.cityError(c,o,w.skills.researchGold(officer,tech));if(error!=null)return error;
        if(has(c.owner,tech))return "该技巧已经掌握";
        if(tech.prerequisite!=null&&!has(c.owner,tech.prerequisite))return "需要前置技巧："+tech.prerequisite.label;
        if(points(c.owner)<tech.points)return "技巧点不足";
        for(Project p:projects)if(p.owner==c.owner&&p.tech!=null)return "本势力已有进行中的技巧研究";
        return null;
    }
    public World.Result research(int city,int officer,Tech tech){
        String error=researchError(city,officer,tech);if(error!=null)return w.fail(error);
        World.City c=w.city(city);World.Officer o=w.officer(officer);w.spend(c,o,w.skills.researchGold(officer,tech));points.put(c.owner,points(c.owner)-tech.points);
        Project project=new Project(c.owner,city,officer,tech,null);projects.add(project);o.otherTask=project.label();o.otherTaskTurns=tech.turns;
        return w.success(o.name+"开始"+project.label()+"，需要"+tech.turns+"旬");
    }
    public int researchGold(int officer,Tech tech){return w.skills.researchGold(officer,tech);}
    public World.Result cancelProject(int officer){
        Project p=projects.stream().filter(x->x.officerId==officer).findFirst().orElse(null);
        if(w.contests.busy()||w.gameOver()||p==null||p.owner!=w.active)return w.fail("请选择本势力研究或培养任务");
        projects.remove(p);World.Officer o=w.officer(officer);o.otherTask="";o.otherTaskTurns=0;o.acted=true;
        return w.success(p.label()+"已中止，费用不退还");
    }
    public int studyValue(int officer,Study study){
        World.Officer o=w.officer(officer);if(o==null||study==null)return 0;
        switch(study){case LEADERSHIP:return o.leadership;case WAR:return o.war;case INTELLIGENCE:return o.intelligence;case POLITICS:return o.politics;case CHARM:return o.charm;default:return o.aptitude[study.index-5];}
    }
    public World.Result study(int city,int officer,Study study){
        World.City c=w.city(city);World.Officer o=w.officer(officer);String error=w.cityError(c,o,600);if(error!=null)return w.fail(error);
        if(study==null||studyValue(officer,study)>=(study.index<5?100:3))return w.fail("培养项目无效或已达上限");
        Project p=new Project(c.owner,city,officer,null,study);w.spend(c,o,600);projects.add(p);o.otherTask=p.label();o.otherTaskTurns=3;
        return w.success(o.name+"开始"+p.label()+"，需要3旬");
    }
    public World.Result repair(int city,int officer){
        World.City c=w.city(city);World.Officer o=w.officer(officer);String error=w.cityError(c,o,300);if(error!=null)return w.fail(error);
        if(c.defense>=3000)return w.fail("城防已达到修复上限3000");
        int amount=400+o.politics*4;if(has(c.owner,Tech.ENGINEERING))amount=amount*3/2;amount=Math.min(3000-c.defense,amount);
        w.spend(c,o,300);c.defense+=amount;earn(c.owner,20);return w.success(c.name+"修复城防"+amount);
    }
    public World.Result dismiss(int city,int officer,int target){
        World.City c=w.city(city);World.Officer o=w.officer(officer),t=w.officer(target);String error=w.cityError(c,o,0);if(error!=null)return w.fail(error);
        if(t==null||t.id==o.id||t.owner!=c.owner||t.cityId!=city||t.unitId>=0||t.role==Strategy.Role.RULER||w.domestic.busy(t.id)||w.strategy.busy(t.id))return w.fail("不能流放君主、执行者或任务中的武将");
        w.spend(c,o,0);w.strategy.releaseGovernor(t.id);w.government.allegianceChanged(t.id);t.owner=-1;t.role=Strategy.Role.UNAFFILIATED;t.loyalty=0;t.acted=true;
        return w.success(t.name+"被流放，成为本城在野武将");
    }
    void cleanupProjects(){
        for(Project p:new ArrayList<>(projects)){
            World.Officer o=w.officer(p.officerId);World.City c=w.city(p.cityId);
            if(o==null||c==null||o.owner!=p.owner||c.owner!=p.owner||o.cityId!=c.id||o.otherTaskTurns<=0){
                projects.remove(p);if(o!=null&&o.otherTask.equals(p.label())){o.otherTask="";o.otherTaskTurns=0;}
                w.note(p.label()+"因武将或城池归属变化中止，不退还费用");
            }
        }
    }
    /** Called once before Strategy.tick decrements its single task clock. */
    void tick(){
        traded.clear();treaties.removeIf(t->{boolean expired=t.expires<=w.turn||!w.alive(t.a)||!w.alive(t.b);if(expired)w.note(w.faction(t.a)+"与"+w.faction(t.b)+"的"+t.kind.label+"结束");return expired;});
        cleanupProjects();
        for(Project p:new ArrayList<>(projects))if(w.officer(p.officerId).otherTaskTurns==1){
            World.Officer o=w.officer(p.officerId);
            if(p.tech!=null)learned.computeIfAbsent(p.owner,k->EnumSet.noneOf(Tech.class)).add(p.tech);
            else switch(p.study){
                case LEADERSHIP:o.leadership=Math.min(100,o.leadership+3);break;case WAR:o.war=Math.min(100,o.war+3);break;
                case INTELLIGENCE:o.intelligence=Math.min(100,o.intelligence+3);break;case POLITICS:o.politics=Math.min(100,o.politics+3);break;
                case CHARM:o.charm=Math.min(100,o.charm+3);break;default:o.aptitude[p.study.index-5]=Math.min(3,o.aptitude[p.study.index-5]+1);
            }
            projects.remove(p);w.note(o.name+"完成"+p.label());
        }
        for(int side=0;side<w.factions.length;side++)if(w.alive(side)){int count=0;for(World.City c:w.cities)if(c.owner==side)count++;earn(side,Math.min(100,count*10));}
    }
    void runAi(){
        if(w.turn<6)return;
        for(World.City c:w.cities)if(c.owner==w.active){
            List<World.Officer> idle=w.idle(c);if(idle.isEmpty())continue;World.Officer o=idle.get(0);
            if(c.defense<2000&&c.gold>=1000){repair(c.id,o.id);continue;}
            if(c.food<6000&&c.gold>=2000){trade(c.id,o.id,true,5000);continue;}
            if(c.gold>=5000)for(Tech tech:Tech.values())if(researchError(c.id,o.id,tech)==null){research(c.id,o.id,tech);break;}
        }
    }
}
