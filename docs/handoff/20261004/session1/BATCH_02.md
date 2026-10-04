# Batch 02：同一权威人物 DTO、正常页面与本次新 APK

目标 active，完整 PC 剧本/人物还原未完成。本批从完整 8400301 checkpoint 继承，不写原目录，不合入媒体 WIP。

## 实现与文件范围

新增 OfficerSnapshot（game-api）与 OfficerQuery（game-runtime），GameApi/GameSession 增加线程受控只读 officers()，携完整 StateToken。姓名、身份、归属、所在地、状态、五项 current/base/growth/XP、适性、功绩、官职、忠诚、关系等均从保存的权威 World 投影，列表集合/能力集合不可变。旧档未知 base/growth/XP 为空，sourceVariant/nativeId/字/原传记明确未知，不从最新 Catalog 追填。没有改变 SaveCodec、规则公式、保存策略、命令经验、商人/生产/七格位移或 RNG。

正常武将列表的显示/检索/排序和详情同 DTO。DataTable 新人物重载保持现有 World.Officer 为媒体/选择 ID 适配器，文字不读它的可变值。MainActivity 只新增查询方法并改人物详情文字，定位和头像沿用旧接口；OverviewUi 仅人物列表改动。没有触碰 PortraitCatalog/OfficerPortrait/头像、SoundEffects/音频、AndroidGameBridge、Unity 或 3D。前镜像 SHA 见 identity-dto-before.json，全部构建源 SHA 见 apk-build-source-guard.json。

新增相关测试/工具：OfficerQueryTest、verify_pc_officer_ui.py、OfficerUiOpeningWriter、ArtOfficerFixtureCanonicalizer、canonicalize_pc_officer_fixture.py、android/OfficerInfoInstrumentation、build_officer_info_ui_probe.py；game-runtime/build.gradle 注册真实 main-style 测试。全部源码都在自己分支。

## 验证

- JVM：九个实际工程剧本，正常巡察、每个三旬、真实存取/重开/下一旬续行，全 Save/RNG/token、不可变、错误线程/closed 检查 515120 PASS。
- 旧档：真实 v32/v33/v34/v35 host+ART/v36 host+ART 八份夹具，查询不改值与完整下一旬续行 33976 PASS。v31 独立夹具未取得，未宣称覆盖；旧保存不会被查询强制升级。
- 原有 CurrentScenarioSession 1666、SnapshotReadOnly 180、BridgeSession、架构及 168 固定资源通过。未改这些原断言。日志在本目录及 out/session1。
- 本次独立 APK 在 emulator-5554 实装并回读同 SHA：正常菜单新局/势力选择/取消/旋转/预览释放/槽3保存读取全 Save/RNG 64 PASS（65.54 秒）；实际列表键盘检索/排序/滚动/旋转/Home恢复 49 PASS（49.15 秒）。未移用旧包结果。
- 独立验收 APK 仅含新测试类，生产类来自上述实装主包；三个实际势力的刘备/曹操/张辽，通过真实列表搜索与行点击详情，对照独立解码保存检查五当前值、base、XP、功绩、忠诚、所在地与未知文字，完整 Save/RNG 不变，30 PASS（11.79 秒）。不是静态页面/算术代替流程。
- 每次内层测试均恢复原七个内部文件，自动档 SHA 82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9。完整外部文件/库 2902 份封存 2.1GiB，末次全部原文件设备 SHA 核对一致。新增证据目录保留；未清用户数据、存档、库或原资源。5554 锁已释放。媒体契约使用 5582。

主 APK：`out/session1/apks/app-debug.apk`，SHA `2c4b8536c3e59c639b422f0b9a1d2902ebbbd45e311464ac38045b9bab083919`，87546102 字节，universal 构建。原测试 APK SHA f220d9323daa07da0ab468713fd942d383f48126a3f9141c59909d1327c00b7e；详情验收 APK SHA d0eb449784ef81fe0c2b6de7564c8cb6208cd06c66ce302fcfc3489bab84c0ca。安装只证实 x86_64；ARM 仅打包，未实机验证。版本标签仍继承159，以 SHA 和源码守卫识别本产物。

## 失败、修复与明确边界

原日志均保留，不删失败。初轮漏传槽位 SHA 被原守卫拒绝；JVM 夹具第二轮因 ART GZIP OS/CRC 五字节差异再次被守卫拒绝。用实装主 APK 的真实 SaveCodec 在 ART 生成稳定夹具，未放松断言。canonicalizer 初版 Path.of 不受 API29 支持，改 Paths.get；只写独立 /data/local/tmp。外部备份上传因设备余量不足被拒绝，未安装即失败；随后从 host 流式恢复，不清设备文件。详情测试初版漏窗口屏幕原点，后修正确触点；搜索曹操还匹配其势力，改按实际稳定 ID 定位行，保留全部数值/保存断言。

本批是当前保存事实的统一读取与页面接入，九个工程剧本仍不是官方复刻。16 PC 来源的有效身份、17 数值/适性差异、87 据点资源/设施/外交、原开局后处理/事件、全字段、原传记/字形连接尚未接入正式来源新局。完整多旬菜单流程需对全部实际 PC 剧本执行；本批只证实当前工程包和人物查询边界。不能称完整目标完成。

## 原传记资源的新证据

inspect_pc_message_resources.py 在只读隔离 VM 执行原4711c0及其bitstream/getter，不重写解压算法。四份 S11MSG00–03 实际解出104346/397539/511349/304691字节，全部压缩输入消费一致，输出前后4KiB/尾部canary、只读源/字典保护通过；第二次所有二进制和JSON字节一致。S11MSG02含原武将列传，但有控制码和gaiji，索引/人物/生效链仍待原 getter 验证。未把扫描文本冒充已绑定原传记。首次工具误认context+0为输入游标，被自身守卫拒绝，按原471090确认+14指针后修复；失败日志保留。

下一批：原人物字段 getter/字形与列传绑定、逐源实际加载链、完整开局资源/设施/外交/事件，再转换新来源剧本与持久化来源字段。新增内容只影响明确新局，旧31–37各策略仍须完整续行证据。
