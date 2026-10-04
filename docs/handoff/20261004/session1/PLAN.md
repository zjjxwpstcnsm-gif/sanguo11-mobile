# Session 1 计划与文件所有权

目标 active；任何批次不代表全部剧本、全武将、正常流程和 ARM 已完成。

实现基点：`840030195e39cec3c0d352e010a3c3e614893ca2`。继承 checkpoint 的 4299 文件、336374894 字节逐 SHA 全等，168 固定资源通过，4 份 JNI 按原相对路径保留。基线提交 `0a6fb12`；仅封存继承差异，不含人物/剧本新实现。checkpoint 的脚本权限缺少 executable，随后按实现基点的 Git mode 恢复，文件内容不变。

独立目录 `/Users/paopao/.codex/worktrees/scenario-officer-restoration/sanguo11-mobile`，分支 `codex/scenario-officer-restoration`，可写缓存限本目录 `out/session1/`。原目录、PC 安装只读，不启动 Wine。

首批确切文件：

- `docs/handoff/20261004/session1/PLAN.md`
- `docs/handoff/20261004/session1/CONTRACT.md`
- `docs/handoff/20261004/session1/baseline-verification.json`
- `docs/handoff/20261004/session1/inherited-source-manifest.json`
- `tools/content/audit_pc_restoration_sources.py`
- `tools/content/test_pc_restoration_sources.py`
- `docs/handoff/20261004/session1/source-manifest.json`

后续允许入口（实际修改前按批次追加前镜像 SHA）：

- core 的 `ScenarioCatalog.java`、`ScenarioData.java`、`ContentRuntime.java`、`ContentCatalog.java`、`World.java`、`SaveCodec.java`；新来源 DTO/转换器与相关规则测试。
- game-api 的 `GameApi.java` 及新只读 DTO。
- game-runtime 的 `GameSession.java` 和来源/人物只读 query、相关测试。
- app 的 `MainActivity.java` 仅剧本选择、新局配置、武将文字详情方法；`OverviewUi.java` 仅武将列表与搜索文本；`ContentUi.java` 仅人物文字资料；`ScenarioFactionPicker.java` 仅剧本势力配置。均位于 `app/src/main/java/game/sanguo/mobile/`。
- 新剧本与人物 metadata 资源、数值/文字导入工具和相应验证。

不编辑 PortraitCatalog、OfficerPortrait、图像资产、SoundEffects、音频播放器、AndroidGameBridge、Unity 或 3D。不改双方共同人物资产 manifest。不并写 progress.md、STATE.json、PC_PARITY_STATUS.md。MainActivity 最终整合须按方法范围与前镜像 SHA 检查，不覆盖媒体增量。

路线：来源 manifest/实际加载链与字形/传记 → 逐字段原 serializer/getter/开局后处理 → 确定性剧本包、正常新局及统一人物 DTO → 旧 31–37 完整 Save/RNG 续行 → 独立 APK 构建、真实菜单多旬存取退出重开。全部来源差异独立保留；未知明确保留。

每次安装前核实设备锁和无人使用，备份全部保存/库/偏好，结束后读回精确恢复。不移用旧包证据。批次提交、增量 SHA、验证与缺口放本目录；最终顺序集成，不复制媒体 WIP。

## Batch 04 当前所有权

新增tools/content/inspect_pc_layered_scenario.py、inspect_pc_scenario_fields.py、test_pc_layered_scenario.py及本目录分层载入/原属性/兼容调查报告。执行原492db0完整构造、Shared与剧本同一注册表载入、4937b0读取及明确边界的后处理；不能手工回填静态表冒充加载。沿用原工具作对照，原报告与公共台账不改。未知全局、菜单设置、MOD优先级或事件未闭合时不标完整开局。新增核心/保存策略修改须再登记准确路径和前镜像SHA；当前未改变旧v33行动力策略。

追加tools/content/pc_readonly_platform.py：仅为原事件读取器提供已核实Win32文件/目录、单线程锁和内存分配平台边界。虚拟G盘映射到PC只读输入目录，写入/删除/截断接口必须拒绝；记录每个原函数要求的实际资源和SHA。不是Wine，不启动PC游戏，不替换事件/规则函数；模拟的进程路径和未执行的应用上下文明确列入边界。

追加tools/content/inspect_pc_event_resources.py与event-resources-native报告：执行679cb0安装目录发现、678550头验证与原解码/标识校验，明确停在679d9f进入配置/Documents Expansion目录之前。平台仅支持已核实PE导入及受限CRT标准接口，未知主机上下文不得默认为真实生效优先级。

## Batch 05 当前所有权

前镜像d3c6ab94eed0e4777d4933592145c4222a04822b。本轮新增tools/content/pc_startup_platform.py、inspect_pc_started_scenario.py、test_pc_started_scenario.py及session1的启动链/覆盖/验证记录；必要时修改inspect_pc_layered_scenario.py的明确恢复接口，逐文件登记前镜像SHA。先以原CRT线程/字符表初始化与原排序、事件资源完整装配闭合已知断点；Win32文档路径等只能作为明确隔离夹具，不升级实际激活优先级。仍不改媒体、共同台账、AndroidGameBridge、Unity和3D。

追加tools/content/inspect_pc_event_bytecode.py、test_pc_event_bytecode.py：复用已核实原事件文件/正文范围，执行原字节码容器构造、头绑定、指令/字符串getter；不执行事件条件、玩法或UI处理器。与人物数值后处理报告保持独立，任何原字形解码失败保留原字节。

## Batch 06 当前所有权

前镜像471e970645b7073b32b6d18399f54dcb207d73f3。新增 core/PcScenarioIdentity.java、PcScenarioCatalog.java、PcScenarioOpening.java、core/resources/pc-scenarios/、tools/content/build_pc_scenario_catalog.py及对应测试；修改 World、SaveCodec、BattleReports、ScenarioCatalog、OfficerAbilities、Lifecycle、Strategy、StrategySave、Government、MerchantMarket、game-runtime/build.gradle；app仅 MainActivity 的剧本选择/新局方法及 ScenarioFactionPicker 的新局配置。实际修改前镜像在 batch06-before.json。新增来源新局字段扩展、47势力边界和明确的新存档38策略；旧31–37编码/载入/已存规则不得改变，不从新目录追填。源NPC与可派遣武将、未登与未发现必须区分；严格身份仍666，四个未知历史身份不得拿native编号冒充项目ID。所有数值输入来自原serializer/getter并保留逐源SHA，开局事件未闭合时明确标记，不把静态元数据声称成原官方开局。新局接入和APK正常流程仍是交付门槛。

Batch06补充确切测试所有权：game-runtime/src/test/java/game/sanguo/core/PcScenarioFrameTest.java、tools/content/PcScenarioLegacyProbe.java及session1对应帧/兼容报告。容量夹具的typed命令/多回合只验证框架边界，不替代实际PC来源开局或设备流程。

兼容闭合追加 core/BasicCityPolicy.java 和 CityActionPlan.java。显式规则：无新策略标记、且无已存v34基础/成长策略的旧31–33档保留旧巡察公式与10AP；已存v34–37仍为20AP。新建但未启用基础策略的作者世界使用独立保存标记以保留20AP；解码旧档不补标记。新游戏实际数据导入和旧档历史策略证据分别验收，不更改任何golden。

验证脚本追加 scripts/test-unity-u01.sh，仅把已存在的 game-runtime/src/test/java/game/sanguo/core/BridgeFactsFixture.java 纳入实际Java编译输入；不改Unity文件、原fixture、bridge协议或生产序列化。新增 BasicCityPolicyTest.java 位于game-runtime/src/test/java/game/sanguo/core。

## Batch 07：实际来源开局转换

确切所有权：tools/content/inspect_pc_opening_references.py、build_pc_scenario_catalog.py及相应test_pc_opening_references.py；core/PcScenarioCatalog.java、PcScenarioOpening.java、PcScenarioPeople.java（源身份/原状态保存）、core/resources/pc-scenarios/*；game-runtime/.../PcScenarioOpeningTest.java。依赖修改的确切前镜像列于batch07-before.json；MainActivity仅新局菜单/配置路径，ScenarioFactionPicker仅来源/可选势力文字与配置。资料统一沿用PcOfficerSources，未解字形保留source-only身份，不声称全670已严格映射。外部覆盖仍unknown，仅给安装候选来源明确开局选项；不能标官方完整。禁止修改任何媒体或共同manifest。

Batch07追加确切来源初始化入口：MerchantMarket仅initializeSource（保存来源行情，初始不重抽）；Governance仅明确sourceFrame的原爵位保存/读取/继承边界，GovernmentSave仅有原保存证据的初始化任命入口；修改前SHA已追加batch07-before.json。不会降低旧世界校验、改变旧策略或公共媒体。

Batch07追加OfficerSnapshot.SourceInfo只读原字段/身份覆盖信息；保持已有nativeId/sourceVariant/officerId连接字段，四个source-only字形身份明确canonicalIdentityUnmapped。MainActivity只武将文字详情追加该DTO内容，所有肖像调用不变。

实际来源APK验收确切工具：tools/content/android/PcScenarioOpeningInstrumentation.java、build_pc_scenario_ui_probe.py、verify_pc_source_opening_ui.py。独立测试包只引用本批生产类；真实新局菜单/武将查阅/巡察/换旬/保存读取，保留所有设备文件，未改既有公共runner或媒体。

## 单挑与舌战：用户新增目标

2026-10-04用户明确要求一并完成单挑和舌战还原。此职责扩展使用相同本地PC只读来源，纳入最终验收而非独立静态演示。确切核心所有权：core/src/main/java/game/sanguo/core/Duel.java、Debate.java、Contests.java、ContestSave.java；必要的现有奖励/经验入口按实际修改前另行登记。确切运行边界：game-api/src/main/java/game/sanguo/api/ContestCommand.java、拟新增ContestSnapshot.java、game-runtime/src/main/java/game/sanguo/runtime/GameSession.java及拟新增query/ContestQuery.java。确切app文件：app/src/main/java/game/sanguo/mobile/ContestUi.java，只规则信息/按钮/命令路径；MainActivity只正常触发及信息入口，不改头像/音频/演出加载、AndroidGameBridge或3D。测试为core/src/test/java/game/sanguo/core/ContestTest.java、拟新增PcContestRulesTest.java、game-runtime拟新增ContestSessionTest.java及独立实际UI验收工具。

新增原信息工具：tools/content/inspect_pc_contest_catalog.py、后续inspect_pc_duel_rules.py、inspect_pc_debate_rules.py及相应原指令回归；实际拥有的输入输出及SHA逐批登记。现有Duel伤害/暴击/支援/气力、Debate牌组/手数/怒气/伤害/憤激时长均明示工程参数，不能当已还原。严格核实触发、携物、角色性格/话术、对战状态、撤退/胜负/捕获/奖励/经验，并完成中途保存→冷启动续战及重复/过期按钮拒绝，完整Save/RNG核对。旧已保存对战与策略必须显式保留，不能改公式后默默续算；新增来源仅影响明确新局，若需新保存策略另列兼容入口。

媒体会话继续拥有像素、头像变体/年龄选择、音频及演出资源；对战信息需求仅输出本独立契约文件，不并改其manifest/PortraitCatalog/OfficerPortrait/SoundEffects/播放器文件。
