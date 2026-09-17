# v0.33 验收记录

基线 main：`5eb88bbd40bbe70390c6bb04c397ba55f426e3ca`，PR #38；分支 `agent/architecture-rules-v033`。完整规则/删除/兼容/扩展说明见 [ARCHITECTURE_V0_33](../../ARCHITECTURE_V0_33.md)。

基线 CI [35177570022](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35177570022) 三屏完成失败，核心/编译/Lint已过，原生脚本停在能力研究任务筛选。修复测试切换世界时未清理 taskQuery 的状态泄漏；新局激活也统一清理ClientState。不能把该基线失败归为运行环境问题。

最终验证记录正在汇总；此处不会将运行中的CI标为通过。`baseline-core.txt` 是改动前完整结果，`refactor-core.txt` 是保持行为迁移结果，`final-core.txt` 是最终完整入口（结束必须 TEST_EXIT=0）。`content-ui.txt` 包含三条可复现生成检查、19项内容、51,810条UI模型。安卓构建和Lint见 `android-build.txt`。

本地使用API29 x86_64软件模拟器；首次系统System UI ANR遮挡起始页，后续重试将与正式应用故障分列。不等同ARM真机。打包检查：`python3 tools/check_apk_content.py app/build/outputs/apk/debug/app-debug.apk`，同时检查正式目录和DEX不存在移出的测试实现。

APK包名 `game.sanguo.mobile.dev`，versionCode 33，versionName `0.33.0-architecture-rules-dev`；沿用公开开发签名，证书SHA-256 `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`。
