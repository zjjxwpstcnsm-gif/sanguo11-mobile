# 正常单挑首输入阻断：原支援字段按实际分支读取

真实source0/player28完整25旬保存8bafff0e…9194c：裴元紹10503/native503从平原库存正常出征SPEAR3000/粮30000/金1000，正常地图移动邻接敌方单位6，实际估计95%接受；原对局fighters[503,-1,-1,411,669,-1]、phase3/sub0、8frames/0inputs，完整当前保存ef0cbcd28b326ead3a7768dc55ce68de151eb10e6fe84b4464bdfafe1d437b4b。首个typed输入被“当前原始忠诚变更尚未核实”拒绝，完整现状保留，非相邻单位夹具或造终局。

Saved raw loyalty未知保持：張郃411缓存98/display98→当前92；裴元紹503缓存98→当前94；盧植669缓存100→当前96，均trusted=false。正常季节/工程修改刻意使原raw失效；禁止从界面倒推原字节、补true、改其数值或直接跳过原忠诚读取。Loyalty/Raw策略本身以及未知衰减/褒奖量不在本修复中冒充原真值。

现有原508890完整port的rawLoyalty只由“非义兄/配偶、非讨厌对手、讨厌主将、主将是君主/其owner有效/同势力、round>=4”分支读取。现有生产SupportBinding却在每次phase3/sub3给全部六slot事先读取原raw，尚未评估实际分支，包含当前active和普通officer主将。这是多旬后潜在的过早准入，不是所有单挑都确实需要raw。实施前先用完整原508890/读取地址观察，在实际411/669/503来源及声明round/raw边界隔离“不消费原字节”的事实；不替换getter或原条件。

若原证据确认：SupportFacts仍保持旧已绑定raw数值接口，生产增加纯lazy raw getter，原条件真正进入需要raw时才调用现有严格PcDuelRawLoyalty.current。未消费不会补字节或用dummy0代替，确实需要且未知仍同IOException拒绝；kernel仅将原已使用的raw表达式换为getter，PC条件/概率/RNG顺序不改。读取错误经campaign统一转IOException以维持typed事务/同原因/不提交部分帧。父Save/PDU/Model DTO/Bridge/Unity/JNI格式不变、旧1–7读写不升级；未知raw及全部原关系与角色保持，旧档不追填。

实施前SHA：
- core/src/main/java/game/sanguo/core/PcDuelKernel.java 5b0415bae5cdeb02ca46f34349429598f033ba230aa699e77c9fd4a5b3e79b84
- core/src/main/java/game/sanguo/core/PcDuelBindings.java f24a52f6c5e5db2fd7ce876e048d1a47686f0f357e7266841e96eb75543869d4
- core/src/main/java/game/sanguo/core/PcDuelCampaign.java 3474353da326db64ac9e84d57eee31d70bca3ee86b0a9cb7b075991b59f45df0
- core/src/main/java/game/sanguo/core/PcDuelChallenge.java 04896800c6ebdc31301e4dbc000412d7147cbd7a4ffd05a3872ba3efb11f6194
- core/src/main/java/game/sanguo/core/Contests.java 265f1702f638b85f862b92135cadb85631d025f83ae53eaf9f32337fc372aab4

原需要raw的未知分支、当前季节/奖惩等全原规则仍待闭合；不能以本非君主分支解决全部未知。普通accepted对局首输入/完整支援/人控/AI/终局/后续三旬/保存冷仍需在这个真实保存继续验证，不重开、补忠诚或调随机数。本策略不是Native全目标完成，正常A新局与APK仍等待冻结后继。

## 原证据与当前实战续行

原完整508890实际411/669/503来源、声明round0/2/4/6/15与raw0/96/100/120/255共25case通过；所有候补ByteAC读取hook为零，同轮原bool结果/原RNG完全相同，全原3MiB World/RNG还原。收据3a19c232c4e23c619bd4cee86a1436aa232d13f7879d9d6c988af25b23032421，未替换原条件/getter。仅证明本非君主分支不消费raw，不当忠诚衰减/奖惩/其他分支原还原。

已实现SupportFacts纯lazy raw getter和supportCurrent；旧明确数值构造/support接口仍要求0..255。只有原508890实际raw表达式调用现有strict current；未知仍为未绑定−1/getter而非dummy0，且确实需要时IOException在随机抽取前拒绝，campaign统一checked转换，typed事务不装入部分帧。RawLoyalty字节和trusted flag没有补填，Source关系/角色/条件和原随机顺序没有改。58检查核对原25cases、实际raw未知/World双RNG纯性、需要raw的明确合成分支拒绝/零随机、轮数与义兄优先级、已知值原chance/RNG等，全部通过。

普通真实对局从ef0cbcd2…保存原样续行：先前13k三将剑部队被击破的25旬不回滚；裴元紹实际新单将槍3000/粮30000/金1000、正常库存/移动/邻军95%接受，当前v5由保存人控推进33输入/177帧/10合，真实winner1/native411，败方native503 manager2自然阵亡，没有改结果或冷起新局。一次正式战役回写/新旧token重复拒绝/fullWorld双RNG保存冷，随后3完整AI+人员+后勤至28旬61检查通过。终局原数据3fd1e3d2…、完成81c9485a…、28旬5243e26a…文件SHA完整见后继guard。不是声明相邻部队或原VM数值端点；仍是Host普通流程，A菜单/新版APK/ARM未验收。

连续原完整initializer/model/manager/RNG与每帧保存127682、人控换将67111当前类回归通过；实际format5/genuine39独立字节兼容、536首次目录、Session1690、正确Bridge、架构/168均通过。Android28tasks48s生产编译、后继tests28tasks20s/5tasks15s通过。当前必要raw未知分支仍保留严格拒绝，忠诚衰减/褒奖原量仍未闭合，不能以lazy未消费成功把unknown标成已知；整Native/16源/旧31–39全矩阵/菜单/APK/U01/ARM仍未完成。

实战奖励readback最初测试硬写WARXP+10漏掉盧植669的实际指导特技，失败results-v1保留；纯 before/after 核对張郃 merit650→750、WARXP2→22，盧植21241功绩/0XP保持、裴元紹100功绩→0并DEAD。继承单位指导倍经验策略无需支援已到场，未改生产奖励以满足错误断言。补核结果以results-v2后继为准。探针初版访问不存在Officer.skill而编译失败，xp-v1误跑旧已编译探针，明确不算新XP核验；改用现有skills.has API后xp-v2真实编译运行，日志保留。
