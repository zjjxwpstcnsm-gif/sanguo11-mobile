# 本轮主机验证失败与纠正

1. 初次memory fix子进程实际PASS后，报告写入因Path与str拼接TypeError失败。修正报告路径后以同生产源码重新执行并保存成功exitCode；不把报告失败抹掉。
2. 首次三角面基线比较把v65 World直接mapRevision改v63，SaveCodec按真实开发地校验拒绝“襄平15,185距离13”；替代空World也因无据点拒绝“记录数量无效”。两个错误属于诊断夹具，不改core断言。随后使用既有真实pc-map-v063.sg11.gz兼容存档，原PC/旧图全部16窗口SHA与Save/RNG比较通过。
3. 最终mem工具baseline模式从git固定0e7b9bc2取全部15份原生产app模型代码，fix模式编译当前完整继承代码，保证后继可复现旧OOM。完整APK/Android设备仍未验，不将桌面栈扩大为用户ARM栈。
