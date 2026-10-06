# Java堆余量：设备决定的申请与双预算回归

用户20261006指出8/12/16GB手机可增大Java堆。A在原分配/生命周期修复基础上申请`android:largeHeap=true`，作为交付包额外余量。app构建配置`-PgameLargeHeap=false`保留普通堆回归包，两个APK必须各自构建、记录SHA、备份安装、正常流程测试并恢复全文件SHA；不能借用旧包成绩。不会修改共享Gradle或4JNI。

整机RAM不是应用Java堆限额。Android官方`application largeHeap`说明：请求大堆，不能保证固定增加；`ActivityManager.getMemoryClass/getLargeMemoryClass`返回设备普通/大堆额度。`GameApplication`启动日志记录这两值、`Runtime.maxMemory()`实际字节及整机RAM，接受实际设备值，不根据手机广告RAM推断。

来源：https://developer.android.com/guide/topics/manifest/application-element#largeHeap 与 https://developer.android.com/reference/android/app/ActivityManager#getLargeMemoryClass() 。5554此前普通实际384MiB，系统属性heapgrowthlimit384m/heapsize512m；512MiB只能在新APK实际运行后确认。没有可用ARM连接，用户原设备/API/ABI/Java限制及原分配栈仍未知。

普通堆要求仍保留：13/16来源零色已修复；地图共享/压缩顶点与有界缓存/释放修复先前在384MiB复现/回归。旧安装包daecedbb…的全16正常地图测试曾采样383.91/384MiB、没有观察到OOM却余量不足。大堆也可能增加活跃对象、GC耗时，native/GPU预算仍需独立测量；新包不能据此宣布内存问题闭合。B在自己的核心结算/人物查询取证和修复，不合其WIP。

本批同时准备PC格子火13/138的ADD/SRCALPHA/ONE地图材质：保留深度测试、关闭深度写入，纹理最多33份、每纹理最多2种混合实例，未知模式显式失败。原over材质与168固定输入不改。新材质两次字节一致并由VerifiedMaterial单独SHA守卫。现4JNI仍仅8个静态模板，动态13控制器/生命周期需最终串行兼容扩展；不能把材质准备称为正常原版火焰验收。

## 大堆实装Large11

6ea385ff新包8e866064fbc340fab7559615731e8531609a3f1a6b7dd18df42f568a2d24cb89，5554 API29/x86_64实际Runtime536870912B(512MiB)。16真实来源完整地图/取消重试/势力/缩放/人物/Home/方向/存读/Activity重开PASS2729；新PID27156→18033真实自动读回PASS79，完整Save/RNG SHA59e38e0368fd67fac4a946a67bbbfcb889ccf4ac6827364daa354c825eb90753。原9内部/3797外部全部最终SHA恢复，未清数据。

413次采样Java最高359.663MiB/512MiB；native heap单独最高273.510MiB，主进程总PSS单独最高574.764MiB。另从来源6后开始的原效果子进程smaps_rollup采样最高69392KiB PSS，最多2个后回落1/0。这些峰值不同时、不相加；graphicsPss0不表示GPU VRAM为0。没有原用户ARM栈，不宣布原OOM根因全闭合。普通堆5a1625…包独立构建/备份/安装复验进行中，结果不能复用大堆包。

## 普通堆实装Normal12

5a1625eded1146953b6abab9e5fddd38c25985afea9b1f8422a2c7316e6a365d实际Runtime402653184B(384MiB)，largeHeap=false。来源7/8正常取消/新局/反复缩放/人物/Home/方向/存读/Activity重开PASS475，独立新PID20871→1447读回完整Save/RNG SHAaed8016befda5e1d0ef25d17a14de178129ca706fbc2fce3cbbfe6e11db47d98，PASS79。77次Java采样最高305.082MiB。原9内部/3797外部最终SHA全等，锁释放。原生60.12秒MP4共1365帧、无音频，SHA78df7c5cb36b163498b88894c5f7d71d6719c64ee19527e0d471472af571c050；文件本身保留，未重定时；原PC时序/ARM未接受。

Cached scene package170cdd76/cbe648… newly installed all-source20 completes2729 normal checks across16 real sources plus79 true cold checks. Runtime512MiB,413 normal Java samples peak399716976B (381.20MiB), native allocated independent peak282964840B, main totalPSS independent peak617424KiB. GPU graphicsPSS reports0 on this emulator driver: unavailable, not zero. Peaks are independent, not summed. New cached API package normal384MiB regression still pending (old6ea Source7/8 ordinary result remains separate). Complete9/3797 original SHA restored. Source13 binding packagec10603d3/6ed8f2… is next independently installed fire21 and cannot inherit these all16 results.

Latest combined8979dae2/d72b47… installed Fire25 repeats full normal/cold Source14 original fire lifecycle after completed Gov integration and cap32MiB.153 source-child smaps samples, max one observed, peak52410KiB (~51.18MiB) vs prior independent24 peak234524KiB (~229.03MiB). Exact50/71 source-record/time proof isolates translator-cap semantic invariance; whole-device/PSS peaks are not synchronized or GPU numbers. Full9/3797 user SHA restored; native cap did not replace the CPU-array fix or ordinary384 regression. Ordinary same-source APK6f987fd… independently built, not yet installed/accepted.


## 2026-10-07 当前结论（历史批次状态保留）

最新可复现OOM26原栈为网格数组分配，不以largeHeap掩盖：c6315ba6保持展开三角数据逐位相同，跨度33格线数组47410688→23430704B、跨度47为67930368→33527440B。4e312b89普通堆APK5df034d1独立实装28全16正常流程2729+不同PID冷79通过；Java峰值330279456B约314.98MiB/384MiB。完整9/3797SHA恢复。大堆a7d4b711亦独立实装正常攻击/火/建设/舌战，证据互不借用。

最新军团规则组合e078fb2f/7415f48c已默认大堆实装35双势力运输/占港/保存读取/新PID冷恢复通过；36真实火暂停及生命周期正在运行。新组合全16与普通384尚待，旧组合28成绩不等于新包通过。无ARM真机，仍不能从整机8/12/16GB推算Java实际上限；交付保留默认大堆和正常堆回归开关，实际设备日志为准。
