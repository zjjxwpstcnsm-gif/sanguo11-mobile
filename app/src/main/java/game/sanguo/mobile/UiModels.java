package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Read-only presentation of engine state. No commands or resource rules live here. */
final class UiModels {
    private UiModels() {}
    static String location(World w, World.Officer o) {
        if(!w.life.present(o.id))return w.life.state(o.id).label;
        if(w.government.captive(o.id))return w.government.locationLabel(w.government.prisoner(o.id));
        for (Domestic.Mission m : w.domestic.missions) if (m.officerId == o.id)
            return w.city(m.sourceCity).name + " → " + w.city(m.targetCity).name;
        if (o.unitId >= 0) return "战场";
        World.City c = w.city(o.cityId);
        return c == null ? "已退出战场" : c.name;
    }
    static String status(World w, World.Officer o) {
        Strategy.OfficerState state=w.strategy.officerState(o.id);
        switch(state.activity){
            case DEAD: return "已故";
            case UNAPPEARED: return "未登场 · "+w.life.life(o.id).appearance+"年";
            case CAPTIVE: return w.government.status(o.id);
            case CONSTRUCTION: case TRANSFER: case TRANSPORT: return w.domestic.assignment(o.id);
            case OTHER_TASK: return o.otherTask+" · 剩"+state.remainingTurns+"旬";
            case DEPLOYED: {World.Unit unit=w.unit(o.unitId);if(unit!=null&&unit.march!=null)return "行军 → "+w.marches.label(unit.march)+(unit.march.paused.isEmpty()?"":" · 暂停");return unit!=null&&unit.acted?"出征 · 已行动":"出征 · 待命";}
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
    static String faction(World w,World.Officer o){return !w.life.present(o.id)?w.life.state(o.id).label:o.owner<0?"在野":w.faction(o.owner);}

    static List<World.Officer> officers(World w, String query, int owner, int city, int sort) {
        List<World.Officer> result = new ArrayList<>();
        for (World.Officer o : w.officers)
            if ((owner == -1 || (owner == -2 ? o.owner < 0&&w.life.present(o.id) : o.owner == owner)) && (city < 0 || o.cityId == city) && o.name.contains(query.trim())) result.add(o);
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
    static List<World.City> cities(World w,int sort,String query,int owner){
        List<World.City> result=cities(w,sort);String needle=query.trim();
        result.removeIf(c->!(c.name.contains(needle)||w.faction(c.owner).contains(needle))||(owner>=0&&c.owner!=owner)||(owner==-2&&c.owner>=0));return result;
    }
    static List<Integer> factions(World w,String query){
        List<Integer> result=new ArrayList<>();for(int i=0;i<w.factions.length;i++)if(w.faction(i).contains(query.trim()))result.add(i);return result;
    }
    static List<Task> tasks(World w,int type,String query){List<Task> result=tasks(w,type);String needle=query.trim();result.removeIf(t->!t.title.contains(needle)&&!t.detail.contains(needle));return result;}
    static String cargo(Domestic.Mission m) {
        StringBuilder s = new StringBuilder("金 "+m.gold+" · 粮 "+m.food+" · 兵 "+m.troops);
        for (World.Weapon weapon : World.Weapon.values()) if (m.equipment[weapon.ordinal()] > 0)
            s.append(" · ").append(weapon.label).append("装 ").append(m.equipment[weapon.ordinal()]);
        return s.toString();
    }
    static final class Task {
        final long id; final String title, detail; final Hex location; final Domestic.Facility facility; final Domestic.Mission mission;
        Campaign.Project project;
        Army.Production production;
        AbilityResearch.Research abilityResearch;
        AbilityResearch.Training abilityTraining;
        World.Unit marching;
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
        if(type==0||type==5)for(Army.Production p:w.army.productions())if(p.owner==w.player){
            Task task=new Task(30000000L+p.officerId,p.label()+" · "+w.officer(p.officerId).name,w.city(p.cityId).name+" · 剩余 "+w.officer(p.officerId).otherTaskTurns+" 旬",w.city(p.cityId).hex,null,null);task.production=p;result.add(task);
        }
        if(type==0||type==4)for(Campaign.Project p:w.campaign.projects())if(p.owner==w.player){
            Task task=new Task(20000000L+p.officerId,p.label()+" · "+w.officer(p.officerId).name,
                w.city(p.cityId).name+" · 剩余 "+w.officer(p.officerId).otherTaskTurns+" 旬\n"+(p.tech!=null?p.tech.effect:"培养期间武将不能执行其他命令"),w.city(p.cityId).hex,null,null);
            task.project=p;result.add(task);
        }
        if(type==0||type==4){
            AbilityResearch.Research r=w.abilities.research(w.player);
            if(r!=null){Task t=new Task(40000000L+w.player,"PK研究"+AbilityResearch.node(r.nodeId).label,w.city(r.cityId).name+" · 剩余 "+r.remaining+" 旬",w.city(r.cityId).hex,null,null);t.abilityResearch=r;result.add(t);}
            for(AbilityResearch.Training p:w.abilities.training())if(p.owner==w.player){Task t=new Task(50000000L+p.officerId,p.label()+" · "+w.officer(p.officerId).name,w.city(p.cityId).name+" · 剩余 "+w.officer(p.officerId).otherTaskTurns+" 旬",w.city(p.cityId).hex,null,null);t.abilityTraining=p;result.add(t);}
        }
        if(type==0||type==6)for(World.Unit u:w.units)if(u.owner==w.player&&u.march!=null){
            Task t=new Task(60000000L+u.id,"行军 · "+w.officer(u.officerId).name,"目标 "+w.marches.label(u.march)+"\n"+(u.march.paused.isEmpty()?"每旬自动前进 · 可点地图改道":u.march.paused),u.hex,null,null);t.marching=u;result.add(t);
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
        for(World.Unit u:before.units)if(u.owner==before.player&&u.march!=null){
            World.Unit next=after.unit(u.id);s.append("\n行军 · ").append(before.officer(u.officerId).name).append("：").append(next==null?"部队已解编":next.march==null?"行军结束":next.march.paused.isEmpty()?"已前进至 "+next.hex:next.march.paused).append('\n');
        }
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
        for(Campaign.Project p:before.campaign.projects())if(p.owner==before.player){
            progress=true;World.Officer o=after.officer(p.officerId);boolean running=after.campaign.projects().stream().anyMatch(next->next.officerId==p.officerId);
            s.append(p.label()).append(" · ").append(before.officer(p.officerId).name).append(running?" · 剩余"+o.otherTaskTurns+"旬":o!=null&&o.owner==p.owner&&o.cityId==p.cityId?" · 已完成":" · 已中止").append('\n');
        }
        AbilityResearch.Research research=before.abilities.research(before.player);
        if(research!=null){
            progress=true;AbilityResearch.Research next=after.abilities.research(before.player);
            s.append("PK研究").append(AbilityResearch.node(research.nodeId).label).append(next!=null&&next.nodeId.equals(research.nodeId)?" · 剩余"+next.remaining+"旬":after.abilities.learned(before.player,research.nodeId)?" · 已完成":" · 已中止").append('\n');
        }
        for(AbilityResearch.Training t:before.abilities.training())if(t.owner==before.player){
            progress=true;boolean running=after.abilities.training().stream().anyMatch(next->next.officerId==t.officerId&&next.nodeId.equals(t.nodeId));
            boolean completed=after.abilities.remaining(t.owner,t.nodeId)<before.abilities.remaining(t.owner,t.nodeId);
            s.append(t.label()).append(" · ").append(before.officer(t.officerId).name).append(running?" · 剩余"+after.officer(t.officerId).otherTaskTurns+"旬":completed?" · 已完成":" · 已中止").append('\n');
        }
        for(Army.Production p:before.army.productions())if(p.owner==before.player){
            progress=true;boolean running=after.army.productions().stream().anyMatch(n->n.officerId==p.officerId);
            World.City c=after.city(p.cityId);boolean delivered=c!=null&&c.owner==p.owner&&(p.weapon!=null?c.equipment[p.weapon.ordinal()]>before.city(p.cityId).equipment[p.weapon.ordinal()]:c.ships[p.ship.ordinal()-1]>before.city(p.cityId).ships[p.ship.ordinal()-1]);
            s.append(p.label()).append(running?" · 制造中":delivered?" · 已入库":" · 已结束/中止，请查看日志").append("\n");
        }
        if (!progress) s.append("本旬没有建设、在途或研究任务\n");
        if (after.gameOver()) s.append(after.winner==after.player?"\n战场胜利":"\n战场战败");
        return s.toString();
    }
    private static String delta(int n) {return n > 0 ? "+"+n : Integer.toString(n);}
}
