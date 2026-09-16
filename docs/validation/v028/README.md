# v0.28 验收记录（进行中）

基线PR #35合并为ae1fef7。本轮PR #36，提交最终结果后补充运行SHA、APK摘要、安装矩阵及配对耗时。不把开发中构建作为最终验证通过。

## 旧版实装

[CI35100123171](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35100123171) / [实际旧版操作证据](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35100123171/artifacts/10447729342)。归档SHA-256 `17c68e9535f508a565d94bd0192f6212d5c713de84dd1c3804e9b08405b070d5`。

API29 x86_64，1080×1920、420dpi，横竖屏。安装上轮实际v0.27 APK并核对固定SHA，不是重新编译一个“旧版”。输入通过Android实际触摸事件和可访问性输入；夹具只布置情境，命令从正式手机入口执行。不是人手真机测试。

`baseline-experience.txt`记录镜头位移、下一队、按钮状态、42城670将和200×200/42城40队3运输场景耗时；`baseline-instrumentation.txt`记录完整操作通过。截图位于归档`files/smoke`，包含选择前后、全国视角、出征、运输、补给、军团和重建流程。

## 新版门槛

正在执行：完整核心/UI模型/内容、36/72旬、Android/Lint、三屏横竖屏、v0.9/v0.25/v0.26/v0.27实际覆盖升级、新版行为回归、同模拟器前后对比。新增验证针对明确行为，不以断言数量代替可用性。
