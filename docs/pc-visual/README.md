v159 默认演出750ms，用户接受500～1000ms。正常关羽战法暴击152/115、妖术126/121、落雷127/122使用真实源图层接入；此前扰乱误绑定已撤回。历史视口严格整轮PASS284（采样误差0）。新增源帧提交计时/独立源深度相机，主机72378；加载过渡修复后关羽严格136、三类连续提交/权威存档76项通过，~754～790ms。窗口刷新实验落雷3325ms失败后恢复原刷新；当前关羽连续35项/~820ms通过，当前严格整轮PASS482（相机/暂停恢复/4x/跳过/释放/完整存档RNG，背景采样均0）。随机定位抽帧返回不可靠像素，已修正为逐样本顺序解码：实际录像确有原立绘/笔触/光层；此前“缺演出”判断撤回。首帧灰白闪屏已由预热视口参数修复，安装129项/522396像素逐通道误差0通过；正常三类连续81项通过，新录像顺序样本已查看，观察窗口无旧灰白帧；妖术估算1140ms仅通过模拟器采样容差，帧时抖动仍待优化。录像稀疏、原镜头/文字裁切/MOD/其他人物计略/ARM及全范围未完成。未启动Wine，证据见validation-v159-working.json。

v158已安装验证：原远景树木16→32格合批，113→39批、原几何属性7098040项一致；三种正常攻城213项通过，受损原模型/暂停恢复/退出/权威状态与存档检查通过。全国远景原120秒仍失败，软件Swiftshader连近景前置也超时，均保留失败记录。录像稀疏且有港口黑图帧，不能接受完整时序/稳定性。原全屏33调用/347贴图模板和人物视觉表正在核对，仍无正常安卓全屏演出验收。

# 当前进展（全范围目标未完成）

v157已接入K3ST原量化法线、4800—4803第35张RGBA明暗层、SENV地表环境色/雾/淡出，以及4807原背面描边。生产Java与原441d30在观测24bit精度下16组逐位一致，秋冬实际c26/c27和描边draw24参数一致；PC单帧220批纹理sRGB读取均为0。原底色和明暗层先在编码色值合成，再进入Filament线性色彩边界。最终同镜头69项通过，12地区季节视图已取得；1650宽视窗远景原120秒失败108待上传，原v156同视窗也失败138待上传；v157默认竖屏远景失败142待上传，暂停恢复未到达，上传/帧接纳瓶颈继续调查。实际安装与失败记录见validation-v157-working.json；原地形LOD、图集mip串色、其他对象原雾/描边、水面、全部战斗和全屏演出仍未完成。

v156已将144张地面图原64/128/256尺寸和原UV相位接入正式地表材质，实际安装同镜头47项、三地区四季919项、正常攻城223项通过，存档全等；工具可重复，图片/录像/APK见validation-v156-working.json。PC实际法线24批504顶点已核对；随后读取正常地图222批/30张managed纹理，24地表实际RGBA/状态一致：明暗4800—4803第35张、描边4807第0张、网格4804第1张。正常结束战略已取得207-10-1原冬景，33纹理/24地表RGBA及状态通过，几何相机与秋景相同，原渲染线程24bit精度下74组含两份实际雾字段逐位一致，历史72组仍通过；失败证据保留。安卓原地表明暗/雾/描边和其他完整视觉目标仍待完成。

v154已安装原地区气候树种选择，943落点/144选择组合/实际PC33树木一致，正常秋季同镜头38项、三地区四季与远景1045项、正常移动攻击3228项通过，原用户自动存档全部精确恢复。APK/实际图录像见validation-v154-working.json。v155新增关隘港口冬季冷暖原材质，87落点和原1056模型/贴图组合通过，2884217项CPU检查与348模型原始几何检查通过；已构建安装，首轮主机合盖休眠导致超时留证；同一APK/原断言重测PASS2082，20季节视图与三种正常攻城受损原模型GPU切换、暂停/退出资源释放通过，用户存档全等。未通过PC像素/时序或ARM实机验收，原完整战斗与全屏演出未接入。以下按阶段保留历史记录；其中较早“未运行PC/未接入任何特效”的陈述已经被后续阶段替代。

# PC 整合版视觉恢复：持续工作记录

目标仍在进行。v139 原部队与投石台阶段包已归档；v140 已完成 29 组正常移动／攻击指令与存档恢复验证，工作记录见 validation-v140-working.json。v141 接入原静态着色贴图，但远景装载超时；v142 分批地形替换让125个可见地形块完成上传，但远景仍有35项待上传并超时，尚未接受为通过版本。原动态特效、全屏演出和 PC 对照仍未完成。v138 在 v137 原墙体连接及此前地图、植被、城池、设施成果上接入四处源水坝：原半格坐标、高度、朝向和主体状态由实际军事实体驱动，正常攻击后受损或移除。阶段包不是完整还原版。继承 `agent/native-pc-visual` 未提交 v131 成果，未 reset 或更换旧 HEAD 工作区；开始时的补丁保存在 `out/pc-visual/inherited-v131.patch`。

资源证据：`inventory.json` 记录 1805 个源文件、4876 个主包资源的格式、摘要、转换、输出、运行绑定与状态；`placements.json` 保留 1195 个启用对象与 126 个效果记录；`scenery-source.json` 记录本次转换及限制；`object-bindings.json` 记录 EXE 的 388 项模型表与 66 类对象的变体映射（城池/关港贴图/状态已直接核对，其他类型仍待完成）；`FORMAT_NOTES.md` 解释实际字节和 EXE 引用证据。范围进度见 `coverage.json`，运行结果见 `validation.json`。未确认的资源和事件仍明确待查。

本次把 943 条 PC 植被对象、源模型及四季透明贴图接入真实全国地图和裁区。保留现有后台构建、窗口裁剪、分块缓存、源近远 LOD 与资源释放。PC 地图不调用旧 Vegetation 的推定分布；旧存档和自定义地图仍走原兼容路径。城市/城墙/关港的87处主体已恢复80个原模型、15张材质及近远LOD，城内规模由完成内政数驱动、城墙和关港受损由正常攻城耐久驱动。设施库共242个源模型、56张原尺寸RGBA材质，绑定当前core的11类内政/19类军事设施，建设和受损替换实际模型，源农场/兵舍/厩舍按季度选择材质。源四处水坝实体与原主体已接入；原部队14模型、14独立RGBA、75曲线正在接入验证，动作语义与比例仍缺PC对照；归属旗帜、效果及暴击演出仍未完成；现有旧素材明确待替换，不能作为原版验收。

地面无效覆盖层的误读已修复。转换时逐项核对所有 419 万个面角，源顶点材质编号完全一致。仍需校准树木偏暗、垂直比例、UV 密度、环境光照、水面与远景。源纹理/模型接入成功不代表对应 PC 画面已经匹配。

目前实际安装验证设备是 API29 x86_64 模拟器、宿主 OpenGL；没有 ARM 真机证据。已在项目隔离副本/前缀尝试便携Wine，仍未取得有效PC画面，采集缺口及失败证据见 `PC_RUNTIME.md`。安卓正常攻城和设施命令录像用于接入证据，不能作为PC对照合格证据；源动态效果与全范围长时间压力测试尚未完成。

## 重建

本次转换环境为Python 3.12.14、numpy 2.3.5、Pillow 12.3；材质编译器使用 Filament 1.56.0 的 matc（版本号 56）。系统Python3.9没有图像依赖，以下python3须指向该转换环境。工具缓存只放忽略的 out/toolchain。

```sh
python3 tools/content/pc_resources.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
python3 tools/content/import_pc_map.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
python3 tools/content/import_pc_scenery.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
python3 tools/content/inspect_pc_object_bindings.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
MATC=/absolute/path/to/matc bash tools/3d/build_pc_ground_material.sh
MATC=/absolute/path/to/matc bash tools/3d/build_pc_scenery_material.sh
python3 tools/content/import_pc_sites.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
python3 tools/content/inspect_pc_terrain_labels.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
python3 tools/content/inspect_pc_facilities.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
python3 tools/content/import_pc_facilities.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
python3 scripts/test-pc-camera-window.py
python3 scripts/test-native-frame-admission.py
python3 tools/content/update_pc_consumers.py
python3 tools/content/import_pc_cliff_walls.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
python3 tools/content/import_pc_dams.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
python3 tools/content/update_pc_consumers.py
python3 tools/content/import_pc_facility_rigs.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
python3 tools/content/import_pc_units.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
MATC=/absolute/path/to/matc bash tools/3d/build_pc_unit_material.sh
python3 tools/content/check_pc_unit_conversion.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' out/pc-visual/unit-source-reference.bin
bash scripts/test-pc-units.sh out/pc-visual/unit-source-reference.bin
python3 tools/content/update_pc_consumers.py
bash scripts/test-pc-dams.sh
bash scripts/test-pc-constructible-walls.sh
bash scripts/test-pc-cliff-walls.sh
bash scripts/test-pc-facilities.sh
bash scripts/test-pc-sites.sh
bash scripts/test-pc-scenery.sh
bash scripts/test-pc-map.sh
bash scripts/test-3d-interaction.sh
bash scripts/test-native-r04.sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
python3 scripts/verify-map-release.py app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.PcSceneryInstrumentation
adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.PcSitesInstrumentation
adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.PcFacilitiesInstrumentation
adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.PcDamsInstrumentation
adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.PcUnitsInstrumentation
adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.PcFacilityRigsInstrumentation
```

转换和材质构建自动同步摘要门禁；输出应逐字节可复现。`PcSitesInstrumentation -e commandsOnly true`可单独检查正常攻城后的源受损模型。主APK与测试APK必须同次构建；新增夹具使用普通只读类，避免测试端record转换辅助类遮蔽主包。原始安装目录没有写入转换结果。源文件完整性由初始清单与重建后的全文件摘要核对。历史 core 失败的基线核对见 `../pc-map/README.md`，本阶段不削弱断言。

下一阶段继续原城旗、源定义其他设施的实际绑定及崖墙地表初始化修补，完成源区域材质、部队动画、环境与战斗效果和全屏文字演出，最后逐项综合验收。177项清单包括整合版新增的设施定义；没有正常玩法绑定的源设施单列待查。不得把当前截图或阶段包描述为整个目标完成。

v135正常城/关/港三次攻城和农场/阵六次建设、受损、摧毁已经安装验证并保存连续录像，结果归档validation-v135.json；原120秒全国准备断言保留。v136将原静态墙161个柱与154段连接按源EXE矩阵接入，安装255项近远/春冬/生命周期和SG11字节不变检查通过，源码/资源/截图/阶段包见validation.json与out/pc-visual/release-v136.json。可建土垒/石墙连接已通过正常命令安装验证；源地表初始化修补及PC对照仍待完成。

v137记录在validation-v137.json与out/pc-visual/release-v137.json：16正常命令主机/土垒石墙GPU状态、SG11/RNG不变；三段实际录像须结合封装与解码帧核对，石墙原四命令录像缺末次摧毁，补录单命令明确单列。不是PC战斗/坍塌动画验收。

v138 新开局地图修订65只增加四处原水坝实体，保持源SHEX湿地分类及所有地格、站点和开发位；旧64存档不自动补坝。实际旧64自动存档与继承64代码独立推进三旬，四个存档快照逐字节一致。水坝CPU 3,235,900项、模拟器连续八次正常攻击及近远/暂停/释放1,970项通过；实际用户自动存档最终恢复为原168,846字节。原模型矩阵和GPU输入核对通过，但原溃坝/洪水特效及PC对照尚未通过。

地表分块去除多余临时数组、邻格对象，保留全部顶点/索引/UV/材质输入，五种PC及旧地图窗口超过6800万项输入逐位一致。120秒准备断言未放宽；此前原APK远景超时、测试端R8 API缺失的失败日志及对应包保留。最新整轮模拟器已通过，仍有明显加载/GC停顿，录像采样率偏低且三段间有间隔，不声称全八次连续录像覆盖或ARM性能通过。详情在 validation.json 和 out/pc-visual/release-v138.json。

当前 v139 工作记录见 units-source.json 与 units-validation-working.json。原始皮肤/UV/颜色/三角形/透明分组约405万项独立源校验通过；带反击/骑射的动作边界及29组原全国地形正常指令/存档准备检查通过。GPU测试曾因测试端record辅助类和Filament检查API优化问题失败，修复不削弱断言；第二次检查端崩溃后的用户自动存档从外部备份恢复且逐字节一致。原动作语义、时序、旗帜、原透明排序及PC画面对照仍待完成。

同地点水坝0受损后的远景120秒断言，在未经修改的v138包和初步v139包上均失败，日志及包保留；不以同样超时证明完全同因。新增有界精确水流/岸线采样缓存已通过五窗口完整GPU输入摘要与v138逐位一致检查，尚未声明安卓性能收益。此前初步部队录像主要是加载画面，不能充当正常动作或PC匹配验收视频。

原投石台活动主体的资源2234/2235与曲线73/74已由源EXE调用及表项确认，6骨骼首帧与原静态主体外形相符。29KB独立rig包复用原蒙皮求值，正常/受损模型接入实际FACILITY_ATTACK；建设状态仍使用原静态变体。源一秒准备、60Hz曲线、frames−2结束边界作为独立演示时间接入命令与正常旬回放，不修改core规则或存档。12组独立源姿态131,952项及两次正常旬结算/完整RNG存档核对通过；安装GPU、原投射物和PC画面对照仍待验收。详情见facility-rigs-source.json及facility-rigs-validation-working.json。

v139最新共享缓存以不可变Ground为唯一持有者，所有地形块和细分采样复用两个65536项精确缓存；此前每块/字段重复分配约1.6MiB的问题已修正。五个完整GPU输入窗口仍与v138逐位一致，地图635,299及R04 87,689项通过。水坝0受损/远景原120秒检查最新313项通过；旧v138/初步v139失败保留，不将本次模拟器结果扩展为全国性能或ARM结论。

原投石台正常/受损主体由实际下一旬触发，独立资源安装125项、真实GPU姿态0/16/43/70、暂停/4×、完整状态/RNG/实际自动存档与释放通过。第一轮默认240秒检查因全国AI尚未到设施结算失败，保留日志。第二轮明确 `-e resourceEvidence true` 只用于资源证据，第一事件等待至600秒，不作为默认240秒性能通过；默认窗口不变并另行复测。已有录像实看1秒正常结算后、20秒受损静止、60秒AI计算、90/120秒退出桌面，不能宣称完整发射动画覆盖；后续补录需与真实事件阶段对应。

走舸/楼船/弩兵近战/骑射已各通过195/195/194/196项正常移动攻击安装验证。当前同一APK的29组完整兵种矩阵仍在运行，结果独立更新。特效246个原KSEF图层结构与347张原效果RGBA调查可复现，已找到原人物卡、特技发光字和胜败笔触；诊断贴图尚未接入玩法，当前全屏原演出仍未完成。目标继续进行。

v139同一共享缓存安装包29组完整兵种正常命令矩阵最终通过2,815项，原始整轮日志及每组完整GPU/权威报告为out/pc-visual/v139-unit-all-shared-water-installed.txt及对应report.txt。包括14原地图模型的实际分支、运输、走舸/楼船/斗舰、弩兵近战拔剑与骑射模型切换；正常状态、暂停/4×、退出CPU/纹理释放与完整Save/RNG核对。不是原PC动作含义/比例/透明排序/旗帜或真机验收。原投石台默认240秒窗口正在单独重跑。

2026-10-01 v139 阶段归档：当前 APK SHA256 d1983e9f8e35b07c44f08e7ff164e087d1b75a08c41c0f5dd87913fb1845b72f，29 组正常兵种指令 2815 项、投石台正常/受损默认 240 秒检查 128 项、水坝0受损原120秒远景313项通过；原用户存档逐字节恢复。两个默认投石台录像已抽查正常75/80/85秒和受损170/175秒源投臂变化，第二段编码稀疏，未接受连续时序或PC一致性。阶段包 out/pc-visual/sanguo11-mobile-v139-pc-source-units-platforms.apk；整体目标保持 active，PC像素/ARM/旗帜/原投射物/环境特效/全屏演出未完成。

2026-10-02 v141 开始接入静态对象原绘画材质。新工具 import_pc_environment.py 转换SENV4799与paint4806/silhouette4807，来源见environment-source-working.json；inspect_pc_shading.py记录原着色器/常量；check_pc_environment.py和check_pc_shading.py执行源机器码算术/状态检查。PcEnvironment当前只向静态对象绑定四季默认方向，未应用雾和环境光。pc-scenery.mat以原贴图两次MODULATE2X计算，并保留原半透明/深度写入。轮廓、单位/地面着色、透明混合色彩空间及PC像素对照仍待完成；构建、安装与验收必须分别查看validation-v141-working.json，不能以解码或主机构建替代安装验证。

2026-10-01 v140继续：原全国模式已拒绝旧旗帜、旧火焰/脚手架、旧粒子和通用暴击提示，缺失原演出在界面注明。SWORD正常命令2986项、正常/受损原投石台785项和自定义地图兼容箭矢/暴击48项通过。测试退出时的onPause自动存档竞态已修复并从外部逐字节确认原168846字节存档恢复。29组源兵种完整回归仍在运行，以validation-v140-working.json为准。原效果UV2008程序48192样本已与源EXE机器码核对；347动态文字/人物贴图写入原通用图集32、126条SEFF编号进入原效果表的调用关系确认。原特效发射、控制器时钟、材质与全屏演出仍未接入；PC与ARM参考缺口不变。

2026-10-02 远景失败与调度改进：v141 的 12 个森林四季近景完成采样，但两次 span50 远景在原120秒窗口仍有168／173项待上传，已保存失败报告及用户存档逐字节恢复结果。工作记录见 validation-v141-working.json；失败不能用近景截图替代验收。v142 将每8个完成地形块沿原有有界工作队列交付，保留尚无新结果的旧CPU块，原有GPU替换延迟释放保持有效，几何、素材、120秒断言不变。版本源快照与97项APK资源校验已归档；v142远景／退出失败详见后文。

源坐标机器码检查 `check_pc_coordinates.py` 对256个高度值及1195个有效OBJS对象通过：地形和对象高度均为字节×0.5，对象半格水平步长10。由现有水平尺度0.05推导统一高度尺度应为0.025／字节；当前0.01为压扁的展示尺度。统一转换、拾取上限及包围盒的同步调整仍待实施；这项算术证据没有提供PC镜头或视觉参考。

v142 安装后远景诊断仍失败：同场景120秒时待装载35，125可见地形块全部GPU驻留。已归档原失败日志、近景及失败截图；用户存档SHA82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9逐字节恢复。不能将此诊断写成完整四季、暂停恢复或PC美术验收通过。下一步继续景物交付和GPU上传瓶颈，随后补城池、设施、投石台及正常部队指令验证。

v143 候选将原OBJS景物及原悬崖墙先于地形完成交付，沿用同一有界队列、epoch取消和最终完成语义；兼容路径继续地形优先。构建、97项APK校验和实际buildMeshes控制流主机检查通过；安装需等待v142设施测试退出后进行。原120秒断言保留，不能凭交付顺序推断性能已解决。`test-native-ground-stream.sh`在前置NativeR03Test出现0.11!=0.08；未修改v139源快照复现同位置、同数值失败，源陆面在水位下与max(land,water)拾取表面不是同一高度。断言未改；该套件未到达后续全国字节检查，有界队列和既有phased-mailbox子集单独运行通过。

2026-10-02：v143已实际安装，主包／测试包与设备base.apk逐字节一致。单场荆州冬季近远景诊断通过231项检查：125可见地形块、89林块全部驻留，暂停恢复、资源释放、权威/RNG及存档恢复检查通过；证据`out/pc-visual/v143/evidence-far/manifest.json`。退出有框架pause-timeout警告，但原10秒销毁门槛实际完成；仍需生命周期压力验证。三处×四季完整检查进行中。远景截图显示高度压扁、岸线棱角和地面UV／材质差异仍未解决，不能作为PC图像验收。

地形暂存数组复用候选：`test-pc-terrain-scratch.py`以不可变v143 SceneMesh源码为独立编译基线，对16个真实PC／兼容地形窗口及后续移窗，顶点、索引、材质／切线、网格提示逐位全等，缓存对象和已发布网格未被后续复用修改。主机累计后台分配1484485224→1303563296字节，减少180921928字节；此为桌面分配计数，尚非安卓内存／帧时验收。PC地图635299项几何／拾取／旧存档隔离检查通过。

2026-10-02：v144三处四季近景完成，最后远景原120秒门槛失败pending43；125地形块已全部GPU驻留，89林块仍有待装载。暂存复用未解决完整远景回归；失败报告、25张截图及存档逐字节恢复证据已归档，未减弱断言。

v145统一原版三轴坐标为PC单位×0.05，高度字节×0.025，同步模型／部队／墙体／船只水面落点／高山拾取上限／GPU包围盒。三个静态包版本更新为PCSCN002、PCSIT003、PCFAC002，拒绝旧压扁包；348个模型顶点、法线方向、UV、颜色及索引与原WKMD逐位对照通过。岸线交点保留原水位、采用共享边参数及double反变换，原1e−5断言未放宽；地图635302项通过。此候选构建通过，安装验证待进行；源法线幅值、地面UV、水材质、雾、阴影、动作时序与PC图像仍未完成。

v145实际安装斗舰MOVE/ATTACK完成3259项，但最终Activitydestroy10秒屏障失败；远景诊断也在原120秒准备断言失败，pending37。原日志、24幅斗舰截图与4份远景失败证据已归档，进程退出后的用户自动存档逐字节一致。视频只确认MOVE阶段，ATTACK未录入；98帧/175.655秒含明显模拟器驱动停顿，不能作为PC时序/FPS验收。

v146修复K3ST水位完整8位及原版微偏移。原机器码1020组与全部1048576面独立核对通过，主机地图1687046项通过；规则/SHEX/高度/旧存档字节不变。已构建97资源摘要校验通过，安装及实际水面验证进行中。原水材质、动画、4×4粗水格、全屏和特效播放尚未完整恢复。Activity暂停/销毁增加阶段耗时日志以调查真实退出卡点，未延长测试屏障。

只读镜头调查可复现：`inspect_pc_presentation_cameras.py` 固定源EXE摘要，验证调用方相机、五处正常绘制调用和512B临时相机复制/恢复；内部25度默认值不作为全屏镜头验收。`archive_pc_visual_snapshot.py --output out/新目录 --manifest out/新清单.json --status 描述`归档当前脏树与资源门禁固定的忽略输入，拒绝覆盖旧快照，排除out工具链缓存。当前3508文件/199548600B快照与157包资源校验见v159记录。
