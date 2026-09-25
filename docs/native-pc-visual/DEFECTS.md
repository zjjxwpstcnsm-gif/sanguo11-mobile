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
