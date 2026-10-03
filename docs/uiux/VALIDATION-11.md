# 第十一批：运输草稿与完整运输路径

2026-10-03，独立san11-uiux / emulator-5580 / API29 x86_64。最终安装包运输表单64项、钱粮送达与人员返程43项通过；含兵装的完整旅程51项也通过。整体目标仍在进行。

## UI改动

运输从长表单末尾选择主将改成三页草稿：运输编队、钱粮兵力、兵装舰船。主副将使用统一可搜索、可排序的能力表，排除重复选择，取消选择返回草稿。数量输入、横竖屏、键盘和预览按钮适应可见区域；返回修改、Home恢复保留主副将、数值与返程偏好。可选兵装初始全部为0，避免隐含携带1000枪装备。[改前](evidence-11/before-cargo.png) / [改后](evidence-11/after-cargo-crew.png)。改前实际安装SHA已单独核对，见before-installed.json。

删除旧DomesticUi的耗粮近似公式、运输容量硬编码和目的地舰船容量推断。输入只约束出发城库存，标题明确为“库存”；完整运输上限、路线、耗粮、容量和失败原因来自现有核心transportError/shipCargoError/transportPreview。[键盘数量错误](evidence-11/quantity-keyboard.png)、[权威拒绝条件](evidence-11/authority-rejection.png)、[最终确认](evidence-11/confirmation.png)。遇到核心拒绝先行内说明，可主动查看完整条件；未自动连弹多级窗口。最终发送仍经版本校验的commandDialog和MainActivity.applyResult，预览没有扣费或消耗RNG。

DataTable.choose增加可选关闭回调，用于恢复底层运输表单的窗口适配跟踪，旧调用签名保持兼容。QuantityControl增加纯显示的范围说明。CargoWizard抽取原DomesticUi职责，只有Bundle草稿；严格边界审查把这个确切文件加入既有legacy consumer名单，说明见docs/architecture/MODULE_RULES.md的UI11段。没有修改核心/API/runtime或规则实现，没有扩大目录通配名单。完整结构化运输DTO仍等待玩法接口，当前库存输入上限不是运输合法性承诺。

## 实装证据

- [表单64项](evidence-11/transport.txt)：实际陳留→官渡港，三将搜索/空态/取消、去重、数量超库存/修正、核心拒绝21000兵且完整状态不变、查看核心条件、横屏、返回修改、Home恢复、双击只派一支队伍/一个revision、货物与库存正确、任务可见、读档恢复、重新打开无旧主将、整表取消。
- [送达与返程43项](evidence-11/cargo-journey.txt)：真实一将运输1000兵/5000粮，两次点击下一旬并跳过演示后完成卸货与人员返程。中间任务[明确显示返程](evidence-11/return-task.png)，最终武将回到陳留、运输任务消失，[官渡港保留兵员](evidence-11/destination.png)，正式读档恢复原局面。使用正常190曹操存档和完整AI回合，没有测试场景直接写入或模拟规则调用。
- [兵装旅程51项](evidence-11/equipment-journey.txt)：增加实际100枪兵装备，滚动至货物列表末尾，零斗舰库存输入1被禁用、修正0后保留之前的兵装数量；真实卸货清空运输载荷，返程后官渡港装备恰好增加100，未重复入库。[长列表末尾错误](evidence-11/equipment-stock.png)。
- run01测试在验证数量错误时因自身String.contentEquals遇到空的contentDescription抛错；错误已修复。保留[test-selector-failure.txt](evidence-11/test-selector-failure.txt)与[failure-recovery.txt](evidence-11/failure-recovery.txt)，不将其说成应用崩溃或通过。
- auto/manual3/自建武将库最终SHA一致，见save-after.sha256。没有清理数据或操作5554。

## 构建、性能与范围

源码faf390d加本批UI/测试和边界审查。仍以独立冻结G的346文件构建，使用contract-v2.init.gradle；root504保护文件与G输入SHA保持一致。两包各168固定美术资源通过，native worker/unicorn与继承库逐字节相同。

- x86_64验证APK：out/uiux/iteration-11/app-uiux-run02.apk，82245084字节，SHA256 `851fdd4845c77ba4b0fc946dc0e4cdb671d45995499c916cce4b65bce98c2f0f`。安装后再次读设备APK核对SHA。后续补充测试仅更新test APK，应用未换包。
- ARM64 APK：out/uiux/iteration-11/app-uiux-arm64.apk，82187925字节，SHA256 `0a2ec57bde78a5142a45dab4c112b0d6c7ea13a3a8bf328c06fbb7724d4bee9c`。仅构建与ABI/资源/库验证，未安装或测试ARM性能。
- 录像：iteration-11/before（旧表单）、run02（64项）、run03（43项）、run04（含兵装51项）。run02的5/15/25/35秒、run03的5/10/15/20秒、run04的5/15/25/35秒有实际解码PTS；已查看确认、键盘、返程关键截图及run03的20秒帧。源视频均保留。
- preview-timing.log是该短途场景的同步核心预览采样；0代表毫秒计时分辨率，不能推广为全国任意路线性能或零成本。查询由明确的预览动作触发，不随每个按键运行。
- 源码快照iteration-11/source和source-manifest.json，配第九批未变的352项iteration-09/build-inputs.tar.gz。

仍未验证海运全部条件、改道/截击/满仓等待组合、进程销毁后的草稿重建、所有城市内政与高级外交、地图多指手势及长时性能。最终合并包仍需由玩法会话集成验收；不把本批短途真实通过称为完整目标完成。
