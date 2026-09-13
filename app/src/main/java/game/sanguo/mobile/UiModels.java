package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Read-only presentation of engine state. No commands or resource rules live here. */
final class UiModels {
    private UiModels() {}
    static String location(World w, World.Officer o) {
        for (Domestic.Mission m : w.domestic.missions) if (m.officerId == o.id)
            return w.city(m.sourceCity).name + " → " + w.city(m.targetCity).name;
        if (o.unitId >= 0) return "战场";
        World.City c = w.city(o.cityId);
        return c == null ? "已退出战场" : c.name;
    }
    static String status(World w, World.Officer o) {
        Strategy.OfficerState state=w.strategy.officerState(o.id);
        switch(state.activity){
            case CONSTRUCTION: case TRANSFER: case TRANSPORT: return w.domestic.assignment(o.id);
            case OTHER_TASK: return o.otherTask+" · 剩"+state.remainingTurns+"旬";
            case DEPLOYED: {World.Unit unit=w.unit(o.unitId);return unit!=null&&unit.acted?"出征 · 已行动":"出征 · 待命";}
            case UNAFFILIATED: return "在野 · 待登用";
            case UNAVAILABLE: return "不可派遣";
            case ACTED: return "本旬已行动";
            default: return "闲置";
        }
    }
    static String governor(World w,int cityId){
        World.City c=w.city(cityId);World.Officer o=c==null?null:w.officer(c.governorId);
        return o==null?"未任命":o.name;
    }
    static String faction(World w,World.Officer o){return o.owner<0?"在野":w.faction(o.owner);}

    static List<World.Officer> officers(World w, String query, int owner, int city, int sort) {
        List<World.Officer> result = new ArrayList<>();
        for (World.Officer o : w.officers)
            if ((owner == -1 || (owner == -2 ? o.owner < 0 : o.owner == owner)) && (city < 0 || o.cityId == city) && o.name.contains(query.trim())) result.add(o);
        Comparator<World.Officer> order = sort == 0 ? Comparator.comparing(o -> o.name)
            : Comparator.comparingInt((World.Officer o) -> ability(o, sort)).reversed();
        result.sort(order.thenComparingInt(o -> o.id));
        return result;
    }
    static int ability(World.Officer o, int sort) {
        switch (sort) { case 1: return o.leadership; case 2: return o.war; case 3: return o.intelligence; case 4: return o.politics; default: return o.charm; }
    }
    static int officerCount(World w, int city) {
        int n = 0; for (World.Officer o : w.officers) if (o.cityId == city && w.city(city)!=null && o.owner==w.city(city).owner && o.owner>=0) n++; return n;
    }
    static String coreOfficer(World w, int city) {
        World.Officer best = null;
        for (World.Officer o : w.officers) if (o.cityId == city && o.owner>=0 && o.owner==w.city(city).owner && (best == null || o.leadership > best.leadership)) best = o;
        return best == null ? "暂无驻将" : best.name;
    }
    static List<World.City> cities(World w, int sort) {
        List<World.City> result = new ArrayList<>(w.cities);
        result.sort(Comparator.comparingInt((World.City c) -> sort == 1 ? c.gold : sort == 2 ? c.food : sort == 3 ? c.troops : sort == 4 ? officerCount(w, c.id) : (c.owner == w.player ? 1 : 0))
            .reversed().thenComparingInt(c -> c.id));
        return result;
    }
    static String cargo(Domestic.Mission m) {
        StringBuilder s = new StringBuilder("金 "+m.gold+" · 粮 "+m.food+" · 兵 "+m.troops);
        for (World.Weapon weapon : World.Weapon.values()) if (m.equipment[weapon.ordinal()] > 0)
            s.append(" · ").append(weapon.label).append("装 ").append(m.equipment[weapon.ordinal()]);
        return s.toString();
    }
    static final class Task {
        final long id; final String title, detail; final Hex location; final Domestic.Facility facility; final Domestic.Mission mission;
        Task(long id, String title, String detail, Hex location, Domestic.Facility f, Domestic.Mission m) {
            this.id=id;this.title=title;this.detail=detail;this.location=location;facility=f;mission=m;
        }
    }
    static List<Task> tasks(World w, int type) {
        List<Task> result = new ArrayList<>();
        if (type == 0 || type == 1) for (Domestic.Facility f : w.domestic.facilities) if (w.city(f.cityId).owner == w.player && f.remaining > 0) {
            String city = w.city(f.cityId).name;
            result.add(new Task(f.id, "建设 · "+f.kind.label+" · "+w.officer(f.builderId).name,
                city+" → "+city+"开发地\n施工中 · 剩余 "+f.remaining+" 旬", f.hex, f, null));
        }
        for (Domestic.Mission m : w.domestic.missions) if (m.owner == w.player && (type == 0 || type == (m.transport ? 3 : 2))) {
            int eta = w.domestic.eta(m);
            String status = eta < 0 ? "道路受阻 · 剩余旬数待定" : eta == 0 ? "等待入城 / 库存空间 · 剩余 0 旬（等待时间未定）" : "在途 · 预计剩余 "+eta+" 旬";
            result.add(new Task(10000000L+m.id, (m.transport?"运输":"调动")+" · "+w.officer(m.officerId).name,
                w.city(m.sourceCity).name+" → "+w.city(m.targetCity).name+"\n"+status+(m.transport?"\n"+cargo(m):""), m.hex, null, m));
        }
        return result;
    }
    static String turnSummary(World before, World after) {
        StringBuilder s = new StringBuilder(before.date()+" → "+after.date()+"\n第 "+(after.turn+1)+" 旬\n\n城池资源净变化（含收入、消耗与战事）\n");
        boolean changed = false;
        for (World.City a : before.cities) if (a.owner == before.player) {
            World.City b = after.city(a.id); if (b == null) continue;
            if (a.owner != b.owner) {s.append(a.name).append(" · 城池失守\n");changed=true;}
            else if (a.gold != b.gold || a.food != b.food || a.troops != b.troops) {
                s.append(a.name).append("：金 ").append(delta(b.gold-a.gold)).append(" · 粮 ").append(delta(b.food-a.food)).append(" · 兵 ").append(delta(b.troops-a.troops)).append('\n');changed=true;
            }
        }
        if (!changed) s.append("本旬城池资源无变化\n");
        for (World.City b : after.cities) if (b.owner == after.player && before.city(b.id) != null && before.city(b.id).owner != before.player) s.append("占领 ").append(b.name).append('\n');
        s.append("\n建设 / 在途进展\n"); boolean progress = false;
        for (Domestic.Facility f : before.domestic.facilities) if (before.city(f.cityId).owner == before.player && f.remaining > 0) {
            Domestic.Facility next = after.domestic.facility(f.id); progress=true;
            s.append(before.city(f.cityId).name).append(" · ").append(f.kind.label).append(next==null?"建设中止":next.remaining==0?"建设完成":"剩余 "+next.remaining+" 旬").append('\n');
        }
        for (Domestic.Mission m : before.domestic.missions) if (m.owner == before.player) {
            Domestic.Mission next = after.domestic.mission(m.id); World.Officer o = after.officer(m.officerId);progress=true;
            s.append(before.officer(m.officerId).name).append(" · ");
            if (next != null) s.append(after.city(next.targetCity).name).append(" · ").append(after.domestic.status(next));
            else if (o != null && o.cityId >= 0) {s.append("抵达").append(after.city(o.cityId).name); if (m.transport) s.append("\n  入库：").append(cargo(m));}
            else s.append("任务终止");
            s.append('\n');
        }
        if (!progress) s.append("本旬没有建设或在途任务\n");
        if (after.gameOver()) s.append(after.winner==after.player?"\n战场胜利":"\n战场战败");
        return s.toString();
    }
    private static String delta(int n) {return n > 0 ? "+"+n : Integer.toString(n);}
}
