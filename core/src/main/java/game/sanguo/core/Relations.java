package game.sanguo.core;

import java.io.IOException;
import java.util.*;

/** Explicit person IDs only. No historical relationships are inferred from names or faction. */
public final class Relations {
    public enum Kind { FATHER("父亲"), MOTHER("母亲"), SPOUSE("配偶"), SWORN("义兄弟"), LIKE("亲爱"), DISLIKE("厌恶");
        public final String label;Kind(String label){this.label=label;}
    }
    static final class Person {
        int father=-1,mother=-1,spouse=-1;
        final SortedSet<Integer> sworn=new TreeSet<>(),likes=new TreeSet<>(),dislikes=new TreeSet<>();
    }
    final SortedMap<Integer,Person> people=new TreeMap<>();
    private final World w;
    Relations(World w){this.w=w;}
    private Person person(int id){return people.computeIfAbsent(id,x->new Person());}
    public int parent(int id,boolean mother){Person p=people.get(id);return p==null?-1:mother?p.mother:p.father;}
    public int spouse(int id){Person p=people.get(id);return p==null?-1:p.spouse;}
    public Set<Integer> links(int id,Kind kind){
        Person p=people.get(id);if(p==null)return Collections.emptySet();
        if(kind==Kind.SWORN||kind==Kind.LIKE||kind==Kind.DISLIKE)return Collections.unmodifiableSet(kind==Kind.SWORN?p.sworn:kind==Kind.LIKE?p.likes:p.dislikes);
        int target=kind==Kind.FATHER?p.father:kind==Kind.MOTHER?p.mother:p.spouse;return target<0?Collections.emptySet():Collections.singleton(target);
    }
    public boolean sworn(int a,int b){Person p=people.get(a);return p!=null&&p.sworn.contains(b);}
    public boolean bonded(int a,int b){return a!=b&&(spouse(a)==b||sworn(a,b));}
    public boolean dislikes(int a,int b){Person p=people.get(a);return p!=null&&p.dislikes.contains(b);}
    public boolean likes(int a,int b){Person p=people.get(a);return p!=null&&p.likes.contains(b);}
    private Set<Integer> ancestors(int id){
        Set<Integer> seen=new HashSet<>();Deque<Integer> todo=new ArrayDeque<>();todo.add(id);
        while(!todo.isEmpty()){int v=todo.remove();if(!seen.add(v))continue;int f=parent(v,false),m=parent(v,true);if(f>=0)todo.add(f);if(m>=0)todo.add(m);}return seen;
    }
    public boolean blood(int a,int b){if(a==b)return true;Set<Integer> aa=ancestors(a),bb=ancestors(b);aa.retainAll(bb);return !aa.isEmpty();}
    public boolean loyalBond(int target){
        World.Officer t=w.officer(target);if(t==null||t.owner<0||!w.alive(t.owner))return false;
        Person p=people.get(target);if(p==null)return false;
        if(p.spouse!=target&&ally(p.spouse,t.owner,true))return true;
        for(int id:p.sworn)if(id!=target&&ally(id,t.owner,true))return true;return false;
    }
    public boolean refuses(int target,int recruiter,int owner){
        if(loyalBond(target))return true;
        if(dislikes(target,recruiter))return true;
        Person p=people.get(target);if(p==null)return false;
        for(int id:p.dislikes){World.Officer o=w.officer(id);if(o!=null&&o.owner==owner&&o.role==Strategy.Role.RULER)return true;}return false;
    }
    public int recruitmentBonus(int target,int recruiter,int owner){
        if(bonded(target,recruiter)||likes(target,recruiter))return 20;
        Person p=people.get(target);if(p==null)return 0;
        if(p.spouse!=target&&ally(p.spouse,owner,false))return 10;
        for(int id:p.sworn)if(id!=target&&ally(id,owner,false))return 10;
        for(int id:p.likes)if(ally(id,owner,false))return 10;return 0;
    }
    private boolean ally(int id,int owner,boolean present){
        if(id<0)return false;World.Officer o=w.officer(id);
        return o!=null&&o.owner==owner&&(!present||w.life.present(id));
    }
    /** Positive-gap fractions from published SAN11 experiments; two deputies never add their boosts. */
    public int contribution(int leader,int deputy,int leaderValue,int deputyValue){
        if(deputyValue<=leaderValue||dislikes(leader,deputy)||dislikes(deputy,leader))return leaderValue;
        if(bonded(leader,deputy))return deputyValue;
        int divisor=likes(leader,deputy)||likes(deputy,leader)?2:blood(leader,deputy)?3:4;
        return leaderValue+(deputyValue-leaderValue)/divisor;
    }
    public int supportChance(int helper,int leader){
        if(dislikes(helper,leader)||dislikes(leader,helper))return 0;
        if(bonded(helper,leader))return 50;
        if(likes(helper,leader)||blood(helper,leader))return 30;
        return w.skills.has(w.officer(helper),Skill.FUZUO)?30:0;
    }
    String linkError(int a,int b,Kind kind){
        if(kind==null||a==b||w.officer(a)==null||w.officer(b)==null)return "请选择两个不同的现存武将";
        if(links(a,kind).contains(b))return "关系已存在";
        if(kind==Kind.FATHER||kind==Kind.MOTHER){
            if(ancestors(b).contains(a))return "父母关系不能形成循环";
            if(parent(a,kind!=Kind.MOTHER)==b)return "同一武将不能同时作为父亲和母亲";
        }
        if(kind==Kind.LIKE&&dislikes(a,b)||kind==Kind.DISLIKE&&(likes(a,b)||bonded(a,b)))return "亲爱、厌恶或亲密关系冲突";
        if(kind==Kind.LIKE&&links(a,kind).size()>=5||kind==Kind.DISLIKE&&links(a,kind).size()>=5)return "亲爱或厌恶最多各5人";
        if(kind==Kind.SPOUSE||kind==Kind.SWORN){
            if(dislikes(a,b)||dislikes(b,a)||blood(a,b))return "有厌恶或血缘关系，不能仲介";
            if(kind==Kind.SPOUSE&&(spouse(a)>=0||spouse(b)>=0||sworn(a,b)))return "已有配偶或互为义兄弟";
            if(kind==Kind.SWORN){Set<Integer> group=swornGroup(a,b);if(group.size()>3)return "义兄弟最多3人";
                for(int x:group)for(int y:group)if(x!=y&&(blood(x,y)||dislikes(x,y)||spouse(x)==y))return "义兄弟组成员关系冲突";}
        }
        return null;
    }
    private Set<Integer> swornGroup(int a,int b){Set<Integer> group=new TreeSet<>();group.add(a);group.add(b);group.addAll(links(a,Kind.SWORN));group.addAll(links(b,Kind.SWORN));return group;}
    void link(int a,int b,Kind kind){
        Person p=person(a);
        switch(kind){
            case FATHER:p.father=b;break;case MOTHER:p.mother=b;break;
            case SPOUSE:p.spouse=b;person(b).spouse=a;break;
            case SWORN:Set<Integer> group=swornGroup(a,b);for(int x:group){person(x).sworn.addAll(group);person(x).sworn.remove(x);}break;
            case LIKE:p.likes.add(b);break;case DISLIKE:p.dislikes.add(b);break;
        }
    }
    void unlink(int a,int b,Kind kind){
        Person p=people.get(a);if(p==null)return;
        switch(kind){case FATHER:p.father=-1;break;case MOTHER:p.mother=-1;break;
            case SPOUSE:p.spouse=-1;person(b).spouse=-1;break;
            case SWORN:for(int member:new ArrayList<>(p.sworn))person(member).sworn.remove(a);p.sworn.clear();break;
            case LIKE:p.likes.remove(b);break;case DISLIKE:p.dislikes.remove(b);break;}
    }
    public String mediateError(int city,int first,int second,Kind kind){
        if(w.commandsBlocked()||w.gameOver()||w.active!=w.player)return "当前不能仲介";
        if(kind!=Kind.SPOUSE&&kind!=Kind.SWORN)return "仲介仅支持结义与婚姻";
        String error=linkError(first,second,kind);if(error!=null)return error;
        World.City c=w.city(city);Set<Integer> group=kind==Kind.SWORN?swornGroup(first,second):new TreeSet<>(Arrays.asList(first,second));
        if(c==null||c.owner!=w.active)return "请选择己方据点";
        for(int id:group){World.Officer o=w.officer(id);if(o.owner!=c.owner||o.cityId!=city||o.unitId>=0||w.government.captive(id)||w.strategy.busy(id)||w.domestic.busy(id))return "参与者须为本城无任务的己方武将";
            if(w.government.merit(id)<500)return "参与者功绩须至少500";
            if(o.sex==World.Sex.UNKNOWN)return "请先设置参与者性别";
            if(kind==Kind.SWORN&&o.sex!=w.officer(first).sex)return "结义需要同性武将";}
        if(kind==Kind.SPOUSE&&w.officer(first).sex==w.officer(second).sex)return "婚姻需要一男一女";
        return w.campaign.points(w.active)<500?"仲介需要500技巧点":null;
    }
    public World.Result mediate(int city,int first,int second,Kind kind){w.reports.prepare();
        String error=mediateError(city,first,second,kind);if(error!=null)return w.fail(error);
        w.campaign.points.put(w.active,w.campaign.points(w.active)-500);link(first,second,kind);
        if(kind==Kind.SPOUSE&&(w.skills.has(w.officer(first),Skill.NEIZHU)||w.skills.has(w.officer(second),Skill.NEIZHU)))for(int id:new int[]{first,second}){
            World.Officer o=w.officer(id);o.leadership=Math.min(100,o.leadership+1);o.war=Math.min(100,o.war+1);o.intelligence=Math.min(100,o.intelligence+1);o.politics=Math.min(100,o.politics+1);o.charm=Math.min(100,o.charm+1);}
        return w.success(w.officer(first).name+"与"+w.officer(second).name+"结为"+kind.label+"，技巧−500");
    }
    public String describe(int id){StringBuilder out=new StringBuilder();for(Kind k:Kind.values())for(int t:links(id,k))out.append(k.label).append("：").append(w.officer(t).name).append('\n');return out.length()==0?"无已记录关系":out.toString().trim();}
    void validate()throws IOException{
        for(Map.Entry<Integer,Person> e:people.entrySet()){
            int a=e.getKey();Person p=e.getValue();require(p.father>=-1&&p.mother>=-1&&p.spouse>=-1,"关系ID越界");require(w.officer(a)!=null,"关系武将缺失");
            for(Kind k:Kind.values())for(int b:links(a,k))require(a!=b&&w.officer(b)!=null,"关系目标缺失或自指");
            require(p.father<0||p.mother<0||p.father!=p.mother,"父母重复");
            require(p.father<0||!ancestors(p.father).contains(a),"父系循环");require(p.mother<0||!ancestors(p.mother).contains(a),"母系循环");
            require(p.spouse<0||spouse(p.spouse)==a&&!blood(a,p.spouse)&&!sworn(a,p.spouse)&&!dislikes(a,p.spouse)&&!dislikes(p.spouse,a),"配偶关系冲突");
            require(p.sworn.size()<=2&&p.likes.size()<=5&&p.dislikes.size()<=5,"关系数量越界");
            for(int b:p.sworn){require(sworn(b,a)&&!blood(a,b)&&!dislikes(a,b)&&!dislikes(b,a),"结义关系冲突");Set<Integer> other=new TreeSet<>(links(b,Kind.SWORN));other.remove(a);Set<Integer> own=new TreeSet<>(p.sworn);own.remove(b);require(other.equals(own),"结义组不闭合");}
            for(int b:p.likes)require(!p.dislikes.contains(b),"亲爱与厌恶冲突");
        }
    }
    private static void require(boolean ok,String msg)throws IOException{if(!ok)throw new IOException(msg);}
}
