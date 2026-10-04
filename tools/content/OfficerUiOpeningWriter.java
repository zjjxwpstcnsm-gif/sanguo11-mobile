import game.sanguo.core.*;
import java.nio.file.*;

/** Normal current ScenarioCatalog opening, solely for protected UI test slots. */
public final class OfficerUiOpeningWriter {
    public static void main(String[] args)throws Exception{
        World opening=ScenarioCatalog.load("coalition-190",0,23);
        byte[] bytes=SaveCodec.encode(opening);Files.write(Path.of(args[0]),bytes);
        if(!java.util.Arrays.equals(bytes,SaveCodec.encode(SaveCodec.decode(bytes))))throw new AssertionError("fixture roundtrip");
        System.out.println("PASS normal190 current opening bytes="+bytes.length);
    }
}
