# R33：逐次技巧点事实候选

2026-10-04。目标active。本阶段core／game-api／game-runtime候选尚未构建实装，不替代R32 a31安装包证据。

问题：原提交／checkpoint只存净变化，同次相抵消的真实增减会消失，没有每次原因、顺序、阶段、完整StateToken及原回放checkpoint关联。

实现：保留原净变化接口，新增瞬时只读point journal；实际规则Map.put捕获 before/after，已审计生产者提供cause/actor/city。事务复制捕获、最终验证复制携带，成功安装后绑定完整token及父提交ID。原journal保留point-only／零净事件，facts指向实际checkpoint。初始化、读档、预览、取消和失败无已提交奖励。只做观察，SaveCodec及所有奖励公式／RNG不变。

验证：新增OrderedFacts59，旧Facts26、PcTechniqueSession137、CurrentSession1666、SnapshotReadOnly180、BattleReportCompression75、PcTechniquePoints2717、CityDisplacement6648和Bridge通过；日志在out/parity/technique-point-write-facts-20261004。初次journal单checkpoint测试假设错误，失败保留并明确校正输入边界，真实Editor双指令整提交检查未删。

未完成：实际Android ART、新组合安装、HUD/声音逐条消费、bridge传输、完整消耗／取消／重放UI验证。没有改app／UIowner文件。原公式未核实者继续按差异台账记录，不以事实字段补造奖励。
