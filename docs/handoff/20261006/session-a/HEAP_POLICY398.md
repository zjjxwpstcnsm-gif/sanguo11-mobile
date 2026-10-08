# 大堆默认与设备额度

用户明确允许增大 Java 堆；保留现有默认 `gameLargeHeap=true`。这次核查未改生产配置，仍允许 `-PgameLargeHeap=false` 的普通堆回归。AndroidManifest 的 `largeHeap` 使用该占位符。

当前实装游戏 SHA `811e02d1bcae8e0d6a3954cb4d225aa54e3f56da762659e58b1b10c27c51e672`。当前5554/API29/x86_64的设备属性为 heapgrowthlimit=384m、heapsize=512m；实际运行日志的 maxMemory=536870912 bytes。这些额度仅属于本模拟器，不能推断8/12/16GB手机的额度。ARM设备尚未连接验收。

Android 官方说明每个应用有设备决定的堆上限，`largeHeap` 不保证固定增量，可用 `getMemoryClass()` / `getLargeMemoryClass()` 查询。来源：[application largeHeap](https://developer.android.com/guide/topics/manifest/application-element#largeHeap)、[内存管理](https://developer.android.com/topic/performance/memory-overview#RestrictAppMemory)。手机总内存与应用 Java 堆上限分别记录。

最新配对的16来源正常菜单/预览/新局/缩放平移与最终保存读取、独立进程冷启动已在 MATRIX_REDO393.json 验收。该有限通过不替代全部16来源独立冷启动、原用户ARM的354832字节失败栈、全瞬时峰值或GPU预算。当前395正在真实火流程过旬阶段，不提前记为通过。

大堆与减少重复网格分配、索引缓冲共享及生命周期释放共同保留；不吞OOM、伪造恢复成功或永久关闭3D。所有数据恢复以各实装 session 的逐文件原SHA为准；此次无安装和设备写入。
