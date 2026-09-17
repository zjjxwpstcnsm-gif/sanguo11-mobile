package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bundled, checksum-pinned scenario catalog. Saves embed data and never depend on this catalog. */
public final class ScenarioCatalog {
    private ScenarioCatalog() {}
    private static Map<String,String> entries()throws IOException {
        Map<String,String> result=new LinkedHashMap<>();
        try(InputStream in=ScenarioCatalog.class.getResourceAsStream("/scenarios/index.txt")) {
            if(in==null)throw new IOException("剧本目录缺失");
            BufferedReader reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));String line;
            while((line=reader.readLine())!=null) {
                if(line.trim().isEmpty()||line.startsWith("#"))continue;
                String[] parts=line.split(" ");
                if(parts.length!=2||!parts[0].matches("[a-z0-9-]{1,80}")||!parts[1].matches("[0-9a-f]{64}")||result.put(parts[0],parts[1])!=null)throw new IOException("剧本目录无效");
                if(result.size()>100)throw new IOException("剧本目录过大");
            }
        }
        if(result.isEmpty())throw new IOException("剧本目录为空");return result;
    }
    /** Lightweight catalog rows: opening the picker must not build every national world. */
    public static final class Summary {
        public final String id,name;public final int sites,officers,factions;
        private Summary(String id,Properties p){this.id=id;name=p.getProperty("name");sites=Integer.parseInt(p.getProperty("cities"));officers=Integer.parseInt(p.getProperty("officers"));factions=Integer.parseInt(p.getProperty("factions"));}
    }
    private static List<Summary> cachedSummaries;
    public static synchronized List<Summary> summaries()throws IOException {
        if(cachedSummaries!=null)return cachedSummaries;
        List<Summary> result=new ArrayList<>();
        for(String id:entries().keySet())try(InputStream in=ScenarioCatalog.class.getResourceAsStream("/scenarios/"+id+".properties")){
            if(in==null)throw new IOException("剧本资源缺失");Properties p=new Properties();p.load(new InputStreamReader(in,StandardCharsets.UTF_8));result.add(new Summary(id,p));
        }catch(IllegalArgumentException e){throw new IOException("剧本目录字段无效",e);}
        return cachedSummaries=Collections.unmodifiableList(result);
    }
    public static List<World> all()throws IOException {
        List<World> worlds=new ArrayList<>();for(String id:entries().keySet())worlds.add(load(id,0));return worlds;
    }
    public static World load(String id,int player,long abilitySeed)throws IOException {
        World w=load(id,player);w.abilities.initialize(abilitySeed);return w;
    }
    public static World load(String id,int player)throws IOException {
        String expected=entries().get(id);if(expected==null)throw new IOException("剧本不存在");
        try(InputStream in=ScenarioCatalog.class.getResourceAsStream("/scenarios/"+id+".properties")) {
            World world=ScenarioData.read(in,player);
            if(!world.scenarioId.equals(id)||!world.dataHash.equals(expected))throw new IOException("剧本ID或校验值不匹配");return world;
        }
    }
}
