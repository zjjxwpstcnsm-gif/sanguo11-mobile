# 第十二阶段：生产与粮食交易权威接口

本阶段继承完整S/R及UI13（8e1d5f7），不修改app，不集成UI14未完成工作。PC安装目录未写、未启动Wine。以下是当前模型接口和行为兼容性工作，不将项目制造、价格、工期、10AP等值标作PC已校准。

## 交易

新增TradePlan、TradeCommand、TradePreview、TradeQuery。正常Campaign.trade和typed命令共享同一校验，数量非法仍返回原输入、千粮价格、范围/步长、资源容量及共享旬额。effects只在允许时提供实际钱粮/AP/武将功绩与行动变化。预览全存档/RNG纯净；同token重复执行不能二次扣款；忙/关闭/读档/跨线程均有验证。

TradePlanTest919、TradeSessionTest88检查通过。独立工具PcTradeCompatibilityProbe在完整R重新编译的类与当前类分别执行222命令、44完整旬，266行结果（消息、成功/失败、全存档SHA与RNG）逐字节全等：52efb1d8c9c44a78635efc325541732a485325743d8893d761a206f20d5c598b。证据out/parity/trade-contract-20261003。

既有verifySession失败于exact old-scenario starting state。完整R全部core/API/runtime Java独立重编，运行R自己的测试与资源时，同样在原GameSessionTest:30失败；更早完整N对照也有相同首个失败。原断言未改，不能宣称总测试全绿。

## 制造

新增ProductionPlan、ProductionCommand、ProductionPreview、ProductionQuery。World普通兵装生产、Army器械/舰船生产保留原命令入口，校验改成共享结构化RuleFailure。preview/execute请求指定EQUIPMENT或SHIP及原枚举item字符串，错误不通过detail解析。当前仍单执行者。

返回费用/AP、设施种类/总次数/剩余次数、即时库存、容量和同类在制品；允许时返回实际即时变化、产量、是否延迟、工期和任务标签。普通兵装立即入库；器械和高级舰船先登记制造任务，在工期结束时入库。outputQuantity是计划产量，不能提前记为库存；拆除/失守/取消/后来库存占满等会影响完成。取消仍为旧officerId入口，稳定任务ID与typed取消契约尚未实现，本阶段不伪造任务ID。

ProductionPlanTest756、ProductionSessionTest216检查通过，包括4旬完整存档重开与RNG对照、2/3旬能力差异、100库存与在制品合计边界、设施次数、研究门槛、重复命令和会话生命周期。PcProductionCompatibilityProbe在完整R与当前生产类执行132命令、108完整旬，240行全等：3140b2796599ea00c73f78bb79a2b9107d4b6361386b92e7b33e01bd103bc1e6。证据out/parity/production-contract-20261003。

开发中三个夹具失败原日志保留：剑兵不允许库存；错误水格覆盖七格城池；会话夹具造船厂未临水。均修正夹具成合法局面，没有削弱游戏验证器或正式断言。最终核心/会话专项通过。架构与git diff空白检查通过。

## R实装复测与后续安装边界

R同包同正常190在5554同AVD重启后，运输126.20秒再次失败于首行主将未写入，名单仍开、尚未派遣。两次分别记录225.81和126.20秒，不以系统driver stall作为唯一解释。installed-transport-190-host-retry含5附件、instrumentation和system日志；finally原7文件全部恢复，无新增，auto/manual3 SHA82554269…f0a9。重启前后7文件也一致。

该测试结束后关闭本側空闲5554减少主机资源竞争，保留AVD数据。UI5580及其进程未操作。新交易/制造接口晚于R/S，必须另冻完整快照、构建、实装，不把R功能通过当成新包通过。ARM真机仍未具备，整体玩法和数据对齐仍未完成。

## T冻结与之后的原行动力校准

T完整快照3876文件283412344B，全部源与冻结工作区SHA/大小一致，含168固定资源和4份原生输入。T保留上述行为完全兼容版本。主/test构建1分3秒76任务成功，168资源核验通过；主87019125B SHA7e069745b9145541b9d1f759aba105e4477ef5c975b6cbc03d0bf681fe0ac0c8，test仍960270f9…b5bc。封存production-trade-build-20261003/apks；T尚未安装，不把R通过归入T。

随后发现原程序生产两个扣费入口5c67a4与5c7119、商人5cad0f都传20。原48f1a0查表证实编号2为生產、4为商人。test_pc_city_action_costs新增原入口测试，执行真实5b9340→4a1820→47e3e0，24个新增AP边界逐次对比3MiB内存仅军团AP字节变化；总4项3.763秒通过、60扣费边界。0/9/10/19/20/21/60/255处观察原扣费饱和到0，不把绕过前置校验后的执行片段当作完整原命令许可证明。

生成器已增加PRODUCTION/TRADE20常量和两生产调用地址；Java与JSON重复导出全等。正常World/Army生产、Campaign.trade、各自typed预览与提交同步使用20，保留当前使者结算特殊处理；AI的生产/买粮尝试增加相同预算门槛。军团AP完整范围、生产其他公式、商人价格/额度/数量步长仍未校准，不能扩大到整体PC一致。

新319项PcEconomyCost边界、ProductionPlan756/会话216、TradeSession88、Territory674通过。TradePlan最终920通过：三笔正常交易实际用尽60AP后，单独为“额度拒绝”夹具设置合法20AP来隔离AP先拒绝；不修改普通游戏预算或历史断言。第一次在零AP时期待额度原因的失败保留。另一次80AP初始夹具虽执行后落入合法范围，已改为从合法60开始并新增归零断言，最终日志为trade-quota-valid-ap.log。

PcEconomyDebitDeltaProbe在完整R与新规则各执行72个正常命令和72完整旬。只在对照工具中、旧正常命令成功后额外扣10AP，逐字节比较其余全部存档/消息/RNG；144行完全一致SHA6fae8bbbea9392c486cb7eb91a1393458dcf1a28e6aad5faaf1ddff3d223b9bf。这是明确预期规则变化的对照，不是声称改AP后和R原样相同。证据merchant-native-20261003。该规则改动晚于T，下一快照/安装包另生成。

## U构建与无窗口环境对照

U完整快照3880文件283434812B，逐SHA/大小和冻结工作区全等，含全部继承资源。U双包32秒76任务构建成功，主87019189B SHA37d46e7c7d69ac121967947f16dc5fe576e2818c3996ecc5051a4e0a2d9ab1af，test960270f9…b5bc，封存native-economy-build-20261003/apks。

本侧同AVD以host GPU/no-window/2048M/4核重启，不清数据；原7文件和重启前全等，emulator-headless-u-20261003/verified.json。先保留R包、同test及同正常190输入复测运输，installed-transport-190-headless 58.95秒通过，原测试不改：真实主副将选择、输入错误、核心拒绝、确认返回、旋转、Home恢复、重复点击只提交一次、派遣后任务、正常读档和全表单取消均通过。11份证据拉取；原7文件最终全等，无新增。前两次首行点击失败仍保留，这支持环境参与，不能归为全部触控/性能问题已经解决。

U主/test实际安装并回读SHA一致，独立run=u13-army-190-01在72.39秒通过61项：正常190真实兵舍→两旬→征兵/训练→读档。9份附件已拉取，原7用户文件全等、无新增。

U新开局首跑u13-opening-01在5.44秒被存档保护断言拒绝：调用者漏传reuseSlotSha，尚未覆盖任何存档。失败日志保留，原7文件恢复全等。补齐原始槽位SHA后，同包u13-opening-02在65.11秒通过62项正常新开局/保存/取消覆盖/设置/旋转/完整读档检查；原7文件全部恢复、无新增。证据installed-opening-pinned。不沿用R结果，不削弱保护断言。

U之后只新增工具/审计：原5ca620商人报价390组有符号数量/能力/城市7c字节及32位饱和边界通过（3.735秒），全部3MiB世界内存不变。原4890a0返回武将173字段，原城市7c进入分母；16剧本42城市共672条源记录的7c均50，逐源SHA、记录SHA、读取偏移和内存解码吻合，merchant-state-native.json SHA2e800044174035c82e616b472d84c13c9a2904f625158ba91d0d63eeed961ba4，重复生成一致。开局/每月价格状态更新与完整许可、商人已行动标志仍待核实，不据此把50硬写运行时或改当前价格。该工具晚于U，不改变已冻结U生产输入。
