# 2026-10-03 玩法与数据持续验证（未完成）

## 顺序集成与安装

先封存完整脏树 `out/parity/pre-ui-test-integration-20261003/source`：3535文件、207711304字节；再逐字接入UI目录已经完成的三行PcPresentationsInstrumentation selector传参修复。基线内全部app文件重新比较，只有该测试文件发生变化；UI目录未被写入。明细integration.json、app-delta-check.json。用户已授权由本会话顺序集成；没有跨会话发消息。

主包与测试包Gradle构建成功。年龄包已实际覆盖安装，安装后从设备读回主APK与候选SHA相同。`tools/content/verify_pc_age_install.py` 明确指定emulator-5554，外部root tar备份全部files/shared_prefs，每例后force-stop再恢复并逐文件核对；没有清数据，不碰UI设备5580。外部自动存档仍168846字节、SHA82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9。

12个年龄案例持续运行，事实以out/parity/age-installed-20261003/all-six/results.json及每例原始txt为准。已通过刘备年轻/年长137/137、关羽年轻/年长134/134、张飞年轻133。刘备年轻step1截图已实际查看到源立绘。验证为受控暂停/源selector/GPU图层/严格背景颜色/生命周期/完整权威RNG存档，不替代连续播放帧时、ARM或PC视频。累计外部report.txt可能包含旧追加记录，pc-presentations-failed.png可能是旧文件；每例新生成的instrumentation txt才是本例结果，不能将通用失败截图当本例证据。

本年龄APK的生产代码冻结在本轮Governance修复之前；后续修复及UI完整集成仍须重新构建安装。不要把年龄包结果扩展为后续新代码已安装通过。

## 原数据工具

人物13600条（16×850），10656身份确认、64扩展字形隔离、2880目录外槽位，17能力/适性向量差异。五项人物测试通过。

原连续读取4496条宝物/势力/军团/城关港；共享Scenario.s11的87名称独立映射项目ID。原军团→势力getter已执行，含指针系统API的VM权限检查，前后3MiB域状态不变；五项域数据测试通过。五份日期对应的重建局比较434可识别据点，96处归属差异及1未知；详见docs/pc-data/scenario-ownership-differences.json。

原元数据16份名称、日期、说明及完整原读取前缀已保留；90→16183→16194→17760原循环独立证明人物起点，两项回归通过。Scen015内嵌ID6与Scen006重复，不能合并。全部确定性gzip和概要在docs/pc-data；README列出再现命令和未知项。安装目录1805文件2780244569字节重新SHA核对无变化；没有Wine。

## 实际旧格式续玩发现与修复

用历史完整core写入器v31提交d352f48ca3e1a5e15624ef8c2e6959b6327edaaf、v32提交9f496f6b362321491aecc5dce18ed0a3f88af9dc分别生成9剧本×4快照，共72份真实旧格式；不是改当前文件版本号。生成器LegacySaveFixtureWriter.java，SHA/来源记录out/parity/save-compat-20261003/manifest.json。

36份v31在当前读取器各续六旬、每旬重新保存读取、对独立路径完整状态和RNG比较全部通过。v32首份central-mobile-sandbox第1旬出现路径分歧，同一失败在完整继承工作区基线独立复现。

原因：旧爵位原始缓存{0=1,1=2,2=2}低于当前有效爵位{0=1,1=4,2=3}；write保存有效值，但原直接续玩还会产生晋爵日志，而save/reload路径不会。Governance.read现将读取级别规范为max(原级别,既得级别)，不降低既有爵位，不改变存档格式，不靠写档或预览触发晋爵。真实旧档作为core/src/test/resources/save-v32-central-native.sg11固定，附原写入器提交/SHA。新LegacyGovernanceSaveTest24检查与RulerTitles540检查通过，断言保留完整日志、reports、RNG字节。36份v32全量六旬复验已在固定的修复后classes中完成，792检查全通过；日志out/parity/save-compat-20261003/v32-fixed-all.log。

## 仍需继续

UI会话送达docs/uiux/RULE_UI_CONTRACT.md：出征完整权威预览优先，建设工期与运输摘要随后。出征预览/typed提交已实现并在主机验证，见architecture/PARITY_UI_CONTRACT.md；UI尚未消费。建设工期与运输摘要仍待实现，不能让UI复制公式。完整UI仍未集成；当前31个历史整套失败以此前独立基线结果为准，后续改动需重新检查。原库存/军队/设施/关系/特技和MOD实际激活、官方新剧本接入、全规则对照、多回合完整UI、ARM与性能均未完成。目标保持active。


## 出征接口与新候选包（尚未安装）

DeploymentPlanTest468、DeploymentSessionTest331通过。39个原部署命令在重构前冻结classes与当前classes的结果文字及完整存档SHA全等（out/parity/deployment-20261003/{baseline,current}.txt）；static架构围栏通过。CityMovement63Test1066通过；CityFootprint55Test仍在“no local around-gate land bypass 潼關”失败，同一夹具在重构前冻结core也产生相同异常，未修改原断言。

新主/测试APK构建成功，已单独封存out/parity/deployment-20261003/apks，manifest记录SHA和installed=false。主包32628ef2684ef3efa72db6a7882de128f555bc746cc0839ce7e207ffb034b727，测试包82c0695f430d872a400f5f0a5149b427d90861d85fbcabdfdae64c288a23e16e。包含爵位修复与核心/API预览，UI尚未调用新入口。年龄专项仍用其最初冻结包，不能混用。

只读正常剧本流程工具PcCampaignFlowProbe新增真实session出征、重复提交拒绝、正常行军和六旬/逐旬读写，全9局验证已完成513项，out/parity/deployment-20261003/campaign.log。年龄累计9/12项通过（新增张飞年长、赵云年轻/年长、诸葛亮年轻）；赵云年长step1实际检查到源立绘。尚未结束的批次不得提前认定全部通过。


完整快照checkpoint-20261003-c含3564文件226187437字节，schema2将两种ABI四份ignored原生库直接纳入原相对路径；另一个独立快照构建53任务全执行成功。主候选与快照重建APK仅META-INF/version-control-info.textproto不同：原目录为GIT revision，无.git快照为NO_SUPPORTED_VCS_FOUND。其余全部ZIP entry内容SHA相等，包括DEX、资源和四原生库；证据apk-reproduction.json。不能称两个APK文件哈希相同。

新增verify_pc_installed_flow.py为下一候选运行既有SceneInstrumentation/GameSmokeRunner提供外部tar备份、实际安装APK读回SHA、失败/超时后的恢复与逐文件核对。仅完成语法检查，尚未运行；需等待年龄批次终止后才可操作emulator-5554，不能与年龄用例争用设备。

## 年龄批次结案、缓存与UI顺序集成

12项年龄安装批次已经结束，11项整例PASS，诸葛亮年长的140项源GPU/严格背景/权威/RNG/save检查通过，但随后FAIL lifecycle restoration barrier（Activity未在10秒内确认销毁），整例仍算FAIL。每例及最终外部恢复原6文件全等，无新增文件，auto168846B/SHA82554269...f0a9。不能删掉最后失败或把11/12写成全通过。后续需独立复验该退出路径；仅x86_64模拟器证据。

出征v2缓存及详情完成：核心570+runtime412，BattleFeedback209+Plot150通过；缓存按完整StateToken失效，私有查询World不会提交或外泄。40次与独立全量复制预览结果、权威保存字节一致；主机首118.702ms、缓存中位0.515ms/p95.967ms，对照完整复制中位58.239ms，Android性能未据此结案。气力/兵装标签/射程/去重特技/战法需求来自core，War/Army共用目标无关校验，没有让UI重写规则。接口契约详见architecture/PARITY_UI_CONTRACT.md。

包含以上改动的778bb1cf5765f9d3f2f146abd85e36ee43c6a61901af3192b1a5f045b9191d99 APK已经实际安装，设备sha256sum等于候选（installed-readback.json）。SceneInstrumentation在site faction flag exists断言失败，203.75秒，尚未到commandFlow出征/移动/回合段；没有声称该综合流程通过。finally外部恢复全部原文件字节相等。源码FilamentMapView仅在snapshot.ground.pcMap==null时建立旧Proxy.flag，与该旧断言有冲突；继承基线80ca.../d128...安装对照已开始，未完成前不把失败归为历史或当前回归。

顺序接入UI已完成六批冻结提交3330ffb（包含前置826d22d/8918feb/1ba9823/77c104b/689af10/d63ebb3）。集成前snapshot-d完整3572文件228089036B含4原生库。81文件预检为79安全增量、1此前已一致测试、1追加progress；无冲突，核心/API/runtime零覆盖，另一个目录与5580不写。out/parity/ui-integration-3330ffb/{preflight,applied}.json记录每个原/当前/新SHA与源commit。UI材料含其57项出征与48项建设触控证据，属于它自己的候选/设备证据，不能替代本合并包实装。

合并包构建、静态架构、982权威专项、app:verifyPresentation全部通过（49012 UI投影、451地形、1136811几何、1168801/898305坐标、11371围城+14overlay）。已冻结out/parity/ui-integration-3330ffb/apks：主d24edf6e84bc08548198e7d626270347244ee282ce71659df1082ae2ab8c7866（86993089B），测试0777eabbf731c8f8895c4ffdfe52a808a0b077e46c68d56be91264b2ee15cbff（665649B）。**这份UI合并包尚未安装**，要等5554基线对照结束并恢复后再安装，覆盖UI opening/deploy/build和后续行军/回合。

### 第五阶段：原战法成本与 UI 合并后实装

原基线 Scene 比较先在 native frame loop running 失败，未到旗帜断言，所以不能算同失败；外部恢复全部原文件。UI3330ffb 合并包 d24edf... 已实装并读回一致，opening 在463.39秒失败于再次打开设置后等待演示默认速度；截图仍菜单。实际保存第三槽、取消覆盖和读取、触感开关、2×设置已经执行，但未达到新开局。所有原文件全恢复，新增只有实际保存的测试manual3，没有删除。

原战法成本链已接入，来源及转换见 pc-data/tactic-costs-native.json。193新规则+982出征+513现有9剧本六旬/存档检查通过；Python原执行块96组成本边界，全部3MiB状态比较通过。扩大回归的Army/CampaignAi/Displacement与继承基线同首失败，全部保留。新APK9b1a7262da198e7745d6634132aed0f8e27bd713d6d179ee975a401c9c9cb246冻结在out/parity/scenario-tail-20261003/apks，目前独立出征安装验证中，不能沿用d24或UI自己的APK结果。

原建设算术新增 test_pc_construction_calculations.py，64设施×8种执行者参数共512组通过原5bb1d0/5bb2b0/5bb2e0，读取字段及全局状态只读得到验证；+c4源变体跟随且相邻记录不变。字段语义、修正设施/科技条件和完整正常建设执行仍待确认，没有把推断当原规则导入。

UI已完成79cdc6b，基于3330ffb的12个文件通过冻结blob/SHA前置检查按序集成，progress只追加；新integrate_ui_snapshot.py可复现。UI的新外交WIP不在本批，不读取其工作树作为交付。每批安装与截图都仍仅x86_64模拟器证据，ARM真机未验证。

## 第六阶段实际结案

71cbc760合并包march240.17秒失败于实际出征未发生，截图仍未选兵种/确认不可用；原7文件恢复全等。相同包诸葛亮年长复验305.76秒，131项源图层/权威/RNG/save检查通过后仍在Activity退出确认失败，整例FAIL；原7文件及最终恢复全等，无新增。证据分别为ui-integration-79cdc6b/installed-march和age-zhugeliang-old。不能合并此前不同APK的成功检查数声称全通过。

外交核心165/runtime468、出征runtime412检查通过，214条重构前后普通命令和续旬记录全等。旧外交设施位置与宿主旧剧本初态失败均匹配完整继承基线，原断言保留。架构PASS；主/测试APK独立79任务23秒构建成功。接口契约见architecture/PARITY_UI_CONTRACT.md；当前UI尚未消费新外交DTO，PC外交公式仍未核实。建设原扣费第三项加入后3项离线测试6.146秒通过，未直接修改工程建设规则。
