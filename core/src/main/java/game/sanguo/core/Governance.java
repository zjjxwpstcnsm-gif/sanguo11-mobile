package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Authored mobile governance rules. Read-only labels never mutate a save or a preview. */
public final class Governance {
    private static final int[] THRESHOLDS={0,2,4,10,20,24};
    private static final String[] TITLES={"君主","刺史","州牧","大将军","王","皇帝"};
    final SortedMap<Integer,Integer> grades=new TreeMap<>();
    final SortedMap<Integer,String> nations=new TreeMap<>();
    private final World w;
    Governance(World w){this.w=w;}
    public int cityCount(int side){int n=0;for(World.City c:w.cities)if(c.owner==side&&c.kind==World.SiteKind.CITY)n++;return n;}
    private static int earned(int cities){int grade=0;while(grade+1<THRESHOLDS.length&&cities>=THRESHOLDS[grade+1])grade++;return grade;}
    public int grade(int side){return side<0||side>=w.factions.length?0:Math.max(grades.getOrDefault(side,0),earned(cityCount(side)));}
    public String title(int side){return TITLES[grade(side)];}
    public int rulerCommand(int side){return 10000+1000*grade(side);}
    public String nation(int side){return nations.getOrDefault(side,"");}
    public String label(int side){String nation=nation(side);if(!nation.isEmpty())return nation;
        World.Officer ruler=w.loyalty.ruler(side);return ruler==null?(side>=0&&side<w.factions.length?w.factions[side]:"空白势力"):ruler.name;}
    public String office(World.Officer o){if(o.role==Strategy.Role.RULER)return title(o.owner);Government.Rank r=w.government.office(o.id);return r==null?"未任官":r.id;}
    public String advisor(int side){World.Officer o=w.government.advisor(side);return o==null?"未任命":o.name;}
    public World.Result nameNation(int side,String name){w.reports.prepare();
        if(w.commandsBlocked()||w.gameOver()||side!=w.active||side!=w.player||w.loyalty.ruler(side)==null)return w.fail("当前不能更改国号");
        if(grade(side)<5)return w.fail("君主成为皇帝后方可设定国号");
        String text=name==null?"":name.trim();if(!validName(text))return w.fail("国号须为1至8个汉字、字母或数字");
        for(int other=0;other<w.factions.length;other++)if(other!=side&&(text.equals(label(other))||text.equals(w.factions[other])))return w.fail("国号与其他势力重名");
        if(text.equals(nation(side)))return w.fail("国号未变化");nations.put(side,text);return w.success("国号定为“"+text+"”，势力编号与外交关系不变");
    }
    private static boolean validName(String s){return s!=null&&s.codePointCount(0,s.length())>=1&&s.codePointCount(0,s.length())<=8&&s.matches("[\\p{L}\\p{N}]+");}
    /** Standing and locally working officers are residents; expeditions, recruitment and envoys are not. */
    public boolean resident(World.Officer o,World.City c){
        if(o==null||c==null||c.owner<0||o.owner!=c.owner||o.cityId!=c.id||o.unitId>=0||!w.life.present(o.id)||w.government.captive(o.id))return false;
        if(o.otherTaskTurns>0&&(o.otherTask.contains("出使")||o.otherTask.contains("外交")||o.otherTask.contains("登用")||o.otherTask.contains("返程")))return false;
        for(Domestic.Mission m:w.domestic.missions)if(m.contains(o.id))return false;
        return true;
    }
    /** One O(officers + sites + mission crew) pass, called only at mutation boundaries. */
    void reconcile(boolean announce){
        int[] cities=new int[w.factions.length];for(World.City c:w.cities)if(c.owner>=0&&c.kind==World.SiteKind.CITY)cities[c.owner]++;
        for(int side=0;side<cities.length;side++){int previous=grades.getOrDefault(side,0),next=Math.max(previous,earned(cities[side]));
            if(next>previous){grades.put(side,next);if(announce)w.note(label(side)+"领有"+cities[side]+"城，君主晋为"+TITLES[next]+"；基础指挥"+(10000+1000*next));}}
        Set<Integer> away=new HashSet<>();for(Domestic.Mission m:w.domestic.missions)for(int id:m.crew())away.add(id);
        Map<Integer,World.Officer> best=new HashMap<>();Set<Integer> residents=new HashSet<>();
        for(World.Officer o:w.officers){World.City c=w.city(o.cityId);
            if(c==null||c.owner<0||o.owner!=c.owner||o.unitId>=0||!w.life.present(o.id)||w.government.captive(o.id)||away.contains(o.id))continue;
            if(o.otherTaskTurns>0&&(o.otherTask.contains("出使")||o.otherTask.contains("外交")||o.otherTask.contains("登用")||o.otherTask.contains("返程")))continue;
            residents.add(o.id);World.Officer b=best.get(c.id);if(b==null||o.politics>b.politics||o.politics==b.politics&&o.id<b.id)best.put(c.id,o);
        }
        Set<Integer> appointed=new HashSet<>();
        for(World.City c:w.cities){World.Officer old=w.officer(c.governorId);
            if(old!=null&&residents.contains(old.id)&&old.cityId==c.id){appointed.add(old.id);continue;}
            World.Officer next=best.get(c.id);int id=next==null?-1:next.id;
            if(c.governorId!=id){c.governorId=id;if(announce&&next!=null)w.note(c.name+"自动任命"+next.name+"为太守（政治"+next.politics+"）");}
            if(id>=0)appointed.add(id);
        }
        for(World.Officer o:w.officers)if(o.role!=Strategy.Role.RULER){
            if(appointed.contains(o.id))o.role=Strategy.Role.GOVERNOR;
            else if(o.role==Strategy.Role.GOVERNOR)o.role=o.owner<0?Strategy.Role.UNAFFILIATED:Strategy.Role.OFFICER;
        }
    }
    void write(DataOutputStream d)throws IOException{
        d.writeInt(0x47563636);d.writeInt(w.fieldUnits().size());
        for(World.Unit u:w.fieldUnits()){d.writeInt(u.id);d.writeInt(u.wounded);d.writeInt(u.woundRemainder);}
        d.writeInt(w.factions.length);for(int side=0;side<w.factions.length;side++){d.writeInt(grade(side));d.writeUTF(nation(side));}
    }
    void read(DataInputStream d)throws IOException{
        if(d.readInt()!=0x47563636)throw new IOException("治理存档段错误");int n=d.readInt();
        if(n!=w.fieldUnits().size())throw new IOException("伤兵部队数量不匹配");Set<Integer> ids=new HashSet<>();
        for(int i=0;i<n;i++){World.Unit u=w.unit(d.readInt());if(u==null||!ids.add(u.id))throw new IOException("伤兵部队引用错误");u.wounded=d.readInt();u.woundRemainder=d.readInt();}
        if(d.readInt()!=w.factions.length)throw new IOException("爵位势力数量错误");
        for(int side=0;side<w.factions.length;side++){int grade=d.readInt();String name=d.readUTF();if(grade<0||grade>5||!name.isEmpty()&&!validName(name))throw new IOException("爵位/国号字段错误");grades.put(side,grade);if(!name.isEmpty())nations.put(side,name);}
        validate();
    }
    void validate()throws IOException{
        for(World.Unit u:w.fieldUnits())if(u.wounded<0||u.wounded>1000000||u.woundRemainder<0||u.woundRemainder>=100)throw new IOException("伤兵数量越界");
        for(Map.Entry<Integer,Integer> e:grades.entrySet())if(e.getKey()<0||e.getKey()>=w.factions.length||e.getValue()<0||e.getValue()>5)throw new IOException("君主爵位错误");
        Set<String> seen=new HashSet<>();for(Map.Entry<Integer,String> e:nations.entrySet())if(e.getKey()<0||e.getKey()>=w.factions.length||grade(e.getKey())<5||!validName(e.getValue())||!seen.add(e.getValue()))throw new IOException("国号无效");
    }
}
