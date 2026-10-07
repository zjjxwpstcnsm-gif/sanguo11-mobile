# A 新局选项适配 224：冻结候选依赖编译

此增量只修改独立 `out/session-a/native-opening-stage224` 中的 MainActivity 与 ScenarioFactionPicker。当前 A 正在运行的媒体/音频/双堆队列及其规范生产源码、游戏 APK、测试 APK 未改变。尚未生成或安装组合 APK；编译不能替代正常 Android 触控/存读/冷重开验收，全部目标未完成。

共同完整继承基点 `0e7b9bc2df90249a50851baeda58c7d183ea6059`；A 制作时 HEAD `9ab4a3d61bd5e6db2f89ff7c787ee1481105a211`。使用 B candidate60 固定报告及 JAR，不读取活动 B 源码。196 overlay 路径 before/after 与 612 未变 B 依赖逐 SHA 一致，无冲突。B 候选的 MainActivity 旧只读 SHA 与 A 当前文件不同；本补丁以 A 已完成的 `a74def84…` 为精确前态，保留 A 后继修复，不能直接覆盖 B 的旧 A 文件。确切前后 SHA、依赖和补丁 SHA 见 OPENING_ADAPTER224.json。

## 正常产品步骤与测试标识

1. 普通菜单选 PC 剧本，进入现有势力预览。卡片、地图、势力、人物文字入口沿用现有流程。
2. 下方“新局设置”按钮 tag `pc.opening.options`，可访问说明“选择新局设置”，打开真实设置对话框。三组容器 tag 为 `pc.opening.difficulty/death/life`；实际按钮 tag 为 `pc.opening.<group.id>.<choice.value>`。用户可见标签直接来自冻结菜单 DTO，未证实默认值的组保持未选择。
3. 固定寿命来源仅选择 DTO 已证实的 fixedMenuValue（菜单 2），显示“剧本固定”，按钮禁用；不添加内部寿命 3 的第四菜单项。固定规则生效值交 B 四参数工厂处理，A 不重算。
4. 实际“确认设置”按钮 tag `pc.opening.confirm`，可访问说明“确认新局设置”。未完成全部组时显示“请完成各项新局设置”，不关闭；“取消”丢弃本次编辑，保留此前确认的本地草稿。关闭整个势力预览丢弃所有草稿。
5. 原“以势力开始新局”在选项完整后可用；现有开始新局确认包含选项摘要。此正按钮才构造不可变 fromMenu 元组；预览同步释放后调用绑定本次 picker 的回调，一次捕获 seed 并交四参工厂。没有选项入口的旧三参/自定义地图和存档读取路线保持原代码。

草稿绑定精确 sourceId、来源 SHA、Shared SHA、path、variant；后台提交再次核对。异步前捕获真实 World/revision/StateToken（首次无 session 使用目录而不造 Token），回调复核 Activity、picker generation 与真实 session。取消、旧回调、重复开始按钮不会激活新 World 或写存档；失败保留旧局，具体动态证明仍待新组合包。

## 可复现与串行集成

`prepare_opening_stage224.py` 核 SHA 后复制 A app，并仅从已核 SHA 的 B overlay 取两份 B 页面、从冻结产物取三个 JAR。它不是新的完整继承目录或完整 B 集成，不能据此声称 168/JNI APK 守卫通过。复现时使用新隔离副本且阶段目录不存在，运行 `--apply-reviewed-adapter` 可按补丁重现确切两份 A 后态；已有证据目录拒绝覆盖。

`build_opening_stage224.py` 使用独立阶段输出、SDK35 AAPT2、JDK17、冻结 B JAR 与现有 Filament，编译全部实际 app Java 与资源；人工生成 BuildConfig 仅用于编译，未 dex/签名/安装。第一次资源编译发现 Gradle largeHeap 占位符需在编译用 manifest 解析，脚本已修正；最终 Java/resources 编译 exit0。补丁另在独立 patch-readback 目录应用并逐字节对照成功。

最终集成应先检查两份精确前态，若前态不同需逐项合并 A 完成修复再审查，不覆盖。组合 B 完成增量、完整继承未跟踪输入、168 固定资产、原四/新增二 JNI 后另构建新 APK；每次安装完整备份保存/库/偏好，真实触控取消/新局/单挑/保存/冷重开后全部读回恢复 SHA。当前 5554 媒体队列不能中断，B 5582 不由 A 操作。模拟器与 ARM 分开，原镜头/完整事件/音频/所有来源完整目标仍待闭合。
