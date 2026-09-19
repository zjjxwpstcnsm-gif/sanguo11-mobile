package game.sanguo.core;

import java.io.IOException;
import java.util.*;

/** Explicit sourced setup/edit operations. Save decoding never consults these definitions. */
public final class ContentProfiles {
    private ContentProfiles(){}
    public static Debate.Temper temper(ContentCatalog.Officer o){
        switch(o.personality){case "小心":return Debate.Temper.TIMID;case "冷靜":return Debate.Temper.CALM;
            case "剛膽":return Debate.Temper.BOLD;case "豬突":return Debate.Temper.RASH;
            default:throw new IllegalArgumentException("未知来源性格："+o.personality);}
    }
    static boolean matches(World w,ContentCatalog catalog,int id){
        World.Officer o=w.officer(id);ContentCatalog.Officer d=catalog.officer(id);
        return o!=null&&d!=null&&(o.name.equals(d.name)||o.name.equals(catalog.alias(id)));
    }
    static void biography(World w,ContentCatalog catalog,int id,int home,boolean dated){
        ContentCatalog.Officer d=catalog.officer(id);Contests.Profile current=w.contests.profile(id);
        w.officer(id).affinity=catalog.profile(id).affinity;w.officer(id).honor=catalog.profile(id).honor;
        int explicitGear=w.contests.profiles.containsKey(id)?w.contests.profiles.get(id).gearMask:0;
        w.contests.configure(id,new Contests.Profile(temper(d),current.talkMask,explicitGear));
        if(dated){
            Lifecycle.State state=d.appearance>w.life.year()?Lifecycle.State.UNAPPEARED:Lifecycle.State.ACTIVE;
            // Historical violent death is not a measured natural lifespan. Preserve the raw year in the catalog only.
            w.life.configure(id,d.birth,d.appearance,catalog.profile(id).naturalDeath?d.death:0,home,state);
        }
    }
    static void relations(World w,ContentCatalog catalog,Set<Integer> selected){
        // Parent edges first: relationship validation must see blood links before spouse/sworn links.
        for(Relations.Kind kind:Relations.Kind.values())for(ContentCatalog.Relation r:catalog.relations()){
            if(r.kind!=kind||!selected.contains(r.officer)&&!selected.contains(r.target)||
                !matches(w,catalog,r.officer)||!matches(w,catalog,r.target)||w.relations.links(r.officer,kind).contains(r.target))continue;
            if((kind==Relations.Kind.FATHER||kind==Relations.Kind.MOTHER)&&w.relations.parent(r.officer,kind==Relations.Kind.MOTHER)>=0)
                throw new IllegalArgumentException("已有父母资料与来源冲突："+w.officer(r.officer).name);
            String error=w.relations.linkError(r.officer,r.target,kind);
            if(error!=null)throw new IllegalArgumentException("来源关系冲突："+w.officer(r.officer).name+" / "+w.officer(r.target).name+"："+error);
            w.relations.link(r.officer,r.target,kind);
        }
    }
    static void initialize(World w,ContentCatalog catalog)throws IOException {
        initialize(w,catalog,true);
    }
    static void initialize(World w,ContentCatalog catalog,boolean dated)throws IOException {
        Set<Integer> ids=new TreeSet<>();
        for(World.Officer o:w.officers){
            if(w.life.life(o.id)!=null||w.contests.profiles.containsKey(o.id))throw new IOException("显式人物配置不能与来源整表同时启用");
            biography(w,catalog,o.id,o.cityId,dated);ids.add(o.id);
        }
        relations(w,catalog,ids);
    }
    static void add(World w,ContentCatalog catalog,int id,int city,boolean dated,boolean relations){
        ContentCatalog.Officer d=catalog.officer(id);World.City c=w.city(city);
        if(d==null||c==null||c.owner!=w.player)throw new IllegalArgumentException("请选择资料武将和己方据点");
        if(w.officer(id)!=null||w.strategy.talents.stream().anyMatch(t->t.id==id))throw new IllegalArgumentException("该人物编号已在局面中，不能重复加入");
        if(w.officers.size()+w.strategy.talents.size()>=10000)throw new IllegalArgumentException("武将数量已达上限");
        boolean future=dated&&d.appearance>w.life.year();
        World.Officer o=new World.Officer(id,d.name,future?-1:w.player,city,d.stat(0),d.stat(1),d.stat(2),d.stat(3),d.stat(4));
        for(int i=0;i<6;i++)o.aptitude[i]=d.aptitude(i);
        o.sex=d.gender.equals("男")?World.Sex.MALE:World.Sex.FEMALE;
        o.skillId=d.skillId.equals("none")?"none":ContentRuntime.skill(d.skillId).id;
        w.officers.add(o);biography(w,catalog,id,city,dated);
        if(relations)relations(w,catalog,Collections.singleton(id));
    }
    public static String describe(ContentCatalog catalog,int id){
        StringBuilder s=new StringBuilder();
        for(ContentCatalog.Relation r:catalog.relations())if(r.officer==id)s.append(r.kind.label).append("：").append(catalog.officer(r.target).name).append(" (#").append(r.target).append(")\n");
        return s.length()==0?"无已解析的直接关系":s.toString().trim();
    }
}
