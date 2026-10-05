# 第九批：出征v2、上下文战法与成都键盘回归

2026-10-03，独立API29 x86_64模拟器：成都场景25项、出征62项、开局/设置/存取档62项真实检查通过。完整目标未完成。

## 实现

消费玩法冻结checkpoint-20261003-g的缓存预览、完整UnitFacts和战法上下文方法。DeployWizard不再调用旧Army.deploymentPreview或使用临时Unit补齐特技、战法、气力、射程、部队统武智和攻防；全部使用不可变DTO。无可用编队时不展示伪造数值，战法显示核心formationError，目标条件仍在正式命令核验。ArmyUi成本调用Army.tacticCost，正确区分陆上/水上投石；ArmyUi、WarUi列表可用性调用各自tacticFormationError，移除UI仅按适性判断的重复条件。实际战场触控验证将在下一批继续，不能把DTO验证称作所有战法实战通过。

编队页在键盘/横屏可见高度不足480dp时收起大块选将卡与说明，保留搜索、空态和实际名单。关闭键盘后恢复详情。没有改地图、美术、核心规则或全局游戏状态。

## 实装证据

- [成都25项](evidence-09/chengdu.txt)：真实菜单新游戏→250幻想重建→刘备/成都→出征→搜索空结果→清空→横屏可触控武将行→取消→加载原190曹操存档。其界面来自生产目录的定制重建剧本，不称官方复刻剧本。[玩法方原失败](evidence-09/before-chengdu-shared.png)→[本批修复后](evidence-09/after-chengdu-keyboard.png)。原图只读复制，未操作5554。
- [出征62项](evidence-09/deploy.txt)：保留已有库存/舰船/空数量范围/输入错误/取消/旋转/双击唯一提交检查，新增搜索空态完整可见与DTO气力、射程、装备、各战法成本核对；正式读档恢复权威全部字节。[详情](evidence-09/authoritative-details.png)。
- [开局62项](evidence-09/opening.txt)：真实覆盖保存、取消覆盖/读取、反复打开设置、震动/速度、势力确认取消/返回、新开局与正式读档；2D/3D预览作用域及返回释放。是自己的5580/190基线回归；玩法方5554的旧opening失败仍须其在最终合并包重验，不以本结果覆盖原失败。
- 同一组自动档/手动槽3/自建库[SHA一致](evidence-09/save-after.sha256)。手动保存发生的文件时间更新属于实际保存操作，不能将字节一致表述为文件元信息完全未变化。

## 性能与构建

13次真实界面查询：[原始采样](evidence-09/preview-timing.json)，首次92ms，后续12次0–4ms，中位0ms（毫秒计时分辨率，非零计算成本）。第六批未缓存时13次108–276ms。此次并非隔离基准，明确改善连续查询停顿；首个查询仍需要全局面复制，不能宣称全部帧率或首次交互无停顿。GameSession要求创建线程调用，UI没有擅自把它移至后台或维护第二权威World。无ARM实测。

本批**构建必须使用 `-I out/uiux/contract-v2.init.gradle`**，详见[依赖说明](CONTRACT_BUILD.md)。只复制G的346份core/API/runtime文件到自己的out，逐项SHA验证；root初始504保护文件不变。[完整性](evidence-09/input-integrity.json)。自己的352项伴随输入含G源码、来源manifest、v2 init及4个原生库，位于out/uiux/iteration-09/build-inputs.tar.gz和build-inputs-manifest.json。没有覆盖原会话代码，也未指向其构建输出。

冻结权威专项在自己的目录实跑DeploymentPlan570、DeploymentSession412，合计982通过；架构边界和168固定美术资源检查通过。

- 最终x86_64验证APK：out/uiux/iteration-09/app-uiux-run03.apk，82243400字节，SHA256 `aabc60f5269e32d14051fb8be3845f4c4ea869a557a6847dde4a6f9586c58418`。对应出征run02、开局run03；源码身份9d45db9加本批UI变更，依赖G。
- 成都run01使用先前app-uiux-run02.apk；DeployWizard与最终版相同，其后只补ArmyUi/WarUi的实际部队战法可用条件绑定。每个APK均单独保存，不混称同一次安装。
- ARM64构建：out/uiux/iteration-09/app-uiux-arm64.apk，82186241字节，SHA256 `db789a1edf26bd0fb535ffabe07298d980fc6ada44e61ddd77ba4a6760405b80`。只含arm64-v8a，worker/unicorn逐字节核对继承库，资源检查通过；**没有安装运行或ARM性能结论**。
- 源码快照：out/uiux/iteration-09/source、source-manifest.json，与本批build-inputs.tar.gz配套。
- 录像：run01/interaction.mp4（成都），run02/interaction.mp4（出征），run03/interaction.mp4（开局）；各自frames提供5/15/25/35秒实际PTS解码样本。已查看出征35秒帧和成都关键截图。所有触控源记录保留。

继续推进普攻、战法、计略、运输及其他内政、高级外交、地图手势和长时交互；首次查询、原生全国加载、更多屏幕与ARM仍有验证或优化工作。完整UI/UX目标未完成。
