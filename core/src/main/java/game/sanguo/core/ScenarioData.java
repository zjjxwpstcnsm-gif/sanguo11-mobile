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
            number(p,"format",1,1);
            String id=take(p,"id"),name=take(p,"name"),source=take(p,"source");
            if(!id.matches("[a-z0-9-]{1,80}"))throw new IOException("剧本ID无效");
            // Only researched formats should be added here; current packs make no original-data claim.
            if(!source.equals("engineering-original")&&!source.equals("community-reference"))throw new IOException("未知数据来源等级");
            String reference=source.equals("community-reference")?take(p,"reference"):null;
            if(reference!=null&&!reference.equals("rlu-officers"))throw new IOException("未知人物资料来源");
            int revision=number(p,"revision",1,1000000),year=number(p,"year",1,9999),month=number(p,"month",1,12);
            int width=number(p,"width",1,128),height=number(p,"height",1,128),sides=number(p,"factions",2,32);
            String[] factions=new String[sides];for(int i=0;i<sides;i++)factions[i]=take(p,"faction."+i);
            if(player<0||player>=sides)throw new IOException("选择的势力不存在");
            World w=new World(width,height,factions);w.player=player;w.active=player;
            w.scenarioId=id;w.scenarioName=name;w.dataSource=source;w.dataRevision=revision;w.startYear=year;w.startMonth=month;
            w.dataHash=hash(raw);
            for(int r=0;r<height;r++) {
                String row=take(p,"terrain."+r);if(row.length()!=width)throw new IOException("地形行宽不匹配："+r);
                for(int q=0;q<width;q++) {
                    int index="PFMW".indexOf(row.charAt(q));if(index<0)throw new IOException("未知地形："+row.charAt(q));
                    w.terrain[q][r]=World.Terrain.values()[index];
                }
            }
            int cities=number(p,"cities",2,1000);
            for(int i=0;i<cities;i++) {
                String[] c=fields(p,"city."+i,15);
                World.City city=new World.City(integer(c[0]),c[1],new Hex(integer(c[2]),integer(c[3])),integer(c[4]));
                city.gold=integer(c[5]);city.food=integer(c[6]);city.troops=integer(c[7]);city.order=integer(c[8]);city.morale=integer(c[9]);city.defense=integer(c[10]);
                for(int j=0;j<4;j++)city.equipment[j]=integer(c[11+j]);w.cities.add(city);
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
            if(!p.isEmpty())throw new IOException("未知剧本字段："+p.keySet().iterator().next());
            if(reference!=null){ContentCatalog catalog=ContentCatalog.get();catalog.validateOpening(w);ContentRuntime.initializeOpening(w,catalog);}
            w.strategy.initializeOffices();
            SaveCodec.validate(w);validateOpening(w);
            w.note(name+(reference==null?"：原创测试布局与数值，非原版历史剧本":"：公开资料能力/适性，原创区域地图与开局；非官方历史剧本"));
            w.note("当前执掌"+w.faction(player)+" · 点选己方城池开始经营");
            return w;
        }catch(IllegalArgumentException e){throw new IOException("剧本格式错误："+e.getMessage(),e);}
    }
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
        while(!pending.isEmpty())for(Hex h:pending.remove().neighbors())if(w.cost(h,World.Weapon.SPEAR)>0&&visited.add(h))pending.add(h);
        for(World.City c:w.cities)if(!visited.contains(c.hex))throw new IOException("城池与陆路地图隔绝："+c.name);
    }
}
