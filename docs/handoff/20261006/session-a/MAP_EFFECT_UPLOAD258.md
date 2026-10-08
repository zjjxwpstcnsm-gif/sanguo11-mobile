# 258 地图特效上传内存候选

实际 PcMapEffects.frame 对每次非空原帧创建 count×36 的 float[]，再创建相同大小直接 FloatBuffer；现有上传无释放回调。这是可核实的分配热点，不等于用户354832字节失败栈根因。

独立stage改为3个上传槽，实际Filament completion callback后才能再次写入，全部槽忙时保留待显示帧并返回；一个可扩容Java数组保留相同PcEffectCoordinates.vertex转换和原packet顺序。每槽按需增长至32768×36 float最大，Java数组上限4718592B，三直接槽上限14155776B；直接缓冲被替换后的回收、GPU内部副本/纹理/进程内存不计入此声明，不冒称实际总峰值。关闭时解除idle引用，busy槽等回调解除。

逐SHA前bf036ccf…后d1dee02f…，patch2f03c7dd…；Android35/JDK17编译exit0，独立patch字节读回一致，规范生产与165/239当前包未改。不是JNI或规则修改，不写Save/RNG/Token。新包需验证连续火/缩放平移/暂停/慢上传背压/Home旋转/退出重开/消失及全文件恢复；尚无安装/像素/内存运行结果。最终组合必须核PcMapEffects确切前SHA，不能盲用到B WIP。
