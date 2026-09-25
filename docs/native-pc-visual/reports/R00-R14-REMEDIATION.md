# R00–R14 整改与复验 — PARTIAL，禁止合并

2026-09-25。本轮实际修改正式渲染源码并构建新 APK；不是全阶段关闭。输入远端 `47e5db471e6dadb22d4c782522f97c113601461e`，main `ac29b458325b52d6e302ca44270d16552de4ed7f`，继续 PR67 / agent/native-pc-visual。未合并 main、未 force push、未处理其他 PR、未启动 R15。原提示词、全局契约与213项原文保留；历史报告不倒填为通过。

## 正式修复及边界

1. `TerrainSurface.sample`：旧半格缓存到32768条即整表清空，全国遍历会反复淘汰刚计算的高度，主线程 Overlay 与 mesh worker 竞争重复计算。本轮改为绑定不可变 Ground 的有界原子数组缓存，最多1MiB；国家地图657720字节。非半格、超边界、超预算仍使用原精确计算，不改地形函数/规则/拾取语义。输入版本专用回归稳定失败，候选通过，同时验证并发读取、换Ground、完整Save字节不变。主机冷热扫描时间不是手机性能。
2. `FilamentMapView.Overlay`：静止全国势力填充和边界此前每帧遍历所有格、重复高度采样。本轮仅将该静态层按原路径栅格化、按完整相机姿态/视口/Ground/势力数组和模式失效，最多4194304像素；超预算回原绘制路径，释放随宿主。标签、日期、行动范围、播放、编辑提示仍实时绘制。未增加超时、未伪造READY、未缩图、未减玩法对象、未暗中2D回退，也未重造加载器。首次全国静态层栅格化在首轮软件模拟器约1.95s，连续相机运动会失效重建，不能称首屏/旋转帧预算已通过。
3. R13旧资产保护：保留core/game-api/game-runtime/data/unity精确保护；登记298份运行资产及哈希，仅明确允许历史R14合法修改的4份filamat，并验证对应材质源码哈希、版本、来源与回归。旧R13原命令在精确输入47e5上独立复现exit1，差分仅4份历史filamat；新门禁通过，破坏清单的负例实际拒绝。未忽略assets整体变化。

## 安装运行与问题定位

首轮 CI36095728029：未经修改的输入生产树47e5独立运行原完整R12仍FAIL，pending598 / terrain177 / environment788 / submitted51 / WAITING_FRAME。候选cefdc同原探针PASS（60220检查包含轮询），全图预览pending0、真实Surface内容输出，后续势力确认、开局、部署移动、回合与存读档、攻击fixture均到达。没有修改原NativeR12Instrumentation和SceneInstrumentation断言。

定位依据：失败预览meshGeneration=1、worker_pending=0、worker_waiting=0、terrain_all_coarse=true，CPU地形177/装饰788已经生成，缺少GPU驻留的pending仍598；不是该次CPU队列死锁或反复换代，也不是全图高精度生成。已有每帧两网格上传预算被缓慢的主线程/Overlay推进拖住。输入surface=true/submitted51只说明Surface存在且有提交，不能证明屏幕已呈现；`WAITING_FRAME`探测器本来就等pending清零，不能反推从未提交首帧。修复后同数量地图/装饰保留，GPU地形177、可见林块507、pending0，并实际PixelCopy有内容。此结论不排除后续窗口合成故障，也不宣称所有装饰优先级/上传时间预算已经完工。

这关闭缓存淘汰和静态Overlay重复计算两个已复现缺陷，并建立原R12阻塞的恢复证据；不等于完整纯触控链或美术通过。原R12部分动作走正式命令API；单独新增的冷启动探针从无auto.sg11的正常启动页注入真实触点，目标覆盖剧本→全图预览→选势力→确认开局；本轮实际在预览断言失败停止。后续全触控选城/出征/移动/攻击/战报/下一旬/存读档仍未完整认证。

最终APK v102 / CI36097729262：assembleDebug、androidTest编译、lint及两次安装成功。原R12 `PASS checks=60370`；focused-window机械断言exit0，但视觉判定FAIL。真实冷启动触控已从无auto.sg11启动页进入184剧本预览，3D有内容且pending0；随后严格断言“UI overlay continues drawing live labels”失败，停止于选择势力之前，确认开局及后续全触控均NOT_REACHED。未删除该断言。还实测首次手动切3D后相机span15（局部视野、36 CPU terrain/115 environment），不是全图；原R12单独fit后的全国span86.256714才覆盖177/788。因此文件名cold-national-preview不代表其实际全图通过，这个正常入口相机适配缺口另列FAIL。

v102日期1/7/4月onDraw固定9、Window frames固定8，Window PixelCopy仍无backing surface；七月/四月整屏仍显示一月。完整开局和UI显示问题都没有关闭。

日期复验同时记录权威日期、TextView、snapshot、View onDraw次数/时间、Window FrameMetrics/vsync、3D Surface PixelCopy、UiAutomation整屏、shell screencap与录屏。最初Window PixelCopy在主线程抛出“Window doesn't have a backing surface”，导致探针进程崩溃；这是新增采集代码的失败，已保留日志并改为记录不可用、继续独立整屏采集，没有强制重绘或改生产日期代码。v101实际证据：1/7/4月权威与控件正确，但onDraw始终18、Window frames始终17、Window PixelCopy无backing surface；独立shell整屏和UiAutomation整屏均仍显示一月，3D Surface季节画面继续变化。说明UI窗口绘制/合成没有产生相应新帧，不能归结为单个截图接口陈旧，更不能直接归因模拟器。

原发布APK无录屏对照 CI36098809236：直接下载上轮badd APK原文件，校验SHA256 `b4deb41359eb2cedcac949444f87c9454fe020ce5d52b94ab4565e36a79b1687`、开发签名与版本98；不替换生产APK。仅单独安装复验instrumentation，运行时反射读取SOURCE=badd、package=0.98.0，未启动screenrecord。结果机械断言PASS79，但七月/四月两种整屏仍一月，onDraw固定77、Window frames固定76、Window PixelCopy同样不可用。录屏不是此故障出现的必要条件；不能认定只是录屏或UiAutomation采集问题，也没有证据把责任直接归给模拟器。保持FAIL，继续排查窗口Surface/遍历恢复/合成。

完整日期/播放问题未关闭；不得仅以PASS AUDIT字样或控件文本判断视觉通过。

## 本轮测试与设备

精确96719e9源码：16组主机（R00–R14审计gate、R01–R14、architecture）全exit0；R03为12368259断言，含新增全国缓存回归。输入47e5饱和缓存负例exit1；候选cache_bytes=657720。构建/测试APK编译/lint PASS；安装成功；原R12 PASS；冷启动触控FAIL；日期实际像素FAIL。Android job及整体整改CI为FAIL（保留冷启动失败），不可标为全绿。

完整core输入与候选分别执行均exit1：`AI uses deployment commands / CoreTest.logistics:75`。分类步骤绿色只证明保留了同一继承失败，全core仍FAIL；未删断言、改预期或调整AI。

设备只有GitHub Actions API29 x86_64 Pixel2配置/SwANGLE软件模拟器（实测日志：OpenGL ES3.1、ANGLE2.1.17841、SwiftShader driver5.0.0）。ARM64真机型号/API/驱动、30分钟热稳、真实PSS/GPU性能均NOT_RUN；含arm64-v8a不等于实机测试。No process found不记零内存。模拟器计时和CPU提交时间不推导手机帧率。录屏编码失败/损坏段原样保留，完整性另列，不能当有效动态证据。

## 未关闭范围（按续作顺序）

| 范围 | 当前结果与续作入口 |
|---|---|
| R12完整交互、R14日期/播放 | FAIL/PARTIAL；先处理日期真实合成与完整触控链，再做不同月份加载、跨月跨年、结束/跳过、对话框、前后台、Activity重建。不要以重启页面或测试强制draw掩盖。 |
| R01/R06资源异步生命周期 | FAIL；FilamentMapView构造/loadAtlas/terrain纹理解码仍有owner CPU工作。复用SceneAssetQueue/SceneWorkQueue做CPU解码，GPU对象仍归owner；20次切换/前后台、换档/剧本、延迟worker、坏资源、部分初始化恢复NOT_RUN。遮挡暂停修复保留。 |
| R03–R09/R14 V1/V2 | V1未闭合，V2 FAIL。没有本轮模型/纹理/生成器美术修复，不能称山脊、水岸、城港关、林缘/栈道/农田、四季已修。原用户洛阳参考及官方资料保留；缺少相机/季节精确配准不编造。近中远双向/网格对照和连续镜头PC验收NOT_RUN，禁止全国模板扩展。 |
| R02/R12触控/覆盖层 | 完整小大屏、安全区、方向、缩放旋转、动态分辨率矩阵NOT_RUN。主机与原R12局部范围/开关验证不能覆盖完整矩阵。 |
| R10单位 | 全实际兵种/装备、轮廓、脚滑漂浮、船靠岸、LOD相位和真实动作录像未闭合；刚体动画是合法实现，不把全骨骼/全脚IK作为新增门槛。 |
| R11事件 | CALM/EXTINGUISH及全实际事件、暂停/倍速/跳过/重建、无重复权威效果/残火/无界资源完整矩阵NOT_RUN。 |
| R13编辑器/SAF | 真实UI城港关CRUD、保存重开、导入导出、新剧本游玩、头像/武将链NOT_RUN。外部openOutputStream(uri,"wt")不能宣称原子替换；取消/撤权/断写/冷启和唯一有效文件恢复未认证。内部原子保存不替代DocumentProvider能力。 |
| 权威对照 | 完整相同存档/命令/RNG下2D/3D×正常/关闭/跳过动画全结果矩阵未完成；仅报告本轮实际主机及R12比较范围。 |

213条逐项保留于 `evidence/R00-R14-remediation-checklist.json`：含原文、行号、历史状态和本轮完整复合验收状态。不能按主机绿色把阶段整体升级。所有R00–R14仍PARTIAL。

## 精确制品

源码 `96719e971d74aa2b3f18d66a7f23832148a6dc8b`；应用 `game.sanguo.mobile.dev`；版本102 / 0.102.0-native-remediation。
APK SHA256 `ab87acc3f151594260ee8c07b802b39ff8bb354712e959c54252dc51680cd2ba`；37370473 bytes；ABI arm64-v8a / armeabi-v7a / x86 / x86_64。
开发证书SHA256 `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`，与输入一致；CI apksigner完整验证，本地独立读取v2证书和APK哈希再次核对。
构建：`./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true`。
CI：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36097729262
PR：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/67

构建、签名、安装测试、截图、录屏、日志和退出码分别交付。首轮GitHub release步骤曾因integration权限403失败，该失败保留；最终v102 release已成功发布并复读：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/releases/tag/native-remediation-96719e971d74 。它明确标为PARTIAL候选，发布成功不改变安装/视觉FAIL。Actions制品和独立APK同时交付。最终复验/证据提交只改测试、CI与文档，APK之后生产源码/资源/版本不变，最终远端HEAD见交付 `REMOTE_DELIVERY.json` 和PR复读记录；不把它冒充APK源码。
