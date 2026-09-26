# v112 P0 运行续作 / PARTIAL

当前 APK source `5f8997ebfef15f4680931d40532411a336506d0d`；最终证据 HEAD 另见远端与独立交付 JSON。详情见 [v112 实测报告](../reports/R00-R14-P0-v112.md)。

正式修复首次资产同步前的 site/unit LOD 选择；输入真实方法回归失败、候选通过。继承 v108–v111 有界流水线与 Window 恢复；不把继承成果计作本次新修复。

API29/API35 独立冷启动首CPU检查均到达，但全国3D readiness均FAIL。API29原R12初始ready失败，API35原R12在重建ready失败；完整纯触控新开局链未通过。API35完整权威一致性12组合PASS，50对完整存档字节复核一致；focused月份原整屏正确，但API29实际日期故障仍未关闭。20轮生命周期完整验收NOT_RUN；ARM64真机NOT_RUN。

未闭合项继续 FAIL/NOT_RUN；213条原文与历史状态保留。以下全部为历史，不代表 v112：

# v107 当前续作入口 / PARTIAL

当前 APK source `a4b09058c6b1ab8c80f57d96f1e22425673de94c`，v107。详细正式修改、原 v105 独立前测、候选结果及未到达操作见 [R00-R14-P0-v107.md](../reports/R00-R14-P0-v107.md).

本轮削减全国重复法线/材质计算并合并远景批次，全部几何和规则字节保持；增加真实队列、CPU、提交与Surface采样诊断。日期实际画面、完整纯触控链、20轮设备生命周期、完整模式RNG矩阵仍未闭合。原213条不因主机套件通过而自动升级。

v106新增API29 CPU超时在v107全国ready回归中通过；API29实际UI停绘、API35原R12下一旬超时仍FAIL。完整触控链NOT_RUN。详见当前报告最终结果表。

以下为v105及更早历史，保留追溯，不代表当前APK：

# 当前续作入口：v105 / PARTIAL

先读 reports/R00-R14-CONTINUATION.md、DEFECTS.md及213项清单。生产源码 ef54951dbb7056beaa782f97a3486eefa3a88ef9，最终文档/证据HEAD另见交付REMOTE_DELIVERY.json及实时远端。v103首次布局后全国fit、v104有界上传、v105相机/布局先于首个CPU任务均已接入正式路径，未建立第二套加载器。保持同一分支/PR67，不回退、不合main、不force push、不进入R15。

最终v105：API29 focused=0/R12=1/cold=1；API35 focused=0/R12=0/cold=1。API29地形worker102327ms，之后GPU地形177齐全但景观pending494/473；日期整屏仍一月。API35冷启动录像从剧本加载进入2D预览，但触点未切到3D；新增epoch断言未到达。继续优先排查API29全国ready与实际窗口日期：用原完整R12和pm-clear冷启动分别复验；不得借用v103 API35曾通过当作最终候选通过。日期有原APK和候选两份独立整屏陈旧证据，decor/Window可见而窗口帧不前进、3D仍出帧，尚未证明是纯采集故障。v104 API35 Quickstep ANR只用于该次对照分类。之后补冷启动后出征/移动/攻击/战报/旬/手动存读完整真实触控链，再做异步生命周期、V1/V2、R10/R11/R13原清单。

CI36108363433保留v105精确APK和原始运行结果；CI36109036703保存v104/v105原始证据的分片，按序连接并核对TRANSFER_SHA256，tar内逐项核对RAW_SHA256。v104 Release发布403，独立重录下载404未运行，不应继续引用不存在的Release或宣称采集修复已通过。最终制品与证据由本次交付独立文件提供；保留损坏录像并标注，禁止用缩略图替代整屏原图。

以下是上轮v102历史交接，仅供追溯，不是当前结果：

> 续作更新：以下保留 v102 历史记录；2026-09-25 在实时 a5119 之后的正式相机修复、v103 与当前复验见 [R00-R14-CONTINUATION.md](../reports/R00-R14-CONTINUATION.md)。不要将历史结论或旧 APK 当成本次结果。

# R00–R14整改续作交接 / PARTIAL

先读当前reports/R00-R14-REMEDIATION.md、DEFECTS.md及evidence/R00-R14-remediation-checklist.json；原EXECUTE_PROMPT和原提示词在交付ZIP的input中。继续同一串行分支/PR67，先读取实时远端，不回退到这里的检查点，不合main、不force push、不开始R15。

已交付正式修改：TerrainSurface有界全国半格缓存、FilamentMapView静态势力层缓存；精确R13资产保护。保留SceneRenderGate、原生Filament、架构优化与全部规则/地图/资源内容。APK生产源码96719e971d74aa2b3f18d66a7f23832148a6dc8b；后续abca2dd2f97924528ce9d1cbc3a63d3571bf6ef5只增加原APK无录屏复验，最终文档HEAD以远端/交付REMOTE_DELIVERY.json为准。

## 先继续三个实际阻塞

1. 日期/UI窗口停止绘制。精确v102：fixture1/7/4权威与TextView均正确，onDraw9/WindowFrames8停住，Window PixelCopy无backing surface，Surface仍更新，两类整屏仍一月。原发布badd APK原文件、无screenrecord的独立控制同败（onDraw77/WindowFrames76）；不要再把它未经证明地归给录屏/单截图API/模拟器。排查MainActivity可见decor/窗口Surface及traversal/合成恢复，保留完整屏幕与原生Surface、帧标识。禁止测试专用重绘/硬编码月份/重启页面掩盖。之后补真实读档、跨月跨年、播放结束/跳过、Dialog、前后台、重建矩阵。
2. 正常冷启动入口相机。NativeColdStartInstrumentation从pm-clear后的正常启动页真实触点进入184剧本，第一次2D/3D后span15、局部视野；ScenarioFactionPicker.mapFit与MapHost首次切换需定位。原R12自行fit全国后通过，不能代替此入口。不要缩地图或用已有auto.sg11绕过。
3. 完整触控开局链。本轮cold探针在“UI overlay continues drawing live labels”严格断言失败，选势力确认之后均NOT_REACHED；保留断言并修正式UI路径。闭合后再延长现有真实触控探针至出征、移动、攻击、战报、下一旬、手动存/读档；原R12的commandFlow含API操作，不冒充全触控。

## 然后按原顺序继续

复用SceneAssetQueue/SceneWorkQueue补CPU纹理/图集/rig异步、引用/取消/上传回调/驱逐/过期worker回灌及20次真实生命周期；Filament对象仍owner线程。再做V1/V2真实地形/材质/城港关/景观/四季修复及PC近中远双向和连续运动验收。V1未闭合、V2 FAIL，不能全国扩散。R10允许刚体动画，但可见脚滑/漂浮/靠岸仍需修；R11补CALM/EXTINGUISH和全事件播放矩阵；R13做UI编辑/头像/新剧本链和DocumentProvider能力内的取消/撤权/断写/冷启恢复，不能把openOutputStream(uri,"wt")视为原子替换。

## 可直接复用的验证

- `bash scripts/test-native-r03.sh`：原R03及新增全国饱和缓存回归。输入47e5的负例为“national sweep evicts first sample”。
- `python3 scripts/check-native-protected-assets.py`：精确298资产+4历史材质源/编译物哈希；不得删成忽略assets。
- `.github/workflows/native-remediation.yml`：构建精确SHA、16主机、独立完整core输入/候选、原R12、聚合采集与冷启动。原ready120秒未改。
- `.github/workflows/native-date-control.yml`：直接下载badd已发布原APK并核对b4deb413…哈希，只重建测试APK，无录屏。当前机械断言绿色不等于日期像素通过。
- 最新APK CI36097729262：host成功；Android机械focused=0、R12=0、cold=1；整屏日期FAIL。原R12 checks60370包含轮询。Release是PARTIAL候选。
- 控制CI36098809236：旧APK原文件，机械PASS79、实际视觉FAIL。原始PNG和完整日志均在交付包。

全core两份exit1同CoreTest.logistics:75继承AI失败，不能改AI/删断言冲绿。无ARM64真机，本轮NOT_RUN；SwANGLE日志、视频、PSS结果不能替代手机，No process found不是零内存。

本轮没有留下待接入的渲染半重构；生产修复已接正常路径并构建安装，未闭合项明确保留。最终证据ZIP包含输入原包、源码patch、所有原始截图/录像（损坏段标记）、日志、退出码、APK身份、213条结果与远端复读记录。

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
