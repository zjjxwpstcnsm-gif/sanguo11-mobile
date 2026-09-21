#!/usr/bin/env bash
# Independent old source encoders on the SAME JVM; Android/JVM ordering is not migration.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/legacy60/raw out/legacy60/canonical
for revision in 58 59; do
  if test "$revision" = 58;then base=894fb37729a6425c670026616f0ee32efa795790;root=out/baseline58/android;else base=db274a08f4ed5c877ee2d9799ed17c98522c3295;root=out/baseline59/android;fi
  encoder="out/encoder$revision"
  mkdir -p "$encoder/src" "$encoder/classes"
  git archive "$base" core/src/main/java core/src/main/resources | tar -x -C "$encoder/src"
  find "$encoder/src/core/src/main/java" -name '*.java' > "$encoder/sources.txt"
  javac -encoding UTF-8 --release 17 -d "$encoder/classes" @"$encoder/sources.txt"
  cat > "$encoder/Canonical60.java" <<'JAVA'
import java.nio.file.*;
import game.sanguo.core.SaveCodec;
public final class Canonical60 {
 public static void main(String[] a)throws Exception {
  for(String id:new String[]{"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"}){
   String name="baseline-v0"+a[2]+"-"+id+".sg11";
   Files.write(Path.of(a[1],name),SaveCodec.encode(SaveCodec.decode(Files.readAllBytes(Path.of(a[0],name)))));
  }
 }
}
JAVA
  javac --release 17 -cp "$encoder/classes" -d "$encoder/classes" "$encoder/Canonical60.java"
  java -cp "$encoder/classes:$encoder/src/core/src/main/resources" Canonical60 "$root" out/legacy60/canonical "$revision"
  cp "$root"/baseline-v0"$revision"-*.sg11 out/legacy60/raw/
  printf '%s\n' "$base" > "out/legacy60/ENCODER_$revision"
  rm -rf "$encoder"
done
sha256sum out/legacy60/raw/* out/legacy60/canonical/* > out/LEGACY_SAVE_HASHES
