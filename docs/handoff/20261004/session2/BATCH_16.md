# 批十六：原普通音乐情势分支严格边界

新增tools/audio/inspect_pc_music_city_threat_predicates.py，PC原EXE SHA守卫、目录只读、不启动Wine、不调用规则命令。原587f00/587d70完整指令执行，明确只读getter/地域汇总/建筑结果边界；2240项验证valid/owner/原方向关系、selfSum的9999/10000/10001、三倍比例前/等/后、建筑override短路。随后移除建筑结果hook，执行完整587e40/587ca0原指令，192项验证HP999/1000/1001（max5000）、troops2999/3000、energy29/30、归属/方向关系/附近结果。共2432项通过，模拟只读fixture全64KiB及原RNG地址前后字节不变；这些是显式fixture边界，不声称完整原世界3MiB或真实普通场景全部闭合。

原城市分支：587f00仅有效己方城市，selfSum<10000且3×(selfSum+relationBitSum)<otherRelationSum；587d70己方或原4b5cc0(cityOwner,queriedForce)许可城市，selfSum>10000且3×(selfSum+relationBitSum)>otherRelationSum。10000与比例等号都不成立。原建筑优先条件是currentHp signed-short<maxHp/5整数界、troops<3000、energy<30、归属/原方向关系、nearby。没有替换任何条件指令，也没有手动模拟函数prologue；fixture值只从明确readonly边界输入。

完整结果music-city-building-native.json.gz（SHA ed2dc49e8a02c67e6c0618bc8a2ed17b60439e0d4f1e2ed1738c0dae40d60fef），含1120个城市输入case×两函数与96个建筑case×两函数、原代码SHA、结果和遍历。587a00实际单位分类/地域汇总、587b50/587c00附近判定仍为未闭合边界，不把它们当已经得到的移动端规则事实。MEDIA_INPUT_CONTRACT.md追加确切下层事实字段，强调native来源/关系方向/全地域而非按钮、文案或当前可见部队。

本批只改媒体工具、自有契约和证据；core/game-api/game-runtime、metadata、MainActivity/公共入口/manifest、全局progress/STATE/PC_PARITY_STATUS未改。没有操作设备或用户数据。生产实现/APK仍是批十四已安装版本；普通原BGM和voice绑定仍0，目标active。下一步完成原汇总/附近与场景语义、消费已提交事实进行顺序集成，必须在正常多回合、读档和实际PCM验收，不能以这些fixture通过替代。
