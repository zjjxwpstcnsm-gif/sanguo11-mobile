# 第十八批：3D行军真实选点与回合耗时口径

## 基线与范围

独立UI目录，基点c03174e，冻结AA396/v6 init和402项companion。原目录与5554只读，规则/剧本/武将数据/内容工具未改。现有地图与资源作为美术基线，不启动Wine。

本批产品改动仅为MainActivity、NativeGameHost、TurnWork的计时和展示。NativeGameHost在beginTurn前记录单调时钟；TurnWork另存wallStartedAt，保留原startedAt/activeMillis及8秒演示预算逻辑。结束统计totalMillis为从请求结算到演示结束的真实经过时间，包含主动暂停和后台；activeMs单独记日志，计算/存档统计仍独立。战报入口不再把扣除暂停的计时写成整个回合耗时，摘要明确两种口径。没有改规则计算或重复提交状态。

## 3D行军失败核实

p17-25保留的失败中，测试在行军命令布局前选目标，后续投影坐标变化；screenHex横坐标用零地形高度，纵坐标用真实高度，二者不一致。测试现在先通过实际菜单进入所需渲染器、先点行军，再在当前可见区域选择目标。3D坐标统一使用地形高度，排除遮挡面板/导航图并通过只读地形拾取检查映射为同一地块，最后仍实际注入DOWN/UP与完整权威断言，不调用onTile或直接提交规则。

第17批生产app03保持原样，p18-02原生行军完整74项通过：实际出征、非法/合法目标、返回与取消、双击唯一行军/回合、真实Home与恢复新帧、暂停/加速/跳过、横屏战报详情、完整读档。故这条此前失败的选点路径来自测试投影和时序，不把它描述为已经修复的新产品缺陷。更多3D地形/遮挡/目标组合仍待验。

p18-01未启动测试：独立5580离线，安装与runner拒绝，记录保留。按原host/no-window/2048MB配置启动同一个san11-uiux AVD，未清数据；运行前root504/AA396/companion402/638资源和三用户文件hash均一致。5554未操作。

## 新计时验证

新增真实暂停等待与独立elapsedRealtime观察区间，检查total涵盖已观测暂停与实际计算、未超出测试观测的请求到完成时段；检查ClientState摘要的暂停口径与实际顶部战报入口。摘要当前存于UI恢复状态，战报中心展示权威历史记录，没有新增耗时弹窗。回合捕获/唯一revision、暂停/后台、加速/跳过和恢复存档沿用完整实际路径。

同一最终app/test-run01：p18-03原生77项、p18-04 2D 73项，共150项检查通过。p18-02旧包74项单列，不计入最终150。两条路径都完成实际出征、目标预览/失败禁用/取消、行军与下一旬唯一提交、真实Home、前台焦点/原生新帧、暂停加速跳过、战报检索/横屏详情、正常槽位3完整读档恢复。检查数不是独立流程数。

| 运行 | PID | 总经过时间ms | 测试观测暂停ms | 运算ms | 演示activeMs |
|---|---:|---:|---:|---:|---:|
| p18-03 原生 | 13370 | 21674 | 18610 | 10821 | 2175 |
| p18-04 2D | 22853 | 15855 | 12251 | 5407 | 2891 |

运算与演示暂停可同时发生，上表不同列不能相加。旧p18-02 total=2219而compute=12719，旧total是扣除暂停的计时；新total已是实际经过时间，日志另加activeMs。字段意义变化必须带批次解读历史数据。3D Home422ms、前台恢复4763ms，2D Home203ms、前台恢复4868ms，包含系统切换和测试等待，仍不够流畅，不能当纯绘制耗时。不是受控提速基准，没有性能验收结论。

## 截图与录像

- [改前入口](evidence-18/native-before-timing.png) / [原生改后](evidence-18/native-final-result.png) / [2D改后](evidence-18/flat-final-result.png)
- [3D非法目标](evidence-18/native-invalid-target.png)、[实际行军预览](evidence-18/native-route.png)、[横屏战报](evidence-18/native-report-landscape.png)、[正常读档](evidence-18/native-restored.png)
- [2D横屏战报](evidence-18/flat-report-landscape.png)、[2D正常读档](evidence-18/flat-restored.png)

`out/uiux/iteration-18/p18-02/03/04`保存完整实际录像92.865/83.315/61.211秒，各顺序解码3帧，末段实际采样已目视检查；直接截图也已检查。`videos.json`保留原始PTS长度及宿主录制区间，decoded-frames.json保留请求/实际PTS，不宣称逐帧无丢帧。p18-01离线失败和p17-25原失败均保留。

## 交付与待办

最终包均version159/source c03174e+dirty，x86已安装5580整包回读一致；ARM64只构建与静态核验，无真机或总体流畅性结论。

| APK | 字节 | SHA256 |
|---|---:|---|
| app-uiux-run01.apk（x86_64） | 82271732 | 50d9509e7b34757294c64ab8243601194489fc602c08213f58f58670658b9d5c |
| app-uiux-arm64-run01.apk | 82214573 | e0b9b31f620f91c338d1a60b92e8229b20ec63ede926a7eb1c0a5d0db6220354 |

audit-final.txt/architecture-final.txt及包内容检查通过：受保护root504、AA396、companion402及tar，每包638初始资源和对应原生库逐SHA一致；正式剧本存在，测试剧本和隔离战斗引擎不在正式包。

auto/manual3均185898B、SHA256 `02ddb3d44d98fbebe763a82551b5cb5eb68da6e70b6087568c0a73943bde5d69`；自建武将库178B、SHA256 `724081dc4bc2185542a7446fd16f2c32eaff4130033c2b411e50e634deb7f07e`。原文件保护、原状态/RNG完整恢复，不保证mtime不变。测试恢复开始时的2×速度，没有清除任何用户数据。

完整source归档与source-manifest.json/ui-increment.patch、402项companion及v6 init配套复现。仅取c03174e之后三个app文件、Instrumentation和docs/progress增量，不把冻结旧core覆盖玩法新规则。

后续仍有三指与部队拖放跨面板、进程销毁/冷启动、长时间资源稳定、舰船生产/任务管理/高级外交和首次权威预览延迟。由玩法会话仅集成c03174e之后UI增量并在其新规则上重建验证；全goal继续。
