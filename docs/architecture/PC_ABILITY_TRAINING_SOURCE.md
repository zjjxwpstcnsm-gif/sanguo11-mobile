# 原能力研究参数、培养准入与奖励块

2026-10-04。只读原程序离线受控执行，不启动Wine，不称完整PC流程或有效MOD身份已闭合。

来源：`san11pk.exe` SHA256 `30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb`；`Media/scenario/Scenario.s11` 47928字节，SHA256 `dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f`。98行位于共享尾表table_86dd8，每行保留原文件offset、字节数、record SHA、读取字段、native index和实际actor。原运行时表root+86dd8、stride6c；显式执行494f50传入序号，不冒称整个postload注册已验证。

原getter494f80将+44编码映射为次数，前15行实际为5／5／3；原始3／3／2不能直接导入。494fb0返回能力索引0—4，494fd0返回70／80／95；494ff0、495010返回适性索引／目标等级。98×5=490次原getter全部只读，完整3MiB世界及RNG不变，没有非栈写入。两次最终JSON SHA `2850fe6805d0054b4a512aa0d45c50716d68c45e593d704dcd1e6377d3e04841`。

原49dc60 STAT分支调用489180读officer+12a的XP，达到2000即拒绝；随后48a390取年龄成长与XP合成能力，必须小于档位上限。它不使用伤病、官职或配偶修正后的当前值。1080例覆盖5项×3档、年龄／成长输入、档位邻值、XP0／99／100／1999／2000／2100；完整世界和RNG不变。受控例：基础70、curve0、20岁、XP0的合成值67允许；基础69、curve0、70岁、XP100的合成值70拒绝。date-source flags为显式输入，不声称其设置名或默认值已核实。

在真正函数5d91a0中，5d9268至5d92ca的完成奖励块取得同一原能力索引、档位和合成能力，通过4a55a0→48a810写入：`newXP = min(3000, oldXP + 100 * min(5, cap - foundation))`。只执行准入允许的300例；完整3MiB世界对照仅该XP与两处当前缓存变化，基础五项和RNG保持。正常流程中的次数扣减、任务释放及UI位于停止边界之后，没有伪造返回值或绕过原setter。此结果证明奖励块，不能代替整个培养命令和回合完成验证。

当前安卓AbilityResearch写base、按base检查上限、维护独立gains20累计；与上述原语义存在差异。用户交接明确要求继承培养写base，本次保留全部实现和旧档含义，不反推基础值或迁移人物。未来必须以明确的独立版本策略处理新局原培养语义，并保留历史自定义身份、XP及v31—33旧数值；不能用替换目录或静默修改旧档解决。完整费用、研究／培养时间、次数扣减、隐藏选择RNG、AI、失败／取消／失守语义继续核实。

复现（输出须为新目录；依赖只读现有工具运行时）：

```sh
export PYTHONPATH=/Users/paopao/workspace/sanguo11-mobile/out/toolchain/pc-emulate:/Users/paopao/workspace/sanguo11-mobile/out/toolchain/pc-inspect
python3 tools/content/inspect_pc_ability_research_native.py --output out/parity/ability-parameters-new
python3 tools/content/verify_pc_ability_training_admission_native.py --output out/parity/ability-admission-award-new
```

最终观察保存在`out/parity/ability-research-source-20261004`，包括逐行映射、函数字节SHA、世界变更检查、最初准入观察与后续奖励观察。参数名称／描述用于来源核对，不作为完整奖励执行证据。没有修改Android规则、存档、UI或PC资源。

## 完整三类完成函数的受控执行补充

`verify_pc_ability_completion_native.py`已执行完整原非玩家控制势力的STAT5d91a0（480成功／4拒绝）、APTITUDE5da0c0（准入48／完成24）、SKILL5da9e0（准入213／完成276）。每例均从原函数入口返回，并核对完整3MiB世界精确变化及RNG；没有替换游戏函数或注入奖励实现。原构造器、登记序号、势力controller+60=-1及完成类型selector41／42／43是显式受控输入，不声称原启动状态已成立。继承的OS导入按VM内存权限模拟指针探测，临界区在单线程VM中处理；源字节读取用于初始原serializer解码。不能写成“完全无任何hook”。

STAT仅增加XP／两处能力缓存；三类都通过原4819e0／481750更新有限培养已用次数，清除六项任务载荷为[-1,0,0,0,0,0]并清除officer+158。APTITUDE设置原兵种索引对应等级；SKILL写原skill整数ID，已拥有目标不准入，其他特技可在本受控非玩家完成路径覆盖。隐藏native ID≥48的每例显式选择slot0，验证了隐藏计数器地址force+114；没有验证随机隐藏选择或默认10槽内容。每行original record SHA、函数字节、原ID、前后值均保留。最终两次完整报告245798B全等，SHA `fdc8fc19d13fa8d424b57f32d1b30bbd5a9863bcaef95f4311139d3dedcdeb46`。

开始函数5d98d0／5da6e0／5daf10的对齐完整反汇编显示三类都设置officer+158=3，PE变量84ce0c为20；这是静态开始写入证据，不作为完整命令费用、三个正常旬倒计时、取消／失守或整个回合派发已验证。`start-source-inspection.json`保留原字节SHA和指令。当前Android三个旬训练及培养写base保持原交接策略；完整STAT原奖励已闭合，但尚未以新版本策略接入。

历史试验`stat-apt-first`失败由探针把APT行混入STAT循环引起，已修正类别范围，原失败完整保留；没有降低完整世界断言。初次报告关于hook范围的措辞已在最终报告更正为实际OS导入模拟边界，最初观察仍保留。当前证据只覆盖三类完整非玩家完成函数，不等于完整培养系统／官方有效资源身份已经对齐。

来源参数及完整完成观察已随源码固定在`docs/pc-data/ability-training-native.json.gz`（60974B，SHA24d1b979ebb3968291b0a68e3c7ab504453a089e21ef620b2b99039b4cc55f1b），含98行源ID／record SHA／读取字段／函数字节和全部分组观察。两份独立原执行报告再打包与该跟踪文件逐字节相等。打包器强制98行连续ID、三个完成分组完整ID覆盖、490getter及480／4／48／24／213／276检查数，固定gzip头与mtime，不降断言、不生成导入值。

```sh
python3 tools/content/verify_pc_ability_completion_native.py --output out/parity/ability-completion-new
python3 tools/content/pack_pc_ability_training_evidence.py \
  --parameters out/parity/ability-parameters-new/native-parameters.json \
  --completion out/parity/ability-completion-new/native-completion.json \
  --output out/parity/ability-training-reproduced.json.gz
```

历史倒计时初探（当前已由下述受控派发证明补齐，失败保留）：受控初始化1100个原officer构造器后，实际59a862→59a89d尾循环观察到目标158字段3→2→1→0，RNG保持。随后599ff7→59a00c的准备派发探针三次均未改变XP或task41；未验证其准备输入／派发边界，所以不能把三个计数下降称为正常培养完成。`ability-countdown-source-20261004/{first.log,observation.json}`保留结果，后续需指令追踪解释；与完整完成函数480／24／276的直接入口证据明确分开。没有改Android任务或保存。

## 原倒计时、门禁和派发的当前证据

`verify_pc_ability_countdown_dispatch_native.py`实际执行原59a862→59a89d全1100officer尾循环、599ff7→59a00c准备条件、原5b9e10任务派发及三类完整完成函数。98个原研究ID各3次全局计数下降，共294次；前两次没有奖励或次数消耗，第三次原派发准确完成；任务释放后再扫描98次均不重复发奖。逐步完整3MiB世界精确对照，包含XP／适性／特技、有限次数、任务载荷与能力缓存，RNG保持。隐含的前置全局回合逻辑和人类UI没有执行，不把尾循环串接称作整个PC回合实测。

原5b9e10静态分发表独立证明task41／42／43落到对应STAT／APTITUDE／SKILL分支。原officer虚表+44指向4883b0，实际读取+94军团ID；此准备字段未赋值时为-1，导致490aa0所得势力无效，原完成正确拒绝。最初探针还错误重复构造了stride180的对象；原共享解码和查表为stride190。修正步长仍未完成，指令轨迹随后找到+94归属缺失。仅将该确切字段设为已准备的军团0也能在第三次成功，排除“原正常计数不完成”的误判。+98候选修改无效，相关失败仍保留。以上均是探针准备问题，未改游戏指令、安卓规则或旧保存。

正式工具使用明确的完整190字节受控actor载荷、原构造器、军团0／城市0及非玩家controller=-1；初始计数3和隐藏slot0仍为显式输入。整个原开始命令、取消／失守／死亡、费用扣除、随机隐藏及真正全局回合未闭合。继承的Android培养writebase与旧31—33含义继续保持。

两次最终JSON全等SHA302e8960b73ae353cee82729f0f6f0b6b0a3dfcc5d377b0b37ddc0d6ea580505；随源码固定的`docs/pc-data/ability-countdown-native.json.gz`6785B，SHAbb5ddfe7ec7f72ef633bf93ae0c5d3c5fe6775c12a46cfcc2723df2f3e9e3a42，包含全部ID／record SHA、函数原字节、分发表、实际计数及再扫描结果。用现有PYTHONPATH运行：

```sh
python3 tools/content/verify_pc_ability_countdown_dispatch_native.py --output out/parity/ability-countdown-new
```

输出JSON及固定mtime／空filename的gzip可与跟踪文件逐字节核对；原PC目录仅只读。
