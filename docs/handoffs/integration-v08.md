# v0.8 联合集成记录

集成分支：`agent/integrate-v08`，[PR #9](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/9)。初次合并的实际父提交为 Agent 1 `9956d9884a4d8d31bdfb276630770d646c93f138` 与 Agent 2 `40596e9c6a2346b1c9151c0e9490cb35e635a9dd`；后续已合入 Agent 2 `08da7a68592bb4834c5a08ebf824dedc27bd0d35`、PR6最终 `6dd63cd34ae50f3306bb5c4a8c7085ccce5c882a` 及 Agent 1 文档 `783b387dbed42c63bc483339dc5d6e9ea2afdfc7`。共同固定基线为 `ff624b6e9c48c81d7111a9a642b18cf6bd098fe1`。本Agent未执行合并任何现有PR或修改main。

## 集成处理

- 实际三方合并；GameSmokeRunner 冲突保留双方完整流程，依次执行规则与资料检查，新增资料武将特技显示/运行时绑定检查。未用任一方整文件覆盖另一方。
- MainActivity 保留 Agent 2 导航与筛选，也保留移动确认、编队特技入口及 PR6 的出征激活/制造任务计数修复。PR6最新 `d89f9882...` 的四处修复已逐项覆盖。
- 原资料 ID `skill-000`～`skill-099` 保留作为来源引用；`ContentRuntime` 明确逐行绑定到稳定 `san11.*` 运行时ID，校验原名与一对一关系。没有依靠枚举序号、简繁同名猜测或重编号。新增资料演练18人绑定真实特技与性别；未实现的特技效果仍然不生效。
- 只在新建明确的 community-reference 演练时绑定。SaveCodec 不读取资料目录；旧档、旧演练、进展中的状态、未知/培养后的技能ID均不重写。
- 统一 Android `versionCode=8`、`versionName=0.8.0-integrated-dev`，唯一存档格式v7。没有第二次存档版本升级。
- CI checkout 精确PR head，保存实际 BUILD_COMMIT，产物名为 sanguo11-mobile-integrated-v08-apk，包含APK、摘要、证书与对照证书。离线导入与15异常测试、全部核心/UI、Android/Lint、三种横屏真实点击全部在合并代码上重跑。

## 版本与真实缺口

共同目标为Windows PC繁中PK；资料分支以1.1为目标，但原安装/补丁/可执行文件哈希尚未取得，不能宣称已精确对照该补丁。来源与规则差异分别见两个Agent交接、RULES_V0_8.md及docs/content/NATIONAL_CONTENT_V08.md。

全国原版地形0格、官方完整历史/假想开局0个；资料目录不是全国可玩地图。仅18人资料演练加载能力/适性/性别/特技，670人为资料目录。全100特技效果、原版36技巧树、PK能力研究、军团官职俘虏、人物事件宝物及单挑舌战状态机仍有缺口。基础伤害、行动尺度与部分状态/火场规则仍是已标注工程近似。

## 验证状态

联合代码 `4f7e4b6b6c96a8fe19f0037a1cbaa57c35cc43eb` 已通过 [独立CI 34795796771](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34795796771)（job103828525812），精确checkout该提交，APK中配套 BUILD_COMMIT 相同。此后的交付记录提交只改文档，不改变APK代码；不以两个独立PR的成功替代联合测试。

最终报告同时纳入 Agent 1 交付文档提交 `db4e2c70293d2d36ce2b4b07571498f60e0fb9cc`；该提交无运行时代码变更。

- 全部核心回归：187基础、359剧本、705内政、136168战术、8747人事、590军政、465军备断言；另有575规则、2638资料、143资料到规则联合断言；UI/相机51730断言与15 Python异常测试均通过。真实v6夹具与v1～v5兼容回归保留。
- Android构建、Lint和API29 x86_64三种横屏（1080×1920、1080×2340、1080×2400）完整安装点击通过，包含双方旧流程、移动预览/取消/Activity重建、神算百出连环、资料开局/导航/搜索/恢复，以及甘宁威风的实际运行时绑定。不是只测启动或按钮存在。
- 已取回 [安装证据](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34795796771/artifacts/10330027094)：每种36张、共108张PNG，3份instrumentation均SMOKE PASS，3份logcat未发现应用FATAL EXCEPTION/ANR。人工检查联合版移动确认和来源武将特技截图。
- [Lint报告](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34795796771/artifacts/10329914188)为12条警告，门禁通过，不能称零警告。日志摘录见 [integrated-ci-summary.txt](../validation/v08/integrated-ci-summary.txt)。
- 工程128×128地形、128城/128将软件Canvas onDraw暖40次：三档p95分别11.08423/7.45959/4.61935ms，访问113/121/131格。它不是FPS或GPU时延，也不是原版全国地图或真机性能。无ARM真机，未做真机验证。

## 实际APK与签名

[APK ZIP下载](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34795796771/artifacts/10330450599) 包含 `sanguo11-mobile-integrated-v08-4f7e4b6.apk`，297602字节，SHA256 `e67c42c8ed0fe731aebbd009b56723f33c69bf8be6055e2b37393111b4c0b75b`；实际下载并核对 SHA256SUMS 与 BUILD_COMMIT。[对应精确源码](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34795796771/artifacts/10329427318)。Android版本8 / 0.8.0-integrated-dev，存档v7。

apksigner验证通过，当前证书SHA256 `382276fbba3f16fa6f01918081873364c6e1b1345df24653971d4e039e6ec007`。CI实际下载了两个旧APK并比较证书：

| 旧包 | 对照证书SHA256 | 结果 |
| --- | --- | --- |
| main e66656e3 v0.6，运行34791182444，产物10328612868 | e1fb6629564cc2d42c1b09ad5af57a372d421440e58dc2b219b4833d0364ea97 | 不同，不能覆盖 |
| d89f9882 v0.7，运行34793420077，产物10329536376 | aac48fb5a704665731c043e4b858f7c706865e7b77391d952481dfe8ab187445 | 不同，不能覆盖 |

请保留有重要存档的旧应用；格式兼容不等于Android签名兼容。当前没有持久发行签名配置，也未验证其他未知旧包的覆盖升级。

## 后续合入记录


追加纳入 Agent 2 `9afa3d6502f53ed0f4c20b5dac460a92fb762dab` 的紧凑资料页、稳定滚动位置恢复及无任何资料包的隔离读档验证；ContentUi冲突同时保留其布局修复和本集成的特技实际绑定说明。PR6 后续 `6dd63cd34ae50f3306bb5c4a8c7085ccce5c882a` 只有交付文档更新，运行时代码仍为已核对的 d89f9882。本轮工作期间PR6由仓库其他操作合并至main c4891cf；本Agent没有执行合并PR或改main。

已将PR6最终6dd63cd3实际合入提交父链并保留其历史APK/签名证据；README、功能表、数据格式和进度以v0.8/v7为当前状态，v0.7记录保留为历史。


追加纳入 Agent 2 `08da7a68592bb4834c5a08ebf824dedc27bd0d35`。其实际截图/失败记录表明一次滚动后首行仍部分可见，原断言误以为一次滚动必定换首行；保留换首行与重建恢复断言，改为有限次真实滚动，并纳入首行像素偏移保存、紧凑行摘要和真实玩家/AI部队交战验证。旧联合CI 34795264241的此项失败已由完整重跑34795796771验证修复，没有删除失败断言求绿。
