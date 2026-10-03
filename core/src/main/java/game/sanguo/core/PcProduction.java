package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.*;

/** Original ordinary production rules; profile exists only in explicitly new games.
 * Source30d33b44,5c63d0/5c6350/5c65b0. Delayed scheduling remains separate. */
public final class PcProduction {
    public static final String SOURCE="30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb";
    private static final String PRICE_SHA="eb73f93c358be45bf40aebf286e930a80de0b9fd47429300cb9b78fc7a9c850f";
    private static final String FLAGS_SHA="469e0fe812bf20661c0427b3ff8d79c0be51e7080411a340fdaa4e6598d46ed1";
    private static final int MAGIC=0x50525031;
    private final World w;
    private boolean enabled;
    private final int[] prices=new int[12];
    private final Map<Integer,int[]> flags=new TreeMap<>();
    PcProduction(World world){w=world;}
    public boolean enabled(){return enabled;}
    public void initializeOpening(){
        if(!w.officerAbilities.enabled()||!w.merchantMarket.enabled())throw new IllegalStateException("生产来源模式需要新局能力与行情状态");
        Map<Integer,int[]> source=new HashMap<>();
        try{
            for(String line:rows("pc-production-prices.tsv")){String[] cells=line.split("\\t");prices[Integer.parseInt(cells[0])]=Integer.parseInt(cells[1]);}
            for(String line:rows("pc-city-production-flags.tsv")){String[] cells=line.split("\\t");source.put(Integer.parseInt(cells[0]),Arrays.stream(cells[1].split(",")).mapToInt(Integer::parseInt).toArray());}
        }catch(IOException|RuntimeException e){throw new IllegalStateException("原生产来源数据无效",e);}
        if(source.size()!=42||prices[1]!=700||prices[10]!=800)throw new IllegalStateException("原生产来源数量无效");
        flags.clear();for(World.City city:w.cities)flags.put(city.id,source.getOrDefault(city.id,new int[6]).clone());enabled=true;
    }
    private static List<String> rows(String file)throws IOException{
        InputStream in=PcProduction.class.getResourceAsStream("/rules/"+file);if(in==null)throw new IOException("缺少原生产数据");
        byte[] raw;try(in){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];for(int count;(count=in.read(buffer))!=-1;)bytes.write(buffer,0,count);raw=bytes.toByteArray();}try{StringBuilder value=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(raw)){int n=b&255;value.append("0123456789abcdef".charAt(n>>>4)).append("0123456789abcdef".charAt(n&15));}String digest=value.toString();if(!digest.equals(file.equals("pc-production-prices.tsv")?PRICE_SHA:FLAGS_SHA))throw new IOException("原生产数据SHA不符");}catch(NoSuchAlgorithmException e){throw new IOException(e);}
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(new ByteArrayInputStream(raw),StandardCharsets.UTF_8))){List<String> out=new ArrayList<>();reader.readLine();for(String line;(line=reader.readLine())!=null;)if(!line.isBlank())out.add(line);return out;}
    }
    public static int nativeItem(World.Weapon weapon){
        if(weapon==null)return -1;
        return switch(weapon){case SWORD->0;case SPEAR->1;case HALBERD->2;case CROSSBOW->3;case CAVALRY->4;case RAM->5;case SIEGE_TOWER->6;case CATAPULT->7;case WOODEN_BEAST->8;};
    }
    static int nativeItem(Army.Ship ship){return ship==null?-1:ship.ordinal()+9;}
    public int gold(int city,int item){
        if(!enabled||item<0||item>=12)throw new IllegalArgumentException("原生产物品无效");
        int category=item==0?-1:item<=4?item-1:item<=8?4:5;
        int[] values=flags.get(city);if(values==null)throw new IllegalStateException("生产城市来源状态缺失");
        return category>=0&&values[category]>0?prices[item]*8/10:prices[item];
    }
    public int[] categoryFlags(int city){int[] values=flags.get(city);return values==null?null:values.clone();}
    /** Native integer basis and skill multiplier precede float32 coefficient/cast. */
    public static int quantity(int[] current,boolean doubled,int low,int middle,int remaining){
        if(current==null||current.length<1||current.length>3||low<0||middle<0||remaining<1)throw new IllegalArgumentException("生产执行者或设施次数无效");
        int sum=0,max=0;for(int value:current){if(value<1||value>100)throw new IllegalArgumentException("原当前智力范围无效");sum+=value;max=Math.max(max,value);}
        int basis=(max+sum+200)*5*(doubled?2:1);float coefficient=remaining>low+middle?1.5f:remaining>low?1.2f:1f;
        return (int)(basis*(double)coefficient);
    }
    /** Original5c6ca0 counter, not a promise that the whole force turn is aligned.
     * Facility45 naval branch is an explicit input; it is not guessed from a label. */
    public static int delayedCounter(int[] current,int item,boolean skill82,boolean skill83,boolean nativeFacility45){
        if(current==null||current.length<1||current.length>3||item<5||item>11)throw new IllegalArgumentException("制造执行者或兵装无效");
        int sum=0,max=0;for(int value:current){if(value<1||value>100)throw new IllegalArgumentException("制造当前智力越界");sum+=value;max=Math.max(max,value);}
        int basis=item==9?24:Math.max(24,max+sum-200);int count=10-basis/24;
        if(item>=9&&nativeFacility45)count=Math.max(2,8-basis/24);
        if(item<=8&&skill82||item>=10&&skill83)count/=2;
        return count;
    }
    int taskCounter(List<World.Officer> actors,World.Weapon weapon,Army.Ship ship){
        int[] current=actors.stream().mapToInt(o->o.intelligence).toArray();return delayedCounter(current,ship==null?nativeItem(weapon):nativeItem(ship),actors.stream().anyMatch(o->w.skills.has(o,Skill.FAMING)),actors.stream().anyMatch(o->w.skills.has(o,Skill.ZAOCHUAN)),false);
    }
    int amount(int city,List<World.Officer> actors,World.Weapon weapon,boolean forecast){
        Domestic.Kind kind=Domestic.productionFacility(weapon);int low=0,middle=0;
        for(Domestic.Facility f:w.domestic.facilities)if(f.cityId==city&&f.kind==kind&&f.hp>0){
            if(f.upgradeTo==2||f.upgradeTo==0&&f.remaining==0&&f.level==1)low++;
            else if(f.upgradeTo==3||f.upgradeTo==0&&f.remaining==0&&f.level==2)middle++;
        }
        int[] current=actors.stream().mapToInt(o->forecast?w.officerAbilities.afterExperience(o.id,2,2):o.intelligence).toArray();
        Skill skill=weapon==World.Weapon.CAVALRY?Skill.FANZHI:Skill.NENGLI;boolean doubled=actors.stream().anyMatch(o->w.skills.has(o,skill));
        return quantity(current,doubled,low,middle,w.domestic.remainingUses(city,kind));
    }
    void write(DataOutputStream out)throws IOException{
        if(!enabled)throw new IOException("生产来源模式未启用");out.writeInt(MAGIC);out.writeUTF(SOURCE);for(int value:prices)out.writeInt(value);
        out.writeInt(flags.size());for(var entry:flags.entrySet()){out.writeInt(entry.getKey());for(int value:entry.getValue())out.writeByte(value);}
        out.writeInt(w.army.productions.size());for(Army.Production p:w.army.productions){out.writeBoolean(p.nativePolicy);if(p.nativePolicy){out.writeInt(p.facilityId);out.writeInt(p.officers().length);for(int id:p.officers())out.writeInt(id);}}
    }
    void read(DataInputStream in)throws IOException{
        if(in.readInt()!=MAGIC||!SOURCE.equals(in.readUTF()))throw new IOException("未知生产来源模式");
        for(int i=0;i<12;i++){int value=in.readInt();if(value<0||value>65535)throw new IOException("生产费用越界");prices[i]=value;}
        try{int[] expected=new int[12];for(String row:rows("pc-production-prices.tsv")){String[] fields=row.split("\\t");expected[Integer.parseInt(fields[0])]=Integer.parseInt(fields[1]);}if(!Arrays.equals(prices,expected))throw new IOException("存档生产价格与来源版本不符");}catch(RuntimeException e){throw new IOException("生产价格格式无效",e);}
        int size=in.readInt();if(size!=w.cities.size())throw new IOException("生产城市状态数量不符");flags.clear();
        for(int i=0;i<size;i++){int id=in.readInt();if(w.city(id)==null||flags.containsKey(id))throw new IOException("生产城市状态引用无效");int[] values=new int[6];for(int n=0;n<6;n++)values[n]=in.readUnsignedByte();flags.put(id,values);}
        int jobs=in.readInt();if(jobs!=w.army.productions.size())throw new IOException("制造策略数量不符");for(Army.Production p:w.army.productions){p.nativePolicy=in.readBoolean();if(p.nativePolicy){p.facilityId=in.readInt();int count=in.readInt();if(count<1||count>3)throw new IOException("制造执行者数量无效");p.crew=new int[count];for(int n=0;n<count;n++)p.crew[n]=in.readInt();if(p.crew[0]!=p.officerId)throw new IOException("制造主执行者不符");}}enabled=true;
    }
    void validate()throws IOException{if(enabled&&(!w.officerAbilities.enabled()||!w.merchantMarket.enabled()||flags.size()!=w.cities.size()))throw new IOException("生产来源模式状态不完整");}
}
