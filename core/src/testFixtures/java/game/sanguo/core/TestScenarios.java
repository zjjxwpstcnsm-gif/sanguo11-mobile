package game.sanguo.core;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Test-only catalog. The production catalog never discovers these resources. */
public final class TestScenarios {
    private TestScenarios(){}
    private static Map<String,String> entries()throws IOException{
        Map<String,String> out=new LinkedHashMap<>();
        try(InputStream in=TestScenarios.class.getResourceAsStream("/test-scenarios/index.txt")){
            if(in==null)throw new IOException("Test resources missing");
            BufferedReader reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));String line;
            while((line=reader.readLine())!=null)if(!line.isEmpty()&&!line.startsWith("#")){String[] pair=line.split(" ");out.put(pair[0],pair[1]);}
        }return out;
    }
    public static World load(String id,int player)throws IOException{
        String hash=entries().get(id);if(hash==null)return ScenarioCatalog.load(id,player);
        try(InputStream in=TestScenarios.class.getResourceAsStream("/test-scenarios/"+id+".properties")){
            World w=ScenarioData.read(in,player);if(!w.scenarioId.equals(id)||!w.dataHash.equals(hash))throw new IOException("Test scenario checksum mismatch");return w;
        }
    }
    public static World load(String id,int player,long seed)throws IOException{World w=load(id,player);w.abilities.initialize(seed);return w;}
    public static List<World> all()throws IOException{List<World> result=new ArrayList<>(ScenarioCatalog.all());for(String id:entries().keySet())result.add(load(id,0));return result;}
}
