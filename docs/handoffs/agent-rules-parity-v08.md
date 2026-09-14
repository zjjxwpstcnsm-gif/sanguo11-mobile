# Agent 1 · rules-parity-v08 交接

分支 `agent/rules-parity-v08`，固定父提交 `ff624b6e9c48c81d7111a9a642b18cf6bd098fe1`，存档从 v6 升级为 v7。本交接随验证推进更新；尚未完成联合集成。

规则清单、来源、近似与明确未完成项见 [RULES_V0_8.md](../RULES_V0_8.md)。本分支不是全玩法或全特技100%完成。

## Agent 2 接口

- 稳定武将/据点/势力ID原样保留；不按显示排序重编号。
- `World.Officer.skillId` 默认 `none`，`Skill.find(id)` 返回已知定义或 null；`Skill.label(id)` 对未知ID直接显示ID，绝不丢掉。静态定义请使用 `Skill.*.id` 或其显式 `san11.*` 字符串。
- `World.Officer.sex` 为 `World.Sex.UNKNOWN/MALE/FEMALE`，未知不作为全男性队伍处理。人物静态加载时可设置；旧档不会自动注入。
- `World.orders.previewMove(unitId, hex)` / `execute(plan)`；`remaining(unit)`；`MovePlan.path/cost/remaining/error/valid()`。过期确认必须重新预览。
- `War.plotCost/plotRange`、`Campaign.researchGold`、`Skills.produceAmount/productionTurns` 为 UI 唯一预览来源。
- `Campaign.cancelProject(officerId)` 使用原项目时钟，校验当前势力占用，无退款。
- `World.Unit.movementBudget/movementSpent` 为保存的运行时状态，不能由地图刷新重置。新旬由 `UnitOrders.reset` 处理。

`ScenarioData/ScenarioCatalog`、资源、全国地图、MapView/MapCamera、OverviewUi 未修改。MainActivity 只改空格点选移动确认、部队剩余移动/编队特技入口、基础生产按钮去除过期产量、PR6后续的 selectAndFocus 部队激活和制造任务计数；没有重排导航。

## 存档与真实夹具

v1～v6 的原有布局不变。v7 在 ArmySave 后、日志前追加 RulesSave：标记0x52554C37；单位数及每个稳定单位ID/移动预算/已用移动/燃烧源倍率；火场数及每个地块q/r、源倍率、火种标记；武将数及每个稳定武将ID/UTF skillId/sex ordinal。读取验证重复或不存在ID、数量、预算、ID格式。静态数据缺失不抹除未知技能；既有 scenarioId 保留。

`core/src/test/resources/legacy-v6.sg11.b64` 是修改编码器前，用固定基线的真实 SaveCodec.encode 生成，原始2167字节，SHA256 `5647187fb47c6548ca422db8770bc05c7b3abfd58fcbdc9b09c8012606acf733`。包含三将投石/斗舰编队、冲车制造、枪兵研究、水军培养、运输、火场、混乱/着火、固定RNG `0x66778899`。生成器随验证文件保存。测试迁移后继续跨旬结算并对照保存恢复结果，绝不使用v7编码器伪造v6档。

## 基线与验证

未修改基线本地核心全部通过；UI模型64断言通过。原始输出见 `docs/validation/v08/baseline-*.txt`。

固定基线 Android Actions [34792195302](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34792195302) 的构建/Lint通过，但安装点击测试失败 `UI action not found: 执行`。PR6后续至 `d89f9882ded3e4d9324b9fce59e40d8a543c9cf7` 修复部队激活、制造任务计数、点击同步与编队确认兵装说明。本分支已逐项纳入，未改换固定基线。

现有回归保留。原断言的必要修正：存档头从6变7；普通移动不再结束行动；兵器攻击城市改为对应战法；测试模拟新旬使用完整预算重置。移除气力影响物理伤害及普攻耗气后，战术内核十队金标从13轮改为12轮；保留相同种子重复、输入重排与伤亡守恒检查，新增藤甲减伤应在兵数截断前应用的濒死边界。未删除失败测试。新 RulesParityTest 目前575断言，包括纯预览、取消/非法状态不变、预算/水陆转换、组合技能与防御优先、移动后攻击/入城、真实v6任务、确定性三势力长局与重载。

首轮本分支CI 34793890292 构建/Lint通过；安装测试暴露旧火计测试在突刺击退后仍从两格距离施放，本轮按PC基础射程1格修正规则后必须先移动至邻格。测试改为真实点击移动再施放，保留火场与气力断言。

Android 三种横屏安装操作、APK摘要与签名：等待修复后CI，完成后更新。未使用ARM真机，不能给出真机结论。签名未比较前不保证覆盖旧版安装，也不建议卸载有重要存档的旧版。

Agent 2 分支 `agent/national-content-v08` 的实际代码尚未合入；联合验证待对方 PR 可用后在独立集成分支执行。
