/goal
目标：继承最新完整main，修复用户反馈的游戏地图黑字、新局势力黑字、放大地图Java OOM和格子火缺失；全局核查并补齐正常Android界面、3D地图与媒体表现，交付正常流程可安装APK，不停在静态展示或资源导出。

当前本地main基点：ef413be3653820dd6449ba7f02aa60bed5b26ef5；完整已合并源码：/Users/paopao/.codex/worktrees/scenario-main-closeout/sanguo11-mobile。首先阅读该目录docs/handoff/20261006/parallel-repair/{AUDIT.md,CONTRACT.md,APP_OWNERSHIP.json,TOOL_OWNERSHIP.json}与三张screenshots；核查git status、README、适用AGENTS.md、progress.md、docs/architecture/MODULE_RULES.md、PARITY_UI_CONTRACT.md、docs/PC_PARITY_STATUS.md及20261004/session1、session2最新收尾。继承审计目标后继文档提交和任何已完成的新main，不回退基点。PC只读参照：/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版；禁止Wine、网上表格或记忆替代原真值，外部Windows Documents/Expansion只有未知，不再要求另一副本。

建立自己的完整继承隔离目录，保留全部未跟踪源码/资源、168固定构建输入及4份忽略JNI，逐SHA核验，独立Gradle/build/out缓存。原/Users/paopao/workspace/sanguo11-mobile旧HEAD和609项dirty不得reset、清理、覆盖或从旧HEAD另起。现有磁盘约2GiB余量，复制前核实，不通过删用户资源解决。并行严格按CONTRACT唯一文件所有权；公共台账不并写，计划、契约、增量、验收与未知只写自己session目录。持续自主推进，定期中文报告；完成批次在自己分支提交，交付共同基点/提交/确切路径/前后SHA/JNI资源守卫与冲突说明，不合另一会话WIP。

分支codex/map-ui-media-repair，记录docs/handoff/20261006/session-a。你拥有APP_OWNERSHIP.json内A路径、app资产/主题/媒体/地图渲染及对应A工具；尤其MainActivity、MapHost、MapSceneSnapshot、ScenarioFactionPicker、FactionColors、UiTheme只能由你改。B拥有16个命令页面及core/API/runtime/数值文字。你不能修改这些B文件；主题机制由你提供，让B在自己页面应用。AndroidGameBridge序列化/Unity/4JNI冻结，按契约最终串行集成。

优先完成用户阻断，再补齐其余已记录媒体缺口：
1. 修复所有来源全部势力槽配色耗尽返回0的问题。主机审计16源有13源有效势力零色，SCEN007/014含韓玄/韓遂/陶謙。全局审计地图势力/部队/城市关港/设施标签、选中/禁用/富文本/列表/弹窗/新局与存档页文字，深底可读；保留势力辨识和原图。不要只改三张图里的姓名。颜色稳定、alpha有效、任意势力数可用；文案可读性与势力填色分别处理。正常触控、选中提示/金额/错误/方向/完成态也检查。
2. 定位并修复新局地图缩放的Java OOM：截图384MiB堆满，354832字节分配失败，具体栈未知。记录设备/API/ABI/APK SHA/Java堆限制，分析Java/native/GPU峰值和分配栈、资源/World/异步准备/多MapHost生命周期。现有picker已release、TerrainSurface已有有界缓存，不预设泄漏根因。复现全部剧本势力预览→新局/取消、全图↔近景反复缩放平移、快速切换/重试、Home/旋转/退出重开/切档。内存预算与缓存/资源释放必须有证据；largeHeap、吞OOM重试或永久关闭3D不算修复。操作失败保留存档，不能以恢复页出现当稳定。
3. 恢复PC地图格子火及关联状态表现。FilamentMapView.animateEffects的pcMap分支跳过所有旧火粒子；恢复原资源/控制器/材质/位置/寿命与性能预算，区分格子持续燃烧、火计演出、火球火种火船连锁、设施施工/受损/完成/拆除。已有原资源不足就明确取证缺口，不把旧工程替代包装成原版。从B只读FireState/设施/事件真值展示；不由渲染重算成功/伤害、造火或抽规则RNG。暂停/低画质/缩放/读档仍能辨认真实燃烧状态，到期/灭火及时消失。
4. 全局媒体闭合：复验最新四字形身份metadata和所有16来源头像/年龄/形态/caller连接，不沿用旧652/4未知计数；原普通/全屏像素、裁切/色彩/时序/生命周期按实际入口验收。接正常地图BGM、真实人物voice、未命中58及普通攻击/行军/建设等原事件声，消费B的StateToken/真实场景/事件id/发言者/profile契约，未知不猜，不按姓名/ordinal接资源。长曲整首连续性原0.995门槛保留，捕获−22失败如实定位。保留33技巧、1取消、49/78命中等现有效果，九种工程合成不得改标原声。审核所有source-map条件是否屏蔽了本应存在的状态、选择和演出，并逐项记录证据。

验收：真实菜单16来源新局/势力切换→多个势力人物和地图→正常移动/火计/军事施工→多旬→保存读取→退出重开；特定操作必须人工可达，不用直接造snapshot或算术测试替代正常路径。B完成规则后接其冻结增量再测火/建设/单挑舌战的表现。A优先5554，先核实独占；每次安装完整备份全部保存/库/偏好，最终恢复并读回SHA，不清数据。建立与用户问题相符的ARM真机长流程证据；模拟器和ARM结果分开，缺设备就明示待验收。每个新APK独立构建/实际安装/记录SHA，最新组合旧包7398ca4d…仅构建未装，不沿用Batch26/媒体31的成绩。要求操作前后完整Save/RNG/StateToken不被纯呈现改变。交付APK、完整源码/可复现工具、逐问题复现根因和修复证据、内存峰值/截图视频、媒体覆盖/未知、测试矩阵和未完成项；与B交付后由最终集成重新生成组合包验收，不以局部通过宣布全目标完成。
