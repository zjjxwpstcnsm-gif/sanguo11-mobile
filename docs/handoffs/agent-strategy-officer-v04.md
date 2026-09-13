# Agent 1：战略内政 / 武将系统 v0.4 交接

> 原始补丁交接记录；已由 v0.5 集成正式落地。下文旧菜单路径和原分支测试环境属于历史信息，当前入口、构建和验证以 [v0.5 集成](../INTEGRATION_V0_5.md) 为准。

## 基线与交付范围

- 目标分支：`agent/strategy-officer-v04`，目标主干：`main`。
- 远端起点：`f23d656a92781573226fbcf03aa92c79da46f2bf`。
- 当前容器没有预克隆仓库，也不能解析 GitHub；经已授权 GitHub 连接器读取仓库，并取回成功 CI 的 `726de6369922f6e5934dbbcc4fcba6ce1128256f` 精确源码快照。它与 f23 的可执行源码完全相同，f23 只修改交付文档。本轮补丁不回退这些文档。
- 工作独立完成，不依赖其他 Agent 的分支，不合并或写入 main。最终远端交付保留真实 f23 历史。
- 规则是可玩的工程实现，不宣称已核验为原版《三国志11》的精确概率和数值。

## 已完成

1. **统一武将行动约束**：沿用 `acted`，从实际建设、调动、运输、部队记录派生状态；新增其他跨旬任务锁。生产、建设、调动、运输、出征及所有新命令共享可用性校验，不能并行占用同一武将。短命令每次消耗 10 行动力，失败校验不改变资源、日志或随机数。
2. **搜索**：按政治、智力计算发现率；隐藏人才按城市和登场旬数筛选。结果分为发现本城在野武将、少量金、无发现；发现不等于自动登用。每名人才只出现一次。
3. **登用**：本城在野武将，或六格内忠诚不高于 60、未行动且不执行任务的敌方非君主武将。魅力、政治、目标忠诚和可选势力关系共同决定概率。金 100 / 行动力 10；概率失败也消耗命令资源，新入仕者本旬休整。策反太守时原城立即失去任命。
4. **褒奖与忠诚**：金 200，提高 6～14 点（由目标属性决定）并封顶 100；同一目标每旬最多接受一次。受赏者可以已经执行过短命令，但不能在长期任务中；执行者仍必须可行动。贫困或低治安城每月使非君主武将忠诚下降 2，最低 0。君主忠诚固定 100。
5. **身份和太守**：君主、太守、普通、在野；任命消耗执行者和被任命者当旬行动（可由本人执行）。太守政治每 4 点增加 1% 金粮收入，最高 25%；参与实际月结算。调动、运输、出征、城陷及被策反会解除太守；君主兼任太守不会丢失君主身份。
6. **巡察**：金 100，政治/魅力决定恢复量，治安封顶 100。治安影响金粮月收入、征兵数量及 AI 优先级。
7. **有限征兵**：城市初始抽象兵源 20,000，征兵逐人扣减，不会凭空无限生成。数量综合兵源、治安、魅力及已建成兵舍；每次金 300、治安 -5，治安低于 30 禁止征兵。保留 100,000 城市兵力上限。
8. **训练与气力**：金 100，统率/武力决定训练增幅，封顶 100；没有守军不能训练。复用原 `City.morale` 而非新增互相矛盾的训练数值；已有出征流程以此初始化部队气力。
9. **战略 AI**：按资源、驻将数量、兵源、兵力、气力、治安、低忠诚核心人才与附近敌城/敌军压力选择搜索、登用、褒奖、巡察、征兵、训练及任命。计划只读、同分稳定排序；执行使用玩家同一套命令。紧急治理和普通决策在旧军事/建设 AI 前执行，避免最后一名文官先被出征占用。
10. **可用 Android 入口**：`菜单 → 政务与在途 → 城名 · 人事 / 城市治理 / 武将状态`。提供状态、概率、成本预览和执行确认；新增页面不直接改资源。原城市征兵/巡察/训练入口兼容保留。
11. **可搜索剧本数据**：两个内置测试包分别新增 2 / 3 名明确标注为原创虚构的测试隐士，初始现役人数仍为 6 / 18。没有导入原版游戏素材、二进制或官方武将库。

## 规则接口

入口为 `world.strategy`（`game.sanguo.core.Strategy`），命令返回既有 `World.Result`：

```java
OfficerState officerState(int officerId);
boolean busy(int officerId); // 仅新增的其他长期任务；完整状态请用 officerState
World.Result beginAssignment(int cityId, int officerId, String label, int turns);
SearchResult searchTalent(int cityId, int officerId);
World.Result search(int cityId, int officerId);
int searchChance(int officerId);
List<World.Officer> recruitmentTargets(int cityId);
boolean canRecruitTarget(int cityId, int targetId);
int recruitmentChance(int cityId, int officerId, int targetId);
World.Result recruitOfficer(int cityId, int officerId, int targetId);
World.Result rewardOfficer(int cityId, int officerId, int targetId);
World.Result appointGovernor(int cityId, int officerId, int targetId);
int governorPolitics(int cityId);
int cityIncome(int cityId, int base);
World.Result patrol(int cityId, int officerId);
int recruitAmount(int cityId, int officerId);
World.Result recruitSoldiers(int cityId, int officerId);
int getArmyReadiness(int cityId);
World.Result trainArmy(int cityId, int officerId);
StrategicAi.Decision planAi(int cityId);
int strategicPressure(int cityId);
```

`SearchResult` 包含 `result / outcome / officerId / goldFound`；`OfficerState` 是不可变快照，包含位置、势力、身份、忠诚、任务类型、剩余旬数、acted 和 canAct。canAct 表示武将自身就绪，不代表具体命令的金/AP也足够。

剧本/测试配置使用 `setSeed(long)`、`getRandomState()`、`addHiddenTalent(Talent)`、`hiddenTalents()`、`setFactionRelation(a,b,value)` 和 `factionRelation(a,b)`。这些不是供玩家无限重掷/免费修改关系的命令。关系为对称 -100～100 的基础修正，不是完整外交。

纯规则集中于 `game.sanguo.core.strategy.StrategyRules`，UI、AI、测试复用。整数截断均向零；不依赖系统时间或未设种子的 Random。SplitMix64 全状态进入存档，同 seed + 同命令序列 + 同起点产生相同结果；拒绝命令不推进随机状态。

旧 `World.recruit/train/patrol` 保留并委托给 Strategy；另保留 `World.getArmyReadiness` 便利入口。战斗 Agent 直接读取该接口/已有 City.morale，不要建立第二套不同步气力。

## 存档变化

写入 **v4**，读取 v1 / v2 / v3 / v4。保留原 magic、长度上限、CRC 外壳及前面的所有字段顺序。在 v3 的 Domestic 记录之后、日志之前追加带 `STR4` 标记的人员治理扩展：

- 64 位随机数状态。
- 以城市 ID 为键保存兵源、太守 ID；治安/气力仍在旧城市记录内。
- 以武将 ID 为键保存忠诚、身份、其他任务名称/剩余旬数、最近褒奖旬数；位置/acted 等仍在旧记录内。
- 隐藏人才完整属性及登场旬数。
- 排序、规范化的非零势力关系修正。

旧档默认兵源 20,000、普通武将忠诚 85、无太守/其他任务/褒奖记录。每个已有势力用最低 ID 的所属武将确定默认君主、忠诚 100；这只是确定性的工程迁移策略。旧档不凭空加入新版剧本的隐藏人才，也不依赖新版外部剧本文件。

编码和解码都校验范围、引用、太守/君主一致性、任务互斥和人才 ID 唯一性；未知未来版本、损坏 CRC、截断及重算 CRC 后的非法扩展字段均拒绝。

新增真实 v3 夹具 `core/src/test/resources/v03-strategy.sg11.b64`，由修改前 v3 编码器产生，含在建市场与待运金粮兵/兵装。文件 SHA-256：`627436cbf74c48167b57902bfea44de5a69f0c2dd49c9988d16fe87696ab69ba`。原 v1/v2 夹具未改动。

## 核心文件

- 新增：`core/.../Strategy.java`（服务）、`StrategySave.java`（扩展序列化/校验）、`StrategicAi.java`（决策/执行）、`strategy/StrategyRules.java`（纯公式）。
- 小范围修改：`World.java`（兼容字段、占用和回合钩子）、`Domestic.java`（收益/太守离城钩子）、`SaveCodec.java`（v4扩展）、`ScenarioData.java`（可选隐藏人才）。
- 新增 Android `StrategyUi.java`；`DomesticUi.java` 仅加入口，`MainActivity.java` 仅改三个过时固定增量按钮标签。
- 新增 `StrategyTest.java`、v3夹具；原测试只更新版本/实际收益预期及手工删城夹具对应的隐藏人才清理，没有删去测试用例。
- `core/build.gradle` 和 `scripts/test-core.sh` 加入新测试执行器；不改全局 Gradle、Android 构建依赖、地图渲染器或战法实现。
- 内置 properties 修订为 2，`scenarios/index.txt` 随内容重算 SHA-256；数据格式仍为兼容的 1。
- `GameSmokeRunner.java` 追加真实点击搜索/登用/任命/褒奖/巡察/征兵/训练及重启读档检查。

## 测试结果

本地 JDK 21 以 `--release 17` 编译，执行仓库完整命令：

```text
LC_ALL=C.UTF-8 bash scripts/test-core.sh
CoreTest       188 PASS
ScenarioTest   359 PASS
DomesticTest   669 PASS
StrategyTest  6777 PASS
合计          7993 次断言通过
```

原始基线完整测试先运行通过（1196 次）。部分旧套件按游戏何时结束循环断言；战略 AI 改变了对局长度，因此断言总数随实际轨迹变化，并非删去用例。新测试包含 128 个 seed、概率参数矩阵、失败不变性、七类 AI 决策及三势力长局配对读档续局。

另运行 `javac -Xlint:all -encoding UTF-8 --release 17 ...`，无告警。`git diff --check` 通过。

本地没有 Gradle Wrapper、Gradle 安装和 Android SDK，不能将本地未运行的 Android 构建声称为通过。使用仓库同版本 CI 环境验证 Android；实际结果附在本文件末尾或对应 PR 检查中。

## 未完成 / 明确边界

- 不包含原版完整人物、特技、关系网、官爵俸禄、军师/都督、单挑、舌战、完整外交或战法动画。
- 兵源是有限的抽象储备，尚未实现人口自然增长、民兵恢复、征兵回流；不会每旬免费回满。
- 忠诚下降是每月城市治理规则，未实现全部性格、亲爱/厌恶、俘虏、自动背叛事件。登用限制及概率均为本轮工程规则。
- `beginAssignment` 提供可持久化的其他任务锁，未添加无需求的“任意任务”手机按钮。
- 未大改战斗数值、地图渲染、原生 UI 框架；没有声称完成原版全量复刻。

## 合并注意

1. 以独立分支提 PR，勿覆盖其他 Agent 的 World/SaveCodec。公共模型只新增字段；保留 `available()`、出征/退却/城陷的太守释放、nextTurn 的 strategy.tick 和两次 AI 钩子。
2. **若其他 Agent 也把存档升为 v4，不能只统一版本号就合并。** 必须整合扩展字段顺序，或建立一个新版本读取两个已发布 v4 变体，并增加相应真实夹具；不要删除 STR4 扩展。
3. `Domestic.monthlyGold/monthlyFood` 已包含治安/太守修正，UI/回合/另一服务不要再乘一次。`World.recruit` 已扣兵源，外部不要重复扣减。
4. 新隐藏人才与在野不等同现役军官。战斗/地图遍历人员时不要以所有 World.officers 都有合法非负 owner 为前提；原城池在野不随城陷撤退。
5. 新增任何跨旬人员任务都必须接入统一可用性和 StrategySave 互斥验证，不能只改 acted。临时状态显示只读 OfficerState，不另存一套互相矛盾的枚举。
6. 原策略/运输/开发功能和 Android 装机流均应重跑。完整回归入口是 `bash scripts/test-core.sh` 或 `gradle :core:check`，不是只跑默认无 JUnit 用例的 `test`。
7. 数据修改须更新索引哈希，存档自身继续使用嵌入数据，不能用新剧本重置旧档资源/人才。
8. 三个 Agent 合并后的 APK 是另一份构建，仍需重新装机验证；本分支构建只能证明本分支。
