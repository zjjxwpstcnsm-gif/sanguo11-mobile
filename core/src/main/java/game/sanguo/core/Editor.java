package game.sanguo.core;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/** PK-style in-game edits, previewed against a validated copy and committed only to the same state. */
public final class Editor {
    public static final class Draft {
        public final String summary,error;
        private final byte[] before;
        private final Consumer<World> change;
        private Draft(String summary,String error,byte[] before,Consumer<World> change){this.summary=summary;this.error=error;this.before=before;this.change=change;}
        public boolean valid(){return error==null;}
    }
    public static final class Template {
        public final String name,skill;
        public final World.Sex sex;
        public final Debate.Temper temper;
        public final int talkMask;
        private final int[] stats,aptitudes;
        public Template(String name,int[] stats,int[] aptitudes,World.Sex sex,String skill,Debate.Temper temper,int talkMask){
            this.name=name;this.stats=stats==null?new int[0]:stats.clone();this.aptitudes=aptitudes==null?new int[0]:aptitudes.clone();this.sex=sex;this.skill=skill;this.temper=temper;this.talkMask=talkMask;
        }
        public int stat(int i){return stats[i];}public int aptitude(int i){return aptitudes[i];}
        public void validate(){
            if(name==null||name.trim().isEmpty()||name.length()>24||name.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("姓名需为1至24个有效字符");
            if(stats.length!=5||aptitudes.length!=6||sex==null||temper==null||talkMask<0||talkMask>31||!("none".equals(skill)||Skill.find(skill)!=null))throw new IllegalArgumentException("武将模板字段不完整或无效");
            for(int n:stats)range(n,0,100);for(int n:aptitudes)range(n,0,3);
        }
    }
    final SortedSet<Integer> customOfficers=new TreeSet<>();
    boolean edited;
    int revision;
    private final World w;
    Editor(World w){this.w=w;}
    public boolean edited(){return edited;}public int revision(){return revision;}
    public boolean custom(int officer){return customOfficers.contains(officer);}
    public Template template(int officer){
        World.Officer o=w.officer(officer);if(o==null)throw new IllegalArgumentException("武将不存在");Contests.Profile p=w.contests.profile(officer);
        return new Template(o.name,new int[]{o.leadership,o.war,o.intelligence,o.politics,o.charm},o.aptitude,o.sex,o.skillId,p.temper,p.talkMask);
    }
    private static String diff(World a,World b){
        StringBuilder s=new StringBuilder();
        for(World.Officer o:b.officers){World.Officer old=a.officer(o.id);if(old==null){s.append("\n新增：").append(o.name).append(" · ").append(b.faction(o.owner)).append(" · ").append(b.city(o.cityId).name);old=o;}
            int[] x={old.leadership,old.war,old.intelligence,old.politics,old.charm,old.loyalty,a.government.merit(o.id)},y={o.leadership,o.war,o.intelligence,o.politics,o.charm,o.loyalty,b.government.merit(o.id)};
            String[] labels={"统率","武力","智力","政治","魅力","忠诚","功绩"};for(int i=0;i<x.length;i++)delta(s,o.name+" "+labels[i],x[i],y[i]);
            for(int i=0;i<6;i++)delta(s,o.name+" "+new String[]{"枪","戟","弩","骑","器","水"}[i]+"适性",old.aptitude[i],o.aptitude[i]);
            if(!old.skillId.equals(o.skillId))s.append("\n特技：").append(Skill.label(old.skillId)).append(" → ").append(Skill.label(o.skillId));
            if(old.sex!=o.sex)s.append("\n性别：").append(old.sex).append(" → ").append(o.sex);
            Contests.Profile pa=a.contests.profile(o.id),pb=b.contests.profile(o.id);if(pa.temper!=pb.temper||pa.talkMask!=pb.talkMask)s.append("\n性格：").append(pa.temper.label).append(" → ").append(pb.temper.label).append("；话术：").append(pa.talkMask).append(" → ").append(pb.talkMask);
            if(a.officer(o.id)!=null&&!a.relations.describe(o.id).equals(b.relations.describe(o.id)))s.append("\n").append(o.name).append("关系：\n").append(a.relations.describe(o.id)).append("\n→\n").append(b.relations.describe(o.id));
        }
        for(World.City c:b.cities){World.City old=a.city(c.id);int[] x={old.gold,old.food,old.troops,old.order,old.morale,old.defense,old.recruitReserve},y={c.gold,c.food,c.troops,c.order,c.morale,c.defense,c.recruitReserve};String[] labels={"金","粮","兵","治安","气力","城防","兵源"};for(int i=0;i<x.length;i++)delta(s,c.name+labels[i],x[i],y[i]);for(int i=0;i<9;i++)delta(s,c.name+World.Weapon.values()[i].label,old.equipment[i],c.equipment[i]);for(int i=0;i<2;i++)delta(s,c.name+Army.Ship.values()[i+1].label,old.ships[i],c.ships[i]);}
        for(int side=0;side<b.factions.length;side++){delta(s,b.faction(side)+"行动力",a.actionPoints[side],b.actionPoints[side]);delta(s,b.faction(side)+"技巧点",a.campaign.points(side),b.campaign.points(side));for(Campaign.Tech t:Campaign.Tech.values())if(!a.campaign.has(side,t)&&b.campaign.has(side,t))s.append("\n解锁：").append(t.label);}
        for(World.Unit u:b.units){World.Unit old=a.unit(u.id);String name=b.officer(u.officerId).name;delta(s,name+"兵",old.troops,u.troops);delta(s,name+"粮",old.food,u.food);delta(s,name+"金",old.gold,u.gold);delta(s,name+"气力",old.energy,u.energy);if(old.status!=u.status||old.statusTurns!=u.statusTurns)s.append("\n状态：").append(old.status.label).append(old.statusTurns).append(" → ").append(u.status.label).append(u.statusTurns);}
        for(Treasures.Item i:b.treasures.items()){Treasures.Item old=a.treasures.item(i.definition.id);if(old==null||old.place!=i.place||old.holder!=i.holder)s.append("\n").append(i.definition.name).append("：").append(old==null?"未配置":a.treasures.location(old)).append(" → ").append(b.treasures.location(i));}
        return s.toString();
    }
    private static void delta(StringBuilder s,String label,int old,int value){if(old!=value)s.append("\n").append(label).append("：").append(old).append(" → ").append(value);}
    private Draft preview(String summary,Consumer<World> change){
        if(w.contests.busy()||w.active!=w.player)return new Draft(summary,"请先完成对局或等待本方行动",null,null);
        try{
            byte[] before=SaveCodec.encode(w);World copy=SaveCodec.decode(before);
            change.accept(copy);if(Arrays.equals(before,SaveCodec.encode(copy)))throw new IllegalArgumentException("没有发生数值或配置变化");
            summary+=diff(w,copy);copy.editor.edited=true;copy.editor.revision=Math.addExact(copy.editor.revision,1);SaveCodec.validate(copy);
            return new Draft(summary,null,before,change);
        }catch(IOException|IllegalArgumentException|ArithmeticException e){return new Draft(summary,e.getMessage(),null,null);}
    }
    public World.Result apply(Draft draft){
        if(draft==null||!draft.valid())return w.fail(draft==null?"没有编辑草稿":draft.error);
        try{if(!Arrays.equals(draft.before,SaveCodec.encode(w)))return w.fail("局面已变化，请重新预览");}catch(IOException e){return w.fail(e.getMessage());}
        draft.change.accept(w);edited=true;revision++;
        return w.success("PK编辑已应用 · "+draft.summary.substring(0,Math.min(1500,draft.summary.length())));
    }
    public Draft officer(int id,int[] stats,int[] aptitude,World.Sex sex,String skill,int loyalty,int merit,Debate.Temper temper,int talkMask){
        World.Officer original=w.officer(id);Template t=new Template(original==null?"?":original.name,stats,aptitude,sex,skill,temper,talkMask);
        return preview((original==null?"武将":original.name)+" · 能力/适性/特技/忠诚/功绩",v->{
            t.validate();World.Officer o=v.officer(id);if(o==null)throw new IllegalArgumentException("武将不存在");range(loyalty,0,100);range(merit,0,1000000);
            set(o,t);if(o.owner<0&&loyalty!=0)throw new IllegalArgumentException("在野武将忠诚应为0");o.loyalty=loyalty;
            v.government.merits.put(id,merit);Contests.Profile p=v.contests.profile(id);v.contests.configure(id,new Contests.Profile(temper,talkMask,v.contests.profiles.containsKey(id)?v.contests.profiles.get(id).gearMask:0));
        });
    }
    private static void set(World.Officer o,Template t){o.leadership=t.stat(0);o.war=t.stat(1);o.intelligence=t.stat(2);o.politics=t.stat(3);o.charm=t.stat(4);for(int i=0;i<6;i++)o.aptitude[i]=t.aptitude(i);o.sex=t.sex;o.skillId=t.skill;}
    public Draft city(int id,int gold,int food,int troops,int order,int morale,int defense,int reserve,int[] equipment,int[] ships){
        final int[] gear=equipment==null?new int[0]:equipment.clone(),fleet=ships==null?new int[0]:ships.clone();
        return preview("据点资源 · 金"+gold+" / 粮"+food+" / 兵"+troops,v->{
            World.City c=v.city(id);if(c==null||gear.length!=9||fleet.length!=2)throw new IllegalArgumentException("据点或兵装字段无效");
            range(gold,0,v.campaign.goldCap(c));range(food,0,v.campaign.foodCap(c));range(troops,0,v.campaign.troopCap(c));range(order,0,100);range(morale,0,v.campaign.energyCap(c.owner));range(defense,1,v.campaign.defenseCap(c));range(reserve,0,100000);
            for(int i=0;i<gear.length;i++)range(gear[i],0,v.campaign.equipmentCap(c,World.Weapon.values()[i]));for(int n:fleet)range(n,0,100);
            if(gear[World.Weapon.SWORD.ordinal()]!=0)throw new IllegalArgumentException("剑兵不持有独立兵装");
            c.gold=gold;c.food=food;c.troops=troops;c.order=order;c.morale=morale;c.defense=defense;c.recruitReserve=reserve;
            System.arraycopy(gear,0,c.equipment,0,9);System.arraycopy(fleet,0,c.ships,0,2);
        });
    }
    public Draft faction(int owner,int actionPoints,int techniquePoints){return preview("势力 · 行动力"+actionPoints+" / 技巧点"+techniquePoints,v->{
        range(owner,0,v.factions.length-1);range(actionPoints,0,60);range(techniquePoints,0,100000);v.actionPoints[owner]=actionPoints;v.campaign.points.put(owner,techniquePoints);
    });}
    public Draft learnTechnology(int owner,Campaign.Tech tech){return preview("势力技巧 · "+(tech==null?"?":tech.label)+"（含前置）",v->{
        range(owner,0,v.factions.length-1);if(tech==null||tech.level==0)throw new IllegalArgumentException("请选择36项技巧之一");
        for(Campaign.Project p:v.campaign.projects)if(p.owner==owner&&p.tech!=null)throw new IllegalArgumentException("请先结束或中止本势力技巧研究");
        learn(v,owner,tech);
    });}
    private static void learn(World v,int owner,Campaign.Tech tech){if(tech.prerequisite!=null)learn(v,owner,tech.prerequisite);v.campaign.finishTech(owner,tech);}
    public Draft unit(int id,int troops,int food,int gold,int energy,War.Status status,int statusTurns){return preview("部队 · 兵"+troops+" / 粮"+food+" / 金"+gold+" / 气力"+energy,v->{
        World.Unit u=v.unit(id);if(u==null||status==null)throw new IllegalArgumentException("部队或状态不存在");
        range(troops,1,18000);range(food,0,1000000);range(gold,0,10000);range(energy,0,v.campaign.energyCap(u.owner));range(statusTurns,0,3);
        if((status==War.Status.NORMAL)!=(statusTurns==0))throw new IllegalArgumentException("正常状态持续0旬，异常状态1至3旬");
        u.troops=troops;u.food=food;u.gold=gold;u.energy=energy;u.status=status;u.statusTurns=statusTurns;
    });}
    public Draft relation(int first,int second,Relations.Kind kind,boolean remove){return preview((remove?"移除":"设置")+"人物关系 · "+(kind==null?"?":kind.label),v->{
        if(kind==null)throw new IllegalArgumentException("请选择关系");
        if(remove){if(!v.relations.links(first,kind).contains(second))throw new IllegalArgumentException("关系不存在");v.relations.unlink(first,second,kind);}
        else {String error=v.relations.linkError(first,second,kind);if(error!=null)throw new IllegalArgumentException(error);v.relations.link(first,second,kind);}
    });}
    public Draft treasure(String id,Treasures.Place place,int holder){return preview("宝物配置 · "+id,v->{
        if(place==null)throw new IllegalArgumentException("请选择宝物位置");
        try{Treasures.Item old=v.treasures.item(id);v.treasures.place(old==null?Treasures.definition(id):old.definition,place,holder);}catch(IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
    });}
    public Draft createOfficer(Template t,int city,int owner){return preview("新武将 · "+(t==null?"?":t.name),v->{
        if(t==null)throw new IllegalArgumentException("模板不存在");t.validate();World.City c=v.city(city);range(owner,-1,v.factions.length-1);
        if(c==null||owner>=0&&c.owner!=owner)throw new IllegalArgumentException("请选择所属势力的据点，或以在野身份加入");
        if(v.officers.size()+v.strategy.talents.size()>=10000)throw new IllegalArgumentException("武将数量已达上限");
        int id=100000;for(World.Officer o:v.officers)id=Math.max(id,Math.addExact(o.id,1));for(Strategy.Talent o:v.strategy.talents)id=Math.max(id,Math.addExact(o.id,1));
        if(id>1000000)throw new IllegalArgumentException("新武将编号已达上限");
        World.Officer o=new World.Officer(id,t.name,owner,city,0,0,0,0,0);set(o,t);v.officers.add(o);v.editor.customOfficers.add(id);
        v.contests.configure(id,new Contests.Profile(t.temper,t.talkMask,0));
    });}
    void validate()throws IOException{
        if(revision<0||revision>1000000||(!edited&&revision!=0))throw new IOException("编辑状态无效");
        for(int id:customOfficers)if(w.officer(id)==null)throw new IOException("新武将引用缺失");
    }
    private static void range(int n,int lo,int hi){if(n<lo||n>hi)throw new IllegalArgumentException("数值需在"+lo+"至"+hi+"之间");}
}
