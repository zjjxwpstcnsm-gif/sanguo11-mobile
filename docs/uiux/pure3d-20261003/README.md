# 纯3D UI 增量交付边界

从 AP 完整交接4078文件、329563797字节、4份 native 构建输入建立基线 c19de29。
只合入该基线之后的 UI 增量，禁止把 c19de29 的 core/game-api/game-runtime/data/unity 覆盖到最终规则会话。
修改范围为 app、专用 scripts/tools、音频资源、本批 docs；510受保护文件逐字节保持 AP。

当前地图验收/驻留候选状态见RESIDENCY_VALIDATION.md，9d51706的冻结包证据单列在VALIDATION.md。

海岸修复统一原生四分之一格的几何与材质采样，废除不兼容的粗补底。真实地形、通行、RNG不变。
MapView 与 Canvas MapOverview 仅保留在 androidTest 历史夹具，生产 APK 无这两个类描述符。
占格查询使用既有 TerrainPresentation.detail，只读全存档字节验证。
9个原创 PCM 音效及可复现合成工具见 audio-assets.json、tools/audio。PC音色绑定尚未证实。
契约与当前性能接口缺口见 CONTRACT.md。

复验使用独立 emulator-5582，独立 Gradle/AVD 可写目录，正常190测试局作为种子。
测试工具每次安装前备份内部文件、结束后还原全部原文件并删除新增文件，校验自动/手动档完整字节。
用户原设备、其他模拟器serial及PC目录未修改。不是对原用户设备8255存档的安装验证。

构建后冻结APK再运行 tools/android/run_pure3d_regressions.py。
该工具顺序执行同一对冻结APK、保存每个安装读回SHA/恢复结果/截图/帧时间/日志，行军录屏单独保存。
Pure3dInstrumentation海岸104检查与MapEditor67Instrumentation编辑器61检查分别运行，不能拿另一APK的通过代替。
默认GameSmoke已迁3D，当前回归工具包含四地图矩阵/导航/原生模型/真实旧存档/实际退出与重开。
历史版本专属像素/版本号断言保留归档，原始v57–60存档尚缺，不宣称历史全部套件全绿。
测试包必须声明当前支持的套件；旧包/缺失套件在操作设备前拒绝，防止未知套件退到普通浏览测试。
完整 architecture 测试中的旧剧本起始状态断言有既存失败；没有修改受保护模块或弱化断言。

目标仍 active：最终规则会话需把本UI增量接到其最新规则，复验骑兵权威城市七格落点与可视边界。
当前实测为 Android29 x86_64 模拟器，ARM64仅构建/资源校验，未实装到ARM真机。
几秒恢复和长主线程调度延迟仍是未通过项；真实墙钟耗时与帧样本必须继续保留。
