# 原格子火：绑定证据与最终串行契约

本轮仅读取供给 PC 安装，不改四份 JNI，不把状态标记称为原火焰恢复。

## 原绑定与生命周期

- `5a0770` 写源格子 DWORD 的 bits23..25，再调用 `59fea0`。媒体不调用该规则状态写入，不执行规则 RNG。
- `59fea0` 在 200×200 边界内检查原地形谓词与占用。A 不重算许可，消费 B 已提交的火存在、消失事实。
- 火非零且格子+0x10没有实例：`417880→417770` 取得世界位置，`414670` 用模板 **13** 经 `413d20` 创建实例；清零时 `413470` 清除实例 byte+6 的 bit0x04，格子句柄归零。
- 模板13真实查表为资源 **138**、`media/effect/effect013.efdx`，20076字节，SHA256 `7cc91cc42aeb548bcc85d21663251391ed5e78495129d38f9f8d8f708fcc44d7`。
- 原 `413510` 构造设 mode9/remap-1，正常13不走九组重映射。该模板的原 `4133d0/413420` 均返回0，走 `457880` count1/matrix。
- `413770` 返回活实例与 generation，不能把 template root 当活实例停止。两个原 factory 实例及原 stop 已在独立 VM 验证，最终绘制归零，无 `472150/4721d0` 调用且 percent RNG 字节不变。
- 诊断明确使用 post-resource-loader ready flag 的输入边界，不包含原完整 GPU/启动验收。

原位置由 `417770` 得到：X=(4×nativeX+114)×5，Z=(4×nativeY+114+2×奇偶)×5。Y 读取源当前地形 raw7/8 对应 coarse 水字节与原0.0025偏置，或源1025格高程字节，再乘0.5。不能一概借用 legacy 平地或猜水陆。再用既有统一0.05比例及 source origin 转到 Filament。

## 原纹理与材质

两次独立原 controller/final quad/material 执行字节一致，七个时间输入的 quad 数为7/7/11/22/19/33/53。纹理仅 common image2、11、13，已与实际归档逐 RGBA 比较。

真实混合为 ADD/SRCALPHA/ONE（1/5/2）及 ADD/SRCALPHA/INVSRCALPHA（1/5/6）。现 `PcMapEffects` 只接受后者，会拒绝原火的前者；不能统一改为 over 或改变原 alpha。

需要独立 map additive material，保留地图深度测试、关闭 ZWRITE、双面和原 alpha GREATER1，并保留原队列顺序。现 fullscreen presentation-add 关闭深度测试，不能直接作为地图材质。原 encoded 色彩及 PC/Android framebuffer 比较仍待验收。

## 冻结 worker 缺口与兼容方案

当前 `pc_effect_scene_probe.c` 容器严格限定 PCFXSC01、八个固定模板 `[8,9,16,17,18,19,20,23]`、126个SEFF落点及2280字节。stream 只有初始化、时间、相机、暂停，无13的动态创建或停止。

最终串行扩展应保持旧容器、旧216字节命令与旧场景 SHA，增加显式版本和原13/138资源，以及独立“格子火集合同步”命令。常规输出保持184字节 source packet；模板、纹理、混合都按原字段验明，未知不绑定。不要把13改标签为8或重排旧126落点。

同步只消费同 StateToken 的不可变事实。以源格子坐标为实例 key，非零时创建、清零或灭火到期时调用原 stop；非零寿命刷新不重开（原59fea0也不重开）。session/generation/map来源变化关闭旧 worker，再从真实当前集合重建。app 保留精确 long；私有句柄不进入 SaveCodec、Bridge 或规则。

最终须验收单格、多格、持续燃烧、同格刷新、灭火与到期、暂停dt0、缩放平移、低画质、后台、读档、快速切换与句柄释放。Java/native/GPU预算须实测，不以本诊断替代正常火计、施工、多旬、存读和退出重开。火球、火种、火船、施工、受损与拆除仍各查原 caller，不能借用13。
