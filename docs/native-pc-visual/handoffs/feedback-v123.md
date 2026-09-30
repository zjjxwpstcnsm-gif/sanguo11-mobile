# v123 模型与性能 — PARTIAL

基线94ab81f06c6ea24afbd491bf7d3cb3201e7e20f7，main ac29b458325b52d6e302ca44270d16552de4ed7f已合入架构PR66。承接PR67；保留Filament1.56/OpenGL、GameSession唯一权威；不改core/game-api/game-runtime/data/unity，不开始下一阶段，不合并main。

实际Blender4.2.3生成41个版本化城港关/13兵种LOD资源，保留v122及更早资源。城池内鼓楼/钟楼与栏杆，港口吊架横梁/撑杆，关隘脊饰，兵种盔脊/颈甲、骑兵鞍卷、器械加强梁。保持原关节、入口/尺度/LOD与材质；仍是程式化低多边形美术，非原版高精成品。模型细节PC参考REFERENCE_MISSING，离线渲染不是APK截图。

完整浮点属性元组合并同关节重复顶点，保留UV/硬法线接缝和所有三角面，不舍弃可见细节；+0/-0按数值相等处理，未做浮点舍入。重写rig的连续范围；生成时逐三角形核对全部属性数值相同。初版按浮点原始字节合并只减少10.2%，未过新增20%目标，原失败日志保留；修复为数值相同判定后重新执行相同断言。

正式性能路径：SiteGlb以primitive数组解码，移除每顶点Float/每索引Integer装箱；FieldAssets单代表姿态保留独立positions，复用不可变indices/UV，不再额外复制；Proxy按ground/位置/朝向/缩放/兵数/LOD/水陆状态缓存接地与GPU transform，snapshot刷新强制失效；去除animateUnits重复position调用。未降低兵力/AI/地图范围，未改变帧就绪门槛。

正常入口安装对照与10秒帧尝试记录由verify-feedback123.sh执行v122和v123同场景/视角/API29、35。原120s readiness、原始测试断言保留。主机CPU/资源缩减不等同于手机FPS；继承全国加载及API35超时、完整触控/生命周期/ARM64/30分钟性能缺口继续开放。

## 首轮构建与主机结果（证据保留）

源码56ddddfa680f13807989855be0532a1259574c1b；CI36600023870 / build109514701111 PASS，84任务、1m38s。APK39,852,561字节，v123/0.123.0-native-models-performance，game.sanguo.mobile.dev，四ABI；SHA256416178662e330f46732e8511269e90cb3f7a531492fb963360cc6d6577f04739。开发签名8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24，391资源身份及ELF/ZIP16KB对齐通过。文件保留旧资源所以APK比v122大；实际新模型GLB总量7,296,852字节，较旧8,591,748减少15.1%。顶点159042→131163减少17.5%；三角面53014→56632增加6.8%，未冒充所有GPU成本都下降。

原349资源字节全部不变，42新增包括41GLB与1rig。主机受影响专项、架构/坐标/网格海岸/战斗/UI导航通过，13类别×8动作×12帧×2LOD检验通过；R10新旧均PASS。完整core两版同点CoreTest.logistics:75失败；完整R09两版同点NativePreviewWorkTest:46旧canonical快照失败，原预期未改。

主机JDK17固定操作分配（每次，中位数）：city0-lod0解码7,611,904→4,187,896字节，减少45.0%；cavalry walk姿态351,592→183,800字节，减少47.7%。预热500次、每轮500次、5轮顺序对照，保存初测与复测原始CSV。CPU中位数city3.152→3.000ms，pose0.208→0.487ms；波动较大且姿态候选更慢，这一风险保留，不能宣称CPU全面加速或手机FPS提升。

Firebase auth job109516787155再次FAIL unauthorized_client / attribute condition拒绝；实际物理设备提交0，ARM64/Adreno/Mali/30分钟NOT_RUN。未改IAM或绕过授权。

为调查上述默认JVM波动，仅追加一次固定C1层级/Xbatch顺序对照（同一预热/采样数）：city解码CPU中位数6.759→3.934ms、骑兵姿态0.245→0.174ms，分配下降仍一致。此补测支持算法开销改善，但不覆盖默认JVM较慢原始结果，不作为Android或真机帧率证明；所有CSV及命令保留。

## 最终 APK 身份

最终源码 `7658a8bcae83e215186d6c89d114760f8d982944`；CI `36604231165`，build job `109529027789` PASS，84任务、1m51s，assembleDebug/assembleDebugAndroidTest/lintDebug及391资产身份、签名、四ABI ELF/ZIP16KB对齐均通过。最终APK 39,852,561字节，SHA256 `200bd574e176000bf072412e550d9e0bf0a355a648e35cad7597c037ff9aaa46`；应用ID `game.sanguo.mobile.dev`，版本123/`0.123.0-native-models-performance`，开发签名同上。

构建命令：`./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PcandidateSource=7658a8bcae83e215186d6c89d114760f8d982944 -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true`。

最终源码相比首轮56ddddf只修测试适用范围并增加精确复验workflow，生产源码/模型/版本完全一致。首轮API29正常城池和港口视野内没有可见部队，新增的无条件部队缓存断言因此错误失败。修正后，fixture仍必须包含可见部队且skips>0；其他场景若没有可见部队则缓存项明确NOT_APPLICABLE，所有ready/新帧/资源/GPU/整份权威字节断言保持。原120秒deadline未放宽，首轮失败日志完整保留，未改原有测试断言或把原失败改写为PASS。为使测试与APK源码一致重新构建，不能用首轮APK身份代表最终APK。

## 首轮运行结果

CI36600023870：API29旧v122三场景均PASS；v123 fixture PASS91，实际缓存uploads33/skips224，4组Surface/UI视角完成，完整存档和RNG一致，新city0与gate进入GPU。v123寿春/港口完成全部4组视角后因上述新缓存断言误用FAIL，之后的整份存档/实际GPU断言未到达；不把这两个失败写成完整通过。API35新旧各三个场景均在原120秒ready失败，保留WAITING_FRAME/待上传记录及失败截图。初轮和最终运行分别索引，不相互替代。

安装/启动路径：`adb install -r`正式APK及测试APK，`pm path`拉回安装字节并cmp/SHA核对，`am instrument -w ... NativeFeedback123Instrumentation`启动正常MainActivity，正式auto.sg11恢复→MapHost→FilamentMapView。fixture明确是测试地形输入；national/port来自第一官方剧本的寿春/首个港口。镜头中心、span5/10、网格开关由展示API驱动；不冒称纯触控拖动/缩放或完整新开局链。180秒上限录屏不覆盖超长测试全部时段。

## 视觉与发布边界

Blender离线18个LOD0预览和packed .blend随证据交付；它们不是APK画面。已检查首轮实际API29近景城池/港口/关隘部队截图及v122对应城池/港口截图：楼阁和吊架细节可见，城港关轮廓及真实位置保留。当前城池仍较方整、士兵面部/衣甲简化、树冠呈分层硬片、岸线与地表细节不足；未完成全兵种实机动作美术复核，没有PC同区域/镜头参考，REFERENCE_MISSING。不能称最终高精美术或全地图美术PASS。

原core/R09继承失败保留。API35装载/呈现不稳定、完整触控/生命周期/旧档SAF、全兵种全事件画面、ARM64/Adreno/Mali与30分钟热稳仍是后续阻塞；没有授权成功的物理测试，不报告真机FPS。没有合并main、重启Unity、修改玩法/地图逻辑或自动开始下一阶段。

性能采样边界：安装脚本在最后视角停留10秒后读取现有300条有界attempt环形缓冲；该CSV没有呈现时间戳，不能把300条当作精确10秒帧数。首轮API29 fixture owner wall P95旧54.335ms/新915.395ms，national旧1389.192ms/新1293.298ms，port旧1278.907ms/新22.300ms；admitted比例也不一致，含大量beginFrame拒绝/等待。所有样本保留，但这些软件模拟器/非同等呈现状态的结果既不能证明整体变快，也不能作为手机FPS指标。算法分配降低已验证，整体运行性能仍PARTIAL。

## 最终安装复验（精确最终包）

CI36604231165整体FAIL；构建PASS，4个场景PASS、2个场景FAIL。六个安装拉回SHA均与最终APK一致。本地再次核对4对完整expected/observed.sg11逐字节相等；不是仅比较局部字段。

| API / 场景 | 结果 | 实际范围 |
|---|---|---|
| 29 / 部队关隘fixture | PASS129 | 4组Surface/UI，visibleUnits2，缓存uploads33/skips226，整份存档/RNG一致，实际新模型GPU |
| 29 / 官方寿春 | PASS138 | 4组Surface/UI，城港关GPU资产、整份存档一致；无可见部队，缓存NOT_APPLICABLE |
| 29 / 官方港口 | FAIL | 原120秒ready，submitted9/pending0/WAITING_FRAME，画面不完成；后续断言未到达 |
| 35 / 部队关隘fixture | FAIL | 原120秒ready，submitted4/pending1/WAITING_FRAME；后续断言未到达 |
| 35 / 官方寿春 | PASS105 | 4组Surface/UI，真实GPU资产及整份存档一致；无可见部队 |
| 35 / 官方港口 | PASS104 | 4组Surface/UI，真实port模型GPU及整份存档一致；无可见部队 |

最终API29部队/关隘与寿春、API35寿春网格/港口Surface实拍已查看，新增模型实际出现；API35 fixture失败画面仍显示装载中、关隘白色未完成材质，不作为美术PASS。16组Surface/UI来自四个通过场景，另外保留两组失败整屏；所有原PNG/MP4/logcat保存。6段最终视频容器/流元数据可解析，但没有将其冒充逐帧完整视觉验收。API29港口首轮可显示、最终包超时，API35部分最终通过而首轮失败，说明稳定性未闭合；不能推断为已证明的纯环境问题，OPEN回归风险保留。

实际GPU新模型包括city0-lod0 6392三角面、port-lod0 3322、gate本体2033（贴山侧墙可增加到2109）；仍需全兵种动作和全部LOD的真机可见性验收。PC参考缺失、完整纯触控流程、实际呈现帧时/GPU时长和ARM64长稳未完成。报告后续提交只含docs/native-pc-visual文档/证据；最终远端HEAD在PR和交付复读记录中给出。

停止点：本轮交付并保持Draft，不自动执行下一阶段。后续获得指示后先处理正常加载/呈现稳定性，再补同区域PC参考、美术细化和ARM64性能验收。
