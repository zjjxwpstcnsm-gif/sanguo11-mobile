import game.sanguo.core.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Run unchanged real saves on two isolated classpaths; compare output bytes. */
class PcSaveContinuationProbe {
    public static void main(String[] args)throws Exception{
        if(args.length!=3)throw new IllegalArgumentException("save output-directory turns");
        World w=SaveCodec.decode(Files.readAllBytes(Path.of(args[0])));Path out=Path.of(args[1]);Files.createDirectories(out);
        for(int i=0;i<=Integer.parseInt(args[2]);i++){
            byte[] bytes=SaveCodec.encode(w);Files.write(out.resolve("turn-"+i+".sg11"),bytes);
            System.out.println("CONTINUATION step="+i+" mapRevision="+w.mapRevision+" turn="+w.turn+" structures="+w.war.structures().size()+" sha256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            if(i<Integer.parseInt(args[2])){World.Result result=w.nextTurn();if(!result.ok)throw new AssertionError(result.message);w=SaveCodec.decode(SaveCodec.encode(w));}
        }
    }
}
