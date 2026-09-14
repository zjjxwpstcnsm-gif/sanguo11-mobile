# Agent 2：national-content-v08 交接

固定基线 `ff624b6e9c48c81d7111a9a642b18cf6bd098fe1`，PR #6 的公开提交。分支 `agent/national-content-v08`。另一 Agent 的工作区和未提交接口均未使用。

本轮实际内容、来源、转换、字段接入和缺口详见 `docs/content/NATIONAL_CONTENT_V08.md`；机器核验/差异为 `docs/content/verification-report.json`，坐标预览为同目录 SVG。

- 670 历史人物社区资料，4 人能力/适性双表比对；原安装核验仍为 0。
- 42 城 / 10 关 / 35 港名录；42 城原表 X/Y 仅资料预览，45 关港坐标隔离。原版地形 0 格。
- 100 特技名、43 宝物行、14 剧本名称日期；事件和完整势力开局未知。
- 新增可玩“武将资料演练”：18 人能力与适性直接加载到实际游戏，其他初态沿用原创区域沙盘。官方完整历史/假想剧本为 0。三个旧场景保留。

## 当前内核与 Agent 1 接口需求

已接入：18 人整数 ID、显示别名、5 能力、6 适性，现有城池资源/地形/位置/所属，v6 快照。静态目录与运行中状态分离。

已采集未接入：生卒登场、关系原文、性格性别等 62 列、特技 ID、宝物及缺少剧本上下文的归属。未取得的事件和开局状态没有伪造。969 条关系目标未解析，不能用同名自动绑定。

具体只读接口、资源列序、ID 台账、可运行样例和兼容案例见 `docs/content/AGENT1_INTEGRATION_CONTRACT.md`。Agent 1 后续统一扩展 World/SaveCodec：原版地图尺寸/地形、城关港/开发地与连接、人物生卒/身份/关系、特技/宝物/事件运行时映射。目标安装未经提取之前不能把来源 X/Y 直接当作游戏六角坐标。本分支没有扩大尺寸、另建游戏状态旁路或升级存档。

## 共享文件精确变更

- `ScenarioData.java`：添加 community-reference 来源及必填 reference，完成解析后比对 ContentCatalog 人物真实字段；保留旧来源行为。
- `ScenarioTest.java`：保留原断言，目录数量由 3 改 4，调用新增 ContentTest。
- `MainActivity.java`：头栏导航图、菜单资料/势力入口、选势力后显示开局边界与数据情况、新局清理城市/任务检索、失效筛选 ID 复位。命令 UI 调用签名不变。
- `MapView.java` / `MapCamera.java`：可视地形界限、对象分块、人物/城市索引、导航图缓存/手势/Bundle、缩放标签、测量计数。
- `OverviewUi.java` / `UiModels.java`：城市、任务检索与势力入口，复用原列表和命令；`ClientState.java` 只增导航 Bundle 字段。
- `PresentationTest.java`：搜索和相机可视区域断言；`GameSmokeRunner.java` 追加资料开局/搜索/导航/恢复/压力测量真实点击流程。
- `.github/workflows/android.yml`：PR 构建明确 checkout head SHA，添加离线数据验证和 UI 模型测试、APK 资源检查，独立文件名含 national-content 与短 SHA；`scripts/smoke-android.sh` 拉取测量证据。
- 新文件 ContentCatalog / ContentUi / ContentTest、data/content 输入、core content 资源、演练包与索引、工具及本文档。

未修改：World、War、Army、Campaign、Domestic、Strategy、SaveCodec、WarUi/ArmyUi/CampaignUi 签名、全局版本号、README、progress、FEATURES。

## 继承问题与验证

第一轮 Android CI 已通过编译/Lint/资源检查。对照 PR #6 后续公开提交 73fb2a9 / d105fc7 / d89f988，确认固定基线的定位清除 moving、任务角标漏军备制造，以及 tapHex 注入/等待问题；本分支仅补入 MainActivity 两处和对应测试触控路径，不改基线、规则或 ArmyUi。PR 比较目标为 PR #6 分支（可能继续前进）；本分支父提交仍严格为指定 ff624b6。

资料页压缩顶部工具栏，长列表按首个稳定 ID 恢复；新增无任何数据资源的独立 ClassLoader 存档往返验证。

PR #6 当时未合并，未假定全部验证。基线实跑：Core 189、Scenario 359、Domestic 513、Tactical 71 案例 / 136168 断言、Strategy 5407、Campaign 59 案例 / 424 断言、Army 45 案例 / 465 断言、UI 64，均通过。未在本地发现继承核心失败；README/旧数据文档仍有 v4/v5 描述，真实 SaveCodec 是 v6，留给 Agent 1 联合整理。

本轮本地：同组核心回归通过；新增 Content 2638 断言，真实新开局到第 3 旬交战；UI 51730 断言；Python 15 异常/语义测试；2 份源 blob 与 850×62 人物/87据点/43宝物/100特技的原行核对；生成资源 --check 一致。

测量：本地 Java 17 冷读目录 68.362 ms（单次，不是手机性能）；地图可见范围压测已通过。Android 软件 Canvas 测量由安装测试输出到 content-performance.txt，不宣称 FPS。

本地无 SDK/模拟器；最终 Android 验证已由下述 GitHub Actions 的 API 29 模拟器完成，未测试实体手机。

Android 尝试 34794749995：编译/Lint通过；1920×1080 旧玩法与资料开局出征走通，长列表新增断言失败。截图证实页面已滚动但首行仍部分可见；修正测试的一次滚动即换首行假设，进一步精简行摘要/顶部，并保存首行像素偏移。后续验证以新提交为准。

## 最终 APK 与 Android 验证

- PR：<https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/8>。本分支固定基线未改变；最初 Git HTTPS 直连受环境网络限制，按公开 GitHub commit/tree/blob 恢复并逐对象核对固定 SHA，提交树也与远端 API 结果一致。
- 已安装验证的功能提交：`08da7a68592bb4834c5a08ebf824dedc27bd0d35`。
- CI：<https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34795560166>，成功。后续交接/证据提交只改 docs，不改变该 APK 的运行时代码或资源。
- APK：`sanguo11-mobile-national-content-08da7a6.apk`，279402 bytes；artifact ID `10329458403`。
- APK SHA-256：`d9e5e1385be4d6eb06a350c1eb7250793508ace5e25421049ac4a8e1e80bb84a`。已核对下载字节与 CI 的 SHA256SUMS；apksigner 验证通过（v2 签名）。debug 构建，保留基线 versionCode 7 / 0.7.0-army-dev，不是 Agent 1 联合发布版。
- `./gradlew test :core:check`、Lint、assembleDebug、测试 APK 构建通过。Lint 0 errors / 12 warnings：OldTargetApi、DiscouragedApi、DataExtractionRules、ViewConstructor 各 1，SetTextI18n 8；未通过抑制或降级错误绕过检查。
- API 29 x86_64 模拟器，density 420；1920×1080、2340×1080、2400×1080 三轮均 SMOKE PASS。每轮 32 张截图，总计 96 张，artifact ID `10330500123`。Lint 报告 artifact ID `10330430264`。
- 保留全部旧 UI 流程；新增真实点击覆盖资料开局/选势力/出征/推进旬、城市/任务/势力/资料检索、空结果、长列表滚动恢复、资料坐标预览、导航图触控定位、不同开局间存读档与 Activity 恢复。核心交战测试通过正常移动后由刘备弩兵在第 3 旬攻击 AI，造成 391 兵损失，非传送或直接注入伤害。
- 人工检查了三种比例的资料恢复、来源坐标和地图导航截图。来源坐标预览在全览时密集城市的标签仍有重叠，可缩放查看；不把它当成可玩的原版全国六角地图。

### 实测绘制

工程压力输入：128×128 地形、128 城、128 人；只测软件 Bitmap Canvas 的 onDraw，预热 5 次后 40 样本。不是 FPS、GPU 呈现时延或实体手机性能。中间样本为排序第 21 个，原输出 p95Ms 为离散取样第 39 个。

| 横屏分辨率 | 实际地图视口 | 访问地形格 / 16384 | 候选对象 | 中间样本 ms | 第39样本 ms | 最大 ms |
|---|---|---:|---:|---:|---:|---:|
| 1920×1080 | 1111×696 | 113 | 16 | 2.86617 | 4.27881 | 4.33209 |
| 2340×1080 | 1374×696 | 121 | 16 | 3.71147 | 9.72511 | 10.2523 |
| 2400×1080 | 1410×696 | 131 | 16 | 3.46911 | 5.04476 | 7.5353 |

原始 instrumentation、content-performance、CI 摘要、APK 元数据与本地回归证据均位于 `docs/content/evidence/`。没有原版全国地图可供性能或拓扑一致性验收；本轮没有对其宣称通过。

### 剩余缺口

完整原版全国地形、关港位置、开发地/道路/水系/岸线/连通；目标安装哈希与原版 ID；古代/额外武将版本归属；全部官方开局的势力/人员/资源/外交/研究；事件条件结果；宝物剧本归属；人物关系目标 ID 与相应运行时规则。官方完整可玩剧本仍为 0。此次交付是来源透明、可实际游玩的资料演练与原生导航增量，不能表述为 100% 还原。
