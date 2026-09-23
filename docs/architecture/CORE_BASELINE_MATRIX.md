# Original core test matrix

Exact old source: `9548bb350051150b21a61213f9068ffb1b7506c0`. Each of the 42 original main/probe invocations from test-core.sh was also run independently, so the original script's early failure does not hide downstream failures. No assertion or test was removed/relaxed. No timeout occurred. Both trees: 12 pass / 30 fail, identical exit codes. Core logic changes in this refactor are only the public rejected-result construction adapter and four package/import moves; no game formula is repaired here.

| Original invocation | Baseline exit | Refactored exit | Result / original failure |
|---|---:|---:|---|
| `CoreTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: AI uses deployment commands |
| `ScenarioTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: combat deployment |
| `DomesticTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: unfinished market gives no yield |
| `battle.TacticalBattleTest` | 0 | 0 | PASS:  |
| `battle.BattlePerformanceTest` | 0 | 0 | PASS:  |
| `StrategyTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: 尚未抵达合法据点入口（上下河须经港口） |
| `CampaignTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 设施位置冲突或无效 |
| `ArmyTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: 尚未抵达合法据点入口（上下河须经港口） |
| `RulesParityTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: 请选择当前势力的部队 |
| `GovernmentTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 据点占地越界、重叠或地形冲突：丙城 @ 18,5；请使用七格地图重新开局 |
| `SupplyTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 据点占地越界、重叠或地形冲突：丙城 @ 18,5；请使用七格地图重新开局 |
| `ContestTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 旧版本地图存档无法继续使用，请保留原档并重新开局；未删除或改写原存档 |
| `AbilityResearchTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 太守位置或所属势力无效 |
| `TechnologyFieldworksTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 旧版本地图存档无法继续使用，请保留原档并重新开局；未删除或改写原存档 |
| `EstatesTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 旧版本地图存档无法继续使用，请保留原档并重新开局；未删除或改写原存档 |
| `WorldSystemsTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: diplomatic per-card replay |
| `MarchOrdersTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: water route crosses river |
| `CampaignAiTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: adjacent resupply consumes actual stock and action points |
| `LifecycleTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 旧版本地图存档无法继续使用，请保留原档并重新开局；未删除或改写原存档 |
| `ContentProfilesTest` | 0 | 0 | PASS:  |
| `BattleFeedbackTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 据点占地越界、重叠或地形冲突：丙城 @ 18,5；请使用七格地图重新开局 |
| `PrisonerEscortTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 据点占地越界、重叠或地形冲突：丙城 @ 18,5；请使用七格地图重新开局 |
| `DiplomacyTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 设施位置冲突或无效 |
| `FacilityCombatTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 据点占地越界、重叠或地形冲突：丙城 @ 18,5；请使用七格地图重新开局 |
| `MobileShortcutsTest` | 0 | 0 | PASS:  |
| `TerritoryAiTest` | 0 | 0 | PASS:  |
| `StrategicManagementTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 旧版本地图存档无法继续使用，请保留原档并重新开局；未删除或改写原存档 |
| `StrategicRegressionProbe_staff` | 0 | 0 | PASS:  |
| `StrategicRegressionProbe_objective` | 0 | 0 | PASS:  |
| `LogisticsCampaignTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.io.IOException: 旧版本地图存档无法继续使用，请保留原档并重新开局；未删除或改写原存档 |
| `TacticalLogisticsTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: raid command uses identical normal combat RNG and consequences |
| `DisplacementTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: city deterministic replay |
| `MapSkillsTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: restored v40 compact map uses 100 by 100 coordinates |
| `NavigationDefenseTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: direct passable northern land route [20036, 20017] got -1 |
| `ArchitectureRulesTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: source death and reload preserve fire power and continuation |
| `BalanceTest` | 0 | 0 | PASS:  |
| `Release42Test` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: hostile gate blocks adjacent bypass |
| `FacilityProductionTest` | 0 | 0 | PASS:  |
| `ObjectiveOrdersTest` | 1 | 1 | FAIL (inherited): Exception in thread "main" java.lang.AssertionError: QuA on the source northwestern Wu shore huangjin-184 |
| `LogisticsRegressionProbe_fee` | 0 | 0 | PASS:  |
| `LogisticsRegressionProbe_food` | 0 | 0 | PASS:  |
| `LogisticsRegressionProbe_progress` | 0 | 0 | PASS:  |
