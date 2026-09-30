# v124 Blender 模型与原生3D性能续作 — PARTIAL

输入远端 `13fa313e39a9bc9ff157701b721d79c62144ce1e`；起止复读 main 为 `ac29b458325b52d6e302ca44270d16552de4ed7f`（最终交付前复读见远端核验），架构PR66已合入。继续同一 `agent/native-pc-visual` 和 Draft PR67。不合main、不重启Unity、不改玩法/地图、不自动开始下阶段。

## 实际改动

Blender4.2.3 LTS实际导出49个版本化GLB：城池三规模、港口、关卡各3LOD（15）；13类兵种/水军/运输各2LOD（26）；阔叶树、山地树、灌木和层岩各2LOD（8）。增加城院廊亭、砌石门拱、木码头梯子、甲裙/盾饰/马具/船甲板等细节，保留轴向、关节父链、入口和既有动画。使用原项目CC0图集及原创几何，完整Blender工程和离线预览在首轮artifact11064172889。49模型合计59,722三角形、7,723,708字节，旧286个3D源码资源逐字节保留；完整APK资源441项匹配。

正常 `MainActivity -> MapHost -> FilamentMapView` 加载 `sites/v124`、`field/v124` 和 `rigs-v124.json`。不是离线图或样例Activity。地形装饰使用原连续高度场及排除区，摆放SEED/PLACEMENT_VERSION、密度、道路/入口/城池七格不改。阔叶树LOD0顶点221→50，岩层LOD0三角形178→82；不是减少地图或军队。

植被分块改primitive增长数组，消除Float/Integer逐元素装箱而保持逐顶点/索引/UV数据与顺序。单位制作法线通过关节切线帧旋转，静止部位直接复制；rig/clip只在读取时校验并解析primitive数据，逐姿态不再访问JSON。解码重试不重复settle既有单位或失效接地缓存。原Filament1.56.0 OPENGL、主Looper/Choreographer、frame gate/backpressure、质量/帧预算全部保留。

## 三条验收线

- 实现/玩法：生产接入、严格加载/动画几何/完整存档/RNG、架构隔离、构建及包身份 PASS。实际运行/触控结果见下表，不能把主机通过扩展为全运行通过。
- 视觉：模型制作和资源接入 PASS；PC同区域/近似镜头参考 `REFERENCE_MISSING`，完整美术 `NOT_RUN`。Blender图只作建模证据，不能冒充APK；最终APK截图按各实际结果留证。
- 设备：模拟器API29/API35 swangle与ARM64真机分列；手机FPS、Adreno/Mali、30分钟真机 `NOT_RUN`。Firebase WIF attribute condition拒绝时物理提交0，不更改IAM。

## 精确源码与构建

APK source `6f6ee9772279c87c4e3ff1228ff7eda726793b18`。SHA256 `edf9f85b94095ee887b07eb6260a2f0f76ba15b722aaf54b6a883e7aee20c3bc`，41,109,988字节；应用ID `game.sanguo.mobile.dev`，versionCode124 / `0.124.0-native-landscape-models`；ABI arm64-v8a / armeabi-v7a / x86 / x86_64。开发签名SHA256 `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`。4ABI/16KB核验、441资源逐字节核验、源码/入口/DEX身份 PASS；本地再次独立核验 PASS。

CI https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36639733339，build artifact11066456381；Blender生成artifact11066215768。按clean完整源码SHA构建，报告/证据后续提交仅docs，最终HEAD在交付manifest/PR和最终回复中复读。

```sh
./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PcandidateSource=6f6ee9772279c87c4e3ff1228ff7eda726793b18 -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true
```

真实安装 `adb install -r APK`、`adb install -r test.apk`；`pm path`拉回APK后cmp/SHA核验。正常MainActivity从auto.sg11恢复测试fixture或真实官方剧本，MapHost切换3D，MEDIUM，原120秒ready、四个固定span5/10与grid开关、至少三个新提交、Surface/屏幕/视频。局部另注入实际滚动与设备最小跨度之上的双指缩放，保留原相机断言。固定镜头仍由API设置，不是完整纯触控新开局链。

## 主机测量（不得写为手机FPS）

同一v124资产，独立编译输入13fa原算法，10预热/7采样，原始CSV完整保存。最终CI中位数如下，CPU仅主机线程CPU计时：

| 操作 | 输入分配B | 候选分配B | 输入CPUms | 候选CPUms |
|---|---:|---:|---:|---:|
| 一次植被视窗构建 | 16068616 | 7551624 | 6.052 | 3.672 |
| 100次骑兵walk姿态 | 18962672 | 14080000 | 9.164 | 2.150 |

植被/姿态分配分别约-53.0%/-25.7%。CPU有平台/JIT波动，保存局部+1.6%、初版+59%等不利样本及最终CI，不能只挑有利数据。原R08 96x96森林fixture相同44块/摆放，CPU网格流估算121,352,376→40,302,160B（约-66.8%）；仍只是host fixture，非全国手机常驻/GPU内存或帧率。

## 测试与失败分类

本轮host5,045,494检查：双staggered/LOD逐字节几何、13兵种×2LOD×8动作×12帧、制作法线的0/90/180/任意旋转、缓存复用、全存档/RNG和五种坏schema拒绝 PASS。原受影响主机测试、架构/GameSession投影隔离 PASS；309GLB Khronos零错误/警告。

原CoreTest.logistics:75“AI uses deployment commands”，输入/候选均exit1：inherited。原R09 canonical hash `ac0848b52965d6cd118582891bb65b16fe18caa3eb5f39126ba24302b4da2668` 双边相同且原断言exit1：inherited。R10双边exit0。原断言/预期哈希/失败退出码不改；已知失败单步保留不阻止打包，整体CI不得写成全绿。

本轮新问题：Blender附加quad的关节计数/primitive首稿/不合法host fixture已修并保留首错日志；首次test runner Manifest遗漏导致六组NOT_REACHED，已注册并增加APK/安装预检；制作法线首次姿态26.2ms及后版JSON姿态15.4ms回退已据此修改并保留原CSV；f6a30d使用host JSONObject.keySet导致Android编译FAIL，改keys并经官方Android35 API编译；最初双指跨度80..188px低于平台阈值，输入/候选均缩放FAIL，按实际ViewConfiguration阈值修正，断言未删。最终运行是否通过须见实际记录，不能把修正代码当运行PASS。

## 最终运行结果

| API / 场景 | 输入v123 | 最终候选v124 | 实际证据 |
|---|---|---|---|
| 29 / 局部 | FAIL scroll | FAIL scroll | 双边四个Surface+UI视角已到达；原滚动断言均FAIL，缩放/静态缓存/最终存档NOT_RUN |
| 29 / 寿春 | FAIL ready | FAIL ready | 原120s未就绪；渲染/输入/全存档下游NOT_RUN |
| 29 / 港口 | FAIL ready | PASS 120 checks | 候选四视角真实Surface/网格、无回退、正常GPU港口模型、183,198B完整存档/RNG一致；此视图无部队，单位位置缓存NOT_APPLICABLE |
| 35 / 局部 | FAIL ready | FAIL ready | 原120s未就绪；下游NOT_RUN |
| 35 / 寿春 | FAIL ready | FAIL ready | 原120s未就绪；下游NOT_RUN |
| 35 / 港口 | FAIL ready | FAIL ready | 原120s未就绪；下游NOT_RUN |

12次真实安装都拉回并验证字节SHA，候选全部匹配本轮APK，输入全部匹配v123原包。候选API29港口10,782ms静态窗口：submitted6 / attempts27；是提交计数，绝不是手机FPS或呈现帧率。完整存档SHA256 `10f2fc98f13202b2b2356c97277b3142b4def7f77f239faa05b4cf6f6399543c`。

所有6个matrix job FAIL（其中港口pair因输入失败而job FAIL）；候选仅港口完整CASE PASS，其余候选FAIL。物理授权FAIL、提交0；整体CI FAIL。局部滚动与多区域装载仍未验收；原缩放阈值调整代码不等于最终触控PASS。两版同点失败属于观测事实，尚不能区分触控遮挡/事件时序/继承交互缺陷，不假定根因已修。

最终APK实拍能确认关卡、部队、码头和新树木进入生产画面；低分辨率/重复树冠图集、山体折面和岸边条带仍需美术修整。缺少PC同区域参考，整体高精美术不能宣称完成。

前一版cca1dde的API29小场景四视角有真实Surface模型，随后原不足跨度的缩放FAIL；其他多场景GPU beginFrame拒绝并停在装载。API29旧港口PASS、新港口ready FAIL，风险OPEN；API35/寿春旧新ready失败。此历史图不作为最终APK通过证据，全部留存。

## 仍开放

正常3D间歇ready/Surface提交、PC美术、ARM64真机/30分钟、完整新开局纯触控/SAF旧档/20次生命周期、原core/R09账本不关闭。原包权威地图寿春湿地缺失、岸线/烟火/山地和未贴山关隘等美术历史缺口仍按既有账本。最优先后续阻塞是装载/真实Surface稳定性及可用ARM64设备授权；没有自动启动下一阶段或合main。
