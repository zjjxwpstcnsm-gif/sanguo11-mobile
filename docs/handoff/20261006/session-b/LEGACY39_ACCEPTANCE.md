# 真实39舌战中途档实际APK续行验收（限定批次）

2026-10-07；自己的分支 `codex/rules-content-contest-repair`，代码共同基点/当前提交 `89534120e46e488136db0e661270757d9c4a4c50`；继承完整主源目录仍为 `scenario-main-closeout/sanguo11-mobile`。没有新Native完成批次提交或向A交付。

## 已证明

- 真实既有 `docs/handoff/20261004/session1/batch19-actual-art-mid.sg11`，2139833字节，SHA `5d002d3cee6f84700cd5e1292c05b672e8a05e0b547ac7d753f2b7987d5e2f7f`。实际冻结测试APK中的asset解压SHA也相同；没有改存档头制造39。
- 实际5582菜单读取 → 保持原手牌/整份PcDebateCampaign序列化/策略、生命周期与原RNG → 取消策略确认纯净 → 玩家明确采用已有终局策略 → 合法按钮出牌 → 自然终局 → 三个完整前台旬 → 普通手动存档与读取 → force-stop后新进程冷续行。暖/冷各自执行全流程。
- 57通过现有“重试3D地图”按钮，暖/冷均观察到A现有非空Surface输出；重试前后完整World和玩法RNG相同。未改A文件。输出存在不等于原版美术验收。
- 中途完整档2140740字节，SHA `9d9876096a3f802a9da4992f8342bc5c9f4d49bacd77e5b25b9b60fdee0bdc86`。暖/冷最终完整档2151609字节，SHA均 `565745800406cbee008e1f9a1d01ed5d18c28756a3dce8418879c18004968072`。56与57最终结果也相同，没有因渲染重抽玩法随机数。
- 每轮独立备份全部15内部/772外部文件；55/56/57最终逐SHA恢复、零额外文件，5582锁释放。没有清数据、触碰5554或删除用户资源。

## 构建与实际证据

55原始测试因Android不支持InputStream.readAllBytes在加载原档前失败，完整恢复，失败日志保留。56改为限长分块读取；57增加正常3D重试。56、57各独立构建、11106输入逐SHA前后不变并冻结。

57构建48秒，76任务/5执行/71缓存；生产314901900字节，SHA `3bccbf959e163958edc6c44d8549fc59ae64d6350f9bbdefee93719e3dfba714`；测试3038504字节，SHA `94a2652c0e0935247dd758852ecd68ad6360f9b05d24cbe4ee37e54972602a15`。安装后二者SHA读回一致。

证据在 `out/session-b/legacy39-ui-build57/{build-inputs.json,frozen-report.json,frozen/}` 和 `out/session-b/legacy39-ui-57/{results.json,instrumentation.txt,cold-instrumentation.txt,evidence/legacy39/,evidence/legacy39-cold/}`。用户备份tar不属于交付内容。生产APK包含完整当前Native WIP，尚不能作为完成的Native源码批次交A；后续需隔离可提交的完成增量并独立重建验收。

## 正式模块检查

原两个core测试越界依赖GameSession，逐字节移到game-runtime测试目录；原三份对照夹具逐字节复制到对应runtime测试资源。RuntimeSettlement测试误依赖core测试类的指针转换辅助，改成测试内同字节转换函数；没有改生产规则或扩大模块依赖。

`:core:testClasses :game-runtime:verifyBridge :game-runtime:verifySession :game-runtime:verifyConstructionSession` 正式Gradle通过57秒；Session1690、建筑197、Bridge实场景/重复/乱序/过期/恢复边界通过。实际Gradle编译类路径上的君主释放61、无人员军团合并及普通调动151检查通过。架构、168资源检查通过；四JNI及旧609dirty状态SHA不变。没有修改Unity旧golden。

## 未完成与下一个增量

本批只有这一份真实39中途档的明确迁移与续行。没有静默启用新策略，未证明31/38、所有自定义档或16来源全流程。原舌战准入/费用、认输、外交完整回调仍未闭合；原退出链扫描只是候选，不能把放弃改成普通败北。新局Main当前三参数加载仍未启用新Native单挑生产器；君主AI处分、同地域/失陷返程、完整来源覆盖等继续推进。ARM及原版美术/音频/Unity动态验收分别待验证。

接下来从完整已提交89534120隔离构建可提交的39验收增量，保留所有Native WIP，完成其独立APK/源边界证明后才交完成批次；随后继续原准入/认输/外交/单挑正常新局闭合。不得用本报告宣布全目标完成。
