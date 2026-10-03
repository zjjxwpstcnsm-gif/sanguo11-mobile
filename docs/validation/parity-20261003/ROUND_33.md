# R33：逐次技巧点事实候选

2026-10-04。目标active。阶段初始为尚未构建的core／game-api／game-runtime候选；当前候选已实装通过ART，原目录独立组合构建待验证，结果见下方。R32 a31证据保持独立。

问题：原提交／checkpoint只存净变化，同次相抵消的真实增减会消失，没有每次原因、顺序、阶段、完整StateToken及原回放checkpoint关联。

实现：保留原净变化接口，新增瞬时只读point journal；实际规则Map.put捕获 before/after，已审计生产者提供cause/actor/city。事务复制捕获、最终验证复制携带，成功安装后绑定完整token及父提交ID。原journal保留point-only／零净事件，facts指向实际checkpoint。初始化、读档、预览、取消和失败无已提交奖励。只做观察，SaveCodec及所有奖励公式／RNG不变。

验证：新增OrderedFacts59，旧Facts26、PcTechniqueSession137、CurrentSession1666、SnapshotReadOnly180、BattleReportCompression75、PcTechniquePoints2717、CityDisplacement6648和Bridge通过；日志在out/parity/technique-point-write-facts-20261004。初次journal单checkpoint测试假设错误，失败保留并明确校正输入边界，真实Editor双指令整提交检查未删。

未完成：实际Android ART、新组合安装、HUD/声音逐条消费、bridge传输、完整消耗／取消／重放UI验证。没有改app／UIowner文件。原公式未核实者继续按差异台账记录，不以事实字段补造奖励。

实装候选 ddae889（完整commit见STATE；APK7685556a…17cbb）：Java17离线构建80任务／66秒PASS；架构PASS。5554 API29 x86_64真实安装回读同SHA；生产11组ART13382（含Ordered59）全部PASS，原7文件／主包不变。独立HUD正常修复／研究扣点／取消及陈旧提交／增减反馈25项、13.32秒PASS，原7文件exact。城市位移ART6648／7.33秒与宿主19／.45秒PASS。没有ARM证据。

完整4269文件／336183864字节源码含4JNI快照归档。原目录26路径增量SHA预检并集成，4243其他路径SHA不变，全部源码匹配新快照。首次集成预检因旧manifest实际在原目录而非本worktree退出，未写任何源文件；错误日志保留，正确读取原目录manifest后通过。原目录独立构建正在进行，不将本候选APK证据算作该新组合包通过。HUD仍消费旧净变化，逐条新事实消费和bridge仍未闭合。
