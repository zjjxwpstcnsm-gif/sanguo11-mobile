# 格子火断路及原来源边界

FilamentMapView.animateEffects在snapshot.ground.pcMap非空时clearEffects后return，War.Fire→MapSceneSnapshot.FireState仍含hex/remaining>0真值，但旧工程格子火路径不进入。已有PcMapEffects仅从PcEffectProcess的冻结source-scene.bin读取8种SEFF模板与126固定原环境落点；它不是格子火生命周期接口。MapSceneSnapshot设施投影已有complete/remaining/hp/maxHp/burning，不能据渲染猜规则成功或伤害。

原SEFF索引8/9/16/17/18/19/20/23经实际413d20映到原133/134/141/142/143/144/145/148；该静态环境实例不是格子火的已证明控制器。需从 supplied EXE格子火/火计/火球火种火船/设施状态真实caller确认实例模板、mat、position、clock及取消/灭火。4JNI冻结，不改worker协议或将旧CombatVisual.fire称原版。此阶段保留已确认证据及缺口，原格子火尚未恢复。
