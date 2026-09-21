package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** New-game-only content composition. All gameplay remains in the ordinary World subsystems. */
public final class CustomOfficers {
    public static final String NAMESPACE="customOfficers";
    public static final int VERSION=1, MAX_PORTRAIT=256*1024, MAX_SNAPSHOT=8*1024*1024;
    public enum Mode { KEEP("保留历史初始身份"), FACTION("加入已有势力"), WILD("在野武将"), UNAPPEARED("未登场");
        public final String label;Mode(String label){this.label=label;}}
    public static final class Link {
        public final Relations.Kind kind;public final String target;
        public Link(Relations.Kind kind,String target){this.kind=Objects.requireNonNull(kind);this.target=Objects.requireNonNull(target);}
    }
    public static final class Definition {
        public String id,baseName="",portrait="";
        public int runtimeId,revision=1,targetId=-1,birth,appearance,death,affinity=75,honor=3;
        public Editor.Template template;
        public boolean replaceRelations;
        public final List<Link> links=new ArrayList<>();
        public byte[] portraitPng=new byte[0];
        public int effectiveId(){return targetId<0?runtimeId:targetId;}
        public void validate(){
            UUID.fromString(id);if(runtimeId<100000||runtimeId>1000000||revision<1||targetId< -1||targetId>1000000||template==null)throw new IllegalArgumentException("人物身份或模板无效");template.validate();if(!SkillSupport.enabled(template.skill))throw new IllegalArgumentException("特技尚未接入实际规则");
            if(affinity<0||affinity>149||honor<1||honor>5)throw new IllegalArgumentException("相性0—149，义理1—5");
            if(birth<0||birth>9999||appearance<0||appearance>9999||death<0||death>9999||birth>0&&(appearance>0&&birth>appearance||death>0&&birth>death)||appearance>0&&death>0&&appearance>death)throw new IllegalArgumentException("生卒/登场年份应为0（未知）或1—9999且顺序正确");
            if(targetId>=0&&baseName.isEmpty())throw new IllegalArgumentException("历史覆盖缺少目标身份指纹");
            if(!portrait.isEmpty()&&!portrait.matches("builtin:(?:[0-9]|[12][0-9]|3[01])|[a-f0-9]{64}\\.png"))throw new IllegalArgumentException("头像引用无效");
            if(portraitPng==null||portraitPng.length>MAX_PORTRAIT)throw new IllegalArgumentException("头像资源过大");
            if(links.size()>16)throw new IllegalArgumentException("人物关系数量超限");
            Set<String> seen=new HashSet<>();for(Link link:links){if(link.target.equals(id)||link.target.equals("h:"+effectiveId())||!seen.add(link.kind+":"+link.target))throw new IllegalArgumentException("关系自指或重复");if(!link.target.matches("h:[0-9]{1,7}"))UUID.fromString(link.target);}
        }
    }
    public static final class Placement {
        public final String definition;public final Mode mode;public final int owner,city,loyalty;public final boolean ignoreDates;
        public Placement(String definition,Mode mode,int owner,int city,int loyalty,boolean ignoreDates){this.definition=definition;this.mode=mode;this.owner=owner;this.city=city;this.loyalty=loyalty;this.ignoreDates=ignoreDates;}
    }
    private CustomOfficers(){}
    public static Definition historical(ContentCatalog.Officer o,boolean override)throws IOException{
        ContentCatalog catalog=ContentCatalog.get();Definition d=new Definition();d.id=UUID.randomUUID().toString();d.runtimeId=100000;
        int[] s=new int[5],a=new int[6];for(int i=0;i<5;i++)s[i]=o.stat(i);for(int i=0;i<6;i++)a[i]=o.aptitude(i);
        Skill skill="none".equals(o.skillId)?null:ContentRuntime.skill(o.skillId);
        d.template=new Editor.Template(o.name,s,a,"男".equals(o.gender)?World.Sex.MALE:World.Sex.FEMALE,skill==null?"none":skill.id,ContentProfiles.temper(o),0);
        d.targetId=override?o.id:-1;d.baseName=override?o.name:"";d.birth=o.birth;d.appearance=o.appearance;d.death=o.death;d.affinity=catalog.profile(o.id).affinity;d.honor=catalog.profile(o.id).honor;return d;
    }
    /** Validate the editable graph without loading a map or mutating any source definition. */
    public static void validateDefinitions(List<Definition> definitions)throws IOException{
        World graph=new World(3,3,new String[]{"校验甲","校验乙"});Map<String,Definition> ids=new HashMap<>();Set<Integer> nums=new HashSet<>();ContentCatalog catalog=ContentCatalog.get();
        for(ContentCatalog.Officer h:catalog.officers())graph.officers.add(new World.Officer(h.id,h.name,-1,-1,0,0,0,0,0));
        for(Definition d:definitions){d.validate();if(ids.put(d.id,d)!=null||!nums.add(d.runtimeId))throw new IllegalArgumentException("模板ID重复");
            if(d.targetId>=0){ContentCatalog.Officer h=catalog.officer(d.targetId);if(h==null||!h.name.equals(d.baseName))throw new IllegalArgumentException("历史覆盖目标身份不匹配");}
            // Separate graph vertices allow mutually exclusive versions of the same historical person in a library.
            if(graph.officer(d.runtimeId)!=null)throw new IllegalArgumentException("自定义ID与历史资料冲突");graph.officers.add(new World.Officer(d.runtimeId,d.template.name,-1,-1,0,0,0,0,0));}
        for(Relations.Kind kind:Relations.Kind.values())for(Definition d:definitions)for(Link l:d.links)if(l.kind==kind){
            int target;if(l.target.startsWith("h:")){target=Integer.parseInt(l.target.substring(2));if(catalog.officer(target)==null)throw new IllegalArgumentException("历史关系对象不存在："+l.target);}
            else{Definition t=ids.get(l.target);if(t==null)throw new IllegalArgumentException("缺失自定义关系对象："+l.target);target=t.runtimeId;}
            if(!graph.relations.links(d.runtimeId,kind).contains(target))link(graph,d.runtimeId,target,kind);
            if(kind==Relations.Kind.FATHER||kind==Relations.Kind.MOTHER){Definition other=ids.get(l.target);int parentBirth=other!=null?other.birth:catalog.officer(target).birth;if(d.birth>0&&parentBirth>0&&parentBirth>=d.birth)throw new IllegalArgumentException("父母出生年必须早于子女："+d.template.name);}
        }
        graph.relations.validate();
    }
    /** Called AFTER the effective map/sites have been resolved, BEFORE activating/saving the new campaign. */
    public static World apply(World resolved,List<Definition> definitions,List<Placement> placements,byte[] config)throws IOException{
        if(placements.isEmpty())return resolved;
        if(resolved.turn!=0||!resolved.units.isEmpty()||resolved.extensions.get(NAMESPACE)!=null)throw new IOException("自定义人物只能在尚未启用的新战局初始化");
        try{
            validateDefinitions(definitions);World w=SaveCodec.decode(SaveCodec.encode(resolved));Map<String,Definition> defs=new HashMap<>();for(Definition d:definitions)defs.put(d.id,d);
            Map<String,Integer> active=new LinkedHashMap<>();Set<Integer> targets=new HashSet<>();List<Definition> chosen=new ArrayList<>();
            for(Placement p:placements){Definition d=defs.get(p.definition);if(d==null||p.mode==null)throw new IllegalArgumentException("投放引用的模板不存在");if(active.put(d.id,d.effectiveId())!=null||!targets.add(d.effectiveId()))throw new IllegalArgumentException("同一人物被重复投放/多个历史版本同时覆盖，请明确选择一个版本");chosen.add(d);
                World.Officer old=d.targetId<0?null:w.officer(d.targetId);World.City city=w.city(p.city);
                if(d.targetId>=0&&(old==null||!ContentProfiles.matches(w,ContentCatalog.get(),d.targetId)))throw new IllegalArgumentException(d.template.name+"：覆盖目标未在本剧本出现或身份不符");
                if(d.targetId<0&&(w.officer(d.runtimeId)!=null||w.strategy.talents.stream().anyMatch(t->t.id==d.runtimeId)))throw new IllegalArgumentException("人物数字ID与现有武将冲突，请重新分配模板ID");
                if(p.mode==Mode.KEEP&&old==null)throw new IllegalArgumentException("新武将不能保留不存在的历史身份");
                if(old!=null&&p.mode!=Mode.KEEP)throw new IllegalArgumentException("历史覆盖保留原始职务和归属，不能通过投放替换君主/军师/太守");
                int owner=p.mode==Mode.KEEP?old.owner:p.mode==Mode.FACTION?p.owner:-1;
                int home=p.mode==Mode.KEEP?(old.cityId>=0?old.cityId:w.life.life(old.id)==null?-1:w.life.life(old.id).home):p.city;
                city=w.city(home);if(city==null)throw new IllegalArgumentException(d.template.name+"：驻地ID "+home+" 在实际启用地图中不存在，请重选驻地");
                if(owner< -1||owner>=w.factions.length||owner>=0&&(city.owner!=owner||!w.alive(owner)))throw new IllegalArgumentException(d.template.name+"：所属势力与驻地不匹配");
                if(p.loyalty<0||p.loyalty>100)throw new IllegalArgumentException("初始忠诚范围0—100");
                boolean unappeared=p.mode==Mode.UNAPPEARED||p.mode==Mode.KEEP&&w.life.state(old.id)==Lifecycle.State.UNAPPEARED;
                if(p.ignoreDates&&unappeared)throw new IllegalArgumentException("未登场状态不能忽略登场年份");
                if(!p.ignoreDates){int year=w.life.year();if(d.death>0&&d.death<year)throw new IllegalArgumentException(d.template.name+"：预计没年早于剧本，请显式选择忽略年代或修改配置");if(unappeared){if(d.appearance<=year)throw new IllegalArgumentException("未登场年份必须晚于剧本年份");}else if(d.birth>year||d.appearance>year)throw new IllegalArgumentException(d.template.name+"：尚未出生/登场，请选未登场或显式忽略年代");}
                World.Officer o;
                if(old==null){o=new World.Officer(d.runtimeId,d.template.name,owner,home,0,0,0,0,0);w.officers.add(o);w.editor.customOfficers.add(o.id);o.loyalty=owner<0?0:p.loyalty;}
                else{o=renamed(old,d.template.name);w.officers.set(w.officers.indexOf(old),o);}
                set(o,d);Contests.Profile profile=w.contests.profile(o.id);w.contests.configure(o.id,new Contests.Profile(d.template.temper,d.template.talkMask,old==null?0:profile.gearMask));
                if(old!=null&&w.life.state(old.id)==Lifecycle.State.DEAD)throw new IllegalArgumentException("不能覆盖已故人物");
                w.life.configure(o.id,p.ignoreDates?0:d.birth,p.ignoreDates?0:d.appearance,p.ignoreDates?0:d.death,home,unappeared?Lifecycle.State.UNAPPEARED:Lifecycle.State.ACTIVE);
            }
            if(w.officers.size()+w.strategy.talents.size()>10000)throw new IllegalArgumentException("武将数量超过引擎上限10000");
            for(Definition d:chosen)if(d.targetId>=0&&d.replaceRelations)for(Relations.Kind kind:Relations.Kind.values())for(int target:new ArrayList<>(w.relations.links(d.effectiveId(),kind)))w.relations.unlink(d.effectiveId(),target,kind);
            for(Relations.Kind kind:Relations.Kind.values())for(Definition d:chosen)for(Link l:d.links)if(l.kind==kind){Integer target=l.target.startsWith("h:")?Integer.valueOf(l.target.substring(2)):active.get(l.target);if(target==null||w.officer(target)==null)throw new IllegalArgumentException(d.template.name+"：关系对象未投放/不在本剧本，不能丢弃引用");if(!w.relations.links(d.effectiveId(),kind).contains(target))link(w,d.effectiveId(),target,kind);}
            w.extensions.put(NAMESPACE,encodeSnapshot(chosen,config));SaveCodec.validate(w);return w;
        }catch(IllegalArgumentException|ArithmeticException e){throw new IOException("自定义武将校验失败："+e.getMessage(),e);}
    }
    private static void link(World w,int a,int b,Relations.Kind kind){
        if((kind==Relations.Kind.FATHER||kind==Relations.Kind.MOTHER)&&w.relations.parent(a,kind==Relations.Kind.MOTHER)>=0)throw new IllegalArgumentException("已有父母关系冲突，不能静默替换");
        String error=w.relations.linkError(a,b,kind);if(error!=null)throw new IllegalArgumentException(w.officer(a).name+" / "+(w.officer(b)==null?b:w.officer(b).name)+"："+error);w.relations.link(a,b,kind);
    }
    private static void set(World.Officer o,Definition d){Editor.Template t=d.template;o.leadership=t.stat(0);o.war=t.stat(1);o.intelligence=t.stat(2);o.politics=t.stat(3);o.charm=t.stat(4);for(int i=0;i<6;i++)o.aptitude[i]=t.aptitude(i);o.sex=t.sex;o.skillId=t.skill;o.affinity=d.affinity;o.honor=d.honor;}
    private static World.Officer renamed(World.Officer old,String name){World.Officer o=new World.Officer(old.id,name,old.owner,old.cityId,0,0,0,0,0);o.unitId=old.unitId;o.acted=old.acted;o.loyalty=old.loyalty;o.role=old.role;o.otherTask=old.otherTask;o.otherTaskTurns=old.otherTaskTurns;o.lastRewardTurn=old.lastRewardTurn;return o;}
    private static byte[] encodeSnapshot(List<Definition> defs,byte[] config)throws IOException{
        if(config==null||config.length>MAX_SNAPSHOT)throw new IOException("人物配置快照过大");ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(VERSION);out.writeInt(config.length);out.write(config);out.writeInt(defs.size());
        for(Definition d:defs){out.writeInt(d.effectiveId());out.writeUTF(d.id);out.writeInt(d.revision);out.writeUTF(d.portrait);out.writeInt(d.portraitPng.length);out.write(d.portraitPng);}out.flush();if(bytes.size()>MAX_SNAPSHOT)throw new IOException("人物与头像快照超过8MiB，请减少启用头像数量");return bytes.toByteArray();
    }
    public static final class Portrait {
        public final String ref;public final byte[] png;Portrait(String ref,byte[] png){this.ref=ref;this.png=png;}
    }
    /** Immutable per-world snapshot lookup. No library paths or mutable global registries. */
    private static final Map<World,Map<Integer,Portrait>> PORTRAITS=Collections.synchronizedMap(new WeakHashMap<>());
    public static Portrait portrait(World w,int officer){
        Map<Integer,Portrait> map=PORTRAITS.get(w);if(map==null){try{map=readSnapshot(w.extensions.get(NAMESPACE));}catch(IOException e){map=Collections.emptyMap();}PORTRAITS.put(w,map);}return map.get(officer);
    }
    public static void validateSnapshot(World w)throws IOException{for(int id:readSnapshot(w.extensions.get(NAMESPACE)).keySet())if(w.officer(id)==null)throw new IOException("自定义人物快照引用缺失");}
    private static Map<Integer,Portrait> readSnapshot(byte[] bytes)throws IOException{
        Map<Integer,Portrait> result=new HashMap<>();if(bytes==null)return result;if(bytes.length>MAX_SNAPSHOT)throw new IOException("人物快照过大");DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readInt()!=VERSION)throw new IOException("人物快照版本不支持");int config=in.readInt();if(config<0||config>in.available())throw new IOException("人物配置长度错误");in.skipBytes(config);int count=in.readInt();if(count<0||count>1000)throw new IOException("人物快照数量错误");Set<String> ids=new HashSet<>();
        for(int i=0;i<count;i++){int id=in.readInt();String uuid=in.readUTF();try{UUID.fromString(uuid);}catch(IllegalArgumentException e){throw new IOException("人物快照身份错误",e);}if(!ids.add(uuid)||in.readInt()<1)throw new IOException("人物快照修订错误");String ref=in.readUTF();int size=in.readInt();if(size<0||size>MAX_PORTRAIT||size>in.available())throw new IOException("头像长度错误");byte[] png=new byte[size];in.readFully(png);if(result.put(id,new Portrait(ref,png))!=null)throw new IOException("快照人物ID重复");}
        if(in.available()!=0)throw new IOException("人物快照尾部错误");return result;
    }
}
