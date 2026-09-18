package game.sanguo.core;

import java.io.*;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

/** Strict, UTF-8, data-only scenario format. No code execution or Android dependency. */
public final class ScenarioData {
    private static final int MAX_BYTES=1024*1024;
    private ScenarioData() {}
    public static World read(InputStream input,int player)throws IOException {
        if(input==null)throw new IOException("剧本资源缺失");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
        while((count=input.read(buffer))!=-1){if(bytes.size()+count>MAX_BYTES)throw new IOException("剧本数据过大");bytes.write(buffer,0,count);}
        byte[] raw=bytes.toByteArray();
        Properties p=new Properties(){
            @Override public synchronized Object put(Object key,Object value){
                if(containsKey(key))throw new IllegalArgumentException("重复字段："+key);
                return super.put(key,value);
            }
        };
        try {
            p.load(new InputStreamReader(new ByteArrayInputStream(raw),StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)));
            int sourceWidth=MapCoordinates.normalize(p);
            number(p,"format",1,1);
            String id=take(p,"id"),name=take(p,"name"),source=take(p,"source");
            if(!id.matches("[a-z0-9-]{1,80}"))throw new IOException("剧本ID无效");
            // Only researched formats should be added here; current packs make no original-data claim.
            if(!source.equals("engineering-original")&&!source.equals("community-reference")&&!source.equals("user-supplied"))throw new IOException("未知数据来源等级");
            String reference=source.equals("community-reference")?take(p,"reference"):null;
            if(reference!=null&&!reference.equals("rlu-officers"))throw new IOException("未知人物资料来源");
            boolean referenceDetails=p.containsKey("reference-details")&&number(p,"reference-details",0,1)==1;
            boolean referenceDates=!p.containsKey("reference-dates")||number(p,"reference-dates",0,1)==1;
            if(!referenceDates&&!referenceDetails)throw new IOException("忽略生卒的资料沙盘需要完整人物资料");
            if(referenceDetails&&reference==null)throw new IOException("完整人物资料需要显式来源");
            int revision=number(p,"revision",1,1000000),year=number(p,"year",1,9999),month=number(p,"month",1,12);
            int width=number(p,"width",1,300),height=number(p,"height",1,200),sides=number(p,"factions",2,32);
            String[] factions=new String[sides];for(int i=0;i<sides;i++)factions[i]=take(p,"faction."+i);
            if(player<0||player>=sides)throw new IOException("选择的势力不存在");
            World w=new World(width,height,factions);w.player=player;w.active=player;w.sourceMapWidth=sourceWidth;
            w.scenarioId=id;w.scenarioName=name;w.dataSource=source;w.dataRevision=revision;w.startYear=year;w.startMonth=month;
            w.dataHash=hash(raw);
            for(int r=0;r<height;r++) {
                String row=take(p,"terrain."+r);if(row.length()!=width)throw new IOException("地形行宽不匹配："+r);
                for(int q=0;q<width;q++) {
                    int index="PFMWDSBXOVZHA".indexOf(row.charAt(q));if(index<0)throw new IOException("未知地形："+row.charAt(q));
                    w.terrain[q][r]=World.Terrain.values()[index];
                }
            }
            int cities=number(p,"cities",2,1000);
            for(int i=0;i<cities;i++) {
                String[] c=fields(p,"city."+i,15);
                World.City city=new World.City(integer(c[0]),c[1],new Hex(integer(c[2]),integer(c[3])),integer(c[4]));
                city.gold=integer(c[5]);city.food=integer(c[6]);city.troops=integer(c[7]);city.order=integer(c[8]);city.morale=integer(c[9]);city.defense=integer(c[10]);city.baseDefense=Math.max(3000,city.defense);
                for(int j=0;j<4;j++)city.equipment[j]=integer(c[11+j]);w.cities.add(city);
            }
            if(p.containsKey("development-plots")){
                int n=number(p,"development-plots",0,36000);Map<Integer,List<Hex>> plots=new LinkedHashMap<>();
                for(int i=0;i<n;i++){String[] f=fields(p,"development-plot."+i,3);plots.computeIfAbsent(integer(f[0]),key->new ArrayList<>()).add(new Hex(integer(f[1]),integer(f[2])));}
                for(Map.Entry<Integer,List<Hex>> entry:plots.entrySet())w.development.configure(entry.getKey(),entry.getValue());
            }
            int officers=number(p,"officers",2,10000);
            for(int i=0;i<officers;i++) {
                String[] o=fields(p,"officer."+i,9);
                w.officers.add(new World.Officer(integer(o[0]),o[1],integer(o[2]),integer(o[3]),integer(o[4]),integer(o[5]),integer(o[6]),integer(o[7]),integer(o[8])));
            }
            // Optional v0.4 personnel data; old format-1 packs remain valid and do not invent talent.
            if(p.containsKey("talents")) {
                int talents=number(p,"talents",0,10000);
                for(int i=0;i<talents;i++) {
                    String[] t=fields(p,"talent."+i,9);
                    w.strategy.addHiddenTalent(new Strategy.Talent(integer(t[0]),t[1],integer(t[2]),
                        integer(t[3]),integer(t[4]),integer(t[5]),integer(t[6]),integer(t[7]),integer(t[8])));
                }
            }
            if(p.containsKey("arsenals")){
                int countArsenal=number(p,"arsenals",0,1000);Set<Integer> seen=new HashSet<>();
                for(int i=0;i<countArsenal;i++){String[] data=fields(p,"arsenal."+i,7);World.City c=w.city(integer(data[0]));if(c==null||!seen.add(c.id))throw new IOException("军备城池重复或缺失");for(int j=0;j<4;j++)c.equipment[5+j]=integer(data[1+j]);c.ships[0]=integer(data[5]);c.ships[1]=integer(data[6]);}
            }
            if(p.containsKey("aptitudes")){
                int countAptitude=number(p,"aptitudes",0,10000);Set<Integer> seen=new HashSet<>();
                for(int i=0;i<countAptitude;i++){String[] data=fields(p,"aptitude."+i,7);World.Officer o=w.officer(integer(data[0]));if(o==null||!seen.add(o.id))throw new IOException("适性武将重复或缺失");for(int j=0;j<6;j++)o.aptitude[j]=integer(data[1+j]);}
            }
            if(p.containsKey("initial-units")){
                int initialCount=number(p,"initial-units",0,10000);
                for(int i=0;i<initialCount;i++){
                    String[] data=fields(p,"initial-unit."+i,11);
                    World.Unit unit=new World.Unit(integer(data[0]),integer(data[1]),integer(data[2]),World.Weapon.valueOf(data[4]),new Hex(integer(data[6]),integer(data[7])),integer(data[8]),integer(data[9]));
                    unit.deputies=data[3].equals("-")?new int[0]:Arrays.stream(data[3].split(",")).mapToInt(Integer::parseInt).toArray();
                    unit.ship=Army.Ship.valueOf(data[5]);unit.energy=integer(data[10]);
                    if(unit.id<1||unit.id>=10000000)throw new IOException("开局部队编号无效");
                    for(int idInCrew:joinCrew(unit)){
                        World.Officer officer=w.officer(idInCrew);
                        if(officer==null||officer.owner!=unit.owner||officer.unitId!=-1)throw new IOException("开局编队武将无效");
                        officer.cityId=-1;officer.unitId=unit.id;
                    }
                    w.units.add(unit);w.nextUnitId=Math.max(w.nextUnitId,unit.id+1);
                }
            }
            if(p.containsKey("contest-profiles")){
                int profileCount=number(p,"contest-profiles",0,10000);Set<Integer> seen=new HashSet<>();
                for(int i=0;i<profileCount;i++){
                    String[] data=fields(p,"contest-profile."+i,4);int officer=integer(data[0]);
                    if(!seen.add(officer))throw new IOException("对局配置武将重复");
                    w.contests.configure(officer,new Contests.Profile(Debate.Temper.valueOf(data[1]),integer(data[2]),integer(data[3])));
                }
            }
            if(p.containsKey("site-kinds")){
                int countKinds=number(p,"site-kinds",0,1000);Set<Integer> seen=new HashSet<>();
                for(int i=0;i<countKinds;i++){String[] data=fields(p,"site-kind."+i,3);World.City c=w.city(integer(data[0]));if(c==null||!seen.add(c.id))throw new IOException("据点类型引用错误");c.kind=World.SiteKind.valueOf(data[1]);c.baseDefense=integer(data[2]);}
            }
            if(p.containsKey("unit-gold")){
                int countGold=number(p,"unit-gold",0,10000);Set<Integer> seen=new HashSet<>();
                for(int i=0;i<countGold;i++){String[] data=fields(p,"unit-gold."+i,2);World.Unit u=w.unit(integer(data[0]));if(u==null||!seen.add(u.id))throw new IOException("携金部队引用错误");u.gold=integer(data[1]);}
            }
            if(p.containsKey("technology-points")){
                int countPoints=number(p,"technology-points",0,sides);Set<Integer> seen=new HashSet<>();
                for(int i=0;i<countPoints;i++){String[] data=fields(p,"technology-points."+i,2);int side=integer(data[0]);if(side<0||side>=sides||!seen.add(side))throw new IOException("技巧点势力错误");w.campaign.points.put(side,integer(data[1]));}
            }
            if(p.containsKey("people-profiles")){
                int n=number(p,"people-profiles",0,10000);Set<Integer> seen=new HashSet<>();
                for(int i=0;i<n;i++){String[] data=fields(p,"people-profile."+i,4);World.Officer o=w.officer(integer(data[0]));if(o==null||!seen.add(o.id))throw new IOException("人物配置重复或缺失");o.sex=World.Sex.valueOf(data[1]);o.skillId=data[2];w.government.merits.put(o.id,integer(data[3]));}
            }
            if(p.containsKey("relations")){
                int n=number(p,"relations",0,10000);
                for(int i=0;i<n;i++){String[] data=fields(p,"relation."+i,3);int a=integer(data[0]),b=integer(data[1]);Relations.Kind k=Relations.Kind.valueOf(data[2]);String error=w.relations.linkError(a,b,k);if(error!=null)throw new IOException(error);w.relations.link(a,b,k);}
            }
            if(p.containsKey("treasures")){
                int n=number(p,"treasures",0,43);Set<String> seen=new HashSet<>();
                for(int i=0;i<n;i++){String[] data=fields(p,"treasure."+i,3);if(!seen.add(data[0]))throw new IOException("宝物重复");w.treasures.place(Treasures.definition(data[0]),Treasures.Place.valueOf(data[1]),integer(data[2]));}
            }
            if(p.containsKey("world-events"))w.events.enabled=number(p,"world-events",0,1)==1;
            if(p.containsKey("regions")){int n=number(p,"regions",0,w.cities.size());Set<Integer> seen=new HashSet<>();for(int i=0;i<n;i++){String[] f=fields(p,"region."+i,2);int city=integer(f[0]);if(!seen.add(city))throw new IOException("区域重复");w.events.configureRegion(city,WorldEvents.Tribe.valueOf(f[1]));}}
            if(p.containsKey("initial-camps")){int n=number(p,"initial-camps",0,w.cities.size());for(int i=0;i<n;i++){String[] f=fields(p,"initial-camp."+i,5);w.events.camps.add(new WorldEvents.Camp(w.events.nextCamp++,integer(f[0]),WorldEvents.Tribe.valueOf(f[1]),new Hex(integer(f[2]),integer(f[3])),integer(f[4])));}}
            if(p.containsKey("initial-hazards")){int n=number(p,"initial-hazards",0,w.cities.size());for(int i=0;i<n;i++){String[] f=fields(p,"initial-hazard."+i,2);int city=integer(f[0]);if(!w.events.beginDisaster(city,WorldEvents.Disaster.valueOf(f[1])))throw new IOException("初始灾害无效");}}
            if(p.containsKey("natural-deaths"))w.life.naturalDeaths=number(p,"natural-deaths",0,1)==1;
            if(p.containsKey("lifetimes")){int n=number(p,"lifetimes",0,w.officers.size());Set<Integer> seen=new HashSet<>();for(int i=0;i<n;i++){String[] f=fields(p,"lifetime."+i,6);int who=integer(f[0]);if(!seen.add(who))throw new IOException("生卒人物重复");w.life.configure(who,integer(f[1]),integer(f[2]),integer(f[3]),integer(f[4]),Lifecycle.State.valueOf(f[5]));}}
            if(p.containsKey("initial-treaties")){
                int n=number(p,"initial-treaties",0,496);Set<String> seen=new HashSet<>();
                for(int i=0;i<n;i++){String[] f=fields(p,"initial-treaty."+i,4);int a=integer(f[0]),b=integer(f[1]),turns=integer(f[3]);
                    if(a<0||b<0||a>=sides||b>=sides||a==b||turns<1||turns>360||!seen.add(Math.min(a,b)+":"+Math.max(a,b)))throw new IOException("初始协定无效");
                    w.campaign.concludeTreaty(a,b,Campaign.TreatyKind.valueOf(f[2]),turns);}
            }
            if(p.containsKey("rulers")){
                int n=number(p,"rulers",0,sides);Set<Integer> seen=new HashSet<>();
                for(int i=0;i<n;i++){String[] f=fields(p,"ruler."+i,2);int side=integer(f[0]);World.Officer o=w.officer(integer(f[1]));
                    if(side<0||side>=sides||!seen.add(side)||o==null||o.owner!=side||o.cityId<0)throw new IOException("君主配置无效");o.role=Strategy.Role.RULER;o.loyalty=100;}
            }
            for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++)if(w.terrain[q][r]==World.Terrain.DAM)
                w.war.structures.add(new War.Structure(w.war.nextStructureId++,-1,War.StructureKind.DAM,new Hex(q,r),War.StructureKind.DAM.hp));
            if(!p.isEmpty())throw new IOException("未知剧本字段："+p.keySet().iterator().next());
            if(reference!=null){ContentCatalog catalog=ContentCatalog.get();catalog.validateOpening(w);ContentRuntime.initializeOpening(w,catalog);if(referenceDetails)ContentProfiles.initialize(w,catalog,referenceDates);}
            w.strategy.initializeOffices();
            w.abilities.initialize(Objects.hash(w.scenarioId,w.startYear,w.startMonth));
            SaveCodec.validate(w);validateOpening(w);
            w.note(name+(source.equals("user-supplied")?"：导入数据，原版一致性未核验":reference==null?"：原创测试布局与数值，非原版历史剧本":"：公开资料能力/适性，原创区域地图与开局；非官方历史剧本"));
            w.note("当前执掌"+w.faction(player)+" · 点选己方城池开始经营");
            return w;
        }catch(IllegalArgumentException e){throw new IOException("剧本格式错误："+e.getMessage(),e);}
    }
    private static int[] joinCrew(World.Unit u){int[] ids=new int[1+u.deputies.length];ids[0]=u.officerId;System.arraycopy(u.deputies,0,ids,1,u.deputies.length);return ids;}
    private static String hash(byte[] bytes)throws IOException {
        try {
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder();
            for(byte b:hash)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
        }catch(NoSuchAlgorithmException e){throw new IOException("SHA-256不可用",e);}
    }
    private static String take(Properties p,String key)throws IOException {
        String value=(String)p.remove(key);if(value==null||value.trim().isEmpty())throw new IOException("缺失剧本字段："+key);return value.trim();
    }
    private static int integer(String text){return Integer.parseInt(text);}
    private static int number(Properties p,String key,int low,int high)throws IOException {
        int n=integer(take(p,key));if(n<low||n>high)throw new IOException("剧本字段越界："+key);return n;
    }
    private static String[] fields(Properties p,String key,int size)throws IOException {
        String[] values=take(p,key).split("\\|",-1);if(values.length!=size)throw new IOException("剧本列数错误："+key);
        for(int i=0;i<values.length;i++)values[i]=values[i].trim();return values;
    }
    private static void validateOpening(World w)throws IOException {
        for(int side=0;side<w.factions.length;side++) {
            boolean staffed=false;
            for(World.Officer o:w.officers)if(o.owner==side&&o.cityId>=0)staffed=true;
            if(!w.alive(side)||!staffed)throw new IOException("开局势力缺少城池或在城武将："+side);
        }
        Set<Hex> cities=new HashSet<>();for(World.City c:w.cities)cities.add(c.hex);
        for(World.City c:w.cities) {
            boolean exit=false;
            for(Hex h:c.hex.neighbors())if(w.cost(h,World.Weapon.SPEAR)>0&&!cities.contains(h))exit=true;
            if(!exit)throw new IOException("城池没有可用出口："+c.name);
        }
        // Ignore city occupancy here: this checks geographic connectivity, not access through ownership.
        Set<Hex> visited=new HashSet<>();ArrayDeque<Hex> pending=new ArrayDeque<>();
        pending.add(w.cities.get(0).hex);visited.add(pending.peek());
        while(!pending.isEmpty())for(Hex h:pending.remove().neighbors())if((w.cost(h,World.Weapon.SPEAR)>0||w.army.water(h))&&visited.add(h))pending.add(h);
        for(World.City c:w.cities)if(!visited.contains(c.hex))throw new IOException("城池与水陆地图隔绝："+c.name);
    }
}
