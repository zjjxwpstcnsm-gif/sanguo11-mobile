package game.sanguo.core;

import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

/** Regression for the #58/#59/#60 merge, using isolated production-model fixtures. */
public final class EditorMergeIntegrationTest {
    private static int checks;
    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }

    public static void main(String[] args) throws Exception {
        MapPatch patch = MapEditorContinuationTest.fixture();
        World map = CustomMaps.load(patch, patch.preview, 0, 20260922L);
        byte[] untouched = SaveCodec.encode(map);
        CustomOfficers.Definition officer = CustomOfficerTest.person(100901, "合并验收将");
        CustomOfficers.Definition wild = CustomOfficerTest.person(100902, "新城在野将");
        List<CustomOfficers.Definition> definitions = new ArrayList<>(Arrays.asList(officer, wild));
        World combined = CustomOfficerTest.apply(map, definitions, Arrays.asList(
            CustomOfficerTest.put(officer, 0, map.home().id),
            CustomOfficerTest.put(wild, -1, 100006701)));
        check(Arrays.equals(untouched, SaveCodec.encode(map)), "officer composition leaves map input unchanged");
        check(combined.customMapId.equals(map.customMapId), "map identity survives officer composition");
        check(combined.customMapFingerprint.equals(map.customMapFingerprint), "map fingerprint survives officer composition");
        check(combined.siteParents.equals(map.siteParents), "port and gate parent associations survive composition");
        check(combined.officer(wild.runtimeId).cityId == 100006701, "officer placement accepts actual new custom-map city");
        check(combined.government.commandLimit(officer.runtimeId) > 0, "custom officer uses merged government command rules");
        combined.extensions.put("futureModule", new byte[] {1, 2, 3});
        byte[] saved = SaveCodec.encode(combined);
        definitions.clear();
        patch.name = "后来修改的地图库";
        World restored = SaveCodec.decode(saved);
        check(restored.customMapName.equals(map.customMapName), "saved map remains pinned after library mutation");
        check(restored.officer(officer.runtimeId).name.equals("合并验收将"), "saved officer survives template removal");
        check(restored.city(100006701) != null, "saved custom city is self contained");
        check(restored.siteParents.equals(map.siteParents), "saved parent associations retained");
        check(restored.extensions.get(CustomOfficers.NAMESPACE) != null, "officer snapshot and map tail coexist");
        check(Arrays.equals(restored.extensions.get("futureModule"), new byte[] {1, 2, 3}), "unknown namespace retained");
        check(Arrays.equals(saved, SaveCodec.encode(restored)), "combined save has canonical round trip");
        check(Arrays.equals(untouched, SaveCodec.encode(SaveCodec.decode(untouched))), "map-only v33 still round trips");

        // Reconstruct the supported v32 wire layout: governance followed by optional namespaces,
        // with no v33 map tail. This is a format fixture, not a claimed original-device save.
        World plain = CustomOfficerTest.base();
        check(SaveCodec.decode(asV32(plain)).customMapId.isEmpty(), "plain v32 wire layout remains readable");
        World oldOfficers = CustomOfficerTest.apply(plain, Collections.singletonList(officer),
            Collections.singletonList(CustomOfficerTest.put(officer, 0, plain.home().id)));
        World migrated = SaveCodec.decode(asV32(oldOfficers));
        check(migrated.officer(officer.runtimeId).name.equals("合并验收将"), "officer v32 wire layout migrates");
        check(migrated.extensions.get(CustomOfficers.NAMESPACE) != null, "v32 officer snapshot not discarded");
        System.out.println("EDITOR MERGE PASS: " + checks + " composition and save-compatibility assertions");
    }

    private static byte[] asV32(World world) throws IOException {
        byte[] current = SaveCodec.encode(world);
        ByteArrayOutputStream mapBytes = new ByteArrayOutputStream();
        CustomMapSave.write(world, new DataOutputStream(mapBytes));
        ByteArrayOutputStream extensionBytes = new ByteArrayOutputStream();
        world.extensions.write(new DataOutputStream(extensionBytes));
        int mapStart = current.length - extensionBytes.size() - mapBytes.size();
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(current, 20, mapStart - 20);
        payload.write(extensionBytes.toByteArray());
        byte[] body = payload.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(body);
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(file);
        out.writeInt(0x53473131); out.writeInt(32); out.writeInt(body.length);
        out.writeLong(crc.getValue()); out.write(body);
        return file.toByteArray();
    }
}
