# v107 P0 运行续作 / PARTIAL

当前 APK source `a4b09058c6b1ab8c80f57d96f1e22425673de94c`，v107。详细正式修改、原 v105 独立前测、候选结果及未到达操作见 [R00-R14-P0-v107.md](R00-R14-P0-v107.md).

本轮削减全国重复法线/材质计算并合并远景批次，全部几何和规则字节保持；增加真实队列、CPU、提交与Surface采样诊断。日期实际画面、完整纯触控链、20轮设备生命周期、完整模式RNG矩阵仍未闭合。原213条不因主机套件通过而自动升级。

v106新增API29 CPU超时在v107全国ready回归中通过；API29实际UI停绘、API35原R12下一旬超时仍FAIL。完整触控链NOT_RUN。详见当前报告最终结果表。

以下为v105及更早历史，保留追溯，不代表当前APK：

# R00–R14 续作复验（2026-09-25）

最终候选为v105，source `ef54951dbb7056beaa782f97a3486eefa3a88ef9`；下文保留v103/v104失败及对照，不能把各轮结果混用。

总体 PARTIAL；本报告是实时远端 a5119b869f710949731781b35f13858cc87809c0 之后的续作，不重置或覆盖上轮成果。原整改报告保留为历史证据，不能把它的 v102 当成本次新 APK。main 读取为 ac29b458325b52d6e302ca44270d16552de4ed7f；继续同一 agent/native-pc-visual 分支、Draft PR67。未合并 main、未 force push、未处理其他 PR、未执行 R15。

## 正式代码变化

本次针对 REMED-03 修改 `MapHost.switchMode` 和 `FilamentMapView.fit/onSizeChanged/snapshot`，接入正常 ScenarioFactionPicker 的首次原生预览路径。

根因：正常启动页经真实触点打开剧本预览后，Canvas 相机 Bundle 没有 `sceneSpan`。首次创建 Filament 视图时 `restoreCamera` 使用默认 span15，导致全国预览只显示局部；已有原 R12 探针随后显式调用 fit，所以无法发现这个入口差异。修复只在 openingPreview 且没有原生相机 span 时请求一次 fit；实际布局尺寸和 snapshot 尚未可用时保留 pendingFit，由正式布局/snapshot 回调完成。已有原生相机和用户缩放继续恢复，未强迫每帧 fit。

保留原有有界地形缓存、静态势力层缓存、SceneRenderGate、正常 GPU 所有者线程、全部地图对象和原 ready120秒条件。没有改地形/玩法/AI/RNG/SaveCodec、基础地图、七格占地、港口规则或任何生产资源。298份 APK 资产逐项与 source SHA 比较完全一致。没有以本次相机修改冒充 V1/V2 美术整改。

`NativeColdStartInstrumentation` 在原 live-overlay 断言之前加入全部有效地图格投影检查，记录 total/outside/span/viewport，并保留旧严格断言。测试从 pm-clear 隔离数据、无 auto.sg11 的正常启动页开始，真实触点打开剧本和 2D/3D；没有额外调用 fit 来帮助候选通过。输入对照安装已发布 v102 原 APK 原文件（SHA256 ab87acc3f151594260ee8c07b802b39ff8bb354712e959c54252dc51680cd2ba），只重新编译相同 instrumentation。

日期方面新增的只有观测：实际 decor attached/shown/focus/visibility/hardware/layoutRequested、Window 和 SurfaceFlinger dumps，与原权威日期、TextView、snapshot、onDraw、Window帧、原生Surface、独立整屏和录屏一起采集。没有加入月份硬编码、测试专用重绘、Activity重启或未经定位的软件渲染开关。

## 第一轮主机、构建与身份（v103，历史候选）

CI36104156087 的16组主机套件（R00–R14 audit、R01–R14、architecture）全部 exit0。完整 core 在输入 a5119 和候选5d9384分别独立执行，均 exit1：`AI uses deployment commands / CoreTest.logistics:75`。分类门禁通过只证明继承失败一致，完整 core 仍 FAIL。主机模型中的20轮暂停测试不等于安装应用20轮生命周期验收。

本地主机另跑 R01、R02、R12、architecture 通过；R02 399124 checks，R12 NativeR12Test 395124 checks，均是主机断言数而非真实触点次数。本地 audit 首次因没有 javac 可执行文件 exit127，保留原日志；使用同一 JDK17 的 jdk.compiler 模块入口重试后通过，没有修改断言。全部原始日志及退出码在 evidence/continuation-5d9384f9/。

APK source：`5d9384f9cba7810ebe9158d531a1c0ec4d6b9659`。

- 应用ID：game.sanguo.mobile.dev。
- 版本：103 / 0.103.0-native-preview（从实时远端102递增）。
- SHA256：a481246a45969e73c0ef78697f3b43da2ef60c0e2a4ad95062d095a0bac35db4。
- 大小：37370473 bytes。
- ABI：arm64-v8a、armeabi-v7a、x86、x86_64。
- 开发证书 SHA256：8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24，与输入一致。
- 构建命令：`./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true`。
- 精确 source SHA 的 assembleDebug、instrumentation 编译、lint 成功。APK字节哈希和全部生产资产已独立复核。
- CI：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36104156087
- PR：https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/67

## 保留的未闭合要求

本次不把继承的已修缓存缺陷重新计为新修复。日期/播放真实画面、完整新开局纯触控链仍需按实测结果分别判定，不能仅凭机械 PASS AUDIT、getText、文件名 national 或非空 Surface 宣称通过。

资源纹理解码/图集/rig 等 owner CPU 工作、共享引用/上传完成/过期worker/预算驱逐完整闭环，20次实际切换与20次前后台、坏资源和部分初始化恢复仍未完成。V1未闭合、V2 FAIL；没有新模型/贴图/材质/生成器美术整改，近中远双向、网格对照和有效 PC 连续运动验收仍未完成，不扩散模板。

R02/R12 全视口/安全区/方向/动态分辨率真实触点、范围权威集合和开关持久化完整矩阵；R10 全实际兵种装备、动作、脚滑漂浮/靠岸/LOD；R11 含 CALM/EXTINGUISH 的所有事件和正常/暂停/倍速/跳过/重建；R13 真实 UI 城港关编辑/头像/自定义武将/新剧本及外部 SAF 取消/撤权/断写/冷启恢复，均保留原未闭合状态。外部 DocumentProvider 不承诺原子替换，内部原子保存不替代其能力。

同存档/命令/RNG 的2D/3D×正常/关闭/跳过动画完整权威对照尚未覆盖全部组合。原213条复合清单逐条保留；本次主机套件通过不能自动提升相应复合条目为PASS。

可用设备仅 GitHub Actions x86_64 SwANGLE 软件模拟器（API29/API35）。无ARM64真机，型号/API/驱动、30分钟长稳和真实PSS/GPU性能均 NOT_RUN。ABI含arm64不等于真机测试，No process found不算零内存；模拟器计时不用于手机性能结论。

## 第一轮实测：v103 / CI36104156087

| 对照 | 原完整 R12 | 正常冷启动入口 | 日期实际整屏 |
|---|---|---|---|
| 原 v102 / API29 | FAIL，预览pending515 | FAIL，span15，38949格中37406格在视口外 | FAIL；七月/四月仍一月 |
| v103 / API29 | FAIL，预览pending517 | FAIL，已span86.256714，但pending559导致ready超时，未到投影/选势力断言 | FAIL；七月/四月仍一月 |
| 原 v102 / API35 | PASS 60141 checks | FAIL，span15，37406格在视口外；断言导致测试进程退出，原始结果保留 | 1/7/4月整屏正确，属于未改APK的控制结果 |
| v103 / API35 | PASS 60111 checks | PASS scoped123 checks；38949格全部在视口内，真实触点选势力/确认/进入184新局 | 1/7/4月整屏正确，但不是本次日期修复成果 |

上述 checks 包含 readiness 轮询，绝非独立用户操作数。正常冷启动探针只完成至新局；出征/移动/攻击/战报/下一旬/手动保存读档完整触控链仍NOT_RUN。API29旧、新两版的原R12全国预览本轮都再次失败，故不能沿用上轮一次R12成功宣称超时已稳定关闭。原120秒条件保留，未以API35通过覆盖API29失败。

本轮 v103 的全部原始PNG、MP4、日志、退出码已原样发布至 [v103部分候选Release](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/releases/tag/native-remediation-5d9384f9cba7)，四份 `native-remediation-{baseline,candidate}-api{29,35}-evidence.zip` 各自有SHA256；下载后逐份核对并解压。`RAW_SHA256.txt` 保留原始字节身份。

API29 日期进一步定位：同一实际 decor attached/shown/focus均true、visibility0、activityDestroyed/finishing均false、hardware true、layoutRequested false。WindowManager记录该 MainActivity isVisible/isOnScreen/mHasSurface均true；SurfaceFlinger保留1080×1920主窗口buffer且queued-frames0，原生SurfaceView 918×1047另有queued-frames1。客户端 Window PixelCopy却报告无backing surface。v103 onDraw固定12/Window帧11；旧APK固定13/12。权威、snapshot和TextView的月份均更新，3D Surface继续更新，两个独立整屏仍一月。不是旧Activity被销毁或简单丢焦点的证据，也不是只有单一截图API陈旧。客户端窗口buffer/HWUI恢复仍需定位，未证明纯采集问题。

同一原v102 APK在API35实际七月/四月正确，Window PixelCopy和窗口帧持续变化；因此“日期在API35正确”不是相机修复带来的结果。API29/35差异只缩小调查范围，不能直接归责模拟器，日期/播放完整验收继续FAIL。API35候选四月Window PixelCopy一次status2，UiAutomation与shell两份整屏正确且帧继续；该局部复制失败单独记录，不能混淆整屏判定。

记录元数据勘误：历史测试日志首行包含写死的“API29”，原R12/Cold中的BuildConfig常量还会被测试APK内联。真实设备/API以manifest、运行时host.report及Window dumps为准，生产APK身份以已校验APK哈希和反射读取的SOURCE为准；不将测试APK编译SHA冒充旧APK源码。

对28段原始录屏逐一ffprobe：23段可解析、5段损坏（moov atom not found等），全部原样保留并在 `v103-video-validation.json` 列明。API35候选cold-start唯一录屏段损坏，故该链没有合格连续录像证据，不能把截图和触点日志替代录像门槛；原R12和其它有效段只按实际内容使用。没有以解码后的缩略图替代原PNG，未接受V1/V2。

## 第二轮正式上传调度修复

API29失败时CPU mesh队列已空、generation仍1、全国仍coarse，地形177块/装饰788块已生成，但GPU地形只有131/133块，pending517/515；并非缩图、CPU队列未返回或高精度全国生成。现有固定每帧最多2块使已完成CPU的全国首屏继续积压。第二轮复用原 loadVisible 两个上传循环，改为每帧最多8块并以owner侧实际上传调用累计耗时4ms停止本帧后续上传（System.nanoTime墙钟耗时，含调用阻塞；不是纯CPU占用或GPU时长）。只对实际创建计费，扫描已有resident的开销不会耗尽预算导致永久饥饿；单次不可抢占上传可能超过4ms，故4ms不是硬实时保证。地形先于景观、原有视锥过滤/LOD替换/缓存释放不变，pending仍计全部应上传对象，未改READY或超时。

实际GPU创建仍在owner线程；增加每帧上传数/CPU耗时诊断，不将CPU提交耗时称作GPU时长。版本104 / 0.104.0-native-upload，source `a9d944310249b26ab3167ea5407f8111758a29c8`；CI36106137919使用v103原APK原字节作对照，再跑同样API29/API35矩阵。此修改不能独自证明完整生命周期或日期故障关闭。

第二轮 APK SHA256 `d57cf80d3747fcb65cb7c982ad0abe1e2bf4e61750e9fe6fd6a4f5e81a703690`，37370469 bytes。应用ID、四种ABI和开发签名保持；同上构建命令，精确source编译/测试APK/lint成功，APK中298份资产逐字节与a9d944源码核对一致。

第二轮API35失败分类（原始失败不删除）：原v103对照的cold-start整屏明确显示“Quickstep isn't responding”，InputDispatcher记录触点本应属于游戏uid10148，却被 `Application Not Responding: com.android.launcher3` 系统窗口拦截。此前focused/R12的frame_queued=false、submitted0与遮挡暂停一致；这是有截图和窗口日志的实际测试环境阻塞，不以此归类API29日期问题。候选v104 focused机械89 checks通过，1/7/4月原整屏正确；原R12在combinations因UiAutomation整屏返回null而FAIL，cold在首次2D/3D断言FAIL且日志未见Filament初始化，不能把这两个失败改记为已通过。独立冷启动采集CI36106664924未取得APK，不能覆盖原失败。

采集脚本另修停止录屏后的收尾：先设置停止标记、向screenrecord发送SIGINT、等待进程完成容器封装后再pull；不再直接杀掉外层循环后立即复制未封装文件。这个改动只在测试采集脚本中，APK源码仍a9d944。独立CI36106664924原计划只重建instrumentation并核验原APK；但v104主CI发布Release返回HTTP403 Resource not accessible by integration，复读Release为404，独立重录未执行成功。不可宣称该轮有新有效录像。原APK与证据仍由Actions artifact保存，最终交付采用已授权的制品下载，不改变仓库访问控制。


第二轮API29实测：候选v104 focused机械130 checks通过，但实际七月/四月仍一月、onDraw18/Window17停止。原R12和冷启动仍FAIL，首个CPU结果只有22块地形/119块景观、terrain_all_coarse=false；全国相机已是86.256714，worker_pending1/submitted2。原v103 APK对照focused121 checks，原R12/冷启动分别pending589/479超时。不能把4ms上传预算写成已解决预览。

## 第三轮：初始全国相机先于CPU任务（v105）

v104日志暴露另一条真实初始化问题：MapHost先publish snapshot再restore/fit相机，即使随后屏幕相机全国适配，首个worker仍按默认局部窗口生成。v105将正式switchMode的publish放到相机恢复/fit请求之后；FilamentMapView在pendingFit且尚未布局时仅保存一份最新不可变snapshot，实际onSizeChanged后消费，先fit再提交原有meshWork。release清空待消费引用，不新建加载器或线程池，不改READY、120秒超时、地图对象或遮挡暂停。

冷启动探针在原全格投影和live-overlay断言之间增加首个CPU epoch==1及terrain_all_coarse检查；直接检验是否仍产生多余局部预热任务。v104已有失败作为前测，v105再跑原完整R12和正常冷启动触点链，结果不得用旧v103 API35一次通过替代。

v105 source `ef54951dbb7056beaa782f97a3486eefa3a88ef9`；版本105 / 0.105.0-native-first-work；SHA256 `2b38ca4741b60650be58555ec118350f2aeff292765f6b3754adc138c1fdf55c`，37370473 bytes。应用ID、开发签名、四ABI及构建命令同上。CI36108363433精确源码构建/lint成功，独立比较APK全部298资产与source字节一致。16组主机exit0，输入a5119与最终候选完整core仍各exit1同一logistics:75。此版本包含录屏SIGINT等待收尾修复，录像是否有效以本轮原文件ffprobe为准。


## 最终v105运行结论：CI36108363433 / PARTIAL

| 实际设备 | focused机械 | 原完整NativeR12 | pm-clear正常冷启动 | 日期原始整屏 |
|---|---|---|---|---|
| API29 x86_64 SwANGLE | exit0，122 checks | exit1，预览pending494 / WAITING_FRAME | exit1，预览pending473 / WAITING_FRAME | FAIL：七月、四月整屏仍显示一月；uiDraws9 / WindowFrames8固定 |
| API35 x86_64 SwANGLE | exit0，84 checks | exit0，60054 checks | exit1，首次“user switches actual full-map preview to 3D”断言失败，尚未初始化Filament | 七月、四月原始整屏正确；属于先前原APK也能通过的环境，不算日期修复 |

最终APK没有通过完整新开局链。v103 API35曾通过至确认新局的历史结果保留，但不能移植为v105通过。v105的首个CPU epoch==1新增断言未到达，不写成PASS。API29失败时已全国span86.256714、terrain_all_coarse=true、CPU177地形/788景观、GPU地形177、worker_pending0；原日志首个Ground CPU ready为177块（冷启动耗时102327ms），没有v104首个局部22块结果。初始化顺序的实际改进已出现，但全国生成消耗约102秒，只剩很少首屏上传时间；494/473剩余项和submitted2说明首帧呈现仍不能验收，不能只归结为固定上传额度。这是该软件模拟器本次worker墙钟测量，不是ARM64性能。

实际原图复核：API29七月/四月shell整屏均仍190年1月，3D地表却已随月份变化；API35对应整屏为190年7月和4月。Window/Surface/权威记录与原PNG一起保留，没有仅凭getText判断。API35冷启动原录像第10秒仍“正在读取剧本”，第12秒显示2D全国预览，未见3D初始化；尚不足以确定输入时序或正式交互根因，保留FAIL而非归为环境通过。API29冷启动录像末段全国势力覆盖层仍标“3D地形装载中”，不是已呈现的完整3D首帧。

采集收尾回归：v104共23段中15段可解析、8段损坏；v105共15段中14段可解析、1段损坏（API35 r12-full/remediation-5.mp4）。API29七段与API35冷启动13.1482秒片段有效，但内容是上述失败流程；录像有效不等于交互/美术通过。未裁掉损坏段，不能宣布录屏采集已完全闭合。原始帧提取仅供定位，源MP4原样保留。

v105发布Release再次HTTP403 Resource not accessible by integration，复读tag仍404；不是构建失败。CI36109036703成功读取两轮全部六份原始运行ZIP，按18MiB分片交付；连接后验证TRANSFER_SHA256和每份RAW_SHA256均一致。最终独立APK已从构建制品原字节重组核对。制品发布失败日志保留，不引用不存在的Release链接。

## 当前交付与续作边界

新增关闭的精确子问题：首次相机从Canvas转换时默认局部span15（v102原APK对照37406格在外，v103 API35变为0并真实确认新局）。这个局部修复保留在v105；最终v105冷启动复验仍FAIL，因此AUDIT-02完整预览/开局不关闭。上传预算和初始化顺序是正式实现改进，分别保留实际失败回归，不能写成整体超时已修复。旧REMED-01/02、AUDIT-09是继承成果。

本轮容量内交付上述正式小范围改动及三轮复验，没有留下新的渲染引擎、第二套加载器或待接入异步重构。后续第一优先仍是API29全国CPU生成/景观首屏与真实UI窗口不再出帧，并定位API35首次触点未切入3D；先完成正常冷启动整链，再按原顺序补异步生命周期、美术和其余矩阵。V1未闭合、V2 FAIL，R15未开始。

独立文件：`sanguo11-native-first-work-v105-ef54951d.apk`；`sanguo11-R00-R14-v105-evidence.zip`（本次报告、213条、源码patch、输入包、v104/v105六份原始运行ZIP、日志、退出码、图像/录像及最终远端复读）；`sanguo11-R00-R14-v102-v103-comparison.zip`（四份未修改的第一轮对照原始ZIP）。各包自带manifest。最终远端HEAD以REMOTE_DELIVERY.json记录，独立于APK source。
