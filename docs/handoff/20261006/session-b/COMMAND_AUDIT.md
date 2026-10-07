# B命令巡检：证据层级与待闭合

共同基点0e7b9bc2，当前生产候选及build16。这里列出已读入口，不把静态审阅视为正常APK完成。GameSession1690/Bridge通过涵盖已存全World、事务、隔离、过期状态及协议；不是下面每页每分支的安装验收。

|命令|列表/预览/正式入口|本批证据|未闭合|
|---|---|---|---|
|军建|FieldworkUi→Fieldworks.sites/buildCheck/build|52语义检查、原类别距离双次同字节、18原费用、真实build14低携金扣500/施工/未完工保存|build16正常多旬完工/重开进行中；HQ折扣、原地域准入、补修/中止/破坏完整APK|
|携金|FieldworkUi→fundingSites/withdrawMaximum/withdrawCheck/withdraw|真实中心站位、取消完整save/RNG不变、双击只扣一次；137合法额度core|关港及小额实际控件、过期确认更多场景|
|军事補修|FieldworkUi.repair→Fieldworks.repair|静态：UI手写owner/距离/HP/builder过滤|UI漏掉project(unit)!=null且不是该工地，以及combatError；可能显示正式拒绝选项。需真实状态复现后共用admission，不放宽正式规则|
|内政设施|BuildPicker/DomesticUi→typed previewBuild与提交|入口存在、主题适配编译|实际地图目标、旧位置、撤销、延迟完工/破坏矩阵|
|运输|CargoWizard→TransportCommand/previewTransport/submit|StateToken及正式库存/路线预测入口|资金预留、取消/双击/在途、跨军团、保存冷续行APK矩阵|
|出征|DeployWizard→DeploymentCommand/previewDeployment|实际5000枪兵/携1000、选择武将、库存扣减由正式执行|三将/不同装备/军团AP/边界容量，native58修复后实际13000|
|移动/进驻|普通MOVE/GARRISON与A冻结精确坐标适配|实际城市中心停留+携金、旧MOVE与显式GARRISON语义边界；完整旧档初始字节守卫|旧32–37未来AI/RNG分歧显式记录；实际跨友城/主动进驻资源合并|
|攻击/战法/火计|WarUi/ArmyUi→同坐标Error/preview，city命中格分支|静态沿用、七格不回退|各目标类型、强制位移/火格寿命/链爆、正式随机及保存APK|
|外交|DiplomacyUi→surrenderError/aidError/exchangeError；正常使者任务|静态条件入口/预留返还说明|实际行程、失败返还、舌战回调/取消/存取|
|官职/人员|GovernmentUi/StrategyUi→Government/Strategy/Personnel|原native58容量getter独立复核，16源10720已连ID容量原型全部匹配；生产未启用|军师/官职手写列表与正式准入差异；国号5、太守、都督、军团、继任、身份激活、普通APK|
|生产/商人/研究|ArmyUi production/TradeCommand/CampaignUi researchError|继承typed报价、延迟制造/研究、旧策略；不强制升级|当前APK所有资金/AP/延迟中止/旬结算边界|
|编辑/培养|LifecycleUi/AbilityUi→研究/培养Error及base写入|沿用base/growth/XP/current、年龄/内助/伤病生命周期契约|本批实际编辑保存与正常经验/旧策略矩阵|
|单挑|ContestUi→Contests.challenge/duelMove；完整saveDTO|已有工程正常入口；原215帧/16合取证不能替代完整原规则|人控/AI/三将/装备/逃跑/终局伤病俘虏/部队结算与原对照|
|舌战|StrategyUi普通说服/外交；PcDebateCampaign原人控实验|39原实验续行已继承；生产native finish/abandon明确拒绝未核实结算|原真实准入/金/AP/功绩/先手装备/一次性终局，胜败放弃/保存重开；不能把工程100/10/+100当原值|

优先继续军建正常流程。本表潜在列表不一致尚未修改规则，不代表已确认所有命令无问题。原流程、正常APK与旧档验收逐项追加；未知须保留。禁止以局部算术、原getter、注册人数或演练界面替代完成。

## Later completed flow updates

Military17 is completed bounded source14 normal funding/build500/wall300/multipleturn/save/cold, described in MILITARY_BATCH.json. Capacity20 is completed bounded Source0/force28/native58 normal roster13000/once-only stock debit/three turns/fullsave/cold, described in CAPACITY_BATCH.json. These supersede the table earlier pending build16/capacity prototype status, but do not close governor/district/budget/activation or all16-page flow matrix. Repair list/formal admission mismatch still awaits normal-state reproduction.

## 2026-10-07 证据更新与当前版本边界

上表是 build16 时的历史入口审阅，以下更新其已经被后续证据替代的待办；不把不同 APK 批次结果计为当前 APK54 的通过。

|范围|已完成的有界生产批次|当前仍需闭合|
|---|---|---|
|军建与携金|APK17，Source14正常新局/出征/城市中心补金取消与双击/土垒300/营垒500/多旬/全保存和冷重开|所有16源、关港与小额、破坏；APK54已通过补修/中止、多旬、冷重开及15+772文件SHA全恢复|
|军建补修列表|本版2生产文件已统一repairCheck，独立父版本63检查/session1690/冻结bridge通过，旧档36行SHA一致|APK54独立完成实际正常页面、多旬、冷重开及全部15+772文件SHA恢复；未用含Duel WIP的APK50替代|
|出征容量|454…/APK20，Source0/势力28/native58普通列表13000出征、取消/双击、旬与存取|多源有效人物/三将与设备装备矩阵；不得启用封存27/28候选|
|军团与太守|451…/APK27，两个势力普通调任、出征、移动夺港、进驻、全保存/冷重开|完整官职/都督/国号统兵/继任/俘虏及所有军团任务|
|军团AP|833…/APK33，PAP1明确新局、两个势力、正常指令和旬AP/全保存|NPC42–46、完整预算与控制准入；旧策略不得追填新局AP|
|舌战|2039af…/APK31，普通说服进入人控自然胜败、多旬、模型/双RNG保存与冷重开|全触发/准入/原费用/装备先手/放弃/外交回调；工程standalone金100/AP10仍未知|
|搜索与登用|d852…/APK38：普通搜索发现/拒绝/可选舌战胜败/冷重开；1b7…/APK39普通独立人物登用成功/失败/原AP20金0/保存；292…/APK40事件Cause修复|无发现/宝物/关系/跨城/其他特殊触发与全部来源；不能移用这些批次验收替代54|
|原物品|APK46真实普通没收/赏赐/库存及保存冷重开已观察，但源码包含本会话Duel WIP，尚未作为完成批次提交|隐藏/潜在/事件物品、费用与全部效果，独立完成源码/APK批次|
|完整单挑|原连续模型、支援换将、胜败/俘虏/替换/部队结算、16源亲族矩阵及保存/API原对照仅属WIP|普通新局/人控命令创建、上游行动/费用、原部队战斗数值、终局UI、逃跑/放弃和全部特殊回调、实际APK/旧档正常续行|
|其余命令|已提交typed预览/正式入口与session/bridge边界检查继续保留|内政/运输/攻击火计/外交/商人生产研究/编辑培养各实际页面、资金军团资源、延迟与保存矩阵仍须逐项验收|

单挑上游原取证新收据明确：目标缓存record+4的attackMask与record+8的strategyMask不同。495170/79cc08气力表及5933a0计略派发不是单挑费用证据。原5a4990相邻敌军16项声明输入准入在气力0/1/10/100都允许bit8，仍不能据此跳过正式扣费与普通流程验证。

所有上述有界结果之外的未知继续保留。Unity U01来源语义失败未改golden，ARM与扬声器未验收。
