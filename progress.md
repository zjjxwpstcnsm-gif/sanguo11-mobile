2026-10-02 v156 冬季原版补证：通过正常结束战略三次，实际PC207-10-1冬景已保存，秋/冬几何相机字节完全相同，仅SENV雾字段改变；冬季220绘制批/33managed纹理全部读取，24地表RGBA/状态检查通过。只读观察原Present线程x87控制0x007f（24bit），修正隔离检查精度后原代码74组含两份实际camera雾数据逐位一致，历史72组原模式仍通过；初始末位/density失败留存。1050625原法线与六张RGBA全像素一致，七PNG暂存在out/pc-visual/v157/ground-staging并可重复转换，正式运行绑定待接入。完整战斗/全屏、描边雾等目标继续未完成。

2026-10-02 v156 后续PC实际材质取证：正常重载207年9月新野，同512B相机完全不变；222原绘制批/30张只读managed纹理全部可读，24地表源RGBA及状态检查通过。实际地表明暗=4800—4803第35张RGBA（alpha5—189），描边=4807第0张（alpha165），网格=4804第1张（alpha0—153），此前RGB-only解析器丢失alpha且拒绝4804的失败保留；严格既有WFTX调色板/源mip解码后全RGBA匹配。此证据仅原版运行，不算安卓材质验收；下一步原地表三pass/法线/雾接入，并继续全屏真实事件和其他完整范围。

2026-10-02 v156 原地面UV与144贴图尺寸校准：源41dcb0/41dc00执行2160组及一组实际D3D9流通过，144图原像素/循环边/36×4尺寸表独立核对与五PNG重复转换字节一致；正确x86 matc56重建后离线构建实际安装APK（78629098B SHA96639234422bfb597166bf177d6a850ef6aca3a417dd4256edc6a982d7b700dc），同PC1650×1050正常207秋季PASS47、三地区12季节/远景/暂停释放PASS919、三种正常攻城PASS223，全部自动存档精确恢复168846B原摘要。95秒/594样本对照录像和120秒/1315样本攻城录像已留存，源时序/ARM验收未通过；PC法线24批504顶点原编码也已确认，安卓原材质/法线/雾/岸线/旗帜与完整战斗全屏尚未完成。继续读取实际PC绑定地表明暗纹理。详情docs/pc-visual/validation-v156-working.json。

# 2026-10-02 v155：关隘港口原地区冬季材质（目标未完成）

原41bbb0按城市风格/冬季/地区气候选材质：六种城池冬季+1；关港climate0/2为+1，其余+2。原41c3a0核对全部87落点、原41bae0/41bbb0核对1056状态组合，sourceSites004加入climate，缓存键按实际冬季贴图分支拆分；既有15张原图/80模型保留。CPU2884217项和348原静态模型几何通过，v154森林包重转换逐字节不变。v155主包83528694字节SHA2566725b9c4f45beeb5b263a758932c271c900d5da65b8fc9b2d74774741432b95b已构建安装；原图集PNG编码变化已单独调查，1040²RGBA像素逐字节相同，不以PNG压缩差异当作美术修改。首轮安装验证失败：主机11:07:17合盖休眠至12:00:56，3219秒停顿后120秒ready门限超时；失败时6可见地形块/6GPU块/pending0，初步零可见推断已纠正。唤醒时原特效子进程及系统media.codec异常留证，168846字节用户存档精确恢复。首轮失败保留；相同APK、相同120秒门限在重启模拟器后PASS2082，十种据点20春冬视图、远景、暂停恢复及城市/关隘/港口三种正常攻城后的原几何替换通过，50张实际PNG和145.718秒2197样本录像归档，168846字节用户存档再次全等。尚未接受PC冬季像素/时序或ARM性能，完整还原仍未完成。

# 2026-10-02 v154：原版地区气候与真实树种选择接入（目标未完成）

PC正常菜单已进入207年9月刘备新野，原输入事件＋仅OS鼠标轮询适配验证，原保存菜单产生48936字节Save001.s11，私有副本内保存，原安装只读。原D3D9 220次绘制166次与源WKMD前缀匹配；33处实际树木世界矩阵比例为1，证实不能通过缩模型解决选错树种。原41c3a0完整执行核对943落点，原41bae0核对144个季度/气候/LOD组合，使用原省区气候快照，PCSCN003接入全部26模型地区变体。仅两个Win32指针范围检查导入在私有VM内支持，未替换原游戏算法。转换可复现、来源摘要/消费者已更新，7098305项景物检查及348个已转换静态模型源几何核对通过。

v154主包83454370字节，SHA2564292bc470d2899231873ee53ca36092d089693bde1da07d60d38c0f20f2d1356，离线构建实际安装，同PC1650×1050镜头正常207秋季38项通过，三张实际GPU图及146秒605样本录像留存；源材质/雾/LOD/地面模糊和岸线差异未接受为还原通过。用户168846字节自动存档逐字节恢复，显示恢复1080×1920；三地区四季及远景安装检查进行中。14个全屏候选原模板已执行观察全部0/1/2/3叶节点，仍只是源调查，不算APK或真实战斗接入。全范围未完成，goal保持active。


2026-10-02 v153 补充：楼船正常移动/攻击、水面播放、暂停恢复、四倍速及全量权威/RNG/存档检查 PASS2988，用户168846字节自动存档与此前有效基线精确一致；32张实际PNG和141秒923样本录像归档 out/pc-visual/v153。正常207年9月秋季场景与实际PC镜头首次加载PASS49；调整模拟器显示后实际1650×1050地图对照PASS41，但画面仍明确有森林范围/颜色、地表清晰度、山体明暗和岸线等差异，未接受像素还原。显示已恢复1080×1920，存档再次精确恢复。PC进口鼠标API在Wine中定位失败的证据保留，新的仅OS输入适配器与原绘制常量只读观察器已编译，尚待实际执行验证。全屏文字与战斗效果未完成，目标继续active。
# 2026-10-02 v153：实际 PC 帧缓冲参考与实测镜头接入（目标未完成）

- 原版副本在 Wine 内置 D3D9 中取得有效1650×1050启动页、菜单及207年秋季新野/湖阳港地图；原安装目录只读，Present观察、正常输入、校验和和转换工具留在项目。GDI旧全黑失败保留，不能据此推断实际游戏黑屏。
- 首段PC水域真实GPU录像29帧/15.382秒，像素动态与MP4封装/解码已核对，约2Hz采样及读回开销明确保留；快速战斗/全屏演出时序尚不验收。
- 4份前后512字节稳定的原版相机快照确认30°视场、16源单位近裁面/目标高度、远裁面=视距1.7倍。生产SceneCamera/Filament镜头、源特效归一化裁剪、透视高度对应的X/Y标记同步修改；原矩阵2580项最大像素差.002171，镜头20160/拾取1152/帧准入检查通过。其他地点/方向、完整画面和触控仍须验证。
- v153主包83436782字节（SHA256 b62b98de302217bc2292cbe991bde77c23d2052bfd6e240620e157f729e6ffc3）已构建安装；正常地图165项通过，三源落点/暂停恢复/释放/权威状态一致，63原实体产生14556变化像素、恢复漂移0。实际用户存档与v152留存168846有效字节全等；run-as首次取到55字节错误文本另留失败记录，修正用模拟器su读取，不称它为有效存档。船只回归继续。整体战斗、全屏、MOD激活、全国远景/持续性能、PC匹配及ARM真机缺口保持，目标未完成。

# 2026-10-02 v152：源预算、透视拾取与真实像素验证候选（目标未完成）

- 保守500万执行块字节预算代替逐指令计数，原每调用5秒/Java帧6秒保持；源归一化透视914项/112包及完整16MiB堆、视觉RNG与原预算逐字节一致。源指令、条件与效果数据未改，不能据独立计时宣称持续性能通过。
- 透视三角拾取采用双精度投影与重心计算；实际float32触控射线参考1152项通过，原精度门限不变。原失败参考和修正尝试保留。暂停现同步冻结植被/通用水面时钟；宿主20160镜头/帧接纳检查通过。
- 137资产/四ABI文件、APK构建安装、实际主包及x86_64原生目录全等；包内31项传输/296包与额外存档边界通过。首轮正常地图59面，原1秒帧推进失败且GPU326次申请只接纳67次；源错误none，不能称GPU像素/生命周期通过。13真实输入重放原第11/12帧为59/67面；失败158.308秒108采样录像及168846字节SG11恢复全等保留。
- 冷启动相同模拟器后正常地图314项通过：三落点、暂停恢复/退出/GPU资源释放与完整权威全等；70原实体显示/移除/恢复产生15902个强变化像素，恢复漂移0，168846字节用户SG11全等。没有用调试发射替代生产地图绑定。保留5次Window backing丢失/恢复；两段178.559/177.039秒仅130/161采样录像，连续性与PC时序不验收。重启中的Activity服务DeadObject启动失败另记，非游戏验收。
- PC地点/镜头/视频及MOD激活覆盖、ARM真机、战斗与全屏演出、v148全国远景pending81等整体缺口继续保留，目标未完成。v152正常船只移动/攻击3271项通过：原GPU皮肤/编队、正常日记动作、暂停恢复/4倍、水面、退出释放与完整权威/RNG/SG11一致；用户168846字节存档恢复全等。资源绑定清单补齐462资源/491消费者。

# 2026-10-02 v151：原透视深度队列接入候选（目标未完成）

- v150真实13命令重放：5149次发射、最后116原绘制前缀、队列接受0。原45d5c0以abs(w)作为排序深度，而正交w恒为1，被4420a0的0<=depth<1过滤拒绝；此前诊断中偶发0.999999是浮点结果，不能作为正确地图投影。没有修改或钳制原深度/裁剪条件。
- PC 3D地图采用透视候选，保留旧地图正交，射线/拾取透视插值、标签和轮廓同步调整。GL投影转D3D并整体按far归一化，与原441b80相同投影比值及原深度语义；1188原相机/1184顶点136046项、安卓112绘制包914项逐字节通过。移动镜头实际参数仍待PC画面校准。
- 原地图视觉帧最多0.1秒，保留积压；暂停只重绘不消耗积压，原5M/5秒及Java6秒门限保持。实际地图553985、旧遮挡10441、镜头棱柱/反投影20160、帧接纳回归及137资源/4原生文件检查通过；两APK构建和安装成功。正常地图前两落点提交96/48四角面，第三处帧推进断言失败并出现Java6秒读取超时；尚未接受动态像素、暂停退出及PC时序。用户168846字节SG11恢复全等，失败录像68.509秒仅43采样帧。
- 全部环境语义、战斗/全屏演出、PC对应视频/MOD覆盖、ARM真机及v148全国远景pending81等缺口仍保留，不能宣称全部还原。

# 2026-10-02 v150：原地图常驻特效接入候选与相机条件修正（目标未完成）

- 原124资源33张原尺寸RGBA、原八类SEFF/126落点、源私有进程及源顺序四角面已绑定正常PC地图渲染，137项资源摘要检查通过。地图动态语义、战斗事件和全屏演出仍待完整接入及PC对照，不能据候选代码宣称还原完成。
- 正常安装测试仍保留89帧零四角面、相机修正后中断读取、延后启动后27帧零四角面三项失败，原30秒门限未放宽。加载中黑屏、GC停顿和稀疏录像完整保留；实际用户168846字节SG11在五份失败后快照中均逐字节一致。首轮安装未收尾就启动测试的竞态另存，不能归类为源特效崩溃。
- 原441ab0/441b80确认RH视图、组合view*projection和投影副本；648相机/1184顶点108506项、真实SceneCamera可见地形棱柱11520项核对通过。三个原落点在距离600源单位能发射，在1000及以上当前输入不发射；旧正交相机恒距300场景单位=6000源单位。候选相机沿视轴按可见地形棱柱选择实际位置，保持屏幕XY投影并保留原径向/扇区条件；补齐+80原投影字段，PC真实镜头仍待对照。
- 两ABI原特效进程重建，x86_64原诊断输入2386项/296绘制包逐字节回归通过；ARM64仍只有交叉构建。缩放后的原RH相机、新APK正常GPU/暂停恢复/退出及玩法回归正在验证，尚未接受动态效果像素或PC时序。v148全国远景pending81、MOD运行覆盖和ARM真机缺口继续保留。

# 2026-10-02 v149：安装包内原特效进程验证（目标未完成）

- 原八类SEFF/126落点与5779520字节原EXE页面接入独立视觉子进程，不访问规则、权威状态、存档或玩法RNG。原4096队列、贴图编号、混合状态、最终顶点/矩阵在独立源程序与安卓执行中逐字节核对；ARM64只有交叉构建证据。
- v149实际安装包82144320字节，103项资源及六份原生/源文件校验通过；安装主包和x86_64原生文件逐字节等于打包文件。安装测试五帧296绘制包、三次更新/两次暂停重绘、重建和关闭通过（31项报告检查，另有最终存档检查）。实际168846字节用户存档保持完全一致。
- 测试注册被AGP首个组件覆盖、独立入口未调用start()、AAPT将.gz展开成.bin三项测试接线错误均保留并修复。首次互斥锁崩溃属于uid2000测试启动shell进程，原特效子进程未启动，不能算游戏崩溃或源特效验证。
- 此包只有原生运算/传输接入，实际地图/战斗/全屏特效尚未绘制。当前工作继续接入地图GPU：原资源124的33张原尺寸RGBA、源坐标相机转换和原透明顺序；尚未作为v149像素或演出验收。v148全国远景pending81失败、PC/MOD匹配证据及ARM真机性能缺口继续保留。

# 2026-10-02 v148：原遮挡判定缓存与当前包验证（目标未完成）

- 保留v147远景120秒超时33项待上传的失败。v148逐对象缓存完全相同的相机/位置/不可变地形遮挡判定，10441项对照原方法、缓存失效和PC/旧地图检查通过；基线方法夹具进入项目，避免依赖忽略的out目录。
- 主包与测试包构建、101资源摘要、实际安装字节核对通过；冬季荆州单点近远景、暂停恢复、退出释放与完整权威不变247项通过，保持原120秒/10秒门限。船只正常移动/攻击、水面暂停恢复、4倍播放与退出2995项通过；移动指令提前完成，未生成旧包的移动step2中间截图，断言未改。当前30张部队/水面截图与4份远景文件分别归档，真实用户168846字节自动存档退出后逐字节恢复。
- 当前视频保留175.779秒地图底部短暂黑条，随后正常移动结果画面恢复；录像稀疏、原岸线和颜色待PC对照，不能作为连续显示或时序验收。十二组地区/季节近景全部采集；随后全国远景在原120秒门限失败、pending81，26份当前证据保留。暂停恢复/退出检查未走到，实际用户自动存档已恢复并逐字节核对。PC运行对照、MOD实际覆盖与ARM性能仍缺。
- 原KSEF绘制入口、源相机裁剪与动态绘制数据继续调查。5个资源的837条动态前缀、其中223条四角面×2缓冲状态446项源VB/矩阵/条带核对通过，原887条静态记录1774项回归通过。此前6个SEFF落点无子粒子的失败保留；源45a436在加载前绑定场景相机slot1，补齐此输入后6个原落点均输出源动态绘制数据。244模板含相机绑定的3步检查全部正常执行、236个有19092条数据，8个在当前输入下无输出；6803四角面/13606机器检查通过。安卓原环境/战斗/全屏特效仍为0，不能用调查记录或通用效果宣称还原完成。

- 新增独立C原特效执行探针，所需原EXE页面导出为5779520字节，四模板与完整EXE内存/RNG一致；宿主越界失败与修复保留。Unicorn配置缺少pkg-config导致空头文件的问题已定位，构建门禁拒绝该失败，依赖固定版本并只放在out。
- 安卓API29 x86_64独立进程68项完整16MiB内存/视觉RNG、相机绑定后489条动态数据3002项、源0/1/2/3最终96字节顶点与64字节矩阵3980项核对通过。保留原219进程45秒超时及按块计数125超时；改为条件等待式5秒看门狗并保留500万指令上限后，在同一45秒门限通过。原依赖2微秒轮询计时带来的调度压力已消除，未改源游戏指令或依赖库代码。ARM64仅交叉构建通过，无真机证据。
- 这些是独立原始运算/绘制边界证据；贴图/混合/透明队列排序、JNI和正常游戏事件尚未接入，原特效在APK内仍为0。1175份app/core文件与v148安装包归档逐字节不变，不能把新增工具或3980项检查算成新游戏演出验收。

# 2026-10-02 v147：原粗水面接入与特效运算核对（目标未完成）

- v146完整水位字节、3251项船只正常移动/攻击和197项冬季单点远景安装通过；额外攻击录像1635项通过，但181.8秒仅95帧，尚未接受动作连续覆盖或PC时序。v145退出/远景及完整12点历史失败保留。
- v147恢复原粗水格、四角alpha255/32、4844两套64帧贴图、源透明混合和关闭深度写入；原视觉初始化与玩法RNG隔离，暂停/可见块时钟、缓存和释放接入。65536原记录/2048顶点机器核对、1602564地图检查、741120全国水格检查和163交互编辑器检查通过；120原时钟更新/2168整数时钟检查通过。
- 101资源摘要与APK构建通过，主包/测试包实际安装并逐字节核对，船只正常移动/攻击、水面暂停恢复与退出释放3269项通过，32张当前截图和3段当前录像归档，真实168846字节自动存档逐字节恢复。录像稀疏，保留场景加载时水面尚未上传的画面，未接受连续时序或PC一致性。v147冬季远景未通过原120秒门限，失败完整保留。全国上传19496/20076湿粗格，全13017可玩中心粗格已包含；外缘580格、原逐格可见列表时钟、PC图像/MOD覆盖与ARM性能仍缺。
- 244个原KSEF启动/发射/更新全部通过；原CRT浮点初始化修正了64个调查环境异常。安卓原特效接入仍为0，部队旗帜、环境/战斗/全屏演出等整体目标继续推进，不宣称完成。

v147冬季单点远景未通过原120秒准备门限，超时pending33；近景和实际用户存档恢复通过。失败截图/日志与23954进程的原生栈保留。主线程仍重复执行标签的地表光线求交，渲染线程也存在模拟器管道等待。v148已引入每个存活对象的精确遮挡结果缓存，镜头、位置、高度或不可变地表改变就重新计算；不放宽精度或断言。对归档v147原方法的10441项PC/旧地图检查通过，v148单点远景247项通过，但整轮十二组后全国远景仍120秒pending81失败；不能作为全国性能通过。

# 2026-10-01 v138：四处原水坝实体、正常攻击与保真分块优化（目标未完成）

- 源OBJS种类20四处原半格坐标、原高度和朝向直接绑定真实DAM实体；源SHEX湿地不改为推定地形。新开局修订65增加四坝，旧64存档保留原实体且不补坝。
- CPU 3,235,900项、安装连续八次正常攻击1,970项及单次击破319项通过：原模型状态与真实GPU矩阵、独立权威/RNG/SG11、洪水敌我600损失、击破不复活、近远/旋转/暂停/退出释放。原溃坝/洪水动画尚未恢复。
- 去除地表无用临时数组与邻格分配，五类窗口超过6800万项GPU输入逐位不变；未放宽120秒准备断言。原远景超时与测试端R8方法缺失失败保留，修复后整轮通过；模拟器加载/GC停顿仍明显，不宣称ARM性能通过。
- 实际旧64存档在当前与继承64代码独立推进三旬，四快照全等；实际安装测试最终自动存档恢复为原168,846字节。历史core失败逐项与HEAD或继承PC64对照，不降低断言，也不声称完整core测试通过。
- v138可安装包、75项资源摘要、421资源/446消费者、截图和四段当前录像归档。部队/动画/环境/战斗/全屏演出、PC对应视频及ARM性能继续推进，177项范围状态仍明确未完成。

# 2026-10-01 v137：土垒/石墙原连接与正常状态更新（目标未完成）

- 源5a0160确认源格→半格位置、原高度采样和方向表；脱离core快照按同类健康/完成墙体计算六邻格连接，原柱136/208与段130/196按源矩阵构建。补修完成增加连接，正常受损/摧毁同时移除两端旧连接，不更改规则/RNG。
- CPU奇偶列16正常指令1,398,264项通过；设施4,755,020项与交互32/131、帧准入/相机回归通过。安装土垒303、石墙321项验证四种正常命令及远景/暂停/释放；石墙摧毁补录59项。有效SG11前后168,846字节全等。
- 两段约180秒录像实际采样率偏低；土垒最后178秒解码确认摧毁，石墙末次摧毁被180秒上限截断，已补录63.195秒，并解码16秒完整连接→20秒中央消失/端点断开。不把旧粒子/旗帜当PC效果验收。
- v137阶段APK已安装，73项摘要通过。421源资源/435共享消费者关系可重建；源1805文件保持只读。部队/动画/环境/战斗/全屏演出、四处堤防、PC对应视频、ARM性能仍未完成。启动黑色加载画面与继承的nativeSession恢复提示已记录，不能声称全范围零黑屏或流畅度通过。

# 2026-10-01 v136：源墙线连接、独立柱与端点高度剪切（目标未完成）

- 从实际EXE确认六邻格连接掩码、仅方向3..5绘制无重复边、柱型号118/120及段114的伸缩/剪切矩阵，新增12个近远连接专用原模型，设施库242模型/56源贴图。161原落点以161柱+154墙段接入正常地图。
- 1,831,366项源矩阵/法线/UV/有向三角形、六邻格图、季度/裁区/缓存及权威不变检查通过；设施4,755,020项回归通过。v136模拟器安装255项验证三处春冬近景、全国远景、暂停恢复与退出释放，外部SG11前后逐字节一致。
- 实看原先平行分离墙段已形成有柱连续折线；这只证明接入及连接，不代表PC高度/光照匹配。v136阶段APK与截图已归档，73项资源摘要通过，源1805文件不变。修复跨Python版本gzip头系统字节，解压内容未变，输出跨版本逐字节相同。
- 可建土垒/石墙动态连接、原堤防、部队/动画、环境/战斗/全屏演出及PC视频/ARM性能继续推进。源墙线初始化对地表的修改也尚未还原，不能宣称墙线视觉验收完成。

# 2026-10-01 v135：源地图墙体基础接入与保真远景合批（目标未完成）

- 161条OBJS对象14原落点与近远主体接入生产分块渲染，复用原设施材质；906,389项源变换/法线/UV/三角形/裁区检查通过。GPU安装254项验证三处春冬近景、全国远景、暂停恢复、退出释放和权威不变。
- 保留原顶点、色值、UV、法线框架和全部三角形/绕序，远景植被238→113批次；7,180,220项检查通过。未延长120秒加载断言，全国远景和三次正常城/关/港攻城回归198项通过，修复v134全国加载超时。
- 六次农场/阵正常建设、受损、摧毁安装复验通过，独立权威/RNG/存档全等；162.325秒正常攻城和132.909秒六设施指令视频已封装验证/解码，旧战斗粒子与旗帜仍未验收。最终外部自动存档前后有效SG11字节全等。
- v135 APK已安装，73项资源摘要通过；源1805文件复核不变。严格保留未验收问题：实看墙段平行/分離后，源EXE已确认六邻格连接位、独立柱模型、端点高度剪切与仅绘制三条无重复边，下一阶段继续实现；不能把基本主体GPU验证称作完整墙线恢复。
- 树木明暗、水面/岸线、比例/光照/透明、部队/动画、原环境/战斗/全屏演出、PC对应画面视频和ARM性能均待完成。详情和失败记录见docs/pc-visual/validation.json。

# 2026-10-01 v134：源内政与军事设施主体及正常指令接入（目标未完成）

- 从整合版Scenario.s11核对64条设施定义，源EXE确认54个可绘制设施与模型/贴图/状态映射；转换55类（含独立悬崖墙模型）、230模型和56张原RGBA贴图。源图保留128/256尺寸，不重绘。
- 当前core的11类内政、19类军事设施及等级、建设、HP受损与近远/季度选择接入生产渲染；建设主体替换旧脚手架。原旗帜、燃烧与坍塌/洪水仍待恢复。
- CPU设施4,755,020项通过；最终安装437项验证六次正常建设/攻击/摧毁，权威状态/RNG/存档与独立执行逐字节一致，暂停恢复、远景与退出释放通过。
- 修复正常指令切换场景后等待旧全国地图任务的阻塞：沿现有有界工作队列取消过时相机窗口，保留GPU帧准入与背景地表所有权。生产方法/真实队列检查通过；失败证据留档。
- v134阶段APK已安装并通过72项资源摘要门禁；两段安卓正常指令录像与20张截图/报告已保存，但录像有间隔且宿主帧率低，不能当作全动态效果验收。源1805文件复核未变。便携Wine已在独立副本运行，未取得有效PC画面。
- 悬崖墙落点、原部队/动画、环境/战斗特效、全屏演出与源运行对照继续推进；截图可见树木偏暗、阶梯岸线/多边形水面仍待校准，无ARM真机性能证据。详情见docs/pc-visual/validation.json。

# 2026-10-01 v133：原城池、城墙、关隘与港口接入（目标未完成）

- 继承v132与v131全部工作区成果，新增87处源据点的80个WKMD近远模型、15张原贴图；城池保留独立城内/城墙组合，源位置/方向/高度用于显示与拾取。
- 从源EXE确认城内规模取完成内政建筑数，城墙受损按500/1000耐久阈值，关港按min(maxHP/2,500)阈值；接入正常规则状态，不增加玩法随机数或改结果。
- 修正v132植被把模型季节顺序与纹理季节顺序混用的问题；四季源模型编号独立断言通过。源安装1805文件复核全部未变。
- 源据点/LOD/材质/裁区/旧存档及正常攻城检查2,882,104项通过；地图635,299项、植被7,179,793项、交互32+编辑器131项、R04 87,689项通过。最终模拟器安装检查1,677项、正常攻城专项211项、植被四季735项通过；90.278秒正常攻城录像、前后截图和源码/资源摘要保存在out/pc-visual/v133/，APK已完成安装与70项资源摘要验证，详情持续写入docs/pc-visual/validation.json。
- 原归属旗帜、其他设施、部队与动画、战斗特效和计略/暴击全屏演出仍未完成；源区域冬季材质、光照、比例与PC动态参考也未完成。当前只能作为真实资源接入的阶段包，不代表全部视觉还原。

# 2026-10-01 v132：PC 视觉恢复持续目标（未完成）

- 在 `agent/native-pc-visual` 的 v131 未提交工作区上继续，保留原成果。源整合版 1805 文件/4876 资源逐项建账，源目录摘要全部未变。
- OBJS 的 943 条类型46/47/48落点、26个WKMD模型及四季透明图集接入生产3D地图、裁区与源近远LOD；PC模式拒绝旧推定植被布置。
- 修复地表无效混合层残留字节被误读导致的块状接缝；4,194,304个面角与源顶点材质全等，实际重装截图确认接缝消失。
- v132 APK、25张当前截图、安卓接入录像、源码摘要和验证日志在 `out/pc-visual/`。API29 x86_64模拟器安装检查779项通过，包含四季、LOD、暂停恢复、退出资源释放及完整权威/RNG不变；无ARM真机/PC动态对照验收。
- 树木偏暗、垂直/UV比例、水面/环境光照尚待校准；城市、设施、兵种动画、战斗效果、计略/暴击原字形与全屏演出均未完成。162项范围状态见 `docs/pc-visual/coverage.json`，完整边界及证据见该目录。目标保持进行，下一阶段继续城池/设施材质与变体绑定；已定位EXE的388项模型表和66类通用对象映射。

## 2026-10-01 PC 地图导入 v131 — 地图数据与渲染验证版

在 `agent/native-pc-visual` 上读取用户提供的 PC 整合版，导入 SHEX 地格、K3ST 高度／面水位、四季 GCOL 与 WFTX。新地理修订 64，19,642 格分类变化、11 港位置校正、591 开发地恢复；旧存档保留其地理。地图专项、真实 v63 旧存档、交互／编辑器、R04 与资源摘要验证通过，API29 x86_64 宿主显卡模拟器实际 Surface 截图通过。新开局默认 3D，显式选择 2D 仍受尊重。

这不是完整 PC 逐像素复刻：原 UV 标志／远景材质、PC 光照、建筑／植被／动画资源与 ARM 真机性能仍待验证或移植。历史 core 测试的既有失败已与修改前 HEAD 对照。详见 `docs/pc-map/README.md`，APK／截图／验证日志在 `out/pc-map/`。

## 2026-09-22 S05 checkpoint — PARTIAL

Continue PR #62 / agent/3d-s01-renderer-foundation; main unchanged.
Delivered moving-unit pose/selection/labels and detached real equipment/state, plus
wounded playback clone correction. S04 final art and full S05 models/clips remain blocked.
See docs/3d/acceptance/s05-status.md for exact coverage, regression results and untested gates.

# v0.62 road-only candidate — incomplete overall task

- DONE: full v061 inherited by merging PR49 only; four production ordinary-road render paths changed; existing UI model regression passed.
- DONE: road-only candidate built from the BUILD_COMMIT recorded in last-run.json, installed and pixel-tested; standalone prerelease APK re-downloaded with identical hash. Full v0.62 remains BLOCKED.
- BLOCKED: all14 VOID repairs and generated runtime art; do not claim completion or use the generated poster as evidence.
- Geography61 / art56 / SaveCodec31 retained; branch PR must remain open.

---

# v0.61 — 两角水面与界外呈现；已安装 APK 已发布

## DONE
- 承接已合并PR48的main049652d45641ff247c964ec30f70f5cff5c629f3；本轮分支agent/map-corners-boundary-v061，不回退、不强推。实际解压读取本轮7200×6752原图并核验指纹，未分发原图/裁片。
- 206个唯一全国源格V→Q：东南41、西北56、北侧107、永安内部2。1051个原VOID仅添加明确范围的外景显示：东南海面869、西北荒原126/山体56。两类数字分开，不重复计算以前版本。
- 200×200 odd-q、World-aware坐标、87据点、591开发位、七格城、CityAtlas56、存档31及已有游戏功能保留；全国源坐标展示统一。外景不改变inside/sourceInside，不参与领土/寻路/开发/点击，不覆盖未知格或旧版存档。
- 正式功能提交9b69f6fd09cd15a75e09666ec12ef0f39575dd7e已提交推送；后续仅androidTest和CI触发修正。最终构建源9ad16d82c5a33211d28fa84e677b53e155f535b5；assembleDebug、模型、实际安装、发布、回下载核验在run35559224769全部SUCCESS。
- APK v0.61.0/code61，22339175 bytes，SHA256279290bc30c1c5f7e5f8b9d4b12d2cde951f8382cf975f71a585e2641d2d6905；独立Release资产已发布。安装、发布回下载及本会话本地复核字节一致。
- API29/x86_64模拟器实际签名覆盖安装，旧手动存档字节未改变；Reference61最终9866条PASS及native200操作92项。四剧本、两角完整视野/LOD/小地图/势力色/开局预览、行军、下一旬、出征、保存重读及12份真实58/59/60旧档通过。未将模拟器当物理ARM验证。
- 最终截图已实际查看；与旧v060同中心/相机/LOD/分辨率完整对照：东南旧整片黑洞消除，西北整片碎裂显著修复但14格局部黑孔仍可见。无裁切隐藏。
- PR49已创建指向main，保留待审未合并。最终文档提交与BUILD_COMMIT分开，不要求文档SHA冒充APK源SHA。

## IN PROGRESS — 地图考证，非构建状态
- 西北14格仍有视觉缺口，本区域尚未全部完成；源坐标x18–38,y14–24，逐格见docs/validation/v061/README.md及data/map/reference-v061/remaining-void.json。没有将它们标成外景或填成水/山。

## TODO / BLOCKER
- 缺无遮挡原图或原版地形/有效格表；精确原版边界/航行权限与部分栈道/地形细分待证。206新Q原版通行权限未确认，继续禁行；100外景材质估计不能冒充恢复原版陆地。
- 物理ARM/FPS、全部历史测试套件未认证。当前APK交付没有待执行阻塞；上述是考证/设备覆盖范围限制。
- 新开局使用revision61。旧档保留自身地形/revision/预算/单位，禁止删除或静默迁移，不把新版外景强套旧档。

[独立APK](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/releases/download/v0.61.0-9ad16d82c5a3/sanguo11-mobile-v0.61.0-9ad16d82c5a3.apk) · [PR49](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/49) · [构建/安装/发布](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35559224769)

详见docs/validation/v061/README.md、final-publication.json、visual-review.json。历史进度原文保存在docs/history/progress-through-v060.md。

### PC visual v139 ongoing (2026-10-01)

Original map troop assets are distinguished from event character857: map models2207–2233, originalRGBA2206–2232, nested75FCVD2236. PCUNIT02 retains source UV, opacity partitions, skin weights/binds, global-frame polynomials and 8×76 formation offsets. Native units use original bodies through normal journal events; legacy flags are not substituted. Source reference462cases and4,050,106checks passed before new event boundary assertions;29real source-map command preconditions and exact saves pass. Unit GPU install checks remain running after two retained instrumentation ABI failures, with recovered original user autosave byte-exact. See docs/pc-visual/units-validation-working.json.

Same source0 damaged-dam far120s readiness fails on exact v138 baseline and preliminary v139. Bounded immutable water-field memo keeps all five complete GPU input hashes bit-identical to v138; installed performance still pending. Original PC reference/timing, source flags/battle/environment/fullscreen effects and ARM acceptance remain outstanding. This is ongoing goal work, not full completion or a newly accepted release.

2026-10-01 PC v139继续：WARSHIP正常移动/攻击与当前事件GPU姿态206项安装通过，实际用户自动存档逐字节恢复。全部兵种29组整轮严格GPU验证运行中。新增原投石台2234/2235六骨骼及2236内73/74曲线包，源一秒准备和60Hz时间边界接入正常旬事件；独立原源姿态/正常旬规则131,952项通过，安装待完成。PC源投射物/旗帜/环境/战斗全屏演出和PC视频对照尚未验收，目标未完成。

v139继续：共享每个Ground的精确水面缓存，五窗口完整GPU输入仍与v138一致；地图635299及R04 87689项通过。水坝0正常受损后的远景原120秒门槛最新313项通过。原正常/受损投石台125项资源安装检查通过，保持原默认240秒失败与独立resourceEvidence600秒运行区别；录像尚未确认连续发射覆盖。走舸/楼船/弩兵近战/骑射安装195/195/194/196项通过，当前29组整轮仍运行。新增原KSEF相对图解析及EXE244效果/347原RGBA绑定，246图2410渲染节点结构通过；原全屏人物卡/字形已找到，未以静态解码代替玩法演出，目标仍进行。

2026-10-01 v139 阶段归档：当前 APK SHA256 d1983e9f8e35b07c44f08e7ff164e087d1b75a08c41c0f5dd87913fb1845b72f，29 组正常兵种指令 2815 项、投石台正常/受损默认 240 秒检查 128 项、水坝0受损原120秒远景313项通过；原用户存档逐字节恢复。两个默认投石台录像已抽查正常75/80/85秒和受损170/175秒源投臂变化，第二段编码稀疏，未接受连续时序或PC一致性。阶段包 out/pc-visual/sanguo11-mobile-v139-pc-source-units-platforms.apk；整体目标保持 active，PC像素/ARM/旗帜/原投射物/环境特效/全屏演出未完成。


2026-10-02 v157—158继续：v157接入原K3ST量化法线、地表RGBA明暗、SENV环境色/雾/淡出与4807背面描边；最终同镜头69项安装检查通过，三地区四季12视图取得，但宽屏远景120秒失败108待上传，原v156同宽屏也失败138待上传。v157默认竖屏远景失败142待上传，正常攻城模式被其远景前置挡住并退出屏障失败，未宣称攻城/暂停恢复通过。所有测试结束实际用户168846B存档与原SHA完全一致，原源目录1805文件2.78GB摘要不变。v157源码3465文件已快照。v158候选增大全国原树木合批，不减少模型细节；7097426项原顶点/三角形核对通过，APK构建及146资源摘要通过，安装远景检查进行中。原全屏演出33静态调用/347原贴图队列关系核对已新增，尚无安卓事件绑定和PC演出视频验收。完整目标保持进行。

2026-10-02 v158继续：原顶点/颜色/UV/切线/三角形7098040项通过；全国远景仍120秒失败。独立近景入口三种正常攻城213项安装通过，原受损模型、完整权威状态/RNG/存档/暂停恢复/退出通过，全国远景明确不在其覆盖内。软件Swiftshader同APK严格120秒检查失败于近景(span8)，仅2次提交/517次尝试，27项待上传；不能据此认为远景只有宿主驱动问题。原用户存档全等。两段录像已查看，城池攻城后/关隘后状态可见，港口某帧地图黑色但战报可见、最终截图源港口可见，问题仍调查；录制间隙不包含关隘攻击。PC只读观察器新增显式512纹理读取范围，人物表只读解析工具新增，正在正常原PC流程采集全屏参考。

2026-10-02 用户演出参考：桌面三段视频已逐字节保留并抽帧，用户确认是网上演出参考，不能当所供整合版的像素/时序验收。用户要求不再启动 Wine，现有进程已关闭；后续只读源程序/资源转换和安卓接入继续。单次暴击当前目标约 500ms，依据用户指定而非 PC 实测。原人物/书法使用不同模板，原413d20工厂分支及最终顶点/材质已执行取证，相机仍为诊断输入，尚未接入安卓全屏。

2026-10-02 v159持续：原人物240/书法246模板和521/495原RGBA，通过原413d20工厂、45a530父级登记更新和原队列/最终GPU顶点转换；两套125帧与先前输出逐字节一致。正常关羽突刺/扰乱原全屏图层376项暂停安装检查通过，完整权威/RNG/存档、暂停恢复/4x/跳过/退出通过；首批仅两种绑定。连续500ms首测约3685ms且仅2源GPU帧失败；第二次仍跳过入场/时钟反复等待且退出屏障失败，均保留。已改真实视觉计时、一次准备后启动、预编译/纹理上传/首帧等待、隐藏遮挡立绘的地图标签轮廓及暂停后台视觉VM；最新重验进行。用户存档168846B每次恢复与原SHA一致，包括失败后停止测试进程再外部恢复；源目录只读、未启动Wine。正常源指令/时间边界28项、PlotOutcome100项、S06 25470项、R11 99443项及严格架构检查通过。当前RulesParity当前与独立原War/TurnJournal基线同actions54选势力失败，未改断言；PC/背景混合/其他人物计略/全战斗效果/全国性能/ARM未完成。

2026-10-02 v159 连续演出继续：原图层相邻同纹理／混合状态合批，人物峰值90层→6批，书法峰值80层→13批，完整原顶点／UV／颜色／索引／顺序55951项核对通过。合批、驱动Fence、背景姿势暂停和实际无像素首绘预热均单独保留失败；公开Engine Fence仅证明驱动命令处理，不能证明GPU完成。改为独立演出Scene并缓存一次真实地图Surface后，正常突刺和扰乱连续43项安装检查通过：各14个源提交帧，估算504.24／504.18ms，源主体90／77层均进入画面，完整权威／RNG／存档一致，原168846B用户存档外部读取全等。两段30秒短录像实际抽帧已确认原关羽／笔触／光线和原书法／紫色图层。主人物峰值位于500ms内部短窗口，不能把总500ms声称为主体驻留500ms或PC精确时序。新增真实缓存颜色／方向／暂停／4x／跳过／释放重验运行中；整体目标未完成，仍未启动Wine。


2026-10-02：用户明确单次0.5～1秒均可，默认调整750ms。原Big5计略名称表/5933a0分派核对发现此前CONFUSE126误绑定，已移除，126/121绑定SORCERY，新增127/122/原247+496落雷。九类真实计略事实150项、正常国地图/原GPU输入/时钟/自定义同名不借用原立绘72161项通过，157源资源包构建安装。关羽连续15帧/~733ms；妖术连续只有3帧、源退场覆盖失败，落雷连续未到达；严格地图缓存色差仍FAIL11。所有失败及原用户存档恢复SHA保留。新只读FCE人物描述表调查不启动Wine，人物具体脸/年龄选择仍待核对。目标继续，未完成。

2026-10-02 v159继续：用户认可500～1000ms，默认750ms。新单项正常妖术/落雷连续播放各PASS23，均21有效源帧、约746/751ms，原文字/紫光/雷光已从实际录像查看；组合连续FAIL保留，不冒充持续序列稳定通过。三种正常演出生命周期PASS247，strictcachecolor未包含且仍FAIL11。原FCE2200项/6604次源直接/actor/性别回退查找已验证，officer年龄/MOD映射未确认；社区sourceId不是已核定PC人物编号。全部用户存档root读回SHA一致。下一候选用实际片元坐标逐像素读取缓存，测试保留原严格断言；PC安装只读，不启动Wine。

2026-10-02 v159继续：源背景严格色差的问题已定位为真实Surface因战报布局从756×785变756×691而缓存仍785高；逐像素texelfetch单独仍FAIL11，原失败留存。生产surfaceChanged失效旧捕获并在重准备期间冻结视觉时钟，三种正常暴击严格安装PASS284，64点192通道最大/总色差全部0；暂停/恢复/4x/跳过/释放/完整权威与RNG存档通过，用户root读回168846B原SHA一致。GPU读回只由显式诊断观察器启用，正常路径无读回开销。主机源输入72161、R11 99443和架构、157资源门禁通过。新版本组合连续复验运行。战斗反馈旧基线亦失败：夹具城市越界/称号未初始化/设施ID与开发地无效，合法修正后发现显式MOVE误用目标APPROACH、山地错误缺少名称，原断言保留并继续修复回归。PC只读，未启动Wine。

2026-10-02 v159复验：viewport严格全三类PASS284后，连续关羽失败只有1有效源帧，设备SurfaceFlinger驱动停顿留存。保留userdata重启本任务模拟器并重新构建安装最新viewport/地形名称APK后，连续仍FAIL（2有效源帧、源phase.0747～.264、峰值1层，未覆盖主体/退场；约762ms时钟不能替代渲染验收）。失败录像分别仅23/11个封装视频样本、源CSV/owner计时和日志归档；原用户root存档168846B SHA全等。严格检查实际284项。BattleFeedback209/Plot150最终独立BUILD SUCCESSFUL5s；Government旧v7迁移拒绝、Marches无港口跨河失败均独立原基线复现，断言不改。完整脏树3508文件199548600B与157固定资源快照归档；新增源镜头只读证据工具确认调用方相机与512B临时复制/恢复，不套用内部25度默认值。整体目标仍进行，未启动Wine。

2026-10-02 v159继续：用户接受500～1000ms，默认750ms。源帧提交计时防止GPU拒绝期间跳过原立绘；clock-only安装14源帧/峰90，但1877ms仍失败。原转换深度约.9～30，map span2裁区将600/2580关羽原图层完全裁掉，独立.05～50源深度相机保留原XY/FOV/模型姿态；主机72378通过。首候选因地形先发布而相机暂时正交导致加载fallback，修复显式过渡后关羽严格136项/背景误差0/相机与完整权威存档检查通过。空覆盖层候选三类连续提交76项通过，各22帧、~754～790ms，但实际录屏缺关键演出像素；仅获准帧Window刷新版本仍缺录像且落雷3325ms失败，未接受性能/动态验收，优化撤回。当前恢复原Window刷新并重新安装验证。新增顺序解码每个实际视频样本工具，避免把提交计数或请求抽帧时间当实际画面证据。所有原用户root存档168846B/SHA全等；源只读，未启动Wine。全范围目标仍未完成。

2026-10-02 取证纠正：AVAssetImageGenerator随机定位1.6s返回旧地图像素，但新AVAssetReader逐样本PTS1.5925确有源关羽立绘/笔触/光层；此前缺演出/窗口合成原因推断撤回，原影片/旧抽帧留存并标记失效。工具改顺序样本与最近实际PTS，现抽样70帧/3s合成图已查看。恢复原Window刷新候选关羽连续PASS35/~820ms/20源帧，真实源演出可见；其4s仅30实际视频样本、2.271s地图区域灰白单帧仍待查。主包新增第一帧GPU读回观察器仅显式诊断启用，正常流程不增加读回。全三类严格安装验证继续，源只读，目标未完成。

2026-10-02 v159实际安装全三类严格PASS482，独立源深度相机投影XY/背景采样误差全部0/暂停恢复/4x/跳过/释放/完整权威RNG存档通过。结束后的root168846B/SHA全等。首stage原生GPU诊断候选已构建安装，显式raster开关才执行，正常路径不增加读回；目标继续进行。

2026-10-02 首帧灰白修复：实际原生GPU第一帧只有4色，颜色计数520950/755/690/1，精确对应1x1viewport夹边采样；本地FilamentEngine::prepare在beginFrame提交材质参数，render内改viewport第一帧仍用旧值。warm1x1目标保持正式捕获视口后，安装PASS129，第一与后续背景522396像素逐通道最大/总误差0，PNG同SHA dd32c833cdeaac969c6c00a57ea5eec7ffefca40cc42eb1b2dc51fe5b210bbd5。157包资源/架构门禁通过；正常三类连续录像正在复验。当前设备srgbSwap=false，sRGB输出层的首帧参数/ARM仍需独立准备及验证，不扩展结论。源只读/用户存档全等/目标继续。

2026-10-02 v159本阶段连续复验PASS81：关羽21源帧/~775ms、妖术16/~1141ms、落雷20/~819ms；妖术超过1000的估算仅通过原400～1200ms模拟器采样容差，不声称稳定帧时。实际源视频SHA/所有顺序样本/合成图已归档查看，关羽/妖术前4s与落雷前8s未出现旧均匀灰白地图帧；落雷准备与系统录像仍稀疏延迟。原用户root168846B/SHA全等；当前APK a00194f6cd678e986e761962b37fe11a57bccc75b2244784d7ad8b2920c3668c，157源资源门禁/首GPU522396像素误差0/架构通过，完整3511文件199601868B源快照。600图层越过有限mapfar是CPU名义投影诊断，FilamentGPU投影far实际无限，不把该数字声称实际GPU裁切修复。完整目标继续，原镜头/其他人物计略/其他战斗/sRGB输出首帧/ARM仍待完成，未启动Wine。

### 2026-10-02 重启前交接：双会话转向玩法/数据与 UI/UX

用户准备重启电脑，要求先提供两个并行会话的提示词，后续重心为玩法对齐、剧本、武将数据及 UI/UX；本轮开发在此交接，不将完整美术目标宣称完成。禁止启动 Wine 的约束继续有效，演出默认750ms，用户接受500–1000ms。

本轮新增 tools/content/inspect_pc_scenario_portraits.py 与 docs/pc-visual/scenario-portraits-source-working.json：实际安装目录16个剧本、6位 canonical 武将，经原48b760完整152字节序列化解码核对姓名/出生/性别与FCE原48a5b0查找，96条剧本记录通过；另用原4a66ea年龄换脸分支及488a20年龄计算核对18条年龄边界。刘备/关羽/张飞/赵云/诸葛亮/曹操的年轻、年长贴图均来自原资源；年龄=年份-出生年+1，阈值分别48/47/42/58/43/54。附件SceCharData只用于识别拆分姓名，不作为生效资源。MOD实际覆盖顺序仍待验证。

本轮未验证工作区改动：CriticalHit增加不可变year，World.tacticCritical捕获当前历年；PcPresentationPlan增加上述6位12种年龄头像，保留既有年长关羽selector152及妖术126/落雷127；import_pc_presentations支持显式selectors并修复重复记录，已转换14个selector和template115；PcCriticalsFixture/Probe及PcPresentationsInstrumentation增加正常命令年龄边界案例。这些新改动尚未运行主机检查、Gradle构建、安装或录像验证，下一轮须作为待验证工作处理，不混用上一包的129+81验证结论。

最后实际已验证并安装包仍为 out/pc-visual/v159/app-debug-critical-first-raster-viewport.apk，SHA a00194f6cd678e986e761962b37fe11a57bccc75b2244784d7ad8b2920c3668c。用户原自动存档备份 out/pc-visual/v159/user-auto-before.sg11，168846B，SHA82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9。不要清除模拟器数据。重启后先核对设备、存档和完整dirty tree；之前的模拟器会话ID不可假定仍有效。

并行边界：会话A拥有core/game-api、剧本与武将内容/转换/验证；会话B在包含全部当前工作区成果的独立目录拥有app UI/UX与交互表现。不得在同一目录并行改动或共用输出/Gradle构建缓存；未提交成果与APK固定资源应完整继承。跨边界接口先形成明确契约，最终由单一集成负责人按次序合入并构建验证。

### 2026-10-02 玩法/数据会话接续第一阶段（目标 active）

已保留完整dirty tree并封存 `out/parity/baseline-20261002/source` / manifest：3524文件205142686B，另封存双ABI四份原生worker构建输入。UI独立目录已继承此基线；本原目录app逐文件SHA仍与基线一致，没有覆盖UI改动。共享契约 `docs/architecture/PARITY_UI_CONTRACT.md`，玩法/数据台账 `docs/PC_PARITY_STATUS.md`。

交接年龄改动主机72450、原六人96记录/18年龄边界、BattleFeedback209、PlotJournal150通过；新增CriticalYearTest50项真实跨年/非1月开局/不可变事件/失败无RNG与存档变化通过。完整core/runtime检查31个失败任务已在封存的完整源树独立复现，任务及首异常文本全部一致；断言保留。静态架构围栏通过，完整架构行为仍被历史old-scenario golden断言挡住。新PcCampaignFlowProbe通过423项：九剧本各真实session巡察/旧token拒绝/六旬/逐旬读写。实际用户档副本在当前与基线各六旬，七份完整字节相等。

只读新工具 inspect_pc_scenario_officers.py 执行原152B serializer，全16剧本×670候选编号10720条，10656身份确认、64扩展Big5字形记录隔离。Scen014 native279/333实际宋憲/徐榮交换，已分剧本按姓名/出生/性别重映射；发现11处能力与6处适性差异，未盲目覆盖现有自制/社区剧本。完整原记录/actor/源SHA/偏移及映射已确定性gzip归档于docs/pc-data，四项独立回归（含此前六人96条）通过。1805源文件2780244569B SHA无变化，未启动Wine。额外人物槽位/关系/特技/势力据点初始局面和MOD覆盖链仍待解码。

新主APK已构建、签名核对、实际覆盖安装；`out/parity/age-validation-20261002/sanguo11-age-candidate.apk` SHA80ca0c5a579814af1a9f6ea20359bfcc539806dba6d3bb20cb45ed35101d8510。Android29/x86_64保留原userdata，正常菜单新开184/何進，实际两旬，修订65/4堤防/turn2存档核实；冷启动文件不变，最终恢复画面未再截图。首旬界面24.8秒，性能未通过；第二旬30秒录像只68样本且为部分覆盖。测试结束原5份文件（auto及偏好）外部读回全等，auto168846B SHA82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9；保留新局创建的customOfficers/library.json，未删userdata。

六位年龄GPU专项仍未验证：继承PcPresentationsInstrumentation引用作用域外fixture导致test APK失败；最小三行修复已在out/parity/age-validation-20261002/ui-test-compile-fix.patch，UI独立目录已自行做完全相同修复，本目录尚未顺序集成。未冒用旧APK或UI测试结论。下一步集成该独立修复重建测试包，年龄专项/更多正常游戏命令实装；继续人物全字段及原剧本其他记录，再接入核实的正式数据，修复有充分依据的规则差异。ARM、全规则、完整剧本和UI最终集成均未完成。详情 docs/validation/parity-20261002/README.md，目标保持active。


### 2026-10-03 玩法/数据接续第二阶段（active，长测仍运行）

顺序集成了UI独立目录已经完成的PcPresentationsInstrumentation三行selector参数修复（不是另行重做UI）；集成前完整3535文件207711304B快照out/parity/pre-ui-test-integration-20261003，集成后逐字核对全部app基线文件仅此一项差异。app/test APK构建成功，主包80ca0c5a579814af1a9f6ea20359bfcc539806dba6d3bb20cb45ed35101d8510、测试包d1282b99aa37888f647a028fd778da92f29e3fc1a6ccbe361fa2c2a7157539eb。外部root备份后实际覆盖安装，主包设备读回SHA同候选；只操作emulator-5554。

12年龄专项仍运行：截至本条刘备年轻137/年长137、关羽年轻134/年长134、张飞年轻133项通过；张飞年长及赵云、诸葛亮、曹操两年龄未结案。受控暂停、原selector/GPU层、严格背景颜色、暂停恢复/4x/退出、完整权威/RNG存档；不声称连续帧时、PC镜头或ARM通过。刘备年轻step1实际查看原立绘。每例结束外部恢复所有原文件逐字节全等，auto168846B原SHA82554269...f0a9；未清userdata。批处理verify_pc_age_install.py每例finally恢复，输出out/parity/age-installed-20261003/all-six/results.json及run.log。exec session25430仍运行（进程如结束以输出为准）；用write_stdin轻量续取或读取日志，不并行再安装同设备。累计report与通用failed.png可能含旧数据，本例新txt才是结果。

数据工作：原48b775/48b787证明类型22只序列化850人物，扩展至16×850=13600条；10656确认映射、64原扩展字形隔离、2880额外槽位无项目身份，不视作可选人物。补齐文件头版本1/2到stream58/5c，人物全部结果不变，其他段此前错位实验保留。新inspect_pc_scenario_domains原连续循环4496条，额外读取共享Scenario.s11的87名称并按名称/类型/關港后缀唯一映射项目ID；原47b2b0/47c320/483810/65d6c0执行军团→势力归属，不把军团编号当势力。指针系统API根据VM映射校验，不替换规则；前后3MiB域状态全等。compare_pc_scenario_ownership比较434已知归属，五份重建有96差异、190汝南因孔伷字形保留1unknown。

新inspect_pc_scenario_metadata执行原480830/480d50、483120及16384网格对象循环，16份90→16183→16194→17760边界一致，独立证明人物段起点。保存实际名称/日期/说明；Scen015是279年3月滾滾長江，内嵌ID6和Scen006南蠻征伐重复；Scen014英雄集結PK新版。人物5/域5/元数据2项回归通过，pack_pc_data_audit.py确定性归档三份native gzip/概要，docs/pc-data/README.md含完整再现命令及未知项。PC原1805文件2780244569B重新SHA全部无变化(out/parity/pc-integrity-20261003.json)，没有Wine。

真实历史存档：用git archive只读取v31 d352f48与v32 9f496f6对应完整core到独立out验证目录，LegacySaveFixtureWriter分别生成9剧本×4快照共72份原写入器文件（不是改版本头），manifest记录SHA。36份v31各六旬/逐旬save-load/独立完整状态RNG全通过；v32首份central在第1旬分歧，完整继承基线同失败。诊断为旧爵位raw缓存{0=1,1=2,2=2}与实际有效{0=1,1=4,2=3}不一致，write存有效值使直接续玩与存读后续玩产生不同晋爵日志。

已修复Governance.read规范恢复max(既有,已获得)爵位，保留格式与高爵位，不削弱日志/随机断言。真实旧fixture及来源固定于core/src/test/resources/save-v32-central-native.sg11/.provenance.json。verifyLegacyGovernanceSave24及verifyRulerTitles540通过。v32全部36样本六旬复验在冻结fixed-classes继续：exec session72230，out/parity/save-compat-20261003/v32-fixed-all.log，当前已到coalition，尚不能宣称全部通过。年龄APK是此修复前的冻结生产版本；修复/后续预览/最终UI集成必须新构建安装，不能继承旧APK验收。

UI会话按用户要求送来其docs/uiux/RULE_UI_CONTRACT.md（已读），优先请求完整只读出征预览，与正式deploy共用校验、错误优先级、成本、范围/保留量、出城与部队/后勤结果，session/revision及不改World/RNG/reports/事件；后续建设工期、运输摘要。当前Army.deploymentPreview仅出城验证；World.cityError字符串及Army.deploy有唯一真实校验，可提取稳定typed failure而不能UI复刻。此接口尚未实现，是下一实现任务。GameApi目前仅typed recruit/patrol/contest，GameSession持authority负责stale/busy校验和提交；完整UI仍在独立分支推进，不要直接覆盖其目录或共用5580。只接入其已完成的三行测试修复，未发回跨会话消息。

详细范围docs/validation/parity-20261003/README.md；规则全范围、完整官方剧本导入、MOD实际生效、库存/军队/设施/关系/特技、全部正常流程/UI最终集成/ARM与性能仍未完成。目标保持active，不标complete/blocked。

### 2026-10-03 玩法/数据接续第三阶段（active，出征接口完成主机验证）

保持branch agent/native-pc-visual与完整脏树，本阶段未写app或UI独立目录。RuleFailure稳定code/field/detail、World.cityFailure和Army.prepareDeployment将原校验顺序/消息提取为唯一实现；DeploymentPlan只读计算真实约束/成本/出城/移动/编队攻防/当前粮耗。GameApi新增不可变DeploymentCommand、DeploymentPreview及preview/execute重载，GameSession在隔离World查询，正式提交重新调用真实deploy；失败和陈旧token不改权威/save/RNG，成功一次revision和DEPLOYED事实。CommandResult增加向后兼容reasonCode/field。UI应消费此契约，不再复制公式；原Army.deploymentPreview保留为兼容出城探针，不是完整校验。详见docs/architecture/PARITY_UI_CONTRACT.md。没有冒称现工程规则已获得PC公式证明。

DeploymentPlanTest468、DeploymentSessionTest331共799通过；全9兵装×3舰船的真实命令、库存/AP/成员锁定/出口移动/攻防/后勤、18拒绝路径、存档RNG、陈旧/重复/忙/关闭/线程/输入数组隔离。早期两个新测试夹具失败（剑兵错误设置库存、山地放入城市占地）保留日志，合法修正后断言不减。DeploymentParityProbe.java同源分别对重构前固定完整工作区classes（out/parity/save-compat-20261003/fixed-classes）与当前classes执行39项，成功/拒绝文字和完整存档SHA逐行一致。CityMovement63Test1066通过；CityFootprint55Test“no local around-gate land bypass 潼關”当前与上述基线同异常，未改断言。static架构围栏PASS。PcCampaignFlowProbe扩展实际session出征和行军，9局每局真实巡察/出征/行军/6旬/逐旬保存加载513检查PASS。证据out/parity/deployment-20261003。

36份v32旧写入器文件全量六旬复验已结束792检查PASS，连同先前v31全36份，72个真实历史档均通过；此前Governance.read修复保留。原exec72230已经完成，不需重跑。所有正常多回合检查主机证据，不替代安装UI流程。

新APK及test构建成功并冻结out/parity/deployment-20261003/apks：主包32628ef2684ef3efa72db6a7882de128f555bc746cc0839ce7e207ffb034b727（86982085B），测试82c0695f430d872a400f5f0a5149b427d90861d85fbcabdfdae64c288a23e16e（651357B）。包含爵位修复和新接口，但UI仍未消费新入口；**尚未安装**，不能借用年龄APK验证。年龄测试仍在emulator-5554运行，所以未抢占安装。

archive_pc_visual_snapshot.py schema2现在将out/pc-native-runtime/jniLibs/{arm64-v8a,x86_64}/{libpc_effect_worker.so,libunicorn.so}四个明确构建输入纳入相对原路径（不会复制任意out缓存）。checkpoint-20261003-c/source与manifest为3564文件226187437B。用ANDROID_HOME指向本地已有SDK、Java17从该独立完整快照实际构建app，53任务全执行成功；重建APK除META-INF/version-control-info.textproto（没有.git快照故NO_SUPPORTED_VCS_FOUND）之外所有ZIP entry内容逐个SHA全等，包含DEX/资源/native。两个APK文件SHA不同，apk-reproduction.json已明确记录，不冒称bit-identical。UI可复核新接口增量out/parity/deployment-20261003/ui-contract.patch和ui-contract-files.json，仅11核心/API/runtime文件，相对checkpoint-b，不包含app，不是自动覆盖授权。本会话未向UI发跨会话消息。

年龄12例exec25430仍运行，截至最近完整result为9例通过：刘备/关羽/张飞/赵云各年轻年长，加诸葛亮年轻；诸葛亮年长正在跑，曹操两例未结案。每例完整外部恢复all_original_files_byte_equal=true，auto168846B SHA82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9。当前执行中的测试有临时测试档，不能中断后忘记finally恢复；最终是否已完成以results.json的final_restoration和进程完成为准，不并发操作5554。赵云年长step1源立绘截图已实际检查。原年龄候选80ca...、testd128...仍是设备上版本；app/build新产物不能代称已安装。

新tools/content/verify_pc_installed_flow.py语法检查通过，尚未运行；用于年龄批次结束后安装冻结新APK并运行已有SceneInstrumentation（真实deploy/move/TurnWork/save/load/生命周期），外部tar保护全部用户files/shared_prefs，读回安装SHA，超时失败finally恢复；不清数据、不操作5580。SceneInstrumentation真实命令经SessionProbe/生产接口，并非全部出征向导点击；表单仍需最终UI集成实际交互。后续优先完成剩余年龄专项、新候选安装与流程、建设工期/运输权威预览，以及继续PC剧本资源/军队/关系/特技语义和正式数据接入。

UI chat 01a0fd3c-4290-74f1-a722-a895696b7424仍active，最近wait cursor 9111b175-826e-4aff-a0a3-2c4ed53978ec:4；其独立目录/5580不写不碰，只读契约可用。目标全范围未完成，保持active；不标complete/blocked。


### UI冻结提交3330ffb交接（顺序集成记录）

以下为UI独立分支追加日志，保留其历史验证范围；不能替代本目录集成后验收。


### 2026-10-03 UI/UX 隔离会话第一批（总目标未完成）

工作目录 `/Users/paopao/workspace/sanguo11-mobile-uiux`，分支 `codex/mobile-uiux`；原项目只读，未在原目录编辑。玩法会话共享基线 out/parity/baseline-20261002/source 的3524文件/205142686字节全部SHA一致；本地继承提交1a968dc。原生运行库4文件另复制且一致。独立Gradle缓存out/uiux/gradle-home、SDK out/toolchain/android-sdk、AVD out/uiux/android-home/avd/san11-uiux，设备emulator-5580；不得误用原会话emulator-5554。

826d22d独立修复继承的PcPresentationsInstrumentation未定义fixture编译错误，传入原expected selector，不降低断言。UI第一批：启动/剧本条目层级、统一弹窗，UiReadTask后台目录/加载/新局准备与取消/重复请求，48dp表格交互、键盘空态、真实清除外部城市搜索，页码内联错误及单次取消，横屏HUD与短屏武将筛选，势力预览尺寸重排。只改app、相应Android测试及文档；504个保护范围文件与基线SHA相同。

实装API29 x86_64独立模拟器run08通过47项真实触控/键盘/排序/滚动/横竖屏/取消/存档目录/暂停恢复检查，完整权威存档/RNG一致；设备auto.sg11 185898B SHA02ddb3d44d98fbebe763a82551b5cb5eb68da6e70b6087568c0a73943bde5d69与模板库178B前后全等。所有失败/修复迭代保留；run06虽44项通过，截图仍裁切，后续增加实际可见范围并修复。run08截图已查看，168 APK原资源/架构/UI模型回归通过。验证APK out/uiux/iteration-01/app-uiux-run08.apk SHA05ad1d4ec739ed4dcc5c010991a1db0bcff4d145d0fe8deadf5320d8dba39030，仅x86_64。编译时HEAD826d22d加完整UI脏树；源码快照manifest才是完整身份。日志/截图/连续录像见docs/uiux/VALIDATION-01.md，视频顺序取样decode_video_frames.swift使用独立out/uiux/swift-cache。

下一步立即继续修改后开局/势力选择/取消/存档读写/设置，然后出征、建设与部队命令、外交和连续回合；需要真实失败/取消/重复点击覆盖。尚无ARM真机结论；API35、安全区多尺寸、长时性能、原生全国远景和继承规则失败未完成。不启动Wine，不扩大美术逆向。需要集成时仅取继承提交后的UI增量，由玩法会话顺序集成。不要把第一批验证描述为全部流程完成。

## 2026-10-03 UI独立分支第二批重启交接

- 工作目录仍 `/Users/paopao/workspace/sanguo11-mobile-uiux`，分支 `codex/mobile-uiux`；原目录只读。共享基线继承提交1a968dc，UI第一批8918feb。
- 第二批：开局确认取消保留势力；返回剧本列表；Activity关闭释放预览；菜单常用项前置与设置；存档动作明确；3D恢复提示每进程一次。
- `docs/uiux/VALIDATION-02.md`：62项实际安装检查通过，含新游戏→存档加载完整状态一致与两个原生地图host退回一个。真实新局后原自动存档已通过正式加载恢复，与测试前SHA完全相同；新增自己测试槽3。
- 验证APK `out/uiux/iteration-02/app-uiux-run04.apk`，SHA56a3ac92610814a176076706b55326f88c3b535732d5c3fd2ce4be7dfd144403；测试包run05。独立emulator-5580，原5554禁止操作。
- 第一批run08录像首次拉取缺moov，已保留错误副本并重新拉取最终文件；`frames-final`实际解码通过。第二批录像已等待录制退出再拉取，run05/frames有实际PTS。
- 后续：先处理实际截图发现的3D预览加载黑底；再推进出征/数量输入、内政/建设、行军/攻击/战法/计略/外交/回合完整触控验证。62项通过不代表上述未验收流程完成。

## 2026-10-03 UI第三批：加载遮罩与上传期间绘制

- `MapHost` 原地图加载占位、48dp取消、地图触摸阻断；原生Surface经既有20帧+pending0+PixelCopy有效像素验证后揭开。取消/关闭/释放清理完整。
- `FilamentMapView` 仅在占位完全遮挡且资源未装完时跳过不必要的场景绘制，仍遵守上传预算、beginFrame准入、全部画质设置；真实提交计数不包含仅上传的帧。
- `UiUxInstrumentation -e suite loading` 16项安装检查通过，含取消、旋转、Home恢复、真实ready、退出资源与完整状态不变。run01只有遮罩时90秒条件失败，run02实测从首条加载日志至ready56.9秒（含旋转/Home）；不能宣称全国远景流畅或ARM性能通过。详见docs/uiux/VALIDATION-03.md。
- APK `out/uiux/iteration-03/app-uiux-run02.apk` SHA72327515ec818fae9c12b080c0db1f19e65ca9f6a6c59a33f1c3ca3dbae405ca；当前测试仍仅独立emulator-5580。
- 权威预览契约docs/uiux/RULE_UI_CONTRACT.md已按用户“交给玩法会话”的授权发送到01a0fd3b-dc7b-7851-a394-b4202bb48779，优先出征完整校验/失败原因/成本，其次建设工期、运输摘要；我方不改规则。
- 下一批：出征编队、数量输入、建设/内政与攻击等主流程实际触屏验证；已有QuantityControl仍使用遮挡式setError、DeployWizard搜索缺清除/空态等可独立处理。

## 2026-10-03 UI第四批：编队、键盘和真实出征

- DeployWizard按实际可见窗口适应键盘/旋转；搜索空态、清除、48dp控件；QuantityControl字段下错误与滚动露出；行内“移除”区分表单“取消”。不增加规则公式。
- 独立emulator-5580上 `UiUxInstrumentation -e suite deploy` run04 **44项通过**：连续三将、空搜索、非法/修正兵力、完整可见输入/错误、横屏、取消无提交、重新开表单无旧选择、快速双击只出征一队、正式加载恢复原状态/RNG。自动存档/模板库SHA仍与初始一致。
- APK和视频/截图路径及哈希见docs/uiux/VALIDATION-04.md；app-uiux-run04.apk为77c104b加本批代码编译。失败run02/03保留，没有将失败标为通过。
- 下一步继续建设/内政、行军/攻击/战法/计略、外交与连续回合实际触控。规则接口已交给玩法会话，尚未在UI分支接入，不直接编辑其core实现。

## 2026-10-03 UI第五批：统一建设表单与实际结果

- 独立目录/分支不变；城市与地块共用BuildPicker，确认取消保留搜索与设施，恢复旋转布局跟踪；搜索48dp、清除、键盘与可见列表高度适配。
- emulator-5580上建设suite run04 **48项通过**，含地图真实选地、取消、中文粘贴、返回、横屏、快速双击只扣费/建设一次、正式读档全状态恢复、核心拒绝铜雀台且零状态变化。
- 交付docs/uiux/VALIDATION-05.md；APK out/uiux/iteration-05/app-uiux-run04.apk，SHA c51c079de154a71a231bf1cd73799a96cad258eafeb7b7e088131193b7287f88，构建基于689af10加本批UI变化。源码/视频/截图归档同目录。存档SHA相同，受保护504文件一致。
- 原规则工期预览仍待玩法接口，不能标记已接入。下一步继续内政命令、行军/攻击/战法/计略、外交与回合交互；全国原生加载仍慢，ARM真机无验证。

## 2026-10-03 UI第六批：消费出征权威接口

- DeployWizard接入玩法checkpoint-20261003-c的DeploymentCommand/DeploymentPreview和typed execute；删除UI上限公式及重复可用性判定，保留纯UI必填提示。QuantityControl保留空区间，弹窗禁用颜色明确，100ms合并输入查询。
- 自己原core/API/runtime不改；344份冻结依赖放out/uiux/contract-20261003-c，构建必须加 `-I out/uiux/contract.init.gradle`。默认旧core无法构建新UI。350项额外输入含原生库已打包iteration-06/build-inputs.tar.gz，详见docs/uiux/CONTRACT_BUILD.md。最终玩法会话有新接口，按通常方式集成。
- emulator-5580出征run05 **57项实际检查通过**，含零兵装/舰船、空范围、typed唯一revision、实际加载恢复；文件SHA一致。APK out/uiux/iteration-06/app-uiux-run05.apk SHA 5b084cb272248e31fd4ffac11fc0eb8eb8eda583b682f8c7435966e44a63b4b7。权威模块799及UI模型等回归通过，504保护文件不变。
- 查询主线程实测108–276ms仍不流畅，已按接口授权向玩法会话交付日志与缓存/异步契约需求；没有自行优化规则。详情附加核心只读查询待后续DTO补足。
- 下一步真实行军、目标取消/错误、回合控制及其他主流程；不能只停留在已验证的出征和建设。

### 2026-10-03 玩法/数据接续第四阶段（active，UI六批已顺序集成）

年龄exec25430已正式终止exit1，不再等待/重启同句柄：12例完成11整例PASS，诸葛亮年长先PASS140源GPU/严格色差/权威RNGsave，但紧接FAIL lifecycle restoration barrier（Activity destroyed 10秒屏障），必须整例FAIL。所有原文件每例和最终restore逐字节全等，原auto168846B SHA82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9，没新增用户文件。失败txt已保留，需独立复验退出路径，不能靠外部force-stop替换此断言。

源数据新增inspect_pc_scenario_tail.py：原492db0构造器/49391c..493b06连续14组循环，每份1967对象；共享Scenario.s11从2310完整读取到47928 EOF，883非空名称记录；16剧本162702→170010 EOF，仅table_7ecd8的84记录共7308B非空，实际魏/吳/蜀等国号和简介（其他字段未猜）。type22/type24单位类1000记录原497270均不消费字节，不能推断后续事件/初始化不产生部队。共享目录实际省/地区、64设施、12兵装、10君主爵位、81官职、100特技、36科技、32战法、32地形、400名号用字、98能力研究。全17份33439对象，完整报告out/parity/scenario-tail-20261003/tables.json（49MB），压缩归档docs/pc-data/scenario-tail-native.json.gz SHA3da18ee79888d66171955e6d9db55968bae29c5e772000732631d4903a5cbe32，解压SHA1fedd777f36f28877a3ab6bd84a0fe7358a8d75307bb9333cb77d20070b8804e。export_pc_shared_catalog.py从原actor+4读取字段边界提取883实际Big5名称（无unknown），来源编号/偏移/记录SHA/serializer均在shared-rule-catalog.json；没有导入运行时或推测placeholder可用。

NativeTailTest3项通过：重用VM shared→scenario→shared完整一致、源战法record交换只改变对应actors、原4951a0→490c90成本getter32项/改actor+30的源字节后返回值跟随/前后3MiBglobals不变。最初大小写glob与Unicorn重用TB跨越emu_stop截止失败保留；修复是原49391c/493b06显式边界停止，不替换规则。源函数4951a0读取actor+0x30：常规12战法与当前值相同；12井阑火矢10、13冲车破碎10、14木兽放射10、15陆投石10、16水火矢10、17猛撞10、18水投石15。可用性消费者5768be/578305/5df171对unit virtual54比较；unit496020读取+1a。当前Army RAM15/FLAME20/STONE20与这些字段不同，**尚未改规则**，下一步继续追原执行扣减/修正因素、生成有出处的规则数据后连同正常命令/随机/存档验证实施。全部反汇编/首失败在scenario-tail-20261003；PC只读、无Wine。

收到UI只读消息：它已消费冻结c的11文件接口，仿真5580每次preview87–239ms（后续108–276ms），要求缓存/详情。不回发跨chat消息（无直接人类发消息授权），按共享契约实现并冻结给它读。GameSession同StateToken私有deploymentQueryWorld只复制一次，所有typed/contest/legacy/turn提交、replace、close invalidateQueries；失败保留revision，busy/stale先检查。UnitFacts补energy/attackRange/equipmentLabel、不可变去重SkillFact列表和TacticFact列表；War/Army抽出真实共用的tacticFormationError，目标校验仍正式命令完成，不能用DTO空error假设任何目标合法。旧构造器兼容，runtime总填新字段。

v2 DeploymentPlan570+DeploymentSession412=982通过，BattleFeedback209、PlotJournal150，主包/test构建。DeploymentQueryBenchmark40变输入与独立全量复制查询一致，完整save/RNG/revision不变；主机first118.702ms，cached median0.515/p95.967/max1.857ms，uncached median58.239/p9582.301/max122.382ms。只主机，Android还需UI实测。证据out/parity/deployment-20261003/{details-build-tests.log,cache-benchmark.log}。v2详情仍反映当前工程规则，PC一致性不冒称。

冻结snapshot-d3572文件228089036B后，顺序集成UI冻结3330ffb的六批完成成果（log还有最早826d22d测试修复）：79个app/docs安全增量按base1a968dc SHA前置检查写入，1PcPresentationsInstrumentation已一致跳过，1progress仅追加UI历史，81文件无冲突。完整preflight/applied JSON记录源base/target/currentSHA，核心/API/runtime没有被UI覆盖，不写UI独立目录，不碰5580。新UI核心依赖仍是本目录最新v2。静态架构、APK/test、982专项、app:verifyPresentation均PASS（49012/451/1136811/1168801/898305/11371/14）。最新UI合并候选out/parity/ui-integration-3330ffb/apks主d24edf6e84bc08548198e7d626270347244ee282ce71659df1082ae2ab8c7866、测试0777eabbf731c8f8895c4ffdfe52a808a0b077e46c68d56be91264b2ee15cbff；**尚未安装**。UI此后新做的行军等在其活跃目录/后续commit，不能覆盖它的WIP。最近wait cursor9111b175-826e-4aff-a0a3-2c4ed53978ec:5，chat active。

当前设备5554验证链：先用带缓存详情的未合UI候选778bb1cf5765f9d3f2f146abd85e36ee43c6a61901af3192b1a5f045b9191d99实装，设备readback全等；其test82c0695...冻结在deployment-20261003/apks-query-details。SceneInstrumentation于203.75秒FAIL site faction flag exists，尚未进入commandFlow，因此未证明deploy/move/turn。外部finally恢复所有6原文件全等，exec64315已结束exit1。源码FilamentMapView1439只有ground.pcMap==null才建legacy Proxy.flag，这与旧测试的无条件flag>0冲突；不能直接删除断言。

已启动真正继承基线APK80ca...及testd128...的同一SceneInstrumentation实装对照：exec **26918**，输出out/parity/deployment-20261003/installed-scene-baseline/ 与 .log，当前仍运行、未结案。它使用verify_pc_installed_flow.py备份/恢复同样全部用户文件，最终必须检查results.json restoration与进程结束，不能同时在5554安装UI合并包。基线结束后若同失败，登记历史测试冲突；然后安装已冻结UI合并候选，使用新UiUxInstrumentation suite=opening/deploy/build真实触控流程。工具verify_pc_installed_flow.py目前runner choices仅SceneInstrumentation/GameSmokeRunner，下一步需增加UiUxInstrumentation；测试opening要求slot3为空，或显式已有test-owned SHA且与当前状态一致；deploy/build要求manual3与当前capture一致，且3闲将、指定零库存。先检查备份和测试夹具，不得覆盖用户槽位/清数据。可以用有明确来源的测试fixture，但真实UI新开局、命令、保存加载必须仍执行，不用直接规则调用代替触控。所有安装结案都外部恢复原auto与其余文件。

后续重点：完成当前基线对照和UI合并APK实装完整流程；复验诸葛亮年长退出；依据新原共享战法成本和设施/科技数据推进真实玩法差异；建设工期/运输权威查询仍待做。所有规则、正式剧本资源/军队/设施/关系特技/MOD链、连续UI回合、ARM/性能目标仍未完成，active不complete/blocked。

### 2026-10-03 第五阶段接续（原战法成本已实施，实装验证继续）

- 完整snapshot-e已成功：3636文件249293937B，保留全部脏树/UI3330ffb与4原生构建库。其后新改动尚待下一快照。
- 原基线Scene对照exec26918已exit1，70.53秒先FAIL native frame loop running；没有到达旗帜断言，不能认定相同历史失败。设备APK80ca...读回一致，原6文件逐字节恢复，无新增。
- 合并UI3330ffb+d24...APK已实际安装，读回SHA一致，UiUx opening（exec78503）463.39秒FAIL visible control timeout@88：实际保存slot3、取消覆盖/读取、触感开关、2×设置通过；再次触摸设置后未找到演示默认速度。03-settings/FAIL截图实际仍菜单，不能计新开局/完整加载通过。原6文件最终恢复全等；新增仅测试实际保存的manual3.sg11，保留不删。证据out/parity/ui-integration-3330ffb/installed-opening，包括截图/evidence、logcat、两份tar。无清除模拟器数据。
- 原成本链终于确认：5a31d0调用所属势力查询后原样读取actor+30，没有成本修正分支；58589c..5858b7/5861e1..5861f8取负调用4964f0，后者clamp0..100/120。NativeTailTest第四项实际执行前一扣减块，32项×3能量上下界，整个3MiB只有unit+1a改变；中性无所属单位，未执行完整命中/目标规则，明确证据边界。4项Python21.232秒PASS。
- 新export_pc_tactic_costs.py按原报告/source字节生成PcTacticCosts.java及docs/pc-data/tactic-costs-native.json，重新生成逐字节一致。Army RAM/FLAME/陆STONE=10、水STONE=15，fire10；tacticCost(Unit,Tactic)共用于合法性、正常扣减、preview、消息、AI、DeploymentPlan.TacticFact。步骑弩12种成本校验原值一致。没有改app文件；ArmyUi和DeployWizard的Army.Tactic.energy上下文显示待UI按PARITY_UI_CONTRACT补齐，陆基energy字段兼容保留，不能拿它显示海上投石。
- 新PcTacticCostTest193PASS：七兵器/水军场景，低于/恰好/100气力，合法拒绝全save/RNG不变，真实正常命令、miss一随机抽样、唯一扣费、本旬行动、重复拒绝、读写与下一旬完整确定性。DeploymentPlan570+Session412仍PASS。9剧本正常Session巡察/typed出征/移动/6旬/reload513PASS，证据scenario-tail-20261003/campaign.log。
- 扩大回归Army/CampaignAi/Displacement三失败，与baseline-core-validation/results.log相同首断言（非法据点入口、adjacent resupply、city deterministic replay），原断言未改。BattleFeedback209/PlotJournal150PASS；APK/test任务完成，但合并Gradle命令由于三历史失败exit1，不称全绿。DisplacementSaveProbe进一步定位city旧fixture grades={}而重载有效{1=1}，正常战法success中的治理reconcile多一条晋升日志；纯fixture/未规范治理状态的save-before行为有缺陷待修，不擅自削弱测试或冒称已解决。
- 当前已冻结新规则候选scenario-tail-20261003/apks主86993345B SHA9b1a7262da198e7745d6634132aed0f8e27bd713d6d179ee975a401c9c9cb246；test0777eabb...，UI仍3330ffb。已启动**exec64772** verify_pc_installed_flow suite=deploy/run=source-costs-deploy，目录scenario-tail-20261003/installed-deploy；备份包括新manual3与原auto，必须等finally恢复结果再做5554其他安装。
- 下一数据规则候选建设：原facility table79c54，+c0/+c2/+c4读出，原5bb1d0按三名执行者4890a0(actor+173)计算值，5bb2e0用3/4*c2除它求回合数，5bb2b0返回c4；49db10存在特定设施0x29同势力的80%分支，5bc470是负费用分支。尚未给字段全部赋游戏语义、未导入Domestic。反汇编facility-*.asm和post-tail.asm在scenario-tail-20261003。原493b06后仍有unit后处理、150/50记录、其他serializer/地图、初始化，因此不能用type22单位记录零读取推断新局绝无部队。

## 2026-10-03 UI第七批：行军/回合实际交互与战报

- 独立UI目录不变，提交仅app UI/测试与文档。BattleReportUi修复键盘空结果裁剪，增加清空、可见高度适配和详情返回跟踪。
- emulator-5580行军suite run05 **64项通过**：实际出征、非法/合法地图点、取消/返回、双击移动唯一revision、下一旬双击、暂停/恢复/速度/跳过不重做权威规则、战报搜索及横屏详情、正式读档全部恢复。失败run01–04和恢复记录保留。
- APK及完整证据见docs/uiux/VALIDATION-07.md。已安装验证universal包SHA ce91af46c05e6bf352a949125fd25e7f6adbe75957500e1f763312591946eea1；ARM64包SHA d15cf38cf1402d1d3db07f52dbff274e1e09b74e86e89b70984bf30e0afee37b仅构建/库资源核验，无ARM实际运行。保留universal原32位打包行为，不宣称32位PC worker可用。
- 自动档/手动3/武将库SHA相同，504保护文件、344冻结构建输入不变。仍必须使用contract.init.gradle；新runner只针对独立AVD，失败用真实读档恢复。
- 完整目标未完成，下一步攻击/战法/计略、外交、其他内政、地图手势及长时验证；同步权威出征预览停顿和全国原生加载仍未解决。

第五阶段即时补充：exec64772出征验证199.17秒FAIL@UiUxInstrumentation128（搜索空态不可见）。APK9b1a读回一致，原7文件全恢复，未新增。FAIL截图真实显示1080×1920 density420键盘将出征表单列表挤没，不能算正常出征命令验证成功；app修复留UI负责，已记录共享契约。原4Python成本+2Python建设计算均PASS；snapshot-f3641文件249319685B完成。收到UI79cdc6b已完成批次与外交预览新需求，未回复跨chat。新增integrate_ui_snapshot.py只读冻结提交、逐项SHA预检、冲突全停、progress追加，12文件顺序集成79cdc6b已成功，out/parity/ui-integration-79cdc6b/preflight/applied。build.log76任务24秒SUCCESS，未触碰UI外交WIP。新合并APK/test已冻结该目录apks，开始suite=march真实出征/行军/回合，exec73806，必须等finally恢复再对5554安装。

第五阶段停止点/自动续行交接：当前唯一设备测试**exec73806**（wrapper进程，工具输出session_id），APK71cbc760b00f435d333fe2dc76d282d749327ec84f5659d35bb676a26fca84b3已成功安装并设备读回一致，test4d3ba1c27b62ae3dfbca87c66409323dcb76d93754ecde8abf5db3411369ae84。suite=march/run=merged-march，输出out/parity/ui-integration-79cdc6b/installed-march/及.log；目前已确认正常载入和manual3匹配，尚未整体结案，**先wait73806再读取results.json restoration，不要在5554并发安装**。此包包含native cost与UI79cdc6b，不能沿用3330ffb不同包结果。用户原autoSHA82554269...必须保持；原manual3为此前真实UI测试创建，现在也纳入备份保护。新wrapper会在安装readback后即写report，所以passed:false且没restoration只是进行中，不是最终失败。

未来重点：结束本次march后收证据/恢复；按键盘小高度失败证据交给UI通过共享文件消费，ArmyUi上下文投石成本同样待接；独立复验诸葛亮年长生命周期；继续建设成本/进度原执行链，已完成512原函数算术边界但不猜完整语义；外交typed预览请求已登记，尚未新增API。不改活跃UI源树，不向其他chat发消息，不用其成功APK抵消本会话失败。`tools/content/integrate_ui_snapshot.py`默认只预检，--apply要求所有文件baseSHA匹配，否则全停；只接完成提交。最近已接79cdc6b，下一UI提交差分base应为79cdc6b，不能再以3330ffb覆盖。

## 2026-10-03 第六阶段：外交接口与安装回归

- exec73806已结束exit1，71cbc760包march240.17秒FAIL@168真实出征无部队。FAIL截图仍未选兵种、确认禁用，未到行军和回合。原7文件外部恢复全等，无新增，不沿用UI5580结果。
- 同包诸葛亮年长exec9190已结束exit1，305.76秒：131项源GPU/严格背景/权威RNG/save通过，随后仍FAIL lifecycle restoration barrier。原7文件每例和最终恢复全等，无新增，autoSHA82554269…f0a9不变。输出ui-integration-79cdc6b/age-zhugeliang-old。5554空闲。
- 原建设第三项验证3 tests/6.146秒PASS：真实建筑构造和5bc462..5bc49b经原虚表将+c4从city+44扣除，下限0，11设施×3金额，全3MiB只预期字段改变。未将Lv1源成本套用现有Lv3建设，Domestic尚未修改。
- 外交新增DiplomacyPlan、DiplomacyCommand/Preview、DiplomacyQuery，GameApi preview/execute，覆盖GOODWILL/CEASEFIRE/ALLIANCE/BREAK_TREATY。Campaign/Envoys纯失败检查与普通公共命令共用；费用、旅途、期限、当前概率效果来自core。已有版本缓存扩展ruleQueryWorld，两类查询共享，提交仍新候选、一次revision。DIPLOMACY_DISPATCHED仅出发，TREATY_BROKEN即时解约；契约已记录，没有修改app。
- 新核心165、runtime468、出征runtime412通过。覆盖完整存档纯度、真实出发/抵达/返程、恰好一次接受随机、费用不重扣、重复/陈旧/忙/线程/替换拒绝、72完整旬保存确定性。初次测试错用randomState()编译失败，改为既有getRandomState()后通过，首日志保留。
- 重构前完整工作区classes先录44命令+170续旬；同一DiplomacyParityProbe在最终classes的214行文字/完整save SHA全等。旧DiplomacyTest非法设施位置、GameSessionTest旧剧本初态失败均与完整继承基线相同，原断言保留。架构PASS，独立APK/test构建79任务23秒SUCCESS；没有将包含历史失败的Gradle命令写成全绿。外交PC公式和不可达路线max(1,-1)缺陷仍待处理。
- 完整快照h正在冻结上述core/API/runtime成果；收到UI完成提交9d45db9、67057ff，接下来按79cdc6b→67057ff预检顺序集成，再构建安装。UI战斗WIP不在本批，不写其独立目录，不向其他chat发消息。快照h开始时尚未包含本段交接，后续新快照补齐。

第六阶段补充：snapshot-h已完成3657文件251313643B，含4原生库。随后已预检并顺序集成UI9d45db9/67057ff的30文件，applied报告在ui-integration-67057ff/applied。完整外交契约仍等待UI消费；出征v2和上下文战法显示已由UI此批接入。83任务25秒build SUCCESS，165/468/412专项通过。冻结APK86998457B SHA02497975c8ca0ec254743cfd41b85d77f0e725346b43bc4f5b18e8bd76f0b9fb，test671133B SHA1265198234ac12ed711dcfe6e2f23956b4520d794a5d1035aa163e41fb995aec。

首装exec97186已结束exit1：installed-deploy124.85秒FAIL@125，定位成都后未打开出征面板；设备读回APK一致，7原文件恢复全等，无新增。FAIL.png已查看，截图仍全国图和底部“成都·指令”。日志大量SurfaceFlinger driver stall，尚不能唯一归因。已adb emu kill并原参数重启本会话san11-pc-map/5554（host GPU、2048MB、2cores、不清数据），重新root后全部7文件与前次恢复tar字节一致。5554新PID25796启动记录restart/emulator.log；UI5580没有操作。**当前仅exec2280**运行同APK同测试suite=deploy/run=merged-670-deploy-restarted，输出installed-deploy-restarted；先等最终restoration再在5554做其他安装。

PcDiplomacyFlowProbe在9现有真实剧本通过367检查：按真实允许目的地出使、真实完整旬结算、每旬新Session存档恢复、完整RNG字节匹配。均找到2旬往返，未以夹具覆盖生产数据；输出diplomacy-20261003/catalog.log及catalog-saves。测试证明现有引擎一致性，不声明PC外交对齐。

原建设新证据：600e15设菜单表8ba678和20行；600e25读取每行pointer-8的ID并调用原490b90。6011bf步长20，源表8ba670。实际20菜单ID31–39、30、40–49，排除50–59高级设施；因此普通菜单确实从Lv1建设，之前通用许可函数遍历64不能代表菜单可选项。新增第四测试执行原初始化与20次查表，全3MiB无写；4 tests5.211秒PASS。反汇编construction-option-callers.asm/construction-menu.asm保留。下一步应生成基础设施费用映射并修复Domestic.buildLevel满级捷径，保护旧存档已有等级，逐步核实进度/耐久/升级，不能把未完成原流程冒充全对齐。

重启后复测exec2280已结束exit1：在adb install主包阶段超过wrapper原60秒超时，游戏instrumentation根本没有启动。不是出征规则失败；finally仍恢复7原文件全等，无新增。installed-deploy-restarted.log保留完整异常。verify_pc_installed_flow.py已将安装与86MB APK回读单独设180秒（其他ADB查询仍60秒），逐个install立即落盘日志，results新增stage/exception；玩法断言与900秒测试时限未改。当前唯一运行**exec82021**：同02497975 APK、12651982测试包、UiUxInstrumentation suite=deploy/run=merged-670-deploy-cold，输出ui-integration-67057ff/installed-deploy-cold。后续先等待82021与finally恢复，禁止5554并发安装。下一完整快照i会包含新接口、UI67057ff、上述工具/证据文档更新，h仍保持原历史内容。

## 2026-10-03 UI第八批：外交与通用选择

- ChoiceDialog空态/清空/48dp搜索/键盘布局；DataTable命令选择动态高度与默认能力列；外交分步返回，Campaign/Diplomacy确认接入统一局面校验，移除亲善重复公式。
- 外交run04 **56项实际交互通过**，包括核心拒绝同盟无完整状态变化、双击只派一名使者及一个revision、任务列表与正式读档恢复。APK SHA 0c9afc3cce21d43e8038753e07c5caa1b38a26234f8a4b3c69af191a80e0922d，详见docs/uiux/VALIDATION-08.md；原始失败与恢复保留。
- 规则/API/root data保持隔离，构建仍C。外交完整预览契约已交接，尚未实现/消费；不能称全部外交验收。
- 刚读取玩法共享契约：v2缓存/完整详情已冻结D，战法tacticCost在F/G；G最新快照含79cdc6b集成。下一批可只复制G的core/API/runtime到自己的out并验证manifest，不能覆盖root源码。仍需新init脚本与独立构建输入档。
- 集成方有真实失败：250成都出征键盘下名单空态被挤掉，原图原目录out/parity/scenario-tail-20261003/installed-deploy/evidence/FAIL.png；opening设置二次点击也曾失败。已读取证据，不操作5554。优先复现/修复，之后继续已有DisplacementFixture战斗场景的实际UI攻击/战法/计略（用SessionProbe.install经真实host安装fixture，严禁GameSmokeRunner旧反射world替换）。

## 2026-10-03 UI第九批：出征v2与集成布局回归

- G规则快照346文件SHA验证后只放out/uiux/contract-20261003-g。当前构建必须 `-I out/uiux/contract-v2.init.gradle`，旧contract.init.gradle仅用于前六至八批。352项可重现输入iteration-09/build-inputs.tar.gz已封存。
- DeployWizard移除旧Unit补充查询，完整使用UnitFacts；ArmyUi/WarUi用上下文tacticCost/formationError。紧凑编队页收起大说明，实际250刘备成都键盘回归25项通过。
- v2出征62项、同最终APK的opening62项通过；主机570+412=982。首次预览92ms，后续12次0–4ms，不能称首次/所有性能通过。root保护504与G346文件SHA一致，所有测试存档字节恢复。
- 最终x86 APK iteration-09/app-uiux-run03.apk SHA aabc60f5269e32d14051fb8be3845f4c4ea869a557a6847dde4a6f9586c58418；ARM64包SHA db789a1edf26bd0fb535ffabe07298d980fc6ada44e61ddd77ba4a6760405b80仅构建/打包核验。详见docs/uiux/VALIDATION-09.md，所有记录/视频在iteration-09。
- 下一步复用现有DisplacementFixture做真实攻击/战法/计略按钮验证，安装场景必须SessionProbe.install走真实host，禁止旧GameSmokeRunner反射world。继续内政/运输/高级外交/手势/长时/性能；全目标仍在进行。原5554opening失败仍由玩法会话新合并后重验，不能用5580通过抹除。

## 2026-10-03 第七阶段：原基础设施规则与权威建设接口

- 上一exec82021结束exit1：同02497975包冷启动出征127.11秒FAIL于第二位武将deploy.role.1002不可见。新键盘完整可见断言已通过，首选武将已触摸；截图名单滑到底部，未完成出征。所有7原文件最终恢复全等、无新增、autoSHA82554269…f0a9。证据ui-integration-67057ff/installed-deploy-cold；5554现在没有测试运行。没有把触控失败认作核心出征成功。
- 根据已执行的原20菜单与真实扣费链，新增export_pc_facility_costs.py，生成PcFacilityCosts.java和facility-costs-native.json。核对EXE/实际共享文件/记录字节及SHA、原+c4字段与11个项目Kind的繁体名称。64费用全记录，当前11类普通内政设施消费基础费用；Java/JSON重复生成全等（construction-20261003/reproduction）。
- 普通build现在Lv1，市场/农场200金，兵舍/锻冶/厩舍/工房/船厂300，造币/谷仓400，黑市50，铜雀1500。移除直接Lv3捷径；存档版本33、枚举顺序不变。facilityEffect(f)区分旧档既有等级与新建基础效果。工期/AP/耐久/产出仍为原工程参数待核实，原三执行者/进度等尚未完成。
- 修改前classes先运行PcConstructionSaveProbe，通过真实market建设/两个正常旬/farm建设写出pre-base-construction-v33.sg11及provenance，非改版本头；已完工市场与在建农场都是旧Lv3。新471核心检查证明原字节往返不变、原等级保留、原在建正常完工无降级、取消不按新费用退款。11设施×不足/恰好/充足金币，正常建设/重复拒绝/RNG/3旬save-reload均通过。
- 新ConstructionPlan、ConstructionCommand/Preview、ConstructionQuery及GameApi preview/execute；与普通build共享纯校验与工期计算，使用同StateToken私有查询缓存，正式候选执行一次、CONSTRUCTION_STARTED一次。宿主197检查通过，外交468/出征412继续通过，架构PASS。完整构建84任务37秒成功，随后仅补了facilityEffect与相应核心断言，最终APK需再构建。契约已记录，BuildPicker仍有继承工期/AP推断，DomesticUi仍有旧满级文字且没有合并入口，留UI按新接口修复，未直接改app。
- 新测试初次缺Treasures.definition IOException声明导致编译失败，已补声明，保留tests.log。扩大回归：CampaignTest非法设施位置与完整继承基线一致；DomesticTest先在“999金不足1000”旧成本断言变为失败，这是本次有来源的费用变化，不伪称相同历史失败。仅将该金币边界改为当前MARKET.cost-1，保留拒绝与全存档原子断言，再复跑核对后续历史失败；未删除其他断言。
- UI已送达完成faf390d（战斗/海战/行军，后于67057ff），其独立结果不是本包证明。接下来先冻结本core/API/runtime完整snapshot-j供UI读取建设接口，再预检67057ff→faf390d，顺序集成23文件；不触碰UI运输/内政WIP或5580，不发跨chat消息。随后新包实装真实combat/naval；完整游戏流程、年龄退出、ARM仍未完成。

## 2026-10-03 UI第十批：战斗流程与重复触摸

- 独立目录/冻结G依赖不变。真实测试发现战法连击第二下落入新战果条；成功命令布局切换后300ms阻止新重复手势，原world/revision/一次提交保护保留。战法可用性整行显示权威原因；选目标时隐藏两条快捷导航释放96dp地图，取消恢复。
- 最终x86包combat66、naval18、march64通过。测试场景来自既有DisplacementFixture，经SessionProbe.install/真实host布置；沙地和斗舰为布置变体，所有命令用屏幕触控，不等于全国剧本/战法组合全部验收。run01实际误触失败与真实读档恢复均保留。
- docs/uiux/VALIDATION-10.md及evidence-10；x86 iteration-10/app-uiux-run03.apk SHA bc436c9a7ad73ecd76471a0f7b05fbc3f32b7ee18f684b8982f9655944718c98；ARM64 SHA 98faac17d4c00c56b7c9dc52fa231853ef5dbcbbfa88a927eaae878baec861db仅构建/ABI/native库/资源验证，未运行。最终两包168资源通过，root504/G346完全一致，三份存档/库SHA恢复。
- 源码快照iteration-10/source配第九批352项build-inputs.tar.gz；仍用contract-v2.init.gradle。录像run03战斗、run04水军、run05行军已记录，run02首次视频取帧超时点失败仅是解码请求越界，后续有效时点成功；原视频/错误保留。
- 后续立即继续运输/其他内政与高级外交、地图多指手势/长时生命周期及首次查询/全国加载性能。DomesticUi仍有继承的ration近似、容量硬编码和副将/主将嵌套选择，下一批优先；完整运输DTO已在RULE_UI_CONTRACT.md请求，不能UI复制规则。全目标未完成，不操作5554，由玩法会话最终集成。

## 2026-10-03 第八阶段：建设20AP与战斗批集成

已将67057ff→faf390d冻结23文件全量SHA预检后顺序合入，未取UI下一批WIP。nextbase=faf390d84d2abb4abc6a4bb14867d04afb7bc8a2。原生建设AP测试先暴露夹具错误：district+4无有效势力，且city+38只写一字节留下ffffff00；失败日志保留，没有替换校验函数。补完整32位军团索引与有效势力后，5项原程序测试PASS，含6AP边界在真实5bc4b5→5b9340→4a1820→47e3e0链上只写district+2c。

生成器schema2核验5bc4b7指令6a14，建设正式校验/预览/扣费改为20AP，其他城市命令未批量改变。489核心、197建设宿主、165外交预览、468外交宿主、570出征预览、412出征宿主通过；43秒86任务构建成功，168资源检查通过。生成工具Java/审计JSON重复输出全等。軍团独立AP、三执行者/工期/耐久/产出仍缺；UI固定AP10须消费新DTO，契约已更新。DomesticTest金币边界改为cost-1后，下一失败unfinished market gives no yield与继承基线一致，未削弱该断言。

完整快照K：3706文件259862671字节，包括全部继承成果、四native库、faf UI和AP20；不含本段后续记录。out/parity/checkpoint-20261003-k/manifest.json逐文件SHA可供UI继承。APK已封存ui-integration-faf390d/apks：主包87000985字节SHA f6e91e2cde370ed368b0187cf5a0c97528f38d02f260d1e13c6cfb5354e1d551；测试包677797字节SHA5cc25b07e93d957efb7e707c11d5707503d0bfb8f47794ff1d4c4cc435cfc8d2。

第一次封存脚本因内联文本编码SyntaxError未执行，随后wrapper读取不存在APK而退出，未安装或跑游戏；installed-combat只保存原备份。已实际封存APK，wrapper新增APK存在/哈希预检于设备操作之前。当前唯一5554测试exec45598：installed-combat-ready，实际新包combat。完成前禁止同设备安装；必须检查finally恢复7原文件、无新增、auto SHA82554269…f0a9。后续再naval，不能引用UI5580结果替代。

### 第八阶段后续：运输v1、真实存档修复和K包实际失败

exec45598已结束：K包f6e91e2实装回读一致，combat131.62秒FAIL于83行点击空格5,5后等待目标错误控件超时。5项通过，无实际攻击。随后同包naval83.04秒FAIL于76行：选择投石显示15、横屏滚动与取消可见通过，但点击6,6没有建立tacticPreview；截图仍选目标状态。证据分别ui-integration-faf390d/installed-combat-ready/evidence与installed-naval/evidence。二者均7原用户文件恢复全等、无新增、auto82554269…f0a9。用户包名是game.sanguo.mobile.dev，之前pull错无.dev的目录失败已保留。不能引用5580替代这两个真实失败。

新增TransportPlan、TransportCommand/Preview、TransportQuery、GameApi preview/execute和TRANSPORT_DISPATCHED。预览与普通dispatchFailure共用纯检查，副将/载货/舰船错误保持原优先级；14种库存/携带限额/目的地容量、AP、实际离城坐标与消耗、真实foodUse/eta来自核心，输入数组防修改。预测仅对当前路线/耗粮成立，不保证未来抵达；满仓仍允许派遣并等待。契约已完整记录，K不含该接口，后续L供UI消费。

同时修复真实继承缺陷：带舰船transport在success之后才扣库存/设置cargoShips，导致报告基线遗漏，直接下一旬把船扣错记到敌军任官，读档后的下一旬没有此变化。用改动前完整classes（不是旧HEAD）运行PcTransportCompatibilityProbe原始/读档规范化两种模式都失败；调试两档除了报告压缩块没有游戏状态差异。现所有舰船变更在同一次success前完成，未改存档版本、未改旧历史报告、未重扣旧任务。原始24结果+30完整旬54行现通过全部字节/RNG断言；无舰船的24结果+48旬72行与改动前逐字节全等，SHA49137362…6838。出错日志、before-classes、debug两档及解压报告差异保留out/parity/transport-20261003。

254运输核心、88运输宿主、489建设核心、197建设宿主、468外交宿主、412出征宿主通过；架构PASS、168资源PASS，86任务34秒构建成功。新包transport-20261003/apks/app-debug.apk87004637字节SHA ed967310250d4de8a19151f89727ee3f9a738ca1cced6b17e43d63bfe90b0711；测试包仍5cc25b07…fc8d2。该包尚未安装，不能沿用K包实装证据。初次新测试编译出现nextMissionId所属类/STALE_WORLD枚举名错误已改正并保留原logs；完整运输断言未削弱。

## 2026-10-03 UI第十一批：运输表单和抵达返程

- 独立UI目录/冻结G不变；新增CargoWizard抽取原DomesticUi运输草稿，三页编队/钱粮/兵装，统一搜索选将与去重、字段错误、返回修改/Home保留输入。删除旧ration近似和容量硬编码；输入标明出发城库存，合法性/费用/路线/耗粮/目的容量由核心预览给出。没有规则代码改动。
- 新类为既有legacy UI职责抽取，精确加入docs/architecture/LEGACY_CORE_ALLOWLIST.json，并在MODULE_RULES的UI11段审查说明。没有扩到目录或修改架构检查器。最终命令仍commandDialog+applyResult惰性提交；DataTable只加可选关闭回调，QuantityControl只加显示说明。
- 同一x86 APK：transport run02 64项；cargoTravel run03 43项真实钱粮入库与返程；run04 51项再加100枪装备、长列表末尾零库存舰船输入/修正，装备恰好入库一次。正常190曹操存档，不是场景fixture；两次真实回合完成短途卸货与人员返程。run01仅测试selector空contentDescription错误，原记录/读档恢复保留。
- 验证docs/uiux/VALIDATION-11.md；x86 out/uiux/iteration-11/app-uiux-run02.apk SHA 851fdd4845c77ba4b0fc946dc0e4cdb671d45995499c916cce4b65bce98c2f0f；ARM64 SHA 0a2ec57bde78a5142a45dab4c112b0d6c7ea13a3a8bf328c06fbb7724d4bee9c仅构建/ABI/资源/库核验无实跑。root504/G346一致，auto/manual3/库SHA恢复。源码归档iteration-11/source配iteration-09的352项build-inputs；使用contract-v2.init.gradle。
- 下一步其余内政（征兵/巡察/训练/生产等）、建设新规则接口接入、高级外交、地图多指/长时生命周期与性能。运输海运/改道/截击/满仓组合、进程销毁草稿恢复仍未验证；结构化容量DTO仍待玩法。全目标未完成，不操作5554。

## 2026-10-03 第九阶段：真实运输剧本与UI11集成

玩法冻结L已就绪：3713文件259893327字节，out/parity/checkpoint-20261003-l/{source,manifest.json}，包含建设20AP、运输v1、舰船原子提交修复、所有继承资源及四native库。UI可按manifest仅继承core/API/runtime到独立out；已在共享PARITY_UI_CONTRACT明确路径与输入输出，未向另一会话发消息。L后新增PcTransportFlowProbe通过9实际项目剧本217检查：不改变真实初始库存，各选合法短途运输并带一艘楼船，正常6旬、逐旬重开Session与直接执行全字节/RNG比对。9结果档和日志在transport-20261003/catalog-saves、catalog.log；首预览2.9–43.4ms仅这些路线。

L包ed967310已在5554安装且回读SHA一致。MapTap57诊断下naval121.14秒仍FAIL，选择投石15气和横屏可见检查通过，点6,6未建立tacticPreview。已有触点日志无回调；需继续核实活动renderer/手势/事件派发，不宣称唯一原因。截图已取回installed-naval-traced/evidence。7原用户文件再次全等恢复、无新增、auto82554269…f0a9；log.tag.MapTap57由空→DEBUG→空恢复，map-trace-property.json证明。没有旧naval运行。

UI11已按faf390d→4c23785b5bf9f0f67387cd0cec907fabdb7f38f2顺序集成34文件。单独审查CargoWizard新路径及MODULE_RULES追加，工具--include-architecture-review仅允许白名单只增不减的精确Java路径和评审段落追加，保留root已有内容。新增5个集成工具测试通过：冲突整批不写、只读冻结blob不读WIP、保留本地架构增加、拒绝删除/通配符、预检不改项目、重复追加不重复。架构与diff检查通过，运输254+88再次通过；新包82任务21秒构建成功。

最新封存APK：ui-integration-4c23785/apks/app-debug.apk87005433字节SHA fb6bcd55ec0a83ffb85b9254cb26861b684dd71b8ed8e2c106a136dbf8bcaa03；测试包682885字节SHA266f8c7aa28d2d5902aa9293b4d378c196cbba88b2428c8631bdf7a85831e5c9。5554唯一正在运行exec55679，installed-transport，suite=transport/run=merged-4c-transport；已安装回读fb6bcd55一致，玩法结果待结束，必须等finally7文件恢复后再安装。不要触碰5580。主包包含arm64-v8a和x86_64两套worker/unicorn，ARM仅打包证据。

下一集成base=4c23785。完整快照M保存上述源码、工具及UI11，不从旧HEAD取基线。建设/运输typed接口的UI消费尚未完成，新建满级旧文字/合并入口待UI修复；实际地图触控与年龄退出失败仍开放，PC规则/16剧本完整转换未完成。继续原goal，不complete、不blocked。

M已完成：3740文件264084417字节，manifest全部文件重新读出SHA校验零差异。其后补了真实旧运输档回归：PcTransportSaveProbe必须运行before-classes，且先断言旧引擎直接/读档后下一旬确实分歧，防止误用新writer重生成。生成pre-atomic-ship-cargo-v33.sg11为4815字节SHA c23d7aaf0be1dc0637b62ebfa39ed27562ff8d635d771bc032992ed5799e617b，配provenance及K源Domestic SHA。新引擎原字节往返全等、舰船库存与货物不重扣、原历史战报不重写，未来6旬save/RNG一致，TransportPlanTest增至269项通过。只补主机测试/资源，生产代码和已封存fb6bcd55 APK未变。下一N完整快照将包含这3新增文件和测试增量。

截至本次更新exec55679仍在5554运输UI测试：真实250成都→綿竹關，正常读档、目的地选择、主将搜索键盘空态、选将、副将取消返回/去重已通过，正在后续选择；不能提前报运输派出成功。最后日志位于ui-integration-4c23785/installed-transport，进度可读设备/storage/emulated/0/Android/data/game.sanguo.mobile.dev/files/uiux/merged-4c-transport/progress.txt。必须等wrapper结束并核对restoration，再做其他5554操作。

UI新请求CityActionPreview已收到，尚未实现：state/operation/city/actor/可选target；金/AP、治安/气力/兵力/兵源前后、忙碌旬、人员状态影响；搜索只给概率/范围，不提前随机。下一步先核对真实Strategy/Government入口并抽取纯校验，不在UI复制巡察/训练/太守公式。UI另一工作区仍活跃，不修改其文件，下一完成提交仍从4c23785顺序集成。

第九阶段最终设备状态：exec55679已结束，fb6bcd55当前合并包运输244.12秒FAIL于UiUxInstrumentation85/331：点击第二副将按钮后等待DataTable可见超时，尚未完成派遣。已完成主将/副将1搜索取消等，不冒称完整运输通过。installed-transport/results.json证明7原用户文件全部恢复字节一致、无新增、auto82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9；截图与进度已取回同目录evidence。5554现在没有测试运行。最新N快照3743文件264093420字节已完成，保存完整代码/测试/工具/资源；其manifest“运输运行中”是创建时历史状态，本段为最终结果。后续仅补验证文档，APK生产输入未变。所有goal目标仍继续，核心进展与UI实际失败分别记录。

### 2026-10-03 城市命令预览与执行统一（checkpoint O 准备）

- 保留全部完整工作区与UI4c23785，未改UI源码/测试。新增CityActionPlan/Command/Preview/Query及GameApi/GameSession六类城市操作：巡察、训练、征兵、搜索、批量褒奖、任命太守。普通命令共享纯校验与效果计算；typed执行真正调用普通命令，一次权威提交/事件。详细输入/错误/资源/搜索条件概率与视觉边界见PARITY_UI_CONTRACT最新节。
- 改动前完整core classes冻结于out/parity/city-actions-20261003/before-classes。PcCityActionCompatibilityProbe：72命令+82后续正常旬结果共154行，before-valid.txt与after.txt字节全等，包含完整save SHA和RNG；初次非法pending builder测试夹具日志保留，补合法builder后重新建立对照，未更改实际规则。
- 核心CityActionPlanTest 1725检查PASS（含四种真实搜索结果分支；输出集合三种来自首组，宝物由独立场景随后验证）；CityActionSessionTest 136检查PASS。搜索预览零RNG/零日志/零revision、实际效果、批量/自身目标、上限、拒绝/重放、完整旬和save/RNG一致。CriticalYear50PASS，架构及168资源检查PASS。
- 扩展回归未全绿：StrategyTest71进驻入口错误、GovernmentTest107仍期待建设10AP、EstatesTest85尝试已被继承策略拒绝的v31前地图存档、GameSessionTest30旧历史起始hash。均在改动前完整core classes重现（session-oldcore使用当前新增API/runtime搭配旧core，仅能定位旧core状态差异）。不删或弱化断言。Government旧断言需要按已证实建设20AP及真实费用强化；旧地图继续兼容策略待专项处理。
- 主APK/测试APK均已生成，out/parity/city-actions-20261003/apks；合并Gradle --continue总体退出失败来自上述三套回归，不能写成全测试通过。5554正使用新包重新安装并跑12年龄案例，wrapper外部备份7原文件并逐例恢复；结果尚未完成，不能沿用fb6bcd55或旧年龄APK结果。PC目录只读、没有Wine、没有ARM真机证据。
- PC城市AP调查新增原标签表8af2c0及标签函数48f190：0开发、1征兵、3巡察、6训练、11搜索、13褒赏。5c3c9c/5cbf8b传20，5c440a后的布尔分支产生10/20；命令标签到真实执行函数的完整绑定与训练减免条件尚待证明，未凭表序把工程城市AP一律改20。bounded反汇编在out/parity/city-actions-20261003/native-*.asm，不能把不对齐起始反汇编当证据。

### 2026-10-03 checkpoint O之后：原城市AP与委任/AI

- 原PC行动力校准已落地：巡察/征兵/训练基础20；训练原军事府41条件减至10，但项目军事府未实现，仍记差异。工具export_pc_city_action_costs.py输出PcCityActionCosts及docs/pc-data/city-action-costs-native.json，EXE指令/名称/SHA校验、导出复现全等；原生3测试36AP边界全部通过，无伪造validator，无PC写入。
- 新纯预览和正常Strategy/GameCommand都使用同一实际AP。StrategicAi过滤不能付费候选，保留合法10AP搜索；经济委任使用建设预览，修复1500金硬门槛（实际市场/农场200）。GovernmentTest旧断言加强为准确200金/20AP/Lv1，目前继续失败于继承的v7旧地图拒绝；没有削弱旧档断言。
- 城市核心1899、委任288、会话140、建设489、运输269+88、CriticalYear50PASS；native-final-build.log BUILD SUCCESSFUL26秒。新冻结out/parity/city-actions-native-20261003/apks/app-debug.apk87009865B SHA f4cd4d34447605cdafc63d7b597e5082e9f6ab516e895b404bec1acbcf461ed3；测试包685091B SHA6e2b280417d49db89596e597f2d313cf3dc614bb01cbafd49aa46af3d2731d4e；双ABI native保留，168资源和架构通过。尚未安装此候选，不能继承O安装结果。
- O快照3750文件全部SHA校验通过。O包06f6ba6a9b86cb0cabf2030a92f45c206c45f349044ee84d364f1d0a3a29eda9已实装，但刘备年轻首例360.03秒超时，未完成任何年龄验收。已在逐例恢复完成后SIGINT停止剩余批次，外层最终恢复也7文件全等、无新增、auto82554269…f0a9。外部采集图可能包含旧证据，不能根据同名旧图认定这轮执行到了相同步骤。
- 5554软件图形诊断：GPU host持续driver stall；只关闭本侧5554，无清数据，重启同san11-pc-map/5554/2core/2048MB，GPU swiftshader。原7文件启动前再次逐字节与最初备份一致；记录out/parity/emulator-software-20261003，当前exec93784是长期模拟器进程，勿当成测试任务终止。等待root后核对7文件并安装f4cd包重跑。UI5580未操作，UI最新仍4c23785，第12批未提交，不集成WIP。

## 2026-10-03 UI第十二批：城市治理与真实建设后征兵训练

- 独立UI目录/冻结G不变。城市分页提前到长概览之前；DataTable增加可保留名单的确认路径与行activated/已选读屏状态，避免Android覆盖selected。StrategyUi统一身份/版本/一次提交确认，搜索/巡察/征兵/训练明确城市与武将、返回保留查询/排序/选中，Home/横屏可用。删除巡察/训练增量、征兵治安技能分支、太守politics/4界面公式；完整效果/成本DTO下一批接入。
- 同一x86包run04治理59项、run05满治安真实核心拒绝33项、run08实际建兵舍→两旬→征兵→训练57项、run09运输回归64项通过。都用正常190曹操、真实触控，不改初始设施或库存、不直接执行规则。auto/manual3/武将库SHA全部恢复。没有把失败的before/run01/02/03/06/07覆盖为成功。
- run06测试固定像素误触地图顶部战报，修为按MapHost边界/面板遮挡选择可见地块；也改善旧build suite的测试选点，不改地图规则。run07第二旬超过45秒窗口，run08放宽后完成；两旬compute 89,100/84,241ms，total91,625/86,364ms，性能未通过，已经给玩法会话逐势力日志。不能归因于唯一组件或称ARM性能通过。
- runner可选30–900秒上限，默认170；真实长流程使用420秒，180秒自动分段视频和时间清单，约249.6秒全流程两段均保留。失败仍只经真实槽位3读档恢复，不覆盖用户文件。
- docs/uiux/VALIDATION-12.md、evidence-12与out/uiux/iteration-12保存完整证据。已安装x86 app-uiux-run07.apk 82,245,764B SHA c8405d8056c29886695d08125779fe384a776842dde3064321c4430adc073f7f；ARM64 82,188,605B SHA cfcb708a7444a2590246d25cc6af1d0cf26ca31d90d6dc58b9083e7666027494仅构建/ABI/原生库/168资源核验。架构PASS，root504/G346一致。仍使用contract-v2.init.gradle和第九批352项build-inputs。
- 下一批依赖已准备：只读玩法checkpoint-20261003-p并复制377模块文件到自己的out/uiux/contract-20261003-p，全SHA一致，active_build=false。P含建设/运输/外交/六类城市DTO、新建Lv1及巡察/征兵/训练基础20AP；本批G是旧10AP/Lv3，禁止套用验证结论。优先新增独立构建输入包，消费这些DTO/typed命令并移除旧成本/工期文字，再在P实装验证。不要修改root core/API/runtime或原目录。
- 玩法已集成至UI4c23785；5554战法触点/运输第二副将、年龄退出仍有真实失败，不能用5580覆盖。后续需要结合原始证据核实其实际renderer/输入/布局；没有操作5554。其他制造/商人/高级外交、多指手势、进程销毁/长时资源释放与性能继续。总goal仍active，未完成。

## 2026-10-03 第十阶段最新重启交接

详细证据见docs/validation/parity-20261003/ROUND_10.md。UI12已按4c23785→a5f93be474ae9d18dfe5ba88df1818c6d5f19fff顺序预检集成54文件；下一base=a5f93be，未取WIP。全量core/runtime check32任务失败，完整N重新编译对照30项首个失败相同；两个新差异处理后军团674通过、Core继续遇到N原AI部署失败，不能报全绿。9真实剧本城市45命令135旬805检查、正常建设→征兵177检查通过。

AI路线优化保持9剧本27旬全部save/RNG一致，本机JVM78.3→45.1秒仅样本。Q完整快照3804文件273917146B保存全部继承成果、UI12、20AP、公平性、性能改动；manifest在out/parity/checkpoint-20261003-q，不含此段后续文档。Q包87011185B SHA7801884b02e2a99de5e9e15814c48d43b12c00d2bb367fd33c2b4bfed9e8c958，测试688371B SHAbc309a78582db7ab5155a3aed738b6480e7ccdb89795f4754301b325c6e3be47，双ABI，构建30秒成功，架构通过。

P包f4cd年龄SwiftShader首例360.03秒超时，11例未运行；lifecycleOnly诊断240.02秒超时，不能替代严格年龄检查。均7原文件全部恢复、无新增、auto82554269…f0a9。实际pid6207的03:52:16栈在age-lifecycle-diagnostic/app-trace_02.txt：runner在ready，scene-cpu在水域材质构网，单样本不判定唯一原因。

现在开始5554唯一installed-opening测试，封存Q包实装后跑正常新开局/保存/读取，out/parity/ui-integration-a5f93be/installed-opening。原用户7文件外部备份+finally全字节恢复；完成前不要同设备安装。exec93784长期模拟器进程不是测试，勿终止。UI5580不触碰。UI12固定AP10/旧期望尚待其下一批typed DTO接入，不能掩盖核心20AP。本goal保持active。

### 第十阶段设备与数据最新状态（覆盖上面的运行中描述）

Q opening最终360.02秒超时，真实保存、取消覆盖/读取、震动设置均通过，尚未新开局。7原文件全字节恢复，无新增。SwiftShader生命周期诊断同样已结束240.02秒超时。之后备份并核验用户数据，仅关本侧5554、重启同AVD为host GPU/4核/2048M，不清数据；前后7文件一致，emulator-host-q-20261003/verified.json。旧模拟器exec93784已正常退出，新长期模拟器为exec48129，勿当测试终止。

同Q包7801884b回读主/测试SHA一致，在新host环境刘备年轻严格年龄案例354.65秒PASS145检查：正常命令、严格缓存颜色、原GPU层、暂停恢复/4倍速/退出、完整权威/RNG/save均通过，finally7文件恢复全等。out/parity/ui-integration-a5f93be/installed-age-young-host含最终结果、原stdout、03号真实栈和本轮step2/after图。不能将此1例当作全部12例通过。当前5554唯一测试exec48995正在跑余11例，目录installed-ages-remaining，逐例600秒外部上限、首失败停止、每例恢复；同设备禁止另开安装/测试。只读采证可进行。

新增原巡察3项测试通过（75数值/周边案例及真实能力更新链），但未闭合的外交字段/普通年龄修正/三执行者仍未写入生产规则。当前生产巡察还是工程政治/魅力公式，差异保留，详见ROUND_10。没有新增美术逆向。

新增inspect_pc_scenario_units.py：16剧本原1100武将循环实际读850×152字节，原部队修正493b06..493b48与1000槽位有效性均执行；合计16000内存槽位，类型22单位负载均0字节，载入阶段有效单位0。不能推断开局事件/MOD不生成军队。两项源字段/原字节变更及有效主将实际识别测试通过，gzip及摘要重复输出全等；docs/pc-data/scenario-units-*已保存，gzip SHA f2d2b82b4570b5d054ddee242676d3f26128974acd8557eb2cdcbb57c09502ef。

正在新增原武将军团→势力归属审计（inspect_pc_scenario_placements.py，exec42875），保留未证实location9c/statusa0原数值，不推断登场/存活身份；逐条与既有姓名/生卒/性别身份审计source SHA绑定。新增归属变化验证在运行。上述工具/数据都晚于Q，后续快照另建，不改Q。UI13来信仍WIP、尚无完成提交，不集成；本侧三模块仍Q兼容接口。

### 第十一阶段更新：UI13已集成、巡察规则专项通过

详见docs/validation/parity-20261003/ROUND_11.md。16×850归属及原+a0身份标签审计已完成，原九标签与五能力名称均由原函数取出；schema2 gzip6fedb983…66cbf重复输出全等。部队源三项测试通过后新增第四项标签测试，完整四项重跑exec62713。未改写项目剧本/旧档，未登/未發与location9c不扩展猜测。

原巡察新162位置+5关系数值边界通过，共三项10.257秒。已接入生产正常巡察和typed预览：当前统率整除28+2、现有敌对部队城市1..3环内先减半、最后治安100封顶；移除政治魅力旧公式。PcPatrol1467、CityAction1899、Session140检查通过，含144完整旬读档/RNG。仍单武将，PC能力修正/三执行者/完整外交和关港许可待做；不宣布整体规则完成。

UI13收到完成提交8e1d5f750390429cf8bb251805ca4456bf6ad6d1后已逐SHA预检并从a5f93be顺序合入52文件；下一base=8e1d5f7，未取WIP，架构/diff检查通过。当前exec85320在做巡察/Strategy/Territory及双APK构建，日志ui-integration-8e1d5f7/build.log。尚未冻结R或安装新包。Q年龄exec48995仍独占5554，赵云老年完成后累计8/12通过，下一诸葛亮青年；每例7原文件/auto82554269…f0a9恢复一致。该批结束前不安装新包，长期模拟器仍48129；UI5580不操作。

### 第十一阶段实装最新状态（覆盖以上运行中描述）

Q年龄已完成：刘备青年独立145检查+余11例批次合计，12/12严格案例共1610检查通过，原7文件逐例/final全恢复、无新增。out/parity/ui-integration-a5f93be/age-12-summary.json固定Q主/test SHA；不存在“沿用旧APK通过”。范围仅API29 x86_64 host GPU，无ARM真机。pid14294/trace_04处于青年/老年交界的夹具准备，不能精确归例或作为渲染死锁证据。

R快照out/parity/checkpoint-20261003-r：3857文件282487260B逐SHA/大小与冻结时工作区全等；含UI13与新巡察规则。双APK构建3分5秒79任务成功、168固定资源全通过。主87012773B d7be448627c1a988ab0e81edaf916b75cd3bdb641c8f2cc309f95f4a38e25c7b，test691951B 960270f9316bdbe31c5e4a4ce813e0a15dc8c19164b86196a8a345df0823b5bc。封存在ui-integration-8e1d5f7/apks。Territory674通过；Strategy与N同进驻入口异常保留，不削弱断言。本侧Gradle daemon40980构建完成后已结束，UI进程未动。

5554已实际安装R双APK并逐SHA回读一致。installed-opening 113.65秒PASS62：真实新开局/取消/旋转/唯一native宿主/设置/保存/读档，原7用户文件全字节恢复、无新增，auto仍82554269…f0a9。12份当前批证据已拉到installed-opening/evidence。当前唯一设备测试exec80373正在installed-governance（r13-governance-01，900秒外部上限），完成前不在5554另跑安装/测试；长期模拟器48129勿杀。

主机新巡察真实剧本45命令/135旬probe仍在exec68442、日志patrol-rule-20261003/production-flow.log，已推进至heroes-250前后，未结束不能报完整805。原部队四项66.181秒通过。R之后新增compare_pc_officer_assignments.py及comparison JSON/来源拒绝测试（exec46912），只审计5份同日期重建的声明归属，不改运行时；差异分名单缺失/归属不同/身份未知，死者或未登场缺席不自动当缺失玩法。该后续工具不在R快照中，下一快照另收录。goal保持active。

## 2026-10-03 UI第十三批：P权威城市与建设接口

- 当前构建改为独立冻结P377文件，`out/uiux/contract-v3.init.gradle`，companion `out/uiux/iteration-13/build-inputs.tar.gz`383项。root core/API/runtime/data/内容工具504份继承文件仍全SHA一致，没有改原目录或5554。旧G输入不适用于本批app，已更新CONTRACT_BUILD.md。
- 六类CityAction与普通Construction已用typed preview/execute；删除旧固定AP10、新建最高级、政治推工期、褒奖总金等UI计算。真实费用/效果/工期/等级/失败原因由DTO给出，拒绝禁用确认、保留修改，正式StateToken仍重验。搜索只说明条件检定，无预抽RNG。
- 太守和褒奖目标/执行人双层保留、明确“返回目标”；褒奖多选、中文查询、键盘与横屏可达。DataTable retained关闭回调保留布局监听清理。
- 同一x86 app-uiux-run06.apk/test-run06：p13-03治理64、p13-04拒绝34、p13-05建设50、p13-06人事59、p13-07真实建兵舍→两旬→征兵→训练61、p13-08运输回归64，共332检查全部通过，均正常190曹操和真实触控。全部读档恢复auto/manual3/武将库原SHA。
- 初次run01测试错线程调用preview、p13-02费用段落selector误用startsWith均已修正，原失败保留。run01复用旧设备目录混入旧截图，不可当纯本批证据；后续全局唯一p13命名，runner新增设备端目录/录像重名拒绝，不删除旧证据。
- 首次预览city最高369ms、正式路径169–313ms，construction125/218ms；重复同token0–1ms不等于首次性能。两旬compute54,760/150,258ms，total59,828/153,132ms；294.4秒两段视频/实际起止/取帧/逐势力日志保留，不是性能通过，也不是ARM数据。主机双模拟器/取帧并行，不能唯一归因；契约/日志交玩法会话。
- x86 82,259,912B SHA aef1ea35cefda35e477950c862432fb63c1e361c39aac4328363097dfc1e3b73，已安装回读一致；ARM64 82,202,753B SHA70a8886ae6eff3635b3b4f59cb8b142546891ce4379779584804c38d87c267cc仅构建/ABI/原生库/168资源验证，未实跑。两包保留version159/source a5f93be+dirty。docs/uiux/VALIDATION-13.md、evidence-13与iteration-13源码/包/录像完整交接。
- 下一步先消费P已有Transport/Diplomacy DTO（MainActivity轻量包装已准备，两个UI尚未切换，不得宣称已接入）；随后制造/交易/高级外交、地图多指、进程销毁/长时释放/性能。取消拆除建设、运输改道/卸货/返程不属本批DTO。玩法5554独立失败不能被本侧通过抹除。全goal继续active，最终玩法会话集成。

### 玩法主会话最新交接：R验证、S完整快照与5554恢复

当前生产core/API/runtime/app仍R，UI集成base=8e1d5f7；UI14仍WIP（自己Q/v4下Transport/Diplomacy），不取工作树、不操作5580。Q六将两年龄12/12共1610已通过。R新开局62/113.65秒、城市64/67.21秒、正常190真实兵舍→两旬→征兵训练→读档61/112.86秒通过。主机9剧本45正常命令/135完整旬/805检查全部通过，9次初始征兵缺兵舍不可用保留。原7用户文件每批均恢复全等、无新增，auto/manual3仍82554269…f0a9。

R旧250建设11.25秒失败于可见合法开发地（未下令）；R正常190运输225.81秒失败于点击首行后主将未写入（未派遣，名单仍开），原断言/FAIL图保留。190来自UI13实际正常开局02ddb3d4…d69/185898B，临时输入工具先外部全备份、只覆盖已存在auto/manual3并逐SHA读回，finally恢复原7文件。JVM与ART仅嵌入战报gzip的OS字节185557及外层CRC不同，解压和其他全部字节一致，未改原输入。ROUND_11和契约详述，不混用初始局面/APK证据。

五日期声明归属对照：3021可核实归属中542不同，308缺席原人物全是死亡状态，921身份/势力名未知；对照SHA509b9ecc…fcf3b，重复输出一致、三个来源错配子案例拒绝通过。原部队/人物源4项66.181秒通过。正则性能仅out隔离原型：153检查结果相同，JVM1899→2069ms无改善证据，不改生产RulesSave。R两旬compute14.4/28.3秒、首次预览346/369ms仍是性能开放项。

S快照已创建：out/parity/checkpoint-20261003-s，3861文件283358049B，收纳R之后数据/工具/文档；生产模块/app与R应逐SHA一致，核验exec81686。R包仍d7be4486…5c7b/test960270f9…b5bc，没有新生产构建输入。第一次用Python追加本节因stdin编码失败，没有写入；S仍保存其创建时完整工作区，不覆盖R/S。

本侧5554在运输结束后系统仍持续Faking VSYNC due to driver stall，日志installed-transport-190/system-input.log，尚不判断为唯一失败原因。已在emulator-host-r-recovery-20261003备份并与上一restore全部7文件比对后请求关闭本侧AVD（exec66258）；准备同AVD host GPU/2048M/4核无清数据重启，再核对文件，重跑相同R/190运输。旧长期模拟器exec48129将正常退出，新进程ID待记录。没有其他5554测试运行；UI5580不碰。goal保持active，完整玩法/数据/ARM尚未完成。

### 最新增量：交易/制造接口开发与R运输复测结束

5554同AVD重启到长期exec48890，启动前后7文件SHA一致。R同包同190运输复测exec24877结束，126.20秒再次失败于首行主将未写入，尚未派遣；installed-transport-190-host-retry含5附件、system日志、原断言。finally7原文件恢复全等、无新增。不是重启修复，不能把driver stall当唯一原因。5554测试已全部结束，随后仅关闭本侧空闲5554以减少主机竞争，未清数据、不动UI5580或其进程。

现有生产源码已晚于R/S：新增TradePlan/TradeCommand/TradePreview/TradeQuery以及GameSession typed预览/执行，保留既有价格/10AP/旬额模型，尚未PC校准。TradePlan919、TradeSession88通过；222正常命令+44完整旬在完整R和新代码输出266行全等，SHA52efb1d8c9c44a78635efc325541732a485325743d8893d761a206f20d5c598b，包含全存档/RNG/消息。verifySession旧基线断言失败，本次完整R重编同样失败，不削弱断言。

ProductionPlan及对应typed DTO/query/session正在开发/验证，统一已有World/Army制造校验并区分立即入库与延迟任务；尚未冻结或安装。首次测试夹具给SWORD设置库存违反存档规则，保留tests.log后修正夹具为合法剑兵0库存，各兵种拒绝仍测；tests-valid-fixture.log在运行。app文件未改，UI14未完成不集成。必须验证专项、完整R生产结果对照、架构并新建完整快照后再发布接口；R安装结论不可沿用到新代码。

### 第十二阶段最终规则增量（覆盖上一段进行中状态）

制造756/会话216通过，R正常命令132次+108旬240行save/RNG/结果完全一致3140b279…bc1e6；三次夹具合法性问题及修复保留，见ROUND_12。T完整快照3876文件283412344B逐SHA/工作区核验全等；T双包构建1分3秒76任务成功，168资源PASS。主87019125B SHA7e069745b9145541b9d1f759aba105e4477ef5c975b6cbc03d0bf681fe0ac0c8，test960270f9…b5bc，T未安装。

之后原EXE新取证：生产两入口5c67a4/5c7119与商人5cad0f实扣20AP；原标签2生產/4商人，60边界四项3.763秒PASS，逐3MiB核实仅军团AP字节变化。生成器及正常World/Army/Campaign/typed接口已改20，AI生产/买粮预算门槛同步。319原成本边界、制造756/216、交易最终920/88、Territory674通过。交易额度夹具先三笔20用完合法60并断言归零，再单独设20隔离额度校验，原失败不删。R对照工具只额外扣旧命令10AP后，72正常命令+72旬全save/RNG/消息一致6fae8bbb…3b9bf；不是声称改规则后和R无差异。T的10AP及兼容证据作为历史保留，新20AP源码须另冻U和构建/安装。

新增原44命令名称目录工具与JSON，保留原ID/地址/Big5字节，不等于剧本实际启用或MOD状态。生产价格/公式/工期/商人数量步长/额度、取消稳定任务ID和完整军团AP仍待核实。app仍UI13，未改UI文件，UI14未完成未集成。5554目前已关闭空闲且用户7文件全恢复；UI5580不触碰。后续优先冻结U、构建后保护存档实装，采用同AVD headless host对照可减少GUI竞争，但不得当作既证实修复。goal active，无ARM真机。

### 当前实际运行状态：U已安装，5554单一opening测试

U完整快照out/parity/checkpoint-20261003-u：3880文件283434812B，逐SHA/大小/工作区全等。双包32秒76任务成功；主87019189B SHA37d46e7c7d69ac121967947f16dc5fe576e2818c3996ecc5051a4e0a2d9ab1af，test960270f9…b5bc。U是原生产/商人20AP版，T7e06是先前10AP版，禁止混淆。UI共享契约已给出U继承路径，app仍UI13base8e1d5f7，无UI14完成提交集成。

同AVD改headless host/同2048M4核，无数据清除，长期模拟器exec91542；启动前后7原文件完全一致，emulator-headless-u-20261003/verified.json。先同R包/同190/同原测试r13-transport-190-03通过58.95秒，真实三人运输/核心容量拒绝/双击仅提交一次/读档恢复，11附件已拉取。两次GUI失败仍保留，不能认定环境是唯一原因。每批原7文件全部恢复，无新增。

随后实际安装U主/test回读SHA一致，installed-army-190 run=u13-army-190-01，72.39秒PASS61：正常190真实兵舍→两旬→征兵训练→正常读档；9附件已拉，finally7原文件全等。当前5554唯一测试为U installed-opening（run=u13-opening-01，exec见下一工具），900秒外部上限，原250存档为输入；完成恢复前不可在5554另跑安装/测试。UI5580不操作。

U之后新增只读工具/审计，不改生产代码：test_pc_merchant_quote390原算术边界3.735秒PASS；inspect_pc_merchant_state核对16×42城市源读取字节7c均50，逐源/记录/偏移/内存一致，JSON SHA2e800044…61ba4重复相同。原44命令JSON SHAe3592bd2…30c4f。原商人AP许可分支5caac0新增8边界，完整city_action_costs现在5项4.597秒PASS（60扣费+8许可），三MiB查询纯净。商人报价状态更新、原city+a4 bit1使用/清零时机未闭合，因此不把初始50硬写生产或改价格。后续快照另收工具，U保持不可变。

### U实装完成与城市容量后续开发

U opening-01 exec69801结束：5.44秒因调用漏传reuseSlotSha被保护断言拒绝，未覆盖存档；原7文件全等。补齐原始82554269…f0a9后，exec37028 opening-02在65.11秒PASS62，正常新开局/保存/取消/设置/旋转/全局面RNG读档，原7文件全等、无新增。与U army61均独立实装。5554没有测试在运行，长期模拟器91542；UI5580不操作。UI14仍未宣布完成提交，最后读到运输往返52、表单69、外交65通过，等待其正式交接，不取WIP。

后续生产源码已晚于U：原城市金getter486d30分支486ddc返回100000，粮getter486ea0分支486f4c返回1000000。新增export_pc_city_capacities生成常量/审计并接入Campaign；SaveCodec保持旧1000000兼容读取，不截断超额余额，后续收入/卖粮仍遵守新上限。原资源段75边界与42城市×2容量getter新增测试；首次误填粮上限500000及城市stride1d8分别失败并保留，查实际原函数/已有GROUPS后修正为1000000/248，不是弱化原规则。完整最终native测试exec13924已结束待查日志；JVM专项exec37515运行，尚未冻结或构建容量版。Domestic/Strategy旧容量断言已明确改为本次预期规则变化，其他历史失败仍需对照U。不要把U实装结果归到后续容量代码。

### 第十三阶段UI14已完成顺序集成

V完整快照3887文件283776523B逐SHA/工作区全等，含城市容量规则及超额旧档兼容。原报价390+资源75+城市getter84边界三项5.106秒PASS；容量专项最终137，交易920，运输269/88，Territory674、交易会话88通过。完整U重新编生产类及U自己的Domestic/Strategy测试，同当前失败分别为unfinished market gives no yield、合法入口断言，历史问题保留。首次baseline全历史测试编译缺额外sourceSet失败日志也保留，最终目标测试按依赖sourcepath成功编译。

已读UI14完成提交9e30e455f9b6a53c86bc9b51b213a21001494c32，从8e1d5f7预检并逐SHA顺序合入55文件，未碰其活动工作树或core。下一UIbase=9e30e45。运输与普通外交正式typed命令消费，制造/交易仍待UI接。UI本侧Q包186项及较早189外交链证据见VALIDATION-14，不能混为本侧新规则验收。此侧双APK构建exec49534已30秒82任务成功，含容量137和DiplomacySession，准备封存W及安装；当前5554无测试运行，原7文件保持恢复，模拟器91542。ROUND_13记录完整限制，goal active。

### W当前实装状态

W3935文件290510437B已冻结逐SHA/工作区全等；主87019933B SHAe2abcc797d76bb69c1fd3a28d2ced7776336c30c7fe377fe7c576a3b64ae87d9，test696127B SHAdee60fe08c39ba1a4cad798a450e5c4233e990e116fdb6bd09ae48c62a98a8b0。168包资源/4原生输入核对通过，架构通过。5554实际安装回读一致，w14-cargo-travel-01 exec92061已55.72秒PASS52，7证据拉取、原7用户文件全等恢复无新增。当前唯一测试W installed-opening run=w14-opening-01，exec22039，900秒上限；完成恢复前不另测。长期模拟器91542。UI15来信已从完整V独立继承三模块并接生产/商人DTO，不向其回信、未授权消息；不取其WIP，不触碰5580。

W后只有新原生test_pc_site_capacity.py和文档，生产/app没变：10关35港tech33扩展港关450原查询/3MiB纯净4.861秒PASS，当前项目关港钱粮容量与原函数相同，不改值；还不证明研究条件、部队容量或MOD激活。该工具未在W，后续另冻。

W opening exec22039已65.04秒PASS62，12证据拉取，原7文件全等恢复无新增。当前唯一5554测试改为W installed-diplomacy-journey / run=w14-diplomacy-journey-01 / exec28262，正常190来源02ddb…d69，900秒上限；不要并行安装/启动5554其他测试。正在亲善→真实同盟多次交涉的回合链，未结束不能报通过。under-cap-comparison.json额外证明完整U和W无任何基线变换的72正常命令+72旬144行消息/save/RNG全等6fae8bbb…3b9bf；只覆盖未触及新金上限，容量变化另有137专项。opening截图08和08b均仍出现3D准备提示，不能用名字推定渲染完成或性能通过。

外交exec28262已结束：234.41秒PASS189，20证据拉取，亲善→两次失败后第三次同盟成立→解除→停战抵达，五次实际旬推进及最终完整资源/协定/任务/RNG读档，原7文件全等恢复。当前5554唯一批处理exec76120顺序跑W installed-transport(run=w14-transport-01)再installed-diplomacy(run=w14-diplomacy-01)，每项外部备份、同包回读、finally恢复7文件，任何失败停止后续；完成前勿另测/安装。UI15仍WIP，来信接Production/Trade已开始编译验证，没有完成提交，不集成。当前1369模块/构建文件SHA与W一致。

### 最新重启交接：W全部5套437通过，5554无运行中测试

exec76120结束：运输69/57.85秒、外交65/54.63秒通过，各11附件拉取。W合计5套437全部独立实际操作通过（opening62/cargoTravel52/transport69/diplomacy65/diplomacyJourney189），61附件，installed-summary.json保存逐包/逐套/恢复身份。每套原7文件全等，无新增，auto/manual3仍82554269…f0a9。长期5554模拟器exec91542仍在，除此无测试进程；不操作UI5580。主W APK e2abcc79…ae87d9/test dee60fe0…a8b0路径ui-integration-9e30e45/apks；仅API29 x86_64 host GPU无窗口，无ARM真机证据。Q年龄和U军政结果不混入W437。

完整V为容量规则/API基线，W为V+UI14；所有生产模块/应用自W未变，1369份SHA一致。准备封存X只补后续证据工具/文档，不另造新APK身份。商人原四项最终4.599秒通过，新增bit1许可256边界；关港450明确目录复测5.458秒通过。原商人city+a4 bit1阻止再次操作已证实，但清零周期未知，全零方法47b730无直接call引用，禁止把原规则擅自写成每旬一次。当前20000累计额度仍待对齐。

UI15基于冻结V在独立目录接Production/Trade，尚无完成提交，不取WIP、不发未授权消息。下一UI增量base9e30e455f9b6a53c86bc9b51b213a21001494c32。下一重点为经济UI最终集成/实装、商人价格/标志周期、三执行者与剧本/武将完整映射等；ROUND_13、PC_PARITY_STATUS记录证据与未完成，goal仍active。

## 2026-10-03 UI第十四批：Q运输与普通外交权威接口

- 独立冻结Q377/v4 init，companion383；root受保护504及固定资源/原生库SHA完全一致，不写原目录/5554/规则/转换工具。当前AVD5580使用host -no-window、2048MB，1080×1920/420dpi；原host窗口和SwiftShader严重停顿失败记录完整保留，不可当业务死锁或性能通过。
- CargoWizard接TransportPreview/Command，库存/容量/范围/费用/路线/耗粮由核心提供；保留原超限输入，100ms合并同步查询，确认防重复。条件按钮与反馈同行；实际长列表失败后兵装/舰船拆为独立页签，避免滚到末项，全部数量保留。
- 普通亲善/停战/同盟/解除协定接DiplomacyPreview/Command；DTO期限/成本/条件检定/抵达阶段，失败禁用、返回保留执行人查询/选择、上一步明确。高级外交未切换。
- 最终app/test-run12：p14-12运输69、p14-13真实混合货物抵达/卸货/人员返程52、p14-14外交65，合计186检查通过。较早app06 p14-10完整外交链189通过：真实亲善、三次同盟交涉（前两次未成立、第三次成立）、解除、停战抵达，五旬与完整读档恢复；之后只改运输页签/测试/runner，未改CampaignUi。不能把189项算作最终APK重复执行。
- p14-02列表selector失败修正；p14-04/07 host及08 SwiftShader慢/超时保留；p14-11长合页14次滑动不到末项，run12独立舰船页真实通过。runner恢复超时后现在仍停止过期instrumentation并回读存档，失败不伪装为通过；实际p14-08和受控模拟都验证此分支。
- 最终x86 82,261,284B SHA7646c956676582b7819fc71282de5c5a514ec75d4bf58fac0adcaaa3ee2898ed（app-uiux-run12.apk，已安装回读）；ARM64 82,204,125B SHA661f9bf3187c9127529cf0cfe8c27afe9ae9a601922d1a727c80091326d4a26d（app-uiux-arm64-run12.apk，仅构建/库/168资源验证）。都version159/source8e1d5f7+dirty。三存档仍原SHA；不清数据。
- 最终三视频61.37/90.55/57.82s，外交完整链198.87s两段，均取帧；截图、逐步检查、阶段性能、失败与恢复、APK/源码/companion都在iteration-14及docs/uiux/VALIDATION-14.md、evidence-14。
- 首次运输381/190ms、外交258ms；外交五旬compute7.4–15.5秒，运输两旬21.9/21.5秒；早期SwiftShader首次预览11.8秒，不能混环境作因果基准。首次查询/冷启动空白等待/长期稳定性仍需改进，没有ARM真机性能结论。
- 制造/贸易/任务管理/高级外交及宿主异步预览契约已交玩法会话。后续还有多指手势、进程销毁/长时释放、舌战与结果检索。全goal active，由玩法会话最终集成，不覆盖它正在推进的新core/API。

## 最新重启交接：R14商人每城每旬一次，准备冻结Y

- 继承完整X；app仍UI14的9e30e45，UI15在独立目录验证V，禁止把其累计交易规则证据套到新核心。下一UI增量基点仍9e30e45；本轮没有修改app。
- 原商人city+a4 bit1与正常结算87据点清零闭合。180个日期推进/540年月日结果、4模式×87据点全3MiB与RNG检查通过。首次关港偏移+6c猜测失败，原48da10证实+68，严格比较保留。日志out/parity/merchant-native-20261003。
- TradePlan/Campaign正常命令使用任何正traded量判定本旬已用；TRADE_USED/field city，quotaRemaining=0或maximum。旧档余额/成交量无损，DTO结构/SaveCodec格式不变。军团采购按一次许可；不同城市独立。
- TradePlan922、TradeSession92、Territory674、容量137通过。冻结X完整core独立编译生成实际两次成交旧档，新core逐字读取再三旬正常交易/save/RNG一致。Campaign新旧X均在merge设施位置冲突失败，未降低断言。
- 原4b3c60月价/RNG另3项18.260秒通过：192初始/32非法月份/2816月更新/85RNG边界。导出月表和函数SHA；价格尚未接入，+9c条件语义/当前政治/数量限制等仍未知，禁止把原始50当开局价。
- 最新说明docs/validation/parity-20261003/ROUND_14.md，契约PARITY_UI_CONTRACT顶部R14段。正准备checkpoint-20261003-y完整快照和新APK；当前实装仍W，原7文件已恢复。5554无运行中测试，5580属于UI会话不要碰。目标active，完整玩法/全部数据仍大量未完成。

## 最新重启交接：Y已实装114通过，Z补齐取证与复现工具

- Y完整3944文件290552774B，manifest/verified.json逐SHA/大小/冻结工作区全等。UI可整组三模块继承Y；下一UI集成基点9e30e45。UI15仍WIP，最新消息明确其75交易检查属于旧V累计额度，不可套Y。没有发送其他会话消息或覆盖其文件。
- Y主APK out/parity/merchant-native-20261003/apks/app-debug.apk，87019885B SHA83578d19c4bba0503f41d359e9bb8e0eb50e3dd270cfc4ee535b94e67d34817f；test696127B SHAdee60fe08c39ba1a4cad798a450e5c4233e990e116fdb6bd09ae48c62a98a8b0。43秒76任务构建成功，168固定资源/4原生输入一致。
- 本側5554实际安装回读双SHA一致；y14-opening-01 62项97.37秒，y14-cargo-travel-01 52项81.77秒。合计114，分别12/7附件，正常190新局/两旬/送达卸货/人员返程/存取。两次各7原用户文件全等、无新增；自动/手动3原SHA82554269…99f0a9。当前没有运行中的安装/测试，5554保留Y和原用户局面；仅API29/x86_64，没有ARM真机。08b截图仍3D准备中，非冷启动渲染通过。
- Y后新增test_pc_merchant_site.py：原5ca985入口，486660检查类型0，490a10索引0..41；87×3共261组全3MiB/RNG纯净，7.354秒通过。**Y还允许关港交易，下一步应实际加TRADE_SITE/正常命令/AI/旧档/typed测试，再新包验证**。不要把新证据当已实现。
- Y后新增verify_pc_merchant_compatibility.py，显式snapshot manifest逐SHA验证旧源码/资源、独立javac旧core写真实双次成交，再候选读取与3旬正常回放；正式脚本复跑通过，旧档2319B SHA51511da05c2c232a61495d0a43f1268d8d66fd45830af14b72fca2357f8936d7。见reproduced-compatibility/results.json。
- 原价格初始化/月更新已确认但未接入；原数量容量函数在5ca7xx，out/parity/merchant-native-20261003/merchant-admission-aligned.txt含后段，尚未完整验证。city+9c mask3/4语义、当前政治修正/数量步长/执行者功绩等仍待。下一工作先补城市限定，再继续商人全规则与UI15整合，不扩美术。
- 本段后完整资料封存checkpoint-20261003-z，验证JSON在父目录；生产模块必须与Y全等，仅增加工具/测试/文档。全目标active；保留Campaign新旧X同一merge设施冲突失败（旧手造3,2/4,2位置可能已不合法，待查），未降低断言。其他历史Domestic/Strategy失败和全玩法/全数据未完成仍见总台账。

## 最新重启交接：R15城市商人许可已实现，UI15准备交付

- 起点完整Z已核验；本轮没有app改动，UI集成基点仍9e30e45。TradePlan新增TRADE_SITE/city拒绝关港，quantity原值保留但quota/availableMaximum为0；正常命令、军团/势力AI共享。旧档余额/商人量保持，格式33不变。
- TradePlan964、TradeSession100、Territory674、容量137、经济成本319、ProductionPlan756/Session216通过。verify_pc_merchant_compatibility正式脚本增加冻结X实际港口买/卖旧档，2446B SHA c2eaebfdf2771a84233f7d73bf5da938214cd80857af861966ecf3df0a713cb9；候选完整字节回读和三旬save/RNG一致。
- 原5ca770最大买卖量320组+2空指针、5ca450数量validator60组通过（5.598秒），函数和UI数值范围原路径已查；显然允许大于2万/非整千，卖出容量最多food−1。**尚未接入新数量/价格**，不能用局部validator代替完整准入。工具inspect_pc_merchant_admission.py与merchant-admission-native.json记录SHA和界限。
- out/parity/merchant-site-20261003含原反汇编/测试/兼容日志，ROUND_15和总台账/共享契约已更新。正在封存checkpoint-20261003-aa当前完整核心以待UI15增量；候选新包未构建/实装。5554当前Y/原7文件，5580属于UI会话，均未清数据。
- UI会话最新状态：V最终交易75/制造138共213交互通过，七旬制造完成，正在提交增量；原V累计交易75不能套R15。等完成提交再按9e30e45顺序集成，不能拷贝其WIP核心或覆盖自身成果。保持目标active。

## 2026-10-03 UI第十五批：V生产与交易权威接口

- 独立UI目录9e30e45之后增量，冻结V394/v5 init，400项companion完整核验；root受保护504与APK内638项初始资源、两ABI原生库都一致。原目录/5554/规则/数据/内容工具未改，无Wine。
- ArmyUi接Production DTO/typed执行，成本、设施剩余次数、产量/工期、即时库存与在制分开；失败禁用、保留执行人/上一步。CampaignUi单表单买卖、原输入/执行人/方向草稿、DTO范围/报价/资源结果，删除AP10与交易公式，正式只看allowed。100ms合并同步查询，不私建World；MainActivity新增包装与trade恢复入口。
- 最终app/test-run04：p15-05交易75、p15-06生产138，共213检查通过，正常190曹操真实触控。实际买1000/卖1000、共享额度拒绝；建锻冶所两旬→生产2500枪装；建工房两旬→冲车任务三旬→库存2变3，七次实际推进，双击唯一revision/费用等于DTO、完整读档恢复。p15-04 UI14+V改前21通过，旧AP10和设施错误留图。造船/取消制造不宣称已实跑。
- p15-01武将表格selector失败、p15-03工房未搜索而定位超时保留并真实读档恢复；build02测试方法签名修正。p15-02早期交易虽75通过，目视发现横屏IME全屏遮挡，run04关闭提取并补isFullscreenMode与完整底栏断言，最终p15-05重跑通过。scrollTo现在完整露出再触摸。
- x86 app-uiux-run04.apk 82,270,608B SHA03dcfc109c06ab4992aca5a6a6a4650f96eedaf8b919f19aa485ee703927d683，安装回读一致；ARM64 82,213,449B SHA7c4bb234247923fde270d8f7e6486afb72c1279a4bf0a9bdc3a63c8d848fc49f，仅构建/ABI/资源验证，均version159/source9e30e45+dirty。三用户文件仍原SHA。
- 证据docs/uiux/VALIDATION-15.md/evidence-15，完整APK/源码/companion/视频在iteration-15。交易视频75.724s，制造177.377/179.916/70.592s三段、约432s墙钟，有实际分段区间与顺序解码；不是绝对无丢帧声明。改前25.096s，首次30s取帧越界失败保留，改5/15/22秒取帧成功。
- 最终七旬compute59.6/50.5/33.7/51.7/20.8/15.4/11.3秒，total63.3/57.2/37.0/58.4/28.2/24.8/22.3秒；交易预览最高424ms。仍未验收流畅性，无ARM真机数据。
- 注意玩法会话当前已推进Y及每城每旬仅一次交易/关港限制，UI15仍严格冻结V，不能套用V同旬买后卖通过结论。下一批先核对其最新交接与完整冻结，再接新字段并改实际测试；不要拷覆盖自己的UI15或对方core。制造取消仍旧officerId，没有伪造taskId。后续舰船制造/任务管理/高级外交、多指、其它数量控件IME、进程销毁/长期释放/冷启动及串行预览优化。总goal active，最终由玩法会话集成。

## 最新重启交接：AB独立实装200通过，AC冻结前资料补齐

- 当前已顺序合入UI15完成74072e9，下一UI增量基点74072e9。UI16在自己5580整组继承AA396模块文件，正在测同城一次/跨旬/关港/数量IME；仅完成提交可集成，未向该会话发消息，未改其WIP。
- 本侧AB主APK87022841B SHAa29558effce346f50aa311137a0d5c47e3ae8af2d75d8624f9b8a7050fdbd309，test700707B SHA42ee55915bc820700e3d005a82cce74fbdc69f2624804aba5db38a281590d2b8。5554制造138/374.31秒、新开局存取62/104.28秒独立通过，共200；两次各7原文件字节全等，无新增。当前5554无测试运行，保留AB与原用户档。没有ARM真机证据。
- 制造实际两条路径：建锻冶所2旬→2500枪装，重载起点→建工房2旬→冲车任务3旬→库存2变3；共7次推进，不能说同一局连续7旬。完整SHA/10+12附件/日志在out/parity/ui-integration-74072e9。原UI15旧V同旬重复交易断言未在AB运行；待UI16新脚本。
- AB后仅修正CampaignTest历史夹具：市场从非法城市占地移至有效相邻格，增加存档validate；两盟友兵力恒定场景隔离第三方城防射击。保留原断言。独立14case审计13通过、migration失败；冻结X原migration与continuation失败已复现存证，完整Campaign仍不绿。
- 新test_pc_merchant_conditions.py：原属性0x9a/9b/9c真实Big5名称与读写分支确认city+9c三位为瘟疫/蝗災/豐收，48组完整3MiB/RNG测试通过。inspect_pc_merchant_prices.py与merchant-prices-native.json更新schema2；价格/数量仍未接入，灾害时序/当前政治修正/原32位RNG兼容仍待。
- 下一步封存完整AC（尚未生成），再推进原价格/数量和后续UI16顺序集成。生产代码仍AB；完整规则/数据差异与历史旧存档迁移尚未完成，目标active。PC只读、无Wine。

## 最新重启交接：R16商人功绩已接正常命令，待新APK验证

- 完整AC3969文件295952016B已逐SHA核对；生产/构建输入与AB相同。AC之后新增R16，app仍UI15的74072e9，UI16在自己5580验证中，未集成WIP。
- 原商人功绩50/上限60000与真实武将属性名功績闭合，12个完整3MiB/RNG边界通过。正常Campaign.trade/TradePlan.effects共用新PcMerchantRules.meritGain，旧档超过60000保留且不再由商人增加，其他命令不变，SaveCodec33/DTO结构未改。
- PcMerchantMerit421、TradePlan964、TradeSession156、经济319通过。冻结X真实旧100功绩交易产生60090档2038B SHAc7b3a1c3d39e41a84df21c5e98dfc407e0cd508760958c583ab8c5f7e43c7bde，候选逐字节读取/三旬交易save/RNG无损；城市/港口旧档也重跑通过。
- 原算术3865行已直接执行导出，含420报价/320容量/224初始化/2816月更新/85RNG，Java11595检查通过。首次INT_MIN负数溢出不一致保留日志，按原32位NEG再扩展修复。这些价格/数量尚未接正常游戏，不能称已完整对齐。
- 新工具verify_pc_android_merchant.py拟以已安装APK生产DEX+独立测试DEX运行ART命令与保存检查，不改UI源；明确不是触控验收。本侧新APK41秒85任务构建成功，尚未冻结AD/实装。5554仍AB与恢复原7文件，无清数据；5580不操作。详见ROUND_16、契约顶部与out/parity/merchant-merit-20261003。目标active。

## 最新重启交接：AD实装与ART验证完成，准备AE资料冻结

- AD完整3981文件296129929B已逐SHA/大小/工作区验证；主APK87023833B SHA46cc245b9c610f61c3aa3753f65fa9408d67a51f8226dbb1a2e65855512d32f3，test716793B SHAd63f8f5f146ef4effb98624d3c091743a03306a06725685a0767805a034d8252。168固定资源和4原生输入一致。AD可供UI整组三模块继承，但UI16本批仍冻结AA；下一UI增量基点74072e9，没有接入其WIP或发消息。
- 5554新包实际安装回读双SHA一致，ad16-opening-01 UI62项124.96秒通过，12附件已提取；全部原7文件恢复，无新增，自动档原SHA82554269…99f0a9。当前无运行中5554测试，保持AD与原用户局面；5580不操作。
- 新verify_pc_android_merchant.py直接用实际安装APK生产类、只含5测试类+TSV的独立dex，在ART功绩421/3.43秒、原算术3865行11595项/0.23秒均通过。用户7文件及安装APK前后完全不变。此为生成局面正常命令/回合/save测试，不是421个UI触控；与UI62分开。无ARM真机结论。
- 原算术工具第二次执行TSV和JSON字节全等，oracle-reproduced.json有SHA。保留原INT_MIN差异、原无效身份fixture、Java零功绩非法条目失败记录；均修正原因而未降低断言。全日志out/parity/merchant-merit-20261003。
- 下一步AE只冻结最新资料，生产仍AD。随后继续价格/数量持久化、当前政治修正与正常月结时序取证/实现，或顺序集成UI16完成批次。商人功绩已接，价格/数量纯算术尚未接，经验+5/完整事件/数据及旧地图存档迁移仍未完成。目标active，不以本轮通过代替全玩法对齐。

## 最新重启交接：R17经验与伤病字段闭合，生产仍已实装AD

- AE已完整核验3981文件296133402B，生产/构建输入与AD相同。R17再补test_pc_merchant_experience.py和inspect_pc_merchant_experience.py/审计JSON/ROUND_17；准备完整AF资料基线，仍没有app改动。下一UI增量基点74072e9；UI16当前最终新交易97已通过、港口收尾及ARM构建中，尚无完成提交，不能集成WIP。
- 原商人5cacb7经验+5/刷新→功绩50→使用位→重新按当前政治报价→资源/AP扣减。160组原片段完整3MiB/RNG通过，其中32组真实成交金额不同于经验增加前报价。当前工程缺政治经验持久化，不能只替换静态公式冒称完整交易对齐。
- 原武将名称初始化/属性setter确认15配偶+60、21官职+a4、33政治经验+130、57伤病+15c。伤病-1/0无减益，1/2/3对前四能力80/50/30%，先基础/经验封顶再乘比例取整最低1；魅力不减，alternate缓存不受伤病影响。100组合+4非法selector原执行全3MiB/RNG通过，和160提交共2测试4.983秒。其他年龄曲线/官职/配偶特技联合公式仍待；不得把条件隔离测试当所有武将规则。
- out/parity/merchant-experience-20261003含原函数反汇编、实际试探与通过日志。只有资料和工具改变，实际安装仍AD主46cc245b…d32f3，UI62/ART功绩421及纯算术11595已验证，原7文件已恢复，5554无运行中测试，5580不操作。
- 接下来顺序集成UI16完成批次，或继续基础/经验/当前能力的数据与存档契约，再接价格数量。不要把成长直接写回原base而丢失后续修正基数；不重算用户自动档。完整规则/数据仍未完成，目标active，PC只读无Wine。

## 最新重启交接：R18人物来源审计完成，UI16准备顺序集成

- AF完整3985文件296153151B已核验，生产/构建输入仍AD。本次在其上新增inspect_pc_officer_ability_sources.py和详细/摘要审计、ROUND_18；准备完整AG作为合入UI16前基线。
- 16实际源/13600人物记录：原152字节serializer逐字节扰动确认growth源118..122、rank101、spouse70..71，12 signed极值确认扩展；完整源SHA/记录SHA/原解码向量/既有身份映射交叉核对。gzip1115775B SHA bff59863d1cdefa729721379c903bf9b164cc2f6f554f018a592736704f797d7。经验/伤病无源输入，旧actor报告零来自缓冲区，不能作为源配置导入；构造/开局默认值待查。
- UI16已完成13ff5b9bfc44015e5f45a4a2cefa84fee4b1e4c4，父74072e9；独立目录明确仅5个app/测试及docs/progress改动、AA冻结。自己最终deploy65/transport70/trade97/tradeSite43共275通过，自己的x86/ARM包不作为本侧验证。后续只取固定commit增量，不拷AA覆盖最新AD核心；未向该会话发消息。
- AG封存后执行integrate_ui_snapshot.py基点74072e9→13ff5b9，再构建、备份5554原7文件、正常190局面独立跑交易/港口与数量界面。当前实际安装仍AD46cc245b…d32f3，原用户文件已恢复，无运行中5554测试；5580不操作。旧opening08b仍显示3D准备覆盖层，不能算首帧性能通过。
- 正常商人已接每城每旬一次/城市限定/20AP/容量/50功绩上限60000，原价格/数量/经验仍未接，全部其他玩法和内容差异见总台账。目标active。

## 2026-10-03 UI第十六批：AA交易许可与横屏数量布局

- UI独立目录以74072e9为基点，冻结AA396/v6 init，402项companion；root保护504、两包638资源与双ABI4原生库均SHA一致。不写原目录、不操作5554、不动规则/数据/内容工具。
- 商人显示本旬可交易量/已成交量，继续只认DTO allowed/reason；每城每旬一次、换武将/方向不可绕过、下一旬恢复及据点限制由AA核心提供。价格/数量仍模型，玩法后续功绩/价格校准不混入本批。
- QuantityControl关闭全屏IME并尽量同时展示名称范围；Cargo/Deploy在短视口将页签/说明移入滚动区，底部取消/提交保留。真实测试发现GlobalLayout/Insets在浮动窗口收键盘后不足，最终绘制前仅按可见高度变化重排，关闭清理；运输也处理条件说明高度变化。
- 最终app/test-run05：p16-09出征65、p16-10运输70、p16-11交易97、p16-12港口43，共275检查通过。正常190曹操真实输入/触摸、唯一提交、完整读档恢复；港口前实际调动曹操并推进一旬，无fixture库存注入。关卡未实跑，不替代全部据点验证。
- 保留p16-03/04字段裁切、p16-06/08键盘后旧高度、p16-07旋转后切页失败及正常读档恢复；p16-05虽70通过但目视名字滚出后又加强断言，未把早期包当最终。p16-01/02早期交易97/港口43另记，不重复计入最终275。
- x86 app-uiux-run05.apk 82,271,244B SHA7ec7309421ef7dcab7b1006f01787d564434e0fd6df7462865fc55df201103c8，安装回读一致；ARM64 app-uiux-arm64-run05.apk 82,214,085B SHA4ef96e2e26ab3183ad15be4680e0da506d027f85d8e95145224711fb90deab9f，仅构建/库/资源验证。version159/source74072e9+dirty；三用户文件保持原SHA。
- docs/uiux/VALIDATION-16.md/evidence-16与iteration-16完整源码/依赖包/截图/视频/性能/失败记录。最终视频45.841/58.301/105.913/74.543秒，两个裁切对照46.481/33.716秒均顺序解码；港口视频PTS时长74.543与宿主墙钟59.660不同，原始数据保留，不能当严格实时性能证据。
- 两旬compute17.755/33.802秒、total20.229/36.035秒；交易预览首次133ms，新revision最高794ms，港口首次615ms，仍未验收流畅性，无ARM真机结论。晚采trade-final-pid.txt其实已是港口12134，最终performance.json按原日志7366/12134正确归属。
- 下一批优先地图多指/面板手势、冷启动/进程销毁/长期释放，同时继续舰船制造/任务管理/高级外交及串行预览性能契约。规则冻结后只继承自己的out，不覆盖UI成果或玩法WIP。总goal继续active，由玩法会话最终集成。

## 最新重启交接：AH整合包实装337通过，能力曲线衔接取证

- AG完整3989文件297287877B核验后，仅顺序合入UI16完成74072e9→13ff5b9的19文件，638份受保护核心/数据/工具不变。AH完整4000文件300634058B已核验，源在out/parity/checkpoint-20261003-ah/source；下一UI增量基点13ff5b9。未向UI会话发送消息、未拷入其AA核心或任何WIP。
- AH主APK87024333B SHA9f3622838c95313cd3f38b5dc76f9254ec367cadd9c38632b4da515e5eea2085，test719653B SHAc38c6993bb0859c5c01c592921a84d0885db6cf22c16bb8bd4fb79af6911c7c0。路径out/parity/ui-integration-13ff5b9/apks；83任务构建成功，168固定资源/4原生输入一致。
- 本侧5554独立实装trade97/98.94秒、tradeSite43/42.45秒、deploy65/47.49秒、transport70/56.37秒、opening62/72.69秒，共337通过。真实交易跨旬、调动到港口后拒绝、出征、三人运输派遣、新局与原250档读写均有证据；运输本套未验送达/返程，五套各恢复起点，不是同一局连续多旬。48附件与逐套原文件备份/恢复/日志齐备；每次7原文件全等无新增，自动档SHA82554269…99f0a9。5580未操作。
- 另AH实际APK生产类ART功绩421/19.10秒、原算术3865行11595/2.51秒通过，独立测试DEX不含生产类；用户文件和APK前后不变。当前5554已结束全部安装测试，保留AH与原用户档，模拟器保持运行。仅API29 x86_64，没有ARM真机证据。opening08b仍显示3D准备覆盖层，不算首帧性能通过。
- round18-installed.json记录337/ART各自范围、精确SHA和48附件；ROUND_18、差异台账、契约已更新。AH后生产模块未改，新增仅能力取证工具/审计与验证文档，准备AI完整资料冻结。
- test_pc_officer_growth.py新增原9曲线×年龄0..120×五组基础/经验/伤病共5445当前政治组合，完整3MiB/RNG对照；原489f10构造三种sentinel只写vtable，不能把缓冲区零当经验/伤病初始化。曲线比例来自原函数，日期设置显式隔离，官职/配偶/特殊700..799槽位及正式载入初始化仍待核实。docs/pc-data/officer-growth-native.json有函数SHA与范围；out/parity/officer-growth-20261003保留探索及正式日志。
- 接下来继续基础能力/经验/当前能力的完整持久化与预览契约、原价格/数量正常提交和月结时序，再做新包验证。现有商人价格/数量/经验未接，官方/MOD生效身份与全玩法差异仍未完成；历史Campaign migration等失败保留，不降断言，目标active。

## 最新重启交接：R19人物原初始化与核心算术完成，新包开局未通过

- 完整AI4003文件300667691B已核验。本轮在其上新增原初始化工具、能力oracle/核心算术和Android验证门控修复；app/API/runtime1027份源仍与AI一致。UI仍13ff5b9，UI17尚无完成提交，不合WIP，不向对方发消息。下一UI增量基点13ff5b9；完整AJ资料/源码封存目标out/parity/checkpoint-20261003-aj/source，以同目录manifest/verified/completion实际结果为准，不能从旧HEAD继承。
- 原48b760在读源前经vtable+20调用488470，明确清五经验与伤病。A5哨兵全16源17600槽位/13600记录，以及首源00/FF两组2200槽位通过；源字段和RNG不变，padding不保证零。officer-initialization-native.json9452B SHA083bf98d…c790。PC只读、无Wine，后续MOD/事件生效仍未确定。
- 新PcOfficerAbilityRules仅为内部纯算术，尚未用于正常命令或存档。原9曲线1143+完整能力3670=4813行直接原程序生成，二次输出字节全等；含经验、伤病、官职、双方内助、无配偶、成长禁用及native700..799边界，非栈写/RNG守卫通过。TSV198215B SHAda507a1b…46b9d，Java4813通过，既有功绩421/商人算术11595通过。666明确映射人物在16源的五成长码均一致，4未映射仍未知。
- 新主APK87024457B SHA8c403c8e4b17e40787c221ebfe51f84328b9a0b604dba981f903f229d42f8449；test736038B SHA7d07866cc1626da28dd9560f4215cacf1bcde0c66c446104584ddf19469d6acc，路径out/parity/officer-ability-20261003/apks。构建76任务成功，168固定资源/4原生输入一致，架构PASS。**候选opening两次未通过**：243.63秒及恢复设备后117.46秒均在UiUxInstrumentation:245等待导航控件超时，各10附件保留。AH冻结双包同环境重跑62项176.17秒通过/12附件。候选两次及AH均恢复7原文件全等无新增，自动档SHA82554269…99f0a9。
- 首轮ART三个数学套件通过但APK后验失败，随后5554持续offline，故总验证失败。原JSON过早passed=true的脚本缺陷已修正并加5种门控测试，原结果保留/erratum说明。定向reconnect、console退出、SIGTERM无效后核验PID结束实例，先保留用户备份和宿主采样，再对退出后的AVD磁盘APFS克隆备份，原AVD无wipe重启；7文件和APK完整回读均一致。5580未操作。不要把恢复后成功回改首次失败。
- 重启后新包ART完整重跑通过：功绩421/2.53秒、商人算术11595/0.32秒、能力4813/0.37秒；用户7文件和87024457B APK后验全等。实际安装生产DEX加载，独立测试DEX只含6测试类/两oracle；这是生成局面命令/save和纯算术，不是UI触控或能力正式接入。APK条目仅classes.dex不同，baksmali确认原1099类全等，仅新能力类增加；开局超时原因仍待定位，不能称基线也失败或新增类造成。当前5554保留新候选/原用户档，无运行中测试，模拟器保持运行。
- docs/validation/parity-20261003/ROUND_19.md与round19-installed.json详记范围/失败/恢复；全部日志out/parity/officer-ability-20261003。只验API29 x86_64，无ARM真机/流畅性结论。AH仍是上一完整五套337项包，不能把它的通过套给新候选。
- 下一步优先定位新局等待超时（执行期线程/时序证据，或UI17完成后顺序集成验证）；并继续人物基础/当前/经验持久化及正常商人先经验+5后报价。SaveCodec33未改，现有学习/仲介/舌战修改五项值，旧档不能反推原base或反扣内助；共享契约已记录兼容边界。完整规则/数据导入与旧地图迁移均未完成，goal保持active。

## 最新重启交接：R20实际线程取证与校验优化，新包159项实装通过

- 继承完整AJ4013文件300928076B，manifest SHA f71e9681…9288ad。上一轮为实际取证/实现/验证进展，非阻塞等待。本轮app/API/runtime仍与AJ一致；UI17还未完成提交，仅只读查看其2D30/3D25等进度，没有发消息、操作5580或合入WIP，下一增量基点仍13ff5b9。
- AJ诊断aj20-opening-trace仍在opening:245超时107.75秒，10附件和7原文件完整恢复证据保留。确认开局后一次SIGQUIT取得有效PID8435线程栈：main在RulesSave.validate的String.matches/Pattern.compile，经SaveCodec.decode/WorldCopies.copy/GameSession.replace/NativeGameHost.install；测试线程等待主线程空闲。采样可能影响时序，只证明该时刻的实际执行路径，不证明唯一根因。原始trace_05及app-logcat在out/parity/opening-diagnostic-20261003/trace。
- 仅改core/RulesSave预编译复用原特技ID表达式，范围/错误/空值与性别校验/顺序均不变，SaveCodec33/API/RNG/事件不变。新包相对AJ反汇编1100类只有RulesSave.smali改变，APK资源/原生库全等。没有改UI等待时限或断言。
- 新主APK87024529B SHA5df0a65caba78f471650b353d97e483d9e8e024649ac3fe2871d6c1162bd25e4；test736038B SHA7d07866cc1626da28dd9560f4215cacf1bcde0c66c446104584ddf19469d6acc与AJ一致。路径out/parity/opening-diagnostic-20261003/apks。构建1分14秒80任务，架构、168固定资源/4原生输入通过，JVM421功绩/4813能力通过。
- 5554新包实际opening62/89.48秒、trade97/119.03秒，共159项通过，分别12/9附件。新开局、真实存取、取消/原局保护与原生host释放；190曹操真实买粮/同旬拒绝/下一旬再卖/读档恢复均通过。两套分别恢复起点，不是连续长局；每次原7文件全等无新增，auto SHA82554269…99f0a9。08b目视仍有3D准备覆盖层，不算首帧通过，单次开局通过不证明历史偶发超时已彻底消失。
- 新SaveCopyProbe及verify_pc_android_save_copy.py以实际安装APK类在ART复制正常190存档。固定测试输入已纳入tools/content/fixtures/normal-ui-190.sg11及provenance.json，185898B SHA02ddb3d4…e5d69，源UI完成8e1d5f7；非用户原250档，不打进生产APK。冷读/十次完整复制/合法未知和非法特技共27项通过，逐字节save/RNG不变。独立probe SHAa535c1f1…fb2810。构建结束后串行AJ→新包样本cold0.814→0.310秒、十次copy4.986→1.722秒、20次Rules0.897→0.052秒；最初与构建并行的更慢样本另保留，不用于夸大改善，无ARM/统计性性能结论。
- 新包ART完整功绩421/2.93秒、商人算术11595/0.55秒、能力4813/0.57秒通过；六测试类独立DEX仍ec32a3d7…c5569，用户7文件及87024529B APK完整后验全等。当前5554无运行中的安装测试，保留新包与原用户档，模拟器运行；5554启动exec95544保留，5580不碰。
- ROUND_20、round20-installed.json和差异台账/契约已更新；完整AK冻结目标out/parity/checkpoint-20261003-ak/source，实际manifest/verified/completion为准，包含全部dirty成果与新探针输入。不要从旧HEAD或只取一个jar继承。
- 下一步继续能力基础/成长/经验/官职的明确持久化与正常商人先经验+5后报价，或UI17完成后按13ff5b9增量顺序集成并重验。成长/完整交易价格数量、16源正式生效和全内容导入仍未完成，旧地图存档迁移及历史规则失败保留。PC只读无Wine，总goal active，不把本轮校验优化当玩法对齐完成。


## 最新重启交接：R21原官职来源接入，AL候选实装127与ART通过

- 本轮从完整AK继承，没有reset/覆盖。原共享Scenario.s11的81×46官职由原serializer和命名getter确认，16份type22源此表不消费字节。新export_pc_officer_ranks.py执行891读取、完整3MiB/RNG无改动，二次输出TSV/审计字节一致。TSV10262B SHA78ce6ffb…f1288，审计20366B SHA1973144c…e097，显式桥接80旧ID和native80無；不是根据ordinal猜身份，不宣称MOD启用。
- Government读取固定SHA来源资源，80旧ID/顺序/统兵/功绩/月俸与AK已编译基线逐项一致；正常任命、出征上限、月俸真正使用新表。新增nativeId/abilityStat/abilityBonus只描述来源，能力持久化尚未接；requiredTitle旧工程门槛和无官职薪俸5的应用仍待查，不静默改旧存档。SaveCodec33、API和事件不变。JVM官职2244/爵位540/能力4813/功绩421通过，架构通过；原AL/EAX getter失败、剑库存fixture失败和漏JAVA_HOME门控测试失败保留，修正原因不削弱断言。
- 新冻结双包out/parity/officer-state-20261003/apks：主87030287B SHAd47da9b4ec8d7394a609681bccaa513387da44c5c4d888844ed0637be3e192f1，test737034B SHA4b5ff8c3437e0f134cded374ced36e218038ea0ca5fd325c84a898dd66da00ef。24秒78任务构建，168固定资源/4原生输入及新表一致。5554真实安装/回读，opening62/69.85秒和deploy65/44.64秒共127通过，23附件；两套独立恢复，不是同局连续多旬。原7文件全等无新增，自动档SHA82554269…99f0a9。08b仍3D准备层，无首帧/流畅性或ARM真机结论。
- 实际安装包生产类ART：功绩421/1.81秒、商人算术11595/0.39秒、能力4813/0.40秒、官职2244/4.49秒全部通过。官职覆盖每项任命/出征/真实月俸/保存后三旬重放，独立DEX只7测试类+测试夹具，无生产类/官职表。用户7文件和全APK前后相同，probe090eb5c3…3910；5种门控回归通过。无运行中5554测试，当前留新包与原档，5580未操作。
- app/API/runtime1027文件与AK一致，UI17仍在重新验证保存优化后的自己包，没有完成提交；未发消息、未合WIP，下一增量基点13ff5b9。准备完整AL冻结out/parity/checkpoint-20261003-al/source，以manifest/verified/completion结果为准；可继承完整当前成果，不可仅从旧HEAD。下一步继续人物基础/当前能力/经验持久化及正常商人价格数量、或顺序合入UI17完成增量。全规则/全剧本与历史失败仍未完成，目标active。


## 最新重启交接：R22能力模式/年龄来源闭合，AM新包实装与ART通过

- AL4029文件301219621B已完整校验，manifest a1b21e8d…a1f7。本轮在AL上新增export_pc_officer_date_oracle.py、原日期TSV/审计及内部日期算术；app/API/runtime1027文件逐SHA不变，无reset，未覆盖UI17。
- 原设置实际标签/控件绑定5460d3/546160及事件分发表545350确认能力變動：11d有效→0，11e無效→1，配置+8→原新局4a4373→global+28。4a443b在global+18非零时强制禁用年龄曲线；六组startup完整3MiB/RNG/写地址通过。此开关不关闭经验/伤病/官职/内助。用户选项默认值未推断，UI原片段在平台设置持久化前停止，没有启动Windows/Wine。
- 16个global header从A5哨兵经原serializer重新解码，16190四字节是固定年龄标志；SCEN007/Scen013/Scen014为1、其余13份0，逐源SHA与既有元数据全等。原年龄固定使用起年，非固定按360日/10天旬算当前年；不会将负年龄钳0，不可直接用现Lifecycle.age。未由名字推断工程heroes/官方/MOD身份。
- 新原date oracle1040行29093B SHA7d4ba634…b1514；审计9888B SHA2f56032c…10954；独立二次输出字节全等。PcOfficerAbilityRules新增age/growthDisabled，JVM原能力4813+日期3120/官职2244通过，架构及验证脚本5种门控通过。仍是内部算术，**人物基础/经验/当前值持久化与正常商人价格数量未接入**，SaveCodec33不变，不能宣称玩法完成。
- 新包out/parity/officer-state-20261003/growth-settings/apks：主87030395B SHAd0187e6330cebc4d16eadc3264338f01cc0b907588192b7c9bdfd294fa692dd3，test740592B SHAac804a8ae85027c302dd3333d1d1ba108c7020a370e7b7414e61bcef7f7e0f08；29秒80任务构建，168资源/4原生/官职表一致。5554实装回读双SHA一致，opening62/87.99秒通过，12附件；7原文件恢复全等无新增，auto82554269…99f0a9，08b仍准备3D层，不算性能结案。
- 同一实际包ART功绩421/1.66秒、商人算术11595/0.28秒、能力4813+日期3120/0.28秒、官职2244/3.50秒通过，独立测试DEX无生产类，7用户文件和全APK前后相同。完整round22-installed.json。5554当前无运行中测试，保留本包与原用户档；5580不碰，无ARM真机结论。
- 完整AM目标out/parity/checkpoint-20261003-am/source，以同目录manifest/verified/completion为准。下一步优先真正接入人物状态，已消除年龄/成长开关语义未知：原base/curve/XP/缓存和sourceNativeId分别保存，旧档不反推base或扣历史内助；所有写入入口Campaign.study、AbilityResearch.setStat、Editor/CustomOfficers、Relations.mediate、Contests舌战必须一起处理，不能只改交易。UI17最后compact cursor9111b175-826e-4aff-a0a3-2c4ed53978ec:41，仍活跃（新版真实行军69通过，2D/3D收尾中），未发消息/未集成WIP，下一UI增量基点13ff5b9。总目标active，历史失败与全数据差异仍待。

## 最新重启交接：R23人物状态接入，实装123和ART通过，横屏交易失败保留

- 继承完整AM，app972份输入不变。新OfficerAbilities把base/growth/XP/current分开，666核实人物在16源10656记录成长码一致，二次输出字节相同，成长TSV18190B SHAba89f918…7721；宋宪/徐荣保留279,333集合。新局v34，旧v31—33维持旧数值模式写v33，不反推base。普通商人政治XP+5/3000、任免官职/内助/伤病/年龄/生命周期已真正接入；编辑培养写base，显式来源导入绑定成长，自建同ID不冒认。价格数量月结、其他经验、原培养奖励仍待。
- 原47a630→488430排除正常+17c=0的6未登/8死亡，72组/360输出及整3MiB/RNG/写守卫、二次输出一致；未知+17c用途不编造。JVM状态29361/typed168/官职2244/原能力4813+日期3120通过，资料890/自定义461/地图人物组合17通过。ContentTest保留精确base并增current原曲线断言；官职无资源加载、旧格式迁移和培养太守三失败与冻结AM对照，未降断言，全套未绿。
- 冻结AM jar SHAa4a9d20e…fc09生成JVM旧夹具，AM APK类直接在ART生成v33夹具1657B SHA9a2bc71c…a9e6，无安装变更；仅CRC/GZIP OS平台字节不同。两原夹具/生成工具/来源JSON保留，同环境严格完整字节比较，无归一化。
- 修正版双包out/parity/officer-persistence-20261003/apks-final：主87039626B SHA238b50470780da6e2302908334688ea3244a9c0f4268bdcdb20daf9d03ae1f7c，test741291B SHA82c6ac2ceb568a61e127785d467f36d7705234f6379cc5441cdc5bb159f6d014。57秒83任务构建成功，168固定资源/4原生/两来源表全等。5554实际opening62/94.31秒，新v34兵舍→多旬完成→征兵→训练→读档61/78.31秒，共123通过，各自恢复起点。v34 trade27.08秒在横屏键盘可见性失败，尚未提交，FAIL没有键盘/输入仍1000；失败与恢复保留，UI17后重验。首候选b63a…ac2b的opening62/141.24秒仅历史独立证据。
- 修正版实际APK ART功绩421/2.34秒、商人算术11595/.26秒、能力4813+日期3120/.34秒、官职2244/3.07秒、状态29361/8.31秒通过，probe6946fe32…bec7含8测试类/6夹具无生产类/来源表。由同APK正常ART生成190曹操player1 seed23 v34，236968B SHA636854cc…aaed作本轮实际玩法输入，非旧档推算。每次原7用户文件全等无新增，auto82554269…99f0a9。
- 中断后模拟器退出，原AVD无wipe重启；adbd最初shell使首ART备份读取停止，恢复root后7文件与全APK相同，工具增加root前置，5种门控通过。新5554 exec95471保持，不因观察超时关闭；5580未操作。当前无安装测试运行，实际包与原用户档保留。仅API29 x86_64，无ARM真机/流畅性结论。
- 完整AN目标out/parity/checkpoint-20261003-an/source，以manifest/verified/completion为准，含全部dirty/资源/原生输入。UI17收到完成c03174e36597f0d445bcbd97c4aed2f3f3904b5d（基13ff5b9、19文件增量、自验129），未发消息/未合WIP。下一步先冻结AN，再仅顺序集成app/Instrumentation/docs/progress并最新规则重建实装，不能覆盖新core/API/runtime。ROUND_23、round23-installed.json、台账/契约已有详细范围，总goal active。

## 2026-10-03 UI第十七批：地图取消与真实后台恢复

- 独立UI目录以13ff5b9为基点，继续冻结AA396/v6 init和402项companion；root受保护504、638初始资源与对应原生库SHA全一致，不写原目录/5554/规则/数据/内容工具，不启动Wine。
- 2D任一触点跨面板/可见边界时整次手势取消，失焦/暂停/禁用/移除清理惯性与未完成拖放；3D小地图出现多指后锁定取消，后台/退出清理识别器。MainActivity暂停用同一份成功原子保存字节计算UI恢复hash，消除同回调二次capture，保存失败不更新提示，保留正式状态所有权。
- 同一最终生产app03：p17-23地图32、p17-24原生27、p17-26 2D行军70，共129项真实检查通过。实际多触点/缩放/旋转/面板滚动与越界取消、惯性→系统Home→真正前台恢复、3D退出engine=null/视图移除/宿主数恢复；出征/非法合法行军/取消/唯一提交/实际下一旬/暂停加速跳过/横屏战报详情/完整读档。23/24 test-app03c；26 test-app03d只新增行军明确2D前置，地图套件未变。
- 失败全部保留：05/06实际旧手势缺陷、缩放跨度不足/同步滑动未触发惯性、旧Home未证明后台、旋转输入冻结、早期45秒回合等待超限、Home保存过慢。21状态断言过早而恢复截图仍桌面，最终补焦点+新帧重验；25从3D恢复后投影/点击坐标变化未打开行军预览，正常读档恢复，此3D选点待单独解决，不由2D通过覆盖。
- x86 app-uiux-run03.apk 82,271,612B SHA81f34813c577b72f08ca1fdda07a7b7b98840dd2c99df53e546def9780570ae8，已安装5580且整包回读一致；ARM64 82,214,453B SHA9e19b7701976baa5710895b528866c99bdc5de0d7f891d19c03403a47cbe24dd，仅构建/ABI/资源核验。均version159/source13ff5b9+dirty。三用户文件恢复原SHA，未清数据；设置只恢复每次开始值，不能声称所有设置与整批最初一致。
- VALIDATION-17.md/evidence-17和iteration-17保存源码、APK、companion、全录像与失败。最终23/24/26视频35.361/84.742/81.487秒、各实际顺序解码3帧；旧05/06对照16.732/43.618秒。129是检查数，不是独立流程数。
- 最终Home暂停失焦510/2491/1324ms，前台焦点/新帧恢复4099/2563/2996ms，pause完成395/1720/1223ms；非受控性能基准、非ARM结论。onSaveInstanceState仍另存。26 compute10517ms、TURN_END total3455ms来自activeMillis排除暂停，现有“本旬耗时”口径待修，不能当墙钟耗时。性能目标尚未通过。
- 接着优先3D行军投影/恢复模式、纠正耗时口径、三指/部队拖放跨面板、进程销毁/冷启动/长时稳定；舰船制造/任务管理/高级外交与异步预览契约继续。仅交13ff5b9后UI/测试/docs/progress增量，由玩法会话在其新规则上集成重建，禁止覆盖新core。总goal active，全部目标未完成。

## 最新重启交接：R24已顺序合入UI17，组合包v34交易99/2D32与ART通过

- AN4050文件301395977B已全核验，manifest4dfb6cc8…ccae2。仅13ff5b9→c03174e完成19文件增量顺序合入，root原文件与UI父提交逐项一致，progress仅追加；666份核心/API/runtime/工具/PC审计SHA不变。未发消息、未导入旧AA/WIP、未操作5580。来源patch和integration.json在out/parity/ui-integration-c03174e。
- 组合主87040486B SHAbb6d8d1ceaa758d13a8ed087531950d6fbe340fdf7723dbf01b3315252db07d8，test751443B SHA0baf442df7a51a0d3692cacfe9e549a7e7e0ba767cd4d3e5fc86fbc5bd6cecc3，封存同目录apks。26秒82任务成功，状态29361/typed168/架构与168固定资源/4原生/两来源表一致。
- 5554实装整包回读后正常v34 190曹操：交易99/85.94秒（新runner两个旋转真实输入恢复检查，不能沿用旧97）、2D手势/真实Home恢复32/31.75秒，共131通过，各自恢复起点。R23横屏失败保留，重验不证明唯一原因。组合包生产ART功绩421/3.28秒、商人算术11595/.21秒、能力4813+日期3120/.21秒、官职2244/11.11秒、状态29361/12.64秒通过，probe6946fe32…bec7，正常生成v34新局SHA636854cc…aaed再次字节全等。
- 所有7用户文件和全APK保持/恢复一致，无新增，auto82554269…99f0a9。此中间包尚未验opening/mapNative/march/governanceArmy，不借用R23或UI5580的通过。仅API29 x86_64，无ARM真机/性能结论。5554 exec95471运行，当前无测试在跑。
- UI18完成0ddedf6fd379b3a87df206bc1d09f0e1662155ab已收到（基c03174e，16文件增量，3 app+Instrumentation/docs/progress），修正墙钟计时并核实原生行军测试投影/命令时序，UI独立150通过不能移用。下一步先完整AO冻结，按c03174e→0ddedf6仅取完成增量，保持AN核心，最终包继续实际新局/2D与原生行军/多回合/保存验证。完整AO目标out/parity/checkpoint-20261003-ao/source，以核验文件为准。ROUND_24/round24-installed.json记录范围，整体goal active，价格数量/官方内容/历史失败仍待。

## 2026-10-03 UI第十八批：原生行军补验与实际经过时间

- 继承独立UI完整c03174e成果，继续冻结AA396/v6 init/402 companion。原目录只读核对最新AM交接，不写其进行中的人物状态/规则/数据/内容工具，不操作5554。504受保护文件、638初始资源、对应原生库和三用户文件逐SHA一致，无Wine。
- 核实p17-25：测试在命令布局前选点、横坐标用零高度而纵坐标用地形高度。Instrumentation先实际进入行军，统一真实高度投影、可见区/小地图排除、只读地形拾取同格后实际点击。第17批原包p18-02 3D完整74项通过，证明此失败来自测试投影/时序，未伪称修复新产品缺陷；更多地形目标组合仍待。
- NativeGameHost在beginTurn前记录单调时钟，TurnWork另存wallStartedAt，原activeMillis/8秒预算时钟保持原逻辑。totalMillis/lastTurnWallMillis现在计真实经过时间（含暂停/后台），日志另记activeMs，ClientState摘要明确两种口径；顶部入口只显示完成与查看战报。摘要当前存于UI恢复状态，未增加耗时弹窗，不声称摘要已有独立UI入口。
- 最终同一app/test-run01：p18-03 3D77、p18-04 2D73，共150检查通过。真实出征/非法合法目标/返回取消/双击唯一行军与回合、Home及前台焦点/3D新帧、暂停加速跳过、空战报检索/横屏详情、正常manual3全捕获/RNG恢复。真实暂停区间与独立elapsedRealtime验证总时长包含暂停/计算并位于实测墙钟范围。
- p18-01因5580离线未启动，安装/runner拒绝证据保留；同一AVD按原host/no-window/2048MB重启，无清数据，恢复前检查原三文件hash。最终x86 app-uiux-run01.apk 82271732B SHA50d9509e7b34757294c64ab8243601194489fc602c08213f58f58670658b9d5c，5580安装/整包回读一致；ARM64 82214573B SHAe0b9b31f620f91c338d1a60b92e8229b20ec63ede926a7eb1c0a5d0db6220354，仅构建/ABI/资源核验。均version159/source c03174e+dirty，无ARM真机结论。
- 最终3D total21674/观测暂停18610/compute10821/active2175ms，2D total15855/暂停12251/compute5407/active2891ms。运算和暂停可并行，不能把列相加；旧日志total=active意义不同，不能混作性能前后。3D Home422/前台4763ms，2D Home203/前台4868ms，仍有数秒恢复，不算流畅性通过。三用户文件hash全恢复，速度恢复开始时2×，mtime不保证。
- docs/uiux/VALIDATION-18.md/evidence-18及iteration-18保存完整包/源/依赖/录像/性能。改前92.865秒、最终83.315/61.211秒，每段顺序解码3实际帧，含系统旋转过渡，稳定横屏直接截图已核对。架构和正式APK内容检查通过；源码归档配402项companion，不覆盖玩法新core。
- 17已提交c03174e并交玩法会话；18仅交c03174e后的三个app文件、Instrumentation与docs/progress增量。后续三指/部队拖放跨面板、进程销毁/冷启动/长时资源、舰船制造/任务管理/高级外交、首次权威预览与暂停保存延迟。最新玩法仍在AM人物来源/持久化工作，需在其新规则上集成重建再验证。总goal继续active。


## 2026-10-03 R25：城市强制位移规则修复、继承核验与新包301项实装

- 接管独立f1b8目录，以完整AP的4078文件/329563797B逐SHA继承，冻结a6b87bd；保留全部dirty/UI18/v34/168固定资源/4原生输入，不复制旧AA、不清模拟器或PC资源。工作分支codex/parity-rules-integration-20261003；原app文件尚未由本侧改动。
- 真正War.tactic ADVANCE在原冻结Displacement把敌军11,6→10,6→自身城格9,6；修复共用stepError强制落点据点拒绝，新命令停10,6。完整七格、各归属/18外缘、两步停靠、熊手/突破/击破跟进/陷阱重验/占格/边界与AI统一；普通友城通行/主动进驻保留，旧城格档不删除迁移。未改原损伤/碰撞/单挑/RNG公式，PC精确阻挡语义仍待核。
- JVM城市6648/会话19、实际APK生产ART城市6648/38.27s/会话19/.54s通过；同一APK状态29361、官职2244、功绩421、原商人算术11595、能力4813/日期3120通过。666人物16源10656成长记录二次转换TSV/审计与继承全等；v34正常ART开局SHA636854cc…aaed全等。六类旧首失败与冻结AP一致，全部历史断言保留，不声称全套绿；位移后五章原断言独立通过。
- 新R25/UI18主87040454B SHA335c37431b4ffccbbaa766950b2ecf34cb44a59d9002a511c0fdc55cf95f6e40，test752267B SHAc6476d6136054e2b558e467127b725212b10c46e2411a5b18336564407e08d36。85s/78任务构建，168固定资源/4原生/两来源表和原签名全等；APK source=a6b87bd+本批dirty规则。仅5554 API29 x86_64，无ARM真机结论，未操作UI独立设备。
- 同一新包真实opening64/130.46s、native march77/204.70s、v34 trade99/94.90s、兵舍建设→多旬完成→征兵训练→读档61/82.85s，共301检查；各套独立恢复，原7文件全等/无新增，auto82554269…99f0a9。首opening漏传reuseSlotSha失败保留，补齐原参数后严格重验；不是产品修复或断言放宽。所有截图/完整tar/失败/结果和APK在out/parity/city-displacement-20261003。
- 共享只读地形、StateToken、提交事实与声音去重契约已登记DISPLACEMENT_PRESENTATION_CONTRACT.md；两个规则/会话注册测试和可复现脚本、仅含测试DEX的ART工具已加入本侧范围。另一会话仍在完成纯3D/占格入口/声音/废弃2D，未合WIP；最终组合包必须另重建实装，3D地图点击突进与音频可听输出仍待组合验证。原商人价格数量月结、其他经验/培养、16来源官方/MOD生效/完整开局和P01—P10/D01—D03继续未完成。总goal active。
