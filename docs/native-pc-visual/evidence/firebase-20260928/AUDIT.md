# Firebase 真机 R00–R14 验证与差距确认 — 2026-09-28

**R00–R14 未全部满足原始要求。** 本轮确实执行真机、回收原始产物并重跑主机检查；没有生产渲染修复，没有改玩法、存档、地图、引擎或版本号，也没有合并 main。

本目录的 `matrix.json` / `matrix.csv` 是逐条结论，`runtime-cases.json` 是逐轮真机与专项结果，`SOURCE-REVIEW.md` 是源码定位及判断范围。原始要求 ZIP 已取回、完整阅读并与仓库原文核对：SHA256 `74e794192d71311b84fed6dfbfd8f25c54a1aa334fbd8234d9e36d145640d03c`。矩阵含459条原包定位和4条明确标注的历史补充定位；不是463个独立功能，不计算“完成百分比”。

`PASS` 只覆盖该原文条款及注明的执行环境；`FAIL` 必须有本轮失败或明确源码缺口；`BLOCKED` 记录明确环境/参考/额度阻塞；`NOT_RUN` 表示完整复合条件未执行闭合，即使已有部分支持测试通过。旧状态只保留引用，不继承为本轮结果。

## 受测二进制及测试版本

全部本轮 Firebase 安装使用首次运行的同一个 app，先校验原哈希；扩展用例只重编测试 APK。旧 app 的源码 SHA 与新测试代码的编译 SHA 分别记录，不能拿测试端 BuildConfig 代替 app 身份。

- app source：`17bd3376508499f2681d3e296939452383506a1e`
- app SHA256：`98edd7f758d41b89826d03e6fd1be3366bbea173d679a58bdd5f157b8e9479f4`
- 文件：`sanguo11-native-v112-17bd3376.apk`，37,387,038字节，universal debug / 仓库开发签名。
- applicationId：`game.sanguo.mobile.dev`；versionCode112；versionName `0.112.0-native-first-asset-lod`。
- APK内ABI：arm64-v8a、armeabi-v7a、x86、x86_64；物理执行为ARM64。未生成单独裁剪的ARM64 APK。
- 后端：Filament1.56.0 / OpenGL ES。
- 测试原包 SHA256：`3118b6b50d4677cadf787cb0d87323af375d479ac79e0dda74cd253597d83ed0`。
- 第二轮API35测试源码a1ee687，test SHA256 `eb5926874b9ba8f38acd5028cab574e346ba9c15d00d5daff7f15762d55f125e`。
- 第二轮API29测试源码5f04b79，test SHA256 `4940f007880cf89b9b4dd08fe5b8ce900ffa341167b0184e569426e479c03db8`。
- 第三轮API35测试源码ed2a70b，test SHA256 `ec1354ff442001129ce544f36c9ecffaa180630c464f9e54a4bd0a764590918f`；完整结果见 `runtime-cases.json`。

最初本地APK提取不完整，交付检查发现后已从完整原始ZIP重新提取；最终APK逐字节哈希与CI一致、所有ZIP entry CRC通过。本地截断文件没有用于真机。

## 独立冷启动与额度

每轮是独立 Firebase 矩阵/安装；前四轮原始冷启动检查确认 auto.sg11 不存在且 MainActivity.world 为空。第五轮在Activity启动处阻塞，未达到这两项运行断言，不计冷启动完成。没有预热、自动存档fixture、隐藏剧本入口、直接业务调用或反射写World。测试发现模式只报告名称，不启动Activity。原 `ready()` 的120000ms未变，保留 beginFrame 判断。

三种设备使用前都查询当前 physical 型号/API支持及容量。Spark effective physical上限5/日；Cloud Monitoring明确返回需要启用billing，项目未启用结算。本轮新增测试每次提交前读取全项目所有history/execution分页及矩阵设备执行数，保守使用滚动24小时计数；不能把“每日5次”当当前剩余5次。没有开启Blaze/结算，没有付费执行，没有flaky retry，扩展入口禁止Actions rerun再次消耗额度；最终为API29首轮入口补同样的attempt=1守卫。本轮实际均只有attempt1，没有以重试覆盖失败。

具体逐轮表由 `runtime-cases.json` 给出。首API35的1个真实JUnit case、instrumentation显式PASS、全屏/Surface/连续视频和运行记录已核验，能够计入独立冷启动次数。其势力点击为默认已选何进，证明触点/确认发生，不证明更换所有势力。

| API / 设备 | Actions / 矩阵 | 真实JUnit结果 | 冷启动范围 |
|---|---|---|---|
| 35 / Pixel8 shiba #1 | 36362991199 / matrix-2qh8xls7v829b | 1测试，1通过 | PASS，17.056秒 |
| 29 / GalaxyS9 starlte #1 | 36365234180 / matrix-xof82udfz39ha | 1测试，1通过 | PASS，28.566秒 |
| 35 / Pixel8 shiba #2 | 36365917134 / matrix-30ogbk6397ncu | 4测试，3通过1失败 | 冷启动PASS；横屏全链FAIL |
| 29 / GalaxyS9 starlte #2 | 36366893750 / matrix-3qstawfpulggu | 5测试，3通过2失败 | 冷启动PASS；横屏全链、竖屏探针FAIL |
| 35 / Xiaomi14 houji #3 | 36367763861 / matrix-3q2x556thgk7g | 1测试，1失败，2579.001秒 | 启动拒绝后超时FAIL；预览验收BLOCKED |

API35为2次冷启动PASS、1次Activity启动失败；API29只完成2次独立冷启动，第3次因免费预算不足 **BLOCKED**。五次物理执行均保留，未提交第六次。因此“两个API各至少3次且稳定完成”**没有满足**。

第三次API35的logcat明确记录 `MIUILOG- Permission Denied Activity`，目标MainActivity，系统START结果102；instrumentation最后报 `ANDROID_INSTRUMENTATION_COMMAND_EXEC_TIMEOUT`。最后正常步骤是测试进程记录设备/API/ABI；第一个被拒步骤是`startActivitySync`启动Activity。没有cold-runtime、截图、视频或Filament驱动日志，不能推断CPU交付/beginFrame/GPU上传已发生，也不把此环境启动阻塞定为Filament缺陷。设备为23127PN0CG / Android15 / arm64 / HyperOS2.0.109.4.VNCEUXM；没有形成该设备GPU渲染覆盖。没有绕过厂商安全策略。

## 三个专项

**全国预览**：同轮原始日志和源码追踪CPU epoch1 / coarse结果、Choreographer帧回调、beginFrame attempts/rejected、GPU preparation与submission平衡、pending清零、render/endFrame后Surface PixelCopy以及完整UI叠加。首API35 attempts101/skipped2/preparations99/submissions99；首API29 198/4/194/194。两轮第二次冷启动亦完成相同正常路径，逐轮原值在runtime索引。没有根据绿色矩阵推导画面成功，没有把两次预览换renderer算两次冷启动。

**日期**：Pixel8第二轮从184年1月上旬经中/下旬至2月上旬；GalaxyS9第二轮从3月中旬经下旬至4月上旬/中旬。World日期、snapshot.month、可见hud.date及四张原始整屏像素逐张相符。snapshot仅含月份，不能伪称包含年月旬。两轮confirmedLosses=0、relayouts=0；正常Window帧持续推进时恢复分支未触发。因此正常跨月显示形成有效证据闭环，但历史backing Surface丢失的异常修复、跨年和重建矩阵仍未闭合。没有用再次setText、改日期算法或3D季节换色冒充UI恢复。

**生命周期**：两API各完整20次2D/3D切换、20次真实HOME/返回；逐轮旧engine释放、activeNativeHosts=1、后台focus/resumed/queued=false、两次后台采样帧数相等、返回后帧增加、完整SaveCodec字节不变。20份资源计数各自稳定。切换使用展示API，明确与纯触控链分开。MainActivity/预览Dialog/MapHost/SceneRenderGate/FilamentMapView的resume、cancelFrame、schedule路径均复查，未删除暂停机制。低内存、系统杀进程、故意延迟worker及故障构造仍NOT_RUN，计数稳定不等于证明所有native/GPU泄漏不存在。

## 完整纯触控链

新增后半段真实触点用例，实际运行而非只提供测试建议。两API横屏都在DeployWizard选将处失败：武将ListView和行不可见，无法选主将。整屏图与UI树一致。固定高度编队区/76dp详情区/工具条占满有限高度，权重列表被挤出。这是当前正式UI实现缺口，**全链FAIL**。

API29独立竖屏用例通过真实方向菜单、选将/剑兵/确认出征，并移动部队及推进7旬；旧选点策略未绕河到攻击位置，最终断言失败，归类为 **EXECUTION_PROBE**。不能据此断言攻击规则有缺陷，攻击/战报/存读档也不能算已完成。

最后一轮测试代码将选点改成只读权威路线查询，仍由MotionEvent和正常按钮提交；另增独立手动存读档、30分钟混合操作入口。但该轮在首个冷启动用例中被系统拒绝启动并超时，余下6个用例均 **NOT_RUN**，包括修正路线、手动存读档、30分钟混合操作。43分钟外层超时绝不等于完成30分钟游戏运行。全链未实际跑通。

## R00–R14 支持测试与仍存范围

| 范围 | 本轮实际执行 | 未关闭项 |
|---|---|---|
| R00 | 原始app/test/lint、保护树、真机新局首段、原文逐条核验 | 全部启动/旧档/异常与美术门槛 |
| R01 | 队列/边界主机；两API完整20+20、live计数 | 同步解码明确FAIL；故障/延迟worker/进程死亡 |
| R02 | 38949有效格、399124投影/拾取检查、1548相机样本 | 实际设备三渲染分辨率和完整触点/遮挡矩阵 |
| R03 | 全局共享边/LOD、32块复用4块变化、局部失效与权威不变 | 正式全镜头跨块/山脊谷地/道路视觉 |
| R04 | 材质权重/UV/归一化、正式材质安装与当前matc编译 | V1/PC配对、全部mip/色彩/画质图组 |
| R05 | 水陆拓扑、803860检查、35港口、真实PNG | 阶梯岸线明确FAIL；真机船行/窄口全录屏 |
| R06 | 199恶意输入、182输出可重复、171模型validator、matc56 | owner同步纹理/动画元数据解码FAIL；全部取消/异常设备路径 |
| R07 | 679据点映射、5族、120设施LOD及预算 | 全建造/受损/易主/自定义据点真机及PC近景 |
| R08 | 确定散布/排除/局部更新、缓存估算 | 森林道路/山谷关口/农田全视角和GPU性能 |
| R09 | 正式洛阳32×32样板1602主机检查 | V2水岸、区域/季节/相机/版本齐全的PC对照 |
| R10 | 13类别×8clip×2mesh，2221136检查，正常/倍速/跳过权威字节 | 全类别/全部LOD/动作的真机美术与压力帧时 |
| R11 | 实际规则命令的独立fixture，8kind/9style/7播放模式及新增CALM/EXTINGUISH真实命令fixture | 全事件真机效果/战报/收益/自动存档闭环 |
| R12 | 触控首段、日期专项、横屏失败与竖屏部分执行 | 完整纯触控链FAIL；四开关组合/范围/分辨率全矩阵 |
| R13 | 正式编辑事务/失败回滚/增量mesh/自定义地图武将及包安全 | 系统SAF、导入中断、编辑CRUD/头像/新局真机端到端 |
| R14 | 590日期/季节/存档/权威检查、两API正常跨月像素 | 四季×三景别×网格/画质完整图组、异常窗口恢复 |

主机计数只是日志定位，不是功能完成比例。刚体动画是原契约允许路线，不将“无骨骼/IK”自行当作缺陷。材质平滑不能抹掉矩形水域几何造成的阶梯岸线。资源映射存在、主机加载通过也不能代替美术目视。

## core、当前CI与环境失败

core原42条调用在输入基线8b42af2和当前生产17bd分别执行，双方均12退出0、30退出1；`CoreTest.logistics:75 AI uses deployment commands`为共同继承失败。初次3份候选stderr不完整的收集问题保留，并用原命令双边重跑确认对应断言；没有把这类规则失败混入渲染故障，也没有顺手改规则。另在实时main `ac29b458325b52d6e302ca44270d16552de4ed7f` 的独立worktree实际执行原test-core.sh，同样首先在logistics:75退出1；该main执行按原脚本停止，不冒称继续跑了42条。

主机最初缺javac入口、保护树缺历史Git对象的失败保留；使用已安装JDK17模块补launcher、取回精确对象后仅重跑受影响检查。最终对应检查通过，不删除首次错误。

当前PR仍有红色CI。已读取最新ed2a70b的工作流/步骤/原始日志；R00/R10/R13/R14及Android core均保留logistics失败，R05还保留PortReplayTest.aiDocks失败。当前R01模拟器的原始证据另回收：API29/x86_64初始ready超时，CPU36块已交付、pending30，frameCallbacks599、beginAttempts593、beginSkipped591、GPU preparation/submission均2，输出WAITING_FRAME。最后正常步骤是CPU交付与帧调度；首先停止推进的是成功frame admission，随后上传和呈现不前进。不能据此直接认定Filament有bug，未删除beginFrame判断。该模拟器是ed2a70b独立构建APK，**不并入17bd真机APK的结果**；其SOURCE_SHA/APK_SHA256保留于附加证据。

## 下一步与不放宽的门槛

1. 首先修复`DeployWizard.show/crewPage`横屏布局，使主将列表和确认流程可触达。修复前两API失败图/视频/UI树已保存。单一变量最小修改后必须重建app/test、记录新SHA/hash并重跑受影响纯触控链，不能沿用本轮旧app的PASS。
2. 单独消除Filament owner上的同步Bitmap/atlas/rig/clips解码；保留有界任务、代际取消及owner上传/销毁，配对验证主线程阻塞与恢复。
3. 按原水陆/港口约束修几何阶梯岸线，保持权威拓扑，补PC同区参考和真实图组。
4. 先处理小米环境的测试启动适配：保留拒绝证据、增加有界启动失败诊断，核对正常授权启动路径；不绕过厂商安全策略，也不以延长ready掩盖。免费额度恢复后重新读取额度/设备，补API29第三次以及API35仍欠的一次成功独立冷启动，失败记录不删除。继续补完整纯触控链、独立手动存读档、30分钟混合操作、异常窗口恢复、SAF/编辑、全LOD/事件和性能测量。不得自动消费下一天额度，不升级结算。

原始证据包与可下载APK随交付提供；具体文件、哈希、Actions/Firebase/GCS位置见证据索引。最终远端HEAD与实际运行结果在最终交付记录中单独复读；报告HEAD不冒充APK源码。
