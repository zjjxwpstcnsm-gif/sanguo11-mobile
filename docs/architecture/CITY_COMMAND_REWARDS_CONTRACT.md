# R27 城市固定经验与功绩契约

正常巡察统率+2、训练武力+2、征兵魅力+2、即时枪/戟/弩/军马生产智力+2来自所给PC EXE固定实际调用。原经验方法3000封顶、刷新当前能力；同一原命令随后给50功绩，60000封顶。700原XP/缓存与20原XP→功绩完整3MiB/RNG观察分别登记，不把方程/源块检查等同正常安卓界面。

`OfficerExperiencePlan`只保存primitive before/after，`requestedAmount=2`为已核实固定奖。stat 0/1/2/4对应统率/武力/智力/魅力；stat=-1/amount0表示本操作没有本批核实的固定奖，不据此宣称PC没有任何奖励。`managed=false`时XP before/after=-1、当前值保持原值，旧数值模式不反推基础值。基于当前保存的base/growth/XP、年龄、官职、内助和伤病计算currentAfter；不对已知人物身份重新绑定来源。

CityActionPlan的OfficerEffect只给实际执行者相应奖，目标/旧太守或其他人员不虚构经验。ProductionPlan仅对即时四兵装返回智力2；原器械/舰船入口先登记type2任务，没有该即时XP调用，完整任务期/结束奖仍未闭合。本批不修改其既有工程任务奖，不给它们套用即时奖。原三执行者、生产量/工期/军事设施修正及完整许可仍待校准；现有输出量仍工程规则，不能把经验子项通过扩大为全部生产一致。

normal Strategy与World.produce及typed宿主共用同一奖。先取得既有命令的资源/效果计算输入，再成功加XP/刷新能力、功绩/20AP与一次行动；拒绝、预览、重复、宿主忙/旧session/token不得领奖或消耗RNG。原即时生产随后重新取当前能力算产量，当前工程产量没有智力项；该完整原产量公式仍待接入，本批不宣称顺序/产量全等。

Game API新增不可变OfficerExperienceChange，原CityActionPreview.OfficerEffect、ProductionPreview.Effects构造器ABI保留；旧构造器experience=null，新query提供明确模式/奖/当前值。只有成功CITY_ACTION_COMMITTED/PRODUCTION_COMMITTED提交一次事实和revision；UI/音效只消费结果与journal，不反写经验或另算奖，不参与规则RNG。

v34和v35均已保存人物分层，后续真实固定命令领取核实新奖，未自动迁移已有XP/base/成长/身份。读写schema不变：v34仍旧行情模式写34，v35保存新行情写35。v31—33未知基础值/旧数值模式继续原100功绩/无新增经验，避免用丢失基础值重算人物；本批矩阵及实际v32夹具续行覆盖。历史超60000功绩保留原值，native固定命令不给额外功绩，不截断读入或减少旧余额。

R26的冻结R25-v34四交易/四完整旬8保存全等是R26候选的特定证据。本批v34已管理人物在后续NPC正常固定命令中也会获得新奖，不能移用那8份结果为本批全部旧流程不变。严格对照工具/断言保留，不做字节归一化；任何差异应登记来源和实际字段，而不是修改旧预期。
