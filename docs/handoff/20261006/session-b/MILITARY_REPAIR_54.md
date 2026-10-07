# 军事设施补修列表与提交一致性：本版实际流程通过

共同继承基点 `0e7b9bc2df90249a50851baeda58c7d183ea6059`，本批次父提交 `aa9bdf6de39b521548b436aeedf81118f82e7ad0`，完成批次提交 `c6f966ce378619b5ebfdb1bb3725c6f4c494e4e4`。APK54本版普通流程、冷重开及全部设备数据恢复均通过。完整目标继续推进，本文仅交付本批次。

原补修页面自行过滤相邻、受损、己方和施工部队条件，遗漏本部队已行动或正在另一设施施工，导致列表可见而正式提交拒绝。现在列表、选中后的复核和正式提交共同调用 `Fieldworks.repairCheck`，拒绝提供确切 code、字段和可理解原因。费用、城市七格和正式施工结算保持已核实规则。

仅两个生产文件：`core/src/main/java/game/sanguo/core/Fieldworks.java`、`app/src/main/java/game/sanguo/mobile/FieldworkUi.java`。另两个 B 测试文件为 SessionBFieldworksTest 与 SessionBFieldworksInstrumentation。既有 commandDialog / LegacyCommandSink 保留 World / StateToken 过期和双击防护。

APK54 从精确父源码导出 588 条编译输入，仅覆盖两个生产文件和一个普通流程设备测试文件。独立缓存和 build/out，使用冻结 A 提交 `47326188fbc43837c8d52caf0fa76051390faaf4` 的六条只读适配输入。未编译当前任何未完成单挑、物品、raw-loyalty WIP，core.jar 与 APK 内容已核对。原 A 文件、冻结桥接、Unity、168 固定输入及四 JNI 未编辑。

本版 APK：`out/session-b/fieldwork-repair-build-54/frozen/app-debug.apk`，313403450 字节，SHA256 `883d1b645deeae949f9912571b10e23c6c4f7f40e3e1ef7862e7d8964eca23fa`。设备测试 APK 为2549535字节，SHA256 `ec1a5f516e7513a215b2ec90c07a6d3e8a0f046e12a5278bfb1d51e17a7533ea`。两份实际安装 SHA 已核对。

父版本加完成增量通过63项 Fieldworks 检查、1690项 GameSession 检查、冻结 BridgeSession 检查。9份真实旧档36行续行 TSV 的 SHA256 `487b6343d13174c0fd57cb93e4923380109124db326e7828b91465ceffa15cd2` 与修复前一致。静态架构检查与 diff --check 通过。Unity U01 继承的 mapRevision63/65、terrain/坐标失败仍须独立追溯，未改 golden。

实际流程：Source14菜单新局曹操→夏侯渊5000枪兵携金1000→补金200取消/双击→城边土垒300多旬完工→普通移动→营垒500取消/双击→中止取消/双击→完整旬停止不增长→普通补修取消/双击→多旬完工→完整 World/双RNG 保存读取、活动重建及新进程重开。七个完整旬已全部完成。新进程自动续行及普通菜单手动读取均恢复完整World/双RNG；15内部和772外部原文件全部读回SHA相同，无新增残留，5582锁已释放。

APK50同类流程虽已通过，但含 dormant Duel WIP，不用于本批次交付或替代 APK54 实际验收。49、51、52、53的失败输入和日志均保留，未将失败或未安装版本算通过。

完整源码包取父版本全部10870条已跟踪文件，加完成增量、四 JNI 和实际冻结编译输入，逐字节及权限核验。不会把设备原用户备份或其他会话 WIP 加入源码包。

完整目标仍未完成：全源普通军建、破坏/所有存取路线、全局命令巡检、全部有效人物字段、完整普通单挑/舌战与31–39及历史自定义 Android续行、Unity来源语义及 ARM/扬声器验收继续推进。

## 封存与复现

完整源码包 `out/session-b/completed-repair-54-source-v2.tar.gz`：11478文件，662448154字节，SHA256 `8d78881dd102dbb7fecd9810a338f8a9c984b356147d4f7a973f27202eac1ec4`。全部条目及权限已从归档读回核验，详情见 `out/session-b/completed-repair-54-source-v2-manifest.json`。31项正常/冷重开及兼容证据包 `out/session-b/completed-repair-54-proof.tar.gz`：13126537字节，SHA256 `bb7edb132d7368a5c204a827d8406b947d92049c0acd7a122f4c4c3adbd00f62`；不含原用户备份或其目录清单。

解包保留源文件权限，使用现有JDK17和Android SDK，在包根目录运行（冻结stage及A只读编译输入已包含，不需要重新从当前WIP导出）：

```sh
./docs/handoff/20261006/session-b/run-session-b.sh ./gradlew -I docs/handoff/20261006/session-b/completed-repair.init.gradle --project-cache-dir out/session-b/completed-repair-gradle-cache --max-workers=2 '-Dorg.gradle.jvmargs=-Xmx2g -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8' :app:assembleDebug :app:assembleDebugAndroidTest --offline --no-daemon
```

包未分发Android SDK或Gradle依赖缓存；本机现有只读SDK和B缓存用于复现。各文件前后SHA、四JNI、安装APK、验收结果SHA、旧档/无Duel WIP边界见同目录 `MILITARY_REPAIR_54.json`。

源码包v1保留；最终v2和提交中的验收工具已剥离未交付物品模式，工作目录中的工具WIP保留。原APK54军建验收执行的fieldworks模式及安装前预检/全恢复逻辑保持一致。
