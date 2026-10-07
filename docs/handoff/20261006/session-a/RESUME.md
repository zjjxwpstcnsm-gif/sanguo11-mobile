# 2026-10-06 用户解除空间阻断后继续

用户明确“有空间了，你继续吧”。恢复审计时磁盘约190GiB可用；main仍ef413be3653820dd6449ba7f02aa60bed5b26ef5，完整继承源仍0e7b9bc2df90249a50851baeda58c7d183ea6059。本轮从A完成提交5279693c继续，不纳入B WIP，不修改冻结JNI/序列化/Unity。

使用当前目录out/session-a/gradle-home独立缓存、app/build及out/session-a。共享SDK与Gradle分发仅只读；没有共享可写Gradle缓存。本恢复检查点最初普通debug构建保持非debuggable/R8。后继已按用户授权默认申请largeHeap=true，并以-PgameLargeHeap=false独立保留普通堆回归；实际额度依运行设备测量，详见HEAP_BUDGET.md。

5554检查为桌面且无游戏进程，A取得/tmp/sanguo11-emulator-5554-session-a.lock；5582不操作。device_session.py重新流式备份完整private和external树（包括内部cache，不只files/shared_prefs），同时核对设备前后逐文件SHA和archive。旧备份缺531文件，不复用。每次安装必须fresh backup-verified，安装两个APK后读回各自SHA，finally恢复原档并读回全部文件SHA。只移除本次独占测试新增的regular文件，不清数据。

SessionAMapRepairInstrumentation从正常菜单选择实际16源、触控势力/确认、手势缩放/平移、取消/重试、正常目录、Home、实际屏幕方向菜单、正常槽位保存/读取和退出重开。只读反射用于观察MapHost/原Session事实，不构造World/snapshot/规则状态。自动控制滚动使菜单项可达，点击与双指/单指操作通过系统输入注入；人工真机可达性仍待ARM设备实测。

本文件的新增不是验收通过声明。备份、构建和真实安装结果由out/session-a/resume-install-01/session.json、build-resume.log与后继INSTALLED_AUDIT.json记录。原格子火worker扩展仍交最终串行集成；正常BGM/voice、全部caller与ARM完整目标仍未闭合。
