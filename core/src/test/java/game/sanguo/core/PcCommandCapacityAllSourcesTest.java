package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Original constructed-domain evidence is not a claim of effective NPC activation. */
public final class PcCommandCapacityAllSourcesTest {
    public static void main(String[] args) throws Exception {
        byte[] raw;
        try (InputStream resource = PcCommandCapacityAllSourcesTest.class.getResourceAsStream(
                "/pc-command-capacity/original-capacities.tsv.gz")) {
            if (resource == null) throw new AssertionError("Original test evidence absent");
            raw = new GZIPInputStream(resource).readAllBytes();
        }
        if (!PcCommandCapacityPolicy.hex(MessageDigest.getInstance("SHA-256").digest(raw)).equals(
                "48b5319118cb9755b33852838881bace8ef2ff870e9756f5d34109f787572220"))
            throw new AssertionError("Pinned original test evidence SHA changed");
        Map<String, Map<Integer, Integer>> expected = new HashMap<>();
        for (String line : new String(raw, StandardCharsets.US_ASCII).split("\n")) {
            if (line.startsWith("sourceId")) continue;
            String[] fields = line.split("\t");
            if (fields.length != 6) throw new AssertionError("Original evidence columns changed");
            Integer old = expected.computeIfAbsent(fields[0], key -> new HashMap<>())
                    .put(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
            if (old != null) throw new AssertionError("Duplicate original identity");
        }
        if (expected.size() != 16) throw new AssertionError("Original source count changed");
        int count = 0;
        for (PcScenarioCatalog.Source source : PcScenarioCatalog.all()) {
            World world = PcScenarioCatalog.preview(source.identity.scenarioId);
            byte[] before = SaveCodec.encode(world);
            PcCommandCapacityPolicy.SourceRefs references = PcCommandCapacityPolicy.sourceRefs(
                    PcScenarioIdentity.saved(world));
            Map<Integer, Integer> values = expected.get(source.identity.scenarioId);
            if (values == null || values.size() != 1100) throw new AssertionError("Original domain changed");
            int mapped = 0;
            for (PcScenarioPeople.Person person : PcScenarioPeople.saved(world)) {
                if (person.officerId < 0) continue;
                PcCommandCapacityPolicy.Ref ref = references.rows.get(person.nativeId);
                if (ref == null || ref.id != person.officerId || !ref.sha.equals(person.recordSha))
                    throw new AssertionError("Original record/runtime identity changed");
                int actual = world.government.commandLimit(person.officerId);
                Integer original = values.get(person.nativeId);
                if (original == null || actual != original)
                    throw new AssertionError(source.identity.path + "/native" + person.nativeId
                            + "/runtime" + person.officerId + ": " + actual + " != " + original);
                mapped++;
                count++;
            }
            if (!Arrays.equals(before, SaveCodec.encode(world)))
                throw new AssertionError("Capacity projection mutated full World/RNG");
            System.out.println(source.identity.path + " mapped=" + mapped + " mismatches=0");
        }
        if (count != 10720) throw new AssertionError("Mapped identity coverage changed: " + count);
        System.out.println("PASS production commandLimit 10720 original mapped queries; "
                + "fullsave/bothRNG pure; effective NPC activation and governor election separate");
    }
}
