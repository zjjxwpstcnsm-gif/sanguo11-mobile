# R00–R14 审计交接 — PARTIAL

只交接本轮审计；不是开始R15的授权。先读reports/R00-R14-AUDIT.md、DEFECTS.md、evidence/R00-R14-AUDIT.json和全部原阶段提示词。

保持同一agent/native-pc-visual / PR67。APK源badd7e56308b4f6a47d60353d49023be8fc492a2，main在审计时ac29b458325b52d6e302ca44270d16552de4ed7f，最终文档HEAD从PR复读，禁止硬回退到这里。main未合并，Unity未启动，game-api/game-runtime/core/地图/资产本轮保护差分为空。

本轮MapHost/SceneRenderGate是正式运行修复：失焦/隐藏/后台停止渲染，保留原资源；owner线程和close终态保护。旧eeb09同探针实际FAIL，新badd3次真实Dialog循环PASS101（含轮询），不能撤销门控或将其当预览/日期已修复。

仍阻塞：R12原完整预览pending614/WAITING_FRAME；此后势力确认、startScenario和正式命令闭环未到达。7月/4月widget/snapshot正确但实际顶部仍1月，不能只检查TextView值或加invalidate就宣布解决。纹理/rig部分同步解码未改。PC V1未闭合、R09 V2 FAIL，禁止直接铺全国。物理ARM64/Mali/Adreno、完整运动视频/全部手势、真实PSS/GPU/30分钟热稳定NOT_RUN。

最终CI36091281604的16 host脚本通过、APK/androidTest/lint通过，整个candidate因R12仍FAIL；core原输入和candidate分别exit1相同AI部署断言。原日志必须继续保留，不能为了绿灯修改期望。旧R13 workflow保护的是过时资产基线，4个R14材质变动使它在前置失败，不等于本轮改了玩法。

R00新文档是追溯补齐，history/原PROGRESS/DEFECTS逐字保留；R03/R06/R07早期PENDING和R10/R11缺独立manifest不得补造历史哈希。当前213条完整CSV/JSON/MD及原图/host/baseline/源码patch随交付总ZIP提供，紧凑索引含哈希。

隔离AVD测试脚本含pm clear，禁止直接对用户手机执行。不要清档、削减AI/地图/单位、移动城港关、换引擎或覆盖既有资源来使验收变绿。
