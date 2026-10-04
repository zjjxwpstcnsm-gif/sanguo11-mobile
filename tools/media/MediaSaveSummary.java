import game.sanguo.core.SaveCodec;
import game.sanguo.core.World;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Host Java17 read-only opaque acceptance saves; no commands, encoding or RNG draws. */
public final class MediaSaveSummary {
    public static void main(String[] args) throws Exception {
        System.out.println("file,bytes,sha256,turn,randomStateSignedLong,officers,units,cities");
        for (String arg : args) {
            Path path = Path.of(arg);
            if (path.getFileName().toString().contains(",")) throw new IllegalArgumentException("CSV filename");
            byte[] raw = Files.readAllBytes(path);
            World saved = SaveCodec.decode(raw);
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
            System.out.println(path.getFileName() + "," + raw.length + "," + sha + "," + saved.turn + "," +
                saved.strategy.getRandomState() + "," + saved.officers.size() + "," + saved.units.size() + "," + saved.cities.size());
        }
    }
}
