/goal
目标：继承最新完整main，修复“部队带足携金且周边合法仍不能设置军事设施”的正常玩法阻断；全局排查规则预览/提交/保存不一致，完成剧本与有效武将权威还原、完整单挑与舌战正常战役闭合，接入菜单、新局、文字页面、存档和实际APK，不能停在数据表、原函数取证或演练界面。

当前本地main基点：ef413be3653820dd6449ba7f02aa60bed5b26ef5；完整已合并源码：/Users/paopao/.codex/worktrees/scenario-main-closeout/sanguo11-mobile。首先阅读该目录docs/handoff/20261006/parallel-repair/{AUDIT.md,CONTRACT.md,APP_OWNERSHIP.json,TOOL_OWNERSHIP.json}与三张screenshots；核查git status、README、适用AGENTS.md、progress.md、docs/architecture/MODULE_RULES.md、PARITY_UI_CONTRACT.md、docs/PC_PARITY_STATUS.md及20261004/session1、session2最新收尾。继承审计目标后继文档提交和任何已完成的新main，不回退基点。PC只读参照：/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版；禁止Wine、网上表格或记忆替代原真值，外部Windows Documents/Expansion只有未知，不再要求另一副本。

建立自己的完整继承隔离目录，保留全部未跟踪源码/资源、168固定构建输入及4份忽略JNI，逐SHA核验，独立Gradle/build/out缓存。原/Users/paopao/workspace/sanguo11-mobile旧HEAD和609项dirty不得reset、清理、覆盖或从旧HEAD另起。现有磁盘约2GiB余量，复制前核实，不通过删用户资源解决。并行严格按CONTRACT唯一文件所有权；公共台账不并写，计划、契约、增量、验收与未知只写自己session目录。持续自主推进，定期中文报告；完成批次在自己分支提交，交付共同基点/提交/确切路径/前后SHA/JNI资源守卫与冲突说明，不合另一会话WIP。

分支codex/rules-content-contest-repair，记录docs/handoff/20261006/session-b。你拥有core、game-api、game-runtime、data/content、权威数值文字资源、B转换工具/测试，以及APP_OWNERSHIP.json的16个B命令页面：FieldworkUi/ArmyUi/WarUi/ContestUi/BuildPicker/DomesticUi/GovernmentUi/DiplomacyUi/CampaignUi/StrategyUi/AbilityUi/LifecycleUi/ContentUi/DeployWizard/CargoWizard/CustomOfficerPlacementUi。MainActivity/MapSceneSnapshot/MapHost/新局势力picker/主题/头像音频/3D归A，不编辑；你提供API/样例/精确适配请求，由A改其文件。新文件与共享脚本按CONTRACT先登记确切路径。AndroidGameBridge序列化/Unity/4JNI冻结。

优先关闭用户阻断，再按证据推进原还原：
1. 在正常新局出征携金部队复现军事设施失败，记录sourceId、部队/军团/行动/任务/状态、实际u.gold、原格/目标格/方向、terrain/占地、前置技巧与具体拒绝code。检查Fieldworks.buildError→sites→地图选择→方向→确认→GameSession提交完整链，预览/正式执行必须同规则、同坐标。核实城市七格边缘的据点距离限制是否比PC过严，FieldworkUi补充携金按中心distance==1而withdraw按canEnterSite是否隐藏合法站位，正式确认是否挡住旧StateToken/双击。不能因金额够便放松全部限制，也不能将七格城市退回一点。提供按格子/条件可理解的真实失败原因；合法格真实扣携金、占行动、施工、多旬完工/修理/中止/破坏/存取都正常。不用只调用buildError证明页面可用。
2. 全局命令巡检：军事/内政设施、补金/补修/运输/出征/进驻/移动/攻击/计略/外交/官职/人员任务/研究/编辑与培养，核对列表可见性、资金所在、资源/AP/军团作用域、距离占地/路线、预览vs正式提交、过期/双击/取消、延迟任务和多旬结算、Save/RNG/StateToken。修复有证据问题，并记录待取证项，不盲改旧规则。B页面消费A统一主题接口，页面按钮与地图错误的实际数据通过契约交给A。
3. 剧本：先读docs/pc-data/README.md和scenario-metadata/officers/placements/tail/ownership台账，复用原serializer/loader/oracle；逐源记录文件/编号/SHA/解码/加载优先级/稳定ID，闭合名称日期、可选势力、君主/军团/太守/都督/国号统兵、城市关港归属库存、设施、武将所属所在地身份/出场条件、初始部队外交与开局事件。16源不能强并差异，9工程剧本不能称官方复刻；官方/MOD/自定义/备用与未知明确区分。原读部队零消费不等于事件不生成部队；原加载AP0不等于实际开局预算。候选28 native58/国号5原13000失败必须修复并重新原对照，不直接启用封存27/28候选。
4. 武将：逐有效人物覆盖姓名/字/性别/生卒/出场/所属所在地身份忠诚、base/growth/XP/current、六适性、特技实际效果、关系/配偶亲属义兄弟亲爱厌恶、官职功绩、原传记和其余原信息；逐字段来源/未知，670注册人数不是全有效覆盖。4原字形身份已核实，继承稳定runtime ID与canonical连接，不按native编号猜项目ID。额外/古代/事件NPC先验证激活身份；原文控制码/字形未解不造传记。两次导入字节一致，正常武将列表/搜索/详情/新局同权威DTO。
5. 完整单挑与舌战：现有单挑215帧/16合及健康写入只是取证，补原完整规则、人控、AI、三将支援/换将、装备特技/逃跑、终局胜败伤病俘虏/部队战役结算、存档DTO/API/正常触发。舌战继承实验39人控/卡牌/AI/怒气反制/保存冷续行，闭合真实触发准入/费用功绩/先手装备、登用及外交回调、一次性生产终局；工程金100/AP10/+100功绩不能当原真值。须玩家通过普通命令进入并完成胜/败/放弃与保存重开，不只演练或原VM测试。未知和替代显式保留。
6. 为A产出只读契约：稳定officerId/nativeId/sourceVariant、来源/当前人物字段、火格位置寿命/状态、设施施工/完成/破坏事实、StateToken、真实事件id/parent及发言者/原voice profile、原地图scene/势力据点地域日期关系。渲染不能改规则或重抽随机数，人物metadata与media manifest分别输出按ID连接。A完成适配后顺序接其冻结增量，不能自行改A文件。

不可回退：v34后base/growth/XP/current分离、年龄官职有效内助伤病生命周期与普通命令经验，编辑培养写base并保留身份经验；31–33不能反推base，34–39各已存策略不强制升级/追填，新内容只影响明确新局。旧档不静默换人/搬城/改值/开新能力策略。继承商人行情、生产/延迟奖励、骑兵七格强制位移拦截、普通友城通行/主动进驻、异常旧位置及Save/RNG/StateToken边界。兼容变更先形成显式策略并证明旧档完整续行。

验收：每源真实菜单新局→多个势力与武将→普通命令/军建/火计/单挑/舌战→多旬→存取全World/双RNG→退出重开；旧31–39策略/已有设施施工/历史自定义存档完整续行，不拿局部算术替代流程。运行必要core/session/bridge/建筑/架构检查；继承Unity U01旧黄金mapRevision63/65/terrain/坐标失败须查来源语义，不能盲改golden或Unity掩盖。B优先5582先核实空闲，A5554占用不可抢；每次安装前完整备份全部保存/库/偏好并最终恢复读回SHA，不清数据。每版APK独立构建/实际安装/SHA，x86_64与ARM证据分别注明；旧批次通过不能移用。交付可安装APK、完整源/可复现导入工具、逐源/逐人物字段覆盖差异/未知、来源manifest、旧档与完整流程证据、明确未完成项；只交完成批次给最终集成，不能以表格通过宣布完整还原。
