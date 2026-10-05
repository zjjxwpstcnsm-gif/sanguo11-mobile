package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Pinned shared Scenario.s11 rank definitions, with explicit legacy ID bridge. */
final class PcOfficerRanks {
    private static final String SHA256="78ce6ffb93c7164d81858df7d5ca5387ffe830f2fabb821ece3b3fb8122f1288";
    static final class Definition {
        final int nativeId,order,command,abilityStat,abilityBonus,salary,grade,merit,sourceOffset;
        final String projectId,nativeName,recordSha256;
        final boolean civilian;
        Definition(String[] c)throws IOException {
            require(c.length==13,"官职列数错误");
            nativeId=number(c[0],0,80);projectId=c[1];order=number(c[2],-1,79);
            require(c[3].equals("true")||c[3].equals("false"),"官职类别无效");civilian=Boolean.parseBoolean(c[3]);nativeName=c[4];
            command=number(c[5],5000,30000);abilityStat=number(c[6],0,4);abilityBonus=number(c[7],0,5);
            salary=number(c[8],0,255);grade=number(c[9],0,12);merit=number(c[10],0,60000);sourceOffset=number(c[11],0,47928-46);recordSha256=c[12];
            require(!projectId.isEmpty()&&!nativeName.isEmpty()&&recordSha256.matches("[0-9a-f]{64}"),"官职来源记录无效");
            require(nativeId==80?projectId.equals("-")&&order==-1:!projectId.equals("-")&&order>=0,"官职兼容映射错误");
        }
    }
    private static final List<Definition> ALL=load();
    static Definition unassigned(){return ALL.get(80);}
    static List<Definition> legacyOrder(){
        List<Definition> result=new ArrayList<>(ALL.subList(0,80));result.sort(Comparator.comparingInt(d->d.order));
        return Collections.unmodifiableList(result);
    }
    static List<Definition> all(){return ALL;}
    private static List<Definition> load(){
        try(InputStream in=PcOfficerRanks.class.getResourceAsStream("/content/pc-officer-ranks.tsv")){
            require(in!=null,"原官职资源缺失");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1){require(out.size()+n<=32768,"官职资源过大");out.write(buffer,0,n);}
            byte[] raw=out.toByteArray();StringBuilder digest=new StringBuilder();
            for(byte b:MessageDigest.getInstance("SHA-256").digest(raw))digest.append(String.format(Locale.ROOT,"%02x",b&255));
            require(digest.toString().equals(SHA256),"原官职资源校验失败");
            String[] lines=new String(raw,StandardCharsets.UTF_8).split("\n");
            require(lines.length==85&&lines[0].equals("# pc-officer-ranks-v1")&&lines[1].equals("# source=Media/scenario/Scenario.s11"),"官职资源格式错误");
            require(lines[3].equals("native_id\tproject_id\tlegacy_order\tcivilian\tnative_name\tcommand\tability_stat\tability_bonus\tsalary\tgrade\tmerit\tsource_offset\trecord_sha256"),"官职字段错误");
            List<Definition> result=new ArrayList<>();Set<String> ids=new HashSet<>();Set<Integer> orders=new HashSet<>();
            for(int i=4;i<lines.length;i++){
                Definition d=new Definition(lines[i].split("\t",-1));require(d.nativeId==i-4&&ids.add(d.projectId)&&orders.add(d.order),"官职重复或排序错误");result.add(d);
            }
            return Collections.unmodifiableList(result);
        }catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    private static int number(String value,int min,int max)throws IOException{
        try{int n=Integer.parseInt(value);require(n>=min&&n<=max,"官职数值越界");return n;}catch(NumberFormatException e){throw new IOException("官职数值无效",e);}
    }
    private static void require(boolean condition,String message)throws IOException{if(!condition)throw new IOException(message);}
}
