# 原单挑明确新局选项：A 前期只读适配方案

2026-10-08。本项只登记方案与只读取证，不实现、不编译、不复制 B WIP。当前 B 完成源仍为 `36f059b452f8fc5712747425a37fecbfee3ddd01`；Native 批次尚未完成提交。当前5554 Source11人物矩阵与音频/双堆串行队列不受本方案影响。

## 精确 A 路径

| 路径 | 后继完成增量中的职责 |
|---|---|
| `app/src/main/java/game/sanguo/mobile/MainActivity.java` | 普通PC来源新局入口建立/确认选项草稿；仅在明确选择后将不可变选项随sourceId/player/seed交给完成版四参数loader；保持原异步准备、stale保护、激活与存档边界 |
| `app/src/main/java/game/sanguo/mobile/ScenarioFactionPicker.java` | 只持有本次picker的呈现草稿/选择结果与确认摘要；原World预览、势力选择、取消/返回及closePreview释放顺序保持现有语义 |
| `app/src/androidTest/java/game/sanguo/mobile/SessionANativeDuelOpeningInstrumentation.java` | 预登记的后继正常UI验收测试；目前不创建。沿普通菜单触控验证明确选项、新局、取消、旧档、保存重开、stale/double与完整RNG/数据恢复 |

本方案不需要编辑UiTheme、MapHost、MapSceneSnapshot或B的16页面。直接复用已完成的UiTheme文本/对话框机制。后继若必须新增A helper或更多测试，先逐确切路径登记，不能借此扩大所有权。

## 已观察到的接口和入口

B未提交 `PcDuelOptions` 的字段是 `life/death/difficulty`，构造范围依次为 `0..3 / 0..2 / 0..2`。B未提交 `PcScenarioCatalog.load(String sourceId,int player,long seed,PcDuelOptions options)` 先调用现有三参数load，再初始化原单挑策略、当前军团，并声明对新原舌战流显式设seed。数值域是接口形状，**不是已核实原GUI默认或文本映射**；原PC启动RNG仍未证实，不能把System.nanoTime种子称为原PC种子复现。

A当前 `chooseScenarioTemplate` 的PC预览使用 `PcScenarioCatalog.preview(id)`；普通新局经 `startScenario(id,player,pinned,officerTextSource)` 调用三参数 `ScenarioCatalog.load` 或原CustomMaps路线。picker确认后先同步释放预览MapHost，才调用选择回调。只读receipt205记录实际路径/字节SHA与B的M/??状态；行号只是取证位置，以完成交付代码为准。

## 待完成冻结源后的实现边界

1. B交付完整完成提交、精确路径/前后SHA、依赖闭包、保存兼容和实际验收回执后，按原A/B串行契约核查并接入；不复制B工作区文件，不仅摘取options两个WIP文件。
2. 原单挑选项只属于本次明确新局选择。初始化为未选择，不从旧World、姓名、ordinal、旧存档缺失字段或未核实GUI默认推断；Android控件的默认position0不能算用户明确选择。三个域的标签与原代码映射须来自B冻结的原真值；未核实前不生成猜测下拉项。
3. 先完成本地草稿的明确选择/确认，再在原新局确认摘要中展示已核实的三个选项。改变势力、查看人物或地图、打开/取消选项对话框，都不调用四参数loader、不构造规则World、不消费任何规则RNG或改变存档。取消修改保留先前已确认的本地草稿；取消整个picker丢弃其草稿，不写下一局默认。
4. 仅明确PC来源新局且已确认完整选项时，后台调用完成版本 `PcScenarioCatalog.load(sourceId,player,seed,options)`。sourceId仍来自原来源身份，player仍来自正常可选势力；不按显示姓名/数组序号转换身份。seed在一次实际提交时捕获一次并交给B，A不初始化或重抽原规则/原舌战流。
5. 未明确选择的既有入口、旧三参数调用、普通/自定义重建地图、导入、预览和所有存档读取保留当前路线与策略；不把所有三参数load替换为四参数，也不加载后补策略或升级旧档。PC以外入口不得接受该原策略草稿。
6. 接受选择时捕获本次不可变source/player/options/人物文字来源，避免异步回调再从可被替换的全局picker读取。沿用当前World/revision/stale/double保护、一次activateWorld与成功后的自动存档边界。失败、取消、旧回调或重复按钮保留原World/手动与自动档，不能靠恢复页出现宣布稳定。
7. 保留当前closePreview幂等同步释放、独立MapHost和生命周期预算，不为选项再建立地图或复制World。需要确认B选项初始化是否影响峰值，并以新组合包实测，不借当前176/180内存成绩。

## B冻结交付前仍需真值

- life、death、difficulty三个原数值域的完整原GUI文本对应关系，以及默认值是否已证实；未证实项明确unknown。没有默认真值时保持未选择，不能称其为原默认。
- 完成四参数接口的完整依赖/持久化格式/旧档不升级证明，原新局初始化与原舌战seed的准确边界。
- 可只读验证已保存设置/策略状态的稳定字段或方法，以及sameStateToken示例；A不解析人类文案或私有序列化字节来猜策略，不新增Bridge/Unity序列化字段。
- 该完成批次支持的正常原单挑准入、自然胜负/继承/处分及已验证范围；源码编译/host fixture不替代实际Android正常流程。

## 后继验收

从真实菜单进入多个PC来源/多个势力，明确选项并新局；分别覆盖每域合法边界与有意义的组合，验证保存后实际策略事实，而不是复写接口算术测试。真实取消/返回/查看/改势力/重复按钮/旧回调均比较完整Save、全部规则/原流RNG和StateToken；旧31–39/既有新源三参数档与真实39手动读取不由新入口回填或升级。确认后的正常原单挑、自然结算/继承与多旬、手动存读、退出重开/新PID，须在B完成规则后走真实可达按钮验收。

A增量提交后与冻结B生成独立新游戏/测试APK，实际安装核SHA，每次完整备份保存/库/偏好并最终逐SHA恢复；默认大堆及普通堆分别测试，原四JNI/新增二JNI/168固定输入保持守卫。模拟器与ARM证据分开。全媒体、原事件发言者、地图BGM或原PC相机/时序不能由本选项方案宣称完成。


## 211 后继菜单事实（2026-10-08，只读）

B未冻结契约已提供原545350九按钮/55c9f0导出/配置写入块27组合的文本和数值映射：difficulty 初級/上級/超級=0/1/2；death 無/標準/多=0/1/2；life 史實/長壽/假想=0/1/2，不加一。先前构造域life0..3只是保存/内部域，原菜单仅0..2；Root+18非零强制life3的来源标志建立链及默认仍未知，不添加第四选项。上述运行结果来自B契约，A未重复执行；未执行完整4a42d0、真实PC GUI或默认开局。

后继完成API `GameApi.pcOpeningOptions()`提供同StateToken、不可变三组choices、原Big5/controlId及可缺省savedValue；defaultsKnown/automaticOverridesKnown=false，savedValue不是新局默认。A从完成API按group id/choice.value消费，不用ordinal/name，不从已保存选项自动选中；只明确确认完整三组后调用`PcDuelOptions.fromMenu`与完成四参工厂。延迟返回必须复核Token和picker生命周期，不为获取菜单建World或重抽RNG。没有活动session/unsupported时的目录读取入口仍需B完成契约，不猜静态替代。

当前B六个相关路径均M/??、完整Native批次未冻结；精确SHA与状态见211。A不实现/编译/复制WIP、不打断5554，原204后继验收与保存/所有RNG/Bridge/Unity/原4JNI边界继续有效。


211入口补核：A当前MainActivity.java:125–145只复用existing session或可读auto；world为空直接showStartScreen，正常“新建游戏”仍可触达scenarioPicker与chooseScenarioTemplate。因此首次启动/不可读auto恢复入口的session为空是实际代码路径，session-only新API不足以提供其菜单目录。后继B完成契约需无session且纯资源读取的目录入口，A不先建World或借预览session初始化策略/RNG；本次只核代码，未另跑Android首次启动成绩。Main源码SHA在211记录。
