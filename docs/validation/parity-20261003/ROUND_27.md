# R27 正常城市固定经验与功绩

继承完整26081c5源码/4105文件330178686B，含R25位移、R26行情/v35、v34人物分层、全部dirty/168固定资源/4原生输入。app、3D、声音文件没有在本规则增量中修改；PC目录只读且没有Wine。整体goal active。

原命令/属性getters和四固定XP调用700完整3MiB/RNG观察继承，新增原XP→50功绩/60000封顶20组原实际调用完整对照。新的原字节/SHA、来源与各观察范围由inspect_pc_city_experience.py写city-experience-native.json；原三执行者/所有生产量/任务时点/伙伴倍增/培养仍分开未完成。原即时生产5c65b0..5c67读取演员能力后发XP/功绩，随后5c63d0重新读取当前智力，工程输出量目前仍不含该公式，明确继续P06。

normal Strategy patrol/trainArmy/recruitSoldiers与World.produce即时四兵装已接XP2及native固定50/60000，采用已保存的base/growth/XP/current。所有拒绝/查询/重复不加奖/RNG。typed forecast加入不可变经验/current before/after，老构造器保留。v31—33未知base模式不创建Profile、不加XP/不改变旧100功绩；v34/35人物正常后续动作领取新奖，不重置既有状态或改变存档schema。详细边界见CITY_COMMAND_REWARDS_CONTRACT.md。

JVM CityCommandRewardsTest25669、Session64通过，实际正常命令/完整save/report/RNG、2经验与50功绩封顶/旧超额保留、base/growth/其他XP不变、预览无写、拒绝/重复/StateToken/event及多旬AI续行。最初拒绝夹具把己方武将置敌城，SaveCodec正确拒绝“武将城池归属错误”；改为合法世界的敌方活动回合，未放宽存档或规则断言，首次失败保留。

既有CityActionPlan1899、ProductionPlan756、CityActionSession140、ProductionSession216、人物29361、LegacyGovernanceSave24、新商人13384/会话76通过。checked-in黄金/历史失败保留，不据此称全套规则绿。该批生产代码构建90秒/76任务成功，主87044486B SHA `18895c0ac63b72c93ae79b6903b2d0087b69247a3b0dca3b77f14710836b2632`，test874912B SHA `eb4c541000a8b93815c30f41f1b7130ad17ecde3c0ea62268c5a665655688762`。同R26所有assets/content/lib659项原字节一致，4原生输入保留。

5554 API29 x86_64实际安装/全APK回读；新包opening64/110.58秒通过，原7文件完整恢复且无新增，auto82554269…99f0a9。同APK生产规则ART25669/24.21秒、会话64/1.91秒通过，probe SHA `1a804cde3caf2d62fa466903ffd1489916eab39d3715749a8d1d512e5e73f7df`只含7测试/fixture/enum-nest类，无生产类。首次D8因未包含CityActionPlanTest$1 nest mate失败、规则尚未运行，完整失败与用户/APK后验保留；补齐原测试编译的nest mate后断言全保留重验。

本批重新执行冻结R25生产jar与候选的同一真实v34四交易/四完整旬，8保存仍逐字节全等，未移用R26结果或做归一化。该具体序列不是巡察/训练/征兵/兵装生产奖励序列，不扩展为全部旧v34正常命令不变；有管理能力的真实固定命令会领取本批核实奖励。

真实AM写入的v33夹具另经固定SHA锁定，从原保存构造合法指令前提，冻结R25生产jar与候选分别执行四类真实城市命令/每类三完整旬，16保存逐字节全等。新工具verify_legacy_city_rewards_continuation.py保留源代码、两个jar/夹具SHA及完整输出，未把schema号改写伪造旧档，也没有序列化归一化。此为具体v33旧数值模式证据，v32还有24项现存夹具续行；未称最新包覆盖所有72份历史档。

当前没有把ART规则通过称正常界面领取奖励通过；具体巡察/训练/征兵/即时生产实装还需实际控件与正常建设/旬/读档。纯3D/SFX完成提交9d51706的app增量已可独立读取，但其同包回归仍进行；只按c19de29后完成增量顺序集成，不能复制其AP核心覆盖本规则。最终组合包必须重新构建、安装并验地块、声音、骑兵七格、多回合、退出和存档，不能移用上述单侧包或UI独立包结果。ARM真机未验，完整共享PC随机流/天气及16源官方启用/全开局、其他经验/培养、P01—P10/D01—D03继续未完成。

完整证据out/parity/city-experience-20261003；SourceRevision26081c5+本批dirty生产，之后只有测试/工具/文档改变。整体目标未完成。
