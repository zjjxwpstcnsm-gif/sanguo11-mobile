# Batch 06：来源新局保存边界与旧档城市指令兼容

目标仍 active，本批没有完成16候选来源新局菜单/人物数据导入/开局事件。实现基点840030195e39cec3c0d352e010a3c3e614893ca2；前镜像471e970645b7073b32b6d18399f54dcb207d73f3。继续独立完整工作树；未改媒体、AndroidGameBridge序列化、Unity、3D或共同台账。

## 必需的新局容量与来源保存

原逐源势力getter确认两份候选（Scen007、Scen014）存在37个普通君主势力，另有固定原42–46的5个部族势力槽；活动势力最大42，原注册表总容量47。不能把原NPC/部族槽位直接当可选玩家，也不能删掉超出32的势力来凑现有框架。

World普通构造仍最多32；新增仅PC来源工厂/存档38解码使用的47容量入口。PcScenarioIdentity固定保存scenarioId/sourceVariant/原路径/原文件SHA/Shared SHA/原EXE SHA/原日期及缺口。变体沿用原manifest的路径slug、精确路径SHA12位与完整文件SHA；加载保存不查询新目录追填。来源identity仅标安装候选，不证明完整开局、实际覆盖或官方身份。当前日期模型仅接受原1日开局，其他日期拒绝，不截成1日。

存档38是明确的新来源世界入口，payload沿用37字段排列并保存独立来源命名空间，允许最多47势力；必须已有四份明确初始化的能力/行情/生产/技巧策略。来源缺失、不一致或异常不得回退成旧格式。旧世界依然按已存策略编码33/34/35/36/37，不强制抬成38；解码旧31–37时，即便有同名未知扩展也保持opaque，不激活新来源规则。新格式不能由老版本APK读取，这是新来源格式边界，旧档不搬城/改值/加来源。

战报关联long mask扩至原47容量范围；0–31的原bit与正常旧世界行为不变。47势力容量夹具通过正式GameSession typed巡察、stale拒绝、3个完整nextTurn/commit、完整保存/RNG与重开对照24项。此夹具不是PC剧本，不把它的结果算作原来源或APK菜单流程。

## 旧31–33城市指令策略

此前七状态原golden失败为已继承v33 AP10/20差异。本批按显式策略处理：无独立新策略标记、无已存基础/成长能力策略的旧31–33世界使用原10AP和9548bb35保存契约下的旧巡察公式；无base/growth/XP追填。已存34–37保持其当前20AP及已接原巡察/经验模型。新建未管理基础能力的作者世界用独立pc-basic-city-policy-v1记录native20策略，跨事务复制/存取不丢失；新局初始化能力后不额外增加该标记，旧解码不补标记。preview与正式命令共用策略。

原golden文件、历史fixture和当前核心其余实现未替换。GameSessionTest1690项通过，包括真正9548bb35 v33初态及后续六步原golden、Native/bridge完整保存/RNG、token、事务和生命周期。BasicCityPolicyTest15项验证原v33普通征兵/巡察、预览、10AP、无标记/无base追填、重开，新建未管理模式标记，以及已存37策略。不是复制旧AA核心，只保留有原历史来源依据的两条旧规则入口。

同一PcScenarioLegacyProbe分别对前镜像完整核心和当前核心运行32组真实保存/新局：初始重编码全部相同；26组34–37正常巡察与3个完整nextTurn/save/decode字节及RNG全等；6组32/33仅因明确恢复旧基础城市模型产生后续差异，具体文件列于比较JSON。真正31fixture仍未找到，不能宣称31实际续行通过。

## 验证的实际范围与失败

PcOfficerInfoTest110066、OfficerQueryTest、CityCommandRewardsTest25669、CityActionPlanTest1899通过，原三模块边界检查通过。完整check未全绿：AbilityResearchTest在移城后保留失效太守而失败，CoreTest在“AI uses deployment commands”失败；二者对471e9706父核心也原样失败，不修改断言或声称通过。

test-unity-u01.sh原来漏编已存在BridgeFactsFixture，本批只增加该Java输入；BridgeSessionTest实际命令回归通过。原Unity U01 fixture仍mapRevision63，而当前/父核心生成65且二者JSON字节相同，因此cmp按原要求失败；未改Unity fixture、序列化或放宽cmp，完整架构脚本不能记整体PASS。GameSession1690和bridge实际通过单列。

## 本批实际APK

独立缓存构建76任务成功；主APK91330629字节，SHA b1b4e68b62ed57ea1d451973ab05e2ebc6ec1eb4c686eb16ef026c7ae3111724；test2018931字节，SHA a040349e193f06775d9f56b1d242a896db55ff854cc8161b1869d1abe62c0d65。路径out/session1/apks-batch06/。不移用Batch03旧包证据。

核实5554无锁/并行instrumentation后独占；先备份全部外部files和内部保存/偏好，再实际安装本包。UiUxInstrumentation opening71.63秒、general61.95秒通过：真实菜单、工程190势力选择/预览、新局确认、保存/读取、人物/城/任务搜索及横竖屏/后台恢复。每套7个原内部文件读回字节全等，无新增；2971个原外部文件（包含库和媒体缓存）最终流式恢复并读回全等。新增仅本次证据文件。设备force-stop，独占锁已释放。

这些流程仍是工程剧本，不能当16份来源新局通过；源PC roster/归属库存转换、未登/未发现/NPC生命周期、来源技能/引用/全部字段接入、开局事件条件和效果仍未闭合。ARM仅封装，没有新增实机证据。下一步必须完成来源新局转换与正常菜单/多势力/多旬/保存冷启动，不能停在容量框架、表或此局部验收。

最终指定六组验证任务整批 BUILD SUCCESSFUL；OfficerQueryLegacy实际33976项，v32–36八份保存。新类与测试均为本分支新增；完整check的三处继承失败继续保留原证据。
