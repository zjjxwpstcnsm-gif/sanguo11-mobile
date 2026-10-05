import game.sanguo.core.SaveCodec;
import game.sanguo.core.World;
import java.nio.file.*;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

/** Read-only media evidence export; no commands or RNG calls. */
public final class MediaSaveEvidence {
    private static long state(Object owner)throws Exception{
        Field field=owner.getClass().getDeclaredField("randomState");field.setAccessible(true);return field.getLong(owner);
    }
    private static boolean gzipHeaderOnly(byte[] raw,byte[] encoded)throws Exception{
        if(raw.length!=encoded.length)return false;int at=-1,count=0;
        for(int i=20;i<raw.length;i++)if(raw[i]!=encoded[i]){at=i;count++;}
        if(count!=1||at<33||raw[at]!=0||(encoded[at]&255)!=255)return false;
        int start=at-9;
        if((raw[start]&255)!=31||(raw[start+1]&255)!=139||raw[start+2]!=8)return false;
        int length=ByteBuffer.wrap(raw,start-4,4).getInt();if(length<18||start+length>raw.length)return false;
        byte[] original,host;
        try(var gzip=new GZIPInputStream(new ByteArrayInputStream(raw,start,length))){original=gzip.readAllBytes();}
        try(var gzip=new GZIPInputStream(new ByteArrayInputStream(encoded,start,length))){host=gzip.readAllBytes();}
        return Arrays.equals(original,host);
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("Explicit generated fixture directory required");
        System.out.println("file,bytes,sha256,turn,strategyRandomSignedLong,eventRandomSignedLong,officers,units,cities,fullRoundtripByteEqual,payloadFirstDifference,reportGzipHeaderOnlyDifference");
        List<Path> files=new ArrayList<>();try(var stream=Files.list(Path.of(args[0]))){stream.filter(p->p.getFileName().toString().matches("authority-(before|round-[0-5])\\.sg11")).sorted().forEach(files::add);}
        if(files.size()!=7)throw new IllegalArgumentException("Seven complete before/committed Saves required");
        int expected=0;
        for(Path path:files){byte[] raw=Files.readAllBytes(path);World world=SaveCodec.decode(raw);
            byte[] encoded=SaveCodec.encode(world);if(world.turn!=expected++)throw new IllegalStateException("Committed fixture turn differs: "+path);
            boolean equal=Arrays.equals(raw,encoded),gzipOnly=gzipHeaderOnly(raw,encoded);World host=SaveCodec.decode(encoded);
            if((!equal&&!gzipOnly)||state(world.strategy)!=state(host.strategy)||state(world.events)!=state(host.events))throw new IllegalStateException("Unexpected host data/RNG difference: "+path);
            StringBuilder digest=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(raw))digest.append(String.format(Locale.ROOT,"%02x",value&255));
            System.out.println(path.getFileName()+","+raw.length+","+digest+","+world.turn+","+state(world.strategy)+","+state(world.events)+","+world.officers.size()+","+world.units.size()+","+world.cities.size()+","+equal+","+Arrays.mismatch(Arrays.copyOfRange(raw,20,raw.length),Arrays.copyOfRange(encoded,20,encoded.length))+","+gzipOnly);
        }
    }
}
