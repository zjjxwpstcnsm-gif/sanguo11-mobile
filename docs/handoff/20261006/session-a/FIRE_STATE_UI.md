# 格子火状态可读性与原控制器边界

新增PC地图的持久信息标记直接消费已发布MapSceneSnapshot.FireState.hex/remaining：所有可见真实燃烧格显示橙色中心标记，近景最多32个文字牌显示原remaining旬数。标记不受暂停/低画质/UiMotion开关影响；新snapshot没有该格或remaining≤0即不绘制，不自行推算到期、造火、判成功/伤害、调用RNG。设施已有complete/remaining/hp/maxHp/burning标记继续读取权威投影。

这是状态UI补齐，不是原PC火焰粒子还原。原模板/控制器/材质/位置/寿命/火计连锁、施工/受损原演出仍待原caller取证；保留pcMap分支对旧CombatVisual粒子的禁用，不将工程火焰标原版。未做新包正常火计/存读/灭火/到期实际验收，不以此宣布格子火目标完成。
