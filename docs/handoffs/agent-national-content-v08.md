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

具体只读接口、资源列序、ID 台账与样例见数据说明。Agent 1 后续统一扩展 World/SaveCodec：原版地图尺寸/地形、城关港/开发地与连接、人物生卒/身份/关系、特技/宝物/事件运行时映射。目标安装未经提取之前不能把来源 X/Y 直接当作游戏六角坐标。本分支没有扩大尺寸、另建游戏状态旁路或升级存档。

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

PR #6 当时未合并，未假定全部验证。基线实跑：Core 189、Scenario 359、Domestic 513、Tactical 71 案例 / 136168 断言、Strategy 5407、Campaign 59 案例 / 424 断言、Army 45 案例 / 465 断言、UI 64，均通过。未在本地发现继承核心失败；README/旧数据文档仍有 v4/v5 描述，真实 SaveCodec 是 v6，留给 Agent 1 联合整理。

本轮本地：同组核心回归通过；新增 Content 2635 断言，真实新开局到第 3 旬交战；UI 51730 断言；Python 15 异常/语义测试；2 份源 blob 与 850×62 人物/87据点/43宝物/100特技的原行核对；生成资源 --check 一致。

测量：本地 Java 17 冷读目录 68.362 ms（单次，不是手机性能）；地图可见范围压测已通过。Android 软件 Canvas 测量由安装测试输出到 content-performance.txt，不宣称 FPS。

本地无 SDK/模拟器，Android 编译、Lint、三种横屏安装及 APK 证据正在独立 PR CI 运行；最终结果将追加到本节。不可把本地 Java 通过视作手机验证。
