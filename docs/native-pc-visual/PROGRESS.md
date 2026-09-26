# v112 P0 运行续作 / PARTIAL

当前 APK source `5f8997ebfef15f4680931d40532411a336506d0d`；最终证据 HEAD 另见远端与独立交付 JSON。详情见 [v112 实测报告](reports/R00-R14-P0-v112.md)。

正式修复首次资产同步前的 site/unit LOD 选择；输入真实方法回归失败、候选通过。继承 v108–v111 有界流水线与 Window 恢复；不把继承成果计作本次新修复。

API29/API35 独立冷启动首CPU检查均到达，但全国3D readiness均FAIL。API29原R12初始ready失败，API35原R12在重建ready失败；完整纯触控新开局链未通过。API35完整权威一致性12组合PASS，50对完整存档字节复核一致；focused月份原整屏正确，但API29实际日期故障仍未关闭。20轮生命周期完整验收NOT_RUN；ARM64真机NOT_RUN。

未闭合项继续 FAIL/NOT_RUN；213条原文与历史状态保留。以下全部为历史，不代表 v112：

# v107 当前进度 / PARTIAL

当前 APK source `a4b09058c6b1ab8c80f57d96f1e22425673de94c`，v107。详细正式修改、原 v105 独立前测、候选结果及未到达操作见 [R00-R14-P0-v107.md](reports/R00-R14-P0-v107.md).

本轮削减全国重复法线/材质计算并合并远景批次，全部几何和规则字节保持；增加真实队列、CPU、提交与Surface采样诊断。日期实际画面、完整纯触控链、20轮设备生命周期、完整模式RNG矩阵仍未闭合。原213条不因主机套件通过而自动升级。

v106新增API29 CPU超时在v107全国ready回归中通过；API29实际UI停绘、API35原R12下一旬超时仍FAIL。完整触控链NOT_RUN。详见当前报告最终结果表。

以下为v105及更早历史，保留追溯，不代表当前APK：

# 原生 PC 视觉进度 — R00–R14 整改复验 / PARTIAL

2026-09-25。本次修改正式 `MapHost` 与 `FilamentMapView` 的首次原生预览相机适配、首次CPU任务顺序与有界上传预算，保留已交付的全国采样缓存、静止势力层缓存和R13精确资产保护。新增 API29/API35 的原APK对照和实际窗口诊断；全部阶段仍未完成。

当前交付APK源码 `ef54951dbb7056beaa782f97a3486eefa3a88ef9`，版本105 / 0.105.0-native-first-work；本次从实时 a5119 续作，正式修改首次原生预览的布局后全图适配和4ms/最多8块的上传预算。旧缓存和资产门禁修复是已继承成果，不计为本次新增。继续同一串行分支 / PR67；保留原生3D、MapHost/SceneRenderGate及已合架构。没有合并main、force push、其他PR操作或R15扩展。APK之后的提交只涉及复验/证据，不能当作APK源码。

| 阶段 | 整体 | 本轮结果及未闭合范围 |
|---|---|---|
| R00 | PARTIAL | 精确源码新APK、签名/ABI/资产核验和原始证据链；完整启动故障矩阵未闭合 |
| R01 | PARTIAL | 保留遮挡暂停；20轮真实生命周期、异常初始化及资源全异步未闭合 |
| R02 | PARTIAL | 原投影与拾取主机回归通过；多视口/安全区/方向/动态分辨率真实触点矩阵未闭合 |
| R03 | PARTIAL | 全国高度缓存淘汰已修并做输入/候选对照；没有新增山脊/谷地造型，连续LOD动态美术门槛未闭合 |
| R04 | PARTIAL | 材质资产保持；V1未闭合，未用雾或模糊掩盖 |
| R05 | PARTIAL | 拓扑/港口规则保持；阶梯岸线和真实船行验收未闭合 |
| R06 | PARTIAL | owner线程部分纹理/图集CPU工作仍存在；异步/取消/损坏/重建完整矩阵未闭合 |
| R07 | PARTIAL | 没有本轮城港关模型修复；地基、朝向、入口遮挡及PC对照未闭合 |
| R08 | PARTIAL | 没有本轮林缘/栈道/农田修复；原动态视觉缺陷保留 |
| R09 | PARTIAL | V2仍FAIL；未扩散全国模板 |
| R10 | PARTIAL | 刚体动画符合允许的实现路线；全兵种装备、脚滑漂浮、靠岸和LOD实拍未闭合 |
| R11 | PARTIAL | 全事件（含CALM/EXTINGUISH）、暂停/倍速/跳过/重建完整矩阵未闭合 |
| R12 | PARTIAL | 分轮原R12结果见当前报告；最终v105 API35原R12通过/冷启动切3D失败；API29原R12及冷启动均预览超时；完整触控链、日期/覆盖层仍不能认证 |
| R13 | PARTIAL | 旧资产门禁已改为298项精确哈希与4项历史合法材质白名单；真实UI编辑/SAF失败恢复闭环未完成 |
| R14 | PARTIAL | 权威/月字段正确仍不足以验收；实际整屏日期陈旧FAIL，四季动态/PC/真机未完成 |

本次 CI36108363433 的16组主机套件PASS；输入与候选完整core独立同败 `AI uses deployment commands / CoreTest.logistics:75`，未改AI或断言。API29 x86_64 SwANGLE是模拟器，ARM64真机与30分钟长稳NOT_RUN；No process found不算零内存。

当前结果以 [本次续作报告](reports/R00-R14-CONTINUATION.md)、[上轮整改历史](reports/R00-R14-REMEDIATION.md)、[缺陷](DEFECTS.md)、[213项复验清单](evidence/R00-R14-remediation-checklist.json)、[manifest](evidence/R00-R14-REMEDIATION.json)、[续作交接](handoffs/R00-R14-REMEDIATION.md) 为准。逐项复合验收不由局部主机通过自动升级。原总审计原文另存history/R00-R14-audit-PROGRESS.md与对应DEFECTS；原阶段报告保留并增加当前状态索引，不倒填历史PASS。

最终v105 CI36108363433：API29 focused=0/r12=1/cold=1，API35 focused=0/r12=0/cold=1。API29实屏日期仍FAIL；v105录像14/15可解析，一段损坏保留。Release发布403，独立APK及六份原始运行ZIP已核验交付，不链接不存在的Release。

## 2026-09-26 原始要求全量验收（本次结论，保留以下历史记录）

**R00–R14是否全部满足原始开发要求：否。** 正式APK source `5f8997ebfef15f4680931d40532411a336506d0d`，APK SHA256 `6ba2211d961567b2aa396ba19e3018c1dc44780fa4351a807db099fc643bd058`，v112原包复验，未改生产源码/资产/规则。

原213 + 补充222 = 435复合来源条款：PARTIAL=172；FAIL=16；PASS=9；NOT_REACHED=5；NOT_RUN=233。原文、历史、调用链和逐条证据见 [完整报告](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/blob/agent/native-pc-visual/docs/native-pc-visual/reports/R00-R14-FULL-ACCEPTANCE.md) 与 `evidence/full-acceptance/matrix.json`。

当前API29生命周期：FAIL：模式切换日志17/20，前后台0/20；下一次ready超时；API35：FAIL：模式切换日志20/20，前后台13/20；background frame loop stopped。原CI108325858061最终是20次切换/18次前后台后FAIL，不再保持未出结果状态。两API独立全国冷启动及原完整R12初始ready均FAIL；完整纯触控新开局未通过，下游NOT_REACHED。兼容测试探针的API29/35权威fixture各12组合、50对完整字节一致；不是纯触控、PC美术或真机验收。ARM64真机NOT_RUN。

V1/V2、R10/R11/R13缺口未关闭；本次正常平原Surface确认R05阶梯岸线缺陷。core原42调用双方12退出0/30退出1，继承失败保留，无规则修改。最优先修复正常全国预览CPU→上传→beginFrame→Surface链，再修生命周期/日期实际合成。保留MapHost/SceneRenderGate/有界队列与缓存，不开始R15。

CI：36222182595（原v112严格复验）、36222424271（仅测试API29兼容修正，全部成功）。唯一测试改动为完整流读取替代API29无readAllBytes；不降断言/超时。完整证据包含原始PNG/MP4、logcat、全存档字节及哈希；详见交付manifest。


## 2026-09-26 本轮独立复验：原v112，不是新生产版本

**R00–R14是否全部满足原始开发要求：否。**

463条：PASS9 / FAIL20 / PARTIAL174 / NOT_RUN256 / NOT_REACHED4。原213原文和历史不动，补24漏联规范段与4精确验收澄清。以 `evidence/repeat-20260926/` 为本轮结论；旧full-acceptance是702feccf历史。

API29/API35全国预览与原NativeR12失败；API29月7/月4整屏仍1月，UI draw95/Window89冻结而3D变化。生命周期本轮为API29 0/0、API35 20/1后后台停帧断言失败；历史20/18不是20+20通过。两API新parity各12组合、50对完整存档字节一致，但只是fixture。纯触控尾链NOT_REACHED，ARM64 NOT_RUN。原core两边同一logistics:75失败且42调用矩阵相同；20HOST门禁不能替代core/设备/美术。

APK source `5f8997ebfef15f4680931d40532411a336506d0d` / SHA256 `6ba2211d961567b2aa396ba19e3018c1dc44780fa4351a807db099fc643bd058`。生产、规则、资产不变，保留原APK与开发签名；未开始R15或合main。
