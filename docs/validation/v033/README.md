# v0.33 验收记录

交付 APK：`sanguo11-mobile-v033.apk`，6,763,359 字节。运行源码提交 [`36600f9faa8ba5bc4abc07b93478590f8787e4df`](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/commit/36600f9faa8ba5bc4abc07b93478590f8787e4df)。规则核心为a7739af，最终增加短横屏列表空间修复；此后仅验收资料更新。[PR #39](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/39) 待审阅，未合并。

|身份|值|
|---|---|
|包名|game.sanguo.mobile.dev|
|版本|versionCode 33 / 0.33.0-architecture-rules-dev|
|系统|minSdk 26 / targetSdk 35|
|APK SHA-256|0eef01ad496f7c0cf6737ee463d5e653b0009b0b4f3b51ec1b1da419a41a9ed5|
|开发证书 SHA-256|8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24|
|签名|既有公开开发证书，APK v2 签名验证通过；见 signature.txt|
|保存格式|v22，未新增持久化规则服务、缓存或上下文|
|运行提交文件树|cbb9eb89aa80dcda7adf3609877e686aee4c25fa|

APK 随本次任务直接交付；CI产物是各自构建结果，其摘要应单独检查，不能与本地交付包混用。

## 本地验证

|检查|结果与证据|
|---|---|
|改动前基线|完整核心通过，baseline-core.txt；基线main 5eb88bb（PR #38）|
|保持行为迁移|refactor-core.txt；2,000组土地基础伤害与冻结旧引擎逐值相同|
|最终完整核心（最终APK核心相同）|verified-core-a7739af.txt，TEST_EXIT=0；含2,290条架构规则、1,391条地理/火系、1,133条导航/守备/年代断言及旧档、俘虏、运输、36/72旬长局|
|UI模型/内容|content-ui.txt；51,810条UI模型、19项内容、三条内容生成可复现检查均通过|
|Gradle完整门禁|gradle-all-gates.txt；test :core:check lint通过，GRADLE_EXIT=0|
|APK与测试APK|android-build.txt / test-apk-build.txt；构建退出0|
|最终横屏增量门禁|landscape-build.txt / ui-unit-final.txt；APK、测试APK、Lint及JVM UI测试通过，退出均0|
|Lint|本地0错误、24警告；历史警告未删除或屏蔽|
|正式包隔离|apk-content.txt；九个演练资源、DemoScenario、TestScenarios、ArchitectureFixture和独立battle模型均不在正式APK|
|API29规则/启动|08-final-apk：规则核心相同的a7739af实际安装，空档横屏、坏档保留/备份、新建年代、火矢分项/神将/威风、军乐台旬回放、重建不重复恢复、PK任务筛选通过|
|最终APK规则实装|14-architecture-retry：36600f9交付包全部architecture33门禁通过，涵盖启动/坏档备份、四组规则、预览取消、旬回放、重建及PK任务筛选|
|正式小地图/港关|07-regressions：跳转、拖动、收起、偏好、双条/主将、横竖屏、六年代入口通过；此轮在最后AI估值修正前运行，地图/导航源码与交付APK相同|
|最终全国绘图|09-performance：地图代码相同的a7739af APK，1080×1920，近远景、三种着色、有界缓存、手势、画面完整性及存档不变均通过|
|最终APK建设/列表/拖动|15-fidelity-final：36600f9交付包，1080×2400，开发地建设、草稿、分页/重建、部队拖动全部通过|
|前一运行包交叉屏覆盖|10-fidelity-2400：a7739af APK，1080×2400，开发地点击建设、草稿恢复、分页及部队拖动通过；07同时覆盖720×1280|

导航断言数比基线少9，仅因正式剧本目录循环从18项变为9项；九个移出的沙盘另由TestScenarios/ScenarioTest验证加载、哈希与旧档，未删除回归测试。

原生结果含失败与重试，完整摘要见 native-results.txt。首轮目标规则门禁还执行了既有24旬PK研究/培养完整操作，05-architecture通过；后来专项门禁只保留此次具体的任务筛选回归，默认全量SMOKE仍保留原来的24旬流程。

截图：[无档横屏](screenshots/v033-empty-landscape.png)、[火矢真实预览](screenshots/v033-fire-preview.png)、[军乐台旬结算](screenshots/v033-music-turn.png)。没有把软件模拟器耗时当成ARM真机FPS，未测ARM真机。

## CI状态与失败分类

最新运行提交为 `36600f9faa8ba5bc4abc07b93478590f8787e4df`，最新测试提交为 `caf9a2004902210ba9a4fb9cb0cc7d3feb23df28`。验收记录写入时状态如下，运行中的任务**不计为通过**：

- [Android三屏 35183982269](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35183982269)：仍在运行。
- [最终运行提交实际v0.28替换/位移 35183557022](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35183557022)：已完成，通过；测试辅助改动后的复验 [35183982274](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35183982274) 也已完成通过。
- [实际旧版体验对比 35183982339](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35183982339)：仍在运行。

本地重点验收已完成，真实v0.28 APK替换已通过；远端全量三屏和旧版体验对比以各任务最终结论为准。此后证据提交不变更应用与测试，不把运行中的检查写成通过。

已核查的失败及处理：

1. 基线 [35177570022](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35177570022) 三屏失败，核心/编译/Lint过；停在能力研究任务筛选。测试切换世界遗留taskQuery，改为清理ClientState，新局同样清理。本地完整PK流程和专项筛选已通过，不能归为纯环境问题。
2. 首轮新CI启动进程崩溃；启动旋转代码会访问尚未创建的地图，已修正并增加无档横屏实装。全国新局改后台建立。
3. TestScenarios使用InputStream.readAllBytes在API29不存在，属于新增测试兼容问题；改BufferedReader，本地导航、建设及缓存门禁恢复通过。
4. JVM PresentationTest的Gradle sourceSet缺少TestScenarios，属于构建配置问题；补齐仅test的夹具和资源，完整Gradle门禁通过。
5. v0.28升级观察器用原始v21文件字节比较v22输出；现在先验证真实输入头为21，再对两边完整迁移后的v22状态逐字节比较，保留推进一旬和重建不重复的检查，不删旧档断言。远端真实旧APK替换复验已完成通过。
6. 2400屏分页失败截图已实际显示2/34，属于快速更新后的可访问性快照滞后；保留真实触摸，改为同时检查实际显示的TextView和持久UI页号。横屏修复前APK本地2400分页/重建通过。
7. 本地软件模拟器首次System UI ANR遮住起始页；另一次720屏缓存门禁不满足Android最小双指距离。前者保留诊断，后者按原1080×1920规格复跑通过。两者与游戏崩溃、代码编译错误分开记录。

8. 8c985ce全量旧档槽位流程把M0曹操军误选为0（实际为1），修正夹具阵营参数；保留原来的曹操军、槽位切换和坏档不污染断言。
9. 同轮体验CI截图显示制造任务已创建且计数为1，但短横屏下重复列表标题挤掉了列表；OverviewUi横屏使用已有面板标题，新增点击制造任务详情回归。该项是实际UI空间问题，不标为环境故障。
10. 最终横屏专项测试在数量表单直接查找屏外输入框失败；测试辅助函数改为先真实滚动至可见输入框再填写，保留出征取消、精确数量及一次扣费断言。正式APK不因这次辅助函数更新而改变。

## 提交身份

Git命令行没有写凭证，使用授权GitHub连接分批创建树/提交并推进同名分支。每批远端树SHA与本地提交树逐一相同；映射见 commit-tree-map.json。核心本地提交0916520对应远端a7739af；最后横屏修复本地036d6d7对应远端36600f9；不是旧版本回退或只提交方案。最终本地分支也对齐远端。

规则依据、执行顺序、实际删除清单、兼容层、未核验原版差异和最小扩展示例见 [ARCHITECTURE_V0_33](../../ARCHITECTURE_V0_33.md)。
