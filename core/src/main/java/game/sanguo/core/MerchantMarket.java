package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Explicit new-opening price state. Loading v31-v34 never invents a market rate.
 * Rule probabilities/branches are pinned; global PC random-stream parity remains
 * separate work. Production uses the existing saved Strategy RNG, never a UI RNG. */
public final class MerchantMarket {
    private static final int MAGIC=0x4d4b5431;
    private final World w;
    private boolean enabled;
    private int lastMonth=-1;
    MerchantMarket(World w){this.w=w;}
    public boolean enabled(){return enabled;}
    public int rate(int city){World.City c=w.city(city);return enabled&&c!=null?c.merchantRate:-1;}
    private PcMerchantRules.Draws draws(){return new PcMerchantRules.Draws(){
        public int uniform(int bound){return bound<2?0:w.strategy.nextInt(bound);}
        public boolean percent(int chance){return chance>0&&w.strategy.nextInt(100)<chance;}
    };}
    void initializeOpening(){
        if(enabled||!w.officerAbilities.enabled())throw new IllegalStateException("行情初始化须是有基础能力的新局");
        List<World.City> cities=ordered();PcMerchantRules.Draws rng=draws();
        for(World.City c:cities)if(c.kind==World.SiteKind.CITY)c.merchantRate=PcMerchantRules.initialRate(w.startMonth,c.merchantRate,rng);
        lastMonth=w.turn/3;enabled=true;
    }
    private List<World.City> ordered(){List<World.City> cities=new ArrayList<>(w.cities);cities.sort(Comparator.comparingInt(c->c.id));return cities;}
    /** Original58f535..58f582 clears harvest in August, before monthly prices.
     * No disaster lifetime is invented; plague/locust read the saved event state. */
    void beforeWeather(){if(enabled&&w.turn%3==0&&(w.startMonth-1+w.turn/3)%12+1==8)for(World.City c:w.cities)c.merchantHarvest=false;}
    void harvest(World.City city){if(enabled&&city!=null&&city.kind==World.SiteKind.CITY)city.merchantHarvest=true;}
    int conditions(World.City city){
        int flags=city.merchantHarvest?4:0;WorldEvents.Hazard h=w.events.hazards.get(city.id);
        if(h!=null&&h.until>w.turn)flags|=h.kind==WorldEvents.Disaster.PLAGUE?1:2;
        return flags;
    }
    void tick(){
        if(!enabled||w.turn%3!=0||lastMonth==w.turn/3)return;
        PcMerchantRules.Draws rng=draws();
        for(World.City c:ordered())if(c.kind==World.SiteKind.CITY)c.merchantRate=PcMerchantRules.monthlyRate(c.merchantRate,conditions(c),rng);
        lastMonth=w.turn/3;
    }
    /** Native stock formula, extended only to retain authored/legacy gold above
     * 100000: buying can debit it, selling cannot credit beyond the native cap. */
    int maximum(World.City c,World.Officer o,boolean buy){
        if(c==null||o==null||c.kind!=World.SiteKind.CITY||c.merchantRate<1)return 0;
        if(c.gold<=PcCityCapacities.GOLD)return PcMerchantRules.maximum(buy,c.gold,c.food,o.politics,c.merchantRate);
        if(!buy)return 0;
        return (int)Math.max(0,Math.min(c.gold*(long)c.merchantRate*400/((450-o.politics)*10L),PcCityCapacities.FOOD-c.food));
    }
    void write(DataOutputStream d)throws IOException{
        d.writeInt(MAGIC);d.writeInt(lastMonth);d.writeInt(w.cities.size());
        for(World.City c:w.cities){d.writeInt(c.id);d.writeInt(c.merchantRate);d.writeBoolean(c.merchantHarvest);}
    }
    void read(DataInputStream d)throws IOException{
        if(d.readInt()!=MAGIC)throw new IOException("行情段标记无效");lastMonth=d.readInt();
        if(d.readInt()!=w.cities.size())throw new IOException("行情据点条数错误");
        for(World.City c:w.cities){if(d.readInt()!=c.id)throw new IOException("行情据点身份错误");c.merchantRate=d.readInt();c.merchantHarvest=d.readBoolean();}
        enabled=true;
    }
    void validate()throws IOException{
        if(enabled&&(!w.officerAbilities.enabled()||lastMonth<0||lastMonth>w.turn/3))throw new IOException("行情时钟或能力模式错误");
        for(World.City c:w.cities)if(c.merchantRate<0||c.merchantRate>255||!enabled&&(c.merchantRate!=0||c.merchantHarvest))throw new IOException("行情值或旧档模式错误");
    }
}
