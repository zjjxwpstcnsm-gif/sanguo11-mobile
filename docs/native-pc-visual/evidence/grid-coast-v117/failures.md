# 失败保留

- API35 attempt1：v116 PASS163、v117初始ready FAIL；attempt2：v116初始ready FAIL、v117 PASS874。两次失败均pending2/submitted3，稳定性继续FAIL。
- 全国R05同源码ready FAIL，pending29/submitted3，完整操作未到达；录屏moov缺失，不能作可播放证据。
- 输入与候选完整core均exit1：CoreTest.logistics:75，AI uses deployment commands。
- 输入与候选PortReplay均exit1：PortReplayTest.aiDocks:52，AI completes embark, sail and land。
- WIF attribute condition拒绝，quota preflight NOT_RUN，物理设备提交0。
- 开发期故意改变显示XZ触发旧字节断言：原失败日志保留；冻结旧实现与原hash控制继续执行，没有用新hash覆盖旧hash。

详细结果与原始文件路径/校验值见manifest.json和raw-files.json，原文件随独立证据ZIP交付。
