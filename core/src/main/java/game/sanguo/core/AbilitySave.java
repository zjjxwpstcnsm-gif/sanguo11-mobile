package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Stable node IDs, explicit hidden selections, bounded counters and cross-system task validation. */
final class AbilitySave {
    private static final int MARKER=0x504B3130;
    static void write(World w,DataOutputStream d)throws IOException {
        d.writeInt(MARKER);d.writeInt(w.factions.length);
        for(AbilityResearch.State s:w.abilities.states){
            strings(d,s.hidden);strings(d,s.learned);d.writeInt(s.used.size());for(Map.Entry<String,Integer> e:s.used.entrySet()){d.writeUTF(e.getKey());d.writeInt(e.getValue());}
            d.writeBoolean(s.research!=null);if(s.research!=null){d.writeInt(s.research.cityId);d.writeUTF(s.research.nodeId);d.writeInt(s.research.remaining);}
        }
        d.writeInt(w.abilities.training.size());for(AbilityResearch.Training t:w.abilities.training){d.writeInt(t.owner);d.writeInt(t.cityId);d.writeInt(t.officerId);d.writeUTF(t.nodeId);d.writeInt(t.startValue);d.writeUTF(t.previousSkill);}
        d.writeInt(w.abilities.gains.size());for(Map.Entry<Integer,int[]> e:w.abilities.gains.entrySet()){d.writeInt(e.getKey());for(int v:e.getValue())d.writeInt(v);}
    }
    private static void strings(DataOutputStream d,Set<String> values)throws IOException{d.writeInt(values.size());for(String s:values)d.writeUTF(s);}
    private static void strings(DataInputStream d,Set<String> values,int max)throws IOException{values.clear();int n=bound(d.readInt(),0,max);for(int i=0;i<n;i++)require(values.add(d.readUTF()),"能力ID重复");}
    static void read(World w,DataInputStream d)throws IOException {
        require(d.readInt()==MARKER&&d.readInt()==w.factions.length,"PK扩展标记或势力数量无效");int max=AbilityResearch.catalog().size();
        for(AbilityResearch.State s:w.abilities.states){
            strings(d,s.hidden,5);strings(d,s.learned,max);int n=bound(d.readInt(),0,max);
            for(int i=0;i<n;i++)require(s.used.put(d.readUTF(),bound(d.readInt(),1,5))==null,"培养次数记录重复");
            if(d.readBoolean())s.research=new AbilityResearch.Research(d.readInt(),d.readUTF(),d.readInt());
        }
        int n=bound(d.readInt(),0,w.factions.length*3);for(int i=0;i<n;i++)w.abilities.training.add(new AbilityResearch.Training(d.readInt(),d.readInt(),d.readInt(),d.readUTF(),d.readInt(),d.readUTF()));
        n=bound(d.readInt(),0,w.officers.size());for(int i=0;i<n;i++){int id=d.readInt();int[] gains=new int[5];for(int j=0;j<5;j++)gains[j]=bound(d.readInt(),0,24);require(w.abilities.gains.put(id,gains)==null,"武将成长记录重复");}
    }
    static void validate(World w)throws IOException {
        AbilityResearch a=w.abilities;
        for(int side=0;side<w.factions.length;side++){
            AbilityResearch.State s=a.states[side];Set<String> slots=new HashSet<>();require(s.hidden.size()==5,"隐藏能力须固定为五处");
            for(String id:s.hidden){AbilityResearch.Node n=AbilityResearch.node(id);require(n!=null&&!n.slot.isEmpty()&&slots.add(n.slot),"隐藏能力位置无效或重复");}
            for(String id:s.learned){AbilityResearch.Node n=AbilityResearch.node(id);require(a.selected(side,n)&&s.learned.containsAll(n.prerequisites),"能力或前置无效");}
            for(Map.Entry<String,Integer> e:s.used.entrySet()){AbilityResearch.Node n=AbilityResearch.node(e.getKey());require(n!=null&&s.learned.contains(e.getKey()),"未研究能力存在培养次数");bound(e.getValue(),1,n.uses);}
            AbilityResearch.Research r=s.research;if(r!=null){AbilityResearch.Node n=AbilityResearch.node(r.nodeId);World.City c=w.city(r.cityId);require(c!=null&&c.owner==side&&a.unlocked(side,n)&&!a.learned(side,r.nodeId),"在研能力归属或前置错误");bound(r.remaining,1,n.turns);}
        }
        Set<Integer> officers=new HashSet<>();Set<String> categories=new HashSet<>();
        for(AbilityResearch.Training t:a.training){
            bound(t.owner,0,w.factions.length-1);AbilityResearch.Node n=AbilityResearch.node(t.nodeId);World.City c=w.city(t.cityId);World.Officer o=w.officer(t.officerId);
            require(n!=null&&a.selected(t.owner,n)&&a.learned(t.owner,n.id)&&a.remaining(t.owner,n.id)>0,"培养能力或次数无效");
            require(c!=null&&o!=null&&c.owner==t.owner&&o.owner==t.owner&&o.cityId==c.id&&o.unitId==-1&&!w.government.captive(o.id),"培养归属或驻地无效");
            require(officers.add(o.id)&&categories.add(t.owner+":"+n.category)&&o.otherTask.equals(t.label()),"培养占用重复或不一致");bound(o.otherTaskTurns,1,3);
            require(!w.domestic.busy(o.id)&&w.campaign.projects().stream().noneMatch(p->p.officerId==o.id)&&w.army.productions().stream().noneMatch(p->p.officerId==o.id),"培养与其他任务冲突");
            require(t.previousSkill!=null&&t.previousSkill.matches("[a-z0-9][a-z0-9._-]{0,79}"),"原特技ID无效");
            if(n.category==AbilityResearch.Category.STAT){require(t.startValue==a.value(o.id,n)&&t.startValue<n.cap&&a.gained(o.id,n.index)<20,"基础能力培养起点无效");}
            else if(n.category==AbilityResearch.Category.APTITUDE)require(t.startValue==n.cap-1&&a.value(o.id,n)==t.startValue,"适性培养起点无效");
            else require(AbilityResearch.skillAvailable(n.skill)&&t.previousSkill.equals(o.skillId)&&!n.skill.id.equals(o.skillId),"特技培养起点无效");
        }
        for(World.Officer o:w.officers)require(!o.otherTask.startsWith("PK培养")||officers.contains(o.id),"缺少PK培养记录");
        for(Map.Entry<Integer,int[]> e:a.gains.entrySet()){World.Officer o=w.officer(e.getKey());require(o!=null&&e.getValue().length==5,"成长武将引用错误");for(int i=0;i<5;i++){bound(e.getValue()[i],0,24);require(e.getValue()[i]<=w.campaign.studyValue(o.id,Campaign.Study.values()[i]),"成长超过当前能力");}}
    }
    private static int bound(int v,int min,int max)throws IOException{require(v>=min&&v<=max,"PK存档字段越界");return v;}
    private static void require(boolean ok,String message)throws IOException{if(!ok)throw new IOException(message);}
}
