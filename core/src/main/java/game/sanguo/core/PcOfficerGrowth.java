package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Only identity-verified growth codes invariant across all16 installed candidates. */
final class PcOfficerGrowth {
    static final String SHA="ba89f918e9852ce5144e94441e132f8fdb87accc243bbec3bb0596b7e2307721";
    static final class Definition {
        final String nativeIds,name;final int[] curves=new int[5];final boolean special;
        Definition(String[] c){nativeIds=c[1];name=c[2];Boolean kind=null;for(String id:nativeIds.split(",")){int n=Integer.parseInt(id);if(n<0||n>=1100)throw new IllegalArgumentException();boolean k=n>=700&&n<=799;if(kind!=null&&kind!=k)throw new IllegalArgumentException();kind=k;}special=Boolean.TRUE.equals(kind);for(int i=0;i<5;i++){curves[i]=Integer.parseInt(c[i+3]);if(curves[i]<0||curves[i]>8)throw new IllegalArgumentException();}}
    }
    private static final Map<Integer,Definition> ALL=load();
    static Definition get(int id){return ALL.get(id);}
    private static Map<Integer,Definition> load(){
        try(InputStream in=PcOfficerGrowth.class.getResourceAsStream("/content/pc-officer-growth.tsv")){
            if(in==null)throw new IOException("成长来源缺失");ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;
            while((n=in.read(chunk))>=0){if(bytes.size()+n>65536)throw new IOException("成长来源过大");bytes.write(chunk,0,n);}
            byte[] raw=bytes.toByteArray();StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(raw))hash.append(String.format(Locale.ROOT,"%02x",b&255));
            if(!SHA.equals(hash.toString()))throw new IOException("成长来源校验失败");
            String[] lines=new String(raw,StandardCharsets.UTF_8).split("\n");if(lines.length!=667||!lines[0].equals("project_id\tnative_ids\tname\tgrowth0\tgrowth1\tgrowth2\tgrowth3\tgrowth4"))throw new IOException("成长来源格式无效");
            Map<Integer,Definition> rows=new TreeMap<>();for(int i=1;i<lines.length;i++){String[] c=lines[i].split("\t",-1);if(c.length!=8||rows.put(Integer.parseInt(c[0]),new Definition(c))!=null)throw new IOException("成长来源映射重复");}return Collections.unmodifiableMap(rows);
        }catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
}
