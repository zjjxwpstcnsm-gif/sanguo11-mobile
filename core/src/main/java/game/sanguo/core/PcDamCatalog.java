package game.sanguo.core;

import game.sanguo.core.map.SourceGridCoord;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Immutable source placements. Referenced only during new-world materialization. */
final class PcDamCatalog {
    private PcDamCatalog(){}
    private static final String SHA256="2c73957fcfb5873185fe72231e2f4b1b980f960e9962a063ecef4304b58f408a";
    private static final List<SourceGridCoord> CELLS=load();
    static List<SourceGridCoord> cells(){return CELLS;}
    private static List<SourceGridCoord> load(){
        Properties p=new Properties();
        try(InputStream in=PcDamCatalog.class.getResourceAsStream("/maps/pc-dams-v065.properties")){
            if(in==null)throw new IOException("PC堤防落点缺失");ByteArrayOutputStream raw=new ByteArrayOutputStream();byte[] buffer=new byte[512];
            for(int n;(n=in.read(buffer))!=-1;){if(raw.size()+n>4096)throw new IOException("PC堤防落点过大");raw.write(buffer,0,n);}byte[] bytes=raw.toByteArray();
            if(bytes.length>4096||!String.format(Locale.ROOT,"%064x",new java.math.BigInteger(1,MessageDigest.getInstance("SHA-256").digest(bytes))).equals(SHA256))throw new IOException("PC堤防落点校验失败");
            p.load(new InputStreamReader(new ByteArrayInputStream(bytes),StandardCharsets.UTF_8));
            if(!"PC_DAMS_1".equals(p.remove("format"))||!"65".equals(p.remove("mapRevision"))||!"4".equals(p.remove("count")))throw new IOException("PC堤防身份错误");
            String source=(String)p.remove("sourceSha256");if(source==null||!source.matches("[a-f0-9]{64}"))throw new IOException("PC堤防来源缺失");
            List<SourceGridCoord> cells=new ArrayList<>();Set<SourceGridCoord> seen=new HashSet<>();
            for(int i=0;i<4;i++){String value=(String)p.remove("dam."+i);if(value==null)throw new IOException("PC堤防落点不完整");String[] xy=value.split(",",-1);if(xy.length!=2)throw new IOException("PC堤防坐标");SourceGridCoord c=new SourceGridCoord(Integer.parseInt(xy[0]),Integer.parseInt(xy[1]));if(!c.isInside(200,200)||!seen.add(c))throw new IOException("PC堤防重复/越界");cells.add(c);}
            if(!p.isEmpty())throw new IOException("PC堤防未知字段");return Collections.unmodifiableList(cells);
        }catch(IOException|IllegalArgumentException|java.security.NoSuchAlgorithmException e){throw new ExceptionInInitializerError(e);}
    }
}
