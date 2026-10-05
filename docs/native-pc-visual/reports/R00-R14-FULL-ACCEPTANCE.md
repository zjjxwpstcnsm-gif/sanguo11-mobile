# R00–R14 原始开发要求全量验收与差距确认

R00–R14是否全部满足原始开发要求：**否**。

本轮是验收，不是R15，也没有改生产游戏代码、资产、玩法或SaveCodec。继续PR67、同一串行分支，不合并main。五种状态按本轮用户定义执行；PARTIAL/NOT_RUN/NOT_REACHED均不构成通过。原213条原文与全部历史字段保留在 `evidence/full-acceptance/original213-history.json`。

## 身份和依据

- 先读远端的输入HEAD：`fbd26d83fdacca89033c9d053babb25450e18b47`；main：`ac29b458325b52d6e302ca44270d16552de4ed7f`。
- 正式APK source：`5f8997ebfef15f4680931d40532411a336506d0d`。
- v112 SHA256：`6ba2211d961567b2aa396ba19e3018c1dc44780fa4351a807db099fc643bd058`；37,387,038 bytes；CRC通过。
- `game.sanguo.mobile.dev`；112 / `0.112.0-native-first-asset-lod`；universal：arm64-v8a、armeabi-v7a、x86、x86_64。
- 开发证书SHA256：`8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`。独立解析APK v2证书DER得到相同指纹；完整APK哈希与原apksigner验证产物一致，未另写签名验签器。
- 生产源/资源相对APK source零差异；300个APK assets与仓库逐字节一致，无libunity/libil2cpp。工作流、androidTest及文档的差异不伪称新生产版本。
- 本轮第一复验工作流source：`f03a426e7fc1da3ec9e158402f7320628a58d981`；API29兼容test APK source：`33e49a3b915bd003dcc2c0cbe989adc638e27320`。最终文档HEAD见提交和最终交付manifest，不与APK source混淆。

六份用户指定文件均完整下载；ZIP逐条CRC、大小、SHA256、解压检查见 `audit/raw/input-integrity.json`、`nested-integrity.json`。原提示词包的64项SHA256和validate_pack真实执行通过。完整原文保存在originals：全局00–09、R00–R14、EXECUTE_PROMPT；原始整改/比较/证据包按哈希保留来源。不是以搜索摘要代替原文。实时远端没有AGENTS.md；已读取要求的报告、交接和清单。

## 覆盖结果

原213条 + 补充来源条款222条 = **435条**。当前：PARTIAL 172，FAIL 16，PASS 9，NOT_REACHED 5，NOT_RUN 233。

原213条全部能逐字定位原提示词，无擅改原文字句。但只收录I/V/G/D列表，会漏掉全局契约、共通验证/交付段落和15阶段特别禁止项。补充清单按原始行号稳定编号，完全相同的重复句合并来源；不同来源的交叉约束允许重叠。总数是**复合验收条款数，不是宣称原需求恰好有这么多原子功能**。每条复合条款全部条件成立才PASS。

逐条矩阵：`evidence/full-acceptance/MATRIX.md`、`matrix.json`；源码位置/正常调用链：`stage-source-index.json`；逐个非空原文行的纳入/交叉引用/标题/事实/未来阶段排除记录：`source-coverage.json`。没有为了保持213忽略原文。R15–R18未来门槛仅作上下文，不启动。性能预算仍是原文建议起点，不能把R16尚未冻结的阈值当普遍手机承诺。

审计发现的证据降格风险：R02内部分辨率HOST仅计算bw/bh，并未真的调整安装renderer；R11 fixture的若干事件不等于所有实际事件；R10资产族映射不等于全类别美术/动作；R13内部AtomicFile不证明外部SAF断写安全；20轮HOST模型不替代安装应用。现有213没有逐条完整实现/设备/证据字段，统一历史说明不能作为每条PASS依据。本轮将这些缺口留在当前状态，历史PASS不覆盖当前失败。

## 每阶段结论

| 阶段 | 状态 | 具体缺口 |
|---|---|---|
| R00 | FAIL | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R01 | FAIL | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R02 | PARTIAL | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R03 | PARTIAL | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R04 | PARTIAL | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R05 | FAIL | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R06 | FAIL | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R07 | PARTIAL | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R08 | PARTIAL | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R09 | PARTIAL | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R10 | PARTIAL | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R11 | PARTIAL | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R12 | FAIL | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R13 | PARTIAL | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R14 | PARTIAL | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |

## 本次实际运行结果

所有运行使用原v112生产APK，先核对source/sha256，再安装app/test APK。每个probe独立force-stop、pm clear，清理隔离的开发应用数据。工作流原命令、UTC时间和退出码在原始job日志。API29、API35均Pixel2配置、1080×1920、x86_64、SwANGLE软件图形、Filament1.56.0/OpenGL；完整getprop与SurfaceFlinger转储在每个raw制品。ARM64打包不是ARM64实测。

| 验收 | API29 | API35 | 范围/未到达 |
|---|---|---|---|
| 安装及APK身份 | PASS | PASS | 原APK哈希一致；不是新版本 |
| 独立纯触点冷启动首CPU/远LOD检查 | 到达，allCoarse=true | 到达，allCoarse=true | v112局部LOD次序修正仍成立 |
| 独立冷启动全国预览 | FAIL，原120秒ready超时 | FAIL，原120秒ready超时 | 不延长超时、不删断言 |
| 原完整NativeR12 | FAIL，初始ready，原line89 | FAIL，初始ready，原line89 | 后续四组合/重建/下一旬等NOT_REACHED |
| focused日期正式剧本探针 | 初始ready失败 | 初始ready失败 | 月份循环NOT_REACHED；不把API29日期缺陷关闭 |
| 权威fixture12组合 | PASS 12组合/332断言，50对完整字节一致 | PASS 12组合/324断言，50对完整字节一致 | 显式fixture/API，不是纯触控 |
| 20轮安装生命周期 | FAIL：模式切换日志17/20，前后台0/20；下一次ready超时 | FAIL：模式切换日志20/20，前后台13/20；background frame loop stopped | 具体实际轮数见下面，不把部分轮数算通过 |
| ARM64物理机 | NOT_RUN | NOT_RUN | 无Adreno/Mali物理设备 |

正常触控链明确断点：冷启动→新剧本已执行，按实际焦点/控件矩形发真实触点→全国3D预览ready失败；因此选择势力确认→选城/出征→选军/移动/攻击→战报→下一旬→手动保存/读档均**NOT_REACHED**。不借fixture、performClick或预热存档填补这条链。

原CI36213760665 attempt1全部8个最终job已读取，包括108325858061。该旧生命周期最终FAIL：switch=0..19，background=0..17，接着`background frame loop stopped`断言失败；后续过期worker、注入解码故障和重试NOT_REACHED。上轮交付时无最终结果，本轮按最终原始证据更新，不再写20轮未出结果或PASS。它是既有APK结果，不是本轮生产回归。

当前CI：36222182595；兼容test APK验证CI：36222424271。API29首轮parity在case0因`InputStream.readAllBytes`缺失失败，属于测试兼容问题。最小修正仅为androidTest的完整字节流读取循环；保留完整字节比较、断言和超时。新test APK独立构建，正式APK未重建。兼容探针最终API29/35全部job成功：12组合分别332/348断言，各50对完整字节一致。原test APK的API35独立结果为324断言，也单独保留。

## P0实际路径与观测

`MainActivity → ScenarioFactionPicker.show → MapHost.switchMode/publish → FilamentMapView.snapshot → SceneWorkQueue → phased result → doFrame/loadVisible → beginFrame/render/endFrame → Surface`。

MapHost在第一次publish前恢复/fit相机；v112在首次syncObjects前selectObjectLods。冷启动原始日志确实在加载中出现FIRST_CPU_DELIVERY，并非等载入后才检查。

| 独立cold-only | API29 | API35 |
|---|---:|---:|
| terrain CPU chunks | 177 | 177 |
| 首个检查allCoarse | true | true |
| 结束pending | 318 | 321 |
| beginFrame尝试/跳过 | 572 / 569 | 446 / 443 |
| 实际累计提交 | 3 | 3 |
| phased deliveries | 24 | 24 |
| 结果背压墙钟ms | 200.3081 | 2539.840939 |
| PixelCopy诊断计数 | 0 | 0 |
| 内部/UI尺寸 | 874×630 / 1028×741 | 874×623 / 1028×733 |

CPU墙钟、线程CPU、队列等待、结果背压分别保存。API29一条实际Ground CPU ready记录：总wall 32035.39348ms，queue wait 0.67162ms，backdrop wall559.54795/cpu199.220762ms，ground wall31475.84553/cpu2635.909922ms。这显示墙钟远大于线程CPU，但不足以单独证明具体调度/GC/GPU根因。logcat保留逐条GC/分配回收与进度；runtime-summary.json列出原记录，禁止相加重叠计时或把采样估算当全阶段耗时。

源码确认CPU队列一运行+一等待、单槽结果背压；资产队列64、CPU ready LRU24MiB、错误上限128；失效generation、owner检查、关闭/释放路径存在。Proxy.shape.references和缓存驱逐检查实际存在，不能误写“完全没有引用计数”。这不证明全部设备取消/过期回调安全，未到达仍未验。

doFrame先消化CPU交付，再尝试beginFrame；只有获准帧进行对象同步/上传/render/endFrame；Overlay仍可单独invalidate。当前录像中前期黑/灰载入区域、末尾出现部分地形/对象，持续显示loading。**overlayDraws不是3D证明；WAITING_FRAME也不是无像素证明**。API35 fixture重建后同样曾报告WAITING_FRAME，但独立screencap确有3D地面。

当前API29生命周期在17轮模式切换后ready超时；API35在20轮切换和13轮前后台后失败。后一断言不能独自区分HOME/焦点事件未完成与真正后台帧循环缺陷，根因仍待事件分发证据。CPU/GPU队列、上传和Surface提交的确切堵点仍未完成驱动级因果定位。优先下一步在该链上补帧获准/队列/Surface同步的最小可观测对照，保留SceneRenderGate遮挡暂停，不能移除gate冲绿。每帧4ms是CPU上传墙钟预算，不是GPU duration；模拟器墙钟不外推手机性能。No process found的meminfo记录为NOT_AVAILABLE，不计零内存。

## 日期、播放与权威一致性

API35当前parity逐个从同一4月/12月存档开始，通过正式Session命令执行攻击及真实nextTurn，2D/3D×normal/off/skip各六种，共12。50对expected/observed sg11重新读取并逐字节比较，含规则RNG、完整序列化结果；原始字节及每对SHA256/大小均保留。自动存档断言在探针内部核验，不能误称它是用户手动保存/读档。

API29及API35所查原始全屏可见190年5月上旬、191年1月上旬，与权威date、snapshot worldMonth、实际banner身份/attached/focus/shown、Overlay/UI draw、Window帧、3D提交及Surface原图对应。190年12月跳过后真实重建的banner身份改变，原始整屏仍191年1月。API29对应三张独立整屏也显示正确日期，但同样不是正式剧本陈旧日期复现路径。该证据只证明此fixture与设备路径；正常剧本日期探针仍未到达，API29已知陈旧日期不能据此宣告修复。对话框/前后台与完整四季组合尚未全部闭合。

## 独立视觉结论

正式APP画面与参考分开审阅。官方PC手册第7页xref240提取原图1276×940，来源与版权记录保留；小沛195年1月春季，准确镜头UNKNOWN。洛阳用户参考02本次URL返回403，保持来源记录但不编造取得图片；原参考01仍缺失。不同区域/构图不作像素级相似性通过。

当前真实冷启动/原R12录像表明全国预览未完成、正式视图长时间仅局部对象和背景；这本身不满足可操作完整世界。fixture平地能显示城市/部队与日期，只用于fixture功能，不用它冒充正常V2场景。历史同一v112正常地图原整屏和Surface作为辅助；代码和300assets未变，不把历史图取代当前必要多视角验收。

| 视觉项 | 当前结论 | 缺口 |
|---|---|---|
| 连续地形轮廓/材质层次（V1） | PARTIAL，原缺陷未关闭 | 原始近中远、两方向、跨块关网格图组不足；细分三角形不自动改善轮廓 |
| 水岸/港口 | FAIL 岸线几何；航行矩阵未全测 | 当前API35正常平原Surface原图逐格阶梯明显；WaterVisualField仅平滑材质带，未实现要求的受约束岸线几何细化 |
| 城/关/港比例、轮廓、贴地 | NOT_RUN完整验收 | 不能以资源映射覆盖代替正式场景多LOD/方向的美术检查 |
| 植被/道路/林缘/穿插 | NOT_RUN完整验收 | 三代表区多角度/动态切换与过绘成本缺失 |
| 部队全类别/动作（R10） | PARTIAL | 13族HOST映射有证据；实际全兵种脚滑/水位/转弯/动作图组缺失 |
| 特效全事件（R11） | PARTIAL | 实际事件覆盖不足；不把fixture未发出的事件视作已测 |
| 光照/阴影/四季（R14） | PARTIAL | season已接入，但四季相机图组、阴影稳定/过绘/三档设备检验不足 |
| UI与3D共同输出 | FAIL正常全流程，fixture局部通过 | 全国ready与正式初始ready阻塞；API29日期问题未关闭 |
| V2整体局部样板 | PARTIAL，未通过 | 正常同一区域全要素/真命令/PC参考构图未闭合 |

本次证据包收录53段原始MP4（含原最终生命周期10段），逐段ffprobe解析、ffmpeg完整解码与抽帧内容核对分开记录。原采集器最多10×180秒录像，长生命周期末段可能未录到；日志完成轮数不伪称全程录像覆盖。抽帧是原录像内容查看，不是独立屏幕PNG，也不冒充Surface原图。完整流程结论以交互/断言/原始整屏共同判断，录像能播放不等于玩法、美术或性能通过。历史损坏录像没有用新录像覆盖其失败。

## core与受影响回归

输入和候选均执行原`test-core.sh`，首失败`AI uses deployment commands / CoreTest.logistics:75`。随后保持原42调用与断言逐个收集退出码，串行隔离重跑，双方12退出0、30退出1。部分首次日志缺少输出，另以合并stdout/stderr重复原调用保留补证，不将无输出写PASS。完整调用矩阵、失败文字及继承/新失败判断在`audit/host/core-comparison.json`。

20个原生/架构/地面流送/帧获准/overview材质/Window-Surface主机套件实际执行exit0。它们只证明对应断言，不替代安装运行。JDK17模块编译器通过本地javac薄包装调用，未改原测试语义；初次环境127也保留。生产源码零差异，没有以改AI/预期/断言冲绿。本轮新发现的是API29测试API兼容故障；正式全国ready/lifecycle/date/V1/V2/R10/R11/R13缺口为继承或此前未覆盖，不伪称已修复。

## 依赖明确的续作顺序

1. 先修正常冷启动全国预览/初始ready：上述CPU→GPU→Surface链；保留修复前原始录像和严格120秒断言，核验整个地图资产/LOD精确内容，不能缩图/隐藏对象/2D回退。
2. 修设备前后台/Surface恢复及日期实际合成：先确认HOME、焦点、Window、gate事件真实分发，再定位旧可见控件/Window帧/Surface提交；勿按getText单值判通过。完成20+20及迟到worker/fault/资源回收。
3. 重新完成纯触控新开局全部链，恢复被阻塞的原R12、日期/播放与手动存读档；同时扩大全权威必含场景。
4. 独立关闭R02视口/触点设备矩阵、R13 SAF断写/自定义开局/旧档及头像回归；修构造器同步解码的最小正式路径，不新造加载器。
5. 在正常运行路径稳定后关闭V1/V2、R10全类别动作、R11实际事件和R14四季视觉，再补ARM64两驱动家族性能/长稳。此顺序不授权开始R15。

## 制品和发布

原生产构建CI：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36213760665 （精确source构建app、androidTest、lint；命令 `./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true`；完整日志job108325502912）。本次复验：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36222182595 。兼容测试：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36222424271 。PR：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/67 。

Release读取能力本次成功；当前连接器没有Release创建/资产上传写接口，无法据此证明发布能力，也没有把旧403冒称本次新403。使用独立APK附件和可下载CI制品；不绕过权限、不提供虚假Release链接。新CI证据保留14天（当前列表到2026-10-10），另提供完整本次证据包。机器可读manifest列身份、来源、每个文件hash、制品ID、设备与具体限制。
