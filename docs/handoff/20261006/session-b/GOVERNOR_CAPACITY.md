# 国号5统兵原复核，尚未生产修复

只读原EXE与Shared/Source0，复用NativeDebateFlow、原serializer/loader、原PE data，493400载入后通过48a4f0有效统兵getter核对。没有Wine、网络表格或工程数值替代。

本次独立原执行：native58 owner28/country5/status3/rawOffice80，内部+54引用58，与native403内部+54引用403不相同。原49d420走rank20，定义+2c为13000，48a4f0返回13000。native403为君主，走title0定义+2e15000，即使势力原title9；48a4f0返回15000。查询前后完整3MiB原World与原标量RNG字节一致。

这证明不能将国号5全体人物都套title0，也不能使用未任官80的普通容量。内部+54引用不能自动当生物学父亲或猜runtime ID。复现工具session_b_pc_command_capacity.py；现场out/session-b/native58-original.json。

候选27/28仍封存未启用，生产Government不在本批改动。后续必须显式新局策略保存上述原分支、通过全16来源有效容量/太守/军团与正常命令续行，并保持旧31–39已存策略不补填；仅本条证据不算生产修复。

本次追加原4c4260势力getter执行：owner28原字段3=403君主、4=435军师、5=9爵位、6=5国号；原内存force+40同为5。查询前后完整World/RNG不变。参数--force-getters可复现字段域3–20/35；现场native58-force-fields.json。字段6已由原getter与原内存交叉核对，后续可按已存原势力字段连接，不按国号显示文字或native编号猜runtime ID。仍未生产启用军团/太守候选。

独立容量原型（out/session-b/capacity-probe，未启用生产）复用封存的原1100内部引用记录资源，只按每源人物recordSHA/officerId连接；与16份完整原选举报告中的有效统兵值对照。先只补country5分支仍7246处不符，原因包括工程无所属人物返回5000而原getter为0。加入无所属势力返回0后，所有已连ID的670x16=10720条容量均匹配，修正7327处现有投影差异，其中country5先前单独修正81条。原型没有合并27/28，没有修改生产Government/军团/太守；未连额外/NPC身份、正常变更所属/国号/关系后规则和AP预算还须另证。原始零字节构造对象不作为有效人物覆盖。

Normal typed source0/force28 admission probe (no injected resources/officer) now confirms native58 maps through saved recordSHA to runtime10058, named HeYi (actual source Chinese name preserved in capacity-normal-admission-before.log), present/idle ordinary officer in city20013/Xuchang. Stock50000troops/106000food/4800gold suffices, but real Army.deploy13000/26000food/500gold rejects with current5000 ceiling. Original getter independently returns13000 for the same source/native58/country5 record. This is a concrete normal command blocker, not only a table mismatch. Next fresh-source saved capacity strategy must preserve old31-39 namespaces, compare all16 mapped rows again, and pass normal UI/deployment; no production capacity change or sealed27/28 activation yet.

Controlled dynamic original query cases now reproduced by --controlled-cases, using native58/force28 original loaded objects and explicit VM-only fields. Ordinary country0/status3/rank80 ->5000; country5/status3/rank80 or20 ->13000; country5/status3/internalRef==native403 ref ->15000; ruler/status0 country0/title9 ->10000, country5 ->15000. Each query preserves full3MiB World and scalarRNG; original source files remain unchanged. Original49d420 disassembly additionally checks actor/force validity, country field+40==5, actual status0 ruler getter488c00, raw+54 equality to403, then title0/rank20 branches. Normal force id0-41 gate481350 and explicit native tech18 +3000 wrapper49d540 are separated from opening values. VM fixture proof is not normal GUI proof. No production capacity policy yet.

## Production capacity closeout (later evidence supersedes prototype-only status above)

Newly written saved new-game-only PcCommandCapacityPolicy passed89 core and10720 direct original production values plus actual APK20 source0/force28/native58 normal13000 deployment/three turns/save/cold;15 internal/772 external originals restored exactSHA. This activates no sealed governor/native-parent code. Governor election, source army/district/opening AP and effective NPC activation remain separate unknowns. Explicit old namespace absence/no-backfill strategy in COMMAND_CAPACITY_STRATEGY.md.

Read-only governor opening audit:1392 site joins across16 saved source opening records compared to pinned original independent4bca30 election outputs.479 current initial-governor differences, zero unknown winner identities; fullWorld/bothRNG remained pure. No policy enabled and no ordinary native opening-controller/AP/event sequence inferred. First draft mistakenly used catalog summaries (empty site lists); zero-row output was rejected/replaced with saved-record native/runtime joins and hard1392 coverage check. Ignored audit code/results remain diagnosis only. Exact per-site evidence out/session-b/governor-opening-current.tsv/log; production governors still unchanged.
