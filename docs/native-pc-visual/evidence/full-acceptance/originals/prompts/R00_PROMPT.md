# sanguo11-mobile｜R00 原生恢复、基线审计与后端锁定

你接手正式仓库 `https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile.git`，本轮只执行 **R00**。
这不是方案讨论、目录规划、素材提示词或独立演示任务。必须真正修改正式源码/资源，接入现有游戏运行路径，验证、构建本阶段 APK、commit、push 并创建/更新真实代码 PR。

## 先执行的约束

完整读取本包 `00_GLOBAL_RULES.md`、`01_ROADMAP.md`、`02_BASELINE_AUDIT.md`、`03_RENDER_CONTRACTS.md`、`04_VISUAL_ACCEPTANCE.md`、`05_ASSET_PIPELINE.md`、`06_TEST_AND_PERF.md`、`07_GIT_DELIVERY.md` 和本阶段全文。

读取远端 `agent/native-pc-visual` 最新提交与对应 PR；首次 R00 才从包含架构优化的最新 main 建分支。已知检查点 `ac29b458325b52d6e302ca44270d16552de4ed7f` 不是强制回退点。不要从旧 main/Unity 分支重启，不覆盖 S01–S13 和 game-api/game-runtime 成果，不重做已可用底层。

默认保留现有 Filament/OpenGL 原生后端；遵守 R00 的唯一后端决定。不要重启 Unity/Godot/Unreal，不无依据另写完整 GLES 引擎。保留 Java 唯一规则权威、现有地图拓扑/坐标/占地/数据与存档。

全程使用 `agent/native-pc-visual` 与同一个指向 main 的 PR。**暂不合并 main，不处理其他 PR，不删分支、不 force push。** GitHub 瞬时失败先读远端确认结果，再有限重试。

读取 `docs/native-pc-visual/PROGRESS.md`、`DEFECTS.md`、前阶段报告/交接和现有架构/原生美术报告。无前阶段；先完成本阶段基线核查。

## 本阶段目标

从最新正式源码恢复可见、可操作、可构建的原生 3D 游戏入口，锁定后端和证据链；不是再建立一个空渲染工程。

## 必读源码与输入

先读 02_BASELINE_AUDIT.md；实际读取最新 main/本路线分支、PR #66 的实时状态、app/build.gradle、settings.gradle、version.properties、FilamentMapView、MapHost、GameSession、架构边界/交付报告、旧 S13 报告。用 git ls-files 定位，不把旧文档当当前代码。

类名/目录仅作定位，必须查到真实文件与运行入口；不存在的路径说明并找对应职责，不能造空文件假装已有实现。

## 必须实际实施

1. 检查本地工作区、分支、远端、未提交内容与既有串行 PR；建立已知基线祖先检查和当前源码清单。盘点 S01–S13 每项：存在的正式源码/资源、实际入口、已测/未测、可复用/需修复，禁止把旧成果整体删除。
2. 实测 JDK/Gradle/SDK、依赖取得、Filament 1.56.0 matc、可用建模/贴图工具、adb/模拟器/ARM64 设备、GitHub 读写与 CI 制品能力。缺真机只标设备门槛，Unity Editor/授权不再成为原生构建依赖。
3. 从正常启动进入正式剧本和存档，切到原生 3D，重现并定位 PR #66 提到的纯背景/冷启动缺口。分开判断会话是否载入、相机/投影、mesh 队列、Surface 首帧、色彩/着色器、截图方法，不预判根因。
4. 修复实际启动/入口/Surface/资源或构建阻塞；建立实际地图首帧可见性和后端诊断。禁止依靠强制进入 demo 场景、隐藏失败提示或只渲染背景骗过检测。
5. 默认锁定现有 Filament/OpenGL，保留固定版本与历史兼容门控。只在可复现底层阻塞满足全局规则时执行纯 GLES 候选；写明失败证据、复用范围、迁移成本和真实地图可运行对比，不能在后续继续摇摆。
6. 隔离 Unity 的默认入口/构建路径，不删除已合并架构和可用历史工程；标准原生 assemble 不应寻找 Unity Editor/Player。若本来已可独立构建，增加明确 profile/入口诊断与自动防回归，不无意义重写。
7. 建立 reference/scenes 索引：选真实区域、稳定剧本/存档、相机和截图命名；启动 PC 参考收集，缺失字段明确记录。后端/设备信息进入 APK 可查看诊断。
8. 把基线失败真实跑一遍并分组，写 DEFECTS；修本轮受影响阻塞，其余留明示债务。完成真实源码修复后创建本路线首个代码 PR。

## 必须验证

- 正常冷启动、新开剧本、读既有档，实际看见地形/至少一个据点和部队，拖动/缩放可以改变视野；运行日志与录屏同属本阶段 APK。
- 2D/3D 切换前后权威状态一致；无新增 World/无重复命令。
- 干净环境或精确 SHA CI 构建 app、instrumentation、lint；继承失败单独记录，不改退出码。
- 后台恢复/Activity 重建能重现已保存场景；至少一次 Surface 截图验证非纯背景且人工确认确实是地图。
- 最终远端 SHA、PR、APK source SHA/哈希一致，原生包不依赖 Unity Player。

仍需执行全局要求的受影响玩法/架构回归、实际 Android 构建与本阶段图形运行。继承失败和新失败分开，不能删除断言、跳过受影响用例或把 NOT_RUN 写为 PASS。

## 通过门槛

V0：实际正常游戏地图可见且可操作。无运行环境可继续提交环境/源码修复，但阶段为 BLOCKED/PARTIAL，不通过 V0；截图纯背景仍失败。后端决定必须锁定且具理由。

构建成功≠运行成功；模拟器通过≠真机通过；有模型≠美术达标；截图有颜色≠PC 对照通过。缺实际参考/资产/设备时继续完成可做工作，但状态诚实保留 PARTIAL/BLOCKED，不把计划当完成。

## 必须交付

本阶段 APK/证据；BACKEND_DECISION.md、BASELINE.md、ENVIRONMENT.md、SCENE_INDEX.json、REFERENCE_INDEX.csv、基线测试矩阵，以及 R00 报告/交接；至少一项正式源码或正式运行配置改动。

交接文件落在 `docs/native-pc-visual/reports/R00.md` 和 `docs/native-pc-visual/handoffs/R00.md`，同步更新 PROGRESS/DEFECTS/资产清单。大制品用 CI artifacts 或实际可下载附件，避免 Git 膨胀。

本轮最终回复前，把全部必要生产源码/资产/测试实际 commit/push，复读远端 HEAD/PR。交付 source SHA 精确对应的独立 APK、SHA-256、应用 ID/版本/ABI、实际后端、证据包、测试与未完成项。若末尾只补报告，分别注明 source SHA 与 evidence-only HEAD；修改生产代码后必须重建。

无法构建/安装/推送时给出原始错误和实际完成范围，不伪造 APK/链接/远端提交，不只回复一份下一步计划。

## 本阶段特别禁止

禁止把 R00 做成纯审计文档；禁止直接从旧 Unity 分支当新基线；禁止未经实测升级 Filament/Java/SDK；禁止把旧 35 MB APK 改名作本阶段交付。
