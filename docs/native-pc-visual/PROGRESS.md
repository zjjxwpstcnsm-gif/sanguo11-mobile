## 2026-09-29 v121 用户反馈续作 — PARTIAL

最终APK源码 b2352ed199fddada917ea224295773c4f9250506；APK SHA256 06ebe774afffb7f3022ec9ae6a6d2f191fdb6dbff814935e2e001aa5fbbaf4d1。连续山地/6关贴山、GPU网格、Blender7模型已接入；原300资源/规则/地图不改。新增手绘高度被压低的回归已修复并保留复现。

首轮API29局部旧171/候选151通过，12组画面显示山地/墙翼/火/网格；全国旧外部900秒超时、新120秒ready失败。最终API29旧局部106通过、最终候选局部/全国ready失败；最终API35四例ready失败。最终安装字节核验通过，不代表3D验收。回归风险OPEN，普通网格手机流畅度仍NOT_ACCEPTED。core.logistics:75双边继承失败，旧几何hash候选有意变化但原失败不抹去。

视觉仍有不透明烟火/尖锐岩石、装载中黑色关隘、全模型写实与PC差距；武关/剑阁/葭萌/绵竹邻域无山且不伪造封路。WIF身份条件拒绝，两轮物理提交0；完整触控/旧档SAF/ARM64/30分钟及历史缺口继续开放。没有合并main或启动下一阶段。

见 [本轮报告](reports/feedback-v121.md)、[交接](handoffs/feedback-v121.md)、[manifest](evidence/feedback-v121/manifest.json)、[307资产清单](feedback-v121-asset-manifest.json)。

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

## 2026-09-29 用户反馈 v122 — PARTIAL

见 [本轮报告](reports/feedback-v122.md)。正式修复栈道材质跨界，改善现有SWAMP材质，41个实际Blender模型接入城港关和13类部队正常加载路径。寿春湿地权威地图缺失仍OPEN；core基线同点失败；正常3D、参考美术和ARM64不得提前记PASS。

本轮最终验证：v122源a8a4eca79164a03f9e58ef379991c839be44968a构建PASS，219GLB零错误/警告。API29小场景四组实际Surface/网格/完整存档对比PASS；两版全国寿春/港口及API35全部就绪FAIL，ARM64授权拒绝/NOT_RUN。报告、资产清单和runtime-summary已更新，整体PARTIAL。
