package game.sanguo.core;

import java.io.IOException;
import java.util.*;

/** Runtime ownership is explicit and saved. The reference catalog never guesses opening placements. */
public final class Treasures {
    public enum Kind {
        HORSE("名马",Contests.Gear.HORSE,"单挑可退却；持有人免于部队溃败捕获"),
        SWORD("名剑",Contests.Gear.SWORD,"单挑斗志增长"),POLEARM("长柄",Contests.Gear.POLEARM,"单挑攻击增强"),
        HIDDEN("暗器",Contests.Gear.HIDDEN,"开放单挑暗器"),BOW("名弓",Contests.Gear.BOW,"开放单挑伪退却"),
        BOOK("书籍",Contests.Gear.BOOK,"舌战可使用全部五类话术"),
        SEAL("玉玺",null,"持有势力每月获得100技巧点"),BRONZE("铜雀",null,"持有势力可以开发铜雀台");
        public final String label,effect;public final Contests.Gear gear;
        Kind(String label,Contests.Gear gear,String effect){this.label=label;this.gear=gear;this.effect=effect;}
    }
    public enum Place { OFFICER, TREASURY, HIDDEN }
    public static final class Definition {
        public final String id,name;public final Kind kind;public final int value;
        Definition(String id,String name,Kind kind,int value){this.id=id;this.name=name;this.kind=kind;this.value=value;}
    }
    public static final class Item {
        public final Definition definition;public final Place place;public final int holder;
        Item(Definition definition,Place place,int holder){this.definition=definition;this.place=place;this.holder=holder;}
    }
    final SortedMap<String,Item> items=new TreeMap<>();
    private final World w;
    Treasures(World w){this.w=w;}
    public static List<Definition> catalog()throws IOException{
        List<Definition> result=new ArrayList<>();
        Map<String,Kind> kinds=new HashMap<>();String[] raw={"名馬","名劍","長柄","暗器","名弓","書籍","玉璽","銅雀"};
        for(int i=0;i<raw.length;i++)kinds.put(raw[i],Kind.values()[i]);
        for(ContentCatalog.Entry e:ContentCatalog.get().rows("items")){Kind kind=kinds.get(e.fields.get(2));if(kind==null)throw new IOException("未知宝物类别");result.add(new Definition(e.id,e.name,kind,Integer.parseInt(e.fields.get(3))));}
        return Collections.unmodifiableList(result);
    }
    public static Definition definition(String id)throws IOException{for(Definition d:catalog())if(d.id.equals(id))return d;throw new IOException("宝物ID不存在");}
    public List<Item> items(){return Collections.unmodifiableList(new ArrayList<>(items.values()));}
    public Item item(String id){return items.get(id);}
    public int owner(Item item){if(item==null)return -1;return item.place==Place.TREASURY?item.holder:item.place==Place.OFFICER&&w.officer(item.holder)!=null?w.officer(item.holder).owner:-1;}
    public List<Item> owned(int owner){List<Item> out=new ArrayList<>();for(Item i:items.values())if(owner(i)==owner)out.add(i);return out;}
    public List<Item> held(int officer){List<Item> out=new ArrayList<>();for(Item i:items.values())if(i.place==Place.OFFICER&&i.holder==officer)out.add(i);return out;}
    public boolean has(int officer,Kind kind){for(Item i:held(officer))if(i.definition.kind==kind)return true;return false;}
    public boolean factionHas(int owner,Kind kind){for(Item i:owned(owner))if(i.definition.kind==kind&&(i.place!=Place.OFFICER||!w.government.captive(i.holder)))return true;return false;}
    public int gearMask(int officer){int result=0;for(Item i:held(officer))if(i.definition.kind.gear!=null)result|=1<<i.definition.kind.gear.ordinal();return result;}
    void place(Definition d,Place p,int holder){items.put(d.id,new Item(d,p,holder));}
    public String location(Item i){return i.place==Place.OFFICER?w.officer(i.holder).name:i.place==Place.TREASURY?w.faction(i.holder)+"府库":w.city(i.holder).name+"未发现";}
    public String describe(int officer){StringBuilder out=new StringBuilder();for(Item i:held(officer))out.append(i.definition.name).append(" · ").append(i.definition.kind.label).append('\n');return out.length()==0?"无已持有宝物":out.toString().trim();}
    public World.Result award(int city,int actor,String id,int target){w.reports.prepare();
        World.City c=w.city(city);World.Officer o=w.officer(actor),t=w.officer(target);String error=w.cityError(c,o,0);if(error!=null)return w.fail(error);
        Item i=item(id);if(i==null||i.place!=Place.TREASURY||i.holder!=c.owner)return w.fail("请选择本势力府库中的宝物");
        if(t==null||t.owner!=c.owner||t.cityId!=city||t.unitId>=0||w.government.captive(target)||w.strategy.busy(target)||w.domestic.busy(target))return w.fail("请选择本城无任务的己方武将");
        w.spend(c,o,0);place(i.definition,Place.OFFICER,target);t.loyalty=Math.min(100,t.loyalty+i.definition.value);
        return w.success("赏赐"+t.name+"："+i.definition.name+"，忠诚提升至"+t.loyalty);
    }
    public World.Result confiscate(int city,int actor,String id){w.reports.prepare();
        World.City c=w.city(city);String error=w.cityError(c,w.officer(actor),0);if(error!=null)return w.fail(error);
        Item i=item(id);World.Officer t=i!=null&&i.place==Place.OFFICER?w.officer(i.holder):null;
        if(t==null||t.owner!=c.owner||t.cityId!=city||t.unitId>=0||w.government.captive(t.id)||w.strategy.busy(t.id)||w.domestic.busy(t.id))return w.fail("请选择本城无任务的己方持宝武将");
        w.spend(c,w.officer(actor),0);place(i.definition,Place.TREASURY,c.owner);
        w.loyalty.lose(t,i.definition.value);
        return w.success("收回"+i.definition.name+"入府库，"+t.name+"忠诚"+t.loyalty);
    }
    /** Called only after a real search has paid and failed to discover an officer. */
    String discover(int city,int actor,int roll){
        World.Officer o=w.officer(actor);
        for(Item i:new ArrayList<>(items.values()))if(i.place==Place.HIDDEN&&i.holder==city){
            if(roll>=Math.min(95,20+o.politics/2))return null;
            place(i.definition,Place.TREASURY,o.owner);return i.definition.name;
        }return null;
    }
    /** Strong-robbery roll precedes capture. A protected horse prevents capture for this defeat even if stolen. */
    void rob(World.Unit victor,List<World.Officer> defeated){
        if(victor==null||!w.skills.has(victor,Skill.QIANGDUO))return;
        List<Item> candidates=new ArrayList<>();for(World.Officer o:defeated)candidates.addAll(held(o.id));
        if(!candidates.isEmpty()&&w.strategy.nextInt(100)<20){Item i=candidates.get(w.strategy.nextInt(candidates.size()));place(i.definition,Place.OFFICER,victor.officerId);w.note("强夺："+w.officer(victor.officerId).name+"夺得"+i.definition.name);}
    }
    void captured(int officer,int captor){for(Item i:new ArrayList<>(held(officer)))place(i.definition,Place.TREASURY,captor);}
    void fallenTreasury(int former,int victor){if(former>=0&&!w.alive(former))for(Item i:new ArrayList<>(items.values()))if(i.place==Place.TREASURY&&i.holder==former)place(i.definition,Place.TREASURY,victor);}
    void tick(){if(w.turn%3==0)for(Domestic.Facility f:w.domestic.facilities)if(f.kind==Domestic.Kind.BRONZE_TERRACE&&f.remaining==0)w.campaign.earn(w.city(f.cityId).owner,100);
        if(w.turn%3==0)for(int side=0;side<w.factions.length;side++)if(w.alive(side)&&factionHas(side,Kind.SEAL)){w.campaign.earn(side,100);w.note(w.faction(side)+"持有玉玺，技巧+100");}}
    void validate()throws IOException{
        require(items.size()<=43,"宝物数量超限");
        for(Map.Entry<String,Item> e:items.entrySet()){
            Item i=e.getValue();Definition d=i.definition;
            require(d!=null&&d.id.matches("item-0(?:[0-3][0-9]|4[0-2])")&&d.id.equals(e.getKey())&&d.name!=null&&!d.name.trim().isEmpty()&&d.name.length()<=80&&d.kind!=null&&d.value>=0&&d.value<=100,"宝物定义无效");
            require(i.place!=null&&i.holder>=0,"宝物持有位置无效");
            require(i.place==Place.OFFICER?w.officer(i.holder)!=null:i.place==Place.TREASURY?i.holder<w.factions.length:w.city(i.holder)!=null,"宝物持有者缺失");
        }
    }
    private static void require(boolean ok,String msg)throws IOException{if(!ok)throw new IOException(msg);}
}
