package game.sanguo.core;

import java.util.*;

/** Read-only, one-pass national accounts. No queries consume RNG, AP or mutate a game world. */
public final class RealmOverview {
    public static final String FORECAST_NOTE="下旬收支为当前兵力、驻地和设施不变时的预计值；已计库存上限。建设、征兵、研究、交易、俘获及调动等临时支出不在预测内。当前规则无每旬固定金钱维护费。";
    public static final class Faction {
        public final int id;
        public final String name;
        public boolean alive;
        public int cities,ports,gates,officers,prisoners,units,convoys,techs,abilities,projects;
        public long gold,food,troops,garrison,fieldTroops,cargoGold,cargoFood;
        public long goldIncome,foodIncome,foodUse,monthGold,seasonFood;
        public final long goldUse=0; // Current engine has no automatic recurring gold upkeep.
        private Faction(int id,String name){this.id=id;this.name=name;}
        public long netGold(){return goldIncome-goldUse;}
        public long netFood(){return foodIncome-foodUse;}
        public int sites(){return cities+ports+gates;}
    }
    public final List<Faction> factions;
    /** Includes undelivered convoys waiting at a city, but never legacy duplicate cargo. */
    public final List<World.Unit> units;
    public RealmOverview(World w){
        List<Faction> rows=new ArrayList<>();for(int id=0;id<w.factions.length;id++)rows.add(new Faction(id,w.faction(id)));
        for(World.City c:w.cities){if(c.owner<0||c.owner>=rows.size())continue;Faction f=rows.get(c.owner);
            if(c.kind==World.SiteKind.CITY){f.cities++;f.alive=true;}else if(c.kind==World.SiteKind.PORT)f.ports++;else f.gates++;
            f.gold+=c.gold;f.food+=c.food;f.garrison+=c.troops;f.troops+=c.troops;
            long use=w.cityFoodUse(c);f.foodUse+=use;
            f.goldIncome+=Math.min(Math.max(0,w.campaign.goldCap(c)-c.gold),w.domestic.goldIncome(c.id,w.turn+1));
            f.foodIncome+=Math.min(Math.max(0L,w.campaign.foodCap(c)-Math.max(0L,c.food-use)),w.domestic.foodIncome(c.id,w.turn+1));
            f.monthGold+=w.domestic.monthlyGold(c.id);f.seasonFood+=w.domestic.monthlyFood(c.id);
        }
        List<World.Unit> armies=new ArrayList<>();Set<Integer> seen=new HashSet<>();
        for(World.Unit u:w.units)if(seen.add(u.id))armies.add(u);
        for(Domestic.Mission m:w.domestic.missions)if(m.transport&&!m.legacyOverlap&&seen.add(m.id))armies.add(m);
        for(World.Unit u:armies){if(u.owner<0||u.owner>=rows.size())continue;Faction f=rows.get(u.owner);
            f.units++;if(u instanceof Domestic.Mission)f.convoys++;
            f.gold+=u.gold;f.food+=u.food;f.cargoGold+=u.gold;f.cargoFood+=u.food;
            f.troops+=u.troops;f.fieldTroops+=u.troops;f.foodUse+=Logistics.foodUse(w,u);
        }
        for(World.Officer o:w.officers)if(o.owner>=0&&o.owner<rows.size()&&w.life.present(o.id)){
            Faction f=rows.get(o.owner);f.officers++;if(w.government.captive(o.id))f.prisoners++;
        }
        for(Faction f:rows){
            f.alive=w.alive(f.id);
            for(Campaign.Tech t:Campaign.Tech.researchable())if(w.campaign.has(f.id,t))f.techs++;
            for(AbilityResearch.Node n:w.abilities.visible(f.id))if(w.abilities.learned(f.id,n.id))f.abilities++;
            for(Campaign.Project p:w.campaign.projects())if(p.owner==f.id)f.projects++;
            if(w.abilities.research(f.id)!=null)f.projects++;
            for(AbilityResearch.Training t:w.abilities.training())if(t.owner==f.id)f.projects++;
        }
        factions=Collections.unmodifiableList(rows);units=Collections.unmodifiableList(armies);
    }
    public static String siteType(World.SiteKind kind){return kind==World.SiteKind.CITY?"城池":kind==World.SiteKind.PORT?"港口":"关卡";}
    public static String commander(World w,World.Unit u){World.Officer o=w.officer(u.officerId);return o==null?"无主将":o.name;}
    public static String unitType(World.Unit u){return u instanceof Domestic.Mission?"运输队":u.weapon.label;}
    public static String crew(World w,World.Unit u){StringJoiner s=new StringJoiner(" / ");s.add(commander(w,u));for(int id:u.deputies){World.Officer o=w.officer(id);if(o!=null)s.add(o.name);}return s.toString();}
    public static String researchSummary(World w,int side){
        StringJoiner out=new StringJoiner("\n");
        for(Campaign.Project p:w.campaign.projects())if(p.owner==side){World.Officer o=w.officer(p.officerId);World.City c=w.city(p.cityId);
            out.add(p.label()+" · "+(c==null?"—":c.name)+" · "+(o==null?"—":o.name+" · 剩"+o.otherTaskTurns+"旬"));}
        AbilityResearch.Research r=w.abilities.research(side);
        if(r!=null)out.add("能力研究 "+AbilityResearch.node(r.nodeId).label+" · 剩"+r.remaining+"旬");
        for(AbilityResearch.Training t:w.abilities.training())if(t.owner==side){World.Officer o=w.officer(t.officerId);out.add(t.label()+" · "+(o==null?"—":o.name+" · 剩"+o.otherTaskTurns+"旬"));}
        return out.length()==0?"当前没有进行中的研究或培养":out.toString();
    }
}
