# 第八批：外交与通用命令选择

2026-10-03，独立API29 x86_64模拟器真实外交交互 **56项通过**，完整目标仍在进行。

ChoiceDialog增加明确空态、48dp搜索/清空、按可见窗口调整列表、缓存搜索文字；DataTable命令选择适配键盘及旋转，默认先显示能力列，减少选择使者时的切页。外交势力→命令→期限→执行人可明确返回上一步。CampaignUi和DiplomacyUi最终确认统一使用既有commandDialog，提交前验证原局面/revision并防重复。删除亲善效果的UI重复公式，交涉效果以真实结果为准。

[56项实际记录](evidence-08/interaction.txt)：在原测试局面选择董卓军，实际中文粘贴搜索、空结果/清空、横屏、连续退回期限及命令、执行人键盘空态、默认能力列、取消最终确认；核心拒绝关系不足的同盟，完整权威存档和RNG逐字节不变；双击执行亲善只生成一名使者及一个revision；任务列表可见使者；最后通过正式加载恢复原局面。未用测试接口派遣使者或改变关系。

[原空白列表](evidence-08/before-empty-keyboard.png)→[明确空结果](evidence-08/after-empty-keyboard.png)，[使者键盘空态](evidence-08/envoy-keyboard.png)、[默认能力列](evidence-08/officer-abilities.png)、[核心拒绝原因](evidence-08/authority-error.png)。录像out/uiux/iteration-08/run04/interaction.mp4，32750658字节；5/15/25/35秒解码样本在run04/frames。run03为55项通过，run04额外验证默认能力列。run01实际复现缺少空态；run02测试错误地假设目标势力在首屏，改用真实中文搜索后通过。失败及游戏内恢复记录均保留。

APK out/uiux/iteration-08/app-uiux-run04.apk，82241240字节，SHA256 `0c9afc3cce21d43e8038753e07c5caa1b38a26234f8a4b3c69af191a80e0922d`，已安装5580验证。源码79cdc6b加本批UI/测试，依赖仍为冻结checkpoint-20261003-c；source与source-manifest.json同iteration-08目录，伴随构建输入复用iteration-06/build-inputs.tar.gz。架构与168固定资源检查通过，504受保护文件不变；[自动档/手动槽3/自建库SHA](evidence-08/save-after.sha256)与基线一致。

外交完整权威预览尚不可调用：历史费用说明和协定成功率仍使用原接口/展示，未声称已替换成typed DTO。新契约已登记[规则接口需求](RULE_UI_CONTRACT.md)并交给玩法会话；正式执行及失败均来自权威事务。高级援军、俘虏交换、劝降、使者抵达交涉和更多尺寸尚待验证。

后续优先消费玩法已冻结的出征v2缓存/详情与战法上下文成本，并复现集成方报告的250年成都编队键盘裁剪，不能用本地190曹操通过覆盖该失败。其后继续普攻/战法/计略及其余内政与手势验证；ARM真机、性能和完整流程仍未全部验收。
