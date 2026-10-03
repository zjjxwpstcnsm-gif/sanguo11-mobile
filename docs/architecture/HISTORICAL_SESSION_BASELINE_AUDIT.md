# 历史会话基线与当前规则验证（2026-10-03）

历史固定基线仍是 `9548bb350051150b21a61213f9068ffb1b7506c0`，`baseline-9548bb35.txt` SHA256 仍为 `687eacf3232cd11f89116716ec50af7dede6cecec40be40eb18f560a95204a37`。没有更新哈希、删掉或降低原完整状态断言。

旧源码与它的资源仅归档到独立 `out/parity/historical-session-fixture-20261003/archived-test-source`，用于历史测试夹具，不是项目开发基线。没有 checkout/reset，也未替换当前游戏源、资源、用户存档或 UI。`tools/content/build_historical_session_fixture.py` 重编译真实旧 core，并让相同的 BaselineSequence 在旧 native 与旧 bridge 路径执行：7 个完整状态精确复现原哈希，含序列化 RNG。

生成的完整 v33 夹具 227372 字节，SHA256 `b27c526efeb53c443e921a24aedc34ab78b9f645c199154710d292ed0b08340b`，只放在 game-runtime 测试资源中。当前 SaveCodec 对该夹具 decode/encode 字节完全相同；不反推 base，不增加 XP/市场/生产配置，不移动旧单位。

原测试拿今日 ScenarioCatalog 新局去比较历史开局固定哈希，混用了输入。现在历史断言使用真实历史完整夹具，同时新增当前新局独立验证。历史 `verifySession` 仍失败于首次征兵续行的旧固定哈希，不能称为已修复整体架构门槛。

具体差异已审计：首次征兵的整个 payload 只差一个 AP 字节，历史源码扣 10，继承项目经 PC 原代码核实扣 20；把诊断副本的 AP 临时换成历史 AP 时完整字节与旧哈希精确相同，诊断后立即恢复当前 AP，未向任何游戏存档写回。下一次巡察历史治安 +10，继承原规则为 +5（以统率及原条件计算），并同样扣 20 AP。现有源验证、原断言和失败日志均保留；不能为满足旧工程值而把核实的 PC 规则退回去。

`CurrentScenarioSessionTest` 独立执行当前新局 native/direct/bridge 全量存档与 RNG 对照、拒绝、去重、会话隔离、线程、生命周期、回合、协议和坐标：1666 项通过。它与历史门槛分别注册，`check` 仍依赖历史 verifySession，未把历史失败从门槛移除。当前通过不能覆盖或冒充历史通过。

续行诊断工具 `tools/content/architecture/HistoricalSessionContinuationProbe.java` 与历史 writer 仅读写 out 中的隔离测试文件。原产出的七份完整存档 SHA 已逐份与原固定列表核对。新增源调用台账 `tools/content/inspect_pc_technique_calls.py` 固定 EXE SHA，仅盘点 52 个原技巧点直接调用；不能将静态请求操作数直接当作最终奖励，也未声称覆盖间接调用或完整命令入口。
