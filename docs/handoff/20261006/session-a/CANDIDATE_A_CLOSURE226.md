# A 完成依赖闭包 226：供候选 60 串行组合

冻结包 `out/session-a/candidate-a-closure226/a-production-closure.tar.gz`（以本完整隔离目录为根），SHA `d12a04bfb87e010af442fd17f7d5dc94ae435133343f4589a1d29d3115a128b7`。32 个 A 路径，逐项前后 SHA 与字节数见 CANDIDATE_A_CLOSURE226.json。只读核验 B 固定 source archive `34655cde…`，其中 26 份原文件/独立编译依赖前态已直接从固定 tar 读取验证。新 A 文件以 absent 为前态。无 B 生产路径、无共享根 Gradle/Bridge/Unity/原四 JNI 修改；新增两份格子火 JNI 单独列 SHA，不代替原四份。

## 为什么仅 224 两文件不足

B candidate60 的完整源码 Main SHA 为 `85622dfe…`；冻结 `readonly-theme-dependencies/java/MainActivity.java` SHA 为 `fc1ed192…`，其编译采用较早的 A 完成子集。224 补丁前态是 A 当前完成 Main `a74def84…`，直接贴到任一较早前态均不正确。不能为了选项接线丢失本会话已完成的地图、内存、火、媒体和幂等呈现修复。

226 给出从两种精确 B 前态到当前 A 完成依赖及 224 两份最终后态的 A-only 文件闭包；不拿 A 当前 B 目录或 B 活动 WIP 进包。两份新选项文件来自 stage224，其余 30 路径都是已经提交的 A 完成生产输入（含两 JNI），制作前检验 app/core/API/runtime 对 HEAD 无未提交改动。原168固定资源保持；三份共享 Gradle、三份构建核验脚本及 Bridge 与固定 B source manifest 逐 SHA 相同，API/core/runtime 的612个未变依赖及196冻结Boverlay兼容检查见224。

## 顺序集成要求

1. 以固定 B candidate60 全源码/196Boverlay和其完整未跟踪资产为 B 边界，另继承 A 完成源225完整输入。读取226每项 `candidateSourceBeforeSha256` / `candidateCompileBeforeSha256`，目的文件须对应精确前态或已是后态；其他值列出冲突后逐项合并，不能盲盖。
2. 在独立完整目录应用32份A冻结后态，原四JNI原路径保留，新增二JNI按包内路径保留。B的两页/API/core/runtime只用固定候选；不要随后再运行旧 readonly-theme 子集复制以覆盖新的 Main。UiTheme/FactionColors/styles 已同 frozen theme 后态，不需重复覆盖。
3. 同一编译只选一份正式 Main/UiTheme/FactionColors 与资源，清除构建配置对旧 theme 目录的重复输入。226携带的 app/build.gradle 是A已完成构建配置，保留largeHeap真/假属性、独立格子火JNI目录与原四JNI。共享根配置不变。
4. 重新构建游戏与正常测试 APK，记录新 SHA，实际安装另做保存/库/偏好完整备份和恢复读回。224编译和旧165/176/180、候选60仅构建成绩均不能代替组合验收。B正式正常单挑验收按224真实控件步骤进行，无额外产品步骤。

## 完整可复现源

源225是 A 完成 HEAD `925748c6139b66a799e59de509c47151b00d8acb` 的完整继承快照，包含所有 SOURCE_GUARD 未跟踪源码/资源与六 JNI：

`out/session-a/source-checkpoint-current225-complete/sanguo11-mobile-source.tar.gz`，SHA `05a6c153f576089e13971e09488cff1273521c7e359b1b815d6eef5f4cbe3b7f`，653951588字节、11330文件，逐条 tar 内容与原输入 SHA 读回一致。包含224工具/补丁/依赖回执及截至223正常来源证据，不含Git数据库、构建缓存、设备备份或226本身。实际app规范生产仍为当前165游戏源码，224两文件仅stage；需应用226，不能称源225已经安装新选项。

`freeze_candidate_a_closure226.py` 从225文件清单和提交后规范输入冻结A闭包，只读取B固定tar；拒绝覆盖既有证据输出。完整源快照、增量与B候选分别有守卫；同一源不把局部动态成绩扩大为新APK或完整还原。当前5554媒体队列继续，ARM仍未验收，目标仍active。
