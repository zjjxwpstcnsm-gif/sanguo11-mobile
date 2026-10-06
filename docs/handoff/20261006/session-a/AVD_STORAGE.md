# A 独立模拟器存储恢复

第二次安装47326188被PackageInstaller拒绝，设备历史finalStatus=-4；当时5554的/data约6GiB且只剩0.9GiB。APK签名、168资源和4JNI核验通过，不能把失败包记成实装通过。失败后9内部/3797外部原文件逐SHA恢复相同。

原AVD为san11-pc-map、PID93106、API29/x86_64、GPU host、2GiB RAM/4核。原设备console kill和TERM未完成退出；在已同步文件系统、暂停avd后，仅结束这个已核实的A独占PID。5582不操作。保留原完整AVD，25个regular文件完整APFS独立复制后逐SHA全等，列表见AVD_CLONE.json。没有删用户库或资源。

副本out/session-a/avd-home/san11-session-a-repair.avd，独立ANDROID_AVD_HOME，继续5554。原live qcow2只读合并为副本raw持久盘并扩到16GiB。旧SDK1.42.13不支持quota/encryption特性，Android设备内1.44.4在线扩容也失败；没有移除文件系统feature。采用kernel.org官方e2fsprogs1.47.3源码，tar.xz SHA857e6ef800feaa2bb4578fbc810214be5d3c88b072ea53c5384733a965737329与官方sha256sums.asc一致。

在out/session-a/e2fsprogs内独立编译：`./configure --disable-nls --disable-fuse2fs --disable-fsck --disable-uuidd --enable-bsd-shlibs`、`make -j2`。没有全局安装。运行时DYLD_LIBRARY_PATH指向其lib。克隆设备再次sync/停机，检查qcow2 full-backing-filename严格指向私有raw，备份私有raw/overlay后commit；使用新e2fsck -f -p修复私有盘journal/两项bitmap，resize2fs成功扩到4194304个4KiB块（16GiB）。完整日志在avd-home/offline-resize-modern.txt。原AVD/已继承原始副本仍保留。

扩容启动和全部用户文件SHA回读、堆限制与新APK流程结果继续记录；文件系统扩容成功不是游戏或ARM验收。每次安装仍须完整备份或完整旧archive与当前全部文件SHA重新全等验证，不使用少531文件的旧archive。克隆也不得清数据。
