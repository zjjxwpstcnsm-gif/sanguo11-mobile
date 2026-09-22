package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Shared implementation manifest; missing/unreviewed entries are NOT offered as working skills. */
public final class SkillSupport {
    private static final Map<String,String> HOOKS=load();
    private SkillSupport(){}
    private static Map<String,String> load(){
        Map<String,String> hooks=new HashMap<>();try(InputStream in=SkillSupport.class.getResourceAsStream("/rules/skill-support.tsv")){
            if(in==null)return hooks;try(BufferedReader reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){if(line.startsWith("#")||line.isEmpty())continue;String[] c=line.split("\t",-1);if(c.length==2)hooks.put(c[0],c[1]);}}
        }catch(IOException ignored){/* A missing manifest disables selection rather than inventing support. */}return Collections.unmodifiableMap(hooks);
    }
    public static boolean enabled(String id){return "none".equals(id)||Skill.find(id)!=null&&HOOKS.containsKey(id);}
    public static String status(String id){return "none".equals(id)?"无特技":enabled(id)?"已接入当前工程规则；非原版完整等价验收\n执行入口："+HOOKS.get(id):"未确认实际效果入口，禁止选择";}
}
