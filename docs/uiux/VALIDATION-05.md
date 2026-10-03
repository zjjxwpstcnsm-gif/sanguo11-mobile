# 第五批：建设选择、返回与权威结果

2026-10-03：48项安装交互检查通过；完整目标继续进行。

城市“设施开发”和地图开发地共用 BuildPicker，统一为地图选地→设施→执行武将→一次成本确认。取消最终确认或系统返回，保留原执行人搜索和设施；返回时恢复表单的旋转布局跟踪。明确“返回选择”“上一步”和“取消”。搜索采用48dp控件、清除和键盘搜索完成，列表随可见高度调整，空结果不再占用整块列表，单页不显示无效翻页栏。保留原版资源和既有规则接口。

[48项真实触屏结果](evidence-05/interaction.txt)：取消地图选取、实际点中高亮地块、无结果与键盘完整可见、清除、中文真实粘贴、确认返回保留草稿、系统返回、横竖屏、上一步、快速双击只建设一处且只扣一次金/AP、正式读档恢复完整权威状态和随机数。再实际申请铜雀台，显示核心“需要持有铜雀”失败原因，完整捕获字节不变；返回地图清除已失效草稿。测试未调用build或onTile替代触屏，坐标仅通过当前渲染器投影只读计算。

截图：[地图目标](evidence-05/01-map-targets.png)、[本批初版空态](evidence-05/before-empty-keyboard-run02.png)、[调整后空态](evidence-05/02-empty-keyboard.png)、[横屏](evidence-05/04-officer-landscape.png)、[正式开工](evidence-05/05-building.png)、[权威拒绝](evidence-05/07-authoritative-error.png)。空态对照是本批run02与run04，不冒称原版游戏对照。

完整录像 `out/uiux/iteration-05/run04/interaction.mp4`，实际解码帧与PTS位于run04/frames。run02失败原始记录保留：硬件sendStringSync没有输入中文，搜索保留断言失败；改为真实Ctrl+V后即时核对输入，run03主链路36项通过。run04新增失败分支并检查最终布局，48项通过。

APK `out/uiux/iteration-05/app-uiux-run04.apk`：SHA256 `c51c079de154a71a231bf1cd73799a96cad258eafeb7b7e088131193b7287f88`，82235520字节；测试包同目录app-uiux-test-run04.apk。源码身份为689af10加本批变更，完整源快照见同目录source-manifest.json。独立emulator-5580/API29/x86_64安装验证，没有ARM真机性能结论。

架构检查、168项固定资源检查通过；504份受保护规则/数据/转换源码与共享基线SHA一致。自动存档和模板库[前后SHA相同](evidence-05/save-integrity.json)，自有测试槽3保留。没有操作原目录或另一模拟器。

建设工期文字仍使用继承代码，权威预览接口契约已交玩法会话，尚未接入。本批移除了城市入口的第二份工期分支，但没有自行修改规则。更多内政、行军/战斗/计略/外交/连续回合、更多尺寸及原生全国加载性能仍需完成。
