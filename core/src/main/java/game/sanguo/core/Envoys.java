package game.sanguo.core;

import game.sanguo.core.Campaign;
import game.sanguo.core.Envoys;
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

public final class Envoys {
    private final List<Mission> missions = new ArrayList();
    private Mission resolving;
    private final World w;

    public enum Kind {
        GOODWILL,
        TREATY,
        RUMOR,
        SURRENDER,
        EXCHANGE,
        AID
    }

    public static final class Mission {
        public final int a;
        public final int actor;
        public final int b;
        public final int c;
        public final int destination;
        public int escrow;
        public final Kind kind;
        public final int owner;
        public int remaining;
        public final int source;
        public final int targetOwner;
        public final int travel;

        Mission(Kind kind, int actor, int source, int destination, int owner, int targetOwner, int a, int b, int c, int travel, int escrow) {
            this.kind = kind;
            this.actor = actor;
            this.source = source;
            this.destination = destination;
            this.owner = owner;
            this.targetOwner = targetOwner;
            this.a = a;
            this.b = b;
            this.c = c;
            this.travel = travel;
            this.escrow = escrow;
            this.remaining = travel * 2;
        }
    }

    Envoys(World w) {
        this.w = w;
    }

    public List<Mission> missions() {
        return Collections.unmodifiableList(this.missions);
    }

    boolean arriving() {
        return this.resolving != null;
    }

    boolean resolving(int actor) {
        return this.resolving != null && this.resolving.actor == actor;
    }

    boolean assigned(World.Officer o) {
        for (Mission m : this.missions) {
            if (m.actor == o.id && m.owner == o.owner && o.otherTask.startsWith("外交")) {
                return true;
            }
        }
        return false;
    }

    void gift(World.City source, int gold) {
        if (this.resolving == null) {
            source.gold -= gold;
        } else {
            this.resolving.escrow = 0;
        }
    }

    public World.Result dispatch(Kind kind, int source, int actor, int destination, int a, int b, int c, int fee, int gift, int ap) {
        World.City from = this.w.city(source);
        World.City to = this.w.city(destination);
        if (to == null) {
            return this.w.fail("目标势力没有可出使的据点");
        }
        for (Mission m : this.missions) {
            if (m.kind == kind && m.owner == from.owner) {
                if (m.a == a && m.remaining > m.travel) {
                    return this.w.fail("已有使者在前往同一目标，请等待抵达");
                }
            }
        }
        int travel = Math.max(1, this.w.personnel.turns(source, destination));
        if (travel > 50) {
            return this.w.fail("出使路线过长");
        }
        World.Officer o = this.w.officer(actor);
        this.w.spend(from, o, fee + gift);
        int[] iArr = this.w.actionPoints;
        int i = from.owner;
        iArr[i] = iArr[i] - (ap - 10);
        Mission m2 = new Mission(kind, actor, source, destination, from.owner, to.owner, a, b, c, travel, gift);
        this.missions.add(m2);
        this.w.strategy.releaseGovernor(actor);
        o.otherTask = "外交出使";
        o.otherTaskTurns = m2.remaining;
        return this.w.success(o.name + "出使" + to.name + "，单程" + travel + "旬，往返" + m2.remaining + "旬；抵达后交涉");
    }

    private World.Result resolve(Mission m) {
        switch(m.kind) {
            case GOODWILL: return w.campaign.goodwill(m.source,m.actor,m.a);
            case TREATY: return w.campaign.negotiate(m.source,m.actor,m.a,Campaign.TreatyKind.values()[m.b],m.c);
            case RUMOR: return w.campaign.rumor(m.source,m.actor,m.a);
            case SURRENDER: return w.diplomacy.surrender(m.source,m.actor,m.a);
            case EXCHANGE: return w.diplomacy.exchange(m.source,m.actor,m.a,m.b,m.c);
            case AID: return w.diplomacy.requestAid(m.source,m.actor,m.a,m.b,m.c);
            default: throw new IllegalStateException();
        }
    }

    private void refund(Mission m) {
        if (m.escrow > 0) {
            World.City c = this.w.city(m.source);
            if (c == null || c.owner != m.owner) {
                c = this.w.government.refuge(m.owner, this.w.city(m.destination).hex);
            }
            if (c != null) {
                int paid = Math.min(m.escrow, Math.max(0, this.w.campaign.goldCap(c) - c.gold));
                c.gold += paid;
                m.escrow -= paid;
            }
            for (World.City other : this.w.cities) {
                if (m.escrow > 0 && other.owner == m.owner && other != c) {
                    int paid2 = Math.min(m.escrow, Math.max(0, this.w.campaign.goldCap(other) - other.gold));
                    other.gold += paid2;
                    m.escrow -= paid2;
                }
            }
        }
    }

    private void release(Mission m, World.Officer o) {
        refund(m);
        if (o != null && o.owner == m.owner && o.otherTask.startsWith("外交")) {
            o.otherTask = "";
            o.otherTaskTurns = 0;
            o.acted = true;
            if (this.w.city(m.source).owner != m.owner) {
                this.w.retreat(o, this.w.city(m.destination).hex);
            }
        }
    }


    void cancelOwner(int owner) { missions.removeIf(m->m.owner==owner); }

    void tick() {
        World world;
        String str;
        Iterator it = new ArrayList(this.missions).iterator();
        while (it.hasNext()) {
            Mission m = (Mission) it.next();
            if (this.missions.contains(m)) {
                World.Officer o = this.w.officer(m.actor);
                if (o == null || !this.w.life.present(o.id) || o.owner != m.owner || o.cityId != m.source || o.otherTaskTurns <= 0 || o.unitId >= 0 || this.w.government.captive(o.id) || !o.otherTask.startsWith("外交") || this.w.city(m.source).owner != m.owner) {
                    release(m, o);
                    this.missions.remove(m);
                } else if (!this.w.contests.busy() || m.remaining <= m.travel) {
                    m.remaining--;
                    if (m.remaining == m.travel) {
                        int active = this.w.active;
                        this.resolving = m;
                        this.w.active = m.owner;
                        try {
                            if (this.w.city(m.destination).owner == m.targetOwner) {
                                world = this.w;
                                str = resolve(m).message;
                            } else {
                                world = this.w;
                                str = "出使目的地已易主，交涉中止";
                            }
                            world.note(str);
                            this.resolving = null;
                            this.w.active = active;
                            refund(m);
                            o.otherTask = "外交返程";
                        } catch (Throwable th) {
                            this.resolving = null;
                            this.w.active = active;
                            throw th;
                        }
                    }
                    if (m.remaining == 0 && m.escrow > 0) {
                        refund(m);
                        if (m.escrow > 0) {
                            m.remaining = 1;
                            o.otherTaskTurns = 2;
                            this.w.note(o.name + "携返礼金等待据点金库腾出容量");
                        }
                    }
                    if (m.remaining == 0) {
                        release(m, o);
                        this.missions.remove(m);
                        this.w.note(o.name + "出使返城");
                    }
                } else {
                    o.otherTaskTurns = m.remaining + 1;
                }
            }
        }
    }

    public String describe(Mission m) {
        return this.w.officer(m.actor).name + " → " + this.w.city(m.destination).name + " · " + (m.remaining > m.travel ? "去程" : "返程") + " · 剩" + m.remaining + "旬";
    }

    void write(DataOutputStream out) throws IOException {
        out.writeInt(this.missions.size());
        for (Mission m : this.missions) {
            out.writeInt(m.kind.ordinal());
            int[] iArr = {m.actor, m.source, m.destination, m.owner, m.targetOwner, m.a, m.b, m.c, m.travel, m.remaining, m.escrow};
            for (int i = 0; i < 11; i++) {
                int v = iArr[i];
                out.writeInt(v);
            }
        }
    }

    void read(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > 10000) {
            throw new IOException("使节数量无效");
        }
        for (int i = 0; i < n; i++) {
            int k = in.readInt();
            if (k < 0 || k >= Kind.values().length) {
                throw new IOException("使节类型无效");
            }
            Mission m = new Mission(Kind.values()[k], in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), 0);
            m.remaining = in.readInt();
            m.escrow = in.readInt();
            this.missions.add(m);
        }
    }

    void validate() throws IOException {
        Set<Integer> actors = new HashSet<>();
        for (Mission m : this.missions) {
            if (this.w.officer(m.actor) == null || this.w.city(m.source) == null || this.w.city(m.destination) == null || m.owner < 0 || m.owner >= this.w.factions.length || m.targetOwner < 0 || m.targetOwner >= this.w.factions.length || m.travel < 1 || m.travel > 50 || m.remaining < 1 || m.remaining > m.travel * 2 || m.escrow < 0 || m.escrow > 10000 || !actors.add(Integer.valueOf(m.actor)) || (m.kind == Kind.TREATY && (m.b < 0 || m.b >= Campaign.TreatyKind.values().length))) {
                throw new IOException("使节字段或引用无效");
            }
        }
    }
}
