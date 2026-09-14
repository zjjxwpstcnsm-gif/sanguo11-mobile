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
