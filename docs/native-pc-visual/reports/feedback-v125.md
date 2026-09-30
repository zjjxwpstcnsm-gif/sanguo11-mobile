# v125 用户反馈续作 — PARTIAL

## 基线与边界

远端 main `ac29b458325b52d6e302ca44270d16552de4ed7f` 已合入架构 PR66；从串行分支输入 `42230940e267b985b6bcd2d0cefec0fae49cca88` 续作，沿用 PR67。不合并 main、不重启 Unity、不启动下一阶段。
保留 Filament 1.56.0 OPENGL、GameSession 唯一权威及原地图/剧本/高度场/城港关；441个旧资产逐字节不变。规则修改仅限用户明确授权的骑兵战法释放和强制单挑、适性菜单显示。

## 正式路径

- 实际 Blender 4.2.3 LTS 制作8个GLB（层状岩台/碎石坡/窄瀑/宽瀑各两级LOD，共1676三角形），新独立景物atlas及精确1.56.0编译材质。FieldAssets → Vegetation原有流式合批 → FilamentMapView景物材质/原帧时钟；不为瀑布另开绘制循环。旧树木/设施atlas不变。模型为本项目原创CC0，非提取PC资源，不能称最终高精美术。
- 更替原山地岩石装饰为岩台/碎石坡，原分布种子、数量和基础地形不变。泰山候选源坐标(147,59)及西南(31,183)仅在既有不可航行水格且具有可贴山安全走廊时放瀑布。主机实际全国近景990/远景258瀑布顶点。庐山候选(119,146)安全走廊不成立，NO_GEOMETRY；不伪造山或河。
- BREAKTHROUGH后方障碍不再否决施放；碰撞检查仍阻止实际位移，伤害/命中/气力/行动结算保持既有公式；CHARGE、ADVANCE原可施放路径保留。新强制单挑判定在伤害结算后独立于位移，接入原Contests/Duel和SaveCodec；玩家交互、AI沿原有限单挑结算，无额外10气力。
- WarUi/ArmyUi适性不足的战法名称显示灰色并禁用。执行层仍核验适性，不可绕过。沿用原ChoiceDialog/InlineChoices，不改目标或射程规则。

## 强制单挑研究及准确性边界

原输入没有骑兵强制单挑入口。此次添加社区记载的基础概率及部队兵力差、性格/健康筛选，三种战法共享概率；TIMID不触发。现有World没有PC战场外持续体力，采用原Duel同一伤势HP；具体宝物增量未建立精确PC对照，不猜测。开场武将按非胆小且有效武力最高选择；该选择和完整PC公式忠实度仍PARTIAL，不将本项目实现声称为原版逐项一致。伤害后目标已灭、异常、位移后不相邻等不满足原存档单挑约束时概率为零。

研究来源（已读取，非PC截图）：
- https://blog.sina.com.cn/s/blog_c0971fbb0102va19.html ：玩家地图景物记录提及下邳西北山瀑、西南瀑布及庐山；泰山/黄果树的具体归属为作者推测。项目坐标是当前地图候选锚点，不能当成PC精确坐标。
- https://www.xycq.org.cn/forum/viewthread.php?authoruid=374759&extra=&page=2&tid=209820 ：San11 Sire社区概率研究。
- https://www.newton.com.tw/wiki/san11%20sire/10760602 ：基础概率和筛选记载的二次整理。
- https://www.9game.cn/news/9061888.html ：骑兵障碍与强制单挑说明。
美术线 REFERENCE_MISSING：未获得同区域/近镜头的PC原版实拍参考。

## 实际架构闭环

安装验收曾发现新强制单挑会话无法从正常ContestUi推进：旧GameSession.legacy忙碌保护返回HOST_BUSY。最终以无World/平台对象的闭合ContestCommand（五种原有单挑/舌战推进操作）接入唯一GameSession，保留session/generation/revision、contest id/revision、线程/回合/生命周期门禁与原普通命令busy保护；候选完整拷贝后单次提交。MainActivity/ContestUi使用该路径，SaveCodec模式、规则公式和兼容白名单不改。此项是有意game-api/game-runtime修改，不能再声称这两模块零差异。33项真实Session测试和安装后原交锋按钮callback/full SaveCodec一致已覆盖。

## 构建与来源

- APK production source：`46361762ccf868c4fce8d55e603639d9e4f23905`；文档/证据提交在它之后，差异仅限docs，最终远端HEAD见证据包remote-confirmation.json和PR67。
- APK SHA-256：`2be377ca7a4b25b1ccfcd13ed93001d18e0fa724a089e221e49ba94f5573d2c2`；41,484,163字节；应用ID `game.sanguo.mobile.dev`，versionCode125，versionName `0.125.0-native-cascades-tactics`。
- arm64-v8a / armeabi-v7a / x86 / x86_64；四库ELF及ZIP16KB对齐PASS。签名证书SHA256 `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`，开发签名，非正式发布验收。
- 后端Filament1.56.0 OPENGL。452正式资产、317GLB，Khronos零error/warning。新增11资源＝8GLB+PNG+ETC2+filamat。旧441资源逐字节一致，data/core资源/unity与输入一致。
- CI [36653575757](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36653575757) 构建PASS，整体运行有FAIL。实际Blender4.2.3模型/PNG及锁版本ETC2编码逐字节复现PASS；512×64 ETC2完整10级mip/21,896B payload，精确1.56 matc材质已核验。
- 构建命令：`./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PcandidateSource=46361762ccf868c4fce8d55e603639d9e4f23905 -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true`。本地主机同源也成功；本地lint0错误/84警告，并非零警告。

## 最终验证

| 子项 | 结果 | 证据及边界 |
| --- | --- | --- |
| 骑兵专项 | PASS | 51,711检查/192真实强制单挑；3战法×8障碍×256seed；命中/伤害/气力/行动、无位移、完整存档/RNG回放、适性原子拒绝、AI有限结算 |
| 景物安全/资源 | PASS | 29,266检查；既有地图高度/通行、全国锚点、近远LOD/cache、ETC2完整性；不等于画面精美 |
| 单挑命令权威 | PASS | 33检查：真实GameSession推进/结算/舌战、SaveCodec、过期/重复/错误命令原子拒绝、普通busy及turn保护 |
| 原相关主机专项 | PASS | S04/S05 7,462,388及真实行军/港口完整状态；架构、投影119,600/GridLayout32；R02/R05/S11地形海岸、S06战斗25,470 |
| 原规则总套件 | FAIL | inherited，输入/候选独立同点失败，详见下文，不将build绿标当作规则全绿 |
| PC同区域近镜头对照 | NOT_RUN | REFERENCE_MISSING；缺少原版实拍，地图博客不是截图验收 |
| ARM64真机/30分钟温升及GPU FPS | NOT_RUN | 无可用物理设备；历史Firebase WIF条件拒绝/提交0，不声称本轮真机跑过 |
| 全国纯触控全链/SAF/生命周期/拖动缩放 | NOT_RUN | 原缺口继续OPEN；fixture兼容测试不替代全链 |

| 安装场景 | 已安装APK字节核验 | 场景结果 | 结论 |
| --- | --- | --- | --- |
| API29 / battle | PASS | 首轮PASS / 未复验 | 2D/3D完整SaveCodec与RNG一致；灰色禁用；单挑推进/存读/结算 |
| API29 / taishan | PASS | 首轮FAIL / 复验FAIL | scene readiness timeout: |
| API29 / southwest | PASS | 首轮FAIL / 复验FAIL | scene readiness timeout: |
| API35 / battle | PASS | 首轮FAIL / 复验PASS | 2D/3D完整SaveCodec与RNG一致；灰色禁用；单挑推进/存读/结算 |
| API35 / taishan | PASS | 首轮FAIL / 复验FAIL | scene readiness timeout: |
| API35 / southwest | PASS | 首轮FAIL / 复验FAIL | scene readiness timeout: |

最终同包首轮6次及仅一次失败项复验5次，共11次已安装APK字节核验PASS。六场景为GitHub x86_64软件渲染模拟器（Pixel2/4GB、swangle），正常MainActivity/MapHost，原120秒SceneInstrumentation.ready不放宽。安装`adb install -r`正式包及测试包，拉取pm path包与交付APK cmp一致；`adb shell am instrument -w -e mode battle/taishan/southwest game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeFeedback125Instrumentation`启动正常组件`game.sanguo.mobile.MainActivity`。全国使用原剧本0/原source锚点、正常3D中画质；12视角矩阵为span3/6/10、yaw0/180、grid关/开，但ready失败的场景未完成该矩阵。原始Surface/UI PNG、screenrecord MP4、logcat/device/meminfo保留，未生成离线图替代实拍。

战斗测试通过正常菜单禁用的实际pointer tap及原ContestUi交锋Button.performClick，规则命令通过真实GameSession；不是全部操作纯触屏。完整SaveCodec（包括规则RNG）逐字节比较在测试内进行。API29/35 checks76/113差异含原Surface探测计数，不表示两API规则不同。实拍可见灰色A级/S级名称与“适性不足”，单挑第1/50合、体力93，fixture3D确有设施/部队。此截图不证明全国瀑布或PC高精美术。

未通过的全国场景具体为WAITING_FRAME、大量beginFrame拒绝和pending景物/地形；新资源初始化无missing atlas/回退，仍不能标运行PASS。相同锚点输入版本未在本轮独立安装对照，回归归因UNKNOWN；b3a935泰山已PASS而最终源码泰山两次FAIL，新增回归风险OPEN，不能直接归为环境或inherited；历史全国ready失败仍OPEN，不把本轮失败自动记成inherited。最终失败截图不足以确认泰山/西南瀑布修正后的完整外观，视觉质量未验收。泰山曾取得12组实拍（b3a935，API29 checks164/API35 checks159），发现白带过曝和穿山裂口；4636176实际Blender重配低亮蓝绿/细泡沫并收窄宽度，在原景物batch内对跨山脊的水面三角形补贴合，29,266项景物检查覆盖；不改变地形/规则。最终实拍及美术结论以本轮对应源码为准。

## 失败账本及修复过程

1. 37aa/03c42、CI36648898492/36649327475：安装脚本每行独立shell导致pm path变量丢失，未执行游戏验收。8015改成单一生产安装脚本调用；没有改测试门槛。第一轮六job原始log保留，第二轮仅保留CI元数据/链接，不假称存在全部原始附件。
2. 8015、CI36649558835：六组安装字节核验PASS；2D战斗子项PASS；新atlas遗漏正常加载器要求ETC2，3D回退（地形探针随后null renderer异常）。f46补正式ETC2和完整mip检查/精确编码复现；探针先检查真实renderer，不降低ready。
3. f46、CI36650573202：材质/atlas正常初始化；四全国场景ready FAIL；两战斗在强制单挑后concede遇到HOST_BUSY，确认真实UI路径阻塞。b3a935以专用权威命令修复推进，新增真正交锋UI callback及Session保护检查，随后b3a935验收两API战斗PASS；旧失败PNG/MP4/logcat未删。
4. b3a935、CI36652119462：两API战斗及泰山12组Surface运行PASS（战斗126/94，泰山164/159），两西南ready FAIL；六组APK核验PASS。人工查看泰山发现瀑布过白及跨山脊三角形遮挡，4636176据真实截图修正配色/宽度/面贴合，新增近远LOD面内部高度检查，未修改任何原地形或ready断言。此前实拍与存档原件完整保留。
5. 最终4636176首轮CI36653575757：API29 battle整体PASS（76），API35 2D/强制单挑子项PASS而3D ready FAIL，四全国场景ready FAIL，六组安装字节核验PASS。为检查同包波动，仅一次rerun_failed_jobs；首轮原始附件/log保留为attempt6-runtime。复验不放宽120秒门槛、未改APK，不将一次复验PASS当稳定性/手机美术通过。首轮/复验结果分别列于上表。
6. 原CoreTest.logistics:75（AI uses deployment commands）、DisplacementTest city deterministic replay:27：输入与最终候选各自非零失败。BREAKTHROUGH旧“障碍不得释放”断言按用户授权更改，因更早旧city replay失败未由原套件到达，新专项实际覆盖；不把未到达记PASS。
7. 受影响原ArmyTest formations43、RulesParity actions54、ContestTest saveAndCorruption135、BattleFeedback lootCaptureAndVacancy20、TechnologyFieldworks migration58，输入/候选五组日志相同/非零失败；附affected-suite-comparison.json与两边原log。未擅修这些旧玩法/旧版本假设问题。
8. 自动触发旧R18固定29261基线零差异保护仍失败；输入分支已146受保护路径差异，本轮又有授权combat/assets及必要窄API变更。原workflow不删/放宽，账本保留其FAIL；并非全仓库CI全绿。

## 验收状态与交接

整体PARTIAL：正式源码、资源、host验证、两API战斗2D子项、同源APK、commit/push及PR更新已交付；全国地形就绪、美术参考/精美程度和ARM64指定验收未通过。强制单挑原PC持续体力/精确宝物项及开场选择对照未做，忠实度仍PARTIAL。

保留唯一串行分支agent/native-pc-visual、同一PR67（Open Draft）、main架构PR66和既有成果，不重启Unity、不合main、不自动开始下一阶段。下一步阻塞为全国正常提交/Surface链、同镜头PC参考、可用ARM64物理设备及历史全链验收。当前不能越过最终发布门槛。
