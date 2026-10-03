# 玩法与 UI 并行契约（2026-10-02）

本文件以完整工作区为基线，不能用旧 HEAD 替代。A 为最终集成负责人，拥有 core、game-api、剧本/武将内容、相关工具和规则测试；B 拥有独立目录中的 app UI/UX。game-runtime 是既有权威适配层，涉及它的接口修改先在本文件登记，再由 A 顺序集成。两方不共用工作目录、build/out 或项目 Gradle 缓存。

继承基线：`out/parity/baseline-20261002/manifest.json` 与 `source/`，HEAD 仅为历史身份 `52315bf070e5d29acb8f509230c5228e47c8d6ef`；3524 文件的完整内容才是本轮起点。`build-inputs.json` 另封存被 Git 忽略的双 ABI 原生 worker。UI 目录须复制这两部分，不能只从 HEAD 创建 worktree。设备 userdata 和用户存档不属于可随意复制、覆盖的构建输入。

## 输入与权威状态

R22能力日期契约已取证：原“能力变动”选项0有效/1无效，通过实际控件分发表写配置+8，再由正常开局写global+28。global+18非零使用剧本起年计算能力年龄并强制growthDisabled；此分支只跳过年龄曲线，不关闭经验/伤病/官职/内助。16源中SCEN007/Scen013/Scen014该字段为1，其余13源为0；不由这些文件推断项目heroes或MOD的身份。原native_age不把负年龄钳成0，不得直接以Lifecycle.age（含0下限）替代。新PcOfficerAbilityRules.age/growthDisabled及1040原输出对照仅为core内部算术；尚无新的UI可操作设置，SaveCodec33/API/命令事件不变。

后续人物状态接入须同时保存起年/月/日、经过旬数、固定年龄来源标志和用户所选能力变动值；effective值由core按原开局限制派生。能力状态须区分原基础5值/成长5码/经验5值/计算缓存及来源native编号，既有编辑/培养/研究/仲介/舌战写入入口须一起处理；旧档原五值和历史内助成果不反扣、不追溯重算。新来源局面与旧档/自定义兼容方式应有显式标记。此为接入边界，不宣称持久化已实现；UI不得先自行应用曲线、官职或婚姻加成。

R21官职来源接入：Government.ranks仍返回原80项、原ID/顺序/文武分组；既有merit/troops/salary/requiredTitle语义兼容，新nativeId/abilityStat/abilityBonus只描述核实的原来源身份和能力加成。正常任命、出征统兵上限及月俸从core打包且SHA锁定的pc-officer-ranks.tsv读取；原81号表项中native80无官职不作为可任命项。SaveCodec33仍存原字符串ID，输入/错误原因/API/事件/revision/RNG边界均未变。UI不得按nativeId猜项目人物ID，不自行叠加能力；当前人物能力持久化尚未接入。requiredTitle仍为旧工程规则，无官职薪俸也未改，不能据导入薪俸5断言正常月结已对齐。app/API/runtime不改，UI17仍仅在完成后按13ff5b9增量顺序集成。

R20开局诊断：AJ新包带SIGQUIT采样仍在原opening:245超时；有效栈显示主线程经NativeGameHost.install/GameSession.replace/WorldCopies.copy/SaveCodec.decode到RulesSave.validate，在String.matches/Pattern.compile中，测试线程等待主线程空闲。原始栈out/parity/opening-diagnostic-20261003/trace/trace_05；采样可能影响时序，不能当正常性能测试或单独证明全部超时根因。A仅在core将该固定特技ID表达式预编译复用，保存格式33、错误原因、接收字符范围、API和事件完全不变。app/API/runtime仍与AJ一致，未修改B负责的文件。后续完整冻结与实装结果以ROUND_20为准。

R19能力衔接仅新增内部PcOfficerAbilityRules算术与原程序oracle，API/DTO/SaveCodec33均不变，尚未用于正常命令；不要在UI直接调用该类或把其测试数据当人物来源。后续须保存基础能力、成长/经验和原官职引用，再由core统一产生当前能力及交易提交后的效果。官职不等于项目爵位；原700..799特殊槽位也不等于项目ID。现有Relations永久内助+1与Contests每级武力减10仍为未对齐项，不从现有存档擅自反推基础值。UI17只取其完成增量，基点仍13ff5b9。

R19后续接入约束：SaveCodec当前直接保存五项值，且Campaign学习、AbilityResearch、Editor、CustomOfficers、Relations及Contests舌战都写这些字段；不能仅在交易读取处套成长公式。旧档必须保留原五项值和既有学习/仲介成果，不能根据现有配偶反扣历史内助；原来源的新局状态与旧档/自定义兼容模式须显式区分。统一当前能力的命令输入、预览和提交在core处理，UI继续只读权威DTO；能力刷新本身不能掷随机数、推进回合或生成独立提交。原官职映射、成长设置含义、生日/年份入口及其他命令的经验奖励尚须逐项闭合。此段记录接入边界，不宣称上述状态迁移已实现。

R18集成已完成：AG3989文件297287877B全工作区先冻结核验，随后仅合入UI16完成74072e9→13ff5b9的19文件。638份core/API/runtime/data/tools源与AG逐SHA一致，未拷入UI的AA核心。下一UI增量基点13ff5b9。AH完整4000文件300634058B已核验，可供整组三模块继承；核心仍AD语义。整合包9f362283…2085已构建并在5554独立实装，交易97/港口43/出征65/运输70/开局存取62共337通过，另实际APK生产类ART功绩421/算术11595通过，每次7原文件全等。未沿用UI275；运输到达/返程、关卡商人、3D首帧及ARM真机不在此次通过范围。详见ROUND_18和round18-installed.json。

R16新增商人功绩：正常Campaign.trade和TradePreview.effects同源改为50、上限60000。旧档超过60000的功绩不降值、不再由商人增加；这是保护旧档的明确兼容例外。输入、DTO结构、事件/revision、SaveCodec33不变，其余命令功绩未改。AA仍是UI16正在使用的完整冻结核心；R16完整AD冻结后才可整组继承。价格/数量已有3865组原函数结果与Java对照，但尚未接正常交易，不据此调整UI数量范围。新包已构建，安装验证待完成。下一UI增量基点仍74072e9。

R16现已完整冻结AD：out/parity/checkpoint-20261003-ad/source，3981文件296129929B，manifest/verified全通过，可整组三模块继承；本侧不覆盖UI16当前AA。新包46cc245b…d32f3实装新开局/读写UI62项通过，另以实际APK生产类在ART执行功绩421和纯算术11595项通过，两种证据独立。原7文件恢复/保持全等。后续AE资料冻结不改变AD生产模块。

R17取证提示，尚未变更接口：原商人政治经验+5发生在真正报价之前，160组原提交测试中32组跨成长阈值改变实际金变化。未来typed预览必须预测完整命令的真实扣款及能力变化，不能在UI自行乘pricePerThousand，也不能先提交再追加经验造成存档/报价不一致。当前仍AD生产规则，原价格/数量和经验尚未接入；等待基础能力/经验/当前值存档契约闭合，不把WIP字段交给UI。

R15集成：已先冻结完整AA（3951文件290582548B，manifest/verified全部通过），再从9e30e45顺序合入UI15完成提交74072e9b0e68fb7efd191782f19e0e1b5dfd69a8；25文件只含app/UI文档/progress，前镜像全等且逐文件合入，未覆盖core/API/runtime。下一UI增量基点为74072e9。AA可供整组三模块继承，新增TRADE_SITE语义见下一段；生产/交易UI均已消费权威DTO。UI15旧V的trade测试仍断言同旬买后可卖及累计额度，候选中应是TRADE_USED且禁用；UI后续需用不同城或真实下一旬完成第二笔，并补港口/关隘TRADE_SITE流程，不能改回核心去满足旧断言。其制造测试可按候选独立重跑。本侧AB独立实装制造138、新开局/存取62合计200通过，两次原7文件全恢复；未沿用UI213通过。

R15冻结语义：已核实原商人入口仅城市，新增`TRADE_SITE`/field=`city`拒绝关隘、港口；quote的quotaRemaining/availableMaximum为0，输入数量仍原样保留，effects为空。沿用原quantity/ownership/actor/AP校验次序，不产生事件/revision/RNG变化。旧档关港库存和历史成交量仍可无损读取、推进；不改SaveCodec格式。军团及势力AI跳过关港商人采购。此增量已冻结AA，UI16已整组继承；合入UI15后的AB包已完成5554独立实装200检查，当前无运行中5554测试。不要根据UI文字推导许可。

R14可继承的完整冻结源：`out/parity/checkpoint-20261003-y/source`，manifest/verified.json在其父目录；3944文件290552774字节，全部大小/SHA与冻结时工作区全等，含168固定资源和4原生输入。UI可以整组三模块继承Y，不能只替换一个jar。接口语义见下一段；UI增量基点仍9e30e45。Y主APK83578d19c4bba0503f41d359e9bb8e0eb50e3dd270cfc4ee535b94e67d34817f，新开局/存取62和正常运输/两旬52共114项实际通过，两次7用户文件全恢复。UI15新交易流程尚待整合验证。后续资料快照Z仅增加证据/工具/文档，生产模块与Y保持一致。

2026-10-03 R14冻结接口（构建/安装范围见上，不沿用W验收）：原EXE的商人置位、拒绝和逐旬重置调用链已核实。TradePlan/TradePreview现在将同城买卖共用一次机会；`tradedBefore`保留旧存档成交量，任何正值表示本旬已用，`quotaRemaining`为0，否则为maximum。`TRADE_USED`/field=`city`给出本旬已交易原因；quantity/actor/AP等前置校验仍按既有顺序。失败无effects、无事件、不改revision/存档/RNG；成功仍仅一个TRADE_COMMITTED。正常完整回合清除状态。DTO结构未变，但UI15基于V的累计额度假设已过期，集成时必须按新权威语义验证；A不修改B负责的app。价格、1000步进和20000单次数量上限仍是未核实模型，不能称为PC规则。

2026-10-03最新增量：U已实际安装，正常190建设/两旬/征兵/训练61项和新开局/存取62项通过，用户7文件恢复全等。U主SHA37d46e7c…9ab1af，完整源checkpoint-20261003-u。后续完整V（checkpoint-20261003-v，3887文件）已冻结城市金容量100000和旧存档超额余额兼容，容量137项通过；API结构不变。已从8e1d5f7顺序集成UI14完成提交9e30e455f9b6a53c86bc9b51b213a21001494c32，下一UI增量基点为9e30e45。集成包已构建，实装待验证；不要把UI自己的Q通过当作本侧新包通过。

UI13待接入差异：ArmyUi的基础生产/器械舰船确认与CampaignUi商人确认仍硬编码行动力10，U核心实际20。请使用已冻结ProductionPreview/TradePreview权威费用和失败原因；只改文字不能完成接口接入。A不修改这些app文件，待B完成提交后顺序集成。U新DTO尚没有对应已安装UI消费证据。

W最新实装：完整snapshot-w含UI14与V容量规则，主APK e2abcc797d76bb69c1fd3a28d2ced7776336c30c7fe377fe7c576a3b64ae87d9、test dee60fe08c39ba1a4cad798a450e5c4233e990e116fdb6bd09ae48c62a98a8b0。本侧5554运输往返52、新开局/存取62、完整外交链189项独立通过，各批7文件恢复一致。1369份当前模块/构建文件仍与W全等。UI15可继续使用V整组三模块，W没有额外API结构变化；其WIP未集成，生产/交易UI接入仍以完成提交为准。

W最终又完成运输表单69和外交表单65，同包5套合计437通过；每套7原文件恢复，无新增。ARM真机未验。源接口继续保持V/W一致，下一UI集成基点9e30e45。

- `GameApi.execute(GameCommand)` 的正式 typed 输入目前仅 RECRUIT、PATROL；单挑/舌战用既有 `ContestCommand`。命令携带实体 ID 与 `StateToken(sessionId,generation,revision)`，在 session 的串行逻辑线程执行。
- 其他正常玩法沿用 `LegacyCommandSink` 的延迟命令事务，必须在 GameSession 的候选 World 上计算，成功后原子提交。不能先执行规则再传入 Result，也不能在 UI detached World 上改变真实局面。
- 唯一权威状态为 GameSession 内的 World；`GameSnapshot`、legacy view 和 TurnJournal 用于读取/表现。输入事件、选择、镜头、网格、动画进度不是游戏权威状态。
- 每次提交产生新 revision；新开局/加载使旧 token 失效。UI 收到失败应刷新真实状态，不能本地补扣资源、行动力、兵力或重新掷随机数。

## 输出与失败

`CommandResult` 包含 error、detail、当前 state 和成功时的 event。错误枚举沿用 NONE、STALE_SESSION、STALE_REVISION、HOST_BUSY、RULE_REJECTED、HOST_ERROR、CLOSED。detail 是人读原因，不是 UI 分支协议。校验失败不提交局面、不消费权威 RNG；测试必须比较完整存档字节，而不只比较显示数值。

`GameEvent` 是已提交事实，监听器不可重入提交。回合中间演示不是可保存的新局面；仅完整回合提交后保存权威状态。预览不得重新执行已生效命令。

## 视觉事件

- `CriticalHit.year` 是正常战法实际命中时捕获的历年；与 officerId、name、tactic、能力/性别快照一起不可变。它不写入 SaveCodec，也不改变随机数或战斗结算。
- 六位核实武将的年龄头像由 UI 已有 PcPresentationPlan 消费该事实。原演员年龄为 year−birth+1；具体出生/阈值及 native ID 见 `docs/pc-visual/scenario-portraits-source-working.json`。自定义同名武将不能据名字借用 canonical 绑定。
- `PlotOutcome` 记录真实命中、暴击、反射、连锁结果；UI 不得重复概率判定。暂停、倍率、跳过、退出仅改变播放，不能改变命令结果。
- 当前演出默认 750ms，用户接受 500–1000ms；这不是 PC 实测时序。新年龄改动必须重新构建/安装，不继承旧 APK 的 GPU 验收。

## 集成顺序

1. B 从完整基线独立开发，列出相对基线的文件与哈希、构建身份、验证结果及新增接口需求。
2. A 先冻结一次可核对的 core/content 快照，再逐项应用 B 的增量；遇到共同修改逐段合并，禁止目录覆盖。
3. 合并后跑架构/规则/内容检查，构建 app 与 test APK；备份设备存档后覆盖安装，验证新开局、典型命令、多旬、保存读取、生命周期及恢复存档哈希。
4. 最终文档区分主机、x86_64 模拟器、ARM 真机证据。未实测项保持待验证。

本轮尚未主动改动 app 文件；其交接时已有年龄绑定与 instrumentation 作为继承内容接受验证。

2026-10-03：A封存pre-ui-test-integration-20261003完整3535文件后，逐字比对并顺序接入B已完成的PcPresentationsInstrumentation三行selector参数修复。该文件与B一致，证据out/parity/pre-ui-test-integration-20261003/integration.json；没有接入B仍在验证的其他app改动。app/test APK构建通过，年龄专项另行实际安装验证。

## 2026-10-03 出征接口 v1（核心/宿主已实现，等待 UI 消费）

入口：`GameApi.preview(DeploymentCommand)` / `execute(DeploymentCommand)`。参数为同一个不可变对象：`expected: StateToken`、`cityId`、`leaderId`、有序0–2个`deputies`、`weapon`/`ship`枚举名称字符串、`troops`、`food`、`gold`。数组在构造与读取时复制；未知枚举交给核心规则拒绝，不能由 UI 改成默认兵装。所有调用在 GameSession 串行逻辑线程。

预览先检查 CLOSED / STALE_SESSION / STALE_REVISION / HOST_BUSY，再在隔离副本调用纯校验，不执行命令、不回滚，也不清 reports。核心 `Army.previewDeployment` 与普通 `Army.deploy` 共用 `prepareDeployment`；原校验顺序和中文原因保留。正式 execute 用当前 authority 重新校验、调用真实 deploy，完整候选复制成功后才提交一次 revision。不要提交此前预览生成的 Unit，也不要在 UI 先扣资源。

输出 `DeploymentPreview`：

- `state/error/reasonCode/field/detail`；`allowed()` 仅 error=NONE 时真。host错误的limits/resources/unit为空；规则拒绝仍有范围和请求成本，但unit为空。detail只显示，不能解析文字作为协议。
- `limits`：统率上限、兵力最少1000、兵力可调上限（取统率、库存、兵装及可携粮的交集）、携粮下限等于本次请求兵力、携粮/携金上限。max<min是空区间；范围不意味着副将/高级舰船/出口也已通过。兵力上限使用可用粮库存，不因当前food输入偏小而禁止用户先调兵力再调粮。
- `stock/reserve/cost/remaining`：兵、粮、金、所选兵装和舰船。remaining按请求量计算，非法请求可以为负，属于假设值。剑兵/走舸无需库存，stock/cost为0；攻城器械扣1件，普通兵装按兵数。资源DTO用long，避免非法极值输入在展示扣减时溢出。
- `reserveEnforced`单独表示当前军团执行上下文是否实际适用留存；非零reserve不能自行作为玩家出征额外限制。`actionPointsAvailable/actionPointsCost`来自核心；使者内部处理沿用既有免AP路径，UI不能硬编码10。
- `unit`仅allowed时存在：原出城格轴坐标、movementBudget/spent/remaining、适性、统武智、基础/地形修正攻防、每旬粮耗/可供完整旬数。粮耗假定当前兵力、位置和补给范围不变；foodUse=0时foodTurns=Integer.MAX_VALUE。以上是**现有引擎行为**，PC尚未证实的公式继续列入差异表。

稳定规则原因（按真实校验顺序）：COMMANDS_BLOCKED、GAME_OVER、CITY_UNAVAILABLE、ENVOY_CHANGED、CITY_DELEGATED、LEADER_UNAVAILABLE、ACTION_POINTS、CITY_GOLD、DISTRICT_RESERVE、GOLD_RANGE、FORMATION_INVALID、LOAD_RANGE、DEPUTY_UNAVAILABLE、STOCK_INSUFFICIENT、UNIT_ID_LIMIT、EXIT_UNAVAILABLE。field为city/leader/deputies/weapon/ship/troops/food/gold/target/global。一次仅返回最高优先级失败。`CommandResult`新增向后兼容的reasonCode/field；部署拒绝与预览相同。核心环境阻塞在GameSession表现为HOST_BUSY，直接core仍为COMMANDS_BLOCKED。

成功事件 `GameEvent.Kind.DEPLOYED` 为一次已提交事实，cityId/leader officerId、troopsDelta=-出征兵数、orderDelta=0；UI刷新snapshot定位officer.unitId，不猜nextUnitId。事件没有镜头、路径动画或演出时长，动画跳过/退出不能再次提交。既有`Army.deploymentPreview`仍是兼容的出城探针，不具有完整命令合法性，新的表单应使用GameApi接口。

主机验证：DeploymentPlanTest覆盖全部27兵装/船组合、18类失败和真实成本/出口/编队/后勤/读写；DeploymentSessionTest覆盖相同27组合、数组隔离、完整正常命令存档/RNG逐字节一致、重复/陈旧/忙/关闭/跨线程拒绝。另用同一DeploymentParityProbe在重构前完整工作区classes与当前classes分别跑39项，核对成功/错误文字和完整存档SHA；不会用旧HEAD替代脏树基线。构建与实际UI接入/安装验证分开记录。

## 2026-10-03 出征接口 v2：版本缓存与完整部队详情

针对UI实测每次87–239ms的全局面复制：GameSession在同一StateToken下只保留一份**私有、不可逃逸**的deploymentQueryWorld。输入改变时重新执行全部核心校验及计算，但不再次全量SaveCodec复制。该副本不会进入真实命令执行、legacyView或外部调用者；preview不返回World/Unit引用。所有成功typed/contest/legacy/turn提交、replace和close通过invalidateQueries立即释放副本，失败不改变revision，beginTurn仍先返回HOST_BUSY，cancelTurn可继续用相同状态缓存。线程约束保持同步串行，没有引入异步回调或取消语义。首个查询每revision仍需一次复制；不能把后续缓存速度声称为首次查询速度。

新增UnitFacts.energy、attackRange、equipmentLabel、只读skills和tactics列表。skills包含去重后的{id,label,description}；tactics含group(INFANTRY/EQUIPMENT)、code、label、description、energy、rank、minRange/maxRange和formationError。formationError来自War/Army真实命令共用的目标无关校验；非空时不可用，空值只表示编队/气力/当前位置检查通过，仍须正式命令检查目标、距离、敌我关系、回合行动等。不要仅按rank推导可用性（攻城器械的适性校验与水军不同）。原构造器兼容保留，新runtime总是填充详情，UI无需再调用旧Army.deploymentPreview补足数据。

新验证DeploymentPlan570、DeploymentSession412、BattleFeedback209、PlotJournal150通过；缓存后提交的新查询验证已行动主将拒绝及更新后库存。主机40次变数量查询与每次独立完整复制路径逐项一致，权威完整存档/RNG/revision不变；首次118.702ms，缓存中位0.515ms/p95 0.967ms/max1.857ms，对照完整复制中位58.239ms/p95 82.301ms。仅主机数据，Android实机/模拟器性能仍由UI实测；日志out/parity/deployment-20261003/cache-benchmark.log。契约v2冻结快照checkpoint-20261003-d，仍由UI自行读取/最终由A顺序集成app增量，未向其他chat发消息。

## 2026-10-03 战法成本接入（UI 待消费）

原共享资源和原执行扣减链已验证，Army 的 RAM/FLAME/陆上 STONE 改为 10 气力，水上 STONE 为 15；FIRE_ARROW 保持 10。`Army.tacticCost(Unit,Tactic)` 是当前位置的权威消耗，`Tactic.energy` 只保留陆上基础值以兼容已有源码，不能用于水上投石展示。正式命令的校验、扣减、预览说明及 AI 评分共用此方法；出征 v2 的 `TacticFact.energy` 同步返回上下文消耗。

待 UI 完成批次时集成：ArmyUi 战法列表应读取 `w.army.tacticCost(u,t)`；DeployWizard 应消费已有 v2 不可变 tactics 详情（至少不能继续用 Army.Tactic.energy 展示海上投石）。玩法会话没有直接改这两个 app 文件，避免覆盖活跃 UI 工作。未接入前将其列为明确显示差异，不能冒称合并包已全部对齐。证据与转换命令见 docs/pc-data/README.md；本变更不改变存档版本或枚举顺序。

### 最终集成回归待办（5554，保留所有原断言）

3330ffb+d24合并包 opening 设置再次打开未出现弹窗（截图仍菜单）；源码成本包9b1a+3330ffb deploy 在键盘展开后空搜索结果不可见：1080×1920 / density420，搜索框下方马上是校验摘要和按钮，列表没有可见高度。截图 `out/parity/scenario-tail-20261003/installed-deploy/evidence/FAIL.png`。测试未修改，193/982/513主机检查仍通过，但上述两份APK的完整触控流程均FAIL。原auto与所有用户文件已恢复；不是沿用UI独立5580的成功来覆盖此失败。

UI79cdc6b已按冻结3330ffb→79cdc6b顺序集成，仅12个app/docs/progress文件；新外交WIP未触碰。新请求外交输入state/city/officer/targetSide/type/turns、允许/原因/实耗/余量/期限/旅途/预测边界已登记，待核心纯校验与typed执行同源实现，尚未承诺可调用接口。

79cdc6b合并APK71cbc760…的march现已结案：240.17秒FAIL@UiUxInstrumentation168「real deployment selects a field unit」。真实触摸步骤已到兵种钱粮/枪兵/确认，但FAIL截图仍是未选择兵种的表单，确认按钮不可用，尚未产生部队，因此未验证行军或回合。证据 `out/parity/ui-integration-79cdc6b/installed-march/evidence/FAIL.png`；全部7原文件字节恢复，无新增。需要检查控件点击后状态是否实际稳定，不能以文本控件存在代替选择成功，也不能删掉真实部队断言。

## 2026-10-03 外交接口 v1（核心与宿主已实现，UI 待消费）

`GameApi.preview(DiplomacyCommand)` / `execute(DiplomacyCommand)`；不可变输入含 expected StateToken、cityId、officerId、targetSide、operation、turns。operation 使用精确名称 GOODWILL / CEASEFIRE / ALLIANCE / BREAK_TREATY，未知或大小写不匹配返回 OPERATION_INVALID。turns 只对两种协定生效，合法期限由 preview.treatyDurations 返回，不在 UI 复制 3/6/12 校验。其他两种操作忽略 turns，保持原命令行为。

核心 Campaign 的纯失败检查与普通 goodwill/negotiate/breakTreaty 共用；Envoys.dispatchFailure 与真正出使也共用。GameSession 仍先校验token/宿主忙状态，在隔离副本上正式重验并执行真实命令，成功才一次提交。出征的私有版本缓存扩展为 ruleQueryWorld，两种查询共享同 StateToken 的单次复制；副本不进入正式命令，任何成功命令/回合/替换/关闭均失效。不要在 UI detached World 扣费或调用真实外交命令来预览。

DiplomacyPreview：

- state/error/reasonCode/field/detail 与出征契约相同；host拒绝 resources/forecast 为空。规则拒绝仍提供 resources 和 treatyDurations，forecast 为空。CITY/LEADER 等通用原因沿用现有 code，其中 field=leader 对应 officerId；外交新增 TARGET_SIDE_INVALID、RELATION_MAX、TREATY_TERMS_INVALID、TREATY_EXISTS、RELATION_TOO_LOW、TREATY_MISSING、DESTINATION_UNAVAILABLE、ENVOY_PENDING、TRAVEL_TOO_LONG、OPERATION_INVALID。
- resources 为出发时实际 goldAvailable/goldCost/goldRemaining、actionPointsAvailable/actionPointsCost/actionPointsRemaining；remaining 使用 long，拒绝时可为负。亲善500金、协定1000金、解约0金来自核心，均沿用普通出发10AP；UI 不硬编码。抵达不重复付费。这里只声明当前引擎，尚不是 PC 成本核实结论。
- forecast.delayed 表示先出使再结算；destinationId/name、oneWayTurns/roundTripTurns 对应真实 Mission。解除协定无旅途，destinationId=-1、name空、回合数0。treatyTurns 是成功抵达后起算的协定期限，不是从出发开始；GOODWILL/BREAK 为0。
- initialAcceptancePercent 是**当前局势下第一轮接受判定概率**；-1表示该操作没有随机接受判定，不能显示为0%或承诺必达。debateOnRejection 表示当前特技/玩家状态可能在拒绝后触发舌战，不能把第一轮概率当最终成功率。currentRelation、relationDeltaOnSuccess 是当前边界内预计变化；otherRelationsDelta 是解约对其他存活势力的名义变化，各关系另受下限约束。
- 旅途中据点易主、使者变化、关系或协定变化会在抵达时重验；不将出发预测写成实际交涉结果。概率、亲善增量与运输寻路尚未做完整 PC 对照。现有 Envoys 把不可达路线的 -1 经 max(1,…) 变成1旬，是已登记的规则缺陷，本次抽取接口保持原行为，不能声称路线已核实。

成功事件 DIPLOMACY_DISPATCHED 只表示真实使者出发及费用提交，绝不表示停战/同盟已经成立；TREATY_BROKEN 表示即时解约完成。cityId/officerId 标识提交来源，troopsDelta/orderDelta 均0。到达、拒绝、舌战、返程仍由正常回合与已有战报事实驱动；视觉结束、取消、跳过不重新执行规则。

验证与可复现基线的最新路径在 progress.md，本次未修改 app 源文件或另一个会话的 WIP。UI 应读取最终冻结完整快照后再消费接口，由集成负责人按已完成提交顺序接入。

### UI67057ff顺序集成结果

已接入79cdc6b→9d45db9→67057ff的30个完成文件，原/当前/新SHA预检均通过，progress只追加；未接活跃战斗WIP。ArmyUi已调用上下文tacticCost，ArmyUi/WarUi共用formationError，DeployWizard已消费完整UnitFacts，因此上述战法费用显示待办在源码层面关闭，仍需合并包真实战斗验证。外交界面尚未消费本节新DTO，仍等待UI后续完成提交。

02497975…APK和12651982…测试包83任务25秒构建成功，165+468+412专项通过。首次5554出征实装124.85秒FAIL于定位成都之后等待“出征”；截图仍全国地图、底部“成都·指令”，尚未进入表单，不能据此认定键盘修复失败或成功。保留全部原断言。`out/parity/ui-integration-67057ff/installed-deploy`含设备读回SHA、FAIL图、日志和7原文件恢复。SurfaceFlinger日志持续“Faking VSYNC due to driver stall”，但原因尚不能唯一归因于驱动。已仅重启5554，同配置/host GPU/分辨率且不清数据，重启后全部7文件字节一致，正在复测同包；未操作5580。

年龄退出待办仍未完成：71cbc760上诸葛亮年长整例在131项源图层/权威检查后继续FAIL Activity lifecycle finished before user save restoration；外部force-stop恢复通过不能代替应用自身的退出屏障。详见ui-integration-79cdc6b/age-zhugeliang-old。

## 建设接口 v1 与基础建设规则（2026-10-03）

原菜单600e15/8ba670的20选项只含基础设施，原5bc462扣+c4费用已执行验证。普通新建设现在均为Lv1；市场/农场200金，兵舍/锻冶/厩舍/工房/造船300，造币/谷仓400，黑市50，铜雀台1500。`Domestic.Kind.cost`来自生成的PcFacilityCosts；枚举顺序、存档版本33不变。旧档已有Lv2/Lv3及正在建设的Lv3保留并按原剩余工期继续，读档不改等级、不补扣/退款。

输入：`ConstructionCommand(expected,cityId,officerId,kind,q,r)`，kind为Domestic.Kind枚举精确名称，q/r是轴坐标。`GameApi.preview/execute`沿用同StateToken、同逻辑线程与私有ruleQueryWorld缓存。新核心`Domestic.previewBuild`与普通build共用纯失败检查；正式execute重新检查当前候选、实际执行普通build、提交一次revision。只覆盖新建设，不把取消、拆除、合并默认为同一个操作。

输出ConstructionPreview：state/error/reasonCode/field/detail；resources含可用金、实际金耗、假设余金、可用AP、实际AP耗与余AP，余量为long；宿主拒绝resources为空，规则拒绝仍有resources。completion仅允许时存在，含turns、level、initialDurability、maximumDurability、label、effect。turns是当前实际剩余完整旬数，effect是当前引擎完工后设施自身贡献的说明，不承诺城市总增益、后续伤害或PC公式已核实。建设现扣20AP（原5bc4b7立即数及真实军团扣除链已验证）；现有单执行者2/3旬、1000耐久与产出仍待原程序对齐，原按军团AP与当前按势力AP的范围差异仍待处理；本接口不允许UI重写这些公式。规则未来修订由同一个预览与命令共同变化。

失败原因：沿用CITY_UNAVAILABLE/LEADER_UNAVAILABLE/ACTION_POINTS/CITY_GOLD等通用code；新增FACILITY_KIND_INVALID(kind)、CITY_TYPE(city)、FACILITY_CAPACITY(city)、BRONZE_REQUIREMENT(kind)、WATER_REQUIRED(target)、BUILD_SITE_INVALID(target)、FACILITY_ID_LIMIT(global)。顺序保持普通build原优先级，不按中文detail分支。成功事件CONSTRUCTION_STARTED只表示扣费与施工开始，cityId/officerId为本次执行者，troopsDelta/orderDelta=0；完成由真实旬结算驱动，不由视觉回调追加一次建设。

UI待接：BuildPicker移除继承的politics>=80工期推断和固定AP10，使用DTO并正式typed提交；新建设等级/金费已经通过现有buildLevel/Kind.cost更新。DomesticUi设施详情仍有“新建设施直接最高级”文字，必须删除并恢复现有合并操作入口；已有设施说明使用新`Domestic.facilityEffect(f)`，不能拿新建设buildEffect描述旧档Lv3。该方法保留现有产出计算的等级差异，尚不宣称产出PC对齐。新的建设UI测试应按经原数据核实的Lv1与实际费用断言，不能为了通过继续期待旧满级捷径。玩法会话没有修改这些活跃UI文件。

5554重启后的同02497975包回归已结束：127.11秒FAIL于选择第二位武将时`full roster target not visible: deploy.role.1002`。本轮搜索空态/键盘完整可见已经通过，不能将旧键盘失败沿用；FAIL截图名单已滑到底部，尚未出征。完整7用户文件再次恢复全等，无新增。UI后续需检查通用测试按排序连续找行的滚动方向/重定位，保留真实可见与正式出征断言。证据ui-integration-67057ff/installed-deploy-cold。

2026-10-03 建设AP补充：完整快照K将包含faf390d UI和建设20AP；J仍是10AP历史版本。UI应继承K并使用ConstructionPreview.resources.actionPointsCost，不能保留固定10。仅普通新建设改变为20，其他城市命令仍沿用各自原实现，未把它们一并改成20。原AP测试5项通过，包含原5bc4b5调用→5b9340→4a1820→47e3e0，6种AP输入全3MiB对比只修改district+2c。

## 运输接口 v1（2026-10-03，K之后的新接口）

`TransportCommand(expected,sourceCityId,targetCityId,officerId,deputies,gold,food,troops,equipment,sea,returnOfficers,ships)`用于新运输派遣。所有输入数组构造时复制，访问器返回新复制；deputies为0–2人，不能null或重复。equipment为现有Weapon顺序：SPEAR/HALBERD/CROSSBOW/CAVALRY/SWORD/RAM/SIEGE_TOWER/WOODEN_BEAST/CATAPULT，兼容旧4项；ships必须2项，顺序TOWER_SHIP/WARSHIP。sea是现有允许航路模式；ships是携带货物，不等于运输队正在乘坐的船。输入不静默修正、不夹取到库存。

`GameApi.preview/execute(TransportCommand)`沿用统一StateToken、逻辑线程和ruleQueryWorld缓存。preview不调用正式命令，不申请任务ID或消费RNG。execute复制当前权威状态，重新检查并调用普通Domestic.transport，只提交一个revision，发TRANSPORT_DISPATCHED，cityId=来源，officerId=主将，troopsDelta=-本次兵力，orderDelta=0；该事件仅表示派出，不表示抵达、入库或返程完成。

输出`TransportPreview`含state/error/reasonCode/field/detail，resources和forecast。宿主拒绝两者为空；规则拒绝仍给resources，forecast为空。resources含真实可用AP、实际AP成本，以及14个按kind识别的不可变Stock：金/粮/兵、9兵装、2船货。sourceAvailable为实际库存，dispatchLimit为硬上限、库存、当前执行中军团留存共同限制，destinationFree为当前余容量。不要根据列表位置推算种类，也不能将dispatchLimit当成整个命令已获准。

forecast仅可执行时存在：turns、foodPerTurn、projectedFoodUse/Arrival(long)、departureQ/R、departureCost、movementRemaining、shortage、capacityFits、returnOfficers。食粮预测按当前路线与当前旬耗估计；未来拦截、营地光环、阻塞、兵损会改变它。capacityFits按现在携带货物与目标库存保守检查，不保证未来入库；容量不足仍可派遣，到达等待，不丢货。turns未知为-1，预计粮值也为-1；不能在UI补用近似公式。原PC运输参数未完整核实，本接口只给当前引擎真实结果。

错误保留普通命令原顺序：先SHIP_CARGO(ships)，再通用city/leader/AP，随后DISTRICT_DISPATCH(target)、TRANSPORT_TARGET(target)、CARGO_RANGE(cargo)、CREW_SIZE/CREW_DUPLICATE(deputies)、副将通用code但field=deputies、CARGO_EMPTY/CARGO_STOCK/DISTRICT_RESERVE(cargo)、EQUIPMENT_STOCK(equipment)、DEPARTURE_BLOCKED/ROUTE_UNAVAILABLE(route)、MISSION_ID_LIMIT(global)。不能解析中文detail。纯调将、补给、改道、停止、卸货和返程不是该命令覆盖范围。

同步修复运输舰船提交顺序：旧实现先success记录战报，再扣舰船，导致直接下一旬和读档后下一旬战报字节不同。现舰船货物/库存与其他运输资源在一次success之前完成。旧存档历史战报不改写，既有运输不重扣。269核心、88宿主检查已通过；使用正常命令和6完整旬、完整save/RNG比对，未删断言。完成新快照L后UI才能继承，K不含运输v1。

集成5554的K包f6e91e2…已实装回读一致。combat失败于UiUxInstrumentation83：攻击后点5,5，等待可见控件超时；截图仍攻击模式，无目标错误面板，5项检查通过但没有执行攻击。所有7原文件恢复全等，无新增，auto82554269…f0a9；原图`out/parity/ui-integration-faf390d/installed-combat-ready/evidence/FAIL.png`。实际包为game.sanguo.mobile.dev，外部证据应取该目录。naval同包正在独立运行，不能把UI5580的66/18项通过当作5554或新核心包通过。

最新接口继承基线已就绪：`out/parity/checkpoint-20261003-l/source`，manifest为3713文件259893327字节，包含core/API/runtime、全部继承资源、4个native构建输入，建设20AP/运输v1/舰船原子报告修复。UI可只复制这三个模块至自己out的独立构建输入并逐文件验证manifest，不能回写root。L之后新增PcTransportFlowProbe（未改变接口）在9真实项目剧本通过217检查：原库存包含一艘楼船，实际派遣6完整旬逐旬重开Session，全部save/RNG一致；短路线首预览2.9–43.4ms不代表全国所有路线性能。

K naval已结束83.04秒FAIL；L包ed967310实装回读一致，独立诊断naval121.14秒同样FAIL于目标预览尚未建立，显示15耗气与横屏列表检查通过。启用已有MapTap57 DEBUG后未收到该回调；这是输入链路线索，不足以单独确定根因。运行结束原7文件全等、无新增，日志属性从空→DEBUG→空恢复。证据transport-20261003/installed-naval-traced及map-tap-trace.txt。下一次地图诊断应核实活动renderer/触点接收/gesture状态，不能削弱实际preview断言。

已审查并顺序合入UI第11批4c23785（base faf390d）：34文件，CargoWizard仅抽取既有UI职责；架构白名单仅追加这个精确路径，MODULE_RULES仅追加评审段落。未覆盖本侧文档/规则/工作区；集成工具新增显式--include-architecture-review，并要求JSON只增不减、确切Java路径、原条目全保留。新批仍使用兼容文本运输预览，下一批应消费L的结构化接口。真实5554新合并transport待验证，不能引用UI独立包64/43/51项替代。

## 六类城市命令接口 v1（2026-10-03，N之后）

`CityActionCommand(expected,operation,cityId,officerId,targets)`和`GameApi.preview/execute`：operation精确为PATROL/TRAIN/RECRUIT/SEARCH/REWARD/APPOINT_GOVERNOR。前四项targets必须空数组，REWARD为一批非空唯一目标，APPOINT_GOVERNOR恰好一个目标。数组构造和读取均复制，不接受null，不悄悄筛除错误目标。保留旧普通Strategy命令及GameCommand兼容入口；新接口正式执行同一普通命令，不另写模拟规则。

`CityActionPreview`沿用state/error/reasonCode/field/detail。宿主拒绝时resources/effects/search均null；规则拒绝仍有资源、没有效果。resources含实际可用金/AP、实际消耗和long余量；搜索的goldRemaining是支付后的保底余额，之后可能搜得金，不能将它标成随机结果。effects含治安、气力、兵力、兵源、太守ID的before/after，征兵的兵舍remainingUses前后（其他指令为-1），busyTurns=0表示本批均即时完成；武将列表含执行者、目标、被替换太守的忠诚、功绩、acted、职分、lastRewardTurn、remainingTurns前后。列表不可变，不能据此额外变更World。

SEARCH只返回条件检查几率officerCheckChance/goldCheckChance、金收益范围0至当前容量下最多120，以及可能结果种类OFFICER/TREASURE/GOLD/NOTHING；这是分支检查几率，不是最终无条件概率。不得透露隐藏武将身份，也不能显示成保证发现。preview不执行搜索、不消费RNG、不产生战报或revision。普通命令才按武将→宝物→金→无发现的实际分支执行。当前金/AP、巡察/训练/征兵/褒奖公式尚未完成PC原程序校准，这次不把工程公式称作原版规则；未来修订必须同时改变核心预览与命令。

错误新增CITY_ACTION_INVALID(operation)、CITY_ACTION_TARGETS(targets)、REWARD_SELECTION/REWARD_COUNT/REWARD_TARGET(targets)、ORDER_FULL/NO_TROOPS/MORALE_FULL/CITY_TYPE/ORDER_LOW/RECRUIT_RESERVE_EMPTY/TROOP_CAPACITY(city)、BARRACKS_UNAVAILABLE(facility)、GOVERNOR_TARGET/GOVERNOR_UNCHANGED(targets)，以及已有通用城市/执行者/AP/金不足错误。保留普通命令原检查优先级。UI按code/field绑定，不解析中文detail；不再推算公式或因只读列表存在便认定可执行。

execute要求当前StateToken、逻辑线程且无进行中回合，复制当前权威状态、重新验证、正式执行后一次提交，发一个CITY_ACTION_COMMITTED事件：cityId/officerId为本次城与执行者，troopsDelta/orderDelta取实际确定性效果；该事件只用于显示已发生的动作，不作为再次执行、随机搜索或回合推进入口。动画结束/取消不产生规则副作用。搜索结果以提交后的权威状态和实际战报为准。

接口继承快照O已冻结：`out/parity/checkpoint-20261003-o/source`、manifest同目录，3750文件264132218字节逐SHA全等，含完整继承成果、UI4c、建设20AP、运输v1、城市v1；新增城市六项AP仍10。UI可将core/game-api/game-runtime作为独立冻结输入继承，不能回写本侧文件。O之后正在修复经济委任旧1500金门槛，此修复不在O内，也不在正在年龄实装的O APK内。后续完成的新快照会单独说明。

### 城市原AP校准与AI修复（O之后，准备P）

原生验证已推进：`test_pc_city_action_costs.py`3项测试、36AP边界案例，真实执行巡察/征兵/训练调用及5b9340→4a1820→47e3e0，整3MiB状态对比仅改变军团AP。巡察/征兵20，训练基础20，原49dab0发现军事府41且+14非零时10；该字段更广语义不猜测。原命令身份由标签函数48f190、8af2c0表、各自原UI和执行函数共同调用相应效果计算函数交叉核对。`export_pc_city_action_costs.py`锁定EXE SHA并核对原指令，重复导出Java/audit字节相同。

因此P起PATROL/TRAIN/RECRUIT为20AP，SEARCH/REWARD/APPOINT_GOVERNOR仍沿用10。项目还没有军事府，当前训练使用基础20；缺失军事府及军团AP范围列为明确未完成项，不能说已经实现减免，也不能给UI加伪设施开关。其他数值/三执行者公式继续待对齐。UI请消费CityActionPreview实际cost；O仍为城市10AP，正在治理WIP不能把O或固定10的期望复制到P。

StrategicAi候选共享同一cityActionFailure/previewBuild，避免剩余10–19AP挑选不能执行的20AP动作；经济委任取消旧1500金门槛，使用同一建设允许性，200金20AP可以开工农场/市场，不能建设时仍尝试现有其他合法城务。政府旧测试加强为精确200金/20AP/Lv1，随后仍在原v7迁移处失败；没有删除迁移断言。新增委任288检查通过，旧完整核心确实失败于“应开工”检查，证明修复有效；夹具第一次在成功命令后手改资源导致战报基线差异，已把场景设置移到正常委任命令之前，保留初始失败日志。

目前核心城市1899、委任288、会话140（含旧GameCommand与新typed完整状态一致）、建设489、运输269+88、年份50专项通过，完整打包成功。O包06f6ba6a…年龄首例360秒TIMEOUT，余11未跑，外部恢复全部7原文件全等；不能称年龄验证通过。自用5554正换SwiftShader软件图形，原数据未清除、UI5580未触及；随后验证P候选f4cd4d34…，新结果单独记录。

### Q完整继承基线与UI12集成

Q已冻结：`out/parity/checkpoint-20261003-q/{source,manifest.json}`，3804文件273917146字节，含全部继承成果、UI12 a5f93be474ae9d18dfe5ba88df1818c6d5f19fff、四native输入、P的20AP、军团公平性修复、AI路线优化。UI可按manifest复制core/game-api/game-runtime到独立out，接口签名不变。下一UI集成base=a5f93be，仅合入完成提交。

Districts对同级低治安优先处理更低治安，原三旬公平断言674检查PASS；AI查询局部格网/等价比较器/纯水域复用在9剧本27完整旬全save/RNG保持一致。本机JVM计时不代表Android性能。

UI12仍有固定AP10文字和对应旧测试，下一批必须消费DTO的实际费用/效果；UI5580旧G（10AP/Lv3）通过记录不能替代Q验收。P包SwiftShader年龄首例仍360秒超时；独立lifecycleOnly亦240秒超时，均7原文件恢复全等、无新增。实际进程栈显示ready等待与scene-cpu水域材质构网，仅为定位线索。详细结果见`docs/validation/parity-20261003/ROUND_10.md`。

Q主包7801884b02e2a99de5e9e15814c48d43b12c00d2bb367fd33c2b4bfed9e8c958已实际安装且双APK回读SHA相同；opening360.02秒超时，存档保存/取消等子项通过但未完成新开局。7原用户文件恢复全等。host GPU同AVD无清数据重启后正在严格年龄批次，结果独立保存，不能继承到后续APK。

### UI13顺序集成与巡察效果更新

已按a5f93be→8e1d5f750390429cf8bb251805ca4456bf6ad6d1完成52文件安全集成，下一base=8e1d5f7，app文件均来自该完成提交。六类CityAction及普通Construction消费typed预览/执行，去除旧AP10、Lv3与政治工期自算；Transport/Diplomacy仅轻包装准备，不表示界面已消费。

本侧Q之后新增原巡察算术：单执行武将当前统率/28取整+2，现有权威敌对部队在城市1..3格环内时先减半，再夹至治安100。命令与CityActionPreview.effects.orderAfter同步变化；接口签名、StateToken、失败原因及CITY_ACTION_COMMITTED视觉边界不变。UI只显示DTO，不复制该公式。新PcPatrol1467、CityAction1899、Session140检查通过；三执行者、PC能力修正及外交数据完整一致性仍未完成。新源码尚不属于Q包，需另冻结基线和重建实装；Q的年龄通过不能替代新包流程验证。UI13自身P/5580通过也不能替代本侧集成验收。

R完整继承基线已冻结并核对：`out/parity/checkpoint-20261003-r/{source,manifest.json}`，3857文件282487260字节，逐SHA/大小与冻结时工作区全等。包含UI13、巡察新效果、原部队与武将归属工具/审计，三模块接口签名继续兼容。R双ABI主APK d7be448627c1a988ab0e81edaf916b75cd3bdb641c8f2cc309f95f4a38e25c7b（87012773B），test960270f9316bdbe31c5e4a4ce813e0a15dc8c19164b86196a8a345df0823b5bc（691951B），封存在ui-integration-8e1d5f7/apks。构建3分5秒成功，Territory674通过；Strategy同完整N进驻入口失败保留。当前5554尚在Q年龄批次，R尚未安装。

R安装结果更新：双APK已在5554实装回读一致；opening62检查113.65秒通过，governance64检查67.21秒通过，实际巡察95→99/金100/AP20与DTO一致。原7文件每批外部恢复全等。旧250地图64的governanceArmy11.25秒失败于当前视野无可见合法开发地，未发出建设；保留失败，不改断言。另一批采用UI13记录的正常190曹操存档02ddb3d4…d69作为临时输入（外部完整备份/结束恢复），正在真实建兵舍/多旬/征兵/训练/读取。与用户旧250存档、190新开局测试分开报告。

Q年龄12/12共1610检查已全部通过，逐例/final原文件全等；它是Q包证据，不写成R年龄全过。UI14来信仍WIP，当前app集成仍止于8e1d5f7；后续只取完成提交。R后的差异审计及验证工具改进没有更改三模块或app构建输入。

R后续实装：正常190曹操governanceArmy61检查112.86秒通过，真实兵舍→两旬→征兵→训练→读档，原7用户文件全恢复。pid16663两旬compute14.4/28.3秒，初次城市查询346/369ms，仍有性能差距。相同190输入transport225.81秒失败在UiUxInstrumentation:132 `real officer row sets cargo leader`，还未派遣；FAIL.png名单仍打开，触点Rect(42,861–1037,998)覆盖首行。原7文件恢复全等。不能把它当作运输核心规则已失败，也不能因UI5580通过而忽略5554失败。tap使用同步down注入再延时80ms发up，实际按压持续时间未记录；仅为待查线索，尚无证据判定长按/事件丢失或world身份失效。没有修改app或削弱断言，待UI14完成后重验。
# 2026-10-03 最新：生产、商人基础AP已从T的10改为原20（U已冻结构建，实装中）

更新：U已冻结并构建，路径见下。5554以同host GPU/同分辨率/同4核2048M但no-window重启，7原文件前后全等。R同包同190运输58.95秒原断言通过，11份证据installed-transport-190-headless；原两次GUI首行触点失败保留，不称唯一原因已定位。当前正在安装U并做真实190建设多旬回归，未将R结果移植到U。未操作UI5580。

下文T兼容阶段的10AP描述是历史值。原生产两个调用入口、商人入口真实机器码扣费及3MiB内存边界核验已完成；当前正式World/Army/Campaign、Production/Trade DTO和typed提交统一20，UI必须读取DTO。PC价格/工期/数量和完整军团规则仍待校准。319成本边界、制造756/216、交易920/88、Territory674通过；同R仅允许额外10AP差异后的72命令/72旬全save/RNG一致。详见ROUND_12。

U完整可继承路径：`out/parity/checkpoint-20261003-u/{source,manifest.json,verified.json}`，3880文件283434812B全部SHA/大小与冻结工作区一致，保留全部继承成果/UI13/固定资源/4原生输入。以该快照三模块及必要输入继承，不拉零散WIP。U双包构建32秒76任务成功，主87019189B SHA37d46e7c7d69ac121967947f16dc5fe576e2818c3996ecc5051a4e0a2d9ab1af；test960270f9…b5bc，封存native-economy-build-20261003/apks。U尚未实装；T是旧10AP兼容版本。app仍UI13，UI14未集成。稳定取消任务ID/异步串行查询契约另行推进，不把officerId改名伪装taskId。

# 2026-10-03 R运输复测与T交易接口增量

R/UI13 同 APK、同正常190输入在5554同AVD重启后，运输复测126.20秒仍失败于 `real officer row sets cargo leader`。点击首行 Rect(42,861–1037,998) 后名单仍开，尚未派遣；不是规则拒绝。两次失败均保留，重启不能消除此问题，也不能认定驱动停顿是唯一原因。证据：`out/parity/ui-integration-8e1d5f7/installed-transport-190-host-retry`，5个附件及原始日志，7原用户文件恢复全等、无新增。UI14仍未集成，app文件未由玩法方修改。

新增 `TradeCommand(expected, operation, cityId, officerId, food)`，operation为BUY/SELL；`GameApi.preview/execute` 同既有线程与StateToken边界。未接UI，不得沿用R APK的实装结论。当前季节价格、10AP、1000整千步长、20000买卖共享旬额都是项目既有模型，PC校准仍待证实。

`TradePreview.quote` 在数量非法时仍给出原输入、最低/最高/步长、千粮单价、已用/剩余旬额、资源容量与按资源计算的availableMaximum。该最大量只表达库存/额度，不代表武将、城市或AP已合法；必须以allowed为准。quantityValid=false时quotedGold仅整数运算结果，不可展示为可成交价。宿主忙/过期/关闭时quote=null。effects仅允许时存在，含实际钱粮/AP/旬额、武将行动与功绩前后值。所有拒绝保留原输入，不自动钳制。

正式命令重验后调用正常 `Campaign.trade`，成功才安装权威候选并发一个TRADE_COMMITTED事件；事件仅是已提交事实，钱粮数值取新权威快照，不解析detail。预览不写权威、不掷随机数、不发事件；旧token重复提交拒绝。价格/资源校验和普通命令共享TradePlan，尚无异步线程模型变更。

专项TradePlan919和TradeSession88通过。既有verifySession在本次与完整R独立重编均失败于exact old-scenario starting state，不能报整体全绿。完整R交易222命令/44旬266行消息、全save/RNG全等，SHA52efb1d8…c598b。

新增 `ProductionCommand(expected, operation, cityId, officerId, item)`：EQUIPMENT+Weapon枚举名（含器械），SHIP+Ship枚举名；不支持SWORD/BOAT制造。preview/execute保持相同线程/token契约。resources含费用、AP、设施种类/总次数/余次、当前库存/容量/同类在制品。允许时effects区分delayed、outputQuantity、stockAfterImmediate、pendingAfter、busyTurns、taskLabel和实际武将行动/功绩。预计产出不保证未来完成，不可提前写入库存。正式普通兵装调用World.produce，舰船调用Army.produce，成功只发PRODUCTION_COMMITTED已提交事实；无动画反馈向规则反写。取消任务仍旧officerId接口，尚无稳定taskId/typed取消。

ProductionPlan756/ProductionSession216通过，完整R制造132命令/108旬240行全save/RNG/结果全等SHA3140b279…bc1e6。本批保持项目原模型，不称PC价格/工期已校准。详见ROUND_12。5554复测后已关闭空闲AVD，保留数据，当前无本侧设备测试。完整增量快照与新包构建正在准备；未冻结前勿按当前工作树零散继承。

## R23 人物能力状态接入契约（实现中，未经新包验证）

新ScenarioData开局使用v34：基础五项、五经验、成长码、人物来源编号集合、来源出生年和固定年龄/能力变动设置均随局保存；Officer五项字段为当前值缓存。读取必须验证缓存与状态一致，不在查询/存档时修复或查外部资料。v31—33解码维持旧数值模式并写回v33，不反推基础值、不扣除历史内助；该兼容模式明确不具备新增经验/动态加成。直接构建World的既有测试/工程局仍为旧模式，正式ScenarioData开局才启用。

来源桥仅使用666已核实身份且16候选来源成长码一致的人物；宋宪/徐荣保留279,333集合。工程基础能力仍为既有剧本作者数值，不冒充原剧本导入。新局可显式设置ability-fixed-age与ability-growth-disabled（0/1）；缺省分别采用现有reference-dates=0沙盘策略和false。这是项目新局策略，不是PC用户设置默认值或原剧本启用身份。无来源/无出生年不编造成长曲线。自定义覆盖保留身份的成长/经验，新增人物使用无来源状态。

Editor.template返回基础五项；编辑/培养写基础值，官职/关系/伤病/日期变化重算缓存。现有培养+3/+5和舌战奖励流程仍未完成原规则校准，不因状态接入宣称完成。

TradePreview.Effects新增abilityStateManaged、politicsExperienceBefore/After、politicsBefore/After；旧构造器保留，旧模式经验标为-1。预览纯读，不推进回合、不消耗随机数、不发视觉事件。普通Campaign.trade完整校验通过后，先政治经验+5（上限3000）并重算，再功绩/钱粮/AP/城市已交易状态；拒绝不改变任何经验或缓存。typed提交仍只发一次TRADE_COMMITTED，StateToken与权威候选安装边界不变。价格/数量暂仍旧模型，不能称PC报价已接入。

R23配偶有效性补充：原47a630→人物虚函数488430明确排除状态6未登与8死亡（正常+17c=0）；原标签来自48ea20/48ea30。72组实际状态/开关/双方技能、360项当前能力输出及完整3MiB/RNG守卫通过。现有Life.UNAPPEARED/DEAD对应无效配偶，登场/死亡/生卒编辑立即重算双方；俘虏仍有效。原+17c强制有效字段的设置来源/用途未知，工程无对应状态，不据此编造选项。
