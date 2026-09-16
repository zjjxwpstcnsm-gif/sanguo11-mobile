#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/check
find core/src/main/java core/src/test/java -name '*.java' -print > core/build/sources.txt
if command -v javac >/dev/null 2>&1; then
  javac -encoding UTF-8 --release 17 -d core/build/check @core/build/sources.txt
else
  java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d core/build/check @core/build/sources.txt
fi
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.CoreTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.ScenarioTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.DomesticTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.battle.TacticalBattleTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.battle.BattlePerformanceTest

java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.StrategyTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.CampaignTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.ArmyTest

java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.RulesParityTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.GovernmentTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.SupplyTest

java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.ContestTest

java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.AbilityResearchTest

java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.TechnologyFieldworksTest

java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.EstatesTest

java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.WorldSystemsTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.MarchOrdersTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.CampaignAiTest

java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.LifecycleTest

java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.ContentProfilesTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.BattleFeedbackTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.PrisonerEscortTest

java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.DiplomacyTest

java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.FacilityCombatTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.MobileShortcutsTest
java -cp core/build/check:core/src/main/resources:core/src/test/resources game.sanguo.core.TerritoryAiTest
