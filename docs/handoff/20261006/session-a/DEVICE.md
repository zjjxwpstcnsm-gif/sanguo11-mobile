# 设备前置核查

2026-10-06：ADB emulator-5554、emulator-5582，均x86_64，没有ARM。5554运行AVD san11-pc-map，PID93106；5582运行san11-media-integrated，PID53152，不杀进程。应用活动页为Launcher，无活动测试进程、当前/tmp无5554会话锁；此时仅只读检查，未获得长期锁，不操作设备。

5554 API29，dalvik.vm.heapsize512m，heapgrowthlimit属性空；不能将它视为截图手机384MiB。内部files376KiB/shared_prefs40KiB、外部Android/data目录2,679,460KiB。未经完整备份不会安装。dumpsys activity instrumentation在API29不支持，需用完整activity的ActiveInstrumentation字段检查。
