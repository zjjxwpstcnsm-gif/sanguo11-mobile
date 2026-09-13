# v0.2.0 验证记录

## 本地内核

`bash scripts/test-core.sh`：当前通过 481 项断言（CoreTest 188，ScenarioTest 293）。旧测试的部分断言次数随 AI 对局结束时点变化，188不表示删除了旧回归场景。

新增验证包括：两个数据包及稳定ID、畸形输入拒绝、SHA-256与目录一致性、任意玩家势力指挥/禁止越权、三方依次行动、跳过已灭亡势力、尚有部队时保留生存、三方胜利/失败条件、绕过长障碍攻城、城破武将退往己方城池、实际v1存档迁移、v2完整往返、迁移后续局、跨年日期、数据包下架后的存档独立性，以及各势力最多90旬的确定性对局和周期存读档。

## Android 构建与安装流程

CI 固定原有 AGP 8.9.2 / Gradle 8.11.1 / SDK 35，增加测试APK编译以及包内资源检查。Android API 29 x86_64 测试设备运行真实安装后的 APK，平台 Instrumentation 操作可访问性节点对应的触控位置，不调用游戏内部命令跳过界面。

运行 `bash scripts/smoke-android.sh` 前，必须使用专用测试设备：脚本会清空该设备的 `game.sanguo.mobile` 测试应用数据。检查场景：

1. 冷启动读取APK中的剧本资源，打开剧本选择。
2. 选择区域争雄 / 孙权军，定位建业。
3. 选择甘宁 / 弩兵 / 3000人出征，核对实际自动存档。
4. 下一旬完成两个电脑势力行动，核对归属与日期。
5. 保存槽1；切换基础演练 / 曹操军，保存槽2；重新读取槽1。
6. 向槽3写入损坏夹具，读取失败后确认当前局面未变化。
7. 重建 Activity，确认自动恢复；保存界面截图和日志。

流程以 `SMOKE PASS` 为明确成功门槛。APK仅在该门槛、编译、Lint和签名校验通过后上传主APK产物。失败仍上传可取得的日志/截图。

参考：[Android Instrumentation](https://developer.android.com/reference/android/app/Instrumentation)、[Android Emulator Runner](https://github.com/ReactiveCircus/android-emulator-runner)。

## 仍需验证

本轮 [CI运行34752984146](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34752984146) 已全部通过，包括真实安装的Android API29 x86_64操作流程和APK v2签名验证；已取回产物核对SHA-256并目视检查出征/存档截图。自动化设备测试不代替用户真机验收：ARM64设备、刘海/挖孔安全区、不同分辨率的双指缩放、多指误触、系统杀进程、长时间功耗/温控、签名一致的覆盖安装仍待实测。

当前每次CI使用临时调试签名，不能保证与上一版APK直接覆盖升级。存档格式兼容与安装包签名兼容是不同问题；正式发布前需要持久签名方案。
