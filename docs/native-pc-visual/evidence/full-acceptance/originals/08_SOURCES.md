# 一手资料与编包核查来源

核查日期：2026-09-23。链接用于开发时复查；版本和仓库状态可能继续变化。本包没有复制/附带 PC 游戏资源。

## 仓库原始资料

以下链接固定到编包时 SHA，执行时另读远端最新版本。

- main/合并历史：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/commit/ac29b458325b52d6e302ca44270d16552de4ed7f
- 架构 PR #66：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/66
- 架构说明：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/blob/ac29b458325b52d6e302ca44270d16552de4ed7f/docs/architecture/OVERVIEW.md
- 正式依赖/版本约束：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/blob/ac29b458325b52d6e302ca44270d16552de4ed7f/app/build.gradle
- 正式原生渲染源码：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/blob/ac29b458325b52d6e302ca44270d16552de4ed7f/app/src/main/java/game/sanguo/mobile/FilamentMapView.java
- 旧原生路线与线程/资产约束：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/blob/ac29b458325b52d6e302ca44270d16552de4ed7f/docs/3d/architecture.md
- 旧连续地图/美术进度和债务：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/blob/ac29b458325b52d6e302ca44270d16552de4ed7f/docs/3d/world-art/progress.md

这些报告包含历史状态；PR #66 已合并不等于报告中所有缺陷已经关闭。编包者没有重新跑其中测试。

## Android / 渲染一手资料

**A01 Android OpenGL ES**
https://developer.android.com/develop/ui/views/graphics/opengl/about-opengl

支持依据：Android 框架提供 GLES 与 GLSurfaceView；设备是否具备 GLES3 需要实现/运行时检测；GLES3 的 ETC2/EAC 与可选 ASTC 不能混同。本包对画面质量、包体和性能的目标是项目设计，不是文档保证。

**A02 GLSurfaceView.Renderer**
https://developer.android.com/reference/android/opengl/GLSurfaceView.Renderer

用于纯 GLES 候选的线程与上下文恢复核对；不意味着 Filament 现有 Surface 必须改成 GLSurfaceView。

**F01 Filament 官方仓库**
https://github.com/google/filament

支持依据：Android Java/JNI API、OpenGL ES 后端、Android Maven 库、自定义材质、gltfio 与离线工具；runtime 和 matc 需要匹配版本。官网最新示例不等于本项目应升级版本。

**G01 glTF 2.0 规范**
https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html

用于格式、坐标、accessor、材质与动画边界核查。项目可以支持受控子集，但应明确离线转换/拒绝策略。

**G02 Khronos glTF Validator**
https://github.com/KhronosGroup/glTF-Validator

用于格式验证，不证明模型外观像 PC 版，也不证明符合项目自己的运行时子集。

**P01 Android 游戏系统跟踪**
https://developer.android.com/games/optimize

用于区分 CPU/GPU/帧呈现并采集真实性能；工具版本/设备支持以实际环境为准。

## PC 版参考

**K01 开发者采访：三国志11制作人**
https://www.4gamer.net/specials/0601_koei_int/0601_koei_int_03.shtml

采访涉及 KOEI 程序持续演进、彩墨风格、Shader 与 Hex 逻辑。它不是完整渲染源代码，也不足以证明某种 terrain mesh、splat map、植被 billboard 或雾遮挡 LOD 的具体内部实现。本包选这些现代实现方法是工程设计，不写成已确认的原版内部事实。

## 引用原则

参考图登记其具体原始页面，不只写“网上找的”。第三方博客可提供线索，关键 API/资源许可/版本要求回到官方说明核实。用实际 APK 截图来证明本项目效果，不用外部示意图代替。
