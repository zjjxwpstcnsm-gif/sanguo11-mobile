# R00–R14 原始要求独立复验


## 2026-09-26 本轮独立复验：原v112，不是新生产版本

**R00–R14是否全部满足原始开发要求：否。**

463条：PASS9 / FAIL20 / PARTIAL174 / NOT_RUN256 / NOT_REACHED4。原213原文和历史不动，补24漏联规范段与4精确验收澄清。以 `evidence/repeat-20260926/` 为本轮结论；旧full-acceptance是702feccf历史。

API29/API35全国预览与原NativeR12失败；API29月7/月4整屏仍1月，UI draw95/Window89冻结而3D变化。生命周期本轮为API29 0/0、API35 20/1后后台停帧断言失败；历史20/18不是20+20通过。两API新parity各12组合、50对完整存档字节一致，但只是fixture。纯触控尾链NOT_REACHED，ARM64 NOT_RUN。原core两边同一logistics:75失败且42调用矩阵相同；20HOST门禁不能替代core/设备/美术。

APK source `5f8997ebfef15f4680931d40532411a336506d0d` / SHA256 `6ba2211d961567b2aa396ba19e3018c1dc44780fa4351a807db099fc643bd058`。生产、规则、资产不变，保留原APK与开发签名；未开始R15或合main。

## 阶段结果

| 阶段 | 结论 | 具体差距 |
|---|---|---|
| R00 | FAIL | 两API独立正常冷启动全国预览FAIL；S01–S13逐项当前质量/完整环境门槛未闭合，不能把源码存在当完成。 |
| R01 | FAIL | 本轮API29初始ready失败，切换0/前后台0；API35完成切换20/前后台1后background frame loop stopped断言失败。owner仍同步读材质、atlas和rig；晚到worker/故障恢复后半段NOT_REACHED。 |
| R02 | PARTIAL | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R03 | PARTIAL | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R04 | PARTIAL | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R05 | FAIL | 正常游戏Surface实际确认逐格阶梯岸线；几何仍是格子矩形并集。弯道上下船、增删水后实际航行与PC水岸完整对照不足。 |
| R06 | FAIL | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R07 | PARTIAL | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R08 | PARTIAL | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R09 | PARTIAL | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R10 | PARTIAL | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R11 | PARTIAL | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R12 | FAIL | 原NativeR12两API均初始ready失败。独立全国预览FAIL。API29月7/月4权威与widget正确，原整屏仍190年1月上旬；UI draw95/Window frames89冻结而3D颜色改变。完整纯触控尾链NOT_REACHED。 |
| R13 | PARTIAL | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R14 | PARTIAL | 月份到季节正式接入；API29三月份fixture到达且Surface颜色变化，但UI日期冻结。四季×近中远×三画质、阴影/雾/过绘、长稳和真机仍未闭合。 |

## 执行和证据边界

本轮实际重跑严格安装CI36222182595 attempt2、原CI36213760665 host attempt2及API35冷启动attempt3、兼容parity CI36222424271 API29 attempt2/API35 attempt3。必须按job started/completed时间识别实际重跑，不能把新attempt中的克隆旧job算新执行。原job108325858061最终失败，旧20/18日志保留。

输入8b42af2a70c0fc066620e2ce79951aa31d8b923e与候选5f8997ebfef15f4680931d40532411a336506d0d在本地重新执行原完整core及全部42调用，均12 exit0/30 exit1，退出矩阵相同。没有新增core失败证据，没有删除断言或改AI冲绿。CI另有20HOST门禁通过。

两API冷启动均在真实触点全国预览内实际执行首CPU/farLOD检查；API29 CPU177地形/211环境已交付，pending285/提交8；API35有效重试pending324/提交2。API35前一次adb root连接关闭发生在安装前，单列基础设施失败。原NativeR12两边均初始ready失败，原120s和原断言未改。

API29月份fixture的208检查PASS仅覆盖内存/轮询。原完整PNG、独立Surface及录像显示顶部仍190年1月上旬，而authority/snapshot/widget为7月/4月，季节颜色已变。UI draw95及Window frames89冻结；Window PixelCopy无backing surface。控件实例ID/bounds及真实手动读档/跨月跨年全链仍缺，不能声称根因全部定位。

本轮两APIparity分别341/255检查，各12组合，50对完整sg11字节逐一相同，另核对9组跨模式同检查点哈希。它是fixture/API命令，不覆盖所有真实事件和完整纯触控。触控链在预览失败后确认开局、选城出征、移动攻击、战报、下一旬及手动存读档NOT_REACHED；当前cold探针未实现全部尾链。

## 源码、视觉、性能分开

SceneWorkQueue/SceneAssetQueue有界、epoch取消和owner交接确实进入正式路径；doFrame在beginFrame前排空CPU结果，但GPU上传需beginFrame准入。overlayDraws不能证明3D已呈现，WAITING_FRAME也不等于绝对没像素。构造器仍同步读材质/解码atlas和rig，因此R01.I03/R06.I05继续FAIL。保护门控不能为了冲绿删掉。

300个正式assets与APK逐字节相符；本轮保护校验通过，复制manifest破坏hash的负测试实际exit1。core/game-api/game-runtime/data/unity等保护生产树不变，APK四ABI无Unity Player，开发证书不变。源码位置及hash见source-paths，正常调用链和每条必要条件见matrix。

独立查看PC手册07原参考、当前整屏及Surface：阶梯斜岸/方形水块与WaterVisualField矩形并集一致；V1/V2无完整同区域相机图组，城港关全LOD、各兵种动作、四季三画质仍缺。没有拿离线图/概念图/测试fit替代实际画面。原R10/R11/R13缺口不自动关闭。

设备是API29/35 x86_64 Pixel2 1080×1920，Filament OPENGL经ANGLE/SwiftShader软件后端，不是ARM64/Adreno/Mali。墙钟、线程CPU、队列等待/背压、pending、beginFrame尝试/拒绝、提交与Surface分别记录；完整分配/GC profile、物理GPU时间/PSS/热稳定NOT_RUN。No process found不记零内存。

原视频逐段ffprobe解析、ffmpeg -xerror完整解码和时间采样内容检查；视频可播放与功能/美术/性能通过分开，抽样不冒称逐帧全流程验证。历史坏片保留，不能由新可播放片段宣布修复。

## 下一步顺序

先沿ScenarioFactionPicker→MapHost→FilamentMapView.doFrame/CPU mailbox→beginFrame→loadVisible/syncObjects→Surface修全国预览，保留本轮原120s失败基线；不隐藏对象、不缩图、不换引擎。并行定位API29 Window backing/UI draw冻结；补控件身份/焦点/布局和整屏+Surface同步证据，不仅getText。随后核查HOME实际焦点/Activity门控/queued时序并修20+20生命周期，再跑晚到worker与异常资源。通过前置后执行完整纯触控尾链，再处理owner解码、R05岸线/V1/V2、R10/R11全类别、R13 SAF及编辑矩阵。ARM64和真性能单列。

原始输入六份ZIP/JSON及嵌套包完整CRC/大小/hash校验；原213及旧435原文和历史保留。463行是规范段/复合条目，不是463个互不重叠功能。完整matrix/coverage/未完成列表/manifest与原始证据包一起交付，最终远端HEAD另行复读。Release按本轮实际stdout/stderr，不把GET成功当可写或伪造链接。pm clear仅用于隔离AVD，禁止清用户真实设备存档。
