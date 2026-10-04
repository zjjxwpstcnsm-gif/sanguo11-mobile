# 技巧点规则事实与表现契约（更新2026-10-04）

## 已实现候选接口

规则数值仍来自 `World.campaign.points(owner)`。本事件接口本身不制造奖励或扣费；后续 v37 原生产策略的奖励和存档边界见 `PC_TECHNIQUE_POINTS_PRODUCTION_POLICY.md`。`GameEvent.techniquePointsChanges` 是每次成功提交的净变化列表，每条只有不可变的 `owner/before/after/delta`。`GameEvent.id` 由 session ID、generation、revision、kind 构成；声音及 HUD 可用 `event.id + owner` 去重。列表防御复制并只读，预览、失败、过期命令和取消回合不发布变化。提交先安装完整状态，再发布事实，观察者不能重新入提交。

`WORLD_REPLACED`（开局、读档）重建基线，`CLOSED` 不制造变化。两者没有奖励动画／增减提示音。成功提交中没有数值变化时列表为空；不得根据按钮、文案或猜测的动作类型补点。一次提交内互相抵消的增减只发布净变化，这是本接口的明确边界。

`TurnJournal.Event.techniquePointsChanges` 在每个原有 checkpoint 上记录各势力实际前后值。仅点数变化也产生事件；列表、标量及事件 ID 在 drain 后保持不变。事件只在对应回合最终成功提交后允许演示；计算草稿、失败任务和取消回合不可播放。回合 GameEvent 是最终净变化，journal 是分阶段变化；同一消费者必须选择一条线路，不能重复消费二者。此列表不是声音指令，也不调用规则。

表现只读规则和事实。播放滚动数值、跳过演示、暂停、后台恢复、重放不得调用 `earn`、研究、存档写入或规则 RNG。`applyVisual` 仍只修改专用展示世界的地图实体；HUD 应消费点数事实，不从展示世界重新推导规则。

## 原程序证据

PC EXE SHA256 `30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb`，安装目录只读，没有启动 Wine。

- 原 `4b6460` 在势力 `+0xa2` WORD 上实际执行：正请求 `max(1, trunc(request/2))`，负请求直接扣除，最终 0–10000；返回实际变化。84 例包含饱和、0、正负请求，完整 3MiB 规则域仅该 WORD 变化，RNG 不变。
- 原普通生产 `5c674c` 经 `4b6580 → 4b6460` 请求 `min(10, floor(实际入库数量/300)+1)`。普通生产奖励及延迟制造完成请求 20 已在显式 v37 新局接入，统一原正请求减半／最少 1 与 10000 上限。旧 v31–36 不推断或截断。其他工程奖励及失城扣点仍待系统对齐；事件接口不会伪造缺失奖励。
- 原研究显示读取同一 `+0xa2` 字段（`610aac`，输出控件 `107b`），相邻原初始化为“技巧Ｐ”标签（`610fc7`，控件 `1079`）；实际 Big5 标签指针 `7e8ebc/7e9bb8/7eb9ac/7ec424` 均严格解码为“技巧Ｐ”。字段身份由这些共同使用路径支持；完整原 UI 启动未执行。
- 原 HUD `6317bf..63182b` 将实际值写入缓存 `+198`，保留旧值 `+19c`，变化标志 `+190`、计时器 `+194`。18 例实际原标量片段通过，只有构造的 UI 缓冲变化，规则世界／RNG 不变。
- 原 HUD `6318c8..6318f8` 是 `(new*t + old*(600-t))/600` 的整数滚动；35 帧原代码通过。静态上下文显示计时 1650 开始、2250 结束、600 范围，音效调用 `6318b2` 的参数 ID 为 33。编号的实际声音资源、计时参数单位及完整原界面回调仍待验证，不能据此声称移动端已播放原声音。

可复现工具：`tools/content/verify_pc_technique_gain_native.py` 和 `verify_pc_technique_hud_native.py`，在项目 `out/toolchain/pc-emulate` 与 `pc-inspect` 的 PYTHONPATH 下执行，各传一个全新 `--output`。原 HUD 测试先前未映射缓冲、重复映射和错误栈帧失败均保存在 `out/parity/native-technique-hud-20261003/`，没有降低断言或补假回调。

## 当前验证边界（2026-10-04）

冻结 v37＋已完成 HUD 组合 APK 的 SHA256 为 `f9c140ae2b7417ec96544f69411b118518135a7ab20ef19a259b0be9428ea6df`，其生产源码版本为 `0213f9d`。同包 API29 x86_64 ART 的十组生产／会话／事实测试合计 13323 次检查通过，其中不可变提交事实 26、原生产技巧策略 2717、typed 技巧会话 137。真实旧 v36 存档的完整再编码保持原字节，用户原 7 个文件全部精确恢复。新开局两次导出一致，保存版本为 37。早期测试读取旧夹具时 exit137 的失败保留；改为有界完整读取，仅改变测试输入读取方式，没有替换 APK 或降低兼容断言。

该包实际 HUD 验收 27 项通过（12.74 秒），覆盖提交后数值、去重、读档静态更新及奖励提示。正常建造／生产／制造／九回合联合验收仍未通过：一次运行在第五个制造旬的主线程队列 10 秒屏障超时，尚未完成全部九旬。不能把 ART 或短 HUD 验收替代完整流程。

现有接口只发布每次提交或 checkpoint 的净变化，尚不具备每次实际增减的独立 factId、cause、parent ID、完整 StateToken 与回放阶段；同一提交中相抵消的动作仍可能消失。HUD 当前消费提交状态中的权威点数，尚未按每条 journal 点数事实演示。两条线路不得重复发声，也不能宣称原 PC 音效 ID 33 的实际资源已经接入。

兼容复验包括原计略 journal 150、城市位移会话 19、普通生产会话 216 及 native／bridge 同规则、去重、乱序和过期提交。旧 `TurnJournal.Event` 构造签名保持兼容。当前完整新局会话另有 1666 项通过；历史 verifySession 的旧 AP10 固定哈希门槛仍失败，原断言保留。

CustomMaps.resolve 的新开局在地理应用结束后重建生产来源配置，避免新增／禁用据点使配置长度失配。修复仅影响显式新地图，旧存档读取和单位位置不迁移；MapEditorRuntimeTest 12 与 Continuation 131 项通过，v36 修复包 `35f096...149a` 的真实编辑器发布 69.41 秒通过。该结果仅对应其 APK，v37 最终组合仍须重新验证。

完整官方开局、有效来源身份、其他奖励和 ARM 真机验证仍未完成。PC 目录保持只读，未启动 Wine。

## R33逐次事实候选（未实装，当前APK仍a31）

`GameEvent.techniquePointsFacts`新增每次真实数值写入的不可变事实：owner／before／after／delta，producer cause，cityId／officerId，sequence，phase，完整提交StateToken，父GameEvent ID与可选presentationParentId。factId为父提交ID+":technique:"+sequence，不使用规则RNG、不写入存档。原techniquePointsChanges仍是净变化，保持现有UI兼容；同一次提交先+20再−20的两个事实保留，净列表为空。饱和后数值未变化不制造事实。原公式、数量、上限和旧31—36含义均不改。

现有生产／完成、研究／关系扣费、编辑、战斗／计略／设施、外交、回合领地、铜雀台／玉玺等所有规则点数写入已核对并标注实际生产者。未标注的显式Map.put写入按RAW_WRITE报告，不猜成原PC奖励。SAVE与ScenarioData初始化默认不捕获；查询复制不捕获，事务复制独立捕获，最终验证的复制只转移不可变观察值。before／after等事实有效性在安装authority前验证，失败候选不发布或漏进下一提交。

World.nextTurn按实际委任／AI／全局／玩家恢复阶段标记事实；TurnJournal.checkpoint保留逐次列表，零净变化也保留事件。写入时的pending journal ID成为原checkpoint父ID，提交API绑定最终StateToken。可以按提交列表或对应journal线路消费，不能把两条线路重复播放。CLOSED／WORLD_REPLACED清基线而不制造奖励；取消回合／过期票据／预览／规则失败没有已提交事实。

新TechniquePointsOrderedFactsTest59项真实生产／编辑净零／失败部分候选／非法临时事实／完整回合／原checkpoint／取消／制造完成与饱和通过；旧Facts26、typed原技巧137、当前会话1666、只读投影180、战报原字节75、原技巧2717、城市6648及bridge通过。新59中的一次journal测试曾误把两条Editor命令当作同checkpoint，实际reports.prepare分段；保留失败后改用明确同checkpoint原Map写入检验观察边界，真实Editor双命令的整提交净零检查仍保留。

这批目前只在本会话core／game-api／game-runtime候选工作区；没有修改app或另一会话文件，没有把a31实装报告当作新代码证据。HUD／声音当前仍消费原提交净变化／最终权威值；逐条事实演示、bridge schema1传输、回放去重消费及新组合构建实装继续推进。原PC奖励数值尚未闭合的条目也不因cause标签而变成已对齐。

## R40 BridgeSession Java提交事实边界

BridgeMessage.state使用完整提交token，techniquePointsFacts为防御复制只读列表；旧构造保留generation0，运行时使用实际token。BridgeSession只在提交delta或提交导致地图snapshot中携带一次实际GameEvent事实。主动snapshot、重复receipt、失败、restore／close和resync恢复不重播。队列预算含事实payload，超限明确resync且不回滚规则。

app-owned AndroidGameBridge JSON、Unity契约和提示消费仍未接；schema1常量不改，禁止把Java字段存在当作wire验收通过。待序列化字段为state{sessionId,generation,revision}与techniquePointsFacts[{id,parentId,presentationParentId,state,sequence,owner,before,after,delta,cause,phase,cityId,officerId}]；所有long直接保留整数，不经过浮点。按fact.id去重，同一消费通道不得再播放NET奖励。同包实装需重新验证。
