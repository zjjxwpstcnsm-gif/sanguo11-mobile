package game.sanguo.core;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class OfficerPackArchiveTest {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static byte[] zip(String name, byte[] data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            ZipEntry entry = new ZipEntry(name); out.putNextEntry(entry); out.write(data); out.closeEntry();
        }
        return bytes.toByteArray();
    }
    private static void rejected(byte[] bytes, String reason) throws Exception {
        try { OfficerPackArchive.read(bytes); throw new AssertionError("Accepted " + reason); }
        catch (IOException expected) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        byte[] json = "{\"format\":\"sg11-custom-officers\"}".getBytes("UTF-8");
        byte[] valid = zip("manifest.json", json);
        check(Arrays.equals(json, OfficerPackArchive.read(valid).get("manifest.json")), "normal deflated descriptor ZIP");
        for (int length = 0; length < valid.length; length++) rejected(Arrays.copyOf(valid, length), "truncated ZIP at " + length);
        rejected(zip("../manifest.json", json), "path traversal");
        rejected(zip("/manifest.json", json), "absolute path");
        rejected(zip("portraits/not-a-digest.png", json), "untrusted portrait path");
        rejected(new byte[OfficerPackArchive.MAX_PACK + 1], "compressed input cap");
        rejected(zip("manifest.json", new byte[OfficerPackArchive.MAX_MANIFEST + 1]), "uncompressed manifest cap");
        int cen = -1;
        for (int i = 0; i < valid.length - 4; i++) if (valid[i] == 80 && valid[i + 1] == 75 && valid[i + 2] == 1 && valid[i + 3] == 2) { cen = i; break; }
        check(cen >= 0, "fixture central directory");
        byte[] broken = valid.clone(); broken[cen + 16] ^= 1; rejected(broken, "central/local CRC mismatch");
        broken = valid.clone(); broken[cen + 24] ^= 1; rejected(broken, "central/local length mismatch");
        broken = valid.clone(); broken[cen + 42] = 1; rejected(broken, "wrong local offset");
        broken = Arrays.copyOf(valid, valid.length + 1); rejected(broken, "trailing junk");
        ByteArrayOutputStream pair = new ByteArrayOutputStream();
        String name = "portraits/" + String.join("", Collections.nCopies(64, "a")) + ".png";
        try (ZipOutputStream out = new ZipOutputStream(pair)) {
            for (String path : Arrays.asList("manifest.json", name)) { out.putNextEntry(new ZipEntry(path)); out.write(json); out.closeEntry(); }
        }
        check(OfficerPackArchive.read(pair.toByteArray()).size() == 2, "manifest and portrait bytes kept together");
        ByteArrayOutputStream stored = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(stored)) {
            ZipEntry entry = new ZipEntry("manifest.json"); entry.setMethod(ZipEntry.STORED); entry.setSize(json.length);
            CRC32 crc = new CRC32(); crc.update(json); entry.setCrc(crc.getValue()); out.putNextEntry(entry); out.write(json); out.closeEntry();
        }
        check(Arrays.equals(json, OfficerPackArchive.read(stored.toByteArray()).get("manifest.json")), "stored ZIP");
        System.out.println("OFFICER PACK ARCHIVE PASS: " + checks + " checks; truncation, directory integrity, paths, CRC and bounded decoding");
    }
}
