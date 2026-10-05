# v120：拖动影子、林中部队与选中格 — PARTIAL

本轮按用户三项反馈继续 agent/native-pc-visual / Draft PR67。已读取规则包（SHA256 74e794192d71311b84fed6dfbfd8f25c54a1aa334fbd8234d9e36d145640d03c）、全局契约、既有架构和遗留验收。输入为远端252aeea39e4b7bdfb0cbdf4cc6884c5b400a45a7；main仍ac29b458325b52d6e302ca44270d16552de4ed7f，包含已合架构PR66。没有新阶段、Unity重启或main合并。

## 正式修改

1. MainActivity → MapHost → FilamentMapView原有拖放链保留；只增加所选部队ID的展示查询。长按拖动时，把当前已加载兵种模型和UnitFormation编队/地面接触变换投影成半透明轮廓，吸附实际拾取格中心，沿计划最后一段转向。可移动为青绿，无效为红，脚下有暗色落点影子。它是实际模型几何的Canvas战术投影，不是新建纹理化GPU分身。预览不执行规则，不在手势中解码资源；停留同格复用路径缓存。
2. Vegetation.exclusions仅排除城港关和设施，部队不再清空所在格及周边树林。既有密度/随机放置/LOD/道路水域安全留白保留；部队移动不再使森林排除集变化。选中林中部队加淡金模型轮廓，树冠遮挡下仍可辨认，不删树、不另造植被透明材质。
3. 选中格使用6dp深色外沿、3.5dp金色线和1.05dp浅色内线，叠加低透明填色。被地形遮挡的目标继续使用虚线语义；七格城占地去重，避免重复描边。拖动落点使用相同描边体系。

取消、双指、禁用、遮挡、快照替换、会话替换、暂停及释放清理影子；松手继续调用原规则预览重新校验和dropUnit，不改变命令次数。玩法/core、game-api、game-runtime、data、Unity、存档/RNG和300份正式资源完全不变，v119智力规则保留。

## 主机证据

同一正式森林生成器与合法原移动命令：v119原Vegetation源码独立编译，部队8,8→7,8前后所在格植被均0，排除集改变；候选分别保留4、3个放置实例，六个窗口网格逐个复用同一对象。新专项候选559653检查、旧缺陷复现559644检查；全存档/RNG在生成和投影前后相同。

受影响S04/S05资源与命令流、R06资产、R07/R08/R09、R10编队2221136、R02拾取399124、R01/R12、GameSession1673、地图投影119600及架构通过。原有树木密度测试保留全部原断言；在当前排除集下740树/112灌木。原ForestDensityTest日志的旧“unit apron”用语未改，其断言在当前排除集下验证放置/clear，不代表仍清空部队周边。此数量不等于真机美术或性能验收。

正式修改提交8198ed9cbf9f558d67f17e6fca9494ce68134370。最终APK源码931a298e5c06b5fd56ccfcf067defa8dd840d17e，tree a2376d58f2ea7d991f0b85296f90a553d0294975；追加提交仅修测试驱动观察到的Quickstep弹窗。最终CI36548264657，首轮36546440805。

## 验收边界

历史全国首次ready、完整全触控开局、V1–V4/PC参考、旧档SAF、ARM64/Adreno/Mali和30分钟长稳缺口不因本轮主机通过而关闭。当前参考仍REFERENCE_MISSING。软件模拟器数据不能替代物理手机。没有受命自动进入下一阶段。

## 首轮构建与身份（保留失败轮次来源）

CI36546440805/build109333857906通过，独立制品11023290455（有效期2026-10-29），源8198ed9cbf9f558d67f17e6fca9494ce68134370。assembleDebug、assembleDebugAndroidTest、lintDebug真实执行，BUILD SUCCESSFUL，84项任务。APK为game.sanguo.mobile.dev，code120 / 0.120.0-native-selection-forest，37,403,422字节，SHA256 48be49bf3cbed1c29cf3b411673f143539fed354303318e54ee27f694a5388f0。四ABI与原开发签名8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24保留；本地再次复核DEX源码、300资源字节、归档及16KB对齐。

原完整core在输入252aeea与候选分别执行，都在CoreTest.logistics:75的AI uses deployment commands失败，原始日志逐字相同，归类inherited。工作流continue-on-error不能作为完整core通过。

Firebase job109335415545在google-github-actions/auth阶段失败：unauthorized_client / credential rejected by attribute condition。只执行到访问认证，预检未到达、物理设备提交0；没有改动IAM或绕过访问条件。

## 最终构建与安装方式

最终CI36548264657/build109339816709通过，84项任务、1m50s。最终build artifact11024050760（2026-10-29到期），ZIP SHA256 ee217543df28ddb3442e3d0754a78a6c0c83e56437278057c3569b49abcc3e92。独立最终APK源码931a298e5c06b5fd56ccfcf067defa8dd840d17e，37,403,422字节，SHA256 **172a482f76d4dc7300403d692a9085c0545b82784076ae8ca17f479d6a78b30c**。版本/applicationId/签名/四ABI与上表相同；本地在仓库根目录复核源码DEX、300份asset哈希、无Unity及16KB对齐通过。

真实构建命令：

```sh
./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PcandidateSource=931a298e5c06b5fd56ccfcf067defa8dd840d17e -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true
```

设备驱动是scripts/verify-feedback120.sh：adb install -r独立APK和test.apk；pm path后拉回installed.apk，cmp与SHA256精确匹配；pm clear后am instrument -w -e phase baseline/candidate game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeFeedback120Instrumentation。正常启动game.sanguo.mobile.MainActivity，明确的合法存档fixture，程序选择己方部队与切换原生3D、设置镜头；长按/移动/抬手使用实际指针事件，不是完整手指新开局流程。ready原120秒，外层900秒仅防进程挂住，不放宽ready。录屏最多180秒，截图来自UI和PixelCopy实际Surface。

## 已取得的首轮运行证据

- API29：旧v119 PASS99，真实拖动移动一次，重复UP不二次执行，独立原命令expected/actual完整sg11逐字节一致。实际照片确认旧版部队所在森林空缺随移动改变。新版初始120秒ready FAIL（submitted3 / pending5 / beginSkipped572），实际UI可见增强选择边框，但3D森林未装完，落点影子/取消分支/移动后林格与相机图组NOT_REACHED。此旧PASS/新FAIL差异维持回归风险OPEN，不能认定纯环境。
- API35：首轮旧/新均120秒ready FAIL，submitted0；两张失败原图都显示系统Quickstep无响应弹窗挡住游戏。这是有直接画面证据的环境阻挡，不能据此认定候选运行通过。
- 复测仅将UiAutomation连接移到启动前；ready观察器匹配包android、精确文本Quickstep isn't responding，保存原截图，最多两次通过真实触点点Close app。不会关闭游戏自己的ANR，不绕过窗口焦点/帧门控，不改原时间或断言。首轮原证据全部保留。
- 四段首轮录像ffprobe可解析，持续126.15–176.25秒；只证明文件可解析，不代表每段覆盖完整测试。失败图不是美术通过；没有离线图冒充APK。



## 最终运行与画面复核

| 环境 | 原v119 | 交付v120 | 结论 |
|---|---|---|---|
| API29 x86_64 / SwANGLE | PASS106 | PASS134 | 真实指针拖动、六种取消/无效路径、松手一次、重复UP、全存档/RNG和相机不移通过；部队移动后原生林块同对象和generation保持，目的地植被仍在 |
| API35 x86_64 / SwANGLE | 120s ready FAIL，submitted3/pending6 | 120s ready FAIL，submitted3/pending6 | 后续拖动/森林检查NOT_RUN；本轮无Quickstep弹窗，说明首轮弹窗并不是全部失败的解释 |
| ARM64真机 | NOT_RUN | NOT_RUN | WIF attribute condition拒绝，预检未达、物理提交0 |

API29 job109341431505 PASS，artifact11024380977（ZIP SHA256 51815302cab8fc09446f38717916357e65d83e18027e5c01c1d9c4f7b03b61b7）。API35 job109341431430 FAIL，artifact11023464556（ZIP SHA256 b26b2931733803d3b93e14476846947023f458cf45bf3f8bc4e58b4434ad5143）。两API安装拉回SHA与各自交付APK相同；API29原/候选各一对完整expected/actual sg11本地逐字节再次相等。最终CI整体failure，不能称全部CI绿。

最终API29每版6组UI/Surface（进入林地、移动后、span4/8×yaw0/90），另有拖动预览，候选还有无效点预览。人工查看候选入林前后、四相机及独立Surface，确认树木留在部队所在格，选中轮廓透过树冠可辨，金色三层边框增强；基线原图则部队周边为空。

**预览屏幕同步验收仍FAIL，不能由134检查推出美术/跟手PASS。** 原始candidate-drag-preview.png实际还是红色起点，candidate-invalid-ghost.png实际为先前青绿有效落点，捕获到的屏幕状态滞后于驱动命名和内部目标。两种真实模型影子均可见，但截图不证明正确时刻贴随手指；选中部队标签也遮盖部分影子。文件按原样保留，未改名伪装。内部目标、投影路径绘制计数和最终drop事务通过，与屏幕及时呈现分别记账。既有Window backing/低频呈现稳定性没有因此关闭。

模拟器运行较慢，最终candidate owner begin wall p95约1983.891ms，presented_interval/GPU duration均NOT_AVAILABLE；这不是手机帧时。meminfo是在instrumentation结束后采到No process found，不能记成零内存。CPU森林网格/R08估算仍121,352,376字节，不是PSS/GPU内存。录像180秒上限覆盖不了全作业，视频索引只校验容器可解析。

## 交付和未完成

独立universal APK与原始证据ZIP由本会话附件交付；仓库不提交APK、SDK、密钥或巨量截图。[PR67](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/67)保持Draft/open，源码SHA与证据HEAD分开，最终远端HEAD写入PR正文及交付FINAL_READBACK.json；该HEAD相对APK源码仅docs/native-pc-visual差异，并经git/API复读。

首轮失败、最终有限功能PASS、预览同步FAIL同时成立。后续首先解决ready/呈现稳定性与影子标签遮挡/跟手证据，再完成全国/全触控/SAF及PC/ARM64美术性能验收。本轮不自动开始下一阶段，不合main，不重启Unity。
