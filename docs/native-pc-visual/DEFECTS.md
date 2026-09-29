# 用户反馈专项 — v118 / PARTIAL

| 条目 | 状态 | 证据与边界 |
|---|---|---|
| FEEDBACK-DRAG | IMPLEMENTED / runtime NOT_REACHED | 正式MapHost回调和Filament手势接入原dropUnit；两轮API29输入确认旧版不移动，候选均初始ready超时，取消/单次提交/完整存档对等尚未运行通过 |
| FEEDBACK-FOREST | host PASS / visual NOT_REACHED | 同fixture树211→703，旧放置和留白全保留，新增LOD1；没有候选森林已就绪截图，不能宣称美术通过 |
| FEEDBACK-CONTROL | scoped host PASS | 同属性普通扰乱45%，普通螺旋命中后混乱15%；10000实命令分布符合；特殊必定/免疫保留，非原版数值校准宣称 |
| FEEDBACK-READY | FAIL / regression OPEN | 两轮API29旧PASS/新FAIL；API35新旧均FAIL。原120秒未放宽；新森林增量影响未排除 |
| FEEDBACK-MEMORY | OPEN | 96×96/44块CPU mesh估算51,306,808→121,352,376字节，不等于PSS/GPU/FPS，ARM64长稳未运行 |
| FEEDBACK-BUILD | PASS identity only | exact23103d9、lint、四ABI、签名、300资源及设备拉回APK相同；不等于整体CI/运行PASS |
| FEEDBACK-PHYSICAL | BLOCKED / NOT_RUN | WIF attribute condition拒绝、预检未达、物理提交0 |
| FEEDBACK-INHERITED | OPEN / FAIL | core logistics:75输入与候选独立失败；全国/PC参考/V1–V4/全触控/SAF/长稳历史门槛不关闭 |

详见[报告](reports/feedback-v118.md)及[manifest](evidence/feedback-v118/manifest.json)。未合main，未自动下一阶段。

---

# 网格/岸线专项 — v117 / PARTIAL

| 条目 | 状态 | 当前证据和剩余问题 |
|---|---|---|
| GRID-READABILITY | PASS（API29局部） | 双层线宽/对比度接入正常3D；旧/新同镜头UI实拍已复核；线宽审美待用户验收 |
| GRID-BLOCKED | PASS（掩码/局部） | 永久禁行格和山体内部不画普通格线；山道/栈道/可航水、编辑网格保留；不变更规则权限 |
| COAST-STEPS | PASS（有效水陆局部） | 共享顶点两次平滑，单轴位移≤0.1875；同源原APK实拍转角改善，拾取/LOD/面积host通过；VOID外缘与四路交叉仍固定，不代表完整PC美术/像素抗锯齿 |
| GRID-API35-READY | FAIL（首轮） | 原版PASS/候选初始ready超时，submitted3/pending2；同APK复跑旧版同样submitted3/pending2失败，新版PASS874并取得12组原图，双方稳定性未关闭。不能据API29通过关闭API35稳定性或推定纯环境 |
| NATIONAL-READY | FAIL | 同源原R05：120秒ready超时，pending29/submitted3，后续操作未到达；继承全国装载门槛保持开放 |
| CORE-LEGACY | FAIL（inherited） | 输入/候选独立复现 logistics:75 与 PortReplay.aiDocks:52；不改规则/断言，原日志保留 |
| PHYSICAL | NOT_RUN / access BLOCKED | WIF attribute condition拒绝，预检未到达、设备提交0，ARM64/Adreno/Mali/30分钟热稳未验收 |
| ART/FULL-TOUCH | NOT_RUN（本专项） | PC REFERENCE_MISSING；V1–V4历史门槛、全国美术、完整触控与编辑/旧档/SAF继续开放 |

详情见[本轮报告](reports/grid-coast-v117.md)。保持PARTIAL，不合并main，不开始下一阶段。

---

# R18 当前风险 — PARTIAL

- R18-RECOVERY：解除保护从构造结束延后到当前Surface内容检测；专项范围见R18报告。并非native abort或全视觉通过。
- R18-IDENTITY：候选preBuild核验clean完整SHA；交付/安装字节核验独立于玩法验收。
- R18-RELEASE：全国冷启动、全触控、旧档/SAF、V1–V4、ARM64长稳等继续阻塞。原core logistics:75输入/候选本轮均失败。
- 历史失败保留在下方，不因阶段推进清零。

API29/35当前v116：恢复及parity限定PASS，全国原120秒冷启动FAIL。真机WIF BLOCKED；V4未通过。

---

# R17 当前风险 — PARTIAL

- R17-MATERIAL：7份材质在native解析前固定hash与大小验证，host43例通过；不覆盖全部纹理/model或驱动故障。
- R17-RECOVERY：Java故障nativeFailure跨进程保留；安装证据见R17报告；不声称native abort测试完成。
- R17-FULL：完整触控、旧/自定义/头像升级、SAF、ARM64/驱动/长稳仍未闭合。
- R16-LOAD/V2/V3/core30失败继续有效并阻塞发布。

---

# R16 当前缺陷账本 — PARTIAL

| ID | 状态 | 证据与边界 |
|---|---|---|
| R16-IDX-01 索引上传字节 | scoped host PASS | 420正式mesh无损往返，减50%索引payload；不是总显存/帧率。 |
| R16-CPU-01 静止地形重复扫描 | scoped host PASS | 提取正式方法、mock GPU前后可见结果相同；移动对象持续裁剪；实际稳态收益未验证。 |
| R16-THERM-01 温控抖动 | scoped policy PASS / device NOT_RUN | 严重热立即降级、持续30秒低温恢复；原/候选模拟信号60→2切换。 |
| R16-UPLOAD-01 owner上传超限 | FAIL budget | 2ms只控制是否开始下一项；初版最大244.31ms单帧mesh上传wall，未宣称全GPU硬上限。 |
| R16-LOAD-01 运行前置 | FAIL | 最终局部PASS165、全国pending138/submitted39仍FAIL；全国压力NOT_REACHED。 |
| R16-REG-01 局部加载差异 | OPEN / FAIL first comparison | 初轮输入PASS/候选FAIL；再轮输入FAIL/候选PASS165。双方不稳定，无新增回归尚未关闭。 |
| R16-PHY-01 ARM64长稳 | BLOCKED | WIF attribute condition拒绝，零本轮物理提交；30分钟/Adreno/Mali/热稳/20次读档未验收。 |
| R16-METRIC-01 真实呈现/GPU | NOT_AVAILABLE | CPU环、PSS和原trace可追踪；没有可靠FrameTimeline/GPU duration或完整投影trace分析。 |
| R16-ART-01 视觉门槛 | FAIL/NOT_ACCEPTED | V2、V3未关闭，无美术资产改动或通过代表区实拍。 |
| R16-CORE-01 规则基线 | inherited FAIL | 输入和候选同一logistics:75；不改规则/断言。 |
| R16-DIAG-01 预算诊断旧值 | FIXED | 初版错误显示4ms；2626153从2ms常量导出并重建，同源最终APK另列。 |

报告与制品：reports/R16.md、evidence/R16/delivery.json。所有未关闭历史缺陷继续有效，不自动R17。

---

# R15 当前缺陷账本 — PARTIAL

详见[本轮报告](reports/R15.md)；source769db307。仅布局专项的限定通过可据证据确认，
不得据主机覆盖数量或构建成功关闭原美术/设备缺陷。

| ID | 状态 | 本轮证据与剩余范围 |
|---|---|---|
| R15-UI-01 横屏出征无可见武将 | scoped PASS on API29 emulator | 正式单ListView滚动header；横竖屏触控选将/确认35检查、完整存档RNG一致。最终同源包重复PASS；真机、3D完整链仍未验收。 |
| R15-MAT-01 普通道路地表配比 | host PASS / art PENDING | 正式v5 ROAD复用PLAIN权重；输入失败、候选1010检查；没有运行画面改善的充分对照。 |
| R15-GOLDEN-01 旧完整材质哈希冲突 | corrected test contract | 保留旧golden/冻结v4独立控制，新增几何与surface流不可变控制；R08/R09/R10受影响host门禁通过，原失败留档。 |
| R15-RUN-01 原生巡航ready | FAIL | 首轮120秒场景未ready，pending43、beginSkipped659/663；最终全图入口仍FAIL，pending330、beginSkipped692/696。不放宽门槛，不算12区实拍。 |
| R15-PHY-01 真机执行 | BLOCKED | WIF unauthorized_client / attribute condition拒绝；尚未额度预检，零提交，未绕过。 |
| R15-V3-01 全国视觉 | BLOCKED / NOT_ACCEPTED | V2 FAIL；岸线台阶、低浅山脊、粗糙/重复建筑、林缘/栈道等未实际补制。161映射不是161美术合格。 |
| R15-COV-01 全量设备覆盖 | PARTIAL | host9剧本+2夹具通过；旧版本实档、编辑SAF、所有地图GPU加载与全部锚点近远画面尚缺。 |
| R15-CORE-01 logistics75 | inherited FAIL | 输入399279b与候选原脚本均失败；原断言和权威规则未改。 |
| R15-SMOKE-01 收起按钮 | unresolved FAIL | 并行最终源体验workflow36374442688无法滚动到达“收起”；根因未独立定位，不能用布局专项替代。 |

所有历史未关闭条目继续有效；本轮不进入R16。

---

# Firebase 真机验证 — 2026-09-28 / NOT COMPLETE

当前验收见 [逐条报告与证据索引](evidence/firebase-20260928/AUDIT.md) 和 [463行可追溯矩阵](evidence/firebase-20260928/matrix.csv)；459条来自原始要求包，4条为明确标注的历史补充。以下旧报告均为历史，不代表当前受测 APK 的结论。

受测 app 源码 `17bd3376508499f2681d3e296939452383506a1e`，SHA256 `98edd7f758d41b89826d03e6fd1be3366bbea173d679a58bdd5f157b8e9479f4`。本轮只改测试/CI/证据，不改生产玩法、存档、地图或渲染，不合并 main。

- API35：Pixel8两次独立冷启动通过；Xiaomi14第三次在Activity启动处被系统拒绝并超时，不能算预览执行。API29：GalaxyS9两次通过，第三次因五次免费物理额度已用完而BLOCKED。双API各三次稳定性未满足。
- 两API各正常跨月四层日期证据，以及完整20次2D/3D切换、20次HOME/恢复通过；异常Window backing恢复、系统杀进程等完整条件未关闭。
- 完整纯触控链FAIL：两API横屏出征武将列表不可见。API29竖屏实际出征/移动后探针未到攻击位置，不能判作攻击规则缺陷。最后一轮后续6例NOT_RUN，独立存读档/30分钟运行未完成。
- owner同步纹理/rig解码与阶梯岸线是重证的实现缺口；logistics:75为输入、候选与实时main共同继承失败。SAF/编辑、全LOD动作和完整PC美术对照等按矩阵保留NOT_RUN/BLOCKED。

首先最小修复DeployWizard横屏可达性，重建配对APK后重跑受影响链；本轮未擅自修生产源码。不得把真机首段PASS、主机计数或CI构建成功称为P0已修复/R00–R14完成。

---

# v112 P0 运行续作 / PARTIAL

当前 APK source `5f8997ebfef15f4680931d40532411a336506d0d`；最终证据 HEAD 另见远端与独立交付 JSON。详情见 [v112 实测报告](reports/R00-R14-P0-v112.md)。

正式修复首次资产同步前的 site/unit LOD 选择；输入真实方法回归失败、候选通过。继承 v108–v111 有界流水线与 Window 恢复；不把继承成果计作本次新修复。

API29/API35 独立冷启动首CPU检查均到达，但全国3D readiness均FAIL。API29原R12初始ready失败，API35原R12在重建ready失败；完整纯触控新开局链未通过。API35完整权威一致性12组合PASS，50对完整存档字节复核一致；focused月份原整屏正确，但API29实际日期故障仍未关闭。20轮生命周期完整验收NOT_RUN；ARM64真机NOT_RUN。

未闭合项继续 FAIL/NOT_RUN；213条原文与历史状态保留。以下全部为历史，不代表 v112：

# v107 缺陷续作 / PARTIAL

当前 APK source `a4b09058c6b1ab8c80f57d96f1e22425673de94c`，v107。详细正式修改、原 v105 独立前测、候选结果及未到达操作见 [R00-R14-P0-v107.md](reports/R00-R14-P0-v107.md).

本轮削减全国重复法线/材质计算并合并远景批次，全部几何和规则字节保持；增加真实队列、CPU、提交与Surface采样诊断。日期实际画面、完整纯触控链、20轮设备生命周期、完整模式RNG矩阵仍未闭合。原213条不因主机套件通过而自动升级。

v106新增API29 CPU超时在v107全国ready回归中通过；API29实际UI停绘、API35原R12下一旬超时仍FAIL。完整触控链NOT_RUN。详见当前报告最终结果表。

以下为v105及更早历史，保留追溯，不代表当前APK：

# 原生PC视觉整改缺陷 — 2026-09-25 / PARTIAL

本次 APK源码 `ef54951dbb7056beaa782f97a3486eefa3a88ef9`。本次输入 `a5119b869f710949731781b35f13858cc87809c0`。API35原APK已能正确显示1/7/4月，不能把这个环境差异算成本次日期修复；API29窗口仍停止出帧。具体源码/CI/图像/退出码见 [当前续作报告](reports/R00-R14-CONTINUATION.md)；原审计逐字保存在history/R00-R14-audit-DEFECTS.md。

| ID | 状态 | 精确结论 |
|---|---|---|
| REMED-01 全国高度缓存反复整表清空 | scoped CLOSED | 真实全国扫描在输入稳定触发淘汰断言失败；有界AtomicIntegerArray候选通过饱和、并发读取、换Ground隔离、完整Save不变与R03回归。函数/拓扑/拾取语义不变。 |
| REMED-02 静止全国势力层重复重算 | scoped CLOSED | 正式Overlay按完整相机/Ground/势力数组/模式缓存原填充和边界，动态层实时绘制；原R12全图GPU积压由输入598/613到候选0并输出实际Surface。首屏栅格化及运动时重建成本仍未达到性能验收。 |
| AUDIT-01 遮挡窗口仍提交 | scoped PASS 保留 | 原SceneRenderGate不回退；本轮真实3轮Dialog继续验证暂停、获焦顺序、renderer身份及完整Save。不能扩大为20轮全部生命周期完成。 |
| AUDIT-02 全图势力预览及完整开局 | PARTIAL / API29 FAIL | v103轮原R12在API29输入/候选均再现超时pending515/517；API35双方通过，候选真触点冷启动至新局通过。最终v105 API29原R12/冷启动pending494/473；API35原R12通过但冷启动切3D失败。完整纯触控出征/移动/攻击/战报/旬/存读档仍未闭合。 |
| REMED-03 冷启动首次切3D非全图 | scoped CLOSED on API35 | 正式MapHost首次相机转换请求布局后fit；原v102在API29/35均有37406/38949格在视口外，v103 API35变为0并通过真实选势力/确认新局。API29加载阻塞单列，不能以此关闭完整预览链。 |
| AUDIT-03 日期/播放覆盖层陈旧 | FAIL on API29 | 控件和snapshot月份正确，独立整屏仍画一月；UI draw/Window帧停止，3D Surface继续。Window PixelCopy缺backing surface。未经原APK对照证明，不归因为纯采集/模拟器；详见报告对照结果。跨月跨年与播放/重建全部矩阵未闭合。 |
| AUDIT-04 V1/V2美术 | FAIL | V1未闭合、V2 FAIL。地形轮廓、材质、水岸、城港关、林缘/栈道/农田与四季未做本轮美术修复。刚体动画是允许方案，不以缺完整骨骼/IK充当缺陷。 |
| AUDIT-05 owner CPU解码/生命周期 | FAIL | 构造/loadAtlas/terrain纹理解码尚有同步工作；版本取消、引用、上传时点、预算驱逐、坏资源/部分初始化恢复完整矩阵NOT_RUN。 |
| AUDIT-06 全core AI | inherited FAIL | 输入与候选完整core各exit1，同一logistics:75。分类CI通过不是全core通过；未改玩法/AI/RNG/SaveCodec。 |
| AUDIT-07 设备/性能/动态验收 | NOT_RUN / PARTIAL evidence | 有真实模拟器录屏与原PNG；损坏段明确标记，不算有效视频。没有ARM64真机/30分钟热稳/PSS/GPU性能结论；AVD计时不代替手机性能。 |
| AUDIT-08 历史交付与完整验收 | PARTIAL | 本轮新制品链、原始结果和213条原文/历史状态已补齐；旧阶段证据不足未倒填。 |
| AUDIT-09 R13资产保护过时 | CLOSED | 精确保护规则模块与298份资产清单，仅登记4份合法R14材质及源码哈希；正例通过、损坏清单负例拒绝。R13完整UI/SAF仍未关闭。 |
| REMED-TEST-01 Window PixelCopy采集崩溃 | corrected harness | 主线程异常原样留档；现在记录不可用并继续独立整屏，不强制重绘、不修改生产日期，不把采集异常处理算UI已修。 |
| REMED-TEST-02 弹窗触点坐标 | corrected harness | 原窗口内坐标误点弹窗外的失败保留；改为getLocalVisibleRect+getLocationOnScreen真实屏幕坐标，复验范围见报告。 |

R02/R12真实触控与权威范围全矩阵、R10全类别动作、R11所有事件、R13编辑CRUD/头像/新剧本/SAF中断等完整验收，继续按213条清单执行，不因未在此表逐字展开而自动关闭。

REMED-04 首个全国CPU任务顺序已正式调整；v105 API29首个Ground ready177/coarse，未再先生成22块局部地形，但102327ms全国生成及剩余上传仍未满足120秒，整体FAIL。新增epoch断言未到达。录屏收尾改动后v105仍有1/15损坏段，不能关闭全部采集问题。

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
