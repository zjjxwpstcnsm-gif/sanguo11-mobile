package game.sanguo.core;
import java.nio.file.*;import java.security.*;import java.util.*;
/** One identical helper runs against completed and candidate core classes. */
class session_b_legacy39_roundtrip {
 public static void main(String[]a)throws Exception{byte[]before=Files.readAllBytes(Path.of(a[0]));World w=SaveCodec.decode(before);byte[]after=SaveCodec.encode(w);Files.write(Path.of(a[1]),after);int differences=0;for(int i=0;i<Math.min(before.length,after.length);i++)if(before[i]!=after[i]){if(differences<12)System.out.println("DIFF "+i+" "+(before[i]&255)+" "+(after[i]&255));differences++;}System.out.println("bytes "+before.length+" / "+after.length+" diffs "+differences+" SHA "+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(after)));}
}
