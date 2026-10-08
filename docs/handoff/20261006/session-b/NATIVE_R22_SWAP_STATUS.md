# r22中止与原备份恢复，r23验收待运行

全目标未完成，最新完成有限提交dbf9b92e544aa9c0307f77320d04fc83ecff83ba。完整main0e7b9bc2df90249a50851baeda58c7d183ea6059未回退，旧609dirty目录未动。生产规则、A/Bridge/Unity/JNI无新改动。

r22 test-only完整11337输入/源码642876708B，source SHA664409d928a018b3e68dcc117fe3a98631e6e1571ee0b9bc4c82e42bea665154；游戏APK341bc343a23d96fc00a54cdd35263cb215f09f952cd1246c876246dd289097c2，teste108b6a00d74d75d8981e1306cf494fd7b0e7800ee88dabfd555d69541d8a089。签名、全archive逐SHA/168固定/6JNI/完整path-set/独立76任务构建通过；全15/772备份后实际安装读回SHA精确。初始普通slot3读取真实r21 accepted battleb81…、一合法人控输入、完整battle save/load通过。

静态对照发现B验收程序在phase3应先点“选择特殊动作”，到phase7才点“必杀技”；生产ContestUi已有正确入口、没有生产问题。B中止自身测试，正常阶段已结束，cold阶段因force-stop打印Process crashed。这是主动中止，不是自然游戏崩溃；r22整体passed=false，不能计正常换将/必杀终局通过。随后误对验收监督程序发SIGINT，恰处于恢复post-test.tar归档，KeyboardInterrupt准确日志保留，原backup完整且没有数据清理/覆盖丢失。该恢复中断不能隐藏或算恢复通过。

新工具session_b_restore_acceptance.py只复用已有verified backup恢复，不安装、不清数据：检查原supervisor已退出、锁属于本B目录、两个local-before.tar逐成员SHA与原results.before相等、冻APK SHA相等；重新push并读回两个remote backup SHA，再完整归档当前文件、只移除本测试新增/逐SHA已验derived文件、提取原backup，完整restored-recovery.tar逐成员SHA+路径核对。15内部/772外部全部相等，无新增遗留，恢复json complete=true，锁释放；准确日志out/session-b/duel-query-check/native-r22-restoration-recovery.log与目录out/session-b/native-duel-apk61-r22-swap-acceptance-v1。原中断post-test.tar保留，重试写独立-recovery.tar。之后才开始r23独立构建。

r23只修测试入口映射及监督程序finally恢复阶段忽略误发SIGINT，保存断言和所有规则不放宽。普通按钮路线、胜败/登用、三旬完整存取/最终cold仍须新实装，不能借用Host策略成绩。Host从真实b81…三不同合法战术：attack/可用换关羽/必杀14合234帧自然胜，捕获侯成10173/native173后自然拒绝；defense/换将/必杀31合558帧胜但无俘虏；spirit/换将25合391帧败。各终局/失败保存分开保留，不重复已失败attempt，无成功登用APK结论。

原恢复取证：完整59c330两次尝试均在59a4b0→5887b0→原0x32602b0脚本接口缺失失败，未到恢复段，未hook替换或跳过原调用。独立原1100人恢复循环59c479→59c4f8边界矩阵Source0/native660/44输入完成：HP<100时+30，injury0/1/2/3分别上限100/80/50/30，原HP100不进入更新；含99重伤→30、100重伤保持100。只有对应体力byte变化，原World/RNG全部恢复，receipt5b2d03100b5e434f54d1bcfe9dc6cc6aa2109c614e5ac857ebc3da59ae6fc768。v1停止方式与Native call API expected boundary不符失败，v2使用原工具显式stop参数完整循环返回；都保留。**这是边界取证，不是完整controller/正常PC新局/完整恢复规则通过**，未自动回填或修改任何旧档/生产健康策略。
