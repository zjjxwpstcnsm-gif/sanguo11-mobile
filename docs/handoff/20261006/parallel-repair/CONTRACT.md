# 两会话唯一文件所有权与交接契约

基线main `ef413be3653820dd6449ba7f02aa60bed5b26ef5`；这份审计/目标的后继文档提交也必须继承。完整源：`/Users/paopao/.codex/worktrees/scenario-main-closeout/sanguo11-mobile`。原目录旧HEAD与609项dirty仅保护，不能覆盖或作为新工作起点。启动时核实main及源目录状态；若main有更新，完整继承更新并记录差异，不降回本基线。

| 文件域 | 唯一修改方 |
|---|---|
| core/、game-api/、game-runtime/、data/content/、core/src/main/resources全部数值/文字权威 | B |
| app生产Java | APP_OWNERSHIP.json逐路径；MainActivity、MapHost、MapSceneSnapshot、ScenarioFactionPicker、FactionColors、UiTheme、PortraitCatalog、OfficerPortrait及音频播放器均归A |
| 16个正常规则页面 | B：FieldworkUi、ArmyUi、WarUi、ContestUi、BuildPicker、DomesticUi、GovernmentUi、DiplomacyUi、CampaignUi、StrategyUi、AbilityUi、LifecycleUi、ContentUi、DeployWizard、CargoWizard、CustomOfficerPlacementUi（.java）；没有扩展目录通配授权 |
| app资产、资源、AndroidManifest、app构建配置、原图/模型/头像/音频media manifest | A；人物文字metadata权威仍归B，不共写media manifest |
| tools/media/、tools/audio/ | A；tools/pc-runtime/归B，唯一例外tools/pc-runtime/read_portraits.c归A；共享底层工具另一方只读复用 |
| tools/content既有文件 | TOOL_OWNERSHIP.json逐路径。底层pc_readonly_platform/pc_startup_platform/原serializer等归B；A只读复用，要求变更写契约让B实现 |
| 新增文件/测试/脚本 | 开始写入前登记确切路径到各自session目录的OWNERSHIP增量；用session-a/session-b唯一命名，不抢现有测试、公共工具或夹具。既有app测试A、core/runtime测试B；共享架构/Unity黄金只读核查，变更由最终集成顺序处理 |
| AndroidGameBridge序列化、Unity、共享.gradle/根Gradle配置、4份JNI输入 | 冻结，双方不并写；需改先形成具体兼容方案/差分，由最终集成串行处理。已有桥接信息不能丢失 |
| progress.md、STATE.json、PC_PARITY_STATUS.md、旧共同契约 | 并行期间不写；各自docs/handoff/20261006/session-a或session-b，最终集成再合并台账 |

A可读B页面但不编辑；A提供UiTheme等统一可读性接口及补丁请求，B应用自己页面。B可读MainActivity/MapSceneSnapshot但不编辑；B提供新API和需接入的精确代码段，A负责这两个文件和菜单/地图适配。双方分别交付可审查增量，最终组合运行时才算跨边界闭合。不得把某文件的不同方法分别交给双方同时改。

B只读事实契约至少覆盖：完整StateToken、稳定officerId/nativeId/sourceVariant、来源与当前数值分离、格子火位置/剩余寿命/来源、设施施工与完成状态、正式命令的成功/失败码及资源变化、事件id/parentId/真实发言者/原声profile、地图scene/势力/据点/地域/日期关系。字段只在被证据支持时填值，未知显式表示。A不能按数组序号/姓名强接身份，不能按turn/owner猜BGM，不调用规则RNG重抽发言；新增视觉选择须走独立演示状态和稳定事件事实，不污染存档RNG。接口先在各自CONTRACT.md登记并提供样例，B生产DTO、A消费投影；不得新建第二份World权威或回到Activity直接提交规则。

两份完整独立目录与独立GRADLE_USER_HOME/build/out；复制最新完整继承的源码/全部资源/未跟踪源码/168固定输入/4忽略JNI并逐SHA守卫，不只git worktree裸HEAD。确认磁盘空间后再复制，不清理资产或用户文件。重复转换要求两次字节一致，按来源保留差异。

A优先emulator-5554，B优先emulator-5582；先查进程、锁、活动会话和实际serial，发现占用就换空闲serial或串行，不杀另一会话设备。每次安装前完整备份存档、库、偏好，安装后测试、最终恢复并读回逐SHA；不清数据。用户截图是真机现象，型号/API/ABI/实际APK SHA未知，模拟器通过不能替代ARM真机。真机日志收集缺失时继续独立工作并明确未验收。

完成批次只提交各自分支；禁止自动覆盖原目录/合另一方WIP/推送或抢先合main。最终顺序集成以共同基点、完成提交、准确路径/前后SHA、资源及JNI守卫、冲突清单为依据；从两方完成源生成一个新APK并单独安装回归，不拼接旧包成绩。
