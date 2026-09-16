# v0.28 验收记录

基线 PR #35 核查后合并为 `ae1fef7`。本轮 [PR #36](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/36) 可审阅、未合并。以下结果对应运行提交 `66417c08739441322ac8d2d01b85b36a16b0d88c`；其后收尾仅更新文档与证据，不改变 APK 运行代码。

[下载 APK 归档](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35113579871/artifacts/10455463236) · [完整 Android CI](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35113579871)

| 核验项 | 结果 |
|---|---|
| APK | `sanguo11-mobile-v028-66417c0.apk` |
| 版本 | `0.28.0-mobile-experience-dev`，versionCode 28 |
| 包名 / 存档 | `game.sanguo.mobile.dev` / v21，不改格式 |
| APK SHA-256 | `bd24da23469cf77ab4991dfa4982af30eb087e2ea3ef72e5228a4d8a6f2e3df1` |
| 固定证书 SHA-256 | `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24` |
| 编译源码 | CI源归档426份文件逐字节匹配运行提交 |
| Lint | 0错误、19警告：SetTextI18n 14、ViewConstructor 2，其余OldTargetApi / DiscouragedApi / DataExtractionRules各1 |

APK归档里的 BUILD_COMMIT、SHA256SUMS、SIGNATURE.txt 已与下载字节、aapt版本及本地apksigner复核。完整产物ID、ZIP摘要及截图摘要见本目录JSON和sha256文件。

## 旧版实装

[CI35100123171](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35100123171) / [实际旧版操作证据](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35100123171/artifacts/10447729342)。归档SHA-256 `17c68e9535f508a565d94bd0192f6212d5c713de84dd1c3804e9b08405b070d5`。

API29 x86_64，1080×1920、420dpi，横竖屏。安装上轮实际v0.27 APK并核对固定SHA，不是重新编译一个“旧版”。输入通过Android实际触摸事件和可访问性输入；夹具只布置情境，命令从正式手机入口执行。不是人手真机测试。

`baseline-experience.txt`记录镜头位移、下一队、按钮状态、42城670将和200×200/42城40队3运输场景耗时；`baseline-instrumentation.txt`记录完整操作通过。截图位于归档`files/smoke`，包含选择前后、全国视角、出征、运输、补给、军团和重建流程。

## 同模拟器前后实装对比（已通过）

[CI35113579989](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35113579989) / [完整对比截图、操作与原始日志](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35113579989/artifacts/10455591168)，对应运行提交 `66417c08739441322ac8d2d01b85b36a16b0d88c`。归档 SHA-256 `cde7a7ee219aa900035038ccfed441cd61d5f9a262a4bf6e7209df777a71dee6`；新旧两份 EXPERIENCE PASS，日志无应用 FATAL EXCEPTION/ANR。先前已实际操作旧版，本次配对顺序为新版后旧版；二者在同一个模拟器运行，旧版继续安装固定SHA的实际交付APK，旧观察runner只同步相同计时方法。

| 实际操作观测 | v0.27 | v0.28 |
|---|---|---|
| 拖动后普通点选邻队，镜头中心位移 | 横竖屏均54.602478地图单位 | 横竖屏均0 |
| 从队1向后切换，队2已行动 | 选中已行动队2 | 选中待行动队3 |
| 已行动部队的攻击按钮 | 禁用且不可点 | 弱化但可点，显示“这支部队本旬已行动” |
| 竖屏选中对象地图尺寸 | 1080×638 | 1080×792，高度增加24.1% |
| 横屏选中对象地图尺寸 | 1041×606 | 1112×606，宽度增加6.8% |
| 顶栏全图 | 需要进入视图菜单 | 一次点击且断言scale等于minScale |

详情尺寸是该实际场景数据，不代表每个弹窗或所有字体设置。路径点击次数及步骤见[本轮说明](../../MOBILE_EXPERIENCE_V0_28.md#操作路径对照)。

[完整配对耗时表](performance.md)，原始文件 `before-experience.txt`、`after-experience.txt`。42城670将绘制中位6.256→6.461ms、详情刷新3.496→4.489ms；200×200/42城40队3运输绘制6.820→6.827ms，详情刷新5.974→5.899ms，整旬中位4936.0→5121.5ms。本轮不是全面提速：大图显示和刷新有开销，完整AI模拟仍是较长等待；已提供结算中反馈，不跳过模拟。全国列表和选中/范围索引没有出现每帧重算。

前四类计算测量各预热3次后记录12个样本；原生手势各记录12次；P95取排序后的最大样本；整旬为同一局面三份副本。软件Canvas不是GPU帧时；注入拖动/双指缩放到idle包含测试事件调度，不等于屏幕触摸延迟。新版地图可见面积较大，原始nativeViewport/tilesVisited同时保留。选中/范围索引是合计，不声称独立寻路耗时或ARM手机性能。

归档中的 `after/files/smoke/` 和 `before/files/smoke/` 保留同名关键截图。`v028-before-select`→`v028-after-select`用于选择前后相机；`23-formation-confirm`→`24-three-officer-army`记录同一三将流程的预览及实际出征（`v023-quantities`另属数量控件场景）；`v027-convoy-map`→`v027-field-supply`记录运输与补给；新版 `v028-national-position-before/after`记录原滚动位置返回，`v028-overlap-picker`记录密集对象选择。截图前缀沿用回归编号，目录和APK版本决定实际来源。

## 新版完整门槛

CI35113579871 三个作业全部成功。完整核心、36/72旬经营、51,804条UI投影/相机断言、19项内容测试、Android主包/测试包构建及Lint全部通过。新增9条模型断言针对明确的选择/命令行为；手机入口还通过实际安装后的原生输入和权威局面断言，不能只据模型数量判断体验。

| API29 x86_64 安装场景 | 最终作业 | 结果 / 证据 |
|---|---|---|
| 1080×1920，420dpi，横竖屏完整流程 | 104853621612 | SMOKE PASS；[184文件证据归档](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35113579871/artifacts/10455763091)含升级、恢复 |
| 1080×2340，420dpi，横竖屏完整流程 | 104853621495 | SMOKE PASS；[证据归档](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35113579871/artifacts/10455332483) |
| 1080×2400，420dpi，横竖屏完整流程 | 104853621707 | SMOKE PASS；[证据归档](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35113579871/artifacts/10455622554) |
| 1080×1920，360dpi，字体1.3，横竖屏IME | 1920作业内前置恢复检查 | RECOVERY prepare/check PASS；实际IME窗口存在、发送按钮≥40dp且位于键盘上方 |
| v0.9原位覆盖 | 同上 | 固定历史提交重编旧APK，UPGRADE PASS |
| 实际v0.25 / v0.26 / v0.27交付APK原位覆盖 | 同上 | 核对各旧APK固定SHA后安装，UPGRADE25/26/27 PASS |

覆盖升级未清除应用数据：旧应用实际读写旧档，再install -r新版，继续一旬与重建核对不重复执行。v0.27使用实际交付APK将真实旧编码器v20夹具写为自身v21，然后覆盖；没有用新版编码器冒充旧数据。

| 正式入口链路 | 核验内容 |
|---|---|
| 城池 → 三将 → 数量 → 出征 | 主副将、滑杆/精确输入、一半/最大、预览取消无扣费、真实编队、数量草稿旋转/重建 |
| 地图 → 指令 → 目标 → 预览/确认 | 行军、普攻、战法、设施攻防；拖动不执行、查看不攻击、取消和Back分层；待行动前后切换 |
| 城池运输 → 地图运输 | 三将派出、携粮真实消耗、停止/改道、补给预览取消/确认、货物守恒、重建、同城双运输稳定ID候选 |
| 全国异常 → 定位 → 列表 | 缺粮/在途筛选、处理入口，返回原筛选、排序、首行与像素偏移 |
| 城池军团 → 设置预览 → 取消/确认 → 下一旬 | 玩家设置不被AI改写，批量影响对象、实际委任与执行原因 |
| 保存/恢复/快捷重复确认 | 连续确认只执行一次；旧局面/旧Activity确认拒绝；HOME真正后台后回读7654草稿，再force-stop新进程恢复，世界/随机数不变；取消后不自动重开 |

完整日志均无应用FATAL EXCEPTION/ANR，原历史功能回归保留。实际抽查三屏运输首屏、补给、列表及战斗画面；488张完整门槛PNG及105张配对PNG解码和摘要核验，连续截图及原始日志随证据包提供。

键盘证据限制：横屏截图可见实际数字键盘与其上方发送按钮；竖屏截图捕获了IME占位黑区，虽然窗口存在和按钮边界断言通过，不能将该PNG解释为完整键盘绘制已目检。没有ARM真机，HOME+force-stop不是系统低内存杀进程，也未注入磁盘写满。


## 完整回归中确认的布局缺陷

CI35108798083 的2340/2400全流程通过，1920在最后外交视觉流程失败：诸葛亮搜索结果不可触达。`failure.png`显示搜索和筛选填满了38%的操作面板，下方ListView空间不足。修复管理页面竖屏默认占62%，保留地图对象详情38%；新增在默认未展开状态下要求列表至少80dp且能真实打开诸葛亮详情。保留原外交、援军、劝降等完整断言，不能把“点击展开后可操作”当作原检查通过。

新字体/恢复门槛前置执行，以免被更早的历史流程失败遮蔽。恢复检查明确要求系统IME窗口存在，并验证发送按钮位于实际键盘顶部之上；仅改变输入焦点不算键盘验收。


CI35112113750的前置冷启动检查复现局面正常恢复、运输草稿未打开。新增同步提交后台UI提示、真实系统HOME动作、强制停止前回读草稿7654的检查和恢复日志。局面编解码独立排查中，原始与读回物流夹具3124字节完全一致；没有改动v21编码格式来绕过问题。最终CI35113579871的prepare/check均通过，世界字节与草稿7654一致。此前测试未核实真实HOME和提示落盘，不能把失败单独归因于异步apply。

## 未完成项

完整选将向导逐页恢复、所有历史报告条目定位、密集场景全部标签同时可见、Toast短暂遮挡仍待改善。v0.27精确运输参数、高级舰船当前船型、复杂路口死锁、连续敌对72旬组合未完成；官方全国逐格地图、完整官方剧本、全部事件和精确公式缺口保持。详见[本轮边界](../../MOBILE_EXPERIENCE_V0_28.md#边界)。
