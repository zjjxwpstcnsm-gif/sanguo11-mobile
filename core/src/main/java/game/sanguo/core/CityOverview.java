package game.sanguo.core;

import java.util.*;

/** Read-only national administration snapshot. Border contact and approaching armies are distinct. */
public final class CityOverview {
    public static final String[] FILTERS={"全部状态","敌境接壤","敌军逼近","缺粮（不足6旬）","缺将（无驻将）","治安不足65","托管受阻","物流异常","有在途援助"};
    public static final String[] SORTS={"己方优先","金最多","粮最多","兵最多","驻将最多","前线优先","粮食续航最少","闲将最少","治安最低"};
    private final World w;private final Territory territory;private final DistrictManagement management;private final CampaignAi ai;
    public CityOverview(World w){this.w=w;territory=new Territory(w);management=new DistrictManagement(w);ai=new CampaignAi(w);}
    public int idle(World.City c){int n=0;for(World.Officer o:w.officers)if(o.owner==c.owner&&o.cityId==c.id&&!o.acted&&!w.domestic.busy(o.id)&&!w.strategy.busy(o.id)&&!w.government.captive(o.id))n++;return n;}
    public List<World.City> cities(int filter,int district,int sort,String query,int owner){
        List<World.City> out=new ArrayList<>();String q=query==null?"":query.trim();
        for(World.City c:w.cities){Districts.District d=w.districts.city(c.id);
            if(owner==-2?c.owner!=-1:owner>=0&&c.owner!=owner)continue;
            if(!q.isEmpty()&&!c.name.contains(q)&&!w.faction(c.owner).contains(q))continue;
            if(district==0&&(c.owner!=w.player||d!=null)||district>0&&(d==null||d.id!=district))continue;
            if(filter==7&&!management.logisticsBlocked(c)||filter==8&&management.incomingFood(c)==0)continue;
            if(filter==1&&!territory.frontline(c.id)||filter==2&&ai.incoming(c)==0||filter==3&&management.foodTurns(c)>=6||filter==4&&management.residents(c)>0||filter==5&&c.order>=65||filter==6&&(d==null||management.reason(c).isEmpty()))continue;
            out.add(c);
        }
        Comparator<World.City> compare;
        switch(sort){case 1:compare=Comparator.comparingInt(c->-c.gold);break;case 2:compare=Comparator.comparingInt(c->-c.food);break;case 3:compare=Comparator.comparingInt(c->-c.troops);break;case 4:compare=Comparator.comparingInt(c->-management.residents(c));break;case 5:compare=Comparator.comparingInt(c->ai.incoming(c)>0?0:territory.frontline(c.id)?1:2);break;case 6:compare=Comparator.comparingInt(management::foodTurns);break;case 7:compare=Comparator.comparingInt(this::idle);break;case 8:compare=Comparator.comparingInt(c->c.order);break;default:compare=Comparator.comparingInt(c->c.owner==w.player?0:1);}
        out.sort(compare.thenComparingInt(c->c.id));return out;
    }
    public String detail(World.City c){Districts.District d=w.districts.city(c.id);String reason=management.reason(c);
        return "金 "+c.gold+" · 粮 "+c.food+" · 约"+management.foodTurns(c)+"旬\n兵 "+c.troops+" · 闲将 "+idle(c)+" · 治安 "+c.order+"\n"+(d==null?"直属":d.name())+" · "+(territory.frontline(c.id)?"敌境接壤":"非接壤")+(ai.incoming(c)>0?" · 敌军逼近":" · 无敌军逼近")+"\n在途粮 "+management.incomingFood(c)+(reason.isEmpty()?"":"\n⚠ "+reason)+"  › 定位";
    }
}
