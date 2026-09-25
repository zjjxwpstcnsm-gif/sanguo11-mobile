# 原生 PC 视觉缺陷 — 2026-09-25 总审计

当前源码 badd7e56308b4f6a47d60353d49023be8fc492a2，CI36091281604。原完整账本逐字保留在 history/DEFECTS-through-R14.md。以下关闭结论只覆盖所述断言/设备，不删除未重跑历史项。

## AUDIT-01 窗口遮挡时原生循环继续 — scoped PASS / 本轮修复

正式ScenarioFactionPicker对话框有第二MapHost，旧主窗口虽失焦但Activity未暂停，底层仍提交帧。新增SceneRenderGate并接入MapHost的Activity生命周期、窗口焦点、可见性、释放。复用原Filament资源，不重造World/相机/存档。

同一个NativeAuditInstrumentation编译到未经修改的eeb09生产树，真实全屏Dialog后 queued=true / frames4，断言 covered map stops native frame callback 实际FAIL。badd同断言实际PASS：3轮Dialog遮挡/前后台顺序/关闭恢复，covered期间无新frame，暂停时获焦不重启，renderer对象身份保持，完整SaveCodec字节一致。host76断言含20轮策略循环/8种状态/终止/线程约束PASS。安装101检查包含readiness轮询，不称101次手工操作。

这只关闭遮挡窗口继续提交问题。不能用它关闭AUDIT-02或03；小弹窗期间背景地图停在最后一帧是此策略的明确行为。

## AUDIT-02 势力选择全图预览 — FAIL / inherited, 本轮再次复现

原R12完整探针不改断言/不加时限，在badd正式全图预览仍到达ready时限：cpuChunks177、environmentCpuChunks788、pending614、submitted45、GPU86、output=WAITING_FRAME；相机99.5,99.75，span86.256714，tilt55。首轮7625同源码修复也失败pending606；原R12为654。数字不同不代表缺陷关闭。

失败栈为NativeR12Instrumentation.preview -> shot -> SceneInstrumentation.ready。此前双开关4组合、2D/重建状态、小地图和面板部分断言通过；此后势力确认/startScenario/正式出征移动攻击回合存读档流程NOT_REACHED。关闭须原完整测试在真实生产预览通过并审查实际Surface/UI、完整触控，不得缩地图/减少AI/隐藏Loading/修改阈值蒙混。

## AUDIT-03 日期/覆盖层合成像素陈旧 — FAIL / inherited, 本轮再次复现

badd 1/7/4月fixture中TextView.getText与snapshot.month正确；但7月和4月的原始UI PNG顶部仍是190年1月上旬。24张本轮原图（11对Surface/UI加2张重复末帧）已目视检查；Surface色调变化不等于UI刷新。原R14真实跨月后四月仍三月/播放40%问题没有本轮完整跨月关闭证据。

logcat存在HWUI dequeueBuffer -110/fallback，但不能仅凭该日志将根因判给驱动，也不能用额外invalidate猜测已修复。关闭需同帧世界日期/组件内容/合成屏幕一致，并在手工回合、读档和物理GPU复核。

## AUDIT-04 V1/V2与模型/岸线美术 — FAIL / inherited

R04 V1未闭合，R09 V2明确FAIL。当前实拍仍有方格/阶梯水岸、角状建筑、疏散树丛与孤立木板；存在正式GLB/纹理/4季uniform不等于PC美术合格。模型标为过渡资源；植被合批非GPU实例，单位是CPU缓存刚体pose+GPU实例draw，非GPU骨骼/全脚IK。

保留已有官方手册和用户洛阳02参考；参考02精确相机/季节UNKNOWN，参考01是占位图片。不是所有参考缺失，而是当前季节/镜头配准不足。V2未通过禁止将模板批量扩散并声称全国完成。关闭须原版近似镜头/区域实拍、多角度/连续运动并按04_VISUAL_ACCEPTANCE记录。

## AUDIT-05 owner线程仍有部分CPU解码 — FAIL / implementation gap

SceneAssetQueue/SceneWorkQueue几何异步不代表texture/atlas/rig全异步。FilamentMapView初始化仍有decodeStream等同步工作。此轮未改资产加载设计。关闭需CPU解码与GPU上传职责实证分离、异常取消/跨世界释放回归，并测量真实预算；仅限制每帧上传两个网格不能证明2ms预算。

## AUDIT-06 全core AI回归 — FAIL / independently reproduced inherited

精确eeb09与badd分别完整运行test-core.sh均exit1，同一AI uses deployment commands，CoreTest.logistics:75。host分类任务PASS只是确认两份原失败被保留，绝不是全规则套件PASS。本轮未改core/game-api/game-runtime/data/存档/RNG，按用户约束未擅修AI。原失败日志及退出码交付。

## AUDIT-07 物理设备/真实性能/完整视频 — NOT_RUN

本轮设备只有API29 x86_64 SwANGLE软件AVD，不是ARM64真机。两份meminfo均No process found，不能输出PSS结论；CPU提交/帧回调间隔不是GPU帧率。Adreno/Mali、30分钟热稳定、真实内存/GPU预算、透明过绘/阴影抖动、全部真实触控矩阵NOT_RUN。本輪24静态原图已审查，没有新增完整录屏；历史R14八个视频未完整人工验收。不得由二进制含arm64-v8a推论真机PASS。

## AUDIT-08 交付记录不一致 — PARTIAL（单项补齐，历史未追认证）

R00原始报告/交接/manifest/基线索引缺失，本轮追溯新增，标明原证据未知而不造hash。R03/R06/R07早期manifest写PENDING与后续记录不一致；R10/R11无独立manifest。旧原文留存，当前优先看总审计实际日志。原阶段完整链条没有本轮逐件重新认证。

## AUDIT-09 R13历史CI保护检查过时 — FAIL / inherited workflow

本轮自动触发CI36090968390在旧保护基线处停止，差分只涉及R14已改的ground/water/site/unit四份filamat；不是新规则改动。未把跳过的后续步骤当PASS，也未删旧断言。本轮总审计采用真实eeb09输入，完整core/game-api/game-runtime/data/资产/unity差分为空；R13 host重新通过。

其余旧缺陷以history/和各阶段原报告保留；未列入此轮关闭项的不自动关闭。下一阶段未启动；优先处理既有预览/日期/解码/美术门槛，但此报告不构成自动执行授权。
