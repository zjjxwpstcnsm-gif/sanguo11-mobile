# 正常新局/单挑组合61

组合来源：B不可变candidate60（HEAD36f059b4、main ef413be、完整继承0e7b9bc）全部11247有效源码/资源输入，A冻结新局适配925748c6及完成依赖闭合01b725ed/closure227。A227明确替代226；冻结overlay SHAfa81f88a31224cd71d7b448d30a7937bbbca290f88e5b68c7931c31bc1852dcb，共37个A路径，manifest直接读取已提交Git blob而非活动WIP。

先核磁盘53.4GB空闲后逐成员安全解包，候选原所有字节/SHA逐一核验；每个A路径原始before SHA须精确匹配候选manifest，再覆盖为A冻结after。保留原四份JNI逐SHA不变，新增两份A完成格子火子进程库独立路径。额外只复制自有B验收/复现工具与init，全部路径在写入前记录于独立source-inputs台账。准备时11259文件精确集合/SHA全部通过；后续工具/说明追加分别记录before-after，不改APK生产源。A/B canonical文件均未修改，没有合另一会话WIP。

完整目录 `out/session-b/native-opening-combined61`，独立build/out/project-cache，复用本B会话独占Gradle依赖缓存，与A无缓存共用。在完整物化的canonical A字节上构建，不再运行旧readonly-theme的partial覆盖。旧候选与旧依赖仍原样保留，仅作为前态证据。先前 `native-opening-combined61.init.gradle` 是尚未应用的partial override准备；实际完整源组合只使用 `native-opening-acceptance61.init.gradle` 设置自有测试runner及组合SOURCE_REVISION，不改变规则或seed。

独立游戏/测试APK和core/api/runtime JAR构建已成功：76tasks全部执行，2m29s。实际命令：

```sh
docs/handoff/20261006/session-b/run-session-b.sh ./gradlew \
  -p out/session-b/native-opening-combined61 \
  -I "$PWD/docs/handoff/20261006/session-b/native-opening-acceptance61.init.gradle" \
  --project-cache-dir "$PWD/out/session-b/native-opening-combined61/out/session-b/gradle-project-cache" \
  --max-workers=2 '-Dorg.gradle.jvmargs=-Xmx2g -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8' \
  :core:jar :game-api:jar :game-runtime:jar :app:assembleDebug :app:assembleDebugAndroidTest \
  --offline --no-daemon
```

全输入/确切A前后SHA及新JNI在 `native-opening-combined61-source-inputs.json`；APK生产guard在 `native-opening-combined61-source-guard.json`，目前6164路径。冻结由 `session_b_freeze_opening_combined.py` 核全部输入、当前生产路径集合、原168固定APK资源、原4+A2 JNI、签名/新runner注册、三B JAR逐ZIP成员字节一致，再归档全部完整源并逐成员读回。不使用旧APK的测试成绩。

设备尚未安装。专用验收顺序见NATIVE_ANDROID_ACCEPTANCE.md：普通新局选项取消与明确确认、正常真实出征/地图移动/AI应战/人控、战斗保存读取和第二进程冷续行、自然终局三旬保存，再第三进程重开。仅可用B5582且每次安装先完整备份用户保存/库/偏好/内外部文件，最后恢复全部原文件并逐SHA读回；5554继续A独占。设备控制工具的`--source-root`只接受这个自有完整目录，先核实实际生产path set及全部SHA再允许设备锁/备份/安装。

此组合是本版正常入口验收候选，尚非完成批次/完整还原。仍需设备结果、完整胜败放弃/三将支援装备、全16来源与有效人物/事件/AP、旧31–39/custom完整续行、UnityU01语义及ARM。所有原未知保留，不启用封存27/28候选，也不从显示忠诚补原字节。
