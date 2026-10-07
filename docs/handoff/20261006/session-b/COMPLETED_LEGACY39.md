# 完成批次：真实39舌战中途档续行验收

共同已提交基点：`89534120e46e488136db0e661270757d9c4a4c50`。继承完整主源包括ef413be及后继0e7b9bc文档；没有从旧52315bf/609dirty另起。自己的分支 `codex/rules-content-contest-repair`，只提交下表测试/夹具/工具与本文，保留全部其他Native WIP。生产源码零增量，A生产文件/Bridge/Unity/四JNI未改。本批不宣称整个还原目标完成。

## 独立编译与真实流程

生产源全部来自89534120，590个stage条目逐SHA核对，只叠加独立运行器及原档夹具。A已冻结47326188依赖及168固定输入/4JNI仍受完整守卫；11705个保留/有效输入构建前后逐SHA一致，独立build/project cache。APK58完整76任务执行、1分45秒。

生产APK313403426字节/SHA `b457efcde0c6ea80f73046b41b98700452d77bb6f7dd892eb35b458866cd76aa`；测试3010480字节/SHA `1afa10aa8574cb7e976a250c0def130c189281d648b32cd94efa0dad361950b8`。实际5582安装后二者SHA读回相同。没有编译PcDuel、新物品策略或其他Native WIP。58使用共享驱动验收；59使用本批独立驱动重新安装同一冻结APK，独立备份及完整验收。

真实既有39档2139833字节/SHA `5d002d3cee6f84700cd5e1292c05b672e8a05e0b547ac7d753f2b7987d5e2f7f`，没有制造版本头或局面。普通菜单读取 → 取消策略确认全World/RNG纯净 → 明确采用已有终局策略且整份舌战序列化/策略、生命周期、原RNG保留 → 合法按钮出牌 → 自然终局 → 三个完整前台旬 → 手动存档/菜单读取 → force-stop后冷自动与手动重开 → 同一完整流程。现有3D重试暖/冷均检测非空Surface且全World/RNG不变；不等于原版美术验收。

暖、冷完整最终档均2151609字节/SHA `565745800406cbee008e1f9a1d01ed5d18c28756a3dce8418879c18004968072`；58以及含WIP的先前56/57结果相同，先前通过未移用为本批证据。中途2140740字节/SHA `9d9876096a3f802a9da4992f8342bc5c9f4d49bacd77e5b25b9b60fdee0bdc86`。

58/59各完整备份并恢复15内部、772外部文件，逐SHA/零额外文件/5582锁释放；未清数据、未使用5554、未删除用户资源。SDK与用户备份不进入交付源包。

## 路径与前后SHA

| 路径 | 前 | 后SHA256 |
|---|---|---|
| app/src/androidTest/java/game/sanguo/mobile/SessionBLegacy39Instrumentation.java | 新增/无旧字节 | 5290db2f20f9b3d9cc08d7c6d0acc795e6d72085d3116f9b00efda7800b5341b |
| app/src/androidTest/assets/session-b/legacy39-original.sg11 | 新增/无旧字节 | 5d002d3cee6f84700cd5e1292c05b672e8a05e0b547ac7d753f2b7987d5e2f7f |
| docs/handoff/20261006/session-b/legacy39-completed.init.gradle | 新增/无旧字节 | 315e16d6aad7a5710863c96f2f9043df5bedd9a427571824b5c55ecc5f0f5c79 |
| tools/content/session_b_stage_completed_legacy39.py | 新增/无旧字节 | 60520fae1d2390a8b74d887caae0adb536d65fb20eb132dd6cb20cb4444e904a |
| tools/content/session_b_freeze_completed_legacy39.py | 新增/无旧字节 | 288410276ca6f12f3db109feae2fe0ef5d196818596759e73e528cd8a205118a |
| tools/content/session_b_verify_legacy39_ui.py | 新增/无旧字节 | 6252bdc68ec1a92032e6503ea1a3a1cce6ce684c82a7c141425db7f29c3b3b4e |
| tools/content/session_b_archive_completed_legacy39.py | 新增/无旧字节 | 87a11b0623ba0b2343de87200fbabe8369b274e24db044501e9a6b87920fb6af |

本文无自引用SHA；其SHA与完整提交由Git给出。所有新增路径先登记在自己的OWNERSHIP.md，公共台账未并写。原共享运行器的items及其他WIP未提交；独立驱动只接受legacy39。

## 复现

在保留完整当前工作区中运行（需要既有JDK17/Android SDK；不分发SDK）：

```sh
./docs/handoff/20261006/session-b/run-session-b.sh /usr/bin/python3 tools/content/session_b_stage_completed_legacy39.py
./docs/handoff/20261006/session-b/run-session-b.sh /usr/bin/python3 tools/content/session_b_freeze_completed_legacy39.py capture legacy39-completed-build-UNIQUE
./docs/handoff/20261006/session-b/run-session-b.sh ./gradlew -I docs/handoff/20261006/session-b/legacy39-completed.init.gradle --project-cache-dir out/session-b/completed-legacy39-build/gradle-project-cache --max-workers=2 '-Dorg.gradle.jvmargs=-Xmx2g -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8' :app:assembleDebug :app:assembleDebugAndroidTest --offline --no-daemon
./docs/handoff/20261006/session-b/run-session-b.sh /usr/bin/python3 tools/content/session_b_freeze_completed_legacy39.py freeze legacy39-completed-build-UNIQUE
./docs/handoff/20261006/session-b/run-session-b.sh /usr/bin/python3 tools/content/session_b_verify_legacy39_ui.py --serial emulator-5582 --apk out/session-b/legacy39-completed-build-UNIQUE/frozen/app-debug.apk --test-apk out/session-b/legacy39-completed-build-UNIQUE/frozen/app-debug-androidTest.apk --source-guard out/session-b/legacy39-completed-build-UNIQUE/frozen/source-guard.json --output out/session-b/legacy39-completed-ui-UNIQUE
```

stage已存在会拒绝覆盖；从源码包使用其中冻结stage，或在新的输出目录中首次生成。脚本允许89534120后的纯测试提交；后续提交若改变生产源，须重审新共同基点，不能静默回到旧生产源。验收驱动要求5582空闲，先备份所有保存/库/偏好，最终逐SHA恢复。

## 证据与未完成

实际证据：`out/session-b/legacy39-completed-build58/{build-inputs.json,frozen-report.json,frozen/}` 与 `out/session-b/legacy39-completed-ui59/{results.json,instrumentation.txt,cold-instrumentation.txt,evidence/legacy39/,evidence/legacy39-cold/}`；源码包及逐项读回清单在 `out/session-b/completed-legacy39-source{.tar.gz,-manifest.json}`。用户备份tar不属于交付。

当前全WIP正式模块Session1690/Bridge/建筑197及架构/168守卫通过，但不以这些结果宣布原规则完整还原。本批没有新增玩法规则，只证明一份真实39档的明确迁移和普通界面续行。31/38真实历史夹具、全部自定义与16来源矩阵仍待验证；原舌战准入/费用、认输/外交，原单挑新局触发与完整回调，ARM/原版美术音频/Unity动态验收都仍在完整目标中。未知不能据本批替代或关闭。
