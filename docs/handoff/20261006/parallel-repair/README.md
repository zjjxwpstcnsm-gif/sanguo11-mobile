# 2026-10-06 并行修复目标

用户将自行建立两个新会话。各复制一份全文：

- [目标A：界面、地图内存、火与媒体](GOAL_A_VISUAL_STABILITY.md)
- [目标B：军事设施、规则、剧本武将、单挑舌战](GOAL_B_RULES_AND_RESTORATION.md)

开始前读[AUDIT.md](AUDIT.md)及[CONTRACT.md](CONTRACT.md)。[APP_OWNERSHIP.json](APP_OWNERSHIP.json)/[TOOL_OWNERSHIP.json](TOOL_OWNERSHIP.json)逐路径登记文件归属和本次基线SHA；并行不覆盖对方文件。三图原始字节保存于screenshots并有manifest。

本批仅新增文档与审计证据，不修生产代码、不操作设备、不启动会话、不恢复已暂停目标、不改公共main。

配色复现（从项目根；编译实际当前FactionColors，类路径沿用独立架构编译产物，缺产物先运行架构编译并保留其已知失败）：

```sh
javac -encoding UTF-8 -cp game-runtime/build/architecture-check -d out/session-a/palette-audit app/src/main/java/game/sanguo/mobile/FactionColors.java docs/handoff/20261006/parallel-repair/PaletteAudit.java
java -Xmx768m -cp out/session-a/palette-audit:game-runtime/build/architecture-check:core/src/main/resources game.sanguo.mobile.PaletteAudit
```

本次使用Temurin17，输出见[palette-audit.txt](palette-audit.txt)。13/16来源有效势力颜色为0，共57个来源×势力组合；这是配色代码复现，不是手机APK全流程成绩。
