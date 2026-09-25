# 原生 PC 视觉进度 — R00–R14 整改复验 / PARTIAL

2026-09-25。本轮已修改正式 `TerrainSurface` 和 `FilamentMapView.Overlay`，修复全国采样缓存整表淘汰和静止势力层每帧重算，并修正R13精确资产保护。不是再次仅审计，也不是全部阶段完成。

当前交付APK源码 `96719e971d74aa2b3f18d66a7f23832148a6dc8b`，版本102 / 0.102.0-native-remediation。继续同一串行分支 / PR67；保留原生3D、MapHost/SceneRenderGate及已合架构。没有合并main、force push、其他PR操作或R15扩展。APK之后的提交只涉及复验/证据，不能当作APK源码。

| 阶段 | 整体 | 本轮结果及未闭合范围 |
|---|---|---|
| R00 | PARTIAL | 精确源码新APK、签名/ABI/资产核验和原始证据链；完整启动故障矩阵未闭合 |
| R01 | PARTIAL | 保留遮挡暂停；20轮真实生命周期、异常初始化及资源全异步未闭合 |
| R02 | PARTIAL | 原投影与拾取主机回归通过；多视口/安全区/方向/动态分辨率真实触点矩阵未闭合 |
| R03 | PARTIAL | 全国高度缓存淘汰已修并做输入/候选对照；没有新增山脊/谷地造型，连续LOD动态美术门槛未闭合 |
| R04 | PARTIAL | 材质资产保持；V1未闭合，未用雾或模糊掩盖 |
| R05 | PARTIAL | 拓扑/港口规则保持；阶梯岸线和真实船行验收未闭合 |
| R06 | PARTIAL | owner线程部分纹理/图集CPU工作仍存在；异步/取消/损坏/重建完整矩阵未闭合 |
| R07 | PARTIAL | 没有本轮城港关模型修复；地基、朝向、入口遮挡及PC对照未闭合 |
| R08 | PARTIAL | 没有本轮林缘/栈道/农田修复；原动态视觉缺陷保留 |
| R09 | PARTIAL | V2仍FAIL；未扩散全国模板 |
| R10 | PARTIAL | 刚体动画符合允许的实现路线；全兵种装备、脚滑漂浮、靠岸和LOD实拍未闭合 |
| R11 | PARTIAL | 全事件（含CALM/EXTINGUISH）、暂停/倍速/跳过/重建完整矩阵未闭合 |
| R12 | PARTIAL | 修复前输入原R12超时、修复后多次原R12到达全部断言；完整触控链和真实日期/覆盖层仍不能认证 |
| R13 | PARTIAL | 旧资产门禁已改为298项精确哈希与4项历史合法材质白名单；真实UI编辑/SAF失败恢复闭环未完成 |
| R14 | PARTIAL | 权威/月字段正确仍不足以验收；实际整屏日期陈旧FAIL，四季动态/PC/真机未完成 |

本轮16组主机套件PASS；输入与候选完整core独立同败 `AI uses deployment commands / CoreTest.logistics:75`，未改AI或断言。API29 x86_64 SwANGLE是模拟器，ARM64真机与30分钟长稳NOT_RUN；No process found不算零内存。

当前结果以 [整改报告](reports/R00-R14-REMEDIATION.md)、[缺陷](DEFECTS.md)、[213项复验清单](evidence/R00-R14-remediation-checklist.json)、[manifest](evidence/R00-R14-REMEDIATION.json)、[续作交接](handoffs/R00-R14-REMEDIATION.md) 为准。逐项复合验收不由局部主机通过自动升级。原总审计原文另存history/R00-R14-audit-PROGRESS.md与对应DEFECTS；原阶段报告保留并增加当前状态索引，不倒填历史PASS。
