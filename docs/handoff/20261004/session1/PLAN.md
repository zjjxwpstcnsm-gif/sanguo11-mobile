# Session 1 计划与文件所有权

目标 active；任何批次不代表全部剧本、全武将、正常流程和 ARM 已完成。

实现基点：`840030195e39cec3c0d352e010a3c3e614893ca2`。继承 checkpoint 的 4299 文件、336374894 字节逐 SHA 全等，168 固定资源通过，4 份 JNI 按原相对路径保留。基线提交 `0a6fb12`；仅封存继承差异，不含人物/剧本新实现。checkpoint 的脚本权限缺少 executable，随后按实现基点的 Git mode 恢复，文件内容不变。

独立目录 `/Users/paopao/.codex/worktrees/scenario-officer-restoration/sanguo11-mobile`，分支 `codex/scenario-officer-restoration`，可写缓存限本目录 `out/session1/`。原目录、PC 安装只读，不启动 Wine。

首批确切文件：

- `docs/handoff/20261004/session1/PLAN.md`
- `docs/handoff/20261004/session1/CONTRACT.md`
- `docs/handoff/20261004/session1/baseline-verification.json`
- `docs/handoff/20261004/session1/inherited-source-manifest.json`
- `tools/content/audit_pc_restoration_sources.py`
- `tools/content/test_pc_restoration_sources.py`
- `docs/handoff/20261004/session1/source-manifest.json`

后续允许入口（实际修改前按批次追加前镜像 SHA）：

- core 的 `ScenarioCatalog.java`、`ScenarioData.java`、`ContentRuntime.java`、`ContentCatalog.java`、`World.java`、`SaveCodec.java`；新来源 DTO/转换器与相关规则测试。
- game-api 的 `GameApi.java` 及新只读 DTO。
- game-runtime 的 `GameSession.java` 和来源/人物只读 query、相关测试。
- app 的 `MainActivity.java` 仅剧本选择、新局配置、武将文字详情方法；`OverviewUi.java` 仅武将列表与搜索文本；`ContentUi.java` 仅人物文字资料；`ScenarioFactionPicker.java` 仅剧本势力配置。均位于 `app/src/main/java/game/sanguo/mobile/`。
- 新剧本与人物 metadata 资源、数值/文字导入工具和相应验证。

不编辑 PortraitCatalog、OfficerPortrait、图像资产、SoundEffects、音频播放器、AndroidGameBridge、Unity 或 3D。不改双方共同人物资产 manifest。不并写 progress.md、STATE.json、PC_PARITY_STATUS.md。MainActivity 最终整合须按方法范围与前镜像 SHA 检查，不覆盖媒体增量。

路线：来源 manifest/实际加载链与字形/传记 → 逐字段原 serializer/getter/开局后处理 → 确定性剧本包、正常新局及统一人物 DTO → 旧 31–37 完整 Save/RNG 续行 → 独立 APK 构建、真实菜单多旬存取退出重开。全部来源差异独立保留；未知明确保留。

每次安装前核实设备锁和无人使用，备份全部保存/库/偏好，结束后读回精确恢复。不移用旧包证据。批次提交、增量 SHA、验证与缺口放本目录；最终顺序集成，不复制媒体 WIP。

## Batch 04 当前所有权

新增tools/content/inspect_pc_layered_scenario.py、inspect_pc_scenario_fields.py、test_pc_layered_scenario.py及本目录分层载入/原属性/兼容调查报告。执行原492db0完整构造、Shared与剧本同一注册表载入、4937b0读取及明确边界的后处理；不能手工回填静态表冒充加载。沿用原工具作对照，原报告与公共台账不改。未知全局、菜单设置、MOD优先级或事件未闭合时不标完整开局。新增核心/保存策略修改须再登记准确路径和前镜像SHA；当前未改变旧v33行动力策略。

追加tools/content/pc_readonly_platform.py：仅为原事件读取器提供已核实Win32文件/目录、单线程锁和内存分配平台边界。虚拟G盘映射到PC只读输入目录，写入/删除/截断接口必须拒绝；记录每个原函数要求的实际资源和SHA。不是Wine，不启动PC游戏，不替换事件/规则函数；模拟的进程路径和未执行的应用上下文明确列入边界。

追加tools/content/inspect_pc_event_resources.py与event-resources-native报告：执行679cb0安装目录发现、678550头验证与原解码/标识校验，明确停在679d9f进入配置/Documents Expansion目录之前。平台仅支持已核实PE导入及受限CRT标准接口，未知主机上下文不得默认为真实生效优先级。
