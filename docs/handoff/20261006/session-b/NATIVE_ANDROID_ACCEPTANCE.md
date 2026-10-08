# 正常单挑 Android 验收准备

2026-10-08。本项仅编译准备，未安装、未执行，不是完成批次。candidate60 仍为不可变组合依赖，不能将它或本测试宣布为完整还原。

独立 B 测试源 `app/src/androidTest/java/game/sanguo/mobile/SessionBNativeDuelInstrumentation.java` 已通过 `:app:compileDebugAndroidTestJavaWithJavac`。第一次编译暴露测试中装备字段、目标标签、包私有策略访问及参数类型错误；修正后第二次编译成功（35 tasks / 2 executed / 16s）。日志为 `out/session-b/duel-query-check/native-android-test-compile-v{1,2}.log`。没有改生产规则来适应测试。

测试使用正常菜单、出征编队、数量输入、地图实坐标 pointer、真实 AI 旬结算、目标/应战候选页面、原人控按钮及正常保存读取。读取权威快照只用于挑选合法行动；不写部队、资源、位置、人物、seed、RNG 或终局。呈现相机/选中由已有测试辅助操作，格子点击必须通过真实 3D ray 核验。

拟执行顺序：来源7的受约束寿命菜单选择/取消（flag18=1，28势力存在），来源0显式寿命0/死亡2/难度0普通新局，native503/stable10503 裴元紹从平原20004正常单人枪兵3000/粮30000/金1000出征，双确认扣账一次，正常地图移动并推进真实 AI 到相邻军队应战。来源0冻结 JAR 只读新局已核实此人 idle，初始城市48000兵/92000粮/4200金、枪库存6000；这只是合法开局前提，不是 Android 流程证据。

普通初始运行在真实应战并执行一次人控后保存/读取完整战斗；第二进程读取完全相同 World、双 RNG 和战斗模型，经真实人控到自然终局，再用可用继承/处置按钮一次结算、推进三旬、保存读取；第三进程再核对最终完整存档。自然拒绝、部队阵亡、36旬无可接受目标均保留失败现场，不重抽 seed 或替换军队。不保证一个自然战役覆盖胜、败、逃跑、三将支援及装备全部路径。

`native-duel.instrumentation.init.gradle` 只覆盖自己的 test runner，不改 AndroidManifest 或共享 Gradle。`session_b_verify_fieldworks_ui.py --runner native-duel` 已接入三阶段及原有独占5582、注册预检、全部内外部保存/库/偏好备份、安装SHA及最终恢复读回。Python语法检查通过；设备阶段尚未执行。不能使用 candidate60 的旧 test APK 运行新 runner。

依赖 A 的只读冻结菜单适配及稳定控件。当前测试按已发送请求使用 `pc.opening.options` / `pc.opening.<group.id>.<value>` / `pc.opening.confirm`；真实 A 控件与步骤返回后必须核对并适配，不能为测试添加产品步骤。目前缺少控件就明确失败，不走旧三参工厂。组合后需独立构建、冻结源与 APK，再核实5582空闲并完整备份。A5554继续属于A。

守卫见 `out/session-b/native-android-preparation-v1.json`：candidate60 的生产输入、冻结5产物、A只读源/Bridge/Unity及4JNI仍逐SHA一致；旧目录用与候选相同 `git status --porcelain=v1 -z` 核对31896字节/618527c6…。首次守卫误加 `--untracked-files=all` 导致输出布局不同，按原命令复核一致，无旧目录修改。有效未跟踪源码依然保留。

待完成：A冻结增量顺序组合和实际设备证据、独立新版 APK 与资源守卫、正常胜败放弃与完整16来源/武将/旧档矩阵、原未知项及 ARM。此文不代替全目标台账。

后继只读核对 A `OPENING_ADAPTER224.json` 和 stage224 控件源码：新局设置在势力picker内，需先选势力、设置、确认设置，再确认开局势力/开始新局；固定寿命的原按钮 selected/disabled，测试仅验证，不点击。已据实际控件改测试，尚未接A源。另修测试终局同名按钮仅选 enabled，避免误取已处理人物的禁用按钮；v3编译通过，v4按实际菜单步骤继续编译。224 A Main before a74def84…/A基点9ab4a3d6不同于candidate60已编译Main fc1ed192…，已请求正式冻结时提供全部已完成A依赖链或以候选Main为before的精确增量，不能直接覆盖混入其它A WIP。

v4最终测试编译成功（35tasks/2executed/17s）；候选6155生产输入再读回一致，仍未安装，安装前注册拒绝预检通过。收据已更新当前测试SHA。
