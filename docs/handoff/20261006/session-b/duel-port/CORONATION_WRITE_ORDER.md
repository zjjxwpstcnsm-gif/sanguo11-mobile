# 原登位写入顺序（session B）

完整原自然单挑/战役回调 v4：out/session-b/duel-ruler-execute-source0-v4.json，SHA 223ccf542586f974f0a06a681427c568299e779f4c293d0059343e01f7ea232c。原390帧自然终局、声明有效EXECUTE输入、原相机/对话注册构造，无数值/呈现跳过。1100人物、47军团、87据点前后快照；完整World恢复；RNG1141642616不变；131字节变化。声明部队与选择输入仍不等于正常出征/原GUI/APK验收。

原4b78f0先将旧君主517降为status3，再将新君主109由status3写为0、rank77写为80。4813e0实际清除force+8匹配军师；481570实际写force+4君主：不能采用先前颠倒的名称。force3君主517→109，军师109→-1。军团1主将517→109，当前继承者同在军团1；这一例没有跨军团合并或据点值变化。

原mask15忠诚名单在死亡清理前仍有14、313、517、558，排除新君主109；每次4a75a0 weighted=1。最终14 raw96→100、313 raw99→100、558 raw100保留。旧君主517也参与中间重算，随后死亡清理army/home/place=-1、status8、rank=-1、raw0、功绩0和任务清除。不能把最终raw0当作4a75a0结果；v5额外观察4b7c2c返回时的raw。v1错误排除旧君主的测试失败完整保留，未计为PASS。

PcRulerCoronation目前仅为纯当前计划：从保存的实际人物连接/行政驻点/军团/原相性关系绑定，复用PcOfficerJoinRules weighted算术，在完整脱离World上移除rank以取得登位后的current魅力；不写base/growth/XP、不抽RNG、不执行工程Lifecycle.crown。生产PcDuelExecution君主guard仍在。跨军团、玩家选继承者、无人继承势力解散、终局死亡关系/功绩/任务清理和正常命令APK均待闭合。

最终v5原返回观察SHA cc93585b103488f83782456ad54eece2ec70e85bbbba074cd9164aa9c0681e34，四次weighted1返回raw均100；World/RNG恢复、131字节变化与v4一致。正式core产物对照28检查PASS，包含实际被俘名单变化/未可信raw拒绝/完整cold。生产guard仍在，下一步应按完整原清理/跨军团/人控继承协议接结算，然后真实普通命令和APK；不能以本计划PASS声称闭合。
