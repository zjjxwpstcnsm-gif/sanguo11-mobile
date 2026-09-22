package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Bounded, memory-only reader for the portable officer ZIP format (not a general unzip tool). */
public final class OfficerPackArchive {
    public static final int MAX_PACK = 20 * 1024 * 1024;
    public static final int MAX_MANIFEST = 16 * 1024 * 1024;
    private OfficerPackArchive() {}

    public static Map<String, byte[]> read(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length > MAX_PACK) throw bad("文件超过20MiB限制");
        Map<String, Meta> directory = directory(bytes);
        Map<String, byte[]> files = new LinkedHashMap<>();
        int total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                Meta expected = directory.get(name);
                if (entry.isDirectory() || expected == null || files.containsKey(name))
                    throw bad("本地条目与目录不一致或路径重复");
                int limit = name.equals("manifest.json") ? MAX_MANIFEST : CustomOfficers.MAX_PORTRAIT;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                CRC32 crc = new CRC32();
                int n;
                while ((n = zip.read(buffer)) != -1) {
                    if (n > limit - out.size() || n > MAX_PACK - total) throw bad("解压内容超过大小限制");
                    out.write(buffer, 0, n); crc.update(buffer, 0, n); total += n;
                }
                zip.closeEntry();
                if (out.size() != expected.size || crc.getValue() != expected.crc
                        || entry.getMethod() != expected.method)
                    throw bad("条目长度、方法或校验码不一致");
                files.put(name, out.toByteArray());
            }
        }
        if (!files.keySet().equals(directory.keySet()) || !files.containsKey("manifest.json"))
            throw bad("缺少人物清单或目录条目");
        return files;
    }

    // ZipInputStream alone accepts a truncated archive with intact local headers but no CEN/END.
    private static Map<String, Meta> directory(byte[] b) throws IOException {
        int end = -1;
        for (int i = b.length - 22; i >= Math.max(0, b.length - 22 - 65535); i--) {
            if (u32(b, i) == 0x06054b50L && i + 22 + u16(b, i + 20) == b.length) { end = i; break; }
        }
        if (end < 0) throw bad("文件被截断或缺少ZIP尾目录");
        int count = u16(b, end + 10);
        long start = u32(b, end + 16), length = u32(b, end + 12);
        if (u16(b, end + 4) != 0 || u16(b, end + 6) != 0 || u16(b, end + 8) != count
                || count < 1 || count > 1001 || start + length != end || start > end)
            throw bad("不支持分卷、ZIP64或无效目录");
        Map<String, Meta> result = new LinkedHashMap<>();
        Set<Integer> offsets = new HashSet<>();
        int at = (int) start;
        for (int i = 0; i < count; i++) {
            if (at > end - 46 || u32(b, at) != 0x02014b50L) throw bad("目录条目损坏");
            int nameSize = u16(b, at + 28), extra = u16(b, at + 30), comment = u16(b, at + 32);
            int next = at + 46 + nameSize + extra + comment;
            if (next > end) throw bad("目录长度越界");
            String name = new String(b, at + 46, nameSize, StandardCharsets.UTF_8);
            if (!name.equals("manifest.json") && !name.matches("portraits/[a-f0-9]{64}\\.png"))
                throw bad("非法文件路径");
            int flags = u16(b, at + 8), method = u16(b, at + 10);
            long size = u32(b, at + 24), compressed = u32(b, at + 20), offset = u32(b, at + 42);
            int limit = name.equals("manifest.json") ? MAX_MANIFEST : CustomOfficers.MAX_PORTRAIT;
            if ((flags & ~0x0808) != 0 || (method != ZipEntry.STORED && method != ZipEntry.DEFLATED)
                    || u16(b, at + 34) != 0 || size > limit || offset > start - 30)
                throw bad("不支持加密、压缩方法或超限条目");
            int local = (int) offset;
            if (!offsets.add(local) || u32(b, local) != 0x04034b50L
                    || u16(b, local + 6) != flags || u16(b, local + 8) != method)
                throw bad("本地条目身份不一致");
            int localName = u16(b, local + 26), localExtra = u16(b, local + 28);
            long data = (long) local + 30 + localName + localExtra;
            if (data > start || compressed > start - data || localName != nameSize
                    || !name.equals(new String(b, local + 30, localName, StandardCharsets.UTF_8)))
                throw bad("本地文件名或数据范围不一致");
            if (result.put(name, new Meta(size, u32(b, at + 16), method)) != null) throw bad("目录路径重复");
            at = next;
        }
        if (at != end) throw bad("目录有未解析内容");
        return result;
    }
    private static final class Meta {
        final long size, crc; final int method;
        Meta(long size, long crc, int method) { this.size = size; this.crc = crc; this.method = method; }
    }
    private static int u16(byte[] b, int p) { return (b[p] & 255) | (b[p + 1] & 255) << 8; }
    private static long u32(byte[] b, int p) { return (long) u16(b, p) | (long) u16(b, p + 2) << 16; }
    private static IOException bad(String reason) { return new IOException("武将数据包无效：" + reason); }
}
