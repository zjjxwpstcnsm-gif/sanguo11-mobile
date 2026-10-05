# 批十八：原普通音乐地域遭遇谓词

新增tools/audio/inspect_pc_music_encounter_predicate.py，在真实原Scenario.s11解码registry、单位/武将/district/势力getter、原城市地域byte表及原外交关系位/计数上执行完整587fb0。没有行为/getter/条件shim，只有已有原解码器的IO/import支持；PC目录只读、不启动Wine、不调用规则命令或RNG。

513项通过：512组合覆盖查询单位或另一owner、区域owner为0/1、query→regionOwner与unitOwner→query的位/计数独立变化、第二单位owner−1/0/1/2及同/异地域；另加空列表false。每次完整3MiB原世界、1MiB地域格表、1MiB外部列表上下文及原RNG前后字节一致。只有显式fixture设定的数据字段，不声称其就是当前Android的正常场景状态。

原逻辑：己方单位进入许可关系地域直接true；进入原关系位许可地域时，还要求同地域存在587b50许可单位。非己方单位须按unitOwner→queriedForce的方向许可，且地域由queriedForce拥有。两个方向不能混用。原城市地域由坐标/bit域/原byte表获取，不用可见距离代替。完整music-encounter-native.json.gz保留输入/结果、原EXE和代码SHA/表SHA；MEDIA_INPUT_CONTRACT.md追加精确依赖。

至此587fb0的条件分支有原代码执行证据，与批十六建筑/城市比例、批十七原汇总和存在判定连起来；还不是全部正常场景/启动后单位列表/移动端来源身份映射闭合。普通BGM和voice仍未自动绑定，当前APK仍为批十四已验证检查点，不把源fixture通过扩大为实际普通音乐恢复。

本批只改媒体工具、自有契约和证据；core/game-api/game-runtime、人物metadata、公共入口/manifest/MainActivity、全局progress/STATE/PC_PARITY_STATUS未改，没有操作设备/存档/用户偏好。下一步完成正常场景/已提交来源投影的顺序集成，再实际多回合/读档/PCM验证。完整目标active。
