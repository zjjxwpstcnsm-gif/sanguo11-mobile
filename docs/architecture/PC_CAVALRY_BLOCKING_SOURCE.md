# 原骑兵落点路径与受阻语义取证（2026-10-04）

PC安装只读，未启动Wine；EXE SHA256 30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb。源Shared Scenario.s11 SHA256 dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f。未修改Android规则或用户存档。

## 可复现静态出处

`tools/content/inspect_pc_cavalry_dispatch.py`先核对已通过原名称读取器的shared-rule-catalog来源和每个原56字节记录SHA，再检查原跳转表587118及真实e8目标。

- native9原名突擊，记录34252，58637b→595b70→595630，原push1。
- native10原名突破，记录34308，586395→595bb0，是另一条路径。
- native11原名突進，记录34364，5863aa→595b90→595630，原push2。
- 595630共同路径的落点循环在595724调用594650。该检查限制原200×200坐标，从原地图格+4低5位索引8a5df8表，最后检查占格低2位；任何非0占格均拒绝。该最终检查没有普通友方城市通行许可。活动筛选器仍可能提前拒绝，不能据此宣称全部原规则准入已闭合。

上述执行／演示链同时涉及原气力扣减、原结果结构和表现调用；完整权威单位坐标更新与表现边界尚未闭合。不能把静态调用或展示落点规划当作完整原战斗命中／伤害／碰撞／失败语义。

## 原检查实际执行范围

`tools/content/verify_pc_cavalry_landing_native.py --inactive-view-filter --output <fresh-dir>`实际执行原594650及其原调用，不替换回调／返回值。已完成1024组：4个有效坐标、4个边界外坐标×32个原地形标志槽×占格低2位0/1/2/3，68组合法空落点，其余拒绝。逐例完整3MiB世界、1MiB格数组及8a5d44 RNG全等；所有非栈写入为0。重复两次JSON字节完全相同，审计reproduction-equality.json。

这是显式构造的输入范围：原67f750检查全局73f550c是否不等于-1。本探针明确将此字段输入设置为-1，以覆盖无活动筛选器分支；记录原PE值0、实际测试值-1和未执行完整初始化。不是原PC正常启动状态证据，也没有伪造一个筛选器回调来执行活动分支。

先前重复映射原PE.data内格数组导致UC_ERR_MAP，随后默认PE状态0触发未初始化筛选器虚表回调、UC_ERR_FETCH；失败与原轨迹保留在out/parity/cavalry-blocking-source-20261004。没有把默认状态失败改为通过。正落点68要求保留，避免未初始化地形表全拒绝而产生假通过。

## 仍需闭合

原城市七格注册已有R37控制夹具原函数证据，正常前端分配／官方开局仍未验证；活动筛选器的真实对象构造／调用、完整战法命中与主伤害、无法位移时碰撞／单挑／失败、反击和连锁及权威坐标提交边界仍未验证。Android城市七格阻断、普通通行／进驻及旧异常位置保留已通过真实命令和新包实装；受阻损伤仍继承工程实现，不以本1024测试改数值或宣称PC全等。继续推进其他已明确的原来源奖励与数据。

依赖路径仍使用项目既有pc-emulate及pc-inspect PYTHONPATH，不安装新运行时。所有源编号、字节、SHA、跳转目标和测试输入都由工具再导出；不使用网络表或记忆补映射。

## 特殊受阻建筑分支的原类型核实

新增`inspect_pc_cavalry_building_predicate.py`：原595746→5957d1在落点被拒绝后检查占格kind2并调用487ab0。该谓词通过原64模板79c54／strideD0的+b4字段判断3，同时明确排除native24。实际64模板及-1／64／999边界共67用例，9个正例为16火種、17火焰種、18火球、19火焰球、20火船、21業火球、22業火種、23落石、25淺灘。native24堤防+b4=3但原谓词返回0；native0都市、1關所、2港均+b4=0、返回0。逐行原编号、名称、offset／recordSHA、函数原字节与实际返回均保留，完整3MiB世界及RNG不变。

因此不能把这个特殊分支视为“撞城”分支，也不能把它设置某输出flag为0直接解释为伤害归零。名字仅采用原读取目录，未用网上设施表。原主要伤害提交路径5b1870以及碰撞／单挑／反击等完整调用尚未实际闭合，Android受阻损伤没有据此修改。

```sh
python3 tools/content/inspect_pc_cavalry_building_predicate.py --output out/parity/cavalry-special-buildings-new
```

两次正式67例JSON17453B全等，SHA302f0f63d73cc73cdf6dea8b2973a0e6f6602f726280ee97a0b2f0c8297c6265。`docs/pc-data/cavalry-special-building-native.json.gz`4670B，SHAca17e309ddcb96e1a04bb2788c66084e7bf2ed28bdd0e66fc8b73a7bb052be64随源码保存；工具同时输出固定gzip供逐字节复现。没有用此谓词的输出flag改城市或伤害实现。

## 原有界落点循环，而非完整战法提交

`verify_pc_cavalry_landing_loop_native.py`执行5956d6→5957e2原指令，实际调用483e10求方向、483a50邻格、594650准入、城市受阻时483b20／487ab0、483aa0退回前一格。168例覆盖x坐标奇偶两种×六原方向×原请求1／2步×无障碍／单位／都市／不合法地形在第一或第二步受阻。源两次JSON全部相同，SHAc67379eb89d3db76defa805517816987cfabe45d5cb1345a99afcbc8a201a2ff；随源码固定gzip SHA259790951f56ea605567a7a4ed3952d47c6f5314b6a9843491d71454a5edd9cc。每例完整3MiB世界、1MiB地格和RNG不变，非栈写入0，原请求结构未提交修改。正常都市不会进入特殊建筑输出flag清零分支。

输入边界明确：采用原4880a0构造的native0都市对象及共享64建筑模板，显式设置483b20的46483b4注册表指针；只构造测试占格，尚未执行正常城市七格注册。73f550c仍明确为-1。进入该块所需5956d2保存的可见性局部值是显式输入1，在停止边界前不消费；前面的5a06d0可见性检查没有执行。未替换原指令／回调／规则返回；这段输出是权威提交前的落点规划，不是完整595630、命中／主伤害／碰撞／失败或最终位置提交证明，Android损伤与RNG公式未改。

```sh
PYTHONPATH=out/toolchain/pc-emulate:out/toolchain/pc-inspect python3 tools/content/verify_pc_cavalry_landing_loop_native.py --output out/parity/cavalry-loop-new
```

在本worktree可使用原目录对应两个工具依赖路径。工具输出可复现JSON和mtime0／空filename的gzip；每份都保存原EXE／Shared SHA、指令块SHA、输入、坐标与具体未完成项。

## 原城市形状注册与实际骑兵拒绝

R37的verify_pc_city_footprint_native.py执行原半坐标／anchor转换、完整416690注册、原28子格与4848f0写入，6中心精确生成中心加六邻格。每格原483b20返回同一城市，42注册占格被实际594650拒绝，6占地外合法正例通过；完整世界／RNG不变，地图七个占格及index精确，图形scratch另披露。二次JSON7c03c608…07f8a、gzip6a451385…270c5全等。record/index0、selector0与receiver是控制输入，正常前端分配417590未执行；filter=-1和合法地形仍明确输入。不能扩大为完整战法伤害／权威提交／官方启动。

## 受阻目标存活与主兵力结果应用

R38两个原块已二次复现：5957e2..59584a实际受阻存活规划38例，目标兵力<=主伤害时选择目标原格跟进；5b1c60..5b1c71结果+c→4ae4a0→4961f0主兵力40例。完整世界／格数组／RNG守卫，真实构造及validity，无原返回值替换。主伤害和受阻local是输入，尚未串完整前置命中生成、死亡／单挑／反击／权威位置提交，不改Android伤害。详见ROUND_38及两个固定gzip。
