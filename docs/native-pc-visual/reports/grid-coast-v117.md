# R18 后用户反馈专项：网格与岸线 — v117 / PARTIAL

本轮只处理三项画面反馈，没有启动 R19 或重跑整套阶段开发。正式修复和新 APK 已交付，但局部对照不能关闭全国运行、完整触控、PC 美术或 ARM64 真机门槛。

## 基线与正式接入

- 实时 main：`ac29b458325b52d6e302ca44270d16552de4ed7f`，已合入架构 PR66；它是串行分支的祖先。
- 续作输入：`ebef624899c25f381101dfaa095cf4057b7f3d1f`，保留 R00–R18 及此前全部架构、队列、缓存、材质和恢复成果。
- APK 源码：`7a6b2a354ad93db30602d59c2632777243d55801`，源码树 `754c4999277d3731d7d8462b09a10b2780629b9c`。
- 分支：`agent/native-pc-visual`；[PR67](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/67) 继续 Draft/open，不合并 main，不 force push。
- 正式调用链仍为 MainActivity → MapHost → FilamentMapView → MapSceneSnapshot.Ground / SceneMesh / TerrainSurface；后端仍为 Filament 1.56.0 OPENGL。
- `core`、`game-api`、`game-runtime`、`data`、`unity` 和全部 300 份正式资源无差异。保留 GameSession 唯一权威，不重启 Unity，不改变地图数据、通行规则、六邻接、格中心或规则 RNG。

## 三项实际修改

| 反馈 | 正式修改 | 已验证范围与限制 |
|---|---|---|
| 网格不明显 | 普通格线使用 2.3dp 深色底线和 1.05dp 浅色细线；近中景保持对比，span36–48 渐隐，保留网格开关 | API29 同镜头原 APK 对比可见，草地与水面均明显；线宽风格仍可由用户主观验收 |
| 山体等不应画网格 | Ground 保存不可变可通行显示掩码，排除 MOUNTAIN、NON_NAVIGABLE_WATER、VOID 和 NationalMap.restricted；普通格线沿用遮挡缓存 | 山体内部普通格线消失，山道/栈道和可航水面保留；不是所选部队本回合的可达范围。编辑器仍可显示合法的不可通行格以便修改 |
| 岸线阶梯锯齿 | 共享半格岸线图执行两次有界平滑，再传输原地形顶点、材质坐标；近远 LOD、格线/领土边界、贴地高度及射线拾取使用同一映射 | 最大单轴位移 0.1875 格，中心固定，三角形数量不增加；局部实拍直角阶梯显著减轻。四路交叉及 VOID 接触点固定，地图外缘仍可能呈阶梯；未改变像素级抗锯齿配置 |

岸线仅改变显示几何。原水陆分类与带宽样本先在规范坐标采样，再随三角面传输，不用修改地形格来伪造曲线。细分三角面的正向、共享边界、LOD 边界、反向拾取与中心均有独立断言。单格岛、窄河及港口中心不被平滑越过。

## 构建和独立安装身份

| 项目 | 值 |
|---|---|
| 应用 ID | `game.sanguo.mobile.dev` |
| 版本 | code117 / `0.117.0-native-grid-coast` |
| APK | `sanguo11-mobile-v117.apk`，37,387,030 bytes |
| SHA-256 | `48ec6ad14cf9f2eefaf020566471dac5292bf3850c33b411262ce89b47f6f185` |
| ABI | arm64-v8a、armeabi-v7a、x86、x86_64 |
| 开发签名 SHA-256 | `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24` |
| 构建/lint | PASS，精确 clean SHA；DEX 内完整源码标识、16KB 原有门禁和全部 300 资源再次核验 |
| CI | [36425723495](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36425723495) |
| 构建 artifact | [10971587883](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36425723495/artifacts/10971587883) |

```sh
./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PcandidateSource=7a6b2a354ad93db30602d59c2632777243d55801 -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true
adb install -r sanguo11-mobile-v117.apk
adb install -r test.apk
adb shell am instrument -w -e phase candidate -e source 7a6b2a354ad93db30602d59c2632777243d55801 game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeGridCoastInstrumentation
```

安装探针从 `pm path` 拉回已安装 base.apk，与被测 APK 逐字节 `cmp`，再记录 SHA-256。正常启动组件是 `game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity`。新包没有携带测试夹具或另建演示 Activity；夹具只在 androidTest 中生成并经正常 auto.sg11 恢复路径进入游戏。

最终证据提交只允许文档/证据差异；准确远端 HEAD 见 PR 最上方及独立证据包 `remote-confirmation.json`，不是本 APK 的源码 SHA。

## 主机验证及旧断言保护

| 检查 | 结果 |
|---|---|
| 网格/岸线专项 | PASS 2,158,535；两种布局 + 正式全国地图，中心、掩码、正向三角形、接缝、面积、拾取、完整 Save/RNG |
| 全国几何 | PASS：38,949 格，19,405 个顶点条目移动；总面积仍为 38,949；单轴最大位移 0.1875 |
| 孤立直角转折 | 90° → 46.397181°；是几何度量，不代替美术验收 |
| TerrainSurface / TerrainMaterialField | PASS 902,230 / 4,978,894 |
| R02 / R05 / 水域地貌 | PASS 399,124 / 55,818 / 803,860；R02 含 1,548 个独立 mesh/ray 样本；35 港口、水连通与完整存档保持 |
| R08、R01、R12、架构 | PASS；R12 为 395,124 |
| 完整 core | FAIL：输入与候选独立复现 `CoreTest.logistics:75 / AI uses deployment commands`，原断言和规则不变 |
| 原 R05 船运规则回放 | FAIL：输入与候选独立复现 `AI completes embark, sail and land`，日志保留；不把 continue-on-error 后 CI 的绿色步骤当规则通过 |

岸线有意改变 XZ，旧完整场景字节 hash 不能直接描述新视觉。没有改写旧预期 hash：冻结输入提交的 SceneMesh/TerrainSurface/原 NativePreviewWorkTest，配现有 v105 材质控制，仍执行全部旧完整字节断言；新实现继续验证相同规范几何 hash，以及除随地面更新的高度/法线之外的旧场景流。原开发失败日志保留在证据包，最终正式门禁通过。

## 实际 APK 画面与运行

Pixel2 x86_64 模拟器，4GB，API29/API35，SwANGLE/SwiftShader，Filament OPENGL；这些不是 ARM64 手机性能证明。以旧 v116 原包（源码 `140ea58b2ae219960a62ab3ff05bd632a8b6b300`、SHA-256 `4ee59d43b91937bcda35bfc3b7b95ba504ba2bda4d54151d097fff4b5d3d37f7`）作对照，使用同一新版 test.apk。

| 执行 | 结果 |
|---|---|
| API29 旧版 | PASS 60 checks；12 张 UI + 12 张 Surface，完整 authority/RNG 前后相同 |
| API29 v117 | PASS 763 checks；同样 12 组，原 120 秒 ready 不变；已安装 APK hash 相符，完整 authority/RNG 前后相同 |
| API35 首轮旧版 | PASS 163 checks；12 组画面及存档一致 |
| API35 首轮 v117 | FAIL：初始 ready 超时，submitted3 / beginSkipped496 of499 / pending2；未到达镜头循环，0 组候选对照，保留装载背景失败图 |
| API35 同 APK 原门槛复跑 | 旧版 FAIL（pending2 / submitted3 / beginSkipped507 of510）；v117 PASS 874 checks，12组UI/Surface及完整authority/RNG前后一致。两轮结果相反，稳定性仍FAIL |
| 正式全国原 R05 路径 | FAIL：同源码 [run36425727830](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36425727830)，原 120 秒 ready，pending29 / submitted3 / beginSkipped583 of586；后续船行/画面对照未到达 |
| ARM64 真机 | NOT_RUN，访问 BLOCKED：WIF `unauthorized_client` / `The given credential is rejected by the attribute condition.`；额度预检未到达，设备提交为0 |

局部夹具为合法 22×16 存档，从正常 MainActivity/MapHost 切到 3D。相机 API 设定 span4/8/16 × yaw0/90 × grid off/on，tilt55、MEDIUM。每次通过原 ready 后才暂停帧循环采集 Surface/全屏，再恢复；不是离线渲染，不以静帧证明动态流畅。平移/缩放由相机 API 驱动，**本轮没有执行完整手指拖动/双指缩放/出征/攻击/回合/SAF 链**。

API29 原始图及API35复跑候选图确认：新版草地/海面格线可读；山体内部不再分格，中央合法山道保留；关闭网格后的水陆阶梯转折减轻，孤岛角变圆。地图外缘 VOID 接触处仍保留台阶、近景仍是有限折线；材质和山脉/建筑整体美术未在本轮重制。没有可配对 PC 参考图，视觉线为 `REFERENCE_MISSING`，不得称为 V1–V4 或 PC 一致性通过。

推荐对照（路径相对独立证据包）：

- `api29/{baseline,candidate}/images/grid-coast-{baseline,candidate}-s8-yaw0-gridtrue-ui.png`：格线与山体过滤。
- `api29/{baseline,candidate}/images/grid-coast-{baseline,candidate}-s4-yaw90-gridfalse-surface.png`：岸线转角与孤岛。
- `api29/candidate/images/grid-coast-candidate-s16-yaw90-gridtrue-ui.png`：较远视角与合法山道。

API29两份及API35两轮共四份MP4可解析；录像上限180秒，不保证覆盖每次测试全程（API35首轮旧版已截断）。全国 R05 的唯一 MP4 缺 moov，标记为不可播放失败原件，不能当运行证据；PNG、日志完整保留。

## 缺陷与后续约束

本轮技术/局部画面结论为 PARTIAL。全国 ready、API35 装载稳定性、完整触控、编辑/旧档/SAF、所有驱动与 30 分钟热稳继续开放；API35两次呈旧版PASS/候选FAIL、旧版FAIL/候选PASS，独立复现双方装载不稳定；不能据复跑通过关闭稳定性、归为纯环境或宣称无新增回归。核心规则历史失败独立记账。本轮不修改这些规则、不提高 ready 时限、不重启 Unity、不合并 main，也不自动开始下一阶段。

API35复跑job `108947587742`，artifact `10971924768`，ZIP SHA-256 `c2a95886fd7f037c7c5358557c04740430a36adec0427fbe63cb729565482c6d`；首轮失败artifact `10972276456`保留。

资产清单、每份原始证据 SHA-256、构建/设备 artifact 身份见 `../evidence/grid-coast-v117/` 和独立 ZIP。所有未完成项继续阻塞完整验收与发布。
