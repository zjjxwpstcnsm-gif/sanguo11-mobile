# 同地域直接释放：显式当前策略

实施前策略。原完整4b1950，地点描述采用既有完整单挑回调观察的[败方部队、胜方部队、1、0]连接；source0实际558普通武将/517君主，原三构造器与598630→59a4b0→599cf0完整人员阶段。明确声明部队、地点和释放选择输入，并非正常出征或GUI证明。

有效取证：out/session-b/same-region-release-source0-v3.json SHA aa03e1884c736bd35a5505b3be22d45dac61464c48c22e95159d98d680e448f3。释放后army1/home21/current21/status原3或0/task37/duration0；下一次人员阶段task-1/duration0，其余连接保留，原RNG1不变。v1地域127哨兵越界失败、v2把据点对象误作地点描述的无效输入结果均保留，不能作为生产依据。

仅PDU3且PGO3的明确当前策略可以直接释放到同地域。保存仍用既有返程格式1，但零时间行必须有当前能力策略、合法地域/驻点和人物连接；保持busy/acted直到下一次人员结算。旧策略及缺少策略的保存不自动开放或补行，不改既有任何字节。历史非零返程完全沿用。

共享validateReturn/startReturn的押送解散及登用零时间分支仍拒绝；本证据不能开放这些回调。驻点失陷/军师都督/其他来源、原全局控制器、自然完整战役和本批APK均未闭合。

改前SHA：PcDuelRelease.java ba73c0b9c8c6fb16b16c6846e2208269f84ff423154c70b44361257ba87ed3b0；PcDuelSettlementSessionTest.java a80d2dd887428f13bf1056214c1d1ceec2ef97f2e92b28044b60d20a13921988。

新增零时间只接受城市驻点0–41且origin==home；同地域但关港驻点不自动套用城市抵达流程，明确保留拒绝。

文字DTO后继：既有OTHER_TASK原返程未设置otherTask标签，详情出现空名剩0旬；只读Strategy.nativeReturnPending由原保存行判定。OfficerQuery保持既有status字符串字段，非零“归队中 · 剩N旬”、零“归队中 · 等待人员结算”；不写规则/保存、不重抽RNG。改前SHA：core/src/main/java/game/sanguo/core/Strategy.java f289087b013a7b62a558ccb9aa624c24a17390b769e90ff4d3e2d40f6776877d; game-runtime/src/main/java/game/sanguo/runtime/query/OfficerQuery.java e5724fe5eb62033b233da5fc5b49440fd196ff7c60d017b02bc3a5750af3c7ab。
