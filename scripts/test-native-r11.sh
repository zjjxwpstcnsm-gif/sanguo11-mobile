#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-3d-combat.sh
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp "app/build/combat-check:$JSON_TEST_JAR" -d app/build/combat-check app/src/main/java/game/sanguo/mobile/CombatReplayLedger.java app/src/main/java/game/sanguo/mobile/CombatSequence.java app/src/test/java/game/sanguo/mobile/NativeR11Test.java core/src/testFixtures/java/game/sanguo/core/FieldSceneFixture.java core/src/testFixtures/java/game/sanguo/core/UnitR10Fixture.java core/src/testFixtures/java/game/sanguo/core/NativeR11Fixture.java
java -Xmx1200m -cp "app/build/combat-check:core/src/main/resources:$JSON_TEST_JAR" game.sanguo.mobile.NativeR11Test
python scripts/check-architecture.py
