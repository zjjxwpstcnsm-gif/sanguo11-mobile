# 已核验基线｜2026-09-23

本文件是**编包时只读核查**，不是一次新的构建/运行验收。执行 Agent 仍须拉取最新远端并复核。仓库读取得到的声明和实际源码验证在下面分开。

## 1. 已读到的实时仓库状态

- main HEAD：`ac29b458325b52d6e302ca44270d16552de4ed7f`。
- PR #66 已 merged，合并消息为 architecture foundation into main。PR 源码/验证提交与证据补充提交不同，见 PR 原文。
- 正式 Gradle 依赖包含 `:core`、`:game-api`、`:game-runtime`、`com.google.android.filament:filament-android:1.56.0`；Unity 库是条件依赖。
- app 使用 Java 17；compile/target SDK 35，minSdk 26；应用 ID `game.sanguo.mobile.dev`。debug 构建默认也可能 R8/minify 且 non-debuggable，不能仅凭构建名推断是调试运行时。
- 已读 `app/src/main/java/game/sanguo/mobile/FilamentMapView.java`：真实创建 `Engine.Backend.OPENGL`，已有 terrain/ground/water 材质加载、SurfaceView、相机、纹理、对象/植被和覆盖层，以及能力门控。

## 2. 应保留的真实架构

`docs/architecture/OVERVIEW.md` 描述并由 PR #66 对应改动支持：

`GameApplication/NativeGameHost -> GameSession -> existing World rules`。

`game-api` 是纯 Java 合约；`game-runtime` 持有权威会话；`MapProjectionQuery` 输出复制的 `MapLayerData` 给原生显示。核心状态和渲染状态不要重新合并。会话换代、旧请求拒绝、保存捕获、回合提交独立于动画均是需要保护的成果。

`GridLayout` / `GridWorldTransform` 已处理源格到显示坐标的行列交错与原点，不能把常见 axial 公式直接套回去，不能再次加 source-origin 偏移。

## 3. 旧原生路线并非空白

`docs/3d/architecture.md` 包含 S01–S08 演进；`docs/3d/world-art/progress.md` 包含 S10、S11、S13 实现和未通过项。已存在的能力线索：

| 能力 | 已有线索；须源码与运行确认 |
|---|---|
| 宿主与回退 | MapHost / MapView / FilamentMapView |
| 坐标相机 | GridWorldTransform、SceneCamera、地形 ray picking |
| 连续高度 | TerrainSurface、SceneMesh、共享场/分块/局部更新 |
| 地表 | 4 层颜色与 normal/roughness、world UV、lit ground 材质 |
| 水体 | 独立 water material、形状导出的流向、保留拓扑 |
| 城港关 | SiteVisual、SiteGlb、图集、LOD、占地锚点 |
| 部队设施 | FieldAssets、UnitMotion、UnitAnimation、刚性分部件动画 |
| 环境/覆盖层 | 植被分块、SceneQuality、EnvironmentProfile、独立网格开关 |

以上类名是定位线索，不要求都位于同一包；R00 用 `git ls-files` / 搜索确认当前路径，不为了吻合本包伪造类文件。

## 4. 必须继承的缺陷，而不是清零

PR #66 交付说明明确保留：最后单独启动的截图仅有背景，不能据此确认冷启动/画面成功；某些核心测试为基线旧失败；全手工玩法和持续性能未验收；Unity Player 和 ARM64 视觉对照未完成。

S13 历史报告仍明确：台阶/材质边界、S12 美术债、物理手机和完整手工矩阵未完成。这说明“已有 S13”不等于“已经像 PC”。

将这些记录转成 R00 的待复现项。它们来自报告，不意味着当前 HEAD 必然还能复现，更不能仅凭描述定位到某一代码 bug。

## 5. 为什么本包不强行从零手写 GLES

上一轮对话的“原生 OpenGL ES Renderer”在本仓库已经有底层落点：Filament/OpenGL。画面差不自动说明底层 API 错误，重写同样不能代替建筑和地形美术。

因此本包固定目标与交付链，默认保留可运行 GPU 后端，在上层重做有问题的连续表现与资产；纯 GLES 只保留有证据的替换出口，不变成第二个长期项目。

## 6. 本轮未做的事情

编包会话没有修改远端源码、创建开发 PR、运行 APK 或验证美术。ZIP 是开发任务包，不是完成的游戏、模型素材包或已有验收证据包。

原始核查链接见 `08_SOURCES.md`。后续运行状态不能从本文件推断。
