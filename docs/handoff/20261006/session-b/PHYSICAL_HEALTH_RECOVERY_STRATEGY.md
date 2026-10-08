# 原单挑战后体力逐旬恢复策略（候选，APK待验）

原EXE30d33b...只读：58051e→4a1180日期推进，5805a8→590c30，590c94→580e70伤病结算，5805d0→59c330全局结算；后者59c479–59c4f8循环1100对象。边界44输入receipt5b2d03100b5e434f54d1bcfe9dc6cc6aa2109c614e5ac857ebc3da59ae6fc768；阶段/状态receipt5aec18d3fe3fbb8ea966d77ca0d2237266546c2cf4d0a23bd34b5d228c0536c4。HP<100时+30、injury0/1/2/3上限100/80/50/30；HP100原跳过，不反压为伤病上限。原status0–5/7有效，-1/6/8无效。现有Source0–15 status6映射SOURCE_WAIT/8DEAD/7UNDISCOVERED，其他ACTIVE；自定义UNAPPEARED不恢复。额外未注册原NPC仍未知，不激活或猜稳定ID。

独立pc-duel-physical-recovery-v1记录精确来源/EXE/原receipt、采用turn与lastTurn。只明确四参PC新局初始化；已有保存缺失或未知namespace不自动升级。普通单挑终局页面主动采用，无即时HP/伤病/人物/模型/双RNG改变，仅后续完整全局旬应用。已有登用待办及原物品等策略不重绑、不重抽。保存容量使用现有32/16MiB真实putError，不删未知尾部。不改变PDU1–3/原体力保存format1/31–39主codec，不修改Bridge/Unity/4JNI。

接入Contests.tick在既有PcNativeHealthPolicy.tick之后、OfficerAbilities.refresh之前，按本次当前injury，所有状态查询纯读取；只对应一个完整global turn，重复调用相同turn不再恢复。lastTurn必须不大于World.turn；新策略损坏拒绝、未知magic保持opaque。单挑中不能推进旬。typed ContestCommand追加operation；只读NativeDuel recovery DTO提供enabled/error/adoptionAvailable，同StateToken。B ContestUi调用A既有executeContest/confirm主题，不改A文件。

完整原59c330因脚本接口对象缺失未跑通，其失败receipt及原World/RNG恢复保留；本策略依据完整原恢复循环与精确普通旬调用/顺序，不冒称原PC GUI全部执行。候选需实际旧档采用取消/双击、普通多旬HP与应战准入恢复、保存/冷重开及全World双RNG验收后才交完成增量；成功登用与全source/officer/其他分支仍须继续。
