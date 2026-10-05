# 安装剧本数据取证

R14：新增`merchant-turn-native.json`与`merchant-prices-native.json`，分别由inspect_pc_merchant_turn.py / inspect_pc_merchant_prices.py从锁定EXE导出。原87据点逐旬清零与商人bit1拒绝已接入正常命令；价格月表/更新及原RNG已离线执行验证但尚未接入，city+9c条件在R15后续已由属性名称/原读写分支确认bit0瘟疫、bit1蝗災、bit2豐收，48组验证通过；事件发生/持续规则仍未知。详见[ROUND_14](../validation/parity-20261003/ROUND_14.md)。

2026-10-03：已执行所供 san11pk.exe 的原反序列化函数，覆盖安装目录16份剧本的850个人物槽位，共13600条记录；另连续读取4496条宝物、势力、军团、城市、关隘和港口记录。没有启动 Wine，也没有写入 PC 安装目录。

当前获得10656条可确认身份映射，其中 Scen014 的两条必须按剧本重映射；64条（四位人物×16剧本）因扩展字形未解码被隔离。另2880条位于现有670人目录之外，保留为无项目身份映射记录。没有将这些数据直接覆盖现有社区/自制剧本，也不宣称其开局归属或 MOD 生效顺序已经确认。

## 具体发现

- `Media/scenario/Scen014.S11` 的 native 279 是宋憲（157年生），333 是徐榮（147年生），与其余已核验剧本的位置不同。项目 ID 分别是10333、10279，转换必须使用分剧本映射。
- 孔伷、司馬伷、朱雋、劉璝对应记录含 Python 标准 Big5 无法解码的字节。工具保留姓名原字节、完整记录和反序列化后的 actor，不用替换字符或单凭编号猜测身份。
- 正确重映射后已确认身份的生年、登场年、没年没有与当前目录不同的项目。能力向量有11处、适性向量有6处不同，具体源文件/人物/数值见审计 JSON；差异集中在 Scen014、Scen015。
- 能力字段是原48b93e..48b947读入 actor+c8 的5字节；适性是48b923..48b93c读入 actor+b0 的6个值，0/1/2/3与目录C/B/A/S逐项交叉核对。字段顺序已获数据佐证，仍需原规则消费者验证。actor+e8保留为原始特技枚举，不擅自映射项目 skill ID。
- 原48b775分支调用记录所在数组的索引，48b787比较0x352：类型22的剧本只序列化索引0–849，850及以上不读字节。内存数组1100个位置不能等同剧本中1100条数据。槽位700靈帝、729張讓、749商人、800孔丘、849古代５０等存在，不代表可登场或可选。
- 剧本头读取器43b330把文件24/28位置的两个版本值1/2放入stream58/5c。省略这些版本会使势力后续字段错位；工具明确传递版本，未知版本拒绝处理。人物13600条在版本补齐前后解码结果全部一致。

## 势力与据点连续读取

`inspect_pc_scenario_domains.py` 从人物段末尾146960开始，执行原49383f..49391c连续数组循环，每剧本读100宝物、47势力、47军团、42城、10关、35港，到162702结束。原492db0的每个实际构造函数用于初始化每条对象，包括含自引用的容器；未通过复制原型伪造对象。

单线程VM将PE的EnterCriticalSection/LeaveCriticalSection设为无竞争空操作，IsBadReadPtr/IsBadWritePtr按VM映射及权限返回；46ff20回调只提供原文件字节。没有替换游戏规则、枚举数量或资源公式。每条记录保留原文件偏移、长度、SHA、IO返回位置、原字节和解码内存。共享Scenario.s11是类型24（47928字节），原相同循环在90–2310区间实际读取87个据点名称，跳过其他三类表及人物、网格负载。已按名称、类型以及明确的關/港后缀规则与当前sites.tsv建立87个唯一项目ID映射，未单凭数组顺序映射；劍閣保持完整名称。地理和库存消费者仍待核实，不把actor偏移擅自命名为兵力或钱粮。

域数据五项回归验证完整连续字节区间、原数组数量/次序、实际宝物名赤兔馬、交换两条城市原记录仅交换对应对象、重复初始化无VM状态泄漏，以及未知版本/截断/错误EXE拒绝、共享表87名称和类型切换后无状态泄漏。原47b2b0按据点virtual44读取军团、经490ad0查军团、再经virtual40取得势力；47c320/483810及65d6c0原函数参与执行。回归将北平军团4换为3，势力从11变为5；改为-1则无归属。全部归属getter执行前后3MiB域内存完全相等。force+4的名字人物编号由481640→490b00→489690证实，未假定其他领袖特性。失败的未设版本/未接系统指针检查实验保留，不能用作有效解码证据。

五份明确日期对应的当前重建剧本与安装文件，已比较434个可识别归属，发现96处差异；190汝南的孔伷原字形仍未解码，该处保留unknown。逐据点明细见scenario-ownership-differences.json。对照数量：184为31处、190为13处、194为18处、200为25处、207定制为9处。250幻想及三份沙盘没有声明与PC剧本一一对应，未擅自替换。

## 剧本元数据与原读取边界

`inspect_pc_scenario_metadata.py` 执行原43b9b5路径使用的480830构造器、480d50读取器、483120全局头和4937ee..493812网格循环，16份源均得到同一组边界：90→16183→16194→17760。最后一段实际运行16384个原对象读取器并消费1566字节，独立证明人物段起点，不依赖姓名扫描。元数据中的42组城市附加记录保留原字节，未知字段未强行赋义。

已保存16份实际名称、日期和说明。尤其Scen015是279年3月《滾滾長江》，内嵌native_id=6，与Scen006（225年7月《南蠻征伐》）重复；Scen014实际标题为《英雄集結PK新版》。文件槽位、内嵌编号、来源SHA必须分开保留，不能据编号覆盖或合并。此处不因文件名/标题推断原版或MOD是否实际启用。元数据两项回归验证完整前缀区间与重复编号保持独立。

## 可复现工具与证据

在项目根目录执行（Python 3，现有 `out/toolchain/pc-emulate` 中的 Unicorn 2.1.4）：

```sh
PYTHONPATH=out/toolchain/pc-emulate python3 tools/content/inspect_pc_scenario_officers.py \
  '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' \
  --output out/parity/scenario-officers-850-versioned-20261003.json
PYTHONPATH=out/toolchain/pc-emulate python3 tools/content/inspect_pc_scenario_domains.py \
  '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' \
  --output out/parity/scenario-domains-ownership-20261003.json
PYTHONPATH=out/toolchain/pc-emulate python3 tools/content/inspect_pc_scenario_metadata.py \
  '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' \
  --output out/parity/scenario-metadata-20261003.json
PC_INSTALLATION='/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' \
  PYTHONPATH=out/toolchain/pc-emulate python3 tools/content/test_pc_scenario_officers.py
PC_INSTALLATION='/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' \
  PYTHONPATH=out/toolchain/pc-emulate python3 tools/content/test_pc_scenario_domains.py
PC_INSTALLATION='/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' \
  PYTHONPATH=out/toolchain/pc-emulate python3 tools/content/test_pc_scenario_metadata.py
python3 tools/content/pack_pc_data_audit.py \
  --officers out/parity/scenario-officers-850-versioned-20261003.json \
  --domains out/parity/scenario-domains-ownership-20261003.json \
  --metadata out/parity/scenario-metadata-20261003.json --output docs/pc-data
python3 tools/content/compare_pc_scenario_ownership.py \
  --officers out/parity/scenario-officers-850-versioned-20261003.json \
  --domains out/parity/scenario-domains-ownership-20261003.json \
  --metadata out/parity/scenario-metadata-20261003.json \
  --output docs/pc-data/scenario-ownership-differences.json
```

工具校验原EXE SHA、剧本头/长度、每条152字节消费和VM退出位置；完整输出包含源SHA、记录偏移/摘要/原字节、actor字节、已解码字段、候选与确认映射、差异和未知项。输出禁止位于PC安装目录内。

`scenario-officers-audit.json`、`scenario-domains-audit.json`、`scenario-metadata-audit.json` 为概要；对应 `*-native.json.gz` 是完整审计JSON的确定性gzip（mtime=0），可用Python gzip.decompress读取。概要记录压缩包及解压内容SHA。完整报告源EXE固定为 `30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb`。

人物五项回归测试覆盖独立六人证据的96条记录、实际交换编号、扩展字形隔离、截断记录与错误EXE拒绝、原850边界分支和额外人物。`out/parity/pc-installation-integrity.json` 对原1805个文件、2780244569字节逐项复核，无变更。

## 接入前仍需完成

姓名字形映射、原特技枚举、关系字段语义/映射、每剧本归属/登场据点与状态、势力/据点字段消费者、共享表剩余数据、军队/设施等后续记录段、剧本元数据与实际加载覆盖链。Backup、附件和SIRE配置不因存在而标为生效。

确认后的官方/整合版剧本以独立ID进入正常ScenarioCatalog；现有自制包身份和旧存档内嵌数据保持可辨识。仅可在明确来源与映射后接入字段，不能把此取证报告称为完整官方剧本转换器。

## 后续表与共享规则目录（2026-10-03）

`inspect_pc_scenario_tail.py`继续执行原49391c..493b06的14组循环；构造器来源492db0..49309d。每份1967对象，17份（共享1+剧本16）共33439对象。共享读取从2310到47928（文件末尾），其中883非空记录，包含省/地区、64设施、12兵装、10君主爵位、81官职、100特技、36科技、32战法、32地形、400名号用字及98能力研究。空占位名称保留，表中存在不等于可用。其余84国号记录在类型22剧本中读取：每剧本7308字节，162702→170010准确到文件末尾。该表首项实际名称魏/吳/蜀/晉/成/黃巾/漢及对应简介，剩余字段含义未猜测。1000个unit类候选的serializer497270在类型22/24不消费字节；不能据此宣称加载后绝不产生部队，原后处理/事件尚未执行。

原路径有共享规则和剧本的类型分派差异。首次试验错误的大小写glob以及重用Unicorn TB跨过前缀截止地址均保留失败日志；现在在原49391c/493b06边界显式停止，未替换规则函数。shared→scenario→shared重复同VM测试、战法记录交换后仅对应actor改变、32次原4951a0成本getter及修改源字段后getter跟随变化，三个回归通过。原getter4951a0经490c90取战法actor+30，UI/AI可用性消费者5768be/578305/5df171与unit virtual54比较，后者496020取unit+1a；现有Army中某些攻城/水军战法成本不同。尚未修改规则：继续核验执行扣减和修正因素后再接入，不能把全部伤害/命中机制当作已对齐。

完整报告确定性gzip为scenario-tail-native.json.gz（SHA3da18ee79888d66171955e6d9db55968bae29c5e772000732631d4903a5cbe32；解压SHA1fedd777f36f28877a3ab6bd84a0fe7358a8d75307bb9333cb77d20070b8804e）。shared-rule-catalog.json从执行后actor+4的原读取边界提取883条完整Big5名称，无未知字形，逐条记录native编号、表偏移、原记录SHA、serializer和name读取返回地址。只是来源目录，不直接写入运行时，不以数值看起来合理来命名其他字段。PC目录依然只读，无Wine。

```sh
PYTHONPATH=out/toolchain/pc-emulate:tools/content python3 tools/content/inspect_pc_scenario_tail.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' --output out/parity/scenario-tail-20261003/tables.json
PYTHONPATH=out/toolchain/pc-emulate:tools/content python3 -m unittest tools/content/test_pc_scenario_tail.py
PYTHONPATH=tools/content python3 tools/content/export_pc_shared_catalog.py out/parity/scenario-tail-20261003/tables.json --output docs/pc-data/shared-rule-catalog.json
```

`pack_pc_data_audit.py`新增可选--tail；原officers/domains/metadata参数与归档不变。安装目录生效/MOD链和剧本官方身份仍为待证项。

### 已接入运行时：原共享战法气力

`export_pc_tactic_costs.py` 将原 serializer 报告的 table_84858/actor+0x30 字节生成 `PcTacticCosts.java`，每项源偏移、记录 SHA、源文件/EXE SHA 在 `tactic-costs-native.json`。原4951a0用于可用性查询；实际执行58589c..5858b7及5861e1..5861f8调用5a31d0读取相同字节，取负后调用4964f0扣减。5a31d0没有改变成本的分支；4964f0对结果做0..100/120边界约束。

新增离线测试通过原执行块运行32项×3种初始气力，检查整个3MiB局面只有该单位气力字节按成本改变。中性孤立单位没有所属武将/势力；这是有明确边界的扣减证据，不是完整原版战斗仿真。4项 Python 测试21.232秒全通过，原二进制函数没有替换。可选外挂运行时补丁是否启用仍未知。

```sh
PYTHONPATH=tools/content python3 tools/content/export_pc_tactic_costs.py out/parity/scenario-tail-20261003/tables.json --java core/src/main/java/game/sanguo/core/PcTacticCosts.java --audit docs/pc-data/tactic-costs-native.json
PYTHONPATH=out/toolchain/pc-emulate:tools/content python3 -m unittest tools/content/test_pc_scenario_tail.py
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew --offline :core:verifyPcTacticCosts
```

运行时原12种步骑弩战法成本相同；4类攻城战法全部10，水上火矢/猛撞10、水上投石15。此前RAM15/FLAME20/STONE20已改正，正式校验、扣减、失败消息、预览、AI及出征DTO共用。193项新命令测试通过，覆盖低于/等于门槛、真实未命中、重复拒绝、随机数消耗和正常下一旬的存档确定性；DeploymentPlan570和Session412回归通过。UI旧战法列表仍需按接口契约消费上下文成本；安装验证与主机规则证据分开。

### 建设下一项原程序证据（尚未作为完整规则导入）

`test_pc_construction_calculations.py`在原函数5bb1d0/5bb2b0/5bb2e0跑64设施×8种执行者参数=512组合，全局3MiB不变；源+c4单项变更原返回值跟随且邻项不变。执行者原4890a0读actor+0x173；算术结果为max(最大执行者参数+参数和/2向下取整,(c2−floor(c2/4))/9向下取整)。5bb2e0按该结果除floor(3*c2/4)向上取整（原函数零/边界行为以测试为准）；5bb2b0返回+c4。尚需验证实际进度累加、伤病/能力修正、设施等级及到期施工结算。

原5bc2e3调用5bb2e0后建立三名执行者工作；5bc470读取+c4取负，经4ae2a0→486c80/487310进入城市+44/关港+28资源字段。49db10另有同势力0x29设施80%分支，目录0x29确认为軍事府；不能把该战地建设分支直接假定适用于所有内政设施。当前Domestic成本、设施初始耐久、单执行者工期与这些字段/原函数存在待核对差异，未靠改UI文案结案。反汇编在out/parity/scenario-tail-20261003/{facility-build-cost,build-resource-chain,post-tail}.asm。

实际扣费块第三项测试已使用原4880a0构造器和真实虚表执行5bc462..5bc49b：11设施×0/恰好费用/10000库存，整个3MiB仅city+44按+c4扣除并钳制至0。3项6.146秒PASS，construction-debit-tests.log。仍未执行位置检查、完整执行者安排与施工结束，不扩大证据范围。

第四项继续执行原建设菜单600e15初始化及600e25..600e37的设施查表，20行源表8ba670、stride20（6011bf），全部3MiB保持只读。实际菜单ID为31–39、30、40–49，仅基础等级，不包含50–59的Lv2/Lv3。原记录仍含高级设施，之前单看通用许可函数5bb4e0无法判断普通菜单入口；本项补上该边界。4项5.211秒PASS，construction-menu-tests.log。下一步可据此修复普通新建设的满级捷径并生成基础费用映射，仍需分别处理旧存档既有等级、完整施工进度与吸收升级；本轮尚未修改Domestic。

### 基础设施费用与等级已接入

现已使用`export_pc_facility_costs.py`生成64项PcFacilityCosts与facility-costs-native.json。工具同时核对实际EXE/Scenario.s11 SHA、每条原文件字节与记录SHA、actor+c4的原读取值、20菜单项以及11种项目Kind对应的原繁体名称，不能只按编号假定身份。重复生成Java/JSON逐字节相同。

```sh
PYTHONPATH=tools/content python3 tools/content/export_pc_facility_costs.py out/parity/scenario-tail-20261003/tables.json --exe '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版/san11pk.exe' --java core/src/main/java/game/sanguo/core/PcFacilityCosts.java --audit docs/pc-data/facility-costs-native.json
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew --offline :core:verifyPcConstruction :game-runtime:verifyConstructionSession
```

实际普通build从Lv1开工，扣原成本；原工程中的直接Lv3捷径已移除。已有存档等级不迁移，使用修改前完整工作区classes真正建设/推进两旬/再次建设生成的v33夹具，验证已完工和在建Lv3都保留。源写入器、snapshot-i的Domestic SHA、文件SHA在pre-base-construction-v33.provenance.json。新的ConstructionPlan/API预览来自普通命令共享检查，不消耗随机数或ID。

仍未对齐：原三执行者、能力修正、进度累计、初始耐久与修复、资源产出、吸收升级费用/工期及其余9种菜单设施。当前保留单执行者2/3旬与1000耐久，明确为待核实差异，而不是移动端简化结案。建设20AP已在下一补充中核实并接入。

建设AP补充（2026-10-03）：原5bc4b7的6a14提供20AP，调用5b9340沿建筑→城市→军团查找，4a1820/47e3e0只修改军团+2c。test_pc_construction_calculations第五项原指令执行覆盖0/19/20/21/60/255，全3MiB状态精确对比。初始化必须写完整32位city+38军团索引及有效district+4势力；之前未启用/只写1字节的夹具会跳过扣除，原失败日志保留。生成器固定EXE SHA并验证6a14，审计schema2记录常量与执行链。建设已采用20AP；按军团独立AP范围、完整三武将建设与进度仍未完成。

### 原部队载入与武将身份/归属（第十一阶段）

`inspect_pc_scenario_units.py`执行真正1100武将循环和部队修正；16文件16000部队槽位在载入边界均无有效部队，不能推论开局事件/MOD不生成军队。`inspect_pc_scenario_placements.py`执行原军团→势力查询，并逐文件、逐记录SHA连接已核实名字映射。新增13600条归属/原身份标签；location9c仍未扩展含义，未登/未發保留原缩写。没有覆盖项目既有剧本和用户存档。

```sh
PYTHONPATH=out/toolchain/pc-emulate:tools/content python3 tools/content/inspect_pc_scenario_units.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' --output out/parity/scenario-units-recheck.json
PYTHONPATH=out/toolchain/pc-emulate:tools/content python3 tools/content/inspect_pc_scenario_placements.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' --output out/parity/scenario-placements-recheck.json
PYTHONPATH=out/toolchain/pc-emulate:tools/content python3 -m unittest tools/content/test_pc_scenario_units.py
```

压缩工具`pack_pc_data_audit.py`增加可选`--units/--placements`，与既有officers/domains参数同时使用，gzip时间戳固定0。已保存两组原报告与摘要，重复生成字节一致；逐SHA和证据范围见[ROUND_11](../validation/parity-20261003/ROUND_11.md)。

声明归属对照工具（R快照之后新增）：`python3 tools/content/compare_pc_officer_assignments.py --output out/parity/officer-assignment-comparison.json`。读取上述四份gzip审计及五份同日期项目properties，核对EXE/来源/身份审计SHA；不把相同年月认作相同官方剧本。3021归属可比较，其中542不同；另308个有映射人物不在项目名单，全部原状态为死亡；921条身份/势力名称未知单列。详细JSON已保存本目录，重复输出全等；三个跨来源拒绝子案例通过。该308条不是308个缺失活跃武将，location9c与完整登场逻辑仍未映射。
## 原城市命令名称与生产/商人行动力（2026-10-03增量）

`inspect_pc_command_catalog.py --exe <安装目录>/san11pk.exe --output <项目输出路径>` 在固定EXE上执行48f1a0..48f1b0原边界及查表，读取44条命令名称、原ID、地址与Big5字节，输出command-catalog-native.json。不得据此认定所有命令在某剧本启用，也不能推断MOD激活、子菜单或游戏设置。重复执行的世界3MiB不变，输出可复现。原名称包含生产、商人、交换俘虏、请求援军、二虎竞食、驱虎吞狼、婚姻/义兄弟仲介、吸收合并、研究能力等，仍须逐项对照实际规则。

`export_pc_city_action_costs.py` 已扩展生产编号2两入口（5c67a9、5c711b）与商人编号4（5cad11）的20AP参数；完整入口机器码核验见test_pc_city_action_costs.py。四项原测试、60扣费边界通过，所有边界只改真实军团AP字节，非替换扣费函数。PC目录只读；字段/执行片段证据不等于完整命令前置许可或MOD补丁状态。当前Java生成常量和正常生产/买卖入口使用20AP；完整军团预算模型仍待核实。

后续已增加原商人AP许可8边界，五项4.597秒通过。商人报价390边界、提交资源段75边界、42城市×2容量getter三项5.106秒通过。`test_pc_merchant_quote.py`运行方式为`PYTHONPATH=out/toolchain/pc-emulate:tools/content python3 -m unittest tools/content/test_pc_merchant_quote.py`。城市容量生成：`python3 tools/content/export_pc_city_capacities.py --exe <安装目录>/san11pk.exe --java <项目Java输出> --audit <审计JSON输出>`，固定原EXE SHA和getter返回机器码。金100000/粮1000000已接正常容量规则，旧存档超额余额保留而不截断。

`inspect_pc_merchant_state.py`逐源SHA/城市记录SHA/serializer偏移核对16×42个city+7c源值均50，见merchant-state-native.json。该值参与原报价分母，但开局和月份更新未闭合，未硬写运行时价格。完整报价、容量、标志证据边界见[ROUND_13](../validation/parity-20261003/ROUND_13.md)。

W后增加`test_pc_site_capacity.py`：10关35港×5种原势力科技位×金粮2getter共450查询，4.861秒通过；原tech33标签及源记录SHA为擴展港關。当前项目关港金10000→40000、粮100000→400000和这项原函数一致，没有因此改研究规则或宣称兵力/兵装容量已核实。运行方式同上述unittest环境。

商人原标志许可新增256边界，`test_pc_merchant_quote.py`现在四项4.599秒通过。原city+a4 bit1已置位时拒绝后续商人操作，命令提交真实置该位；清零时机未核实。当前项目累计交易数量额度仍为待对齐差异，不能以它替代原标志。该证据不改变已冻结W的生产代码。

## 商人功绩与跨语言算术（R16）

`inspect_pc_merchant_merit.py`导出merchant-merit-native.json，原武将属性表/商人提交/写入函数确认功绩50、上限60000；正常游戏已接入，旧档超额保留例外详见ROUND_16。`test_pc_merchant_merit.py`执行原名称初始化、属性查表及12组原提交全局面/RNG检查。

`export_pc_merchant_oracle.py --exe <san11pk.exe> --shared <Media/scenario/Scenario.s11> --output <tsv> --audit <json>`直接执行原函数输出3865组结果。运行环境`PYTHONPATH=out/toolchain/pc-emulate:tools/content`；Java任务`:core:verifyPcMerchantArithmetic`检查原结果、最终随机状态及消耗次数11595项。没有通过Python重写公式生成预期值。价格/数量纯规则仍未接正常交易。

R18能力字段来源：`inspect_pc_officer_ability_sources.py --installation <PC目录> --prior docs/pc-data/scenario-officers-native.json.gz --output <json.gz> --summary <json>`，运行环境同上。逐源SHA和13600记录对照，原serializer152字节扰动及12组符号宽度确认成长码/官职/配偶真实源偏移。经验和伤病在这段剧本serializer未读入，不能把零填充解码值当源配置。详见[ROUND_18](../validation/parity-20261003/ROUND_18.md)。仅审计，不覆盖已存在的社区/自定义剧本、未确认MOD或旧存档。
