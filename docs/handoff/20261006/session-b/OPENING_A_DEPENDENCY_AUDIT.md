# A224 冻结依赖审计

2026-10-08。当前 B HEAD36f059b4 / common main ef413be / 完整继承0e7b9bc。前轮测试准备是实际进展，本轮不改目标范围或宣称完成。

A已将新局适配冻结在 `925748c6139b66a799e59de509c47151b00d8acb` 的 session-a 文档/补丁中，A生产源未修改。B直接读该提交里的manifest/patch，并核对stage两份before都与A已完成基点`9ab4a3d61bd5e6db2f89ff7c787ee1481105a211`对应Git blob逐字节一致；两份after与冻结SHA一致，不读取活动WIP作来源。manifest SHA37bd7d4d…de51，patch SHAefb8a1da…669d。read-only完整明细见 `out/session-b/opening-a-dependency-audit-v1.json`。

candidate60当前实际编译Main为旧完成A依赖fc1ed192…de5，A224 before为a74def84…16b4；在原47326188到A9ab4a3d6之间包含39增/9删的已完成Main变化：sceneFacts绑定、真实事件记录、攻击任务入口、施工真值提示、原人物详情日志、TurnPlayback接线。不能因为新菜单hunk恰好匹配便丢掉这批完成修复。picker候选20c6fa13…b12、A before0fa45a93…253，另有完成63284162的同步preview资源释放修复（18增/5删）；224的六picker hunks有三段在候选中不匹配，因此不能强套patch掩盖前态差异。

已向A请求正式已完成依赖链/逐路径before-after SHA或候选before的A-owner增量。其当前可核实活跃chat在补清单；B不改MainActivity、picker或任何A canonical文件。读取源与审计不等于串行集成已完成。

为后继组合准备自有 `native-opening-combined61.init.gradle`：必须有A完成依赖链manifest且每份Java字节SHA一致才允许构建；明确逐路径排除旧canonical编译输入，也排除同类旧完成A生成副本，保留其它主题依赖。B冻结/设备守卫工具已显式覆盖新的 ignored readonly-opening-dependencies61 输入目录及manifest，防止新加入的编译依赖漏出 source guard。尚未复制任何A stage源，manifest不存在时必须拒绝构建。公共Gradle/原168固定输入/4JNI仍冻结。

下一步按A明确冻结的依赖路径预登记并核SHA，顺序接回自有编译输入；然后独立构建/冻结新游戏与测试APK，实际5582前核空闲/完整备份、普通菜单→出征/AI单挑→保存冷续行→终局三旬→最终重开并逐SHA恢复。新的组合依赖将单独形成candidate61证据，candidate60不覆盖。不把A编译、B审核或任何旧模拟器成绩移作本版验收。

后继：A正式提交01b725ed的closure227完成37路径，明确替代226。为避免遗漏资源/Manifest/新火JNI，本轮选择完整候选源安全解包后依精确original before物化全部A冻结after，而非partial override。新完整源组合已经76tasks全执行构建成功，文档OPENING_COMBINED61.md与source-inputs记录实际方案；早先partial init未应用。A/B canonical不变。
