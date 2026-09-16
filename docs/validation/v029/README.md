# v0.29 最终验收记录

PR #36 已核查并合入 main `6fa745bd8dc537922494705882d0c4c688e20650`；本轮 [PR #37](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/37)，分支 `agent/tactical-displacement-v029`，不自动合并。

## 提交与 APK 身份

- 最终构建提交：`b08c518c3c6ed2c92916364c181794c34477d107`。应用运行代码与37ac1cc完全一致；f0df381与b08c518补齐两处旧测试重启Activity后等待恢复窗口稳定。37ac1cc修复恢复草稿的携粮下限，并把展开提示前置、隐藏无位置结果的定位入口；核心战法规则与3201500一致。
- 中间完整验收提交：`bb4e3ea9856db3b0c15928bc624cc393e3523916`，CI35125136314三屏全部通过。它相较3201500仅修正人事测试误点；随后截图复核发现携粮下限仍用默认3000，故增加37ac1cc收尾并重新完整验收。
- 最后收尾提交仅更新README、progress、FEATURES、规则说明和验收文档/原始记录，不更改运行或测试代码。
- APK：`sanguo11-mobile-v029-b08c518.apk`；大小 `6,274,126` 字节；SHA-256 `d03f9d30e525d719217c9aef365177c1f30d5a6b6f09435da28f20f776f0252b`。
- 包名 `game.sanguo.mobile.dev`；versionCode 29；versionName `0.29.0-tactical-displacement-dev`；写存档v21。
- 固定签名证书SHA-256：`8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`。
- APK下载归档：[归档10462473769](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35132774435/artifacts/10462473769)，ZIP SHA-256 `c797844066b2ab1f37b80e58f25e18c80b24748dbc7b725edd6eb64ebc7a83dd`。
- 最终源码归档：[归档10461629576](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35132774435/artifacts/10461629576)，ZIP SHA-256 `36087f02c4e302ea2a3ce34b5252bfd0e96a486b325dcb1b917ecdf758ecb859`；457个文件与`git archive b08c518`逐字节一致。此前3201500、bb4e3ea、37ac1cc和f0df381归档各457份文件已核对一致。

## 门禁和真实入口

| 检查 | 状态与证据 |
|---|---|
| 完整核心、UI模型、内容与长局回归 | 通过；266条新增位移断言、51,804条UI模型断言、19项内容测试及全部旧核心/36与72旬回归 |
| Android构建、Lint | 通过；CI Lint **0错误、20警告**（[报告](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35132774435/artifacts/10462089086)）。本地0/19；CI额外报告现有targetSdk35的OldTargetApi，其余19条与本地一致，未忽略或压制警告 |
| 1080×1920/2340/2400，API29横竖屏完整安装操作 | 三组全部通过；完整正式操作、横竖屏及存档断言均保留；[CI35132774435](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35132774435) |
| v0.9/v0.25/v0.26/v0.27原位覆盖 | 全部通过；v0.9从固定旧提交重建，v0.25/v0.26/v0.27使用实际交付APK；主CI标准分辨率作业 |
| 实际v0.28覆盖＋23场景位移配对 | 通过 [CI35132774542](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35132774542)，原始记录与归档见配对说明 |
| 既有体验配对（实际v0.27基线） | 通过 [CI35132774416](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35132774416)；数值样本另标明测量提交，未混用轮次 |
| 1.3倍字体、真实IME、HOME后force-stop恢复 | 通过；HOME后force-stop覆盖运输草稿；新出征向导覆盖横屏键盘、旋转、逐步Activity重建、过期库存拒绝与真实出征 |

战法按正式“选己方→战法→地图目标→预览→取消/确认”操作；夹具只布置局面。三屏脚本另覆盖受阻突刺、己方火种、二段第二格受阻、水军岸线、主伤击破跟进、熊手退路/突破落点原因、战果定位返回，以及下一支未行动部队进入腾空格；不重置已行动部队。新向导逐步保留主副将/兵装/舰船/数量，导航和重建保持存档字节一致，改变库存后真实确认先拒绝，再由用户确认实际出征。另验证恢复2500兵时2499粮被拒绝、2500粮可确认，防止下限仍用默认3000兵。

新战法预览不采样正式RNG；核心回归检查预览/非法命令/重复确认无资源或随机状态变化、逐格与各自通行能力、盟停战保护、连锁死亡身份、运输对象和货物/缴获一次结算、AI评分纯度与稳定回放。既有随机未命中扣气力/行动而无位移的回归保留。

## 基线与可复核证据

实际v0.28 APK `sanguo11-mobile-v028-66417c0.apk`，6,258,594字节，SHA-256 `bd24da23469cf77ab4991dfa4982af30eb087e2ea3ef72e5228a4d8a6f2e3df1`。旧版运行66417c0至最后文档7c2053b无app/core/scripts/工作流变化；CI35113579871与35113579989均已核查通过。

- [初次旧版复现CI35119445521](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35119445521)；23局原始文字 `baseline-displacement.txt`，67张截图及每局存档在[归档10456658758](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35119445521/artifacts/10456658758)。
- [位移/升级配对说明](PAIRED_DISPLACEMENT.md)、`candidate-displacement.txt`、`upgrade-v028.txt`：实际变化与已有能力分开列出。
- [既有手机体验回归和性能](EXPERIENCE_REGRESSION.md)、`experience-before.txt`、`experience-after.txt`：保留v0.28原配对工作流的v0.27基线；37ac1cc同模拟器样本的大图整旬约+1.8%，未宣称全面提速，也不当作v0.28→v0.29直接性能比较。
- 最终三屏原始截图、日志和断言：共579张PNG及完整日志；203张为标准屏（含恢复/升级），两个长屏各188张；归档见下表。

| 分辨率 | PNG | 证据归档 | ZIP SHA-256 |
|---|---:|---|---|
| 1080×1920 | 203 | [10461949314](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35132774435/artifacts/10461949314) | `3521912238bd58b7b9a339736b7af320fca1b278542daf8b86a7881a6a2ec5a4` |
| 1080×2340 | 188 | [10462388064](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35132774435/artifacts/10462388064) | `963c1e101e5c0a2bc07c2e472e6616312423f0fab04d43e22ab990a7fcba1418` |
| 1080×2400 | 188 | [10462983440](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35132774435/artifacts/10462983440) | `d4a17e2aed5b080fe57392a0dd169f19c65a2e94ed8de4c5961020c39d6fe6c1` |

中间CI35122887437的两个长屏作业失败在旧人事测试误点“清空副将”，不是应用崩溃。已从人事步骤移除该点击，保留褒奖忠诚增长等所有旧断言；未跳过该流程或放宽门禁。其标准分辨率升级和恢复门禁已通过，后因新提交重跑而取消。以最终完整CI为验收结论。

37ac1cc的CI35128269155中，2340完整通过，2400在旧人事测试重启后首次导航时失败：系统从竖屏恢复横屏，向旧坐标(100,2205)的测试触摸被InputDispatcher丢弃，存档一致性断言已先通过。该轮1920因新提交触发并发取消，不计为通过。现场无应用FATAL EXCEPTION或ANR；日志摘录见 `rotation-retry.txt`，[失败现场归档](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35128269155/artifacts/10461175766)。f0df381补充新Activity身份、窗口焦点、尺寸/方向和连续稳定检查后再点击；未修改游戏运行逻辑、未跳过人事流程或减弱存档断言，重新运行完整门禁。

f0df381的2400作业在更早的旧战略流程另一处CLEAR_TASK重启后出现相同丢触摸，旧竖屏坐标为(927,2205)，同样无应用崩溃/ANR；[现场归档10461373943](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35130621921/artifacts/10461373943)。b08c518将同一窗口等待用于这第二个入口；已核对全部两处CLEAR_TASK都等待新窗口稳定，保留存档/运输/旬推进所有旧断言。最终完整CI重新覆盖全部流程。

## 人工检查与限制

人工抽查最终三屏的6张关键界面：1920受阻突刺预览与1.3倍字体出征搜索真实IME；2340恢复后的2500兵/7654粮/321金及二段第二格受阻横屏预览；2400击破战果地点返回及人事重启后的状态。预览取消/执行、结果返回与出征确认均可见，恢复携粮下限为2500。579是归档截图数，人工检查范围为上述抽样；三组日志均无应用FATAL EXCEPTION或ANR。

1.3倍字体横屏输入时，确认/取消保持键盘上方；有限屏高下选将列表需收起键盘后查看。覆盖的是模拟器原生窗口和Activity重建、HOME后force-stop恢复，不等于ARM真机、OEM输入法、系统低内存自动回收或所有辅助功能组合验证。

[完整规则与差异](../../TACTICAL_DISPLACEMENT_V0_29.md)：必要的熊手退路和突破落点限制保留；险径通行和己方火种触发修正。100碰撞损兵是继承工程值；设施/据点碰撞耐久尚未实现精确结算；原版数值、全部地形边界和水军命名/等级/气力仍待核。本轮未完成官方全国地图、完整剧本/事件及原版全规则，也不把已有v0.28手机功能重新列为新增。
