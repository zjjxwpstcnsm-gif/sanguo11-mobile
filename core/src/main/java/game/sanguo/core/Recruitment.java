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
                if (m.joined && m.target == officer.id && officer.otherTask.equals("登用返程")) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean returning(World.Officer officer) {
        for (Mission m : this.missions) {
            if (m.joined && m.target == officer.id && m.owner == officer.owner && m.destination == officer.cityId && m.remaining > 0 && officer.otherTaskTurns > 0 && officer.otherTask.equals("登用返程")) {
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

    public int travelTurns(int source, int target) {
        World.City from = this.w.city(source);
        World.Officer officer = this.w.officer(target);
        World.City to = officer == null ? null : this.w.city(officer.cityId);
        if (from == null || to == null || from == to) {
            return 0;
        }
        return Math.min(50, Math.max(1, this.w.personnel.turns(from.id, to.id)));
    }

    World.Result start(World.City source, World.Officer actor, World.Officer target) {
        Mission m = new Mission(actor.id, target.id, source.id, target.cityId, source.owner, target.owner, travelTurns(source.id, target.id));
        this.missions.add(m);
        this.w.strategy.releaseGovernor(actor.id);
        actor.otherTask = "出使登用：" + target.name.substring(0, Math.min(60, target.name.length()));
        actor.otherTaskTurns = m.remaining;
        return this.w.success(actor.name + "前往" + this.w.city(m.destination).name + "登用" + target.name + "，去程" + m.travel + "旬，往返" + m.remaining + "旬");
    }

    public String describe(Mission m) {
        World.Officer actor = this.w.officer(m.actor);
        World.Officer target = this.w.officer(m.target);
        return (actor == null ? "使者" : actor.name) + " → " + (target == null ? "目标" : target.name) + " · " + this.w.city(m.destination).name + (m.remaining > m.travel ? " · 去程" : " · 返程") + " · 剩" + m.remaining + "旬" + (m.joined ? " · 已说服，同路返回" : "");
    }

    private boolean actorAvailable(Mission m, World.Officer actor) {
        return actor != null && this.w.life.present(actor.id) && actor.owner == m.owner && !this.w.government.captive(actor.id) && actor.unitId < 0 && actor.otherTaskTurns > 0 && (actor.otherTask.startsWith("出使登用：") || actor.otherTask.equals("登用返程")) && this.w.city(m.source).owner == m.owner;
    }

    private void release(World.Officer officer, Mission m) {
        if (officer == null || officer.owner != m.owner || !this.w.life.present(officer.id) || this.w.government.captive(officer.id) || officer.unitId >= 0) {
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
                    if (m.joined) {
                        release(target, m);
                    }
                    this.missions.remove(m);
                    this.w.note("登用使节因驻城或人员状态变化中止，人员返回可用据点");
                } else {
                    m.remaining--;
                    if (m.remaining == m.travel) {
                        boolean valid = target != null && target.owner == m.targetOwner && target.cityId == m.destination && this.w.strategy.recruitable(m.owner, m.target);
                        int chance = valid ? this.w.strategy.recruitChance(m.owner, m.actor, m.target) : 0;
                        if (valid && chance > 0 && this.w.strategy.nextInt(100) < chance) {
                            this.w.strategy.join(actor, target, m.destination);
                            m.joined = true;
                            target.otherTask = "登用返程";
                            target.otherTaskTurns = m.remaining + 1;
                            this.w.note(target.name + "接受登用，与" + actor.name + "返程，尚需" + m.remaining + "旬");
                        } else {
                            this.w.note(actor.name + (valid ? "登用未成功" : "抵达时目标已移动或不可登用") + "，返程尚需" + m.remaining + "旬");
                        }
                        actor.otherTask = "登用返程";
                    }
                    if (m.remaining == 0) {
                        release(actor, m);
                        if (m.joined) {
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

    void validate() throws IOException {
        Set<Integer> actors=new HashSet<>();Set<String> targets=new HashSet<>();
        for(Mission m:missions) {
            if(w.officer(m.actor)==null||w.officer(m.target)==null||w.city(m.source)==null||w.city(m.destination)==null
                ||m.source==m.destination||m.owner<0||m.owner>=w.factions.length||m.targetOwner< -1||m.targetOwner>=w.factions.length
                ||m.travel<1||m.travel>50||m.remaining<1||m.remaining>m.travel*2||m.joined&&m.remaining>m.travel
                ||!actors.add(m.actor)||!targets.add(m.owner+":"+m.target))throw new IOException("登用任务字段或引用无效");
        }
    }
}
