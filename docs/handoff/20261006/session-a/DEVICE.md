# 设备前置核查

2026-10-06：ADB emulator-5554、emulator-5582，均x86_64，没有ARM。5554运行AVD san11-pc-map，PID93106；5582运行san11-media-integrated，PID53152，不杀进程。应用活动页为Launcher，无活动测试进程、当前/tmp无5554会话锁；此时仅只读检查，未获得长期锁，不操作设备。

5554 API29，dalvik.vm.heapsize512m，heapgrowthlimit属性空；不能将它视为截图手机384MiB。内部files376KiB/shared_prefs40KiB、外部Android/data目录2,679,460KiB。未经完整备份不会安装。dumpsys activity instrumentation在API29不支持，需用完整activity的ActiveInstrumentation字段检查。

只读流式读取5554现有已安装应用（没有安装或写数据）：

[
  {
    "path": "/data/app/game.sanguo.mobile.dev-8HMOjt5RNB-_Yui6fL6UXQ==/base.apk",
    "bytes": 94708220,
    "sha256": "306830e30ce5a944fb356b0664de4beaae4adce4ff4de77f6a052a7bcbb1f0b6"
  }
]

该包身份仅为设备现状，不能作为本轮A修复安装通过。
