#!/usr/bin/env bash
# Compare two independent encoders on the SAME JVM, rather than confusing
# Android/JVM collection iteration differences with a terrain migration.
set -euo pipefail
cd "$(dirname "$0")/.."
base=894fb37729a6425c670026616f0ee32efa795790
mkdir -p out/legacy-encoder/src out/legacy-encoder/classes out/baseline-canonical
git archive "$base" core/src/main/java core/src/main/resources | tar -x -C out/legacy-encoder/src
find out/legacy-encoder/src/core/src/main/java -name '*.java' > out/legacy-encoder/sources.txt
javac -encoding UTF-8 --release 17 -d out/legacy-encoder/classes @out/legacy-encoder/sources.txt
cat > out/legacy-encoder/CanonicalLegacy59.java <<'JAVA'
import java.nio.file.*;
import game.sanguo.core.SaveCodec;
public final class CanonicalLegacy59 {
 public static void main(String[] a)throws Exception {
  for(String id:new String[]{"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"}) {
   String name="baseline-v058-"+id+".sg11";
   Files.write(Path.of(a[1],name),SaveCodec.encode(SaveCodec.decode(Files.readAllBytes(Path.of(a[0],name)))));
  }
 }
}
JAVA
javac --release 17 -cp out/legacy-encoder/classes -d out/legacy-encoder/classes out/legacy-encoder/CanonicalLegacy59.java
java -cp out/legacy-encoder/classes:out/legacy-encoder/src/core/src/main/resources CanonicalLegacy59 out/baseline/android out/baseline-canonical
printf '%s\n' "$base" > out/LEGACY_ENCODER_COMMIT
sha256sum out/baseline/android/baseline-v058-*.sg11 out/baseline-canonical/* > out/LEGACY_SAVE_HASHES
