# 第十阶段：规则回归、真实流程与 UI12

完整继承分支 `agent/native-pc-visual` 的未提交成果。UI12 从 `4c23785b5bf9f0f67387cd0cec907fabdb7f38f2` 顺序集成至 `a5f93be474ae9d18dfe5ba88df1818c6d5f19fff`，54 文件冻结 blob 全量预检，无冲突；保留本侧 progress 追加内容，未取后续 WIP。下一集成以 a5f93be 为基线。

## 实际规则与回归

- 原 PC 巡察、征兵、训练基础 20AP 已接入普通命令与城市 DTO。军事府 41 对训练减至 10 的原分支已验证，但设施未实现；军团 AP 与当前势力 AP 的范围仍不同。
- 9 个真实项目剧本：45 成功城市命令、135 完整旬与重开档，805 检查通过。初始无可用兵舍的 9 次征兵明确拒绝。另走普通付费建设兵舍、正常 2/3 旬完工、普通征兵，9 剧本 177 检查通过，没有修改初始库存。证据 `out/parity/city-actions-native-20261003/catalog.log`、`build-recruit.log` 及相邻保存档。
- 完整 core/runtime check 总体失败 32 任务。完整 N 快照的 132 份 main Java 重新编译后，与当前测试逐套对照，30 项首个异常相同。分类和原日志见 `out/parity/city-actions-native-20261003/failed-baseline-classification.json`，不能声称全量通过。
- 两个新差异已处理：Core 征兵旧预期剩 50AP 加强为实际剩 40AP，随后仍遇到 N 的 AI deployment 旧失败；Districts 对同优先级低治安城市先处理治安更低者，修复 20AP 下重复治理导致其他城市饥饿的回归。原三旬断言保留，TerritoryAi 674 检查通过。
- 定向规则检查通过：城市 1899、委任 288、城市会话 140、军团 674、建设 489、运输 269+88、年份 50。历史失败没有删断言或降标准。

## AI 路线计算

查询局部阻塞/友军布尔格网取代集合查询；直接比较器保留 priority/cost/q/r 顺序；单边复用纯水域判定。没有持久缓存、新状态或新随机数消耗。

改动前完整 classes 位于 `out/parity/ai-route-performance-20261003/before-classes`。9 生产剧本各 3 完整旬，每旬与重新读档的状态比较，27 行完整 save/RNG 字节一致，比较 SHA256 `2d998fd958a71e51efbacaf3b904eed098fc56173de94d49a5a27d411b67dcd0`。本机顺序 JVM 78,331→45,137ms，有并存系统负载，仅为这个样本，不代表 Android 或 ARM 性能成绩。

## APK 与安装边界

新 UI12 合并包构建成功，30 秒、76 任务。封存 `out/parity/ui-integration-a5f93be/apks`：

- 主包 87,011,185 字节，SHA256 `7801884b02e2a99de5e9e15814c48d43b12c00d2bb367fd33c2b4bfed9e8c958`。
- 测试包 688,371 字节，SHA256 `bc309a78582db7ab5155a3aed738b6480e7ccdb89795f4754301b325c6e3be47`。
- 同时保留 x86_64 与 ARM64 原生库。此记录时新包尚未安装；ARM 无真机证据。

UI12 独立 5580 验证使用旧 G（城市 10AP、新建 Lv3），不替代当前核心 20AP/Lv1 的安装验收。UI 固定 AP10 文字仍需下一批消费 CityActionPreview/ConstructionPreview；不能把接口已存在称为界面已接入。

P 包 `f4cd4d34…` 已实装回读一致，在 SwiftShader 5554 上刘备年轻仍 360.03 秒超时，剩余 11 例未运行。7 原用户文件恢复全等、无新增，自动档 SHA256 `82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9`。首次安装 60 秒超时在冷启动 dex 优化阶段，独立保存；安装与大文件回读时限改为 180 秒，不改变玩法断言。

独立 lifecycleOnly 诊断不含严格缓存颜色检查，不能替代年龄验收。实际进程 6207 的 SIGQUIT 栈 `age-lifecycle-diagnostic/app-trace_02.txt` 显示设备时间 03:52:16 runner 在 ready 等待，scene-cpu 在 TerrainSurface.water→WaterVisualField.distance→TerrainMaterialField.attach 构网。单次栈不证明死锁或唯一根因；其他系统进程 ANR 也没有误标成游戏栈。

完整快照 Q 包含全部继承源码、资源、4 个 native 输入、UI12、公平性与路线优化；以 `out/parity/checkpoint-20261003-q/manifest.json` 为准。Q 不包含此文档的后续记录。全目标未完成，原有玩法、数据、旧存档与安装流程差异继续保留。

## Q 首次实装结果及原程序巡察补充

Q 主/测试 APK 已实际安装、分别回读 SHA 一致。`installed-opening` 在 360.02 秒时超时：原保存恢复启动、真实写入槽位3、取消覆盖保持字节、取消读取保持权威状态/RNG、震动开关持久化已通过；尚未进入新开局。证据已取回 `out/parity/ui-integration-a5f93be/installed-opening/evidence`，实际截图保留，不能声称完整流程通过。结束后全部7原用户文件恢复字节全等、无新增。随后仅重启本侧5554恢复host图形/4核心，同一AVD不清数据；记录 `out/parity/emulator-host-q-20261003`。UI5580未操作。

新增 `tools/content/test_pc_patrol_calculations.py` 三项原程序测试通过：55组无周围部队的1–3人/治安边界，真实5cba10调用其校验器与原周边扫描；合计actor+170除以28加2，之后治安上限截断。48a2d0→原vtable+4c/48a110证明+170来自基础能力索引0，隔离样本使用原700..799无年龄修正分支，不把它推广为普通武将忽略年龄、经验或物品。另20组实际有效单位/势力/军团调用原4b9960→4b5cc0，在测试的距离1–3点触发减半，距离4和同势力点不触发，先整除减半再按100治安封顶。完整全局3MiB及地图1MiB在每次计算后不变。

这仍是有边界的原计算证据：外交字段中文语义、全形状范围、普通武将修正和正常三执行者界面/准入未全部校准。当前运行时仍为旧政治/魅力巡察公式，差异明确保留；本批没有把未闭合解释写进生产规则。初始探针由于城市军团未赋值导致原owner=-1，补合法city+38后才触发原分支，未替换原校验器。
