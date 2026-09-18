package game.sanguo.core;

import java.util.*;

/** Playable campaign rules. Costs, probabilities and effects are engineering values, not verified SAN11 formulas. */
public final class Campaign {
    public enum TreatyKind { CEASEFIRE("停战"), ALLIANCE("同盟");
        public final String label; TreatyKind(String label){this.label=label;}
    }
    /** The first 15 ordinals are the immutable v5-v10 wire format. New saves use names. */
    public enum Tech {
        SPEAR_DRILL("枪兵锻炼",0,1,"枪兵普攻与战法伤害增加10%"),
        HALBERD_DRILL("戟兵锻炼",1,1,"戟兵普攻与战法伤害增加10%"),
        CROSSBOW_DRILL("弩兵锻炼",2,1,"弩兵普攻与战法伤害增加10%"),
        CAVALRY_DRILL("骑兵锻炼",3,1,"骑兵普攻与战法伤害增加10%"),
        SUPPLY_RAID("兵粮袭击",0,2,"枪兵造成攻击伤害时夺取敌军兵粮"),
        SHIELD("矢盾",1,2,"戟兵30%概率阻挡间接普攻"),
        STRONG_BOW("强弩",2,3,"弩兵普攻与战法射程增加1格"),
        HORSE_BREEDING("良马产出",3,2,"骑兵移动力提高"),
        ENGINEERING("工兵育成",6,1,"据点自动补修提高至基础的2.5倍"),
        WALLS("城壁强化",6,3,"据点耐久上限增加3000；阵系升级为城塞"),
        FIRE_MASTERY("神火计",7,2,"火计基础范围扩至3格"),
        LOGISTICS("熟练兵",4,1,"部队与据点气力上限120"),
        WOODEN_BEAST("开发木兽",7,1,"可制造木兽"),
        CATAPULT("开发投石",5,3,"可制造投石、斗舰及建造投石台"),
        WARSHIP("开发斗舰",-1,0,"保留旧版独立研究；新局由开发投石解锁"),
        FOREST_AMBUSH("奇袭",0,3,"枪兵在森林发起普通攻击不受反击"),
        ELITE_SPEAR("精锐枪兵",0,4,"枪兵攻防、移动与伤害提高"),
        LARGE_SHIELD("大盾",1,3,"戟兵30%概率阻挡普通攻击；与矢盾不重复抽签"),
        ELITE_HALBERD("精锐戟兵",1,4,"戟兵攻防、移动与伤害提高"),
        RETURN_FIRE("应射",2,2,"弩兵受到射击时可在自身射程内反击"),
        ELITE_CROSSBOW("精锐弩兵",2,4,"弩兵攻防、移动与伤害提高"),
        MOUNTED_ARCHERY("骑射",3,3,"骑兵可进行2格弓攻击"),
        ELITE_CAVALRY("精锐骑兵",3,4,"骑兵攻防、移动与伤害提高"),
        DIFFICULT_MARCH("难所行军",4,2,"允许通过间道、浅滩；免疫栈道行军损失"),
        MILITARY_REFORM("军制改革",4,3,"主将统兵上限增加3000"),
        SIEGE_LADDERS("云梯",4,4,"普通陆军对据点伤害增加40%；兵器舰船增加20%"),
        AXLE("车轴强化",5,1,"陆上攻城兵器移动力提高"),
        STONE_BUILDING("石造建筑",5,2,"可建石兵八阵；土垒升级为石壁"),
        THUNDERBOLT("霹雳",5,4,"投石波及目标邻接格，可能误伤己方"),
        FACILITY_REINFORCEMENT("设施强化",6,2,"阵升级为砦、箭楼升级为连弩楼"),
        DEFENSE_REINFORCEMENT("防卫强化",6,4,"据点与阵系设施反击伤害翻倍"),
        GUNPOWDER("火药炼成",7,3,"火种、火球升级为火焰种、火焰球"),
        EXPLOSIVES("爆药炼成",7,4,"解锁业火陷阱，着火伤害额外提高"),
        WOODEN_OX("木牛流马",8,1,"运输任务移动加快"),
        PORT_EXPANSION("港关扩张",8,2,"港关金粮容量提高4倍，兵与兵装提高2倍"),
        ADMINISTRATION("政令整备",8,3,"降低征兵、流言造成的治安损失"),
        POPULAR_SUPPORT("人心掌握",8,4,"降低日常、流言造成的忠诚损失");
        public static final String[] BRANCHES={"枪兵","戟兵","弩兵","骑兵","练兵","发明","防卫","火攻","内政（PK）"};
        public final String label,effect; public final int branch,level,points,gold,turns;
        public Tech prerequisite;
        Tech(String label,int branch,int level,String effect){
            this.label=label;this.branch=branch;this.level=level;this.effect=effect;
            points=level==0?800:new int[]{1000,2000,3000,5000}[level-1];
            gold=level==0?2000:new int[]{1000,2000,5000,10000}[level-1];
            turns=level==0?4:level+2; // Engineering duration; executable timing is not yet calibrated.
        }
        static {for(Tech t:values())if(t.level>1)for(Tech prior:values())if(prior.branch==t.branch&&prior.level==t.level-1)t.prerequisite=prior;}
        public static List<Tech> branch(int branch){List<Tech> out=new ArrayList<>();for(Tech t:values())if(t.branch==branch)out.add(t);out.sort(Comparator.comparingInt(t->t.level));return Collections.unmodifiableList(out);}
        public static List<Tech> researchable(){List<Tech> out=new ArrayList<>();for(int i=0;i<BRANCHES.length;i++)out.addAll(branch(i));return Collections.unmodifiableList(out);}
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
    final Map<Integer,EnumSet<Tech>> legacyTechs=new TreeMap<>();
    final Map<Integer,Integer> points=new TreeMap<>(),traded=new TreeMap<>();
    Campaign(World w){this.w=w;}
    public List<Project> projects(){return Collections.unmodifiableList(projects);}
    public int energyCap(int side){return has(side,Tech.LOGISTICS)?120:100;}
    public int points(int side){return points.getOrDefault(side,0);}
    void earn(int side,int amount){if(side>=0&&side<w.factions.length&&amount>0)points.put(side,Math.min(100000,points(side)+amount));}
    public boolean has(int side,Tech tech){EnumSet<Tech> set=learned.getOrDefault(side,EnumSet.noneOf(Tech.class));return set.contains(tech)||tech==Tech.WARSHIP&&set.contains(Tech.CATAPULT);}
    boolean grandfathered(int side,Tech tech){return legacyTechs.getOrDefault(side,EnumSet.noneOf(Tech.class)).contains(tech);}
    public int defenseCap(World.City c){return Math.min(100000,c.baseDefense+(has(c.owner,Tech.WALLS)?3000:0));}
    public int goldCap(World.City c){return c.kind==World.SiteKind.CITY?1000000:has(c.owner,Tech.PORT_EXPANSION)?40000:10000;}
    public int foodCap(World.City c){return c.kind==World.SiteKind.CITY?1000000:has(c.owner,Tech.PORT_EXPANSION)?400000:100000;}
    public int troopCap(World.City c){return c.kind==World.SiteKind.CITY?100000:has(c.owner,Tech.PORT_EXPANSION)?60000:30000;}
    public int equipmentCap(World.City c,World.Weapon weapon){return Army.siegeWeapon(weapon)?100:troopCap(c);}
    public int orderLoss(int owner,int base){return has(owner,Tech.ADMINISTRATION)?(base+1)/2:base;}
    public int loyaltyLoss(int owner,int base){return has(owner,Tech.POPULAR_SUPPORT)?(base+1)/2:base;}
    public Tech elite(World.Unit u){return eliteAt(u,u.hex);}
    private Tech eliteAt(World.Unit u,Hex h){if(w.army.water(h)||u.weapon.ordinal()>3)return null;return new Tech[]{Tech.ELITE_SPEAR,Tech.ELITE_HALBERD,Tech.ELITE_CROSSBOW,Tech.ELITE_CAVALRY}[u.weapon.ordinal()];}
    public boolean eliteUnit(World.Unit u){return eliteUnitAt(u,u.hex);}
    boolean eliteUnitAt(World.Unit u,Hex h){Tech t=eliteAt(u,h);return t!=null&&has(u.owner,t);}
    int constructionDamage(World.Unit u,int amount){return has(u.owner,Tech.SIEGE_LADDERS)?amount*(w.army.water(u.hex)||Army.siegeWeapon(u.weapon)?120:140)/100:amount;}
    void finishTech(int owner,Tech tech){learned.computeIfAbsent(owner,k->EnumSet.noneOf(Tech.class)).add(tech);w.fieldworks.upgrade(owner);}

    public Treaty treaty(int a,int b){for(Treaty t:treaties)if(t.a==Math.min(a,b)&&t.b==Math.max(a,b)&&t.expires>w.turn)return t;return null;}
    public boolean hostile(int a,int b){return a!=b&&(a<0||b<0||treaty(a,b)==null);}
    public String relationLabel(int a,int b){if(a==b)return "本势力";if(a<0||b<0)return "未占领";Treaty t=treaty(a,b);return (t==null?"交战":t.kind.label+" · 剩"+(t.expires-w.turn)+"旬")+" · 关系 "+w.strategy.factionRelation(a,b);}
    private String foreignError(World.City c,World.Officer o,int side,int cost){
        String error=w.cityError(c,o,cost);if(error!=null)return error;
        return side<0||side>=w.factions.length||side==w.active||!w.alive(side)?"请选择另一个存活势力":null;
    }
    private void relation(int a,int b,int change){w.strategy.setFactionRelation(a,b,Math.max(-100,Math.min(100,w.strategy.factionRelation(a,b)+change)));}
    public World.Result goodwill(int city, int officer, int side) {
        World.City c = this.w.city(city);
        World.Officer o = this.w.officer(officer);
        String error = foreignError(c, o, side, 500);
        if (error != null) {
            return this.w.fail(error);
        }
        if (this.w.strategy.factionRelation(c.owner, side) >= 100) {
            return this.w.fail("双方关系已达上限");
        }
        if (!this.w.envoys.resolving(officer)) {
            return this.w.envoys.dispatch(Envoys.Kind.GOODWILL, city, officer, this.w.personnel.destination(side), side, 0, 0, 500, 0, 10);
        }
        this.w.spend(c, o, 500);
        relation(c.owner, side, (o.politics / 10) + 15);
        earn(c.owner, 20);
        return this.w.success(o.name + "出使" + this.w.faction(side) + "，双方关系改善");
    }
    public int treatyChance(int officer,int side,TreatyKind kind){
        World.Officer o=w.officer(officer);if(o==null||side<0||side>=w.factions.length||side==o.owner||kind==null)return 0;
        return Math.max(10,Math.min(95,35+o.politics/3+w.strategy.factionRelation(o.owner,side)/3+(kind==TreatyKind.CEASEFIRE?15:0)));
    }
    public World.Result negotiate(int city, int officer, int side, TreatyKind kind, int turns) {
        StringBuilder sb;
        StringBuilder sbAppend;
        World.City c = this.w.city(city);
        World.Officer o = this.w.officer(officer);
        String error = foreignError(c, o, side, 1000);
        if (error != null) {
            return this.w.fail(error);
        }
        if (kind != null && (turns == 3 || turns == 6 || turns == 12)) {
            if (treaty(c.owner, side) != null) {
                return this.w.fail("双方已有有效协定");
            }
            if (kind == TreatyKind.ALLIANCE && this.w.strategy.factionRelation(c.owner, side) < 20) {
                return this.w.fail("同盟需要双方关系至少20");
            }
            if (!this.w.envoys.resolving(officer)) {
                return this.w.envoys.dispatch(Envoys.Kind.TREATY, city, officer, this.w.personnel.destination(side), side, kind.ordinal(), turns, 1000, 0, 10);
            }
            int chance = treatyChance(officer, side, kind);
            this.w.spend(c, o, 1000);
            boolean accepted = this.w.strategy.nextInt(100) < chance;
            if (accepted) {
                concludeTreaty(c.owner, side, kind, turns);
            } else if (this.w.active == this.w.player && this.w.skills.has(o, Skill.LUNKE)) {
                return this.w.contests.diplomaticDebate(city, officer, side, kind, turns);
            }
            World world = this.w;
            String strFaction = this.w.faction(side);
            String str = kind.label;
            if (accepted) {
                sb = new StringBuilder();
                sbAppend = sb.append("接受").append(turns).append("旬").append(str);
            } else {
                sb = new StringBuilder();
                sbAppend = sb.append("拒绝").append(str).append("提议，出使费用已消耗");
            }
            return world.success(strFaction + sbAppend.toString());
        }
        return this.w.fail("协定类型或期限无效");
    }
    void concludeTreaty(int owner,int side,TreatyKind kind,int turns){
        treaties.removeIf(t->t.a==Math.min(owner,side)&&t.b==Math.max(owner,side));treaties.add(new Treaty(owner,side,kind,w.turn+turns));relation(owner,side,10);earn(owner,50);w.districts.cleanup();
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
    public World.Result rumor(int city, int officer, int targetCity) {
        World.City c = this.w.city(city);
        World.City target = this.w.city(targetCity);
        World.Officer o = this.w.officer(officer);
        String error = this.w.cityError(c, o, Strategy.RECRUIT_COST);
        if (error != null) {
            return this.w.fail(error);
        }
        if (target == null || target.owner < 0 || !hostile(c.owner, target.owner)) {
            return this.w.fail("请选择交战势力的城池");
        }
        if (!this.w.envoys.resolving(officer)) {
            return this.w.envoys.dispatch(Envoys.Kind.RUMOR, city, officer, targetCity, targetCity, 0, 0, Strategy.RECRUIT_COST, 0, 10);
        }
        int chance = rumorChance(officer, targetCity);
        this.w.spend(c, o, Strategy.RECRUIT_COST);
        boolean success = this.w.strategy.nextInt(100) < chance;
        relation(c.owner, target.owner, -5);
        if (success) {
            target.order = Math.max(0, target.order - orderLoss(target.owner, 10));
            for (World.Officer t : this.w.officers) {
                if (t.cityId == target.id && t.owner == target.owner && t.role != Strategy.Role.RULER && !this.w.relations.loyalBond(t.id)) {
                    t.loyalty = Math.max(0, t.loyalty - loyaltyLoss(t.owner, 5));
                }
            }
            earn(c.owner, 30);
        }
        return this.w.success(o.name + "在" + target.name + "散布流言" + (success ? "，治安与武将忠诚下降" : "，被识破"));
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
        if(buy?c.food>w.campaign.foodCap(c)-food:c.food<food||c.gold>w.campaign.goldCap(c)-price)return w.fail("粮草不足或库存容量不足");
        w.spend(c,o,buy?price:0);if(buy)c.food+=food;else{c.food-=food;c.gold+=price;}traded.put(city,traded(city)+food);
        return w.success(c.name+(buy?"买入":"卖出")+food+"粮，"+(buy?"支出":"收入")+price+"金");
    }
    public String researchError(int city,int officer,Tech tech){
        if(tech==null)return "技巧无效";
        if(tech==Tech.WARSHIP)return "斗舰由开发投石解锁，不再单独研究";
        World.City c=w.city(city);World.Officer o=w.officer(officer);String error=w.cityError(c,o,w.skills.researchGold(officer,tech));if(error!=null)return error;
        if(c.kind!=World.SiteKind.CITY)return "技巧研究需要城市";
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
        if(w.commandsBlocked()||w.gameOver()||p==null||p.owner!=w.active)return w.fail("请选择本势力研究或培养任务");
        projects.remove(p);if(p.tech!=null)legacyTechs.getOrDefault(p.owner,EnumSet.noneOf(Tech.class)).remove(p.tech);World.Officer o=w.officer(officer);o.otherTask="";o.otherTaskTurns=0;o.acted=true;
        return w.success(p.label()+"已中止，费用不退还");
    }
    public int studyValue(int officer,Study study){
        World.Officer o=w.officer(officer);if(o==null||study==null)return 0;
        switch(study){case LEADERSHIP:return o.leadership;case WAR:return o.war;case INTELLIGENCE:return o.intelligence;case POLITICS:return o.politics;case CHARM:return o.charm;default:return o.aptitude[study.index-5];}
    }
    public World.Result study(int city,int officer,Study study){
        if(study==null)return w.fail("培养项目无效");
        for(AbilityResearch.Node n:w.abilities.visible(w.active))if(n.category!=AbilityResearch.Category.SKILL&&
            (n.category==AbilityResearch.Category.STAT?n.index:n.index+5)==study.index&&w.abilities.trainingError(city,officer,n.id,false)==null)
            return w.abilities.train(city,officer,n.id,false);
        return w.fail("请先完成对应PK能力研究，并检查能力上限、培养次数和同类任务");
    }

    public World.Result repair(int city,int officer){
        World.City c=w.city(city);World.Officer o=w.officer(officer);String error=w.cityError(c,o,300);if(error!=null)return w.fail(error);
        if(c.defense>=defenseCap(c))return w.fail("城防已达到修复上限"+defenseCap(c));
        int amount=w.cityDefense.repairAmount(c,o);
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
                projects.remove(p);if(p.tech!=null)legacyTechs.getOrDefault(p.owner,EnumSet.noneOf(Tech.class)).remove(p.tech);if(o!=null&&o.otherTask.equals(p.label())){o.otherTask="";o.otherTaskTurns=0;}
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
            if(p.tech!=null)finishTech(p.owner,p.tech);
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
            if(c.gold>=5000)for(Tech tech:Tech.researchable())if(researchError(c.id,o.id,tech)==null){research(c.id,o.id,tech);break;}
        }
    }
}
