# B contracts and A adaptation requests

Ownership uses ../parallel-repair/CONTRACT.md; historical 20261002 A/B role names do not override current ownership. Implemented B admission/fee APIs are listed below; additional detached scene facts remain pending.

Existing authoritative facts: GameSession state/sessionId/generation/revision; LegacyCommandSink lazy checked transaction; source officer metadata/nativeId/sourceVariant and current ability DTO; existing TurnJournal fire/facility/action facts. Rendering must not execute rules or consume either RNG.

Pending exact A adaptations will be listed here with consumer path and same-state example after B interface is implemented and tested. MainActivity, MapSceneSnapshot, MapHost, faction picker, theme/media and frozen bridge are read-only for B.

## Installed B probe runner

The new exact B-owned instrumentation class is selected only through session-b/instrumentation.init.gradle at test build invocation. Existing shared root/Android Gradle and AndroidTest manifest stay byte-identical. The init script changes only testInstrumentationRunner, not production APK input or JNI. Final integration can register the runner in the A-owned test manifest sequentially if desired; it is not a production menu dependency.

## Fieldwork admission diagnostics (B implementation candidate)

`Fieldworks.buildCheck(unitId,kind,target,direction)` returns immutable Validation `{code,field,detail,allowed()}`; buildError/sites/build share this single pure rule check. Same coordinates remain axial Hex; user display conversion only uses MapCoordinates.display/nationalSource. No rule distance/terrain/gold/tech relaxation, no saved schema or RNG changes.

Stable codes: NONE, UNIT_COMMAND, BUILD_TECHNOLOGY, BUILD_DIRECTION, UNIT_BUILDING, UNIT_GOLD, TARGET_ADJACENCY, TARGET_OCCUPIED_OR_FIRE, TERRAIN_NON_NAVIGABLE, TERRAIN_FOREST_OR_SWAMP, BUILD_TERRAIN, SITE_DISTANCE, MILITARY_DISTANCE, STRUCTURE_LIMIT. detail is display text, never parse it as a protocol.

`withdrawCheck(unitId,cityId,gold)`, `withdrawMaximum` and `fundingSites` share actual canEnterSite admission; funding sites include seven-cell city positions. Stable codes: UNIT_COMMAND, FUND_SITE_OWNER, FUND_SITE_ENTRY, FUND_AMOUNT, UNIT_GOLD_CAPACITY, SITE_GOLD. Same command remains lazy LegacyCommandSink/GameSession. Successful construction/funding preserves previous debit/action/save semantics. Example: source14/LiuBei/Yongan/unit1 carries3000 at202,46: CAMP target202,45 rejected SITE_DISTANCE; after ordinary move202,45 target202,44 allows NONE and1500 debit/action/incomplete structure.

FieldworkUi candidate consumes A existing commandDialog/trackDialog theme+stale/double boundaries. No MainActivity/MapHost/MapSceneSnapshot/UiTheme change needed. A map error callback receives this exact core code/detail and actual chosen coordinate. Source execution6x81 proves original city footprint-distance<=2 guard; it remains intact.

A theme contract received: completed source reference5431de8d at2191/session-a/THEME_CONTRACT.md. New readable/readableTree require A frozenUiTheme plus FactionColors dependency. B does not copy A WIP; current dialogs use common existing UiTheme.dialog via trackDialog and commandDialog. Theme-specific final adaptation is pending frozen A increment.

## A exact movement adapter request (delivered frozen47326188; A-owned file unedited by B)

MainActivity.java:778 previewMarch currently infers a friendly city target as `world.marches.previewCity(unit.id,c.id)` even when the player selected ordinary "行军". For the ordinary march command, use existing pure `world.marches.previewMove(unit.id,target)` and keep the actual chosen axial target. Reserve previewCity/GARRISON for the explicit existing "进驻" action (ArmyUi.garrison/previewGarrison already provides it); preserve explicit auto-attack/approach pathways and routeMove restoration flag. B core candidate now permits normal tile movement to stop on friendly seven-cell positions, while explicit/saved-legacy CITY garrison still docks. Please apply only in A's branch and deliver a frozen increment with exact SHA. B will not edit this A path or copy WIP.

UI evidence target: Source14/LiuBei/Yongan ordinary deployment3000 gold -> move onto lawful city cell -> "补充携金" lists it -> cancel is pure -> confirmed transfer charges city/u and action once -> nextturn construction -> saved resume. The current inherited UI automatically calls garrison, so the city-center branch cannot be declared closed until this adapter is installed.

## Confirmed category distance correction

Original spatial probe expanded: six centers×81 cells×9 native kinds; original camp3/category1 rejects footprintDistance<=2, native walls8/9 category2 and traps16–19/21/22 category3 allow empty city-near cells. Original5a4170 checks separate0x100000 versus0x200000 bits; complete416690→4848f0→4847a0 only clears category1 radius around all seven city cells. Candidate Fieldworks now applies the siteDistance<=2 guard only to existing military(kind), retaining every other admission requirement and seven-cell occupancy. Normal Source14/Yongan wall/trap formerly had SITE_DISTANCE on legal free tiles. This is finite original spatial evidence, not whole construction cost/progress/AI restore.

Foreign/neutral/ally province admission of original5a3460 is not closed by the null-unit spatial fixture. Candidate distance correction therefore only permits city-near walls/traps at own sites; existing distance bounds at non-owned sites stay pending source verification. This does not claim all placement legality or original diplomacy parity.

## Fresh-source military base fee API (candidate)

Fieldworks.baseBuildCost(kind) returns the saved base fee; buildCost(unitId,kind,targetHex) is the authoritative current base-only quote consumed by buildCheck/build and B confirmation. Fresh explicit PC-source new games save pc-military-base-fees-v1 with full sourceId/sourceVariant/sourceSha/SharedSha/EXESha and18 verified native mappings. CAMP/FORT/FORTRESS now500 in those new games. Old31–39 worlds lacking it and authored engineering worlds retain legacy1500 without decode backfill/refund/HP/RNG or capability changes. Scalar base query is pure; militaryHQ80percent/target-region discount is explicitly unimplemented. A must use this API if showing fees, never infer them from StructureKind.gold. No A path adaptation found necessary outside B FieldworkUi; all app fee references audited. Future discount rules must use the target-aware API and same StateToken.

## Existing immutable officer contract and remaining scene facts

GameApi.officers -> GameSession.officers -> OfficerQuery.capture(authority,state) returns immutable OfficerSnapshot on the serial logic thread. Join Officer.id with SourceInfo nativeId/sourceVariant/sourcePath/sourceSha/recordSha; canonicalOfficerId is nullable. The four verified original-glyph identities have proven canonical links; never derive runtimeId from nativeId arithmetic. Current/base/growth/experience/aptitudes, presence, owner, city, unit, role, merit, office and commandLimit come from the current save. Older missing source/ability policy stays unknown. Metadata and media manifests remain separate ID-linked objects. Rendering must not run commands or draw either rule RNG.

Existing GameSnapshot projects terrain/layout/sites/units, but does not yet expose complete fire lifetimes, construction/destruction, date and region/relationship facts. Those fields have not been delivered through a new API. GameEvent.id uses session/generation/revision/kind; TurnJournal.Event.id uses journalId/sequence. Do not conflate these identities, invent parents, or parse human messages to derive events. Structured speaker/original voice profile and full fire/facility facts still require B DTOs and exact examples before A adaptation. The raw voice number is currently human original-information text; A should not parse that prose to choose audio. MainActivity/MapSceneSnapshot/MapHost and bridge serialization remain untouched by B.

Frozen A47326188 six-file dependency package is staged read-only: five theme files and one completed ordinary-MOVE adapter. All16 B pages compile against its UiTheme.dialog/readable interface. No A WIP was copied.

## Pending A original budget editor adapter (APK32 still in acceptance)

A-owned EditorUi.java:76: existing faction-editor label0–60 becomes“第一军团行动力（0—255）”only when PcArmyActionPolicy.enabled(w); oldabsence retains0–60. Existingw.actionPoints[side] is first-army projection in newpolicy; coreEditor.faction edits uniqueoriginalfirstarmy and rejectsambiguity. No B edit ofApath. Additive detached SceneFactsSnapshot.NativeArmy.originalValid/actionPoints (−1absence/unknown) retainold constructors; do not fabricate60/enableinvalidrawowner0. RequestsenttoA withoutWIPintegrationauthorization; onlycompletedfrozenBdelta afteractualAPK/restore.

## 玩家单挑败北后的继承待选（B已接API/页面，当前APK待验收）

ContestSnapshot.NativeDuel.inheritance 为可空 immutable DuelInheritance {rulerId,selectedHeir,candidates}，DuelHeir {officerId,nativeId,name,error,enabled()}；父级sourceId/sourceVariant/sourceSha和StateToken保持现有语义。正常B ContestUi展示继承人并经已有MainActivity.executeContest入口发送 ContestCommand.nativeDuelHeir(state,contestId,contestRevision,officerId)。未选择时settlementAvailable/dispositionReady=false，确认前完整终局/双方部队/RNG/旧君主保留；保存重开恢复选择。AI处分在第一次prepare保存一次，不因读/重复按钮重抽。更新格式6只在明确PDU3实际创建该待办后写入；旧1–5不由加载升级。B已经编辑唯一所有权的ContestUi，A不用按name/native index解析或重抽随机数。

实际host389检查：source0/player11，原618讨厌180，原自然败北捕获码1→AI处斩→7候选中玩家选native111（非AI默认176）→完整cold/foreign/stale/double拒绝→一次生产继承/死亡→4完整旬/cold。声明相邻部队仍不是正常出征/实际APK证据。原人控4b7e70 roster/有效选择另有完整只读receipt7a731...；原player-slot在force+60，由481480 setter，不是force+128。

A-owned MainActivity.java:1297 当前普通新局仍 ScenarioCatalog.load(id,player,seed) 3arg；本接口不授权B编辑它或自动改变所有旧策略。完整新局原单挑入口的最终适配需从B完成源选明确 options overload，随后按冻结A增量顺序真实APK验收；该菜单适配和全source matrix仍未完成。

## 当前义兄弟死亡关系（B规则增量，A读取契约）

来源人物raw锚点仍在不可变pc-duel-runtime-bindings-v1；实际PDU3处斩创建pc-duel-current-sworn-v1当前覆盖。公共Relations按原清理死者并保留剩余两人组；单人解组。既有immutable OfficerSnapshot当前relations使用稳定officerId即可，来源信息不得覆盖当前关系，也不得从native编号猜projectID。B的AI/登用/支援/编队/继承已改读当前锚点；读DTO不重抽规则RNG。原六次“614先登位组员、再死非君主”回调核实剩余同势力非君主raw150/显示100保留，B只写已经验证的原值。待登位顺序、俘虏/未出场有效性仍明确拒绝。没有新增Bridge/Unity字段或授权A修改规则；当前A适配主缺口仍是明确新局options入口及后续真实APK验收。

## 显式新局适配协作（20261008只读准备）

已按用户最初A/B“B提供精确适配请求、A改其文件”分工交给A正常新局options API和准确旧策略边界。A204方案/205只读取证已登记MainActivity、ScenarioFactionPicker；没有B WIP集成/编译或当前旧入口行为改变。需要B完成冻结源和选项文本/枚举原绑定后再由A交冻结适配。当前原EXE文本/指针表有“初級/上級/超級”“無/標準/多”“史實/長壽/假想”，其表位置和原控件选择字段已有源证；内部枚举对应、默认和rawlife3未闭合，不将数组顺序当真值或对旧档补策略。原军队押送释放新增由B逻辑所有，渲染只读当前人物所在地/返程状态，不能把原task37变成UI瞬移。

本轮只完成B内部原出征继承人登位、严格原行政任命Save边界与真实AI跨城太守阻断修复；所有A/App入口适配仍等待整批完成冻结。出征君主可有与真实unit所在地分离的行政home太守任命；SceneFactsAdministration现有site governor/assignment/unit facts保持真实分离，渲染不得把home任命当物理驻城、改规则或重抽随机。全原下一旬控制器/正常部署/native两连续处斩/APK未验证，不称全目标完成；本轮current原1392选任与真实13→14失败重放为独立证据。

原菜单选项后继：GameApi.pcOpeningOptions提供不可变、同StateToken三组0..2原菜单选择。savedValue非新局默认；默认与自动源覆盖未知，rawlife3不作第四选项。B单挑页面已消费同state原存设置摘要；A正常新局按NATIVE_OPENING_OPTIONS_CONTRACT.md继续只读准备，整批未冻不拷贝/集成B WIP，取消不创建World/变RNG。Root20=难度、24=战死、38=寿命以完整按钮/导出及写入块纠正历史注释；不调整旧保存字段值或策略。

来源寿命覆盖后继：16原文件Root18实际序列化来源已核实，仅7/13/14非零，Source7原配置27组合实测life3覆盖。只有明确4参新局新增pc-source-opening-options-v1并按原条件生成有效PDU3参数；旧保存缺失继续原策略。DTO.sourceFlag18/automaticOverridesKnown是这三设置范围事实，非完整生命周期/开局证明。A继续只读契约准备，未冻整批不集成WIP；默认与UI全部约束仍未知。

同地域直接释放后继：既有OfficerSnapshot.status可显示真实零时间task37占用“归队中 · 等待人员结算”；nativeReturnPending只读已有保存行，页面不得将remaining0当闲置或瞬移。稳定id/current city/行政home/StateToken边界不变，下一真实人员结算解除。没有新增Bridge/Unity/API字段或A文件适配需求；只有明确当前PDU3/PGO3 source0城市驻点直接释放开放，旧策略、关港、押送/登用零时间保持拒绝。正常菜单新局与整批APK仍等待完成冻结，不拷贝A WIP。

自然阵亡后继只读契约：DuelFighter.terminalOutcome保留原manager结果0/1/2，-1未知；只在terminal事实下显示阵亡待结算。NativeDuel.settlementError为纯当前共享准入原因，字段只读，B按钮用settlementAvailable。阵亡事实不提前写World死亡，也不创建俘虏处置；普通无携物当前PDU3死亡100功绩给原上阵胜者，无处斩缴物。held/ruler/district/task/old-policy未闭合仍保留；不能以static君主携物getter代替完整装备流程。A无新增序列化/Unity适配请求，整批未冻结不复制WIP；现有officer status及source/native/stableID保持。

自然阵亡携物后继：合法普通人物manager2携物的正式终局已按原current force ruler移交，胜者active/unitCommander/currentRuler不可混同；现有OfficerSnapshot/treasuresDescription读取保存后的实际持有。无需新增A/BridgeJSON/Unity字段，渲染不可提前赠物/写死亡/重抽规则RNG。旧策略、自然君主/都督/未知currentRuler等边界仍有真实拒绝，不将单源声明装备端点当完整正常菜单APK证明。整批未冻结不合任何A WIP。

自然君主阵亡后继：现有NativeDuel.inheritance/DuelHeir与typed NATIVE_DUEL_HEIR复用，新增内部保存format7只对应当前明确新局策略下自然outcome2的人控选择；旧format6捕获后处斩与1–5不升级。第一确认创建待选择事实，未选不死亡/移物/重抽；选择后才能一次终局。nativeDuelNaturalDeathError/settlementAvailable读取共享纯准入，页面不把无继承人选择当永久不能开启界面。141 typed检查及独立原AI/human候选回调已通过，详情NATURAL_RULER_STRATEGY.md。B ContestUi现有继承按钮足够，无A/BridgeJSON/Unity/JNI新字段或文件请求；仍待整Native批冻结，不合A WIP，不称正常菜单新APK完成。

首次启动无session后继：新增独立PcNewGameOptionsSnapshot与静态GameSession.previewNewSourceOptions(scenarioId)，提供来源身份/path/SHA/Shared/variant/unknown/原三组选项和已验证固定约束。没有StateToken、World、已存值或默认；旧PcOpeningOptionsSnapshot非空token要求保留。有session既有接口继续用当前state。A首次正常picker读取此静态目录入口并绑定source/SHA/generation，禁止dummy World/Token；B不改MainActivity/picker。536 first-launch+439/151旧接口/真实format5与39纯性当前通过，准确策略FIRST_LAUNCH_OPTIONS_STRATEGY.md；整Native冻结/正常菜单/新APK仍未完成，不拷WIP。

实际正常战役支援raw后继：primitive raw仍是严格未知；原508890未消费的分支采用pure lazy getter，不提前阻止正常操作或回填原字节，真正消费且未知保留原原因事务拒绝。实际普通25旬原库存部署/地图移动/95%接受后的首输入已修复，同一保存33人控输入/177帧/原自然阵亡/一次战役/3旬完整冷续行通过。无新API/BridgeJSON/Unity/JNI字段或A文件请求；B ContestUi现有同state输入继续消费事实。SUPPORT_RAW_CONSUMPTION_STRATEGY.md/guard记录完整路径/前后SHA。整批/正常A首次菜单/实装APK仍待冻结，不能移用此Host场次为Android验收。

组合候选60后继取代此前“无冻结来源不能适配”的准备状态：NATIVE_INTEGRATION_CANDIDATE.json绑定不可变全源码/196 B-only overlay/三JAR/APK，所有输入和归档逐SHA，A baseline196+612只读匹配。已正式请求A只在自己Main/picker路径进行stage组合适配，向B交不可变增量；B不改/取A WIP。API/严格未知/保存边界按前述契约保持；候选仍非最终完成批次、无新入口/实装/ARM验收。只有完成组合正常菜单和新APK/保存恢复后才形成相应完成交付，原全目标不缩减。

## 原AI处置人物绑定接口（r16候选，实际APK尚待验收）

Contests.NativeAiActor提供当前正式规则的只读人物身份；ContestSnapshot.NativeDuel新增aiActorPolicyEnabled/aiActorPolicyAdoptionAvailable/aiActorPolicyError、aiActorOfficerId/aiActorNativeId/aiActorName。无自动处置的非终局/平局/玩家获胜时身份为-1/-1/空，不推定角色。来源variant仍用外层sourceVariant。实际r13旧策略为officer10189/native189皇甫嵩；明确采用新策略后为officer10091/native91何进。获胜上阵者native555与原发言者接口独立，不能把aiActor当发言者/voice profile；此接口不证明原GUI台词或场景。

ADOPT_NATIVE_AI_ACTOR追加到API operation末尾，需StateToken+contestId+revision；只增加显式保存策略与一次协议revision，人物数值/原模型/双RNG/原部队与驻点不变。已保存AI选择不得重选。旧31–39/PDU3不在加载时升级；四参数明确新局可启用pc-duel-ai-force-ruler-v1。AndroidGameBridge与Unity序列化冻结，不新增其wire字段。A消费该DTO时只读身份和状态，不运行原概率或重抽随机数；B仅改自身ContestUi。

## r17候选人工处分判定人物接口

原4b2380的4afd60参数取collector+4势力君主，独立于AI策略。新增pc-duel-human-force-ruler-v1仅明确新局或旧玩家胜利终局主动采用；未尝试登用/未选处置时才可采用。DTO NativeDuel新增humanActorPolicyEnabled/humanActorPolicyAdoptionAvailable/humanActorPolicyError/humanActorOfficerId/humanActorNativeId/humanActorName；实际r12玩家胜利旧判定者503裴元绍，新判定者403张角。非玩家终局不填人物ID(-1)。ADOPT_NATIVE_HUMAN_ACTOR追加API尾部，StateToken/id/revision边界沿用。此人物是概率判定者，不替代发言者/voice。保留数值、位置、原对局/RNG、已有处置，取消纯，采用只改显式receipt与协议revision。原PC GUI仍未验证，普通新APK人工登用成功/失败流程另验。

人工策略r17补充：humanActor字段是原登用判定者，非上阵者/发言者/voice profile。独立pc-duel-human-force-ruler-v1显式采用同时开放Source0/PDU3/PGO3/CITY同城task37 duration0归队（下次人员结算结束），普通modal披露。零距离关港/未知地域/旧无标记仍拒绝。预览和提交共用当前君主/终局伤病魅力的原登用计划，预览不抽随机。采用不能重做已有尝试/处分，typed Operation只末尾追加；完整Native仍候选。

r19候选只读契约：NativeDuel末尾追加recruitItemPolicyEnabled/recruitItemPolicyAdoptionAvailable/recruitItemPolicyError/recruitItemRecipientOfficerId/recruitItemRecipientNativeId/recruitItemRecipientName；原登用种类6/7物品接收者与概率判定者分别绑定，旧无标记仍部署主将，仅新局/普通终局主动采用pc-duel-recruit-item-ruler-v1启用原当前君主。未执行完整单挑/正常正向回调验收前仍候选。Operation仅末尾追加ADOPT_NATIVE_RECRUIT_ITEM_RECIPIENT，明确采用不改物品/人物/驻点/数值模型/RNG，已尝试或选择处分禁止重做。A字段只读，非发言者/voice，Bridge/Unity/JNI不动。
