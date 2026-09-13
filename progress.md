# 项目进度

日期：2026-09-13。仓库：`zjjxwpstcnsm-gif/sanguo11-mobile`；private。

用户最终目标保持：独立Android手机版，完整玩法/武将/兵种/城池/地图，最终APK。当前 **v0.2.0 / M1开发中**，不是原版完整复刻。

## 本轮已实现

- 在主干 `de030c4309cfcb33dd1ddc0aed4a3bc4114acd2b` 基础上继续，逐文件校验本地基线一致。
- 2个UTF-8剧本包，稳定ID、来源等级、修订号、SHA-256和严格载入校验。
- 保留3城6将基础演练；新增28×23、9城18将、3势力的区域争雄原创沙盘。
- 玩家可选刘备/曹操/孙权（演练为前两方）；其他存活势力轮流行动，多方灭亡/胜负判断。
- 电脑跨旬绕过河流山地，缺兵装时生产；城破/溃散武将退往剩余己方城池（工程近似，无俘虏系统）。
- Android剧本与势力选择、城池列表定位、三色旗帜、3个手动存档槽；电脑回合后台快照结算，避免重复触发并保留相机位置。
- 存档v2；读取实际M0 v1存档、迁移后确定性续局，旧手动存档仍为槽1。
- 本地内核通过481断言（188+293）；两个内置包和各玩家最多90旬对局、数据错误与旧存档迁移通过。
- 新增Android安装操作测试：选剧本/势力→城池导航→出征→三方回合→存读档→损坏存档恢复→Activity重建；Android API29 x86_64 安装操作流程已全部通过。

## 构建记录

- 上一版M0：[成功构建](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34752227881)，193项历史断言、编译/Lint/签名通过。
- 本轮v0.2.0源码：`dfc4b51266a3daedd928946cad7dfa265375d706`。
- [本轮成功构建](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34752984146)：481断言、`:core:check`、APK/测试APK编译、Lint、APK内数据包检查、Android API29安装操作测试、APK v2签名校验全部通过。
- [v0.2.0 APK下载](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34752984146/artifacts/10316298273)：ZIP含 `app-debug.apk` 与 `SHA256SUMS`，仓库成员登录可下载，保留至2026-10-13。
- APK SHA-256：`32551466e593a2c82db3cade249c964b0d6aa3785860f99b7d57ce9416e0f4ac`；已重新取回产物并核对。
- [安装操作日志与5张截图](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34752984146/artifacts/10316547717)。操作流程输出 `SMOKE PASS`，已目视检查出征地图和存档选择截图。
- 使用临时调试签名，无法保证覆盖安装旧APK；真机性能与稳定签名升级仍待完成。

## 下一步

1. 保持本轮内核及Android安装流程回归通过，作为后续改造基线。
2. 按 `docs/DATA_FORMAT.md` 继续收集原版版本与完整数据依据；不能将原创沙盘当作原版地图或剧本。
3. 推进设施开发、运输、人员移动与完整全国战略层，保留现有回归门槛。
4. 真机核验缩放/多指、安全区、暂停恢复/杀进程、ARM64性能和安装升级；配置持久签名后再声称可覆盖升级。

完整差异继续在 `docs/FEATURES.md` 追踪；本轮未完成全国地图、全武将、原版战法、外交、单挑/舌战和3D表现。
