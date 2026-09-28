# 2026-09-28 真机验证：源码与证据边界

本轮没有修改生产源码、资源、规则、地图或存档。受测 app 一直是首次 Firebase 构建的 `17bd3376508499f2681d3e296939452383506a1e`，SHA256 `98edd7f758d41b89826d03e6fd1be3366bbea173d679a58bdd5f157b8e9479f4`。新增测试 APK 与生产 app 的版本分别记录在每轮 PAIR.txt；测试编译时内联的 BuildConfig.SOURCE_REVISION 不能用作旧 app 的来源证明。

## 已重新确认的实现缺口

1. **横屏出征选将不可达**。Pixel 8 / API35 第二轮以及 Galaxy S9 / API29 第二轮的正常新局，通过陈留城池按钮和“出征”进入 DeployWizard。`deploy.officers` 以及唯一可用的袁绍行均无可见边界。整屏图显示列表被上方编队卡、说明和工具条挤出；“确认出征”不可用。测试只发送真实 MotionEvent，没有 performClick、业务命令或世界 fixture。`DeployWizard.show/crewPage` 的固定高度编队区、详情区、搜索区和权重 ListView 在横屏有限高度中竞争空间。完整链在选将处 FAIL；后续移动/攻击/战报/手动存读档不可据此算通过。定位文件 `app/src/main/java/game/sanguo/mobile/DeployWizard.java`，构建 root 高度及 crewPage 的布局。修复优先于扩展动作美术。
2. **同步 CPU 解码仍存在**。`FilamentMapView` 构造器直接调用 `loadGroundMaterials`、`loadAtlas` 和 `new FieldAssets`。`loadGroundMaterials` 内 8 张纹理 `BitmapFactory.decodeStream`，`loadAtlas` 的 bounds/full decode 与 FieldAssets 的 JSON 读取均仍在 owner 路径。`MapHost.switchMode` 在 UI 所有者创建该视图。SceneWorkQueue/SceneAssetQueue 已把地形和部分 GLB/pose 后台化，不能因此宣布所有耗时解码后台化。对应 R01.I03、R06.I05、03_RENDER_CONTRACTS。
3. **阶梯岸线仍存在**。`WaterVisualField` 明确使用投影 tile 矩形并集距离，只对材质带平滑；几何轮廓没有完成要求中的受约束简化/细化。两 API 的正常下邳 Surface 和整屏图均见连续方形台阶海岸与河岸。R05.I03、R05.G01 仍 FAIL。水域拓扑/港口检查通过不能覆盖视觉失败。

## 首轮全国预览正常推进的范围

`NativeColdStartInstrumentation` 从空数据页触点新游戏、首剧本、2D/3D、势力 chip、确认及“执行”。它只读取会话来验证，无预写 auto.sg11，无调用 startScenario 或业务命令。第一次势力触点是已经默认选择的何进；因此证明了选择触点和确认开局，不证明切换到不同势力后所有显示更新。

同一轮追踪：首 CPU epoch=1、coarse/far LOD -> Choreographer doFrame -> meshWork.drain/acceptMeshes -> beginAttempts/beginSkipped -> 成功 beginFrame 后 GPU preparation/loadVisible -> render/finally endFrame -> renderedFrames/surfaceFrames -> 完成后的 Surface PixelCopy -> 全屏/连续视频。首 API35 preparations=submissions=99，attempts=101，skipped=2；首 API29 preparations=submissions=194，attempts=198，skipped=4。两轮 pending 均最终清零。ready() 仍为原 120000 ms；没有删除 beginFrame 判断、预热或延长该期限。

这些是该设备/该轮正常链的实证，不能推出所有驱动、后半段玩法、30 分钟性能或 R00–R14 完成。

## 日期及窗口恢复

新增测试分别记录 World.date/turn、MapSceneSnapshot.month、可见 hud.date 的文本和屏幕边界、WindowSurfaceRecovery.report、整屏、Surface 与录像。snapshot 当前只保存月份，不能假称它保存完整年月旬。

API35 第二轮正常触点下一旬三次：184年1月上旬 -> 中旬 -> 下旬 -> 2月上旬；权威、widget 和原始整屏像素一致。windowFrames 从209推进至603，confirmedLosses=0、relayouts=0。该场景没有触发恢复，故不能证明 WindowSurfaceRecovery 对历史 backing Surface 丢失的修复分支有效，更不能以四季换色替代普通 UI 刷新。API29第二轮turn7至10的原图为184年3月中旬、下旬、4月上旬、中旬，亦与四层证据一致；windowFrames=2246至3093，confirmedLosses/relayouts均0。跨年、实际异常恢复、进程死亡/Activity 重建仍另列未验。

`WindowSurfaceRecovery.check` 在 attached/shown/focused 成立后，若 lastFrame >= lastRequest 则不探测；安静期后才做 Window PixelCopy；确认缺 backing 后才同窗 relayout。正常帧推进条件阻止不必要恢复，属于设计路径，本轮没有制造异常来闭合其恢复效果。

## 生命周期

MainActivity.onPause/onResume -> MapHost.resume -> SceneRenderGate(resumed/focused/visible) -> FilamentMapView.resume -> cancelFrame/schedule。预览 Dialog 的焦点/可见性通过 MapHost 窗口回调纳入门控，dismiss 则释放。没有删暂停机制。

20份切换后计数分别稳定：Pixel 8的entity/material instance/mesh/texture/material/pose cache为382/124/129/11/7/38；Galaxy S9为432/126/180/11/7/57。不同场景计数不互比，只检查同一轮稳定。

新增专项从真实冷启动后的会话执行完整 20 次 3D/2D 切换、20 次 HOME/恢复。切换使用展示 API，**明确独立于纯触控链**；不修改 World。每次销毁检查 released/queued/engine/activeNativeHosts 和完整 SaveCodec 字节。HOME 使用系统键后采样 hasWindowFocus、resumed、queued、两个间隔帧计数；恢复要求同一活动 host 且帧数继续增加。API35 第二轮和 API29 第二轮均20+20全部执行，真实后台 focus=false/resumed=false/queued=false、帧数不变，返回后恢复。未通过数量稳定推断无全部 native/GPU 泄漏；worker 延迟/异常构造/系统杀进程仍未形成真机闭环。

## 其余实现与主机验证

- R02：重新执行38949有效格与负坐标/VOID/据点 footprint检查及3缩放×4方向×3倾角1548抽样。0.7/0.85/1.0部分是主机 view-pixel 契约，测试中没有实际调整设备 render buffer，不能写成三分辨率真机触点验收。
- R03：地形共享边/有限数值/邻接LOD、32块复用4块变化及规则字节不变重跑通过；实际跨块各视角无裂缝的完整画面矩阵仍不充分。
- R04：权重/归一化/UV等主机通过。真实材质可绘制不等于完成 PC 对照、三画质、全部 mip/色彩空间安装检查。
- R06/R07：主机实际加载全部资源，恶意 header/截断/索引/stride/required extension/路径/纹理预算输入验证；679据点实例、5族和120设施LOD映射通过。资源存在、映射无空引用与近景外观合格分开记录。实际构建中的 clean export、glTF validator、matc56 另以当轮 CI 步骤日志为证。
- R08：确定散布/排除/局部更新主机通过。mergedChunks、CPU mesh bytes是估算，不能声称实测GPU内存或手机FPS。
- R09：正式洛阳样板区域主机数据核验通过；V2未关闭，尤其水岸。REFERENCE_INDEX 中参考画面部分版本/季节/相机仍 UNKNOWN，不能自行补造精确对照。
- R10：13真实类别、8刚体clip、近/中远共享的2套mesh映射和动作/接触/LOD/phase主机测试通过。刚体与 GPU instanced draw、CPU posing 分开描述。完整兵种×LOD×动作的手机录像、脚滑/穿模/水位目视与压力帧时未完成，不能因没有骨骼/IK就自行判缺陷，也不能反向宣布美术合格。
- R11：从真实规则命令产生的 host fixtures验证 typed journals、8事件kind、9style和7播放模式；没有把夹具混进纯触控PASS。各类真机特效/战报/权威一致的完整组合仍未执行。
- R13：地图笔刷、版本、地图事务、自定义武将/头像包安全、所增据点规则及边界检查实际执行。`MapEditorActivity` 使用 ACTION_OPEN_DOCUMENT/ACTION_CREATE_DOCUMENT 和 content URI、后台解析、事务预检；主机检查没有操作系统文档提供者，外部存储真机端到端不能算通过。
- R14：590条日期/季节/存档/权威检查重跑通过；只跨一个月的手机日期显示不能替代四季×三景别×网格/画质的画面对照。

## core 与测试环境问题分层

原完整 core 42 个调用分别在输入基线 `8b42af2a70c0fc066620e2ce79951aa31d8b923e` 和当前生产源码执行；均12个退出0、30个退出1，没有改变退出码。`CoreTest.logistics:75` 两边同为 `AI uses deployment commands`。三个候选日志缺完整 stderr 的收集问题被保留，并对原命令双边单独复跑；combat deployment、water route crosses river、northern land route失败也相同。不是渲染回归，也未修改规则。

最初主机环境缺 javac 可执行入口，但已装 JDK17 compiler module；建立调用现有 jdk.compiler 的临时 launcher 后仅重跑受影响4项。最初缺历史 git对象的保护树检查在取回精确对象后重跑通过。所有初次失败和复跑日志都保留。

## 已执行但失败的探针与未执行的玩法

API29第二轮竖屏使用真实方向菜单后，可以触点袁绍选将、剑兵3000、确认出征并移动至(121,109)。后续7旬都由真实下一旬/执行推进，但旧探针按直线距离选路，未绕开河岸到达攻击位置。此处归类为EXECUTION_PROBE，不因此宣称攻击功能有缺陷，也不将出征/移动局部成功算完整链PASS。第三轮API35仅将选点改为只读权威MarchOrders.previewCity结果，再发物理触点；结果另见runtime-cases.json。

本轮新增独立手动存读档用例与30分钟混合操作；实际最后一轮未到达这些入口。Xiaomi14/API35测试进程启动并记录设备后，MainActivity启动被MIUI拒绝（Permission Denied Activity / START result code=102），随后instrumentation外层超时。JUnit只记录normalColdStartToNativeGame一个失败；后续6例NOT_RUN。没有cold-runtime、截图、视频、Filament驱动日志，不宣称30分钟运行或该设备GPU验证。归类为ENVIRONMENT_ACTIVITY_LAUNCH；不是全国预览帧管线已运行后卡住的证据。原始logcat、instrumentation与设备记录一起保存，未做自动重试或安全策略绕过。

## 制品提取完整性

原始GitHub firebase-apks ZIP为32062332字节，SHA256 afe604b984765dd3efd185f33e763fb7cf0e087842c9813a39104a910ed68dca。交付核验发现先前本地解包app不完整；重新从同一完整ZIP提取后，APK为37387038字节，SHA256与CI的98edd7…一致，ZIP全部entry CRC通过。测试实际使用的是CI验证过的原始app；本地截断未用于真机。交付APK已替换为完整原包。支持arm64-v8a/armeabi-v7a/x86/x86_64，属于universal包；ARM64单独裁剪包未生成。

## owner 路径的实际计时口径

首轮Pixel8/第二轮Pixel8的全国构造ground_load_cpu_ms为57.38/56.34；两轮GalaxyS9为86.86/92.15。该字段来自owner上的System.nanoTime前后差，包含材质读取、CPU解码、native创建与上传调用，不是纯解码CPU采样，也不是GPU执行时间。全国首次势力层构建313.11/353.39ms与889.80/926.59ms亦保留；后续静止帧复用缓存不能消除首次主线程工作。具体值见evidence/owner-load-timings.json。

当前独立CI R01模拟器(API29/x86_64)仍在初始ready失败：worker交付36块而pending30、593次begin只有2次成功，frameCallbacks599，不能以本轮物理正常运行关闭该环境故障；原始lifecycle.txt/logcat/失败图已另存。该CI APK source ed2a70b、hash10498c1726e6c91a62da17577cc8cb459b33b2b3bb0a99c9ace593fca3892d39，明确不混同受测17bd app。

## R11遗漏事件的本轮补充执行

新增FirebasePlotAuditFixture/Test及test-firebase-audit-events.sh，仅位于host test路径。实际调用正式War.plot，分别产生CALM清除CONFUSED、CALM清除MISLED、EXTINGUISH移除火场三组，每组恰有1个对应typed PLOT事件；三画质效果池实际采样、目标位置、七播放模式完成与ledger不重复、完整SaveCodec/RNG和无journal控制组一致，共1841检查通过。它们不是Android触控/真机图像/自动存档IO，不关闭全部R11。首次fixture把在外君主仍登记为太守，SaveCodec正确拒绝；该输入错误与修正后的运行日志均保留，正式规则未修改。

实时main ac29b458的core树与8b42/17bd不完全相同，因此另建只读分离worktree实际执行main原test-core.sh，仍首先在CoreTest.logistics:75以AI uses deployment commands失败，exit1。该main脚本按原set-e停止，后续case没有执行；未把它写成第三套42调用矩阵。
