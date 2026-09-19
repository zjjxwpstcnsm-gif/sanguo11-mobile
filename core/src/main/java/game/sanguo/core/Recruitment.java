package game.sanguo.core;

import game.sanguo.core.Recruitment;
import game.sanguo.core.World;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class Recruitment {
    private final List<Mission> missions = new ArrayList();
    private final World w;

    public static final class Mission {
        public final int actor;
        public final int destination;
        public boolean joined;
        public int fieldUnit=-1;
        public boolean fieldJoined;
        public final int owner;
        public int remaining;
        public final int source;
        public final int target;
        public final int targetOwner;
        public final int travel;

        Mission(int actor, int target, int source, int destination, int owner, int targetOwner, int travel) {
            this.actor = actor;
            this.target = target;
            this.source = source;
            this.destination = destination;
            this.owner = owner;
            this.targetOwner = targetOwner;
            this.travel = travel;
            this.remaining = travel * 2;
        }
    }

    Recruitment(World w) {
        this.w = w;
    }

    public List<Mission> missions() {
        return Collections.unmodifiableList(this.missions);
    }

    boolean assigned(World.Officer officer) {
        for (Mission m : this.missions) {
            if (m.owner == officer.owner && m.remaining == officer.otherTaskTurns) {
                if (m.actor == officer.id && officer.otherTask.startsWith("出使登用：")) {
                    return true;
                }
                if (m.actor == officer.id && officer.otherTask.equals("登用返程")) {
                    return true;
                }
                if (m.joined && !m.fieldJoined && m.target == officer.id && officer.otherTask.equals("登用返程")) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean returning(World.Officer officer) {
        for (Mission m : this.missions) {
            if (m.joined && !m.fieldJoined && m.target == officer.id && m.owner == officer.owner && m.destination == officer.cityId && m.remaining > 0 && officer.otherTaskTurns > 0 && officer.otherTask.equals("登用返程")) {
                return true;
            }
        }
        return false;
    }

    public boolean pending(int owner, int target) {
        for (Mission m : this.missions) {
            if (m.owner == owner && m.target == target) {
                return true;
            }
        }
        return false;
    }

    public int travelTurns(int source,int target){
        World.City from=w.city(source);World.Officer o=w.officer(target);if(from==null||o==null)return 0;
        World.Unit field=w.loyalty.fieldUnit(target);
        if(field!=null)return Math.min(50,Math.max(1,w.personnel.turns(field.hex,source)));
        World.City to=w.city(o.cityId);return to==null||to==from?0:Math.min(50,Math.max(1,w.personnel.turns(from.id,to.id)));
    }
    public String travelDescription(int source,int target){
        World.Unit field=w.loyalty.fieldUnit(target);int turns=travelTurns(source,target);
        if(field==null&&turns==0)return "本城交涉，当旬结算。";
        return (field==null?"跨城登用":"野外登用：追踪当前部队编号，抵达时再次校验")+"；去程"+turns+"旬，往返"+(turns*2)+"旬。使者离城期间不可再执行命令。"
            +(field==null?"":"主将接受登用将整队改旗；副将单独接受登用只离队，不带走原部队。");
    }
    World.Result start(World.City source,World.Officer actor,World.Officer target){
        World.Unit field=w.loyalty.fieldUnit(target.id);
        World.City destination=field==null?w.city(target.cityId):w.personnel.region(field.hex);
        Mission m=new Mission(actor.id,target.id,source.id,destination.id,source.owner,target.owner,travelTurns(source.id,target.id));
        m.fieldUnit=field==null?-1:field.id;missions.add(m);w.strategy.releaseGovernor(actor.id);
        actor.otherTask="出使登用："+target.name.substring(0,Math.min(60,target.name.length()));actor.otherTaskTurns=m.remaining;
        return w.success(actor.name+"前往"+(field==null?destination.name:"野外部队")+"登用"+target.name+"，去程"+m.travel+"旬，往返"+m.remaining+"旬");
    }
    public String describe(Mission m){
        return w.officer(m.actor).name+" → "+w.officer(m.target).name+" · "+(m.fieldUnit>=0?"野外部队":w.city(m.destination).name)
            +(m.remaining>m.travel?" · 去程":" · 返程")+" · 剩"+m.remaining+"旬"+(m.joined?(m.fieldJoined?" · 部队已倒戈，使者独自返程":" · 已说服，同路返回"):"");
    }

    private boolean actorAvailable(Mission m, World.Officer actor) {
        return actor != null && this.w.life.present(actor.id) && actor.owner == m.owner && !this.w.government.captive(actor.id) && actor.unitId < 0 && actor.otherTaskTurns > 0 && (actor.otherTask.startsWith("出使登用：") || actor.otherTask.equals("登用返程")) && this.w.city(m.source).owner == m.owner;
    }

    private void release(World.Officer officer, Mission m) {
        if (officer == null || officer.owner != m.owner || !this.w.life.present(officer.id) || this.w.government.captive(officer.id) || officer.unitId >= 0 || this.w.domestic.busy(officer.id)) {
            return;
        }
        if (officer.otherTaskTurns <= 0 || officer.otherTask.startsWith("出使登用：") || officer.otherTask.equals("登用返程")) {
            officer.otherTaskTurns = 0;
            officer.otherTask = "";
            officer.acted = true;
            if (this.w.city(m.source).owner != m.owner) {
                this.w.retreat(officer, this.w.city(m.destination).hex);
            } else {
                officer.cityId = m.source;
            }
        }
    }


    void cancelOwner(int owner) { missions.removeIf(m->m.owner==owner); }

    void tick() {
        Iterator it = new ArrayList(this.missions).iterator();
        while (it.hasNext()) {
            Mission m = (Mission) it.next();
            if (this.missions.contains(m)) {
                World.Officer actor = this.w.officer(m.actor);
                World.Officer target = this.w.officer(m.target);
                if (!actorAvailable(m, actor)) {
                    release(actor, m);
                    if (m.joined && !m.fieldJoined) {
                        release(target, m);
                    }
                    this.missions.remove(m);
                    this.w.note("登用使节因驻城或人员状态变化中止，人员返回可用据点");
                } else {
                    m.remaining--;
                    if (m.remaining == m.travel) {
                        World.Unit field=target==null?null:w.loyalty.fieldUnit(target.id);
                        boolean located=m.fieldUnit<0?target!=null&&target.cityId==m.destination:field!=null&&field.id==m.fieldUnit;
                        boolean valid=target!=null&&target.owner==m.targetOwner&&located&&w.strategy.recruitable(m.owner,m.target);
                        int chance = valid ? this.w.strategy.recruitChance(m.owner, m.actor, m.target) : 0;
                        if (valid && chance > 0 && this.w.strategy.nextInt(100) < chance) {
                            this.w.strategy.join(actor, target, m.destination);
                            m.joined = true;
                            m.fieldJoined=m.fieldUnit>=0&&w.loyalty.fieldUnit(target.id)!=null;
                            if(!m.fieldJoined){target.otherTask="登用返程";target.otherTaskTurns=m.remaining+1;}
                            w.note(target.name+(m.fieldJoined?"部队已加入；":"接受登用，一同返程；")+actor.name+"返程尚需"+m.remaining+"旬");
                        } else {
                            this.w.note(actor.name + (valid ? "登用未成功" : "抵达时目标已移动或不可登用") + "，返程尚需" + m.remaining + "旬");
                        }
                        actor.otherTask = "登用返程";
                    }
                    if (m.remaining == 0) {
                        release(actor, m);
                        if (m.joined && !m.fieldJoined) {
                            release(target, m);
                        }
                        this.missions.remove(m);
                        this.w.note(actor.name + "完成登用出使并返回");
                    }
                }
            }
        }
    }

    void write(DataOutputStream out) throws IOException {
        out.writeInt(this.missions.size());
        for (Mission m : this.missions) {
            int[] iArr = {m.actor, m.target, m.source, m.destination, m.owner, m.targetOwner, m.travel, m.remaining};
            for (int i = 0; i < 8; i++) {
                int v = iArr[i];
                out.writeInt(v);
            }
            out.writeBoolean(m.joined);
        }
    }

    void read(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > 10000) {
            throw new IOException("登用任务数量无效");
        }
        for (int i = 0; i < count; i++) {
            Mission m = new Mission(in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt());
            m.remaining = in.readInt();
            m.joined = in.readBoolean();
            this.missions.add(m);
        }
    }

    void writeField(DataOutputStream out)throws IOException{
        out.writeInt(missions.size());for(Mission m:missions){out.writeInt(m.actor);out.writeInt(m.fieldUnit);out.writeBoolean(m.fieldJoined);}
    }
    void readField(DataInputStream in)throws IOException{
        int n=in.readInt();if(n!=missions.size())throw new IOException("野外登用任务数量不一致");
        Set<Integer> seen=new HashSet<>();for(int i=0;i<n;i++){
            int actor=in.readInt();Mission match=null;for(Mission m:missions)if(m.actor==actor)match=m;
            if(match==null||!seen.add(actor))throw new IOException("野外登用任务引用错误");
            match.fieldUnit=in.readInt();match.fieldJoined=in.readBoolean();
        }
    }

    void validate() throws IOException {
        Set<Integer> actors=new HashSet<>();Set<String> targets=new HashSet<>();
        for(Mission m:missions) {
            if(w.officer(m.actor)==null||w.officer(m.target)==null||w.city(m.source)==null||w.city(m.destination)==null
                ||m.source==m.destination&&m.fieldUnit<0||m.owner<0||m.owner>=w.factions.length||m.targetOwner< -1||m.targetOwner>=w.factions.length
                ||m.fieldUnit< -1||m.fieldUnit==0||m.fieldUnit>=20000000||m.fieldJoined&&(!m.joined||m.fieldUnit<0)
                ||m.travel<1||m.travel>50||m.remaining<1||m.remaining>m.travel*2||m.joined&&m.remaining>m.travel
                ||!actors.add(m.actor)||!targets.add(m.owner+":"+m.target))throw new IOException("登用任务字段或引用无效");
        }
    }
}
