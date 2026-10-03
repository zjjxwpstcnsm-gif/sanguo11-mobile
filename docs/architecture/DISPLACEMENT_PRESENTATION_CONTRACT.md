# R25 强制位移、地形查询与声音事实契约

本文件是 `PARITY_UI_CONTRACT.md` 的增量。交接 AP 的完整源码已在本会话独立目录逐文件核验，冻结提交 `a6b87bd`。没有复制旧 AA 核心；UI18、v34人物状态及4份被忽略的原生构建输入全部继承。

## 位移许可

`War.tacticPreview(actorId,targetId,tactic)` 是纯读；`War.tactic` 是正常规则入口。陆战与水战的强制落点共用 `Displacement.stepError`，`World.cityAt(to)` 使用权威 axial 坐标和完整城市七格。目的格属于任何据点时强制位移停止，归属不提供豁免；关港仍为单格。此检查涵盖目标推动、骑兵跟进、突破落点、熊手退路和击破后的跟进。

普通 `Army.moveCost` / `SiteFootprint.mayStep` 保留友方据点通行和城池易主后的向外撤离许可；显式进驻继续使用 `SiteFootprint.entry`。强制移动不等于进驻，不转移城库存或删除部队。旧档已有城格部队仍以原身份/位置读取、保存，不静默迁移；新的强制落点不能把重叠加深。普通向外通行仍可用。

骑兵身后受阻时保留现行主伤害、一次行动/气力扣除和独立单挑判定；熊手必需退路受阻则保留原拒绝语义。本批没有改命中、伤害、碰撞数值或 RNG 顺序。具体 PC 阻挡伤害、单挑概率和碰撞算法仍待原程序取证，不因本修复声明已精确对齐。

## 提交与事件

app 的既有兼容入口通过 `GameSession.legacyView` 获得分离草稿，预览不得把草稿修改当作权威结果。提交经 `GameSession.legacy`：先校验草稿关联的 `StateToken(sessionId,generation,revision)`、busy与生命周期状态，成功才复制候选、递增revision、发一次 `LEGACY_COMMITTED`。过期、已消费和规则拒绝没有新提交事实；过期/已消费草稿的闭包不执行。

声音可对 `GameEvent` 使用 `(sessionId,generation,revision,kind)` 去重，只消费已提交事实。战斗明细消费已成功规则调用生成的不可变 `TurnJournal.Event`，以 `event.id=journalId:sequence` 去重；若演示批次需归属，在app队列入列时关联成功提交token或完整回合票据。失败草稿产生的临时journal不得入列，不能仅见到事件对象就认定提交成功。

`TurnJournal` 的编号是瞬时演示身份，不写入存档，不作为跨进程持久化ID。重开/读档须清理旧演示与声音队列，冷启动不把恢复局面当作新战斗播放。预览、旋转、重复快照、重复收到同一批事实不重放声音；静音、焦点和后台处理只改变播放器状态，不改变规则或 RNG。

演示的 `applyVisual` 只作用于独立渲染World，不能写GameSession、SaveCodec或权威RNG。声音与渲染不能重算伤害、判定战法成功或根据文本推导资源变更。本批会话测试验证了一次提交、重复/过期拒绝、唯一journal ID、演示不改权威、正常回合及读档边界。

## 占格地形查询

`GameSnapshot.terrain` 为只读完整地形编码，索引 `r*width+q`；`SnapshotQuery` 遍历全部坐标，不因单位占格省略地形。`snapshot.layout` 是该快照对应的坐标转换，`snapshot.state` 是读时token，不能混用旧坐标/新世界。

查询前校验 `0<=q<width`、`0<=r<height`，VOID/非可航水等原地形编码保持原值；单位与据点属于独立实体层，不覆盖terrain字符串。地图点选、单位格“查看地块”的优先级属于app，显示同一快照的地形事实，不把UI选择写回规则。会话测试验证被单位占据的格子仍返回PLAIN且不变更保存字节、token、事件或RNG。

## 证据边界

JVM和同一安装APK ART：城市位移6648检查、会话19检查。新包开局64、3D行军77、v34交易99、建设多旬61共301项独立实装检查通过，所有7用户文件恢复且无新增。源规则与实际3D城市位移触控、可听见的音频输出是不同验收；新UI的纯3D/占格入口/音效/2D移除仍由另一会话完成，最终组合包必须重建重验，不能移用本批包或旧UI包结果。
