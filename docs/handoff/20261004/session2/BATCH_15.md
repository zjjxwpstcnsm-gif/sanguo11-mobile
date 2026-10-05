# 批十五：原普通音乐日期/势力/城市输入闭合

新增tools/audio/inspect_pc_music_context_sources.py，以PC只读安装的原EXE和原场景解码器运行，不启动Wine、不改PC资源、不运行规则命令/RNG。对全部16份源场景、42原城和47原势力进行只读getter核验，且每次比较完整3MiB及原RNG；日期36组合×3年份及caller控制槽−2..9原范围一起执行，最终4488项通过。完整结果music-context-source-native.json.gz，场景原SHA/每城归属/每势力原阈值/控制槽和日期向量保留。

这次把上一轮未命名“42对象”证明为原城市registry0..41，virtual40确实调用47b2b0；原491270势力pointer→native force身份已核验。49d670的至少10原城阈值不能应用到项目owner ordinal/关港/自定义城市。原季节计算4825a0在已核验的零calendarOffset日期与(month−1)/3一致，所有场景的实际offset也记录，不推断其它offset。caller57fc70的virtual48实际480fa0仅在源势力+60的controlSlotRaw0..7时调用音乐选择器；不凭此把未知12/16场景标成菜单。

NativeScenarioUnitDecoder直接执行情势谓词的早期诊断发现建筑registry还需原metadata/grid初始化，不能把空/未构造vtable得到的结果当原场景情势。组合已解码header/grid后，明确零外部UI/unit-list上下文可返回false，但这只是负输入边界，不等于正常游戏情势还原，未用于普通播放。三个优先情势谓词与原调用场景仍需完整闭合。

MEDIA_INPUT_CONTRACT.md增加具体MusicSourceContext：同已提交StateToken的来源势力/控制槽/时钟/原42城归属及nullable精确谓词/scene。当前GameSnapshot没有时钟及源势力/城身份，不从按钮或项目owner猜。高优先未知不得当false跳到季节曲；媒体不得生成规则事件/调用RNG、earn或save。普通原BGM/人物voice绑定仍0，这批未宣称正常播放完成。

实现/APK仍为批十四已安装00c18c9c；本批只改媒体来源工具/自有契约与证据，不改core/game-api/game-runtime、人物metadata、公共入口/manifest、MainActivity、全局progress/STATE/PC_PARITY_STATUS，也不操作设备。下一步按精确来源补全情势/场景及顺序集成输入，最终在正常流程实际播放验收。完整目标active。
