# 实际环境（2026-09-25 总审计追溯补齐）

| 能力 | 本轮实测结果 |
|---|---|
| 本地容器 | Linux；OpenJDK21.0.11，使用 javac --release17；Python3.13.5；新门控76断言与架构检查可运行 |
| 容器网络 | 无法直连 GitHub/DNS，未伪称本地 git clone/push；通过授权 GitHub 连接器读源码、原子 Git tree/commit/ref 写入和读取 CI 制品 |
| 本地 Android/图形 | 无 SDK/adb/可用设备；没有本地安装/真机结论 |
| 本地资产工具 | Blender/matc/etcpak 不可用；此次不重新导出模型/材质，不把已有二进制校验称作重新生成 |
| 精确 SHA CI | Ubuntu GitHub-hosted；Temurin17；Gradle8.13/AGP8.10.0；platform/build-tools35.0.0；Filament1.56.0 |
| CI 图形设备 | API29 Pixel2 profile、x86_64、SwANGLE 软件后端、4GiB AVD RAM、512MiB heap；不是 ARM64 物理机 |
| 当前构建 | badd7e56308b4f6a47d60353d49023be8fc492a2 的 APK/androidTest/lint 实际成功；下载后核对 APK SHA、DEX源码身份、签名和298项资产 |
| PC 参考 | REFERENCE_INDEX.csv 有官方手册/用户洛阳02参考；精确相机/多数季节UNKNOWN；洛阳01是占位响应，不能当参考 |
| 真机/性能 | ARM64 Adreno/Mali、PSS/GPU/30分钟热稳定、完整手势矩阵 NOT_RUN；不从回调间隔估算手机FPS |
| Unity | 未启动，未寻找授权，不进入原生构建，历史工程保留 |

构建命令：

```sh
./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true
```

安装组件为 `game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity`，测试组件在 `game.sanguo.mobile.dev.test`。scripts/verify-native-r00-r14-audit.sh 的 pm clear 只用于隔离的 CI AVD，会删除测试应用数据；禁止直接对用户有存档的手机照搬该脚本。日常安装不要求清档。

当前签名与历史开发包相同；versionCode98、versionName0.98.0-native-r00-r14-audit。arm64-v8a/armeabi-v7a/x86/x86_64 均打包，minSdk26/target35。打包 ABI 与对应设备运行结论严格分开。
