# Java堆余量：设备决定的申请与双预算回归

用户20261006指出8/12/16GB手机可增大Java堆。A在原分配/生命周期修复基础上申请`android:largeHeap=true`，作为交付包额外余量。app构建配置`-PgameLargeHeap=false`保留普通堆回归包，两个APK必须各自构建、记录SHA、备份安装、正常流程测试并恢复全文件SHA；不能借用旧包成绩。不会修改共享Gradle或4JNI。

整机RAM不是应用Java堆限额。Android官方`application largeHeap`说明：请求大堆，不能保证固定增加；`ActivityManager.getMemoryClass/getLargeMemoryClass`返回设备普通/大堆额度。`GameApplication`启动日志记录这两值、`Runtime.maxMemory()`实际字节及整机RAM，接受实际设备值，不根据手机广告RAM推断。

来源：https://developer.android.com/guide/topics/manifest/application-element#largeHeap 与 https://developer.android.com/reference/android/app/ActivityManager#getLargeMemoryClass() 。5554此前普通实际384MiB，系统属性heapgrowthlimit384m/heapsize512m；512MiB只能在新APK实际运行后确认。没有可用ARM连接，用户原设备/API/ABI/Java限制及原分配栈仍未知。

普通堆要求仍保留：13/16来源零色已修复；地图共享/压缩顶点与有界缓存/释放修复先前在384MiB复现/回归。旧安装包daecedbb…的全16正常地图测试曾采样383.91/384MiB、没有观察到OOM却余量不足。大堆也可能增加活跃对象、GC耗时，native/GPU预算仍需独立测量；新包不能据此宣布内存问题闭合。B在自己的核心结算/人物查询取证和修复，不合其WIP。

本批同时准备PC格子火13/138的ADD/SRCALPHA/ONE地图材质：保留深度测试、关闭深度写入，纹理最多33份、每纹理最多2种混合实例，未知模式显式失败。原over材质与168固定输入不改。新材质两次字节一致并由VerifiedMaterial单独SHA守卫。现4JNI仍仅8个静态模板，动态13控制器/生命周期需最终串行兼容扩展；不能把材质准备称为正常原版火焰验收。
