import game.sanguo.core.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
/** Compare isolated codec implementations on real saves, errors and full bytes. */
public final class PcSaveCodecProbe {
    static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    public static void main(String[] args)throws Exception{
        int repetitions=Integer.parseInt(args[0]);long elapsed=0;int checks=0;
        for(int n=1;n<args.length;n++){
            byte[] source=Files.readAllBytes(Path.of(args[n]));World original=SaveCodec.decode(source);
            // Cross-platform GZIP header differences are excluded by one normal
            // roundtrip; every measured same-runtime save must then be identical.
            byte[] canonical=SaveCodec.encode(original);
            for(int warm=0;warm<2;warm++)SaveCodec.decode(canonical);
            long start=System.nanoTime();
            for(int i=0;i<repetitions;i++){
                World decoded=SaveCodec.decode(canonical);byte[] out=SaveCodec.encode(decoded);
                if(!Arrays.equals(canonical,out)||decoded.strategy.getRandomState()!=original.strategy.getRandomState())throw new AssertionError("Codec changed state/RNG");
                checks++;
            }
            elapsed+=System.nanoTime()-start;
            System.out.println("SAVE "+Path.of(args[n]).getFileName()+" "+sha(canonical));
            for(String skill:new String[]{"none","future.unknown-1","a".repeat(80),"","_bad","Upper","a".repeat(81),"bad\n","中文"}){
                World probe=SaveCodec.decode(canonical);probe.officers.get(0).skillId=skill;
                try {byte[] out=SaveCodec.encode(probe);World copy=SaveCodec.decode(out);if(!skill.equals(copy.officers.get(0).skillId))throw new AssertionError("Unknown skill not preserved");System.out.println("SKILL "+skill.length()+" "+sha(skill.getBytes(java.nio.charset.StandardCharsets.UTF_8))+" ACCEPT");}
                catch(java.io.IOException error){System.out.println("SKILL "+skill.length()+" "+sha(skill.getBytes(java.nio.charset.StandardCharsets.UTF_8))+" REJECT "+error.getMessage());}
                checks++;
            }
        }
        System.out.println("PASS checks="+checks+" measuredMs="+(elapsed/1000000));
    }
}
