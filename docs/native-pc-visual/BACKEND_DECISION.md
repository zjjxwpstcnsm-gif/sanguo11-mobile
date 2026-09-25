# 唯一后端决定（R00–R14 总审计追溯补齐）

记录日期：2026-09-25。不是声称 R00 当时已交付本文件。

继续现有 Android 原生 Filament 1.56.0，`Engine.Backend.OPENGL`，OpenGL ES 3.0 基线。复用 `MainActivity -> MapHost -> FilamentMapView` 的正式地图路径、已有 GLB/材质/连续地形，不引入第二套 GLES/GLSurfaceView。既有 2D 是安全回退/用户选择，不是伪装为原生 3D 的替代验收。

理由：精确源码 badd7e56308b4f6a47d60353d49023be8fc492a2 已实际构建 APK、instrumentation 与 lint；同次审计的 7625ad3 安装实拍能显示正式地图，真实模态窗口生命周期修复通过。仍有全图预览超时和 UI 合成画面陈旧，但没有证据证明是不可修复的 Filament 底层阻塞，更没有支持换引擎的同场景对比。不得把这些问题当作重启 Unity 或重写 GPU 层的理由。

固定 Java 17 源码/字节码、Gradle 8.13、AGP 8.10.0、SDK35、Filament runtime/material 1.56.0 与已有开发签名。此次未升级依赖，也未重新编译已有 filamat；已有材质输入/产物校验随主机回归执行。CPU mesh/GLB 队列继续有界；Filament 上传、释放保留 owner thread。纹理/rig 初始化并非全部异步，记录为未闭合项。

Unity 历史工程及 game-api/game-runtime 保留；原生构建使用 `-PnativePcVisual=true -PunityBridgeProbe=false`，APK 没有 libunity.so。ARM64 打包不等于 ARM64 真机通过。PC 美术和性能仍 PARTIAL，不因后端构建成功而升级结论。
