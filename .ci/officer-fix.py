"""Reviewed PR59 finalization, applied once in an isolated checkout, then removed."""
from pathlib import Path

def edit(path, old, new, count=1):
    p = Path(path)
    text = p.read_text()
    assert text.count(old) == count, (path, 'anchor mismatch', text.count(old))
    p.write_text(text.replace(old, new))

def add(path, text):
    p = Path(path)
    assert not p.exists(), ('already exists', path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text)

add('core/src/main/java/game/sanguo/core/OfficerPackArchive.java', r'''package game.sanguo.core;

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
''')
add('core/src/test/java/game/sanguo/core/OfficerPackArchiveTest.java', r'''package game.sanguo.core;
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
''')

path = 'app/src/main/java/game/sanguo/mobile/CustomOfficerPack.java'
p = Path(path)
text = p.read_text()
lines = text.splitlines(True)
anchors = [line for line in lines if line.strip().startswith('else try(ZipInputStream zip=')]
assert len(anchors) == 1
edit(path, anchors[0], '        else files.putAll(OfficerPackArchive.read(bytes));\n')

path = 'app/src/main/java/game/sanguo/mobile/CustomOfficerLibrary.java'
edit(path, 'UUID.fromString(root.getString("libraryId"));JSONArray values=', 'UUID.fromString(root.getString("libraryId"));strictInt(root.get("version"));strictInt(root.get("nextId"));if(root.has("libraryRevision"))strictInt(root.get("libraryRevision"));JSONArray values=')
edit(path, '"death","affinity"})strictInt', '"death","affinity","honor","talkMask"})strictInt')
edit(path, 'String id=o.getString("id");UUID.fromString(id);', 'String id=o.getString("id");UUID.fromString(id);if(o.has("origin"))UUID.fromString(o.getString("origin"));')

path = 'app/src/androidTest/java/game/sanguo/mobile/CustomOfficerProbe.java'
edit(path, '        // Deletion of the complete library', '''        String unchanged=library.snapshot().toString();
        try{CustomOfficerPack.preview(context(),new ByteArrayInputStream(Arrays.copyOf(bytes,bytes.length-22)),library);throw new AssertionError("truncated central directory accepted");}catch(IOException expected){check(unchanged.equals(library.snapshot().toString()),"truncated ZIP rejected before any library change");}
        for(String field:new String[]{"honor","talkMask"}){
            JSONObject fractional=library.snapshot();fractional.getJSONArray("entries").getJSONObject(0).put(field,1.5);
            try{library.replace(fractional);throw new AssertionError("fractional "+field+" accepted");}catch(IllegalArgumentException expected){check(unchanged.equals(library.snapshot().toString()),"fractional "+field+" rejected atomically");}
        }
        // Deletion of the complete library''')

path = 'core/src/test/java/game/sanguo/core/CoreTest.java'
edit(path, '        Hex destination=null;for(Hex h:reachable.keySet())if(!h.equals(initial)){destination=h;break;}', '''        // This checks field commands; landing inside a seven-cell city legitimately auto-enters.
        Hex destination=null;for(Hex h:reachable.keySet())if(!h.equals(initial)&&w.cityAt(h)==null){destination=h;break;}''')
edit(path, '        check(w.enter(1,0).ok&&w.units.isEmpty(),"enter adjacent friendly city");', '''        check(!w.enter(1,0).ok,"cannot enter before reaching the city footprint");
        w.unit(1).hex=w.city(0).hex;
        check(w.enter(1,0).ok&&w.units.isEmpty(),"enter reached friendly city footprint");''')

path = 'app/src/androidTest/java/game/sanguo/mobile/GameSmokeRunner.java'
edit(path, 'click("曹操军",true)', 'click("选择势力 · 曹操军",true)', 3)
edit(path, 'if(text.equals("返回全国列表")||text.startsWith("导航 · ")', 'if(text.startsWith("选择势力 · ")||text.equals("返回全国列表")||text.startsWith("导航 · ")')

path = 'scripts/test-custom-officers.sh'
edit(path, 'core/src/test/java/game/sanguo/core/CustomOfficerTest.java >>', 'core/src/test/java/game/sanguo/core/CustomOfficerTest.java core/src/test/java/game/sanguo/core/OfficerPackArchiveTest.java >>')
edit(path, 'game.sanguo.core.CustomOfficerTest\n', 'game.sanguo.core.CustomOfficerTest\njava -cp core/build/custom-officers:core/src/main/resources game.sanguo.core.OfficerPackArchiveTest\n')

add('scripts/audit-officer-baseline.py', r'''#!/usr/bin/env python3
"""Compare legacy suites against main and candidate using identical current test fixtures.
This is diagnostic, not a replacement for test-core.sh or Gradle check.
Exit nonzero for any changed failing outcome, timeout, compilation error, or new failure.
"""
import concurrent.futures
import json
from pathlib import Path
import re
import shlex
import subprocess
import sys

candidate = Path.cwd()
baseline = Path(sys.argv[1]).resolve()
out = candidate / 'out/custom-officers/legacy-audit'
out.mkdir(parents=True, exist_ok=True)
commands = []
for line in (candidate / 'scripts/test-core.sh').read_text().splitlines():
    if line.startswith('java -cp') and '"$mode"' not in line:
        commands.append(shlex.split(line)[3:])
for mode in ('fee', 'food', 'progress'):
    commands.append(['game.sanguo.core.LogisticsRegressionProbe', mode])
tests = list((candidate / 'core/src/test/java').rglob('*.java')) + list((candidate / 'core/src/testFixtures/java').rglob('*.java'))
tests = [p for p in tests if p.name not in ('CustomOfficerTest.java', 'OfficerPackArchiveTest.java')]

def execute(root, label):
    classes = out / (label + '-classes')
    classes.mkdir(exist_ok=True)
    sources = list((root / 'core/src/main/java').rglob('*.java')) + tests
    argfile = out / (label + '-sources.txt')
    argfile.write_text('\n'.join(str(p) for p in sources))
    build = subprocess.run(['javac', '-encoding', 'UTF-8', '--release', '17', '-d', str(classes), '@' + str(argfile)], capture_output=True, text=True, timeout=120)
    (out / (label + '-compile.txt')).write_text(build.stdout + build.stderr)
    if build.returncode:
        raise RuntimeError(label + ' compilation failed; inspect compile log')
    cp = ':'.join(map(str, (classes, root / 'core/src/main/resources', candidate / 'core/src/test/resources')))
    def test(args):
        name = '-'.join(args).replace('game.sanguo.core.', '')
        try:
            result = subprocess.run(['java', '-Dfile.encoding=UTF-8', '-cp', cp] + args, capture_output=True, text=True, timeout=90, cwd=candidate)
            log = result.stdout + result.stderr
            reason = next((line for line in log.splitlines() if line.startswith('Exception in thread')), '')
            code = result.returncode
        except subprocess.TimeoutExpired:
            log, reason, code = 'TIMEOUT', 'TIMEOUT', 124
        (out / (label + '-' + name + '.txt')).write_text(log)
        return name, {'code': code, 'firstError': reason}
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        return dict(pool.map(test, commands))

before = execute(baseline, 'main')
after = execute(candidate, 'candidate')
changed = {name: {'main': before[name], 'candidate': result} for name, result in after.items()
           if result['code'] == 124 or result['code'] != 0 and result != before[name]}
report = {'baseline': subprocess.check_output(['git', '-C', str(baseline), 'rev-parse', 'HEAD'], text=True).strip(),
          'candidate': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip(),
          'sameCurrentFixtures': True, 'main': before, 'candidateResults': after, 'changedFailures': changed}
(out / 'comparison.json').write_text(json.dumps(report, ensure_ascii=False, indent=2))
print('LEGACY COMPARISON:', len(commands), 'suites;', sum(r['code'] != 0 for r in before.values()),
      'main failures;', sum(r['code'] != 0 for r in after.values()), 'candidate failures;', len(changed), 'changed failures')
print('Existing failures remain visible in comparison.json and do NOT constitute a green full regression.')
if changed:
    print(json.dumps(changed, ensure_ascii=False, indent=2))
    sys.exit(1)
''')

p = Path('docs/custom-officers.md')
p.write_text(p.read_text() + '''
## 本次收尾与全量回归边界

- 人物 ZIP 经 `OfficerPackArchive` 同时检查尾目录、本地条目、文件名、偏移、长度与 CRC；拒绝截断、分卷、ZIP64、加密、异常路径及超限解压。除有清单的 ZIP 外，旧 JSON 配置入口保留。新增纯 Java 数据包回归及 Android 库原子性回归。
- 义理、话术掩码、分配器和修订号必须为真正整数；来源标识须为 UUID，不再接受 JSON 小数的隐式截断。
- 旧 CoreTest 行军检查避开会自动进驻的城市占地，回城检查先到达合法占地。安卓势力选择回归按稳定的“选择势力 · 曹操军”无障碍描述定位，不要求显示文案退回旧势力名。
- `scripts/audit-officer-baseline.py <main-worktree>` 以相同现行测试和 fixture 分别编译 main 与候选正式核心，记录每一项历史套件的退出状态与首个异常。对新增/改变的失败和超时失败，不删除原 `test-core.sh` / Gradle check，也不把已有失败包装为全量通过。
- 旧回归还包含早期存档迁移、单格据点 fixture 和旧 AI 行为预期等问题。本轮不回退现行地图/存档规则来迎合过时测试；具体范围以本次构建的 `legacy-audit/comparison.json` 和日志为准。
''')
