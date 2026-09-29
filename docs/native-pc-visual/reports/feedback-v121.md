# v121 山地、网格、Blender 建模反馈 — PARTIAL

输入远端 agent/native-pc-visual 为 afac21886f8b092ed3099aa1bf88eba966955285；最新 main 为 ac29b458325b52d6e302ca44270d16552de4ed7f，已含架构 PR66。读取用户规则包与历史验收，继续同一分支和 Draft PR67。不进入新阶段，不启动 Unity，不合并 main。

## 正式实现

- TerrainSurface 保持共享连续场、原逻辑地形、城池完整平基、低道路与水面。取消道路/关隘过宽的山体压低范围，提高山脊目标；坡脚使用导数上限 1.35 的平滑过渡，避免第一版陡坡造成默认镜头格心遮挡。最高上限仍 2.6。所有分块、拾取、地表贴附消费同一场。
- SiteVisual 在原关隘朝向与真实相邻山地之间生成贴地护坡墙，所有新增顶点只能落在原关格/山格。10 关中6关可连接；武关、剑阁、葭萌关、绵竹关六邻域没有山地，不能通过视觉假山封住原可通行格，原数据不改。不是全部关隘瓶颈验收通过。
- 普通网格由 worker 生成贴合实际三角面的静态 GPU 分块线，按地形生命周期/视锥显示，共用原每帧8次/2ms上传预算、复用GPU对象并在淘汰/释放时销毁。去除普通网格 Canvas 全格扫描和遮挡射线；编辑器网格、战术选择/路径保持原语义。新增内存/绘制成本由资源报告计入，不能仅据此宣称手机流畅。
- 实际运行免费开源 Blender 4.2.3 LTS（0e22e4fcea03）创建弯曲火舌、炭块/火星、烟团、层理岩石，并给原关模型做焊接和小倒角。导出7份正式GLB，原300资源保持字节不变。原生资产异步队列加载，火焰使用既有顶点颜色不透明材质及轻微伸缩，未加入透明体积烟火/流体模拟。保留64粒子/16处火预算，不改火计规则、持续回合或RNG。不是全模型已达到写实成品。

MainActivity → MapHost → FilamentMapView，Filament1.56.0 / OpenGL ES、GameSession唯一权威和模块方向保留。core/game-api/game-runtime/data/unity没有修改。离线 Blender 场景只能证明制作过程，不能替代 APK 实拍。

## 参考与验证边界

参考4Gamer转载光荣官方 PC 发布画面（2006-01-27）：虎牢关墙与连续山坡相接，成都周边为连续高山脊。已实际查看800×600原图，来源记录在REFERENCE_INDEX.csv；镜头参数未知，不声称精确同镜头或与原版一致，版权图不进入运行资源。

新增专项验证资源预算、允许格网格/地面一致、复用、真实关隘连接与完整存档/RNG不变。原TerrainSurfaceTest的格心拾取断言完整保留；第一版的失败保留在本轮证据，收缓坡脚后再验。原NativePreviewWorkTest冻住旧山体及岩石几何字节，其原哈希不改：在候选上保留失败，并独立运行输入控制，不把预期美术变更改写为旧几何字节PASS。原完整core在输入/候选均CoreTest.logistics:75失败。

APK、安装、实际Surface与整屏、软件模拟器对照和物理访问结果均已回收，详见下文。不得把构建/主机通过等同V1–V4、完整触控、全部模型、ARM64/Adreno/Mali或30分钟验收。历史首次ready、拖动影子迟到与完整旧档SAF等问题保持开放。

## 首轮构建与身份（中间包，后被编辑器高度修复取代）

首轮源码 **db50c897fdbc698778c5c034a5616068fb22a3fb**，tree df2f0e62df3d31f0841ae9f3ca6c6eee1e431e50。[CI36565246134](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36565246134) build109395463859通过，84项任务实际执行，1m48s。普通git push因无终端凭据失败；经已授权GitHub连接器逐blob核验SHA、创建完全相同tree并快进同一分支，未force/reset任何远端成果。PR67已更新。

独立APK：game.sanguo.mobile.dev / versionCode121 / 0.121.0-native-relief-grid-fire，37,551,483 bytes，SHA256 **f65f784ee8806d0ddf2caad4497ab575928eb4a3e916fa33e7d1808b0e3d96b8**。arm64-v8a/armeabi-v7a/x86/x86_64；原开发证书SHA256 8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24。16KB ELF/ZIP、DEX源码身份、全部307项资源字节在CI和本地复核；四ABI归档不等于物理设备验证。制品11031966490，ZIP SHA256 85acb8c2f441f016d92eef38bc520c0addec6c2b469e659a0b2d69c5d469adc5，2026-10-29到期。

构建命令：
```
./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PcandidateSource=db50c897fdbc698778c5c034a5616068fb22a3fb -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true
```

首轮主机：新专项72731检查；旧窄路旁山高0.16916323，候选0.69363636，道路均0.08；9331山格平均高度0.40149771→0.85247576（地图数据不变，不是像素美术分）。原格心/地形902230、网格海岸2158535、材质4978894、R02 399124/R05 55818、水域803860、R07 7194/R08 24333965、R10 2221136、战斗25470、R01/R12/架构通过。Khronos178 GLB零错误/警告，全部新资源还经过实际正式subset loader。

完整core输入/候选.exit均1，同一logistics:75；原R09脚本候选.exit1/输入0，失败为NativePreviewWorkTest固定旧position/index/UV/normal/shore/flow/macro哈希，新候选5d399ea4…与旧2c0db7e3…不同。此项归类NEW/有意视觉数据变更，不能写整体回归PASS；旧断言与脚本未改，原脚本后半段未到达。R10独立脚本双方退出0。continue-on-error只让APK证据构建继续，绝不把失败变PASS。

物理访问job109397468713：google-github-actions/auth unauthorized_client / credential rejected by attribute condition；只读库存预检未到达，物理提交0。没有改IAM或绕过条件；ARM64/Adreno/Mali/30分钟性能 NOT_RUN。

补充：保留完整脚本失败后，单独编译并运行未修改的 NativeR09Test，候选 PASS1602（实际洛阳区域地形/设施/道路断言）。不将此结果覆盖原R09脚本退出1。

## API35 实际安装结果

job109397468689 FAIL，artifact11032905487，74,213,018 bytes，ZIP SHA256 ca35d8ec044762f646f20d332cf530629cdffdee25fe8c65749c5779e733b5a9。安装命令为 `adb install -r`（先原v120再候选v121），每次拉回pm path的base.apk并cmp；候选安装字节本地再次等于独立交付APK。正常 `game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity` 自动存档恢复，fixture为实际火计后的合法小图，全国为184黄巾首剧本虎牢关。相机由API控制，不算完整触控链。

| API35场景 | 原120秒结果 | 提交帧 / 待上传 | 后续 |
|---|---|---|---|
| v120 fixture | FAIL ready | 4 / 6 | 网格/镜头/计时/全存档比较 NOT_REACHED |
| v120 全国虎牢关 | FAIL ready | 6 / 23 | 同上 |
| v121 fixture | FAIL ready | 3 / 6 | 同上；gridUploads=0 |
| v121 全国虎牢关 | FAIL ready | 6 / 24 | 同上；gridUploads=0 |

实际整屏已逐张查看：旧/新地表仍显示“3D地形装载中”，不完整的山地轮廓存在差别，但关隘/火焰/森林模型未完整呈现，不能据此验收贴山关隘和新火焰。没有取得该API的独立Surface对照或网格性能样本，不用CPU资产已解码代替已上屏。4段MP4可解析（130.91/135.08/132.50/132.92秒）；没有放宽ready或用离线Blender图替代APK。源码相同的旧验收工作流也有模拟器运行失败，不能声称所有CI通过。

## 编辑器高度保护修复（最终源码取代首轮包）

追加复核发现首轮自动坡脚 cap 会将平原显式2.4高度压成0.3，归类NEW/本轮回归。已在 b2352ed199fddada917ea224295773c4f9250506 修复：分开自动坡脚上限与水/道路/完整地基硬约束，手绘高度只覆盖自动造型，原硬约束继续生效。新增3项断言，专项72734通过；无手绘覆盖的全国和测试场景高度数据保持首轮结果。此前提供的APK是中间测试包，最终以本节后续的源码/哈希为准。

原首轮完整12镜头对照仍保留执行和失败证据；本轮追加 focused 参数提供单镜头(span6/yaw0)的网格开关、火焰加载与完整存档检查，保持原120秒ready、三次新帧、全部功能断言和20步镜头计时，明确不是完整缩放/方位矩阵。默认不传focused仍运行原完整矩阵，不以定向结果覆盖首轮失败。两个CI输入隔离，旧任务未取消或覆写。

## 最终构建（应下载此包）

最终APK源码 **b2352ed199fddada917ea224295773c4f9250506**，tree 14f2adb5e741083ac8e99aa7b66ed1e7dfe39c79；[CI36568523262](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36568523262) build109406387912通过，84项任务、1m20s。原appId/121版本/开发签名/四ABI保持；37,551,483 bytes；最终APK SHA256 **06ebe774afffb7f3022ec9ae6a6d2f191fdb6dbff814935e2e001aa5fbbaf4d1**。全部307资源与源码DEX在本地复核通过，16KB归档/ELF通过。最终制品11032963832，ZIP SHA256 dbc8faeda7571404a57dc23d80a59d8c243146d6f7425928a2126fba4fda4e2c，2026-10-29到期。构建命令与首轮相同，candidateSource替换为本节最终SHA。

最终全国完整几何/材质字段hash仍为5d399ea4054e4e3792103fc73b4d709a7e40037c0d3a72d1f196c4777e56c433，与首轮相同；新增修复只改变受手绘高度覆盖的造型约束，正式7模型字节不变。完整core和原冻结hash失败继续保留。再次物理访问job109408154773仍在WIF attribute condition失败，库存未到达、提交0。

## 最终 API35 定向运行

job109408154585 FAIL，artifact11033447625，74,653,734 bytes，ZIP SHA256 d8e7d5ab2276a5088e06e72cdd1adef2ac2af18c1821e7f9761c452f0d6c90de。旧fixture submitted5/pending6、旧全国6/23；最终候选fixture5/5、全国9/21，四例仍在原120秒ready失败。候选均gridUploads0，镜头/网格采样、火焰正式mesh断言、完整存档前后比较NOT_REACHED。安装拉回字节再次与最终06ebe7…APK完全相同。

已查看实际整屏：候选山脊/岩石出现，局部关隘出现黑色未完整呈现的轮廓，全国关模型有贴图但整体仍在装载。基线相同阶段关模型尚未呈现，不能证明黑色轮廓是旧问题或确认已经修复；列为视觉风险OPEN。不能把CPU加载、资源hash或未完成场景的局部几何当最终美术验收。此API没有完成网格性能对照，也未看到可验收的新火焰完整画面。原始PNG/MP4/logcat不覆盖。

## 首轮 API29 完整镜头结果（源码db50c8，中间包）

job109397468467 FAIL，artifact11033443166，147,925,250 bytes，ZIP SHA256 950cb086924463c35422aa2085f17f397935634587398114d7a82fce5677b4cb。旧fixture PASS171、新fixture PASS151，每版12组UI/独立Surface，原完整镜头矩阵实际执行；两对expected/observed完整存档字节相同，且新旧fixture输入存档也逐字节相同。新GPU网格实际累计6批上传/末态2驻留批次，新1362三角面火焰经正常effect renderer加载检查通过、资产回退0。

已直接查看span4/yaw0实际Surface：候选山道两侧山坡更高，护坡墙伸入相邻山格；原白褐色层叠岩石换为灰蓝层理岩石；弯曲火舌/烟团可见；打开网格后实际GPU地表线出现并在山坡前遮挡。关模型在此已就绪场景正常贴图，不是此前API35装载中的黑轮廓。火与烟仍偏实体/卡通，岩石在陡坡贴合时显得尖锐，整体写实目标未完成；不能称PC美术一致。跨视角/远景最终美术需要继续独立验收。

资源代价：同局部结尾可见三角形39938→53143，mesh缓冲估计2603076→3723294 bytes，primitive20→22；这是模型与GPU网格合计变化，不是实测GPU内存。网格ON的owner-wall p95旧1915.568ms/新1912.560ms，beginFrame等待占主导；OFF分别1902.422/1859.661ms。样本各146/156/171/159，计时为owner回调，**不包含完整Canvas绘制耗时，也不是显示帧率/GPU时长**，无法据此给出流畅度改善比例。真机仍未测。

全国：旧版已经取得11/12组镜头图（23张含临时Surface），随后外部900秒总预算超时，进程exit124、未取得最终权威比较/计时；不是完整全国PASS。候选原120秒ready失败submitted12/pending11，后续NOT_REACHED；本轮全国新旧差异仍为OPEN回归风险，不能直接归成纯环境问题。未放宽任何ready断言，记录所有部分图像和失败。

## 最终 API29 定向结果及最终结论

job109408154686 FAIL，artifact11033747616，86,748,842 bytes，ZIP SHA256 811cd6265f84f1cb1e18e7b7f0b428ddfe01d3d51610c1b15831c2727890202e。旧fixture PASS106、取得单镜头grid OFF/ON两组UI/Surface及完整存档一致；旧全国、最终候选fixture/全国均原120秒ready失败。最终候选gridUploads0，网格/火焰运行断言及前后权威对照NOT_REACHED，安装拉回字节与最终APK相同。

首轮相同无手绘场景的候选PASS151与本轮最终候选FAIL并列保留，**不能以首轮通过代替最终包稳定性**；旧fixture通过而最终候选失败，回归风险OPEN，不能归为已经证实的纯模拟器问题。没有挑选成功轮次、降低门槛或再重跑以换绿。两轮总CI均FAIL，build任务通过不改变运行结果。

整体 **PARTIAL**。已完成正式源码/7模型、原300资源保护、免费Blender实际制作、构建/签名/307资源核验、commit与串行分支推送/PR更新；未完成最终3D稳定运行、网格手机流畅度、全模型写实/PC V1–V4、四处无邻山关隘、完整触控/旧档SAF/历史影子同步与ARM64/30分钟验收。下一次首先解决正常装载/呈现稳定性并验证最终新旧差异，再继续美术与物理设备验收；本轮不自动进入任何下一阶段。

Blender `.blend` 和离线轮廓图保存在本轮交付证据包（本地实际生成），不是APK截图或本次CI构建制品中的离线模型工程；全部实际运行截图在对应API原始ZIP。

最终API29局部失败整屏也已直接查看：只有UI标签和灰色背景，仍显示“3D地形装载中”，没有完整地形/关隘/火焰，不计视觉PASS。16段原始MP4均ffprobe及完整解码退出0；null输出复核出现DTS重复警告，原日志完整保留，不能声称无警告。录屏上限180秒，不能覆盖首轮较长完整镜头测试的全部时段。
