# R25 城市强制位移与完整继承

整体目标仍active。本批从AP交接的完整4078文件、329563797字节继承到独立工作目录 `/Users/paopao/.codex/worktrees/f1b8/sanguo11-mobile`，逐文件大小/SHA与快照和原目录核对一致。分支 `codex/parity-rules-integration-20261003`，完整dirty源码冻结提交 `a6b87bd`；4份被忽略的原生输入另随manifest保存。没有reset、删资源、清模拟器数据或复制旧AA核心，PC目录只读，没有Wine。

## 规则缺陷及实际复现

正式入口为 `War.tactic` → `Displacement.execute`，预览共用 `Displacement.stepError`。冻结AP的原 `Displacement.java` 独立编译后，以真实骑兵ADVANCE命令把敌军从 `11,6` 经 `10,6` 推入其自身城市占格 `9,6`；该城中心 `8,6`，完整七格由 `SiteFootprint.cells`给出。旧预览也错误包含城格。原代码将普通友方城市通行的 `SiteFootprint.mayStep` 当作强制落点许可。

修复在共用stepError中直接拒绝任何据点目的格，覆盖七格城市与单格关港、目标推拉、演员跟进/突破、熊手退路与主伤害击破后的跟进。相同真实ADVANCE在新规则下只推一格，停 `10,6`，演员停 `11,6`。主伤害、行动、气力和RNG路径不增改；普通友城通行与显式进驻未改。

旧档已有城格部队按原位置和身份继续读取/保存，不删除或迁移；新的强制落点不能加深重叠。原相邻受阻的主伤害/碰撞/单挑语义仍是继承实现，本批不宣称它们已与PC精确对齐，未凭猜测更改损伤。共享边界见 `docs/architecture/DISPLACEMENT_PRESENTATION_CONTRACT.md`。

## 规则与会话验证

新增JVM城市位移6648项：七格18条外缘入口、4归属（含中立/第三方联盟）、枪兵/骑兵五类战法、一格/两格、最近合法停靠、城市中心/旧重叠、熊手必需退路、陷阱触发后第二步重验、部队/山地/水陆/塔阻挡、主伤害击破后不得跟进城格、普通通行/进驻保留、AI纯候选、重复命令、RNG对照及每例真实下一旬完整存档回放。预览不得消耗随机数或修改完整存档。

会话19项：真正 `GameSession.legacy`提交与普通规则完整save/RNG全等；一次revision/事实、过期及已消费草稿闭包不执行、规则拒绝不发新声音事实、唯一journal ID、独立演示应用不改权威、占格格地形查询、完整旬票据/存档/加载。

继承人物状态29361与战斗反馈209通过。旧DisplacementTest在 `city deterministic replay` 失败，新旧冻结规则同一首个异常；其后五章地形能力、陷阱火场、生死身份、运输和AI单独调用原断言全部通过，这不等于原聚合套件全绿。另TacticalLogistics、FacilityCombat、Core、CampaignAi及架构GameSession旧起局断言的6个首失败全部与冻结AP对照相同，记录 `historical-failures.json`，不降断言。架构静态边界PASS；完整架构脚本止于上述旧起局断言，没有冒称全部可执行架构通过。

可复现命令：`bash scripts/test-city-displacement.sh`，或Gradle `:core:verifyCityDisplacement :game-runtime:verifyCityDisplacementSession`。实际APK规则验证使用 `tools/content/verify_pc_android_city_displacement.py`，先编译注册测试和三模块jar，探针DEX仅3个测试/夹具类，生产core/API/runtime与资源均由真实安装APK提供。

## APK与实装范围

R25/UI18主包87040454字节SHA256 `335c37431b4ffccbbaa766950b2ecf34cb44a59d9002a511c0fdc55cf95f6e40`；test752267字节SHA `c6476d6136054e2b558e467127b725212b10c46e2411a5b18336564407e08d36`，构建85秒/78任务成功。该包SOURCE_REVISION为完整基线a6b87bd加本批dirty规则，没有冒称Git提交已包含修复。仅API29 x86_64 emulator-5554实际安装整包回读。没有操作UI独立设备或ARM真机。

新包初版城市ART6612项/29.99秒、会话19/.48秒通过。补充击破跟进和阻挡RNG检查后，串行UI批次结束再次执行最终城市ART6648/38.27秒及会话19/.54秒，用户文件/APK全等。最终探针SHA `0f8ad9943a6f6798502a39860d88f594b2377842f4402e012a02bbab4e490f5a`，仅3个测试/夹具类，无生产类。实际包人物状态29361/21.02秒、官职2244/17.89秒、功绩421/6.04秒、商人原算术11595/.36秒、原能力4813行及日期3120/.36秒全部通过。原商人算术仍未接实际价格，不借此声明普通交易已完全一致。

同APK正常ART开局生成190曹操player1/seed23 v34，236968字节SHA `636854cc2b5878cbd70bf05b71993c0fe2dcdee77405b4db7862c9ed206eaaed`，与交接正常开局字节全等。新局base/growth/XP/current、旧v31—33数值语义未被覆盖。666人物/16源10656记录成长重新导出两次，TSV/审计均与交接逐字节相同，保存growth-reproduction。APK的168固定资源、4原生输入、成长和官职资源全部与继承源全等；开发签名SHA256 `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`保持原签名。

开局/完整存取64项130.46秒通过。首次测试漏传已有slot3的reuseSlotSha，明确断言失败未进入新局，记录保留；按原runner所需SHA补齐后通过，未放宽断言。每次7用户文件外部备份与最终恢复字节全等，无新增，自动档SHA `82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9`。

同一新包正常190 v34已实际3D行军77项/204.70秒、交易99/94.90秒、建设军政61/82.85秒通过。真实出征/非法合法目标/唯一行军提交/正常下一旬、暂停加速跳过与完整存取；真实买粮/同旬拒绝/下一旬卖粮；兵舍正常建设至完成/征兵/训练/读档。与开局64共301项，四套分别外部恢复起点，并非一局跨套连续。每套7原文件完整恢复且无新增，截图/progress/result保存screens目录。不挪用旧214项、R23/R24或UI独立设备通过；耗时是本次非受控样本，不声明性能改进。

另会话的纯3D、占格入口、音频与2D移除尚未合入，该最终组合包必须重建重验。当前6648规则测试也不能代替地图点击“突进”的3D端到端用例，音效可听输出和退出/后台验证仍待其完成增量后的合包。

## 继续推进

原商人月价state、报价/数量及月初结算尚未接入。已继承原city+7c行情、city+9c瘟疫/蝗灾/丰收、原报价及RNG函数审计；需要保存行情/兼容模式、按政治经验刷新后的能力报价和数量上限、闭合月初外层顺序。非商人经验、原培养奖励、完整官方/MOD启用与16源完整开局、所有P01—P10/D01—D03差异仍待完成。九份工程剧本不能称官方复刻；整体目标没有完成。

完整本批证据目录为 `out/parity/city-displacement-20261003`，含真实基线命令失败、新规则/JVM/ART日志、完整用户文件tar、安装回读、所有首失败和归档APK。
