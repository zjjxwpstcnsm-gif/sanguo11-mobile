# sanguo11-mobile｜R01 渲染边界、资源所有权与生命周期补齐

你接手正式仓库 `https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile.git`，本轮只执行 **R01**。
这不是方案讨论、目录规划、素材提示词或独立演示任务。必须真正修改正式源码/资源，接入现有游戏运行路径，验证、构建本阶段 APK、commit、push 并创建/更新真实代码 PR。

## 先执行的约束

完整读取本包 `00_GLOBAL_RULES.md`、`01_ROADMAP.md`、`02_BASELINE_AUDIT.md`、`03_RENDER_CONTRACTS.md`、`04_VISUAL_ACCEPTANCE.md`、`05_ASSET_PIPELINE.md`、`06_TEST_AND_PERF.md`、`07_GIT_DELIVERY.md` 和本阶段全文。

读取远端 `agent/native-pc-visual` 最新提交与对应 PR；首次 R00 才从包含架构优化的最新 main 建分支。已知检查点 `ac29b458325b52d6e302ca44270d16552de4ed7f` 不是强制回退点。不要从旧 main/Unity 分支重启，不覆盖 S01–S13 和 game-api/game-runtime 成果，不重做已可用底层。

默认保留现有 Filament/OpenGL 原生后端；遵守 R00 的唯一后端决定。不要重启 Unity/Godot/Unreal，不无依据另写完整 GLES 引擎。保留 Java 唯一规则权威、现有地图拓扑/坐标/占地/数据与存档。

全程使用 `agent/native-pc-visual` 与同一个指向 main 的 PR。**暂不合并 main，不处理其他 PR，不删分支、不 force push。** GitHub 瞬时失败先读远端确认结果，再有限重试。

读取 `docs/native-pc-visual/PROGRESS.md`、`DEFECTS.md`、前阶段报告/交接和现有架构/原生美术报告。读取 R00 的远端交接及所依赖门槛；先修会阻塞本阶段的前置问题，再执行本阶段。

## 本阶段目标

在既有会话架构上收敛原生显示职责，建立后续连续地形可安全增量更新的运行基础。

## 必读源码与输入

读取 R00 后端决定和缺陷、docs/architecture/OVERVIEW.md/MODULE_RULES.md（若存在）、MapHost、MapSceneSnapshot、MapLayerData、MapProjectionQuery、FilamentMapView 的正式调用链。

类名/目录仅作定位，必须查到真实文件与运行入口；不存在的路径说明并找对应职责，不能造空文件假装已有实现。

## 必须实际实施

1. 沿用 GameSession 与现有不可变投影，把场景提交、资源缓存、Surface 生命周期、相机输入等不同职责从单体 renderer 有限拆出；用真实调用替换，不生成一排空 service/interface。
2. 明确并测试 session/generation/map/visual/asset revision；世界替换时旧 mesh/解码任务、事件订阅和回调全部失效。快照合并可以 latest-wins，权威展示事件保留实际序列与取消语义。
3. 现有 Filament owner thread 不随意迁移；CPU 生成/解码使用有界执行队列，上传和销毁回原所有者；纯 GLES 分支依其已锁定线程模型。记录 CPU buffer 何时可以复用/释放，不能在异步上传结束前回收。
4. 资源生命周期覆盖创建中失败、Surface 重建、view detach、pause/resume、显式退出、world replacement。禁止同一地图两个常驻帧循环/两个持续绘制的后台视图。
5. 为 mesh/texture/material/entity 建立可观测的 live/cache/pending 计数和内存估计；限制缓存，引用存活对象不能驱逐。诊断不能把估计内存称作实测 GPU 占用。
6. 保留并增强 2D 安全回退和上次异常启动保护；区分用户取消、进程死亡、native crash 和 Java 初始化失败。错误可诊断，不能吞掉异常后显示永久 loading。
7. 增加真实边界测试，防止 renderer 新增 World 写入或把引擎类型塞进 game-api；已有直接 core 白名单只收敛不盲目扩大。

## 必须验证

- 2D/3D 反复切换 20 次与前后台 20 次，只有一个有效渲染循环，计数回到稳定区间。
- 快速读档/新剧本/退出再进时故意延迟 worker，旧任务不得回灌；无 use-after-free 或 native crash。
- 相同命令/RNG 的 2D/3D 权威状态与基线一致；renderer 不能新增规则调用。
- 资源缺失/解码失败/构造中失败路径能释放部分资源并回到可玩界面。
- 受影响主机测试、APK 构建和实际地图渲染回归。

仍需执行全局要求的受影响玩法/架构回归、实际 Android 构建与本阶段图形运行。继承失败和新失败分开，不能删除断言、跳过受影响用例或把 NOT_RUN 写为 PASS。

## 通过门槛

运行与所有权门槛通过；允许保持 R00 美术，但不得因拆分类破坏已有模型/水体/覆盖层。物理机未测必须单列，不能用计数稳定证明所有 native 泄漏都不存在。

构建成功≠运行成功；模拟器通过≠真机通过；有模型≠美术达标；截图有颜色≠PC 对照通过。缺实际参考/资产/设备时继续完成可做工作，但状态诚实保留 PARTIAL/BLOCKED，不把计划当完成。

## 必须交付

正式拆分/接入源码和边界测试、资源/线程所有权说明、20 次生命周期日志、实际地图 APK/截图、R01 报告及交接。

交接文件落在 `docs/native-pc-visual/reports/R01.md` 和 `docs/native-pc-visual/handoffs/R01.md`，同步更新 PROGRESS/DEFECTS/资产清单。大制品用 CI artifacts 或实际可下载附件，避免 Git 膨胀。

本轮最终回复前，把全部必要生产源码/资产/测试实际 commit/push，复读远端 HEAD/PR。交付 source SHA 精确对应的独立 APK、SHA-256、应用 ID/版本/ABI、实际后端、证据包、测试与未完成项。若末尾只补报告，分别注明 source SHA 与 evidence-only HEAD；修改生产代码后必须重建。

无法构建/安装/推送时给出原始错误和实际完成范围，不伪造 APK/链接/远端提交，不只回复一份下一步计划。

## 本阶段特别禁止

禁止重做 World/GameSession；禁止只新增 wrapper 不替换生产路径；禁止给 Filament 套 GLSurfaceView 双重管理；禁止用无限队列/全量复制掩盖并发问题。
