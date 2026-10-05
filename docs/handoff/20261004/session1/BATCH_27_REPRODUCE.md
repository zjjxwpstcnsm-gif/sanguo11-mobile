# Batch27 未完成候选的可复现边界

生产基点443c47826d637f3718d044ad94cbb9bddd88c072，实现继承840030195e39cec3c0d352e010a3c3e614893ca2完整工作区；不得从旧HEAD重建或复制整个core。生产APK仍Batch26。本批不使用设备、不修改媒体、桥、Unity、公共台账或旧存档。

batch27-candidate-delta.json逐条给出19个路径的expectedCurrentSha256/candidateSha256。tar.gz只包含这些完整候选文件；archive SHA635bb2f3cd3ddcdea62aad5a72981710817cc953d9f8b3b35fc06d2e3c0312d1。应在另一个完整继承目录先核对全部旧SHA和归属，再逐文件试验；此候选未闭合，不得直接作为生产交付。

原程序提取（PC安装只读；没有Wine）：

```sh
PYTHONPATH=tools/content:out/session1/toolchain/pc-emulate python3 tools/content/inspect_pc_internal_parent_references.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' --output out/session1/relations27/replay-parents.json.gz
PYTHONPATH=tools/content:out/session1/toolchain/pc-emulate python3 tools/content/inspect_pc_governor_order.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' --output out/session1/relations27/replay-order.json.gz
PYTHONPATH=tools/content:out/session1/toolchain/pc-emulate python3 tools/content/inspect_pc_governor_rosters.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' --source-index 0 --output out/session1/relations27/replay-rosters.json.gz
PYTHONPATH=tools/content:out/session1/toolchain/pc-emulate python3 tools/content/inspect_pc_debate_campaign_settlement.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' --postload --source-date --ability-change 0 --output out/session1/relations27/replay-campaign.json.gz
```

明确能力选项0是“有效”，三个fixedAge非零来源仍按原开局限制禁用曲线。用户实际PC偏好及外部Expansion覆盖未知。不能将构造器默认能力选项1冒充来源新局当前能力，也不能把源边界当完整开局事件执行。

导入与原对照夹具：

```sh
python3 tools/content/build_pc_internal_parent_references.py docs/handoff/20261004/session1/internal-parents-native.json.gz --expected-sha 4a457201caa09a755ca152f854d07f6751a00e8b571e4712601435c02d46f7a6 --output out/session1/relations27/replay-parent-content
python3 tools/content/build_pc_governor_order_fixtures.py docs/handoff/20261004/session1/governor-order-native.json.gz --expected-sha 95e20e50948f8cd5ad4112f2b43c101547a3526f10572b2c646db35cac4022b8 --output out/session1/relations27/replay-order-fixtures.tsv
```

候选core主机测试PcNativeParentPolicyTest接收original-fixtures.tsv和真实旧39 old-native-start.sg11；PcGovernorOrderTest接收排序TSV。完整正常触发/typed命令、重复拒绝、存取和普通三旬通过PcDebateOutcomeCampaignTest的host/cold、win/win-cold模式；原父亲38390、排序1416、失败10879/cold8、成功21/cold9检查通过。测试和日志在独立out/session1/relations27中；这不是实装或完整原上下文等价证据。

仍须完成太守军团/驻地过滤和任命接入、开战费用/功绩及触发准入、有效装备/先手、外交、完整单挑人控模型/保存/DTO/新APK、全部剧本开局事件和生效身份。现有工程单挑不得标原还原。最终新包必须独立SHA、实际安装和完整保存/RNG恢复，ARM证据另记。
